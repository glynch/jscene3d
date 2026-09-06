/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
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
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises multi-shape collision, typed signals, transform synchronization, and lifecycle end to end. */
final class Physics3dWorldCompositionTest {
    private static final AssetId WORLD_ID = AssetId.from("98787f80-47f6-48af-a7dc-e8e303f523d2");
    private static final EntityId STATIC_ENTITY = EntityId.from("58d41576-85b5-4415-92c5-ac88905536c7");
    private static final EntityId SENSOR_ENTITY = EntityId.from("22973372-2a99-4e94-a64c-1851551cb7cf");
    private static final EntityId BEHAVIOR_ENTITY = EntityId.from("6424b794-783f-43be-880d-cb146c0cf501");
    private static final ComponentId STATIC_TRANSFORM = ComponentId.from("ed374a06-510b-41bf-af3f-bf26011a47d7");
    private static final ComponentId STATIC_SHAPE = ComponentId.from("c01d2e40-5a20-466d-9e92-3a6b9a6097ae");
    private static final ComponentId STATIC_BODY = ComponentId.from("eaf73941-5117-415c-a787-50d5aa93da4d");
    private static final ComponentId SENSOR_TRANSFORM = ComponentId.from("8fc7d0f0-948a-4991-8796-74223225c3e1");
    private static final ComponentId SENSOR_BOX = ComponentId.from("d81d3a47-6486-4ce8-9bb2-7a34b60a1c24");
    private static final ComponentId SENSOR_SPHERE = ComponentId.from("ce671595-0450-4208-a1a7-6fd923b0fc44");
    private static final ComponentId SENSOR = ComponentId.from("58ee0e67-82fe-4275-a0bc-c551d155cb4c");
    private static final ComponentId BEHAVIOR = ComponentId.from("a705a889-6341-4420-8e6c-93e1c22b4afe");
    private static final String BEHAVIOR_EXTENSION = "example.collision-behavior";
    private static final ComponentType BEHAVIOR_TYPE = ComponentType.of(BEHAVIOR_EXTENSION + "/recorder", 1);
    private static final EndpointId RECEIVE_OVERLAP = new EndpointId("receive-overlap");
    private static final ResourceReference STATIC_BOX_RESOURCE = ResourceReference.asset("static-box");
    private static final ResourceReference SENSOR_BOX_RESOURCE = ResourceReference.asset("sensor-box");
    private static final ResourceReference SENSOR_SPHERE_RESOURCE = ResourceReference.asset("sensor-sphere");
    private static final Duration STEP = Duration.ofMillis(16L);

    @TempDir
    private Path temporaryDirectory;

    /** Delivers exact shape pairs after physics and emits exit when authoritative transform state separates them. */
    @Test
    void deliversTypedMultiShapeOverlapSignals() throws IOException {
        List<CollisionOverlap3d> overlaps = new ArrayList<>();
        List<String> phases = new ArrayList<>();
        ShapeResources resources = new ShapeResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Physics3dWorldModule physics = Physics3dAdapters.standard();
        World world = compose(validWorld(), resources, spatial, physics, overlaps, phases)
                .world()
                .orElseThrow();
        Entity staticEntity = world.roots().getFirst();
        Entity sensorEntity = world.roots().get(1);
        CollisionSensor3d sensor =
                sensorEntity.component(SENSOR, CollisionSensor3d.class).orElseThrow();

        assertThat(sensor.shapes())
                .extracting(CollisionShape3d::componentId)
                .containsExactly(SENSOR_BOX, SENSOR_SPHERE);
        assertThat(physics.collisionObjectCount()).isEqualTo(2);
        assertThat(physics.collisionShapeCount()).isEqualTo(3);
        world.activate();
        world.advanceFixed(STEP);

        assertThat(overlaps).hasSize(2);
        assertThat(overlaps)
                .extracting(value -> value.sensorShape().componentId())
                .containsExactly(SENSOR_BOX, SENSOR_SPHERE);
        assertThat(overlaps).allSatisfy(overlap -> {
            assertThat(overlap.sensor()).isSameAs(sensor);
            assertThat(overlap.other().componentId()).isEqualTo(STATIC_BODY);
            assertThat(overlap.otherShape().componentId()).isEqualTo(STATIC_SHAPE);
        });
        assertThat(phases).containsExactly("signal", "signal", "after-physics");

        overlaps.clear();
        phases.clear();
        world.advanceFixed(STEP);
        assertThat(overlaps).isEmpty();
        assertThat(phases).containsExactly("after-physics");

        overlaps.clear();
        phases.clear();
        Transform3d transform =
                staticEntity.component(STATIC_TRANSFORM, Transform3d.class).orElseThrow();
        transform.setPosition(10.0F, 0.0F, 0.0F);
        world.advanceFixed(STEP);

        assertThat(overlaps).hasSize(2);
        assertThat(phases).containsExactly("signal", "signal", "after-physics");
        world.close();
        assertThat(physics.isClosed()).isTrue();
        assertThat(spatial.isClosed()).isTrue();
        assertThat(resources.released()).isEqualTo(3);
    }

