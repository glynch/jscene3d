/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Narrow semantic-version value used for project compatibility checks.
 *
 * @param major major version number
 * @param minor minor version number
 * @param patch patch version number
 * @param preRelease optional prerelease identifiers
 */
public record SemanticVersion(
        int major, int minor, int patch, @Nullable String preRelease) implements Comparable<SemanticVersion> {
    private static final Pattern PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([\\dA-Za-z.-]+))?(?:\\+[\\dA-Za-z.-]+)?$");

    /** Parses a semantic version without throwing for project-authored input.
     *
     * @param value candidate semantic-version text
     * @return parsed version, or empty when the text is invalid or numerically unsupported
     */
    public static Optional<SemanticVersion> parse(String value) {
        Matcher matcher = PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SemanticVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int numericComparison = compareNumeric(other);
        if (numericComparison != 0) {
            return numericComparison;
        }
        if (preRelease == null) {
            return other.preRelease == null ? 0 : 1;
        }
        if (other.preRelease == null) {
            return -1;
        }
        return comparePreRelease(preRelease, other.preRelease);
    }

    /** Compares the three required numeric components. */
    private int compareNumeric(SemanticVersion other) {
        int comparison = Integer.compare(major, other.major);
        if (comparison == 0) {
            comparison = Integer.compare(minor, other.minor);
        }
        if (comparison == 0) {
            comparison = Integer.compare(patch, other.patch);
        }
        return comparison;
    }

    /** Compares dot-separated pre-release identifiers using semantic-version precedence. */
    private static int comparePreRelease(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int sharedLength = Math.min(leftParts.length, rightParts.length);
        for (int index = 0; index < sharedLength; index++) {
            int comparison = comparePreReleasePart(leftParts[index], rightParts[index]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(leftParts.length, rightParts.length);
    }

    /** Compares numeric identifiers below textual identifiers. */
    private static int comparePreReleasePart(String left, String right) {
        boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            return compareNumericText(left, right);
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    /** Compares arbitrarily long unsigned numeric text without parsing overflow. */
    private static int compareNumericText(String left, String right) {
        String normalizedLeft = stripLeadingZeroes(left);
        String normalizedRight = stripLeadingZeroes(right);
        int comparison = Integer.compare(normalizedLeft.length(), normalizedRight.length());
        return comparison != 0 ? comparison : normalizedLeft.compareTo(normalizedRight);
    }

    /** Retains one zero when an identifier contains only zeroes. */
    private static String stripLeadingZeroes(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }
}
