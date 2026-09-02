/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.CollisionSensor;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.movement.KinematicContact;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.queries.SweepHit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Resolves explicit kinematic-body translation against solid world geometry. */
public final class KinematicMovement {
    private static final float MINIMUM_MOTION_SQUARED = 1.0E-10F;
    private static final float MINIMUM_STEP_PROGRESS_SQUARED = 1.0E-8F;
    private static final float MINIMUM_CLOSING_SPEED = 1.0E-7F;
    private static final float STEP_HEIGHT_TOLERANCE = 1.0E-5F;

    private final CollisionQueries queries;

    /**
     * Creates movement backed by the world's shared query machinery.
     *
     * @param queries shared collision-query facade
     */
    public KinematicMovement(CollisionQueries queries) {
        this.queries = queries;
    }

    /**
     * Resolves translation without mutating the body or world index.
     *
     * @param body kinematic body whose colliders are resolved
     * @param requestedTranslation desired world-space translation
     * @param settings collision-resolution settings
     * @return immutable resolution result without overlap events
     */
    public KinematicMoveResult resolve(
            KinematicBody body, Vector3fc requestedTranslation, KinematicMoveSettings settings) {
        MovementState state = new MovementState(body, requestedTranslation);
        for (int iteration = 0;
                iteration < settings.maximumSlideIterations() && state.hasRemainingMotion();
                iteration++) {
            Optional<SweepHit> hit = sweep(state, state.remaining);
            if (hit.isEmpty()) {
                state.position.add(state.remaining);
                state.remaining.zero();
                break;
            }
            SweepHit contact = hit.orElseThrow();
            state.advanceBefore(contact, settings.skinWidth());
            state.contacts.add(contact(contact));
            if (tryStep(state, contact, settings)) {
                state.stepped = true;
                continue;
            }
            state.slideAlong(contact.normal(new Vector3f()));
        }
        detectGround(state, settings);
        return state.result();
    }

    private boolean tryStep(MovementState state, SweepHit blockingHit, KinematicMoveSettings settings) {
        Vector3f up = settings.up(new Vector3f());
        Vector3f wallNormal = blockingHit.normal(new Vector3f());
        if (settings.maximumStepHeight() <= 0.0F || isWalkable(wallNormal, up, settings)) {
            return false;
        }
        Vector3f horizontalMotion = reject(state.remaining, up);
        if (horizontalMotion.lengthSquared() < MINIMUM_STEP_PROGRESS_SQUARED) {
            return false;
        }
        Vector3f raisedPosition = new Vector3f(state.position);
        Vector3f upwardMotion = new Vector3f(up).mul(settings.maximumStepHeight());
        if (sweep(state, raisedPosition, upwardMotion).isPresent()) {
            return false;
        }
        raisedPosition.add(upwardMotion);
        Vector3f probeMotion = stepProbeMotion(state, raisedPosition, horizontalMotion, settings);
        StepAdvance advance = advanceStep(state, raisedPosition, probeMotion, settings);
        if (!advance.reaches(horizontalMotion)) {
            return false;
        }
        return landStep(state, advance, horizontalMotion, up, settings);
    }

    private static Vector3f stepProbeMotion(
            MovementState state, Vector3fc raisedPosition, Vector3fc horizontalMotion, KinematicMoveSettings settings) {
        Vector3f direction = new Vector3f(horizontalMotion).normalize();
        float shapeReach = maximumReach(state, raisedPosition, direction);
        float probeDistance = Math.max(horizontalMotion.length(), shapeReach + settings.skinWidth());
        return direction.mul(probeDistance);
    }

    private static float maximumReach(MovementState state, Vector3fc bodyPosition, Vector3fc direction) {
        float maximum = 0.0F;
        for (Collider collider : state.colliders) {
            if (collider.isEnabled()) {
                ShapePose shapePose = pose(collider, bodyPosition, state.orientation);
                float reach = ShapeGeometry.support(shapePose, direction)
                        .sub(bodyPosition)
                        .dot(direction);
                maximum = Math.max(maximum, reach);
            }
        }
        return maximum;
    }

    private StepAdvance advanceStep(
            MovementState state, Vector3fc raisedPosition, Vector3f horizontalMotion, KinematicMoveSettings settings) {
        Optional<SweepHit> forwardHit = sweep(state, raisedPosition, horizontalMotion);
        if (forwardHit.isEmpty()) {
            return new StepAdvance(new Vector3f(horizontalMotion), List.of());
        }
        SweepHit hit = forwardHit.orElseThrow();
        Vector3f translation = beforeHit(horizontalMotion, hit, settings.skinWidth());
        return new StepAdvance(translation, List.of(contact(hit)));
    }

