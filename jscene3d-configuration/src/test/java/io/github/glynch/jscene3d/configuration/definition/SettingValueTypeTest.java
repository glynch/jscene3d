/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies conversion between stored setting values and their declared Java types. */
class SettingValueTypeTest {
    @Test
    void exposesTheJavaTypeForEveryValueFamily() {
        assertThat(Map.of(
                        SettingValueType.BOOLEAN, Boolean.class,
                        SettingValueType.INTEGER, Integer.class,
                        SettingValueType.NUMBER, BigDecimal.class,
                        SettingValueType.STRING, String.class,
                        SettingValueType.PATH, Path.class,
                        SettingValueType.ENUM, String.class))
                .allSatisfy((type, valueClass) -> assertThat(type.valueClass()).isEqualTo(valueClass));
    }

    @Test
    void parsesDescriptorSpellings() {
        assertThat(SettingValueType.parse("boolean")).isEqualTo(SettingValueType.BOOLEAN);
        assertThat(SettingValueType.parse("integer")).isEqualTo(SettingValueType.INTEGER);
        assertThatNullPointerException().isThrownBy(() -> SettingValueType.parse(null));
        assertThatThrownBy(() -> SettingValueType.parse("not-a-value-family"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsBooleanAndTextValuesWithoutCoercion() {
        assertThat((Boolean) SettingValueType.BOOLEAN.convert(true)).isTrue();
        assertThat(SettingValueType.STRING.convert("text")).isEqualTo("text");
        assertThat(SettingValueType.ENUM.convert("dark")).isEqualTo("dark");
        assertThatThrownBy(() -> SettingValueType.BOOLEAN.convert("true"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Boolean");
        assertThatThrownBy(() -> SettingValueType.STRING.convert(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String");
        assertThatNullPointerException().isThrownBy(() -> SettingValueType.STRING.convert(null));
    }

    @Test
    void convertsEverySupportedIntegralRepresentation() {
        assertThat(SettingValueType.INTEGER.convert(new BigDecimal("12"))).isEqualTo(12);
        assertThat(SettingValueType.INTEGER.convert(BigInteger.valueOf(13))).isEqualTo(13);
        assertThat(SettingValueType.INTEGER.convert((byte) 14)).isEqualTo(14);
        assertThat(SettingValueType.INTEGER.convert((short) 15)).isEqualTo(15);
        assertThat(SettingValueType.INTEGER.convert(16)).isEqualTo(16);
        assertThat(SettingValueType.INTEGER.convert(17L)).isEqualTo(17);
    }

    @Test
    void rejectsFractionalOverflowingAndNonIntegralValues() {
        BigDecimal fractional = new BigDecimal("1.5");
        BigInteger overflowingBigInteger = BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE);

        assertThatThrownBy(() -> SettingValueType.INTEGER.convert(fractional))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signed 32-bit integer");
        assertThatThrownBy(() -> SettingValueType.INTEGER.convert(overflowingBigInteger))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signed 32-bit integer");
        assertThatThrownBy(() -> SettingValueType.INTEGER.convert(Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signed 32-bit integer");
        assertThatThrownBy(() -> SettingValueType.INTEGER.convert("12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integer");
    }

    @Test
    void convertsEverySupportedNumericRepresentation() {
        BigDecimal decimal = new BigDecimal("12.5");

        assertThat(SettingValueType.NUMBER.convert(decimal)).isSameAs(decimal);
        assertThat((BigDecimal) SettingValueType.NUMBER.convert(BigInteger.valueOf(13)))
                .isEqualByComparingTo("13");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert((byte) 14)).isEqualByComparingTo("14");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert((short) 15)).isEqualByComparingTo("15");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert(16)).isEqualByComparingTo("16");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert(17L)).isEqualByComparingTo("17");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert(18.5F)).isEqualByComparingTo("18.5");
        assertThat((BigDecimal) SettingValueType.NUMBER.convert(19.5D)).isEqualByComparingTo("19.5");
        assertThatThrownBy(() -> SettingValueType.NUMBER.convert("20"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("number");
    }

    @Test
    void convertsAndStoresPortablePaths() {
        Path normalized = Path.of("assets", "..", "resources", "image.png");

        assertThat(SettingValueType.PATH.convert(normalized)).isEqualTo(Path.of("resources", "image.png"));
        assertThat(SettingValueType.PATH.convert("assets/../resources/image.png"))
                .isEqualTo(Path.of("resources", "image.png"));
        assertThat(SettingValueType.PATH.store(normalized)).isEqualTo("resources/image.png");
        assertThat(SettingValueType.STRING.store("text")).isEqualTo("text");
        assertThatThrownBy(() -> SettingValueType.PATH.convert(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-blank path");
        assertThatThrownBy(() -> SettingValueType.PATH.convert(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-blank path");
    }
}
