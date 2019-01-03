package il.ac.bgu.cs.fvm.impl;

import il.ac.bgu.cs.fvm.FvmFacade;
import il.ac.bgu.cs.fvm.automata.Automaton;
import il.ac.bgu.cs.fvm.automata.MultiColorAutomaton;
import il.ac.bgu.cs.fvm.channelsystem.ChannelSystem;
import il.ac.bgu.cs.fvm.channelsystem.ParserBasedInterleavingActDef;
import il.ac.bgu.cs.fvm.circuits.Circuit;
import il.ac.bgu.cs.fvm.exceptions.ActionNotFoundException;
import il.ac.bgu.cs.fvm.exceptions.StateNotFoundException;
import il.ac.bgu.cs.fvm.ltl.LTL;
import il.ac.bgu.cs.fvm.programgraph.*;
import il.ac.bgu.cs.fvm.programgraph.ProgramGraph;
import il.ac.bgu.cs.fvm.transitionsystem.AlternatingSequence;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;
import il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem;
import il.ac.bgu.cs.fvm.util.Pair;
import il.ac.bgu.cs.fvm.verification.VerificationResult;
import il.ac.bgu.cs.fvm.nanopromela.NanoPromelaFileReader;
import il.ac.bgu.cs.fvm.nanopromela.NanoPromelaParser.OptionContext;
import il.ac.bgu.cs.fvm.nanopromela.NanoPromelaParser.StmtContext;
import il.ac.bgu.cs.fvm.verification.VerificationFailed;
import il.ac.bgu.cs.fvm.verification.VerificationSucceeded;
import java.io.InputStream;
import java.util.*;

import java.util.stream.Collectors;
/**
 * Implement the methods in this class. You may add additional classes as you
 <<<<<<< HEAD
 =======
 * want, as long as they live in the {@code impl} package, or one of its
 >>>>>>> d93f54d50c944a309b134d537ce66792880f9763
 * sub-packages.
 */
public class FvmFacadeImpl implements FvmFacade {

    @Override
    public <S, A, P> TransitionSystem<S, A, P> createTransitionSystem() {
        return new il.ac.bgu.cs.fvm.impl.TransitionSystem<>();
    }

