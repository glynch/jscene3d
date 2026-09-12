/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises Activity Bar contribution identities and metadata validation. */
final class EditorActivityContractTest {
    /** Retains one valid, namespaced activity contribution. */
    @Test
    void retainsValidContribution() {
        ActivityId id = new ActivityId("io.github.glynch.test.scene");
        EditorIcon icon = new EditorIcon(EditorIcons.SCENE, "Scene");
        ViewId view = new ViewId("io.github.glynch.test.scene-view");

        EditorActivityContribution contribution = new EditorActivityContribution(id, "Scene", icon, view, 20);

        assertThat(contribution.id()).isEqualTo(id);
        assertThat(contribution.title()).isEqualTo("Scene");
        assertThat(contribution.icon()).isEqualTo(icon);
        assertThat(contribution.views()).containsExactly(view);
        assertThat(contribution.order()).isEqualTo(20);
        assertThat(id).hasToString("io.github.glynch.test.scene");
    }

    /** Rejects invalid activity identities and blank user-facing titles. */
    @Test
    void rejectsInvalidContribution() {
        EditorIcon icon = new EditorIcon(EditorIcons.SCENE, "Scene");
        ViewId view = new ViewId("io.github.glynch.test.scene-view");
        ActivityId id = new ActivityId("io.github.glynch.test.scene");
        List<ViewId> noViews = List.of();
        List<ViewId> duplicateViews = List.of(view, view);

        assertThatThrownBy(() -> new ActivityId("scene"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("namespaced");
        assertThatThrownBy(() -> new EditorActivityContribution(id, " ", icon, view, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(() -> new EditorActivityContribution(id, "Scene", icon, noViews, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one view");
        assertThatThrownBy(() -> new EditorActivityContribution(id, "Scene", icon, duplicateViews, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate views");
    }
}
