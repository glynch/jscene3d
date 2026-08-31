/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.gui.internal.GuiCanvas;
import io.github.glynch.jscene3d.gui.internal.GuiFont;
import io.github.glynch.jscene3d.gui.internal.OverlayGuiCanvas;
import io.github.glynch.jscene3d.gui.internal.Preconditions;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.Nullable;

/**
 * Themed immediate control panel explicitly bound to application state.
 *
 * <p>Sections contain checkboxes, floating-point sliders, and action buttons. Bindings use Java
 * method references or lambdas; the panel does not use reflection or direct field access. Call
 * {@link #update()} after {@link Window#pollEvents()}, then pass this overlay to the renderer after
 * drawing the scene.
 *
 * <p>The panel owns no native or GPU resources. It is mutable, not thread-safe, and subject to its
 * window's thread affinity.
 */
public final class ControlPanel implements Overlay {
    static final float WIDTH = 268.0f;
    static final float MARGIN = 16.0f;
    static final float TITLE_HEIGHT = 42.0f;
    static final float SECTION_HEIGHT = 30.0f;
    static final float ITEM_HEIGHT = 38.0f;
    static final float HORIZONTAL_PADDING = 12.0f;
    static final float SLIDER_X_OFFSET = 132.0f;
    static final float SLIDER_VALUE_WIDTH = 42.0f;
    static final float SLIDER_VALUE_GAP = 8.0f;
    static final float SLIDER_RIGHT_PADDING = 12.0f;
    static final float SLIDER_WIDTH =
            WIDTH - SLIDER_X_OFFSET - SLIDER_VALUE_WIDTH - SLIDER_VALUE_GAP - SLIDER_RIGHT_PADDING;

    private static final float PANEL_RADIUS = 9.0f;
    private static final float TITLE_FONT_SIZE = 15.0f;
    private static final float SECTION_FONT_SIZE = 13.0f;
    private static final float ITEM_FONT_SIZE = 13.0f;
    private static final float VALUE_FONT_SIZE = 12.0f;
    private static final GuiFont FONT = GuiFont.defaultFont();

    private final @Nullable Window window;
    private final String title;
    private final GuiTheme theme;
    private final List<Section> sections = new ArrayList<>();
    private final OverlayGuiCanvas overlayCanvas = new OverlayGuiCanvas();

    private @Nullable SliderItem activeSlider;
    private boolean visible = true;
    private boolean capturesPointer;
    private boolean ownsPointerPress;
    private double pointerX = Double.NEGATIVE_INFINITY;
    private double pointerY = Double.NEGATIVE_INFINITY;

    /**
     * Creates an empty, visible panel with the default theme.
     *
     * @param window window supplying logical dimensions and input
     * @param title non-blank panel title
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public ControlPanel(Window window, String title) {
        this(window, title, GuiTheme.dark());
    }

    /**
     * Creates an empty, visible panel with an explicit theme.
     *
     * @param window window supplying logical dimensions and input
     * @param title non-blank panel title
     * @param theme immutable visual theme
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public ControlPanel(Window window, String title, GuiTheme theme) {
        this.window = Objects.requireNonNull(window, "window");
        this.title = Preconditions.requireNonBlank(title, "title");
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /** Creates a detached panel model for headless interaction tests. */
    ControlPanel(String title) {
        window = null;
        this.title = Preconditions.requireNonBlank(title, "title");
        theme = GuiTheme.dark();
    }

    /**
     * Adds a collapsible section in insertion order.
     *
     * @param title non-blank section title
     * @return section to populate with controls
     * @throws NullPointerException if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is blank
     */
    public Section addSection(String title) {
        Section section = new Section(this, Preconditions.requireNonBlank(title, "title"));
        sections.add(section);
        return section;
    }

    /**
     * Returns whether the panel is drawn and processes input.
     *
     * @return {@code true} by default
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows or hides the panel.
     *
     * @param visible whether the panel should be visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            activeSlider = null;
            capturesPointer = false;
            ownsPointerPress = false;
        }
    }

    /**
     * Applies the latest pointer input to panel controls.
     *
     * <p>Call this once after each event poll and before camera controls process the same input.
     *
     * @return {@code true} if a control value or section state changed
     * @throws IllegalStateException if the window is closed or accessed from the wrong thread
     */
    public boolean update() {
        Window validWindow = requireWindow();
        InputState input = validWindow.input();
        PointerFrame pointer = new PointerFrame(
                input.pointerX(),
                input.pointerY(),
                input.wasMouseButtonPressed(MouseButton.LEFT),
                input.isMouseButtonDown(MouseButton.LEFT),
                input.wasMouseButtonReleased(MouseButton.LEFT));
        return update(pointer, validWindow.width(), validWindow.height());
    }

