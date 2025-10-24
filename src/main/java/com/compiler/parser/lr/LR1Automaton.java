package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;

/**
 * Builds the canonical collection of LR(1) items (the DFA automaton).
 * Items contain a lookahead symbol.
 */
public class LR1Automaton {
    private final Grammar grammar;
    private final List<Set<LR1Item>> states = new ArrayList<>();
    private final Map<Integer, Map<Symbol, Integer>> transitions = new HashMap<>();
    private String augmentedLeftName = null;

    public LR1Automaton(Grammar grammar) {
        this.grammar = Objects.requireNonNull(grammar);
    }

    public List<Set<LR1Item>> getStates() { return states; }
    public Map<Integer, Map<Symbol, Integer>> getTransitions() { return transitions; }

    /**
     * CLOSURE for LR(1): standard algorithm using FIRST sets to compute lookaheads for new items.
     */
    private Set<LR1Item> closure(Set<LR1Item> items) {
        Set<LR1Item> closure = new HashSet<>(items);
        Queue<LR1Item> worklist = new LinkedList<>(items);

        // Prepare FIRST sets and epsilon symbol
        Map<Symbol, Set<Symbol>> firstSets = computeFirstSets();
        Symbol eps = null;
        for (Symbol t : grammar.getTerminals()) if ("ε".equals(t.name)) { eps = t; break; }

        while (!worklist.isEmpty()) {
            LR1Item item = worklist.poll();
            Symbol B = item.getSymbolAfterDot();
            if (B != null && grammar.getNonTerminals().contains(B)) {
                // build beta_a sequence
                List<Symbol> beta_a = new ArrayList<>();
                if (item.dotPosition + 1 <= item.production.right.size() - 1) {
                    beta_a.addAll(item.production.right.subList(item.dotPosition + 1, item.production.right.size()));
                }
                beta_a.add(item.lookahead);

                Set<Symbol> firstOfBetaA = computeFirstOfSequence(beta_a, firstSets, eps);

                for (com.compiler.parser.grammar.Production prod : grammar.getProductions()) {
                    if (!prod.left.equals(B)) continue;
                    for (Symbol b : firstOfBetaA) {
                        if (eps != null && b.equals(eps)) continue; // skip epsilon as lookahead
                        LR1Item newItem = new LR1Item(prod, 0, b);
                        if (closure.add(newItem)) {
                            worklist.add(newItem);
                        }
                    }
                }
            }
        }
        return closure;
    }

