/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises descriptor-compiled scheduling solely through the public world interface. */
final class WorldSchedulingTest {
    private static final String EXTENSION_ID = "example.schedule";
    private static final AssetId WORLD_ID = AssetId.from("d7cd8bec-0840-42a6-8ab4-ec17eb425904");
    private static final EntityId ROOT_ID = EntityId.from("142270fd-a661-4561-aa0e-02937be9fb2b");
    private static final ComponentId ALPHA_ID = ComponentId.from("a150e062-dd4f-4d57-a131-703e4d6739ec");
    private static final ComponentId ZULU_ID = ComponentId.from("8793cf0d-8f02-4fd2-a219-23ff8408f613");
    private static final ComponentType ALPHA_TYPE = ComponentType.of(EXTENSION_ID + "/alpha", 1);
    private static final ComponentType ZULU_TYPE = ComponentType.of(EXTENSION_ID + "/zulu", 1);
    private static final Set<ComponentUpdatePhase> ALL_PHASES = EnumSet.allOf(ComponentUpdatePhase.class);
    private static final Duration STEP = Duration.ofMillis(10L);
    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the scheduling fixture defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Uses descriptor phases and stable type identity rather than authored component or registration order. */
    @Test
    void executesDescriptorScheduleWithWorldOwnedTiming() throws IOException {
        List<String> events = new ArrayList<>();
        Map<ComponentType, ComponentFactory<?>> factories = new LinkedHashMap<>();
        factories.put(ZULU_TYPE, context -> new RecordingUpdates("zulu", events));
        factories.put(ALPHA_TYPE, context -> new RecordingUpdates("alpha", events));
        World world = compose(
                world(true, List.of(component(ZULU_ID, ZULU_TYPE), component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ZULU_TYPE, ALL_PHASES), descriptor(ALPHA_TYPE, ALL_PHASES)),
                factories);
        world.activate();

        world.advanceFixed(STEP);
        world.advanceFrame(Duration.ofMillis(4L), 0.25F);
        world.advanceFixed(STEP);

        assertThat(events)
                .containsExactly(
                        "before:alpha:0:PT0S",
                        "before:zulu:0:PT0S",
                        "after:alpha:0:PT0S",
                        "after:zulu:0:PT0S",
                        "frame:alpha:PT0.01S:0.25",
                        "frame:zulu:PT0.01S:0.25",
                        "before:alpha:1:PT0.01S",
                        "before:zulu:1:PT0.01S",
                        "after:alpha:1:PT0.01S",
                        "after:zulu:1:PT0.01S");
    }

    /** Invokes only phases declared by the exact component descriptor. */
    @Test
    void descriptorControlsUpdateParticipation() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(
                world(true, List.of(component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ALPHA_TYPE, Set.of(ComponentUpdatePhase.AFTER_PHYSICS))),
                Map.of(ALPHA_TYPE, context -> new RecordingUpdates("alpha", events)));
        world.activate();

        world.advanceFixed(STEP);
        world.advanceFrame(Duration.ZERO, 0.0F);

