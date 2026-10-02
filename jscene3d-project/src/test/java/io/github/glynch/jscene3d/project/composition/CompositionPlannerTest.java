/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
import io.github.glynch.jscene3d.project.contract.EntityContract;
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
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises safe composition semantics through the public project-module interface. */
final class CompositionPlannerTest {
    private static final String EXTENSION_ID = "example.composition";
    private static final AssetId WORLD_ID = AssetId.from("aa8e0814-bb16-4683-b924-dacc0e660f75");
    private static final AssetId OUTER_ID = AssetId.from("5f4502e3-65d4-4f42-a305-3b52ab31b771");
    private static final AssetId INNER_ID = AssetId.from("836eb20c-7f17-422a-bf8c-c559df41bc79");
    private static final EntityId OUTER_ROOT = EntityId.from("a3833a61-1715-4c4b-a19a-4cdbafcb490b");
    private static final EntityId INNER_ROOT = EntityId.from("c66433c3-226f-401d-87d5-b9a1b4eeb087");
    private static final EntityId INNER_CHILD = EntityId.from("29899aca-94b4-494d-87d0-1b91bf7a6b80");
    private static final EntityId INNER_PLACEMENT = EntityId.from("1cb6ae20-e5ff-4dc0-a647-58e5ef9efe43");
    private static final EntityId FIRST_PLACEMENT = EntityId.from("69d00d22-b3f8-4d7f-a3b9-1df6281e22b7");
    private static final EntityId SECOND_PLACEMENT = EntityId.from("26f759bb-0884-4135-a7b4-4c02f67156ef");
    private static final ComponentId COMPONENT_ID = ComponentId.from("cab117c7-e73f-4b6f-a66d-3b14fcdbb92f");
    private static final ComponentType COMPONENT_TYPE = ComponentType.of(EXTENSION_ID + "/value", 1);
    private static final PropertyId VALUE = new PropertyId("value");
    private static final PropertyId FORWARDED_VALUE = new PropertyId("forwarded-value");
    private static final PropertyId RESOURCE = new PropertyId("resource");
    private static final PropertyId TARGET = new PropertyId("target");
    private static final PropertyId DEFAULTED = new PropertyId("defaulted");
    private static final ResourceReference FIRST_RESOURCE = ResourceReference.asset("first-resource");
    private static final ResourceReference SECOND_RESOURCE = ResourceReference.asset("second-resource");

    @TempDir
    private Path temporaryDirectory;

    /** Expands nested repeated placements without wrappers and preserves scoped effective values. */
    @Test
    void plansNestedRepeatedPlacementsAndEffectiveProperties() throws IOException {
        EntityDefinition inner = innerDefinition();
        EntityDefinition outer = outerDefinition();
        DefinitionWriter.write(temporaryDirectory.resolve("inner.entity.json"), inner);
        DefinitionWriter.write(temporaryDirectory.resolve("outer.entity.json"), outer);
        DefinitionResolver resolver =
                AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        WorldDefinition world = worldDefinition();

        CompositionPlanResult result = CompositionPlanner.plan(
                temporaryDirectory.resolve("working.world.json").toUri(), world, resolver, types());

        assertThat(result.diagnostics()).isEmpty();
        CompositionPlan plan = result.plan().orElseThrow();
        assertThat(plan.entities())
                .extracting(
                        CompositionEntity::authoredId,
                        entity -> entity.occurrence().entityPath(),
                        CompositionEntity::locallyEnabled,
                        CompositionEntity::effectivelyEnabled)
                .containsExactly(
                        tuple(FIRST_PLACEMENT, List.of(FIRST_PLACEMENT), false, false),
                        tuple(INNER_PLACEMENT, List.of(FIRST_PLACEMENT, INNER_PLACEMENT), true, false),
                        tuple(INNER_CHILD, List.of(FIRST_PLACEMENT, INNER_PLACEMENT, INNER_CHILD), true, false),
                        tuple(SECOND_PLACEMENT, List.of(SECOND_PLACEMENT), true, true),
                        tuple(INNER_PLACEMENT, List.of(SECOND_PLACEMENT, INNER_PLACEMENT), true, true),
                        tuple(INNER_CHILD, List.of(SECOND_PLACEMENT, INNER_PLACEMENT, INNER_CHILD), true, true));
        assertThat(plan.entities()).noneMatch(entity -> entity.authoredId().equals(OUTER_ROOT));
        List<CompositionComponent> components = plan.components();
        assertThat(components).hasSize(2);
        assertThat(components)
                .extracting(
                        component -> number(component.values().get(VALUE)),
                        component -> reference(component.values().get(RESOURCE)),
                        component -> number(component.values().get(DEFAULTED)))
                .containsExactly(
                        tuple(new BigDecimal("2"), FIRST_RESOURCE, new BigDecimal("9")),
                        tuple(new BigDecimal("3"), SECOND_RESOURCE, new BigDecimal("9")));
        assertThat(components.get(0).effectiveProperties().get(TARGET).authoredScope())
                .isNotEqualTo(
                        components.get(1).effectiveProperties().get(TARGET).authoredScope());
        assertThat(components
                        .get(0)
                        .effectiveProperties()
                        .get(TARGET)
                        .authoredScope()
                        .anchor())
                .isEqualTo(new CompositionOccurrenceId(WORLD_ID, List.of(FIRST_PLACEMENT, INNER_PLACEMENT)));
        assertThat(components.get(0).resourceReferences()).containsEntry(RESOURCE, FIRST_RESOURCE);
    }

