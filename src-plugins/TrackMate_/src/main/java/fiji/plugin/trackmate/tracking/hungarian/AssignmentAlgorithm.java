package fiji.plugin.trackmate.tracking.hungarian;

/**
 * Just in case I want to try more than one implementation of the assignment algorithm.
 * Copyright 2007 Gary Baker (GPL v3)
 * @author Gary Baker
 */
public interface AssignmentAlgorithm {

	/**
	 * Solve this assignment problem for the given cost matrix. 
	 * <p>
	 * The solutions are returned as a 2D array of int. Each solution is an int array
	 * of 2 elements: 
	 * <ol>
	 * 	<li> the first int is the index of the source object (line index in the cost matrix)
	 * 	<li> the second int is the index of the target object (column index in the cost matrix)
	 * </ol>
	 * @param costMatrix  the cost matrix. It is modified heavily by implementation algorithms.
	 * @return an array of solutions, as arrays of 2 ints.
	 */
    public int[][] computeAssignments(double[][] costMatrix);

}
