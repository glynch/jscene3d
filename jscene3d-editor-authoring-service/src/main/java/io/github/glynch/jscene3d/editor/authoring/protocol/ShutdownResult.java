/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

/**
 * Acknowledges orderly service shutdown.
 *
 * @param shutdown always true after the request is accepted
 */
public record ShutdownResult(boolean shutdown) {}
