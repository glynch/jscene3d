/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import java.util.Arrays;

/** Host-owned application transitions which live Worlds may request. */
public enum ApplicationCommand {
    /** Requests a host-selected menu while retaining gameplay for resume where supported. */
    SHOW_MENU("show-menu"),
    /** Requests a host-selected menu and discards the gameplay session where supported. */
    RETURN_TO_MENU("return-to-menu"),
    /** Replaces any retained gameplay with a fresh instance of the selected launch Scene. */
    NEW_GAME("new-game"),
    /** Returns to the retained gameplay world when one exists. */
    RESUME("resume"),
    /** Requests orderly application shutdown. */
    QUIT("quit");

    private final String id;

    ApplicationCommand(String id) {
        this.id = id;
    }

    /**
     * Returns the stable descriptor value.
     *
     * @return lowercase command identity
     */
    public String id() {
        return id;
    }

    /**
     * Resolves one descriptor-authored command identity.
     *
     * @param id lowercase command identity
     * @return matching command
     */
    public static ApplicationCommand fromId(String id) {
        return Arrays.stream(values())
                .filter(command -> command.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported application command: " + id));
    }
}
