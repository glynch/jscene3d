/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import java.nio.file.Path;
import java.util.Objects;

/** User-facing metadata for one reversible authoring operation.
 *
 * @param label author-facing operation label
 * @param source affected authored source
 */
public record AuthoringOperation(AuthoringText label, Path source) {
    /** Normalizes the affected authored source. */
    public AuthoringOperation {
        Objects.requireNonNull(label, "label");
        source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    }
}
