/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.iosurface.macos;

/**
 * A newly acquired IOSurface reference and its physical pixel dimensions.
 *
 * @param handle process-local native IOSurface reference
 * @param width physical width in pixels
 * @param height physical height in pixels
 */
public record IOSurfaceDescriptor(long handle, int width, int height) {}
