/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Creates deterministic glTF and GLB files used through the public loader interface. */
final class GltfTestAssets {
    /** Prevents instantiation of this fixture utility. */
    private GltfTestAssets() {
        throw new AssertionError("GltfTestAssets cannot be instantiated");
    }

    /** Writes a feature-rich external-buffer glTF asset. */
    static Path writeTexturedTriangle(Path directory) throws IOException {
        ByteBuffer data = ByteBuffer.allocate(99).order(ByteOrder.LITTLE_ENDIAN);
        putFloats(data, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        putFloats(data, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        data.putShort((short) 0).putShort((short) 0);
        data.putShort((short) 65_535).putShort((short) 0);
        data.putShort((short) 32_768).putShort((short) 65_535);
        data.put(new byte[] {(byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 128, 0, 0, (byte) 255, (byte) 255});
        data.put(new byte[] {0, 1, 2});
        Files.write(directory.resolve("triangle.bin"), data.array());
        writePixel(directory.resolve("pixel.png"));
        String json = """
                {
                  "asset": {"version": "2.0"},
                  "scene": 1,
                  "scenes": [{}, {"nodes": [0]}],
                  "nodes": [
                    {
                      "mesh": 0,
                      "children": [1],
                      "translation": [1, 2, 3],
                      "rotation": [0, 0, 0, 1],
                      "scale": [2, 3, 4]
                    },
                    {"mesh": 0, "translation": [0, 1, 0]}
                  ],
                  "meshes": [{"primitives": [{
                    "attributes": {"POSITION": 0, "NORMAL": 1, "TEXCOORD_0": 2, "COLOR_0": 3},
                    "indices": 4,
                    "material": 0
                  }]}],
                  "materials": [{
                    "pbrMetallicRoughness": {
                      "baseColorFactor": [0.25, 0.5, 0.75, 0.6],
                      "baseColorTexture": {"index": 0},
                      "metallicFactor": 0.8,
                      "roughnessFactor": 0.3,
                      "metallicRoughnessTexture": {"index": 0}
                    },
                    "normalTexture": {"index": 0, "scale": 0.4},
                    "occlusionTexture": {"index": 0, "strength": 0.7},
                    "emissiveTexture": {"index": 0},
                    "emissiveFactor": [0.1, 0.2, 0.3],
                    "alphaMode": "BLEND",
                    "alphaCutoff": 0.35,
                    "doubleSided": true
                  }],
                  "textures": [{"sampler": 0, "source": 0}],
                  "samplers": [{"magFilter": 9728, "minFilter": 9985, "wrapS": 33648, "wrapT": 33071}],
                  "images": [{"uri": "pixel.png"}],
                  "buffers": [{"uri": "triangle.bin", "byteLength": 99}],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 36},
                    {"buffer": 0, "byteOffset": 36, "byteLength": 36},
                    {"buffer": 0, "byteOffset": 72, "byteLength": 12},
                    {"buffer": 0, "byteOffset": 84, "byteLength": 12},
                    {"buffer": 0, "byteOffset": 96, "byteLength": 3}
                  ],
                  "accessors": [
                    {"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 2, "componentType": 5123, "normalized": true, "count": 3, "type": "VEC2"},
                    {"bufferView": 3, "componentType": 5121, "normalized": true, "count": 3, "type": "VEC4"},
                    {"bufferView": 4, "componentType": 5121, "count": 3, "type": "SCALAR"}
                  ]
                }
                """;
        Path source = directory.resolve("triangle.gltf");
        Files.writeString(source, json);
        return source;
    }

    /** Writes a minimal binary GLB containing one non-indexed triangle. */
    static Path writeGlbTriangle(Path directory) throws IOException {
        ByteBuffer binary = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        putFloats(binary, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        String json = """
                {"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],
                "nodes":[{"mesh":0}],"meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                "buffers":[{"byteLength":36}],"bufferViews":[{"buffer":0,"byteLength":36}],
                "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]}
                """;
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int jsonLength = alignedLength(jsonBytes.length);
        int binaryLength = alignedLength(binary.capacity());
        int totalLength = 12 + 8 + jsonLength + 8 + binaryLength;
        ByteBuffer glb = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546C67).putInt(2).putInt(totalLength);
        glb.putInt(jsonLength).putInt(0x4E4F534A).put(jsonBytes);
        while (glb.position() < 20 + jsonLength) {
            glb.put((byte) 0x20);
        }
        glb.putInt(binaryLength).putInt(0x004E4942).put(binary.array());
        while (glb.hasRemaining()) {
            glb.put((byte) 0);
        }
        Path source = directory.resolve("triangle.glb");
        Files.write(source, glb.array());
        return source;
    }

    /** Writes one triangle node with linear, step, and cubic-spline transform channels. */
    static Path writeAnimatedTriangle(Path directory) throws IOException {
        ByteBuffer data = ByteBuffer.allocate(240).order(ByteOrder.LITTLE_ENDIAN);
        putFloats(data, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        putFloats(data, 0.0f, 1.0f, 2.0f);
        putFloats(data, 0.0f, 0.0f, 0.0f, 2.0f, 4.0f, 6.0f, 4.0f, 8.0f, 12.0f);
        putFloats(data, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        putFloats(
                data, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 3.0f, 3.0f, 3.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f);
        Files.write(directory.resolve("animated.bin"), data.array());
        String json = """
                {
                  "asset":{"version":"2.0"},
                  "scene":0,
                  "scenes":[{"nodes":[0]}],
                  "nodes":[{"mesh":0}],
                  "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
                  "animations":[{
                    "name":"Transform interpolation",
                    "samplers":[
                      {"input":1,"output":2,"interpolation":"LINEAR"},
                      {"input":1,"output":3,"interpolation":"STEP"},
                      {"input":1,"output":4,"interpolation":"CUBICSPLINE"}
                    ],
                    "channels":[
                      {"sampler":0,"target":{"node":0,"path":"translation"}},
                      {"sampler":1,"target":{"node":0,"path":"rotation"}},
                      {"sampler":2,"target":{"node":0,"path":"scale"}}
                    ]
                  }],
                  "buffers":[{"uri":"animated.bin","byteLength":240}],
                  "bufferViews":[
                    {"buffer":0,"byteOffset":0,"byteLength":36},
                    {"buffer":0,"byteOffset":36,"byteLength":12},
                    {"buffer":0,"byteOffset":48,"byteLength":36},
                    {"buffer":0,"byteOffset":84,"byteLength":48},
                    {"buffer":0,"byteOffset":132,"byteLength":108}
                  ],
                  "accessors":[
                    {"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"},
                    {"bufferView":1,"componentType":5126,"count":3,"type":"SCALAR","min":[0],"max":[2]},
                    {"bufferView":2,"componentType":5126,"count":3,"type":"VEC3"},
                    {"bufferView":3,"componentType":5126,"count":3,"type":"VEC4"},
                    {"bufferView":4,"componentType":5126,"count":9,"type":"VEC3"}
                  ]
                }
                """;
        return writeJson(directory, "animated.gltf", json);
    }

    /** Writes a one-primitive embedded-buffer triangle with caller-selected node and root features. */
    static Path writeSimpleTriangle(
            Path directory, String fileName, String node, String primitive, String additionalRootProperties)
            throws IOException {
        ByteBuffer binary = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        putFloats(binary, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        String encoded = Base64.getEncoder().encodeToString(binary.array());
        String json = """
                {"asset":{"version":"2.0"},"scene":0,"scenes":[{"nodes":[0]}],
                "nodes":[%s],"meshes":[{"primitives":[%s]}],
                "buffers":[{"uri":"data:application/octet-stream;base64,%s","byteLength":36}],
                "bufferViews":[{"buffer":0,"byteLength":36}],
                "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}]%s}
                """;
        json = String.format(Locale.ROOT, json, node, primitive, encoded, additionalRootProperties);
        return writeJson(directory, fileName, json);
    }

    /** Writes a triangle with a base-colour texture and default glTF sampler state. */
    static Path writeDefaultTextureTriangle(Path directory) throws IOException {
        writePixel(directory.resolve("default.png"));
        return writeSimpleTriangle(
                directory,
                "default-texture.gltf",
                "{\"mesh\":0}",
                "{\"attributes\":{\"POSITION\":0},\"material\":0}",
                ",\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}],"
                        + "\"textures\":[{\"source\":0}],\"images\":[{\"uri\":\"default.png\"}]");
    }

    /** Writes a syntactically valid asset from the supplied root fragments. */
    static Path writeJson(Path directory, String fileName, String body) throws IOException {
        Path source = directory.resolve(fileName);
        Files.writeString(source, body);
        return source;
    }

    /** Writes one opaque source pixel. */
    static void writePixel(Path destination) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF3366CC);
        if (!ImageIO.write(image, "png", destination.toFile())) {
            throw new IOException("PNG writer is unavailable");
        }
    }

    /** Writes floats in little-endian order. */
    private static void putFloats(ByteBuffer destination, float... values) {
        for (float value : values) {
            destination.putFloat(value);
        }
    }

    /** Returns a four-byte-aligned length. */
    private static int alignedLength(int length) {
        return (length + 3) & ~3;
    }
}
