/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** JSON persistence model for one immutable imported artifact.
 *
 * @param identity importer-local artifact identity
 * @param kind serialized artifact kind
 * @param resourceType optional registered resource type identity
 * @param resourceTypeVersion optional registered resource type version
 * @param mediaType serialized media type
 * @param references referenced output identities
 * @param contentFingerprint lowercase content SHA-256 fingerprint
 * @param size serialized byte size
 * @param file generation-relative content path
 */
public record CachedArtifact(
        String identity,
        String kind,
        @Nullable String resourceType,
        @Nullable Integer resourceTypeVersion,
        String mediaType,
        List<String> references,
        String contentFingerprint,
        long size,
        String file) {}
