package com.compiler.parser.syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;
import com.compiler.parser.grammar.Production;

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
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Calculates and returns the FIRST sets for all symbols.
     * @return A map from Symbol to its FIRST set.
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {
        // TODO: Implement the algorithm to calculate FIRST sets.
        /*
         * Pseudocode for FIRST set calculation:
         *
         * 1. For each symbol S in grammar:
         *      - If S is a terminal, FIRST(S) = {S}
         *      - If S is a non-terminal, FIRST(S) = {}
         *
         * 2. Repeat until no changes:
         *      For each production A -> X1 X2 ... Xn:
         *          - For each symbol Xi in the right-hand side:
         *              a. Add FIRST(Xi) - {ε} to FIRST(A)
         *              b. If ε is in FIRST(Xi), continue to next Xi
         *                 Otherwise, break
         *          - If ε is in FIRST(Xi) for all i, add ε to FIRST(A)
         *
         * 3. Return the map of FIRST sets for all symbols.
         */
        Set<Symbol> terminals = this.grammar.getTerminals();
        Set<Symbol> nonTerminals = this.grammar.getNonTerminals();
        
        for(Symbol s : terminals) {
            Set<Symbol> firstS = new HashSet<>();
            firstS.add(s);
            this.firstSets.put(s, firstS);
        }

        for(Symbol s: nonTerminals) {
            this.firstSets.put(s, new HashSet<Symbol>());
        }

        boolean changed = true;
        while(changed) {
            changed = false;
            for(Production production : this.grammar.getProductions()) {
                Symbol left = production.getLeft();
                List<Symbol> right = production.getRight();
                Set<Symbol> original = new HashSet<>(this.firstSets.get(left)); 

                Symbol epsilon = new Symbol("ε", SymbolType.TERMINAL);

                if (right.size() == 1 && right.get(0).equals(epsilon)) {
                    this.firstSets.get(left).add(epsilon);
                } else {
                    boolean allHaveEpsilon = true;
                    
                    for (int i = 0; i < right.size(); i++) {
                        Symbol current = right.get(i);
                        Set<Symbol> firstOfCurrent = this.firstSets.get(current);

                        if (firstOfCurrent == null) {
                            firstOfCurrent = new HashSet<>();
                            this.firstSets.put(current, firstOfCurrent);
                        }
                        
                        // Add FIRST(Xi) - {ε} to FIRST(A)
                        for (Symbol symbol : firstOfCurrent) {
                            if (!symbol.equals(epsilon)) {
                                this.firstSets.get(left).add(symbol);
                            }
                        }
                        
                        if (!firstOfCurrent.contains(epsilon)) {
                            allHaveEpsilon = false;
                            break;
                        }
                    }
                    
                    if (allHaveEpsilon) {
                        this.firstSets.get(left).add(epsilon);
                    }
                }
                if (!original.equals(this.firstSets.get(left))) {
                    changed = true;
                }
            }
        }

        return this.firstSets;
    }

    /**
     * Calculates and returns the FOLLOW sets for non-terminals.
     * @return A map from Symbol to its FOLLOW set.
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        // TODO: Implement the algorithm to calculate FOLLOW sets.
        /*
         * Pseudocode for FOLLOW set calculation:
         *
         * 1. For each non-terminal A, FOLLOW(A) = {}
         * 2. Add $ (end of input) to FOLLOW(S), where S is the start symbol
         *
         * 3. Repeat until no changes:
         *      For each production B -> X1 X2 ... Xn:
         *          For each Xi (where Xi is a non-terminal):
         *              a. For each symbol Xj after Xi (i < j <= n):
         *                  - Add FIRST(Xj) - {ε} to FOLLOW(Xi)
         *                  - If ε is in FIRST(Xj), continue to next Xj
         *                    Otherwise, break
         *              b. If ε is in FIRST(Xj) for all j > i, add FOLLOW(B) to FOLLOW(Xi)
         *
         * 4. Return the map of FOLLOW sets for all non-terminals.
         *
         * Note: This method should call getFirstSets() first to obtain FIRST sets.
         */
        if(this.firstSets.isEmpty()){
            this.getFirstSets();
        }

        Set<Symbol> nonTerminals = this.grammar.getNonTerminals();
        for (Symbol nonTerminal : nonTerminals) {
            this.followSets.put(nonTerminal, new HashSet<>());
        }

        Symbol start = this.grammar.getStartSymbol();               // Non-terminal
        Symbol special = new Symbol("$", SymbolType.TERMINAL);
        this.followSets.get(start).add(special);

        boolean changed = true;
        while(changed) {
            changed = false;
            
            for (Production production : this.grammar.getProductions()) {
                Symbol left = production.getLeft();
                List<Symbol> right = production.getRight();
                for(int i = 0; i < right.size(); i++){
                    Symbol current = right.get(i);
                    
                    if(current.type == SymbolType.NON_TERMINAL) {
                        Set<Symbol> original = new HashSet<>(this.followSets.get(current));

                        boolean allHaveEpsilon = true;
                        Symbol epsilon = new Symbol("ε", SymbolType.TERMINAL);

                        for (int j = i + 1; j < right.size(); j++) {
                            Symbol next = right.get(j);
                            Set<Symbol> firstOfNext = this.firstSets.get(next);
                                
                            if (firstOfNext == null) {
                                firstOfNext = new HashSet<>();
                                this.firstSets.put(next, firstOfNext);
                            }

                            // Add FIRST(next) - {ε} to FOLLOW(current)
                            for (Symbol symbol : firstOfNext) {
                                if (!symbol.equals(epsilon)) {
                                    this.followSets.get(current).add(symbol);
                                }
                            }
                            
                            // If epsilon is not in FIRST(next), stop
                            if (!firstOfNext.contains(epsilon)) {
                                allHaveEpsilon = false;
                                break;
                            }
                        }
                        // If we've processed all symbols after current OR
                        // all symbols after current can derive epsilon,
                        // add FOLLOW(leftSide) to FOLLOW(current)
                        if (i == right.size() - 1 || allHaveEpsilon) {
                            this.followSets.get(current).addAll(this.followSets.get(left));
                        }

                        if (!original.equals(this.followSets.get(current))) {
                            changed = true;
                        }
                    }
                }
            }
        }

        return this.followSets;
    }
}
