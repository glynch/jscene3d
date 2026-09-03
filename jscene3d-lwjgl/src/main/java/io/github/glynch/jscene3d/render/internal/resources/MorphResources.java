/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.objects.Mesh;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Shared lifecycle and binding boundary for renderer-owned morph-target resources. */
public final class MorphResources implements AutoCloseable {
    /** Texture unit containing geometry target deltas. */
    public static final int TARGET_TEXTURE_UNIT = 16;

    /** Texture unit containing ordinary or per-instance weights. */
    public static final int WEIGHT_TEXTURE_UNIT = 17;

    private final Map<BufferGeometry, MorphTargetResource> targets;
    private final Map<Mesh, MorphWeightResource> weights;
    private final Set<BufferGeometry> activeTargets;
    private final Set<Mesh> activeWeights;
    private final DefaultMorphBuffers defaults;

    /** Creates an empty unrealized cache. */
    public MorphResources() {
        targets = new IdentityHashMap<>();
        weights = new IdentityHashMap<>();
        activeTargets = Collections.newSetFromMap(new IdentityHashMap<>());
        activeWeights = Collections.newSetFromMap(new IdentityHashMap<>());
        defaults = new DefaultMorphBuffers();
    }

    /** Starts a frame in which bindings establish the retained active set. */
    public void beginFrame() {
        activeTargets.clear();
        activeWeights.clear();
    }

    /**
     * Synchronizes and binds all morph data consumed by one mesh draw.
     *
     * @param mesh mesh about to be drawn
     * @return shader layout and upload activity for the draw
     */
    public Binding bind(Mesh mesh) {
        if (mesh.morphTargetCount() == 0) {
            defaults.bind();
            return Binding.DISABLED;
        }
        BufferGeometry geometry = mesh.geometry();
        MorphTargetResource targetResource = targets.computeIfAbsent(geometry, ignored -> new MorphTargetResource());
        MorphWeightResource weightResource = weights.computeIfAbsent(mesh, ignored -> new MorphWeightResource());
        activeTargets.add(geometry);
        activeWeights.add(mesh);
        MorphTargetResource.UploadResult targetUpload =
                targetResource.synchronizeAndBind(geometry, TARGET_TEXTURE_UNIT);
        MorphWeightResource.UploadResult weightUpload = weightResource.synchronizeAndBind(mesh, WEIGHT_TEXTURE_UNIT);
        boolean instanceWeights =
                mesh instanceof InstancedMesh instancedMesh && instancedMesh.hasInstanceMorphTargetInfluences();
        return new Binding(
                true,
                mesh.morphTargetCount(),
                geometry.attribute(BufferGeometry.POSITION).count(),
                instanceWeights,
                targetUpload.count() + weightUpload.count(),
                targetUpload.byteCount() + weightUpload.byteCount());
    }

    /** Releases resources that were not used by either render pass this frame. */
    public void finishFrame() {
        releaseInactive(targets, activeTargets);
        releaseInactive(weights, activeWeights);
    }

    /**
     * Returns the combined active geometry- and weight-resource count.
     *
     * @return number of retained morph resources
     */
    public int resourceCount() {
        return targets.size() + weights.size();
    }

    /** Releases every retained resource. */
    @Override
    public void close() {
        targets.values().forEach(MorphTargetResource::close);
        weights.values().forEach(MorphWeightResource::close);
        targets.clear();
        weights.clear();
        activeTargets.clear();
        activeWeights.clear();
        defaults.close();
    }

    /** Removes entries whose identity keys are absent from a frame's active set. */
    private static <K, V extends AutoCloseable> void releaseInactive(Map<K, V> resources, Set<K> active) {
        Iterator<Map.Entry<K, V>> iterator = resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            if (!active.contains(entry.getKey())) {
                closeUnchecked(entry.getValue());
                iterator.remove();
            }
        }
    }

    /** Closes a renderer resource whose implementations do not throw checked exceptions. */
    private static void closeUnchecked(AutoCloseable resource) {
        try {
            resource.close();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unexpected checked resource-close failure", exception);
        }
    }

    /**
     * Complete shader layout and upload work for one draw.
     *
     * @param enabled whether morph deformation is enabled
     * @param targetCount number of morph targets
     * @param vertexCount number of vertices in each target
     * @param instanceWeights whether weights vary by instance
     * @param uploadCount number of OpenGL upload operations performed
     * @param uploadedBytes number of bytes uploaded
     */
    public record Binding(
            boolean enabled,
            int targetCount,
            int vertexCount,
            boolean instanceWeights,
            int uploadCount,
            long uploadedBytes) {
        private static final Binding DISABLED = new Binding(false, 0, 0, false, 0, 0L);
    }
}
