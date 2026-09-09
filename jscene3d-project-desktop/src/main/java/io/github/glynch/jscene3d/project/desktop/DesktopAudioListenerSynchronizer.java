/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Derives a backend-independent audio-listener pose from the active camera transform. */
final class DesktopAudioListenerSynchronizer {
    private DesktopAudioListenerSynchronizer() {}

    /** Updates one listener from the supplied camera's current world transform. */
    static void synchronize(Transform3d camera, Listener listener) {
        Matrix4fc world = Objects.requireNonNull(camera, "camera").worldMatrix();
        Vector3f position = world.getTranslation(new Vector3f());
        Vector3f forward =
                world.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F)).normalize();
        Vector3f up = world.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        Objects.requireNonNull(listener, "listener").setTransform(position, forward, up);
    }

    /** Receives one camera-compatible world-space listener pose. */
    @FunctionalInterface
    interface Listener {
        /** Applies one position, forward direction, and up direction. */
        void setTransform(Vector3fc position, Vector3fc forward, Vector3fc up);
    }
}
