/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

/** A removable synchronous authoring change subscription. */
@FunctionalInterface
public interface AuthoringSubscription extends AutoCloseable {
    /** Removes the subscription; repeated calls have no effect. */
    @Override
    void close();
}
