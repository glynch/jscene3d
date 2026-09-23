/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.framing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Reads UTF-8 protocol messages framed by ASCII {@code Content-Length} headers. */
public final class ContentLengthMessageReader {
    private static final int MAX_HEADER_BYTES = 8 * 1024;
    private static final int MAX_PAYLOAD_BYTES = 8 * 1024 * 1024;

    private final InputStream input;

    /**
     * Creates a reader over a caller-owned byte stream.
     *
     * @param input framed protocol input
     */
    public ContentLengthMessageReader(InputStream input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Reads one complete message, returning empty only for clean boundary EOF.
     *
     * @return decoded message or empty when the stream ended between messages
     * @throws IOException when the stream cannot be read
     * @throws ProtocolFramingException when headers, UTF-8, or payload termination are invalid
     */
    public Optional<String> readMessage() throws IOException {
        Optional<byte[]> header = readHeader();
        if (header.isEmpty()) {
            return Optional.empty();
        }
        int length = contentLength(header.orElseThrow());
        byte[] payload = readPayload(length);
        try {
            return Optional.of(StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString());
        } catch (CharacterCodingException exception) {
            throw new ProtocolFramingException("Protocol payload is not valid UTF-8");
        }
    }

    /** Reads bytes through the terminating empty header line. */
    private Optional<byte[]> readHeader() throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        int matched = 0;
        int[] terminator = {'\r', '\n', '\r', '\n'};
        while (true) {
            int next = input.read();
            if (next < 0) {
                if (header.size() == 0) {
                    return Optional.empty();
                }
                throw new ProtocolFramingException("Unexpected EOF in protocol headers");
            }
            header.write(next);
            if (header.size() > MAX_HEADER_BYTES) {
                throw new ProtocolFramingException("Protocol headers exceed " + MAX_HEADER_BYTES + " bytes");
            }
            if (next == terminator[matched]) {
                matched++;
            } else if (next == terminator[0]) {
                matched = 1;
            } else {
                matched = 0;
            }
            if (matched == terminator.length) {
                return Optional.of(header.toByteArray());
            }
        }
    }

    /** Resolves the one required payload length. */
    private static int contentLength(byte[] headerBytes) throws ProtocolFramingException {
        String header = new String(headerBytes, StandardCharsets.ISO_8859_1);
        String value = null;
        for (String line : header.substring(0, header.length() - 4).split("\\r\\n")) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new ProtocolFramingException("Malformed protocol header");
            }
            String name = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!name.equals("content-length")) {
                continue;
            }
            if (value != null) {
                throw new ProtocolFramingException("Duplicate Content-Length header");
            }
            value = line.substring(separator + 1).trim();
        }
        if (value == null || value.isEmpty()) {
            throw new ProtocolFramingException("Missing Content-Length header");
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0 || parsed > MAX_PAYLOAD_BYTES) {
                throw new ProtocolFramingException("Content-Length is outside the supported range");
            }
            return (int) parsed;
        } catch (NumberFormatException exception) {
            throw new ProtocolFramingException("Malformed Content-Length header");
        }
    }

    /** Reads an exact payload without assuming one stream read is sufficient. */
    private byte[] readPayload(int length) throws IOException {
        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(payload, offset, length - offset);
            if (read < 0) {
                throw new ProtocolFramingException("Unexpected EOF in protocol payload");
            }
            if (read == 0) {
                continue;
            }
            offset += read;
        }
        return payload;
    }
}
