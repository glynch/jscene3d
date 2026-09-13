/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.lsp.client.DefaultLanguageClient;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Handles the JDT-specific client protocol and translates it into editor Java status. */
final class JdtLanguageClient extends DefaultLanguageClient implements JdtLanguageClientProtocol {
    private static final System.Logger LOGGER = System.getLogger(JdtLanguageClient.class.getName());
    private static final String SERVICE_READY = "ServiceReady";
    private static final String ERROR = "Error";

    private final Consumer<JdtLanguageServerStatus> status;
    private final AtomicBoolean ready = new AtomicBoolean();

    JdtLanguageClient(Consumer<JdtLanguageServerStatus> status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public void sendStatusReport(JdtStatusReport report) {
        if (report == null || report.type() == null) {
            LOGGER.log(System.Logger.Level.WARNING, "JDT LS sent an invalid language/status notification");
            return;
        }
        String type = report.type().strip();
        if (SERVICE_READY.equals(type)) {
            ready.set(true);
            status.accept(JdtLanguageServerStatus.ready());
        } else if (ERROR.equals(type)) {
            status.accept(JdtLanguageServerStatus.failed(detail(report, "Eclipse JDT Language Server failed")));
        } else if (!ready.get()) {
            status.accept(JdtLanguageServerStatus.starting(detail(report, type)));
        }
    }

    private static String detail(JdtStatusReport report, String fallback) {
        String message = report.message();
        return message == null || message.isBlank() ? fallback : message.strip();
    }
}
