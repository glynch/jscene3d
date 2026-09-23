# JScene3D Code OSS Integration — Electron POC Handover

## Purpose

This document hands over the results of the standalone Electron proof of concept developed to validate the native rendering architecture required for integrating JScene3D into a Code OSS-based editor.

The POC is now technically complete. Its purpose was to retire the major architectural and platform risks before beginning the Code OSS implementation.

The POC demonstrated a working end-to-end rendering architecture in which Java/LWJGL/OpenGL renders directly into an IOSurface owned and coordinated by Electron. Chromium consumes that IOSurface through its shared-texture infrastructure and presents it in the browser renderer using WebGPU, without CPU framebuffer readback or pixel copying.

The POC also validated the supporting lifecycle required for a real editor integration, including:

- secure initial IOSurface transfer to the Java child process using Mach port rendezvous;
- a persistent Mach control channel for transferring replacement IOSurfaces;
- continuous rendering with frame back-pressure;
- browser-to-Java interaction;
- dynamic render-surface replacement during viewport resizing;
- Retina/device-pixel-ratio handling;
- resize event coalescing;
- explicit renderer readiness;
- repeated surface replacement without restarting Java; and
- complete Java renderer shutdown and recreation within the same Electron process.

The POC should be treated as a working reference implementation and source of architectural evidence, not as production code to copy wholesale.

Some implementation choices were deliberately appropriate for proving the architecture quickly—for example, the simple line-oriented renderer protocol, the standalone test page, the test cube, and POC-specific Java/JNI organization. These should not automatically become production design decisions.

The next phase is to use the mechanisms and lifecycle proven by the POC to design and implement the JScene3D editor integration in Code OSS, while fitting that implementation cleanly into the existing JScene3D architecture and Code OSS lifecycle.

This handover therefore distinguishes between:

1. mechanisms and lifecycle behavior that have been proven by the POC;
2. architectural direction suggested by those results; and
3. production design decisions that remain to be made during the Code OSS implementation.

## 1. POC Scope and Outcome

The Electron POC was created to answer the technical questions that needed to be resolved before modifying Code OSS.

The primary question was whether a Java/LWJGL JScene3D renderer could render directly into GPU-backed memory that Electron/Chromium could consume efficiently, without introducing a CPU framebuffer-copy path.

The target architecture was:

```text
Java / JScene3D / LWJGL / OpenGL
                ↓
             IOSurface
                ↓
       Electron native layer
                ↓
     Chromium SharedTexture
                ↓
            VideoFrame
                ↓
     WebGPU external texture
                ↓
        browser/editor view
```

The POC successfully demonstrated this complete path.

Java runs as a separate child process of Electron. Electron creates the shared IOSurface and transfers access to Java using Mach rights. Java renders directly into that IOSurface using LWJGL/OpenGL. Electron then imports the same surface into Chromium's shared-texture infrastructure and delivers it to the browser renderer for WebGPU presentation.

No CPU framebuffer readback is required between Java and Chromium.

The POC was subsequently extended beyond a single static frame to validate the lifecycle required by an interactive editor.

The final POC successfully demonstrated:

- creation of an IOSurface by Electron;
- secure transfer of the initial IOSurface to the Java child process;
- acquisition of that IOSurface from JNI;
- binding of the IOSurface to an OpenGL texture;
- rendering into an FBO backed by the IOSurface;
- importing the IOSurface into Chromium as a shared texture;
- presentation of the resulting frame in a WebGPU canvas;
- browser-to-Java pointer interaction;
- continuous animated rendering;
- one-frame-in-flight back-pressure;
- dynamic creation and transfer of replacement IOSurfaces;
- repeated surface replacement without restarting Java;
- rendering and interaction while viewport resizing occurs;
- correct handling of physical pixel dimensions on a Retina display;
- resize-event debouncing to avoid excessive surface allocation;
- explicit Java renderer readiness before rendering or resizing begins;
- preservation of the desired viewport size across renderer recreation;
- complete shutdown of one Java renderer and launch of another within the same Electron process; and
- successful continuation of animation, interaction and correctly sized rendering after renderer recreation.

The POC also exposed and resolved several lifecycle and ownership problems that would otherwise likely have appeared during the Code OSS implementation. These included control-channel lifetime, startup ordering, callback registration ordering, viewport-state persistence and resize behavior.

The POC was intentionally limited in scope.

It was not intended to establish the final Code OSS API, final Java module structure, production renderer protocol, crash-recovery strategy, multi-editor/session model, cross-platform implementation, security hardening, production diagnostics, or final testing architecture.

Those remain production design concerns.

The significant outcome of the POC is therefore not the test cube or the standalone Electron application. It is that the core macOS rendering and process architecture has been demonstrated end-to-end under continuous rendering, interaction, resizing, repeated IOSurface replacement and renderer recreation.

The Code OSS work can proceed from a proven rendering mechanism rather than beginning with an unresolved platform experiment.

## 2. Proven Rendering Architecture

The central architecture proven by the POC is a zero-CPU-copy rendering path between the Java JScene3D renderer and the Chromium browser renderer.

At a high level:

```text
┌─────────────────────────────────────────────┐
│ Java Renderer Process                       │
│                                             │
│ JScene3D / LWJGL / OpenGL                   │
│                  │                          │
│                  ▼                          │
│           OpenGL framebuffer                │
│                  │                          │
│                  ▼                          │
│              IOSurface                      │
└──────────────────┬──────────────────────────┘
                   │
                   │ shared GPU-backed surface
                   │
┌──────────────────▼──────────────────────────┐
│ Electron Browser Process                    │
│                                             │
│ JScene3D renderer session                   │
│                  │                          │
│                  ▼                          │
│       Chromium SharedTexture                │
└──────────────────┬──────────────────────────┘
                   │
                   │ shared texture delivery
                   │
┌──────────────────▼──────────────────────────┐
│ Chromium Renderer Process                   │
│                                             │
│ imported SharedTexture                      │
│          ↓                                  │
│ VideoFrame                                  │
│          ↓                                  │
│ WebGPU external texture                     │
│          ↓                                  │
│ editor canvas                               │
└─────────────────────────────────────────────┘
```

The IOSurface is the shared rendering resource connecting the native Java/OpenGL renderer with Chromium.

Electron creates and owns the browser-side representation of the surface. Java receives access to the same underlying IOSurface through a Mach port and binds it to OpenGL.

Java therefore renders directly into memory that Chromium can subsequently consume.

There is no intermediate operation equivalent to:

```text
glReadPixels()
    ↓
CPU byte buffer
    ↓
IPC pixel transfer
    ↓
browser texture upload
```

Avoiding that path is fundamental to the proposed editor architecture. A CPU framebuffer-copy design would introduce unnecessary memory bandwidth, latency and synchronization overhead, particularly for large Retina-resolution editor viewports and continuous rendering.

### 2.1 Process Boundaries

The proven architecture uses three distinct execution contexts:

```text
Electron browser process
        │
        ├──── Java renderer child process
        │
        └──── Chromium renderer process
```

The Java renderer is a direct child of the main Electron process.

The Java renderer does not run inside the Chromium renderer process and does not require embedding a JVM into Chromium.

The Chromium renderer does not communicate directly with Java.

Electron acts as the coordinator between the two sides:

```text
Chromium renderer
        ↕
Electron browser process
        ↕
Java renderer
```

This provides a useful architectural boundary for the Code OSS implementation.

The Java process remains responsible for JScene3D rendering.

The browser renderer remains responsible for the editor UI and presentation.

The Electron/native layer owns the lifecycle and shared-resource bridge between them.

### 2.2 Rendering Direction

Rendered frames travel conceptually in this direction:

```text
Java
  ↓
OpenGL
  ↓
IOSurface
  ↓
Electron
  ↓
Chromium SharedTexture
  ↓
VideoFrame
  ↓
WebGPU
  ↓
editor viewport
```

The IOSurface itself is not copied between these stages. The processes exchange rights/handles that allow them to refer to the shared resource.

### 2.3 Control and Input Direction

Control information travels separately from rendered image data.

For example, pointer interaction follows:

```text
editor viewport
      ↓
browser renderer
      ↓
Electron
      ↓
renderer command
      ↓
Java
      ↓
JScene3D scene state
```

Viewport changes similarly originate on the browser side but result in Electron creating and transferring a new rendering surface to Java.

This separation is important:

```text
large rendered image data
    → shared GPU resource

small control/lifecycle messages
    → IPC / renderer protocol
```

The design does not send rendered pixels through the command protocol.

### 2.4 Surface Ownership

Electron is responsible for creating rendering surfaces.

Java does not independently create the IOSurface that Chromium will consume.

The initial lifecycle is therefore:

```text
Electron creates IOSurface
        ↓
Electron retains browser-side ownership
        ↓
Electron transfers an IOSurface Mach right to Java
        ↓
Java looks up the IOSurface
        ↓
Java binds it to OpenGL
```

The same principle applies during resizing:

```text
Electron creates replacement IOSurface
        ↓
Electron retains it as pending
        ↓
Electron transfers access to Java
        ↓
Java successfully binds replacement
        ↓
Java acknowledges replacement
        ↓
Electron promotes replacement to active
```

This ownership model gives Electron control over which surface Chromium presents and allows surface transitions to be coordinated safely.

### 2.5 Platform Boundary

The POC proves the architecture specifically on macOS using:

```text
IOSurface
Mach ports
Chromium MachPortRendezvous
OpenGL / LWJGL
Chromium SharedTexture
WebGPU
```

The generic renderer/session concepts should not depend directly on IOSurface or Mach APIs.

Platform-specific responsibilities should remain behind abstractions for concepts such as:

```text
shared rendering surface
inter-process surface transfer
native shared-texture handle
```

The initial Code OSS implementation can remain macOS-focused, but the architecture should avoid unnecessarily making higher-level editor and renderer-session code macOS-specific.

### 2.6 Architectural Result

The most important conclusion from the POC is that the fundamental rendering architecture is viable.

The Code OSS implementation does not need to investigate whether Java/OpenGL output can be efficiently presented inside a Chromium-based editor. That mechanism has already been demonstrated.

The production work should therefore concentrate on integrating this proven rendering path cleanly into Code OSS and the existing JScene3D architecture rather than redesigning the underlying transport from scratch.

## 3. Electron Native Architecture

The Electron native implementation developed for the POC separates renderer lifecycle, child-process management, shared-surface management, and platform-specific Mach/IOSurface operations.

The principal POC source files are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_surface.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/
```

The POC arrived at the following conceptual decomposition:

```text
JavaScript / Electron API
            │
            ▼
JScene3DRendererSession
            │
            ├── JScene3DRendererProcess
            │
            ├── JScene3DControlChannel
            │
            ├── active JScene3DSurface
            │
            └── pending JScene3DSurface
```

This decomposition should be treated as architectural evidence rather than a requirement to reproduce the exact POC class structure.

### 3.1 JScene3DRendererSession

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc
```

`JScene3DRendererSession` is the central coordinator for one Java rendering session.

Its responsibilities in the final POC include:

- creating the initial shared rendering surface;
- creating the control channel;
- configuring both resources for Java process launch;
- coordinating the Java child process;
- owning the currently active surface;
- owning a pending replacement surface during resize;
- initiating surface replacement;
- interpreting renderer protocol events;
- validating `SURFACE_READY`;
- promoting a pending surface to active;
- maintaining renderer-ready and frame-ready callbacks; and
- coordinating session shutdown.

The session deliberately does not contain the low-level pipe and process-watching implementation. That responsibility was extracted into `JScene3DRendererProcess`.

Conceptually:

```text
JScene3DRendererSession
        │
        ├── renderer lifecycle
        ├── surface lifecycle
        ├── protocol semantics
        └── control-channel coordination
```

The session is therefore the point at which the process lifecycle and rendering-resource lifecycle meet.

A particularly important responsibility is active/pending surface ownership.

During a replacement:

```text
active surface A
        │
        ├── remains active
        │
        ▼
create surface B
        │
        ▼
pending surface B
        │
        ▼
transfer B to Java
        │
        ▼
Java binds B
        │
        ▼
SURFACE_READY B
        │
        ▼
validate B
        │
        ▼
promote B
        │
        ▼
active surface B
```

The existing active surface is not discarded merely because the replacement was created or transferred.

This gives the surface transition an explicit acknowledgement boundary.

### 3.2 JScene3DRendererProcess

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc
```

`JScene3DRendererProcess` was extracted from `JScene3DRendererSession` during the POC.

The reason for this refactoring was separation of responsibility, not source-file length.

Its responsibilities include:

- constructing the Java process command line;
- launching the Java child process;
- establishing stdin and stdout pipes;
- writing commands to Java stdin;
- monitoring Java stdout;
- buffering partial stdout reads;
- converting stdout into complete lines;
- forwarding complete output lines to the session; and
- requesting graceful renderer shutdown.

The responsibility boundary is:

```text
JScene3DRendererProcess
    process + transport mechanics

JScene3DRendererSession
    renderer + surface semantics
```

The process object does not need to understand the meaning of `FRAME_READY`, `SURFACE_READY`, or other renderer events.

It only needs to deliver complete lines to the session.

Similarly, most renderer protocol semantics should not migrate into the generic process-management class.

One exception in the POC is graceful shutdown: the process object sends the protocol's `QUIT` command because terminating the child process is part of its lifecycle responsibility.

### 3.3 JScene3DSurface

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_surface.h
```

The corresponding macOS implementation is under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/
```

`JScene3DSurface` represents the native rendering surface at the generic JScene3D session layer.

The higher-level session should reason in terms of:

```text
surface
width
height
shared-texture handle
inter-process transfer
```

rather than directly manipulating IOSurface APIs.

The macOS implementation is responsible for the actual IOSurface operations.

Platform-specific responsibilities include:

- creating the IOSurface;
- retaining its native lifetime;
- creating a Mach send right for transfer to Java; and
- exposing the platform handle required by Chromium's shared-texture import path.

This abstraction is important for keeping platform details out of the renderer-session lifecycle.

The production implementation may evolve the abstraction, but the higher-level Code OSS/JScene3D integration should not directly depend on IOSurface implementation details unless there is a strong reason to do so.

### 3.4 JScene3DControlChannel

Generic reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h
```

macOS reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm
```

The control channel exists to transfer native rendering resources after the Java process has already launched.

Initial Mach rendezvous solves bootstrap, but it does not by itself provide the repeated resource-transfer mechanism required for viewport resizing.

The control channel therefore has a lifetime associated with the renderer session rather than with an individual surface.

On macOS, its conceptual ownership is:

```text
Electron
    │
    └── persistent Mach send right

Java renderer
    │
    └── persistent Mach receive right
```

For each replacement surface, Electron creates a Mach send right representing that IOSurface and transfers that right through the persistent channel.

The control channel itself remains alive.

This lifetime distinction proved critical during the POC and is covered in detail later in the handover.

### 3.5 Native Electron Binding

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc
```

The POC exposes the native renderer functionality to Electron JavaScript through a linked binding.

The binding is intentionally thin.

Its responsibilities are primarily to:

- own/access the current renderer session;
- create a session;
- install lifecycle callbacks;
- launch the renderer;
- expose the current surface;
- forward renderer commands;
- request resize; and
- stop the renderer.

The binding should not become the place where renderer lifecycle logic accumulates.

That logic belongs in the renderer/session layer.

An important ordering decision was eventually incorporated into the launch API: renderer callbacks are supplied as part of launch so that they can be installed before the Java process starts.

Conceptually:

```text
JavaScript calls launch
        ↓
create session
        ↓
install callbacks
        ↓
launch Java process
```

rather than:

```text
launch Java process
        ↓
register callbacks later
```

The latter creates a race with early renderer lifecycle events.

### 3.6 Protocol Vocabulary

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h
```

Protocol constants were moved out of individual C++ implementation files once the renderer communication evolved beyond the earliest POC stages.

This prevents strings such as:

```text
QUIT
RECEIVE_SURFACE
FRAME_READY
SURFACE_READY
RENDERER_READY
```

from being scattered throughout unrelated classes.

The Java side has a corresponding protocol definition.

The POC does not prove that duplicated C++/Java string constants are the correct final production protocol architecture. It only establishes that protocol vocabulary should be explicit and centralized rather than represented by arbitrary literals throughout the implementation.

### 3.7 Responsibility Boundary to Preserve

The useful conceptual boundary produced by the POC is:

```text
Electron API
    exposes capability

RendererSession
    coordinates one renderer and its shared resources

RendererProcess
    manages the Java child and byte/line transport

Surface abstraction
    represents shared rendering storage

ControlChannel abstraction
    transfers replacement native resources

Platform implementation
    implements IOSurface + Mach behavior
```

The production Code OSS integration does not need to preserve every POC class exactly, but it should preserve these responsibility boundaries unless the Code OSS architecture provides a clearly better existing abstraction.

## 4. Initial IOSurface Transfer via Mach Rendezvous

The first shared rendering surface must be transferred from Electron to the Java renderer during process startup.

The POC deliberately avoided using globally discoverable IOSurface IDs. Instead, it transfers an IOSurface Mach port to the child process using Chromium's existing Mach port rendezvous infrastructure.

The relevant Electron-side POC implementation can be inspected under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/
```

The Java/JNI bootstrap implementation is under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/
```

In particular, the rendezvous implementation is:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.c
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.h
```

### 4.1 Bootstrap Sequence

The initial startup sequence is conceptually:

```text
Electron
    │
    ├── create initial IOSurface
    │
    ├── obtain IOSurface Mach send right
    │
    ├── create control channel
    │
    ├── prepare Mach rendezvous ports
    │
    └── launch Java child
             │
             ▼
Java / JNI
    │
    ├── connect to parent Mach rendezvous
    │
    ├── receive initial IOSurface Mach right
    │
    ├── receive control-channel receive right
    │
    ├── IOSurfaceLookupFromMachPort(...)
    │
    └── construct OpenGL render target
```

The IOSurface itself is not serialized or copied into the child process.

Electron transfers a Mach right which allows Java to obtain a reference to the same underlying IOSurface.

### 4.2 Chromium Mach Rendezvous

The POC uses Chromium's existing macOS process-launch machinery rather than introducing an independent bootstrap server.

The Electron side adds the required Mach ports to the child process launch configuration.

Conceptually:

```text
base::LaunchOptions
        │
        └── mach_ports_for_rendezvous
                 │
                 ├── initial IOSurface right
                 └── control-channel receive right
```

The Java/JNI side connects to the rendezvous service associated with the parent Electron process and retrieves those rights.

During successful POC execution, startup produced output equivalent to:

```text
Created JScene3D surface: 800x600
Launched Java renderer PID: ...

JNI bootstrap name: com.github.Electron.MachPortRendezvousServer....
JNI received JScene3D control receive right: ...
JNI received IOSurface Mach port: ...
JNI IOSurface lookup succeeded: 800x600
Java acquired IOSurface through Mach rendezvous: ...
```

This sequence was exercised repeatedly, including after completely stopping one Java renderer and launching another within the same Electron process.

### 4.3 Initial IOSurface Ownership

Electron creates the initial IOSurface before Java starts.

Conceptually:

```text
Electron
    │
    ├── owns native IOSurface
    │
    └── creates transferable Mach right
                  │
                  ▼
                Java
                  │
                  └── obtains IOSurfaceRef
```

The transfer of the Mach right does not mean Electron relinquishes its own rendering-surface state.

Both sides obtain the native access required for their respective responsibilities:

```text
Electron / Chromium
    uses IOSurface for SharedTexture presentation

Java / OpenGL
    uses IOSurface as rendering storage
```

The native resource lifetime therefore has to account for references held on both sides.

### 4.4 Java Acquisition

The Java-facing JNI bridge ultimately converts the received Mach port into an `IOSurfaceRef`.

The important native operation is:

```c
IOSurfaceLookupFromMachPort(...)
```

The resulting `IOSurfaceRef` is represented to Java as an opaque native handle.

Java does not need to understand Mach ports or IOSurface internals.

Its conceptual API is closer to:

```text
lookup parent renderer resources
        ↓
receive native surface handle
        ↓
construct IOSurfaceRenderTarget
```

The JNI/native layer contains the platform-specific implementation.

This is an important boundary for the production JScene3D architecture: ordinary Java scene/rendering code should not become responsible for macOS Mach bootstrap mechanics.

### 4.5 Control Channel Bootstrapping

The same initial rendezvous also transfers the receive right for the persistent control channel.

This is important because the two resources serve different purposes:

```text
initial IOSurface right
    bootstraps the first rendering surface

control-channel receive right
    bootstraps future native-resource transfers
```

The initial IOSurface right is associated with one surface.

The control-channel right is associated with the renderer session.

