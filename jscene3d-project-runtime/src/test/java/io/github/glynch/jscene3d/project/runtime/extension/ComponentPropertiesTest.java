/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ComponentPropertiesTest {
    private static final PropertyId SPEED = new PropertyId("speed");
    private static final PropertyId LABEL = new PropertyId("label");
    private static final PropertyId ENABLED = new PropertyId("enabled");
    private static final PropertyId MESH = new PropertyId("mesh");

    @Test
    void readsTypedEffectiveValues() {
        ResourceReference reference = ResourceReference.asset("garden-model");
        Map<PropertyId, ProjectValue> values = new LinkedHashMap<>();
        values.put(SPEED, new ProjectValue.NumberValue(new BigDecimal("2.5")));
        values.put(LABEL, new ProjectValue.TextValue("Player"));
        values.put(ENABLED, new ProjectValue.BooleanValue(true));
        values.put(MESH, new ProjectValue.ReferenceValue(reference));

        ComponentProperties properties = new ComponentProperties(values);

        assertThat(properties.finiteFloat(SPEED)).isEqualTo(2.5F);
        assertThat(properties.text(LABEL)).isEqualTo("Player");
        assertThat(properties.booleanValue(ENABLED)).isTrue();
        assertThat(properties.resourceReference(MESH)).isEqualTo(reference);
        assertThat(properties.values()).containsExactlyEntriesOf(values);
    }

    @Test
    void rejectsMissingMismatchedAndUnrepresentableValues() {
        ComponentProperties missing = new ComponentProperties(Map.of());
        ComponentProperties mismatched = new ComponentProperties(Map.of(SPEED, new ProjectValue.TextValue("fast")));
        ComponentProperties overflowing =
                new ComponentProperties(Map.of(SPEED, new ProjectValue.NumberValue(new BigDecimal("1e1000"))));

        assertThatThrownBy(() -> missing.value(SPEED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("component property is missing: speed");
        assertThatThrownBy(() -> mismatched.finiteFloat(SPEED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("speed must be a number");
        assertThatThrownBy(() -> overflowing.finiteFloat(SPEED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("speed must be representable as a finite float");
    }

    @Test
    void snapshotsConstructionValues() {
        Map<PropertyId, ProjectValue> source = new LinkedHashMap<>();
        source.put(LABEL, new ProjectValue.TextValue("original"));
        ComponentProperties properties = new ComponentProperties(source);

        source.put(LABEL, new ProjectValue.TextValue("changed"));
        Map<PropertyId, ProjectValue> immutableValues = properties.values();

        assertThat(properties.text(LABEL)).isEqualTo("original");
        assertThatThrownBy(immutableValues::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}
