/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.gui.internal.GuiCanvas;
import io.github.glynch.jscene3d.gui.internal.GuiFont;
import io.github.glynch.jscene3d.gui.internal.OverlayGuiCanvas;
import io.github.glynch.jscene3d.gui.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Searchable, scrollable thumbnail gallery occupying the left side of a window. */
public final class GalleryPanel implements Overlay {
    /** Fixed logical width reserved by the gallery. */
    public static final int WIDTH = 332;

    private static final float HEADER_HEIGHT = 60.0f;
    private static final float SEARCH_HEIGHT = 42.0f;
    private static final float STATUS_HEIGHT = 30.0f;
    private static final float OUTER_PADDING = 14.0f;
    private static final float CARD_GAP = 14.0f;
    private static final float CARD_HEIGHT = 208.0f;
    private static final float CARD_IMAGE_HEIGHT = 142.0f;
    private static final float CARD_WIDTH = WIDTH - OUTER_PADDING * 2.0f;
    private static final float CONTENT_TOP = HEADER_HEIGHT + SEARCH_HEIGHT + STATUS_HEIGHT;
    private static final float TITLE_FONT_SIZE = 19.0f;
    private static final float BODY_FONT_SIZE = 13.0f;
    private static final float SMALL_FONT_SIZE = 11.0f;
    private static final float ATTRIBUTION_WIDTH = 410.0f;
    private static final float ATTRIBUTION_PADDING = 10.0f;
    private static final float ATTRIBUTION_ROW_HEIGHT = 72.0f;
    private static final double SCROLL_UNITS_PER_CARD = 3.0;
    private static final int MAXIMUM_QUERY_CODE_POINTS = 80;
    private static final GuiFont FONT = GuiFont.defaultFont();

    private final @Nullable Window window;
    private final String title;
    private final List<GalleryItem> items;
    private final GuiTheme theme;
    private final int[] filteredIndices;
    private final StringBuilder query = new StringBuilder();
    private final OverlayGuiCanvas overlayCanvas = new OverlayGuiCanvas();

    private int filteredCount;
    private int selectedIndex;
    private int firstVisibleIndex;
    private Focus focus = Focus.NONE;
    private boolean capturesKeyboard;
    private boolean capturesPointer;
    private double accumulatedScrollY;
    private double pointerX = Double.NEGATIVE_INFINITY;
    private double pointerY = Double.NEGATIVE_INFINITY;

    /**
     * Creates a gallery bound to one window with the default dark theme.
     *
     * @param window input and dimensions for the gallery
     * @param title non-blank gallery title
     * @param items non-empty immutable item descriptions
     */
    public GalleryPanel(Window window, String title, List<GalleryItem> items) {
        this(window, title, items, GuiTheme.dark());
    }

    /**
     * Creates a gallery bound to one window with an explicit theme.
     *
     * @param window input and dimensions for the gallery
     * @param title non-blank gallery title
     * @param items non-empty immutable item descriptions
     * @param theme immutable visual theme
     */
    public GalleryPanel(Window window, String title, List<GalleryItem> items, GuiTheme theme) {
        this.window = Objects.requireNonNull(window, "window");
        this.title = Preconditions.requireNonBlank(title, "title");
        this.items = requireItems(items);
        this.theme = Objects.requireNonNull(theme, "theme");
        filteredIndices = new int[this.items.size()];
        updateFilter();
    }

    /** Creates a detached gallery for headless interaction and layout tests. */
    GalleryPanel(String title, List<GalleryItem> items) {
        window = null;
        this.title = Preconditions.requireNonBlank(title, "title");
        this.items = requireItems(items);
        theme = GuiTheme.dark();
        filteredIndices = new int[this.items.size()];
        updateFilter();
    }

    /**
     * Applies text, pointer, and scroll input from the latest event poll.
     *
     * @return {@code true} if selection, filtering, focus, or scrolling changed
     */
    public boolean update() {
        Window validWindow = requireWindow();
        InputState input = validWindow.input();
        StringBuilder typedText = new StringBuilder();
        for (int index = 0; index < input.typedCodePointCount(); index++) {
            int codePoint = input.typedCodePoint(index);
            if (!Character.isISOControl(codePoint)) {
                typedText.appendCodePoint(codePoint);
            }
        }
        GalleryInput galleryInput = new GalleryInput(
                input.pointerX(),
                input.pointerY(),
                input.wasMouseButtonPressed(MouseButton.LEFT),
                input.scrollDeltaY(),
                input.wasKeyPressed(Key.BACKSPACE),
                typedText.toString(),
                Navigation.from(input),
                Navigation.isAnyDown(input));
        return update(galleryInput, validWindow.width(), validWindow.height());
    }

