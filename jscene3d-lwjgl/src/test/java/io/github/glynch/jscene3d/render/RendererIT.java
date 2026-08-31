/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static io.github.glynch.jscene3d.core.Angles.PI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.joml.Math.toRadians;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferAttribute;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.IndexBuffer;
import io.github.glynch.jscene3d.core.MaterialSide;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.core.Texture;
import io.github.glynch.jscene3d.core.TextureFilter;
import io.github.glynch.jscene3d.core.TextureWrap;
import io.github.glynch.jscene3d.platform.VerticalSync;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

final class RendererIT {
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

    private static BufferGeometry createTriangle() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(new float[] {-0.8f, -0.8f, 0.0f, 0.8f, -0.8f, 0.0f, 0.0f, 0.8f, 0.0f}, 3));
        return geometry;
    }

    private static BufferGeometry createSmallTriangle() {
        return BufferGeometry.builder()
                .positions(-0.2f, -0.2f, 0.0f, 0.2f, -0.2f, 0.0f, 0.0f, 0.2f, 0.0f)
                .build();
    }

    private static BufferGeometry createTexturedTriangle() {
        return BufferGeometry.builder()
                .positions(-0.25f, -0.25f, 0.0f, 0.25f, -0.25f, 0.0f, 0.0f, 0.25f, 0.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .build();
    }

    private static void assertCenterPixelIsRed(Window window) {
        assertPixelIsRed(window.framebufferWidth() / 2, window.framebufferHeight() / 2);
    }

    private static void assertPixelIsRed(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        assertThat(Byte.toUnsignedInt(pixel.get(0))).isGreaterThan(240);
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

    private static void assertPixelIsNotBlack(int x, int y) {
        ByteBuffer pixel = readPixel(x, y);

        int brightness =
                Byte.toUnsignedInt(pixel.get(0)) + Byte.toUnsignedInt(pixel.get(1)) + Byte.toUnsignedInt(pixel.get(2));
        assertThat(brightness).isPositive();
    }

    private static ByteBuffer readPixel(int x, int y) {
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        return pixel;
    }
}
