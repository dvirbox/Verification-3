package il.ac.bgu.cs.fvm.impl;

import il.ac.bgu.cs.fvm.programgraph.PGTransition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgramGraph <L, A> implements il.ac.bgu.cs.fvm.programgraph.ProgramGraph <L, A> {

    private String name;
    private Set <L> locations;
    private Set<PGTransition<L, A>> programTransitions;
    private Set <L> initialLocations;
    private Set <List<String>> initialValues;

    public ProgramGraph(){
        this.locations = new HashSet<L>();
        this.programTransitions = new HashSet<PGTransition<L,A>>();
        this.initialLocations = new HashSet<L>();
        this.initialValues = new HashSet<List<String>>();
    }

    public ProgramGraph(String name, Set<L> locations, Set<PGTransition<L, A>> programTransitions
            , Set<L> initialLocations, Set<List<String>> intialValues) {
        this.name = name;
        this.locations = locations;
        this.programTransitions = programTransitions;
        this.initialLocations = initialLocations;
        this.initialValues = intialValues;
    }
    /**
     * Add an option for the initial value of the variables. The format of the
     * initialization is a list of actions. For example the initialization
     * {@code asList("x := 15", "y:=9")} says that the initial value of x is 15
     * and that the initial value of y is 9.
     * <p>
     * Note that this method can be called several times with different
     * parameters to allow for nondeterministic initialization.
     *
     * @param init A list of initialization actions.
     */
    @Override
    public void addInitalization(List<String> init) {
        this.initialValues.add(init);
    }
    /**
     * Add an initial state.
     *
     * @param location An location already in the graph
     * @param isInitial whether {@code location} should be an initial location in {@code this}.
     * @throws IllegalArgumentException, if {@code location} is not a location in {@code this}.
     */
    @Override
    public void setInitial(L location, boolean isInitial) {
        if (!this.locations.contains(location)) {
            throw new IllegalArgumentException();
        } else {
            if(isInitial)
                this.initialLocations.add(location);
            else
                this.initialLocations.remove(location);
        }
    }
    /**
     * Ann a new location (node) to the program graph.
     *
     * @param l The name of the new location.
     */
    @Override
    public void addLocation(L l) {
        this.locations.add(l);
    }
    /**
     * Add a transition to the program graph.
     *
     * @param t A transition to add.
     */
    @Override
    public void addTransition(PGTransition<L, A> t) {
        this.programTransitions.add(t);
    }
    /**
     * @return The set of initialization lists.
     */
    @Override
    public Set<List<String>> getInitalizations() {
        return this.initialValues;
    }
    /**
     * @return The set of initial locations.
     */
    @Override
    public Set<L> getInitialLocations() {
        return this.initialLocations;
    }
    /**
     * @return The set of locations.
     */
    @Override
    public Set<L> getLocations() {
        return this.locations;
    }
    /**
     * @return The name of the program graph.
     */
    @Override
    public String getName() {
        return this.name;
    }
    /**
     * @return the transitions
     */
    @Override
    public Set<PGTransition<L, A>> getTransitions() {
        return this.programTransitions;
    }
    /**
     * Removes a location from the program graph.
     *
     * @param l A location to remove.
     */
    @Override
    public void removeLocation(L l) {
        this.locations.remove(l);
    }
    /**
     * Remove a transition.
     *
     * @param t A transition to remove.
     */
    @Override
    public void removeTransition(PGTransition<L, A> t) {
        this.programTransitions.remove(t);
    }
    /**
     * Set the name of the program graph.
     *
     * @param name The new name.
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }
}