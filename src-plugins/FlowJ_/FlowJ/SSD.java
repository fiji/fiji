package FlowJ;
import ij.*;
import volume.*;


/*
	This class implements SSD (sum of squared differences) surface computation
	and operations on such surfaces.
	Used by Singh's optical flow algorithm.

	(c) 1999 Michael Abramoff for implementation
*/
public class SSD
{
	public float[][] w;
	private int size;
	private int n;
	int [] peakloc;

	public SSD(int size, int n)
	{
		/*
		  Create a new empty SSD
		 */
		this.size = size;
		this.n = n;
		w = new float[size][size];
		peakloc = new int[2];
	}
	public SSD(VolumeFloat v, int size, int n, int direction, int x, int y)
	{
		/*
		  Create and init a square SSD surface (size x size) around x,y
		  in direction (-1 or 1) from the central image in a 3xheightxwidth volume.
		*/
		peakloc = new int[2];
		if (direction != 1 && direction != -1)
		{
			IJ.error("Error in SSD ");
			return;
		}
		this.size = size;
		this.n = n;
		w = new float[size][size];

		float[][][] p = v.v;
		if (direction == 1) // right
		{
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
			   w[l+size/2][k+size/2] = 0;
			   for (int j = -n; j <= n; j++)
				  for (int i = -n; i <= n; i++)
					  w[l+size/2][k+size/2] += Math.pow(p[1][y+j][x+i] - p[2][y+j+l][x+i+k], 2);
			}
		}
		else
		{
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
			   // left is inverse order. size is odd!
			   w[(size-1) - (l+size/2)][(size-1) - (k+size/2)] = 0;
			   for (int j = -n; j <= n; j++)
				  for (int i = -n; i <= n; i++)
					  w[(size-1) - (l+size/2)][(size-1) - (k+size/2)] += Math.pow(p[1][y+j][x+i] - p[0][y+j+l][x+i+k], 2);
			}
		}
	  }
	  public void add(SSD ssdl, SSD ssdr)
	  {
		  // add two SSD surfaces.
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			   w[l+size/2][k+size/2] = ssdl.w[l+size/2][k+size/2] + ssdr.w[l+size/2][k+size/2];

	  }
	  public int [] peak(int x, int y)
	  {
		  // find the peak and minimum ssd coordinates.
		  float minp = Float.MAX_VALUE;
		  float mag = 0;
		  peakloc[0] = peakloc[1] = 0;
		  for (int l = -size/2; l <= size/2; l++)
		  {
			for (int k = -size/2; k <= size/2; k++)
			{
				// look for minimum in size/2xsize/2 central region only.
				/* COMPILER BUG. EXTERNALISED THIS CODE. */
				// Compiler bug! Inline code for offsets.
				int lo = l+size/2; int ko = k+size/2;
				int distance = l*l+k*k;
				if (l >= -size/4 && l <= size/4 && k >= -size/4 && k <= size/4)
				{
					// compiler bug???
					boolean test1 = Math.abs(w[lo][ko] - minp) <= 0.1;
					boolean test2 = distance < mag; // centrality
					boolean test3 = (minp - w[lo][ko]) > 0.1; // significance of delta.
					if (test1 && test2 || test3)
					{
						minp = w[lo][ko];
						peakloc[1] = l; peakloc[0] = k;
						mag = distance;  // Euclidean distance from center.
					}
				}
			}
		  }
		  return peakloc;
	  }
	  public void center(SSD ssd, int [] peakloc)
	  /* creates new centered ssd from noncentered ssd around peakloc (x,y). */
	  {
		  // center around sizexsize surface around peak.
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
				w[l+size/2][k+size/2] = ssd.w[l+size/2+peakloc[1]+ssd.size/4][k+size/2+peakloc[0]+ssd.size/4];
	  } // center
	  public float min()
	  /* Finds minimum non-zero value in ssd. */
	  {
		  float min = Float.MAX_VALUE;
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
				if (w[l+size/2][k+size/2] < min && w[l+size/2][k+size/2] != 0)
					min = w[l+size/2][k+size/2];
			}
		  return min;
	  }
	  public void recompute(float K)
	  /* recompute the SSD using K value. SSD will be a probability distribution. */
	  {
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
				 w[l+size/2][k+size/2] = (float) Math.exp(- K * w[l+size/2][k+size/2]);

	  }
	  public void mean(int displacement, int [] peakloc, float [] mean, float [][] covariance, int x, int y)
	  /*
		   Calculate the mean of this ssd patch relative to the peak
		   in the x and y direction.
	  */
	  {
		  mean[0] = 0;
		  mean[1] = 0;
		  float sum = 0;
		  float inc = (2 * displacement) / (size - 1);
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
				sum += w[l+size/2][k+size/2];
				float ux = peakloc[0] - displacement + (k + size/2) * inc;    // av
				float uy = peakloc[1] - displacement + (l + size/2) * inc;    // bv
				mean[0] += w[l+size/2][k+size/2] * ux;       // af
				mean[1] += w[l+size/2][k+size/2] * uy;       // bf
			 }
		  mean[0] /= sum;
		  mean[1] /= sum;
		  // calculate covariances.
		  covariance[0][0] = covariance[0][1] = covariance[1][1] = 0;
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
				float ux = peakloc[0] - displacement + (k + size/2) * inc;    // av
				float uy = peakloc[1] - displacement + (l + size/2) * inc;    // bv
				covariance[0][0] += w[l+size/2][k+size/2] * Math.pow(ux - mean[0], 2);
				covariance[1][1] += w[l+size/2][k+size/2] * Math.pow(uy - mean[1], 2);
				covariance[0][1] += w[l+size/2][k+size/2] * (ux - mean[0]) * (uy - mean[1]);
			 }
		  // covariance is symmetric matrix.
		  covariance[1][0] = covariance[0][1];
		  for (int j = 0; j < 2; j++) for (int i = 0; i < 2; i++)
			  covariance[j][i] /= sum;
	  }
	  public void map(float [] pixels, int width, int offset)
	  /* map this SSD to a pixel array. */
	  {
		  for (int l = -size/2; l <= size/2; l++)
			for (int k = -size/2; k <= size/2; k++)
			{
				  int i = offset + l*width + k;
				  pixels[i] = w[l+size/2][k+size/2];
			}
	  }
	  public String toString()
	  /* map this SSD to a string. */
	  {
		  String s = "";
		  for (int l = -size/2; l <= size/2; l++)
		  {
				for (int k = -size/2; k <= size/2; k++)
					  s+=""+IJ.d2s(w[l+size/2][k+size/2],4)+"\t";
				s+="\n";
		  }
		  return s;
	  }
	  public void check(int x, int y, int [] loc, float startmag)
	  {
			int r = 10;
			float [][] SSD = new float[size][size];
			int N = size/4;

			for (int l = -size/2; l <= size/2; l++)
				for (int k = -size/2; k <= size/2; k++)
					SSD[l+size/2][k+size/2] = w[l+size/2][k+size/2];
			IJ.write("Check minima at "+x+","+y+" max (peak)="+loc[0]+","+loc[1]);
			for (int i=0;i<r;i++)
			{
				  float min_value = Float.MAX_VALUE;
				  float SSDmag = startmag;   // 0 or MAX_VALUE;
				  int u_min = 0; int v_min = 0;
				  for (int u=(-N);u<=N;u++)
				  for (int v=(-N);v<=N;v++)
					{
						  int u_index = u+N;
						int v_index = v+N;
						if ((Math.abs(SSD[u_index][v_index] - min_value) <= 0.1 && (u*u+v*v) < SSDmag)
							||
						  (min_value - SSD[u_index][v_index] > 0.1))
						{
								  min_value = SSD[u_index][v_index];
							  v_min = v; u_min = u;
							  SSDmag = u*u+v*v;  // Euclidean distance from center.
						}
					}
				  SSD[u_min+N][v_min+N] = Float.MAX_VALUE;
				  if (startmag < 1)
						IJ.write(""+i+"th minimum: "+min_value+" at "+u_min+","+v_min
						+" SSDmag: "+(u_min*u_min+v_min*v_min));
				  else  IJ.write(""+i+"th minimum: "+min_value+" at "+u_min+","+v_min);

			}
	  }

}
