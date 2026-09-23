/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.framing;

import java.io.IOException;

/** Reports malformed or truncated {@code Content-Length} transport input. */
public final class ProtocolFramingException extends IOException {
    /**
     * Creates a framing failure with safe process-level detail.
     *
     * @param message failure detail
     */
    public ProtocolFramingException(String message) {
        super(message);
    }
}
