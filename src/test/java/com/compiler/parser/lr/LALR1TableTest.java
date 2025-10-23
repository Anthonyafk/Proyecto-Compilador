package com.compiler.parser.lr;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.compiler.parser.grammar.Grammar;

/**
 * Basic test to ensure LALR(1) table can be built without conflicts for a simple grammar.
 */
public class LALR1TableTest {
    @Test
    public void testSimpleGrammarNoConflicts() {
        // Grammar: S -> a
        String g = "S -> a";
        Grammar grammar = new Grammar(g);

        LR1Automaton automaton = new LR1Automaton(grammar);
        LALR1Table table = new LALR1Table(automaton);

        table.build();

    assertTrue(!table.getLALRStates().isEmpty(), "LALR states should be produced");
    assertTrue(table.getConflicts().isEmpty(), "No conflicts expected for this trivial grammar");
    }
}
