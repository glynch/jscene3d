/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Validated immutable files required to launch the staged JDT LS release. */
record JdtLanguageServerDistribution(Path home, Path launcher, Path platformConfiguration) {
    static JdtLanguageServerDistribution fromHome(Path home, OperatingSystem operatingSystem) throws IOException {
        return fromHome(home, operatingSystem, System.getProperty("os.arch", ""));
    }

    static JdtLanguageServerDistribution fromHome(Path home, OperatingSystem operatingSystem, String architecture)
            throws IOException {
        Path root = Objects.requireNonNull(home, "home").toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("JDT LS distribution directory does not exist: " + root);
        }
        Path launcher = findLauncher(root.resolve("plugins"));
        Path configuration = root.resolve(configurationDirectory(operatingSystem, architecture));
        if (!Files.isDirectory(configuration)) {
            throw new IOException("JDT LS platform configuration does not exist: " + configuration);
        }
        return new JdtLanguageServerDistribution(root, launcher, configuration);
    }

    private static Path findLauncher(Path plugins) throws IOException {
        List<Path> launchers = new ArrayList<>();
        try (DirectoryStream<Path> candidates =
                Files.newDirectoryStream(plugins, "org.eclipse.equinox.launcher_*.jar")) {
            candidates.forEach(launchers::add);
        }
        if (launchers.size() != 1) {
            throw new IOException("Expected one Eclipse launcher in " + plugins + ", found " + launchers.size());
        }
        return launchers.getFirst().toAbsolutePath().normalize();
    }

    private static String configurationDirectory(OperatingSystem operatingSystem, String architecture)
            throws IOException {
        boolean arm = isArmArchitecture(architecture);
        return switch (Objects.requireNonNull(operatingSystem, "operatingSystem")) {
            case MACOS -> arm ? "config_mac_arm" : "config_mac";
            case WINDOWS -> "config_win";
            case LINUX -> arm ? "config_linux_arm" : "config_linux";
            case OTHER -> throw new IOException("JDT LS does not provide configuration for this operating system");
        };
    }

    private static boolean isArmArchitecture(String architecture) {
        String normalized =
                Objects.requireNonNull(architecture, "architecture").strip().toLowerCase(Locale.ROOT);
        return normalized.equals("aarch64") || normalized.equals("arm64");
    }
}
