/* Copyright (c) 2012-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.cli.porcelain;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.locationtech.geogig.cli.AbstractCommand;
import org.locationtech.geogig.cli.CLICommand;
import org.locationtech.geogig.cli.CommandFailedException;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevObject.TYPE;
import org.locationtech.geogig.plumbing.DiffCount;
import org.locationtech.geogig.plumbing.ParseTimestamp;
import org.locationtech.geogig.plumbing.ResolveObjectType;
import org.locationtech.geogig.plumbing.RevParse;
import org.locationtech.geogig.plumbing.merge.ReadMergeCommitMessageOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.NothingToCommitException;
import org.locationtech.geogig.repository.DiffObjectCount;
import org.locationtech.geogig.repository.ProgressListener;

import org.locationtech.geogig.base.Strings;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Stores the current contents of the index in a new commit along with a log message from the user
 * describing the changes.
 * <p>
 * CLI proxy for {@link CommitOp}
 * <p>
 * Usage:
 * <ul>
 * <li>{@code geogig commit -m <msg>}
 * </ul>
 * 
 * @see CommitOp
 */
@Command(name = "commit", aliases = "ci", description = "Record staged changes to the repository")
public class Commit extends AbstractCommand implements CLICommand {

    @Option(names = "-m", description = "Commit message")
    private String message;

    @Option(names = "-c", description = "Commit to reuse")
    private String commitToReuse;

    @Option(names = "-t", description = "Commit timestamp")
    private String commitTimestamp;

    @Option(names = "--amend", description = "Amends last commit")
    private boolean amend;

    @Option(names = { "--quiet",
            "-q" }, description = "Do not count and report changes. Useful to avoid unnecessary waits on large changesets")
    private boolean quiet;

    @Parameters(arity = "*", description = "If given, commit only the indicated paths")
    private List<String> paths = new LinkedList<>();

    @Option(names = "--allow-empty", description = "Create commit even if there are no staged changes (i.e. it'll point to the same root tree than its parent)")
    private boolean allowEmpty;

    /**
     * Executes the commit command using the provided options.
     * 
     * @param cli
     * @see org.locationtech.geogig.cli.AbstractCommand#runInternal(org.locationtech.geogig.cli.GeogigCLI)
     */
    public @Override void runInternal(GeogigCLI cli) throws IOException {

        final Geogig geogig = cli.getGeogig();

        if (message == null || Strings.isNullOrEmpty(message)) {
            message = geogig.command(ReadMergeCommitMessageOp.class).call();
        }
        checkParameter(!Strings.isNullOrEmpty(message) || commitToReuse != null || amend,
                "No commit message provided");

        Console console = cli.getConsole();

        Ansi ansi = newAnsi(console);

        RevCommit commit;
        ProgressListener progress = cli.getProgressListener();
        try {
            CommitOp commitOp = geogig.command(CommitOp.class).setMessage(message).setAmend(amend)
                    .setAllowEmpty(allowEmpty);
            if (commitTimestamp != null && !Strings.isNullOrEmpty(commitTimestamp)) {
                Long millis = geogig.command(ParseTimestamp.class).setString(commitTimestamp)
                        .call();
                commitOp.setCommitterTimestamp(millis.longValue());
            }

            if (commitToReuse != null) {
                Optional<ObjectId> commitId = geogig.command(RevParse.class)
                        .setRefSpec(commitToReuse).call();
                checkParameter(commitId.isPresent(), "Provided reference does not exist");
                TYPE type = geogig.command(ResolveObjectType.class).setObjectId(commitId.get())
                        .call();
                checkParameter(TYPE.COMMIT.equals(type),
                        "Provided reference does not resolve to a commit");
                commitOp.setCommit(geogig.getRepository().context().objectDatabase()
                        .getCommit(commitId.get()));
            }
            commit = commitOp.setPathFilters(paths).setProgressListener(progress).call();
        } catch (NothingToCommitException | IllegalStateException notificationError) {
            throw new CommandFailedException(notificationError.getMessage(), true);
        }
        final ObjectId parentId = commit.parentN(0).orElse(ObjectId.NULL);

        console.println("[" + commit.getId() + "] " + commit.getMessage());

        if (progress.isCanceled()) {
            return;
        }

        if (quiet) {
            console.println("Committed.");
        } else {
            console.print("Committed, counting objects...");
            console.flush();
            final DiffObjectCount diffCount = geogig.command(DiffCount.class)
                    .setOldVersion(parentId.toString()).setNewTree(commit.getTreeId())
                    .setProgressListener(progress).call();

            if (progress.isCanceled()) {
                return;
            }
            ansi.fg(Color.GREEN).a(diffCount.getFeaturesAdded()).reset().a(" features added, ")
                    .fg(Color.YELLOW).a(diffCount.getFeaturesChanged()).reset().a(" changed, ")
                    .fg(Color.RED).a(diffCount.getFeaturesRemoved()).reset().a(" deleted.").reset()
                    .newline();

            console.print(ansi.toString());
        }
    }
}
