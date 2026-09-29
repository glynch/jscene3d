# Rendering

[Rendering fundamentals](fundamentals.md) introduces the low-level API by
building a rotating cube. This reference continues from that foundation and
summarizes the current materials, lighting, scene effects, geometry helpers,
and selection facilities. Runnable demonstrations are indexed in
[Examples](examples.md).

## Materials

JScene3D's built-in mesh materials cover progressively richer shading:

| Material | Purpose |
| --- | --- |
| `BasicMaterial` | Unlit color, vertex color, and optional color map. |
| `LambertMaterial` | Diffuse lighting with optional vertex color and color map. |
| `PhongMaterial` | Diffuse lighting, Blinn-Phong highlights, and emissive color. |
| `StandardMaterial` | Metallic-roughness PBR with base-color, metallic-roughness, normal, occlusion, and emissive maps. |
| `NormalMaterial` | Unlit diagnostic display of transformed normals. |
| `ShaderMaterial` | Application-supplied GLSL with typed uniforms and declared inputs. |

Lit materials require geometry normals. They do not receive a hidden default
light, so a non-emissive lit surface is black when no supported light is
visible. Materials and textures are mutable, shareable resources and are not
owned by a `Mesh` that references them.

## Transparency and depth

Opacity and alpha mode are separate. Choose `AlphaMode.BLEND` for blended
surfaces or `AlphaMode.MASK` plus an alpha cutoff for binary cutouts.
`Material.setTransparent(true)` is a convenience alias for blend mode.

The renderer sorts transparent objects back-to-front using their object origins
in camera space and scene traversal as a stable tie-breaker. This object-level
sort cannot resolve intersecting transparent objects or overlapping triangles
inside one mesh. Blended effects commonly disable depth writes with
`setDepthWriteEnabled(false)`, but JScene3D leaves that choice explicit.

`Object3D.setRenderOrder` orders objects within the opaque or transparent list;
it does not change alpha classification or bypass depth testing. Materials
default to `DepthFunction.LESS_OR_EQUAL`.

## Custom shaders

`ShaderMaterial` accepts immutable OpenGL 3.3 Core vertex and fragment source,
optional preprocessor definitions, and declared standard attribute
requirements. The renderer supplies active reserved transform uniforms:

```text
modelMatrix
viewMatrix
projectionMatrix
modelViewMatrix
normalMatrix
```

Application values use typed uniform setters. Scalars, booleans, vectors,
matrices, `Color`, and two-dimensional `Texture` values are supported. Vector
and matrix values are copied; textures remain application-owned.

A shader can opt into renderer-managed instance transforms and instance color,
and can declare up to four custom floating-point per-instance inputs. Uniform
arrays, arbitrary custom per-vertex attributes, uniform blocks, and raw OpenGL
access are not part of the current public interface. Custom shaders also own
their fog and texture-transform logic rather than inheriting built-in shader
behavior automatically.

## Lighting

Scenes can contain `AmbientLight`, `PointLight`, `DirectionalLight`,
`SpotLight`, and `HemisphereLight` nodes. Point and spot lights expose distance
and decay controls; spotlights additionally expose cone angle and penumbra.
Directional and spot lights illuminate toward a copied world-space target.
Hemisphere lights blend sky and ground colors according to surface direction.

The current renderer accepts at most eight visible lights of each point,
directional, spot, and hemisphere kind. These limits are exposed as
`Renderer.MAX_POINT_LIGHTS`, `MAX_DIRECTIONAL_LIGHTS`, `MAX_SPOT_LIGHTS`, and
`MAX_HEMISPHERE_LIGHTS`; exceeding them fails rather than silently dropping
lights.

## Shadows

Directional, spot, and point lights can cast shadows. Shadow mapping is
explicit at every boundary:

- enable shadow casting on the light;
- enable casting on each contributing mesh;
- enable receiving on each receiving mesh.

Directional lights expose orthographic shadow-camera bounds. All
shadow-capable lights expose map size, camera range, depth bias, and normal
bias. `LambertMaterial`, `PhongMaterial`, and `StandardMaterial` receive the
filtered results.

One draw supports at most four combined directional/spot shadow maps and four
point-light shadow maps, exposed by
`Renderer.MAX_TWO_DIMENSIONAL_SHADOW_MAPS` and
`Renderer.MAX_POINT_SHADOW_MAPS`.

## Fog

A `Scene` can use `LinearFog`, with explicit near and far distances, or
`ExponentialSquaredFog`, with continuously increasing density. Built-in mesh
and line materials apply scene fog. `ShaderMaterial` programs must implement
their own fog calculations.

## Environment lighting and tone mapping

`EnvironmentMapLoader` synchronously decodes a Radiance HDR equirectangular
image into an application-owned `EnvironmentMap`. `Scene.setEnvironment`
selects image-based lighting for `StandardMaterial`; the background is selected
independently with `setBackgroundEnvironment`. Scene intensity and rotation
apply to all environment-lit materials, and each `StandardMaterial` has an
additional environment-intensity multiplier.

The renderer derives and caches irradiance, prefiltered reflection levels, and
the BRDF lookup needed by the metallic-roughness path. Tone mapping is explicit:

```java
renderer.setToneMapping(ToneMapping.ACES_FILMIC);
renderer.setExposure(1.0f);
```

The default tone-mapping mode is `NONE`. Keep an environment map open for as
long as any rendered scene refers to it, then close it with the other
application-owned resources.

## Lines, helpers, and generated geometry

`Line` draws a connected strip and `LineSegments` draws independent pairs.
Both use `BufferGeometry` with `LineBasicMaterial` and support transforms,
visibility, frustum culling, vertex colors, draw ranges, transparency, and
depth state. Portable line width is fixed at one framebuffer pixel. An odd
`LineSegments` element count is rejected because it cannot form complete
pairs.

`AxesHelper`, `GridHelper`, and `BoxHelper` are ordinary `LineSegments`
objects. They own their generated geometry and material, so close the helper
itself. `BoxHelper.update()` refreshes world-axis-aligned bounds after its
target hierarchy changes.

The core module includes generated box, plane, sphere, ring, circle, cylinder,
cone, torus, teapot, and wireframe geometry facilities. Configurable factories
expose the tessellation and shape-specific options appropriate to each type.

## Raycasting

`Raycaster` creates a normalized world-space ray directly or from normalized
device coordinates through a perspective or orthographic camera. It traverses
visible scene subtrees and returns immutable `RaycastHit` values nearest first.

Mesh intersections account for hierarchy transforms, material visibility and
side selection, indexed or non-indexed draw ranges, morph influences,
instanced transforms, and optional texture coordinates. Mesh bounds provide a
broad phase before exact triangle tests. Lines and billboards are not included
in the current mesh raycast path; line picking requires an application-defined
distance tolerance.

## Resource ownership

Geometry, materials, textures, environment maps, helpers, and renderer objects
that implement `AutoCloseable` retain owned CPU or native resources until
closed. A `Mesh` retains but does not close its shared geometry or material.
Make ownership explicit, close dependants before shared resources, and use
try-with-resources where the lifetime is lexical.
