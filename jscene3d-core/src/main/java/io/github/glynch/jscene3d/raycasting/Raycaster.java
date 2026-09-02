/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/**
 * Stateful CPU-side ray used to select visible triangle meshes.
 *
 * <p>Camera coordinates use OpenGL normalized-device coordinates: negative one is the left or
 * bottom edge and positive one is the right or top edge. Queries return immutable hits ordered by
 * increasing world-space distance. The raycaster retains traversal and mathematical scratch state
 * between calls, is mutable, and is not thread-safe.
 */
public final class Raycaster {
    private static final Comparator<RaycastHit> BY_DISTANCE = Comparator.comparingDouble(RaycastHit::distance);

    private final RayState ray;
    private final MeshIntersector meshIntersector;
    private final ArrayDeque<Object3D> pendingObjects;
    private final ArrayList<RaycastHit> hits;
    private final Vector4f unprojectedPoint;
    private final Vector3f cameraPoint;

    /** Creates a raycaster at the origin directed along negative Z. */
    public Raycaster() {
        ray = new RayState();
        meshIntersector = new MeshIntersector();
        pendingObjects = new ArrayDeque<>();
        hits = new ArrayList<>();
        unprojectedPoint = new Vector4f();
        cameraPoint = new Vector3f();
    }

    /**
     * Creates a raycaster from copied origin and direction values.
     *
     * @param origin finite world-space origin
     * @param direction finite non-zero direction, normalized when copied
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component is not finite or the direction is zero
     */
    public Raycaster(Vector3fc origin, Vector3fc direction) {
        this();
        setRay(origin, direction);
    }

    /**
     * Copies the current world-space origin into caller-owned storage.
     *
     * @param destination vector receiving the origin
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Vector3f origin(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(ray.origin);
    }

    /**
     * Copies the current normalized world-space direction into caller-owned storage.
     *
     * @param destination vector receiving the direction
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Vector3f direction(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(ray.direction);
    }

    /**
     * Replaces the world-space ray and normalizes its direction.
     *
     * @param originX finite origin X coordinate
     * @param originY finite origin Y coordinate
     * @param originZ finite origin Z coordinate
     * @param directionX finite direction X component
     * @param directionY finite direction Y component
     * @param directionZ finite direction Z component
     * @throws IllegalArgumentException if a component is not finite or the direction is zero
     */
    public void setRay(
            float originX, float originY, float originZ, float directionX, float directionY, float directionZ) {
        float validOriginX = Preconditions.requireFinite(originX, "originX");
        float validOriginY = Preconditions.requireFinite(originY, "originY");
        float validOriginZ = Preconditions.requireFinite(originZ, "originZ");
        float validDirectionX = Preconditions.requireFinite(directionX, "directionX");
        float validDirectionY = Preconditions.requireFinite(directionY, "directionY");
        float validDirectionZ = Preconditions.requireFinite(directionZ, "directionZ");
        float largestComponent =
                Math.max(Math.max(Math.abs(validDirectionX), Math.abs(validDirectionY)), Math.abs(validDirectionZ));
        if (largestComponent == 0.0f) {
            throw new IllegalArgumentException("direction must not have zero length");
        }
        float scaledX = validDirectionX / largestComponent;
        float scaledY = validDirectionY / largestComponent;
        float scaledZ = validDirectionZ / largestComponent;
        float inverseLength = (float) (1.0 / Math.sqrt(scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ));
        ray.origin.set(validOriginX, validOriginY, validOriginZ);
        ray.direction.set(scaledX * inverseLength, scaledY * inverseLength, scaledZ * inverseLength);
    }

    /**
     * Replaces the world-space ray from copied vector values.
     *
     * @param origin finite origin to copy
     * @param direction finite non-zero direction to copy and normalize
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a component is not finite or the direction is zero
     */
    public void setRay(Vector3fc origin, Vector3fc direction) {
        Vector3fc validOrigin = Preconditions.requireFinite(origin, "origin");
        Vector3fc validDirection = Preconditions.requireFinite(direction, "direction");
        setRay(
                validOrigin.x(),
                validOrigin.y(),
                validOrigin.z(),
                validDirection.x(),
                validDirection.y(),
                validDirection.z());
    }

