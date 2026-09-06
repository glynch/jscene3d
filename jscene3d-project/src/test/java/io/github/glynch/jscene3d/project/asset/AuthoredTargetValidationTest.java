/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises persisted authored targets and their definition-seam validation. */
final class AuthoredTargetValidationTest {
    private static final String EXTENSION_ID = "example.targets";
    private static final AssetId DEFINITION_ID = AssetId.from("588363cd-b2a9-4df8-9855-a00475092614");
    private static final AssetId WORLD_ID = AssetId.from("221b95e4-c135-48b0-a1ca-9c8397320e8d");
    private static final EntityId ROOT = EntityId.from("a12cbfc3-53ff-4936-ab98-f89d2320ef35");
    private static final EntityId CHILD = EntityId.from("62b99e3a-73c1-4ff8-91df-adc79f7f4ab6");
    private static final EntityId PLACEMENT = EntityId.from("336d45df-befb-4118-8344-c33176567079");
    private static final EntityId LOCAL_PROBE = EntityId.from("9dffccda-d17c-4602-9f0a-83b82cae0efd");
    private static final EntityId MISSING = EntityId.from("88a5eb03-d2aa-4744-828b-6ef88b4d66b2");
    private static final ComponentId BODY = ComponentId.from("d47e7893-c3ea-4d9a-b018-ae5404efad37");
    private static final ComponentId PROBE = ComponentId.from("cd0bb50b-60a3-472b-9518-378716994928");
    private static final ComponentType BODY_TYPE = ComponentType.of(EXTENSION_ID + "/body", 1);
    private static final ComponentType PROBE_TYPE = ComponentType.of(EXTENSION_ID + "/probe", 1);
    private static final PropertyId TARGET = new PropertyId("target");

    @TempDir
    private Path temporaryDirectory;

    /** Writes and reads entity and component target values without losing their identity kinds. */
    @Test
    void roundTripsStableAuthoredTargets() throws IOException {
        EntityDefinition definition = validDefinition();
        Path target = temporaryDirectory.resolve("target.entity.json");
        DefinitionWriter.write(target, definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();

        DefinitionLoadResult<EntityDefinition> result = assets.loadEntity(AssetRef.to(DEFINITION_ID), types());
        String serialized = Files.readString(target, StandardCharsets.UTF_8);

        assertThat(result.definition()).contains(definition);
        assertThat(serialized).contains("\"$target\"", "\"entityId\"", "\"componentId\"");
        assertThat(ProjectValueKind.of(new ProjectValue.EntityTargetValue(ROOT)))
                .isEqualTo(ProjectValueKind.ENTITY_TARGET);
        assertThat(ProjectValueKind.of(componentTarget(ROOT))).isEqualTo(ProjectValueKind.COMPONENT_TARGET);
    }

    /** Rejects a target whose stable entity identity is absent from the containing authored asset. */
    @Test
    void rejectsMissingComponentTarget() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("target.entity.json"), definitionTargeting(MISSING));
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();

        DefinitionLoadResult<EntityDefinition> result = assets.loadEntity(AssetRef.to(DEFINITION_ID), types());

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .contains(AssetDiagnosticCode.TARGET_INVALID);
    }

    /** Prevents an outer asset from naming a private component inside a reusable-definition placement. */
    @Test
    void rejectsComponentTargetAcrossDefinitionSeam() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("target.entity.json"), validDefinition());
        EntityPlacement placement = new EntityPlacement(PLACEMENT, true, AssetRef.to(DEFINITION_ID), Map.of());
        ComponentDefinition probe = new ComponentDefinition(
                PROBE, PROBE_TYPE.id(), PROBE_TYPE.version(), Map.of(TARGET, componentTarget(PLACEMENT)));
        LocalEntity localProbe = new LocalEntity(LOCAL_PROBE, true, List.of(probe), List.of());
        WorldDefinition world = new WorldDefinition(WORLD_ID, "Invalid seam", List.of(placement, localProbe));
        DefinitionWriter.write(temporaryDirectory.resolve("target.world.json"), world);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();

        DefinitionLoadResult<WorldDefinition> result = assets.loadWorld(AssetRef.to(WORLD_ID), types());

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code())
                .contains(AssetDiagnosticCode.TARGET_INVALID);
    }

    /** Creates a valid definition whose child targets its root entity and body component. */
    private static EntityDefinition validDefinition() {
        return definitionTargeting(ROOT);
    }

    /** Creates a definition whose probe targets the supplied entity identity. */
    private static EntityDefinition definitionTargeting(EntityId targetEntity) {
        ComponentDefinition body = new ComponentDefinition(BODY, BODY_TYPE.id(), BODY_TYPE.version(), Map.of());
        Map<PropertyId, ProjectValue> properties = Map.of(TARGET, componentTarget(targetEntity));
        ComponentDefinition probe = new ComponentDefinition(PROBE, PROBE_TYPE.id(), PROBE_TYPE.version(), properties);
        LocalEntity child = new LocalEntity(CHILD, true, List.of(probe), List.of());
        LocalEntity root = new LocalEntity(ROOT, true, List.of(body), List.of(child));
        return new EntityDefinition(DEFINITION_ID, "Authored targets", root);
    }

    /** Creates one component target property value. */
    private static ProjectValue.ComponentTargetValue componentTarget(EntityId entity) {
        return new ProjectValue.ComponentTargetValue(new ComponentTarget(entity, BODY));
    }

    /** Creates the descriptor catalog required for target-aware validation. */
    private static RegisteredTypeCatalog types() {
        PropertyDescriptor target = PropertyDescriptor.required(
                TARGET.value(),
                ProjectValueKind.COMPONENT_TARGET,
                DescriptorPresentation.named("Target"),
                Map.of(),
                Set.of());
        ComponentTypeDescriptor body = ComponentTypeDescriptor.builder(BODY_TYPE, DescriptorPresentation.named("Body"))
                .build();
        ComponentTypeDescriptor probe = ComponentTypeDescriptor.builder(
                        PROBE_TYPE, DescriptorPresentation.named("Probe"))
                .properties(List.of(target))
                .build();
        ExtensionDescriptor extension = new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("Targets"),
                List.of(),
                List.of(body, probe));
        return RegisteredTypeCatalog.of(List.of(extension));
    }
}
