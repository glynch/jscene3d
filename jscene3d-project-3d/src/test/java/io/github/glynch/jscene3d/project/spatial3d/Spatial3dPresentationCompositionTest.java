/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
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
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.Offset;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises descriptor-backed presentation through supported world and component interfaces. */
final class Spatial3dPresentationCompositionTest {
    private static final AssetId WORLD_ID = AssetId.from("280d958d-7220-4b16-96b1-d6f523288980");
    private static final ComponentId CAMERA = ComponentId.from("f88c23a4-5b70-4af5-b409-e991fa1abf14");
    private static final ComponentId LIGHT = ComponentId.from("25694276-072e-4f76-91bb-a0da0d96e9e0");
    private static final ComponentId RENDERER = ComponentId.from("0dcce20a-9fe3-4583-bc7b-fca33062a3d1");
    private static final ComponentId BILLBOARD = ComponentId.from("fb616ed9-c194-47ec-be93-09d0ec5b70f9");
    private static final ComponentId TRANSFORM = ComponentId.from("93a714ee-a89e-40e4-9801-24b48a6b6824");
    private static final ResourceReference MESH_REFERENCE = ResourceReference.asset("cube-mesh");
    private static final ResourceReference MATERIAL_REFERENCE = ResourceReference.asset("blue-material");
    private static final ResourceReference BASIC_MATERIAL_REFERENCE = ResourceReference.asset("sprite-material");

    @TempDir
    private Path temporaryDirectory;

    /** Composes one complete descriptor-backed presentation world with its authored values. */
    @Test
    void composesPresentationWithAuthoredValues() throws IOException {
        TestResources resources = new TestResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(presentationWorld(), spatial, resources).world().orElseThrow();
        Entity cameraEntity = world.roots().get(0);
        Entity lightEntity = world.roots().get(1);
        Entity meshEntity = world.roots().get(2);
        Entity billboardEntity = world.roots().get(3);
        PerspectiveCamera3d camera =
                cameraEntity.component(CAMERA, PerspectiveCamera3d.class).orElseThrow();
        DirectionalLight3d light =
                lightEntity.component(LIGHT, DirectionalLight3d.class).orElseThrow();
        MeshRenderer3d renderer =
                meshEntity.component(RENDERER, MeshRenderer3d.class).orElseThrow();
        BillboardRenderer3d billboard =
                billboardEntity.component(BILLBOARD, BillboardRenderer3d.class).orElseThrow();

        assertThat(spatial.isReadyToRender()).isFalse();
        assertThat(camera.fieldOfViewDegrees()).isCloseTo(50.0F, within(0.0001F));
        assertThat(camera.near()).isEqualTo(0.25F);
        assertThat(camera.far()).isEqualTo(200.0F);
        assertThat(camera.isPrimary()).isTrue();
        assertThat(light.intensity()).isEqualTo(2.0F);
        assertThat(coordinates(light.target())).containsExactly(0.0F, 0.0F, 0.0F);
        assertThat(renderer.mesh()).isSameAs(resources.mesh);
        assertThat(renderer.material()).isSameAs(resources.material);
        assertThat(renderer.isVisible()).isTrue();
        assertThat(billboard.material()).isSameAs(resources.basicMaterial);
        assertThat(billboard.size().x()).isEqualTo(2.0F);
        assertThat(billboard.size().y()).isEqualTo(3.0F);
        assertThat(billboard.anchor().x()).isEqualTo(0.5F);
        assertThat(billboard.anchor().y()).isEqualTo(0.0F);
        assertThat(billboard.alignment()).isEqualTo(BillboardAlignment.CYLINDRICAL);
        assertThat(billboard.isVisible()).isTrue();

        world.close();
    }

    /** Mutates descriptor-backed camera, lighting, mesh, and billboard presentation state. */
    @Test
    void mutatesPresentation() throws IOException {
        TestResources resources = new TestResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(presentationWorld(), spatial, resources).world().orElseThrow();
        Entity cameraEntity = world.roots().get(0);
        Entity lightEntity = world.roots().get(1);
        Entity meshEntity = world.roots().get(2);
        Entity billboardEntity = world.roots().get(3);
        PerspectiveCamera3d camera =
                cameraEntity.component(CAMERA, PerspectiveCamera3d.class).orElseThrow();
        DirectionalLight3d light =
                lightEntity.component(LIGHT, DirectionalLight3d.class).orElseThrow();
        MeshRenderer3d renderer =
                meshEntity.component(RENDERER, MeshRenderer3d.class).orElseThrow();
        BillboardRenderer3d billboard =
                billboardEntity.component(BILLBOARD, BillboardRenderer3d.class).orElseThrow();

        camera.setFieldOfViewDegrees(70.0F);
        camera.setClippingPlanes(0.5F, 300.0F);
        light.setColor(Color.RED);
        light.setIntensity(3.0F);
        light.setTarget(1.0F, 2.0F, 3.0F);
        renderer.setVisible(false);
        billboard.setVisible(false);

        assertThat(camera.fieldOfViewDegrees()).isCloseTo(70.0F, within(0.0001F));
        assertThat(camera.near()).isEqualTo(0.5F);
        assertThat(camera.far()).isEqualTo(300.0F);
        assertThat(light.color()).isEqualTo(Color.RED);
        assertThat(light.intensity()).isEqualTo(3.0F);
        assertThat(coordinates(light.target())).containsExactly(1.0F, 2.0F, 3.0F);
        assertThat(renderer.isVisible()).isFalse();
        assertThat(billboard.isVisible()).isFalse();

        world.close();
    }

