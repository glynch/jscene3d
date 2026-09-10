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

    /**
     * Writes one overlay-image resource document for an independently published RGBA payload.
     *
     * @param output destination for the resource document
     * @param width image width in pixels
     * @param height image height in pixels
     * @param payloadReference published RGBA payload reference
     * @throws IOException if the document cannot be written
     */
    public static void writeOverlayImageDefinition(
            OutputStream output, int width, int height, ResourceReference payloadReference) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("overlay image dimensions must be positive");
        }
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                GamePresentationDescriptors.overlayImageResourceType(),
                Map.of(
                        "payload",
                                new ProjectValue.ReferenceValue(
                                        Objects.requireNonNull(payloadReference, "payloadReference")),
                        "width", number(width),
                        "height", number(height)));
    }

    /**
     * Writes one overlay-image resource document backed directly by an authored PNG or JPEG.
     *
     * @param output destination for the resource document
     * @param encoding exact {@code png} or {@code jpeg} encoding
     * @param payloadReference authored image payload reference
     * @throws IOException if the document cannot be written
     */
    public static void writeEncodedOverlayImageDefinition(
            OutputStream output, String encoding, ResourceReference payloadReference) throws IOException {
        String validEncoding = Objects.requireNonNull(encoding, "encoding");
        if (!(validEncoding.equals("png") || validEncoding.equals("jpeg"))) {
            throw new IllegalArgumentException("overlay image encoding must be png or jpeg");
        }
        ResourceWriter.write(
                Objects.requireNonNull(output, "output"),
                GamePresentationDescriptors.overlayImageResourceType(),
                Map.of(
                        "payload",
                                new ProjectValue.ReferenceValue(
                                        Objects.requireNonNull(payloadReference, "payloadReference")),
                        "encoding", new ProjectValue.TextValue(validEncoding)));
    }

    /**
     * Writes one exact row-major sRGB RGBA overlay-image payload.
     *
     * @param output destination for the payload
     * @param width image width in pixels
     * @param height image height in pixels
     * @param pixels row-major sRGB RGBA bytes
     * @throws IOException if the payload cannot be written
     */
    public static void writeOverlayImagePayload(OutputStream output, int width, int height, byte[] pixels)
            throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("overlay image dimensions must be positive");
        }
        byte[] validPixels = Objects.requireNonNull(pixels, "pixels");
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (validPixels.length != expected) {
            throw new IllegalArgumentException("overlay image pixels must contain exactly " + expected + " bytes");
        }
        Objects.requireNonNull(output, "output").write(validPixels);
    }

    /** Creates one exact portable integer. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }
}
