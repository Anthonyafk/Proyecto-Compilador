package com.compiler.lexer;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import java.util.*;

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
		public NfaToDfaConverter() {
			// TODO: Implement constructor if needed
		}

	/**
	 * Converts an NFA to a DFA using the subset construction algorithm.
	 * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
	 *
	 * @param nfa The input NFA
	 * @param alphabet The input alphabet (set of characters)
	 * @return The resulting DFA
	 */
	public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
	List<DfaState> estadosDFA = new ArrayList<>();
        Queue<DfaState> cola = new LinkedList<>();

        // Calcular cierre epsilon del estado inicial
        Set<State> inicioCerradura = epsilonClosure(Set.of(nfa.startState));
        DfaState inicioDFA = new DfaState(inicioCerradura);
        // Marcar como final si corresponde
        for (State nfaState : inicioCerradura) {
            if (nfaState.isFinal) {
                inicioDFA.setFinal(true);
                break;
            }
        }
        estadosDFA.add(inicioDFA);
        cola.add(inicioDFA);

        // Algoritmo de construcción por subconjuntos
        while (!cola.isEmpty()) {
            DfaState current = cola.poll();
            for (char symbol : alphabet) {
                Set<State> moverResultado = move(current.nfaStates, symbol);
                Set<State> cerradura = epsilonClosure(moverResultado);
            
                if (!cerradura.isEmpty()) {
                    DfaState existing = findDfaState(estadosDFA, cerradura);
                    if (existing == null) {
                        existing = new DfaState(cerradura);
                        // Marcar como final si corresponde
                        for (State nfaState : cerradura) {
                            if (nfaState.isFinal) {
                                existing.setFinal(true);
                                break;
                            }
                        }
                        estadosDFA.add(existing);
                        cola.add(existing);
                    }
                    current.addTransition(symbol, existing);
                }
            }
        }
        return new DFA(inicioDFA, estadosDFA);
    }

	/**
	 * Computes the epsilon-closure of a set of NFA states.
	 * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
	 *
	 * @param states The set of NFA states.
	 * @return The epsilon-closure of the input states.
	 */
	private static Set<State> epsilonClosure(Set<State> states) {
	    Set<State> cerradura = new HashSet<>();
        Stack<State> stack = new Stack<>();
        stack.addAll(states);

        while (!stack.isEmpty()) {
            State s = stack.pop();
            if (cerradura.add(s)) { // solo si no estaba antes
                for (State target : s.getEpsilonTransitions()) {
                    stack.push(target);
                }
            }
        }
        return cerradura;
    }

	/**
	 * Returns the set of states reachable from a set of NFA states by a given symbol.
	 *
	 * @param states The set of NFA states.
	 * @param symbol The input symbol.
	 * @return The set of reachable states.
	 */
	private static Set<State> move(Set<State> states, char symbol) {
	        Set<State> resultado = new HashSet<>();
        for (State s : states) {
            resultado.addAll(s.getTransitions(symbol));
        }
        return resultado;
    }

	/**
	 * Finds an existing DFA state representing a given set of NFA states.
	 *
	 * @param dfaStates The list of DFA states.
	 * @param targetNfaStates The set of NFA states to search for.
	 * @return The matching DFA state, or null if not found.
	 */
	private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
	        for (DfaState dfa : dfaStates) {
            if (dfa.nfaStates.equals(targetNfaStates)) {
 			   return dfa;
			}
        }
        return null;
    }
}
