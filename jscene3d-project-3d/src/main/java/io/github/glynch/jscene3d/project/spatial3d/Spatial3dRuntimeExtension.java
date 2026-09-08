/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.Locale;
import java.util.Objects;

/** Executable Transform3d factory corresponding exactly to {@link Spatial3dDescriptors}. */
public final class Spatial3dRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless built-in runtime contribution. */
    public Spatial3dRuntimeExtension() {
        // Explicit construction supports ordinary runtime-extension registration.
    }

    @Override
    public String id() {
        return Spatial3dDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        ComponentFactoryRegistry validRegistry = Objects.requireNonNull(registry, "registry");
        validRegistry.register(Spatial3dDescriptors.transformType(), context -> {
            AuthoredTransform3d authored =
                    AuthoredTransform3d.from(context.properties().values());
            Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
            return spatial.createTransform(
                    context.owner(), authored.position(), authored.orientation(), authored.scale());
        });
        validRegistry.register(Spatial3dDescriptors.perspectiveCameraType(), context -> {
            AuthoredPresentation3d.Camera authored =
                    AuthoredPresentation3d.camera(context.properties().values());
            Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
            return spatial.createPerspectiveCamera(
                    context.owner(),
                    authored.fieldOfViewDegrees(),
                    authored.near(),
                    authored.far(),
                    authored.primary());
        });
        validRegistry.register(Spatial3dDescriptors.directionalLightType(), context -> {
            AuthoredPresentation3d.Light authored =
                    AuthoredPresentation3d.light(context.properties().values());
            Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
            return spatial.createDirectionalLight(
                    context.owner(), authored.color(), authored.intensity(), authored.target());
        });
        validRegistry.register(Spatial3dDescriptors.meshRendererType(), meshRendererFactory());
        validRegistry.register(Spatial3dDescriptors.billboardRendererType(), billboardRendererFactory());
    }

    /** Creates the resource-aware mesh-renderer preparation and construction adapter. */
    private static ComponentFactory<MeshRenderer3d> meshRendererFactory() {
        return new ComponentFactory<>() {
            @Override
            public void prepare(ComponentPreparationContext context) {
                AuthoredPresentation3d.MeshRenderer authored =
                        AuthoredPresentation3d.meshRenderer(context.properties().values());
                context.resolveResource(authored.mesh(), Mesh3dResource.class);
                context.resolveResource(authored.material(), Material3dResource.class);
            }

            @Override
            public MeshRenderer3d create(ComponentFactoryContext context) {
                AuthoredPresentation3d.MeshRenderer authored =
                        AuthoredPresentation3d.meshRenderer(context.properties().values());
                Mesh3dResource mesh = context.resolveResource(authored.mesh(), Mesh3dResource.class);
                Material3dResource material = context.resolveResource(authored.material(), Material3dResource.class);
                Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
                return spatial.createMeshRenderer(context.owner(), mesh, material, authored.visible());
            }
        };
    }

    /** Creates the resource-aware billboard-renderer preparation and construction adapter. */
    private static ComponentFactory<BillboardRenderer3d> billboardRendererFactory() {
        return new ComponentFactory<>() {
            @Override
            public void prepare(ComponentPreparationContext context) {
                AuthoredPresentation3d.BillboardRenderer authored = AuthoredPresentation3d.billboardRenderer(
                        context.properties().values());
                context.resolveResource(authored.material(), Material3dResource.class);
            }

            @Override
            public BillboardRenderer3d create(ComponentFactoryContext context) {
                AuthoredPresentation3d.BillboardRenderer authored = AuthoredPresentation3d.billboardRenderer(
                        context.properties().values());
                Material3dResource material = context.resolveResource(authored.material(), Material3dResource.class);
                BillboardAlignment alignment = BillboardAlignment.valueOf(
                        authored.alignment().toUpperCase(Locale.ROOT).replace('-', '_'));
                Spatial3dWorldModule spatial = context.world().requireModule(Spatial3dWorldModule.class);
                return spatial.createBillboardRenderer(
                        context.owner(), material, authored.size(), authored.anchor(), alignment, authored.visible());
            }
        };
    }
}
