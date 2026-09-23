/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;

/** Applies one validated component-property replacement to an authored entity. */
@FunctionalInterface
public interface EditorComponentPropertyEditor {
    /**
     * Replaces one authored component property.
     *
     * @param entityId authored entity to update
     * @param componentId component containing the property
     * @param propertyId property to replace
     * @param value validated replacement value
     */
    void set(EntityId entityId, ComponentId componentId, PropertyId propertyId, ProjectValue value);
}
