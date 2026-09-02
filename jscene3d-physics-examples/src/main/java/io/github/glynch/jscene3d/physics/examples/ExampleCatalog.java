/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import java.util.List;

/** Declares the stable ordered physics example suite. */
public final class ExampleCatalog {
    /** Prevents instantiation of this static catalog. */
    private ExampleCatalog() {
        throw new AssertionError("ExampleCatalog cannot be instantiated");
    }

    /**
     * Returns the complete renderer-independent physics example suite.
     *
     * @return physics suite metadata and factories
     */
    public static ExampleSuite suite() {
        return new ExampleSuite(
                "JScene3D Physics Examples",
                "JScene3D Physics",
                ExampleCatalog.class,
                "/META-INF/jscene3d/physics-examples/thumbnails",
                definitions());
    }

    /** Returns all physics definitions in stable display order. */
    static List<ExampleDefinition> definitions() {
        return List.of(new ExampleDefinition(
                "kinematic-movement",
                "Kinematic movement",
                "Movement",
                "Explicit fixed-step movement with gravity, wall sliding, steps, sensors, and debug lines.",
                List.of("physics", "kinematic", "collision", "slide", "step", "sensor", "debug"),
                List.of(),
                KinematicMovementExample::create));
    }
}
