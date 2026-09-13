/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.language.EditorTextDocument;
import io.github.glynch.jscene3d.editor.language.EditorTextDocumentChange;
import io.github.glynch.jscene3d.editor.language.EditorTextEdit;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorEventSource;
import io.github.glynch.jscene3d.editor.workingcopy.EditorTextWorkingCopy;
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
import java.util.List;
import java.util.Objects;

/** UTF-8 file content and persistence isolated from its Monaco presentation. */
public final class EditorTextFileWorkingCopy implements EditorTextWorkingCopy {
    private static final String TYPE = "io.github.glynch.jscene3d.editor.working-copy.text-file";
    private static final int MAXIMUM_BYTES = 10 * 1024 * 1024;

    private final Path path;
    private final EditorWorkingCopyId id;
    private final EditorLanguageId language;
    private final EditorEventSource<EditorWorkingCopy> contentChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorTextDocumentChange> textChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> dirtyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> saves = new EditorEventSource<>();
    private String savedContent;
    private String content;
    private int version = 1;

    private EditorTextFileWorkingCopy(Path path, EditorLanguageId language, String content) {
        this.path = path;
        this.id = new EditorWorkingCopyId(path.toUri(), TYPE);
        this.language = Objects.requireNonNull(language, "language");
        this.savedContent = content;
        this.content = content;
    }

    /** Opens a bounded UTF-8 text file, rejecting binary and malformed input. */
    public static EditorTextFileWorkingCopy load(Path path) throws IOException {
        return load(path, EditorLanguages.PLAIN_TEXT);
    }

    /** Opens a bounded UTF-8 text file with its resolved editor language. */
    public static EditorTextFileWorkingCopy load(Path path, EditorLanguageId language) throws IOException {
        Path source = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        long size = Files.size(source);
        if (size > MAXIMUM_BYTES) {
            throw new IOException("File is too large for the source editor (maximum 10 MiB)");
        }
        byte[] bytes = Files.readAllBytes(source);
        if (containsNull(bytes)) {
            throw new IOException("File appears to contain binary data");
        }
        return new EditorTextFileWorkingCopy(source, language, decode(bytes));
    }

    /** Returns the normalized source path. */
    public Path path() {
        return path;
    }

    /** Returns the current in-memory text. */
    @Override
    public String content() {
        return content;
    }

    @Override
    public EditorLanguageId language() {
        return language;
    }

    @Override
    public int version() {
        return version;
    }

    /** Replaces the current content after an editor change. */
    public void update(String replacement) {
        update(replacement, List.of(new EditorTextEdit(fullRange(content), replacement)));
    }

    /** Replaces current content and publishes the ordered Monaco edits which produced it. */
    public void update(String replacement, List<EditorTextEdit> edits) {
        String updated = Objects.requireNonNull(replacement, "replacement");
        List<EditorTextEdit> changes = List.copyOf(Objects.requireNonNull(edits, "edits"));
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("edits must not be empty");
        }
        if (content.equals(updated)) {
            return;
        }
        boolean wasDirty = isDirty();
        content = updated;
        version++;
        contentChanges.emit(this);
        textChanges.emit(new EditorTextDocumentChange(snapshot(), changes));
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
    public EditorEvent<EditorTextDocumentChange> onDidChangeText() {
        return textChanges;
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
        String previous = content;
        savedContent = replacement;
        content = replacement;
        if (contentChanged) {
            version++;
            contentChanges.emit(this);
            textChanges.emit(new EditorTextDocumentChange(
                    snapshot(), List.of(new EditorTextEdit(fullRange(previous), replacement))));
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

    private EditorTextDocument snapshot() {
        return new EditorTextDocument(id.resource(), language, version, content);
    }

    private static EditorTextRange fullRange(String text) {
        int line = 0;
        int character = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                line++;
                character = 0;
            } else {
                character++;
            }
        }
        return new EditorTextRange(new EditorTextPosition(0, 0), new EditorTextPosition(line, character));
    }
}
