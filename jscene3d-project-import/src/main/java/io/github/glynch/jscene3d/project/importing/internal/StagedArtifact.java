/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import java.nio.file.Path;

/** One completed artifact and its engine-owned staging path.
 *
 * @param metadata public immutable metadata
 * @param path normalized absolute staging path
 * @param relativePath generation-relative cache path
 */
public record StagedArtifact(ImportedArtifactMetadata metadata, Path path, String relativePath) {}
