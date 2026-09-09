/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Standard adapter mapping live 3D components onto one hidden JScene3D scene graph. */
@SuppressWarnings("ReferenceEquality")
final class Object3dSpatialAdapter implements Spatial3dWorldModule {
    /** Hidden renderable scene root. */
    private final Scene root = new Scene();

    /** Hidden entity nodes created independently of authored component order. */
    private final Map<Entity, Object3D> nodes = new IdentityHashMap<>();

    /** Live transforms indexed by entity reference identity. */
    private final Map<Entity, Object3dTransform> transforms = new IdentityHashMap<>();

    /** Every live adapter registration in deterministic construction order. */
    private final List<AutoCloseable> registrations = new ArrayList<>();

    /** World identity captured by the first registration. */
    private @Nullable World world;

    /** The unique authored primary camera, whether or not its entity is active. */
    private @Nullable Object3dPerspectiveCamera primaryCamera;

    /** Terminal adapter state. */
    private boolean closed;

    @Override
    public Transform3d createTransform(Entity owner, Vector3fc position, Quaternionfc orientation, Vector3fc scale) {
        Entity validOwner = requireOwner(owner);
        if (transforms.containsKey(validOwner)) {
            throw new IllegalArgumentException("entity already has a registered Transform3d: " + validOwner.id());
        }
        Object3D node = node(validOwner);
        node.setPosition(Objects.requireNonNull(position, "position"));
        node.setQuaternion(Objects.requireNonNull(orientation, "orientation"));
        node.setScale(Objects.requireNonNull(scale, "scale"));
        Object3dTransform transform = new Object3dTransform(this, validOwner, node);
        transforms.put(validOwner, transform);
        registrations.add(transform);
        return transform;
    }

    @Override
    public Optional<Transform3d> findTransform(Entity owner) {
        return Optional.ofNullable(transforms.get(requireOwner(owner)));
    }

    @Override
    public PerspectiveCamera3d createPerspectiveCamera(
            Entity owner, float fieldOfViewDegrees, float near, float far, boolean primary) {
        Entity validOwner = requireOwner(owner);
        if (primary && primaryCamera != null) {
            throw new IllegalArgumentException("world already has a primary PerspectiveCamera3d");
        }
        Object3dPerspectiveCamera camera =
                new Object3dPerspectiveCamera(this, validOwner, fieldOfViewDegrees, near, far, primary);
        node(validOwner).add(camera.camera());
        registrations.add(camera);
        if (primary) {
            primaryCamera = camera;
        }
        return camera;
    }

    @Override
    public DirectionalLight3d createDirectionalLight(Entity owner, Color color, float intensity, Vector3fc target) {
        Entity validOwner = requireOwner(owner);
        Object3dDirectionalLight light = new Object3dDirectionalLight(
                this, validOwner, Objects.requireNonNull(color, "color"), intensity, target);
        node(validOwner).add(light.light());
        registrations.add(light);
        return light;
    }

    @Override
    public MeshRenderer3d createMeshRenderer(
            Entity owner, Mesh3dResource mesh, Material3dResource material, boolean visible) {
        Entity validOwner = requireOwner(owner);
        Object3dMeshRenderer renderer = new Object3dMeshRenderer(
                this,
                validOwner,
                Objects.requireNonNull(mesh, "mesh"),
                Objects.requireNonNull(material, "material"),
                visible);
        node(validOwner).add(renderer.renderable());
        registrations.add(renderer);
        return renderer;
    }

    @Override
    public BillboardRenderer3d createBillboardRenderer(
            Entity owner,
            Material3dResource material,
            Vector2fc size,
            Vector2fc anchor,
            BillboardAlignment alignment,
            boolean visible) {
        Entity validOwner = requireOwner(owner);
        Object3dBillboardRenderer renderer = new Object3dBillboardRenderer(
                this, validOwner, Objects.requireNonNull(material, "material"), size, anchor, alignment, visible);
        node(validOwner).add(renderer.renderable());
        registrations.add(renderer);
        return renderer;
    }

    @Override
    public void render(Renderer renderer, float viewportAspectRatio) {
        requireOpen();
        Renderer validRenderer = Objects.requireNonNull(renderer, "renderer");
        Object3dPerspectiveCamera camera = requireActivePrimaryCamera();
        camera.setAspectRatio(viewportAspectRatio);
        validRenderer.render(root, camera.camera());
    }

    @Override
    public boolean isReadyToRender() {
        requireOpen();
        return primaryCamera != null && primaryCamera.isActive();
    }

    @Override
    public Transform3d activePrimaryCameraTransform() {
        Object3dPerspectiveCamera camera = requireActivePrimaryCamera();
        Object3dTransform transform = transforms.get(camera.owner());
        if (transform == null) {
            throw new IllegalStateException("active primary camera entity has no Transform3d");
        }
        return transform;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<AutoCloseable> remaining = List.copyOf(registrations);
        RuntimeException failure = null;
        for (int index = remaining.size() - 1; index >= 0; index--) {
            failure = close(remaining.get(index), failure);
        }
        root.clear();
        nodes.clear();
        transforms.clear();
        primaryCamera = null;
        if (failure != null) {
            throw failure;
        }
    }

