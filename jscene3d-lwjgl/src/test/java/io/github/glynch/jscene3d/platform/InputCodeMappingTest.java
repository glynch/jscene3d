/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class InputCodeMappingTest {
    @Test
    void mapsSupportedAndRejectsOutOfRangeKeyCodes() {
        assertThat(Key.fromPlatformCode(Key.A.platformCode())).isEqualTo(Key.A);
        assertThat(Key.fromPlatformCode(-1)).isNull();
        assertThat(Key.fromPlatformCode(Key.platformCodeLimit())).isNull();
    }

    @Test
    void mapsSupportedAndRejectsOutOfRangeMouseButtonCodes() {
        assertThat(MouseButton.fromPlatformCode(MouseButton.LEFT.platformCode()))
                .isEqualTo(MouseButton.LEFT);
        assertThat(MouseButton.fromPlatformCode(-1)).isNull();
        assertThat(MouseButton.fromPlatformCode(MouseButton.platformCodeLimit()))
                .isNull();
    }
}
