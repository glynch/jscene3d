/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Creates idempotent editor registrations. */
final class EditorRegistrationOnce {
    private EditorRegistrationOnce() {}

    static EditorRegistration of(Runnable removal) {
        Runnable action = Objects.requireNonNull(removal, "removal");
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                action.run();
            }
        };
    }
}
