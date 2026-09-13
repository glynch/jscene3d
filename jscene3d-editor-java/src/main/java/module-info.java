/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Bundled Eclipse JDT Language Server integration for the JScene3D editor. */
module io.github.glynch.jscene3d.editor.javalanguage {
    requires io.github.glynch.jscene3d.core;
    requires transitive io.github.glynch.jscene3d.editor.api;
    requires io.github.glynch.jscene3d.editor.lsp;
    requires io.github.glynch.jscene3d.i18n;
    requires org.eclipse.lsp4j.jsonrpc;

    exports io.github.glynch.jscene3d.editor.javalanguage;

    opens io.github.glynch.jscene3d.editor.javalanguage to
            io.github.glynch.jscene3d.i18n;
    opens io.github.glynch.jscene3d.editor.javalanguage.jdt to
            com.google.gson,
            org.eclipse.lsp4j.jsonrpc;
}
