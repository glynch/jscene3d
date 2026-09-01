/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_MAX_TEXTURE_SIZE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.DirectionalLightShadow;
import io.github.glynch.jscene3d.lights.LightShadow;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.lights.ShadowCastingLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.render.internal.ShadowFrame.PointShadow;
import io.github.glynch.jscene3d.render.internal.ShadowFrame.ShadowRenderMetrics;
import io.github.glynch.jscene3d.render.internal.ShadowFrame.TwoDimensionalShadow;
import io.github.glynch.jscene3d.render.internal.programs.ShadowDepthProgram;
import io.github.glynch.jscene3d.render.internal.resources.GeometryResource;
import io.github.glynch.jscene3d.render.internal.resources.ShadowMapResource;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Owns context-local shadow maps, depth rendering, and per-frame light projection state. */
public final class ShadowRenderer implements AutoCloseable {
    private static final Vector3f[] CUBE_DIRECTIONS = {
        new Vector3f(1.0f, 0.0f, 0.0f),
        new Vector3f(-1.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, 1.0f, 0.0f),
        new Vector3f(0.0f, -1.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, 1.0f),
        new Vector3f(0.0f, 0.0f, -1.0f)
    };
    private static final Vector3f[] CUBE_UPS = {
        new Vector3f(0.0f, -1.0f, 0.0f),
        new Vector3f(0.0f, -1.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, 1.0f),
        new Vector3f(0.0f, 0.0f, -1.0f),
        new Vector3f(0.0f, -1.0f, 0.0f),
        new Vector3f(0.0f, -1.0f, 0.0f)
    };

    private final IdentityHashMap<ShadowCastingLight, ShadowMapResource> resources;
    private final ArrayList<Mesh> casters;
    private final ArrayDeque<Object3D> pendingObjects;
    private final Set<ShadowCastingLight> activeLights;
    private final Matrix4f projection;
    private final Matrix4f view;
    private final Matrix4f viewProjection;
    private final Matrix4f inverseMainView;
    private final Matrix4f biasMatrix;
    private final Vector3f lightPosition;
    private final Vector3f lightTarget;
    private final Vector3f cubeTarget;
    private final Vector3f up;
    private final int maximumTextureSize;

    private ShadowDepthProgram projectedProgram;
    private ShadowDepthProgram pointProgram;
    private int drawCalls;
    private long triangles;
    private int uploads;
    private long uploadedBytes;

    /** Creates an unrealized renderer-owned shadow subsystem. */
    public ShadowRenderer() {
        resources = new IdentityHashMap<>();
        casters = new ArrayList<>();
        pendingObjects = new ArrayDeque<>();
        activeLights = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        projection = new Matrix4f();
        view = new Matrix4f();
        viewProjection = new Matrix4f();
        inverseMainView = new Matrix4f();
        biasMatrix = new Matrix4f().translate(0.5f, 0.5f, 0.5f).scale(0.5f);
        lightPosition = new Vector3f();
        lightTarget = new Vector3f();
        cubeTarget = new Vector3f();
        up = new Vector3f();
        maximumTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
    }

    /**
     * Generates every enabled visible light's shadow map and returns sampling state.
     *
     * @param scene current scene
     * @param lights visible lights collected for the main frame
     * @param mainViewMatrix main camera view matrix
     * @param geometryResources renderer geometry cache shared with the main pass
     * @return immutable completed frame state
     */
    public ShadowFrame render(
            Scene scene,
            LightCollection lights,
            Matrix4fc mainViewMatrix,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        collectCasters(scene);
        activeLights.clear();
        drawCalls = 0;
        triangles = 0L;
        uploads = 0;
        uploadedBytes = 0L;
        inverseMainView.set(mainViewMatrix).invert();
        requireSupportedShadowCounts(lights);
        int[] directionalIndices = indices(lights.directionalLightCount());
        int[] spotIndices = indices(lights.spotLightCount());
        int[] pointIndices = indices(lights.pointLightCount());
        ArrayList<TwoDimensionalShadow> twoDimensional = new ArrayList<>();
        ArrayList<PointShadow> points = new ArrayList<>();
        int passes = renderDirectional(lights, directionalIndices, twoDimensional, geometryResources);
        passes += renderSpots(lights, spotIndices, twoDimensional, geometryResources);
        passes += renderPoints(lights, pointIndices, points, geometryResources);
        releaseInactiveResources();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        ShadowRenderMetrics metrics = new ShadowRenderMetrics(
                twoDimensional.size() + points.size(), passes, drawCalls, triangles, uploads, uploadedBytes);
        return new ShadowFrame(twoDimensional, points, directionalIndices, spotIndices, pointIndices, metrics);
    }

