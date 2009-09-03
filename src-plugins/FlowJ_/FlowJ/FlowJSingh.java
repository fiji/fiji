package FlowJ;
import ij.*;
import ij.process.*;
import volume.*;
import bijnum.*;

/*
	  This class implements the Singh region matching optical flow algorithm,
	  as coded by Barron and Beauchemin on their web site (csd.uwo.ca/vision)

	  (c) 1999, Michael Abramoff for translation to Java, debugging of original code.

	  Reference:
	  Barron, Fleet, Beauchemin, 1994, IJCV 12, 1, 43-77

	  Original implementation by Steven Beauchemin
	  See below copyright notice.

	  AUTHOR : Steven Beauchemin
	  AT : University of Western Ontario
	  DATE : September 13 1990
*/
public class FlowJSingh
{
	  public final boolean  debug = false;
	  private final int     N = 4;             // maximum displacement in pixels. Max 4.
	  private final int     n = 2;             // neighborhood size for step 1 (2n+1).
	  private float        tau1;              // eigenvalue threshold tau step 1
	  private float        tau2;              // eigenvalue threshold tau step 2
	  private final int     i = 10;            // Number of max iterations step 2
	  private final int     w = 2;             // window size for step 2. (2w+1)
	  private final float  DIFF_THRESH = 1e-5f; // iteration underflow.
	  private final int     edge = 10;          // more or less arbitrary distance from edges.
	  private float        sigmas;             // Singh: 1.0
	  private float        density;
	  private int           width;
	  private int           height;
	  private int           depth;
	  private               VolumeFloat v;           // the filtered image volume
	  private float [][][][] Scc;               // covariance matrix for all pixels.
	  private float [][][] Ucc;                 // distribution mean for each component for each pixel.
	  private float []      pixels;              // for debugging.

