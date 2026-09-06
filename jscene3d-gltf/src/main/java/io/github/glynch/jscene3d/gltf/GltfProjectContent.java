/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.List;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Prepared project-native view of one selected static glTF scene and its owned resources. */
final class GltfProjectContent implements AutoCloseable {
    private final String name;
    private final List<Node> roots;
    private final List<BufferGeometry> geometries;
    private final List<Material> materials;
    private final List<Texture> textures;
    private boolean closed;

    /** Takes ownership of all converted resources. */
    GltfProjectContent(
            String name,
            List<Node> roots,
            List<BufferGeometry> geometries,
            List<Material> materials,
            List<Texture> textures) {
        this.name = Objects.requireNonNull(name, "name");
        this.roots = List.copyOf(roots);
        this.geometries = List.copyOf(geometries);
        this.materials = List.copyOf(materials);
        this.textures = List.copyOf(textures);
    }

    String name() {
        return name;
    }

    List<Node> roots() {
        return roots;
    }

    /** Closes every converted resource once after project publication finishes. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        textures.forEach(Texture::close);
        materials.forEach(Material::close);
        geometries.forEach(BufferGeometry::close);
    }

    /** Static source node with decomposed local transform, mesh primitives, and owned children. */
    record Node(int sourceIndex, String name, Transform transform, List<Primitive> primitives, List<Node> children) {
        Node {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(transform, "transform");
            primitives = List.copyOf(primitives);
            children = List.copyOf(children);
        }
    }

    /** One source mesh primitive and its shared converted resources. */
    record Primitive(
            int meshIndex, int primitiveIndex, int materialIndex, BufferGeometry geometry, StandardMaterial material) {
        Primitive {
            Objects.requireNonNull(geometry, "geometry");
            Objects.requireNonNull(material, "material");
        }
    }

    /** Immutable local transform snapshot. */
    record Transform(Vector3f position, Quaternionf orientation, Vector3f scale) {
        Transform {
            position = new Vector3f(position);
            orientation = new Quaternionf(orientation);
            scale = new Vector3f(scale);
        }

        @Override
        public Vector3f position() {
            return new Vector3f(position);
        }

        @Override
        public Quaternionf orientation() {
            return new Quaternionf(orientation);
        }

        @Override
        public Vector3f scale() {
            return new Vector3f(scale);
        }
    }
}
