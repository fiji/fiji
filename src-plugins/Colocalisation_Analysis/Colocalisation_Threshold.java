//22/4/5

import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import java.text.DecimalFormat;
import ij.measure.*;
import ij.util.*;
import java.util.*;




public class Colocalisation_Threshold implements PlugIn {
	boolean headingsSetCTC;
	private static int index1=0;
	private static int index2=1;
	private static int indexMask;
	private static boolean displayCounts;
	private static boolean useMask;
	private static boolean useRoi;
	private Roi roi, roi2;
	private ImagePlus imp1, imp2, impMask;
	private static boolean threshold;
	private int  ch1, ch2, ch3, nslices, width, height;
	private int ch1Sum=0;
	private int ch2Sum=0;
	private int ch3Sum=0;
	private int N=0;
	private int indexRoi= (int)Prefs.get("CTC_indexRoi.int",0);
	private DecimalFormat df4 = new DecimalFormat("##0.0000");
	private DecimalFormat df3 = new DecimalFormat("##0.000");
	private DecimalFormat df2 = new DecimalFormat("##0.00");
	private DecimalFormat df1 = new DecimalFormat("##0.0");
	private DecimalFormat df0 = new DecimalFormat("##0");
	private static boolean colocValConst= Prefs.get("CTC_colocConst.boolean", false);
	private int dualChannelIndex = (int)Prefs.get("CTC_channels.int",0);
	private static boolean bScatter= Prefs.get("CTC_bScatter.boolean", false);
	private static boolean bShowLocalisation=Prefs.get("CTC_show.boolean", false);
	boolean opt0 = Prefs.get("CTC_opt0.boolean", true);
	boolean opt1 = Prefs.get("CTC_opt1.boolean", true);
	boolean opt1a = Prefs.get("CTC_opt1a.boolean", true);
	boolean opt2 = Prefs.get("CTC_opt2.boolean", true);
	boolean opt3a = Prefs.get("CTC_opt3a.boolean", true);
	boolean opt3b = Prefs.get("CTC_opt3b.boolean", true);
	boolean opt4 = Prefs.get("CTC_opt4.boolean", true);
	boolean opt5 = Prefs.get("CTC_opt5.boolean", true);
	boolean opt6 = Prefs.get("CTC_opt6.boolean", true);
	boolean opt7 = Prefs.get("CTC_opt7.boolean", true);
	boolean opt8 = Prefs.get("CTC_opt8.boolean", true);
	boolean opt9 = Prefs.get("CTC_opt9.boolean", true);
	boolean opt10 = Prefs.get("CTC_opt10.boolean", true);
	String[] dualChannels=  { "Red : Green","Red : Blue", "Green : Blue",};
	private int colIndex1 = 0;
	private int colIndex2 = 1;
	private int colIndex3 = 2;
	ImageProcessor ip1, ip2, ipmask;
	ColorProcessor ipColoc;
	private int rwidth, rheight, xOffset, yOffset;
	String[] chooseROI=  { "None","Channel 1", "Channel 2",};
	protected static TextWindow textWindow;

	public void run(String arg) {
		if (showDialog())
			correlate(imp1, imp2);
	}

