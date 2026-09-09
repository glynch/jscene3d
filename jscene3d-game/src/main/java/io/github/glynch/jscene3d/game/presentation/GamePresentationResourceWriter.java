/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.audio.PcmAudio;
import io.github.glynch.jscene3d.project.resource.ResourceWriter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;

/** Publishes version-one signed PCM resources without exposing their storage format to importers. */
public final class GamePresentationResourceWriter {
    /** Prevents construction of this stateless writer. */
    private GamePresentationResourceWriter() {
        throw new AssertionError("GamePresentationResourceWriter cannot be instantiated");
    }

    /**
     * Writes one PCM resource document for an independently published sample payload.
     *
     * @param output destination for the resource document
     * @param audio PCM metadata to publish
     * @param payloadReference reference to the independently published sample payload
     * @throws IOException if the resource document cannot be written
     */
    public static void writePcmAudioDefinition(OutputStream output, PcmAudio audio, ResourceReference payloadReference)
            throws IOException {
        PcmAudio validAudio = Objects.requireNonNull(audio, "audio");
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                GamePresentationDescriptors.pcmAudioResourceType(),
                Map.of(
                        "payload",
                                new ProjectValue.ReferenceValue(
                                        Objects.requireNonNull(payloadReference, "payloadReference")),
                        "channels", number(validAudio.channels()),
                        "sample-rate", number(validAudio.sampleRate()),
                        "sample-count", number(validAudio.samples().length)));
    }

    /**
     * Writes one PCM sample payload as signed little-endian 16-bit values.
     *
     * @param output destination for the sample payload
     * @param audio PCM samples to publish
     * @throws IOException if the payload cannot be written
     */
    public static void writePcmAudioPayload(OutputStream output, PcmAudio audio) throws IOException {
        PcmAudio validAudio = Objects.requireNonNull(audio, "audio");
        short[] samples = validAudio.samples();
        ByteBuffer bytes = ByteBuffer.allocate(Math.multiplyExact(samples.length, Short.BYTES))
                .order(ByteOrder.LITTLE_ENDIAN);
        bytes.asShortBuffer().put(samples);
        Objects.requireNonNull(output, "output").write(bytes.array());
    }

    /** Creates one exact portable integer. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }
}
