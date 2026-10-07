/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests strict parsing of renderer protocol commands. */
final class RendererCommandTest {
    @Test
    void parsesStageOneAndLifecycleCommands() throws RendererProtocolException {
        assertThat(RendererCommand.parse("FRAME 42")).isEqualTo(new RendererCommand.Frame("42"));
        assertThat(RendererCommand.parse("DRAG 1.5 -2")).isEqualTo(new RendererCommand.Drag(1.5f, -2.0f));
        assertThat(RendererCommand.parse("SCENE_SNAPSHOT e30")).isEqualTo(new RendererCommand.SceneSnapshot("e30"));
        CompositionOccurrenceId occurrence = new CompositionOccurrenceId(
                AssetId.from("11111111-1111-4111-8111-111111111111"),
                List.of(EntityId.from("22222222-2222-4222-8222-222222222222")));
        assertThat(RendererCommand.parse("SCENE_PICK request-7 3 -0.25 0.5"))
                .isEqualTo(new RendererCommand.ScenePick("request-7", 3L, -0.25f, 0.5f));
        assertThat(RendererCommand.parse("SCENE_SELECT 3 " + SceneViewOccurrenceCodec.encode(occurrence)))
                .isEqualTo(new RendererCommand.SceneSelect(3L, Optional.of(occurrence)));
        assertThat(RendererCommand.parse("SCENE_SELECT 3 NONE"))
                .isEqualTo(new RendererCommand.SceneSelect(3L, Optional.empty()));
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
                .isThrownBy(() -> RendererCommand.parse("SCENE_PICK request 2 1.1 0"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("MALFORMED_REQUEST"));
        assertThatExceptionOfType(RendererProtocolException.class)
                .isThrownBy(() -> RendererCommand.parse("SCENE_SELECT 2 not-base64"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("MALFORMED_REQUEST"));
        assertThatExceptionOfType(RendererProtocolException.class)
                .isThrownBy(() -> RendererCommand.parse("PROJECT_OPEN anything"))
                .satisfies(exception -> assertThat(exception.category()).isEqualTo("UNKNOWN_COMMAND"));
    }
}
