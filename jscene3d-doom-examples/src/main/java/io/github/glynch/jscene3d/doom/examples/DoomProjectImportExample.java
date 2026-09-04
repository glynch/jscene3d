/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.examples;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.importing.ImportManager;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.importing.SourceInspection;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Runs a generated classic map through the service-discovered Doom project importer. */
public final class DoomProjectImportExample {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final Logger LOGGER = Logger.getLogger(DoomProjectImportExample.class.getName());
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.jscene3d.doom-import-example",
                "name": "Doom Import Example",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.jscene3d.doom",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "io.github.glynch.jscene3d.wad", "requires": "0.1.0-SNAPSHOT"},
                {"id": "io.github.glynch.jscene3d.doom", "requires": "0.1.0-SNAPSHOT"}
              ],
              "assets": [
                {
                  "id": "content",
                  "type": "io.github.glynch.jscene3d.wad/source",
                  "path": "assets/content.wad"
                }
              ],
              "imports": ["imports/maps.import.json"]
            }
            """;
    private static final String IMPORT_DEFINITION = """
            {
              "schemaVersion": 1,
              "id": "maps",
              "source": "asset:content",
              "importer": "io.github.glynch.jscene3d.doom/maps",
              "selection": ["maps/MAP01"]
            }
            """;

    /** Prevents instantiation of this application entry point. */
    private DoomProjectImportExample() {
        throw new AssertionError("DoomProjectImportExample cannot be instantiated");
    }

    /**
     * Builds, inspects, imports, and reads one self-contained classic Doom map project.
     *
     * @param arguments no arguments
     * @throws IOException when example project creation or artifact reading fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 0) {
            throw new IllegalArgumentException("expected no arguments");
        }
        try (TemporaryWorkspace workspace = TemporaryWorkspace.create("jscene3d-doom-import-example-")) {
            Path projectDirectory = createProject(workspace.root().resolve("project"));
            GameProject project = loadProject(projectDirectory);
            ExtensionCatalogLoadResult catalog = new ExtensionCatalogLoader(ENGINE_VERSION)
                    .load(project, DoomProjectImportExample.class.getClassLoader());
            if (!catalog.isComplete()) {
                throw new IllegalStateException("extension catalog could not be loaded: " + catalog.diagnostics());
            }
            ImportDefinition definition = loadDefinition(project);
            ImportManager manager = ImportManager.create(
                    project,
                    catalog.catalog(),
                    workspace.root().resolve("cache"),
                    DoomProjectImportExample.class.getClassLoader(),
                    List.of());
            SourceInspection inspection = manager.inspect("content", "io.github.glynch.jscene3d.doom/maps");
            try (PreparedImport prepared = manager.prepare(definition)) {
                if (!prepared.preview().isValid()) {
                    throw new IllegalStateException(
                            "map import failed: " + prepared.preview().diagnostics());
                }
                prepared.commit();
            }
            String resource = readMap(manager, definition);
            LOGGER.info(() ->
                    "Discovered " + (inspection.items().size() - 1) + " map and imported maps/MAP01:\n" + resource);
        }
    }

    /** Creates the portable project documents and classic source map used by the example. */
    private static Path createProject(Path root) throws IOException {
        Files.createDirectories(root.resolve("assets"));
        Files.createDirectories(root.resolve("imports"));
        Files.writeString(root.resolve(ProjectLoader.MANIFEST_NAME), PROJECT_MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(root.resolve("main.scene.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("imports/maps.import.json"), IMPORT_DEFINITION, StandardCharsets.UTF_8);
        ExampleDoomWad.write(root.resolve("assets/content.wad"));
        return root;
    }

    /** Loads the generated project or reports its structured diagnostics. */
    private static GameProject loadProject(Path root) {
        ProjectLoadResult result = new ProjectLoader(ENGINE_VERSION).load(root);
        return requireValue(result.project(), result.diagnostics(), "project");
    }

    /** Loads the generated import definition or reports its structured diagnostics. */
    private static ImportDefinition loadDefinition(GameProject project) {
        ImportLoadResult result = new ImportLoader().load(project, Path.of("imports/maps.import.json"));
        return requireValue(result.definition(), result.diagnostics(), "import definition");
    }

    /** Reads the published MAP01 resource through its logical artifact identity. */
    private static String readMap(ImportManager manager, ImportDefinition definition) throws IOException {
        try (ImportedArtifact artifact =
                        manager.openArtifact(definition, "maps/MAP01").orElseThrow();
                InputStream input = artifact.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Requires one successful loading result while retaining structured diagnostics on failure. */
    private static <T> T requireValue(Optional<T> value, List<ProjectDiagnostic> diagnostics, String description) {
        return value.orElseThrow(() -> new IllegalStateException(description + " could not be loaded: " + diagnostics));
    }
}
