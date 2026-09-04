/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireProjectId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireSemanticVersion;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireSemanticVersionRequirement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Safe metadata discovered from one JScene3D extension artifact. */
public final class ExtensionDescriptor {
    private final String id;
    private final String version;
    private final String engineRequirement;
    private final DescriptorPresentation presentation;
    private final List<RegisteredTypeDescriptor> types;

    /**
     * Creates one immutable extension descriptor.
     *
     * @param id stable reverse-domain extension identifier
     * @param version semantic extension version
     * @param engineRequirement compatible JScene3D engine versions
     * @param presentation human-readable metadata
     * @param types registered types in declaration order
     */
    public ExtensionDescriptor(
            String id,
            String version,
            String engineRequirement,
            DescriptorPresentation presentation,
            List<RegisteredTypeDescriptor> types) {
        this.id = requireProjectId(id, "id");
        this.version = requireSemanticVersion(version, "version");
        this.engineRequirement = requireSemanticVersionRequirement(engineRequirement, "engineRequirement");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.types = List.copyOf(types);
        validateTypes();
    }

    /**
     * Returns the stable reverse-domain extension identifier.
     *
     * @return extension identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the semantic extension version.
     *
     * @return extension version
     */
    public String version() {
        return version;
    }

    /**
     * Returns the semantic-version requirement for the JScene3D engine.
     *
     * @return engine version requirement
     */
    public String engineRequirement() {
        return engineRequirement;
    }

    /**
     * Returns human-readable extension metadata.
     *
     * @return presentation metadata
     */
    public DescriptorPresentation presentation() {
        return presentation;
    }

    /**
     * Returns registered types in declaration order.
     *
     * @return immutable registered-type descriptors
     */
    public List<RegisteredTypeDescriptor> types() {
        return types;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ExtensionDescriptor descriptor
                && id.equals(descriptor.id)
                && version.equals(descriptor.version)
                && engineRequirement.equals(descriptor.engineRequirement)
                && presentation.equals(descriptor.presentation)
                && types.equals(descriptor.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, engineRequirement, presentation, types);
    }

    @Override
    public String toString() {
        return "ExtensionDescriptor[id=" + id + ", version=" + version + ", engineRequirement=" + engineRequirement
                + ", presentation=" + presentation + ", types=" + types + ']';
    }

    /** Requires every type to belong to this extension and have a unique identity and version. */
    private void validateTypes() {
        String prefix = id + '/';
        Set<RegisteredType> unique = new HashSet<>();
        for (RegisteredTypeDescriptor descriptor : types) {
            RegisteredType type =
                    Objects.requireNonNull(descriptor, "types entry").type();
            if (!type.id().startsWith(prefix)) {
                throw new IllegalArgumentException("registered type does not belong to extension " + id + ": " + type);
            }
            if (!unique.add(type)) {
                throw new IllegalArgumentException("registered type is duplicated: " + type);
            }
        }
    }
}
