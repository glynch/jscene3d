/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Resolves and validates project-relative paths for project-format validators. */
public final class ProjectPathResolver {
    private final Path root;
    private final DiagnosticCollector diagnostics;
    private final PathDiagnosticCodes codes;
    private final ValidationContext fields;

    /**
     * Creates a resolver rooted at one normalized absolute project directory.
     *
     * @param root normalized absolute project root
     * @param diagnostics destination for validation diagnostics
     * @param codes feature-owned path diagnostic codes
     */
    public ProjectPathResolver(Path root, DiagnosticCollector diagnostics, PathDiagnosticCodes codes) {
        this.root = ProjectPaths.requireNormalizedAbsolute(root, "root");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.codes = Objects.requireNonNull(codes, "codes");
        fields = new ValidationContext(diagnostics, codes.fields());
    }

    /**
     * Validates and resolves one required project-relative path.
     *
     * @param value nullable authored path
     * @param location JSON Pointer location
     * @param warnIfMissing whether an absent resolved path produces a warning
     * @return validated normalized absolute path when resolution succeeds
     */
    public Optional<Path> resolveRequired(@Nullable String value, String location, boolean warnIfMissing) {
        String text = fields.requiredText(value, location);
        return text.isEmpty() ? Optional.empty() : resolve(text, location, warnIfMissing);
    }

    /**
     * Validates and resolves one optional project-relative path.
     *
     * @param value nullable authored path
     * @param location JSON Pointer location
     * @param warnIfMissing whether an absent resolved path produces a warning
     * @return validated normalized absolute path when present and resolution succeeds
     */
    public Optional<Path> resolveOptional(@Nullable String value, String location, boolean warnIfMissing) {
        Optional<String> text = fields.optionalText(value, location);
        return text.isEmpty() ? Optional.empty() : resolve(text.orElseThrow(), location, warnIfMissing);
    }

    /**
     * Resolves one portable relative path and confines it to the project root.
     *
     * @param value project-relative path text
     * @param location JSON Pointer location
     * @param warnIfMissing whether an absent resolved path produces a warning
     * @return validated normalized absolute path when resolution succeeds
     */
    public Optional<Path> resolve(String value, String location, boolean warnIfMissing) {
        if (value.indexOf('\\') >= 0) {
            diagnostics.error(codes.portable(), "project paths must use forward slashes", location);
            return Optional.empty();
        }
        try {
            return resolvePath(Path.of(value), value, location, warnIfMissing);
        } catch (InvalidPathException ignored) {
            diagnostics.error(codes.invalid(), "project path is invalid", location);
            return Optional.empty();
        }
    }

    /** Resolves a parsed relative path. */
    private Optional<Path> resolvePath(Path relative, String value, String location, boolean warnIfMissing) {
        if (relative.isAbsolute()) {
            diagnostics.error(codes.absolute(), "project path must be relative", location);
            return Optional.empty();
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            diagnostics.error(codes.escape(), "project path escapes the project directory", location);
            return Optional.empty();
        }
        if (!validateExistingPath(resolved, location)) {
            return Optional.empty();
        }
        if (warnIfMissing && Files.notExists(resolved)) {
            diagnostics.warning(codes.missing(), "referenced project path does not exist: " + value, location);
        }
        return Optional.of(resolved);
    }

    /** Rejects an existing symlink target outside the real project directory. */
    private boolean validateExistingPath(Path resolved, String location) {
        if (Files.notExists(resolved)) {
            return true;
        }
        try {
            if (!resolved.toRealPath().startsWith(root.toRealPath())) {
                diagnostics.error(codes.escape(), "project path resolves outside the project directory", location);
                return false;
            }
            return true;
        } catch (IOException exception) {
            diagnostics.error(codes.read(), "project path cannot be resolved: " + exception.getMessage(), location);
            return false;
        }
    }
}
