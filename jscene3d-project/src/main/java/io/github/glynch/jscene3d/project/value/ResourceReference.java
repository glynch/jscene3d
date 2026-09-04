/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.value;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePortableLocator;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Portable reference to project content used by a generic {@link ProjectValue}. */
public final class ResourceReference {
    private final Kind kind;
    private final String locator;
    private final Optional<Path> projectPath;

    /** Stores one validated reference. */
    private ResourceReference(Kind kind, String locator, Optional<Path> projectPath) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.locator = requireNonBlank(locator, "locator");
        this.projectPath = Objects.requireNonNull(projectPath, "projectPath");
        if ((kind == Kind.PROJECT) != projectPath.isPresent()) {
            throw new IllegalArgumentException("only project references have a resolved project path");
        }
        projectPath.ifPresent(path -> requireNormalizedAbsolute(path, "projectPath"));
    }

    /**
     * Creates a project-file reference.
     *
     * @param locator portable project-relative locator
     * @param resolvedPath normalized absolute path confined to the project root
     * @return project reference
     */
    public static ResourceReference project(String locator, Path resolvedPath) {
        return new ResourceReference(
                Kind.PROJECT, requirePortableLocator(locator, "locator"), Optional.of(resolvedPath));
    }

    /**
     * Creates a source-asset reference.
     *
     * @param assetId declared source-asset identifier
     * @return asset reference
     */
    public static ResourceReference asset(String assetId) {
        return new ResourceReference(Kind.ASSET, requireLocalId(assetId, "assetId"), Optional.empty());
    }

    /**
     * Creates an imported-output reference.
     *
     * @param locator importer and output locator
     * @return imported-output reference
     */
    public static ResourceReference imported(String locator) {
        String validLocator = requireNonBlank(locator, "locator");
        int separator = validLocator.indexOf('/');
        if (separator < 1 || separator == validLocator.length() - 1) {
            throw new IllegalArgumentException("import locator must contain an import id and output locator");
        }
        requireLocalId(validLocator.substring(0, separator), "importId");
        requirePortableLocator(validLocator.substring(separator + 1), "importOutput");
        return new ResourceReference(Kind.IMPORT, validLocator, Optional.empty());
    }

    /**
     * Returns the reference namespace.
     *
     * @return reference kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the portable locator without its namespace prefix.
     *
     * @return reference locator
     */
    public String locator() {
        return locator;
    }

    /**
     * Returns the resolved path for a {@link Kind#PROJECT} reference.
     *
     * @return resolved project path, or empty for non-file references
     */
    public Optional<Path> projectPath() {
        return projectPath;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ResourceReference reference
                && kind == reference.kind
                && locator.equals(reference.locator)
                && projectPath.equals(reference.projectPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, locator, projectPath);
    }

    @Override
    public String toString() {
        return kind.prefix + locator;
    }

    /** Supported resource-reference namespaces. */
    public enum Kind {
        /** File within the project directory. */
        PROJECT("project:"),
        /** Authoritative source asset declared by the manifest. */
        ASSET("asset:"),
        /** Named output produced by an import definition. */
        IMPORT("import:");

        private final String prefix;

        /** Stores the serialized namespace prefix. */
        Kind(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns the serialized namespace prefix.
         *
         * @return prefix including its trailing colon
         */
        public String prefix() {
            return prefix;
        }
    }
}
