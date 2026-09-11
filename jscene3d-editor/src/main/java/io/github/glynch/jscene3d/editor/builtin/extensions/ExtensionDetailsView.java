/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.extensions;

import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorDetailsDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Projects the selected extension into the standard toolkit-independent details model. */
final class ExtensionDetailsView implements EditorDetailsView {
    private final ExtensionSelection selection;

    ExtensionDetailsView(ExtensionSelection selection) {
        this.selection = Objects.requireNonNull(selection, "selection");
    }

    @Override
    public ViewId id() {
        return ExtensionsExtension.DETAILS_VIEW_ID;
    }

    @Override
    public String title() {
        return "Extension Details";
    }

    @Override
    public EditorDetailsDataProvider dataProvider() {
        return new EditorDetailsDataProvider() {
            @Override
            public Optional<EditorDetails> details() {
                return selection.current().map(ExtensionDetailsView::detailsOf);
            }

            @Override
            public EditorRegistration observe(Consumer<Optional<EditorDetails>> listener) {
                Consumer<Optional<EditorDetails>> observer = Objects.requireNonNull(listener, "listener");
                return selection.observe(selected -> observer.accept(selected.map(ExtensionDetailsView::detailsOf)));
            }
        };
    }

    @Override
    public String emptyMessage() {
        return "Select an installed extension to inspect its metadata.";
    }

    private static EditorDetails detailsOf(EditorExtensionDescriptor descriptor) {
        List<EditorDetails.Property> properties = List.of(
                property("identifier", "Identifier", descriptor.id()),
                property("publisher", "Publisher", descriptor.publisher()),
                property("version", "Version", descriptor.version().orElse("Bundled with this editor")),
                property("installation", "Installation", descriptor.builtIn() ? "Built in" : "Installed"));
        EditorDetails.Section section =
                new EditorDetails.Section("Extension", Optional.of(descriptor.description()), true, properties);
        return new EditorDetails(
                descriptor.displayName(),
                descriptor.builtIn() ? "Built-in extension" : "Editor extension",
                descriptor.publisher(),
                descriptor.id(),
                List.of(new EditorIcon(EditorIcons.EXTENSIONS, "Editor extension")),
                List.of(section));
    }

    private static EditorDetails.Property property(String identity, String displayName, String value) {
        return new EditorDetails.Property(
                identity,
                displayName,
                "text",
                value,
                EditorDetails.ValueOrigin.AUTHORED,
                true,
                Optional.empty(),
                Map.of());
    }
}
