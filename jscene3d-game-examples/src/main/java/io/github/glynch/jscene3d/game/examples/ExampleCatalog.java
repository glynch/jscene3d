/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import java.util.List;

/** Declares the stable ordered game-runtime example suite. */
public final class ExampleCatalog {
    /** Prevents instantiation of this static catalog. */
    private ExampleCatalog() {
        throw new AssertionError("ExampleCatalog cannot be instantiated");
    }

    /**
     * Returns the complete game-runtime example suite.
     *
     * @return game suite metadata and factories
     */
    public static ExampleSuite suite() {
        return new ExampleSuite(
                "JScene3D Game Examples",
                "JScene3D Game",
                ExampleCatalog.class,
                "/META-INF/jscene3d/game-examples/thumbnails",
                definitions());
    }

    /** Returns all game definitions in stable display order. */
    static List<ExampleDefinition> definitions() {
        return List.of(new ExampleDefinition(
                "first-person-sandbox",
                "First-person game sandbox",
                "Game Runtime",
                "Fixed simulation, semantic input, pointer look, character movement, and interpolated physics presentation.",
                List.of("game", "loop", "input", "actions", "physics", "binding", "interpolation", "first-person"),
                List.of(),
                FirstPersonSandboxExample::create));
    }
}
