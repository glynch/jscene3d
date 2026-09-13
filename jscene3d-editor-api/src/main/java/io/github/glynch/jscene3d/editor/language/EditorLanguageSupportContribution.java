/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import java.util.Objects;
import java.util.Set;

/**
 * Declares which source languages are handled by one project-scoped language adapter.
 *
 * @param id stable contribution identity
 * @param languages non-empty set of handled language identities
 */
public record EditorLanguageSupportContribution(EditorLanguageSupportId id, Set<EditorLanguageId> languages) {
    /** Copies and validates one language-support contribution. */
    public EditorLanguageSupportContribution {
        Objects.requireNonNull(id, "id");
        languages = Set.copyOf(Objects.requireNonNull(languages, "languages"));
        if (languages.isEmpty()) {
            throw new IllegalArgumentException("languages must not be empty");
        }
    }
}
