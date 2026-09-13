/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.i18n;

import java.util.Locale;

/** Resolves and formats application messages independently of their storage format. */
public interface MessageSource {
    /**
     * Resolves a required message for the requested locale.
     *
     * @param code stable message identity
     * @param locale requested locale
     * @param arguments values used by indexed {@link java.text.MessageFormat} placeholders
     * @return resolved and formatted message
     * @throws NoSuchMessageException when no configured catalogue contains the message
     */
    String getMessage(String code, Locale locale, Object... arguments);

    /**
     * Resolves a message, formatting the supplied default when the code is absent.
     *
     * @param code stable message identity
     * @param defaultMessage fallback message pattern
     * @param locale requested locale
     * @param arguments values used by indexed {@link java.text.MessageFormat} placeholders
     * @return resolved message, or the formatted default message
     */
    String getMessage(String code, String defaultMessage, Locale locale, Object... arguments);
}
