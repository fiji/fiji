package FlowJ;
import ij.*;
import ij.process.*;
import volume.*;
import bijnum.*;

/*
	  This is a class that implements the Fleet/Jepson optical flow algorithm.


	  (c) 1999, Michael Abramoff for translation to Java, optimization, generalization

	  Reference:
	  Barron, Fleet, Beauchemin, 1994, IJCV 12, 1, 43-77
	  Jepson, 1992

	  Original implementation by Steven Beauchemin
	  See below copyright notice.

	  AUTHOR : Steven Beauchemin
	  AT : University of Western Ontario
	  DATE : September 13 1990
*/
public class FlowJFleet
{
	  private float [][]   filter;     // contains gabor filter constants.
	  private float [][][][] normal;    // the normal velocities for all direction/frequency components.
	  private final boolean debug = false;
	  private float        residualThreshold;        // Fleet & Jepson 0.5
	  private float        conditionLimit;           // Fleet & Jepson 10
	  private float        sigmas;                   // Fleet & Jepson 2.5
	  private float        sigmat;                   // Fleet & Jepson 2.5
	  private float        tau;                      // FJ 2.5
	  private float        maxampFraction;           // FJ 0.05
	  private int           width;
	  private int           height;
	  private int           depth;
	  private float        density;
	  private float        maxamp;                   // maximum filter response.
	  private int           edge;
	  private int           validNormals, failedTau, failedAmp;

