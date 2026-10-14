/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Verifies strict decoding of the runtime-free Scene View renderer payload. */
final class SceneViewSnapshotCodecTest {
    private static final String SCENE = "11111111-1111-4111-8111-111111111111";

    @Test
    void decodesCompleteProjectedVisualState() {
        var snapshot = SceneViewSnapshotCodec.decode(encoded(String.format(
                Locale.ROOT,
                """
                {
                  "sceneAssetId": "%s",
                  "revision": 5,
                  "occurrences": [{
                    "occurrence": {"rootDefinitionAssetId": "%s", "entityPath": ["33333333-3333-4333-8333-333333333333"]},
                    "parent": null,
                    "authoredAssetId": "22222222-2222-4222-8222-222222222222",
                    "authoredSource": "file:///project/example.scene.json",
                    "authoredEntityId": "33333333-3333-4333-8333-333333333333",
                    "name": "Example",
                    "enabled": true,
                    "transform": {
                      "identity": %s,
                      "position": {"x": "1", "y": "2.5", "z": "-3"},
                      "orientationDegrees": {"x": "10", "y": "20", "z": "30"},
                      "scale": {"x": "1", "y": "1", "z": "1"}
                    },
                    "meshes": [{
                      "identity": %s,
                      "mesh": {"kind": "project", "locator": "meshes/cube", "projectPath": "/project/meshes/cube.json"},
                      "material": {"kind": "asset", "locator": "default-material", "projectPath": null},
                      "visible": true
                    }],
                    "directionalLight": {
                      "identity": %s,
                      "color": {"x": "1", "y": "0.5", "z": "0.25"},
                      "intensity": "2",
                      "target": {"x": "0", "y": "0", "z": "0"}
                    },
                    "cameras": [{
                      "kind": "perspective",
                      "identity": %s,
                      "fieldOfViewDegrees": "55",
                      "near": "0.1",
                      "far": "500",
                      "primary": false
                    }, {
                      "kind": "orthographic",
                      "identity": %s,
                      "left": "-8",
                      "right": "8",
                      "top": "5",
                      "bottom": "-5",
                      "near": "0",
                      "far": "500",
                      "zoom": "1.5",
                      "primary": true
                    }]
                  }]
                }
                """,
                SCENE,
                SCENE,
                identity("66666666-6666-4666-8666-666666666666"),
                identity("77777777-7777-4777-8777-777777777777"),
                identity("88888888-8888-4888-8888-888888888888"),
                identity("99999999-9999-4999-8999-999999999999"),
                identity("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))));

        assertThat(snapshot.scene().toString()).isEqualTo(SCENE);
        assertThat(snapshot.revision()).isEqualTo(5L);
        assertThat(snapshot.occurrences()).singleElement().satisfies(occurrence -> {
            assertThat(occurrence.name()).contains("Example");
            assertThat(occurrence.transform()).isPresent();
            assertThat(occurrence.meshes()).singleElement().satisfies(mesh -> {
                assertThat(mesh.mesh().kind().name()).isEqualTo("PROJECT");
                assertThat(mesh.material().kind().name()).isEqualTo("ASSET");
            });
            assertThat(occurrence.directionalLight()).isPresent();
            assertThat(occurrence.cameras())
                    .extracting(camera -> camera.getClass().getSimpleName())
                    .containsExactly("PerspectiveCamera3d", "OrthographicCamera3d");
        });
    }

    @Test
    void rejectsMalformedCameraProjectionPayloads() {
        String occurrence = String.format(Locale.ROOT, """
                {"sceneAssetId":"%s","revision":0,"occurrences":[{
                  "occurrence":{"rootDefinitionAssetId":"%s","entityPath":["33333333-3333-4333-8333-333333333333"]},
                  "parent":null,
                  "authoredAssetId":"22222222-2222-4222-8222-222222222222",
                  "authoredSource":"file:///project/example.scene.json",
                  "authoredEntityId":"33333333-3333-4333-8333-333333333333",
                  "name":null,"enabled":true,"transform":null,"meshes":[],"directionalLight":null,
                  "cameras":[%s]
                }]}
                """, SCENE, SCENE, "%s");
        String identity = identity("99999999-9999-4999-8999-999999999999");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> SceneViewSnapshotCodec.decode(encoded(String.format(
                        Locale.ROOT, occurrence, "{\"kind\":\"panoramic\",\"identity\":" + identity + "}"))));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SceneViewSnapshotCodec.decode(encoded(String.format(
                        Locale.ROOT,
                        occurrence,
                        "{\"kind\":\"perspective\",\"identity\":" + identity
                                + ",\"fieldOfViewDegrees\":\"55\",\"near\":\"1\",\"far\":\"1\",\"primary\":false}"))));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SceneViewSnapshotCodec.decode(encoded(String.format(
                        Locale.ROOT,
                        occurrence,
                        "{\"kind\":\"perspective\",\"identity\":" + identity
                                + ",\"fieldOfViewDegrees\":\"55\",\"near\":\"0.1\",\"far\":\"500\"}"))));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SceneViewSnapshotCodec.decode(encoded(String.format(
                        Locale.ROOT,
                        occurrence,
                        "{\"kind\":\"perspective\",\"identity\":" + identity
                                + ",\"fieldOfViewDegrees\":\"55\",\"near\":\"0.1\",\"far\":\""
                                + "9".repeat(400) + "\",\"primary\":false}"))));
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> SceneViewSnapshotCodec.decode(
                                encoded(
                                        String.format(
                                                Locale.ROOT,
                                                occurrence,
                                                "{\"kind\":\"orthographic\",\"identity\":" + identity
                                                        + ",\"left\":\"1\",\"right\":\"-1\",\"top\":\"1\",\"bottom\":\"-1\",\"near\":\"0\",\"far\":\"10\",\"zoom\":\"1\",\"primary\":false}"))));
    }

    @Test
    void rejectsMalformedExactDecimalText() {
        String json = String.format(Locale.ROOT, """
                {"sceneAssetId":"%s","revision":0,"occurrences":[{
                  "occurrence":{"rootDefinitionAssetId":"%s","entityPath":[]},
                  "parent":null,
                  "authoredAssetId":"22222222-2222-4222-8222-222222222222",
                  "authoredSource":"file:///project/example.scene.json",
                  "authoredEntityId":"33333333-3333-4333-8333-333333333333",
                  "name":null,
                  "enabled":true,
                  "transform":null,
                  "meshes":[],
                  "directionalLight":{"identity":%s,"color":{"x":"NaN","y":"0","z":"0"},"intensity":"1","target":{"x":"0","y":"0","z":"0"}},
                  "cameras":[]
                }]}
                """, SCENE, SCENE, identity("88888888-8888-4888-8888-888888888888"));

        assertThatIllegalArgumentException().isThrownBy(() -> SceneViewSnapshotCodec.decode(encoded(json)));
    }

    private static String identity(String componentId) {
        return String.format(Locale.ROOT, """
                {
                  "occurrence": {"rootDefinitionAssetId": "%s", "entityPath": ["33333333-3333-4333-8333-333333333333"]},
                  "scope": {
                    "definitionAssetId": "22222222-2222-4222-8222-222222222222",
                    "anchor": {"rootDefinitionAssetId": "%s", "entityPath": []}
                  },
                  "authoredEntityId": "33333333-3333-4333-8333-333333333333",
                  "componentId": "%s"
                }
                """, SCENE, SCENE, componentId);
    }

    private static String encoded(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