    /** Uses the caller-owned in-memory root rather than reloading the saved world revision. */
    @Test
    void treatsTheInMemoryRootAsAuthoritative() throws IOException {
        WorldDefinition saved = new WorldDefinition(
                WORLD_ID, "Saved", List.of(new LocalEntity(FIRST_PLACEMENT, "Saved root", true, List.of(), List.of())));
        DefinitionWriter.write(temporaryDirectory.resolve("saved.world.json"), saved);
        AssetCatalog catalog = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        WorldDefinition edited = new WorldDefinition(
                WORLD_ID,
                "Edited",
                List.of(new LocalEntity(SECOND_PLACEMENT, "Unsaved root", true, List.of(), List.of())));

        CompositionPlan plan = CompositionPlanner.plan(
                        temporaryDirectory.resolve("saved.world.json").toUri(), edited, catalog, types())
                .plan()
                .orElseThrow();

        assertThat(plan.entities())
                .singleElement()
                .extracting(CompositionEntity::authoredId)
                .isEqualTo(SECOND_PLACEMENT);
    }

    /** Resolves already-published generated definitions through the same safe planner. */
    @Test
    void plansGeneratedDefinitionsThroughTheResolver() throws IOException {
        WorldDefinition world = new WorldDefinition(
                WORLD_ID,
                "Generated",
                List.of(new EntityPlacement(
                        FIRST_PLACEMENT,
                        true,
                        AssetRef.to(INNER_ID),
                        Map.of(
                                VALUE, number(4),
                                RESOURCE, reference(FIRST_RESOURCE)))));
        DefinitionWriter.write(temporaryDirectory.resolve("generated.world.json"), world);
        AssetCatalog authored = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        ByteArrayOutputStream generated = new ByteArrayOutputStream();
        DefinitionWriter.write(generated, innerDefinition());
        URI generatedSource = URI.create("import:composition/inner.entity.json");
        DefinitionResolver resolver = DefinitionResolvers.builder(authored)
                .addGeneratedEntity(INNER_ID, generatedSource, new ByteArrayInputStream(generated.toByteArray()))
                .build();

        CompositionPlan plan = CompositionPlanner.plan(
                        temporaryDirectory.resolve("generated.world.json").toUri(), world, resolver, types())
                .plan()
                .orElseThrow();

        assertThat(plan.definitionSources()).containsEntry(INNER_ID, generatedSource);
        assertThat(plan.entities().getFirst().authoredSource())
                .isEqualTo(temporaryDirectory.resolve("generated.world.json").toUri());
        assertThat(plan.components().getFirst().authoredSource()).isEqualTo(generatedSource);
    }

