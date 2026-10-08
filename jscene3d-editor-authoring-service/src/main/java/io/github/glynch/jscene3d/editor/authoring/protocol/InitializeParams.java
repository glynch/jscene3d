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
 * @param protocolVersion exact internal protocol version expected by the client
 * @param contractIdentity stable internal development contract identity
 * @param buildIdentity source-derived development build identity
 * @param clientLanguage Code OSS UI language as a BCP 47 language tag
 */
public record InitializeParams(
        ProtocolVersion protocolVersion, String contractIdentity, String buildIdentity, String clientLanguage) {
    /** Validates initialization parameters. */
    public InitializeParams {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        contractIdentity = requireIdentity(contractIdentity, "contractIdentity");
        buildIdentity = requireIdentity(buildIdentity, "buildIdentity");
        Objects.requireNonNull(clientLanguage, "clientLanguage");
        if (clientLanguage.isBlank() || !clientLanguage.equals(clientLanguage.trim())) {
            throw new IllegalArgumentException("clientLanguage must be a non-blank BCP 47 language tag");
        }
        clientLocale(clientLanguage);
    }

    private static String requireIdentity(String value, String name) {
        String identity = Objects.requireNonNull(value, name).trim();
        if (identity.isEmpty() || !identity.equals(value)) {
            throw new IllegalArgumentException(name + " must be non-blank without surrounding whitespace");
        }
        return identity;
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
