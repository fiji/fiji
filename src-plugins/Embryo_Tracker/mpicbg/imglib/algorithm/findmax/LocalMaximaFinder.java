package mpicbg.imglib.algorithm.findmax;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.Algorithm;

public interface LocalMaximaFinder extends Algorithm {
	public ArrayList< double[] > getLocalMaxima();
}
