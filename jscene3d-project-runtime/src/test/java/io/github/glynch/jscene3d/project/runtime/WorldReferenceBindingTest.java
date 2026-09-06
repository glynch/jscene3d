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
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises authored reference binding through the public world composition interface. */
final class WorldReferenceBindingTest {
    private static final String EXTENSION_ID = "example.references";
    private static final AssetId DEFINITION_ID = AssetId.from("714d6e42-6cda-423a-aaf1-3b192e115b40");
    private static final AssetId WORLD_ID = AssetId.from("943ec0e6-e13c-4ad0-a77d-f3e7f90a0e5b");
    private static final EntityId DEFINITION_ROOT = EntityId.from("851888e0-915e-4ccf-9028-3ea26756608a");
    private static final EntityId PROBE_ENTITY = EntityId.from("b82767ab-f603-4524-ac63-45cbdbfc17a7");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("b328fd67-5355-46f6-80c1-2c28620ddd7e");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("157ac257-8b28-40fc-9919-fe804f45e49a");
    private static final EntityId EXTERNAL_BODY = EntityId.from("69850dc6-576a-4b29-a41c-e27e5bd510ef");
    private static final ComponentId BODY_COMPONENT = ComponentId.from("8eb89211-318d-49de-9fa3-d099cc957060");
    private static final ComponentId PROBE_COMPONENT = ComponentId.from("7393359c-d727-4dd1-9f5f-eb2123868104");
    private static final ComponentType BODY_TYPE = ComponentType.of(EXTENSION_ID + "/body", 1);
    private static final ComponentType PROBE_TYPE = ComponentType.of(EXTENSION_ID + "/probe", 1);
    private static final PropertyId BODY_TARGET = new PropertyId("body");
    private static final PropertyId ENTITY_TARGET = new PropertyId("entity");
    private static final PropertyId EXPORTED_BODY = new PropertyId("external-body");
    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the reference fixture defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Keeps identical authored targets isolated inside two placements of the same definition. */
    @Test
    void bindsEachDefinitionPlacementWithinItsOwnInstanceScope() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(
                        definition(EntityContract.empty()),
                        placedWorld(Map.of(), true),
                        new ReferenceExtension(events, false))
                .world()
                .orElseThrow();

        Entity first = world.roots().get(0);
        Entity second = world.roots().get(1);
        Body firstBody = first.component(BODY_COMPONENT, Body.class).orElseThrow();
        Body secondBody = second.component(BODY_COMPONENT, Body.class).orElseThrow();
        ReferenceProbe firstProbe = probe(first);
        ReferenceProbe secondProbe = probe(second);

