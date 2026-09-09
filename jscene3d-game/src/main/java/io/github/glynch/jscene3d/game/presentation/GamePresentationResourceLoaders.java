/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.audio.PcmAudio;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/** Supplies runtime loaders corresponding exactly to {@link GamePresentationDescriptors}. */
public final class GamePresentationResourceLoaders {
    private static final List<RuntimeResourceLoader<?>> ALL = List.of(new PcmAudioLoader());

    /** Prevents construction of this stable loader collection. */
    private GamePresentationResourceLoaders() {
        throw new AssertionError("GamePresentationResourceLoaders cannot be instantiated");
    }

    /**
     * Returns all built-in version-one game-presentation resource loaders.
     *
     * @return immutable loader collection
     */
    public static List<RuntimeResourceLoader<?>> all() {
        return ALL;
    }

    /** Reconstructs immutable PCM from its exact signed little-endian sample payload. */
    private static final class PcmAudioLoader implements RuntimeResourceLoader<PcmAudioResource> {
        @Override
        public RegisteredType type() {
            return GamePresentationDescriptors.pcmAudioResourceType();
        }

        @Override
        public Class<PcmAudioResource> valueType() {
            return PcmAudioResource.class;
        }

        @Override
        public PcmAudioResource load(ResourceDefinition definition, ResourceContent content) throws IOException {
            Map<String, ProjectValue> values = definition.properties();
            int channels = positiveInteger(require(values, "channels"), "channels");
            int sampleRate = positiveInteger(require(values, "sample-rate"), "sample-rate");
            int sampleCount = positiveInteger(require(values, "sample-count"), "sample-count");
            if (channels != 1 && channels != 2) {
                throw new IllegalArgumentException("PCM channels must be one or two: " + channels);
            }
            if (sampleCount % channels != 0) {
                throw new IllegalArgumentException("PCM sample count must contain complete frames");
            }
            ProjectValue payload = require(values, "payload");
            if (!(payload instanceof ProjectValue.ReferenceValue reference)) {
                throw new IllegalArgumentException("PCM payload must be a reference");
            }
            short[] samples = readSamples(content, reference, sampleCount);
            PcmAudio audio =
                    channels == 1 ? PcmAudio.mono16(sampleRate, samples) : PcmAudio.stereo16(sampleRate, samples);
            return PcmAudioResource.owning(audio);
        }

        /** Reads exactly the declared number of signed little-endian samples. */
        private static short[] readSamples(
                ResourceContent content, ProjectValue.ReferenceValue reference, int sampleCount) throws IOException {
            int byteCount = Math.multiplyExact(sampleCount, Short.BYTES);
            try (InputStream input = content.openPayload(reference.reference())) {
                byte[] bytes = input.readNBytes(byteCount);
                if (bytes.length != byteCount || input.read() != -1) {
                    throw new IllegalArgumentException("PCM payload length must be " + byteCount + " bytes");
                }
                short[] samples = new short[sampleCount];
                ByteBuffer.wrap(bytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(samples);
                return samples;
            }
        }

        /** Returns one required resource property. */
        private static ProjectValue require(Map<String, ProjectValue> values, String name) {
            ProjectValue value = values.get(name);
            if (value == null) {
                throw new IllegalArgumentException("resource property is missing: " + name);
            }
            return value;
        }

        /** Reads one exact positive integer resource property. */
        private static int positiveInteger(ProjectValue value, String name) {
            if (!(value instanceof ProjectValue.NumberValue number)) {
                throw new IllegalArgumentException("resource property must be a number: " + name);
            }
            try {
                int result = number.value().intValueExact();
                if (result < 1) {
                    throw new IllegalArgumentException("resource property must be positive: " + name);
                }
                return result;
            } catch (ArithmeticException failure) {
                throw new IllegalArgumentException("resource property must be an exact integer: " + name, failure);
            }
        }
    }
}
