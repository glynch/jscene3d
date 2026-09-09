/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Executable factories corresponding exactly to {@link GamePresentationDescriptors}. */
public final class GamePresentationRuntimeExtension implements ComponentRuntimeExtension {
    /** Creates the stateless built-in runtime contribution. */
    public GamePresentationRuntimeExtension() {
        // Explicit construction supports ordinary runtime-extension registration.
    }

    @Override
    public String id() {
        return GamePresentationDescriptors.extensionId();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        ComponentFactoryRegistry validRegistry = Objects.requireNonNull(registry, "registry");
        validRegistry.register(
                GamePresentationDescriptors.screenCanvasType(),
                context -> new ScreenCanvas(
                        context.owner(),
                        context.world().requireModule(PresentationWorldModule.class),
                        context.properties().finiteFloat(GamePresentationDescriptors.referenceWidthProperty()),
                        context.properties().finiteFloat(GamePresentationDescriptors.referenceHeightProperty())));
        validRegistry.register(GamePresentationDescriptors.screenRegionType(), context -> region(context.properties()));
        validRegistry.register(GamePresentationDescriptors.screenImageType(), new ImageFactory());
        validRegistry.register(GamePresentationDescriptors.bitmapNumberType(), new BitmapNumberFactory());
    }

    private static ScreenRegion region(ComponentProperties properties) {
        return new ScreenRegion(
                new ScreenRegion.Point(
                        properties.finiteFloat(GamePresentationDescriptors.anchorXProperty()),
                        properties.finiteFloat(GamePresentationDescriptors.anchorYProperty())),
                new ScreenRegion.Point(
                        properties.finiteFloat(GamePresentationDescriptors.pivotXProperty()),
                        properties.finiteFloat(GamePresentationDescriptors.pivotYProperty())),
                new ScreenRegion.Point(
                        properties.finiteFloat(GamePresentationDescriptors.offsetXProperty()),
                        properties.finiteFloat(GamePresentationDescriptors.offsetYProperty())),
                new ScreenRegion.Size(
                        properties.finiteFloat(GamePresentationDescriptors.widthProperty()),
                        properties.finiteFloat(GamePresentationDescriptors.heightProperty())));
    }

    private static final class ImageFactory implements ComponentFactory<ScreenImage> {
        @Override
        public void prepare(ComponentPreparationContext context) {
            context.resolveResource(imageReference(context.properties()), OverlayImageResource.class);
        }

        @Override
        public ScreenImage create(ComponentFactoryContext context) {
            return new ScreenImage(
                    context.resolveResource(imageReference(context.properties()), OverlayImageResource.class));
        }

        private static ResourceReference imageReference(ComponentProperties properties) {
            return properties.resourceReference(GamePresentationDescriptors.imageProperty());
        }
    }

    private static final class BitmapNumberFactory implements ComponentFactory<BitmapNumber> {
        @Override
        public void prepare(ComponentPreparationContext context) {
            references(context.properties(), GamePresentationDescriptors.digitsProperty())
                    .forEach(reference -> context.resolveResource(reference, OverlayImageResource.class));
            suffix(context.properties())
                    .ifPresent(reference -> context.resolveResource(reference, OverlayImageResource.class));
        }

        @Override
        public BitmapNumber create(ComponentFactoryContext context) {
            ComponentProperties properties = context.properties();
            List<OverlayImageResource> digits =
                    references(properties, GamePresentationDescriptors.digitsProperty()).stream()
                            .map(reference -> context.resolveResource(reference, OverlayImageResource.class))
                            .toList();
            Optional<OverlayImageResource> suffix =
                    suffix(properties).map(reference -> context.resolveResource(reference, OverlayImageResource.class));
            return new BitmapNumber(
                    digits,
                    suffix,
                    exactNonNegativeInteger(properties.value(GamePresentationDescriptors.initialValueProperty())),
                    properties.text(GamePresentationDescriptors.alignmentProperty()));
        }

        private static List<ResourceReference> references(ComponentProperties properties, PropertyId property) {
            ProjectValue value = properties.value(property);
            if (!(value instanceof ProjectValue.ArrayValue(List<ProjectValue> values))) {
                throw new IllegalArgumentException(property + " must be an array");
            }
            return values.stream().map(element -> reference(element, property)).toList();
        }

        private static Optional<ResourceReference> suffix(ComponentProperties properties) {
            return Optional.ofNullable(properties.values().get(GamePresentationDescriptors.suffixProperty()))
                    .map(value -> reference(value, GamePresentationDescriptors.suffixProperty()));
        }

        private static ResourceReference reference(ProjectValue value, PropertyId property) {
            if (!(value instanceof ProjectValue.ReferenceValue(ResourceReference reference))) {
                throw new IllegalArgumentException(property + " must contain resource references");
            }
            return reference;
        }

        private static int exactNonNegativeInteger(ProjectValue value) {
            if (!(value instanceof ProjectValue.NumberValue(var number))) {
                throw new IllegalArgumentException("initial-value must be a number");
            }
            try {
                int result = number.intValueExact();
                if (result < 0) {
                    throw new IllegalArgumentException("initial-value must be non-negative");
                }
                return result;
            } catch (ArithmeticException failure) {
                throw new IllegalArgumentException("initial-value must be an exact integer", failure);
            }
        }
    }
}
