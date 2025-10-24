package com.compiler.parser.lr;

/**
 * Builds the LALR(1) parsing table (ACTION/GOTO).
 * Main task for Practice 9.
 */
public class LALR1Table {
    private final LR1Automaton automaton;

    // merged LALR states and transitions
    private java.util.List<java.util.Set<LR1Item>> lalrStates = new java.util.ArrayList<>();
    private java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lalrTransitions = new java.util.HashMap<>();
    
    // ACTION table: state -> terminal -> Action
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT }
        public final Type type;
        public final Integer state; // for SHIFT
        public final com.compiler.parser.grammar.Production reduceProd; // for REDUCE

        private Action(Type type, Integer state, com.compiler.parser.grammar.Production prod) {
            this.type = type; this.state = state; this.reduceProd = prod;
        }

        public static Action shift(int s) { return new Action(Type.SHIFT, s, null); }
        public static Action reduce(com.compiler.parser.grammar.Production p) { return new Action(Type.REDUCE, null, p); }
        public static Action accept() { return new Action(Type.ACCEPT, null, null); }
    }

    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> action = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> gotoTable = new java.util.HashMap<>();
    private final java.util.List<String> conflicts = new java.util.ArrayList<>();
    private int initialState = 0;

    public LALR1Table(LR1Automaton automaton) {
        this.automaton = automaton;
    }

    /**
     * Builds the LALR(1) parsing table.
     */
    public void build() {
        // Build LALR(1) states by merging LR(1) states that share the same kernel
        automaton.build();

        java.util.List<java.util.Set<LR1Item>> lr1States = automaton.getStates();
        java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lr1Transitions = automaton.getTransitions();

        // (removed debug-only code that printed LR(1) transitions)

        // 1) Group LR(1) states by kernel (set of KernelEntry)
        java.util.Map<java.util.Set<KernelEntry>, java.util.List<Integer>> kernelToStates = new java.util.HashMap<>();
        for (int i = 0; i < lr1States.size(); i++) {
            java.util.Set<KernelEntry> kernel = new java.util.HashSet<>();
            for (LR1Item it : lr1States.get(i)) {
                kernel.add(new KernelEntry(it.production, it.dotPosition));
            }
            kernelToStates.computeIfAbsent(kernel, k -> new java.util.ArrayList<>()).add(i);
        }

        // 2) For each kernel group, create a merged LALR state and map old LR1 indices
        lalrStates = new java.util.ArrayList<>();
        java.util.Map<Integer, Integer> lr1ToLalr = new java.util.HashMap<>();

        for (java.util.List<Integer> group : kernelToStates.values()) {
            java.util.Set<LR1Item> merged = new java.util.HashSet<>();
            for (int idx : group) {
                merged.addAll(lr1States.get(idx));
            }
            int newIndex = lalrStates.size();
            for (int idx : group) lr1ToLalr.put(idx, newIndex);
            lalrStates.add(merged);
        }

        // 3) Rebuild transitions: merged(s_lr1) -X-> merged(t_lr1)
        lalrTransitions = new java.util.HashMap<>();
        for (java.util.Map.Entry<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> e : lr1Transitions.entrySet()) {
            int s_lr1 = e.getKey();
            Integer s_lalr = lr1ToLalr.get(s_lr1);
            if (s_lalr == null) continue;
            for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> tr : e.getValue().entrySet()) {
                com.compiler.parser.grammar.Symbol X = tr.getKey();
                int t_lr1 = tr.getValue();
                Integer t_lalr = lr1ToLalr.get(t_lr1);
                if (t_lalr == null) continue;
                lalrTransitions.computeIfAbsent(s_lalr, k -> new java.util.HashMap<>()).put(X, t_lalr);
            }
        }

        // 4) initial state is the merged state that contains LR1 state 0
        initialState = lr1ToLalr.getOrDefault(0, 0);

        // 5) Fill ACTION and GOTO tables
        fillActionGoto();

        // tables are built; no debug printing here to keep output clean
    }

    private void fillActionGoto() {
        // 1. Clear tables
        action.clear();
        gotoTable.clear();
        conflicts.clear();

    // helpers
    String augmentedStartName = automaton.getAugmentedLeftName();
    com.compiler.parser.grammar.Symbol eof = new com.compiler.parser.grammar.Symbol("$", com.compiler.parser.grammar.SymbolType.TERMINAL);

        // iterate states
        for (int s = 0; s < lalrStates.size(); s++) {
            action.put(s, new java.util.HashMap<>());
            gotoTable.put(s, new java.util.HashMap<>());

            // populate GOTO entries from transitions
            if (lalrTransitions.containsKey(s)) {
                for (java.util.Map.Entry<com.compiler.parser.grammar.Symbol, Integer> trans : lalrTransitions.get(s).entrySet()) {
                    com.compiler.parser.grammar.Symbol B = trans.getKey();
                    int t = trans.getValue();
                    if (B.type == com.compiler.parser.grammar.SymbolType.NON_TERMINAL) {
                        gotoTable.get(s).put(B, t);
                    }
                }
            }

            // for each LR1 item in this state
            for (LR1Item it : lalrStates.get(s)) {
                com.compiler.parser.grammar.Symbol X = it.getSymbolAfterDot();
                // SHIFT on terminal
                if (X != null && X.type == com.compiler.parser.grammar.SymbolType.TERMINAL) {
                    Integer t = lalrTransitions.getOrDefault(s, java.util.Collections.emptyMap()).get(X);
                    if (t == null) continue;
                    Action newAction = Action.shift(t);
                    if (action.get(s).containsKey(X)) {
                        Action existing = action.get(s).get(X);
                        if (existing.type == Action.Type.REDUCE) {
                            conflicts.add(String.format("Shift/Reduce conflict in state %d on %s: SHIFT %d vs REDUCE %s", s, X.name, t, existing.reduceProd));
                        }
                    } else {
                        action.get(s).put(X, newAction);
                    }
                }

                // REDUCE or ACCEPT when dot at end
                else if (X == null) {
                    Action newAction;
                    if (it.production.left.name.equals(augmentedStartName)) {
                        if (it.lookahead.equals(eof)) {
                            newAction = Action.accept();
                        } else {
                            continue;
                        }
                    } else {
                        newAction = Action.reduce(it.production);
                    }

                    com.compiler.parser.grammar.Symbol a = it.lookahead;
                    if (action.get(s).containsKey(a)) {
                        Action existing = action.get(s).get(a);
                        if (existing.type == Action.Type.SHIFT) {
                            conflicts.add(String.format("Shift/Reduce conflict in state %d on %s: REDUCE %s vs SHIFT %d", s, a.name, it.production, existing.state));
                        } else if (existing.type == Action.Type.REDUCE) {
                            if (!existing.reduceProd.equals(it.production)) {
                                conflicts.add(String.format("Reduce/Reduce conflict in state %d on %s: REDUCE %s vs REDUCE %s", s, a.name, it.production, existing.reduceProd));
                            }
                        }
                    } else {
                        action.get(s).put(a, newAction);
                    }
                }
            }
        }
    }
    
    // ... (Getters and KernelEntry class can remain as is)
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> getActionTable() { return action; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getGotoTable() { return gotoTable; }
    public java.util.List<String> getConflicts() { return conflicts; }
    private static class KernelEntry {
        public final com.compiler.parser.grammar.Production production;
        public final int dotPosition;
        KernelEntry(com.compiler.parser.grammar.Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof KernelEntry)) return false;
            KernelEntry o = (KernelEntry) obj;
            return dotPosition == o.dotPosition && production.equals(o.production);
        }
        @Override
        public int hashCode() {
            int r = production.hashCode();
            r = 31 * r + dotPosition;
            return r;
        }
    }
    public java.util.List<java.util.Set<LR1Item>> getLALRStates() { return lalrStates; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getLALRTransitions() { return lalrTransitions; }
    public int getInitialState() { return initialState; }
}
