/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.physics3d.BoxCollisionShape3dResource;
import io.github.glynch.jscene3d.project.physics3d.CollisionOverlap3d;
import io.github.glynch.jscene3d.project.physics3d.CollisionShape3dResource;
import io.github.glynch.jscene3d.project.physics3d.Physics3dAdapters;
import io.github.glynch.jscene3d.project.physics3d.Physics3dDescriptors;
import io.github.glynch.jscene3d.project.physics3d.Physics3dRuntimeExtension;
import io.github.glynch.jscene3d.project.physics3d.Physics3dWorldModule;
import io.github.glynch.jscene3d.project.physics3d.SphereCollisionShape3dResource;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Composes authored collision objects and reports precise sensor-shape overlap transitions. */
public final class Collision3dCompositionExample {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.collision-example";
    private static final AssetId WORLD_ID = AssetId.from("89c78715-7849-41fe-a999-d316c2de4c0d");
    private static final ComponentId TARGET_TRANSFORM = ComponentId.from("fa52798f-6a4c-4e2d-8d6a-fbab834c6eb7");
    private static final ComponentId RECORDER_COMPONENT = ComponentId.from("22ddd9bb-18ac-4725-94ce-cddf4f4ff8c9");
    private static final ComponentType RECORDER_TYPE = ComponentType.of(EXTENSION_ID + "/overlap-recorder", 1);
    private static final EndpointId RECORD_ENTERED = new EndpointId("record-entered");
    private static final EndpointId RECORD_EXITED = new EndpointId("record-exited");
    private static final Logger LOGGER = Logger.getLogger(Collision3dCompositionExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private Collision3dCompositionExample() {
        throw new AssertionError("Collision3dCompositionExample cannot be instantiated");
    }

    /**
     * Composes and steps the collision world in the supplied asset directory.
     *
     * @param arguments one world asset-directory path
     */
    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one world asset-directory path");
        }
        Path assetDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        AssetCatalog assets = AssetCatalog.scan(assetDirectory).catalog().orElseThrow();
        CollisionResources resources = new CollisionResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Physics3dWorldModule physics = Physics3dAdapters.standard();
        WorldCompositionResult result = compose(assets, resources, spatial, physics);
        try (World world = result.world().orElseThrow(() -> compositionFailure(result, spatial, physics))) {
            world.activate();
            OverlapRecorder recorder = world.roots()
                    .getLast()
                    .component(RECORDER_COMPONENT, OverlapRecorder.class)
                    .orElseThrow();
            LOGGER.info(() -> "Collision objects = " + physics.collisionObjectCount() + ", shapes = "
                    + physics.collisionShapeCount());

            world.advanceFixed(Duration.ofMillis(16L));
            LOGGER.info(() -> "Entered overlaps = " + recorder.entered().size() + ", sensor shapes = "
                    + recorder.enteredShapeIds());

            Transform3d target = world.roots()
                    .getFirst()
                    .component(TARGET_TRANSFORM, Transform3d.class)
                    .orElseThrow();
            target.setPosition(10.0F, 0.0F, 0.0F);
            world.advanceFixed(Duration.ofMillis(16L));
            LOGGER.info(() ->
                    "Exited overlaps = " + recorder.exited().size() + ", sensor shapes = " + recorder.exitedShapeIds());
        }
        LOGGER.info(() -> "Physics adapter closed = " + physics.isClosed() + ", spatial adapter closed = "
                + spatial.isClosed() + ", resource leases closed = " + resources.released());
    }

    /** Composes one world from the collision, spatial, and example behavior extensions. */
    private static WorldCompositionResult compose(
            AssetCatalog assets,
            CollisionResources resources,
            Spatial3dWorldModule spatial,
            Physics3dWorldModule physics) {
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(
                Spatial3dDescriptors.extensionDescriptor(),
                Physics3dDescriptors.extensionDescriptor(),
                recorderDescriptor()));
        List<ComponentRuntimeExtension> extensions =
                List.of(new Spatial3dRuntimeExtension(), new Physics3dRuntimeExtension(), new RecorderExtension());
        List<WorldModuleBinding<?>> modules = List.of(
                WorldModuleBinding.of(Spatial3dWorldModule.class, spatial),
                WorldModuleBinding.of(Physics3dWorldModule.class, physics));
        return WorldComposer.compose(
                assets, AssetRef.<WorldDefinition>to(WORLD_ID), types, extensions, modules, resources);
    }

    /** Releases host-created modules when composition cannot transfer ownership to a world. */
    private static IllegalStateException compositionFailure(
            WorldCompositionResult result, Spatial3dWorldModule spatial, Physics3dWorldModule physics) {
        physics.close();
        spatial.close();
        return new IllegalStateException("world composition failed: " + result.diagnostics());
    }

    /** Declares the two typed behavior actions targeted by authored sensor connections. */
    private static ExtensionDescriptor recorderDescriptor() {
        EndpointDescriptor entered = EndpointDescriptor.withPayload(
                RECORD_ENTERED.value(),
                Physics3dDescriptors.overlapPayloadType(),
                DescriptorPresentation.named("Record entered overlap"));
        EndpointDescriptor exited = EndpointDescriptor.withPayload(
                RECORD_EXITED.value(),
                Physics3dDescriptors.overlapPayloadType(),
                DescriptorPresentation.named("Record exited overlap"));
        ComponentTypeDescriptor recorder = ComponentTypeDescriptor.builder(
                        RECORDER_TYPE, DescriptorPresentation.named("Overlap recorder"))
                .actions(List.of(entered, exited))
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Collision example behavior"),
                List.of(),
                List.of(recorder));
    }

    /** Supplies the executable behavior factory independently of its safe descriptor. */
    private static final class RecorderExtension implements ComponentRuntimeExtension {
        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(RECORDER_TYPE, context -> new OverlapRecorder());
        }
    }

    /** Records each precise overlap payload delivered through the authored connections. */
    private static final class OverlapRecorder implements ComponentEndpointBinder {
        private final List<CollisionOverlap3d> entered = new ArrayList<>();
        private final List<CollisionOverlap3d> exited = new ArrayList<>();

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            endpoints.action(RECORD_ENTERED, payload -> entered.add(overlap(payload)));
            endpoints.action(RECORD_EXITED, payload -> exited.add(overlap(payload)));
        }

        /** Returns the entered transitions in delivery order. */
        private List<CollisionOverlap3d> entered() {
            return List.copyOf(entered);
        }

        /** Returns the exited transitions in delivery order. */
        private List<CollisionOverlap3d> exited() {
            return List.copyOf(exited);
        }

        /** Formats the exact sensor shape identities involved in entered transitions. */
        private List<ComponentId> enteredShapeIds() {
            return entered.stream()
                    .map(overlap -> overlap.sensorShape().componentId())
                    .toList();
        }

        /** Formats the exact sensor shape identities involved in exited transitions. */
        private List<ComponentId> exitedShapeIds() {
            return exited.stream()
                    .map(overlap -> overlap.sensorShape().componentId())
                    .toList();
        }

        /** Extracts the declared collision payload. */
        private static CollisionOverlap3d overlap(RuntimePayload payload) {
            return (CollisionOverlap3d) payload.value();
        }
    }

    /** Supplies independently leased immutable geometry for the three authored shape components. */
    private static final class CollisionResources implements RuntimeResourceProvider {
        private int released;

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            CollisionShape3dResource resource =
                    switch (reference.locator()) {
                        case "target-box" -> new BoxCollisionShape3dResource(2.0F, 2.0F, 2.0F);
                        case "sensor-box" -> new BoxCollisionShape3dResource(0.5F, 0.5F, 0.5F);
                        case "sensor-sphere" -> new SphereCollisionShape3dResource(0.25F);
                        default -> throw new IllegalArgumentException("unknown collision resource: " + reference);
                    };
            return RuntimeResourceLease.of(valueType.cast(resource), () -> {
                resource.close();
                released++;
            });
        }

        /** Returns the number of released world-owned leases. */
        private int released() {
            return released;
        }
    }
}
