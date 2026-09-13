/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.lsp.LanguageServerProcess;
import io.github.glynch.jscene3d.editor.lsp.LanguageServerProcessConfiguration;
import io.github.glynch.jscene3d.editor.lsp.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Asynchronously prepares and launches one project's JDT LS child process. */
final class JdtLanguageProjectSession implements EditorLanguageProjectSession {
    private static final System.Logger LOGGER = System.getLogger(JdtLanguageProjectSession.class.getName());

    private final AtomicBoolean closed = new AtomicBoolean();
    private final CompletionStage<LanguageServerProcess> process;

    JdtLanguageProjectSession(
            Path projectRoot,
            Path distributionHome,
            JdtLanguageServerMetadata metadata,
            OperatingSystem operatingSystem,
            Executor executor,
            LanguageServerProcessLauncher launcher) {
        process = CompletableFuture.supplyAsync(
                        () -> prepare(projectRoot, distributionHome, metadata, operatingSystem), executor)
                .thenCompose(launcher::launch)
                .whenComplete(this::launchCompleted);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        LanguageServerProcess running = process.toCompletableFuture().getNow(null);
        if (running != null) {
            running.close();
        }
    }

    private static LanguageServerProcessConfiguration prepare(
            Path projectRoot,
            Path distributionHome,
            JdtLanguageServerMetadata metadata,
            OperatingSystem operatingSystem) {
        try {
            JdtLanguageServerDistribution distribution =
                    JdtLanguageServerDistribution.fromHome(distributionHome, operatingSystem);
            JdtLanguageServerProjectLayout layout =
                    JdtLanguageServerProjectLayout.prepare(projectRoot, distribution, metadata);
            return JdtLanguageServerCommand.create(projectRoot, distribution, layout, operatingSystem);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not prepare JDT LS for " + projectRoot, failure);
        }
    }

    private void launchCompleted(LanguageServerProcess running, Throwable failure) {
        if (failure != null) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not launch Eclipse JDT Language Server", failure);
        } else if (closed.get()) {
            running.close();
        }
    }
}
