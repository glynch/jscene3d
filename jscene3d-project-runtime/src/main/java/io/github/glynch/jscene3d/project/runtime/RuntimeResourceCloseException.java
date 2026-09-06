/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Objects;

/** Identifies a runtime-resource lease which failed while world cleanup continued. */
public final class RuntimeResourceCloseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Portable identity of the resource whose lease failed to close. */
    private final ResourceReference reference;

    /**
     * Wraps one lease-release failure with its portable resource reference.
     *
     * @param reference resource whose lease failed to close
     * @param cause lease-release failure
     */
    public RuntimeResourceCloseException(ResourceReference reference, RuntimeException cause) {
        super("runtime resource lease cleanup failed: " + Objects.requireNonNull(reference, "reference"), cause);
        this.reference = reference;
    }

    /**
     * Returns the resource whose lease failed to close.
     *
     * @return portable resource reference
     */
    public ResourceReference reference() {
        return reference;
    }
}
