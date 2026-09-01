/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_GEQUAL;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_NEVER;
import static org.lwjgl.opengl.GL11.GL_NOTEQUAL;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_SAMPLES;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.lwjgl.internal.Preconditions;
import io.github.glynch.jscene3d.lwjgl.internal.WindowContextRegistry;
import io.github.glynch.jscene3d.materials.AlphaMode;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.ShaderAttribute;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import io.github.glynch.jscene3d.materials.ShaderUniform;
import io.github.glynch.jscene3d.materials.ShaderUniformType;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.internal.Frustum;
import io.github.glynch.jscene3d.render.internal.PrimitiveTopology;
import io.github.glynch.jscene3d.render.internal.RenderItem;
import io.github.glynch.jscene3d.render.internal.RenderList;
import io.github.glynch.jscene3d.render.internal.programs.BasicProgram;
import io.github.glynch.jscene3d.render.internal.programs.EnvironmentBackgroundProgram;
import io.github.glynch.jscene3d.render.internal.programs.LambertProgram;
import io.github.glynch.jscene3d.render.internal.programs.LineProgram;
import io.github.glynch.jscene3d.render.internal.programs.NormalProgram;
import io.github.glynch.jscene3d.render.internal.programs.PhongProgram;
import io.github.glynch.jscene3d.render.internal.programs.ShaderProgram;
import io.github.glynch.jscene3d.render.internal.programs.ShaderProgramKey;
import io.github.glynch.jscene3d.render.internal.programs.StandardProgram;
import io.github.glynch.jscene3d.render.internal.programs.ToneMappingProgram;
import io.github.glynch.jscene3d.render.internal.resources.BrdfLookupResource;
import io.github.glynch.jscene3d.render.internal.resources.DefaultTexture;
import io.github.glynch.jscene3d.render.internal.resources.EnvironmentResource;
import io.github.glynch.jscene3d.render.internal.resources.GeometryResource;
import io.github.glynch.jscene3d.render.internal.resources.TextureResource;
import io.github.glynch.jscene3d.render.internal.resources.ToneMappingTarget;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureCoordinateOrigin;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/** Owns rendering and all OpenGL state for one JScene3D window context. */
public final class Renderer implements AutoCloseable {
    /** Maximum number of visible point lights supported by one rendered scene in version 0.1. */
    public static final int MAX_POINT_LIGHTS = 8;

    /** Maximum number of visible directional lights supported by one rendered scene in version 0.1. */
    public static final int MAX_DIRECTIONAL_LIGHTS = 8;

    /** Maximum number of visible spotlights supported by one rendered scene in version 0.1. */
    public static final int MAX_SPOT_LIGHTS = 8;

    /** Maximum number of visible hemisphere lights supported by one rendered scene in version 0.1. */
    public static final int MAX_HEMISPHERE_LIGHTS = 8;

    private final Window window;
    private final WindowContextRegistry.Access context;
    private final boolean automaticClear;
    private final RendererInfo info;
    private final RenderStatistics statistics;
    private final ResourceStatistics resources;
    private final IdentityHashMap<BufferGeometry, GeometryResource> geometryResources;
    private final IdentityHashMap<Texture, TextureResource> textureResources;
    private final IdentityHashMap<EnvironmentMap, EnvironmentResource> environmentResources;
    private final Map<ShaderProgramKey, ShaderProgram> shaderPrograms;
    private final RenderList renderList;
    private final Frustum frustum;
    private final float[] matrixValues;
    private final float[] matrix3Values;
    private final Matrix3f textureTransformMatrix;
    private final Matrix3f viewToWorldMatrix;
    private final Matrix3f environmentRotationMatrix;
    private final Matrix4f inverseProjectionMatrix;
    private final Quaternionf environmentRotation;
    private final Vector2f normalScale;
    private final OverlayCanvas overlayCanvas;
    private final int maxTextureUnits;

    private Color clearColor;
    private float clearAlpha;
    private ToneMapping toneMapping;
    private float exposure;
    private @Nullable BasicProgram basicProgram;
    private @Nullable EnvironmentBackgroundProgram environmentBackgroundProgram;
    private @Nullable LambertProgram lambertProgram;
    private @Nullable LineProgram lineProgram;
    private @Nullable NormalProgram normalProgram;
    private @Nullable PhongProgram phongProgram;
    private @Nullable StandardProgram standardProgram;
    private @Nullable ToneMappingProgram toneMappingProgram;
    private @Nullable ToneMappingTarget toneMappingTarget;
    private @Nullable OverlayRenderer overlayRenderer;
    private @Nullable DefaultTexture defaultTexture;
    private @Nullable BrdfLookupResource brdfLookupResource;
    private boolean customViewport;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private boolean standardTextureUnitsPrimed;
    private boolean closed;

    /** Initializes context-local renderer state after the window context is exclusively claimed. */
    private Renderer(Window window, WindowContextRegistry.Access context, RendererOptions options) {
        this.window = window;
        this.context = context;
        automaticClear = options.automaticClear();
        clearColor = options.clearColor();
        clearAlpha = options.clearAlpha();
        toneMapping = options.toneMapping();
        exposure = options.exposure();
        info = new RendererInfo();
        statistics = info.statistics();
        resources = info.resources();
        geometryResources = new IdentityHashMap<>();
        textureResources = new IdentityHashMap<>();
        environmentResources = new IdentityHashMap<>();
        shaderPrograms = new HashMap<>();
        renderList = new RenderList(MAX_POINT_LIGHTS, MAX_DIRECTIONAL_LIGHTS, MAX_SPOT_LIGHTS, MAX_HEMISPHERE_LIGHTS);
        frustum = new Frustum();
        matrixValues = new float[16];
        matrix3Values = new float[9];
        textureTransformMatrix = new Matrix3f();
        viewToWorldMatrix = new Matrix3f();
        environmentRotationMatrix = new Matrix3f();
        inverseProjectionMatrix = new Matrix4f();
        environmentRotation = new Quaternionf();
        normalScale = new Vector2f();
        overlayCanvas = new OverlayCanvas();
        maxTextureUnits = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
    }

