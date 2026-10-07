/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Strict base64url JSON codec for one stable Scene View occurrence identity. */
final class SceneViewOccurrenceCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SceneViewOccurrenceCodec() {
        throw new AssertionError("SceneViewOccurrenceCodec cannot be instantiated");
    }

    static String encode(CompositionOccurrenceId occurrence) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("rootDefinitionAssetId", occurrence.rootDefinition().toString());
        ArrayNode path = root.putArray("entityPath");
        occurrence.entityPath().forEach(entity -> path.add(entity.toString()));
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(root));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode Scene View occurrence", exception);
        }
    }

    static CompositionOccurrenceId decode(String encodedOccurrence) {
        try {
            JsonNode root = MAPPER.readTree(Base64.getUrlDecoder().decode(encodedOccurrence));
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("occurrence must be an object");
            }
            JsonNode rootDefinition = root.get("rootDefinitionAssetId");
            JsonNode entityPath = root.get("entityPath");
            if (rootDefinition == null
                    || !rootDefinition.isTextual()
                    || rootDefinition.textValue().isBlank()
                    || entityPath == null
                    || !entityPath.isArray()
                    || entityPath.isEmpty()) {
                throw new IllegalArgumentException("occurrence identity is incomplete");
            }
            List<EntityId> path = new ArrayList<>();
            for (JsonNode entity : entityPath) {
                if (!entity.isTextual()) {
                    throw new IllegalArgumentException("entityPath entries must be strings");
                }
                path.add(EntityId.from(entity.textValue()));
            }
            return new CompositionOccurrenceId(AssetId.from(rootDefinition.textValue()), path);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Scene View occurrence", exception);
        }
    }
}
