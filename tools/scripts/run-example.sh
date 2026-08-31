#!/usr/bin/env bash

# Copyright 2026 Graham Lynch
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIRECTORY
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
readonly PROJECT_DIRECTORY
readonly EXAMPLES_PACKAGE="io.github.glynch.jscene3d.examples"
readonly EXAMPLES_SOURCE_DIRECTORY="${PROJECT_DIRECTORY}/jscene3d-examples/src/main/java/io/github/glynch/jscene3d/examples"

print_examples() {
    find "${EXAMPLES_SOURCE_DIRECTORY}" -maxdepth 1 -type f -name '*.java' -exec grep -l 'public static void main' {} \; \
        | while IFS= read -r source_file; do
            basename "${source_file}" .java
        done \
        | sort
}

print_usage() {
    printf 'Usage: %s <ExampleName>\n' "$0"
    printf '       %s --list\n' "$0"
}

if [[ $# -ne 1 ]]; then
    print_usage >&2
    exit 2
fi

if [[ $1 == "--list" ]]; then
    print_examples
    exit 0
fi

if [[ $1 == "--help" || $1 == "-h" ]]; then
    print_usage
    exit 0
fi

readonly EXAMPLE_NAME="$1"
readonly EXAMPLE_SOURCE="${EXAMPLES_SOURCE_DIRECTORY}/${EXAMPLE_NAME}.java"

if [[ ! -f "${EXAMPLE_SOURCE}" ]]; then
    printf 'Unknown example: %s\n\nAvailable examples:\n' "${EXAMPLE_NAME}" >&2
    print_examples >&2
    exit 2
fi

exec "${PROJECT_DIRECTORY}/mvnw" \
    -f "${PROJECT_DIRECTORY}/pom.xml" \
    clean \
    compile \
    -pl jscene3d-examples \
    -am \
    -Prun-example \
    "-Djscene3d.exampleMainClass=${EXAMPLES_PACKAGE}.${EXAMPLE_NAME}"
