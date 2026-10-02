/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.protocol.RendererProtocolVersion;
import io.github.glynch.jscene3d.project.asset.AssetId;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Validated process arguments supplied by the Electron structured-launch boundary. */
record RendererConfiguration(
        String electronBundleId,
        int initialWidth,
        int initialHeight,
        RendererProtocolVersion protocolVersion,
        Optional<ProjectLaunch> projectLaunch) {
    private static final String VERSION_OPTION = "--protocol-version=";
    private static final String PROJECT_ROOT_OPTION = "--project-root=";
    private static final String PUBLISHED_CONTENT_OPTION = "--published-content-root=";
    private static final String ENGINE_VERSION_OPTION = "--engine-version=";
    private static final String PROJECT_ID_OPTION = "--project-id=";
    private static final String WORLD_ASSET_ID_OPTION = "--world-asset-id=";

    static RendererConfiguration from(String[] arguments) {
        if (arguments.length < 3) {
            throw usageFailure();
        }
        int processArgumentCount = arguments.length - 3;
        Map<String, String> options = new LinkedHashMap<>();
        RendererProtocolVersion version = RendererProtocolVersion.CURRENT;
        for (int index = 0; index < processArgumentCount; index++) {
            String argument = arguments[index];
            if (argument.startsWith(VERSION_OPTION)) {
                version = parseVersion(argument.substring(VERSION_OPTION.length()));
            } else if (!putOption(options, argument, PROJECT_ROOT_OPTION)
                    && !putOption(options, argument, PUBLISHED_CONTENT_OPTION)
                    && !putOption(options, argument, ENGINE_VERSION_OPTION)
                    && !putOption(options, argument, PROJECT_ID_OPTION)
                    && !putOption(options, argument, WORLD_ASSET_ID_OPTION)) {
                throw usageFailure();
            }
        }

        int offset = processArgumentCount;
        String bundleId = arguments[offset].strip();
        if (bundleId.isEmpty()) {
            throw new IllegalArgumentException("Electron bundle ID must not be blank");
        }
        int width = parseDimension(arguments[offset + 1], "width");
        int height = parseDimension(arguments[offset + 2], "height");
        if (!version.equals(RendererProtocolVersion.CURRENT)) {
            throw new IllegalArgumentException("Unsupported renderer protocol version: " + version);
        }
        return new RendererConfiguration(bundleId, width, height, version, projectLaunch(options));
    }

    private static Optional<ProjectLaunch> projectLaunch(Map<String, String> options) {
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ProjectLaunch(
                absolutePath(options, PROJECT_ROOT_OPTION),
                absolutePath(options, PUBLISHED_CONTENT_OPTION),
                option(options, ENGINE_VERSION_OPTION),
                option(options, PROJECT_ID_OPTION),
                AssetId.from(option(options, WORLD_ASSET_ID_OPTION))));
    }

    private static boolean putOption(Map<String, String> options, String argument, String prefix) {
        if (!argument.startsWith(prefix)) {
            return false;
        }
        String value = argument.substring(prefix.length());
        if (value.isBlank() || options.put(prefix, value) != null) {
            throw new IllegalArgumentException("Renderer option must occur once with a non-blank value: " + prefix);
        }
        return true;
    }

    private static String option(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Project renderer option is required: " + name);
        }
        return value;
    }

    private static Path absolutePath(Map<String, String> options, String name) {
        Path path = Path.of(option(options, name));
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Project renderer path must be absolute: " + name);
        }
        return path.normalize();
    }

    private static IllegalArgumentException usageFailure() {
        return new IllegalArgumentException(
                "Expected renderer options followed by Electron bundle ID, width and height");
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

    /** Java-owned semantic project launch retained independently of Electron surface identity. */
    record ProjectLaunch(
            Path projectRoot,
            Path publishedContentRoot,
            String engineVersion,
            String projectId,
            AssetId worldAssetId) {}
}
