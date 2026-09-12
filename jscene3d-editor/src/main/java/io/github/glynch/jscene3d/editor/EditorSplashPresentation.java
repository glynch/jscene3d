/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

/** Identifies whether the splash is presenting cold startup or an in-session project load. */
enum EditorSplashPresentation {
    /** The editor is starting. */
    STARTUP,

    /** A project is loading in an editor that is already running. */
    PROJECT_LOADING
}
