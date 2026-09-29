/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the closed mapping between JSON-RPC method names and Java protocol methods. */
final class AuthoringProtocolMethodTest {
    /** Resolves every defined wire name without relying on Java enum constant names. */
    @Test
    void resolvesDefinedWireNames() {
        for (AuthoringProtocolMethod method : AuthoringProtocolMethod.values()) {
            assertThat(AuthoringProtocolMethod.fromWireName(method.wireName())).contains(method);
        }
    }

    /** Keeps unknown JSON-RPC methods outside the closed Java method set. */
    @Test
    void rejectsUnknownWireName() {
        assertThat(AuthoringProtocolMethod.fromWireName("project/unknown")).isEmpty();
    }

    /** Derives advertised capabilities from the method model while excluding initialization. */
    @Test
    void advertisesOperationalCapabilities() {
        assertThat(AuthoringProtocolMethod.capabilities())
                .containsExactly(
                        "project/open",
                        "project/replace",
                        "project/close",
                        "definition/open",
                        "definition/mutate",
                        "definition/undo",
                        "definition/redo",
                        "definition/save",
                        "definition/revert",
                        "definition/backup",
                        "definition/restoreBackup",
                        "inspector/read",
                        "service/shutdown");
    }
}
