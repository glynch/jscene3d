/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class DesktopLaunchPolicyTest {
    @Test
    void grantsTheInitialSplashOnlyOnce() {
        DesktopLaunchPolicy policy = new DesktopLaunchPolicy();

        assertThat(policy.claimInitialSplash()).isTrue();
        assertThat(policy.claimInitialSplash()).isFalse();
    }

    @Test
    void keepsTheSplashUntilItsMinimumDurationHasElapsed() {
        long shownAt = 1_000_000_000L;
        Duration minimum = Duration.ofSeconds(2);

        assertThat(DesktopLaunchPolicy.minimumElapsed(shownAt, shownAt, minimum))
                .isFalse();
        assertThat(DesktopLaunchPolicy.minimumElapsed(shownAt, shownAt + minimum.toNanos() - 1L, minimum))
                .isFalse();
        assertThat(DesktopLaunchPolicy.minimumElapsed(shownAt, shownAt + minimum.toNanos(), minimum))
                .isTrue();
    }
}
