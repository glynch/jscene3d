/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Action implementation for an endpoint declared without a payload. */
@FunctionalInterface
public interface RuntimeAction {
    /** Executes the action synchronously. */
    void execute();
}
