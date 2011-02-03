//version 21 4 05
//added van Steensel CCF analysis
//JCellSci v109.p787
//version 29/4/05
//Costes randomisation uses pixels in ch2 only once per random image

import java.math.*;
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
import ij.plugin.filter.*;
import ij.measure.Calibration;
import ij.measure.*;

import ij.text.TextWindow;

public class Colocalisation_Test implements PlugIn
    {static boolean headingsSet2;
    private static int index1=0;
    private static int index2=1;
    private ImagePlus imp1, imp2,impmask, imp3;
    private ImageProcessor ipmask;
    private int indexRoi= (int)Prefs.get("Rand_indexRoi.int",0);
    private int indexRand= (int)Prefs.get("Rand_indexRand.int",0);
    private boolean useROI, useMask ;
    private Roi roi, roi1, roi2;
    private static boolean randZ= Prefs.get("Rand_randZ.boolean", false);
    private static boolean ignoreZeroZero= Prefs.get("Rand_ignore.boolean", true);
    private static boolean smooth= Prefs.get("Rand_smooth.boolean", true);
    private static boolean keep = Prefs.get("Rand_keep.boolean", false);
    private static boolean currentSlice= Prefs.get("Rand_currentSlice.boolean", true);
    private static boolean useManPSF= Prefs.get("Rand_useManPSF.boolean", false);
    private static boolean showR= Prefs.get("Rand_showR.boolean", false);
    private static double psf =0;
    private static int manPSF= (int)Prefs.get("Rand_manPSF.int",10);
    private static int iterations= (int)Prefs.get("Rand_iterations.int",0);
    private static double ch2Lambda = Prefs.get("Rand_ch2L.double",520);
    private static double NA = Prefs.get("Rand_NA.double",1.4);
    private static double pixelSize  = Prefs.get("Rand_pixelSize.double",0.10);
    DecimalFormat df3 = new DecimalFormat("##0.000");
    DecimalFormat df2 = new DecimalFormat("##0.00");
    DecimalFormat df1 = new DecimalFormat("##0.0");
    DecimalFormat df0 = new DecimalFormat("##0");
    String[] chooseRand=  { "Fay (x,y,z translation)","Costes approximation (smoothed noise)", "van Steensel (x translation)"};
    private int width, height, rwidth, rheight, xOffset, yOffset, mask;
    String randMethod = "Fay";
    private long startTime;
    private long endTime;
    StringBuffer rVals = new StringBuffer();
    boolean Costes = false;
    boolean Fay = false; 
    boolean vanS = false;
    boolean rBlocks= false;
    protected static TextWindow textWindow;

    public void run(String arg) 
	{startTime = System.currentTimeMillis();
     	if (showDialog())
	            correlate(imp1, imp2, imp3);
	}
    
    public boolean showDialog() 
	{
	int[] wList = WindowManager.getIDList();
	if (wList==null) 
		{
		IJ.noImage();
		return false;
        		}
	String[] titles = new String[wList.length];
    	String[] chooseROI=  new String[wList.length+3];
	chooseROI[0] = "None";
	chooseROI[1] = "ROI in channel 1 ";
	chooseROI[2] = "ROI in channel 2";
	
	if (indexRoi>wList.length+3) indexRoi=0;
	for (int i=0; i<wList.length; i++) 
		{
		ImagePlus imp = WindowManager.getImage(wList[i]);
		if (imp!=null){	titles[i] = imp.getTitle();
				chooseROI[i+3] =	imp.getTitle();}
            		else	titles[i] = "";
        		}	
	
	if (index1>=titles.length)index1 = 0;
	if (index2>=titles.length)index2 = 0;
	GenericDialog gd = new GenericDialog("Colocalisation Test");
	gd.addChoice("Channel 1", titles, titles[index1]);
	gd.addChoice("Channel 2", titles, titles[index2]);
	gd.addChoice("ROI or Mask", chooseROI, chooseROI[indexRoi]);
	gd.addChoice("Randomization method", chooseRand,chooseRand[indexRand]);
	//gd.addCheckbox("Ignore zero-zero pixels", ignoreZeroZero);
	gd.addCheckbox("Current slice only (Ch1)", currentSlice);
	gd.addCheckbox("Keep example randomized image", keep);

	gd.addCheckbox("Show all R values from Ch1 vs  Ch2(rand)", showR);
	gd.addMessage("See: http://uhnresearch.ca/wcif/imagej");
	gd.showDialog();
	if (gd.wasCanceled())	return false;
	index1 = gd.getNextChoiceIndex();
	index2 = gd.getNextChoiceIndex();
	indexRoi = gd.getNextChoiceIndex();
	indexRand=gd.getNextChoiceIndex();
	ignoreZeroZero = true;
	currentSlice= gd.getNextBoolean();
	keep = gd.getNextBoolean();
	
	showR=gd.getNextBoolean();
	String title1 = titles[index1];
	String title2 = titles[index2];
	imp1 = WindowManager.getImage(wList[index1]);
	imp2 = WindowManager.getImage(wList[index2]);

	if (imp1.getType()==imp1.COLOR_RGB || imp2.getType()==imp1.COLOR_RGB)
		{
		IJ.showMessage("Colocalisation Test", "Both images must be grayscale.");
		return false;
		}
	useMask=false;
	if (indexRoi >=3) 
		{
		imp3 = WindowManager.getImage(indexRoi-2);
 		useMask=true;
		}
	else imp3 = WindowManager.getImage(wList[index2]);
	
                Calibration cal = imp2.getCalibration();
	pixelSize  = cal.pixelWidth;

	if(indexRand==0)
		 {Fay=true;
		randMethod = "Fay";
		}
	if(indexRand==1)
		{Costes = true;
		randMethod = "Costes X, Y";
		}
	
	if(indexRand==2)
		{vanS=true;
		randMethod = "van Steensel";
		}

	
	
//test to ensure all images match up.
	boolean matchWidth=false;
	boolean matchHeight=false;
	boolean  matchSlice = false;
	if (imp1.getWidth()==imp2.getWidth()&&imp1.getWidth()==imp3.getWidth()) matchWidth = true;
	if (imp1.getHeight()==imp2.getHeight()&&imp1.getHeight()==imp3.getHeight()) matchHeight = true;
	if (imp1.getStackSize()==imp2.getStackSize()&&imp1.getStackSize()==imp3.getStackSize()) matchSlice = true;

	if (!(matchWidth&&matchHeight&&matchSlice)) 
		{IJ.showMessage("Image mismatch","Images do not match. Exiting");
		return false;
		}

	if (Costes||rBlocks)
		{
		GenericDialog gd2 = new GenericDialog("PSF details");
		gd2.addCheckbox("Randomize pixels in z-axis", randZ);
		gd2.addNumericField("Pixel Size (µm)", pixelSize,3);
		gd2.addNumericField("Channel 2 wavelength (nm)", ch2Lambda,0);
		gd2.addNumericField("NA of objective", NA,2);
		gd2.addNumericField("Iterations",iterations,0);
		gd2.addMessage("");
		gd2.addCheckbox("Use manual PSF", useManPSF);
		gd2.addNumericField("PSF radius in pixels", manPSF, 0);
		gd2.showDialog();
		if (gd2.wasCanceled())	return false;
		randZ = gd2.getNextBoolean();
		if (randZ) randMethod+=", Z";
		pixelSize =gd2.getNextNumber();
		ch2Lambda = gd2.getNextNumber();
		NA = gd2.getNextNumber();
		iterations = (int)gd2.getNextNumber();
		useManPSF = gd2.getNextBoolean();
		manPSF = (int)gd2.getNextNumber();
		psf = (0.61*ch2Lambda)/NA;
		psf = (psf)/(pixelSize*1000);
		if (useManPSF) psf = manPSF;
		//IJ.showMessage("PSF radius = "+df3.format(psf));		
		}
	return true;
	}
    
  public void correlate(ImagePlus imp1, ImagePlus imp2, ImagePlus imp3) 
	{
	String Ch1fileName = imp1.getTitle();
	String Ch2fileName = imp2.getTitle();
	ImageStack img1 = imp1.getStack();
	ImageStack img2 = imp2.getStack();
	ImageStack img3=imp3.getStack();
	int width = imp1.getWidth();
	int height = imp1.getHeight();
	String fileName = Ch1fileName +  " and " + Ch2fileName;
	ImageProcessor ip1 = imp1.getProcessor();
	ImageProcessor ip2 = imp2.getProcessor();
	ImageProcessor ip3= null;
	if (useMask) ip3 = imp3.getProcessor();
	ImageStack stackRand = new ImageStack(rwidth,rheight);
	ImageProcessor ipRand= img2.getProcessor(1);
	int currentSliceNo = imp1.getCurrentSlice();
	if(currentSlice) fileName = fileName+ ". slice: "+currentSliceNo ;
	double pearsons1 = 0;
	double pearsons2 = 0;
	double pearsons3 = 0;
	double r2= 1;
	double r=1;
	double ch1Max=0;
	double ch1Min = ip1.getMax();
	double ch2Max=0;
	double ch2Min = ip1.getMax();
	int nslices = imp1.getStackSize();
	int ch1, ch2, count;
	double sumX = 0;
	double sumXY = 0;
	double sumXX = 0;
	double sumYY = 0;
	double sumY = 0;
	double sumXtotal=0;
	double sumYtotal=0;
	double colocX=0;
	double colocY=0;
	int N = 0;
	int N2 = 0;
	double r2min=1;
	double r2max=-1;
	sumX = 0;
	sumXY = 0;
	sumXX = 0;
	sumYY = 0;
	sumY = 0;
	int i=1;
	double coloc2M1 = 0;
	double coloc2M2 = 0;
	int colocCount=0;
	int colocCount1=0;
	int colocCount2=0;
	double r2sd=0;
	double sumr2sqrd=0;
	double sumr2=0;
	double ICQ2mean=0;
	double sumICQ2sqrd=0;
	double sumICQ2=0;
	double ICQobs=0;
	int countICQ=0;
	int Nr=0;
	int Ng=0;
//get stack2 histogram
ImageStatistics stats = imp2.getStatistics();
if (imp2.getType() == imp2.GRAY16)
	stats.nBins = 1<<16;
int [] histogram = new int[stats.nBins];


//roi code
	if (indexRoi==1||indexRoi==2)	useROI = true;
	else useROI=false;

	ip1 = imp1.getProcessor();
	ip2 = imp2.getProcessor();
	ip3 = imp2.getProcessor();
	if (useMask) ip3 = imp3.getProcessor();
	roi1 = imp1.getRoi();
	roi2 = imp2.getRoi();
	Rectangle rect =ip1.getRoi();
	if (indexRoi==1) 
		{if(roi1==null) useROI=false;
		else
			{
			ipmask = imp1.getMask();
			rect = ip1.getRoi();
			}
		}
	if (indexRoi==2)
		{if(roi2==null)  useROI=false;
		else{ipmask = imp2.getMask();	rect = ip2.getRoi();}
		}
	if (useROI==false) {xOffset = 0;yOffset = 0; rwidth=width; rheight  =height;}
	else {xOffset = rect.x; yOffset = rect.y; rwidth=rect.width; rheight  =rect.height;}
	
	int g1=0;int g2=0;
	int histCount=0;
//calulate pearsons for existing image;
	for (int s=1; s<=nslices;s++)
		{if (currentSlice)
			{s=currentSliceNo;
			nslices=s;	
			}
		ip1 = img1.getProcessor(s);
		ip2 = img2.getProcessor(s);
		ip3 = img3.getProcessor(s);
		for (int y=0; y<rheight; y++) 
			{IJ.showStatus("Calculating r for original images. Press 'Esc' to abort");	
			if (IJ.escapePressed()) 
			{IJ.beep();  return;}
	            		for (int x=0; x<rwidth; x++) 
				{mask = (int)ip3.getPixelValue(x,y);
				if (indexRoi==0) mask=1;
				if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(x,y);
				if (mask!=0)
					{
		           	    		ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset); 
					if (ch1Max<ch1) ch1Max = ch1;
					if (ch1Min>ch1) ch1Min = ch1;
					if (ch2Max<ch2) ch2Max = ch2;
					if (ch2Min>ch2) ch2Min = ch2;	
					N++;
					if(ch1+ch2!=0) N2++;
					sumXtotal += ch1;
					sumYtotal += ch2;
					if(ch2>0) colocX += ch1;
					if(ch1>0) colocY += ch2;	
					sumX +=ch1;
					sumXY += (ch1 * ch2);
					sumXX += (ch1 * ch1);
					sumYY += (ch2 *ch2);
					sumY += ch2;
					if(ch1>0) Nr++;
					if(ch2>0)Ng++;

				//add ch2 value to histogram
					histCount = histogram[ch2];
					histCount++;
					histogram[ch2]=histCount;
					
					
					}
				}
			}
		}
	
	if (ignoreZeroZero) N = N2;
	//N = N2;
	//double ch1Mean = sumX/Nr;
	//double ch2Mean = sumY/Ng;
	double ch1Mean = sumX/N;
	double ch2Mean = sumY/N;
//	IJ.showMessage("Ch1: "+ch1Mean +"  Ch2:  "+ch2Mean +" count nonzerozero:   "+N);
	pearsons1 = sumXY - (sumX*sumY/N);
	pearsons2 = sumXX - (sumX*sumX/N);
	pearsons3 = sumYY - (sumY*sumY/N);
	//IJ.showMessage("p1: "+pearsons1+"    p2: "+pearsons2+"     p3: "+pearsons3);
	r= pearsons1/(Math.sqrt(pearsons2*pearsons3));
	double colocM1 = (double)(colocX/sumXtotal);
	double colocM2 = (double)(colocY/sumYtotal);

//calucalte ICQ
	int countAll=0;
	int countPos=0;
	double PDMobs=0;
	double PDM=0;
	double ICQ2;
	int countAll2=0;

	for (int s=1; s<=nslices;s++)
		{if (currentSlice)
			{s=currentSliceNo;
			nslices=s;	
			}
		ip1 = img1.getProcessor(s);
		ip2 = img2.getProcessor(s);
		ip3 = img3.getProcessor(s);
		for (int y=0; y<=rheight; y++) 
			{IJ.showStatus("Calculating r for original images. Press 'Esc' to abort");	
			if (IJ.escapePressed()) 
			{IJ.beep();  return;}
	            		for (int x=0; x<=rwidth; x++) 
				{mask = (int)ip3.getPixelValue(x,y);
				if (indexRoi==0) mask=1;
				if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(x,y);
				
				if (mask!=0)
					{
		           	    		ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset); 
					if (ch1+ch2!=0)
						{
						PDMobs = ((double)ch1-(double)ch1Mean)*((double)ch2-(double)ch2Mean);
						if (PDMobs>0) countPos++;
						countAll2++;
						}	
		
					}
				}
			}
		}
	
	//IJ.showMessage("count+ =  "+countPos	+"   CountNonZeroPair=  "+countAll2);
	ICQobs = ((double)countPos/(double)countAll2)-0.5;
	boolean ch3found= false;
	//do random localisations
	int rx=0;
	int ry = 0;
	int rz=0;
	int ch3;	
	GaussianBlur gb = new GaussianBlur();
	double r2mean=0;
	int slicesDone = 0;
	int xCount=0;
	int xOffset2 = -15;
	int yOffset2 = -10;
	int zOffset2=0;
	int startSlice=1;

	if(Costes)
		{
		xOffset2=0;
		yOffset2=0;
		}
	if (Fay) iterations = 25;
	if (nslices>=2&&Fay) zOffset2=-1;

	if (Fay&&nslices>=2) 
		{startSlice=2;
		nslices-=2;
		iterations=75;
		}

	if (vanS)
		{xOffset2=-21;
		startSlice=1;
		iterations=41;
		}

