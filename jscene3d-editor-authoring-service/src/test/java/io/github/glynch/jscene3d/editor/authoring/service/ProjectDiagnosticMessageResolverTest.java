/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises feature-owned message resolution and fallback formatting. */
final class ProjectDiagnosticMessageResolverTest {
    private final ProjectDiagnosticMessageResolver resolver = new ProjectDiagnosticMessageResolver();

    /** Resolves a project-module resource using the requested locale. */
    @Test
    void resolvesFeatureOwnedBundleOutsideServiceModule() {
        ProjectDiagnostic diagnostic = diagnostic(ProjectDiagnosticCode.MANIFEST_JSON_INVALID, List.of());

        assertThat(resolver.resolve(diagnostic, Locale.ENGLISH))
                .isEqualTo("The project manifest is not valid Project Manifest JSON");
        assertThat(resolver.resolve(diagnostic, Locale.FRENCH))
                .isEqualTo("Le manifeste du projet n’est pas un manifeste JScene3D valide au format JSON");
    }

    /** Resolves the property-validation diagnostic family from its Java-owned French bundle. */
    @Test
    void resolvesLocalizedPropertyValidationMessage() {
        ProjectDiagnostic diagnostic = diagnostic(PropertyValidationDiagnosticCode.INTEGER, List.of("count"));

        assertThat(resolver.resolve(diagnostic, Locale.ENGLISH))
                .isEqualTo("Property count must be a mathematically integral number");
        assertThat(resolver.resolve(diagnostic, Locale.FRENCH))
                .isEqualTo("La propriété count doit être un nombre mathématiquement entier");
    }

    /** Formats the feature fallback with deterministic ordered arguments when no bundle is registered. */
    @Test
    void formatsMissingTranslationFromFeatureFallback() {
        ProjectDiagnostic diagnostic = diagnostic(TestDiagnosticCode.ORDERED, List.of("first", "second"));

        assertThat(resolver.resolve(diagnostic, Locale.JAPANESE)).isEqualTo("first precedes second");
    }

    private static ProjectDiagnostic diagnostic(DiagnosticCode code, List<Object> arguments) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                URI.create("file:///project/game.j3d"),
                "",
                arguments,
                Map.of());
    }

    /** Test-only diagnostic family deliberately absent from the production owner registry. */
    private enum TestDiagnosticCode implements DiagnosticCode {
        /** Pattern with observable positional ordering. */
        ORDERED;

        @Override
        public String code() {
            return "test.ordered";
        }

        @Override
        public String defaultMessage() {
            return "{0} precedes {1}";
        }
    }
}
