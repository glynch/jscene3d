/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/** Tests strict parsing of renderer protocol commands. */
final class RendererCommandTest {
    @Test
    void parsesStageOneAndLifecycleCommands() throws RendererProtocolException {
        assertThat(RendererCommand.parse("FRAME 42")).isEqualTo(new RendererCommand.Frame("42"));
        assertThat(RendererCommand.parse("DRAG 1.5 -2")).isEqualTo(new RendererCommand.Drag(1.5f, -2.0f));
        assertThat(RendererCommand.parse("RECEIVE_SURFACE")).isInstanceOf(RendererCommand.ReceiveSurface.class);
        assertThat(RendererCommand.parse("PAUSE")).isInstanceOf(RendererCommand.Pause.class);
        assertThat(RendererCommand.parse("RESUME")).isInstanceOf(RendererCommand.Resume.class);
        assertThat(RendererCommand.parse("SHUTDOWN")).isInstanceOf(RendererCommand.Shutdown.class);
        assertThat(RendererCommand.parse("QUIT")).isInstanceOf(RendererCommand.Quit.class);
    }

    @Test
    void distinguishesMalformedInputFromUnknownCommands() {
        assertThatExceptionOfType(RendererProtocolException.class)
                .isThrownBy(() -> RendererCommand.parse("FRAME"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("MALFORMED_REQUEST"));
        assertThatExceptionOfType(RendererProtocolException.class)
                .isThrownBy(() -> RendererCommand.parse("DRAG NaN 1"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("MALFORMED_REQUEST"));
        assertThatExceptionOfType(RendererProtocolException.class)
                .isThrownBy(() -> RendererCommand.parse("PROJECT_OPEN anything"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("UNKNOWN_COMMAND"));
    }
}
