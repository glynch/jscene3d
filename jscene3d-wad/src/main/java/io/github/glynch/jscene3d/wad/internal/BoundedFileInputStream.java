/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.internal;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Caller-owned file stream that cannot read beyond one previously validated source range. */
public final class BoundedFileInputStream extends InputStream {
    private final FileChannel channel;
    private final ByteBuffer singleByte = ByteBuffer.allocate(1);
    private long remaining;
    private boolean closed;

    /** Stores one positioned channel and its remaining readable byte count. */
    private BoundedFileInputStream(FileChannel channel, long remaining) {
        this.channel = channel;
        this.remaining = remaining;
    }

    /**
     * Opens a source only when its current size matches the validated provenance.
     *
     * @param source source file to open
     * @param offset first readable byte
     * @param size number of readable bytes
     * @param expectedFileSize source size recorded during validation
     * @return caller-owned stream bounded to the requested range
     * @throws IOException when the source cannot be opened or its size changed
     */
    public static InputStream openStream(Path source, long offset, long size, long expectedFileSize)
            throws IOException {
        FileChannel channel = FileChannel.open(source, StandardOpenOption.READ);
        try {
            if (channel.size() != expectedFileSize) {
                throw new EOFException("WAD source size changed after its directory was validated");
            }
            channel.position(offset);
            return new BoundedFileInputStream(channel, size);
        } catch (IOException | RuntimeException exception) {
            closeAfterFailure(channel, exception);
            throw exception;
        }
    }

    @Override
    public int read() throws IOException {
        requireOpen();
        if (remaining == 0L) {
            return -1;
        }
        singleByte.clear();
        int count = readChannel(singleByte);
        remaining -= count;
        return Byte.toUnsignedInt(singleByte.array()[0]);
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, target.length);
        requireOpen();
        if (length == 0) {
            return 0;
        }
        if (remaining == 0L) {
            return -1;
        }
        int boundedLength = (int) Math.min(length, remaining);
        int count = readChannel(ByteBuffer.wrap(target, offset, boundedLength));
        remaining -= count;
        return count;
    }

    @Override
    public long skip(long count) throws IOException {
        requireOpen();
        if (count <= 0L) {
            return 0L;
        }
        long skipped = Math.min(count, remaining);
        channel.position(channel.position() + skipped);
        remaining -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        requireOpen();
        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        remaining = 0L;
        channel.close();
    }

    /** Reads from a blocking regular-file channel and treats premature EOF as source mutation. */
    private int readChannel(ByteBuffer target) throws IOException {
        int count;
        do {
            count = channel.read(target);
        } while (count == 0);
        if (count < 0) {
            throw new EOFException("WAD source ended inside a validated lump");
        }
        return count;
    }

    /** Rejects operations after ownership of the channel has ended. */
    private void requireOpen() throws IOException {
        if (closed) {
            throw new IOException("WAD lump stream is closed");
        }
    }

    /** Closes a partially initialized channel while preserving the primary failure. */
    private static void closeAfterFailure(FileChannel channel, Exception failure) {
        try {
            channel.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
