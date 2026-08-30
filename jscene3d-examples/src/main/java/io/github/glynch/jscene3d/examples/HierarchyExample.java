/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.Object3D;
import io.github.glynch.jscene3d.core.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Demonstrates ordered scene hierarchy construction, traversal, and reparenting. */
public final class HierarchyExample {
    /** Prevents instantiation of this example entry point. */
    private HierarchyExample() {}

    /**
     * Builds and verifies a small multilevel hierarchy.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        Scene solarSystem = new Scene();
        Group planetarySystem = new Group();
        Object3D planet = new Object3D();
        Object3D moon = new Object3D();

        solarSystem.setBackground(Color.BLACK);
        solarSystem.add(planetarySystem);
        planetarySystem.add(planet);
        planet.add(moon);

        List<Object3D> visited = new ArrayList<>();
        solarSystem.traverse(visited::add);
        requireOrder(visited, solarSystem, planetarySystem, planet, moon);
        if (!Objects.equals(solarSystem.background(), Color.BLACK)) {
            throw new IllegalStateException("Scene background was not retained");
        }

        solarSystem.add(moon);
        if (!Objects.equals(moon.parent(), solarSystem) || !planet.children().isEmpty()) {
            throw new IllegalStateException("Reparenting did not update both hierarchy relationships");
        }
    }

    /** Verifies the example's observed traversal order and fails visibly if it changes. */
    private static void requireOrder(List<Object3D> actual, Object3D... expected) {
        if (!actual.equals(List.of(expected))) {
            throw new IllegalStateException("Unexpected traversal order");
        }
    }
}
