/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceLoadResult;
import io.github.glynch.jscene3d.project.resource.ResourceLoader;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Creates runtime-resource providers over validated project files and published import artifacts. */
public final class ImportedRuntimeResources {
    /** Prevents construction of this stateless integration entry point. */
    private ImportedRuntimeResources() {
        throw new AssertionError("ImportedRuntimeResources cannot be instantiated");
    }

    /**
     * Creates a provider that loads and owns one runtime value per acquired lease.
     *
     * <p>The supplied project, catalog, imports, artifact lookup, and loaders remain caller-owned. Acquisition performs
     * synchronous I/O. The returned provider creates no threads and retains no values after their leases close.
     *
     * @param project containing validated project
     * @param catalog resolved safe type metadata
     * @param imports import definitions addressable by imported references
     * @param artifacts published imported content
     * @param loaders runtime loaders for supported resource types
     * @return synchronous runtime-resource provider
     */
    public static RuntimeResourceProvider create(
            GameProject project,
            RegisteredTypeCatalog catalog,
            Collection<ImportDefinition> imports,
            ImportedArtifactLookup artifacts,
            Collection<RuntimeResourceLoader<?>> loaders) {
        return new Provider(project, catalog, imports, artifacts, loaders);
    }

    /** Deep provider implementation hiding definition loading, content namespaces, ownership, and type dispatch. */
    private static final class Provider implements RuntimeResourceProvider, ResourceContent {
        private final GameProject project;
        private final RegisteredTypeCatalog catalog;
        private final ImportedArtifactLookup artifacts;
        private final Map<String, ImportDefinition> imports;
        private final Map<String, GameProject.AssetSource> assets;
        private final Map<RegisteredType, RuntimeResourceLoader<?>> loaders;
        private final ResourceLoader resourceLoader = new ResourceLoader();

        /** Indexes all caller-owned dependencies after validating ambiguity and registered scopes. */
        private Provider(
                GameProject project,
                RegisteredTypeCatalog catalog,
                Collection<ImportDefinition> imports,
                ImportedArtifactLookup artifacts,
                Collection<RuntimeResourceLoader<?>> loaders) {
            this.project = Objects.requireNonNull(project, "project");
            this.catalog = Objects.requireNonNull(catalog, "catalog");
            this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
            this.imports = indexImports(imports);
            assets = indexAssets(project.assets());
            this.loaders = indexLoaders(catalog, loaders);
        }

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            ResourceReference validReference = Objects.requireNonNull(reference, "reference");
            Class<T> validValueType = Objects.requireNonNull(valueType, "valueType");
            try {
                LoadedDefinition loaded = loadDefinition(validReference);
                RuntimeResourceLoader<?> loader = requireLoader(loaded.definition());
                requireDescriptorType(loaded.descriptor(), loaded.definition());
                AutoCloseable value =
                        Objects.requireNonNull(loader.load(loaded.definition(), this), "runtime resource loader value");
                AutoCloseable loaderValue = castOrClose(
                        value,
                        loader.valueType(),
                        "runtime resource loader returned the wrong value type for " + loader.type());
                T typedValue = castOrClose(
                        loaderValue,
                        validValueType,
                        "runtime resource has the wrong requested value type for " + validReference);
                return RuntimeResourceLease.of(typedValue, () -> closeValue(loaderValue));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to acquire runtime resource " + validReference, exception);
            }
        }

        @Override
        public InputStream openPayload(ResourceReference reference) throws IOException {
            ResourceReference validReference = Objects.requireNonNull(reference, "reference");
            return switch (validReference.kind()) {
                case PROJECT ->
                    Files.newInputStream(validReference.projectPath().orElseThrow());
                case ASSET ->
                    Files.newInputStream(requireAsset(validReference.locator()).path());
                case IMPORT -> openImported(validReference).stream();
            };
        }

        /** Loads one native resource document and preserves imported descriptor metadata for cross-checking. */
        private LoadedDefinition loadDefinition(ResourceReference reference) throws IOException {
            if (reference.kind() == ResourceReference.Kind.PROJECT) {
                ResourceLoadResult result =
                        resourceLoader.load(project, reference.projectPath().orElseThrow());
                return validated(result, Optional.empty(), reference);
            }
            if (reference.kind() == ResourceReference.Kind.ASSET) {
                ResourceLoadResult result = resourceLoader.load(
                        project, requireAsset(reference.locator()).path());
                return validated(result, Optional.empty(), reference);
            }
            try (OpenedArtifact opened = openImported(reference)) {
                ResourceLoadResult result = resourceLoader.load(project, opened.source(), opened.stream());
                return validated(result, Optional.of(opened.descriptor()), reference);
            }
        }

