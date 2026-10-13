/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionBackupResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionMutationParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.DefinitionOperationResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringBackupResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringMutationResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringPersistenceResult;
import io.github.glynch.jscene3d.editor.project.session.AuthoringRestoreResult;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.project.session.EditorRetainedDefinition;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Maps authoring operation targets, candidates, outcomes, diagnostics, and recovery payloads. */
final class AuthoringOperationProtocolMapper {
    private final DiagnosticProtocolMapper diagnostics;

    AuthoringOperationProtocolMapper(DiagnosticProtocolMapper diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    InspectorMutationTarget target(DefinitionMutationParams.MutationTarget target) {
        HierarchyOccurrenceId occurrence = new HierarchyOccurrenceId(
                AssetId.from(target.occurrence().definitionAssetId()),
                target.occurrence().entityPath().stream().map(EntityId::from).toList());
        EntityId entity = EntityId.from(target.entityId());
        return switch (target.kind()) {
            case "entity-enabled" -> new InspectorMutationTarget.EntityEnabled(occurrence, entity);
            case "component-property" ->
                new InspectorMutationTarget.ComponentProperty(
                        occurrence,
                        entity,
                        ComponentId.from(Objects.requireNonNull(target.componentId(), "componentId")),
                        new PropertyId(Objects.requireNonNull(target.propertyId(), "propertyId")));
            default -> throw new IllegalArgumentException("Unsupported mutation target kind: " + target.kind());
        };
    }

    ProjectValue value(DefinitionMutationParams.CandidateValue value) {
        return switch (value.kind()) {
            case "boolean" -> new ProjectValue.BooleanValue(Objects.requireNonNull(value.value(), "value"));
            case "integer", "number" ->
                new ProjectValue.NumberValue(new BigDecimal(Objects.requireNonNull(value.literal(), "literal")));
            case "text" -> new ProjectValue.TextValue(Objects.requireNonNull(value.literal(), "literal"));
            case "number-array" ->
                new ProjectValue.ArrayValue(Objects.requireNonNull(value.literals(), "literals").stream()
                        .map(BigDecimal::new)
                        .map(ProjectValue.NumberValue::new)
                        .map(ProjectValue.class::cast)
                        .toList());
            default -> throw new IllegalArgumentException("Unsupported mutation candidate kind: " + value.kind());
        };
    }

    DefinitionOperationResult invalidNumericLiteral(
            EditorProjectSession session,
            AssetId definition,
            InspectorMutationTarget target,
            DefinitionMutationParams.CandidateValue candidate,
            long expectedRevision,
            Locale locale) {
        URI source = session.retainDefinition(definition)
                .definition()
                .map(EditorRetainedDefinition::source)
                .orElseGet(() -> session.project().root().toUri());
        String property = target
                        instanceof
                        InspectorMutationTarget.ComponentProperty(
                                var occurrence,
                                var entity,
                                var component,
                                PropertyId propertyId)
                ? propertyId.toString()
                : "enabled";
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                PropertyValidationDiagnosticCode.KIND,
                source,
                "",
                List.of(property, candidate.kind().toUpperCase(Locale.ROOT), "invalid numeric literal"),
                Map.of("candidateKind", candidate.kind(), "technicalDetail", "numeric literal could not be parsed"));
        return result(session.rejectMutation(definition, target, expectedRevision, List.of(diagnostic)), locale);
    }

    DefinitionOperationResult result(AuthoringMutationResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                outcome(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics.map(result.diagnostics(), locale));
    }

    DefinitionOperationResult result(AuthoringPersistenceResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                outcome(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics.map(result.diagnostics(), locale));
    }

    DefinitionOperationResult result(AuthoringRestoreResult result, Locale locale) {
        return new DefinitionOperationResult(
                result.definition().toString(),
                outcome(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                diagnostics.map(result.diagnostics(), locale));
    }

    DefinitionBackupResult backup(AuthoringBackupResult result) {
        return new DefinitionBackupResult(
                result.definition().toString(),
                outcome(result.outcome()),
                result.revision(),
                result.dirty(),
                result.canUndo(),
                result.canRedo(),
                result.backup()
                        .map(backup -> Base64.getEncoder().encodeToString(backup.encode()))
                        .orElse(null));
    }

    static String outcome(Enum<?> outcome) {
        return outcome.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
