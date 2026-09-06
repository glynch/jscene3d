/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

/** Number of instances of one component type permitted on a single entity. */
public enum ComponentMultiplicity {
    /** At most one instance is permitted on an entity. */
    SINGLE,
    /** Any number of independently identified instances is permitted. */
    MULTIPLE
}
