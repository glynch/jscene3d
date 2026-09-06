/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Transactionally constructs component values after allocation of the complete entity graph. */
final class RuntimeComponentConstructor {
    /** Prevents construction of this stateless construction implementation. */
    private RuntimeComponentConstructor() {
        throw new AssertionError("RuntimeComponentConstructor cannot be instantiated");
    }

    /** Constructs every planned component and completes the world, rolling back on any failure. */
    static World construct(
            AllocatedWorld allocation,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            RuntimeResourceLookup resources) {
        List<Object> created = new ArrayList<>();
        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (ComponentPlan plan : allocation.components()) {
                Object value = create(plan, allocation.world(), catalog, factories, resources);
                if (!identities.add(value)) {
                    throw new RuntimeCompositionException(
                            RuntimeDiagnosticCode.FACTORY_CREATE_FAILED,
                            "a component factory returned the same object for more than one component",
                            plan.location());
                }
                created.add(value);
                plan.owner().addComponent(plan.definition().id(), value);
            }
            allocation.world().complete(created);
            return allocation.world();
        } catch (RuntimeException failure) {
            rollback(allocation.world(), created, failure);
            throw failure;
        }
    }

    /** Creates one component through its exact descriptor-backed factory. */
    private static Object create(
            ComponentPlan plan,
            InternalWorld world,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            RuntimeResourceLookup resources) {
        ComponentDefinition definition = plan.definition();
        ComponentType type = new ComponentType(definition.type(), definition.typeVersion());
        ComponentTypeDescriptor descriptor = catalog.findComponent(type)
                .orElseThrow(() -> new RuntimeCompositionException(
                        RuntimeDiagnosticCode.TYPE_MISSING,
                        "component descriptor is absent after validation: " + type,
                        plan.location()));
        ComponentFactory<?> factory = factories.requireComponent(type, plan.location());
        Map<PropertyId, ProjectValue> properties =
                EffectiveComponentProperties.merge(descriptor, definition, plan.overrides());
        ComponentCreationContext context =
                new ComponentCreationContext(plan.owner(), world, definition, descriptor, properties, resources);
        try {
            return Objects.requireNonNull(factory.create(context), "component factory result");
        } catch (RuntimeDiagnosticsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw factoryFailure(type, plan, exception);
        }
    }

    /** Wraps arbitrary factory failure in one stable structured composition failure. */
    private static RuntimeCompositionException factoryFailure(
            ComponentType type, ComponentPlan plan, RuntimeException failure) {
        String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.FACTORY_CREATE_FAILED,
                "factory for " + type + " failed for component "
                        + plan.definition().id() + ": " + detail,
                plan.location());
    }

    /** Closes created component values in reverse order and marks the partial world unusable. */
    private static void rollback(InternalWorld world, List<Object> created, RuntimeException failure) {
        world.fail();
        for (int index = created.size() - 1; index >= 0; index--) {
            Object value = created.get(index);
            if (value instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
        }
    }
}
