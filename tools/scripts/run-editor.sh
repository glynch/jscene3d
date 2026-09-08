#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
EDITOR_TARGET_DIRECTORY="${PROJECT_DIRECTORY}/jscene3d-editor/target"
EDITOR_MODULE_PATH="${EDITOR_TARGET_DIRECTORY}/editor-module-path"

"${PROJECT_DIRECTORY}/mvnw" \
    -f "${PROJECT_DIRECTORY}/pom.xml" \
    -pl :jscene3d-editor \
    -am \
    -Pprepare-editor-run \
    package

shopt -s nullglob
EDITOR_JARS=("${EDITOR_TARGET_DIRECTORY}"/jscene3d-editor-*.jar)
shopt -u nullglob

if [[ ${#EDITOR_JARS[@]} -ne 1 ]]; then
    printf 'Expected one editor JAR in %s, found %d.\n' \
        "${EDITOR_TARGET_DIRECTORY}" \
        "${#EDITOR_JARS[@]}" >&2
    exit 1
fi

JAVA_EXECUTABLE="$(command -v java)"

exec "${JAVA_EXECUTABLE}" \
    -Dprism.vsync=false \
    --module-path "${EDITOR_JARS[0]}:${EDITOR_MODULE_PATH}" \
    --add-exports=javafx.graphics/com.sun.prism=openglfx \
    --add-exports=javafx.graphics/com.sun.javafx.scene.layout=openglfx \
    --add-exports=javafx.graphics/com.sun.javafx.scene=openglfx \
    --add-exports=javafx.graphics/com.sun.javafx.sg.prism=openglfx \
    --add-exports=javafx.graphics/com.sun.scenario=openglfx \
    --add-exports=javafx.graphics/com.sun.javafx.tk=openglfx \
    --add-exports=javafx.graphics/com.sun.glass.ui=openglfx \
    --module io.github.glynch.jscene3d.editor/io.github.glynch.jscene3d.editor.EditorLauncher \
    "$@"
