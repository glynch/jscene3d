/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Transactionally constructs component values after allocation of the complete entity graph. */
final class RuntimeComponentConstructor {
    /** Prevents construction of this stateless construction implementation. */
    private RuntimeComponentConstructor() {
        throw new AssertionError("RuntimeComponentConstructor cannot be instantiated");
    }

    /** Constructs every planned component and completes the world, rolling back on any failure. */
    static World construct(AllocatedWorld allocation, RegisteredTypeCatalog catalog, FactoryBindings factories) {
        List<Object> created = new ArrayList<>();
        try {
            List<WorldComponentEntry> entries = construct(
                    allocation.world(),
                    allocation.components(),
                    allocation.connections(),
                    catalog,
                    factories,
                    ComponentCreationContext.ResourceAccess.ACQUIRE,
                    created);
            allocation.world().complete(entries);
            return allocation.world();
        } catch (RuntimeException failure) {
            rollbackWorld(allocation.world(), created, failure);
            throw failure;
        }
    }

    /** Constructs one detached prepared instance without publishing or invoking lifecycle callbacks. */
    static List<WorldComponentEntry> construct(
            AllocatedInstance allocation, RegisteredTypeCatalog catalog, FactoryBindings factories) {
        List<Object> created = new ArrayList<>();
        try {
            List<WorldComponentEntry> entries = construct(
                    allocation.world(),
                    allocation.components(),
                    allocation.connections(),
                    catalog,
                    factories,
                    ComponentCreationContext.ResourceAccess.PREPARED_ONLY,
                    created);
            allocation.entities().forEach(InternalEntity::complete);
            return entries;
        } catch (RuntimeException failure) {
            allocation.world().rollbackSpawn(allocation.entities(), created, failure);
            throw failure;
        }
    }

    /** Constructs and binds one allocated component set through the canonical instance pipeline. */
    private static List<WorldComponentEntry> construct(
            InternalWorld world,
            List<ComponentPlan> plans,
            List<RuntimeConnectionPlan> connections,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            ComponentCreationContext.ResourceAccess resourceAccess,
            List<Object> created) {
        List<WorldComponentEntry> entries = new ArrayList<>();
        List<ComponentBindingEntry> bindings = new ArrayList<>();
        List<ComponentEndpointBindingEntry> endpointBindings = new ArrayList<>();
        for (ComponentPlan plan : plans) {
            ComponentTypeDescriptor descriptor = descriptor(plan, catalog);
            EffectiveComponentProperties properties =
                    EffectiveComponentProperties.merge(descriptor, plan.definition(), plan.overrides(), plan.scope());
            Object value = create(plan, world, descriptor, properties, factories, resourceAccess);
            world.claimComponent(value, plan.location());
            created.add(value);
            requireLifecycleSupport(plan, descriptor, value);
            requireUpdateSupport(plan, descriptor, value);
            boolean bindsReferences = declaresTargetProperties(descriptor, properties);
            requireReferenceSupport(plan, bindsReferences, value);
            boolean bindsEndpoints = declaresEndpoints(descriptor);
            requireEndpointSupport(plan, bindsEndpoints, value);
            plan.owner().addComponent(plan.definition().id(), descriptor, value);
            plan.scope().bindComponent(plan.authoredEntity(), plan.definition().id(), value);
            if (bindsReferences && value instanceof ComponentReferenceBinder binder) {
                bindings.add(new ComponentBindingEntry(plan, binder, properties));
            }
            if (bindsEndpoints && value instanceof ComponentEndpointBinder binder) {
                endpointBindings.add(new ComponentEndpointBindingEntry(plan, descriptor, binder));
            }
            entries.add(new WorldComponentEntry(
                    plan.owner(),
                    plan.definition().id(),
                    descriptor.type(),
                    value,
                    descriptor.lifecycle(),
                    descriptor.updatePhases()));
        }
        bindReferences(bindings);
        bindEndpoints(endpointBindings, world.endpointRouter());
        world.endpointRouter().connect(connections);
        return entries;
    }

