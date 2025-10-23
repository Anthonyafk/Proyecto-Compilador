
/**
 * DfaMinimizer
 * -------------
 * This class provides an implementation of DFA minimization using the table-filling algorithm.
 * It identifies and merges equivalent states in a deterministic finite automaton (DFA),
 * resulting in a minimized DFA with the smallest number of states that recognizes the same language.
 *
 * Main steps:
 *   1. Initialization: Mark pairs of states as distinguishable if one is final and the other is not.
 *   2. Iterative marking: Mark pairs as distinguishable if their transitions lead to distinguishable states,
 *      or if only one state has a transition for a given symbol.
 *   3. Partitioning: Group equivalent states and build the minimized DFA.
 *
 * Helper methods are provided for partitioning, union-find operations, and pair representation.
 */
package com.compiler.lexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;


/**
 * Implements DFA minimization using the table-filling algorithm.
 */
/**
 * Utility class for minimizing DFAs using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
    public DfaMinimizer() {}

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
    // Step 1: Collect and sort all DFA states for consistent table ordering
    List<DfaState> allStates = new ArrayList<>(originalDfa.allStates);
    allStates.sort(Comparator.comparingInt(s -> s.id));

        // Step 2: Initialize the table of state pairs
        // Mark pairs as distinguishable if one is final and the other is not
        Map<Pair, Boolean> table = new HashMap<>();
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                table.put(pair, s1.isFinal() != s2.isFinal());
            }
        }

    /**
     * --- PHASE 2: Iterative Marking of Distinguishable Pairs ---
     * This loop goes through the table of state pairs and marks as distinguishable those pairs
     * that meet any of the following conditions:
     *   1. Both states have a transition for the same symbol, and their destinations are distinguishable.
     *   2. Only one of the states has a transition for the symbol (the other does not).
     * The process repeats until no more pairs are marked in an iteration.
     */
        boolean changed;
        do {
            changed = false;
            for (Pair pair : table.keySet()) {
                // Only process pairs not marked as distinguishable
                if (!table.get(pair)) {
                    DfaState s1 = pair.s1;
                    DfaState s2 = pair.s2;
                    for (char symbol : alphabet) {
                        DfaState t1 = s1.getTransition(symbol);
                        DfaState t2 = s2.getTransition(symbol);

                        // Determine if the pair should be marked as distinguishable
                        boolean areDistinguishable = false;
                        if (t1 != null && t2 != null) {
                            // Case 1: Both have transition, check if their destinations are distinguishable
                            if (t1.id != t2.id) {
                                Pair targetPair = new Pair(t1, t2);
                                if (table.getOrDefault(targetPair, false)) {
                                    areDistinguishable = true;
                                }
                            }
                        } else if (t1 != t2) {
                            // Case 2: One has transition and the other does not (t1 is null and t2 is not, or vice versa)
                            areDistinguishable = true;
                        }

                        // If the pair is found to be distinguishable, mark it and exit the loop
                        if (areDistinguishable) {
                            table.put(pair, true);
                            changed = true;
                            // Uncomment for debugging:
                            // System.out.printf("Marking pair (%d, %d) by '%c'\n", s1.id, s2.id, symbol);
                            break;
                        }
                    }
                }
            }
        } while (changed);

        // Step 3: Partition states and build the minimized DFA
        List<Set<DfaState>> partitions = createPartitions(allStates, table);

        // Map old states to new minimized states
        Map<DfaState, DfaState> oldToNewStateMap = new HashMap<>();
        List<DfaState> minimizedStatesList = new ArrayList<>();

        // Create new states for each partition
        for (Set<DfaState> partition : partitions) {
            DfaState newState = new DfaState(new HashSet<>());
            // Mark as final if any state in the partition is final
            newState.setFinal(partition.stream().anyMatch(DfaState::isFinal));
            minimizedStatesList.add(newState);
            for (DfaState oldState : partition) {
                oldToNewStateMap.put(oldState, newState);
            }
        }

        // Reconstruct transitions for minimized states
        for (DfaState oldState : allStates) {
            DfaState newState = oldToNewStateMap.get(oldState);
            for (Map.Entry<Character, DfaState> entry : oldState.getTransitions().entrySet()) {
                DfaState newTarget = oldToNewStateMap.get(entry.getValue());
                newState.addTransition(entry.getKey(), newTarget);
            }
        }

        // Set the start state for the minimized DFA
        DfaState minimizedStartState = oldToNewStateMap.get(originalDfa.startState);
        return new DFA(minimizedStartState, minimizedStatesList);
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        Map<DfaState, DfaState> parent = new HashMap<>();
        for (DfaState state : allStates) parent.put(state, state);

        // Merge states that are not distinguishable
        for (Pair pair : table.keySet()) {
            if (!table.get(pair)) {
                union(parent, pair.s1, pair.s2);
            }
        }

        // Group states by their root parent
        Map<DfaState, Set<DfaState>> groups = new HashMap<>();
        for (DfaState state : allStates) {
            DfaState root = find(parent, state);
            groups.computeIfAbsent(root, k -> new HashSet<>()).add(state);
        }
        return new ArrayList<>(groups.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
        if (parent.get(state) == state) return state;
        parent.put(state, find(parent, parent.get(state)));
        return parent.get(state);
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {
        DfaState root1 = find(parent, s1);
        DfaState root2 = find(parent, s2);
        if (root1.id != root2.id) {
            parent.put(root2, root1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param s1 First state.
         * @param s2 Second state.
         */
        public Pair(DfaState s1, DfaState s2) {
            if (s1.id < s2.id) {
                this.s1 = s1;
                this.s2 = s2;
            } else {
                this.s1 = s2;
                this.s2 = s1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return s1.id == pair.s1.id && s2.id == pair.s2.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(s1.id, s2.id);
        }
    }
}
