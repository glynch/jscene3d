/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Headless project loading, sessions, and authoring projections for JScene3D editors. */
module io.github.glynch.jscene3d.editor.authoring {
    requires io.github.glynch.jscene3d.configuration;
    requires io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.editor.api;
    requires transitive io.github.glynch.jscene3d.project;
    requires io.github.glynch.jscene3d.project.importing;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.diagnostics;
    exports io.github.glynch.jscene3d.editor.project.asset;
    exports io.github.glynch.jscene3d.editor.project.loading;
    exports io.github.glynch.jscene3d.editor.project.session;
    exports io.github.glynch.jscene3d.editor.workbench.hierarchy;
    exports io.github.glynch.jscene3d.editor.workbench.inspector;
    exports io.github.glynch.jscene3d.editor.workbench.workingcopy;
}
