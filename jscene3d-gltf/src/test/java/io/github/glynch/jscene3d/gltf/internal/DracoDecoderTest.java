/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import dev.fileformat.drako.AttributeType;
import dev.fileformat.drako.Draco;
import dev.fileformat.drako.DracoMesh;
import dev.fileformat.drako.DrakoException;
import dev.fileformat.drako.PointAttribute;
import dev.fileformat.drako.Vector3;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class DracoDecoderTest {
    @Test
    void decodesRequestedAttributesAndTriangleIndices() throws DrakoException {
        byte[] encoded = encodedTriangle();

        DracoDecoder.DecodedPrimitive decoded = DracoDecoder.decode(
                ByteBuffer.wrap(encoded), Map.of("POSITION", 0), Map.of("POSITION", 3), Map.of("POSITION", new float[] {
                    -1.0f, -1.0f, 0.0f
                }));

        assertThat(decoded.vertexCount()).isEqualTo(3);
        assertThat(decoded.indices()).containsExactly(0, 1, 2);
        float[] positions = Objects.requireNonNull(decoded.attributes().get("POSITION"), "position");
        assertThat(positions).hasSize(9);
        assertThat(positions[0]).isEqualTo(-1.0f);
        assertThat(positions[3]).isEqualTo(1.0f);
        assertThat(positions[7]).isEqualTo(1.0f);
    }

    @Test
    void returnsDefensiveCopies() throws DrakoException {
        DracoDecoder.DecodedPrimitive decoded = DracoDecoder.decode(
                ByteBuffer.wrap(encodedTriangle()),
                Map.of("POSITION", 0),
                Map.of("POSITION", 3),
                Map.of("POSITION", new float[] {-1.0f, -1.0f, 0.0f}));

        decoded.indices()[0] = 2;
        Objects.requireNonNull(decoded.attributes().get("POSITION"), "position")[0] = 9.0f;

        assertThat(decoded.indices()[0]).isZero();
        assertThat(Objects.requireNonNull(decoded.attributes().get("POSITION"), "position")[0])
                .isEqualTo(-1.0f);
    }

    @Test
    void restoresEveryComponentToItsDeclaredMinimum() throws DrakoException {
        DracoDecoder.DecodedPrimitive decoded = DracoDecoder.decode(
                ByteBuffer.wrap(encodedTriangle()),
                Map.of("POSITION", 0),
                Map.of("POSITION", 3),
                Map.of("POSITION", new float[] {-3.0f, -4.0f, 2.0f}));

        assertThat(Objects.requireNonNull(decoded.attributes().get("POSITION"), "position"))
                .containsExactly(
                        new float[] {-3.0f, -4.0f, 2.0f, -1.0f, -4.0f, 2.0f, -2.0f, -2.0f, 2.0f}, within(0.001f));
    }

    @Test
    void comparesPackedArrayContentRatherThanArrayIdentity() {
        DracoDecoder.DecodedPrimitive first =
                new DracoDecoder.DecodedPrimitive(Map.of("POSITION", new float[] {1.0f, 2.0f}), new int[] {0, 1}, 2);
        DracoDecoder.DecodedPrimitive second =
                new DracoDecoder.DecodedPrimitive(Map.of("POSITION", new float[] {1.0f, 2.0f}), new int[] {0, 1}, 2);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("POSITION", "indexCount=2", "vertexCount=2");
    }

    @Test
    void rejectsInvalidPayloadAndDescriptorMismatches() throws DrakoException {
        byte[] encoded = encodedTriangle();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> DracoDecoder.decode(
                        ByteBuffer.wrap(new byte[] {1, 2, 3}), Map.of("POSITION", 0), Map.of("POSITION", 3), Map.of()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DracoDecoder.decode(
                        ByteBuffer.wrap(encoded), Map.of("POSITION", 8), Map.of("POSITION", 3), Map.of()))
                .withMessageContaining("unique id 8");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DracoDecoder.decode(
                        ByteBuffer.wrap(encoded), Map.of("POSITION", 0), Map.of("POSITION", 2), Map.of()))
                .withMessageContaining("component count");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DracoDecoder.decode(
                        ByteBuffer.wrap(encoded),
                        Map.of("POSITION", 0),
                        Map.of("POSITION", 3),
                        Map.of("POSITION", new float[] {0.0f, 0.0f})))
                .withMessageContaining("minimum component count");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DracoDecoder.DecodedPrimitive(Map.of(), new int[0], -1));
    }

    private static byte[] encodedTriangle() throws DrakoException {
        DracoMesh mesh = new DracoMesh();
        mesh.setNumPoints(3);
        PointAttribute position = PointAttribute.wrap(AttributeType.POSITION, new Vector3[] {
            new Vector3(-1.0f, -1.0f, 0.0f), new Vector3(1.0f, -1.0f, 0.0f), new Vector3(0.0f, 1.0f, 0.0f)
        });
        position.setUniqueId((short) 7);
        mesh.addAttribute(position);
        mesh.addFace(new int[] {0, 1, 2});
        return Draco.encode(mesh);
    }
}
