/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/** Deterministic pretty printer for retained project JSON trees. */
public final class ProjectJsonTreeWriter {
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    /** Prevents construction of this stateless writer. */
    private ProjectJsonTreeWriter() {
        throw new AssertionError("ProjectJsonTreeWriter cannot be instantiated");
    }

    /**
     * Serializes a complete tree while preserving object and array order and appending one newline.
     *
     * @param tree complete project JSON tree
     * @return deterministic UTF-8 JSON bytes
     * @throws IOException when the tree cannot be serialized
     */
    public static byte[] serialize(JsonNode tree) throws IOException {
        byte[] content = WRITER.writeValueAsBytes(Objects.requireNonNull(tree, "tree"));
        if (content.length > 0 && content[content.length - 1] == '\n') {
            return content;
        }
        byte[] terminated = Arrays.copyOf(content, content.length + 1);
        terminated[content.length] = '\n';
        return terminated;
    }
}
