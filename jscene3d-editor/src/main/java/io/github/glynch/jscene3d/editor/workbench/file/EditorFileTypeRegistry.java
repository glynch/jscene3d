/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.file;

import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorFileTypeId;
import io.github.glynch.jscene3d.editor.file.EditorFileTypes;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Resolves deterministic filename associations contributed by activated extensions. */
public final class EditorFileTypeRegistry implements EditorFileTypes {
    private final Map<EditorFileTypeId, EditorFileType> fileTypes = new LinkedHashMap<>();

    @Override
    public EditorRegistration register(EditorFileType fileType) {
        EditorFileType contribution = Objects.requireNonNull(fileType, "fileType");
        if (fileTypes.putIfAbsent(contribution.id(), contribution) != null) {
            throw new IllegalArgumentException("file type identity is already registered: " + contribution.id());
        }
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (active.compareAndSet(true, false)) {
                fileTypes.remove(contribution.id(), contribution);
            }
        };
    }

    @Override
    public Optional<EditorFileType> resolve(URI resource) {
        Path path = Path.of(Objects.requireNonNull(resource, "resource"));
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return Optional.empty();
        }
        String fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);
        Optional<String> extension = extension(fileName);
        return fileTypes.values().stream()
                .filter(candidate -> candidate.fileNames().contains(fileName)
                        || extension.map(candidate.extensions()::contains).orElse(false))
                .max(Comparator.comparingInt((EditorFileType candidate) ->
                                candidate.fileNames().contains(fileName) ? 1 : 0)
                        .thenComparingInt(EditorFileType::priority)
                        .thenComparing(candidate -> candidate.id().value()));
    }

    private static Optional<String> extension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator <= 0 || separator == fileName.length() - 1
                ? Optional.empty()
                : Optional.of(fileName.substring(separator + 1));
    }
}
