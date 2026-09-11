/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.inspector;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorDetailsDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Built-in extension which presents details supplied by the shared editor selection. */
public final class InspectorExtension implements EditorExtension {
    /** Stable identity of the built-in Inspector view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.inspector");

    /** Creates the built-in Inspector extension. */
    public InspectorExtension() {
        super();
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.inspector";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new InspectorDetailsView(editor.selections()),
                                EditorViewContainers.SECONDARY_SIDEBAR,
                                10)));
    }

    private record InspectorDetailsView(EditorSelections selections) implements EditorDetailsView {
        private InspectorDetailsView {
            Objects.requireNonNull(selections, "selections");
        }

        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Inspector";
        }

        @Override
        public EditorDetailsDataProvider dataProvider() {
            return new SelectionDetailsProvider(selections);
        }

        @Override
        public String emptyMessage() {
            return "Select an authored entity or asset to inspect it.";
        }
    }

    private record SelectionDetailsProvider(EditorSelections selections) implements EditorDetailsDataProvider {
        @Override
        public Optional<EditorDetails> details() {
            return selections.current().flatMap(selection -> selection.details());
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<EditorDetails>> listener) {
            Consumer<Optional<EditorDetails>> observer = Objects.requireNonNull(listener, "listener");
            return selections.observe(selection -> observer.accept(selection.flatMap(value -> value.details())));
        }
    }
}
