/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.plumbing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.locationtech.geogig.base.Preconditions;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.porcelain.BranchListOp;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;
import org.locationtech.geogig.storage.GraphDatabase;

/**
 * Rebuilds the {@link GraphDatabase} and returns a list of {@link ObjectId}s that were found to be
 * missing or incomplete.
 */
public class RebuildGraphOp extends AbstractGeoGigOp<List<ObjectId>> {

    /**
     * Executes the {@code RebuildGraphOp} operation.
     * 
     * @return a list of {@link ObjectId}s that were found to be missing or incomplete
     */
    protected @Override List<ObjectId> _call() {
        Repository repository = repository();
        Preconditions.checkState(!repository.isSparse(),
                "Cannot rebuild the graph of a sparse repository.");

        List<ObjectId> updated = new LinkedList<ObjectId>();
        List<Ref> branches = command(BranchListOp.class).setLocal(true).setRemotes(true).call();

        GraphDatabase graphDb = repository.context().graphDatabase();

        for (Ref ref : branches) {
            Iterator<RevCommit> commits = command(LogOp.class).setUntil(ref.getObjectId()).call();
            while (commits.hasNext()) {
                RevCommit next = commits.next();
                if (graphDb.put(next.getId(), next.getParentIds())) {
                    updated.add(next.getId());
                }
            }
        }

        return updated;
    }
}
