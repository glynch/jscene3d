/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import java.net.URI;
import java.util.Objects;

/**
 * Describes a request for language-aware completion at a position in an editor document.
 *
 * <p>The document version identifies the editor working-copy version for which completion is
 * requested. Implementations may use it to reject a request when the document has already advanced
 * to a different version. The version is editor-domain metadata and need not be transmitted by an
 * underlying language protocol.
 */
public final class EditorCompletionRequest {

    private final URI resource;
    private final int version;
    private final EditorTextPosition position;
    private final EditorCompletionTrigger trigger;

    /**
     * Creates a completion request.
     *
     * @param resource the document resource
     * @param version the positive editor document version
     * @param position the completion position
     * @param trigger how completion was triggered
     * @throws NullPointerException if {@code resource}, {@code position}, or {@code trigger} is
     *     {@code null}
     * @throws IllegalArgumentException if {@code version} is not positive
     */
    public EditorCompletionRequest(
            URI resource, int version, EditorTextPosition position, EditorCompletionTrigger trigger) {
        this.resource = Objects.requireNonNull(resource, "resource");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.version = version;
        this.position = Objects.requireNonNull(position, "position");
        this.trigger = Objects.requireNonNull(trigger, "trigger");
    }

    /**
     * Returns the document resource.
     *
     * @return the document resource
     */
    public URI resource() {
        return resource;
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
     * Returns the completion position.
     *
     * @return the completion position
     */
    public EditorTextPosition position() {
        return position;
    }

    /**
     * Returns how completion was triggered.
     *
     * @return the completion trigger
     */
    public EditorCompletionTrigger trigger() {
        return trigger;
    }
}
