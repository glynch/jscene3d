/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class GameRuntimeTest {
    private static final InputAction JUMP = new InputAction("jump");
    private static final Duration STEP = Duration.ofMillis(10L);

    @Test
    void coordinatesFixedFrameAndRenderCallbacks() {
        RecordingApplication application = new RecordingApplication();
        GameRuntime runtime = new GameRuntime(application, settings());
        runtime.start();

        FrameUpdate frame = runtime.advance(Duration.ofMillis(25L), ActionSnapshot.empty());
        runtime.render();

        assertThat(application.events).containsExactly("start", "fixed:0", "fixed:1", "update", "render");
        assertThat(frame.fixedUpdateCount()).isEqualTo(2);
        assertThat(frame.simulationTime()).isEqualTo(Duration.ofMillis(20L));
        assertThat(frame.interpolation()).isEqualTo(0.5F);
        assertThat(runtime.settings()).isEqualTo(settings());
        runtime.close();
        runtime.close();
        assertThat(application.closeCount).isEqualTo(1);
        assertThat(runtime.isClosed()).isTrue();
    }

    @Test
    void buffersTransitionsUntilOneFixedUpdateConsumesThem() {
        RecordingApplication application = new RecordingApplication();
        GameRuntime runtime = new GameRuntime(application, settings());
        runtime.start();

        runtime.advance(
                Duration.ofMillis(4L), ActionSnapshot.builder().pressed(JUMP).build());
        runtime.advance(
                Duration.ofMillis(26L), ActionSnapshot.builder().down(JUMP).build());

        assertThat(application.fixedInputs).hasSize(3);
        assertThat(application.fixedInputs.get(0).wasPressed(JUMP)).isTrue();
        assertThat(application.fixedInputs.get(1).wasPressed(JUMP)).isFalse();
        assertThat(application.fixedInputs.get(2).isDown(JUMP)).isTrue();
        runtime.close();
    }

    @Test
    void clampsLongFramesAndReportsDroppedTime() {
        RecordingApplication application = new RecordingApplication();
        GameRuntime runtime = new GameRuntime(application, settings());
        runtime.start();

        FrameUpdate frame = runtime.advance(Duration.ofMillis(100L), ActionSnapshot.empty());

        assertThat(frame.elapsed()).isEqualTo(Duration.ofMillis(40L));
        assertThat(frame.fixedUpdateCount()).isEqualTo(4);
        assertThat(frame.droppedTime()).isEqualTo(Duration.ofMillis(60L));
        runtime.close();
    }

    @Test
    void enforcesLifecycleAndArgumentContracts() {
        RecordingApplication application = new RecordingApplication();
        GameRuntime runtime = new GameRuntime(application, settings());
        ActionSnapshot empty = ActionSnapshot.empty();

        assertThatThrownBy(runtime::render).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> runtime.advance(Duration.ZERO, empty)).isInstanceOf(IllegalStateException.class);
        runtime.start();
        assertThat(runtime.isStarted()).isTrue();
        assertThatThrownBy(runtime::start).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> runtime.advance(Duration.ofMillis(-1L), empty))
                .isInstanceOf(IllegalArgumentException.class);
        runtime.close();
        assertThatThrownBy(runtime::render).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void remainsClosedWhenApplicationCleanupFails() {
        GameApplication application = new RecordingApplication() {
            @Override
            public void close() {
                super.close();
                throw new IllegalStateException("cleanup failed");
            }
        };
        GameRuntime runtime = new GameRuntime(application, settings());

        assertThatThrownBy(runtime::close).isInstanceOf(IllegalStateException.class);
        assertThat(runtime.isClosed()).isTrue();
        runtime.close();
    }

    private static GameLoopSettings settings() {
        return GameLoopSettings.builder()
                .fixedStep(STEP)
                .maximumFrameTime(Duration.ofMillis(40L))
                .maximumFixedUpdates(4)
                .build();
    }

    /** Records callback ordering and fixed-update input. */
    private static class RecordingApplication implements GameApplication {
        private final List<String> events = new ArrayList<>();
        private final List<ActionSnapshot> fixedInputs = new ArrayList<>();
        private int closeCount;

        @Override
        public void start() {
            events.add("start");
        }

        @Override
        public void fixedUpdate(FixedUpdate update) {
            events.add("fixed:" + update.tick());
            fixedInputs.add(update.input());
            assertThat(update.step()).isEqualTo(STEP);
        }

        @Override
        public void update(FrameUpdate update) {
            events.add("update");
        }

        @Override
        public void render(FrameUpdate update) {
            events.add("render");
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
