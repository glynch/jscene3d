/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.preview;

import com.huskerdev.grapl.gl.GLProfile;
import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.lwjgl.LWJGLExecutor;
import java.util.Objects;

/** Creates and connects the OpenGLFX canvas used by the editor viewport. */
public final class OpenGlFxViewport {
    private OpenGlFxViewport() {}

    /** Returns a core-profile canvas configured for the renderer baseline. */
    public static GLCanvas createCanvas() {
        GLCanvas.Builder.ContextDescription.New context = new GLCanvas.Builder.ContextDescription.New()
                .setProfile(GLProfile.CORE)
                .setMajorVersion(3)
                .setMinorVersion(3);
        GLCanvas result = new GLCanvas.Builder()
                .setExecutor(LWJGLExecutor.LWJGL_MODULE)
                .setContextDescription(context)
                .setFlipY(false)
                .setMSAA(0)
                .setSwapBuffers(2)
                .setFps(60.0)
                .build();
        result.setMinSize(1.0, 1.0);
        result.setPrefSize(800.0, 600.0);
        result.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        result.setFocusTraversable(true);
        return result;
    }

    /** Connects rendering, disposal, and focus events to one viewport controller. */
    public static void installEvents(GLCanvas canvas, ViewportController controller) {
        GLCanvas viewport = Objects.requireNonNull(canvas, "canvas");
        ViewportController target = Objects.requireNonNull(controller, "controller");
        viewport.addOnRenderEvent(target::render);
        viewport.addOnDisposeEvent(ignored -> target.dispose());
        viewport.focusedProperty().addListener((ignored, oldValue, focused) -> target.setFocused(focused));
        viewport.setOnMousePressed(ignored -> viewport.requestFocus());
    }
}
