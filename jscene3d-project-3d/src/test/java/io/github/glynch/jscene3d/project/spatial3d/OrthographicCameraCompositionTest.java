/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldCompositionResult;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises authored orthographic cameras through public composition and lifecycle boundaries. */
final class OrthographicCameraCompositionTest {
    private static final AssetId SCENE_ID = AssetId.from("16663e23-0cd8-42b9-8437-cb04bf01d91f");
    private static final EntityId PARENT_ID = EntityId.from("e2589d55-3f2c-42ce-a2ec-d7cb0a39b21c");
    private static final EntityId CAMERA_ID = EntityId.from("6854b7ee-48f4-44c2-8ee4-a9157b1384c0");
    private static final EntityId OTHER_ID = EntityId.from("d7da1888-0ff1-4ef0-82d2-1dcc31ca70c3");
    private static final ComponentId PARENT_TRANSFORM = ComponentId.from("fb4ec9a9-3950-47a1-aa92-2906b665c2a6");
    private static final ComponentId CAMERA_TRANSFORM = ComponentId.from("81be4f55-8e2a-48a1-a876-20f447a26526");
    private static final ComponentId CAMERA = ComponentId.from("46305579-8d6c-4a7c-bec9-6e771e6dc141");
    private static final ComponentId OTHER_TRANSFORM = ComponentId.from("a509fb40-5d25-47b0-afda-3c0f90db4d66");
    private static final ComponentId OTHER_CAMERA = ComponentId.from("9009aeb9-7340-4c67-b0c8-881093d8bd16");

    @TempDir
    private Path temporaryDirectory;

