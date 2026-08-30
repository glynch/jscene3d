# Reserve five automatic transform uniforms

JScene3D `ShaderMaterial` programs may declare `modelMatrix`, `viewMatrix`,
`projectionMatrix`, `modelViewMatrix`, and `normalMatrix` using their documented
`mat4` or `mat3` types. The renderer owns and automatically supplies their
values, binds only declarations active in the linked program, and rejects a
reserved name declared with the wrong type. All other uniforms are explicitly
application supplied.

This small public contract makes common custom shaders useful without repetitive
matrix plumbing and preserves familiar Three.js terminology. Reserving unprefixed
names reduces the application's uniform namespace and makes later renaming a
breaking change, but prefixing them would add friction to every custom shader
without providing a demonstrated benefit.
