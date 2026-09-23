/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Locates the single project descriptor selected by a project directory or descriptor path. */
public final class ProjectDescriptorLocator {
    private ProjectDescriptorLocator() {}

    /**
     * Resolves one project directory and descriptor under the compatibility discovery rules.
     *
     * @param suppliedPath project directory or selected descriptor path
     * @return resolved location or deterministic structured diagnostics
     */
    public static Result locate(Path suppliedPath) {
        Path supplied = Objects.requireNonNull(suppliedPath, "suppliedPath")
                .toAbsolutePath()
                .normalize();
        if (Files.isDirectory(supplied)) {
            return locateDirectory(supplied);
        }
        if (Files.isRegularFile(supplied)) {
            return locateSelectedDescriptor(supplied);
        }
        if (isRecognizedDescriptorName(supplied)) {
            return failure(
                    supplied,
                    ProjectDiagnosticCode.DESCRIPTOR_MISSING,
                    "selected project descriptor does not exist or is not a regular file: " + supplied);
        }
        return failure(
                supplied,
                ProjectDiagnosticCode.DIRECTORY_MISSING,
                "project directory does not exist or is not a directory: " + supplied);
    }

    /**
     * Returns whether a directory contains any independently owned project descriptor candidate.
     *
     * <p>This intentionally does not validate exact descriptor count. Asset scanning needs only a conservative project
     * boundary; loading that nested project reports any ambiguity.
     *
     * @param directory directory visited by asset discovery
     * @return {@code true} when a current or legacy descriptor candidate is present
     * @throws IOException if the directory entries cannot be read
     */
    public static boolean containsDescriptorCandidate(Path directory) throws IOException {
        if (Files.isRegularFile(directory.resolve(ProjectLoader.LEGACY_DESCRIPTOR_NAME))) {
            return true;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.anyMatch(ProjectDescriptorLocator::isCurrentDescriptorCandidate);
        }
    }

    /** Resolves a directory before scanning only its immediate children. */
    private static Result locateDirectory(Path suppliedRoot) {
        Path root;
        try {
            root = suppliedRoot.toRealPath();
        } catch (IOException exception) {
            return failure(
                    suppliedRoot,
                    ProjectDiagnosticCode.DIRECTORY_READ_FAILED,
                    "project directory cannot be resolved: " + exception.getMessage());
        }
        return discover(root);
    }

