/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.ResourceContent;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.textures.MipmapMode;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Supplies the runtime loaders corresponding exactly to {@link Spatial3dDescriptors}. */
public final class Spatial3dResourceLoaders {
    private static final List<RuntimeResourceLoader<?>> ALL =
            List.of(new MeshLoader(), new MaterialLoader(), new TextureLoader(), new BasicMaterialLoader());

    /** Prevents construction of this stable loader collection. */
    private Spatial3dResourceLoaders() {
        throw new AssertionError("Spatial3dResourceLoaders cannot be instantiated");
    }

    /**
     * Returns all built-in version-one spatial resource loaders.
     *
     * @return immutable loader list
     */
    public static List<RuntimeResourceLoader<?>> all() {
        return ALL;
    }

    /** Loads an owned mesh from its referenced binary payload. */
    private static final class MeshLoader implements RuntimeResourceLoader<Mesh3dResource> {
        @Override
        public RegisteredType type() {
            return Spatial3dDescriptors.meshResourceType();
        }

        @Override
        public Class<Mesh3dResource> valueType() {
            return Mesh3dResource.class;
        }

        @Override
        public Mesh3dResource load(ResourceDefinition definition, ResourceContent content) throws IOException {
            ProjectValue value = require(definition.properties(), "payload");
            if (!(value instanceof ProjectValue.ReferenceValue reference)) {
                throw new IllegalArgumentException("mesh resource payload must be a reference");
            }
            try (InputStream input = content.openPayload(reference.reference())) {
                BufferGeometry geometry = Spatial3dResourceCodec.readMesh(input);
                return Mesh3dResource.owning(geometry);
            }
        }
    }

    /** Reconstructs one owned texture-free standard material from portable properties. */
    private static final class MaterialLoader implements RuntimeResourceLoader<Material3dResource> {
        @Override
        public RegisteredType type() {
            return Spatial3dDescriptors.materialResourceType();
        }

        @Override
        public Class<Material3dResource> valueType() {
            return Material3dResource.class;
        }

        @Override
        public Material3dResource load(ResourceDefinition definition, ResourceContent content) {
            Map<String, ProjectValue> values = definition.properties();
            StandardMaterial material = new StandardMaterial(color(require(values, "color"), "color"));
            try {
                material.setMetalness(number(require(values, "metalness"), "metalness"));
                material.setRoughness(number(require(values, "roughness"), "roughness"));
                material.setEmissive(color(require(values, "emissive"), "emissive"));
                material.setEmissiveIntensity(number(require(values, "emissive-intensity"), "emissive-intensity"));
                material.setOpacity(number(require(values, "opacity"), "opacity"));
                material.setAlphaMode(enumValue(require(values, "alpha-mode"), AlphaMode.class, "alpha-mode"));
                material.setAlphaCutoff(number(require(values, "alpha-cutoff"), "alpha-cutoff"));
                material.setSide(enumValue(require(values, "side"), MaterialSide.class, "side"));
                material.setUsesVertexColors(booleanValue(require(values, "vertex-colors"), "vertex-colors"));
                return Material3dResource.owning(material);
            } catch (RuntimeException failure) {
                material.close();
                throw failure;
            }
        }
    }

    /** Reconstructs one owned texture from an exact RGBA8 payload and portable sampler properties. */
    private static final class TextureLoader implements RuntimeResourceLoader<Texture3dResource> {
        @Override
        public RegisteredType type() {
            return Spatial3dDescriptors.textureResourceType();
        }

        @Override
        public Class<Texture3dResource> valueType() {
            return Texture3dResource.class;
        }

        @Override
        public Texture3dResource load(ResourceDefinition definition, ResourceContent content) throws IOException {
            Map<String, ProjectValue> values = definition.properties();
            int width = positiveInteger(require(values, "width"), "width");
            int height = positiveInteger(require(values, "height"), "height");
            ProjectValue payload = require(values, "payload");
            if (!(payload instanceof ProjectValue.ReferenceValue reference)) {
                throw new IllegalArgumentException("texture resource payload must be a reference");
            }
            byte[] pixels = readPixels(content, reference, width, height);
            TextureColorSpace colorSpace =
                    enumValue(require(values, "color-space"), TextureColorSpace.class, "color-space");
            Texture texture = colorSpace == TextureColorSpace.SRGB
                    ? Texture.baseColor(width, height, pixels)
                    : Texture.data(width, height, pixels);
            try {
                texture.setMinificationFilter(
                        enumValue(require(values, "minification-filter"), TextureFilter.class, "minification-filter"));
                texture.setMagnificationFilter(enumValue(
                        require(values, "magnification-filter"), TextureFilter.class, "magnification-filter"));
                texture.setHorizontalWrap(
                        enumValue(require(values, "horizontal-wrap"), TextureWrap.class, "horizontal-wrap"));
                texture.setVerticalWrap(
                        enumValue(require(values, "vertical-wrap"), TextureWrap.class, "vertical-wrap"));
                texture.setCoordinateOrigin(enumValue(
                        require(values, "coordinate-origin"), TextureCoordinateOrigin.class, "coordinate-origin"));
                texture.setMipmapMode(enumValue(require(values, "mipmap-mode"), MipmapMode.class, "mipmap-mode"));
                return Texture3dResource.owning(texture);
            } catch (RuntimeException failure) {
                texture.close();
                throw failure;
            }
        }

