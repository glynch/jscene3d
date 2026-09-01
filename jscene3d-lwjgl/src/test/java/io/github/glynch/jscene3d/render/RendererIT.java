/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.joml.Math.toRadians;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.fogs.ExponentialSquaredFog;
import io.github.glynch.jscene3d.fogs.LinearFog;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.CircleGeometry;
import io.github.glynch.jscene3d.geometries.ConeGeometry;
import io.github.glynch.jscene3d.geometries.CylinderGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.geometries.TorusGeometry;
import io.github.glynch.jscene3d.helpers.AxesHelper;
import io.github.glynch.jscene3d.helpers.BoxHelper;
import io.github.glynch.jscene3d.helpers.GridHelper;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.DepthFunction;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.ShaderAttribute;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Bone;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.RenderContext;
import io.github.glynch.jscene3d.objects.RenderPass;
import io.github.glynch.jscene3d.objects.Skeleton;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import io.github.glynch.jscene3d.platform.VerticalSync;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import io.github.glynch.jscene3d.textures.TextureCoordinateSet;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

final class RendererIT {
    private static final int MAXIMUM_COLOR_CHANNEL_VALUE = 255;
    private static final String CUSTOM_VERTEX_SHADER = """
            in vec3 position;

            uniform mat4 modelViewMatrix;
            uniform mat4 projectionMatrix;

            void main() {
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
            """;
    private static final String CUSTOM_FRAGMENT_SHADER = """
            out vec4 fragmentColor;

            uniform vec3 tint;
            uniform float intensity;
            uniform bool enabled;

            void main() {
            #ifdef USE_TINT
                fragmentColor = vec4(enabled ? tint * intensity : vec3(0.0), 1.0);
            #else
                fragmentColor = vec4(1.0, 0.0, 1.0, 1.0);
            #endif
            }
            """;

    @Test
    void rendersIndependentMorphWeightsAcrossOneInstancedDraw() {
        WindowOptions options = WindowOptions.builder()
                .size(320, 240)
                .title("Renderer instanced morph integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createMorphTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 2);
            mesh.setMatrixAt(0, new Matrix4f().translation(-0.6f, 0.0f, 0.0f));
            mesh.setMatrixAt(1, new Matrix4f().translation(0.6f, 0.0f, 0.0f));
            mesh.setMorphTargetInfluenceAt(0, 0, 0.0f);
            mesh.setMorphTargetInfluenceAt(1, 0, 1.0f);
            Scene scene = new Scene();
            scene.add(mesh);
            OrthographicCamera camera = new OrthographicCamera(-1.5f, 1.5f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertPixelIsRed(Math.round(window.framebufferWidth() * 0.30f), window.framebufferHeight() / 2);
            RedBounds rightInstance = redBounds(
                    Math.round(window.framebufferWidth() * 0.60f),
                    Math.round(window.framebufferWidth() * 0.80f),
                    0,
                    window.framebufferHeight() - 1);
            assertThat(rightInstance.minimumY()).isGreaterThan(Math.round(window.framebufferHeight() * 0.60f));
            assertThat(renderer.info().statistics().drawCalls()).isOne();
            assertThat(renderer.info().resources().activeMorphResources()).isEqualTo(2);

            renderer.render(scene, camera);
            assertThat(renderer.info().statistics().bufferUploads()).isZero();

            mesh.setMorphTargetInfluenceAt(1, 0, 0.5f);
            renderer.render(scene, camera);
            assertThat(renderer.info().statistics().bufferUploads()).isOne();
            assertThat(renderer.info().statistics().bufferUploadBytes()).isEqualTo(Float.BYTES);
        }
    }

    @Test
    void realizesEveryBuiltInMeshProgramForMorphedGeometry() {
        WindowOptions options = WindowOptions.builder()
                .size(320, 240)
                .title("Renderer morph material integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();
        Material[] materials = {
            new BasicMaterial(Color.RED),
            new LambertMaterial(Color.RED),
            new NormalMaterial(),
            new PhongMaterial(Color.RED),
            new StandardMaterial(Color.RED)
        };
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createMorphTriangle()) {
            Scene scene = new Scene();
            scene.add(new AmbientLight(Color.WHITE, 1.0f));
            DirectionalLight shadowLight = new DirectionalLight(Color.WHITE, 1.0f);
            shadowLight.setPosition(0.0f, 3.0f, 3.0f);
            shadowLight.setTarget(0.0f, 0.0f, 0.0f);
            shadowLight.setShadowCastingEnabled(true);
            shadowLight.shadow().setMapSize(128, 128);
            scene.add(shadowLight);
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);
            for (Material material : materials) {
                Mesh mesh = new Mesh(geometry, material);
                mesh.setMorphTargetInfluence(0, 1.0f);
                mesh.setShadowCastingEnabled(true);
                scene.add(mesh);
                renderer.render(scene, camera);
                assertThat(renderer.info().statistics().drawCalls()).isOne();
                assertThat(renderer.info().statistics().shadowDrawCalls()).isOne();
                scene.remove(mesh);
            }
        } finally {
            for (Material material : materials) {
                material.close();
            }
        }
    }

