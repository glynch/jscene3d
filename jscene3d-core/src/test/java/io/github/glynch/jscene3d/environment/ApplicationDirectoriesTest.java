/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ApplicationDirectoriesTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesMacOsCachesUnderLibraryCaches() {
        assertThat(cache(OperatingSystem.MACOS, Map.of()))
                .isEqualTo(temporaryDirectory.resolve("Library/Caches/jscene3d"));
    }

    @Test
    void resolvesWindowsCachesUnderLocalApplicationData() {
        Path localApplicationData = temporaryDirectory.resolve("local-application-data");

        assertThat(cache(OperatingSystem.WINDOWS, Map.of("LOCALAPPDATA", localApplicationData.toString())))
                .isEqualTo(localApplicationData.resolve("jscene3d/Cache"));
    }

    @Test
    void fallsBackToTheWindowsUserProfileWhenLocalApplicationDataIsMissing() {
        assertThat(cache(OperatingSystem.WINDOWS, Map.of()))
                .isEqualTo(temporaryDirectory.resolve("AppData/Local/jscene3d/Cache"));
    }

    @Test
    void honorsTheLinuxXdgCacheDirectory() {
        Path xdgCache = temporaryDirectory.resolve("xdg-cache");

        assertThat(cache(OperatingSystem.LINUX, Map.of("XDG_CACHE_HOME", xdgCache.toString())))
                .isEqualTo(xdgCache.resolve("jscene3d"));
    }

    @Test
    void ignoresARelativeLinuxXdgCacheDirectory() {
        assertThat(cache(OperatingSystem.LINUX, Map.of("XDG_CACHE_HOME", "relative-cache")))
                .isEqualTo(temporaryDirectory.resolve(".cache/jscene3d"));
    }

    @Test
    void fallsBackToTheUnixUserCacheDirectory() {
        assertThat(cache(OperatingSystem.LINUX, Map.of())).isEqualTo(temporaryDirectory.resolve(".cache/jscene3d"));
        assertThat(cache(OperatingSystem.OTHER, Map.of())).isEqualTo(temporaryDirectory.resolve(".cache/jscene3d"));
    }

    @Test
    void rejectsApplicationIdsThatCouldEscapeTheCacheDirectory() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ApplicationDirectories.cache(
                        "../jscene3d", OperatingSystem.LINUX, temporaryDirectory, Map.of()));
    }

    private Path cache(OperatingSystem operatingSystem, Map<String, String> environment) {
        return ApplicationDirectories.cache("jscene3d", operatingSystem, temporaryDirectory, environment);
    }
}
