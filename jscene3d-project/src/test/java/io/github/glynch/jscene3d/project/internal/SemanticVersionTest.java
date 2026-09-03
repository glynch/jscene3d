/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Covers semantic-version precedence used at the engine compatibility boundary. */
final class SemanticVersionTest {
    /** Accepts standard versions, prereleases, and ignored build metadata. */
    @Test
    void parsesSemanticVersions() {
        SemanticVersion release = version("1.2.3+build.7");
        SemanticVersion prerelease = version("1.2.3-rc.1+build.8");

        assertThat(release).isEqualTo(new SemanticVersion(1, 2, 3, null)).isGreaterThan(prerelease);
        assertThat(prerelease.preRelease()).isEqualTo("rc.1");
    }

    /** Rejects malformed input and numeric components too large for this implementation. */
    @Test
    void rejectsInvalidSemanticVersions() {
        assertThat(SemanticVersion.parse("latest")).isEmpty();
        assertThat(SemanticVersion.parse("01.2.3")).isEmpty();
        assertThat(SemanticVersion.parse("999999999999999999999.2.3")).isEmpty();
    }

    /** Implements the significant semantic-version prerelease ordering rules. */
    @Test
    void ordersPrereleaseIdentifiers() {
        assertThat(version("1.0.0-alpha")).isLessThan(version("1.0.0-alpha.1"));
        assertThat(version("1.0.0-alpha.1")).isLessThan(version("1.0.0-alpha.beta"));
        assertThat(version("1.0.0-beta.2")).isLessThan(version("1.0.0-beta.11"));
        assertThat(version("1.0.0-beta.11")).isLessThan(version("1.0.0-rc.1"));
        assertThat(version("1.0.0-rc.1")).isLessThan(version("1.0.0"));
        assertThat(version("1.0.0-000000000000000000002")).isLessThan(version("1.0.0-000000000000000000011"));
        assertThat(version("1.0.0-2")).isLessThan(version("1.0.0-word"));
        assertThat(version("1.0.0-word")).isGreaterThan(version("1.0.0-2"));
        assertThat(version("1.0.0-same")).isEqualByComparingTo(version("1.0.0-same"));
    }

    /** Evaluates every supported version-requirement comparison operator. */
    @Test
    void evaluatesEngineRequirementOperators() {
        assertThat(requirement(">=1.2.3 <2.0.0").includes(version("1.2.3"))).isTrue();
        assertThat(requirement(">1.2.3").includes(version("1.2.4"))).isTrue();
        assertThat(requirement(">1.2.3").includes(version("1.2.3"))).isFalse();
        assertThat(requirement("<=1.2.3").includes(version("1.2.3"))).isTrue();
        assertThat(requirement("<1.2.3").includes(version("1.2.2"))).isTrue();
        assertThat(requirement("=1.2.3").includes(version("1.2.3"))).isTrue();
        assertThat(requirement("1.2.3").includes(version("1.2.4"))).isFalse();
    }

    /** Rejects empty requirements and any requirement containing an invalid clause. */
    @Test
    void rejectsInvalidEngineRequirements() {
        assertThat(EngineVersionRequirement.parse(" ")).isEmpty();
        assertThat(EngineVersionRequirement.parse(">=1.2.3 nonsense")).isEmpty();
    }

    /** Parses a version known to be valid for a test case. */
    private static SemanticVersion version(String text) {
        return SemanticVersion.parse(text).orElseThrow();
    }

    /** Parses a requirement known to be valid for a test case. */
    private static EngineVersionRequirement requirement(String text) {
        Optional<EngineVersionRequirement> requirement = EngineVersionRequirement.parse(text);
        assertThat(requirement).isPresent();
        return requirement.orElseThrow();
    }
}