    @Test
    void rendersColoredInstancesWithOneDrawAndUploadsOnlyChangedTransformRange() {
        WindowOptions options = WindowOptions.builder()
                .size(320, 240)
                .title("Renderer instancing integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();
        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial()) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 2);
            mesh.setMatrixAt(0, new Matrix4f().translation(-0.65f, 0.0f, 0.0f).scale(0.55f));
            mesh.setMatrixAt(1, new Matrix4f().translation(0.65f, 0.0f, 0.0f).scale(0.55f));
            mesh.setColorAt(0, Color.RED);
            mesh.setColorAt(1, Color.GREEN);
            Scene scene = new Scene();
            scene.add(mesh);
            OrthographicCamera camera = new OrthographicCamera(-1.5f, 1.5f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            RenderStatistics statistics = renderer.info().statistics();
            assertThat(statistics.drawCalls()).isOne();
            assertThat(statistics.visibleMeshes()).isOne();
            assertThat(statistics.renderedInstances()).isEqualTo(2L);
            assertThat(statistics.triangles()).isEqualTo(2L);
            assertThat(renderer.info().resources().activeInstanceResources()).isOne();
            assertPixelIsRed(Math.round(window.framebufferWidth() * 0.28f), window.framebufferHeight() / 2);
            assertPixelIsGreen(Math.round(window.framebufferWidth() * 0.72f), window.framebufferHeight() / 2);

            renderer.render(scene, camera);
            assertThat(statistics.bufferUploads()).isZero();

            mesh.setMatrixAt(1, new Matrix4f().translation(0.7f, 0.0f, 0.0f).scale(0.55f));
            renderer.render(scene, camera);
            assertThat(statistics.bufferUploads()).isOne();
            assertThat(statistics.bufferUploadBytes()).isEqualTo(Float.BYTES);

            scene.remove(mesh);
            renderer.render(scene, camera);
            assertThat(renderer.info().resources().activeInstanceResources()).isZero();
        }
    }

    @Test
    void rendersGeometryAndUploadsOnlyChangedData() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Renderer integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            RendererInfo info = renderer.info();
            RenderStatistics statistics = info.statistics();
            ResourceStatistics resources = info.resources();

            assertThat(statistics.frame()).isEqualTo(1L);
            assertThat(statistics.drawCalls()).isEqualTo(1);
            assertThat(statistics.triangles()).isEqualTo(1L);
            assertThat(statistics.visibleMeshes()).isEqualTo(1);
            assertThat(statistics.culledMeshes()).isZero();
            assertThat(statistics.bufferUploads()).isEqualTo(1);
            assertThat(resources.activeGeometryResources()).isEqualTo(1);
            assertThat(resources.programCount()).isEqualTo(1);
            assertCenterPixelIsRed(window);

            renderer.render(scene, camera);

            assertThat(renderer.info()).isSameAs(info);
            assertThat(info.statistics()).isSameAs(statistics);
            assertThat(info.resources()).isSameAs(resources);
            assertThat(statistics.frame()).isEqualTo(2L);
            assertThat(statistics.bufferUploads()).isZero();

            BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
            positions.setX(0, -0.9f);
            renderer.render(scene, camera);

            assertThat(statistics.frame()).isEqualTo(3L);
            assertThat(statistics.bufferUploads()).isEqualTo(1);

            geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2}));
            renderer.render(scene, camera);

            assertThat(statistics.frame()).isEqualTo(4L);
            assertThat(statistics.drawCalls()).isEqualTo(1);
            assertThat(statistics.bufferUploads()).isEqualTo(1);
        }
    }

    @Test
    void appliesAndClearsSupportedSceneFog() {
        try (Window window = Window.create("Fog integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            scene.setFog(new LinearFog(Color.BLUE, 0.1f, 1.0f));
            renderer.render(scene, camera);
            assertPixelIsBlue(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            scene.setFog(new ExponentialSquaredFog(Color.BLUE, 4.0f));
            renderer.render(scene, camera);
            assertPixelIsBlue(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            scene.clearFog();
            renderer.render(scene, camera);
            assertCenterPixelIsRed(window);
        }
    }

    @Test
    void rendersGeneratedCircleCylinderConeAndTorusGeometry() {
        try (Window window = Window.create("Generated geometry integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry circleGeometry = CircleGeometry.create(0.75f);
                BufferGeometry cylinderGeometry = CylinderGeometry.create(0.6f, 1.5f);
                BufferGeometry coneGeometry = ConeGeometry.create(0.75f, 1.5f);
                BufferGeometry torusGeometry = TorusGeometry.create(0.6f, 0.2f);
                BasicMaterial material = new BasicMaterial(Color.WHITE)) {
            Mesh circle = new Mesh(circleGeometry, material);
            circle.setPosition(-4.5f, 0.0f, 0.0f);
            Mesh cylinder = new Mesh(cylinderGeometry, material);
            cylinder.setPosition(-1.5f, 0.0f, 0.0f);
            Mesh cone = new Mesh(coneGeometry, material);
            cone.setPosition(1.5f, 0.0f, 0.0f);
            Mesh torus = new Mesh(torusGeometry, material);
            torus.setPosition(4.5f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(circle);
            scene.add(cylinder);
            scene.add(cone);
            scene.add(torus);
            OrthographicCamera camera = new OrthographicCamera(-6.0f, 6.0f, 2.5f, -2.5f, 0.1f, 20.0f);
            camera.setPosition(0.0f, 0.0f, 10.0f);

            renderer.render(scene, camera);

            assertThat(renderer.info().statistics().drawCalls()).isEqualTo(4);
            assertThat(renderer.info().statistics().triangles()).isEqualTo(1_376L);
            assertThat(renderer.info().statistics().bufferUploads()).isEqualTo(16);
            assertThat(renderer.info().resources().activeGeometryResources()).isEqualTo(4);
        }
    }

    @Test
    void enforcesOneRendererAndRendererBeforeWindowClosure() {
        Window window = Window.create(320, 240, "Renderer ownership test");
        Renderer renderer = Renderer.create(window);
        try {
            assertThatIllegalStateException().isThrownBy(() -> Renderer.create(window));
            assertThatIllegalStateException()
                    .isThrownBy(window::close)
                    .withMessage("Window cannot close while its renderer is open");
        } finally {
            renderer.close();
            window.close();
        }

        assertThat(renderer.isClosed()).isTrue();
        assertThat(window.isClosed()).isTrue();
    }

    @Test
    void rendersTheBackFaceOfADoubleSidedMaterial() {
        try (Window window = Window.create("Double-sided material test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            material.setSide(MaterialSide.DOUBLE);
            Mesh triangle = new Mesh(geometry, material);
            triangle.rotateY(PI);
            Scene scene = new Scene();
            scene.add(triangle);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsRed(window);
        }
    }

    @Test
    void rendersLambertMaterialWithVisibleAmbientAndParentedPointLights() {
        try (Window window = Window.create("Lambert lighting integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                LambertMaterial material = new LambertMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            AmbientLight ambientLight = new AmbientLight(Color.WHITE);
            scene.add(ambientLight);
            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            ambientLight.setVisible(false);
            Group lightParent = new Group();
            PointLight pointLight = new PointLight(Color.WHITE);
            pointLight.setDecay(0.0f);
            lightParent.setPosition(0.0f, 0.0f, 1.0f);
            lightParent.add(pointLight);
            scene.add(lightParent);
            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            lightParent.setPosition(0.0f, 0.0f, -1.0f);
            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            lightParent.setPosition(0.0f, 0.0f, 1.0f);
            pointLight.setDistance(0.5f);
            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            assertThat(renderer.info().resources().programCount()).isEqualTo(1);
        }
    }

    @Test
    void rendersLambertMaterialWithDirectionalLightWorldPositionAndTarget() {
        try (Window window = Window.create("Directional lighting integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                LambertMaterial material = new LambertMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            Group lightParent = new Group();
            lightParent.setPosition(0.0f, 0.0f, 1.0f);
            DirectionalLight light = new DirectionalLight(Color.WHITE, 1.0f);
            light.setPosition(0.0f, 0.0f, 0.0f);
            lightParent.add(light);
            scene.add(lightParent);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            light.setTarget(0.0f, 0.0f, 2.0f);
            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            light.setTarget(0.0f, 0.0f, 1.0f);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("DirectionalLight position must differ from its target");
        }
    }

    @Test
    void rendersLambertMaterialWithHemisphereSkyAndGroundColors() {
        try (Window window = Window.create("Hemisphere lighting integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                LambertMaterial material = new LambertMaterial(Color.WHITE)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            HemisphereLight light = new HemisphereLight(Color.RED, Color.BLUE);
            light.setPosition(0.0f, 0.0f, 1.0f);
            scene.add(light);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            light.setPosition(0.0f, 0.0f, -1.0f);
            renderer.render(scene, camera);
            assertPixelIsNormalizedBlue(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            light.setPosition(0.0f, 0.0f, 0.0f);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("HemisphereLight world position must not be zero");
        }
    }

    @Test
    void rendersPhongMaterialInsideASpotlightCone() {
        try (Window window = Window.create("Spotlight integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                PhongMaterial material = new PhongMaterial(Color.RED)) {
            material.setSpecular(Color.BLACK);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            SpotLight light = new SpotLight(Color.WHITE);
            light.setPosition(0.0f, 0.0f, 1.0f);
            light.setTarget(0.0f, 0.0f, 0.0f);
            light.setAngle(toRadians(15.0f));
            light.setDecay(0.0f);
            scene.add(light);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            light.setTarget(2.0f, 0.0f, 0.0f);
            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            light.setTarget(0.0f, 0.0f, 1.0f);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("SpotLight position must differ from its target");
        }
    }

    @Test
    void rendersDirectionalShadowsOnLambertMaterials() {
        try (Window window = Window.create("Directional shadow integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                LambertMaterial material = new LambertMaterial(Color.RED)) {
            Mesh mesh = new Mesh(geometry, material);
            mesh.setShadowCastingEnabled(true);
            mesh.setShadowReceivingEnabled(true);
            DirectionalLight light = new DirectionalLight(Color.WHITE);
            light.setPosition(0.0f, 0.0f, 2.0f);
            light.setShadowCastingEnabled(true);
            light.shadow().setMapSize(128, 128);
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(light);
            PerspectiveCamera camera = shadowTestCamera(window);

            renderer.render(scene, camera);

            assertShadowActivity(renderer, 1, 1, 1, 1L);
        }
    }

    @Test
    void restoresRendererStateAfterShadowCallbackFailure() {
        try (Window window = Window.create("Shadow callback failure integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                LambertMaterial material = new LambertMaterial(Color.RED)) {
            Mesh mesh = new Mesh(geometry, material);
            mesh.setShadowCastingEnabled(true);
            mesh.setBeforeShadowRenderCallback(ignored -> {
                throw new IllegalStateException("shadow callback failed");
            });
            DirectionalLight light = new DirectionalLight(Color.WHITE);
            light.setPosition(0.0f, 0.0f, 2.0f);
            light.setShadowCastingEnabled(true);
            light.shadow().setMapSize(128, 128);
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(light);
            PerspectiveCamera camera = shadowTestCamera(window);

            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("shadow callback failed");

            mesh.clearBeforeShadowRenderCallback();
            renderer.render(scene, camera);

            assertCenterPixelIsNormalizedRed(window);
            assertShadowActivity(renderer, 1, 1, 1, 1L);
        }
    }

    @Test
    void rendersStableSolidDirectionalShadowDepth() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 320)
                .title("Directional shadow depth regression test")
                .verticalSync(VerticalSync.DISABLED)
                .build();
        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry receiverGeometry = PlaneGeometry.create(1.4f, 1.4f);
                BufferGeometry casterGeometry = PlaneGeometry.create(0.4f, 0.4f);
                LambertMaterial receiverMaterial = new LambertMaterial(Color.WHITE);
                LambertMaterial casterMaterial = new LambertMaterial(Color.WHITE)) {
            Mesh receiver = new Mesh(receiverGeometry, receiverMaterial);
            receiver.setShadowReceivingEnabled(true);
            Mesh caster = new Mesh(casterGeometry, casterMaterial);
            caster.setPosition(-1.0f, 0.0f, 1.0f);
            caster.setShadowCastingEnabled(true);
            DirectionalLight light = new DirectionalLight(Color.WHITE);
            light.setPosition(-4.0f, 0.0f, 4.0f);
            light.setTarget(0.0f, 0.0f, 0.0f);
            light.setShadowCastingEnabled(true);
            light.shadow().setMapSize(128, 128);
            light.shadow().setCameraBounds(-2.0f, 2.0f, -2.0f, 2.0f);
            light.shadow().setCameraRange(0.1f, 10.0f);
            light.shadow().setBias(0.001f);
            Scene scene = new Scene();
            scene.add(new AmbientLight(Color.WHITE, 0.1f));
            scene.add(receiver);
            scene.add(caster);
            scene.add(light);
            OrthographicCamera camera = new OrthographicCamera(-0.7f, 0.7f, 0.7f, -0.7f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);

            renderer.render(scene, camera);
            OverlayImage baseline = renderer.captureViewport();

            assertSolidShadowCore(baseline);
            assertSmoothShadowEdge(baseline);
            for (int frame = 0; frame < 8; frame++) {
                renderer.render(scene, camera);
                assertThat(renderer.captureViewport().pixels()).isEqualTo(baseline.pixels());
            }
        }
    }

    @Test
    void rendersSpotShadowsOnPhongMaterials() {
        try (Window window = Window.create("Spot shadow integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                PhongMaterial material = new PhongMaterial(Color.RED)) {
            material.setSpecular(Color.BLACK);
            Mesh mesh = new Mesh(geometry, material);
            mesh.setShadowCastingEnabled(true);
            mesh.setShadowReceivingEnabled(true);
            SpotLight light = new SpotLight(Color.WHITE);
            light.setPosition(0.0f, 0.0f, 2.0f);
            light.setTarget(0.0f, 0.0f, 0.0f);
            light.setDecay(0.0f);
            light.setShadowCastingEnabled(true);
            light.shadow().setMapSize(128, 128);
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(light);
            PerspectiveCamera camera = shadowTestCamera(window);

            renderer.render(scene, camera);

            assertShadowActivity(renderer, 1, 1, 1, 1L);
        }
    }

    @Test
    void rendersPointShadowsOnStandardMaterialsAndInvokesCallbacksForEveryCubeFace() {
        try (Window window = Window.create("Point shadow integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                StandardMaterial material = new StandardMaterial(Color.RED)) {
            Mesh mesh = new Mesh(geometry, material);
            mesh.setShadowCastingEnabled(true);
            mesh.setShadowReceivingEnabled(true);
            List<RenderContext> beforeContexts = new ArrayList<>();
            List<RenderContext> afterContexts = new ArrayList<>();
            mesh.setBeforeShadowRenderCallback(beforeContexts::add);
            mesh.setAfterShadowRenderCallback(afterContexts::add);
            PointLight light = new PointLight(Color.WHITE, 10.0f);
            light.setPosition(0.0f, 0.0f, 2.0f);
            light.setDecay(0.0f);
            light.setShadowCastingEnabled(true);
            light.shadow().setMapSize(128, 128);
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(light);
            PerspectiveCamera camera = shadowTestCamera(window);

            renderer.render(scene, camera);

            assertShadowActivity(renderer, 1, 6, 6, 6L);
            assertThat(beforeContexts).hasSize(6).allSatisfy(context -> {
                assertThat(context.scene()).isSameAs(scene);
                assertThat(context.camera()).isSameAs(camera);
                assertThat(context.object()).isSameAs(mesh);
                assertThat(context.geometry()).isSameAs(geometry);
                assertThat(context.material()).isSameAs(material);
                assertThat(context.pass()).isEqualTo(RenderPass.SHADOW);
            });
            assertThat(afterContexts).hasSize(6);
            for (int index = 0; index < beforeContexts.size(); index++) {
                assertThat(afterContexts.get(index)).isSameAs(beforeContexts.get(index));
            }
        }
    }

    @Test
    void rejectsMoreEnabledShadowMapsThanBuiltInShadersCanSample() {
        try (Window window = Window.create("Shadow limit integration test");
                Renderer renderer = Renderer.create(window)) {
            Scene scene = new Scene();
            for (int index = 0; index < 5; index++) {
                DirectionalLight light = new DirectionalLight();
                light.setPosition(index + 1.0f, 2.0f, 3.0f);
                light.setShadowCastingEnabled(true);
                scene.add(light);
            }
            PerspectiveCamera camera = shadowTestCamera(window);

            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage(
                            "Scene has more visible directional and spot shadow maps than Renderer supports: 5 > 4");
        }
    }

    @Test
    void appliesLambertVertexColorsAndColorMaps() {
        Texture texture = Texture.baseColor(1, 1, new byte[] {(byte) 0xff, 0, 0, (byte) 0xff});
        try (Window window = Window.create("Lambert surface inputs integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedGreenTriangle();
                LambertMaterial material = new LambertMaterial()) {
            material.setUsesVertexColors(true);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            scene.add(new AmbientLight(Color.WHITE));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedGreen(window);

            material.setUsesVertexColors(false);
            material.setColorMap(texture);
            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);
            assertThat(renderer.info().statistics().textureUploads()).isEqualTo(1);
            assertThat(renderer.info().resources().activeTextureResources()).isEqualTo(1);

            texture.close();
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("LambertMaterial colorMap is closed");
        } finally {
            texture.close();
        }
    }

    @Test
    void rendersViewSpaceNormalsWithoutSceneLights() {
        try (Window window = Window.create("Normal material integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                NormalMaterial material = new NormalMaterial()) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsPositiveZNormal(window);
            assertThat(renderer.info().resources().programCount()).isEqualTo(1);
        }
    }

    @Test
    void rendersPhongEmissiveAndSpecularContributions() {
        try (Window window = Window.create("Phong lighting integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTriangle();
                PhongMaterial material = new PhongMaterial(Color.BLACK)) {
            material.setEmissive(Color.RED);
            material.setSpecular(Color.WHITE);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsRed(window);

            material.setEmissive(Color.BLACK);
            DirectionalLight light = new DirectionalLight(Color.WHITE, 1.0f);
            light.setPosition(0.0f, 0.0f, 1.0f);
            scene.add(light);
            renderer.render(scene, camera);

            assertCenterPixelIsWhite(window);
        }
    }

    @Test
    void appliesPhongVertexColorsAndColorMapsAndValidatesNormals() {
        Texture texture = Texture.baseColor(1, 1, new byte[] {(byte) 0xff, 0, 0, (byte) 0xff});
        try (Window window = Window.create("Phong surface inputs integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedGreenTriangle();
                PhongMaterial material = new PhongMaterial()) {
            material.setSpecular(Color.BLACK);
            material.setUsesVertexColors(true);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            scene.add(new AmbientLight(Color.WHITE));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedGreen(window);

            material.setUsesVertexColors(false);
            material.setColorMap(texture);
            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);

            texture.close();
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("PhongMaterial colorMap is closed");

            material.clearColorMap();
            scene.clear();
            try (BufferGeometry unlitGeometry = createTriangle()) {
                scene.add(new Mesh(unlitGeometry, material));
                assertThatIllegalStateException()
                        .isThrownBy(() -> renderer.render(scene, camera))
                        .withMessage("PhongMaterial requires a normal attribute but geometry has none");
            }
        } finally {
            texture.close();
        }
    }

    @Test
    void validatesLambertNormalsAndVisibleLightLimits() {
        try (Window window = Window.create("Lambert validation integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                LambertMaterial material = new LambertMaterial()) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            scene.add(new AmbientLight());
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("LambertMaterial requires a normal attribute but geometry has none");

            scene.clear();
            PointLight lastLight = null;
            for (int index = 0; index <= Renderer.MAX_POINT_LIGHTS; index++) {
                lastLight = new PointLight();
                scene.add(lastLight);
            }
            PointLight excessLight = Objects.requireNonNull(lastLight);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("Scene has more visible point lights than Renderer supports: 9 > 8");

            excessLight.setVisible(false);
            renderer.render(scene, camera);
            assertThat(renderer.info().statistics().drawCalls()).isZero();

            scene.clear();
            for (int index = 0; index <= Renderer.MAX_DIRECTIONAL_LIGHTS; index++) {
                scene.add(new DirectionalLight());
            }
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("Scene has more visible directional lights than Renderer supports: 9 > 8");

            scene.clear();
            for (int index = 0; index <= Renderer.MAX_SPOT_LIGHTS; index++) {
                scene.add(new SpotLight());
            }
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("Scene has more visible spotlights than Renderer supports: 9 > 8");

            scene.clear();
            for (int index = 0; index <= Renderer.MAX_HEMISPHERE_LIGHTS; index++) {
                scene.add(new HemisphereLight());
            }
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("Scene has more visible hemisphere lights than Renderer supports: 9 > 8");
        }
    }

    @Test
    void rendersMultipleObjectsWithHierarchyVisibilityAndSharedResources() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Multiple object integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createSmallTriangle();
                BasicMaterial redMaterial = new BasicMaterial(Color.RED);
                BasicMaterial greenMaterial = new BasicMaterial(Color.GREEN)) {
            Mesh leftTriangle = new Mesh(geometry, redMaterial);
            leftTriangle.setPosition(-0.6f, 0.0f, 0.0f);

            Group translatedParent = new Group();
            translatedParent.setPosition(0.5f, 0.0f, 0.0f);
            Mesh inheritedTriangle = new Mesh(geometry, redMaterial);
            inheritedTriangle.setPosition(0.1f, 0.0f, 0.0f);
            translatedParent.add(inheritedTriangle);

            Group hiddenParent = new Group();
            hiddenParent.setVisible(false);
            hiddenParent.add(new Mesh(geometry, greenMaterial));

            Mesh outsideTriangle = new Mesh(geometry, redMaterial);
            outsideTriangle.setPosition(3.0f, 0.0f, 0.0f);

            Scene scene = new Scene();
            scene.add(leftTriangle);
            scene.add(translatedParent);
            scene.add(hiddenParent);
            scene.add(outsideTriangle);
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            RenderStatistics statistics = renderer.info().statistics();
            ResourceStatistics resources = renderer.info().resources();
            assertThat(statistics.drawCalls()).isEqualTo(2);
            assertThat(statistics.visibleMeshes()).isEqualTo(2);
            assertThat(statistics.culledMeshes()).isEqualTo(1);
            assertThat(statistics.triangles()).isEqualTo(2L);
            assertThat(statistics.bufferUploads()).isEqualTo(1);
            assertThat(resources.activeGeometryResources()).isEqualTo(1);
            int centerY = window.framebufferHeight() / 2;
            assertPixelIsRed(Math.round(window.framebufferWidth() * 0.2f), centerY);
            assertPixelIsRed(Math.round(window.framebufferWidth() * 0.8f), centerY);
            assertPixelIsBlack(window.framebufferWidth() / 2, centerY);

            outsideTriangle.setFrustumCullingEnabled(false);
            renderer.render(scene, camera);

            assertThat(statistics.drawCalls()).isEqualTo(3);
            assertThat(statistics.visibleMeshes()).isEqualTo(3);
            assertThat(statistics.culledMeshes()).isZero();
            assertThat(statistics.bufferUploads()).isZero();
            assertThat(resources.activeGeometryResources()).isEqualTo(1);
        }
    }

    @Test
    void rendersSolidAndAlphaMaskOverlaysThroughOwnedRendererState() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Renderer overlay integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);
            OverlayImage alphaMask = OverlayImage.alphaMask(1, 1, new byte[] {(byte) 0xff});
            Overlay overlay = (canvas, width, height) -> {
                canvas.rectangle(width - 30.0f, 10.0f, 20.0f, 20.0f, Color.WHITE, 1.0f);
                canvas.alphaMask(alphaMask.fullRegion(), 10.0f, 10.0f, 20.0f, 20.0f, Color.WHITE, 1.0f);
            };

            renderer.render(scene, camera);
            renderer.render(overlay);

            assertThat(renderer.info().resources().programCount()).isEqualTo(2);
            int panelX = Math.round((window.width() - 20.0f) * window.framebufferWidth() / window.width());
            int panelY = Math.round((window.height() - 20.0f) * window.framebufferHeight() / window.height());
            assertPixelIsNotBlack(panelX, panelY);
        }
    }

    @Test
    void uploadsAndSamplesASharedColorMapOnlyWhenItsImageChanges() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Texture integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        byte[] pixels = {(byte) 0xff, 0, 0, (byte) 0xff, 0, 0, (byte) 0xff, (byte) 0xff};
        Texture texture = Texture.baseColor(1, 2, pixels);
        texture.setMinificationFilter(TextureFilter.NEAREST);
        texture.setMagnificationFilter(TextureFilter.NEAREST);
        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTexturedTriangle();
                BasicMaterial material = new BasicMaterial()) {
            material.setColorMap(texture);
            Mesh leftTriangle = new Mesh(geometry, material);
            leftTriangle.setPosition(-0.4f, 0.0f, 0.0f);
            Mesh rightTriangle = new Mesh(geometry, material);
            rightTriangle.setPosition(0.4f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(leftTriangle);
            scene.add(rightTriangle);
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            RenderStatistics statistics = renderer.info().statistics();
            ResourceStatistics resources = renderer.info().resources();
            assertThat(statistics.textureUploads()).isEqualTo(1);
            assertThat(statistics.textureUploadBytes()).isEqualTo(8L);
            assertThat(resources.activeTextureResources()).isEqualTo(1);
            int upperY = Math.round(window.framebufferHeight() * 0.56f);
            int lowerY = Math.round(window.framebufferHeight() * 0.44f);
            int leftX = Math.round(window.framebufferWidth() * 0.3f);
            int rightX = Math.round(window.framebufferWidth() * 0.7f);
            assertPixelIsRed(leftX, upperY);
            assertPixelIsBlue(leftX, lowerY);
            assertPixelIsRed(rightX, upperY);
            assertPixelIsBlue(rightX, lowerY);

            renderer.render(scene, camera);
            assertThat(statistics.textureUploads()).isZero();

            texture.setHorizontalWrap(TextureWrap.REPEAT);
            renderer.render(scene, camera);
            assertThat(statistics.textureUploads()).isZero();

            texture.setImage(1, 2, pixels);
            renderer.render(scene, camera);
            assertThat(statistics.textureUploads()).isEqualTo(1);

            texture.close();
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("BasicMaterial colorMap is closed");
            assertThat(resources.activeTextureResources()).isZero();
        } finally {
            texture.close();
        }
    }

    @Test
    void appliesTextureTransformsToBasicAndLambertWithoutReuploadingTheImage() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Texture transform integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();
        byte[] pixels = {(byte) 0xff, 0, 0, (byte) 0xff, 0, 0, (byte) 0xff, (byte) 0xff};
        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedTriangle();
                Texture texture = Texture.baseColor(1, 2, pixels);
                BasicMaterial basicMaterial = new BasicMaterial();
                LambertMaterial lambertMaterial = new LambertMaterial()) {
            texture.setMinificationFilter(TextureFilter.NEAREST);
            texture.setMagnificationFilter(TextureFilter.NEAREST);
            basicMaterial.setColorMap(texture);
            lambertMaterial.setColorMap(texture);
            Mesh basicTriangle = new Mesh(geometry, basicMaterial);
            basicTriangle.setPosition(-0.4f, 0.0f, 0.0f);
            Mesh lambertTriangle = new Mesh(geometry, lambertMaterial);
            lambertTriangle.setPosition(0.4f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(basicTriangle);
            scene.add(lambertTriangle);
            scene.add(new AmbientLight(Color.WHITE));
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);
            int upperY = Math.round(window.framebufferHeight() * 0.56f);
            int lowerY = Math.round(window.framebufferHeight() * 0.44f);
            int leftX = Math.round(window.framebufferWidth() * 0.3f);
            int rightX = Math.round(window.framebufferWidth() * 0.7f);

            renderer.render(scene, camera);
            assertPixelIsRed(leftX, upperY);
            assertPixelIsBlue(leftX, lowerY);
            assertPixelIsNormalizedRed(rightX, upperY);
            assertPixelIsNormalizedBlue(rightX, lowerY);

            texture.setOffset(0.0f, -0.5f);
            renderer.render(scene, camera);
            assertPixelIsBlue(leftX, upperY);
            assertPixelIsBlue(leftX, lowerY);
            assertPixelIsNormalizedBlue(rightX, upperY);
            assertPixelIsNormalizedBlue(rightX, lowerY);
            assertThat(renderer.info().statistics().textureUploads()).isZero();

            texture.setOffset(0.0f, 0.0f);
            texture.setCenter(0.5f, 0.5f);
            texture.setRotation((float) Math.PI);
            renderer.render(scene, camera);
            assertPixelIsBlue(leftX, upperY);
            assertPixelIsRed(leftX, lowerY);
            assertPixelIsNormalizedBlue(rightX, upperY);
            assertPixelIsNormalizedRed(rightX, lowerY);
            assertThat(renderer.info().statistics().textureUploads()).isZero();
        }
    }

    @Test
    void honorsTopLeftCoordinatesAcrossBuiltInTextureMaterials() {
        byte[] pixels = {(byte) 0xff, 0, 0, (byte) 0xff, 0, 0, (byte) 0xff, (byte) 0xff};
        try (Window window = Window.create("Texture coordinate origin integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedTriangle();
                Texture texture = Texture.baseColor(1, 2, pixels);
                BasicMaterial basic = new BasicMaterial();
                LambertMaterial lambert = new LambertMaterial();
                PhongMaterial phong = new PhongMaterial();
                StandardMaterial standard = new StandardMaterial()) {
            texture.setMinificationFilter(TextureFilter.NEAREST);
            texture.setMagnificationFilter(TextureFilter.NEAREST);
            texture.setCoordinateOrigin(TextureCoordinateOrigin.TOP_LEFT);
            basic.setColorMap(texture);
            lambert.setColorMap(texture);
            phong.setColorMap(texture);
            phong.setSpecular(Color.BLACK);
            standard.setColorMap(texture);
            Mesh basicMesh = new Mesh(geometry, basic);
            basicMesh.setPosition(-0.6f, 0.0f, 0.0f);
            Mesh lambertMesh = new Mesh(geometry, lambert);
            lambertMesh.setPosition(-0.2f, 0.0f, 0.0f);
            Mesh phongMesh = new Mesh(geometry, phong);
            phongMesh.setPosition(0.2f, 0.0f, 0.0f);
            Mesh standardMesh = new Mesh(geometry, standard);
            standardMesh.setPosition(0.6f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(basicMesh);
            scene.add(lambertMesh);
            scene.add(phongMesh);
            scene.add(standardMesh);
            scene.add(new AmbientLight(Color.WHITE));
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            int upperY = Math.round(window.framebufferHeight() * 0.56f);
            int lowerY = Math.round(window.framebufferHeight() * 0.44f);
            assertPixelIsBlue(Math.round(window.framebufferWidth() * 0.2f), upperY);
            assertPixelIsRed(Math.round(window.framebufferWidth() * 0.2f), lowerY);
            for (float horizontalPosition : new float[] {0.4f, 0.6f, 0.8f}) {
                int horizontalPixel = Math.round(window.framebufferWidth() * horizontalPosition);
                assertPixelIsNormalizedBlue(horizontalPixel, upperY);
                assertPixelIsNormalizedRed(horizontalPixel, lowerY);
            }
        }
    }

    @Test
    void rendersTypedCustomUniformsAndReusesTheStructuralProgram() {
        WindowOptions windowOptions = WindowOptions.builder()
                .size(320, 240)
                .title("Shader material integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        try (Window window = Window.create(windowOptions);
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                ShaderMaterial firstMaterial = createCustomMaterial();
                ShaderMaterial secondMaterial = createCustomMaterial()) {
            firstMaterial.setUniform("tint", Color.RED);
            firstMaterial.setUniform("intensity", 1.0f);
            firstMaterial.setUniform("enabled", true);
            secondMaterial.setUniform("tint", Color.BLUE);
            secondMaterial.setUniform("intensity", 1.0f);
            secondMaterial.setUniform("enabled", true);

            Mesh triangle = new Mesh(geometry, firstMaterial);
            Scene scene = new Scene();
            scene.add(triangle);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsRed(window);
            assertThat(renderer.info().resources().programCount()).isEqualTo(1);

            triangle.setMaterial(secondMaterial);
            renderer.render(scene, camera);

            assertPixelIsBlue(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
            assertThat(renderer.info().resources().programCount()).isEqualTo(1);
        }
    }

    @Test
    void reportsMissingCustomUniformsAndNumberedCompilationFailures() {
        try (Window window = Window.create("Shader diagnostics integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                ShaderMaterial missingUniform = createCustomMaterial();
                ShaderMaterial invalidShader =
                        new ShaderMaterial("in vec3 position; void main() { invalidToken }", CUSTOM_FRAGMENT_SHADER)) {
            Scene scene = new Scene();
            Mesh triangle = new Mesh(geometry, missingUniform);
            scene.add(triangle);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("ShaderMaterial has no value for active uniform:");

            triangle.setMaterial(invalidShader);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("vertex shader compilation failed")
                    .withMessageContaining("Numbered source:");
        }
    }

    @Test
    void bindsCustomTextureUniformsAndDeclaredTextureCoordinates() {
        String vertexShader = """
                in vec3 position;
                in vec2 uv;
                uniform mat4 modelViewMatrix;
                uniform mat4 projectionMatrix;
                out vec2 textureCoordinate;
                void main() {
                    textureCoordinate = uv;
                    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
                }
                """;
        String fragmentShader = """
                in vec2 textureCoordinate;
                uniform sampler2D colorMap;
                out vec4 fragmentColor;
                void main() {
                    fragmentColor = texture(colorMap, vec2(textureCoordinate.x, 1.0 - textureCoordinate.y));
                }
                """;
        try (Window window = Window.create("Custom texture uniform integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTexturedTriangle();
                Texture texture = Texture.baseColor(1, 1, new byte[] {(byte) 0xff, 0, 0, (byte) 0xff});
                ShaderMaterial material = ShaderMaterial.builder(vertexShader, fragmentShader)
                        .requireAttribute(ShaderAttribute.UV)
                        .build()) {
            material.setUniform("colorMap", texture);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsRed(window);
            assertThat(renderer.info().statistics().textureUploads()).isEqualTo(1);
            assertThat(renderer.info().resources().activeTextureResources()).isEqualTo(1);
        }
    }

    @Test
    void supportsExplicitFrameControlAndRejectsUseAfterClosure() {
        RendererOptions options = RendererOptions.builder()
                .automaticClear(false)
                .clearColor(Color.BLUE)
                .clearAlpha(0.5f)
                .build();
        try (Window window = Window.create("Renderer lifecycle integration test")) {
            Renderer renderer = Renderer.create(window, options);
            Scene scene = new Scene();
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);

            renderer.setViewport(1, 2, 100, 80);
            renderer.clear();
            renderer.resetViewport();
            renderer.setClearColor(Color.RED, 0.75f);
            renderer.render(scene, camera);
            renderer.render((canvas, width, height) -> canvas.clear());

            assertThatIllegalArgumentException().isThrownBy(() -> renderer.setViewport(-1, 0, 1, 1));
            assertThatIllegalArgumentException().isThrownBy(() -> renderer.setViewport(0, -1, 1, 1));
            assertThatIllegalArgumentException().isThrownBy(() -> renderer.setViewport(0, 0, 0, 1));
            assertThatIllegalArgumentException().isThrownBy(() -> renderer.setViewport(0, 0, 1, 0));
            assertThatIllegalArgumentException().isThrownBy(() -> renderer.setClearColor(Color.BLACK, 1.1f));

            renderer.close();
            renderer.close();

            assertThat(renderer.isClosed()).isTrue();
            assertThatIllegalStateException().isThrownBy(renderer::clear).withMessage("Renderer is closed");
            assertThatIllegalStateException()
                    .isThrownBy(renderer::resetViewport)
                    .withMessage("Renderer is closed");
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.setViewport(0, 0, 1, 1))
                    .withMessage("Renderer is closed");
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.setClearColor(Color.BLACK, 1.0f))
                    .withMessage("Renderer is closed");
            assertThatIllegalStateException().isThrownBy(renderer::info).withMessage("Renderer is closed");
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("Renderer is closed");
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render((canvas, width, height) -> canvas.clear()))
                    .withMessage("Renderer is closed");
        }
    }

    @Test
    void releasesClosedGeometryAndTextureRealizations() {
        Texture texture = Texture.baseColor(1, 1, new byte[] {(byte) 255, 0, 0, (byte) 255});
        try (Window window = Window.create("Renderer resource release test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTexturedTriangle();
                BasicMaterial material = new BasicMaterial()) {
            material.setColorMap(texture);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);
            renderer.render(scene, camera);

            assertThat(renderer.info().resources().activeGeometryResources()).isOne();
            assertThat(renderer.info().resources().activeTextureResources()).isOne();

            scene.clear();
            geometry.close();
            texture.close();
            renderer.render(scene, camera);

            assertThat(renderer.info().resources().activeGeometryResources()).isZero();
            assertThat(renderer.info().resources().activeTextureResources()).isZero();
        } finally {
            texture.close();
        }
    }

    @Test
    void uploadsEverySupportedApplicationUniformTypeAndReportsMismatches() {
        String fragmentShader = """
                uniform int choice;
                uniform vec2 offset;
                uniform vec4 tint;
                uniform mat3 transform3;
                uniform mat4 transform4;
                out vec4 fragmentColor;
                void main() {
                    vec3 transformed = transform3 * tint.rgb;
                    vec4 transformed4 = transform4 * vec4(transformed, tint.a);
                    fragmentColor = transformed4 + vec4(offset, float(choice), 0.0) * 0.001;
                }
                """;
        String mismatchedFragmentShader = """
                uniform float intensity;
                out vec4 fragmentColor;
                void main() {
                    fragmentColor = vec4(intensity);
                }
                """;

        try (Window window = Window.create("Complete shader uniform integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                ShaderMaterial material = new ShaderMaterial(CUSTOM_VERTEX_SHADER, fragmentShader);
                ShaderMaterial mismatched = new ShaderMaterial(CUSTOM_VERTEX_SHADER, mismatchedFragmentShader)) {
            material.setUniform("choice", 1);
            material.setUniform("offset", 0.0f, 0.0f);
            material.setUniform("tint", 1.0f, 0.0f, 0.0f, 1.0f);
            material.setUniform("transform3", new Matrix3f());
            material.setUniform("transform4", new Matrix4f());
            mismatched.setUniform("intensity", 1);
            Mesh triangle = new Mesh(geometry, material);
            Scene scene = new Scene();
            scene.add(triangle);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);
            assertCenterPixelIsRed(window);

            triangle.setMaterial(mismatched);
            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessageContaining("uniform intensity is configured as INTEGER")
                    .withMessageContaining("active GLSL expects FLOAT");
        }
    }

    @Test
    void rendersTransparentBackFacesWithoutDepthState() {
        try (Window window = Window.create("Material state integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            material.setSide(MaterialSide.BACK);
            material.setTransparent(true);
            material.setOpacity(0.5f);
            material.setDepthTestEnabled(false);
            material.setDepthWriteEnabled(false);
            Mesh triangle = new Mesh(geometry, material);
            triangle.rotateY(PI);
            Scene scene = new Scene();
            scene.add(triangle);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertPixelIsNotBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
        }
    }

    @Test
    void rendersStandardMaterialTextureRolesAndMaskedAlpha() {
        byte opaque = (byte) 255;
        try (Window window = Window.create("Standard material integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedTriangle();
                StandardMaterial material = new StandardMaterial();
                Texture colorMap = Texture.baseColor(1, 1, new byte[] {opaque, 0, 0, opaque});
                Texture propertiesMap = Texture.data(1, 1, new byte[] {0, opaque, 0, opaque});
                Texture normalMap = Texture.data(1, 1, new byte[] {(byte) 128, (byte) 128, opaque, opaque});
                Texture occlusionMap = Texture.data(1, 1, new byte[] {opaque, opaque, opaque, opaque});
                Texture emissiveMap = Texture.baseColor(1, 1, new byte[] {opaque, opaque, opaque, opaque})) {
            material.setColorMap(colorMap);
            material.setMetalnessRoughnessMap(propertiesMap);
            material.setNormalMap(normalMap);
            material.setOcclusionMap(occlusionMap);
            material.setEmissiveMap(emissiveMap);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            scene.add(new AmbientLight(Color.WHITE));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsNormalizedRed(window);
            assertThat(renderer.info().resources().activeTextureResources()).isEqualTo(5);
            assertThat(renderer.info().resources().programCount()).isOne();

            colorMap.setImage(1, 1, new byte[] {opaque, 0, 0, 0});
            material.setAlphaMode(AlphaMode.MASK);
            renderer.render(scene, camera);
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            material.setAlphaCutoff(0.0f);
            renderer.render(scene, camera);
            assertCenterPixelIsNormalizedRed(window);
        }
    }

    @Test
    void rendersSecondaryTextureCoordinates() {
        byte opaque = (byte) 255;
        try (Window window = Window.create("Secondary texture coordinates integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createLitTexturedTriangle();
                StandardMaterial material = new StandardMaterial();
                Texture colorMap = Texture.baseColor(2, 1, new byte[] {opaque, 0, 0, opaque, 0, opaque, 0, opaque})) {
            geometry.setAttribute(
                    BufferGeometry.UV1, BufferAttribute.of(new float[] {0.75f, 0.5f, 0.75f, 0.5f, 0.75f, 0.5f}, 2));
            colorMap.setMagnificationFilter(TextureFilter.NEAREST);
            colorMap.setHorizontalWrap(TextureWrap.CLAMP_TO_EDGE);
            material.setColorMap(colorMap);
            material.setColorMapCoordinateSet(TextureCoordinateSet.SECONDARY);
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            scene.add(new AmbientLight(Color.WHITE));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsNormalizedGreen(window);
        }
    }

    @Test
    void rendersGpuSkinnedStandardMaterial() {
        try (Window window = Window.create("Skinning integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createSkinnedTriangle();
                StandardMaterial material = new StandardMaterial(Color.RED)) {
            Bone bone = new Bone();
            Skeleton skeleton = Skeleton.fromCurrentPose(List.of(bone));
            SkinnedMesh mesh = new SkinnedMesh(geometry, material, skeleton);
            mesh.add(bone);
            bone.setPosition(1.0f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(new AmbientLight(Color.WHITE));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertCenterPixelIsNormalizedRed(window);
        }
    }

    @Test
    void rendersHdrEnvironmentLightingBackgroundAndToneMapping() {
        RendererOptions options = RendererOptions.builder()
                .toneMapping(ToneMapping.ACES_FILMIC)
                .exposure(0.8f)
                .build();
        float[] radiance = {4.0f, 2.0f, 1.0f, 4.0f, 2.0f, 1.0f};
        try (Window window = Window.create(320, 240, "Environment lighting integration test");
                Renderer renderer = Renderer.create(window, options);
                EnvironmentMap environment = EnvironmentMap.equirectangular(2, 1, radiance);
                BufferGeometry geometry = createLitTriangle();
                StandardMaterial material = new StandardMaterial(Color.WHITE)) {
            material.setMetalness(1.0f);
            material.setRoughness(0.25f);
            Scene scene = new Scene();
            scene.setEnvironment(environment);
            scene.setBackgroundEnvironment(environment);
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertPixelIsNotBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
            assertPixelIsNotBlack(8, 8);
            assertThat(renderer.info().statistics().textureUploads()).isEqualTo(2);
            assertThat(renderer.info().resources().activeTextureResources()).isEqualTo(4);
            assertThat(renderer.info().resources().programCount()).isEqualTo(3);
        }
    }

    @Test
    void invokesMainCallbacksOnlyForDrawnObjectsAndAppliesMaterialChangesToTheSelectedDraw() {
        try (Window window = Window.create(320, 240, "Main render callback integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry meshGeometry = createTriangle();
                BufferGeometry lineGeometry = BufferGeometry.builder()
                        .positions(-0.8f, 0.7f, 0.0f, 0.8f, 0.7f, 0.0f)
                        .build();
                BasicMaterial meshMaterial = new BasicMaterial(Color.BLUE);
                LineBasicMaterial lineMaterial = new LineBasicMaterial(Color.WHITE)) {
            List<String> events = new ArrayList<>();
            List<RenderContext> contexts = new ArrayList<>();
            Mesh mesh = new Mesh(meshGeometry, meshMaterial);
            mesh.setBeforeRenderCallback(context -> {
                events.add("mesh-before");
                contexts.add(context);
                meshMaterial.setColor(Color.RED);
            });
            mesh.setAfterRenderCallback(context -> {
                events.add("mesh-after");
                contexts.add(context);
                meshMaterial.setColor(Color.BLUE);
            });
            LineSegments line = new LineSegments(lineGeometry, lineMaterial);
            line.setRenderOrder(1);
            line.setBeforeRenderCallback(context -> {
                events.add("line-before");
                contexts.add(context);
            });
            line.setAfterRenderCallback(context -> {
                events.add("line-after");
                contexts.add(context);
            });
            Mesh culled = new Mesh(meshGeometry, meshMaterial);
            culled.setPosition(100.0f, 0.0f, 0.0f);
            culled.setBeforeRenderCallback(ignored -> events.add("culled-before"));
            Scene scene = new Scene();
            scene.add(mesh);
            scene.add(line);
            scene.add(culled);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertThat(events).containsExactly("mesh-before", "mesh-after", "line-before", "line-after");
            assertThat(contexts).allSatisfy(context -> {
                assertThat(context.scene()).isSameAs(scene);
                assertThat(context.camera()).isSameAs(camera);
                assertThat(context.pass()).isEqualTo(RenderPass.MAIN);
            });
            assertThat(contexts.get(0).object()).isSameAs(mesh);
            assertThat(contexts.get(0).geometry()).isSameAs(meshGeometry);
            assertThat(contexts.get(0).material()).isSameAs(meshMaterial);
            assertThat(contexts.get(0)).isSameAs(contexts.get(1));
            assertThat(contexts.get(2).object()).isSameAs(line);
            assertThat(contexts.get(2)).isSameAs(contexts.get(3));
            assertCenterPixelIsRed(window);
            assertThat(meshMaterial.color()).isEqualTo(Color.BLUE);
        }
    }

    @Test
    void propagatesCallbackFailuresAndDoesNotInvokeAfterRenderForAnUnsuccessfulDraw() {
        try (Window window = Window.create(320, 240, "Render callback failure integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            List<String> events = new ArrayList<>();
            Mesh mesh = new Mesh(geometry, material);
            mesh.setBeforeRenderCallback(ignored -> {
                events.add("before");
                throw new IllegalStateException("callback failed");
            });
            mesh.setAfterRenderCallback(ignored -> events.add("after"));
            Scene scene = new Scene();
            scene.add(mesh);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            assertThatIllegalStateException()
                    .isThrownBy(() -> renderer.render(scene, camera))
                    .withMessage("callback failed");
            assertThat(events).containsExactly("before");

            mesh.clearBeforeRenderCallback();
            renderer.render(scene, camera);

            assertThat(events).containsExactly("before", "after");
            assertCenterPixelIsRed(window);
        }
    }

    @Test
    void rendersConnectedAndIndexedLineSegmentsWithVertexColors() {
        try (Window window = Window.create(320, 240, "Line rendering integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry stripGeometry = BufferGeometry.builder()
                        .positions(-0.8f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.8f, 0.0f, 0.0f)
                        .build();
                BufferGeometry segmentsGeometry = BufferGeometry.builder()
                        .positions(-0.8f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.8f, 0.0f, 0.0f)
                        .vertexColors(Color.GREEN, Color.GREEN, Color.GREEN)
                        .indices(0, 1, 1, 2)
                        .build();
                LineBasicMaterial redMaterial = new LineBasicMaterial(Color.RED);
                LineBasicMaterial vertexColorMaterial = new LineBasicMaterial()) {
            vertexColorMaterial.setUsesVertexColors(true);
            Scene scene = new Scene();
            scene.add(new Line(stripGeometry, redMaterial));
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            RenderStatistics statistics = renderer.info().statistics();
            assertThat(statistics.drawCalls()).isOne();
            assertThat(statistics.lineSegments()).isEqualTo(2L);
            assertThat(statistics.visibleLines()).isOne();
            assertThat(statistics.triangles()).isZero();
            assertThat(statistics.visibleMeshes()).isZero();
            assertNeighborhoodContainsRed(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            scene.clear();
            scene.add(new LineSegments(segmentsGeometry, vertexColorMaterial));
            renderer.render(scene, camera);

            assertThat(statistics.drawCalls()).isOne();
            assertThat(statistics.lineSegments()).isEqualTo(2L);
            assertThat(statistics.visibleLines()).isOne();
            assertThat(renderer.info().resources().programCount()).isOne();
            assertNeighborhoodContainsGreen(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
        }
    }

    @Test
    void rendersGeneratedLineHelpers() {
        try (Window window = Window.create(320, 240, "Line helpers integration test");
                AxesHelper axes = new AxesHelper();
                GridHelper grid = new GridHelper(2.0f, 2);
                Renderer renderer = Renderer.create(window)) {
            Scene scene = new Scene();
            scene.add(grid);
            scene.add(axes);
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 10.0f);
            camera.setPosition(2.0f, 2.0f, 2.0f);
            camera.lookAt(0.0f, 0.0f, 0.0f);

            renderer.render(scene, camera);

            RenderStatistics statistics = renderer.info().statistics();
            assertThat(statistics.drawCalls()).isEqualTo(2);
            assertThat(statistics.lineSegments()).isEqualTo(9L);
            assertThat(statistics.visibleLines()).isEqualTo(2);
            assertThat(statistics.triangles()).isZero();
            assertThat(renderer.info().resources().activeGeometryResources()).isEqualTo(2);
            assertThat(renderer.info().resources().programCount()).isOne();
        }
    }

    @Test
    void rendersAndUpdatesBoxHelperBounds() {
        try (Window window = Window.create(320, 240, "Box helper integration test");
                BufferGeometry boxGeometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BasicMaterial boxMaterial = new BasicMaterial(Color.srgb(0x404040));
                Renderer renderer = Renderer.create(window)) {
            Mesh box = new Mesh(boxGeometry, boxMaterial);
            try (BoxHelper helper = new BoxHelper(box)) {
                Scene scene = new Scene();
                scene.add(box);
                scene.add(helper);
                PerspectiveCamera camera =
                        new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 10.0f);
                camera.setPosition(2.0f, 2.0f, 2.0f);
                camera.lookAt(0.0f, 0.0f, 0.0f);

                renderer.render(scene, camera);

                RenderStatistics statistics = renderer.info().statistics();
                assertThat(statistics.drawCalls()).isEqualTo(2);
                assertThat(statistics.triangles()).isEqualTo(12L);
                assertThat(statistics.lineSegments()).isEqualTo(12L);
                assertThat(statistics.visibleMeshes()).isOne();
                assertThat(statistics.visibleLines()).isOne();

                box.setPosition(0.25f, 0.0f, 0.0f);
                helper.update();
                renderer.render(scene, camera);

                assertThat(statistics.bufferUploads()).isOne();
                assertThat(statistics.drawCalls()).isEqualTo(2);
            }
        }
    }

    @Test
    void usesRenderOrderAndDepthFunctionToResolveCoincidentLines() {
        try (Window window = Window.create(320, 240, "Coplanar line ordering integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = BufferGeometry.builder()
                        .positions(-0.8f, 0.0f, 0.0f, 0.8f, 0.0f, 0.0f)
                        .build();
                LineBasicMaterial gridMaterial = new LineBasicMaterial(Color.GRAY);
                LineBasicMaterial axisMaterial = new LineBasicMaterial(Color.RED);
                LineBasicMaterial foregroundMaterial = new LineBasicMaterial(Color.BLUE)) {
            LineSegments gridLine = new LineSegments(geometry, gridMaterial);
            LineSegments axisLine = new LineSegments(geometry, axisMaterial);
            axisLine.setRenderOrder(1);
            axisMaterial.setDepthFunction(DepthFunction.LESS_OR_EQUAL);
            Scene scene = new Scene();
            scene.add(gridLine);
            scene.add(axisLine);
            OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.1f, 10.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            renderer.render(scene, camera);

            assertNeighborhoodContainsRed(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

            LineSegments foregroundLine = new LineSegments(geometry, foregroundMaterial);
            foregroundLine.setPosition(0.0f, 0.0f, 0.1f);
            scene.add(foregroundLine);
            renderer.render(scene, camera);

            assertNeighborhoodContainsBlue(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
        }
    }

    @Test
    void mapsEveryMaterialDepthFunction() {
        try (Window window = Window.create(320, 240, "Depth function integration test");
                Renderer renderer = Renderer.create(window);
                BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Scene scene = new Scene();
            scene.add(new Mesh(geometry, material));
            PerspectiveCamera camera =
                    new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 2.0f);

            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.NEVER, false);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.LESS, true);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.EQUAL, false);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.LESS_OR_EQUAL, true);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.GREATER, false);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.NOT_EQUAL, true);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.GREATER_OR_EQUAL, false);
            assertDepthFunctionResult(window, renderer, scene, camera, material, DepthFunction.ALWAYS, true);
        }
    }

    private static BufferGeometry createTriangle() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(new float[] {-0.8f, -0.8f, 0.0f, 0.8f, -0.8f, 0.0f, 0.0f, 0.8f, 0.0f}, 3));
        return geometry;
    }

    /** Creates the shared camera used by focused shadow-map integration tests. */
    private static PerspectiveCamera shadowTestCamera(Window window) {
        PerspectiveCamera camera =
                new PerspectiveCamera(toRadians(60.0f), window.framebufferAspectRatio(), 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 2.0f);
        return camera;
    }

    /** Checks both current-frame shadow work and retained per-light resources. */
    private static void assertShadowActivity(Renderer renderer, int maps, int passes, int drawCalls, long triangles) {
        RenderStatistics statistics = renderer.info().statistics();
        assertThat(statistics.shadowMaps()).isEqualTo(maps);
        assertThat(statistics.shadowPasses()).isEqualTo(passes);
        assertThat(statistics.shadowDrawCalls()).isEqualTo(drawCalls);
        assertThat(statistics.shadowTriangles()).isEqualTo(triangles);
        assertThat(renderer.info().resources().activeShadowMaps()).isEqualTo(maps);
    }

    /** Checks that the center of a deliberately oversized projected shadow contains no lit holes. */
    private static void assertSolidShadowCore(OverlayImage image) {
        byte[] pixels = image.pixels();
        int darkPixels = 0;
        int minimum = image.width() / 2 - 10;
        int maximum = image.width() / 2 + 10;
        for (int y = minimum; y < maximum; y++) {
            for (int x = minimum; x < maximum; x++) {
                int offset = (y * image.width() + x) * 4;
                int brightness = Byte.toUnsignedInt(pixels[offset])
                        + Byte.toUnsignedInt(pixels[offset + 1])
                        + Byte.toUnsignedInt(pixels[offset + 2]);
                if (brightness < 360) {
                    darkPixels++;
                }
            }
        }
        assertThat(darkPixels).isEqualTo((maximum - minimum) * (maximum - minimum));
    }

    /** Checks that filtering produces a continuous transition instead of a small set of visible bands. */
    private static void assertSmoothShadowEdge(OverlayImage image) {
        byte[] pixels = image.pixels();
        int row = image.height() / 2;
        int minimumBrightness = Integer.MAX_VALUE;
        int maximumBrightness = Integer.MIN_VALUE;
        int[] brightnesses = new int[image.width()];
        for (int x = 0; x < image.width(); x++) {
            int offset = (row * image.width() + x) * 4;
            int brightness = Byte.toUnsignedInt(pixels[offset])
                    + Byte.toUnsignedInt(pixels[offset + 1])
                    + Byte.toUnsignedInt(pixels[offset + 2]);
            brightnesses[x] = brightness;
            minimumBrightness = Math.min(minimumBrightness, brightness);
            maximumBrightness = Math.max(maximumBrightness, brightness);
        }
        int margin = (maximumBrightness - minimumBrightness) / 10;
        boolean[] observedBrightnesses = new boolean[MAXIMUM_COLOR_CHANNEL_VALUE * 3 + 1];
        int distinctIntermediateBrightnesses = 0;
        for (int brightness : brightnesses) {
            if (brightness > minimumBrightness + margin
                    && brightness < maximumBrightness - margin
                    && !observedBrightnesses[brightness]) {
                observedBrightnesses[brightness] = true;
                distinctIntermediateBrightnesses++;
            }
        }
        assertThat(distinctIntermediateBrightnesses).isGreaterThan(10);
    }

    /** Renders one depth comparison against the cleared depth buffer and checks its result. */
    private static void assertDepthFunctionResult(
            Window window,
            Renderer renderer,
            Scene scene,
            PerspectiveCamera camera,
            BasicMaterial material,
            DepthFunction depthFunction,
            boolean expectedToRender) {
        material.setDepthFunction(depthFunction);
        renderer.render(scene, camera);
        if (expectedToRender) {
            assertCenterPixelIsRed(window);
        } else {
            assertPixelIsBlack(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
        }
    }

    private static BufferGeometry createSmallTriangle() {
        return BufferGeometry.builder()
                .positions(-0.2f, -0.2f, 0.0f, 0.2f, -0.2f, 0.0f, 0.0f, 0.2f, 0.0f)
                .build();
    }

    /** Creates a centered triangle with one relative upward translation target. */
    private static BufferGeometry createMorphTriangle() {
        BufferGeometry geometry = BufferGeometry.builder()
                .positions(-0.22f, -0.22f, 0.0f, 0.22f, -0.22f, 0.0f, 0.0f, 0.22f, 0.0f)
                .normals(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
                .build();
        geometry.addMorphTarget(new MorphTarget(
                "rise", BufferAttribute.of(new float[] {0.0f, 0.55f, 0.0f, 0.0f, 0.55f, 0.0f, 0.0f, 0.55f, 0.0f}, 3)));
        return geometry;
    }

    private static BufferGeometry createTexturedTriangle() {
        return BufferGeometry.builder()
                .positions(-0.25f, -0.25f, 0.0f, 0.25f, -0.25f, 0.0f, 0.0f, 0.25f, 0.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .build();
    }

    private static BufferGeometry createLitTexturedTriangle() {
        return BufferGeometry.builder()
                .positions(-0.25f, -0.25f, 0.0f, 0.25f, -0.25f, 0.0f, 0.0f, 0.25f, 0.0f)
                .normals(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .build();
    }

    private static BufferGeometry createLitTriangle() {
        return BufferGeometry.builder()
                .positions(-0.8f, -0.8f, 0.0f, 0.8f, -0.8f, 0.0f, 0.0f, 0.8f, 0.0f)
                .normals(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
                .build();
    }

    private static BufferGeometry createSkinnedTriangle() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(new float[] {-1.4f, -0.4f, 0.0f, -0.6f, -0.4f, 0.0f, -1.0f, 0.4f, 0.0f}, 3));
        geometry.setAttribute(
                BufferGeometry.NORMAL,
                BufferAttribute.of(new float[] {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f}, 3));
        geometry.setAttribute(BufferGeometry.JOINTS, BufferAttribute.of(new float[12], 4));
        geometry.setAttribute(
                BufferGeometry.WEIGHTS,
                BufferAttribute.of(
                        new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}, 4));
        return geometry;
    }

    private static BufferGeometry createLitTexturedGreenTriangle() {
        return BufferGeometry.builder()
                .positions(-0.8f, -0.8f, 0.0f, 0.8f, -0.8f, 0.0f, 0.0f, 0.8f, 0.0f)
                .normals(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .vertexColors(Color.GREEN, Color.GREEN, Color.GREEN)
                .build();
    }

    private static ShaderMaterial createCustomMaterial() {
        return ShaderMaterial.builder(CUSTOM_VERTEX_SHADER, CUSTOM_FRAGMENT_SHADER)
                .define("USE_TINT")
                .build();
    }

    private static void assertCenterPixelIsRed(Window window) {
        assertPixelIsRed(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
    }

    private static void assertCenterPixelIsNormalizedRed(Window window) {
        assertPixelIsNormalizedRed(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
    }

    private static void assertCenterPixelIsNormalizedGreen(Window window) {
        ByteBuffer pixel = readPixel(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isBetween(150, 156);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isLessThan(10);
    }

    private static void assertCenterPixelIsWhite(Window window) {
        ByteBuffer pixel = readPixel(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isGreaterThan(240);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isGreaterThan(240);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isGreaterThan(240);
    }

    private static void assertCenterPixelIsPositiveZNormal(Window window) {
        ByteBuffer pixel = readPixel(window.framebufferWidth() / 2, window.framebufferHeight() / 2);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isBetween(180, 195);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isBetween(180, 195);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isGreaterThan(250);
    }

    private static void assertPixelIsRed(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isGreaterThan(240);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isLessThan(10);
    }

    /** Returns inclusive vertical bounds of red pixels in one framebuffer region. */
    private static RedBounds redBounds(int minimumX, int maximumX, int minimumY, int maximumY) {
        int firstY = Integer.MAX_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int y = minimumY; y <= maximumY; y++) {
            for (int x = minimumX; x <= maximumX; x++) {
                ByteBuffer pixel = readPixel(x, y);
                if (Byte.toUnsignedInt(pixel.get(0)) > 240
                        && Byte.toUnsignedInt(pixel.get(1)) < 10
                        && Byte.toUnsignedInt(pixel.get(2)) < 10) {
                    firstY = Math.min(firstY, y);
                    lastY = Math.max(lastY, y);
                }
            }
        }
        assertThat(firstY).as("first red framebuffer row").isNotEqualTo(Integer.MAX_VALUE);
        return new RedBounds(firstY, lastY);
    }

    /** Inclusive vertical framebuffer extent of selected red pixels. */
    private record RedBounds(int minimumY, int maximumY) {}

    private static void assertPixelIsGreen(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isGreaterThan(240);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isLessThan(10);
    }

    private static void assertPixelIsNormalizedRed(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isBetween(150, 156);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isLessThan(10);
    }

    private static void assertPixelIsBlack(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isLessThan(10);
    }

    private static void assertPixelIsBlue(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isGreaterThan(240);
    }

    private static void assertPixelIsNormalizedBlue(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(1))).isLessThan(10);
        assertThat(Byte.toUnsignedInt(pixel.get(2))).isBetween(150, 156);
    }

    private static void assertPixelIsNotBlack(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        int brightness =
                Byte.toUnsignedInt(pixel.get(0)) + Byte.toUnsignedInt(pixel.get(1)) + Byte.toUnsignedInt(pixel.get(2));
        assertThat(brightness).isPositive();
    }

    private static void assertNeighborhoodContainsRed(int centerX, int centerY) {
        assertThat(neighborhoodContainsColor(centerX, centerY, 0, 1, 2)).isTrue();
    }

    private static void assertNeighborhoodContainsGreen(int centerX, int centerY) {
        assertThat(neighborhoodContainsColor(centerX, centerY, 1, 0, 2)).isTrue();
    }

    private static void assertNeighborhoodContainsBlue(int centerX, int centerY) {
        assertThat(neighborhoodContainsColor(centerX, centerY, 2, 0, 1)).isTrue();
    }

    private static boolean neighborhoodContainsColor(
            int centerX, int centerY, int dominantComponent, int firstLowComponent, int secondLowComponent) {
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                ByteBuffer pixel = readPixel(x, y);
                if (Byte.toUnsignedInt(pixel.get(dominantComponent)) > 240
                        && Byte.toUnsignedInt(pixel.get(firstLowComponent)) < 10
                        && Byte.toUnsignedInt(pixel.get(secondLowComponent)) < 10) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ByteBuffer readPixel(int x, int y) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        return pixel;
    }
}
