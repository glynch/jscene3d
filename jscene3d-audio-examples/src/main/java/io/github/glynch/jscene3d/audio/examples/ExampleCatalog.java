/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import io.github.glynch.jscene3d.gui.GalleryAttribution;
import java.net.URI;
import java.util.List;

/** Declares the stable ordered audio example suite and its CC0 asset provenance. */
public final class ExampleCatalog {
    private static final URI CC0 = URI.create("https://creativecommons.org/publicdomain/zero/1.0/");
    private static final GalleryAttribution INTERFACE_SOUNDS = new GalleryAttribution(
            "Interface Sounds",
            "Kenney",
            "Kenney Interface Sounds",
            URI.create("https://kenney.nl/assets/interface-sounds"),
            "CC0-1.0",
            CC0);
    private static final GalleryAttribution MUSIC_JINGLES = new GalleryAttribution(
            "Music Jingles",
            "Kenney Vleugels",
            "Kenney Music Jingles",
            URI.create("https://kenney.nl/assets/music-jingles"),
            "CC0-1.0",
            CC0);
    private static final GalleryAttribution SCI_FI_SOUNDS = new GalleryAttribution(
            "Sci-Fi Sounds",
            "Kenney",
            "Kenney Sci-Fi Sounds",
            URI.create("https://kenney.nl/assets/sci-fi-sounds"),
            "CC0-1.0",
            CC0);

    /** Prevents instantiation of this static catalog. */
    private ExampleCatalog() {
        throw new AssertionError("ExampleCatalog cannot be instantiated");
    }

    /**
     * Returns the complete audio example suite.
     *
     * @return audio suite metadata and factories
     */
    public static ExampleSuite suite() {
        return new ExampleSuite(
                "JScene3D Audio Examples",
                "JScene3D Audio",
                ExampleCatalog.class,
                "/META-INF/jscene3d/audio-examples/thumbnails",
                definitions());
    }

    /** Returns every audio definition in stable display order. */
    static List<ExampleDefinition> definitions() {
        return List.of(
                new ExampleDefinition(
                        "positional-audio",
                        "Positional audio",
                        "Audio",
                        "A mono effect circles the listener while distance attenuation and the camera listener remain synchronized.",
                        List.of("audio", "openal", "3d", "positional", "listener", "attenuation"),
                        List.of(SCI_FI_SOUNDS),
                        PositionalAudioExample::create),
                new ExampleDefinition(
                        "audio-mixing",
                        "Music and effects mixing",
                        "Audio",
                        "Independent master, music, and effects volumes control a stereo jingle and non-positional interface sound.",
                        List.of("audio", "openal", "music", "effects", "volume", "mixing", "interface"),
                        List.of(MUSIC_JINGLES, INTERFACE_SOUNDS),
                        AudioMixingExample::create));
    }
}
