/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies complete definition replacement through sibling temporary files. */
class DefinitionWriterTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void replacesCompleteDefinitionWithoutLeavingTemporaryFiles() throws IOException {
        Path target = temporaryDirectory.resolve("worlds/test.world.json");
        WorldDefinition first = world("First", true);
        WorldDefinition second = world("Second", false);

        DefinitionWriter.write(target, first);
        DefinitionWriter.write(target, second);

        assertThat(Files.readString(target)).contains("\"name\" : \"Second\"").contains("\"enabled\" : false");
        try (var files = Files.list(target.getParent())) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("test.world.json");
        }
    }

    private static WorldDefinition world(String name, boolean enabled) {
        LocalEntity entity = new LocalEntity(
                new EntityId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                name,
                enabled,
                List.of(),
                List.of());
        return new WorldDefinition(
                new AssetId(UUID.fromString("00000000-0000-0000-0000-000000000002")), name, List.of(entity));
    }
}
