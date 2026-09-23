/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.framing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Writes UTF-8 protocol messages with byte-accurate {@code Content-Length} framing. */
public final class ContentLengthMessageWriter {
    private final OutputStream output;

    /**
     * Creates a writer over a caller-owned byte stream.
     *
     * @param output protocol output
     */
    public ContentLengthMessageWriter(OutputStream output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Writes and flushes one complete frame.
     *
     * @param message JSON message text
     * @throws IOException when the frame cannot be written
     */
    public synchronized void writeMessage(String message) throws IOException {
        byte[] payload = Objects.requireNonNull(message, "message").getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + payload.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        output.write(header);
        output.write(payload);
        output.flush();
    }
}
