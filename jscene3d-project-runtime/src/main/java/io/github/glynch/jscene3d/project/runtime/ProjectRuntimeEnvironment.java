/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.List;

/** Host-selected engine facilities used to compose each project world. */
public interface ProjectRuntimeEnvironment {
    /**
     * Returns safe built-in descriptors available independently of project extension artifacts.
     *
     * @return immutable descriptor collection
     */
    List<ExtensionDescriptor> descriptors();

    /**
     * Returns executable built-in component factories corresponding to {@link #descriptors()}.
     *
     * @return immutable runtime-extension collection
     */
    List<ComponentRuntimeExtension> runtimeExtensions();

    /**
     * Creates fresh world-scoped adapter bindings for one load operation.
     *
     * <p>A successfully composed world takes ownership. The host closes adapters after failed composition.
     *
     * @return bindings in ownership and reverse-cleanup order
     */
    List<WorldModuleBinding<?>> createWorldModules();

    /**
     * Returns the host-owned provider used to resolve immutable runtime resources.
     *
     * @return runtime resource provider
     */
    RuntimeResourceProvider resources();
}
