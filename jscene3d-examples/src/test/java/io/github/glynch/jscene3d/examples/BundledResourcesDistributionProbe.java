/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.BundledResources;
import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies that archive-backed example resources become ordinary readable files. */
public final class BundledResourcesDistributionProbe {
    private static final String RESOURCE_NAME = "/io/github/glynch/jscene3d/examples/gltf/imported-cubes.gltf";

    /** Prevents instantiation of this distribution verification entry point. */
    private BundledResourcesDistributionProbe() {
        throw new AssertionError("BundledResourcesDistributionProbe cannot be instantiated");
    }

    /**
     * Resolves one resource from the shaded examples JAR and validates caching.
     *
     * @param arguments ignored command-line arguments
     * @throws Exception if the resource cannot be resolved or inspected
     */
    public static void main(String[] arguments) throws Exception {
        Path first = BundledResources.path(
                BundledResourcesDistributionProbe.class.getResource(RESOURCE_NAME), RESOURCE_NAME);
        Path second = BundledResources.path(
                BundledResourcesDistributionProbe.class.getResource(RESOURCE_NAME), RESOURCE_NAME);
        if (!Files.isRegularFile(first) || Files.size(first) == 0L) {
            throw new AssertionError("Bundled resource did not resolve to a readable file: " + first);
        }
        if (!first.equals(second)) {
            throw new AssertionError("Bundled resource was materialized more than once");
        }
    }
}
