/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.dialog;

import io.github.glynch.jscene3d.editor.EditorTheme;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButton;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonBehavior;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonRole;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** JavaFX adapter for toolkit-independent application-modal dialog declarations. */
public final class JavaFxModalDialogs {
    static final double CARD_WIDTH = 340.0;
    static final StageStyle WINDOW_STYLE = StageStyle.TRANSPARENT;

    private final Stage owner;

    /** Creates a modal-dialog adapter owned by one editor stage. */
    public JavaFxModalDialogs(Stage owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /** Presents one dialog and returns its selected logical action. */
    public Optional<EditorDialogButtonId> show(EditorDialog specification) {
        EditorDialog model = Objects.requireNonNull(specification, "specification");
        Stage dialog = new Stage(WINDOW_STYLE);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(model.title());
        dialog.setResizable(false);

        AtomicReference<EditorDialogButtonId> result = new AtomicReference<>();
        VBox card = createCard(dialog, model, result);
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(14.0));
        root.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_ROOT);
        root.setAccessibleRole(AccessibleRole.DIALOG);
        root.setAccessibleText(model.title());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(EditorTheme.stylesheet());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> dismissOnEscape(dialog, event));
        dialog.setScene(scene);
        dialog.setOnShown(ignored -> {
            centerOnOwner(dialog);
            Platform.runLater(() -> requestDefaultButtonFocus(card));
        });
        dialog.showAndWait();
        return Optional.ofNullable(result.get());
    }

    private static VBox createCard(Stage dialog, EditorDialog model, AtomicReference<EditorDialogButtonId> result) {
        Label heading = new Label(model.heading());
        heading.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_HEADING);

        Label content = new Label(model.content());
        content.setWrapText(true);
        content.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_CONTENT);

        HBox actions = new HBox(10.0);
        actions.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_ACTIONS);
        for (EditorDialogButton declaration : model.buttons()) {
            actions.getChildren().add(createButton(dialog, model, declaration, result));
        }

        VBox card = new VBox(18.0, heading, content, actions);
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL);
        return card;
    }

    private static Button createButton(
            Stage dialog,
            EditorDialog model,
            EditorDialogButton declaration,
            AtomicReference<EditorDialogButtonId> result) {
        Button button = new Button(declaration.title());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setDefaultButton(declaration.role() == EditorDialogButtonRole.DEFAULT);
        button.setCancelButton(declaration.role() == EditorDialogButtonRole.CANCEL);
        button.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_BUTTON);
        HBox.setHgrow(button, Priority.ALWAYS);
        if (declaration.behavior() == EditorDialogButtonBehavior.COPY_CONTENT) {
            button.getStyleClass().add(EditorStyleClasses.EDITOR_MODAL_COPY_BUTTON);
            button.setOnAction(ignored -> copy(model.content()));
        } else {
            button.setOnAction(ignored -> {
                result.set(declaration.id());
                dialog.close();
            });
        }
        return button;
    }

    private static void copy(String text) {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(text);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    private static void dismissOnEscape(Stage dialog, KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            dialog.close();
            event.consume();
        }
    }

    private void centerOnOwner(Stage dialog) {
        dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2.0);
        dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2.0);
    }

    private static void requestDefaultButtonFocus(VBox card) {
        card.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(Button::isDefaultButton)
                .findFirst()
                .ifPresent(Button::requestFocus);
    }
}
