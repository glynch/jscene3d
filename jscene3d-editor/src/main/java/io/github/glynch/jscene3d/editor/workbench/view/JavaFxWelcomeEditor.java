/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import java.util.Objects;
import javafx.scene.control.Tab;
import org.jspecify.annotations.Nullable;

/** Owns the no-project Welcome editor tab. */
public final class JavaFxWelcomeEditor implements AutoCloseable {
    private final JavaFxEditorArea area;
    private final JavaFxIconRenderer icons;
    private final Runnable openProject;
    private @Nullable Tab tab;

    /** Creates a Welcome-editor owner. */
    public JavaFxWelcomeEditor(JavaFxEditorArea area, JavaFxIconRenderer icons, Runnable openProject) {
        this.area = Objects.requireNonNull(area, "area");
        this.icons = Objects.requireNonNull(icons, "icons");
        this.openProject = Objects.requireNonNull(openProject, "openProject");
    }

    /** Opens or reveals the Welcome editor. */
    public void show() {
        Tab current = tab;
        if (current == null) {
            current = new Tab("Welcome", new JavaFxWelcomePane(icons, openProject));
            current.setClosable(true);
            Tab opened = current;
            current.setOnClosed(ignored -> {
                if (tab == opened) {
                    tab = null;
                }
            });
            tab = current;
            area.addFirst(current, () -> {});
        }
        area.select(current);
    }

    @Override
    public void close() {
        Tab current = tab;
        tab = null;
        if (current != null) {
            area.remove(current);
        }
    }
}
