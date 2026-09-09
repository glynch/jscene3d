/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.spatial3d.Material3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Mesh3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.List;
import org.joml.Quaternionf;

/** Renders a descriptor-authored camera, directional light, and immutable-resource cube. */
public final class Presentation3dCompositionExample {
    private static final AssetId WORLD_ID = AssetId.from("91d7de40-0364-4d1e-b357-8f255cf46d56");
    private static final ComponentId CUBE_TRANSFORM = ComponentId.from("ef97052c-8dbb-4cb7-b3f2-bce92385f4af");
    private static final ResourceReference MESH_REFERENCE = ResourceReference.asset("example-cube-mesh");
    private static final ResourceReference MATERIAL_REFERENCE = ResourceReference.asset("example-blue-material");

    /** Prevents instantiation of this application entry point. */
    private Presentation3dCompositionExample() {
        throw new AssertionError("Presentation3dCompositionExample cannot be instantiated");
    }

    /**
     * Opens the authored presentation world supplied as the sole argument.
     *
     * @param arguments one world asset-directory path
     */
    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one world asset-directory path");
        }
        Path assetDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        ExampleLauncher.launch(
                "JScene3D - Descriptor-backed 3D Presentation", context -> create(context, assetDirectory));
    }

    /** Composes the world and returns its host-facing graphical lifecycle. */
    private static HostedExample create(ExampleContext context, Path assetDirectory) {
        AssetCatalog assets = AssetCatalog.scan(assetDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        ExampleResources resources = new ExampleResources();
        WorldCompositionResult result = WorldComposer.compose(
                assets,
                AssetRef.<WorldDefinition>to(WORLD_ID),
                types,
                List.of(new Spatial3dRuntimeExtension()),
                List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                resources);
        World world = result.world().orElseThrow(() -> compositionFailure(result, spatial, resources));
        world.activate();
        Transform3d cube = world.roots()
                .get(2)
                .component(CUBE_TRANSFORM, Transform3d.class)
                .orElseThrow();
        return new PresentationExample(context, world, spatial, resources, cube);
    }

    /** Releases unpublished host-owned state and describes a failed composition. */
    private static IllegalStateException compositionFailure(
            WorldCompositionResult result, Spatial3dWorldModule spatial, ExampleResources resources) {
        resources.close();
        spatial.close();
        return new IllegalStateException("world composition failed: " + result.diagnostics());
    }

    /** Bridges the generic example host to one composed project's narrow presentation seam. */
    private static final class PresentationExample implements HostedExample {
        private final ExampleContext context;
        private final World world;
        private final Spatial3dWorldModule spatial;
        private final ExampleResources resources;
        private final Transform3d cube;
        private final Quaternionf orientation = new Quaternionf();

        private float angle;

        /** Stores the complete composed and host-owned example state. */
        private PresentationExample(
                ExampleContext context,
                World world,
                Spatial3dWorldModule spatial,
                ExampleResources resources,
                Transform3d cube) {
            this.context = context;
            this.world = world;
            this.spatial = spatial;
            this.resources = resources;
            this.cube = cube;
        }

        /** Projection synchronization happens at each render submission. */
        @Override
        public void resize() {
            // The render operation receives the current viewport aspect ratio.
        }

        /** Rotates only per-instance transform state, leaving shared resources immutable. */
        @Override
        public void update(ExampleFrame frame) {
            angle += frame.elapsedSeconds() * 0.7F;
            orientation.identity().rotateY(angle).rotateX(angle * 0.35F);
            cube.setOrientation(orientation.x, orientation.y, orientation.z, orientation.w);
        }

        /** Submits the hidden composed scene through its effectively active primary camera. */
        @Override
        public void render() {
            spatial.render(context.renderer(), context.aspectRatio());
        }

        /** Releases world-owned components and leases, then any unleased provider values. */
        @Override
        public void close() {
            try {
                world.close();
            } finally {
                resources.close();
            }
        }
    }

    /** Supplies two example resources without exposing them through entity components. */
    private static final class ExampleResources implements RuntimeResourceProvider, AutoCloseable {
        private final Mesh3dResource mesh = Mesh3dResource.owning(BoxGeometry.create(2.0F, 2.0F, 2.0F));
        private final Material3dResource material =
                Material3dResource.owning(new LambertMaterial(Color.srgb(0x35a7ff)));

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            if (reference.equals(MESH_REFERENCE)) {
                return RuntimeResourceLease.of(valueType.cast(mesh), mesh::close);
            }
            if (reference.equals(MATERIAL_REFERENCE)) {
                return RuntimeResourceLease.of(valueType.cast(material), material::close);
            }
            throw new IllegalStateException("unknown example resource " + reference);
        }

        /** Closes resources that were not transferred through a world-owned lease. */
        @Override
        public void close() {
            material.close();
            mesh.close();
        }
    }
}
