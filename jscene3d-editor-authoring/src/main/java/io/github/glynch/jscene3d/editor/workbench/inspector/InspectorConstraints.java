/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyEditorDescriptor;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Structured descriptor constraints retained without display-string conversion.
 *
 * @param elementKind homogeneous array-element kind when constrained
 * @param exactElementCount exact array length when constrained
 * @param acceptedReferenceKinds accepted resource-reference namespaces
 * @param editor validated descriptor-owned editor semantics
 */
public record InspectorConstraints(
        Optional<ProjectValueKind> elementKind,
        Optional<Integer> exactElementCount,
        Set<ResourceReference.Kind> acceptedReferenceKinds,
        PropertyEditorDescriptor editor) {
    /** Copies all structured constraint values. */
    public InspectorConstraints {
        elementKind = Optional.ofNullable(elementKind.orElse(null));
        exactElementCount = Optional.ofNullable(exactElementCount.orElse(null));
        acceptedReferenceKinds = Set.copyOf(acceptedReferenceKinds);
        Objects.requireNonNull(editor, "editor");
    }

    /**
     * Returns an unconstrained property shape.
     *
     * @return empty constraints
     */
    public static InspectorConstraints empty() {
        return new InspectorConstraints(
                Optional.empty(), Optional.empty(), Set.of(), PropertyEditorDescriptor.defaultEditor());
    }
}
