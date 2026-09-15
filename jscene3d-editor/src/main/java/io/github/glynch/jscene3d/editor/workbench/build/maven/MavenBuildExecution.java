/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** Owns one Maven wrapper process and its captured terminal result. */
final class MavenBuildExecution implements ProjectBuildExecution {
    private static final Duration TERMINATION_GRACE_PERIOD = Duration.ofMillis(500);
    private static final Duration FORCED_TERMINATION_TIMEOUT = Duration.ofSeconds(2);

    private final Process process;
    private final Path projectRoot;
    private final List<String> command;
    private final long startedAtNanos = System.nanoTime();
    private final AtomicReference<@Nullable CompletableFuture<Void>> cancellation = new AtomicReference<>();
    private final CompletableFuture<String> standardOutput;
    private final CompletableFuture<String> standardError;
    private final CompletableFuture<ProjectBuildResult> completion = new CompletableFuture<>();

    private MavenBuildExecution(Process process, Path projectRoot, List<String> command) {
        this.process = process;
        this.projectRoot = projectRoot;
        this.command = command;
        standardOutput = read(process.getInputStream(), "jscene3d-maven-stdout");
        standardError = read(process.getErrorStream(), "jscene3d-maven-stderr");
        CompletableFuture.runAsync(this::await);
    }

    static MavenBuildExecution start(Path projectRoot, List<String> command) throws IOException {
        Process process =
                new ProcessBuilder(command).directory(projectRoot.toFile()).start();
        return new MavenBuildExecution(process, projectRoot.toAbsolutePath().normalize(), List.copyOf(command));
    }

    @Override
    public CompletableFuture<ProjectBuildResult> completion() {
        return completion;
    }

    @Override
    public void cancel() {
        CompletableFuture<Void> termination = new CompletableFuture<>();
        if (!cancellation.compareAndSet(null, termination)) {
            return;
        }
        List<ProcessHandle> processTree = Stream.concat(process.descendants(), Stream.of(process.toHandle()))
                .toList();
        CompletableFuture.runAsync(() -> terminate(processTree, termination));
    }

    private void await() {
        try {
            int exitCode = process.waitFor();
            CompletableFuture<Void> termination = cancellation.get();
            if (termination != null) {
                termination.join();
            }
            ProjectBuildOutcome outcome = outcome(exitCode, termination != null);
            String output = standardOutput.join();
            String error = standardError.join();
            completion.complete(new ProjectBuildResult(
                    outcome,
                    command,
                    output,
                    error,
                    Duration.ofNanos(System.nanoTime() - startedAtNanos),
                    MavenBuildDiagnostics.parse(projectRoot, output, error)));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            completion.completeExceptionally(failure);
        } catch (CompletionException failure) {
            completion.completeExceptionally(failure.getCause());
        }
    }

    private static CompletableFuture<String> read(InputStream input, String threadName) {
        return CompletableFuture.supplyAsync(() -> {
            try (input) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new UncheckedIOException(threadName, failure);
            }
        });
    }

    private static ProjectBuildOutcome outcome(int exitCode, boolean cancelled) {
        if (cancelled) {
            return ProjectBuildOutcome.CANCELLED;
        }
        return exitCode == 0 ? ProjectBuildOutcome.SUCCEEDED : ProjectBuildOutcome.FAILED;
    }

    private static void terminate(List<ProcessHandle> processTree, CompletableFuture<Void> completion) {
        try {
            processTree.forEach(ProcessHandle::destroy);
            awaitExit(processTree, TERMINATION_GRACE_PERIOD);
            processTree.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (!awaitExit(processTree, FORCED_TERMINATION_TIMEOUT)) {
                throw new IllegalStateException("Maven build process tree did not terminate");
            }
            completion.complete(null);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            completion.completeExceptionally(failure);
        } catch (RuntimeException failure) {
            completion.completeExceptionally(failure);
        }
    }

    private static boolean awaitExit(List<ProcessHandle> processes, Duration timeout) throws InterruptedException {
        CompletableFuture<?>[] exits = processes.stream()
                .filter(ProcessHandle::isAlive)
                .map(ProcessHandle::onExit)
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(exits).get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            return true;
        } catch (TimeoutException failure) {
            return processes.stream().noneMatch(ProcessHandle::isAlive);
        } catch (ExecutionException failure) {
            throw new IllegalStateException("could not observe Maven build process termination", failure.getCause());
        }
    }
}
