/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.client;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Toolkit- and protocol-neutral information required to initialize one language-server workspace.
 *
 * @param projectRoot absolute or relative project root, normalized during construction
 * @param projectName display name reported for the workspace folder
 * @param clientName editor client name reported to the language server
 * @param clientVersion editor client version reported to the language server
 */
public record LanguageServerInitialization(
        Path projectRoot, String projectName, String clientName, String clientVersion) {
    /** Validates and normalizes one initialization request. */
    public LanguageServerInitialization {
        projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        projectName = requireText(projectName, "projectName");
        clientName = requireText(clientName, "clientName");
        clientVersion = requireText(clientVersion, "clientVersion");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).strip();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
