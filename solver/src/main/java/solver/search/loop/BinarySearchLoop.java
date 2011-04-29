/**
 *  Copyright (c) 2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.search.loop;

import choco.kernel.ESat;
import choco.kernel.common.util.tools.StringUtils;
import org.slf4j.LoggerFactory;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.exception.SolverException;
import solver.propagation.engines.IPropagationEngine;
import solver.search.limits.TimeCacheThread;
import solver.search.strategy.decision.Decision;
import solver.search.strategy.strategy.AbstractStrategy;

/**
 * This is the default implementation of {@link AbstractSearchLoop} abstract class.
 *
 * @author Xavier Lorca
 * @author Charles Prud'homme
 * @version 0.01, june 2010
 * @since 0.01
 */
public class BinarySearchLoop extends AbstractSearchLoop {

    Decision decision;

    /* Define the state to move to once a solution is found : UP_BRANCH or RESTART */
    static final int stateAfterSolution = UP_BRANCH;

    @SuppressWarnings({"unchecked"})
    BinarySearchLoop(Solver solver, IPropagationEngine pilotPropag, AbstractStrategy strategy) {
        super(solver, pilotPropag, strategy);
        if (LoggerFactory.getLogger(SearchLayout.class).isInfoEnabled()) {
            this.searchLayout = new BSDefaultLayout(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void initialPropagation() {
        this.env.worldPush();
        try {
            pilotPropag.initialPropagation();
        } catch (ContradictionException e) {
            this.env.worldPop();
            solver.setFeasible(Boolean.FALSE);
            pilotPropag.flushAll();
            interrupt();
        }
        measures.setInitialPropagationTimeCount(TimeCacheThread.currentTimeMillis - startingTime);
        updateTimeCount();
        // call to HeuristicVal.update(Action.initial_propagation)
        strategy.init();
        moveTo(OPEN_NODE);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Computes the next decision to apply, move to the next step in the run loop.
     * If there are no next decision, the current state is considered as a solution.
     * Once the solution has been stored -- and checked--, the next step in the tree search is computed: continue or stop.
     */
    @Override
    protected void openNode() {
        Decision tmp = decision;
        updateTimeCount();
        decision = strategy.getDecision();
        if (decision != null) {
            decision.setPrevious(tmp);
            moveTo(DOWN_LEFT_BRANCH);
        } else {
            recordSolution();
            decision = tmp;
        }
    }

    protected void recordSolution() {
        //todo: checker d'�tat
        measures.incSolutionCount(1);
        solver.setFeasible(Boolean.TRUE);
        solutionpool.recordSolution(solver);
        objectivemanager.update();
        searchLayout.onSolution();
        assert (ESat.TRUE.equals(solver.isSatisfied()));
        if (stopAtFirstSolution) {
            moveTo(RESUME);
            interrupt();
        } else {
            moveTo(stateAfterSolution);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean resume() {
        if (nextState == INIT) {
            throw new SolverException("the search loop has not been initialized.\n " +
                    "This appears when 'nextSolution' is called before 'findSolution'.");
        } else if (nextState != RESUME) {
            throw new SolverException("The search cannot be resumed. \n" +
                    "Be sure you are respecting one of these call configurations :\n " +
                    "\tfindSolution ( nextSolution )* | findAllSolutions | findOptimalSolution\n");
        }
        previousSolutionCount = measures.getSolutionCount();
        moveTo(stateAfterSolution);
        return this.loop();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Backs up the current state.
     * Then gets the current computed decision,applies it to the problem and calls the main propagation.
     */
    @Override
    protected void downLeftBranch() {
        searchLayout.onLeftBranch();
        downBranch();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Backs up the current state.
     * Then gets the current computed decision,applies it to the problem and calls the main propagation.
     */
    @Override
    protected void downRightBranch() {
        searchLayout.onRightBranch();
        downBranch();
    }

    private void downBranch() {
        env.worldPush();
        try {
            decision.buildNext();
            objectivemanager.apply(decision);

            pilotPropag.fixPoint();
            moveTo(OPEN_NODE);
        } catch (ContradictionException e) {
            measures.incFailCount(1);
            pilotPropag.flushAll();
            moveTo(UP_BRANCH);
        }
    }


    /**
     * {@inheritDoc}
     * Rolls back the previous state.
     * Then, if it goes back to the base world, stop the search.
     * Otherwise, gets the oppposite decision, applies it and calls the propagation.
     */
    @Override
    protected void upBranch() {
        env.worldPop();
        if (env.getWorldIndex() == baseWorld) {
            // The entire tree search has been explored, the search cannot be followed
            interrupt();
        } else {
            if (decision.hasNext()) {
                moveTo(DOWN_RIGHT_BRANCH);
            } else {
                decision.free();
                decision = decision.getPrevious();
                measures.incFailCount(1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void restartSearch() {
        throw new UnsupportedOperationException();
    }

    /**
     * Required method to be sure a restart is taken into account.
     * Because, restart limit checker are threads, si they can interrupt the search loop at any moment.
     * And the interruption must not be forget and replaced by the wrong next state.
     * <br/>
     * <b>Beware, if this method is called from RESTART case, it leads to an infinite loop!</b>
     *
     * @param to
     */
    void moveTo(int to) {
        if ((nextState & RESTART) == 0) {
            nextState = to;
        }
    }

    protected static final class BSDefaultLayout extends SearchLayout<BinarySearchLoop> {

        public BSDefaultLayout(BinarySearchLoop searchLoop) {
            super(searchLoop);
        }

        @Override
        protected void onOpenNode() {
        }

        @Override
        protected void onLeftBranch() {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{}[L]{} //{}", new Object[]{
                        StringUtils.pad("", searchLoop.env.getWorldIndex(), "."),
                        searchLoop.decision.toString(), print(searchLoop.strategy.vars)});
            }
        }

        @Override
        protected void onRightBranch() {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{}[R]{} //{}", new Object[]{
                        StringUtils.pad("", searchLoop.env.getWorldIndex(), "."),
                        searchLoop.decision.toString(), print(searchLoop.strategy.vars)});
            }
        }

        @Override
        protected void onSolution() {
            if (LOGGER.isDebugEnabled()) {
                searchLoop.updateTimeCount();
                searchLoop.updatePropagationCount();
                LOGGER.debug("- Solution #{} found. {} \n\t{}.",
                        new Object[]{searchLoop.getMeasures().getSolutionCount(),
                                searchLoop.getMeasures().toOneLineString(),
                                print(searchLoop.strategy.vars)}
                );
            }
        }

        @Override
        protected void onClose() {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(searchLoop.measures.toString());
            }
        }
    }

}
