/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.Nullable;

/** Reusable hosted lifecycle for a perspective scene with optional orbit controls and overlays. */
public final class SceneExample implements HostedExample {
    private final ExampleContext context;
    private final Scene scene;
    private final PerspectiveCamera camera;
    private final @Nullable OrbitControls controls;
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final List<Overlay> overlays = new ArrayList<>();

    private FrameAction frameAction = (example, frame) -> {
        // Static scenes intentionally require no example-specific update.
    };
    private BooleanSupplier pointerCapture = () -> false;

    /**
     * Retains the scene and camera without enabling camera controls.
     *
     * @param context shared host context
     * @param scene scene to render
     * @param camera perspective camera to render through
     * @throws NullPointerException if an argument is {@code null}
     */
    public SceneExample(ExampleContext context, Scene scene, PerspectiveCamera camera) {
        this(context, scene, camera, null);
    }

    /**
     * Retains the scene, camera, and optional orbit controls.
     *
     * @param context shared host context
     * @param scene scene to render
     * @param camera perspective camera to render through
     * @param controls optional orbit controls
     * @throws NullPointerException if {@code context}, {@code scene}, or {@code camera} is null
     */
    public SceneExample(
            ExampleContext context, Scene scene, PerspectiveCamera camera, @Nullable OrbitControls controls) {
        this.context = Objects.requireNonNull(context, "context");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.controls = controls;
    }

    /**
     * Returns the retained scene for example-specific setup.
     *
     * @return retained scene
     */
    public Scene scene() {
        return scene;
    }

    /**
     * Returns the retained camera for example-specific setup.
     *
     * @return retained camera
     */
    public PerspectiveCamera camera() {
        return camera;
    }

    /**
     * Returns the retained controls or fails if this example has none.
     *
     * @return retained orbit controls
     * @throws IllegalStateException if this example has no orbit controls
     */
    public OrbitControls controls() {
        if (controls == null) {
            throw new IllegalStateException("This scene example has no orbit controls");
        }
        return controls;
    }

    /**
     * Adds an owned resource that will be closed in reverse registration order.
     *
     * @param resource resource to own
     * @param <T> resource type
     * @return the supplied resource for inline assignment
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    public <T extends AutoCloseable> T own(T resource) {
        T validResource = Objects.requireNonNull(resource, "resource");
        resources.add(validResource);
        return validResource;
    }

    /**
     * Adds an overlay painted after the scene in registration order.
     *
     * @param overlay overlay to retain
     * @param <T> overlay type
     * @return the supplied overlay for inline assignment
     * @throws NullPointerException if {@code overlay} is {@code null}
     */
    public <T extends Overlay> T addOverlay(T overlay) {
        T validOverlay = Objects.requireNonNull(overlay, "overlay");
        overlays.add(validOverlay);
        return validOverlay;
    }

    /**
     * Replaces the example-specific per-frame action.
     *
     * @param frameAction action invoked once per frame
     * @throws NullPointerException if {@code frameAction} is {@code null}
     */
    public void setFrameAction(FrameAction frameAction) {
        this.frameAction = Objects.requireNonNull(frameAction, "frameAction");
    }

    /**
     * Adds an example-local pointer-capture source such as a control panel.
     *
     * @param pointerCapture supplier reporting whether local UI owns pointer input
     * @throws NullPointerException if {@code pointerCapture} is {@code null}
     */
    public void setPointerCapture(BooleanSupplier pointerCapture) {
        this.pointerCapture = Objects.requireNonNull(pointerCapture, "pointerCapture");
    }

    /** Synchronizes camera projection and control scaling with the assigned content viewport. */
    @Override
    public void resize() {
        camera.setAspectRatio(context.aspectRatio());
        if (controls != null) {
            controls.setViewportSize(context.logicalWidth(), context.logicalHeight());
        }
    }

    /**
     * Advances custom state followed by camera controls when present.
     *
     * @param frame immutable current-frame state
     * @throws NullPointerException if {@code frame} is {@code null}
     */
    @Override
    public void update(ExampleFrame frame) {
        ExampleFrame validFrame = Objects.requireNonNull(frame, "frame");
        frameAction.update(this, validFrame);
        if (controls != null) {
            boolean pointerCaptured = validFrame.pointerCaptured() || pointerCapture.getAsBoolean();
            if (pointerCaptured && validFrame.keyboardCaptured()) {
                controls.updateWithoutUserInput(validFrame.elapsedSeconds());
            } else if (pointerCaptured) {
                controls.updateWithoutPointerInput(validFrame.elapsedSeconds());
            } else if (validFrame.keyboardCaptured()) {
                controls.updateWithoutKeyboardInput(validFrame.elapsedSeconds());
            } else {
                controls.update(validFrame.elapsedSeconds());
            }
        }
    }

    /** Renders the scene followed by every registered overlay. */
    @Override
    public void render() {
        context.renderer().render(scene, camera);
        for (Overlay overlay : overlays) {
            context.renderer().render(overlay);
        }
    }

    /** Renders only the representative scene, without example control overlays. */
    @Override
    public void renderThumbnail() {
        context.renderer().render(scene, camera);
    }

    /** Closes every owned resource in reverse registration order. */
    @Override
    public void close() {
        RuntimeException failure = null;
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close example resource", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        resources.clear();
        if (failure != null) {
            throw failure;
        }
    }

    /** Example-specific state update invoked once before camera controls. */
    @FunctionalInterface
    public interface FrameAction {
        /**
         * Advances retained state for one host frame.
         *
         * @param example scene example being advanced
         * @param frame immutable current-frame state
         */
        void update(SceneExample example, ExampleFrame frame);
    }
}