		public String toString()
		{
			return "FJ s"+sigmas+" t"+sigmat+" tau"+tau+" maxamp"+maxampFraction*100+"% cond"+conditionLimit
				+" resid"+residualThreshold+" ("+IJ.d2s(density*100,2)+"%)";
		}
		public int getWidth()
		{ return width; }
		public int getHeight()
		{ return height; }
		public void filterAll(ImageStack stack, int center, float sigmat, float sigmas)
		throws FlowJException
		{
			width = stack.getWidth();
			height = stack.getHeight();
			depth = 5;
			maxamp = 0;
			edge = 0;
			this.sigmas = sigmas;
			this.sigmat = sigmat;
			VolumeFloat v = new VolumeFloat(width, height, depth);

			Gaussian Gt = new Gaussian(sigmat);

			if (stack.getSize() < v.discreteSupport(Gt))
			{
					  FlowJException e = new FlowJException("Need at least " + v.discreteSupport(Gt) + " slices.");
					  throw e;
				}
			if (center < v.discreteSupport(Gt) / 2)
			{
					  FlowJException e = new FlowJException("Please select a slice over "
					  + (v.discreteSupport(Gt) / 2) + " and below " +
					(stack.getSize() + 1 - v.discreteSupport(Gt) / 2));
					  throw e;
			}

			initFilters(sigmat, sigmas);
			float [] pixels=null;
			if (debug)
			{
				// pixels = new float[5*width*height*5+1]; // 5 rows of 5 images.
			}
			Gaussian Gs = new Gaussian(sigmas);
			VolumeFloat DC = new VolumeFloat(width, height, depth);
			// Compute 0.001 of the Gaussian (= background)
			DC.convolvet(stack, center, Gt);
			DC.convolvey(v, Gs);
			DC.mul(0.001);
			edge = v.discreteSupport(Gs) / 2;
			for (int n = 0; n < filter.length; n++)
			{
				  IJ.showProgress((float) n/(float) filter.length);
				  // Create the tuned Gabor filters.
				  GaborSin Gsx = new GaborSin(sigmas, filter[n][0]);
				  GaborSin Gsy = new GaborSin(sigmas, filter[n][1]);
				  GaborSin Gst = new GaborSin(sigmat, filter[n][2]);
				  GaborCos Gcx = new GaborCos(sigmas, filter[n][0]);
				  GaborCos Gcy = new GaborCos(sigmas, filter[n][1]);
				  GaborCos Gct = new GaborCos(sigmat, filter[n][2]);
				  VolumeFloat savex = new VolumeFloat(width, height, depth);
				  VolumeFloat savett = new VolumeFloat(width, height, depth);
				  VolumeIO vImag = new VolumeIO(width, height, depth);  // writable
				  VolumeIO vReal = new VolumeIO(width, height, depth);  // writable

				  /*
					  This sequence of 1d-convolutions is supposedly the equivalent
					  of a 3-D convolution.
					  Heeger, 1987, appendix B
				  */
				  v.convolvet(stack, center, Gct); // convolve with Gct, leave in savet.
				  savex.convolvex(v, Gcx);        // convolve with Gcx, leave in savex.
				  vImag.convolvey(savex, Gsy);
				  vReal.convolvey(savex, Gcy);

				  savex.convolvex(v, Gsx);
				  v.convolvey(savex, Gcy);
				  vImag.add(v);
				  v.convolvey(savex, Gsy);
				  vReal.sub(v);

				  savett.convolvet(stack, center, Gst); // convolve with Gst, leave in savett.
				  savex.convolvex(savett, Gsx);        // convolve with Gsx, leave in savex.
				  v.convolvey(savex, Gsy);
				  vImag.sub(v);
				  v.convolvey(savex, Gcy);
				  vReal.sub(v);

				  savex.convolvex(savett, Gcx);
				  v.convolvey(savex, Gcy);
				  vImag.add(v);
				  v.convolvey(savex, Gsy);
				  vReal.sub(v);

				  // subtract the DC component.
				  vReal.sub(DC);

				  maxamp = maxAmplitude(vReal, vImag, n, maxamp);
				  // if (debug) mapAmplitude(pixels, vReal, vImag, n, maxamp);

				  // store the filter results temporarily.
				  vReal.write("real"+IJ.d2s(n,0)+".v");
				  vImag.write("imag"+IJ.d2s(n,0)+".v");
			}
			if (debug)
			{
				  ImageProcessor ip = (ImageProcessor) (new FloatProcessor(width*5, height*5, pixels, null));
				  ImagePlus imp = new ImagePlus("responses", ip);
				  imp.show();
			}
	  }
	  private void initFilters(float sigmat, float sigmas)
	  {
				float [][] tuning = new float[22][2];

				tuning[0][0] = 0; tuning[0][1] = 0;
				tuning[1][0] = 30; tuning[1][1] = 0;
				tuning[2][0] = 60; tuning[2][1] = 0;
				tuning[3][0] = 90; tuning[3][1] = 0;
				tuning[4][0] = 120; tuning[4][1] = 0;
				tuning[5][0] = 150; tuning[5][1] = 0;
				tuning[6][0] = 0; tuning[6][1] = 1/ (float) Math.sqrt(3);
				tuning[7][0] = 36; tuning[7][1] = 1/ (float) Math.sqrt(3);
				tuning[8][0] = 72; tuning[8][1] = 1/ (float) Math.sqrt(3);
				tuning[9][0] = 108; tuning[9][1] = 1/ (float) Math.sqrt(3);
				tuning[10][0] = 144; tuning[10][1] = 1/ (float) Math.sqrt(3);
				tuning[11][0] = 180; tuning[11][1] = 1/ (float) Math.sqrt(3);
				tuning[12][0] = 216; tuning[12][1] = 1/ (float) Math.sqrt(3);
				tuning[13][0] = 252; tuning[13][1] = 1/ (float) Math.sqrt(3);
				tuning[14][0] = 288; tuning[14][1] = 1/ (float) Math.sqrt(3);
				tuning[15][0] = 324; tuning[15][1] = 1/ (float) Math.sqrt(3);
				tuning[16][0] = 0; tuning[16][1] =  (float) Math.sqrt(3);
				tuning[17][0] = 60; tuning[17][1] =  (float) Math.sqrt(3);
				tuning[18][0] = 120; tuning[18][1] =  (float) Math.sqrt(3);
				tuning[19][0] = 180; tuning[19][1] =  (float) Math.sqrt(3);
				tuning[20][0] = 240; tuning[20][1] =  (float) Math.sqrt(3);
				tuning[21][0] = 300; tuning[21][1] =  (float) Math.sqrt(3);

				filter = new float[tuning.length][3]; // x, y, t

				IJ.write("Fleet: Gabor filters (sigmas="+sigmas+", sigmat="+sigmat+")");
				final float mu = 1;
				final float beta = 0.8f;
				final float base = 2;
				float b =  (float) Math.pow(base,beta); /* 1.7411 */

				for (int n=0; n<filter.length; n++)
                                {
                                        float theta = tuning[n][0];
                                        float speed = tuning[n][1];

                                        float wavelengths = (2 * (float) Math.PI*(b-1)*sigmas)/(mu*(b+1));
                                        float lambdas = wavelengths*( (float) Math.sqrt(speed*speed+1));
                                        float wavelengtht = (2 * (float) Math.PI*(b-1)*sigmat)/(mu*(b+1));
                                        float lambdat = -wavelengtht*( (float) Math.sqrt(speed*speed+1))/speed;
                                        if (Math.abs(speed) <= 0.000001) lambdat = Float.MAX_VALUE;

                                        float k3 = 2 * (float) Math.PI/lambdat;
                                        float rho = 2 * (float) Math.PI/lambdas;
                                        float k1 = rho* (float) Math.sin(theta*2*Math.PI/360);
                                        float k2 = -rho* (float) Math.cos(theta*2*Math.PI/360);
                                        filter[n][0] = (float) k1;    // x
                                        filter[n][1] = (float) -k2;   // y
                                        filter[n][2] = (float) k3;    // t
                                        if (debug)   IJ.write("<"+n+">: wavelength(s) "+IJ.d2s(wavelengths,2)
						  +" wavelength(t) "+IJ.d2s(wavelengtht,2)+" speed "+IJ.d2s(speed, 2)
						  +" dir "+IJ.d2s(theta, 2)+" "+IJ.d2s(filter[n][0],2)+" "+IJ.d2s(filter[n][1],2)
						  +" "+IJ.d2s(filter[n][2], 2));
                                }
	  }
	  private float maxAmplitude(VolumeFloat vReal, VolumeFloat vImag, int n, float maxamp)
	  {
                float filterMax = 0;
                for (int y = 0; y < vReal.getHeight(); y++)
                for (int x = 0; x < vReal.getWidth(); x++)
                {
                                  if (vReal.valid(x, y))
                                  {
                                                float amplitude =  (float) Math.sqrt(vReal.v[2][y][x]*vReal.v[2][y][x]+
                                                        vImag.v[2][y][x]*vImag.v[2][y][x]);
                                                if (amplitude > filterMax)
                                                         filterMax = amplitude;
                                  }
                          }
                if (debug) IJ.write("<"+n+"> max amplitude: "+IJ.d2s(filterMax, 5)+" maxamp " +IJ.d2s(Math.max(filterMax, maxamp),5));
                return Math.max(filterMax, maxamp);
	  }
	   /************************************************************
			Returns the norm of a vector v of length n.
	  ************************************************************/
	  private float L2norm(float [] v, int length)
	  {
                float sum = 0;
                for (int i=0; i<length; i++)
                                  sum += v[i]*v[i];
                return  (float) Math.sqrt(sum);
	  }
	  /*
			Compute normal velocities.
			In contrast to the Barron code,
			because the computation of frequency/amplitude response for thresholding
			also needs the phase differences, phase differences are thresholded in the same loop,
			as are the normal velocities.
	  */
	  public void normals(float percent_maxamp, float tau)
	  {
			maxampFraction = percent_maxamp;

			this.tau = tau;

			int [] pixels=null;

			normal = new float[filter.length][height][width][2];

			float ampthresh = maxampFraction*maxamp;

			validNormals = failedTau = failedAmp = 0;

			IJ.write("Fleet components: response > "+maxampFraction

				+"; frequency/amplitude < "+tau+"(tau)");

			if (debug) pixels = new int[5*width*height*5+1]; // 5 rows of 5 images.

			for(int n=0; n<filter.length; n++)

			{
				  IJ.showProgress((float) n/(float) filter.length);
				  final float beta = 0.8f;
				  final float base = 2.0f;
				  float b =  (float) Math.pow(base,beta); /* 1.7411 */

				  // compute threshold from filter tuning and user set tau.
				  float f0 = L2norm(filter[n], 3);
				  float sigmak_tau_2 =  (float) Math.pow(tau/((b+1)/((b-1)*f0)), 2);

				  VolumeIO vReal = new VolumeIO("real"+IJ.d2s(n,0)+".v");
				  vReal.delete("real"+IJ.d2s(n,0)+".v");
				  VolumeIO vImag = new VolumeIO("imag"+IJ.d2s(n,0)+".v");
				  vImag.delete("imag"+IJ.d2s(n,0)+".v");
				  // Compute phase differences and threshold.
				  float [][][] phi = new float[height][width][3];
				  Dphi((VolumeFloat) vReal, (VolumeFloat) vImag, phi, sigmak_tau_2, ampthresh, n);
				  computeNormal(phi, n);
			}
			IJ.write(""+validNormals+" valid components ("+(width*height*filter.length)+")");
			IJ.write("failed amplitude test "+failedAmp+", failed amp/freq test "+failedTau);
	  }
	  /************************************************************

		  Apply 1D complex kernel in the x direction to a complex volume at x, y, t
	  ************************************************************/
	  private void convolveComplexx(float [] r, VolumeFloat vReal, VolumeFloat vImag,
			Kernel1D kReal, Kernel1D kImag, int x, int y, int t, float c)
	  {
		  r[0] = r[1] = 0;
		  if (kReal.halfwidth != kImag.halfwidth)
		  {
			  IJ.error("kernel not correct");
			  return;
		  }
		  for (int i=-kReal.halfwidth;i<=kReal.halfwidth;i++)
			{
				  r[0] += kReal.k[i+kReal.halfwidth]*vReal.v[t][y][x+i]-kImag.k[i+kImag.halfwidth]*vImag.v[t][y][x+i];
				  r[1] += kImag.k[i+kImag.halfwidth]*vReal.v[t][y][x+i]+kReal.k[i+kReal.halfwidth]*vImag.v[t][y][x+i];
			}
		  r[0] -= c * vImag.v[t][y][x];
		  r[1] += c * vReal.v[t][y][x];
	  }

