/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.activity;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchPart;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies Activity Bar container selection independently from sidebar visibility. */
class EditorActivitySelectionTest {
    private static final ActivityId HIERARCHY_ACTIVITY = new ActivityId("io.github.glynch.test.hierarchy-activity");
    private static final ActivityId EXTENSIONS_ACTIVITY = new ActivityId("io.github.glynch.test.extensions-activity");
    private static final ViewId HIERARCHY_VIEW = new ViewId("io.github.glynch.test.hierarchy-view");
    private static final ViewId EXTENSIONS_VIEW = new ViewId("io.github.glynch.test.extensions-view");
    private static final ViewId ORDINARY_VIEW = new ViewId("io.github.glynch.test.ordinary-view");

    @Test
    void switchesContainersWithoutCouplingSelectionToSidebarVisibility() {
        EditorExtensionHost host = host();
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        EditorActivitySelection selection = new EditorActivitySelection(host, layout, HIERARCHY_ACTIVITY);
        host.activate(activityExtension());

        assertThat(selection.current().selected()).contains(HIERARCHY_ACTIVITY);
        assertThat(selection.current().selectedViews()).containsExactly(HIERARCHY_VIEW);
        assertThat(selection.current().activityViews()).containsExactlyInAnyOrder(HIERARCHY_VIEW, EXTENSIONS_VIEW);
        assertThat(selection.current().includes(HIERARCHY_VIEW)).isTrue();
        assertThat(selection.current().includes(EXTENSIONS_VIEW)).isFalse();
        assertThat(selection.current().includes(ORDINARY_VIEW)).isTrue();

        layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, false);

        assertThat(selection.current().selected()).contains(HIERARCHY_ACTIVITY);

        selection.select(EXTENSIONS_ACTIVITY);

        assertThat(selection.current().selected()).contains(EXTENSIONS_ACTIVITY);
        assertThat(selection.current().selectedViews()).containsExactly(EXTENSIONS_VIEW);
        assertThat(selection.current().includes(HIERARCHY_VIEW)).isFalse();
        assertThat(selection.current().includes(EXTENSIONS_VIEW)).isTrue();
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isTrue();

        selection.select(EXTENSIONS_ACTIVITY);
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isFalse();

        selection.select(EXTENSIONS_ACTIVITY);
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isTrue();
        assertThat(selection.current().selectedViews()).containsExactly(EXTENSIONS_VIEW);

