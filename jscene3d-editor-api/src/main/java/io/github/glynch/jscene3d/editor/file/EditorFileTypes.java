/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.net.URI;
import java.util.Optional;

/** Registry and resolver for extension-contributed workspace file types. */
public interface EditorFileTypes {
    /**
     * Registers a file type for the activating extension's lifetime.
     *
     * @param fileType complete file-type contribution
     * @return removable registration
     */
    EditorRegistration register(EditorFileType fileType);

    /**
     * Resolves the most specific contributed type for a file resource.
     *
     * @param resource file resource URI
     * @return matching type, or empty for the plain-text/binary fallback
     */
    Optional<EditorFileType> resolve(URI resource);
}
