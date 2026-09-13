/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ClientInfo;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/** Owns the JSON-RPC transport and initialization lifecycle for one language-server connection. */
public final class LspClientSession implements AutoCloseable {
    private final ExecutorService protocolExecutor;
    private final LanguageServer server;
    private final Future<Void> listener;
    private final CompletableFuture<Void> initialized;
    private final AtomicBoolean initializedSuccessfully = new AtomicBoolean();
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private LspClientSession(
            InputStream serverOutput,
            OutputStream serverInput,
            LanguageServerInitialization initialization,
            LanguageClient languageClient,
            ExecutorService protocolExecutor) {
        this.protocolExecutor = Objects.requireNonNull(protocolExecutor, "protocolExecutor");
        Launcher<LanguageServer> launcher = new LSPLauncher.Builder<LanguageServer>()
                .setLocalService(Objects.requireNonNull(languageClient, "languageClient"))
                .setRemoteInterface(LanguageServer.class)
                .setInput(Objects.requireNonNull(serverOutput, "serverOutput"))
                .setOutput(Objects.requireNonNull(serverInput, "serverInput"))
                .setExecutorService(protocolExecutor)
                .create();
        server = launcher.getRemoteProxy();
        listener = launcher.startListening();
        initialized = initialize(initialization);
    }

    /**
     * Starts listening and sends the LSP initialize request without blocking the caller.
     *
     * @param serverOutput language-server standard output read by the client
     * @param serverInput language-server standard input written by the client
     * @param initialization workspace and client identity sent with the initialize request
     * @return the connected client session
     */
    public static LspClientSession connect(
            InputStream serverOutput, OutputStream serverInput, LanguageServerInitialization initialization) {
        return connect(serverOutput, serverInput, initialization, new DefaultLanguageClient());
    }

    /**
     * Starts a connection using a language-specific client which may implement protocol extensions.
     *
     * @param serverOutput language-server standard output read by the client
     * @param serverInput language-server standard input written by the client
     * @param initialization workspace and client identity sent with the initialize request
     * @param languageClient standard and language-specific client callbacks
     * @return the connected client session
     */
    public static LspClientSession connect(
            InputStream serverOutput,
            OutputStream serverInput,
            LanguageServerInitialization initialization,
            LanguageClient languageClient) {
        return new LspClientSession(
                serverOutput,
                serverInput,
                Objects.requireNonNull(initialization, "initialization"),
                Objects.requireNonNull(languageClient, "languageClient"),
                Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Returns completion of the initialize response and subsequent initialized notification.
     *
     * @return asynchronous initialization completion
     */
    public CompletionStage<Void> initialized() {
        return initialized;
    }

    /**
     * Requests orderly LSP shutdown and sends the required exit notification.
     *
     * @return asynchronous shutdown-request completion
     */
    public CompletionStage<Void> shutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        if (!initializedSuccessfully.get()) {
            return CompletableFuture.completedFuture(null);
        }
        return server.shutdown().handle((ignored, failure) -> {
            server.exit();
            if (failure != null) {
                throw new LanguageServerProtocolException("Language-server shutdown failed", failure);
            }
            return null;
        });
    }

    /** Stops the client transport. The process owner remains responsible for closing the child process. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        listener.cancel(true);
        protocolExecutor.shutdownNow();
    }

    private CompletableFuture<Void> initialize(LanguageServerInitialization initialization) {
        InitializeParams parameters = new InitializeParams();
        String rootUri = initialization.projectRoot().toUri().toString();
        parameters.setProcessId(Math.toIntExact(ProcessHandle.current().pid()));
        parameters.setClientInfo(new ClientInfo(initialization.clientName(), initialization.clientVersion()));
        parameters.setRootUri(rootUri);
        parameters.setWorkspaceFolders(List.of(new WorkspaceFolder(rootUri, initialization.projectName())));
        parameters.setCapabilities(new ClientCapabilities());
        return server.initialize(parameters).thenRun(() -> {
            if (closed.get()) {
                throw new IllegalStateException("language-server client session is closed");
            }
            server.initialized(new InitializedParams());
            initializedSuccessfully.set(true);
        });
    }
}
