/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.extension.ExtensionDiagnosticCode;
import io.github.glynch.jscene3d.project.imports.ImportDefinitionDiagnosticCode;
import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import io.github.glynch.jscene3d.project.resource.ResourceDiagnosticCode;
import io.github.glynch.jscene3d.project.scene.SceneDiagnosticCode;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Verifies the stable localization catalog exposed by project-definition features. */
final class DiagnosticCodeCatalogTest {
    /** Requires globally unique keys and non-blank English fallbacks. */
    @Test
    void validatesProjectDiagnosticCatalog() {
        DiagnosticCode[] codes = Stream.of(
                        ProjectDiagnosticCode.values(),
                        SceneDiagnosticCode.values(),
                        ResourceDiagnosticCode.values(),
                        ExtensionDiagnosticCode.values(),
                        ImportDefinitionDiagnosticCode.values())
                .flatMap(Arrays::stream)
                .toArray(DiagnosticCode[]::new);

        assertThat(codes)
                .extracting(DiagnosticCode::code)
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).isNotBlank());
        assertThat(codes)
                .extracting(DiagnosticCode::defaultMessage)
                .allSatisfy(message -> assertThat(message).isNotBlank());
    }
}
