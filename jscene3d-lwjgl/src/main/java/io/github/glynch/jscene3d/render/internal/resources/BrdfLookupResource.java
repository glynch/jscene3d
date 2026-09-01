/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.GL_RG16F;

import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

/** Shared context-local split-sum GGX BRDF integration lookup. */
public final class BrdfLookupResource implements AutoCloseable {
    private static final int SIZE = 128;
    private static final int SAMPLE_COUNT = 128;
    private final int texture;

    /** Computes and uploads the deterministic two-channel integration lookup. */
    public BrdfLookupResource() {
        float[] pixels = integrate();
        FloatBuffer staging = MemoryUtil.memAllocFloat(pixels.length);
        texture = glGenTextures();
        try {
            staging.put(pixels).flip();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16F, SIZE, SIZE, 0, GL_RG, GL_FLOAT, staging);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        } finally {
            MemoryUtil.memFree(staging);
        }
    }

    /** Binds the lookup to the active texture unit. */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    /**
     * Returns the one-time upload size.
     *
     * @return uploaded byte count
     */
    public long uploadedBytes() {
        return (long) SIZE * SIZE * 2L * Float.BYTES;
    }

    /** Deletes the context-local lookup texture. */
    @Override
    public void close() {
        glDeleteTextures(texture);
    }

    /** Numerically integrates the scale and bias terms across view angle and roughness. */
    private static float[] integrate() {
        float[] result = new float[SIZE * SIZE * 2];
        for (int y = 0; y < SIZE; y++) {
            float roughness = (y + 0.5f) / SIZE;
            for (int x = 0; x < SIZE; x++) {
                float normalDotView = (x + 0.5f) / SIZE;
                float viewX = (float) Math.sqrt(Math.max(0.0f, 1.0f - normalDotView * normalDotView));
                float scale = 0.0f;
                float bias = 0.0f;
                for (int index = 0; index < SAMPLE_COUNT; index++) {
                    float first = (index + 0.5f) / SAMPLE_COUNT;
                    float second = radicalInverse(index);
                    float alpha = roughness * roughness;
                    float alphaSquared = alpha * alpha;
                    float phi = (float) (Math.TAU * first);
                    float normalDotHalf =
                            (float) Math.sqrt((1.0f - second) / Math.max(1.0f + (alphaSquared - 1.0f) * second, 1e-6f));
                    float sine = (float) Math.sqrt(Math.max(0.0f, 1.0f - normalDotHalf * normalDotHalf));
                    float halfX = sine * (float) Math.cos(phi);
                    float viewDotHalf = Math.max(viewX * halfX + normalDotView * normalDotHalf, 0.0f);
                    float lightZ = 2.0f * viewDotHalf * normalDotHalf - normalDotView;
                    if (lightZ > 0.0f) {
                        float geometry = geometrySmith(normalDotView, lightZ, roughness);
                        float visibility = geometry * viewDotHalf / Math.max(normalDotHalf * normalDotView, 1e-6f);
                        float fresnel = (float) Math.pow(1.0f - viewDotHalf, 5.0);
                        scale += (1.0f - fresnel) * visibility;
                        bias += fresnel * visibility;
                    }
                }
                int offset = (y * SIZE + x) * 2;
                result[offset] = scale / SAMPLE_COUNT;
                result[offset + 1] = bias / SAMPLE_COUNT;
            }
        }
        return result;
    }

    /** Evaluates separable Smith masking for environment integration. */
    private static float geometrySmith(float normalDotView, float normalDotLight, float roughness) {
        float k = roughness * roughness * 0.5f;
        return geometrySchlick(normalDotView, k) * geometrySchlick(normalDotLight, k);
    }

    /** Evaluates one Schlick-GGX masking term. */
    private static float geometrySchlick(float normalDotDirection, float k) {
        return normalDotDirection / Math.max(normalDotDirection * (1.0f - k) + k, 1e-6f);
    }

    /** Computes the base-two Van der Corput radical inverse. */
    private static float radicalInverse(int bits) {
        return (float) (Integer.toUnsignedLong(Integer.reverse(bits)) * 2.3283064365386963e-10);
    }
}
