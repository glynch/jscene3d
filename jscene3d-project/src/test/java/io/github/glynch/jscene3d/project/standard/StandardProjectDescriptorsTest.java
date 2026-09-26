/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantic;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
import io.github.glynch.jscene3d.project.standard.physics3d.StandardPhysics3dDescriptors;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies the safe, authoritative built-in descriptor catalog. */
final class StandardProjectDescriptorsTest {
    /** Publishes every built-in descriptor once and in dependency order. */
    @Test
    void publishesBuiltInDescriptors() {
        assertThat(StandardProjectDescriptors.all())
                .extracting(ExtensionDescriptor::id)
                .containsExactly(
                        "io.github.glynch.jscene3d.application",
                        "io.github.glynch.jscene3d.spatial3d",
                        "io.github.glynch.jscene3d.physics3d",
                        "io.github.glynch.jscene3d.game3d",
                        "io.github.glynch.jscene3d.presentation");
    }

    /** Retains editor semantics in safe descriptor metadata. */
    @Test
    void publishesEditorSemantics() {
        var transform =
                StandardSpatial3dDescriptors.extensionDescriptor().components().getFirst();

        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.positionProperty())
                        .editorMetadata())
                .containsEntry(
                        PropertyDescriptorKeys.EDITOR_SEMANTIC,
                        new ProjectValue.TextValue(PropertyEditorSemantics.VECTOR3));
    }

    /** Corrects both version-one ordinary orientation properties to authored XYZ degrees. */
    @Test
    void publishesEulerOrientationDescriptors() {
        ComponentTypeDescriptor transform = StandardSpatial3dDescriptors.extensionDescriptor().components().stream()
                .filter(component -> component.type().equals(StandardSpatial3dDescriptors.transformType()))
                .findFirst()
                .orElseThrow();
        ComponentTypeDescriptor shape = StandardPhysics3dDescriptors.extensionDescriptor().components().stream()
                .filter(component -> component.type().equals(StandardPhysics3dDescriptors.collisionShapeType()))
                .findFirst()
                .orElseThrow();

        assertThat(transform.type().version()).isEqualTo(1);
        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.orientationProperty())
                        .editor()
                        .semantic())
                .isEqualTo(PropertyEditorSemantic.EULER_ROTATION);
        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.orientationProperty())
                        .exactElementCount())
                .contains(3);
        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.orientationProperty())
                        .defaultValue())
                .contains(numbers("0.0", "0.0", "0.0"));
        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.orientationProperty())
                        .presentation()
                        .description())
                .contains("Local Euler rotation in degrees, applied in canonical XYZ order");
        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.orientationProperty())
                        .accepts(numbers("0", "0", "0", "1")))
                .isFalse();
        assertThat(shape.type().version()).isEqualTo(1);
        assertThat(shape.properties()
                        .get(StandardPhysics3dDescriptors.localOrientationProperty())
                        .editor()
                        .semantic())
                .isEqualTo(PropertyEditorSemantic.EULER_ROTATION);
        assertThat(shape.properties()
                        .get(StandardPhysics3dDescriptors.localOrientationProperty())
                        .exactElementCount())
                .contains(3);
        assertThat(shape.properties()
                        .get(StandardPhysics3dDescriptors.localOrientationProperty())
                        .defaultValue())
                .contains(numbers("0.0", "0.0", "0.0"));
        assertThat(shape.properties()
                        .get(StandardPhysics3dDescriptors.localOrientationProperty())
                        .presentation()
                        .description())
                .contains("Shape-local Euler rotation in degrees, applied in canonical XYZ order");
        assertThat(shape.properties()
                        .get(StandardPhysics3dDescriptors.localOrientationProperty())
                        .accepts(numbers("0", "0", "0", "1")))
                .isFalse();
    }

    /** Creates a portable numeric array from exact decimal spellings. */
    private static ProjectValue.ArrayValue numbers(String... values) {
        List<ProjectValue> numbers = Arrays.stream(values)
                .map(BigDecimal::new)
                .map(ProjectValue.NumberValue::new)
                .map(ProjectValue.class::cast)
                .toList();
        return new ProjectValue.ArrayValue(numbers);
    }
}
