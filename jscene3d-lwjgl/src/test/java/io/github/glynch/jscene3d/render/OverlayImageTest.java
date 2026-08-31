/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class OverlayImageTest {
    @Test
    void createsDefensiveAlphaAndColorImages() {
        byte[] alpha = {(byte) 0xff};
        byte[] rgba = {(byte) 0xff, 0, 0, (byte) 0xff};

        OverlayImage alphaImage = OverlayImage.alphaMask(1, 1, alpha);
        OverlayImage colorImage = OverlayImage.srgbRgba(1, 1, rgba);
        alpha[0] = 0;
        rgba[0] = 0;

        assertThat(alphaImage.format()).isEqualTo(OverlayImageFormat.ALPHA_MASK);
        assertThat(alphaImage.pixels()).containsExactly((byte) 0xff);
        assertThat(colorImage.format()).isEqualTo(OverlayImageFormat.SRGB_RGBA);
        assertThat(colorImage.pixels()).containsExactly((byte) 0xff, (byte) 0, (byte) 0, (byte) 0xff);
    }

    @Test
    void rejectsInvalidPixelStorage() {
        assertThatIllegalArgumentException().isThrownBy(() -> OverlayImage.alphaMask(2, 1, new byte[1]));
        assertThatIllegalArgumentException().isThrownBy(() -> OverlayImage.srgbRgba(1, 1, new byte[3]));
    }

    @Test
    void writesFullColorPngAndRejectsAlphaMasks(@TempDir Path directory) throws Exception {
        OverlayImage color = OverlayImage.srgbRgba(1, 1, new byte[] {(byte) 0xff, 0, 0, (byte) 0xff});
        Path destination = directory.resolve("thumbnail.png");

        OverlayImageWriter.writePng(destination, color);

        assertThat(Files.size(destination)).isPositive();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OverlayImageWriter.writePng(destination, OverlayImage.alphaMask(1, 1, new byte[1])));
    }
}
