/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleFactory;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.List;

/** Declares the stable ordered catalogue rendered by {@link ExampleBrowser}. */
final class ExampleCatalog {
    /** Prevents instantiation of this static catalogue. */
    private ExampleCatalog() {
        throw new AssertionError("ExampleCatalog cannot be instantiated");
    }

    /** Returns all live examples in their display order. */
    static List<ExampleDefinition> definitions() {
        return List.of(
                definition(
                        "basic-triangle",
                        "Basic triangle",
                        "Fundamentals",
                        "Vertex colours, animation, and double-sided rendering.",
                        List.of("triangle", "vertex colors", "animation"),
                        0x00b8ff,
                        BasicTriangleExample::create),
                definition(
                        "textured-cube",
                        "Textured cube",
                        "Fundamentals",
                        "A generated colour texture mapped onto an animated box.",
                        List.of("texture", "cube", "uv", "animation"),
                        0xa855f7,
                        TexturedCubeExample::create),
                definition(
                        "generated-geometries",
                        "Generated geometries",
                        "Geometry",
                        "Circle, cylinder, cone, and torus geometry in one lit scene.",
                        List.of("circle", "cylinder", "cone", "torus"),
                        0xff4bd8,
                        GeneratedGeometriesExample::create),
                definition(
                        "line-rendering",
                        "Line rendering",
                        "Objects",
                        "Connected lines, independent segments, and vertex colours.",
                        List.of("line", "segments", "orbit", "axes"),
                        0x00e5ff,
                        LineRenderingExample::create),
                definition(
                        "helpers",
                        "Axes and grid helpers",
                        "Helpers",
                        "Reference axes, a world grid, depth bias, and render ordering.",
                        List.of("axes", "grid", "debug"),
                        0x42d66b,
                        HelpersExample::create),
                definition(
                        "box-helper",
                        "Box helper",
                        "Helpers",
                        "Dynamic world bounds around a rotating object hierarchy.",
                        List.of("bounds", "box", "hierarchy", "debug"),
                        0xffdc4d,
                        BoxHelperExample::create),
                definition(
                        "lambert-lighting",
                        "Lambert lighting",
                        "Lighting",
                        "Ambient, directional, and animated point lights.",
                        List.of("ambient", "directional", "point", "lambert"),
                        0xffa34d,
                        LightingExample::create),
                definition(
                        "spot-hemisphere-lights",
                        "Spot and hemisphere lights",
                        "Lighting",
                        "Editable spotlight cones and sky-to-ground illumination.",
                        List.of("spot", "hemisphere", "phong", "gui"),
                        0xff7a45,
                        SpotAndHemisphereLightsExample::create),
                definition(
                        "materials",
                        "Mesh materials",
                        "Materials",
                        "Basic, Lambert, normal, and Phong materials with live controls.",
                        List.of("basic", "lambert", "normal", "phong", "gui"),
                        0xffc928,
                        MaterialsExample::create),
                definition(
                        "orbit-controls",
                        "Orbit controls",
                        "Interaction",
                        "Orbit, pan, dolly, damping, auto-rotation, and live settings.",
                        List.of("camera", "orbit", "pan", "zoom", "gui"),
                        0x20d8c4,
                        OrbitControlsExample::create),
                definition(
                        "object-selection",
                        "Object selection",
                        "Interaction",
                        "Nearest-first pointer raycasting and selection feedback.",
                        List.of("raycaster", "picking", "selection", "mouse"),
                        0x57d8ff,
                        ObjectSelectionExample::create),
                definition(
                        "shader-material",
                        "Shader material",
                        "Materials",
                        "Typed uniforms, shader defines, animation, and automatic transforms.",
                        List.of("glsl", "shader", "uniform", "custom"),
                        0x4f8cff,
                        ShaderMaterialExample::create),
                definition(
                        "texture-transforms",
                        "Texture transforms",
                        "Textures",
                        "Interactive offset, repeat, rotation, centre, and wrapping.",
                        List.of("texture", "repeat", "offset", "rotation", "wrap"),
                        0xff5f73,
                        TextureTransformsExample::create),
                definition(
                        "transparency",
                        "Transparency",
                        "Rendering",
                        "Back-to-front transparent sorting and depth-write control.",
                        List.of("alpha", "blend", "sorting", "depth"),
                        0x638cff,
                        TransparencyExample::create),
                definition(
                        "solar-system",
                        "Solar System Viewer",
                        "Showcases",
                        "A complete textured, lit, animated, and controllable scene.",
                        List.of("planets", "textures", "orbit", "showcase"),
                        0xffb33b,
                        SolarSystemViewer::create));
    }

    /** Creates one definition and its deterministic full-colour thumbnail. */
    private static ExampleDefinition definition(
            String id,
            String title,
            String category,
            String description,
            List<String> tags,
            int accentRgb,
            ExampleFactory factory) {
        return new ExampleDefinition(id, title, category, description, tags, thumbnail(id, accentRgb), factory);
    }

    /** Loads a captured classpath thumbnail with generated artwork as a missing-resource fallback. */
    private static OverlayImage thumbnail(String id, int accentRgb) {
        String resourceName = "/io/github/glynch/jscene3d/examples/thumbnails/" + id + ".png";
        if (ExampleCatalog.class.getResource(resourceName) == null) {
            return ExampleThumbnailFactory.create(id, accentRgb);
        }
        return OverlayImageLoader.loadResource(ExampleCatalog.class, resourceName);
    }
}
