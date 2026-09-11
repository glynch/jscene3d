/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.style;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Protects the single Java-side vocabulary for editor CSS classes. */
final class EditorStyleClassesTest {
    private static final Pattern STYLE_CLASS_LITERAL = Pattern.compile("\"(?:editor|diagnostic)-[a-z0-9-]+\"");

    /** Keeps every catalog entry public, immutable, uniquely named, and mechanically discoverable. */
    @Test
    void exposesConsistentlyNamedConstants() throws IllegalAccessException {
        Map<String, String> constants = new LinkedHashMap<>();
        for (Field field : EditorStyleClasses.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            assertThat(isPublic(modifiers) && isStatic(modifiers) && isFinal(modifiers))
                    .isTrue();
            String value = (String) field.get(null);
            assertThat(field.getName()).isEqualTo(value.toUpperCase(Locale.ROOT).replace('-', '_'));
            constants.put(field.getName(), value);
        }

        assertThat(constants.values()).doesNotHaveDuplicates();
    }

    /** Prevents JavaFX construction code from bypassing the central style-class catalog. */
    @Test
    void productionCodeContainsNoStyleClassLiteralsOutsideCatalog() throws IOException {
        Path sourceRoot = sourceRoot();
        Path catalog = sourceRoot.resolve("io/github/glynch/jscene3d/editor/workbench/style/EditorStyleClasses.java");
        Map<Path, List<String>> violations = new LinkedHashMap<>();

        try (Stream<Path> sources = Files.walk(sourceRoot)) {
            sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.equals(catalog))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations).isEmpty();
    }

    private static Path sourceRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path reactorSourceRoot = workingDirectory.resolve("jscene3d-editor/src/main/java");
        return Files.isDirectory(reactorSourceRoot) ? reactorSourceRoot : workingDirectory.resolve("src/main/java");
    }

    private static void collectViolations(Path source, Map<Path, List<String>> violations) {
        try {
            List<String> matches = Files.readAllLines(source, UTF_8).stream()
                    .filter(line -> STYLE_CLASS_LITERAL.matcher(line).find())
                    .toList();
            if (!matches.isEmpty()) {
                violations.put(source, matches);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect " + source, exception);
        }
    }
}
