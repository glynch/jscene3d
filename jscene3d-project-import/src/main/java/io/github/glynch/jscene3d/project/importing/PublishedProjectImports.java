/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Loads validated project import declarations without discovering or executing import providers. */
public final class PublishedProjectImports {
    private PublishedProjectImports() {
        throw new AssertionError("PublishedProjectImports cannot be instantiated");
    }

    /**
     * Loads every manifest-declared import and preserves structured failures in the thrown summary.
     *
     * @param project validated project declaring imports
     * @return immutable import definitions in manifest order
     * @throws IllegalStateException when any declared import is invalid
     */
    public static List<ImportDefinition> load(GameProject project) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        ImportLoader loader = new ImportLoader();
        List<ImportDefinition> definitions = new ArrayList<>();
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        for (Path importPath : validProject.imports()) {
            ImportLoadResult result = loader.load(validProject, importPath);
            result.definition().ifPresent(definitions::add);
            diagnostics.addAll(result.diagnostics());
        }
        if (!diagnostics.isEmpty()) {
            String summary = diagnostics.stream()
                    .map(diagnostic -> diagnostic.code().code() + " at " + diagnostic.source() + diagnostic.location())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("declared project imports are invalid: " + summary);
        }
        return List.copyOf(definitions);
    }
}
