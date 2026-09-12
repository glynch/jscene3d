/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional constraints and presentation hints applied by the registry and generated UI.
 *
 * @param minimum inclusive numeric minimum
 * @param maximum inclusive numeric maximum
 * @param step positive numeric editor step
 * @param choices allowed values for an enumerated setting
 * @param pathKind expected filesystem target for a path setting
 * @param projectRelative whether a path must remain beneath the project root
 */
public record SettingConstraints(
        Optional<BigDecimal> minimum,
        Optional<BigDecimal> maximum,
        Optional<BigDecimal> step,
        List<SettingChoice> choices,
        Optional<SettingPathKind> pathKind,
        boolean projectRelative) {
    /** Empty constraints suitable for boolean and text settings. */
    public static final SettingConstraints NONE = new SettingConstraints(
            Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), false);

    /** Copies and validates one constraint set. */
    public SettingConstraints {
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        Objects.requireNonNull(step, "step");
        choices = List.copyOf(choices);
        Objects.requireNonNull(pathKind, "pathKind");
        if (minimum.isPresent() && maximum.isPresent() && minimum.orElseThrow().compareTo(maximum.orElseThrow()) > 0) {
            throw new IllegalArgumentException("setting minimum must not exceed maximum");
        }
        if (step.isPresent() && step.orElseThrow().signum() <= 0) {
            throw new IllegalArgumentException("setting step must be positive");
        }
        HashSet<String> choiceValues = new HashSet<>();
        for (SettingChoice choice : choices) {
            if (!choiceValues.add(
                    Objects.requireNonNull(choice, "choices entry").value())) {
                throw new IllegalArgumentException("setting choice is duplicated: " + choice.value());
            }
        }
    }

    /** Creates constraints for an enumerated text setting. */
    public static SettingConstraints choices(List<SettingChoice> choices) {
        return new SettingConstraints(
                Optional.empty(), Optional.empty(), Optional.empty(), choices, Optional.empty(), false);
    }

    /** Creates constraints for one path editor. */
    public static SettingConstraints path(SettingPathKind kind, boolean projectRelative) {
        return new SettingConstraints(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                Optional.of(Objects.requireNonNull(kind, "kind")),
                projectRelative);
    }

    /** Validates one already converted value for its declared value family. */
    public void validate(SettingValueType type, Object value) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        validateShape(type);
        if (type == SettingValueType.INTEGER || type == SettingValueType.NUMBER) {
            BigDecimal number =
                    type == SettingValueType.INTEGER ? BigDecimal.valueOf((Integer) value) : (BigDecimal) value;
            minimum.ifPresent(bound -> require(number.compareTo(bound) >= 0, "setting value is below its minimum"));
            maximum.ifPresent(bound -> require(number.compareTo(bound) <= 0, "setting value exceeds its maximum"));
        }
        if (type == SettingValueType.ENUM
                && choices.stream().noneMatch(choice -> choice.value().equals(value))) {
            throw new IllegalArgumentException("setting value is not one of its declared choices");
        }
        if (type == SettingValueType.PATH && projectRelative) {
            Path path = (Path) value;
            if (path.isAbsolute() || path.toString().isBlank() || path.startsWith("..")) {
                throw new IllegalArgumentException("setting path must remain inside the project workspace");
            }
        }
    }

    private void validateShape(SettingValueType type) {
        boolean numeric = type == SettingValueType.INTEGER || type == SettingValueType.NUMBER;
        if (!numeric && (minimum.isPresent() || maximum.isPresent() || step.isPresent())) {
            throw new IllegalArgumentException("numeric constraints require an integer or number setting");
        }
        if (type != SettingValueType.ENUM && !choices.isEmpty()) {
            throw new IllegalArgumentException("choices require an enum setting");
        }
        if (type == SettingValueType.ENUM && choices.isEmpty()) {
            throw new IllegalArgumentException("enum settings require at least one choice");
        }
        if (type != SettingValueType.PATH && (pathKind.isPresent() || projectRelative)) {
            throw new IllegalArgumentException("path constraints require a path setting");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
