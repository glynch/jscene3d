/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.CollisionFilter;
import io.github.glynch.jscene3d.physics.CollisionObject;
import io.github.glynch.jscene3d.physics.CollisionSensor;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.StaticBody;
import io.github.glynch.jscene3d.physics.queries.OverlapHit;
import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.RaycastHit;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Standard renderer-independent backend for descriptor-backed 3D collision components. */
@SuppressWarnings("ReferenceEquality")
final class StandardPhysics3dWorldModule implements Physics3dWorldModule {
    private static final float UNIT_SCALE_TOLERANCE = 1.0E-5F;

    private final PhysicsWorld physics = new PhysicsWorld();
    private final List<Registration> registrations = new ArrayList<>();
    private final Map<CollisionObject3d, Registration> registrationsByComponent = new IdentityHashMap<>();
    private final Map<CollisionObject, Registration> registrationsByObject = new IdentityHashMap<>();
    private final Map<CollisionShape3d, Registration> shapeOwners = new IdentityHashMap<>();
    private @Nullable World world;
    private boolean closed;

    @Override
    public CollisionObject3dRegistration registerStaticBody(
            StaticBody3d body, Transform3d transform, List<CollisionShape3d> shapes) {
        return register(Objects.requireNonNull(body, "body"), transform, shapes, null);
    }

