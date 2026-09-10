/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable host request selecting a scene and supplying project-defined launch parameters. */
public final class ProjectLaunchRequest {
    private static final ProjectLaunchRequest STANDARD =
            new ProjectLaunchRequest(Optional.empty(), Optional.empty(), Map.of());

    private final Optional<String> profile;
    private final Optional<Path> scene;
    private final Map<String, ProjectValue> parameters;

    /** Stores one validated launch request. */
    private ProjectLaunchRequest(Optional<String> profile, Optional<Path> scene, Map<String, ProjectValue> parameters) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.parameters = immutableParameters(parameters);
    }

    /**
     * Returns the ordinary manifest-selected launch request.
     *
     * @return shared standard request
     */
    public static ProjectLaunchRequest standard() {
        return STANDARD;
    }

    /**
     * Creates a named playtest request for an explicitly selected scene.
     *
     * @param profile stable playtest profile name
     * @param scene project-relative world-definition path
     * @param parameters immutable application-defined parameters
     * @return validated playtest request
     */
    public static ProjectLaunchRequest playtest(String profile, Path scene, Map<String, ProjectValue> parameters) {
        String validProfile = requireNonBlank(profile, "profile");
        Path validScene = Objects.requireNonNull(scene, "scene").normalize();
        if (validScene.isAbsolute() || validScene.toString().isBlank() || validScene.startsWith("..")) {
            throw new IllegalArgumentException("scene must be a project-relative path");
        }
        return new ProjectLaunchRequest(Optional.of(validProfile), Optional.of(validScene), parameters);
    }

    /** Returns the optional human-readable profile identity.
     *
     * @return selected profile name when present
     */
    public Optional<String> profile() {
        return profile;
    }

    /** Returns the optional scene overriding the manifest-selected scene.
     *
     * @return project-relative selected scene when present
     */
    public Optional<Path> scene() {
        return scene;
    }

    /** Returns all application-defined launch parameters.
     *
     * @return immutable portable parameter values
     */
    public Map<String, ProjectValue> parameters() {
        return parameters;
    }

    /** Finds one application-defined launch parameter.
     *
     * @param name parameter identity
     * @return selected value when present
     */
    public Optional<ProjectValue> parameter(String name) {
        return Optional.ofNullable(parameters.get(Objects.requireNonNull(name, "name")));
    }

    /** Reports whether this is a named playtest launch.
     *
     * @return true for a named playtest request
     */
    public boolean isPlaytest() {
        return profile.isPresent();
    }

    /** Copies and validates parameter entries while retaining declaration order. */
    private static Map<String, ProjectValue> immutableParameters(Map<String, ProjectValue> parameters) {
        Map<String, ProjectValue> copy = new LinkedHashMap<>();
        Objects.requireNonNull(parameters, "parameters")
                .forEach((name, value) -> copy.put(
                        requireNonBlank(name, "parameter name"), Objects.requireNonNull(value, "parameter value")));
        return Map.copyOf(copy);
    }

    /** Rejects null or blank externally visible names. */
    private static String requireNonBlank(String value, String description) {
        String valid = Objects.requireNonNull(value, description);
        if (valid.isBlank()) {
            throw new IllegalArgumentException(description + " must not be blank");
        }
        return valid;
    }
}