	  public String toString()
	  // Use a format that is compatible with file names.
	  { return "S s"+sigmas+" tau1"+tau1+" tau2"+tau2
				+" N"+N+" n"+n+" i"+i+" w"+w+" ("+IJ.d2s(density*100,2)+"%)"; }
	  public void filterAll(ImageStack stack, int center, float sigmas)
	  throws FlowJException
	  {
			  width = stack.getWidth();
			  height = stack.getHeight();
			  depth = 3;
			  this.sigmas = sigmas;

			  IJ.write("Singh: laplace convolution sigma "+sigmas);
			  v = new VolumeFloat(width, height, depth);
			  if (stack.getSize() < depth)
			  {
							FlowJException e = new FlowJException("Need at least " + depth + " slices.");
						  throw e;
				  }
			  if (center < depth/2)
			  {
						  FlowJException e = new FlowJException("Please select a slice over "
					  + depth / 2 + " and below " + depth / 2);
						  throw e;
				  }
			  Gaussian sGaussian = new Gaussian(sigmas);
			  /* Compute L(v) as a difference of Gaussians: L(v) = v-((v*Gaussian)*Gaussian). */
			  v.load(stack, center - depth / 2);
			  VolumeFloat vc = new VolumeFloat(width, height, depth);
			  VolumeFloat vcc = new VolumeFloat(width, height, depth);
			  vc.convolvex(v, sGaussian);
			  vcc.convolvey(vc, sGaussian);
			  /* Subtract convolved image from original image. */
			  v.sub(vcc);
			  // map the laplacian.
			  if (debug) pixels = v.map();
		}
		public int getWidth()
		{ return width; }
		public int getHeight()
		{ return height; }
		public void compute1(FlowJFlow flow, float tau1)
		{
			  int total = 0; int full = 0;

			  Scc = new float[height][width][2][2];
			  Ucc = new float[height][width][2];
			  this.tau1 = tau1;
			  IJ.showStatus("Singh...");
			  // Step 1 Singh. (see Barron)
			  IJ.write("Singh: step 1 tau = "+tau1);
			  flow.v.setEdge(edge);
			  for (int y = 0; y < height; y++)
			  {
					  IJ.showProgress((float) y/(float) height);
					  for (int x = 0; x < width; x++)
					  {
							Ucc[y][x][0] = Ucc[y][x][1] = 100;
							Scc[y][x][0][0] = Scc[y][x][0][1] = Scc[y][x][1][0] = Scc[y][x][1][1] = 100;
							flow.set(x, y, 0, 0, false);
							if (flow.valid(x,y))
							{
								  SSD ssdl = new SSD(v, N*4+1, n, -1, x, y);
								  SSD ssdr = new SSD(v, N*4+1, n, 1, x, y);
								  SSD ssd = new SSD(N*4+1, n);
								  ssd.add(ssdl, ssdr);
								  int [] loc = ssd.peak(x,y);       // find peak
								  if (false && debug && x==45 && y==46)
								  {
										IJ.write(ssd.toString());
										IJ.write("peak: "+loc[0]+","+loc[1]);
								  }
								  if (debug && x >= 44 && x < 46 && y >= 45 && y < 47)
										ssd.check(x, y, loc, 0);
								  SSD ssdc = new SSD(N*2+1, n);  // subregion patch
								  ssdc.center(ssd, loc);         // center around peak
								  float min = ssdc.min();       // minimum value in ssdc.
								  if (debug && x >= 44 && x < 46 && y >= 45 && y < 47)
										IJ.write("min = "+min);
								  if (debug && x >= 44 && x < 46 && y >= 45 && y < 47)
										ssdc.check(x, y, loc, Float.MAX_VALUE);
								  float k;
								  if (min < 1.0e-6)
										k = 0.0085f;
								  else
										k = - (float) Math.log(0.95) / min;
								  ssdc.recompute(k);            // recompute with k value.
								  float[][] covariance = new float[2][2];
								  float[] mean = new float[2];
								  ssdc.mean(4, loc, mean, covariance, x, y);   // determine mean
								  if (debug && x==45 && y==46)
								  {
										// map the 4 ssd's.
										ssdr.map(pixels, width*5, width*5*height+32);
										ssdl.map(pixels, width*5, width*5*height+64);
										ssd.map(pixels, width*5, width*5*height+96);
										ssdc.map(pixels, width*5, width*5*height+128);
										ImageProcessor ip = (ImageProcessor) (new FloatProcessor(width*5, height*5, pixels, null));
										ImagePlus imp = new ImagePlus("laplacian+ssd's", ip); imp.show();
								  }
								  final float step_size = 1;

								  // Keep mean and covariance matrix for step 2.
								  Scc[y][x][0][0] = (float) covariance[0][0];
								  Scc[y][x][0][1] = (float) covariance[0][1];
								  Scc[y][x][1][0] = (float) covariance[1][0];
								  Scc[y][x][1][1] = (float) covariance[1][1];
								  Ucc[y][x][0] = (float) (mean[0]/step_size);
								  Ucc[y][x][1] = (float) -(mean[1]/step_size);
								  /*
										Threshold the velocities based on the eigenvalues of the covariance.
								  */
                                                                  float [][] mi = null; BIJJacobi j = null;
                                                                  try
                                                                  {
                                                                        j = new BIJJacobi(covariance, true);
                                                                } catch (Exception e) { IJ.write("Inverse or Jacobi error "+e); }
								  j.compute();
								  j.sort();
								  //if (debug) j.check(covariance);
								  if (tau1 == 0 || j.eigenvalues[0] < tau1)
								  {
											  flow.set(x, y, Ucc[y][x][0], Ucc[y][x][1]);
											  full++;
								  }
								  total++;
						  }
				  }
			  }
			  density = (float) full/(float) total;
		} // compute1
		public void compute2(FlowJFlow flow, float tau2)
		/*
			Smooth velocity field using velocities computed in step 1,
			Step 2 Neighbourhood Information
		*/
		{
			  this.tau2 = tau2;
			  IJ.showStatus("Singh...");
			  IJ.write("Singh: step 2 tau = "+tau2);
			  int total=0; int full=0;
			  int FIRST = 0;
			  int SECOND = 1;
			  float [][][][] Un = new float[2][height][width][2]; // x,y
			  for (int y = 0; y < height; y++)
			  {
					  for (int x = 0; x < width; x++)
					  {
							Un[FIRST][y][x][0] = Un[SECOND][y][x][0] = Ucc[y][x][0];
							Un[FIRST][y][x][1] = Un[SECOND][y][x][1] = Ucc[y][x][1];
					  }
			  }
			  Gaussian G = new Gaussian(2*w+1);
			  float [][] Wm = new float[2*w+1][2*w+1];
			  /* Compute weights for weight matrix */
				float s = 0;
			  for (int k = 0; k <= 2*w; k++)
					  for (int j = 0; j <= 2*w; j++)
			  {
						Wm[k][j] = (float) (G.k[k] * G.k[j]);
						s += Wm[k][j];
			  }

			  float [][][][] SccI     = new float[height][width][2][2];
			  float [][][][] SccI_Ucc = new float[height][width][2][2];
			  float [][][] Ua         = new float[height][width][2];      // x,y
			  float [][][][][] Sn     = new float[2][height][width][2][2];
			  /* Initialization: compute the inverse of Scc and multiply by Ucc, compute the
					2w+1 x 2w+1 velocity neighborhood and its mean and covariance matrix */
			  for (int y = 0; y < height; y++)
			  {
					  for (int x = 0; x < width; x++)
					  {
							if (x >= edge+w && x < width-edge-w && y >= edge+w && y < height-edge-w)
							{
								  try { BIJmatrix.pseudoinverse(SccI[y][x], Scc[y][x], 0.1); }
								  catch (Exception e) { IJ.write("init inverse error");  }
									BIJmatrix.mul(SccI_Ucc[y][x][0], SccI[y][x], Ucc[y][x]);

									float [][][] neighborhood = new float [w*2+1][w*2+1][2];
								  /* Compute neighbourhood velocities for initialization */
									for (int k=-w;k<=w;k++)
											for (int l=-w;l<=w;l++)
										  {
													if(Un[FIRST][y+k][x+l][0] != 100 && Un[FIRST][y+k][x+l][1] != 100)
													{
														  neighborhood[k+w][l+w][0] = Un[FIRST][y+k][x+l][0];
														  neighborhood[k+w][l+w][1] = Un[FIRST][y+k][x+l][1];
													}
										  }
									calc_mean_and_covariance2(Wm, neighborhood, Ua[y][x], Sn[FIRST][y][x]);
						   }
					  }
			  }
			  if (debug)
					IJ.write("step 2 initialization completed");
			  // Try to reach an iterative estimate.
			  float max_diff = 0;
			  // Make available for gc.
			  Scc = null; Ucc = null;
			  float [][][][] ScSn     = new float[height][width][2][2]; // "S, SsumI"
			  for (int n = 0; n < i; n++)
			  {
					boolean stop = false;
					for (int y = 0; y < height; y++)
					{
							IJ.showProgress((float) y/(float) height);
							for (int x = 0; x < width; x++)
								if (x >= edge+w && x < width-edge-w && y >= edge+w && y < height-edge-w)
								{
                                                                        float [][] SnI = new float [2][2];
                                                                        try { BIJmatrix.pseudoinverse(SnI, Sn[FIRST][y][x], 0.1); }
                                                                        catch (Exception e) { IJ.write("iter "+n+" inverse error");  }
                                                                        float [][] Ssum = new float [2][2];
                                                                        BIJmatrix.add(Ssum, Sn[FIRST][y][x], SnI);
                                                                        try { BIJmatrix.pseudoinverse(ScSn[y][x], Ssum, 0.1); }  // ScSn = SsumI
                                                                        catch (Exception e) { IJ.write("iter 2, "+n+" inverse error"); }
                                                                        float [] vt1 = new float [2];
                                                                        BIJmatrix.mul(vt1, SnI, Ua[y][x]);
                                                                        float [] vt2 = BIJmatrix.addElements(SccI_Ucc[y][x][0], vt1);
                                                                        BIJmatrix.mul(Un[SECOND][y][x], ScSn[y][x], vt2);
                                                                        float [] diff = new float[2];
                                                                        diff[0] = (float) (Un[FIRST][y][x][0]-Un[SECOND][y][x][0]);
                                                                        diff[1] = (float) (Un[FIRST][y][x][1]-Un[SECOND][y][x][1]);
                                                                        float size  = BIJmatrix.norm(diff);
                                                                        if (size > max_diff) max_diff = size;
									  stop = size > DIFF_THRESH;
								}
					}
					if (stop)
					{
						  IJ.write("Step 2 convergence ("+n+") detected - iterative calculations are stopped");
						  break;
					}
					for (int y = 0; y < height; y++)
							  for (int x = 0; x < width; x++)
									  if (x >= edge+w && x < width-edge-w && y >= edge+w && y < height-edge-w)
									  {
											  float [][][] neighborhood = new float [w*2+1][w*2+1][2];
											/* Compute neighbourhood velocities for initialization */
											  for (int k=-w;k<=w;k++)
													for (int l=-w;l<=w;l++)
												  {
															  if(Un[SECOND][y+k][x+l][0] != 100 && Un[SECOND][y+k][x+l][1] != 100)
															  {
																	  neighborhood[k+w][l+w][0] = Un[SECOND][y+k][x+l][0];
																	  neighborhood[k+w][l+w][1] = Un[SECOND][y+k][x+l][1];
															  }
												  }
											  calc_mean_and_covariance2(Wm, neighborhood, Ua[y][x], Sn[SECOND][y][x]);
									  }
					int temp = FIRST; FIRST = SECOND; SECOND = temp;
			  } // n
			  int no_vels = 0;
			  for (int y = edge; y < height-edge; y++)
					for (int x = edge; x < width-edge; x++)
						  if (x >= edge+w && x < width-edge-w && y >= edge+w && y < height-edge-w)
						  {
										flow.set(x, y, 0, 0, false);
										// compute eigenvalues of covariance here.
										Jacobi j = new Jacobi(2);
										j.compute(ScSn[y][x]);
										j.sort();
										//if (debug) j.check(ScSn[y][x]);
										if (tau2 == 0 || j.eigenvalues[0] < tau2)
										{
												if (Un[FIRST][y][x][0]!=100 && Un[FIRST][y][x][1]!=100)
												{
													  no_vels++;
													  flow.set(x, y, Un[FIRST][y][x][0], Un[FIRST][y][x][1]);
													  full++;
											  }
										}
										total++;
						  }
			  IJ.write("step 2 complete. Valid computations: "+no_vels);
			  density = (float) full/(float) total;
	  } // compute2
	  /*
		  Compute the normalized mean and convariance matrix.
		  Step2: Recovery of Neighbourhood Information.
	  */
	  private static void calc_mean_and_covariance2(float [][] weights, float [][][] velocities, float [] mean, float [][] covariance)
	  {
		  /* Compute weighted means - sum of weights is 1.0 */
		  float w_sum = 0;
		  for (int k=0;k<weights.length;k++)
				for (int j=0;j<weights.length;j++)
				  {
						  if (velocities[k][j][0] != 100 && velocities[k][j][1] != 100)
							{
								  w_sum += weights[k][j];
								  mean[0] += weights[k][j]*velocities[k][j][0];
								  mean[1] += weights[k][j]*velocities[k][j][1];
						}
					}

		  /* Compute normalized weighted covariance matrix */
		  for (int k=0;k<2;k++)
			  for (int  j=0;j<2;j++)
					  covariance[k][j] = 0;
		  /* Sum of the weights is 1.0 so no need to normalize */
		  for (int k=0;k<weights.length;k++)
			  for (int j=0;j<weights.length;j++)
				{
					  if (velocities[k][j][0] != 100 && velocities[k][j][1] != 100)
					  {
							  covariance[0][0] += (velocities[k][j][0]-mean[0])*(velocities[k][j][0]-mean[0])*weights[k][j];
							covariance[1][1] += (velocities[k][j][1]-mean[1])*(velocities[k][j][1]-mean[1])*weights[k][j];
							  covariance[0][1] += (velocities[k][j][0]-mean[0])*(velocities[k][j][1]-mean[1])*weights[k][j];
					  }
			  }
		  covariance[0][0] /= w_sum;
		  covariance[0][1] /= w_sum;
		  covariance[1][1] /= w_sum;
		  covariance[1][0] = covariance[0][1];
	  }
}