int blockNumberX = (int)(width/(psf*2));
int blockNumberY = (int)(height/(psf*2));

ImageProcessor blockIndex = new ByteProcessor(blockNumberX,blockNumberY);
 
double [] vSx = new double [41];
double [] vSr = new double [41] ;
int ch4=0;
int [] zUsed = new int [nslices];
//stackRand = new ImageStack(rwidth,rheight);
int blockCount=0;
boolean rBlock=true;
int vacant;
//start randomisations and calculation or Rrands
	for (int c=1; c<=iterations; c++)
		{
		stackRand = new ImageStack(rwidth,rheight);
		if(Fay)
			{if (c==26||c==51)
				{zOffset2 += 1;
				xOffset2=-15;
				yOffset2=-10;
				}
			if (xOffset2<10)	xOffset2+=5;
			else	{xOffset2 = -15;	yOffset2+=5;}
			}
		if(vanS)
			{
			//IJ.showMessage("xOffset:  "+xOffset2);
			xOffset2+=1;
			}
		
		
		for (int s=startSlice; s<=nslices; s++)	
			{
			ipRand = new ShortProcessor(rwidth,rheight);
			slicesDone++;
			if (currentSlice)
				{s=currentSliceNo;
				nslices=s;	
				}
			IJ.showStatus("Iteration "+c+ "/"+iterations+"  Slice: "+s +"/" +nslices+" 'Esc' to abort");
			if (IJ.escapePressed()) 
				{IJ.beep();  return;}
		     	ip1= img1.getProcessor(s);
			ip2 = img2.getProcessor(s+zOffset2);
			ip3 = img3.getProcessor(s);
			for (int y=0; y<rheight; y++)
				{
				for (int x=0; x<=rwidth;x++)
					{mask=1;
					if (useMask) mask = (int)ip3.getPixelValue(x,y);
							
					if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(x,y);
					if (indexRoi==0) mask=1;
					if (mask!=0)
						{
						ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
						ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset); 
						ch3 = 0;
						if ((ignoreZeroZero))
							{

							if (Fay) 	
								{
								ch3 = (int)ip2.getPixel(x+xOffset+xOffset2,y+yOffset+yOffset2); 
								ipRand.putPixel(x,y,ch3); 
								}
							if (vanS)	
								{ch3 = (int)ip2.getPixel(x+xOffset+xOffset2,y+yOffset);   
								ipRand.putPixel(x,y,ch3); 
								}
							if((Costes&&!randZ)||(Costes&&nslices<2))
								{ch4=1;
								while (ch4!=0||mask==0)
										{
										rx=(int)(Math.random()*(double)width);	
										ry = (int)(Math.random()*(double)height);
										if (useMask) mask = (int)ip3.getPixelValue(rx,ry);
										if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(rx,ry);
										ch4 = ipRand.getPixel(rx,ry);
										}
								ipRand.putPixel(rx,ry, ch2); 
								}

							if((Costes&&randZ&&nslices>1)&&ch2!=0)						
								{
								ch3 = (int)((Math.random()*(ch2Max-ch2Min))+ch2Min) ;
								ipRand.putPixel(x,y,ch3); 
								}
							
							}
							if (IJ.escapePressed()) 
								{IJ.beep();  return;}		
						//add to random image
							
							}		
						}		
					}
				if (Costes) gb.blur(ipRand, psf);
				stackRand.addSlice("Correlation Plot", ipRand);
			}
	//random image created now calculate r

		//reset values for r
			sumXX=0;
			sumX=0;
			sumXY = 0;
			sumYY = 0;
			sumY = 0;
			N = 0;
			N2=0;
			int s2=0;
			sumXtotal = 0;
			sumYtotal = 0;
			colocX = 0;
			colocY = 0;
			double ICQrand=0;
			int countPos2=0;
			countAll=0;
			//xOffset2=-21;
			if (IJ.escapePressed()) 
				{IJ.beep();  return;}
			for (int s=startSlice; s<=nslices;s++)
				{
				s2=s;
				if (Fay&&nslices>=2) s2-=1;
				if (currentSlice)
					{s=currentSliceNo;
					nslices=s;
					s2=1;	
					}
				ip1= img1.getProcessor(s);
				ip2 = stackRand.getProcessor(s2);
	       				for (int y=0; y<rheight; y++) 
					{
		           		for (int x=0; x<rwidth; x++) 
					{
					mask=1;
					if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(x,y);
					if (mask!=0)
						{ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
						ch2 = (int)ip2.getPixel(x,y);  
						if (ch1Max<ch1) ch1Max = ch1;
						if (ch1Min>ch1) ch1Min = ch1;
						if (ch2Max<ch2) ch2Max = ch2;
						if (ch2Min>ch2) ch2Min = ch2;
						N++;
					//Mander calc
						sumXtotal = sumXtotal+ch1;
						sumYtotal = sumYtotal+ch2;
						if(ch2>0) colocX = colocX + ch1;
						if(ch1>0) colocY = colocY + ch2;
						if((ch1+ch2!=0))	N2++;	
						sumX = 	sumX+ch1;
						sumXY = sumXY + (ch1 * ch2);
						sumXX = sumXX + (ch1 * ch1);
						sumYY = sumYY + (ch2 *ch2);
						sumY = sumY + ch2;
						if (ch1+ch2!=0)
							{PDM = ((double)ch1-(double)ch1Mean)*((double)ch2-(double)ch2Mean);
							if (PDM>0) countPos2++;
							countAll++;
							}	
						}
					}
				}
			}
		if (ignoreZeroZero) N = N2;	
		ICQ2 = ((double)countPos2/(double)countAll)-0.5;	
		ICQ2mean+=ICQ2;
		if (ICQobs>ICQ2) countICQ++;
		pearsons1 = sumXY - (sumX*sumY/N);
		pearsons2 = sumXX - (sumX*sumX/N);
		pearsons3 = sumYY - (sumY*sumY/N);
		r2= pearsons1/(Math.sqrt(pearsons2*pearsons3));
		
		if(vanS)
			{
			vSx[c-1] = (double)xOffset2;
			vSr[c-1] =  (double)r2 ;
			}

		if (r2<r2min) r2min = r2;
		if(r2>r2max) r2max = r2;
		//IJ.write("Random "+ c + "\t"+df3.format(r2)+ "\t"+ df3.format(coloc2M1)  + "\t"+df3.format(coloc2M2));
		//IJ.write(df3.format(r2));
		rVals.append(r2+"\n");
		r2mean = r2mean+r2;
		if (r>r2) colocCount++;
		sumr2sqrd =sumr2sqrd +(r2*r2);
		sumr2 = sumr2+r2;
		sumICQ2sqrd +=(ICQ2*ICQ2);
		sumICQ2 +=ICQ2;
		//IJ.write(IJ.d2s(ICQ2,3));
		}
