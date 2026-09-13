/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Bundled Eclipse JDT Language Server integration for the JScene3D editor. */
module io.github.glynch.jscene3d.editor.javalanguage {
    requires io.github.glynch.jscene3d.core;
    requires io.github.glynch.jscene3d.editor.api;
    requires io.github.glynch.jscene3d.editor.lsp;

    exports io.github.glynch.jscene3d.editor.javalanguage;
}
