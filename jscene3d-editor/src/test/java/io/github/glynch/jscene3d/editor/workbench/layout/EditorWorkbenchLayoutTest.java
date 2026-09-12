/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Exercises session layout changes through the same seam used by JavaFX containers. */
final class EditorWorkbenchLayoutTest {
    private static final ViewId VIEW_ID = new ViewId("io.github.glynch.test.layout-view");

    /** Resolves defaults, moves registered views, and restores the declared default. */
    @Test
    void resolvesCurrentPlacementWithoutMutatingTheContribution() {
        EditorExtensionHost host = host();
        AtomicReference<EditorViewContribution> registered = new AtomicReference<>();
        host.activate(extension(registered));
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        List<List<EditorViewPlacement>> snapshots = new ArrayList<>();
        layout.observeViews(snapshots::add);

        assertThat(layout.containerOf(VIEW_ID)).contains(EditorViewContainers.PRIMARY_SIDEBAR);
        assertThat(snapshots.getLast())
                .singleElement()
                .extracting(EditorViewPlacement::container)
                .isEqualTo(EditorViewContainers.PRIMARY_SIDEBAR);
        assertThat(snapshots.getLast())
                .singleElement()
                .extracting(EditorViewPlacement::movable)
                .isEqualTo(true);

        layout.move(VIEW_ID, EditorViewContainers.BOTTOM_PANEL);

        assertThat(layout.containerOf(VIEW_ID)).contains(EditorViewContainers.BOTTOM_PANEL);
        assertThat(snapshots.getLast())
                .singleElement()
                .extracting(EditorViewPlacement::container)
                .isEqualTo(EditorViewContainers.BOTTOM_PANEL);
        assertThat(registered.get().container()).isEqualTo(EditorViewContainers.PRIMARY_SIDEBAR);

        layout.move(VIEW_ID, EditorViewContainers.PRIMARY_SIDEBAR);

        assertThat(layout.containerOf(VIEW_ID)).contains(EditorViewContainers.PRIMARY_SIDEBAR);
        layout.close();
        host.close();
    }

    /** Applies region visibility and side-bar position changes, then restores every layout default. */
    @Test
    void updatesAndResetsSessionLayoutState() {
        EditorExtensionHost host = host();
        host.activate(extension(new AtomicReference<>()));
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        List<EditorWorkbenchLayoutState> snapshots = new ArrayList<>();
        layout.observe(snapshots::add);

        assertThat(layout.current().visibleParts()).containsExactlyInAnyOrder(EditorWorkbenchPart.values());
        assertThat(layout.current().primarySidebarPosition()).isEqualTo(EditorPrimarySidebarPosition.LEFT);

        layout.setVisible(EditorWorkbenchPart.ACTIVITY_BAR, false);
        layout.setPrimarySidebarPosition(EditorPrimarySidebarPosition.RIGHT);
        layout.move(VIEW_ID, EditorViewContainers.BOTTOM_PANEL);
        layout.setVisible(EditorWorkbenchPart.PANEL, false);

        assertThat(layout.isVisible(EditorWorkbenchPart.ACTIVITY_BAR)).isFalse();
        assertThat(layout.current().primarySidebarPosition()).isEqualTo(EditorPrimarySidebarPosition.RIGHT);
        assertThat(snapshots.getLast().views())
                .singleElement()
                .extracting(EditorViewPlacement::container)
                .isEqualTo(EditorViewContainers.BOTTOM_PANEL);

        host.showView(VIEW_ID);

        assertThat(layout.isVisible(EditorWorkbenchPart.PANEL)).isTrue();

        layout.reset();

        assertThat(layout.current().visibleParts()).containsExactlyInAnyOrder(EditorWorkbenchPart.values());
        assertThat(layout.current().primarySidebarPosition()).isEqualTo(EditorPrimarySidebarPosition.LEFT);
        assertThat(layout.containerOf(VIEW_ID)).contains(EditorViewContainers.PRIMARY_SIDEBAR);
        layout.close();
        host.close();
    }

