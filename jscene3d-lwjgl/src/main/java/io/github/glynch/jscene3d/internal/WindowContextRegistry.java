/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.internal;

import io.github.glynch.jscene3d.platform.Window;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Internal bridge between the platform and renderer packages. */
public final class WindowContextRegistry {
    private static final IdentityHashMap<Window, Entry> ENTRIES = new IdentityHashMap<>();

    private WindowContextRegistry() {
        throw new AssertionError("WindowContextRegistry cannot be instantiated");
    }

    /** Registers a newly created window context. */
    public static void register(
            Window window, Runnable makeCurrent, IntSupplier framebufferWidth, IntSupplier framebufferHeight) {
        Window validWindow = Objects.requireNonNull(window, "window");
        Entry entry = new Entry(
                Objects.requireNonNull(makeCurrent, "makeCurrent"),
                Objects.requireNonNull(framebufferWidth, "framebufferWidth"),
                Objects.requireNonNull(framebufferHeight, "framebufferHeight"));
        if (ENTRIES.put(validWindow, entry) != null) {
            throw new IllegalStateException("Window context is already registered");
        }
    }

    /** Exclusively claims a window context for one renderer. */
    public static Access claim(Window window) {
        Entry entry = requireEntry(window);
        if (entry.claimed) {
            throw new IllegalStateException("Window already has a renderer");
        }
        entry.claimed = true;
        return new Access(entry);
    }

    /** Releases a renderer's exclusive context claim. */
    public static void release(Window window, Access access) {
        Entry entry = requireEntry(window);
        if (entry != Objects.requireNonNull(access, "access").entry || !entry.claimed) {
            throw new IllegalStateException("Renderer does not own this window context");
        }
        entry.claimed = false;
    }

    /** Ensures a window has no active renderer before its context is destroyed. */
    public static void requireUnclaimed(Window window) {
        if (requireEntry(window).claimed) {
            throw new IllegalStateException("Window cannot close while its renderer is open");
        }
    }

    /** Removes a window whose native context is being destroyed. */
    public static void unregister(Window window) {
        Entry entry = ENTRIES.remove(Objects.requireNonNull(window, "window"));
        if (entry == null) {
            throw new IllegalStateException("Window context is not registered");
        }
    }

    private static Entry requireEntry(Window window) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(window, "window"));
        if (entry == null) {
            throw new IllegalStateException("Window is closed");
        }
        return entry;
    }

    /** Renderer-side access to an exclusively claimed context. */
    public static final class Access {
        private final Entry entry;

        private Access(Entry entry) {
            this.entry = entry;
        }

        /** Makes the associated context current on its owner thread. */
        public void makeCurrent() {
            entry.makeCurrent.run();
        }

        /** Returns the current framebuffer width. */
        public int framebufferWidth() {
            return entry.framebufferWidth.getAsInt();
        }

        /** Returns the current framebuffer height. */
        public int framebufferHeight() {
            return entry.framebufferHeight.getAsInt();
        }
    }

    private static final class Entry {
        private final Runnable makeCurrent;
        private final IntSupplier framebufferWidth;
        private final IntSupplier framebufferHeight;

        private boolean claimed;

        private Entry(Runnable makeCurrent, IntSupplier framebufferWidth, IntSupplier framebufferHeight) {
            this.makeCurrent = makeCurrent;
            this.framebufferWidth = framebufferWidth;
            this.framebufferHeight = framebufferHeight;
        }
    }
}
