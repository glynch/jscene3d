/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.Offset;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises descriptor-backed character movement through a completely composed project world. */
final class CharacterBody3dWorldCompositionTest {
    private static final AssetId WORLD_ID = AssetId.from("c21cb443-3f9b-49a1-a745-e54f7063d3a3");
    private static final EntityId FLOOR_ENTITY = EntityId.from("a597ce22-c47d-443c-943f-844819f16234");
    private static final EntityId WALL_ENTITY = EntityId.from("ba139f5d-71db-4ee4-956e-435e40228544");
    private static final EntityId CHARACTER_ENTITY = EntityId.from("81048474-7440-4182-a41b-f751ddc34c31");
    private static final ComponentId FLOOR_TRANSFORM = ComponentId.from("63b915e1-d8ed-4673-96d4-d3ea1ce18870");
    private static final ComponentId FLOOR_SHAPE = ComponentId.from("cfb06ef8-9517-4af5-9961-a8e940f12e62");
    private static final ComponentId FLOOR_BODY = ComponentId.from("544d772c-9127-4b03-9415-cd6d2bd20f05");
    private static final ComponentId WALL_TRANSFORM = ComponentId.from("b7180c91-6e53-4158-beac-ecb3c8d7ab3a");
    private static final ComponentId WALL_SHAPE = ComponentId.from("3057fd69-fda0-4746-88bf-256100ea4613");
    private static final ComponentId WALL_BODY = ComponentId.from("538f24bc-5946-480d-9b49-ec788202d0d7");
    private static final ComponentId CHARACTER_TRANSFORM = ComponentId.from("53fd34f9-f64d-47c1-9e23-73d06454ea37");
    private static final ComponentId CHARACTER_SHAPE = ComponentId.from("00b4e4e8-6971-4bea-aec7-685326e0ab81");
    private static final ComponentId CHARACTER_BODY = ComponentId.from("d382dc11-9f56-48a2-88af-20a0c2cf5dd3");
    private static final ResourceReference FLOOR_RESOURCE = ResourceReference.asset("floor-box");
    private static final ResourceReference WALL_RESOURCE = ResourceReference.asset("wall-box");
    private static final ResourceReference CHARACTER_RESOURCE = ResourceReference.asset("character-capsule");
    private static final Duration STEP = Duration.ofNanos(1_000_000_000L / 120L);
    private static final Offset<Float> TOLERANCE = Offset.offset(5.0E-3F);

    @TempDir
    private Path temporaryDirectory;

