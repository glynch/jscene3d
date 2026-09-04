/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.util.ArrayList;
import java.util.List;

/** Shared observations from the service-loaded acceptance extension. */
final class TestRuntimeState {
    static final List<String> EVENTS = new ArrayList<>();
    static String rootLabel = "";
    static String timerParent = "";

    private TestRuntimeState() {}

    static void reset() {
        EVENTS.clear();
        rootLabel = "";
        timerParent = "";
    }
}
