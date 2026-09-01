/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.impl.v2.MeshPrimitive;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.BufferViewModel;
import de.javagl.jgltf.model.ElementType;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.ImageModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v2.GltfModelCreatorV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.animation.AnimationTrack;
import io.github.glynch.jscene3d.animation.Interpolation;
import io.github.glynch.jscene3d.animation.MorphTargetKeyframeTrack;
import io.github.glynch.jscene3d.animation.QuaternionKeyframeTrack;
import io.github.glynch.jscene3d.animation.Vector3KeyframeTrack;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.gltf.internal.AccessorDecoder;
import io.github.glynch.jscene3d.gltf.internal.DracoDecoder;
import io.github.glynch.jscene3d.gltf.internal.DracoDecoder.DecodedPrimitive;
import io.github.glynch.jscene3d.gltf.internal.ImageDecoder;
import io.github.glynch.jscene3d.gltf.internal.ImageDecoder.DecodedImage;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Bone;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.Skeleton;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureCoordinateSet;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Performs one complete JglTF-to-JScene3D conversion without exposing parser types. */
final class GltfConverter {
    private static final String DRACO_EXTENSION = "KHR_draco_mesh_compression";

    private final Path source;
    private final GlTF gltf;
    private final GltfModel model;
    private final int defaultSceneIndex;
    private final List<BufferGeometry> geometries = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Texture> textures = new ArrayList<>();
    private final Map<MeshPrimitiveModel, BufferGeometry> geometryCache = new IdentityHashMap<>();
    private final Map<MaterialModel, StandardMaterial> materialCache = new IdentityHashMap<>();
    private final Map<TextureModel, EnumMap<TextureColorSpace, Texture>> textureCache = new IdentityHashMap<>();
    private final Map<ImageModel, DecodedImage> imageCache = new IdentityHashMap<>();
    private final Map<NodeModel, Object3D> convertedNodes = new IdentityHashMap<>();
    private final Map<NodeModel, List<Mesh>> convertedMeshesByNode = new IdentityHashMap<>();
    private final Map<SkinModel, Skeleton> skeletonCache = new IdentityHashMap<>();
    private final Map<MeshPrimitiveModel, MeshPrimitive> primitiveDefinitions = new IdentityHashMap<>();
    private final Map<MeshPrimitiveModel, List<String>> morphTargetNames = new IdentityHashMap<>();

    private @Nullable StandardMaterial defaultMaterial;

    /** Retains parsed model state for one isolated conversion. */
    private GltfConverter(Path source, GlTF gltf, GltfModel model, int defaultSceneIndex) {
        this.source = source;
        this.gltf = gltf;
        this.model = model;
        this.defaultSceneIndex = defaultSceneIndex;
        indexPrimitiveDefinitions();
    }

    /** Associates JglTF primitive models with their extension-bearing source definitions. */
    private void indexPrimitiveDefinitions() {
        var definitions = gltf.getMeshes();
        List<MeshModel> meshModels = model.getMeshModels();
        if (definitions == null || definitions.size() != meshModels.size()) {
            if (!meshModels.isEmpty()) {
                throw failure("Parsed mesh definitions do not match JglTF mesh models", null);
            }
            return;
        }
        for (int meshIndex = 0; meshIndex < meshModels.size(); meshIndex++) {
            List<MeshPrimitiveModel> primitiveModels = meshModels.get(meshIndex).getMeshPrimitiveModels();
            List<MeshPrimitive> primitiveDefinitionsForMesh =
                    definitions.get(meshIndex).getPrimitives();
            if (primitiveDefinitionsForMesh.size() != primitiveModels.size()) {
                throw failure("Parsed primitive definitions do not match JglTF primitive models", null);
            }
            for (int primitiveIndex = 0; primitiveIndex < primitiveModels.size(); primitiveIndex++) {
                primitiveDefinitions.put(
                        primitiveModels.get(primitiveIndex), primitiveDefinitionsForMesh.get(primitiveIndex));
                morphTargetNames.put(
                        primitiveModels.get(primitiveIndex),
                        targetNames(definitions.get(meshIndex).getExtras(), primitiveModels.get(primitiveIndex)));
            }
        }
    }

