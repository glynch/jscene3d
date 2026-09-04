/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies descriptor-model value semantics, copying, and invariants. */
final class ExtensionDescriptorModelTest {
    private static final DescriptorPresentation PRESENTATION = DescriptorPresentation.named("Visible");

    /** Gives property and endpoint descriptors structural value semantics. */
    @Test
    void givesDescriptorLeavesValueSemantics() {
        PropertyDescriptor property = PropertyDescriptor.optionalWithDefault(
                "visible",
                ProjectValueKind.BOOLEAN,
                new ProjectValue.BooleanValue(true),
                PRESENTATION,
                Map.of("group", new ProjectValue.TextValue("Rendering")),
                Set.of());
        PropertyDescriptor sameProperty = PropertyDescriptor.optionalWithDefault(
                "visible",
                ProjectValueKind.BOOLEAN,
                new ProjectValue.BooleanValue(true),
                PRESENTATION,
                Map.of("group", new ProjectValue.TextValue("Rendering")),
                Set.of());
        EndpointDescriptor endpoint = EndpointDescriptor.withPayload(
                "changed", new RegisteredType("example.game/change", 1), DescriptorPresentation.named("Changed"));
        EndpointDescriptor sameEndpoint = EndpointDescriptor.withPayload(
                "changed", new RegisteredType("example.game/change", 1), DescriptorPresentation.named("Changed"));

        assertThat(property).isEqualTo(sameProperty).hasSameHashCodeAs(sameProperty);
        assertThat(property.toString()).contains("id=visible", "valueKind=BOOLEAN");
        assertThat(endpoint).isEqualTo(sameEndpoint).hasSameHashCodeAs(sameEndpoint);
        assertThat(endpoint.toString()).contains("id=changed", "example.game/change");
    }

    /** Gives complete registered-type and extension descriptors structural value semantics. */
    @Test
    void givesAggregateDescriptorsValueSemantics() {
        RegisteredTypeDescriptor type = typeDescriptor();
        RegisteredTypeDescriptor sameType = typeDescriptor();
        ExtensionDescriptor extension = extension(type);
        ExtensionDescriptor sameExtension = extension(sameType);

        assertThat(type).isEqualTo(sameType).hasSameHashCodeAs(sameType);
        assertThat(type.toString()).contains("example.game/group-3d", "SCENE_NODE");
        assertThat(extension).isEqualTo(sameExtension).hasSameHashCodeAs(sameExtension);
        assertThat(extension.toString()).contains("id=example.game", "version=1.0.0");
    }

    /** Defensively copies mutable descriptor collections. */
    @Test
    void copiesDescriptorCollections() {
        Map<String, ProjectValue> metadata = new LinkedHashMap<>();
        metadata.put("group", new ProjectValue.TextValue("Rendering"));
        List<PropertyDescriptor> properties = new ArrayList<>();
        PropertyDescriptor property = PropertyDescriptor.optional(
                "mesh", ProjectValueKind.REFERENCE, PRESENTATION, metadata, Set.of(ResourceReference.Kind.PROJECT));
        properties.add(property);
        RegisteredTypeDescriptor type = new RegisteredTypeDescriptor(
                new RegisteredType("example.game/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                PRESENTATION,
                properties,
                List.of(),
                List.of(),
                List.of());

        metadata.clear();
        properties.clear();

        assertThat(property.editorMetadata()).containsKey("group");
        assertThat(type.properties()).containsKey("mesh");
        assertThatThrownBy(type.properties()::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Applies value-kind and reference-namespace constraints. */
    @Test
    void checksPropertyValues() {
        PropertyDescriptor reference = PropertyDescriptor.optional(
                "mesh", ProjectValueKind.REFERENCE, PRESENTATION, Map.of(), Set.of(ResourceReference.Kind.ASSET));
        ProjectValue accepted = new ProjectValue.ReferenceValue(ResourceReference.asset("mesh"));
        ProjectValue wrongKind = new ProjectValue.TextValue("mesh");
        ProjectValue wrongNamespace = new ProjectValue.ReferenceValue(ResourceReference.imported("mesh/output"));

        assertThat(reference.accepts(accepted)).isTrue();
        assertThat(reference.accepts(wrongKind)).isFalse();
        assertThat(reference.accepts(wrongNamespace)).isFalse();
        assertThat(ProjectValueKind.of(ProjectValue.NullValue.INSTANCE)).isEqualTo(ProjectValueKind.NULL);
        assertThat(ProjectValueKind.of(new ProjectValue.ArrayValue(List.of()))).isEqualTo(ProjectValueKind.ARRAY);
    }

    /** Rejects inconsistent descriptor construction. */
    @Test
    void rejectsInvalidDescriptorInvariants() {
        ProjectValue text = new ProjectValue.TextValue("true");
        Map<String, ProjectValue> editorMetadata = Map.of();
        Set<ResourceReference.Kind> noReferences = Set.of();
        Set<ResourceReference.Kind> projectReferences = Set.of(ResourceReference.Kind.PROJECT);
        RegisteredTypeDescriptor foreignType = new RegisteredTypeDescriptor(
                new RegisteredType("example.other/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                PRESENTATION,
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertThatThrownBy(() -> PropertyDescriptor.optionalWithDefault(
                        "visible", ProjectValueKind.BOOLEAN, text, PRESENTATION, editorMetadata, noReferences))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PropertyDescriptor.optional(
                        "visible", ProjectValueKind.BOOLEAN, PRESENTATION, editorMetadata, projectReferences))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> extension(foreignType)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Creates one representative registered-type descriptor. */
    private static RegisteredTypeDescriptor typeDescriptor() {
        return new RegisteredTypeDescriptor(
                new RegisteredType("example.game/group-3d", 1),
                RegisteredTypeScope.SCENE_NODE,
                DescriptorPresentation.described("Group 3d", "Groups child nodes."),
                List.of(PropertyDescriptor.required(
                        "name", ProjectValueKind.TEXT, DescriptorPresentation.named("Name"), Map.of(), Set.of())),
                List.of(EndpointDescriptor.withoutPayload("ready", DescriptorPresentation.named("Ready"))),
                List.of(EndpointDescriptor.withoutPayload("reset", DescriptorPresentation.named("Reset"))),
                List.of("org.jscene3d.render/mesh-3d"));
    }

    /** Creates one representative extension descriptor. */
    private static ExtensionDescriptor extension(RegisteredTypeDescriptor type) {
        return new ExtensionDescriptor(
                "example.game", "1.0.0", ">=0.1.0 <0.2.0", DescriptorPresentation.named("Example Game"), List.of(type));
    }
}
