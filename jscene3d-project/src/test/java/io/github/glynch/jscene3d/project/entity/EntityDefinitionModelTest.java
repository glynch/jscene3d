/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentTypeId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifies the immutable identity, entity-definition, placement, and world-definition kernel. */
final class EntityDefinitionModelTest {
    private static final AssetId BULLET_ASSET = AssetId.from("4c189475-9845-4810-b7f9-af744d3cc726");
    private static final AssetId WORLD_ASSET = AssetId.from("8d95e0d5-7cac-42a6-bceb-931437834532");
    private static final EntityId BULLET_ROOT = EntityId.from("853f50a0-17dc-46ac-9f04-f772e54c44b2");
    private static final EntityId BULLET_PLACEMENT = EntityId.from("722c16bf-922b-41a5-9499-b23b68d8ab8e");
    private static final EntityId CAMERA = EntityId.from("4b2fefc2-4d1e-441d-814d-fe94b5b23f09");
    private static final ComponentId TRANSFORM = ComponentId.from("b991ca3e-66bb-4ef0-a682-74773bbef0d0");

    /** Models local entities and reusable placements without an instance-wrapper entity. */
    @Test
    void modelsWorldRootsAndDefinitionPlacements() {
        ComponentDefinition transform = transformDefinition(Map.of());
        LocalEntity bulletRoot = new LocalEntity(BULLET_ROOT, "Bullet", true, List.of(transform), List.of());
        EntityDefinition bullet = new EntityDefinition(BULLET_ASSET, "Bullet", bulletRoot);
        AssetRef<EntityDefinition> reference = AssetRef.to(BULLET_ASSET, "entities/bullet.entity.json");
        EntityPlacement placement = new EntityPlacement(
                BULLET_PLACEMENT,
                "Preview Bullet",
                true,
                reference,
                Map.of(property("speed"), new ProjectValue.NumberValue(BigDecimal.TEN)));
        LocalEntity camera = new LocalEntity(CAMERA, "Camera", true, List.of(transformDefinition(Map.of())), List.of());
        WorldDefinition world = new WorldDefinition(WORLD_ASSET, "Garden", List.of(camera, placement));

        assertThat(bullet.root()).isSameAs(bulletRoot);
        assertThat(world.roots()).containsExactly(camera, placement);
        assertThat(placement.id())
                .isEqualTo(BULLET_PLACEMENT)
                .isNotEqualTo(bullet.root().id());
        assertThat(placement.definition()).isEqualTo(reference);
        assertThat(placement.arguments()).containsKey(property("speed"));
        assertThat(placement.name()).contains("Preview Bullet");
        assertThat(placement.isEnabled()).isTrue();
    }

    /** Keeps asset location hints non-authoritative for identity and equality. */
    @Test
    void keepsAssetReferencesIndependentOfLocation() {
        AssetRef<EntityDefinition> original = AssetRef.to(BULLET_ASSET, "entities/bullet.entity.json");
        AssetRef<EntityDefinition> moved = AssetRef.to(BULLET_ASSET, "archive/projectile.entity.json");
        AssetRef<EntityDefinition> withoutHint = AssetRef.to(BULLET_ASSET);

        assertThat(original).isEqualTo(moved).isEqualTo(withoutHint);
        assertThat(original.hashCode()).isEqualTo(withoutHint.hashCode());
        assertThat(original.id()).isEqualTo(BULLET_ASSET);
        assertThat(original.pathHint()).contains("entities/bullet.entity.json");
        assertThat(withoutHint.pathHint()).isEmpty();
        assertThat(original.toString()).contains(BULLET_ASSET.toString(), "entities/bullet.entity.json");
    }

