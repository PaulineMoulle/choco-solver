package solver.variables.graph;

import solver.ICause;
import solver.exception.ContradictionException;

/**An interface for graph variable manipulation in constraint programming
 * @author Jean-Guillaume Fages, Xavier Lorca
 *
 */
public interface IVariableGraph {

    /**
     * Remove node x from the maximal partial subgraph
     * @param x node's index
     * @param cause algorithm which is related to the removal
     * @return true iff the removal has an effect
     */
    boolean removeNode(int x, ICause cause) throws ContradictionException;
    
    /**
     * Enforce the node x to belong to any partial subgraph
     * @param x node's index
     * @param cause algorithm which is related to the modification
     * @return true iff the node is effectively added to the mandatory structure
     */
    boolean enforceNode(int x, ICause cause) throws ContradictionException;

    /**
     * Remove node y from the neighborhood of node x from the maximal partial subgraph
     * @param x node's index
     * @param y node's index
     * @param cause algorithm which is related to the removal
     * @return true iff the removal has an effect
     * @throws ContradictionException 
     */
    boolean removeArc(int x, int y, ICause cause) throws ContradictionException;

    /**
     * Enforce the node y into the neighborhood of node x in any partial subgraph
     * @param x node's index
     * @param y node's index
     * @param cause algorithm which is related to the removal
     * @return true iff the node y is effectively added in the neighborhooh of node x
     */
    boolean enforceArc(int x, int y, ICause cause) throws ContradictionException;


    /**
     * Compute the order of the graph in its current state (ie the number of nodes that may belong to an instantiation)
     * @return the number of nodes that may belong to an instantiation
     */
    int getEnvelopOrder();

    /**
     * Compute the order of the graph in its final state (ie the minimum number of nodes that necessarily belong to any instantiation)
     * @return the minimum number of nodes that necessarily belong to any instantiation
     */
    int getKernelOrder();

    /**
     * @return the graph representing the domain of the variable graph
     */
    IStoredGraph getKernelGraph();
    
    /**
     * @return the graph representing the instantiated values (nodes and edges) of the variable graph
     */
    IStoredGraph getEnvelopGraph();
}
