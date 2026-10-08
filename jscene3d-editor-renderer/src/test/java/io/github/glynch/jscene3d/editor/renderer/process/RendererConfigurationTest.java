/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import io.github.glynch.jscene3d.project.asset.AssetId;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests structured renderer-launch argument validation. */
final class RendererConfigurationTest {
    @Test
    void acceptsStageOneArgumentsWithCurrentProtocol() {
        RendererConfiguration configuration = configuration(new String[] {"org.example.Editor", "640", "480"});

        assertThat(configuration)
                .isEqualTo(new RendererConfiguration(
                        "org.example.Editor",
                        640,
                        480,
                        RendererProtocolVersion.CURRENT,
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void acceptsCompleteProjectLaunchSpecification() {
        RendererConfiguration configuration = configuration(new String[] {
            "--project-root=/projects/example",
            "--published-content-root=/projects/example/.jscene3d/published",
            "--engine-version=0.1.0-SNAPSHOT",
            "--project-id=example.project",
            "--scene-asset-id=e890c4c3-fb32-49d8-88b8-4e04e7a29656",
            "org.example.Editor",
            "1280",
            "720"
        });

        assertThat(configuration.projectLaunch())
                .contains(new RendererConfiguration.ProjectLaunch(
                        Path.of("/projects/example"),
                        Path.of("/projects/example/.jscene3d/published"),
                        "0.1.0-SNAPSHOT",
                        "example.project",
                        AssetId.from("e890c4c3-fb32-49d8-88b8-4e04e7a29656")));
    }

    @Test
    void distinguishesRuntimeFreeSceneViewLaunch() {
        RendererConfiguration configuration = configuration(new String[] {
            "--scene-view",
            "--project-root=/projects/example",
            "--published-content-root=/projects/example/.jscene3d/published",
            "--engine-version=0.1.0-SNAPSHOT",
            "--project-id=example.project",
            "--scene-asset-id=e890c4c3-fb32-49d8-88b8-4e04e7a29656",
            "org.example.Editor",
            "1280",
            "720"
        });

        assertThat(configuration.projectLaunch()).isEmpty();
        assertThat(configuration.sceneViewLaunch())
                .contains(new RendererConfiguration.SceneViewLaunch(
                        Path.of("/projects/example"),
                        Path.of("/projects/example/.jscene3d/published"),
                        "0.1.0-SNAPSHOT",
                        "example.project",
                        AssetId.from("e890c4c3-fb32-49d8-88b8-4e04e7a29656")));
    }

    @Test
    void rejectsPartialDuplicateOrInvalidProjectLaunchSpecification() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration(
                        new String[] {"--project-root=/projects/example", "org.example.Editor", "640", "480"}))
                .withMessageContaining("required");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration(new String[] {
                    "--project-root=/projects/example",
                    "--project-root=/projects/other",
                    "org.example.Editor",
                    "640",
                    "480"
                }))
                .withMessageContaining("must occur once");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration(new String[] {
                    "--project-root=relative",
                    "--published-content-root=/published",
                    "--engine-version=0.1.0-SNAPSHOT",
                    "--project-id=example.project",
                    "--scene-asset-id=not-an-asset-id",
                    "org.example.Editor",
                    "640",
                    "480"
                }));
    }

    @Test
    void acceptsExplicitCurrentProtocolVersion() {
        RendererConfiguration configuration =
                configuration(new String[] {"--protocol-version=1.0", "org.example.Editor", "1920", "1080"});

        assertThat(configuration.protocolVersion()).isEqualTo(RendererProtocolVersion.CURRENT);
    }

    @Test
    void rejectsUnsupportedOrMalformedProtocolVersions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        configuration(new String[] {"--protocol-version=2.0", "org.example.Editor", "640", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        configuration(new String[] {"--protocol-version=one", "org.example.Editor", "640", "480"}));
    }

    @Test
    void rejectsStaleDevelopmentBuildIdentity() {
        RendererBuildInfo build = RendererBuildInfo.current();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(new String[] {
                    "--protocol-version=1.0",
                    "--contract-identity=" + build.contractIdentity(),
                    "--build-identity=stale-build",
                    "org.example.Editor",
                    "640",
                    "480"
                }))
                .withMessageContaining("Stale renderer build identity");
    }

    @Test
    void rejectsInvalidLaunchIdentityAndDimensions() {
        assertThatIllegalArgumentException().isThrownBy(() -> configuration(new String[] {" ", "640", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration(new String[] {"org.example.Editor", "0", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> configuration(new String[] {"org.example.Editor", "640", "height"}));
        assertThatIllegalArgumentException().isThrownBy(() -> configuration(new String[0]));
    }

    private static RendererConfiguration configuration(String[] arguments) {
        RendererBuildInfo build = RendererBuildInfo.current();
        List<String> complete = new ArrayList<>();
        if (Arrays.stream(arguments).noneMatch(argument -> argument.startsWith("--protocol-version="))) {
            complete.add("--protocol-version=1.0");
        }
        complete.add("--contract-identity=" + build.contractIdentity());
        complete.add("--build-identity=" + build.buildIdentity());
        complete.addAll(Arrays.asList(arguments));
        return RendererConfiguration.from(complete.toArray(String[]::new));
    }
}
