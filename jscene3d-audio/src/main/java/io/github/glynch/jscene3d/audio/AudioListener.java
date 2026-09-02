/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import io.github.glynch.jscene3d.audio.internal.Preconditions;
import java.nio.FloatBuffer;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryStack;

/**
 * The single listener associated with an {@link AudioEngine}.
 *
 * <p>Applications normally update the listener after updating the active camera. Position,
 * forward, and up must use the same coordinate system as positional audio sources.
 */
public final class AudioListener {
    private final AudioEngine engine;

    /** Creates the single listener facade owned by an engine. */
    AudioListener(AudioEngine engine) {
        this.engine = engine;
    }

    /**
     * Sets listener position and orientation together from an active camera transform.
     *
     * @param position listener world position
     * @param forward non-zero direction the listener faces
     * @param up non-zero direction above the listener, not parallel to {@code forward}
     */
    public void setTransform(Vector3fc position, Vector3fc forward, Vector3fc up) {
        Vector3f validPosition = Preconditions.requireFinite(position, "position");
        Vector3f validForward = Preconditions.requireDirection(forward, "forward");
        Vector3f validUp = Preconditions.requireDirection(up, "up");
        if (new Vector3f(validForward).cross(validUp).lengthSquared() < 1.0E-8F) {
            throw new IllegalArgumentException("forward and up must not be parallel");
        }
        engine.useListener(() -> applyTransform(validPosition, validForward, validUp), "change listener transform");
    }

    /**
     * Sets listener velocity used by OpenAL Doppler calculations.
     *
     * @param value finite world-space velocity
     */
    public void setVelocity(Vector3fc value) {
        Vector3f velocity = Preconditions.requireFinite(value, "value");
        engine.useListener(
                () -> AL10.alListener3f(AL10.AL_VELOCITY, velocity.x, velocity.y, velocity.z),
                "change listener velocity");
    }

    /** Uploads a validated camera-compatible transform to OpenAL. */
    private static void applyTransform(Vector3fc position, Vector3fc forward, Vector3fc up) {
        AL10.alListener3f(AL10.AL_POSITION, position.x(), position.y(), position.z());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer orientation = stack.mallocFloat(6);
            orientation.put(forward.x()).put(forward.y()).put(forward.z());
            orientation.put(up.x()).put(up.y()).put(up.z()).flip();
            AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
        }
    }
}
