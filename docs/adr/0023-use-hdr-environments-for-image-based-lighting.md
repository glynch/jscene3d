# Use HDR environments for image-based lighting

`EnvironmentMap` is a renderer-independent, application-owned linear-RGB
equirectangular image. The core artifact owns its copied floating-point pixels;
the LWJGL artifact provides Radiance HDR decoding and context-local OpenGL
realization. Public APIs do not expose STB or OpenGL types.

`Scene.environment` supplies image-based lighting to `StandardMaterial`, while
`Scene.backgroundEnvironment` controls visible background rendering. The two
roles are independent even when they reference the same map. Scene intensity
and rotation apply consistently to diffuse and specular lighting; background
intensity is independent. Each standard material has an additional environment
intensity multiplier.

The renderer converts each realized environment into a compact diffuse
irradiance map and a complete GGX-prefiltered reflection mip chain. A shared
split-sum BRDF lookup supplies the remaining view-angle and roughness terms.
These resources use ordinary floating-point two-dimensional textures so the
implementation remains within the OpenGL 3.3 Core baseline. Derivation occurs
once per environment and renderer context on the CPU. This has a one-time cost,
but avoids exposing preprocessing requirements or adding compute-shader and
framebuffer-cubemap dependencies to version 0.1.

HDR presentation is a renderer-level operation because backgrounds, built-in
materials, and custom shaders must share one exposure and output transform.
`ToneMapping.ACES_FILMIC` therefore renders the complete scene into a
renderer-owned RGBA16F target before presentation. `ToneMapping.NONE` remains
the default so existing applications retain their previous output and avoid
the extra framebuffer pass unless they opt in.

Closed environment maps fail clearly when a scene attempts to render them.
Renderer caches remain context-local and are released with the renderer; the
application remains responsible for closing the source map after its scenes no
longer use it.
