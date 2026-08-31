/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

/** Creates one independently owned hosted-example instance. */
@FunctionalInterface
public interface ExampleFactory {
    /**
     * Creates an example using the supplied stable window, renderer, and content-area context.
     *
     * @param context stable host context
     * @return newly owned hosted example
     */
    HostedExample create(ExampleContext context);
}
