/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Build-time versions advertised during protocol initialization. */
public final class AuthoringServiceBuildInfo {
    private static final String RESOURCE = "authoring-service-build.properties";

    private final String serviceVersion;
    private final String engineVersion;

    /** Stores validated embedded build versions. */
    private AuthoringServiceBuildInfo(String serviceVersion, String engineVersion) {
        this.serviceVersion = requireValue(serviceVersion, "serviceVersion");
        this.engineVersion = requireValue(engineVersion, "engineVersion");
    }

    /**
     * Loads versions filtered into the service artifact.
     *
     * @return current build information
     */
    public static AuthoringServiceBuildInfo current() {
        Properties properties = new Properties();
        try (InputStream input = AuthoringServiceBuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing authoring service build information");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read authoring service build information", exception);
        }
        return new AuthoringServiceBuildInfo(
                properties.getProperty("serviceVersion"), properties.getProperty("engineVersion"));
    }

    /**
     * Returns the authoring-service implementation version.
     *
     * @return service implementation version
     */
    public String serviceVersion() {
        return serviceVersion;
    }

    /**
     * Returns the JScene3D engine compatibility version.
     *
     * @return engine compatibility version
     */
    public String engineVersion() {
        return engineVersion;
    }

    /** Rejects missing or unfiltered build values. */
    private static String requireValue(String value, String name) {
        String candidate = Objects.requireNonNull(value, name).trim();
        if (candidate.isEmpty() || candidate.contains("${")) {
            throw new IllegalStateException("Invalid authoring service build value: " + name);
        }
        return candidate;
    }
}
