/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol.framing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Exercises byte-accurate framed transport independently of JSON dispatch. */
final class ContentLengthFramingTest {
    /** Reads one complete framed message. */
    @Test
    void readsCompleteMessage() throws IOException {
        ContentLengthMessageReader reader = reader("Content-Length: 2\r\n\r\n{}");

        assertThat(reader.readMessage()).contains("{}");
        assertThat(reader.readMessage()).isEmpty();
    }

    /** Reads headers delivered one byte at a time. */
    @Test
    void readsPartialHeaders() throws IOException {
        ContentLengthMessageReader reader =
                new ContentLengthMessageReader(new ChunkedInputStream(frame("{\"value\":1}"), 1));

        assertThat(reader.readMessage()).contains("{\"value\":1}");
    }

    /** Reads a payload delivered in short bulk reads. */
    @Test
    void readsPartialPayload() throws IOException {
        ContentLengthMessageReader reader =
                new ContentLengthMessageReader(new ChunkedInputStream(frame("{\"payload\":true}"), 2));

        assertThat(reader.readMessage()).contains("{\"payload\":true}");
    }

    /** Leaves the stream positioned for each consecutive message. */
    @Test
    void readsMultipleMessages() throws IOException {
        byte[] first = frame("{\"id\":1}");
        byte[] second = frame("{\"id\":2}");
        byte[] combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        ContentLengthMessageReader reader = new ContentLengthMessageReader(new ByteArrayInputStream(combined));

        assertThat(reader.readMessage()).contains("{\"id\":1}");
        assertThat(reader.readMessage()).contains("{\"id\":2}");
        assertThat(reader.readMessage()).isEmpty();
    }

    /** Computes Content-Length from UTF-8 bytes rather than Java characters. */
    @Test
    void writesUtf8ByteLength() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ContentLengthMessageWriter(output).writeMessage("{\"name\":\"ห้อง\"}");
        String frame = output.toString(StandardCharsets.UTF_8);

        assertThat(frame).startsWith("Content-Length: 23\r\n\r\n");
        assertThat(new ContentLengthMessageReader(new ByteArrayInputStream(output.toByteArray())).readMessage())
                .contains("{\"name\":\"ห้อง\"}");
    }

    /** Rejects absent, non-numeric, duplicate, negative, and oversized lengths. */
    @Test
    void rejectsMalformedContentLength() {
        ContentLengthMessageReader missing = reader("X-Test: 1\r\n\r\n");
        ContentLengthMessageReader malformed = reader("Content-Length: nope\r\n\r\n");
        ContentLengthMessageReader duplicate = reader("Content-Length: 1\r\nContent-Length: 1\r\n\r\na");
        ContentLengthMessageReader negative = reader("Content-Length: -1\r\n\r\n");
        ContentLengthMessageReader oversized = reader("Content-Length: 9000000\r\n\r\n");

        assertThatThrownBy(missing::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("Missing");
        assertThatThrownBy(malformed::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("Malformed");
        assertThatThrownBy(duplicate::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(negative::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("range");
        assertThatThrownBy(oversized::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("range");
    }

    /** Distinguishes clean boundary EOF from truncated headers and payloads. */
    @Test
    void rejectsUnexpectedEof() {
        ContentLengthMessageReader truncatedHeaders = reader("Content-Length: 2\r\n");
        ContentLengthMessageReader truncatedPayload = reader("Content-Length: 2\r\n\r\n{");

        assertThatThrownBy(truncatedHeaders::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("headers");
        assertThatThrownBy(truncatedPayload::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("payload");
    }

    /** Rejects payload bytes which are not a valid UTF-8 sequence. */
    @Test
    void rejectsInvalidUtf8() {
        byte[] frame = "Content-Length: 2\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        byte[] invalid = new byte[frame.length + 2];
        System.arraycopy(frame, 0, invalid, 0, frame.length);
        invalid[frame.length] = (byte) 0xC3;
        invalid[frame.length + 1] = 0x28;
        ContentLengthMessageReader reader = new ContentLengthMessageReader(new ByteArrayInputStream(invalid));

        assertThatThrownBy(reader::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("UTF-8");
    }

    /** Rejects malformed header lines and headers above the transport limit. */
    @Test
    void rejectsMalformedAndOversizedHeaders() {
        ContentLengthMessageReader malformed = reader("Content-Length 2\r\n\r\n{}");
        ContentLengthMessageReader oversized = reader("X".repeat(8 * 1024 + 1));

        assertThatThrownBy(malformed::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("Malformed");
        assertThatThrownBy(oversized::readMessage)
                .isInstanceOf(ProtocolFramingException.class)
                .hasMessageContaining("exceed");
    }

    /** Continues reading when a stream temporarily reports no payload progress. */
    @Test
    void continuesAfterZeroLengthPayloadRead() throws IOException {
        ContentLengthMessageReader reader =
                new ContentLengthMessageReader(new ZeroThenDelegateInputStream(frame("{}")));

        assertThat(reader.readMessage()).contains("{}");
    }

    /** Creates a reader from UTF-8 test input. */
    private static ContentLengthMessageReader reader(String input) {
        return new ContentLengthMessageReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    /** Frames one UTF-8 message for input-side tests. */
    private static byte[] frame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] header = ("Content-Length: " + payload.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] frame = new byte[header.length + payload.length];
        System.arraycopy(header, 0, frame, 0, header.length);
        System.arraycopy(payload, 0, frame, header.length, payload.length);
        return frame;
    }

    /** Input stream which limits every bulk read to a deterministic chunk size. */
    private static final class ChunkedInputStream extends InputStream {
        private final ByteArrayInputStream delegate;
        private final int chunkSize;

        private ChunkedInputStream(byte[] content, int chunkSize) {
            delegate = new ByteArrayInputStream(content);
            this.chunkSize = chunkSize;
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            return delegate.read(bytes, offset, Math.min(length, chunkSize));
        }
    }

    /** Input stream which reports one zero-length bulk read before delivering payload bytes. */
    private static final class ZeroThenDelegateInputStream extends InputStream {
        private final ByteArrayInputStream delegate;
        private boolean returnedZero;

        private ZeroThenDelegateInputStream(byte[] content) {
            delegate = new ByteArrayInputStream(content);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (!returnedZero) {
                returnedZero = true;
                return 0;
            }
            return delegate.read(bytes, offset, length);
        }
    }
}