    private boolean landStep(
            MovementState state,
            StepAdvance advance,
            Vector3fc horizontalMotion,
            Vector3f up,
            KinematicMoveSettings settings) {
        Vector3f landingStart = new Vector3f(state.position)
                .fma(settings.maximumStepHeight(), up)
                .add(advance.translation());
        float descentDistance = settings.maximumStepHeight() + settings.groundSnapDistance();
        Vector3f descent = new Vector3f(up).mul(-descentDistance);
        Optional<SweepHit> landing = sweepWalkable(state, landingStart, descent, up, settings);
        if (landing.isEmpty()) {
            return false;
        }
        SweepHit ground = landing.orElseThrow();
        Vector3f normal = ground.normal(new Vector3f());
        float travel = Math.clamp(ground.distance() - settings.skinWidth(), 0.0F, Float.POSITIVE_INFINITY);
        Vector3f landingPosition = new Vector3f(landingStart).fma(-travel, up);
        float stepHeight = new Vector3f(landingPosition).sub(state.position).dot(up);
        if (stepHeight <= STEP_HEIGHT_TOLERANCE || stepHeight > settings.maximumStepHeight() + STEP_HEIGHT_TOLERANCE) {
            return false;
        }
        state.position.add(horizontalMotion).fma(stepHeight, up);
        state.removeHorizontalRemaining(up, horizontalMotion);
        state.contacts.addAll(advance.contacts());
        state.contacts.add(contact(ground));
        state.grounded = true;
        state.groundNormal.set(normal);
        return true;
    }

    private void detectGround(MovementState state, KinematicMoveSettings settings) {
        Vector3f up = settings.up(new Vector3f());
        if (state.requested.dot(up) > 0.0F) {
            return;
        }
        float probeDistance = settings.groundSnapDistance() + settings.skinWidth();
        if (probeDistance <= 0.0F) {
            return;
        }
        Vector3f probe = new Vector3f(up).mul(-probeDistance);
        Optional<SweepHit> groundHit = sweepWalkable(state, state.position, probe, up, settings);
        if (groundHit.isEmpty()) {
            return;
        }
        SweepHit ground = groundHit.orElseThrow();
        Vector3f normal = ground.normal(new Vector3f());
        float snapDistance = Math.clamp(ground.distance() - settings.skinWidth(), 0.0F, Float.POSITIVE_INFINITY);
        state.position.fma(-snapDistance, up);
        state.grounded = true;
        state.groundNormal.set(normal);
    }

    private Optional<SweepHit> sweep(MovementState state, Vector3fc translation) {
        return sweep(state, state.position, translation);
    }

    private Optional<SweepHit> sweep(MovementState state, Vector3fc bodyPosition, Vector3fc translation) {
        @Nullable SweepHit nearest = null;
        for (Collider movingCollider : state.colliders) {
            if (!movingCollider.isEnabled()) {
                continue;
            }
            ShapePose movingPose = pose(movingCollider, bodyPosition, state.orientation);
            Optional<SweepHit> hit =
                    sweepCollider(movingPose, translation, candidate -> acceptsSolid(movingCollider, candidate));
            if (hit.isPresent() && isNearer(hit.orElseThrow(), nearest)) {
                nearest = hit.orElseThrow();
            }
        }
        return Optional.ofNullable(nearest);
    }

    private Optional<SweepHit> sweepCollider(
            ShapePose pose, Vector3fc translation, Predicate<Collider> solidAcceptance) {
        Set<Collider> nonBlockingContacts = new HashSet<>();
        while (true) {
            Optional<SweepHit> hit = queries.sweepAccepted(
                    pose,
                    translation,
                    candidate -> solidAcceptance.test(candidate) && !nonBlockingContacts.contains(candidate));
            if (hit.isEmpty()) {
                return hit;
            }
            SweepHit contact = hit.orElseThrow();
            if (isBlocking(translation, contact)) {
                return hit;
            }
            nonBlockingContacts.add(contact.collider());
        }
    }

    private Optional<SweepHit> sweepWalkable(
            MovementState state,
            Vector3fc bodyPosition,
            Vector3fc translation,
            Vector3fc up,
            KinematicMoveSettings settings) {
        Set<Collider> rejectedColliders = new HashSet<>();
        while (true) {
            Optional<SweepHit> hit = sweepExcluding(state, bodyPosition, translation, rejectedColliders);
            if (hit.isEmpty()) {
                return hit;
            }
            SweepHit contact = hit.orElseThrow();
            if (isWalkable(contact.normal(new Vector3f()), up, settings)) {
                return hit;
            }
            rejectedColliders.add(contact.collider());
        }
    }

