/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises preparation and transactional owner-scoped spawning through the public runtime interface. */
final class WorldSpawningTest {
    private static final String EXTENSION_ID = "example.spawn";
    private static final AssetId WORLD_ID = AssetId.from("4ddba976-2960-41a5-a949-2997205b2bed");
    private static final AssetId PROJECTILE_ID = AssetId.from("6696491a-df32-49d8-b433-786e676126aa");
    private static final EntityId EMITTER_ID = EntityId.from("75e3b289-58df-436a-b535-dff3e69cc0b4");
    private static final EntityId PROJECTILE_ROOT = EntityId.from("d2345c2c-0a2a-4302-bc84-a0ad8f998347");
    private static final EntityId PROJECTILE_CHILD = EntityId.from("919414f8-21c8-474f-88d8-1a5cd7a39885");
    private static final ComponentId EMITTER_COMPONENT = ComponentId.from("93759631-6771-4e1a-b69e-553f67317c31");
    private static final ComponentId PROJECTILE_COMPONENT = ComponentId.from("6c095158-cdd4-49cd-bfad-984a65f14183");
    private static final ComponentType EMITTER_TYPE = ComponentType.of(EXTENSION_ID + "/emitter", 1);
    private static final ComponentType PROJECTILE_TYPE = ComponentType.of(EXTENSION_ID + "/projectile", 1);
    private static final PropertyId SPEED = new PropertyId("speed");
    private static final PropertyId RESOURCE = new PropertyId("resource");
    private static final ResourceReference RESOURCE_REFERENCE = ResourceReference.asset("projectile-resource");
    private static final Duration STEP = Duration.ofMillis(16L);

    @TempDir
    private Path temporaryDirectory;

    /** Commits a fixed-update request at the phase boundary and schedules it only for later phases. */
    @Test
    void spawnsPreparedDefinitionAtFixedPhaseBoundary() throws IOException {
        List<String> events = new ArrayList<>();
        SpawnFixture fixture = compose(events, resourceProvider(events));
        PreparedEntityDefinition prepared = fixture.world().prepare(AssetRef.to(PROJECTILE_ID));
        fixture.emitter().prepared(prepared);
        fixture.world().activate();

        fixture.world().advanceFixed(STEP);

        SpawnOperation operation = fixture.emitter().operation();
        Entity spawned = operation.entity().orElseThrow();
        assertThat(events)
                .containsExactly(
                        "acquire:asset:projectile-resource",
                        "before:emitter",
                        "request:PENDING",
                        "created:7",
                        "activated:7",
                        "after:7:0");
        assertThat(operation.status()).isEqualTo(SpawnStatus.ACTIVE);
        assertThat(operation.diagnostics()).isEmpty();
        assertThat(spawned.instantiationKind()).isEqualTo(EntityInstantiationKind.SPAWN);
        assertThat(spawned.instantiatedDefinition()).contains(PROJECTILE_ID);
        assertThat(spawned.parent()).containsSame(fixture.owner());
        assertThat(spawned.children()).singleElement().satisfies(child -> {
            assertThat(child.authoredId()).isEqualTo(PROJECTILE_CHILD);
            assertThat(child.instantiationKind()).isEqualTo(EntityInstantiationKind.LOCAL_ENTITY);
            assertThat(child.instantiatedDefinition()).isEmpty();
        });
        assertThat(fixture.world().find(spawned.id())).containsSame(spawned);
        fixture.world().close();
        assertThat(events).endsWith("release:asset:projectile-resource");
    }

