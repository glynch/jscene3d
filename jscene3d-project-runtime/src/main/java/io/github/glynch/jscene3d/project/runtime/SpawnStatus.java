/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Closed states of one transactional spawn operation. */
public enum SpawnStatus {
    /** The request is waiting for its next structural commit point. */
    PENDING,
    /** The complete instance was activated and published. */
    ACTIVE,
    /** The request was cancelled before composition began. */
    CANCELLED,
    /** Preparation validation, composition, or activation failed without publishing the instance. */
    FAILED
}
