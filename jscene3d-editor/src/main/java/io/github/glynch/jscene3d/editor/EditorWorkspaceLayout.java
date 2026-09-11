/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

/** Calculates bounded initial divider positions for the resizable editor workspace. */
public final class EditorWorkspaceLayout {
    /** Smallest supported width of the composed workbench. */
    public static final double MINIMUM_WORKSPACE_WIDTH = 780.0;

    /** Smallest width retained for the central editor area. */
    public static final double MINIMUM_CENTRE_WIDTH = 360.0;

    /** Smallest width retained for the primary side bar. */
    public static final double MINIMUM_PRIMARY_SIDEBAR_WIDTH = 180.0;

    /** Smallest width retained for the secondary side bar. */
    public static final double MINIMUM_SECONDARY_SIDEBAR_WIDTH = 240.0;

    /** Smallest combined width of the primary side bar and central editor area. */
    public static final double MINIMUM_PRIMARY_WORKSPACE_WIDTH = MINIMUM_PRIMARY_SIDEBAR_WIDTH + MINIMUM_CENTRE_WIDTH;

    /** Preferred initial width of the primary side bar. */
    public static final double PREFERRED_PRIMARY_SIDEBAR_WIDTH = 240.0;

    /** Preferred initial width of the secondary side bar. */
    public static final double PREFERRED_SECONDARY_SIDEBAR_WIDTH = 320.0;

    /** Smallest height retained for the central scene preview. */
    public static final double MINIMUM_PREVIEW_HEIGHT = 240.0;

    /** Smallest expanded height of the lower panel. */
    public static final double MINIMUM_BOTTOM_HEIGHT = 120.0;

    /** Preferred initial height of the lower panel. */
    public static final double PREFERRED_BOTTOM_HEIGHT = 250.0;

    private static final double PRIMARY_SIDEBAR_SHRINK_CAPACITY =
            PREFERRED_PRIMARY_SIDEBAR_WIDTH - MINIMUM_PRIMARY_SIDEBAR_WIDTH;
    private static final double SECONDARY_SIDEBAR_SHRINK_CAPACITY =
            PREFERRED_SECONDARY_SIDEBAR_WIDTH - MINIMUM_SECONDARY_SIDEBAR_WIDTH;
    private static final double TOTAL_SHRINK_CAPACITY =
            PRIMARY_SIDEBAR_SHRINK_CAPACITY + SECONDARY_SIDEBAR_SHRINK_CAPACITY;

    private EditorWorkspaceLayout() {}

    /**
     * Returns cumulative horizontal divider positions for the supplied content width.
     *
     * @param contentWidth available workbench width
     * @return bounded horizontal divider positions
     */
    public static HorizontalDividers horizontal(double contentWidth) {
        double width = Math.max(MINIMUM_WORKSPACE_WIDTH, contentWidth);
        double availableForSidePanels = width - MINIMUM_CENTRE_WIDTH;
        double preferredSidePanelWidth = PREFERRED_PRIMARY_SIDEBAR_WIDTH + PREFERRED_SECONDARY_SIDEBAR_WIDTH;
        double shrink = Math.max(0.0, preferredSidePanelWidth - availableForSidePanels);
        double primarySidebarWidth =
                PREFERRED_PRIMARY_SIDEBAR_WIDTH - shrink * PRIMARY_SIDEBAR_SHRINK_CAPACITY / TOTAL_SHRINK_CAPACITY;
        double secondarySidebarWidth =
                PREFERRED_SECONDARY_SIDEBAR_WIDTH - shrink * SECONDARY_SIDEBAR_SHRINK_CAPACITY / TOTAL_SHRINK_CAPACITY;
        double primaryWorkspaceWidth = width - secondarySidebarWidth;
        return new HorizontalDividers(primarySidebarWidth / primaryWorkspaceWidth, primaryWorkspaceWidth / width);
    }

    /**
     * Returns the vertical divider position while retaining usable preview and lower regions.
     *
     * @param contentHeight available workbench height
     * @return bounded vertical divider position
     */
    public static double vertical(double contentHeight) {
        return verticalForBottomHeight(contentHeight, PREFERRED_BOTTOM_HEIGHT);
    }

    /**
     * Returns the vertical divider position for one requested lower-panel height.
     *
     * @param contentHeight available workbench height
     * @param requestedBottomHeight requested lower-panel height
     * @return bounded vertical divider position
     */
    public static double verticalForBottomHeight(double contentHeight, double requestedBottomHeight) {
        double requested = Math.max(0.0, requestedBottomHeight);
        double minimumHeight = MINIMUM_PREVIEW_HEIGHT + Math.min(MINIMUM_BOTTOM_HEIGHT, requested);
        double height = Math.max(minimumHeight, contentHeight);
        double bottomHeight = Math.clamp(requested, 0.0, height - MINIMUM_PREVIEW_HEIGHT);
        return (height - bottomHeight) / height;
    }

    /**
     * Divider positions for the nested upper workspace and outer secondary-side-bar split panes.
     *
     * @param primarySidebarEnd end position of the primary-side-bar region
     * @param secondarySidebarStart start position of the secondary-side-bar region
     */
    public record HorizontalDividers(double primarySidebarEnd, double secondarySidebarStart) {}
}
