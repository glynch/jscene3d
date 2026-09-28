/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.session.internal.AuthoringChangeSource;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionBackup;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Internal authoritative working copy shared by authored world and entity definitions.
 *
 * <p>The current document, persisted semantic baseline, monotonic revision, and validated document-state history are
 * owned together. Dirty state is always derived by comparing current and persisted authored trees. Marking a state
 * persisted performs no I/O and preserves history.
 */
final class AuthoredDefinitionWorkingCopy implements AutoCloseable {
    private final Deque<Transaction> undoHistory = new ArrayDeque<>();
    private final Deque<Transaction> redoHistory = new ArrayDeque<>();
    private final AuthoringChangeSource<AuthoringDefinitionChange> changes = new AuthoringChangeSource<>();

    private AuthoredDefinitionDocument current;
    private AuthoredDefinitionDocument persisted;
    private long revision;
    private boolean closed;

    /** Creates one clean working copy from a validated persisted document. */
    AuthoredDefinitionWorkingCopy(AuthoredDefinitionDocument document) {
        current = Objects.requireNonNull(document, "document");
        persisted = current;
    }

    /** Returns the current source-preserving authored document. */
    AuthoredDefinitionDocument current() {
        ensureOpen();
        return current;
    }

    /** Returns the current immutable lifecycle and domain state. */
    AuthoringDefinitionState state() {
        ensureOpen();
        return new AuthoringDefinitionState(
                current.id(), current.kind(), revision, isDirty(), canUndo(), canRedo(), content(current));
    }

    /** Returns whether current authored state differs semantically from the persisted authored state. */
    boolean isDirty() {
        ensureOpen();
        return !current.hasSameAuthoredState(persisted);
    }

    /** Returns whether an earlier accepted transaction is available. */
    boolean canUndo() {
        ensureOpen();
        return !undoHistory.isEmpty();
    }

    /** Returns whether a previously undone transaction is available. */
    boolean canRedo() {
        ensureOpen();
        return !redoHistory.isEmpty();
    }

    /** Returns the next undoable operation metadata. */
    Optional<AuthoringOperation> undoOperation() {
        ensureOpen();
        return Optional.ofNullable(undoHistory.peek()).map(Transaction::operation);
    }

    /** Returns the semantic state-change stream. */
    AuthoringChange<AuthoringDefinitionChange> onDidChange() {
        ensureOpen();
        return changes;
    }

    /** Applies one source-preserving component-property SET transaction. */
    AuthoringMutationResult set(
            EntityId entity, ComponentId component, PropertyId property, ProjectValue value, long expectedRevision) {
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(entity, component, property);
        return mutate(
                expectedRevision,
                new AuthoringOperation(
                        AuthoringText.message("editor.operation.set-component-property", "Set component property"),
                        current.source()),
                AuthoringDefinitionChange.Kind.SET,
                document -> document.set(target, value));
    }

    /** Applies one source-preserving component-property REMOVE transaction. */
    AuthoringMutationResult remove(EntityId entity, ComponentId component, PropertyId property, long expectedRevision) {
        AuthoredDefinitionDocument.ComponentPropertyTarget target =
                new AuthoredDefinitionDocument.ComponentPropertyTarget(entity, component, property);
        return mutate(
                expectedRevision,
                new AuthoringOperation(
                        AuthoringText.message(
                                "editor.operation.remove-component-property", "Remove component property"),
                        current.source()),
                AuthoringDefinitionChange.Kind.REMOVE,
                document -> document.remove(target));
    }

    /** Applies the existing source-preserving entity-enabled SET transaction. */
    AuthoringMutationResult setEntityEnabled(EntityId entity, boolean enabled, long expectedRevision) {
        return mutate(
                expectedRevision,
                new AuthoringOperation(
                        AuthoringText.message("editor.operation.set-entity-enabled", "Set entity enabled"),
                        current.source()),
                AuthoringDefinitionChange.Kind.SET,
                document -> document.setEntityEnabled(entity, enabled));
    }

