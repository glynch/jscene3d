/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

final class ViewIdentityTest {
    @Test
    void preservesPortableNamespacedViewAndContainerIdentities() {
        ViewId view = new ViewId("io.github.glynch.jscene3d.editor.project-browser");
        ViewContainerId container = new ViewContainerId("io.github.glynch.jscene3d.editor.bottom-panel");
        ViewKindId kind = new ViewKindId("io.github.glynch.jscene3d.editor.tree");

        assertThat(view.value()).isEqualTo("io.github.glynch.jscene3d.editor.project-browser");
        assertThat(view).hasToString(view.value());
        assertThat(container.value()).isEqualTo("io.github.glynch.jscene3d.editor.bottom-panel");
        assertThat(container).hasToString(container.value());
        assertThat(kind.value()).isEqualTo("io.github.glynch.jscene3d.editor.tree");
        assertThat(kind).hasToString(kind.value());
        assertThat(EditorViewContainers.BOTTOM_PANEL).isEqualTo(container);
        assertThat(EditorViewContainers.PRIMARY_SIDEBAR.value())
                .isEqualTo("io.github.glynch.jscene3d.editor.primary-sidebar");
        assertThat(EditorViewContainers.EDITOR_AREA.value()).isEqualTo("io.github.glynch.jscene3d.editor.editor-area");
        assertThat(EditorViewContainers.SECONDARY_SIDEBAR.value())
                .isEqualTo("io.github.glynch.jscene3d.editor.secondary-sidebar");
    }

    @Test
    void rejectsMalformedViewIdentities() {
        assertRejected(ViewId::new);
    }

    @Test
    void rejectsMalformedContainerIdentities() {
        assertRejected(ViewContainerId::new);
    }

    @Test
    void rejectsMalformedViewKindIdentities() {
        assertRejected(ViewKindId::new);
    }

    @SuppressWarnings("NullAway") // Deliberate null verifies public boundary validation.
    private static <T> void assertRejected(Function<String, T> constructor) {
        assertThatNullPointerException()
                .isThrownBy(() -> constructor.apply(null))
                .withMessage("value");
        for (String invalid : List.of("", "local", ".leading", "trailing.", "two..dots", "Upper.case", "has space")) {
            assertThatThrownBy(() -> constructor.apply(invalid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("value must be a lowercase dotted namespaced identity");
        }
    }
}
