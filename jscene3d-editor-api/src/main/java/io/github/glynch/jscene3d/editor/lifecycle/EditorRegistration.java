/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lifecycle;

/** A contribution or subscription which can be removed without a checked failure. */
@FunctionalInterface
public interface EditorRegistration extends AutoCloseable {
    /** Removes this registration; repeated closure must have no additional effect. */
    @Override
    void close();
}