    /** Reuses one preparation while allocating independent entity and component state for each instance. */
    @Test
    void reusesPreparationForIndependentInstances() throws IOException {
        List<String> events = new ArrayList<>();
        SpawnFixture fixture = compose(events, resourceProvider(events));
        PreparedEntityDefinition prepared = fixture.world().prepare(AssetRef.to(PROJECTILE_ID));
        fixture.world().activate();
        SpawnTarget target = fixture.world().spawnTarget(fixture.owner());

        SpawnOperation first = target.spawn(prepared, Map.of(SPEED, number(3)));
        SpawnOperation second = target.spawn(prepared, Map.of(SPEED, number(9)));

        Entity firstEntity = first.entity().orElseThrow();
        Entity secondEntity = second.entity().orElseThrow();
        Projectile firstValue =
                firstEntity.component(PROJECTILE_COMPONENT, Projectile.class).orElseThrow();
        Projectile secondValue =
                secondEntity.component(PROJECTILE_COMPONENT, Projectile.class).orElseThrow();
        assertThat(first.status()).isEqualTo(SpawnStatus.ACTIVE);
        assertThat(second.status()).isEqualTo(SpawnStatus.ACTIVE);
        assertThat(firstEntity.id()).isNotEqualTo(secondEntity.id());
        assertThat(firstValue).isNotSameAs(secondValue);
        assertThat(firstValue.speed()).isEqualByComparingTo("3");
        assertThat(secondValue.speed()).isEqualByComparingTo("9");
        assertThat(events).filteredOn(value -> value.startsWith("acquire:")).hasSize(1);
        fixture.world().close();
    }

    /** Cancels a queued request without allocating or exposing an entity. */
    @Test
    void cancelsPendingSpawn() throws IOException {
        List<String> events = new ArrayList<>();
        SpawnFixture fixture = compose(events, resourceProvider(events));
        PreparedEntityDefinition prepared = fixture.world().prepare(AssetRef.to(PROJECTILE_ID));
        fixture.emitter().prepared(prepared);
        fixture.emitter().cancelRequest();
        fixture.world().activate();

        fixture.world().advanceFixed(STEP);

        SpawnOperation operation = fixture.emitter().operation();
        assertThat(operation.status()).isEqualTo(SpawnStatus.CANCELLED);
        assertThat(operation.entity()).isEmpty();
        assertThat(operation.diagnostics()).isEmpty();
        assertThat(fixture.owner().children()).isEmpty();
        fixture.world().close();
    }

    /** Reports invalid instance parameters without changing the existing live world. */
    @Test
    void rejectsInvalidSpawnArgumentsTransactionally() throws IOException {
        List<String> events = new ArrayList<>();
        SpawnFixture fixture = compose(events, resourceProvider(events));
        PreparedEntityDefinition prepared = fixture.world().prepare(AssetRef.to(PROJECTILE_ID));
        fixture.world().activate();

        SpawnOperation operation = fixture.world()
                .spawnTarget(fixture.owner())
                .spawn(prepared, Map.of(new PropertyId("unknown"), number(1)));

        assertThat(operation.status()).isEqualTo(SpawnStatus.FAILED);
        assertThat(operation.entity()).isEmpty();
        assertThat(operation.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.SPAWN_ARGUMENT_INVALID);
        assertThat(fixture.owner().children()).isEmpty();
        assertThat(fixture.world().isActive()).isTrue();
        fixture.world().close();
    }

    /** Restricts preparation to inactive worlds and prepared handles to their owning world. */
    @Test
    void enforcesPreparationAndWorldOwnership() throws IOException {
        List<String> firstEvents = new ArrayList<>();
        List<String> secondEvents = new ArrayList<>();
        SpawnFixture first = composeIn(temporaryDirectory.resolve("first"), firstEvents, resourceProvider(firstEvents));
        SpawnFixture second =
                composeIn(temporaryDirectory.resolve("second"), secondEvents, resourceProvider(secondEvents));
        World firstWorld = first.world();
        PreparedEntityDefinition prepared = firstWorld.prepare(AssetRef.to(PROJECTILE_ID));
        firstWorld.activate();
        second.world().activate();
        SpawnTarget foreignTarget = second.world().spawnTarget(second.owner());
        AssetRef<EntityDefinition> projectileReference = AssetRef.to(PROJECTILE_ID);

        assertThatThrownBy(() -> firstWorld.prepare(projectileReference))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("before world activation");
        assertThatThrownBy(() -> foreignTarget.spawn(prepared))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another world");
        firstWorld.close();
        second.world().close();
    }

    /** Writes one complete spawn fixture and returns its composed inactive world. */
    private SpawnFixture compose(List<String> events, RuntimeResourceProvider resources) throws IOException {
        return composeIn(temporaryDirectory, events, resources);
    }

