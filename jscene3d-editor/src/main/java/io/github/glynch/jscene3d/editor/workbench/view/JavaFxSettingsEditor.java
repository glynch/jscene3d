/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.appearance.JavaFxAppearanceSettingsPane;
import io.github.glynch.jscene3d.editor.workbench.configuration.JavaFxProjectSettingsPane;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jspecify.annotations.Nullable;

/** Owns the Settings editor and its appearance/project sections. */
public final class JavaFxSettingsEditor implements AutoCloseable {
    private final JavaFxEditorArea area;
    private final EditorExtensionHost extensions;
    private @Nullable OpenSettings current;

    /** Creates a Settings-editor owner. */
    public JavaFxSettingsEditor(JavaFxEditorArea area, EditorExtensionHost extensions) {
        this.area = Objects.requireNonNull(area, "area");
        this.extensions = Objects.requireNonNull(extensions, "extensions");
    }

    /** Opens or reveals user appearance and optional project settings. */
    public void show(@Nullable EditorProjectSession session, Consumer<EditorMessage> messages) {
        OpenSettings settings = current;
        if (settings == null) {
            settings = create(session, Objects.requireNonNull(messages, "messages"));
            current = settings;
            OpenSettings opened = settings;
            settings.tab().setOnClosed(ignored -> remove(opened));
            area.add(settings.tab(), () -> {});
        }
        area.select(settings.tab());
    }

    private OpenSettings create(@Nullable EditorProjectSession session, Consumer<EditorMessage> messages) {
        JavaFxAppearanceSettingsPane appearance = new JavaFxAppearanceSettingsPane(extensions.colorThemes());
        TabPane sections = new TabPane();
        sections.getTabs().add(fixedTab("Appearance", appearance.node()));
        if (session != null) {
            sections.getTabs().add(fixedTab("Project", new JavaFxProjectSettingsPane(session, messages).node()));
        }
        Label title = new Label("Settings");
        title.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
        Tab tab = new Tab();
        tab.setGraphic(title);
        tab.setContent(sections);
        tab.setClosable(true);
        return new OpenSettings(tab, appearance);
    }

    private static Tab fixedTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void remove(OpenSettings settings) {
        if (current == settings) {
            current = null;
            settings.close();
        }
    }

    @Override
    public void close() {
        OpenSettings settings = current;
        current = null;
        if (settings != null) {
            area.remove(settings.tab());
            settings.close();
        }
    }

    private record OpenSettings(Tab tab, JavaFxAppearanceSettingsPane appearance) {
        private OpenSettings {
            Objects.requireNonNull(tab, "tab");
            Objects.requireNonNull(appearance, "appearance");
        }

        private void close() {
            appearance.close();
        }
    }
}
