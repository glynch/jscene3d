/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LanguageServerProcessLauncherTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void launchesOnlyWhenTheBackgroundExecutorRuns() {
        QueuedExecutor executor = new QueuedExecutor();
        StubProcess child = new StubProcess();
        LanguageServerProcessLauncher launcher =
                new LanguageServerProcessLauncher(executor, Duration.ZERO, ignored -> child);

        CompletionStage<LanguageServerProcess> launch = launcher.launch(configuration());
        assertThat(launch.toCompletableFuture()).isNotCompleted();

        executor.runNext();
        LanguageServerProcess server = launch.toCompletableFuture().join();
        assertThat(server.isAlive()).isTrue();

        server.close();
        executor.runNext();
        assertThat(child.destroyed).isTrue();
    }

    private LanguageServerProcessConfiguration configuration() {
        return new LanguageServerProcessConfiguration(
                List.of("language-server"), temporaryDirectory, temporaryDirectory.resolve("logs/server.log"));
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runNext() {
            tasks.remove().run();
        }
    }

    private static final class StubProcess extends Process {
        private final OutputStream input = new ByteArrayOutputStream();
        private boolean destroyed;

        @Override
        public OutputStream getOutputStream() {
            return input;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            destroyed = true;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return destroyed;
        }

        @Override
        public int exitValue() {
            if (!destroyed) {
                throw new IllegalThreadStateException("process is still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public boolean isAlive() {
            return !destroyed;
        }
    }
}
