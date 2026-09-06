/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import java.util.List;

/** Invokes descriptor-backed component preparation as one resource-retention transaction. */
final class RuntimeComponentPreparer {
    /** Prevents construction of this stateless preparation implementation. */
    private RuntimeComponentPreparer() {
        throw new AssertionError("RuntimeComponentPreparer cannot be instantiated");
    }

    /** Prepares every component plan and rolls back resources after any failure. */
    static void prepare(
            List<ComponentPreparationPlan> plans,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            WorldResources resources) {
        WorldResources.Preparation preparation = resources.beginPreparation();
        try {
            for (ComponentPreparationPlan plan : plans) {
                prepare(plan, catalog, factories, preparation);
            }
            preparation.commit();
        } catch (RuntimeException failure) {
            preparation.rollback(failure);
            throw failure;
        }
    }

    /** Invokes one exact component factory's bounded preparation hook. */
    private static void prepare(
            ComponentPreparationPlan plan,
            RegisteredTypeCatalog catalog,
            FactoryBindings factories,
            WorldResources.Preparation resources) {
        ComponentDefinition definition = plan.definition();
        ComponentType type = new ComponentType(definition.type(), definition.typeVersion());
        ComponentTypeDescriptor descriptor = catalog.findComponent(type)
                .orElseThrow(() -> new RuntimeCompositionException(
                        RuntimeDiagnosticCode.TYPE_MISSING,
                        "component descriptor is absent after validation: " + type,
                        plan.location()));
        EffectiveComponentProperties properties =
                EffectiveComponentProperties.merge(descriptor, definition, plan.overrides(), plan.scope());
        ComponentFactory<?> factory = factories.requireComponent(type, plan.location());
        ComponentResourcePreparationContext context =
                new ComponentResourcePreparationContext(definition, descriptor, properties, resources, plan.location());
        try {
            factory.prepare(context);
        } catch (RuntimeDiagnosticsException | RuntimeCompositionException exception) {
            throw exception;
        } catch (RuntimeException failure) {
            String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.ENTITY_PREPARATION_FAILED,
                    "preparation for " + type + " failed for component " + definition.id() + ": " + detail,
                    plan.location());
        } finally {
            context.expire();
        }
    }
}
