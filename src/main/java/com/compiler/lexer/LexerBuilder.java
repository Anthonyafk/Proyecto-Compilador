package com.compiler.lexer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;

public class LexerBuilder {
    /**
     * Builds an NFA from a regular expression using RegexParser.
     * @param regex regular expression in infix notation
     * @return constructed NFA
     */
    public static NFA buildNfaFromRegex(String regex) {
        com.compiler.lexer.regex.RegexParser parser = new com.compiler.lexer.regex.RegexParser();
        return parser.parse(regex);
    }

    /**
     * Reads a token definition file and builds a list of NFAs.
     * Each non-empty, non-comment line must have the format: regex;TokenType
     * Lines that are empty or that start with '#' are ignored.
     * @param filePath path to the token definitions file
     * @return list of constructed NFAs
     * @throws Exception if an IO or build error occurs while reading the file
     */
    public static List<NFA> buildNfasFromFile(String filePath) throws Exception {
        List<NFA> nfas = new ArrayList<>();
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        List<String> tokenLines = new ArrayList<>();
        for (String line : allLines) {
            if (line == null) continue;
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            tokenLines.add(t);
        }

        // Assign priorities so that later lines have higher precedence (lower numeric priority)
        int total = tokenLines.size();
        for (int idx = 0; idx < total; idx++) {
            String line = tokenLines.get(idx);
            String[] parts = line.split(";", 2);
            if (parts.length != 2) continue;
            String regex = parts[0].trim();
            String tokenTypeName = parts[1].trim();
            NFA nfa = buildNfaFromRegex(regex);
            int priority = total - idx - 1; // last line gets 0
            nfa.endState.setFinal(tokenTypeName, priority);
            nfas.add(nfa);
        }
        return nfas;
    }

    /**
     * Builds a portable DFA transition table (LexerDefinition) from a token definition file.
     * The provided alphabet is used to drive DFA construction and to order the transition table columns.
     * @param filePath path to token definitions (same format as buildNfasFromFile)
     * @param alphabet set of characters that form the input alphabet
     * @return LexerDefinition containing the alphabet, transitions, start state and accepting mapping
     * @throws Exception on IO or build errors
     */
    public static LexerDefinition buildLexerDefinitionFromFile(String filePath, Set<Character> alphabet) throws Exception {
        List<NFA> nfas = buildNfasFromFile(filePath);
        NFA combined = NFA.union(nfas);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(combined, alphabet);
        // Convert DFA to table representation
        List<DfaState> states = dfa.allStates;
        int stateCount = states.size();
        List<Character> alphabetList = new ArrayList<>(alphabet);
        int alphaSize = alphabetList.size();

        int[][] transitions = new int[stateCount][alphaSize];
        boolean[] isFinal = new boolean[stateCount];
        String[] tokenTypeNames = new String[stateCount];

        // Map states to indices
        Map<DfaState, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < stateCount; i++) indexMap.put(states.get(i), i);

        for (int i = 0; i < stateCount; i++) {
            DfaState st = states.get(i);
            isFinal[i] = st.isFinal;
            tokenTypeNames[i] = st.tokenTypeName;
            for (int a = 0; a < alphaSize; a++) {
                char c = alphabetList.get(a);
                DfaState target = st.getTransition(c);
                Integer targetIndex = (target == null) ? null : indexMap.get(target);
                transitions[i][a] = (targetIndex == null) ? -1 : targetIndex;
            }
        }

        int startIndex = indexMap.get(dfa.startState);
        return new LexerDefinition(alphabetList, startIndex, transitions, isFinal, tokenTypeNames);
    }
}