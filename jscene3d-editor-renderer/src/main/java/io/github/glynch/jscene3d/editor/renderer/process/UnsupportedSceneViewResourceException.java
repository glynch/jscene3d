/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

/** Signals that a resource lies outside the fixed first-version Scene View resource set. */
final class UnsupportedSceneViewResourceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    UnsupportedSceneViewResourceException(String message) {
        super(message);
    }
}
