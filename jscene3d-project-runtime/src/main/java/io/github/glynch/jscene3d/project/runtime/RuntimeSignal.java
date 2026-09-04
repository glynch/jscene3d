/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Declared signal output bound to the runtime connection graph. */
public interface RuntimeSignal {
    /** Emits a signal declared without a payload. */
    void emit();

    /**
     * Emits a signal carrying its declared payload type.
     *
     * @param payload payload matching the signal's declared type
     */
    void emit(RuntimePayload payload);
}
