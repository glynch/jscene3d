/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

/** Owned lifetime of one language adapter within one open project. */
@FunctionalInterface
public interface EditorLanguageProjectSession extends AutoCloseable {
    /** Stops the project language session; repeated closure must have no additional effect. */
    @Override
    void close();
}