        selection.close();
        layout.close();
        host.close();
    }

    @Test
    void revealRequestSelectsOwningContainerAndShowsSidebar() {
        EditorExtensionHost host = host();
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        EditorActivitySelection selection = new EditorActivitySelection(host, layout, HIERARCHY_ACTIVITY);
        host.activate(activityExtension());
        layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, false);

        host.showView(EXTENSIONS_VIEW);

        assertThat(selection.current().selected()).contains(EXTENSIONS_ACTIVITY);
        assertThat(selection.current().selectedViews()).containsExactly(EXTENSIONS_VIEW);
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isTrue();

        selection.close();
        layout.close();
        host.close();
    }

    @Test
    void reappliesSelectionWhenTheSelectedActivityIsRevealedAgain() {
        EditorExtensionHost host = host();
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        EditorActivitySelection selection = new EditorActivitySelection(host, layout, HIERARCHY_ACTIVITY);
        host.activate(activityExtension());
        List<EditorActivitySelectionState> snapshots = new ArrayList<>();
        selection.observe(snapshots::add);

        int initialSnapshotCount = snapshots.size();

        selection.revealDefault();

        assertThat(snapshots).hasSize(initialSnapshotCount + 1);
        assertThat(snapshots.getLast().selected()).contains(HIERARCHY_ACTIVITY);
        assertThat(layout.current().isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR))
                .isTrue();
        selection.close();
        layout.close();
        host.close();
    }

    @Test
    void keepsSidebarClosedUntilTheDefaultActivityBecomesAvailable() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorExtensionHost host = new EditorExtensionHost(projects, new EditorSelectionContext());
        EditorWorkbenchLayout layout = new EditorWorkbenchLayout(host);
        EditorActivitySelection selection = new EditorActivitySelection(host, layout, HIERARCHY_ACTIVITY);
        host.activate(contextualActivityExtension());

        assertThat(selection.current().selected()).isEmpty();
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isFalse();
        assertThat(layout.current().isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR))
                .isFalse();

        projects.showProject(
                new EditorProject("project", "Project", URI.create("file:///project")), hierarchy(), List.of());

        assertThat(selection.current().selected()).contains(HIERARCHY_ACTIVITY);
        assertThat(selection.current().selectedViews()).containsExactly(HIERARCHY_VIEW);
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isTrue();
        assertThat(layout.current().isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR))
                .isTrue();

        layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, false);
        assertThat(selection.current().selected()).contains(HIERARCHY_ACTIVITY);

        selection.revealDefault();

        assertThat(layout.current().isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR))
                .isTrue();

        projects.clear();

        assertThat(selection.current().selected()).isEmpty();
        assertThat(layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)).isFalse();
        assertThat(layout.current().isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR))
                .isFalse();

        selection.close();
        layout.close();
        host.close();
    }

    private static EditorExtension activityExtension() {
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.activity-containers";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                registerView(context, HIERARCHY_VIEW, "Hierarchy", 10);
                registerActivity(context, HIERARCHY_ACTIVITY, "Hierarchy", HIERARCHY_VIEW, 10);
                registerView(context, EXTENSIONS_VIEW, "Extensions", 20);
                registerActivity(context, EXTENSIONS_ACTIVITY, "Extensions", EXTENSIONS_VIEW, 20);
            }
        };
    }

    private static EditorExtension contextualActivityExtension() {
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.contextual-activity-containers";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                EditorContextCondition<Boolean> projectOpen =
                        EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN);
                context.subscriptions()
                        .add(context.views()
                                .register(new EditorViewContribution(
                                        new TestView(HIERARCHY_VIEW, "Hierarchy"),
                                        EditorViewContainers.PRIMARY_SIDEBAR,
                                        10,
                                        projectOpen)));
                context.subscriptions()
                        .add(context.activities()
                                .register(new EditorActivityContribution(
                                        HIERARCHY_ACTIVITY,
                                        "Hierarchy",
                                        new EditorIcon(EditorIcons.ENTITY, "Hierarchy"),
                                        HIERARCHY_VIEW,
                                        10,
                                        projectOpen)));
                registerView(context, EXTENSIONS_VIEW, "Extensions", 20);
                registerActivity(context, EXTENSIONS_ACTIVITY, "Extensions", EXTENSIONS_VIEW, 20);
            }
        };
    }

    private static void registerView(EditorExtensionContext context, ViewId id, String title, int order) {
        context.subscriptions()
                .add(context.views()
                        .register(new EditorViewContribution(
                                new TestView(id, title), EditorViewContainers.PRIMARY_SIDEBAR, order)));
    }

    private static void registerActivity(
            EditorExtensionContext context, ActivityId id, String title, ViewId view, int order) {
        context.subscriptions()
                .add(context.activities()
                        .register(new EditorActivityContribution(
                                id, title, new EditorIcon(EditorIcons.ENTITY, title), view, order)));
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }

    private static EditorHierarchyNode hierarchy() {
        EditorDetails details = new EditorDetails("World", "World", "world", "world", List.of(), List.of());
        EditorSelection selection = new EditorSelection(EditorSelectionKinds.WORLD, "world", details);
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "World",
                Optional.empty(),
                Optional.empty(),
                true,
                selection,
                List.of());
    }

    private record TestView(ViewId id, String title) implements EditorView {
        @Override
        public ViewKindId kind() {
            return new ViewKindId("io.github.glynch.test.activity-view");
        }
    }
}
