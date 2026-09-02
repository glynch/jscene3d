/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** OpenAL-backed positional effects, interface sounds, music, and listener control. */
module io.github.glynch.jscene3d.audio {
    requires transitive org.joml;
    requires org.lwjgl;
    requires org.lwjgl.openal;
    requires org.lwjgl.stb;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.audio;
}
