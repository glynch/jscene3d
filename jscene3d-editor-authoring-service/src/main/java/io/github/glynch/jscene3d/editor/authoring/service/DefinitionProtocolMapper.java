/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionSnapshot;
import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.session.EditorRetainedDefinition;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Maps retained definitions and semantic targets to and from their wire representation. */
final class DefinitionProtocolMapper {
    DefinitionSnapshot snapshot(EditorRetainedDefinition definition) {
        DefinitionSnapshot.DefinitionContext context = new DefinitionSnapshot.DefinitionContext(
                definition.id().toString(),
                definition.kind().serializedName(),
                definition.origin().name().toLowerCase(Locale.ROOT),
                definition.editable(),
                definition.source().toString(),
                text(definition.hierarchy().context().label()));
        return new DefinitionSnapshot(
                definition.revision(),
                context,
                definition.hierarchy().roots().stream().map(this::hierarchyNode).toList());
    }

    DefinitionSnapshot.SemanticTarget target(InspectorTarget target) {
        return new DefinitionSnapshot.SemanticTarget(
                targetKind(target.kind()),
                target.source().toString(),
                target.identity(),
                target.occurrence().map(this::occurrence).orElse(null));
    }

    DefinitionSnapshot.Occurrence occurrence(HierarchyOccurrenceId occurrence) {
        return new DefinitionSnapshot.Occurrence(
                occurrence.definition().toString(),
                occurrence.entityPath().stream().map(Object::toString).toList());
    }

    InspectorTarget inspectorTarget(DefinitionSnapshot.SemanticTarget target) {
        InspectorTarget.Kind kind =
                switch (target.kind()) {
                    case "scene" -> InspectorTarget.Kind.SCENE;
                    case "local-entity" -> InspectorTarget.Kind.LOCAL_ENTITY;
                    case "generated-entity" -> InspectorTarget.Kind.GENERATED_ENTITY;
                    case "placement" -> InspectorTarget.Kind.PLACEMENT;
                    case "asset" -> InspectorTarget.Kind.ASSET;
                    default ->
                        throw new IllegalArgumentException("Unsupported Inspector target kind: " + target.kind());
                };
        Optional<HierarchyOccurrenceId> occurrence = Optional.ofNullable(target.occurrence())
                .map(value -> new HierarchyOccurrenceId(
                        AssetId.from(value.definitionAssetId()),
                        value.entityPath().stream().map(EntityId::from).toList()));
        return new InspectorTarget(kind, URI.create(target.source()), target.identity(), occurrence);
    }

    private DefinitionSnapshot.HierarchyNode hierarchyNode(EditorHierarchyNode node) {
        return new DefinitionSnapshot.HierarchyNode(
                occurrence(node.occurrence()),
                hierarchyKind(node.kind()),
                node.entityId().map(Object::toString).orElse(null),
                node.definitionId().map(Object::toString).orElse(null),
                text(node.label()),
                node.isEnabled(),
                node.isModified(),
                node.isEditable(),
                target(node.inspectorTarget()),
                node.children().stream().map(this::hierarchyNode).toList());
    }

    private static DefinitionSnapshot.AuthoringTextDto text(AuthoringText value) {
        return switch (value) {
            case AuthoringText.Literal(String literal) ->
                new DefinitionSnapshot.AuthoringTextDto("literal", literal, null, List.of());
            case AuthoringText.Message(String code, String defaultMessage, List<Object> arguments) ->
                new DefinitionSnapshot.AuthoringTextDto(
                        "message",
                        defaultMessage,
                        code,
                        arguments.stream().map(Object::toString).toList());
        };
    }

    private static String hierarchyKind(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case SCENE -> "scene";
            case LOCAL_ENTITY -> "local-entity";
            case PLACEMENT -> "placement";
            case GENERATED_ENTITY -> "generated-entity";
        };
    }

    private static String targetKind(InspectorTarget.Kind kind) {
        return switch (kind) {
            case SCENE -> "scene";
            case LOCAL_ENTITY -> "local-entity";
            case GENERATED_ENTITY -> "generated-entity";
            case PLACEMENT -> "placement";
            case ASSET -> "asset";
        };
    }
}
