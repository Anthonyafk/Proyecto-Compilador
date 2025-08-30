package com.compiler.lexer.regex;

import java.util.Stack;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.Transition;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */
/**
 * Parses regular expressions and constructs NFAs using Thompson's construction.
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
        public RegexParser() {
            // TODO: Implement constructor if needed
        }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
        String posfijo = ShuntingYard.toPostfix(infixRegex);
        return buildNfaFromPostfix(posfijo);
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    
    private NFA buildNfaFromPostfix(String postfixRegex) {
    Stack<NFA> pila = new Stack<>();

        for (char token : postfixRegex.toCharArray()) {
            if (isOperand(token)) {
                pila.push(createNfaForCharacter(token));
            } else {
                switch (token) {
                    case '·' -> handleConcatenation(pila);
                    case '|' -> handleUnion(pila);
                    case '*' -> handleKleeneStar(pila);
                    case '+' -> handlePlus(pila);
                    case '?' -> handleOptional(pila);
                    default -> throw new IllegalArgumentException("Operador desconocido: " + token);
                }
            }
        }

        if (pila.size() != 1) {
            throw new IllegalStateException("Expresión de sufijo no válida");
        }
        return pila.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("El operador opcional requiere un NFA");
        }

        NFA interno = stack.pop();
        State estadoInicial = new State();
        State estadoFinal = new State();

        // Zero occurrences
        estadoInicial.transitions.add(new Transition(null, estadoFinal));
        // One occurrence
        estadoInicial.transitions.add(new Transition(null, interno.startState));
        interno.endState.transitions.add(new Transition(null, estadoFinal));
        interno.endState.isFinal = false;

        stack.push(new NFA(estadoInicial, estadoFinal));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
         if (stack.isEmpty()) {
            throw new IllegalStateException("Plus operator requires one NFA");
        }

        NFA interno = stack.pop();
        State estadoInicial = new State();
        State estadoFinal = new State();

        // First occurrence
        estadoInicial.transitions.add(new Transition(null, interno.startState));
        // Repetition and end
        interno.endState.transitions.add(new Transition(null, interno.startState));
        interno.endState.transitions.add(new Transition(null, estadoFinal));
        interno.endState.isFinal = false;

        stack.push(new NFA(estadoInicial, estadoFinal));
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
        State estadoInicial = new State();
        State estadoFinal = new State();
        estadoFinal.isFinal = true;

        estadoInicial.transitions.add(new Transition(c, estadoFinal));
        return new NFA(estadoInicial, estadoFinal);
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Concatenation requires two NFAs");
        }

        NFA derecho = stack.pop();
        NFA izquierdo = stack.pop();

        izquierdo.endState.transitions.add(new Transition(null, derecho.startState));
        izquierdo.endState.isFinal = false;

        stack.push(new NFA(izquierdo.startState, derecho.endState));
    }

    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Union requires two NFAs");
        }

        NFA derecho = stack.pop();
        NFA izquierdo = stack.pop();

        State estadoInicial = new State();
        State estadoFinal = new State();

        // Empieza merge
        estadoInicial.transitions.add(new Transition(null, izquierdo.startState));
        estadoInicial.transitions.add(new Transition(null, derecho.startState));

        // Merge termina
        izquierdo.endState.transitions.add(new Transition(null, estadoFinal));
        derecho.endState.transitions.add(new Transition(null, estadoFinal));
        izquierdo.endState.isFinal = false;
        derecho.endState.isFinal = false;

        stack.push(new NFA(estadoInicial, estadoFinal));
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Kleene star requires one NFA");
        }

        NFA interno = stack.pop();
        State estadoInicial = new State();
        State estadoFinal = new State();

        // cero repeticiones
        estadoInicial.transitions.add(new Transition(null, estadoFinal));
        // una o más repeticiones
        estadoInicial.transitions.add(new Transition(null, interno.startState));
        interno.endState.transitions.add(new Transition(null, interno.startState));
        interno.endState.transitions.add(new Transition(null, estadoFinal));
        interno.endState.isFinal = false;

        stack.push(new NFA(estadoInicial, estadoFinal));
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
    return !String.valueOf(c).matches("[|*?+()·]");
    }
}
