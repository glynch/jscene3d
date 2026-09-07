/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.util.Objects;
import java.util.regex.Pattern;

/** Shared validation for values passed to macOS packaging tools. */
final class MacOsPackageValues {
    private static final Pattern APPLICATION_VERSION = Pattern.compile("[1-9]\\d*(?:\\.\\d+){0,2}");

    /** Prevents construction. */
    private MacOsPackageValues() {
        throw new AssertionError("MacOsPackageValues cannot be instantiated");
    }

    /** Requires a macOS host. */
    static void requireHost(String operatingSystemName) {
        String validName = Objects.requireNonNull(operatingSystemName, "operatingSystemName");
        if (!validName.startsWith("Mac")) {
            throw new UnsupportedOperationException("native application export currently supports macOS only");
        }
    }

    /** Requires a name that is also one safe application-bundle filename. */
    static String requireApplicationName(String name) {
        String validName = Objects.requireNonNull(name, "applicationName");
        if (validName.isBlank()
                || validName.equals(".")
                || validName.equals("..")
                || validName.indexOf('/') >= 0
                || validName.indexOf(':') >= 0) {
            throw new IllegalArgumentException("invalid macOS application name: " + name);
        }
        return validName;
    }

    /** Requires the restricted numeric version accepted by macOS packaging tools. */
    static String requireApplicationVersion(String version) {
        String validVersion = Objects.requireNonNull(version, "applicationVersion");
        if (!APPLICATION_VERSION.matcher(validVersion).matches()) {
            throw new IllegalArgumentException(
                    "applicationVersion must contain one to three numeric components and start above zero: " + version);
        }
        return validVersion;
    }
}
