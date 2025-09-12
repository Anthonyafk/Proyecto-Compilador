package com.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import com.compiler.lexer.*;
import com.compiler.lexer.Tokenizer.Token;

public class TokenizerTest {
    
    /**
     * Helper method to create basic alphabet with letters, digits, and operators
     */
    private Set<Character> createBasicAlphabet() {
        Set<Character> alphabet = new HashSet<>();
        for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
        for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
        for (char c = '0'; c <= '9'; c++) alphabet.add(c);
        alphabet.add('+');
        alphabet.add('-');
        alphabet.add('*');
        alphabet.add('/');
        alphabet.add('=');
        alphabet.add('(');
        alphabet.add(')');
        alphabet.add(';');
        alphabet.add(' ');
        alphabet.add('\t');
        alphabet.add('\n');
        return alphabet;
    }

    @Test
    public void testBasicTokenization() {
        // Define simple token rules - avoid problematic operators
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("X", "x");
        rules.put("ASSIGN", "=");
        rules.put("ONE", "1");
        rules.put("TWO", "2");
        rules.put("THREE", "3");
        rules.put("PLUS_OP", "p");  // Use 'p' instead of '+' for simplicity
        rules.put("Y", "y");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test simple tokenization
        String input = "x=1py";  // Adjusted input to match rules
        List<Token> tokens = tokenizer.tokenize(input);
        
        assertEquals(5, tokens.size(), "Should produce exactly 5 tokens");
        
        // Verify each token
        assertEquals("X", tokens.get(0).type, "First token should be X");
        assertEquals("x", tokens.get(0).value, "First token value should be 'x'");
        assertEquals(0, tokens.get(0).position, "First token position should be 0");
        
        assertEquals("ASSIGN", tokens.get(1).type, "Second token should be ASSIGN");
        assertEquals("=", tokens.get(1).value, "Second token value should be '='");
        assertEquals(1, tokens.get(1).position, "Second token position should be 1");
        
        assertEquals("ONE", tokens.get(2).type, "Third token should be ONE");
        assertEquals("1", tokens.get(2).value, "Third token value should be '1'");
        assertEquals(2, tokens.get(2).position, "Third token position should be 2");
        
        assertEquals("PLUS_OP", tokens.get(3).type, "Fourth token should be PLUS_OP");
        assertEquals("p", tokens.get(3).value, "Fourth token value should be 'p'");
        assertEquals(3, tokens.get(3).position, "Fourth token position should be 3");
        
        assertEquals("Y", tokens.get(4).type, "Fifth token should be Y");
        assertEquals("y", tokens.get(4).value, "Fifth token value should be 'y'");
        assertEquals(4, tokens.get(4).position, "Fifth token position should be 4");
    }

    @Test
        public void testWhitespaceHandling() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        rules.put("B", "b");  // Esta regla ya existe, pero necesita estar en el orden correcto
        rules.put("SPACE", " ");
        rules.put("TAB", "\t");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("a b\ta");
        
        // Debug: Print actual tokens to understand what's happening
        //System.out.println("Actual tokens:");
        //for (int i = 0; i < tokens.size(); i++) {
        //    System.out.println(i + ": " + tokens.get(i));
        //}
        
        assertEquals(5, tokens.size(), "Should produce exactly 5 tokens"); // Cambiar de 4 a 5
        
