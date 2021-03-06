/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.geotools.cli.postgis;

import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.geogig.cli.CommandFailedException;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.cli.InvalidParameterException;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.geotools.TestHelper;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

@Ignore // REVISIT: ExportOp needs a revamp
public class PGExportTest extends RepositoryTestCase {

    private GeogigCLI cli;

    public @Override void setUpInternal() throws Exception {
        Console consoleReader = new Console().disableAnsi();
        cli = new GeogigCLI(consoleReader);

        cli.setGeogig(Geogig.of(repo));

        // Add points
        insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(points3);

        repo.command(CommitOp.class).call();

        // Add lines
        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(lines3);

        repo.command(CommitOp.class).call();
    }

    public @Override void tearDownInternal() throws Exception {
        cli.close();
    }

    @Test
    public void testExport() throws Exception {

        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.support.dataStoreFactory = TestHelper.createNullTestFactory();
        assertThrows(CommandFailedException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testNoArgs() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList();
        exportCommand.support.dataStoreFactory = TestHelper.createNullTestFactory();
        assertThrows(CommandFailedException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportToTableThatExists() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points", "table1");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(CommandFailedException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportToTableThatExistsWithOverwrite() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("WORK_HEAD:Points", "testTable");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", "testTable");
        exportCommand.overwrite = true;
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNonexistentFeatureTypeTree() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("invalidType", "invalidTable");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithNullTable() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points", null);
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList(null, "invalidTable");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithEmptyStringForFeatureType() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("", "invalidTable");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithEmptyStringForTable() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points", "");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithFeatureNameInsteadOfType() throws Exception {
        PGExport exportCommand = new PGExport();
        exportCommand.args = Arrays.asList("Points/Points.1", "invalidTable");
        exportCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }
}
