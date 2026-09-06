/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.AttachmentPointId;
import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.contract.EntityContract;
import io.github.glynch.jscene3d.project.entity.EndpointTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.entity.PropertyTarget;
import io.github.glynch.jscene3d.project.entity.SignalConnection;
import io.github.glynch.jscene3d.project.entity.SpatialTarget;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises catalog-aware validation through the public asset loading interface. */
final class ComponentDefinitionValidationTest {
    private static final AssetId ASSET_ID = AssetId.from("a5ab4f15-7953-4e08-861d-4aa61d6ee6be");
    private static final EntityId ROOT = EntityId.from("78968894-f873-4cb5-b496-aba59f57d61c");
    private static final ComponentId TRANSFORM = ComponentId.from("05fa9e21-3033-4242-872b-50b9780dbade");
    private static final ComponentId MOVER = ComponentId.from("3709b66b-d01a-4ec5-b390-ecb08b69d668");
    private static final ComponentTypeId TRANSFORM_TYPE = new ComponentTypeId("example.game/transform-3d");
    private static final ComponentTypeId MOVER_TYPE = new ComponentTypeId("example.game/mover");
    private static final CapabilityId TRANSFORM_CAPABILITY = new CapabilityId("example.game/transform");

    @TempDir
    private Path temporaryDirectory;

    /** Accepts a component graph whose descriptors, contract, dependencies, and endpoints all agree. */
    @Test
    void loadsDescriptorValidDefinition() throws IOException {
        EntityDefinition definition = validDefinition();

        DefinitionLoadResult<EntityDefinition> result = load(definition, transformDescriptor(), moverDescriptor());

        assertThat(result.isValid()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.definition()).contains(definition);
    }

