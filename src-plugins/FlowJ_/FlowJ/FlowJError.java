package FlowJ;
import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import bijnum.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;

/*
	  This class implements statistics computations on flow fields and comparisons
	  between flow fields (including true flows).

	  Definition of psi and density derive from Barron et al., 1994 IJCV

	  Originaly written in C++ by Michael Abramoff, 1997.
	  Translated to Java, Michael Abramoff, 1999
*/
public class FlowJError
{
        private float 			psi[][];                // angular error in degrees
        private float 			angular[][];            // actual angular velocity.
        private float [][][] 	        delta;                  // separated error measure
        private boolean 			hasflow[][];            // whether flow was computed.
        private int 				binNr;                  // total number of bins.
        public FlowJFlow 		        flow, trueFlow;
        private float 			binWidth;
        private float [] 		        binPsi;
        private float [] 		        binStdDev;
        private float [] 		        density;
        private float [] 		        binMax;
        private float [] 		        binMin;
        private float [] 		        binDeltaAngle;
        private float [] 		        binDeltaLength;
        private int [] 			n;
        private int [] 			total;
        private int  			width, height;

        /* Create a new error field. */
        public FlowJError(int width, int height)
        {
		this.width = width;
		this.height = height;
		psi = new float[height][width];
		angular = new float[height][width];
		delta = new float[height][width][2];
		hasflow = new boolean[height][width];
        }
	public float xExpected(int x, int y)
	{   return trueFlow.getX(x, y);   }
	public float yExpected(int x, int y)
	{   return trueFlow.getY(x, y);   }
	public float psi(int x, int y)
	{ return psi[y][x]; }
        private static float PsiErr(float[] ve, float [] va)
        {
		  float [] veNorm = new float[3];
		  float [] vaNorm = new float[3];
		  veNorm[0] = ve[0];
		  veNorm[1] = ve[1];
		  veNorm[2] = 1;

		  vaNorm[0] = va[0];
		  vaNorm[1] = va[1];
		  vaNorm[2] = 1;

		  float v = (veNorm[0]*vaNorm[0]+veNorm[1]*vaNorm[1]+1)/(BIJmatrix.norm(vaNorm)*BIJmatrix.norm(veNorm));

		  /**  sometimes roundoff error causes problems **/
		  if(v > 1 && v < 1.0001) v = 1;

		  float r =  (float) Math.acos(v)*180/ (float) Math.PI;

		  if (r < 0 || r >= 180)
			   IJ.error("ERROR in PsiErr() " + r + " " + v + "\nActual " + va[0] + ", " + va[1] + " expected " + ve[0] + " ," + ve[1]);
		  return r;
	}
        private static float PsiEN(float[] ve, float [] va)
        {
		  float [] veNorm = new float[2];
		  float r;
		  float nva = BIJmatrix.norm(va);
		  float nve = BIJmatrix.norm(ve);
		  if (nve > 0.00000001)
		  {
			  veNorm[0] = (float) (ve[0]/nve);
			  veNorm[1] = (float) (ve[1]/nve);
			  r = (va[0] * veNorm[0] + va[1] * veNorm[1] - nve) ;
			  float t = r /( (float) Math.sqrt(1 + nva*nva) *  (float) Math.sqrt(1 + nve*nve));
			  r = (float) Math.asin(t) * 180 / (float) Math.PI;

			  if(r < -90 || r > 90)
				  IJ.error("ERROR in PsiEN() r " + r + " t " + t);
		  }
		  else r = 100;
		  return Math.abs(r);
	}
        /*
	  compute the average error over an area defined in Roi.
	  Return the average error.
   */
   public float average(Roi roi)
   {
		  Rectangle r = roi.getBoundingRect();
		  float psiSum = 0;
		  int s = 0;
		  for (int y = 0; y < r.height; y++)
		  {
			for (int x = 0; x < r.width; x++)
			{
				if (! (roi instanceof Roi) || roi.contains(r.x + x, r.y + y))
				{
					psiSum += psi[r.y + y][r.x + x];
					s++;
				}
			}
		  }
		  return psiSum / (float) s;
	}
	public void computePsi(FlowJFlow flow, FlowJFlow trueFlow)
	/*
		Compute the psi in flow compared to the true flow field in trueFlow.
		Error analysis in accordance with Barron, 1994
	*/
	{
		this.flow = flow;
		this.trueFlow = trueFlow;
		int full = 0; int total = 0;
		float sumPsi = 0;
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
			{
				psi[y][x] = 0;
				if (flow.valid(x,y))
				{
					  float [] actA = FlowJFlow.polar(flow.getX(x, y), flow.getY(x, y));
					  // keep the angular velocity, which is the actual one.
					  // Compute true angular velocity.
					  float [] trueA = FlowJFlow.polar(trueFlow.getX(x, y), trueFlow.getY(x, y));
					  angular[y][x] = (float) trueA[0];
					  boolean f1 = flow.full[y][x];
					  boolean f2 = (flow.getX(x, y) == 100 && flow.getY(x, y) == 100);
					  boolean f3 = trueFlow.full[y][x];
					  boolean f4 = (trueFlow.getX(x, y) == 100 && trueFlow.getY(x, y) == 100);
					  // externalised tests. bug in Java compiler.
					  if (! f1 || f2 || ! f3  || f4)
					  {
							// No error, because algorithm knew it could not estimate
							// or no true velocity known.
							hasflow[y][x] = false;
					  }
					  else
					  {
							// There is an estimate, now calculate the error.
							hasflow[y][x] = true;
						   // calculate psi error in accordance with Barron.
							psi[y][x] = (float) PsiErr(flow.get(x, y), trueFlow.get(x, y));
							sumPsi += psi[y][x];
							// Also compute separate angle/length errors.
							// Compute fractional error in length.
							delta[y][x][0] = (float) (actA[0]-trueA[0]);
							// Compute absolute error in angle.
							delta[y][x][1] = (float) ((actA[1]-trueA[1])*180/Math.PI);
							// Arg.
							if (delta[y][x][1] >= 360)
								  delta[y][x][1] -= 360;
							if (delta[y][x][1] <= -360)
								  delta[y][x][1] += 360;
							if (Math.abs(delta[y][x][1]) > 180)
								  delta[y][x][1] = - (360-Math.abs(delta[y][x][1]));
							full++;
					  }
					  total++;
				}
			}
		IJ.write("Density "+IJ.d2s(100*(float)full /(float)total,2)+
				"%; psi average: "+IJ.d2s(sumPsi / (float) full, 2));
	}
	public static void map(Graphics g, int width, int height, FlowJFlow flow, FlowJFlow trueFlow)
	/*
		Map the correlation of true flow to estimated flow as a density in a graph.
	*/
	{
		  final int MARGIN = 5;

		height = (int) Math.min(height-MARGIN-2, width-MARGIN-2);
		float max = 4;       // max velocity to be mapped.
		float scaling = (float) height / max;
		Rectangle frame = new Rectangle(MARGIN+2, 0, height-1, height);
		// the ideal relationship true == est.
		g.setColor(new Color(0xff3d3d));
		// g.drawLine(frame.x, frame.y+frame.height, frame.x + frame.width, frame.y);
		int [][] duplicate = new int[height+1][height+1];
		for (int y = 0; y < flow.getHeight(); y++)
			for (int x = 0; x < flow.getWidth(); x++)
			{
				if (flow.valid(x,y))
				{
					  float [] actA = FlowJFlow.polar(flow.getX(x, y), flow.getY(x, y));
					  float [] trueA = FlowJFlow.polar(trueFlow.getX(x, y), trueFlow.getY(x, y));
					  if (actA[0] < max && trueA[0] < max && actA[0] >= 0 && trueA[0] >= 0)
					  {
							int xC = (int) (trueA[0]*scaling+0.5);
							int yC = (int) (actA[0]*scaling+0.5);
							duplicate[yC][xC]++;
							int i = 255-duplicate[yC][xC]*85;
							int p = ((i & 255) << 16) | ((i & 255) << 8) | (i & 255);
							g.setColor(new Color(p));
							g.drawLine(frame.x+xC, frame.y+frame.height-yC,
								  frame.x + xC, frame.y+frame.height-yC);
					  }
				}
			}
		// Draw a nice frame.
		g.setColor(new Color(0x0));
		g.drawLine(0, frame.y+frame.height, frame.x+frame.width, frame.y+frame.height);
		g.drawLine(frame.x, frame.y, frame.x, frame.y+frame.height);
		// make the tick marks
		for (float d=0; d <= max; d++)
		{
				g.drawLine(frame.x+(int)(d*scaling+0.5), frame.y+frame.height,
					frame.x+(int)(d*scaling+0.5), frame.y+frame.height+MARGIN);
				g.drawLine(frame.x - MARGIN, frame.y+frame.height-(int)(d*scaling+0.5),
						frame.x, frame.y+frame.height-(int)(d*scaling+0.5));
		}
	}
	public void map(ColorProcessor ip)
	/*
		Map the psi field into a fitting pseudocolor image.
		The higher psi,the greater the intensity
	*/
	{
			int[] pixels = (int[])ip.getPixels();

		int p = 0;
			for (int y = 0; y < height; y++)
		{
				for (int x = 0; x < width; x++)
		  {
				int r, g, b;

				int i;
				if (hasflow[y][x])
					  i = (int) ((psi[y][x] / 180) * (255-100)+100);
				else
					  i = 0;
				r = (int) i;
				g = (int) i;
				b = (int) i;
				pixels[p++] = ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
		  }
			  }
	}
	public void averageOverVelocity()
	/*
	  Do statistics.
	*/
	{
		binWidth = 0.1f;
		binNr = 60;
		binPsi = new float[binNr];
		total = new int[binNr];
		density = new float[binNr];
		n = new int[binNr];
		binStdDev = new float[binNr];
		binMin = new float[binNr];
		binMax = new float[binNr];
		binDeltaAngle = new float[binNr];
		binDeltaLength = new float[binNr];
		// Compute range errors (for speeds 0-partMax).
		float rangeMax = 3;
		float rangePsi = 0; int rangeN = 0; int rangeS = 0; float rangeStddev = 0;
		float rangeLength = 0;

		for (int i = 0; i < binNr; i++)
		{    binPsi[i] = binStdDev[i] = binDeltaLength[i] = binDeltaAngle[i] = n[i] = total[i] = 0; binMin[i] = Float.MAX_VALUE; binMax[i] = - Float.MAX_VALUE;}
			// fill the bins.
		int validSurface = 0;
		for (int y = 0; y < height; y++)
				  for (int x = 0; x < width; x++)
			{
					if (flow.valid(x,y))
					{
						  // Compute average psi, density and stddev for a range.
						  if (angular[y][x] < rangeMax)
						  {
								if (hasflow[y][x])
								{
									  rangePsi += psi[y][x];
									  rangeStddev += psi[y][x]*psi[y][x];
									  rangeLength += delta[y][x][0];
									  rangeN++;
								}
								rangeS++;
						  }
						  // which bin should it be in?
						  int i = (int) ((angular[y][x]) / binWidth);  // from (n-1) - (n) binWidths
						  // if in an interesting range.
						  if (i < binNr)
						  {
								// Only computed velocities are counted.
								if (hasflow[y][x])
								{
									  // compute average psi.
									  binPsi[i] += psi[y][x];
									  // compute stddev
									  binStdDev[i] += psi[y][x] * psi[y][x];  // first sum the squares.
									  // compute difference in speed.
									  binDeltaLength[i] += delta[y][x][0];
									  // compute difference in orientation.
									  binDeltaAngle[i] += delta[y][x][1];
									  // compute n.
									  n[i]++;
									  // compute min and max.
									  if (binMax[i] < psi[y][x])
											binMax[i] = psi[y][x];
									  if (binMin[i] > psi[y][x])
											binMin[i] = psi[y][x];
								}
								// total surface within bin.
								total[i]++;
						  }
						  validSurface++;
					}
			}
		rangePsi /= (float) rangeN;
		float std = Math.abs((rangeStddev - rangeN*rangePsi*rangePsi)/(rangeN-1));
		rangeStddev = (float) Math.sqrt(std);
		float rangeDensity = (float) rangeN / (float) rangeS;
		IJ.write("Average psi (0.0-"+rangeMax+"): "+IJ.d2s(rangePsi,2)+" stddev: "
			  +IJ.d2s(rangeStddev,2)+" ("+IJ.d2s(rangeDensity*100,2)+"%)"+" relative speed error: "+IJ.d2s(rangeLength/rangeN,2));
		 // over n.
		for (int i = 0; i < binNr; i++)
		{
			  if (n[i] > 0)
			  {
				  binPsi[i] /= (float) n[i];
				  binDeltaAngle[i] /= (float) n[i];
				  binDeltaLength[i] /= (float) n[i];
				  if (n[i] > 1)
					  binStdDev[i] = (float) Math.sqrt(Math.abs((binStdDev[i]-n[i]*binPsi[i] * binPsi[i]) / (n[i] - 1)));
				  else
					  binStdDev[i] = 0;
			  }
			  density[i] = (float) n[i] / (float) validSurface;
		}
   }
	public void mapAverage(Graphics g)
	{
		final int WIDTH = 257;
		final int HEIGHT = 128;
		  final int BAR_HEIGHT = 12;
		  final int XMARGIN = 20;
		  final int YMARGIN = 10;

		float scaling = (float) HEIGHT / 180;
		int xScale = WIDTH / binNr;
		Rectangle frame = new Rectangle(XMARGIN, YMARGIN, WIDTH, HEIGHT);
		g.drawRect(frame.x, frame.y, frame.width, frame.height);
			for (int i = 0; i < binNr; i++)
			g.drawLine(i*xScale + XMARGIN, YMARGIN + HEIGHT, i*xScale + XMARGIN, YMARGIN + HEIGHT - ((int)(binPsi[i]* scaling)));
		g.drawString("" + 180 + "º", 0, YMARGIN);
		int y = YMARGIN + HEIGHT + 20;
		g.drawString("0.0", (int) (xScale * 0) + XMARGIN, y);
		g.drawString("1.0", (int) (xScale * 1 / binWidth) + XMARGIN, y);
		g.drawString("2.0", (int) (xScale * 2 / binWidth) + XMARGIN, y);
		g.drawString("3.0", (int) (xScale * 3 / binWidth) + XMARGIN, y);
		g.drawString("4.0", (int) (xScale * 4 / binWidth) + XMARGIN, y);
		g.drawString("5.0", (int) (xScale * 5 / binWidth) + XMARGIN, y);
		y += 20;
		g.drawString("(" + IJ.d2s(binPsi[(int) (0 / binWidth)], 2) + "º)", (int) (xScale * 0 / binWidth) + XMARGIN, y);
		g.drawString("(" + IJ.d2s(binPsi[(int) (1 / binWidth)], 2) + "º)", (int) (xScale * 1 / binWidth) + XMARGIN, y);
		g.drawString("(" + IJ.d2s(binPsi[(int) (2 / binWidth)], 2) + "º)", (int) (xScale * 2 / binWidth) + XMARGIN, y);
		g.drawString("(" + IJ.d2s(binPsi[(int) (3 / binWidth)], 2) + "º)", (int) (xScale * 3 / binWidth) + XMARGIN, y);
		g.drawString("(" + IJ.d2s(binPsi[(int) (4 / binWidth)], 2) + "º)", (int) (xScale * 4 / binWidth) + XMARGIN, y);
		g.drawString("(" + IJ.d2s(binPsi[(int) (5 / binWidth)], 2) + "º)", (int) (xScale * 5 / binWidth) + XMARGIN, y);
	 }
	 public void clipboard(PrintWriter pw)
	 /* Copy the error statistics to the clipboard in MS Excel readable format. */
	 {
			pw.print("v-ang\t" + "psi "+flow.toString() + "\tstddev\tdelta l"
			  +"\tdelta angle\tdensity\n");
		for (int i = 0; i < binNr; i++)
		   pw.print(IJ.d2s(i*binWidth, 2) + "\t" + IJ.d2s(binPsi[i], 2)
				  + "\t" + IJ.d2s(binStdDev[i], 2) + "\t" + IJ.d2s(binDeltaLength[i], 2)
				  + "\t" + IJ.d2s(binDeltaAngle[i], 2) + "\t" + IJ.d2s(density[i]*100,3)+"%\n");
	 }
}

