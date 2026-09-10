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
        WorldStub startupMenu = new WorldStub(0);
        WorldStub firstGame = new WorldStub(1);
        WorldStub pauseMenu = new WorldStub(2);
        try (var lifecycle = new DesktopSessionLifecycle<WorldStub>(true)) {
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
    }

    @Test
    void replacesGameplayOnlyAfterTheFreshWorldLoadsSuccessfully() {
        WorldStub existing = new WorldStub(1);
        Supplier<WorldStub> menuLoader = () -> new WorldStub(2);
        Supplier<WorldStub> failingGameplayLoader = () -> {
            throw new IllegalStateException("load failed");
        };
        try (var lifecycle = new DesktopSessionLifecycle<WorldStub>(false)) {
            lifecycle.start(existing);

            assertThatThrownBy(() -> lifecycle.apply(ApplicationCommand.NEW_GAME, menuLoader, failingGameplayLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("load failed");

            assertThat(lifecycle.current()).isSameAs(existing);
            assertThat(existing.closed).isFalse();
        }
    }

    @Test
    void createsANewGameplayWorldForEveryNewGameAndHonorsQuit() {
        AtomicInteger identities = new AtomicInteger();
        try (var lifecycle = new DesktopSessionLifecycle<WorldStub>(true)) {
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
    }

    @Test
    void returnsToANonResumableStartupMenuAndDiscardsTerminalGameplay() {
        WorldStub startupMenu = new WorldStub(0);
        WorldStub gameplay = new WorldStub(1);
        WorldStub returnedMenu = new WorldStub(2);
        try (var lifecycle = new DesktopSessionLifecycle<WorldStub>(true)) {
            lifecycle.start(startupMenu);
            lifecycle.apply(ApplicationCommand.NEW_GAME, () -> returnedMenu, () -> gameplay);

            assertThat(lifecycle.apply(ApplicationCommand.RETURN_TO_MENU, () -> returnedMenu, () -> new WorldStub(3)))
                    .isTrue();

            assertThat(gameplay.closed).isTrue();
            assertThat(lifecycle.current()).isSameAs(returnedMenu);
            assertThat(lifecycle.canResume()).isFalse();
        }
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
