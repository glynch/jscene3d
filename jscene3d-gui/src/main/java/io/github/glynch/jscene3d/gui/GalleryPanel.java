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
    private boolean searchFocused;
    private boolean capturesPointer;
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
                typedText.toString());
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
        return searchFocused;
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
        Color border = searchFocused ? theme.accent() : theme.border();
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
            changed |= activate(pointerX, pointerY, windowHeight);
            if (!pointerInside) {
                changed |= setSearchFocused(false);
            }
        }
        if (pointerInside && validInput.scrollDeltaY() != 0.0) {
            changed |= scroll(validInput.scrollDeltaY(), windowHeight);
        }
        if (searchFocused) {
            changed |= editQuery(validInput);
        }
        return changed;
    }

    /** Activates the search field, clear affordance, or one visible card. */
    private boolean activate(double x, double y, int windowHeight) {
        if (contains(x, y, OUTER_PADDING, HEADER_HEIGHT + 7.0f, CARD_WIDTH, 32.0f)) {
            if (!query.isEmpty() && x >= WIDTH - OUTER_PADDING - 28.0f) {
                query.setLength(0);
                updateFilter();
                return true;
            }
            return setSearchFocused(true);
        }
        if (x < 0.0 || x >= WIDTH || y < CONTENT_TOP) {
            return false;
        }
        int cardOffset = (int) ((y - CONTENT_TOP) / (CARD_HEIGHT + CARD_GAP));
        float cardY = CONTENT_TOP + cardOffset * (CARD_HEIGHT + CARD_GAP);
        if (cardOffset >= visibleCardCount(windowHeight)
                || !contains(x, y, OUTER_PADDING, cardY, CARD_WIDTH, CARD_HEIGHT)) {
            return false;
        }
        int filteredIndex = firstVisibleIndex + cardOffset;
        if (filteredIndex >= filteredCount) {
            return false;
        }
        int replacement = filteredIndices[filteredIndex];
        boolean changed = replacement != selectedIndex || searchFocused;
        selectedIndex = replacement;
        searchFocused = false;
        return changed;
    }

    /** Applies discrete card scrolling while keeping the final page full where possible. */
    private boolean scroll(double deltaY, int windowHeight) {
        int maximumFirst = Math.max(0, filteredCount - visibleCardCount(windowHeight));
        int replacement = firstVisibleIndex;
        if (deltaY < 0.0 && firstVisibleIndex < maximumFirst) {
            replacement++;
        } else if (deltaY > 0.0 && firstVisibleIndex > 0) {
            replacement--;
        }
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
    }

    /** Returns whether every query term occurs in one item's searchable metadata. */
    private static boolean matches(GalleryItem item, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String searchable = (item.title()
                        + ' '
                        + item.category()
                        + ' '
                        + item.description()
                        + ' '
                        + String.join(" ", item.tags()))
                .toLowerCase(Locale.ROOT);
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

    /** Replaces search focus and reports whether it changed. */
    private boolean setSearchFocused(boolean replacement) {
        if (searchFocused == replacement) {
            return false;
        }
        searchFocused = replacement;
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

    /** One headless-testable input snapshot. */
    record GalleryInput(double x, double y, boolean pressed, double scrollDeltaY, boolean backspace, String typedText) {
        /** Rejects a null text payload. */
        GalleryInput {
            Objects.requireNonNull(typedText, "typedText");
        }
    }
}
