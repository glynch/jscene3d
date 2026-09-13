/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EditorTextFileWorkingCopyTest {
    @Test
    void tracksChangesSavesAndRevertsUtf8Content(@TempDir Path directory) throws Exception {
        Path source = directory.resolve("Example.java");
        Files.writeString(source, "class Example {}\n");
        EditorTextFileWorkingCopy workingCopy = EditorTextFileWorkingCopy.load(source);
        AtomicInteger contentChanges = new AtomicInteger();
        AtomicInteger dirtyChanges = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        workingCopy.onDidChangeContent().subscribe(ignored -> contentChanges.incrementAndGet());
        workingCopy.onDidChangeDirty().subscribe(ignored -> dirtyChanges.incrementAndGet());
        workingCopy.onDidSave().subscribe(ignored -> saves.incrementAndGet());

        workingCopy.update("class Example { int value; }\n");

        assertThat(workingCopy.isDirty()).isTrue();
        assertThat(contentChanges).hasValue(1);
        assertThat(dirtyChanges).hasValue(1);

        workingCopy.save();

        assertThat(workingCopy.isDirty()).isFalse();
        assertThat(Files.readString(source)).isEqualTo("class Example { int value; }\n");
        assertThat(dirtyChanges).hasValue(2);
        assertThat(saves).hasValue(1);

        workingCopy.update("temporary");
        workingCopy.revert();

        assertThat(workingCopy.content()).isEqualTo("class Example { int value; }\n");
        assertThat(workingCopy.isDirty()).isFalse();
    }

    @Test
    void rejectsBinaryAndMalformedUtf8(@TempDir Path directory) throws Exception {
        Path binary = directory.resolve("binary.dat");
        Files.write(binary, new byte[] {1, 0, 2});
        Path malformed = directory.resolve("malformed.txt");
        Files.write(malformed, new byte[] {(byte) 0xc3, (byte) 0x28});

        assertThatIOException().isThrownBy(() -> EditorTextFileWorkingCopy.load(binary));
        assertThatIOException().isThrownBy(() -> EditorTextFileWorkingCopy.load(malformed));
    }
}
