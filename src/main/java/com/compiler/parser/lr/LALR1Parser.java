package com.compiler.parser.lr;

import java.util.List;

import com.compiler.lexer.Token;

/**
 * Implements the LALR(1) parsing engine.
 * Uses a stack and the LALR(1) table to process a sequence of tokens.
 * Complementary task for Practice 9.
 */
public class LALR1Parser {
    private final LALR1Table table;

    public LALR1Parser(LALR1Table table) {
        this.table = table;
    }

   // package-private accessor for tests
   LALR1Table getTable() {
       return table;
   }

   /**
    * Parses a sequence of tokens using the LALR(1) parsing algorithm.
    * @param tokens The list of tokens from the lexer.
    * @return true if the sequence is accepted, false if a syntax error is found.
    */
   public boolean parse(List<Token> tokens) {
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        stack.push(table.getInitialState());

        java.util.List<Token> input = new java.util.ArrayList<>(tokens);
        // append EOF token
        input.add(new Token("$", "$"));

        int ip = 0;

        while (true) {
            int state = stack.peek();
            Token aTok = input.get(ip);

            // map token type (string) to a grammar terminal Symbol by name
            com.compiler.parser.grammar.Symbol aSym = new com.compiler.parser.grammar.Symbol(aTok.type, com.compiler.parser.grammar.SymbolType.TERMINAL);

            LALR1Table.Action action = null;
            java.util.Map<com.compiler.parser.grammar.Symbol, LALR1Table.Action> row = table.getActionTable().get(state);
            if (row != null) action = row.get(aSym);

            if (action == null) {
                // Detailed error reporting (optional challenge)
                reportSyntaxError(state, aTok, row);
                return false; // syntax error
            }

            if (action.type == LALR1Table.Action.Type.SHIFT) {
                stack.push(action.state);
                ip++;
                continue;
            } else if (action.type == LALR1Table.Action.Type.REDUCE) {
                com.compiler.parser.grammar.Production prod = action.reduceProd;
                int betaLength = prod.right.size();
                for (int i = 0; i < betaLength; i++) stack.pop();
                int s = stack.peek();
                Integer goto_state = null;
                java.util.Map<com.compiler.parser.grammar.Symbol, Integer> grow = table.getGotoTable().get(s);
                if (grow != null) goto_state = grow.get(prod.left);
                if (goto_state == null) return false;
                stack.push(goto_state);
                continue;
            } else if (action.type == LALR1Table.Action.Type.ACCEPT) {
                return true;
            } else {
                return false;
            }
        }
   }

   /**
    * Reports a detailed syntax error message (optional challenge implementation).
    * Shows what token was found and what tokens were expected at the current state.
    * 
    * @param state The current parser state where the error occurred
    * @param foundToken The token that was encountered
    * @param actionRow The ACTION table row for the current state (may be null)
    */
   private void reportSyntaxError(int state, Token foundToken, 
                                   java.util.Map<com.compiler.parser.grammar.Symbol, LALR1Table.Action> actionRow) {
       StringBuilder expected = new StringBuilder();
       
       // Collect all valid tokens from the ACTION table for this state
       if (actionRow != null && !actionRow.isEmpty()) {
           java.util.List<String> validTokens = new java.util.ArrayList<>();
           for (com.compiler.parser.grammar.Symbol sym : actionRow.keySet()) {
               validTokens.add("'" + sym.name + "'");
           }
           
           if (validTokens.size() == 1) {
               expected.append(validTokens.get(0));
           } else if (validTokens.size() == 2) {
               expected.append(validTokens.get(0)).append(" o ").append(validTokens.get(1));
           } else {
               for (int i = 0; i < validTokens.size(); i++) {
                   if (i == validTokens.size() - 1) {
                       expected.append(" o ").append(validTokens.get(i));
                   } else {
                       expected.append(validTokens.get(i));
                       if (i < validTokens.size() - 2) expected.append(", ");
                   }
               }
           }
       } else {
           expected.append("(ningún token válido)");
       }
       
       System.err.println("Error de sintaxis en estado " + state + ":");
       System.err.println("  Se esperaba: " + expected.toString());
       System.err.println("  Se encontró: '" + foundToken.type + "'");
   }
}
