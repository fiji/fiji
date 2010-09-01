package fiji.plugin.nperry.tracking;

import mpicbg.imglib.algorithm.Algorithm;

public interface CostMatrixCreator extends Algorithm {

	public double[][] getCostMatrix();
	
}
