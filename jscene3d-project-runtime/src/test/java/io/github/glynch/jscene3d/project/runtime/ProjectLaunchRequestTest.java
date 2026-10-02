/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies the immutable project-launch contract. */
final class ProjectLaunchRequestTest {
    private static final AssetRef<SceneDefinition> SCENE =
            AssetRef.to(AssetId.from("2f26576c-570d-4338-bc30-52bc41def3a5"), "worlds/map01.scene.json");

    /** Keeps ordinary launches free of developer-only selections and parameters. */
    @Test
    void representsStandardLaunch() {
        ProjectLaunchRequest request = ProjectLaunchRequest.standard();

        assertThat(request.isPlaytest()).isFalse();
        assertThat(request.profile()).isEmpty();
        assertThat(request.scene()).isEmpty();
        assertThat(request.parameters()).isEmpty();
        assertThat(request.parameter("missing")).isEmpty();
    }

    /** Preserves a named playtest selection while isolating it from caller mutation. */
    @Test
    void representsImmutablePlaytestLaunch() {
        Map<String, ProjectValue> parameters = new LinkedHashMap<>();
        ProjectValue value = new ProjectValue.BooleanValue(true);
        parameters.put("example.invulnerable", value);

        ProjectLaunchRequest request = ProjectLaunchRequest.playtest("moving-floor-34", SCENE, parameters);
        parameters.clear();

        assertThat(request.isPlaytest()).isTrue();
        assertThat(request.profile()).contains("moving-floor-34");
        assertThat(request.scene()).contains(SCENE);
        assertThat(request.parameter("example.invulnerable")).contains(value);
        Map<String, ProjectValue> immutableParameters = request.parameters();
        assertThatThrownBy(immutableParameters::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Rejects profile identities that cannot safely label a launch. */
    @Test
    void rejectsBlankProfileName() {
        Map<String, ProjectValue> parameters = Map.of();

        assertThatThrownBy(() -> ProjectLaunchRequest.playtest(" ", SCENE, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile must not be blank");
    }

    /** Rejects absent explicit Scene selections. */
    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullSceneReferences() {
        Map<String, ProjectValue> parameters = Map.of();

        assertThatThrownBy(() -> ProjectLaunchRequest.scene(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("scene");
        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", null, parameters))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("scene");
    }

    /** Rejects parameter maps whose entries cannot form a stable portable contract. */
    @Test
    void rejectsInvalidParameters() {
        ProjectValue value = new ProjectValue.BooleanValue(true);
        Map<String, ProjectValue> blankName = Map.of(" ", value);
        Map<String, ProjectValue> nullValue = new LinkedHashMap<>();
        nullValue.put("example.value", null);

        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", SCENE, blankName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter name must not be blank");
        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", SCENE, nullValue))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameter value");
    }
}
