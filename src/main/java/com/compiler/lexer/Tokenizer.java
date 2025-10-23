package com.compiler.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer driven by a portable LexerDefinition (transition table).
 * Implements longest-match (maximal munch) semantics: at each input position
 * it advances as far as possible and selects the last accepting state reached.
 */
public class Tokenizer {
    private final LexerDefinition def;

    public Tokenizer(LexerDefinition def) {
        this.def = def;
    }

    /**
     * Tokenize the entire input and return a list of Tokens.
     * Unknown characters (not in the alphabet or with no valid transition)
     * are emitted as tokens with type "UNKNOWN" and length 1.
     */
    public List<Token> tokenize(String input) {
        List<Token> out = new ArrayList<>();
        int n = input.length();
        int pos = 0;
        while (pos < n) {
            int state = def.startState;
            int lastAcceptState = -1;
            int lastAcceptPos = -1;
            int j = pos;
            while (j < n) {
                char c = input.charAt(j);
                int a = def.alphabetIndex(c);
                if (a == -1) break; // char not in alphabet
                state = def.transitions[state][a];
                if (state == -1) break; // no transition
                if (def.isFinal[state]) {
                    lastAcceptState = state;
                    lastAcceptPos = j + 1;
                }
                j++;
            }

            if (lastAcceptState != -1) {
                String type = def.tokenTypeNames[lastAcceptState];
                String lexeme = input.substring(pos, lastAcceptPos);
                out.add(new Token(type, lexeme, pos, lastAcceptPos));
                pos = lastAcceptPos;
            } else {
                // Emit single-character UNKNOWN token and advance by one
                String lexeme = input.substring(pos, pos + 1);
                out.add(new Token("UNKNOWN", lexeme, pos, pos + 1));
                pos += 1;
            }
        }
        return out;
    }
}
