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
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoadResult;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/** Imports a resource and resolves it through a declarative scene at runtime. */
public final class ProjectImportExample {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.import-example";
    private static final String IMPORTER_ID = EXTENSION_ID + "/text-importer";
    private static final RegisteredType IMPORTED_TEXT = new RegisteredType(EXTENSION_ID + "/imported-text", 1);
    private static final Logger LOGGER = Logger.getLogger(ProjectImportExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private ProjectImportExample() {
        throw new AssertionError("ProjectImportExample cannot be instantiated");
    }

    /**
     * Loads, publishes, and resolves imported content in the supplied example project.
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
            runProject(project, catalogResult, manager);
        }
    }

    /** Loads the authored import definition from the example project. */
    private static ImportDefinition loadDefinition(GameProject project) {
        ImportLoadResult result = new ImportLoader().load(project, Path.of("imports/text.import.json"));
        return requireValue(result.definition(), result.diagnostics(), "import definition");
    }

    /** Composes the scene through the logical import reference authored in project data. */
    private static void runProject(
            GameProject project, ExtensionCatalogLoadResult catalogResult, ImportManager manager) {
        ProjectRuntimeLoadResult result = new ProjectRuntimeLoader(ENGINE_VERSION)
                .load(project, catalogResult.catalog(), List.of(new TextRuntimeExtension()), manager);
        try (ProjectRuntime runtime = requireValue(result.runtime(), result.diagnostics(), "runtime")) {
            runtime.start();
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
            return EXTENSION_ID;
        }

        @Override
        public void register(ProjectImportRegistry registry) {
            registry.registerImporter(TYPE, new TextImporter());
        }
    }

    /** Converts one UTF-8 text source to a native typed project resource. */
    private static final class TextImporter implements ProjectImporter {
        @Override
        public void inspect(ImportInspectionContext context) {
            context.sourceItem(new SourceItem("text", EXTENSION_ID + "/text", "Text", true, Map.of(), List.of()));
        }

        @Override
        public void prepare(ImportPreparationContext context) throws IOException {
            inspect(context);
            String source = Files.readString(context.asset().path(), StandardCharsets.UTF_8);
            ProjectValue uppercase = context.definition().settings().get("uppercase");
            String content = uppercase instanceof ProjectValue.BooleanValue value && value.value()
                    ? source.toUpperCase(Locale.ROOT)
                    : source;
            String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            String resource = String.format(Locale.ROOT, """
                    {
                      "$schema": "https://jscene3d.org/schemas/resource-1.json",
                      "schemaVersion": 1,
                      "type": "io.github.glynch.jscene3d.import-example/imported-text",
                      "typeVersion": 1,
                      "properties": {
                        "content-base64": "%s"
                      }
                    }
                    """, encoded);
            context.artifact(
                    ImportArtifactDescriptor.resource("output/text", IMPORTED_TEXT, List.of()),
                    output -> output.write(resource.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /** Provides executable factories declared by the example descriptor. */
    private static final class TextRuntimeExtension implements ProjectRuntimeExtension {
        private static final RegisteredType CONSUMER = new RegisteredType(EXTENSION_ID + "/text-consumer-3d", 1);

        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ProjectRuntimeRegistry registry) {
            registry.registerSceneNode(CONSUMER, TextRuntimeExtension::createConsumer);
            registry.registerResource(IMPORTED_TEXT, TextRuntimeExtension::createText);
        }

        /** Creates a scene object after resolving its imported resource reference. */
        private static ProjectRuntimeObject createConsumer(SceneNodeContext context) {
            ProjectValue value = Objects.requireNonNull(context.properties().get("text"), "text");
            ResourceReference reference = ((ProjectValue.ReferenceValue) value).reference();
            return new TextConsumer(context.resolveResource(reference, ImportedText.class));
        }

        /** Decodes one validated native resource into its runtime value. */
        private static Object createText(ResourceFactoryContext context) {
            ProjectValue value = Objects.requireNonNull(context.properties().get("content-base64"), "content-base64");
            String encoded = ((ProjectValue.TextValue) value).value();
            String content = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            return new ImportedText(content);
        }
    }

    /** Immutable runtime value created from the generated resource document. */
    private record ImportedText(String content) {}

    /** Minimal scene object proving imported data reached executable runtime code. */
    private static final class TextConsumer implements ProjectRuntimeObject {
        private final ImportedText text;

        /** Stores the resolved imported text. */
        private TextConsumer(ImportedText text) {
            this.text = text;
        }

        @Override
        public void start() {
            LOGGER.info(() -> "Resolved imported resource: " + text.content());
        }

        @Override
        public void close() {
            // This example object owns no external resources.
        }
    }
}
