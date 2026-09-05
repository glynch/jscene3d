/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/** Creates consistently configured JSON generators for Doom import artifacts. */
final class DoomJsonGenerators {
    private static final JsonFactory JSON_FACTORY =
            JsonFactory.builder().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

    /** Prevents construction of this stateless serializer support class. */
    private DoomJsonGenerators() {
        throw new AssertionError("DoomJsonGenerators cannot be instantiated");
    }

    /** Opens a pretty-printing generator that does not close the supplied artifact stream. */
    static JsonGenerator create(OutputStream output) throws IOException {
        JsonGenerator generator =
                JSON_FACTORY.createGenerator(Objects.requireNonNull(output, "output"), JsonEncoding.UTF8);
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        generator.setPrettyPrinter(prettyPrinter);
        return generator;
    }
}
