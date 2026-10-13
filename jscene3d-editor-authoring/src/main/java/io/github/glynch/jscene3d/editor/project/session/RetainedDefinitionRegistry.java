/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Owns retained definitions, their authoritative working copies, and working-copy subscriptions. */
final class RetainedDefinitionRegistry implements AutoCloseable {
    private final AssetCatalog authoredAssets;
    private final DefinitionResolver definitions;
    private final RegisteredTypeCatalog types;
    private final EditorHierarchyProjector hierarchyProjector;
    private final Consumer<AuthoringDefinitionChange> changeConsumer;
    private final Map<AssetId, AuthoredDefinitionWorkingCopy> workingCopies = new LinkedHashMap<>();
    private final List<AuthoringSubscription> subscriptions = new ArrayList<>();
    private final Map<AssetId, RetainedState> retainedDefinitions = new LinkedHashMap<>();

    RetainedDefinitionRegistry(
            AssetCatalog authoredAssets,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            EditorHierarchyProjector hierarchyProjector,
            Consumer<AuthoringDefinitionChange> changeConsumer) {
        this.authoredAssets = Objects.requireNonNull(authoredAssets, "authoredAssets");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.types = Objects.requireNonNull(types, "types");
        this.hierarchyProjector = Objects.requireNonNull(hierarchyProjector, "hierarchyProjector");
        this.changeConsumer = Objects.requireNonNull(changeConsumer, "changeConsumer");
    }

    DefinitionRetentionResult retain(AssetId id) {
        AssetId validId = Objects.requireNonNull(id, "id");
        RetainedState existing = retainedDefinitions.get(validId);
        if (existing != null) {
            return new DefinitionRetentionResult(Optional.of(snapshot(existing)), List.of());
        }
        Optional<AssetMetadata> authored = authoredAssets.find(validId);
        return authored.isPresent() ? retainAuthored(authored.orElseThrow()) : retainGeneratedEntity(validId);
    }

    List<AssetId> retainedIds() {
        return List.copyOf(retainedDefinitions.keySet());
    }

