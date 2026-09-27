/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionDocument;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares authored definition projections for modified-entity presentation. */
final class AuthoredDefinitionStates {
    private AuthoredDefinitionStates() {}

    /** Returns source-local entity identities whose authored state differs from the persisted state. */
    static Set<EntityId> modifiedEntityIds(
            AuthoredDefinitionDocument.Content current, AuthoredDefinitionDocument.Content persisted) {
        List<EntityEntry> currentRoots = roots(current);
        Map<EntityId, EntityEntry> persistedEntries = new HashMap<>();
        indexEntries(roots(persisted), persistedEntries);
        Set<EntityId> modified = new LinkedHashSet<>();
        collectModifiedEntries(currentRoots, persistedEntries, modified);
        return Set.copyOf(modified);
    }

    /** Returns direct authored roots for either structural definition kind. */
    private static List<EntityEntry> roots(AuthoredDefinitionDocument.Content content) {
        return switch (content) {
            case AuthoredDefinitionDocument.Content.World world ->
                world.definition().roots();
            case AuthoredDefinitionDocument.Content.Entity entity ->
                List.of(entity.definition().root());
        };
    }

    /** Indexes one authored local tree by stable entity identity. */
    private static void indexEntries(List<EntityEntry> entries, Map<EntityId, EntityEntry> indexed) {
        for (EntityEntry entry : entries) {
            indexed.put(entry.id(), entry);
            if (entry instanceof LocalEntity local) {
                indexEntries(local.children(), indexed);
            }
        }
    }

    /** Collects entries whose authored scalar/component or direct-child structure changed. */
    private static void collectModifiedEntries(
            List<EntityEntry> entries, Map<EntityId, EntityEntry> persistedEntries, Set<EntityId> modified) {
        for (EntityEntry entry : entries) {
            EntityEntry persisted = persistedEntries.get(entry.id());
            if (persisted == null || !sameAuthoredEntry(entry, persisted)) {
                modified.add(entry.id());
            }
            if (entry instanceof LocalEntity local) {
                collectModifiedEntries(local.children(), persistedEntries, modified);
            }
        }
    }

    /** Compares authored state without recursively duplicating child comparisons. */
    private static boolean sameAuthoredEntry(EntityEntry current, EntityEntry persisted) {
        return switch (current) {
            case LocalEntity local
            when persisted instanceof LocalEntity saved ->
                local.name().equals(saved.name())
                        && local.isEnabled() == saved.isEnabled()
                        && local.components().equals(saved.components())
                        && sameChildStructure(local.children(), saved.children());
            case EntityPlacement placement when persisted instanceof EntityPlacement saved -> placement.equals(saved);
            default -> false;
        };
    }

    /** Compares direct child identities and kinds while recursion handles their authored state. */
    private static boolean sameChildStructure(List<EntityEntry> current, List<EntityEntry> persisted) {
        if (current.size() != persisted.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            EntityEntry currentChild = current.get(index);
            EntityEntry persistedChild = persisted.get(index);
            if (!currentChild.id().equals(persistedChild.id())
                    || currentChild.getClass() != persistedChild.getClass()) {
                return false;
            }
        }
        return true;
    }
}
