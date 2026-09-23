/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;

/**
 * Negotiated process identity, versions, and implemented capabilities.
 *
 * @param protocolVersion negotiated protocol version
 * @param processKind process role
 * @param serviceVersion authoring-service implementation version
 * @param engineVersion JScene3D engine compatibility version
 * @param capabilities implemented protocol capabilities
 */
public record InitializeResult(
        ProtocolVersion protocolVersion,
        String processKind,
        String serviceVersion,
        String engineVersion,
        List<String> capabilities) {
    /** Validates and copies initialization results. */
    public InitializeResult {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        Objects.requireNonNull(processKind, "processKind");
        Objects.requireNonNull(serviceVersion, "serviceVersion");
        Objects.requireNonNull(engineVersion, "engineVersion");
        capabilities = List.copyOf(capabilities);
    }
}
