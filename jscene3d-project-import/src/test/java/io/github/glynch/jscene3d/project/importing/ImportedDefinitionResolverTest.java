/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the boundary joining published import artifacts to stable definition resolution. */
final class ImportedDefinitionResolverTest {
    private static final AssetId DEFINITION_ID = AssetId.from("4e230064-13e5-4ad2-bba8-8fc7dbf4ab32");
    private static final AssetId OTHER_ID = AssetId.from("ade45bdd-4251-4450-a301-efc548ff8992");
    private static final EntityId ROOT_ID = EntityId.from("cb40d4a7-ef36-4bd4-9598-fe7180d78a24");
    private static final String CONTENT_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @TempDir
    private Path temporaryDirectory;

    /** Copies published bytes, preserves their logical source, and resolves their declared identity. */
    @Test
    void resolvesPublishedDefinition() throws IOException {
        byte[] content = serialized(definition(DEFINITION_ID));
        ImportDefinition owner = importDefinition("first-import");
        TestArtifactLookup artifacts = TestArtifactLookup.published(descriptor(DEFINITION_ID), content);

        DefinitionResolver resolver = ImportedDefinitionResolver.create(authored(), List.of(owner), artifacts);
        DefinitionLoadResult<EntityDefinition> result =
                resolver.loadEntity(AssetRef.to(DEFINITION_ID), RegisteredTypeCatalog.of(List.of()));

        assertThat(result.definition()).contains(definition(DEFINITION_ID));
        assertThat(result.source()).isEqualTo(URI.create("import:first-import/definitions/main"));
        assertThat(artifacts.lastArtifact())
                .get()
                .extracting(ImportedArtifact::isClosed)
                .isEqualTo(true);
    }

    /** Leaves unpublished imports absent from the mixed-source resolver. */
    @Test
    void ignoresImportWithoutPublishedGeneration() throws IOException {
        DefinitionResolver resolver = ImportedDefinitionResolver.create(
                authored(), List.of(importDefinition("empty-import")), TestArtifactLookup.empty());

        DefinitionLoadResult<EntityDefinition> result =
                resolver.loadEntity(AssetRef.to(DEFINITION_ID), RegisteredTypeCatalog.of(List.of()));

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("asset.reference.missing");
    }

    /** Rejects cache metadata that cannot be opened as corresponding content. */
    @Test
    void rejectsMissingPublishedContent() {
        TestArtifactLookup artifacts = TestArtifactLookup.missing(descriptor(DEFINITION_ID));

        assertThatIllegalStateException()
                .isThrownBy(() -> ImportedDefinitionResolver.create(
                        authored(), List.of(importDefinition("missing-import")), artifacts))
                .withMessageContaining("has no content");
    }

    /** Rejects a generation switch observed between metadata listing and content opening. */
    @Test
    void rejectsChangedPublishedArtifact() throws IOException {
        TestArtifactLookup artifacts = TestArtifactLookup.changed(
                descriptor(DEFINITION_ID), descriptor(OTHER_ID), serialized(definition(OTHER_ID)));

        assertThatIllegalStateException()
                .isThrownBy(() -> ImportedDefinitionResolver.create(
                        authored(), List.of(importDefinition("changing-import")), artifacts))
                .withMessageContaining("changed while definitions were being resolved");
    }

