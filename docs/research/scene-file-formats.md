# Scene and 3D Asset File Formats for JScene3D

Research date: 2026-08-30

## Question and scope

Which disk formats should JScene3D load in its first loading milestone, and
which should remain longer-term compatibility targets?

This note distinguishes two different product needs that are easy to conflate:

1. **Asset interchange** imports content made elsewhere into a JScene3D scene.
2. **JScene3D persistence** saves and restores JScene3D-specific application
   state without losing library-specific behavior.

glTF, OBJ, STL, PLY, FBX, COLLADA, and USD are interchange formats. None is
automatically a lossless persistence format for every future JScene3D object,
custom shader, renderer option, or application-defined behavior.

## Executive recommendation

The first loading milestone should support **glTF 2.0 in both `.gltf` and `.glb`
forms**, with a precisely documented feature profile. Khronos defines glTF as a
runtime delivery format for complete scenes, and Three.js itself recommends it
where possible because it is compact, fast to load, and covers meshes,
materials, textures, skins, morph targets, animations, lights, and cameras.
([Khronos glTF 2.0 specification](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html),
[Three.js loading guide](https://threejs.org/manual/en/loading-3d-models.html))

"First loading milestone" does not necessarily mean JScene3D 0.1. A useful
glTF scene loader depends on texture loading and on material behavior compatible
with glTF's core metallic-roughness PBR model. If those features are post-0.1,
the honest choices are to schedule glTF after them or explicitly market an
earlier importer as geometry-only and visually incomplete.

Recommended sequence:

1. **First loader:** glTF 2.0 and GLB, initially limited to features JScene3D can
   render correctly.
2. **Second tier:** OBJ with MTL for common legacy static assets.
3. **Demand-driven geometry importers:** STL for CAD/3D-print meshes and PLY for
   scanned meshes, point clouds, and vertex-color data.
4. **Conversion-first legacy formats:** ask users to convert FBX and COLLADA to
   glTF. Add an optional Assimp-backed compatibility artifact only if real usage
   justifies native dependencies and partial-fidelity behavior.
5. **Long-term specialist integration:** USD/USDZ belongs in a separate optional
   integration, not the core loader, because USD is a scene-composition system
   substantially deeper than a model-file parser.
6. **Native JScene3D persistence:** defer a versioned JScene3D scene format until
   the public scene/material/resource model is stable enough to preserve without
   repeatedly breaking saved files.

This order follows product fit rather than parser size. STL is easier to parse
than glTF, but it does not satisfy the stated requirement to load useful scenes.

## Verified format capabilities

The following table summarizes what the formats can represent in principle.
Individual exporters and loaders often support only subsets.

| Format | Scene graph | Materials and textures | Animation and skinning | Cameras and lights | Main character |
| --- | --- | --- | --- | --- | --- |
| glTF 2.0 / GLB | Yes; multiple scenes, nodes, transforms, hierarchy | Yes; core metallic-roughness PBR, images, textures, samplers | Yes; keyframe animation, morph targets, skins | Core cameras; punctual lights through `KHR_lights_punctual` | Runtime delivery |
| OBJ + MTL | Object/group partitioning, but no parenting or transforms | Basic companion MTL materials and texture references | No native skeletal or clip animation | No | Legacy static geometry interchange |
| STL | No; geometry result only | No standard material/texture model; some binary color conventions exist | No | No | Triangle surfaces for CAD/printing |
| PLY | Exactly one object; no hierarchy or transforms | Extensible vertex/face properties, including colors and UVs, but no texture-description language | No | No | Dense static meshes and point/scan data |
| FBX | Yes | Yes | Yes, including skeletons, animation curves and deformation data | Yes | Broad DCC interchange, proprietary ecosystem |
| COLLADA | Yes | Yes, including effects and textures | Yes; animation, skinning, morphing | Yes | Broad XML authoring/interchange schema |
| USD / USDZ | Yes; composed hierarchical stage with layers, references, variants, and instancing | Yes; shading/material schemas and packaged texture assets | Yes; animation, linear-blend skinning and blend shapes | Yes | Large-scale scene description and composition |

### glTF 2.0 and GLB

The Khronos specification defines a glTF asset as JSON scene description plus
binary buffers and images. The scene description includes hierarchy, materials,
cameras, meshes, animations, and other constructs. Resources may remain external
or be embedded; GLB packages JSON and binary data in one binary blob. glTF is
explicitly designed for runtime efficiency, not as an authoring format.
([glTF basics and goals](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#introduction),
[GLB container](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#glb-file-format-specification))

Core glTF 2.0 covers node hierarchy, meshes and primitives, indexed and
non-indexed attributes, morph targets, skins, images, textures, samplers,
metallic-roughness materials, animations, and perspective or orthographic
cameras. Punctual lights are standardized separately by the Khronos
`KHR_lights_punctual` extension.
([glTF 2.0 specification](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html),
[`KHR_lights_punctual`](https://registry.khronos.org/glTF/extensions/2.0/Khronos/KHR_lights_punctual.html))

Three.js's `GLTFLoader` returns a default scene, all scenes, animations, and
cameras, and supports a substantial but explicit list of extensions. Its
extension list illustrates an important compatibility rule for JScene3D:
support for "glTF 2.0" must still say which optional and required extensions are
implemented.
([Three.js `GLTFLoader`](https://threejs.org/docs/pages/GLTFLoader.html))

**Java implications:** the container and schema are openly specified and can be
implemented in pure Java, keeping import independent from the rendering backend.
The MIT-licensed JglTF project provides generated glTF 1.0/2.0 schema classes and
a Java-oriented model layer, but warns that parts of its viewer's glTF 2.0
rendering support remain incomplete. JScene3D could reuse only its data/model
layers after a dependency and maintenance review, or implement the required
subset directly.
([JglTF repository](https://github.com/javagl/JglTF))

The hard work is not JSON parsing. It is correct mapping of accessors, sparse
data, primitive modes, color spaces, samplers, PBR semantics, resource paths,
coordinate conventions, extension rules, and object/resource ownership. This is
an engineering inference from the normative structures in the glTF
specification, not a quoted specification claim.

### OBJ and MTL

OBJ is a human-readable static geometry format containing vertex positions,
texture coordinates, normals, polygon faces, and object/group divisions.
Three.js returns a group from `OBJLoader` and can attach materials produced by
the separate `MTLLoader`; MTL describes surface shading properties for one or
more OBJ files.
([Three.js `OBJLoader`](https://threejs.org/docs/pages/OBJLoader.html),
[Three.js `MTLLoader`](https://threejs.org/docs/pages/MTLLoader.html))

Blender documents the practical boundaries clearly: basic geometry and
materials/textures are supported, while armatures, animation, lights, cameras,
parenting, and transformations are not part of the format's useful interchange
model. OBJ files can also consume substantial memory because their main encoding
is text.
([Blender OBJ documentation](https://docs.blender.org/manual/en/3.2/addons/import_export/scene_obj.html))

**Java implications:** an OBJ/MTL importer can be pure Java and conceptually
small, but robust interoperability still requires polygon triangulation,
separate position/UV/normal indices, negative indices, smoothing groups,
multiple material groups, path resolution, and tolerant text parsing. It should
return an imported object/group, not claim to restore a complete scene.

### STL

Three.js's STL loader supports ASCII and binary encodings and returns a
non-indexed geometry rather than a scene. The documentation notes optional
binary "Magics" colors and endian/encoding ambiguities, demonstrating that even
this small format has non-standard conventions.
([Three.js `STLLoader`](https://threejs.org/docs/pages/STLLoader.html))

Blender describes STL primarily as a CAD and 3D-printing interchange format.
([Blender STL documentation](https://docs.blender.org/manual/en/2.91/addons/import_export/mesh_stl.html))

**Java implications:** STL is a low-complexity pure-Java importer and a useful
focused example, but it only satisfies a geometry-loading use case. The API must
have an explicit unit/scale policy because the useful interchange model does not
carry a complete JScene3D scene.

### PLY

PLY has ASCII, little-endian binary, and big-endian binary encodings. It
describes exactly one object as extensible elements and properties, normally
vertices and faces. Properties can carry color, normals, texture coordinates,
transparency, or domain-specific data, but PLY deliberately omits transformation
matrices, instancing, hierarchy, object parts, and texture descriptions.
([PLY format description](https://paulbourke.net/dataformats/ply/index.html),
[Stanford 3D Scanning Repository](https://graphics.stanford.edu/data/3Dscanrep/))

Three.js's PLY importer returns geometry and supports configurable mappings for
custom properties; its exporter documents positions, colors, normals, and UVs
but no texture references.
([Three.js `PLYLoader`](https://threejs.org/docs/pages/PLYLoader.html),
[Three.js `PLYExporter`](https://threejs.org/docs/pages/PLYExporter.html))

**Java implications:** a useful PLY importer needs a generic header-driven
property decoder rather than a parser hard-coded only for one vertex layout. It
is still much smaller than a full scene importer and can remain pure Java.

### FBX

Autodesk's FBX model is a full hierarchical scene: nodes can carry meshes,
cameras, lights, skeletons, materials, textures, constraints, animation curves,
poses, and deformation data.
([Autodesk supported scene elements](https://help.autodesk.com/cloudhelp/2018/ENU/FBX-Developer-Help/welcome_to_the_fbx_sdk/supported_scene_elements.html),
[Autodesk FBX scene graph](https://help.autodesk.com/cloudhelp/2018/ENU/FBX-Developer-Help/nodes_and_scene_graph/fbx_scenes.html))

The official Autodesk SDK is a C++ SDK and is distributed under its accompanying
end-user license agreement. Blender states that Autodesk's implementation and
license remain closed and describes its own published binary-format description
as incomplete and reverse-engineered. Three.js's independent loader accepts
limited FBX version ranges and lists unsupported morph/blend-shape normals.
([Autodesk FBX SDK](https://aps.autodesk.com/developer/overview/fbx-sdk),
[Blender's FBX binary-format notes](https://code.blender.org/2013/08/fbx-binary-file-format-specification/),
[Three.js `FBXLoader`](https://threejs.org/docs/pages/FBXLoader.html))

**Java implications:** there is no attractive small, portable, pure-Java path to
high-fidelity FBX. Using Autodesk's SDK would require JNI/native distribution,
platform builds, and license review. Maintaining an independent parser means
tracking a complex, incompletely public format. Converting FBX to glTF during an
asset pipeline is the lower-risk default.

### COLLADA (`.dae`)

Khronos describes COLLADA as a royalty-free schema covering geometry,
materials, textures, lights, cameras, scenes, instancing, animation, skinning,
morphing, shader effects, and physics.
([Khronos COLLADA FAQ](https://www.khronos.org/collada/faq/),
[COLLADA 1.4 specification](https://www.khronos.org/files/collada_spec_1_4.pdf))

The breadth is also its implementation cost. Three.js says COLLADA is very
complex and intentionally supports only a subset; its loader result may contain
a scene, animations, and kinematics. Blender's documentation shows that real
interchange includes profile-specific extras and best-effort behavior for such
features as lights and rigs.
([Three.js `ColladaLoader`](https://threejs.org/docs/pages/ColladaLoader.html),
[Blender COLLADA documentation](https://docs.blender.org/manual/en/4.1/files/import_export/collada.html))

**Java implications:** XML parsing itself is straightforward, but correctly
implementing the schema, profiles, transform stacks, controller/skin semantics,
effect models, animations, coordinate conversion, and vendor extensions is a
large compatibility project. Prefer conversion to glTF unless users provide a
specific corpus that must load directly.

### USD, USDA, USDC, and USDZ

OpenUSD's primary job is not merely file interchange. It composes scene
description from layers and other data sources into one hierarchical scenegraph
view, using references, sublayers, inherits, variants, and other composition
arcs. It also interchanges geometry, shading/materials, lights, rendering data,
linear-blend skinning, and blend-shape animation.
([OpenUSD FAQ](https://openusd.org/release/usdfaq.html),
[OpenUSD composition concepts](https://openusd.org/release/glossary.html#usdglossary-composition))

USDZ is not a simplified scene schema. It is a constrained, uncompressed ZIP
package containing USD layers and allowed asset files, with 64-byte alignment
and rules intended to permit direct consumption without extraction. A USDZ
package can still contain composition and asset references.
([USDZ specification](https://openusd.org/release/spec_usdz.html))

The current OpenUSD repository is open source under the Tomorrow's Open Source
Technology License 1.0 (`TOST-1.0`), while its supported programming interfaces
are C++ and Python rather than Java.
([OpenUSD license](https://github.com/PixarAnimationStudios/OpenUSD/blob/dev/LICENSE.txt),
[OpenUSD FAQ: programming languages](https://openusd.org/release/usdfaq.html#what-programming-languages-are-supported))

**Java implications:** implementing only USDA text parsing would not constitute
general USD support. Full fidelity requires composition, asset resolution,
schema interpretation, binary USDC support, and USDZ packaging behavior. A
future `jscene3d-usd` integration should therefore wrap or communicate with the
official OpenUSD runtime and declare its native/platform costs. USD belongs
after JScene3D has concrete professional visualization or DCC-pipeline demand.

Three.js currently exposes one `USDLoader` for USD, USDA, USDC, and USDZ. That
is useful precedent for the eventual user experience, but it does not remove
the semantic and Java-runtime costs described above.
([Three.js `USDLoader`](https://threejs.org/docs/pages/USDLoader.html))

## Licensing and redistribution observations

This is an engineering survey, not legal advice. The relevant verified facts
are:

| Format or implementation path | Verified licensing/availability fact | Consequence for JScene3D |
| --- | --- | --- |
| glTF 2.0 | Public Khronos specification and registry | A clean-room pure-Java loader is practical; dependency code still needs its own license review |
| OBJ, STL, PLY | Publicly documented legacy formats, but no current governing standards body or modern format-license grant was identified in this survey | Implement from format descriptions and test files; do not copy parser code without checking that code's license |
| COLLADA | Khronos says the specification and schema are free to use without royalties or fees | Pure-Java implementation is legally accessible, though technically broad |
| Autodesk FBX SDK | Free download subject to the accompanying Autodesk EULA | Requires explicit legal and redistribution review in addition to JNI/native packaging |
| OpenUSD | Official implementation uses `TOST-1.0` | A native integration is open source but must comply with that license and bundled dependency notices |
| Assimp | Modified BSD 3-Clause | Suitable for an optional redistributable adapter if its license text and native dependencies are handled |
| JglTF | MIT | Candidate pure-Java parser/model dependency, subject to normal version and maintenance review |

Sources: [Khronos glTF registry](https://registry.khronos.org/glTF/),
[Khronos COLLADA FAQ](https://www.khronos.org/collada/faq/),
[Autodesk FBX SDK](https://aps.autodesk.com/developer/overview/fbx-sdk),
[OpenUSD license](https://github.com/PixarAnimationStudios/OpenUSD/blob/dev/LICENSE.txt),
[Assimp repository](https://github.com/assimp/assimp), and
[JglTF repository](https://github.com/javagl/JglTF).

## Import implementation choices

### Pure-Java format-specific loaders

This is the recommended default for glTF, OBJ/MTL, STL, and PLY. It keeps the
headless scene model usable without native rendering libraries and permits each
loader to expose precise format-specific diagnostics. A loader should map into
JScene3D's public resource descriptions rather than exposing its parser's object
model as the library's permanent API.

The likely artifact boundary is an optional `jscene3d-loaders` artifact (or one
artifact per sufficiently heavy format), rather than adding every parser and
JSON/XML dependency to `jscene3d-core`. The exact artifact split is an
architectural recommendation, not a verified external fact.

### Optional Assimp bridge

Assimp imports more than forty formats into a common scene representation and
is BSD-3-Clause licensed. Its own documentation cautions that some importers are
partial, some source formats lack proper specifications, and the library is
oriented toward one-time asset import rather than fast everyday loading. Assimp
is C/C++, while LWJGL exposes Java bindings to it.
([Assimp repository and license](https://github.com/assimp/assimp),
[Assimp importer limitations](https://github.com/assimp/assimp/blob/master/doc/dox.h),
[LWJGL Assimp binding](https://github.com/LWJGL/lwjgl3))

Assimp is therefore useful as an optional compatibility layer, especially for
FBX and COLLADA, but should not define JScene3D's core scene semantics. It adds
native binaries, platform classifiers, native-memory lifecycle, a conversion
layer, and fidelity differences between formats. A distinct
`jscene3d-assimp` artifact would make those costs explicit.

### Official native runtimes

Autodesk FBX SDK and OpenUSD can provide deeper semantics than a small custom
parser, but both are C++ integrations with packaging, ABI, native-memory, and
platform-support consequences; Autodesk also requires EULA review. They should
not become transitive requirements of the headless core.

## Proposed glTF first-loader contract

Do not publish an unqualified promise of "full glTF support." Publish a versioned
support profile. The first useful profile should load:

- `.gltf` with relative external buffers and PNG/JPEG images;
- `.glb` with embedded buffers and images;
- selected scene or default scene, node hierarchy, names, and local transforms;
- triangle meshes, indices, positions, normals, tangents, UVs, and vertex colors
  where the corresponding JScene3D attribute exists;
- metallic-roughness material data and sampler state only once JScene3D has the
  corresponding material/texture behavior;
- perspective and orthographic cameras;
- shared meshes, materials, and textures without silently duplicating their
  resource descriptions.

Initially unsupported capabilities should be handled explicitly:

- If a file lists an unsupported extension in `extensionsRequired`, loading must
  fail with the extension name. The glTF specification requires clients to fail
  when they cannot support a required extension; optional extensions may be
  ignored while loading the base asset.
  ([glTF extension rules](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#specifying-extensions))
- Unsupported primitive modes or accessor encodings should fail at the affected
  asset with a useful location, not produce corrupted geometry.
- Animations, skins, and morph targets should be added only alongside their
  runtime features and focused runnable examples.
- `KHR_lights_punctual` should be added alongside compatible JScene3D light
  types.
- Draco, Meshopt, and KTX2/Basis compression should be separate optional decoder
  integrations rather than hidden mandatory native/codec costs. Three.js follows
  this pattern by requiring separately supplied decoders/loaders for these
  extensions.
  ([Three.js `GLTFLoader`](https://threejs.org/docs/pages/GLTFLoader.html))

Conformance should use Khronos's official validator and sample assets in
addition to JScene3D-owned regression fixtures.
([Khronos glTF Validator](https://github.com/KhronosGroup/glTF-Validator),
[Khronos glTF Sample Assets](https://github.com/KhronosGroup/glTF-Sample-Assets))

## Native JScene3D scene persistence

A native format eventually becomes valuable when users need to save and restore
JScene3D-specific state: built-in and custom materials, shader source and uniform
values, resource sharing, scene background, renderer-independent settings, and
future library-specific object types.

It should not be Java built-in object serialization. A durable native format
should instead have:

- an explicit schema version independent of the Maven artifact version;
- stable IDs and references so shared geometry, materials, and textures remain
  shared after loading;
- explicit type discriminators with a registry/extension mechanism;
- documented unknown-field and unknown-type behavior;
- portable resource references and a defined package option for embedded assets;
- migration tests from every previously published schema version;
- a clear boundary between serializable scene state and arbitrary application
  code or callbacks.

Those are recommendations derived from JScene3D's stated ownership and API
goals, not properties of an existing specification. Designing this format before
the object model stabilizes would either freeze immature concepts or create an
immediate migration burden. Until then, glTF should be the portable scene/asset
input, while application code or application-owned configuration constructs the
JScene3D-specific parts.

## Decision matrix

| Format | User value | Implementation/fidelity risk | Recommended position |
| --- | ---: | ---: | --- |
| glTF 2.0 + GLB | Very high | Medium to high, but bounded by a public specification and declared profile | First loader |
| OBJ + MTL | High for legacy static assets | Low to medium | Second tier |
| STL | Medium for CAD/printing audiences | Low | Demand-driven geometry loader |
| PLY | Medium for scans/point clouds | Low to medium | Demand-driven geometry loader |
| FBX | High in DCC pipelines | Very high; proprietary SDK or incomplete independent parser | Convert to glTF; optional Assimp bridge later |
| COLLADA | Declining but useful legacy compatibility | High because of schema breadth and profiles | Convert to glTF; optional Assimp bridge later |
| USD/USDZ | High in professional/DCC composition workflows | Extremely high; native runtime and semantic mismatch with a small engine | Separate long-term integration |
| Native JScene3D | Eventually high for exact persistence | High compatibility commitment | Design after the public model stabilizes |

## Suggested immediate architectural decision

Record only the durable direction now:

> JScene3D will treat glTF 2.0/GLB as its primary portable runtime scene and
> asset format. Format support is capability-profiled rather than all-or-nothing.
> Legacy and specialist formats are optional integrations and must not add native
> dependencies to the headless core. A native JScene3D persistence format is a
> separate future concern from asset interchange.

The exact first glTF feature profile should be finalized when the loading
milestone is scheduled, because it depends on which material, texture, lighting,
animation, and geometry capabilities exist at that point.
