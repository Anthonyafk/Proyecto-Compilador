package com.compiler.lexer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;


public class LexerBuilderTest {
    @Test
    public void testBuildNfasFromFileAndDfaRecognition() throws Exception {
        // Prepara archivo de tokens temporal
        String tokensFile = "tokens_test.txt";
        List<String> lines = Arrays.asList(
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_)(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_|0|1|2|3|4|5|6|7|8|9)*;IDENTIFIER",
            "(0|1|2|3|4|5|6|7|8|9)+;NUMBER",
            "if|else|while;KEYWORD"
        );
        Files.write(Paths.get(tokensFile), lines);

    // Build a portable lexer definition (transition table)
    Set<Character> alphabet = new HashSet<>();
    for (char c = '0'; c <= '9'; c++) alphabet.add(c);
    for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
    for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
    alphabet.add('_');

    com.compiler.lexer.LexerDefinition def = LexerBuilder.buildLexerDefinitionFromFile(tokensFile, alphabet);
    assertNotNull(def);

    // Simula reconocimiento simple usando la tabla: busca el tipo de token para "if" y "123"
    assertEquals("KEYWORD", simulateTable(def, "if"));
    assertEquals("NUMBER", simulateTable(def, "123"));
    assertEquals("IDENTIFIER", simulateTable(def, "variable"));

        // Limpia archivo temporal
        Files.deleteIfExists(Paths.get(tokensFile));
    }

    // Simula el DFA sobre la entrada y retorna el tokenTypeName del estado final alcanzado

    private String simulateTable(com.compiler.lexer.LexerDefinition def, String input) {
        int state = def.startState;
        for (char c : input.toCharArray()) {
            int a = def.alphabetIndex(c);
            if (a == -1) return null;
            state = def.transitions[state][a];
            if (state == -1) return null;
        }
        return def.isFinal[state] ? def.tokenTypeNames[state] : null;
    }
}
