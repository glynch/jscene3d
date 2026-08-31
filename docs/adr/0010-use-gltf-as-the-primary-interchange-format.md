# Use glTF as the primary interchange format

JScene3D will use glTF 2.0 in `.gltf` and `.glb` form as its primary portable
runtime scene and asset format. Each loader release will publish an explicit
capability profile and fail diagnostically when an unsupported required feature
cannot be represented correctly. OBJ/MTL, STL, and PLY are later demand-driven
importers; FBX and COLLADA are conversion-first formats with a possible optional
Assimp bridge; USD/USDZ is a separate long-term specialist integration.

Loaders remain optional and do not add parser or native dependencies to the core
scene model or LWJGL renderer. A native JScene3D persistence format is a separate
future design for preserving library-specific state and will not use Java object
serialization.

glTF is a better fit than simpler mesh formats because it can deliver complete
runtime-oriented hierarchies, resources, cameras, materials, and animation. Its
greater implementation cost and dependence on matching renderer features are
controlled through the capability profile rather than through partial silent
imports or a promise of universal format fidelity.

Version 0.1 includes correct static glTF/GLB loading after its required
metallic-roughness PBR and lighting foundations were implemented. Animation,
skinning, morph targets, stored lights, and compression extensions follow only
with matching runtime capabilities and focused examples.

The loader is published separately as `io.github.glynch:jscene3d-gltf`. It
depends on `jscene3d-core` and performs parsing and scene-model construction
without requiring `jscene3d-lwjgl`, GLFW, OpenGL, or an active graphics context.
This keeps loader dependencies optional and permits headless import tests and
offline asset processing.

JglTF is the internal parser and reference resolver. Its types are not exposed
through JScene3D interfaces; callers receive one `LoadedGltf` owner containing
the converted JScene3D scene and close its resources through that owner.
