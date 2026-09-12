/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable diagnostics produced while loading portable project settings. */
public enum ProjectSettingsDiagnosticCode implements DiagnosticCode {
    /** The settings document is not valid JSON or does not satisfy the version-one contract. */
    SETTINGS_INVALID("project.settings.invalid", "Project settings are invalid"),
    /** The settings document could not be read. */
    SETTINGS_READ_FAILED("project.settings.read", "Project settings could not be read");

    private final String code;
    private final String message;

    ProjectSettingsDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
