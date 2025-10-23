package com.compiler.lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.nfa.NFA;

public class ComplexRegexTest {
    @Test
    public void testComplexRegexRecognition() {
        List<String> lines = Arrays.asList(
            "(a|b)*c+;ABC_COMPLEX",
            "d(e|f)g*;DEFG_COMPLEX"
        );
        List<NFA> nfas = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(";", 2);
            String regex = parts[0].trim();
            String tokenTypeName = parts[1].trim();
            NFA nfa = LexerBuilder.buildNfaFromRegex(regex);
            nfa.endState.setFinal(tokenTypeName);
            nfas.add(nfa);
        }
        NFA combinedNfa = NFA.union(nfas);
        Set<Character> alphabet = new HashSet<>(Arrays.asList('a','b','c','d','e','f','g'));
        DFA dfa = com.compiler.lexer.NfaToDfaConverter.convertNfaToDfa(combinedNfa, alphabet);
        assertNotNull(dfa);

        // Pruebas de reconocimiento
        assertEquals("ABC_COMPLEX", simulateDfa(dfa, "c"));
        assertEquals("ABC_COMPLEX", simulateDfa(dfa, "aac"));
        assertEquals("ABC_COMPLEX", simulateDfa(dfa, "bbbc"));
        assertEquals("ABC_COMPLEX", simulateDfa(dfa, "ababc"));
        assertEquals("ABC_COMPLEX", simulateDfa(dfa, "abaccc"));
        assertNull(simulateDfa(dfa, "d"));
        assertNull(simulateDfa(dfa, "dg"));
        assertEquals("DEFG_COMPLEX", simulateDfa(dfa, "df"));
        assertEquals("DEFG_COMPLEX", simulateDfa(dfa, "de"));
    }

    private String simulateDfa(DFA dfa, String input) {
        com.compiler.lexer.dfa.DfaState state = dfa.startState;
        for (char c : input.toCharArray()) {
            com.compiler.lexer.dfa.DfaState next = state.getTransition(c);
            if (next == null) return null;
            state = next;
        }
        return state.isFinal ? state.tokenTypeName : null;
    }
}
