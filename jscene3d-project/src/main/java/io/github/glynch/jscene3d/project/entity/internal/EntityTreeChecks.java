/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity.internal;

import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Structural invariants shared by world and reusable entity definitions. */
public final class EntityTreeChecks {
    /** Prevents construction of this invariant container. */
    private EntityTreeChecks() {
        throw new AssertionError("EntityTreeChecks cannot be instantiated");
    }

    /**
     * Copies root entries and rejects duplicate entity identities across their local hierarchy.
     *
     * @param roots root entries
     * @param name argument name used in failures
     * @return immutable roots
     */
    public static List<EntityEntry> copyRoots(List<? extends EntityEntry> roots, String name) {
        List<EntityEntry> copied = List.copyOf(Objects.requireNonNull(roots, name));
        requireUniqueIds(copied, name);
        return copied;
    }

    /** Rejects duplicate identities while traversing only locally declared hierarchy entries. */
    private static void requireUniqueIds(List<EntityEntry> roots, String name) {
        Set<EntityId> ids = new HashSet<>();
        ArrayDeque<EntityEntry> remaining = new ArrayDeque<>(roots);
        while (!remaining.isEmpty()) {
            EntityEntry entry = remaining.removeFirst();
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException(name + " contains a duplicate entity id: " + entry.id());
            }
            if (entry instanceof LocalEntity local) {
                remaining.addAll(local.children());
            }
        }
    }
}
