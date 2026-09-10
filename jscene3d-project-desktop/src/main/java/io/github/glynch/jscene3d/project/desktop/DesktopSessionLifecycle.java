/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.application.ApplicationCommand;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Owns menu and gameplay replacement independently from native-window concerns. */
final class DesktopSessionLifecycle<T extends AutoCloseable> implements AutoCloseable {
    private final boolean startupMenu;

    private @Nullable T current;
    private @Nullable T menu;
    private @Nullable T gameplay;

    /** Creates lifecycle state for a project with or without a distinct startup menu. */
    DesktopSessionLifecycle(boolean startupMenu) {
        this.startupMenu = startupMenu;
    }

    /** Takes ownership of the manifest-selected initial world exactly once. */
    void start(T started) {
        if (current != null) {
            throw new IllegalStateException("desktop session lifecycle has already started");
        }
        T validStarted = Objects.requireNonNull(started, "started");
        if (startupMenu) {
            menu = validStarted;
        } else {
            gameplay = validStarted;
        }
        current = validStarted;
    }

    /** Returns the world currently presented by the desktop host. */
    T current() {
        if (current == null) {
            throw new IllegalStateException("desktop session lifecycle has not started");
        }
        return current;
    }

    /** Returns whether a retained gameplay world can be resumed from the current menu. */
    boolean canResume() {
        return gameplay != null && current == menu;
    }

    /** Applies one application command using lazy world construction only when required. */
    boolean apply(ApplicationCommand command, Supplier<T> menuLoader, Supplier<T> gameplayLoader) {
        Objects.requireNonNull(command, "command");
        Supplier<T> validMenuLoader = Objects.requireNonNull(menuLoader, "menuLoader");
        Supplier<T> validGameplayLoader = Objects.requireNonNull(gameplayLoader, "gameplayLoader");
        return switch (command) {
            case SHOW_MENU -> {
                showMenu(validMenuLoader);
                yield true;
            }
            case NEW_GAME -> {
                newGame(validGameplayLoader);
                yield true;
            }
            case RESUME -> {
                resume();
                yield true;
            }
            case QUIT -> false;
        };
    }

    /** Opens a fresh menu only while distinct startup-menu semantics and gameplay are active. */
    private void showMenu(Supplier<T> menuLoader) {
        if (!startupMenu || gameplay == null || current != gameplay) {
            return;
        }
        menu = Objects.requireNonNull(menuLoader.get(), "loaded menu");
        current = menu;
    }

    /** Loads a replacement before closing either prior world, preserving state on load failure. */
    private void newGame(Supplier<T> gameplayLoader) {
        T replacement = Objects.requireNonNull(gameplayLoader.get(), "loaded gameplay");
        try {
            closeMenu();
            closeGameplay();
        } catch (RuntimeException failure) {
            closeAfterFailure(replacement, failure);
            throw failure;
        }
        gameplay = replacement;
        current = gameplay;
    }

    /** Returns to retained gameplay and closes the transient menu. */
    private void resume() {
        if (!canResume()) {
            return;
        }
        closeMenu();
        current = gameplay;
    }

    @Override
    public void close() {
        current = null;
        @Nullable RuntimeException failure = null;
        try {
            closeMenu();
        } catch (RuntimeException closeFailure) {
            failure = closeFailure;
        }
        try {
            closeGameplay();
        } catch (RuntimeException closeFailure) {
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /** Closes and forgets the menu world. */
    private void closeMenu() {
        if (menu != null) {
            close(menu);
            menu = null;
        }
    }

    /** Closes and forgets the gameplay world. */
    private void closeGameplay() {
        if (gameplay != null) {
            close(gameplay);
            gameplay = null;
        }
    }

    /** Adapts the close contract to this module's unchecked lifecycle surface. */
    private static void close(AutoCloseable value) {
        try {
            value.close();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("desktop world could not close", failure);
        }
    }

    /** Closes a rejected replacement without hiding the original ownership failure. */
    private static void closeAfterFailure(AutoCloseable replacement, RuntimeException failure) {
        try {
            close(replacement);
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
