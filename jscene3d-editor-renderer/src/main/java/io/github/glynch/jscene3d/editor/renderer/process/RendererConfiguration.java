/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;

/** Validated process arguments supplied by the Electron structured-launch boundary. */
record RendererConfiguration(
        String electronBundleId, int initialWidth, int initialHeight, RendererProtocolVersion protocolVersion) {
    private static final String VERSION_OPTION = "--protocol-version=";

    static RendererConfiguration from(String[] arguments) {
        int offset;
        RendererProtocolVersion version;
        if (arguments.length == 3) {
            offset = 0;
            version = RendererProtocolVersion.CURRENT;
        } else if (arguments.length == 4 && arguments[0].startsWith(VERSION_OPTION)) {
            offset = 1;
            version = parseVersion(arguments[0].substring(VERSION_OPTION.length()));
        } else {
            throw new IllegalArgumentException(
                    "Expected [--protocol-version=<major>.<minor>] Electron bundle ID, width and height");
        }

        String bundleId = arguments[offset].strip();
        if (bundleId.isEmpty()) {
            throw new IllegalArgumentException("Electron bundle ID must not be blank");
        }
        int width = parseDimension(arguments[offset + 1], "width");
        int height = parseDimension(arguments[offset + 2], "height");
        if (!version.equals(RendererProtocolVersion.CURRENT)) {
            throw new IllegalArgumentException("Unsupported renderer protocol version: " + version);
        }
        return new RendererConfiguration(bundleId, width, height, version);
    }

    private static RendererProtocolVersion parseVersion(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Renderer protocol version must use <major>.<minor>");
        }
        try {
            return new RendererProtocolVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Renderer protocol version must contain integers", exception);
        }
    }

    private static int parseDimension(String value, String name) {
        try {
            int dimension = Integer.parseInt(value);
            if (dimension <= 0) {
                throw new IllegalArgumentException("Initial surface " + name + " must be positive");
            }
            return dimension;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Initial surface " + name + " must be an integer", exception);
        }
    }
}