	  /************************************************************
		  Apply 1D complex kernel in the y direction
	  ************************************************************/
	  private void convolveComplexy(float [] r, VolumeFloat vReal, VolumeFloat vImag,
			Kernel1D kReal, Kernel1D kImag, int x, int y, int t, float c)
	  {
		  r[0] = r[1] = 0;
		  if (kReal.halfwidth != kImag.halfwidth)
		  {
			  IJ.error("kernel not correct");
			  return;
		  }
		  for(int i=-kReal.halfwidth;i<=kReal.halfwidth;i++)
			{
				  r[0] += kReal.k[i+kReal.halfwidth]*vReal.v[t][y+i][x]-kImag.k[i+kImag.halfwidth]*vImag.v[t][y+i][x];
				  r[1] += kImag.k[i+kReal.halfwidth]*vReal.v[t][y+i][x]+kReal.k[i+kImag.halfwidth]*vImag.v[t][y+i][x];
			}
		  r[0] -= c * vImag.v[t][y][x];
		  r[1] += c * vReal.v[t][y][x];
	  }
	  /************************************************************
		  Apply 1D complex kernel in the t direction
	  ************************************************************/
	  private void convolveComplext(float [] r, VolumeFloat vReal, VolumeFloat vImag,
			Kernel1D kReal, Kernel1D kImag, int x, int y, int t, float c)
	  {
		  r[0] = r[1] = 0;
		  if (kReal.halfwidth != kImag.halfwidth)
		  {
			  IJ.error("kernel not correct");
			  return;
		  }
		  for(int i=-kReal.halfwidth;i<=kReal.halfwidth;i++)
			{
				  r[0] += kReal.k[i+kReal.halfwidth]*vReal.v[t+i][y][x]-kImag.k[i+kImag.halfwidth]*vImag.v[t+i][y][x];
				  r[1] += kImag.k[i+kReal.halfwidth]*vReal.v[t+i][y][x]+kReal.k[i+kImag.halfwidth]*vImag.v[t+i][y][x];
			}
		  r[0] -= c * vImag.v[t][y][x];
		  r[1] += c * vReal.v[t][y][x];
	  }
	  /*
		  Compute phase differences for filters n.
		  Compute thresholds on the fly (a fudge but 3x more efficient).
	  */
	  private void Dphi(VolumeFloat vReal, VolumeFloat vImag, float[][][] phi,
			float tau, float ampthresh, int n)
	  {

			// Demodulation kernels for x,y,t.

			Demodulation demodRealx = new Demodulation(filter[n][0], true); // real part

			Demodulation demodImagx = new Demodulation(filter[n][0], false);// imag part
			Demodulation demodRealy = new Demodulation(filter[n][1], true); // real part
			Demodulation demodImagy = new Demodulation(filter[n][1], false);// imag part
			Demodulation demodRealt = new Demodulation(filter[n][2], true); // real part
			Demodulation demodImagt = new Demodulation(filter[n][2], false);// imag part

			int valid = 0;
			// compute Im[R(complex conjugate) R] / | R |(square)
			for (int y = 0; y < height; y++)
			{
					for (int x = 0; x < width; x++)
				{
					  phi[y][x][0] = phi[y][x][1] = phi[y][x][2] = 100;
					  if (vReal.valid(x,y) && x >= demodRealx.halfwidth && x < width - demodRealx.halfwidth && y >= demodRealy.halfwidth && y < height - demodRealy.halfwidth)
					  {
                                                float ampSquare =  (float) Math.pow(vReal.v[2][y][x],2)+ (float) Math.pow(vImag.v[2][y][x],2);
                                                float amp =  (float) Math.sqrt(ampSquare);
                                                if (amp > 0)
                                                {
                                                        float [] rx = new float[2]; // real, imag
                                                        float [] ry = new float[2];
                                                        float [] rt = new float[2];
                                                        convolveComplexx(rx, vReal, vImag, demodRealx, demodImagx,
                                                                x, y, depth/2, filter[n][0]);
                                                        convolveComplexy(ry, vReal, vImag, demodRealy, demodImagy,
                                                                x, y, depth/2, filter[n][1]);
                                                        convolveComplext(rt, vReal, vImag, demodRealt, demodImagt,
                                                                x, y, depth/2, filter[n][2]);
                                                        // compute phase differences for each image location
                                                        phi[y][x][0] = (float) ((vReal.v[depth/2][y][x]*rx[1]- rx[0]*vImag.v[depth/2][y][x])/ampSquare);
                                                        phi[y][x][1] = (float) ((vReal.v[depth/2][y][x]*ry[1]- ry[0]*vImag.v[depth/2][y][x])/ampSquare);
                                                        phi[y][x][2] = (float) ((vReal.v[depth/2][y][x]*rt[1]- rt[0]*vImag.v[depth/2][y][x])/ampSquare);
                                                        // compute constraint components for thresholding.
                                                        float [] dA = new float[3];
                                                        dA[0] = (vReal.v[depth/2][y][x]*rx[0]+vImag.v[depth/2][y][x]*rx[1])/ampSquare;
                                                        dA[1] = (vReal.v[depth/2][y][x]*ry[0]+vImag.v[depth/2][y][x]*ry[1])/ampSquare;
                                                        dA[2] = (vReal.v[depth/2][y][x]*rt[0]+vImag.v[depth/2][y][x]*rt[1])/ampSquare;
                                                        float [] diff = new float[3];
                                                        diff[0] = phi[y][x][0]-filter[n][0];
                                                        diff[1] = phi[y][x][1]-filter[n][1];
                                                        diff[2] = phi[y][x][2]-filter[n][2];
                                                        // now threshold. if over tau or below ampthresh, set phi's to 100.
                                                        if  (amp < ampthresh)
                                                        {
                                                                phi[y][x][0] = phi[y][x][1] = phi[y][x][2] = 100;
                                                                failedAmp++;
                                                        }
                                                        else if ((Math.pow(L2norm(dA,3),2)+Math.pow(L2norm(diff,3),2)) > tau)
                                                        {
                                                                phi[y][x][0] = phi[y][x][1] = phi[y][x][2] = 100;
                                                                failedTau++;
                                                        }
                                                        else
                                                        { validNormals++; valid++; }
                                                        // count all valid responses
                                                }
                                        }
				}
			}
	  } // Dphi
	  // Compute normal velocity components from the phase differences with filter n.
	  private void computeNormal(float [][][] phi, int n)
	  {
			for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
				{
					  // is the phi valid.
					  if (phi[y][x][0] == 100 && phi[y][x][1] == 100 && phi[y][x][2] == 100)
					  {   normal[n][y][x][0] = normal[n][y][x][1] = 100; }
					  else
					  {
						  /*
							  s = -phi(t)/(sqr(phi(x))+sqr(phi(y)))
							  n = phi(x)/(sqr(phi(x))+sqr(phi(y)))
						  */
						  float denom=( (float) Math.pow(phi[y][x][0],2)+  (float) Math.pow(phi[y][x][1],2));
							  normal[n][y][x][0] = (float) (-(phi[y][x][0]*phi[y][x][2])/denom); // x
							  normal[n][y][x][1] = (float) -(-(phi[y][x][1]*phi[y][x][2])/denom);
					  }
			   }
	  }
	  /************************************************************
		  Compute all full velocities from 5*5 neighbourhoods of
		  normal velocities.
	  ************************************************************/
	  public void computeFull(FlowJFlow flow, float conditionLimit, float residualThreshold)
	  {
			int no_wronginverse = 0; int no_notcomp = 0;
			int no_count = 0;
			int no_ill_conditioned = 0; int no_large_residuals = 0;
			int total=0; int full=0;
			this.conditionLimit = conditionLimit;
			this.residualThreshold = residualThreshold;
			IJ.write("Fleet: full velocities cond nr < "+IJ.d2s(conditionLimit,2)
				  +"; residual < "+residualThreshold);
			System.gc();
			for (int y = 0; y < height; y++)
			{
				  IJ.showProgress((float) y/(float) height);
				  for (int x = 0; x < width; x++)
				  {
						flow.set(x, y, 0, 0, false);
						int count = 0;
						float [][] J = new float [filter.length*5*5][6];
						float [] vn = new float [filter.length*5*5];
						/* 5x5 neighborhood for all filters. */
						if (x > 2 && x < width-2 && y > 2 && y < height-2)
						{
							for (int n = 0; n < filter.length; n++)
							{
								  for (int m = -2; m <= 2; m++)
									  for (int l = -2; l <= 2; l++)
											  if (normal[n][y+m][x+l][0]!= 100 && normal[n][y+m][x+l][1] != 100)
										  {
													float size = L2norm(normal[n][y+m][x+l],2);
													  if (size > 0.000001)
													  {
														float [] nv = new float[2];
												  nv[0] = normal[n][y+m][x+l][0]/size;
												  nv[1] = normal[n][y+m][x+l][1]/size;
														J[count][0] = nv[0];
														J[count][1] = nv[0]*l; // x
														J[count][2] = nv[0]*m; // y
														J[count][3] = nv[1];
														J[count][4] = nv[1]*l; // x
												  J[count][5] = nv[1]*m; // y
														vn[count++] = size;
												  if (debug && x==60 && y==58 && l==0 && m == 0)
												  {
														IJ.write("full: normal<"+n+">["+(y+m)+"]["+(x+l)+"]/size: "+IJ.d2s(nv[0],5)+","+IJ.d2s(nv[1],5)
														+" size "+IJ.d2s(size,5)+"("+m+", "+l+")");
												  }
												} // if
										  } // if
							} // for
							total++;
						} // if
						else
							  no_notcomp++;
						if (count >= 6) /* At least 6 normal velocities are needed */
						{
									float [] product = new float[count];
									float [][][] v = new float[3][3][2];
									float condnum = 0;
									boolean inversefailed = false;
									try {condnum = optimizeVelocity(count, 6, J, vn, v, product);}
									catch (Exception e) { no_wronginverse++; inversefailed = true; condnum = Float.MAX_VALUE; }

									if (condnum < conditionLimit)
									{
										  float [] difference = new float[count];
										  for (int i = 0; i < count; i++)
												difference[i] =  (float) Math.pow(vn[i] - product[i], 2);
										  float residual = L2norm(difference, count)/L2norm(vn, count);
										  if (residual < residualThreshold)
										  {
												flow.set(x, y, v[1][1][0], v[1][1][1], true);
												full++;
										  }
										  else
												no_large_residuals++;
									 }
									 else
										  no_ill_conditioned++;
						} // if
						else
							  no_count++;
				 } // for x
			  } // for y
			  density = (float) full/(float) total;
			  IJ.write("Fleet failed computations:\n"
					+"on residual: "+no_large_residuals
					+"; ill conditioned: "+no_ill_conditioned
					+"; on too few valid normal velocities: "+no_count
					+"; invalid locations: "+no_notcomp);
			  if (no_wronginverse > 0)
					IJ.write("Invalid pseudoinverse calculations: "+no_wronginverse);
	  }
	  private float optimizeVelocity(int r, int n, float [][] J, float [] vn, float [][][] v, float [] product)
	  /*
		  Optimize the n normal image velocities in J
		  This function accepts a collection of r normal image velocity
		  measurements and a n*6 matrix computed using normal directions
		  and image locations and computes the full image velocity for
		  some local neighbourhood. A linear approximation is used to
		  relate normal image velocity in some small neighbourhood to
		  full image velocity at one point.
	  */
	  {
			float [] alphabeta = new float [n];
			float [][] JI = new float[n][r];
			float condnum = BIJmatrix.pseudoinverse(JI, J, 0);   // compute with SVD, do not replace singular values.
			if (condnum < Float.MAX_VALUE)
			{
				  for(int i=0;i<n;i++)
					{
						  alphabeta[i] = 0;
						  for(int j=0;j<r;j++)
								  alphabeta[i] += JI[i][j]*vn[j];
					}
				  // check product of J and JI corrected for length
				  for(int i=0;i<r;i++)
					{
						  product[i] = 0;
						  for(int j=0;j<n;j++)
								  product[i] += J[i][j]*alphabeta[j];
					}
				  /* Compute full velocity field in 3*3 neighbourhood */
				  for(int i=-1;i<=1;i++)
						for(int j=-1;j<=1;j++)
						  {
								v[i+1][j+1][0] = alphabeta[0]+alphabeta[1]*j+alphabeta[2]*i;
								v[i+1][j+1][1] = alphabeta[3]+alphabeta[4]*j+alphabeta[5]*i;
						  }
			  }
			  return condnum;
	  }
	  private float [] mapAmplitude(float [] pixels, VolumeFloat vReal, VolumeFloat vImag, int n, float maxamp)
	  /* map all filter amplitudes to a single image. */
	  {
			int col = n % 5; int row = n / 5;
			for (int y = 0; y < height; y++)
			{
					for (int x = 0; x < width; x++)
				{
					  int i = col * width + row * width * height * 5 + x + y * 5 * width;
					  if (vReal.valid(x,y))
					  {
							float amplitude =  (float) Math.sqrt(vReal.v[2][y][x]*vReal.v[2][y][x]+
								vImag.v[2][y][x]*vImag.v[2][y][x]);
							pixels[i++] = (float) amplitude;
					  }
				  }
			}
			return pixels;
	  }
	  private float [] mapFloat(float [] pixels, VolumeFloat v, int n)
	  /* map all central images of the volume in a 5x5 image. */
	  {
			int col = n % 5; int row = n / 5;
			for (int y = 0; y < height; y++)
			{
					for (int x = 0; x < width; x++)
				{
					  int i = col * width + row * width * height * 5 + x + y * 5 * width;
					  if (v.valid(x,y))
					  {
							float value = v.v[2][y][x];
							pixels[i++] = (float) value;
					  }
				  }
			}
			return pixels;
	  }
}
