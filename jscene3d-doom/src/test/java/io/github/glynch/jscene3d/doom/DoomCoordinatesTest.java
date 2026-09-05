/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.doom.map.DoomCoordinates;
import org.junit.jupiter.api.Test;

/** Specifies the shared Doom-to-JScene3D coordinate convention. */
final class DoomCoordinatesTest {
    /** Converts measurements and reverses the horizontal-axis mapping exactly. */
    @Test
    void convertsCoordinates() {
        assertThat(DoomCoordinates.toWorld(64.0F)).isEqualTo(2.0F);
        assertThat(DoomCoordinates.deltaToWorld(96, 32)).isEqualTo(2.0F);
        assertThat(DoomCoordinates.yToWorldZ(64.0)).isEqualTo(-2.0F);
        assertThat(Float.floatToIntBits(DoomCoordinates.yToWorldZ(0.0))).isEqualTo(Float.floatToIntBits(0.0F));
        assertThat(DoomCoordinates.fromWorld(2.0F)).isEqualTo(64.0);
        assertThat(DoomCoordinates.fromWorldFloat(2.0F)).isEqualTo(64.0F);
        assertThat(DoomCoordinates.worldZToY(-2.0F)).isEqualTo(64.0);
    }
}
