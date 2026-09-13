/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorEventSource;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** UTF-8 file content and persistence isolated from its Monaco presentation. */
public final class EditorTextFileWorkingCopy implements EditorWorkingCopy {
    private static final String TYPE = "io.github.glynch.jscene3d.editor.working-copy.text-file";
    private static final int MAXIMUM_BYTES = 10 * 1024 * 1024;

    private final Path path;
    private final EditorWorkingCopyId id;
    private final EditorEventSource<EditorWorkingCopy> contentChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> dirtyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> saves = new EditorEventSource<>();
    private String savedContent;
    private String content;

    private EditorTextFileWorkingCopy(Path path, String content) {
        this.path = path;
        this.id = new EditorWorkingCopyId(path.toUri(), TYPE);
        this.savedContent = content;
        this.content = content;
    }

    /** Opens a bounded UTF-8 text file, rejecting binary and malformed input. */
    public static EditorTextFileWorkingCopy load(Path path) throws IOException {
        Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        long size = Files.size(source);
        if (size > MAXIMUM_BYTES) {
            throw new IOException("File is too large for the source editor (maximum 10 MiB)");
        }
        byte[] bytes = Files.readAllBytes(source);
        if (containsNull(bytes)) {
            throw new IOException("File appears to contain binary data");
        }
        return new EditorTextFileWorkingCopy(source, decode(bytes));
    }

    /** Returns the normalized source path. */
    public Path path() {
        return path;
    }

    /** Returns the current in-memory text. */
    public String content() {
        return content;
    }

    /** Replaces the current content after an editor change. */
    public void update(String replacement) {
        String updated = Objects.requireNonNull(replacement, "replacement");
        if (content.equals(updated)) {
            return;
        }
        boolean wasDirty = isDirty();
        content = updated;
        contentChanges.emit(this);
        if (wasDirty != isDirty()) {
            dirtyChanges.emit(this);
        }
    }

    @Override
    public EditorWorkingCopyId id() {
        return id;
    }

    @Override
    public boolean isDirty() {
        return !content.equals(savedContent);
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidChangeContent() {
        return contentChanges;
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidChangeDirty() {
        return dirtyChanges;
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidSave() {
        return saves;
    }

    @Override
    public void save() throws IOException {
        boolean wasDirty = isDirty();
        Files.writeString(
                path, content, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        savedContent = content;
        if (wasDirty) {
            dirtyChanges.emit(this);
        }
        saves.emit(this);
    }

    @Override
    public void revert() throws IOException {
        String replacement = decode(Files.readAllBytes(path));
        boolean contentChanged = !content.equals(replacement);
        boolean wasDirty = isDirty();
        savedContent = replacement;
        content = replacement;
        if (contentChanged) {
            contentChanges.emit(this);
        }
        if (wasDirty != isDirty()) {
            dirtyChanges.emit(this);
        }
    }

    private static String decode(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("File is not valid UTF-8 text", exception);
        }
    }

    private static boolean containsNull(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }
}
