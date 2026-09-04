/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies direct-construction invariants of the project manifest model. */
final class GameProjectModelTest {
    /** Rejects malformed identity, compatibility, and link values. */
    @Test
    void rejectsMalformedMetadata() {
        Optional<LocalDate> noDate = Optional.empty();
        Optional<String> noDescription = Optional.empty();
        Optional<Path> noIcon = Optional.empty();
        Optional<String> noAuthoredVersion = Optional.empty();
        Optional<URI> relativeUri = Optional.of(URI.create("relative"));
        Optional<URI> noUri = Optional.empty();

        assertThatThrownBy(() ->
                        new GameProject.Identity("Example", "Example", "1.0.0", noDate, noDate, noDescription, noIcon))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        new GameProject.Identity("example.game", "Example", "1", noDate, noDate, noDescription, noIcon))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameProject.EngineCompatibility("latest", noAuthoredVersion))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameProject.Links(relativeUri, noUri, noUri))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Normalizes valid SHA-256 text and rejects malformed asset identities. */
    @Test
    void validatesAssetSource() {
        Path source = Path.of("/project/assets/source.wad");
        Optional<String> uppercaseDigest = Optional.of("A".repeat(64));
        Optional<String> malformedDigest = Optional.of("invalid");
        Optional<String> noDigest = Optional.empty();

        GameProject.AssetSource asset =
                new GameProject.AssetSource("freedoom", "example.game/wad", source, uppercaseDigest);

        assertThat(asset.sha256()).contains("a".repeat(64));
        assertThatThrownBy(() -> new GameProject.AssetSource("Freedoom", "example.game/wad", source, noDigest))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameProject.AssetSource("freedoom", "wad", source, noDigest))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameProject.AssetSource("freedoom", "example.game/wad", source, malformedDigest))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