    /**
     * Returns the number of retained per-light shadow maps.
     *
     * @return active map count
     */
    public int resourceCount() {
        return resources.size();
    }

    /**
     * Returns the number of realized projected and radial depth programs.
     *
     * @return value from zero through two
     */
    public int programCount() {
        return (projectedProgram == null ? 0 : 1) + (pointProgram == null ? 0 : 1);
    }

    /** Releases all per-light maps and realized depth programs. */
    @Override
    public void close() {
        resources.values().forEach(ShadowMapResource::close);
        resources.clear();
        if (projectedProgram != null) {
            projectedProgram.close();
            projectedProgram = null;
        }
        if (pointProgram != null) {
            pointProgram.close();
            pointProgram = null;
        }
    }

    /** Generates enabled directional maps until the shared two-dimensional limit is reached. */
    private int renderDirectional(
            LightCollection lights,
            int[] indices,
            List<TwoDimensionalShadow> entries,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        int passes = 0;
        for (int index = 0;
                index < lights.directionalLightCount() && entries.size() < ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS;
                index++) {
            DirectionalLight light = lights.directionalLight(index);
            if (!light.isShadowCastingEnabled()) {
                continue;
            }
            DirectionalLightShadow shadow = light.shadow();
            light.worldPosition(lightPosition);
            light.target(lightTarget);
            setLookAt(lightPosition, lightTarget);
            projection.setOrtho(
                    shadow.cameraLeft(),
                    shadow.cameraRight(),
                    shadow.cameraBottom(),
                    shadow.cameraTop(),
                    shadow.cameraNear(),
                    shadow.cameraFar());
            viewProjection.set(projection).mul(view);
            ShadowMapResource resource = renderMap(light, shadow, false, 0, geometryResources);
            Matrix4f textureFromView =
                    new Matrix4f(biasMatrix).mul(viewProjection).mul(inverseMainView);
            indices[index] = entries.size();
            entries.add(new TwoDimensionalShadow(resource, textureFromView, shadow.bias(), shadow.normalBias()));
            passes++;
        }
        return passes;
    }

    /** Generates enabled spotlight maps using the light cone as the shadow camera field of view. */
    private int renderSpots(
            LightCollection lights,
            int[] indices,
            List<TwoDimensionalShadow> entries,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        int passes = 0;
        for (int index = 0;
                index < lights.spotLightCount() && entries.size() < ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS;
                index++) {
            SpotLight light = lights.spotLight(index);
            if (!light.isShadowCastingEnabled()) {
                continue;
            }
            LightShadow shadow = light.shadow();
            light.worldPosition(lightPosition);
            light.target(lightTarget);
            setLookAt(lightPosition, lightTarget);
            float aspect = (float) shadow.mapWidth() / shadow.mapHeight();
            projection.setPerspective(light.angle() * 2.0f, aspect, shadow.cameraNear(), shadow.cameraFar());
            viewProjection.set(projection).mul(view);
            ShadowMapResource resource = renderMap(light, shadow, false, 0, geometryResources);
            Matrix4f textureFromView =
                    new Matrix4f(biasMatrix).mul(viewProjection).mul(inverseMainView);
            indices[index] = entries.size();
            entries.add(new TwoDimensionalShadow(resource, textureFromView, shadow.bias(), shadow.normalBias()));
            passes++;
        }
        return passes;
    }