    /** Supports each major region exposed by the header's quick-access controls. */
    @Test
    void togglesQuickAccessRegionsIndependently() {
        EditorExtensionHost host = host();
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        List<EditorWorkbenchPart> quickAccessParts = List.of(
                EditorWorkbenchPart.PRIMARY_SIDEBAR, EditorWorkbenchPart.PANEL, EditorWorkbenchPart.SECONDARY_SIDEBAR);

        quickAccessParts.forEach(part -> layout.setVisible(part, false));

        assertThat(layout.current().visibleParts())
                .contains(EditorWorkbenchPart.ACTIVITY_BAR, EditorWorkbenchPart.STATUS_BAR)
                .doesNotContainAnyElementsOf(quickAccessParts);

        quickAccessParts.forEach(part -> layout.setVisible(part, true));

        assertThat(layout.current().visibleParts()).containsExactlyInAnyOrder(EditorWorkbenchPart.values());
        layout.close();
        host.close();
    }

    /** Rejects unknown views and containers which are not user-facing layout destinations. */
    @Test
    void rejectsUnsupportedMoves() {
        EditorExtensionHost host = host();
        host.activate(extension(new AtomicReference<>()));
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        ViewId unknown = new ViewId("io.github.glynch.test.unknown");

        assertThatThrownBy(() -> layout.move(unknown, EditorViewContainers.BOTTOM_PANEL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not registered");
        assertThatThrownBy(() -> layout.move(VIEW_ID, EditorViewContainers.EDITOR_AREA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be moved");

        layout.close();
        host.close();
    }

    /** Pins Activity Bar-owned views to their contributed primary-sidebar location. */
    @Test
    void preventsActivityBarViewsFromMoving() {
        EditorExtensionHost host = host();
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        host.activate(activityExtension());

        assertThat(layout.current().views())
                .singleElement()
                .extracting(EditorViewPlacement::movable)
                .isEqualTo(false);
        assertThatThrownBy(() -> layout.move(VIEW_ID, EditorViewContainers.BOTTOM_PANEL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activity Bar view cannot be moved");
        assertThat(layout.containerOf(VIEW_ID)).contains(EditorViewContainers.PRIMARY_SIDEBAR);

        layout.close();
        host.close();
    }

    /** Removes an override when its extension-owned view registration is removed. */
    @Test
    void forgetsPlacementsForRemovedViews() {
        EditorExtensionHost host = host();
        AtomicReference<EditorRegistration> registration = new AtomicReference<>();
        host.activate(new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.removable-layout";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                registration.set(context.views()
                        .register(
                                new EditorViewContribution(new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 10)));
            }
        });
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        layout.move(VIEW_ID, EditorViewContainers.BOTTOM_PANEL);

        registration.get().close();

        assertThat(layout.containerOf(VIEW_ID)).isEmpty();
        layout.close();
        host.close();
    }

    private static EditorExtension extension(AtomicReference<EditorViewContribution> registered) {
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.layout";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                EditorViewContribution contribution =
                        new EditorViewContribution(new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 10);
                registered.set(contribution);
                context.subscriptions().add(context.views().register(contribution));
            }
        };
    }

    private static EditorExtension activityExtension() {
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.activity-layout";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                context.subscriptions()
                        .add(context.views()
                                .register(new EditorViewContribution(
                                        new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 10)));
                context.subscriptions()
                        .add(context.activities()
                                .register(new EditorActivityContribution(
                                        new ActivityId("io.github.glynch.test.activity-layout"),
                                        "Activity Layout Test",
                                        new EditorIcon(EditorIcons.ENTITY, "Activity Layout Test"),
                                        VIEW_ID,
                                        10)));
            }
        };
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }

    private record TestView() implements EditorView {
        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Layout Test";
        }

        @Override
        public ViewKindId kind() {
            return new ViewKindId("io.github.glynch.test.layout-kind");
        }
    }
}
