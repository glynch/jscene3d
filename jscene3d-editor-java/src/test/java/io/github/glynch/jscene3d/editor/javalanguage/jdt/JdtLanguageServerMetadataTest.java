/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class JdtLanguageServerMetadataTest {
    @Test
    void exposesTheChecksumPinnedBuildDistribution() {
        JdtLanguageServerMetadata metadata = JdtLanguageServerMetadata.load();

        assertThat(metadata.version()).isEqualTo("1.61.0");
        assertThat(metadata.archive()).isEqualTo("jdt-language-server-1.61.0-202609031315.tar.gz");
        assertThat(metadata.sha256()).isEqualTo("338e7e73d61836651ba2453919a0d34fa763eb4e7c03342092309bffb8934c64");
        assertThat(metadata.source())
                .isEqualTo(
                        "https://download.eclipse.org/jdtls/milestones/1.61.0/jdt-language-server-1.61.0-202609031315.tar.gz");
    }
}
