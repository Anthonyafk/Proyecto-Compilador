package com.compiler.lexer.regex;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
        // TODO: Implement constructor if needed
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {
        if (regex == null || regex.isEmpty()) return regex;

        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            char c1 = regex.charAt(i);
            salida.append(c1);

            if (i + 1 < regex.length()) {
                char c2 = regex.charAt(i + 1);

                boolean leftCanConcat = isOperand(c1) || c1 == ')' || c1 == '*' || c1 == '+' || c1 == '?';
                boolean rightCanConcat = isOperand(c2) || c2 == '(';

                if (leftCanConcat && rightCanConcat) {
                    salida.append('·');
                }
            }
        }
        return salida.toString();
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
            return !String.valueOf(c).matches("[|*?+()·]");
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        if (infixRegex == null) return "";

        String expReg = insertConcatenationOperator(infixRegex);

        // Prioridades
        Map<Character, Integer> prec = new HashMap<>();
        prec.put('|', 1);
        prec.put('·', 2);
        prec.put('*', 3);
        prec.put('+', 3);
        prec.put('?', 3);

        StringBuilder salida = new StringBuilder();
        Stack<Character> pila = new Stack<>();

        for (int i = 0; i < expReg.length(); i++) {
            char c = expReg.charAt(i);

            if (isOperand(c)) {
                salida.append(c);
            } else if (c == '(') {
                pila.push(c);
            } else if (c == ')') {
                while (!pila.isEmpty() && pila.peek() != '(') {
                    salida.append(pila.pop());
                }
                if (!pila.isEmpty() && pila.peek() == '(') {
                    pila.pop(); // descartar '('
                } else {
                    throw new IllegalArgumentException("')' no coincide en la expresión regular");
                }
            } else {
                // c es un operador: |, ·, *, +, ?
                while (!pila.isEmpty()
                        && pila.peek() != '('
                        && prec.getOrDefault(pila.peek(), 0) >= prec.getOrDefault(c, 0)) {
                    salida.append(pila.pop());
                }
                pila.push(c);
            }
        }

        while (!pila.isEmpty()) {
            char top = pila.pop();
            if (top == '(' || top == ')') {
                throw new IllegalArgumentException("'(' no coincidente en la expresión regular");
            }
            salida.append(top);
        }

        return salida.toString();
    }
}