    private Optional<SweepHit> sweepExcluding(
            MovementState state, Vector3fc bodyPosition, Vector3fc translation, Set<Collider> rejectedColliders) {
        @Nullable SweepHit nearest = null;
        for (Collider movingCollider : state.colliders) {
            if (!movingCollider.isEnabled()) {
                continue;
            }
            ShapePose movingPose = pose(movingCollider, bodyPosition, state.orientation);
            Optional<SweepHit> hit = sweepCollider(
                    movingPose,
                    translation,
                    candidate -> acceptsSolid(movingCollider, candidate) && !rejectedColliders.contains(candidate));
            if (hit.isPresent() && isNearer(hit.orElseThrow(), nearest)) {
                nearest = hit.orElseThrow();
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static boolean isNearer(SweepHit candidate, @Nullable SweepHit current) {
        return current == null
                || candidate.fraction() < current.fraction()
                || candidate.fraction() == current.fraction()
                        && candidate.collider().id() < current.collider().id();
    }

    private static boolean isBlocking(Vector3fc translation, SweepHit hit) {
        return -translation.dot(hit.normal(new Vector3f())) > MINIMUM_CLOSING_SPEED;
    }

    private static boolean acceptsSolid(Collider movingCollider, Collider candidate) {
        return candidate.collisionObject() != movingCollider.collisionObject()
                && candidate.isRegistered()
                && candidate.isEnabled()
                && candidate.collisionObject().isEnabled()
                && !(candidate.collisionObject() instanceof CollisionSensor)
                && movingCollider.collisionFilter().matches(candidate.collisionFilter());
    }

    private static ShapePose pose(Collider collider, Vector3fc bodyPosition, Quaternionfc bodyOrientation) {
        Quaternionf orientation = new Quaternionf(bodyOrientation).mul(collider.localOrientation(new Quaternionf()));
        Vector3f position = new Quaternionf(bodyOrientation)
                .transform(collider.localPosition(new Vector3f()))
                .add(bodyPosition);
        return new ShapePose(collider.shape(), position, orientation);
    }

    private static boolean isWalkable(Vector3fc normal, Vector3fc up, KinematicMoveSettings settings) {
        return normal.dot(up) >= Math.cos(settings.maximumSlopeAngle());
    }

    private static Vector3f reject(Vector3fc vector, Vector3fc axis) {
        return new Vector3f(vector).fma(-vector.dot(axis), axis);
    }

    private static Vector3f beforeHit(Vector3fc translation, SweepHit hit, float skinWidth) {
        float length = translation.length();
        if (length <= 0.0F) {
            return new Vector3f();
        }
        Vector3f normal = hit.normal(new Vector3f());
        float closingSpeed = -translation.dot(normal);
        float skinTravel = closingSpeed > MINIMUM_CLOSING_SPEED ? skinWidth * length / closingSpeed : 0.0F;
        float travel = Math.clamp(hit.distance() - skinTravel, 0.0F, Float.POSITIVE_INFINITY);
        return new Vector3f(translation).mul(Math.clamp(travel / length, 0.0F, 1.0F));
    }

    private static KinematicContact contact(SweepHit hit) {
        return new KinematicContact(hit.collider(), hit.point(new Vector3f()), hit.normal(new Vector3f()));
    }

    private record StepAdvance(Vector3f translation, List<KinematicContact> contacts) {
        private boolean reaches(Vector3fc requiredTranslation) {
            Vector3f direction = new Vector3f(requiredTranslation).normalize();
            return translation.dot(direction) + STEP_HEIGHT_TOLERANCE >= requiredTranslation.length();
        }
    }

    private static final class MovementState {
        private final List<Collider> colliders;
        private final Vector3f start;
        private final Vector3f requested;
        private final Quaternionf orientation;
        private final Vector3f position;
        private final Vector3f remaining;
        private final List<KinematicContact> contacts = new ArrayList<>();
        private final Vector3f groundNormal = new Vector3f();
        private boolean grounded;
        private boolean stepped;

        private MovementState(KinematicBody body, Vector3fc requestedTranslation) {
            colliders = body.colliders();
            start = body.position(new Vector3f());
            requested = Preconditions.requireFinite(requestedTranslation, "translation");
            orientation = body.orientation(new Quaternionf());
            position = new Vector3f(start);
            remaining = new Vector3f(requested);
        }

        private boolean hasRemainingMotion() {
            return remaining.lengthSquared() >= MINIMUM_MOTION_SQUARED;
        }

        private void advanceBefore(SweepHit hit, float skinWidth) {
            Vector3f advance = beforeHit(remaining, hit, skinWidth);
            position.add(advance);
            remaining.sub(advance);
        }

        private void slideAlong(Vector3fc normal) {
            float intoSurface = remaining.dot(normal);
            if (intoSurface < 0.0F) {
                remaining.fma(-intoSurface, normal);
            }
        }

        private void removeHorizontalRemaining(Vector3fc up, Vector3fc consumedHorizontal) {
            float vertical = remaining.dot(up);
            Vector3f horizontal = reject(remaining, up).sub(consumedHorizontal);
            remaining.set(horizontal).fma(vertical, up);
            if (remaining.lengthSquared() < MINIMUM_MOTION_SQUARED) {
                remaining.zero();
            }
        }

        private KinematicMoveResult result() {
            return new KinematicMoveResult(
                    new Vector3f(position).sub(start), remaining, groundNormal, grounded, stepped, contacts, List.of());
        }
    }
}
