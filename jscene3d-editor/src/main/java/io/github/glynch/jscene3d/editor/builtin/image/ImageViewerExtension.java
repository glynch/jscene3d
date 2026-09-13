/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.image;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorFileTypeId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Built-in extension associating common raster images with the image viewer. */
public final class ImageViewerExtension implements EditorExtension {
    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.image-viewer";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(), "Image Viewer", "Previews PNG and JPEG workspace images.", "JScene3D", Optional.empty(), true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.fileTypes()
                        .register(new EditorFileType(
                                new EditorFileTypeId("io.github.glynch.jscene3d.editor.file-type.raster-image"),
                                "Image",
                                EditorFileKind.IMAGE,
                                Optional.empty(),
                                new EditorIcon(EditorIcons.IMAGE, "Image"),
                                Set.of(),
                                Set.of("png", "jpg", "jpeg"),
                                0)));
    }
}
