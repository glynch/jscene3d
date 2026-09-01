/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Arrays;

/** Tessellates the Utah teapot's bicubic Bézier patches into indexed triangles. */
public final class TeapotGeometry {
    private static final int DEFAULT_SEGMENTS = 10;
    private static final int BODY_PATCH_END = 20;
    private static final int LID_PATCH_END = 28;
    private static final int PATCH_COUNT = 32;
    private static final int CONTROL_POINTS_PER_PATCH = 16;
    private static final double DATA_HEIGHT = 3.15;
    private static final double BLINN_SCALE = 1.3;
    private static final double FITTED_LID_SCALE = 1.077;

    /** Prevents instantiation of this geometry factory. */
    private TeapotGeometry() {
        throw new AssertionError("TeapotGeometry cannot be instantiated");
    }

    /**
     * Creates a complete fitted-lid teapot with ten segments per patch edge and traditional Blinn
     * proportions.
     *
     * @param size finite positive nominal half-height
     * @return new application-owned geometry
     * @throws IllegalArgumentException if {@code size} is invalid
     */
    public static BufferGeometry create(float size) {
        return builder(size).build();
    }

    /**
     * Creates a complete fitted-lid teapot with traditional Blinn proportions.
     *
     * @param size finite positive nominal half-height
     * @param segments patch-edge segments, at least two
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or requested arrays exceed Java
     *     array limits
     */
    public static BufferGeometry create(float size, int segments) {
        return builder(size).segments(segments).build();
    }

    /**
     * Creates a configurable geometry builder.
     *
     * @param size finite positive nominal half-height
     * @return new one-use-independent builder
     * @throws IllegalArgumentException if {@code size} is invalid
     */
    public static Builder builder(float size) {
        return new Builder(Preconditions.requirePositive(size, "size"));
    }

