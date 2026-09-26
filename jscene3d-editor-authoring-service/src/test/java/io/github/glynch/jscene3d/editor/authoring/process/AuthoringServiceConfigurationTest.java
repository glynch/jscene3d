/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises authoring-service command-line configuration parsing. */
final class AuthoringServiceConfigurationTest {
    /** Preserves configured metadata order while normalizing every path. */
    @Test
    void parsesInstalledExtensionMetadataInArgumentOrder() {
        Path first = Path.of("extensions", "..", "extensions", "first.jar");
        Path second = Path.of("extensions", "second.jar");

        AuthoringServiceConfiguration configuration = AuthoringServiceConfiguration.from(
                new String[] {"--extension-metadata=" + first, "--extension-metadata=" + second});
        List<Path> configuredMetadata = configuration.installedExtensionMetadata();
        Path third = Path.of("third.jar");

        assertThat(configuredMetadata)
                .containsExactly(
                        first.toAbsolutePath().normalize(),
                        second.toAbsolutePath().normalize());
        assertThatThrownBy(() -> configuredMetadata.add(third)).isInstanceOf(UnsupportedOperationException.class);
    }

    /** Accepts the supported empty configuration. */
    @Test
    void acceptsNoInstalledExtensionMetadata() {
        AuthoringServiceConfiguration configuration = AuthoringServiceConfiguration.from(new String[0]);

        assertThat(configuration.installedExtensionMetadata()).isEmpty();
    }

    /** Rejects null, unsupported, or incomplete process arguments. */
    @Test
    void rejectsInvalidArguments() {
        String[] nullArgument = {null};
        String[] unsupportedArgument = {"--unsupported=value"};
        String[] blankMetadataPath = {"--extension-metadata= \t"};

        assertThatThrownBy(() -> AuthoringServiceConfiguration.from(nullArgument))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("argument");
        assertThatThrownBy(() -> AuthoringServiceConfiguration.from(unsupportedArgument))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported authoring service argument");
        assertThatThrownBy(() -> AuthoringServiceConfiguration.from(blankMetadataPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }
}
