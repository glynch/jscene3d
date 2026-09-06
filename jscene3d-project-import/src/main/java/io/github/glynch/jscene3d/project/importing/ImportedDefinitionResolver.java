/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionResolvers;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Publishes authored and generated entity definitions through one stable-identity resolver. */
public final class ImportedDefinitionResolver {
    /** Prevents construction of this stateless integration entry point. */
    private ImportedDefinitionResolver() {
        throw new AssertionError("ImportedDefinitionResolver cannot be instantiated");
    }

    /**
     * Creates an immutable resolver from authored assets and active imported generations.
     *
     * <p>Only generated entity-definition artifacts participate. Each artifact descriptor supplies the authoritative
     * project-wide identity; the serialized document must declare the same identity when it is later loaded. The
     * artifact bytes are copied while this method runs, so the returned resolver has no cache handles to close.
     *
     * @param authored authored definition catalog
     * @param imports import definitions whose active generations should participate
     * @param artifacts published-artifact lookup
     * @return immutable mixed-source definition resolver
     * @throws IOException when published content cannot be read
     * @throws IllegalArgumentException when definition identities collide
     * @throws IllegalStateException when published metadata has no corresponding readable artifact
     */
    public static DefinitionResolver create(
            AssetCatalog authored, Collection<ImportDefinition> imports, ImportedArtifactLookup artifacts)
            throws IOException {
        DefinitionResolvers.Builder definitions =
                DefinitionResolvers.builder(Objects.requireNonNull(authored, "authored"));
        ImportedArtifactLookup validArtifacts = Objects.requireNonNull(artifacts, "artifacts");
        for (ImportDefinition definition : List.copyOf(imports)) {
            addDefinitions(definitions, Objects.requireNonNull(definition, "imports entry"), validArtifacts);
        }
        return definitions.build();
    }

    /** Adds every generated definition from one active imported generation. */
    private static void addDefinitions(
            DefinitionResolvers.Builder definitions, ImportDefinition definition, ImportedArtifactLookup artifacts)
            throws IOException {
        for (ImportedArtifactMetadata metadata : artifacts.artifacts(definition)) {
            ImportArtifactDescriptor descriptor = metadata.descriptor();
            if (descriptor.kind() == ImportArtifactKind.ENTITY_DEFINITION) {
                addDefinition(definitions, definition, descriptor, artifacts);
            }
        }
    }

    /** Copies one published generated definition into the resolver builder. */
    private static void addDefinition(
            DefinitionResolvers.Builder definitions,
            ImportDefinition definition,
            ImportArtifactDescriptor descriptor,
            ImportedArtifactLookup artifacts)
            throws IOException {
        AssetId id = descriptor.assetId().orElseThrow();
        ImportedArtifact artifact = artifacts
                .openArtifact(definition, descriptor.identity())
                .orElseThrow(() -> new IllegalStateException(
                        "published artifact metadata has no content: " + descriptor.identity()));
        try (artifact;
                InputStream input = artifact.openStream()) {
            if (!artifact.metadata().descriptor().equals(descriptor)) {
                throw new IllegalStateException(
                        "published artifact changed while definitions were being resolved: " + descriptor.identity());
            }
            definitions.addGeneratedEntity(id, logicalSource(definition, descriptor), input);
        }
    }

    /** Creates a stable non-file source URI for imported-definition diagnostics. */
    private static URI logicalSource(ImportDefinition definition, ImportArtifactDescriptor descriptor) {
        return URI.create("import:" + definition.id() + '/' + descriptor.identity());
    }
}
