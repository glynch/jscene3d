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
                        "keyframe-animation",
                        "Keyframe animation",
                        "Animation",
                        "Step, linear, and cubic-spline transform tracks with live playback controls.",
                        List.of("animation", "keyframe", "step", "linear", "cubic spline", "mixer"),
                        KeyframeAnimationExample::create),
                definition(
                        "animation-blending",
                        "Animation blending",
                        "Animation",
                        "Weighted cross-fades between idle, walking, and running skeletal clips.",
                        List.of("animation", "blending", "cross-fade", "skinning", "gltf", "fox"),
                        List.of(
                                new GalleryAttribution(
                                        "Fox glTF model",
                                        "PixelMannen / tomkranis / Asobo Studio / scurest",
                                        "Khronos glTF Sample Assets — Fox",
                                        URI.create(
                                                "https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/Fox"),
                                        "CC0-1.0 and CC-BY-4.0",
                                        URI.create("https://creativecommons.org/licenses/by/4.0/")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        AnimationBlendingExample::create),
                definition(
                        "soldier-animation-blending",
                        "Soldier animation blending",
                        "Animation",
                        "Three.js-style skeletal blending with activation, stepping, cross-fades, and live weights.",
                        List.of("animation", "blending", "cross-fade", "skinning", "gltf", "soldier", "mixamo"),
                        List.of(new GalleryAttribution(
                                "Soldier (Vanguard) model and animations",
                                "T. Choonyung / Mixamo",
                                "Three.js — Skeletal Animation Blending",
                                URI.create("https://threejs.org/examples/webgl_animation_skinning_blending.html"),
                                "Mixamo Content Terms",
                                URI.create("https://www.adobe.com/legal/terms.html"))),
                        SoldierAnimationBlendingExample::create),
                definition(
                        "gltf-animation",
                        "glTF animation",
                        "Animation",
                        "Imported glTF transform clips comparing all three interpolation modes.",
                        List.of("animation", "gltf", "keyframe", "step", "linear", "cubic spline"),
                        List.of(new GalleryAttribution(
                                "Interpolation Test glTF model",
                                "Khronos Group",
                                "Khronos glTF Sample Assets — Interpolation Test",
                                URI.create(
                                        "https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/InterpolationTest"),
                                "CC0-1.0",
                                URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        GltfAnimationExample::create),
                definition(
                        "skeletal-animation",
                        "Skeletal animation",
                        "Animation",
                        "GPU skinning driven by a two-bone Java scene hierarchy and keyframe clip.",
                        List.of("animation", "skinning", "skeleton", "bones", "gpu"),
                        SkeletalAnimationExample::create),
                definition(
                        "generated-geometries",
                        "Generated geometries",
                        "Geometry",
                        "Circle, cylinder, cone, and torus geometry in one lit scene.",
                        List.of("circle", "cylinder", "cone", "torus"),
                        GeneratedGeometriesExample::create),
                definition(
                        "utah-teapot",
                        "Utah teapot",
                        "Geometry",
                        "Interactive bicubic-patch tessellation with six rendering presentations.",
                        List.of("teapot", "bezier", "patch", "tessellation", "wireframe", "pbr"),
                        List.of(
                                new GalleryAttribution(
                                        "Utah Teapot geometry data",
                                        "Martin Newell / Three.js Authors",
                                        "Three.js TeapotGeometry",
                                        URI.create(
                                                "https://github.com/mrdoob/three.js/blob/dev/examples/jsm/geometries/TeapotGeometry.js"),
                                        "MIT",
                                        URI.create("https://github.com/mrdoob/three.js/blob/dev/LICENSE")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        TeapotExample::create),
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
                        "shadows",
                        "Shadow mapping",
                        "Lighting",
                        "Directional, spot, and point shadows with live map controls.",
                        List.of("shadow", "directional", "spot", "point", "bias", "pcf"),
                        ShadowsExample::create),
                definition(
                        "spot-hemisphere-lights",
                        "Spot and hemisphere lights",
                        "Lighting",
                        "Editable spotlight cones and sky-to-ground illumination.",
                        List.of("spot", "hemisphere", "phong", "gui"),
                        SpotAndHemisphereLightsExample::create),
                definition(
                        "fog",
                        "Distance fog",
                        "Scenes",
                        "Linear and exponential-squared fog across a deterministic field of meshes and lines.",
                        List.of("fog", "linear", "exponential", "distance", "atmosphere"),
                        List.of(new GalleryAttribution(
                                "Fog example design",
                                "Three.js Authors",
                                "Three.js Fog Manual and OrbitControls Example",
                                URI.create("https://threejs.org/manual/en/fog.html"),
                                "MIT",
                                URI.create("https://github.com/mrdoob/three.js/blob/dev/LICENSE"))),
                        FogExample::create),
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
                        "render-callbacks",
                        "Render callbacks",
                        "Rendering",
                        "Per-draw customization of one material shared by three animated meshes.",
                        List.of("callback", "before render", "after render", "material", "lifecycle"),
                        RenderCallbacksExample::create),
                definition(
                        "solar-system",
                        "Solar System Viewer",
                        "Showcases",
                        "A complete textured, lit, animated, and controllable scene.",
                        List.of("planets", "textures", "orbit", "showcase"),
                        SolarSystemViewer::create),
                definition(
                        "littlest-tokyo",
                        "Littlest Tokyo",
                        "Showcases",
                        "A Draco-compressed glTF city animated through a 32-joint imported skeleton.",
                        List.of("animation", "gltf", "draco", "skinning", "pbr", "showcase"),
                        List.of(
                                new GalleryAttribution(
                                        "Littlest Tokyo",
                                        "Glen Fox (glenatron)",
                                        "ArtStation — Littlest Tokyo",
                                        URI.create("https://glenatron.artstation.com/projects/AJGbV"),
                                        "CC BY 4.0",
                                        URI.create("https://creativecommons.org/licenses/by/4.0/")),
                                new GalleryAttribution(
                                        "Studio Small 08 HDRI",
                                        "Sergej Majboroda",
                                        "Poly Haven — polyhaven.com/a/studio_small_08",
                                        URI.create("https://polyhaven.com/a/studio_small_08"),
                                        "CC0-1.0",
                                        URI.create("https://creativecommons.org/publicdomain/zero/1.0/"))),
                        LittlestTokyoExample::create));
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
