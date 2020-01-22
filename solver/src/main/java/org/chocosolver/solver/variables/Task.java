/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 04/02/13
 * Time: 15:48
 */

package org.chocosolver.solver.variables;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.learn.Implications;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.view.OffsetView;
import org.chocosolver.util.objects.ValueSortedMap;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSetUtils.unionOf;

/**
 * Container representing a task:
 * It ensures that: start + duration = end
 *
 * @author Jean-Guillaume Fages
 * @since 04/02/2013
 */
public class Task {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private IntVar start, duration, end;
    private IVariableMonitor<IntVar> update;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param model the Model of the variables
     * @param est earliest starting time
     * @param lst latest starting time
     * @param d duration
     * @param ect earliest completion time
     * @param lct latest completion time time
     */
    public Task(Model model, int est, int lst, int d, int ect, int lct) {
        start = model.intVar(est, lst);
        duration = model.intVar(d);
        if(ect == est+d && lct == lst+d) {
            end = start.getModel().intOffsetView(start, d);
        } else {
            end = model.intVar(ect, lct);
            declareMonitor();
        }
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     */
    public Task(IntVar s, int d) {
        start = s;
        duration = start.getModel().intVar(d);
        end = start.getModel().intOffsetView(start, d);
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end, end being an offset view of start + duration.
     *
     * @param s start variable
     * @param d duration value
     * @param e end variable
     */
    public Task(IntVar s, int d, IntVar e) {
        start = s;
        duration = start.getModel().intVar(d);
        end = e;
        if(!isOffsetView(s, d, e)) {
            declareMonitor();
        }
    }

    /**
     * Container representing a task:
     * It ensures that: start + duration = end
     *
     * @param s start variable
     * @param d duration variable
     * @param e end variable
     */
    public Task(IntVar s, IntVar d, IntVar e) {
        start = s;
        duration = d;
        end = e;
        if(!d.isInstantiated() || !isOffsetView(s, d.getValue(), e)) {
            declareMonitor();
        }
    }

    @Deprecated
    public Task(IntVar s, IntVar d, IntVar e, boolean declareMonitor) {
        start = s;
        duration = d;
        end = e;
        if(declareMonitor && (!d.isInstantiated() || !isOffsetView(s, d.getValue(), e))) {
            declareMonitor();
        }
    }

    private static boolean isOffsetView(IntVar s, int d, IntVar e) {
        if(e instanceof OffsetView) {
            OffsetView offsetView = (OffsetView) e;
            return offsetView.cste == d && offsetView.getVariable().equals(s);
        }
        return false;
    }

    private void declareMonitor() {
        if (start.hasEnumeratedDomain() || duration.hasEnumeratedDomain() || end.hasEnumeratedDomain()) {
            update = new TaskMonitor(start, duration, end, true);
        } else {
            update = new TaskMonitor(start, duration, end, false);
        }
        Model model = start.getModel();
        //noinspection unchecked
        ArrayList<Task> tset = (ArrayList<Task>) model.getHook(Model.TASK_SET_HOOK_NAME);
        if(tset == null){
            tset = new ArrayList<>();
            model.addHook(Model.TASK_SET_HOOK_NAME, tset);
        }
        tset.add(this);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    /**
     * Applies BC-filtering so that start + duration = end
     *
     * @throws ContradictionException thrown if a inconsistency has been detected between start, end and duration
     */
    public void ensureBoundConsistency() throws ContradictionException {
        update.onUpdate(start, IntEventType.REMOVE);
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    public IntVar getStart() {
        return start;
    }

    public IntVar getDuration() {
        return duration;
    }

    public IntVar getEnd() {
        return end;
    }

    public IVariableMonitor<IntVar> getMonitor() {
        return update;
    }

    private static void doExplain(IntVar S, IntVar D, IntVar E,
        ExplanationForSignedClause clause,
        ValueSortedMap<IntVar> front,
        Implications ig, int p) {
        IntVar pivot = ig.getIntVarAt(p);
        IntIterableRangeSet dom;
        dom = clause.getComplementSet(S);
        if (S == pivot) {
            unionOf(dom, ig.getDomainAt(p));
            clause.addLiteral(S, dom, true);
        } else {
            clause.addLiteral(S, dom, false);
        }
        dom = clause.getComplementSet(D);
        if (D == pivot) {
            unionOf(dom, ig.getDomainAt(p));
            clause.addLiteral(D, dom, true);
        } else {
            clause.addLiteral(D, dom, false);
        }
        dom = clause.getComplementSet(E);
        if (E == pivot) {
            unionOf(dom, ig.getDomainAt(p));
            clause.addLiteral(E, dom, true);
        } else {
            clause.addLiteral(E, dom, false);
        }
    }

    @Override
    public String toString() {
        return "Task[" +
            "start=" + start +
            ", duration=" + duration +
            ", end=" + end +
            ']';
    }

    private static class TaskMonitor implements IVariableMonitor<IntVar> {
        private final IntVar S, D, E;
        private final boolean isEnum;

        private TaskMonitor(IntVar S, IntVar D, IntVar E, boolean isEnum) {
            this.S = S;
            this.D = D;
            this.E = E;
            S.addMonitor(this);
            D.addMonitor(this);
            E.addMonitor(this);
            this.isEnum = isEnum;
        }

        @Override
        public void onUpdate(IntVar var, IEventType evt) throws ContradictionException {
            boolean fixpoint;
            do {
                // start
                fixpoint = S.updateBounds(E.getLB() - D.getUB(), E.getUB() - D.getLB(), this);
                // end
                fixpoint |= E.updateBounds(S.getLB() + D.getLB(), S.getUB() + D.getUB(), this);
                // duration
                fixpoint |= D.updateBounds(E.getLB() - S.getUB(), E.getUB() - S.getLB(), this);
            } while (fixpoint && isEnum);
        }

        @Override
        public void explain(ExplanationForSignedClause clause,
            ValueSortedMap<IntVar> front,
            Implications ig, int p) {
            doExplain(S, D, E, clause, front, ig, p);
        }

        @Override
        public void forEachIntVar(Consumer<IntVar> action) {
            action.accept(S);
            action.accept(D);
            action.accept(E);
        }

        @Override
        public String toString() {
            return "Task["+S.getName()+"+"+D.getName()+"="+E.getName()+"]";
        }
    }
}