    /**
     * Creates a renderer with default options and exclusively claims the window context.
     *
     * @param window open window whose context the renderer will own
     * @return renderer owning the window's OpenGL context
     * @throws NullPointerException if {@code window} is {@code null}
     * @throws IllegalStateException if the window is closed or its context is already claimed
     */
    public static Renderer create(Window window) {
        return create(window, RendererOptions.defaults());
    }

    /**
     * Creates a renderer and exclusively claims the window context.
     *
     * @param window open window whose context the renderer will own
     * @param options immutable renderer configuration
     * @return renderer owning the window's OpenGL context
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if the window is closed or its context is already claimed
     */
    public static Renderer create(Window window, RendererOptions options) {
        Window validWindow = Objects.requireNonNull(window, "window");
        RendererOptions validOptions = Objects.requireNonNull(options, "options");
        WindowContextRegistry.Access context = WindowContextRegistry.claim(validWindow);
        try {
            context.makeCurrent();
            glEnable(GL_FRAMEBUFFER_SRGB);
            return new Renderer(validWindow, context, validOptions);
        } catch (RuntimeException exception) {
            WindowContextRegistry.release(validWindow, context);
            throw exception;
        }
    }

    /**
     * Renders the visible meshes in a scene with the supplied camera.
     *
     * @param scene scene graph to render
     * @param camera camera defining the view and projection
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if this renderer is closed
     */
    public void render(Scene scene, Camera camera) {
        requireOpen();
        Scene validScene = Objects.requireNonNull(scene, "scene");
        Camera validCamera = Objects.requireNonNull(camera, "camera");
        context.makeCurrent();
        releaseClosedGeometryResources();
        releaseClosedTextureResources();
        releaseClosedEnvironmentResources();
        standardTextureUnitsPrimed = false;
        statistics.beginFrame();

        int framebufferWidth = context.framebufferWidth();
        int framebufferHeight = context.framebufferHeight();
        boolean usesToneMapping = toneMapping != ToneMapping.NONE && framebufferWidth > 0 && framebufferHeight > 0;
        if (usesToneMapping) {
            toneMappingTarget().begin(framebufferWidth, framebufferHeight, glGetInteger(GL_SAMPLES));
        }
        applyViewport(framebufferWidth, framebufferHeight);
        if (automaticClear) {
            Color background = validScene.background();
            clearBuffers(background == null ? clearColor : background);
        }
        boolean rendered = false;
        try {
            if (framebufferWidth > 0 && framebufferHeight > 0) {
                Matrix4fc viewMatrix = validCamera.viewMatrix();
                Matrix4fc projectionMatrix = validCamera.projectionMatrix();
                prepareEnvironmentMatrices(validScene, viewMatrix);
                renderEnvironmentBackground(validScene, projectionMatrix);
                frustum.update(viewMatrix, projectionMatrix);
                renderList.build(validScene, viewMatrix, frustum);
                statistics.recordCulledMeshes(renderList.culledMeshes());
                statistics.recordCulledLines(renderList.culledLines());
                for (int index = 0; index < renderList.opaqueCount(); index++) {
                    renderItem(renderList.opaqueItem(index), viewMatrix, projectionMatrix, validScene);
                }
                for (int index = 0; index < renderList.transparentCount(); index++) {
                    renderItem(renderList.transparentItem(index), viewMatrix, projectionMatrix, validScene);
                }
            }
            statistics.completeFrame();
            rendered = true;
        } finally {
            renderList.clear();
            glBindVertexArray(0);
            if (usesToneMapping) {
                if (rendered) {
                    toneMappingTarget().present(toneMappingProgram(), exposure);
                } else {
                    toneMappingTarget().cancel();
                }
            }
        }
    }

    /**
     * Draws a safe logical-coordinate overlay over the complete current framebuffer.
     *
     * <p>Call this after {@link #render(Scene, Camera)} and before swapping buffers. Overlay draws
     * do not alter the most-recent-scene values in {@link RenderStatistics}. The supplied overlay
     * receives no OpenGL state or handles. An overlay that paints nothing does not realize GPU
     * resources.
     *
     * @param overlay overlay to paint in current logical window coordinates
     * @throws NullPointerException if {@code overlay} is {@code null}
     * @throws IllegalStateException if this renderer is closed
     */
    public void render(Overlay overlay) {
        requireOpen();
        Overlay validOverlay = Objects.requireNonNull(overlay, "overlay");
        int logicalWidth = window.width();
        int logicalHeight = window.height();
        overlayCanvas.clear();
        validOverlay.paint(overlayCanvas, logicalWidth, logicalHeight);
        if (overlayCanvas.vertexCount() == 0) {
            return;
        }
        context.makeCurrent();
        overlayRenderer()
                .render(
                        overlayCanvas,
                        logicalWidth,
                        logicalHeight,
                        context.framebufferWidth(),
                        context.framebufferHeight());
    }

    /** Clears the current color and depth buffers using the renderer clear color. */
    public void clear() {
        requireOpen();
        context.makeCurrent();
        applyViewport(context.framebufferWidth(), context.framebufferHeight());
        clearBuffers(clearColor);
    }

    /**
     * Sets an explicit framebuffer-pixel viewport for subsequent frames.
     *
     * @param x non-negative horizontal origin
     * @param y non-negative vertical origin
     * @param width positive viewport width
     * @param height positive viewport height
     * @throws IllegalArgumentException if an origin is negative or a dimension is not positive
     * @throws IllegalStateException if this renderer is closed
     */
    public void setViewport(int x, int y, int width, int height) {
        requireOpen();
        context.makeCurrent();
        int validX = Preconditions.requireNonNegative(x, "x");
        int validY = Preconditions.requireNonNegative(y, "y");
        int validWidth = Preconditions.requirePositive(width, "width");
        int validHeight = Preconditions.requirePositive(height, "height");
        viewportX = validX;
        viewportY = validY;
        viewportWidth = validWidth;
        viewportHeight = validHeight;
        customViewport = true;
    }

    /** Restores automatic use of the complete current framebuffer. */
    public void resetViewport() {
        requireOpen();
        context.makeCurrent();
        customViewport = false;
    }

