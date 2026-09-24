/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies that the reusable authoring session and named module remain runtime-free. */
final class EditorAuthoringBoundaryTest {
    /** Retains safe definitions without exposing a runtime-content aggregate. */
    @Test
    void exposesSafeDefinitionsWithoutRuntimeContent() throws NoSuchMethodException {
        Method definitions = EditorProjectSession.class.getMethod("definitions");

        assertThat(definitions.getReturnType()).isEqualTo(DefinitionResolver.class);
        assertThat(EditorProjectSession.class.getMethods())
                .extracting(Method::getName)
                .doesNotContain("content");
    }

    /** Reads safe project/import modules and excludes executable runtime verticals. */
    @Test
    void declaresSafeNamedModuleDependencies() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> requires = descriptor.requires().stream()
                .map(ModuleDescriptor.Requires::name)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(requires)
                .contains(
                        "io.github.glynch.jscene3d.editor.api",
                        "io.github.glynch.jscene3d.project",
                        "io.github.glynch.jscene3d.project.importing")
                .doesNotContain(
                        "io.github.glynch.jscene3d.game",
                        "io.github.glynch.jscene3d.project.runtime",
                        "io.github.glynch.jscene3d.project.spatial3d",
                        "io.github.glynch.jscene3d.project.physics3d",
                        "io.github.glynch.jscene3d.lwjgl",
                        "io.github.glynch.jscene3d.audio");
    }
}
