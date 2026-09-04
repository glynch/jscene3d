/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeContext;
import io.github.glynch.jscene3d.project.scene.SceneNodeDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Runtime-owned context for one scene-node factory call. */
final class SceneNodeCreationContext extends AbstractCreationContext implements SceneNodeContext {
    private final Optional<RuntimeNode> parent;

    /** Creates one node context. */
    SceneNodeCreationContext(
            RuntimeCreationServices services,
            SceneNodeDefinition node,
            Map<String, ProjectValue> properties,
            RegisteredTypeDescriptor descriptor,
            BooleanSupplier enabled,
            Optional<RuntimeNode> parent) {
        super(services, node, properties, descriptor, enabled);
        this.parent = parent;
    }

    @Override
    public Optional<RuntimeNode> parent() {
        return parent;
    }
}
