/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Juan Marin (Boundless) - initial implementation
 */
package org.locationtech.geogig.geotools.cli.geojson;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.geogig.cli.CommandFailedException;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.cli.InvalidParameterException;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

@Ignore // REVISIT: ExportOp needs a revamp
public class GeoJsonExportTest extends RepositoryTestCase {

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
        GeoJsonExport exportCommand = new GeoJsonExport();
        String geoJsonFileName = new File(testRepository.getPlatform().pwd(), "TestPoints.geojson")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points", geoJsonFileName);
        exportCommand.run(cli);

        deleteGeoJson(geoJsonFileName);
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        GeoJsonExport exportCommand = new GeoJsonExport();
        String geoJsonFileName = new File(testRepository.getPlatform().pwd(), "TestPoints.geojson")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList(null, geoJsonFileName);
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportWithInvalidFeatureType() throws Exception {
        GeoJsonExport exportCommand = new GeoJsonExport();
        String geoJsonFileName = new File(testRepository.getPlatform().pwd(), "TestPoints.geojson")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("invalidType", geoJsonFileName);
        assertThrows(InvalidParameterException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportToFileThatAlreadyExists() throws Exception {
        GeoJsonExport exportCommand = new GeoJsonExport();
        String geoJsonFileName = new File(testRepository.getPlatform().pwd(), "TestPoints.geojson")
                .getAbsolutePath();

        exportCommand.args = Arrays.asList("WORK_HEAD:Points", geoJsonFileName);
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", geoJsonFileName);
        try {
            exportCommand.run(cli);
            fail();
        } catch (CommandFailedException e) {

        } finally {
            deleteGeoJson(geoJsonFileName);
        }
    }

    @Test
    public void testExportWithNoArgs() throws Exception {
        GeoJsonExport exportCommand = new GeoJsonExport();
        exportCommand.args = Arrays.asList();
        assertThrows(CommandFailedException.class, () -> exportCommand.run(cli));
    }

    @Test
    public void testExportToFileThatAlreadyExistsWithOverwrite() throws Exception {
        GeoJsonExport exportCommand = new GeoJsonExport();
        String geoJsonFileName = new File(testRepository.getPlatform().pwd(), "TestPoints.geojson")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points", geoJsonFileName);
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", geoJsonFileName);
        exportCommand.overwrite = true;
        exportCommand.run(cli);

        deleteGeoJson(geoJsonFileName);
    }

    private void deleteGeoJson(String geoJson) {
        File file = new File(geoJson + ".geojson");
        if (file.exists()) {
            file.delete();
        }
    }
}
