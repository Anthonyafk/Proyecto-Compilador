package com.compiler.lexer;

import java.util.HashSet;
import java.util.Set;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;

/**
 * NfaSimulator
 * ------------
 * This class provides functionality to simulate a Non-deterministic Finite Automaton (NFA)
 * on a given input string. It determines whether the input string is accepted by the NFA by processing
 * each character and tracking the set of possible states, including those reachable via epsilon (ε) transitions.
 *
 * Simulation steps:
 * - Initialize the set of current states with the ε-closure of the NFA's start state.
 * - For each character in the input, compute the next set of states by following transitions labeled with that character,
 *   and include all states reachable via ε-transitions from those states.
 * - After processing the input, check if any of the current states is a final (accepting) state.
 *
 * The class also provides a helper method to compute the ε-closure of a given state, which is the set of all states
 * reachable from the given state using only ε-transitions.
 */
/**
 * Simulator for running input strings on an NFA.
 */
public class NfaSimulator {
    /**
     * Default constructor for NfaSimulator.
     */
    public NfaSimulator() {}

    /**
     * Simulates the NFA on the given input string.
     * Starts at the NFA's start state and processes each character, following transitions and epsilon closures.
     * If any final state is reached after processing the input, the string is accepted.
     *
     * @param nfa The NFA to simulate.
     * @param input The input string to test.
     * @return True if the input is accepted by the NFA, false otherwise.
     */
    public boolean simulate(NFA nfa, String input) {
        // The set of states we are currently in
        Set<State> currentStates = new HashSet<>();
        // Initialize with the epsilon-closure of the NFA's start state
        addEpsilonClosure(nfa.startState, currentStates);

        // Process each character of the input string
        for (char c : input.toCharArray()) {
            Set<State> nextStates = new HashSet<>();
            // For each current state, compute the next states
            for (State state : currentStates) {
                for (Transition t : state.transitions) {
                    if (t.symbol != null && t.symbol == c) {
                        // Add the epsilon-closure of the destination state
                        addEpsilonClosure(t.toState, nextStates);
                    }
                }
            }
            currentStates = nextStates;
        }

        // After processing the entire string, check if any of the current states is a final state of the NFA
        for (State state : currentStates) {
            if (state.isFinal) {
                return true; // The string is accepted!
            }
        }

        return false; // The string is rejected
    }

    /**
     * Computes the epsilon-closure: all states reachable from 'start' using only epsilon (null) transitions.
     *
     * @param start The starting state.
     * @param closureSet The set to accumulate reachable states.
     */
    private void addEpsilonClosure(State start, Set<State> closureSet) {
        if (closureSet.contains(start)) {
            return;
        }
        closureSet.add(start);
        for (Transition t : start.transitions) {
            if (t.symbol == null) { // Epsilon transition
                addEpsilonClosure(t.toState, closureSet);
            }
        }
    }
}