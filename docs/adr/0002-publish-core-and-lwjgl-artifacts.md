# Publish separate core and LWJGL artifacts

Version 0.1 will publish a headless core artifact containing the scene model, cameras, geometry, materials, textures, and JOML-facing APIs, plus an LWJGL artifact containing the OpenGL renderer and GLFW window/input integration. Examples will be built separately but not published; GLFW will remain bundled with the renderer until a real alternative host requires a narrower public context seam, preserving headless use without designing that seam speculatively.
