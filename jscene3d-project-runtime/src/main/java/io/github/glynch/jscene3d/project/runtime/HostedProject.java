/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.util.Objects;

/** Loaded project metadata, definition catalog, and its owned inactive world. */
public final class HostedProject implements AutoCloseable {
    private final GameProject project;
    private final AssetCatalog assets;
    private final World world;
    private final ProjectLaunchRequest launchRequest;

    /** Stores one successfully composed host result. */
    HostedProject(GameProject project, AssetCatalog assets, World world, ProjectLaunchRequest launchRequest) {
        this.project = Objects.requireNonNull(project, "project");
        this.assets = Objects.requireNonNull(assets, "assets");
        this.world = Objects.requireNonNull(world, "world");
        this.launchRequest = Objects.requireNonNull(launchRequest, "launchRequest");
    }

    /**
     * Returns the validated project manifest.
     *
     * @return loaded project
     */
    public GameProject project() {
        return project;
    }

    /**
     * Returns the stable definition catalog used by the world.
     *
     * @return project asset catalog
     */
    public AssetCatalog assets() {
        return assets;
    }

    /**
     * Returns the composed inactive world.
     *
     * @return owned world
     */
    public World world() {
        return world;
    }

    /**
     * Returns the request that selected and parameterized this hosted world.
     *
     * @return immutable launch request
     */
    public ProjectLaunchRequest launchRequest() {
        return launchRequest;
    }

    /** Closes the world and its owned adapters. Repeated closure is harmless. */
    @Override
    public void close() {
        world.close();
    }
}
