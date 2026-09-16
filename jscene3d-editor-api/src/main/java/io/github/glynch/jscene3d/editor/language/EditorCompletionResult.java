/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import java.util.List;
import java.util.Objects;

/**
 * Contains completion candidates returned for a particular editor document version.
 *
 * <p>The version identifies the working-copy version for which completion was requested. Consumers
 * can compare it with the current document version and discard a result that has become stale while
 * the asynchronous completion request was in progress.
 *
 * <p>An incomplete result indicates that the language service may provide additional or different
 * candidates after further input.
 */
public final class EditorCompletionResult {

    private final int version;
    private final List<EditorCompletionItem> items;
    private final boolean incomplete;

    /**
     * Creates a completion result.
     *
     * @param version the positive editor document version for which completion was requested
     * @param items the completion candidates
     * @param incomplete whether the language service reports the result as incomplete
     * @throws NullPointerException if {@code items} is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if {@code version} is not positive
     */
    public EditorCompletionResult(int version, List<EditorCompletionItem> items, boolean incomplete) {
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.version = version;
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.incomplete = incomplete;
    }

    /**
     * Returns the editor document version for which completion was requested.
     *
     * @return the positive document version
     */
    public int version() {
        return version;
    }

    /**
     * Returns the completion candidates.
     *
     * @return the immutable completion candidates
     */
    public List<EditorCompletionItem> items() {
        return items;
    }

    /**
     * Returns whether the language service reports that the result is incomplete.
     *
     * @return {@code true} when additional or different candidates may be returned after further
     *     input
     */
    public boolean incomplete() {
        return incomplete;
    }
}
