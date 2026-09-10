/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import java.time.Duration;
import java.util.Objects;

/** Process launch policy separating the one-time splash from later world loads. */
final class DesktopLaunchPolicy {
    private boolean initialSplashAvailable = true;

    /** Claims the process's one permitted initial splash presentation. */
    synchronized boolean claimInitialSplash() {
        boolean available = initialSplashAvailable;
        initialSplashAvailable = false;
        return available;
    }

    /** Returns whether the configured minimum display duration has elapsed. */
    static boolean minimumElapsed(long shownAt, long currentTime, Duration minimumDuration) {
        Objects.requireNonNull(minimumDuration, "minimumDuration");
        return currentTime - shownAt >= minimumDuration.toNanos();
    }
}
