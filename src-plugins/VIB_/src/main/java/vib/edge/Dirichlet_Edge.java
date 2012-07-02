package vib.edge;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import inference.LogFuncs;

import java.awt.Polygon;

import vib.InterpolatedImage;
import vib.MaxEntHistogram;
import vib.VIB;

/* Dirichlet evidence:
 *
 * given two histograms {m_i} and {n_i},
 *
 * Evidence=\prod_i \frac{m_i!n_i!}{(m_i+n_i+1)!}
 *
 *
 * Endres-Schindelin metric:
 *
 * given two histograms {p_i} and {q_i},
 *
 * D^2_{PQ}=\sum_i p_i\log\frac{2p_i}{p_i+q_i}+q_i\log\frac{2q_i}{p_i+q_i}
 *
 * if the sum is calculated with counts (n_i) instead of probabilities (p_i),
 * the result only differs by the factor \sum_i n_i
 *
 * Dirichlet mutual information:
 *
 * given two histograms {n_i} and {m_i},
 *
 * I=\sum_{j=N+M+1}^{L}\frac 1j
 *   -\frac 1L \sum_i ( (n_i+1)\sum_{j=n_i+2}^{n_i+m_i+2} \frac 1j
 *                     +(m_i+1)\sum_{j=m_i+2}^{n_i+m_i+2} \frac 1j )
 * where N=\sum_i n_i=\sum_i m_i, i=1,..,M, L=2(M+N)
 *
 * in this context, we forget about the first term, and also about the
 * factor \frac 1L.
 */

public class Dirichlet_Edge implements PlugInFilter {
	ImagePlus image;
	ImageStack angleStack;

	final static String[] types={"MutualInformation","DirichletMutualInformation","Dirichlet","Jensen","Endres","Euclidean","DirichletVarianceMutualInformation","Dirichlet2"};

	private int maxBin=255;

	public void run(ImageProcessor ip) {
		ImageStack stack = image.getStack();

		GenericDialog gd = new GenericDialog("Parameters");
		gd.addChoice("measure",types,"DirichletMutualInformation");
		gd.addNumericField("radius", 7, 2);
		gd.addCheckbox("useSelection", false);
		gd.addCheckbox("showSelection", false);
		gd.addCheckbox("3d",(stack.getSize()>1));
		String[] histoPost={"none","constant","binom","maxEnt"};
		gd.addChoice("histogramPostProcessing",histoPost,"none");
		gd.addCheckbox("showAngles",false);
		gd.addCheckbox("showEdgelets",false);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		String measureString=gd.getNextChoice();
		Measure measure;
		if(measureString.equals("DirichletMutualInformation"))
			measure=new DirichletMutualInformation();
		else if(measureString.equals("Endres"))
			measure=new Endres();
		else if(measureString.equals("MutualInformation"))
			measure=new MutualInformation();
		else if(measureString.equals("Jensen"))
			measure=new Jensen();
		else if(measureString.equals("Euclidean"))
			measure=new Euclidean();
		else if(measureString.equals("DirichletVarianceMutualInformation"))
			measure=new DirichletVarianceMutualInformation();
		else if(measureString.equals("Dirichlet2"))
			measure=new Dirichlet2();
		else
			measure=new Dirichlet();
		double radius = gd.getNextNumber();
		boolean useSelection = gd.getNextBoolean();
		boolean showSelection = gd.getNextBoolean();
		boolean use3d = gd.getNextBoolean();
		int postProc = gd.getNextChoiceIndex();
		boolean showAngles = gd.getNextBoolean();
		boolean showEdgelets = gd.getNextBoolean();

		ImageStack res = new ImageStack(stack.getWidth(), stack.getHeight());
		if(angleStack == null)
			angleStack = new ImageStack(stack.getWidth(), stack.getHeight());

		InterpolatedImage ii = new InterpolatedImage(image);

		if(use3d) {
			do3d(image,res,measure,radius);
		} else 
			for (int s = 1; s <= stack.getSize(); s++) {
				if(useSelection)
					res.addSlice("", doitWithSelection(stack.getProcessor(s), measure, radius, showSelection));
				else
					res.addSlice("", doit(ii, s - 1, measure, radius, postProc, showAngles, showEdgelets));
			}
		if(showAngles)
			new ImagePlus("Angles of "+measureString+radius, angleStack).show();
		new ImagePlus(measureString+radius, res).show();
	}

