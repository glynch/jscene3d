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
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises transactional semantic lifecycle solely through the public world interface. */
final class WorldLifecycleTest {
    private static final String EXTENSION_ID = "example.lifecycle";
    private static final AssetId WORLD_ID = AssetId.from("46c6b191-c96e-47aa-a13a-142191b0521d");
    private static final EntityId ROOT_ID = EntityId.from("f557c58f-2cdb-447c-a98b-11a33a55e3fb");
    private static final EntityId CHILD_ID = EntityId.from("9276030d-746f-4f6c-9ecb-2ea7286b61d2");
    private static final ComponentId COMPONENT_ID = ComponentId.from("ce31ace2-bf9d-44f8-b88b-677b18ae48ce");
    private static final ComponentType TYPE = ComponentType.of(EXTENSION_ID + "/recording", 1);
    private static final PropertyId LABEL = new PropertyId("label");
    private static final Set<ComponentLifecycle> ALL_EVENTS = EnumSet.allOf(ComponentLifecycle.class);
    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the lifecycle fixture defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Creates and activates owner-first, then deactivates, destroys, and closes child-first. */
    @Test
    void activatesAndClosesInOwnershipOrder() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(world(true), descriptor(ALL_EVENTS), new LifecycleFactory(events, Map.of()));

        assertThat(world.isActive()).isFalse();
        assertThat(events).isEmpty();
        world.activate();
        assertThat(world.isActive()).isTrue();
        assertThat(events).containsExactly("created:parent", "created:child", "activated:parent", "activated:child");
        assertThatThrownBy(world::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");

        world.close();

        assertThat(world.isActive()).isFalse();
        assertThat(world.isClosed()).isTrue();
        assertThat(events)
                .containsExactly(
                        "created:parent",
                        "created:child",
                        "activated:parent",
                        "activated:child",
                        "deactivated:child",
                        "deactivated:parent",
                        "destroyed:child",
                        "destroyed:parent",
                        "close:child",
                        "close:parent");
    }

    /** Creates disabled ownership subtrees without activating or later deactivating their components. */
    @Test
    void leavesDisabledSubtreesInactive() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(world(false), descriptor(ALL_EVENTS), new LifecycleFactory(events, Map.of()));

        world.activate();
        world.close();

