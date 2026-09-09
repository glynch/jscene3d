/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable safe metadata for reusable descriptor-backed 3D game components. */
public final class Game3dDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.game3d";
    private static final ComponentType FIRST_PERSON_CONTROLLER = type("first-person-character-controller-3d");
    private static final PropertyId BODY = new PropertyId("body");
    private static final PropertyId VIEW_TRANSFORM = new PropertyId("view-transform");
    private static final PropertyId MOVE_ACTION = new PropertyId("move-action");
    private static final PropertyId LOOK_ACTION = new PropertyId("look-action");
    private static final PropertyId TURN_LEFT_ACTION = new PropertyId("turn-left-action");
    private static final PropertyId TURN_RIGHT_ACTION = new PropertyId("turn-right-action");
    private static final PropertyId MOVE_SPEED = new PropertyId("move-speed");
    private static final PropertyId TURN_SPEED_DEGREES = new PropertyId("turn-speed-degrees");
    private static final PropertyId MAXIMUM_KEYBOARD_TURN_SPEED_DEGREES =
            new PropertyId("maximum-keyboard-turn-speed-degrees");
    private static final PropertyId KEYBOARD_TURN_ACCELERATION_DEGREES =
            new PropertyId("keyboard-turn-acceleration-degrees");
    private static final PropertyId POINTER_SENSITIVITY = new PropertyId("pointer-sensitivity");
    private static final PropertyId MAXIMUM_PITCH_DEGREES = new PropertyId("maximum-pitch-degrees");
    private static final ExtensionDescriptor DESCRIPTOR = createDescriptor();

    private Game3dDescriptors() {
        throw new AssertionError("Game3dDescriptors cannot be instantiated");
    }

    /** Returns the built-in 3D gameplay extension identity.
     *
     * @return stable extension identity
     */
    public static String extensionId() {
        return EXTENSION_ID;
    }

    /** Returns the reusable first-person controller component type.
     *
     * @return exact version-one component type
     */
    public static ComponentType firstPersonControllerType() {
        return FIRST_PERSON_CONTROLLER;
    }

    /** Returns the character-body target property identity.
     *
     * @return body property identity
     */
    public static PropertyId bodyProperty() {
        return BODY;
    }

    /** Returns the view-transform target property identity.
     *
     * @return view-transform property identity
     */
    public static PropertyId viewTransformProperty() {
        return VIEW_TRANSFORM;
    }

    /** Returns the planar movement action property identity.
     *
     * @return move-action property identity
     */
    public static PropertyId moveActionProperty() {
        return MOVE_ACTION;
    }

    /** Returns the continuous look action property identity.
     *
     * @return look-action property identity
     */
    public static PropertyId lookActionProperty() {
        return LOOK_ACTION;
    }

    /** Returns the held left-turn action property identity.
     *
     * @return turn-left property identity
     */
    public static PropertyId turnLeftActionProperty() {
        return TURN_LEFT_ACTION;
    }

    /** Returns the held right-turn action property identity.
     *
     * @return turn-right property identity
     */
    public static PropertyId turnRightActionProperty() {
        return TURN_RIGHT_ACTION;
    }

    /** Returns the planar movement speed property identity.
     *
     * @return move-speed property identity
     */
    public static PropertyId moveSpeedProperty() {
        return MOVE_SPEED;
    }

    /** Returns the continuous look-rate property identity.
     *
     * @return turn-speed property identity
     */
    public static PropertyId turnSpeedDegreesProperty() {
        return TURN_SPEED_DEGREES;
    }

    /** Returns the held-key maximum turn-rate property identity.
     *
     * @return maximum keyboard turn-rate property identity
     */
    public static PropertyId maximumKeyboardTurnSpeedDegreesProperty() {
        return MAXIMUM_KEYBOARD_TURN_SPEED_DEGREES;
    }

    /** Returns the held-key angular-acceleration property identity.
     *
     * @return keyboard turn-acceleration property identity
     */
    public static PropertyId keyboardTurnAccelerationDegreesProperty() {
        return KEYBOARD_TURN_ACCELERATION_DEGREES;
    }

    /** Returns the relative-pointer sensitivity property identity.
     *
     * @return pointer-sensitivity property identity
     */
    public static PropertyId pointerSensitivityProperty() {
        return POINTER_SENSITIVITY;
    }

    /** Returns the symmetric pitch-limit property identity.
     *
     * @return maximum-pitch property identity
     */
    public static PropertyId maximumPitchDegreesProperty() {
        return MAXIMUM_PITCH_DEGREES;
    }

    /** Returns safe metadata for built-in 3D gameplay components.
     *
     * @return immutable extension descriptor
     */
    public static ExtensionDescriptor extensionDescriptor() {
        return DESCRIPTOR;
    }

    private static ExtensionDescriptor createDescriptor() {
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("JScene3D 3D gameplay"),
                List.of(),
                List.of(firstPersonControllerDescriptor()));
    }

    private static ComponentTypeDescriptor firstPersonControllerDescriptor() {
        return ComponentTypeDescriptor.builder(
                        FIRST_PERSON_CONTROLLER, DescriptorPresentation.named("First-person character controller 3D"))
                .properties(List.of(
                        required(BODY, ProjectValueKind.COMPONENT_TARGET, "Character body"),
                        required(VIEW_TRANSFORM, ProjectValueKind.COMPONENT_TARGET, "View transform"),
                        required(MOVE_ACTION, ProjectValueKind.TEXT, "Move action"),
                        required(LOOK_ACTION, ProjectValueKind.TEXT, "Look action"),
                        required(TURN_LEFT_ACTION, ProjectValueKind.TEXT, "Turn left action"),
                        required(TURN_RIGHT_ACTION, ProjectValueKind.TEXT, "Turn right action"),
                        positiveNumber(MOVE_SPEED, "Move speed"),
                        positiveNumber(TURN_SPEED_DEGREES, "Initial keyboard and continuous look speed"),
                        positiveNumber(MAXIMUM_KEYBOARD_TURN_SPEED_DEGREES, "Maximum keyboard turn speed"),
                        positiveNumber(KEYBOARD_TURN_ACCELERATION_DEGREES, "Keyboard turn acceleration"),
                        positiveNumber(POINTER_SENSITIVITY, "Pointer sensitivity"),
                        boundedPitch()))
                .updatePhases(Set.of(ComponentUpdatePhase.BEFORE_PHYSICS, ComponentUpdatePhase.FRAME_UPDATE))
                .build();
    }

    private static PropertyDescriptor required(PropertyId id, ProjectValueKind kind, String name) {
        return PropertyDescriptor.required(id.value(), kind, DescriptorPresentation.named(name), Map.of(), Set.of());
    }

    private static PropertyDescriptor positiveNumber(PropertyId id, String name) {
        return PropertyDescriptor.required(
                id.value(),
                ProjectValueKind.NUMBER,
                DescriptorPresentation.named(name),
                Map.of("minimum-exclusive", number(0.0F)),
                Set.of());
    }

    private static PropertyDescriptor boundedPitch() {
        return PropertyDescriptor.required(
                MAXIMUM_PITCH_DEGREES.value(),
                ProjectValueKind.NUMBER,
                DescriptorPresentation.named("Maximum pitch"),
                Map.of("minimum-exclusive", number(0.0F), "maximum-exclusive", number(90.0F)),
                Set.of());
    }

    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }

    private static ComponentType type(String localName) {
        return ComponentType.of(EXTENSION_ID + '/' + localName, 1);
    }
}
