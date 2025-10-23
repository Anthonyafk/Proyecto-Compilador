package com.compiler.parser.ll;

import java.util.List;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;

/**
 * Implements the LL(1) predictive parsing engine using the parsing table.
 * Complementary task for Practice 7.
 */
public class LL1Parser {
    private final LL1Table table;

    public LL1Parser(LL1Table table) {
        this.table = table;
    }

    /**
     * Validates a sequence of input tokens using the predictive parsing algorithm with a stack.
     * @param tokens The token stream from the lexer.
     * @return true if the string is accepted, false otherwise.
     */
    public boolean parse(List<Token> tokens) {
        // Create a working stack of Symbols. We'll use the grammar Symbol objects.
        java.util.Deque<Symbol> stack = new java.util.ArrayDeque<>();

        // Start symbol from the table
        Symbol start = table.getStartSymbol();
        if (start == null) return false;

        // push end marker '$' and start symbol
        Symbol dollar = new com.compiler.parser.grammar.Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);
        stack.push(dollar);
        stack.push(start);

        // Input pointer: tokens plus an end marker token. We will map Token.type to terminal Symbol.name
        int ip = 0;
        java.util.List<Token> input = new java.util.ArrayList<>(tokens);
        // Append a special end token with type "$"
        input.add(new Token("$", "$"));

        while (!stack.isEmpty()) {
            Symbol X = stack.peek();
            Token a = input.get(ip);

            if (X.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                // match terminals by name: Symbol.name should equal token.type
                if (X.name.equals(a.type)) {
                    // consume
                    stack.pop();
                    ip++;
                } else if (X.name.equals("ε")) {
                    // epsilon: just pop
                    stack.pop();
                } else {
                    return false; // terminal mismatch
                }
            } else {
                // non-terminal: consult table using (X, a)
                Symbol termSym = new Symbol(a.type, com.compiler.parser.grammar.SymbolType.TERMINAL);
                Production p = table.getProduction(X, termSym);
                if (p == null) return false; // error

                // apply production: pop X and push RHS in reverse
                stack.pop();
                java.util.List<Symbol> rhs = p.getRight();
                // if rhs is empty or epsilon, do nothing
                if (rhs != null && !rhs.isEmpty()) {
                    // push in reverse
                    for (int i = rhs.size() - 1; i >= 0; i--) {
                        Symbol s = rhs.get(i);
                        // treat explicit epsilon symbol as no-op
                        if (s.name.equals("ε")) continue;
                        stack.push(s);
                    }
                }
            }
        }

        // accept if input consumed except the appended $ (we advanced past $)
        return ip == input.size();
    }
}