Once Java has acquired the control-channel receive right, Electron can subsequently transfer replacement IOSurfaces without another process launch or another rendezvous bootstrap.

### 4.6 Why Rendezvous Is Preferred to Global IOSurface IDs

The POC specifically proved that global IOSurface IDs are unnecessary for this architecture.

The desired model is capability transfer:

```text
Electron explicitly owns resource
        ↓
Electron explicitly transfers access
        ↓
specific Java child receives access
```

rather than:

```text
publish globally discoverable surface identifier
        ↓
other process looks it up globally
```

The Mach-right design gives the process relationship and resource transfer an explicit ownership boundary.

The Code OSS implementation should therefore preserve the Mach-right/rendezvous approach unless a concrete production constraint requires a different mechanism.

The POC has already demonstrated that this approach works with the Electron/Chromium process model.

### 4.7 Renderer Readiness Is Separate from Process Launch

Successful Mach rendezvous does not by itself mean that the renderer is ready to accept frames.

After receiving the initial IOSurface, Java still has to complete renderer initialization:

```text
receive IOSurface
        ↓
IOSurfaceLookupFromMachPort
        ↓
initialize GLFW/OpenGL
        ↓
create OpenGL capabilities
        ↓
construct IOSurfaceRenderTarget
        ↓
bind IOSurface to OpenGL
        ↓
RENDERER_READY
```

This distinction became important later when dynamic resize requests could arrive immediately after the browser page initialized.

The production implementation should preserve this lifecycle boundary:

```text
process launched
    !=
renderer ready
```

### 4.8 Result Proven by the POC

The POC established that Electron can securely bootstrap a separate Java/LWJGL renderer with a non-global IOSurface using Chromium's existing macOS process-launch and Mach rendezvous infrastructure.

This mechanism worked both for the initial renderer process and after destroying that renderer and launching a replacement renderer inside the same Electron application process.

The initial shared-surface bootstrap therefore does not remain an unresolved Code OSS integration risk.

## 5. Persistent Mach Control Channel

The initial Mach rendezvous solves renderer bootstrap, but dynamic editor operation requires Electron to transfer additional native resources after the Java process is already running.

The POC therefore establishes a persistent Mach control channel between Electron and Java.

The principal Electron-side references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm
```

The corresponding Java/JNI-side references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.c
```

The control channel was introduced specifically to support repeated IOSurface replacement without restarting Java.

### 5.1 Channel Creation

Electron creates a Mach port pair consisting conceptually of:

```text
receive right
send right
```

The receive right is transferred to Java as part of the initial Mach rendezvous.

Electron retains the send right.

After startup, ownership is therefore:

```text
Electron process
    │
    └── persistent control-channel send right
                  │
                  │ Mach messages
                  ▼
Java renderer process
    │
    └── persistent control-channel receive right
```

The receive right is transferred only once during renderer bootstrap.

It is not recreated for each IOSurface replacement.

### 5.2 Purpose of the Channel

The control channel transfers native resource rights that cannot be represented by the ordinary text command protocol.

The POC uses it to transfer replacement IOSurface Mach rights.

The control flow for a replacement is:

```text
Electron creates IOSurface B
        ↓
Electron obtains Mach send right for B
        ↓
Electron sends Mach message through control channel
        ↓
message contains IOSurface B Mach port descriptor
        ↓
Java receives message
        ↓
Java obtains IOSurface B Mach port
        ↓
IOSurfaceLookupFromMachPort(...)
        ↓
Java obtains IOSurfaceRef for B
```

The ordinary renderer protocol is then used to coordinate when Java should receive and adopt that transferred resource.

This gives two complementary communication mechanisms:

```text
stdin/stdout renderer protocol
    small commands and lifecycle events

Mach control channel
    native capability/resource transfer
```

These should not be conflated.

### 5.3 Surface Message

The native control-channel message contains:

```text
Mach message header
Mach message body
IOSurface Mach port descriptor
surface width
surface height
```

The Java/JNI receiver validates the incoming message before using it.

The POC checks properties including:

- expected message ID;
- complex-message flag;
- descriptor count;
- descriptor type;
- transferred port disposition; and
- message size.

The width and height travel with the native resource so Java receives a descriptor containing both:

```text
native IOSurface handle
width
height
```

This becomes the Java-side `IOSurfaceDescriptor`.

### 5.4 Persistent Channel Lifetime

The control channel belongs to the renderer session.

Its lifetime is therefore conceptually:

```text
renderer session starts
        ↓
create control channel
        ↓
transfer receive right to Java
        ↓
surface A
        ↓
surface B
        ↓
surface C
        ↓
surface D
        ↓
...
        ↓
renderer session stops
        ↓
destroy control channel
```

It does not belong to any individual IOSurface.

This distinction is critical.

The POC originally violated this rule and consequently supported only one successful replacement. That bug is documented separately in Section 13, but the resulting ownership rule should be considered part of the architecture:

```text
surface lifetime < renderer-session lifetime

control-channel lifetime = renderer-session lifetime
```

### 5.5 Repeated Transfer Was Explicitly Proven

The POC did not stop after demonstrating one replacement.

During interactive resize testing, the browser generated many changing physical viewport dimensions.

After fixing control-channel ownership, the POC successfully performed repeated sequences equivalent to:

```text
send surface B
receive B
promote B

send surface C
receive C
promote C

send surface D
receive D
promote D

send surface E
receive E
promote E
```

This was repeated dozens of times during a single Java renderer session.

The successful sequence observed repeatedly was:

```text
Sent JScene3D replacement surface: <width>x<height>
JNI received control-channel IOSurface: <width>x<height>
Promoted JScene3D surface: <width>x<height>
```

This proves that the channel is genuinely reusable rather than merely sufficient for a one-time handoff.

### 5.6 Mach Right Ownership During Surface Transfer

Each replacement IOSurface has its own transferable Mach send right.

That right is distinct from the persistent control-channel right.

Conceptually:

```text
persistent control-channel send right
        │
        │ transports
        ▼
per-surface IOSurface send right
```

The Java receiver extracts the IOSurface port from the message and performs:

```c
IOSurfaceLookupFromMachPort(...)
```

After obtaining the `IOSurfaceRef`, the received Mach port right can be deallocated according to the native ownership rules.

This does not close the persistent control channel.

The distinction between:

```text
message transport right
```

and:

```text
resource right transported by the message
```

must remain explicit in the production implementation.

### 5.7 Relationship to Renderer Commands

The POC does not rely on the Mach message alone to perform a complete surface transition.

The session performs two coordinated operations:

```text
1. transfer replacement IOSurface through Mach channel

2. send RECEIVE_SURFACE through renderer command protocol
```

Java responds to `RECEIVE_SURFACE` by waiting for and consuming the corresponding native control-channel message.

After Java has successfully rebound its render target, it emits:

```text
SURFACE_READY width height
```

The complete protocol is therefore:

```text
Electron
    │
    ├── create pending surface B
    │
    ├── transfer B over Mach channel
    │
    └── send RECEIVE_SURFACE
              │
              ▼
Java
    │
    ├── receive Mach message
    ├── look up IOSurface B
    ├── replace OpenGL render target
    └── emit SURFACE_READY width height
              │
              ▼
Electron
    │
    ├── validate acknowledgement
    └── promote B to active
```

This explicit acknowledgement is important because successful Mach delivery alone does not prove that Java successfully adopted the new rendering target.

### 5.8 Failure Diagnosis Added During the POC

During development, the native send path was changed to report the actual `mach_msg()` result rather than simply returning `false`.

This proved important when repeated surface transfer initially failed.

The diagnostic produced:

```text
JScene3D control send failed: 0x10000003
```

which identified:

```text
MACH_SEND_INVALID_DEST
```

That immediately narrowed the problem to the lifetime of the Java-side receive right.

Production code should preserve useful native error reporting around Mach operations rather than reducing failures to generic boolean results without diagnostic context.

The exact logging mechanism can change, but native Mach failure codes are valuable diagnostic information.

### 5.9 Session Recreation

The POC also proved that the control channel can be completely destroyed with one renderer session and recreated for another renderer inside the same Electron process.

The tested lifecycle was:

```text
renderer A
    ↓
control channel A
    ↓
multiple surface transfers
    ↓
QUIT renderer A
    ↓
destroy session A
    ↓
launch renderer B
    ↓
create control channel B
    ↓
Mach rendezvous B
    ↓
renderer B receives new control-channel receive right
    ↓
surface transfer continues
```

This succeeded.

There is therefore no architectural requirement for the Mach control channel to be process-global or application-lifetime state.

It naturally belongs to a renderer session.

### 5.10 Production Direction

The Code OSS implementation should preserve the conceptual distinction between:

```text
bootstrap resource transfer
    Mach rendezvous during child launch

ongoing native resource transfer
    persistent per-renderer control channel

ordinary renderer commands/events
    renderer protocol
```

The exact production classes and APIs may change, but collapsing these three responsibilities into a single mechanism would lose useful lifecycle and ownership boundaries established by the POC.

## 6. Java and JNI Rendering Architecture

The Java side of the POC is responsible for the actual OpenGL rendering while JNI provides the macOS-specific bridge required to acquire, bind, replace, and release IOSurfaces.

The principal Java POC sources are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceBridge.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceDescriptor.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/RendererProtocol.java
```

The principal JNI/native sources are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.c
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.c
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.c
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.h
```

The POC Java renderer is intentionally simple. `CubeRenderer` exists to exercise the rendering architecture rather than to represent the eventual production JScene3D renderer design.

### 6.1 IOSurfaceBridge

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceBridge.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c
```

`IOSurfaceBridge` provides the Java-facing JNI boundary for the macOS IOSurface functionality required by the POC.

Its responsibilities include:

- acquiring the initial IOSurface through the parent Electron process's Mach rendezvous;
- establishing the persistent control-channel receive right during bootstrap;
- binding an IOSurface to the current OpenGL texture;
- receiving replacement IOSurfaces through the control channel; and
- releasing individual IOSurface references.

Conceptually, ordinary Java code sees operations equivalent to:

```text
lookup(...)
bindToTexture(...)
receiveSurface()
release(...)
```

The JNI layer hides:

```text
Mach rendezvous
Mach port ownership
IOSurfaceRef
IOSurfaceLookupFromMachPort
IOSurface texture binding
control-channel receive mechanics
```

from the higher-level Java renderer.

This is an important production boundary.

The JScene3D renderer should operate in terms of rendering surfaces or render targets rather than embedding macOS Mach and IOSurface mechanics throughout normal scene code.

### 6.2 IOSurfaceDescriptor

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceDescriptor.java
```

A replacement surface needs more information than an opaque native handle.

The POC represents an incoming surface using a descriptor containing:

```text
native surface handle
width
height
```

Conceptually:

```text
IOSurfaceDescriptor
    ├── handle
    ├── width
    └── height
```

The dimensions accompany the native surface through the control-channel message and are returned to Java with the received surface.

This allows the OpenGL render target to reconfigure all size-dependent resources when adopting a replacement surface.

The production type does not have to retain this exact name, but native surface identity and dimensions should remain explicit data rather than being inferred indirectly.

### 6.3 IOSurfaceRenderTarget

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java
```

`IOSurfaceRenderTarget` encapsulates the Java/OpenGL resources associated with the current shared rendering surface.

It owns:

```text
current IOSurface native handle
current width
current height

OpenGL texture
OpenGL framebuffer
OpenGL depth renderbuffer
```

The initial construction sequence is conceptually:

```text
receive initial IOSurface
        ↓
generate OpenGL texture
        ↓
generate framebuffer
        ↓
generate depth renderbuffer
        ↓
bind IOSurface as texture storage
        ↓
attach texture to framebuffer
        ↓
allocate depth buffer
        ↓
configure viewport
        ↓
configure projection
        ↓
validate framebuffer completeness
```

The color attachment therefore writes directly into the IOSurface that Electron/Chromium can subsequently present.

### 6.4 OpenGL Binding

The native bridge binds the IOSurface to the OpenGL texture.

Java establishes the OpenGL texture and framebuffer state, while JNI performs the platform-specific IOSurface/OpenGL binding.

The conceptual boundary is:

```text
Java
    owns OpenGL render-target structure

JNI/macOS
    connects IOSurface storage to OpenGL texture
```

This keeps most OpenGL lifecycle management in Java while isolating the native platform operation that LWJGL does not directly provide for this use case.

### 6.5 Depth Buffer

The IOSurface provides the shared color storage required for Chromium presentation.

The depth buffer does not need to be shared with Chromium.

The POC therefore creates an ordinary OpenGL renderbuffer for depth:

```text
IOSurface-backed texture
        ↓
GL_COLOR_ATTACHMENT0

local renderbuffer
        ↓
GL_DEPTH_ATTACHMENT
```

When the IOSurface dimensions change, the depth renderbuffer storage is recreated for the new dimensions.

This separation should carry naturally into the production renderer: only resources required for presentation need to cross the process boundary.

### 6.6 Surface Replacement

`IOSurfaceRenderTarget` supports replacing its underlying IOSurface without recreating the Java process or OpenGL context.

Conceptually:

```text
current surface A
        ↓
receive descriptor for B
        ↓
retain A temporarily
        ↓
bind B to existing OpenGL texture
        ↓
reattach/reconfigure framebuffer resources
        ↓
resize depth storage
        ↓
update viewport
        ↓
update projection
        ↓
B becomes current surface
        ↓
release A
```

The OpenGL texture object, framebuffer object, and renderer process survive the transition.

The backing IOSurface changes.

This was repeatedly exercised during interactive viewport resizing while the cube continued to animate.

### 6.7 Viewport and Projection Reconfiguration

A surface replacement may change both resolution and aspect ratio.

`IOSurfaceRenderTarget` therefore reconfigures the OpenGL viewport whenever a surface is bound:

```java
glViewport(
        0,
        0,
        width,
        height);
```

It also recalculates the projection from the new aspect ratio:

```java
double aspect =
        (double) width / height;

glFrustum(
        -aspect,
        aspect,
        -1.0,
        1.0,
        1.5,
        20.0);
```

This became relevant when the browser viewport was substantially taller than the original `800×600` POC surface.

The renderer must treat surface dimensions as part of render-target state, not merely as metadata used by Chromium.

### 6.8 CubeRenderer

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

`CubeRenderer` is the POC renderer process entry point.

Its responsibilities include:

- validating process arguments;
- acquiring the initial IOSurface;
- initializing GLFW;
- creating a hidden OpenGL context;
- creating LWJGL OpenGL capabilities;
- constructing `IOSurfaceRenderTarget`;
- announcing renderer readiness;
- reading renderer commands;
- applying pointer-drag rotation;
- advancing automatic rotation;
- rendering frames;
- receiving replacement surfaces; and
- shutting down when `QUIT` is received.

The cube itself has no architectural significance.

It was useful because it made several rendering failures visually obvious:

- missing frames;
- incorrect orientation;
- broken interaction;
- aspect-ratio distortion;
- resize failures; and
- renderer restart failures.

The final POC continuously rotates the cube so that sustained frame transport is visible without requiring user interaction.

### 6.9 Hidden GLFW Context

The Java process creates a GLFW window with visibility disabled.

The GLFW window exists to provide the OpenGL context; it is not presented as a native application window.

Conceptually:

```text
hidden GLFW window
        ↓
OpenGL context
        ↓
IOSurface-backed FBO
        ↓
rendering occurs offscreen
```

The visible result is presented exclusively through the Code OSS/Electron browser side.

This avoids trying to embed or position a native Java/GLFW child window inside the editor.

### 6.10 Renderer Readiness

Java does not emit `RENDERER_READY` merely because the JVM started.

The event is emitted only after the rendering environment has successfully reached the point where the initial `IOSurfaceRenderTarget` exists.

Conceptually:

```text
JVM starts
    ↓
Mach rendezvous succeeds
    ↓
initial IOSurface acquired
    ↓
GLFW initialized
    ↓
OpenGL context created
    ↓
LWJGL capabilities created
    ↓
IOSurfaceRenderTarget created
    ↓
RENDERER_READY
```

This provides Electron with a meaningful lifecycle boundary.

### 6.11 Frame Rendering

The final POC does not run an independent unbounded Java animation loop.

Java renders when it receives:

```text
FRAME <frameNumber>
```

For the continuous-animation test, each frame request advances the cube rotation before rendering.

After rendering and flushing OpenGL, Java emits:

```text
FRAME_READY
```

Electron does not request the next frame until the current shared texture has been presented.

This arrangement makes Java part of a back-pressured rendering pipeline rather than an independent producer.

The production JScene3D renderer may eventually require a more sophisticated frame scheduler, but the POC proved that this coordinated model works.

### 6.12 Surface Ownership and Lifetime

The most important Java/native ownership rule established during the POC is that the following lifetimes are independent:

```text
individual IOSurface
control channel
OpenGL context
Java renderer process
```

An individual IOSurface can be replaced and released while the control channel and OpenGL context remain alive.

Conceptually:

```text
renderer process
│
├── OpenGL context
│
├── control channel
│
├── surface A
│     ↓ released
├── surface B
│     ↓ released
├── surface C
│     ↓ released
└── ...
```

The control channel is closed only as part of renderer/session teardown, not as a side effect of releasing an individual surface.

This rule was learned through an actual POC failure and must be preserved.

### 6.13 Native Cleanup

When an individual surface is no longer required, Java calls the JNI bridge to release its `IOSurfaceRef`.

When the render target itself closes, it also deletes its OpenGL resources:

```text
depth renderbuffer
framebuffer
texture
```

Renderer shutdown subsequently destroys the GLFW/OpenGL environment.

The production implementation should make these ownership boundaries similarly explicit and preferably deterministic.

### 6.14 Production Direction

The POC demonstrates a useful Java-side separation:

```text
renderer/application logic
        ↓
render-target abstraction
        ↓
JNI platform bridge
        ↓
macOS IOSurface/Mach implementation
```

The production implementation should adapt this to the existing JScene3D Maven/module architecture rather than introducing a separate `poc` package structure.

The key design principle to preserve is that JScene3D rendering code should not need to understand Electron, Chromium SharedTexture, Mach rendezvous, or Mach message mechanics.

Those concerns belong at the renderer integration and platform boundaries.

## 7. Renderer Protocol and Lifecycle

The POC uses a small bidirectional protocol between the Electron browser process and the Java renderer process.

The protocol serves a different purpose from the Mach control channel:

```text
renderer protocol
    commands, acknowledgements and lifecycle events

Mach control channel
    transfer of native resource rights
```

The two mechanisms cooperate during operations such as surface replacement but should remain conceptually separate.

The principal protocol definitions are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/RendererProtocol.java
```

The Electron process transport implementation is:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc
```

The Java command handling can be inspected in:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

### 7.1 Protocol Vocabulary

The final POC protocol is conceptually:

#### Electron → Java

```text
DRAG <dx> <dy>

FRAME <frameNumber>

RECEIVE_SURFACE

QUIT
```

#### Java → Electron

```text
RENDERER_READY

FRAME_READY

SURFACE_READY <width> <height>
```

Each message has a specific lifecycle meaning.

`DRAG` modifies scene state using accumulated pointer movement.

`FRAME` asks Java to render the next frame.

`RECEIVE_SURFACE` tells Java to consume a replacement IOSurface from the native Mach control channel.

`QUIT` requests graceful renderer shutdown.

`RENDERER_READY` indicates that Java has completed renderer initialization and can accept rendering and surface-management operations.

`FRAME_READY` indicates that Java has finished rendering the requested frame into the current IOSurface.

`SURFACE_READY` indicates that Java has successfully received and adopted a replacement IOSurface.

### 7.2 Protocol Constants

The earliest POC implementation used hard-coded protocol strings directly in Java and C++.

That became inappropriate once the protocol started representing a real interface between the two processes.

The vocabulary was therefore centralized into:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h
```

and:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/RendererProtocol.java
```

The important lesson is not that the production implementation must duplicate constants in exactly this form.

The lesson is that protocol vocabulary is part of an interface and should be explicit rather than represented by scattered string literals.

The production implementation should consider whether a stronger protocol representation is appropriate.

Possible production directions include:

```text
typed command/event model
structured messages
versioned protocol
generated/shared protocol definitions
```

That decision was deliberately not made in the POC.

### 7.3 Transport

The POC uses the Java child process's standard streams for the ordinary renderer protocol.

Electron writes commands to Java stdin.

Java writes events to stdout.

Conceptually:

```text
Electron
    │
    │ stdin
    ▼
Java

Java
    │
    │ stdout
    ▼
