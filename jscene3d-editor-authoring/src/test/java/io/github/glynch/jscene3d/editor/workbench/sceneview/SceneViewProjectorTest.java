/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.sceneview;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionResolvers;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionPlan;
import io.github.glynch.jscene3d.project.composition.CompositionPlanner;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.standard.StandardProjectDescriptors;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies safe projection from generic composition plans into the first Scene View model. */
final class SceneViewProjectorTest {
    private static final AssetId SCENE = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final AssetId VISUAL_DEFINITION = AssetId.from("22222222-2222-4222-8222-222222222222");
    private static final EntityId SCENE_ROOT = EntityId.from("33333333-3333-4333-8333-333333333333");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("44444444-4444-4444-8444-444444444444");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("55555555-5555-4555-8555-555555555555");
    private static final EntityId DEFINITION_ROOT = EntityId.from("66666666-6666-4666-8666-666666666666");
    private static final EntityId DEFINITION_CHILD = EntityId.from("77777777-7777-4777-8777-777777777777");
    private static final ComponentId TRANSFORM = ComponentId.from("88888888-8888-4888-8888-888888888888");
    private static final ComponentId MESH = ComponentId.from("99999999-9999-4999-8999-999999999999");
    private static final ComponentId LIGHT = ComponentId.from("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final ComponentId CUSTOM = ComponentId.from("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final PropertyId POSITION_ARGUMENT = new PropertyId("position-argument");
    private static final PropertyId INTENSITY_ARGUMENT = new PropertyId("intensity-argument");
    private static final PropertyId MESH_ARGUMENT = new PropertyId("mesh-argument");
    private static final PropertyId MATERIAL_ARGUMENT = new PropertyId("material-argument");
    private static final ComponentType CUSTOM_TYPE = ComponentType.of("example.scene-view/custom", 1);

    @TempDir
    private Path temporaryDirectory;

    /** Preserves repeated placement hierarchy, identity, and effective enabled state. */
    @Test
    void projectsRepeatedPlacementIdentityAndEffectiveState() throws IOException {
        ProjectionFixture fixture = projection();
        SceneViewSnapshot snapshot = fixture.snapshot();
        assertThat(snapshot).returns(SCENE, SceneViewSnapshot::scene).returns(7L, SceneViewSnapshot::revision);
        assertThat(snapshot.occurrences()).hasSize(5);
        VisualOccurrence first = occurrence(snapshot, FIRST_PLACEMENT);
        VisualOccurrence second = occurrence(snapshot, SECOND_PLACEMENT);
        assertThat(first.occurrence()).isNotEqualTo(second.occurrence());
        assertThat(first.parent()).contains(occurrence(snapshot, SCENE_ROOT).occurrence());
        assertThat(first.enabled()).isFalse();
        assertThat(second.enabled()).isTrue();
        assertThat(snapshot.occurrences().stream()
                        .filter(occurrence -> occurrence.authoredEntity().equals(DEFINITION_CHILD))
                        .map(VisualOccurrence::enabled))
                .containsExactly(false, true);
        assertThat(first.authoredSource()).isEqualTo(fixture.source().toUri());
    }

    /** Projects only the effective built-in visual values and safe resource references. */
    @Test
    void projectsSupportedBuiltInVisualSemantics() throws IOException {
        SceneViewSnapshot snapshot = projection().snapshot();
        VisualOccurrence first = occurrence(snapshot, FIRST_PLACEMENT);
        VisualOccurrence second = occurrence(snapshot, SECOND_PLACEMENT);

        assertThat(first.transform())
                .get()
                .extracting(transform -> transform.position().x())
                .isEqualTo(new BigDecimal("1"));
        assertThat(second.transform())
                .get()
                .extracting(transform -> transform.position().x())
                .isEqualTo(new BigDecimal("4"));
        assertThat(second.transform()).get().satisfies(transform -> {
            assertThat(transform.position()).isEqualTo(vector("4", "5", "6"));
            assertThat(transform.orientationDegrees()).isEqualTo(vector("10", "20", "30"));
            assertThat(transform.scale()).isEqualTo(vector("2", "3", "4"));
            assertThat(transform.identity().occurrence()).isEqualTo(second.occurrence());
            assertThat(transform.identity().scope().definition()).isEqualTo(VISUAL_DEFINITION);
            assertThat(transform.identity().component()).isEqualTo(TRANSFORM);
        });
        assertThat(second.meshes()).singleElement().satisfies(mesh -> {
            assertThat(mesh.mesh()).isEqualTo(ResourceReference.asset("second-mesh"));
            assertThat(mesh.material()).isEqualTo(ResourceReference.asset("second-material"));
            assertThat(mesh.visible()).isTrue();
            assertThat(mesh.identity().component()).isEqualTo(MESH);
        });
        assertThat(second.directionalLight()).get().satisfies(light -> {
            assertThat(light.color()).isEqualTo(vector("0.25", "0.5", "0.75"));
            assertThat(light.intensity()).isEqualByComparingTo("3.5");
            assertThat(light.target()).isEqualTo(vector("7", "8", "9"));
            assertThat(light.identity().component()).isEqualTo(LIGHT);
        });
        assertThat(first.transform().orElseThrow().identity().occurrence())
                .isNotEqualTo(second.transform().orElseThrow().identity().occurrence());
    }

    /** Resolves generated definitions while safely ignoring project-defined components. */
    @Test
    void projectsGeneratedDefinitionsAndIgnoresCustomComponents() throws IOException {
        ProjectionFixture fixture = projection();

        assertThat(fixture.plan().definitionSources()).containsEntry(VISUAL_DEFINITION, fixture.generatedSource());
        assertThat(projectedComponentIds(fixture.snapshot()))
                .containsOnly(TRANSFORM, MESH, LIGHT)
                .doesNotContain(CUSTOM);
    }

    /** Keeps the exported snapshot and module graph free of runtime and renderer types. */
    @Test
    void exposesOnlyRuntimeFreeSnapshotTypes() {
        Set<String> requires = getClass().getModule().getDescriptor().requires().stream()
                .map(requirement -> requirement.name())
                .collect(Collectors.toUnmodifiableSet());

        assertThat(requires)
                .doesNotContain(
                        "io.github.glynch.jscene3d.project.runtime",
                        "io.github.glynch.jscene3d.project.spatial3d",
                        "io.github.glynch.jscene3d.lwjgl");
        assertThat(SceneViewSnapshot.class.getRecordComponents())
                .extracting(component -> component.getType().getPackageName())
                .allMatch(packageName -> packageName.startsWith("io.github.glynch.jscene3d.project")
                        || packageName.startsWith("io.github.glynch.jscene3d.editor")
                        || packageName.startsWith("java."));
    }

    /** Builds one complete generated-definition projection fixture. */
    private ProjectionFixture projection() throws IOException {
        SceneDefinition scene = scene();
        Path source = temporaryDirectory.resolve("nested/visual.scene.json");
        DefinitionWriter.write(source, scene);
        AssetCatalog authored = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        ByteArrayOutputStream generated = new ByteArrayOutputStream();
        DefinitionWriter.write(generated, visualDefinition());
        URI generatedSource = URI.create("import:scene-view/visual.entity.json");
        DefinitionResolver resolver = DefinitionResolvers.builder(authored)
                .addGeneratedEntity(
                        VISUAL_DEFINITION, generatedSource, new ByteArrayInputStream(generated.toByteArray()))
                .build();
        CompositionPlan plan = CompositionPlanner.plan(source.toUri(), scene, resolver, types())
                .plan()
                .orElseThrow();
        return new ProjectionFixture(SceneViewProjector.project(plan, 7L), plan, source, generatedSource);
    }

    /** Finds the unique occurrence whose authored local identity matches the fixture. */
    private static VisualOccurrence occurrence(SceneViewSnapshot snapshot, EntityId entity) {
        return snapshot.occurrences().stream()
                .filter(occurrence -> occurrence.authoredEntity().equals(entity))
                .findFirst()
                .orElseThrow();
    }

    /** Collects every component identity actually exposed by the narrow snapshot. */
    private static List<ComponentId> projectedComponentIds(SceneViewSnapshot snapshot) {
        List<ComponentId> components = new ArrayList<>();
        snapshot.occurrences().forEach(occurrence -> {
            occurrence
                    .transform()
                    .ifPresent(transform -> components.add(transform.identity().component()));
            occurrence.meshes().forEach(mesh -> components.add(mesh.identity().component()));
            occurrence
                    .directionalLight()
                    .ifPresent(light -> components.add(light.identity().component()));
        });
        return List.copyOf(components);
    }

    /** Creates a Scene with two differently configured instances beneath one local parent. */
    private static SceneDefinition scene() {
        EntityPlacement first = placement(
                FIRST_PLACEMENT,
                false,
                vectorValue("1", "2", "3"),
                "1.5",
                ResourceReference.asset("first-mesh"),
                ResourceReference.asset("first-material"));
        EntityPlacement second = placement(
                SECOND_PLACEMENT,
                true,
                vectorValue("4", "5", "6"),
                "3.5",
                ResourceReference.asset("second-mesh"),
                ResourceReference.asset("second-material"));
        LocalEntity parent = new LocalEntity(SCENE_ROOT, "Parent", true, List.of(), List.of(first, second));
        return new SceneDefinition(SCENE, "Visual Scene", List.of(parent));
    }

    /** Creates one configured placement of the generated visual definition. */
    private static EntityPlacement placement(
            EntityId id,
            boolean enabled,
            ProjectValue position,
            String intensity,
            ResourceReference mesh,
            ResourceReference material) {
        return new EntityPlacement(
                id,
                enabled,
                AssetRef.to(VISUAL_DEFINITION),
                Map.of(
                        POSITION_ARGUMENT,
                        position,
                        INTENSITY_ARGUMENT,
                        number(intensity),
                        MESH_ARGUMENT,
                        reference(mesh),
                        MATERIAL_ARGUMENT,
                        reference(material)));
    }

    /** Creates the component-bearing generated definition and its public argument contract. */
    private static EntityDefinition visualDefinition() {
        ComponentDefinition transform = component(
                TRANSFORM,
                StandardSpatial3dDescriptors.transformType(),
                Map.of(
                        StandardSpatial3dDescriptors.positionProperty(), vectorValue("0", "0", "0"),
                        StandardSpatial3dDescriptors.orientationProperty(), vectorValue("10", "20", "30"),
                        StandardSpatial3dDescriptors.scaleProperty(), vectorValue("2", "3", "4")));
        ComponentDefinition mesh = component(
                MESH,
                StandardSpatial3dDescriptors.meshRendererType(),
                Map.of(
                        StandardSpatial3dDescriptors.meshProperty(), reference(ResourceReference.asset("base-mesh")),
                        StandardSpatial3dDescriptors.materialProperty(),
                                reference(ResourceReference.asset("base-material")),
                        StandardSpatial3dDescriptors.visibleProperty(), new ProjectValue.BooleanValue(true)));
        ComponentDefinition light = component(
                LIGHT,
                StandardSpatial3dDescriptors.directionalLightType(),
                Map.of(
                        StandardSpatial3dDescriptors.colorProperty(), vectorValue("0.25", "0.5", "0.75"),
                        StandardSpatial3dDescriptors.intensityProperty(), number("1"),
                        StandardSpatial3dDescriptors.targetProperty(), vectorValue("7", "8", "9")));
        ComponentDefinition custom = component(CUSTOM, CUSTOM_TYPE, Map.of());
        LocalEntity child = new LocalEntity(DEFINITION_CHILD, "Child", true, List.of(), List.of());
        LocalEntity root = new LocalEntity(
                DEFINITION_ROOT, "Visual Root", true, List.of(transform, mesh, light, custom), List.of(child));
        EntityContract contract = new EntityContract(
                List.of(
                        new EntityContract.Parameter(
                                POSITION_ARGUMENT,
                                ProjectValueKind.ARRAY,
                                EntityContract.Requirement.REQUIRED,
                                PropertyTarget.component(
                                        DEFINITION_ROOT, TRANSFORM, StandardSpatial3dDescriptors.positionProperty())),
                        new EntityContract.Parameter(
                                INTENSITY_ARGUMENT,
                                ProjectValueKind.NUMBER,
                                EntityContract.Requirement.REQUIRED,
                                PropertyTarget.component(
                                        DEFINITION_ROOT, LIGHT, StandardSpatial3dDescriptors.intensityProperty()))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new EntityContract.ResourceBinding(
                                MESH_ARGUMENT,
                                EntityContract.Requirement.REQUIRED,
                                Set.of(ResourceReference.Kind.ASSET),
                                PropertyTarget.component(
                                        DEFINITION_ROOT, MESH, StandardSpatial3dDescriptors.meshProperty())),
                        new EntityContract.ResourceBinding(
                                MATERIAL_ARGUMENT,
                                EntityContract.Requirement.REQUIRED,
                                Set.of(ResourceReference.Kind.ASSET),
                                PropertyTarget.component(
                                        DEFINITION_ROOT, MESH, StandardSpatial3dDescriptors.materialProperty()))));
        return new EntityDefinition(VISUAL_DEFINITION, "Visual", contract, List.of(), root);
    }

    /** Creates one component definition for an exact registered type. */
    private static ComponentDefinition component(
            ComponentId id, ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(id, type.id(), type.version(), properties);
    }

    /** Creates the safe built-in and custom descriptor catalog used by planning. */
    private static RegisteredTypeCatalog types() {
        List<ExtensionDescriptor> extensions = new ArrayList<>(StandardProjectDescriptors.all());
        extensions.add(new ExtensionDescriptor(
                "example.scene-view",
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Scene View test"),
                List.of(),
                List.of(ComponentTypeDescriptor.builder(CUSTOM_TYPE, DescriptorPresentation.named("Custom"))
                        .build())));
        return RegisteredTypeCatalog.of(extensions);
    }

    /** Creates one exact three-number array value. */
    private static ProjectValue.ArrayValue vectorValue(String x, String y, String z) {
        return new ProjectValue.ArrayValue(List.of(number(x), number(y), number(z)));
    }

    /** Creates one exact number value. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }

    /** Creates one resource-reference value. */
    private static ProjectValue.ReferenceValue reference(ResourceReference reference) {
        return new ProjectValue.ReferenceValue(reference);
    }

    /** Creates one expected projected vector. */
    private static SceneViewSnapshot.Vector3 vector(String x, String y, String z) {
        return new SceneViewSnapshot.Vector3(new BigDecimal(x), new BigDecimal(y), new BigDecimal(z));
    }

    /** Complete reusable test input and its projected output. */
    private record ProjectionFixture(
            SceneViewSnapshot snapshot, CompositionPlan plan, Path source, URI generatedSource) {}
}
