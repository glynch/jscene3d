/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.render.RenderSurface;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.RendererOptions;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Owns the renderer and the currently opened authored-world preview. */
final class EditorPreview implements AutoCloseable {
    private final Renderer renderer;

    private @Nullable EditorWorldPreview worldPreview;
    private long frameCount;
    private boolean closed;

    /** Creates an initially empty renderer for the editor viewport. */
    EditorPreview(RenderSurface surface) {
        renderer = Renderer.create(surface, RendererOptions.defaults());
    }

    /** Replaces the current preview with a safely composed view of the supplied project. */
    List<ProjectDiagnostic> show(EditorProjectSession session) {
        requireOpen();
        clearProject();
        EditorWorldPreviewLoadResult result = EditorWorldPreview.compose(session);
        worldPreview = result.preview().orElse(null);
        return result.diagnostics();
    }

    /** Removes the current authored-world preview while retaining the surface renderer. */
    void clearProject() {
        requireOpen();
        EditorWorldPreview current = worldPreview;
        worldPreview = null;
        if (current != null) {
            current.close();
        }
    }

    /** Renders the authored world, or a clear viewport before a project is available. */
    void render(RenderSurfaceSize size) {
        requireOpen();
        EditorWorldPreview current = worldPreview;
        if (current == null) {
            renderer.clear();
        } else {
            float aspectRatio = (float) size.framebufferWidth() / size.framebufferHeight();
            current.render(renderer, aspectRatio);
        }
        frameCount++;
    }

    /** Returns the number of completed JScene3D frames. */
    long frameCount() {
        return frameCount;
    }

    /** Releases renderer GPU resources before closing the authored world and its resources. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            renderer.close();
        } finally {
            EditorWorldPreview current = worldPreview;
            worldPreview = null;
            if (current != null) {
                current.close();
            }
            closed = true;
        }
    }

    /** Rejects operations after terminal renderer cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Editor preview is closed");
        }
    }
}
