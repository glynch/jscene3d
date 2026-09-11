/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Exercises installed-extension metadata validation. */
final class EditorExtensionDescriptorTest {
    /** Normalizes an optional version while retaining descriptive metadata. */
    @Test
    void retainsValidMetadata() {
        EditorExtensionDescriptor descriptor = new EditorExtensionDescriptor(
                "test.extension", "Test", "A test extension.", "Tests", Optional.of(" 1.2.3 "), false);

        assertThat(descriptor.id()).isEqualTo("test.extension");
        assertThat(descriptor.displayName()).isEqualTo("Test");
        assertThat(descriptor.description()).isEqualTo("A test extension.");
        assertThat(descriptor.publisher()).isEqualTo("Tests");
        assertThat(descriptor.version()).contains("1.2.3");
        assertThat(descriptor.builtIn()).isFalse();
        assertThat(new EditorExtensionDescriptor(
                                "test.extension", "Test", "A test extension.", "Tests", Optional.of(" "), true)
                        .version())
                .isEmpty();
    }

    /** Supplies safe default metadata for extensions which only declare an identity. */
    @Test
    void suppliesDefaultExtensionMetadata() {
        EditorExtension extension = new EditorExtension() {
            @Override
            public String id() {
                return "test.extension";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                // No contributions are needed for the metadata contract.
            }
        };

        assertThat(extension.descriptor())
                .isEqualTo(new EditorExtensionDescriptor(
                        "test.extension",
                        "test.extension",
                        "Installed editor extension",
                        "Unknown publisher",
                        Optional.empty(),
                        false));
    }

    /** Rejects every blank required metadata field. */
    @Test
    void rejectsBlankRequiredMetadata() {
        assertInvalid(" ", "Test", "A test extension.", "Tests", "identity");
        assertInvalid("test.extension", " ", "A test extension.", "Tests", "display name");
        assertInvalid("test.extension", "Test", " ", "Tests", "description");
        assertInvalid("test.extension", "Test", "A test extension.", " ", "publisher");
    }

    private static void assertInvalid(
            String id, String displayName, String description, String publisher, String message) {
        Optional<String> version = Optional.empty();
        assertThatThrownBy(() -> new EditorExtensionDescriptor(id, displayName, description, publisher, version, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
    }
}
