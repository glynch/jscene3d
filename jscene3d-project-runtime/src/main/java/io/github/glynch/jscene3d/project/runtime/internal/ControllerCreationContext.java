/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.RuntimeNode;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerContext;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Runtime-owned context for one controller factory call. */
final class ControllerCreationContext extends AbstractCreationContext implements NodeControllerContext {
    private final RuntimeNode node;

    /** Creates one controller context. */
    ControllerCreationContext(
            GameProject project,
            SceneDefinition scene,
            RuntimeNode node,
            Map<String, ProjectValue> properties,
            RegisteredTypeDescriptor descriptor,
            EndpointRouter router,
            ProjectResourceResolver resources,
            BooleanSupplier enabled) {
        super(project, scene, node.definition(), properties, descriptor, router, resources, enabled);
        this.node = node;
    }

    @Override
    public RuntimeNode node() {
        return node;
    }
}
