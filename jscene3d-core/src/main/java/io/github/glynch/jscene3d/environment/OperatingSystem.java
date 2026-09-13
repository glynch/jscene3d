/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.environment;

import java.util.Locale;
import java.util.Objects;

/** Broad host operating-system families needed by portable JScene3D features. */
public enum OperatingSystem {
    /** Apple macOS. */
    MACOS,

    /** Microsoft Windows. */
    WINDOWS,

    /** Linux distributions. */
    LINUX,

    /** An operating system outside the explicitly recognized families. */
    OTHER;

    /** Name of the standard Java host operating-system property. */
    private static final String OS_NAME_PROPERTY = "os.name";

    /**
     * Detects the operating-system family of the current Java process.
     *
     * @return current operating-system family
     */
    public static OperatingSystem current() {
        return fromName(System.getProperty(OS_NAME_PROPERTY, ""));
    }

    /**
     * Classifies a Java operating-system name without relying on the current host.
     *
     * @param name operating-system name, normally obtained from {@code os.name}
     * @return matching broad operating-system family
     */
    public static OperatingSystem fromName(String name) {
        String normalized = Objects.requireNonNull(name, "name").strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("mac") || normalized.startsWith("darwin")) {
            return MACOS;
        }
        if (normalized.startsWith("windows")) {
            return WINDOWS;
        }
        if (normalized.startsWith("linux")) {
            return LINUX;
        }
        return OTHER;
    }
}
