/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/**
 * Client protocol range offered during initialization.
 *
 * @param protocolVersion newest protocol version supported by the client
 */
public record InitializeParams(ProtocolVersion protocolVersion) {
    /** Validates initialization parameters. */
    public InitializeParams {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
    }
}
