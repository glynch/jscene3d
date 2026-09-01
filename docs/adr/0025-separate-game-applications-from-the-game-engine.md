# Separate game applications from the game engine

`jscene3d-game` will be a reusable, genre-independent Game Engine rather than a
home for the first playable title. Each Game Application will live in its own
artifact and will own all title- and genre-specific rules, content models,
assets, and integrations. Consequently, the planned Doom-style FPS will not add
WAD concepts, sectors, weapons, enemies, or other Doom-specific code to either
`jscene3d-game` or the renderer-independent `jscene3d-physics` artifact. A
capability may move into a reusable artifact only when it has a genuinely
general contract independent of that game.

This separation adds an application artifact but prevents the first game from
defining the abstractions of the Physics Engine or Game Engine around one genre.
The application artifact will receive an original title before its Maven
artifact is named, avoiding Doom branding and any implication of endorsement by
the Doom or Freedoom projects.
