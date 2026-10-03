/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ImportedRuntimeResources;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.spatial3d.Material3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Mesh3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.standard.StandardProjectDescriptors;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Validates the permanent manual editor-development sandbox through public project APIs. */
final class EditorSandboxProjectTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final AssetId MAIN_SCENE_ID = AssetId.from("05931084-bd10-4f1c-b4d6-4182be5c1a2c");
    private static final AssetId SECOND_SCENE_ID = AssetId.from("747a501f-65ca-492f-92d8-6a0711c8d7a5");
    private static final AssetId CRATE_ID = AssetId.from("fd16cb58-68f1-4996-b267-a0b7a266fea6");
    private static final Path PROJECT_ROOT =
            Path.of("src", "main", "editor-sandbox").toAbsolutePath().normalize();

    /** Loads the manifest, extension metadata, catalog, and complete authored definition graph. */
    @Test
    void loadsCompleteSandboxProject() throws Exception {
        ProjectLoadResult projectResult = new ProjectLoader(ENGINE_VERSION).load(PROJECT_ROOT);

        assertThat(projectResult.diagnostics()).isEmpty();
        GameProject project = projectResult.project().orElseThrow();
        assertThat(project.identity().name()).isEqualTo("JScene3D Editor Sandbox");
        assertThat(project.runtime().mainScene()).contains(AssetRef.to(MAIN_SCENE_ID));
        assertThat(project.runtime().mainScene().orElseThrow().pathHint()).contains("scenes/main.scene.json");

        RegisteredTypeCatalog types = loadTypes(project);
        AssetCatalog assets = AssetCatalog.scan(PROJECT_ROOT).catalog().orElseThrow();
        assertThat(assets.assets()).hasSize(3);
        assertThat(assets.assets())
                .filteredOn(asset -> asset.kind() == AssetKind.SCENE_DEFINITION)
                .hasSize(2);
        assertThat(assets.assets())
                .filteredOn(asset -> asset.kind() == AssetKind.ENTITY_DEFINITION)
                .singleElement()
                .satisfies(asset -> assertThat(asset.id()).isEqualTo(CRATE_ID));

        SceneDefinition main = requireValid(assets.loadScene(AssetRef.to(MAIN_SCENE_ID), types));
        assertThat(main.name()).isEqualTo("Main");
        assertThat(main.roots())
                .extracting(entry -> entry.name().orElseThrow())
                .containsExactly("Cube", "Crate", "Directional Light");
        assertThat(main.roots().get(1))
                .isInstanceOfSatisfying(
                        EntityPlacement.class,
                        placement -> assertThat(placement.definition().id()).isEqualTo(CRATE_ID));

        SceneDefinition second = requireValid(assets.loadScene(AssetRef.to(SECOND_SCENE_ID), types));
        assertThat(second.name()).isEqualTo("Second");
        assertThat(second.roots())
                .extracting(entry -> entry.name().orElseThrow())
                .containsExactly("Sphere", "Directional Light");

        EntityDefinition crate = requireValid(assets.loadEntity(AssetRef.to(CRATE_ID), types));
        assertThat(crate.name()).isEqualTo("Crate");
        assertThat(crate.root().components()).hasSize(2);
    }

    /** Acquires every mesh and material through the production runtime resource boundary. */
    @Test
    void loadsAllSandboxResources() throws Exception {
        GameProject project =
                new ProjectLoader(ENGINE_VERSION).load(PROJECT_ROOT).project().orElseThrow();
        RegisteredTypeCatalog types = loadTypes(project);
        RuntimeResourceProvider resources =
                ImportedRuntimeResources.create(project, types, Spatial3dResourceLoaders.all());

        assertMesh(resources, "sandbox-box-mesh");
        assertMesh(resources, "sandbox-sphere-mesh");
        assertMaterial(resources, "sandbox-cube-material");
        assertMaterial(resources, "sandbox-crate-material");
        assertMaterial(resources, "sandbox-sphere-material");
    }

    /** Loads project-local metadata and combines it with the safe built-in descriptors used by the editor. */
    private static RegisteredTypeCatalog loadTypes(GameProject project) throws Exception {
        URL resources = PROJECT_ROOT.resolve("src/main/resources").toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {resources}, ClassLoader.getPlatformClassLoader())) {
            ExtensionCatalogLoadResult result = new ExtensionCatalogLoader(ENGINE_VERSION).load(project, loader);
            assertThat(result.diagnostics()).isEmpty();
            List<ExtensionDescriptor> descriptors =
                    new ArrayList<>(result.catalog().extensions());
            descriptors.addAll(StandardProjectDescriptors.all());
            return RegisteredTypeCatalog.of(descriptors);
        }
    }

    /** Returns a definition only after proving that its complete validation result is clean. */
    private static <T> T requireValid(DefinitionLoadResult<T> result) {
        assertThat(result.diagnostics()).isEmpty();
        return result.definition().orElseThrow();
    }

    /** Acquires and releases one generated mesh resource. */
    private static void assertMesh(RuntimeResourceProvider resources, String assetId) throws Exception {
        try (RuntimeResourceLease<Mesh3dResource> lease =
                resources.acquire(ResourceReference.asset(assetId), Mesh3dResource.class)) {
            assertThat(lease.value()).isNotNull();
        }
    }

    /** Acquires and releases one generated standard-material resource. */
    private static void assertMaterial(RuntimeResourceProvider resources, String assetId) throws Exception {
        try (RuntimeResourceLease<Material3dResource> lease =
                resources.acquire(ResourceReference.asset(assetId), Material3dResource.class)) {
            assertThat(lease.value()).isNotNull();
        }
    }
}