        /** Requires structural and catalog validation before runtime construction. */
        private LoadedDefinition validated(
                ResourceLoadResult result, Optional<ImportArtifactDescriptor> descriptor, ResourceReference reference) {
            ResourceDefinition definition =
                    result.resource().orElseThrow(() -> invalid(reference, result.diagnostics()));
            List<ProjectDiagnostic> diagnostics = catalog.validate(definition);
            if (diagnostics.stream()
                    .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)) {
                throw invalid(reference, diagnostics);
            }
            return new LoadedDefinition(definition, descriptor);
        }

        /** Requires one registered runtime loader for the validated resource type. */
        private RuntimeResourceLoader<?> requireLoader(ResourceDefinition definition) {
            RuntimeResourceLoader<?> loader = loaders.get(definition.type());
            if (loader == null) {
                throw new IllegalStateException("no runtime resource loader is registered for " + definition.type());
            }
            return loader;
        }

        /** Requires imported publication metadata to agree with serialized resource content. */
        private static void requireDescriptorType(
                Optional<ImportArtifactDescriptor> descriptor, ResourceDefinition definition) {
            if (descriptor.isEmpty()) {
                return;
            }
            ImportArtifactDescriptor published = descriptor.orElseThrow();
            if (published.kind() != ImportArtifactKind.RESOURCE) {
                throw new IllegalStateException(
                        "imported reference does not identify a resource artifact: " + published.identity());
            }
            RegisteredType publishedType = published.resourceType().orElseThrow();
            if (!publishedType.equals(definition.type())) {
                throw new IllegalStateException("published resource type does not match its content: " + publishedType
                        + " != " + definition.type());
            }
        }

        /** Opens one imported artifact while coupling stream and generation-handle ownership. */
        private OpenedArtifact openImported(ResourceReference reference) throws IOException {
            ImportedLocator locator = ImportedLocator.from(reference.locator());
            ImportDefinition definition = imports.get(locator.importId());
            if (definition == null) {
                throw new IllegalArgumentException("unknown import reference " + reference);
            }
            ImportedArtifact artifact = artifacts
                    .openArtifact(definition, locator.artifactIdentity())
                    .orElseThrow(() -> new IllegalArgumentException("unknown imported artifact " + reference));
            try {
                ImportArtifactDescriptor descriptor = artifact.metadata().descriptor();
                InputStream stream = artifact.openStream();
                return new OpenedArtifact(URI.create(reference.toString()), descriptor, stream, artifact);
            } catch (IOException | RuntimeException failure) {
                artifact.close();
                throw failure;
            }
        }

        /** Finds one manifest source asset by project-local identity. */
        private GameProject.AssetSource requireAsset(String id) {
            GameProject.AssetSource asset = assets.get(id);
            if (asset == null) {
                throw new IllegalArgumentException("unknown source asset " + id);
            }
            return asset;
        }

        /** Indexes unique import definitions by project-local identity. */
        private static Map<String, ImportDefinition> indexImports(Collection<ImportDefinition> definitions) {
            Map<String, ImportDefinition> indexed = new LinkedHashMap<>();
            for (ImportDefinition definition : List.copyOf(Objects.requireNonNull(definitions, "imports"))) {
                ImportDefinition validDefinition = Objects.requireNonNull(definition, "imports entry");
                if (indexed.putIfAbsent(validDefinition.id(), validDefinition) != null) {
                    throw new IllegalArgumentException("duplicate import identity " + validDefinition.id());
                }
            }
            return Map.copyOf(indexed);
        }

        /** Indexes unique source assets by project-local identity. */
        private static Map<String, GameProject.AssetSource> indexAssets(List<GameProject.AssetSource> sources) {
            Map<String, GameProject.AssetSource> indexed = new LinkedHashMap<>();
            for (GameProject.AssetSource source : sources) {
                if (indexed.putIfAbsent(source.id(), source) != null) {
                    throw new IllegalArgumentException("duplicate source asset identity " + source.id());
                }
            }
            return Map.copyOf(indexed);
        }

        /** Indexes unique loaders and requires matching registered resource descriptors. */
        private static Map<RegisteredType, RuntimeResourceLoader<?>> indexLoaders(
                RegisteredTypeCatalog catalog, Collection<RuntimeResourceLoader<?>> supplied) {
            Map<RegisteredType, RuntimeResourceLoader<?>> indexed = new LinkedHashMap<>();
            for (RuntimeResourceLoader<?> loader : List.copyOf(Objects.requireNonNull(supplied, "loaders"))) {
                RuntimeResourceLoader<?> validLoader = Objects.requireNonNull(loader, "loaders entry");
                RegisteredTypeDescriptor descriptor = catalog.find(validLoader.type())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "runtime resource loader type has no descriptor: " + validLoader.type()));
                if (descriptor.scope() != RegisteredTypeScope.RESOURCE) {
                    throw new IllegalArgumentException(
                            "runtime resource loader type is not a resource: " + validLoader.type());
                }
                Objects.requireNonNull(validLoader.valueType(), "runtime resource loader value type");
                if (indexed.putIfAbsent(validLoader.type(), validLoader) != null) {
                    throw new IllegalArgumentException("duplicate runtime resource loader " + validLoader.type());
                }
            }
            return Map.copyOf(indexed);
        }

        /** Creates an actionable failure from the first available structured diagnostic. */
        private static IllegalStateException invalid(ResourceReference reference, List<ProjectDiagnostic> diagnostics) {
            String detail = diagnostics.isEmpty()
                    ? "resource definition is unavailable"
                    : diagnostics.get(0).code().code() + " at "
                            + diagnostics.get(0).location();
            return new IllegalStateException("invalid runtime resource " + reference + ": " + detail);
        }

        /** Casts an owned value or closes it before reporting a violated type contract. */
        private static <T> T castOrClose(AutoCloseable value, Class<T> expectedType, String failureMessage) {
            try {
                return expectedType.cast(value);
            } catch (ClassCastException failure) {
                closeRejected(value, failure);
                throw new IllegalStateException(failureMessage, failure);
            }
        }

        /** Closes a value rejected after construction and preserves its original failure. */
        private static void closeRejected(AutoCloseable value, RuntimeException failure) {
            try {
                value.close();
            } catch (Exception closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }

        /** Closes one lease-owned value without leaking a checked close failure through Runnable. */
        private static void closeValue(AutoCloseable value) {
            try {
                value.close();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to close runtime resource", exception);
            }
        }
    }

    /** Validated resource plus optional authoritative imported publication metadata. */
    private record LoadedDefinition(ResourceDefinition definition, Optional<ImportArtifactDescriptor> descriptor) {}

    /** Parsed imported-output locator. */
    private record ImportedLocator(String importId, String artifactIdentity) {
        /** Splits a validated imported reference at its first path separator. */
        private static ImportedLocator from(String locator) {
            int separator = locator.indexOf('/');
            return new ImportedLocator(locator.substring(0, separator), locator.substring(separator + 1));
        }
    }

    /** Owns one imported artifact and its open stream as a single closeable value. */
    private static final class OpenedArtifact implements AutoCloseable {
        private final URI source;
        private final ImportArtifactDescriptor descriptor;
        private final InputStream stream;

        /** Stores open resources after successful acquisition. */
        private OpenedArtifact(
                URI source, ImportArtifactDescriptor descriptor, InputStream stream, ImportedArtifact artifact) {
            this.source = source;
            this.descriptor = descriptor;
            this.stream = new ArtifactStream(stream, artifact);
        }

        /** Returns the logical content source. */
        private URI source() {
            return source;
        }

        /** Returns authoritative publication metadata. */
        private ImportArtifactDescriptor descriptor() {
            return descriptor;
        }

        /** Returns the stream closed together with this owner. */
        private InputStream stream() {
            return stream;
        }

        /** Releases the content stream and retained generation handle. */
        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    /** Couples a payload stream's lifetime to its retained import-generation handle. */
    private static final class ArtifactStream extends FilterInputStream {
        private final ImportedArtifact artifact;

        /** Stores the stream and its owning artifact handle. */
        private ArtifactStream(InputStream stream, ImportedArtifact artifact) {
            super(stream);
            this.artifact = artifact;
        }

        /** Delegates bulk reads directly to the retained artifact stream. */
        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return in.read(buffer, offset, length);
        }

        /** Closes the stream before releasing the generation handle. */
        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                artifact.close();
            }
        }
    }
}
