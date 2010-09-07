package fiji.plugin.spottracker.tracking;

import mpicbg.imglib.algorithm.Algorithm;

public interface CostMatrixCreator extends Algorithm {

	public double[][] getCostMatrix();
	
}
