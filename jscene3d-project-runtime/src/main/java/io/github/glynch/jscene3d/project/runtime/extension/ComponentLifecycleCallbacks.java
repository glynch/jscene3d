/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/**
 * Optional semantic lifecycle callbacks implemented by an ordinary runtime component.
 *
 * <p>A component descriptor remains authoritative about participation. The world invokes only callbacks declared by
 * the exact descriptor, even when the component overrides additional methods. Components whose descriptors declare
 * any lifecycle participation must implement this interface. Callbacks run on the thread which activates or closes
 * the owning world and must not recursively activate that world.
 */
public interface ComponentLifecycleCallbacks {
    /** Invoked once after every component in the complete world graph has been constructed. */
    default void onCreated() {}

    /** Invoked once in the initial world slice when the owning entity becomes effectively active. */
    default void onActivated() {}

    /** Invoked during rollback or closure after this component became effectively active. */
    default void onDeactivated() {}

    /** Invoked during rollback or closure after this component entered semantic lifecycle management. */
    default void onDestroyed() {}
}
