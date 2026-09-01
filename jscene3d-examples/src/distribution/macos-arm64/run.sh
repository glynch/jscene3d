#!/usr/bin/env bash

# Copyright 2026 Graham Lynch
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

readonly EXPECTED_SYSTEM="Darwin"
readonly EXPECTED_ARCHITECTURE="arm64"
readonly MINIMUM_JAVA_VERSION=21

if [[ $(uname -s) != "${EXPECTED_SYSTEM}" || $(uname -m) != "${EXPECTED_ARCHITECTURE}" ]]; then
    printf 'This JScene3D distribution requires macOS ARM64. Detected %s %s.\n' \
        "$(uname -s)" "$(uname -m)" >&2
    exit 1
fi

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIRECTORY
readonly BUNDLED_JAVA="${SCRIPT_DIRECTORY}/runtime/bin/java"

java_major_version() {
    local java_command="$1"
    local version_line
    local version
    local major_version

    version_line="$("${java_command}" -version 2>&1 | head -n 1)" || return 1
    version="${version_line#*\"}"
    version="${version%%\"*}"

    if [[ "${version}" == 1.* ]]; then
        major_version="${version#1.}"
        major_version="${major_version%%.*}"
    else
        major_version="${version%%.*}"
        major_version="${major_version%%-*}"
        major_version="${major_version%%+*}"
    fi

    [[ "${major_version}" =~ ^[0-9]+$ ]] || return 1
    printf '%s\n' "${major_version}"
}

supports_java_21() {
    local java_command="$1"
    local major_version

    major_version="$(java_major_version "${java_command}")" || return 1
    ((major_version >= MINIMUM_JAVA_VERSION))
}

if [[ -x "${BUNDLED_JAVA}" ]] && supports_java_21 "${BUNDLED_JAVA}"; then
    readonly JAVA_COMMAND="${BUNDLED_JAVA}"
elif command -v java >/dev/null 2>&1 && supports_java_21 "$(command -v java)"; then
    readonly JAVA_COMMAND="$(command -v java)"
    printf 'Warning: The bundled Java runtime is unavailable. Using system Java %s.\n' \
        "$(java_major_version "${JAVA_COMMAND}")" >&2
else
    printf 'JScene3D requires Java 21 or newer, but no suitable Java runtime was found.\n' >&2
    printf 'Install Temurin 21 for macOS ARM64 from:\n' >&2
    printf 'https://adoptium.net/temurin/releases/?version=21&os=mac&arch=aarch64\n' >&2
    exit 1
fi

exec "${JAVA_COMMAND}" -XstartOnFirstThread -jar "${SCRIPT_DIRECTORY}/jscene3d-examples.jar" "$@"
