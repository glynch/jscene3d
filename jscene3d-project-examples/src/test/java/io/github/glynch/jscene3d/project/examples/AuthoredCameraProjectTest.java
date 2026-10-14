/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.standard.StandardProjectDescriptors;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Validates the focused mixed authored-camera fixture through public project serialization APIs. */
final class AuthoredCameraProjectTest {
    private static final AssetId SCENE_ID = AssetId.from("53bb0d96-eb30-4481-a3ef-351951dcfbc5");
    private static final Path PROJECT_ROOT =
            Path.of("src", "main", "camera-project").toAbsolutePath().normalize();

    @TempDir
    private Path temporaryDirectory;

    /** Loads both camera types and preserves the complete scene through generic serialization. */
    @Test
    void loadsAndRoundTripsMixedAuthoredCameras() throws Exception {
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(StandardProjectDescriptors.all());
        AssetCatalog assets = AssetCatalog.scan(PROJECT_ROOT).catalog().orElseThrow();
        SceneDefinition scene = requireValid(assets.loadScene(AssetRef.to(SCENE_ID), types));

        assertThat(scene.roots()).hasSize(2);
        assertThat(scene.roots()).allMatch(LocalEntity.class::isInstance);
        assertThat(((LocalEntity) scene.roots().get(0)).components())
                .anyMatch(component -> component
                                .type()
                                .equals(StandardSpatial3dDescriptors.perspectiveCameraType()
                                        .id())
                        && component.typeVersion()
                                == StandardSpatial3dDescriptors.perspectiveCameraType()
                                        .version());
        assertThat(((LocalEntity) scene.roots().get(1)).components())
                .anyMatch(component -> component
                                .type()
                                .equals(StandardSpatial3dDescriptors.orthographicCameraType()
                                        .id())
                        && component.typeVersion()
                                == StandardSpatial3dDescriptors.orthographicCameraType()
                                        .version());

        Path written = temporaryDirectory.resolve("cameras.scene.json");
        DefinitionWriter.write(written, scene);
        SceneDefinition reloaded = requireValid(
                AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow().loadScene(AssetRef.to(SCENE_ID), types));
        assertThat(reloaded).isEqualTo(scene);
    }

    /** Returns a definition only after proving its descriptor-backed validation result is clean. */
    private static <T> T requireValid(DefinitionLoadResult<T> result) {
        assertThat(result.diagnostics()).isEmpty();
        return result.definition().orElseThrow();
    }
}
