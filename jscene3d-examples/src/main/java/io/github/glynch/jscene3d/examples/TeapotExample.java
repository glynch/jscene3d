/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.RendererSettingsScope;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.examples.teapot.TeapotPresentation;
import io.github.glynch.jscene3d.examples.teapot.TeapotPresentation.Shading;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.loaders.EnvironmentMapLoader;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.ToneMapping;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Interactive Utah teapot showcasing patch tessellation and six rendering presentations. */
public final class TeapotExample {
    private static final String ENVIRONMENT_RESOURCE =
            "/io/github/glynch/jscene3d/examples/environment/studio_small_08_1k.hdr";
    private static final List<ControlPanel.Choice<Integer>> TESSELLATION_CHOICES = List.of(
            choice(2),
            choice(3),
            choice(4),
            choice(5),
            choice(6),
            choice(8),
            choice(10),
            choice(15),
            choice(20),
            choice(30),
            choice(40),
            choice(50));
    private static final List<ControlPanel.Choice<Shading>> SHADING_CHOICES = List.of(
            new ControlPanel.Choice<>(Shading.WIREFRAME, "wireframe"),
            new ControlPanel.Choice<>(Shading.FLAT, "flat"),
            new ControlPanel.Choice<>(Shading.SMOOTH, "smooth"),
            new ControlPanel.Choice<>(Shading.GLOSSY, "glossy"),
            new ControlPanel.Choice<>(Shading.TEXTURED, "textured"),
            new ControlPanel.Choice<>(Shading.REFLECTIVE, "reflective"));

    /** Prevents instantiation of this example entry point. */
    private TeapotExample() {
        throw new AssertionError("TeapotExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Utah Teapot", TeapotExample::create);
    }

    /** Creates the shared hosted implementation used by standalone and browser launch modes. */
    static HostedExample create(ExampleContext context) {
        EnvironmentMap environmentMap = EnvironmentMapLoader.load(environmentPath());
        TeapotPresentation presentation = new TeapotPresentation();
        Scene scene = createScene(presentation, environmentMap);
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 1.8f, 5.5f);
        OrbitControls controls = new OrbitControls(camera, context.window());
        controls.setDistanceLimits(3.0f, 14.0f);
        controls.setDampingEnabled(true);
        controls.update();

        Renderer renderer = context.renderer();
        RendererSettingsScope rendererSettings = RendererSettingsScope.capture(renderer);
        renderer.setToneMapping(ToneMapping.ACES_FILMIC);
        renderer.setExposure(1.05f);

