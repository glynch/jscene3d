/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.importing.ImportedDefinitionResolver;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Proves that an authored world can compose a published imported entity definition. */
final class ImportedDefinitionCompositionTest {
    private static final AssetId WORLD_ID = AssetId.from("31764fc9-a3ad-40aa-9783-ebc154e11ae7");
    private static final AssetId DEFINITION_ID = AssetId.from("4e230064-13e5-4ad2-bba8-8fc7dbf4ab32");
    private static final EntityId PLACEMENT_ID = EntityId.from("33e4b59e-946e-41b7-ac95-7d5559efe73e");
    private static final EntityId DEFINITION_ROOT = EntityId.from("cb40d4a7-ef36-4bd4-9598-fe7180d78a24");
    private static final String CONTENT_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final RuntimeResourceProvider NO_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the test defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Composes a runtime entity through import lookup, mixed-source validation, and placement expansion. */
    @Test
    void composesImportedDefinitionPlacement() throws IOException {
        WorldDefinition authoredWorld = new WorldDefinition(
                WORLD_ID,
                "Imported world",
                List.of(new EntityPlacement(PLACEMENT_ID, true, AssetRef.to(DEFINITION_ID), Map.of())));
        DefinitionWriter.write(temporaryDirectory.resolve("imported.world.json"), authoredWorld);
        AssetCatalog authored = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        EntityDefinition generated = new EntityDefinition(
                DEFINITION_ID,
                "Imported entity",
                new LocalEntity(DEFINITION_ROOT, "Generated root", true, List.of(), List.of()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DefinitionWriter.write(output, generated);
        ImportDefinition importDefinition = importDefinition();
        ImportedArtifactLookup artifacts = lookup(output.toByteArray());
        DefinitionResolver definitions =
                ImportedDefinitionResolver.create(authored, List.of(importDefinition), artifacts);

        WorldCompositionResult result = WorldComposer.compose(
                definitions,
                AssetRef.to(WORLD_ID),
                RegisteredTypeCatalog.of(List.of()),
                List.of(),
                List.of(),
                NO_RESOURCES);

        assertThat(result.diagnostics()).isEmpty();
        World world = result.world().orElseThrow();
        assertThat(world.roots()).singleElement().satisfies(entity -> {
            assertThat(entity.authoredId()).isEqualTo(PLACEMENT_ID);
            assertThat(entity.name()).contains("Generated root");
        });
        world.close();
    }

    /** Creates the import definition owning the generated artifact. */
    private ImportDefinition importDefinition() {
        GameProject.AssetSource source = new GameProject.AssetSource(
                "model", "io.github.glynch.example/model", temporaryDirectory.resolve("model.gltf"), Optional.empty());
        return new ImportDefinition(
                temporaryDirectory.resolve("model.import.json"),
                "model-import",
                source,
                "io.github.glynch.example/model-importer",
                List.of(),
                Map.of(),
                Map.of());
    }

    /** Creates an in-memory published-artifact lookup with one generated definition. */
    private static ImportedArtifactLookup lookup(byte[] content) {
        ImportArtifactDescriptor descriptor =
                ImportArtifactDescriptor.entityDefinition("definitions/main", DEFINITION_ID, List.of());
        ImportedArtifactMetadata metadata = new ImportedArtifactMetadata(descriptor, CONTENT_HASH, content.length);
        return new ImportedArtifactLookup() {
            @Override
            public List<ImportedArtifactMetadata> artifacts(ImportDefinition definition) {
                return List.of(metadata);
            }

            @Override
            public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
                return descriptor.identity().equals(identity)
                        ? Optional.of(new MemoryArtifact(metadata, content))
                        : Optional.empty();
            }
        };
    }

    /** Minimal owned imported-artifact handle for the public integration boundary. */
    private static final class MemoryArtifact implements ImportedArtifact {
        private final ImportedArtifactMetadata metadata;
        private final byte[] content;
        private boolean closed;

        /** Stores immutable test content. */
        private MemoryArtifact(ImportedArtifactMetadata metadata, byte[] content) {
            this.metadata = metadata;
            this.content = content.clone();
        }

        @Override
        public ImportedArtifactMetadata metadata() {
            requireOpen();
            return metadata;
        }

        @Override
        public InputStream openStream() {
            requireOpen();
            return new ByteArrayInputStream(content);
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }

        /** Rejects access after ownership release. */
        private void requireOpen() {
            if (closed) {
                throw new IllegalStateException("artifact is closed");
            }
        }
    }
}