    /** Removes one terminal transform registration. */
    void release(Object3dTransform transform) {
        Object3dTransform validTransform = Objects.requireNonNull(transform, "transform");
        if (transforms.remove(validTransform.owner()) != validTransform) {
            throw new IllegalStateException("transform registration does not belong to this adapter");
        }
        registrations.remove(validTransform);
        prune(validTransform.owner());
    }

    /** Removes one terminal camera registration and its hidden scene object. */
    void release(Object3dPerspectiveCamera camera) {
        Object3dPerspectiveCamera validCamera = Objects.requireNonNull(camera, "camera");
        requireRegistration(validCamera);
        if (primaryCamera == validCamera) {
            primaryCamera = null;
        }
        validCamera.camera().detach();
        prune(validCamera.owner());
    }

    /** Removes one terminal directional-light registration and its hidden scene object. */
    void release(Object3dDirectionalLight light) {
        Object3dDirectionalLight validLight = Objects.requireNonNull(light, "light");
        requireRegistration(validLight);
        validLight.light().detach();
        prune(validLight.owner());
    }

    /** Removes one terminal mesh-renderer registration and its hidden scene object. */
    void release(Object3dMeshRenderer renderer) {
        Object3dMeshRenderer validRenderer = Objects.requireNonNull(renderer, "renderer");
        requireRegistration(validRenderer);
        validRenderer.renderable().detach();
        prune(validRenderer.owner());
    }

    /** Removes one terminal billboard-renderer registration and its hidden scene object. */
    void release(Object3dBillboardRenderer renderer) {
        Object3dBillboardRenderer validRenderer = Objects.requireNonNull(renderer, "renderer");
        requireRegistration(validRenderer);
        validRenderer.renderable().detach();
        prune(validRenderer.owner());
    }

    /** Returns or creates the hidden transform anchor for one entity. */
    private Object3D node(Entity owner) {
        Object3D existing = nodes.get(owner);
        if (existing != null) {
            return existing;
        }
        Object3D created = new Object3D();
        Object3D parent =
                owner.parent().map(transforms::get).map(Object3dTransform::node).orElse(root);
        parent.add(created);
        nodes.put(owner, created);
        return created;
    }

    /** Removes an entity node after its last component registration disappears. */
    private void prune(Entity owner) {
        if (!transforms.containsKey(owner) && registrations.stream().noneMatch(value -> owns(value, owner))) {
            Object3D removed = nodes.remove(owner);
            if (removed != null) {
                removed.detach();
            }
        }
    }

    /** Returns whether one presentation registration belongs to an entity. */
    private static boolean owns(AutoCloseable registration, Entity owner) {
        return switch (registration) {
            case Object3dPerspectiveCamera camera -> camera.owner() == owner;
            case Object3dDirectionalLight light -> light.owner() == owner;
            case Object3dMeshRenderer renderer -> renderer.owner() == owner;
            case Object3dBillboardRenderer renderer -> renderer.owner() == owner;
            default -> false;
        };
    }

    /** Captures one compatible world identity and returns a validated owner. */
    private Entity requireOwner(Entity owner) {
        requireOpen();
        Entity validOwner = Objects.requireNonNull(owner, "owner");
        World ownerWorld = validOwner.world();
        if (world == null) {
            world = ownerWorld;
        } else if (world != ownerWorld) {
            throw new IllegalArgumentException("spatial adapter cannot be shared by multiple worlds");
        }
        return validOwner;
    }

    /** Removes a known presentation registration. */
    private void requireRegistration(AutoCloseable registration) {
        if (!registrations.remove(registration)) {
            throw new IllegalStateException("presentation registration does not belong to this adapter");
        }
    }

    /** Returns the effectively active primary camera or reports why presentation cannot proceed. */
    private Object3dPerspectiveCamera requireActivePrimaryCamera() {
        if (primaryCamera == null || !primaryCamera.isActive()) {
            throw new IllegalStateException("world has no effectively active primary PerspectiveCamera3d");
        }
        return primaryCamera;
    }

    /** Closes one registration while preserving the first failure. */
    private static @Nullable RuntimeException close(AutoCloseable value, @Nullable RuntimeException existing) {
        try {
            value.close();
            return existing;
        } catch (Exception closeFailure) {
            RuntimeException failure = new IllegalStateException("3D registration cleanup failed", closeFailure);
            if (existing == null) {
                return failure;
            }
            existing.addSuppressed(failure);
            return existing;
        }
    }

    /** Rejects registration and rendering after terminal cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("spatial adapter is closed");
        }
    }
}