    @Override
    public CollisionObject3dRegistration registerSensor(
            CollisionSensor3d sensor,
            Transform3d transform,
            List<CollisionShape3d> shapes,
            CollisionOverlapListener listener) {
        return register(
                Objects.requireNonNull(sensor, "sensor"),
                transform,
                shapes,
                Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void stepPhysics(FixedUpdateContext update) {
        requireOpen();
        Objects.requireNonNull(update, "update");
        List<Registration> stable = List.copyOf(registrations);
        stable.forEach(Registration::synchronizeTransform);
        stable.stream().filter(Registration::isSensor).forEach(Registration::detectOverlaps);
    }

    @Override
    public Optional<CollisionRaycastHit3d> raycast(Vector3fc origin, Vector3fc direction, float maximumDistance) {
        requireOpen();
        return physics.raycast(origin, direction, maximumDistance).map(this::projectHit);
    }

    @Override
    public int collisionObjectCount() {
        requireOpen();
        return physics.collisionObjectCount();
    }

    @Override
    public int collisionShapeCount() {
        requireOpen();
        return physics.colliderCount();
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
        List<Registration> remaining = List.copyOf(registrations);
        for (int index = remaining.size() - 1; index >= 0; index--) {
            remaining.get(index).close();
        }
        physics.clear();
        registrations.clear();
        registrationsByComponent.clear();
        registrationsByObject.clear();
        shapeOwners.clear();
    }

    /** Transactionally creates one low-level collision object and all explicitly referenced shapes. */
    private Registration register(
            CollisionObject3d component,
            Transform3d transform,
            List<CollisionShape3d> shapes,
            @Nullable CollisionOverlapListener listener) {
        requireOpen();
        CollisionObject3d validComponent = Objects.requireNonNull(component, "component");
        Transform3d validTransform = Objects.requireNonNull(transform, "transform");
        List<CollisionShape3d> validShapes = validateShapes(validComponent, shapes);
        requireWorld(validComponent.owner().world());
        if (registrationsByComponent.containsKey(validComponent)) {
            throw new IllegalArgumentException(
                    "collision object is already registered: " + validComponent.componentId());
        }
        Pose pose = pose(validTransform);
        CollisionObject object = createObject(validComponent, pose);
        object.setEnabled(false);
        Registration registration = new Registration(validComponent, validTransform, object, validShapes, listener);
        try {
            registration.attachShapes();
            registrations.add(registration);
            registrationsByComponent.put(validComponent, registration);
            registrationsByObject.put(object, registration);
            validShapes.forEach(shape -> shapeOwners.put(shape, registration));
            return registration;
        } catch (RuntimeException failure) {
            physics.remove(object);
            throw failure;
        }
    }

    /** Validates shape ownership, uniqueness, and exclusive membership before backend mutation. */
    private List<CollisionShape3d> validateShapes(CollisionObject3d component, List<CollisionShape3d> shapes) {
        List<CollisionShape3d> copied = List.copyOf(Objects.requireNonNull(shapes, "shapes"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("a collision object requires at least one shape");
        }
        Map<CollisionShape3d, Boolean> unique = new IdentityHashMap<>();
        for (CollisionShape3d shape : copied) {
            if (shape.owner() != component.owner()) {
                throw new IllegalArgumentException("collision shape must belong to the collision-object entity");
            }
            if (unique.put(shape, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("collision shape membership is duplicated: " + shape.componentId());
            }
            if (shapeOwners.containsKey(shape)) {
                throw new IllegalArgumentException(
                        "collision shape already belongs to another object: " + shape.componentId());
            }
        }
        return copied;
    }

    /** Creates the appropriate low-level object without exposing it through the project interface. */
    private CollisionObject createObject(CollisionObject3d component, Pose pose) {
        if (component instanceof StaticBody3d) {
            return physics.addStaticBody(pose.position(), pose.orientation());
        }
        if (component instanceof CollisionSensor3d) {
            return physics.addCollisionSensor(pose.position(), pose.orientation());
        }
        throw new IllegalArgumentException("unsupported collision object component: " + component.getClass());
    }

    /** Captures one owning world and prevents cross-world registrations. */
    private void requireWorld(World candidate) {
        if (world == null) {
            world = candidate;
        } else if (world != candidate) {
            throw new IllegalArgumentException("physics adapter cannot register components from another world");
        }
    }

    /** Forgets relationships to an object which can no longer be named safely by later exit signals. */
    private void removePreviousOverlaps(CollisionObject3d removed) {
        registrations.stream()
                .filter(Registration::isSensor)
                .forEach(registration -> registration
                        .previous
                        .entrySet()
                        .removeIf(entry -> entry.getValue().sensor() == removed
                                || entry.getValue().other() == removed));
    }

    /** Converts one backend collider to its project-level shape component. */
    private CollisionShape3d projectShape(Collider collider) {
        Registration owner = registrationsByObject.get(collider.collisionObject());
        if (owner == null) {
            throw new IllegalStateException("physics collider has no project registration");
        }
        CollisionShape3d shape = owner.shapesByCollider.get(collider);
        if (shape == null) {
            throw new IllegalStateException("physics collider has no project shape");
        }
        return shape;
    }

    /** Converts one backend raycast result without leaking backend collision identities. */
    private CollisionRaycastHit3d projectHit(RaycastHit hit) {
        Registration owner = registrationsByObject.get(hit.collisionObject());
        if (owner == null) {
            throw new IllegalStateException("physics raycast object has no project registration");
        }
        return new CollisionRaycastHit3d(
                owner.component,
                projectShape(hit.collider()),
                hit.distance(),
                hit.point(new Vector3f()),
                hit.normal(new Vector3f()));
    }

    /** Converts one project resource to a backend shape without transferring ownership. */
    private static CollisionShape backendShape(CollisionShape3dResource resource) {
        return switch (resource) {
            case BoxCollisionShape3dResource box -> new BoxShape(box.width(), box.height(), box.depth());
            case SphereCollisionShape3dResource sphere -> new SphereShape(sphere.radius());
            case TriangleMeshCollisionShape3dResource mesh -> mesh.shape();
        };
    }

    /** Extracts an unscaled rigid pose from one authoritative world transform. */
    private static Pose pose(Transform3d transform) {
        Matrix4fc matrix = transform.worldMatrix();
        Vector3f scale = matrix.getScale(new Vector3f());
        if (!approximatelyUnit(scale.x) || !approximatelyUnit(scale.y) || !approximatelyUnit(scale.z)) {
            throw new IllegalArgumentException("collision-object Transform3d world scale must be one");
        }
        return new Pose(
                matrix.getTranslation(new Vector3f()),
                matrix.getUnnormalizedRotation(new Quaternionf()).normalize());
    }

    /** Tests one extracted scale axis against the first profile's no-scaling invariant. */
    private static boolean approximatelyUnit(float value) {
        return Math.abs(value - 1.0F) <= UNIT_SCALE_TOLERANCE;
    }

    /** Rejects all operations after module-owned cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Physics3dWorldModule is closed");
        }
    }

    /** One object registration and its precise shape-pair overlap history. */
    private final class Registration implements CollisionObject3dRegistration {
        private final CollisionObject3d component;
        private final Transform3d transform;
        private final CollisionObject object;
        private final List<CollisionShape3d> shapes;
        private final @Nullable CollisionOverlapListener listener;
        private final Map<Collider, CollisionShape3d> shapesByCollider = new LinkedHashMap<>();
        private Map<ShapePair, CollisionOverlap3d> previous = new LinkedHashMap<>();
        private boolean closed;

        /** Stores one initially disabled registration before attaching its member shapes. */
        private Registration(
                CollisionObject3d component,
                Transform3d transform,
                CollisionObject object,
                List<CollisionShape3d> shapes,
                @Nullable CollisionOverlapListener listener) {
            this.component = component;
            this.transform = transform;
            this.object = object;
            this.shapes = shapes;
            this.listener = listener;
        }

        @Override
        public void setEnabled(boolean enabled) {
            requireRegistrationOpen();
            object.setEnabled(enabled);
            if (!enabled && isSensor()) {
                previous = new LinkedHashMap<>();
            }
        }

        @Override
        public boolean isEnabled() {
            requireRegistrationOpen();
            return object.isEnabled();
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
            release();
            shapesByCollider.clear();
            previous = new LinkedHashMap<>();
        }

        /** Removes this terminal registration and its shape claims from the owning module. */
        private void release() {
            if (!registrations.remove(this)) {
                return;
            }
            registrationsByComponent.remove(component);
            registrationsByObject.remove(object);
            shapes.forEach(shape -> shapeOwners.remove(shape, this));
            removePreviousOverlaps(component);
            if (object.isRegistered()) {
                physics.remove(object);
            }
        }

        /** Attaches every project shape in stable authored membership order. */
        private void attachShapes() {
            for (CollisionShape3d shape : shapes) {
                Collider collider = object.addCollider(
                        backendShape(shape.resource()), shape.localPosition(), shape.localOrientation());
                CollisionFilter3d filter = shape.filter();
                collider.setCollisionFilter(new CollisionFilter(filter.categoryBits(), filter.maskBits()));
                shapesByCollider.put(collider, shape);
            }
        }

        /** Synchronizes authored transform state before any collision query runs. */
        private void synchronizeTransform() {
            if (closed) {
                return;
            }
            Pose current = pose(transform);
            switch (object) {
                case StaticBody body -> body.setTransform(current.position(), current.orientation());
                case CollisionSensor sensor -> sensor.setTransform(current.position(), current.orientation());
                default -> throw new IllegalStateException("unsupported registered collision object");
            }
        }

        /** Detects and emits deterministic enter and exit transitions for every precise sensor shape pair. */
        private void detectOverlaps() {
            if (closed || !object.isEnabled()) {
                return;
            }
            CollisionOverlapListener sink = requireListener();
            Map<ShapePair, CollisionOverlap3d> current = currentOverlaps();
            current.forEach((pair, overlap) -> {
                if (!previous.containsKey(pair)) {
                    sink.onEntered(overlap);
                }
            });
            previous.forEach((pair, overlap) -> {
                if (!current.containsKey(pair)) {
                    sink.onExited(overlap);
                }
            });
            previous = current;
        }

        /** Computes all accepted exact shape pairs for the current sensor pose. */
        private Map<ShapePair, CollisionOverlap3d> currentOverlaps() {
            Map<ShapePair, CollisionOverlap3d> current = new LinkedHashMap<>();
            for (Map.Entry<Collider, CollisionShape3d> sensorEntry : shapesByCollider.entrySet()) {
                Collider sensorCollider = sensorEntry.getKey();
                CollisionShape3d sensorShape = sensorEntry.getValue();
                QueryFilter query =
                        QueryFilter.layers(sensorShape.filter().maskBits()).excluding(object);
                List<OverlapHit> hits = physics.overlap(
                        sensorCollider.shape(),
                        sensorCollider.position(new Vector3f()),
                        sensorCollider.orientation(new Quaternionf()),
                        query);
                for (OverlapHit hit : hits) {
                    addAcceptedOverlap(current, sensorCollider, sensorShape, hit.collider());
                }
            }
            return current;
        }

        /** Adds one mutually filtered project-level relationship. */
        private void addAcceptedOverlap(
                Map<ShapePair, CollisionOverlap3d> current,
                Collider sensorCollider,
                CollisionShape3d sensorShape,
                Collider otherCollider) {
            CollisionShape3d otherShape = projectShape(otherCollider);
            if (!sensorShape.filter().matches(otherShape.filter())) {
                return;
            }
            Registration otherRegistration = registrationsByObject.get(otherCollider.collisionObject());
            if (otherRegistration == null) {
                throw new IllegalStateException("overlap object has no project registration");
            }
            ShapePair pair = new ShapePair(sensorCollider.id(), otherCollider.id());
            current.put(
                    pair,
                    new CollisionOverlap3d(
                            (CollisionSensor3d) component, sensorShape, otherRegistration.component, otherShape));
        }

        /** Returns whether this registration represents a sensor. */
        private boolean isSensor() {
            return component instanceof CollisionSensor3d;
        }

        /** Returns the required listener for a sensor registration. */
        private CollisionOverlapListener requireListener() {
            if (listener == null) {
                throw new IllegalStateException("sensor registration has no overlap listener");
            }
            return listener;
        }

        /** Rejects registration access after cleanup. */
        private void requireRegistrationOpen() {
            if (closed) {
                throw new IllegalStateException("collision object registration is closed");
            }
        }
    }

    /** One exact low-level collider relationship used only for transition tracking. */
    private record ShapePair(long sensorShape, long otherShape) {}

    /** One unscaled rigid world pose. */
    private record Pose(Vector3f position, Quaternionf orientation) {}
}
