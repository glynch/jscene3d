/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessConfiguration;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.nio.file.Path;
import java.util.List;

/** Produces the direct, shell-free JVM command documented by Eclipse JDT LS. */
final class JdtLanguageServerCommand {
    private JdtLanguageServerCommand() {}

    static LanguageServerProcessConfiguration create(
            Path projectRoot,
            JdtLanguageServerDistribution distribution,
            JdtLanguageServerProjectLayout layout,
            OperatingSystem operatingSystem) {
        return new LanguageServerProcessConfiguration(
                List.of(
                        javaExecutable(operatingSystem).toString(),
                        "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                        "-Dosgi.bundles.defaultStartLevel=4",
                        "-Declipse.product=org.eclipse.jdt.ls.core.product",
                        "-Dlog.level=INFO",
                        "-Xmx1G",
                        "--add-modules=ALL-SYSTEM",
                        "--add-opens",
                        "java.base/java.util=ALL-UNNAMED",
                        "--add-opens",
                        "java.base/java.lang=ALL-UNNAMED",
                        "-jar",
                        distribution.launcher().toString(),
                        "-configuration",
                        layout.configuration().toString(),
                        "-data",
                        layout.workspace().toString()),
                projectRoot,
                layout.errorLog());
    }

    private static Path javaExecutable(OperatingSystem operatingSystem) {
        String executable = operatingSystem == OperatingSystem.WINDOWS ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable)
                .toAbsolutePath()
                .normalize();
    }
}