    /**
     * Captures the current custom viewport, or the complete framebuffer, as an overlay image.
     *
     * <p>The image is read from the current back buffer and returned in conventional top-to-bottom
     * row order. Call this after rendering and before swapping buffers.
     *
     * @return immutable sRGB RGBA image of the current renderer viewport
     * @throws IllegalStateException if this renderer is closed
     */
    public OverlayImage captureViewport() {
        requireOpen();
        context.makeCurrent();
        int x = customViewport ? viewportX : 0;
        int y = customViewport ? viewportY : 0;
        int width = customViewport ? viewportWidth : context.framebufferWidth();
        int height = customViewport ? viewportHeight : context.framebufferHeight();
        long byteCount = (long) width * height * 4L;
        if (byteCount > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Framebuffer viewport exceeds Java array limits: " + width + " x " + height);
        }
        ByteBuffer source = ByteBuffer.allocateDirect((int) byteCount);
        glReadPixels(x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, source);
        byte[] pixels = new byte[(int) byteCount];
        int rowBytes = width * 4;
        for (int destinationRow = 0; destinationRow < height; destinationRow++) {
            int sourceOffset = (height - destinationRow - 1) * rowBytes;
            source.get(sourceOffset, pixels, destinationRow * rowBytes, rowBytes);
        }
        return OverlayImage.srgbRgba(width, height, pixels);
    }

    /**
     * Sets the renderer's default linear-sRGB clear color and alpha.
     *
     * @param color clear color in JScene3D's linear-sRGB working space
     * @param alpha clear alpha in the inclusive range {@code [0, 1]}
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code alpha} is non-finite or outside {@code [0, 1]}
     * @throws IllegalStateException if this renderer is closed
     */
    public void setClearColor(Color color, float alpha) {
        requireOpen();
        context.makeCurrent();
        Color validColor = Objects.requireNonNull(color, "color");
        float validAlpha = Preconditions.requireUnitInterval(alpha, "alpha");
        clearColor = validColor;
        clearAlpha = validAlpha;
    }

    /**
     * Returns the current high-dynamic-range tone mapping mode.
     *
     * @return current mode
     * @throws IllegalStateException if this renderer is closed
     */
    public ToneMapping toneMapping() {
        requireOpen();
        return toneMapping;
    }

    /**
     * Changes the high-dynamic-range tone mapping mode for subsequent scene frames.
     *
     * @param toneMapping new mode
     * @throws NullPointerException if {@code toneMapping} is {@code null}
     * @throws IllegalStateException if this renderer is closed
     */
    public void setToneMapping(ToneMapping toneMapping) {
        requireOpen();
        this.toneMapping = Objects.requireNonNull(toneMapping, "toneMapping");
    }

    /**
     * Returns the current linear exposure multiplier.
     *
     * @return finite positive multiplier
     * @throws IllegalStateException if this renderer is closed
     */
    public float exposure() {
        requireOpen();
        return exposure;
    }

    /**
     * Changes the linear exposure multiplier used during tone mapping.
     *
     * @param exposure finite positive multiplier
     * @throws IllegalArgumentException if {@code exposure} is non-positive or non-finite
     * @throws IllegalStateException if this renderer is closed
     */
    public void setExposure(float exposure) {
        requireOpen();
        this.exposure = Preconditions.requirePositive(exposure, "exposure");
    }

    /**
     * Returns this renderer's stable diagnostic container.
     *
     * @return renderer-owned diagnostics updated in place
     * @throws IllegalStateException if this renderer is closed
     */
    public RendererInfo info() {
        requireOpen();
        context.makeCurrent();
        return info;
    }

    /**
     * Returns whether terminal closure has completed.
     *
     * @return {@code true} after this renderer has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /** Releases all context-local GPU resources and the exclusive window claim. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        context.makeCurrent();
        try {
            closeCachedResources();
            closeBuiltInPrograms();
            closeRenderTargets();
            glBindVertexArray(0);
            glUseProgram(0);
            renderList.clear();
            resources.setActiveGeometryResources(0);
            resources.setActiveTextureResources(0);
            resources.setProgramCount(0);
            closed = true;
        } finally {
            WindowContextRegistry.release(window, context);
        }
    }

    /** Releases resources cached by scene object identity. */
    private void closeCachedResources() {
        geometryResources.values().forEach(GeometryResource::close);
        geometryResources.clear();
        textureResources.values().forEach(TextureResource::close);
        textureResources.clear();
        environmentResources.values().forEach(EnvironmentResource::close);
        environmentResources.clear();
        shaderPrograms.values().forEach(ShaderProgram::close);
        shaderPrograms.clear();
    }

    /** Releases lazily created built-in shader programs. */
    private void closeBuiltInPrograms() {
        if (basicProgram != null) {
            basicProgram.close();
            basicProgram = null;
        }
        if (environmentBackgroundProgram != null) {
            environmentBackgroundProgram.close();
            environmentBackgroundProgram = null;
        }
        if (lambertProgram != null) {
            lambertProgram.close();
            lambertProgram = null;
        }
        if (lineProgram != null) {
            lineProgram.close();
            lineProgram = null;
        }
        if (normalProgram != null) {
            normalProgram.close();
            normalProgram = null;
        }
        if (phongProgram != null) {
            phongProgram.close();
            phongProgram = null;
        }
        if (standardProgram != null) {
            standardProgram.close();
            standardProgram = null;
        }
        if (toneMappingProgram != null) {
            toneMappingProgram.close();
            toneMappingProgram = null;
        }
    }

    /** Releases lazily created framebuffer and overlay resources. */
    private void closeRenderTargets() {
        if (toneMappingTarget != null) {
            toneMappingTarget.close();
            toneMappingTarget = null;
        }
        if (overlayRenderer != null) {
            overlayRenderer.close();
            overlayRenderer = null;
        }
        if (defaultTexture != null) {
            defaultTexture.close();
            defaultTexture = null;
        }
        if (brdfLookupResource != null) {
            brdfLookupResource.close();
            brdfLookupResource = null;
        }
    }

    /** Resolves the camera-to-world and inverse environment rotations for one scene frame. */
    private void prepareEnvironmentMatrices(Scene scene, Matrix4fc viewMatrix) {
        viewToWorldMatrix.set(viewMatrix).invert();
        scene.environmentRotation(environmentRotation);
        environmentRotationMatrix.rotation(environmentRotation).transpose();
    }

