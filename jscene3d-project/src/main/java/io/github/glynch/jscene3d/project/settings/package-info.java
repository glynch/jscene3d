/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Portable settings shared by tools operating on a JScene3D project workspace.
 *
 * <p>A reader or writer translates one caller-owned JSON stream without deciding where it lives. A loader or saver
 * applies project-level policy: it locates the conventional settings file, handles its lifecycle, and reports or
 * prevents filesystem failures. This vocabulary applies to the settings persistence module and distinguishes format
 * translation from logical resource acquisition.
 */
@NullMarked
package io.github.glynch.jscene3d.project.settings;

import org.jspecify.annotations.NullMarked;
