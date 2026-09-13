/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

/** Canonical keys used when exposing property-descriptor constraints and editor metadata. */
public final class PropertyDescriptorKeys {
    /** Structural kind required for every array element. */
    public static final String ELEMENT_KIND = "elementKind";

    /** Exact number of elements required in a fixed-size array. */
    public static final String EXACT_ELEMENT_COUNT = "exactElementCount";

    /** Resource-reference namespaces accepted by a reference property. */
    public static final String ACCEPTED_REFERENCE_KINDS = "acceptedReferenceKinds";

    /** Editor semantic used to select a specialized property control. */
    public static final String EDITOR_SEMANTIC = "semantic";

    private PropertyDescriptorKeys() {
        throw new AssertionError("PropertyDescriptorKeys cannot be instantiated");
    }
}