        assertThat(events).containsExactly("after:alpha:0:PT0S");
    }

    /** Omits every scheduled callback owned by an effectively disabled entity. */
    @Test
    void skipsDisabledEntities() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(
                world(false, List.of(component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ALPHA_TYPE, ALL_PHASES)),
                Map.of(ALPHA_TYPE, context -> new RecordingUpdates("alpha", events)));
        world.activate();

        world.advanceFixed(STEP);
        world.advanceFrame(Duration.ZERO, 0.0F);

        assertThat(events).isEmpty();
    }

    /** Rejects descriptor update declarations unsupported by the factory result. */
    @Test
    void rejectsMissingUpdateImplementation() throws IOException {
        WorldCompositionResult result = composeResult(
                world(true, List.of(component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ALPHA_TYPE, Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))),
                Map.of(ALPHA_TYPE, context -> new Object()));

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_UPDATE_UNSUPPORTED);
    }

    /** Requires active state and validates timing arguments at the public world interface. */
    @Test
    void rejectsInvalidStateAndTiming() throws IOException {
        World world = compose(world(true, List.of()), List.of(), Map.of());

        assertThatThrownBy(() -> world.advanceFixed(STEP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
        world.activate();
        assertThatThrownBy(() -> world.advanceFixed(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> world.advanceFrame(Duration.ZERO, Float.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
        world.close();
        assertThatThrownBy(() -> world.advanceFrame(Duration.ZERO, 0.0F))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Identifies a failing scheduled component without advancing the world clock. */
    @Test
    void identifiesUpdateFailureAndRetainsCurrentTick() throws IOException {
        List<FixedUpdateContext> attempts = new ArrayList<>();
        FailingOnceUpdates updates = new FailingOnceUpdates(attempts);
        World world = compose(
                world(true, List.of(component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ALPHA_TYPE, Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))),
                Map.of(ALPHA_TYPE, context -> updates));
        world.activate();

        WorldUpdateException failure = catchThrowableOfType(WorldUpdateException.class, () -> world.advanceFixed(STEP));
        world.advanceFixed(STEP);

        assertThat(failure.phase()).isEqualTo(ComponentUpdatePhase.BEFORE_PHYSICS);
        assertThat(failure.entity()).isEqualTo(world.roots().getFirst().id());
        assertThat(failure.component()).isEqualTo(ALPHA_ID);
        assertThat(attempts).extracting(FixedUpdateContext::tick).containsExactly(0L, 0L);
        assertThat(attempts).extracting(FixedUpdateContext::simulationTime).containsOnly(Duration.ZERO);
    }

    /** Prevents a component callback from recursively advancing or closing its world. */
    @Test
    void rejectsReentrantWorldControl() throws IOException {
        World world = compose(
                world(true, List.of(component(ALPHA_ID, ALPHA_TYPE))),
                List.of(descriptor(ALPHA_TYPE, Set.of(ComponentUpdatePhase.BEFORE_PHYSICS))),
                Map.of(ALPHA_TYPE, context -> new ReentrantUpdates(context.world())));
        world.activate();

        WorldUpdateException failure = catchThrowableOfType(WorldUpdateException.class, () -> world.advanceFixed(STEP));

        assertThat(failure.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("update is in progress");
        assertThat(world.isActive()).isTrue();
        world.close();
    }

    /** Writes and composes one fixture world, requiring success. */
    private World compose(
            WorldDefinition definition,
            List<ComponentTypeDescriptor> descriptors,
            Map<ComponentType, ComponentFactory<?>> factories)
            throws IOException {
        return composeResult(definition, descriptors, factories).world().orElseThrow();
    }

    /** Writes and composes one fixture world through the public composer. */
    private WorldCompositionResult composeResult(
            WorldDefinition definition,
            List<ComponentTypeDescriptor> descriptors,
            Map<ComponentType, ComponentFactory<?>> factories)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("schedule.world.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Schedule fixture"),
                List.of(),
                descriptors)));
        return WorldComposer.compose(
                assets, AssetRef.to(definition.id()), types, List.of(extension(factories)), NO_RESOURCES);
    }

    /** Creates the fixture runtime extension using the supplied deterministic registration order. */
    private static ComponentRuntimeExtension extension(Map<ComponentType, ComponentFactory<?>> factories) {
        return new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                factories.forEach(registry::register);
            }
        };
    }

    /** Creates one local root with configurable enablement and authored component order. */
    private static WorldDefinition world(boolean enabled, List<ComponentDefinition> components) {
        LocalEntity root = new LocalEntity(ROOT_ID, "Root", enabled, components, List.of());
        return new WorldDefinition(WORLD_ID, "Schedule world", List.of(root));
    }

    /** Creates one authored component from an exact type. */
    private static ComponentDefinition component(ComponentId id, ComponentType type) {
        return new ComponentDefinition(id, type.id(), type.version(), Map.of());
    }

    /** Creates one descriptor with exact update participation. */
    private static ComponentTypeDescriptor descriptor(ComponentType type, Set<ComponentUpdatePhase> phases) {
        return ComponentTypeDescriptor.builder(
                        type, DescriptorPresentation.named(type.id().value()))
                .updatePhases(phases)
                .build();
    }

    /** Runtime component recording every available update callback. */
    private static final class RecordingUpdates implements ComponentUpdateCallbacks {
        private final String label;
        private final List<String> events;

        /** Stores one label and shared observation list. */
        private RecordingUpdates(String label, List<String> events) {
            this.label = label;
            this.events = events;
        }

        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            events.add("before:" + label + ':' + update.tick() + ':' + update.simulationTime());
        }

        @Override
        public void onAfterPhysics(FixedUpdateContext update) {
            events.add("after:" + label + ':' + update.tick() + ':' + update.simulationTime());
        }

        @Override
        public void onFrameUpdate(FrameUpdateContext update) {
            events.add("frame:" + label + ':' + update.simulationTime() + ':' + update.interpolation());
        }
    }

    /** Runtime component failing its first fixed update and accepting the retry. */
    private static final class FailingOnceUpdates implements ComponentUpdateCallbacks {
        private final List<FixedUpdateContext> attempts;
        private boolean first = true;

        /** Stores the callback observations. */
        private FailingOnceUpdates(List<FixedUpdateContext> attempts) {
            this.attempts = attempts;
        }

        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            attempts.add(update);
            if (first) {
                first = false;
                throw new IllegalStateException("deliberate update failure");
            }
        }
    }

    /** Runtime component deliberately attempting to close its world from an update callback. */
    private record ReentrantUpdates(World world) implements ComponentUpdateCallbacks {
        /** Prevents closure while the schedule is on the call stack. */
        @Override
        public void onBeforePhysics(FixedUpdateContext update) {
            world.close();
        }
    }
}
