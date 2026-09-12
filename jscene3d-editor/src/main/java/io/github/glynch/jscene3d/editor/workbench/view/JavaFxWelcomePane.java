/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.EditorBrandMark;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Presents the useful no-project starting point within the editor area. */
final class JavaFxWelcomePane extends StackPane {
    private static final double MARK_SIZE = 72.0;

    JavaFxWelcomePane(JavaFxIconRenderer icons, Runnable openProject) {
        JavaFxIconRenderer renderer = Objects.requireNonNull(icons, "icons");
        Runnable openProjectAction = Objects.requireNonNull(openProject, "openProject");

        EditorBrandMark mark = new EditorBrandMark(MARK_SIZE);
        mark.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_MARK);

        Label title = new Label("JScene3D Editor");
        title.setAccessibleText("JScene3D Editor");
        title.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_TITLE);
        Label subtitle = new Label("Build interactive 3D worlds in Java");
        subtitle.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_SUBTITLE);
        VBox product = new VBox(4.0, title, subtitle);
        product.setAlignment(Pos.CENTER_LEFT);
        HBox identity = new HBox(18.0, mark, product);
        identity.setAlignment(Pos.CENTER_LEFT);
        identity.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_IDENTITY);

        Label start = new Label("Start");
        start.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_SECTION_TITLE);
        Button openProjectButton = new Button("Open Project…");
        openProjectButton.setGraphic(renderer.create(
                new EditorIcon(EditorIcons.PROJECT, "Open Project"), EditorStyleClasses.EDITOR_WELCOME_ACTION_ICON));
        openProjectButton.setOnAction(ignored -> openProjectAction.run());
        openProjectButton.setAccessibleText("Open Project");
        openProjectButton.setAccessibleHelp("Choose a JScene3D project directory to open");
        openProjectButton
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_WELCOME_ACTION, EditorStyleClasses.EDITOR_WELCOME_OPEN_PROJECT);

        VBox content = new VBox(28.0, identity, new VBox(8.0, start, openProjectButton));
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME_CONTENT);

        getChildren().add(content);
        getStyleClass().add(EditorStyleClasses.EDITOR_WELCOME);
    }
}
