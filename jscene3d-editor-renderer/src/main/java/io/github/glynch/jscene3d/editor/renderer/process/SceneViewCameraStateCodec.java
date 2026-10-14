/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import org.joml.Vector3fc;

/** Base64url JSON codec for renderer-authoritative Scene View camera state. */
final class SceneViewCameraStateCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SceneViewCameraStateCodec() {
        throw new AssertionError("SceneViewCameraStateCodec cannot be instantiated");
    }

    static String encode(SceneViewCameraState state) {
        ObjectNode root = MAPPER.createObjectNode();
        vector(root.putArray("position"), state.position());
        vector(root.putArray("orientationDegrees"), state.orientationDegrees());
        vector(root.putArray("pivot"), state.pivot());
        root.put("projection", state.projection().name().toLowerCase(Locale.ROOT));
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MAPPER.writeValueAsBytes(root));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode Scene View camera state", exception);
        }
    }

    private static void vector(ArrayNode destination, Vector3fc value) {
        destination.add(value.x());
        destination.add(value.y());
        destination.add(value.z());
    }
}