Electron
```

`JScene3DRendererProcess` owns the pipe mechanics.

Java stdout is asynchronous from Electron's perspective, so the native process layer:

1. watches the stdout file descriptor;
2. reads available bytes;
3. appends them to a buffer;
4. identifies complete newline-terminated messages;
5. retains incomplete trailing data for the next read; and
6. forwards complete lines to `JScene3DRendererSession`.

The session then interprets the protocol semantics.

This separation prevents process-I/O mechanics from becoming entangled with renderer state.

### 7.4 Process Launch Is Not Renderer Readiness

An important lifecycle distinction discovered during the POC is:

```text
Java process launched
        !=
renderer ready
```

A valid Java PID only proves that the child process was created.

Java still has to perform:

```text
Mach rendezvous
        ↓
initial IOSurface lookup
        ↓
GLFW initialization
        ↓
hidden OpenGL context creation
        ↓
LWJGL capability initialization
        ↓
IOSurfaceRenderTarget construction
        ↓
OpenGL/IOSurface binding
```

Only after those steps succeed does Java emit:

```text
RENDERER_READY
```

Electron should not send frame requests or initiate surface replacement before this acknowledgement.

### 7.5 Startup Resize Race

This readiness distinction became necessary when browser-driven sizing was introduced.

The browser could determine its actual physical rendering dimensions almost immediately.

For example:

```text
initial native surface
    800x600

actual browser canvas on Retina display
    2200x1536
```

Before `RENDERER_READY` existed, Electron could attempt to transfer the `2200x1536` replacement while Java was still acquiring and initializing the original `800x600` surface.

That happened to work during one test, but it represented an invalid lifecycle dependency on timing.

The corrected sequence is:

```text
launch Java
        ↓
browser determines desired size
        ↓
retain desired size
        ↓
Java initializes initial render target
        ↓
RENDERER_READY
        ↓
apply desired resize
```

The Code OSS implementation should preserve this ordering explicitly rather than relying on process-start timing.

### 7.6 Callback Registration Race

The POC also identified a race on the Electron side.

An intermediate API performed:

```text
launchRenderer(...)
        ↓
setRendererReadyCallback(...)
        ↓
setFrameReadyCallback(...)
```

That creates a theoretical window in which Java could emit `RENDERER_READY` before Electron had registered the callback.

The final POC changed the launch operation so the callbacks are supplied before process launch.

Conceptually:

```text
create renderer session
        ↓
install RENDERER_READY callback
        ↓
install FRAME_READY callback
        ↓
launch Java
```

Only then can Java produce renderer events.

The exact production API does not have to use JavaScript callbacks in this form, but the lifecycle invariant should remain:

```text
event consumers must exist before event producers can start
```

### 7.7 Frame Lifecycle

The frame protocol is deliberately request/acknowledgement based.

Electron sends:

```text
FRAME n
```

Java renders exactly one frame and responds:

```text
FRAME_READY
```

Electron then presents the shared surface through Chromium.

Only after presentation completes does Electron request another frame.

Conceptually:

```text
FRAME 1
    ↓
Java renders
    ↓
FRAME_READY
    ↓
Electron presents frame
    ↓
presentation completes
    ↓
FRAME 2
```

This forms the basis of the POC's frame back-pressure mechanism.

Continuous rendering therefore does not mean Java runs an independent unrestricted rendering loop.

### 7.8 Surface Replacement Lifecycle

Surface replacement uses both communication mechanisms.

The native resource is transferred over Mach:

```text
IOSurface B Mach right
        ↓
Mach control channel
```

The semantic operation is coordinated through the renderer protocol:

```text
RECEIVE_SURFACE
```

Java receives the native resource, replaces its render target and then sends:

```text
SURFACE_READY <width> <height>
```

Electron validates the reported dimensions against the pending surface.

Only after successful validation does the pending surface become active.

Conceptually:

```text
Electron                           Java
   │                                │
   │ create surface B               │
   │                                │
   │── Mach right for B ───────────>│
   │                                │
   │── RECEIVE_SURFACE ────────────>│
   │                                │
   │                         receive B
   │                         bind B
   │                         configure GL
   │                                │
   │<── SURFACE_READY w h ──────────│
   │                                │
validate dimensions                 │
   │                                │
promote B                           │
```

This acknowledgement boundary should be retained in production.

### 7.9 Renderer Shutdown

Graceful shutdown is initiated with:

```text
QUIT
```

Java exits its command-processing loop.

The Java renderer then unwinds its resources:

```text
command loop exits
        ↓
IOSurfaceRenderTarget closes
        ↓
current IOSurface released
        ↓
OpenGL resources deleted
        ↓
GLFW window/context destroyed
        ↓
GLFW terminated
        ↓
Java process exits
```

The POC verified that the Java child disappears after the Electron window is closed.

### 7.10 Renderer Recreation

The protocol and lifecycle were also tested across complete renderer replacement.

The sequence was:

```text
launch renderer 1
        ↓
RENDERER_READY
        ↓
continuous frames
        ↓
QUIT
        ↓
destroy renderer session
        ↓
launch renderer 2
        ↓
new Mach rendezvous
        ↓
new IOSurface
        ↓
new control channel
        ↓
RENDERER_READY
        ↓
restore desired viewport dimensions
        ↓
continuous frames resume
```

This succeeded within one Electron process.

The renderer lifecycle should therefore be modeled as restartable rather than assuming that one Java renderer exists for the entire Electron application lifetime.

### 7.11 POC Protocol Limitations

The line-oriented stdin/stdout protocol was appropriate for proving the architecture, but it should not automatically become the production protocol.

Questions that remain for the Code OSS implementation include:

- whether stdin/stdout remains an appropriate transport;
- whether messages should become structured;
- whether commands need correlation IDs;
- whether `FRAME_READY` should include the corresponding frame number;
- whether errors need explicit protocol messages;
- whether protocol version negotiation is required;
- how unexpected renderer termination should be reported;
- how renderer startup failures should be represented;
- whether surface-transition acknowledgements need stronger identity than dimensions alone; and
- whether multiple renderer/editor sessions require explicit session identifiers.

These are production design questions, not unresolved feasibility questions.

The POC proved that the required lifecycle can be implemented; it did not attempt to define the final production protocol.

## 8. Frame Rendering and Back-Pressure

The final POC continuously renders an animated scene while maintaining explicit back-pressure between Java rendering and Chromium presentation.

This was an important extension beyond the earlier interaction-driven POC because it exercised the complete rendering path continuously rather than only when pointer input occurred.

The principal POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js
```

### 8.1 Why Continuous Rendering Was Added

The earlier POC rendered frames only when Electron explicitly requested them in response to activity such as pointer dragging.

That proved:

```text
input
    ↓
Java scene update
    ↓
OpenGL rendering
    ↓
IOSurface
    ↓
Chromium presentation
```

but did not stress the architecture as a sustained rendering pipeline.

The final POC therefore made the cube rotate continuously.

This was not added merely as a visual effect. It was intended to prove that the architecture remains stable while repeatedly performing:

```text
Java rendering
        ↓
IOSurface update
        ↓
FRAME_READY
        ↓
SharedTexture import
        ↓
browser presentation
        ↓
next frame
```

while interaction and resizing occur concurrently.

### 8.2 One Frame in Flight

The POC maintains a `frameInFlight` state in the Electron main-side test harness.

Conceptually:

```text
frameInFlight = false
        ↓
send FRAME n
        ↓
frameInFlight = true
        ↓
wait for FRAME_READY
        ↓
present shared texture
        ↓
presentation completes
        ↓
frameInFlight = false
        ↓
request next frame
```

If a frame is already in flight, another frame request is not sent.

This prevents Electron from building an unbounded queue of render requests.

### 8.3 Java Does Not Free-Run

The Java renderer does not independently render as quickly as possible.

Instead, Java waits for:

```text
FRAME <frameNumber>
```

When a frame command arrives, Java:

1. applies any scene changes associated with the next frame;
2. advances the POC cube's automatic rotation;
3. renders into the current IOSurface-backed framebuffer;
4. flushes the OpenGL commands; and
5. emits `FRAME_READY`.

Conceptually:

```text
FRAME n
    ↓
update scene
    ↓
render
    ↓
glFlush()
    ↓
FRAME_READY
```

Java then waits for another command.

This means the renderer is driven by downstream demand rather than operating as an unrestricted producer.

### 8.4 Browser Presentation Is Part of the Back-Pressure Loop

Receiving `FRAME_READY` does not immediately cause Electron to request the next Java frame.

Electron first presents the current IOSurface through Chromium's shared-texture path.

Conceptually:

```text
FRAME_READY
        ↓
obtain current JScene3D surface
        ↓
import SharedTexture
        ↓
send SharedTexture to browser renderer
        ↓
browser consumes VideoFrame
        ↓
WebGPU presentation
        ↓
sendSharedTexture promise completes
        ↓
request next FRAME
```

The next frame therefore depends on completion of the current presentation operation.

The complete loop is:

```text
Electron                         Java
   │                              │
   │──── FRAME n ────────────────>│
   │                              │
   │                         update scene
   │                         render OpenGL
   │                              │
   │<──── FRAME_READY ────────────│
   │
   │ import IOSurface
   │ as SharedTexture
   │
   │ send to browser
   │
   │ await presentation
   │
   │──── FRAME n+1 ──────────────>│
```

### 8.5 Continuous Animation

The final `CubeRenderer` advances one component of cube rotation on each requested frame.

The POC deliberately uses a simple per-frame rotation increment rather than attempting to implement a production-quality time-based animation clock.

That distinction matters.

The purpose of the animation was to stress:

- sustained rendering;
- sustained Java-to-Chromium frame transfer;
- repeated shared-texture import/release;
- frame ordering;
- interaction during continuous rendering;
- resize during continuous rendering; and
- surface replacement while rendering remains active.

The exact animation timing mechanism has no production significance.

### 8.6 Interaction During Continuous Rendering

Pointer dragging remains functional while automatic rotation is active.

Browser pointer events accumulate drag deltas.

The next available frame consumes those deltas before rendering.

Conceptually:

```text
pointer event
    ↓
accumulate dx/dy
    ↓
current frame continues
    ↓
next frame becomes available
    ↓
send DRAG accumulatedDx accumulatedDy
    ↓
send FRAME
```

This avoids generating additional overlapping frame requests merely because multiple pointer events arrive while another frame is already in flight.

The POC successfully demonstrated dragging the cube while it continued to rotate automatically.

### 8.7 Resizing During Continuous Rendering

The POC also tested surface replacement while the continuous frame loop was active.

The observed behavior was:

```text
continuous frames on surface A
        ↓
viewport resize
        ↓
create and transfer surface B
        ↓
Java adopts B
        ↓
SURFACE_READY
        ↓
Electron promotes B
        ↓
continuous frames continue on B
```

This was repeated across several surface sizes during a single running renderer.

The cube continued rotating and remained interactive after each transition.

This is important because it demonstrates that surface replacement does not require suspending and recreating the entire Java renderer.

### 8.8 Shared Surface and Frame Identity

The POC increments a frame number for each `FRAME` request and also uses that value as the timestamp supplied when importing the current shared texture.

However, the current POC `FRAME_READY` response does not include the frame number.

This was sufficient for the single-frame-in-flight test because there can only be one outstanding frame.

For production, frame identity should be considered explicitly.

A stronger protocol could use:

```text
FRAME 123
        ↓
FRAME_READY 123
```

or another correlation mechanism.

This would make diagnostics and future pipeline changes safer if the rendering model ever allows more than one operation to be outstanding.

The POC does not establish the final answer here.

### 8.9 Failure Behavior

If a Java frame command cannot be sent, the POC clears `frameInFlight` and treats the operation as a failure.

If shared-texture presentation fails, the POC also clears the in-flight state and logs the presentation error.

This was sufficient for experimentation.

Production needs an explicit policy for failures such as:

```text
renderer process exits
OpenGL render failure
SharedTexture import failure
browser presentation failure
surface becomes invalid
protocol failure
```

Those conditions should not be hidden inside the frame scheduler.

They need to feed into the renderer/session lifecycle and editor error handling.

### 8.10 Back-Pressure Is a Proven Concept, Not Necessarily the Final Scheduler

The POC proves that an explicitly back-pressured frame pipeline works with the Java/OpenGL → IOSurface → Chromium/WebGPU architecture.

It does not prove that the exact POC scheduling policy is optimal for the production editor.

Production may need to consider:

- display refresh rate;
- visibility of the editor;
- inactive/background editor tabs;
- scene dirtiness;
- editor occlusion;
- user interaction latency;
- animation timing;
- frame dropping;
- multiple JScene3D editors;
- renderer CPU/GPU load; and
- whether rendering should be continuous or demand-driven for a particular scene.

The important architectural result is that Java does not need to push uncontrolled frames toward Chromium.

The consumer side can participate in pacing rendering.

### 8.11 Result Proven by the POC

The final POC successfully sustained the following simultaneously:

```text
continuous Java/OpenGL rendering
        +
IOSurface-backed rendering
        +
Chromium SharedTexture presentation
        +
WebGPU display
        +
pointer interaction
        +
dynamic viewport resizing
        +
repeated IOSurface replacement
```

The frame transport remained stable under this combined workload.

The Code OSS implementation can therefore treat sustained rendering through the shared-surface path as proven and concentrate on designing the production frame scheduler and editor lifecycle around that mechanism.

## 9. Chromium SharedTexture and WebGPU Presentation

The browser-facing half of the POC takes the IOSurface rendered by Java and presents it inside Chromium without copying the rendered pixels through JavaScript or CPU memory.

The principal POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/index.html
```

The presentation path is conceptually:

```text
Java renders into IOSurface
        ↓
FRAME_READY
        ↓
Electron obtains active JScene3DSurface
        ↓
convert IOSurface to Chromium-compatible handle
        ↓
sharedTexture.importSharedTexture(...)
        ↓
sharedTexture.sendSharedTexture(...)
        ↓
Chromium renderer receives imported texture
        ↓
getVideoFrame()
        ↓
WebGPU importExternalTexture(...)
        ↓
draw fullscreen geometry
        ↓
canvas
```

### 9.1 Exposing the Active Native Surface

The native Electron binding exposes the currently active `JScene3DSurface`.

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc
```

The JavaScript-facing surface information contains:

```text
native shared-texture handle
width
height
```

Conceptually:

```javascript
{
    handle,
    width,
    height
}
```

Returning the dimensions with the handle became important once dynamic surface replacement was introduced.

The browser-side shared-texture import must describe the actual active surface dimensions rather than assuming the original `800×600` size.

### 9.2 SharedTexture Import

The Electron main-side POC imports the active native surface using Electron's `sharedTexture` API.

Conceptually:

```javascript
const texture =
    sharedTexture.importSharedTexture({
        textureInfo: {
            pixelFormat: "bgra",
            codedSize: {
                width: surface.width,
                height: surface.height
            },
            visibleRect: {
                x: 0,
                y: 0,
                width: surface.width,
                height: surface.height
            },
            timestamp: frameNumber,
            handle: {
                ioSurface: surface.handle
            }
        }
    });
```

The important point is that this imports the IOSurface-backed resource into Chromium's shared-texture infrastructure.

It does not copy Java's rendered framebuffer into a JavaScript pixel buffer.

### 9.3 Sending the Shared Texture to the Renderer

Once imported, the POC sends the shared texture to the Chromium renderer associated with the test window.

Conceptually:

```javascript
await sharedTexture.sendSharedTexture({
    frame: window.webContents.mainFrame,
    importedSharedTexture: texture
});
```

The imported texture is explicitly released afterward.

The presentation lifecycle is therefore:

```text
import native shared resource
        ↓
send to Chromium renderer
        ↓
await completion
        ↓
release imported SharedTexture wrapper
```

The underlying IOSurface remains owned by the JScene3D renderer session.

The imported SharedTexture object represents a presentation/import lifetime, not ownership of the JScene3D surface itself.

### 9.4 Preload Boundary

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js
```

The POC uses the preload layer to expose a deliberately small JScene3D API to the browser page while keeping:

```text
contextIsolation: true
nodeIntegration: false
```

The preload registers the shared-texture receiver.

When Chromium delivers an imported shared texture, the preload obtains a `VideoFrame` from it.

Conceptually:

```javascript
sharedTexture.setSharedTextureReceiver(async (data) => {
    const { importedSharedTexture } = data;

    const frame =
        importedSharedTexture.getVideoFrame();

    await callback(frame);

    frame.close();
    importedSharedTexture.release();
});
```

The exact production boundary may differ depending on how the Code OSS editor contribution is implemented.

The POC nevertheless establishes an important separation:

```text
Electron/native resource handling
        ↓
controlled preload/API boundary
        ↓
browser/editor presentation
```

The browser editor does not need direct access to IOSurface or Mach APIs.

### 9.5 VideoFrame

The shared texture becomes usable by the browser renderer through a `VideoFrame`.

The `VideoFrame` is not converted into an image bitmap or CPU pixel array.

Instead, it becomes the source for a WebGPU external texture.

Conceptually:

```javascript
const externalTexture =
    device.importExternalTexture({
        source: frame
    });
```

This preserves the GPU-oriented presentation path.

The frame is closed after the browser-side callback has finished using it.

Resource lifetime matters here:

```text
ImportedSharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture usage
        ↓
finish browser rendering
        ↓
close VideoFrame
        ↓
release ImportedSharedTexture
```

Production code should preserve explicit resource lifetime management rather than relying on eventual garbage collection for these per-frame resources.

### 9.6 WebGPU Pipeline

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js
```

The POC creates a small WebGPU pipeline whose only purpose is to present the externally supplied texture.

The fragment stage samples:

```text
texture_external
```

using:

```wgsl
textureSampleBaseClampToEdge(...)
```

The renderer draws fullscreen geometry covering the canvas.

The browser renderer is therefore not reproducing the JScene3D scene.

Its role is only:

```text
receive rendered external texture
        ↓
sample texture
        ↓
present it in editor/browser surface
```

All 3D scene rendering remains on the Java/OpenGL side.

### 9.7 Removal of Fixed 800×600 Shader Assumption

An earlier version of the POC derived texture coordinates from fragment coordinates using hard-coded `800×600` dimensions.

That became incorrect as soon as dynamic viewport resizing was introduced.

The final POC instead supplies normalized texture coordinates from the vertex shader.

Conceptually:

```text
fullscreen vertex
        +
normalized UV coordinate
        ↓
fragment shader
        ↓
sample external texture
```

This makes the WebGPU presentation independent of the current IOSurface dimensions.

The native surface can therefore transition between sizes such as:

```text
800×600
2200×1536
1880×982
1822×858
2122×1192
```

without requiring shader constants to change.

### 9.8 Canvas Sizing

The original test page also fixed the canvas CSS dimensions at:

```text
800×600
```

This initially hid the fact that BrowserWindow resizing was not actually changing the rendering element.

The final POC page allows the canvas to fill its available content area.

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/index.html
```

Conceptually:

```css
html,
body {
    width: 100%;
    height: 100%;
}

canvas {
    display: block;
    width: 100%;
    height: 100%;
}
```

This matters for Code OSS because the render surface should follow the actual editor viewport, not an outer window dimension or fixed initial size.

The production implementation should derive render-target dimensions from the element that actually presents the JScene3D scene.

### 9.9 Presentation and Surface Promotion

The browser presentation path always obtains the current active surface from the renderer session.

During resize, a replacement surface is not exposed as active merely because it exists.

The sequence remains:

```text
active A
pending B
        ↓
Java adopts B
        ↓
SURFACE_READY B
        ↓
Electron promotes B
        ↓
getSurface() now returns B
```

This means the SharedTexture path follows the session's acknowledged surface state.

The browser side does not need to understand the pending-surface transition itself.

### 9.10 Per-Frame Resource Lifetime

The POC explicitly releases temporary presentation resources.

On the Electron side:

```text
ImportedSharedTexture
    released after sendSharedTexture completes
```

On the browser side:

```text
VideoFrame
    closed after WebGPU use

received ImportedSharedTexture
    released after callback completes
```

The long-lived IOSurface is separate from these temporary per-frame wrappers.

Conceptually:

```text
JScene3DSurface / IOSurface
    long-lived across many frames

ImportedSharedTexture
    per presentation

VideoFrame
    per presentation

WebGPU external texture
    valid for presentation usage
```

Production code should keep these lifetime categories distinct.

### 9.11 What the Browser Side Does Not Do

The POC browser renderer does not:

- receive raw BGRA byte arrays;
- allocate a CPU framebuffer;
- call into Java directly;
- know about Mach ports;
- know about IOSurface ownership;
- manage OpenGL resources;
- recreate the 3D scene;
- perform Java renderer lifecycle management; or
- decide which pending IOSurface becomes active.

Those responsibilities remain outside the editor presentation layer.

This is a useful boundary for the Code OSS integration.

### 9.12 Code OSS Direction

The standalone POC uses:

```text
index.html
preload.js
renderer.js
test-jscene3d-binding.js
```

because that was the simplest environment in which to prove the Chromium presentation path.

These files are not a proposed Code OSS editor architecture.

The Code OSS implementation should instead identify the appropriate existing editor/view contribution and lifecycle mechanisms while preserving the proven data path:

```text
active native JScene3D surface
        ↓
