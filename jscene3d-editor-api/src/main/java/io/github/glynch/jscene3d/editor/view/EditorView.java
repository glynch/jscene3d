/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/**
 * Toolkit-independent logical view contributed to the editor workbench.
 *
 * <p>A view owns its semantic state and behavior. A workbench adapter is responsible for presenting that state with
 * its UI toolkit; implementations must not expose toolkit controls through this interface.
 */
public interface EditorView {
    /**
     * Returns this view's stable identity.
     *
     * @return stable view identity
     */
    ViewId id();

    /**
     * Returns the human-readable tab or panel title.
     *
     * @return non-blank title
     */
    String title();

    /**
     * Returns the stable presentation kind used to select a workbench adapter.
     *
     * @return stable view-kind identity
     */
    ViewKindId kind();
}
