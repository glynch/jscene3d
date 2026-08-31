#!/usr/bin/env bash

# Copyright 2026 Graham Lynch
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIRECTORY
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
readonly PROJECT_DIRECTORY

exec "${PROJECT_DIRECTORY}/mvnw" \
    -f "${PROJECT_DIRECTORY}/pom.xml" \
    clean compile \
    -pl jscene3d-examples \
    -am \
    -Prun-example \
    -Djscene3d.exampleMainClass=io.github.glynch.jscene3d.examples.tools.ExampleThumbnailCapture
