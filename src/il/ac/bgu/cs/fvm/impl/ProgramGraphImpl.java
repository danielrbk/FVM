package il.ac.bgu.cs.fvm.impl;

import il.ac.bgu.cs.fvm.exceptions.FVMException;
import il.ac.bgu.cs.fvm.exceptions.InvalidInitialStateException;
import il.ac.bgu.cs.fvm.exceptions.InvalidTransitionException;
import il.ac.bgu.cs.fvm.programgraph.PGTransition;
import il.ac.bgu.cs.fvm.programgraph.ProgramGraph;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;

import java.util.*;

public class ProgramGraphImpl<L,A> implements ProgramGraph<L,A> {
    private String _name;
    private Set<L> _locations = new HashSet<>();
    private Set<L> _initial = new HashSet<>();
    private Set<PGTransition<L,A>> _transitions = new HashSet<>();
    private Set<List<String>> _inits = new HashSet<>();

    @Override
    public void addInitalization(List<String> init) {
        _inits.add(init);
    }

    @Override
    public void addInitialLocation(L location) {
        if(!_locations.contains(location))
            throw new InvalidInitialStateException(location);
        _initial.add(location);
    }

    @Override
    public void addLocation(L l) {
        _locations.add(l);
    }

    @Override
    public void addTransition(PGTransition<L, A> t) {
        if(!_locations.contains(t.getFrom()))
            throw new FVMException("From not in PG for transition " +t.toString());
        if(!_locations.contains(t.getTo()))
            throw new FVMException("To not in PG for transition " +t.toString());
        _transitions.add(t);
    }

    @Override
    public Set<List<String>> getInitalizations() {
        return _inits;
    }

    @Override
    public Set<L> getInitialLocations() {
        return _initial;
    }

    @Override
    public Set<L> getLocations() {
        return _locations;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Set<PGTransition<L, A>> getTransitions() {
        return _transitions;
    }

    @Override
    public void removeLocation(L l) {
        for(PGTransition t:_transitions) {
            if(t.getFrom().equals(l) || t.getTo().equals(l))
                throw new FVMException(l.toString() + " cannot be removed - is in " +t.toString());
        }
        if(_initial.contains(l))
            throw new FVMException(l.toString() + " cannot be removed - is in initial locations");
        _locations.remove(l);
    }

    @Override
    public void removeTransition(PGTransition<L, A> t) {
        _transitions.remove(t);
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ProgramGraph)) {
            return false;
        }
        ProgramGraph<L,A> pg = (ProgramGraph<L, A>) o;
        if(Objects.equals(getInitalizations(),pg.getInitalizations()) && Objects.equals(getInitialLocations(), pg.getInitialLocations())
                && Objects.equals(getLocations(), pg.getLocations()) && Objects.equals(getName(),pg.getName())
                && Objects.equals(getTransitions(), pg.getTransitions()))
            return true;
        return false;
    }
}
