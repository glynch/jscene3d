/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.RingGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.loaders.TextureLoader;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.textures.Texture;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Owns all shared geometry, texture, and material descriptions used by the viewer. */
final class SolarSystemResources implements AutoCloseable {
    private static final String RESOURCE_DIRECTORY = "/io/github/glynch/jscene3d/examples/solar-system/";

    private final BufferGeometry sphereGeometry = SphereGeometry.create(1.0f, 48, 24);
    private final BufferGeometry ringGeometry = RingGeometry.create(1.3f, 2.25f, 128);
    private final List<Texture> textures = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();

    /** Creates an empty owner with the two shared geometry descriptions. */
    SolarSystemResources() {}

    /** Returns the shared unit-sphere geometry. */
    BufferGeometry sphereGeometry() {
        return sphereGeometry;
    }

    /** Returns the shared Saturn-ring geometry. */
    BufferGeometry ringGeometry() {
        return ringGeometry;
    }

    /** Loads and retains one texture-backed Lambert material. */
    LambertMaterial createLambertMaterial(String textureFileName) {
        Texture texture = loadTexture(textureFileName);
        LambertMaterial material = new LambertMaterial(Color.WHITE);
        material.setColorMap(texture);
        materials.add(material);
        return material;
    }

    /** Loads and retains one unlit texture-backed material. */
    BasicMaterial createBasicMaterial(String textureFileName) {
        Texture texture = loadTexture(textureFileName);
        BasicMaterial material = new BasicMaterial(Color.WHITE);
        material.setColorMap(texture);
        materials.add(material);
        return material;
    }

    /** Closes every retained description in reverse ownership order. */
    @Override
    public void close() {
        for (int index = materials.size() - 1; index >= 0; index--) {
            materials.get(index).close();
        }
        for (int index = textures.size() - 1; index >= 0; index--) {
            textures.get(index).close();
        }
        ringGeometry.close();
        sphereGeometry.close();
    }

    /** Resolves, loads, and retains one packaged PNG or JPEG texture. */
    private Texture loadTexture(String fileName) {
        URL resource = SolarSystemResources.class.getResource(RESOURCE_DIRECTORY + fileName);
        if (resource == null) {
            throw new IllegalStateException("Missing Solar System Viewer texture: " + fileName);
        }
        if (!resource.getProtocol().equals("file")) {
            throw new IllegalStateException("Solar System Viewer textures must be available as files: " + resource);
        }
        try {
            Texture texture = TextureLoader.load(Path.of(resource.toURI()));
            textures.add(texture);
            return texture;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid Solar System Viewer texture location: " + resource, exception);
        }
    }
}
