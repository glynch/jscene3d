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
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises descriptor-authoritative endpoint wiring through the public world composer. */
final class WorldEndpointBindingTest {
    private static final String EXTENSION_ID = "example.endpoints";
    private static final AssetId DEFINITION_ID = AssetId.from("2fb9c930-f805-4498-a393-a12943408bbc");
    private static final AssetId WRAPPER_ID = AssetId.from("608797e9-d1b0-42f4-95c5-033cc0167722");
    private static final AssetId WORLD_ID = AssetId.from("7c8c2ef1-9266-4d43-b622-d71d67b3b916");
    private static final EntityId DEFINITION_ROOT = EntityId.from("e867f976-bdd3-4f35-9fcb-0c8e1c74a2bb");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("073089b6-f50e-4e5e-947d-265d370845be");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("0a245092-9cd6-4602-906b-0171c61f4791");
    private static final EntityId WRAPPER_ROOT = EntityId.from("1ef31f41-a3ee-45aa-9b0a-69173fb2895d");
    private static final EntityId INNER_PLACEMENT = EntityId.from("0619b71a-08eb-4a74-b092-ad4a8c9d5f48");
    private static final ComponentId SOURCE_COMPONENT = ComponentId.from("41856739-a95f-44e0-a1ea-8f77d7ab64da");
    private static final ComponentId TARGET_COMPONENT = ComponentId.from("e921d56f-3ae7-4111-86b4-78c868e495a4");
    private static final ComponentType SOURCE_TYPE = ComponentType.of(EXTENSION_ID + "/source", 1);
    private static final ComponentType TARGET_TYPE = ComponentType.of(EXTENSION_ID + "/target", 1);
    private static final RegisteredType PAYLOAD_TYPE = new RegisteredType(EXTENSION_ID + "/message", 1);
    private static final RegisteredType OTHER_PAYLOAD_TYPE = new RegisteredType(EXTENSION_ID + "/other", 1);
    private static final EndpointId CHANGED = new EndpointId("changed");
    private static final EndpointId PING = new EndpointId("ping");
    private static final EndpointId APPLY = new EndpointId("apply");
    private static final EndpointId RESET = new EndpointId("reset");
    private static final EndpointId PUBLIC_CHANGED = new EndpointId("public-changed");
    private static final EndpointId PUBLIC_APPLY = new EndpointId("public-apply");
    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the endpoint fixture defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Keeps internal routes isolated and supports both payload and payload-free endpoints. */
    @Test
    void routesWithinEachRepeatedDefinitionPlacement() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(definition(internalConnections()), placedWorld(List.of()), completeExtension(events))
                .world()
                .orElseThrow();
        SourceComponent firstSource = source(world.roots().getFirst());
        SourceComponent secondSource = source(world.roots().getLast());
        TargetComponent firstTarget = target(world.roots().getFirst());
        TargetComponent secondTarget = target(world.roots().getLast());

        assertThatThrownBy(() -> firstSource.send("early")).isInstanceOf(IllegalStateException.class);
        world.activate();
        firstSource.send("north");
        firstSource.ping();
        secondSource.send("south");

