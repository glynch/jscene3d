/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EditorContributionRegistryTest {
    private static final ViewId VIEW_ID = new ViewId("io.github.glynch.test.view");
    private static final ActivityId ACTIVITY_ID = new ActivityId("io.github.glynch.test.activity");

    @Test
    void publishesAvailableViewsAndActivities() {
        EditorContributionRegistry registry = new EditorContributionRegistry();
        List<List<EditorActivityContribution>> activitySnapshots = new ArrayList<>();
        registry.observeActivities(activitySnapshots::add);
        registry.register(new EditorViewContribution(new TestView(), EditorViewContainers.PRIMARY_SIDEBAR, 5));
        registry.register(activity("Test"));

        assertThat(activitySnapshots.getLast())
                .singleElement()
                .extracting(EditorActivityContribution::id)
                .isEqualTo(ACTIVITY_ID);

        registry.close();
        assertThat(activitySnapshots.getLast()).isEmpty();
    }

    @Test
    void rejectsActivityViewsOutsideThePrimarySidebar() {
        EditorContributionRegistry registry = new EditorContributionRegistry();
        registry.register(new EditorViewContribution(new TestView(), EditorViewContainers.BOTTOM_PANEL, 5));
        EditorActivityContribution misplaced = activity("Misplaced");

        assertThatThrownBy(() -> registry.register(misplaced))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary sidebar");
        registry.close();
    }

    private static EditorActivityContribution activity(String name) {
        return new EditorActivityContribution(
                ACTIVITY_ID, name, new EditorIcon(EditorIcons.ENTITY, name + " activity"), VIEW_ID, 5);
    }

    private static final class TestView implements EditorView {
        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Test";
        }

        @Override
        public ViewKindId kind() {
            return new ViewKindId("io.github.glynch.test.kind");
        }
    }
}