    /**
     * Returns whether the latest update claimed pointer input for the panel.
     *
     * @return whether the pointer is over the panel or the current press began within it
     */
    public boolean capturesPointer() {
        return capturesPointer;
    }

    /** Paints the complete themed panel in the upper-right corner. */
    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        overlayCanvas.bind(Objects.requireNonNull(canvas, "canvas"));
        try {
            paint(overlayCanvas, width, height);
        } finally {
            overlayCanvas.unbind();
        }
    }

    /** Paints the panel through the internal headless-testable drawing boundary. */
    void paint(GuiCanvas canvas, int width, int height) {
        Objects.requireNonNull(canvas, "canvas");
        Preconditions.requirePositive(width, "width");
        Preconditions.requirePositive(height, "height");
        if (!visible) {
            return;
        }
        float x = panelX(width);
        float panelHeight = height();
        canvas.roundedRectangle(x + 4.0f, MARGIN + 6.0f, WIDTH, panelHeight, PANEL_RADIUS, theme.shadow(), 0.38f);
        canvas.roundedRectangle(x, MARGIN, WIDTH, panelHeight, PANEL_RADIUS, theme.border(), 1.0f);
        canvas.roundedRectangle(x + 1.0f, MARGIN + 1.0f, WIDTH - 2.0f, panelHeight - 2.0f, 8.0f, theme.panel(), 0.97f);

        float y = MARGIN;
        canvas.roundedRectangle(x + 1.0f, y + 1.0f, WIDTH - 2.0f, TITLE_HEIGHT, 8.0f, theme.title(), 0.98f);
        FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, title, TITLE_FONT_SIZE, theme.text());
        y += TITLE_HEIGHT;

        for (Section section : sections) {
            paintSection(canvas, section, x, y);
            y += SECTION_HEIGHT;
            if (!section.expanded) {
                continue;
            }
            for (Item item : section.items) {
                boolean hovered = contains(pointerX, pointerY, x, y, WIDTH, ITEM_HEIGHT);
                canvas.rectangle(
                        x + 1.0f, y, WIDTH - 2.0f, ITEM_HEIGHT, hovered ? theme.rowHover() : theme.row(), 0.97f);
                item.paint(canvas, x, y, hovered, theme);
                canvas.rectangle(x + 1.0f, y + ITEM_HEIGHT - 1.0f, WIDTH - 2.0f, 1.0f, theme.border(), 0.45f);
                y += ITEM_HEIGHT;
            }
        }
    }

    /** Paints one section header and its disclosure chevron. */
    private void paintSection(GuiCanvas canvas, Section section, float x, float y) {
        boolean hovered = contains(pointerX, pointerY, x, y, WIDTH, SECTION_HEIGHT);
        canvas.rectangle(
                x + 1.0f, y, WIDTH - 2.0f, SECTION_HEIGHT, hovered ? theme.rowHover() : theme.section(), 0.98f);
        float chevronX = x + HORIZONTAL_PADDING + 3.0f;
        float chevronY = y + SECTION_HEIGHT * 0.5f;
        if (section.expanded) {
            canvas.line(chevronX - 3.0f, chevronY - 2.0f, chevronX, chevronY + 2.0f, 1.5f, theme.mutedText(), 1.0f);
            canvas.line(chevronX, chevronY + 2.0f, chevronX + 3.0f, chevronY - 2.0f, 1.5f, theme.mutedText(), 1.0f);
        } else {
            canvas.line(chevronX - 2.0f, chevronY - 3.0f, chevronX + 2.0f, chevronY, 1.5f, theme.mutedText(), 1.0f);
            canvas.line(chevronX + 2.0f, chevronY, chevronX - 2.0f, chevronY + 3.0f, 1.5f, theme.mutedText(), 1.0f);
        }
        FONT.text(
                canvas,
                x + HORIZONTAL_PADDING + 14.0f,
                y + 7.0f,
                section.title,
                SECTION_FONT_SIZE,
                theme.secondaryText());
    }

    /** Applies a testable pointer snapshot in logical window coordinates. */
    boolean update(PointerFrame pointer, int windowWidth, int windowHeight) {
        PointerFrame validPointer = Objects.requireNonNull(pointer, "pointer");
        Preconditions.requirePositive(windowWidth, "windowWidth");
        Preconditions.requirePositive(windowHeight, "windowHeight");
        pointerX = validPointer.x();
        pointerY = validPointer.y();
        if (!visible) {
            capturesPointer = false;
            activeSlider = null;
            ownsPointerPress = false;
            return false;
        }

        float panelX = panelX(windowWidth);
        boolean pointerOverPanel = contains(pointerX, pointerY, panelX, MARGIN, WIDTH, height());
        if (validPointer.pressed() && pointerOverPanel) {
            ownsPointerPress = true;
        }
        capturesPointer = ownsPointerPress || activeSlider != null || pointerOverPanel;

        boolean changed = false;
        if (activeSlider != null && validPointer.down()) {
            changed |= applySlider(activeSlider, pointerX, panelX);
        }
        if (validPointer.pressed()) {
            changed |= activate(pointerX, pointerY, panelX);
        }
        if (validPointer.released()) {
            activeSlider = null;
            ownsPointerPress = false;
        }
        return changed;
    }

    /** Finds and activates the panel row under one primary-button press. */
    private boolean activate(double x, double y, float panelX) {
        float rowY = MARGIN + TITLE_HEIGHT;
        for (Section section : sections) {
            if (contains(x, y, panelX, rowY, WIDTH, SECTION_HEIGHT)) {
                section.expanded = !section.expanded;
                activeSlider = null;
                return true;
            }
            rowY += SECTION_HEIGHT;
            if (!section.expanded) {
                continue;
            }
            for (Item item : section.items) {
                if (contains(x, y, panelX, rowY, WIDTH, ITEM_HEIGHT)) {
                    return activateItem(item, x, panelX);
                }
                rowY += ITEM_HEIGHT;
            }
        }
        return false;
    }

    /** Applies the activation semantics for one concrete control type. */
    private boolean activateItem(Item item, double x, float panelX) {
        return switch (item) {
            case BooleanItem booleanItem -> {
                booleanItem.setter.accept(!booleanItem.getter.getAsBoolean());
                yield true;
            }
            case SliderItem sliderItem -> {
                float sliderX = panelX + SLIDER_X_OFFSET;
                if (x < sliderX) {
                    yield false;
                }
                activeSlider = sliderItem;
                yield applySlider(sliderItem, x, panelX);
            }
            case ButtonItem buttonItem -> {
                buttonItem.action.run();
                yield true;
            }
        };
    }

    /** Maps a pointer position to one slider's configured interval. */
    private static boolean applySlider(SliderItem slider, double x, float panelX) {
        float fraction = (float) Math.clamp((x - panelX - SLIDER_X_OFFSET) / SLIDER_WIDTH, 0.0, 1.0);
        float value = slider.minimum + fraction * (slider.maximum - slider.minimum);
        slider.setter.accept(value);
        return true;
    }

    /** Computes the current panel height from expanded sections. */
    private float height() {
        float result = TITLE_HEIGHT;
        for (Section section : sections) {
            result += SECTION_HEIGHT;
            if (section.expanded) {
                result += section.items.size() * ITEM_HEIGHT;
            }
        }
        return result;
    }

    /** Computes the upper-right anchored horizontal origin. */
    private static float panelX(int windowWidth) {
        return Math.max(MARGIN, windowWidth - WIDTH - MARGIN);
    }

    /** Tests a point against one logical-coordinate rectangle. */
    private static boolean contains(double pointX, double pointY, float x, float y, float width, float height) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }

    /** Returns the associated native window or rejects detached headless use. */
    private Window requireWindow() {
        if (window == null) {
            throw new IllegalStateException("Detached ControlPanel has no window");
        }
        return window;
    }

    /** Supplies one floating-point value without boxing. */
    @FunctionalInterface
    public interface FloatSupplier {
        /**
         * Returns the current bound value.
         *
         * @return current bound value
         */
        float getAsFloat();
    }

    /** Accepts one floating-point value without boxing. */
    @FunctionalInterface
    public interface FloatConsumer {
        /**
         * Replaces the bound value.
         *
         * @param value replacement value
         */
        void accept(float value);
    }

    /** Accepts one boolean value without boxing. */
    @FunctionalInterface
    public interface BooleanConsumer {
        /**
         * Replaces the bound value.
         *
         * @param value replacement value
         */
        void accept(boolean value);
    }

    /** A collapsible ordered group of related controls. */
    public static final class Section {
        private final ControlPanel panel;
        private final String title;
        private final List<Item> items = new ArrayList<>();

        private boolean expanded = true;

        /** Retains the owning panel and validated title. */
        private Section(ControlPanel panel, String title) {
            this.panel = panel;
            this.title = title;
        }

        /**
         * Returns whether this section currently shows its controls.
         *
         * @return whether this section currently shows its controls
         */
        public boolean isExpanded() {
            return expanded;
        }

        /**
         * Expands or collapses this section.
         *
         * @param expanded whether controls should be shown
         */
        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            if (!expanded && panel.activeSlider != null && items.contains(panel.activeSlider)) {
                panel.activeSlider = null;
            }
        }

        /**
         * Adds a checkbox explicitly bound to a boolean getter and setter.
         *
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         */
        public void addBoolean(String label, BooleanSupplier getter, BooleanConsumer setter) {
            items.add(new BooleanItem(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter")));
        }

        /**
         * Adds a floating-point slider explicitly bound to a getter and setter.
         *
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param minimum finite inclusive lower endpoint
         * @param maximum finite inclusive upper endpoint
         */
        public void addFloat(String label, FloatSupplier getter, FloatConsumer setter, float minimum, float maximum) {
            Preconditions.requireOrdered(minimum, "minimum", maximum, "maximum");
            items.add(new SliderItem(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    minimum,
                    maximum));
        }

        /**
         * Adds an action button.
         *
         * @param label non-blank button label
         * @param action action invoked once for each primary-button press
         */
        public void addButton(String label, Runnable action) {
            items.add(new ButtonItem(
                    Preconditions.requireNonBlank(label, "label"), Objects.requireNonNull(action, "action")));
        }
    }

    /** One frame of primary-pointer state in logical window coordinates. */
    record PointerFrame(double x, double y, boolean pressed, boolean down, boolean released) {}

    /** Shared paint contract for one panel row. */
    private sealed interface Item permits BooleanItem, SliderItem, ButtonItem {
        /** Paints row-specific visuals. */
        void paint(GuiCanvas canvas, float x, float y, boolean hovered, GuiTheme theme);
    }

    /** Explicitly bound checkbox row. */
    private record BooleanItem(String label, BooleanSupplier getter, BooleanConsumer setter) implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, boolean hovered, GuiTheme theme) {
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            float boxX = x + WIDTH - 31.0f;
            float boxY = y + 10.0f;
            canvas.roundedRectangle(boxX, boxY, 18.0f, 18.0f, 4.0f, theme.border(), 1.0f);
            canvas.roundedRectangle(
                    boxX + 1.0f,
                    boxY + 1.0f,
                    16.0f,
                    16.0f,
                    3.0f,
                    getter.getAsBoolean() ? theme.accent() : theme.control(),
                    1.0f);
            if (getter.getAsBoolean()) {
                canvas.line(boxX + 4.5f, boxY + 9.5f, boxX + 8.0f, boxY + 13.0f, 2.0f, theme.text(), 1.0f);
                canvas.line(boxX + 8.0f, boxY + 13.0f, boxX + 14.0f, boxY + 5.5f, 2.0f, theme.text(), 1.0f);
            }
        }
    }

    /** Explicitly bound floating-point slider row. */
    private record SliderItem(String label, FloatSupplier getter, FloatConsumer setter, float minimum, float maximum)
            implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, boolean hovered, GuiTheme theme) {
            float value = Preconditions.requireFinite(getter.getAsFloat(), label);
            float fraction =
                    maximum == minimum ? 1.0f : Math.clamp((value - minimum) / (maximum - minimum), 0.0f, 1.0f);
            float sliderX = x + SLIDER_X_OFFSET;
            float trackY = y + ITEM_HEIGHT * 0.5f - 2.0f;
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            canvas.roundedRectangle(sliderX, trackY, SLIDER_WIDTH, 4.0f, 2.0f, theme.control(), 1.0f);
            canvas.roundedRectangle(sliderX, trackY, SLIDER_WIDTH * fraction, 4.0f, 2.0f, theme.accent(), 1.0f);
            float thumbX = sliderX + SLIDER_WIDTH * fraction - 6.0f;
            canvas.roundedRectangle(thumbX, trackY - 4.0f, 12.0f, 12.0f, 6.0f, theme.accent(), 1.0f);
            String valueText = compact(value);
            float valueX = x + WIDTH - SLIDER_RIGHT_PADDING - FONT.width(valueText, VALUE_FONT_SIZE);
            FONT.text(canvas, valueX, y + 11.5f, valueText, VALUE_FONT_SIZE, theme.text());
        }

        /** Produces a compact two-decimal value without locale-sensitive formatting. */
        private static String compact(float value) {
            float rounded = Math.round(value * 100.0f) / 100.0f;
            return Float.toString(rounded);
        }
    }

    /** Action-button row. */
    private record ButtonItem(String label, Runnable action) implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, boolean hovered, GuiTheme theme) {
            float buttonX = x + HORIZONTAL_PADDING;
            float buttonY = y + 6.0f;
            float buttonWidth = WIDTH - HORIZONTAL_PADDING * 2.0f;
            canvas.roundedRectangle(buttonX, buttonY, buttonWidth, 26.0f, 5.0f, theme.accent(), hovered ? 1.0f : 0.82f);
            float textX = buttonX + (buttonWidth - FONT.width(label, ITEM_FONT_SIZE)) * 0.5f;
            FONT.text(canvas, textX, y + 11.0f, label, ITEM_FONT_SIZE, theme.text());
        }
    }
}
