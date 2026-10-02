/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Closed Java representation of the authoring service's JSON-RPC method names. */
enum AuthoringProtocolMethod {
    /** Negotiates the connection protocol and client presentation locale. */
    INITIALIZE("initialize"),
    /** Opens a project when no project is currently retained. */
    PROJECT_OPEN("project/open"),
    /** Replaces the retained project using optimistic generation matching. */
    PROJECT_REPLACE("project/replace"),
    /** Closes the retained project without terminating the service. */
    PROJECT_CLOSE("project/close"),
    /** Prepares an isolated renderer launch for one authoritative project world. */
    VIEWPORT_PREPARE_LAUNCH("viewport/prepareLaunch"),
    /** Resolves and retains a structural definition with its complete hierarchy snapshot. */
    DEFINITION_OPEN("definition/open"),
    /** Applies one exact SET or REMOVE mutation. */
    DEFINITION_MUTATE("definition/mutate"),
    /** Restores the preceding accepted authored state. */
    DEFINITION_UNDO("definition/undo"),
    /** Reapplies the next previously undone authored state. */
    DEFINITION_REDO("definition/redo"),
    /** Persists current authored state through the B2 lifecycle. */
    DEFINITION_SAVE("definition/save"),
    /** Reloads authoritative authored state through the B2 lifecycle. */
    DEFINITION_REVERT("definition/revert"),
    /** Captures deterministic B3 recovery state. */
    DEFINITION_BACKUP("definition/backup"),
    /** Restores deterministic B3 recovery state. */
    DEFINITION_RESTORE_BACKUP("definition/restoreBackup"),
    /** Reads one complete generation- and revision-checked Inspector snapshot. */
    INSPECTOR_READ("inspector/read"),
    /** Performs orderly service shutdown. */
    SERVICE_SHUTDOWN("service/shutdown");

    private static final List<String> CAPABILITIES = Arrays.stream(values())
            .filter(method -> method != INITIALIZE)
            .map(AuthoringProtocolMethod::wireName)
            .toList();

    private final String wireName;

    /** Stores the stable JSON-RPC wire name. */
    AuthoringProtocolMethod(String wireName) {
        this.wireName = wireName;
    }

    /** Returns the stable JSON-RPC wire name. */
    String wireName() {
        return wireName;
    }

    /** Returns whether successful initialization must precede this method. */
    boolean requiresInitialization() {
        return this != INITIALIZE && this != SERVICE_SHUTDOWN;
    }

    /** Resolves a wire name without converting unknown methods into parameter errors. */
    static Optional<AuthoringProtocolMethod> fromWireName(String wireName) {
        String validWireName = Objects.requireNonNull(wireName, "wireName");
        return Arrays.stream(values())
                .filter(method -> method.wireName.equals(validWireName))
                .findFirst();
    }

    /** Returns implemented methods advertised after successful initialization. */
    static List<String> capabilities() {
        return CAPABILITIES;
    }
}
