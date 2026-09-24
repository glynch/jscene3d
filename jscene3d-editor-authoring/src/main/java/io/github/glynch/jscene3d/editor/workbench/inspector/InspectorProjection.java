/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import java.util.List;
import java.util.Objects;

/** Immutable typed Inspector projection independent of UI controls and callbacks.
 *
 * @param target stable semantic target
 * @param title author-facing title
 * @param generated whether the target originates within a placed definition
 * @param editable whether the target supports mutations
 * @param sections typed sections in declaration order
 */
public record InspectorProjection(
        InspectorTarget target, String title, boolean generated, boolean editable, List<InspectorSection> sections) {
    /** Copies and validates projected Inspector data. */
    public InspectorProjection {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(title, "title");
        sections = List.copyOf(sections);
    }
}
