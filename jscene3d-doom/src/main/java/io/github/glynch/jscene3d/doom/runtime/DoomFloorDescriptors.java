/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime;

import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;

/** Stable portable identities shared by moving-floor publication and runtime behavior. */
public final class DoomFloorDescriptors {
    private static final String NAMESPACE = "io.github.glynch.jscene3d.doom/";

    /** Descriptor-selected component type for a vertically moving floor. */
    public static final ComponentType FLOOR_TYPE = new ComponentType(new ComponentTypeId(NAMESPACE + "floor"), 1);

    /** Capability exposing the live imported floor mechanism. */
    public static final CapabilityId FLOOR_CAPABILITY = new CapabilityId(NAMESPACE + "floor");

    /** Explicit transform target moved by the floor behavior. */
    public static final PropertyId TRANSFORM_PROPERTY = new PropertyId("transform");

    /** Original local Y position in world units. */
    public static final PropertyId RAISED_HEIGHT_PROPERTY = new PropertyId("raised-height");

    /** Terminal local Y position in world units. */
    public static final PropertyId LOWERED_HEIGHT_PROPERTY = new PropertyId("lowered-height");

    /** Vertical movement speed in world units per second. */
    public static final PropertyId SPEED_PROPERTY = new PropertyId("speed");

    /** Imported source behavior profile. */
    public static final PropertyId PROFILE_PROPERTY = new PropertyId("profile");

    /** Player-only walk-over notification emitted by the generated trigger sensor. */
    public static final EndpointId TRIGGER_ENTERED_ACTION = new EndpointId("trigger-entered");

    private DoomFloorDescriptors() {
        throw new AssertionError("DoomFloorDescriptors cannot be instantiated");
    }
}
