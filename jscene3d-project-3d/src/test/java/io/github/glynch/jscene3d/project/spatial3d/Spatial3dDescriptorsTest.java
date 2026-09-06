/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies the stable safe descriptor contract for built-in 3D spatial state. */
final class Spatial3dDescriptorsTest {
    /** Publishes exact stable identities and one primary spatial authority. */
    @Test
    void describesTransform3d() {
        ComponentTypeDescriptor transform =
                Spatial3dDescriptors.extensionDescriptor().components().getFirst();

        assertThat(Spatial3dDescriptors.extensionId()).isEqualTo("io.github.glynch.jscene3d.spatial3d");
        assertThat(transform.type()).isEqualTo(Spatial3dDescriptors.transformType());
        assertThat(transform.spatialDomain()).isEqualTo(ComponentSpatialDomain.THREE_DIMENSIONAL);
        assertThat(transform.providedCapabilities()).containsExactly(Spatial3dDescriptors.spatialCapability());
        assertThat(transform.properties().values())
                .extracting(PropertyDescriptor::id)
                .containsExactly("position", "orientation", "scale");
    }

    /** Supplies portable defaults and editor semantics without executable metadata. */
    @Test
    void describesPortableTransformProperties() {
        List<PropertyDescriptor> properties =
                Spatial3dDescriptors.extensionDescriptor().components().getFirst().properties().values().stream()
                        .toList();

        assertThat(properties).allSatisfy(property -> {
            assertThat(property.valueKind()).isEqualTo(ProjectValueKind.ARRAY);
            assertThat(property.isRequired()).isFalse();
            assertThat(property.defaultValue()).isPresent();
        });
        assertThat(properties.get(0).editorMetadata()).containsEntry("semantic", new ProjectValue.TextValue("vector3"));
        assertThat(properties.get(1).editorMetadata())
                .containsEntry("semantic", new ProjectValue.TextValue("quaternion"));
        assertThat(properties.get(2).editorMetadata()).containsEntry("semantic", new ProjectValue.TextValue("vector3"));
        assertThat(array(properties.get(0))).containsExactly(0.0F, 0.0F, 0.0F);
        assertThat(array(properties.get(1))).containsExactly(0.0F, 0.0F, 0.0F, 1.0F);
        assertThat(array(properties.get(2))).containsExactly(1.0F, 1.0F, 1.0F);
    }

    /** Extracts float values from one descriptor default array. */
    private static List<Float> array(PropertyDescriptor property) {
        ProjectValue value = property.defaultValue().orElseThrow();
        return ((ProjectValue.ArrayValue) value)
                .values().stream()
                        .map(ProjectValue.NumberValue.class::cast)
                        .map(number -> number.value().floatValue())
                        .toList();
    }
}
