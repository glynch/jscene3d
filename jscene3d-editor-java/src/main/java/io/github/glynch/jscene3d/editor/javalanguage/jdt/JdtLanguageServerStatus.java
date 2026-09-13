/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable lifecycle state published by the project-scoped JDT LS adapter.
 *
 * @param state current lifecycle state
 * @param detail optional diagnostic detail for the current state
 */
public record JdtLanguageServerStatus(State state, Optional<String> detail) {
    /** Supported language-server lifecycle states. */
    public enum State {
        /** No Java project session is active. */
        INACTIVE,
        /** The distribution, process, or protocol connection is being prepared. */
        STARTING,
        /** JDT LS has reported that its language service is ready. */
        READY,
        /** Startup, initialization, or the running process has failed. */
        FAILED
    }

    /** Validates one complete lifecycle status. */
    public JdtLanguageServerStatus {
        Objects.requireNonNull(state, "state");
        detail = Objects.requireNonNull(detail, "detail").map(String::strip).filter(value -> !value.isEmpty());
    }

    /**
     * Returns the hidden status used when no Java project session is active.
     *
     * @return inactive lifecycle status
     */
    public static JdtLanguageServerStatus inactive() {
        return new JdtLanguageServerStatus(State.INACTIVE, Optional.empty());
    }

    /**
     * Returns the status used while process and protocol initialization are in progress.
     *
     * @return starting lifecycle status
     */
    public static JdtLanguageServerStatus starting() {
        return new JdtLanguageServerStatus(State.STARTING, Optional.empty());
    }

    /**
     * Returns a startup status carrying detail reported by JDT LS.
     *
     * @param detail current JDT LS startup detail
     * @return starting lifecycle status
     */
    public static JdtLanguageServerStatus starting(String detail) {
        return new JdtLanguageServerStatus(State.STARTING, Optional.of(detail));
    }

    /**
     * Returns the status used after JDT LS reports {@code ServiceReady}.
     *
     * @return ready lifecycle status
     */
    public static JdtLanguageServerStatus ready() {
        return new JdtLanguageServerStatus(State.READY, Optional.empty());
    }

    /**
     * Returns a failed status with diagnostic detail suitable for a tooltip or log.
     *
     * @param detail failure detail suitable for a tooltip or log
     * @return failed lifecycle status
     */
    public static JdtLanguageServerStatus failed(String detail) {
        return new JdtLanguageServerStatus(State.FAILED, Optional.of(detail));
    }
}
