/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.Objects;

/** Executable factories corresponding exactly to {@link Game3dDescriptors}. */
public final class Game3dRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless built-in runtime contribution. */
    public Game3dRuntimeExtension() {
        // Explicit construction supports ordinary runtime-extension registration.
    }

    @Override
    public String id() {
        return Game3dDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .register(Game3dDescriptors.firstPersonControllerType(), context -> {
                    ComponentProperties properties = context.properties();
                    return new FirstPersonCharacterController3d(
                            context.world().requireModule(InputWorldModule.class),
                            new FirstPersonCharacterController3d.Actions(
                                    action(properties, Game3dDescriptors.moveActionProperty()),
                                    action(properties, Game3dDescriptors.lookActionProperty()),
                                    action(properties, Game3dDescriptors.turnLeftActionProperty()),
                                    action(properties, Game3dDescriptors.turnRightActionProperty())),
                            new FirstPersonCharacterController3d.Tuning(
                                    properties.finiteFloat(Game3dDescriptors.moveSpeedProperty()),
                                    properties.finiteFloat(Game3dDescriptors.turnSpeedDegreesProperty()),
                                    properties.finiteFloat(Game3dDescriptors.pointerSensitivityProperty()),
                                    properties.finiteFloat(Game3dDescriptors.maximumPitchDegreesProperty())));
                });
    }

    private static InputAction action(ComponentProperties properties, PropertyId id) {
        return new InputAction(properties.text(id));
    }
}
