package fiji.plugin.trackmate.tracking.hungarian;

/**
 * Just in case I want to try more than one implementation of the assignment algorithm.
 * Copyright 2007 Gary Baker (GPL v3)
 * @author gbaker
 */
public interface AssignmentAlgorithm {


    int[][] computeAssignments(double[][] costMatrix);

}