    /** Compute FIRST sets for all grammar symbols. */
    private Map<Symbol, Set<Symbol>> computeFirstSets() {
        Map<Symbol, Set<Symbol>> first = new HashMap<>();
        // locate epsilon symbol if present
        Symbol eps = null;
        for (Symbol t : grammar.getTerminals()) if ("ε".equals(t.name)) { eps = t; break; }
        // initialize
        for (Symbol t : grammar.getTerminals()) {
            Set<Symbol> s = new HashSet<>();
            s.add(t);
            first.put(t, s);
        }
        for (Symbol nt : grammar.getNonTerminals()) {
            first.putIfAbsent(nt, new HashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (com.compiler.parser.grammar.Production p : grammar.getProductions()) {
                Symbol A = p.left;
                List<Symbol> rhs = p.right;
                Set<Symbol> firstA = first.get(A);
                if (firstA == null) { firstA = new HashSet<>(); first.put(A, firstA); }

                // compute FIRST(rhs)
                boolean allEps = true;
                for (Symbol X : rhs) {
                    Set<Symbol> firstX = first.get(X);
                    if (firstX == null) { allEps = false; break; }
                    for (Symbol s : firstX) {
                        if (eps != null && s.equals(eps)) continue;
                        if (firstA.add(s)) changed = true;
                    }
                    if (!firstX.contains(eps)) { allEps = false; break; }
                }
                if (allEps && eps != null) {
                    if (firstA.add(eps)) changed = true;
                }
            }
        }
        return first;
    }

    /**
     * Compute FIRST of a sequence of symbols.
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> seq, Map<Symbol, Set<Symbol>> firstSets, Symbol epsilon) {
        Set<Symbol> result = new HashSet<>();
        if (seq == null || seq.isEmpty()) {
            if (epsilon != null) result.add(epsilon);
            return result;
        }

        boolean allHaveEps = true;
        for (Symbol X : seq) {
            Set<Symbol> firstX = firstSets.get(X);
            // If the symbol is not present in the precomputed FIRST sets it
            // is most likely an external terminal (e.g. EOF '$') that is not
            // part of grammar.getTerminals(). In that case treat it as a
            // terminal whose FIRST is itself.
            if (firstX == null) {
                if (X.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                    result.add(X);
                }
                allHaveEps = false;
                break;
            }
            for (Symbol s : firstX) {
                if (epsilon == null || !s.equals(epsilon)) result.add(s);
            }
            if (!firstX.contains(epsilon)) { allHaveEps = false; break; }
        }
        if (allHaveEps && epsilon != null) result.add(epsilon);
        return result;
    }

    /**
     * GOTO for LR(1): moves dot over symbol and takes closure.
     */
    private Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {
        Set<LR1Item> moved = new HashSet<>();
        for (LR1Item item : state) {
            Symbol X = item.getSymbolAfterDot();
            if (X != null && X.equals(symbol)) {
                moved.add(new LR1Item(item.production, item.dotPosition + 1, item.lookahead));
            }
        }
        return closure(moved);
    }

    /**
     * Build the LR(1) canonical collection: states and transitions.
     */
    public void build() {
        // Build canonical LR(1) collection
        states.clear();
        transitions.clear();

    // Prepare EOF and epsilon symbols (local only)
    com.compiler.parser.grammar.Symbol eof = new com.compiler.parser.grammar.Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);
    for (Symbol t : grammar.getTerminals()) if ("ε".equals(t.name)) { /* found epsilon if present */ }

        // Augmented production S' -> S
        Symbol start = grammar.getStartSymbol();
        Symbol augLeft = new com.compiler.parser.grammar.Symbol(start.name + "'", com.compiler.parser.grammar.SymbolType.NON_TERMINAL);
        List<Symbol> right = new ArrayList<>();
        right.add(start);
    com.compiler.parser.grammar.Production startProd = new com.compiler.parser.grammar.Production(augLeft, right);
        this.augmentedLeftName = augLeft.name;

        // initial item [S' -> • S, $]
        Set<LR1Item> I0Items = new HashSet<>();
    I0Items.add(new LR1Item(startProd, 0, eof));
        Set<LR1Item> I0 = closure(I0Items);
        states.add(I0);

        Queue<Set<LR1Item>> worklist = new LinkedList<>();
        worklist.add(I0);

        Map<Set<LR1Item>, Integer> stateIndex = new HashMap<>();
        stateIndex.put(I0, 0);

        while (!worklist.isEmpty()) {
            Set<LR1Item> I = worklist.poll();
            int iIdx = stateIndex.get(I);
            // collect symbols from grammar: terminals U non-terminals
            Set<Symbol> symbols = new HashSet<>();
            symbols.addAll(grammar.getTerminals());
            symbols.addAll(grammar.getNonTerminals());

            for (Symbol X : symbols) {
                Set<LR1Item> J = goTo(I, X);
                if (!J.isEmpty()) {
                    int jIdx;
                    if (!stateIndex.containsKey(J)) {
                        states.add(J);
                        jIdx = states.size() - 1;
                        stateIndex.put(J, jIdx);
                        worklist.add(J);
                    } else {
                        jIdx = stateIndex.get(J);
                    }
                    transitions.computeIfAbsent(iIdx, k -> new HashMap<>()).put(X, jIdx);
                }
            }
        }
    }

    public String getAugmentedLeftName() { return augmentedLeftName; }
}
