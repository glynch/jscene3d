/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import java.util.Objects;
import java.util.Optional;

/** Stable authored identity associated with one realized renderer object. */
record SceneViewObjectIdentity(CompositionOccurrenceId occurrence, Optional<ComponentIdentity> component) {
    SceneViewObjectIdentity {
        Objects.requireNonNull(occurrence, "occurrence");
        Objects.requireNonNull(component, "component");
        component.ifPresent(identity -> {
            if (!occurrence.equals(identity.occurrence())) {
                throw new IllegalArgumentException("component must belong to occurrence");
            }
        });
    }

    static SceneViewObjectIdentity occurrence(CompositionOccurrenceId occurrence) {
        return new SceneViewObjectIdentity(occurrence, Optional.empty());
    }

    static SceneViewObjectIdentity component(ComponentIdentity component) {
        ComponentIdentity validComponent = Objects.requireNonNull(component, "component");
        return new SceneViewObjectIdentity(validComponent.occurrence(), Optional.of(validComponent));
    }
}
