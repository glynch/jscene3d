/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.teapot;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.FlatShadedGeometry;
import io.github.glynch.jscene3d.geometries.TeapotGeometry;
import io.github.glynch.jscene3d.geometries.WireframeGeometry;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.Objects;

/** Owns the replaceable geometry and material presentations of the interactive Utah teapot. */
public final class TeapotPresentation implements AutoCloseable {
    private static final float TEAPOT_SIZE = 1.75f;
    private static final int TEXTURE_SIZE = 256;
    private static final int GRID_CELL_SIZE = 32;

    private final Object3D root = new Object3D();
    private final Texture uvGridTexture = createUvGridTexture();
    private final LineBasicMaterial wireframeMaterial = new LineBasicMaterial(Color.WHITE);
    private final PhongMaterial flatMaterial = createFlatMaterial();
    private final LambertMaterial smoothMaterial = createSmoothMaterial();
    private final PhongMaterial glossyMaterial = createGlossyMaterial();
    private final PhongMaterial texturedMaterial = createTexturedMaterial(uvGridTexture);
    private final StandardMaterial reflectiveMaterial = createReflectiveMaterial();

    private GeometrySet geometries;
    private final Mesh mesh;
    private final LineSegments wireframe;

    private int tessellation = 15;
    private boolean includeLid = true;
    private boolean includeBody = true;
    private boolean includeBottom = true;
    private boolean fittedLid;
    private boolean originalProportions;
    private Shading shading = Shading.GLOSSY;

    /** Creates a complete glossy teapot at the default tessellation level. */
    public TeapotPresentation() {
        geometries = createGeometries();
        mesh = new Mesh(geometries.smooth(), glossyMaterial);
        wireframe = new LineSegments(geometries.wireframe(), wireframeMaterial);
        wireframe.setVisible(false);
        root.add(mesh);
        root.add(wireframe);
    }

    /**
     * Returns the scene-graph root containing the surface and wireframe objects.
     *
     * @return stable presentation root
     */
    public Object3D root() {
        return root;
    }

    /**
     * Returns the current patch subdivision count.
     *
     * @return subdivision count, at least two
     */
    public int tessellation() {
        return tessellation;
    }

    /**
     * Rebuilds the teapot at a different patch subdivision count.
     *
     * @param tessellation subdivision count, at least two
     * @throws IllegalArgumentException if {@code tessellation} is less than two
     */
    public void setTessellation(int tessellation) {
        if (tessellation < 2) {
            throw new IllegalArgumentException("tessellation must be at least 2: " + tessellation);
        }
        if (this.tessellation != tessellation) {
            this.tessellation = tessellation;
            rebuildGeometry();
        }
    }

    /**
     * Returns whether the lid is included.
     *
     * @return whether the lid is included
     */
    public boolean includesLid() {
        return includeLid;
    }

    /**
     * Shows or hides the lid while preserving at least one visible teapot section.
     *
     * @param included whether the lid should be included
     */
    public void setIncludeLid(boolean included) {
        if (canChangeSection(includeLid, included, includeBody, includeBottom)) {
            includeLid = included;
            rebuildGeometry();
        }
    }

    /**
     * Returns whether the body and spout are included.
     *
     * @return whether the body and spout are included
     */
    public boolean includesBody() {
        return includeBody;
    }

    /**
     * Shows or hides the body while preserving at least one visible teapot section.
     *
     * @param included whether the body and spout should be included
     */
    public void setIncludeBody(boolean included) {
        if (canChangeSection(includeBody, included, includeLid, includeBottom)) {
            includeBody = included;
            rebuildGeometry();
        }
    }

    /**
     * Returns whether the bottom is included.
     *
     * @return whether the bottom is included
     */
    public boolean includesBottom() {
        return includeBottom;
    }

    /**
     * Shows or hides the bottom while preserving at least one visible teapot section.
     *
     * @param included whether the bottom should be included
     */
    public void setIncludeBottom(boolean included) {
        if (canChangeSection(includeBottom, included, includeLid, includeBody)) {
            includeBottom = included;
            rebuildGeometry();
        }
    }

