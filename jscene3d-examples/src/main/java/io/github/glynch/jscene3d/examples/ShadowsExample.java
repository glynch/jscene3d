/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.controls.OrbitControls;
import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.examples.framework.SceneExample;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.SphereGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.gui.ControlPanel;
import io.github.glynch.jscene3d.gui.FpsMonitor;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.DirectionalLightShadow;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.lights.ShadowCastingLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.render.RenderStatistics;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;

/** Demonstrates configurable directional, spot, and point-light shadow maps. */
public final class ShadowsExample {
    private static final List<ControlPanel.Choice<LightType>> LIGHT_CHOICES = List.of(
            new ControlPanel.Choice<>(LightType.DIRECTIONAL, "directional"),
            new ControlPanel.Choice<>(LightType.SPOT, "spot"),
            new ControlPanel.Choice<>(LightType.POINT, "point"));
    private static final List<ControlPanel.Choice<Integer>> MAP_SIZE_CHOICES = List.of(
            new ControlPanel.Choice<>(256, "256"),
            new ControlPanel.Choice<>(512, "512"),
            new ControlPanel.Choice<>(1024, "1024"));

    /** Prevents instantiation of this example entry point. */
    private ShadowsExample() {
        throw new AssertionError("ShadowsExample cannot be instantiated");
    }

    /**
     * Opens the example window and renders until it is closed or Escape is pressed.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        ExampleLauncher.launch("JScene3D - Shadows", ShadowsExample::create);
    }

    /** Creates the shared hosted implementation used by both launch modes. */
    static HostedExample create(ExampleContext context) {
        BufferGeometry boxGeometry = BoxGeometry.create(1.8f, 1.8f, 1.8f);
        BufferGeometry sphereGeometry = SphereGeometry.create(1.0f, 48, 24);
        BufferGeometry torusGeometry = TorusGeometry.create(0.9f, 0.32f, 24, 64);
        BufferGeometry groundGeometry = PlaneGeometry.create(13.0f, 9.0f);
        LambertMaterial lambert = new LambertMaterial(Color.srgb(0x1fc7ff));
        PhongMaterial phong = new PhongMaterial(Color.srgb(0xff4f9a));
        phong.setSpecular(Color.WHITE);
        phong.setShininess(90.0f);
        StandardMaterial standard = new StandardMaterial(Color.srgb(0xffb52e));
        standard.setMetalness(0.55f);
        standard.setRoughness(0.28f);
        LambertMaterial groundMaterial = new LambertMaterial(Color.srgb(0x3a4250));
        groundMaterial.setSide(MaterialSide.DOUBLE);

        Mesh box = shadowCaster(boxGeometry, lambert, -2.4f, 0.0f);
        Mesh sphere = shadowCaster(sphereGeometry, phong, 0.0f, 0.0f);
        Mesh torus = shadowCaster(torusGeometry, standard, 2.4f, 0.15f);
        torus.rotateX(PI_OVER_TWO);
        Mesh ground = new Mesh(groundGeometry, groundMaterial);
        ground.rotateX(-PI_OVER_TWO);
        ground.setPosition(0.0f, -1.05f, 0.0f);
        ground.setShadowReceivingEnabled(true);

        DirectionalLight directional = directionalLight();
        SpotLight spot = spotLight();
        PointLight point = pointLight();
        List<ShadowCastingLight> lights = List.of(directional, spot, point);
        List<Mesh> casters = List.of(box, sphere, torus);
        List<Mesh> receivers = List.of(ground);
        ShadowControls settings = new ShadowControls(lights, casters, receivers);

        Scene scene = new Scene();
        scene.setBackground(Color.srgb(0x070b12));
        scene.add(new AmbientLight(Color.srgb(0x8096b8), 0.12f));
        scene.add(box);
        scene.add(sphere);
        scene.add(torus);
        scene.add(ground);
        scene.add(directional);
        scene.add(spot);
        scene.add(point);

        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, context.aspectRatio(), 0.1f, 100.0f);
        camera.setPosition(7.0f, 4.5f, 8.5f);
        OrbitControls orbit = new OrbitControls(camera, context.window());
        orbit.setTarget(0.0f, 0.0f, 0.0f);
        orbit.setDistanceLimits(6.0f, 28.0f);
        orbit.setDampingEnabled(true);
        orbit.update();

