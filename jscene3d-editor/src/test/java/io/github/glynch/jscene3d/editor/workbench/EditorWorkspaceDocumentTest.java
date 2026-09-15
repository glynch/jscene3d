/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.application.EditorBuildInfo;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.build.preference.InMemoryWorkspaceBuildPreferences;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusBarPane;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxEditorArea;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

/** Exercises mutually exclusive Welcome and project document presentation. */
final class EditorWorkspaceDocumentTest {
    /** Switches between startup, Welcome, and opening-project editor tabs. */
    @Test
    void presentsStartupDocuments() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorSelectionContext selections = new EditorSelectionContext();
        EditorConfigurationContext configuration = new EditorConfigurationContext();
        AtomicBoolean openProjectRequested = new AtomicBoolean();
        StackPane previewContent = new StackPane();
        JavaFxIconRenderer icons = JavaFxIconRenderer.builtIn();
        try (EditorExtensionHost extensions = new EditorExtensionHost(projects, selections, configuration);
                JavaFxEditorArea editorArea = new JavaFxEditorArea();
                EditorStatusBarPane statusBar = new EditorStatusBarPane(extensions, icons);
                EditorWorkspaceDocument documents = new EditorWorkspaceDocument(
                        new EditorWorkspaceDocument.Context(
                                extensions,
                                selections,
                                editorArea,
                                icons,
                                previewContent,
                                new Label("No project"),
                                statusBar),
                        new EditorWorkspaceDocument.Actions(
                                () -> openProjectRequested.set(true),
                                () -> {},
                                new InMemoryWorkspaceBuildPreferences(),
                                EditorBuildInfo.current(),
                                ForkJoinPool.commonPool()))) {
            Tab preview = editorArea.node().getTabs().getFirst();
            assertThat(preview.getContent()).isSameAs(previewContent);
            assertThat(preview.getText()).isEqualTo("Empty Preview");

            documents.showWelcome();
            assertThat(editorArea.node().getTabs())
                    .singleElement()
                    .extracting(Tab::getText)
                    .isEqualTo("Welcome");
            Tab welcome = editorArea.node().getTabs().getFirst();
            Button openProject =
                    (Button) findByStyleClass(welcome.getContent(), EditorStyleClasses.EDITOR_WELCOME_OPEN_PROJECT)
                            .orElseThrow();
            openProject.fire();
            assertThat(openProjectRequested).isTrue();

            documents.beginOpening(Path.of("/example/Doomed Corridors"));
            assertThat(editorArea.node().getTabs()).hasSize(2);
            assertThat(editorArea.node().getSelectionModel().getSelectedItem().getContent())
                    .isSameAs(previewContent);

            documents.showWelcome();
            assertThat(editorArea.node().getTabs())
                    .singleElement()
                    .extracting(Tab::getText)
                    .isEqualTo("Welcome");
        } finally {
            configuration.close();
        }
    }

    private static Optional<Node> findByStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return Optional.of(node);
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .map(child -> findByStyleClass(child, styleClass))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        return Optional.empty();
    }
}
