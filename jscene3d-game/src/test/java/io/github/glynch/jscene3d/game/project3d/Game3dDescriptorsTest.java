/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class Game3dDescriptorsTest {
    @Test
    void describesTheReusableFirstPersonController() {
        var descriptor = Game3dDescriptors.extensionDescriptor();
        var controller = descriptor.components().getFirst();

        assertThat(descriptor.id()).isEqualTo(Game3dDescriptors.extensionId());
        assertThat(controller.type()).isEqualTo(Game3dDescriptors.firstPersonControllerType());
        assertThat(controller.properties())
                .containsOnlyKeys(
                        Game3dDescriptors.bodyProperty(),
                        Game3dDescriptors.viewTransformProperty(),
                        Game3dDescriptors.moveActionProperty(),
                        Game3dDescriptors.lookActionProperty(),
                        Game3dDescriptors.turnLeftActionProperty(),
                        Game3dDescriptors.turnRightActionProperty(),
                        Game3dDescriptors.moveSpeedProperty(),
                        Game3dDescriptors.turnSpeedDegreesProperty(),
                        Game3dDescriptors.maximumKeyboardTurnSpeedDegreesProperty(),
                        Game3dDescriptors.keyboardTurnAccelerationDegreesProperty(),
                        Game3dDescriptors.pointerSensitivityProperty(),
                        Game3dDescriptors.maximumPitchDegreesProperty());
        assertThat(controller.updatePhases())
                .isEqualTo(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS, ComponentUpdatePhase.FRAME_UPDATE));
    }
}
