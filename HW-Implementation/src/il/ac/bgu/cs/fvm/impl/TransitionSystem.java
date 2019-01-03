package il.ac.bgu.cs.fvm.impl;


import il.ac.bgu.cs.fvm.exceptions.*;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransitionSystem<STATE,ACTION,ATOMIC_PROPOSITION> implements il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem <STATE,ACTION,ATOMIC_PROPOSITION> {
    private String name;
    private Map<STATE,Set <ATOMIC_PROPOSITION>> states;
    private Set<ACTION> actions;
    private Set<Transition<STATE, ACTION>> transitions;
    private Set<STATE> initialStates;
    private Set<ATOMIC_PROPOSITION> atomicPropositions;
//    this.statesTransitions.computeIfAbsent(t.getFrom(), k -> new HashSet<>()).add(t);

    public TransitionSystem(){
        this.name = "";
        this.states = new HashMap<>();
        this.actions = new HashSet<>();
        this.transitions = new HashSet<>();
        this.initialStates = new HashSet<>();
        this.atomicPropositions = new HashSet<>();

    }

    public TransitionSystem(String name,
                            Map<STATE,Set <ATOMIC_PROPOSITION>> states,
                            Set<ACTION> actions,
                            Set<Transition<STATE, ACTION>> transitions,
                            Set<STATE> initialStates,
                            Set<ATOMIC_PROPOSITION> atomicPropositions
    ){
        this.name = name;
        this.states = states;
        this.actions = actions;
        this.transitions = transitions;
        this.initialStates = initialStates;
        this.atomicPropositions = atomicPropositions;

    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addAction(ACTION anAction) {
        this.actions.add(anAction);
    }

    @Override
    public void setInitial(STATE aState, boolean isInitial) throws StateNotFoundException {

        if(this.states.containsKey(aState)){
            if(isInitial){
                this.initialStates.add(aState);
            }
            else{
                this.initialStates.remove(aState);
            }
        }
        else{
            throw new StateNotFoundException(aState);
        }
    }

    @Override
    public void addState(STATE state) {
        this.states.put(state, new HashSet<>());
    }

    @Override
    public void addTransition(Transition<STATE, ACTION> t) throws FVMException {
        if(!this.states.containsKey(t.getFrom()) || !this.actions.contains(t.getAction()) || !this.states.containsKey(t.getTo())){
            throw new InvalidTransitionException(t);
        }
        this.transitions.add(t);
    }

    @Override
    public Set<ACTION> getActions() {
        return new HashSet<>(this.actions);
    }

    @Override
    public void addAtomicProposition(ATOMIC_PROPOSITION p) {
        this.atomicPropositions.add(p);
    }

    @Override
    public Set<ATOMIC_PROPOSITION> getAtomicPropositions() {
        return this.atomicPropositions;
    }

    @Override
    public void addToLabel(STATE s, ATOMIC_PROPOSITION l) throws FVMException {
        if(this.atomicPropositions.contains(l)){
            if(this.states.containsKey(s)){
                this.states.get(s).add(l);
            }
            else{
                throw new StateNotFoundException(s);
            }
        }
        else{
            throw new InvalidLablingPairException(s, l);
        }
    }

    @Override
    public Set<ATOMIC_PROPOSITION> getLabel(STATE s) {
        if(this.states.containsKey(s)){
            return this.states.get(s);
        }
        throw new StateNotFoundException(s);
    }

    @Override
    public Set<STATE> getInitialStates() {
        return this.initialStates;
    }

    @Override
    public Map<STATE, Set<ATOMIC_PROPOSITION>> getLabelingFunction() {
        return this.states;
    }

    @Override
    public Set<STATE> getStates() {
        return this.states.keySet();
    }

    @Override
    public Set<Transition<STATE, ACTION>> getTransitions() {
        return this.transitions;
    }

    @Override
    public void removeAction(ACTION action) throws FVMException {
        for(Transition trans : this.transitions){
            if(trans.getAction().equals(action)){
                throw new DeletionOfAttachedActionException(action, TransitionSystemPart.TRANSITIONS);
            }
        }
        this.actions.remove(action);
    }

    @Override
    public void removeAtomicProposition(ATOMIC_PROPOSITION p) throws FVMException {
        for (Set<ATOMIC_PROPOSITION> value : this.states.values()) { //check If the proposition is used as label of a state.
            if(value.contains(p)){
                throw new DeletionOfAttachedAtomicPropositionException(p, TransitionSystemPart.STATES);
            }
        }
        this.atomicPropositions.remove(p);
    }

    @Override
    public void removeLabel(STATE s, ATOMIC_PROPOSITION l) {
        Set<ATOMIC_PROPOSITION> labels = this.states.get(s);
        if(labels != null){
            labels.remove(l);
        }
    }

    @Override
    public void removeState(STATE state) throws FVMException {
        for(Transition trans : this.transitions){
            if(trans.getFrom().equals(state) || trans.getTo().equals(state)){
                throw new DeletionOfAttachedStateException(state, TransitionSystemPart.TRANSITIONS);
            }
        }
        if(! this.states.get(state).isEmpty() || this.initialStates.contains(state)){
            throw new DeletionOfAttachedStateException(state, TransitionSystemPart.STATES);
        }
        else{
            this.states.remove(state);
        }
    }

    @Override
    public void removeTransition(Transition<STATE, ACTION> t) {
        this.transitions.remove(t);
    }
}