	static interface Measure {
		double doit(int count1,int count2);
	}

	static interface MeasureWithNullModel extends Measure {
		// compute the null model
		double doitNull(int count1,int count2);
		// normalization factors. may differ for null hypothesis and not-null hypothesis
		// totcount: total number of points in full circle
		double getNormalize(int totcount,int numbins);
		double getNormalizeNull(int totcount,int numbins);
	}

	static class Dirichlet2 implements MeasureWithNullModel {
		public double doit(int count1,int count2) {
			return LogFuncs.LogFactorial(count1)+LogFuncs.LogFactorial(count2);
		}
		public double doitNull(int count1,int count2) {
			return LogFuncs.LogFactorial(count1+count2);
		}
		public double getNormalize(int totcount,int numbins) {
			return 2.0*(LogFuncs.LogFactorial(numbins)-LogFuncs.LogFactorial(totcount/2+numbins));
		}
		public double getNormalizeNull(int totcount,int numbins) {
			return LogFuncs.LogFactorial(numbins)-LogFuncs.LogFactorial(totcount+numbins);
		}
	}
		

	static class Endres implements Measure {
		public double doit(int count1,int count2) {
			int denom=count1+count2;
			if(denom==0)
				return 0;
			double result=0;
			if(count1!=0)
				result+=count1*Math.log(count1*2.0/denom);
			if(count2!=0)
				result+=count2*Math.log(count2*2.0/denom);
			return result;
		}
	}


	static class Dirichlet implements Measure {
		public double doit(int count1,int count2) {
			return LogFuncs.LogFactorial(count1)+LogFuncs.LogFactorial(count2)-LogFuncs.LogFactorial(count1+count2+1);
		}
	}

	final static Harmonic _harmonic=new Harmonic();

	static class DirichletMutualInformation implements Measure {
		public double doit(int count1,int count2) {
			return -(count1+1)*_harmonic.get(count1+1,count1+count2+2)-(count2+1)*_harmonic.get(count2+1,count1+count2+2);
		}
	}

	static class DirichletVarianceMutualInformation extends DirichletMutualInformation {
		// this function wants the histograms with a horizontal boundary, and the index of halfCircle at which the maximal MutualInformation was obtained. It then approximates the variance by Hutter's formula
		public double variance(EdgeInformation edgeInfo,int i,int j,int halfCircleEndIndex) {
			int[][] privHisto = new int[2][edgeInfo.histograms[0].length];
			for(int k=0;k<edgeInfo.histograms[0].length;k++) {
				privHisto[0][k]=edgeInfo.histograms[0][k];
				privHisto[1][k]=edgeInfo.histograms[1][k];
			}
			for(int k=0;k<=halfCircleEndIndex;k++) {
				int value1=edgeInfo.ii.getNoInterpol(i+edgeInfo.halfCircle[k][0],j+edgeInfo.halfCircle[k][1], edgeInfo.slice);
				int value2=edgeInfo.ii.getNoInterpol(i-edgeInfo.halfCircle[k][0],j-edgeInfo.halfCircle[k][1], edgeInfo.slice);
				if(value1!=value2) {
					privHisto[0][value1]--;
					privHisto[1][value1]++;
					privHisto[0][value2]++;
					privHisto[1][value2]--;
				}
			}
			double n=edgeInfo.halfCircle.length*2,nIDot=0.5;
			double sum1=0,sum2=0;
			for(int k=0;k<privHisto[0].length;k++) {
				double nDotJ=privHisto[0][k]+privHisto[1][k];
//System.out.print(privHisto[0][k]+":"+privHisto[1][k]+" ");
				if(nDotJ!=0) {
					double nij=privHisto[0][k];
					double lg;
					if(nij!=0) {
						lg=Math.log(nij*n/nIDot/nDotJ);
						sum1+=nij/n*lg*lg;
						sum2+=nij/n*lg;
					}
					nij=privHisto[1][k];
					if(nij!=0) {
						lg=Math.log(nij*n/nIDot/nDotJ);
						sum1+=nij/n*lg*lg;
						sum2+=nij/n*lg;
					}
				}
			}
//System.out.println((sum1-sum2*sum2)/n);
			return (sum1-sum2*sum2)/n;
		}
	}