    /** Reports invalid preparation and instance arguments without runtime exceptions or factories. */
    @Test
    void reportsStructuredArgumentDiagnosticsAndReplansPreparedGraphs() throws IOException {
        EntityDefinition inner = innerDefinition();
        DefinitionWriter.write(temporaryDirectory.resolve("inner.entity.json"), inner);
        DefinitionResolver resolver =
                AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();

        CompositionPlanResult invalid = CompositionPlanner.prepare(
                temporaryDirectory.resolve("inner.entity.json").toUri(),
                inner,
                resolver,
                types(),
                Map.of(RESOURCE, number(1)));
        CompositionPlan prepared = CompositionPlanner.prepare(
                        temporaryDirectory.resolve("inner.entity.json").toUri(),
                        inner,
                        resolver,
                        types(),
                        Map.of(RESOURCE, reference(FIRST_RESOURCE)))
                .plan()
                .orElseThrow();
        CompositionPlanResult missingParameter = CompositionPlanner.instantiate(
                prepared, types(), Map.of(), Map.of(RESOURCE, reference(FIRST_RESOURCE)));
        CompositionPlan instance = CompositionPlanner.instantiate(
                        prepared, types(), Map.of(VALUE, number(7)), Map.of(RESOURCE, reference(FIRST_RESOURCE)))
                .plan()
                .orElseThrow();

        assertThat(invalid.plan()).isEmpty();
        assertThat(invalid.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(CompositionDiagnosticCode.ARGUMENT_INVALID);
        assertThat(missingParameter.plan()).isEmpty();
        assertThat(missingParameter.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code())
                .isEqualTo(CompositionDiagnosticCode.ARGUMENT_INVALID);
        assertThat(number(instance.components().getFirst().values().get(VALUE))).isEqualByComparingTo("7");
        assertThat(getClass().getModule().getDescriptor().requires())
                .noneMatch(require -> require.name().contains("project.runtime"));
    }

    /** Creates a world with two independently configured instances of one nested reusable definition. */
    private static WorldDefinition worldDefinition() {
        return new WorldDefinition(
                WORLD_ID,
                "World working copy",
                List.of(
                        new EntityPlacement(
                                FIRST_PLACEMENT,
                                "First",
                                false,
                                AssetRef.to(OUTER_ID),
                                Map.of(FORWARDED_VALUE, number(2), RESOURCE, reference(FIRST_RESOURCE))),
                        new EntityPlacement(
                                SECOND_PLACEMENT,
                                true,
                                AssetRef.to(OUTER_ID),
                                Map.of(FORWARDED_VALUE, number(3), RESOURCE, reference(SECOND_RESOURCE)))));
    }

    /** Creates a reusable definition which forwards its public contract into one nested placement. */
    private static EntityDefinition outerDefinition() {
        EntityPlacement nested = new EntityPlacement(
                INNER_PLACEMENT,
                true,
                AssetRef.to(INNER_ID),
                Map.of(VALUE, number(1), RESOURCE, reference(FIRST_RESOURCE)));
        LocalEntity root = new LocalEntity(OUTER_ROOT, "Outer root", true, List.of(), List.of(nested));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        FORWARDED_VALUE,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.placement(INNER_PLACEMENT, VALUE))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityContract.ResourceBinding(
                        RESOURCE,
                        EntityContract.Requirement.REQUIRED,
                        Set.of(ResourceReference.Kind.ASSET),
                        PropertyTarget.placement(INNER_PLACEMENT, RESOURCE))));
        return new EntityDefinition(OUTER_ID, "Outer", contract, List.of(), root);
    }

    /** Creates the component-bearing reusable leaf definition. */
    private static EntityDefinition innerDefinition() {
        ComponentDefinition component = new ComponentDefinition(
                COMPONENT_ID,
                COMPONENT_TYPE.id(),
                COMPONENT_TYPE.version(),
                Map.of(
                        VALUE,
                        number(5),
                        RESOURCE,
                        reference(FIRST_RESOURCE),
                        TARGET,
                        new ProjectValue.EntityTargetValue(INNER_ROOT)));
        LocalEntity child = new LocalEntity(INNER_CHILD, "Inner child", true, List.of(), List.of());
        LocalEntity root = new LocalEntity(INNER_ROOT, "Inner root", true, List.of(component), List.of(child));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        VALUE,
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.REQUIRED,
                        PropertyTarget.component(INNER_ROOT, COMPONENT_ID, VALUE))),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityContract.ResourceBinding(
                        RESOURCE,
                        EntityContract.Requirement.REQUIRED,
                        Set.of(ResourceReference.Kind.ASSET),
                        PropertyTarget.component(INNER_ROOT, COMPONENT_ID, RESOURCE))));
        return new EntityDefinition(INNER_ID, "Inner", contract, List.of(), root);
    }

    /** Creates safe inert metadata for the fixture component. */
    private static RegisteredTypeCatalog types() {
        ComponentTypeDescriptor descriptor = ComponentTypeDescriptor.builder(
                        COMPONENT_TYPE, DescriptorPresentation.named("Value"))
                .properties(List.of(
                        PropertyDescriptor.required(
                                VALUE.value(),
                                ProjectValueKind.NUMBER,
                                DescriptorPresentation.named("Value"),
                                Map.of(),
                                Set.of()),
                        PropertyDescriptor.required(
                                RESOURCE.value(),
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.named("Resource"),
                                Map.of(),
                                Set.of(ResourceReference.Kind.ASSET)),
                        PropertyDescriptor.required(
                                TARGET.value(),
                                ProjectValueKind.ENTITY_TARGET,
                                DescriptorPresentation.named("Target"),
                                Map.of(),
                                Set.of()),
                        PropertyDescriptor.optionalWithDefault(
                                DEFAULTED.value(),
                                ProjectValueKind.NUMBER,
                                number(9),
                                DescriptorPresentation.named("Defaulted"),
                                Map.of(),
                                Set.of())))
                .build();
        return RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Composition"),
                List.of(),
                List.of(descriptor))));
    }

    /** Creates one number value. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Reads one number value. */
    private static BigDecimal number(ProjectValue value) {
        return ((ProjectValue.NumberValue) value).value();
    }

    /** Creates one resource-reference value. */
    private static ProjectValue.ReferenceValue reference(ResourceReference reference) {
        return new ProjectValue.ReferenceValue(reference);
    }

    /** Reads one resource-reference value. */
    private static ResourceReference reference(ProjectValue value) {
        return ((ProjectValue.ReferenceValue) value).reference();
    }
}
