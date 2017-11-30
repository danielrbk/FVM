package il.ac.bgu.cs.fvm.impl;

import com.sun.org.apache.xpath.internal.operations.Bool;
import il.ac.bgu.cs.fvm.FvmFacade;
import il.ac.bgu.cs.fvm.automata.Automaton;
import il.ac.bgu.cs.fvm.automata.MultiColorAutomaton;
import il.ac.bgu.cs.fvm.channelsystem.ChannelSystem;
import il.ac.bgu.cs.fvm.circuits.Circuit;
import il.ac.bgu.cs.fvm.exceptions.ActionNotFoundException;
import il.ac.bgu.cs.fvm.exceptions.StateNotFoundException;
import il.ac.bgu.cs.fvm.ltl.AP;
import il.ac.bgu.cs.fvm.ltl.LTL;
import il.ac.bgu.cs.fvm.programgraph.ActionDef;
import il.ac.bgu.cs.fvm.programgraph.ConditionDef;
import il.ac.bgu.cs.fvm.programgraph.PGTransition;
import il.ac.bgu.cs.fvm.programgraph.ProgramGraph;
import il.ac.bgu.cs.fvm.transitionsystem.AlternatingSequence;
import il.ac.bgu.cs.fvm.transitionsystem.Transition;
import il.ac.bgu.cs.fvm.transitionsystem.TransitionSystem;
import il.ac.bgu.cs.fvm.util.Pair;
import il.ac.bgu.cs.fvm.verification.VerificationResult;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Implement the methods in this class. You may add additional classes as you
 * want, as long as they live in the {@code impl} package, or one of its 
 * sub-packages.
 */
public class FvmFacadeImpl implements FvmFacade {

    @Override
    public <S, A, P> TransitionSystem<S, A, P> createTransitionSystem() {
        return new TransitionSystemImpl<S ,A ,P>();
    }

    @Override
    public <S, A, P> boolean isActionDeterministic(TransitionSystem<S, A, P> ts) {
        if(ts.getInitialStates().size()>1)
            return false;
        HashMap<S,Set<A>> stateToActions = new HashMap<>();
        for(S state:ts.getStates()) {
            stateToActions.put(state, new HashSet<>());
        }
        for(Transition<S,A> transition:ts.getTransitions()) {
            if(stateToActions.get(transition.getFrom()).contains(transition.getAction()))
                return false;
            stateToActions.get(transition.getFrom()).add(transition.getAction());
        }
        return true;
    }

    @Override
    public <S, A, P> boolean isAPDeterministic(TransitionSystem<S, A, P> ts) {
        if(ts.getInitialStates().size()>1)
            return false;
        HashMap<S,Set<Set<P>>> stateToAP = new HashMap<>();
        for(S state:ts.getStates()) {
            stateToAP.put(state, new HashSet<>());
        }
        for(Transition<S,A> transition:ts.getTransitions()) {
            Set<Set<P>> setOfLabels = stateToAP.get(transition.getFrom());
            Set<P> newLabel = ts.getLabel(transition.getTo()) ;
            if(setOfLabels.contains(newLabel)) {
                return false;
            }
            setOfLabels.add(newLabel);
        }
        return true;
    }

    @Override
    public <S, A, P> boolean isExecution(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        return isExecutionFragment(ts,e) && isInitialExecutionFragment(ts,e) && isMaximalExecutionFragment(ts,e);
    }

