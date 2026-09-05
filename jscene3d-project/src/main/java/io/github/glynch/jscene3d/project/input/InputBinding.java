/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import java.util.Objects;

/** One portable physical control bound to a semantic input action.
 *
 * @param device physical input device family
 * @param control stable host control identifier
 */
public record InputBinding(Device device, String control) {
    /** Validates one physical binding. */
    public InputBinding {
        Objects.requireNonNull(device, "device");
        control = requireNonBlank(control, "control");
    }

    /** Physical device families supported by input-map version 1. */
    public enum Device {
        /** A keyboard key identified by the JScene3D key name. */
        KEYBOARD,
        /** A mouse button identified by the JScene3D button name. */
        MOUSE_BUTTON
    }
}
