/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Verifies the editor shell's reference geometry and minimum usable bounds. */
final class EditorWorkspaceLayoutTest {
    /** Uses the accepted side-panel widths at the 1440-pixel reference width. */
    @Test
    void usesReferenceSidePanelWidths() {
        double width = 1440.0;
        EditorWorkspaceLayout.HorizontalDividers dividers = EditorWorkspaceLayout.horizontal(width);
        double leftWorkspaceWidth = dividers.inspectorStart() * width;

        assertThat(dividers.hierarchyEnd() * leftWorkspaceWidth)
                .isCloseTo(EditorWorkspaceLayout.PREFERRED_HIERARCHY_WIDTH, within(0.000_001));
        assertThat((1.0 - dividers.inspectorStart()) * width)
                .isCloseTo(EditorWorkspaceLayout.PREFERRED_INSPECTOR_WIDTH, within(0.000_001));
    }

    /** Preserves all three column minimums when the shell reaches its minimum width. */
    @Test
    void preservesMinimumColumnWidths() {
        double width = EditorWorkspaceLayout.MINIMUM_WORKSPACE_WIDTH;
        EditorWorkspaceLayout.HorizontalDividers dividers = EditorWorkspaceLayout.horizontal(width);
        double leftWorkspaceWidth = dividers.inspectorStart() * width;
        double hierarchyWidth = dividers.hierarchyEnd() * leftWorkspaceWidth;
        double centreWidth = (1.0 - dividers.hierarchyEnd()) * leftWorkspaceWidth;
        double inspectorWidth = (1.0 - dividers.inspectorStart()) * width;

        assertThat(hierarchyWidth).isGreaterThanOrEqualTo(EditorWorkspaceLayout.MINIMUM_HIERARCHY_WIDTH);
        assertThat(centreWidth).isGreaterThanOrEqualTo(EditorWorkspaceLayout.MINIMUM_CENTRE_WIDTH);
        assertThat(inspectorWidth).isGreaterThanOrEqualTo(EditorWorkspaceLayout.MINIMUM_INSPECTOR_WIDTH);
    }

    /** Keeps the lower region near its target while protecting preview height. */
    @Test
    void boundsVerticalRegions() {
        double referenceHeight = 822.0;
        double referenceDivider = EditorWorkspaceLayout.vertical(referenceHeight);
        double minimumHeight =
                EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT + EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT;
        double minimumDivider = EditorWorkspaceLayout.vertical(minimumHeight);

        assertThat((1.0 - referenceDivider) * referenceHeight)
                .isCloseTo(EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT, within(0.000_001));
        assertThat(minimumDivider * minimumHeight)
                .isCloseTo(EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT, within(0.000_001));
        assertThat((1.0 - minimumDivider) * minimumHeight)
                .isCloseTo(EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT, within(0.000_001));
    }

    /** Allows a collapsed panel header to return its space to the preview. */
    @Test
    void positionsARequestedCollapsedPanelHeight() {
        double height = 822.0;
        double panelHeight = 34.0;

        double divider = EditorWorkspaceLayout.verticalForBottomHeight(height, panelHeight);

        assertThat((1.0 - divider) * height).isCloseTo(panelHeight, within(0.000_001));
    }
}
