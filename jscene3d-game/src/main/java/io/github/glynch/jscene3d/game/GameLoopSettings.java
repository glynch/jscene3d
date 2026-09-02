/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.game.internal.Preconditions;
import java.time.Duration;
import java.util.Objects;

/** Immutable fixed-timestep and overload-protection settings. */
public final class GameLoopSettings {
    /** Default 120 Hz simulation with at most twelve updates per rendered frame. */
    public static final GameLoopSettings DEFAULT = builder().build();

    private final Duration fixedStep;
    private final Duration maximumFrameTime;
    private final int maximumFixedUpdates;

    /** Copies validated builder state. */
    private GameLoopSettings(Builder builder) {
        fixedStep = builder.fixedStep;
        maximumFrameTime = builder.maximumFrameTime;
        maximumFixedUpdates = builder.maximumFixedUpdates;
    }

    /**
     * Returns a builder initialized with the default loop settings.
     *
     * @return new settings builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized from this value.
     *
     * @return copied settings builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the deterministic simulation duration.
     *
     * @return positive fixed duration
     */
    public Duration fixedStep() {
        return fixedStep;
    }

    /**
     * Returns the maximum real time accepted from one rendered frame.
     *
     * @return positive frame-time limit
     */
    public Duration maximumFrameTime() {
        return maximumFrameTime;
    }

    /**
     * Returns the maximum simulation updates performed for one rendered frame.
     *
     * @return positive update limit
     */
    public int maximumFixedUpdates() {
        return maximumFixedUpdates;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GameLoopSettings settings)) {
            return false;
        }
        return maximumFixedUpdates == settings.maximumFixedUpdates
                && fixedStep.equals(settings.fixedStep)
                && maximumFrameTime.equals(settings.maximumFrameTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixedStep, maximumFrameTime, maximumFixedUpdates);
    }

    @Override
    public String toString() {
        return "GameLoopSettings{fixedStep=" + fixedStep + ", maximumFrameTime=" + maximumFrameTime
                + ", maximumFixedUpdates=" + maximumFixedUpdates + '}';
    }

    /** Builds immutable loop settings. */
    public static final class Builder {
        private Duration fixedStep = Duration.ofNanos(8_333_333L);
        private Duration maximumFrameTime = Duration.ofMillis(100L);
        private int maximumFixedUpdates = 12;

        /** Creates a default builder. */
        private Builder() {}

        /** Copies an existing value into a builder. */
        private Builder(GameLoopSettings settings) {
            fixedStep = settings.fixedStep;
            maximumFrameTime = settings.maximumFrameTime;
            maximumFixedUpdates = settings.maximumFixedUpdates;
        }

        /**
         * Sets the deterministic simulation duration.
         *
         * @param fixedStep positive duration representable in nanoseconds
         * @return this builder
         */
        public Builder fixedStep(Duration fixedStep) {
            this.fixedStep = Preconditions.requirePositive(fixedStep, "fixedStep");
            return this;
        }

        /**
         * Sets the maximum accepted real time from one rendered frame.
         *
         * @param maximumFrameTime positive duration representable in nanoseconds
         * @return this builder
         */
        public Builder maximumFrameTime(Duration maximumFrameTime) {
            this.maximumFrameTime = Preconditions.requirePositive(maximumFrameTime, "maximumFrameTime");
            return this;
        }

        /**
         * Sets the maximum simulation updates performed for one rendered frame.
         *
         * @param maximumFixedUpdates positive update limit
         * @return this builder
         */
        public Builder maximumFixedUpdates(int maximumFixedUpdates) {
            if (maximumFixedUpdates <= 0) {
                throw new IllegalArgumentException("maximumFixedUpdates must be positive: " + maximumFixedUpdates);
            }
            this.maximumFixedUpdates = maximumFixedUpdates;
            return this;
        }

        /**
         * Builds validated immutable settings.
         *
         * @return immutable loop settings
         */
        public GameLoopSettings build() {
            if (maximumFrameTime.compareTo(fixedStep) < 0) {
                throw new IllegalStateException("maximumFrameTime must not be shorter than fixedStep");
            }
            Math.multiplyExact(fixedStep.toNanos(), maximumFixedUpdates);
            return new GameLoopSettings(this);
        }
    }
}
