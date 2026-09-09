/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import org.junit.jupiter.api.Test;

/** Specifies the complete standard descriptor set shared by game hosts and authoring tools. */
final class StandardGameDescriptorsTest {
    /** Preserves dependency order and the complete set of standard extension identities. */
    @Test
    void returnsCompleteStandardSet() {
        assertThat(StandardGameDescriptors.all())
                .extracting(ExtensionDescriptor::id)
                .containsExactly(
                        "io.github.glynch.jscene3d.spatial3d",
                        "io.github.glynch.jscene3d.physics3d",
                        "io.github.glynch.jscene3d.game3d",
                        "io.github.glynch.jscene3d.presentation");
    }
}
