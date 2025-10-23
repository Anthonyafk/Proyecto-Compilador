package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Grammar;

public class LALR1ParserTest {

    private LALR1Parser buildParser(String grammarDef) {
        Grammar grammar = new Grammar(grammarDef);
        LR1Automaton automaton = new LR1Automaton(grammar);
        LALR1Table table = new LALR1Table(automaton);
        table.build();
        return new LALR1Parser(table);
    }

    private List<Token> tokensFromString(String s) {
        List<Token> tokens = new ArrayList<>();
        for (char c : s.toCharArray()) {
            tokens.add(new Token(String.valueOf(c), String.valueOf(c)));
        }
        return tokens;
    }

    @Test
    public void testSAAGrammar() {
        String g = "S -> A A\nA -> a A | b";
        LALR1Parser parser = buildParser(g);
        LALR1Table table = parser.getTable(); // access table for debugging

        for (var e : table.getActionTable().entrySet()) {
            System.out.println("State " + e.getKey() + ":");
            for (var kv : e.getValue().entrySet()) {
                System.out.println("  on " + kv.getKey().name + " -> " + kv.getValue().type + (kv.getValue().state != null ? " " + kv.getValue().state : ""));
            }
        }

        assertTrue(parser.parse(tokensFromString("abab")), "abab should be accepted");
        assertFalse(parser.parse(tokensFromString("aab")), "aab should be rejected");
        assertTrue(parser.parse(tokensFromString("aaabab")), "aaabab should be accepted");
        assertFalse(parser.parse(tokensFromString("ababab")), "ababab should be rejected");
    }
}
