/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the supported wire API and encapsulated authoring-service implementation boundary. */
final class AuthoringServiceModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInTheAuthoringServiceModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.editor.authoring.service");
    }

    /** Exports only the supported protocol and framing packages. */
    @Test
    void exportsOnlySupportedWirePackages() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.editor.authoring.protocol",
                        "io.github.glynch.jscene3d.editor.authoring.protocol.framing")
                .doesNotContain("io.github.glynch.jscene3d.editor.authoring.service");
    }

    /** Keeps the reusable authoring JPMS module as a non-transitive implementation dependency. */
    @Test
    void requiresAuthoringWithoutTransitiveReadability() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.editor.authoring"))
                .singleElement()
                .satisfies(requirement -> assertThat(requirement.modifiers())
                        .doesNotContain(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
