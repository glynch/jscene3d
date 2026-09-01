/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v2.GltfModelCreatorV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.gltf.internal.AccessorDecoder;
import io.github.glynch.jscene3d.gltf.internal.ImageDecoder;
import io.github.glynch.jscene3d.gltf.internal.ImageDecoder.DecodedImage;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Performs one complete JglTF-to-JScene3D conversion without exposing parser types. */
final class GltfConverter {
    private final Path source;
    private final GltfModel model;
    private final int defaultSceneIndex;
    private final List<BufferGeometry> geometries = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Texture> textures = new ArrayList<>();
    private final Map<MeshPrimitiveModel, BufferGeometry> geometryCache = new IdentityHashMap<>();
    private final Map<MaterialModel, StandardMaterial> materialCache = new IdentityHashMap<>();
    private final Map<TextureModel, EnumMap<TextureColorSpace, Texture>> textureCache = new IdentityHashMap<>();
    private final Map<ImageModel, DecodedImage> imageCache = new IdentityHashMap<>();

    private @Nullable StandardMaterial defaultMaterial;

    /** Retains parsed model state for one isolated conversion. */
    private GltfConverter(Path source, GltfModel model, int defaultSceneIndex) {
        this.source = source;
        this.model = model;
        this.defaultSceneIndex = defaultSceneIndex;
    }

    /** Reads and converts one source file. */
    static LoadedGltf load(Path source) throws IOException {
        GltfAsset asset = new GltfAssetReader().read(source);
        if (!(asset instanceof GltfAssetV2 assetV2)) {
            throw new GltfLoadException(source, "Only glTF 2.0 assets are supported", null);
        }
        GlTF gltf = assetV2.getGltf();
        validateRootCapabilities(source, gltf);
        int sceneIndex = gltf.getScene() == null ? 0 : gltf.getScene();
        GltfConverter converter = new GltfConverter(source, GltfModelCreatorV2.create(assetV2), sceneIndex);
        return converter.convert();
    }

    /** Converts and transfers ownership, closing partial resources after any failure. */
    private LoadedGltf convert() {
        try {
            Scene scene = convertScene();
            return new LoadedGltf(scene, geometries, materials, textures);
        } catch (RuntimeException failure) {
            closePartialResources();
            throw failure;
        }
    }

    /** Rejects unsupported root features before JglTF constructs their specialized models. */
    private static void validateRootCapabilities(Path source, GlTF gltf) {
        rejectPresent(source, "required extensions " + gltf.getExtensionsRequired(), gltf.getExtensionsRequired());
        rejectPresent(source, "animation", gltf.getAnimations());
        rejectPresent(source, "skinning", gltf.getSkins());
        rejectPresent(source, "embedded cameras", gltf.getCameras());
    }

    /** Rejects one nullable root collection when it contains entries. */
    private static void rejectPresent(Path source, String capability, @Nullable List<?> values) {
        if (values != null && !values.isEmpty()) {
            throw new GltfLoadException(source, "Unsupported glTF capability: " + capability, null);
        }
    }