    /** Tessellates the selected patches using the builder's validated configuration. */
    private static BufferGeometry build(Builder options) {
        int segments = options.segments;
        int patchCount = includedPatchCount(options);
        if (patchCount == 0) {
            throw new IllegalArgumentException("At least one teapot section must be included");
        }

        long verticesPerPatch = (segments + 1L) * (segments + 1L);
        long vertexCount = patchCount * verticesPerPatch;
        long maximumTriangleCount = (long) patchCount * segments * segments * 2L;
        float[] positions = new float[Preconditions.requireArrayLength(vertexCount, 3, "position and normal")];
        GeometryBuffers buffers = new GeometryBuffers(
                positions,
                new float[positions.length],
                new float[Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate")],
                new int[Preconditions.requireArrayLength(maximumTriangleCount, 3, "index")]);

        double maximumHeight = DATA_HEIGHT * (options.blinnProportions ? 1.0 : BLINN_SCALE);
        double halfHeight = maximumHeight * 0.5;
        double outputScale = options.size / halfHeight;
        int indexCount = tessellatePatches(options, outputScale, halfHeight, buffers);

        return BufferGeometryFactorySupport.create(
                buffers.positions,
                buffers.normals,
                buffers.textureCoordinates,
                Arrays.copyOf(buffers.candidateIndices, indexCount));
    }

    /** Counts the canonical patches enabled by one builder configuration. */
    private static int includedPatchCount(Builder options) {
        int bodyPatchCount = options.bodyIncluded ? BODY_PATCH_END : 0;
        int lidPatchCount = options.lidIncluded ? LID_PATCH_END - BODY_PATCH_END : 0;
        int bottomPatchCount = options.bottomIncluded ? PATCH_COUNT - LID_PATCH_END : 0;
        return bodyPatchCount + lidPatchCount + bottomPatchCount;
    }

    /** Tessellates every enabled patch and returns the emitted index count. */
    private static int tessellatePatches(
            Builder options, double outputScale, double halfHeight, GeometryBuffers buffers) {
        int indexOffset = 0;
        int emittedPatchCount = 0;
        for (int patch = 0; patch < PATCH_COUNT; patch++) {
            if (!options.includesPatch(patch)) {
                continue;
            }
            int rowLength = options.segments + 1;
            int patchVertexStart = emittedPatchCount * rowLength * rowLength;
            tessellatePatchVertices(patch, patchVertexStart, options, outputScale, halfHeight, buffers);
            indexOffset = appendPatchIndices(patchVertexStart, options.segments, rowLength, buffers, indexOffset);
            emittedPatchCount++;
        }
        return indexOffset;
    }

    /** Evaluates and writes every vertex belonging to one canonical patch. */
    private static void tessellatePatchVertices(
            int patch,
            int patchVertexStart,
            Builder options,
            double outputScale,
            double halfHeight,
            GeometryBuffers buffers) {
        int segments = options.segments;
        int rowLength = segments + 1;
        double[] sBasis = new double[4];
        double[] tBasis = new double[4];
        double[] sDerivative = new double[4];
        double[] tDerivative = new double[4];
        for (int sStep = 0; sStep <= segments; sStep++) {
            double s = (double) sStep / segments;
            evaluateBasis(s, sBasis, sDerivative);
            for (int tStep = 0; tStep <= segments; tStep++) {
                double t = (double) tStep / segments;
                evaluateBasis(t, tBasis, tDerivative);
                SurfacePoint point = evaluateSurface(patch, sBasis, tBasis, sDerivative, tDerivative, options);
                int vertexIndex = patchVertexStart + sStep * rowLength + tStep;
                writeVertex(buffers, vertexIndex, point, outputScale, halfHeight, s, t);
            }
        }
    }

    /** Writes one evaluated surface point in JScene3D coordinates. */
    private static void writeVertex(
            GeometryBuffers buffers,
            int vertexIndex,
            SurfacePoint point,
            double outputScale,
            double halfHeight,
            double s,
            double t) {
        int positionOffset = vertexIndex * 3;
        buffers.positions[positionOffset] = (float) (outputScale * point.x());
        buffers.normals[positionOffset++] = (float) point.normalX();
        buffers.positions[positionOffset] = (float) (outputScale * (point.z() - halfHeight));
        buffers.normals[positionOffset++] = (float) point.normalZ();
        buffers.positions[positionOffset] = (float) (-outputScale * point.y());
        buffers.normals[positionOffset] = (float) -point.normalY();
        int textureCoordinateOffset = vertexIndex * 2;
        buffers.textureCoordinates[textureCoordinateOffset] = (float) (1.0 - t);
        buffers.textureCoordinates[textureCoordinateOffset + 1] = (float) (1.0 - s);
    }

    /** Appends every non-degenerate triangle belonging to one tessellated patch. */
    private static int appendPatchIndices(
            int patchVertexStart, int segments, int rowLength, GeometryBuffers buffers, int indexOffset) {
        for (int sStep = 0; sStep < segments; sStep++) {
            for (int tStep = 0; tStep < segments; tStep++) {
                int first = patchVertexStart + sStep * rowLength + tStep;
                int second = first + 1;
                int third = second + rowLength;
                int fourth = first + rowLength;
                indexOffset =
                        appendTriangle(buffers.candidateIndices, indexOffset, buffers.positions, first, second, third);
                indexOffset =
                        appendTriangle(buffers.candidateIndices, indexOffset, buffers.positions, first, third, fourth);
            }
        }
        return indexOffset;
    }

    /** Evaluates cubic Bernstein basis values and first derivatives at one parameter. */
    private static void evaluateBasis(double parameter, double[] basis, double[] derivative) {
        double inverse = 1.0 - parameter;
        double inverseSquared = inverse * inverse;
        double parameterSquared = parameter * parameter;
        basis[0] = inverseSquared * inverse;
        basis[1] = 3.0 * parameter * inverseSquared;
        basis[2] = 3.0 * parameterSquared * inverse;
        basis[3] = parameterSquared * parameter;
        derivative[0] = -3.0 * inverseSquared;
        derivative[1] = 3.0 * inverseSquared - 6.0 * parameter * inverse;
        derivative[2] = 6.0 * parameter * inverse - 3.0 * parameterSquared;
        derivative[3] = 3.0 * parameterSquared;
    }

    /** Evaluates one patch position and its oriented unit normal. */
    private static SurfacePoint evaluateSurface(
            int patch, double[] sBasis, double[] tBasis, double[] sDerivative, double[] tDerivative, Builder options) {
        double horizontalScale =
                options.fittedLid && patch >= BODY_PATCH_END && patch < LID_PATCH_END ? FITTED_LID_SCALE : 1.0;
        double verticalScale = options.blinnProportions ? 1.0 : BLINN_SCALE;
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        double tangentSx = 0.0;
        double tangentSy = 0.0;
        double tangentSz = 0.0;
        double tangentTx = 0.0;
        double tangentTy = 0.0;
        double tangentTz = 0.0;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int patchOffset = patch * CONTROL_POINTS_PER_PATCH + row * 4 + column;
                int controlPointOffset = TeapotData.PATCHES[patchOffset] * 3;
                double controlX = TeapotData.CONTROL_POINTS[controlPointOffset] * horizontalScale;
                double controlY = TeapotData.CONTROL_POINTS[controlPointOffset + 1] * horizontalScale;
                double controlZ = TeapotData.CONTROL_POINTS[controlPointOffset + 2] * verticalScale;

                double weight = sBasis[row] * tBasis[column];
                double sWeight = sDerivative[row] * tBasis[column];
                double tWeight = sBasis[row] * tDerivative[column];
                x += weight * controlX;
                y += weight * controlY;
                z += weight * controlZ;
                tangentSx += sWeight * controlX;
                tangentSy += sWeight * controlY;
                tangentSz += sWeight * controlZ;
                tangentTx += tWeight * controlX;
                tangentTy += tWeight * controlY;
                tangentTz += tWeight * controlZ;
            }
        }