	static class MutualInformation implements Measure {
		public double doit(int count1,int count2) {
			int sum=count1+count2;
			if(sum==0)
				return 0;
			double result=-sum*Math.log(sum);
			if(count1!=0)
				result+=count1*Math.log(count1);
			if(count2!=0)
				result+=count2*Math.log(count2);
			return result;
		}
	}

	static class Jensen implements Measure {
		public double doit(int count1,int count2) {
			return count1*count2;
		}
	}

	static class Euclidean implements Measure {
		public double doit(int count1,int count2) {
			int diff=count1-count2;
			return diff*diff;
		}
	}

	static class Dummy implements Measure {
		public double doit(int count1,int count2) {
			return count1+count2;
		}
	}

	//int w,h;

	public static class PostProcessHistogram {
		int postProcessIndex;
		byte[] mapping;

		public PostProcessHistogram(int index) {
			postProcessIndex = index;
System.err.println("index: " + index);
		}

		public int[][] postProcessHisto(int[][] histograms) {
			if (postProcessIndex == 3) {
				MaxEntHistogram h = new MaxEntHistogram(histograms);
				//System.err.println("quantizing");
				h.quantize(4);
				mapping = h.getMapping(true);
				return h.get(histograms);
			}
			int[] h1=postProcessHisto(histograms[0]);
			int[] h2=postProcessHisto(histograms[1]);
			histograms[0]=h1;
			histograms[1]=h2;
			return histograms;
		}

		//final static int postProcessWidth=256/20;
		final static int postProcessWidth=20;
		double pp2[];

		int[] postProcessHisto(int[] histogram) {
			int length=histogram.length;
			int[] result=new int[length];

			if(postProcessIndex==2 && pp2==null) {
				pp2=new double[2*postProcessWidth+1];
				pp2[0]=1;
				for(int i=-postProcessWidth+1;
						i<=postProcessWidth;i++) {
					pp2[i+postProcessWidth]=
						pp2[i+postProcessWidth-1]*
						(2*postProcessWidth+1-
						 (i+postProcessWidth))/
						(i+postProcessWidth);
				}
			}
			for(int i=0;i<length;i++) {
				int min=(i>postProcessWidth?
						i-postProcessWidth:0);
				int max=(i<length-1-postProcessWidth?
						i+1+postProcessWidth:
						length+1-1);
				if(postProcessIndex==1) {
					for(int j=min;j<max;j++)
						result[i]+=histogram[j];
					result[i]/=(max-min);
				} else if(postProcessIndex==2) {
					double total=0;
					double r=0;
					for(int j=min;j<max;j++) {
						double value=pp2[postProcessWidth+j-i];
						total+=value;
						r+=histogram[j]*value;
					}
					if(r<0)
						throw new RuntimeException(
								"result["+i+
								"]="+result[i]);
					result[i]=(int)(r/total);
				}
			}
			return result;
		}
	};

	static class EdgeInformation3d {
		InterpolatedImage image;
		Measure measure;
		MeasureWithNullModel nullMeasure;
		double radius;

		int[][] sphere;
		int[][] histograms;


		public EdgeInformation3d(ImagePlus ip, Measure m, double r) {
			this(new InterpolatedImage(ip),
					m, r);
		}

		public EdgeInformation3d(InterpolatedImage i,
				Measure m, double r) {
			image = i;
			measure = m;
			if (measure instanceof MeasureWithNullModel)
				nullMeasure = (MeasureWithNullModel)measure;
			radius = r;

			Calibration c = i.image.getCalibration();
			sphere = SphereIterators.SphereIterator(r,
					c.pixelWidth,  c.pixelHeight,
					c.pixelDepth);
			histograms = new int[2][256];
		}

		void calculateHistograms(int x, int y, int z,
				double nx, double ny, double nz) {
			for (int i = 0; i < sphere.length; i++) {
				int[] p = sphere[i];
				double dist = p[0] * nx + p[1] * ny + p[2] * nz;
				if (dist == 0)
					continue;
				int value = image.getNoInterpol(x + p[0],
						y + p[1], z + p[2]);
				histograms[dist > 0 ? 1 : 0][value]++;
			}
		}

		void resetHistograms() {
			for (int i = 0; i < histograms[0].length; i++)
				histograms[0][i] = histograms[1][i] = 0;
		}

