/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.i18n.resourcebundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.NoSuchMessageException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies locale fallback, catalogue precedence, formatting, and failure behavior. */
final class ResourceBundleMessageSourceTest {
    private static final String PRIMARY = "io.github.glynch.jscene3d.i18n.TestMessages";
    private static final String SECONDARY = "io.github.glynch.jscene3d.i18n.SecondaryMessages";

    @Test
    void resolvesLocalizedMessagesAndFallsBackToTheRootBundle() {
        MessageSource messages = new ResourceBundleMessageSource(PRIMARY);

        assertThat(messages.getMessage("greeting", Locale.FRENCH, "Graham")).isEqualTo("Bonjour, Graham !");
        assertThat(messages.getMessage("amount", Locale.FRENCH, 12.5)).isEqualTo("Amount: 12,50");
        assertThat(messages.getMessage("plain", Locale.ENGLISH)).isEqualTo("Developer's tools");
    }

    @Test
    void searchesOrderedCataloguesAndUsesTheFirstMatchingMessage() {
        MessageSource messages = new ResourceBundleMessageSource(PRIMARY, SECONDARY);

        assertThat(messages.getMessage("precedence", Locale.ENGLISH)).isEqualTo("Primary");
        assertThat(messages.getMessage("secondary", Locale.ENGLISH)).isEqualTo("Secondary catalogue");
    }

    @Test
    void formatsTheDefaultMessageWhenNoCatalogueContainsTheCode() {
        MessageSource messages = new ResourceBundleMessageSource(PRIMARY);

        assertThat(messages.getMessage("missing", "Welcome, {0}!", Locale.ENGLISH, "developer"))
                .isEqualTo("Welcome, developer!");
    }

    @Test
    void reportsAnUnresolvedRequiredMessage() {
        MessageSource messages = new ResourceBundleMessageSource(PRIMARY);

        NoSuchMessageException exception = catchThrowableOfType(
                NoSuchMessageException.class, () -> messages.getMessage("missing", Locale.CANADA_FRENCH));

        assertThat(exception.code()).isEqualTo("missing");
        assertThat(exception.locale()).isEqualTo(Locale.CANADA_FRENCH);
        assertThat(exception).hasMessageContaining("missing", "fr_CA");
    }

    @Test
    void validatesCatalogueAndMessageInputs() {
        assertThatThrownBy(ResourceBundleMessageSource::new)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
        assertThatThrownBy(() -> new ResourceBundleMessageSource(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> new ResourceBundleMessageSource((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bundleBaseName");

        MessageSource messages = new ResourceBundleMessageSource(PRIMARY);
        assertThatThrownBy(() -> messages.getMessage(" ", Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message code");
    }

    @Test
    void loadsAnExtensionOwnedCatalogueFromAnExplicitClassLoader(@TempDir Path directory) throws IOException {
        Path catalogue = directory.resolve("extension/Messages.properties");
        Files.createDirectories(catalogue.getParent());
        Files.writeString(catalogue, "extension.message=Extension message\n");

        URL[] locations = {directory.toUri().toURL()};
        try (URLClassLoader classLoader = new URLClassLoader(locations, null)) {
            MessageSource messages = new ResourceBundleMessageSource(classLoader, "extension.Messages");
            assertThat(messages.getMessage("extension.message", Locale.ENGLISH)).isEqualTo("Extension message");
        }

        assertThatThrownBy(() -> new ResourceBundleMessageSource((ClassLoader) null, PRIMARY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("classLoader");
    }

    @Test
    void loadsACatalogueOwnedByANamedModule() {
        MessageSource messages = new ResourceBundleMessageSource(getClass().getModule(), PRIMARY);

        assertThat(messages.getMessage("greeting", Locale.FRENCH, "Graham")).isEqualTo("Bonjour, Graham !");
        assertThatThrownBy(() -> new ResourceBundleMessageSource((Module) null, PRIMARY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ownerModule");
    }
}
