/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.editor.diagnostics.EditorDiagnosticCode;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import io.github.glynch.jscene3d.project.asset.AssetDiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionDiagnosticCode;
import io.github.glynch.jscene3d.project.importing.ImportDiagnosticCode;
import io.github.glynch.jscene3d.project.imports.ImportDefinitionDiagnosticCode;
import io.github.glynch.jscene3d.project.input.InputMapDiagnosticCode;
import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import io.github.glynch.jscene3d.project.resource.ResourceDiagnosticCode;
import io.github.glynch.jscene3d.project.scene.SceneDiagnosticCode;
import io.github.glynch.jscene3d.project.settings.ProjectSettingsDiagnosticCode;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Resolves feature-owned project diagnostics at the authoring presentation boundary. */
final class ProjectDiagnosticMessageResolver {
    private static final Map<Class<?>, MessageSource> SOURCES = Map.ofEntries(
            source(AssetDiagnosticCode.class, "io.github.glynch.jscene3d.project.asset.messages"),
            source(ExtensionDiagnosticCode.class, "io.github.glynch.jscene3d.project.extension.messages"),
            source(ImportDefinitionDiagnosticCode.class, "io.github.glynch.jscene3d.project.imports.messages"),
            source(InputMapDiagnosticCode.class, "io.github.glynch.jscene3d.project.input.messages"),
            source(ProjectDiagnosticCode.class, "io.github.glynch.jscene3d.project.manifest.messages"),
            source(ResourceDiagnosticCode.class, "io.github.glynch.jscene3d.project.resource.messages"),
            source(SceneDiagnosticCode.class, "io.github.glynch.jscene3d.project.scene.messages"),
            source(ProjectSettingsDiagnosticCode.class, "io.github.glynch.jscene3d.project.settings.messages"),
            source(ImportDiagnosticCode.class, "io.github.glynch.jscene3d.project.importing.messages"),
            source(EditorDiagnosticCode.class, "io.github.glynch.jscene3d.editor.diagnostics.messages"));

    /** Resolves one complete display message using its feature owner and ordered arguments. */
    String resolve(ProjectDiagnostic diagnostic, Locale locale) {
        ProjectDiagnostic validDiagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
        Locale validLocale = Objects.requireNonNull(locale, "locale");
        DiagnosticCode code = validDiagnostic.code();
        Object[] arguments = validDiagnostic.messageArguments().toArray();
        MessageSource source = SOURCES.get(code.getClass());
        if (source == null) {
            return arguments.length == 0
                    ? code.defaultMessage()
                    : new MessageFormat(code.defaultMessage(), validLocale).format(arguments);
        }
        return source.getMessage(code.code(), code.defaultMessage(), validLocale, arguments);
    }

    /** Creates one owner-module resource source for a diagnostic family. */
    private static Map.Entry<Class<?>, MessageSource> source(
            Class<? extends DiagnosticCode> owner, String bundleBaseName) {
        return Map.entry(owner, new ResourceBundleMessageSource(owner.getModule(), bundleBaseName));
    }
}
