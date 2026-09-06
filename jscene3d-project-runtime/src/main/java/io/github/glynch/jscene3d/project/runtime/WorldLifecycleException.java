/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import java.util.Objects;

/** Failure of one declared component callback during world activation, rollback, or closure. */
public final class WorldLifecycleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Lifecycle event being delivered when the callback failed. */
    private final ComponentLifecycle event;

    /** Live identity of the entity which owns the failed component. */
    private final RuntimeEntityId entity;

    /** Authored identity of the failed component within its owning entity. */
    private final ComponentId component;

    /**
     * Identifies one failed callback while retaining its implementation cause.
     *
     * @param event lifecycle event being delivered
     * @param entity owning live entity identity
     * @param component authored component identity within the entity
     * @param cause callback failure
     */
    public WorldLifecycleException(
            ComponentLifecycle event, RuntimeEntityId entity, ComponentId component, RuntimeException cause) {
        super(message(event, entity, component), Objects.requireNonNull(cause, "cause"));
        this.event = Objects.requireNonNull(event, "event");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.component = Objects.requireNonNull(component, "component");
    }

    /**
     * Returns the lifecycle event which failed.
     *
     * @return failed event
     */
    public ComponentLifecycle event() {
        return event;
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
    private static String message(ComponentLifecycle event, RuntimeEntityId entity, ComponentId component) {
        return "component lifecycle " + event + " failed for entity " + entity + " component " + component;
    }
}
