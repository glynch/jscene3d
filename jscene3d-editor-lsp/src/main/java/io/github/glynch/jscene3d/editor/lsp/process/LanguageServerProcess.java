/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owned input/output connection and bounded lifetime of one language-server child process. */
public final class LanguageServerProcess implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(LanguageServerProcess.class.getName());

    private final Process process;
    private final Executor shutdownExecutor;
    private final Duration shutdownTimeout;
    private final AtomicBoolean closed = new AtomicBoolean();

    LanguageServerProcess(Process process, Executor shutdownExecutor, Duration shutdownTimeout) {
        this.process = Objects.requireNonNull(process, "process");
        this.shutdownExecutor = Objects.requireNonNull(shutdownExecutor, "shutdownExecutor");
        this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
    }

    /**
     * Returns protocol bytes written by the server.
     *
     * @return server standard output
     */
    public InputStream serverOutput() {
        return process.getInputStream();
    }

    /**
     * Returns the stream used to write protocol bytes to the server.
     *
     * @return server standard input
     */
    public OutputStream serverInput() {
        return process.getOutputStream();
    }

    /**
     * Returns completion of the child process.
     *
     * @return eventual process exit code
     */
    public CompletionStage<Integer> exitCode() {
        return process.onExit().thenApply(Process::exitValue);
    }

    /**
     * Returns whether the child process is currently alive.
     *
     * @return {@code true} while the server is running
     */
    public boolean isAlive() {
        return process.isAlive();
    }

    /** Closes protocol input, requests process termination, and bounds forced cleanup. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeProtocolInput();
        process.destroy();
        shutdownExecutor.execute(this::awaitTermination);
    }

    private void closeProtocolInput() {
        try {
            process.getOutputStream().close();
        } catch (IOException failure) {
            LOGGER.log(System.Logger.Level.DEBUG, "Could not close language-server input", failure);
        }
    }

    private void awaitTermination() {
        try {
            if (!process.waitFor(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
