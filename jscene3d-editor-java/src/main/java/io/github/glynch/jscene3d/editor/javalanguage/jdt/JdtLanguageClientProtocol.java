/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

/** Client callbacks defined by Eclipse JDT LS in addition to the standard language-server protocol. */
interface JdtLanguageClientProtocol {
    /** Receives the JDT language service's current lifecycle status. */
    @JsonNotification("language/status")
    void sendStatusReport(JdtStatusReport report);
}
