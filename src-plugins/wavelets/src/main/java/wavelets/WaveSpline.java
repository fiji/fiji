package wavelets;

/**
 * This class make a Spline wavelets transformation and its inverse.
 * 
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

public class WaveSpline {

	/**
	* Perform an wavelet transformation of the ImageObject 
	* calling this method with n scale. The size of image should 
	* be a interger factor of 2 at the power n.
	* The input is an image.
	* The result is the wavelet coefficients. It is put in the ImageObject 
	* calling this method. 
	*
	* @param in		an ImageAcess object provided by ImageJ
	* @param n  	a integer value giving the number of scale
	*/
	static public ImageAccess analysis(ImageAccess in, int order, int n) {
		
		// Compute the size to the fine and coarse levels
		int nxfine = in.getWidth();
		int nyfine = in.getHeight();

		// Declare the object image
		ImageAccess sub;
		
		// Initialization
		int nx = nxfine;
		int ny = nyfine;
		ImageAccess out = in.duplicate();

		// From fine to coarse main loop
		for ( int i=0; i<n; i++) {
		
			// Create a new image array of size [nx,ny]
			sub = new ImageAccess(nx, ny);

			// Copy in[] into image[]
			out.getSubImage(0,0,sub);
			
			// Apply the Wavelet splitting
			sub = split(sub, order);
			
			// Put the result image[] into in[]
			out.putSubImage(0,0,sub);
			
			// Reduce the size by a factor of 2
			nx = nx / 2;
			ny = ny / 2;
		}
		return out;
	}

	/**
	* Perform 1 iteration of the wavelet transformation of an ImageObject.
	* The algorithm use the separability of the wavelet transformation.
	* The result of the computation is put in the ImageObject calling
	* this method. 
	*
	* @param in		an ImageAcess object provided by ImageJ
	*/
	static private ImageAccess split(ImageAccess in, int order) {
		int nx = in.getWidth();
		int ny = in.getHeight();
		ImageAccess out = new ImageAccess(nx, ny);

		WaveSplineFilter wf = new WaveSplineFilter(order);
			
		if (nx >= 1 ) {
			double rowin[]  = new double[nx];
			double rowout[] = new double[nx];
			for (int y=0; y<ny; y++) {
				in.getRow(y, rowin);
				split_1D(rowin, rowout, wf.h, wf.g);
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
				split_1D(colin, colout, wf.h, wf.g);
				out.putColumn(x,colout);
			}
		}	

		return out;
	}

	/**
	* Perform 1 iteration of the wavelet transformation of a 1D vector
	* using the spline wavelet transformation.
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
		
		/////////////////////////////////////////////
		// Order is 0 -> Haar Transform
		/////////////////////////////////////////////
		if ((nh<=1) || (ng<=1)) {	
			double sqrt2 = Math.sqrt(2);	
			int j;
			for(int i=0; i<n2; i++) {
				j=2*i;
				vout[i]    = (vin[j]+vin[j+1]) / sqrt2;
				vout[i+n2] = (vin[j]-vin[j+1]) / sqrt2;
			}
			return;
		}
		
		/////////////////////////////////////////////
		// Order is higher than 0 
		/////////////////////////////////////////////
		double 	pix;
		int 	j, k, j1, j2;
		int 	period = 2 * n - 2;	// period for mirror boundary conditions

		for (int i=0;i<n2;i++) {
			j=i*2;
			pix = vin[j] * h[0];
			for (k=1;k<nh;k++) {					// Low pass part
				j1 = j - k;
				if (j1<0) {							// Mirror conditions
					while (j1<0) j1 += period;		// Periodize	
					if (j1 >= n) j1 = period - j1;	// Symmetrize
				}
				j2 = j + k;
				if (j2>=n) {						// Mirror conditions			
					while (j2>=n) j2 -= period;		// Periodize	
					if (j2 < 0) j2 = -j2;			// Symmetrize
				}
				pix = pix + h[k]*(vin[j1]+vin[j2]);
			}
			vout[i] = pix;

			j=j+1;
			pix = vin[j] * g[0];					// High pass part
			for (k=1;k<ng;k++) {
				j1 = j - k;
				if (j1<0) {							// Mirror conditions
					while (j1<0) j1 += period;		// Periodize	
					if (j1 >= n) j1 = period - j1;	// Symmetrize
				}
				j2 = j + k;
				if (j2>=n) {						// Mirror conditions			
					while (j2>=n) j2 -= period;		// Periodize	
					if (j2 < 0) j2 = -j2;			// Symmetrize
				}
				pix = pix + g[k]*(vin[j1]+vin[j2]);
			}
			vout[i+n2] = pix;
		}
	}

	/**
	* Perform an inverse wavelet transformation of the ImageObject 
	* calling this method with n scale. The size of image should 
	* be a interger factor of 2 at the power n.
	* The input is the results of a wavelet transformation.
	* The result is the reconstruction. It is put in the ImageObject 
	* calling this method. 
	*
	* @param in		an ImageAcess object provided by ImageJ
	* @param n  	a integer value giving the number of scale
	*/

	static public ImageAccess synthesis(ImageAccess in, int order, int n) {
		// Compute the size to the fine and coarse levels
		int div = (int)Math.pow(2.0, (double)(n-1));
		int nxcoarse = in.getWidth() / div;
		int nycoarse = in.getHeight() / div;

		// Declare the object image
		ImageAccess sub;

		// Initialisazion
		int nx = nxcoarse;
		int ny = nycoarse;
		ImageAccess out = in.duplicate();
		
		// From fine to coarse main loop
		for ( int i=0; i<n; i++) {
		
			// Create a new image array of size [nx,ny]
			sub = new ImageAccess(nx, ny);
			
			// Copy in[] into image[]
			out.getSubImage(0,0, sub);
			
			// Apply the Wavelet splitting
			sub = merge(sub, order);
			
			// Put the result image[] into in[]
			out.putSubImage(0,0, sub);
			// Reduce the size by a factor of 2
			nx = nx * 2;
			ny = ny * 2;
		}
		return out;
	}

	/**
	* Perform 1 iteration of the inverse wavelet transformation of an 
	* ImageObject.
	* The algorithm use the separability of the wavelet transformation.
	* The result of the computation is put in the ImageObject calling
	* this method. 
	*
	* @param in		an ImageAcess object provided by ImageJ
	*/
	static private ImageAccess merge(ImageAccess in, int order) {
		int nx = in.getWidth();
		int ny = in.getHeight();
		ImageAccess out = new ImageAccess(nx, ny);
		WaveSplineFilter wf = new WaveSplineFilter(order);

		if (nx >= 1 ) {
			double rowin[]  = new double[nx];
			double rowout[] = new double[nx];
			for (int y=0; y<ny; y++) {
				in.getRow(y, rowin);
				merge_1D(rowin, rowout, wf.h, wf.g);
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
				merge_1D(colin, colout, wf.h, wf.g);
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

		/////////////////////////////////////////////
		// Order is 0 -> Haar Transform
		/////////////////////////////////////////////
		if ((nh<=1) || (ng<=1)) {			
			double sqrt2 = Math.sqrt(2);	
			for (int i=0; i<n2; i++) {
				vout[2*i]  = (vin[i] + vin[i+n2])/sqrt2;
				vout[2*i+1]= (vin[i] - vin[i+n2])/sqrt2;
			}
			return;
		}
		
		/////////////////////////////////////////////
		// Order is higher than 0 
		/////////////////////////////////////////////
		double	pix1, pix2;
		int	j, k, kk, i1, i2;
		int k01 = (nh/2)*2-1;
		int k02 = (ng/2)*2-1;
		
		int period = 2*n2 - 1;	// period for mirror boundary conditions
		
		for (int i=0; i<n2; i++)	{
			j = 2*i;
			pix1 = h[0] * vin[i];
			for(k=2; k<nh; k+=2) {
				i1 = i-(k/2);
				if (i1<0) {
					i1 = (-i1) % period;
					if (i1>=n2) i1 = period-i1;
				}
				i2 = i+(k/2);
				if (i2>n2-1) {
					i2 = i2 % period;
					if (i2>=n2) i2 = period-i2;
				}
				pix1 = pix1 + h[k]*(vin[i1]+vin[i2]);
			}

			pix2=0.;
			for(k=-k02; k<ng; k+=2) {
				kk = Math.abs(k);
				i1 = i+(k-1)/2;
				if (i1<0) {					
					i1 = (-i1-1) % period;
					if (i1>=n2) i1 = period-1-i1;
				}
				if (i1>=n2) {				
					i1 = i1 % period;
					if (i1>=n2) i1 = period-1-i1;
				} 
				pix2 = pix2 + g[kk]*vin[i1+n2];
			}

			vout[j] = pix1 + pix2;

			j = j+1;
			pix1 = 0.;
			for (k=-k01; k<nh; k+=2) {
				kk = Math.abs(k);
				i1=i+(k+1)/2;
				if (i1<0) {
					i1 = (-i1) % period;
					if (i1>=n2) i1 = period-i1;
				}
				if (i1>=n2) { 
					i1 = (i1) % period;
					if (i1>=n2) i1 = period-i1;
				}
				pix1 = pix1 + h[kk]*vin[i1];
			}
			pix2 = g[0] * vin[i+n2];
			for (k=2; k<ng; k+=2) {
				i1 = i-(k/2);
				if (i1<0) { 
					i1 = (-i1-1) % period;
					if (i1>=n2) i1 = period-1-i1;
				}
				i2 = i+(k/2);
				if (i2>n2-1) { 
					i2 = i2 % period;
					if (i2>=n2) i2 = period-1-i2;
				}
				pix2 = pix2 + g[k]*(vin[i1+n2]+vin[i2+n2]);
			}
			vout[j] = pix1 + pix2;
		}
	}

}
