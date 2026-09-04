/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.diagnostic;

/**
 * Stable feature-owned identity and fallback text for one kind of diagnostic.
 *
 * <p>The code is suitable for persistence, tests, and resource-bundle lookup. The default message is the
 * locale-neutral producer's English fallback when no localized resource is available.
 */
public interface DiagnosticCode {
    /**
     * Returns the stable machine-readable and localization key.
     *
     * @return stable diagnostic code
     */
    String code();

    /**
     * Returns the English fallback message.
     *
     * @return non-blank fallback message
     */
    String defaultMessage();
}
