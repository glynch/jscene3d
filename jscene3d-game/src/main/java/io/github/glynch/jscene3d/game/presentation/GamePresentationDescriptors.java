/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Safe metadata for generic game-presentation resources and screen components. */
public final class GamePresentationDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.presentation";
    private static final RegisteredType PCM_AUDIO_RESOURCE = new RegisteredType(EXTENSION_ID + "/pcm-audio", 1);
    private static final RegisteredType OVERLAY_IMAGE_RESOURCE = new RegisteredType(EXTENSION_ID + "/overlay-image", 1);
    private static final ComponentType SCREEN_CANVAS = componentType("screen-canvas");
    private static final ComponentType SCREEN_REGION = componentType("screen-region");
    private static final ComponentType SCREEN_IMAGE = componentType("screen-image");
    private static final ComponentType BITMAP_NUMBER = componentType("bitmap-number");
    private static final CapabilityId SCREEN_REGION_CAPABILITY = new CapabilityId(EXTENSION_ID + "/screen-region");
    private static final CapabilityId SCREEN_CONTENT_CAPABILITY = new CapabilityId(EXTENSION_ID + "/screen-content");
    private static final CapabilityId SCREEN_NUMBER_CAPABILITY = new CapabilityId(EXTENSION_ID + "/screen-number");
    private static final PropertyId PAYLOAD = new PropertyId("payload");
    private static final PropertyId WIDTH = new PropertyId("width");
    private static final PropertyId HEIGHT = new PropertyId("height");
    private static final PropertyId REFERENCE_WIDTH = new PropertyId("reference-width");
    private static final PropertyId REFERENCE_HEIGHT = new PropertyId("reference-height");
    private static final PropertyId ANCHOR_X = new PropertyId("anchor-x");
    private static final PropertyId ANCHOR_Y = new PropertyId("anchor-y");
    private static final PropertyId PIVOT_X = new PropertyId("pivot-x");
    private static final PropertyId PIVOT_Y = new PropertyId("pivot-y");
    private static final PropertyId OFFSET_X = new PropertyId("offset-x");
    private static final PropertyId OFFSET_Y = new PropertyId("offset-y");
    private static final PropertyId IMAGE = new PropertyId("image");
    private static final PropertyId DIGITS = new PropertyId("digits");
    private static final PropertyId SUFFIX = new PropertyId("suffix");
    private static final PropertyId INITIAL_VALUE = new PropertyId("initial-value");
    private static final PropertyId ALIGNMENT = new PropertyId("alignment");
    private static final ExtensionDescriptor DESCRIPTOR = createDescriptor();

    /** Prevents construction of this stable metadata container. */
    private GamePresentationDescriptors() {
        throw new AssertionError("GamePresentationDescriptors cannot be instantiated");
    }

    /**
     * Returns the built-in presentation extension identity.
     *
     * @return stable extension identity
     */
    public static String extensionId() {
        return EXTENSION_ID;
    }

    /**
     * Returns the exact version-one signed PCM resource type.
     *
     * @return PCM resource type
     */
    public static RegisteredType pcmAudioResourceType() {
        return PCM_AUDIO_RESOURCE;
    }

    /**
     * Returns the exact version-one immutable overlay-image resource type.
     *
     * @return overlay-image resource type
     */
    public static RegisteredType overlayImageResourceType() {
        return OVERLAY_IMAGE_RESOURCE;
    }

    /**
     * Returns the descriptor-backed screen-canvas component type.
     *
     * @return screen-canvas component type
     */
    public static ComponentType screenCanvasType() {
        return SCREEN_CANVAS;
    }

    /**
     * Returns the descriptor-backed screen-region component type.
     *
     * @return screen-region component type
     */
    public static ComponentType screenRegionType() {
        return SCREEN_REGION;
    }

    /**
     * Returns the descriptor-backed screen-image component type.
     *
     * @return screen-image component type
     */
    public static ComponentType screenImageType() {
        return SCREEN_IMAGE;
    }

    /**
     * Returns the descriptor-backed bitmap-number component type.
     *
     * @return bitmap-number component type
     */
    public static ComponentType bitmapNumberType() {
        return BITMAP_NUMBER;
    }

    /**
     * Returns the screen-number capability used by explicit consumers.
     *
     * @return screen-number capability identity
     */
    public static CapabilityId screenNumberCapability() {
        return SCREEN_NUMBER_CAPABILITY;
    }

    /**
     * Returns safe metadata for generic game-presentation resources and components.
     *
     * @return immutable extension descriptor
     */
    public static ExtensionDescriptor extensionDescriptor() {
        return DESCRIPTOR;
    }

    static CapabilityId screenRegionCapability() {
        return SCREEN_REGION_CAPABILITY;
    }

    static CapabilityId screenContentCapability() {
        return SCREEN_CONTENT_CAPABILITY;
    }

    static PropertyId payloadProperty() {
        return PAYLOAD;
    }

    static PropertyId widthProperty() {
        return WIDTH;
    }

    static PropertyId heightProperty() {
        return HEIGHT;
    }

    static PropertyId referenceWidthProperty() {
        return REFERENCE_WIDTH;
    }

    static PropertyId referenceHeightProperty() {
        return REFERENCE_HEIGHT;
    }

    static PropertyId anchorXProperty() {
        return ANCHOR_X;
    }

    static PropertyId anchorYProperty() {
        return ANCHOR_Y;
    }

    static PropertyId pivotXProperty() {
        return PIVOT_X;
    }

    static PropertyId pivotYProperty() {
        return PIVOT_Y;
    }

    static PropertyId offsetXProperty() {
        return OFFSET_X;
    }

    static PropertyId offsetYProperty() {
        return OFFSET_Y;
    }

    static PropertyId imageProperty() {
        return IMAGE;
    }

    static PropertyId digitsProperty() {
        return DIGITS;
    }

    static PropertyId suffixProperty() {
        return SUFFIX;
    }

    static PropertyId initialValueProperty() {
        return INITIAL_VALUE;
    }

    static PropertyId alignmentProperty() {
        return ALIGNMENT;
    }

    private static ExtensionDescriptor createDescriptor() {
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("JScene3D game presentation"),
                List.of(pcmAudioDescriptor(), overlayImageDescriptor()),
                List.of(
                        screenCanvasDescriptor(),
                        screenRegionDescriptor(),
                        screenImageDescriptor(),
                        bitmapNumberDescriptor()));
    }

    /** Describes signed interleaved PCM backed by an opaque little-endian payload. */
    private static RegisteredTypeDescriptor pcmAudioDescriptor() {
        return new RegisteredTypeDescriptor(
                PCM_AUDIO_RESOURCE,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.described("PCM audio", "Immutable signed 16-bit interleaved PCM audio"),
                List.of(
                        required(PAYLOAD, ProjectValueKind.REFERENCE, "Payload"),
                        required("channels", ProjectValueKind.NUMBER),
                        required("sample-rate", ProjectValueKind.NUMBER),
                        required("sample-count", ProjectValueKind.NUMBER)),
                List.of(),
                List.of(),
                List.of());
    }

    /** Describes one immutable row-major sRGB RGBA image used by screen presentation. */
    private static RegisteredTypeDescriptor overlayImageDescriptor() {
        return new RegisteredTypeDescriptor(
                OVERLAY_IMAGE_RESOURCE,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.described("Overlay image", "Immutable row-major sRGB RGBA screen image"),
                List.of(
                        required(PAYLOAD, ProjectValueKind.REFERENCE, "Payload"),
                        required(WIDTH, ProjectValueKind.NUMBER, "Width"),
                        required(HEIGHT, ProjectValueKind.NUMBER, "Height")),
                List.of(),
                List.of(),
                List.of());
    }

    private static ComponentTypeDescriptor screenCanvasDescriptor() {
        return ComponentTypeDescriptor.builder(SCREEN_CANVAS, DescriptorPresentation.named("Screen canvas"))
                .properties(List.of(
                        positiveNumber(REFERENCE_WIDTH, "Reference width"),
                        positiveNumber(REFERENCE_HEIGHT, "Reference height")))
                .build();
    }

    private static ComponentTypeDescriptor screenRegionDescriptor() {
        return ComponentTypeDescriptor.builder(SCREEN_REGION, DescriptorPresentation.named("Screen region"))
                .properties(List.of(
                        unitNumber(ANCHOR_X, "Horizontal anchor"),
                        unitNumber(ANCHOR_Y, "Vertical anchor"),
                        unitNumber(PIVOT_X, "Horizontal pivot"),
                        unitNumber(PIVOT_Y, "Vertical pivot"),
                        required(OFFSET_X, ProjectValueKind.NUMBER, "Horizontal offset"),
                        required(OFFSET_Y, ProjectValueKind.NUMBER, "Vertical offset"),
                        positiveNumber(WIDTH, "Width"),
                        positiveNumber(HEIGHT, "Height")))
                .providedCapabilities(Set.of(SCREEN_REGION_CAPABILITY))
                .build();
    }

    private static ComponentTypeDescriptor screenImageDescriptor() {
        return ComponentTypeDescriptor.builder(SCREEN_IMAGE, DescriptorPresentation.named("Screen image"))
                .properties(List.of(required(IMAGE, ProjectValueKind.REFERENCE, "Image")))
                .providedCapabilities(Set.of(SCREEN_CONTENT_CAPABILITY))
                .build();
    }

    private static ComponentTypeDescriptor bitmapNumberDescriptor() {
        return ComponentTypeDescriptor.builder(BITMAP_NUMBER, DescriptorPresentation.named("Bitmap number"))
                .properties(List.of(
                        PropertyDescriptor.requiredArray(
                                DIGITS.value(),
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.named("Digit images"),
                                Map.of()),
                        PropertyDescriptor.optional(
                                SUFFIX.value(),
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.named("Suffix image"),
                                Map.of(),
                                Set.of()),
                        required(INITIAL_VALUE, ProjectValueKind.NUMBER, "Initial value"),
                        required(ALIGNMENT, ProjectValueKind.TEXT, "Alignment")))
                .providedCapabilities(Set.of(SCREEN_CONTENT_CAPABILITY, SCREEN_NUMBER_CAPABILITY))
                .build();
    }

    /** Creates one required resource property descriptor. */
    private static PropertyDescriptor required(String id, ProjectValueKind kind) {
        return PropertyDescriptor.required(id, kind, DescriptorPresentation.named(id), Map.of(), Set.of());
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

    private static PropertyDescriptor unitNumber(PropertyId id, String name) {
        return PropertyDescriptor.required(
                id.value(),
                ProjectValueKind.NUMBER,
                DescriptorPresentation.named(name),
                Map.of("minimum", number(0.0F), "maximum", number(1.0F)),
                Set.of());
    }

    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }

    private static ComponentType componentType(String name) {
        return ComponentType.of(EXTENSION_ID + '/' + name, 1);
    }
}