        SceneExample example = new SceneExample(context, scene, camera, orbit);
        ownResources(
                example,
                boxGeometry,
                sphereGeometry,
                torusGeometry,
                groundGeometry,
                lambert,
                phong,
                standard,
                groundMaterial);
        ControlPanel panel = example.addOverlay(createPanel(context, settings));
        FpsMonitor fps = example.addOverlay(new FpsMonitor());
        fps.setPosition(context.logicalLeft() + 16.0f, 16.0f);
        example.setPointerCapture(panel::capturesPointer);
        example.setFrameAction((ignored, frame) -> {
            panel.update();
            fps.update();
            box.rotateY(frame.elapsedSeconds() * 0.3f);
            torus.rotateZ(frame.elapsedSeconds() * 0.22f);
        });
        return example;
    }

    /** Creates one positioned mesh that casts onto the separate receiving floor. */
    private static Mesh shadowCaster(BufferGeometry geometry, Material material, float x, float y) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, y, 0.0f);
        mesh.setShadowCastingEnabled(true);
        return mesh;
    }

    /** Creates the default parallel key light and its orthographic shadow camera. */
    private static DirectionalLight directionalLight() {
        DirectionalLight light = new DirectionalLight(Color.srgb(0xffedd4), 2.2f);
        light.setPosition(-5.0f, 7.0f, 5.0f);
        light.setTarget(0.0f, 0.0f, 0.0f);
        light.setShadowCastingEnabled(true);
        DirectionalLightShadow shadow = light.shadow();
        shadow.setCameraBounds(-7.0f, 7.0f, -6.0f, 6.0f);
        shadow.setCameraRange(0.5f, 30.0f);
        shadow.setBias(0.0025f);
        shadow.setNormalBias(0.06f);
        return light;
    }

    /** Creates the default conical key light and its perspective shadow camera. */
    private static SpotLight spotLight() {
        SpotLight light = new SpotLight(Color.srgb(0xffedd4), 65.0f);
        light.setPosition(0.0f, 7.0f, 5.0f);
        light.setTarget(0.0f, 0.0f, 0.0f);
        light.setDistance(30.0f);
        light.setAngle(0.62f);
        light.setPenumbra(0.28f);
        light.shadow().setCameraRange(0.5f, 30.0f);
        light.shadow().setBias(0.0025f);
        light.shadow().setNormalBias(0.06f);
        light.setShadowCastingEnabled(true);
        return light;
    }

    /** Creates the default local key light and its six-face shadow camera. */
    private static PointLight pointLight() {
        PointLight light = new PointLight(Color.srgb(0xffedd4), 85.0f);
        light.setPosition(3.5f, 4.5f, 3.0f);
        light.setDistance(30.0f);
        light.shadow().setCameraRange(0.5f, 30.0f);
        light.shadow().setBias(0.0025f);
        light.shadow().setNormalBias(0.06f);
        light.setShadowCastingEnabled(true);
        return light;
    }

    /** Registers all example-owned geometry and material resources. */
    private static void ownResources(SceneExample example, AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            example.own(resource);
        }
    }

    /** Creates controls for light selection, participation flags, and shadow-map tuning. */
    private static ControlPanel createPanel(ExampleContext context, ShadowControls settings) {
        ControlPanel panel = new ControlPanel(context.window(), "Shadows");
        ControlPanel.Section light = panel.addSection("Light");
        light.addChoice("type", settings::lightType, settings::setLightType, LIGHT_CHOICES);
        light.addBoolean("enabled", settings::shadowsEnabled, settings::setShadowsEnabled);
        ControlPanel.Section objects = panel.addSection("Objects");
        objects.addBoolean("cast shadows", settings::castersEnabled, settings::setCastersEnabled);
        objects.addBoolean("receive shadows", settings::receiversEnabled, settings::setReceiversEnabled);
        ControlPanel.Section map = panel.addSection("Shadow map");
        map.addChoice("resolution", settings::mapSize, settings::setMapSize, MAP_SIZE_CHOICES);
        map.addFloat("bias", settings::bias, settings::setBias, -0.005f, 0.02f);
        map.addFloat("normal bias", settings::normalBias, settings::setNormalBias, 0.0f, 0.15f);
        map.addText("passes", () -> shadowPasses(context));
        return panel;
    }

    /** Formats the latest shadow-map and depth-pass activity for the live panel. */
    private static String shadowPasses(ExampleContext context) {
        RenderStatistics statistics = context.renderer().info().statistics();
        return statistics.shadowMaps() + " maps / " + statistics.shadowPasses() + " passes";
    }

    /** Selectable light type used by the example's control panel. */
    private enum LightType {
        DIRECTIONAL,
        SPOT,
        POINT
    }

    /** Explicit bindings that keep the three shadow-capable lights and participating meshes aligned. */
    private static final class ShadowControls {
        private final List<ShadowCastingLight> lights;
        private final List<Mesh> casters;
        private final List<Mesh> receivers;
        private LightType lightType = LightType.DIRECTIONAL;
        private boolean shadowsEnabled = true;
        private boolean castersEnabled = true;
        private boolean receiversEnabled = true;
        private int mapSize = 512;
        private float bias = 0.0025f;
        private float normalBias = 0.06f;

        /** Retains controlled scene objects and applies the initial selected-light state. */
        private ShadowControls(List<ShadowCastingLight> lights, List<Mesh> casters, List<Mesh> receivers) {
            this.lights = List.copyOf(lights);
            this.casters = List.copyOf(casters);
            this.receivers = List.copyOf(receivers);
            applyLightSelection();
        }

        /** Returns the selected shadow-casting light type. */
        private LightType lightType() {
            return lightType;
        }

        /** Selects exactly one visible light while retaining every light's configuration. */
        private void setLightType(LightType lightType) {
            this.lightType = lightType;
            applyLightSelection();
        }

        /** Returns whether shadow-map generation is enabled for the selected light. */
        private boolean shadowsEnabled() {
            return shadowsEnabled;
        }

        /** Enables or disables shadow-map generation without disabling illumination. */
        private void setShadowsEnabled(boolean enabled) {
            shadowsEnabled = enabled;
            lights.forEach(light -> light.setShadowCastingEnabled(enabled));
        }

        /** Returns whether the displayed objects cast shadows. */
        private boolean castersEnabled() {
            return castersEnabled;
        }

        /** Changes shadow casting for every displayed object. */
        private void setCastersEnabled(boolean enabled) {
            castersEnabled = enabled;
            casters.forEach(mesh -> mesh.setShadowCastingEnabled(enabled));
        }

        /** Returns whether the floor receives shadows. */
        private boolean receiversEnabled() {
            return receiversEnabled;
        }

        /** Changes shadow receiving for the displayed floor. */
        private void setReceiversEnabled(boolean enabled) {
            receiversEnabled = enabled;
            receivers.forEach(mesh -> mesh.setShadowReceivingEnabled(enabled));
        }

        /** Returns the common square map dimension. */
        private int mapSize() {
            return mapSize;
        }

        /** Changes every light's requested map dimensions. */
        private void setMapSize(int mapSize) {
            this.mapSize = mapSize;
            lights.forEach(light -> light.shadow().setMapSize(mapSize, mapSize));
        }

        /** Returns the common normalized comparison bias. */
        private float bias() {
            return bias;
        }

        /** Changes every light's normalized comparison bias. */
        private void setBias(float bias) {
            this.bias = bias;
            lights.forEach(light -> light.shadow().setBias(bias));
        }

        /** Returns the common scene-unit receiver normal bias. */
        private float normalBias() {
            return normalBias;
        }

        /** Changes every light's scene-unit receiver normal bias. */
        private void setNormalBias(float normalBias) {
            this.normalBias = normalBias;
            lights.forEach(light -> light.shadow().setNormalBias(normalBias));
        }

        /** Makes only the selected light visible to the renderer. */
        private void applyLightSelection() {
            for (int index = 0; index < lights.size(); index++) {
                lights.get(index).setVisible(index == lightType.ordinal());
            }
        }
    }
}
