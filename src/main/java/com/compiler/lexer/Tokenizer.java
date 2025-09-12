package com.compiler.lexer;

import java.util.*;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * Tokenizer that uses multiple DFAs to recognize different token types.
 * Implements maximum matching (longest match) strategy.
 */
public class Tokenizer {
    
    /**
     * Represents a token with its type and value.
     */
    public static class Token {
        public final String type;
        public final String value;
        public final int position;
        
        public Token(String type, String value, int position) {
            this.type = type;
            this.value = value;
            this.position = position;
        }
        
        @Override
        public String toString() {
            return String.format("Token{type='%s', value='%s', pos=%d}", type, value, position);
        }
    }
    
    /**
     * Rule associating a DFA with a token type.
     */
    public static class TokenRule {
        public final DFA dfa;
        public final String tokenType;
        public final int priority;
        
        public TokenRule(DFA dfa, String tokenType, int priority) {
            this.dfa = dfa;
            this.tokenType = tokenType;
            this.priority = priority;
        }
    }
    
    private final List<TokenRule> rules;
    
    public Tokenizer() {
        this.rules = new ArrayList<>();
    }
    
    /**
     * Adds a tokenization rule.
     */
    public void addRule(DFA dfa, String tokenType, int priority) {
        rules.add(new TokenRule(dfa, tokenType, priority));
        // Sort by priority (higher priority first)
        rules.sort((r1, r2) -> Integer.compare(r2.priority, r1.priority));
    }
    
    /**
     * Tokenizes the input string using maximum matching strategy.
     */
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        
        while (position < input.length()) {
            TokenMatch bestMatch = findLongestMatch(input, position);
            
            if (bestMatch != null) {
                tokens.add(new Token(bestMatch.tokenType, bestMatch.value, position));
                position += bestMatch.length;
            } else {
                // No match found - handle error or skip character
                throw new RuntimeException("No valid token found at position " + position + 
                                         " for character: '" + input.charAt(position) + "'");
            }
        }
        
        return tokens;
    }
    
    /**
     * Finds the longest possible match at the current position.
     */
    private TokenMatch findLongestMatch(String input, int startPos) {
        TokenMatch bestMatch = null;
        
        for (TokenRule rule : rules) {
            TokenMatch match = tryMatch(rule, input, startPos);
            
            if (match != null) {
                // Prefer longer matches, or higher priority if same length
                if (bestMatch == null || 
                    match.length > bestMatch.length ||
                    (match.length == bestMatch.length && match.priority > bestMatch.priority)) {
                    bestMatch = match;
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Tries to match a specific rule at the given position.
     */
    private TokenMatch tryMatch(TokenRule rule, String input, int startPos) {
        int maxLength = 0;
        int currentPos = startPos;
        
        // Simulate the DFA character by character
        DfaState currentState = rule.dfa.startState;
        
        while (currentPos < input.length() && currentState != null) {
            char c = input.charAt(currentPos);
            currentState = currentState.transitions.get(c);
            
            if (currentState != null) {
                currentPos++;
                if (currentState.isFinal) {
                    maxLength = currentPos - startPos;
                }
            }
        }
        
        if (maxLength > 0) {
            String value = input.substring(startPos, startPos + maxLength);
            return new TokenMatch(rule.tokenType, value, maxLength, rule.priority);
        }
        
        return null;
    }
    
    /**
     * Helper class for token matching results.
     */
    private static class TokenMatch {
        final String tokenType;
        final String value;
        final int length;
        final int priority;
        
        TokenMatch(String tokenType, String value, int length, int priority) {
            this.tokenType = tokenType;
            this.value = value;
            this.length = length;
            this.priority = priority;
        }
    }
}