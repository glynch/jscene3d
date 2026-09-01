/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

/**
 * A triangular scene object that binds one buffer geometry to one material.
 *
 * <p>The class is extensible only for scene-object specializations that preserve this resource
 * binding contract, such as {@link SkinnedMesh}.
 */
public class Mesh extends RenderableObject {
    private BufferGeometry geometry;
    private Material material;
    private float[] morphTargetInfluences;
    private final List<BufferAttribute> cachedBoundsMorphPositions = new ArrayList<>();
    private final List<Long> cachedBoundsMorphPositionVersions = new ArrayList<>();
    private @Nullable BufferGeometry cachedBoundsGeometry;
    private @Nullable BufferAttribute cachedBoundsPositions;
    private @Nullable MeshBounds cachedLocalBounds;
    private long cachedBoundsGeometryVersion = -1L;
    private long cachedBoundsPositionVersion = -1L;
    private long cachedBoundsInfluenceVersion = -1L;
    private long boundsVersion;
    private long morphTargetInfluenceVersion;
    private boolean shadowCastingEnabled;
    private boolean shadowReceivingEnabled;
    private @Nullable RenderCallback beforeShadowRenderCallback;
    private @Nullable RenderCallback afterShadowRenderCallback;

    /**
     * Creates a mesh retaining shared geometry and material references.
     *
     * @param geometry open triangle geometry
     * @param material open surface material
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is already closed
     */
    public Mesh(BufferGeometry geometry, Material material) {
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
        this.material = Preconditions.requireOpen(material, "material");
        morphTargetInfluences = new float[this.geometry.morphTargets().size()];
    }

    /**
     * Returns the shared geometry.
     *
     * @return the retained geometry
     * @throws IllegalStateException if the retained geometry is closed
     */
    public BufferGeometry geometry() {
        if (geometry.isClosed()) {
            throw new IllegalStateException("Mesh geometry is closed");
        }
        return geometry;
    }

    /**
     * Replaces the shared geometry reference.
     *
     * @param geometry open triangle geometry
     * @throws NullPointerException if {@code geometry} is {@code null}
     * @throws IllegalArgumentException if {@code geometry} is closed
     */
    public void setGeometry(BufferGeometry geometry) {
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
        morphTargetInfluences = new float[this.geometry.morphTargets().size()];
        morphTargetInfluenceVersion++;
        onMorphTargetShapeChanged();
    }

    /**
     * Returns the number of ordered morph influences required by the current geometry.
     *
     * @return target count, possibly zero
     */
    public final int morphTargetCount() {
        requireCurrentMorphTargetShape();
        return morphTargetInfluences.length;
    }

    /**
     * Returns one morph-target influence.
     *
     * @param targetIndex zero-based target index
     * @return finite influence, initially zero
     * @throws IndexOutOfBoundsException if the index is outside the current target range
     * @throws IllegalStateException if targets changed after this mesh bound its geometry
     */
    public final float morphTargetInfluence(int targetIndex) {
        requireCurrentMorphTargetShape();
        return morphTargetInfluences[Objects.checkIndex(targetIndex, morphTargetInfluences.length)];
    }

    /**
     * Changes one morph-target influence.
     *
     * <p>Negative and greater-than-one influences are supported. On an {@link InstancedMesh}, this
     * batch-level operation changes the default and every allocated per-instance value for the
     * selected target.
     *
     * @param targetIndex zero-based target index
     * @param influence finite replacement influence
     * @throws IndexOutOfBoundsException if the index is outside the current target range
     * @throws IllegalArgumentException if the influence is not finite
     * @throws IllegalStateException if targets changed after this mesh bound its geometry
     */
    public final void setMorphTargetInfluence(int targetIndex, float influence) {
        requireCurrentMorphTargetShape();
        int validIndex = Objects.checkIndex(targetIndex, morphTargetInfluences.length);
        float validInfluence = Preconditions.requireFinite(influence, "influence");
        if (morphTargetInfluences[validIndex] != validInfluence) {
            morphTargetInfluences[validIndex] = validInfluence;
            morphTargetInfluenceVersion++;
            onMorphTargetInfluenceChanged(validIndex, validInfluence);
        }
    }

    /**
     * Replaces every current morph-target influence from one exact-length array.
     *
     * @param influences finite influences in geometry target order
     * @throws NullPointerException if {@code influences} is {@code null}
     * @throws IllegalArgumentException if the length differs or a value is not finite
     * @throws IllegalStateException if targets changed after this mesh bound its geometry
     */
    public final void setMorphTargetInfluences(float[] influences) {
        requireCurrentMorphTargetShape();
        float[] validInfluences = Objects.requireNonNull(influences, "influences");
        if (validInfluences.length != morphTargetInfluences.length) {
            throw new IllegalArgumentException("influences length must equal morph target count: "
                    + validInfluences.length
                    + " != "
                    + morphTargetInfluences.length);
        }
        float[] copy = validInfluences.clone();
        for (int index = 0; index < copy.length; index++) {
            copy[index] = Preconditions.requireFinite(copy[index], "influences[" + index + "]");
        }
        if (!Arrays.equals(morphTargetInfluences, copy)) {
            morphTargetInfluences = copy;
            morphTargetInfluenceVersion++;
            onMorphTargetInfluencesChanged();
        }
    }

