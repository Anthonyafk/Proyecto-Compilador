package com.compiler.parser.ll;

import java.util.Map;

import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.syntax.StaticAnalyzer;

/**
 * Builds and represents the LL(1) parsing table.
 * Main task of Practice 7.
 */
public class LL1Table {
    // The table is a nested Map: Map<NonTerminal, Map<Terminal, Production>>
    private final Map<Symbol, Map<Symbol, Production>> table;
    private final StaticAnalyzer analyzer;

    public LL1Table(StaticAnalyzer analyzer) {
        this.analyzer = analyzer;
        this.table = new java.util.HashMap<>();
        // initialize an empty inner map for every non-terminal known from productions
        for (com.compiler.parser.grammar.Production p : analyzer.getProductions()) {
            Symbol nt = p.getLeft();
            if (nt.type == com.compiler.parser.grammar.SymbolType.NON_TERMINAL) {
                table.computeIfAbsent(nt, k -> new java.util.HashMap<>());
            }
        }
    }

    /**
     * Fills the parsing table M using FIRST and FOLLOW sets.
     * Should detect and report conflicts if the grammar is not LL(1).
     *
     * Pseudocode for LL(1) table construction:
     *
     * 1. For each production A -> α in the grammar:
     *      a. For each terminal 'a' in FIRST(α):
     *          - If 'a' is not ε:
     *              - If M[A, a] is empty:
     *                  - Set M[A, a] = production (A -> α)
     *              - Else:
     *                  - Report conflict (grammar is not LL(1))
     *      b. If ε is in FIRST(α):
     *          - For each terminal 'b' in FOLLOW(A):
     *              - If M[A, b] is empty:
     *                  - Set M[A, b] = production (A -> α)
     *              - Else:
     *                  - Report conflict (grammar is not LL(1))
     *
     * 2. After filling, the table M can be used for parsing.
     */
    public void build() {
        java.util.Map<Symbol, java.util.Set<Symbol>> first = analyzer.getFirstSets();
        java.util.Map<Symbol, java.util.Set<Symbol>> follow = analyzer.getFollowSets();

        // epsilon symbol used in StaticAnalyzer/Grammar is "ε"
        Symbol epsilon = new Symbol("ε", com.compiler.parser.grammar.SymbolType.TERMINAL);

        for (com.compiler.parser.grammar.Production p : analyzer.getProductions()) {
            Symbol A = p.getLeft();
            java.util.List<Symbol> rhs = p.getRight();

            // Compute FIRST(alpha) for the production's rhs
            java.util.Set<Symbol> firstAlpha = new java.util.HashSet<>();
            boolean allNullable = true;
            for (Symbol X : rhs) {
                java.util.Set<Symbol> firstX = first.get(X);
                if (firstX != null) {
                    for (Symbol s : firstX) {
                        firstAlpha.add(s);
                    }
                    if (!firstX.contains(epsilon)) {
                        allNullable = false;
                        break;
                    }
                } else {
                    // If there is no FIRST set for X, assume it's non-nullable terminal
                    allNullable = false;
                    break;
                }
            }

            // For each terminal a in FIRST(alpha) except epsilon, set M[A,a] = p
            java.util.Map<Symbol, Production> row = table.computeIfAbsent(A, k -> new java.util.HashMap<>());
            for (Symbol a : firstAlpha) {
                if (a.equals(epsilon)) continue;
                Production existing = row.get(a);
                if (existing == null) {
                    row.put(a, p);
                } else if (!existing.equals(p)) {
                    throw new IllegalStateException("Grammar is not LL(1): conflict at M[" + A.name + "," + a.name + "] between productions " + existing + " and " + p);
                }
            }

            // If epsilon is in FIRST(alpha), then for each b in FOLLOW(A) set M[A,b] = p
            if (allNullable || firstAlpha.contains(epsilon)) {
                java.util.Set<Symbol> followA = follow.get(A);
                if (followA != null) {
                    for (Symbol b : followA) {
                        Production existing = row.get(b);
                        if (existing == null) {
                            row.put(b, p);
                        } else if (!existing.equals(p)) {
                            throw new IllegalStateException("Grammar is not LL(1): conflict at M[" + A.name + "," + b.name + "] between productions " + existing + " and " + p);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the production for a non-terminal and an input token.
     * @param nonTerminal The non-terminal at the top of the stack.
     * @param terminal The current input token.
     * @return The production to apply, or null if it is an error.
     */
    public Production getProduction(Symbol nonTerminal, Symbol terminal) {
        java.util.Map<Symbol, Production> row = table.get(nonTerminal);
        if (row == null) return null;
        return row.get(terminal);
    }

    /**
     * Returns the start symbol of the grammar associated with this table.
     */
    public Symbol getStartSymbol() {
        // Use the left-hand side of the first production as the start symbol
        java.util.List<Production> prods = analyzer.getProductions();
        if (prods == null || prods.isEmpty()) return null;
        return prods.get(0).getLeft();
    }
}