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

    /** Prevents instantiation of this registry utility class. */
    private WindowContextRegistry() {
        throw new AssertionError("WindowContextRegistry cannot be instantiated");
    }

    /**
     * Registers a newly created window context.
     *
     * @param window window owning the context
     * @param makeCurrent operation that makes the context current
     * @param framebufferWidth current framebuffer-width supplier
     * @param framebufferHeight current framebuffer-height supplier
     */
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

    /**
     * Exclusively claims a window context for one renderer.
     *
     * @param window window whose context is claimed
     * @return renderer-side access to the claimed context
     */
    public static Access claim(Window window) {
        Entry entry = requireEntry(window);
        if (entry.claimed) {
            throw new IllegalStateException("Window already has a renderer");
        }
        entry.claimed = true;
        return new Access(entry);
    }

    /**
     * Releases a renderer's exclusive context claim.
     *
     * @param window window whose context is released
     * @param access access token returned when the context was claimed
     */
    public static void release(Window window, Access access) {
        Entry entry = requireEntry(window);
        if (entry != Objects.requireNonNull(access, "access").entry || !entry.claimed) {
            throw new IllegalStateException("Renderer does not own this window context");
        }
        entry.claimed = false;
    }

    /**
     * Ensures a window has no active renderer before its context is destroyed.
     *
     * @param window window about to be destroyed
     */
    public static void requireUnclaimed(Window window) {
        if (requireEntry(window).claimed) {
            throw new IllegalStateException("Window cannot close while its renderer is open");
        }
    }

    /**
     * Removes a window whose native context is being destroyed.
     *
     * @param window window removed from the registry
     */
    public static void unregister(Window window) {
        Entry entry = ENTRIES.remove(Objects.requireNonNull(window, "window"));
        if (entry == null) {
            throw new IllegalStateException("Window context is not registered");
        }
    }

    /** Returns the registered entry for an open window. */
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

        /** Wraps the exclusively claimed registry entry. */
        private Access(Entry entry) {
            this.entry = entry;
        }

        /** Makes the associated context current on its owner thread. */
        public void makeCurrent() {
            entry.makeCurrent.run();
        }

        /**
         * Returns the current framebuffer width.
         *
         * @return framebuffer width in pixels
         */
        public int framebufferWidth() {
            return entry.framebufferWidth.getAsInt();
        }

        /**
         * Returns the current framebuffer height.
         *
         * @return framebuffer height in pixels
         */
        public int framebufferHeight() {
            return entry.framebufferHeight.getAsInt();
        }
    }

    /** Registered platform operations and exclusive-claim state for one window. */
    private static final class Entry {
        private final Runnable makeCurrent;
        private final IntSupplier framebufferWidth;
        private final IntSupplier framebufferHeight;

        private boolean claimed;

        /** Stores context operations supplied by the platform package. */
        private Entry(Runnable makeCurrent, IntSupplier framebufferWidth, IntSupplier framebufferHeight) {
            this.makeCurrent = makeCurrent;
            this.framebufferWidth = framebufferWidth;
            this.framebufferHeight = framebufferHeight;
        }
    }
}
