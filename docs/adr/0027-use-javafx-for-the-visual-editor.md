# Use JavaFX for the visual editor

The JScene3D visual editor will use JavaFX for its native desktop shell and a
tested JavaFX/OpenGL bridge for an embedded viewport rendered by the actual
JScene3D renderer. JavaFX provides a modern, styleable Java UI toolkit without
requiring a browser-hosted editor, while the bridge avoids a second WebGL,
WebGPU, or Three.js rendering implementation whose output could diverge from
the game. SWT, Swing, Vaadin, and an immediate-mode engine UI remain rejected as
the primary editor shell; the OpenGL bridge itself will be selected by a
focused cross-platform prototype before the renderer is refactored around it.
