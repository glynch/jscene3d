# Publish genuine JPMS modules

Each published JScene3D artifact contains `module-info.java` while remaining
usable on the ordinary classpath. The initial module names are
`io.github.glynch.jscene3d.core` for `jscene3d-core` and
`io.github.glynch.jscene3d.lwjgl` for `jscene3d-lwjgl`. Packages never split
across artifacts, only deliberate caller packages are exported, and minimal
external consumers verify both module-path and classpath use during ordinary
verification.

This exposes stable JPMS identities and requires additional consumer fixtures,
but it prevents accidental exports and catches packaging failures before users
do. Supporting both consumption modes avoids requiring downstream applications
to adopt JPMS merely to use JScene3D.