//done randomisations
//calcualte mean Rrand
	r2mean = r2mean/iterations;
	r2sd = Math.sqrt(((iterations*(sumr2sqrd))-(sumr2*sumr2))/(iterations*(iterations-1)));
	ICQ2mean=ICQ2mean/iterations;
	double ICQ2sd  =Math.sqrt(((iterations*(sumICQ2sqrd))-(sumICQ2*sumICQ2))/(iterations*(iterations-1)));
	double Zscore =(r-r2mean)/r2sd;
	double ZscoreICQ = (ICQobs-ICQ2mean)/ICQ2sd;
	String icqPercentile= "<50%";

String Percentile = ""+(iterations-colocCount)+"/"+iterations;

//calculate percentage of Rrand that is less than Robs
//code from:
//http://www.cs.princeton.edu/introcs/26function/MyMath.java.html
//Thanks to Bob Dougherty
//50*{1 + erf[(V -mean)/(sqrt(2)*sdev)]

	double fx = 0.5*(1+erf(r-r2mean)/(Math.sqrt(2)*r2sd));
	if (fx>=1) fx=1;
	if (fx<=0) fx=0;
	String Percentile2 = IJ.d2s(fx,3)+"";
	if(keep)  new ImagePlus("Example random image", stackRand).show();
	double percColoc = ((double)colocCount/(double)iterations)*100;
	double percICQ =  ((double)countICQ/(double)iterations)*100;
	String Headings2 = "Image" 	
	+"\tR(obs)"
	+"\tR(rand) mean±sd"
	+"\tP-value"
	+"\tR(rand)>R(obs)"
	+"\tIterations"
	+" \tRandomisation"
	+"\tPSF width\n";

	String strPSF = "na";
	if (Costes||rBlocks) strPSF =  df3.format(psf*pixelSize*2)+ " µm ("+df0.format(psf*2)+" pix.)" ;
	String str = fileName  +
	
		"\t"+df3.format(r)+
		"\t" +df3.format(r2mean) + "±"+ df3.format(r2sd)+
		"\t"+Percentile2+ 
		"\t" + (Percentile )+
		"\t" +df0.format(iterations)+
		"\t" + randMethod+
		"\t" +strPSF;
	if (textWindow == null)
		textWindow = new TextWindow("Results",
		Headings2, str, 400, 250);
	else {
		textWindow.getTextPanel().setColumnHeadings(Headings2);
		textWindow.getTextPanel().appendLine(str);
	}

	IJ.selectWindow("Results");
	if (showR) new TextWindow( "Random R values", "R(rand)", rVals.toString(),300, 400);
	if(vanS)  
		{PlotWindow plot = new PlotWindow("CCF","x-translation","Pearsons",vSx,vSr);
		//r2min = (1.05*r2min);
		//r2max= (r2max*1.05);
		//plot.setLimits(-20, 20, r2min, r2max);
		plot.draw();
		}
	Prefs.set("Rand_ignore.boolean", ignoreZeroZero);
	Prefs.set("Rand_keep.boolean", keep);
	Prefs.set("Rand_manPSF.int", manPSF);
	Prefs.set("Rand_smooth.boolean", smooth);
	if (Costes) Prefs.set("Rand_iterations.int", (int)iterations);
	Prefs.set("Rand_ch2L.double",ch2Lambda);
	Prefs.set("Rand_NA.double",NA);
	Prefs.set("Rand_pixelSize.double",pixelSize);
	Prefs.set("Rand_currentSlice.boolean", currentSlice);
	Prefs.set("Rand_useManPSF.boolean", useManPSF);
	Prefs.set("Rand_showR.boolean", showR);
	Prefs.set("Rand_indexRoi.int",indexRoi);
	Prefs.set("Rand_indexRand.int",indexRand);
	Prefs.set("Rand_randZ.boolean",randZ);
	long elapsedTime = (System.currentTimeMillis()-startTime)/1000;
	String units = "secs";
	if (elapsedTime>90) 
		{elapsedTime /= 60; 
		units = "mins";}
	IJ.showStatus("Done.  "+ elapsedTime+ " "+ units);
	}

//code from:
//http://www.cs.princeton.edu/introcs/26function/MyMath.java.html

   public static double erf(double z) 
	{
	double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
        // use Horner's method
	double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                            t * ( 1.00002368 +
                                            t * ( 0.37409196 + 
                                            t * ( 0.09678418 + 
                                            t * (-0.18628806 + 
                                            t * ( 0.27886807 + 
                                            t * (-1.13520398 + 
                                            t * ( 1.48851587 + 
                                            t * (-0.82215223 + 
                                            t * ( 0.17087277))))))))));
	if (z >= 0) return  ans;
	else        return -ans;
    	}
}
 