	public boolean showDialog() {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.noImage();
			return false;
		}
		String[] titles = new String[wList.length];
		String[] chooseMask=  new String[wList.length+1];
		chooseMask[0]="<None>";

		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp!=null)
				if (imp!=null) {
					titles[i] = imp.getTitle();
					chooseMask[i+1] =imp.getTitle();
				} else	titles[i] = "";
		}
		if (index1>=titles.length)index1 = 0;
		if (index2>=titles.length)index2 = 0;
		if (indexMask>=titles.length)indexMask = 0;

		displayCounts = false;
		threshold = false;
		int indexMask=0;
		GenericDialog gd = new GenericDialog("Colocalisation Thresholds");
		gd.addChoice("Channel 1", titles, titles[index1]);
		gd.addChoice("Channel 2", titles, titles[index2]);
		gd.addChoice("Use ROI", chooseROI, chooseROI[indexRoi]);

		//	gd.addChoice("Mask channel", chooseMask, chooseMask[indexMask]);
		gd.addChoice("Channel Combination", dualChannels, dualChannels[dualChannelIndex]);
		gd.addCheckbox("Show Colocalised pixels",bShowLocalisation);
		gd.addCheckbox("Use constant intensity for colocalised pixels",colocValConst);
		gd.addCheckbox("Show Scatter plot",bScatter);
		gd.addCheckbox("Include zero-zero pixels in threshold calculation",opt0);

		gd.addCheckbox("Set options",false);
		gd.addMessage("See: http://uhnresearch.ca/wcif/imagej");
		gd.showDialog();
		if (gd.wasCanceled())	return false;
		index1 = gd.getNextChoiceIndex();
		index2 = gd.getNextChoiceIndex();
		indexRoi = gd.getNextChoiceIndex();
		//	indexMask = gd.getNextChoiceIndex();
		//IJ.showMessage(""+indexMask);
		dualChannelIndex = gd.getNextChoiceIndex();
		bShowLocalisation = gd.getNextBoolean();
		colocValConst = gd.getNextBoolean();
		bScatter= gd.getNextBoolean();
		opt0 =gd.getNextBoolean();
		boolean options=gd.getNextBoolean();
		String title1 = titles[index1];
		String title2 = titles[index2];
		imp1 = WindowManager.getImage(wList[index1]);
		imp2 = WindowManager.getImage(wList[index2]);
		useMask=false;
		//IJ.showMessage(""+indexMask);


		imp1 = WindowManager.getImage(wList[index1]);
		imp2 = WindowManager.getImage(wList[index2]);
		ImageWindow winimp1= imp1.getWindow();
		ImageWindow winimp2= imp2.getWindow();
		if (imp1.getType()!=imp1.GRAY8&&imp1.getType()!=imp1.GRAY16&&imp2.getType()!=imp2.GRAY16 &&imp2.getType()!=imp1.GRAY8) {
			IJ.showMessage("Image Correlator", "Both images must be 8-bit or 16-bit grayscale.");
			return false;
		}
		ip1 = imp1.getProcessor();
		ip2 = imp2.getProcessor();
		Roi roi1 = imp1.getRoi();
		Roi roi2= imp2.getRoi();
		boolean keepROIimage=true;
		width = imp1.getWidth();
		height = imp1.getHeight();
		useRoi=true;
		if (indexRoi== 0) useRoi = false;
		Rectangle rect =ip1.getRoi();

		//IJ.showMessage("index"+rect.width);

		if ((indexRoi==1)) {
			if (roi1==null) {
				useRoi=false;
			} else {
				if (roi1.getType()==Roi.RECTANGLE) {
					IJ.showMessage("Does not work with rectangular ROIs");
					return false;
				}
				ipmask = imp1.getMask();
				//if (keepROIimage) new ImagePlus("Mask",ipmask).show();
				rect = ip1.getRoi();
			}
		}

		if ((indexRoi==2)) {
			if (roi2==null) {
				useRoi=false;
			} else {
				if (roi2.getType()==Roi.RECTANGLE) {
					IJ.showMessage("Does not work with rectangular ROIs");
					return false;
				}
				ipmask = imp2.getMask();
				//if (keepROIimage) new ImagePlus("Mask",ipmask).show();
				rect = ip2.getRoi();
			}
		}

		if (indexRoi==0) {
			xOffset = 0;
			yOffset = 0;
			rwidth=width;
			rheight  =height;
		} else {
			xOffset = rect.x;
			yOffset = rect.y;
			rwidth=rect.width;
			rheight  =rect.height;

		}
		//IJ.showMessage("Xoffset:"+xOffset+ " Yoffset:"+yOffset);

		//if red-blue
		if (dualChannelIndex==1) {
			colIndex2 = 2;
			colIndex3=1;
		};

		//if blue-green
		if (dualChannelIndex==2) {
			colIndex1 = 1;
			colIndex2 =2 ;
			colIndex3=0;
		}




		boolean matchWidth=false;
		boolean matchHeight=false;
		boolean  matchSlice = false;
		//if (imp1.getWidth()==imp2.getWidth()&&imp1.getWidth()==impMask.getWidth()) matchWidth = true;
		//if (imp1.getHeight()==imp2.getHeight()&&imp1.getHeight()==impMask.getHeight()) matchHeight = true;
		//if (imp1.getStackSize()==imp2.getStackSize()&&imp1.getStackSize()==impMask.getStackSize()) matchSlice = true;

		//if (!(matchWidth&&matchHeight&&matchSlice))
		//	{IJ.showMessage("Image mismatch","Images do not match. Exiting");
		//	return false;
		//	}
		if (options) {
			GenericDialog gd2 = new GenericDialog("Set Results Options");
			gd2.addMessage("See online manual for detailed description of these values");
			gd2.addCheckbox("Show linear regression solution",opt1a);
			gd2.addCheckbox("Show thresholds",opt1);
			gd2.addCheckbox("Pearson's for whole image",opt2);
			gd2.addCheckbox("Pearson's for image above thresholds",opt3a);
			gd2.addCheckbox("Pearson's for image below thresholds (should be ~0)",opt3b);
			gd2.addCheckbox("Mander's original coefficients (threshold = 0)",opt4);
			gd2.addCheckbox("Mander's using thresholds",opt5);
			gd2.addCheckbox("Number of colocalised voxels",opt6);
			gd2.addCheckbox("% Image volume colocalised",opt7);
			gd2.addCheckbox("% Voxels colocalised",opt8);
			gd2.addCheckbox("% Intensity colocalised",opt9);
			gd2.addCheckbox("% Intensity above threshold colocalised",opt10);
			gd2.showDialog();
			if (gd2.wasCanceled())	return false;


			opt1=gd2.getNextBoolean();
			opt1a=gd2.getNextBoolean();
			opt2=gd2.getNextBoolean();
			opt3a=gd2.getNextBoolean();
			opt3b=gd2.getNextBoolean();
			opt4=gd2.getNextBoolean();
			opt5=gd2.getNextBoolean();
			opt6=gd2.getNextBoolean();
			opt7=gd2.getNextBoolean();
			opt8=gd2.getNextBoolean();
			opt9=gd2.getNextBoolean();
			opt10=gd2.getNextBoolean();
			headingsSetCTC = false;
		}
		//IJ.showMessage(""+indexMask);
		return true;
	}

	public void correlate(ImagePlus imp1, ImagePlus imp2) {//IJ.showMessage("mask? "+useMask);
		String ch1fileName = imp1.getTitle();
		String ch2fileName = imp2.getTitle();
		//String maskName = impMask.getTitle();
		String fileName = ch1fileName +  " & " + ch2fileName;
		ImageProcessor ip1 = imp1.getProcessor();
		ImageProcessor ip2 = imp2.getProcessor();

		ImageProcessor ipMask = imp1.getMask();
		if (indexRoi>1) ipMask = imp2.getMask();

		//	ImageStack imgMask = impMask.getStack();
		ImageStack img1 = imp1.getStack();
		ImageStack img2 = imp2.getStack();
		if (indexRoi== 0) useRoi = false;
		Rectangle rect1 =ip1.getRoi();
		Rectangle rect2 =ip2.getRoi();

		Roi roi1 = imp1.getRoi();
		Roi roi2= imp2.getRoi();

		nslices = imp1.getStackSize();
		width = imp1.getWidth();
		height = imp1.getHeight();
		ipColoc = new ColorProcessor(rwidth,rheight);
		ImageStack stackColoc = new ImageStack(rwidth,rheight);
		double ch1threshmin=0;
		double ch1threshmax=ip1.getMax();
		double ch2threshmin=0;
		double  ch2threshmax=ip2.getMax();
		double pearsons1 = 0;
		double pearsons2 = 0;
		double pearsons3 = 0;
		double r = 1;
		pearsons1 =0;
		pearsons2 = 0;
		pearsons3 = 0;
		double r2= 1;
		boolean thresholdFound=false;
		boolean unsigned = true;
		int count =0;
		double sumX = 0;
		double sumXY = 0;
		double sumXX = 0;
		double sumYY = 0;
		double sumY = 0;
		double colocX = 0;
		double colocY = 0;
		double countX = 0;
		double countY = 0;
		double sumXYm=0;
		int Nch1=0,Nch2=0;
		double oldMax=0;
		int sumCh2gtT =0;
		int sumCh1gtT =0;
		int N = 0;
		int N2 = 0;
		int Nzero=0;
		int Nch1gtT=0;
		int Nch2gtT=0;

		double oldMax2=0;
		int ch1Max=(int)ip1.getMax();
		int ch2Max=(int)ip2.getMax();
		int ch1Min = (int)ip1.getMin();
		int ch2Min = (int)ip2.getMin();
		int ch1ROIMax=0;
		int ch2ROIMax=0;

		String Headings = "\t \t \t \t \t \t \t \n";
		ImageProcessor plot32 = new FloatProcessor(256, 256);
		ImageProcessor plot16 = new ShortProcessor(256, 256);
		int scaledXvalue =0;
		int scaledYvalue=0;
		if (ch1Max<255) ch1Max=255;
		if (ch2Max<255) ch2Max=255;
		double ch1Scaling = (double)255/(double)ch1Max;
		double ch2Scaling = (double)255/(double)ch2Max;
		boolean divByZero=false;

		StringBuffer sb= new StringBuffer();
		String str="";
		int i = imp1.getCurrentSlice();
		double bBest=0;
		double mBest = 0;
		double bestr2=1;
		double ch1BestThresh=0;
		double ch2BestThresh=0;
		String mString;
		//start regression
		IJ.showStatus("1/3: Performing regression. Press 'Esc' to abort");
		int ch1Sum=0;
		int ch2Sum=0;
		int ch3Sum=0;
		double ch1mch1MeanSqSum=0;
		double ch2mch2MeanSqSum= 0;
		double ch3mch3MeanSqSum= 0;
		ImageProcessor ipPlot = new ShortProcessor (256,256);
		int mask=0;

		if (indexRoi== 0) useRoi = false;
		Rectangle rect =ip1.getRoi();


		if ((indexRoi==1)) {
			if (roi1==null) {
				useRoi=false;
			} else {
				if (roi1.getType()==Roi.RECTANGLE) {
					IJ.showMessage("Does not work with rectangular ROIs");
					return;
				}
				ipMask = imp1.getMask();
				//if (keepROIimage) new ImagePlus("Mask",ipmask).show();
				rect = ip1.getRoi();
			}
		}

		if ((indexRoi==2)) {
			if (roi2==null) {
				useRoi=false;
			} else {
				if (roi2.getType()==Roi.RECTANGLE) {
					IJ.showMessage("Does not work with rectangular ROIs");
					return ;
				}
				ipMask = imp2.getMask();
				//if (keepROIimage) new ImagePlus("Mask",ipmask).show();
				rect = ip2.getRoi();
			}
		}
		if (useRoi==false) {
			xOffset = 0;
			yOffset = 0;
			rwidth=width;
			rheight  =height;
		} else {
			xOffset = rect.x;
			yOffset = rect.y;
			rwidth=rect.width;
			rheight  =rect.height;

		}
		//new ImagePlus("Mask",ipMask).show();

		//get means
		for (int s=1; s<=nslices; s++) {
			if (IJ.escapePressed()) {
				IJ.beep();
				return;
			}
			ip1 = img1.getProcessor(s);
			ip2 = img2.getProcessor(s);
			//ipMask = imgMask.getProcessor(s);

			for (int y=0; y<rheight; y++) {
				for (int x=0; x<rwidth; x++) {
					mask=1;
					if (useRoi) mask = (int)ipMask.getPixelValue(x,y);
					if (mask!=0) {
						ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset);
						ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);
						if (ch1>ch1ROIMax) ch1ROIMax=ch1;
						if (ch2>ch2ROIMax) ch2ROIMax=ch2;

						ch3 = ch1+ch2;
						ch1Sum+=ch1;
						ch2Sum+=ch2;
						ch3Sum+=ch3;
						if (ch1+ch2!=0) N++;
					}
				}
			}
		}
		double ch1Mean = ch1Sum/N;
		double ch2Mean = ch2Sum/N;
		double ch3Mean = ch3Sum/N;
		N=0;
		//calulate variances
		for (int s=1; s<=nslices; s++) {
			if (IJ.escapePressed()) {
				IJ.beep();
				return;
			}
			ip1 = img1.getProcessor(s);
			ip2 = img2.getProcessor(s);
			//ipMask = imgMask.getProcessor(s);
			for (int y=0; y<rheight; y++) {
				for (int x=0; x<rwidth; x++) {
					mask=1;
					if (useRoi) mask = (int)ipMask.getPixelValue(x,y);
					if (mask!=0) {
						ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset);
						ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);
						ch3 = ch1+ch2;
						ch1mch1MeanSqSum+= (ch1-ch1Mean)*(ch1-ch1Mean);
						ch2mch2MeanSqSum+= (ch2-ch2Mean)*(ch2-ch2Mean);
						ch3mch3MeanSqSum+=	 (ch3-ch3Mean)*(ch3-ch3Mean);

						if (ch1+ch2==0) Nzero++;
						//calc pearsons for original image
						sumX = sumX+ch1;
						sumXY = sumXY + (ch1 * ch2);
						sumXX = sumXX + (ch1 * ch1);
						sumYY = sumYY + (ch2 *ch2);
						sumY = sumY + ch2;
						N++;
					}
				}
			}
		}
		N=N-Nzero;
		pearsons1 = sumXY - (sumX*sumY/N);
		pearsons2 = sumXX - (sumX*sumX/N);
		pearsons3 = sumYY - (sumY*sumY/N);
		double rTotal = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		//http://mathworld.wolfram.com/Covariance.html
		//?2 = X2?(X)2
		// = E[X2]?(E[X])2
		//var (x+y) = var(x)+var(y)+2(covar(x,y));
		//2(covar(x,y)) = var(x+y) - var(x)-var(y);

		double ch1Var = ch1mch1MeanSqSum/(N-1);
		double ch2Var = ch2mch2MeanSqSum/(N-1);
		double ch3Var = ch3mch3MeanSqSum/(N-1);
		double ch1ch2covar = 0.5*(ch3Var-(ch1Var+ch2Var));

		//do regression
		//See:Dissanaike and Wang
		// http://papers.ssrn.com/sol3/papers.cfm?abstract_id=407560

		double denom = 2*ch1ch2covar;
		double num = ch2Var - ch1Var + Math.sqrt((ch2Var- ch1Var)*(ch2Var- ch1Var) + (4*ch1ch2covar *ch1ch2covar) );

		double m= num/denom;
		double b = ch2Mean - m*ch1Mean ;
		//IJ.showMessage("ch2 = "+ df2.format(m)+"*ch1 + "+df2.format(b));
		//IJ.showStatus("Done");

		boolean prevDivByZero=false;
		double newMax=ch1Max;
		double r2Prev=0;
		double r2Prev2 =1;
		int iteration=1;
		r2=0;
		boolean prevByZero = false;
		double tolerance = 0.01;

		//newMax=20;

		while (( thresholdFound==false)&&iteration<30) {
			if (iteration==2&&r2<0) {
				IJ.showMessage("No positive correlations found. Ending");
				IJ.showStatus("Done");
				return;
			}
			ch1threshmax=Math.round(newMax);
			ch2threshmax=Math.round(((double)ch1threshmax*(double)m)+(double)b);
			if (IJ.escapePressed()) {
				IJ.beep();
				return;
			}
			IJ.showStatus("2/3: Calculating Threshold. i = "+iteration+" Press 'Esc' to abort");
			//reset values
			sumX = 0;
			sumXY = 0;
			sumXX = 0;
			sumYY = 0;
			sumY = 0;
			N2 = 0;
			N=0;
			Nzero=0;
			for (int s=1; s<=nslices; s++) {
				ip1 = img1.getProcessor(s);
				ip2 = img2.getProcessor(s);
				//ipMask = imgMask.getProcessor(s);

				for (int y=0; y<rheight; y++) {
					for (int x=0; x<rwidth; x++) {
						mask=1;
						if (useRoi) mask = (int)ipMask.getPixelValue(x,y);
						if (mask!=0) {
							ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset);
							ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);

							if ((ch1<(ch1threshmax))||(ch2<(ch2threshmax))) {
								if (ch1+ch2==0) Nzero++;
								//calc pearsons
								sumX = sumX+ch1;
								sumXY = sumXY + (ch1 * ch2);
								sumXX = sumXX + (ch1 * ch1);
								sumYY = sumYY + (ch2 *ch2);
								sumY = sumY + ch2;
								N++;
							}
						}
					}
				}
			}
			if (!opt0) N=N-Nzero;
			pearsons1 = sumXY - (sumX*sumY/N);
			pearsons2 = sumXX - (sumX*sumX/N);
			pearsons3 = sumYY - (sumY*sumY/N);

			r2Prev2 = r2Prev;
			r2Prev=r2;
			r2= pearsons1/(Math.sqrt(pearsons2*pearsons3));

			//IJ.write(iteration+"\t"+df2.format(ch1threshmax)+"\t"+df2.format(ch2threshmax)+"\t"+ df4.format(r2)+"\t"+ ch1BestThresh) ;

			//if r is not a number then set divide by zero to be true
			if (((Math.sqrt(pearsons2*pearsons3))==0)||N==0)	divByZero =true;
			else divByZero = false;

			//check to see if we're getting colser to zero for r
			if ((bestr2*bestr2>r2*r2)) {
				ch1BestThresh=ch1threshmax;
				bestr2=r2;
			}

			//if our r is close to our level of tolerance then set threshold has been found
			if ((r2<tolerance)&&(r2>-tolerance) )thresholdFound = true;

			//if we've reached ch1 =1 then we've exhausted posibilities
			if (Math.round(ch1threshmax)==0) thresholdFound =true;

			oldMax= newMax;
			//change threshold max
			if (r2>=0) {
				if ((r2>=r2Prev)&&(!divByZero))  newMax =  newMax/2;
				if ((r2<r2Prev)||(divByZero)) newMax = newMax+(newMax/2);
			}
			if ((r2<0)||divByZero) {
				newMax = newMax+(newMax/2);
			}
			iteration++;

		}

		ch1threshmax =Math.round((ch1BestThresh));
		ch2threshmax =Math.round(((double)ch1BestThresh*(double)m)+(double)b);
		int colocInt=255;

		Nzero=0;
		int sumColocCh1=0;
		int sumColocCh2=0;
		int Ncoloc=0;
		imp1.setSlice(i);
		imp2.setSlice(i);
		sumXYm=0;
		sumCh1gtT=0;
		sumCh2gtT=0;

		double mCh2coloc = 0;
		double mCh1coloc = 0;
		double sumCh1total = 0;
		double sumCh2total = 0;

		//IJ.showMessage("thresholds "+ (int)ch1threshmax+ ";"+(int)ch2threshmax);

		sumXYm=0;
		sumX = 0;
		sumXY = 0;
		sumXX = 0;
		sumYY = 0;
		sumY = 0;
		N2 = 0;
		N=0;
		Ncoloc=0;

		int [] color  = new int [3];
		for (int s=1;s<=nslices; s++) {
			IJ.showStatus("3/3: Performing final regression. Slice = "+s +" Press 'Esc' to abort");
			ip1 = img1.getProcessor(s);
			ip2 = img2.getProcessor(s);
			//ipMask = imgMask.getProcessor(s);
			ipColoc = new ColorProcessor(rwidth,rheight);
			for (int y=0; y<rheight; y++) {
				for (int x=0; x<rwidth; x++) {
					mask=1;
					if (useRoi) mask = (int)ipMask.getPixelValue(x,y);
					if (mask!=0) {
						ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset);
						ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);

						color[colIndex1 ]=(int)ch1;
						color[colIndex2 ]=(int)ch2;
						color[colIndex3 ]=(int)0;

						ipColoc.putPixel(x,y,color );

						sumCh1total=sumCh1total+ch1;
						sumCh2total=sumCh2total+ch2;
						N++;
						scaledXvalue = (int)((double)ch1*ch1Scaling);
						scaledYvalue = 255-(int)((double)ch2*ch2Scaling);
						count = plot32.getPixel(scaledXvalue ,scaledYvalue );
						count++;
						plot32.putPixel(scaledXvalue ,scaledYvalue, count);
						if (count<65535) plot16.putPixel(scaledXvalue ,scaledYvalue, count);
						if (ch1+ch2==0) Nzero++;
						if (ch1>0) {
							Nch1++;
							mCh2coloc = mCh2coloc+ch2;
						}
						if (ch2>0) {
							Nch2++;
							mCh1coloc = mCh1coloc+ch1;
						}

						if ((double)ch2>=ch2threshmax) {
							Nch2gtT++;
							sumCh2gtT = sumCh2gtT+ch2;
							colocX=colocX+ch1;
						}
						if ((double)ch1>=ch1threshmax) {
							Nch1gtT++;
							sumCh1gtT = sumCh1gtT+ch1;
							colocY=colocY+ch2;
						}
						if (((double)ch1>ch1threshmax)&&((double)ch2>ch2threshmax)) {
							sumColocCh1 = sumColocCh1+ch1;
							sumColocCh2 = sumColocCh2+ch2;
							Ncoloc++;
							colocInt=255;
							if (!colocValConst) {
								colocInt = (int)Math.sqrt(ch1*ch2);
							}
							color[colIndex1 ]=(int)colocInt;
							color[colIndex2 ]=(int)colocInt;
							//color[colIndex1 ]=(int)0;
							//color[colIndex2 ]=(int)0;
							color[colIndex3 ]=(int)colocInt;

							ipColoc.putPixel(x,y,color );

							//calc pearsons
							sumX = sumX+ch1;
							sumXY = sumXY + (ch1 * ch2);
							sumXX = sumXX + (ch1 * ch1);
							sumYY = sumYY + (ch2 *ch2);
							sumY = sumY + ch2;
						}
					}
				}
			}
			//IJ.showMessage(stackColoc.getWidth()+ "  -  " + ipColoc.getWidth());
			stackColoc.addSlice("Correlation Plot", ipColoc);
		}

		// N=Ncoloc;

		//IJ.showMessage("Totoal"+N+"   N0:"+Nzero+" Nc :"+ Ncoloc);
		pearsons1 = sumXY - (sumX*sumY/N);
		pearsons2 = sumXX - (sumX*sumX/N);
		pearsons3 = sumYY - (sumY*sumY/N);


		//Pearsons for coloclaised volume
		double Rcoloc= pearsons1/(Math.sqrt(pearsons2*pearsons3));

		//Mander's original
		//[i.e. E(ch1if ch2>0) ÷ E(ch1total)]

		double M1 = mCh1coloc /sumCh1total;
		double M2 = mCh2coloc /sumCh2total;


		//Manders using threshold
		//[i.e. E(ch1 if ch2>ch2threshold) ÷ (Ech1total)]
		double colocM1 = (double) colocX/(double)sumCh1total;
		double colocM2 = (double) colocY/(double)sumCh2total;

		//as in Coste's paper
		//[i.e. E(ch1>ch1threshold) ÷ E(ch1total)]

		double colocC1 = (double)sumCh1gtT/ (double)sumCh1total;
		double colocC2 = (double)sumCh2gtT/(double)sumCh2total;


		//Imaris percentage volume
		double percVolCh1 =  (double)Ncoloc/ (double)Nch1gtT;
		double percVolCh2 =  (double)Ncoloc/(double)Nch2gtT;

		double percTotCh1 =  (double) sumColocCh1/ (double)sumCh1total;
		double percTotCh2 =  (double) sumColocCh2/ (double)sumCh2total;

		//Imaris percentage material
		double percMatCh1 = (double) sumColocCh1/(double)sumCh1gtT;
		double percMatCh2 =  (double)sumColocCh2/(double)sumCh2gtT;

		sb.append(fileName+"\n");

		//if (!useMask) maskName = "<none>";

		str = fileName +"\t"+"ROI" + indexRoi+"\texcl.\t";
		if (opt0) str = fileName +"\t"+str+"\tincl.\t";

		if (opt2)	str+= df3.format(rTotal)+ "\t";
		if (opt1a)	str+= df3.format(m)+ "\t "+df1.format(b)+ "\t";
		if (opt1)	str+= IJ.d2s(ch1threshmax,0)+"\t"+IJ.d2s(ch2threshmax,0)+"\t";
		if (opt3a) str+= df4.format(Rcoloc) +"\t";
		if (opt3b)	str+= df3.format(bestr2)+"\t";
		if (opt4)	 str+= df4.format(M1)+ "\t "+df4.format(M2)+"\t";
		if (opt5)	 str+= df4.format(colocM1)+ "\t"+df4.format(colocM2)+"\t";
		if (opt6)	 str+= Ncoloc+ "\t";
		if (opt7)	 str+= df2.format(((double)Ncoloc*(double)100)/((double)width*(double)height*(double)nslices))+"%\t";
		if (opt8)	 str+= df2.format(percVolCh1*100 )+ "%\t";
		if (opt8)	 str+= df2.format(percVolCh2*100 )+ "%\t";
		if (opt9)	 str+= df2.format(percTotCh1*100 )+ "%\t";
		if (opt9)	 str+= df2.format(percTotCh2*100 )+ "%\t";
		if (opt10)	 str+= df2.format(percMatCh1*100 )+ "%\t";
		if (opt10)	 str+= df2.format(percMatCh2*100 )+ "%\t";

		String heading = "Images\tMask\tZeroZero\t";

		if (opt2)	heading += "Rtotal\t";
		if (opt1a)	heading += "m\tb\t";
		if (opt1)	heading += "Ch1 thresh\tCh2 thresh\t";
		if (opt3a) heading += "Rcoloc\t";
		if (opt3b)	heading += "R<threshold\t";
		if (opt4)	 heading += "M1\tM2\t";
		if (opt5)	 heading += "tM1\ttM2\t";
		if (opt6)	 heading += "Ncoloc\t";
		if (opt7)	 heading += "%Volume\t";
		if (opt8)	 heading += "%Ch1 Vol\t";
		if (opt8)	 heading += "%Ch2 Vol\t";
		if (opt9)	 heading += "%Ch1 Int\t";
		if (opt9)	 heading += "%Ch2 Int\t";
		if (opt10)	 heading += "%Ch1 Int > thresh\t";
		if (opt10)	 heading += "%Ch2 Int >thresh\t";
		heading+="\n";
		//	sb.append("%Voxels colocalised\t Ch1 = "+ df2.format(percVolCh1*100 )+ "%\tCh2 = "+df2.format(percVolCh2*100)+"%\n");

		//sb.append("Sum of gtT \t Ch1 = "+ df3.format(sumCh1gtT )+ "\tCh2 = "+df3.format(sumCh2gtT)+"\n");
		//sb.append("Sum total \t Ch1 = "+ df3.format(sumXtotal )+ "\tCh2 = "+df3.format(sumYtotal)+"\n");
		double plotY=0;
		double plotY2=0;

		if (textWindow == null)
			textWindow = new TextWindow("Results",
				heading, str, 400, 250);
		else {
			textWindow.getTextPanel().setColumnHeadings(heading);
			textWindow.getTextPanel().appendLine(str);
		}

		//new TextWindow( "mRegression: "+fileName, "\t \t \t.", sb.toString(), 420, 450);
		Prefs.set("CTC_annels.int", (int)dualChannelIndex );
		Prefs.set("CTC_channels.int", (int)dualChannelIndex );
		Prefs.set("CTC_show.boolean", bShowLocalisation);
		Prefs.set("CTC_colocConst.boolean", colocValConst);
		Prefs.set("CTC_bScatter.boolean", bScatter);
		Prefs.set("CTC_opt0.boolean", opt0);
		Prefs.set("CTC_opt1.boolean", opt1);
		Prefs.set("CTC_opt1a.boolean", opt1a);
		Prefs.set("CTC_opt2.boolean", opt2);
		Prefs.set("CTC_opt3a.boolean", opt3a);
		Prefs.set("CTC_opt3b.boolean", opt3b);
		Prefs.set("CTC_opt4.boolean", opt4);
		Prefs.set("CTC_opt5.boolean", opt5);
		Prefs.set("CTC_opt6.boolean", opt6);
		Prefs.set("CTC_opt7.boolean", opt7);
		Prefs.set("CTC_opt8.boolean", opt8);
		Prefs.set("CTC_opt9.boolean", opt9);
		Prefs.set("CTC_opt10.boolean", opt10);
		Prefs.set("CTC_indexRoi.int",(int)indexRoi);
		//String colocString = "channel='"+Ch1fileName + "' channel='"+Ch2fileName +"' ratio=0 threshold="+IJ.d2s(ch1threshmax,1)+" threshold="+IJ.d2s(ch2threshmax,1)+" display=255";


		if (bShowLocalisation) {//ipColoc.resetMinAndMax();
			new ImagePlus("Colocalised pixels", stackColoc).show();
		}

		if (bScatter) {
			plot16.resetMinAndMax();
			for (int c=0; c<256; c++) {
				plotY = ((double)c*m)+b;
				//plotY2 = ((double)c*m2)+b2;
				int plotmax2=(int)(plot16.getMax());
				int plotmax = (int)(plotmax2/2);

				plot16.putPixel(c, (int)255-(int)plotY,plotmax );

				plot16.putPixel(c, 255-(int)(ch2threshmax*ch2Scaling), plotmax );


				plot16.putPixel((int)(ch1threshmax*ch1Scaling), c, plotmax );


				//plot16.putPixel(c, (int)255-(int)plotY2,plotmax2 );
			}
			ImagePlus imp3 = new ImagePlus("Correlation Plot",
					plot16);
			imp3.show();
			IJ.run(imp3, "Enhance Contrast", "saturated=50 equalize");
			IJ.run(imp3, "Fire", null);
			imp3.setTitle(fileName + " Freq. CP");
		}
		IJ.selectWindow("Results");
		IJ.showStatus("Done");

	}




}