Chromium SharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture
        ↓
JScene3D editor viewport
```

The POC proves that this presentation mechanism works. The remaining task is to integrate it into the Code OSS editor architecture cleanly rather than reproduce the standalone test page inside Code OSS.

## 10. Input and Interaction

The POC proves that interaction originating in the Chromium renderer can affect the Java-rendered JScene3D scene while continuous rendering is active.

The principal POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

The POC uses pointer dragging to rotate the cube.

The cube interaction itself is only a test mechanism. The architectural result is that browser/editor input can cross the process boundary, modify Java-side scene state, and be reflected in subsequent shared-surface frames.

### 10.1 Input Path

The proven interaction path is:

```text
browser pointer event
        ↓
renderer.js
        ↓
preload API
        ↓
Electron IPC
        ↓
Electron main-side renderer coordination
        ↓
Java renderer command
        ↓
Java scene state
        ↓
next rendered frame
        ↓
IOSurface
        ↓
Chromium presentation
```

This establishes bidirectional communication around the rendering pipeline:

```text
Java → browser
    rendered frames

browser → Java
    interaction and control
```

The rendered image itself still travels through the shared GPU resource rather than through the control path.

### 10.2 Browser Pointer Handling

The browser-side POC tracks pointer dragging on the canvas.

Conceptually:

```text
pointerdown
    ↓
begin drag
    ↓
remember pointer position

pointermove
    ↓
calculate dx/dy
    ↓
accumulate/send drag delta

pointerup / pointercancel
    ↓
end drag
```

Pointer capture is used so an active drag remains associated with the canvas even when the pointer moves during the gesture.

The browser therefore deals in ordinary editor-style input concepts rather than Java/AWT/native-window events.

This is significant for Code OSS because the visible JScene3D viewport remains a normal browser/editor surface from the UI's perspective.

### 10.3 Preload Boundary

The POC does not expose Electron internals directly to the browser page.

The preload provides the browser-facing JScene3D operations required by the test, including interaction and resize forwarding.

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js
```

The production Code OSS implementation may use a different internal API boundary, but it should preserve the principle that the editor-facing layer interacts with a controlled JScene3D capability rather than arbitrary Electron/native APIs.

### 10.4 Drag Delta Accumulation

Once continuous rendering was introduced, the POC stopped treating every pointer movement as a reason to request another frame.

Instead, drag movement is accumulated.

Conceptually:

```text
pointermove
    ↓
pendingDx += dx
pendingDy += dy
```

When the next frame can be requested:

```text
read pendingDx/pendingDy
        ↓
reset accumulated values
        ↓
send DRAG dx dy
        ↓
send FRAME n
```

This is important because browser pointer events can arrive much faster than complete Java → IOSurface → Chromium frame cycles.

Without accumulation, the system could generate a large queue of small input commands or unnecessary frame requests.

### 10.5 Input and Frame Back-Pressure

The final POC therefore combines interaction with the frame scheduler.

Conceptually:

```text
frame n currently in flight
        │
        ├── pointermove
        │      ↓
        │   accumulate delta
        │
        ├── pointermove
        │      ↓
        │   accumulate delta
        │
        └── frame n presentation completes
                   ↓
             consume accumulated delta
                   ↓
             send DRAG
                   ↓
             request frame n+1
```

The scene receives the latest accumulated interaction without violating the one-frame-in-flight policy.

This worked while the cube was also rotating automatically.

### 10.6 Java-Side Interaction

The Java POC renderer interprets:

```text
DRAG <dx> <dy>
```

and updates its rotation state.

Conceptually:

```java
rotationY += dx * DRAG_SENSITIVITY;
rotationX += dy * DRAG_SENSITIVITY;
```

The subsequent frame renders using the updated state.

The important point is that the browser does not transform the rendered image to simulate 3D interaction.

The actual Java/JScene3D scene state changes.

This validates the architecture needed for future editor operations such as:

```text
camera orbit
camera pan
camera zoom
selection
gizmo manipulation
scene picking
tool interaction
keyboard navigation
```

The POC does not implement those operations; it proves the communication path they can use.

### 10.7 Interaction Survives Surface Replacement

Pointer interaction was tested while dynamically resizing the browser window.

During resize, the renderer can transition:

```text
surface A
    ↓
surface B
    ↓
surface C
```

while Java's scene state remains alive.

The Java process and OpenGL context are not recreated for an ordinary surface replacement.

Therefore state such as cube rotation survives the transition.

Conceptually:

```text
scene state
    ├── rotation
    ├── camera
    ├── objects
    └── future editor state

render target
    ├── surface A
    └── replaced by surface B
```

The scene and render-target lifetimes are distinct.

This will be important in the production editor: resizing the JScene3D viewport must not recreate the scene.

### 10.8 Interaction Survives Continuous Rendering

The final POC also verified that manual drag input works while the cube rotates continuously.

This exercises simultaneous:

```text
automatic scene update
        +
user scene update
        +
continuous frame transport
```

The interaction remained responsive and rendering continued afterward.

### 10.9 Renderer Restart and Interaction

The final restart test completely destroyed renderer 1 and launched renderer 2.

After renderer 2 became ready and restored the desired viewport dimensions, the new cube:

```text
rotated continuously
and
could be dragged
```

This proves that the browser-to-Java interaction path can be re-established after renderer recreation.

The POC did not attempt to preserve the old Java scene state across process restart.

Renderer 2 starts with its own new scene state.

Persisting/restoring an actual editor scene across renderer failure or deliberate renderer replacement is a separate production concern.

### 10.10 Input Protocol Is POC-Level

The current input representation:

```text
DRAG dx dy
```

should not be interpreted as the proposed production input API.

A real JScene3D editor will require a richer interaction model.

Potential production concerns include:

- pointer position;
- pointer buttons;
- modifier keys;
- scroll/wheel input;
- keyboard events;
- pointer capture;
- editor focus;
- high-DPI coordinate conversion;
- camera tools;
- selection and picking;
- drag start/update/end semantics;
- command ordering;
- scene mutations;
- tool modes; and
- potentially multiple simultaneous editor views.

These should be designed around actual JScene3D editor requirements.

### 10.11 Coordinate Systems

The POC drag command uses movement deltas and therefore avoids most coordinate-system issues.

Production interaction will likely need explicit coordinates for operations such as picking.

At that point the implementation must distinguish between:

```text
CSS pixels
physical render-surface pixels
normalized viewport coordinates
Java/OpenGL viewport coordinates
scene/world coordinates
```

The POC's device-pixel-ratio work already establishes that CSS and physical surface dimensions are not interchangeable.

The production input design should therefore make coordinate-space semantics explicit rather than passing ambiguous integer positions across the process boundary.

### 10.12 Architectural Result

The POC proves that the Code OSS editor does not need a native Java child window in order to provide interactive JScene3D editing.

The visible editor can remain browser-based:

```text
Code OSS editor viewport
        ↓
browser input events
        ↓
JScene3D renderer integration
        ↓
Java scene
```

while rendered output returns through:

```text
Java/OpenGL
        ↓
IOSurface
        ↓
Chromium SharedTexture
        ↓
WebGPU
        ↓
same editor viewport
```

This allows Code OSS to own the editor UI and interaction surface while Java/JScene3D remains responsible for the actual 3D scene and rendering.

## 11. Dynamic Surface Resizing

A production editor viewport cannot assume a fixed rendering resolution. The visible JScene3D area changes as the user resizes the window, moves editor boundaries, changes layout, maximizes the application, changes display scaling, or moves the application between displays.

The POC therefore evolved from a fixed `800×600` surface into a dynamically replaceable rendering surface.

The principal POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceDescriptor.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

The POC proved that resizing does not require restarting the Java renderer or recreating its OpenGL context.

Instead, the IOSurface backing the render target is replaced.

### 11.1 Surface Replacement Protocol

The core resize lifecycle is:

```text
browser determines desired size
        ↓
Electron creates replacement IOSurface
        ↓
replacement becomes pending surface
        ↓
Electron transfers replacement through Mach control channel
        ↓
Electron sends RECEIVE_SURFACE
        ↓
Java receives IOSurfaceDescriptor
        ↓
Java rebinds IOSurfaceRenderTarget
        ↓
Java emits SURFACE_READY width height
        ↓
Electron validates acknowledgement
        ↓
pending surface becomes active surface
```

This separates three important states:

```text
desired viewport size

active rendering surface

pending replacement surface
```

They are not necessarily identical at every instant.

During a transition:

```text
desired size = B
active surface = A
pending surface = B
```

Only after Java acknowledges B does the state become:

```text
desired size = B
active surface = B
pending surface = none
```

### 11.2 Why Replacement Is Acknowledged

Creating and sending a replacement IOSurface does not prove that Java successfully adopted it.

Possible failures could occur while:

```text
receiving Mach message
looking up IOSurface
binding IOSurface to OpenGL
resizing depth storage
configuring framebuffer
validating framebuffer
```

The POC therefore does not promote the replacement immediately after sending it.

Java first completes the render-target replacement and then emits:

```text
SURFACE_READY <width> <height>
```

Electron verifies those dimensions against the pending surface.

Only then is the pending surface promoted.

This creates an explicit transition boundary:

```text
resource delivered
    !=
resource active
```

That distinction should remain in the production design.

### 11.3 Java Render-Target Replacement

Java receives a replacement as an `IOSurfaceDescriptor` containing:

```text
native IOSurface handle
width
height
```

`IOSurfaceRenderTarget` then rebinds the new surface while preserving the existing Java renderer and OpenGL context.

Conceptually:

```text
OpenGL context
    remains alive

scene state
    remains alive

texture object
    remains alive

framebuffer object
    remains alive

IOSurface backing
    replaced

depth storage
    resized

viewport
    updated

projection
    updated
```

After the new surface is active, Java releases its reference to the previous IOSurface.

This allows ordinary editor resizing to remain a render-target operation rather than a renderer-process lifecycle operation.

### 11.4 Device Pixel Ratio

The browser viewport is measured in CSS pixels, but the IOSurface should represent physical rendering pixels.

The POC calculates:

```text
physicalWidth =
    canvas.clientWidth × window.devicePixelRatio

physicalHeight =
    canvas.clientHeight × window.devicePixelRatio
```

with integer rounding and a minimum dimension of one pixel.

For example, on the Retina development display:

```text
800 × 600 CSS pixels
        ↓
devicePixelRatio = 2
        ↓
1600 × 1200 physical pixels
```

During actual testing, surfaces included dimensions such as:

```text
2200×1536
1880×982
1822×858
2122×1192
1698×2062
```

The important rule is:

```text
CSS viewport dimensions
    !=
native render-surface dimensions
```

The Code OSS implementation should derive the physical render-target size from the actual editor viewport and current device pixel ratio.

This becomes particularly important if the editor can move between displays with different scaling factors.

### 11.5 Actual Editor Element Size Matters

An early POC version had:

```css
canvas {
    width: 800px;
    height: 600px;
}
```

As a result, resizing the Electron window did not resize the canvas.

The window became larger or smaller around the same fixed rendering element, so the `ResizeObserver` correctly emitted no new canvas dimensions.

This initially looked like a failure in the IOSurface resize path.

After the canvas was changed to fill its available area, browser resize events correctly drove native surface replacement.

The production lesson is:

```text
application window size
    is not necessarily
editor viewport size
```

The JScene3D render target must follow the actual DOM/editor element used for presentation.

It should not infer rendering dimensions from the outer Code OSS window.

### 11.6 ResizeObserver

The POC uses a browser `ResizeObserver` on the actual canvas.

Conceptually:

```text
editor/canvas layout changes
        ↓
ResizeObserver
        ↓
measure clientWidth/clientHeight
        ↓
apply devicePixelRatio
        ↓
derive physical dimensions
        ↓
request renderer resize
```

This naturally handles layout-driven size changes rather than only explicit operating-system window resize events.

That distinction is relevant to Code OSS because editor dimensions can change without the outer application window changing at all.

Examples include:

```text
opening or closing sidebars
moving panel boundaries
splitting editors
changing editor groups
entering/exiting maximized editor layouts
```

The production integration should therefore attach sizing to the actual JScene3D editor/view lifecycle.

### 11.7 Resize Event Frequency

Once real browser resizing worked, the POC exposed another issue.

A `ResizeObserver` can emit many intermediate dimensions during a single interactive resize.

Without flow control, the POC produced sequences equivalent to:

```text
2200×1536
2194×1520
2192×1516
2190×1512
2188×1508
2186×1504
...
```

Each accepted size could result in:

```text
allocate IOSurface
create Mach right
transfer IOSurface
receive IOSurface
reconfigure OpenGL
promote surface
release old surface
```

The architecture handled this correctly, but creating a new large IOSurface for nearly every pixel movement was unnecessary.

### 11.8 Resize Debouncing

The POC therefore added browser-side resize debouncing.

The final test uses approximately:

```text
75 ms
```

of quiet time before forwarding the latest dimensions.

Conceptually:

```text
ResizeObserver event
        ↓
remember latest dimensions
        ↓
restart debounce timer

ResizeObserver event
        ↓
replace remembered dimensions
        ↓
restart debounce timer

no resize event for ~75 ms
        ↓
send latest dimensions once
```

This reduced a resize operation from potentially hundreds of IOSurface replacements to a small number of meaningful transitions.

The exact `75 ms` value is not a production requirement.

It was a reasonable POC value used to prove the concept.

The Code OSS implementation should tune resize scheduling based on editor responsiveness and allocation cost.

### 11.9 Browser Debounce and Native Transition Protection Are Different

The browser-side debounce is an optimization.

It reduces unnecessary surface allocation.

The native/session-side pending-surface state is a correctness mechanism.

These should not be collapsed conceptually.

```text
browser debounce
    ↓
reduces frequency of resize requests

session pending-surface protection
    ↓
prevents overlapping native surface transitions
```

Even if the browser is heavily debounced, the renderer session should remain correct if resize requests arrive while another transition is underway.

Likewise, native correctness does not remove the need to avoid wasteful high-frequency allocation.

### 11.10 Coalescing While a Transition Is Pending

During testing, a new browser size could arrive while a previous replacement was still being transferred or acknowledged.

The desired behavior is not to queue every intermediate size.

Instead, the system should retain the latest desired size.

Conceptually:

```text
request B
    ↓
B transition in progress

request C
    ↓
remember C

request D
    ↓
replace remembered C with D

B completes
    ↓
if desired size is now D
    ↓
transition directly to D
```

The production implementation should preserve this "latest desired state" model rather than treating every resize observation as a command that must eventually execute.

### 11.11 Persistent Desired Viewport State

Renderer restart exposed a further distinction.

The current viewport dimensions are not merely a transient resize event.

They are persistent desired state.

During the restart test:

```text
renderer 1 starts at 800×600
        ↓
browser reports actual viewport 1698×2062
        ↓
renderer 1 resized to 1698×2062
        ↓
renderer 1 stops
        ↓
renderer 2 starts at 800×600
```

The browser canvas itself had not changed size during the restart.

Therefore the `ResizeObserver` had no reason to emit another resize event.

If the dimensions had been treated only as a consumed event, renderer 2 would incorrectly remain at its initial `800×600` surface.

The corrected model retains:

```text
desiredViewportWidth
desiredViewportHeight
```

independently of whether a resize transition is currently pending.

When renderer 2 emits `RENDERER_READY`, the current desired dimensions are reapplied:

```text
renderer 2 initial surface 800×600
        ↓
RENDERER_READY
        ↓
desired viewport still 1698×2062
        ↓
request 1698×2062 replacement
        ↓
SURFACE_READY
        ↓
renderer 2 active at 1698×2062
```

This distinction is important for production.

Viewport dimensions are state.

A resize notification is only one way that state changes.

### 11.12 Initial Surface Versus Desired Surface

The POC still launches Java with an initial configured surface size:

```text
800×600
```

The browser may subsequently require a different physical size.

This results in:

```text
create bootstrap surface
        ↓
launch renderer
        ↓
RENDERER_READY
        ↓
replace with actual viewport surface
```

That was acceptable for the POC.

Production may choose to improve this by determining the initial editor viewport dimensions before launching the renderer, if the Code OSS lifecycle makes that practical.

However, production should not sacrifice lifecycle correctness merely to avoid the initial replacement.

The proven safe model is:

```text
renderer always starts with a valid surface
        ↓
desired viewport state applied after readiness
```

### 11.13 Resize During Continuous Rendering

Dynamic surface replacement was tested while the cube was rotating continuously.

The sequence worked as:

```text
FRAME on A
        ↓
present A
        ↓
resize requested
        ↓
create pending B
        ↓
Java adopts B
        ↓
promote B
        ↓
subsequent FRAME uses B
```

The Java process and scene remained alive.

After each resize:

- animation continued;
- pointer dragging continued;
- the new surface was presented;
- additional resizes remained possible.

This validates resizing as part of normal editor operation rather than a special renderer-reset event.

### 11.14 Aspect Ratio

Because the render target can change shape as well as resolution, Java reconfigures its viewport and projection when adopting a replacement surface.

This was especially visible when testing a tall viewport such as:

```text
1698×2062
```

The render-target dimensions therefore affect both:

```text
native storage allocation
and
3D projection configuration
```

A production renderer must update any size-dependent rendering state when the viewport changes.

### 11.15 Result Proven by the POC

The POC demonstrated that a running Java/LWJGL renderer can repeatedly change its Chromium-visible rendering dimensions without restarting the JVM or recreating the OpenGL context.

The complete proven path is:

```text
Code OSS-style browser viewport changes
        ↓
physical dimensions calculated
        ↓
resize events coalesced
        ↓
Electron creates replacement IOSurface
        ↓
Mach control channel transfers surface
        ↓
Java rebinds render target
        ↓
Java acknowledges surface
        ↓
Electron promotes surface
        ↓
Chromium presents new surface
        ↓
continuous rendering continues
```

Dynamic editor sizing should therefore be treated as a solved rendering-mechanism problem.

The Code OSS work still needs to determine the appropriate editor lifecycle integration and production resize policy, but it does not need to rediscover how to resize the cross-process rendering surface.

## 12. Renderer Restart Validation

The final major lifecycle test performed in the POC was complete destruction and recreation of the Java renderer while keeping the Electron application process alive.

This test was deliberately performed before considering the POC complete because ordinary surface replacement proves only that a running renderer can change render targets. It does not prove that all renderer-session resources can be torn down and recreated cleanly.

The principal POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc
```

### 12.1 Why Restart Was Tested

Several parts of the POC contain state whose lifetime could accidentally have become application-global or effectively one-shot:

```text
renderer session
Java child process
Mach rendezvous state
control-channel rights
IOSurface ownership
native callbacks
frame state
desired viewport state
```

A POC that works only for the first Java process launched by Electron would not be sufficient evidence for an editor architecture.

The restart test was therefore designed to prove:

```text
renderer A
    can be completely destroyed

and

renderer B
    can be created afterward
    inside the same Electron process
```

without restarting Electron itself.

### 12.2 Tested Lifecycle

The final restart test performs:

```text
Electron application starts
        ↓
create renderer session 1
        ↓
create initial IOSurface
        ↓
create control channel
        ↓
launch Java renderer 1
        ↓
Mach rendezvous
        ↓
Java acquires initial surface
        ↓
RENDERER_READY
        ↓
apply actual viewport dimensions
        ↓
continuous rendering
        ↓
stop renderer 1
        ↓
destroy session 1 resources
        ↓
create renderer session 2
        ↓
create new initial IOSurface
        ↓
create new control channel
        ↓
launch Java renderer 2
        ↓
new Mach rendezvous
        ↓
Java acquires new initial surface
        ↓
RENDERER_READY
        ↓
reapply current desired viewport dimensions
        ↓
continuous rendering resumes
```

This succeeded.

### 12.3 Separate Java Processes

The test confirmed that renderer 2 is a genuinely new Java process rather than reuse of renderer 1.

A successful run produced output equivalent to:

```text
Launched Java renderer PID: 81944
Launched JScene3D renderer 1: 81944
...
JScene3D renderer 1 ready
...
Restarting JScene3D renderer
Requested Java renderer shutdown

Created JScene3D surface: 800x600
Launched Java renderer PID: 81968
Launched JScene3D renderer 2: 81968
...
JScene3D renderer 2 ready
```

The different PIDs demonstrate complete Java process recreation.

### 12.4 Mach Rendezvous Is Reusable Across Renderer Generations

Renderer 2 successfully performs the Mach bootstrap sequence again.

Conceptually:

```text
renderer 1
    ↓
