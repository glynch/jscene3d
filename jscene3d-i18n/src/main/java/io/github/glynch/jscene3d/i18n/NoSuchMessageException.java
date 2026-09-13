/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.i18n;

import java.util.Locale;
import java.util.Objects;

/** Indicates that no configured message catalogue contains a required message. */
public final class NoSuchMessageException extends RuntimeException {
    /** Stable identity that could not be resolved. */
    private final String code;

    /** Locale used for the failed lookup. */
    private final Locale locale;

    /**
     * Creates an exception identifying the unresolved message and requested locale.
     *
     * @param code unresolved message identity
     * @param locale requested locale
     */
    public NoSuchMessageException(String code, Locale locale) {
        super("No message found for code '" + code + "' and locale '" + locale + "'");
        this.code = Objects.requireNonNull(code, "code");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    /**
     * Returns the unresolved message identity.
     *
     * @return unresolved message identity
     */
    public String code() {
        return code;
    }

    /**
     * Returns the requested locale.
     *
     * @return requested locale
     */
    public Locale locale() {
        return locale;
    }
}
