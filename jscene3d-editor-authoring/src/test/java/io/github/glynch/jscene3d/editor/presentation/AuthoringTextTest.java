/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Verifies authored and Java-owned authoring text semantics. */
final class AuthoringTextTest {
    private static final String MESSAGES = "io.github.glynch.jscene3d.editor.presentation.messages";
    private final MessageSource messages =
            new ResourceBundleMessageSource(getClass().getModule(), MESSAGES);

    /** Preserves project-authored text independently of the requested locale. */
    @Test
    void preservesAuthoredLiteral() {
        AuthoringText text = AuthoringText.literal("Player");

        assertThat(text.resolve(messages, Locale.FRENCH)).isEqualTo("Player");
    }

    /** Resolves Java-owned text and its ordered arguments through the feature bundle. */
    @Test
    void resolvesJavaOwnedMessage() {
        AuthoringText text = AuthoringText.message(
                "editor.inspector.descriptor-unavailable",
                "Descriptor metadata is unavailable for type version {0,number,integer}",
                12);

        assertThat(text.resolve(messages, Locale.FRENCH))
                .isEqualTo("Les métadonnées du descripteur sont indisponibles pour la version de type 12");
    }
}
