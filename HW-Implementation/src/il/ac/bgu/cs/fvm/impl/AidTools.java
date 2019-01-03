package il.ac.bgu.cs.fvm.impl;

import il.ac.bgu.cs.fvm.channelsystem.ParserBasedInterleavingActDef;
import il.ac.bgu.cs.fvm.programgraph.ActionDef;

import il.ac.bgu.cs.fvm.programgraph.ConditionDef;
import il.ac.bgu.cs.fvm.programgraph.PGTransition;
import il.ac.bgu.cs.fvm.programgraph.ProgramGraph;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;
import il.ac.bgu.cs.fvm.util.Pair;

import java.util.*;

class AidTools {
    static <S1, S2> Set<Pair<S1, S2>> productSets(Set<S1> s1, Set<S2> s2){
        Set<Pair<S1, S2>> prod = new HashSet<>();
        for(S1 init1 : s1){
            for(S2 init2 : s2){
                prod.add(new Pair<>(init1,init2));
            }
        }
        return prod;
    }

    static <S1, S2, P> Map<Pair<S1, S2>,Set <P>> productMaps(Map<S1, Set<P>> s1, Map<S2, Set<P>> s2){
        Map<Pair<S1, S2>,Set <P>> prod = new HashMap<>();
        for(Map.Entry<S1, Set<P>> init1 : s1.entrySet()){
            for(Map.Entry<S2, Set<P>> init2 : s2.entrySet()){
                Set<P> labels = new HashSet<>();
                labels.addAll(init1.getValue());
                labels.addAll(init2.getValue());
                prod.put(new Pair<>(init1.getKey(),init2.getKey()), labels);
            }
        }
        return prod;
    }

