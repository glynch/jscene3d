# Assets and animation

JScene3D keeps asset conversion and animation state independent of the OpenGL
renderer. This page describes the current direct glTF loader, texture model,
skeletal and keyframe animation, morph targets, and sprite animation. See
[Examples](examples.md) for runnable demonstrations.

## Load glTF and GLB assets

The optional `jscene3d-gltf` module loads glTF 2.0 JSON and binary GLB files
without requiring LWJGL, OpenGL, or a graphics context:

```java
try (LoadedGltf loaded = GltfLoader.load(Path.of("scene.glb"))) {
    Scene scene = loaded.scene();
    AnimationMixer mixer = new AnimationMixer();
    loaded.animations().forEach(clip -> mixer.action(clip).play());

    // Advance once per application frame while the loaded owner remains open.
    mixer.update(elapsedSeconds);
}
```

`LoadedGltf` owns the converted textures, materials, and geometries. Closing it
is terminal and makes those render resources unavailable; the lightweight
scene graph itself does not require closing. JglTF is an internal parser and no
JglTF type appears in the public JScene3D API.

The current direct loader supports:

- the default scene hierarchy, TRS transforms, and affine matrices that can be
  decomposed without shear;
- triangle primitives, indices, positions, normals, RGB/RGBA vertex colors,
  and `TEXCOORD_0` and `TEXCOORD_1`;
- metallic-roughness materials, alpha modes, double-sided materials, and
  base-color, metallic-roughness, normal, occlusion, and emissive maps;
- PNG and JPEG images plus core glTF sampler state;
- skeletal skinning with four joint influences per vertex;
- relative position and normal morph targets;
- `KHR_draco_mesh_compression` triangle primitives;
- translation, rotation, scale, and morph-weight animation channels using
  step, linear, or cubic-spline interpolation.

Unsupported required extensions, embedded cameras, texture-coordinate set
indices above one, non-triangle primitives, and transforms containing shear
fail with a `GltfLoadException` tied to the source path. The generic project
importer intentionally supports a narrower static, texture-free subset because
it publishes data through project-native resource formats; direct loading and
project importing are not interchangeable capability profiles.

## Textures and texture transforms

`Texture` is a renderer-independent, application-owned RGBA8 image and sampler
description. It defensively copies top-row-first pixel data and distinguishes
sRGB base-color images from linear data textures. Filters, mipmap policy,
horizontal and vertical wrapping, and texture-coordinate origin are explicit.

Offset, repeat, rotation, and rotation center form a cached UV transform.
Changing that transform updates its version without replacing pixels or
rewriting geometry coordinates. Built-in textured materials apply it
automatically; custom shaders must opt into the behavior they need.

Ordinary textures default to bottom-left texture coordinates. The glTF loader
preserves glTF's top-left convention on imported textures instead of rewriting
the model's UV attributes. `StandardMaterial` lets each texture role select the
primary or secondary texture-coordinate set independently.

## Keyframe animation

`AnimationClip` is an immutable named collection of typed tracks. The current
track types animate position, rotation, scale, and morph-target weights.
Keyframe construction copies source arrays, and tracks retain their target
`Object3D` without exposing mutable transform state.

`AnimationMixer` owns an `AnimationAction` for each clip identity. The caller
advances it explicitly with elapsed seconds; the animation package creates no
thread and reads no hidden clock. Actions support play, pause, stop, reset,
seeking, positive or negative time scale, contribution weight, linear fades,
and once, repeat, or ping-pong loops.

Concurrent actions targeting the same property are accumulated before one
coherent result is applied. Partial weights retain influence from the captured
base pose, overweight blends are normalized, and quaternion signs are aligned
before blending. `AnimationMixer.crossFade` fades between two mixer-owned
actions and deactivates the source when the transition completes.

## Skeletal and morph animation

`Skeleton` binds stable ordered `Bone` nodes to copied inverse-bind matrices.
`SkinnedMesh` requires four-component joint and weight attributes and computes
its palette from the current bone transforms. Animated bounds are not updated
automatically in the current implementation, so a skinned mesh starts with
frustum culling disabled unless the application supplies bounds covering every
pose.

`BufferGeometry` can carry named relative morph targets for position and
optional normal deltas. `Mesh` stores morph influences, and
`MorphTargetKeyframeTrack` animates the influence vector. Instanced meshes can
also keep per-instance morph weights for renderer-managed instanced morphing.

## Sprite animation and billboards

`SpriteAnimation` stores a named sequence of `TextureRegion` frames with
per-frame timing and loop behavior. `SpriteAnimationSet` provides a reusable
immutable collection. `TextureRegion.fromPixels` accepts the top-row-first
coordinates commonly produced by image and atlas tools.

`Billboard` is an unlit rectangular object that the renderer orients toward
the active camera. Spherical alignment follows camera yaw and pitch;
cylindrical alignment rotates only around world up. Scale controls world-space
size and the local anchor selects the point held at the object's position.

`AnimatedBillboard` combines a shared material and animation set with
independent playback state. The caller advances it explicitly and can select an
animation, seek by frame and within-frame progress, change playback speed, and
observe animation, frame, loop, and completion events. It owns its generated
quad but not the shared material, texture, or animation set.

Billboards are distinct from triangle meshes: they are not included in mesh
raycasts or shadow-caster passes. Batching is not part of the current billboard
API.

## Ownership checklist

- Keep `LoadedGltf` open while using its scene or animation resources.
- Close application-owned textures, materials, geometries, environment maps,
  billboards, and loaded assets after their dependants stop using them.
- Do not close a shared resource merely because one referencing object is
  removed from a scene.
- Advance mixers and animated billboards from the application's chosen update
  loop; neither facility owns time or creates background threads.

Third-party assets bundled with the examples retain their own provenance and
licensing; see [Third-party notices](../../THIRD_PARTY_NOTICES.md).
