/*
 * Graph_Cut plugin
 *
 * This is the interface plugin to the graph cut algorithm for images as
 * proposed by Boykkov and Kolmogorov in:
 *
 *		"An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy
 *		Minimization in Vision."
 *		Yuri Boykov and Vladimir Kolmogorov
 *		In IEEE Transactions on Pattern Analysis and Machine
 *		Intelligence (PAMI),
 *		September 2004
 */

import ij.*;
import ij.ImagePlus.*;

import ij.plugin.PlugIn;

/**
 * Plugin interface to the graph cut algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Graph_Cut implements PlugIn {

	// test
	public static void main(String[] args) {

		GraphCut graph = new GraphCut(4, 4);

		graph.setTerminalWeights(0, 0.0f, 1.0f);
		graph.setTerminalWeights(1, 0.3f, 0.7f);
		graph.setTerminalWeights(2, 0.7f, 0.3f);
		graph.setTerminalWeights(3, 1.0f, 0.0f);

		graph.setEdgeWeight(0, 1, 0.0f);
		graph.setEdgeWeight(1, 2, 0.0f);
		graph.setEdgeWeight(2, 3, 0.0f);
		graph.setEdgeWeight(3, 0, 0.0f);

		float maxFlow = graph.computeMaximumFlow(false, null);

		System.out.println("max flow: " + maxFlow);
		for (int i = 0; i < 4; i++) {
			System.out.println("node " + i + ": " + graph.getTerminal(i));
		}
	}

	public void run(String arg) {
	}
}
