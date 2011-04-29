/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
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
package solver.constraints.nary;

import choco.kernel.ESat;
import solver.Solver;
import solver.constraints.IntConstraint;
import solver.constraints.propagators.PropagatorPriority;
import solver.constraints.propagators.nary.sum.PropSumEq;
import solver.constraints.propagators.nary.sum.PropSumGeq;
import solver.constraints.propagators.nary.sum.PropSumLeq;
import solver.variables.IntVar;
import solver.variables.VariableFactory;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 18/03/11
 */
public class Sum extends IntConstraint {

    final int[] coeffs;
    final int b;
    final Type op;

    public enum Type {
        LEQ, GEQ, EQ
    }

    protected Sum(IntVar[] vars, int[] coeffs, Type type, int b, Solver solver) {
        super(vars, solver, PropagatorPriority.LINEAR);
        this.coeffs = coeffs.clone();
        this.b = b;
        this.op = type;
        int l = vars.length;
        IntVar[] x = new IntVar[l];
        int s = 0;
        for (int i = 0, e = l; i < l; i++) {
            if (coeffs[i] >= 0) {
                if (coeffs[i] != 1) {
                    x[s++] = VariableFactory.timesPosCste(vars[i], coeffs[i]);
                } else {
                    x[s++] = vars[i];
                }
            } else {
                if (coeffs[i] != -1) {
                    x[--e] = VariableFactory.timesPosCste(vars[i], -coeffs[i]);
                } else {
                    x[--e] = vars[i];
                }
            }
        }
        switch (type) {
            case LEQ:
                setPropagators(new PropSumLeq(x, s, b, solver.getEnvironment(), this));
                break;
            case GEQ:
                setPropagators(new PropSumGeq(x, s, b, solver.getEnvironment(), this));
                break;
            case EQ:
                setPropagators(new PropSumEq(x, s, b, solver.getEnvironment(), this));
                break;
        }

    }

    public static Sum leq(IntVar[] vars, int[] coeffs, int c, Solver solver) {
        return new Sum(vars, coeffs, Type.LEQ, c, solver);
    }

    public static Sum leq(IntVar[] vars, int[] coeffs, IntVar b, int c, Solver solver) {
        IntVar[] x = new IntVar[vars.length + 1];
        System.arraycopy(vars, 0, x, 0, vars.length);
        x[x.length - 1] = b;
        int[] cs = new int[coeffs.length + 1];
        System.arraycopy(coeffs, 0, cs, 0, coeffs.length);
        cs[cs.length - 1] = -c;
        return new Sum(x, cs, Type.LEQ, 0, solver);
    }

    public static Sum geq(IntVar[] vars, int[] coeffs, int c, Solver solver) {
        return new Sum(vars, coeffs, Type.GEQ, c, solver);
    }

    public static Sum geq(IntVar[] vars, int[] coeffs, IntVar b, int c, Solver solver) {
        IntVar[] x = new IntVar[vars.length + 1];
        System.arraycopy(vars, 0, x, 0, vars.length);
        x[x.length - 1] = b;
        int[] cs = new int[coeffs.length + 1];
        System.arraycopy(coeffs, 0, cs, 0, coeffs.length);
        cs[cs.length - 1] = -c;
        return new Sum(x, cs, Type.GEQ, 0, solver);
    }

    public static Sum eq(IntVar[] vars, int[] coeffs, int c, Solver solver) {
        return new Sum(vars, coeffs, Type.EQ, c, solver);
    }

    public static Sum eq(IntVar[] vars, int[] coeffs, IntVar b, int c, Solver solver) {
        IntVar[] x = new IntVar[vars.length + 1];
        System.arraycopy(vars, 0, x, 0, vars.length);
        x[x.length - 1] = b;
        int[] cs = new int[coeffs.length + 1];
        System.arraycopy(coeffs, 0, cs, 0, coeffs.length);
        cs[cs.length - 1] = -c;
        return new Sum(x, cs, Type.EQ, 0, solver);
    }

    @Override
    public ESat isSatisfied(int[] tuple) {
        int sum = 0;
        for (int i = 0; i < tuple.length; i++) {
            sum += coeffs[i] * tuple[i];
        }
        switch (op) {
            case EQ:
                return ESat.eval(sum == b);
            case GEQ:
                return ESat.eval(sum >= b);
            case LEQ:
                return ESat.eval(sum <= b);
        }
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        StringBuilder linComb = new StringBuilder(20);
        linComb.append(coeffs[0]).append('*').append(vars[0].getName());
        for (int i = 1; i < coeffs.length; i++) {
            linComb.append(coeffs[i]>=0?" +":" ").append(coeffs[i]).append('*').append(vars[i].getName());
        }
        switch (op) {
            case EQ:
                linComb.append(" = ");
                break;
            case GEQ:
                linComb.append(" >= ");
                break;
            case LEQ:
                linComb.append(" <= ");
                break;
        }
        linComb.append(b);
        return linComb.toString();
    }
}