    /** Defensively copies authored properties and hierarchy collections. */
    @Test
    void copiesAuthoredCollections() {
        Map<PropertyId, ProjectValue> mutableProperties = new LinkedHashMap<>();
        mutableProperties.put(property("mass"), new ProjectValue.NumberValue(BigDecimal.ONE));
        ComponentDefinition component = transformDefinition(mutableProperties);
        List<ComponentDefinition> mutableComponents = new ArrayList<>(List.of(component));
        List<EntityEntry> mutableChildren = new ArrayList<>();
        LocalEntity root = new LocalEntity(BULLET_ROOT, true, mutableComponents, mutableChildren);
        List<EntityEntry> mutableRoots = new ArrayList<>(List.of(root));
        WorldDefinition world = new WorldDefinition(WORLD_ASSET, "Garden", mutableRoots);

        mutableProperties.clear();
        mutableComponents.clear();
        mutableChildren.add(new LocalEntity(CAMERA, true, List.of(), List.of()));
        mutableRoots.clear();
        Map<PropertyId, ProjectValue> immutableProperties = component.properties();
        List<EntityEntry> immutableChildren = root.children();
        List<EntityEntry> immutableRoots = world.roots();

        assertThat(component.properties()).containsKey(property("mass"));
        assertThat(root.components()).containsExactly(component);
        assertThat(root.children()).isEmpty();
        assertThat(world.roots()).containsExactly(root);
        assertThatThrownBy(immutableProperties::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(immutableChildren::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(immutableRoots::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Rejects duplicate component identities within one authored entity. */
    @Test
    void rejectsDuplicateComponentIds() {
        ComponentDefinition first = transformDefinition(Map.of());
        ComponentDefinition duplicate =
                transformDefinition(Map.of(property("x"), new ProjectValue.NumberValue(BigDecimal.ONE)));
        List<ComponentDefinition> components = List.of(first, duplicate);
        List<EntityEntry> noChildren = List.of();

        assertThatThrownBy(() -> new LocalEntity(BULLET_ROOT, true, components, noChildren))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate id", TRANSFORM.toString());
    }

    /** Rejects duplicate entity identities anywhere in one locally authored hierarchy. */
    @Test
    void rejectsDuplicateEntityIdsAcrossHierarchy() {
        LocalEntity duplicate = new LocalEntity(CAMERA, "Duplicate", true, List.of(), List.of());
        LocalEntity child = new LocalEntity(CAMERA, "Child", true, List.of(), List.of(duplicate));
        LocalEntity root = new LocalEntity(BULLET_ROOT, "Root", true, List.of(), List.of(child));

        assertThatThrownBy(() -> new EntityDefinition(BULLET_ASSET, "Bullet", root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate entity id", CAMERA.toString());
    }

    /** Rejects duplicate identities across separate world roots and their descendants. */
    @Test
    void rejectsDuplicateEntityIdsAcrossWorldRoots() {
        LocalEntity first = new LocalEntity(CAMERA, "First", true, List.of(), List.of());
        LocalEntity second = new LocalEntity(CAMERA, "Second", true, List.of(), List.of());
        List<EntityEntry> roots = List.of(first, second);

        assertThatThrownBy(() -> new WorldDefinition(WORLD_ASSET, "Garden", roots))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate entity id", CAMERA.toString());
    }

    /** Provides structural value semantics for definitions, local entities, placements, and components. */
    @Test
    void providesDefinitionValueSemantics() {
        ComponentDefinition firstComponent = transformDefinition(Map.of());
        ComponentDefinition secondComponent = transformDefinition(Map.of());
        LocalEntity firstRoot = new LocalEntity(BULLET_ROOT, "Bullet", true, List.of(firstComponent), List.of());
        LocalEntity secondRoot = new LocalEntity(BULLET_ROOT, "Bullet", true, List.of(secondComponent), List.of());
        EntityDefinition first = new EntityDefinition(BULLET_ASSET, "Bullet", firstRoot);
        EntityDefinition second = new EntityDefinition(BULLET_ASSET, "Bullet", secondRoot);
        EntityPlacement firstPlacement =
                new EntityPlacement(BULLET_PLACEMENT, true, AssetRef.to(BULLET_ASSET), Map.of());
        EntityPlacement secondPlacement =
                new EntityPlacement(BULLET_PLACEMENT, true, AssetRef.to(BULLET_ASSET), Map.of());

        assertThat(firstComponent).isEqualTo(secondComponent).hasSameHashCodeAs(secondComponent);
        assertThat(firstRoot).isEqualTo(secondRoot).hasSameHashCodeAs(secondRoot);
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(firstPlacement).isEqualTo(secondPlacement).hasSameHashCodeAs(secondPlacement);
        assertThat(firstRoot.toString()).contains("Bullet", BULLET_ROOT.toString());
        assertThat(first.toString()).contains("Bullet", BULLET_ASSET.toString());
        assertThat(firstPlacement.toString()).contains(BULLET_PLACEMENT.toString(), BULLET_ASSET.toString());
    }

    /** Validates canonical identity, type, version, name, and path-hint syntax. */
    @Test
    void validatesScalarDefinitionValues() {
        UUID uuid = UUID.fromString("4c189475-9845-4810-b7f9-af744d3cc726");
        ComponentTypeId type = new ComponentTypeId("io.github.glynch.jscene3d/transform-3d");
        Map<PropertyId, ProjectValue> properties = Map.of();
        LocalEntity unnamed = unnamedEntity();

        assertThat(new AssetId(uuid).toString()).isEqualTo(uuid.toString());
        assertThat(new EntityId(uuid).toString()).isEqualTo(uuid.toString());
        assertThat(new ComponentId(uuid).toString()).isEqualTo(uuid.toString());
        assertThat(type.toString()).isEqualTo("io.github.glynch.jscene3d/transform-3d");
        assertThatThrownBy(() -> AssetId.from("4C189475-9845-4810-B7F9-AF744D3CC726"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntityId.from("not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ComponentTypeId("transform-3d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ComponentDefinition(TRANSFORM, type, 0, properties))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetRef.to(BULLET_ASSET, "../bullet.entity.json"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntityDefinition(BULLET_ASSET, " ", unnamed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Exposes absence of a display name without requiring null or placeholder text. */
    @Test
    void supportsUnnamedAuthoredEntries() {
        LocalEntity local = unnamedEntity();
        EntityPlacement placement = new EntityPlacement(BULLET_PLACEMENT, false, AssetRef.to(BULLET_ASSET), Map.of());

        assertThat(local.name()).isEmpty();
        assertThat(placement.name()).isEmpty();
        assertThat(local.isEnabled()).isTrue();
        assertThat(placement.isEnabled()).isFalse();
    }

    /** Creates the repeated transform-component fixture. */
    private static ComponentDefinition transformDefinition(Map<PropertyId, ProjectValue> properties) {
        return new ComponentDefinition(
                TRANSFORM, new ComponentTypeId("io.github.glynch.jscene3d/transform-3d"), 1, properties);
    }

    /** Creates one stable local property identity. */
    private static PropertyId property(String value) {
        return new PropertyId(value);
    }

    /** Creates one unnamed local entity for scalar and optional-name checks. */
    private static LocalEntity unnamedEntity() {
        return new LocalEntity(BULLET_ROOT, true, List.of(), List.of());
    }
}
