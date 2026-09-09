/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

/** Internal paint capability selected exclusively by safe component descriptors. */
interface ScreenContent {
    /** Paints content inside its resolved design region. */
    void paint(ScreenPainter painter, ScreenRegion.Bounds bounds, float scale);
}
