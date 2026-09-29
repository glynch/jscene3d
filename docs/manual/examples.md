# Examples

JScene3D groups its interactive examples into rendering and asset-loading,
physics, game-runtime, and audio suites. Each suite can run in a searchable
native browser, and each example remains directly launchable for focused work.

## Launch an example

List every entry point supported by the repository runner:

```shell
./tools/scripts/run-example.sh --list
```

Launch one by Java class name:

```shell
./tools/scripts/run-example.sh SolarSystemViewer
```

The runner resolves the owning example module, then runs a clean compile of
that module and its reactor dependencies with the `run-example` profile. It
does not run tests or the complete verification lifecycle. Because the command
includes Maven's `clean` phase, it is intentionally not an incremental build.

## Example browsers

The four visual suites have dedicated browser entry points:

```shell
./tools/scripts/run-example.sh ExampleBrowser
./tools/scripts/run-example.sh PhysicsExampleBrowser
./tools/scripts/run-example.sh GameExampleBrowser
./tools/scripts/run-example.sh AudioExampleBrowser
```

All four use `jscene3d-example-framework`. A browser keeps one native window
and renderer alive while replacing the selected example. Replacing a selection
closes the previous example before creating the next one.

The left panel searches titles, categories, descriptions, tags, and bundled
asset attribution. Click a thumbnail to select it, use the wheel or trackpad to
scroll, or navigate filtered results with:

- Up and Down for the previous or next result;
- Page Up and Page Down for a visible page;
- Home and End for the first or last result.

Click the search field to enter text. Clicking the live example returns
pointer and keyboard input to it. Escape first releases a captured pointer; a
subsequent Escape closes the browser.

## Browse by capability

The rendering and asset browser is deliberately broad. Useful starting points
include:

- **Foundations:** `BasicTriangleExample`, `TransformsExample`,
  `HierarchyExample`, `CamerasExample`, and `BufferGeometryExample`.
- **Materials and lighting:** `MaterialsExample`, `StandardMaterialExample`,
  `LightingExample`, `SpotAndHemisphereLightsExample`, `ShadowsExample`,
  `FogExample`, and `EnvironmentLightingExample`.
- **Textures and assets:** `TexturedCubeExample`,
  `TextureTransformsExample`, `GltfLoadingExample`, and the Avocado, Water
  Bottle, Boom Box, and Littlest Tokyo model examples.
- **Animation:** `KeyframeAnimationExample`, `AnimationBlendingExample`,
  `SkeletalAnimationExample`, `MorphTargetsExample`,
  `GltfAnimationExample`, and `GltfMorphAnimationExample`.
- **Rendering techniques:** `TransparencyExample`, `ShaderMaterialExample`,
  `InstancingExample`, `InstanceAttributesExample`,
  `InstancedMorphTargetsExample`, and the line and helper examples.
- **Interaction:** `OrbitControlsExample`, `PointerLockControlsExample`, and
  `ObjectSelectionExample`.
- **Complete scenes:** `SolarSystemViewer` and `LittlestTokyoExample`.

The physics suite demonstrates fixed-step character movement, grounding,
jumping, wall sliding, step traversal, sensors, and debug geometry. The game
suite adds semantic input, first- and third-person control, and interpolated
presentation. The audio suite demonstrates positional attenuation and
independent music/effects mixing.

Use `--list` as the authoritative inventory rather than relying on a copied
catalogue in documentation.

## Maintain thumbnails

The browsers load checked-in thumbnails for their catalogued examples.
Maintainers can regenerate an entire suite from the examples' own OpenGL
framebuffers:

```shell
./tools/scripts/capture-example-thumbnails.sh
./tools/scripts/capture-example-thumbnails.sh --suite physics
./tools/scripts/capture-example-thumbnails.sh --suite game
./tools/scripts/capture-example-thumbnails.sh --suite audio
```

With no catalogue IDs, the command captures the complete selected suite. Pass
one or more stable, lowercase-hyphen catalogue IDs to capture only those
entries:

```shell
./tools/scripts/capture-example-thumbnails.sh shadows basic-triangle
./tools/scripts/capture-example-thumbnails.sh --suite game first-person-sandbox
```

The capture utility creates one hidden shared window, settles each selected
example for three frames, captures the renderer viewport without the browser's
large control overlays, and writes PNG files in suite order. It uses an
incremental `compile` command, unlike `run-example.sh`.

For silent audio capture on a machine with OpenAL Soft, select its null driver:

```shell
ALSOFT_DRIVERS=null ./tools/scripts/capture-example-thumbnails.sh \
  --suite audio positional-audio
```

Omit that environment variable when listening interactively.

## Package the example browser

On a macOS ARM64 host, build the standalone browser application and disk image
with:

```shell
./mvnw clean verify -pl jscene3d-examples -am \
  -Pexample-distribution-macos-arm64
```

The result is:

```text
jscene3d-examples/target/distribution/
└── jscene3d-examples-1.0.0-macos-arm64.dmg
```

The disk image contains `JScene3D Examples.app`, the application assets and
runtime dependencies, macOS ARM64 LWJGL natives, and a trimmed Java runtime.
The launcher supplies `-XstartOnFirstThread`. Its bundle version is `1.0.0`
because `jpackage` requires a positive first macOS version component; this does
not change the JScene3D artifact version.

## Related references

- [Rendering](rendering.md)
- [Assets and animation](assets-and-animation.md)
- [Application runtime](application-runtime.md)
- [Third-party notices](../../THIRD_PARTY_NOTICES.md)
