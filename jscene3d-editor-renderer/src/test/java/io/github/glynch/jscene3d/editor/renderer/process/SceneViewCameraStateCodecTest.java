/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Tests the renderer-to-editor camera-state wire representation. */
final class SceneViewCameraStateCodecTest {
    @Test
    void encodesFiniteCameraValuesAndProjectionAsBase64UrlJson() {
        SceneViewCameraState state = new SceneViewCameraState(
                new Vector3f(2.5f, 4.0f, 8.25f),
                new Vector3f(15.0f, 45.0f, -0.0f),
                new Vector3f(),
                SceneViewCameraState.Projection.PERSPECTIVE);

        String json = new String(
                Base64.getUrlDecoder().decode(SceneViewCameraStateCodec.encode(state)), StandardCharsets.UTF_8);

        assertThat(json)
                .isEqualTo(
                        "{\"position\":[2.5,4.0,8.25],\"orientationDegrees\":[15.0,45.0,-0.0],\"pivot\":[0.0,0.0,0.0],\"projection\":\"perspective\"}");
    }
}
