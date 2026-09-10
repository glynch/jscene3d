/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.application.ApplicationCommand;
import io.github.glynch.jscene3d.game.application.ApplicationControl;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Desktop-session state shared by short-lived world-scoped application-control adapters. */
final class DesktopApplicationState {
    private @Nullable ApplicationCommand pending;
    private boolean resumeAvailable;

    /** Creates one world-owned adapter over this session's command mailbox. */
    ApplicationControl createControl() {
        return new Control();
    }

    /** Updates whether a subsequently activated menu may offer Resume. */
    void setResumeAvailable(boolean available) {
        resumeAvailable = available;
    }

    /** Takes at most one transition requested since the previous host frame. */
    Optional<ApplicationCommand> takeRequest() {
        ApplicationCommand result = pending;
        pending = null;
        return Optional.ofNullable(result);
    }

    /** One independently closeable world view of the shared desktop session. */
    private final class Control implements ApplicationControl {
        private boolean closed;

        @Override
        public boolean canResume() {
            requireOpen();
            return resumeAvailable;
        }

        @Override
        public void request(ApplicationCommand command) {
            requireOpen();
            if (pending == null) {
                pending = Objects.requireNonNull(command, "command");
            }
        }

        @Override
        public void close() {
            closed = true;
        }

        /** Rejects component access after its owning world has closed. */
        private void requireOpen() {
            if (closed) {
                throw new IllegalStateException("application control is closed");
            }
        }
    }
}
