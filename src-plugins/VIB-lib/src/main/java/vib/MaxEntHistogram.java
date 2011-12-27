package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

public class MaxEntHistogram {
	private final static boolean debug=false;
	private long[] values; // the histogram
	private long total;
	public MaxEntHistogram(ImagePlus image) {
		values=new long[256];
		ImageStack stack=image.getStack();
		for(int i=1;i<=stack.getSize();i++)
			initHistogram((ByteProcessor)stack.getProcessor(i));
		total=stack.getWidth()*stack.getHeight()*stack.getSize();
	}

	public MaxEntHistogram(int[][] histograms) {
		values = new long[256];
		total = 0;
		for (int i = 0; i < 256; i++)
			for (int j = 0; j < histograms.length; j++) {
				values[i] += histograms[j][i];
				total += values[i];
			}
	}

	private void initHistogram(ByteProcessor image) {
		byte[] pixels=(byte[])image.getPixels();
		int w=image.getWidth(),h=image.getHeight();
		for(int i=0;i<w*h;i++) {
			int value=pixels[i];
			if(value<0)
				value+=256;
			values[value]++;
		}
	}

	/** calculates the mean from a bin combining the
	    old bins from start to end-1 */
	private int getMean(int start,int end) {
		long count=0,count2=0;
		for(int i=start;i<end;i++) {
				count+=i*values[i];
				count2+=values[i];
		}
		if(count2==0)
				return (end-start)/2;
		return (int)(count/count2);
	}
	/** calculates the entropy summand from a bin combining the
	    old bins from start to end-1 */
	private double getPartialEntropy(int start,int end) {
		long count=0;
		for(int i=start;i<end;i++)
			count+=values[i];
		if(count==0)
			return 0;
		return -Math.log(count/(double)total)*count/total;
	}
	/** quantizes according to maximum entropy principle,
	    imitating MaxEnt's dynamic programming
	    algorithm from ciq.ps.gz; returns the new right bin
	    boundaries (exclusive, so result[k-1]==256).
	    returns the entropy of the result */
	public double quantize(int K) {
		int[][][] boundaries=new int[K][256][K];
		double[][] entropies=new double[K][256];

		for(int k=1;k<=K;k++) {
if(debug)
IJ.write("loop: k="+k+", K="+K);
			// optimization
			int startX=(k<K?k:256);
			int endX=256-K+k;
			for(int x=startX;x<=endX;x++) {
				if(k==1) {
					entropies[0][x-1]=getPartialEntropy(0,x);
					boundaries[0][x-1][0]=x;
				} else {
					int bestIndex=-1;
					double bestEntropy=-Double.MAX_VALUE;
					for(int xLeft=k-1;xLeft<x;xLeft++) {
						double entropy=entropies[k-2][xLeft-1]+getPartialEntropy(xLeft,x);
						if(entropy>bestEntropy) {
							bestIndex=xLeft;
							bestEntropy=entropy;
						}
					}
					entropies[k-1][x-1]=bestEntropy;
					System.arraycopy(boundaries[k-2][bestIndex-1],0,boundaries[k-1][x-1],0,k-1);
					boundaries[k-1][x-1][k-1]=x;
if(debug) {
IJ.write("k="+k+", x="+x+", entropy="+bestEntropy+", bestIndex="+bestIndex);
String tmp="\t";
for(int kk=0;kk<k;kk++)
	tmp+=boundaries[k-1][x-1][kk]+" ";
IJ.write(tmp);
}
				}
				IJ.showProgress(k/(double)K);
			}
		}
if(debug)
IJ.write("K="+K+"\n");
		this.boundaries=boundaries[K-1][256-1];
		return entropies[K-1][256-1];
	}
	/** use naive approach: every bin should have the same amount of
	    data points. */
	public void quantizeNaive(int K) {
		boundaries = new int[K];
		long total = 0;
		for(int i=0;i<values.length;i++)
			total+=values[i];
		long cumul=0;
		int index=0;
		for(int i=0;i<K-1;i++) {
			for(;index<values.length-(K-1-i) && cumul<total*(i+1)/K;index++)
				cumul+=values[index];
			boundaries[i]=index;
System.err.println(i+": "+index);
		}
		boundaries[K-1]=values.length;
System.err.println((K-1)+": "+boundaries[K-1]);
	}

	int[] boundaries;

	/** after quantization, returns a mapping from old pixel values
	    to new pixel values. */
	public byte[] getMapping(boolean showIndex) {
		byte[] result=new byte[256];

		int oldX=0;
		for(int i=0;i<boundaries.length;i++) {
			int value=(showIndex?i:getMean(oldX,boundaries[i]));
			for(int j=oldX;j<boundaries[i];j++) {
				if(debug)
					System.err.println(j+":"+value);
				result[j]=(byte)value;
			}
			oldX=boundaries[i];
		}

		return result;
	}

	public int[][] get(int[][] histos) {
		int[][] result = new int[histos.length][boundaries.length];
		for (int j = 0; j < boundaries.length; j++)
			for (int i = 0; i < boundaries[j]; i++)
				for (int k = 0; k < histos.length; k++)
					result[k][j] += histos[k][i];
		return result;
	}
}

