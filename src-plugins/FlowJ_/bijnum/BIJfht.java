package bijnum;
import java.awt.Rectangle;
import ij.*;

/**
 * This class implements the fast Hartley transform on matrices and vectors (real float matrices).
 * It also includes some utility functions for dealing with Hartley transforms,
 * especially for correlation.
 * This class is based on Arlso Reeves'
 * Pascal implementation of the Fast Hartley Transform from NIH Image
 * (http://rsb.info.nih.gov/ij/docs/ImageFFT/).
 * The Fast Hartley Transform was restricted by U.S. Patent No. 4,646,256, but was placed
 * in the public domain by Stanford University in 1995 and is now freely available.
 * This implementation based in part on the implementation by Wyane Rasband in ImageJ.
 * Copyright (c) 1999-2004, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * This source code, and any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJfht
{
	/** Static variables used in BIJfht algorithms. */
	protected float [] C;
	protected float [] S;
	protected int [] bitrev;
	protected int maxN;
	protected float[] tempArr;

	/**
	 * Set up a BIJfht for matrices of size length = maxNxmaxN
	 * @param length: the square of the size of the matrices (the length of the vector used for storing the matrix).
	 */
	public BIJfht(int length)
	{
		this.maxN = (int) Math.sqrt(length);
		makeSinCosTables(maxN);
		makeBitReverseTable(maxN);
		tempArr = new float[maxN];
	}
	/**
	* Can be used to show progress in other programs.
	*/
	protected void progress(double percent)
	{
		IJ.showProgress(percent);
	}

	/**
	* Perform a Fast Hartley Transform on an image BIJfht of maxN x maxN pixels.
	* If inverse == true, do the inverse transform. m is modified!
	* @param m the matrix of the image, an array of floats
	* @param maxN the size of the matrix
	* @param inverse a flag whether to perform the forward or inverse transform.
	* @return m which is changed!
	*/
	public void compute(float [] m, boolean inverse)
	throws IllegalArgumentException
	{
		if (maxN != (int) Math.sqrt(m.length))
		        throw new IllegalArgumentException("BIJfht.compute(): matrix not square");
                else
		        rc2DFHT(m, inverse, maxN);
	}
	/**
	* Compute the conjugate product R = IM1* IM2 for real matrices (Hartley transformed) im1 and im2.
	* im1 and im2 are not modified.
	* To go from Hartley to Fourier:
	* F(x,y) = Re { F(x,y) } + j Im { F(x,y) }
	*        = He(x,y) - j Ho(x,y)
	*        = (H(x,y) + H(-x,-y))/2 - j (H(x,y) - H(-x, -y))/2
	* This is the best function for obtaining the translation from im1 to im2.
	* @param im1 a real Hartley transform of an image
	* @param im2 a real Hartley transform of another image
	* @param maxN the size of im1 and im2 in pixels. im1 and im2 should be maxN x maxN.
	* @return the resulting matrix with the conjugate product of the two transforms.
	*/
	public float [] crossPowerSpectrum(float [] im1, float [] im2)
	{
		float [] r = new float[im1.length];
		for (int row = 0; row < maxN; row++)
		{
			int rowM = (maxN - row) % maxN;
			for (int col = 0; col < maxN; col++)
			{
				int colM = (maxN - col) % maxN;
				float H2e = (im2[row * maxN + col] + im2[rowM * maxN + colM]) / 2.0f;
				float H2o = (im2[row * maxN + col] - im2[rowM * maxN + colM]) / 2.0f;
				r[row * maxN + col] = im1[row * maxN + col] * H2e
				        - im1[rowM * maxN + colM] * H2o;
			}
		}
		return r;
	}
	/**
	* Alternative method to compute the cross power spectrum (im1* im2) / | im1 im2 |
	* for Hartley (as opposed to Fourier) opposed matrices im1 and im2.
	* Conversion between Hartley and Fourier:
	* F(x,y) = Re { F(x,y) } + j Im { F(x,y) }
	*        = He(x,y) - j Ho(x,y)
	*        = (H(x,y) + H(-x,-y))/2 - j (H(x,y) - H(-x, -y))/2
	* To go from Fourier to Hartley
	*       H(x,y) = He = Re - Im and H(-x,-y) = Ho = Re + Im;
	*/
	public float [] crossPowerSpectrum2(float [] im1, float [] im2, int maxN)
	{
		float [] real = new float[im1.length];
		float [] imag = new float[im1.length];
		for (int row = 0; row < maxN; row++)
		{
			int rowM = (maxN - row) % maxN;
			for (int col = 0; col < maxN; col++)
			{
				int colM = (maxN - col) % maxN;
				// Re part of im1.
				double im1Re = (im1[row * maxN + col] + im1[rowM * maxN + colM]) / 2.0;
				// Im part of im1.
				double im1Im = - (im1[row * maxN + col] - im1[rowM * maxN + colM]) / 2.0;
				// Re part of im2.
				double im2Re = (im2[row * maxN + col] + im2[rowM * maxN + colM]) / 2.0;
				// Im part of im2.
				double im2Im = - (im2[row * maxN + col] - im2[rowM * maxN + colM]) / 2.0;
				double rcps = im1Re * im2Re + im1Im * im2Im;
				double icps = im1Im * im2Re - im2Im * im1Re;
				double mag = Math.sqrt(rcps * rcps + icps * icps);
				if (mag == 0)
				        { rcps = 0; icps = 0; }
				else
				        { rcps /= mag; icps = mag; }
				// Now you have real and imag parts for the cross power spectrum.
				real[row * maxN + col] = (float) rcps;
				imag[row * maxN + col] = (float) icps;
			}
		}
		float [] h = new float[im1.length];
		// Translate the crosspower spectrum back so it will fit in a Hartley transform.
		for (int row = 0; row < maxN; row++)
		{
			int rowM = (maxN - row) % maxN;
			for (int col = 0; col < maxN; col++)
			{
				int colM = (maxN - col) % maxN;
				float He = real[row * maxN + col] - imag[row * maxN + col];
				float Ho = real[row * maxN + col] + imag[row * maxN + col];
				h[row * maxN + col] = He;
				h[rowM * maxN + colM] = Ho;
			}
		}
		return h;
	}
	/**
	* Flip quadrants 1 and 3 and quadrants 2 and 4 of an image column vector.
	* 2 1
	* 3 4
	* @param m an image vector.
	* @return the flipped image vector.
	*/
 	public float [] flipquad(float [] m)
	{
		float [] t = new float[m.length];
		int halfwidth = maxN/2;
		// Copy 4 to t.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			t[x+y*maxN] = m[x+halfwidth+(y+halfwidth)*maxN];
		// Copy 2 to 4.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			m[x+halfwidth+(y+halfwidth)*maxN] = m[x+y*maxN];
		// Copy t to 2.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			m[x+y*maxN] = t[x+y*maxN];
		// Copy 3 to t.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			t[x+halfwidth+y*maxN] = m[x+(y+halfwidth)*maxN];
		// Copy 1 to 3.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			m[x+(y+halfwidth)*maxN] = m[x+halfwidth+y*maxN];
 		// Copy t to 1.
		for (int x = 0; x < halfwidth; x++)
		for (int y = 0; y < halfwidth; y++)
			m[x+halfwidth+y*maxN] = t[x+halfwidth+y*maxN];
		return m;
	}
	protected void makeSinCosTables(int maxN)
	{
		int n = maxN/4;
		C = new float[n];
		S = new float[n];
		double theta = 0.0;
		double dTheta = 2.0 * Math.PI/maxN;
		for (int i=0; i<n; i++)
		{
			C[i] = (float)Math.cos(theta);
			S[i] = (float)Math.sin(theta);
			theta += dTheta;
		}
	}
	protected void makeBitReverseTable(int maxN)
	{
		bitrev = new int[maxN];
		int nLog2 = log2(maxN);
		for (int i=0; i<maxN; i++)
			bitrev[i] = bitRevX(i, nLog2);
		maxN = maxN;
	}

        /** Row-column Fast Hartley Transform */
        void rc2DFHT(float[] x, boolean inverse, int maxN) {
                //IJ.write("FFT: rc2DFHT (row-column Fast Hartley Transform)");
                for (int row=0; row<maxN; row++)
                        dfht3(x, row*maxN, inverse, maxN);
                progress(0.4);
                transposeR(x, maxN);
                progress(0.5);
                for (int row=0; row<maxN; row++)
                        dfht3(x, row*maxN, inverse, maxN);
                progress(0.7);
                transposeR(x, maxN);
                progress(0.8);

                int mRow, mCol;
                float A,B,C,D,E;
                for (int row=0; row<maxN/2; row++) { // Now calculate actual Hartley transform
                        for (int col=0; col<maxN/2; col++) {
                                mRow = (maxN - row) % maxN;
                                mCol = (maxN - col)  % maxN;
                                A = x[row * maxN + col];	//  see Bracewell, 'Fast 2D Hartley Transf.' IEEE Procs. 9/86
                                B = x[mRow * maxN + col];
                                C = x[row * maxN + mCol];
                                D = x[mRow * maxN + mCol];
                                E = ((A + D) - (B + C)) / 2;
                                x[row * maxN + col] = A - E;
                                x[mRow * maxN + col] = B + E;
                                x[row * maxN + mCol] = C + E;
                                x[mRow * maxN + mCol] = D - E;
                        }
                }
                progress(0.95);
        }

        /* An optimized real FHT */
        void dfht3 (float[] x, int base, boolean inverse, int maxN) {
                int i, stage, gpNum, gpIndex, gpSize, numGps, Nlog2;
                int bfNum, numBfs;
                int Ad0, Ad1, Ad2, Ad3, Ad4, CSAd;
                float rt1, rt2, rt3, rt4;

                Nlog2 = log2(maxN);
                BitRevRArr(x, base, Nlog2, maxN);	//bitReverse the input array
                gpSize = 2;     //first & second stages - do radix 4 butterflies once thru
                numGps = maxN / 4;
                for (gpNum=0; gpNum<numGps; gpNum++)  {
                        Ad1 = gpNum * 4;
                        Ad2 = Ad1 + 1;
                        Ad3 = Ad1 + gpSize;
                        Ad4 = Ad2 + gpSize;
                        rt1 = x[base+Ad1] + x[base+Ad2];   // a + b
                        rt2 = x[base+Ad1] - x[base+Ad2];   // a - b
                        rt3 = x[base+Ad3] + x[base+Ad4];   // c + d
                        rt4 = x[base+Ad3] - x[base+Ad4];   // c - d
                        x[base+Ad1] = rt1 + rt3;      // a + b + (c + d)
                        x[base+Ad2] = rt2 + rt4;      // a - b + (c - d)
                        x[base+Ad3] = rt1 - rt3;      // a + b - (c + d)
                        x[base+Ad4] = rt2 - rt4;      // a - b - (c - d)
                 }

                if (Nlog2 > 2) {
                         // third + stages computed here
                        gpSize = 4;
                        numBfs = 2;
                        numGps = numGps / 2;
                        //IJ.write("FFT: dfht3 "+Nlog2+" "+numGps+" "+numBfs);
                        for (stage=2; stage<Nlog2; stage++) {
                                for (gpNum=0; gpNum<numGps; gpNum++) {
                                        Ad0 = gpNum * gpSize * 2;
                                        Ad1 = Ad0;     // 1st butterfly is different from others - no mults needed
                                        Ad2 = Ad1 + gpSize;
                                        Ad3 = Ad1 + gpSize / 2;
                                        Ad4 = Ad3 + gpSize;
                                        rt1 = x[base+Ad1];
                                        x[base+Ad1] = x[base+Ad1] + x[base+Ad2];
                                        x[base+Ad2] = rt1 - x[base+Ad2];
                                        rt1 = x[base+Ad3];
                                        x[base+Ad3] = x[base+Ad3] + x[base+Ad4];
                                        x[base+Ad4] = rt1 - x[base+Ad4];
                                        for (bfNum=1; bfNum<numBfs; bfNum++) {
                                        // subsequent BF's dealt with together
                                                Ad1 = bfNum + Ad0;
                                                Ad2 = Ad1 + gpSize;
                                                Ad3 = gpSize - bfNum + Ad0;
                                                Ad4 = Ad3 + gpSize;

                                                CSAd = bfNum * numGps;
                                                rt1 = x[base+Ad2] * C[CSAd] + x[base+Ad4] * S[CSAd];
                                                rt2 = x[base+Ad4] * C[CSAd] - x[base+Ad2] * S[CSAd];

                                                x[base+Ad2] = x[base+Ad1] - rt1;
                                                x[base+Ad1] = x[base+Ad1] + rt1;
                                                x[base+Ad4] = x[base+Ad3] + rt2;
                                                x[base+Ad3] = x[base+Ad3] - rt2;

                                        } /* end bfNum loop */
                                } /* end gpNum loop */
                                gpSize *= 2;
                                numBfs *= 2;
                                numGps = numGps / 2;
                        } /* end for all stages */
                } /* end if Nlog2 > 2 */

                if (inverse)  {
                        for (i=0; i<maxN; i++)
                        x[base+i] = x[base+i] / maxN;
                }
        }

        void transposeR (float[] x, int maxN) {
                int   r, c;
                float  rTemp;

                for (r=0; r<maxN; r++)  {
                        for (c=r; c<maxN; c++) {
                                if (r != c)  {
                                        rTemp = x[r*maxN + c];
                                        x[r*maxN + c] = x[c*maxN + r];
                                        x[c*maxN + r] = rTemp;
                                }
                        }
                }
        }

        int log2 (int x) {
                int count = 15;
                while (!btst(x, count))
                        count--;
                return count;
        }


        private boolean btst (int  x, int bit) {
                //int mask = 1;
                return ((x & (1<<bit)) != 0);
        }

        void BitRevRArr (float[] x, int base, int bitlen, int maxN) {
                for (int i=0; i<maxN; i++)
                        tempArr[i] = x[base+bitrev[i]];
                for (int i=0; i<maxN; i++)
                        x[base+i] = tempArr[i];
        }

        //private int BitRevX (int  x, int bitlen) {
        //	int  temp = 0;
        //	for (int i=0; i<=bitlen; i++)
        //		if (btst (x, i))
        //			temp = bset(temp, bitlen-i-1);
        //	return temp & 0x0000ffff;
        //}

        private int bitRevX (int  x, int bitlen) {
                int  temp = 0;
                for (int i=0; i<=bitlen; i++)
                        if ((x & (1<<i)) !=0)
                                temp  |= (1<<(bitlen-i-1));
                return temp & 0x0000ffff;
        }

        private int bset (int x, int bit) {
                x |= (1<<bit);
                return x;
        }
	/**
	* Get the power spectrum of the hartley transform (maxN x maxN pixels)
	* and return as a byte array.
	*/
	public byte [] getPowerSpectrum(float[] BIJfht)
	{
   		float[] fps = new float[maxN*maxN];
		float min = Float.MAX_VALUE;
  		float max = Float.MIN_VALUE;
  		for (int row=0; row<maxN; row++)
		{
			fhtps(row, maxN, BIJfht, fps);
			int base = row * maxN;
			for (int col=0; col<maxN; col++)
			{
				float r = fps[base+col];
				if (r<min) min = r;
				if (r>max) max = r;
			}
		}
		flipquad(fps);
		if (min < 1.0)
			min = 0f;
		else
			min = (float)Math.log(min);
		max = (float)Math.log(max);
		float scale = (float)(253.0/(max-min));

 		byte [] ps = new byte[maxN*maxN];
		for (int row=0; row<maxN; row++)
		{
			int base = row*maxN;
			for (int col=0; col<maxN; col++)
			{
				float r = fps[base+col];
				if (r<1f)
					r = 0f;
				else
					r = (float)Math.log(r);
				ps[base+col] = (byte)(((r-min)*scale+0.5)+1);
			}
		}
		return ps;
	}
	/**
	 * Calculate the Power Spectrum of one row from 2D Hartley Transform.
	*/
 	protected void fhtps(int row, int maxN, float[] BIJfht, float[] ps)
	 {
 		int base = row*maxN;
		int l;
		for (int c=0; c<maxN; c++)
		{
			l = ((maxN-row)%maxN) * maxN + (maxN-c)%maxN;
			ps[base+c] = (float) (Math.pow(BIJfht[base+c], 2) + Math.pow(BIJfht[l], 2))/2f;
 		}
	}
}