Mach rendezvous 1
    ↓
initial surface 1
    ↓
control channel 1
    ↓
renderer 1 stops

renderer 2
    ↓
Mach rendezvous 2
    ↓
initial surface 2
    ↓
control channel 2
```

This proves that the architecture does not depend on a single successful rendezvous for the entire Electron application lifetime.

The renderer/session resources can be recreated.

### 12.5 Control Channel Is Per Renderer Session

The restart test also confirms the intended control-channel ownership model.

Renderer 1 owns one receive side of a control channel associated with session 1.

When renderer 1 is destroyed, that channel is no longer needed.

Renderer 2 receives a newly created control-channel receive right during its own bootstrap.

Conceptually:

```text
session 1
    └── control channel 1

session 2
    └── control channel 2
```

The control channel is therefore naturally renderer-session state rather than Electron-application-global state.

### 12.6 Callback Generation Protection

Restart introduces another asynchronous lifecycle issue.

Shared-texture presentation is asynchronous.

It is therefore theoretically possible for a presentation initiated by renderer 1 to complete after renderer 1 has been stopped and renderer 2 has already started.

The restart test harness uses a renderer-generation value to distinguish callbacks belonging to different renderer instances.

Conceptually:

```text
rendererGeneration = 1
        ↓
frame presentation begins

restart
        ↓
rendererGeneration = 2

old generation-1 async callback completes
        ↓
generation mismatch
        ↓
ignore callback
```

This prevents a stale callback from renderer 1 from restarting the frame loop or modifying renderer-2 state.

The exact generation-counter mechanism is POC-level, but the underlying production requirement is important:

```text
asynchronous work from an obsolete renderer
must not affect a replacement renderer
```

Code OSS renderer/session lifecycle design should account for stale asynchronous completions.

### 12.7 Desired Viewport State Must Survive Restart

The restart test exposed an important state-model issue.

Renderer 1 initially starts with the POC bootstrap dimensions:

```text
800×600
```

The actual browser viewport may then request, for example:

```text
1698×2062
```

Renderer 1 transitions to that surface successfully.

When renderer 2 starts, it again begins with:

```text
800×600
```

However, the browser viewport itself has not changed during the restart.

Therefore the browser's `ResizeObserver` has no reason to emit another resize event.

If viewport dimensions are modeled only as one-shot resize notifications, renderer 2 incorrectly remains at the bootstrap dimensions.

The POC corrected this by retaining the current desired viewport dimensions as persistent state.

The lifecycle becomes:

```text
desired viewport = 1698×2062

renderer 1
    ↓
RENDERER_READY
    ↓
apply 1698×2062

renderer 1 stops

desired viewport remains 1698×2062

renderer 2
    ↓
RENDERER_READY
    ↓
reapply 1698×2062
```

The successful restart log then showed:

```text
JScene3D renderer 2 ready
Sent JScene3D replacement surface: 1698x2062
JNI received control-channel IOSurface: 1698x2062
Promoted JScene3D surface: 1698x2062
```

This distinction should carry into production:

```text
viewport dimensions are persistent state

resize notifications are events that modify that state
```

### 12.8 Rendering After Restart

After renderer 2 became ready and received the current desired viewport size, continuous rendering resumed successfully.

The replacement renderer:

- displayed the scene;
- rotated continuously;
- used the correct viewport geometry;
- accepted pointer dragging; and
- remained capable of further surface replacement.

This demonstrates that the complete pipeline can be reconstructed:

```text
Java/OpenGL
    ↓
new IOSurface
    ↓
Electron SharedTexture
    ↓
VideoFrame
    ↓
WebGPU
    ↓
browser viewport
```

without restarting Electron.

### 12.9 Scene State Is Not Preserved by the POC

The restart test creates a new `CubeRenderer`.

The scene state belonging to renderer 1 is not serialized and restored into renderer 2.

For the test cube this means renderer 2 starts from its own initial rotation state.

The POC therefore proves:

```text
renderer infrastructure can restart
```

but does not prove:

```text
arbitrary JScene3D scene state can automatically survive process restart
```

That is a separate production design concern.

In the real editor, the authoritative scene/document model should determine how a replacement renderer reconstructs its state.

The renderer process should not necessarily be treated as the sole durable owner of editor content.

### 12.10 Visible Restart Pause

A hard renderer restart produces a visible pause.

This is expected with the tested lifecycle because the sequence is deliberately sequential:

```text
stop Java renderer 1
        ↓
destroy renderer 1 resources
        ↓
launch new JVM
        ↓
initialize GLFW
        ↓
initialize OpenGL
        ↓
perform Mach rendezvous
        ↓
construct render target
        ↓
RENDERER_READY
        ↓
restore viewport
        ↓
resume frames
```

Launching a JVM and initializing LWJGL/OpenGL is not instantaneous.

The POC did not attempt to hide this pause.

Possible production strategies, if seamless recovery becomes a requirement, could include:

```text
retain last presented frame while renderer restarts

keep a renderer process alive rather than routinely restarting it

overlap old/new renderer lifetimes during deliberate replacement

show an editor-level renderer recovery state
```

These are production UX/lifecycle questions.

The pause does not indicate a failure of the shared-surface architecture.

### 12.11 Restart Is Not Intended as Normal Resize Behavior

Ordinary viewport resizing does not restart Java.

The two lifecycles should remain distinct:

```text
viewport resize
    ↓
replace IOSurface
    ↓
same JVM
same OpenGL context
same scene

renderer restart
    ↓
destroy renderer session
    ↓
new JVM
new OpenGL context
new render target
new renderer state
```

The restart capability exists for renderer lifecycle management, failure recovery, editor lifecycle requirements, or future architectural needs.

It should not replace the lightweight surface-replacement mechanism used for normal editor resizing.

### 12.12 Result Proven by the POC

The final restart test proves that the rendering architecture is not dependent on one Java renderer living for the entire Electron application lifetime.

A complete renderer session can be destroyed and recreated while Electron remains running.

The replacement session can successfully recreate:

```text
Java child process
Mach rendezvous
initial IOSurface
control channel
OpenGL render target
renderer callbacks
frame loop
viewport sizing
browser presentation
interaction path
```

This was the final major lifecycle validation performed before considering the technical POC complete.

## 13. Important Bugs and Lessons from the POC

Several of the most useful results from the POC came from failures encountered while extending the original static rendering path into a reusable renderer lifecycle.

These issues should be documented because they expose ownership and ordering constraints that may not be obvious when implementing the Code OSS version.

The purpose of this section is not to prescribe the exact POC fixes as production code. It is to preserve the reasoning behind the resulting architecture so the same problems are not rediscovered.

### 13.1 Control Channel Lifetime Bug

The most significant native ownership bug appeared when the POC progressed from one successful surface replacement to repeated replacement.

The first transition worked:

```text
surface A
    ↓
surface B
```

but the next transition failed:

```text
surface B
    ↓
surface C
    ↓
MACH_SEND_INVALID_DEST
```

Electron reported:

```text
JScene3D control send failed: 0x10000003
```

which corresponds to:

```text
MACH_SEND_INVALID_DEST
```

The relevant Java/JNI implementation was:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c
```

The problem was that the JNI surface-release operation performed both:

```c
jscene3d_iosurface_release(surface);

jscene3d_control_channel_close();
```

This incorrectly tied two independent lifetimes together.

The actual sequence was:

```text
Java owns surface A
        ↓
receive surface B
        ↓
bind surface B
        ↓
release surface A
        ↓
release(A) also closes control channel
        ↓
SURFACE_READY B
        ↓
Electron later tries to send surface C
        ↓
Java control-channel receive right no longer exists
        ↓
MACH_SEND_INVALID_DEST
```

The correct fix was to make surface release release only the surface:

```c
jscene3d_iosurface_release(surface);
```

The persistent control channel remains alive until renderer/session teardown.

The resulting ownership rule is:

```text
IOSurface lifetime
        !=
control-channel lifetime
```

More specifically:

```text
renderer session
    │
    ├── control channel
    │
    ├── surface A
    │     ↓ release
    ├── surface B
    │     ↓ release
    ├── surface C
    │     ↓ release
    └── ...
```

After this correction, repeated surface replacement worked continuously.

This is a critical production invariant.

### 13.2 Fixed Canvas Size Hid Resize Behavior

An early browser-side POC page used:

```css
canvas {
    width: 800px;
    height: 600px;
}
```

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/index.html
```

The Electron `BrowserWindow` could be resized, but the actual canvas remained `800×600` CSS pixels.

As a result:

```text
BrowserWindow changes size
        ↓
canvas remains 800×600
        ↓
ResizeObserver sees no canvas-size change
        ↓
no native resize request
```

This initially looked like a failure in the surface-resize path.

It was not.

The browser layout was behaving exactly as specified.

The fix was to make the canvas fill its available content area.

The production lesson is:

```text
outer application window dimensions
    are not
render viewport dimensions
```

The native rendering surface must track the actual editor element that presents JScene3D.

This is especially important in Code OSS because editor dimensions can change through layout operations even when the outer application window remains unchanged.

### 13.3 Startup Resize Race

Once the browser canvas became dynamic, the browser could report its actual physical dimensions almost immediately after startup.

For example:

```text
initial renderer surface
    800×600

actual browser viewport
    2200×1536
```

An intermediate implementation sent the replacement surface immediately after launching Java.

The observed ordering was effectively:

```text
Electron creates 800×600
        ↓
Electron launches Java
        ↓
browser requests 2200×1536
        ↓
Electron sends replacement 2200×1536
        ↓
Java is still acquiring initial 800×600
```

The test happened to succeed, but the lifecycle depended on timing.

Successful process creation did not mean Java had completed initialization.

The architectural fix was to introduce:

```text
RENDERER_READY
```

Java emits this only after:

```text
Mach rendezvous
        ↓
initial IOSurface lookup
        ↓
GLFW initialization
        ↓
OpenGL initialization
        ↓
IOSurfaceRenderTarget construction
```

Electron retains the desired resize until that event arrives.

The corrected lifecycle is:

```text
launch renderer
        ↓
browser determines desired viewport
        ↓
retain desired viewport
        ↓
RENDERER_READY
        ↓
apply desired viewport
```

The production lesson is:

```text
process launched
    !=
renderer initialized
```

### 13.4 Callback Registration Race

After adding `RENDERER_READY`, the POC initially exposed separate JavaScript operations equivalent to:

```text
launchRenderer(...)
setRendererReadyCallback(...)
setFrameReadyCallback(...)
```

This introduced another timing race.

The Java process could theoretically initialize quickly enough to emit:

```text
RENDERER_READY
```

before JavaScript registered the corresponding callback.

The final POC changed the native launch API so lifecycle callbacks are supplied as part of launch.

The native binding can therefore perform:

```text
create session
        ↓
install renderer-ready callback
        ↓
install frame-ready callback
        ↓
launch Java
```

The important invariant is:

```text
event consumers must exist
before
event producers can emit
```

The production Code OSS API may use events, promises, services, callbacks, or another mechanism, but it should preserve this ordering.

### 13.5 One Successful Surface Replacement Was Not Sufficient Validation

At one point the POC successfully demonstrated:

```text
800×600
    ↓
640×480
```

That proved the basic replacement mechanism but did not prove that the control channel and ownership model were reusable.

Only after testing actual interactive resizing did the one-shot control-channel bug become visible.

The lesson is that lifecycle mechanisms should be tested repeatedly.

For surface replacement, the meaningful validation was:

```text
A → B → C → D → E ...
```

not merely:

```text
A → B
```

The Code OSS implementation should include repeated lifecycle tests rather than relying on a single successful transition.

### 13.6 High-Frequency Resize Allocation

After fixing the persistent control channel, interactive resizing produced a large number of successful surface transitions.

A typical sequence contained many closely spaced sizes:

```text
2200×1536
2194×1520
2192×1516
2190×1512
2188×1508
...
```

The native architecture handled these transitions correctly.

However, each one potentially involved:

```text
allocate large IOSurface
        ↓
create Mach right
        ↓
transfer resource
        ↓
receive resource
        ↓
reconfigure OpenGL
        ↓
promote surface
        ↓
release old surface
```

Correctness alone therefore produced unnecessary allocation churn.

The POC added browser-side resize debouncing of approximately:

```text
75 ms
```

This reduced surface transitions substantially.

The lesson is that correctness and flow control are separate concerns.

The native session should remain correct under rapid requests, while the browser/editor layer should avoid producing unnecessary native work.

### 13.7 Resize Events Are Not the Same as Viewport State

The renderer restart test exposed another subtle state-model problem.

The browser initially sent its current viewport dimensions to renderer 1.

After those dimensions were applied, the resize request was considered consumed.

Renderer 1 was then stopped and renderer 2 launched.

Renderer 2 started with the bootstrap size:

```text
800×600
```

but the browser viewport had not changed.

Therefore:

```text
no ResizeObserver event
        ↓
no new resize request
        ↓
renderer 2 remains 800×600
```

The mistake was treating the current dimensions purely as an event.

The correct model is:

```text
desired viewport size
    persistent state

resize notification
    event that changes desired state
```

When any new renderer becomes ready, the current desired state must be applied whether or not a new browser resize event occurred.

This lesson is likely to matter beyond resizing.

Production renderer integrations should distinguish between:

```text
events
and
state that must be reconstructed after renderer replacement
```

### 13.8 Stale Asynchronous Callback Risk During Restart

Shared-texture presentation is asynchronous.

During renderer restart, work initiated for renderer 1 can theoretically complete after renderer 2 has already started.

Without protection, an obsolete callback could:

```text
clear renderer-2 frame state
request a new frame
apply old lifecycle state
```

The POC restart harness protects against this with a renderer-generation identifier.

Conceptually:

```text
generation 1
    ↓
async presentation begins

restart
    ↓
generation 2

generation-1 callback completes
    ↓
generation mismatch
    ↓
discard
```

The generation counter itself is not necessarily the production solution.

The production lesson is:

```text
async operations must be scoped to renderer/session identity
```

Once a renderer session is obsolete, its asynchronous completions must not mutate the replacement session.

### 13.9 Protocol Strings Became an Interface

The earliest POC used literal strings such as:

```text
FRAME_READY
SURFACE_READY
QUIT
```

directly throughout Java and C++.

As the renderer lifecycle grew, these literals became a genuine cross-process protocol.

They were centralized into:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/RendererProtocol.java
```

The lesson is broader than string constants.

Once two processes depend on the same message vocabulary, that vocabulary is an interface and should be treated as such.

The production implementation should make protocol evolution deliberate.

### 13.10 Renderer Session Refactoring Was Responsibility-Driven

`JScene3DRendererSession` grew substantially while the POC acquired process launch, pipe handling, stdout parsing, surface ownership, Mach coordination and protocol semantics.

The process mechanics were eventually extracted into:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc
```

The reason was not that the session source file had reached a particular number of lines.

The reason was that process transport had become a separate responsibility.

This is relevant to the production implementation:

```text
do not refactor merely because a file is long

do refactor when responsibilities have become distinct
```

The POC's useful separation was:

```text
renderer session
    renderer semantics and resource lifecycle

renderer process
    child-process and stream mechanics
```

### 13.11 Aspect-Ratio Diagnosis Required Looking at Actual Render-Target State

During the restart test, the cube appeared as a cuboid after the browser had selected a tall surface:

```text
1698×2062
```

At first this looked like evidence that resize did not update the OpenGL projection.

Inspection of:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java
```

showed that the render target already recalculated both:

```text
glViewport(...)
and
glFrustum(...)
```

using the replacement dimensions.

The actual restart problem was that renderer 2 had not been given the browser's persistent desired viewport dimensions.

Once desired viewport state was reapplied after `RENDERER_READY`, the restart test rendered correctly.

The lesson is to distinguish:

```text
rendering/projection state problem
from
renderer lifecycle/state restoration problem
```

rather than fixing the visually obvious layer before checking the actual state transition.

### 13.12 Chromium Shutdown Warning Was Not a Java Renderer Failure

During many test runs, application shutdown printed messages similar to:

```text
Requested Java renderer shutdown
[...:ERROR:base/process/process_posix.cc:313]
Unable to terminate process ...: No such process (3)
```

Because this appeared immediately after the JScene3D shutdown message, it initially looked like a Java process lifecycle problem.

The process tree was inspected explicitly.

A representative run showed:

```text
Electron main process
Electron GPU helper
Electron Network Service helper
Electron renderer helper
Java JScene3D renderer
```

The PID reported by the Chromium error was then compared with the live process tree.

It matched:

```text
Electron Helper
--type=utility
--utility-sub-type=network.mojom.NetworkService
```

not the Java renderer PID.

The Java process was a different PID and exited correctly.

The conclusion was that Chromium was attempting to terminate its Network Service helper after that helper had already exited.

No JScene3D shutdown change was made in response.

This investigation is important because it prevents production work from trying to "fix" an unrelated Chromium shutdown race inside the JScene3D renderer lifecycle.

### 13.13 Hard Renderer Restart Has a Visible Pause

The restart test deliberately performs a sequential hard restart:

```text
stop Java renderer 1
        ↓
destroy session 1
        ↓
launch new JVM
        ↓
initialize GLFW/OpenGL
        ↓
Mach rendezvous
        ↓
RENDERER_READY
        ↓
restore viewport
        ↓
resume frames
```

A visible pause occurs.

This is expected.

The POC did not attempt to solve seamless renderer replacement.

If production requires transparent recovery, possible strategies can be investigated separately.

Examples include:

```text
retain the last presented frame during restart

show a renderer-restarting state

keep renderer processes alive longer

overlap deliberate renderer replacement
```

The pause is not evidence of a problem with IOSurface sharing.

### 13.14 General Lessons to Preserve

The main lifecycle lessons from the POC can be summarized as:

```text
resource lifetime must follow ownership

process launch is not renderer readiness

native resource delivery is not resource activation

viewport dimensions are persistent state

surface resize is not renderer restart

async callbacks belong to a specific renderer generation

high-frequency UI events require flow control

platform-specific resource transfer should remain behind abstractions

one successful transition does not prove a reusable lifecycle

diagnose process IDs before attributing shutdown errors
```

These lessons are at least as important as the individual POC classes.

The Code OSS implementation should use the working POC as a reference, but it should carry forward these invariants rather than mechanically reproducing every implementation detail.

## 14. What Is Proven vs POC-Specific

The POC should be used as evidence for the viability of the rendering architecture, not as a specification requiring every implementation detail to be reproduced in Code OSS.

This distinction is important because the POC contains two different kinds of decisions:

```text
mechanisms and lifecycle behavior
    that were explicitly tested and proven

implementation choices
    made because they were convenient for the POC
```

The Code OSS implementation should preserve the proven architectural invariants while reconsidering POC-specific implementation choices in the context of the existing JScene3D and Code OSS architectures.

### 14.1 Proven Mechanisms

The following mechanisms were demonstrated end-to-end and should be treated as technically proven on macOS.

#### Shared GPU Rendering Path

The complete rendering path works:

```text
Java / LWJGL / OpenGL
        ↓
IOSurface
        ↓
Electron native layer
        ↓
Chromium SharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture
        ↓
browser/editor canvas
```

No CPU framebuffer readback is required between Java and Chromium.

#### Separate Java Renderer Process

Java can run as a child process of Electron rather than being embedded into Chromium.

The renderer can initialize LWJGL/OpenGL independently while Electron coordinates its lifecycle.

#### Non-Global IOSurface Sharing

The initial IOSurface can be transferred to Java using a Mach right through Chromium's Mach rendezvous infrastructure.

A globally discoverable IOSurface ID is not required.

#### Persistent Native Resource Transfer

A persistent Mach control channel can transfer replacement IOSurface rights after Java has launched.

The channel supports repeated transfers during one renderer session.

#### Dynamic Surface Replacement

A running Java renderer can replace its IOSurface-backed render target without:

```text
restarting the JVM
recreating the OpenGL context
recreating the scene
```

The replacement can change both resolution and aspect ratio.

#### Acknowledged Surface Promotion

Electron can maintain:

```text
active surface
pending surface
```

and promote the pending surface only after Java acknowledges successful adoption.

This lifecycle works.

#### Retina / Physical Pixel Rendering

The native render surface can use physical pixel dimensions derived from:

```text
CSS dimensions × devicePixelRatio
```

rather than being restricted to CSS logical dimensions.

#### Continuous Rendering

The Java renderer can continuously render through the shared-surface path.

The complete pipeline remained stable under sustained frame delivery.

#### Frame Back-Pressure

Electron can pace Java rendering so only one frame is in flight.

