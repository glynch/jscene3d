/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

/** Calculates bounded initial divider positions for the resizable editor workspace. */
final class EditorWorkspaceLayout {
    static final double MINIMUM_WORKSPACE_WIDTH = 780.0;
    static final double MINIMUM_CENTRE_WIDTH = 360.0;
    static final double MINIMUM_HIERARCHY_WIDTH = 180.0;
    static final double MINIMUM_INSPECTOR_WIDTH = 240.0;
    static final double MINIMUM_LEFT_WORKSPACE_WIDTH = MINIMUM_HIERARCHY_WIDTH + MINIMUM_CENTRE_WIDTH;
    static final double PREFERRED_HIERARCHY_WIDTH = 240.0;
    static final double PREFERRED_INSPECTOR_WIDTH = 320.0;
    static final double MINIMUM_PREVIEW_HEIGHT = 240.0;
    static final double MINIMUM_BOTTOM_HEIGHT = 120.0;
    static final double PREFERRED_BOTTOM_HEIGHT = 250.0;

    private static final double HIERARCHY_SHRINK_CAPACITY = PREFERRED_HIERARCHY_WIDTH - MINIMUM_HIERARCHY_WIDTH;
    private static final double INSPECTOR_SHRINK_CAPACITY = PREFERRED_INSPECTOR_WIDTH - MINIMUM_INSPECTOR_WIDTH;
    private static final double TOTAL_SHRINK_CAPACITY = HIERARCHY_SHRINK_CAPACITY + INSPECTOR_SHRINK_CAPACITY;

    private EditorWorkspaceLayout() {}

    /** Returns cumulative horizontal divider positions for the supplied content width. */
    static HorizontalDividers horizontal(double contentWidth) {
        double width = Math.max(MINIMUM_WORKSPACE_WIDTH, contentWidth);
        double availableForSidePanels = width - MINIMUM_CENTRE_WIDTH;
        double preferredSidePanelWidth = PREFERRED_HIERARCHY_WIDTH + PREFERRED_INSPECTOR_WIDTH;
        double shrink = Math.max(0.0, preferredSidePanelWidth - availableForSidePanels);
        double hierarchyWidth = PREFERRED_HIERARCHY_WIDTH - shrink * HIERARCHY_SHRINK_CAPACITY / TOTAL_SHRINK_CAPACITY;
        double inspectorWidth = PREFERRED_INSPECTOR_WIDTH - shrink * INSPECTOR_SHRINK_CAPACITY / TOTAL_SHRINK_CAPACITY;
        double leftWorkspaceWidth = width - inspectorWidth;
        return new HorizontalDividers(hierarchyWidth / leftWorkspaceWidth, leftWorkspaceWidth / width);
    }

    /** Returns the vertical divider position while retaining usable preview and lower regions. */
    static double vertical(double contentHeight) {
        return verticalForBottomHeight(contentHeight, PREFERRED_BOTTOM_HEIGHT);
    }

    /** Returns the vertical divider position for one requested lower-panel height. */
    static double verticalForBottomHeight(double contentHeight, double requestedBottomHeight) {
        double requested = Math.max(0.0, requestedBottomHeight);
        double minimumHeight = MINIMUM_PREVIEW_HEIGHT + Math.min(MINIMUM_BOTTOM_HEIGHT, requested);
        double height = Math.max(minimumHeight, contentHeight);
        double bottomHeight = Math.clamp(requested, 0.0, height - MINIMUM_PREVIEW_HEIGHT);
        return (height - bottomHeight) / height;
    }

    /** Divider positions for the nested upper workspace and outer Inspector split panes. */
    record HorizontalDividers(double hierarchyEnd, double inspectorStart) {}
}
