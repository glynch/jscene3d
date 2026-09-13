/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LspClientSessionTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void initializesAndShutsDownAWorkspaceOverJsonRpc() throws Exception {
        try (ProtocolPair protocol = new ProtocolPair()) {
            RecordingLanguageServer server = new RecordingLanguageServer();
            protocol.startServer(server);
            LanguageServerInitialization initialization =
                    new LanguageServerInitialization(temporaryDirectory, "Example", "Test Editor", "1.2.3");

            try (LspClientSession client =
                    LspClientSession.connect(protocol.clientInput(), protocol.clientOutput(), initialization)) {
                client.initialized().toCompletableFuture().get(2, TimeUnit.SECONDS);
                assertThat(server.initialized.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(server.initializeParameters.get().getRootUri())
                        .isEqualTo(temporaryDirectory
                                .toAbsolutePath()
                                .normalize()
                                .toUri()
                                .toString());
                assertThat(server.initializeParameters.get().getWorkspaceFolders())
                        .singleElement()
                        .satisfies(folder -> {
                            assertThat(folder.getName()).isEqualTo("Example");
                            assertThat(folder.getUri())
                                    .isEqualTo(temporaryDirectory.toUri().toString());
                        });
                assertThat(server.initializeParameters.get().getClientInfo().getName())
                        .isEqualTo("Test Editor");
                assertThat(server.initializeParameters.get().getClientInfo().getVersion())
                        .isEqualTo("1.2.3");

                client.shutdown().toCompletableFuture().get(2, TimeUnit.SECONDS);
                assertThat(server.exited.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(server.shutdown.get()).isTrue();
            }
        }
    }

    @Test
    void routesServerCallbacksToTheProvidedLanguageClient() throws Exception {
        try (ProtocolPair protocol = new ProtocolPair()) {
            RecordingLanguageServer server = new RecordingLanguageServer();
            LanguageClient serverClient = protocol.startServer(server);
            CountDownLatch messageReceived = new CountDownLatch(1);
            AtomicReference<String> message = new AtomicReference<>();
            DefaultLanguageClient languageClient = new DefaultLanguageClient() {
                @Override
                public void showMessage(MessageParams parameters) {
                    message.set(parameters.getMessage());
                    messageReceived.countDown();
                }
            };
            LanguageServerInitialization initialization =
                    new LanguageServerInitialization(temporaryDirectory, "Example", "Test Editor", "1.2.3");

            try (LspClientSession client = LspClientSession.connect(
                    protocol.clientInput(), protocol.clientOutput(), initialization, languageClient)) {
                client.initialized().toCompletableFuture().get(2, TimeUnit.SECONDS);
                serverClient.showMessage(new MessageParams(MessageType.Info, "Connected"));

                assertThat(messageReceived.await(2, TimeUnit.SECONDS)).isTrue();
                assertThat(message).hasValue("Connected");
            }
        }
    }

    private static final class ProtocolPair implements AutoCloseable {
        private final PipedInputStream clientInput = new PipedInputStream();
        private final PipedOutputStream serverOutput;
        private final PipedInputStream serverInput = new PipedInputStream();
        private final PipedOutputStream clientOutput;
        private final ExecutorService serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        private Future<Void> serverListener;

        private ProtocolPair() throws IOException {
            serverOutput = new PipedOutputStream(clientInput);
            clientOutput = new PipedOutputStream(serverInput);
        }

        private LanguageClient startServer(LanguageServer server) {
            Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
                    .setLocalService(server)
                    .setRemoteInterface(LanguageClient.class)
                    .setInput(serverInput)
                    .setOutput(serverOutput)
                    .setExecutorService(serverExecutor)
                    .create();
            serverListener = launcher.startListening();
            return launcher.getRemoteProxy();
        }

        private PipedInputStream clientInput() {
            return clientInput;
        }

        private PipedOutputStream clientOutput() {
            return clientOutput;
        }

        @Override
        public void close() throws IOException {
            if (serverListener != null) {
                serverListener.cancel(true);
            }
            serverExecutor.shutdownNow();
            clientOutput.close();
            serverInput.close();
            serverOutput.close();
            clientInput.close();
        }
    }

    private static final class RecordingLanguageServer implements LanguageServer {
        private final AtomicReference<InitializeParams> initializeParameters = new AtomicReference<>();
        private final CountDownLatch initialized = new CountDownLatch(1);
        private final AtomicBoolean shutdown = new AtomicBoolean();
        private final CountDownLatch exited = new CountDownLatch(1);
        private final TextDocumentService documents = new InertTextDocumentService();
        private final WorkspaceService workspace = new InertWorkspaceService();

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
            shutdown.set(true);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
            exited.countDown();
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
