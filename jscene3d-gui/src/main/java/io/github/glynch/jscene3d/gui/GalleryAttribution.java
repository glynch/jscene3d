/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.gui.internal.Preconditions;
import java.net.URI;
import java.util.Objects;

/**
 * Immutable source and licensing metadata for one third-party example asset.
 *
 * @param assetName non-blank asset name
 * @param creator non-blank creator or rights-holder name
 * @param sourceName non-blank human-readable source name
 * @param source absolute original source URI
 * @param licenseName non-blank license name or SPDX identifier
 * @param license absolute license URI
 */
public record GalleryAttribution(
        String assetName, String creator, String sourceName, URI source, String licenseName, URI license) {
    /** Validates required display text and absolute source locations. */
    public GalleryAttribution {
        assetName = Preconditions.requireNonBlank(assetName, "assetName");
        creator = Preconditions.requireNonBlank(creator, "creator");
        sourceName = Preconditions.requireNonBlank(sourceName, "sourceName");
        source = requireAbsolute(source, "source");
        licenseName = Preconditions.requireNonBlank(licenseName, "licenseName");
        license = requireAbsolute(license, "license");
    }

    /** Requires an absolute URI suitable for durable provenance metadata. */
    private static URI requireAbsolute(URI uri, String parameterName) {
        URI validUri = Objects.requireNonNull(uri, parameterName);
        if (!validUri.isAbsolute()) {
            throw new IllegalArgumentException(parameterName + " must be absolute: " + validUri);
        }
        return validUri;
    }
}
