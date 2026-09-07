/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.PreparedEntityDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.SpawnOperation;
import io.github.glynch.jscene3d.project.runtime.SpawnStatus;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/** Prepares one reusable projectile definition and spawns independent instances at safe boundaries. */
public final class RuntimeSpawningExample {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.spawning-example";
    private static final AssetId WORLD_ID = AssetId.from("592c525f-c8e1-4e9e-8546-12e44598527d");
    private static final AssetId PROJECTILE_ID = AssetId.from("b78d0f03-2cdb-4142-8d41-14245cfc7453");
    private static final ComponentId EMITTER_ID = ComponentId.from("676966fc-549a-4174-a2dc-df420268808f");
    private static final ComponentId PROJECTILE_COMPONENT = ComponentId.from("6810ad02-0002-421a-8fd0-b856876795e8");
    private static final ComponentType EMITTER_TYPE = ComponentType.of(EXTENSION_ID + "/emitter", 1);
    private static final ComponentType PROJECTILE_TYPE = ComponentType.of(EXTENSION_ID + "/projectile", 1);
    private static final PropertyId SPEED = new PropertyId("speed");
    private static final Logger LOGGER = Logger.getLogger(RuntimeSpawningExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private RuntimeSpawningExample() {
        throw new AssertionError("RuntimeSpawningExample cannot be instantiated");
    }

    /**
     * Loads the supplied project directory and demonstrates fixed-boundary and idle spawning.
     *
     * @param arguments one authored asset-directory path
     */
    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one authored asset-directory path");
        }
        AssetCatalog assets = AssetCatalog.scan(
                        Path.of(arguments[0]).toAbsolutePath().normalize())
                .catalog()
                .orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(descriptor()));
        World world = WorldComposer.compose(
                        assets,
                        AssetRef.<WorldDefinition>to(WORLD_ID),
                        types,
                        List.of(new SpawningRuntimeExtension()),
                        List.of(),
                        noResources())
                .world()
                .orElseThrow();
        try (world) {
            Entity owner = world.roots().getFirst();
            Emitter emitter = owner.component(EMITTER_ID, Emitter.class).orElseThrow();
            PreparedEntityDefinition prepared = world.prepare(AssetRef.to(PROJECTILE_ID));
            emitter.prepared(prepared);
            LOGGER.info(() -> "Prepared projectile = " + (prepared.world() == world));
            LOGGER.info(() -> "Authored owner = " + owner.instantiationKind());
            world.activate();
            world.advanceFixed(Duration.ofMillis(16L));
            SpawnOperation boundary = emitter.operation();
            Entity first = boundary.entity().orElseThrow();
            Projectile firstProjectile =
                    first.component(PROJECTILE_COMPONENT, Projectile.class).orElseThrow();
            LOGGER.info(() -> "Requested status = " + emitter.statusAtRequest());
            LOGGER.info(() -> "Committed status = " + boundary.status() + ", owner children = "
                    + owner.children().size() + ", after-physics updates = " + firstProjectile.updates());
            LOGGER.info(() -> "Spawned child = " + first.instantiationKind() + ", definition = "
                    + first.instantiatedDefinition().orElseThrow());
            SpawnOperation immediate = world.spawnTarget(owner).spawn(prepared, Map.of(SPEED, number(20)));
            Entity second = immediate.entity().orElseThrow();
            Projectile secondProjectile =
                    second.component(PROJECTILE_COMPONENT, Projectile.class).orElseThrow();
            LOGGER.info(() -> "Immediate status = " + immediate.status() + ", independent identities = "
                    + !first.id().equals(second.id()) + ", speeds = [" + firstProjectile.speed() + ", "
                    + secondProjectile.speed() + "]");
        }
    }

    /** Creates safe component metadata for the example's exact runtime factories. */
    private static ExtensionDescriptor descriptor() {
        ComponentTypeDescriptor emitter = ComponentTypeDescriptor.builder(
                        EMITTER_TYPE, DescriptorPresentation.named("Projectile emitter"))
                .updatePhases(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))
                .build();
        PropertyDescriptor speed = PropertyDescriptor.required(
                SPEED.value(), ProjectValueKind.NUMBER, DescriptorPresentation.named("Speed"), Map.of(), Set.of());
        ComponentTypeDescriptor projectile = ComponentTypeDescriptor.builder(
                        PROJECTILE_TYPE, DescriptorPresentation.named("Projectile"))
                .properties(List.of(speed))
                .lifecycle(EnumSet.allOf(ComponentLifecycle.class))
                .updatePhases(Set.of(ComponentUpdatePhase.AFTER_PHYSICS))
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Runtime spawning example"),
                List.of(),
                List.of(emitter, projectile));
    }

    /** Supplies exact executable factories independently of safe authored metadata. */
    private static final class SpawningRuntimeExtension implements ComponentRuntimeExtension {
        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(EMITTER_TYPE, context -> new Emitter(context.spawnTarget()));
            registry.register(PROJECTILE_TYPE, RuntimeSpawningExample::createProjectile);
        }
    }

    /** Constructs one projectile from its validated effective instance parameter. */
    private static Projectile createProjectile(ComponentFactoryContext context) {
        ProjectValue value = Objects.requireNonNull(context.properties().get(SPEED), "speed");
        return new Projectile(((ProjectValue.NumberValue) value).value());
    }

    /** Component which requests exactly one prepared child during its first fixed update. */
    private static final class Emitter implements ComponentUpdateCallbacks {
        private final SpawnTarget target;
        private Optional<PreparedEntityDefinition> prepared = Optional.empty();
        private Optional<SpawnOperation> operation = Optional.empty();
        private Optional<SpawnStatus> statusAtRequest = Optional.empty();

        /** Stores the owner-scoped capability. */
        private Emitter(SpawnTarget target) {
            this.target = target;
        }

        /** Supplies the host-prepared definition before world activation. */
        private void prepared(PreparedEntityDefinition value) {
            prepared = Optional.of(value);
        }

        /** Returns the operation requested during the first fixed update. */
        private SpawnOperation operation() {
            return operation.orElseThrow(() -> new IllegalStateException("spawn was not requested"));
        }

        /** Returns the status observed before the requesting phase completed. */
        private SpawnStatus statusAtRequest() {
            return statusAtRequest.orElseThrow(() -> new IllegalStateException("spawn was not requested"));
        }

        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            if (operation.isPresent()) {
                return;
            }
            SpawnOperation requested = target.spawn(
                    prepared.orElseThrow(() -> new IllegalStateException("projectile is not prepared")),
                    Map.of(SPEED, number(12)));
            operation = Optional.of(requested);
            statusAtRequest = Optional.of(requested.status());
        }
    }

    /** Independent projectile component recording lifecycle and update state. */
    private static final class Projectile implements ComponentLifecycleCallbacks, ComponentUpdateCallbacks {
        private final BigDecimal speed;
        private int updates;

        /** Stores one instance's supplied speed. */
        private Projectile(BigDecimal speed) {
            this.speed = speed;
        }

        /** Returns this instance's supplied speed. */
        private BigDecimal speed() {
            return speed;
        }

        /** Returns the number of after-physics updates received. */
        private int updates() {
            return updates;
        }

        @Override
        public void onCreated() {
            // The example observes publication after the complete lifecycle transaction.
        }

        @Override
        public void onActivated() {
            // The example observes the resulting active operation.
        }

        @Override
        public void onDeactivated() {
            // No external resource requires deactivation in this example.
        }

        @Override
        public void onDestroyed() {
            // No external resource requires destruction in this example.
        }

        @Override
        public void onAfterPhysics(FixedUpdateContext update) {
            updates++;
        }
    }

    /** Creates a provider which makes accidental resource access explicit. */
    private static RuntimeResourceProvider noResources() {
        return new RuntimeResourceProvider() {
            @Override
            public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                throw new IllegalStateException("the spawning example declares no runtime resources");
            }
        };
    }

    /** Creates one portable numeric parameter. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }
}
