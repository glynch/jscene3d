/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.javalanguage.jdt.JdtLanguageSupportFactory;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportContribution;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupportId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Built-in extension contributing project-scoped Java support backed by Eclipse JDT LS. */
public final class JavaLanguageExtension implements EditorExtension {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.editor.builtin.java";
    private static final EditorLanguageSupportId SUPPORT_ID = new EditorLanguageSupportId(EXTENSION_ID + ".jdtls");

    private final String serverVersion;
    private final EditorLanguageSupport support;

    /** Creates Java support from the checksum-verified distribution staged by the editor build. */
    public JavaLanguageExtension() {
        this(JdtLanguageSupportFactory.create());
    }

    private JavaLanguageExtension(JdtLanguageSupportFactory.Result result) {
        JdtLanguageSupportFactory.Result resolved = Objects.requireNonNull(result, "result");
        this.serverVersion = resolved.serverVersion();
        this.support = resolved.support();
    }

    @Override
    public String id() {
        return EXTENSION_ID;
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Java Language Support",
                "Provides Java project intelligence using Eclipse JDT Language Server.",
                "JScene3D",
                Optional.of(serverVersion),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.languageSupports()
                        .register(
                                new EditorLanguageSupportContribution(SUPPORT_ID, Set.of(EditorLanguages.JAVA)),
                                support));
    }
}
