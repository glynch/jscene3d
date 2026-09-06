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
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
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
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises inactive entity-world composition through its public interface. */
final class WorldComposerTest {
    private static final String EXTENSION_ID = "example.game";
    private static final AssetId DEFINITION_ASSET = AssetId.from("4947c3cd-fda8-423b-ab21-cd8073669e64");
    private static final AssetId WORLD_ASSET = AssetId.from("c3125765-280d-458a-8041-48249ab6d426");
    private static final EntityId DEFINITION_ROOT = EntityId.from("d38178b0-ef97-4934-aae0-95c7ed267b52");
    private static final EntityId DEFINITION_CHILD = EntityId.from("7ce2b765-e087-437c-b4e0-84b911c6845c");
    private static final EntityId LOCAL_ROOT = EntityId.from("752e0293-21cf-4c1e-9235-38b06cc36be8");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("ddf9d759-6b38-4717-b85a-444b30fca403");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("45ac2b84-af14-4c37-9881-4cc377ba36bd");
    private static final ComponentId VALUE_COMPONENT = ComponentId.from("f00d1dcf-a908-45a0-855c-21e55dfef9fc");
    private static final ComponentType VALUE_TYPE = ComponentType.of("example.game/value", 1);
    private static final PropertyId VALUE = new PropertyId("value");

