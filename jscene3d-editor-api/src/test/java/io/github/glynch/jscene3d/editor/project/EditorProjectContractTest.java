/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

final class EditorProjectContractTest {
    @Test
    void retainsPortableOpenedProjectIdentity() {
        EditorProject project = new EditorProject(
                "io.github.glynch.doomed-corridors", "Doomed Corridors", URI.create("file:///projects/doomed/"));

        assertThat(project.id()).isEqualTo("io.github.glynch.doomed-corridors");
        assertThat(project.name()).isEqualTo("Doomed Corridors");
        assertThat(project.root()).isEqualTo(URI.create("file:///projects/doomed/"));
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesOpenedProjectIdentity() {
        URI root = URI.create("file:///projects/doomed/");
        URI relativeRoot = URI.create("relative");

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorProject(null, "Doomed Corridors", root))
                .withMessage("id");
        assertThatThrownBy(() -> new EditorProject(" ", "Doomed Corridors", root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorProject("example.project", null, root))
                .withMessage("name");
        assertThatThrownBy(() -> new EditorProject("example.project", " ", root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorProject("example.project", "Project", null))
                .withMessage("root");
        assertThatThrownBy(() -> new EditorProject("example.project", "Project", relativeRoot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("root must be absolute");
    }
}
