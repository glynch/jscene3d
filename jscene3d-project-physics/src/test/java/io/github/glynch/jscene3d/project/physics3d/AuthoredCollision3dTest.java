/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Exercises defensive decoding at the trusted runtime-extension boundary. */
final class AuthoredCollision3dTest {
    /** Rejects missing or structurally invalid authored shape values. */
    @Test
    void rejectsInvalidShapeAndPositionValues() {
        Map<PropertyId, ProjectValue> missingShape = replaced(Physics3dDescriptors.shapeProperty(), null);
        Map<PropertyId, ProjectValue> textShape =
                replaced(Physics3dDescriptors.shapeProperty(), new ProjectValue.TextValue("box"));
        Map<PropertyId, ProjectValue> textPosition =
                replaced(Physics3dDescriptors.localPositionProperty(), new ProjectValue.TextValue("origin"));
        Map<PropertyId, ProjectValue> shortPosition =
                replaced(Physics3dDescriptors.localPositionProperty(), array(number(0), number(0)));
        Map<PropertyId, ProjectValue> mixedPosition = replaced(
                Physics3dDescriptors.localPositionProperty(),
                array(number(0), new ProjectValue.TextValue("zero"), number(0)));
        Map<PropertyId, ProjectValue> infinitePosition =
                replaced(Physics3dDescriptors.localPositionProperty(), array(number(0), number("1e1000"), number(0)));

        assertThatThrownBy(() -> AuthoredCollision3d.shape(missingShape))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(textShape))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resource reference");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(textPosition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(shortPosition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 3");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(mixedPosition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only numbers");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(infinitePosition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite numbers");
    }

    /** Rejects invalid orientations and collision-filter integers. */
    @Test
    void rejectsInvalidOrientationAndFilterValues() {
        Map<PropertyId, ProjectValue> zeroOrientation = replaced(
                Physics3dDescriptors.localOrientationProperty(), array(number(0), number(0), number(0), number(0)));
        Map<PropertyId, ProjectValue> textCategory =
                replaced(Physics3dDescriptors.categoryBitsProperty(), new ProjectValue.TextValue("all"));
        Map<PropertyId, ProjectValue> fractionalMask = replaced(Physics3dDescriptors.maskBitsProperty(), number("1.5"));

        assertThatThrownBy(() -> AuthoredCollision3d.shape(zeroOrientation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-zero");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(textCategory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an integer");
        assertThatThrownBy(() -> AuthoredCollision3d.shape(fractionalMask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32-bit integer");
    }

    /** Returns a complete effective property map with one replacement or removal. */
    private static Map<PropertyId, ProjectValue> replaced(PropertyId property, @Nullable ProjectValue replacement) {
        Map<PropertyId, ProjectValue> result = new LinkedHashMap<>(validProperties());
        if (replacement == null) {
            result.remove(property);
        } else {
            result.put(property, replacement);
        }
        return result;
    }

    /** Creates one complete effective collision-shape property set. */
    private static Map<PropertyId, ProjectValue> validProperties() {
        return Map.of(
                Physics3dDescriptors.shapeProperty(),
                new ProjectValue.ReferenceValue(ResourceReference.asset("box")),
                Physics3dDescriptors.localPositionProperty(),
                array(number(0), number(0), number(0)),
                Physics3dDescriptors.localOrientationProperty(),
                array(number(0), number(0), number(0), number(1)),
                Physics3dDescriptors.categoryBitsProperty(),
                number(1),
                Physics3dDescriptors.maskBitsProperty(),
                number(-1));
    }

    /** Creates one portable array. */
    private static ProjectValue.ArrayValue array(ProjectValue... values) {
        return new ProjectValue.ArrayValue(List.of(values));
    }

    /** Creates one portable integer. */
    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Creates one portable decimal from an exact string. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }
}
