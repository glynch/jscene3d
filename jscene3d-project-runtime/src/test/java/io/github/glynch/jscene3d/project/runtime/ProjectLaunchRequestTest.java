/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies the immutable project-launch contract. */
final class ProjectLaunchRequestTest {
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

        ProjectLaunchRequest request = ProjectLaunchRequest.playtest(
                "moving-floor-34", Path.of("worlds/../worlds/map01.world.json"), parameters);
        parameters.clear();

        assertThat(request.isPlaytest()).isTrue();
        assertThat(request.profile()).contains("moving-floor-34");
        assertThat(request.scene()).contains(Path.of("worlds/map01.world.json"));
        assertThat(request.parameter("example.invulnerable")).contains(value);
        Map<String, ProjectValue> immutableParameters = request.parameters();
        assertThatThrownBy(immutableParameters::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Rejects profile identities that cannot safely label a launch. */
    @Test
    void rejectsBlankProfileName() {
        Path scene = Path.of("world.json");
        Map<String, ProjectValue> parameters = Map.of();

        assertThatThrownBy(() -> ProjectLaunchRequest.playtest(" ", scene, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("profile must not be blank");
    }

    /** Rejects scene selections that are not contained by the project. */
    @Test
    void rejectsInvalidScenePaths() {
        Map<String, ProjectValue> parameters = Map.of();
        Path absoluteScene = Path.of("/world.json");
        Path escapingScene = Path.of("../world.json");
        Path emptyScene = Path.of("");

        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", absoluteScene, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scene must be a project-relative path");
        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", escapingScene, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scene must be a project-relative path");
        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", emptyScene, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scene must be a project-relative path");
    }

    /** Rejects parameter maps whose entries cannot form a stable portable contract. */
    @Test
    void rejectsInvalidParameters() {
        ProjectValue value = new ProjectValue.BooleanValue(true);
        Path scene = Path.of("world.json");
        Map<String, ProjectValue> blankName = Map.of(" ", value);
        Map<String, ProjectValue> nullValue = new LinkedHashMap<>();
        nullValue.put("example.value", null);

        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", scene, blankName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parameter name must not be blank");
        assertThatThrownBy(() -> ProjectLaunchRequest.playtest("profile", scene, nullValue))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("parameter value");
    }
}
