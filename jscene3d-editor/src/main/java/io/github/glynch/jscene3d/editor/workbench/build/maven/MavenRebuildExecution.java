/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

/** Commits successful clean-build output and rolls other outcomes back to the prior output. */
final class MavenRebuildExecution implements ProjectBuildExecution {
    private final ProjectBuildExecution delegate;
    private final CompletionStage<ProjectBuildResult> completion;

    MavenRebuildExecution(ProjectBuildExecution delegate, MavenBuildOutputBackup backup) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        MavenBuildOutputBackup output = Objects.requireNonNull(backup, "backup");
        completion = delegate.completion().handle((result, failure) -> finish(output, result, failure));
    }

    @Override
    public CompletionStage<ProjectBuildResult> completion() {
        return completion;
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }

    private static ProjectBuildResult finish(
            MavenBuildOutputBackup output, @Nullable ProjectBuildResult result, @Nullable Throwable failure) {
        try {
            if (failure == null && result != null && result.outcome() == ProjectBuildOutcome.SUCCEEDED) {
                output.commit();
            } else {
                output.rollback();
            }
        } catch (IOException rollbackFailure) {
            if (failure != null) {
                rollbackFailure.addSuppressed(failure);
            }
            throw new CompletionException(
                    new UncheckedIOException("could not restore Maven build output", rollbackFailure));
        }
        if (failure != null) {
            throw new CompletionException(failure);
        }
        return Objects.requireNonNull(result, "build result");
    }
}
