/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises invariants enforced at the versioned wire-DTO boundary. */
@SuppressWarnings("NullAway") // DTO contract tests intentionally exercise nullable wire shapes.
final class ProtocolDtoValidationTest {
    /** Rejects negative major or minor protocol components independently. */
    @Test
    void rejectsNegativeProtocolVersionComponents() {
        assertThatThrownBy(() -> new ProtocolVersion(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new ProtocolVersion(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    /** Rejects blank project selections before they reach path or domain loading. */
    @Test
    void rejectsBlankProjectPath() {
        assertThatThrownBy(() -> new ProjectOpenParams(" \t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    /** Rejects non-positive expected generations and blank replacement selections. */
    @Test
    void rejectsInvalidProjectReplacementParams() {
        assertThatThrownBy(() -> new ProjectReplaceParams(0L, "/project"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new ProjectReplaceParams(1L, " \t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    /** Enforces generation, identity, and mutually exclusive result shapes for viewport launches. */
    @Test
    void validatesViewportLaunchDtos() {
        List<String> noRuntimeArtifacts = List.of();
        List<String> blankRuntimeArtifact = List.of(" ");
        List<ProjectDiagnosticDto> noDiagnostics = List.of();
        assertThatThrownBy(() -> new ViewportLaunchParams(0L, "world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new ViewportLaunchParams(1L, " \t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        ViewportLaunchSpecification launch = new ViewportLaunchSpecification(
                7L,
                "example.project",
                "Example",
                "/projects/example",
                "/projects/example/.jscene3d/published",
                "0.1.0-SNAPSHOT",
                "2f26576c-570d-4338-bc30-52bc41def3a5",
                "Opening Scene",
                List.of("/runtime/example.jar"));
        assertThatThrownBy(() -> new ViewportLaunchSpecification(
                        0L,
                        "example.project",
                        "Example",
                        "/projects/example",
                        "/projects/example/.jscene3d/published",
                        "0.1.0-SNAPSHOT",
                        "world",
                        "Opening Scene",
                        noRuntimeArtifacts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new ViewportLaunchSpecification(
                        7L,
                        "example.project",
                        "Example",
                        "/projects/example",
                        "/projects/example/.jscene3d/published",
                        "0.1.0-SNAPSHOT",
                        "world",
                        "Opening Scene",
                        blankRuntimeArtifact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeArtifacts");
        assertThatThrownBy(() -> new ViewportLaunchResult(true, null, noDiagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ViewportLaunchResult(false, launch, noDiagnostics, "rejected"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Rejects non-positive generations and blank definition asset identities. */
    @Test
    void rejectsInvalidDefinitionOpenParams() {
        assertThatThrownBy(() -> new DefinitionOpenParams(0L, "asset"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new DefinitionOpenParams(1L, " \t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    /** Rejects inconsistent success and failure result shapes. */
    @Test
    void rejectsInconsistentProjectOpenResults() {
        ProjectSummary summary = summary();
        List<ProjectDiagnosticDto> diagnostics = List.of();

        assertThatThrownBy(() -> new ProjectOpenResult(true, null, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(true, 1L, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(false, 1L, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(false, null, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Rejects unknown or structurally inconsistent replacement result outcomes. */
    @Test
    void rejectsInconsistentProjectReplaceResults() {
        ProjectSummary summary = summary();
        List<ProjectDiagnosticDto> diagnostics = List.of();

        assertThatThrownBy(() -> new ProjectReplaceResult("unknown", null, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new ProjectReplaceResult(ProjectReplaceResult.REPLACED, null, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectReplaceResult(
                        ProjectReplaceResult.CANDIDATE_REJECTED, 1L, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectReplaceResult(ProjectReplaceResult.CONFLICT, null, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Rejects either negative authored or projected asset counts. */
    @Test
    void rejectsNegativeAssetCounts() {
        assertThatThrownBy(() -> new ProjectSummary.AssetCounts(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new ProjectSummary.AssetCounts(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    /** Carries the explicit positive orderly-shutdown acknowledgement. */
    @Test
    void representsAcceptedShutdown() {
        assertThat(new ShutdownResult(true).shutdown()).isTrue();
    }

    /** Serializes exact decimal text and only the closed typed editor semantic shape. */
    @Test
    void serializesExactInspectorDecimalWithoutRawMetadata() {
        InspectorSnapshot.Value exact =
                new InspectorSnapshot.NumberValue("number", "12345678901234567890.12345678901234567890");
        InspectorSnapshot snapshot = new InspectorSnapshot(
                0,
                new DefinitionSnapshot.SemanticTarget(
                        "local-entity",
                        "file:///project/world.json",
                        "entity-a",
                        new DefinitionSnapshot.Occurrence("world-a", List.of("entity-a"))),
                "Entity",
                "authored",
                "local",
                false,
                List.of(new InspectorSnapshot.TargetGroup(
                        "entity",
                        "entity",
                        "Entity",
                        null,
                        null,
                        null,
                        "available",
                        false,
                        List.of(new InspectorSnapshot.Property(
                                "precision",
                                "Precision",
                                null,
                                "number",
                                false,
                                new InspectorSnapshot.Constraints(
                                        null,
                                        null,
                                        List.of(),
                                        new InspectorSnapshot.EditorSemantics("default", null, null)),
                                new InspectorSnapshot.PropertyState(
                                        exact, null, exact, "authored", "valid", false, false),
                                null)))));

        JsonNode json = new ObjectMapper().valueToTree(snapshot);

        assertThat(json.at("/groups/0/properties/0/state/effectiveValue/decimal")
                        .asText())
                .isEqualTo("12345678901234567890.12345678901234567890");
        assertThat(json.toString()).doesNotContain("editorMetadata", "semanticMetadata");
    }

    /** Serializes Euler semantics beside exact three-component decimal values. */
    @Test
    void serializesEulerRotationInspectorSemantics() {
        InspectorSnapshot.Value rotation = new InspectorSnapshot.ArrayValue(
                "array",
                List.of(
                        new InspectorSnapshot.NumberValue("number", "0"),
                        new InspectorSnapshot.NumberValue("number", "90.0000000000000000001"),
                        new InspectorSnapshot.NumberValue("number", "-2.5")));
        InspectorSnapshot snapshot = new InspectorSnapshot(
                0,
                new DefinitionSnapshot.SemanticTarget(
                        "local-entity",
                        "file:///project/world.json",
                        "entity-a",
                        new DefinitionSnapshot.Occurrence("world-a", List.of("entity-a"))),
                "Entity",
                "authored",
                "local",
                false,
                List.of(new InspectorSnapshot.TargetGroup(
                        "rotation",
                        "component",
                        "Rotation",
                        null,
                        "component-a",
                        new InspectorSnapshot.ComponentTypeDto("example/rotation", 1),
                        "available",
                        false,
                        List.of(new InspectorSnapshot.Property(
                                "rotation",
                                "Rotation",
                                null,
                                "array",
                                false,
                                new InspectorSnapshot.Constraints(
                                        "number",
                                        3,
                                        List.of(),
                                        new InspectorSnapshot.EditorSemantics("euler-rotation", null, null)),
                                new InspectorSnapshot.PropertyState(
                                        rotation, null, rotation, "authored", "valid", false, false),
                                null)))));

        JsonNode json = new ObjectMapper().valueToTree(snapshot);

        assertThat(json.at("/groups/0/properties/0/constraints/editor/semantic").asText())
                .isEqualTo("euler-rotation");
        assertThat(json.at("/groups/0/properties/0/state/effectiveValue/values/1/decimal")
                        .asText())
                .isEqualTo("90.0000000000000000001");
    }

    /** Preserves the complete semantic Inspector value vocabulary without losing identity data. */
    @Test
    void representsSemanticInspectorValueVocabulary() {
        InspectorSnapshot.NullValue nullValue = new InspectorSnapshot.NullValue("null");
        InspectorSnapshot.TextValue textValue = new InspectorSnapshot.TextValue("text", "display value");
        LinkedHashMap<String, InspectorSnapshot.Value> sourceValues = new LinkedHashMap<>();
        sourceValues.put("empty", nullValue);
        sourceValues.put("label", textValue);
        InspectorSnapshot.ObjectValue objectValue = new InspectorSnapshot.ObjectValue("object", sourceValues);
        InspectorSnapshot.ReferenceValue referenceValue = new InspectorSnapshot.ReferenceValue(
                "reference", "asset", "texture:wall", "Wall Texture", "resolved", "file:///project/wall.png");
        DefinitionSnapshot.Occurrence occurrence =
                new DefinitionSnapshot.Occurrence("world-a", List.of("room-a", "entity-a"));
        InspectorSnapshot.EntityTargetValue entityTarget = new InspectorSnapshot.EntityTargetValue(
                "entity-target", "entity-a", "Entity A", "resolved", occurrence);
        InspectorSnapshot.ComponentTypeDto componentType =
                new InspectorSnapshot.ComponentTypeDto("example/transform", 2);
        InspectorSnapshot.ComponentTargetValue componentTarget = new InspectorSnapshot.ComponentTargetValue(
                "component-target",
                "entity-a",
                "transform-a",
                "Entity A",
                "Transform",
                componentType,
                "resolved",
                occurrence);
        InspectorSnapshot.NumericBound minimum = new InspectorSnapshot.NumericBound("-1.25", true);

        sourceValues.put("late", textValue);

        assertThat(objectValue.values().keySet()).containsExactly("empty", "label");
        assertThat(objectValue.values()).containsEntry("empty", nullValue).containsEntry("label", textValue);
        assertThat(referenceValue)
                .extracting(
                        InspectorSnapshot.ReferenceValue::referenceKind,
                        InspectorSnapshot.ReferenceValue::locator,
                        InspectorSnapshot.ReferenceValue::resolution)
                .containsExactly("asset", "texture:wall", "resolved");
        assertThat(entityTarget.occurrence()).isEqualTo(occurrence);
        assertThat(componentTarget.componentType()).isEqualTo(componentType);
        assertThat(componentTarget.occurrence()).isEqualTo(occurrence);
        assertThat(minimum.decimal()).isEqualTo("-1.25");
        assertThat(minimum.inclusive()).isTrue();
    }

    /** Enforces the closed SET/REMOVE and exact scalar candidate transport vocabulary. */
    @Test
    void validatesDefinitionMutationParams() {
        DefinitionMutationParams.MutationTarget target = new DefinitionMutationParams.MutationTarget(
                "entity-enabled", new DefinitionSnapshot.Occurrence("world", List.of("entity")), "entity", null, null);

        assertThat(new DefinitionMutationParams.CandidateValue("number", null, "0.00000000000000000001").literal())
                .isEqualTo("0.00000000000000000001");
        assertThatThrownBy(() -> new DefinitionMutationParams(1L, "world", 0L, "reset", target, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("set or remove");
        assertThatThrownBy(() -> new DefinitionMutationParams(1L, "world", 0L, "set", target, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> new DefinitionMutationParams.CandidateValue("boolean", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boolean");
    }

    /** Rejects every invalid generation, revision, identity, and candidate variant in operation DTOs. */
    @Test
    void rejectsInvalidDefinitionOperationContracts() {
        DefinitionSnapshot.Occurrence occurrence = new DefinitionSnapshot.Occurrence("world", List.of("entity"));
        DefinitionMutationParams.MutationTarget target =
                new DefinitionMutationParams.MutationTarget("entity-enabled", occurrence, "entity", null, null);
        DefinitionMutationParams.CandidateValue text =
                new DefinitionMutationParams.CandidateValue("text", null, "value");

        assertThatThrownBy(() -> new DefinitionMutationParams(0L, "world", 0L, "set", target, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new DefinitionMutationParams(1L, " ", 0L, "set", target, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> new DefinitionMutationParams(1L, "world", -1L, "set", target, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new DefinitionMutationParams(1L, "world", 0L, "remove", target, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");

        assertThatThrownBy(() -> new DefinitionMutationParams.MutationTarget(
                        "entity-enabled", occurrence, "entity", "component", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot contain");
        assertThatThrownBy(() -> new DefinitionMutationParams.MutationTarget(
                        "component-property", occurrence, "entity", null, "property"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires");
        assertThatThrownBy(
                        () -> new DefinitionMutationParams.MutationTarget("unknown", occurrence, "entity", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");

        assertThatThrownBy(() -> new DefinitionMutationParams.CandidateValue("boolean", true, "true"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only value");
        assertThatThrownBy(() -> new DefinitionMutationParams.CandidateValue("integer", true, "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only literal");
        assertThatThrownBy(() -> new DefinitionMutationParams.CandidateValue("unknown", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");

        assertThatThrownBy(() -> new DefinitionOperationParams(0L, "world", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new DefinitionOperationParams(1L, " ", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> new DefinitionOperationParams(1L, "world", -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    /** Rejects inconsistent lifecycle results and recovery requests at the JSON boundary. */
    @Test
    void rejectsInvalidDefinitionLifecycleResults() {
        List<ProjectDiagnosticDto> noDiagnostics = List.of();

        assertThatThrownBy(() -> new DefinitionOperationResult(" ", "accepted", 0L, false, false, false, noDiagnostics))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() ->
                        new DefinitionOperationResult("world", "accepted", -1L, false, false, false, noDiagnostics))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new DefinitionBackupResult("world", "backed-up", 0L, true, false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backup");
        assertThatThrownBy(() -> new DefinitionBackupResult("world", "no-op", 0L, false, false, false, "bytes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backup");
        assertThatThrownBy(() -> new DefinitionRestoreParams(0L, "world", 0L, "bytes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new DefinitionRestoreParams(1L, " ", 0L, "bytes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> new DefinitionRestoreParams(1L, "world", -1L, "bytes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new DefinitionRestoreParams(1L, "world", 0L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    /** Creates one valid project summary for result-shape validation. */
    private static ProjectSummary summary() {
        return new ProjectSummary(
                "project",
                "Project",
                "1.0.0",
                "/project",
                "/project/project.j3d",
                new ProjectSummary.SceneSummary("world", "World"),
                new ProjectSummary.AssetCounts(0, 0));
    }
}
