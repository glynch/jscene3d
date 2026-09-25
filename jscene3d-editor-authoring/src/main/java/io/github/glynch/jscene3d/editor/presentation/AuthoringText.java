/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.presentation;

import io.github.glynch.jscene3d.i18n.MessageSource;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Authoring presentation text that preserves whether content is authored or Java-owned. */
public sealed interface AuthoringText permits AuthoringText.Literal, AuthoringText.Message {
    /**
     * Resolves this value for one Java presentation boundary.
     *
     * @param messages feature-owned message source
     * @param locale requested display locale
     * @return authored text unchanged or a localized Java-owned message
     */
    String resolve(MessageSource messages, Locale locale);

    /**
     * Creates text supplied by project or extension data.
     *
     * @param value authored display text
     * @return literal authoring text
     */
    static AuthoringText literal(String value) {
        return new Literal(value);
    }

    /**
     * Creates Java-owned semantic presentation text.
     *
     * @param code stable feature-owned message key
     * @param defaultMessage complete English fallback pattern
     * @param arguments ordered formatting arguments
     * @return localizable authoring text
     */
    static AuthoringText message(String code, String defaultMessage, Object... arguments) {
        return new Message(code, defaultMessage, List.of(arguments));
    }

    /**
     * Project- or extension-authored text that must be preserved unchanged.
     *
     * @param value authored display text
     */
    record Literal(String value) implements AuthoringText {
        /** Validates authored text. */
        public Literal {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String resolve(MessageSource messages, Locale locale) {
            Objects.requireNonNull(messages, "messages");
            Objects.requireNonNull(locale, "locale");
            return value;
        }
    }

    /**
     * Java-owned semantic text resolved from a feature-owned message catalogue.
     *
     * @param code stable feature-owned message key
     * @param defaultMessage complete English fallback pattern
     * @param arguments immutable ordered formatting arguments
     */
    record Message(String code, String defaultMessage, List<Object> arguments) implements AuthoringText {
        /** Validates and copies message data. */
        public Message {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(defaultMessage, "defaultMessage");
            arguments = List.copyOf(arguments);
            if (code.isBlank()) {
                throw new IllegalArgumentException("code must not be blank");
            }
            if (defaultMessage.isBlank()) {
                throw new IllegalArgumentException("defaultMessage must not be blank");
            }
        }

        @Override
        public String resolve(MessageSource messages, Locale locale) {
            return Objects.requireNonNull(messages, "messages")
                    .getMessage(code, defaultMessage, Objects.requireNonNull(locale, "locale"), arguments.toArray());
        }
    }
}
