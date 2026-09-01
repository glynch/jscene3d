/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static java.lang.Math.PI;
import static java.lang.Math.TAU;

import java.util.ArrayList;
import java.util.List;

/** CPU-side deterministic image-based-lighting convolution for one HDR environment. */
final class EnvironmentPrefilter {
    static final int IRRADIANCE_WIDTH = 32;
    static final int IRRADIANCE_HEIGHT = 16;
    static final int REFLECTION_WIDTH = 256;
    static final int REFLECTION_HEIGHT = 128;
    static final int SAMPLE_COUNT = 64;

    private static final float INVERSE_PI = (float) (1.0 / PI);

    private final int sourceWidth;
    private final int sourceHeight;
    private final float[] source;
    private final Sample sample = new Sample();

    /** Retains one renderer-local source copy for deterministic convolution. */
    EnvironmentPrefilter(int sourceWidth, int sourceHeight, float[] source) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.source = source;
    }

    /** Produces a compact cosine-weighted diffuse-radiance equirectangular map. */
    float[] irradiance() {
        float[] result = new float[IRRADIANCE_WIDTH * IRRADIANCE_HEIGHT * 3];
        for (int y = 0; y < IRRADIANCE_HEIGHT; y++) {
            for (int x = 0; x < IRRADIANCE_WIDTH; x++) {
                Direction direction = direction(x, y, IRRADIANCE_WIDTH, IRRADIANCE_HEIGHT);
                Basis basis = basis(direction);
                float red = 0.0f;
                float green = 0.0f;
                float blue = 0.0f;
                for (int index = 0; index < SAMPLE_COUNT; index++) {
                    float first = (index + 0.5f) / SAMPLE_COUNT;
                    float second = radicalInverse(index);
                    float radius = (float) Math.sqrt(first);
                    float phi = (float) (TAU * second);
                    float tangentX = radius * (float) Math.cos(phi);
                    float tangentY = radius * (float) Math.sin(phi);
                    float normal = (float) Math.sqrt(Math.max(0.0f, 1.0f - first));
                    float sampleX = basis.tangentX * tangentX + basis.bitangentX * tangentY + direction.x * normal;
                    float sampleY = basis.tangentY * tangentX + basis.bitangentY * tangentY + direction.y * normal;
                    float sampleZ = basis.tangentZ * tangentX + basis.bitangentZ * tangentY + direction.z * normal;
                    sample(sampleX, sampleY, sampleZ);
                    red += sample.red;
                    green += sample.green;
                    blue += sample.blue;
                }
                int offset = (y * IRRADIANCE_WIDTH + x) * 3;
                result[offset] = red / SAMPLE_COUNT;
                result[offset + 1] = green / SAMPLE_COUNT;
                result[offset + 2] = blue / SAMPLE_COUNT;
            }
        }
        return result;
    }

    /** Produces GGX-prefiltered reflection radiance from sharp to fully rough mip levels. */
    List<Level> reflections() {
        int largestDimension = Math.max(REFLECTION_WIDTH, REFLECTION_HEIGHT);
        int levelCount = 1 + Integer.numberOfTrailingZeros(largestDimension);
        List<Level> levels = new ArrayList<>(levelCount);
        for (int level = 0; level < levelCount; level++) {
            int width = Math.max(1, REFLECTION_WIDTH >> level);
            int height = Math.max(1, REFLECTION_HEIGHT >> level);
            float roughness = (float) level / (levelCount - 1);
            float[] pixels =
                    roughness == 0.0f ? resample(width, height) : prefilterReflection(width, height, roughness);
            levels.add(new Level(width, height, pixels));
        }
        return List.copyOf(levels);
    }

    /** Resamples the source for the sharpest reflection level. */
    private float[] resample(int width, int height) {
        float[] result = new float[width * height * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Direction direction = direction(x, y, width, height);
                sample(direction.x, direction.y, direction.z);
                int offset = (y * width + x) * 3;
                result[offset] = sample.red;
                result[offset + 1] = sample.green;
                result[offset + 2] = sample.blue;
            }
        }
        return result;
    }

    /** Integrates incoming radiance around each reflection direction using GGX importance samples. */
    private float[] prefilterReflection(int width, int height, float roughness) {
        float[] result = new float[width * height * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Direction normal = direction(x, y, width, height);
                Basis basis = basis(normal);
                float red = 0.0f;
                float green = 0.0f;
                float blue = 0.0f;
                float weight = 0.0f;
                for (int index = 0; index < SAMPLE_COUNT; index++) {
                    float first = (index + 0.5f) / SAMPLE_COUNT;
                    float second = radicalInverse(index);
                    float alpha = roughness * roughness;
                    float alphaSquared = alpha * alpha;
                    float phi = (float) (TAU * first);
                    float cosine =
                            (float) Math.sqrt((1.0f - second) / Math.max(1.0f + (alphaSquared - 1.0f) * second, 1e-6f));
                    float sine = (float) Math.sqrt(Math.max(0.0f, 1.0f - cosine * cosine));
                    float halfTangentX = sine * (float) Math.cos(phi);
                    float halfTangentY = sine * (float) Math.sin(phi);
                    float halfX = basis.tangentX * halfTangentX + basis.bitangentX * halfTangentY + normal.x * cosine;
                    float halfY = basis.tangentY * halfTangentX + basis.bitangentY * halfTangentY + normal.y * cosine;
                    float halfZ = basis.tangentZ * halfTangentX + basis.bitangentZ * halfTangentY + normal.z * cosine;
                    float normalDotHalf = normal.x * halfX + normal.y * halfY + normal.z * halfZ;
                    float lightX = 2.0f * normalDotHalf * halfX - normal.x;
                    float lightY = 2.0f * normalDotHalf * halfY - normal.y;
                    float lightZ = 2.0f * normalDotHalf * halfZ - normal.z;
                    float normalDotLight = normal.x * lightX + normal.y * lightY + normal.z * lightZ;
                    if (normalDotLight > 0.0f) {
                        sample(lightX, lightY, lightZ);
                        red += sample.red * normalDotLight;
                        green += sample.green * normalDotLight;
                        blue += sample.blue * normalDotLight;
                        weight += normalDotLight;
                    }
                }
                int offset = (y * width + x) * 3;
                float inverseWeight = weight > 0.0f ? 1.0f / weight : 0.0f;
                result[offset] = red * inverseWeight;
                result[offset + 1] = green * inverseWeight;
                result[offset + 2] = blue * inverseWeight;
            }
        }
        return result;
    }

    /** Bilinearly samples the top-row-first equirectangular source with horizontal wrapping. */
    private void sample(float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        float inverseLength = 1.0f / Math.max(length, 1e-6f);
        float normalizedX = x * inverseLength;
        float normalizedY = y * inverseLength;
        float normalizedZ = z * inverseLength;
        float u = (float) (Math.atan2(normalizedZ, normalizedX) / TAU + 0.5);
        float v = (float) (Math.acos(Math.clamp(normalizedY, -1.0f, 1.0f)) * INVERSE_PI);
        float imageX = u * sourceWidth - 0.5f;
        float imageY = v * sourceHeight - 0.5f;
        int firstX = Math.floorMod((int) Math.floor(imageX), sourceWidth);
        int secondX = (firstX + 1) % sourceWidth;
        int firstY = Math.clamp((int) Math.floor(imageY), 0, sourceHeight - 1);
        int secondY = Math.min(firstY + 1, sourceHeight - 1);
        float fractionX = imageX - (float) Math.floor(imageX);
        float fractionY = imageY - (float) Math.floor(imageY);
        sample.red = interpolate(firstX, secondX, firstY, secondY, fractionX, fractionY, 0);
        sample.green = interpolate(firstX, secondX, firstY, secondY, fractionX, fractionY, 1);
        sample.blue = interpolate(firstX, secondX, firstY, secondY, fractionX, fractionY, 2);
    }

    /** Interpolates one source color component. */
    private float interpolate(
            int firstX, int secondX, int firstY, int secondY, float fractionX, float fractionY, int component) {
        float top = mix(
                source[(firstY * sourceWidth + firstX) * 3 + component],
                source[(firstY * sourceWidth + secondX) * 3 + component],
                fractionX);
        float bottom = mix(
                source[(secondY * sourceWidth + firstX) * 3 + component],
                source[(secondY * sourceWidth + secondX) * 3 + component],
                fractionX);
        return mix(top, bottom, fractionY);
    }

    /** Converts one equirectangular pixel center to a unit direction. */
    private static Direction direction(int x, int y, int width, int height) {
        float longitude = (float) (((x + 0.5f) / width - 0.5f) * TAU);
        float latitude = (0.5f - (y + 0.5f) / height) * (float) PI;
        float latitudeCosine = (float) Math.cos(latitude);
        return new Direction(
                latitudeCosine * (float) Math.cos(longitude),
                (float) Math.sin(latitude),
                latitudeCosine * (float) Math.sin(longitude));
    }

    /** Builds an orthonormal tangent basis around a unit direction. */
    private static Basis basis(Direction direction) {
        float upX = Math.abs(direction.y) < 0.999f ? 0.0f : 1.0f;
        float upY = Math.abs(direction.y) < 0.999f ? 1.0f : 0.0f;
        float tangentX = upY * direction.z;
        float tangentY = -upX * direction.z;
        float tangentZ = upX * direction.y - upY * direction.x;
        float inverseLength = 1.0f / (float) Math.sqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
        tangentX *= inverseLength;
        tangentY *= inverseLength;
        tangentZ *= inverseLength;
        float bitangentX = direction.y * tangentZ - direction.z * tangentY;
        float bitangentY = direction.z * tangentX - direction.x * tangentZ;
        float bitangentZ = direction.x * tangentY - direction.y * tangentX;
        return new Basis(tangentX, tangentY, tangentZ, bitangentX, bitangentY, bitangentZ);
    }

    /** Computes the base-two Van der Corput radical inverse without allocation. */
    private static float radicalInverse(int bits) {
        int reversed = Integer.reverse(bits);
        return (float) (Integer.toUnsignedLong(reversed) * 2.3283064365386963e-10);
    }

    /** Linearly interpolates two scalar values. */
    private static float mix(float first, float second, float fraction) {
        return first + (second - first) * fraction;
    }

    /** One immutable prefiltered equirectangular mip level. */
    static final class Level {
        private final int width;
        private final int height;
        private final float[] pixels;

        /** Retains the generated pixels for upload by the owning environment resource. */
        Level(int width, int height, float[] pixels) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
        }

        /** Returns the level width in pixels. */
        int width() {
            return width;
        }

        /** Returns the level height in pixels. */
        int height() {
            return height;
        }

        /** Returns the renderer-owned RGB pixel data. */
        float[] pixels() {
            return pixels;
        }
    }

    /** One immutable unit direction. */
    private record Direction(float x, float y, float z) {}

    /** One immutable tangent frame. */
    private record Basis(
            float tangentX, float tangentY, float tangentZ, float bitangentX, float bitangentY, float bitangentZ) {}

    /** Reused bilinear sample result. */
    private static final class Sample {
        private float red;
        private float green;
        private float blue;
    }
}
