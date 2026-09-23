/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

/**
 * Negotiated authoring-protocol version.
 *
 * @param major compatibility boundary
 * @param minor backward-compatible feature level
 */
public record ProtocolVersion(int major, int minor) {
    /** Current protocol version implemented by this service. */
    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 0);

    /** Validates non-negative version components. */
    public ProtocolVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Protocol version components must be non-negative");
        }
    }
}