    /** Applies entity lifecycle to presentation and releases component and resource ownership. */
    @Test
    void appliesLifecycleAndReleasesResources() throws IOException {
        TestResources resources = new TestResources();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(presentationWorld(), spatial, resources).world().orElseThrow();
        Entity cameraEntity = world.roots().get(0);
        Entity lightEntity = world.roots().get(1);
        Entity meshEntity = world.roots().get(2);
        Entity billboardEntity = world.roots().get(3);
        PerspectiveCamera3d camera =
                cameraEntity.component(CAMERA, PerspectiveCamera3d.class).orElseThrow();
        DirectionalLight3d light =
                lightEntity.component(LIGHT, DirectionalLight3d.class).orElseThrow();
        MeshRenderer3d renderer =
                meshEntity.component(RENDERER, MeshRenderer3d.class).orElseThrow();
        BillboardRenderer3d billboard =
                billboardEntity.component(BILLBOARD, BillboardRenderer3d.class).orElseThrow();

        assertThat(spatial.isReadyToRender()).isFalse();
        world.activate();
        assertThat(spatial.isReadyToRender()).isTrue();
        world.disable(cameraEntity);
        assertThat(spatial.isReadyToRender()).isFalse();
        world.enable(cameraEntity);
        assertThat(spatial.isReadyToRender()).isTrue();
        world.destroy(meshEntity);
        world.destroy(billboardEntity);

        assertThat(renderer.isClosed()).isTrue();
        assertThat(billboard.isClosed()).isTrue();
        renderer.close();
        assertThat(resources.mesh.isClosed()).isTrue();
        assertThat(resources.material.isClosed()).isTrue();
        assertThat(resources.basicMaterial.isClosed()).isTrue();
        world.close();
        assertThat(camera.isClosed()).isTrue();
        assertThat(light.isClosed()).isTrue();
        assertThat(spatial.isClosed()).isTrue();
        camera.close();
        light.close();
    }

    /** Rejects ambiguous authored primary-camera selection during transactional composition. */
    @Test
    void rejectsMultiplePrimaryCameras() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        WorldDefinition definition = new WorldDefinition(
                WORLD_ID, "Ambiguous cameras", List.of(cameraEntity("First", true), cameraEntity("Second", true)));