		double get(int x, int y, int z,
				double nx, double ny, double nz) {
			resetHistograms();
			calculateHistograms(x, y, z, nx, ny, nz);
			double result = 0;
			for (int i = 0; i < histograms[0].length; i++)
				result += measure.doit(histograms[0][i],
						histograms[1][i]);
			if (nullMeasure != null) {
				result+=nullMeasure.getNormalize(sphere.length,
						256);

				double nullEv=0.0;
				double edgePrior=0.001;
				for(int k=0;k<histograms[0].length;k++)
					nullEv+=nullMeasure.doitNull(histograms[0][k],histograms[1][k]);
				nullEv+=nullMeasure.getNormalizeNull(sphere.length,
						256);
				nullEv+=Math.log(1.0-edgePrior);
				result+=Math.log(edgePrior);
				result=Math.exp(result-nullEv);
			}

			return result;
		}
	}

	static class EdgeInformation {
		InterpolatedImage ii;
		Measure measure;
		MeasureWithNullModel nullMeasure;
		double radius;
		int maxBin, numberOfBins;
		private PostProcessHistogram postProc;

		int slice;
		int[][] fullCircle,halfCircle;
		int[][] histograms;

		public EdgeInformation(ImagePlus image, Measure measure,
				double radius, int maxBin, int numberOfBins) {
			this(new InterpolatedImage(image), measure, radius,
					maxBin, numberOfBins);
			slice = image.getCurrentSlice() - 1;
		}

		public EdgeInformation(InterpolatedImage image, Measure measure, double radius, int maxBin, int numberOfBins) {
			ii = image;
			this.measure = measure;
			if (measure instanceof MeasureWithNullModel)
				nullMeasure = (MeasureWithNullModel)measure;
			this.radius = radius;
			this.maxBin = maxBin;
			this.numberOfBins = numberOfBins;

			histograms = new int[2][maxBin+1];

			fullCircle = CircleIterators.FullCircle(radius,false);
			halfCircle = CircleIterators.SortedHalfCircle(radius);
			int[][] sickle = CircleIterators.RightSickle(radius);
		}

		void setPostProc(int index) {
			if (index != 0)
				postProc = new PostProcessHistogram(index);
		}

		void resetHistograms() {
			histograms = new int[2][maxBin + 1];
			/*
			for(int i=0;i<histograms[0].length;i++) {
				histograms[0][i]=0;
				histograms[1][i]=0;
			}
			*/
		}

		void calculateHistograms(int x, int y, int z) {
			resetHistograms();
			for(int k=0;k<fullCircle.length;k++) {
				int i=x+fullCircle[k][0],j=y+fullCircle[k][1];
				int value = ii.getNoInterpol(i, j, z);
				/* upper half? */
				if(fullCircle[k][1]>0 || (fullCircle[k][1]==0 && fullCircle[k][0]>0))
					histograms[0][value]++;
				else
					histograms[1][value]++;

			}
			if (postProc != null) {
				histograms = postProc.postProcessHisto(histograms);
				System.err.println("quantized for " + x + ", " + y + ", " + z);
			}
		}

		/* the same as above, but draw the line perpendicular to (xperp;yperp) */
		void calculateHistograms(int x, int y, int z,
				double xperp, double yperp) {
			resetHistograms();
			for(int k=0;k<fullCircle.length;k++) {
				int i=x+fullCircle[k][0],j=y+fullCircle[k][1];
				int value = ii.getNoInterpol(i, j, z);
				/* upper half? */
				double scalarProduct = fullCircle[k][0]*xperp+fullCircle[k][1]*yperp;
				if(scalarProduct>0)
					histograms[0][value]++;
				else if(scalarProduct<0)
					histograms[1][value]++;
			}

			if (postProc != null)
				histograms = postProc.postProcessHisto(histograms);
		}

		private int angleIndex;

		double getEdgeInformation(int i,int j,
				double xPerp, double yPerp) {
			return getEdgeInformation(i, j, slice, xPerp, yPerp);
		}

		double getEdgeInformation(int i,int j, int z,
				double xPerp, double yPerp) {
			calculateHistograms(i, j, z, xPerp, yPerp);
			double value = 0;
			for(int k=0;k<histograms[0].length;k++)
				value+=measure.doit(histograms[0][k],histograms[1][k]);
			if (nullMeasure != null) {
				value+=nullMeasure.getNormalize(fullCircle.length,numberOfBins);

				double nullEv=0.0;
				double edgePrior=0.001;
				for(int k=0;k<histograms[0].length;k++)
					nullEv+=nullMeasure.doitNull(histograms[0][k],histograms[1][k]);
				nullEv+=nullMeasure.getNormalizeNull(fullCircle.length,numberOfBins);
				nullEv+=Math.log(1.0-edgePrior);
				value+=Math.log(edgePrior);
				value=Math.exp(value-nullEv);
			}

			return value;
		}

