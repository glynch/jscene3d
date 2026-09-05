/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.game.FixedUpdate;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateParticipant;
import io.github.glynch.jscene3d.project.runtime.FixedUpdatePhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable deterministic schedule for enabled fixed-update participants. */
final class FixedUpdateSchedule {
    private final List<FixedUpdateParticipant> beforePhysics;
    private final List<FixedUpdateParticipant> physics;
    private final List<FixedUpdateParticipant> afterPhysics;

    /** Classifies participants once while preserving authored order within each phase. */
    FixedUpdateSchedule(List<LifecycleEntry> lifecycle) {
        List<FixedUpdateParticipant> mutableBeforePhysics = new ArrayList<>();
        List<FixedUpdateParticipant> mutablePhysics = new ArrayList<>();
        List<FixedUpdateParticipant> mutableAfterPhysics = new ArrayList<>();
        for (LifecycleEntry entry : List.copyOf(lifecycle)) {
            if (entry.enabled() && entry.object() instanceof FixedUpdateParticipant participant) {
                FixedUpdatePhase phase =
                        Objects.requireNonNull(participant.fixedUpdatePhase(), "fixed-update participant phase");
                List<FixedUpdateParticipant> phaseParticipants =
                        switch (phase) {
                            case BEFORE_PHYSICS -> mutableBeforePhysics;
                            case PHYSICS -> mutablePhysics;
                            case AFTER_PHYSICS -> mutableAfterPhysics;
                        };
                phaseParticipants.add(participant);
            }
        }
        beforePhysics = List.copyOf(mutableBeforePhysics);
        physics = List.copyOf(mutablePhysics);
        afterPhysics = List.copyOf(mutableAfterPhysics);
    }

    /** Advances every participant in deterministic phase and authored order. */
    void update(FixedUpdate update) {
        FixedUpdate validUpdate = Objects.requireNonNull(update, "update");
        updateParticipants(beforePhysics, validUpdate);
        updateParticipants(physics, validUpdate);
        updateParticipants(afterPhysics, validUpdate);
    }

    /** Advances one immutable phase schedule in authored order. */
    private static void updateParticipants(List<FixedUpdateParticipant> participants, FixedUpdate update) {
        for (FixedUpdateParticipant participant : participants) {
            participant.fixedUpdate(update);
        }
    }
}
