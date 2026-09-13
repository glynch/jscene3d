/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jspecify.annotations.Nullable;

/** JavaFX adapter for the central editor-tab surface. */
public final class JavaFxEditorArea implements AutoCloseable {
    private final TabPane tabs = new TabPane();
    private final Map<Tab, Runnable> focusRequests = new IdentityHashMap<>();
    private final List<Runnable> selectionObservers = new ArrayList<>();
    private int focusSuppressionDepth;

    /** Creates an empty editor area. */
    public JavaFxEditorArea() {
        tabs.setAccessibleText("Editor tabs");
        tabs.setAccessibleHelp("Use Left and Right Arrow keys to switch editor tabs");
        tabs.getStyleClass().add(EditorStyleClasses.EDITOR_AREA_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            List.copyOf(selectionObservers).forEach(Runnable::run);
            if (focusSuppressionDepth == 0) {
                requestFocus(selected);
            }
        });
    }

    /** Returns the workbench-owned JavaFX node. */
    public TabPane node() {
        return tabs;
    }

    /** Observes selected-tab changes. */
    public EditorRegistration observeSelectionChanged(Runnable observer) {
        Runnable listener = Objects.requireNonNull(observer, "observer");
        selectionObservers.add(listener);
        return () -> selectionObservers.remove(listener);
    }

    void add(Tab tab, Runnable requestFocus) {
        register(tab, requestFocus);
        tabs.getTabs().add(tab);
    }

    void addFirst(Tab tab, Runnable requestFocus) {
        register(tab, requestFocus);
        tabs.getTabs().addFirst(tab);
    }

    void select(Tab tab) {
        if (tabs.getSelectionModel().getSelectedItem() == tab) {
            requestFocus(tab);
        } else {
            tabs.getSelectionModel().select(tab);
        }
    }

    void preview(Tab tab) {
        withoutFocusRequests(() -> tabs.getSelectionModel().select(tab));
    }

    void remove(Tab tab) {
        focusRequests.remove(tab);
        tabs.getTabs().remove(tab);
    }

    void removePreview(Tab tab) {
        withoutFocusRequests(() -> remove(tab));
    }

    boolean contains(Tab tab) {
        return tabs.getTabs().contains(tab);
    }

    @Nullable
    Tab selectedTab() {
        return tabs.getSelectionModel().getSelectedItem();
    }

    private void register(Tab tab, Runnable requestFocus) {
        Tab editorTab = Objects.requireNonNull(tab, "tab");
        focusRequests.put(editorTab, Objects.requireNonNull(requestFocus, "requestFocus"));
        editorTab.addEventHandler(Tab.CLOSED_EVENT, ignored -> focusRequests.remove(editorTab));
    }

    private void requestFocus(@Nullable Tab tab) {
        Runnable request = focusRequests.get(tab);
        if (request != null) {
            request.run();
        }
    }

    private void withoutFocusRequests(Runnable action) {
        focusSuppressionDepth++;
        try {
            action.run();
        } finally {
            focusSuppressionDepth--;
        }
    }

    @Override
    public void close() {
        selectionObservers.clear();
        focusRequests.clear();
        tabs.getTabs().clear();
    }
}
