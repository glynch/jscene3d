/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;

/**
 * Client protocol range offered during initialization.
 *
 * @param protocolVersion newest protocol version supported by the client
 * @param clientLanguage Code OSS UI language as a BCP 47 language tag
 */
public record InitializeParams(ProtocolVersion protocolVersion, String clientLanguage) {
    /** Validates initialization parameters. */
    public InitializeParams {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        Objects.requireNonNull(clientLanguage, "clientLanguage");
        if (clientLanguage.isBlank() || !clientLanguage.equals(clientLanguage.trim())) {
            throw new IllegalArgumentException("clientLanguage must be a non-blank BCP 47 language tag");
        }
        clientLocale(clientLanguage);
    }

    /**
     * Returns the validated client display locale.
     *
     * @return requested client display locale
     */
    public Locale clientLocale() {
        return clientLocale(clientLanguage);
    }

    private static Locale clientLocale(String languageTag) {
        try {
            Locale locale = new Locale.Builder().setLanguageTag(languageTag).build();
            if (locale.getLanguage().isBlank() || "und".equals(locale.toLanguageTag())) {
                throw new IllegalArgumentException("clientLanguage must identify a language");
            }
            return locale;
        } catch (IllformedLocaleException exception) {
            throw new IllegalArgumentException("clientLanguage must be a valid BCP 47 language tag", exception);
        }
    }
}
