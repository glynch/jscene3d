/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable filtering options shared by spatial queries. */
public final class QueryFilter {
    /** Default query filter: all layers, excluding triggers. */
    public static final QueryFilter DEFAULT = new QueryFilter(-1, TriggerMode.EXCLUDE, null);

    private final int layerMask;
    private final TriggerMode triggerMode;
    private final @Nullable Collider excludedCollider;

    private QueryFilter(int layerMask, TriggerMode triggerMode, @Nullable Collider excludedCollider) {
        this.layerMask = layerMask;
        this.triggerMode = Objects.requireNonNull(triggerMode, "triggerMode");
        this.excludedCollider = excludedCollider;
    }

    /**
     * Creates a query filter for the supplied category bits.
     *
     * @param layerMask collider category bits accepted by the query
     * @return immutable query filter
     */
    public static QueryFilter layers(int layerMask) {
        return new QueryFilter(layerMask, TriggerMode.EXCLUDE, null);
    }

    /**
     * Returns the category bits accepted by this query.
     *
     * @return accepted category bits
     */
    public int layerMask() {
        return layerMask;
    }

    /**
     * Returns how this query treats trigger colliders.
     *
     * @return trigger policy
     */
    public TriggerMode triggerMode() {
        return triggerMode;
    }

    /**
     * Returns the collider excluded from this query, if any.
     *
     * @return optional excluded collider
     */
    public Optional<Collider> excludedCollider() {
        return Optional.ofNullable(excludedCollider);
    }

    /**
     * Returns a copy using the supplied category mask.
     *
     * @param newLayerMask replacement category mask
     * @return updated immutable filter
     */
    public QueryFilter withLayerMask(int newLayerMask) {
        return new QueryFilter(newLayerMask, triggerMode, excludedCollider);
    }

    /**
     * Returns a copy using the supplied trigger policy.
     *
     * @param newTriggerMode replacement trigger policy
     * @return updated immutable filter
     */
    public QueryFilter withTriggerMode(TriggerMode newTriggerMode) {
        return new QueryFilter(layerMask, newTriggerMode, excludedCollider);
    }

    /**
     * Returns a copy that excludes the supplied collider.
     *
     * @param collider collider to exclude by identity
     * @return updated immutable filter
     */
    public QueryFilter excluding(Collider collider) {
        return new QueryFilter(layerMask, triggerMode, Objects.requireNonNull(collider, "collider"));
    }

    /**
     * Returns a copy that does not exclude a particular collider.
     *
     * @return filter without a collider exclusion
     */
    public QueryFilter withoutExclusion() {
        return excludedCollider == null ? this : new QueryFilter(layerMask, triggerMode, null);
    }
}
