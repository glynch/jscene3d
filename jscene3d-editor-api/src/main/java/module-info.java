/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Toolkit-independent extension and view contracts for the JScene3D editor. */
module io.github.glynch.jscene3d.editor.api {
    requires transitive io.github.glynch.jscene3d.configuration;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.activity;
    exports io.github.glynch.jscene3d.editor.command;
    exports io.github.glynch.jscene3d.editor.configuration;
    exports io.github.glynch.jscene3d.editor.context;
    exports io.github.glynch.jscene3d.editor.diagnostic;
    exports io.github.glynch.jscene3d.editor.extension;
    exports io.github.glynch.jscene3d.editor.file;
    exports io.github.glynch.jscene3d.editor.lifecycle;
    exports io.github.glynch.jscene3d.editor.menu;
    exports io.github.glynch.jscene3d.editor.project;
    exports io.github.glynch.jscene3d.editor.selection;
    exports io.github.glynch.jscene3d.editor.status;
    exports io.github.glynch.jscene3d.editor.view;
    exports io.github.glynch.jscene3d.editor.window;
    exports io.github.glynch.jscene3d.editor.workingcopy;
}
