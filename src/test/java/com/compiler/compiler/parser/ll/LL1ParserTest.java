package com.compiler.parser.ll;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.syntax.StaticAnalyzer;

/**
 * Simple LL(1) parser tests for the grammar:
 * S -> a S | b
 */
public class LL1ParserTest {
    @Test
    public void testSimpleGrammar() {
        String grammarDef = "S -> a S | b";
        Grammar grammar = new Grammar(grammarDef);
        StaticAnalyzer analyzer = new StaticAnalyzer(grammar);
        LL1Table table = new LL1Table(analyzer);
        table.build();

        LL1Parser parser = new LL1Parser(table);

        // Accepting inputs: b, a b, a a b, ...
        assertTrue(parser.parse(tokensFromString("b")));
        assertTrue(parser.parse(tokensFromString("ab")));
        assertTrue(parser.parse(tokensFromString("aaab")));

        // Rejecting inputs
        assertFalse(parser.parse(tokensFromString(""))); // empty
        assertFalse(parser.parse(tokensFromString("a"))); // incomplete
        assertFalse(parser.parse(tokensFromString("ba"))); // extra
    }

    private List<Token> tokensFromString(String s) {
        List<Token> out = new ArrayList<>();
        for (char c : s.toCharArray()) {
            out.add(new Token(String.valueOf(c), String.valueOf(c)));
        }
        return out;
    }
}
