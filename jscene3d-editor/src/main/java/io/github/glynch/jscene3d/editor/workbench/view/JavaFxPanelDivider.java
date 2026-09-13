/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.SplitPane;
import org.jspecify.annotations.Nullable;

/** Tracks and moves the split-pane divider that controls a collapsible panel's height. */
final class JavaFxPanelDivider implements AutoCloseable {
    private final double minimumHeight;
    private final DoubleBinaryOperator dividerPosition;
    private final BooleanSupplier expanded;
    private final DoubleConsumer expandedHeight;
    private final ChangeListener<Number> positionListener;
    private final ListChangeListener<SplitPane.Divider> dividerListListener;

    private @Nullable SplitPane owner;
    private SplitPane.@Nullable Divider observedDivider;

    /** Creates divider tracking for one panel's sizing policy. */
    JavaFxPanelDivider(
            double minimumHeight,
            DoubleBinaryOperator dividerPosition,
            BooleanSupplier expanded,
            DoubleConsumer expandedHeight) {
        this.minimumHeight = minimumHeight;
        this.dividerPosition = Objects.requireNonNull(dividerPosition, "dividerPosition");
        this.expanded = Objects.requireNonNull(expanded, "expanded");
        this.expandedHeight = Objects.requireNonNull(expandedHeight, "expandedHeight");
        positionListener = (ignored, previous, current) -> rememberExpandedHeight(current.doubleValue());
        dividerListListener = ignored -> observeDivider();
    }

    /** Attaches to the split pane which owns the panel. */
    void attach(SplitPane splitPane) {
        SplitPane candidate = Objects.requireNonNull(splitPane, "splitPane");
        if (owner != null) {
            throw new IllegalStateException("panel is already attached");
        }
        owner = candidate;
        candidate.getDividers().addListener(dividerListListener);
        observeDivider();
    }

    /** Moves the attached divider to display the requested panel height. */
    void moveToHeight(double panelHeight) {
        SplitPane current = owner;
        if (current != null && current.getHeight() > 0.0) {
            current.setDividerPositions(dividerPosition.applyAsDouble(current.getHeight(), panelHeight));
        }
    }

    /** Stops observing the attached split pane and its current divider. */
    @Override
    public void close() {
        SplitPane current = owner;
        owner = null;
        if (current != null) {
            current.getDividers().removeListener(dividerListListener);
        }
        replaceObservedDivider(null);
    }

    private void observeDivider() {
        SplitPane current = owner;
        SplitPane.Divider divider = current == null || current.getDividers().isEmpty()
                ? null
                : current.getDividers().getFirst();
        replaceObservedDivider(divider);
    }

    private void rememberExpandedHeight(double dividerPositionValue) {
        SplitPane current = owner;
        if (expanded.getAsBoolean() && current != null && current.getHeight() > 0.0) {
            double height = current.getHeight() * (1.0 - dividerPositionValue);
            if (height >= minimumHeight) {
                expandedHeight.accept(height);
            }
        }
    }

    private void replaceObservedDivider(SplitPane.@Nullable Divider replacement) {
        if (observedDivider == replacement) {
            return;
        }
        if (observedDivider != null) {
            observedDivider.positionProperty().removeListener(positionListener);
        }
        observedDivider = replacement;
        if (replacement != null) {
            replacement.positionProperty().addListener(positionListener);
        }
    }
}
