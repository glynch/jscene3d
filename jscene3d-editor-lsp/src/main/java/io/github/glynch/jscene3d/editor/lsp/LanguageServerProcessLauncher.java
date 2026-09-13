/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/** Starts language servers asynchronously without exposing {@link ProcessBuilder} to adapters. */
public final class LanguageServerProcessLauncher {
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(3);

    private final Executor executor;
    private final Duration shutdownTimeout;
    private final ProcessStarter processStarter;

    /**
     * Creates a launcher whose process startup and bounded shutdown use the supplied executor.
     *
     * @param executor caller-owned background executor
     */
    public LanguageServerProcessLauncher(Executor executor) {
        this(executor, DEFAULT_SHUTDOWN_TIMEOUT, ProcessBuilder::start);
    }

    LanguageServerProcessLauncher(Executor executor, Duration shutdownTimeout, ProcessStarter processStarter) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
        this.processStarter = Objects.requireNonNull(processStarter, "processStarter");
    }

    /**
     * Starts one child process on the caller-owned executor.
     *
     * @param configuration complete launch configuration
     * @return eventual owned child-process connection
     */
    public CompletionStage<LanguageServerProcess> launch(LanguageServerProcessConfiguration configuration) {
        LanguageServerProcessConfiguration launch = Objects.requireNonNull(configuration, "configuration");
        return CompletableFuture.supplyAsync(() -> start(launch), executor);
    }

    private LanguageServerProcess start(LanguageServerProcessConfiguration configuration) {
        try {
            Files.createDirectories(configuration.errorLog().getParent());
            ProcessBuilder builder = new ProcessBuilder(configuration.command())
                    .directory(configuration.workingDirectory().toFile())
                    .redirectError(ProcessBuilder.Redirect.appendTo(
                            configuration.errorLog().toFile()));
            return new LanguageServerProcess(processStarter.start(builder), executor, shutdownTimeout);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not start language server", failure);
        }
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
