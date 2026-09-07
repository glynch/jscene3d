/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.PreparedEntityDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class WorldFrameDriverTest {
    private static final InputAction JUMP = new InputAction("jump");
    private static final Duration STEP = Duration.ofMillis(10L);

    @Test
    void advancesDueFixedUpdatesThenOneFrame() {
        try (ProjectInput input = ProjectInput.empty()) {
            RecordingWorld world = new RecordingWorld(input);
            WorldFrameDriver driver = new WorldFrameDriver(world, input, settings());

            driver.advance(Duration.ofMillis(25L), ActionSnapshot.empty());

            assertThat(world.steps).containsExactly(STEP, STEP);
            assertThat(world.frameElapsed).isEqualTo(Duration.ofMillis(25L));
            assertThat(world.interpolation).isEqualTo(0.5F);
            assertThat(driver.settings()).isEqualTo(settings());
        }
    }

    @Test
    void buffersTransitionsUntilOneFixedUpdateConsumesThem() {
        try (ProjectInput input = ProjectInput.empty()) {
            RecordingWorld world = new RecordingWorld(input);
            WorldFrameDriver driver = new WorldFrameDriver(world, input, settings());
            ActionSnapshot pressed = ActionSnapshot.builder().pressed(JUMP).build();
            ActionSnapshot held = ActionSnapshot.builder().down(JUMP).build();

            driver.advance(Duration.ofMillis(4L), pressed);
            driver.advance(Duration.ofMillis(26L), held);

            assertThat(world.fixedInputs).hasSize(3);
            assertThat(world.fixedInputs.get(0).wasPressed(JUMP)).isTrue();
            assertThat(world.fixedInputs.get(1).wasPressed(JUMP)).isFalse();
            assertThat(world.fixedInputs.get(2).isDown(JUMP)).isTrue();
            assertThat(world.frameInput).isSameAs(held);
        }
    }

    @Test
    void clampsLongFramesAndBoundsCatchUp() {
        try (ProjectInput input = ProjectInput.empty()) {
            RecordingWorld world = new RecordingWorld(input);
            WorldFrameDriver driver = new WorldFrameDriver(world, input, settings());

            driver.advance(Duration.ofMillis(100L), ActionSnapshot.empty());

            assertThat(world.steps).hasSize(4);
            assertThat(world.frameElapsed).isEqualTo(Duration.ofMillis(40L));
            assertThat(world.interpolation).isZero();
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate null verifies public boundary validation.
    void validatesArgumentsAndInputOwnership() {
        try (ProjectInput input = ProjectInput.empty();
                ProjectInput other = ProjectInput.empty()) {
            RecordingWorld world = new RecordingWorld(input);
            ActionSnapshot empty = ActionSnapshot.empty();
            Duration negative = Duration.ofMillis(-1L);
            GameLoopSettings loopSettings = settings();

            assertThatThrownBy(() -> new WorldFrameDriver(world, other, loopSettings))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("module bound");
            WorldFrameDriver driver = new WorldFrameDriver(world, input);
            assertThatThrownBy(() -> driver.advance(negative, empty)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> driver.advance(Duration.ZERO, null)).isInstanceOf(NullPointerException.class);
        }
    }

    private static GameLoopSettings settings() {
        return GameLoopSettings.builder()
                .fixedStep(STEP)
                .maximumFrameTime(Duration.ofMillis(40L))
                .maximumFixedUpdates(4)
                .build();
    }

    /** Minimal observable world used to verify the driver's public timing contract. */
    private static final class RecordingWorld implements World {
        private final ProjectInput input;
        private final List<Duration> steps = new ArrayList<>();
        private final List<ActionSnapshot> fixedInputs = new ArrayList<>();
        private Duration frameElapsed = Duration.ZERO;
        private ActionSnapshot frameInput = ActionSnapshot.empty();
        private float interpolation;

        private RecordingWorld(ProjectInput input) {
            this.input = input;
        }

        @Override
        public WorldDefinition definition() {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public List<Entity> roots() {
            return List.of();
        }

        @Override
        public Optional<Entity> find(RuntimeEntityId id) {
            return Optional.empty();
        }

        @Override
        public <T extends WorldModule> Optional<T> findModule(Class<T> type) {
            return type == InputWorldModule.class ? Optional.of(type.cast(input)) : Optional.empty();
        }

        @Override
        public <T extends WorldModule> T requireModule(Class<T> type) {
            return findModule(type).orElseThrow();
        }

        @Override
        public PreparedEntityDefinition prepare(
                AssetRef<EntityDefinition> definition, Map<PropertyId, ProjectValue> resourceBindings) {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public SpawnTarget spawnTarget(Entity owner) {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public void activate() {
            throw new UnsupportedOperationException("the fixture is already active");
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void advanceFixed(Duration step) {
            steps.add(step);
            fixedInputs.add(input.snapshot());
        }

        @Override
        public void advanceFrame(Duration elapsed, float interpolation) {
            frameElapsed = elapsed;
            this.interpolation = interpolation;
            frameInput = input.snapshot();
        }

        @Override
        public void enable(Entity entity) {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public void disable(Entity entity) {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public void destroy(Entity entity) {
            throw new UnsupportedOperationException("not required by this fixture");
        }

        @Override
        public void close() {
            // The fixture owns no resources.
        }
    }
}
