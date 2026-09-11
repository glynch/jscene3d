/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.extensions;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Objects;
import java.util.Optional;

/** Bundled extension which exposes the installed-extension browser and detail editor. */
public final class ExtensionsExtension implements EditorExtension {
    /** Stable identity of the installed-extensions browser. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.extensions");

    /** Stable identity of the extension-detail editor. */
    public static final ViewId DETAILS_VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.extension-details");

    private static final ActivityId ACTIVITY_ID =
            new ActivityId("io.github.glynch.jscene3d.editor.extensions-activity");

    /** Creates the bundled Extensions extension. */
    public ExtensionsExtension() {
        super();
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.extensions";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Extensions",
                "Browses extensions activated in the current JScene3D editor window.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        ExtensionSelection selection = new ExtensionSelection(editor.window());
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new ExtensionsView(editor.extensions(), selection),
                                EditorViewContainers.PRIMARY_SIDEBAR,
                                90)));
        editor.subscriptions()
                .add(editor.activities()
                        .register(new EditorActivityContribution(
                                ACTIVITY_ID,
                                "Extensions",
                                new EditorIcon(EditorIcons.EXTENSIONS, "Extensions"),
                                VIEW_ID,
                                90)));
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new ExtensionDetailsView(selection), EditorViewContainers.EDITOR_AREA, 90)));
    }
}
