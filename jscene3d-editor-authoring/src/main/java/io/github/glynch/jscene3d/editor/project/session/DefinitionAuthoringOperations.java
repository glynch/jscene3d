/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Routes authored-definition mutation, history, persistence, recovery, and interactive operations. */
final class DefinitionAuthoringOperations {
    private final String project;
    private final RetainedDefinitionRegistry definitions;
    private final MutationTargetResolver targets;

    DefinitionAuthoringOperations(
            String project, RetainedDefinitionRegistry definitions, MutationTargetResolver targets) {
        this.project = Objects.requireNonNull(project, "project");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.targets = Objects.requireNonNull(targets, "targets");
    }

    Optional<AuthoringOperation> undoOperation(AssetId id) {
        return definitions.workingCopy(id).flatMap(AuthoredDefinitionWorkingCopy::undoOperation);
    }

    AuthoringMutationResult undo(AssetId id, long expectedRevision) {
        return history(id, expectedRevision, true);
    }

    AuthoringMutationResult redo(AssetId id, long expectedRevision) {
        return history(id, expectedRevision, false);
    }

    AuthoringPersistenceResult save(AssetId id, long expectedRevision) {
        AssetId validId = Objects.requireNonNull(id, "id");
        return definitions
                .workingCopy(validId)
                .map(workingCopy -> workingCopy.save(expectedRevision))
                .orElseGet(() -> persistenceFailure(validId, expectedRevision));
    }

    AuthoringPersistenceResult revert(AssetId id, long expectedRevision) {
        AssetId validId = Objects.requireNonNull(id, "id");
        return definitions
                .workingCopy(validId)
                .map(workingCopy -> workingCopy.revert(expectedRevision))
                .orElseGet(() -> persistenceFailure(validId, expectedRevision));
    }

    AuthoringBackupResult backup(AssetId id, long expectedRevision) {
        AssetId validId = Objects.requireNonNull(id, "id");
        return definitions
                .workingCopy(validId)
                .map(workingCopy -> workingCopy.backup(project, expectedRevision))
                .orElseGet(() -> backupFailure(validId, expectedRevision));
    }

    AuthoringRestoreResult restore(AssetId id, long expectedRevision, byte[] encodedBackup) {
        AssetId validId = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(encodedBackup, "encodedBackup");
        return definitions
                .workingCopy(validId)
                .map(workingCopy -> workingCopy.restore(project, encodedBackup, expectedRevision))
                .orElseGet(() -> restoreFailure(validId, expectedRevision));
    }

    AuthoringMutationResult markPersisted(
            AssetId id, AuthoredDefinitionDocument baselinedDocument, long expectedRevision) {
        return definitions
                .workingCopy(id)
                .map(workingCopy -> workingCopy.markPersisted(baselinedDocument, expectedRevision))
                .orElseGet(() -> mutationFailure(id, expectedRevision));
    }

    AuthoringMutationResult mutate(
            AssetId definition, InspectorMutationTarget target, AuthoringMutation mutation, long expectedRevision) {
        AuthoringMutation validMutation = Objects.requireNonNull(mutation, "mutation");
        return switch (targets.resolve(definition, target, expectedRevision)) {
            case MutationTargetResolver.Resolution.Rejected rejected -> result(definition, rejected, List.of());
            case MutationTargetResolver.Resolution.Accepted accepted ->
                switch (target) {
                    case InspectorMutationTarget.EntityEnabled enabled ->
                        mutateEnabled(accepted.workingCopy(), enabled, validMutation, expectedRevision);
                    case InspectorMutationTarget.ComponentProperty property ->
                        mutateProperty(accepted.workingCopy(), property, validMutation, expectedRevision);
                };
        };
    }

    AuthoringMutationResult interact(
            AssetId definition,
            InspectorMutationTarget.ComponentProperty target,
            AuthoringInteractivePhase phase,
            String editId,
            Optional<ProjectValue> value,
            long expectedRevision) {
        AuthoringInteractivePhase validPhase = Objects.requireNonNull(phase, "phase");
        Optional<ProjectValue> validValue = Objects.requireNonNull(value, "value");
        if ((validPhase == AuthoringInteractivePhase.UPDATE) != validValue.isPresent()) {
            throw new IllegalArgumentException("interactive update requires exactly one value");
        }
        return switch (targets.resolve(definition, target, expectedRevision)) {
            case MutationTargetResolver.Resolution.Rejected rejected -> result(definition, rejected, List.of());
            case MutationTargetResolver.Resolution.Accepted accepted ->
                switch (validPhase) {
                    case BEGIN ->
                        accepted.workingCopy()
                                .beginInteractiveSet(
                                        editId,
                                        target.entity(),
                                        target.component(),
                                        target.property(),
                                        expectedRevision);
                    case UPDATE ->
                        accepted.workingCopy()
                                .previewInteractiveSet(editId, validValue.orElseThrow(), expectedRevision);
                    case COMMIT -> accepted.workingCopy().commitInteractiveSet(editId, expectedRevision);
                    case CANCEL -> accepted.workingCopy().cancelInteractiveSet(editId, expectedRevision);
                };
        };
    }