    /** Grounds a capsule, slides it along a wall, and publishes the resolved pose to its entity transform. */
    @Test
    void movesCharacterAgainstComposedStaticCollision() throws IOException {
        DefinitionWriter.write(temporaryDirectory.resolve("character.world.json"), worldDefinition());
        AssetCatalog assets = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        Physics3dWorldModule physics = Physics3dAdapters.standard();
        RuntimeResourceProvider resources = new ShapeResources();
        World world = WorldComposer.compose(
                        assets,
                        AssetRef.to(WORLD_ID),
                        RegisteredTypeCatalog.of(List.of(
                                Spatial3dDescriptors.extensionDescriptor(),
                                Physics3dDescriptors.extensionDescriptor())),
                        List.of(new Spatial3dRuntimeExtension(), new Physics3dRuntimeExtension()),
                        List.of(
                                WorldModuleBinding.of(Spatial3dWorldModule.class, spatial),
                                WorldModuleBinding.of(Physics3dWorldModule.class, physics)),
                        resources)
                .world()
                .orElseThrow();
        world.activate();
        Entity characterEntity = world.roots().get(2);
        CharacterBody3d character =
                characterEntity.component(CHARACTER_BODY, CharacterBody3d.class).orElseThrow();
        Transform3d transform = characterEntity
                .component(CHARACTER_TRANSFORM, Transform3d.class)
                .orElseThrow();

        CharacterMove3dResult grounded = character.move(new Vector3f(), STEP);
        CharacterMove3dResult sliding = character.move(new Vector3f(4.0F, 0.0F, 3.0F), Duration.ofSeconds(1L));
        world.advanceFixed(STEP);

        assertThat(grounded.isGrounded()).isTrue();
        assertThat(character.isGrounded()).isTrue();
        assertThat(sliding.contacts())
                .extracting(contact -> contact.object().componentId())
                .contains(WALL_BODY);
        assertThat(sliding.contacts())
                .extracting(contact -> contact.shape().componentId())
                .contains(WALL_SHAPE);
        CharacterContact3d wallContact = sliding.contacts().stream()
                .filter(contact -> contact.object().componentId().equals(WALL_BODY))
                .findFirst()
                .orElseThrow();
        assertThat(wallContact.normal(new Vector3f()).x).isNegative();
        assertThat(wallContact.point(new Vector3f()).isFinite()).isTrue();
        assertThat(sliding.appliedTranslation(new Vector3f()).x).isCloseTo(0.999F, TOLERANCE);
        assertThat(transform.position().x()).isCloseTo(0.999F, TOLERANCE);
        assertThat(transform.position().z()).isCloseTo(3.0F, TOLERANCE);
        assertThat(character.groundNormal(new Vector3f()).y).isCloseTo(1.0F, TOLERANCE);

        assertThat(character.tryJump()).isTrue();
        CharacterMove3dResult jumping = character.move(new Vector3f(), STEP);
        world.advanceFixed(STEP);
        assertThat(jumping.jumped()).isTrue();
        assertThat(jumping.isGrounded()).isFalse();
        assertThat(transform.position().y()).isGreaterThan(1.001F);

        world.disable(characterEntity);
        Vector3f disabledMovement = new Vector3f();
        assertThatThrownBy(() -> character.move(disabledMovement, STEP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
        world.close();
        assertThat(character.isClosed()).isTrue();
        assertThatThrownBy(character::isGrounded).isInstanceOf(IllegalStateException.class);
    }

    /** Creates the floor, wall, and independently shaped character fixture. */
    private static WorldDefinition worldDefinition() {
        LocalEntity floor = collisionEntity(
                new CollisionEntityIds(FLOOR_ENTITY, FLOOR_TRANSFORM, FLOOR_SHAPE, FLOOR_BODY),
                "Floor",
                Physics3dDescriptors.staticBodyType(),
                FLOOR_RESOURCE,
                new Vector3f(0.0F, -0.5F, 0.0F));
        LocalEntity wall = collisionEntity(
                new CollisionEntityIds(WALL_ENTITY, WALL_TRANSFORM, WALL_SHAPE, WALL_BODY),
                "Wall",
                Physics3dDescriptors.staticBodyType(),
                WALL_RESOURCE,
                new Vector3f(2.0F, 0.0F, 0.0F));
        LocalEntity character = collisionEntity(
                new CollisionEntityIds(CHARACTER_ENTITY, CHARACTER_TRANSFORM, CHARACTER_SHAPE, CHARACTER_BODY),
                "Character",
                Physics3dDescriptors.characterBodyType(),
                CHARACTER_RESOURCE,
                new Vector3f(0.0F, 1.001F, 0.0F));
        return new WorldDefinition(WORLD_ID, "Character acceptance", List.of(floor, wall, character));
    }

    /** Creates one spatial collision entity with stable explicit shape membership. */
    private static LocalEntity collisionEntity(
            CollisionEntityIds ids,
            String name,
            ComponentType bodyType,
            ResourceReference resource,
            Vector3f position) {
        return new LocalEntity(
                ids.entity(),
                name,
                true,
                List.of(
                        new ComponentDefinition(
                                ids.transform(),
                                Spatial3dDescriptors.transformType().id(),
                                Spatial3dDescriptors.transformType().version(),
                                Map.of(
                                        Spatial3dDescriptors.positionProperty(),
                                        numbers(position.x, position.y, position.z))),
                        new ComponentDefinition(
                                ids.shape(),
                                Physics3dDescriptors.collisionShapeType().id(),
                                Physics3dDescriptors.collisionShapeType().version(),
                                Map.of(
                                        Physics3dDescriptors.shapeProperty(),
                                        new ProjectValue.ReferenceValue(resource))),
                        new ComponentDefinition(
                                ids.body(),
                                bodyType.id(),
                                bodyType.version(),
                                Map.of(
                                        Physics3dDescriptors.shapesProperty(),
                                        new ProjectValue.ArrayValue(List.of(new ProjectValue.ComponentTargetValue(
                                                new ComponentTarget(ids.entity(), ids.shape()))))))),
                List.of());
    }

    /** Stable authored identities belonging to one collision entity. */
    private record CollisionEntityIds(EntityId entity, ComponentId transform, ComponentId shape, ComponentId body) {}

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(new ProjectValue.NumberValue(BigDecimal.valueOf(value)));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Supplies fixture collision resources through ordinary runtime leases. */
    private static final class ShapeResources implements RuntimeResourceProvider {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            CollisionShape3dResource resource =
                    switch (reference.locator()) {
                        case "floor-box" -> new BoxCollisionShape3dResource(20.0F, 1.0F, 20.0F);
                        case "wall-box" -> new BoxCollisionShape3dResource(1.0F, 5.0F, 10.0F);
                        case "character-capsule" -> new CapsuleCollisionShape3dResource(0.5F, 1.0F);
                        default -> throw new IllegalArgumentException("unknown collision resource: " + reference);
                    };
            return RuntimeResourceLease.of(valueType.cast(resource), resource::close);
        }
    }
}
