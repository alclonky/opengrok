/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.index;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.opengrok.indexer.configuration.Configuration;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.history.RepositoryFactory;
import org.opengrok.indexer.util.FileUtilities;
import org.opengrok.indexer.util.IOUtils;
import org.opengrok.indexer.util.TestRepository;

/**
 * Verify index check.
 * @author Vladimír Kotal
 */
class IndexCheckTest {

    private TestRepository repository;
    private final RuntimeEnvironment env = RuntimeEnvironment.getInstance();
    private Path oldIndexDataDir;
    private Configuration configuration;

    @BeforeAll
    public static void setUpClass() {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        RepositoryFactory.initializeIgnoredNames(env);
    }

    @BeforeEach
    public void setUp() throws IOException, URISyntaxException {
        repository = new TestRepository();
        repository.create(IndexerTest.class.getResource("/repositories"));
        oldIndexDataDir = null;
        configuration = new Configuration();
        configuration.setDataRoot(env.getDataRootPath());
        configuration.setSourceRoot(env.getSourceRootPath());
    }

    @AfterEach
    public void tearDown() throws IOException {
        repository.destroy();

        if (oldIndexDataDir != null) {
            IOUtils.removeRecursive(oldIndexDataDir);
        }
    }

    /**
     * Generate index(es) with history enabled, check the index.
     */
    private void testIndex(boolean projectsEnabled, List<String> subFiles, IndexCheck.IndexCheckMode mode) throws Exception {
        env.setHistoryEnabled(false);
        configuration.setHistoryEnabled(false);
        env.setProjectsEnabled(projectsEnabled);
        configuration.setProjectsEnabled(projectsEnabled);
        Indexer.getInstance().prepareIndexer(env, true, true,
                null, null);
        Indexer.getInstance().doIndexerExecution(null, null);

        IndexCheck.isOkay(configuration, mode, subFiles);
    }

    @Test
    void testIndexVersionNoIndex() throws Exception {
        IndexCheck.isOkay(configuration, IndexCheck.IndexCheckMode.VERSION, new ArrayList<>());
    }

    @Test
    void testIndexVersionProjects() throws Exception {
        testIndex(true, new ArrayList<>(), IndexCheck.IndexCheckMode.VERSION);
    }

    @Test
    void testIndexVersionSelectedProjects() throws Exception {
        testIndex(true, Arrays.asList("mercurial", "git"), IndexCheck.IndexCheckMode.VERSION);
    }

    @Test
    void testIndexVersionNoProjects() throws Exception {
        testIndex(false, new ArrayList<>(), IndexCheck.IndexCheckMode.VERSION);
    }

    @Test
    void testIndexVersionOldIndex() throws Exception {
        oldIndexDataDir = Files.createTempDirectory("data");
        Path indexPath = oldIndexDataDir.resolve("index");
        Files.createDirectory(indexPath);
        File indexDir = new File(indexPath.toString());
        assertTrue(indexDir.isDirectory(), "index directory check");
        URL oldIndex = getClass().getResource("/index/oldindex.zip");
        assertNotNull(oldIndex, "resource needs to be non null");
        File archive = new File(oldIndex.getPath());
        assertTrue(archive.isFile(), "archive exists");
        FileUtilities.extractArchive(archive, indexDir);
        env.setDataRoot(oldIndexDataDir.toString());
        configuration.setDataRoot(oldIndexDataDir.toString());
        env.setProjectsEnabled(false);
        configuration.setProjectsEnabled(false);
        assertFalse(IndexCheck.isOkay(configuration, IndexCheck.IndexCheckMode.VERSION, new ArrayList<>()));

        assertThrows(IndexCheck.IndexVersionException.class, () ->
                IndexCheck.checkDir(Path.of(env.getSourceRootPath()), indexPath, IndexCheck.IndexCheckMode.VERSION));
    }

    @Test
    void testEmptyDir(@TempDir Path tempDir) throws Exception {
        assertEquals(0, tempDir.toFile().list().length);
        IndexCheck.checkDir(null, tempDir, IndexCheck.IndexCheckMode.VERSION);
    }

    /**
     * Check that {@link IOException} thrown during index check is propagated further.
     * Runs only on Unix systems because the {@link IOException} is not thrown on Windows
     * for non-existent directories.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testIndexCheckIOException() throws Exception {
        // This is set to simulate IOException in IndexCheck.checkDir().
        configuration.setDataRoot("/nonexistent");

        configuration.setProjectsEnabled(false);

        IndexCheck.IndexCheckMode mode = IndexCheck.IndexCheckMode.VERSION;
        assertThrows(IOException.class, () -> IndexCheck.checkDir(Path.of(configuration.getSourceRoot()),
                Path.of(configuration.getDataRoot()), mode));
        // Assumes that IndexCheck.checkDir() is called via IndexCheck.isOkay() and the latter method
        // infers the result from the call.
        assertThrows(IOException.class, () -> IndexCheck.isOkay(configuration, mode, new ArrayList<>()));
    }

    @Test
    void testIndexDocumentsCheckProjects() throws Exception {
        testIndex(true, Arrays.asList("mercurial", "git"), IndexCheck.IndexCheckMode.DEFINITIONS);
    }

    @Test
    void testIndexDocumentsCheckNoProjects() throws Exception {
        testIndex(false, new ArrayList<>(), IndexCheck.IndexCheckMode.DEFINITIONS);
    }
}