    @Override
    public <S, A, P> boolean isActionDeterministic(TransitionSystem<S, A, P> ts) {
        if(ts.getInitialStates().size() > 1){
            return false;
        }
        for (S s : ts.getStates()){
            for(A a : ts.getActions()){
                if(post(ts, s, a).size() > 1){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public <S, A, P> boolean isAPDeterministic(TransitionSystem<S, A, P> ts) {
        if(ts.getInitialStates().size() > 1){
            return false;
        }
        Map<S,Set<P>> labelingMap = ts.getLabelingFunction();
        for (S s : ts.getStates()){
            Set<S> predecessor = post(ts, s);
            for(S postState1 : predecessor){
                for(S postState2 : predecessor){
                    if(!postState1.equals(postState2)){ // check that we don't test same predecessor
                        if(labelingMap.get(postState1).containsAll(labelingMap.get(postState2)) &&
                                labelingMap.get(postState2).containsAll(labelingMap.get(postState1))){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public <S, A, P> boolean isExecution(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        return this.isInitialExecutionFragment(ts, e) && isMaximalExecutionFragment(ts, e);
    }

    @Override
    public <S, A, P> boolean isExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        Set<S> states = ts.getStates();
        Set<A> actions = ts.getActions();
        Set<Transition<S,A>> trans = ts.getTransitions();
        S from = e.head();
        if(!states.contains(from)){
            throw new StateNotFoundException(from);
        }
        AlternatingSequence<A, S> ea = e.tail();
        for (int i = 0; i < ea.size(); i++) {
            A act = ea.head();
            if(!actions.contains(act)){
                throw new ActionNotFoundException(act);
            }
            e = ea.tail();
            S to = e.head();
            if( !states.contains(to)){
                throw new StateNotFoundException(to);
            }
            ea = e.tail();
            boolean isExistingTrans = trans.contains(new Transition<>(from,act,to));
            if(!isExistingTrans){
                return false;
            }
            from = to;
        }
        return true;
    }

    @Override
    public <S, A, P> boolean isInitialExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        return ts.getInitialStates().contains(e.head()) && isExecutionFragment(ts, e);
    }

    @Override
    public <S, A, P> boolean isMaximalExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        return this.isExecutionFragment(ts, e) && this.isStateTerminal(ts, e.last());
    }

    @Override
    public <S, A> boolean isStateTerminal(TransitionSystem<S, A, ?> ts, S s) {
        return this.post(ts, s).isEmpty();
    }

    @Override
    public <S> Set<S> post(TransitionSystem<S, ?, ?> ts, S s) {
        if(!ts.getStates().contains(s)){
            throw new StateNotFoundException(s);
        }
        Set<S> postStates = new HashSet<>();
        for(Transition<S,?> trans : ts.getTransitions()){
            if(trans.getFrom().equals(s)){
                postStates.add(trans.getTo());
            }
        }
        return postStates;
    }

    @Override
    public <S> Set<S> post(TransitionSystem<S, ?, ?> ts, Set<S> c) {
        Set<S> postStates = new HashSet<>();
        for(S state : c){
            if(!ts.getStates().contains(state)){
                throw new StateNotFoundException(state);
            }
            postStates.addAll(post(ts, state));
        }
        return postStates;
    }

    @Override
    public <S, A> Set<S> post(TransitionSystem<S, A, ?> ts, S s, A a) {
        if(!ts.getActions().contains(a)){
            throw new ActionNotFoundException(a);
        }
        if(!ts.getStates().contains(s)){
            throw new StateNotFoundException(s);
        }
        Set<S> postStates = new HashSet<>();
        for(Transition<S,A> trans : ts.getTransitions()){
            if(trans.getFrom().equals(s) && trans.getAction().equals(a)){
                postStates.add(trans.getTo());
            }
        }
        return postStates;
    }

    @Override
    public <S, A> Set<S> post(TransitionSystem<S, A, ?> ts, Set<S> c, A a) {
        if(!ts.getActions().contains(a)){
            throw new ActionNotFoundException(a);
        }
        Set<S> postStates = new HashSet<>();
        for(S state : c){
            if(!ts.getStates().contains(state)){
                throw new StateNotFoundException(state);
            }
            postStates.addAll(post(ts, state, a));
        }
        return postStates;
    }

    @Override
    public <S> Set<S> pre(TransitionSystem<S, ?, ?> ts, S s) {
        if(!ts.getStates().contains(s)){
            throw new StateNotFoundException(s);
        }
        Set<S> postStates = new HashSet<>();
        for(Transition<S,?> trans : ts.getTransitions()){
            if(trans.getTo().equals(s)){
                postStates.add(trans.getFrom());
            }
        }
        return postStates;
    }

    @Override
    public <S> Set<S> pre(TransitionSystem<S, ?, ?> ts, Set<S> c) {
        Set<S> postStates = new HashSet<>();
        for(S state : c){
            if(!ts.getStates().contains(state)){
                throw new StateNotFoundException(state);
            }
            postStates.addAll(pre(ts, state));
        }
        return postStates;
    }

    @Override
    public <S, A> Set<S> pre(TransitionSystem<S, A, ?> ts, S s, A a) {
        if(!ts.getStates().contains(s)){
            throw new StateNotFoundException(s);
        }
        if(!ts.getActions().contains(a)){
            throw new ActionNotFoundException(a);
        }
        Set<S> postStates = new HashSet<>();
        for(Transition<S,A> trans : ts.getTransitions()){
            if(trans.getTo().equals(s) && trans.getAction().equals(a)){
                postStates.add(trans.getFrom());
            }
        }
        return postStates;
    }

    @Override
    public <S, A> Set<S> pre(TransitionSystem<S, A, ?> ts, Set<S> c, A a) {
        if(!ts.getActions().contains(a)){
            throw new ActionNotFoundException(a);
        }
        Set<S> postStates = new HashSet<>();
        for(S state : c){
            if(!ts.getStates().contains(state)){
                throw new StateNotFoundException(state);
            }
            postStates.addAll(pre(ts, state, a));
        }
        return postStates;
    }

    @Override
    public <S, A> Set<S> reach(TransitionSystem<S, A, ?> ts) {
        Set<S> reachable = new HashSet<>();
        LinkedList<S> subjects = new LinkedList<S>(ts.getInitialStates());
        while(subjects.size() > 0){
            S curr = subjects.pollFirst();
            for(Transition<S, A> trans : ts.getTransitions()){
                if(trans.getFrom().equals(curr)
                        && !reachable.contains(trans.getTo())
                        && !subjects.contains(trans.getTo())){
                    subjects.addLast(trans.getTo());
                }
            }
            reachable.add(curr);
        }
        return reachable;
    }

    @Override
    public <S1, S2, A, P> TransitionSystem<Pair<S1, S2>, A, P> interleave(TransitionSystem<S1, A, P> ts1, TransitionSystem<S2, A, P> ts2) {
        return interleave(ts1, ts2, new HashSet<>());
    }

    @Override
    public <S1, S2, A, P> TransitionSystem<Pair<S1, S2>, A, P> interleave(TransitionSystem<S1, A, P> ts1, TransitionSystem<S2, A, P> ts2, Set<A> handShakingActions) {
        String name = ts1.getName() + ts2.getName();
        Set<A> actions = new HashSet<>();
        actions.addAll(ts1.getActions());
        actions.addAll(ts2.getActions());

        Set<P> atomicPropositions = new HashSet<>();
        atomicPropositions.addAll(ts1.getAtomicPropositions());
        atomicPropositions.addAll(ts2.getAtomicPropositions());

        Set<Pair<S1, S2>> initialStates = AidTools.productSets(ts1.getInitialStates(), ts2.getInitialStates());
        Set<Transition<Pair<S1, S2>, A>> transitions = AidTools.productTransitionsHandshake(ts1.getTransitions(), ts2.getTransitions(), handShakingActions);
        Map<Pair<S1, S2>,Set <P>> states = AidTools.productMaps(ts1.getLabelingFunction(), ts2.getLabelingFunction());
        il.ac.bgu.cs.fvm.impl.TransitionSystem interleaveTS = new il.ac.bgu.cs.fvm.impl.TransitionSystem<>(name, states, actions, transitions, initialStates, atomicPropositions);
        Set reachable = reach(interleaveTS);
        states.entrySet().removeIf(e-> !reachable.contains(e.getKey()) );
        transitions.removeIf(t -> !reachable.contains(t.getFrom()));
        return new il.ac.bgu.cs.fvm.impl.TransitionSystem<>(name, states, actions, transitions, initialStates, atomicPropositions);
    }

    @Override
    public <L, A> ProgramGraph<L, A> createProgramGraph() {
        return new il.ac.bgu.cs.fvm.impl.ProgramGraph<>();
    }

    @Override
    public <L1, L2, A> ProgramGraph<Pair<L1, L2>, A> interleave(ProgramGraph<L1, A> pg1, ProgramGraph<L2, A> pg2) {
        String name = pg1.getName() + pg2.getName();
        Set <Pair<L1, L2>> locations = AidTools.productSets(pg1.getLocations(),pg2.getLocations());

        Set<Pair<L1, L2>> initialLocations = AidTools.productSets(pg1.getInitialLocations(),pg2.getInitialLocations());

        Set <List<String>> initialValues = AidTools.unionBetweenListInSets(pg1.getInitalizations(),pg2.getInitalizations());

        Set<PGTransition<Pair<L1,L2>, A>> productProgramTransitions =  AidTools.getPgTransitionsFromPgProduct(initialLocations,
                pg1.getTransitions(), pg2.getTransitions());
        ProgramGraph<Pair<L1, L2>, A> ans = new il.ac.bgu.cs.fvm.impl.ProgramGraph<Pair<L1, L2>, A>
                (name,locations,productProgramTransitions ,initialLocations,initialValues);
        return ans;
    }

    @Override
    public TransitionSystem<Pair<Map<String, Boolean>, Map<String, Boolean>>, Map<String, Boolean>, Object>
    transitionSystemFromCircuit(Circuit c) {

        TransitionSystem<Pair<Map<String, Boolean>, Map<String, Boolean>>, Map<String, Boolean>, Object> ts =
                new il.ac.bgu.cs.fvm.impl.TransitionSystem<>();
        Set<Map<String,Boolean>> inputPowerSet = AidTools.genPowerSet(c.getInputPortNames());
        Set<Map<String,Boolean>> RegPowerSet = AidTools.genPowerSet(c.getRegisterNames());

        /* Create states and add them to Transition system.
         *  Create Initial states and add tem to Transition systems*/
        for(Map<String,Boolean> inElement : inputPowerSet){
            for(Map<String,Boolean> regElement: RegPowerSet){
                Pair<Map<String,Boolean>,Map<String,Boolean>> state = new Pair<>(inElement, regElement);
                ts.addState(state);
                if(!regElement.containsValue(true)){
                    ts.setInitial(state, true);
                }
            }
        }

        /* add Actions to Transition system */
        ts.addAllActions(inputPowerSet);

        /* Create transitions and add them to Transition system. */
        for(Pair<Map<String, Boolean>, Map<String, Boolean>> stateFrom : ts.getStates()){
            for(Map<String, Boolean> act : ts.getActions()){
                Map<String, Boolean> newReg = c.updateRegisters(stateFrom.getFirst(), stateFrom.getSecond());
                Pair<Map<String, Boolean>, Map<String, Boolean>> stateTo = new Pair<>(act, newReg);
                ts.addTransition(new Transition<>(stateFrom, act, stateTo));
            }
        }
        /* Check which states are reachable in ts*/
        Set<Pair<Map<String, Boolean>,Map<String, Boolean>>> reachable = reach(ts);
        ts.getStates().removeIf(e-> !reachable.contains(e) );
        ts.getTransitions().removeIf(t -> !reachable.contains(t.getFrom()));

        /* create AP and add them to Transition system. */
        for (String reg : c.getRegisterNames()) {
            ts.addAtomicProposition(reg);
        }
        for (String in : c.getInputPortNames()) {
            ts.addAtomicProposition(in);
        }
        for (String out : c.getOutputPortNames()) {
            ts.addAtomicProposition(out);
        }

        /* create Labels and add them to Transition system. */
        for(Pair<Map<String,Boolean>,Map<String,Boolean>> state : ts.getStates()){
            //ri...r#
            for(String reg: state.second.keySet()){
                if(state.second.get(reg)){
                    ts.addToLabel(state, reg);
                }
            }
            //xi...x#
            for(String in: state.first.keySet()){
                if(state.first.get(in)){
                    ts.addToLabel(state, in);
                }
            }
            //out...out#
            Map<String, Boolean> outs = c.computeOutputs(state.first, state.second);
            for(String out : outs.keySet()){
                if(outs.get(out)){
                    ts.addToLabel(state, out);
                }
            }
        }
        return ts;
    }

    @Override
    public <L, A> TransitionSystem<Pair<L, Map<String, Object>>, A, String> transitionSystemFromProgramGraph(
            ProgramGraph<L, A> pg, Set<ActionDef> actionDefs,
            Set<ConditionDef> conditionDefs) {
        String name = pg.getName();
        Set<List<String>> initializations = pg.getInitalizations();
        Set<Map<String, Object>> initialValues = AidTools.convertActionSetsToInitialValues(initializations, actionDefs);
        Set <Pair<L, Map<String, Object>>> initialStates = new HashSet<>();
        initialStates = AidTools.productSets(pg.getInitialLocations(), initialValues);
        Set <Pair<L, Map<String, Object>>> copyOfInitialStates = new HashSet<>(initialStates);
        Set <A> actions = new HashSet<>();
        /**IMPORTANT: besides creating the new transitions the getTsTransitionsDerivedFromPg adding CopyOfInitialStates**/
        Set<Transition<Pair<L, Map<String, Object>>, A>> tsTransitions = AidTools.getTsTransitionsDerivedFromPg(copyOfInitialStates,pg.getTransitions(),
                conditionDefs,actionDefs,actions);

//      iterating all the states and creating the needed AP
        Set <String> atomicPropositions = new HashSet<>();
        Map<Pair<L, Map<String, Object>>,Set <String>> statesAndAtomicProp = new HashMap<>();
        for (Pair<L,Map<String,Object>> currState : copyOfInitialStates) {
            Set<String> AtomicPropOfCurrState = new HashSet<>();
            atomicPropositions.add(currState.first.toString());
            AtomicPropOfCurrState.add(currState.first.toString());
            for (String key : currState.second.keySet()) {
                atomicPropositions.add(key + " = " + currState.second.get(key).toString());
                AtomicPropOfCurrState.add(key + " = " + currState.second.get(key).toString());
            }
            statesAndAtomicProp.put(currState,AtomicPropOfCurrState);
        }
        return new il.ac.bgu.cs.fvm.impl.TransitionSystem<>(name, statesAndAtomicProp, actions, tsTransitions, initialStates, atomicPropositions);
    }


    @Override
    public <L, A> TransitionSystem<Pair<List<L>, Map<String, Object>>, A, String> transitionSystemFromChannelSystem(ChannelSystem<L, A> channSys) {

        ProgramGraph <List<L>,A> pg = AidTools.getListPgDerivedFromPg(channSys.getProgramGraphs().get(0));
        Set<ConditionDef> conditions = new LinkedHashSet<>();
        Set<ActionDef> actions = new LinkedHashSet<>();
        conditions.add(new ParserBasedCondDef());
        actions.add(new ParserBasedInterleavingActDef());
        actions.add(new ParserBasedActDef());

        for (ProgramGraph<L,A> currPg: channSys.getProgramGraphs().subList(1,channSys.getProgramGraphs().size())){
            pg = AidTools.addNewPgToListPg(pg, currPg);
        }
        return AidTools.getTrasitionsDerivedFromListPg(pg, conditions ,actions);
    }



    @Override
    public <Sts, Saut, A, P> TransitionSystem<Pair<Sts, Saut>, A, Saut> product(TransitionSystem<Sts, A, P> ts, Automaton<Saut, P> aut) {
        TransitionSystem<Pair<Sts, Saut>, A, Saut> tsRes = createTransitionSystem();
        ts.getInitialStates().forEach(s ->
        {
            aut.getInitialStates().forEach(in_state -> {
                Set<Pair<Sts, Saut>> initials = aut.nextStates(in_state, ts.getLabel(s)).stream()
                        .map(q -> Pair.pair(s, q))
                        .collect(Collectors.toSet());
                initials.forEach(state ->{
                    tsRes.addState(state);
                    tsRes.setInitial(state, true);
                });
            });
        });

        List<Pair<Sts, Saut>> done = new ArrayList<>();
        List<Pair<Sts, Saut>> subjects = new ArrayList<>(tsRes.getInitialStates());

        while (!subjects.isEmpty()) {
            Pair<Sts, Saut> current = subjects.get(0);
            ts.getTransitions().forEach(trans -> {
                if (trans.getFrom().equals(current.getFirst())) {
                    Set<Saut> next = aut.nextStates(current.getSecond(), ts.getLabel(trans.getTo()));
                    if(next != null){
                        Set<Pair<Sts, Saut>> newStates = aut.nextStates(current.getSecond(), ts.getLabel(trans.getTo())).stream()
                                .map(q -> Pair.pair(trans.getTo(), q))
                                .collect(Collectors.toSet());
                        if (!newStates.isEmpty()) {
                            tsRes.addAction(trans.getAction());
                            newStates.forEach(s -> {
                                tsRes.addState(s);
                                tsRes.addTransition(new Transition<>(current, trans.getAction(), s));
                                tsRes.addAtomicProposition(s.getSecond());
                                tsRes.addToLabel(s, s.getSecond());
                                if (!subjects.contains(s) && !done.contains(s)) {
                                    subjects.add(s);
                                }
                            });
                        }
                    }
                }
            });
            subjects.remove(current);
            done.add(current);
        }
        return tsRes;
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromela(String filename) throws Exception {
        return programGraphFromStmtContext(NanoPromelaFileReader.pareseNanoPromelaFile(filename));
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromelaString(String nanopromela) throws Exception {
        return programGraphFromStmtContext(NanoPromelaFileReader.pareseNanoPromelaString(nanopromela));
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromela(InputStream inputStream) throws Exception {
        return programGraphFromStmtContext(NanoPromelaFileReader.parseNanoPromelaStream(inputStream));
    }

    private ProgramGraph<String, String> programGraphFromStmtContext(StmtContext rootLocation) throws Exception {
        ProgramGraph<String, String> pg = createProgramGraph();
        Map< String, Set<PGTransition<String, String> >> locToTransMap = new HashMap<>();
        sub(rootLocation, locToTransMap);

        addReachableToPG(rootLocation, pg, locToTransMap); // generate Transitions and locations to PG
        pg.setInitial(rootLocation.getText(), true);
        return pg;
    }

    private boolean isAtomicStmt(StmtContext stmt){///
        return stmt.assstmt() != null ||
                stmt.chanreadstmt() != null ||
                stmt.chanwritestmt() != null ||
                stmt.atomicstmt() != null ||
                stmt.skipstmt() != null;
    }

    private void sub(StmtContext stmtNode,
                     Map<String, Set<PGTransition<String, String>>> locToTransMap) {
        String curNodeText = stmtNode.getText();
        if (!locToTransMap.containsKey(curNodeText)){

            Set<PGTransition<String, String>> transitions = new HashSet<>();
            // Atomic Stmt
            if (isAtomicStmt(stmtNode)) {
                //generate exit transition
                transitions.add(new PGTransition<>(curNodeText/** From **/, "", curNodeText/** to **/ , ""));
                locToTransMap.put(curNodeText, transitions);
            }

            // if..fi Stmt
            else if (stmtNode.ifstmt() != null) {
                for (OptionContext opt : stmtNode.ifstmt().option()) {
                    sub(opt.stmt(), locToTransMap);
                    String optText = opt.stmt().getText();
                    for (PGTransition<String, String> trans : locToTransMap.get(optText)) {
                        String optBoolExprText = opt.boolexpr().getText();
                        String condition = getCondition(optBoolExprText, trans);
                        PGTransition<String, String> newTransition = new PGTransition<>(curNodeText, condition, trans.getAction(), trans.getTo());
                        transitions.add(newTransition);
                    }
                }
                locToTransMap.put(curNodeText, transitions);
            }

            // do..od Stmt
            else if (stmtNode.dostmt() != null) {
                boolean isFirst = true;
                StringBuilder not = new StringBuilder();
                for (OptionContext opt : stmtNode.dostmt().option()) {
                    String optText = opt.stmt().getText();
                    sub(opt.stmt(), locToTransMap);
                    String optBoolExprText = opt.boolexpr().getText();
                    if(isFirst){
                        isFirst = false;
                        not = new StringBuilder("!" + generateParantesis(optBoolExprText));
                    }
                    else{
                        not.append(" && " + "!").append(generateParantesis(optBoolExprText));
                    }
                    handleDoTransitions(curNodeText, optText, curNodeText, optBoolExprText,locToTransMap, new HashSet<>());
                }
                PGTransition<String, String> noConditionsMet = new PGTransition<>(curNodeText, not.toString(), "", "");
                //add Transition
                if (!locToTransMap.containsKey(noConditionsMet.getFrom())){
                    locToTransMap.put(noConditionsMet.getFrom(), new HashSet<>());
                }
                locToTransMap.get(noConditionsMet.getFrom()).add(noConditionsMet);
            }

            // chain Stmt
            else {
                StmtContext left = stmtNode.stmt(0);
                sub(left, locToTransMap);
                StmtContext right = stmtNode.stmt(1);
                sub(right, locToTransMap);
                handleConcatTransitions(left.getText(), right.getText(), curNodeText,locToTransMap, new HashSet<>());
            }
        }
    }

    private void addReachableToPG(StmtContext rootLocation,
                                  ProgramGraph<String, String> pg,
                                  Map<String, Set<PGTransition<String, String>>> locToTransMap){
        pg.addLocation(rootLocation.getText());
        Queue<String> LocationstoHandle = new LinkedList<>(pg.getLocations());
        for(; !LocationstoHandle.isEmpty() ;){
            String currLocation = LocationstoHandle.poll();
            if (!currLocation.isEmpty())
                for (PGTransition<String, String> transition : locToTransMap.get(currLocation)) {
                    String toLoc = transition.getTo();
                    boolean locationExist = pg.getLocations().contains(toLoc);
                    if (!locationExist) {
                        LocationstoHandle.add(toLoc);
                        pg.addLocation(toLoc);
                    }
                    pg.addTransition(transition);
                }
        }
    }

    private String generateParantesis(String element){
        return "(" + element + ")";
    }

    private void handleDoTransitions(String currLocation,
                                     String opString,
                                     String rightStmtString,
                                     String optBoolExprText,
                                     Map<String,Set<PGTransition<String,String>>> locToTransMap,
                                     Set<String> handled ){
        if (!handled.contains(opString)){
            handled.add(opString);
            for (PGTransition<String, String> trans : locToTransMap.get(opString)) {
                String condition = getCondition(optBoolExprText, trans);
                boolean toIsEmpty = trans.getTo().isEmpty();
                String to = trans.getTo() + (toIsEmpty ? "" : ";") + rightStmtString;
                //add Transition
                PGTransition<String, String> transition = new PGTransition<>(currLocation, condition, trans.getAction(), to);
                if (!locToTransMap.containsKey(transition.getFrom())){
                    locToTransMap.put(transition.getFrom(), new HashSet<>());
                }
                locToTransMap.get(transition.getFrom()).add(transition);
                if (!toIsEmpty){
                    handleDoTransitions(to, trans.getTo(), rightStmtString, "", locToTransMap, handled);
                }
            }
        }
    }

    private void handleConcatTransitions(String left,
                                         String right,
                                         String curNodeText,
                                         Map<String, Set<PGTransition<String, String>>> locToTransMap,
                                         Set<String> handled ){
        if (!handled.contains(left)) {
            handled.add(left);
            for (PGTransition<String, String> leftTrans : locToTransMap.get(left)) {
                boolean leftToIsEmpty = leftTrans.getTo().isEmpty();
                String leftright = leftTrans.getTo() + (leftToIsEmpty ? "" : ";") + right;
                PGTransition<String, String> transition = new PGTransition<>(curNodeText, leftTrans.getCondition(), leftTrans.getAction(), leftright);
                if (!locToTransMap.containsKey(transition.getFrom())){
                    locToTransMap.put(transition.getFrom(), new HashSet<>());
                }
                locToTransMap.get(transition.getFrom()).add(transition);
                if (!leftToIsEmpty){
                    handleConcatTransitions(leftTrans.getTo(), right, leftright, locToTransMap, handled);
                }
            }
        }
    }

    private String getCondition(String optBoolExprText, PGTransition<String, String> trans) {
        String condition;
        if(optBoolExprText.isEmpty()){
            condition = trans.getCondition();
        }
        else if(trans.getCondition().isEmpty()){
            condition = optBoolExprText;
        }
        else{
            condition = optBoolExprText + " && " + trans.getCondition();
        }
        return condition;
    }

    /********************************************** implement untill here ********************************************************/

    @Override
    public <S, A, P, Saut> VerificationResult<S> verifyAnOmegaRegularProperty(TransitionSystem<S, A, P> ts, Automaton<Saut, P> aut) {
        TransitionSystem<Pair<S, Saut>, A, Saut> product = product(ts,aut);
        Set<Saut> accStates = aut.getAcceptingStates();

        for(Pair<S, Saut> s0 : product.getInitialStates()){
            for(Pair<S, Saut> s : AidTools.DFS(s0, product, accStates)){
                List<Pair<S, Saut>> reachable = AidTools.DFS(s, product, accStates);
                if(reachable.contains(s)){
                    List<S> fromStart = getPath(s0, s, product);
                    List<S> circle = getPath(s, s, product);
                    circle.remove(0);

                    VerificationFailed<S> failed = new VerificationFailed<>();
                    failed.setPrefix(fromStart);
                    failed.setCycle(circle);

                    return failed;
                }
            }
        }

        return new VerificationSucceeded<>();
    }

    private <S, A, P, Saut> List<S> getPath(Pair<S,Saut> start, Pair<S,Saut> end, TransitionSystem<Pair<S,Saut>, A, P> ts){
        ArrayList<Pair<S,Saut>> subjects = new ArrayList<>(ts.getStates());
        Map<Pair<S,Saut>,List<List<S>>> paths = new HashMap<>();
        paths.put(start, Collections.singletonList(Collections.singletonList(start.getFirst())));
        Stack<Pair<S,Saut>> stack = new Stack<>();
        stack.push(start);

        while(!stack.isEmpty()){
            Pair<S,Saut> state = stack.pop();
            subjects.remove(state);
            ts.getTransitions().stream()
                    .filter(trans -> trans.getFrom().equals(state))
                    .forEach(trans ->{
                        if(subjects.contains(trans.getTo()) && !stack.contains(trans.getTo())){
                            stack.push(trans.getTo());
                        }
                        List<List<S>> toAdd = new ArrayList<>();
                        paths.get(trans.getFrom()).forEach(lst ->{
                            List<S> newList = new ArrayList<>(lst);
                            newList.add(trans.getTo().first);
                            toAdd.add(newList);
                        });

                        paths.put(trans.getTo(), toAdd);
                    });
        }
        return paths.get(end).get(0);
    }


    @Override
    public <L> Automaton<?, L> LTL2NBA(LTL<L> ltl) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement LTL2NBA
    }

    @Override
    public <L> Automaton<?, L> GNBA2NBA(MultiColorAutomaton<?, L> mulAut) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement GNBA2NBA
    }

}
