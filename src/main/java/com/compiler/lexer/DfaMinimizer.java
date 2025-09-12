
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.State;


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
        public DfaMinimizer() {
            // TODO: Implement constructor if needed
        }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
        public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
        List<DfaState> states = new ArrayList<>(originalDfa.allStates);
        int n = states.size();
        
        // 1. Initialize distinguishability table
        Map<Pair, Boolean> table = new HashMap<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                DfaState s1 = states.get(i);
                DfaState s2 = states.get(j);
                table.put(new Pair(s1, s2), s1.isFinal != s2.isFinal);
            }
        }
        
        // 2. Iteratively mark distinguishable pairs
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    DfaState s1 = states.get(i);
                    DfaState s2 = states.get(j);
                    Pair pair = new Pair(s1, s2);
                    if (!table.get(pair)) {
                        for (Character c : alphabet) {
                            DfaState t1 = s1.transitions.get(c);
                            DfaState t2 = s2.transitions.get(c);
                            if ((t1 == null && t2 != null) || (t1 != null && t2 == null)) {
                                table.put(pair, true);
                                changed = true;
                                break;
                            }
                            if (t1 != null && t2 != null) {
                                Pair nextPair = new Pair(t1, t2);
                                Boolean marked = table.get(nextPair);
                                if (marked != null && marked) {
                                    table.put(pair, true);
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 3. Create partitions using existing method
        List<Set<DfaState>> partitions = createPartitions(states, table);
        
        // 4. Build state mapping and new states
        Map<DfaState, DfaState> stateMapping = new HashMap<>();
        Set<DfaState> newStates = new HashSet<>();
        
        for (Set<DfaState> partition : partitions) {
            DfaState representative = partition.iterator().next();
            Set<State> combinedNfaStates = new HashSet<>();
            for (DfaState s : partition) {
                combinedNfaStates.addAll(s.nfaStates);
            }
            DfaState newState = new DfaState(combinedNfaStates);
            newState.isFinal = representative.isFinal;
            newStates.add(newState);
            
            for (DfaState state : partition) {
                stateMapping.put(state, newState);
            }
        }
        
        // 5. Rebuild transitions
        for (Set<DfaState> partition : partitions) {
            DfaState representative = partition.iterator().next();
            DfaState newState = stateMapping.get(representative);
            
            for (Character c : alphabet) {
                DfaState target = representative.transitions.get(c);
                if (target != null) {
                    newState.transitions.put(c, stateMapping.get(target));
                }
            }
        }
        
        DfaState newStart = stateMapping.get(originalDfa.startState);
        return new DFA(newStart, new ArrayList<>(newStates));
    }
    // TODO: Implement minimizeDfa
    /*
     Pseudocode:
     1. Collect and sort all DFA states
     2. Initialize table of state pairs; mark pairs as distinguishable if one is final and the other is not
     3. Iteratively mark pairs as distinguishable if their transitions lead to distinguishable states or only one has a transition
     4. Partition states into equivalence classes (using union-find)
     5. Create new minimized states for each partition
     6. Reconstruct transitions for minimized states
     7. Set start state and return minimized DFA
    */

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
        private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        // 1. Initialize each state as its own parent (Union-Find initialization)
        Map<DfaState, DfaState> parent = new HashMap<>();
        for (DfaState state : allStates) {
            parent.put(state, state);
        }
        
        // 2. For each pair not marked as distinguishable, union the states
        int n = allStates.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair pair = new Pair(s1, s2);
                
                // If the pair is not marked as distinguishable (equivalent states)
                Boolean isDistinguishable = table.get(pair);
                if (isDistinguishable == null || !isDistinguishable) {
                    union(parent, s1, s2);
                }
            }
        }
        
        // 3. Group states by their root parent
        Map<DfaState, Set<DfaState>> partitionMap = new HashMap<>();
        for (DfaState state : allStates) {
            DfaState root = find(parent, state);
            partitionMap.computeIfAbsent(root, k -> new HashSet<>()).add(state);
        }
        
        // 4. Return list of partitions
        return new ArrayList<>(partitionMap.values());
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
    if (parent.get(state) != state) {
            parent.put(state, find(parent, parent.get(state)));
        }
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
        DfaState r1 = find(parent, s1);
        DfaState r2 = find(parent, s2);
        if (r1 != r2) {
            parent.put(r2, r1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;
        public Pair(DfaState s1, DfaState s2) {
            if (s1.id <= s2.id) {
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
            if (!(o instanceof Pair)) return false;
            Pair p = (Pair) o;
            return s1.id == p.s1.id && s2.id == p.s2.id;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(s1.id);
            result = 31 * result + Integer.hashCode(s2.id);
            return result;
        }
    }
}