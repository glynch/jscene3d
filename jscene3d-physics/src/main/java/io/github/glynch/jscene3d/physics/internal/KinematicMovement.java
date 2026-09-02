/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.movement.KinematicContact;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.queries.SweepHit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Resolves explicit collider translation against solid world geometry. */
public final class KinematicMovement {
    private static final float MINIMUM_MOTION_SQUARED = 1.0E-10F;
    private static final float MINIMUM_STEP_PROGRESS_SQUARED = 1.0E-8F;

    private final CollisionQueries queries;

    /** Creates movement backed by the world's shared query machinery.
     * @param queries shared collision-query facade
     */
    public KinematicMovement(CollisionQueries queries) {
        this.queries = queries;
    }

    /** Resolves translation without mutating the collider or world index.
     * @param movingCollider collider whose shape and pose are resolved
     * @param requestedTranslation desired world-space translation
     * @param settings collision-resolution settings
     * @return immutable resolution result without trigger events
     */
    public KinematicMoveResult resolve(
            Collider movingCollider, Vector3fc requestedTranslation, KinematicMoveSettings settings) {
        MovementState state = new MovementState(movingCollider, requestedTranslation);
        Predicate<Collider> solidAcceptance = candidate -> acceptsSolid(movingCollider, candidate);
        for (int iteration = 0;
                iteration < settings.maximumSlideIterations() && state.hasRemainingMotion();
                iteration++) {
            Optional<SweepHit> hit = sweep(state, state.remaining, solidAcceptance);
            if (hit.isEmpty()) {
                state.position.add(state.remaining);
                state.remaining.zero();
                break;
            }
            SweepHit contact = hit.orElseThrow();
            state.advanceBefore(contact, settings.skinWidth());
            state.contacts.add(contact(contact));
            if (tryStep(state, contact, settings, solidAcceptance)) {
                state.stepped = true;
                continue;
            }
            state.slideAlong(contact.normal(new Vector3f()));
        }
        detectGround(state, settings, solidAcceptance);
        return state.result();
    }

    private boolean tryStep(
            MovementState state,
            SweepHit blockingHit,
            KinematicMoveSettings settings,
            Predicate<Collider> solidAcceptance) {
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
        if (sweep(state.at(raisedPosition), upwardMotion, solidAcceptance).isPresent()) {
            return false;
        }
        raisedPosition.add(upwardMotion);
        StepAdvance advance = advanceStep(state.at(raisedPosition), horizontalMotion, settings, solidAcceptance);
        if (advance.translation().lengthSquared() < MINIMUM_STEP_PROGRESS_SQUARED) {
            return false;
        }
        return landStep(state, advance, up, settings, solidAcceptance);
    }

    private StepAdvance advanceStep(
            ShapePose raisedPose,
            Vector3f horizontalMotion,
            KinematicMoveSettings settings,
            Predicate<Collider> solidAcceptance) {
        Optional<SweepHit> forwardHit = sweep(raisedPose, horizontalMotion, solidAcceptance);
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
            Vector3f up,
            KinematicMoveSettings settings,
            Predicate<Collider> solidAcceptance) {
        Vector3f landingStart = new Vector3f(state.position)
                .fma(settings.maximumStepHeight(), up)
                .add(advance.translation());
        float descentDistance = settings.maximumStepHeight() + settings.groundSnapDistance();
        Vector3f descent = new Vector3f(up).mul(-descentDistance);
        Optional<SweepHit> landing = sweep(state.at(landingStart), descent, solidAcceptance);
        if (landing.isEmpty()) {
            return false;
        }
        SweepHit ground = landing.orElseThrow();
        Vector3f normal = ground.normal(new Vector3f());
        if (!isWalkable(normal, up, settings)) {
            return false;
        }
        float travel = Math.clamp(ground.distance() - settings.skinWidth(), 0.0F, Float.POSITIVE_INFINITY);
        state.position.set(landingStart).fma(-travel, up);
        state.removeHorizontalRemaining(up, advance.translation());
        state.contacts.addAll(advance.contacts());
        state.contacts.add(contact(ground));
        state.grounded = true;
        state.groundNormal.set(normal);
        return true;
    }

    private void detectGround(
            MovementState state, KinematicMoveSettings settings, Predicate<Collider> solidAcceptance) {
        Vector3f up = settings.up(new Vector3f());
        if (state.requested.dot(up) > 0.0F) {
            return;
        }
        float probeDistance = settings.groundSnapDistance() + settings.skinWidth();
        if (probeDistance <= 0.0F) {
            return;
        }
        Vector3f probe = new Vector3f(up).mul(-probeDistance);
        Optional<SweepHit> groundHit = sweep(state, probe, solidAcceptance);
        if (groundHit.isEmpty()) {
            return;
        }
        SweepHit ground = groundHit.orElseThrow();
        Vector3f normal = ground.normal(new Vector3f());
        if (!isWalkable(normal, up, settings)) {
            return;
        }
        float snapDistance = Math.clamp(ground.distance() - settings.skinWidth(), 0.0F, Float.POSITIVE_INFINITY);
        state.position.fma(-snapDistance, up);
        state.grounded = true;
        state.groundNormal.set(normal);
    }

    private Optional<SweepHit> sweep(MovementState state, Vector3fc translation, Predicate<Collider> solidAcceptance) {
        return sweep(state.at(state.position), translation, solidAcceptance);
    }

    private Optional<SweepHit> sweep(ShapePose pose, Vector3fc translation, Predicate<Collider> solidAcceptance) {
        return queries.sweepAccepted(pose, translation, solidAcceptance);
    }

    private static boolean acceptsSolid(Collider movingCollider, Collider candidate) {
        return candidate != movingCollider
                && candidate.isRegistered()
                && candidate.isEnabled()
                && !candidate.isTrigger()
                && movingCollider.collisionFilter().matches(candidate.collisionFilter());
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
        float travel = Math.clamp(hit.distance() - skinWidth, 0.0F, Float.POSITIVE_INFINITY);
        return new Vector3f(translation).mul(Math.clamp(travel / length, 0.0F, 1.0F));
    }

    private static KinematicContact contact(SweepHit hit) {
        return new KinematicContact(hit.collider(), hit.point(new Vector3f()), hit.normal(new Vector3f()));
    }

    private record StepAdvance(Vector3f translation, List<KinematicContact> contacts) {}

    private static final class MovementState {
        private final Collider collider;
        private final Vector3f start;
        private final Vector3f requested;
        private final Quaternionf orientation;
        private final Vector3f position;
        private final Vector3f remaining;
        private final List<KinematicContact> contacts = new ArrayList<>();
        private final Vector3f groundNormal = new Vector3f();
        private boolean grounded;
        private boolean stepped;

        private MovementState(Collider collider, Vector3fc requestedTranslation) {
            this.collider = collider;
            start = collider.position(new Vector3f());
            requested = requireFinite(requestedTranslation);
            orientation = collider.orientation(new Quaternionf());
            position = new Vector3f(start);
            remaining = new Vector3f(requested);
        }

        private boolean hasRemainingMotion() {
            return remaining.lengthSquared() >= MINIMUM_MOTION_SQUARED;
        }

        private ShapePose at(Vector3fc posePosition) {
            return new ShapePose(collider.shape(), posePosition, orientation);
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

        private static Vector3f requireFinite(Vector3fc translation) {
            if (translation == null || !translation.isFinite()) {
                throw new IllegalArgumentException("translation must be finite");
            }
            return new Vector3f(translation);
        }
    }
}
