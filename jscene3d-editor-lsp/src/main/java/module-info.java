/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Reusable process and protocol support for editor language servers. */
module io.github.glynch.jscene3d.editor.lsp {
    requires com.google.gson;
    requires transitive io.github.glynch.jscene3d.editor.api;
    requires transitive org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.lsp.client;
    exports io.github.glynch.jscene3d.editor.lsp.process;
}
