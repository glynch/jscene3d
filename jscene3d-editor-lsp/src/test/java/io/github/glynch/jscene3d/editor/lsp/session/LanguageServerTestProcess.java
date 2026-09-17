/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * Provides an in-memory process transport for language-server protocol tests.
 *
 * <p>The process streams are connected to corresponding server streams so a test language server
 * can communicate with an LSP client without starting an operating-system process.
 */
final class LanguageServerTestProcess extends Process {

    private final PipedInputStream clientInput = new PipedInputStream();
    private final PipedOutputStream serverOutput;
    private final PipedInputStream serverInput = new PipedInputStream();
    private final PipedOutputStream clientOutput;
    private final CountDownLatch terminated = new CountDownLatch(1);
    private final CompletableFuture<Process> exit = new CompletableFuture<>();

    /** Creates a connected in-memory process transport. */
    LanguageServerTestProcess() throws IOException {
        serverOutput = new PipedOutputStream(clientInput);
        clientOutput = new PipedOutputStream(serverInput);
    }

    /**
     * Returns the stream from which the test language server reads client messages.
     *
     * @return the server input stream
     */
    InputStream serverInput() {
        return serverInput;
    }

    /**
     * Returns the stream to which the test language server writes responses.
     *
     * @return the server output stream
     */
    OutputStream serverOutput() {
        return serverOutput;
    }

    @Override
    public OutputStream getOutputStream() {
        return clientOutput;
    }

    @Override
    public InputStream getInputStream() {
        return clientInput;
    }

    @Override
    public InputStream getErrorStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public int waitFor() throws InterruptedException {
        terminated.await();
        return exitValue();
    }

    @Override
    public int exitValue() {
        if (isAlive()) {
            throw new IllegalThreadStateException("Process is still running");
        }
        return 0;
    }

    @Override
    public void destroy() {
        terminate();
    }

    @Override
    public Process destroyForcibly() {
        terminate();
        return this;
    }

    @Override
    public boolean isAlive() {
        return terminated.getCount() != 0;
    }

    @Override
    public CompletableFuture<Process> onExit() {
        return exit;
    }

    private void terminate() {
        if (exit.complete(this)) {
            terminated.countDown();
        }
    }
}
