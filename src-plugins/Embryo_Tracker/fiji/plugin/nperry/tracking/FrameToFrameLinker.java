package fiji.plugin.nperry.tracking;

import fiji.plugin.nperry.hungarian.AssignmentProblem;
import fiji.plugin.nperry.hungarian.HungarianAlgorithm;
import mpicbg.imglib.algorithm.Algorithm;

public class FrameToFrameLinker implements Algorithm {

	/*
	 * (non-Javadoc)
	 * @see mpicbg.imglib.algorithm.Algorithm#checkInput()
	 */
	
	@Override
	public boolean checkInput() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see mpicbg.imglib.algorithm.Algorithm#getErrorMessage()
	 */
	
	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see mpicbg.imglib.algorithm.Algorithm#process()
	 */
	
	@Override
	public boolean process() {
		// TODO Auto-generated method stub
		return false;
	}
	
	// testing
	public static void main(String[] args) {
		float[][] costs = new float[][] {{1,Float.POSITIVE_INFINITY},{2,Float.POSITIVE_INFINITY}};
		AssignmentProblem prob = new AssignmentProblem(costs);
		HungarianAlgorithm hung = new HungarianAlgorithm();
		int[][] results = prob.solve(hung);
		System.out.println("results length: " + results.length);
		for (int[] row : results) {
			System.out.println("row length: " + row.length);
			System.out.println(String.format("{ %d, %d}", row[0], row[1]));
			//System.out.println(row.toString());
		}
	}
}
