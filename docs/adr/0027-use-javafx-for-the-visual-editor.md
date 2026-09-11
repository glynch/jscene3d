# Use JavaFX for the visual editor

The JScene3D visual editor will use JavaFX for its native desktop shell and
OpenGLFX for an embedded viewport rendered by the actual JScene3D renderer.
JavaFX provides a modern, styleable Java UI toolkit without requiring a
browser-hosted editor, while OpenGLFX avoids a second WebGL, WebGPU, or Three.js
rendering implementation whose output could diverge from the game. SWT, Swing,
Vaadin, and an immediate-mode engine UI remain rejected as the primary editor
shell.

The prototype on branch `prototype/javafx-opengl-viewport` proved that the
actual renderer can draw inside an OpenGLFX `GLCanvas`, receive physical and
logical resize information, accept JavaFX-managed input and focus, and release
its GPU resources before the externally owned context is destroyed. It also ran
as a named JPMS module and as a `jpackage` macOS application image launched from
outside the source repository.

The renderer exposes a small `RenderSurface` seam rather than depending on
JavaFX or OpenGLFX. A host adapter activates its context and presentation
framebuffer, reports one consistent physical/logical size snapshot, and releases
the renderer's exclusive access without destroying the host-owned control or
context. Frame publication remains host-owned: GLFW swaps its window buffers,
while OpenGLFX publishes after its render callback returns. This also permits
the host to add content between JScene3D rendering and presentation.

OpenGLFX currently requires narrowly targeted exports from JavaFX internals and
platform-specific native modules. Those launch and packaging requirements
belong to the editor application, never `jscene3d-lwjgl` or exported games.
The prototype qualifies the initial macOS editor; Windows and Linux require
their own render, input, high-DPI, disposal, and packaged-execution qualification
before support is claimed.

The production JavaFX workbench lives in the `jscene3d-editor` application
artifact. It owns the JavaFX shell, the OpenGLFX adapter, and editor-specific
viewport coordination. Toolkit-independent extension contracts live in
`jscene3d-editor-api`, preventing editor extensions from exposing JavaFX controls.
The API separates executable commands from their workbench placements, describes
views through stable kinds and editor-rendered models (including asynchronous
trees), owns registrations through extension-scoped subscriptions, and provides
portable status, window-message, and diagnostic publication facilities. The
initial hierarchy, asset browser, and inspector do not introduce a second project
document model; they project authored definitions through editor-owned views.