Java does not need to run an unrestricted render loop.

#### Browser-to-Java Interaction

Pointer interaction can originate in the browser/editor layer, cross the Electron/Java boundary, update Java scene state, and appear in subsequent rendered frames.

#### Rendering During Resize

Continuous rendering and interaction remain functional while IOSurfaces are repeatedly replaced.

#### Renderer Readiness Handshake

An explicit `RENDERER_READY` lifecycle event successfully prevents rendering and resize operations from racing Java/OpenGL initialization.

#### Renderer Recreation

A Java renderer session can be completely destroyed and another created within the same Electron process.

The replacement renderer can establish:

```text
new Java process
new Mach rendezvous
new IOSurface
new control channel
new OpenGL render target
new frame loop
```

and resume browser presentation.

#### Persistent Desired Viewport State

The desired viewport size can be retained independently of a particular renderer instance and reapplied when a replacement renderer becomes ready.

This behavior was required and verified during restart testing.

### 14.2 Proven Lifecycle Invariants

The POC also established several lifecycle rules that should be treated as architectural invariants unless the production design intentionally replaces the underlying mechanism.

#### Process Launch and Renderer Readiness Are Different States

```text
process launched
    !=
renderer ready
```

Java must complete native and OpenGL initialization before renderer operations begin.

#### Surface Delivery and Surface Activation Are Different States

```text
surface transferred
    !=
surface active
```

Java must successfully adopt a replacement before Electron promotes it.

#### Surface Lifetime and Control-Channel Lifetime Are Independent

```text
individual IOSurface lifetime
    !=
control-channel lifetime
```

The control channel belongs to the renderer session.

#### Scene Lifetime and Render-Target Lifetime Are Independent

Ordinary viewport resize should replace the render target without destroying the Java scene.

#### Viewport Size Is State

```text
resize event
    modifies
desired viewport state
```

The desired dimensions must survive renderer recreation.

#### Asynchronous Work Belongs to a Renderer Session

Callbacks or promises initiated by an obsolete renderer must not mutate a replacement renderer.

#### UI Event Frequency Should Not Dictate Native Allocation Frequency

Browser resize events can occur much faster than it is useful to allocate replacement IOSurfaces.

Flow control should exist between UI observation and native resource replacement.

### 14.3 POC Implementation Details That Are Not Production Decisions

The following implementation choices were useful for the POC but should not automatically be reproduced.

#### `CubeRenderer`

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

The cube is purely a validation scene.

Its immediate-mode OpenGL rendering, rotation logic and command processing are not a proposed JScene3D renderer architecture.

#### `poc` Java Package

The POC Java code lives under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/
```

This package structure exists because the renderer was embedded in the Electron test area.

Production Java code should fit the existing JScene3D Maven/module architecture.

#### Standalone Test Page

The POC uses:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/index.html

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js
```

These files exist to prove the browser rendering path.

They are not a proposed Code OSS editor implementation.

#### `test-jscene3d-binding.js`

Reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js
```

This file coordinates most of the POC lifecycle from one test application.

Production Code OSS responsibilities should be placed into appropriate services, editor contributions and lifecycle components rather than reproducing this monolithic test harness.

#### Single Global Native Session

The POC binding effectively manages one current renderer session.

This was sufficient for proving the architecture.

It does not establish that production should support only one JScene3D renderer, one editor, or one renderer process.

The Code OSS design must decide the required relationship between:

```text
JScene3D documents
editor instances
renderer sessions
Java processes
native surfaces
```

#### Line-Oriented stdin/stdout Protocol

The POC protocol uses text messages over Java stdin/stdout.

This proved the required lifecycle but does not establish the final production transport or serialization format.

#### Protocol Message Shape

Messages such as:

```text
FRAME 123
DRAG 10 -4
SURFACE_READY 2200 1536
```

are POC representations.

Production may require typed, structured, correlated or versioned messages.

#### Frame-Per-Request Animation

The cube advances rotation once for each `FRAME` request.

This was useful for validating back-pressure.

It is not a production animation timing model.

#### 75 ms Resize Debounce

The POC uses approximately:

```text
75 ms
```

for browser-side resize debouncing.

This value is not a requirement.

Production should tune resize behavior based on actual editor UX and native allocation cost.

#### Initial 800×600 Bootstrap Surface

The POC launches Java with:

```text
800×600
```

and subsequently replaces that surface with the actual viewport dimensions.

This is known to work safely.

Production may be able to launch with the actual editor dimensions immediately.

#### Callback API Shape

The POC passes JavaScript callbacks into the native renderer launch API.

This solved the callback-registration race.

The production implementation can use a different API model as long as lifecycle observers are installed before the renderer can emit events.

#### Renderer Generation Counter

The restart harness uses a generation number to reject stale asynchronous completions.

The invariant is important.

The specific integer-generation implementation is not mandatory.

#### Hard Restart UX

The POC allows the visible frame loop to pause while a new JVM initializes.

This proves restartability but does not define the desired production restart experience.

### 14.4 Production Questions Not Answered by the POC

The POC intentionally did not resolve several production concerns.

These include:

```text
final Code OSS editor architecture
final JScene3D Java module placement
renderer/session multiplicity
renderer process sharing strategy
production renderer protocol
protocol versioning
error propagation
renderer crash detection
renderer crash recovery
scene reconstruction after renderer failure
security hardening
sandbox implications
resource limits
memory-pressure handling
GPU/device-loss handling
visibility and editor-background behavior
production frame scheduling
production resize policy
telemetry/diagnostics
automated integration testing
cross-platform implementation
Windows shared-surface architecture
Linux shared-surface architecture
packaging and distribution of native/JNI components
packaging and launching the Java runtime
```

These should be addressed as part of production design rather than inferred from the POC.

### 14.5 macOS Is the Proven Platform

The POC proves the architecture on macOS.

Specifically, the proven native mechanisms depend on:

```text
IOSurface
Mach ports
Chromium MachPortRendezvous
macOS OpenGL/IOSurface interoperability
Chromium SharedTexture
```

The higher-level abstractions were deliberately kept more generic, but equivalent Windows and Linux resource-sharing mechanisms have not been proven by this POC.

The initial Code OSS implementation can therefore treat macOS as the validated platform.

Cross-platform support should be designed separately rather than assuming IOSurface/Mach semantics map directly to other operating systems.

### 14.6 How Codex Should Use the POC

When implementing the Code OSS integration, use the POC in two ways.

First, use it as executable evidence when the behavior of a native mechanism is uncertain.

Inspect the working implementation under:

```text
/Users/glynch/development/projects/jscene3d-electron
```

rather than re-deriving already solved Mach, IOSurface, JNI or SharedTexture behavior.

Second, treat the POC architecture as constraints and lessons rather than a source tree to copy mechanically.

Before porting a POC class or API directly, determine whether Code OSS or the existing JScene3D modules already provide a more appropriate abstraction.

The desired result is:

```text
proven native mechanism
        +
existing JScene3D architecture
        +
existing Code OSS architecture
        ↓
production integration
```

not:

```text
copy standalone POC into Code OSS
```

### 14.7 Summary

The POC has answered the feasibility questions.

The following should no longer be treated as speculative:

```text
Java can render directly into IOSurface

Electron can securely transfer that surface to Java

Chromium can present the same surface without CPU framebuffer copying

surfaces can be replaced repeatedly

continuous rendering works

browser interaction works

resize works

renderer recreation works
```

The remaining work is primarily production architecture and integration.

That is the boundary between the completed POC and the Code OSS implementation phase.

## 15. Proposed Code OSS Direction

The proposed next phase is to integrate the rendering architecture proven by the Electron POC into the Code OSS-based JScene3D editor.

This section describes architectural direction, not a final implementation specification.

Codex should first inspect the existing Code OSS and JScene3D architecture and map these responsibilities onto appropriate existing abstractions before introducing new services, classes, APIs, or modules.

The objective is not to embed the standalone POC application inside Code OSS.

The objective is to make the proven rendering path a native capability of the JScene3D editor.

### 15.1 Target Architecture

At a high level, the proposed production architecture remains:

```text
┌─────────────────────────────────────────────┐
│ Code OSS Renderer Process                   │
│                                             │
│ JScene3D editor                             │
│ editor viewport                             │
│ WebGPU presentation                         │
│ user interaction                            │
└─────────────────────┬───────────────────────┘
                      │
                      │ Code OSS / Electron
                      │ integration boundary
                      ▼
┌─────────────────────────────────────────────┐
│ Electron Browser Process                    │
│                                             │
│ JScene3D renderer-session management        │
│ native shared-surface management            │
│ Java process management                     │
│ frame coordination                          │
└──────────────┬──────────────────┬───────────┘
               │                  │
               │ protocol         │ native resource transfer
               │                  │
               ▼                  ▼
┌─────────────────────────────────────────────┐
│ Java JScene3D Renderer Process              │
│                                             │
│ JScene3D scene/rendering                    │
│ LWJGL / OpenGL                              │
│ platform render-target integration          │
└─────────────────────────────────────────────┘
```

The principal responsibility split should remain:

```text
Code OSS renderer
    editor UI
    editor viewport
    browser input
    presentation

Electron browser/native layer
    renderer lifecycle
    Java process lifecycle
    shared native surfaces
    frame coordination
    native resource transfer

Java/JScene3D
    scene state
    scene rendering
    OpenGL render target
```

### 15.2 JScene3D Should Become an Editor Capability

The Code OSS side should expose JScene3D rendering through the appropriate editor architecture rather than through a standalone BrowserWindow test page.

The visible result should behave as a normal Code OSS editor/view.

Conceptually:

```text
JScene3D-compatible document/resource
        ↓
Code OSS opens JScene3D editor
        ↓
editor creates rendering viewport
        ↓
renderer session becomes available
        ↓
Java renders scene
        ↓
frames presented inside editor viewport
```

The exact editor registration mechanism should be determined by inspecting the existing Code OSS architecture in the target workspace/version.

Do not assume the POC's:

```text
index.html
renderer.js
preload.js
```

structure maps directly onto Code OSS.

### 15.3 Preserve the Browser-Based Editor Surface

The POC demonstrates that the visible JScene3D editor does not need to be a native Java or GLFW window.

The proposed direction is to keep the visible editor surface inside the Code OSS renderer.

That gives:

```text
normal Code OSS DOM/editor integration
        +
browser input handling
        +
Code OSS layout behavior
        +
WebGPU presentation
```

while Java remains responsible for actual 3D rendering.

This avoids native child-window embedding and its associated problems with:

```text
z-order
clipping
layout
focus
DPI
window movement
editor tabs
split editors
overlays
Code OSS workbench integration
```

The browser editor displays the Java-rendered shared texture rather than embedding Java's hidden GLFW window.

### 15.4 Native Electron Capability

The Code OSS/Electron fork will require native functionality equivalent to the capability proven in:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/
```

The production API should provide the concepts required by the editor integration, such as:

```text
create/start renderer session
observe renderer readiness
obtain/present current shared surface
request viewport size
send renderer commands
observe frame readiness
stop renderer session
```

The exact public API should be designed around the Code OSS integration rather than preserving the POC function names.

The native API should remain thin.

Lifecycle logic should reside in renderer/session abstractions rather than accumulating in the JavaScript binding layer.

### 15.5 Renderer Session Identity

The POC has effectively one global current renderer session.

Production should not assume that is sufficient.

Code OSS can have:

```text
multiple editor groups
multiple open JScene3D documents
multiple views of one document
background editors
restored editors
```

Before designing the native JavaScript API, determine the required session model.

Potential relationships include:

```text
one Java renderer per editor instance

one Java renderer per JScene3D document

one Java renderer shared by multiple editor views

one Java renderer process hosting multiple logical sessions
```

The POC does not answer this question.

The first production implementation should choose the simplest model that satisfies actual JScene3D editor requirements while avoiding an API that permanently assumes a single global renderer.

Session identity should therefore be explicit even if the first implementation supports only one active session.

### 15.6 Renderer Process Strategy

The POC launches one JVM containing one cube renderer.

Production must decide how JScene3D rendering processes relate to editor sessions.

Relevant considerations include:

```text
JVM startup cost
memory usage
scene isolation
failure isolation
multiple editors
renderer restart
resource ownership
debugging
future plugin/tool requirements
```

Do not prematurely optimize this into a shared multi-scene renderer process without understanding the JScene3D requirements.

Likewise, do not bake "one JVM per application" or "one JVM per editor" into low-level native APIs unless that decision has been made deliberately.

The native surface/session architecture should allow the process strategy to evolve.

### 15.7 Java Integration Should Fit Existing JScene3D Modules

The POC Java implementation lives under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/
```

Production code should not reproduce this package.

Codex should inspect the existing JScene3D Maven modules and determine where responsibilities belong.

Conceptually, the Java side will need equivalents of:

```text
renderer process entry/lifecycle
renderer protocol
shared render-target abstraction
macOS IOSurface JNI integration
scene/editor renderer integration
```

These should fit existing module boundaries and dependency direction.

In particular, ordinary JScene3D scene/rendering code should not acquire dependencies on:

```text
Electron
Chromium
Mach rendezvous
Mach message structures
```

Those belong in integration/platform layers.

### 15.8 Native Platform Boundary

The production design should preserve a generic conceptual boundary around the shared rendering surface.

Higher-level code should reason about:

```text
shared render surface
dimensions
surface replacement
surface readiness
native presentation handle
```

The macOS implementation can then provide:

```text
IOSurface
Mach right transfer
Mach rendezvous
Chromium IOSurface handle
```

This keeps future Windows/Linux work possible without pretending that those platforms have already been solved.

The macOS implementation is the only one currently proven.

### 15.9 Initial Renderer Startup

A production editor startup should conceptually perform:

```text
JScene3D editor created
        ↓
determine initial desired viewport state
        ↓
create renderer session
        ↓
create shared surface
        ↓
create control channel
        ↓
install lifecycle observers
        ↓
launch Java renderer
        ↓
Mach rendezvous
        ↓
Java initializes OpenGL/render target
        ↓
RENDERER_READY
        ↓
ensure current desired viewport is applied
        ↓
start frame scheduling
```

If the actual editor dimensions are available before renderer launch, production may avoid the POC's initial `800×600` bootstrap replacement.

However, the implementation must still tolerate viewport changes during startup.

Desired viewport state should remain independent of renderer readiness.

### 15.10 Frame Presentation

The production editor should preserve the proven GPU presentation path:

```text
active JScene3D native surface
        ↓
Chromium SharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture
        ↓
editor viewport
```

Do not replace this with:

```text
glReadPixels
CPU buffers
base64
PNG/JPEG
canvas ImageData
```

unless a separate fallback/debug path explicitly requires it.

The purpose of the POC was specifically to avoid such a copy-heavy architecture.

### 15.11 Frame Scheduling

The POC proves that consumer-driven back-pressure works.

The production scheduler should build on that result but should be designed around editor behavior.

Potential states include:

```text
visible + animated
    continuous rendering

visible + static
    render on demand

hidden/background
    pause or heavily reduce rendering

interaction
    prioritize low-latency frame

resize
    coordinate with surface transition
```

Do not assume the POC's always-rotating cube means every production editor should render continuously.

The important property is that rendering can be paced and back-pressured.

### 15.12 Viewport State

The production editor should maintain explicit desired viewport state.

Conceptually:

```text
desiredViewport = {
    physicalWidth,
    physicalHeight
}
```

The desired state belongs to the editor/session lifecycle rather than to a single resize event.

When:

```text
layout changes
DPI changes
renderer starts
renderer restarts
editor becomes visible
```

the renderer session can reconcile:

```text
desired viewport
vs
active native surface
```

and initiate a replacement when necessary.

This is more robust than treating resize as a fire-and-forget command.

### 15.13 Resize Policy

The Code OSS editor should observe the actual JScene3D viewport element.

The POC's `ResizeObserver` approach is appropriate conceptually because Code OSS layout changes do not always correspond to outer window resize events.

The production flow should resemble:

```text
JScene3D editor viewport changes
        ↓
measure CSS dimensions
        ↓
determine current device pixel ratio
        ↓
calculate desired physical dimensions
        ↓
coalesce rapid changes
        ↓
update desired viewport state
        ↓
renderer session reconciles surface
```

The exact debounce/throttle strategy should be chosen based on real editor behavior.

### 15.14 Input Architecture

Browser/editor input should remain on the Code OSS side.

The editor can translate Code OSS/browser interaction into renderer operations.

The production protocol will likely need richer input semantics than the POC's:

```text
DRAG dx dy
```

Potential operations include:

```text
camera orbit
camera pan
camera zoom
selection
picking
transform gizmos
keyboard navigation
tool commands
scene editing commands
```

The editor should send semantic renderer/editor operations where appropriate rather than blindly forwarding every DOM event across the process boundary.

### 15.15 Renderer Restart and Recovery

The POC proves that renderer infrastructure can be recreated.

Production should use that capability as a foundation for lifecycle and failure recovery.

A renderer/session service should be able to represent states such as:

```text
starting
ready
stopping
stopped
failed
restarting
```

The exact state machine should be designed explicitly.

Do not infer renderer health merely from whether a Java PID exists.

`RENDERER_READY` already demonstrates why process state and renderer state are different concepts.

### 15.16 Document/Scene Authority

The POC renderer owns only a temporary cube scene, so renderer restart simply creates another cube.

A production editor needs a deliberate answer to:

```text
where is authoritative scene/document state?
```

A Java renderer process that can fail or restart should not accidentally become the only durable representation of unsaved editor state unless that is an explicit architectural decision.

The existing JScene3D document/model architecture should guide this.

Renderer reconstruction after restart should eventually be possible from authoritative editor/document state.

### 15.17 Error Propagation

Production should replace generic POC logging with explicit renderer/session errors.

Potential failure categories include:

```text
Java executable unavailable
renderer process launch failure
Mach rendezvous failure
IOSurface creation failure
IOSurface transfer failure
OpenGL initialization failure
render-target initialization failure
surface replacement failure
protocol failure
unexpected renderer exit
SharedTexture import failure
WebGPU presentation failure
```

The editor should be able to distinguish recoverable renderer failure from document/editor failure.

Native error details should remain available for diagnostics.

### 15.18 Do Not Over-Generalize Before the First Integration Works

Although production architecture matters, the first Code OSS implementation should remain incremental.

The POC has already retired the difficult feasibility risk.

The next objective should be to reproduce the smallest useful vertical slice inside the actual Code OSS editor architecture:

```text
open JScene3D editor
        ↓
launch real renderer integration
        ↓
display Java-rendered scene
        ↓
interact
        ↓
resize correctly
        ↓
close cleanly
```

Once that works in the real editor lifecycle, additional capabilities can be layered on deliberately.

Avoid designing a complete multi-platform, multi-renderer, crash-recovering rendering framework before the first Code OSS vertical slice exists.

### 15.19 Proposed Architectural Principle

The production integration should preserve this separation:

```text
Code OSS owns
    editor UX
    editor layout
    editor lifecycle
    browser input
    browser presentation

Electron native integration owns
    renderer-session lifecycle
    Java process lifecycle
    shared native resources
    native capability transfer
    Chromium SharedTexture bridge

JScene3D Java owns
    scene behavior
    scene rendering
    OpenGL rendering state
```

The POC demonstrates that these three layers can cooperate without requiring a native Java child window or CPU framebuffer-copy path.

That is the proposed foundation for the Code OSS implementation.

## 16. Production Design Questions Still Open

The Electron POC deliberately stopped once the core rendering architecture and lifecycle had been proven.

The Code OSS implementation therefore begins with substantially reduced technical risk, but several production architecture decisions remain open.

These questions should be resolved from the requirements and existing architecture of JScene3D and Code OSS rather than by treating POC choices as defaults.

### 16.1 Renderer Session Model

The POC supports one current renderer session.

Code OSS may need to support:

```text
multiple JScene3D documents
multiple editor groups
multiple visible JScene3D editors
multiple views of the same document
background editors
restored editors
```

The production design must determine what a renderer session represents.

Possible models include:

```text
one renderer session per editor instance

one renderer session per document

one renderer session shared by multiple views of one document

multiple logical renderer sessions hosted by one Java process
```

This decision affects:

- Java process count;
- scene ownership;
- native surface ownership;
- editor lifecycle;
- failure isolation;
- memory usage;
- session identifiers; and
- renderer protocol design.

The native APIs should not unnecessarily assume a single application-global renderer before this question is resolved.

### 16.2 Java Process Model

Closely related to renderer-session identity is the Java process model.

The POC uses:

```text
one Java process
    ↓
