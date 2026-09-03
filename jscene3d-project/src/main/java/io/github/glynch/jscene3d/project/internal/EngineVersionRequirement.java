/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Conjunction of simple semantic-version comparisons. */
record EngineVersionRequirement(List<Clause> clauses) {
    /** Supported comparison operator. */
    private enum Operator {
        GREATER_OR_EQUAL,
        GREATER,
        LESS_OR_EQUAL,
        LESS,
        EQUAL
    }

    /** One comparison in a requirement expression. */
    private record Clause(Operator operator, SemanticVersion version) {
        /** Evaluates this comparison. */
        private boolean includes(SemanticVersion candidate) {
            int comparison = candidate.compareTo(version);
            return switch (operator) {
                case GREATER_OR_EQUAL -> comparison >= 0;
                case GREATER -> comparison > 0;
                case LESS_OR_EQUAL -> comparison <= 0;
                case LESS -> comparison < 0;
                case EQUAL -> comparison == 0;
            };
        }
    }

    /** Parses space-separated comparisons such as {@code >=0.1.0 <0.2.0}. */
    static Optional<EngineVersionRequirement> parse(String expression) {
        if (expression.isBlank()) {
            return Optional.empty();
        }
        List<Clause> clauses = new ArrayList<>();
        for (String token : expression.trim().split("\\s+")) {
            Optional<Clause> clause = parseClause(token);
            if (clause.isEmpty()) {
                return Optional.empty();
            }
            clauses.add(clause.orElseThrow());
        }
        return Optional.of(new EngineVersionRequirement(List.copyOf(clauses)));
    }

    /** Returns whether every comparison accepts the candidate. */
    boolean includes(SemanticVersion candidate) {
        return clauses.stream().allMatch(clause -> clause.includes(candidate));
    }

    /** Parses one operator and semantic-version pair. */
    private static Optional<Clause> parseClause(String token) {
        Operator operator;
        String versionText;
        if (token.startsWith(">=")) {
            operator = Operator.GREATER_OR_EQUAL;
            versionText = token.substring(2);
        } else if (token.startsWith("<=")) {
            operator = Operator.LESS_OR_EQUAL;
            versionText = token.substring(2);
        } else if (token.startsWith(">")) {
            operator = Operator.GREATER;
            versionText = token.substring(1);
        } else if (token.startsWith("<")) {
            operator = Operator.LESS;
            versionText = token.substring(1);
        } else if (token.startsWith("=")) {
            operator = Operator.EQUAL;
            versionText = token.substring(1);
        } else {
            operator = Operator.EQUAL;
            versionText = token;
        }
        return SemanticVersion.parse(versionText).map(version -> new Clause(operator, version));
    }
}
