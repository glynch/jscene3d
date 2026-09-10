/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies the editor's user-facing loading sequence remains coherent. */
final class EditorLoadingPhaseTest {
    /** Keeps phase progress cumulative and descriptions suitable for direct display. */
    @Test
    void definesOrderedCumulativeProgress() {
        List<Double> progress = Arrays.stream(EditorLoadingPhase.values())
                .map(EditorLoadingPhase::progress)
                .toList();
        assertThat(progress).isSorted().doesNotHaveDuplicates().allMatch(value -> value > 0.0 && value <= 1.0);
        assertThat(EditorLoadingPhase.values())
                .allSatisfy(phase -> assertThat(phase.description()).isNotBlank());
    }
}
