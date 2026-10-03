/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Controlled result of attempting to replace realized Scene View content. */
record SceneViewRealizationResult(Status status, OptionalLong currentRevision, List<Diagnostic> diagnostics) {
    SceneViewRealizationResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(currentRevision, "currentRevision");
        diagnostics = List.copyOf(diagnostics);
        if ((status == Status.FAILED) != !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("only failed realization results contain diagnostics");
        }
    }

    static SceneViewRealizationResult success(Status status, long revision) {
        return new SceneViewRealizationResult(status, OptionalLong.of(revision), List.of());
    }

    static SceneViewRealizationResult failure(OptionalLong currentRevision, Diagnostic diagnostic) {
        return new SceneViewRealizationResult(Status.FAILED, currentRevision, List.of(diagnostic));
    }

    enum Status {
        APPLIED,
        UNCHANGED,
        STALE,
        FAILED
    }

    /** One renderer-safe realization failure without exposing an exception type. */
    record Diagnostic(Code code, String message, Optional<ResourceReference> resource) {
        Diagnostic {
            Objects.requireNonNull(code, "code");
            if (Objects.requireNonNull(message, "message").isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
            Objects.requireNonNull(resource, "resource");
        }
    }

    enum Code {
        SCENE_MISMATCH,
        INVALID_SNAPSHOT,
        RESOURCE_UNSUPPORTED,
        RESOURCE_FAILURE,
        CLEANUP_FAILURE
    }
}
