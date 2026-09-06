/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import java.util.Objects;

/** Failure of one descriptor-authorized component callback during a world update. */
public final class WorldUpdateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Update phase being delivered when the callback failed. */
    private final ComponentUpdatePhase phase;

    /** Live identity of the entity which owns the failed component. */
    private final RuntimeEntityId entity;

    /** Authored identity of the failed component within its owning entity. */
    private final ComponentId component;

    /**
     * Identifies one failed update callback while retaining its implementation cause.
     *
     * @param phase update phase being delivered
     * @param entity owning live entity identity
     * @param component authored component identity within the entity
     * @param cause callback failure
     */
    public WorldUpdateException(
            ComponentUpdatePhase phase, RuntimeEntityId entity, ComponentId component, RuntimeException cause) {
        super(message(phase, entity, component), Objects.requireNonNull(cause, "cause"));
        this.phase = Objects.requireNonNull(phase, "phase");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.component = Objects.requireNonNull(component, "component");
    }

    /**
     * Returns the update phase which failed.
     *
     * @return failed update phase
     */
    public ComponentUpdatePhase phase() {
        return phase;
    }

    /**
     * Returns the owning live entity identity.
     *
     * @return owning entity identity
     */
    public RuntimeEntityId entity() {
        return entity;
    }

    /**
     * Returns the authored component identity within the entity.
     *
     * @return component identity
     */
    public ComponentId component() {
        return component;
    }

    /** Creates the stable exception message before validated fields have been assigned. */
    private static String message(ComponentUpdatePhase phase, RuntimeEntityId entity, ComponentId component) {
        return "component update " + phase + " failed for entity " + entity + " component " + component;
    }
}
