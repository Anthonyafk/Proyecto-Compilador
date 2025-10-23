package com.compiler.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaToDfaConverter
 * -----------------
 * This class provides a static method to convert a Non-deterministic Finite Automaton (NFA)
 * into a Deterministic Finite Automaton (DFA) using the standard subset construction algorithm.
 */
/**
 * Utility class for converting NFAs to DFAs using the subset construction algorithm.
 */
public class NfaToDfaConverter {
	/**
	 * Default constructor for NfaToDfaConverter.
	 */
	public NfaToDfaConverter() {}

	/**
	 * Converts an NFA to a DFA using the subset construction algorithm.
	 * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
	 *
	 * @param nfa The input NFA
	 * @param alphabet The input alphabet (set of characters)
	 * @return The resulting DFA
	 */
	public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
		List<DfaState> dfaStates = new ArrayList<>();
		Queue<DfaState> unmarkedStates = new LinkedList<>();

		// 1. Initial DFA state: epsilon-closure of the NFA start state
		Set<State> startNfaStates = epsilonClosure(Collections.singleton(nfa.getStartState()));
		DfaState startDfaState = new DfaState(startNfaStates);
		dfaStates.add(startDfaState);
		unmarkedStates.add(startDfaState);

		// 2. Process states
		while (!unmarkedStates.isEmpty()) {
			DfaState currentDfaState = unmarkedStates.poll();
			for (Character c : alphabet) {
				Set<State> nextNfaStates = move(currentDfaState.nfaStates, c);
				Set<State> targetNfaStates = epsilonClosure(nextNfaStates);
				if (targetNfaStates.isEmpty()) continue;
				DfaState targetDfaState = findDfaState(dfaStates, targetNfaStates);
				if (targetDfaState == null) {
					targetDfaState = new DfaState(targetNfaStates);
					dfaStates.add(targetDfaState);
					unmarkedStates.add(targetDfaState);
				}
				currentDfaState.addTransition(c, targetDfaState);
			}
		}

		// 3. Mark final states in the DFA and assign tokenTypeName
		for (DfaState dfaState : dfaStates) {
			String foundTokenTypeName = null;
			int bestPriority = Integer.MAX_VALUE;
			for (State nfaState : dfaState.nfaStates) {
				if (nfaState.isFinal()) {
					dfaState.isFinal = true;
							if (nfaState.tokenTypeName != null && nfaState.priority <= bestPriority) {
								bestPriority = nfaState.priority;
								foundTokenTypeName = nfaState.tokenTypeName;
							}
				}
			}
			dfaState.tokenTypeName = foundTokenTypeName;
		}

		// 4. Return the constructed DFA
		return new DFA(startDfaState, dfaStates);
	}

	/**
	 * Computes the epsilon-closure of a set of NFA states.
	 * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
	 *
	 * @param states The set of NFA states.
	 * @return The epsilon-closure of the input states.
	 */
	private static Set<State> epsilonClosure(Set<State> states) {
		Set<State> closure = new HashSet<>(states);
		Stack<State> stack = new Stack<>();
		stack.addAll(states);
		while (!stack.isEmpty()) {
			State state = stack.pop();
			for (State next : state.getEpsilonTransitions()) {
				if (closure.add(next)) {
					stack.push(next);
				}
			}
		}
		return closure;
	}

	/**
	 * Returns the set of states reachable from a set of NFA states by a given symbol.
	 *
	 * @param states The set of NFA states.
	 * @param symbol The input symbol.
	 * @return The set of reachable states.
	 */
	private static Set<State> move(Set<State> states, char symbol) {
		Set<State> result = new HashSet<>();
		for (State state : states) {
			for (State next : state.getTransitions(symbol)) {
				result.add(next);
			}
		}
		return result;
	}

	/**
	 * Finds an existing DFA state representing a given set of NFA states.
	 *
	 * @param dfaStates The list of DFA states.
	 * @param targetNfaStates The set of NFA states to search for.
	 * @return The matching DFA state, or null if not found.
	 */
	private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
		for (DfaState dfaState : dfaStates) {
			if (dfaState.nfaStates.equals(targetNfaStates)) {
				return dfaState;
			}
		}
		return null;
	}
}
