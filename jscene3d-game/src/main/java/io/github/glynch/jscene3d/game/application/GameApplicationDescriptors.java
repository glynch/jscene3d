/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Safe metadata for generic application-lifecycle components. */
public final class GameApplicationDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.application";
    private static final ComponentType COMMAND_BINDING = ComponentType.of(EXTENSION_ID + "/command-binding", 1);
    private static final PropertyId ACTION = new PropertyId("action");
    private static final PropertyId COMMAND = new PropertyId("command");
    private static final ExtensionDescriptor DESCRIPTOR = new ExtensionDescriptor(
            EXTENSION_ID,
            "1.0.0",
            ">=0.1.0 <0.2.0",
            DescriptorPresentation.named("JScene3D application lifecycle"),
            List.of(),
            List.of(ComponentTypeDescriptor.builder(
                            COMMAND_BINDING, DescriptorPresentation.named("Application command binding"))
                    .properties(List.of(required(ACTION, "Action"), required(COMMAND, "Command")))
                    .updatePhases(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))
                    .build()));

    private GameApplicationDescriptors() {
        throw new AssertionError("GameApplicationDescriptors cannot be instantiated");
    }

    /**
     * Returns the built-in application extension identity.
     *
     * @return stable built-in extension identity
     */
    public static String extensionId() {
        return EXTENSION_ID;
    }

    /**
     * Returns the descriptor-backed semantic action-to-command component type.
     *
     * @return application command-binding type
     */
    public static ComponentType commandBindingType() {
        return COMMAND_BINDING;
    }

    /**
     * Returns safe metadata for generic application behavior.
     *
     * @return built-in application descriptor
     */
    public static ExtensionDescriptor extensionDescriptor() {
        return DESCRIPTOR;
    }

    static PropertyId actionProperty() {
        return ACTION;
    }

    static PropertyId commandProperty() {
        return COMMAND;
    }

    private static PropertyDescriptor required(PropertyId id, String name) {
        return PropertyDescriptor.required(
                id.value(), ProjectValueKind.TEXT, DescriptorPresentation.named(name), Map.of(), Set.of());
    }
}