    /** Restores the previous accepted document state under optimistic revision checking. */
    AuthoringMutationResult undo(long expectedRevision) {
        ensureOpen();
        Optional<AuthoringMutationResult> stale = stale(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        Transaction transaction = undoHistory.poll();
        if (transaction == null) {
            return result(AuthoringMutationResult.Outcome.UNDO_UNAVAILABLE, List.of());
        }
        current = transaction.before().withPersistedSourceBaseline(persisted);
        redoHistory.push(transaction);
        revision++;
        emit(AuthoringDefinitionChange.Kind.UNDO);
        return result(AuthoringMutationResult.Outcome.ACCEPTED, List.of());
    }

    /** Restores the next previously undone document state under optimistic revision checking. */
    AuthoringMutationResult redo(long expectedRevision) {
        ensureOpen();
        Optional<AuthoringMutationResult> stale = stale(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        Transaction transaction = redoHistory.poll();
        if (transaction == null) {
            return result(AuthoringMutationResult.Outcome.REDO_UNAVAILABLE, List.of());
        }
        current = transaction.after().withPersistedSourceBaseline(persisted);
        undoHistory.push(transaction);
        revision++;
        emit(AuthoringDefinitionChange.Kind.REDO);
        return result(AuthoringMutationResult.Outcome.ACCEPTED, List.of());
    }

    /**
     * Establishes the current authored state as persisted without writing it.
     *
     * <p>The supplied document carries the persisted-source fingerprint established by a future successful save.
     * History remains available and the content revision does not change.
     */
    AuthoringMutationResult markPersisted(AuthoredDefinitionDocument baselinedDocument, long expectedRevision) {
        ensureOpen();
        Optional<AuthoringMutationResult> stale = stale(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        AuthoredDefinitionDocument baseline = Objects.requireNonNull(baselinedDocument, "baselinedDocument");
        if (!current.hasSameAuthoredState(baseline)) {
            throw new IllegalArgumentException("persisted document must match current authored state");
        }
        boolean changed = isDirty() || !current.sourceFingerprint().equals(baseline.sourceFingerprint());
        if (!changed) {
            return result(AuthoringMutationResult.Outcome.NO_OP, List.of());
        }
        current = current.withPersistedSourceBaseline(baseline);
        persisted = baseline;
        emit(AuthoringDefinitionChange.Kind.PERSISTED);
        return result(AuthoringMutationResult.Outcome.ACCEPTED, List.of());
    }

    /** Persists current authored state without changing content revision or edit history. */
    AuthoringPersistenceResult save(long expectedRevision) {
        ensureOpen();
        Optional<AuthoringPersistenceResult> stale = stalePersistence(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        if (!isDirty()) {
            AuthoredDefinitionDocument.SourceStatus status = current.verifySource();
            AuthoringPersistenceResult.Outcome outcome = status == AuthoredDefinitionDocument.SourceStatus.MATCH
                    ? AuthoringPersistenceResult.Outcome.NO_OP
                    : sourceOutcome(status);
            return persistenceResult(outcome, List.of());
        }
        return switch (current.save()) {
            case AuthoredDefinitionDocument.SaveResult.Saved saved -> acceptSave(saved.document());
            case AuthoredDefinitionDocument.SaveResult.SourceFailure failure ->
                persistenceResult(sourceOutcome(failure.status()), List.of(failure.diagnostic()));
            case AuthoredDefinitionDocument.SaveResult.WriteFailure failure ->
                persistenceResult(AuthoringPersistenceResult.Outcome.WRITE_FAILED, List.of(failure.diagnostic()));
        };
    }

    /** Reloads and validates physical authored source, accepting legitimate external changes. */
    AuthoringPersistenceResult revert(long expectedRevision) {
        ensureOpen();
        Optional<AuthoringPersistenceResult> stale = stalePersistence(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        return switch (current.reload()) {
            case AuthoredDefinitionDocument.ReloadResult.Loaded loaded -> acceptReload(loaded.document());
            case AuthoredDefinitionDocument.ReloadResult.SourceFailure failure ->
                persistenceResult(sourceOutcome(failure.status()), List.of(failure.diagnostic()));
            case AuthoredDefinitionDocument.ReloadResult.Rejected rejected ->
                persistenceResult(AuthoringPersistenceResult.Outcome.VALIDATION_REJECTED, rejected.diagnostics());
        };
    }

    /** Captures current recovery state without changing content, lifecycle state, history, revision, or disk. */
    AuthoringBackupResult backup(String projectId, long expectedRevision) {
        ensureOpen();
        if (expectedRevision != revision) {
            return backupResult(AuthoringBackupResult.Outcome.STALE_REVISION, Optional.empty());
        }
        AuthoredDefinitionBackup backup = AuthoredDefinitionBackup.create(projectId, current);
        return backupResult(AuthoringBackupResult.Outcome.BACKED_UP, Optional.of(backup));
    }

    /** Strictly decodes and restores recovery state against this working copy's persisted physical baseline. */
    AuthoringRestoreResult restore(String projectId, byte[] encodedBackup, long expectedRevision) {
        ensureOpen();
        if (expectedRevision != revision) {
            return restoreResult(AuthoringRestoreResult.Outcome.STALE_REVISION, List.of());
        }
        AuthoredDefinitionBackup.DecodeResult decoded =
                AuthoredDefinitionBackup.decode(Objects.requireNonNull(encodedBackup, "encodedBackup"));
        return switch (decoded) {
            case AuthoredDefinitionBackup.DecodeResult.Decoded accepted -> restore(projectId, accepted.backup());
            case AuthoredDefinitionBackup.DecodeResult.Rejected rejected ->
                restoreResult(decodeOutcome(rejected.failure()), List.of());
        };
    }

    /** Returns locally authored entity identities modified from the persisted semantic baseline. */
    Set<EntityId> modifiedEntityIds() {
        ensureOpen();
        return AuthoredDefinitionStates.modifiedEntityIds(current.content(), persisted.content());
    }

    /** Releases listeners and rejects every subsequent operation. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            changes.close();
            undoHistory.clear();
            redoHistory.clear();
        }
    }

    /** Applies one candidate only after revision checking and records one accepted changed state. */
    private AuthoringMutationResult mutate(
            long expectedRevision,
            AuthoringOperation operation,
            AuthoringDefinitionChange.Kind kind,
            CandidateFactory candidates) {
        ensureOpen();
        Optional<AuthoringMutationResult> stale = stale(expectedRevision);
        if (stale.isPresent()) {
            return stale.orElseThrow();
        }
        return switch (candidates.create(current)) {
            case AuthoredDefinitionDocument.CandidateResult.Accepted accepted -> accept(operation, kind, accepted);
            case AuthoredDefinitionDocument.CandidateResult.Rejected rejected ->
                result(AuthoringMutationResult.Outcome.VALIDATION_REJECTED, rejected.diagnostics());
            case AuthoredDefinitionDocument.CandidateResult.InvalidTarget ignored ->
                result(AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
            case AuthoredDefinitionDocument.CandidateResult.InvalidEntityTarget ignored ->
                result(AuthoringMutationResult.Outcome.INVALID_TARGET, List.of());
        };
    }

    /** Records one accepted state-changing candidate or reports an accepted no-op. */
    private AuthoringMutationResult accept(
            AuthoringOperation operation,
            AuthoringDefinitionChange.Kind kind,
            AuthoredDefinitionDocument.CandidateResult.Accepted accepted) {
        if (!accepted.changed()) {
            return result(AuthoringMutationResult.Outcome.NO_OP, List.of());
        }
        AuthoredDefinitionDocument changed = accepted.document();
        undoHistory.push(new Transaction(operation, current, changed));
        redoHistory.clear();
        current = changed;
        revision++;
        emit(kind);
        return result(AuthoringMutationResult.Outcome.ACCEPTED, List.of());
    }

    /** Returns a stale outcome before any candidate or history work occurs. */
    private Optional<AuthoringMutationResult> stale(long expectedRevision) {
        return expectedRevision == revision
                ? Optional.empty()
                : Optional.of(result(AuthoringMutationResult.Outcome.STALE_REVISION, List.of()));
    }

    /** Establishes the exact successfully persisted bytes as the current semantic and physical baseline. */
    private AuthoringPersistenceResult acceptSave(AuthoredDefinitionDocument saved) {
        current = saved;
        persisted = saved;
        emit(AuthoringDefinitionChange.Kind.PERSISTED);
        return persistenceResult(AuthoringPersistenceResult.Outcome.SAVED, List.of());
    }

    /** Accepts one validated physical source while applying the defined revision and history semantics. */
    private AuthoringPersistenceResult acceptReload(AuthoredDefinitionDocument loaded) {
        boolean contentChanged = !current.hasSameAuthoredState(loaded);
        boolean fingerprintChanged = !current.sourceFingerprint().equals(loaded.sourceFingerprint());
        boolean wasDirty = isDirty();
        if (!contentChanged && !fingerprintChanged && !wasDirty) {
            return persistenceResult(AuthoringPersistenceResult.Outcome.NO_OP, List.of());
        }
        current = loaded;
        persisted = loaded;
        if (contentChanged) {
            undoHistory.clear();
            redoHistory.clear();
            revision++;
            emit(AuthoringDefinitionChange.Kind.REVERTED);
        } else if (wasDirty) {
            emit(AuthoringDefinitionChange.Kind.PERSISTED);
        }
        return persistenceResult(AuthoringPersistenceResult.Outcome.REVERTED, List.of());
    }

    /** Validates decoded recovery identity and content before any working-copy state changes. */
    private AuthoringRestoreResult restore(String projectId, AuthoredDefinitionBackup backup) {
        return switch (backup.restore(projectId, current)) {
            case AuthoredDefinitionBackup.RestoreResult.Restored restored -> acceptRestore(restored.document());
            case AuthoredDefinitionBackup.RestoreResult.IdentityRejected rejected ->
                restoreResult(identityOutcome(rejected.mismatch()), List.of());
            case AuthoredDefinitionBackup.RestoreResult.SourceConflict ignored ->
                restoreResult(AuthoringRestoreResult.Outcome.SOURCE_CHANGED, List.of());
            case AuthoredDefinitionBackup.RestoreResult.ValidationRejected rejected ->
                restoreResult(AuthoringRestoreResult.Outcome.VALIDATION_REJECTED, rejected.diagnostics());
        };
    }

    /** Accepts validated recovery content as one new-session content transition with no inherited history. */
    private AuthoringRestoreResult acceptRestore(AuthoredDefinitionDocument restored) {
        if (current.hasSameAuthoredState(restored)) {
            return restoreResult(AuthoringRestoreResult.Outcome.NO_OP, List.of());
        }
        current = restored;
        undoHistory.clear();
        redoHistory.clear();
        revision++;
        emit(AuthoringDefinitionChange.Kind.RESTORED);
        return restoreResult(AuthoringRestoreResult.Outcome.RESTORED, List.of());
    }

    /** Returns a stale persistence outcome before any source I/O occurs. */
    private Optional<AuthoringPersistenceResult> stalePersistence(long expectedRevision) {
        return expectedRevision == revision
                ? Optional.empty()
                : Optional.of(persistenceResult(AuthoringPersistenceResult.Outcome.STALE_REVISION, List.of()));
    }

    /** Maps the project-layer source model to the public authoring persistence vocabulary. */
    private static AuthoringPersistenceResult.Outcome sourceOutcome(AuthoredDefinitionDocument.SourceStatus status) {
        return switch (status) {
            case CONTENT_CHANGED -> AuthoringPersistenceResult.Outcome.SOURCE_CHANGED;
            case SOURCE_MISSING -> AuthoringPersistenceResult.Outcome.SOURCE_MISSING;
            case SOURCE_INACCESSIBLE -> AuthoringPersistenceResult.Outcome.SOURCE_INACCESSIBLE;
            case SOURCE_OUTSIDE_PROJECT -> AuthoringPersistenceResult.Outcome.SOURCE_OUTSIDE_PROJECT;
            case MATCH -> throw new IllegalArgumentException("matching source has no failure outcome");
        };
    }

    /** Maps strict project-layer decoding failures to the public recovery vocabulary. */
    private static AuthoringRestoreResult.Outcome decodeOutcome(AuthoredDefinitionBackup.DecodeFailure failure) {
        return switch (failure) {
            case MALFORMED -> AuthoringRestoreResult.Outcome.BACKUP_MALFORMED;
            case UNSUPPORTED_FORMAT -> AuthoringRestoreResult.Outcome.BACKUP_UNSUPPORTED;
            case INVALID_STRUCTURE -> AuthoringRestoreResult.Outcome.BACKUP_INVALID;
        };
    }

    /** Maps stable project-layer identity dimensions to the public recovery vocabulary. */
    private static AuthoringRestoreResult.Outcome identityOutcome(AuthoredDefinitionBackup.IdentityMismatch mismatch) {
        return switch (mismatch) {
            case PROJECT -> AuthoringRestoreResult.Outcome.PROJECT_MISMATCH;
            case DEFINITION -> AuthoringRestoreResult.Outcome.DEFINITION_MISMATCH;
            case KIND -> AuthoringRestoreResult.Outcome.KIND_MISMATCH;
            case SOURCE -> AuthoringRestoreResult.Outcome.SOURCE_MISMATCH;
        };
    }

    /** Creates one result from the current authoritative state. */
    private AuthoringMutationResult result(
            AuthoringMutationResult.Outcome outcome, List<ProjectDiagnostic> diagnostics) {
        return new AuthoringMutationResult(
                current.id(), outcome, revision, isDirty(), canUndo(), canRedo(), diagnostics);
    }

    /** Creates one persistence result from current authoritative lifecycle state. */
    private AuthoringPersistenceResult persistenceResult(
            AuthoringPersistenceResult.Outcome outcome, List<ProjectDiagnostic> diagnostics) {
        return new AuthoringPersistenceResult(
                current.id(), outcome, revision, isDirty(), canUndo(), canRedo(), diagnostics);
    }

    /** Creates one backup-capture result from current authoritative lifecycle state. */
    private AuthoringBackupResult backupResult(
            AuthoringBackupResult.Outcome outcome, Optional<AuthoredDefinitionBackup> backup) {
        return new AuthoringBackupResult(current.id(), outcome, revision, isDirty(), canUndo(), canRedo(), backup);
    }

    /** Creates one restore result from current authoritative lifecycle state. */
    private AuthoringRestoreResult restoreResult(
            AuthoringRestoreResult.Outcome outcome, List<ProjectDiagnostic> diagnostics) {
        return new AuthoringRestoreResult(
                current.id(), outcome, revision, isDirty(), canUndo(), canRedo(), diagnostics);
    }

    /** Emits one coherent post-transition state notification. */
    private void emit(AuthoringDefinitionChange.Kind kind) {
        changes.emit(new AuthoringDefinitionChange(current.id(), revision, isDirty(), canUndo(), canRedo(), kind));
    }

    /** Converts the retained document projection to the session's closed content model. */
    private static EditorRetainedDefinition.Content content(AuthoredDefinitionDocument document) {
        return switch (document.content()) {
            case AuthoredDefinitionDocument.Content.World world ->
                new EditorRetainedDefinition.Content.World(world.definition());
            case AuthoredDefinitionDocument.Content.Entity entity ->
                new EditorRetainedDefinition.Content.Entity(entity.definition());
        };
    }

    /** Rejects access after project-session disposal. */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Authored definition working copy is closed");
        }
    }

    /** Produces one source-preserving candidate from the current accepted document. */
    @FunctionalInterface
    private interface CandidateFactory {
        /** Returns one accepted, rejected, or invalid candidate. */
        AuthoredDefinitionDocument.CandidateResult create(AuthoredDefinitionDocument document);
    }

    /** One logical transaction retaining previously validated immutable document states. */
    private record Transaction(
            AuthoringOperation operation, AuthoredDefinitionDocument before, AuthoredDefinitionDocument after) {
        /** Validates one complete reversible transaction. */
        private Transaction {
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
        }
    }
}