    /** Rejects a generated identity that collides with an authored definition. */
    @Test
    void rejectsAuthoredIdentityCollision() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("authored.entity.json"), definition(DEFINITION_ID));
        byte[] content = serialized(definition(DEFINITION_ID));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportedDefinitionResolver.create(
                        authored(),
                        List.of(importDefinition("colliding-import")),
                        TestArtifactLookup.published(descriptor(DEFINITION_ID), content)))
                .withMessageContaining("already present");
    }

    /** Scans the authored side of the mixed-source resolver. */
    private AssetCatalog authored() {
        return AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
    }

    /** Creates one structurally valid import owner. */
    private ImportDefinition importDefinition(String id) {
        GameProject.AssetSource source = new GameProject.AssetSource(
                "model",
                "io.github.glynch.import-test/model",
                temporaryDirectory.resolve("model.gltf"),
                Optional.empty());
        return new ImportDefinition(
                temporaryDirectory.resolve(id + ".import.json"),
                id,
                source,
                "io.github.glynch.import-test/model-importer",
                List.of(),
                Map.of(),
                Map.of());
    }

    /** Creates one generated entity-definition descriptor. */
    private static ImportArtifactDescriptor descriptor(AssetId id) {
        return ImportArtifactDescriptor.entityDefinition("definitions/main", id, List.of());
    }

    /** Creates one minimal reusable definition. */
    private static EntityDefinition definition(AssetId id) {
        return new EntityDefinition(
                id, "Imported entity", new LocalEntity(ROOT_ID, "Imported root", true, List.of(), List.of()));
    }

    /** Serializes one definition through the public canonical writer. */
    private static byte[] serialized(EntityDefinition definition) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DefinitionWriter.write(output, definition);
        return output.toByteArray();
    }

    /** Configurable imported-artifact lookup for resolver boundary cases. */
    private static final class TestArtifactLookup implements ImportedArtifactLookup {
        private final List<ImportedArtifactMetadata> listed;
        private final Optional<ImportArtifactDescriptor> openedDescriptor;
        private final byte[] content;
        private Optional<ImportedArtifact> lastArtifact = Optional.empty();

        /** Stores one immutable lookup scenario. */
        private TestArtifactLookup(
                List<ImportedArtifactMetadata> listed,
                Optional<ImportArtifactDescriptor> openedDescriptor,
                byte[] content) {
            this.listed = List.copyOf(listed);
            this.openedDescriptor = openedDescriptor;
            this.content = content.clone();
        }

        /** Creates one successful publication. */
        private static TestArtifactLookup published(ImportArtifactDescriptor descriptor, byte[] content) {
            return new TestArtifactLookup(List.of(metadata(descriptor, content)), Optional.of(descriptor), content);
        }

        /** Creates an import with no active generation. */
        private static TestArtifactLookup empty() {
            return new TestArtifactLookup(List.of(), Optional.empty(), new byte[0]);
        }

        /** Creates indexed metadata with absent content. */
        private static TestArtifactLookup missing(ImportArtifactDescriptor descriptor) {
            return new TestArtifactLookup(List.of(metadata(descriptor, new byte[0])), Optional.empty(), new byte[0]);
        }

        /** Creates a lookup which switches descriptor between listing and opening. */
        private static TestArtifactLookup changed(
                ImportArtifactDescriptor listed, ImportArtifactDescriptor opened, byte[] content) {
            return new TestArtifactLookup(List.of(metadata(listed, content)), Optional.of(opened), content);
        }

        @Override
        public List<ImportedArtifactMetadata> artifacts(ImportDefinition definition) {
            return listed;
        }

        @Override
        public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
            lastArtifact =
                    openedDescriptor.map(descriptor -> new MemoryArtifact(metadata(descriptor, content), content));
            return lastArtifact;
        }

        /** Returns the last opened handle for ownership assertions. */
        private Optional<ImportedArtifact> lastArtifact() {
            return lastArtifact;
        }

        /** Creates syntactically valid metadata for test content. */
        private static ImportedArtifactMetadata metadata(ImportArtifactDescriptor descriptor, byte[] content) {
            return new ImportedArtifactMetadata(descriptor, CONTENT_HASH, content.length);
        }
    }

    /** Minimal owned in-memory artifact handle. */
    private static final class MemoryArtifact implements ImportedArtifact {
        private final ImportedArtifactMetadata metadata;
        private final byte[] content;
        private boolean closed;

        /** Copies immutable handle state. */
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