    /** Resolves optional exporter names or deterministic target-number fallbacks. */
    private static List<String> targetNames(Object extras, MeshPrimitiveModel primitive) {
        List<String> names = new ArrayList<>(primitive.getTargets().size());
        List<?> configured = extras instanceof Map<?, ?> values && values.get("targetNames") instanceof List<?> list
                ? list
                : List.of();
        for (int index = 0; index < primitive.getTargets().size(); index++) {
            Object configuredName = index < configured.size() ? configured.get(index) : null;
            names.add(configuredName instanceof String string && !string.isBlank() ? string : "Target " + (index + 1));
        }
        return List.copyOf(names);
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
        GltfConverter converter = new GltfConverter(source, gltf, GltfModelCreatorV2.create(assetV2), sceneIndex);
        return converter.convert();
    }

    /** Converts and transfers ownership, closing partial resources after any failure. */
    private LoadedGltf convert() {
        try {
            Scene scene = convertScene();
            List<AnimationClip> animations = convertAnimations();
            return new LoadedGltf(scene, animations, geometries, materials, textures);
        } catch (RuntimeException failure) {
            closePartialResources();
            throw failure;
        }
    }

    /** Rejects unsupported root features before JglTF constructs their specialized models. */
    private static void validateRootCapabilities(Path source, GlTF gltf) {
        List<String> requiredExtensions = gltf.getExtensionsRequired();
        if (requiredExtensions != null) {
            for (String extension : requiredExtensions) {
                if (!DRACO_EXTENSION.equals(extension)) {
                    throw new GltfLoadException(source, "Unsupported required extensions entry: " + extension, null);
                }
            }
        }
        rejectPresent(source, "embedded cameras", gltf.getCameras());
    }

    /** Rejects one nullable root collection when it contains entries. */
    private static void rejectPresent(Path source, String capability, @Nullable List<?> values) {
        if (values != null && !values.isEmpty()) {
            throw new GltfLoadException(source, "Unsupported glTF capability: " + capability, null);
        }
    }

