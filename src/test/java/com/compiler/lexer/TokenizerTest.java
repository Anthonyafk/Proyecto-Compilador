package com.compiler.lexer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TokenizerTest {
    @Test
    public void testTokenizeSimple() throws Exception {
        String tokensFile = "tokens_for_tokenizer.txt";
        List<String> lines = Arrays.asList(
            "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_)(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_|0|1|2|3|4|5|6|7|8|9)*;IDENTIFIER",
            "(0|1|2|3|4|5|6|7|8|9)+;NUMBER",
            "if|else|while;KEYWORD"
        );
        Files.write(Paths.get(tokensFile), lines);

        Set<Character> alphabet = new HashSet<>();
        for (char c = '0'; c <= '9'; c++) alphabet.add(c);
        for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
        for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
        alphabet.add('_');

        LexerDefinition def = LexerBuilder.buildLexerDefinitionFromFile(tokensFile, alphabet);
        Tokenizer tokenizer = new Tokenizer(def);

        List<Token> t1 = tokenizer.tokenize("if 123 variable");
        // The tokenizer doesn't split whitespace because whitespace is not in the alphabet; we expect UNKNOWN tokens for spaces
        // Let's just assert the types sequence ignoring UNKNOWNs for spaces
        String[] types = t1.stream().filter(t -> !t.type.equals("UNKNOWN")).map(t -> t.type).toArray(String[]::new);
        assertEquals(Arrays.asList("KEYWORD", "NUMBER", "IDENTIFIER"), Arrays.asList(types));

        // Longest match: input "iff" should be IDENTIFIER, not KEYWORD+IDENTIFIER
        List<Token> t2 = tokenizer.tokenize("iff");
        assertEquals(1, t2.size());
        assertEquals("IDENTIFIER", t2.get(0).type);

        Files.deleteIfExists(Paths.get(tokensFile));
    }
}
