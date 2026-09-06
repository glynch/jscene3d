/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies component descriptor defaults, metadata, copying, and invariants. */
final class ComponentTypeDescriptorTest {
    private static final ComponentType TYPE = ComponentType.of("example.game/mover", 1);
    private static final DescriptorPresentation PRESENTATION = DescriptorPresentation.named("Mover");

    /** Preserves typed declaration order and structural value semantics. */
    @Test
    void describesOneComponentType() {
        ComponentTypeDescriptor descriptor = descriptor();
        ComponentTypeDescriptor same = descriptor();

        assertThat(descriptor).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(descriptor.type()).isEqualTo(TYPE);
        assertThat(descriptor.properties()).containsOnlyKeys(new PropertyId("speed"));
        assertThat(descriptor.signals()).containsOnlyKeys(new EndpointId("moved"));
        assertThat(descriptor.actions()).containsOnlyKeys(new EndpointId("stop"));
        assertThat(descriptor.providedCapabilities()).containsExactly(new CapabilityId("example.game/movement"));
        assertThat(descriptor.requiredCapabilities()).containsExactly(new CapabilityId("example.game/transform"));
        assertThat(descriptor.attachments()).containsExactly(new AttachmentPointId("trail"));
        assertThat(descriptor.multiplicity()).isEqualTo(ComponentMultiplicity.MULTIPLE);
        assertThat(descriptor.conflicts()).containsExactly(new ComponentTypeId("example.game/teleporter"));
        assertThat(descriptor.spatialDomain()).isEqualTo(ComponentSpatialDomain.THREE_DIMENSIONAL);
        assertThat(descriptor.lifecycle()).containsExactly(ComponentLifecycle.CREATED, ComponentLifecycle.DESTROYED);
        assertThat(descriptor.updatePhases()).containsExactly(ComponentUpdatePhase.BEFORE_PHYSICS);
        assertThat(descriptor.toString()).contains("example.game/mover", "providedCapabilities");
    }

    /** Includes every descriptor field in structural equality. */
    @Test
    void distinguishesDescriptorStructure() {
        ComponentTypeDescriptor descriptor = descriptor();

        assertThat(descriptor.equals(descriptor)).isTrue();
        assertThat(descriptor).isNotNull().isNotEqualTo("not a descriptor");
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(ComponentType.of("example.game/other", 1))
                        .build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE, DescriptorPresentation.named("Other"))
                        .build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).properties(List.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).signals(List.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).actions(List.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(
                        descriptorBuilder(TYPE).providedCapabilities(Set.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(
                        descriptorBuilder(TYPE).requiredCapabilities(Set.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).attachments(Set.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE)
                        .multiplicity(ComponentMultiplicity.SINGLE)
                        .build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).conflicts(Set.of()).build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE)
                        .spatialDomain(ComponentSpatialDomain.TWO_DIMENSIONAL)
                        .build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE)
                        .lifecycle(Set.of(ComponentLifecycle.CREATED))
                        .build());
        assertThat(descriptor)
                .isNotEqualTo(descriptorBuilder(TYPE).updatePhases(Set.of()).build());
    }

    /** Defensively copies mutable builder inputs and publishes immutable collections. */
    @Test
    void copiesDescriptorCollections() {
        List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(property());
        Set<CapabilityId> capabilities = new LinkedHashSet<>();
        capabilities.add(new CapabilityId("example.game/movement"));
        ComponentTypeDescriptor descriptor = ComponentTypeDescriptor.builder(TYPE, PRESENTATION)
                .properties(properties)
                .providedCapabilities(capabilities)
                .build();

        properties.clear();
        capabilities.clear();

        assertThat(descriptor.properties()).containsKey(new PropertyId("speed"));
        assertThat(descriptor.providedCapabilities()).containsExactly(new CapabilityId("example.game/movement"));
        assertThatThrownBy(descriptor.properties()::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(descriptor.providedCapabilities()::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Applies safe non-spatial, single-instance, and callback-free defaults. */
    @Test
    void appliesConservativeDefaults() {
        ComponentTypeDescriptor descriptor =
                ComponentTypeDescriptor.builder(TYPE, PRESENTATION).build();

        assertThat(descriptor.multiplicity()).isEqualTo(ComponentMultiplicity.SINGLE);
        assertThat(descriptor.spatialDomain()).isEqualTo(ComponentSpatialDomain.NONE);
        assertThat(descriptor.lifecycle()).isEmpty();
        assertThat(descriptor.updatePhases()).isEmpty();
    }

    /** Rejects dependency and spatial declarations that cannot be resolved coherently. */
    @Test
    void rejectsInvalidDescriptorRelationships() {
        CapabilityId shared = new CapabilityId("example.game/shared");
        ComponentTypeDescriptor.Builder selfDependency = ComponentTypeDescriptor.builder(TYPE, PRESENTATION)
                .providedCapabilities(Set.of(shared))
                .requiredCapabilities(Set.of(shared));
        ComponentTypeDescriptor.Builder selfConflict =
                ComponentTypeDescriptor.builder(TYPE, PRESENTATION).conflicts(Set.of(TYPE.id()));
        ComponentTypeDescriptor.Builder nonSpatialAttachment =
                ComponentTypeDescriptor.builder(TYPE, PRESENTATION).attachments(Set.of(new AttachmentPointId("mount")));

        assertThatThrownBy(selfDependency::build).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(selfConflict::build).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(nonSpatialAttachment::build).isInstanceOf(IllegalArgumentException.class);
    }

    /** Builds one fully populated descriptor. */
    private static ComponentTypeDescriptor descriptor() {
        return descriptorBuilder(TYPE).build();
    }

    /** Starts one fully populated descriptor builder. */
    private static ComponentTypeDescriptor.Builder descriptorBuilder(ComponentType type) {
        return descriptorBuilder(type, PRESENTATION);
    }

    /** Starts one fully populated descriptor builder with custom presentation metadata. */
    private static ComponentTypeDescriptor.Builder descriptorBuilder(
            ComponentType type, DescriptorPresentation presentation) {
        return ComponentTypeDescriptor.builder(type, presentation)
                .properties(List.of(property()))
                .signals(List.of(EndpointDescriptor.withoutPayload("moved", DescriptorPresentation.named("Moved"))))
                .actions(List.of(EndpointDescriptor.withoutPayload("stop", DescriptorPresentation.named("Stop"))))
                .providedCapabilities(Set.of(new CapabilityId("example.game/movement")))
                .requiredCapabilities(Set.of(new CapabilityId("example.game/transform")))
                .attachments(Set.of(new AttachmentPointId("trail")))
                .multiplicity(ComponentMultiplicity.MULTIPLE)
                .conflicts(Set.of(new ComponentTypeId("example.game/teleporter")))
                .spatialDomain(ComponentSpatialDomain.THREE_DIMENSIONAL)
                .lifecycle(new LinkedHashSet<>(List.of(ComponentLifecycle.CREATED, ComponentLifecycle.DESTROYED)))
                .updatePhases(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS));
    }

    /** Creates one representative property. */
    private static PropertyDescriptor property() {
        return PropertyDescriptor.required("speed", ProjectValueKind.NUMBER, PRESENTATION, Map.of(), Set.of());
    }
}