        SceneExample example = new SceneExample(context, scene, camera, controls);
        example.own(rendererSettings);
        example.own(environmentMap);
        example.own(presentation);
        TeapotControls settings = new TeapotControls(presentation);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fpsMonitor = example.addOverlay(new FpsMonitor());
        fpsMonitor.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fpsMonitor.update();
            if (settings.isAutoRotating()) {
                presentation.root().rotateY(frame.elapsedSeconds() * 0.24f);
            }
        });
        return example;
    }

    /** Creates the environmental and direct-lighting scene. */
    private static Scene createScene(TeapotPresentation presentation, EnvironmentMap environmentMap) {
        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x020409));
        scene.setEnvironment(environmentMap);
        scene.setEnvironmentIntensity(1.0f);
        scene.add(presentation.root());
        scene.add(new AmbientLight(Color.srgb(0x7890bc), 0.3f));
        DirectionalLight keyLight = new DirectionalLight(Color.srgb(0xfff1db), 2.6f);
        keyLight.setPosition(-4.0f, 7.0f, 6.0f);
        scene.add(keyLight);
        DirectionalLight fillLight = new DirectionalLight(Color.srgb(0x719dff), 0.8f);
        fillLight.setPosition(5.0f, 2.0f, -4.0f);
        scene.add(fillLight);
        return scene;
    }

    /** Creates controls corresponding to the canonical interactive teapot presentation. */
    private static ControlPanel createPanel(ExampleContext context, TeapotControls settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Utah Teapot");
        ControlPanel.Section geometry = panel.addSection("Geometry");
        geometry.addSelect("tessellation", settings::tessellation, settings::setTessellation, TESSELLATION_CHOICES);
        geometry.addBoolean("display lid", settings::includesLid, settings::setIncludeLid);
        geometry.addBoolean("display body", settings::includesBody, settings::setIncludeBody);
        geometry.addBoolean("display bottom", settings::includesBottom, settings::setIncludeBottom);
        geometry.addBoolean("fitted lid", settings::hasFittedLid, settings::setFittedLid);
        geometry.addBoolean("original scale", settings::hasOriginalProportions, settings::setOriginalProportions);
        ControlPanel.Section appearance = panel.addSection("Appearance");
        appearance.addSelect("shading", settings::shading, settings::setShading, SHADING_CHOICES);
        appearance.addBoolean("auto rotate", settings::isAutoRotating, settings::setAutoRotating);
        return panel;
    }

    /** Creates one labelled tessellation choice. */
    private static ControlPanel.Choice<Integer> choice(int tessellation) {
        return new ControlPanel.Choice<>(tessellation, Integer.toString(tessellation));
    }

    /** Resolves the required bundled HDR environment. */
    private static Path environmentPath() {
        URL resource =
                Objects.requireNonNull(TeapotExample.class.getResource(ENVIRONMENT_RESOURCE), ENVIRONMENT_RESOURCE);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid bundled environment URI", exception);
        }
    }

    /** Mutable explicit GUI bindings around the owned presentation. */
    private static final class TeapotControls {
        private final TeapotPresentation presentation;
        private boolean autoRotating = true;

        /** Retains the presentation without taking ownership. */
        private TeapotControls(TeapotPresentation presentation) {
            this.presentation = presentation;
        }

        /** Returns the patch subdivision count. */
        private int tessellation() {
            return presentation.tessellation();
        }

        /** Rebuilds the teapot with a patch subdivision count. */
        private void setTessellation(int tessellation) {
            presentation.setTessellation(tessellation);
        }

        /** Returns whether the lid is displayed. */
        private boolean includesLid() {
            return presentation.includesLid();
        }

        /** Shows or hides the lid. */
        private void setIncludeLid(boolean included) {
            presentation.setIncludeLid(included);
        }

        /** Returns whether the body is displayed. */
        private boolean includesBody() {
            return presentation.includesBody();
        }

        /** Shows or hides the body. */
        private void setIncludeBody(boolean included) {
            presentation.setIncludeBody(included);
        }

        /** Returns whether the bottom is displayed. */
        private boolean includesBottom() {
            return presentation.includesBottom();
        }

        /** Shows or hides the bottom. */
        private void setIncludeBottom(boolean included) {
            presentation.setIncludeBottom(included);
        }

        /** Returns whether the lid is widened to fit the opening. */
        private boolean hasFittedLid() {
            return presentation.hasFittedLid();
        }

        /** Enables or disables the fitted-lid correction. */
        private void setFittedLid(boolean fittedLid) {
            presentation.setFittedLid(fittedLid);
        }

        /** Returns whether original vertical proportions are used. */
        private boolean hasOriginalProportions() {
            return presentation.hasOriginalProportions();
        }

        /** Selects original or customary Blinn-corrected proportions. */
        private void setOriginalProportions(boolean originalProportions) {
            presentation.setOriginalProportions(originalProportions);
        }

        /** Returns the current rendering presentation. */
        private Shading shading() {
            return presentation.shading();
        }

        /** Selects a rendering presentation. */
        private void setShading(Shading shading) {
            presentation.setShading(shading);
        }

        /** Returns whether automatic turntable rotation is enabled. */
        private boolean isAutoRotating() {
            return autoRotating;
        }

        /** Enables or disables automatic turntable rotation. */
        private void setAutoRotating(boolean autoRotating) {
            this.autoRotating = autoRotating;
        }
    }
}
