/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the identifier grammar shared by persisted project documents. */
final class ProjectIdentifiersTest {
    @Test
    void acceptsValidProjectAndLocalIdentifiers() {
        assertThat(ProjectIdentifiers.isProjectId("io.github-glynch.jscene3d2")).isTrue();
        assertThat(ProjectIdentifiers.isLocalId("editor-2")).isTrue();
        assertThat(ProjectIdentifiers.isLocalId("a")).isTrue();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("io.github.glynch/editor-2"))
                .isTrue();
        assertThat(ProjectIdentifiers.isPortableLocator("assets/models/player.glb"))
                .isTrue();
    }

    @Test
    void rejectsMalformedProjectIdentifiers() {
        assertThat(ProjectIdentifiers.isProjectId("")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("Io.github")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("project")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId(".project")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("project.")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("project..name")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("project._name")).isFalse();
        assertThat(ProjectIdentifiers.isProjectId("project.name-")).isFalse();
    }

    @Test
    void rejectsMalformedLocalAndRegisteredTypeIdentifiers() {
        assertThat(ProjectIdentifiers.isLocalId("")).isFalse();
        assertThat(ProjectIdentifiers.isLocalId("-name")).isFalse();
        assertThat(ProjectIdentifiers.isLocalId("name-")).isFalse();
        assertThat(ProjectIdentifiers.isLocalId("name_value")).isFalse();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("missing")).isFalse();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("io.github.glynch/too/many"))
                .isFalse();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("io.github.glynch/")).isFalse();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("io._github/local")).isFalse();
        assertThat(ProjectIdentifiers.isRegisteredTypeId("io.github/-local")).isFalse();
    }

    @Test
    void rejectsUnsafePortableLocators() {
        assertThat(ProjectIdentifiers.isPortableLocator(" ")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("assets\\model.glb")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("/assets/model.glb")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("assets/model.glb/")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("assets//model.glb")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("assets/./model.glb")).isFalse();
        assertThat(ProjectIdentifiers.isPortableLocator("assets/../model.glb")).isFalse();
    }
}
