/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable item discovered by read-only source inspection. */
public final class SourceItem {
    private final String identity;
    private final String kind;
    private final String displayName;
    private final boolean selectable;
    private final Map<String, ProjectValue> properties;
    private final List<SourceItemRelation> relations;

    /**
     * Creates one inspected source item.
     *
     * @param identity stable source-local identity
     * @param kind adapter-qualified source-item kind
     * @param displayName human-readable label
     * @param selectable whether the item may be a selection root
     * @param properties immutable descriptive source properties
     * @param relations outgoing graph relationships
     */
    public SourceItem(
            String identity,
            String kind,
            String displayName,
            boolean selectable,
            Map<String, ProjectValue> properties,
            List<SourceItemRelation> relations) {
        this.identity = Preconditions.requirePortableIdentity(identity, "identity");
        this.kind = Preconditions.requireRegisteredTypeId(kind, "kind");
        this.displayName = Preconditions.requireNonBlank(displayName, "displayName");
        this.selectable = selectable;
        this.properties = Preconditions.copyProjectValues(properties, "properties");
        this.relations = List.copyOf(relations);
    }

    /**
     * Returns the stable source-local identity.
     *
     * @return stable source-local identity
     */
    public String identity() {
        return identity;
    }

    /**
     * Returns the adapter-qualified source-item kind.
     *
     * @return adapter-qualified source-item kind
     */
    public String kind() {
        return kind;
    }

    /**
     * Returns the human-readable item label.
     *
     * @return human-readable item label
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns whether this item may be selected as an import root.
     *
     * @return whether this item may be selected as an import root
     */
    public boolean isSelectable() {
        return selectable;
    }

    /**
     * Returns immutable descriptive properties.
     *
     * @return immutable descriptive properties
     */
    public Map<String, ProjectValue> properties() {
        return properties;
    }

    /**
     * Returns immutable outgoing graph relationships.
     *
     * @return immutable outgoing graph relationships
     */
    public List<SourceItemRelation> relations() {
        return relations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SourceItem item
                && selectable == item.selectable
                && identity.equals(item.identity)
                && kind.equals(item.kind)
                && displayName.equals(item.displayName)
                && properties.equals(item.properties)
                && relations.equals(item.relations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity, kind, displayName, selectable, properties, relations);
    }

    @Override
    public String toString() {
        return "SourceItem[identity=" + identity + ", kind=" + kind + ", displayName=" + displayName + ", selectable="
                + selectable + ", properties=" + properties + ", relations=" + relations + ']';
    }
}
