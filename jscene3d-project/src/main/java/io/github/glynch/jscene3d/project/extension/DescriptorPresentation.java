/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireOptionalNonBlank;

import java.util.Objects;
import java.util.Optional;

/** Human-readable descriptor metadata shared by editor and diagnostic surfaces. */
public final class DescriptorPresentation {
    private final String displayName;
    private final Optional<String> description;

    /** Stores validated presentation metadata. */
    private DescriptorPresentation(String displayName, Optional<String> description) {
        this.displayName = requireNonBlank(displayName, "displayName");
        this.description = requireOptionalNonBlank(description, "description");
    }

    /**
     * Creates presentation metadata without a description.
     *
     * @param displayName human-readable name
     * @return presentation metadata
     */
    public static DescriptorPresentation named(String displayName) {
        return new DescriptorPresentation(displayName, Optional.empty());
    }

    /**
     * Creates presentation metadata with a description.
     *
     * @param displayName human-readable name
     * @param description human-readable description
     * @return presentation metadata
     */
    public static DescriptorPresentation described(String displayName, String description) {
        return new DescriptorPresentation(displayName, Optional.of(description));
    }

    /**
     * Returns the human-readable name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the optional human-readable description.
     *
     * @return optional description
     */
    public Optional<String> description() {
        return description;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof DescriptorPresentation presentation
                && displayName.equals(presentation.displayName)
                && description.equals(presentation.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, description);
    }

    @Override
    public String toString() {
        return "DescriptorPresentation[displayName=" + displayName + ", description=" + description + ']';
    }
}
