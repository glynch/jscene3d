/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/** Framed protocol and persistent headless authoring process for JScene3D editors. */
module io.github.glynch.jscene3d.editor.authoring.service {
    requires com.fasterxml.jackson.databind;
    requires io.github.glynch.jscene3d.editor.authoring;
    requires io.github.glynch.jscene3d.i18n;
    requires io.github.glynch.jscene3d.project.importing;
    requires static org.jspecify;

    exports io.github.glynch.jscene3d.editor.authoring.protocol;
    exports io.github.glynch.jscene3d.editor.authoring.protocol.framing;
}