    /** Validates a selected descriptor name, then applies the same root-wide discovery rules. */
    private static Result locateSelectedDescriptor(Path suppliedDescriptor) {
        if (!isRecognizedDescriptorName(suppliedDescriptor)) {
            return failure(
                    suppliedDescriptor,
                    ProjectDiagnosticCode.DESCRIPTOR_INVALID,
                    "selected project descriptor must end in " + ProjectLoader.DESCRIPTOR_SUFFIX + " or equal "
                            + ProjectLoader.LEGACY_DESCRIPTOR_NAME + ": " + suppliedDescriptor);
        }
        Path parent = suppliedDescriptor.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            return failure(
                    suppliedDescriptor,
                    ProjectDiagnosticCode.DIRECTORY_MISSING,
                    "project descriptor parent does not exist or is not a directory: " + parent);
        }
        return locateDirectory(parent);
    }

    /** Applies current and legacy descriptor precedence within one canonical project root. */
    private static Result discover(Path root) {
        List<Path> current;
        try (Stream<Path> entries = Files.list(root)) {
            current = entries.filter(ProjectDescriptorLocator::isCurrentDescriptorCandidate)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException exception) {
            return failure(
                    root,
                    ProjectDiagnosticCode.DIRECTORY_READ_FAILED,
                    "project directory cannot be scanned for descriptors: " + exception.getMessage());
        }

        Path legacy = root.resolve(ProjectLoader.LEGACY_DESCRIPTOR_NAME);
        boolean hasLegacy = Files.isRegularFile(legacy);
        if (current.size() > 1 || (!current.isEmpty() && hasLegacy)) {
            return ambiguous(root, current, hasLegacy);
        }
        if (current.size() == 1) {
            return validateSelected(root, current.getFirst(), false);
        }
        if (hasLegacy) {
            return validateSelected(root, legacy, true);
        }
        return failure(
                root,
                ProjectDiagnosticCode.DESCRIPTOR_MISSING,
                "project root contains no " + ProjectLoader.DESCRIPTOR_SUFFIX + " descriptor or legacy "
                        + ProjectLoader.LEGACY_DESCRIPTOR_NAME + ": " + root);
    }

    /** Produces one deterministic ambiguity diagnostic naming candidates in discovery order. */
    private static Result ambiguous(Path root, List<Path> current, boolean hasLegacy) {
        List<String> names = new ArrayList<>(current.size() + (hasLegacy ? 1 : 0));
        current.stream().map(path -> path.getFileName().toString()).forEach(names::add);
        if (hasLegacy) {
            names.add(ProjectLoader.LEGACY_DESCRIPTOR_NAME);
        }
        DiagnosticCollector diagnostics = new DiagnosticCollector(root);
        diagnostics.error(
                ProjectDiagnosticCode.DESCRIPTOR_AMBIGUOUS,
                "project root contains ambiguous descriptors: " + String.join(", ", names),
                "");
        return new Result(Optional.empty(), diagnostics.diagnostics());
    }

    /** Verifies descriptor readability and real-target confinement before returning its location. */
    private static Result validateSelected(Path root, Path descriptor, boolean legacy) {
        if (!Files.isReadable(descriptor)) {
            return failure(
                    descriptor,
                    ProjectDiagnosticCode.MANIFEST_READ_FAILED,
                    "project descriptor is not readable: " + descriptor);
        }
        Path realDescriptor;
        try {
            realDescriptor = descriptor.toRealPath();
        } catch (IOException exception) {
            return failure(
                    descriptor,
                    ProjectDiagnosticCode.MANIFEST_READ_FAILED,
                    "project descriptor cannot be resolved: " + exception.getMessage());
        }
        if (!realDescriptor.startsWith(root)) {
            return failure(
                    descriptor,
                    ProjectDiagnosticCode.MANIFEST_ESCAPES_PROJECT,
                    "project descriptor resolves outside the project directory: " + descriptor);
        }

        List<ProjectDiagnostic> diagnostics = List.of();
        if (legacy) {
            diagnostics = List.of(new ProjectDiagnostic(
                    ProjectDiagnostic.Severity.WARNING,
                    ProjectDiagnosticCode.DESCRIPTOR_LEGACY,
                    descriptor.toUri(),
                    "",
                    Map.of("replacementSuffix", ProjectLoader.DESCRIPTOR_SUFFIX)));
        }
        return new Result(Optional.of(new Location(root, descriptor)), diagnostics);
    }

    /** Creates one terminal discovery result. */
    private static Result failure(Path source, ProjectDiagnosticCode code, String technicalDetail) {
        DiagnosticCollector diagnostics = new DiagnosticCollector(source);
        diagnostics.error(code, technicalDetail, "");
        return new Result(Optional.empty(), diagnostics.diagnostics());
    }

    /** Returns whether an entry is a regular current-format descriptor candidate. */
    private static boolean isCurrentDescriptorCandidate(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(ProjectLoader.DESCRIPTOR_SUFFIX);
    }

    /** Returns whether a selected filename belongs to the current or legacy descriptor contract. */
    private static boolean isRecognizedDescriptorName(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(ProjectLoader.DESCRIPTOR_SUFFIX) || name.equals(ProjectLoader.LEGACY_DESCRIPTOR_NAME);
    }

    /** Canonical project root and selected descriptor path.
     *
     * @param root canonical project root
     * @param descriptor selected immediate-child descriptor path
     */
    public record Location(Path root, Path descriptor) {
        /** Validates normalized absolute location paths. */
        public Location {
            root = requireNormalizedAbsolute(root, "root");
            descriptor = requireNormalizedAbsolute(descriptor, "descriptor");
            if (!Objects.equals(descriptor.getParent(), root)) {
                throw new IllegalArgumentException("descriptor must be an immediate child of root: " + descriptor);
            }
        }
    }

    /** Descriptor discovery result containing a location exactly when no error was reported.
     *
     * @param location resolved descriptor location, or empty when discovery failed
     * @param diagnostics ordered discovery warnings or errors
     */
    public record Result(Optional<Location> location, List<ProjectDiagnostic> diagnostics) {
        /** Validates and copies one discovery result. */
        public Result {
            Objects.requireNonNull(location, "location");
            diagnostics = List.copyOf(diagnostics);
            boolean hasErrors = diagnostics.stream()
                    .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
            if (location.isPresent() == hasErrors) {
                throw new IllegalArgumentException(
                        "a location must be present exactly when diagnostics have no errors");
            }
        }
    }
}
