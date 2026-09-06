/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises deterministic hashing of the complete portable project-value algebra. */
final class ImportHashesTest {
    private static final EntityId ENTITY = EntityId.from("4f62196b-f9ec-4fa0-969b-d36b13a5719e");
    private static final ComponentId COMPONENT = ComponentId.from("e3949b66-63fb-4205-a5bb-b40ba5f8ac25");
    private static final RegisteredType IMPORTER = new RegisteredType("example.test/importer", 1);

    /** Includes stable entity and component identities in a definition fingerprint. */
    @Test
    void hashesAuthoredTargetValues(@TempDir Path temporaryDirectory) {
        ProjectValue entityTarget = new ProjectValue.EntityTargetValue(ENTITY);
        ProjectValue componentTarget = new ProjectValue.ComponentTargetValue(new ComponentTarget(ENTITY, COMPONENT));
        ImportDefinition targets =
                definition(temporaryDirectory, Map.of("entity", entityTarget, "component", componentTarget));
        ImportDefinition onlyEntity = definition(temporaryDirectory, Map.of("entity", entityTarget));

        assertThat(ImportHashes.definition(targets, IMPORTER))
                .isEqualTo(ImportHashes.definition(targets, IMPORTER))
                .isNotEqualTo(ImportHashes.definition(onlyEntity, IMPORTER));
    }

    /** Creates one otherwise identical import definition with the supplied settings. */
    private static ImportDefinition definition(Path directory, Map<String, ProjectValue> settings) {
        GameProject.AssetSource asset = new GameProject.AssetSource(
                "source", "example.test/source", directory.resolve("source.bin"), Optional.empty());
        return new ImportDefinition(
                directory.resolve("source.import.json"),
                "source-import",
                asset,
                IMPORTER.id(),
                List.of(),
                settings,
                Map.of());
    }
}
