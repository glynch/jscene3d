/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

/** Verifies the reusable authoring module's supported presentation boundary. */
final class AuthoringModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInTheAuthoringModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.editor.authoring");
    }

    /** Keeps presentation values available to external authoring adapters. */
    @Test
    void exportsThePresentationPackage() {
        assertThat(getClass().getModule().getDescriptor().exports())
                .extracting(ModuleDescriptor.Exports::source)
                .contains("io.github.glynch.jscene3d.editor.presentation");
    }

    /** Re-exports the message-source API used by the public presentation contract. */
    @Test
    void requiresI18nTransitively() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.i18n"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
