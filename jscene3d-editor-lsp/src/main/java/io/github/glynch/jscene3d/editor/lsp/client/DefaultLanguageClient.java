/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.client;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

/** Default handling for standard language-client notifications and requests. */
public class DefaultLanguageClient implements LanguageClient {
    private final System.Logger logger;

    /** Creates a client whose protocol messages are logged under its concrete client type. */
    public DefaultLanguageClient() {
        logger = System.getLogger(getClass().getName());
    }

    @Override
    public void telemetryEvent(Object event) {
        logger.log(System.Logger.Level.DEBUG, "Language-server telemetry: {0}", event);
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        // Diagnostic translation is the next vertical-slice step.
    }

    @Override
    public void showMessage(MessageParams message) {
        logger.log(System.Logger.Level.INFO, "Language server: {0}", message.getMessage());
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams request) {
        logger.log(System.Logger.Level.INFO, "Language server requested user input: {0}", request.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams message) {
        logger.log(System.Logger.Level.DEBUG, "Language server: {0}", message.getMessage());
    }
}
