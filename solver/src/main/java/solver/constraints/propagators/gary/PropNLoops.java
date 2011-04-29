package solver.constraints.propagators.gary;

import java.util.LinkedList;

import choco.kernel.ESat;
import choco.kernel.common.util.procedure.IntProcedure;
import choco.kernel.memory.IEnvironment;
import choco.kernel.memory.IStateInt;
import solver.constraints.Constraint;
import solver.constraints.propagators.GraphPropagator;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IntVar;
import solver.variables.domain.delta.IntDelta;
import solver.variables.graph.IActiveNodes;
import solver.variables.Variable;
import solver.variables.graph.directedGraph.DirectedGraphVar;
import solver.variables.graph.graphStructure.iterators.ActiveNodesIterator;
import solver.views.GraphView;
import solver.views.IView;

/**
 * @author Jean-Guillaume Fages
 * Ensures that each node in the kernel has exactly NLOOPS loops
 *
 * @param <V>
 */
public class PropNLoops<V extends Variable> extends GraphPropagator<V>{

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	DirectedGraphVar g;
	IntVar nLoops;
	IntProcedure removeProc;
	IntProcedure enforceProc;
	IStateInt nbKerLoop;
	IStateInt nbEnvLoop;
	
	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNLoops(
			DirectedGraphVar graph, IntVar nL,
			IEnvironment environment,
			Constraint<V, Propagator<V>> constraint,
			PropagatorPriority priority, boolean reactOnPromotion) {
		super((V[]) new Variable[]{graph,nL}, environment, constraint, priority, reactOnPromotion);
		g = graph;
		nLoops = nL;
		removeProc = new RemProc(this);
		enforceProc = new EnfLoop(this);
		nbEnvLoop = environment.makeInt();
		nbKerLoop = environment.makeInt();
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate() throws ContradictionException {
		ActiveNodesIterator<IActiveNodes> nodeIter = g.getEnvelopGraph().activeNodesIterator();
		int node;
		int ker = 0;
		int env = 0;
		while(nodeIter.hasNext()){
			node = nodeIter.next();
			if (g.getEnvelopGraph().arcExists(node, node)){
				env++;
				if (g.getKernelGraph().arcExists(node, node)){
					ker++;
				}
			}			
		}
		nLoops.updateLowerBound(ker, this);
		nLoops.updateUpperBound(g.getEnvelopOrder(), this);
		nLoops.updateUpperBound(env, this);
		int added = 0;
		if(env==nLoops.getLB()){
			nodeIter = g.getEnvelopGraph().activeNodesIterator();
			while(nodeIter.hasNext()){
				node = nodeIter.next();
				if (g.getEnvelopGraph().arcExists(node, node)){
					g.enforceArc(node, node, this);
					added ++;
				}			
			}
		}
		nbEnvLoop.set(env);
		nbKerLoop.set(ker+added);
	}

	@Override
	public void propagateOnView(IView<V> view, int idxVarInProp, int mask) throws ContradictionException {
		if (view instanceof GraphView) {
			GraphView gv = (GraphView) view;
			if ((mask & EventType.REMOVEARC.mask) != 0){
				IntDelta d = (IntDelta) g.getDelta().getArcRemovalDelta();
				d.forEach(removeProc, gv.fromArcRemoval(), gv.toArcRemoval());
			}
			if ((mask & EventType.ENFORCEARC.mask) != 0){
				IntDelta d = (IntDelta) g.getDelta().getArcEnforcingDelta();
				int nbKer = nbKerLoop.get();
				d.forEach(enforceProc, gv.fromArcEnforcing(), gv.toArcEnforcing());
				if(nbKer<nbKerLoop.get()){
					nLoops.updateLowerBound(nbKerLoop.get(), this);//recently added
					checkAllLoopsFound();
				}
			}
			if ((mask & EventType.REMOVENODE.mask) != 0){
				nLoops.updateUpperBound(g.getEnvelopOrder(), this);
			}
		}
	}

	private void checkAllLoopsFound() throws ContradictionException {
		int loopsInKer = nbKerLoop.get();
		int loopsInEnv = nbEnvLoop.get();
		if(loopsInKer>nLoops.getUB()){
			ContradictionException.throwIt(this, g, "too many loops");
		}else{
			ActiveNodesIterator<IActiveNodes> nodeIter = g.getEnvelopGraph().activeNodesIterator();
			int node;
			LinkedList<Integer> loopOutOfKer = new LinkedList<Integer>();
			while(nodeIter.hasNext()){
				node = nodeIter.next();
				if (!g.getKernelGraph().arcExists(node, node)){
					if (g.getEnvelopGraph().arcExists(node, node)){
						loopOutOfKer.addFirst(node);
					}
				}
			}
			if(loopsInKer==nLoops.getUB()){
				for(int l:loopOutOfKer){
					g.removeArc(l, l, this);
				}
			}else{
				if (loopsInEnv==nLoops.getValue()){
					for(int l:loopOutOfKer){
						g.enforceArc(l, l, this);
					}
				}
			}
		}
	}

	@Override
	public int getPropagationConditions() {
		return EventType.REMOVEARC.mask+EventType.ENFORCEARC.mask+EventType.REMOVENODE.mask;
	}

	@Override
	public ESat isEntailed() {
		return ESat.UNDEFINED;
	}
	
	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	/**
	 * @author Jean-Guillaume Fages
	 * Checks if a loop has been removed
	 */
	private static class RemProc implements IntProcedure {

        private final PropNLoops p;

        public RemProc(PropNLoops p) {
            this.p = p;
        }

        @Override
        public void execute(int i) throws ContradictionException {
    		int n = p.g.getEnvelopGraph().getNbNodes();
        	if (i>=n){
        		int from = i/n-1;
        		int to   = i%n;
        		if (from == to){
        			p.nbEnvLoop.set(p.nbEnvLoop.get()-1);
        			int env = p.nbEnvLoop.get();
        			int ker = p.nbKerLoop.get();
        			ActiveNodesIterator<IActiveNodes> nodeIter;
        			p.nLoops.updateUpperBound(env, p);
        			p.nLoops.updateLowerBound(ker, p);
        			if(p.nLoops.getLB() == env && env>ker){
        				nodeIter = p.g.getEnvelopGraph().activeNodesIterator();
            			while(nodeIter.hasNext()){
            				from = nodeIter.next();
            				if (p.g.getEnvelopGraph().arcExists(from, from)){
            					p.g.enforceArc(from, from, p);
            				}
            			}
        			}
        		}
        	}else{
        		throw new UnsupportedOperationException();
        	}
        }
    }
	
	/**
	 * @author Jean-Guillaume Fages
	 * Checks if a loop has been enforced
	 */
	private static class EnfLoop implements IntProcedure {

        private final PropNLoops p;

        public EnfLoop(PropNLoops p) {
            this.p = p;
        }

        @Override
        public void execute(int i) throws ContradictionException {
    		int n = p.g.getEnvelopGraph().getNbNodes();
        	if (i>=n){
        		int from = i/n-1;
        		int to   = i%n;
        		if (from == to){
        			p.nbKerLoop.set(p.nbKerLoop.get()+1);
        		}
        	}else{
        		throw new UnsupportedOperationException();
        	}
        }
    }
}
