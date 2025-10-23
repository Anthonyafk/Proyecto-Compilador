package com.compiler.lexer.regex;

/**
 * Utility class for regular expression parsing using the Shunting Yard algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit concatenation operators,
 * and to convert infix regular expressions to postfix notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard algorithm.
 */
public class ShuntingYard {
    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {}
    /**
     * Inserts the explicit concatenation operator ('·') into the regular expression according to standard rules.
     * This makes implicit concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < regex.length(); i++) {
            char currentChar = regex.charAt(i);
            output.append(currentChar);

            // Do not insert at the end of the string
            if (i + 1 >= regex.length()) {
                break;
            }

            char nextChar = regex.charAt(i + 1);

            boolean isCurrentOperand = isOperand(currentChar) || currentChar == '*' || currentChar == '?' || currentChar == '+' || currentChar == ')';
            boolean isNextOperand = isOperand(nextChar) || nextChar == '(';

            if (isCurrentOperand && isNextOperand) {
                output.append('·');
            }
        }

        return output.toString();
    }

    /**
     * Determines if the given character is an operand (not an operator or parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
        return c != '|' && c != '*' && c != '?' && c != '+' && c != '(' && c != ')' && c != '·';
    }

    /**
     * Converts an infix regular expression to postfix notation using the Shunting Yard algorithm.
     * This is useful for constructing NFAs from regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        java.util.Map<Character, Integer> precedence = new java.util.HashMap<>();
        precedence.put('|', 1);
        precedence.put('·', 2);
        precedence.put('?', 3);
        precedence.put('*', 3);
        precedence.put('+', 3);

        StringBuilder output = new StringBuilder();
        java.util.Stack<Character> operatorStack = new java.util.Stack<>();

        String preprocessedRegex = insertConcatenationOperator(infixRegex);

        for (int i = 0; i < preprocessedRegex.length(); i++) {
            char c = preprocessedRegex.charAt(i);

            if (isOperand(c)) {
                output.append(c);
            } else if (c == '(') {
                operatorStack.push(c);
            } else if (c == ')') {
                while (!operatorStack.isEmpty() && operatorStack.peek() != '(') {
                    output.append(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) {
                    operatorStack.pop(); // Discard '('
                }
            } else { // Is an operator
                while (!operatorStack.isEmpty() && operatorStack.peek() != '('
                        && precedence.getOrDefault(operatorStack.peek(), 0) >= precedence.getOrDefault(c, 0)) {
                    output.append(operatorStack.pop());
                }
                operatorStack.push(c);
            }
        }

        while (!operatorStack.isEmpty()) {
            output.append(operatorStack.pop());
        }

        return output.toString();
    }
}