    /**
     * Replaces the ray with one passing through a camera-plane coordinate.
     *
     * <p>Coordinates outside {@code [-1, 1]} are accepted and cast beyond the current viewport.
     *
     * @param x finite horizontal normalized-device coordinate
     * @param y finite vertical normalized-device coordinate
     * @param camera perspective or orthographic camera supplying current transforms
     * @throws NullPointerException if {@code camera} is {@code null}
     * @throws IllegalArgumentException if a coordinate is not finite
     * @throws IllegalStateException if the camera world transform is not finite and invertible
     */
    public void setFromCamera(float x, float y, Camera camera) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        Camera validCamera = Objects.requireNonNull(camera, "camera");
        validCamera.viewMatrix();
        switch (validCamera) {
            case PerspectiveCamera perspective -> setFromPerspectiveCamera(validX, validY, perspective);
            case OrthographicCamera orthographic -> setFromOrthographicCamera(validX, validY, orthographic);
        }
    }

    /**
     * Intersects one visible object and all its visible descendants.
     *
     * @param root root object to test
     * @return immutable nearest-first intersection list
     * @throws NullPointerException if {@code root} is {@code null}
     * @throws IllegalStateException if selected mesh resources are closed or a mesh transform is
     *     not finite and invertible
     */
    public List<RaycastHit> intersect(Object3D root) {
        return intersect(root, true);
    }

    /**
     * Intersects one visible object and optionally its visible descendants.
     *
     * <p>An invisible object excludes its complete subtree. Only {@link Mesh} objects participate
     * in version 0.1; line picking requires a separately configurable tolerance.
     *
     * @param root root object to test
     * @param recursive whether to include descendants
     * @return immutable nearest-first intersection list
     * @throws NullPointerException if {@code root} is {@code null}
     * @throws IllegalStateException if selected mesh resources are closed or a mesh transform is
     *     not finite and invertible
     */
    public List<RaycastHit> intersect(Object3D root, boolean recursive) {
        Object3D validRoot = Objects.requireNonNull(root, "root");
        pendingObjects.clear();
        hits.clear();
        pendingObjects.push(validRoot);
        try {
            while (!pendingObjects.isEmpty()) {
                Object3D object = pendingObjects.pop();
                if (!object.isVisible()) {
                    continue;
                }
                if (object instanceof Mesh mesh) {
                    meshIntersector.intersect(mesh, ray, hits);
                }
                if (recursive) {
                    List<Object3D> children = object.children();
                    for (int index = children.size() - 1; index >= 0; index--) {
                        pendingObjects.push(children.get(index));
                    }
                }
            }
            hits.sort(BY_DISTANCE);
            return List.copyOf(hits);
        } finally {
            pendingObjects.clear();
            hits.clear();
        }
    }

    /** Resolves a ray from the camera position through a perspective clip-space point. */
    private void setFromPerspectiveCamera(float x, float y, PerspectiveCamera camera) {
        unproject(x, y, 0.5f, camera.inverseProjectionMatrix(), camera.matrixWorld(), cameraPoint);
        camera.worldPosition(ray.origin);
        cameraPoint.sub(ray.origin);
        setRay(ray.origin, cameraPoint);
    }

    /** Resolves a parallel ray through an orthographic camera-plane point. */
    private void setFromOrthographicCamera(float x, float y, OrthographicCamera camera) {
        float cameraPlaneDepth = (float) (((double) camera.near() + camera.far()) / (camera.near() - camera.far()));
        Matrix4fc worldMatrix = camera.matrixWorld();
        unproject(x, y, cameraPlaneDepth, camera.inverseProjectionMatrix(), worldMatrix, cameraPoint);
        worldMatrix.transformDirection(0.0f, 0.0f, -1.0f, ray.direction);
        setRay(cameraPoint, ray.direction);
    }

    /** Unprojects one normalized-device coordinate through projection and world transforms. */
    private void unproject(
            float x, float y, float z, Matrix4fc inverseProjection, Matrix4fc worldMatrix, Vector3f destination) {
        unprojectedPoint.set(x, y, z, 1.0f);
        inverseProjection.transform(unprojectedPoint);
        float reciprocalW = 1.0f / unprojectedPoint.w();
        destination.set(
                unprojectedPoint.x() * reciprocalW,
                unprojectedPoint.y() * reciprocalW,
                unprojectedPoint.z() * reciprocalW);
        worldMatrix.transformPosition(destination);
        if (!destination.isFinite()) {
            throw new IllegalStateException("Camera unprojection must produce a finite world-space point");
        }
    }
}