        /** Reads an exact payload after checking dimensions without integer overflow. */
        private static byte[] readPixels(
                ResourceContent content, ProjectValue.ReferenceValue reference, int width, int height)
                throws IOException {
            long byteCount = 4L * width * height;
            if (byteCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("texture pixel payload is too large: " + byteCount);
            }
            try (InputStream input = content.openPayload(reference.reference())) {
                byte[] pixels = input.readNBytes((int) byteCount);
                if (pixels.length != byteCount || input.read() != -1) {
                    throw new IllegalArgumentException("texture pixel payload length must be " + byteCount + " bytes");
                }
                return pixels;
            }
        }
    }

    /** Reconstructs one owned unlit material while retaining its optional shared texture resource. */
    private static final class BasicMaterialLoader implements RuntimeResourceLoader<Material3dResource> {
        @Override
        public RegisteredType type() {
            return Spatial3dDescriptors.basicMaterialResourceType();
        }

        @Override
        public Class<Material3dResource> valueType() {
            return Material3dResource.class;
        }

        @Override
        public Material3dResource load(ResourceDefinition definition, ResourceContent content) {
            Map<String, ProjectValue> values = definition.properties();
            BasicMaterial material = new BasicMaterial(color(require(values, "color"), "color"));
            RuntimeResourceLease<Texture3dResource> textureLease = null;
            try {
                material.setOpacity(number(require(values, "opacity"), "opacity"));
                material.setAlphaMode(enumValue(require(values, "alpha-mode"), AlphaMode.class, "alpha-mode"));
                material.setAlphaCutoff(number(require(values, "alpha-cutoff"), "alpha-cutoff"));
                material.setSide(enumValue(require(values, "side"), MaterialSide.class, "side"));
                material.setUsesVertexColors(booleanValue(require(values, "vertex-colors"), "vertex-colors"));
                ProjectValue colorMap = values.get("color-map");
                if (colorMap != null) {
                    if (!(colorMap instanceof ProjectValue.ReferenceValue reference)) {
                        throw new IllegalArgumentException("resource property must be a reference: color-map");
                    }
                    textureLease = content.acquire(reference.reference(), Texture3dResource.class);
                    material.setColorMap(textureLease.value().texture());
                }
                return textureLease == null
                        ? Material3dResource.owning(material)
                        : Material3dResource.owning(material, List.of(textureLease));
            } catch (RuntimeException failure) {
                material.close();
                if (textureLease != null) {
                    textureLease.close();
                }
                throw failure;
            }
        }
    }

    /** Returns one required property. */
    private static ProjectValue require(Map<String, ProjectValue> values, String name) {
        ProjectValue value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("resource property is missing: " + name);
        }
        return value;
    }

    /** Reads one portable finite float. */
    private static float number(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException("resource property must be a number: " + name);
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("resource property must be finite: " + name);
        }
        return result;
    }

    /** Reads one positive exact integer. */
    private static int positiveInteger(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException("resource property must be a number: " + name);
        }
        try {
            int result = number.value().intValueExact();
            if (result < 1) {
                throw new IllegalArgumentException("resource property must be positive: " + name);
            }
            return result;
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("resource property must be an exact integer: " + name, failure);
        }
    }

    /** Reads one three-channel linear color. */
    private static Color color(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != 3) {
            throw new IllegalArgumentException("resource property must be a three-number array: " + name);
        }
        return Color.linear(
                number(array.values().get(0), name + "[0]"),
                number(array.values().get(1), name + "[1]"),
                number(array.values().get(2), name + "[2]"));
    }

    /** Reads one boolean property. */
    private static boolean booleanValue(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.BooleanValue booleanValue)) {
            throw new IllegalArgumentException("resource property must be a boolean: " + name);
        }
        return booleanValue.value();
    }

    /** Reads one enum using the stable lower-kebab-case serialized name. */
    private static <E extends Enum<E>> E enumValue(ProjectValue value, Class<E> type, String name) {
        if (!(value instanceof ProjectValue.TextValue text)) {
            throw new IllegalArgumentException("resource property must be text: " + name);
        }
        try {
            return Enum.valueOf(type, text.value().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("resource property has an unsupported value: " + name, failure);
        }
    }
}