    private static final RuntimeResourceLookup NO_RESOURCES = new RuntimeResourceLookup() {
        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the test defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Rejects invalid live identities at their public boundary. */
    @Test
    void rejectsNonPositiveRuntimeIdentity() {
        assertThatThrownBy(() -> new RuntimeEntityId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    /** Composes all roots before factories and gives repeated placements independent identity and state. */
    @Test
    void composesCompleteIndependentPlacementGraphs() throws IOException {
        EntityDefinition reusable = reusableDefinition();
        WorldDefinition definition = worldDefinition(reusable);
        RecordingFactory factory = new RecordingFactory();

        WorldCompositionResult result = compose(definition, reusable, descriptor(), factory);

        assertThat(result.isComposed()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        World world = result.world().orElseThrow();
        assertThat(world.definition()).isEqualTo(definition);
        assertThat(world.roots()).hasSize(3);
        assertThat(factory.entityCounts()).containsOnly(5);
        assertThat(world.roots())
                .extracting(Entity::authoredId)
                .containsExactly(LOCAL_ROOT, FIRST_PLACEMENT, SECOND_PLACEMENT);
        assertThat(world.roots()).extracting(Entity::id).doesNotHaveDuplicates();
    }

    /** Applies public arguments in the correct definition-instance scope without a placement wrapper. */
    @Test
    void resolvesPlacementArgumentsWithinEachInstance() throws IOException {
        EntityDefinition reusable = reusableDefinition();
        WorldDefinition definition = worldDefinition(reusable);
        RecordingFactory factory = new RecordingFactory();
        World world =
                compose(definition, reusable, descriptor(), factory).world().orElseThrow();
        Entity first = world.roots().get(1);
        Entity second = world.roots().get(2);
        RecordedComponent firstValue =
                first.component(VALUE_COMPONENT, RecordedComponent.class).orElseThrow();
        RecordedComponent secondValue =
                second.component(VALUE_COMPONENT, RecordedComponent.class).orElseThrow();

        assertThat(first.name()).contains("First beacon");
        assertThat(second.name()).contains("Beacon");
        assertThat(first.children())
                .singleElement()
                .extracting(Entity::authoredId)
                .isEqualTo(DEFINITION_CHILD);
        assertThat(first.children().getFirst().parent()).contains(first);
        assertThat(first.authoredAsset()).isEqualTo(WORLD_ASSET);
        assertThat(first.children().getFirst().authoredAsset()).isEqualTo(DEFINITION_ASSET);
        assertThat(firstValue.value()).isEqualByComparingTo("2");
        assertThat(secondValue.value()).isEqualByComparingTo("3");
        assertThat(firstValue).isNotSameAs(secondValue);
        assertThat(firstValue.owner()).isSameAs(first);
        assertThat(secondValue.world()).isSameAs(world);
    }

    /** Propagates disabled ownership while retaining each entity's independent local flag. */
    @Test
    void computesInitialEffectiveEnabledState() throws IOException {
        LocalEntity child = new LocalEntity(DEFINITION_CHILD, true, List.of(), List.of());
        LocalEntity root = new LocalEntity(LOCAL_ROOT, false, List.of(), List.of(child));
        WorldDefinition definition = new WorldDefinition(WORLD_ASSET, "Disabled world", List.of(root));

        World world = compose(definition, descriptor(), new RecordingFactory())
                .world()
                .orElseThrow();
        Entity runtimeRoot = world.roots().getFirst();
        Entity runtimeChild = runtimeRoot.children().getFirst();

        assertThat(runtimeRoot.isLocallyEnabled()).isFalse();
        assertThat(runtimeRoot.isEnabled()).isFalse();
        assertThat(runtimeChild.isLocallyEnabled()).isTrue();
        assertThat(runtimeChild.isEnabled()).isFalse();
        assertThat(world.find(runtimeChild.id())).containsSame(runtimeChild);
        assertThat(runtimeChild.component(VALUE_COMPONENT, Object.class)).isEmpty();
    }

    /** Closes successful worlds once and releases component values in reverse construction order. */
    @Test
    void closesConstructedComponentsInReverseOrder() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentFactory<ClosingComponent> factory = context -> {
            String value = text(Objects.requireNonNull(context.properties().get(VALUE), "value"));
            events.add("create:" + value);
            return new ClosingComponent(value, events);
        };
        LocalEntity root = new LocalEntity(
                LOCAL_ROOT,
                true,
                List.of(
                        component("81411df2-a65c-449b-9611-1ec68dc5a722", "1"),
                        component("0d6d3e6e-a2ab-49c5-9b04-bcb261341f39", "2")),
                List.of());
        WorldDefinition definition = new WorldDefinition(WORLD_ASSET, "Close world", List.of(root));
        ComponentTypeDescriptor multiple = descriptor(ComponentMultiplicity.MULTIPLE);
        World world = compose(definition, multiple, factory).world().orElseThrow();

        world.close();
        world.close();
        ResourceReference reference = ResourceReference.asset("unused");

        assertThat(world.isClosed()).isTrue();
        assertThat(events).containsExactly("create:1", "create:2", "close:2", "close:1");
        assertThatThrownBy(() -> world.resolveResource(reference, Object.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Withholds a partial world and rolls back prior values when one factory fails. */
    @Test
    void rollsBackFailedComposition() throws IOException {
        List<String> events = new ArrayList<>();
        ComponentFactory<ClosingComponent> factory = context -> {
            String value = text(Objects.requireNonNull(context.properties().get(VALUE), "value"));
            events.add("create:" + value);
            if ("99".equals(value)) {
                throw new IllegalStateException("deliberate failure");
            }
            return new ClosingComponent(value, events);
        };
        LocalEntity root = new LocalEntity(
                LOCAL_ROOT,
                true,
                List.of(
                        component("81411df2-a65c-449b-9611-1ec68dc5a722", "1"),
                        component("0d6d3e6e-a2ab-49c5-9b04-bcb261341f39", "2"),
                        component("b07ad64a-fd9f-482f-8000-ac644975bd45", "99")),
                List.of());
        WorldDefinition definition = new WorldDefinition(WORLD_ASSET, "Failing world", List.of(root));

        WorldCompositionResult result = compose(definition, descriptor(ComponentMultiplicity.MULTIPLE), factory);

        assertThat(result.isComposed()).isFalse();
        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(RuntimeDiagnosticCode.FACTORY_CREATE_FAILED);
            assertThat(diagnostic.location()).isEqualTo("/roots/0/components/2");
        });
        assertThat(events).containsExactly("create:1", "create:2", "create:99", "close:2", "close:1");
    }

    /** Reports duplicate executable providers as structured diagnostics rather than publishing a world. */
    @Test
    void rejectsDuplicateRuntimeExtensions() throws IOException {
        WorldDefinition definition = emptyWorld();
        ComponentRuntimeExtension provider = extension(new RecordingFactory());

        WorldCompositionResult result = compose(definition, descriptor(), List.of(provider, provider), NO_RESOURCES);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo(RuntimeDiagnosticCode.EXTENSION_DUPLICATE);
    }

    /** Converts provider registration failures into terminal structured diagnostics. */
    @Test
    void reportsRuntimeExtensionRegistrationFailure() throws IOException {
        WorldDefinition definition = emptyWorld();
        ComponentRuntimeExtension provider = new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                throw new IllegalStateException("registration failed");
            }
        };

        WorldCompositionResult result = compose(definition, descriptor(), List.of(provider), NO_RESOURCES);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo(RuntimeDiagnosticCode.EXTENSION_REGISTRATION_FAILED);
    }

    /** Prevents a factory singleton from becoming mutable state shared by two authored components. */
    @Test
    void rejectsFactoryValuesReusedAcrossComponents() throws IOException {
        Object shared = new Object();
        LocalEntity root = new LocalEntity(
                LOCAL_ROOT,
                true,
                List.of(
                        component("81411df2-a65c-449b-9611-1ec68dc5a722", "1"),
                        component("0d6d3e6e-a2ab-49c5-9b04-bcb261341f39", "2")),
                List.of());
        WorldDefinition definition = new WorldDefinition(WORLD_ASSET, "Shared state", List.of(root));

        WorldCompositionResult result =
                compose(definition, descriptor(ComponentMultiplicity.MULTIPLE), context -> shared);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo(RuntimeDiagnosticCode.FACTORY_CREATE_FAILED);
    }

    /** Delegates resource resolution without transferring ownership of the lookup to the world. */
    @Test
    void delegatesRuntimeResourceResolution() throws IOException {
        RuntimeResourceLookup resources = new RuntimeResourceLookup() {
            @Override
            public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
                return valueType.cast("resource-value");
            }
        };
        World world = compose(emptyWorld(), descriptor(), List.of(extension(new RecordingFactory())), resources)
                .world()
                .orElseThrow();
        ResourceReference reference = ResourceReference.asset("shared");

        assertThat(world.resolveResource(reference, String.class)).isEqualTo("resource-value");
    }

    /** Writes a world with no reusable definitions and composes it through the catalog. */
    private WorldCompositionResult compose(
            WorldDefinition world, ComponentTypeDescriptor descriptor, ComponentFactory<?> factory) throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("world.world.json"), world);
        return composeCatalog(world, descriptor, factory);
    }

    /** Writes one world and composes it with explicitly supplied runtime collaborators. */
    private WorldCompositionResult compose(
            WorldDefinition world,
            ComponentTypeDescriptor descriptor,
            List<ComponentRuntimeExtension> extensions,
            RuntimeResourceLookup resources)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("world.world.json"), world);
        return composeCatalog(world, descriptor, extensions, resources);
    }

    /** Writes the fixture assets and composes through the supported catalog interface. */
    private WorldCompositionResult compose(
            WorldDefinition world,
            EntityDefinition reusable,
            ComponentTypeDescriptor descriptor,
            ComponentFactory<?> factory)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("world.world.json"), world);
        DefinitionWriter.write(temporaryDirectory.resolve("reusable.entity.json"), reusable);
        return composeCatalog(world, descriptor, factory);
    }

    /** Scans written fixtures and invokes the public world composer. */
    private WorldCompositionResult composeCatalog(
            WorldDefinition world, ComponentTypeDescriptor descriptor, ComponentFactory<?> factory) {
        return composeCatalog(world, descriptor, List.of(extension(factory)), NO_RESOURCES);
    }

    /** Scans written fixtures and invokes the public world composer with explicit runtime collaborators. */
    private WorldCompositionResult composeCatalog(
            WorldDefinition world,
            ComponentTypeDescriptor descriptor,
            List<ComponentRuntimeExtension> extensions,
            RuntimeResourceLookup resources) {
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Example Game"),
                List.of(),
                List.of(descriptor))));
        return WorldComposer.compose(assets, AssetRef.to(world.id()), types, extensions, resources);
    }

    /** Creates the executable contribution for the test component type. */
    private static ComponentRuntimeExtension extension(ComponentFactory<?> factory) {
        return new ComponentRuntimeExtension() {
            @Override
            public String id() {
                return EXTENSION_ID;
            }

            @Override
            public void register(ComponentFactoryRegistry registry) {
                registry.register(VALUE_TYPE, factory);
            }
        };
    }

    /** Creates a reusable definition with one configurable root component and one owned child. */
    private static EntityDefinition reusableDefinition() {
        LocalEntity child = new LocalEntity(DEFINITION_CHILD, "Glow", true, List.of(), List.of());
        LocalEntity root = new LocalEntity(DEFINITION_ROOT, "Beacon", true, List.of(component("1")), List.of(child));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        VALUE,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(DEFINITION_ROOT, VALUE_COMPONENT, VALUE))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        return new EntityDefinition(DEFINITION_ASSET, "Beacon", contract, List.of(), root);
    }

    /** Creates a world with one local root and two placements of the same definition. */
    private static WorldDefinition worldDefinition(EntityDefinition reusable) {
        LocalEntity local = new LocalEntity(LOCAL_ROOT, "Garden", true, List.of(component("10")), List.of());
        EntityPlacement first = new EntityPlacement(
                FIRST_PLACEMENT, "First beacon", true, AssetRef.to(reusable.id()), Map.of(VALUE, number("2")));
        EntityPlacement second =
                new EntityPlacement(SECOND_PLACEMENT, true, AssetRef.to(reusable.id()), Map.of(VALUE, number("3")));
        return new WorldDefinition(WORLD_ASSET, "Garden", List.of(local, first, second));
    }

    /** Creates a component-free world suitable for registration and resource boundary tests. */
    private static WorldDefinition emptyWorld() {
        LocalEntity root = new LocalEntity(LOCAL_ROOT, "Empty", true, List.of(), List.of());
        return new WorldDefinition(WORLD_ASSET, "Empty world", List.of(root));
    }

    /** Creates a single-instance value component descriptor. */
    private static ComponentTypeDescriptor descriptor() {
        return descriptor(ComponentMultiplicity.SINGLE);
    }

    /** Creates a value component descriptor with configurable per-entity multiplicity. */
    private static ComponentTypeDescriptor descriptor(ComponentMultiplicity multiplicity) {
        PropertyDescriptor property = PropertyDescriptor.optionalWithDefault(
                VALUE.value(),
                ProjectValueKind.NUMBER,
                number("1"),
                DescriptorPresentation.named("Value"),
                Map.of(),
                Set.of());
        return ComponentTypeDescriptor.builder(VALUE_TYPE, DescriptorPresentation.named("Value"))
                .properties(List.of(property))
                .multiplicity(multiplicity)
                .build();
    }

    /** Creates one value component using the shared identity where its entity scope makes that valid. */
    private static ComponentDefinition component(String value) {
        return component(VALUE_COMPONENT.toString(), value);
    }

    /** Creates one numeric value component with an explicit stable identity. */
    private static ComponentDefinition component(String identity, String value) {
        return new ComponentDefinition(
                ComponentId.from(identity), VALUE_TYPE.id(), VALUE_TYPE.version(), Map.of(VALUE, number(value)));
    }

    /** Creates one numeric project value. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Extracts a required text representation from one numeric project value. */
    private static String text(ProjectValue value) {
        return ((ProjectValue.NumberValue) value).value().toPlainString();
    }

    /** Counts allocated entities visible through the public world hierarchy. */
    private static int entityCount(World world) {
        return world.roots().stream().mapToInt(WorldComposerTest::subtreeSize).sum();
    }

    /** Counts one allocated ownership subtree. */
    private static int subtreeSize(Entity entity) {
        return 1
                + entity.children().stream()
                        .mapToInt(WorldComposerTest::subtreeSize)
                        .sum();
    }

    /** Factory recording owner, world, values, and graph visibility. */
    private static final class RecordingFactory implements ComponentFactory<RecordedComponent> {
        private final List<Integer> entityCounts = new ArrayList<>();

        @Override
        public RecordedComponent create(ComponentFactoryContext context) {
            entityCounts.add(entityCount(context.world()));
            ProjectValue authoredValue =
                    Objects.requireNonNull(context.properties().get(VALUE), "value");
            BigDecimal value = ((ProjectValue.NumberValue) authoredValue).value();
            return new RecordedComponent(context.owner(), context.world(), value);
        }

        /** Returns complete-graph entity counts observed by each invocation. */
        List<Integer> entityCounts() {
            return List.copyOf(entityCounts);
        }
    }

    /** Ordinary runtime component used to observe construction context values. */
    private record RecordedComponent(Entity owner, World world, BigDecimal value) {}

    /** Auto-closeable runtime component used to observe release ordering. */
    private record ClosingComponent(String value, List<String> events) implements AutoCloseable {
        /** Records release without owning the observation list. */
        @Override
        public void close() {
            events.add("close:" + value);
        }
    }
}
