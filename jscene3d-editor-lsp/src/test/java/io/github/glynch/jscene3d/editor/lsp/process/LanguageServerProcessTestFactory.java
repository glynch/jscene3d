/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.process;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/** Creates language-server process instances for tests outside the process package. */
public final class LanguageServerProcessTestFactory {

    /** Prevents instantiation of this test utility class. */
    private LanguageServerProcessTestFactory() {
        throw new AssertionError("LanguageServerProcessTestFactory cannot be instantiated");
    }

    /**
     * Creates a language-server process around a controlled test process.
     *
     * @param process controlled child process
     * @param shutdownExecutor executor used for bounded process shutdown
     * @return the language-server process
     */
    public static LanguageServerProcess create(Process process, Executor shutdownExecutor) {
        return new LanguageServerProcess(
                Objects.requireNonNull(process, "process"),
                Objects.requireNonNull(shutdownExecutor, "shutdownExecutor"),
                Duration.ZERO);
    }
}