    AuthoringMutationResult reject(
            AssetId definition,
            InspectorMutationTarget target,
            long expectedRevision,
            List<ProjectDiagnostic> diagnostics) {
        List<ProjectDiagnostic> validDiagnostics = List.copyOf(diagnostics);
        if (validDiagnostics.isEmpty()) {
            throw new IllegalArgumentException("mutation rejection requires diagnostics");
        }
        return switch (targets.resolve(definition, target, expectedRevision)) {
            case MutationTargetResolver.Resolution.Rejected rejected -> result(definition, rejected, List.of());
            case MutationTargetResolver.Resolution.Accepted accepted ->
                result(accepted.workingCopy(), AuthoringMutationResult.Outcome.VALIDATION_REJECTED, validDiagnostics);
        };
    }

    private AuthoringMutationResult history(AssetId id, long expectedRevision, boolean undo) {
        AssetId validId = Objects.requireNonNull(id, "id");
        return definitions
                .workingCopy(validId)
                .map(workingCopy -> undo ? workingCopy.undo(expectedRevision) : workingCopy.redo(expectedRevision))
                .orElseGet(() -> mutationFailure(validId, expectedRevision));
    }

    private AuthoringMutationResult mutateEnabled(
            AuthoredDefinitionWorkingCopy workingCopy,
            InspectorMutationTarget.EntityEnabled target,
            AuthoringMutation mutation,
            long expectedRevision) {
        if (mutation instanceof AuthoringMutation.Remove) {
            return result(workingCopy, AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
        }
        ProjectValue value = ((AuthoringMutation.Set) mutation).value();
        if (!(value instanceof ProjectValue.BooleanValue booleanValue)) {
            ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                    ProjectDiagnostic.Severity.ERROR,
                    PropertyValidationDiagnosticCode.KIND,
                    workingCopy.current().source().toUri(),
                    "/enabled",
                    List.of("enabled", "BOOLEAN", ProjectValueKind.of(value)),
                    Map.of("technicalDetail", "enabled replacement must be a boolean value"));
            return result(workingCopy, AuthoringMutationResult.Outcome.VALIDATION_REJECTED, List.of(diagnostic));
        }
        return workingCopy.setEntityEnabled(target.entity(), booleanValue.value(), expectedRevision);
    }

    private static AuthoringMutationResult mutateProperty(
            AuthoredDefinitionWorkingCopy workingCopy,
            InspectorMutationTarget.ComponentProperty target,
            AuthoringMutation mutation,
            long expectedRevision) {
        return switch (mutation) {
            case AuthoringMutation.Set set ->
                workingCopy.set(target.entity(), target.component(), target.property(), set.value(), expectedRevision);
            case AuthoringMutation.Remove ignored ->
                workingCopy.remove(target.entity(), target.component(), target.property(), expectedRevision);
        };
    }

    private AuthoringMutationResult mutationFailure(AssetId id, long expectedRevision) {
        return new AuthoringMutationResult(
                id,
                targets.unavailableOutcome(id),
                targets.currentRevision(id, expectedRevision),
                false,
                false,
                false,
                List.of());
    }

    private AuthoringPersistenceResult persistenceFailure(AssetId id, long expectedRevision) {
        return new AuthoringPersistenceResult(
                id,
                definitions.isAuthored(id)
                        ? AuthoringPersistenceResult.Outcome.INVALID_TARGET
                        : definitions.retainedKind(id).isPresent()
                                ? AuthoringPersistenceResult.Outcome.NON_EDITABLE
                                : AuthoringPersistenceResult.Outcome.INVALID_TARGET,
                targets.currentRevision(id, expectedRevision),
                false,
                false,
                false,
                List.of());
    }

    private AuthoringBackupResult backupFailure(AssetId id, long expectedRevision) {
        AuthoringBackupResult.Outcome outcome = definitions.retainedKind(id).isPresent()
                ? AuthoringBackupResult.Outcome.NON_EDITABLE
                : AuthoringBackupResult.Outcome.INVALID_TARGET;
        return new AuthoringBackupResult(
                id, outcome, targets.currentRevision(id, expectedRevision), false, false, false, Optional.empty());
    }

    private AuthoringRestoreResult restoreFailure(AssetId id, long expectedRevision) {
        AuthoringRestoreResult.Outcome outcome = definitions.retainedKind(id).isPresent()
                ? AuthoringRestoreResult.Outcome.NON_EDITABLE
                : AuthoringRestoreResult.Outcome.INVALID_TARGET;
        return new AuthoringRestoreResult(
                id, outcome, targets.currentRevision(id, expectedRevision), false, false, false, List.of());
    }

    private static AuthoringMutationResult result(
            AssetId definition,
            MutationTargetResolver.Resolution.Rejected rejected,
            List<ProjectDiagnostic> diagnostics) {
        if (rejected.workingCopy().isPresent()) {
            return result(rejected.workingCopy().orElseThrow(), rejected.outcome(), diagnostics);
        }
        return new AuthoringMutationResult(
                definition, rejected.outcome(), rejected.revision(), false, false, false, diagnostics);
    }

    private static AuthoringMutationResult result(
            AuthoredDefinitionWorkingCopy workingCopy,
            AuthoringMutationResult.Outcome outcome,
            List<ProjectDiagnostic> diagnostics) {
        AuthoringDefinitionState state = workingCopy.state();
        return new AuthoringMutationResult(
                state.definition(),
                outcome,
                state.revision(),
                state.dirty(),
                state.canUndo(),
                state.canRedo(),
                diagnostics);
    }
}
