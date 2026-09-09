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

/** Stable portable identities shared by Doom map publication and runtime behavior. */
public final class DoomDoorDescriptors {
    private static final String NAMESPACE = "io.github.glynch.jscene3d.doom/";

    /** Descriptor-selected component type for a vertically moving door. */
    public static final ComponentType DOOR_TYPE = new ComponentType(new ComponentTypeId(NAMESPACE + "door"), 1);

    /** Capability used by project-defined interaction components. */
    public static final CapabilityId DOOR_CAPABILITY = new CapabilityId(NAMESPACE + "door");

    /** Explicit transform target moved by the door behavior. */
    public static final PropertyId TRANSFORM_PROPERTY = new PropertyId("transform");

    /** Closed local Y position in world units. */
    public static final PropertyId CLOSED_HEIGHT_PROPERTY = new PropertyId("closed-height");

    /** Open local Y position in world units. */
    public static final PropertyId OPEN_HEIGHT_PROPERTY = new PropertyId("open-height");

    /** Vertical movement speed in world units per second. */
    public static final PropertyId SPEED_PROPERTY = new PropertyId("speed");

    /** Open hold duration in seconds, or zero for an open-and-stay door. */
    public static final PropertyId HOLD_OPEN_SECONDS_PROPERTY = new PropertyId("hold-open-seconds");

    /** Imported semantic profile used by presentation components. */
    public static final PropertyId PROFILE_PROPERTY = new PropertyId("profile");

    /** Action notified when a descriptor-connected obstruction sensor begins overlapping a character. */
    public static final EndpointId OBSTRUCTION_ENTERED_ACTION = new EndpointId("obstruction-entered");

    /** Action notified when a descriptor-connected obstruction sensor stops overlapping a character. */
    public static final EndpointId OBSTRUCTION_EXITED_ACTION = new EndpointId("obstruction-exited");

    private DoomDoorDescriptors() {}
}
