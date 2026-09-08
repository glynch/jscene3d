/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.jspecify.annotations.Nullable;

/** Coordinates OpenGLFX callbacks, renderer lifecycle, and visible viewport status. */
final class ViewportController {
    private static final System.Logger LOGGER = System.getLogger(ViewportController.class.getName());

    private final GLCanvas canvas;
    private final ViewportInteraction interaction;
    private final Label status;
    private final Runnable disposalComplete;
    private final OpenGlFxRenderSurface surface = new OpenGlFxRenderSurface();

    private @Nullable EditorPreview preview;
    private @Nullable CheckBox animationControl;
    private boolean focused;
    private boolean disposed;

    /** Stores the JavaFX controls and state used by rendering callbacks. */
    ViewportController(GLCanvas canvas, ViewportInteraction interaction, Label status, Runnable disposalComplete) {
        this.canvas = canvas;
        this.interaction = interaction;
        this.status = status;
        this.disposalComplete = disposalComplete;
    }

    /** Renders one OpenGLFX frame through the actual JScene3D renderer. */
    void render(GLRenderEvent event) {
        try {
            surface.beginFrame(canvas, event);
            RenderSurfaceSize size = surface.size();
            if (!size.isDrawable()) {
                return;
            }
            EditorPreview currentPreview = preview;
            if (currentPreview == null) {
                currentPreview = new EditorPreview(surface);
                preview = currentPreview;
            }
            float elapsedSeconds = (float) Math.clamp(event.delta, 0.0, 0.1);
            currentPreview.render(elapsedSeconds, size, interaction.consume());
            if (currentPreview.frameCount() % 30L == 0L) {
                updateStatus(event, size, currentPreview.frameCount());
            }
        } catch (RuntimeException exception) {
            canvas.setFps(0.0);
            LOGGER.log(System.Logger.Level.ERROR, "Editor viewport rendering failed", exception);
            updateStatus("Rendering failed: " + exception.getMessage());
        }
    }

    /** Releases renderer resources from OpenGLFX's context-owning disposal callback. */
    void dispose() {
        if (disposed) {
            return;
        }
        try {
            EditorPreview currentPreview = preview;
            if (currentPreview != null) {
                currentPreview.close();
            }
            LOGGER.log(System.Logger.Level.INFO, "Disposed editor viewport and JScene3D renderer");
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Editor viewport disposal failed", exception);
        } finally {
            disposed = true;
            Platform.runLater(disposalComplete);
        }
    }

    /** Stores the toolbar animation control after the shell creates it. */
    void setAnimationControl(CheckBox animationControl) {
        this.animationControl = animationControl;
    }

    /** Reflects a keyboard animation toggle in the toolbar control. */
    void synchronizeAnimationControl() {
        CheckBox currentControl = animationControl;
        if (currentControl != null) {
            currentControl.setSelected(interaction.isSpinning());
        }
    }

    /** Records JavaFX focus changes for the next visible status update. */
    void setFocused(boolean focused) {
        this.focused = focused;
    }

    /** Formats physical, logical, frame-rate, focus, and frame-count diagnostics. */
    private void updateStatus(GLRenderEvent event, RenderSurfaceSize size, long frameCount) {
        String text = "JScene3D frames: "
                + frameCount
                + "   JavaFX: "
                + size.logicalWidth()
                + "×"
                + size.logicalHeight()
                + " logical   OpenGL: "
                + size.framebufferWidth()
                + "×"
                + size.framebufferHeight()
                + " pixels   FPS: "
                + event.fps
                + "   Focus: "
                + (focused ? "viewport" : "editor controls");
        updateStatus(text);
    }

    /** Applies a status update on the JavaFX Application Thread. */
    private void updateStatus(String text) {
        if (Platform.isFxApplicationThread()) {
            status.setText(text);
        } else {
            Platform.runLater(() -> status.setText(text));
        }
    }
}
