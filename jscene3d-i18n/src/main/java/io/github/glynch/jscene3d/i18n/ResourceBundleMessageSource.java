/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.i18n;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/** Resolves messages from one or more ordered Java resource-bundle families. */
public final class ResourceBundleMessageSource implements MessageSource {
    private final List<String> bundleBaseNames;
    private final ClassLoader classLoader;

    /**
     * Creates a message source using the current thread context class loader.
     *
     * @param bundleBaseNames ordered bundle family names; earlier families take precedence
     */
    public ResourceBundleMessageSource(String... bundleBaseNames) {
        this(defaultClassLoader(), bundleBaseNames);
    }

    /**
     * Creates a message source using an explicit class loader.
     *
     * @param classLoader class loader containing the bundle resources
     * @param bundleBaseNames ordered bundle family names; earlier families take precedence
     */
    public ResourceBundleMessageSource(ClassLoader classLoader, String... bundleBaseNames) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.bundleBaseNames = validatedBaseNames(bundleBaseNames);
    }

    @Override
    public String getMessage(String code, Locale locale, Object... arguments) {
        String validCode = requireCode(code);
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        Object[] validArguments = Objects.requireNonNull(arguments, "arguments");
        return findMessage(validCode, validLocale)
                .map(pattern -> format(pattern, validLocale, validArguments))
                .orElseThrow(() -> new NoSuchMessageException(validCode, validLocale));
    }

    @Override
    public String getMessage(String code, String defaultMessage, Locale locale, Object... arguments) {
        String validCode = requireCode(code);
        String fallback = Objects.requireNonNull(defaultMessage, "defaultMessage");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        Object[] validArguments = Objects.requireNonNull(arguments, "arguments");
        String pattern = findMessage(validCode, validLocale).orElse(fallback);
        return format(pattern, validLocale, validArguments);
    }

    private Optional<String> findMessage(String code, Locale locale) {
        for (String baseName : bundleBaseNames) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, classLoader);
                if (bundle.containsKey(code)) {
                    return Optional.of(bundle.getString(code));
                }
            } catch (MissingResourceException ignored) {
                // Continue to the next configured catalogue.
            }
        }
        return Optional.empty();
    }

    private static String format(String pattern, Locale locale, Object[] arguments) {
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, locale).format(arguments);
    }

    private static String requireCode(String code) {
        String validCode = Objects.requireNonNull(code, "code");
        if (validCode.isBlank()) {
            throw new IllegalArgumentException("message code must not be blank");
        }
        return validCode;
    }

    private static List<String> validatedBaseNames(String[] baseNames) {
        Objects.requireNonNull(baseNames, "bundleBaseNames");
        if (baseNames.length == 0) {
            throw new IllegalArgumentException("at least one bundle base name is required");
        }
        List<String> validated = new ArrayList<>(baseNames.length);
        for (String baseName : baseNames) {
            String value = Objects.requireNonNull(baseName, "bundleBaseName");
            if (value.isBlank()) {
                throw new IllegalArgumentException("bundle base name must not be blank");
            }
            validated.add(value);
        }
        return List.copyOf(validated);
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader == null ? ResourceBundleMessageSource.class.getClassLoader() : contextClassLoader;
    }
}
