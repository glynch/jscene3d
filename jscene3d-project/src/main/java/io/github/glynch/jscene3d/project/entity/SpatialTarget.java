/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Stable target of primary spatial state or a named attachment supplied by a component or placement. */
public final class SpatialTarget {
    private final EntityId entity;
    private final @Nullable ComponentId component;
    private final @Nullable AttachmentPointId attachment;

    /** Stores one target selected through a named public factory. */
    private SpatialTarget(EntityId entity, @Nullable ComponentId component, @Nullable AttachmentPointId attachment) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.component = component;
        this.attachment = attachment;
    }

    /**
     * Targets an entity's primary spatial state or a placement's root.
     *
     * @param entity entity or placement identity
     * @return primary spatial target
     */
    public static SpatialTarget entity(EntityId entity) {
        return new SpatialTarget(entity, null, null);
    }

    /**
     * Targets spatial state supplied by a locally authored component.
     *
     * @param entity local entity identity
     * @param component component identity
     * @return component spatial target
     */
    public static SpatialTarget component(EntityId entity, ComponentId component) {
        return new SpatialTarget(entity, Objects.requireNonNull(component, "component"), null);
    }

    /**
     * Targets a named spatial attachment supplied by a locally authored component.
     *
     * @param entity local entity identity
     * @param component component identity
     * @param attachment attachment identity
     * @return component attachment target
     */
    public static SpatialTarget componentAttachment(
            EntityId entity, ComponentId component, AttachmentPointId attachment) {
        return new SpatialTarget(
                entity,
                Objects.requireNonNull(component, "component"),
                Objects.requireNonNull(attachment, "attachment"));
    }

    /**
     * Targets an attachment exported by a reusable-definition placement.
     *
     * @param placement placement identity
     * @param attachment exported attachment identity
     * @return placement attachment target
     */
    public static SpatialTarget placementAttachment(EntityId placement, AttachmentPointId attachment) {
        return new SpatialTarget(placement, null, Objects.requireNonNull(attachment, "attachment"));
    }

    /**
     * Returns the target entity or placement identity.
     *
     * @return target identity
     */
    public EntityId entity() {
        return entity;
    }

    /**
     * Returns the local component identity, absent for entity or placement targets.
     *
     * @return optional local component identity
     */
    public Optional<ComponentId> component() {
        return Optional.ofNullable(component);
    }

    /**
     * Returns the named component or placed-definition attachment.
     *
     * @return optional attachment identity
     */
    public Optional<AttachmentPointId> attachment() {
        return Optional.ofNullable(attachment);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SpatialTarget target
                && entity.equals(target.entity)
                && Objects.equals(component, target.component)
                && Objects.equals(attachment, target.attachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, component, attachment);
    }

    @Override
    public String toString() {
        return "SpatialTarget[entity=" + entity + ", component=" + component + ", attachment=" + attachment + ']';
    }
}
