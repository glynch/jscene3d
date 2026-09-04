/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Stable high-level phases reported by import orchestration and adapters. */
public enum ImportPhase {
    /** Source discovery and inspection. */
    INSPECTING,
    /** Source or dependency input reading. */
    READING,
    /** Source decoding or interpretation. */
    DECODING,
    /** Transactional import preparation. */
    PREPARING,
    /** Serialized artifact writing. */
    WRITING,
    /** Artifact and fingerprint validation. */
    VALIDATING,
    /** Atomic generation publication. */
    COMMITTING
}