one renderer
```

Production could retain this initially, but it should be a deliberate decision.

Questions include:

- Is JVM startup cost acceptable per editor?
- Should multiple editors share a JVM?
- Is process isolation valuable for renderer crashes?
- How much memory does each renderer require?
- Can multiple OpenGL contexts coexist efficiently?
- Does JScene3D already have lifecycle abstractions that favor one model?
- Should renderer processes survive editor tab changes?
- Should a renderer process survive temporary editor invisibility?

The first implementation should prefer simplicity unless actual requirements justify process sharing.

### 16.3 Authoritative Scene and Document State

The POC's scene is disposable.

A production editor is not.

The architecture must establish where authoritative scene/document state lives.

Questions include:

```text
Is the Java renderer itself authoritative?

Does Code OSS own an editor/document model?

Does an existing JScene3D Java model remain authoritative?

How are unsaved edits represented?

How can a restarted renderer reconstruct the current scene?
```

Renderer restart has already been proven technically, but meaningful recovery requires enough state to recreate what the user was editing.

This should be resolved using the existing JScene3D document/model architecture rather than introducing a second competing representation unnecessarily.

### 16.4 Production Renderer Protocol

The POC protocol is intentionally minimal and line-oriented.

Production must decide whether that remains sufficient.

Questions include:

- Should messages be structured rather than textual?
- Is stdin/stdout the appropriate long-term transport?
- Should commands and events have explicit types?
- Should messages carry session IDs?
- Should frame commands and acknowledgements carry correlation IDs?
- Should surface transitions carry unique surface IDs?
- How are renderer errors represented?
- How are malformed or unsupported messages handled?
- Is protocol versioning required?
- How is backward compatibility handled if Java and Electron components can differ in version?
- Are request/response semantics required for editor commands?

The POC proves the lifecycle semantics but does not prescribe the serialization format.

### 16.5 Surface Transition Identity

The POC validates `SURFACE_READY` using:

```text
width
height
```

This is sufficient for the tested single-pending-surface model.

Production should consider whether a stronger identity is useful.

For example:

```text
surface generation
surface ID
transition sequence number
```

could make diagnostics and asynchronous state transitions clearer.

A production acknowledgement could conceptually become:

```text
SURFACE_READY <surfaceId> <width> <height>
```

This is not required by the POC result, but it should be considered before the production protocol is fixed.

### 16.6 Frame Identity

The POC sends:

```text
FRAME <frameNumber>
```

but receives:

```text
FRAME_READY
```

without echoing the frame number.

Because only one frame can be outstanding, this is unambiguous.

Production should decide whether frame acknowledgements should carry explicit identity:

```text
FRAME 123
        ↓
FRAME_READY 123
```

This may improve:

- diagnostics;
- stale-event detection;
- future frame dropping;
- performance measurement; and
- any later move beyond a strict single-frame-in-flight pipeline.

### 16.7 Frame Scheduling Policy

The POC continuously requests frames because the cube is always rotating.

A real editor may contain both animated and static scenes.

Production needs a policy for states such as:

```text
visible + changing
visible + static
actively interacting
background editor
hidden editor
occluded application
minimized application
renderer temporarily unavailable
```

Possible rendering strategies include:

```text
continuous while animated

render on scene change

render on interaction

pause when hidden

reduced rate when backgrounded
```

The POC proves that consumer-driven back-pressure works but does not define the final scheduling policy.

### 16.8 Refresh Rate and Animation Timing

The POC advances rotation once per requested frame.

Production animation should not depend implicitly on frame count.

If JScene3D scenes contain time-based animation, rendering should use an appropriate clock or elapsed-time model.

Questions include:

- Should rendering target the display refresh rate?
- Should Code OSS/browser scheduling influence Java frame timing?
- Should Java own the animation clock?
- How should dropped or delayed frames affect simulation?
- Should rendering and scene simulation be decoupled?

These questions are outside the POC's scope.

### 16.9 Resize Scheduling

The POC uses approximately:

```text
75 ms
```

of browser-side debounce.

Production needs to balance:

```text
visual responsiveness
        vs
IOSurface allocation churn
```

Possible strategies include:

- debounce;
- throttle;
- resize only after layout settles;
- use a temporary scaled old frame during active resize;
- allocate at selected size intervals;
- immediate resize for large changes but coalesce small changes.

The native renderer/session layer should remain correct regardless of the chosen UI policy.

### 16.10 Device Pixel Ratio Changes

The POC validates Retina scaling, but production should explicitly handle device-pixel-ratio changes that occur without a conventional editor resize.

For example, the application may move between displays with different scale factors.

The editor should reconcile:

```text
CSS viewport size
        +
current devicePixelRatio
        ↓
desired physical surface size
```

whenever either input changes.

### 16.11 Editor Visibility and Lifetime

Code OSS editors can become hidden without being destroyed.

Production must determine what happens to the Java renderer when an editor:

```text
moves to a background tab
becomes hidden behind another editor
moves between editor groups
is temporarily detached from layout
is restored
is closed
```

Possible policies include:

```text
keep renderer and pause frames

keep renderer at reduced activity

destroy native surface but retain Java scene

destroy renderer session entirely

share renderer with document state
```

The POC does not answer this.

### 16.12 Multiple Editor Views

If Code OSS permits two visible views of the same JScene3D document, the design must decide whether they share:

```text
scene
camera
renderer process
render surface
```

or have independent view state.

A shared scene with different cameras may require:

```text
one document model
multiple renderer views
multiple surfaces
```

This should be driven by editor requirements rather than constrained by the single-view POC.

### 16.13 Renderer Crash Detection

The POC tests deliberate graceful shutdown.

Production must also handle unexpected Java termination.

The renderer/session layer should be able to detect:

```text
process exits unexpectedly
stdout closes
pipe write fails
renderer stops responding
native control channel disappears
```

and transition the editor into an explicit failed state.

A Java PID existing at one point is not sufficient renderer-health information.

### 16.14 Renderer Recovery

Because renderer recreation has been proven, production can potentially recover from renderer failure.

However, recovery policy remains open.

Questions include:

- Should restart happen automatically?
- How many restart attempts are appropriate?
- Should the user be notified?
- What happens to unsaved scene state?
- Should the last rendered frame remain visible?
- How are repeated renderer crashes handled?
- Should some failures disable automatic restart?

The restart mechanism and the recovery UX are separate concerns.

### 16.15 Startup Failure

`RENDERER_READY` establishes successful initialization, but production also needs a defined failure path when readiness never occurs.

Possible failures include:

```text
JVM launch failure
JNI library load failure
Mach rendezvous failure
IOSurface lookup failure
GLFW initialization failure
OpenGL context failure
render-target failure
```

The renderer/session state machine should support explicit startup failure rather than waiting indefinitely for `RENDERER_READY`.

Timeout behavior may also be required.

### 16.16 Surface Replacement Failure

The POC logs failures during replacement.

Production needs a defined recovery policy.

If Java cannot adopt a replacement surface, possible responses include:

```text
retain old active surface
retry latest desired size
recreate renderer
report renderer failure
```

The active/pending model already provides a useful basis because the old surface remains active until promotion.

Production should exploit that property deliberately.

### 16.17 GPU and WebGPU Failure

The POC assumes successful WebGPU initialization and presentation.

Production must consider:

```text
WebGPU adapter unavailable
device creation failure
device loss
external texture import failure
SharedTexture failure
GPU process restart
```

The editor should have a defined failure state rather than silently becoming blank.

Whether any software/fallback rendering path is required is a product decision.

### 16.18 Native Resource Limits and Memory Pressure

Large Retina IOSurfaces consume significant GPU/shared memory.

For example, a BGRA surface around:

```text
2200×1536
```

contains millions of pixels before accounting for additional rendering resources.

Multiple editors or pending surface transitions can multiply this cost.

Production should consider:

- number of simultaneously retained surfaces;
- active plus pending surface memory;
- hidden editor resources;
- multiple renderer sessions;
- depth buffers;
- Chromium-side imported resources; and
- cleanup under memory pressure.

The POC proves resource lifetime behavior but does not establish production limits.

### 16.19 Java Runtime Packaging and Launch

The POC launches:

```text
java
```

from the development environment and uses a generated classpath.

Production must determine:

- which Java runtime is used;
- whether a runtime is bundled;
- how the renderer classpath/module path is assembled;
- how native JNI libraries are located;
- how application installation paths are resolved;
- how development and packaged modes differ; and
- how Java/JNI version compatibility is enforced.

This should integrate with the existing JScene3D build and distribution architecture.

### 16.20 Native Library Packaging

The POC builds JNI/native code directly inside the Electron test renderer directory.

Production needs a defined home and packaging strategy for:

```text
IOSurface JNI bridge
Mach rendezvous native code
control-channel native code
platform-specific libraries
```

These should fit the existing JScene3D Maven/native module organization where appropriate.

The POC directory structure is not a production recommendation.

### 16.21 Security and Trust Boundaries

The POC focuses on technical feasibility.

Production should review:

- Java process arguments;
- native library loading;
- renderer command validation;
- malformed protocol input;
- native message validation;
- Mach right ownership;
- renderer process trust;
- Code OSS context isolation;
- exposed preload/native APIs; and
- file/document inputs supplied to Java.

The existing POC already uses:

```text
contextIsolation: true
nodeIntegration: false
```

for the standalone page, but a full security review belongs to the production integration.

### 16.22 Diagnostics and Logging

The POC benefited significantly from detailed lifecycle logs such as:

```text
Created JScene3D surface
Launched Java renderer PID
JNI received IOSurface Mach port
JScene3D renderer ready
Sent JScene3D replacement surface
Promoted JScene3D surface
```

and from retaining raw Mach failure codes.

Production should preserve useful diagnostics while avoiding uncontrolled console noise.

It may be useful to define renderer-session logging around:

```text
session ID
renderer PID
surface generation
frame generation
viewport dimensions
lifecycle state
native failure code
```

This will be particularly valuable for debugging cross-process failures.

### 16.23 Automated Testing

The POC was validated manually through visible rendering and terminal output.

Production should add automated coverage where practical.

Important behaviors include:

```text
renderer startup
renderer readiness
surface creation
surface replacement
repeated replacement
resize coalescing
renderer shutdown
renderer recreation
stale callback rejection
protocol parsing
invalid dimensions
failure cleanup
```

Native integration tests may still require macOS-specific execution, but pure lifecycle and protocol components should be unit-testable independently.

### 16.24 Cross-Platform Architecture

Only macOS has been proven.

Windows and Linux require separate investigation into equivalent mechanisms for:

```text
GPU-shareable native surface
inter-process resource transfer
Chromium SharedTexture import
Java/OpenGL binding
```

The production abstractions should avoid unnecessarily preventing those implementations.

However, do not delay the proven macOS integration by attempting to design speculative platform implementations in excessive detail.

The appropriate goal is:

```text
platform-neutral lifecycle concepts
        +
concrete proven macOS implementation
```

not:

```text
pretend all three platforms are already solved
```

### 16.25 Code OSS Integration Points

The exact Code OSS services and editor contribution points have not been specified by the POC.

Codex should inspect the target Code OSS source before proposing these.

Areas that will likely require mapping include:

```text
custom editor/editor pane lifecycle
renderer-process/browser-process IPC
preload or exposed Electron capability
workbench services
editor visibility
editor layout
resource disposal
application shutdown
```

Existing Code OSS patterns should be preferred where they already solve the relevant lifecycle problem.

### 16.26 Avoid Premature Decisions

The purpose of documenting these open questions is not to require all of them to be solved before implementation begins.

The first Code OSS vertical slice should remain focused.

Questions should be resolved when they become necessary to establish a clean architecture for the next working increment.

The POC has already removed the need for speculative work on the fundamental rendering mechanism.

The production implementation can now proceed incrementally while keeping these open design areas visible.

## 17. Recommended Code OSS Implementation Sequence

The Code OSS integration should be implemented incrementally, using the working Electron POC as the reference whenever native behavior is uncertain.

The objective of the first implementation phase is not to build every production feature identified in Section 16.

The objective is to establish the smallest complete vertical slice inside the real Code OSS editor architecture while preserving the lifecycle boundaries proven by the POC.

The recommended sequence is deliberately ordered so that each stage produces a testable result before the next responsibility is added.

### 17.1 Begin by Inspecting the Existing Architecture

Before changing code, Codex should inspect the relevant existing Code OSS and JScene3D implementation.

Do not begin by copying files from:

```text
/Users/glynch/development/projects/jscene3d-electron
```

into the current workspace.

First determine:

- how the JScene3D editor should participate in the Code OSS editor lifecycle;
- which existing Code OSS services should own renderer/session functionality;
- how renderer-process/browser-process communication is normally implemented in this Code OSS version;
- what lifecycle/disposal patterns existing custom editors use;
- where browser-side editor presentation belongs;
- how the existing JScene3D Maven modules divide rendering, platform and application responsibilities; and
- whether existing JScene3D abstractions already correspond to concepts introduced by the POC.

The result of this inspection should be a proposed integration design mapped onto the actual repositories, not an abstract recreation of the POC structure.

### 17.2 Define the First Vertical Slice

The first useful Code OSS milestone should be intentionally small.

A suitable target is:

```text
open JScene3D editor
        ↓
create renderer session
        ↓
launch Java renderer
        ↓
Java renders a simple scene
        ↓
IOSurface reaches Chromium
        ↓
WebGPU presents it in the editor
        ↓
editor closes
        ↓
renderer resources are released
```

At this stage it is acceptable to use a simple known scene.

The purpose is to prove that the POC rendering mechanism has been transplanted successfully into the actual Code OSS lifecycle.

Do not initially combine this milestone with:

```text
full scene editing
multiple renderer sessions
crash recovery
cross-platform support
complex input tools
production protocol redesign
```

unless the existing architecture makes one of those unavoidable.

### 17.3 Establish the Native Electron Surface Abstraction

Port or adapt the proven shared-surface capability into the Code OSS Electron fork.

The POC references are:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_surface.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/
```

The initial production implementation should establish a native abstraction capable of:

```text
create shared render surface

report width/height

produce transferable native resource

produce Chromium-compatible shared-texture handle

release surface deterministically
```

The higher-level session implementation should not need to manipulate IOSurface APIs directly.

At this stage, macOS is the required concrete implementation because that is the platform proven by the POC.

### 17.4 Establish Initial Mach Rendezvous

Implement the initial renderer bootstrap using the proven Mach-right approach.

Use the POC as reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.c

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.h
```

The initial target is:

```text
Electron creates IOSurface
        ↓
launch Java
        ↓
Java receives Mach right
        ↓
IOSurfaceLookupFromMachPort succeeds
```

Do not introduce global IOSurface IDs as a shortcut.

The non-global capability-transfer architecture is already proven.

### 17.5 Establish the Java Shared Render Target

Integrate the Java-side IOSurface render-target capability into the appropriate existing JScene3D Maven modules.

POC references:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceBridge.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceDescriptor.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.c

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.h
```

The first Java milestone is:

```text
receive native surface
        ↓
bind IOSurface to OpenGL texture
        ↓
attach to framebuffer
        ↓
render known content
```

Do not initially redesign unrelated JScene3D rendering architecture.

Introduce only the abstraction necessary to allow existing JScene3D rendering to target the shared surface cleanly.

### 17.6 Add Explicit Renderer Readiness

Do not let Code OSS infer readiness from successful Java process launch.

Preserve:

```text
process launched
        ↓
Java initializes
        ↓
render target exists
        ↓
RENDERER_READY
        ↓
renderer may now receive normal work
```

The production representation of this event can differ from the POC protocol, but the state transition should be explicit.

The Code OSS renderer/session service should be able to distinguish at least:

```text
starting
ready
stopping
stopped
failed
```

even if the first implementation exposes only part of that state externally.

### 17.7 Establish SharedTexture Presentation in the Actual Editor

Once Java can render into the shared surface, connect that surface to the actual Code OSS editor viewport.

POC references:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js
```

The Code OSS path should preserve:

```text
IOSurface
        ↓
Chromium SharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture
        ↓
actual JScene3D editor viewport
```

At the end of this step, a Java-rendered image should appear inside the real Code OSS editor.

This is the first major integration checkpoint.

### 17.8 Add Frame Handshake and Back-Pressure

Once a single frame can be presented, introduce the renderer frame lifecycle.

Preserve the proven conceptual loop:

```text
request frame
        ↓
Java renders
        ↓
frame ready
        ↓
present shared texture
        ↓
presentation completes
        ↓
request next frame when required
```

Do not initially optimize for maximum frame rate.

First establish:

```text
correct ordering
one frame in flight
deterministic resource lifetime
clean shutdown
```

Then measure actual performance before changing the scheduling model.

### 17.9 Integrate Actual JScene3D Rendering

After the shared rendering path works inside Code OSS, replace any temporary integration scene with the appropriate existing JScene3D renderer/scene path.

This is the point where the current workspace architecture becomes more important than the POC.

Codex should identify:

- the existing scene representation;
- the existing rendering entry points;
- how cameras are represented;
- how rendering is currently initiated;
- which module should own the shared render target; and
- how an editor document selects the scene to render.

Avoid creating a parallel POC-style scene architecture if the existing JScene3D code already provides these concepts.

### 17.10 Add Basic Editor Interaction

Once real JScene3D content renders, add the smallest useful interaction path.

A reasonable first interaction is camera orbit or another existing JScene3D camera operation.

The POC reference for the communication path is:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

The production implementation should prefer semantic editor operations over mechanically copying the POC's `DRAG dx dy` command if the existing JScene3D API provides a better abstraction.

At the end of this step:

```text
user input in Code OSS
        ↓
Java scene/camera changes
        ↓
next shared frame reflects change
```

should work.

### 17.11 Add Persistent Viewport State

Before implementing dynamic surface replacement, model the editor's desired physical viewport dimensions explicitly.

Conceptually:

```text
desiredViewport = {
    width,
    height
}
```

This state should exist independently of:

```text
current active surface
pending surface
renderer readiness
individual ResizeObserver events
```

This avoids rediscovering the restart-state problem from the POC.

### 17.12 Add Dynamic Surface Replacement

Next introduce the persistent Mach control channel and replacement lifecycle.

POC references:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.c

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.h
```

Implement the full acknowledged transition:

```text
desired size changes
        ↓
create pending surface
        ↓
transfer native resource
        ↓
Java adopts resource
        ↓
surface-ready acknowledgement
        ↓
promote pending surface
```

Do not promote a surface merely because its Mach right was sent successfully.

### 17.13 Connect Resize to the Actual Code OSS Editor Viewport

Observe the actual JScene3D editor viewport rather than the outer Code OSS application window.

The resize path should calculate:

```text
CSS width
    ×
devicePixelRatio
    =
physical surface width
```

and likewise for height.

Verify at minimum:

```text
normal editor resize
editor-group resize
window resize
maximized/restored layout
Retina dimensions
```

If practical, also test movement between displays with different scaling.

### 17.14 Add Resize Flow Control

Only after correct resize works should resize-frequency optimization be added.

Use the POC result as guidance:

```text
browser/layout event coalescing
        +
native pending-transition correctness
```

Do not rely on debounce for correctness.

Do not rely on native correctness as justification for allocating a surface for every intermediate layout pixel.

Measure the real Code OSS layout behavior before selecting the final debounce/throttle value.

### 17.15 Validate Repeated Surface Replacement

Do not stop after one successful resize.

Explicitly test:

```text
A → B → C → D → E
```

while:

```text
rendering continues
interaction continues
```

This test is what exposed the POC control-channel lifetime bug.

Repeated replacement should be part of the initial integration acceptance criteria.

### 17.16 Add Renderer Shutdown

Integrate renderer teardown with the actual Code OSS editor/session lifecycle.

Verify deterministic cleanup of:

```text
frame scheduling
pending asynchronous callbacks
SharedTexture wrappers
native surfaces
pending surfaces
control channel
Java stdin/stdout
Java process
JNI/OpenGL resources
```

Closing a JScene3D editor should not leave an orphan Java renderer unless the chosen production process model intentionally retains it.

### 17.17 Validate Renderer Recreation

Once normal startup, rendering, resize and shutdown work, repeat the POC restart test inside the real integration.

Test:

```text
renderer 1 starts
        ↓
renders
        ↓
resizes
        ↓
stops
        ↓
renderer 2 starts
        ↓
desired viewport reapplied
        ↓
renders
        ↓
interacts
        ↓
resizes again
```

This verifies that the production Code OSS lifecycle has not accidentally introduced global one-shot state.

### 17.18 Add Failure Handling After the Happy Path Works

Only after the complete vertical slice works should the implementation expand failure handling systematically.

Prioritize failures that are realistic at integration boundaries:

```text
Java launch failure
renderer readiness failure
unexpected Java exit
surface creation failure
surface transfer failure
SharedTexture presentation failure
```

