/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** One immutable progress update emitted on the import operation's calling thread. */
public final class ImportProgress {
    private final ImportPhase phase;
    private final String description;
    private final OptionalLong completedWork;
    private final OptionalLong totalWork;
    private final Optional<String> sourceItem;

    /**
     * Creates an unquantified phase update.
     *
     * @param phase current import phase
     * @param description human-readable activity
     * @return progress update without a work count or source item
     */
    public static ImportProgress phase(ImportPhase phase, String description) {
        return new ImportProgress(phase, description, OptionalLong.empty(), OptionalLong.empty(), Optional.empty());
    }

    /**
     * Creates a quantified progress update.
     *
     * @param phase current import phase
     * @param description human-readable activity
     * @param completedWork non-negative completed work
     * @param totalWork positive total work not below completed work
     * @param sourceItem optional source-item identity
     * @return quantified progress update
     */
    public static ImportProgress quantified(
            ImportPhase phase, String description, long completedWork, long totalWork, Optional<String> sourceItem) {
        return new ImportProgress(
                phase,
                description,
                OptionalLong.of(completedWork),
                OptionalLong.of(totalWork),
                Objects.requireNonNull(sourceItem, "sourceItem"));
    }

    /** Stores one validated progress update. */
    private ImportProgress(
            ImportPhase phase,
            String description,
            OptionalLong completedWork,
            OptionalLong totalWork,
            Optional<String> sourceItem) {
        this.phase = Objects.requireNonNull(phase, "phase");
        this.description = Preconditions.requireNonBlank(description, "description");
        this.completedWork = Objects.requireNonNull(completedWork, "completedWork");
        this.totalWork = Objects.requireNonNull(totalWork, "totalWork");
        this.sourceItem = Preconditions.requireOptionalPortableIdentity(sourceItem, "sourceItem");
        requireWorkRange();
    }

    /**
     * Returns the current import phase.
     *
     * @return current import phase
     */
    public ImportPhase phase() {
        return phase;
    }

    /**
     * Returns the current human-readable activity.
     *
     * @return human-readable current activity
     */
    public String description() {
        return description;
    }

    /**
     * Returns completed work when the activity is measurable.
     *
     * @return completed work when the activity is measurable
     */
    public OptionalLong completedWork() {
        return completedWork;
    }

    /**
     * Returns total work when the activity is measurable.
     *
     * @return total work when the activity is measurable
     */
    public OptionalLong totalWork() {
        return totalWork;
    }

    /**
     * Returns the source-item identity when this update concerns one item.
     *
     * @return source-item identity when the update concerns one item
     */
    public Optional<String> sourceItem() {
        return sourceItem;
    }

    /** Requires paired work values forming a valid range. */
    private void requireWorkRange() {
        if (completedWork.isPresent() != totalWork.isPresent()) {
            throw new IllegalArgumentException("completedWork and totalWork must be present together");
        }
        if (completedWork.isPresent()) {
            long completed = completedWork.orElseThrow();
            long total = totalWork.orElseThrow();
            if (completed < 0L || total < 1L || completed > total) {
                throw new IllegalArgumentException("work must satisfy 0 <= completedWork <= totalWork");
            }
        }
    }
}
