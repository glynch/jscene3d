/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import java.util.List;
import java.util.Objects;

/** One typed semantic action and its ordered physical bindings.
 *
 * @param valueType semantic value produced by the action
 * @param bindings non-empty compatible physical bindings
 */
public record InputActionDefinition(InputValueType valueType, List<InputBinding> bindings) {
    /** Copies and validates one complete action definition. */
    public InputActionDefinition {
        Objects.requireNonNull(valueType, "valueType");
        bindings = List.copyOf(bindings);
        if (bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings must not be empty");
        }
        if (bindings.stream().anyMatch(binding -> binding.valueType() != valueType)) {
            throw new IllegalArgumentException("every binding must produce " + valueType);
        }
    }
}
