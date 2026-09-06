/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.SpawnOperation;
import io.github.glynch.jscene3d.project.runtime.SpawnStatus;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Mutable world-thread state of one queued spawn transaction. */
public final class InternalSpawnOperation implements SpawnOperation {
    private final InternalEntity owner;
    private final InternalPreparedEntityDefinition prepared;
    private final Map<PropertyId, ProjectValue> parameters;
    private SpawnStatus status = SpawnStatus.PENDING;
    private @Nullable Entity entity;
    private List<ProjectDiagnostic> diagnostics = List.of();

    /** Stores one validated request before its structural commit. */
    InternalSpawnOperation(
            InternalEntity owner, InternalPreparedEntityDefinition prepared, Map<PropertyId, ProjectValue> parameters) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.prepared = Objects.requireNonNull(prepared, "prepared");
        this.parameters = Map.copyOf(parameters);
    }

    @Override
    public SpawnStatus status() {
        return status;
    }

    @Override
    public Optional<Entity> entity() {
        return Optional.ofNullable(entity);
    }

    @Override
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    @Override
    public boolean cancel() {
        if (status != SpawnStatus.PENDING) {
            return false;
        }
        status = SpawnStatus.CANCELLED;
        return true;
    }

    /** Returns the fixed owner of the requested instance. */
    InternalEntity owner() {
        return owner;
    }

    /** Returns the prepared definition backing the request. */
    InternalPreparedEntityDefinition prepared() {
        return prepared;
    }

    /** Returns copied instance-specific ordinary parameters. */
    Map<PropertyId, ProjectValue> parameters() {
        return parameters;
    }

    /** Returns whether this request should begin composition at the next boundary. */
    boolean isPending() {
        return status == SpawnStatus.PENDING;
    }

    /** Publishes one complete activated result. */
    void complete(Entity value) {
        requirePending();
        entity = Objects.requireNonNull(value, "value");
        status = SpawnStatus.ACTIVE;
    }

    /** Records one terminal structured failure without exposing an entity. */
    void fail(List<ProjectDiagnostic> values) {
        requirePending();
        List<ProjectDiagnostic> validValues = List.copyOf(values);
        if (validValues.isEmpty()) {
            throw new IllegalArgumentException("spawn failure diagnostics must not be empty");
        }
        diagnostics = validValues;
        status = SpawnStatus.FAILED;
    }

    /** Requires that a terminal transition happens once. */
    private void requirePending() {
        if (status != SpawnStatus.PENDING) {
            throw new IllegalStateException("spawn operation is already terminal: " + status);
        }
    }
}
