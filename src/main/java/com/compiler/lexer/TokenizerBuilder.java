package com.compiler.lexer;

import java.util.*;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.regex.RegexParser;

/**
 * Builder class for creating tokenizers with multiple rules.
 */
public class TokenizerBuilder {
    private final RegexParser regexParser;
    
    public TokenizerBuilder() {
        this.regexParser = new RegexParser();
    }
    
    /**
     * Creates a tokenizer from regex patterns and token types.
     */
    public Tokenizer buildTokenizer(Map<String, String> tokenRules, Set<Character> alphabet) {
        Tokenizer tokenizer = new Tokenizer();
        int priority = 1000; // Start with high priority
        
        for (Map.Entry<String, String> entry : tokenRules.entrySet()) {
            String tokenType = entry.getKey();
            String regex = entry.getValue();
            
            try {
                // Parse regex to NFA
                NFA nfa = regexParser.parse(regex);
                
                // Convert NFA to DFA (using static method)
                DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
                
                // Minimize DFA (using static method)
                DFA minimizedDfa = DfaMinimizer.minimizeDfa(dfa, alphabet);
                
                // Add rule to tokenizer
                tokenizer.addRule(minimizedDfa, tokenType, priority--);
                
            } catch (Exception e) {
                throw new RuntimeException("Error processing token rule for " + tokenType + 
                                         " with regex: " + regex, e);
            }
        }
        
        return tokenizer;
    }
}