    /** Generates six radial-depth faces for every enabled point light up to the cube-map limit. */
    private int renderPoints(
            LightCollection lights,
            int[] indices,
            List<PointShadow> entries,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        int passes = 0;
        for (int index = 0;
                index < lights.pointLightCount() && entries.size() < ShadowFrame.MAX_POINT_SHADOWS;
                index++) {
            PointLight light = lights.pointLight(index);
            if (!light.isShadowCastingEnabled()) {
                continue;
            }
            LightShadow shadow = light.shadow();
            light.worldPosition(lightPosition);
            projection.setPerspective((float) Math.PI / 2.0f, 1.0f, shadow.cameraNear(), shadow.cameraFar());
            ShadowMapResource resource = resource(light, shadow, true);
            for (int face = 0; face < 6; face++) {
                cubeTarget.set(lightPosition).add(CUBE_DIRECTIONS[face]);
                view.setLookAt(lightPosition, cubeTarget, CUBE_UPS[face]);
                viewProjection.set(projection).mul(view);
                renderFace(resource, shadow, true, face, geometryResources);
                passes++;
            }
            indices[index] = entries.size();
            entries.add(new PointShadow(
                    resource, new Vector3f(lightPosition), shadow.cameraFar(), shadow.bias(), shadow.normalBias()));
        }
        return passes;
    }

    /** Realizes and renders one non-cube map. */
    private ShadowMapResource renderMap(
            ShadowCastingLight light,
            LightShadow shadow,
            boolean cube,
            int face,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        ShadowMapResource resource = resource(light, shadow, cube);
        renderFace(resource, shadow, false, face, geometryResources);
        return resource;
    }

    /** Clears and draws all visible casters into one selected depth image. */
    private void renderFace(
            ShadowMapResource resource,
            LightShadow shadow,
            boolean radialDepth,
            int face,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        resource.bindForWriting(face);
        glViewport(0, 0, shadow.mapWidth(), shadow.mapHeight());
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glClear(GL_DEPTH_BUFFER_BIT);
        ShadowDepthProgram activeProgram = radialDepth ? pointProgram() : projectedProgram();
        glUseProgram(activeProgram.id());
        if (radialDepth) {
            activeProgram.uploadPointPass(viewProjection, lightPosition, shadow.cameraFar());
        } else {
            activeProgram.uploadProjectedPass(viewProjection);
        }
        for (Mesh mesh : casters) {
            drawCaster(mesh, activeProgram, geometryResources);
        }
    }

    /** Synchronizes and draws one caster with material face orientation respected. */
    private void drawCaster(
            Mesh mesh,
            ShadowDepthProgram activeProgram,
            IdentityHashMap<BufferGeometry, GeometryResource> geometryResources) {
        BufferGeometry geometry = mesh.geometry();
        int elementCount = geometry.drawRangeCount();
        if (elementCount < 3) {
            return;
        }
        if (elementCount % 3 != 0) {
            throw new IllegalStateException("Shadow-casting mesh draw range is not divisible by 3: " + elementCount);
        }
        applySide(mesh.material());
        GeometryResource geometryResource =
                geometryResources.computeIfAbsent(geometry, ignored -> new GeometryResource());
        GeometryResource.UploadResult result =
                geometryResource.synchronize(geometry, false, false, false, "Shadow pass");
        uploads += result.count();
        uploadedBytes += result.byteCount();
        glUseProgram(activeProgram.id());
        activeProgram.uploadModel(mesh.matrixWorld());
        geometryResource.bind();
        int start = geometry.drawRangeStart();
        IndexBuffer index = geometry.index();
        if (index == null) {
            glDrawArrays(PrimitiveTopology.TRIANGLES.openGlMode(), start, elementCount);
        } else {
            glDrawElements(
                    PrimitiveTopology.TRIANGLES.openGlMode(),
                    elementCount,
                    GL_UNSIGNED_INT,
                    (long) start * Integer.BYTES);
        }
        drawCalls++;
        triangles += elementCount / 3L;
    }

    /** Applies only face culling from a caster's material; shadow depth always writes and tests. */
    private static void applySide(Material material) {
        MaterialSide side = material.side();
        if (side == MaterialSide.DOUBLE) {
            glDisable(GL_CULL_FACE);
            return;
        }
        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glCullFace(side == MaterialSide.FRONT ? GL_BACK : GL_FRONT);
    }

    /** Collects visible, visible-material meshes that explicitly cast shadows. */
    private void collectCasters(Scene scene) {
        casters.clear();
        pendingObjects.clear();
        pendingObjects.push(scene);
        while (!pendingObjects.isEmpty()) {
            Object3D object = pendingObjects.pop();
            if (!object.isVisible()) {
                continue;
            }
            if (object instanceof Mesh mesh
                    && mesh.isShadowCastingEnabled()
                    && mesh.material().visible()) {
                casters.add(mesh);
            }
            List<Object3D> children = object.children();
            for (int index = children.size() - 1; index >= 0; index--) {
                pendingObjects.push(children.get(index));
            }
        }
    }

