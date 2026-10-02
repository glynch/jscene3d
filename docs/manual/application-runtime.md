# Application runtime

JScene3D's runtime facilities instantiate authored Scenes as live Worlds and connect them to fixed-step simulation,
input, physics, audio, native desktop hosting, and export. The detailed entity
and component model is covered in
[Project and game fundamentals](project-fundamentals.md) and the
[entity-component world architecture](../design/entity-component-world-architecture.md).

## Physics worlds and collision

`jscene3d-physics` is independent of rendering. A `PhysicsWorld` owns its
registered collision objects and answers deterministic three-dimensional
queries:

- `StaticBody` represents immovable world geometry;
- `KinematicBody` is moved explicitly by its caller;
- `CollisionSensor` reports non-blocking overlaps;
- each object owns one or more locally transformed `Collider` instances;
- box, sphere, capsule, and static triangle-mesh shapes describe geometry;
- raycast, overlap, and convex sweep queries accept explicit filters.

Collision filtering is mutual. Sensors participate in overlap reporting but do
not block movement. `PhysicsWorld.move` resolves a desired translation for a
registered kinematic body and returns contacts plus deterministic sensor enter,
stay, and exit events.

`PhysicsWorld.debugSnapshot()` exposes renderer-independent world-space lines
for the registered colliders. Applications may render those lines without
introducing a renderer dependency into the physics module.

## Character movement

`CharacterController` builds grounding, gravity, jumping, wall sliding, and
step traversal on top of kinematic movement. The caller supplies planar
velocity once per fixed update; the controller owns vertical movement state and
returns an immutable `CharacterMoveResult`.

`CharacterMovementController` in `jscene3d-game` converts semantic movement
actions and an application-supplied view direction into normalized,
camera-relative planar velocity and jump requests. It depends on a
`CharacterController`, not a particular camera or presentation implementation.

`PhysicsBinding` is the lower-level presentation adapter. Capture a physics
transform after each fixed update and apply an interpolation fraction before
rendering. It updates the caller-owned scene object without changing physics
state.

## Sensors and queries

Collision sensors model trigger-like regions while preserving the same shape,
transform, filtering, and ownership rules as other collision objects. Movement
results identify overlap phase transitions, while direct overlap, raycast, and
sweep operations support systems that do not move a character.

In authored projects, `jscene3d-project-physics` supplies the descriptors and
runtime components that expose these facilities through the generic world
model. The low-level physics API remains usable without a project.

## Timing and semantic input

`WorldFrameDriver` advances an already active `World`. It owns fixed-step
accumulation, clamps long host frames, bounds catch-up work, buffers pressed and
released transitions until a fixed update consumes them, and supplies an
interpolation fraction to the frame-update phase. It does not create, activate,
render, or close the world.

`InputMap` compiles authored `InputMapDefinition` data or can be built
programmatically. It maps keyboard, mouse, and standard gamepad controls to
typed semantic button, one-dimensional axis, and two-dimensional axis actions.
`InputCapture` prevents host UI interactions from leaking into game input.
Runtime components consume semantic actions rather than device-specific key
codes.

## Desktop project hosting

`jscene3d-project-desktop` supplies the standard graphical host. Its
`DesktopProjectRunner`:

1. loads and validates the project descriptor;
2. uses `ProjectRuntimeHost` to compose the requested inactive world;
3. creates the native window, renderer, input, 3D, physics, presentation, and
   audio facilities required by the project;
4. activates and advances the world through `WorldFrameDriver`;
5. renders the active primary camera and screen overlays;
6. releases the complete owned session when the application ends.

The runner creates no parallel game lifecycle. Trusted application behavior is
discovered from the runtime extension selected by the descriptor. Packaged
applications start through the generic `DesktopProjectLauncher`, which accepts
the engine version, packaged project directory, and published-content
directory as arguments or launch properties. Local development may select a
named playtest profile without making it exported project state.

## Audio

The optional `jscene3d-audio` module owns one OpenAL device and context per
`AudioEngine`. It supports complete buffered clips from classpath Ogg Vorbis or
in-memory signed 16-bit mono/stereo PCM. Streaming is intentionally outside the
current interface.

An `AudioClip` is reusable; each `AudioSource` has independent playback,
seeking, looping, gain, position, velocity, and attenuation state. Mono clips
can be spatialized, while stereo clips are intended for listener-relative music
or interface playback. `AudioListener` controls the listener transform, and
master plus `MUSIC` and `EFFECTS` category gains provide separate mixing
levels.

The engine and every child object are confined to the thread that created the
engine. Close sources before clips, or close the engine to release its remaining
sources, clips, context, and device. The desktop host synchronizes the audio
listener with the active project camera.

## Application export and packaging

`jscene3d-project-export` separates content selection from native packaging.
`ApplicationDirectoryExporter` first validates and assembles a relocatable
directory containing relative launchers, runtime JARs, native dependencies,
project data, and completed published imports. Assembly uses a sibling staging
directory and installs the result transactionally where the filesystem permits.

`ApplicationImageExporter` consumes that directory through `jpackage` rather
than repeating its selection rules. The current native application-image
implementation is macOS-specific. `MacOsDiskImageExporter` can package the
completed `.app` as a non-interactive DMG. The export model supports a project
`.icns` application icon and a project-owned Finder background that the
exporter converts to the TIFF resource required by `jpackage`.

A conventional executable JAR is not the application export contract. The
exported launcher and its bundled runtime preserve the same generic desktop
project host used during development.

## Explore the runtime

The native suites described in [Examples](examples.md) demonstrate character
physics, first- and third-person semantic input, positional audio, and mixing.
The `jscene3d-project-examples` module also contains focused composition,
spawning, collision, telemetry, and application-directory export examples.