    /** Creates one component through its exact descriptor-backed factory. */
    private static Object create(
            ComponentPlan plan,
            InternalWorld world,
            ComponentTypeDescriptor descriptor,
            EffectiveComponentProperties properties,
            FactoryBindings factories,
            ComponentCreationContext.ResourceAccess resourceAccess) {
        ComponentDefinition definition = plan.definition();
        ComponentType type = new ComponentType(definition.type(), definition.typeVersion());
        ComponentFactory<?> factory = factories.requireComponent(type, plan.location());
        ComponentCreationContext context = new ComponentCreationContext(
                plan.owner(), world, definition, descriptor, properties, plan.location(), resourceAccess);
        try {
            return Objects.requireNonNull(factory.create(context), "component factory result");
        } catch (RuntimeDiagnosticsException | RuntimeCompositionException exception) {
            throw exception;
        } catch (MissingWorldModuleException exception) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.WORLD_MODULE_MISSING,
                    "factory for " + type + " requires absent world module "
                            + exception.moduleType().getName(),
                    plan.location());
        } catch (RuntimeException exception) {
            throw factoryFailure(type, plan, exception);
        } finally {
            context.expire();
        }
    }

    /** Returns the descriptor which catalog-aware validation already established for one plan. */
    private static ComponentTypeDescriptor descriptor(ComponentPlan plan, RegisteredTypeCatalog catalog) {
        ComponentDefinition definition = plan.definition();
        ComponentType type = new ComponentType(definition.type(), definition.typeVersion());
        return catalog.findComponent(type)
                .orElseThrow(() -> new RuntimeCompositionException(
                        RuntimeDiagnosticCode.TYPE_MISSING,
                        "component descriptor is absent after validation: " + type,
                        plan.location()));
    }

    /** Requires a callback implementation whenever safe metadata declares lifecycle participation. */
    private static void requireLifecycleSupport(ComponentPlan plan, ComponentTypeDescriptor descriptor, Object value) {
        if (!descriptor.lifecycle().isEmpty() && !(value instanceof ComponentLifecycleCallbacks)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_LIFECYCLE_UNSUPPORTED,
                    "component factory result does not implement ComponentLifecycleCallbacks",
                    plan.location());
        }
    }

    /** Requires an update implementation whenever safe metadata declares scheduling participation. */
    private static void requireUpdateSupport(ComponentPlan plan, ComponentTypeDescriptor descriptor, Object value) {
        if (!descriptor.updatePhases().isEmpty() && !(value instanceof ComponentUpdateCallbacks)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_UPDATE_UNSUPPORTED,
                    "component factory result does not implement ComponentUpdateCallbacks",
                    plan.location());
        }
    }

    /** Returns whether safe metadata declares at least one target-valued property. */
    private static boolean declaresTargetProperties(
            ComponentTypeDescriptor descriptor, EffectiveComponentProperties properties) {
        return descriptor.properties().values().stream()
                        .anyMatch(property -> isTargetKind(property.valueKind())
                                || property.elementKind()
                                        .filter(RuntimeComponentConstructor::isTargetKind)
                                        .isPresent())
                || properties.values().values().stream().anyMatch(RuntimeComponentConstructor::containsTarget);
    }

    /** Requires target-valued component types to expose the one binding callback. */
    private static void requireReferenceSupport(ComponentPlan plan, boolean bindsReferences, Object value) {
        if (bindsReferences && !(value instanceof ComponentReferenceBinder)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_REFERENCE_BINDING_UNSUPPORTED,
                    "component factory result does not implement ComponentReferenceBinder",
                    plan.location());
        }
    }

    /** Returns whether one safe property declaration contains a definition-instance target. */
    private static boolean isTargetKind(ProjectValueKind kind) {
        return kind == ProjectValueKind.ENTITY_TARGET || kind == ProjectValueKind.COMPONENT_TARGET;
    }

    /** Finds target values nested inside portable arrays or objects. */
    private static boolean containsTarget(ProjectValue value) {
        return switch (value) {
            case ProjectValue.EntityTargetValue ignored -> true;
            case ProjectValue.ComponentTargetValue ignored -> true;
            case ProjectValue.ArrayValue(List<ProjectValue> values) ->
                values.stream().anyMatch(RuntimeComponentConstructor::containsTarget);
            case ProjectValue.ObjectValue(Map<String, ProjectValue> values) ->
                values.values().stream().anyMatch(RuntimeComponentConstructor::containsTarget);
            default -> false;
        };
    }

    /** Returns whether safe metadata declares at least one signal or action. */
    private static boolean declaresEndpoints(ComponentTypeDescriptor descriptor) {
        return !descriptor.signals().isEmpty() || !descriptor.actions().isEmpty();
    }

    /** Requires endpoint-declaring component types to expose the one binding callback. */
    private static void requireEndpointSupport(ComponentPlan plan, boolean bindsEndpoints, Object value) {
        if (bindsEndpoints && !(value instanceof ComponentEndpointBinder)) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_ENDPOINT_BINDING_UNSUPPORTED,
                    "component factory result does not implement ComponentEndpointBinder",
                    plan.location());
        }
    }

    /** Binds every component only after all factory-created values have entered their instance indexes. */
    private static void bindReferences(List<ComponentBindingEntry> bindings) {
        for (ComponentBindingEntry binding : bindings) {
            ComponentReferenceBindingContext references = new ComponentReferenceBindingContext(
                    binding.properties(), binding.plan().location());
            try {
                binding.binder().bindReferences(references);
            } catch (RuntimeCompositionException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw bindingFailure(binding.plan(), failure);
            } finally {
                references.expire();
            }
        }
    }

    /** Binds every descriptor-declared endpoint after all authored references have resolved. */
    private static void bindEndpoints(List<ComponentEndpointBindingEntry> bindings, EndpointRouter router) {
        for (ComponentEndpointBindingEntry binding : bindings) {
            ComponentEndpointBindingContext endpoints = new ComponentEndpointBindingContext(binding, router);
            try {
                binding.binder().bindEndpoints(endpoints);
                endpoints.requireComplete();
            } catch (RuntimeCompositionException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw endpointBindingFailure(binding.plan(), failure);
            } finally {
                endpoints.expire();
            }
        }
    }

    /** Wraps an arbitrary endpoint implementation failure in one stable binding diagnostic. */
    private static RuntimeCompositionException endpointBindingFailure(ComponentPlan plan, RuntimeException failure) {
        String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_ENDPOINT_BINDING_FAILED,
                "endpoint binding failed for component " + plan.definition().id() + ": " + detail,
                plan.location());
    }

    /** Wraps an arbitrary implementation failure in one stable binding diagnostic. */
    private static RuntimeCompositionException bindingFailure(ComponentPlan plan, RuntimeException failure) {
        String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
        return new RuntimeCompositionException(
                RuntimeDiagnosticCode.COMPONENT_REFERENCE_BINDING_FAILED,
                "reference binding failed for component " + plan.definition().id() + ": " + detail,
                plan.location());
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
    private static void rollbackWorld(InternalWorld world, List<Object> created, RuntimeException failure) {
        world.releaseComponentClaims(created);
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
        world.fail(failure);
    }
}
