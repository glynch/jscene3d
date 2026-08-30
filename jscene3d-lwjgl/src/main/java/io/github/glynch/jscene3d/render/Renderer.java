/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Camera;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.IndexBuffer;
import io.github.glynch.jscene3d.core.Material;
import io.github.glynch.jscene3d.core.MaterialSide;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.internal.WindowContextRegistry;
import io.github.glynch.jscene3d.platform.Window;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Owns rendering and all OpenGL state for one JScene3D window context. */
public final class Renderer implements AutoCloseable {
    private final Window window;
    private final WindowContextRegistry.Access context;
    private final boolean automaticClear;
    private final RendererInfo info;
    private final RenderStatistics statistics;
    private final ResourceStatistics resources;
    private final IdentityHashMap<BufferGeometry, GeometryResource> geometryResources;
    private final RenderList renderList;
    private final Frustum frustum;
    private final float[] matrixValues;

    private Color clearColor;
    private float clearAlpha;
    private @Nullable BasicProgram basicProgram;
    private boolean customViewport;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private boolean closed;

    /** Initializes context-local renderer state after the window context is exclusively claimed. */
    private Renderer(Window window, WindowContextRegistry.Access context, RendererOptions options) {
        this.window = window;
        this.context = context;
        automaticClear = options.automaticClear();
        clearColor = options.clearColor();
        clearAlpha = options.clearAlpha();
        info = new RendererInfo();
        statistics = info.statistics();
        resources = info.resources();
        geometryResources = new IdentityHashMap<>();
        renderList = new RenderList();
        frustum = new Frustum();
        matrixValues = new float[16];
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
        statistics.beginFrame();

        int framebufferWidth = context.framebufferWidth();
        int framebufferHeight = context.framebufferHeight();
        applyViewport(framebufferWidth, framebufferHeight);
        if (automaticClear) {
            Color background = validScene.background();
            clearBuffers(background == null ? clearColor : background);
        }
        try {
            if (framebufferWidth > 0 && framebufferHeight > 0) {
                Matrix4fc viewMatrix = validCamera.viewMatrix();
                Matrix4fc projectionMatrix = validCamera.projectionMatrix();
                frustum.update(viewMatrix, projectionMatrix);
                renderList.build(validScene, frustum, statistics);
                for (int index = 0; index < renderList.opaqueCount(); index++) {
                    renderMesh(renderList.opaqueItem(index), viewMatrix, projectionMatrix);
                }
                for (int index = 0; index < renderList.transparentCount(); index++) {
                    renderMesh(renderList.transparentItem(index), viewMatrix, projectionMatrix);
                }
            }
            statistics.completeFrame();
        } finally {
            renderList.clear();
            glBindVertexArray(0);
        }
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
            for (GeometryResource resource : geometryResources.values()) {
                resource.close();
            }
            geometryResources.clear();
            if (basicProgram != null) {
                basicProgram.close();
                basicProgram = null;
            }
            glBindVertexArray(0);
            glUseProgram(0);
            renderList.clear();
            resources.setActiveGeometryResources(0);
            resources.setProgramCount(0);
            closed = true;
        } finally {
            WindowContextRegistry.release(window, context);
        }
    }

    /** Synchronizes and submits one prepared mesh draw. */
    private void renderMesh(RenderItem item, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        BufferGeometry geometry = item.geometry();
        BasicMaterial material = item.material();
        BasicProgram program = basicProgram();
        GeometryResource resource = geometryResources.computeIfAbsent(geometry, ignored -> new GeometryResource());
        resources.setActiveGeometryResources(geometryResources.size());
        resource.synchronize(geometry, material.usesVertexColors(), statistics);
        applyMaterialState(material);

        glUseProgram(program.id());
        uploadMatrix(program.modelMatrixLocation(), item.worldMatrix());
        uploadMatrix(program.viewMatrixLocation(), viewMatrix);
        uploadMatrix(program.projectionMatrixLocation(), projectionMatrix);
        Color color = material.color();
        float alpha = material.transparent() ? material.opacity() : 1.0f;
        glUniform4f(program.baseColorLocation(), color.red(), color.green(), color.blue(), alpha);
        glUniform1i(program.useVertexColorLocation(), material.usesVertexColors() ? 1 : 0);
        resource.bind();

        int start = geometry.drawRangeStart();
        int elementCount = item.elementCount();
        IndexBuffer index = geometry.index();
        if (index == null) {
            glDrawArrays(GL_TRIANGLES, start, elementCount);
        } else {
            glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_INT, (long) start * Integer.BYTES);
        }
        statistics.recordDraw(elementCount);
    }

    /** Lazily creates and returns the context-local built-in program. */
    private BasicProgram basicProgram() {
        if (basicProgram == null) {
            basicProgram = BasicProgram.create();
            resources.setProgramCount(1);
        }
        return basicProgram;
    }

    /** Applies depth, blending, and face-culling state for one material. */
    private void applyMaterialState(Material material) {
        if (material.depthTestEnabled()) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
        glDepthMask(material.depthWriteEnabled());

        if (material.transparent()) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else {
            glDisable(GL_BLEND);
        }

        MaterialSide side = material.side();
        if (side == MaterialSide.DOUBLE) {
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

    /** Rejects renderer use after close. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Renderer is closed");
        }
    }
}