    /**
     * Returns the currently selected item, which remains selected while filters change.
     *
     * @return selected immutable gallery item
     */
    public GalleryItem selectedItem() {
        return items.get(selectedIndex);
    }

    /**
     * Returns the current search query.
     *
     * @return possibly empty query
     */
    public String query() {
        return query.toString();
    }

    /**
     * Returns whether the search field currently receives text input.
     *
     * @return whether search has keyboard focus
     */
    public boolean isSearchFocused() {
        return focus == Focus.SEARCH;
    }

    /**
     * Returns whether keyboard input belongs to the gallery rather than the live example.
     *
     * @return whether search is focused or a gallery navigation key is held
     */
    public boolean capturesKeyboard() {
        return capturesKeyboard;
    }

    /**
     * Returns whether pointer input belongs to the gallery rather than the live example.
     *
     * @return whether the latest pointer position lies within the gallery
     */
    public boolean capturesPointer() {
        return capturesPointer;
    }

    /**
     * Returns the number of items matching the current query.
     *
     * @return non-negative matching item count
     */
    public int matchingItemCount() {
        return filteredCount;
    }

    /**
     * Paints the complete gallery over the left side of the current framebuffer.
     *
     * @param canvas current overlay drawing surface
     * @param width positive logical window width
     * @param height positive logical window height
     */
    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        overlayCanvas.bind(Objects.requireNonNull(canvas, "canvas"));
        try {
            paint(overlayCanvas, width, height);
        } finally {
            overlayCanvas.unbind();
        }
    }

    /** Paints through the internal headless-testable drawing boundary. */
    void paint(GuiCanvas canvas, int width, int height) {
        Objects.requireNonNull(canvas, "canvas");
        Preconditions.requirePositive(width, "width");
        Preconditions.requirePositive(height, "height");
        canvas.rectangle(0.0f, 0.0f, WIDTH, height, theme.panel(), 1.0f);
        canvas.rectangle(WIDTH - 1.0f, 0.0f, 1.0f, height, theme.border(), 1.0f);
        paintHeader(canvas);
        paintSearch(canvas);
        paintStatus(canvas);
        paintCards(canvas, height);
        paintAttribution(canvas, width, height);
    }

    /** Paints selected third-party provenance without crowding the thumbnail cards. */
    private void paintAttribution(GuiCanvas canvas, int width, int height) {
        List<GalleryAttribution> attributions = selectedItem().attributions();
        int maximumRows = (int) ((height - 32.0f - ATTRIBUTION_PADDING * 2.0f) / ATTRIBUTION_ROW_HEIGHT);
        int visibleRows = Math.clamp(maximumRows, 0, attributions.size());
        if (visibleRows == 0 || width <= WIDTH + 40) {
            return;
        }
        float panelHeight = ATTRIBUTION_PADDING * 2.0f + visibleRows * ATTRIBUTION_ROW_HEIGHT;
        float panelWidth = Math.min(ATTRIBUTION_WIDTH, width - WIDTH - 32.0f);
        float x = WIDTH + 16.0f;
        float y = height - panelHeight - 16.0f;
        canvas.roundedRectangle(x + 3.0f, y + 4.0f, panelWidth, panelHeight, 8.0f, theme.shadow(), 0.35f);
        canvas.roundedRectangle(x, y, panelWidth, panelHeight, 8.0f, theme.border(), 0.96f);
        canvas.roundedRectangle(x + 1.0f, y + 1.0f, panelWidth - 2.0f, panelHeight - 2.0f, 7.0f, theme.panel(), 0.92f);
        for (int index = 0; index < visibleRows; index++) {
            float rowY = y + ATTRIBUTION_PADDING + index * ATTRIBUTION_ROW_HEIGHT;
            paintAttribution(canvas, attributions.get(index), x, rowY);
            if (index + 1 < visibleRows) {
                canvas.rectangle(
                        x + 10.0f,
                        rowY + ATTRIBUTION_ROW_HEIGHT - 1.0f,
                        panelWidth - 20.0f,
                        1.0f,
                        theme.border(),
                        0.8f);
            }
        }
        int hiddenRows = attributions.size() - visibleRows;
        if (hiddenRows > 0) {
            String additional = "+" + hiddenRows + " more asset sources";
            float textX = x + panelWidth - 12.0f - FONT.width(additional, SMALL_FONT_SIZE);
            FONT.text(canvas, textX, y + panelHeight - 18.0f, additional, SMALL_FONT_SIZE, theme.mutedText());
        }
    }

    /** Paints complete compact provenance for one selected example asset. */
    private void paintAttribution(GuiCanvas canvas, GalleryAttribution attribution, float x, float y) {
        FONT.text(canvas, x + 12.0f, y + 4.0f, attribution.assetName(), BODY_FONT_SIZE, theme.text());
        FONT.text(
                canvas,
                x + 12.0f,
                y + 26.0f,
                "Creator: " + attribution.creator(),
                SMALL_FONT_SIZE,
                theme.secondaryText());
        FONT.text(
                canvas,
                x + 12.0f,
                y + 43.0f,
                "Source: " + attribution.sourceName(),
                SMALL_FONT_SIZE,
                theme.secondaryText());
        FONT.text(
                canvas, x + 12.0f, y + 60.0f, "License: " + attribution.licenseName(), SMALL_FONT_SIZE, theme.accent());
    }

    /** Paints the gallery title bar. */
    private void paintHeader(GuiCanvas canvas) {
        canvas.rectangle(0.0f, 0.0f, WIDTH, HEADER_HEIGHT, theme.title(), 1.0f);
        FONT.text(canvas, OUTER_PADDING, 18.0f, title, TITLE_FONT_SIZE, theme.accent());
        String suffix = "examples";
        float suffixX = WIDTH - OUTER_PADDING - FONT.width(suffix, BODY_FONT_SIZE);
        FONT.text(canvas, suffixX, 22.0f, suffix, BODY_FONT_SIZE, theme.secondaryText());
    }

    /** Paints the focused or inactive search field. */
    private void paintSearch(GuiCanvas canvas) {
        float x = OUTER_PADDING;
        float y = HEADER_HEIGHT + 7.0f;
        float width = CARD_WIDTH;
        Color border = isSearchFocused() ? theme.accent() : theme.border();
        canvas.roundedRectangle(x, y, width, 32.0f, 6.0f, border, 1.0f);
        canvas.roundedRectangle(x + 1.0f, y + 1.0f, width - 2.0f, 30.0f, 5.0f, theme.control(), 1.0f);
        String text = query.isEmpty() ? "Search examples" : visibleQuery(width - 38.0f);
        Color textColor = query.isEmpty() ? theme.mutedText() : theme.text();
        FONT.text(canvas, x + 10.0f, y + 8.0f, text, BODY_FONT_SIZE, textColor);
        if (!query.isEmpty()) {
            FONT.text(canvas, x + width - 20.0f, y + 8.0f, "x", BODY_FONT_SIZE, theme.mutedText());
        }
    }

    /** Paints result count and scroll position. */
    private void paintStatus(GuiCanvas canvas) {
        float y = HEADER_HEIGHT + SEARCH_HEIGHT;
        String count = filteredCount + (filteredCount == 1 ? " example" : " examples");
        FONT.text(canvas, OUTER_PADDING, y + 8.0f, count, SMALL_FONT_SIZE, theme.mutedText());
        if (firstVisibleIndex > 0) {
            FONT.text(canvas, WIDTH - 31.0f, y + 8.0f, "up", SMALL_FONT_SIZE, theme.mutedText());
        }
    }

    /** Paints each complete visible thumbnail card. */
    private void paintCards(GuiCanvas canvas, int height) {
        int visibleCount = visibleCardCount(height);
        if (filteredCount == 0) {
            FONT.text(canvas, OUTER_PADDING, CONTENT_TOP + 18.0f, "No matching examples", BODY_FONT_SIZE, theme.text());
            return;
        }
        int end = Math.min(filteredCount, firstVisibleIndex + visibleCount);
        float y = CONTENT_TOP;
        for (int filteredIndex = firstVisibleIndex; filteredIndex < end; filteredIndex++) {
            int itemIndex = filteredIndices[filteredIndex];
            paintCard(canvas, items.get(itemIndex), itemIndex == selectedIndex, y);
            y += CARD_HEIGHT + CARD_GAP;
        }
    }

    /** Paints one image, title, category, and selected-state card. */
    private void paintCard(GuiCanvas canvas, GalleryItem item, boolean selected, float y) {
        float x = OUTER_PADDING;
        Color border = selected ? theme.accent() : theme.border();
        boolean hovered = contains(pointerX, pointerY, x, y, CARD_WIDTH, CARD_HEIGHT);
        canvas.roundedRectangle(x, y, CARD_WIDTH, CARD_HEIGHT, 7.0f, border, 1.0f);
        canvas.roundedRectangle(
                x + 1.0f,
                y + 1.0f,
                CARD_WIDTH - 2.0f,
                CARD_HEIGHT - 2.0f,
                6.0f,
                hovered ? theme.rowHover() : theme.row(),
                1.0f);
        canvas.image(
                item.thumbnail().fullRegion(),
                x + 6.0f,
                y + 6.0f,
                CARD_WIDTH - 12.0f,
                CARD_IMAGE_HEIGHT,
                Color.WHITE,
                1.0f);
        FONT.text(canvas, x + 10.0f, y + CARD_IMAGE_HEIGHT + 14.0f, item.title(), BODY_FONT_SIZE, theme.text());
        FONT.text(
                canvas,
                x + 10.0f,
                y + CARD_IMAGE_HEIGHT + 38.0f,
                item.category(),
                SMALL_FONT_SIZE,
                selected ? theme.accent() : theme.mutedText());
    }

    /** Applies one headless-testable input snapshot. */
    boolean update(GalleryInput input, int windowWidth, int windowHeight) {
        GalleryInput validInput = Objects.requireNonNull(input, "input");
        Preconditions.requirePositive(windowWidth, "windowWidth");
        Preconditions.requirePositive(windowHeight, "windowHeight");
        pointerX = validInput.x();
        pointerY = validInput.y();
        boolean pointerInside = contains(pointerX, pointerY, 0.0f, 0.0f, WIDTH, windowHeight);
        capturesPointer = pointerInside;
        boolean changed = false;
        if (validInput.pressed()) {
            if (pointerInside) {
                changed |= activate(pointerX, pointerY, windowHeight);
            } else {
                changed |= setFocus(Focus.NONE);
                accumulatedScrollY = 0.0;
            }
        }
        if (pointerInside && validInput.scrollDeltaY() != 0.0) {
            changed |= scroll(validInput.scrollDeltaY(), windowHeight);
        }
        if (isSearchFocused()) {
            changed |= editQuery(validInput);
        }
        if (focus == Focus.NONE && validInput.navigation() != Navigation.NONE) {
            changed |= setFocus(Focus.LIST);
        }
        capturesKeyboard = isSearchFocused() || validInput.navigationHeld();
        if (capturesKeyboard() && validInput.navigation() != Navigation.NONE) {
            changed |= navigate(validInput.navigation(), windowHeight);
        }
        return changed;
    }

    /** Activates the search field, clear affordance, or one visible card. */
    private boolean activate(double x, double y, int windowHeight) {
        if (contains(x, y, OUTER_PADDING, HEADER_HEIGHT + 7.0f, CARD_WIDTH, 32.0f)) {
            boolean changed = setFocus(Focus.SEARCH);
            if (!query.isEmpty() && x >= WIDTH - OUTER_PADDING - 28.0f) {
                query.setLength(0);
                updateFilter();
                return true;
            }
            return changed;
        }
        boolean changed = setFocus(Focus.LIST);
        if (x < 0.0 || x >= WIDTH || y < CONTENT_TOP) {
            return changed;
        }
        int cardOffset = (int) ((y - CONTENT_TOP) / (CARD_HEIGHT + CARD_GAP));
        float cardY = CONTENT_TOP + cardOffset * (CARD_HEIGHT + CARD_GAP);
        if (cardOffset >= visibleCardCount(windowHeight)
                || !contains(x, y, OUTER_PADDING, cardY, CARD_WIDTH, CARD_HEIGHT)) {
            return changed;
        }
        int filteredIndex = firstVisibleIndex + cardOffset;
        if (filteredIndex >= filteredCount) {
            return changed;
        }
        int replacement = filteredIndices[filteredIndex];
        changed |= replacement != selectedIndex;
        selectedIndex = replacement;
        changed |= setFocus(Focus.NONE);
        return changed;
    }

    /** Accumulates normalized wheel or trackpad movement before advancing one complete card. */
    private boolean scroll(double deltaY, int windowHeight) {
        if (accumulatedScrollY * deltaY < 0.0) {
            accumulatedScrollY = 0.0;
        }
        double normalizedDelta = Math.clamp(deltaY, -1.0, 1.0);
        accumulatedScrollY =
                Math.clamp(accumulatedScrollY + normalizedDelta, -SCROLL_UNITS_PER_CARD, SCROLL_UNITS_PER_CARD);
        if (Math.abs(accumulatedScrollY) < SCROLL_UNITS_PER_CARD) {
            return false;
        }
        int cardDelta = accumulatedScrollY < 0.0 ? 1 : -1;
        accumulatedScrollY = 0.0;
        return scrollByCards(cardDelta, windowHeight);
    }

    /** Moves the first visible card by a signed count within the available result pages. */
    private boolean scrollByCards(int cardDelta, int windowHeight) {
        int maximumFirst = Math.max(0, filteredCount - visibleCardCount(windowHeight));
        int replacement = (int) Math.clamp((long) firstVisibleIndex + cardDelta, 0L, maximumFirst);
        if (replacement == firstVisibleIndex) {
            return false;
        }
        firstVisibleIndex = replacement;
        return true;
    }

    /** Applies one focused keyboard-navigation command to the filtered results. */
    private boolean navigate(Navigation navigation, int windowHeight) {
        if (filteredCount == 0) {
            return false;
        }
        int currentPosition = filteredPosition(selectedIndex);
        int finalPosition = filteredCount - 1;
        int pageSize = visibleCardCount(windowHeight);
        int replacementPosition =
                switch (navigation) {
                    case PREVIOUS -> currentPosition < 0 ? finalPosition : Math.max(currentPosition - 1, 0);
                    case NEXT -> currentPosition < 0 ? 0 : Math.min(currentPosition + 1, finalPosition);
                    case PREVIOUS_PAGE -> currentPosition < 0 ? finalPosition : Math.max(currentPosition - pageSize, 0);
                    case NEXT_PAGE -> currentPosition < 0 ? 0 : Math.min(currentPosition + pageSize, finalPosition);
                    case FIRST -> 0;
                    case LAST -> finalPosition;
                    case NONE -> currentPosition;
                };
        if (replacementPosition < 0) {
            return false;
        }
        boolean changed = selectedIndex != filteredIndices[replacementPosition];
        selectedIndex = filteredIndices[replacementPosition];
        return ensureVisible(replacementPosition, windowHeight) || changed;
    }

    /** Returns one item's position in the current filtered ordering, or {@code -1} when absent. */
    private int filteredPosition(int itemIndex) {
        for (int filteredIndex = 0; filteredIndex < filteredCount; filteredIndex++) {
            if (filteredIndices[filteredIndex] == itemIndex) {
                return filteredIndex;
            }
        }
        return -1;
    }

    /** Adjusts the first visible card so the supplied filtered position is fully visible. */
    private boolean ensureVisible(int filteredPosition, int windowHeight) {
        int visibleCount = visibleCardCount(windowHeight);
        int replacement = firstVisibleIndex;
        if (filteredPosition < firstVisibleIndex) {
            replacement = filteredPosition;
        } else if (filteredPosition >= firstVisibleIndex + visibleCount) {
            replacement = filteredPosition - visibleCount + 1;
        }
        int maximumFirst = Math.max(0, filteredCount - visibleCount);
        replacement = Math.clamp(replacement, 0, maximumFirst);
        if (replacement == firstVisibleIndex) {
            return false;
        }
        firstVisibleIndex = replacement;
        return true;
    }

    /** Applies backspace and entered Unicode text to the active query. */
    private boolean editQuery(GalleryInput input) {
        boolean changed = false;
        if (input.backspace() && !query.isEmpty()) {
            int lastCodePoint = query.codePointBefore(query.length());
            query.delete(query.length() - Character.charCount(lastCodePoint), query.length());
            changed = true;
        }
        int offset = 0;
        while (offset < input.typedText().length()
                && query.codePointCount(0, query.length()) < MAXIMUM_QUERY_CODE_POINTS) {
            int codePoint = input.typedText().codePointAt(offset);
            query.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
            changed = true;
        }
        if (changed) {
            updateFilter();
        }
        return changed;
    }

    /** Rebuilds matching indices from searchable item metadata. */
    private void updateFilter() {
        String normalizedQuery = query.toString().strip().toLowerCase(Locale.ROOT);
        filteredCount = 0;
        for (int index = 0; index < items.size(); index++) {
            if (matches(items.get(index), normalizedQuery)) {
                filteredIndices[filteredCount] = index;
                filteredCount++;
            }
        }
        firstVisibleIndex = 0;
        accumulatedScrollY = 0.0;
    }

    /** Returns whether every query term occurs in one item's searchable metadata. */
    private static boolean matches(GalleryItem item, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        StringBuilder searchableMetadata = new StringBuilder(item.title())
                .append(' ')
                .append(item.category())
                .append(' ')
                .append(item.description())
                .append(' ')
                .append(String.join(" ", item.tags()));
        for (GalleryAttribution attribution : item.attributions()) {
            searchableMetadata
                    .append(' ')
                    .append(attribution.assetName())
                    .append(' ')
                    .append(attribution.creator())
                    .append(' ')
                    .append(attribution.sourceName())
                    .append(' ')
                    .append(attribution.licenseName());
        }
        String searchable = searchableMetadata.toString().toLowerCase(Locale.ROOT);
        for (String term : normalizedQuery.split("\\s+")) {
            if (!searchable.contains(term)) {
                return false;
            }
        }
        return true;
    }

    /** Returns the tail of a long query that fits the search field. */
    private String visibleQuery(float maximumWidth) {
        String visible = query.toString();
        while (visible.length() > 1 && FONT.width(visible, BODY_FONT_SIZE) > maximumWidth) {
            int firstCodePoint = visible.codePointAt(0);
            visible = visible.substring(Character.charCount(firstCodePoint));
        }
        return visible;
    }

    /** Returns how many complete cards fit below the status row. */
    private static int visibleCardCount(int height) {
        return Math.max(1, (int) ((height - CONTENT_TOP + CARD_GAP) / (CARD_HEIGHT + CARD_GAP)));
    }

    /** Replaces gallery focus and reports whether it changed. */
    private boolean setFocus(Focus replacement) {
        if (focus == replacement) {
            return false;
        }
        focus = replacement;
        return true;
    }

    /** Validates and immutably copies a non-empty gallery item list. */
    private static List<GalleryItem> requireItems(List<GalleryItem> items) {
        List<GalleryItem> copy = List.copyOf(Objects.requireNonNull(items, "items"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        return copy;
    }

    /** Returns the bound window or reports detached test-only use. */
    private Window requireWindow() {
        if (window == null) {
            throw new IllegalStateException("Detached gallery has no window input");
        }
        return window;
    }

    /** Returns whether one point lies within a half-open rectangle. */
    private static boolean contains(double x, double y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** Distinguishes the native gallery's keyboard focus targets. */
    private enum Focus {
        NONE,
        LIST,
        SEARCH
    }

    /** One filtered-result keyboard-navigation command. */
    enum Navigation {
        FIRST(Key.HOME),
        LAST(Key.END),
        PREVIOUS_PAGE(Key.PAGE_UP),
        NEXT_PAGE(Key.PAGE_DOWN),
        PREVIOUS(Key.UP),
        NEXT(Key.DOWN),
        NONE(null);

        private final @Nullable Key key;

        /** Associates a command with its physical key, or no key for the neutral command. */
        Navigation(@Nullable Key key) {
            this.key = key;
        }

        /** Returns the first navigation command pressed during the latest input poll. */
        private static Navigation from(InputState input) {
            for (Navigation navigation : values()) {
                if (navigation.key != null && input.wasKeyPressed(navigation.key)) {
                    return navigation;
                }
            }
            return NONE;
        }

        /** Returns whether any gallery navigation key is currently held. */
        private static boolean isAnyDown(InputState input) {
            for (Navigation navigation : values()) {
                if (navigation.key != null && input.isKeyDown(navigation.key)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** One headless-testable input snapshot. */
    record GalleryInput(
            double x,
            double y,
            boolean pressed,
            double scrollDeltaY,
            boolean backspace,
            String typedText,
            Navigation navigation,
            boolean navigationHeld) {
        /** Creates a snapshot without keyboard navigation. */
        GalleryInput(double x, double y, boolean pressed, double scrollDeltaY, boolean backspace, String typedText) {
            this(x, y, pressed, scrollDeltaY, backspace, typedText, Navigation.NONE, false);
        }

        /** Creates a snapshot whose navigation command remains held for this update. */
        GalleryInput(
                double x,
                double y,
                boolean pressed,
                double scrollDeltaY,
                boolean backspace,
                String typedText,
                Navigation navigation) {
            this(x, y, pressed, scrollDeltaY, backspace, typedText, navigation, navigation != Navigation.NONE);
        }

        /** Rejects a null text payload. */
        GalleryInput {
            Objects.requireNonNull(typedText, "typedText");
            Objects.requireNonNull(navigation, "navigation");
        }
    }
}
