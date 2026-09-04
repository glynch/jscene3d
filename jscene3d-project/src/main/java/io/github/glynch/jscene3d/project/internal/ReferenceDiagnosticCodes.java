/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.util.Objects;

/**
 * Typed diagnostic codes used by shared resource-reference validation.
 *
 * @param object invalid reference representation code
 * @param scheme unsupported reference scheme code
 * @param locator blank reference locator code
 * @param asset invalid asset reference code
 * @param missingAsset undeclared asset code
 * @param imported invalid imported-resource reference code
 */
public record ReferenceDiagnosticCodes(
        DiagnosticCode object,
        DiagnosticCode scheme,
        DiagnosticCode locator,
        DiagnosticCode asset,
        DiagnosticCode missingAsset,
        DiagnosticCode imported) {
    /** Creates a complete reference-code set. */
    public ReferenceDiagnosticCodes {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(scheme, "scheme");
        Objects.requireNonNull(locator, "locator");
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(missingAsset, "missingAsset");
        Objects.requireNonNull(imported, "imported");
    }
}
