/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.environment;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Resolves platform-native per-user directories for JScene3D applications. */
public final class ApplicationDirectories {
    private static final Pattern APPLICATION_ID = Pattern.compile("[A-Za-z0-9._-]+");
    private static final String LOCAL_APP_DATA = "LOCALAPPDATA";
    private static final String XDG_CACHE_HOME = "XDG_CACHE_HOME";

    private ApplicationDirectories() {}

    /**
     * Resolves the current user's platform-native cache directory for an application.
     *
     * <p>The returned path is not created by this method.
     *
     * @param applicationId stable filesystem-safe application identifier
     * @return absolute normalized cache directory
     * @throws IllegalArgumentException if the application identifier is invalid
     * @throws IllegalStateException if the current user's home directory is unavailable
     */
    public static Path cache(String applicationId) {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            throw new IllegalStateException("The current user's home directory is unavailable");
        }
        return cache(applicationId, OperatingSystem.current(), Path.of(home), System.getenv());
    }

    static Path cache(
            String applicationId, OperatingSystem operatingSystem, Path userHome, Map<String, String> environment) {
        String id = requireApplicationId(applicationId);
        OperatingSystem system = Objects.requireNonNull(operatingSystem, "operatingSystem");
        Path home =
                Objects.requireNonNull(userHome, "userHome").toAbsolutePath().normalize();
        Map<String, String> variables = Objects.requireNonNull(environment, "environment");
        Path directory =
                switch (system) {
                    case MACOS -> home.resolve("Library/Caches").resolve(id);
                    case WINDOWS ->
                        environmentPath(variables, LOCAL_APP_DATA)
                                .orElseGet(() -> home.resolve("AppData/Local"))
                                .resolve(id)
                                .resolve("Cache");
                    case LINUX ->
                        environmentPath(variables, XDG_CACHE_HOME)
                                .orElseGet(() -> home.resolve(".cache"))
                                .resolve(id);
                    case OTHER -> home.resolve(".cache").resolve(id);
                };
        return directory.toAbsolutePath().normalize();
    }

    private static String requireApplicationId(String applicationId) {
        String id = Objects.requireNonNull(applicationId, "applicationId").strip();
        if (!APPLICATION_ID.matcher(id).matches() || id.equals(".") || id.equals("..")) {
            throw new IllegalArgumentException("applicationId must be a filesystem-safe name");
        }
        return id;
    }

    private static Optional<Path> environmentPath(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(value).normalize();
        return path.isAbsolute() ? Optional.of(path) : Optional.empty();
    }
}