    /** Writes one fixture into an isolated project directory. */
    private SpawnFixture composeIn(Path directory, List<String> events, RuntimeResourceProvider resources)
            throws IOException {
        Files.createDirectories(directory);
        EntityDefinition projectile = projectileDefinition();
        WorldDefinition definition = worldDefinition();
        DefinitionWriter.write(directory.resolve("spawn.world.json"), definition);
        DefinitionWriter.write(directory.resolve("projectile.entity.json"), projectile);
        AssetCatalog assets = AssetCatalog.scan(directory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Spawn fixture"),
                List.of(),
                List.of(emitterDescriptor(), projectileDescriptor()))));
        EmitterFactory emitterFactory = new EmitterFactory(events);
        ProjectileFactory projectileFactory = new ProjectileFactory(events);
        World world = WorldComposer.compose(
                        assets,
                        AssetRef.to(WORLD_ID),
                        types,
                        List.of(extension(emitterFactory, projectileFactory)),
                        List.of(),
                        resources)
                .world()
                .orElseThrow();
        Emitter emitter = world.roots()
                .getFirst()
                .component(EMITTER_COMPONENT, Emitter.class)
                .orElseThrow();
        return new SpawnFixture(world, world.roots().getFirst(), emitter);
    }

    /** Creates the emitter-only startup world. */
    private static WorldDefinition worldDefinition() {
        ComponentDefinition emitter =
                new ComponentDefinition(EMITTER_COMPONENT, EMITTER_TYPE.id(), EMITTER_TYPE.version(), Map.of());
        LocalEntity root = new LocalEntity(EMITTER_ID, "Emitter", true, List.of(emitter), List.of());
        return new WorldDefinition(WORLD_ID, "Spawn world", List.of(root));
    }

    /** Creates one reusable projectile definition with a non-component child. */
    private static EntityDefinition projectileDefinition() {
        ComponentDefinition component = new ComponentDefinition(
                PROJECTILE_COMPONENT,
                PROJECTILE_TYPE.id(),
                PROJECTILE_TYPE.version(),
                Map.of(SPEED, number(1), RESOURCE, new ProjectValue.ReferenceValue(RESOURCE_REFERENCE)));
        LocalEntity child = new LocalEntity(PROJECTILE_CHILD, "Trail", true, List.of(), List.of());
        LocalEntity root = new LocalEntity(PROJECTILE_ROOT, "Projectile", true, List.of(component), List.of(child));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        SPEED,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.component(PROJECTILE_ROOT, PROJECTILE_COMPONENT, SPEED))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        return new EntityDefinition(PROJECTILE_ID, "Projectile", contract, List.of(), root);
    }

    /** Creates the fixed-update emitter descriptor. */
    private static ComponentTypeDescriptor emitterDescriptor() {
        return ComponentTypeDescriptor.builder(EMITTER_TYPE, DescriptorPresentation.named("Emitter"))
                .updatePhases(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))
                .build();
    }

    /** Creates the lifecycle, resource, parameter, and update declarations for projectiles. */
    private static ComponentTypeDescriptor projectileDescriptor() {
        PropertyDescriptor speed = PropertyDescriptor.required(
                SPEED.value(), ProjectValueKind.NUMBER, DescriptorPresentation.named("Speed"), Map.of(), Set.of());
        PropertyDescriptor resource = PropertyDescriptor.required(
                RESOURCE.value(),
                ProjectValueKind.REFERENCE,
                DescriptorPresentation.named("Resource"),
                Map.of(),
                Set.of(ResourceReference.Kind.ASSET));
        return ComponentTypeDescriptor.builder(PROJECTILE_TYPE, DescriptorPresentation.named("Projectile"))
                .properties(List.of(speed, resource))
                .lifecycle(EnumSet.allOf(ComponentLifecycle.class))
                .updatePhases(Set.of(ComponentUpdatePhase.AFTER_PHYSICS))
                .build();
    }

    /** Registers the two exact descriptor-backed component factories. */
    private static ComponentRuntimeExtension extension(
            ComponentFactory<?> emitterFactory, ComponentFactory<?> projectileFactory) {
        return new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                registry.register(EMITTER_TYPE, emitterFactory);
                registry.register(PROJECTILE_TYPE, projectileFactory);
            }
        };
    }

    /** Creates a provider which records one independently releasable shared string resource. */
    private static RuntimeResourceProvider resourceProvider(List<String> events) {
        return new RuntimeResourceProvider() {
            @Override
            public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                events.add("acquire:" + reference);
                T value = valueType.cast("prepared-resource");
                return RuntimeResourceLease.of(value, () -> events.add("release:" + reference));
            }
        };
    }

    /** Creates one portable number. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Composed world and the component used to request a spawn. */
    private record SpawnFixture(World world, Entity owner, Emitter emitter) {}

    /** Factory retaining the owner-scoped target required after world activation. */
    private static final class EmitterFactory implements ComponentFactory<Emitter> {
        private final List<String> events;

        /** Stores shared observations. */
        private EmitterFactory(List<String> events) {
            this.events = events;
        }

        @Override
        public Emitter create(ComponentFactoryContext context) {
            return new Emitter(context.spawnTarget(), events);
        }
    }

    /** Fixed-update behavior which makes exactly one configurable spawn request. */
    private static final class Emitter implements ComponentUpdateCallbacks {
        private final SpawnTarget target;
        private final List<String> events;
        private @Nullable PreparedEntityDefinition prepared;
        private @Nullable SpawnOperation operation;
        private boolean cancelRequest;

        /** Stores the scoped capability and shared observations. */
        private Emitter(SpawnTarget target, List<String> events) {
            this.target = target;
            this.events = events;
        }

        /** Supplies content prepared by the host before activation. */
        private void prepared(PreparedEntityDefinition value) {
            prepared = value;
        }

        /** Selects cancellation during the requesting callback. */
        private void cancelRequest() {
            cancelRequest = true;
        }

        /** Returns the requested operation after the first fixed update. */
        private SpawnOperation operation() {
            return Objects.requireNonNull(operation, "spawn operation");
        }

        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            if (operation != null) {
                return;
            }
            events.add("before:emitter");
            operation = target.spawn(Objects.requireNonNull(prepared, "prepared definition"), Map.of(SPEED, number(7)));
            events.add("request:" + operation.status());
            if (cancelRequest) {
                operation.cancel();
            }
        }
    }

    /** Resource-aware factory proving acquisition happens during preparation rather than spawning. */
    private static final class ProjectileFactory implements ComponentFactory<Projectile> {
        private final List<String> events;

        /** Stores shared observations. */
        private ProjectileFactory(List<String> events) {
            this.events = events;
        }

        @Override
        public void prepare(ComponentPreparationContext context) {
            ResourceReference reference = reference(context.properties());
            context.resolveResource(reference, String.class);
        }

        @Override
        public Projectile create(ComponentFactoryContext context) {
            ResourceReference reference = reference(context.properties());
            String resource = context.resolveResource(reference, String.class);
            ProjectValue speedValue = context.properties().value(SPEED);
            BigDecimal speed = ((ProjectValue.NumberValue) speedValue).value();
            return new Projectile(speed, resource, events);
        }

        /** Reads one required portable resource reference. */
        private static ResourceReference reference(ComponentProperties properties) {
            ProjectValue value = properties.value(RESOURCE);
            return ((ProjectValue.ReferenceValue) value).reference();
        }
    }

    /** Spawned lifecycle and update behavior with independent mutable lifecycle state. */
    private static final class Projectile
            implements ComponentLifecycleCallbacks, ComponentUpdateCallbacks, AutoCloseable {
        private final BigDecimal speed;
        private final String resource;
        private final List<String> events;

        /** Stores prepared values and shared observations. */
        private Projectile(BigDecimal speed, String resource, List<String> events) {
            this.speed = speed;
            this.resource = resource;
            this.events = events;
        }

        /** Returns this instance's contract-supplied speed. */
        private BigDecimal speed() {
            return speed;
        }

        @Override
        public void onCreated() {
            events.add("created:" + speed);
        }

        @Override
        public void onActivated() {
            events.add("activated:" + speed);
        }

        @Override
        public void onAfterPhysics(FixedUpdateContext update) {
            events.add("after:" + speed + ':' + update.tick());
        }

        @Override
        public void onDeactivated() {
            events.add("deactivated:" + speed);
        }

        @Override
        public void onDestroyed() {
            events.add("destroyed:" + speed);
        }

        @Override
        public void close() {
            events.add("close:" + speed + ':' + resource);
        }
    }
}