        assertThat(firstProbe.body()).isSameAs(firstBody);
        assertThat(secondProbe.body()).isSameAs(secondBody).isNotSameAs(firstBody);
        assertThat(firstProbe.entity()).isSameAs(first);
        assertThat(secondProbe.entity()).isSameAs(second);
        assertThat(events)
                .containsExactly("create:body", "create:probe", "create:body", "create:probe", "bind", "bind");
        world.close();
    }

    /** Preserves the containing scope of a target supplied through a placed definition's public contract. */
    @Test
    void bindsContractArgumentInItsOriginalContainingScope() throws IOException {
        List<String> events = new ArrayList<>();
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        EXPORTED_BODY,
                        ProjectValueKind.COMPONENT_TARGET,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.component(PROBE_ENTITY, PROBE_COMPONENT, BODY_TARGET))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        ProjectValue externalTarget =
                new ProjectValue.ComponentTargetValue(new ComponentTarget(EXTERNAL_BODY, BODY_COMPONENT));
        World world = compose(
                        definition(contract),
                        worldWithExternalBody(Map.of(EXPORTED_BODY, externalTarget)),
                        new ReferenceExtension(events, false))
                .world()
                .orElseThrow();

        Entity external = world.roots().getFirst();
        Entity placement = world.roots().getLast();
        Body externalValue = external.component(BODY_COMPONENT, Body.class).orElseThrow();
        Body internalValue = placement.component(BODY_COMPONENT, Body.class).orElseThrow();

        assertThat(probe(placement).body()).isSameAs(externalValue).isNotSameAs(internalValue);
        world.close();
    }

    /** Converts an incompatible implementation type into a structured failure and closes every created value. */
    @Test
    void rollsBackWholeWorldAfterReferenceTypeFailure() throws IOException {
        List<String> events = new ArrayList<>();
        WorldCompositionResult result = compose(
                definition(EntityContract.empty()), placedWorld(Map.of(), false), new ReferenceExtension(events, true));

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_REFERENCE_TYPE_INVALID);
        assertThat(events).containsExactly("create:body", "create:probe", "bind", "close:probe", "close:body");
    }

    /** Rejects a factory value which cannot bind its descriptor-declared target properties. */
    @Test
    void rejectsComponentWithoutReferenceBindingCapability() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentRuntimeExtension extension = new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                registry.register(BODY_TYPE, context -> new Body(events));
                registry.register(PROBE_TYPE, context -> new ClosingValue(events));
            }
        };

        WorldCompositionResult result =
                compose(definition(EntityContract.empty()), placedWorld(Map.of(), false), extension);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(RuntimeDiagnosticCode.COMPONENT_REFERENCE_BINDING_UNSUPPORTED);
        assertThat(events).endsWith("close:unsupported", "close:body");
    }

    /** Expires the composition-only resolver immediately after one component's binding callback. */
    @Test
    void expiresResolverAfterBindingCallback() throws IOException {
        List<String> events = new ArrayList<>();
        World world = compose(
                        definition(EntityContract.empty()),
                        placedWorld(Map.of(), false),
                        new ReferenceExtension(events, false))
                .world()
                .orElseThrow();
        ReferenceProbe probe = probe(world.roots().getFirst());

        assertThatThrownBy(probe::resolveAgain)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        world.close();
    }

    /** Writes and composes one definition and world through the supported public seam. */
    private WorldCompositionResult compose(
            EntityDefinition definition, WorldDefinition world, ComponentRuntimeExtension extension)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("reference.entity.json"), definition);
        DefinitionWriter.write(temporaryDirectory.resolve("reference.world.json"), world);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(descriptor()));
        return WorldComposer.compose(assets, AssetRef.to(WORLD_ID), types, List.of(extension), List.of(), NO_RESOURCES);
    }

    /** Creates the reusable definition whose child targets its root component by stable identity. */
    private static EntityDefinition definition(EntityContract contract) {
        ComponentDefinition body =
                new ComponentDefinition(BODY_COMPONENT, BODY_TYPE.id(), BODY_TYPE.version(), Map.of());
        Map<PropertyId, ProjectValue> targets = Map.of(
                BODY_TARGET,
                new ProjectValue.ComponentTargetValue(new ComponentTarget(DEFINITION_ROOT, BODY_COMPONENT)),
                ENTITY_TARGET,
                new ProjectValue.EntityTargetValue(DEFINITION_ROOT));
        ComponentDefinition probe =
                new ComponentDefinition(PROBE_COMPONENT, PROBE_TYPE.id(), PROBE_TYPE.version(), targets);
        LocalEntity child = new LocalEntity(PROBE_ENTITY, "Probe", true, List.of(probe), List.of());
        LocalEntity root = new LocalEntity(DEFINITION_ROOT, "Body", true, List.of(body), List.of(child));
        return new EntityDefinition(DEFINITION_ID, "Reference definition", contract, List.of(), root);
    }

    /** Creates one or two placements of the reusable definition. */
    private static WorldDefinition placedWorld(Map<PropertyId, ProjectValue> arguments, boolean twice) {
        EntityPlacement first =
                new EntityPlacement(FIRST_PLACEMENT, "First", true, AssetRef.to(DEFINITION_ID), arguments);
        if (!twice) {
            return new WorldDefinition(WORLD_ID, "Reference world", List.of(first));
        }
        EntityPlacement second =
                new EntityPlacement(SECOND_PLACEMENT, "Second", true, AssetRef.to(DEFINITION_ID), arguments);
        return new WorldDefinition(WORLD_ID, "Reference world", List.of(first, second));
    }

    /** Creates an outer body and a placed definition receiving that body through its contract. */
    private static WorldDefinition worldWithExternalBody(Map<PropertyId, ProjectValue> arguments) {
        ComponentDefinition body =
                new ComponentDefinition(BODY_COMPONENT, BODY_TYPE.id(), BODY_TYPE.version(), Map.of());
        LocalEntity external = new LocalEntity(EXTERNAL_BODY, "External", true, List.of(body), List.of());
        EntityPlacement placement =
                new EntityPlacement(FIRST_PLACEMENT, "Placed", true, AssetRef.to(DEFINITION_ID), arguments);
        return new WorldDefinition(WORLD_ID, "Contract reference world", List.of(external, placement));
    }

    /** Creates safe descriptors for the body and reference-binding probe. */
    private static ExtensionDescriptor descriptor() {
        PropertyDescriptor body = PropertyDescriptor.required(
                BODY_TARGET.value(),
                ProjectValueKind.COMPONENT_TARGET,
                DescriptorPresentation.named("Body"),
                Map.of(),
                Set.of());
        PropertyDescriptor entity = PropertyDescriptor.required(
                ENTITY_TARGET.value(),
                ProjectValueKind.ENTITY_TARGET,
                DescriptorPresentation.named("Entity"),
                Map.of(),
                Set.of());
        ComponentTypeDescriptor bodyType = ComponentTypeDescriptor.builder(
                        BODY_TYPE, DescriptorPresentation.named("Body"))
                .build();
        ComponentTypeDescriptor probeType = ComponentTypeDescriptor.builder(
                        PROBE_TYPE, DescriptorPresentation.named("Reference probe"))
                .properties(List.of(body, entity))
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Reference fixture"),
                List.of(),
                List.of(bodyType, probeType));
    }

    /** Returns the probe on one placed definition's child entity. */
    private static ReferenceProbe probe(Entity placementRoot) {
        return placementRoot
                .children()
                .getFirst()
                .component(PROBE_COMPONENT, ReferenceProbe.class)
                .orElseThrow();
    }

    /** Registers deterministic body and probe factories. */
    private static final class ReferenceExtension implements ComponentRuntimeExtension {
        private final List<String> events;
        private final boolean wrongType;

        /** Stores shared observations and whether the probe deliberately requests the wrong Java type. */
        private ReferenceExtension(List<String> events, boolean wrongType) {
            this.events = events;
            this.wrongType = wrongType;
        }

        @Override
        public String id() {
            return EXTENSION_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            registry.register(BODY_TYPE, context -> {
                events.add("create:body");
                return new Body(events);
            });
            registry.register(PROBE_TYPE, context -> {
                events.add("create:probe");
                return new ReferenceProbe(events, wrongType);
            });
        }
    }

    /** Target component whose undeclared binder also proves that descriptors control participation. */
    private record Body(List<String> events) implements ComponentReferenceBinder, AutoCloseable {
        @Override
        public void bindReferences(ComponentReferenceResolver references) {
            events.add("bind:undeclared-body");
        }

        @Override
        public void close() {
            events.add("close:body");
        }
    }

    /** Runtime component which binds direct references and deliberately retains its expiring resolver for testing. */
    private static final class ReferenceProbe implements ComponentReferenceBinder, AutoCloseable {
        private final List<String> events;
        private final boolean wrongType;
        private @Nullable Entity entity;
        private @Nullable Body body;
        private @Nullable ComponentReferenceResolver retainedResolver;

        /** Stores fixture settings before reference binding. */
        private ReferenceProbe(List<String> events, boolean wrongType) {
            this.events = events;
            this.wrongType = wrongType;
        }

        @Override
        public void bindReferences(ComponentReferenceResolver references) {
            events.add("bind");
            retainedResolver = references;
            entity = references.entity(ENTITY_TARGET);
            if (wrongType) {
                references.component(BODY_TARGET, String.class);
            } else {
                body = references.component(BODY_TARGET, Body.class);
            }
        }

        /** Returns the directly bound live entity. */
        private Entity entity() {
            return Objects.requireNonNull(entity, "bound entity");
        }

        /** Returns the directly bound body component. */
        private Body body() {
            return Objects.requireNonNull(body, "bound body");
        }

        /** Deliberately attempts to reuse the short-lived resolver. */
        private Entity resolveAgain() {
            return Objects.requireNonNull(retainedResolver, "retained resolver").entity(ENTITY_TARGET);
        }

        @Override
        public void close() {
            events.add("close:probe");
        }
    }

    /** Closeable factory result deliberately lacking reference-binding support. */
    private record ClosingValue(List<String> events) implements AutoCloseable {
        @Override
        public void close() {
            events.add("close:unsupported");
        }
    }
}