		double getEdgeInformation(int i,int j) {
			return getEdgeInformation(i, j, slice);
		}

		double getEdgeInformation(int i, int j, int z) {
			calculateHistograms(i, j, z);
			double value = 0;
			for(int k=0;k<histograms[0].length;k++)
				value+=measure.doit(histograms[0][k],histograms[1][k]);
			if (nullMeasure != null)
				value+=nullMeasure.getNormalize(fullCircle.length,numberOfBins);

			double maxValue=value,tempValue=value;
			double edgeEvidenceSum=-1e300;
			angleIndex=0;

			for(int k=0;k<halfCircle.length;k++) {
				int value1 = ii.getNoInterpol(
						i + halfCircle[k][0],
						j + halfCircle[k][1], z);
				int value2 = ii.getNoInterpol(
						i - halfCircle[k][0],
						j-halfCircle[k][1], z);
				if(value1!=value2) {
					if (postProc != null && postProc.mapping != null) {
						value1 = postProc.mapping[value1];
						if (value1 < 0)
							value1 += 256;
						value2 = postProc.mapping[value2];
						if (value2 < 0)
							value2 += 256;
					}
					double old=measure.doit(histograms[0][value1],histograms[1][value1])
						+measure.doit(histograms[0][value2],histograms[1][value2]);
					histograms[0][value1]--;
					histograms[1][value1]++;
					histograms[0][value2]++;
					histograms[1][value2]--;
					double diff=measure.doit(histograms[0][value1],histograms[1][value1])
						+measure.doit(histograms[0][value2],histograms[1][value2])-old;
					tempValue+=diff;
					if(tempValue>maxValue) {
						maxValue=tempValue;
						angleIndex=k;
					}
				}
				edgeEvidenceSum=LogFuncs.LogAddLogLog(edgeEvidenceSum,tempValue);
			}


			if(nullMeasure != null) {
				double nullEv=0.0;
				double edgePrior=0.001;
				for(int k=0;k<histograms[0].length;k++)
					nullEv+=nullMeasure.doitNull(histograms[0][k],histograms[1][k]);
				nullEv+=nullMeasure.getNormalizeNull(fullCircle.length,numberOfBins);
				nullEv+=Math.log(1.0-edgePrior);
				edgeEvidenceSum+=Math.log(edgePrior);
				maxValue+=Math.log(edgePrior);
				maxValue=Math.exp(maxValue-LogFuncs.LogAddLogLog(edgeEvidenceSum,nullEv));
			}

			if(var!=null)
				calculateVariance(maxValue,i,j);

			if(edgelets!=null)
				drawEdgelet(maxValue,i,j);

			if(angles!=null)
				setAngle(i,j);

			return maxValue;
		}

		// angles image

		double[] angles;

		void setAngle(int i, int j) {
			angles[i+j*ii.getWidth()]=Math.atan2(halfCircle[angleIndex][0],halfCircle[angleIndex][1]);
		}

		// variance

		DirichletVarianceMutualInformation var = null;
		double maxMutInf=1e-300,maxVarMutInf=1e-300,maxMutInfRatio=1e-300;

		void calculateVariance(double maxValue,int i, int j) {
			double maxValueBup=maxValue;
			if(maxMutInf<maxValue)
				maxMutInf=maxValue;
			calculateHistograms(i, j, slice);
			maxValue = var.variance(this,i,j,angleIndex);
			if(maxVarMutInf<maxValue)
				maxVarMutInf=maxValue;
			if(maxValueBup!=0) {
				double v=Math.sqrt(maxValue)/maxValueBup;
				if(v>maxMutInfRatio)
					maxMutInfRatio=v;
			}
		}

		double getMaxVariance() {
			return _harmonic.get(4*halfCircle.length)
				-_harmonic.get(2*halfCircle.length)
				+1/(4.0*halfCircle.length)*maxMutInf;
		}

		// for edglet picture
		int[][][] edgelets=null;
		double[] res3=null;

		void initEdgelets() {
			edgelets=CircleIterators.Edgelets(halfCircle,radius);
			res3=new double[ii.getWidth() * ii.getHeight()];
		}

