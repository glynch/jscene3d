/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Verifies project-build state transitions through the coordinator interface. */
final class ProjectBuildCoordinatorTest {
    private ProjectBuildCoordinator coordinator;

    @AfterEach
    void closeCoordinator() {
        if (coordinator != null) {
            coordinator.close();
        }
    }

    @Test
    void completesManualBuildForCurrentSavedRevision() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        List<ProjectBuildPhase> phases = new ArrayList<>();
        coordinator.observe(snapshot -> phases.add(snapshot.phase()));

        coordinator.request(ProjectBuildKind.INCREMENTAL);

        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL));
        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.BUILDING);

        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(coordinator.snapshot())
                .isEqualTo(new ProjectBuildSnapshot(ProjectBuildPhase.CURRENT, 0, OptionalLong.of(0), false, false));
        assertThat(phases)
                .containsExactly(
                        ProjectBuildPhase.UNKNOWN,
                        ProjectBuildPhase.QUEUED,
                        ProjectBuildPhase.BUILDING,
                        ProjectBuildPhase.CURRENT);
    }

    @Test
    void keepsSavedChangesStaleUntilManuallyBuiltWhenAutomaticBuildIsDisabled() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);

        coordinator.savedBuildRelevantChange();

        assertThat(adapter.requests()).isEmpty();
        assertThat(coordinator.snapshot())
                .isEqualTo(new ProjectBuildSnapshot(ProjectBuildPhase.STALE, 1, OptionalLong.empty(), false, false));

        coordinator.request(ProjectBuildKind.CLEAN);
        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(1, ProjectBuildKind.CLEAN));
        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.CURRENT);
    }

    @Test
    void coalescesChangesDuringAnAutomaticBuildIntoOneNewestRevisionFollowUp() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, true);

        coordinator.savedBuildRelevantChange();
        coordinator.savedBuildRelevantChange();
        coordinator.savedBuildRelevantChange();

        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(1, ProjectBuildKind.INCREMENTAL));
        assertThat(coordinator.snapshot())
                .isEqualTo(new ProjectBuildSnapshot(ProjectBuildPhase.BUILDING, 3, OptionalLong.empty(), true, true));

        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(adapter.requests())
                .containsExactly(
                        new ProjectBuildRequest(1, ProjectBuildKind.INCREMENTAL),
                        new ProjectBuildRequest(3, ProjectBuildKind.INCREMENTAL));
        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.BUILDING);

        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(coordinator.snapshot())
                .isEqualTo(new ProjectBuildSnapshot(ProjectBuildPhase.CURRENT, 3, OptionalLong.of(3), true, false));
    }

    @Test
    void preservesCleanIntentWhenRequestsAreCoalescedBehindAnActiveBuild() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);

        coordinator.request(ProjectBuildKind.INCREMENTAL);
        coordinator.request(ProjectBuildKind.CLEAN);
        coordinator.request(ProjectBuildKind.INCREMENTAL);

        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL));
        assertThat(coordinator.snapshot().followUpQueued()).isTrue();

        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(adapter.requests())
                .containsExactly(
                        new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL),
                        new ProjectBuildRequest(0, ProjectBuildKind.CLEAN));
    }

    @Test
    void cancellationReturnsToStaleWithoutStartingAQueuedFollowUp() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, true);
        coordinator.savedBuildRelevantChange();
        coordinator.savedBuildRelevantChange();

        boolean cancelled = coordinator.cancel();

        assertThat(cancelled).isTrue();
        assertThat(adapter.activeBuildWasCancelled()).isTrue();
        assertThat(coordinator.snapshot().followUpQueued()).isFalse();

        adapter.completeActive(ProjectBuildOutcome.CANCELLED);

        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(1, ProjectBuildKind.INCREMENTAL));
        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.STALE);
    }

    @Test
    void enablingAutomaticBuildBringsAStaleSavedRevisionCurrent() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        coordinator.savedBuildRelevantChange();

        coordinator.automaticBuild(true);

        assertThat(coordinator.snapshot().automaticBuild()).isTrue();
        assertThat(adapter.requests()).containsExactly(new ProjectBuildRequest(1, ProjectBuildKind.INCREMENTAL));

        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.CURRENT);
    }

    @Test
    void reportsAdapterStartupFailureWithoutEscapingTheCoordinator() {
        ProjectBuildAdapter adapter = request -> {
            throw new IllegalStateException("build tool is unavailable");
        };
        coordinator = new ProjectBuildCoordinator(adapter, false);

        assertThatCode(() -> coordinator.request(ProjectBuildKind.INCREMENTAL)).doesNotThrowAnyException();

        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.FAILED);
    }

    @Test
    void retainsTheLastSuccessfulRevisionAfterANewerBuildFails() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        coordinator.request(ProjectBuildKind.INCREMENTAL);
        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);
        coordinator.savedBuildRelevantChange();

        coordinator.request(ProjectBuildKind.INCREMENTAL);
        adapter.completeActive(ProjectBuildOutcome.FAILED);

        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.FAILED);
        assertThat(coordinator.snapshot().successfulRevision()).isEqualTo(OptionalLong.of(0));
    }

    @Test
    void reportsExceptionalAdapterCompletionAsFailure() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        coordinator.request(ProjectBuildKind.INCREMENTAL);

        adapter.failActive(new IllegalStateException("build process exited unexpectedly"));

        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.FAILED);
    }

    @Test
    void cancelledRebuildKeepsAnIndependentlyCurrentSuccessfulRevision() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        coordinator.request(ProjectBuildKind.INCREMENTAL);
        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);
        coordinator.request(ProjectBuildKind.CLEAN);

        coordinator.cancel();
        adapter.completeActive(ProjectBuildOutcome.CANCELLED);

        assertThat(coordinator.snapshot().phase()).isEqualTo(ProjectBuildPhase.CURRENT);
        assertThat(coordinator.snapshot().successfulRevision()).isEqualTo(OptionalLong.of(0));
    }

    @Test
    void closingCancelsTheActiveBuildAndIsolatesLateCompletion() {
        FakeProjectBuildAdapter adapter = new FakeProjectBuildAdapter();
        coordinator = new ProjectBuildCoordinator(adapter, false);
        List<ProjectBuildSnapshot> snapshots = new ArrayList<>();
        coordinator.observe(snapshots::add);
        coordinator.request(ProjectBuildKind.INCREMENTAL);

        coordinator.close();
        int snapshotCountAtClose = snapshots.size();
        adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

        assertThat(adapter.activeBuildWasCancelled()).isTrue();
        assertThat(snapshots).hasSize(snapshotCountAtClose);
        assertThatThrownBy(() -> coordinator.request(ProjectBuildKind.INCREMENTAL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }
}
