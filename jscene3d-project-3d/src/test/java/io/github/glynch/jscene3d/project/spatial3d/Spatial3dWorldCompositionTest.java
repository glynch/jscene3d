/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetDiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises descriptor-backed 3D composition through the supported runtime interfaces. */
final class Spatial3dWorldCompositionTest {
    private static final AssetId WORLD_ID = AssetId.from("80650465-c697-4558-9c39-7e85dc6a875f");
    private static final EntityId ROOT_ID = EntityId.from("b86224bd-aed1-4263-a838-ed3ed28c55fe");
    private static final EntityId CHILD_ID = EntityId.from("652ad534-90f7-4ac9-b09c-5744120190e4");
    private static final ComponentId ROOT_TRANSFORM = ComponentId.from("a471bf77-d87a-47d8-aaf3-e6c76fbd7bf7");
    private static final ComponentId CHILD_TRANSFORM = ComponentId.from("74fdcdbf-e167-4022-a852-7cbdd4809582");

    private static final RuntimeResourceProvider NO_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the test defines no runtime resources");
        }
    };

    @TempDir
    private Path temporaryDirectory;

    /** Derives child world state automatically and keeps it current after local mutation. */
    @Test
    void composesAndUpdatesTransformHierarchy() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(hierarchicalWorld(), spatial).world().orElseThrow();
        Entity root = world.roots().getFirst();
        Entity child = root.children().getFirst();
        Transform3d parentTransform =
                root.component(ROOT_TRANSFORM, Transform3d.class).orElseThrow();
        Transform3d childTransform =
                child.component(CHILD_TRANSFORM, Transform3d.class).orElseThrow();

        assertThat(translation(parentTransform)).containsExactly(10.0F, 0.0F, 0.0F);
        assertThat(translation(childTransform)).containsExactly(10.0F, 4.0F, 0.0F);

        parentTransform.setPosition(20.0F, 0.0F, 0.0F);
        parentTransform.setOrientation(0.0F, 0.0F, 0.0F, 2.0F);
        childTransform.setScale(3.0F, 4.0F, 5.0F);

        assertThat(parentTransform.orientation().w()).isEqualTo(1.0F);
        assertThat(childTransform.scale().x()).isEqualTo(3.0F);
        assertThat(childTransform.scale().y()).isEqualTo(4.0F);
        assertThat(childTransform.scale().z()).isEqualTo(5.0F);
        assertThat(translation(childTransform)).containsExactly(20.0F, 4.0F, 0.0F);

        world.close();

        assertThat(parentTransform.isClosed()).isTrue();
        assertThat(childTransform.isClosed()).isTrue();
        assertThat(spatial.isClosed()).isTrue();
    }

    /** Converts an authoritative world-space pose back into local state beneath an ownership parent. */
    @Test
    void setsWorldPoseWithinTransformHierarchy() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        try (World world = compose(hierarchicalWorld(), spatial).world().orElseThrow()) {
            Entity child = world.roots().getFirst().children().getFirst();
            Transform3d transform =
                    child.component(CHILD_TRANSFORM, Transform3d.class).orElseThrow();
            Quaternionf orientation = new Quaternionf().rotateY(0.5F);

            transform.setWorldPose(new Vector3f(2.0F, 3.0F, 4.0F), orientation);

            assertThat(translation(transform)).containsExactly(2.0F, 3.0F, 4.0F);
            assertThat(transform.position()).isEqualTo(new Vector3f(-4.0F, 1.5F, 2.0F));
            assertThat(transform
                            .worldMatrix()
                            .getUnnormalizedRotation(new Quaternionf())
                            .normalize())
                    .isEqualTo(orientation);
        }
    }

    /** Releases a destroyed subtree without closing a still-live parent registration. */
    @Test
    void removesDestroyedTransformSubtree() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        try (World world = compose(hierarchicalWorld(), spatial).world().orElseThrow()) {
            Entity root = world.roots().getFirst();
            Entity child = root.children().getFirst();
            Transform3d parentTransform =
                    root.component(ROOT_TRANSFORM, Transform3d.class).orElseThrow();
            Transform3d childTransform =
                    child.component(CHILD_TRANSFORM, Transform3d.class).orElseThrow();
            world.activate();

            world.destroy(child);

            assertThat(childTransform.isClosed()).isTrue();
            assertThat(parentTransform.isClosed()).isFalse();
            assertThat(root.children()).isEmpty();
        }
        assertThat(spatial.isClosed()).isTrue();
    }

    /** Reports the missing host seam at the exact authored component location. */
    @Test
    void reportsMissingSpatialWorldModule() throws IOException {
        WorldCompositionResult result = compose(hierarchicalWorld());

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(RuntimeDiagnosticCode.WORLD_MODULE_MISSING);
            assertThat(diagnostic.location()).isEqualTo("/roots/0/components/0");
        });
    }

    /** Rejects malformed numeric shape through descriptor validation before factory creation. */
    @Test
    void reportsMalformedTransformProperty() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Map<PropertyId, ProjectValue> properties = Map.of(Spatial3dDescriptors.positionProperty(), numbers(1.0F, 2.0F));
        WorldDefinition definition = singleTransformWorld(properties);

        WorldCompositionResult result = compose(definition, spatial);

        assertThat(result.world()).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(AssetDiagnosticCode.COMPONENT_PROPERTY_VALUE_INVALID);
            assertThat(diagnostic.location()).isEqualTo("/roots/0/components/0/properties/position");
        });
        assertThat(spatial.isClosed()).isFalse();
        spatial.close();
    }

    /** Makes world-owned closure idempotent and component access terminal. */
    @Test
    void rejectsTransformAccessAfterWorldClosure() throws IOException {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        World world = compose(singleTransformWorld(Map.of()), spatial).world().orElseThrow();
        Entity root = world.roots().getFirst();
        Transform3d transform =
                root.component(ROOT_TRANSFORM, Transform3d.class).orElseThrow();

        world.close();
        world.close();
        transform.close();
        spatial.close();

        assertThat(transform.isClosed()).isTrue();
        assertThatThrownBy(transform::position)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Writes and composes a world without host modules. */
    private WorldCompositionResult compose(WorldDefinition definition) throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("spatial.world.json"), definition);
        return composeCatalog(definition, List.of());
    }

    /** Writes and composes a world with one spatial adapter. */
    private WorldCompositionResult compose(WorldDefinition definition, Spatial3dWorldModule spatial)
            throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("spatial.world.json"), definition);
        WorldModuleBinding<Spatial3dWorldModule> binding = WorldModuleBinding.of(Spatial3dWorldModule.class, spatial);
        return composeCatalog(definition, List.of(binding));
    }

    /** Composes the written fixture through the public asset and runtime boundaries. */
    private WorldCompositionResult composeCatalog(WorldDefinition definition, List<WorldModuleBinding<?>> modules) {
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        return WorldComposer.compose(
                assets,
                AssetRef.to(definition.id()),
                types,
                List.of(new Spatial3dRuntimeExtension()),
                modules,
                NO_RESOURCES);
    }

    /** Creates one scaled parent and translated child. */
    private static WorldDefinition hierarchicalWorld() {
        ComponentDefinition childTransform =
                transform(CHILD_TRANSFORM, Map.of(Spatial3dDescriptors.positionProperty(), numbers(0.0F, 2.0F, 0.0F)));
        LocalEntity child = new LocalEntity(CHILD_ID, "Child", true, List.of(childTransform), List.of());
        ComponentDefinition rootTransform = transform(
                ROOT_TRANSFORM,
                Map.of(
                        Spatial3dDescriptors.positionProperty(), numbers(10.0F, 0.0F, 0.0F),
                        Spatial3dDescriptors.scaleProperty(), numbers(2.0F, 2.0F, 2.0F)));
        LocalEntity root = new LocalEntity(ROOT_ID, "Root", true, List.of(rootTransform), List.of(child));
        return new WorldDefinition(WORLD_ID, "Spatial hierarchy", List.of(root));
    }

    /** Creates one root transform with the supplied authored property overrides. */
    private static WorldDefinition singleTransformWorld(Map<PropertyId, ProjectValue> properties) {
        ComponentDefinition transform = transform(ROOT_TRANSFORM, properties);
        LocalEntity root = new LocalEntity(ROOT_ID, "Root", true, List.of(transform), List.of());
        return new WorldDefinition(WORLD_ID, "Single transform", List.of(root));
    }

    /** Creates one Transform3d component definition. */
    private static ComponentDefinition transform(ComponentId id, Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(
                id,
                Spatial3dDescriptors.transformType().id(),
                Spatial3dDescriptors.transformType().version(),
                properties);
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(new ProjectValue.NumberValue(BigDecimal.valueOf(value)));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Extracts world translation into an assertion-friendly list. */
    private static List<Float> translation(Transform3d transform) {
        Vector3f value = transform.worldMatrix().getTranslation(new Vector3f());
        return List.of(value.x, value.y, value.z);
    }
}
