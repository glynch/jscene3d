/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.nio.file.Path;

/** Stable host seam for loading one project into an inactive runtime world. */
@FunctionalInterface
public interface ProjectHost {
    /**
     * Loads and composes the project rooted at {@code projectRoot}.
     *
     * <p>The returned project owns its composed world. Loading performs no activation and creates no execution thread.
     *
     * @param projectRoot project directory containing {@code jscene3d.json}
     * @return loaded project owning one inactive world
     * @throws ProjectHostException if discovery, validation, composition, or application preparation fails
     */
    HostedProject load(Path projectRoot);
}