		void drawEdgelet(double maxValue,int i,int j) {
			int x,y,l;
			for(l=0;l<edgelets[angleIndex].length;l++) {
				x=i+edgelets[angleIndex][l][0];
				y=j+edgelets[angleIndex][l][1];
				if(x<0 || x>=ii.getWidth()) continue;
				if(y<0 || y>=ii.getHeight()) continue;
				if(res3[x+y*ii.getWidth()]<maxValue) res3[x+y*ii.getWidth()]=maxValue;
			}
		}

		void postLudium() {
			if(var!=null) {
				double realMaxMutInf = getMaxVariance();
				IJ.write("maximal MutInf: "+realMaxMutInf+", maximal variance: "+maxVarMutInf+" (std. dev.: "+Math.sqrt(maxVarMutInf)+"), ratio: "+maxMutInfRatio);
			}

			if(res3!=null)
				new ImagePlus("Edgelets", new FloatProcessor(ii.getWidth(), ii.getHeight(), res3)).show();
		}
	}

	private FloatProcessor doit(InterpolatedImage ii, int slice, Measure measure, double radius, int postProcessHistogram, boolean showAngles, boolean showEdgelets) {

		int w=ii.getWidth();
		int h=ii.getHeight();

		double[] res2=new double[w*h];

		// number of bins
		int nbins=0;
		boolean isfull[]=new boolean[256];
		for(int i=0;i<w;i++)
		 for(int j=0;j<w;j++)
			isfull[ii.getNoInterpol(i, j, slice)]=true;
		maxBin=0;
		for(int i=0;i<256;i++)
			if(isfull[i]) {
				nbins++;
				maxBin=i;
			}
		VIB.println("Number of values in image is "+nbins);

		EdgeInformation edgeInfo = new EdgeInformation(ii, measure, radius, maxBin, nbins);
		edgeInfo.slice = slice;
		edgeInfo.setPostProc(postProcessHistogram);

		if(measure instanceof DirichletVarianceMutualInformation)
			edgeInfo.var = (DirichletVarianceMutualInformation)measure;

		if(showAngles)
			edgeInfo.angles=new double[w*h];

		if(showEdgelets)
			edgeInfo.initEdgelets();

		for(int j=0;j<h;j++) {
			for(int i=0;i<w;i++) {
				/* TODO:
				if(postProcessHistogram>0)
					histograms=postProcessHisto(histograms,postProcessHistogram);
				*/

				res2[i+j*w]=edgeInfo.getEdgeInformation(i,j);
			}
			IJ.showProgress(j+1,h);
		}

		edgeInfo.postLudium();

		if (showAngles)
			angleStack.addSlice("", new FloatProcessor(w,h,edgeInfo.angles));

		FloatProcessor ip2 = new FloatProcessor(w, h, res2);

		return ip2;
	}

