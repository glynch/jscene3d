/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;

/**
 * Validated process identity, versions, and implemented capabilities.
 *
 * @param protocolVersion validated internal protocol version
 * @param contractIdentity stable internal development contract identity
 * @param buildIdentity source-derived development build identity
 * @param processKind process role
 * @param serviceVersion authoring-service implementation version
 * @param engineVersion JScene3D engine compatibility version
 * @param capabilities implemented protocol capabilities
 */
public record InitializeResult(
        ProtocolVersion protocolVersion,
        String contractIdentity,
        String buildIdentity,
        String processKind,
        String serviceVersion,
        String engineVersion,
        List<String> capabilities) {
    /** Validates and copies initialization results. */
    public InitializeResult {
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        Objects.requireNonNull(contractIdentity, "contractIdentity");
        Objects.requireNonNull(buildIdentity, "buildIdentity");
        Objects.requireNonNull(processKind, "processKind");
        Objects.requireNonNull(serviceVersion, "serviceVersion");
        Objects.requireNonNull(engineVersion, "engineVersion");
        capabilities = List.copyOf(capabilities);
    }
}
