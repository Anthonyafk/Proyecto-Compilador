package com.compiler.parser.syntax;

import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;

/**
 * Calculates the FIRST and FOLLOW sets for a given grammar.
 * Main task of Practice 5.
 */
public class StaticAnalyzer {
    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

    public StaticAnalyzer(Grammar grammar) {
        this.grammar = grammar;
        // initialize maps
        this.firstSets = new java.util.HashMap<>();
        this.followSets = new java.util.HashMap<>();
    }

    /**
     * Calculates and returns the FIRST sets for all symbols.
     * @return A map from Symbol to its FIRST set.
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {
        if (!firstSets.isEmpty()) return firstSets;

        // Initialize FIRST sets: ensure an entry for every symbol referenced in the grammar
        for (Symbol nt : grammar.getNonTerminals()) {
            firstSets.put(nt, new java.util.HashSet<>());
        }
        for (Symbol t : grammar.getTerminals()) {
            java.util.Set<Symbol> set = new java.util.HashSet<>();
            set.add(t);
            firstSets.put(t, set);
        }
        // Also ensure symbols that appear only on RHS (like the explicit epsilon symbol) are present
        for (com.compiler.parser.grammar.Production p : grammar.getProductions()) {
            for (Symbol s : p.getRight()) {
                if (!firstSets.containsKey(s)) {
                    if (s.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                        java.util.Set<Symbol> set = new java.util.HashSet<>();
                        set.add(s);
                        firstSets.put(s, set);
                    } else {
                        firstSets.put(s, new java.util.HashSet<>());
                    }
                }
            }
        }

        // epsilon symbol (named \u03b5 in grammar's symbol map)
        Symbol epsilon = new Symbol("\u03b5", com.compiler.parser.grammar.SymbolType.TERMINAL);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (com.compiler.parser.grammar.Production p : grammar.getProductions()) {
                Symbol A = p.getLeft();
                java.util.Set<Symbol> firstA = firstSets.get(A);
                java.util.List<Symbol> rhs = p.getRight();

                boolean allNullable = true;
                for (Symbol Xi : rhs) {
                    java.util.Set<Symbol> firstXi = firstSets.get(Xi);
                    if (firstXi == null) firstXi = new java.util.HashSet<>();

                    // add FIRST(Xi) - {epsilon} to FIRST(A)
                    for (Symbol s : firstXi) {
                        if (!s.equals(epsilon)) {
                            if (firstA.add(s)) changed = true;
                        }
                    }

                    // if Xi's FIRST does not contain epsilon, stop
                    if (!firstXi.contains(epsilon)) {
                        allNullable = false;
                        break;
                    }
                }

                if (allNullable) {
                    if (firstA.add(epsilon)) changed = true;
                }
            }
        }

        return firstSets;
    }

    /**
     * Calculates and returns the FOLLOW sets for non-terminals.
     * @return A map from Symbol to its FOLLOW set.
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        if (!followSets.isEmpty()) return followSets;

        // Ensure FIRST sets are computed
        Map<Symbol, Set<Symbol>> first = getFirstSets();

        // initialize FOLLOW for non-terminals
        for (Symbol nt : grammar.getNonTerminals()) {
            followSets.put(nt, new java.util.HashSet<>());
        }

        // Add end marker $ to start symbol's FOLLOW set. Represent $ as a special terminal symbol
        Symbol dollar = new Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);
        followSets.get(grammar.getStartSymbol()).add(dollar);

        Symbol epsilon = new Symbol("\u03b5", com.compiler.parser.grammar.SymbolType.TERMINAL);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (com.compiler.parser.grammar.Production p : grammar.getProductions()) {
                Symbol B = p.getLeft();
                java.util.List<Symbol> rhs = p.getRight();

                for (int i = 0; i < rhs.size(); i++) {
                    Symbol Xi = rhs.get(i);
                    if (Xi.type != com.compiler.parser.grammar.SymbolType.NON_TERMINAL) continue;

                    java.util.Set<Symbol> followXi = followSets.get(Xi);
                    // compute FIRST of beta (symbols after Xi)
                    boolean allNullable = true;
                    for (int j = i + 1; j < rhs.size(); j++) {
                        Symbol Xj = rhs.get(j);
                        java.util.Set<Symbol> firstXj = (java.util.Set<Symbol>) first.get(Xj);
                        if (firstXj == null) firstXj = new java.util.HashSet<>();

                        for (Symbol s : firstXj) {
                            if (!s.equals(epsilon)) {
                                if (followXi.add(s)) changed = true;
                            }
                        }

                        if (!firstXj.contains(epsilon)) {
                            allNullable = false;
                            break;
                        }
                    }

                    if (allNullable) {
                        // add FOLLOW(B) to FOLLOW(Xi)
                        java.util.Set<Symbol> followB = followSets.get(B);
                        for (Symbol s : followB) {
                            if (followXi.add(s)) changed = true;
                        }
                    }
                }
            }
        }

        return followSets;
    }

    /**
     * Returns the list of productions from the underlying grammar.
     * This is a convenience accessor used by components that build parsing tables.
     */
    public java.util.List<com.compiler.parser.grammar.Production> getProductions() {
        return grammar.getProductions();
    }
}