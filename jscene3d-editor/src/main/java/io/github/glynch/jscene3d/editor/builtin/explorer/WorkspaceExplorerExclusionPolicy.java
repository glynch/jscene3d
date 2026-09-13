/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Decides which workspace paths are safe and useful to expose in the Explorer. */
public final class WorkspaceExplorerExclusionPolicy {
    private static final WorkspaceExplorerExclusionPolicy DEFAULTS = WorkspaceExplorerExclusionPolicy.of(
            Set.of(".git", "target"), Set.of(".DS_Store"), Set.of(Path.of(".jscene3d", "cache")));

    private final Set<String> directoryNames;
    private final Set<String> fileNames;
    private final Set<Path> relativeTrees;

    private WorkspaceExplorerExclusionPolicy(
            Set<String> directoryNames, Set<String> fileNames, Set<Path> relativeTrees) {
        this.directoryNames = directoryNames;
        this.fileNames = fileNames;
        this.relativeTrees = relativeTrees;
    }

    /**
     * Returns the editor's built-in exclusions.
     *
     * @return immutable default policy
     */
    public static WorkspaceExplorerExclusionPolicy defaults() {
        return DEFAULTS;
    }

    /**
     * Creates a policy from explicit exclusion categories.
     *
     * @param directoryNames directory names excluded wherever they occur
     * @param fileNames file names excluded wherever they occur
     * @param relativeTrees workspace-relative paths whose complete subtrees are excluded
     * @return immutable exclusion policy
     */
    public static WorkspaceExplorerExclusionPolicy of(
            Collection<String> directoryNames, Collection<String> fileNames, Collection<Path> relativeTrees) {
        return new WorkspaceExplorerExclusionPolicy(
                normalizedNames(directoryNames, "directoryNames"),
                normalizedNames(fileNames, "fileNames"),
                normalizedRelativeTrees(relativeTrees));
    }

    /**
     * Combines this policy with additional exclusions.
     *
     * @param additions exclusions contributed by another policy source
     * @return a policy containing both sets of exclusions
     */
    public WorkspaceExplorerExclusionPolicy plus(WorkspaceExplorerExclusionPolicy additions) {
        WorkspaceExplorerExclusionPolicy extra = Objects.requireNonNull(additions, "additions");
        return new WorkspaceExplorerExclusionPolicy(
                union(directoryNames, extra.directoryNames),
                union(fileNames, extra.fileNames),
                union(relativeTrees, extra.relativeTrees));
    }

    boolean includes(Path workspaceRoot, Path candidate) {
        Path root = Objects.requireNonNull(workspaceRoot, "workspaceRoot")
                .toAbsolutePath()
                .normalize();
        Path path =
                Objects.requireNonNull(candidate, "candidate").toAbsolutePath().normalize();
        if (!path.startsWith(root) || Files.isSymbolicLink(path)) {
            return false;
        }

        Path fileName = path.getFileName();
        if (fileName == null) {
            return true;
        }
        String name = fileName.toString();
        boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        if ((directory && directoryNames.contains(name)) || (!directory && fileNames.contains(name))) {
            return false;
        }

        Path relativePath = root.relativize(path);
        return relativeTrees.stream().noneMatch(relativePath::startsWith);
    }

    private static Set<String> normalizedNames(Collection<String> names, String parameterName) {
        Collection<String> suppliedNames = Objects.requireNonNull(names, parameterName);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String name : suppliedNames) {
            String value = Objects.requireNonNull(name, parameterName + " entry");
            Path path = Path.of(value);
            if (value.isBlank() || value.equals(".") || value.equals("..") || !path.equals(path.getFileName())) {
                throw new IllegalArgumentException(parameterName + " entries must be non-blank file names");
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }

    private static Set<Path> normalizedRelativeTrees(Collection<Path> relativeTrees) {
        Collection<Path> suppliedTrees = Objects.requireNonNull(relativeTrees, "relativeTrees");
        LinkedHashSet<Path> normalized = new LinkedHashSet<>();
        for (Path relativeTree : suppliedTrees) {
            Path path =
                    Objects.requireNonNull(relativeTree, "relativeTrees entry").normalize();
            if (path.isAbsolute() || path.toString().isBlank() || path.startsWith("..")) {
                throw new IllegalArgumentException("relativeTrees entries must remain inside the workspace");
            }
            normalized.add(path);
        }
        return Set.copyOf(normalized);
    }

    private static <T> Set<T> union(Set<T> first, Set<T> second) {
        LinkedHashSet<T> combined = new LinkedHashSet<>(first);
        combined.addAll(second);
        return Set.copyOf(combined);
    }
}