	public void do3d(ImagePlus stack,ImageStack result,Measure measure,double radius) {
		InterpolatedImage input=new InterpolatedImage(stack);

		// get FullSphereIterator, SphereIteratorsIterator
		int[][] sphereIterator=SphereIterators.SphereIterator(radius);
		double[] normal={1,0,0};
		GenericDialog gd=new GenericDialog("Normal Coordinates");
		gd.addNumericField("x",normal[0],3);
		gd.addNumericField("y",normal[1],3);
		gd.addNumericField("z",normal[2],3);
		gd.showDialog();
		if(!gd.wasCanceled()) {
			normal[0]=gd.getNextNumber();
			normal[1]=gd.getNextNumber();
			normal[2]=gd.getNextNumber();
		}

		// for(x,y,z)
		int w=input.getWidth(),h=input.getHeight(),d=input.getDepth();
		IJ.showProgress(0);
		for(int z=0;z<d;z++) {
		//for(int z=d/2;z<d/2+1;z++) {
			double[] slice=new double[w*h];
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					double value=0;
					// getHistograms
					int[][] histograms=new int[3][maxBin+1];
					for(int i=0;i<sphereIterator.length;i++)
						try {
							int v = input.getNoInterpol(x+sphereIterator[i][0],y+sphereIterator[i][1],z+sphereIterator[i][2]);
							histograms[SphereIterators.isUpperHalf(sphereIterator[i],normal)?0:1][v]++;
						} catch(Exception e) { /* do not count */ }
					for(int k=0;k<histograms[0].length;k++)
						value+=measure.doit(histograms[0][k],histograms[1][k]);

					// set maximum
					slice[x+w*y]=value;
				}
				IJ.showProgress((y+1.0)/h);
			}
			result.addSlice("", new FloatProcessor(w, h, slice));
			IJ.showProgress((z+1.0)/d);
		}
		IJ.showProgress(1.0);
	}

	public void do3d_complete(ImagePlus stack,ImageStack result,Measure measure,double radius) {
		InterpolatedImage input=new InterpolatedImage(stack);

		// get FullSphereIterator, SphereIteratorsIterator
		int[][] sphereIterator=SphereIterators.SphereIterator(radius);
		double[][] normals=SphereIterators.SampleSphereSurface(radius,10);
		int[][][] iterIterator=SphereIterators.HalfSphereIteratorsIterator(radius,normals);

		// for(x,y,z)
		int w=input.getWidth(),h=input.getHeight(),d=input.getDepth();
		IJ.showProgress(0);
		//for(int z=0;z<d;z++) {
		for(int z=d/2;z<d/2+1;z++) {
			double[] slice=new double[w*h];
			for(int y=0;y<h;y++) {
				for(int x=0;x<w;x++) {
					double value=0;
					// getHistograms
					int[][] histograms=new int[2][maxBin+1];
					for(int i=0;i<sphereIterator.length;i++)
						try {
							int v = input.getNoInterpol(x+sphereIterator[i][0],y+sphereIterator[i][1],z+sphereIterator[i][2]);
							histograms[SphereIterators.isUpperHalf(sphereIterator[i],normals[0])?0:1][v]++;
						} catch(Exception e) { /* do not count */ }
					for(int k=0;k<histograms[0].length;k++)
						value+=measure.doit(histograms[0][k],histograms[1][k]);

					double maxValue=value;
					// iterate via iteratorsIterator
					for(int i=0;i<iterIterator.length;i++) {
						for(int k=0;k<iterIterator[i].length;k++) {
							int value1=input.getNoInterpol(x+iterIterator[i][k][0],y+iterIterator[i][k][1],z+iterIterator[i][k][2]);
							int value2=input.getNoInterpol(x-iterIterator[i][k][0],y-iterIterator[i][k][1],z-iterIterator[i][k][2]);
							if(value1!=value2) {
								double old=measure.doit(histograms[0][value1],histograms[1][value1])+measure.doit(histograms[0][value2],histograms[1][value2]);
								histograms[0][value1]--;
								histograms[1][value1]++;
								histograms[0][value2]++;
								histograms[1][value2]--;
								double diff=measure.doit(histograms[0][value1],histograms[1][value1])+measure.doit(histograms[0][value2],histograms[1][value2])-old;
								value+=diff;
							}
						}
						// record maximum
						if(value>maxValue) {
							maxValue=value;
						}
					}
						
					// set maximum
					slice[x+w*y]=maxValue;
				}
				IJ.showProgress((y+1.0)/h);
			}
			result.addSlice("", new FloatProcessor(w, h, slice));
			IJ.showProgress((z+1.0)/d);
		}
		IJ.showProgress(1.0);
	}

	/* this function adds value to each pixel on the line (x1,y1)-(x2,y2) excluding (x2,y2) */
	private void line(double[] floats,int w,int h,int x1,int y1,int x2,int y2,double value) {
		float dx=Math.abs(x2-x1),dy=Math.abs(y2-y1);
		double count=(dx>dy?dx:dy);
		for(double i=0;i<count;i++) {
			int x=(int)(x1+(x2-x1)*i/count);
			int y=(int)(y1+(y2-y1)*i/count);
			if(x>=0 && y>=0 && x<w && y<h && value>floats[x+y*w])
				floats[x+y*w]=value;
		}
	}

	/* This function takes a polygon selection, and tests at each coordinate the likeliness
	   of that shape centered on this coordinate. That value is stored there. */
	private FloatProcessor doitWithSelection(ImageProcessor ip, Measure measure, double radius, boolean showSelection) {
/*TODO
		Roi roi = image.getRoi();
		if(roi == null || roi.getType()!=Roi.POLYGON) {
			IJ.error("Need a selection of type polygon");
			return null;
		}

		Polygon poly = ((PolygonRoi)roi).getPolygon();
		int[] xcoords = poly.xpoints, ycoords = poly.ypoints;
		int count = xcoords.length;
		int xcenter=0,ycenter=0,xmax=0,ymax=0;
		for(int i=0;i<count;i++) {
			xcenter+=xcoords[i];
			ycenter+=ycoords[i];
			if(xcoords[i]>xmax)
				xmax=xcoords[i];
			if(ycoords[i]>ymax)
				ymax=ycoords[i];
		}
		xcenter/=count;
		ycenter/=count;
		for(int i=0;i<count;i++) {
			xcoords[i]-=xcenter;
			ycoords[i]-=ycenter;
		}

		int r=1; //(radius*2<count || radius<1?(int)radius:1);
		int[] xperp = new int[count], yperp = new int[count];
		for(int i=0;i<count;i++) {
			int index1 = i-r, index2 = i+r;
			if(index1<0)
				index1+=count;
			if(index2>=count)
				index2-=count;
			xperp[i] = -(ycoords[index2]-ycoords[index1]);
			yperp[i] = +(xcoords[index2]-xcoords[index1]);
		}

		fullCircle = CircleIterators.FullCircle(radius,false);

		w=ip.getWidth();
		h=ip.getHeight();

		double[] res2=new double[w*h];
		if(showSelection) {
			for(int i=0;i<w*h;i++)
				res2[i]=-1e300;
		}

		for(int j=0;j<h;j++) {
			IJ.showProgress(j/(h-1.0));
			for(int i=0;i<w;i++) {
				/* /
				if(((i%90)!=0)||((j%90)!=0))
					continue;
				/** /
				double value = 0;
				for(int l=0;l<count;l++) {
					int[][] histograms=calculateHistograms(ip,i+xcoords[l],j+ycoords[l],xperp[l],yperp[l]);
					for(int k=0;k<histograms[0].length;k++)
						value+=measure.doit(histograms[0][k],histograms[1][k]);
				}

				if(!showSelection)
					res2[i+w*j] = value;
				else {
					int x=xcoords[count-1],y=ycoords[count-1];
					for(int l=0;l<count;l++) {
//IJ.write("line of color "+value+" from "+(i+x)+","+(j+y)+" to "+(i+xcoords[l])+","+(j+ycoords[l]));
						line(res2,w,h,i+x,j+y,i+xcoords[l],j+ycoords[l],value);
						x=xcoords[l]; y=ycoords[l];
					}
				}
			}
		}

		if(false && showSelection) {
			double minValue=1e300,maxValue=-1e300;
			for(int j=0;j<w*h;j++) {
				if(minValue>res2[j])
					minValue=res2[j];
				else if(maxValue<res2[j])
					maxValue=res2[j];
			}

			double[] res3 = new double[w*h];

			int xborder=xmax-xcenter,yborder=ymax-ycenter;
			for(int j=-yborder;j<h+yborder;j++)
				for(int i=-xborder;i<w+xborder;i++) {
					double value;
					if(i>=0 && j>=0 && i<w && j<h)
						value = res2[i+j*w];
					else
						value = minValue;
//					value=Math.exp

					int x=xcoords[count-1],y=ycoords[count-1];
					for(int l=0;l<count;l++) {
						line(res2,w,h,i+x,j+y,i+xcoords[l],j+ycoords[l],value);
						x=xcoords[l]; y=ycoords[l];
					}
				}
			res2 = res3;

			if(false) for(int i=0;i<4+xmax-xcenter;i++)
				for(int j=0;j<h;j++) {
					res2[i+j*w]=minValue;
					res2[w-1-i+j*w]=minValue;
				}
			if(false) for(int j=0;j<ymax-ycenter;j++)
				for(int i=xmax-xcenter;i<w-xmax+xcenter;i++) {
					res2[i+j*w]=minValue;
					res2[i+(h-1-j)*w]=minValue;
				}
		}

		FloatProcessor ip2 = new FloatProcessor(w, h, res2);

		return ip2;
*/ return null;
	}


	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}

}

class Harmonic {
	double[] cache;
	public Harmonic() {
		int count=1000000;
		cache=new double[count];
		cache[0]=1;
		for(int i=1;i<count;i++)
			cache[i]=cache[i-1]+1.0/(i+1.0);
	}
	public double get(int i) {
		return cache[i];
	}
	public double get(int i,int j) {
		return cache[j]-(i>0?cache[i-1]:0);
	}
}