    /**
     * Returns the index of a named geometry morph target.
     *
     * @param name target name
     * @return target index, or an empty value when absent
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public final OptionalInt morphTargetIndex(String name) {
        String validName = Objects.requireNonNull(name, "name");
        var targets = geometry().morphTargets();
        for (int index = 0; index < targets.size(); index++) {
            if (targets.get(index).name().equals(validName)) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Returns the version observed by renderer-owned weight resources.
     *
     * @return monotonically increasing version
     */
    public final long morphTargetInfluenceVersion() {
        requireCurrentMorphTargetShape();
        return morphTargetInfluenceVersion;
    }

    /**
     * Copies current morph-target influences into an exact-length destination.
     *
     * @param destination destination whose length equals {@link #morphTargetCount()}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalArgumentException if its length differs
     */
    public final void copyMorphTargetInfluencesTo(float[] destination) {
        requireCurrentMorphTargetShape();
        float[] validDestination = Objects.requireNonNull(destination, "destination");
        if (validDestination.length != morphTargetInfluences.length) {
            throw new IllegalArgumentException("destination length must equal morph target count: "
                    + validDestination.length
                    + " != "
                    + morphTargetInfluences.length);
        }
        System.arraycopy(morphTargetInfluences, 0, validDestination, 0, morphTargetInfluences.length);
    }

    /**
     * Returns cached exact local-space bounds for the current morph influences.
     *
     * <p>Base positions, morph-position deltas, geometry structure, and influences are all
     * versioned inputs. Mutating any of them invalidates the cache automatically.
     *
     * @return current local-space box
     */
    public BoundingBox boundingBox() {
        return currentLocalBounds().box();
    }

    /**
     * Returns cached exact local-space spherical bounds for the current morph influences.
     *
     * @return current local-space sphere
     */
    public @Nullable BoundingSphere boundingSphere() {
        return currentLocalBounds().sphere();
    }

    /** Returns a monotonically increasing revision for current geometry and shared morph bounds. */
    final long boundsVersion() {
        currentLocalBounds();
        return boundsVersion;
    }

    /** Computes exact local bounds using one instance's independent morph influences. */
    final MeshBounds computeBoundsAt(int instanceIndex) {
        return MeshBounds.compute(this, instanceIndex);
    }

    /** Refreshes and returns exact local bounds when any contributing input changed. */
    private MeshBounds currentLocalBounds() {
        if (!hasCurrentBoundsInputs()) {
            cachedLocalBounds = MeshBounds.compute(this, -1);
            captureBoundsInputs();
            boundsVersion++;
        }
        return Objects.requireNonNull(cachedLocalBounds, "cachedLocalBounds");
    }

    /** Returns whether every identity and version contributing to cached bounds still matches. */
    private boolean hasCurrentBoundsInputs() {
        BufferGeometry currentGeometry = geometry();
        BufferAttribute positions = currentGeometry.attribute(BufferGeometry.POSITION);
        List<MorphTarget> targets = currentGeometry.morphTargets();
        if (cachedLocalBounds == null
                || cachedBoundsGeometry != currentGeometry
                || cachedBoundsGeometryVersion != currentGeometry.version()
                || cachedBoundsPositions != positions
                || positions == null
                || cachedBoundsPositionVersion != positions.version()
                || cachedBoundsInfluenceVersion != morphTargetInfluenceVersion
                || cachedBoundsMorphPositions.size() != targets.size()) {
            return false;
        }
        for (int index = 0; index < targets.size(); index++) {
            BufferAttribute targetPositions = targets.get(index).positions();
            if (cachedBoundsMorphPositions.get(index) != targetPositions
                    || cachedBoundsMorphPositionVersions.get(index) != targetPositions.version()) {
                return false;
            }
        }
        return true;
    }

