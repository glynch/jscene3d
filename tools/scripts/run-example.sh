#!/usr/bin/env bash

# Copyright 2026 Graham Lynch
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIRECTORY
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
readonly PROJECT_DIRECTORY
readonly RENDERING_MODULE="jscene3d-examples"
readonly RENDERING_PACKAGE="io.github.glynch.jscene3d.examples"
readonly PHYSICS_MODULE="jscene3d-physics-examples"
readonly PHYSICS_PACKAGE="io.github.glynch.jscene3d.physics.examples"
readonly GAME_MODULE="jscene3d-game-examples"
readonly GAME_PACKAGE="io.github.glynch.jscene3d.game.examples"

example_directory() {
    local module=$1
    local package_path=${2//./\/}
    printf '%s/%s/src/main/java/%s' "${PROJECT_DIRECTORY}" "${module}" "${package_path}"
}

print_module_examples() {
    local module=$1
    local package_name=$2
    local source_directory
    source_directory=$(example_directory "${module}" "${package_name}")
    find "${source_directory}" -maxdepth 1 -type f -name '*.java' -exec grep -l 'public static void main' {} \; \
        | while IFS= read -r source_file; do
            basename "${source_file}" .java
        done
}

print_examples() {
    {
        print_module_examples "${RENDERING_MODULE}" "${RENDERING_PACKAGE}"
        print_module_examples "${PHYSICS_MODULE}" "${PHYSICS_PACKAGE}"
        print_module_examples "${GAME_MODULE}" "${GAME_PACKAGE}"
    } | sort
}

print_usage() {
    printf 'Usage: %s <ExampleName>\n' "$0"
    printf '       %s --list\n' "$0"
}

resolve_example() {
    local example_name=$1
    local rendering_source
    local physics_source
    local game_source
    rendering_source="$(example_directory "${RENDERING_MODULE}" "${RENDERING_PACKAGE}")/${example_name}.java"
    physics_source="$(example_directory "${PHYSICS_MODULE}" "${PHYSICS_PACKAGE}")/${example_name}.java"
    game_source="$(example_directory "${GAME_MODULE}" "${GAME_PACKAGE}")/${example_name}.java"
    if [[ -f ${rendering_source} ]]; then
        printf '%s\t%s' "${RENDERING_MODULE}" "${RENDERING_PACKAGE}"
        return
    fi
    if [[ -f ${physics_source} ]]; then
        printf '%s\t%s' "${PHYSICS_MODULE}" "${PHYSICS_PACKAGE}"
        return
    fi
    if [[ -f ${game_source} ]]; then
        printf '%s\t%s' "${GAME_MODULE}" "${GAME_PACKAGE}"
        return
    fi
    return 1
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

readonly EXAMPLE_NAME=$1
if ! EXAMPLE_LOCATION=$(resolve_example "${EXAMPLE_NAME}"); then
    printf 'Unknown example: %s\n\nAvailable examples:\n' "${EXAMPLE_NAME}" >&2
    print_examples >&2
    exit 2
fi
readonly EXAMPLE_LOCATION
IFS=$'\t' read -r EXAMPLE_MODULE EXAMPLE_PACKAGE <<< "${EXAMPLE_LOCATION}"
readonly EXAMPLE_MODULE EXAMPLE_PACKAGE

exec "${PROJECT_DIRECTORY}/mvnw" \
    -f "${PROJECT_DIRECTORY}/pom.xml" \
    clean \
    compile \
    -pl "${EXAMPLE_MODULE}" \
    -am \
    -Prun-example \
    "-Djscene3d.exampleMainClass=${EXAMPLE_PACKAGE}.${EXAMPLE_NAME}"