    /** Instantiates exact projection state and propagates the standard parent-child world transform. */
    @Test
    void composesOrthographicCameraWithWorldTransform() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        try (World world = compose(orthographicScene(true), spatial).world().orElseThrow()) {
            Entity cameraEntity = world.roots().getFirst().children().getFirst();
            OrthographicCamera3d camera =
                    cameraEntity.component(CAMERA, OrthographicCamera3d.class).orElseThrow();

            assertThat(camera.left()).isEqualTo(-8.0F);
            assertThat(camera.right()).isEqualTo(8.0F);
            assertThat(camera.top()).isEqualTo(5.0F);
            assertThat(camera.bottom()).isEqualTo(-5.0F);
            assertThat(camera.near()).isZero();
            assertThat(camera.far()).isEqualTo(250.0F);
            assertThat(camera.zoom()).isEqualTo(2.0F);
            assertThat(camera.isPrimary()).isTrue();

            world.activate();
            Vector3f translation =
                    spatial.activePrimaryCameraTransform().worldMatrix().getTranslation(new Vector3f());
            assertThat(List.of(translation.x, translation.y, translation.z)).containsExactly(10.0F, 2.0F, 3.0F);
        }
    }

    /** Applies bounds, clipping, and zoom mutations through the engine-backed component. */
    @Test
    void mutatesProjectionAtomically() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        OrthographicCamera3d camera;
        try (World world = compose(orthographicScene(true), spatial).world().orElseThrow()) {
            camera = world.roots()
                    .getFirst()
                    .children()
                    .getFirst()
                    .component(CAMERA, OrthographicCamera3d.class)
                    .orElseThrow();
            camera.setBounds(-4.0F, 6.0F, 7.0F, -3.0F);
            camera.setClippingPlanes(1.0F, 500.0F);
            camera.setZoom(3.0F);

            assertThat(List.of(camera.left(), camera.right(), camera.top(), camera.bottom()))
                    .containsExactly(-4.0F, 6.0F, 7.0F, -3.0F);
            assertThat(List.of(camera.near(), camera.far(), camera.zoom())).containsExactly(1.0F, 500.0F, 3.0F);
            assertThatThrownBy(() -> camera.setBounds(1.0F, 1.0F, 2.0F, -2.0F))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(camera.left()).isEqualTo(-4.0F);
        }
        assertThat(camera.isClosed()).isTrue();
    }

    /** Rejects invalid state at the typed public projection boundary before runtime registration. */
    @Test
    void validatesProjectionValueObject() {
        assertThat(new OrthographicProjection3d(-1.0F, 1.0F, 1.0F, -1.0F, 0.0F, 1000.0F, 1.0F))
                .isNotNull();
        assertThatThrownBy(() -> new OrthographicProjection3d(Float.NaN, 1.0F, 1.0F, -1.0F, 0.0F, 1000.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrthographicProjection3d(1.0F, 1.0F, 1.0F, -1.0F, 0.0F, 1000.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrthographicProjection3d(-1.0F, 1.0F, -1.0F, -1.0F, 0.0F, 1000.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrthographicProjection3d(-1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1000.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrthographicProjection3d(-1.0F, 1.0F, 1.0F, -1.0F, 1.0F, 1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrthographicProjection3d(-1.0F, 1.0F, 1.0F, -1.0F, 0.0F, 1000.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Supports several orthographic cameras while applying descriptor defaults to omitted properties. */
    @Test
    void composesMultipleOrthographicCamerasWithDefaults() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        SceneDefinition scene = new SceneDefinition(
                SCENE_ID,
                "Multiple orthographic cameras",
                List.of(orthographicScene(true).roots().getFirst(), orthographicEntity(false)));

        try (World world = compose(scene, spatial).world().orElseThrow()) {
            OrthographicCamera3d secondary = world.roots()
                    .get(1)
                    .component(OTHER_CAMERA, OrthographicCamera3d.class)
                    .orElseThrow();

            assertThat(List.of(
                            secondary.left(),
                            secondary.right(),
                            secondary.top(),
                            secondary.bottom(),
                            secondary.near(),
                            secondary.far(),
                            secondary.zoom()))
                    .containsExactly(-1.0F, 1.0F, 1.0F, -1.0F, 0.0F, 1000.0F, 1.0F);
            world.activate();
            assertThat(spatial.isReadyToRender()).isTrue();
        }
    }

    /** Uses one shared primary registry and preserves rejection of ambiguous mixed-camera scenes. */
    @Test
    void rejectsMixedPrimaryCameraDeclarations() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        SceneDefinition scene = new SceneDefinition(
                SCENE_ID,
                "Mixed cameras",
                List.of(orthographicScene(true).roots().getFirst(), perspectiveEntity(true)));

        WorldCompositionResult result = compose(scene, spatial);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(RuntimeDiagnosticCode.FACTORY_CREATE_FAILED);
            assertThat(diagnostic.location()).isEqualTo("/roots/1/components/1");
        });
        assertThat(spatial.isReadyToRender()).isFalse();
        spatial.close();
    }

    /** Preserves no-primary fallback and clears readiness when the primary camera entity is destroyed. */
    @Test
    void followsPrimaryLifecycleWithoutFallback() throws IOException {
        Spatial3dWorldModule withoutPrimary = Spatial3dAdapters.standard();
        try (World world =
                compose(orthographicScene(false), withoutPrimary).world().orElseThrow()) {
            world.activate();
            assertThat(withoutPrimary.isReadyToRender()).isFalse();
        }

        Spatial3dWorldModule withPrimary = Spatial3dAdapters.standard();
        try (World world = compose(orthographicScene(true), withPrimary).world().orElseThrow()) {
            Entity cameraEntity = world.roots().getFirst().children().getFirst();
            world.activate();
            assertThat(withPrimary.isReadyToRender()).isTrue();
            world.disable(cameraEntity);
            assertThat(withPrimary.isReadyToRender()).isFalse();
            world.enable(cameraEntity);
            world.destroy(cameraEntity);
            assertThat(withPrimary.isReadyToRender()).isFalse();
        }
    }

    /** Composes through the real catalog, descriptor, runtime-extension, and world-module seams. */
    private WorldCompositionResult compose(SceneDefinition definition, Spatial3dWorldModule spatial)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("orthographic.scene.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        return WorldComposer.compose(
                assets,
                AssetRef.to(definition.id()),
                types,
                List.of(new Spatial3dRuntimeExtension()),
                List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                noResources());
    }

    /** Creates a parented authored orthographic camera with explicit projection values. */
    private static SceneDefinition orthographicScene(boolean primary) {
        ComponentDefinition transform = component(
                CAMERA_TRANSFORM,
                Spatial3dDescriptors.transformType(),
                Map.of(Spatial3dDescriptors.positionProperty(), array(0.0F, 1.0F, 1.5F)));
        ComponentDefinition camera = component(
                CAMERA,
                Spatial3dDescriptors.orthographicCameraType(),
                Map.of(
                        Spatial3dDescriptors.leftProperty(), number(-8.0F),
                        Spatial3dDescriptors.rightProperty(), number(8.0F),
                        Spatial3dDescriptors.topProperty(), number(5.0F),
                        Spatial3dDescriptors.bottomProperty(), number(-5.0F),
                        Spatial3dDescriptors.nearProperty(), number(0.0F),
                        Spatial3dDescriptors.farProperty(), number(250.0F),
                        Spatial3dDescriptors.zoomProperty(), number(2.0F),
                        Spatial3dDescriptors.primaryProperty(), new ProjectValue.BooleanValue(primary)));
        LocalEntity child = new LocalEntity(CAMERA_ID, "Orthographic", true, List.of(transform, camera), List.of());
        ComponentDefinition parentTransform = component(
                PARENT_TRANSFORM,
                Spatial3dDescriptors.transformType(),
                Map.of(
                        Spatial3dDescriptors.positionProperty(), array(10.0F, 0.0F, 0.0F),
                        Spatial3dDescriptors.scaleProperty(), array(2.0F, 2.0F, 2.0F)));
        LocalEntity parent = new LocalEntity(PARENT_ID, "Rig", true, List.of(parentTransform), List.of(child));
        return new SceneDefinition(SCENE_ID, "Orthographic camera", List.of(parent));
    }

    /** Creates a perspective camera used to verify mixed primary-camera policy. */
    private static LocalEntity perspectiveEntity(boolean primary) {
        return new LocalEntity(
                OTHER_ID,
                "Perspective",
                true,
                List.of(
                        component(OTHER_TRANSFORM, Spatial3dDescriptors.transformType(), Map.of()),
                        component(
                                OTHER_CAMERA,
                                Spatial3dDescriptors.perspectiveCameraType(),
                                Map.of(
                                        Spatial3dDescriptors.primaryProperty(),
                                        new ProjectValue.BooleanValue(primary)))),
                List.of());
    }

    /** Creates an orthographic camera using projection defaults. */
    private static LocalEntity orthographicEntity(boolean primary) {
        return new LocalEntity(
                OTHER_ID,
                "Secondary orthographic",
                true,
                List.of(
                        component(OTHER_TRANSFORM, Spatial3dDescriptors.transformType(), Map.of()),
                        component(
                                OTHER_CAMERA,
                                Spatial3dDescriptors.orthographicCameraType(),
                                Map.of(
                                        Spatial3dDescriptors.primaryProperty(),
                                        new ProjectValue.BooleanValue(primary)))),
                List.of());
    }

    /** Creates one component with stable identity. */
    private static ComponentDefinition component(
            ComponentId id, ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(id, type.id(), type.version(), properties);
    }

    /** Creates one portable number. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue array(float... values) {
        return new ProjectValue.ArrayValue(IntStream.range(0, values.length)
                .mapToObj(index -> number(values[index]))
                .map(ProjectValue.class::cast)
                .toList());
    }

    /** Returns a provider which rejects the resource-free fixture's unexpected requests. */
    private static RuntimeResourceProvider noResources() {
        return new RuntimeResourceProvider() {
            @Override
            public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                throw new IllegalStateException("camera fixture has no resources");
            }
        };
    }
}
