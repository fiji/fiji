
public class ColorKLT {

	double[] mean;
	private int kltLength;

	KLT klt;

	public ColorKLT(double[][] p) {
		
		kltLength = 3; 
		
		mean = new double[kltLength];

		klt = new KLT(KLT.normalize(p, mean));

	}

	public ColorKLT(int[] pixels) {
		kltLength = 3; 
		
		mean = new double[kltLength];

		double[][] p = pixels2double(pixels);
		
		klt = new KLT(KLT.normalize(p, mean));
		
	}

	private double[][] pixels2double(int[] pixels) {
		int length = pixels.length;
		
		double[][] p = new double[length][3];
		
		for (int i=0; i<length; i++) {
				int c = pixels[i];
				double[] pp = p[i]; 
				pp[0] = (c & 0x00ff0000)>>16;
				pp[1] = (c & 0x0000ff00)>>8;
				pp[2] = (c & 0x000000ff);
		}
		return p;
	}
	
	
	private final double[] einheitsVektor(int i) {
		double[] out = new double[kltLength];

		out[i] = mean[i];

		return out;
	}

	public double[][] getEigenVectors() {
		double[][] evecs = new double[kltLength][kltLength];

		for(int i = 0; i < kltLength; i++)
			evecs[i] = KLT.denormal(klt.inverseTransform(einheitsVektor(i)), mean);

		return evecs;
	}

	public double[][] standardToKlt(double[][] p) {
		return transform(p, false, null);
	}
	
	public double[][] standardToKlt(int[] pixels) {
		double[][] p = pixels2double(pixels);
		return transform(p, false, null);
	}

	private double[][] transform(double[][] in, boolean inverse, double[] weights)  {

		int numberOfVectors = in.length;
		
		double[][] out = new double[numberOfVectors][kltLength];

		if (!inverse)
			klt.transform(in, out, mean);
				
		else
			if(weights == null)
				klt.inverseTransform(in, out, mean);			
			else
				klt.inverseTransform(in, out, mean, weights);
				
		return out; 
	}

	public double[][] kltToStandard(double[][] p) {
		return transform(p, true, null);
	}

	public double[][] kltToStandard(double[][] p, double[] weights) {

		return transform(p, true, weights);
	}
}