    Optional<AuthoredDefinitionDocument> authoredDocument(AssetId id) {
        RetainedState retained = retainedDefinitions.get(Objects.requireNonNull(id, "id"));
        return retained instanceof AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy)
                ? Optional.of(workingCopy.current())
                : Optional.empty();
    }

    Optional<AuthoredDefinitionWorkingCopy> workingCopy(AssetId id) {
        return Optional.ofNullable(workingCopies.get(Objects.requireNonNull(id, "id")));
    }

    Optional<AuthoringDefinitionState> definitionState(AssetId id) {
        return workingCopy(id).map(AuthoredDefinitionWorkingCopy::state);
    }

    Optional<EditorRetainedDefinition> snapshot(AssetId id) {
        return Optional.ofNullable(retainedDefinitions.get(Objects.requireNonNull(id, "id")))
                .map(this::snapshot);
    }

    Optional<AssetKind> retainedKind(AssetId id) {
        return Optional.ofNullable(retainedDefinitions.get(Objects.requireNonNull(id, "id")))
                .map(RetainedState::kind);
    }

    boolean isAuthored(AssetId id) {
        return retainedDefinitions.get(Objects.requireNonNull(id, "id")) instanceof AuthoredRetainedState;
    }

    boolean isDirty() {
        return workingCopies.values().stream().anyMatch(AuthoredDefinitionWorkingCopy::isDirty);
    }

    private DefinitionRetentionResult retainAuthored(AssetMetadata metadata) {
        AuthoredDefinitionWorkingCopy workingCopy = workingCopies.get(metadata.id());
        List<ProjectDiagnostic> diagnostics = List.of();
        if (workingCopy == null) {
            AuthoredDefinitionDocument.LoadResult result =
                    AuthoredDefinitionDocument.load(authoredAssets, definitions, types, metadata.id());
            if (result.document().isEmpty()) {
                return new DefinitionRetentionResult(Optional.empty(), result.diagnostics());
            }
            workingCopy = openWorkingCopy(result.document().orElseThrow());
            diagnostics = result.diagnostics();
        }
        RetainedState state = new AuthoredRetainedState(workingCopy);
        retainedDefinitions.put(metadata.id(), state);
        return new DefinitionRetentionResult(Optional.of(snapshot(state)), diagnostics);
    }

    private DefinitionRetentionResult retainGeneratedEntity(AssetId id) {
        DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(id), types);
        return retainLoaded(
                result,
                AssetKind.ENTITY_DEFINITION,
                result.definition().map(EditorRetainedDefinition.Content.Entity::new));
    }

    private DefinitionRetentionResult retainLoaded(
            DefinitionLoadResult<?> result, AssetKind kind, Optional<EditorRetainedDefinition.Content> content) {
        if (content.isEmpty()) {
            return new DefinitionRetentionResult(Optional.empty(), result.diagnostics());
        }
        EditorRetainedDefinition.Content loaded = content.orElseThrow();
        RetainedState state = new GeneratedRetainedState(loaded.id(), kind, result.source(), loaded);
        retainedDefinitions.put(loaded.id(), state);
        return new DefinitionRetentionResult(Optional.of(snapshot(state)), result.diagnostics());
    }

    private AuthoredDefinitionWorkingCopy openWorkingCopy(AuthoredDefinitionDocument document) {
        AuthoredDefinitionWorkingCopy existing = workingCopies.get(document.id());
        if (existing != null) {
            return existing;
        }
        AuthoredDefinitionWorkingCopy workingCopy = new AuthoredDefinitionWorkingCopy(document);
        workingCopies.put(document.id(), workingCopy);
        subscriptions.add(workingCopy.onDidChange().subscribe(changeConsumer));
        return workingCopy;
    }

    private EditorRetainedDefinition snapshot(RetainedState state) {
        EditorRetainedDefinition.Content content;
        long revision;
        boolean editable;
        Set<EntityId> modifiedEntities;
        if (state instanceof AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy)) {
            AuthoringDefinitionState authoredState = workingCopy.state();
            content = authoredState.content();
            revision = authoredState.revision();
            editable = true;
            modifiedEntities = workingCopy.modifiedEntityIds();
        } else {
            GeneratedRetainedState generated = (GeneratedRetainedState) state;
            content = generated.content();
            revision = 0L;
            editable = false;
            modifiedEntities = Set.of();
        }
        EditorHierarchyProjection hierarchy =
                switch (content) {
                    case EditorRetainedDefinition.Content.Scene scene ->
                        hierarchyProjector.project(
                                scene.definition(), state.source(), editable, editable, modifiedEntities);
                    case EditorRetainedDefinition.Content.Entity entity ->
                        hierarchyProjector.project(
                                entity.definition(), state.source(), editable, editable, modifiedEntities);
                };
        return new EditorRetainedDefinition(
                state.id(), state.kind(), state.origin(), editable, state.source(), revision, content, hierarchy);
    }

    @Override
    public void close() {
        subscriptions.forEach(AuthoringSubscription::close);
        workingCopies.values().forEach(AuthoredDefinitionWorkingCopy::close);
        subscriptions.clear();
        workingCopies.clear();
        retainedDefinitions.clear();
    }

    private sealed interface RetainedState permits AuthoredRetainedState, GeneratedRetainedState {
        AssetId id();

        AssetKind kind();

        EditorRetainedDefinition.Origin origin();

        URI source();
    }

    private record AuthoredRetainedState(AuthoredDefinitionWorkingCopy workingCopy) implements RetainedState {
        private AuthoredRetainedState {
            Objects.requireNonNull(workingCopy, "workingCopy");
        }

        @Override
        public AssetId id() {
            return workingCopy.state().definition();
        }

        @Override
        public AssetKind kind() {
            return workingCopy.state().kind();
        }

        @Override
        public EditorRetainedDefinition.Origin origin() {
            return EditorRetainedDefinition.Origin.AUTHORED;
        }

        @Override
        public URI source() {
            return workingCopy.current().source().toUri();
        }
    }

    private record GeneratedRetainedState(
            AssetId id, AssetKind kind, URI source, EditorRetainedDefinition.Content content) implements RetainedState {
        private GeneratedRetainedState {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(content, "content");
            if (!id.equals(content.id()) || kind != content.kind()) {
                throw new IllegalArgumentException("generated definition identity and content must agree");
            }
        }

        @Override
        public EditorRetainedDefinition.Origin origin() {
            return EditorRetainedDefinition.Origin.GENERATED;
        }
    }
}
