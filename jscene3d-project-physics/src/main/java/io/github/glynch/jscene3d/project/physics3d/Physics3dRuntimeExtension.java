/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import java.util.Objects;

/** Executable collision factories corresponding exactly to {@link Physics3dDescriptors}. */
public final class Physics3dRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless built-in runtime contribution. */
    public Physics3dRuntimeExtension() {
        // Explicit construction supports ordinary runtime-extension registration.
    }

    @Override
    public String id() {
        return Physics3dDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        ComponentFactoryRegistry validRegistry = Objects.requireNonNull(registry, "registry");
        validRegistry.register(Physics3dDescriptors.collisionShapeType(), collisionShapeFactory());
        validRegistry.register(
                Physics3dDescriptors.staticBodyType(),
                context -> new InternalStaticBody3d(
                        context.owner(),
                        context.definition().id(),
                        context.world().requireModule(Spatial3dWorldModule.class),
                        context.world().requireModule(Physics3dWorldModule.class)));
        validRegistry.register(
                Physics3dDescriptors.collisionSensorType(),
                context -> new InternalCollisionSensor3d(
                        context.owner(),
                        context.definition().id(),
                        context.world().requireModule(Spatial3dWorldModule.class),
                        context.world().requireModule(Physics3dWorldModule.class)));
    }

    /** Creates the resource-aware collision-shape preparation and construction adapter. */
    private static ComponentFactory<CollisionShape3d> collisionShapeFactory() {
        return new ComponentFactory<>() {
            @Override
            public void prepare(ComponentPreparationContext context) {
                AuthoredCollision3d.Shape authored = AuthoredCollision3d.shape(context.properties());
                context.resolveResource(authored.resource(), CollisionShape3dResource.class);
            }

            @Override
            public CollisionShape3d create(ComponentFactoryContext context) {
                AuthoredCollision3d.Shape authored = AuthoredCollision3d.shape(context.properties());
                CollisionShape3dResource resource =
                        context.resolveResource(authored.resource(), CollisionShape3dResource.class);
                return new CollisionShape3d(
                        context.owner(),
                        context.definition().id(),
                        resource,
                        authored.localPosition(),
                        authored.localOrientation(),
                        authored.filter());
            }
        };
    }
}
