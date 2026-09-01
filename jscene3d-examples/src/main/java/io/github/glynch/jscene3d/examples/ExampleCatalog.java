/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleFactory;
import io.github.glynch.jscene3d.gui.GalleryAttribution;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.net.URI;
import java.util.List;

/** Declares the stable ordered catalogue rendered by {@link ExampleBrowser}. */
final class ExampleCatalog {
    /** Prevents instantiation of this static catalogue. */
    private ExampleCatalog() {
        throw new AssertionError("ExampleCatalog cannot be instantiated");
    }

    /** Returns all browser definitions after loading every required captured thumbnail. */
    static List<ExampleDefinition> definitions() {
        return entries().stream().map(ExampleCatalog::definition).toList();
    }

    /** Returns all thumbnail-independent catalogue entries in display order. */
    static List<ExampleCatalogEntry> entries() {
        return List.of(
                definition(
                        "basic-triangle",
                        "Basic triangle",
                        "Fundamentals",
                        "Vertex colours, animation, and double-sided rendering.",
                        List.of("triangle", "vertex colors", "animation"),
                        BasicTriangleExample::create),
                definition(
                        "textured-cube",
                        "Textured cube",
                        "Fundamentals",
                        "A generated colour texture mapped onto an animated box.",
                        List.of("texture", "cube", "uv", "animation"),
                        TexturedCubeExample::create),
                definition(
                        "generated-geometries",
                        "Generated geometries",
                        "Geometry",
                        "Circle, cylinder, cone, and torus geometry in one lit scene.",
                        List.of("circle", "cylinder", "cone", "torus"),
                        GeneratedGeometriesExample::create),
                definition(
                        "line-rendering",
                        "Line rendering",
                        "Objects",
                        "Connected lines, independent segments, and vertex colours.",
                        List.of("line", "segments", "orbit", "axes"),
                        LineRenderingExample::create),
                definition(
                        "helpers",
                        "Axes and grid helpers",
                        "Helpers",
                        "Reference axes, a world grid, depth bias, and render ordering.",
                        List.of("axes", "grid", "debug"),
                        HelpersExample::create),
                definition(
                        "box-helper",
                        "Box helper",
                        "Helpers",
                        "Dynamic world bounds around a rotating object hierarchy.",
                        List.of("bounds", "box", "hierarchy", "debug"),
                        BoxHelperExample::create),
                definition(
                        "lambert-lighting",
                        "Lambert lighting",
                        "Lighting",
                        "Ambient, directional, and animated point lights.",
                        List.of("ambient", "directional", "point", "lambert"),
                        LightingExample::create),
                definition(
                        "spot-hemisphere-lights",
                        "Spot and hemisphere lights",
                        "Lighting",
                        "Editable spotlight cones and sky-to-ground illumination.",
                        List.of("spot", "hemisphere", "phong", "gui"),
                        SpotAndHemisphereLightsExample::create),
                definition(
                        "materials",
                        "Mesh materials",
                        "Materials",
                        "Basic, Lambert, normal, and Phong materials with live controls.",
                        List.of("basic", "lambert", "normal", "phong", "gui"),
                        MaterialsExample::create),
                definition(
                        "standard-material",
                        "Standard material",
                        "Materials",
                        "Metallic-roughness PBR across a grid of material values.",
                        List.of("pbr", "metalness", "roughness", "standard"),
                        StandardMaterialExample::create),
                definition(
                        "environment-lighting",
                        "Environment lighting",
                        "Lighting",
                        "HDR image-based lighting across metallic and rough surfaces.",
                        List.of("pbr", "ibl", "hdr", "environment", "metalness", "roughness"),
                        List.of(new GalleryAttribution(
                                "Studio Small 08 HDRI",
                                "Sergej Majboroda",
                                "Poly Haven — polyhaven.com/a/studio_small_08",
                                URI.create("https://polyhaven.com/a/studio_small_08"),
                                "CC0-1.0",
                                URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        EnvironmentLightingExample::create),
                definition(
                        "avocado-model",
                        "Avocado glTF model",
                        "Loading",
                        "A realistic CC0 glTF asset rendered with HDR image-based lighting.",
                        List.of("gltf", "glb", "pbr", "ibl", "environment", "realistic"),
                        List.of(
                                new GalleryAttribution(
                                        "Avocado glTF model",
                                        "Microsoft",
                                        "Khronos glTF Sample Assets — Avocado",
                                        URI.create(
                                                "https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/Avocado"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        AvocadoModelExample::create),
                definition(
                        "water-bottle-model",
                        "Water Bottle glTF model",
                        "Loading",
                        "A CC0 PBR asset exercising normal, occlusion, and emissive maps.",
                        List.of("gltf", "glb", "pbr", "normal map", "occlusion", "emissive", "ibl"),
                        List.of(
                                new GalleryAttribution(
                                        "Water Bottle glTF model",
                                        "Microsoft",
                                        "Khronos glTF Sample Assets — Water Bottle",
                                        URI.create(
                                                "https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/WaterBottle"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        WaterBottleModelExample::create),
                definition(
                        "boom-box-model",
                        "Boom Box glTF model",
                        "Loading",
                        "A CC0 portable radio with metallic surfaces and an emissive front panel.",
                        List.of("gltf", "glb", "pbr", "emissive", "metallic", "ibl"),
                        List.of(
                                new GalleryAttribution(
                                        "Boom Box glTF model",
                                        "Microsoft",
                                        "Khronos glTF Sample Assets — Boom Box",
                                        URI.create(
                                                "https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/BoomBox"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        BoomBoxModelExample::create),
                definition(
                        "gltf-loading",
                        "glTF loading",
                        "Loading",
                        "A bundled glTF 2.0 scene loaded through the optional glTF artifact.",
                        List.of("gltf", "glb", "loader", "pbr"),
                        GltfLoadingExample::create),
                definition(
                        "orbit-controls",
                        "Orbit controls",
                        "Interaction",
                        "Orbit, pan, dolly, damping, auto-rotation, and live settings.",
                        List.of("camera", "orbit", "pan", "zoom", "gui"),
                        OrbitControlsExample::create),
                definition(
                        "object-selection",
                        "Object selection",
                        "Interaction",
                        "Nearest-first pointer raycasting and selection feedback.",
                        List.of("raycaster", "picking", "selection", "mouse"),
                        ObjectSelectionExample::create),
                definition(
                        "shader-material",
                        "Shader material",
                        "Materials",
                        "Typed uniforms, shader defines, animation, and automatic transforms.",
                        List.of("glsl", "shader", "uniform", "custom"),
                        ShaderMaterialExample::create),
                definition(
                        "texture-transforms",
                        "Texture transforms",
                        "Textures",
                        "Interactive offset, repeat, rotation, centre, and wrapping.",
                        List.of("texture", "repeat", "offset", "rotation", "wrap"),
                        TextureTransformsExample::create),
                definition(
                        "transparency",
                        "Transparency",
                        "Rendering",
                        "Back-to-front transparent sorting and depth-write control.",
                        List.of("alpha", "blend", "sorting", "depth"),
                        TransparencyExample::create),
                definition(
                        "solar-system",
                        "Solar System Viewer",
                        "Showcases",
                        "A complete textured, lit, animated, and controllable scene.",
                        List.of("planets", "textures", "orbit", "showcase"),
                        SolarSystemViewer::create));
    }

    /** Creates one definition and loads its required captured thumbnail. */
    private static ExampleCatalogEntry definition(
            String id, String title, String category, String description, List<String> tags, ExampleFactory factory) {
        return definition(id, title, category, description, tags, List.of(), factory);
    }

    /** Creates one definition with explicit third-party asset provenance. */
    private static ExampleCatalogEntry definition(
            String id,
            String title,
            String category,
            String description,
            List<String> tags,
            List<GalleryAttribution> attributions,
            ExampleFactory factory) {
        return new ExampleCatalogEntry(id, title, category, description, tags, attributions, factory);
    }

    /** Loads the captured thumbnail required to promote one entry into a browser definition. */
    private static ExampleDefinition definition(ExampleCatalogEntry entry) {
        return new ExampleDefinition(
                entry.id(),
                entry.title(),
                entry.category(),
                entry.description(),
                entry.tags(),
                thumbnail(entry.id()),
                entry.attributions(),
                entry.factory());
    }

    /** Loads the captured classpath thumbnail, failing if it is absent or invalid. */
    private static OverlayImage thumbnail(String id) {
        String resourceName = "/META-INF/jscene3d/examples/thumbnails/" + id + ".png";
        return OverlayImageLoader.loadResource(ExampleCatalog.class, resourceName);
    }
}
