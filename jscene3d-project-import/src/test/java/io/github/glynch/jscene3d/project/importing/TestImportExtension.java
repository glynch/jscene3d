/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportRegistry;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Synthetic importer exercising the public extension seam. */
final class TestImportExtension implements ProjectImportExtension {
    private static final RegisteredType IMPORTER =
            new RegisteredType("io.github.glynch.import-test/source-importer", 1);
    private static final AssetId DEFINITION_ID = new AssetId(UUID.fromString("4e230064-13e5-4ad2-bba8-8fc7dbf4ab32"));
    private static final EntityId ROOT_ID = new EntityId(UUID.fromString("cb40d4a7-ef36-4bd4-9598-fe7180d78a24"));

    @Override
    public String id() {
        return "io.github.glynch.import-test";
    }

    @Override
    public void register(ProjectImportRegistry registry) {
        registry.registerImporter(IMPORTER, new TextImporter());
    }

    /** Imports one text source and one tracked dependency as a payload. */
    private static final class TextImporter implements ProjectImporter {
        @Override
        public void inspect(ImportInspectionContext context) throws IOException {
            String source = Files.readString(context.asset().path(), StandardCharsets.UTF_8);
            if (source.startsWith("WARN")) {
                context.warning(TestDiagnosticCode.SOURCE_NOTICE, "", Map.of("sourceText", source));
            }
            context.dependency(dependency(context));
            context.sourceItem(new SourceItem(
                    "entries/main",
                    "io.github.glynch.import-test/text-entry",
                    "Main entry",
                    true,
                    Map.of(),
                    List.of(new SourceItemRelation("contains", "entries/child"))));
            context.sourceItem(new SourceItem(
                    "entries/child",
                    "io.github.glynch.import-test/text-entry",
                    "Child entry",
                    false,
                    Map.of(),
                    List.of()));
            context.sourceItem(new SourceItem(
                    "entries/unused",
                    "io.github.glynch.import-test/text-entry",
                    "Unused entry",
                    true,
                    Map.of(),
                    List.of()));
        }

        @Override
        public void prepare(ImportPreparationContext context) throws IOException {
            inspect(context);
            Path dependency = dependency(context);
            context.dependency(dependency);
            String source = Files.readString(context.asset().path(), StandardCharsets.UTF_8);
            if (source.startsWith("FAIL")) {
                throw new IOException("synthetic import failure");
            }
            String content = source + ":" + Files.readString(dependency, StandardCharsets.UTF_8);
            EntityDefinition definition = new EntityDefinition(
                    DEFINITION_ID,
                    "Imported text",
                    new LocalEntity(ROOT_ID, "Imported text", true, List.of(), List.of()));
            context.artifact(
                    ImportArtifactDescriptor.entityDefinition("definitions/main", List.of("output/main")),
                    output -> DefinitionWriter.write(output, definition));
            context.artifact(
                    ImportArtifactDescriptor.payload("output/main", "text/plain"),
                    output -> output.write(content.getBytes(StandardCharsets.UTF_8)));
        }

        /** Returns the synthetic dependency belonging to the test project. */
        private static Path dependency(ImportInspectionContext context) {
            return context.project().root().resolve("assets/dependency.txt");
        }
    }

    /** Diagnostic identity owned by this synthetic importing feature. */
    private enum TestDiagnosticCode implements DiagnosticCode {
        /** The source requested a synthetic warning. */
        SOURCE_NOTICE;

        @Override
        public String code() {
            return "test-import.source.notice";
        }

        @Override
        public String defaultMessage() {
            return "The synthetic source requested a notice";
        }
    }
}
