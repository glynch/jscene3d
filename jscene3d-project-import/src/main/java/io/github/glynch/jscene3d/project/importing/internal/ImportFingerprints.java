/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

/**
 * Related fingerprints that identify one prepared import generation.
 *
 * @param definition authored definition and importer fingerprint
 * @param source primary source-content fingerprint
 * @param complete complete definition, source, and dependency fingerprint
 */
record ImportFingerprints(String definition, String source, String complete) {}
