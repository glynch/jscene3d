/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;

/** Applies one validated component-property replacement to an authored entity. */
@FunctionalInterface
interface EditorComponentPropertyEditor {
    /** Replaces one authored component property. */
    void set(EntityId entityId, ComponentId componentId, PropertyId propertyId, ProjectValue value);
}
