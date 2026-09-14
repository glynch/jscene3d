/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies setting constraint construction and value validation. */
class SettingConstraintsTest {
    private static final SettingChoice DARK = new SettingChoice("dark", "Dark");
    private static final SettingChoice LIGHT = new SettingChoice("light", "Light");
    private static final Optional<BigDecimal> NO_DECIMAL = Optional.empty();
    private static final List<SettingChoice> NO_CHOICES = List.of();
    private static final Optional<SettingPathKind> NO_PATH_KIND = Optional.empty();

    @Test
    void copiesChoiceCollectionsAndProvidesFactories() {
        ArrayList<SettingChoice> supplied = new ArrayList<>(List.of(DARK));
        SettingConstraints choices = SettingConstraints.choices(supplied);
        supplied.add(LIGHT);

        assertThat(choices.choices()).containsExactly(DARK);
        List<SettingChoice> immutableChoices = choices.choices();
        assertThatThrownBy(() -> immutableChoices.add(LIGHT)).isInstanceOf(UnsupportedOperationException.class);

        SettingConstraints path = SettingConstraints.path(SettingPathKind.DIRECTORY, true);
        assertThat(path.pathKind()).contains(SettingPathKind.DIRECTORY);
        assertThat(path.projectRelative()).isTrue();
        assertThatNullPointerException().isThrownBy(() -> SettingConstraints.path(null, false));
    }

    @Test
    void rejectsInvalidConstraintDeclarations() {
        Optional<BigDecimal> two = decimal("2");
        Optional<BigDecimal> one = decimal("1");
        Optional<BigDecimal> zero = decimal("0");
        Optional<BigDecimal> negativeOne = decimal("-1");
        List<SettingChoice> duplicateChoices = List.of(DARK, DARK);
        List<SettingChoice> choicesWithNull = Arrays.asList(DARK, null);

        assertThatThrownBy(() -> new SettingConstraints(two, one, NO_DECIMAL, NO_CHOICES, NO_PATH_KIND, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum");
        assertThatThrownBy(() -> new SettingConstraints(NO_DECIMAL, NO_DECIMAL, zero, NO_CHOICES, NO_PATH_KIND, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() ->
                        new SettingConstraints(NO_DECIMAL, NO_DECIMAL, negativeOne, NO_CHOICES, NO_PATH_KIND, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new SettingConstraints(
                        NO_DECIMAL, NO_DECIMAL, NO_DECIMAL, duplicateChoices, NO_PATH_KIND, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicated");
        assertThatThrownBy(() -> new SettingConstraints(
                        NO_DECIMAL, NO_DECIMAL, NO_DECIMAL, choicesWithNull, NO_PATH_KIND, false))
                .isInstanceOf(NullPointerException.class);
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new SettingConstraints(null, NO_DECIMAL, NO_DECIMAL, NO_CHOICES, NO_PATH_KIND, false));
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new SettingConstraints(NO_DECIMAL, null, NO_DECIMAL, NO_CHOICES, NO_PATH_KIND, false));
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new SettingConstraints(NO_DECIMAL, NO_DECIMAL, null, NO_CHOICES, NO_PATH_KIND, false));
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new SettingConstraints(NO_DECIMAL, NO_DECIMAL, NO_DECIMAL, null, NO_PATH_KIND, false));
        assertThatNullPointerException()
                .isThrownBy(() -> new SettingConstraints(NO_DECIMAL, NO_DECIMAL, NO_DECIMAL, NO_CHOICES, null, false));
    }

    @Test
    void validatesInclusiveIntegerAndNumberBounds() {
        SettingConstraints constraints = constraints("1", "10", "0.5", List.of(), null, false);
        BigDecimal withinBounds = new BigDecimal("5.5");
        BigDecimal aboveMaximum = new BigDecimal("11");

        assertThatCode(() -> constraints.validate(SettingValueType.INTEGER, 1)).doesNotThrowAnyException();
        assertThatCode(() -> constraints.validate(SettingValueType.INTEGER, 10)).doesNotThrowAnyException();
        assertThatCode(() -> constraints.validate(SettingValueType.NUMBER, withinBounds))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> constraints.validate(SettingValueType.INTEGER, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("below");
        assertThatThrownBy(() -> constraints.validate(SettingValueType.NUMBER, aboveMaximum))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void validatesEnumeratedChoices() {
        SettingConstraints constraints = SettingConstraints.choices(List.of(DARK, LIGHT));

        assertThatCode(() -> constraints.validate(SettingValueType.ENUM, "dark"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> constraints.validate(SettingValueType.ENUM, "system"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declared choices");
    }

    @Test
    void validatesProjectRelativePaths() {
        SettingConstraints constraints = SettingConstraints.path(SettingPathKind.FILE, true);
        Path workspacePath = Path.of("assets/image.png");
        Path absolutePath = Path.of("image.png").toAbsolutePath();
        Path blankPath = Path.of("");
        Path escapingPath = Path.of("../image.png");

        assertThatCode(() -> constraints.validate(SettingValueType.PATH, workspacePath))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> constraints.validate(SettingValueType.PATH, absolutePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspace");
        assertThatThrownBy(() -> constraints.validate(SettingValueType.PATH, blankPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspace");
        assertThatThrownBy(() -> constraints.validate(SettingValueType.PATH, escapingPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workspace");
    }

    @Test
    void rejectsConstraintsAppliedToTheWrongValueFamily() {
        SettingConstraints numeric = constraints("1", null, null, List.of(), null, false);
        SettingConstraints choices = SettingConstraints.choices(List.of(DARK));
        SettingConstraints typedPath = SettingConstraints.path(SettingPathKind.FILE, false);
        SettingConstraints projectRelativePath = constraints(null, null, null, List.of(), null, true);

        assertThatThrownBy(() -> numeric.validate(SettingValueType.STRING, "text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numeric constraints");
        assertThatThrownBy(() -> choices.validate(SettingValueType.STRING, "dark"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("choices require");
        assertThatThrownBy(() -> SettingConstraints.NONE.validate(SettingValueType.ENUM, "dark"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one choice");
        assertThatThrownBy(() -> typedPath.validate(SettingValueType.STRING, "image.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path constraints");
        assertThatThrownBy(() -> projectRelativePath.validate(SettingValueType.STRING, "text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path constraints");
    }

    @Test
    void requiresATypeAndValue() {
        assertThatNullPointerException().isThrownBy(() -> SettingConstraints.NONE.validate(null, "text"));
        assertThatNullPointerException()
                .isThrownBy(() -> SettingConstraints.NONE.validate(SettingValueType.STRING, null));
        assertThatCode(() -> SettingConstraints.NONE.validate(SettingValueType.BOOLEAN, true))
                .doesNotThrowAnyException();
    }

    private static SettingConstraints constraints(
            String minimum,
            String maximum,
            String step,
            List<SettingChoice> choices,
            SettingPathKind pathKind,
            boolean projectRelative) {
        return new SettingConstraints(
                decimal(minimum),
                decimal(maximum),
                decimal(step),
                choices,
                Optional.ofNullable(pathKind),
                projectRelative);
    }

    private static Optional<BigDecimal> decimal(String value) {
        return Optional.ofNullable(value).map(BigDecimal::new);
    }
}
