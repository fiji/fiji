package wavelets;

/**
 * This class make the Complex wavelets transformation ans its inverse.
 * 
 * @author
 * <hr>
 * @author
 * <p style="background-color:#EEEEEE; border-top:1px solid #CCCCCC; border-bottom:1px solid #CCCCCC"">
 * Daniel Sage<br>
 * <a href="http://bigwww.epfl.ch">Biomedical Imaging Group</a> (BIG), 
 * Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland<br>
 * More information: http://bigwww.epfl.ch/</p>
 * <p>You'll be free to use this software for research purposes, but you should
 *  not redistribute it without our consent. In addition, we expect you to 
 *  include a citation or acknowledgement whenever you present or publish 
 *  results that are based on it.</p>
 *  
 *  @version
 *  11 July 2009
 */

 public class ComplexWavelet {
	
	/**
	 * This public method computes the complex wavelets transform of a given image
	 * and a given number of scale.
	 *
	 * @param in	input image
	 * @param n		number of scale
	 * @param length
	 * @return the wavelets coefficients
	 */
	 static public ImageAccess[] analysis(ImageAccess in, int n, int length) {
		
		// Compute the size to the fine and coarse levels
		int nxfine = in.getWidth();
		int nyfine = in.getHeight();
		
		// Declare the object image
		ImageAccess sub1;
		ImageAccess sub2;
		ImageAccess sub3;
		ImageAccess sub4;
		ImageAccess subre; 
		ImageAccess subim;
		ImageAccess outRe;
		ImageAccess outIm;	
		
		// Initialization
		int nx = nxfine;
		int ny = nyfine;
		outRe = in.duplicate();
		outIm = in.duplicate();
		
		int re = 0;
		int im = 1; 
		
		// From fine to coarse main loop
		// first iteration
		
		subre = new ImageAccess(nx, ny);
		sub1 = new ImageAccess(nx, ny);
		sub2 = new ImageAccess(nx, ny);
		
		
		// Copy in[] into image[]
		outRe.getSubImage(0,0,subre);
		
		
		// Apply the Wavelet splitting
		sub1 = split(subre, re, re, length);
		sub2 = split(subre, im, im, length);
		
		sub1.subtract(sub1, sub2);
		
		// Put the result image[] into in[]
		outRe.putSubImage(0,0,sub1);
		
		// Apply the Wavelet splitting
		sub1 = split(subre, re, im, length);
		sub2 = split(subre, im, re, length);
		
		sub1.add(sub1, sub2);
		
		outIm.putSubImage(0,0,sub1);
		
		// Reduce the size by a factor of 2
		nx = nx / 2;
		ny = ny / 2;
		
		for ( int i=1; i<n; i++) {
			
			// Create a new image array of size [nx,ny]
			subre = new ImageAccess(nx, ny);
			subim = new ImageAccess(nx, ny);
			sub1 = new ImageAccess(nx, ny);
			sub2 = new ImageAccess(nx, ny);
			sub3 = new ImageAccess(nx, ny);
			sub4 = new ImageAccess(nx, ny);
			
			
			// Copy in[] into image[]
			outRe.getSubImage(0,0,subre);
			outIm.getSubImage(0,0,subim);
			
			sub1 = split(subre, re, re, length);
			sub2 = split(subre, im, im, length);
			sub3 = split(subim, re, im, length);
			sub4 = split(subim, im, re, length);
			
			sub1.subtract(sub1, sub2);
			sub1.subtract(sub1, sub3);
			sub1.subtract(sub1, sub4);
			
			outRe.putSubImage(0,0,sub1);
			
			
			sub1 = split(subre, re, im, length);
			sub2 = split(subre, im, re, length);
			sub3 = split(subim, re, re, length);
			sub4 = split(subim, im, im, length);
			
			sub1.add(sub1, sub2);
			sub1.add(sub1, sub3);
			sub1.subtract(sub1, sub4);
			
			outIm.putSubImage(0,0,sub1);
			
			
			// Reduce the size by a factor of 2
			nx = nx / 2;
			ny = ny / 2;
		}
		ImageAccess[] outComplex = new ImageAccess[2];
		outComplex[0] = outRe.duplicate();
		outComplex[1]  = outIm.duplicate();
		return outComplex;
	}
	
	/**
	 * Perform 1 iteration of the wavelet transformation of an ImageObject.
	 * The algorithm use the separability of the wavelet transformation.
	 * The result of the computation is put in the ImageObject calling
	 * this method. 
	 *
	 * @param in		an ImageAcess object provided by ImageJ
	 */
	static private ImageAccess split(ImageAccess in, int type1, int type2, int length) {
		int nx = in.getWidth();
		int ny = in.getHeight();
		ImageAccess out = new ImageAccess(nx, ny);
		
		ComplexWaveFilter wf = new ComplexWaveFilter(length);
		
		if (nx >= 1 ) {
			double rowin[]  = new double[nx];
			double rowout[] = new double[nx];
			for (int y=0; y<ny; y++) {
				in.getRow(y, rowin);
				
				if (type1 == 0)
					split_1D(rowin, rowout, wf.h, wf.g);
				
				if (type1 == 1)
					split_1D(rowin, rowout, wf.hi, wf.gi);	
				
				out.putRow(y,rowout);
			}
		}
		else {
			//out.copy(in);
			out = in.duplicate();
		}
		
		if (ny > 1 ) {
			double colin[] = new double[ny];
			double colout[] = new double[ny];
			for (int x=0; x<nx; x++) {
				out.getColumn(x, colin);
				
				if (type2 == 0)
					split_1D(colin, colout, wf.h, wf.g);
				
				if (type2 == 1)
					split_1D(colin, colout, wf.hi, wf.gi);
				
				out.putColumn(x,colout);
			}
		}	
		
		return out;
	}
	
	/**
	 * Perform 1 iteration of the wavelet transformation of a 1D vector
	 * using the wavelet transformation.
	 * The output vector has the same size of the input vector and it 
	 * contains first the low pass part of the wavelet transform and then
	 * the high pass part of the wavelet transformation.
	 *
	 * @param vin	input, a double 1D vector
	 * @param vout	output, a double 1D vector
	 * @param h		input, a double 1D vector, lowpass filter
	 * @param g		input, a double 1D vector, highpass filter
	 */
	static private void split_1D(double vin[], double vout[], double h[],double g[]) {	  
		int n  = vin.length;
		int n2 = n / 2;
		int nh = h.length;
		int ng = g.length;
		
		double voutL[] = new double [n];
		double voutH[] = new double [n];
		double 	pix;
		int j1;
		
		for (int i=0; i<n; i++)	{
			pix = 0.0;
			for (int k=0;k<nh;k++) {					// Low pass part
				j1 = i + k - (nh/2);
				if (j1<0) {							// Periodic conditions
					while (j1<n) j1 = n+j1;
					j1 = (j1) % n;
				}
				if (j1>=n) {						// Periodic conditions			
					j1 = (j1) % n;
				}
				pix = pix + h[k]*vin[j1];
			}
			voutL[i] = pix;
		}
		
		for (int i=0; i<n; i++)	{
			pix = 0.0;
			for (int k=0; k<ng; k++) {					// Low pass part
				j1 = i + k - (ng/2);
				if (j1<0) {							// Periodic conditions
					while (j1<n) j1 = n+j1;
					j1 = (j1) % n;
				}
				if (j1>=n) {						// Periodic conditions			
					j1 = (j1) % n;
				}
				pix = pix + g[k]*vin[j1];
			}
			voutH[i] = pix;
		}	
		
		for (int k=0; k<n2; k++)
			vout[k] = voutL[2*k];
		for (int k=n2; k<n; k++)	
			vout[k] = voutH[2*k - n];
		
	}
	
	/**
	 * Perform an inverse wavelet transformation of the ImageObject 
	 * calling this method with n scale. The size of image should 
	 * be a interger factor of 2 at the power n.
	 * The input is the results of a wavelet transformation.
	 * The result is the reconstruction. It is put in the ImageObject 
	 * calling this method. 
	 *
	 * @param inRe		the real part of the wavelets coefficients
	 * @param inIm		the imaginary part of the wavelets coefficients
	 * @param n			a integer value giving the number of scale
	 * @param length  	
	 * @return the reconstructed image
	 */
	
	static public ImageAccess[] synthesis(ImageAccess inRe, ImageAccess inIm, int n, int length) {
		// Compute the size to the fine and coarse levels
		int div = (int)Math.pow(2.0, (double)(n-1));
		int nxcoarse = inRe.getWidth() / div;
		int nycoarse = inRe.getHeight() / div;
		
		// Declare the object image
		ImageAccess subre, subim, sub1, sub2, sub3, sub4;
		ImageAccess outRe;
		ImageAccess outIm;
		
		// Initialisazion
		int nx = nxcoarse;
		int ny = nycoarse;
		
		outRe = inRe.duplicate();
		outIm = inIm.duplicate();
		
		int re = 0;
		int im = 1;
		
		// From fine to coarse main loop
		for ( int i=0; i<n; i++) {
			// Create a new image array of size [nx,ny]
			subre = new ImageAccess(nx, ny);
			subim = new ImageAccess(nx, ny);
			sub1 = new ImageAccess(nx, ny);
			sub2 = new ImageAccess(nx, ny);
			sub3 = new ImageAccess(nx, ny);
			sub4 = new ImageAccess(nx, ny);
			// Copy in[] into image[]
			outRe.getSubImage(0,0, subre);
			outIm.getSubImage(0,0, subim);
			
			// Apply the Wavelet splitting
			sub1 = merge(subre, re, re, length);
			sub2 = merge(subre, im, im, length);
			sub3 = merge(subim, re, im, length);
			sub4 = merge(subim, im, re, length);
			
			sub1.subtract(sub1 ,sub2);
			sub1.add(sub1 ,sub3);
			sub1.add(sub1 ,sub4);
			
			outRe.putSubImage(0,0,sub1);
			
			// Apply the Wavelet splitting
			sub1 = merge(subre, re, im, length);
			sub2 = merge(subre, im, re, length);
			sub3 = merge(subim, re, re, length);
			sub4 = merge(subim, im, im, length);
			
			sub3.subtract(sub3 ,sub1);
			sub3.subtract(sub3 ,sub2);
			sub3.subtract(sub3 ,sub4);
			outIm.putSubImage(0,0,sub3);
			// Enlarge the size by a factor of 2
			nx = nx * 2;
			ny = ny * 2;
			
		}
		ImageAccess[] ReconstComplex = new ImageAccess[2];
		ReconstComplex[0] = outRe.duplicate();
		ReconstComplex[1]  = outIm.duplicate();
		return ReconstComplex;
	}
	
	/**
	 * Perform 1 iteration of the inverse wavelet transformation of an 
	 * ImageObject.
	 * The algorithm use the separability of the wavelet transformation.
	 * The result of the computation is put in the ImageAccess calling
	 * this method. 
	 *
	 * @param in		an ImageAcess object provided by ImageJ
	 * @param type1		
	 * @param type2		
	 * @param length
	 * @return merge		
	 */
	static private ImageAccess merge(ImageAccess in, int type1, int type2, int length) {
		int nx = in.getWidth();
		int ny = in.getHeight();
		ImageAccess out = new ImageAccess(nx, ny);
		ComplexWaveFilter wf = new ComplexWaveFilter(length);
		
		if (nx >= 1 ) {
			double rowin[]  = new double[nx];
			double rowout[] = new double[nx];
			for (int y=0; y<ny; y++) {
				in.getRow(y, rowin);
				
				if (type1 == 0)
					merge_1D(rowin, rowout, wf.h, wf.g);
				if (type1 == 1)	{
					merge_1D(rowin, rowout, wf.hi, wf.gi);
				}
				out.putRow(y,rowout);
			}
		}
		else {
			out = in.duplicate();
		}
		
		if (ny > 1 ) {
			double colin[] = new double[ny];
			double colout[] = new double[ny];
			for (int x=0; x<nx; x++) {
				out.getColumn(x, colin);
				
				if (type2 == 0)
					merge_1D(colin, colout, wf.h, wf.g);
				if (type2 == 1) {
					merge_1D(colin, colout, wf.hi, wf.gi);
				}
				out.putColumn(x,colout);
			}
		}
		return out;	
	}
	
	/**
	 * Perform 1 iteration of the inverse wavelet transformation of a 
	 * 1D vector using the Spline wavelet transformation.
	 * The output vector has the same size of the input vector and it 
	 * contains the reconstruction of the input signal. 
	 * The input vector constains first the low pass part of the wavelet 
	 * transform and then the high pass part of the wavelet transformation.
	 *
	 * @param vin	input, a double 1D vector
	 * @param vout	output, a double 1D vector
	 * @param h		input, a double 1D vector, lowpass filter
	 * @param g		input, a double 1D vector, highpass filter
	 */
	static private void merge_1D(double vin[], double vout[], double h[], double g[]) {	  
		int n  = vin.length;
		int n2 = n / 2;
		int nh = h.length;
		int ng = g.length;
		int j1;
		
		double pix;
		// Upsampling
		
		double vinL[] = new double [n];
		double vinH[] = new double [n];
		for (int k=0; k<n; k++)	{
			vinL[k]=0;
			vinH[k]=0;
		}
		
		for (int k=0; k<n2; k++)	{
			vinL[2*k] = vin[k];
			vinH[2*k] = vin[k + n2];
		}
		
		// filtering
		
		for (int i=0; i<n; i++)	{
			pix = 0.0;
			for (int k=0;k<nh;k++) {					// Low pass part
				j1 = i - k + (nh/2);
				if (j1<0) {							// Periodic conditions
					while (j1<n) j1 = n+j1;
					j1 = (j1) % n;
				}
				if (j1>=n) {						// Periodic conditions			
					j1 = (j1) % n;
				}
				pix = pix + h[k]*vinL[j1];
			}
			vout[i] = pix;
		}
		
		
		for (int i=0; i<n; i++)	{
			pix = 0.0;
			for (int k=0; k<ng; k++) {					// High pass part
				j1 = i - k + (ng/2);
				if (j1<0) {							// Periodic conditions
					while (j1<n) j1 = n+j1;
					j1 = (j1) % n;
				}
				if (j1>=n) {						// Periodic conditions			
					j1 = (j1) % n;
				}
				pix = pix + g[k]*vinH[j1];
			}
			vout[i] = vout[i] + pix;
		}	
	}
	
	/**
	 * This method computes the modulus from a real ImageAccess and a
	 * imaginary ImageAccess objects.
	 *
	 * @param inRe
	 * @param inIm
	 * @return modulus
	 */
	static public ImageAccess modulus(ImageAccess inRe, ImageAccess inIm) {
		int nx = inRe.getWidth();
		int ny = inRe.getHeight();
		double m,r,i;
		ImageAccess modulus = new ImageAccess(nx,ny);
		int x,y;
		for (x=0; x<nx; x++) {
			for (y=0; y<ny; y++) {
				r = inRe.getPixel(x,y);
				i = inIm.getPixel(x,y);
				m = Math.sqrt((r*r) + (i*i));		
				modulus.putPixel(x,y,m);
			}
		}
		return modulus;
	}
	
	
}
