/**
 * Copyright (c) 2014,
 *       Charles Prud'homme (TASC, INRIA Rennes, LINA CNRS UMR 6241),
 *       Jean-Guillaume Fages (COSLING S.A.S.).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chocosolver.samples.explanation;

import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.explanations.ExplanationFactory;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

/**
 * Created by IntelliJ IDEA.
 * User: njussien
 * Date: 01/05/11
 * Time: 13:26
 */
public class ExplainedSimpleProblem extends AbstractProblem {

    IntVar[] vars;
    int n = 4;
    int vals = n + 1;

    @Override
    public void createSolver() {
        solver = new Solver();
    }


    @Override
    public void buildModel() {
        vars = VariableFactory.enumeratedArray("x", n, 1, vals, solver);
        for (int i = 0; i < vars.length - 1; i++) {
            solver.post(IntConstraintFactory.arithm(vars[i], ">", vars[i + 1]));
        }
    }

    @Override
    public void configureSearch() {
        solver.set(IntStrategyFactory.minDom_LB(vars));
    }

    @Override
    public void solve() {
        ExplanationFactory.CBJ.plugin(solver, false);
        if (solver.findSolution()) {
            do {
                this.prettyOut();
            }
            while (solver.nextSolution());
        }
    }

    @Override
    public void prettyOut() {

        for (IntVar v : vars) {
//            System.out.println("* variable " + v);
            for (int i = 1; i <= vals; i++) {
                if (!v.contains(i)) {
                    System.out.println(v.getName() + " != " + i + " because " + solver.getExplainer().retrieve(v, i));
                }
            }
        }
    }

    public static void main(String[] args) {
        new ExplainedSimpleProblem().execute(args);
    }
}