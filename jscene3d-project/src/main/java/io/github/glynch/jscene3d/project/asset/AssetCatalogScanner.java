/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Deterministic asset-header scan hidden behind {@link AssetCatalog#scan(Path)}. */
final class AssetCatalogScanner {
    private final ProjectJsonReader jsonReader = ProjectJsonReader.strict();
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();
    private final Map<AssetId, AssetMetadata> assets = new LinkedHashMap<>();

    /** Prevents external construction. */
    private AssetCatalogScanner() {}

    /** Scans one project directory without constructing complete definitions. */
    static AssetCatalogLoadResult scan(Path suppliedRoot) {
        Path absoluteRoot = suppliedRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(absoluteRoot)) {
            return rootFailure(absoluteRoot, "project root is not a directory: " + absoluteRoot);
        }
        Path root;
        try {
            root = absoluteRoot.toRealPath();
        } catch (IOException exception) {
            return rootFailure(absoluteRoot, "project root cannot be resolved: " + exception.getMessage());
        }
        AssetCatalogScanner scanner = new AssetCatalogScanner();
        scanner.scanFiles(root);
        if (scanner.hasErrors()) {
            return new AssetCatalogLoadResult(Optional.empty(), scanner.diagnostics);
        }
        return new AssetCatalogLoadResult(
                Optional.of(new AssetCatalog(root, List.copyOf(scanner.assets.values()))), scanner.diagnostics);
    }

    /** Creates one terminal root failure. */
    private static AssetCatalogLoadResult rootFailure(Path root, String technicalDetail) {
        DiagnosticCollector diagnostics = new DiagnosticCollector(root);
        diagnostics.error(AssetDiagnosticCode.ROOT_INVALID, technicalDetail, "");
        return new AssetCatalogLoadResult(Optional.empty(), diagnostics.diagnostics());
    }

    /** Discovers supported definition filenames in portable relative-path order. */
    private void scanFiles(Path root) {
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(AssetCatalogScanner::isDefinitionFile)
                    .sorted(Comparator.comparing(path -> portablePath(root.relativize(path))))
                    .forEach(path -> scanFile(root, path));
        } catch (IOException exception) {
            DiagnosticCollector collector = new DiagnosticCollector(root);
            collector.error(
                    AssetDiagnosticCode.FILE_READ_FAILED,
                    "asset tree cannot be scanned: " + exception.getMessage(),
                    "");
            diagnostics.addAll(collector.diagnostics());
        }
    }

    /** Reads and indexes one asset envelope. */
    private void scanFile(Path root, Path discoveredPath) {
        Path source;
        try {
            source = discoveredPath.toRealPath();
        } catch (IOException exception) {
            addError(
                    discoveredPath,
                    AssetDiagnosticCode.FILE_READ_FAILED,
                    "asset path cannot be resolved: " + exception.getMessage(),
                    "");
            return;
        }
        if (!source.startsWith(root)) {
            addError(source, AssetDiagnosticCode.PATH_ESCAPES_ROOT, "asset resolves outside project root", "");
            return;
        }
        try (InputStream input = Files.newInputStream(source)) {
            JsonNode raw = jsonReader.readTree(input);
            scanHeader(source, raw);
        } catch (JsonProcessingException exception) {
            addError(
                    source,
                    AssetDiagnosticCode.JSON_INVALID,
                    "asset header is invalid JSON: " + exception.getOriginalMessage(),
                    "");
        } catch (IOException exception) {
            addError(
                    source,
                    AssetDiagnosticCode.FILE_READ_FAILED,
                    "asset header cannot be read: " + exception.getMessage(),
                    "");
        }
    }

    /** Validates the minimal common envelope and indexes it by stable identity. */
    private void scanHeader(Path source, @Nullable JsonNode raw) {
        if (raw == null || !raw.isObject()) {
            addError(source, AssetDiagnosticCode.JSON_INVALID, "asset document must be a JSON object", "");
            return;
        }
        Optional<AssetId> id = readAssetId(source, raw.get("assetId"));
        Optional<AssetKind> kind = readKind(source, raw.get("assetType"));
        int formatVersion = readFormatVersion(source, raw.get("formatVersion"));
        kind.ifPresent(value -> validateSuffix(source, value));
        if (id.isEmpty() || kind.isEmpty() || formatVersion != AssetCatalog.FORMAT_VERSION) {
            return;
        }
        AssetMetadata metadata = new AssetMetadata(id.orElseThrow(), kind.orElseThrow(), formatVersion, source);
        AssetMetadata previous = assets.putIfAbsent(metadata.id(), metadata);
        if (previous != null) {
            addError(
                    source,
                    AssetDiagnosticCode.ID_DUPLICATE,
                    "asset ID " + metadata.id() + " is also declared by " + previous.path(),
                    "/assetId");
        }
    }

    /** Reads one required canonical asset identity. */
    private Optional<AssetId> readAssetId(Path source, JsonNode raw) {
        if (raw == null || !raw.isTextual()) {
            addError(source, AssetDiagnosticCode.FIELD_REQUIRED, "assetId must be a string", "/assetId");
            return Optional.empty();
        }
        try {
            return Optional.of(AssetId.from(raw.textValue()));
        } catch (IllegalArgumentException exception) {
            addError(source, AssetDiagnosticCode.ID_INVALID, exception.toString(), "/assetId");
            return Optional.empty();
        }
    }

    /** Reads one required supported asset kind. */
    private Optional<AssetKind> readKind(Path source, JsonNode raw) {
        if (raw == null || !raw.isTextual()) {
            addError(source, AssetDiagnosticCode.FIELD_REQUIRED, "assetType must be a string", "/assetType");
            return Optional.empty();
        }
        Optional<AssetKind> kind = AssetKind.fromSerializedName(raw.textValue());
        if (kind.isEmpty()) {
            addError(
                    source,
                    AssetDiagnosticCode.KIND_INVALID,
                    "unsupported assetType: " + raw.textValue(),
                    "/assetType");
        }
        return kind;
    }

    /** Reads the required current asset-format version. */
    private int readFormatVersion(Path source, JsonNode raw) {
        if (raw == null || !raw.canConvertToInt() || !raw.isIntegralNumber()) {
            addError(source, AssetDiagnosticCode.FIELD_REQUIRED, "formatVersion must be an integer", "/formatVersion");
            return 0;
        }
        int version = raw.intValue();
        if (version != AssetCatalog.FORMAT_VERSION) {
            addError(
                    source,
                    AssetDiagnosticCode.FORMAT_UNSUPPORTED,
                    "formatVersion must be " + AssetCatalog.FORMAT_VERSION + ": " + version,
                    "/formatVersion");
        }
        return version;
    }

    /** Ensures filenames remain cheap and deterministic to discover. */
    private void validateSuffix(Path source, AssetKind kind) {
        if (!source.getFileName().toString().endsWith(kind.fileSuffix())) {
            addError(
                    source,
                    AssetDiagnosticCode.KIND_SUFFIX_MISMATCH,
                    "assetType " + kind + " requires filename suffix " + kind.fileSuffix(),
                    "/assetType");
        }
    }

    /** Adds one source-local error to the complete deterministic scan result. */
    private void addError(Path source, AssetDiagnosticCode code, String detail, String location) {
        DiagnosticCollector collector = new DiagnosticCollector(source);
        collector.error(code, detail, location);
        diagnostics.addAll(collector.diagnostics());
    }

    /** Returns whether scanning has encountered a terminal diagnostic. */
    private boolean hasErrors() {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }

    /** Returns whether a path uses one of the supported discoverable suffixes. */
    private static boolean isDefinitionFile(Path path) {
        String name = path.getFileName().toString();
        for (AssetKind kind : AssetKind.values()) {
            if (name.endsWith(kind.fileSuffix())) {
                return true;
            }
        }
        return false;
    }

    /** Converts one relative path to its stable serialized separator form. */
    private static String portablePath(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }
}
