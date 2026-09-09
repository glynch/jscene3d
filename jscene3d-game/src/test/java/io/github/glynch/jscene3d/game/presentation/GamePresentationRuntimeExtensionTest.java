/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.audio.AudioCategory;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentPreparationContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentProperties;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayImage;
import io.github.glynch.jscene3d.render.Renderer;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Verifies every safe presentation component descriptor has one executable factory. */
final class GamePresentationRuntimeExtensionTest {
    /** Registers and constructs canvas, region, image, and bitmap-number components from validated properties. */
    @Test
    void constructsAllDescriptorBackedScreenComponents() {
        GamePresentationRuntimeExtension extension = new GamePresentationRuntimeExtension();
        Map<ComponentType, ComponentFactory<?>> factories = new LinkedHashMap<>();
        extension.register(factories::put);
        OverlayImageResource image = OverlayImageResource.owning(OverlayImage.srgbRgba(1, 1, new byte[] {1, 2, 3, 4}));
        ResourceReference reference = ResourceReference.imported("screen/image");
        Map<ResourceReference, Object> resources = Map.of(reference, image);

        assertThat(extension.id()).isEqualTo(GamePresentationDescriptors.extensionId());
        assertThat(factories.keySet())
                .containsExactly(
                        GamePresentationDescriptors.screenCanvasType(),
                        GamePresentationDescriptors.screenRegionType(),
                        GamePresentationDescriptors.screenImageType(),
                        GamePresentationDescriptors.bitmapNumberType());

        Object region = create(
                factory(factories, GamePresentationDescriptors.screenRegionType()),
                properties(Map.of(
                        GamePresentationDescriptors.anchorXProperty(), number(0),
                        GamePresentationDescriptors.anchorYProperty(), number(1),
                        GamePresentationDescriptors.pivotXProperty(), number(0),
                        GamePresentationDescriptors.pivotYProperty(), number(1),
                        GamePresentationDescriptors.offsetXProperty(), number(8),
                        GamePresentationDescriptors.offsetYProperty(), number(-8),
                        GamePresentationDescriptors.widthProperty(), number(80),
                        GamePresentationDescriptors.heightProperty(), number(18))),
                resources);
        assertThat(region).isInstanceOf(ScreenRegion.class);

        ComponentProperties imageProperties = properties(
                Map.of(GamePresentationDescriptors.imageProperty(), new ProjectValue.ReferenceValue(reference)));
        prepare(factory(factories, GamePresentationDescriptors.screenImageType()), imageProperties, resources);
        assertThat(create(
                        factory(factories, GamePresentationDescriptors.screenImageType()), imageProperties, resources))
                .isInstanceOf(ScreenImage.class);

        ProjectValue.ArrayValue digits =
                new ProjectValue.ArrayValue(Collections.nCopies(10, new ProjectValue.ReferenceValue(reference)));
        ComponentProperties numberProperties = properties(Map.of(
                GamePresentationDescriptors.digitsProperty(),
                digits,
                GamePresentationDescriptors.suffixProperty(),
                new ProjectValue.ReferenceValue(reference),
                GamePresentationDescriptors.initialValueProperty(),
                number(100),
                GamePresentationDescriptors.alignmentProperty(),
                new ProjectValue.TextValue("left")));
        prepare(factory(factories, GamePresentationDescriptors.bitmapNumberType()), numberProperties, resources);
        assertThat(create(
                        factory(factories, GamePresentationDescriptors.bitmapNumberType()),
                        numberProperties,
                        resources))
                .isInstanceOf(ScreenNumber.class)
                .extracting(value -> ((ScreenNumber) value).value())
                .isEqualTo(100);

        RecordingPresentation presentation = new RecordingPresentation();
        ComponentProperties canvasProperties = properties(Map.of(
                GamePresentationDescriptors.referenceWidthProperty(), number(320),
                GamePresentationDescriptors.referenceHeightProperty(), number(200)));
        Object canvas = create(
                factory(factories, GamePresentationDescriptors.screenCanvasType()),
                canvasProperties,
                resources,
                presentation);
        assertThat(canvas).isInstanceOf(ScreenCanvas.class);
        ((ScreenCanvas) canvas).close();
        image.close();
    }

    private static void prepare(
            ComponentFactory<?> factory, ComponentProperties properties, Map<ResourceReference, Object> resources) {
        factory.prepare((ComponentPreparationContext) Proxy.newProxyInstance(
                GamePresentationRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {ComponentPreparationContext.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "properties" -> properties;
                    case "resolveResource" -> resources.get(arguments[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                }));
    }

    private static ComponentFactory<?> factory(
            Map<ComponentType, ComponentFactory<?>> factories, ComponentType componentType) {
        return Objects.requireNonNull(factories.get(componentType));
    }

    private static Object create(
            ComponentFactory<?> factory, ComponentProperties properties, Map<ResourceReference, Object> resources) {
        return create(factory, properties, resources, new RecordingPresentation());
    }

    private static Object create(
            ComponentFactory<?> factory,
            ComponentProperties properties,
            Map<ResourceReference, Object> resources,
            PresentationWorldModule presentation) {
        Entity owner = proxy(Entity.class, Map.of());
        World world = proxy(World.class, Map.of("requireModule", presentation));
        ComponentFactoryContext context = (ComponentFactoryContext) Proxy.newProxyInstance(
                GamePresentationRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {ComponentFactoryContext.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "properties" -> properties;
                    case "resolveResource" -> resources.get(arguments[0]);
                    case "owner" -> owner;
                    case "world" -> world;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return factory.create(context);
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(
                GamePresentationRuntimeExtensionTest.class.getClassLoader(),
                new Class<?>[] {type},
                (proxy, method, arguments) -> {
                    Object value = values.get(method.getName());
                    if (value == null) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return value;
                }));
    }

    private static ComponentProperties properties(Map<PropertyId, ProjectValue> values) {
        return new ComponentProperties(values);
    }

    private static ProjectValue.NumberValue number(int value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    private static final class RecordingPresentation implements PresentationWorldModule {
        @Override
        public OverlayRegistration registerOverlay(Overlay overlay) {
            return () -> {};
        }

        @Override
        public LocalSound createLocalSound(PcmAudioResource audio, AudioCategory category) {
            throw new UnsupportedOperationException("factory test creates no sound");
        }

        @Override
        public void renderOverlays(Renderer renderer) {
            throw new UnsupportedOperationException("factory test has no renderer");
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("factory test does not close the presentation module");
        }
    }
}
