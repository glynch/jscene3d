/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.language.EditorTextDocument;
import io.github.glynch.jscene3d.editor.language.EditorTextDocumentChange;
import io.github.glynch.jscene3d.editor.lsp.client.LanguageServerInitialization;
import io.github.glynch.jscene3d.editor.lsp.client.LspClientSession;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcess;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessConfiguration;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Asynchronously prepares and launches one project's JDT LS child process. */
final class JdtLanguageProjectSession implements EditorLanguageProjectSession {
    private static final System.Logger LOGGER = System.getLogger(JdtLanguageProjectSession.class.getName());
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 3;

    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean failureReported = new AtomicBoolean();
    private final Consumer<JdtLanguageServerStatus> status;
    private final JdtDiagnosticPublisher diagnostics;
    private final Map<URI, EditorTextDocument> openDocuments = new LinkedHashMap<>();
    private final CompletableFuture<LanguageServerProcess> process;
    private final CompletableFuture<LspClientSession> protocol;
    private @Nullable LspClientSession connection;

    JdtLanguageProjectSession(
            Path projectRoot,
            String projectName,
            Consumer<JdtLanguageServerStatus> status,
            JdtDiagnosticPublisher diagnostics,
            Runtime runtime) {
        this.status = Objects.requireNonNull(status, "status");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        JdtLanguageClient languageClient = new JdtLanguageClient(status, diagnostics::publish);
        process = CompletableFuture.supplyAsync(() -> prepare(projectRoot, runtime), runtime.executor())
                .thenCompose(runtime.launcher()::launch)
                .toCompletableFuture();
        protocol = process.thenApply(
                running -> connect(running, projectRoot, projectName, runtime.clientVersion(), languageClient));
        protocol.thenCompose(session -> session.initialized().thenApply(ignored -> session))
                .whenComplete(this::protocolInitializationCompleted);
        process.thenAccept(running -> running.exitCode().whenComplete(this::processExited));
    }

    @Override
    public synchronized void didOpen(EditorTextDocument document) {
        EditorTextDocument current = Objects.requireNonNull(document, "document");
        openDocuments.put(current.resource(), current);
        diagnostics.documentVersion(current.resource(), current.version());
        if (connection != null) {
            connection.didOpen(current);
        }
    }

    @Override
    public synchronized void didChange(EditorTextDocumentChange change) {
        EditorTextDocumentChange currentChange = Objects.requireNonNull(change, "change");
        EditorTextDocument current = currentChange.document();
        boolean alreadyOpen = openDocuments.containsKey(current.resource());
        openDocuments.put(current.resource(), current);
        diagnostics.documentVersion(current.resource(), current.version());
        if (connection != null) {
            if (alreadyOpen) {
                connection.didChange(currentChange);
            } else {
                connection.didOpen(current);
            }
        }
    }

    @Override
    public synchronized void didSave(EditorTextDocument document) {
        EditorTextDocument current = Objects.requireNonNull(document, "document");
        openDocuments.put(current.resource(), current);
        diagnostics.documentVersion(current.resource(), current.version());
        if (connection != null) {
            connection.didSave(current);
        }
    }

    @Override
    public synchronized void didClose(EditorTextDocument document) {
        EditorTextDocument current = Objects.requireNonNull(document, "document");
        openDocuments.remove(current.resource());
        diagnostics.documentClosed(current.resource());
        if (connection != null) {
            connection.didClose(current);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        status.accept(JdtLanguageServerStatus.inactive());
        closeDocuments();
        diagnostics.clear();
        LspClientSession clientSession = completedValue(protocol);
        LanguageServerProcess running = completedValue(process);
        if (clientSession == null || running == null) {
            if (running != null) {
                running.close();
            }
            return;
        }
        clientSession
                .shutdown()
                .toCompletableFuture()
                .orTimeout(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ignored, failure) -> close(clientSession, running));
    }

    private static LanguageServerProcessConfiguration prepare(Path projectRoot, Runtime runtime) {
        try {
            JdtLanguageServerDistribution distribution =
                    JdtLanguageServerDistribution.fromHome(runtime.distributionHome(), runtime.operatingSystem());
            JdtLanguageServerProjectLayout layout = JdtLanguageServerProjectLayout.prepare(
                    projectRoot, runtime.cacheRoot(), distribution, runtime.metadata());
            return JdtLanguageServerCommand.create(projectRoot, distribution, layout, runtime.operatingSystem());
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not prepare JDT LS for " + projectRoot, failure);
        }
    }

    private LspClientSession connect(
            LanguageServerProcess running,
            Path projectRoot,
            String projectName,
            String clientVersion,
            JdtLanguageClient languageClient) {
        if (closed.get()) {
            running.close();
            throw new IllegalStateException("Java language session was closed during startup");
        }
        return LspClientSession.connect(
                running.serverOutput(),
                running.serverInput(),
                new LanguageServerInitialization(projectRoot, projectName, "JScene3D Editor", clientVersion),
                languageClient);
    }

    private void protocolInitializationCompleted(LspClientSession initializedSession, Throwable failure) {
        if (closed.get()) {
            return;
        }
        if (failure == null) {
            openDocuments(initializedSession);
            return;
        }
        LOGGER.log(System.Logger.Level.ERROR, "Could not initialize Eclipse JDT Language Server", failure);
        reportFailure(failureMessage(failure));
        closeFailedStartup();
    }

    private synchronized void openDocuments(LspClientSession initializedSession) {
        if (closed.get()) {
            return;
        }
        connection = Objects.requireNonNull(initializedSession, "initializedSession");
        List.copyOf(openDocuments.values()).forEach(connection::didOpen);
    }

    private synchronized void closeDocuments() {
        if (connection != null) {
            List.copyOf(openDocuments.values()).forEach(connection::didClose);
        }
        openDocuments.clear();
        connection = null;
    }

    private void processExited(Integer exitCode, Throwable failure) {
        if (closed.get()) {
            return;
        }
        String detail = failure == null ? "JDT LS exited unexpectedly with code " + exitCode : failureMessage(failure);
        reportFailure(detail);
    }

    private void closeFailedStartup() {
        LspClientSession clientSession = completedValue(protocol);
        if (clientSession != null) {
            clientSession.close();
        }
        LanguageServerProcess running = completedValue(process);
        if (running != null) {
            running.close();
        }
    }

    private static void close(LspClientSession clientSession, LanguageServerProcess running) {
        clientSession.close();
        running.close();
    }

    private void reportFailure(String detail) {
        if (failureReported.compareAndSet(false, true)) {
            status.accept(JdtLanguageServerStatus.failed(detail));
        }
    }

    private static <T> T completedValue(CompletableFuture<T> future) {
        return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()
                ? future.getNow(null)
                : null;
    }

    private static String failureMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    /** Runtime dependencies shared by one project's preparation, process and protocol phases. */
    record Runtime(
            Path distributionHome,
            Path cacheRoot,
            JdtLanguageServerMetadata metadata,
            OperatingSystem operatingSystem,
            Executor executor,
            LanguageServerProcessLauncher launcher,
            String clientVersion) {
        Runtime {
            Objects.requireNonNull(distributionHome, "distributionHome");
            Objects.requireNonNull(cacheRoot, "cacheRoot");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(operatingSystem, "operatingSystem");
            Objects.requireNonNull(executor, "executor");
            Objects.requireNonNull(launcher, "launcher");
            Objects.requireNonNull(clientVersion, "clientVersion");
        }
    }
}