        if (x == 0.0 && y == 0.0) {
            double halfDataHeight = DATA_HEIGHT * verticalScale * 0.5;
            double normalZ = z > halfDataHeight ? 1.0 : -1.0;
            return new SurfacePoint(x, y, z, 0.0, 0.0, normalZ);
        }
        double normalX = tangentTy * tangentSz - tangentTz * tangentSy;
        double normalY = tangentTz * tangentSx - tangentTx * tangentSz;
        double normalZ = tangentTx * tangentSy - tangentTy * tangentSx;
        double inverseLength = 1.0 / Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        return new SurfacePoint(x, y, z, normalX * inverseLength, normalY * inverseLength, normalZ * inverseLength);
    }

    /** Appends one non-degenerate triangle and returns the next insertion offset. */
    private static int appendTriangle(int[] indices, int offset, float[] positions, int first, int second, int third) {
        if (samePosition(positions, first, second)
                || samePosition(positions, first, third)
                || samePosition(positions, second, third)) {
            return offset;
        }
        indices[offset++] = first;
        indices[offset++] = second;
        indices[offset++] = third;
        return offset;
    }

    /** Returns whether two vertices have exactly matching generated positions. */
    private static boolean samePosition(float[] positions, int first, int second) {
        int firstOffset = first * 3;
        int secondOffset = second * 3;
        return positions[firstOffset] == positions[secondOffset]
                && positions[firstOffset + 1] == positions[secondOffset + 1]
                && positions[firstOffset + 2] == positions[secondOffset + 2];
    }

    /** One evaluated surface sample in the canonical teapot coordinate system. */
    private record SurfacePoint(double x, double y, double z, double normalX, double normalY, double normalZ) {}

    /** Mutable-array storage shared while tessellating one geometry. */
    private static final class GeometryBuffers {
        private final float[] positions;
        private final float[] normals;
        private final float[] textureCoordinates;
        private final int[] candidateIndices;

        /** Retains the arrays allocated for one tessellation operation. */
        private GeometryBuffers(
                float[] positions, float[] normals, float[] textureCoordinates, int[] candidateIndices) {
            this.positions = positions;
            this.normals = normals;
            this.textureCoordinates = textureCoordinates;
            this.candidateIndices = candidateIndices;
        }
    }

    /** Mutable fluent configuration for one generated teapot. */
    public static final class Builder {
        private final float size;

        private int segments = DEFAULT_SEGMENTS;
        private boolean bottomIncluded = true;
        private boolean lidIncluded = true;
        private boolean bodyIncluded = true;
        private boolean fittedLid = true;
        private boolean blinnProportions = true;

        /** Retains the validated nominal size. */
        private Builder(float size) {
            this.size = size;
        }

        /**
         * Selects the number of segments along each Bézier patch edge.
         *
         * @param segments segment count, at least two
         * @return this builder
         * @throws IllegalArgumentException if {@code segments} is less than two
         */
        public Builder segments(int segments) {
            this.segments = Preconditions.requireInRange(segments, 2, Integer.MAX_VALUE, "segments");
            return this;
        }

        /**
         * Selects whether the four bottom patches are generated.
         *
         * @param included whether the bottom is included
         * @return this builder
         */
        public Builder includeBottom(boolean included) {
            bottomIncluded = included;
            return this;
        }

        /**
         * Selects whether the eight lid patches are generated.
         *
         * @param included whether the lid is included
         * @return this builder
         */
        public Builder includeLid(boolean included) {
            lidIncluded = included;
            return this;
        }

        /**
         * Selects whether the body, rim, handle, and spout patches are generated.
         *
         * @param included whether the body is included
         * @return this builder
         */
        public Builder includeBody(boolean included) {
            bodyIncluded = included;
            return this;
        }

        /**
         * Selects whether the lid is widened slightly to close the historical gap.
         *
         * @param fitted whether the lid is fitted
         * @return this builder
         */
        public Builder fittedLid(boolean fitted) {
            fittedLid = fitted;
            return this;
        }

        /**
         * Selects traditional vertically compressed Blinn proportions or the original height.
         *
         * @param enabled whether traditional Blinn proportions are used
         * @return this builder
         */
        public Builder blinnProportions(boolean enabled) {
            blinnProportions = enabled;
            return this;
        }

        /**
         * Tessellates a new application-owned geometry from the current configuration.
         *
         * @return new teapot geometry
         * @throws IllegalArgumentException if no section is included or requested arrays exceed
         *     Java array limits
         */
        public BufferGeometry build() {
            return TeapotGeometry.build(this);
        }

        /** Returns whether one canonical patch belongs to an enabled section. */
        private boolean includesPatch(int patch) {
            if (patch < BODY_PATCH_END) {
                return bodyIncluded;
            }
            if (patch < LID_PATCH_END) {
                return lidIncluded;
            }
            return bottomIncluded;
        }
    }
}
