package adt;

/**
 * Class used to track statistics with low memory footprint where large number data sets are expected
 * <p/>
 * User: Tom Larkworthy
 * Date: 12-Jul-2006
 * Time: 15:25:56
 */
public class PixelStats {

	int n = 0;

	long pixelOccurences[] = new long[256];



	int seqLength = 0;
	double seqSum = 0;
	double seqSumSquared = 0;

	int seqN = 0;

	public void addData(byte val) {
		pixelOccurences[val&0xFF]++;
		n++;
		seqLength++;
	}

	public void endOfSequence(){
    	seqSum += seqLength;
		seqSumSquared += seqLength * seqLength;
		seqLength = 0;
		seqN++;
	}

	public int getN(){
		return n;
	}

	public double getMeanSequenceLength(){
		return seqSum / seqN;
	}

	public double getVarianceSequenceLength(){
		return seqSumSquared / seqN - getMeanSequenceLength() * getMeanSequenceLength();
	}

	/**
	 * prob
	 * @param pixel
	 * @return
	 */
	public double getProb(byte pixel){
    	return (double)pixelOccurences[pixel&0xFF] / n;
	}

	public static void main(String[] args) {

	}
}
