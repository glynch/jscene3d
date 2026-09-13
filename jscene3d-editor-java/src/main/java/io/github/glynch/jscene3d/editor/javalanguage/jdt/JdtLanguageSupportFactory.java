/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.environment.ApplicationDirectories;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/** Creates the bundled JDT LS language support behind the Java extension seam. */
public final class JdtLanguageSupportFactory {
    private JdtLanguageSupportFactory() {}

    /**
     * Returns the bundled language-server version without constructing a project adapter.
     *
     * @return bundled JDT LS version
     */
    public static String serverVersion() {
        return JdtLanguageServerMetadata.load().version();
    }

    /**
     * Creates Java language support using the staged JDT LS distribution.
     *
     * @param status lifecycle status consumer
     * @param diagnostics diagnostic collection receiving Java language problems
     * @return the server identity and project-scoped language support
     */
    public static Result create(Consumer<JdtLanguageServerStatus> status, EditorDiagnosticCollection diagnostics) {
        JdtLanguageServerMetadata metadata = JdtLanguageServerMetadata.load();
        ForkJoinPool executor = ForkJoinPool.commonPool();
        JavaProjectLanguageSupport support = new JavaProjectLanguageSupport(
                new JdtLanguageServerDistributionLocator().locate(),
                new JavaProjectLanguageSupport.Configuration(
                        ApplicationDirectories.cache("jscene3d"),
                        metadata,
                        OperatingSystem.current(),
                        executor,
                        new LanguageServerProcessLauncher(executor),
                        clientVersion()),
                Objects.requireNonNull(status, "status"),
                Objects.requireNonNull(diagnostics, "diagnostics"));
        return new Result(metadata.version(), support);
    }

    private static String clientVersion() {
        return JdtLanguageSupportFactory.class
                .getModule()
                .getDescriptor()
                .rawVersion()
                .orElse("Development");
    }

    /**
     * Internal construction result consumed by the exported Java extension.
     *
     * @param serverVersion bundled JDT LS version
     * @param support Java project language support
     */
    public record Result(String serverVersion, EditorLanguageSupport support) {
        /**
         * Creates a validated construction result.
         *
         * @param serverVersion bundled JDT LS version
         * @param support Java project language support
         */
        public Result {
            Objects.requireNonNull(serverVersion, "serverVersion");
            Objects.requireNonNull(support, "support");
        }
    }
}
