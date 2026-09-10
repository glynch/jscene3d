/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.playtest;

/** Reports an invalid or unavailable local playtest profile. */
public final class PlaytestProfileException extends RuntimeException {
    /** Creates a profile failure with a concise user-facing message.
     *
     * @param message failure description
     */
    public PlaytestProfileException(String message) {
        super(message);
    }

    /** Creates a profile failure retaining its parsing cause.
     *
     * @param message failure description
     * @param cause underlying input failure
     */
    public PlaytestProfileException(String message, Throwable cause) {
        super(message, cause);
    }
}