    /** Captures every bounds input after a complete calculation. */
    private void captureBoundsInputs() {
        BufferGeometry currentGeometry = geometry();
        BufferAttribute positions =
                Objects.requireNonNull(currentGeometry.attribute(BufferGeometry.POSITION), "position attribute");
        cachedBoundsGeometry = currentGeometry;
        cachedBoundsGeometryVersion = currentGeometry.version();
        cachedBoundsPositions = positions;
        cachedBoundsPositionVersion = positions.version();
        cachedBoundsInfluenceVersion = morphTargetInfluenceVersion;
        cachedBoundsMorphPositions.clear();
        cachedBoundsMorphPositionVersions.clear();
        for (MorphTarget target : currentGeometry.morphTargets()) {
            BufferAttribute targetPositions = target.positions();
            cachedBoundsMorphPositions.add(targetPositions);
            cachedBoundsMorphPositionVersions.add(targetPositions.version());
        }
    }

    /**
     * Allows specializations to mirror one changed batch-level influence.
     *
     * @param targetIndex changed target index
     * @param influence replacement influence
     */
    protected void onMorphTargetInfluenceChanged(int targetIndex, float influence) {
        // Ordinary meshes need no additional storage.
    }

    /** Allows specializations to mirror a complete batch-level influence replacement. */
    protected void onMorphTargetInfluencesChanged() {
        // Ordinary meshes need no additional storage.
    }

    /** Allows specializations to discard shape-dependent storage after geometry replacement. */
    protected void onMorphTargetShapeChanged() {
        // Ordinary meshes keep only the freshly allocated shared vector.
    }

    /** Rejects structural target mutation after a mesh has established its weight vector. */
    private void requireCurrentMorphTargetShape() {
        int currentCount = geometry().morphTargets().size();
        if (currentCount != morphTargetInfluences.length) {
            throw new IllegalStateException("Geometry morph targets changed after binding; call setGeometry to reset "
                    + "mesh influences: "
                    + currentCount
                    + " != "
                    + morphTargetInfluences.length);
        }
    }

    /**
     * Returns the shared material.
     *
     * @return the retained material
     * @throws IllegalStateException if the retained material is closed
     */
    public Material material() {
        if (material.isClosed()) {
            throw new IllegalStateException("Mesh material is closed");
        }
        return material;
    }

    /**
     * Replaces the shared material reference.
     *
     * @param material open surface material
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     */
    public void setMaterial(Material material) {
        this.material = Preconditions.requireOpen(material, "material");
    }

    /**
     * Returns whether this mesh participates in shadow-map depth passes.
     *
     * @return {@code false} by default
     */
    public boolean isShadowCastingEnabled() {
        return shadowCastingEnabled;
    }

    /**
     * Changes whether this mesh participates in shadow-map depth passes.
     *
     * @param enabled whether this mesh casts shadows from shadow-enabled lights
     */
    public void setShadowCastingEnabled(boolean enabled) {
        shadowCastingEnabled = enabled;
    }

    /**
     * Returns whether lit materials on this mesh sample generated shadow maps.
     *
     * @return {@code false} by default
     */
    public boolean isShadowReceivingEnabled() {
        return shadowReceivingEnabled;
    }

    /**
     * Changes whether lit materials on this mesh sample generated shadow maps.
     *
     * @param enabled whether this mesh receives shadows from shadow-enabled lights
     */
    public void setShadowReceivingEnabled(boolean enabled) {
        shadowReceivingEnabled = enabled;
    }

    /**
     * Returns the callback invoked immediately before each selected shadow-depth draw.
     *
     * @return configured callback, or an empty value
     */
    public final Optional<RenderCallback> beforeShadowRenderCallback() {
        return Optional.ofNullable(beforeShadowRenderCallback);
    }

    /**
     * Replaces the callback invoked immediately before each selected shadow-depth draw.
     *
     * <p>Directional and spot lights draw a caster once per shadow map. Point lights draw it once
     * for each of the six cube-map faces. Material changes that affect shadow face orientation can
     * affect the selected draw; scene-graph and resource-binding changes take effect in a later
     * frame.
     *
     * @param callback callback to retain
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public final void setBeforeShadowRenderCallback(RenderCallback callback) {
        beforeShadowRenderCallback = Objects.requireNonNull(callback, "callback");
    }

    /** Removes the shadow-depth callback without invoking it. */
    public final void clearBeforeShadowRenderCallback() {
        beforeShadowRenderCallback = null;
    }

    /**
     * Returns the callback invoked immediately after each successful shadow-depth draw.
     *
     * @return configured callback, or an empty value
     */
    public final Optional<RenderCallback> afterShadowRenderCallback() {
        return Optional.ofNullable(afterShadowRenderCallback);
    }

    /**
     * Replaces the callback invoked immediately after each successful shadow-depth draw.
     *
     * @param callback callback to retain
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public final void setAfterShadowRenderCallback(RenderCallback callback) {
        afterShadowRenderCallback = Objects.requireNonNull(callback, "callback");
    }

    /** Removes the shadow-depth callback without invoking it. */
    public final void clearAfterShadowRenderCallback() {
        afterShadowRenderCallback = null;
    }
}
