/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.composition.CompositionScope;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Strict decoder for complete runtime-free Scene View snapshots sent over the renderer protocol. */
final class SceneViewSnapshotCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SceneViewSnapshotCodec() {
        throw new AssertionError("SceneViewSnapshotCodec cannot be instantiated");
    }

    static SceneViewSnapshot decode(String encodedSnapshot) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedSnapshot);
            JsonNode root = requireObject(MAPPER.readTree(json), "snapshot");
            AssetId scene = AssetId.from(text(root, "sceneAssetId"));
            long revision = nonNegativeLong(root, "revision");
            List<SceneViewSnapshot.VisualOccurrence> occurrences = new ArrayList<>();
            for (JsonNode occurrence : array(root, "occurrences")) {
                occurrences.add(visualOccurrence(requireObject(occurrence, "occurrence")));
            }
            return new SceneViewSnapshot(scene, revision, occurrences);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Scene View snapshot", exception);
        }
    }

    private static SceneViewSnapshot.VisualOccurrence visualOccurrence(JsonNode node) {
        return new SceneViewSnapshot.VisualOccurrence(
                occurrence(object(node, "occurrence")),
                optionalObject(node, "parent").map(SceneViewSnapshotCodec::occurrence),
                AssetId.from(text(node, "authoredAssetId")),
                URI.create(text(node, "authoredSource")),
                EntityId.from(text(node, "authoredEntityId")),
                optionalText(node, "name"),
                booleanValue(node, "enabled"),
                optionalObject(node, "transform").map(SceneViewSnapshotCodec::transform),
                meshes(node),
                optionalObject(node, "directionalLight").map(SceneViewSnapshotCodec::directionalLight));
    }

    private static List<SceneViewSnapshot.MeshRenderer3d> meshes(JsonNode node) {
        List<SceneViewSnapshot.MeshRenderer3d> meshes = new ArrayList<>();
        for (JsonNode mesh : array(node, "meshes")) {
            JsonNode value = requireObject(mesh, "mesh");
            meshes.add(new SceneViewSnapshot.MeshRenderer3d(
                    componentIdentity(object(value, "identity")),
                    resource(object(value, "mesh")),
                    resource(object(value, "material")),
                    booleanValue(value, "visible")));
        }
        return meshes;
    }

    private static SceneViewSnapshot.Transform3d transform(JsonNode node) {
        return new SceneViewSnapshot.Transform3d(
                componentIdentity(object(node, "identity")),
                vector(object(node, "position")),
                vector(object(node, "orientationDegrees")),
                vector(object(node, "scale")));
    }

    private static SceneViewSnapshot.DirectionalLight3d directionalLight(JsonNode node) {
        return new SceneViewSnapshot.DirectionalLight3d(
                componentIdentity(object(node, "identity")),
                vector(object(node, "color")),
                decimal(node, "intensity"),
                vector(object(node, "target")));
    }

    private static SceneViewSnapshot.ComponentIdentity componentIdentity(JsonNode node) {
        JsonNode scope = object(node, "scope");
        return new SceneViewSnapshot.ComponentIdentity(
                occurrence(object(node, "occurrence")),
                new CompositionScope(
                        AssetId.from(text(scope, "definitionAssetId")), occurrence(object(scope, "anchor"))),
                EntityId.from(text(node, "authoredEntityId")),
                ComponentId.from(text(node, "componentId")));
    }

    private static CompositionOccurrenceId occurrence(JsonNode node) {
        List<EntityId> path = new ArrayList<>();
        for (JsonNode entity : array(node, "entityPath")) {
            if (!entity.isTextual()) {
                throw new IllegalArgumentException("entityPath entries must be strings");
            }
            path.add(EntityId.from(entity.textValue()));
        }
        return new CompositionOccurrenceId(AssetId.from(text(node, "rootDefinitionAssetId")), path);
    }

    private static SceneViewSnapshot.Vector3 vector(JsonNode node) {
        return new SceneViewSnapshot.Vector3(decimal(node, "x"), decimal(node, "y"), decimal(node, "z"));
    }

    private static ResourceReference resource(JsonNode node) {
        String kind = text(node, "kind");
        String locator = text(node, "locator");
        return switch (kind) {
            case "project" -> ResourceReference.project(locator, Path.of(text(node, "projectPath")));
            case "asset" -> ResourceReference.asset(locator);
            case "import" -> ResourceReference.imported(locator);
            default -> throw new IllegalArgumentException("Unsupported Scene View resource kind: " + kind);
        };
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return new BigDecimal(text(node, field));
    }

    private static long nonNegativeLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong() || value.longValue() < 0) {
            throw new IllegalArgumentException(field + " must be a non-negative integer");
        }
        return value.longValue();
    }

    private static boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new IllegalArgumentException(field + " must be a boolean");
        }
        return value.booleanValue();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(field + " must be a non-blank string");
        }
        return value.textValue();
    }

    private static Optional<String> optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " must be a string or null");
        }
        return Optional.of(value.textValue());
    }

    private static JsonNode object(JsonNode node, String field) {
        return requireObject(node.get(field), field);
    }

    private static Optional<JsonNode> optionalObject(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? Optional.empty() : Optional.of(requireObject(value, field));
    }

    private static JsonNode requireObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        return node;
    }

    private static JsonNode array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException(field + " must be an array");
        }
        return value;
    }
}
