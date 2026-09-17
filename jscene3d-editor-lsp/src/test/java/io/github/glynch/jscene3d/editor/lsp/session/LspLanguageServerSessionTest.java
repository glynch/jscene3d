/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.lsp.client.DefaultLanguageClient;
import io.github.glynch.jscene3d.editor.lsp.client.LanguageServerInitialization;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcess;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessTestFactory;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LspLanguageServerSessionTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void connectsToRunningLanguageServerProcess() throws Exception {
        LanguageServerTestProcess testProcess = new LanguageServerTestProcess();

        try (ExecutorService processExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService protocolExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            RecordingLanguageServer server = new RecordingLanguageServer(testProcess);

            try (ServerConnection serverConnection = startServer(testProcess, server, serverExecutor)) {
                LanguageServerProcess process = LanguageServerProcessTestFactory.create(testProcess, processExecutor);
                LanguageServerInitialization initialization = initialization();

                try (LspLanguageServerSession session = LspLanguageServerSession.connect(
                                process, initialization, new DefaultLanguageClient(), protocolExecutor)
                        .toCompletableFuture()
                        .get(2, TimeUnit.SECONDS)) {
                    assertThat(server.initialized.await(2, TimeUnit.SECONDS)).isTrue();
                    assertThat(server.initializeParameters.get().getRootUri())
                            .isEqualTo(temporaryDirectory
                                    .toAbsolutePath()
                                    .normalize()
                                    .toUri()
                                    .toString());
                    assertThat(server.initializeParameters.get().getClientInfo().getName())
                            .isEqualTo("Test Editor");
                    assertThat(server.initializeParameters.get().getClientInfo().getVersion())
                            .isEqualTo("1.2.3");
                    assertThat(session.client().initialized().toCompletableFuture())
                            .isCompleted();
                    assertThat(testProcess.isAlive()).isTrue();
                }

                assertThat(testProcess.isAlive()).isFalse();
                assertThat(process.exitCode().toCompletableFuture().get(2, TimeUnit.SECONDS))
                        .isZero();
                assertThat(protocolExecutor.isShutdown()).isFalse();
            }
        }
    }

    @Test
    void closesProcessWhenInitializationFails() throws Exception {
        LanguageServerTestProcess testProcess = new LanguageServerTestProcess();

        try (ExecutorService processExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService protocolExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            FailingLanguageServer server = new FailingLanguageServer(testProcess);

            try (ServerConnection serverConnection = startServer(testProcess, server, serverExecutor)) {
                LanguageServerProcess process = LanguageServerProcessTestFactory.create(testProcess, processExecutor);
                CompletableFuture<LspLanguageServerSession> connection = LspLanguageServerSession.connect(
                                process, initialization(), new DefaultLanguageClient(), protocolExecutor)
                        .toCompletableFuture();

                assertThatThrownBy(() -> connection.get(2, TimeUnit.SECONDS))
                        .hasRootCauseInstanceOf(ResponseErrorException.class);

                assertThat(testProcess.isAlive()).isFalse();
                assertThat(process.exitCode().toCompletableFuture().get(2, TimeUnit.SECONDS))
                        .isZero();
                assertThat(protocolExecutor.isShutdown()).isFalse();
            }
        }
    }

    @Test
    void shutsDownLanguageServerBeforeTerminatingProcess() throws Exception {
        LanguageServerTestProcess testProcess = new LanguageServerTestProcess();

        try (ExecutorService processExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService protocolExecutor = Executors.newVirtualThreadPerTaskExecutor();
                ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            RecordingLanguageServer server = new RecordingLanguageServer(testProcess);

            try (ServerConnection serverConnection = startServer(testProcess, server, serverExecutor)) {
                LanguageServerProcess process = LanguageServerProcessTestFactory.create(testProcess, processExecutor);
                LspLanguageServerSession session = LspLanguageServerSession.connect(
                                process, initialization(), new DefaultLanguageClient(), protocolExecutor)
                        .toCompletableFuture()
                        .get(2, TimeUnit.SECONDS);

                assertThat(server.initialized.await(2, TimeUnit.SECONDS)).isTrue();

                session.close();

                assertThat(server.shutdownReceived.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(server.processAliveDuringShutdown.get()).isTrue();
                assertThat(testProcess.isAlive()).isFalse();
                assertThat(protocolExecutor.isShutdown()).isFalse();
            }
        }
    }

    private LanguageServerInitialization initialization() {
        return new LanguageServerInitialization(temporaryDirectory, "Example", "Test Editor", "1.2.3");
    }

    private static ServerConnection startServer(
            LanguageServerTestProcess process, LanguageServer server, ExecutorService executor) {
        Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
                .setLocalService(server)
                .setRemoteInterface(LanguageClient.class)
                .setInput(process.serverInput())
                .setOutput(process.serverOutput())
                .setExecutorService(executor)
                .create();
        return new ServerConnection(launcher.startListening());
    }

    private static final class ServerConnection implements AutoCloseable {

        private final Future<Void> listener;

        private ServerConnection(Future<Void> listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            listener.cancel(true);
        }
    }

    private static class RecordingLanguageServer implements LanguageServer {

        private final LanguageServerTestProcess process;
        private final AtomicReference<InitializeParams> initializeParameters = new AtomicReference<>();
        private final CountDownLatch initialized = new CountDownLatch(1);
        private final CountDownLatch shutdownReceived = new CountDownLatch(1);
        private final CountDownLatch exitReceived = new CountDownLatch(1);
        private final AtomicBoolean processAliveDuringShutdown = new AtomicBoolean();
        private final AtomicBoolean processAliveDuringExit = new AtomicBoolean();
        private final TextDocumentService documents = new InertTextDocumentService();
        private final WorkspaceService workspace = new InertWorkspaceService();

        private RecordingLanguageServer(LanguageServerTestProcess process) {
            this.process = process;
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams parameters) {
            initializeParameters.set(parameters);
            return CompletableFuture.completedFuture(new InitializeResult(new ServerCapabilities()));
        }

        @Override
        public void initialized(InitializedParams parameters) {
            initialized.countDown();
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            processAliveDuringShutdown.set(process.isAlive());
            shutdownReceived.countDown();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
            processAliveDuringExit.set(process.isAlive());
            exitReceived.countDown();
        }

        @Override
        public TextDocumentService getTextDocumentService() {
            return documents;
        }

        @Override
        public WorkspaceService getWorkspaceService() {
            return workspace;
        }
    }

    private static final class FailingLanguageServer extends RecordingLanguageServer {

        private FailingLanguageServer(LanguageServerTestProcess process) {
            super(process);
        }

        @Override
        public CompletableFuture<InitializeResult> initialize(InitializeParams parameters) {
            ResponseError error = new ResponseError(ResponseErrorCode.RequestFailed, "Initialization failed", null);
            return CompletableFuture.failedFuture(new ResponseErrorException(error));
        }
    }

    private static final class InertTextDocumentService implements TextDocumentService {

        @Override
        public void didOpen(DidOpenTextDocumentParams parameters) {
            throw unexpectedNotification("textDocument/didOpen");
        }

        @Override
        public void didChange(DidChangeTextDocumentParams parameters) {
            throw unexpectedNotification("textDocument/didChange");
        }

        @Override
        public void didClose(DidCloseTextDocumentParams parameters) {
            throw unexpectedNotification("textDocument/didClose");
        }

        @Override
        public void didSave(DidSaveTextDocumentParams parameters) {
            throw unexpectedNotification("textDocument/didSave");
        }
    }

    private static final class InertWorkspaceService implements WorkspaceService {

        @Override
        public void didChangeConfiguration(DidChangeConfigurationParams parameters) {
            throw unexpectedNotification("workspace/didChangeConfiguration");
        }

        @Override
        public void didChangeWatchedFiles(DidChangeWatchedFilesParams parameters) {
            throw unexpectedNotification("workspace/didChangeWatchedFiles");
        }
    }

    private static AssertionError unexpectedNotification(String method) {
        return new AssertionError("Unexpected LSP notification: " + method);
    }
}
