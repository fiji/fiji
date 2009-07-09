package FlowJ;
import ij.*;
import volume.*;

/*
	  This class implements the Uras optical flow algorithm.
	  It computes flows using second order derivatives.

	  (c) 1999, Michael Abramoff for implementation in Java, modifications and
	  optimizations.

	  Reference:
	  Barron, Fleet, Beauchemin, 1994, IJCV 12, 1, 43-77
	  Uras, 1988, IJCV

	  Original implementation by Steven Beauchemin
	  See below copyright notice.

	  AUTHOR : Steven Beauchemin
	  AT : University of Western Ontario
	  DATE : September 13 1990

*/
public class FlowJUras
{
	  public final boolean  debug = false;
	  private float         tau;                  // threshold tau
	  private int           region = 1;           // size of regularization region.
	  private float         sigmat, sigmas;
	  private float         density;
	  private int           width;
	  private int           height;
	  private int           depth;
	  private VolumeFloat   v;
	  private VolumeFloat   dx, dy, dxt, dyt, dxx, dyy, dxy;
	  private int           edge;
	  private final int     EDGE = 2;
	  private final int     NRADIUS = 4;
	  private final float MAX_COND =   10000;
	  private int           support;

	  public String toString()
	  // Use a format that is compatible with file names.
	  { return "U s"+sigmas+" t"+sigmat+" tau"+
			  tau+" region"+region+" ("+IJ.d2s(density*100,2)+"%) (support="+support+")"; }
	  public void filterAll(ImageStack is, int center, float sigmat, float sigmas)
	  throws FlowJException
	  // Initialize the image volume. Check constraints.
	  {
			width = is.getWidth();
			height = is.getHeight();
			depth = 5;
			edge = 0;
			this.sigmat = sigmat;
			this.sigmas = sigmas;

			Gaussian tGaussian = null;
			if (sigmat > 0)
				  tGaussian = new Gaussian(sigmat);
			v = new VolumeFloat(width, height, depth);
			support = tGaussian.support()+depth-1;
			// Check enough frames in sequence for filtering.
			if (is.getSize() < support)
			{
					  FlowJException e = new FlowJException("Need at least " +support+" slices in stack.");
					  throw e;
			}
			if (center-1 < support/2 || center-1 > (is.getSize()-support/2))
			{
					  FlowJException e = new FlowJException("Please select the frame  > "
						  + support/2 + " and < " + (is.getSize()+1-support/2));
							throw e;
			}
			// Filter temporally
			if (sigmat > 0)
				  v.convolvet(is, center, tGaussian);
			else
				  v.load(is, center - depth / 2);
			if (sigmas > 0)
			{
				  // Filter spatially.
				  Gaussian sGaussian = new Gaussian(sigmas);
				  edge = v.discreteSupport(sGaussian)/2;
				  v.convolvexy(sGaussian);
			}
		}
		public int getWidth()
		{ return width; }
		public int getHeight()
		{ return height; }
		public void gradients()
		// Compute second order gradients xx, xy, yy, xt, yt.
		{
			  IJ.showStatus("computing derivatives...");
			  // Make the 4 point central difference kernel.
			  Kernel1D cd4p = new CentralDiff();
			  // Compute the first order gradients with 4 point central difference convolution.
			  dx = new VolumeFloat(v.getWidth(), v.getHeight(), 5);
			  dx.convolvex(v, cd4p);
			  dy = new VolumeFloat(v.getWidth(), v.getHeight(), 5);
			  dy.convolvey(v, cd4p);
			  // Compute the second order gradients with 4 point central difference convolution.
			  dxt = new VolumeFloat(v.getWidth(), v.getHeight(), 1);
			  dxt.convolvez(dx, cd4p);
			  dyt = new VolumeFloat(v.getWidth(), v.getHeight(), 1);
			  dyt.convolvez(dy, cd4p);
			  dxx = new VolumeFloat(v.getWidth(), v.getHeight(), 1);
			  dxx.convolvex(dx, cd4p);
			  dyy = new VolumeFloat(v.getWidth(), v.getHeight(), 1);
			  dyy.convolvey(dy, cd4p);
			  dxy = new VolumeFloat(v.getWidth(), v.getHeight(), 1);
			  dxy.convolvex(dy, cd4p);
			  dxx.setEdge(v.discreteSupport(cd4p)/2+4);
		 }
		  public void computeFull(FlowJFlow flow, int region, float tau)
		  /*
			  Uras:
			  Compute flow according to the Uras algorithm.
		  */
		  {
			  this.tau = tau;
			  this.region = region;
			  flow.v.setEdge(edge);
			  int total = 0; int fulls = 0;
			  for (int y = 0; y < height; y++)
			  {
					IJ.showProgress((float) y/(float)height);
					for (int x = 0; x < width; x++)
					{
							flow.set(x, y, 0, 0, false);
							if (dxx.valid(x, y))
							{
								float xx = dxx.v[0][y][x];
								float yy = dyy.v[0][y][x];
								float xy = dxy.v[0][y][x];
								float xt = dxt.v[0][y][x];
								float yt = dyt.v[0][y][x];
								float amp = xx*yy - xy*xy;
								if (amp != 0)
								{
									float[] vv = new float[2];
									  vv[0] = (yt*xy-xt*yy) / amp;
									  vv[1] = - (xt*xy-yt*xx) / amp;
									  float mag = (float) Math.sqrt(Math.pow(vv[0],2)+Math.pow(vv[1],2));
									  // limit length of flow vector.
									  if (mag > 20)
									  {
												vv[0] = vv[0]/mag * 20;
												vv[1] = vv[1]/mag * 20;
									  }
									  flow.set(x, y, vv[0], vv[1], true);
									  fulls++;
								}
								total++;
						  }
					} // for x
			  } // for y
			  if (region > 0)
			  {
					  // Regularize the flow field over region.
					  float [][] cond = new float[height][width];
					  float [][] gauss = new float[height][width];
					  float [][] discr = new float[height][width];
					  discriminant(flow, cond, gauss, discr);
					  regularize(flow, cond, gauss, discr);
			  }
			  density = (float) fulls/(float) total;
		} // compute
		private void regularize(FlowJFlow flow, float [][] cond, float [][] gauss, float [][] discr)
		/*
				Uras: Regularize the flows
				operates procedure 1 (TR1 described in Uras et al.[88])
				  for regularization of flow field.
				Uses the condition numbers in cond and the gaussians in gauss.
		*/
		{
			  int xf = (int)(((width - EDGE*2 - NRADIUS -(EDGE*2 + NRADIUS))%region)/2.0 + 0.5) ;
			  int yf = (int)(((height - EDGE*2 - NRADIUS -(EDGE*2 + NRADIUS))%region)/2.0 + 0.5) ;
			  for (int y = EDGE+NRADIUS+yf; y < height - EDGE*2-NRADIUS-yf; y+=region)
			  {
					for (int x = EDGE+NRADIUS+xf; x < width - EDGE*2-NRADIUS-xf; x+=region)
					{
						  int[][] sample = new int [region*region][2];
						  int n = min(x, y, discr, sample);
						  int m = minCond(sample, cond, n);
						  if (m >= 0)
						  {
								  // threshold
								  // Compiler bug: inline indirection.
								  int m0 = sample[m][0];
								  int m1 = sample[m][1];
								  if (gauss[m1][m0] > tau)
								  {
										  float g = gauss[m1][m0];
										  float cn = cond[m1][m0];
										  propagate(flow, cond, gauss, x, y, flow.getX(m0, m1),
												  flow.getY(m0, m1), cn, g, true);
								  }
								  else propagate(flow, cond, gauss, x, y, 0, 0, 0, 0, false);
						  }
						  else propagate(flow, cond, gauss, x, y, 0, 0, 0, 0, false);
					}
			  }
		}  // Regularize
		private void propagate(FlowJFlow flow, float [][] cond, float [][] gauss, int x, int y,
				float vx, float vy, float cn, float g, boolean fullFlow)
		// Propagate vx, vy and the discriminant over the region.
		{
				for (int k = 0; k < region; k++)
					  for (int l = 0; l < region; l++)
					  {
							flow.set(x+l, y+k, vx, vy, fullFlow);
							gauss[y+k][x+l] = (float) g;
							cond[y+k][x+l] = (float) cn;
					  }
		}
		public void discriminant(FlowJFlow flow, float [][] cond, float [][] gauss, float [][] discr)
		/*
				Computes ||Mt*gradient(U)||/||gradient(It)|| and gaussian curvature.
		*/
		{
			  flow.v.setEdge(flow.v.getEdge()+2);
			  for (int y = 0; y < height; y++)
			  {
					for (int x = 0; x < width; x++)
					{
						  if (dx.valid(x, y))
						  {
								// width = 2*2+1
								Beaudetx bx = new Beaudetx();
								float ux = dx.ux(y, x, bx);
								float uy = dx.uy(y, x, bx);
								float vx = dy.ux(y, x, bx);
								float vy = dy.uy(y, x, bx);
								float MtU = (float) (Math.pow(dx.v[2][y][x]*ux + dy.v[2][y][x]*vx,2)
									+ Math.pow(dx.v[2][y][x]*uy + dy.v[2][y][x]*vy,2));
								float It = (float) (Math.pow(dxt.v[0][y][x],2) + Math.pow(dyt.v[0][y][x],2.0));
								discr[y][x] = (float) Math.sqrt(MtU / It);
								float xx = dxx.v[0][y][x];
								float yy = dyy.v[0][y][x];
								float xy = dxy.v[0][y][x];
								gauss[y][x] = (float) Math.abs(xx * yy - Math.pow(xy, 2));
								float cmax = (float) (0.5 *((xx+yy)+Math.sqrt(Math.pow(xx-yy,2)+4.0*xy*xy)));
								float cmin = (float) (0.5*((xx+yy)-Math.sqrt(Math.pow(xx-yy,2)+4.0*xy*xy)));
								if (Math.abs(cmin) < Math.abs(cmax))
								{
										if (cmin != 0)
												cond[y][x] = (float) Math.abs(cmax/cmin);
										else
												cond[y][x] = (float) MAX_COND;
								}
								else
								{
										if (cmax != 0)
												cond[y][x] = (float) Math.abs(cmin/cmax);
										else
												cond[y][x] = (float) MAX_COND;
								} // if
						} // valid
				  } // for x
			} // for y
                        flow.v.setEdge(flow.v.getEdge()-2);
	  }
	  private void sort(float[] sampleDiscr, int[][] sample, int n)
	  /*
		  Bubblesort the sample up to n on discr.
	  */
	  {
			int m = region;
			if (n < m)
				m = n;
			for (int i = 0 ; i < m - 1 ; i++)
			{
				  int index = i ;
				  for (int j = index + 1; j < n; j++)
				  {
					  if (sampleDiscr[index] > sampleDiscr[j])
							index = j ;
				  }
				  // swap
				  float td = sampleDiscr[index];
				  sampleDiscr[index] = sampleDiscr[i];
				  sampleDiscr[i] = td;
				  // swap
				  int [] t = sample[index];
				  sample[index] = sample[i];
				  sample[i] = t;
			}
	  } // sort
	  public int min(int x, int y, float [][] discr, int[][] sample)
	  /*
		  determines (into sample) n image locations from the image area starting at x,y
				  where the discriminant value is minimal.
	  */
	  {
			float[] sampleDiscr = new float[region * region];
			int n = 0 ;
			for (int k = 0 ; k < region; k++)
				  for (int l = 0 ; l < region; l++)
				  {
						// if no error
						sample[n][0] = x + l;  // x
						sample[n][1] = y + k;  // y
						sampleDiscr[n++] = discr[y+k][x+l];
				  }
			sort(sampleDiscr, sample, n);
			return n;
	  } // min
	  public int minCond(int[][] sample, float [][] cond, int n)
	  // Find the sample with the smallest cond.
	  {
			  float minCon = MAX_COND + 1;
			  if (n < region) n = region;
			  int m = -1;
			  for (int l = 0 ; l < n ; l++)
			  {
					// Compiler bug: inline indirections.
					int l0 = sample[l][0];
					int l1 = sample[l][1];
					if (cond[l1][l0] < minCon)
					{
						  minCon = cond[l1][l0];
						  m = l;
					}
			  }
			  return m;
	} // MinCond
}
