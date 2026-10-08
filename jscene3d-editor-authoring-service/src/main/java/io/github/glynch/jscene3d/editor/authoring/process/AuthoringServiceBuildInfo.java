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
    private final String protocolVersion;
    private final String contractIdentity;
    private final String buildIdentity;

    /** Stores validated embedded build and contract identity. */
    private AuthoringServiceBuildInfo(
            String serviceVersion,
            String engineVersion,
            String protocolVersion,
            String contractIdentity,
            String buildIdentity) {
        this.serviceVersion = requireValue(serviceVersion, "serviceVersion");
        this.engineVersion = requireValue(engineVersion, "engineVersion");
        this.protocolVersion = requireValue(protocolVersion, "protocolVersion");
        this.contractIdentity = requireValue(contractIdentity, "contractIdentity");
        this.buildIdentity = requireValue(buildIdentity, "buildIdentity");
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
                properties.getProperty("serviceVersion"),
                properties.getProperty("engineVersion"),
                properties.getProperty("protocolVersion"),
                properties.getProperty("contractIdentity"),
                properties.getProperty("buildIdentity"));
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

    /**
     * Returns the fixed development protocol version.
     *
     * @return development protocol version
     */
    public String protocolVersion() {
        return protocolVersion;
    }

    /**
     * Returns the stable development contract identity.
     *
     * @return development contract identity
     */
    public String contractIdentity() {
        return contractIdentity;
    }

    /**
     * Returns the source-derived development build identity.
     *
     * @return development build identity
     */
    public String buildIdentity() {
        return buildIdentity;
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
