/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Serializes project builds and publishes the freshness of saved project content. */
public final class ProjectBuildCoordinator implements AutoCloseable {
    private final ProjectBuildAdapter adapter;
    private final List<Consumer<ProjectBuildSnapshot>> observers = new ArrayList<>();

    private ProjectBuildPhase phase = ProjectBuildPhase.UNKNOWN;
    private long savedRevision;
    private boolean automaticBuild;
    private @Nullable ProjectBuildRequest activeRequest;
    private @Nullable ProjectBuildExecution activeExecution;
    private @Nullable ProjectBuildKind pendingKind;
    private long successfulRevision = -1;
    private boolean cancellationRequested;
    private boolean closed;

    /**
     * Creates a coordinator for one open project.
     *
     * @param adapter selected project build-system adapter
     * @param automaticBuild whether relevant saves initially request builds
     */
    public ProjectBuildCoordinator(ProjectBuildAdapter adapter, boolean automaticBuild) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.automaticBuild = automaticBuild;
    }

    /**
     * Returns the latest observable build state.
     *
     * @return current immutable state
     */
    public synchronized ProjectBuildSnapshot snapshot() {
        OptionalLong successful = successfulRevision < 0 ? OptionalLong.empty() : OptionalLong.of(successfulRevision);
        return new ProjectBuildSnapshot(phase, savedRevision, successful, automaticBuild, pendingKind != null);
    }

    /**
     * Observes state changes and immediately receives the current state.
     *
     * @param observer state observer
     * @return registration which stops subsequent notifications when closed
     */
    public synchronized EditorRegistration observe(Consumer<ProjectBuildSnapshot> observer) {
        Consumer<ProjectBuildSnapshot> listener = Objects.requireNonNull(observer, "observer");
        ensureOpen();
        observers.add(listener);
        listener.accept(snapshot());
        return () -> removeObserver(listener);
    }

    /**
     * Requests a build of the newest saved revision.
     *
     * @param kind semantic build intent
     */
    public void request(ProjectBuildKind kind) {
        ProjectBuildRequest request;
        synchronized (this) {
            ensureOpen();
            ProjectBuildKind requestedKind = Objects.requireNonNull(kind, "kind");
            if (activeRequest != null) {
                pendingKind = merge(pendingKind, requestedKind);
                publish();
                return;
            }
            request = queue(requestedKind);
        }
        start(request);
    }

    /** Records that successfully saved build-relevant input advanced the project revision. */
    public void savedBuildRelevantChange() {
        ProjectBuildRequest request = null;
        synchronized (this) {
            ensureOpen();
            savedRevision++;
            if (!automaticBuild) {
                if (activeRequest == null) {
                    transition(ProjectBuildPhase.STALE);
                } else {
                    publish();
                }
                return;
            }
            if (activeRequest == null) {
                request = queue(ProjectBuildKind.INCREMENTAL);
            } else {
                pendingKind = merge(pendingKind, ProjectBuildKind.INCREMENTAL);
                publish();
            }
        }
        if (request != null) {
            start(request);
        }
    }

    /**
     * Cancels the active build and discards any coalesced follow-up request.
     *
     * @return whether a queued or running build accepted the cancellation request
     */
    public boolean cancel() {
        ProjectBuildExecution execution;
        synchronized (this) {
            ensureOpen();
            if (activeRequest == null) {
                return false;
            }
            pendingKind = null;
            cancellationRequested = true;
            execution = activeExecution;
            publish();
        }
        if (execution != null) {
            execution.cancel();
        }
        return true;
    }

    /**
     * Changes whether relevant saved revisions request builds automatically.
     *
     * @param enabled whether automatic building is enabled
     */
    public void automaticBuild(boolean enabled) {
        ProjectBuildRequest request = null;
        synchronized (this) {
            ensureOpen();
            if (automaticBuild == enabled) {
                return;
            }
            automaticBuild = enabled;
            if (!enabled || successfulRevision == savedRevision) {
                publish();
                return;
            }
            if (activeRequest == null) {
                request = queue(ProjectBuildKind.INCREMENTAL);
            } else if (activeRequest.revision() != savedRevision) {
                pendingKind = merge(pendingKind, ProjectBuildKind.INCREMENTAL);
                publish();
            } else {
                publish();
            }
        }
        if (request != null) {
            start(request);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        observers.clear();
        if (activeExecution != null) {
            activeExecution.cancel();
        }
    }

    private void start(ProjectBuildRequest request) {
        ProjectBuildExecution execution;
        try {
            execution = Objects.requireNonNull(adapter.start(request), "adapter execution");
        } catch (RuntimeException failure) {
            complete(request, null, failure);
            return;
        }
        synchronized (this) {
            if (closed) {
                execution.cancel();
                return;
            }
            activeExecution = execution;
            transition(ProjectBuildPhase.BUILDING);
            if (cancellationRequested) {
                execution.cancel();
            }
        }
        execution.completion().whenComplete((outcome, failure) -> complete(request, outcome, failure));
    }

    private void complete(
            ProjectBuildRequest request, @Nullable ProjectBuildOutcome outcome, @Nullable Throwable failure) {
        ProjectBuildRequest followUp = null;
        synchronized (this) {
            if (closed || !request.equals(activeRequest)) {
                return;
            }
            activeRequest = null;
            activeExecution = null;
            cancellationRequested = false;
            if (failure == null && outcome == ProjectBuildOutcome.SUCCEEDED) {
                successfulRevision = request.revision();
            }
            if (pendingKind != null) {
                ProjectBuildKind requestedKind = pendingKind;
                pendingKind = null;
                followUp = queue(requestedKind);
            } else {
                transition(terminalPhase(request, outcome, failure));
            }
        }
        if (followUp != null) {
            start(followUp);
        }
    }

    private void transition(ProjectBuildPhase replacement) {
        phase = Objects.requireNonNull(replacement, "replacement");
        publish();
    }

    private void publish() {
        ProjectBuildSnapshot current = snapshot();
        List.copyOf(observers).forEach(observer -> observer.accept(current));
    }

    private ProjectBuildRequest queue(ProjectBuildKind kind) {
        ProjectBuildRequest request = new ProjectBuildRequest(savedRevision, kind);
        activeRequest = request;
        cancellationRequested = false;
        transition(ProjectBuildPhase.QUEUED);
        return request;
    }

    private ProjectBuildPhase terminalPhase(
            ProjectBuildRequest request, @Nullable ProjectBuildOutcome outcome, @Nullable Throwable failure) {
        if (failure == null && outcome == ProjectBuildOutcome.SUCCEEDED && request.revision() == savedRevision) {
            return ProjectBuildPhase.CURRENT;
        }
        if (failure == null && outcome == ProjectBuildOutcome.CANCELLED) {
            return successfulRevision == savedRevision ? ProjectBuildPhase.CURRENT : ProjectBuildPhase.STALE;
        }
        return request.revision() == savedRevision ? ProjectBuildPhase.FAILED : ProjectBuildPhase.STALE;
    }

    private static ProjectBuildKind merge(@Nullable ProjectBuildKind pending, ProjectBuildKind requested) {
        return pending == ProjectBuildKind.CLEAN || requested == ProjectBuildKind.CLEAN
                ? ProjectBuildKind.CLEAN
                : ProjectBuildKind.INCREMENTAL;
    }

    private synchronized void removeObserver(Consumer<ProjectBuildSnapshot> observer) {
        observers.remove(observer);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("project build coordinator is closed");
        }
    }
}