    /** Resolves and resizes one light-owned map while validating context limits and map kind. */
    private ShadowMapResource resource(ShadowCastingLight light, LightShadow shadow, boolean cube) {
        requireSupportedMapSize(shadow);
        ShadowMapResource resource = resources.get(light);
        if (resource == null || resource.isCube() != cube) {
            if (resource != null) {
                resource.close();
            }
            resource = new ShadowMapResource(cube);
            resources.put(light, resource);
        }
        resource.realize(shadow.mapWidth(), shadow.mapHeight());
        activeLights.add(light);
        return resource;
    }

    /** Rejects requested dimensions beyond the active OpenGL context's limit. */
    private void requireSupportedMapSize(LightShadow shadow) {
        if (shadow.mapWidth() > maximumTextureSize || shadow.mapHeight() > maximumTextureSize) {
            throw new IllegalStateException("Shadow-map dimensions exceed this context's maximum texture size "
                    + maximumTextureSize
                    + ": "
                    + shadow.mapWidth()
                    + " x "
                    + shadow.mapHeight());
        }
    }

    /** Drops maps for lights not actively generating shadows in this frame. */
    private void releaseInactiveResources() {
        Iterator<Map.Entry<ShadowCastingLight, ShadowMapResource>> iterator =
                resources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ShadowCastingLight, ShadowMapResource> entry = iterator.next();
            if (!activeLights.contains(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    /** Rejects visible enabled shadow lights that cannot be represented by built-in shaders. */
    private static void requireSupportedShadowCounts(LightCollection lights) {
        int twoDimensionalCount = 0;
        for (int index = 0; index < lights.directionalLightCount(); index++) {
            if (lights.directionalLight(index).isShadowCastingEnabled()) {
                twoDimensionalCount++;
            }
        }
        for (int index = 0; index < lights.spotLightCount(); index++) {
            if (lights.spotLight(index).isShadowCastingEnabled()) {
                twoDimensionalCount++;
            }
        }
        if (twoDimensionalCount > ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS) {
            throw new IllegalStateException("Scene has more visible directional and spot shadow maps than Renderer "
                    + "supports: "
                    + twoDimensionalCount
                    + " > "
                    + ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS);
        }
        int pointCount = 0;
        for (int index = 0; index < lights.pointLightCount(); index++) {
            if (lights.pointLight(index).isShadowCastingEnabled()) {
                pointCount++;
            }
        }
        if (pointCount > ShadowFrame.MAX_POINT_SHADOWS) {
            throw new IllegalStateException("Scene has more visible point-light shadow maps than Renderer supports: "
                    + pointCount
                    + " > "
                    + ShadowFrame.MAX_POINT_SHADOWS);
        }
    }

    /** Initializes a light-index mapping to the no-shadow sentinel. */
    private static int[] indices(int size) {
        int[] values = new int[size];
        Arrays.fill(values, -1);
        return values;
    }

    /** Builds a stable look-at matrix, selecting an alternate up axis near vertical directions. */
    private void setLookAt(Vector3f position, Vector3f target) {
        Vector3f direction = cubeTarget.set(target).sub(position);
        if (direction.lengthSquared() == 0.0f) {
            throw new IllegalStateException("Shadow-casting light position must differ from its target");
        }
        direction.normalize();
        if (Math.abs(direction.y()) > 0.999f) {
            up.set(0.0f, 0.0f, 1.0f);
        } else {
            up.set(0.0f, 1.0f, 0.0f);
        }
        view.setLookAt(position, target, up);
    }

    /** Lazily realizes the projected depth program. */
    private ShadowDepthProgram projectedProgram() {
        if (projectedProgram == null) {
            projectedProgram = ShadowDepthProgram.createProjected();
        }
        return projectedProgram;
    }

    /** Lazily realizes the radial point-light depth program. */
    private ShadowDepthProgram pointProgram() {
        if (pointProgram == null) {
            pointProgram = ShadowDepthProgram.createPoint();
        }
        return pointProgram;
    }
}
