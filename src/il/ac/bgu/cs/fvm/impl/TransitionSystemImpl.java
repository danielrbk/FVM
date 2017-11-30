package il.ac.bgu.cs.fvm.impl;


import il.ac.bgu.cs.fvm.exceptions.*;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;
import il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem;

import java.util.*;

public class TransitionSystemImpl<STATE,ACTION,ATOMIC_PROPOSITION> implements TransitionSystem<STATE,ACTION,ATOMIC_PROPOSITION>  {

    private String _name;
    private Set<ACTION> _actions = new HashSet<>();
    private Set<STATE> _initial = new HashSet<>();
    private Set<STATE> _states = new HashSet<>();
    private Set<Transition<STATE,ACTION>> _transitions = new HashSet<>();
    private Set<ATOMIC_PROPOSITION> _ap = new HashSet<>();
    private HashMap<STATE, Set<ATOMIC_PROPOSITION>> _labels = new HashMap<>();

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }


    @Override
    public void addAction(ACTION action) {
        _actions.add(action);
    }

    @Override
    public void addInitialState(STATE state) throws FVMException {
        if(_states.contains(state))
            _initial.add(state);
        else
            throw new InvalidInitialStateException("New initial state does not exist in statess");
    }

    @Override
    public void addState(STATE state) {
        _states.add(state);
        _labels.put(state, new HashSet<ATOMIC_PROPOSITION>());
    }

    @Override
    public void addTransition(Transition<STATE, ACTION> t) throws FVMException {
        if(!(_states.contains(t.getFrom()) && _states.contains(t.getTo()) && _actions.contains(t.getAction())))
            throw new InvalidTransitionException(t);
        _transitions.add(t);
    }

    @Override
    public Set<ACTION> getActions() {
        return _actions;
    }

    @Override
    public void addAtomicProposition(ATOMIC_PROPOSITION p) {
        _ap.add(p);
    }

    @Override
    public Set<ATOMIC_PROPOSITION> getAtomicPropositions() {
        return _ap;
    }

    @Override
    public void addToLabel(STATE s, ATOMIC_PROPOSITION l) throws FVMException {
        if(!_states.contains(s))
            throw new StateNotFoundException("Label added to non-existing state");
        if(!_ap.contains(l))
            throw new InvalidLablingPairException(s,l);
        _labels.get(s).add(l);
    }

    @Override
    public Set<ATOMIC_PROPOSITION> getLabel(STATE s) {
        if(!_labels.containsKey(s))
            throw new StateNotFoundException(s);
        return _labels.get(s);
    }

    @Override
    public Set<STATE> getInitialStates() {
        return _initial;
    }

    @Override
    public Map<STATE, Set<ATOMIC_PROPOSITION>> getLabelingFunction() {
        return _labels;
    }

    @Override
    public Set<STATE> getStates() {
        return _states;
    }

    @Override
    public Set<Transition<STATE, ACTION>> getTransitions() {
        return _transitions;
    }

    @Override
    public void removeAction(ACTION action) throws FVMException {
        for(Transition t:_transitions)
            if(t.getAction().equals(action))
                throw new DeletionOfAttachedActionException(action,TransitionSystemPart.TRANSITIONS);
    }

    @Override
    public void removeAtomicProposition(ATOMIC_PROPOSITION p) throws FVMException {
        for(Set<ATOMIC_PROPOSITION> labels:_labels.values())
            if(labels.contains(p))
                throw new DeletionOfAttachedAtomicPropositionException(p,TransitionSystemPart.LABELING_FUNCTION);
        _ap.remove(p);
    }

    @Override
    public void removeInitialState(STATE state) {
        _initial.remove(state);
    }

    @Override
    public void removeLabel(STATE s, ATOMIC_PROPOSITION l) {
        _labels.get(s).remove(l);
    }

    @Override
    public void removeState(STATE state) throws FVMException {
        if(_initial.contains(state))
            throw new DeletionOfAttachedStateException(state,TransitionSystemPart.INITIAL_STATES);
        if(_labels.get(state).size()>0)
            throw new DeletionOfAttachedStateException(state,TransitionSystemPart.LABELING_FUNCTION);
        for(Transition t:_transitions)
            if(t.getFrom().equals(state) || t.getTo().equals(state))
                throw new DeletionOfAttachedStateException(state,TransitionSystemPart.TRANSITIONS);
        _states.remove(state);
        _labels.remove(state);
    }

    @Override
    public void removeTransition(Transition<STATE, ACTION> t) {
        _transitions.remove(t);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof TransitionSystem))
            return false;
        TransitionSystem<STATE,ACTION,ATOMIC_PROPOSITION> ts = (TransitionSystem<STATE, ACTION, ATOMIC_PROPOSITION>) o;
        if(!Objects.equals(this._labels,ts.getLabelingFunction()))
            return false;
        if(!Objects.equals(this._transitions,ts.getTransitions()))
            return false;
        if(!Objects.equals(this._states,ts.getStates()))
            return false;
        if(!Objects.equals(_initial,ts.getInitialStates()))
            return false;
        if(!Objects.equals(_actions,ts.getActions()))
            return false;
        if(!Objects.equals(_ap,ts.getAtomicPropositions()))
            return false;
        if(!Objects.equals(_name,ts.getName()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 17;
        int result = 1;
        result = prime * result + ((_labels == null) ? 0 : _labels.hashCode());
        result = prime * result + ((_transitions == null) ? 0 : _transitions.hashCode());
        result = prime * result + ((_states == null) ? 0 : _states.hashCode());
        result = prime * result + ((_initial == null) ? 0 : _initial.hashCode());
        result = prime * result + ((_actions == null) ? 0 : _actions.hashCode());
        result = prime * result + ((_ap == null) ? 0 : _ap.hashCode());
        result = prime * result + ((_name == null) ? 0 : _name.hashCode());
        return result;
    }

}