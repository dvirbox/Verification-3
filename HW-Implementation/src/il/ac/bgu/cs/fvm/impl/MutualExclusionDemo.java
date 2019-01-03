package il.ac.bgu.cs.fvm.impl;

import il.ac.bgu.cs.fvm.FvmFacade;
import il.ac.bgu.cs.fvm.automata.Automaton;
import il.ac.bgu.cs.fvm.examples.PetersonProgramGraphBuilder;
import il.ac.bgu.cs.fvm.programgraph.*;
import il.ac.bgu.cs.fvm.programgraph.ProgramGraph;
import il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem;
import il.ac.bgu.cs.fvm.util.Pair;
import il.ac.bgu.cs.fvm.util.Util;
import il.ac.bgu.cs.fvm.verification.VerificationResult;
import il.ac.bgu.cs.fvm.verification.VerificationSucceeded;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static il.ac.bgu.cs.fvm.util.CollectionHelper.seq;
import static il.ac.bgu.cs.fvm.util.CollectionHelper.set;

public class MutualExclusionDemo {

    private static FvmFacade fvm;

    public static void main(String[] args){
        System.out.println("Initialize the fvm instance.");
        fvm = FvmFacade.createInstance();
        System.out.println("Generate Peterson PG-1.");
        ProgramGraph<String, String> pg1 = PetersonProgramGraphBuilder.build(1);
        System.out.println("Generate Peterson PG-2.");
        ProgramGraph<String, String> pg2 = PetersonProgramGraphBuilder.build(2);
        System.out.println("Interleave PG-1 and PG-2 .");
        ProgramGraph<Pair<String, String>, String> pg = fvm.interleave(pg1, pg2);

        System.out.println("Generate TS from the interleaved PG.");
        Set<ActionDef> ActDef = set(new ParserBasedActDef());
        Set<ConditionDef> CondDef = set(new ParserBasedCondDef());
        TransitionSystem<Pair<Pair<String, String>, Map<String, Object>>, String, String> ts =
                fvm.transitionSystemFromProgramGraph(pg, ActDef, CondDef);
        addLabelsToTrans(ts);

        Set<Set<String>> all = Util.powerSet(ts.getAtomicPropositions());

        System.out.println("Generate automaton for mutex.");
        Automaton<String, String> mutexAutomaton = new AutomatonGenerator(ts).generateMutexAutomaton();
        System.out.println("Verify omega regular property: Mutex");
        VerificationResult<Pair<Pair<String, String>, Map<String, Object>>> mutexResult = fvm.verifyAnOmegaRegularProperty(ts, mutexAutomaton);
        System.out.println("Mutex verification " + (mutexResult instanceof VerificationSucceeded ? "succeeded!" : "failed!\n" + mutexResult.toString()));

        System.out.println("Generate automaton for starvation.");
        Automaton<String, String> starvationAutomaton = new AutomatonGenerator(ts).generateStarvationAutomaton();
        System.out.println("Verify omega regular property: Starvation");
        VerificationResult<Pair<Pair<String, String>, Map<String, Object>>> starvationResult = fvm.verifyAnOmegaRegularProperty(ts, starvationAutomaton);
        System.out.println("Starvation verification " + (starvationResult instanceof VerificationSucceeded ? "succeeded!" : "failed!\n" + starvationResult.toString()));

        System.out.println( starvationResult instanceof VerificationSucceeded &&
                mutexResult instanceof VerificationSucceeded ? "Success!!!" : "Fail!!!");
    }


    private static void addLabelsToTrans(TransitionSystem<Pair<Pair<String, String>, Map<String, Object>>, String, String> ts) {
        cleanAP(ts);
        seq("wait1", "wait2", "crit1", "crit2", "crit1_enabled").forEach(ts::addAtomicPropositions);
        addLabelToStates(ts, "crit1", "wait1");
        addLabelToStates(ts, "crit2", "wait2");
        Predicate<Pair<Pair<String, String>, ?>> isCrit1 = state -> state.getFirst().getFirst().equals("crit1");
        ts.getStates().stream().filter(s -> fvm.post(ts, s).stream().anyMatch(isCrit1)).forEach(state -> ts.addToLabel(state, "crit1_enabled"));
    }

    private static void cleanAP(TransitionSystem<Pair<Pair<String, String>, Map<String, Object>>, String, String> ts){
        ts.getStates().forEach(st -> ts.getAtomicPropositions().forEach(ap -> ts.removeLabel(st, ap)));
        Set<String> aps = new HashSet<>(ts.getAtomicPropositions());
        aps.forEach(ts::removeAtomicProposition);
    }

    private static void addLabelToStates(TransitionSystem<Pair<Pair<String, String>, Map<String, Object>>, String, String> ts, String critLabel, String waitLabel){
        ts.getStates().stream().filter(state -> state.getFirst().getFirst().equals(critLabel)).forEach(state -> ts.addToLabel(state, critLabel));
        ts.getStates().stream().filter(state -> state.getFirst().getFirst().equals(waitLabel)).forEach(state -> ts.addToLabel(state, waitLabel));
    }

    public static class AutomatonGenerator<P> {

        private Set<Set<P>> all;

        AutomatonGenerator(TransitionSystem<?, ?, P> ts) {
            all = Util.powerSet(ts.getAtomicPropositions());
        }

        Automaton<String, P> generateMutexAutomaton(){
            Automaton<String, P> mutexAutomaton = new Automaton<>();
            Predicate<Set<P>> pred = a -> a.contains("crit1") && a.contains("crit2");
            all.stream().filter(pred.negate()).forEach(l -> mutexAutomaton.addTransition("q0", l, "q0"));
            all.stream().filter(pred).forEach(l -> mutexAutomaton.addTransition("q0", l, "q1"));
            all.forEach(l -> mutexAutomaton.addTransition("q1", l, "q1"));
            mutexAutomaton.setInitial("q0");
            mutexAutomaton.setAccepting("q1");
            return mutexAutomaton;
        }

        Automaton<String, P> generateStarvationAutomaton(){
            Automaton<String, P> starvationAutomaton = new Automaton<>();
            all.forEach(s -> starvationAutomaton.addTransition("q0", s, "q0"));
            all.stream().filter(s -> s.contains("wait1")).forEach(s -> starvationAutomaton.addTransition("q0", s, "q1"));
            all.stream().filter(s -> !s.contains("wait1") && !s.contains("crit1")).forEach(s -> starvationAutomaton.addTransition("q1", s, "q1"));
            all.stream().filter(s -> !s.contains("crit1") && !s.contains("crit1_enabled")).forEach(s -> starvationAutomaton.addTransition("q1", s, "q2"));
            all.stream().filter(s -> !s.contains("crit1") && !s.contains("crit1_enabled")).forEach(s -> starvationAutomaton.addTransition("q2", s, "q2"));
            starvationAutomaton.setInitial("q0");
            starvationAutomaton.setAccepting("q2");
            return starvationAutomaton;
        }
    }
}