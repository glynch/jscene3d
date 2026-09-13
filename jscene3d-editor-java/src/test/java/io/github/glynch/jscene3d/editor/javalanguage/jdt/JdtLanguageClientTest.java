/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.junit.jupiter.api.Test;

/** Verifies JDT-specific client notification handling. */
final class JdtLanguageClientTest {
    @Test
    void declaresTheJdtLanguageStatusNotification() {
        assertThat(ServiceEndpoints.getSupportedMethods(JdtLanguageClient.class))
                .containsKey("language/status");
    }

    @Test
    void reportsReadyOnlyWhenJdtLsReportsServiceReady() {
        List<JdtLanguageServerStatus> statuses = new ArrayList<>();
        JdtLanguageClient client = new JdtLanguageClient(statuses::add, ignored -> {});

        client.sendStatusReport(new JdtStatusReport("Starting", "Starting Java Language Server"));
        client.sendStatusReport(new JdtStatusReport("ServiceReady", "ServiceReady"));
        client.sendStatusReport(new JdtStatusReport("ProjectStatus", "Ready"));

        assertThat(statuses)
                .containsExactly(
                        JdtLanguageServerStatus.starting("Starting Java Language Server"),
                        JdtLanguageServerStatus.ready());
    }

    @Test
    void translatesJdtLsErrorsIntoFailedStatus() {
        List<JdtLanguageServerStatus> statuses = new ArrayList<>();
        JdtLanguageClient client = new JdtLanguageClient(statuses::add, ignored -> {});

        client.sendStatusReport(new JdtStatusReport("Error", "Could not import project"));

        assertThat(statuses).containsExactly(JdtLanguageServerStatus.failed("Could not import project"));
    }
}
