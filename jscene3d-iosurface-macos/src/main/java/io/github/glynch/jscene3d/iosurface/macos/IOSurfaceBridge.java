/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.iosurface.macos;

/** JNI access to the IOSurface rendezvous and replacement channel. */
public final class IOSurfaceBridge {
    static {
        System.loadLibrary("iosurface_bridge");
    }

    private IOSurfaceBridge() {
        throw new AssertionError("IOSurfaceBridge cannot be instantiated");
    }

    /**
     * Acquires the initial Electron-owned IOSurface reference.
     *
     * @param parentBundleId Electron application bundle identifier used for rendezvous
     * @return process-local native reference, or zero on failure
     */
    public static native long lookup(String parentBundleId);

    /**
     * Binds an acquired IOSurface to the currently bound OpenGL rectangle texture.
     *
     * @param surface process-local native reference
     * @param width physical width in pixels
     * @param height physical height in pixels
     */
    public static native void bindToTexture(long surface, int width, int height);

    /**
     * Waits for a replacement IOSurface on this process's control channel.
     *
     * @return replacement descriptor, or null on failure
     */
    public static native IOSurfaceDescriptor receiveSurface();

    /**
     * Releases this process's reference to an IOSurface.
     *
     * @param surface process-local native reference
     */
    public static native void release(long surface);
}
