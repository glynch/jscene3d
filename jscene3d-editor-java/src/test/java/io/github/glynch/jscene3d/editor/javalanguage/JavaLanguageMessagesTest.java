/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Verifies the Java extension's localized status catalogue. */
final class JavaLanguageMessagesTest {
    private static final String MESSAGES = "io.github.glynch.jscene3d.editor.javalanguage.messages";

    @Test
    void fallsBackToTheRootCatalogueForEnglishInThailand() {
        MessageSource messages = new ResourceBundleMessageSource(getClass().getModule(), MESSAGES);

        assertThat(messages.getMessage("java.status.ready", Locale.forLanguageTag("en-TH")))
                .isEqualTo("Java: Ready");
    }
}
