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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Themed immediate control panel explicitly bound to application state.
 *
 * <p>Sections contain read-only text, checkboxes, floating-point and integer sliders, steppers,
 * radio groups, selects, and action buttons. Bindings use Java method references or lambdas; the
 * panel does not use reflection or direct field access. Call {@link #update()} after {@link
 * Window#pollEvents()}, then pass this overlay to the renderer after drawing the scene.
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
    static final float OPTION_HEIGHT = 30.0f;
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
    private @Nullable SelectItem<?> openSelect;
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
            closeOpenSelect();
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
            y = paintItems(canvas, section, x, y);
        }
    }

    /** Paints one expanded section's controls and returns the following vertical position. */
    private float paintItems(GuiCanvas canvas, Section section, float x, float y) {
        if (!section.expanded) {
            return y;
        }
        boolean enabled = section.isEnabled();
        float nextY = y;
        for (Item item : section.items) {
            nextY = paintItem(canvas, item, x, nextY, enabled);
        }
        return nextY;
    }

    /** Paints one control, including hover, disabled, and separator layers. */
    private float paintItem(GuiCanvas canvas, Item item, float x, float y, boolean enabled) {
        float itemHeight = item.height();
        boolean hovered = enabled && contains(pointerX, pointerY, x, y, WIDTH, itemHeight);
        canvas.rectangle(x + 1.0f, y, WIDTH - 2.0f, itemHeight, hovered ? theme.rowHover() : theme.row(), 0.97f);
        float pointerOffset = hovered ? (float) pointerY - y : -1.0f;
        item.paint(canvas, x, y, pointerOffset, theme);
        if (!enabled) {
            canvas.rectangle(x + 1.0f, y, WIDTH - 2.0f, itemHeight, theme.panel(), 0.58f);
        }
        canvas.rectangle(x + 1.0f, y + itemHeight - 1.0f, WIDTH - 2.0f, 1.0f, theme.border(), 0.45f);
        return y + itemHeight;
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
            closeOpenSelect();
            ownsPointerPress = false;
            return false;
        }
        if (activeSlider != null && !isEnabled(activeSlider)) {
            activeSlider = null;
        }
        if (openSelect != null && !isEnabled(openSelect)) {
            closeOpenSelect();
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
            if (pointerOverPanel) {
                changed |= activate(pointerX, pointerY, panelX);
            } else {
                changed |= closeOpenSelect();
            }
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
                closeOpenSelect();
                section.expanded = !section.expanded;
                activeSlider = null;
                return true;
            }
            rowY += SECTION_HEIGHT;
            if (!section.expanded) {
                continue;
            }
            for (Item item : section.items) {
                float itemHeight = item.height();
                if (contains(x, y, panelX, rowY, WIDTH, itemHeight)) {
                    if (!section.isEnabled()) {
                        return closeOpenSelect();
                    }
                    return activateItem(item, x, y, panelX, rowY);
                }
                rowY += itemHeight;
            }
        }
        return closeOpenSelect();
    }

    /** Returns whether the section containing an active item currently accepts input. */
    private boolean isEnabled(Item item) {
        for (Section section : sections) {
            if (section.items.contains(item)) {
                return section.isEnabled();
            }
        }
        return false;
    }

    /** Applies the activation semantics for one concrete control type. */
    private boolean activateItem(Item item, double x, double y, float panelX, float itemY) {
        boolean closedSelect = item != openSelect && closeOpenSelect();
        boolean activated =
                switch (item) {
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
                    case ChoiceItem<?> choiceItem -> choiceItem.select(x >= panelX + WIDTH * 0.5f);
                    case RadioGroupItem<?> radioGroupItem -> radioGroupItem.select((float) y - itemY);
                    case SelectItem<?> selectItem -> {
                        activateSelect(selectItem, (float) y - itemY);
                        yield true;
                    }
                    case ButtonItem buttonItem -> {
                        if (!buttonItem.enabled.getAsBoolean()) {
                            yield false;
                        }
                        buttonItem.action.run();
                        yield true;
                    }
                    case TextItem ignored -> false;
                };
        return closedSelect || activated;
    }

    /** Toggles one select or applies an option from its expanded list. */
    private void activateSelect(SelectItem<?> selectItem, float pointerOffset) {
        if (pointerOffset < ITEM_HEIGHT) {
            boolean opens = openSelect != selectItem;
            openSelect = opens ? selectItem : null;
            selectItem.setExpanded(opens);
            return;
        }
        selectItem.select(pointerOffset - ITEM_HEIGHT);
        openSelect = null;
        selectItem.setExpanded(false);
    }

    /** Closes the currently expanded select, if any. */
    private boolean closeOpenSelect() {
        if (openSelect == null) {
            return false;
        }
        openSelect.setExpanded(false);
        openSelect = null;
        return true;
    }

    /** Maps a pointer position to one slider's configured interval. */
    private static boolean applySlider(SliderItem slider, double x, float panelX) {
        float fraction = (float) Math.clamp((x - panelX - SLIDER_X_OFFSET) / SLIDER_WIDTH, 0.0, 1.0);
        slider.apply(fraction);
        return true;
    }

    /** Computes the current panel height from expanded sections. */
    private float height() {
        float result = TITLE_HEIGHT;
        for (Section section : sections) {
            result += SECTION_HEIGHT;
            if (section.expanded) {
                for (Item item : section.items) {
                    result += item.height();
                }
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

    /**
     * One labelled value offered by a finite choice control.
     *
     * @param <T> value type
     * @param value value passed to the bound setter
     * @param label non-blank display label
     */
    public record Choice<T>(T value, String label) {
        /**
         * Creates a non-null choice with a non-blank display label.
         *
         * @param value value passed to the bound setter
         * @param label non-blank display label
         * @throws NullPointerException if an argument is {@code null}
         * @throws IllegalArgumentException if {@code label} is blank
         */
        public Choice {
            Objects.requireNonNull(value, "value");
            label = Preconditions.requireNonBlank(label, "label");
        }
    }

    /** A collapsible ordered group of related controls. */
    public static final class Section {
        private final ControlPanel panel;
        private final String title;
        private final List<Item> items = new ArrayList<>();

        private BooleanSupplier enabled = () -> true;
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
            if (!expanded && panel.openSelect != null && items.contains(panel.openSelect)) {
                panel.closeOpenSelect();
            }
        }

        /**
         * Returns whether this section's controls currently accept input.
         *
         * @return current value supplied by the enabled-state binding
         */
        public boolean isEnabled() {
            return enabled.getAsBoolean();
        }

        /**
         * Binds this section's enabled state to application state.
         *
         * <p>Disabled controls remain visible but are dimmed and ignore pointer input. The section
         * header remains available so the section can still be expanded or collapsed.
         *
         * @param enabled current enabled-state supplier
         * @throws NullPointerException if {@code enabled} is {@code null}
         */
        public void setEnabled(BooleanSupplier enabled) {
            this.enabled = Objects.requireNonNull(enabled, "enabled");
            if (!isEnabled() && panel.activeSlider != null && items.contains(panel.activeSlider)) {
                panel.activeSlider = null;
            }
            if (!isEnabled() && panel.openSelect != null && items.contains(panel.openSelect)) {
                panel.closeOpenSelect();
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
            addFloat(label, getter, setter, minimum, maximum, 2);
        }

        /**
         * Adds a floating-point slider with explicit displayed decimal precision.
         *
         * <p>Trailing fractional zeroes are omitted, except that whole values retain one decimal
         * place to distinguish them from integer controls.
         *
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param minimum finite inclusive lower endpoint
         * @param maximum finite inclusive upper endpoint
         * @param decimalPlaces displayed decimal places before trailing-zero removal, from one to
         *     four
         * @throws NullPointerException if a binding is {@code null}
         * @throws IllegalArgumentException if the label is blank, the interval is invalid, or the
         *     precision is outside the supported range
         */
        public void addFloat(
                String label,
                FloatSupplier getter,
                FloatConsumer setter,
                float minimum,
                float maximum,
                int decimalPlaces) {
            Preconditions.requireOrdered(minimum, "minimum", maximum, "maximum");
            if (decimalPlaces < 1 || decimalPlaces > 4) {
                throw new IllegalArgumentException("decimalPlaces must be between 1 and 4: " + decimalPlaces);
            }
            items.add(new FloatSliderItem(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    minimum,
                    maximum,
                    decimalPlaces));
        }

        /**
         * Adds an integer slider explicitly bound to a getter and setter.
         *
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param minimum inclusive lower endpoint
         * @param maximum inclusive upper endpoint
         * @throws NullPointerException if a binding is {@code null}
         * @throws IllegalArgumentException if the label is blank or {@code minimum} is greater
         *     than {@code maximum}
         */
        public void addInteger(String label, IntSupplier getter, IntConsumer setter, int minimum, int maximum) {
            if (minimum > maximum) {
                throw new IllegalArgumentException("minimum must not exceed maximum: " + minimum + " > " + maximum);
            }
            items.add(new IntegerSliderItem(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    minimum,
                    maximum));
        }

        /**
         * Adds a finite choice explicitly bound to a getter and setter.
         *
         * <p>Click the left or right half of the row to select the previous or next value. The
         * current getter value must equal one of the supplied choices.
         *
         * @param <T> value type
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param choices ordered non-empty choices
         * @throws NullPointerException if an argument or choice is {@code null}
         * @throws IllegalArgumentException if the label is blank or choices are empty
         */
        public <T> void addChoice(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices) {
            items.add(new ChoiceItem<>(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    validatedChoices(choices)));
        }

        /**
         * Adds a group of visible radio buttons for a small exclusive choice set.
         *
         * <p>The current getter value must equal one of the supplied choices. Every option remains
         * visible, making this presentation suitable when comparing the alternatives is useful.
         *
         * @param <T> value type
         * @param label non-blank group label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param choices ordered non-empty choices
         * @throws NullPointerException if an argument or choice is {@code null}
         * @throws IllegalArgumentException if the label is blank or choices are empty
         */
        public <T> void addRadioGroup(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices) {
            items.add(new RadioGroupItem<>(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    validatedChoices(choices)));
        }

        /**
         * Adds a compact select control that reveals its finite options when opened.
         *
         * <p>The current getter value must equal one of the supplied choices. Opening the select
         * expands its options within the panel so they remain readable and participate in pointer
         * capture without a separate native popup.
         *
         * @param <T> value type
         * @param label non-blank row label
         * @param getter current-value supplier
         * @param setter replacement-value consumer
         * @param choices ordered non-empty choices
         * @throws NullPointerException if an argument or choice is {@code null}
         * @throws IllegalArgumentException if the label is blank or choices are empty
         */
        public <T> void addSelect(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices) {
            items.add(new SelectItem<>(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(getter, "getter"),
                    Objects.requireNonNull(setter, "setter"),
                    validatedChoices(choices)));
        }

        /**
         * Adds an action button.
         *
         * @param label non-blank button label
         * @param action action invoked once for each primary-button press
         */
        public void addButton(String label, Runnable action) {
            addButton(label, () -> true, action);
        }

        /**
         * Adds an action button whose availability is evaluated when painted or activated.
         *
         * @param label non-blank button label
         * @param enabled current enabled-state supplier
         * @param action action invoked once for each enabled primary-button press
         */
        public void addButton(String label, BooleanSupplier enabled, Runnable action) {
            items.add(new ButtonItem(
                    Preconditions.requireNonBlank(label, "label"),
                    Objects.requireNonNull(enabled, "enabled"),
                    Objects.requireNonNull(action, "action")));
        }

        /**
         * Adds a read-only text value supplied when the panel is painted.
         *
         * @param label non-blank row label
         * @param getter current non-null text supplier
         */
        public void addText(String label, Supplier<String> getter) {
            items.add(new TextItem(
                    Preconditions.requireNonBlank(label, "label"), Objects.requireNonNull(getter, "getter")));
        }

        /** Copies and validates a finite non-empty choice list. */
        private static <T> List<Choice<T>> validatedChoices(List<Choice<T>> choices) {
            List<Choice<T>> validChoices = List.copyOf(Objects.requireNonNull(choices, "choices"));
            if (validChoices.isEmpty()) {
                throw new IllegalArgumentException("choices must not be empty");
            }
            return validChoices;
        }
    }

    /** One frame of primary-pointer state in logical window coordinates. */
    record PointerFrame(double x, double y, boolean pressed, boolean down, boolean released) {}

    /** Shared paint contract for one panel row. */
    private sealed interface Item
            permits BooleanItem, SliderItem, ChoiceItem, RadioGroupItem, SelectItem, ButtonItem, TextItem {
        /** Returns this control's current logical height. */
        default float height() {
            return ITEM_HEIGHT;
        }

        /** Paints row-specific visuals. */
        void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme);
    }

    /** Shared behavior of rows controlled by horizontal pointer dragging. */
    private sealed interface SliderItem extends Item permits FloatSliderItem, IntegerSliderItem {
        /** Applies a normalized position within the slider track. */
        void apply(float fraction);
    }

    /** Explicitly bound checkbox row. */
    private record BooleanItem(String label, BooleanSupplier getter, BooleanConsumer setter) implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
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
    private record FloatSliderItem(
            String label, FloatSupplier getter, FloatConsumer setter, float minimum, float maximum, int decimalPlaces)
            implements SliderItem {
        @Override
        public void apply(float fraction) {
            setter.accept(minimum + fraction * (maximum - minimum));
        }

        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
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
            String valueText = compact(value, decimalPlaces);
            float valueX = x + WIDTH - SLIDER_RIGHT_PADDING - FONT.width(valueText, VALUE_FONT_SIZE);
            FONT.text(canvas, valueX, y + 11.5f, valueText, VALUE_FONT_SIZE, theme.text());
        }

        /** Produces a compact value at the requested precision without locale-sensitive formatting. */
        private static String compact(float value, int decimalPlaces) {
            BigDecimal rounded = new BigDecimal(Float.toString(value))
                    .setScale(decimalPlaces, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            return rounded.scale() > 0
                    ? rounded.toPlainString()
                    : rounded.setScale(1).toPlainString();
        }
    }

    /** Explicitly bound integer slider row. */
    private record IntegerSliderItem(String label, IntSupplier getter, IntConsumer setter, int minimum, int maximum)
            implements SliderItem {
        @Override
        public void apply(float fraction) {
            long range = (long) maximum - minimum;
            long selectedValue = Math.round(minimum + (double) fraction * range);
            setter.accept(Math.clamp(selectedValue, minimum, maximum));
        }

        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            int value = getter.getAsInt();
            long range = (long) maximum - minimum;
            float fraction = maximum == minimum
                    ? 1.0f
                    : Math.clamp((float) (((long) value - minimum) / (double) range), 0.0f, 1.0f);
            float sliderX = x + SLIDER_X_OFFSET;
            float trackY = y + ITEM_HEIGHT * 0.5f - 2.0f;
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            canvas.roundedRectangle(sliderX, trackY, SLIDER_WIDTH, 4.0f, 2.0f, theme.control(), 1.0f);
            canvas.roundedRectangle(sliderX, trackY, SLIDER_WIDTH * fraction, 4.0f, 2.0f, theme.accent(), 1.0f);
            float thumbX = sliderX + SLIDER_WIDTH * fraction - 6.0f;
            canvas.roundedRectangle(thumbX, trackY - 4.0f, 12.0f, 12.0f, 6.0f, theme.accent(), 1.0f);
            String valueText = Integer.toString(value);
            float valueX = x + WIDTH - SLIDER_RIGHT_PADDING - FONT.width(valueText, VALUE_FONT_SIZE);
            FONT.text(canvas, valueX, y + 11.5f, valueText, VALUE_FONT_SIZE, theme.text());
        }
    }

    /** Explicitly bound finite-choice row. */
    private record ChoiceItem<T>(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices)
            implements Item {
        /** Selects the adjacent choice, clamped at the corresponding endpoint. */
        boolean select(boolean next) {
            int currentIndex = currentIndex();
            int selectedIndex = Math.clamp((long) currentIndex + (next ? 1 : -1), 0, choices.size() - 1);
            if (selectedIndex == currentIndex) {
                return false;
            }
            setter.accept(choices.get(selectedIndex).value());
            return true;
        }

        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            String value = choices.get(currentIndex()).label();
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            float valueX = x + WIDTH - HORIZONTAL_PADDING - FONT.width(value, VALUE_FONT_SIZE);
            FONT.text(canvas, valueX, y + 11.5f, value, VALUE_FONT_SIZE, theme.text());
            paintChoiceChevron(canvas, x + SLIDER_X_OFFSET - 8.0f, y + ITEM_HEIGHT * 0.5f, false, theme);
            paintChoiceChevron(canvas, x + WIDTH - 5.0f, y + ITEM_HEIGHT * 0.5f, true, theme);
        }

        /** Locates the bound value in the finite choice list. */
        private int currentIndex() {
            return currentChoiceIndex(label, getter, choices);
        }

        /** Paints one previous/next chevron. */
        private static void paintChoiceChevron(
                GuiCanvas canvas, float x, float y, boolean pointsRight, GuiTheme theme) {
            float direction = pointsRight ? 1.0f : -1.0f;
            canvas.line(x - 2.0f * direction, y - 3.0f, x + 2.0f * direction, y, 1.5f, theme.mutedText(), 1.0f);
            canvas.line(x + 2.0f * direction, y, x - 2.0f * direction, y + 3.0f, 1.5f, theme.mutedText(), 1.0f);
        }
    }

    /** Explicitly bound visible radio-button group. */
    private record RadioGroupItem<T>(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices)
            implements Item {
        @Override
        public float height() {
            return ITEM_HEIGHT + OPTION_HEIGHT * choices.size();
        }

        /** Selects the option under a pointer offset below the group heading. */
        boolean select(float pointerOffset) {
            if (pointerOffset < ITEM_HEIGHT) {
                return false;
            }
            int index = Math.clamp((int) ((pointerOffset - ITEM_HEIGHT) / OPTION_HEIGHT), 0, choices.size() - 1);
            if (index == currentChoiceIndex(label, getter, choices)) {
                return false;
            }
            setter.accept(choices.get(index).value());
            return true;
        }

        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            int currentIndex = currentChoiceIndex(label, getter, choices);
            for (int index = 0; index < choices.size(); index++) {
                paintOption(
                        canvas, x, y + ITEM_HEIGHT + index * OPTION_HEIGHT, index, currentIndex, pointerOffset, theme);
            }
        }

        /** Paints one radio option with its selected and hovered state. */
        private void paintOption(
                GuiCanvas canvas, float x, float y, int index, int currentIndex, float pointerOffset, GuiTheme theme) {
            boolean hovered = pointerOffset >= ITEM_HEIGHT + index * OPTION_HEIGHT
                    && pointerOffset < ITEM_HEIGHT + (index + 1) * OPTION_HEIGHT;
            if (hovered) {
                canvas.rectangle(x + 1.0f, y, WIDTH - 2.0f, OPTION_HEIGHT, theme.rowHover(), 1.0f);
            }
            float radioX = x + HORIZONTAL_PADDING + 7.0f;
            float radioY = y + OPTION_HEIGHT * 0.5f;
            canvas.roundedRectangle(radioX - 7.0f, radioY - 7.0f, 14.0f, 14.0f, 7.0f, theme.border(), 1.0f);
            canvas.roundedRectangle(
                    radioX - 5.0f,
                    radioY - 5.0f,
                    10.0f,
                    10.0f,
                    5.0f,
                    index == currentIndex ? theme.accent() : theme.control(),
                    1.0f);
            FONT.text(
                    canvas,
                    x + HORIZONTAL_PADDING + 25.0f,
                    y + 7.0f,
                    choices.get(index).label(),
                    VALUE_FONT_SIZE,
                    index == currentIndex ? theme.text() : theme.secondaryText());
        }
    }

    /** Explicitly bound compact select with an inline expanded option list. */
    private static final class SelectItem<T> implements Item {
        private final String label;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final List<Choice<T>> choices;

        private boolean expanded;

        /** Retains the validated select binding and immutable choices. */
        private SelectItem(String label, Supplier<T> getter, Consumer<T> setter, List<Choice<T>> choices) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.choices = choices;
        }

        /** Replaces the expanded state owned by the enclosing panel. */
        private void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        @Override
        public float height() {
            return ITEM_HEIGHT + (expanded ? OPTION_HEIGHT * choices.size() : 0.0f);
        }

        /** Selects one expanded option from its local vertical offset. */
        private void select(float optionOffset) {
            int index = Math.clamp((int) (optionOffset / OPTION_HEIGHT), 0, choices.size() - 1);
            if (index == currentChoiceIndex(label, getter, choices)) {
                return;
            }
            setter.accept(choices.get(index).value());
        }

        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            paintValue(canvas, x, y, theme);
            if (expanded) {
                paintOptions(canvas, x, y, pointerOffset, theme);
            }
        }

        /** Paints the closed select field and disclosure chevron. */
        private void paintValue(GuiCanvas canvas, float x, float y, GuiTheme theme) {
            String value =
                    choices.get(currentChoiceIndex(label, getter, choices)).label();
            float fieldX = x + SLIDER_X_OFFSET - 8.0f;
            float fieldWidth = WIDTH - (fieldX - x) - HORIZONTAL_PADDING;
            canvas.roundedRectangle(fieldX, y + 6.0f, fieldWidth, 26.0f, 4.0f, theme.control(), 1.0f);
            FONT.text(canvas, fieldX + 7.0f, y + 11.5f, value, VALUE_FONT_SIZE, theme.text());
            float chevronX = fieldX + fieldWidth - 10.0f;
            float chevronY = y + ITEM_HEIGHT * 0.5f;
            float direction = expanded ? -1.0f : 1.0f;
            canvas.line(
                    chevronX - 3.0f,
                    chevronY - direction * 2.0f,
                    chevronX,
                    chevronY + direction,
                    1.5f,
                    theme.mutedText(),
                    1.0f);
            canvas.line(
                    chevronX,
                    chevronY + direction,
                    chevronX + 3.0f,
                    chevronY - direction * 2.0f,
                    1.5f,
                    theme.mutedText(),
                    1.0f);
        }

        /** Paints every expanded select option. */
        private void paintOptions(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            int currentIndex = currentChoiceIndex(label, getter, choices);
            for (int index = 0; index < choices.size(); index++) {
                float optionY = y + ITEM_HEIGHT + index * OPTION_HEIGHT;
                boolean hovered = pointerOffset >= ITEM_HEIGHT + index * OPTION_HEIGHT
                        && pointerOffset < ITEM_HEIGHT + (index + 1) * OPTION_HEIGHT;
                boolean selected = index == currentIndex;
                canvas.rectangle(
                        x + 1.0f, optionY, WIDTH - 2.0f, OPTION_HEIGHT, hovered ? theme.rowHover() : theme.row(), 1.0f);
                FONT.text(
                        canvas,
                        x + HORIZONTAL_PADDING + 12.0f,
                        optionY + 7.0f,
                        choices.get(index).label(),
                        VALUE_FONT_SIZE,
                        selected ? theme.text() : theme.secondaryText());
                if (selected) {
                    canvas.roundedRectangle(
                            x + HORIZONTAL_PADDING,
                            optionY + OPTION_HEIGHT * 0.5f - 3.0f,
                            6.0f,
                            6.0f,
                            3.0f,
                            theme.accent(),
                            1.0f);
                }
            }
        }
    }

    /** Action-button row. */
    private record ButtonItem(String label, BooleanSupplier enabled, Runnable action) implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            float buttonX = x + HORIZONTAL_PADDING;
            float buttonY = y + 6.0f;
            float buttonWidth = WIDTH - HORIZONTAL_PADDING * 2.0f;
            boolean isEnabled = enabled.getAsBoolean();
            canvas.roundedRectangle(
                    buttonX,
                    buttonY,
                    buttonWidth,
                    26.0f,
                    5.0f,
                    isEnabled ? theme.accent() : theme.control(),
                    isEnabled && pointerOffset >= 0.0f ? 1.0f : 0.82f);
            float textX = buttonX + (buttonWidth - FONT.width(label, ITEM_FONT_SIZE)) * 0.5f;
            FONT.text(canvas, textX, y + 11.0f, label, ITEM_FONT_SIZE, isEnabled ? theme.text() : theme.mutedText());
        }
    }

    /** Explicitly bound read-only text row. */
    private record TextItem(String label, Supplier<String> getter) implements Item {
        @Override
        public void paint(GuiCanvas canvas, float x, float y, float pointerOffset, GuiTheme theme) {
            String value = Objects.requireNonNull(getter.get(), label + " value");
            FONT.text(canvas, x + HORIZONTAL_PADDING, y + 11.0f, label, ITEM_FONT_SIZE, theme.secondaryText());
            float valueX = x + WIDTH - HORIZONTAL_PADDING - FONT.width(value, VALUE_FONT_SIZE);
            FONT.text(canvas, valueX, y + 11.5f, value, VALUE_FONT_SIZE, theme.text());
        }
    }

    /** Locates a bound value in its finite choice list. */
    private static <T> int currentChoiceIndex(String label, Supplier<T> getter, List<Choice<T>> choices) {
        T currentValue = Objects.requireNonNull(getter.get(), label + " value");
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index).value().equals(currentValue)) {
                return index;
            }
        }
        throw new IllegalStateException(label + " value is not one of the configured choices: " + currentValue);
    }
}
