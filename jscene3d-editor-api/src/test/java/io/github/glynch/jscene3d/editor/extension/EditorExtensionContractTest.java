/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.activity.EditorActivityRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import io.github.glynch.jscene3d.editor.menu.EditorMenuRegistry;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EditorExtensionContractTest {
    @Test
    void extensionContributesLogicalViewsThroughTheBoundedContext() {
        List<EditorViewContribution> contributions = new ArrayList<>();
        EditorViewRegistry views = contribution -> {
            contributions.add(contribution);
            return () -> contributions.remove(contribution);
        };
        EditorExtensionContext context = new TestContext(views);
        EditorExtension extension = new TestExtension();

        extension.activate(context);

        assertThat(extension.id()).isEqualTo("io.github.glynch.test-editor");
        assertThat(contributions).singleElement().satisfies(contribution -> {
            assertThat(contribution.view().id().value()).isEqualTo("io.github.glynch.test-editor.outline");
            assertThat(contribution.view().title()).isEqualTo("Outline");
            assertThat(contribution.container().value()).isEqualTo("io.github.glynch.jscene3d.editor.primary-sidebar");
            assertThat(contribution.order()).isEqualTo(20);
        });
    }

    private static final class TestExtension implements EditorExtension {
        @Override
        public String id() {
            return "io.github.glynch.test-editor";
        }

        @Override
        public void activate(EditorExtensionContext context) {
            context.views()
                    .register(new EditorViewContribution(
                            new TestView(),
                            new ViewContainerId("io.github.glynch.jscene3d.editor.primary-sidebar"),
                            20));
        }
    }

    private static final class TestView implements EditorView {
        @Override
        public ViewId id() {
            return new ViewId("io.github.glynch.test-editor.outline");
        }

        @Override
        public String title() {
            return "Outline";
        }

        @Override
        public ViewKindId kind() {
            return new ViewKindId("io.github.glynch.test-editor.outline-kind");
        }
    }

    private record TestContext(EditorViewRegistry views) implements EditorExtensionContext {
        @Override
        public EditorActivityRegistry activities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorExtensions extensions() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorCommandRegistry commands() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorCommandPlacementRegistry commandPlacements() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorMenuRegistry menus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorConfiguration configuration() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorStatusBar statusBar() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorWindow window() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorDiagnostics diagnostics() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorProjects projects() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EditorSelections selections() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ExtensionSubscriptions subscriptions() {
            throw new UnsupportedOperationException();
        }
    }
}
