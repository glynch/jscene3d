/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.dialog;

import io.github.glynch.jscene3d.editor.EditorTheme;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButton;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** JavaFX adapter for toolkit-independent application-modal dialog declarations. */
public final class JavaFxModalDialogs {
    private final Stage owner;

    /** Creates a modal-dialog adapter owned by one editor stage. */
    public JavaFxModalDialogs(Stage owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /** Presents one dialog and returns its selected logical action. */
    public Optional<EditorDialogButtonId> show(EditorDialog specification) {
        EditorDialog model = Objects.requireNonNull(specification, "specification");
        Dialog<EditorDialogButtonId> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(model.title());
        dialog.setHeaderText(model.heading());
        Label content = new Label(model.content());
        content.setWrapText(true);
        content.setMaxWidth(520.0);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(EditorTheme.stylesheet());

        Map<ButtonType, EditorDialogButtonId> actions = new LinkedHashMap<>();
        for (EditorDialogButton button : model.buttons()) {
            ButtonType type = new ButtonType(button.title(), buttonData(button.role()));
            dialog.getDialogPane().getButtonTypes().add(type);
            actions.put(type, button.id());
        }
        dialog.setResultConverter(actions::get);
        return dialog.showAndWait();
    }

    private static ButtonBar.ButtonData buttonData(EditorDialogButtonRole role) {
        return switch (role) {
            case DEFAULT -> ButtonBar.ButtonData.OK_DONE;
            case SECONDARY -> ButtonBar.ButtonData.OTHER;
            case DESTRUCTIVE -> ButtonBar.ButtonData.NO;
            case CANCEL -> ButtonBar.ButtonData.CANCEL_CLOSE;
        };
    }
}
