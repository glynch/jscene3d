/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;

final class DesktopProjectLauncherTest {
    @Test
    void rejectsIncompleteLaunchConfigurationBeforeAccessingNativeState() {
        String[] incompleteArguments = {"0.1.0", "project"};

        assertThatThrownBy(() -> DesktopProjectLauncher.main(incompleteArguments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected either no arguments")
                .hasMessageContaining("engine-version")
                .hasMessageContaining("project-directory")
                .hasMessageContaining("published-content-directory");
    }

    @Test
    void resolvesExplicitCommandLineConfiguration() {
        String[] arguments = {"0.1.0", "project", "content"};

        DesktopLaunchConfiguration configuration = DesktopLaunchConfiguration.resolve(arguments, new Properties());

        assertThat(configuration.engineVersion()).isEqualTo("0.1.0");
        assertThat(configuration.projectDirectory()).isEqualTo(Path.of("project"));
        assertThat(configuration.contentDirectory()).isEqualTo(Path.of("content"));
        assertThat(configuration.playtestProfile()).isEmpty();
    }

    @Test
    void resolvesLocalPlaytestProfileWithoutChangingPackagedArguments() {
        Properties properties = packagedProperties();
        properties.setProperty(DesktopProjectLauncher.PLAYTEST_PROFILE_PROPERTY, " moving-floor-34 ");

        DesktopLaunchConfiguration configuration = DesktopLaunchConfiguration.resolve(new String[0], properties);

        assertThat(configuration.playtestProfile()).isEqualTo(Optional.of("moving-floor-34"));
    }

    @Test
    void resolvesLocalPlaytestProfileWithExplicitArguments() {
        Properties properties = new Properties();
        properties.setProperty(DesktopProjectLauncher.PLAYTEST_PROFILE_PROPERTY, "moving-floor-34");

        DesktopLaunchConfiguration configuration =
                DesktopLaunchConfiguration.resolve(new String[] {"0.1.0", "project", "content"}, properties);

        assertThat(configuration.playtestProfile()).contains("moving-floor-34");
    }

    @Test
    void resolvesPackagedLaunchProperties() {
        Properties properties = packagedProperties();

        DesktopLaunchConfiguration configuration = DesktopLaunchConfiguration.resolve(new String[0], properties);

        assertThat(configuration.engineVersion()).isEqualTo("0.1.0");
        assertThat(configuration.projectDirectory()).isEqualTo(Path.of("/application/project"));
        assertThat(configuration.contentDirectory()).isEqualTo(Path.of("/application/content"));
    }

    @Test
    void rejectsMissingPackagedLaunchProperty() {
        Properties properties = packagedProperties();
        properties.remove(DesktopProjectLauncher.CONTENT_DIRECTORY_PROPERTY);
        String[] noArguments = {};

        assertThatThrownBy(() -> DesktopLaunchConfiguration.resolve(noArguments, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(DesktopProjectLauncher.CONTENT_DIRECTORY_PROPERTY);
    }

    private static Properties packagedProperties() {
        Properties properties = new Properties();
        properties.setProperty(DesktopProjectLauncher.ENGINE_VERSION_PROPERTY, "0.1.0");
        properties.setProperty(DesktopProjectLauncher.PROJECT_DIRECTORY_PROPERTY, "/application/project");
        properties.setProperty(DesktopProjectLauncher.CONTENT_DIRECTORY_PROPERTY, "/application/content");
        return properties;
    }
}
