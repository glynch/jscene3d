#!/usr/bin/env bash

# Copyright 2026 Graham Lynch
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIRECTORY
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
readonly PROJECT_DIRECTORY

print_usage() {
    printf 'Usage: %s [catalog-id ...]\n' "$0"
    printf 'Captures every example when no catalog IDs are supplied.\n'
}

if [[ $# -eq 1 && ($1 == "--help" || $1 == "-h") ]]; then
    print_usage
    exit 0
fi

for catalog_id in "$@"; do
    if [[ ! ${catalog_id} =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
        printf 'Invalid example catalog ID: %s\n\n' "${catalog_id}" >&2
        print_usage >&2
        exit 2
    fi
done

MAVEN_ARGUMENTS=(
    -f "${PROJECT_DIRECTORY}/pom.xml"
    compile
    -pl jscene3d-examples
    -am
    -Prun-example
    -Djscene3d.exampleMainClass=io.github.glynch.jscene3d.examples.tools.ExampleThumbnailCapture
)

thumbnail_selection=""
for catalog_id in "$@"; do
    if [[ -n ${thumbnail_selection} ]]; then
        thumbnail_selection+=,
    fi
    thumbnail_selection+="${catalog_id}"
done
readonly thumbnail_selection
MAVEN_ARGUMENTS+=("-Djscene3d.thumbnailSelection=${thumbnail_selection}")

exec "${PROJECT_DIRECTORY}/mvnw" "${MAVEN_ARGUMENTS[@]}"
