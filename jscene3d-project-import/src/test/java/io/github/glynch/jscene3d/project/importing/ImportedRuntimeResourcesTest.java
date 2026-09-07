/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceWriter;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises runtime resource dispatch across project, asset, and imported namespaces. */
final class ImportedRuntimeResourcesTest {
    private static final RegisteredType TYPE = new RegisteredType("example.runtime-resource/data", 1);
    private static final RegisteredType OTHER_TYPE = new RegisteredType("example.runtime-resource/other", 1);
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {"id": "example.runtime-resource", "name": "Resources", "version": "1.0.0"},
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {"applicationExtension": "example.runtime-resource", "entryScene": "main.world.json"},
              "extensions": [{"id": "example.runtime-resource", "requires": "1.0.0"}],
              "assets": [
                {"id": "resource-doc", "type": "example.runtime-resource/source", "path": "assets/resource.json"},
                {"id": "source-payload", "type": "example.runtime-resource/source", "path": "assets/payload.bin"}
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject project;
    private ImportDefinition importDefinition;

    /** Creates files addressable through both authored namespaces. */
    @BeforeEach
    void createProject() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("assets"));
        Files.writeString(temporaryDirectory.resolve(ProjectLoader.MANIFEST_NAME), MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("assets/payload.bin"), "asset payload", StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("payload.bin"), "project payload", StandardCharsets.UTF_8);
        ResourceWriter.write(
                temporaryDirectory.resolve("assets/resource.json"),
                TYPE,
                Map.of("payload", new ProjectValue.ReferenceValue(projectReference("payload.bin"))));
        ResourceWriter.write(
                temporaryDirectory.resolve("project.resource.json"),
                TYPE,
                Map.of("payload", new ProjectValue.ReferenceValue(ResourceReference.asset("source-payload"))));
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
        importDefinition = new ImportDefinition(
                temporaryDirectory.resolve("model.import.json"),
                "model-import",
                project.assets().getFirst(),
                "example.runtime-resource/importer",
                List.of(),
                Map.of(),
                Map.of());
    }

    /** Resolves nested content and transfers value ownership for every portable namespace. */
    @Test
    void acquiresProjectAssetAndImportedResources() throws IOException {
        Map<String, Published> publications = new LinkedHashMap<>();
        publications.put(
                "resource",
                published(
                        ImportArtifactDescriptor.resource("resource", TYPE, List.of("payload")),
                        resourceBytes(ResourceReference.imported("model-import/payload"))));
        publications.put(
                "payload",
                published(
                        ImportArtifactDescriptor.payload("payload", "application/octet-stream"),
                        bytes("import payload")));
        RuntimeResourceProvider provider = provider(new MemoryLookup(publications), List.of(new TestLoader()));

        assertResource(
                provider,
                ResourceReference.project(
                        "project.resource.json", project.root().resolve("project.resource.json")),
                "asset payload");
        assertResource(provider, ResourceReference.asset("resource-doc"), "project payload");
        assertResource(provider, ResourceReference.imported("model-import/resource"), "import payload");
    }

    /** Resolves authored resources without requiring callers to construct unused import infrastructure. */
    @Test
    void createsAuthoredOnlyProvider() {
        RuntimeResourceProvider provider =
                ImportedRuntimeResources.create(project, catalog(), List.of(new TestLoader()));

        assertResource(provider, ResourceReference.asset("resource-doc"), "project payload");
    }

    /** Rejects requested Java types and publication metadata that disagree with serialized content. */
    @Test
    void rejectsTypeMismatchesAndClosesRejectedValues() throws IOException {
        TestLoader loader = new TestLoader();
        MemoryLookup valid = lookup(
                ImportArtifactDescriptor.resource("resource", TYPE, List.of()),
                resourceBytes(ResourceReference.imported("model-import/payload")));
        valid.add("payload", ImportArtifactDescriptor.payload("payload", "application/octet-stream"), bytes("value"));
        RuntimeResourceProvider provider = provider(valid, List.of(loader));

        assertThatIllegalStateException()
                .isThrownBy(() -> provider.acquire(ResourceReference.imported("model-import/resource"), String.class));
        assertThat(loader.last()).get().extracting(TestResource::isClosed).isEqualTo(true);

        MemoryLookup wrongKind = lookup(
                ImportArtifactDescriptor.payload("resource", "application/json"),
                resourceBytes(ResourceReference.imported("model-import/payload")));
        assertThatIllegalStateException()
                .isThrownBy(() -> provider(wrongKind, List.of(new TestLoader()))
                        .acquire(ResourceReference.imported("model-import/resource"), TestResource.class))
                .withMessageContaining("does not identify a resource");

        MemoryLookup wrongType = lookup(
                ImportArtifactDescriptor.resource("resource", OTHER_TYPE, List.of()),
                resourceBytes(ResourceReference.imported("model-import/payload")));
        assertThatIllegalStateException()
                .isThrownBy(() -> provider(wrongType, List.of(new TestLoader()))
                        .acquire(ResourceReference.imported("model-import/resource"), TestResource.class))
                .withMessageContaining("does not match");
    }

    /** Reports missing definitions, content, loaders, and invalid serialized resources at acquisition. */
    @Test
    void reportsUnavailableRuntimeResources() throws IOException {
        RuntimeResourceProvider empty = provider(new MemoryLookup(Map.of()), List.of(new TestLoader()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> empty.acquire(ResourceReference.imported("unknown/resource"), TestResource.class))
                .withMessageContaining("unknown import");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> empty.acquire(ResourceReference.imported("model-import/missing"), TestResource.class))
                .withMessageContaining("unknown imported artifact");

        MemoryLookup malformed =
                lookup(ImportArtifactDescriptor.resource("resource", TYPE, List.of()), bytes("not json"));
        assertThatIllegalStateException()
                .isThrownBy(() -> provider(malformed, List.of(new TestLoader()))
                        .acquire(ResourceReference.imported("model-import/resource"), TestResource.class))
                .withMessageContaining("invalid runtime resource");

        MemoryLookup valid = lookup(
                ImportArtifactDescriptor.resource("resource", TYPE, List.of()),
                resourceBytes(ResourceReference.imported("model-import/payload")));
        assertThatIllegalStateException()
                .isThrownBy(() -> provider(valid, List.of())
                        .acquire(ResourceReference.imported("model-import/resource"), TestResource.class))
                .withMessageContaining("no runtime resource loader");
    }

    /** Rejects ambiguous or descriptor-inconsistent provider registration eagerly. */
    @Test
    void validatesProviderRegistration() {
        ImportDefinition duplicate = new ImportDefinition(
                temporaryDirectory.resolve("duplicate.import.json"),
                importDefinition.id(),
                importDefinition.asset(),
                importDefinition.importer(),
                List.of(),
                Map.of(),
                Map.of());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImportedRuntimeResources.create(
                        project,
                        catalog(),
                        List.of(importDefinition, duplicate),
                        new MemoryLookup(Map.of()),
                        List.of(new TestLoader())))
                .withMessageContaining("duplicate import");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider(new MemoryLookup(Map.of()), List.of(new TestLoader(), new TestLoader())))
                .withMessageContaining("duplicate runtime resource loader");
        RuntimeResourceLoader<TestResource> unknown = new TestLoader() {
            @Override
            public RegisteredType type() {
                return new RegisteredType("example.runtime-resource/unknown", 1);
            }
        };
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider(new MemoryLookup(Map.of()), List.of(unknown)))
                .withMessageContaining("has no descriptor");
    }

    /** Acquires and closes one expected test resource. */
    private static void assertResource(RuntimeResourceProvider provider, ResourceReference reference, String content) {
        TestResource value;
        try (RuntimeResourceLease<TestResource> lease = provider.acquire(reference, TestResource.class)) {
            value = lease.value();
            assertThat(value.content()).isEqualTo(content);
            assertThat(value.isClosed()).isFalse();
        }
        assertThat(value.isClosed()).isTrue();
    }

    /** Creates the provider under test. */
    private RuntimeResourceProvider provider(ImportedArtifactLookup artifacts, List<RuntimeResourceLoader<?>> loaders) {
        return ImportedRuntimeResources.create(project, catalog(), List.of(importDefinition), artifacts, loaders);
    }

    /** Builds safe metadata for the two resource identities used by tests. */
    private static RegisteredTypeCatalog catalog() {
        return RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                "example.runtime-resource",
                "1.0.0",
                ">=0.1.0-SNAPSHOT <0.2.0",
                DescriptorPresentation.named("Test resources"),
                List.of(resourceDescriptor(TYPE), resourceDescriptor(OTHER_TYPE)))));
    }

    /** Describes one resource with a required payload reference. */
    private static RegisteredTypeDescriptor resourceDescriptor(RegisteredType type) {
        return new RegisteredTypeDescriptor(
                type,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.named(type.id()),
                List.of(PropertyDescriptor.required(
                        "payload",
                        ProjectValueKind.REFERENCE,
                        DescriptorPresentation.named("Payload"),
                        Map.of(),
                        Set.of())),
                List.of(),
                List.of(),
                List.of());
    }

    /** Creates one resolved project-file reference. */
    private ResourceReference projectReference(String locator) {
        return ResourceReference.project(
                locator, temporaryDirectory.resolve(locator).toAbsolutePath().normalize());
    }

    /** Serializes one imported resource document. */
    private static byte[] resourceBytes(ResourceReference payload) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ResourceWriter.write(output, TYPE, Map.of("payload", new ProjectValue.ReferenceValue(payload)));
        return output.toByteArray();
    }

    /** Encodes fixture text. */
    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /** Creates one lookup containing an initial publication. */
    private static MemoryLookup lookup(ImportArtifactDescriptor descriptor, byte[] content) {
        MemoryLookup lookup = new MemoryLookup(new LinkedHashMap<>());
        lookup.add(descriptor.identity(), descriptor, content);
        return lookup;
    }

    /** Creates one immutable publication value. */
    private static Published published(ImportArtifactDescriptor descriptor, byte[] content) {
        return new Published(descriptor, content);
    }

    /** Loader which returns the complete referenced payload as its owned value. */
    private static class TestLoader implements RuntimeResourceLoader<TestResource> {
        private Optional<TestResource> last = Optional.empty();

        @Override
        public RegisteredType type() {
            return TYPE;
        }

        @Override
        public Class<TestResource> valueType() {
            return TestResource.class;
        }

        @Override
        public TestResource load(ResourceDefinition definition, ResourceContent content) throws IOException {
            ProjectValue.ReferenceValue payload = (ProjectValue.ReferenceValue)
                    Objects.requireNonNull(definition.properties().get("payload"));
            try (InputStream input = content.openPayload(payload.reference())) {
                TestResource value = new TestResource(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                last = Optional.of(value);
                return value;
            }
        }

        private Optional<TestResource> last() {
            return last;
        }
    }

    /** Minimal owned runtime value. */
    private static final class TestResource implements AutoCloseable {
        private final String content;
        private boolean closed;

        private TestResource(String content) {
            this.content = content;
        }

        private String content() {
            return content;
        }

        private boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** In-memory imported publication lookup. */
    private static final class MemoryLookup implements ImportedArtifactLookup {
        private final Map<String, Published> publications;

        private MemoryLookup(Map<String, Published> publications) {
            this.publications = new LinkedHashMap<>(publications);
        }

        private void add(String identity, ImportArtifactDescriptor descriptor, byte[] content) {
            publications.put(identity, published(descriptor, content));
        }

        @Override
        public List<ImportedArtifactMetadata> artifacts(ImportDefinition definition) {
            return publications.values().stream().map(Published::metadata).toList();
        }

        @Override
        public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
            return Optional.ofNullable(publications.get(identity)).map(MemoryArtifact::new);
        }
    }

    /** Immutable published bytes with intentional identity equality and derived metadata. */
    private static final class Published {
        private final ImportArtifactDescriptor descriptor;
        private final byte[] content;

        private Published(ImportArtifactDescriptor descriptor, byte[] content) {
            this.descriptor = descriptor;
            this.content = content.clone();
        }

        private ImportedArtifactMetadata metadata() {
            return new ImportedArtifactMetadata(descriptor, HASH, content.length);
        }

        private byte[] content() {
            return content.clone();
        }
    }

    /** One independently closeable artifact generation handle. */
    private static final class MemoryArtifact implements ImportedArtifact {
        private final Published publication;
        private boolean closed;

        private MemoryArtifact(Published publication) {
            this.publication = publication;
        }

        @Override
        public ImportedArtifactMetadata metadata() {
            requireOpen();
            return publication.metadata();
        }

        @Override
        public InputStream openStream() {
            requireOpen();
            return new ByteArrayInputStream(publication.content());
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }

        private void requireOpen() {
            if (closed) {
                throw new IllegalStateException("artifact is closed");
            }
        }
    }
}
