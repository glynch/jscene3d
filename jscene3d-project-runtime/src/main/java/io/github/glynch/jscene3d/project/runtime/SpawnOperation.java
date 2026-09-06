/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.runtime.internal.InternalSpawnOperation;
import java.util.List;
import java.util.Optional;

/**
 * Observable result of one transactional request to spawn a prepared definition.
 *
 * <p>No entity is exposed while the request is pending or when it fails. This interface is not thread-safe; callers
 * inspect and cancel it on the owning world's logical simulation thread.
 */
public sealed interface SpawnOperation permits InternalSpawnOperation {
    /**
     * Returns the current terminal or pending state.
     *
     * @return operation state
     */
    SpawnStatus status();

    /**
     * Returns the completely activated root only after successful publication.
     *
     * @return spawned root when {@link #status()} is {@link SpawnStatus#ACTIVE}
     */
    Optional<Entity> entity();

    /**
     * Returns ordered structured diagnostics only after failed composition.
     *
     * @return immutable failure diagnostics
     */
    List<ProjectDiagnostic> diagnostics();

    /**
     * Cancels a request which has not begun its structural commit.
     *
     * @return {@code true} when this call changed a pending request to cancelled
     */
    boolean cancel();
}
