/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Build-time protocol, contract, and source identity embedded in the renderer artifact. */
public final class RendererBuildInfo {
    private static final String RESOURCE = "renderer-build.properties";

    private final String protocolVersion;
    private final String contractIdentity;
    private final String buildIdentity;

    /** Stores validated embedded renderer build identity. */
    private RendererBuildInfo(String protocolVersion, String contractIdentity, String buildIdentity) {
        this.protocolVersion = requireValue(protocolVersion, "protocolVersion");
        this.contractIdentity = requireValue(contractIdentity, "contractIdentity");
        this.buildIdentity = requireValue(buildIdentity, "buildIdentity");
    }

    /**
     * Loads identity filtered into the renderer artifact.
     *
     * @return current renderer build information
     */
    public static RendererBuildInfo current() {
        Properties properties = new Properties();
        try (InputStream input = RendererBuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing renderer build information");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read renderer build information", exception);
        }
        return new RendererBuildInfo(
                properties.getProperty("protocolVersion"),
                properties.getProperty("contractIdentity"),
                properties.getProperty("buildIdentity"));
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

    private static String requireValue(String value, String name) {
        String candidate = Objects.requireNonNull(value, name).trim();
        if (candidate.isEmpty() || candidate.contains("${")) {
            throw new IllegalStateException("Invalid renderer build value: " + name);
        }
        return candidate;
    }
}
