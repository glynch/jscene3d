/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises independently releasable runtime-resource retention. */
final class RuntimeResourceLeaseTest {
    /** Exposes its value only while open and invokes release at most once. */
    @Test
    void closesIdempotentlyAndTerminally() {
        List<String> events = new ArrayList<>();
        RuntimeResourceLease<String> lease = RuntimeResourceLease.of("resource", () -> events.add("release"));

        assertThat(lease.value()).isEqualTo("resource");

        lease.close();
        lease.close();

        assertThat(lease.isClosed()).isTrue();
        assertThat(events).containsExactly("release");
        assertThatThrownBy(lease::value)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Remains terminal when its release operation fails. */
    @Test
    void remainsClosedAfterReleaseFailure() {
        RuntimeResourceLease<String> lease = RuntimeResourceLease.of("resource", () -> {
            throw new IllegalStateException("release failed");
        });

        assertThatThrownBy(lease::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("release failed");

        assertThat(lease.isClosed()).isTrue();
        lease.close();
    }
}
