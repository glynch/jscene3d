/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import org.junit.jupiter.api.Test;

/** Tests structured renderer-launch argument validation. */
final class RendererConfigurationTest {
    @Test
    void acceptsStageOneArgumentsWithCurrentProtocol() {
        RendererConfiguration configuration =
                RendererConfiguration.from(new String[] {"org.example.Editor", "640", "480"});

        assertThat(configuration)
                .isEqualTo(new RendererConfiguration("org.example.Editor", 640, 480, RendererProtocolVersion.CURRENT));
    }

    @Test
    void acceptsExplicitCurrentProtocolVersion() {
        RendererConfiguration configuration = RendererConfiguration.from(
                new String[] {"--protocol-version=1.0", "org.example.Editor", "1920", "1080"});

        assertThat(configuration.protocolVersion()).isEqualTo(RendererProtocolVersion.CURRENT);
    }

    @Test
    void rejectsUnsupportedOrMalformedProtocolVersions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(
                        new String[] {"--protocol-version=2.0", "org.example.Editor", "640", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(
                        new String[] {"--protocol-version=one", "org.example.Editor", "640", "480"}));
    }

    @Test
    void rejectsInvalidLaunchIdentityAndDimensions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(new String[] {" ", "640", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(new String[] {"org.example.Editor", "0", "480"}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RendererConfiguration.from(new String[] {"org.example.Editor", "640", "height"}));
        assertThatIllegalArgumentException().isThrownBy(() -> RendererConfiguration.from(new String[0]));
    }
}