    /** Draws a selected HDR environment background before scene geometry. */
    private void renderEnvironmentBackground(Scene scene, Matrix4fc projectionMatrix) {
        @Nullable EnvironmentMap background = scene.backgroundEnvironment();
        if (background == null) {
            return;
        }
        requireOpenEnvironment(background, "Scene background environment");
        inverseProjectionMatrix.set(projectionMatrix).invert();
        environmentBackgroundProgram()
                .render(
                        environmentResource(background),
                        inverseProjectionMatrix,
                        viewToWorldMatrix,
                        environmentRotationMatrix,
                        scene.backgroundIntensity());
    }

    /** Synchronizes and submits one prepared scene-object draw. */
    private void renderItem(RenderItem item, Matrix4fc viewMatrix, Matrix4fc projectionMatrix, Scene scene) {
        BufferGeometry geometry = item.geometry();
        Material material = item.material();
        GeometryResource resource = geometryResources.computeIfAbsent(geometry, ignored -> new GeometryResource());
        resources.setActiveGeometryResources(geometryResources.size());
        PrimitiveTopology topology = item.topology();
        applyMaterialState(material, topology);
        if (topology.isLine()) {
            renderLine(item, geometry, (LineBasicMaterial) material, resource, viewMatrix, projectionMatrix);
        } else {
            switch (material) {
                case BasicMaterial basicMaterial ->
                    renderBasicMesh(item, geometry, basicMaterial, resource, viewMatrix, projectionMatrix);
                case LambertMaterial lambertMaterial ->
                    renderLambertMesh(item, geometry, lambertMaterial, resource, viewMatrix, projectionMatrix);
                case NormalMaterial normalMaterial ->
                    renderNormalMesh(item, geometry, normalMaterial, resource, viewMatrix, projectionMatrix);
                case PhongMaterial phongMaterial ->
                    renderPhongMesh(item, geometry, phongMaterial, resource, viewMatrix, projectionMatrix);
                case ShaderMaterial shaderMaterial ->
                    renderShaderMesh(item, geometry, shaderMaterial, resource, viewMatrix, projectionMatrix);
                case StandardMaterial standardMaterial ->
                    renderStandardMesh(item, geometry, standardMaterial, resource, viewMatrix, projectionMatrix, scene);
                default ->
                    throw new IllegalStateException("Unsupported mesh material type: "
                            + material.getClass().getName());
            }
        }

        resource.bind();
        drawGeometry(geometry, topology, item.elementCount());
    }

