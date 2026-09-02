/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

/** Runtime switch for optional example diagnostics. */
public final class ExampleDiagnostics {
    /** System property used to enable example diagnostics at launch. */
    public static final String ENABLED_PROPERTY = "jscene3d.examples.diagnostics";

    private boolean enabled;

    /** Creates a switch whose initial value comes from {@link #ENABLED_PROPERTY}. */
    public ExampleDiagnostics() {
        this(Boolean.getBoolean(ENABLED_PROPERTY));
    }

    /**
     * Creates a switch with an explicit initial value.
     *
     * @param enabled whether diagnostics are initially enabled
     */
    public ExampleDiagnostics(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns whether diagnostic collection and presentation are enabled.
     *
     * @return whether diagnostics are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables diagnostic collection and presentation.
     *
     * @param enabled whether diagnostics should be enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
