/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.session;

import io.github.glynch.jscene3d.editor.lsp.client.LanguageServerInitialization;
import io.github.glynch.jscene3d.editor.lsp.client.LspClientSession;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcess;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Owns a running language-server process and its initialized LSP client session.
 *
 * <p>The session represents one lifecycle unit consisting of an already-started external
 * language-server process and the protocol connection to that process. Language-specific
 * preparation and process launching remain the responsibility of the caller.
 *
 * <p>The executor supplied when connecting a session is used by the LSP client but remains owned
 * by the caller. Closing this session does not shut down that executor.
 */
public final class LspLanguageServerSession implements AutoCloseable {

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(3);

    private final LanguageServerProcess process;
    private final LspClientSession client;

    private LspLanguageServerSession(LanguageServerProcess process, LspClientSession client) {
        this.process = Objects.requireNonNull(process, "process");
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * Asynchronously establishes an LSP session with an already-running language-server process.
     *
     * <p>The returned stage completes successfully only after LSP protocol initialization has
     * completed. If initialization fails, the partially initialized client and owned process are
     * closed before the returned stage completes exceptionally.
     *
     * @param process the running language-server process owned by the resulting session
     * @param initialization the LSP initialization parameters
     * @param languageClient the client receiving callbacks from the language server
     * @param protocolExecutor the executor used for protocol message processing
     * @return a stage containing the initialized language-server session
     * @throws NullPointerException if any argument is {@code null}
     */
    public static CompletionStage<LspLanguageServerSession> connect(
            LanguageServerProcess process,
            LanguageServerInitialization initialization,
            LanguageClient languageClient,
            ExecutorService protocolExecutor) {
        LanguageServerProcess serverProcess = Objects.requireNonNull(process, "process");
        Objects.requireNonNull(initialization, "initialization");
        Objects.requireNonNull(languageClient, "languageClient");
        Objects.requireNonNull(protocolExecutor, "protocolExecutor");

        LspClientSession client = LspClientSession.connect(
                serverProcess.serverOutput(),
                serverProcess.serverInput(),
                initialization,
                languageClient,
                protocolExecutor);

        return client.initialized().handle((ignored, failure) -> {
            if (failure != null) {
                client.close();
                serverProcess.close();
                throw new LanguageServerInitializationException("Could not initialize language server", failure);
            }
            return new LspLanguageServerSession(serverProcess, client);
        });
    }

    /**
     * Returns the initialized LSP client session.
     *
     * @return the LSP client session
     */
    public LspClientSession client() {
        return client;
    }

    /**
     * Returns a stage completed with the language-server process exit code when the process
     * terminates.
     *
     * @return the process exit-code stage
     */
    public CompletionStage<Integer> exitCode() {
        return process.exitCode();
    }

    /**
     * Gracefully shuts down the LSP connection and closes the owned language-server process.
     *
     * <p>The method waits for a bounded period for the language server to acknowledge protocol
     * shutdown. The protocol connection and process are closed regardless of whether graceful
     * shutdown succeeds or times out. The caller-owned protocol executor is not shut down.
     */
    @Override
    public void close() {
        try {
            client.shutdown().toCompletableFuture().get(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception failure) {
            // Cleanup must continue when graceful protocol shutdown fails or times out.
        } finally {
            client.close();
            process.close();
        }
    }

    private static final class LanguageServerInitializationException extends RuntimeException {

        private LanguageServerInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