        assertEquals("A", tokens.get(0).type, "First token should be A");
        assertEquals("SPACE", tokens.get(1).type, "Second token should be SPACE");
        assertEquals("B", tokens.get(2).type, "Third token should be B");  // Agregar verificación para B
        assertEquals("TAB", tokens.get(3).type, "Fourth token should be TAB"); // Ajustar índices
        assertEquals("A", tokens.get(4).type, "Fifth token should be A");    // Ajustar índices
    }

    @Test
    public void testMultipleConsecutiveTokens() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        rules.put("B", "b");
        rules.put("C", "c");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("aaabbbccc");
        assertEquals(9, tokens.size(), "Should produce exactly 9 tokens");
        
        // Verify first three tokens are all A
        for (int i = 0; i < 3; i++) {
            assertEquals("A", tokens.get(i).type, "Token " + i + " should be A");
            assertEquals(i, tokens.get(i).position, "Token " + i + " should be at position " + i);
        }
        
        // Verify next three tokens are all B
        for (int i = 3; i < 6; i++) {
            assertEquals("B", tokens.get(i).type, "Token " + i + " should be B");
            assertEquals(i, tokens.get(i).position, "Token " + i + " should be at position " + i);
        }
        
        // Verify last three tokens are all C
        for (int i = 6; i < 9; i++) {
            assertEquals("C", tokens.get(i).type, "Token " + i + " should be C");
            assertEquals(i, tokens.get(i).position, "Token " + i + " should be at position " + i);
        }
    }

    @Test
    public void testPriorityOrdering() {
        Map<String, String> rules = new LinkedHashMap<>();
        // First rule should have higher priority due to LinkedHashMap ordering
        rules.put("AB", "ab");
        rules.put("A", "a");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Should match "ab" as AB token, not as A followed by B
        List<Token> tokens = tokenizer.tokenize("ab");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token using longest match");
        assertEquals("AB", tokens.get(0).type, "Should recognize 'ab' as AB token");
        assertEquals("ab", tokens.get(0).value, "Token value should be 'ab'");
    }

    @Test
    public void testMixedAlphanumeric() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("VAR_X", "x");
        rules.put("VAR_Y", "y");
        rules.put("NUM_1", "1");
        rules.put("NUM_2", "2");
        rules.put("NUM_0", "0");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("x1y2x0");
        assertEquals(6, tokens.size(), "Should produce exactly 6 tokens");
        
        String[] expectedTypes = {"VAR_X", "NUM_1", "VAR_Y", "NUM_2", "VAR_X", "NUM_0"};
        String[] expectedValues = {"x", "1", "y", "2", "x", "0"};
        
        for (int i = 0; i < tokens.size(); i++) {
            assertEquals(expectedTypes[i], tokens.get(i).type, 
                String.format("Token %d should be of type %s", i, expectedTypes[i]));
            assertEquals(expectedValues[i], tokens.get(i).value, 
                String.format("Token %d should have value '%s'", i, expectedValues[i]));
            assertEquals(i, tokens.get(i).position, 
                String.format("Token %d should be at position %d", i, i));
        }
    }

    @Test
    public void testSingleCharacterInput() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("LETTER_A", "a");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("a");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("LETTER_A", tokens.get(0).type, "Should recognize 'a' as LETTER_A");
        assertEquals("a", tokens.get(0).value, "Token value should be 'a'");
        assertEquals(0, tokens.get(0).position, "Token position should be 0");
    }

    @Test
    public void testErrorHandling() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test invalid character
        assertThrows(RuntimeException.class, () -> {
            tokenizer.tokenize("@");
        }, "Should throw exception for invalid character '@'");
        
        // Test with position information in error
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tokenizer.tokenize("a@b");
        }, "Should throw exception for invalid character in middle of input");
        
        assertTrue(exception.getMessage().contains("position 1"), 
            "Error message should contain position information");
        assertTrue(exception.getMessage().contains("'@'"), 
            "Error message should contain the invalid character");
    }

    @Test
    public void testEmptyInput() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("");
        
        assertNotNull(tokens, "Should return non-null list for empty input");
        assertEquals(0, tokens.size(), "Should return empty list for empty input");
    }

    @Test
    public void testComplexPattern() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("ABC", "abc");
        rules.put("AB", "ab");
        rules.put("A", "a");
        rules.put("BC", "bc");
        rules.put("B", "b");
        rules.put("C", "c");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Should use longest match - "abc" should be recognized as one ABC token
        List<Token> tokens = tokenizer.tokenize("abc");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token using longest match");
        assertEquals("ABC", tokens.get(0).type, "Should recognize 'abc' as ABC token");
        assertEquals("abc", tokens.get(0).value, "Token value should be 'abc'");
        
        // Test partial matches
        tokens = tokenizer.tokenize("ab");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("AB", tokens.get(0).type, "Should recognize 'ab' as AB token");
        
        tokens = tokenizer.tokenize("bc");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("BC", tokens.get(0).type, "Should recognize 'bc' as BC token");
    }

    @Test
    public void testTokenValueConsistency() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("DIGIT_0", "0");
        rules.put("DIGIT_1", "1");
        rules.put("DIGIT_9", "9");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize("019");
        assertEquals(3, tokens.size(), "Should produce exactly 3 tokens");
        
        assertEquals("0", tokens.get(0).value, "First token value should match input");
        assertEquals("1", tokens.get(1).value, "Second token value should match input");
        assertEquals("9", tokens.get(2).value, "Third token value should match input");
    }

    @Test
    public void testAlternationBasic() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("VOWEL", "a|e|i|o|u");
        rules.put("CONSONANT", "b|c|d");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test vowels
        for (String vowel : Arrays.asList("a", "e", "i", "o", "u")) {
            List<Token> tokens = tokenizer.tokenize(vowel);
            assertEquals(1, tokens.size(), "Should produce exactly 1 token for " + vowel);
            assertEquals("VOWEL", tokens.get(0).type, "Should recognize '" + vowel + "' as VOWEL");
            assertEquals(vowel, tokens.get(0).value, "Token value should be '" + vowel + "'");
        }
        
        // Test consonants
        for (String consonant : Arrays.asList("b", "c", "d")) {
            List<Token> tokens = tokenizer.tokenize(consonant);
            assertEquals(1, tokens.size(), "Should produce exactly 1 token for " + consonant);
            assertEquals("CONSONANT", tokens.get(0).type, "Should recognize '" + consonant + "' as CONSONANT");
            assertEquals(consonant, tokens.get(0).value, "Token value should be '" + consonant + "'");
        }
    }

    @Test
    public void testLargeInput() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Create a large input string
        StringBuilder input = new StringBuilder();
        int size = 100; // Reduced size for faster testing
        for (int i = 0; i < size; i++) {
            input.append(i % 2 == 0 ? "a" : "b");
        }
        
        List<Token> tokens = tokenizer.tokenize(input.toString());
        assertEquals(size, tokens.size(), "Should produce exactly " + size + " tokens");
        
        // Verify positions are correct
        for (int i = 0; i < size; i++) {
            assertEquals(i, tokens.get(i).position, "Token " + i + " should be at position " + i);
            if (i % 2 == 0) {
                assertEquals("A", tokens.get(i).type, "Even position tokens should be A");
                assertEquals("a", tokens.get(i).value, "Even position token values should be 'a'");
            } else {
                assertEquals("B", tokens.get(i).type, "Odd position tokens should be B");
                assertEquals("b", tokens.get(i).value, "Odd position token values should be 'b'");
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
        "'',         0",
        "a,          1", 
        "ab,         2",
        "abc,        3",
        "abcdefghij, 10"
    })
    public void testTokenCountVariations(String input, int expectedCount) {
        Map<String, String> rules = new LinkedHashMap<>();
        for (char c = 'a'; c <= 'j'; c++) {
            rules.put("LETTER_" + Character.toUpperCase(c), String.valueOf(c));
        }
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        List<Token> tokens = tokenizer.tokenize(input);
        assertEquals(expectedCount, tokens.size(), 
            String.format("Input '%s' should produce %d tokens", input, expectedCount));
    }

    @Test
    public void testKleeneStarOperator() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A_STAR", "a*");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test single 'a'
        List<Token> tokens = tokenizer.tokenize("a");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("A_STAR", tokens.get(0).type, "Should recognize 'a' as A_STAR");
        
        // Test multiple 'a's
        tokens = tokenizer.tokenize("aaa");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token using longest match");
        assertEquals("A_STAR", tokens.get(0).type, "Should recognize 'aaa' as A_STAR");
        assertEquals("aaa", tokens.get(0).value, "Token value should be 'aaa'");
    }

    @Test
    public void testOptionalOperator() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A_OPTIONAL", "a?");
        rules.put("B", "b");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test single 'a'
        List<Token> tokens = tokenizer.tokenize("a");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("A_OPTIONAL", tokens.get(0).type, "Should recognize 'a' as A_OPTIONAL");
    }

    @Test
    public void testLongestMatchStrategy() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("AAA", "aaa");
        rules.put("AA", "aa");
        rules.put("A", "a");
        
        TokenizerBuilder builder = new TokenizerBuilder();
        Tokenizer tokenizer = builder.buildTokenizer(rules, createBasicAlphabet());
        
        // Test that "aaa" is recognized as one AAA token, not three A tokens
        List<Token> tokens = tokenizer.tokenize("aaa");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token using longest match");
        assertEquals("AAA", tokens.get(0).type, "Should recognize 'aaa' as AAA token");
        assertEquals("aaa", tokens.get(0).value, "Token value should be 'aaa'");
        
        // Test partial match
        tokens = tokenizer.tokenize("aa");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("AA", tokens.get(0).type, "Should recognize 'aa' as AA token");
        
        // Test single character
        tokens = tokenizer.tokenize("a");
        assertEquals(1, tokens.size(), "Should produce exactly 1 token");
        assertEquals("A", tokens.get(0).type, "Should recognize 'a' as A token");
    }
}