    @Override
    public <S, A, P> boolean isExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        Set<S> states = ts.getStates();
        Set<A> actions = ts.getActions();
        while(e.size()!=1 && ts.getStates().contains(e.head())){
            if(!states.contains(e.head()))
                throw new StateNotFoundException(e.head());
            if(!actions.contains(e.tail().head()))
                throw new ActionNotFoundException(e.tail().head());
            AlternatingSequence<S,A> newState = e.tail().tail();
            if(!post(ts,e.head(),e.tail().head()).contains(newState.head()))
                return false;
            e = newState;
        }
        if(!states.contains(e.head()))
            throw new StateNotFoundException(e.head());
        return true;
    }

    @Override
    public <S, A, P> boolean isInitialExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        if(ts.getStates().contains(e.head()))
            return ts.getInitialStates().contains(e.head());
        throw new StateNotFoundException(e.head());
    }

    @Override
    public <S, A, P> boolean isMaximalExecutionFragment(TransitionSystem<S, A, P> ts, AlternatingSequence<S, A> e) {
        while(e.tail().size()!=0) {
            e = e.tail().tail();
        }
        return isStateTerminal(ts,e.head());
    }

    @Override
    public <S, A> boolean isStateTerminal(TransitionSystem<S, A, ?> ts, S s) {
        if(ts.getStates().contains(s))
            return post(ts,s).size()==0;
        throw new StateNotFoundException(s);
    }

    @Override
    public <S> Set<S> post(TransitionSystem<S, ?, ?> ts, S s) {
        return ts.getTransitions().parallelStream().filter(transition -> transition.getFrom().equals(s))
                .map(Transition::getTo).collect(Collectors.toSet());
    }

    @Override
    public <S> Set<S> post(TransitionSystem<S, ?, ?> ts, Set<S> c) {
        return ts.getTransitions().parallelStream().filter(transition -> c.contains(transition.getFrom()))
                .map(Transition::getTo).collect(Collectors.toSet());
    }

    @Override
    public <S, A> Set<S> post(TransitionSystem<S, A, ?> ts, S s, A a) {
        return ts.getTransitions().parallelStream().filter(transition -> transition.getFrom().equals(s) && transition.getAction().equals(a))
                .map(Transition::getTo).collect(Collectors.toSet());
    }

    @Override
    public <S, A> Set<S> post(TransitionSystem<S, A, ?> ts, Set<S> c, A a) {
        return ts.getTransitions().parallelStream().filter(transition -> c.contains(transition.getFrom()) && transition.getAction().equals(a))
                .map(Transition::getTo).collect(Collectors.toSet());
    }

    @Override
    public <S> Set<S> pre(TransitionSystem<S, ?, ?> ts, S s) {
        return ts.getTransitions().parallelStream().filter(transition -> transition.getTo().equals(s))
                .map(Transition::getFrom).collect(Collectors.toSet());
    }

    @Override
    public <S> Set<S> pre(TransitionSystem<S, ?, ?> ts, Set<S> c) {
        return ts.getTransitions().parallelStream().filter(transition -> c.contains(transition.getTo()))
                .map(Transition::getFrom).collect(Collectors.toSet());
    }

    @Override
    public <S, A> Set<S> pre(TransitionSystem<S, A, ?> ts, S s, A a) {
        return ts.getTransitions().parallelStream().filter(transition -> transition.getTo().equals(s) && transition.getAction().equals(a))
                .map(Transition::getFrom).collect(Collectors.toSet());
    }

    @Override
    public <S, A> Set<S> pre(TransitionSystem<S, A, ?> ts, Set<S> c, A a) {
        return ts.getTransitions().parallelStream().filter(transition -> c.contains(transition.getTo()) && transition.getAction().equals(a))
                .map(Transition::getFrom).collect(Collectors.toSet());
    }

    @Override
    public <S, A> Set<S> reach(TransitionSystem<S, A, ?> ts) {
        HashSet<S> reachable = new HashSet<>();
        for(S s:ts.getInitialStates())
            travelStates(ts,s,reachable);
        return reachable;
    }

    private <S, A> void travelStates(TransitionSystem<S, A, ?> ts, S s, HashSet<S> reachable)
    {
        if(!reachable.contains(s))
        {
            reachable.add(s);
            for(S state:post(ts,s))
                travelStates(ts,state,reachable);
        }
    }

    private <S1, S2, A, P> Set<Pair<S1,S2>> initializeTs(TransitionSystem<Pair<S1,S2>,A,P> ts, TransitionSystem<S1, A, P> ts1, TransitionSystem<S2, A, P> ts2, Set<A> handShakingActions)
    {
        Set<S1> ts1States = ts1.getStates();
        Set<S2> ts2States = ts2.getStates();

        Set<A> actionsProduct = new HashSet<>(ts1.getActions());
        actionsProduct.addAll(ts2.getActions());
        ts.addAllActions(actionsProduct);

        Set<P> apProduct = new HashSet<>(ts1.getAtomicPropositions());
        apProduct.addAll(ts2.getAtomicPropositions());
        ts.addAllAtomicPropositions(apProduct);

        Set<Pair<S1,S2>> statesProduct = new HashSet<>();
        for(S1 s1:ts1States)
            for(S2 s2:ts2States) {
                statesProduct.add(new Pair<>(s1,s2));
            }

        Set<S1> initials1 = ts1.getInitialStates();
        Set<S2> initials2 = ts2.getInitialStates();
        Set<Pair<S1,S2>> initialProduct = statesProduct.parallelStream().filter(product -> initials1.contains(product.first) && initials2.contains(product.second))
                .collect(Collectors.toSet());

        Set<Transition<S1,A>> transitions1 = ts1.getTransitions();
        Set<Transition<S2,A>> transitions2 = ts2.getTransitions();
        Map<S1,Set<A>> s2a1 = transitions1.parallelStream().collect(Collectors.toMap(Transition<S1,A>::getFrom,
                transition -> new HashSet<A>(Collections.singleton(transition.getAction())),
                (x,y) -> {x.addAll(y); return x;}));
        Map<S2,Set<A>> s2a2 = transitions2.parallelStream().collect(Collectors.toMap(Transition<S2,A>::getFrom,
                transition -> new HashSet<A>(Collections.singleton(transition.getAction())),
                (x,y) -> {x.addAll(y); return x;}));
        Map<Pair<S1,A>,Set<S1>> sa2s1 = transitions1.parallelStream().collect(Collectors.toMap(t -> new Pair<>(t.getFrom(),t.getAction()),
                t -> new HashSet<S1>(Collections.singleton(t.getTo())),
                (x,y) -> {x.addAll(y); return x;}));
        Map<Pair<S2,A>,Set<S2>> sa2s2 = transitions2.parallelStream().collect(Collectors.toMap(t -> new Pair<>(t.getFrom(),t.getAction()),
                t -> new HashSet<S2>(Collections.singleton(t.getTo())),
                (x,y) -> {x.addAll(y); return x;}));
        for(Pair<S1,S2> s:initialProduct) {
            ts.addState(s);
            ts.addInitialState(s);
            travelTS(ts,s,s2a1,s2a2,sa2s1,sa2s2,handShakingActions, new HashSet<>());
        }


        for(Pair<S1,S2> states:statesProduct) {
            HashSet<P> ap = new HashSet<>(ts1.getLabel(states.first));
            ap.addAll(ts2.getLabel(states.second));
            for(P p:ap)
                ts.addToLabel(states,p);
        }
        return statesProduct;
    }

    @Override
    public <S1, S2, A, P> TransitionSystem<Pair<S1, S2>, A, P> interleave(TransitionSystem<S1, A, P> ts1, TransitionSystem<S2, A, P> ts2) {
        TransitionSystem<Pair<S1, S2>, A, P> ts = new TransitionSystemImpl<>();
        Set<Pair<S1,S2>> statesProduct = initializeTs(ts,ts1,ts2, null);
        return ts;

    }
    private <T> Set<T> addSets(Set<T> s1, Set<T> s2) {
        Set<T> s3 = new HashSet<>(s1);
        s3.addAll(s2);
        return s3;
    }
    @Override
    public <S1, S2, A, P> TransitionSystem<Pair<S1, S2>, A, P> interleave(TransitionSystem<S1, A, P> ts1, TransitionSystem<S2, A, P> ts2, Set<A> handShakingActions) {
        TransitionSystem<Pair<S1, S2>, A, P> ts = new TransitionSystemImpl<>();
        Set<Pair<S1,S2>> statesProduct = initializeTs(ts,ts1,ts2,handShakingActions);
        return ts;
    }

    private <S1,S2,A,P> void travelTS(TransitionSystem<Pair<S1, S2>, A, P> ts, Pair<S1,S2> states, Map<S1,Set<A>> s2a1,
                                      Map<S2,Set<A>> s2a2, Map<Pair<S1,A>,Set<S1>> sa2s1, Map<Pair<S2,A>,Set<S2>> sa2s2,
                                      Set<A> handShakingActions, Set<Pair<S1,S2>> visited) {
        Pair<S1,S2> next;
        if(visited.contains(states))
            return;
        visited.add(states);
        boolean isHandshakeAction;
        if(s2a1.containsKey(states.first)) {
            for (A action : s2a1.get(states.first)) {
                isHandshakeAction = handShakingActions != null && handShakingActions.contains(action);

                if (isHandshakeAction && s2a2.containsKey(states.second) && s2a2.get(states.second).contains(action)) {
                    for (S1 to1 : sa2s1.get(new Pair<S1, A>(states.first, action))) {
                        for (S2 to2 : sa2s2.get(new Pair<S2, A>(states.second, action))) {
                            next = new Pair<>(to1, to2);
                            ts.addState(next);
                            ts.addTransition(new Transition<>(states, action, next));
                            travelTS(ts, next, s2a1, s2a2, sa2s1, sa2s2, handShakingActions,visited);
                        }
                    }
                } else if (!isHandshakeAction) {
                    for (S1 to : sa2s1.get(new Pair<S1, A>(states.first, action))) {
                        next = new Pair<>(to, states.second);
                        ts.addState(next);
                        ts.addTransition(new Transition<>(states, action, next));
                        travelTS(ts, next, s2a1, s2a2, sa2s1, sa2s2, handShakingActions,visited);
                    }
                }
            }
        }
        if(s2a2.containsKey(states.second)) {
            for (A action : s2a2.get(states.second)) {
                isHandshakeAction = handShakingActions != null && handShakingActions.contains(action);
                if (!isHandshakeAction) {
                    for (S2 to : sa2s2.get(new Pair<S2, A>(states.second, action))) {
                        next = new Pair<>(states.first,to);
                        ts.addState(next);
                        ts.addTransition(new Transition<>(states, action, next));
                        travelTS(ts, next, s2a1, s2a2, sa2s1, sa2s2, handShakingActions,visited);
                    }
                }
            }
        }

    }

    @Override
    public <L, A> ProgramGraph<L, A> createProgramGraph() {
        return new ProgramGraphImpl<>();
    }

    @Override
    public <L1, L2, A> ProgramGraph<Pair<L1, L2>, A> interleave(ProgramGraph<L1, A> pg1, ProgramGraph<L2, A> pg2) {
        ProgramGraph<Pair<L1, L2>, A> pg = new ProgramGraphImpl<>();
        for(L1 l1:pg1.getLocations()) {
            for(L2 l2:pg2.getLocations()) {
                pg.addLocation(new Pair<L1,L2>(l1,l2));
            }
        }

        for(L1 l1:pg1.getInitialLocations()) {
            for(L2 l2:pg2.getInitialLocations()) {
                pg.addInitialLocation(new Pair<L1,L2>(l1,l2));
            }
        }

        Set<Pair<L1,L2>> locProduct = pg.getLocations();
        for(PGTransition<L1,A> t1:pg1.getTransitions()) {
            Set<Pair<L1,L2>> locsForT = locProduct.parallelStream().filter(pair -> pair.first.equals(t1.getFrom())).collect(Collectors.toSet());
            for(Pair<L1,L2> l:locsForT) {
                pg.addTransition(new PGTransition<Pair<L1,L2>,A>(l, t1.getCondition(),t1.getAction(),new Pair<L1,L2>(t1.getTo(),l.second)));
            }
        }
        for(PGTransition<L2,A> t2:pg2.getTransitions()) {
            Set<Pair<L1,L2>> locsForT = locProduct.parallelStream().filter(pair -> pair.second.equals(t2.getFrom())).collect(Collectors.toSet());
            for(Pair<L1,L2> l:locsForT) {
                pg.addTransition(new PGTransition<Pair<L1,L2>,A>(l, t2.getCondition(),t2.getAction(),new Pair<L1,L2>(l.first,t2.getTo())));
            }
        }

        for(List<String> l1:pg1.getInitalizations()) {
            for(List<String> l2:pg2.getInitalizations()) {
                List<String> l = new Vector<>(l1);
                l.addAll(l2);
                pg.addInitalization(l);
            }
        }
        return pg;
    }


    private void generateBinary(List<List<Boolean>> l, List<Boolean> input, int index) {
        if(index>=input.size()) {
            l.add(input);
            return;
        }

        List<Boolean> newInput = new ArrayList<>(input);
        newInput.set(index,FALSE);
        generateBinary(l, newInput, index+1);
        newInput = new ArrayList<>(input);
        newInput.set(index,TRUE);
        generateBinary(l, newInput, index+1);
    }

    private List<Map<String,Boolean>> generateInputs(List<String> inputNames) {
        List<Boolean> initialInput = inputNames.parallelStream().map(s -> FALSE).collect(Collectors.toList());
        List<List<Boolean>> l = new ArrayList<>();
        generateBinary(l,initialInput,0); // l contains all binary combinations
        List<Map<String,Boolean>> listOfMaps = new ArrayList<>();
        for(List<Boolean> input:l) {
            Map<String,Boolean> map = new HashMap<>();
            for(int i=0; i<input.size(); i++) {
                map.put(inputNames.get(i),input.get(i));
            }
            listOfMaps.add(map);
        }
        return listOfMaps;
    }

    private void visitCircuit(TransitionSystem<Pair<Map<String, Boolean>, Map<String, Boolean>>, Map<String, Boolean>, Object> ts,
                              Circuit c, Map<String, Boolean> inputs, Map<String, Boolean> registers,
                              List<Map<String,Boolean>> allInputs) {
        Pair<Map<String,Boolean>,Map<String,Boolean>> p1 = new Pair<>(inputs,registers);

        if(ts.getStates().contains(p1))
            return;

        ts.addState(p1);
        for(String s:inputs.entrySet().parallelStream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList())) {
            ts.addAtomicProposition(s);
            ts.addToLabel(p1,s);
        }
        for(String s:registers.entrySet().parallelStream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList())) {
            ts.addAtomicProposition(s);
            ts.addToLabel(p1,s);
        }
        for(String s:c.computeOutputs(inputs,registers).entrySet().parallelStream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList())) {
            ts.addAtomicProposition(s);
            ts.addToLabel(p1,s);
        }

        for(Map<String,Boolean> newInput:allInputs) {
            ts.addAction(newInput);
            Map<String,Boolean> newRegister = c.updateRegisters(inputs,registers);
            visitCircuit(ts,c,newInput,newRegister,allInputs);
            Pair<Map<String,Boolean>,Map<String,Boolean>> p2 = new Pair<>(newInput,newRegister);
            ts.addTransition(new Transition<>(p1,newInput,p2));
        }



    }
    @Override
    public TransitionSystem<Pair<Map<String, Boolean>, Map<String, Boolean>>, Map<String, Boolean>, Object> transitionSystemFromCircuit(Circuit c) {
        TransitionSystem<Pair<Map<String, Boolean>, Map<String, Boolean>>, Map<String, Boolean>, Object> ts = new TransitionSystemImpl<>();
        List<String> inputNames = new ArrayList<>(c.getInputPortNames());
        Set<String> registers = c.getRegisterNames();
        Map<String,Boolean> registerValues = new HashMap<>();

        for(String s:registers) {
            registerValues.put(s, FALSE);
        }

        List<Map<String,Boolean>> allInputs = generateInputs(inputNames);
        for(Map<String,Boolean> input:allInputs) {
            Pair<Map<String,Boolean>,Map<String,Boolean>> p = new Pair<>(input,registerValues);
            visitCircuit(ts,c,input,registerValues,allInputs);
            ts.addInitialState(p);
        }

        return ts;
    }

    private <L, A> void addStateToTs(TransitionSystem<Pair<L, Map<String, Object>>, A, String> ts, Pair<L,Map<String, Object>> p) {
        ts.addState(p);
        String str;
        str = p.first.toString();
        ts.addAtomicProposition(str);
        ts.addToLabel(p,str);
        for(Map.Entry<String,Object> e:p.second.entrySet()) {
            str = String.format("%s = %s",e.getKey(),e.getValue());
            ts.addAtomicProposition(str);
            ts.addToLabel(p,str);
        }
    }

    private <L,A> void visitState(TransitionSystem<Pair<L, Map<String, Object>>, A, String> ts, ProgramGraph<L, A> pg, Set<ActionDef> actionDefs, Set<ConditionDef> conditionDefs, Pair<L, Map<String, Object>> curr) {
        if(ts.getStates().contains(curr))
            return;
        addStateToTs(ts,curr);

        for(PGTransition<L,A> t1:pg.getTransitions().parallelStream().filter(t -> t.getFrom().equals(curr.first)).collect(Collectors.toSet())) {
            for(ConditionDef cd:conditionDefs) {
                if(cd.evaluate(curr.second,t1.getCondition())) {
                    List<ActionDef> a = actionDefs.parallelStream().filter(action -> action.isMatchingAction(t1.getAction())).collect(Collectors.toList());
                    Map<String,Object> map;
                    if(a.size()>0)
                        map = a.get(0).effect(curr.second,t1.getAction());
                    else
                        map = curr.second;
                    Pair<L,Map<String,Object>> p = new Pair<L,Map<String,Object>>(t1.getTo(),map);
                    visitState(ts,pg,actionDefs,conditionDefs,p);
                    ts.addAction(t1.getAction());
                    ts.addTransition(new Transition<>(curr,t1.getAction(),p));
                }
            }
        }
    }

    @Override
    public <L, A> TransitionSystem<Pair<L, Map<String, Object>>, A, String> transitionSystemFromProgramGraph(ProgramGraph<L, A> pg, Set<ActionDef> actionDefs, Set<ConditionDef> conditionDefs) {
        TransitionSystem<Pair<L, Map<String, Object>>, A, String> ts = new TransitionSystemImpl<>();

        for(L l:pg.getInitialLocations()) {
            for(List<String> lstr:pg.getInitalizations()) {
                Map<String,Object> newMap = new HashMap<>();
                for(String str:lstr) {
                    ActionDef a = actionDefs.stream().filter(action -> action.isMatchingAction(str)).collect(Collectors.toList()).get(0);
                    newMap = a.effect(newMap,str);
                }
                Pair<L,Map<String, Object>> p = new Pair<>(l,newMap);
                visitState(ts,pg,actionDefs,conditionDefs,p);
                ts.addInitialState(p);
            }
        }

        return ts;
    }

    @Override
    public <L, A> TransitionSystem<Pair<List<L>, Map<String, Object>>, A, String> transitionSystemFromChannelSystem(ChannelSystem<L, A> cs) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement product
    }

    @Override
    public <Sts, Saut, A, P> TransitionSystem<Pair<Sts, Saut>, A, Saut> product(TransitionSystem<Sts, A, P> ts, Automaton<Saut, P> aut) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement product
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromela(String filename) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement programGraphFromNanoPromela
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromelaString(String nanopromela) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement programGraphFromNanoPromelaString
    }

    @Override
    public ProgramGraph<String, String> programGraphFromNanoPromela(InputStream inputStream) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement programGraphFromNanoPromela
    }

    @Override
    public <S, A, P, Saut> VerificationResult<S> verifyAnOmegaRegularProperty(TransitionSystem<S, A, P> ts, Automaton<Saut, P> aut) {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: Implement verifyAnOmegaRegularProperty
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
