/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies exact, extension-owned component factory registration. */
final class ComponentFactoryRegistrationTest {
    private static final String EXTENSION_ID = "example.game";
    private static final ComponentType MOVER = ComponentType.of("example.game/mover", 1);
    private static final ComponentType UNKNOWN = ComponentType.of("example.game/unknown", 1);
    private static final ComponentType FOREIGN = ComponentType.of("another.game/mover", 1);
    private static final ComponentFactory<Object> FACTORY = context -> new Object();

    /** Registers and retrieves one factory by exact component type and version. */
    @Test
    void registersDescriptorBackedFactory() {
        FactoryBindings bindings = new FactoryBindings();
        RuntimeRegistry registry = registry(bindings);

        registry.registerComponent(MOVER, FACTORY);

        assertThat(bindings.requireComponent(MOVER, "/components/0")).isSameAs(FACTORY);
    }

    /** Rejects factories for missing descriptors and types owned by another extension. */
    @Test
    void rejectsUnownedOrUndeclaredFactory() {
        RuntimeRegistry registry = registry(new FactoryBindings());

        assertThatThrownBy(() -> registry.registerComponent(UNKNOWN, FACTORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no descriptor");
        assertThatThrownBy(() -> registry.registerComponent(FOREIGN, FACTORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    /** Rejects duplicate contribution and mutation after extension registration closes. */
    @Test
    void rejectsDuplicateOrLateFactory() {
        FactoryBindings bindings = new FactoryBindings();
        RuntimeRegistry registry = registry(bindings);
        registry.registerComponent(MOVER, FACTORY);

        assertThatThrownBy(() -> registry.registerComponent(MOVER, FACTORY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
        registry.closeRegistration();
        assertThatThrownBy(() -> registry.registerComponent(MOVER, FACTORY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    /** Reports a structured composition failure when a validated type has no factory. */
    @Test
    void reportsMissingFactory() {
        FactoryBindings bindings = new FactoryBindings();

        RuntimeCompositionException exception = catchThrowableOfType(
                RuntimeCompositionException.class, () -> bindings.requireComponent(MOVER, "/root/components/0"));

        assertThat(exception.code()).isEqualTo(RuntimeDiagnosticCode.COMPONENT_FACTORY_MISSING);
        assertThat(exception.location()).isEqualTo("/root/components/0");
    }

    /** Creates an isolated registration scope backed by one safe component descriptor. */
    private static RuntimeRegistry registry(FactoryBindings bindings) {
        ComponentTypeDescriptor descriptor = ComponentTypeDescriptor.builder(
                        MOVER, DescriptorPresentation.named("Mover"))
                .build();
        ExtensionDescriptor extension = new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Example Game"),
                List.of(),
                List.of(descriptor));
        return new RuntimeRegistry(EXTENSION_ID, RegisteredTypeCatalog.of(List.of(extension)), bindings);
    }
}
