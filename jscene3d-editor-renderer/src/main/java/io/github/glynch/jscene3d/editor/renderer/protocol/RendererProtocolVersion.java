/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.protocol;

/**
 * Version of the line-framed editor-renderer protocol.
 *
 * @param major fixed pre-release protocol major
 * @param minor fixed pre-release protocol minor
 */
public record RendererProtocolVersion(int major, int minor) {
    /** Current product protocol version. */
    public static final RendererProtocolVersion CURRENT = new RendererProtocolVersion(1, 0);

    /** Validates a non-negative protocol version. */
    public RendererProtocolVersion {
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Protocol version parts must be non-negative");
        }
    }

    /** Returns the stable wire representation. */
    @Override
    public String toString() {
        return major + "." + minor;
    }
}
