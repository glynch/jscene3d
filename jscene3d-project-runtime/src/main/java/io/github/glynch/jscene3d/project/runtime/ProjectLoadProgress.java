/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Receives truthful milestones from synchronous project composition.
 *
 * <p>Callbacks run on the loading thread and must return promptly. Hosts may use them to repaint a
 * responsive loading surface without moving graphics or audio realization to another thread.
 */
@FunctionalInterface
public interface ProjectLoadProgress {
    /**
     * Receives one completed-or-starting load phase in declaration order.
     *
     * @param phase current project-loading phase
     */
    void report(Phase phase);

    /** Stable coarse-grained project-loading phases in execution order. */
    enum Phase {
        /** The project manifest is being read and validated. */
        MANIFEST("Reading project"),
        /** Declared and built-in extension contracts are being assembled. */
        EXTENSIONS("Discovering extensions"),
        /** Authored and imported project assets are being catalogued. */
        ASSETS("Scanning project assets"),
        /** The optional semantic input map is being loaded. */
        INPUT("Loading input map"),
        /** Runtime resources and definitions are being loaded. */
        CONTENT("Loading project content"),
        /** Host-selected runtime modules are being created. */
        MODULES("Creating runtime modules"),
        /** The selected world is being composed. */
        WORLD("Composing world"),
        /** The application extension is preparing composed state. */
        APPLICATION("Preparing application"),
        /** Project composition has completed successfully. */
        READY("Ready");

        private final String description;

        Phase(String description) {
            this.description = description;
        }

        /**
         * Returns concise human-readable loading feedback.
         *
         * @return phase description
         */
        public String description() {
            return description;
        }

        /**
         * Returns completed progress including this phase as a unit interval.
         *
         * @return progress in the inclusive range {@code [0, 1]}
         */
        public float fraction() {
            return (ordinal() + 1.0F) / values().length;
        }
    }
}