    /** Synchronizes and binds one built-in line-material draw. */
    private void renderLine(
            RenderItem item,
            BufferGeometry geometry,
            LineBasicMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        LineProgram program = lineProgram();
        recordUploads(resource.synchronize(geometry, false, material.usesVertexColors(), false, "LineBasicMaterial"));
        glUseProgram(program.id());
        uploadMatrix(program.modelMatrixLocation(), item.worldMatrix());
        uploadMatrix(program.viewMatrixLocation(), viewMatrix);
        uploadMatrix(program.projectionMatrixLocation(), projectionMatrix);
        Color color = material.color();
        float alpha = resolvedAlpha(material);
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), alpha);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        glUniform1f(program.alphaCutoffLocation(), resolvedAlphaCutoff(material));
    }

    /** Synchronizes and binds one built-in basic-material draw. */
    private void renderBasicMesh(
            RenderItem item,
            BufferGeometry geometry,
            BasicMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        BasicProgram program = basicProgram();
        @Nullable Texture colorMap = material.colorMap().orElse(null);
        if (colorMap != null && colorMap.isClosed()) {
            throw new IllegalStateException("BasicMaterial colorMap is closed");
        }
        recordUploads(
                resource.synchronize(geometry, false, material.usesVertexColors(), colorMap != null, "BasicMaterial"));

        glUseProgram(program.id());
        uploadMatrix(program.modelMatrixLocation(), item.worldMatrix());
        uploadMatrix(program.viewMatrixLocation(), viewMatrix);
        uploadMatrix(program.projectionMatrixLocation(), projectionMatrix);
        if (colorMap != null) {
            uploadTextureState(program.colorMapTransformLocation(), program.flipColorMapVerticallyLocation(), colorMap);
        }
        Color color = material.color();
        float alpha = resolvedAlpha(material);
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), alpha);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        glUniform1f(program.alphaCutoffLocation(), resolvedAlphaCutoff(material));
        glActiveTexture(GL_TEXTURE0);
        if (colorMap == null) {
            defaultTexture().bind();
            glUniform1i(program.useColorMapLocation(), 0);
        } else {
            TextureResource textureResource =
                    textureResources.computeIfAbsent(colorMap, ignored -> new TextureResource());
            updateTextureResourceCount();
            synchronizeTexture(textureResource, colorMap);
            glUniform1i(program.colorMapLocation(), 0);
            glUniform1i(program.useColorMapLocation(), 1);
        }
    }

    /** Synchronizes and binds one built-in diffuse Lambert-material draw. */
    private void renderLambertMesh(
            RenderItem item,
            BufferGeometry geometry,
            LambertMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        LambertProgram program = lambertProgram();
        @Nullable Texture colorMap = material.colorMap().orElse(null);
        if (colorMap != null && colorMap.isClosed()) {
            throw new IllegalStateException("LambertMaterial colorMap is closed");
        }
        recordUploads(
                resource.synchronize(geometry, true, material.usesVertexColors(), colorMap != null, "LambertMaterial"));

        glUseProgram(program.id());
        program.uploadTransforms(item.worldMatrix(), viewMatrix, projectionMatrix);
        if (colorMap != null) {
            uploadTextureState(program.colorMapTransformLocation(), program.flipColorMapVerticallyLocation(), colorMap);
        }
        program.uploadLights(renderList.lights(), viewMatrix);
        Color color = material.color();
        float alpha = resolvedAlpha(material);
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), alpha);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        glUniform1f(program.alphaCutoffLocation(), resolvedAlphaCutoff(material));
        glActiveTexture(GL_TEXTURE0);
        if (colorMap == null) {
            defaultTexture().bind();
            glUniform1i(program.useColorMapLocation(), 0);
        } else {
            TextureResource textureResource =
                    textureResources.computeIfAbsent(colorMap, ignored -> new TextureResource());
            updateTextureResourceCount();
            synchronizeTexture(textureResource, colorMap);
            glUniform1i(program.colorMapLocation(), 0);
            glUniform1i(program.useColorMapLocation(), 1);
        }
    }

    /** Synchronizes and binds one built-in normal-visualization draw. */
    private void renderNormalMesh(
            RenderItem item,
            BufferGeometry geometry,
            NormalMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        NormalProgram program = normalProgram();
        recordUploads(resource.synchronize(geometry, true, false, false, "NormalMaterial"));

        glUseProgram(program.id());
        program.uploadTransforms(item.worldMatrix(), viewMatrix, projectionMatrix);
        glUniform1f(program.opacityLocation(), resolvedAlpha(material));
        glUniform1f(program.alphaCutoffLocation(), resolvedAlphaCutoff(material));
    }

    /** Synchronizes and binds one built-in Blinn-Phong-material draw. */
    private void renderPhongMesh(
            RenderItem item,
            BufferGeometry geometry,
            PhongMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        PhongProgram program = phongProgram();
        @Nullable Texture colorMap = material.colorMap().orElse(null);
        if (colorMap != null && colorMap.isClosed()) {
            throw new IllegalStateException("PhongMaterial colorMap is closed");
        }
        recordUploads(
                resource.synchronize(geometry, true, material.usesVertexColors(), colorMap != null, "PhongMaterial"));

        glUseProgram(program.id());
        program.uploadTransforms(item.worldMatrix(), viewMatrix, projectionMatrix);
        if (colorMap != null) {
            uploadTextureState(program.colorMapTransformLocation(), program.flipColorMapVerticallyLocation(), colorMap);
        }
        program.uploadLights(renderList.lights(), viewMatrix);
        Color color = material.color();
        float alpha = resolvedAlpha(material);
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), alpha);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        glUniform1f(program.alphaCutoffLocation(), resolvedAlphaCutoff(material));
        Color emissive = material.emissive();
        float emissiveIntensity = material.emissiveIntensity();
        glUniform3f(
                program.emissiveColorLocation(),
                emissive.red() * emissiveIntensity,
                emissive.green() * emissiveIntensity,
                emissive.blue() * emissiveIntensity);
        Color specular = material.specular();
        glUniform3f(program.specularColorLocation(), specular.red(), specular.green(), specular.blue());
        glUniform1f(program.shininessLocation(), material.shininess());
        glActiveTexture(GL_TEXTURE0);
        if (colorMap == null) {
            defaultTexture().bind();
            glUniform1i(program.useColorMapLocation(), 0);
        } else {
            TextureResource textureResource =
                    textureResources.computeIfAbsent(colorMap, ignored -> new TextureResource());
            updateTextureResourceCount();
            synchronizeTexture(textureResource, colorMap);
            glUniform1i(program.colorMapLocation(), 0);
            glUniform1i(program.useColorMapLocation(), 1);
        }
    }

    /** Synchronizes and binds one metallic-roughness physically based material draw. */
    private void renderStandardMesh(
            RenderItem item,
            BufferGeometry geometry,
            StandardMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix,
            Scene scene) {
        StandardProgram program = standardProgram();
        @Nullable Texture colorMap = material.colorMap().orElse(null);
        @Nullable
        Texture metalnessRoughnessMap = material.metalnessRoughnessMap().orElse(null);
        @Nullable Texture normalMap = material.normalMap().orElse(null);
        @Nullable Texture occlusionMap = material.occlusionMap().orElse(null);
        @Nullable Texture emissiveMap = material.emissiveMap().orElse(null);
        requireOpenTexture(colorMap, "StandardMaterial colorMap");
        requireOpenTexture(metalnessRoughnessMap, "StandardMaterial metalnessRoughnessMap");
        requireOpenTexture(normalMap, "StandardMaterial normalMap");
        requireOpenTexture(occlusionMap, "StandardMaterial occlusionMap");
        requireOpenTexture(emissiveMap, "StandardMaterial emissiveMap");
        boolean requiresTextureCoordinates = colorMap != null
                || metalnessRoughnessMap != null
                || normalMap != null
                || occlusionMap != null
                || emissiveMap != null;
        recordUploads(resource.synchronize(
                geometry, true, material.usesVertexColors(), requiresTextureCoordinates, "StandardMaterial"));

        primeStandardTextureUnits();
        glUseProgram(program.id());
        program.uploadTransforms(item.worldMatrix(), viewMatrix, projectionMatrix);
        program.uploadLights(renderList.lights(), viewMatrix);
        Color color = material.color();
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), resolvedAlpha(material));
        glUniform1f(program.metalnessLocation(), material.metalness());
        glUniform1f(program.roughnessLocation(), material.roughness());
        Color emissive = material.emissive();
        float emissiveIntensity = material.emissiveIntensity();
        glUniform3f(
                program.emissiveColorLocation(),
                emissive.red() * emissiveIntensity,
                emissive.green() * emissiveIntensity,
                emissive.blue() * emissiveIntensity);
        material.normalScale(normalScale);
        glUniform2f(program.normalScaleLocation(), normalScale.x(), normalScale.y());
        glUniform1f(program.occlusionStrengthLocation(), material.occlusionStrength());
        glUniform1f(
                program.alphaCutoffLocation(), material.alphaMode() == AlphaMode.MASK ? material.alphaCutoff() : -1.0f);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        bindStandardTexture(colorMap, program.colorMap(), 0);
        bindStandardTexture(metalnessRoughnessMap, program.metalnessRoughnessMap(), 1);
        bindStandardTexture(normalMap, program.normalMap(), 2);
        bindStandardTexture(occlusionMap, program.occlusionMap(), 3);
        bindStandardTexture(emissiveMap, program.emissiveMap(), 4);
        bindEnvironment(program, scene, material);
    }

    /** Binds optional scene-wide image-based-lighting textures and uniforms. */
    private void bindEnvironment(StandardProgram program, Scene scene, StandardMaterial material) {
        program.uploadEnvironmentMatrices(viewToWorldMatrix, environmentRotationMatrix);
        @Nullable EnvironmentMap environmentMap = scene.environment();
        if (environmentMap == null) {
            glUniform1i(program.useEnvironmentMapLocation(), 0);
            glUniform1f(program.environmentIntensityLocation(), 0.0f);
            glUniform1f(program.maximumReflectionLevelLocation(), 0.0f);
            return;
        }
        requireOpenEnvironment(environmentMap, "Scene environment");
        if (maxTextureUnits < 8) {
            throw new IllegalStateException(
                    "StandardMaterial environment lighting requires at least 8 texture units: " + maxTextureUnits);
        }
        EnvironmentResource environment = environmentResource(environmentMap);
        glActiveTexture(GL_TEXTURE0 + 5);
        environment.bindIrradiance();
        glUniform1i(program.environmentIrradianceMapLocation(), 5);
        glActiveTexture(GL_TEXTURE0 + 6);
        environment.bindReflections();
        glUniform1i(program.environmentReflectionMapLocation(), 6);
        glActiveTexture(GL_TEXTURE0 + 7);
        brdfLookupResource().bind();
        glUniform1i(program.environmentBrdfMapLocation(), 7);
        glUniform1f(
                program.environmentIntensityLocation(), scene.environmentIntensity() * material.environmentIntensity());
        glUniform1f(program.maximumReflectionLevelLocation(), environment.maximumReflectionLevel());
        glUniform1i(program.useEnvironmentMapLocation(), 1);
    }

    /** Synchronizes and binds one custom shader-material draw. */
    private void renderShaderMesh(
            RenderItem item,
            BufferGeometry geometry,
            ShaderMaterial material,
            GeometryResource resource,
            Matrix4fc viewMatrix,
            Matrix4fc projectionMatrix) {
        if (material.alphaMode() == AlphaMode.MASK) {
            throw new IllegalStateException(
                    "ShaderMaterial does not provide automatic alpha masking; implement discard in the shader");
        }
        ShaderProgram program = shaderProgram(material);
        Set<ShaderAttribute> requiredAttributes = material.requiredAttributes();
        recordUploads(resource.synchronize(
                geometry,
                requiredAttributes.contains(ShaderAttribute.NORMAL),
                requiredAttributes.contains(ShaderAttribute.COLOR),
                requiredAttributes.contains(ShaderAttribute.UV),
                "ShaderMaterial"));
        glUseProgram(program.id());
        program.uploadAutomaticUniforms(item.worldMatrix(), viewMatrix, projectionMatrix);
        uploadApplicationUniforms(program, material);
    }

    /** Issues one indexed or non-indexed primitive draw and records its statistics. */
    private void drawGeometry(BufferGeometry geometry, PrimitiveTopology topology, int elementCount) {
        int start = geometry.drawRangeStart();
        IndexBuffer index = geometry.index();
        if (index == null) {
            glDrawArrays(topology.openGlMode(), start, elementCount);
        } else {
            glDrawElements(topology.openGlMode(), elementCount, GL_UNSIGNED_INT, (long) start * Integer.BYTES);
        }
        if (topology.isLine()) {
            statistics.recordLineDraw(topology.primitiveCount(elementCount));
        } else {
            statistics.recordMeshDraw(elementCount);
        }
    }

    /** Resolves a shared custom program, caching only a successful realization. */
    private ShaderProgram shaderProgram(ShaderMaterial material) {
        ShaderProgramKey key = ShaderProgramKey.from(material);
        ShaderProgram program = shaderPrograms.get(key);
        if (program == null) {
            program = ShaderProgram.create(key);
            shaderPrograms.put(key, program);
            updateProgramCount();
        }
        return program;
    }

    /** Validates and uploads every active application-controlled uniform. */
    private void uploadApplicationUniforms(ShaderProgram program, ShaderMaterial material) {
        Map<String, ShaderUniform> values = material.uniforms();
        int textureUnit = 0;
        for (ShaderProgram.ActiveUniform activeUniform : program.applicationUniforms()) {
            ShaderUniform value = values.get(activeUniform.name());
            if (value == null) {
                throw new IllegalStateException(
                        "ShaderMaterial has no value for active uniform: " + activeUniform.name());
            }
            requireCompatibleUniformType(activeUniform, value);
            int location = activeUniform.location();
            switch (value.type()) {
                case FLOAT -> glUniform1f(location, value.floatComponent(0));
                case INTEGER -> glUniform1i(location, value.integerValue());
                case BOOLEAN -> glUniform1i(location, value.booleanValue() ? 1 : 0);
                case VECTOR2 -> glUniform2f(location, value.floatComponent(0), value.floatComponent(1));
                case VECTOR3, COLOR ->
                    glUniform3f(location, value.floatComponent(0), value.floatComponent(1), value.floatComponent(2));
                case VECTOR4 ->
                    glUniform4f(
                            location,
                            value.floatComponent(0),
                            value.floatComponent(1),
                            value.floatComponent(2),
                            value.floatComponent(3));
                case MATRIX3 -> uploadUniformMatrix3(location, value);
                case MATRIX4 -> uploadUniformMatrix4(location, value);
                case TEXTURE -> {
                    bindUniformTexture(activeUniform.name(), location, value.textureValue(), textureUnit);
                    textureUnit++;
                }
            }
        }
    }

    /** Requires a configured Java uniform type to match its active GLSL declaration. */
    private static void requireCompatibleUniformType(
            ShaderProgram.ActiveUniform activeUniform, ShaderUniform configuredUniform) {
        ShaderUniformType expected = activeUniform.type();
        ShaderUniformType actual = configuredUniform.type();
        boolean compatible =
                expected == actual || (expected == ShaderUniformType.VECTOR3 && actual == ShaderUniformType.COLOR);
        if (!compatible) {
            throw new IllegalStateException("ShaderMaterial uniform "
                    + activeUniform.name()
                    + " is configured as "
                    + actual
                    + ", but active GLSL expects "
                    + expected);
        }
    }

    /** Copies and uploads one configured three-by-three matrix. */
    private void uploadUniformMatrix3(int location, ShaderUniform uniform) {
        for (int index = 0; index < matrix3Values.length; index++) {
            matrix3Values[index] = uniform.floatComponent(index);
        }
        glUniformMatrix3fv(location, false, matrix3Values);
    }

    /** Copies and uploads one configured four-by-four matrix. */
    private void uploadUniformMatrix4(int location, ShaderUniform uniform) {
        for (int index = 0; index < matrixValues.length; index++) {
            matrixValues[index] = uniform.floatComponent(index);
        }
        glUniformMatrix4fv(location, false, matrixValues);
    }

    /** Uploads one built-in texture's UV transform and coordinate origin without allocating. */
    private void uploadTextureState(int transformLocation, int verticalFlipLocation, Texture texture) {
        texture.transformMatrix(textureTransformMatrix);
        textureTransformMatrix.get(matrix3Values);
        glUniformMatrix3fv(transformLocation, false, matrix3Values);
        glUniform1i(verticalFlipLocation, texture.coordinateOrigin() == TextureCoordinateOrigin.BOTTOM_LEFT ? 1 : 0);
    }

    /** Synchronizes and binds one active texture uniform to a consecutive texture unit. */
    private void bindUniformTexture(String name, int location, Texture texture, int textureUnit) {
        if (texture.isClosed()) {
            throw new IllegalStateException("ShaderMaterial texture uniform is closed: " + name);
        }
        if (textureUnit >= maxTextureUnits) {
            throw new IllegalStateException(
                    "ShaderMaterial requires more texture units than this context supports: " + (textureUnit + 1));
        }
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        TextureResource textureResource = textureResources.computeIfAbsent(texture, ignored -> new TextureResource());
        updateTextureResourceCount();
        synchronizeTexture(textureResource, texture);
        glUniform1i(location, textureUnit);
    }

    /** Binds one optional standard-material texture role to its fixed texture unit. */
    private void bindStandardTexture(
            @Nullable Texture texture, StandardProgram.TextureLocations locations, int textureUnit) {
        if (textureUnit >= maxTextureUnits) {
            throw new IllegalStateException(
                    "StandardMaterial requires more texture units than this context supports: " + (textureUnit + 1));
        }
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glUniform1i(locations.sampler(), textureUnit);
        if (texture == null) {
            defaultTexture().bind();
            glUniform1i(locations.enabled(), 0);
            return;
        }
        TextureResource textureResource = textureResources.computeIfAbsent(texture, ignored -> new TextureResource());
        updateTextureResourceCount();
        synchronizeTexture(textureResource, texture);
        uploadTextureState(locations.transform(), locations.verticalFlip(), texture);
        glUniform1i(locations.enabled(), 1);
    }

    /** Ensures every fixed standard-program sampler is complete before the program becomes active. */
    private void primeStandardTextureUnits() {
        if (standardTextureUnitsPrimed) {
            return;
        }
        int fixedUnitCount = Math.min(8, maxTextureUnits);
        for (int textureUnit = 0; textureUnit < fixedUnitCount; textureUnit++) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            defaultTexture().bind();
        }
        standardTextureUnitsPrimed = true;
    }

    /** Rejects a closed optional texture while allowing an absent role. */
    private static void requireOpenTexture(@Nullable Texture texture, String label) {
        if (texture != null && texture.isClosed()) {
            throw new IllegalStateException(label + " is closed");
        }
    }

    /** Resolves the fragment alpha used by masked and blended materials. */
    private static float resolvedAlpha(Material material) {
        return material.alphaMode() == AlphaMode.OPAQUE ? 1.0f : material.opacity();
    }

    /** Resolves the active mask threshold, or a disabled sentinel for other alpha modes. */
    private static float resolvedAlphaCutoff(Material material) {
        return material.alphaMode() == AlphaMode.MASK ? material.alphaCutoff() : -1.0f;
    }

    /** Records the buffer uploads performed while synchronizing one geometry. */
    private void recordUploads(GeometryResource.UploadResult uploads) {
        statistics.recordUploads(uploads.count(), uploads.byteCount());
    }

    /** Synchronizes one texture and records an image upload when one occurred. */
    private void synchronizeTexture(TextureResource resource, Texture texture) {
        long uploadedBytes = resource.synchronize(texture);
        if (uploadedBytes > 0L) {
            statistics.recordTextureUpload(uploadedBytes);
        }
    }

    /** Lazily creates and returns the context-local built-in program. */
    private BasicProgram basicProgram() {
        if (basicProgram == null) {
            basicProgram = BasicProgram.create();
            updateProgramCount();
        }
        return basicProgram;
    }

    /** Lazily creates the fullscreen environment-background program. */
    private EnvironmentBackgroundProgram environmentBackgroundProgram() {
        if (environmentBackgroundProgram == null) {
            environmentBackgroundProgram = EnvironmentBackgroundProgram.create();
            updateProgramCount();
        }
        return environmentBackgroundProgram;
    }

    /** Lazily creates and returns the context-local built-in Lambert program. */
    private LambertProgram lambertProgram() {
        if (lambertProgram == null) {
            lambertProgram = LambertProgram.create();
            updateProgramCount();
        }
        return lambertProgram;
    }

    /** Lazily creates and returns the context-local built-in line program. */
    private LineProgram lineProgram() {
        if (lineProgram == null) {
            lineProgram = LineProgram.create();
            updateProgramCount();
        }
        return lineProgram;
    }

    /** Lazily creates and returns the context-local built-in normal program. */
    private NormalProgram normalProgram() {
        if (normalProgram == null) {
            normalProgram = NormalProgram.create();
            updateProgramCount();
        }
        return normalProgram;
    }

    /** Lazily creates and returns the context-local built-in Phong program. */
    private PhongProgram phongProgram() {
        if (phongProgram == null) {
            phongProgram = PhongProgram.create();
            updateProgramCount();
        }
        return phongProgram;
    }

    /** Lazily creates and returns the context-local standard material program. */
    private StandardProgram standardProgram() {
        if (standardProgram == null) {
            standardProgram = StandardProgram.create();
            updateProgramCount();
        }
        return standardProgram;
    }

    /** Lazily creates the fullscreen ACES tone mapping program. */
    private ToneMappingProgram toneMappingProgram() {
        if (toneMappingProgram == null) {
            toneMappingProgram = ToneMappingProgram.create();
            updateProgramCount();
        }
        return toneMappingProgram;
    }

    /** Lazily creates resizable HDR framebuffer storage. */
    private ToneMappingTarget toneMappingTarget() {
        if (toneMappingTarget == null) {
            toneMappingTarget = new ToneMappingTarget();
        }
        return toneMappingTarget;
    }

    /** Lazily creates and returns context-local overlay drawing resources. */
    private OverlayRenderer overlayRenderer() {
        if (overlayRenderer == null) {
            overlayRenderer = OverlayRenderer.create(defaultTexture());
            updateProgramCount();
        }
        return overlayRenderer;
    }

    /** Lazily creates the complete fallback image required by optional sampler uniforms. */
    private DefaultTexture defaultTexture() {
        if (defaultTexture == null) {
            defaultTexture = new DefaultTexture();
        }
        return defaultTexture;
    }

    /** Lazily computes and uploads the shared split-sum BRDF integration lookup. */
    private BrdfLookupResource brdfLookupResource() {
        if (brdfLookupResource == null) {
            brdfLookupResource = new BrdfLookupResource();
            statistics.recordTextureUpload(brdfLookupResource.uploadedBytes());
            updateTextureResourceCount();
        }
        return brdfLookupResource;
    }

    /** Resolves or realizes all context-local maps derived from one environment. */
    private EnvironmentResource environmentResource(EnvironmentMap environmentMap) {
        EnvironmentResource resource = environmentResources.get(environmentMap);
        if (resource == null) {
            resource = new EnvironmentResource(environmentMap);
            environmentResources.put(environmentMap, resource);
            statistics.recordTextureUpload(resource.uploadedBytes());
            updateTextureResourceCount();
        }
        return resource;
    }

    /** Synchronizes the diagnostic program count with realized built-in programs. */
    private void updateProgramCount() {
        resources.setProgramCount((basicProgram == null ? 0 : 1)
                + (environmentBackgroundProgram == null ? 0 : 1)
                + (lambertProgram == null ? 0 : 1)
                + (lineProgram == null ? 0 : 1)
                + (normalProgram == null ? 0 : 1)
                + (phongProgram == null ? 0 : 1)
                + (standardProgram == null ? 0 : 1)
                + (toneMappingProgram == null ? 0 : 1)
                + (overlayRenderer == null ? 0 : 1)
                + shaderPrograms.size());
    }

    /** Synchronizes diagnostics with ordinary, environment, and shared lookup textures. */
    private void updateTextureResourceCount() {
        resources.setActiveTextureResources(
                textureResources.size() + environmentResources.size() * 3 + (brdfLookupResource == null ? 0 : 1));
    }

    /** Applies depth, blending, and face-culling state for one material. */
    private void applyMaterialState(Material material, PrimitiveTopology topology) {
        if (material.depthTestEnabled()) {
            glEnable(GL_DEPTH_TEST);
            int depthFunction =
                    switch (material.depthFunction()) {
                        case NEVER -> GL_NEVER;
                        case LESS -> GL_LESS;
                        case EQUAL -> GL_EQUAL;
                        case LESS_OR_EQUAL -> GL_LEQUAL;
                        case GREATER -> GL_GREATER;
                        case NOT_EQUAL -> GL_NOTEQUAL;
                        case GREATER_OR_EQUAL -> GL_GEQUAL;
                        case ALWAYS -> GL_ALWAYS;
                    };
            glDepthFunc(depthFunction);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthMask(material.depthWriteEnabled());

        if (material.alphaMode() == AlphaMode.BLEND) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else {
            glDisable(GL_BLEND);
        }

        MaterialSide side = material.side();
        if (topology.isLine() || side == MaterialSide.DOUBLE) {
            glDisable(GL_CULL_FACE);
        } else {
            glEnable(GL_CULL_FACE);
            glFrontFace(GL_CCW);
            glCullFace(side == MaterialSide.FRONT ? GL_BACK : GL_FRONT);
        }
    }

    /** Applies either the custom viewport or the complete current framebuffer. */
    private void applyViewport(int framebufferWidth, int framebufferHeight) {
        if (customViewport) {
            glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
        } else {
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        }
    }

    /** Clears color and depth using renderer and scene clear state. */
    private void clearBuffers(Color color) {
        glClearColor(color.red(), color.green(), color.blue(), clearAlpha);
        glDepthMask(true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /** Copies a matrix into reusable staging and uploads one uniform. */
    private void uploadMatrix(int location, Matrix4fc matrix) {
        matrix.get(matrixValues);
        glUniformMatrix4fv(location, false, matrixValues);
    }

    /** Releases realized GPU resources whose geometry descriptions were closed. */
    private void releaseClosedGeometryResources() {
        Iterator<Map.Entry<BufferGeometry, GeometryResource>> iterator =
                geometryResources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BufferGeometry, GeometryResource> entry = iterator.next();
            if (entry.getKey().isClosed()) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        resources.setActiveGeometryResources(geometryResources.size());
    }

    /** Releases realized GPU resources whose texture descriptions were closed. */
    private void releaseClosedTextureResources() {
        Iterator<Map.Entry<Texture, TextureResource>> iterator =
                textureResources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Texture, TextureResource> entry = iterator.next();
            if (entry.getKey().isClosed()) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        updateTextureResourceCount();
    }

    /** Releases derived GPU resources whose environment descriptions were closed. */
    private void releaseClosedEnvironmentResources() {
        Iterator<Map.Entry<EnvironmentMap, EnvironmentResource>> iterator =
                environmentResources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EnvironmentMap, EnvironmentResource> entry = iterator.next();
            if (entry.getKey().isClosed()) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        updateTextureResourceCount();
    }

    /** Requires one shared environment description to remain open for rendering. */
    private static void requireOpenEnvironment(EnvironmentMap environmentMap, String label) {
        if (environmentMap.isClosed()) {
            throw new IllegalStateException(label + " is closed");
        }
    }

    /** Rejects renderer use after close. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Renderer is closed");
        }
    }
}
