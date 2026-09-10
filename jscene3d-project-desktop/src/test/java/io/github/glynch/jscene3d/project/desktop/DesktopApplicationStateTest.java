/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.application.ApplicationCommand;
import io.github.glynch.jscene3d.game.application.ApplicationControl;
import org.junit.jupiter.api.Test;

/** Specifies the deferred application-command seam shared across desktop worlds. */
final class DesktopApplicationStateTest {
    @Test
    void exposesResumeStateAndRetainsOnlyTheFirstCommandPerFrame() {
        DesktopApplicationState state = new DesktopApplicationState();
        ApplicationControl control = state.createControl();

        assertThat(control.canResume()).isFalse();
        state.setResumeAvailable(true);
        assertThat(control.canResume()).isTrue();

        control.request(ApplicationCommand.NEW_GAME);
        control.request(ApplicationCommand.QUIT);
        assertThat(state.takeRequest()).contains(ApplicationCommand.NEW_GAME);
        assertThat(state.takeRequest()).isEmpty();
    }

    @Test
    void closesEachWorldAdapterIndependently() {
        DesktopApplicationState state = new DesktopApplicationState();
        ApplicationControl closed = state.createControl();
        ApplicationControl active = state.createControl();

        closed.close();

        assertThatThrownBy(closed::canResume).isInstanceOf(IllegalStateException.class);
        active.request(ApplicationCommand.RESUME);
        assertThat(state.takeRequest()).contains(ApplicationCommand.RESUME);
    }
}
