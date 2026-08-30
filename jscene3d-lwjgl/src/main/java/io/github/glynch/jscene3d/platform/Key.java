/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** A physical keyboard key recognized by the version 0.1 input interface. */
public enum Key {
    /** The A key. */
    A(GLFW.GLFW_KEY_A),
    /** The B key. */
    B(GLFW.GLFW_KEY_B),
    /** The C key. */
    C(GLFW.GLFW_KEY_C),
    /** The D key. */
    D(GLFW.GLFW_KEY_D),
    /** The E key. */
    E(GLFW.GLFW_KEY_E),
    /** The F key. */
    F(GLFW.GLFW_KEY_F),
    /** The G key. */
    G(GLFW.GLFW_KEY_G),
    /** The H key. */
    H(GLFW.GLFW_KEY_H),
    /** The I key. */
    I(GLFW.GLFW_KEY_I),
    /** The J key. */
    J(GLFW.GLFW_KEY_J),
    /** The K key. */
    K(GLFW.GLFW_KEY_K),
    /** The L key. */
    L(GLFW.GLFW_KEY_L),
    /** The M key. */
    M(GLFW.GLFW_KEY_M),
    /** The N key. */
    N(GLFW.GLFW_KEY_N),
    /** The O key. */
    O(GLFW.GLFW_KEY_O),
    /** The P key. */
    P(GLFW.GLFW_KEY_P),
    /** The Q key. */
    Q(GLFW.GLFW_KEY_Q),
    /** The R key. */
    R(GLFW.GLFW_KEY_R),
    /** The S key. */
    S(GLFW.GLFW_KEY_S),
    /** The T key. */
    T(GLFW.GLFW_KEY_T),
    /** The U key. */
    U(GLFW.GLFW_KEY_U),
    /** The V key. */
    V(GLFW.GLFW_KEY_V),
    /** The W key. */
    W(GLFW.GLFW_KEY_W),
    /** The X key. */
    X(GLFW.GLFW_KEY_X),
    /** The Y key. */
    Y(GLFW.GLFW_KEY_Y),
    /** The Z key. */
    Z(GLFW.GLFW_KEY_Z),
    /** The top-row 0 key. */
    DIGIT_0(GLFW.GLFW_KEY_0),
    /** The top-row 1 key. */
    DIGIT_1(GLFW.GLFW_KEY_1),
    /** The top-row 2 key. */
    DIGIT_2(GLFW.GLFW_KEY_2),
    /** The top-row 3 key. */
    DIGIT_3(GLFW.GLFW_KEY_3),
    /** The top-row 4 key. */
    DIGIT_4(GLFW.GLFW_KEY_4),
    /** The top-row 5 key. */
    DIGIT_5(GLFW.GLFW_KEY_5),
    /** The top-row 6 key. */
    DIGIT_6(GLFW.GLFW_KEY_6),
    /** The top-row 7 key. */
    DIGIT_7(GLFW.GLFW_KEY_7),
    /** The top-row 8 key. */
    DIGIT_8(GLFW.GLFW_KEY_8),
    /** The top-row 9 key. */
    DIGIT_9(GLFW.GLFW_KEY_9),
    /** The Escape key. */
    ESCAPE(GLFW.GLFW_KEY_ESCAPE),
    /** The Space key. */
    SPACE(GLFW.GLFW_KEY_SPACE),
    /** The Enter key. */
    ENTER(GLFW.GLFW_KEY_ENTER),
    /** The Tab key. */
    TAB(GLFW.GLFW_KEY_TAB),
    /** The Backspace key. */
    BACKSPACE(GLFW.GLFW_KEY_BACKSPACE),
    /** The Insert key. */
    INSERT(GLFW.GLFW_KEY_INSERT),
    /** The Delete key. */
    DELETE(GLFW.GLFW_KEY_DELETE),
    /** The left arrow key. */
    LEFT(GLFW.GLFW_KEY_LEFT),
    /** The right arrow key. */
    RIGHT(GLFW.GLFW_KEY_RIGHT),
    /** The up arrow key. */
    UP(GLFW.GLFW_KEY_UP),
    /** The down arrow key. */
    DOWN(GLFW.GLFW_KEY_DOWN),
    /** The Page Up key. */
    PAGE_UP(GLFW.GLFW_KEY_PAGE_UP),
    /** The Page Down key. */
    PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN),
    /** The Home key. */
    HOME(GLFW.GLFW_KEY_HOME),
    /** The End key. */
    END(GLFW.GLFW_KEY_END),
    /** The F1 key. */
    F1(GLFW.GLFW_KEY_F1),
    /** The F2 key. */
    F2(GLFW.GLFW_KEY_F2),
    /** The F3 key. */
    F3(GLFW.GLFW_KEY_F3),
    /** The F4 key. */
    F4(GLFW.GLFW_KEY_F4),
    /** The F5 key. */
    F5(GLFW.GLFW_KEY_F5),
    /** The F6 key. */
    F6(GLFW.GLFW_KEY_F6),
    /** The F7 key. */
    F7(GLFW.GLFW_KEY_F7),
    /** The F8 key. */
    F8(GLFW.GLFW_KEY_F8),
    /** The F9 key. */
    F9(GLFW.GLFW_KEY_F9),
    /** The F10 key. */
    F10(GLFW.GLFW_KEY_F10),
    /** The F11 key. */
    F11(GLFW.GLFW_KEY_F11),
    /** The F12 key. */
    F12(GLFW.GLFW_KEY_F12),
    /** The left Shift key. */
    LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT),
    /** The left Control key. */
    LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
    /** The left Alt or Option key. */
    LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT),
    /** The left Super or Command key. */
    LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER),
    /** The right Shift key. */
    RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT),
    /** The right Control key. */
    RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL),
    /** The right Alt or Option key. */
    RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT),
    /** The right Super or Command key. */
    RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER),
    /** The Menu key. */
    MENU(GLFW.GLFW_KEY_MENU);

    private static final Key[] BY_PLATFORM_CODE = createLookup();

    private final int platformCode;

    Key(int platformCode) {
        this.platformCode = platformCode;
    }

    static int platformCodeLimit() {
        return GLFW.GLFW_KEY_LAST + 1;
    }

    int platformCode() {
        return platformCode;
    }

    static @Nullable Key fromPlatformCode(int platformCode) {
        return platformCode >= 0 && platformCode < BY_PLATFORM_CODE.length ? BY_PLATFORM_CODE[platformCode] : null;
    }

    private static Key[] createLookup() {
        Key[] lookup = new Key[GLFW.GLFW_KEY_LAST + 1];
        for (Key key : values()) {
            lookup[key.platformCode] = key;
        }
        return lookup;
    }
}
