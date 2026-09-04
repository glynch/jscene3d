/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.diagnostic;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.doom.internal.Preconditions;

/** Stable diagnostic codes and English fallback messages owned by Doom content decoding. */
public enum DoomDiagnosticCode implements DiagnosticCode {
    /** No map marker has the requested name. */
    MAP_MISSING("doom.map.missing", "The requested Doom map marker is not present"),

    /** A map does not contain the required classic lump sequence. */
    MAP_LAYOUT_INVALID("doom.map.layout", "The Doom map lump sequence is incomplete or out of order"),

    /** The map uses the unsupported Universal Doom Map Format. */
    MAP_FORMAT_UDMF_UNSUPPORTED("doom.map.format.udmf", "UDMF maps are not supported"),

    /** The map uses the unsupported Hexen binary format. */
    MAP_FORMAT_HEXEN_UNSUPPORTED("doom.map.format.hexen", "Hexen-format maps are not supported"),

    /** A fixed-size classic map lump contains a partial record. */
    MAP_RECORD_SIZE_INVALID("doom.map.record-size", "A classic Doom map lump has an invalid record size"),

    /** A decoded index or range points outside its owning table. */
    MAP_REFERENCE_INVALID("doom.map.reference", "A Doom map reference points outside its owning table"),

    /** A decoded scalar lies outside the classic format's supported values. */
    MAP_VALUE_INVALID("doom.map.value", "A Doom map value is outside its supported range"),

    /** The REJECT table is too short for the map's sector count. */
    MAP_REJECT_SIZE_INVALID("doom.map.reject-size", "The Doom REJECT table is incomplete"),

    /** The BLOCKMAP structure is malformed. */
    MAP_BLOCKMAP_INVALID("doom.map.blockmap", "The Doom BLOCKMAP structure is invalid"),

    /** Map lump data could not be read or decoded safely. */
    MAP_DATA_UNREADABLE("doom.map.data", "The classic Doom map data could not be decoded");

    private final String value;
    private final String message;

    /** Stores one stable code and its English fallback. */
    DoomDiagnosticCode(String value, String message) {
        this.value = Preconditions.requireNonBlank(value, "value");
        this.message = Preconditions.requireNonBlank(message, "message");
    }

    @Override
    public String code() {
        return value;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
