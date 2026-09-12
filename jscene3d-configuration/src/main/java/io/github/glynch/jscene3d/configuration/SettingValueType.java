/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Built-in value families from which the editor generates setting controls. */
public enum SettingValueType {
    /** Boolean value rendered as a checkbox. */
    BOOLEAN(Boolean.class),
    /** Signed 32-bit integer value. */
    INTEGER(Integer.class),
    /** Arbitrary-precision decimal value. */
    NUMBER(BigDecimal.class),
    /** Plain text value. */
    STRING(String.class),
    /** Filesystem path stored portably as text. */
    PATH(Path.class),
    /** One text value selected from declared choices. */
    ENUM(String.class);

    private final Class<?> valueClass;

    SettingValueType(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /** Returns the Java type exposed by a key using this value family. */
    public Class<?> valueClass() {
        return valueClass;
    }

    /** Parses the lowercase descriptor spelling. */
    public static SettingValueType parse(String value) {
        return valueOf(Objects.requireNonNull(value, "value").replace('-', '_').toUpperCase(Locale.ROOT));
    }

    /** Converts one Jackson-compatible stored value into this family's typed value. */
    public Object convert(Object value) {
        Objects.requireNonNull(value, "value");
        return switch (this) {
            case BOOLEAN -> requireType(value, Boolean.class);
            case INTEGER -> integer(value);
            case NUMBER -> decimal(value);
            case STRING, ENUM -> requireType(value, String.class);
            case PATH -> path(value);
        };
    }

    /** Converts one typed value into a Jackson-compatible stored value. */
    public Object store(Object value) {
        Object converted = convert(value);
        if (this == PATH) {
            return ((Path) converted).toString().replace(File.separatorChar, '/');
        }
        return converted;
    }

    private static Object requireType(Object value, Class<?> type) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("setting value must be " + type.getSimpleName());
        }
        return value;
    }

    private static Integer integer(Object value) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal.intValueExact();
            }
            if (value instanceof BigInteger integer) {
                return integer.intValueExact();
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                return ((Number) value).intValue();
            }
            if (value instanceof Long integer) {
                return Math.toIntExact(integer);
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("setting value must be a signed 32-bit integer", exception);
        }
        throw new IllegalArgumentException("setting value must be an integer");
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        throw new IllegalArgumentException("setting value must be a number");
    }

    private static Path path(Object value) {
        if (value instanceof Path path) {
            return path.normalize();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Path.of(text).normalize();
        }
        throw new IllegalArgumentException("setting value must be a non-blank path");
    }
}