    static <S1, S2, A> Set<Transition<Pair<S1, S2>, A>> productTransitions(Set<Transition<S1, A>> s1, Set<Transition<S2, A>> s2, Set<A> handShakingActions){
        Set<Transition<Pair<S1, S2>, A>> prod = new HashSet<>();
        for(Transition t1 : s1){
            for(Transition t2 : s2){
                if(!handShakingActions.contains(t1.getAction())){
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getFrom()),
                            t1.getAction(),
                            new Pair<>(t1.getTo(), t2.getFrom())));
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getTo()),
                            t1.getAction(),
                            new Pair<>(t1.getTo(), t2.getTo())));
                }
                if(!handShakingActions.contains(t2.getAction())) {
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getFrom()),
                            t2.getAction(),
                            new Pair<>(t1.getFrom(), t2.getTo())));
                    prod.add(new Transition(
                            new Pair<>(t1.getTo(), t2.getFrom()),
                            t2.getAction(),
                            new Pair<>(t1.getTo(), t2.getTo())));
                }
            }
        }
        return prod;
    }

    static <A, S2, S1> Set<Transition<Pair<S1,S2>,A>> productTransitionsHandshake(Set<Transition<S1, A>> s1, Set<Transition<S2, A>> s2, Set<A> handShakingActions) {
        Set<Transition<Pair<S1, S2>, A>> prod = new HashSet<>();
        for(A act : handShakingActions){
            for(Transition<S1,A> t1 : s1){
                for(Transition<S2,A> t2 : s2){
                    if(t1.getAction().equals(t2.getAction()) && act.equals(t1.getAction())){
                        prod.add(new Transition(
                                new Pair<>(t1.getFrom(), t2.getFrom()),
                                t1.getAction(),
                                new Pair<>(t1.getTo(), t2.getTo())));
                    }

                }
            }
        }
        for(Transition<S1,A> t1 : s1){
            for(Transition<S2,A> t2 : s2) {
                if (!handShakingActions.contains(t1.getAction())) {
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getFrom()),
                            t1.getAction(),
                            new Pair<>(t1.getTo(), t2.getFrom())));
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getTo()),
                            t1.getAction(),
                            new Pair<>(t1.getTo(), t2.getTo())));
                }
                if (!handShakingActions.contains(t2.getAction())) {
                    prod.add(new Transition(
                            new Pair<>(t1.getFrom(), t2.getFrom()),
                            t2.getAction(),
                            new Pair<>(t1.getFrom(), t2.getTo())));
                    prod.add(new Transition(
                            new Pair<>(t1.getTo(), t2.getFrom()),
                            t2.getAction(),
                            new Pair<>(t1.getTo(), t2.getTo())));
                }
            }
        }
        return prod;

    }

    //check this - from stack overflow
    private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    static Set<Map<String,Boolean>> genPowerSet(Set<String> names){
        Set<Map<String, Boolean>> powerSetMaps = new HashSet<>();
        Set<Set<String>> namesPowerSet = powerSet(names);
        for (Set<String> setString : namesPowerSet){
            Map<String,Boolean> nameToVal = new HashMap<>();
            for(String name : names){
                if(setString.contains(name)){
                    nameToVal.put(name,true);
                }else{
                    nameToVal.put(name, false);
                }
            }
            powerSetMaps.add(nameToVal);
        }
        return powerSetMaps;
    }

    public static <L1, L2, A> Set<PGTransition<Pair<L1, L2>,A>> getPgTransitionsFromPgProduct(Set <Pair<L1, L2>> initialLocations,
                                                                                              Set<PGTransition<L1, A>> programTransitions1, Set<PGTransition<L2, A>> programTransitions2) {
        Set<Pair<L1, L2>>  reachedLocations = new HashSet<>();
        Set<PGTransition<Pair<L1,L2>, A>> ans = new HashSet<>();
        LinkedList <Pair<L1, L2>> unreachedLocations = new LinkedList<Pair<L1, L2>>(initialLocations);
        while(unreachedLocations.size() > 0){
            Pair<L1, L2> currLocation = unreachedLocations.pollFirst();
            for(PGTransition <L1, A> programTrans : programTransitions1){
                if(programTrans.getFrom().equals(currLocation.first)){
                    Pair<L1, L2> theNewPair = new Pair<>(programTrans.getTo(),currLocation.second);
                    if((!reachedLocations.contains(theNewPair)) && (!unreachedLocations.contains(theNewPair)))
                        unreachedLocations.addLast(theNewPair);

                    ans.add(new PGTransition<>(currLocation,programTrans.getCondition(),
                            programTrans.getAction(),theNewPair));
                }
            }
            for(PGTransition <L2, A> programTrans : programTransitions2){
                if(programTrans.getFrom().equals(currLocation.second)
                ){

                    Pair<L1, L2> theNewPair = new Pair<>(currLocation.first,programTrans.getTo());
                    if((!reachedLocations.contains(theNewPair)) && (!unreachedLocations.contains(theNewPair)))
                        unreachedLocations.addLast(theNewPair);

                    ans.add(new PGTransition<>(currLocation,programTrans.getCondition(),
                            programTrans.getAction(),theNewPair));
                }
            }
            reachedLocations.add(currLocation);
        }
        return ans;
    }

    /**
     * ToDo: check if it needed here to send the transitions
     * */
    public static Set<Map<String, Object>> convertActionSetsToInitialValues(Set<List<String>> initializationList,
                                                                            Set<ActionDef> actionDefs){
        Set<Map<String, Object>> ans = new HashSet<>();
        for (List<String> list :initializationList) {
            Map <String,Object> listValuesMap = new HashMap<>();
            for (Object action: list) {
                listValuesMap = ActionDef.effect(actionDefs,listValuesMap,action);
            }
            ans.add(listValuesMap);
        }
        if (initializationList.isEmpty()){
            ans.add(new HashMap<>());
        }
        return ans;
    }


    /**notice!!!
     * - this function changes the initial states and add to it all the states it's creating
     * - this function changes the ActionSetToFill and add all the needed actions
     * **/
    public static <L, A> Set<Transition<Pair<L, Map<String, Object>>, A>> getTsTransitionsDerivedFromPg(
            Set <Pair<L,Map<String, Object>>> copyOfinitialStates, Set<PGTransition<L, A>> programTransitions,
            Set<ConditionDef> conditionDefs,
            Set<ActionDef> actionDefs,Set <A> ActionSetToFill){

        Set<Transition<Pair<L, Map<String, Object>>, A>> ans = new HashSet<>();
        Set <Pair<L,Map<String, Object>>> reachedStates = new HashSet<>();
        LinkedList <Pair<L,Map<String, Object>>> unreachedStates = new LinkedList<>(copyOfinitialStates);
        while(unreachedStates.size() > 0){
            Pair<L,Map<String, Object>> currLocation = unreachedStates.pollFirst();
            Set<PGTransition<L, A>> relevantTransitions = new HashSet<>(programTransitions);
            relevantTransitions.removeIf(x->!x.getFrom().equals(currLocation.first) ); //removes the new locations that does not have curr location as from
            for (PGTransition<L, A> transition : relevantTransitions) {
                if(ConditionDef.evaluate(conditionDefs,currLocation.second,transition.getCondition())){
                    ActionSetToFill.add(transition.getAction());
                    Map<String, Object> newValuesMap = ActionDef.effect(actionDefs,currLocation.second,
                            transition.getAction());
                    Pair<L,Map<String, Object>> newState = new Pair<>(transition.getTo(),newValuesMap);
                    if ((!reachedStates.contains(newState)) && (!unreachedStates.contains(newState))){
                        unreachedStates.addLast(newState);
                    }
                    Transition <Pair<L, Map<String, Object>>, A> newTransition = new Transition<>(currLocation
                            ,transition.getAction(),newState);
                    ans.add(newTransition);
                }
            }
            reachedStates.add(currLocation);
        }
        copyOfinitialStates.addAll(reachedStates);
        return ans;
    }


    public static <L,A> Set<A> getTsActionsDerivedFromPg(ProgramGraph <L,A> pg){
        Set <A> ans = new HashSet<>();
        for (PGTransition <L,A> transition:pg.getTransitions()) {
            ans.add(transition.getAction());
        }
        return ans;
    }

    static <S1> Set<List<S1>> unionBetweenListInSets(Set<List<S1>> set1, Set<List<S1>> set2){
        Set<List<S1>> ans = new HashSet();
        for(List<S1> listFromSet1 : set1){
            for(List<S1> listFromSet2 : set2){
                LinkedList <S1> newList = new LinkedList(listFromSet1);
                newList.addAll(listFromSet2);
                ans.add(newList);
            }
        }
        return ans;
    }

    public static <L, A> ProgramGraph<List<L>, A> getListPgDerivedFromPg(ProgramGraph<L, A> pg){
        String name = pg.getName();
        Set <List<L>> locations = new HashSet<>();
        Set <List<L>> initialsLocations = new HashSet<>();
        Set <List<String>> initializations = pg.getInitalizations();
        Set<PGTransition<List<L>, A>> transitions = new HashSet<>();

        for (L location : pg.getLocations()) {
            List<L> newLocation = new LinkedList<>(); //the locations are list of L now
            newLocation.add(location);
            locations.add(newLocation);

            if (pg.getInitialLocations().contains(location)){
                initialsLocations.add(newLocation);
            }
        }

        for (PGTransition<L,A> trans : pg.getTransitions()){
            List<L> locationGotOutFrom = new LinkedList<>();
            List<L> locationToGetTo = new LinkedList<>();

            locationGotOutFrom.add(trans.getFrom());
            locationToGetTo.add(trans.getTo());

            PGTransition<List<L>,A> new_trans = new PGTransition<>(locationGotOutFrom, trans.getCondition(),
                    trans.getAction(), locationToGetTo);
            transitions.add(new_trans);
        }

        return new il.ac.bgu.cs.fvm.impl.ProgramGraph<>(name,locations,transitions,initialsLocations,initializations);
    }


    public static <L, A> ProgramGraph<List<L>, A> addNewPgToListPg(ProgramGraph<List<L>, A> pgOfLists, ProgramGraph<L, A> pgToAdd){
        String name = pgOfLists.getName() + pgToAdd.getName();
        Set <List<L>> locations = new HashSet<>();
        Set <List<L>> initialsLocations = new HashSet<>();
        Set <List<String>> initializations = new HashSet<>();
        Set<PGTransition<List<L>, A>> transitions = new HashSet<>();

        ParserBasedInterleavingActDef parser = new ParserBasedInterleavingActDef();



        for (List<L> listLocation : pgOfLists.getLocations())
        {
            for (L location : pgToAdd.getLocations()) {
                List<L> newLocation = new LinkedList<>();
                newLocation.addAll(listLocation);
                newLocation.add(location);
                locations.add(newLocation);
                if(pgOfLists.getInitialLocations().contains(listLocation) &&
                        pgToAdd.getInitialLocations().contains(location))
                    initialsLocations.add(newLocation);

            }
        }
//
//        for (List<L> location1: pgOfLists.getInitialLocations()) {
//            for (L location2: pgToAdd.getInitialLocations()) {
//                List<L> loc = new LinkedList<L>(location1);
//                loc.add(location2);
//                pg_res.setInitial(loc, true);
//            }
//        }

        for (PGTransition<List<L>,A> ListPgTransitions: pgOfLists.getTransitions()) {
            A action = ListPgTransitions.getAction();
            if(parser.isOneSidedAction((String)action)) {
                String listPgAction = (String)action;
                char actionMark = listPgAction.charAt(listPgAction.length()-1);
                if (actionMark == '?') {
                    for (PGTransition<L, A> pgTransition : pgToAdd.getTransitions()) {
                        String currAction = (String) pgTransition.getAction();
                        if (parser.isOneSidedAction(currAction) &&
                                currAction.charAt(currAction.length()-1) == '!' &&
                                currAction.substring(0,currAction.length()-1).equals(listPgAction.substring(0, listPgAction.length()-1))) {

                            List<L> locationGotOutFrom = new LinkedList<>(ListPgTransitions.getFrom());
                            List<L> locationToGetTo = new LinkedList<>(ListPgTransitions.getTo());
                            locationGotOutFrom.add(pgTransition.getFrom());
                            locationToGetTo.add(pgTransition.getTo());
                            String actionToAdd = ListPgTransitions.getAction().toString() + "|" + pgTransition.getAction().toString();

                            transitions.add(new PGTransition<>(locationGotOutFrom, ListPgTransitions.getCondition(), (A) actionToAdd, locationToGetTo));
                        }
                    }
                }
                else {
                    for (PGTransition<L, A> trans2 : pgToAdd.getTransitions()) {
                        String currAction = (String) trans2.getAction();
                        if (currAction.charAt(currAction.length()-1) == '?' && parser.isOneSidedAction(currAction) &&
                                currAction.substring(0,currAction.length()-1).equals(listPgAction.substring(0, listPgAction.length()-1))) {

                            List<L> locationGotOutFrom = new LinkedList<>(ListPgTransitions.getFrom());
                            List<L> locationToGetTo = new LinkedList<>(ListPgTransitions.getTo());
                            locationGotOutFrom.add(trans2.getFrom());
                            locationToGetTo.add(trans2.getTo());
                            String newAction = ListPgTransitions.getAction().toString() + "|" + trans2.getAction().toString();

                            transitions.add(new PGTransition<>(locationGotOutFrom, ListPgTransitions.getCondition(), (A) newAction, locationToGetTo));
                        }
                    }
                }
            }
            else
            {
                for (L location2 : pgToAdd.getLocations()) {
                    List<L> locationGotOutFrom = new LinkedList<>(ListPgTransitions.getFrom());
                    List<L> locationToGetTo = new LinkedList<>(ListPgTransitions.getTo());
                    locationGotOutFrom.add(location2);
                    locationToGetTo.add(location2);

                    transitions.add(new PGTransition<>(locationGotOutFrom, ListPgTransitions.getCondition(), ListPgTransitions.getAction(), locationToGetTo));
                }
            }

        }
        for (PGTransition<L,A> pgTransition: pgToAdd.getTransitions()) {
            if(!parser.isOneSidedAction((String)pgTransition.getAction())) {
                for (List<L> location : pgOfLists.getLocations()) {
                    List<L> locationGotOutFrom = new LinkedList<>(location);
                    locationGotOutFrom.add(pgTransition.getFrom());
                    List<L> locationToGetTo = new LinkedList<>(location);
                    locationToGetTo.add(pgTransition.getTo());

                    transitions.add(new PGTransition<>(locationGotOutFrom, pgTransition.getCondition(), pgTransition.getAction(), locationToGetTo));
                }
            }
        }
        if (pgOfLists.getInitalizations().size() == 0) {
            initializations.addAll(pgToAdd.getInitalizations());
        }
        else if (pgToAdd.getInitalizations().size() == 0) {
            initializations.addAll(pgOfLists.getInitalizations());
        }
        else {
            for (List<String> listPgInitalizations : pgOfLists.getInitalizations()) {
                for (List<String> pgInitalizations : pgToAdd.getInitalizations()) {
                    List<String> initalizations = new LinkedList<>(listPgInitalizations);
                    initalizations.addAll(pgInitalizations);
                    initializations.add(initalizations);
                }
            }
        }
        return new il.ac.bgu.cs.fvm.impl.ProgramGraph<>(name,locations,transitions,initialsLocations,initializations);
    }


    public static <L, A> il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem<Pair<List<L>, Map<String, Object>>, A, String>
    getTrasitionsDerivedFromListPg(ProgramGraph<List<L>, A> pg, Set<ConditionDef> conditionDefs, Set<ActionDef> actionDefs) {
        il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem<Pair<List<L>, Map<String, Object>>, A, String> ans = new il.ac.bgu.cs.fvm.impl.TransitionSystem<>();
        Set<Pair<List<L>, Map<String, Object>>> states = new HashSet<>();
        Set<Pair<List<L>, Map<String, Object>>> initialStates = new HashSet<>();
        Set<Transition<Pair<List<L>, Map<String, Object>>, A>> newTransitions = new HashSet<>();
        Map<Pair<List<L>, Map<String, Object>>, Boolean> stateStatus = new HashMap<>();  //telling us if the state is done or not

        for (List<L> location : pg.getInitialLocations())
            if (pg.getInitalizations().size() == 0) {
                Map<String, Object> varsValues = new HashMap<>();
                Pair<List<L>, Map<String, Object>> state = new Pair<>(location, varsValues);
                stateStatus.put(state, false);
                initialStates.add(state);
                states.add(state);
            } else {
                for (List<String> initialization : pg.getInitalizations()) {
                    Map<String, Object> valuesMap = new HashMap<>();
                    for (String action : initialization) {
                        valuesMap = ActionDef.effect(actionDefs, valuesMap, action);
                    }
                    Pair<List<L>, Map<String, Object>> newState = new Pair<>(location, valuesMap);
                    stateStatus.put(newState, false);
                    initialStates.add(newState);
                    states.add(newState);
                }
            }

        Queue<Pair<List<L>, Map<String, Object>>> statesList = new LinkedList<>(states);

        while (!statesList.isEmpty()) {
            Pair<List<L>, Map<String, Object>>  state = statesList.remove();

            if (stateStatus.get(state)) {
                continue;
            }

            for (PGTransition<List<L>, A> transition : pg.getTransitions()) {
                if(ConditionDef.evaluate(conditionDefs, state.getSecond(),transition.getCondition())
                        && transition.getFrom().equals(state.getFirst())) {

                    if(ActionDef.effect(actionDefs, state.getSecond(), transition.getAction()) == null) {
                        continue;
                    }
                    Pair<List<L>, Map<String, Object>> next = new Pair<>(transition.getTo(), ActionDef.effect(actionDefs,
                            state.getSecond(), transition.getAction()));

                    ans.addAction(transition.getAction());
                    newTransitions.add(new Transition<>(state, transition.getAction(), next));
                    stateStatus.putIfAbsent(next, false);
                    if (!stateStatus.get(next)) {
                        statesList.add(next);
                    }
                }
            }
            stateStatus.put(state, true);
            states.add(state);
        }

        ans.addAllStates(states);

        for (Pair<List<L>, Map<String, Object>> state: states){
            for (L atomicProp: state.getFirst()){
                ans.addAtomicProposition(atomicProp.toString());
                ans.addToLabel(state, atomicProp.toString());
            }

            for (String key : state.second.keySet()) {
                String label = key + " = " + state.second.get(key);
                ans.addAtomicProposition(label);
                ans.addToLabel(state, label);
            }
        }

        for (Pair<List<L>, Map<String, Object>> state: initialStates){
            ans.setInitial(state, true);
        }

        for (Transition<Pair<List<L>, Map<String, Object>> ,A> t: newTransitions){
            ans.addTransition(t);
        }

        return ans;
    }

    public static <S, A, P, Saut> List<Pair<S,Saut>> DFS(Pair<S,Saut> start, il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem<Pair<S,Saut>, A, P> ts, Set<Saut> badStates){
        List<Pair<S,Saut>> result = new ArrayList<>();
        ArrayList<Pair<S,Saut>> toCheck = new ArrayList<>(ts.getStates());
        Stack<Pair<S,Saut>> stack = new Stack<>();
        stack.push(start);
        while(!stack.isEmpty()){
            Pair<S,Saut> state = stack.pop();
            toCheck.remove(state);
            ts.getTransitions().stream()
                    .filter(trans -> trans.getFrom().equals(state))
                    .forEach(trans -> {
                        if(toCheck.contains(trans.getTo()) && !stack.contains(trans.getTo())) {
                            stack.push(trans.getTo());
                        }
                        if(badStates.contains(trans.getTo().getSecond()) && !result.contains(trans.getTo())) {
                            result.add(trans.getTo());
                        }

                    });
        }
        return result;
    }


















}
