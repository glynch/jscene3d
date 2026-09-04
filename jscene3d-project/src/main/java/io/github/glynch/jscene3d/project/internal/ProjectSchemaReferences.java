/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Matches canonical and project-local schema references used by project documents. */
public final class ProjectSchemaReferences {
    private ProjectSchemaReferences() {
        throw new AssertionError("not instantiable");
    }

    /**
     * Tests whether an authored schema reference identifies one supported schema.
     *
     * @param projectRoot normalized absolute project root
     * @param documentSource normalized absolute document source
     * @param reference authored schema reference
     * @param canonicalUri canonical public schema URI
     * @param projectRelativeSchemaPath schema path relative to the project root
     * @return whether the reference identifies the supported schema
     */
    public static boolean matches(
            Path projectRoot,
            Path documentSource,
            String reference,
            String canonicalUri,
            String projectRelativeSchemaPath) {
        if (canonicalUri.equals(reference) || projectRelativeSchemaPath.equals(reference)) {
            return true;
        }
        if (reference.indexOf('\\') >= 0) {
            return false;
        }
        try {
            Path referencePath = Path.of(reference);
            Path sourceParent = documentSource.getParent();
            return !referencePath.isAbsolute()
                    && sourceParent != null
                    && sourceParent
                            .resolve(referencePath)
                            .normalize()
                            .equals(projectRoot.resolve(projectRelativeSchemaPath));
        } catch (InvalidPathException ignored) {
            return false;
        }
    }

    /**
     * Tests a schema reference for a resource whose source may not be a file.
     *
     * @param projectRoot normalized absolute project root
     * @param documentSource absolute logical document source
     * @param reference authored schema reference
     * @param canonicalUri canonical public schema URI
     * @param projectRelativeSchemaPath schema path relative to the project root
     * @return whether the reference identifies the supported schema
     */
    public static boolean matches(
            Path projectRoot,
            URI documentSource,
            String reference,
            String canonicalUri,
            String projectRelativeSchemaPath) {
        if (canonicalUri.equals(reference) || projectRelativeSchemaPath.equals(reference)) {
            return true;
        }
        if (!"file".equals(documentSource.getScheme())) {
            return false;
        }
        try {
            return matches(projectRoot, Path.of(documentSource), reference, canonicalUri, projectRelativeSchemaPath);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
