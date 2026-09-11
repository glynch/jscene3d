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
    void opensItsApplicationPackageOnlyToJavaFx() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("javafx.graphics"))
                .singleElement()
                .satisfies(requirement -> assertThat(requirement.modifiers())
                        .doesNotContain(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        assertThat(descriptor.exports()).isEmpty();
        assertThat(descriptor.opens())
                .singleElement()
                .returns("io.github.glynch.jscene3d.editor", ModuleDescriptor.Opens::source)
                .returns(true, ModuleDescriptor.Opens::isQualified)
                .satisfies(opening -> assertThat(opening.targets()).containsExactly("javafx.graphics"));
    }
}
