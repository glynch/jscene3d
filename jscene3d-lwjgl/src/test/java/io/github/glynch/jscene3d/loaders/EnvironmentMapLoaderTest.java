/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EnvironmentMapLoaderTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsRadianceHdrIntoOwnedEnvironmentMap() throws IOException {
        Path source = temporaryDirectory.resolve("one-pixel.hdr");
        Files.write(source, onePixelHdr());

        try (EnvironmentMap environmentMap = EnvironmentMapLoader.load(source)) {
            assertThat(environmentMap.width()).isOne();
            assertThat(environmentMap.height()).isOne();
            assertThat(environmentMap.pixelComponentCount()).isEqualTo(3);
        }
    }

    @Test
    void rejectsOtherImageFormatsClearly() throws IOException {
        Path source = temporaryDirectory.resolve("not-hdr.png");
        Files.write(source, new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        assertThatExceptionOfType(EnvironmentMapLoadException.class)
                .isThrownBy(() -> EnvironmentMapLoader.load(source))
                .withMessageContaining("expected HDR")
                .satisfies(exception -> assertThat(exception.source()).isEqualTo(source));
    }

    /** Creates a valid legacy one-pixel Radiance RGBE stream. */
    private static byte[] onePixelHdr() {
        byte[] header = ("#?RADIANCE\nFORMAT=32-bit_rle_rgbe\n\n-Y 1 +X 1\n").getBytes(StandardCharsets.US_ASCII);
        byte[] encoded = new byte[header.length + 4];
        System.arraycopy(header, 0, encoded, 0, header.length);
        encoded[header.length] = (byte) 128;
        encoded[header.length + 1] = (byte) 64;
        encoded[header.length + 2] = (byte) 32;
        encoded[header.length + 3] = (byte) 129;
        return encoded;
    }
}
