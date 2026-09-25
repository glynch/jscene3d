/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Immutable context and ordered entity roots for one retained structural definition.
 *
 * @param context identity and document context outside the entity tree
 * @param roots actual authored hierarchy roots in definition order
 */
public record EditorHierarchyProjection(Context context, List<EditorHierarchyNode> roots) {
    /** Copies and validates the complete hierarchy projection. */
    public EditorHierarchyProjection {
        Objects.requireNonNull(context, "context");
        roots = List.copyOf(roots);
    }

    /** Context of the definition that owns the projected occurrences.
     *
     * @param definitionId authoritative definition identity
     * @param kind structural definition kind
     * @param label authored definition label
     * @param source logical definition source
     * @param editable whether this definition supports authoring mutations
     */
    public record Context(AssetId definitionId, AssetKind kind, AuthoringText label, URI source, boolean editable) {
        /** Validates the definition context. */
        public Context {
            Objects.requireNonNull(definitionId, "definitionId");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(source, "source");
        }
    }
}
