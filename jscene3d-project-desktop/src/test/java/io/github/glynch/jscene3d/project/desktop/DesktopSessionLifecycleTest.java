/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.application.ApplicationCommand;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Specifies world ownership independently from GLFW and expensive project composition. */
final class DesktopSessionLifecycleTest {
    @Test
    void startsAtTheMenuAndMovesThroughFreshGameplayPauseAndResume() {
        DesktopSessionLifecycle<WorldStub> lifecycle = new DesktopSessionLifecycle<>(true);
        WorldStub startupMenu = new WorldStub(0);
        WorldStub firstGame = new WorldStub(1);
        WorldStub pauseMenu = new WorldStub(2);

        lifecycle.start(startupMenu);
        assertThat(lifecycle.current()).isSameAs(startupMenu);
        assertThat(lifecycle.canResume()).isFalse();

        assertThat(lifecycle.apply(ApplicationCommand.NEW_GAME, () -> pauseMenu, () -> firstGame))
                .isTrue();
        assertThat(startupMenu.closed).isTrue();
        assertThat(lifecycle.current()).isSameAs(firstGame);

        assertThat(lifecycle.apply(ApplicationCommand.SHOW_MENU, () -> pauseMenu, () -> new WorldStub(3)))
                .isTrue();
        assertThat(lifecycle.current()).isSameAs(pauseMenu);
        assertThat(lifecycle.canResume()).isTrue();
        assertThat(firstGame.closed).isFalse();

        assertThat(lifecycle.apply(ApplicationCommand.RESUME, () -> new WorldStub(4), () -> new WorldStub(5)))
                .isTrue();
        assertThat(pauseMenu.closed).isTrue();
        assertThat(lifecycle.current()).isSameAs(firstGame);
        assertThat(lifecycle.canResume()).isFalse();
    }

    @Test
    void replacesGameplayOnlyAfterTheFreshWorldLoadsSuccessfully() {
        DesktopSessionLifecycle<WorldStub> lifecycle = new DesktopSessionLifecycle<>(false);
        WorldStub existing = new WorldStub(1);
        lifecycle.start(existing);
        Supplier<WorldStub> menuLoader = () -> new WorldStub(2);
        Supplier<WorldStub> failingGameplayLoader = () -> {
            throw new IllegalStateException("load failed");
        };

        assertThatThrownBy(() -> lifecycle.apply(ApplicationCommand.NEW_GAME, menuLoader, failingGameplayLoader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("load failed");

        assertThat(lifecycle.current()).isSameAs(existing);
        assertThat(existing.closed).isFalse();
    }

    @Test
    void createsANewGameplayWorldForEveryNewGameAndHonorsQuit() {
        DesktopSessionLifecycle<WorldStub> lifecycle = new DesktopSessionLifecycle<>(true);
        AtomicInteger identities = new AtomicInteger();
        lifecycle.start(new WorldStub(identities.getAndIncrement()));

        lifecycle.apply(
                ApplicationCommand.NEW_GAME,
                () -> new WorldStub(-1),
                () -> new WorldStub(identities.getAndIncrement()));
        WorldStub first = lifecycle.current();
        lifecycle.apply(
                ApplicationCommand.NEW_GAME,
                () -> new WorldStub(-1),
                () -> new WorldStub(identities.getAndIncrement()));

        assertThat(lifecycle.current()).isNotSameAs(first);
        assertThat(first.closed).isTrue();
        assertThat(lifecycle.apply(ApplicationCommand.QUIT, () -> new WorldStub(-1), () -> new WorldStub(-1)))
                .isFalse();
    }

    /** Minimal observable replacement for one owned running world. */
    private static final class WorldStub implements AutoCloseable {
        private final int identity;
        private boolean closed;

        private WorldStub(int identity) {
            this.identity = identity;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public String toString() {
            return "WorldStub[identity=" + identity + "]";
        }
    }
}