    /**
     * Returns whether the lid is widened to fit the body opening.
     *
     * @return whether the fitted-lid correction is enabled
     */
    public boolean hasFittedLid() {
        return fittedLid;
    }

    /**
     * Enables or disables the fitted-lid correction.
     *
     * @param fittedLid whether the lid should be widened
     */
    public void setFittedLid(boolean fittedLid) {
        if (this.fittedLid != fittedLid) {
            this.fittedLid = fittedLid;
            rebuildGeometry();
        }
    }

    /**
     * Returns whether the original, vertically elongated proportions are used.
     *
     * @return whether original proportions are used
     */
    public boolean hasOriginalProportions() {
        return originalProportions;
    }

    /**
     * Selects original proportions or the customary Blinn height correction.
     *
     * @param originalProportions whether original proportions should be used
     */
    public void setOriginalProportions(boolean originalProportions) {
        if (this.originalProportions != originalProportions) {
            this.originalProportions = originalProportions;
            rebuildGeometry();
        }
    }

    /**
     * Returns the active surface presentation.
     *
     * @return active shading mode
     */
    public Shading shading() {
        return shading;
    }

    /**
     * Selects the wireframe, lighting, texture, or reflective presentation.
     *
     * @param shading non-null shading mode
     * @throws NullPointerException if {@code shading} is {@code null}
     */
    public void setShading(Shading shading) {
        Shading validShading = Objects.requireNonNull(shading, "shading");
        if (this.shading != validShading) {
            this.shading = validShading;
            applyShading();
        }
    }

    /** Closes all geometry, material, and generated-texture resources owned by this presentation. */
    @Override
    public void close() {
        geometries.close();
        reflectiveMaterial.close();
        texturedMaterial.close();
        glossyMaterial.close();
        smoothMaterial.close();
        flatMaterial.close();
        wireframeMaterial.close();
        uvGridTexture.close();
    }

    /** Returns whether a requested section change is both different and leaves visible geometry. */
    private static boolean canChangeSection(
            boolean current, boolean requested, boolean otherFirst, boolean otherSecond) {
        return current != requested && (requested || otherFirst || otherSecond);
    }

    /** Replaces all derived presentation geometry atomically. */
    private void rebuildGeometry() {
        GeometrySet replacement = createGeometries();
        GeometrySet previous = geometries;
        geometries = replacement;
        mesh.setGeometry(shading == Shading.FLAT ? replacement.flat() : replacement.smooth());
        wireframe.setGeometry(replacement.wireframe());
        previous.close();
    }

    /** Generates smooth, flat, and wireframe variants from the same current settings. */
    private GeometrySet createGeometries() {
        BufferGeometry smooth = TeapotGeometry.builder(TEAPOT_SIZE)
                .segments(tessellation)
                .includeLid(includeLid)
                .includeBody(includeBody)
                .includeBottom(includeBottom)
                .fittedLid(fittedLid)
                .blinnProportions(!originalProportions)
                .build();
        try {
            BufferGeometry flat = FlatShadedGeometry.create(smooth);
            try {
                BufferGeometry wire = WireframeGeometry.create(smooth);
                return new GeometrySet(smooth, flat, wire);
            } catch (RuntimeException exception) {
                flat.close();
                throw exception;
            }
        } catch (RuntimeException exception) {
            smooth.close();
            throw exception;
        }
    }

    /** Applies the active shading mode without rebuilding unchanged geometry. */
    private void applyShading() {
        boolean wireframeVisible = shading == Shading.WIREFRAME;
        wireframe.setVisible(wireframeVisible);
        mesh.setVisible(!wireframeVisible);
        if (!wireframeVisible) {
            mesh.setGeometry(shading == Shading.FLAT ? geometries.flat() : geometries.smooth());
            mesh.setMaterial(materialFor(shading));
        }
    }

