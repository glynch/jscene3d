/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import io.github.glynch.jscene3d.doom.runtime.DoomDoor;
import io.github.glynch.jscene3d.doom.runtime.DoomDoorDescriptors;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.Locale;
import java.util.Objects;

/** Runtime implementation for descriptor-selected Doom components. */
public final class DoomRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless service provider. */
    public DoomRuntimeExtension() {
        // Public construction is required by ServiceLoader on both the class path and module path.
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.doom";
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        Objects.requireNonNull(registry, "registry").register(DoomDoorDescriptors.DOOR_TYPE, context -> {
            var properties = context.properties();
            return new DoomDoorComponent(
                    DoomDoor.Profile.valueOf(properties
                            .text(DoomDoorDescriptors.PROFILE_PROPERTY)
                            .toUpperCase(Locale.ROOT)),
                    properties.finiteFloat(DoomDoorDescriptors.CLOSED_HEIGHT_PROPERTY),
                    properties.finiteFloat(DoomDoorDescriptors.OPEN_HEIGHT_PROPERTY),
                    properties.finiteFloat(DoomDoorDescriptors.SPEED_PROPERTY),
                    properties.finiteFloat(DoomDoorDescriptors.HOLD_OPEN_SECONDS_PROPERTY));
        });
    }
}