    /** Removes disabled and destroyed registrations through ordinary entity lifecycle control. */
    @Test
    void followsEntityEnablementAndDestruction() throws IOException {
        ShapeResources resources = new ShapeResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Physics3dWorldModule physics = Physics3dAdapters.standard();
        World world = compose(validWorld(), resources, spatial, physics, new ArrayList<>(), new ArrayList<>())
                .world()
                .orElseThrow();
        world.activate();
        Entity sensorEntity = world.roots().get(1);
        CollisionSensor3d sensor =
                sensorEntity.component(SENSOR, CollisionSensor3d.class).orElseThrow();

        world.disable(sensorEntity);
        assertThat(sensor.isActive()).isFalse();
        world.enable(sensorEntity);
        assertThat(sensor.isActive()).isTrue();
        world.destroy(sensorEntity);

        assertThat(sensor.isClosed()).isTrue();
        assertThat(physics.collisionObjectCount()).isOne();
        assertThat(physics.collisionShapeCount()).isOne();
        world.close();
        assertThat(resources.released()).isEqualTo(3);
    }

    /** Rejects shape membership across entity boundaries during transactional composition. */
    @Test
    void rejectsNonSiblingShapeMembership() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Physics3dWorldModule physics = Physics3dAdapters.standard();
        WorldDefinition invalid = invalidMembershipWorld();