    /** Selects the owned surface material for one non-wireframe mode. */
    private Material materialFor(Shading shading) {
        return switch (shading) {
            case FLAT -> flatMaterial;
            case SMOOTH -> smoothMaterial;
            case GLOSSY -> glossyMaterial;
            case TEXTURED -> texturedMaterial;
            case REFLECTIVE -> reflectiveMaterial;
            case WIREFRAME -> throw new IllegalArgumentException("Wireframe mode has no surface material");
        };
    }

    /** Creates the low-specular material used with expanded face normals. */
    private static PhongMaterial createFlatMaterial() {
        PhongMaterial material = new PhongMaterial(Color.srgb(0xf4f4f4));
        material.setSpecular(Color.BLACK);
        material.setShininess(0.0f);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }

    /** Creates the diffuse smooth-shaded material. */
    private static LambertMaterial createSmoothMaterial() {
        LambertMaterial material = new LambertMaterial(Color.srgb(0xf4f4f4));
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }

    /** Creates the highly polished Blinn-Phong material. */
    private static PhongMaterial createGlossyMaterial() {
        PhongMaterial material = new PhongMaterial(Color.srgb(0xc0c0c0));
        material.setSpecular(Color.WHITE);
        material.setShininess(300.0f);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }

    /** Creates the UV diagnostic material sharing the generated grid texture. */
    private static PhongMaterial createTexturedMaterial(Texture texture) {
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setColorMap(texture);
        material.setSpecular(Color.srgb(0x333333));
        material.setShininess(60.0f);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }

    /** Creates the metallic material illuminated by the scene environment. */
    private static StandardMaterial createReflectiveMaterial() {
        StandardMaterial material = new StandardMaterial(Color.srgb(0xd8d8d8));
        material.setMetalness(1.0f);
        material.setRoughness(0.08f);
        material.setEnvironmentIntensity(1.2f);
        material.setSide(MaterialSide.DOUBLE);
        return material;
    }

    /** Creates a colourful square UV grid without requiring another bundled asset. */
    private static Texture createUvGridTexture() {
        byte[] pixels = new byte[TEXTURE_SIZE * TEXTURE_SIZE * 4];
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                writeUvGridPixel(pixels, x, y);
            }
        }
        return Texture.baseColor(TEXTURE_SIZE, TEXTURE_SIZE, pixels);
    }

    /** Writes one coloured grid pixel into top-row-first RGBA storage. */
    private static void writeUvGridPixel(byte[] pixels, int x, int y) {
        int offset = (y * TEXTURE_SIZE + x) * 4;
        boolean gridLine = x % GRID_CELL_SIZE < 2 || y % GRID_CELL_SIZE < 2;
        int cellX = x / GRID_CELL_SIZE;
        int cellY = y / GRID_CELL_SIZE;
        int red = gridLine ? 245 : 48 + cellX * 24;
        int green = gridLine ? 245 : 48 + cellY * 24;
        int blue = gridLine ? 245 : 210 - ((cellX + cellY) % 4) * 38;
        pixels[offset] = (byte) red;
        pixels[offset + 1] = (byte) green;
        pixels[offset + 2] = (byte) blue;
        pixels[offset + 3] = (byte) 0xff;
    }

    /** Complete set of mutually consistent geometry presentations. */
    private record GeometrySet(BufferGeometry smooth, BufferGeometry flat, BufferGeometry wireframe)
            implements AutoCloseable {
        @Override
        public void close() {
            wireframe.close();
            flat.close();
            smooth.close();
        }
    }

    /** Surface presentations offered by the interactive example. */
    public enum Shading {
        /** Unique triangle edges rendered as lines. */
        WIREFRAME,
        /** One lighting normal per triangle face. */
        FLAT,
        /** Interpolated diffuse lighting. */
        SMOOTH,
        /** Interpolated high-shininess lighting. */
        GLOSSY,
        /** Generated UV diagnostic texture. */
        TEXTURED,
        /** Metallic image-based reflection. */
        REFLECTIVE
    }
}
