/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks the adapter that uses JavaFX's required JavaScript-object bridge. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface AllowJavaFxWebInterop {}