    /** Converts the selected scene using an iterative graph walk. */
    private Scene convertScene() {
        Scene scene = new Scene();
        List<SceneModel> sceneModels = model.getSceneModels();
        if (sceneModels.isEmpty()) {
            return scene;
        }
        if (defaultSceneIndex < 0 || defaultSceneIndex >= sceneModels.size()) {
            throw failure("Default scene index is outside the scene list: " + defaultSceneIndex, null);
        }
        List<NodeModel> roots = sceneModels.get(defaultSceneIndex).getNodeModels();
        Map<NodeModel, Group> converted = new IdentityHashMap<>();
        ArrayDeque<NodeModel> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            NodeModel node = pending.removeFirst();
            if (converted.containsKey(node)) {
                continue;
            }
            Group group = convertNode(node);
            converted.put(node, group);
            pending.addAll(node.getChildren());
        }
        for (Map.Entry<NodeModel, Group> entry : converted.entrySet()) {
            for (NodeModel child : entry.getKey().getChildren()) {
                Group convertedChild = converted.get(child);
                if (convertedChild == null) {
                    throw failure("Scene hierarchy contains an unreachable child", null);
                }
                entry.getValue().add(convertedChild);
            }
        }
        for (NodeModel root : roots) {
            scene.add(Objects.requireNonNull(converted.get(root), "converted root"));
        }
        return scene;
    }

    /** Converts one node's local transform and attached mesh primitives. */
    private Group convertNode(NodeModel node) {
        if (node.getSkinModel() != null) {
            unsupported("skinned node");
        }
        if (node.getCameraModel() != null) {
            unsupported("camera node");
        }
        Group group = new Group();
        applyTransform(node, group);
        for (MeshModel meshModel : node.getMeshModels()) {
            float[] weights = meshModel.getWeights();
            if (weights != null && weights.length > 0) {
                unsupported("mesh morph weights");
            }
            for (MeshPrimitiveModel primitive : meshModel.getMeshPrimitiveModels()) {
                group.add(convertPrimitive(primitive));
            }
        }
        return group;
    }

    /** Applies either a decomposed matrix or explicit glTF TRS values. */
    private void applyTransform(NodeModel node, Object3D target) {
        float[] matrix = node.getMatrix();
        if (matrix != null) {
            applyMatrix(matrix, target);
            return;
        }
        float[] translation = node.getTranslation();
        if (translation != null) {
            target.setPosition(translation[0], translation[1], translation[2]);
        }
        float[] rotation = node.getRotation();
        if (rotation != null) {
            target.setQuaternion(rotation[0], rotation[1], rotation[2], rotation[3]);
        }
        float[] scale = node.getScale();
        if (scale != null) {
            target.setScale(scale[0], scale[1], scale[2]);
        }
    }

    /** Decomposes an affine matrix and rejects transforms containing unsupported shear. */
    private void applyMatrix(float[] values, Object3D target) {
        if (values.length != 16) {
            throw failure("Node matrix must contain 16 values", null);
        }
        Matrix4f matrix = new Matrix4f().set(values);
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Vector3f scale = matrix.getScale(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
        Matrix4f reconstructed = new Matrix4f().translationRotateScale(translation, rotation, scale);
        for (int index = 0; index < 16; index++) {
            if (Math.abs(matrix.get(index / 4, index % 4) - reconstructed.get(index / 4, index % 4)) > 1.0e-4f) {
                unsupported("node matrices containing shear or reflection");
            }
        }
        target.setPosition(translation);
        target.setQuaternion(rotation);
        target.setScale(scale);
    }

    /** Converts one triangle primitive into a mesh sharing cached resources. */
    private Mesh convertPrimitive(MeshPrimitiveModel primitive) {
        if (primitive.getMode() != GltfConstants.GL_TRIANGLES) {
            unsupported("non-triangle primitive mode " + primitive.getMode());
        }
        if (!primitive.getTargets().isEmpty()) {
            unsupported("morph targets");
        }
        BufferGeometry geometry = geometryCache.computeIfAbsent(primitive, this::createGeometry);
        StandardMaterial material = materialFor(primitive.getMaterialModel());
        if (primitive.getAttributes().containsKey("COLOR_0")) {
            material.setUsesVertexColors(true);
        }
        return new Mesh(geometry, material);
    }

    /** Creates geometry from renderer-supported glTF attributes. */
    private BufferGeometry createGeometry(MeshPrimitiveModel primitive) {
        Map<String, AccessorModel> attributes = primitive.getAttributes();
        if (attributes.containsKey("JOINTS_0") || attributes.containsKey("WEIGHTS_0")) {
            unsupported("skinning attributes");
        }
        AccessorModel position = attributes.get("POSITION");
        if (position == null) {
            throw failure("Triangle primitive is missing POSITION", null);
        }
        BufferGeometry geometry = new BufferGeometry();
        geometries.add(geometry);
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(
                        AccessorDecoder.floats(position, de.javagl.jgltf.model.ElementType.VEC3, "POSITION"), 3));
        setOptionalAttribute(geometry, attributes, "NORMAL", BufferGeometry.NORMAL, 3);
        setOptionalAttribute(geometry, attributes, "TEXCOORD_0", BufferGeometry.UV, 2);
        setOptionalColor(geometry, attributes.get("COLOR_0"));
        AccessorModel indices = primitive.getIndices();
        if (indices != null) {
            geometry.setIndex(IndexBuffer.of(AccessorDecoder.indices(indices)));
        }
        return geometry;
    }

    /** Adds one optional fixed-width vertex attribute. */
    private static void setOptionalAttribute(
            BufferGeometry geometry,
            Map<String, AccessorModel> attributes,
            String sourceName,
            String targetName,
            int components) {
        AccessorModel accessor = attributes.get(sourceName);
        if (accessor != null) {
            de.javagl.jgltf.model.ElementType type =
                    components == 2 ? de.javagl.jgltf.model.ElementType.VEC2 : de.javagl.jgltf.model.ElementType.VEC3;
            geometry.setAttribute(
                    targetName, BufferAttribute.of(AccessorDecoder.floats(accessor, type, sourceName), components));
        }
    }

    /** Adds an optional RGB or RGBA vertex-colour attribute. */
    private static void setOptionalColor(BufferGeometry geometry, @Nullable AccessorModel accessor) {
        if (accessor == null) {
            return;
        }
        int components = accessor.getElementType().getNumComponents();
        if (components != 3 && components != 4) {
            throw new IllegalArgumentException("COLOR_0 must use VEC3 or VEC4 elements");
        }
        geometry.setAttribute(
                BufferGeometry.COLOR,
                BufferAttribute.of(AccessorDecoder.floats(accessor, accessor.getElementType(), "COLOR_0"), components));
    }

    /** Returns or creates the StandardMaterial corresponding to one glTF material. */
    private StandardMaterial materialFor(@Nullable MaterialModel materialModel) {
        if (materialModel == null) {
            if (defaultMaterial == null) {
                defaultMaterial = new StandardMaterial();
                materials.add(defaultMaterial);
            }
            return defaultMaterial;
        }
        StandardMaterial existing = materialCache.get(materialModel);
        if (existing != null) {
            return existing;
        }
        if (!(materialModel instanceof MaterialModelV2 sourceMaterial)) {
            throw failure("Only glTF 2.0 materials are supported", null);
        }
        StandardMaterial material = createMaterial(sourceMaterial);
        materialCache.put(materialModel, material);
        return material;
    }

    /** Converts metallic-roughness factors, textures, alpha state, and face orientation. */
    private StandardMaterial createMaterial(MaterialModelV2 sourceMaterial) {
        float[] baseColor = sourceMaterial.getBaseColorFactor();
        StandardMaterial material = new StandardMaterial(Color.linear(baseColor[0], baseColor[1], baseColor[2]));
        materials.add(material);
        material.setOpacity(baseColor[3]);
        material.setMetalness(sourceMaterial.getMetallicFactor());
        material.setRoughness(sourceMaterial.getRoughnessFactor());
        float[] emissive = sourceMaterial.getEmissiveFactor();
        material.setEmissive(Color.linear(emissive[0], emissive[1], emissive[2]));
        material.setOcclusionStrength(sourceMaterial.getOcclusionStrength());
        material.setNormalScale(sourceMaterial.getNormalScale(), sourceMaterial.getNormalScale());
        material.setAlphaMode(
                switch (sourceMaterial.getAlphaMode()) {
                    case OPAQUE -> AlphaMode.OPAQUE;
                    case MASK -> AlphaMode.MASK;
                    case BLEND -> AlphaMode.BLEND;
                });
        material.setAlphaCutoff(sourceMaterial.getAlphaCutoff());
        if (sourceMaterial.isDoubleSided()) {
            material.setSide(MaterialSide.DOUBLE);
        }
        setTexture(
                material,
                sourceMaterial.getBaseColorTexture(),
                sourceMaterial.getBaseColorTexcoord(),
                TextureRole.COLOR);
        setTexture(
                material,
                sourceMaterial.getMetallicRoughnessTexture(),
                sourceMaterial.getMetallicRoughnessTexcoord(),
                TextureRole.METALLIC_ROUGHNESS);
        setTexture(material, sourceMaterial.getNormalTexture(), sourceMaterial.getNormalTexcoord(), TextureRole.NORMAL);
        setTexture(
                material,
                sourceMaterial.getOcclusionTexture(),
                sourceMaterial.getOcclusionTexcoord(),
                TextureRole.OCCLUSION);
        setTexture(
                material,
                sourceMaterial.getEmissiveTexture(),
                sourceMaterial.getEmissiveTexcoord(),
                TextureRole.EMISSIVE);
        return material;
    }

    /** Applies an optional texture after validating primary-coordinate selection. */
    private void setTexture(
            StandardMaterial material,
            @Nullable TextureModel textureModel,
            @Nullable Integer texcoord,
            TextureRole role) {
        if (textureModel == null) {
            return;
        }
        if (texcoord != null && texcoord != 0) {
            unsupported("texture coordinate set " + texcoord);
        }
        Texture texture = textureFor(textureModel, role.colorSpace());
        switch (role) {
            case COLOR -> material.setColorMap(texture);
            case METALLIC_ROUGHNESS -> material.setMetalnessRoughnessMap(texture);
            case NORMAL -> material.setNormalMap(texture);
            case OCCLUSION -> material.setOcclusionMap(texture);
            case EMISSIVE -> material.setEmissiveMap(texture);
        }
    }

    /** Returns a cached texture with a role-correct colour-space interpretation. */
    private Texture textureFor(TextureModel textureModel, TextureColorSpace colorSpace) {
        EnumMap<TextureColorSpace, Texture> variants =
                textureCache.computeIfAbsent(textureModel, ignored -> new EnumMap<>(TextureColorSpace.class));
        Texture existing = variants.get(colorSpace);
        if (existing != null) {
            return existing;
        }
        ImageModel imageModel = Objects.requireNonNull(textureModel.getImageModel(), "texture image");
        DecodedImage image;
        try {
            image = imageCache.computeIfAbsent(imageModel, this::decodeImageUnchecked);
        } catch (ImageDecodeFailure failure) {
            throw failure("Failed to decode glTF image", failure.getCause());
        }
        Texture texture = colorSpace == TextureColorSpace.SRGB
                ? Texture.baseColor(image.width(), image.height(), image.pixels())
                : Texture.data(image.width(), image.height(), image.pixels());
        texture.setCoordinateOrigin(TextureCoordinateOrigin.TOP_LEFT);
        textures.add(texture);
        configureSampler(texture, textureModel);
        variants.put(colorSpace, texture);
        return texture;
    }

    /** Adapts checked image decoding for identity-cache construction. */
    private DecodedImage decodeImageUnchecked(ImageModel imageModel) {
        try {
            return ImageDecoder.decode(imageModel);
        } catch (IOException exception) {
            throw new ImageDecodeFailure(exception);
        }
    }

    /** Maps glTF sampler constants and defaults to renderer-independent texture state. */
    private static void configureSampler(Texture texture, TextureModel sourceTexture) {
        texture.setMagnificationFilter(filter(sourceTexture.getMagFilter(), false));
        texture.setMinificationFilter(filter(sourceTexture.getMinFilter(), true));
        texture.setHorizontalWrap(wrap(sourceTexture.getWrapS()));
        texture.setVerticalWrap(wrap(sourceTexture.getWrapT()));
    }

    /** Maps a nullable glTF filter constant, applying glTF defaults. */
    private static TextureFilter filter(@Nullable Integer value, boolean minification) {
        if (value == null) {
            return minification ? TextureFilter.LINEAR_MIPMAP_LINEAR : TextureFilter.LINEAR;
        }
        return switch (value) {
            case GltfConstants.GL_NEAREST -> TextureFilter.NEAREST;
            case GltfConstants.GL_LINEAR -> TextureFilter.LINEAR;
            case GltfConstants.GL_NEAREST_MIPMAP_NEAREST -> TextureFilter.NEAREST_MIPMAP_NEAREST;
            case GltfConstants.GL_LINEAR_MIPMAP_NEAREST -> TextureFilter.LINEAR_MIPMAP_NEAREST;
            case GltfConstants.GL_NEAREST_MIPMAP_LINEAR -> TextureFilter.NEAREST_MIPMAP_LINEAR;
            case GltfConstants.GL_LINEAR_MIPMAP_LINEAR -> TextureFilter.LINEAR_MIPMAP_LINEAR;
            default -> throw new IllegalArgumentException("unsupported glTF texture filter: " + value);
        };
    }

    /** Maps a nullable glTF wrap constant, applying the repeat default. */
    private static TextureWrap wrap(@Nullable Integer value) {
        if (value == null) {
            return TextureWrap.REPEAT;
        }
        return switch (value) {
            case GltfConstants.GL_REPEAT -> TextureWrap.REPEAT;
            case GltfConstants.GL_MIRRORED_REPEAT -> TextureWrap.MIRRORED_REPEAT;
            case GltfConstants.GL_CLAMP_TO_EDGE -> TextureWrap.CLAMP_TO_EDGE;
            default -> throw new IllegalArgumentException("unsupported glTF texture wrap: " + value);
        };
    }

    /** Closes every resource constructed before a failed conversion. */
    private void closePartialResources() {
        textures.forEach(Texture::close);
        materials.forEach(Material::close);
        geometries.forEach(BufferGeometry::close);
    }

    /** Throws a source-aware unsupported-capability diagnostic. */
    private void unsupported(String capability) {
        throw failure("Unsupported glTF capability: " + capability, null);
    }

    /** Creates a source-aware conversion failure. */
    private GltfLoadException failure(String message, @Nullable Throwable cause) {
        return new GltfLoadException(source, message, cause);
    }

    /** Distinguishes checked decoder failures crossing a cache factory. */
    private static final class ImageDecodeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /** Retains the checked image failure as its cause. */
        private ImageDecodeFailure(IOException cause) {
            super(cause);
        }
    }

    /** Identifies map semantics and their required colour-space interpretation. */
    private enum TextureRole {
        /** Base-colour map. */
        COLOR(TextureColorSpace.SRGB),
        /** Shared metallic-roughness data map. */
        METALLIC_ROUGHNESS(TextureColorSpace.LINEAR),
        /** Tangent-space normal data map. */
        NORMAL(TextureColorSpace.LINEAR),
        /** Ambient-occlusion data map. */
        OCCLUSION(TextureColorSpace.LINEAR),
        /** Emissive colour map. */
        EMISSIVE(TextureColorSpace.SRGB);

        private final TextureColorSpace colorSpace;

        /** Retains the required colour-space interpretation. */
        TextureRole(TextureColorSpace colorSpace) {
            this.colorSpace = colorSpace;
        }

        /** Returns the required colour-space interpretation. */
        private TextureColorSpace colorSpace() {
            return colorSpace;
        }
    }
}
