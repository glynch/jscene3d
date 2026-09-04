/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Nullable deserialization model kept behind the validated public descriptor.
 *
 * @param schema optional schema identifier
 * @param schemaVersion authoritative manifest schema version
 * @param identity nullable raw identity
 * @param authors nullable raw authors
 * @param links nullable raw public links
 * @param legal nullable raw legal references
 * @param engine nullable raw engine compatibility
 * @param runtime nullable raw runtime configuration
 * @param extensions nullable raw extension requirements
 * @param assets nullable raw source assets
 * @param imports nullable raw import definitions
 * @param exportPresets nullable raw export presets
 * @param catalog nullable raw catalog metadata
 */
public record RawManifest(
        @JsonProperty("$schema") @Nullable String schema,
        int schemaVersion,
        @Nullable Identity identity,
        @Nullable List<@Nullable Author> authors,
        @Nullable Links links,
        @Nullable Legal legal,
        @Nullable Engine engine,
        @Nullable RuntimeConfiguration runtime,
        @Nullable List<@Nullable ExtensionRequirement> extensions,
        @Nullable List<@Nullable Asset> assets,
        @Nullable List<@Nullable String> imports,
        @Nullable List<@Nullable String> exportPresets,
        @Nullable Catalog catalog) {
    /** Nullable raw identity. */
    record Identity(
            @Nullable String id,
            @Nullable String name,
            @Nullable String version,
            @Nullable String created,
            @Nullable String released,
            @Nullable String description,
            @Nullable String icon) {}

    /** Nullable raw author. */
    record Author(
            @Nullable String name,
            @Nullable List<@Nullable String> roles,
            @Nullable String url) {}

    /** Nullable raw public links. */
    record Links(
            @Nullable String homepage,
            @Nullable String source,
            @Nullable String issues) {}

    /** Nullable raw project license. */
    record ProjectLicense(
            @Nullable String expression, @Nullable String file) {}

    /** Nullable raw legal references. */
    record Legal(
            @Nullable ProjectLicense projectLicense,
            @Nullable String thirdPartyNotices,
            @Nullable String credits) {}

    /** Nullable raw engine compatibility. */
    record Engine(@Nullable String requires, @Nullable String authoredWith) {}

    /** Nullable raw runtime configuration. */
    record RuntimeConfiguration(
            @Nullable String applicationExtension,
            @Nullable String entryScene,
            @Nullable String projectSystems,
            @Nullable String inputMap) {}

    /** Nullable raw extension requirement. */
    record ExtensionRequirement(
            @Nullable String id, @Nullable String requires) {}

    /** Nullable raw asset source. */
    record Asset(
            @Nullable String id,
            @Nullable String type,
            @Nullable String path,
            @Nullable String sha256) {}

    /** Nullable raw player-count range. */
    record Players(int minimum, int maximum) {}

    /** Nullable raw catalog metadata. */
    record Catalog(
            @Nullable List<@Nullable String> genres,
            @Nullable List<@Nullable String> tags,
            @Nullable Players players,
            @Nullable List<@Nullable String> contentWarnings) {}
}