        WorldCompositionResult result =
                compose(invalid, new ShapeResources(), spatial, physics, new ArrayList<>(), new ArrayList<>());

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.details().get("technicalDetail")).asString().contains("sibling component");
            assertThat(diagnostic.location()).contains("/components/");
        });
        assertThat(physics.collisionObjectCount()).isZero();
        physics.close();
        spatial.close();
    }

    /** Writes and composes one collision fixture through every supported project/runtime seam. */
    private WorldCompositionResult compose(
            WorldDefinition definition,
            RuntimeResourceProvider resources,
            Spatial3dWorldModule spatial,
            Physics3dWorldModule physics,
            List<CollisionOverlap3d> overlaps,
            List<String> phases)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("collision.world.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(
                Spatial3dDescriptors.extensionDescriptor(),
                Physics3dDescriptors.extensionDescriptor(),
                behaviorDescriptor()));
        List<ComponentRuntimeExtension> extensions = List.of(
                new Spatial3dRuntimeExtension(), new Physics3dRuntimeExtension(), behaviorExtension(overlaps, phases));
        List<WorldModuleBinding<?>> modules = List.of(
                WorldModuleBinding.of(Spatial3dWorldModule.class, spatial),
                WorldModuleBinding.of(Physics3dWorldModule.class, physics));
        return WorldComposer.compose(assets, AssetRef.to(WORLD_ID), types, extensions, modules, resources);
    }

    /** Creates the valid static-body, two-shape sensor, and signal receiver world. */
    private static WorldDefinition validWorld() {
        LocalEntity staticEntity = new LocalEntity(
                STATIC_ENTITY,
                "Static target",
                true,
                List.of(
                        collisionObject(
                                STATIC_BODY, Physics3dDescriptors.staticBodyType(), STATIC_ENTITY, STATIC_SHAPE),
                        shape(STATIC_SHAPE, STATIC_BOX_RESOURCE, Map.of()),
                        transform(STATIC_TRANSFORM)),
                List.of());
        LocalEntity sensorEntity = new LocalEntity(
                SENSOR_ENTITY,
                "Two-shape sensor",
                true,
                List.of(
                        shape(SENSOR_BOX, SENSOR_BOX_RESOURCE, Map.of()),
                        transform(SENSOR_TRANSFORM),
                        collisionObject(
                                SENSOR,
                                Physics3dDescriptors.collisionSensorType(),
                                SENSOR_ENTITY,
                                SENSOR_BOX,
                                SENSOR_SPHERE),
                        shape(
                                SENSOR_SPHERE,
                                SENSOR_SPHERE_RESOURCE,
                                Map.of(Physics3dDescriptors.localPositionProperty(), numbers(0.5F, 0.0F, 0.0F)))),
                List.of());
        ComponentDefinition behavior =
                new ComponentDefinition(BEHAVIOR, BEHAVIOR_TYPE.id(), BEHAVIOR_TYPE.version(), Map.of());
        LocalEntity behaviorEntity = new LocalEntity(BEHAVIOR_ENTITY, "Behavior", true, List.of(behavior), List.of());
        List<SignalConnection> connections = List.of(
                connection(Physics3dDescriptors.overlapEnteredSignal()),
                connection(Physics3dDescriptors.overlapExitedSignal()));
        return new WorldDefinition(
                WORLD_ID, "Collision acceptance", connections, List.of(staticEntity, sensorEntity, behaviorEntity));
    }

    /** Creates a world whose body illegally references a shape on another entity. */
    private static WorldDefinition invalidMembershipWorld() {
        LocalEntity body = new LocalEntity(
                STATIC_ENTITY,
                "Body",
                true,
                List.of(
                        transform(STATIC_TRANSFORM),
                        collisionObject(STATIC_BODY, Physics3dDescriptors.staticBodyType(), SENSOR_ENTITY, SENSOR_BOX)),
                List.of());
        LocalEntity shape = new LocalEntity(
                SENSOR_ENTITY,
                "Foreign shape",
                true,
                List.of(shape(SENSOR_BOX, SENSOR_BOX_RESOURCE, Map.of())),
                List.of());
        return new WorldDefinition(WORLD_ID, "Invalid membership", List.of(body, shape));
    }

    /** Creates one signal connection from the sensor to the behavior receiver. */
    private static SignalConnection connection(EndpointId signal) {
        return new SignalConnection(
                EndpointTarget.component(SENSOR_ENTITY, SENSOR, signal),
                EndpointTarget.component(BEHAVIOR_ENTITY, BEHAVIOR, RECEIVE_OVERLAP));
    }

    /** Creates one collision-object component with explicit shape targets. */
    private static ComponentDefinition collisionObject(
            ComponentId id, ComponentType type, EntityId shapeOwner, ComponentId... shapeIds) {
        List<ProjectValue> targets = new ArrayList<>(shapeIds.length);
        for (ComponentId shapeId : shapeIds) {
            targets.add(new ProjectValue.ComponentTargetValue(new ComponentTarget(shapeOwner, shapeId)));
        }
        return new ComponentDefinition(
                id,
                type.id(),
                type.version(),
                Map.of(Physics3dDescriptors.shapesProperty(), new ProjectValue.ArrayValue(targets)));
    }

    /** Creates one collision-shape component. */
    private static ComponentDefinition shape(
            ComponentId id, ResourceReference resource, Map<PropertyId, ProjectValue> additions) {
        Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>();
        properties.put(Physics3dDescriptors.shapeProperty(), new ProjectValue.ReferenceValue(resource));
        properties.putAll(additions);
        return new ComponentDefinition(
                id,
                Physics3dDescriptors.collisionShapeType().id(),
                Physics3dDescriptors.collisionShapeType().version(),
                properties);
    }

    /** Creates one default Transform3d component. */
    private static ComponentDefinition transform(ComponentId id) {
        return new ComponentDefinition(
                id,
                Spatial3dDescriptors.transformType().id(),
                Spatial3dDescriptors.transformType().version(),
                Map.of());
    }

    /** Describes the behavior action receiving precise overlap payloads after physics. */
    private static ExtensionDescriptor behaviorDescriptor() {
        EndpointDescriptor action = EndpointDescriptor.withPayload(
                RECEIVE_OVERLAP.value(),
                Physics3dDescriptors.overlapPayloadType(),
                DescriptorPresentation.named("Receive overlap"));
        ComponentTypeDescriptor component = ComponentTypeDescriptor.builder(
                        BEHAVIOR_TYPE, DescriptorPresentation.named("Collision recorder"))
                .actions(List.of(action))
                .updatePhases(Set.of(ComponentUpdatePhase.AFTER_PHYSICS))
                .build();
        return new ExtensionDescriptor(
                BEHAVIOR_EXTENSION,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Collision behavior fixture"),
                List.of(),
                List.of(component));
    }

    /** Creates the trusted behavior implementation used by the signal acceptance fixture. */
    private static ComponentRuntimeExtension behaviorExtension(List<CollisionOverlap3d> overlaps, List<String> phases) {
        return new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return BEHAVIOR_EXTENSION;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                registry.register(BEHAVIOR_TYPE, context -> new RecordingBehavior(overlaps, phases));
            }
        };
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(new ProjectValue.NumberValue(BigDecimal.valueOf(value)));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Records synchronous signal delivery and subsequent after-physics scheduling. */
    private record RecordingBehavior(List<CollisionOverlap3d> overlaps, List<String> phases)
            implements ComponentEndpointBinder, ComponentUpdateCallbacks {
        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            endpoints.action(RECEIVE_OVERLAP, this::receive);
        }

        /** Retains one precisely typed signal value. */
        private void receive(RuntimePayload payload) {
            overlaps.add((CollisionOverlap3d) payload.value());
            phases.add("signal");
        }

        @Override
        public void onAfterPhysics(FixedUpdateContext update) {
            phases.add("after-physics");
        }
    }

    /** Supplies independently leased immutable collision geometry without file I/O. */
    private static final class ShapeResources implements RuntimeResourceProvider {
        private int released;

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            CollisionShape3dResource resource =
                    switch (reference.locator()) {
                        case "static-box" -> new BoxCollisionShape3dResource(2.0F, 2.0F, 2.0F);
                        case "sensor-box" -> new BoxCollisionShape3dResource(0.5F, 0.5F, 0.5F);
                        case "sensor-sphere" -> new SphereCollisionShape3dResource(0.25F);
                        default -> throw new IllegalArgumentException("unknown collision resource: " + reference);
                    };
            T value = valueType.cast(resource);
            return RuntimeResourceLease.of(value, () -> {
                resource.close();
                released++;
            });
        }

        /** Returns the number of independently released resource leases. */
        private int released() {
            return released;
        }
    }
}
