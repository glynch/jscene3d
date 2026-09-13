/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

/** Verifies that the editor remains a named application module with no broad public export. */
final class EditorModuleDescriptorTest {

    @Test
    void runsTestsInTheEditorModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.editor");
    }

    @Test
    void opensOnlyTheUiPackagesToTheirJavaFxModules() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("javafx.graphics"))
                .singleElement()
                .satisfies(requirement -> assertThat(requirement.modifiers())
                        .doesNotContain(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        assertThat(descriptor.exports()).isEmpty();
        assertThat(descriptor.opens())
                .allSatisfy(opening -> assertThat(opening.isQualified()).isTrue())
                .extracting(ModuleDescriptor.Opens::source)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.editor", "io.github.glynch.jscene3d.editor.builtin.text");
        assertThat(descriptor.opens())
                .filteredOn(opening -> opening.source().equals("io.github.glynch.jscene3d.editor"))
                .singleElement()
                .satisfies(opening -> assertThat(opening.targets()).containsExactly("javafx.graphics"));
        assertThat(descriptor.opens())
                .filteredOn(opening -> opening.source().equals("io.github.glynch.jscene3d.editor.builtin.text"))
                .singleElement()
                .satisfies(opening -> assertThat(opening.targets()).containsExactly("javafx.web"));
    }
}
