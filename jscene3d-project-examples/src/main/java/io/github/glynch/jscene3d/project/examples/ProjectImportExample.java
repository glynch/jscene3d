/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportManager;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/** Runs a deterministic text import through the public project-import interfaces. */
public final class ProjectImportExample {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final String IMPORTER_ID = "io.github.glynch.jscene3d.import-example/text-importer";
    private static final Logger LOGGER = Logger.getLogger(ProjectImportExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private ProjectImportExample() {
        throw new AssertionError("ProjectImportExample cannot be instantiated");
    }

    /**
     * Loads, inspects, prepares, publishes, and reads the supplied example project.
     *
     * @param arguments one example project-directory path
     * @throws IOException when imported content cannot be read
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one project-directory path");
        }
        Path projectDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        ProjectLoadResult projectResult = new ProjectLoader(ENGINE_VERSION).load(projectDirectory);
        GameProject project = requireValue(projectResult.project(), projectResult.diagnostics(), "project");
        ExtensionCatalogLoadResult catalogResult =
                new ExtensionCatalogLoader(ENGINE_VERSION).load(project, ProjectImportExample.class.getClassLoader());
        if (!catalogResult.isComplete()) {
            throw new IllegalStateException("extension catalog could not be loaded: " + catalogResult.diagnostics());
        }
        ImportDefinition definition = loadDefinition(project);
        try (TemporaryWorkspace cache = TemporaryWorkspace.create("jscene3d-import-example-")) {
            ImportManager manager = ImportManager.create(
                    project, catalogResult.catalog(), cache.root(), List.of(new TextImportExtension()));
            manager.inspect("source-text", IMPORTER_ID);
            try (PreparedImport prepared = manager.prepare(definition)) {
                prepared.commit();
            }
            String content = read(manager, definition);
            LOGGER.info(() -> "Imported content: " + content);
        }
    }

    /** Loads the authored import definition from the example project. */
    private static ImportDefinition loadDefinition(GameProject project) {
        ImportLoadResult result = new ImportLoader().load(project, Path.of("imports/text.import.json"));
        return requireValue(result.definition(), result.diagnostics(), "import definition");
    }

    /** Reads the published example artifact without depending on its physical cache path. */
    private static String read(ImportManager manager, ImportDefinition definition) throws IOException {
        try (ImportedArtifact artifact =
                        manager.openArtifact(definition, "output/text").orElseThrow();
                InputStream input = artifact.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Requires one successful loading result while retaining structured diagnostics on failure. */
    private static <T> T requireValue(Optional<T> value, List<ProjectDiagnostic> diagnostics, String description) {
        return value.orElseThrow(() -> new IllegalStateException(description + " could not be loaded: " + diagnostics));
    }

    /** Contributes the example's declared text importer. */
    private static final class TextImportExtension implements ProjectImportExtension {
        private static final RegisteredType TYPE = new RegisteredType(IMPORTER_ID, 1);

        @Override
        public String id() {
            return "io.github.glynch.jscene3d.import-example";
        }

        @Override
        public void register(ProjectImportRegistry registry) {
            registry.registerImporter(TYPE, new TextImporter());
        }
    }

    /** Converts one UTF-8 text source to an uppercase payload. */
    private static final class TextImporter implements ProjectImporter {
        @Override
        public void inspect(ImportInspectionContext context) {
            context.sourceItem(new SourceItem(
                    "text", "io.github.glynch.jscene3d.import-example/text", "Text", true, Map.of(), List.of()));
        }

        @Override
        public void prepare(ImportPreparationContext context) throws IOException {
            inspect(context);
            String source = Files.readString(context.asset().path(), StandardCharsets.UTF_8);
            ProjectValue uppercase = context.definition().settings().get("uppercase");
            String content = uppercase instanceof ProjectValue.BooleanValue value && value.value()
                    ? source.toUpperCase(Locale.ROOT)
                    : source;
            context.artifact(
                    ImportArtifactDescriptor.payload("output/text", "text/plain"),
                    output -> output.write(content.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
