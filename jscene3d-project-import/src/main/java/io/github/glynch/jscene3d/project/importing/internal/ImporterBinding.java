/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;

/** Exact safe importer type paired with its trusted executable adapter.
 *
 * @param type registered importer identity and version
 * @param importer executable adapter
 */
public record ImporterBinding(RegisteredType type, ProjectImporter importer) {}
