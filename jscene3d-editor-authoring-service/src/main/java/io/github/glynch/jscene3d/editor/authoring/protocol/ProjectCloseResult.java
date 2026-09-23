/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import org.jspecify.annotations.Nullable;

/**
 * Outcome of an idempotent project close.
 *
 * @param closed whether an active project was closed
 * @param invalidatedProjectGeneration generation invalidated by the close
 */
public record ProjectCloseResult(boolean closed, @Nullable Long invalidatedProjectGeneration) {}