        WorldCompositionResult result = compose(definition, spatial, noResources());

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(RuntimeDiagnosticCode.FACTORY_CREATE_FAILED);
            assertThat(diagnostic.location()).isEqualTo("/roots/1/components/1");
        });
        assertThat(spatial.isReadyToRender()).isFalse();
        spatial.close();
    }

    /** Reports that a non-primary camera cannot provide a render view. */
    @Test
    void reportsPresentationWithoutPrimaryCamera() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(singleCameraWorld(false), spatial, noResources())
                .world()
                .orElseThrow();

        assertThat(spatial.isReadyToRender()).isFalse();

        world.close();
    }

    /** Writes and composes a definition using the real catalog and runtime extension seams. */
    private WorldCompositionResult compose(
            WorldDefinition definition, Spatial3dWorldModule spatial, RuntimeResourceProvider resources)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("presentation.world.json"), definition);
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        return WorldComposer.compose(
                assets,
                AssetRef.to(definition.id()),
                types,
                List.of(new Spatial3dRuntimeExtension()),
                List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                resources);
    }

    /** Creates the camera, sun, and visible mesh acceptance world. */
    private static WorldDefinition presentationWorld() {
        LocalEntity camera = cameraEntity("Camera", true);
        LocalEntity light = entity(
                "Sun",
                component(
                        Spatial3dDescriptors.transformType(),
                        Map.of(Spatial3dDescriptors.positionProperty(), numbers(4.0F, 6.0F, 5.0F))),
                component(
                        LIGHT,
                        Spatial3dDescriptors.directionalLightType(),
                        Map.of(Spatial3dDescriptors.intensityProperty(), number(2.0F))));
        LocalEntity mesh = entity(
                "Cube",
                component(Spatial3dDescriptors.transformType(), Map.of()),
                component(
                        RENDERER,
                        Spatial3dDescriptors.meshRendererType(),
                        Map.of(
                                Spatial3dDescriptors.meshProperty(), reference(MESH_REFERENCE),
                                Spatial3dDescriptors.materialProperty(), reference(MATERIAL_REFERENCE))));
        LocalEntity billboard = entity(
                "Billboard",
                component(Spatial3dDescriptors.transformType(), Map.of()),
                component(
                        BILLBOARD,
                        Spatial3dDescriptors.billboardRendererType(),
                        Map.of(
                                Spatial3dDescriptors.materialProperty(), reference(BASIC_MATERIAL_REFERENCE),
                                Spatial3dDescriptors.sizeProperty(), numbers(2.0F, 3.0F),
                                Spatial3dDescriptors.anchorProperty(), numbers(0.5F, 0.0F),
                                Spatial3dDescriptors.alignmentProperty(), new ProjectValue.TextValue("cylindrical"))));
        return new WorldDefinition(WORLD_ID, "3D presentation", List.of(camera, light, mesh, billboard));
    }

    /** Creates a world containing exactly one camera. */
    private static WorldDefinition singleCameraWorld(boolean primary) {
        return new WorldDefinition(WORLD_ID, "Camera", List.of(cameraEntity("Camera", primary)));
    }

    /** Creates one camera entity with explicit projection and selection state. */
    private static LocalEntity cameraEntity(String name, boolean primary) {
        return entity(
                name,
                component(
                        Spatial3dDescriptors.transformType(),
                        Map.of(Spatial3dDescriptors.positionProperty(), numbers(0.0F, 0.0F, 5.0F))),
                component(
                        CAMERA,
                        Spatial3dDescriptors.perspectiveCameraType(),
                        Map.of(
                                Spatial3dDescriptors.fieldOfViewDegreesProperty(), number(50.0F),
                                Spatial3dDescriptors.nearProperty(), number(0.25F),
                                Spatial3dDescriptors.farProperty(), number(200.0F),
                                Spatial3dDescriptors.primaryProperty(), new ProjectValue.BooleanValue(primary))));
    }

    /** Creates one local entity with deterministic stable identity. */
    private static LocalEntity entity(String name, ComponentDefinition... components) {
        EntityId id =
                switch (name) {
                    case "Camera" -> EntityId.from("da01ec61-22f9-44dd-9e1a-d927c663d666");
                    case "First" -> EntityId.from("790102e4-4263-4c5d-879f-f5a9c649d133");
                    case "Second" -> EntityId.from("921a70ea-f838-493d-903a-502f4fc63cc4");
                    case "Sun" -> EntityId.from("41219435-4728-4388-967a-f2f0f5ed6357");
                    case "Cube" -> EntityId.from("bc67eff8-31b9-4df0-8acb-283a76c544df");
                    case "Billboard" -> EntityId.from("4f359d31-2fb1-4caf-9910-b97372702af3");
                    default -> throw new IllegalArgumentException("unknown fixture entity " + name);
                };
        return new LocalEntity(id, name, true, List.of(components), List.of());
    }

    /** Creates one transform component with its entity-scoped stable identity. */
    private static ComponentDefinition component(ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return component(TRANSFORM, type, properties);
    }

    /** Creates one component with explicit stable identity. */
    private static ComponentDefinition component(
            ComponentId id, ComponentType type, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(id, type.id(), type.version(), properties);
    }

    /** Creates one portable numeric value. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(number(value));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Creates one portable runtime-resource reference value. */
    private static ProjectValue.ReferenceValue reference(ResourceReference reference) {
        return new ProjectValue.ReferenceValue(reference);
    }

    /** Returns a provider that rejects every resource request. */
    private static RuntimeResourceProvider noResources() {
        return new RuntimeResourceProvider() {
            @Override
            public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
                throw new IllegalStateException("the test defines no resources");
            }
        };
    }

    /** AssertJ offset without a static wildcard import. */
    private static Offset<Float> within(float value) {
        return Offset.offset(value);
    }

    /** Copies one vector into an assertion-friendly scalar list. */
    private static List<Float> coordinates(Vector3fc value) {
        return List.of(value.x(), value.y(), value.z());
    }

    /** Owns the two runtime resources supplied to one composed world. */
    private static final class TestResources implements RuntimeResourceProvider {
        private final Mesh3dResource mesh = Mesh3dResource.owning(BoxGeometry.create(1.0F, 1.0F, 1.0F));
        private final Material3dResource material = Material3dResource.owning(new LambertMaterial(Color.BLUE));
        private final Material3dResource basicMaterial = Material3dResource.owning(new BasicMaterial(Color.WHITE));

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            Object value;
            Runnable release;
            if (reference.equals(MESH_REFERENCE)) {
                value = mesh;
                release = mesh::close;
            } else if (reference.equals(MATERIAL_REFERENCE)) {
                value = material;
                release = material::close;
            } else if (reference.equals(BASIC_MATERIAL_REFERENCE)) {
                value = basicMaterial;
                release = basicMaterial::close;
            } else {
                throw new IllegalStateException("unknown resource " + reference);
            }
            T typed = valueType.cast(value);
            return RuntimeResourceLease.of(typed, release);
        }
    }
}
