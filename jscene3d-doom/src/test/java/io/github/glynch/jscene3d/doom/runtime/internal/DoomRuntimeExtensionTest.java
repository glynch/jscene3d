/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.doom.runtime.DoomDoor;
import io.github.glynch.jscene3d.doom.runtime.DoomDoorDescriptors;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Verifies that the Doom runtime provider constructs only descriptor-selected Doom components. */
final class DoomRuntimeExtensionTest {
    /** Registers and constructs the door implementation from validated effective properties. */
    @Test
    void constructsDoor() {
        DoomRuntimeExtension extension = new DoomRuntimeExtension();
        Map<ComponentType, ComponentFactory<?>> factories = new LinkedHashMap<>();
        extension.register(factories::put);
        ComponentProperties properties = new ComponentProperties(Map.of(
                DoomDoorDescriptors.CLOSED_HEIGHT_PROPERTY,
                number(-2.0F),
                DoomDoorDescriptors.OPEN_HEIGHT_PROPERTY,
                number(0.875F),
                DoomDoorDescriptors.SPEED_PROPERTY,
                number(8.75F),
                DoomDoorDescriptors.HOLD_OPEN_SECONDS_PROPERTY,
                number(150.0F / 35.0F),
                DoomDoorDescriptors.PROFILE_PROPERTY,
                new ProjectValue.TextValue("blaze")));

        assertThat(extension.id()).isEqualTo("io.github.glynch.jscene3d.doom");
        assertThat(factories).containsOnlyKeys(DoomDoorDescriptors.DOOR_TYPE);
        assertThat(Objects.requireNonNull(factories.get(DoomDoorDescriptors.DOOR_TYPE))
                        .create(context(properties)))
                .isInstanceOf(DoomDoor.class)
                .satisfies(value -> {
                    DoomDoor door = (DoomDoor) value;
                    assertThat(door.currentHeight()).isEqualTo(-2.0F);
                    assertThat(door.profile()).isEqualTo(DoomDoor.Profile.BLAZE);
                });
    }

    private static ComponentFactoryContext context(ComponentProperties properties) {
        return (ComponentFactoryContext) Proxy.newProxyInstance(
                DoomRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {ComponentFactoryContext.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("properties")) {
                        return properties;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }
}
