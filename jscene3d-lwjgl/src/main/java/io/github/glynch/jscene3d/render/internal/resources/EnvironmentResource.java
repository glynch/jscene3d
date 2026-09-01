/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
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
import static org.lwjgl.opengl.GL30.GL_RGB16F;

import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.nio.FloatBuffer;
import java.util.List;
import org.lwjgl.system.MemoryUtil;

/** Context-local HDR source, diffuse irradiance, and GGX reflection realization. */
public final class EnvironmentResource implements AutoCloseable {
    private final int sourceTexture;
    private final int irradianceTexture;
    private final int reflectionTexture;
    private final int reflectionLevelCount;
    private final long uploadedBytes;

    /**
     * Copies source pixels once, derives IBL maps, and uploads all context-local textures.
     *
     * @param environmentMap application-owned HDR source
     */
    public EnvironmentResource(EnvironmentMap environmentMap) {
        float[] sourcePixels = new float[environmentMap.pixelComponentCount()];
        environmentMap.copyPixelsTo(FloatBuffer.wrap(sourcePixels));
        EnvironmentPrefilter prefilter =
                new EnvironmentPrefilter(environmentMap.width(), environmentMap.height(), sourcePixels);
        float[] irradiancePixels = prefilter.irradiance();
        List<EnvironmentPrefilter.Level> reflectionLevels = prefilter.reflections();
        reflectionLevelCount = reflectionLevels.size();
        sourceTexture = uploadSingle(environmentMap.width(), environmentMap.height(), sourcePixels);
        irradianceTexture = uploadSingle(
                EnvironmentPrefilter.IRRADIANCE_WIDTH, EnvironmentPrefilter.IRRADIANCE_HEIGHT, irradiancePixels);
        reflectionTexture = uploadLevels(reflectionLevels);
        uploadedBytes =
                ((long) sourcePixels.length + irradiancePixels.length + reflectionComponentCount(reflectionLevels))
                        * Float.BYTES;
    }

    /** Binds the original sharp HDR environment to the active texture unit. */
    public void bindSource() {
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
    }

    /** Binds diffuse irradiance to the active texture unit. */
    public void bindIrradiance() {
        glBindTexture(GL_TEXTURE_2D, irradianceTexture);
    }

    /** Binds the GGX-prefiltered reflection mip chain to the active texture unit. */
    public void bindReflections() {
        glBindTexture(GL_TEXTURE_2D, reflectionTexture);
    }

    /**
     * Returns the largest valid reflection mip index.
     *
     * @return maximum reflection level
     */
    public float maximumReflectionLevel() {
        return reflectionLevelCount - 1.0f;
    }

    /**
     * Returns the total CPU-to-GPU HDR upload size.
     *
     * @return uploaded byte count
     */
    public long uploadedBytes() {
        return uploadedBytes;
    }

    /** Deletes all context-local environment textures. */
    @Override
    public void close() {
        glDeleteTextures(sourceTexture);
        glDeleteTextures(irradianceTexture);
        glDeleteTextures(reflectionTexture);
    }

    /** Uploads one linear RGB16F equirectangular image. */
    private static int uploadSingle(int width, int height, float[] pixels) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        uploadLevel(0, width, height, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        applyWrap();
        return texture;
    }

    /** Uploads every explicitly convolved reflection mip level. */
    private static int uploadLevels(List<EnvironmentPrefilter.Level> levels) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        for (int index = 0; index < levels.size(); index++) {
            EnvironmentPrefilter.Level level = levels.get(index);
            uploadLevel(index, level.width(), level.height(), level.pixels());
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        applyWrap();
        return texture;
    }

    /** Uploads one direct floating-point RGB level through temporary native staging. */
    private static void uploadLevel(int level, int width, int height, float[] pixels) {
        FloatBuffer staging = MemoryUtil.memAllocFloat(pixels.length);
        try {
            staging.put(pixels).flip();
            glTexImage2D(GL_TEXTURE_2D, level, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, staging);
        } finally {
            MemoryUtil.memFree(staging);
        }
    }

    /** Applies equirectangular horizontal wrapping and vertical edge clamping. */
    private static void applyWrap() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    /** Counts all reflection RGB components without overflowing intermediate integer arithmetic. */
    private static long reflectionComponentCount(List<EnvironmentPrefilter.Level> levels) {
        long count = 0L;
        for (EnvironmentPrefilter.Level level : levels) {
            count += level.pixels().length;
        }
        return count;
    }
}