    /** Converts nodes in stable source order before wiring hierarchy, skins, and selected roots. */
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
        Map<NodeModel, Boolean> jointNodes = new IdentityHashMap<>();
        for (SkinModel skin : model.getSkinModels()) {
            skin.getJoints().forEach(joint -> jointNodes.put(joint, Boolean.TRUE));
        }
        for (NodeModel node : model.getNodeModels()) {
            Object3D converted = jointNodes.containsKey(node) ? new Bone() : new Group();
            applyTransform(node, converted);
            convertedNodes.put(node, converted);
        }
        for (NodeModel node : model.getNodeModels()) {
            addNodeMeshes(node, Objects.requireNonNull(convertedNodes.get(node), "converted node"));
        }
        for (Map.Entry<NodeModel, Object3D> entry : convertedNodes.entrySet()) {
            for (NodeModel child : entry.getKey().getChildren()) {
                Object3D convertedChild = convertedNodes.get(child);
                if (convertedChild == null) {
                    throw failure("Scene hierarchy contains an unknown child", null);
                }
                entry.getValue().add(convertedChild);
            }
        }
        for (NodeModel root : roots) {
            scene.add(Objects.requireNonNull(convertedNodes.get(root), "converted root"));
        }
        return scene;
    }

    /** Converts source animation channels into renderer-independent typed transform tracks. */
    private List<AnimationClip> convertAnimations() {
        List<AnimationModel> sourceAnimations = model.getAnimationModels();
        List<AnimationClip> animations = new ArrayList<>(sourceAnimations.size());
        for (int animationIndex = 0; animationIndex < sourceAnimations.size(); animationIndex++) {
            AnimationModel sourceAnimation = sourceAnimations.get(animationIndex);
            List<AnimationTrack> tracks =
                    new ArrayList<>(sourceAnimation.getChannels().size());
            for (AnimationModel.Channel channel : sourceAnimation.getChannels()) {
                tracks.addAll(convertAnimationChannel(channel));
            }
            String name = sourceAnimation.getName();
            if (name == null || name.isBlank()) {
                name = "Animation " + (animationIndex + 1);
            }
            animations.add(new AnimationClip(name, tracks));
        }
        return List.copyOf(animations);
    }

    /** Converts one channel after resolving its target in the selected scene. */
    private List<AnimationTrack> convertAnimationChannel(AnimationModel.Channel channel) {
        NodeModel sourceTarget = channel.getNodeModel();
        Object3D target = convertedNodes.get(sourceTarget);
        if (target == null) {
            throw failure("Animation targets a node outside the selected scene", null);
        }
        AnimationModel.Sampler sampler = channel.getSampler();
        AccessorModel input = sampler.getInput();
        AccessorModel output = sampler.getOutput();
        requireAnimationFloatAccessor(input, "animation input");
        requireAnimationFloatAccessor(output, "animation output");
        float[] times = AccessorDecoder.floats(input, ElementType.SCALAR, "animation input");
        Interpolation interpolation = interpolation(sampler.getInterpolation());
        return switch (channel.getPath()) {
            case "translation" -> List.of(positionTrack(target, times, output, interpolation));
            case "rotation" -> List.of(rotationTrack(target, times, output, interpolation));
            case "scale" -> List.of(scaleTrack(target, times, output, interpolation));
            case "weights" -> morphTracks(sourceTarget, times, output, interpolation);
            default -> throw unsupportedFailure("animation target path " + channel.getPath());
        };
    }

    /** Creates one weight track for every primitive mesh attached to the animated source node. */
    private List<AnimationTrack> morphTracks(
            NodeModel sourceTarget, float[] times, AccessorModel output, Interpolation interpolation) {
        List<Mesh> meshes = convertedMeshesByNode.getOrDefault(sourceTarget, List.of());
        if (meshes.isEmpty()) {
            throw failure("Morph-weight animation target has no converted mesh primitives", null);
        }
        float[] values = AccessorDecoder.floats(output, ElementType.SCALAR, "animation morph-weight output");
        List<AnimationTrack> tracks = new ArrayList<>(meshes.size());
        for (Mesh mesh : meshes) {
            tracks.add(MorphTargetKeyframeTrack.influences(mesh, times, values, interpolation));
        }
        return tracks;
    }

    /** Creates one position track while preserving source timestamp discontinuities. */
    private AnimationTrack positionTrack(
            Object3D target, float[] times, AccessorModel output, Interpolation interpolation) {
        float[] values = AccessorDecoder.floats(output, ElementType.VEC3, "animation translation output");
        return Vector3KeyframeTrack.position(target, times, values, interpolation);
    }

    /** Creates one rotation track while preserving source timestamp discontinuities. */
    private AnimationTrack rotationTrack(
            Object3D target, float[] times, AccessorModel output, Interpolation interpolation) {
        float[] values = AccessorDecoder.floats(output, ElementType.VEC4, "animation rotation output");
        return QuaternionKeyframeTrack.rotation(target, times, values, interpolation);
    }

    /** Creates one scale track while preserving source timestamp discontinuities. */
    private AnimationTrack scaleTrack(
            Object3D target, float[] times, AccessorModel output, Interpolation interpolation) {
        float[] values = AccessorDecoder.floats(output, ElementType.VEC3, "animation scale output");
        return Vector3KeyframeTrack.scale(target, times, values, interpolation);
    }

    /** Maps JglTF's interpolation vocabulary into the public animation vocabulary. */
    private static Interpolation interpolation(AnimationModel.@Nullable Interpolation interpolation) {
        if (interpolation == null) {
            return Interpolation.LINEAR;
        }
        return switch (interpolation) {
            case STEP -> Interpolation.STEP;
            case LINEAR -> Interpolation.LINEAR;
            case CUBICSPLINE -> Interpolation.CUBIC_SPLINE;
        };
    }

    /** Requires animation scalar storage mandated by core glTF 2.0. */
    private void requireAnimationFloatAccessor(AccessorModel accessor, String semantic) {
        if (accessor.getComponentType() != GltfConstants.GL_FLOAT || accessor.isNormalized()) {
            throw failure(semantic + " must contain non-normalized floating-point values", null);
        }
    }

    /** Creates an unsupported-capability failure for use inside switch expressions. */
    private GltfLoadException unsupportedFailure(String capability) {
        return failure("Unsupported glTF capability: " + capability, null);
    }

    /** Converts and attaches one node's mesh primitives after its skeleton hierarchy exists. */
    private void addNodeMeshes(NodeModel node, Object3D target) {
        if (node.getCameraModel() != null) {
            unsupported("camera node");
        }
        SkinModel skinModel = node.getSkinModel();
        Skeleton skeleton = skinModel == null ? null : skeletonFor(skinModel);
        List<Mesh> convertedMeshes = new ArrayList<>();
        for (MeshModel meshModel : node.getMeshModels()) {
            for (MeshPrimitiveModel primitive : meshModel.getMeshPrimitiveModels()) {
                Mesh converted = convertPrimitive(primitive, skeleton);
                applyMorphWeights(converted, node.getWeights(), meshModel.getWeights());
                target.add(converted);
                convertedMeshes.add(converted);
            }
        }
        convertedMeshesByNode.put(node, List.copyOf(convertedMeshes));
    }

    /** Applies node overrides or mesh defaults to one converted primitive. */
    private void applyMorphWeights(Mesh mesh, float @Nullable [] nodeWeights, float @Nullable [] meshWeights) {
        float[] selected = nodeWeights == null ? meshWeights : nodeWeights;
        if (selected == null) {
            return;
        }
        if (selected.length != mesh.morphTargetCount()) {
            throw failure(
                    "Morph weight count must equal primitive target count: "
                            + selected.length
                            + " != "
                            + mesh.morphTargetCount(),
                    null);
        }
        mesh.setMorphTargetInfluences(selected);
    }

    /** Returns or creates one skeleton using the source skin's stable joint ordering. */
    private Skeleton skeletonFor(SkinModel skinModel) {
        Skeleton existing = skeletonCache.get(skinModel);
        if (existing != null) {
            return existing;
        }
        List<Bone> bones = new ArrayList<>(skinModel.getJoints().size());
        List<Matrix4f> inverseBindMatrices =
                new ArrayList<>(skinModel.getJoints().size());
        for (int index = 0; index < skinModel.getJoints().size(); index++) {
            Object3D joint = convertedNodes.get(skinModel.getJoints().get(index));
            if (!(joint instanceof Bone bone)) {
                throw failure("Skin joint was not converted as a Bone", null);
            }
            bones.add(bone);
            float[] values = skinModel.getInverseBindMatrices() == null
                    ? new Matrix4f().get(new float[16])
                    : skinModel.getInverseBindMatrix(index, new float[16]);
            inverseBindMatrices.add(new Matrix4f().set(values));
        }
        Skeleton skeleton = new Skeleton(bones, inverseBindMatrices);
        skeletonCache.put(skinModel, skeleton);
        return skeleton;
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
    private Mesh convertPrimitive(MeshPrimitiveModel primitive, @Nullable Skeleton skeleton) {
        if (primitive.getMode() != GltfConstants.GL_TRIANGLES) {
            unsupported("non-triangle primitive mode " + primitive.getMode());
        }
        BufferGeometry geometry = geometryCache.computeIfAbsent(primitive, this::createGeometry);
        StandardMaterial material = materialFor(primitive.getMaterialModel());
        if (primitive.getAttributes().containsKey("COLOR_0")) {
            material.setUsesVertexColors(true);
        }
        return skeleton == null ? new Mesh(geometry, material) : new SkinnedMesh(geometry, material, skeleton);
    }

    /** Creates geometry from renderer-supported glTF attributes. */
    private BufferGeometry createGeometry(MeshPrimitiveModel primitive) {
        Map<String, AccessorModel> attributes = primitive.getAttributes();
        boolean hasJoints = attributes.containsKey("JOINTS_0");
        boolean hasWeights = attributes.containsKey("WEIGHTS_0");
        if (hasJoints != hasWeights) {
            throw failure("Primitive must provide JOINTS_0 and WEIGHTS_0 together", null);
        }
        AccessorModel position = attributes.get("POSITION");
        if (position == null) {
            throw failure("Triangle primitive is missing POSITION", null);
        }
        @Nullable DecodedPrimitive compressed = decodeCompressedPrimitive(primitive, attributes);
        BufferGeometry geometry = new BufferGeometry();
        geometries.add(geometry);
        if (compressed == null) {
            setUncompressedAttributes(geometry, primitive, attributes, position);
        } else {
            setCompressedAttributes(geometry, attributes, compressed);
        }
        if (geometry.attribute(BufferGeometry.NORMAL) == null) {
            geometry.computeVertexNormals();
        }
        addMorphTargets(geometry, primitive);
        return geometry;
    }

    /** Decodes ordered relative position and normal displacement targets. */
    private void addMorphTargets(BufferGeometry geometry, MeshPrimitiveModel primitive) {
        List<String> names = morphTargetNames.getOrDefault(primitive, List.of());
        for (int targetIndex = 0; targetIndex < primitive.getTargets().size(); targetIndex++) {
            Map<String, AccessorModel> attributes = primitive.getTargets().get(targetIndex);
            if (attributes.containsKey("TANGENT")) {
                unsupported("morph tangent targets");
            }
            AccessorModel positions = attributes.get("POSITION");
            if (positions == null) {
                throw unsupportedFailure("morph targets without POSITION");
            }
            requireMorphAccessor(positions, "morph POSITION");
            BufferAttribute positionDeltas =
                    BufferAttribute.of(AccessorDecoder.floats(positions, ElementType.VEC3, "morph POSITION"), 3);
            AccessorModel normals = attributes.get("NORMAL");
            BufferAttribute normalDeltas = null;
            if (normals != null) {
                requireMorphAccessor(normals, "morph NORMAL");
                normalDeltas = BufferAttribute.of(AccessorDecoder.floats(normals, ElementType.VEC3, "morph NORMAL"), 3);
            }
            String name = targetIndex < names.size() ? names.get(targetIndex) : "Target " + (targetIndex + 1);
            geometry.addMorphTarget(new MorphTarget(name, positionDeltas, normalDeltas));
        }
    }

    /** Requires floating-point VEC3 storage for one glTF displacement target. */
    private void requireMorphAccessor(AccessorModel accessor, String semantic) {
        if (accessor.getComponentType() != GltfConstants.GL_FLOAT
                || accessor.isNormalized()
                || accessor.getElementType() != ElementType.VEC3) {
            throw failure(semantic + " must contain non-normalized floating-point VEC3 values", null);
        }
    }

    /** Decodes a compressed primitive when its source definition selects the Draco extension. */
    private @Nullable DecodedPrimitive decodeCompressedPrimitive(
            MeshPrimitiveModel primitive, Map<String, AccessorModel> accessors) {
        MeshPrimitive definition = primitiveDefinitions.get(primitive);
        if (definition == null || definition.getExtensions() == null) {
            return null;
        }
        Object extension = definition.getExtensions().get(DRACO_EXTENSION);
        if (extension == null) {
            return null;
        }
        if (!(extension instanceof Map<?, ?> descriptor)) {
            throw failure("Draco extension descriptor must be an object", null);
        }
        int bufferViewIndex = integerProperty(descriptor, "bufferView", "Draco extension");
        if (bufferViewIndex < 0
                || bufferViewIndex >= model.getBufferViewModels().size()) {
            throw failure("Draco bufferView index is outside the buffer-view list: " + bufferViewIndex, null);
        }
        Object rawAttributes = descriptor.get("attributes");
        if (!(rawAttributes instanceof Map<?, ?> attributeDescriptor)) {
            throw failure("Draco extension attributes must be an object", null);
        }
        Map<String, Integer> attributeIds = new LinkedHashMap<>();
        Map<String, Integer> componentCounts = new LinkedHashMap<>();
        Map<String, float[]> attributeMinimums = new LinkedHashMap<>();
        for (String semantic :
                List.of("POSITION", "NORMAL", "TEXCOORD_0", "TEXCOORD_1", "COLOR_0", "JOINTS_0", "WEIGHTS_0")) {
            AccessorModel accessor = accessors.get(semantic);
            if (accessor == null) {
                continue;
            }
            Object rawId = attributeDescriptor.get(semantic);
            if (!(rawId instanceof Number number)) {
                throw failure("Draco extension does not map required semantic " + semantic, null);
            }
            attributeIds.put(semantic, number.intValue());
            componentCounts.put(semantic, accessor.getElementType().getNumComponents());
            float[] minimum = sourceAttributeMinimum(definition, semantic);
            if (minimum != null) {
                attributeMinimums.put(semantic, minimum);
            }
        }
        BufferViewModel bufferView = model.getBufferViewModels().get(bufferViewIndex);
        try {
            return DracoDecoder.decode(
                    bufferView.getBufferViewData(), attributeIds, componentCounts, attributeMinimums);
        } catch (IllegalArgumentException exception) {
            throw failure("Failed to decode Draco primitive", exception);
        }
    }

    /** Returns bounds retained by the source accessor rather than JglTF's compressed placeholder. */
    private float @Nullable [] sourceAttributeMinimum(MeshPrimitive primitive, String semantic) {
        Integer accessorIndex = primitive.getAttributes().get(semantic);
        if (accessorIndex == null
                || accessorIndex < 0
                || accessorIndex >= gltf.getAccessors().size()) {
            throw failure("Draco primitive has an invalid source accessor for " + semantic, null);
        }
        Number[] minimum = gltf.getAccessors().get(accessorIndex).getMin();
        return minimum == null ? null : floats(minimum);
    }

    /** Converts accessor bounds into primitive storage used by Draco correction. */
    private static float[] floats(Number[] values) {
        float[] converted = new float[values.length];
        for (int index = 0; index < values.length; index++) {
            converted[index] = values[index].floatValue();
        }
        return converted;
    }

    /** Reads one required integral numeric extension property. */
    private int integerProperty(Map<?, ?> values, String name, String label) {
        Object value = values.get(name);
        if (!(value instanceof Number number)) {
            throw failure(label + " requires numeric " + name, null);
        }
        int integer = number.intValue();
        if (number.doubleValue() != integer) {
            throw failure(label + ' ' + name + " must be an integer: " + number, null);
        }
        return integer;
    }

    /** Populates one ordinary uncompressed primitive. */
    private static void setUncompressedAttributes(
            BufferGeometry geometry,
            MeshPrimitiveModel primitive,
            Map<String, AccessorModel> attributes,
            AccessorModel position) {
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(AccessorDecoder.floats(position, ElementType.VEC3, "POSITION"), 3));
        setOptionalAttribute(geometry, attributes, "NORMAL", BufferGeometry.NORMAL, 3);
        setOptionalAttribute(geometry, attributes, "TEXCOORD_0", BufferGeometry.UV, 2);
        setOptionalAttribute(geometry, attributes, "TEXCOORD_1", BufferGeometry.UV1, 2);
        setOptionalAttribute(geometry, attributes, "JOINTS_0", BufferGeometry.JOINTS, 4);
        setOptionalAttribute(geometry, attributes, "WEIGHTS_0", BufferGeometry.WEIGHTS, 4);
        setOptionalColor(geometry, attributes.get("COLOR_0"));
        AccessorModel indices = primitive.getIndices();
        if (indices != null) {
            geometry.setIndex(IndexBuffer.of(AccessorDecoder.indices(indices)));
        }
    }

    /** Populates one primitive from already decoded Draco point data. */
    private void setCompressedAttributes(
            BufferGeometry geometry, Map<String, AccessorModel> accessors, DecodedPrimitive compressed) {
        Map<String, float[]> values = compressed.attributes();
        setCompressedAttribute(
                geometry, accessors, values, "POSITION", BufferGeometry.POSITION, 3, compressed.vertexCount());
        setCompressedAttribute(
                geometry, accessors, values, "NORMAL", BufferGeometry.NORMAL, 3, compressed.vertexCount());
        setCompressedAttribute(
                geometry, accessors, values, "TEXCOORD_0", BufferGeometry.UV, 2, compressed.vertexCount());
        setCompressedAttribute(
                geometry, accessors, values, "TEXCOORD_1", BufferGeometry.UV1, 2, compressed.vertexCount());
        setCompressedAttribute(
                geometry, accessors, values, "JOINTS_0", BufferGeometry.JOINTS, 4, compressed.vertexCount());
        setCompressedAttribute(
                geometry, accessors, values, "WEIGHTS_0", BufferGeometry.WEIGHTS, 4, compressed.vertexCount());
        AccessorModel color = accessors.get("COLOR_0");
        if (color != null) {
            int components = color.getElementType().getNumComponents();
            setCompressedAttribute(
                    geometry, accessors, values, "COLOR_0", BufferGeometry.COLOR, components, compressed.vertexCount());
        }
        geometry.setIndex(IndexBuffer.of(compressed.indices()));
    }

    /** Adds one decoded Draco semantic after checking its accessor metadata. */
    private void setCompressedAttribute(
            BufferGeometry geometry,
            Map<String, AccessorModel> accessors,
            Map<String, float[]> values,
            String sourceName,
            String targetName,
            int components,
            int vertexCount) {
        AccessorModel accessor = accessors.get(sourceName);
        if (accessor == null) {
            return;
        }
        if (accessor.getCount() != vertexCount) {
            throw failure(
                    sourceName + " accessor count differs from decoded Draco point count: " + accessor.getCount()
                            + " != " + vertexCount,
                    null);
        }
        float[] decoded = values.get(sourceName);
        if (decoded == null) {
            throw failure("Draco extension does not map required semantic " + sourceName, null);
        }
        geometry.setAttribute(targetName, BufferAttribute.of(decoded, components));
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
            ElementType type =
                    switch (components) {
                        case 2 -> ElementType.VEC2;
                        case 3 -> ElementType.VEC3;
                        case 4 -> ElementType.VEC4;
                        default ->
                            throw new IllegalArgumentException("unsupported attribute component count: " + components);
                    };
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
        if (texcoord != null && texcoord != 0 && texcoord != 1) {
            unsupported("texture coordinate set " + texcoord);
        }
        TextureCoordinateSet coordinateSet =
                texcoord != null && texcoord == 1 ? TextureCoordinateSet.SECONDARY : TextureCoordinateSet.PRIMARY;
        Texture texture = textureFor(textureModel, role.colorSpace());
        switch (role) {
            case COLOR -> {
                material.setColorMap(texture);
                material.setColorMapCoordinateSet(coordinateSet);
            }
            case METALLIC_ROUGHNESS -> {
                material.setMetalnessRoughnessMap(texture);
                material.setMetalnessRoughnessMapCoordinateSet(coordinateSet);
            }
            case NORMAL -> {
                material.setNormalMap(texture);
                material.setNormalMapCoordinateSet(coordinateSet);
            }
            case OCCLUSION -> {
                material.setOcclusionMap(texture);
                material.setOcclusionMapCoordinateSet(coordinateSet);
            }
            case EMISSIVE -> {
                material.setEmissiveMap(texture);
                material.setEmissiveMapCoordinateSet(coordinateSet);
            }
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
