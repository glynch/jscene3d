/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.List;
import java.util.Optional;

/** Read-only view of one live entity owned by a {@link World}. */
public interface Entity {
    /**
     * Returns the identity unique within the owning world.
     *
     * @return live entity identity
     */
    RuntimeEntityId id();

    /**
     * Returns the asset containing the local entry which produced this entity.
     *
     * <p>For a placed definition root this is the containing asset and {@link #authoredId()} is the placement ID.
     * Descendants authored inside the placed definition report that definition's asset identity.
     *
     * @return containing authored asset identity
     */
    AssetId authoredAsset();

    /**
     * Returns the stable local entry identity which produced this entity.
     *
     * @return authored entity or placement identity
     */
    EntityId authoredId();

    /**
     * Returns the optional display name selected for this instance.
     *
     * @return display name
     */
    Optional<String> name();

    /**
     * Returns the entity's own initial enabled flag without considering its owner.
     *
     * @return local enabled state
     */
    boolean isLocallyEnabled();

    /**
     * Returns the initial enabled state inherited through the ownership hierarchy.
     *
     * <p>An inactive composed world does not invoke behavior merely because this value is {@code true}; lifecycle
     * activation is a separate operation.
     *
     * @return effective initial enabled state
     */
    boolean isEnabled();

    /**
     * Returns the ownership parent, or empty for a world root.
     *
     * @return optional ownership parent
     */
    Optional<Entity> parent();

    /**
     * Returns owned children in deterministic authored order.
     *
     * @return immutable child list
     */
    List<Entity> children();

    /**
     * Returns constructed component identities in authored order.
     *
     * @return immutable component identity list
     */
    List<ComponentId> componentIds();

    /**
     * Finds one component by explicit stable identity and checks its implementation type.
     *
     * <p>This is not lookup by implementation class. The component identity selects the value; {@code valueType}
     * verifies the caller's expected Java representation.
     *
     * @param <T> expected runtime component type
     * @param component authored component identity
     * @param valueType expected Java type
     * @return component value when the identity exists
     * @throws ClassCastException if the identified value has another Java type
     */
    <T> Optional<T> component(ComponentId component, Class<T> valueType);

    /**
     * Returns the world which exclusively owns this entity.
     *
     * @return owning world
     */
    World world();
}
