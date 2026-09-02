/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

import io.github.glynch.jscene3d.physics.CollisionObject;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable filtering options shared by spatial queries. */
public final class QueryFilter {
    /** Default query filter: all layers, excluding collision sensors. */
    public static final QueryFilter DEFAULT = new QueryFilter(-1, SensorMode.EXCLUDE, null);

    private final int layerMask;
    private final SensorMode sensorMode;
    private final @Nullable CollisionObject excludedObject;

    private QueryFilter(int layerMask, SensorMode sensorMode, @Nullable CollisionObject excludedObject) {
        this.layerMask = layerMask;
        this.sensorMode = Objects.requireNonNull(sensorMode, "sensorMode");
        this.excludedObject = excludedObject;
    }

    /**
     * Creates a query filter for the supplied category bits.
     *
     * @param layerMask collider category bits accepted by the query
     * @return immutable query filter
     */
    public static QueryFilter layers(int layerMask) {
        return new QueryFilter(layerMask, SensorMode.EXCLUDE, null);
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
     * Returns how this query treats collision sensors.
     *
     * @return collision-sensor policy
     */
    public SensorMode sensorMode() {
        return sensorMode;
    }

    /**
     * Returns the collision object excluded from this query, if any.
     *
     * @return optional excluded collision object
     */
    public Optional<CollisionObject> excludedObject() {
        return Optional.ofNullable(excludedObject);
    }

    /**
     * Returns a copy using the supplied category mask.
     *
     * @param newLayerMask replacement category mask
     * @return updated immutable filter
     */
    public QueryFilter withLayerMask(int newLayerMask) {
        return new QueryFilter(newLayerMask, sensorMode, excludedObject);
    }

    /**
     * Returns a copy using the supplied collision-sensor policy.
     *
     * @param newSensorMode replacement collision-sensor policy
     * @return updated immutable filter
     */
    public QueryFilter withSensorMode(SensorMode newSensorMode) {
        return new QueryFilter(layerMask, newSensorMode, excludedObject);
    }

    /**
     * Returns a copy that excludes every collider owned by the supplied object.
     *
     * @param collisionObject collision object to exclude by identity
     * @return updated immutable filter
     */
    public QueryFilter excluding(CollisionObject collisionObject) {
        return new QueryFilter(layerMask, sensorMode, Objects.requireNonNull(collisionObject, "collisionObject"));
    }

    /**
     * Returns a copy that does not exclude a particular collision object.
     *
     * @return filter without a collider exclusion
     */
    public QueryFilter withoutExclusion() {
        return excludedObject == null ? this : new QueryFilter(layerMask, sensorMode, null);
    }
}