    /** Collects property, multiplicity, conflict, and capability failures before construction. */
    @Test
    void rejectsInvalidComponentComposition() throws IOException {
        ComponentDefinition firstTransform = component(TRANSFORM, TRANSFORM_TYPE, Map.of());
        ComponentDefinition secondTransform =
                component(ComponentId.from("b211e965-ff76-43b3-8b52-bfece775af8b"), TRANSFORM_TYPE, Map.of());
        ComponentDefinition mover = component(
                MOVER,
                MOVER_TYPE,
                Map.of(
                        new PropertyId("speed"), new ProjectValue.TextValue("fast"),
                        new PropertyId("mystery"), new ProjectValue.BooleanValue(true)));
        ComponentDefinition teleporter = component(
                ComponentId.from("88d1888e-51cd-4ca3-90aa-89e62c863f52"),
                new ComponentTypeId("example.game/teleporter"),
                Map.of());
        LocalEntity root =
                new LocalEntity(ROOT, true, List.of(firstTransform, secondTransform, mover, teleporter), List.of());
        EntityDefinition definition = new EntityDefinition(ASSET_ID, "Invalid", root);

        DefinitionLoadResult<EntityDefinition> result =
                load(definition, transformDescriptor(), moverDescriptor(), teleporterDescriptor(), orphanDescriptor());

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "asset.component.catalog.property.value",
                        "asset.component.catalog.property.unknown",
                        "asset.component.catalog.multiplicity",
                        "asset.component.catalog.conflict",
                        "asset.component.catalog.capability.ambiguous");
    }

    /** Rejects multiple components claiming primary spatial authority even across different domains. */
    @Test
    void rejectsMultiplePrimarySpatialComponents() throws IOException {
        ComponentDefinition transform3d = component(TRANSFORM, TRANSFORM_TYPE, Map.of());
        ComponentDefinition transform2d = component(
                ComponentId.from("4516a7de-fcde-470a-97c5-b107db5ce0d1"),
                new ComponentTypeId("example.game/transform-2d"),
                Map.of());
        LocalEntity root = new LocalEntity(ROOT, true, List.of(transform3d, transform2d), List.of());
        EntityDefinition definition = new EntityDefinition(ASSET_ID, "Ambiguous spatial entity", root);

        DefinitionLoadResult<EntityDefinition> result =
                load(definition, transformDescriptor(), transform2dDescriptor());

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(AssetDiagnosticCode.COMPONENT_SPATIAL_DOMAIN_AMBIGUOUS);
            assertThat(diagnostic.location()).isEqualTo("/root/components");
        });
    }

    /** Rejects public and connected endpoints that disagree with their private component declarations. */
    @Test
    void rejectsInvalidLocalContractTargets() throws IOException {
        ComponentDefinition transform = component(TRANSFORM, TRANSFORM_TYPE, Map.of());
        ComponentDefinition mover = component(
                MOVER, MOVER_TYPE, Map.of(new PropertyId("speed"), new ProjectValue.NumberValue(BigDecimal.ONE)));
        LocalEntity root = new LocalEntity(ROOT, true, List.of(transform, mover), List.of());
        EndpointTarget moved = EndpointTarget.component(ROOT, MOVER, new EndpointId("moved"));
        EndpointTarget missing = EndpointTarget.component(ROOT, MOVER, new EndpointId("missing"));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        new PropertyId("speed"),
                        ProjectValueKind.TEXT,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(ROOT, MOVER, new PropertyId("speed")))),
                List.of(new EntityContract.Signal(new EndpointId("moved"), moved)),
                List.of(),
                List.of(),
                List.of(new EntityContract.Attachment(
                        new AttachmentPointId("weapon"),
                        SpatialTarget.componentAttachment(ROOT, TRANSFORM, new AttachmentPointId("missing")))),
                List.of());
        EntityDefinition definition = new EntityDefinition(
                ASSET_ID, "Invalid contract", contract, List.of(new SignalConnection(moved, missing)), root);

        DefinitionLoadResult<EntityDefinition> result = load(definition, transformDescriptor(), moverDescriptor());

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "asset.contract.payload",
                        "asset.component.catalog.endpoint.missing",
                        "asset.component.catalog.attachment.missing");
    }

    /** Writes, scans, and catalog-validates one definition through supported interfaces. */
    private DefinitionLoadResult<EntityDefinition> load(
            EntityDefinition definition, ComponentTypeDescriptor... descriptors) throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("subject.entity.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(new ExtensionDescriptor(
                "example.game",
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Example Game"),
                List.of(),
                List.of(descriptors))));
        return assets.loadEntity(AssetRef.to(definition.id()), types);
    }

    /** Creates one authored component with version one. */
    private static ComponentDefinition component(
            ComponentId id, ComponentTypeId type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(id, type, 1, properties);
    }

    /** Creates the valid definition shared by the positive case. */
    private static EntityDefinition validDefinition() {
        ComponentDefinition transform = component(TRANSFORM, TRANSFORM_TYPE, Map.of());
        ComponentDefinition mover = component(
                MOVER, MOVER_TYPE, Map.of(new PropertyId("speed"), new ProjectValue.NumberValue(BigDecimal.ONE)));
        LocalEntity root = new LocalEntity(ROOT, true, List.of(transform, mover), List.of());
        EndpointTarget moved = EndpointTarget.component(ROOT, MOVER, new EndpointId("moved"));
        EndpointTarget stop = EndpointTarget.component(ROOT, MOVER, new EndpointId("stop"));
        EntityContract contract = new EntityContract(
                List.of(new EntityContract.Parameter(
                        new PropertyId("speed"),
                        ProjectValueKind.NUMBER,
                        EntityContract.Requirement.OPTIONAL,
                        PropertyTarget.component(ROOT, MOVER, new PropertyId("speed")))),
                List.of(new EntityContract.Signal(new EndpointId("moved"), moved)),
                List.of(new EntityContract.Action(new EndpointId("stop"), stop)),
                List.of(),
                List.of(new EntityContract.Attachment(
                        new AttachmentPointId("origin"), SpatialTarget.component(ROOT, TRANSFORM))),
                List.of());
        return new EntityDefinition(ASSET_ID, "Valid", contract, List.of(new SignalConnection(moved, stop)), root);
    }

    /** Declares a single three-dimensional transform provider. */
    private static ComponentTypeDescriptor transformDescriptor() {
        return ComponentTypeDescriptor.builder(
                        new ComponentType(TRANSFORM_TYPE, 1), DescriptorPresentation.named("Transform 3d"))
                .providedCapabilities(Set.of(TRANSFORM_CAPABILITY))
                .spatialDomain(ComponentSpatialDomain.THREE_DIMENSIONAL)
                .attachments(Set.of(new AttachmentPointId("socket")))
                .build();
    }

    /** Declares a second primary spatial component in another domain for ambiguity validation. */
    private static ComponentTypeDescriptor transform2dDescriptor() {
        return ComponentTypeDescriptor.builder(
                        ComponentType.of("example.game/transform-2d", 1), DescriptorPresentation.named("Transform 2d"))
                .spatialDomain(ComponentSpatialDomain.TWO_DIMENSIONAL)
                .build();
    }

    /** Declares behavior requiring exactly one sibling transform provider. */
    private static ComponentTypeDescriptor moverDescriptor() {
        PropertyDescriptor speed = PropertyDescriptor.required(
                "speed", ProjectValueKind.NUMBER, DescriptorPresentation.named("Speed"), Map.of(), Set.of());
        return ComponentTypeDescriptor.builder(new ComponentType(MOVER_TYPE, 1), DescriptorPresentation.named("Mover"))
                .properties(List.of(speed))
                .signals(List.of(EndpointDescriptor.withoutPayload("moved", DescriptorPresentation.named("Moved"))))
                .actions(List.of(EndpointDescriptor.withoutPayload("stop", DescriptorPresentation.named("Stop"))))
                .requiredCapabilities(Set.of(TRANSFORM_CAPABILITY))
                .conflicts(Set.of(new ComponentTypeId("example.game/teleporter")))
                .build();
    }

    /** Declares a component that conflicts with movement behavior. */
    private static ComponentTypeDescriptor teleporterDescriptor() {
        return ComponentTypeDescriptor.builder(
                        ComponentType.of("example.game/teleporter", 1), DescriptorPresentation.named("Teleporter"))
                .build();
    }

    /** Declares an unused component with a deliberately unsatisfied capability requirement. */
    private static ComponentTypeDescriptor orphanDescriptor() {
        return ComponentTypeDescriptor.builder(
                        ComponentType.of("example.game/orphan", 1), DescriptorPresentation.named("Orphan"))
                .requiredCapabilities(Set.of(new CapabilityId("example.game/missing")))
                .multiplicity(ComponentMultiplicity.MULTIPLE)
                .build();
    }
}