        assertThat(firstTarget.received()).containsExactly("north");
        assertThat(firstTarget.resetCount()).isOne();
        assertThat(secondTarget.received()).containsExactly("south");
        assertThat(secondTarget.resetCount()).isZero();
        assertThat(events.subList(0, 4))
                .containsExactly("create:source", "create:target", "create:source", "create:target");
        world.close();
        assertThatThrownBy(() -> firstSource.send("late")).isInstanceOf(IllegalStateException.class);
    }

    /** Resolves exported endpoints per placement and preserves authored listener order. */
    @Test
    void routesAcrossPlacedDefinitionContractsInDeclarationOrder() throws IOException {
        List<String> events = new ArrayList<>();
        List<SignalConnection> connections = List.of(
                new SignalConnection(
                        EndpointTarget.placement(FIRST_PLACEMENT, PUBLIC_CHANGED),
                        EndpointTarget.placement(SECOND_PLACEMENT, PUBLIC_APPLY)),
                new SignalConnection(
                        EndpointTarget.placement(FIRST_PLACEMENT, PUBLIC_CHANGED),
                        EndpointTarget.placement(FIRST_PLACEMENT, PUBLIC_APPLY)));
        World world = compose(definition(List.of()), placedWorld(connections), completeExtension(events))
                .world()
                .orElseThrow();
        world.activate();

        source(world.roots().getFirst()).send("ordered");
        source(world.roots().getLast()).send("unconnected");

        assertThat(events).endsWith("receive:second:ordered", "receive:first:ordered");
        assertThat(target(world.roots().getFirst()).received()).containsExactly("ordered");
        assertThat(target(world.roots().getLast()).received()).containsExactly("ordered");
        world.close();
    }

    /** Resolves endpoints re-exported through a nested definition without leaking private identities. */
    @Test
    void routesAcrossNestedContractReExports() throws IOException {
        List<String> events = new ArrayList<>();
        List<SignalConnection> connections = List.of(new SignalConnection(
                EndpointTarget.placement(FIRST_PLACEMENT, PUBLIC_CHANGED),
                EndpointTarget.placement(SECOND_PLACEMENT, PUBLIC_APPLY)));
        World world = compose(
                        List.of(definition(List.of()), wrapperDefinition()),
                        placedWorld(WRAPPER_ID, connections),
                        completeExtension(events))
                .world()
                .orElseThrow();
        world.activate();
        Entity firstInner = world.roots().getFirst().children().getFirst();
        Entity secondInner = world.roots().getLast().children().getFirst();

        source(firstInner).send("nested");

        assertThat(target(firstInner).received()).isEmpty();
        assertThat(target(secondInner).received()).containsExactly("nested");
        world.close();
    }

    /** Enforces the signal's exact registered payload identity at emission time. */
    @Test
    void rejectsPayloadWithAnotherRegisteredType() throws IOException {
        World world = compose(
                        definition(internalConnections()), placedWorld(List.of()), completeExtension(new ArrayList<>()))
                .world()
                .orElseThrow();
        world.activate();
        RuntimePayload wrongPayload = new RuntimePayload(OTHER_PAYLOAD_TYPE, "wrong");
        SourceComponent firstSource = source(world.roots().getFirst());

        assertThatThrownBy(() -> firstSource.send(wrongPayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PAYLOAD_TYPE.toString(), OTHER_PAYLOAD_TYPE.toString());
        world.close();
    }

    /** Rejects a factory result which cannot implement descriptor-declared endpoints. */
    @Test
    void rejectsComponentWithoutEndpointBindingCapability() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentRuntimeExtension extension = extension(
                events, () -> new ClosingValue(events, "unsupported"), () -> new TargetComponent(events, "unused"));

        WorldCompositionResult result = compose(definition(List.of()), placedWorld(List.of()), extension);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_ENDPOINT_BINDING_UNSUPPORTED);
        assertThat(events).containsExactly("create:source", "close:unsupported");
    }

    /** Rejects a binder which omits any signal or action declared by its descriptor. */
    @Test
    void rejectsUnimplementedDeclaredEndpointAndRollsBack() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentRuntimeExtension extension =
                extension(events, () -> new SourceComponent(events), () -> new IncompleteTargetComponent(events));

        WorldCompositionResult result = compose(definition(List.of()), placedWorld(List.of()), extension);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_ENDPOINT_UNIMPLEMENTED);
        assertThat(events).endsWith("close:target", "close:source");
    }

    /** Converts arbitrary binder failure to a stable diagnostic and rolls back all component values. */
    @Test
    void diagnosesEndpointBindingCallbackFailure() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentRuntimeExtension extension = extension(
                events, () -> new FailingSourceComponent(events), () -> new TargetComponent(events, "unused"));

        WorldCompositionResult result = compose(definition(List.of()), placedWorld(List.of()), extension);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_ENDPOINT_BINDING_FAILED);
        assertThat(events).endsWith("close:target", "close:source");
    }

    /** Expires the endpoint context while leaving its returned signal handle live. */
    @Test
    void expiresEndpointContextAfterBindingCallback() throws IOException {
        World world = compose(definition(List.of()), placedWorld(List.of()), completeExtension(new ArrayList<>()))
                .world()
                .orElseThrow();
        SourceComponent source = source(world.roots().getFirst());

        assertThatThrownBy(source::bindAgain)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        world.activate();
        source.send("still-live");
        world.close();
    }

    /** Writes and composes one definition and world through the supported public seam. */
    private WorldCompositionResult compose(
            EntityDefinition definition, WorldDefinition world, ComponentRuntimeExtension extension)
            throws IOException {
        return compose(List.of(definition), world, extension);
    }

    /** Writes all reusable definitions before composing one world. */
    private WorldCompositionResult compose(
            List<EntityDefinition> definitions, WorldDefinition world, ComponentRuntimeExtension extension)
            throws IOException {
        for (int index = 0; index < definitions.size(); index++) {
            DefinitionWriter.write(
                    temporaryDirectory.resolve("endpoints-" + index + ".entity.json"), definitions.get(index));
        }
        DefinitionWriter.write(temporaryDirectory.resolve("endpoints.world.json"), world);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(descriptor()));
        return WorldComposer.compose(assets, AssetRef.to(WORLD_ID), types, List.of(extension), NO_RESOURCES);
    }

    /** Creates the reusable definition with optional internal routing and exported endpoints. */
    private static EntityDefinition definition(List<SignalConnection> connections) {
        ComponentDefinition source =
                new ComponentDefinition(SOURCE_COMPONENT, SOURCE_TYPE.id(), SOURCE_TYPE.version(), Map.of());
        ComponentDefinition target =
                new ComponentDefinition(TARGET_COMPONENT, TARGET_TYPE.id(), TARGET_TYPE.version(), Map.of());
        LocalEntity root = new LocalEntity(DEFINITION_ROOT, "Endpoint root", true, List.of(source, target), List.of());
        EntityContract contract = new EntityContract(
                List.of(),
                List.of(new EntityContract.Signal(
                        PUBLIC_CHANGED,
                        PAYLOAD_TYPE,
                        EndpointTarget.component(DEFINITION_ROOT, SOURCE_COMPONENT, CHANGED))),
                List.of(new EntityContract.Action(
                        PUBLIC_APPLY,
                        PAYLOAD_TYPE,
                        EndpointTarget.component(DEFINITION_ROOT, TARGET_COMPONENT, APPLY))),
                List.of(),
                List.of(),
                List.of());
        return new EntityDefinition(DEFINITION_ID, "Endpoints", contract, connections, root);
    }

    /** Creates a definition which re-exports another definition's public endpoints unchanged. */
    private static EntityDefinition wrapperDefinition() {
        EntityPlacement inner =
                new EntityPlacement(INNER_PLACEMENT, "Inner endpoints", true, AssetRef.to(DEFINITION_ID), Map.of());
        LocalEntity root = new LocalEntity(WRAPPER_ROOT, "Wrapper", true, List.of(), List.of(inner));
        EntityContract contract = new EntityContract(
                List.of(),
                List.of(new EntityContract.Signal(
                        PUBLIC_CHANGED, PAYLOAD_TYPE, EndpointTarget.placement(INNER_PLACEMENT, PUBLIC_CHANGED))),
                List.of(new EntityContract.Action(
                        PUBLIC_APPLY, PAYLOAD_TYPE, EndpointTarget.placement(INNER_PLACEMENT, PUBLIC_APPLY))),
                List.of(),
                List.of(),
                List.of());
        return new EntityDefinition(WRAPPER_ID, "Endpoint wrapper", contract, List.of(), root);
    }

    /** Creates the definition's payload and payload-free internal connections. */
    private static List<SignalConnection> internalConnections() {
        return List.of(
                new SignalConnection(
                        EndpointTarget.component(DEFINITION_ROOT, SOURCE_COMPONENT, CHANGED),
                        EndpointTarget.component(DEFINITION_ROOT, TARGET_COMPONENT, APPLY)),
                new SignalConnection(
                        EndpointTarget.component(DEFINITION_ROOT, SOURCE_COMPONENT, PING),
                        EndpointTarget.component(DEFINITION_ROOT, TARGET_COMPONENT, RESET)));
    }

    /** Creates two placements and the supplied world-level connections. */
    private static WorldDefinition placedWorld(List<SignalConnection> connections) {
        return placedWorld(DEFINITION_ID, connections);
    }

    /** Creates two placements of the selected definition and the supplied world-level connections. */
    private static WorldDefinition placedWorld(AssetId definition, List<SignalConnection> connections) {
        EntityPlacement first = new EntityPlacement(FIRST_PLACEMENT, "First", true, AssetRef.to(definition), Map.of());
        EntityPlacement second =
                new EntityPlacement(SECOND_PLACEMENT, "Second", true, AssetRef.to(definition), Map.of());
        return new WorldDefinition(WORLD_ID, "Endpoint world", connections, List.of(first, second));
    }

    /** Creates the safe endpoint descriptors used by every fixture. */
    private static ExtensionDescriptor descriptor() {
        EndpointDescriptor changed =
                EndpointDescriptor.withPayload(CHANGED.value(), PAYLOAD_TYPE, DescriptorPresentation.named("Changed"));
        EndpointDescriptor ping = EndpointDescriptor.withoutPayload(PING.value(), DescriptorPresentation.named("Ping"));
        EndpointDescriptor apply =
                EndpointDescriptor.withPayload(APPLY.value(), PAYLOAD_TYPE, DescriptorPresentation.named("Apply"));
        EndpointDescriptor reset =
                EndpointDescriptor.withoutPayload(RESET.value(), DescriptorPresentation.named("Reset"));
        ComponentTypeDescriptor source = ComponentTypeDescriptor.builder(
                        SOURCE_TYPE, DescriptorPresentation.named("Source"))
                .signals(List.of(changed, ping))
                .build();
        ComponentTypeDescriptor target = ComponentTypeDescriptor.builder(
                        TARGET_TYPE, DescriptorPresentation.named("Target"))
                .actions(List.of(apply, reset))
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Endpoint fixture"),
                List.of(),
                List.of(source, target));
    }

    /** Creates the complete source and target runtime adapters. */
    private static ComponentRuntimeExtension completeExtension(List<String> events) {
        AtomicInteger targetIndex = new AtomicInteger();
        return extension(
                events,
                () -> new SourceComponent(events),
                () -> new TargetComponent(events, targetIndex.getAndIncrement() == 0 ? "first" : "second"));
    }

    /** Creates one runtime extension from deterministic factory-result suppliers. */
    private static ComponentRuntimeExtension extension(List<String> events, FactoryValue source, FactoryValue target) {
        return new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                registry.register(SOURCE_TYPE, context -> {
                    events.add("create:source");
                    return source.create();
                });
                registry.register(TARGET_TYPE, context -> {
                    events.add("create:target");
                    return target.create();
                });
            }
        };
    }

    /** Returns the source component on one placement root. */
    private static SourceComponent source(Entity root) {
        return root.component(SOURCE_COMPONENT, SourceComponent.class).orElseThrow();
    }

    /** Returns the target component on one placement root. */
    private static TargetComponent target(Entity root) {
        return root.component(TARGET_COMPONENT, TargetComponent.class).orElseThrow();
    }

    /** Supplies an arbitrary factory result without coupling fixtures to one implementation type. */
    @FunctionalInterface
    private interface FactoryValue {
        Object create();
    }

    /** Complete signal implementation retaining only persistent signal handles after binding. */
    private static class SourceComponent implements ComponentEndpointBinder, AutoCloseable {
        private final List<String> events;
        private @Nullable RuntimeSignal changed;
        private @Nullable RuntimeSignal ping;
        private @Nullable ComponentEndpoints retainedEndpoints;

        /** Stores shared observations before endpoint binding. */
        SourceComponent(List<String> events) {
            this.events = events;
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            events.add("bind:source");
            retainedEndpoints = endpoints;
            changed = endpoints.signal(CHANGED);
            ping = endpoints.signal(PING);
        }

        /** Emits one correctly typed payload. */
        final void send(String value) {
            send(new RuntimePayload(PAYLOAD_TYPE, value));
        }

        /** Emits a caller-supplied payload for exact-type validation. */
        final void send(RuntimePayload payload) {
            Objects.requireNonNull(changed, "changed signal").emit(payload);
        }

        /** Emits the payload-free signal. */
        final void ping() {
            Objects.requireNonNull(ping, "ping signal").emit();
        }

        /** Deliberately attempts to reuse the short-lived endpoint context. */
        final RuntimeSignal bindAgain() {
            return Objects.requireNonNull(retainedEndpoints, "retained endpoints")
                    .signal(CHANGED);
        }

        @Override
        public void close() {
            events.add("close:source");
        }
    }

    /** Complete action implementation recording payloads and payload-free calls. */
    private static final class TargetComponent implements ComponentEndpointBinder, AutoCloseable {
        private final List<String> events;
        private final String placementName;
        private final List<String> received = new ArrayList<>();
        private int resetCount;

        /** Stores shared observations before endpoint binding. */
        private TargetComponent(List<String> events, String placementName) {
            this.events = events;
            this.placementName = placementName;
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            events.add("bind:target");
            endpoints.action(APPLY, this::receive);
            endpoints.action(RESET, this::reset);
        }

        /** Records one typed payload action. */
        private void receive(RuntimePayload payload) {
            String value = (String) payload.value();
            received.add(value);
            events.add("receive:" + placementName + ":" + value);
        }

        /** Records one payload-free action. */
        private void reset() {
            resetCount++;
        }

        /** Returns received payload values. */
        private List<String> received() {
            return List.copyOf(received);
        }

        /** Returns the number of payload-free calls. */
        private int resetCount() {
            return resetCount;
        }

        @Override
        public void close() {
            events.add("close:target");
        }
    }

    /** Action implementation which deliberately omits one declared endpoint. */
    private static final class IncompleteTargetComponent implements ComponentEndpointBinder, AutoCloseable {
        private final List<String> events;

        /** Stores shared observations before endpoint binding. */
        private IncompleteTargetComponent(List<String> events) {
            this.events = events;
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            endpoints.action(APPLY, payload -> events.add("unused"));
        }

        @Override
        public void close() {
            events.add("close:target");
        }
    }

    /** Signal implementation which deliberately fails from its binding callback. */
    private static final class FailingSourceComponent extends SourceComponent {
        /** Stores shared observations before failing. */
        private FailingSourceComponent(List<String> events) {
            super(events);
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            throw new IllegalStateException("deliberate endpoint failure");
        }
    }

    /** Closeable factory result deliberately lacking endpoint-binding support. */
    private record ClosingValue(List<String> events, String name) implements AutoCloseable {
        @Override
        public void close() {
            events.add("close:" + name);
        }
    }
}