        assertThat(events)
                .containsExactly(
                        "created:parent",
                        "created:child",
                        "destroyed:child",
                        "destroyed:parent",
                        "close:child",
                        "close:parent");
    }

    /** Delivers only lifecycle events selected by safe descriptor metadata. */
    @Test
    void descriptorControlsLifecycleParticipation() throws IOException {
        List<String> events = new ArrayList<>();
        Set<ComponentLifecycle> declared = Set.of(ComponentLifecycle.CREATED, ComponentLifecycle.DESTROYED);
        World world = compose(world(true), descriptor(declared), new LifecycleFactory(events, Map.of()));

        world.activate();
        world.close();

        assertThat(events)
                .containsExactly(
                        "created:parent",
                        "created:child",
                        "destroyed:child",
                        "destroyed:parent",
                        "close:child",
                        "close:parent");
    }

    /** Compensates completed creation and closes every value when a later creation callback fails. */
    @Test
    void rollsBackFailedCreation() throws IOException {
        List<String> events = new ArrayList<>();
        Map<String, Set<ComponentLifecycle>> failures = Map.of("child", Set.of(ComponentLifecycle.CREATED));
        World world = compose(world(true), descriptor(ALL_EVENTS), new LifecycleFactory(events, failures));

        WorldLifecycleException failure = catchThrowableOfType(WorldLifecycleException.class, world::activate);
        RuntimeEntityId child = world.roots().getFirst().children().getFirst().id();

        assertThat(failure.event()).isEqualTo(ComponentLifecycle.CREATED);
        assertThat(failure.entity()).isEqualTo(child);
        assertThat(failure.component()).isEqualTo(COMPONENT_ID);
        assertThat(world.isClosed()).isTrue();
        assertThat(events)
                .containsExactly("created:parent", "created:child", "destroyed:parent", "close:child", "close:parent");
        assertThatThrownBy(world::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Reverses completed activation before destroying all created components after activation failure. */
    @Test
    void rollsBackFailedActivation() throws IOException {
        List<String> events = new ArrayList<>();
        Map<String, Set<ComponentLifecycle>> failures = Map.of("child", Set.of(ComponentLifecycle.ACTIVATED));
        World world = compose(world(true), descriptor(ALL_EVENTS), new LifecycleFactory(events, failures));

        WorldLifecycleException failure = catchThrowableOfType(WorldLifecycleException.class, world::activate);

        assertThat(failure.event()).isEqualTo(ComponentLifecycle.ACTIVATED);
        assertThat(events)
                .containsExactly(
                        "created:parent",
                        "created:child",
                        "activated:parent",
                        "activated:child",
                        "deactivated:parent",
                        "destroyed:child",
                        "destroyed:parent",
                        "close:child",
                        "close:parent");
    }

    /** Continues all reverse-order cleanup and suppresses later lifecycle failures. */
    @Test
    void completesCleanupAfterCallbackFailures() throws IOException {
        List<String> events = new ArrayList<>();
        Map<String, Set<ComponentLifecycle>> failures = Map.of(
                "child", Set.of(ComponentLifecycle.DEACTIVATED),
                "parent", Set.of(ComponentLifecycle.DESTROYED));
        World world = compose(world(true), descriptor(ALL_EVENTS), new LifecycleFactory(events, failures));
        world.activate();

        WorldLifecycleException failure = catchThrowableOfType(WorldLifecycleException.class, world::close);

        assertThat(failure.event()).isEqualTo(ComponentLifecycle.DEACTIVATED);
        assertThat(failure.getSuppressed()).singleElement().isInstanceOf(WorldLifecycleException.class);
        assertThat(world.isClosed()).isTrue();
        assertThat(events)
                .endsWith(
                        "deactivated:child",
                        "deactivated:parent",
                        "destroyed:child",
                        "destroyed:parent",
                        "close:child",
                        "close:parent");
    }

    /** Rejects descriptor lifecycle declarations unsupported by the factory result and closes that result. */
    @Test
    void rejectsMissingLifecycleImplementation() throws IOException {
        List<String> events = new ArrayList<>();
        ClosingOnly value = new ClosingOnly(events);
        WorldCompositionResult result = composeResult(world(true), descriptor(ALL_EVENTS), context -> value);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_LIFECYCLE_UNSUPPORTED);
        assertThat(events).containsExactly("close:unsupported");
    }

    /** Writes and composes one fixture world, requiring success. */
    private World compose(WorldDefinition definition, ComponentTypeDescriptor descriptor, ComponentFactory<?> factory)
            throws IOException {
        return composeResult(definition, descriptor, factory).world().orElseThrow();
    }

    /** Writes and composes one fixture world through the public composer. */
    private WorldCompositionResult composeResult(
            WorldDefinition definition, ComponentTypeDescriptor descriptor, ComponentFactory<?> factory)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("lifecycle.world.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Lifecycle fixture"),
                List.of(),
                List.of(descriptor))));
        return WorldComposer.compose(
                assets, AssetRef.to(definition.id()), types, List.of(extension(factory)), NO_RESOURCES);
    }

    /** Creates the fixture runtime extension for one supplied factory. */
    private static ProjectRuntimeExtension extension(ComponentFactory<?> factory) {
        return new ProjectRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ProjectRuntimeRegistry registry) {
                registry.registerComponent(TYPE, factory);
            }
        };
    }

    /** Creates a parent and child component hierarchy with configurable root enablement. */
    private static WorldDefinition world(boolean rootEnabled) {
        LocalEntity child = new LocalEntity(CHILD_ID, "Child", true, List.of(component("child")), List.of());
        LocalEntity root =
                new LocalEntity(ROOT_ID, "Parent", rootEnabled, List.of(component("parent")), List.of(child));
        return new WorldDefinition(WORLD_ID, "Lifecycle world", List.of(root));
    }

    /** Creates one authored recording component. */
    private static ComponentDefinition component(String label) {
        return new ComponentDefinition(
                COMPONENT_ID, TYPE.id(), TYPE.version(), Map.of(LABEL, new ProjectValue.TextValue(label)));
    }

    /** Creates the descriptor with an exact declared lifecycle set. */
    private static ComponentTypeDescriptor descriptor(Set<ComponentLifecycle> lifecycle) {
        PropertyDescriptor label = PropertyDescriptor.required(
                LABEL.value(), ProjectValueKind.TEXT, DescriptorPresentation.named("Label"), Map.of(), Set.of());
        return ComponentTypeDescriptor.builder(TYPE, DescriptorPresentation.named("Recording lifecycle"))
                .properties(List.of(label))
                .lifecycle(lifecycle)
                .build();
    }

    /** Creates recording lifecycle components and selects callback failures by authored label. */
    private static final class LifecycleFactory implements ComponentFactory<RecordingLifecycle> {
        private final List<String> events;
        private final Map<String, Set<ComponentLifecycle>> failures;

        /** Stores shared observations and immutable per-label failure selections. */
        private LifecycleFactory(List<String> events, Map<String, Set<ComponentLifecycle>> failures) {
            this.events = events;
            this.failures = failures;
        }

        @Override
        public RecordingLifecycle create(ComponentFactoryContext context) {
            ProjectValue value = Objects.requireNonNull(context.properties().get(LABEL), "label");
            String label = ((ProjectValue.TextValue) value).value();
            return new RecordingLifecycle(label, events, failures.getOrDefault(label, Set.of()));
        }
    }

    /** Runtime component recording every lifecycle and close invocation. */
    private static final class RecordingLifecycle implements ComponentLifecycleCallbacks, AutoCloseable {
        private final String label;
        private final List<String> events;
        private final Set<ComponentLifecycle> failures;

        /** Stores one component's identity, shared observations, and selected failures. */
        private RecordingLifecycle(String label, List<String> events, Set<ComponentLifecycle> failures) {
            this.label = label;
            this.events = events;
            this.failures = failures;
        }

        @Override
        public void onCreated() {
            recordEvent(ComponentLifecycle.CREATED);
        }

        @Override
        public void onActivated() {
            recordEvent(ComponentLifecycle.ACTIVATED);
        }

        @Override
        public void onDeactivated() {
            recordEvent(ComponentLifecycle.DEACTIVATED);
        }

        @Override
        public void onDestroyed() {
            recordEvent(ComponentLifecycle.DESTROYED);
        }

        @Override
        public void close() {
            events.add("close:" + label);
        }

        /** Records one callback before deliberately failing when selected by the fixture. */
        private void recordEvent(ComponentLifecycle event) {
            events.add(event.name().toLowerCase(Locale.ROOT) + ':' + label);
            if (failures.contains(event)) {
                throw new IllegalStateException("deliberate " + event + " failure");
            }
        }
    }

    /** Closeable value deliberately lacking semantic lifecycle support. */
    private record ClosingOnly(List<String> events) implements AutoCloseable {
        /** Records rollback closure of the unsupported value. */
        @Override
        public void close() {
            events.add("close:unsupported");
        }
    }
}
