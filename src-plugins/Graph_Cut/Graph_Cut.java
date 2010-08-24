
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.plugin.PlugIn;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

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


/**
 * Plugin interface to the graph cut algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Graph_Cut<T extends RealType<T>> implements PlugIn {

	// the image to process
	private Image<T> image;

	// the segmentation image
	private Image<T> segmentation;

	// image dimensions
	private int[]    dimensions;

	// the graph cut implementation
	private GraphCut graphCut;

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

		// read image
		ImagePlus imp = WindowManager.getCurrentImage();
		image        = ImagePlusAdapter.wrap(imp);

		// initialise graph cut
		init();

		// process
		processSingleChannelImage(128.0f);

		// create segmentation image
		int[] segDimensions = new int[3];
		segDimensions[0] = 0;
		segDimensions[1] = 0;
		segDimensions[2] = 0;
		for (int d = 0; d < dimensions.length; d++)
			segDimensions[d] = dimensions[d];

		ImagePlus seg = IJ.createImage("GraphCut segmentation", "8-bit",
		                               segDimensions[0], segDimensions[1], segDimensions[2]);
		segmentation = ImagePlusAdapter.wrap(seg);

		LocalizableByDimCursor<T> cursor = segmentation.createLocalizableByDimCursor();
		int[] imagePosition = new int[dimensions.length];
		while (cursor.hasNext()) {

			cursor.fwd();

			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition);

			if (graphCut.getTerminal(nodeNum) == Terminal.FOREGROUND)
				cursor.getType().setReal(255.0);
			else
				cursor.getType().setReal(0.0);
		}

		seg.show();
	}

	private void init() {

		int   numNodes   = image.size();
		dimensions       = image.getDimensions();

		int numEdges = 0;

		for (int d = 0; d < dimensions.length; d++)
			numEdges += numNodes - numNodes/dimensions[d];

		IJ.log("Creating graph structure of " + numNodes + " nodes and " + numEdges + " edges...");
		graphCut = new GraphCut(numNodes, numEdges);
		IJ.log("...done.");
	}


	/**
	 * Processes a single channel image.
	 *
	 * The intensities of the image are interpreted as the probability of each
	 * pixel to belong to the foreground. The potts weight represents an
	 * isotropic edge weight.
	 *
	 * @param pottsWeight Isotropic edge weights.
	 */
	private void processSingleChannelImage(float pottsWeight) {

		LocalizableByDimCursor<T> cursor = image.createLocalizableByDimCursor();
		int[] imagePosition              = new int[dimensions.length];

		// set terminal weights, i.e., segmentation probabilities
		IJ.log("Setting terminal weights...");
		while (cursor.hasNext()) {

			cursor.fwd();
			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition);
			
			T type = cursor.getType();
			float value = type.getRealFloat();

			graphCut.setTerminalWeights(nodeNum, value, 255.0f - value);
		}
		IJ.log("...done.");

		// set edge weights
		IJ.log("Setting edge weights...");
		cursor   = image.createLocalizableByDimCursor();
		int[] neighborPosition = new int[dimensions.length];
		int e = 0;
		while (cursor.hasNext()) {

			cursor.fwd();

			// image position
			cursor.getPosition(imagePosition);
			int nodeNum = listPosition(imagePosition);

			neighborPosition = imagePosition;

			for (int d = 0; d < dimensions.length; d++) {

				neighborPosition[d] -= 1;

				if (neighborPosition[d] >= 0) {

					int neighborNum = listPosition(neighborPosition);
					graphCut.setEdgeWeight(nodeNum, neighborNum, pottsWeight);
					e++;
				}
				neighborPosition[d] += 1;
			}
		}
		IJ.log("...done inserting " + e + " edges.");

		// calculate max flow
		IJ.log("Calculating max flow...");
		float maxFlow = graphCut.computeMaximumFlow(false, null);
		IJ.log("...done. Max flow is " + maxFlow);
	}

	private int listPosition(int[] imagePosition) {

		int pos = 0;
		int fac = 1;
		for (int d = 0; d < dimensions.length; d++) {
			pos += fac*imagePosition[d];
			fac *= dimensions[d];
		}
		return pos;
	}

}
