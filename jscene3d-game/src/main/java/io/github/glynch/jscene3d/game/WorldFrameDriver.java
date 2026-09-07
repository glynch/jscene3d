/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.game.internal.Preconditions;
import io.github.glynch.jscene3d.project.runtime.World;
import java.time.Duration;
import java.util.Objects;

/** Drives one active project world from elapsed host-frame time and semantic input.
 *
 * <p>The driver owns fixed-step accumulation, overload protection, input-transition buffering,
 * and interpolation. It never creates, activates, closes, or renders the supplied world. Calls
 * are synchronous and must remain on the world's owning logical simulation thread.
 */
public final class WorldFrameDriver {
    private final World world;
    private final ProjectInput input;
    private final GameLoopSettings settings;
    private final long fixedNanos;
    private final long maximumFrameNanos;
    private final long maximumAccumulatedNanos;

    private ActionSnapshot pendingInput = ActionSnapshot.empty();
    private long accumulatorNanos;

    /** Creates a driver using the default fixed-step settings.
     *
     * @param world world containing the supplied input module
     * @param input host-writable input module owned by {@code world}
     */
    public WorldFrameDriver(World world, ProjectInput input) {
        this(world, input, GameLoopSettings.DEFAULT);
    }

    /** Creates a driver with explicit fixed-step settings.
     *
     * @param world world containing the supplied input module
     * @param input host-writable input module owned by {@code world}
     * @param settings immutable timing and overload settings
     * @throws IllegalArgumentException if the input module is not the world's exact bound module
     */
    public WorldFrameDriver(World world, ProjectInput input, GameLoopSettings settings) {
        this.world = Objects.requireNonNull(world, "world");
        this.input = Objects.requireNonNull(input, "input");
        this.settings = Objects.requireNonNull(settings, "settings");
        if (world.requireModule(InputWorldModule.class) != input) {
            throw new IllegalArgumentException("input must be the module bound to the supplied world");
        }
        fixedNanos = settings.fixedStep().toNanos();
        maximumFrameNanos = settings.maximumFrameTime().toNanos();
        maximumAccumulatedNanos = Math.multiplyExact(fixedNanos, settings.maximumFixedUpdates());
    }

    /** Advances every due fixed update and exactly one presentation update.
     *
     * <p>Pressed and released transitions remain buffered across rendered frames until a fixed
     * update consumes them. If several fixed updates run, only the first observes those
     * transitions; later updates observe the current held and axis state. The frame update sees
     * the unbuffered input acquired for this host frame.
     *
     * @param elapsed non-negative real time since the preceding host frame
     * @param acquiredInput semantic input acquired for this host frame
     */
    public void advance(Duration elapsed, ActionSnapshot acquiredInput) {
        Duration validElapsed = Preconditions.requireNonNegative(elapsed, "elapsed");
        ActionSnapshot validInput = Objects.requireNonNull(acquiredInput, "acquiredInput");
        long acceptedNanos = Math.min(validElapsed.toNanos(), maximumFrameNanos);
        pendingInput = pendingInput.merge(validInput);
        accumulatorNanos = Math.min(Math.addExact(accumulatorNanos, acceptedNanos), maximumAccumulatedNanos);
        runFixedUpdates();
        input.publish(validInput);
        world.advanceFrame(Duration.ofNanos(acceptedNanos), (float) accumulatorNanos / fixedNanos);
    }

    /** Returns the immutable settings used by this driver.
     *
     * @return loop settings
     */
    public GameLoopSettings settings() {
        return settings;
    }

    /** Runs every fixed update currently permitted by the bounded accumulator. */
    private void runFixedUpdates() {
        int updates = 0;
        while (accumulatorNanos >= fixedNanos && updates < settings.maximumFixedUpdates()) {
            input.publish(pendingInput);
            world.advanceFixed(settings.fixedStep());
            pendingInput = pendingInput.heldOnly();
            accumulatorNanos -= fixedNanos;
            updates++;
        }
    }
}
