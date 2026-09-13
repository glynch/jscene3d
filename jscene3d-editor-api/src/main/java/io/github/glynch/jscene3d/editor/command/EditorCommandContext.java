/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.util.Objects;
import java.util.Optional;

/** Invocation-scoped facilities supplied to an executing editor command. */
public interface EditorCommandContext {
    /**
     * Returns safe interaction with the containing editor window.
     *
     * @return editor window facility
     */
    EditorWindow window();

    /**
     * Returns the semantic argument supplied by the interaction which invoked this command.
     *
     * <p>Menu and keyboard invocations ordinarily have no argument. Item-oriented interactions supply the exact
     * logical item involved, independently of mutable global selection.
     *
     * @return invocation argument, or empty when the command was invoked without one
     */
    default Optional<Object> argument() {
        return Optional.empty();
    }

    /**
     * Returns the invocation argument when it has the requested type.
     *
     * @param type expected argument type
     * @param <T> expected argument type
     * @return typed invocation argument, or empty when absent or of another type
     */
    default <T> Optional<T> argument(Class<T> type) {
        Class<T> argumentType = Objects.requireNonNull(type, "type");
        return argument().filter(argumentType::isInstance).map(argumentType::cast);
    }
}