Represent these through explicit renderer/session state rather than scattered console logging.

### 17.19 Add Tests Alongside Production Components

Where responsibilities can be tested without the complete GPU pipeline, add focused tests as the implementation is introduced.

Good candidates include:

```text
renderer state transitions
protocol parsing
desired viewport reconciliation
surface-transition state
resize coalescing
stale session/generation handling
process-output line buffering
invalid dimension handling
```

Platform integration tests should then cover the actual macOS path.

The POC repository remains useful as a known-working comparison when integration tests fail.

### 17.20 Delay Cross-Platform Work Until the macOS Vertical Slice Is Stable

The macOS architecture is proven.

Windows and Linux are not.

The initial implementation should preserve appropriate platform boundaries but should not block the Code OSS macOS integration on speculative equivalents for:

```text
IOSurface
Mach ports
Mach rendezvous
```

Once the production macOS vertical slice is stable, equivalent platform mechanisms can be investigated against a concrete abstraction rather than an imagined one.

### 17.21 Recommended Checkpoint Sequence

A practical checkpoint sequence is:

```text
Checkpoint 1
Code OSS editor exists and owns a JScene3D viewport

Checkpoint 2
Java renderer launches from Code OSS and reaches RENDERER_READY

Checkpoint 3
One Java-rendered IOSurface frame appears in the editor

Checkpoint 4
Continuous/back-pressured rendering works

Checkpoint 5
Basic interaction reaches the Java scene

Checkpoint 6
Actual JScene3D scene rendering replaces temporary integration content

Checkpoint 7
Physical viewport sizing and repeated IOSurface replacement work

Checkpoint 8
Resize flow control works under interactive layout changes

Checkpoint 9
Editor close tears renderer resources down cleanly

Checkpoint 10
Renderer can be recreated and resume rendering in the same Code OSS process
```

Each checkpoint should be kept working before proceeding.

### 17.22 Implementation Principle

When uncertainty arises about native behavior, inspect and reproduce the known-working mechanism from:

```text
/Users/glynch/development/projects/jscene3d-electron
```

When uncertainty arises about where that mechanism belongs in production, inspect the existing JScene3D and Code OSS architecture.

In other words:

```text
POC answers:
    "How does the native rendering mechanism work?"

Existing production code answers:
    "Where should this responsibility live?"
```

The implementation should combine those two sources rather than allowing either the standalone POC or an abstract redesign to dominate the production architecture.

## 18. POC Reference Source Inventory

The completed Electron POC remains the primary executable reference for the native rendering mechanisms described in this handover.

POC repository root:

```text
/Users/glynch/development/projects/jscene3d-electron
```

The files below are grouped by responsibility so Codex can locate the relevant implementation quickly when working on the Code OSS integration.

This inventory is a reference map, not a list of files that should be copied directly into the production workspace.

### 18.1 Electron Native API Binding

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc
```

This is the JavaScript/native binding used by the POC.

It provides the bridge from Electron JavaScript into the native renderer-session implementation.

Relevant concepts include:

```text
renderer-session creation
callback installation before launch
Java renderer launch
current surface access
renderer commands
resize requests
renderer shutdown
```

Use this file when tracing how the JavaScript test harness enters the native renderer architecture.

### 18.2 Renderer Session

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc
```

These files contain the central renderer-session orchestration.

Relevant concepts include:

```text
initial surface creation
control-channel creation
Java process coordination
active surface ownership
pending surface ownership
surface replacement
renderer protocol interpretation
RENDERER_READY handling
FRAME_READY handling
SURFACE_READY handling
surface promotion
session shutdown
```

These are the most important files to inspect when understanding the complete native lifecycle.

### 18.3 Java Child Process Management

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc
```

These files isolate process and stream mechanics from renderer semantics.

Relevant concepts include:

```text
Java command-line construction
base::LaunchProcess
stdin pipe
stdout pipe
FileDescriptorWatcher
partial stdout buffering
line parsing
command writes
QUIT
process cleanup
```

Use these files when implementing or debugging Java child-process lifecycle and renderer-protocol transport.

### 18.4 Renderer Protocol

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_protocol.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/RendererProtocol.java
```

These contain the centralized protocol vocabulary used by the POC.

The conceptual protocol includes:

```text
Electron → Java

DRAG
FRAME
RECEIVE_SURFACE
QUIT


Java → Electron

RENDERER_READY
FRAME_READY
SURFACE_READY
```

Use these files to understand the tested lifecycle semantics.

Do not assume the textual protocol representation is the required production design.

### 18.5 Shared Surface Abstraction

Generic reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_surface.h
```

Platform implementation files are under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/
```

Relevant concepts include:

```text
shared surface creation
surface dimensions
IOSurface ownership
Mach send-right creation
Chromium shared-texture handle conversion
```

Use these files when implementing the native surface abstraction and macOS IOSurface support.

### 18.6 Persistent Control Channel

Electron side:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm
```

Java/JNI side:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.c
```

Relevant concepts include:

```text
Mach control-port creation
receive-right transfer during bootstrap
persistent Electron send right
persistent Java receive right
replacement IOSurface message
Mach port descriptor transfer
surface dimensions in native message
repeated resource transfer
control-channel cleanup
```

These files are particularly important because the POC discovered the control-channel lifetime bug while exercising repeated surface replacement.

### 18.7 Mach Rendezvous Bootstrap

Java/JNI references:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_rendezvous.c
```

Relevant concepts include:

```text
parent Electron Mach rendezvous service
initial IOSurface Mach port acquisition
control-channel receive-right acquisition
bootstrap naming
Mach right ownership
```

Use these files when implementing or debugging the initial Electron → Java native-resource bootstrap.

### 18.8 IOSurface JNI Bridge

Java reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceBridge.java
```

JNI reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c
```

Relevant concepts include:

```text
initial IOSurface lookup
control-channel setup
OpenGL texture binding
replacement surface receive
IOSurface release
JNI handle conversion
```

The final `release()` implementation is especially important.

It releases the individual IOSurface but does not close the persistent control channel.

### 18.9 Native IOSurface Operations

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.h

/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_iosurface.c
```

Relevant concepts include:

```text
IOSurfaceLookupFromMachPort
IOSurface lifetime
OpenGL texture binding
native surface release
```

Use these files for the Java-side native IOSurface/OpenGL integration.

### 18.10 Java Surface Descriptor

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceDescriptor.java
```

This represents a replacement surface received by Java.

Conceptually it contains:

```text
native surface handle
width
height
```

The production equivalent should fit the existing JScene3D architecture rather than necessarily retaining this exact class.

### 18.11 Java IOSurface Render Target

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java
```

This is the primary Java/OpenGL render-target reference.

Relevant concepts include:

```text
IOSurface-backed OpenGL texture
framebuffer
depth renderbuffer
framebuffer validation
surface replacement
depth-buffer resize
glViewport
projection/aspect-ratio update
previous-surface release
OpenGL resource cleanup
```

Use this file when integrating the shared native surface with the real JScene3D rendering architecture.

### 18.12 POC Java Renderer

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java
```

This is the Java renderer process used to validate the architecture.

Relevant concepts include:

```text
renderer process entry point
initial IOSurface acquisition
GLFW initialization
hidden OpenGL context
LWJGL capability initialization
IOSurfaceRenderTarget construction
RENDERER_READY
command processing
FRAME
FRAME_READY
DRAG
RECEIVE_SURFACE
SURFACE_READY
QUIT
continuous animation
```

The cube rendering code itself is not production architecture.

### 18.13 Browser Test Harness

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js
```

This file contains the final standalone Electron lifecycle test.

Relevant concepts include:

```text
BrowserWindow creation
renderer launch
renderer callbacks
frameInFlight
frame numbering
SharedTexture import
SharedTexture presentation
desired viewport state
renderer readiness
continuous frame scheduling
drag forwarding
resize forwarding
renderer restart
renderer-generation protection
shutdown
```

This is a useful end-to-end reference because it shows how the individual native mechanisms were composed into the final working POC.

It should not be copied as a production Code OSS service.

### 18.14 Browser Preload

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js
```

Relevant concepts include:

```text
context-isolated browser API
shared-texture receiver
VideoFrame acquisition
resource cleanup
drag forwarding
resize forwarding
renderer/browser readiness
```

Use this as a reference for the browser/native boundary proven by the POC.

The Code OSS integration should use the appropriate existing application boundary rather than assuming a standalone preload API is required in the same form.

### 18.15 Browser WebGPU Renderer

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js
```

Relevant concepts include:

```text
WebGPU adapter/device setup
external texture binding
VideoFrame → WebGPU import
fullscreen texture presentation
normalized UV coordinates
ResizeObserver
CSS → physical pixel conversion
devicePixelRatio
resize debounce
pointer input
```

This is the main reference for the browser-side presentation mechanism.

### 18.16 POC HTML

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/index.html
```

This file is useful mainly for understanding the final canvas-layout behavior.

The important result is that the rendering canvas fills its available content area rather than remaining fixed at `800×600`.

The standalone HTML page itself has no proposed production role.

### 18.17 Renderer Build Files

The Java/native POC renderer is under:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/
```

This directory also contains the POC build support used for:

```text
Java compilation
JNI header generation
native library build
LWJGL dependencies/classpath
```

These build files are useful when reproducing the POC locally or checking native compilation details.

They should not determine the production Maven/build organization.

### 18.18 Electron Build Registration

The POC added its native Electron sources to Electron's build source inventory.

Relevant reference:

```text
/Users/glynch/development/projects/jscene3d-electron/src/electron/filenames.gni
```

When adapting the native implementation into the Code OSS Electron fork, ensure new native sources are registered through the appropriate Electron build configuration.

Do not assume the exact source list will remain identical if the production implementation reorganizes the POC classes.

### 18.19 Recommended Reference Order

When investigating the POC from scratch, the following reading order provides a useful top-down view:

```text
1.
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/test-jscene3d-binding.js

2.
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/api/electron_api_jscene3d_renderer.cc

3.
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_session.cc

4.
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_renderer_process.cc

5.
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/jscene3d_surface.h
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/

6.
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/CubeRenderer.java

7.
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceRenderTarget.java

8.
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/src/main/java/poc/IOSurfaceBridge.java
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/iosurface_bridge.c

9.
/Users/glynch/development/projects/jscene3d-electron/src/electron/shell/browser/jscene3d/mac/jscene3d_control_channel_mac.mm
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer/native/jscene3d_control_channel.c

10.
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/preload.js
/Users/glynch/development/projects/jscene3d-electron/src/electron/spec/jscene3d/renderer.js
```

This moves from:

```text
end-to-end orchestration
        ↓
native API
        ↓
session lifecycle
        ↓
process lifecycle
        ↓
native surface
        ↓
Java renderer
        ↓
Java render target
        ↓
JNI
        ↓
Mach replacement channel
        ↓
browser presentation
```

### 18.20 Reference Principle

When Codex encounters a question about how the native mechanism behaves, inspect the POC source directly.

The POC repository contains the exact implementation that successfully demonstrated:

```text
initial IOSurface bootstrap
repeated IOSurface replacement
continuous rendering
frame back-pressure
browser interaction
Retina resizing
renderer shutdown
renderer recreation
```

Use that implementation to avoid re-solving already answered native problems.

At the same time, use the current JScene3D and Code OSS workspace to decide where the corresponding production responsibility belongs.

## 19. Acceptance Criteria for the Initial Code OSS Integration

The first Code OSS integration should have a clear stopping point.

The objective is not to reproduce every possible production feature before the integration can be considered successful. The initial milestone should demonstrate that the architecture proven in the standalone Electron POC works correctly inside the real Code OSS and JScene3D lifecycle.

The following criteria define that milestone.

### 19.1 JScene3D Editor Integration

A JScene3D resource can be opened through the intended Code OSS editor architecture.

The visible 3D viewport is part of the normal Code OSS workbench rather than a separate native Java/GLFW window or standalone Electron test window.

The editor participates correctly in normal Code OSS lifecycle behavior, including creation, layout and disposal.

### 19.2 Java Renderer Launch

Opening the JScene3D editor can create or acquire the required renderer session and launch the Java renderer according to the production process model chosen during implementation.

The implementation can distinguish:

```text
process launched
```

from:

```text
renderer ready
```

Normal rendering does not begin until Java has completed the required renderer initialization.

### 19.3 Initial Native Surface Bootstrap

Electron can create the initial shared rendering surface and transfer it to Java using the proven macOS Mach rendezvous mechanism.

Java can successfully:

```text
receive IOSurface Mach right
        ↓
IOSurfaceLookupFromMachPort
        ↓
bind IOSurface to OpenGL
        ↓
construct usable render target
```

The production integration must not depend on globally discoverable IOSurface IDs.

### 19.4 Real JScene3D Rendering

The Java renderer renders actual JScene3D content through the production JScene3D rendering path.

A temporary diagnostic scene may be used during earlier implementation checkpoints, but the initial integration should not be considered complete while it still depends solely on the POC cube renderer.

The shared render target should integrate with the existing JScene3D rendering architecture rather than establish a parallel POC-only rendering system.

### 19.5 GPU-Shared Presentation

The rendered output reaches the Code OSS editor through the proven GPU-sharing path:

```text
Java / OpenGL
        ↓
IOSurface
        ↓
Chromium SharedTexture
        ↓
VideoFrame
        ↓
WebGPU external texture
        ↓
JScene3D editor viewport
```

The normal rendering path must not introduce:

```text
glReadPixels
CPU framebuffer copies
encoded image transfer
ImageData upload
```

between Java rendering and Chromium presentation.

### 19.6 Sustained Frame Rendering

The integration can render more than a single diagnostic frame.

A sustained sequence of frames can pass through the complete pipeline without resource corruption or unbounded frame accumulation.

The implementation has an explicit frame-pacing/back-pressure mechanism.

At minimum, the first integration should preserve the proven invariant:

```text
do not allow Java to produce an unlimited queue of frames
```

The exact production scheduler may differ from the POC.

### 19.7 Browser-to-Java Interaction

User interaction originating in the Code OSS editor can modify the Java-side scene or camera and the result appears in subsequent rendered frames.

The exact first interaction may be selected based on the existing JScene3D APIs.

A suitable initial example is:

```text
camera orbit
```

or another simple existing camera operation.

The acceptance requirement is the complete round trip:

```text
Code OSS input
        ↓
renderer command/operation
        ↓
Java/JScene3D state change
        ↓
new rendered frame
        ↓
Code OSS viewport
```

### 19.8 Actual Editor Viewport Sizing

The native rendering surface follows the dimensions of the actual JScene3D editor viewport.

It must not be tied to:

```text
outer application window dimensions
```

or:

```text
a fixed 800×600 size
```

The integration correctly distinguishes CSS dimensions from physical rendering dimensions.

### 19.9 Device Pixel Ratio

The desired native rendering dimensions account for the current device pixel ratio.

Conceptually:

```text
physicalWidth =
    cssWidth × devicePixelRatio

physicalHeight =
    cssHeight × devicePixelRatio
```

The initial macOS integration should render at the expected Retina physical resolution rather than unintentionally producing a low-resolution logical-pixel surface that Chromium then scales.

### 19.10 Dynamic Surface Replacement

Changing the JScene3D editor viewport size can replace the active native rendering surface without restarting Java.

The transition follows the acknowledged lifecycle:

```text
desired size changes
        ↓
create replacement
        ↓
replacement becomes pending
        ↓
transfer replacement to Java
        ↓
Java adopts replacement
        ↓
Java acknowledges replacement
        ↓
Electron promotes replacement
```

The previous active surface is not discarded before successful adoption of the replacement.

### 19.11 Repeated Surface Replacement

The implementation must be tested with repeated resize transitions.

One successful resize is insufficient.

The required test is conceptually:

```text
A → B → C → D → E
```

while the same Java renderer remains alive.

This specifically protects against recurrence of the control-channel lifetime bug discovered in the POC.

### 19.12 Rendering and Interaction After Resize

After repeated surface replacement:

```text
rendering still works
interaction still works
scene state remains intact
additional resize remains possible
```

Ordinary viewport resize must not recreate the scene or Java renderer.

### 19.13 Resize Flow Control

Interactive editor resizing does not allocate a new IOSurface for every intermediate pixel-sized layout event.

The browser/editor side coalesces high-frequency viewport changes appropriately.

The exact timing strategy is not prescribed by this handover.

The native renderer/session layer must nevertheless remain correct independently of that optimization.

### 19.14 Persistent Desired Viewport State

The current desired viewport dimensions are modeled as persistent session/editor state rather than as a consumed one-shot event.

If a renderer is recreated while the viewport remains unchanged, the replacement renderer receives the current desired dimensions after it becomes ready.

The implementation must not depend on a new `ResizeObserver` event occurring after renderer restart.

### 19.15 Renderer Shutdown

Closing or disposing the relevant JScene3D editor/session cleanly terminates or releases renderer resources according to the selected production process model.

For a renderer that is being destroyed, cleanup includes the appropriate ownership of:

```text
frame scheduling
asynchronous presentation work
active surface
pending surface
control channel
Java process communication
Java renderer resources
OpenGL resources
native IOSurface references
```

No unintended Java renderer process should remain solely because an editor was closed.

### 19.16 Renderer Recreation

The production integration can perform the lifecycle proven by the POC:

```text
renderer 1
    ↓
ready
    ↓
render
    ↓
resize
    ↓
stop

renderer 2
    ↓
new process/session resources
    ↓
ready
    ↓
restore desired viewport
    ↓
render
    ↓
interact
    ↓
resize
```

This must work without restarting Code OSS itself.

The exact reason for recreation may initially be a deliberate test rather than automatic crash recovery.

### 19.17 Stale Asynchronous Work Is Isolated

Asynchronous work belonging to an obsolete renderer/session cannot mutate a replacement renderer/session.

The production implementation may use:

```text
session identity
generation identity
cancellation
disposed-object guards
```

or an existing Code OSS lifecycle mechanism.

The exact mechanism is not prescribed.

The invariant is:

```text
old renderer callbacks
must not affect
new renderer state
```

### 19.18 Correct Native Resource Lifetime

The production implementation preserves the ownership rule discovered by the POC:

```text
individual IOSurface lifetime
        !=
control-channel lifetime
```

Replacing or releasing a surface must not destroy the persistent control channel required by the current renderer session.

Repeated replacement must continue to work after old surfaces are released.

### 19.19 Renderer Failure Has an Explicit State

The initial production integration should not silently hang or remain indefinitely in a "starting" state if Java initialization fails.

At minimum, renderer/session lifecycle should be able to represent a startup or runtime failure and expose enough information for the Code OSS editor to respond appropriately.

Full automatic recovery does not have to be part of the first milestone.

### 19.20 Resource Cleanup Is Deterministic

The integration should explicitly release native and browser-side rendering resources when their ownership ends.

This includes the distinction between:

```text
long-lived renderer/session resources

and

per-frame presentation resources
```

The implementation should not rely on garbage collection as the primary lifecycle mechanism for native surfaces, imported shared textures, `VideoFrame` objects, Java native handles, or OpenGL resources.

### 19.21 POC Regression Comparison

If a native mechanism fails during Code OSS integration, the equivalent behavior should be compared against the known-working POC under:

```text
/Users/glynch/development/projects/jscene3d-electron
```

The initial integration should not be considered complete if it works only by removing an important property already demonstrated by the POC, such as:

```text
non-global IOSurface transfer
repeated surface replacement
frame back-pressure
Retina sizing
renderer recreation
```

unless that change is an intentional and documented production design decision.

### 19.22 macOS Scope

These acceptance criteria apply to the initial macOS implementation.

The first Code OSS integration does not require Windows and Linux shared-surface implementations before the macOS architecture can be considered successful.

However, platform-neutral lifecycle code should avoid unnecessary dependencies on macOS implementation details.

### 19.23 Definition of Initial Integration Success

The first Code OSS integration can be considered successful when the following complete workflow is reliable:

```text
open JScene3D editor
        ↓
launch Java renderer
        ↓
complete Mach/IOSurface bootstrap
        ↓
RENDERER_READY
        ↓
render actual JScene3D content
        ↓
present through SharedTexture/WebGPU
        ↓
interact from Code OSS
        ↓
resize editor repeatedly
        ↓
replace IOSurfaces without restarting Java
        ↓
continue rendering and interaction
        ↓
close renderer cleanly
        ↓
create another renderer session
        ↓
resume correctly sized rendering
```

At that point the architecture proven in the standalone Electron POC will also have been demonstrated inside the real Code OSS/JScene3D environment.

Further work such as richer editing tools, multiple simultaneous renderer sessions, seamless crash recovery, production performance tuning and additional operating systems can then proceed on top of a validated production integration rather than being mixed into the initial port.
