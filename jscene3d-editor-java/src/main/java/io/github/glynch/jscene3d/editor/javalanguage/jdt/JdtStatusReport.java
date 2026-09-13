/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

/** Wire representation of one Eclipse JDT LS {@code language/status} notification. */
record JdtStatusReport(String type, String message) {}
