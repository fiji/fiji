import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;

// Autothreshold segmentation v0.1
// Following the guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines
// ImageJ plugin by G. Landini at bham. ac. uk

public class Auto_Threshold implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {
		// 1 - Obtain the currently active image:
		ImagePlus imp = IJ.getImage();

		if (null == imp){
			IJ.showMessage("There must be at least one image open");
			return;
		}

		if (imp.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}

		 // 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Auto Threshold v0.1");
//		String [] methods={"Bernsen", "Huang", "Intermodes", "IsoData",  "Li", "MaxEntropy", "MinError", "Minimum", "Moments", "Niblack", "Otsu", "Percentile", "RenyiEntropy", "Sauvola", "Shanbhag" , "Triangle", "Yen"};
		String [] methods={"Huang", "Intermodes", "IsoData",  "Li", "MaxEntropy", "MinError", "Minimum", "Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag" , "Triangle", "Yen"};
		gd.addChoice("Method", methods, methods[0]);
		String[] labels = new String[2];
		boolean[] states = new boolean[2];
		labels[0]="Ignore_black"; 
		states[0]=false;
		labels[1]="Ignore_white"; 
		states[1]=false;
		gd.addCheckboxGroup(1, 2, labels, states);

		gd.addCheckbox("White objects on black background",false);
		gd.addCheckbox("SetThreshold instead of Threshold (single images)",false);
		gd.addCheckbox("Show threshold values in log window",false);

		gd.showDialog();
		if (gd.wasCanceled()) return;
 
		// 3 - Retrieve parameters from the dialog
		String myMethod= gd.getNextChoice ();
		boolean noBlack = gd.getNextBoolean ();
		boolean noWhite = gd.getNextBoolean ();
		boolean doIwhite = gd.getNextBoolean ();
		boolean doIset = gd.getNextBoolean ();
		boolean doIlog = gd.getNextBoolean ();

		//if (imp.getStackSize()>1) {
		//	IJ.showMessage("Have to ask about stacks...");
		//}

		// 4 - Execute!
		//long start = System.currentTimeMillis();
		Object[] result = exec(imp, myMethod, noWhite, noBlack, doIwhite, doIset, doIlog );

		// 5 - If all went well, show the image:
		/* not needed here as the source image is binarised 
		if (null != result) {
			int threshold = result[0];
			ImagePlus resultImage = (ImagePlus) result[1];
		}
		*/
	}
	//IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");



	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, String myMethod, boolean noWhite, boolean noBlack, boolean doIwhite, boolean doIset, boolean doIlog ) {

		ImageProcessor ip = imp.getProcessor();
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int threshold=-1;
		int x, y, b=255, c=0;
		int [] data = (ip.getHistogram());

		// 0 - Check validity of parameters
		if (null == imp) return null;

		IJ.showStatus("Thresholding...");

		//1 Do it

		if (noBlack) data[0]=0;
		if (noWhite) data[255]=0;

		// Apply the selected algorithm

//		if (myMethod.equals("Bernsen")){
//			IJ.showMessage("Not yet...");
//		}
		if(myMethod.equals("Huang")){
			threshold = Huang(data);
		}
		else if(myMethod.equals("Intermodes")){
			threshold = Intermodes(data);
		}
		else if(myMethod.equals("IsoData")){
			threshold = IsoData (data); // so we can ignore black/white and set the bright or dark objects
			//IJ.showMessage("IJ "+ip.getAutoThreshold());
		}
		else if(myMethod.equals("Li")){
			threshold = Li(data);
		}
		else if(myMethod.equals("MaxEntropy")){
			threshold = MaxEntropy(data);
		}
		else if(myMethod.equals("MinError")){
			threshold = MinError(data);
		}
		else if(myMethod.equals("Minimum")){
			threshold = Minimum(data);
		}
		else if(myMethod.equals("Moments")){
			threshold = Moments(data);
		}
//		else if(myMethod.equals("Niblack")){
//			IJ.showMessage("Not yet...");
//		}
		else if(myMethod.equals("Otsu")){
			threshold = Otsu(data);
		}
		else if(myMethod.equals("Percentile")){
			threshold = Percentile(data);
		}
		else if(myMethod.equals("RenyiEntropy")){
			threshold = RenyiEntropy(data);
		}
//		else if(myMethod.equals("Sauvola")){
//				IJ.showMessage("Not yet...");
//		}
		else if(myMethod.equals("Shanbhag")){
			threshold = Shanbhag(data);
		}
		else if(myMethod.equals("Triangle")){
			threshold = Triangle(data);
		}
		else if(myMethod.equals("Yen")){
			threshold = Yen(data);
		}

		// show treshold in log window if required
		if (doIlog) IJ.log(myMethod+": "+threshold);
		if (threshold>-1){ 
			//threshold it
			if (doIwhite){
				c=255;
				b=0;
			}
			if (doIset){
				if (doIwhite) 
					IJ.setThreshold(threshold+1, 255);      
				else
					IJ.setThreshold(0,threshold);
			}
			else{
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++){
						if(ip.getPixel(x,y)>threshold)
							ip.putPixel(x,y,c);
						else
							ip.putPixel(x,y,b);
					}
				}
			}
		}
		//IJ.showProgress((double)(255-i)/255);
		imp.updateAndDraw();
		// 2 - Return the threshold and the image
		return new Object[] {threshold, imp};
	}

	int Huang(int [] data ) {
		// Implements Huang's fuzzy thresholding method 
		// Uses Shannon's entropy function (one can also use Yager's entropy function) 
		// Huang L.-K. and Wang M.-J.J. (1995) "Image Thresholding by Minimizing  
		// the Measures of Fuzziness" Pattern Recognition, 28(1): 41-51
		// M. Emre Celebi
		// 06.15.2007
		// Ported to ImageJ plugin by G. Landini from E Celebi's fourier_0.8 routines
		int threshold=-1;
		int ih, it;
		int first_bin;
		int last_bin;
		int sum_pix;
		int num_pix;
		double term;
		double ent;	/* entropy */
		double min_ent;	/* min entropy */
		double mu_x;

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < 256; ih++ ) {
			if ( data[ih] != 0 ) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=255;
		for (ih = 255; ih >= first_bin; ih-- ) {
			if ( data[ih] != 0 ) {
				last_bin = ih;
				break;
			}
		}
		term = 1.0 / ( double ) ( last_bin - first_bin );
		double [] mu_0 = new double[256];
		sum_pix = num_pix = 0;
		for ( ih = first_bin; ih < 256; ih++ ){
			sum_pix += ih * data[ih];
			num_pix += data[ih];
			/* NUM_PIX cannot be zero ! */
			mu_0[ih] = sum_pix / ( double ) num_pix;
		}

		double [] mu_1 = new double[256];
		sum_pix = num_pix = 0;
		for ( ih = last_bin; ih > 0; ih-- ){
			sum_pix += ih * data[ih];
			num_pix += data[ih];
			/* NUM_PIX cannot be zero ! */
			mu_1[ih - 1] = sum_pix / ( double ) num_pix;
		}

		/* Determine the threshold that minimizes the fuzzy entropy */
		threshold = -1;
		min_ent = Double.MAX_VALUE;
		for ( it = 0; it < 256; it++ ){
			ent = 0.0;
			for ( ih = 0; ih <= it; ih++ ) {
				/* Equation (4) in Ref. 1 */
				mu_x = 1.0 / ( 1.0 + term * Math.abs ( ih - mu_0[it] ) );
				if ( !((mu_x  < 1e-06 ) || ( mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += data[ih] * ( -mu_x * Math.log ( mu_x ) - ( 1.0 - mu_x ) * Math.log ( 1.0 - mu_x ) );
				}
			}

			for ( ih = it + 1; ih < 256; ih++ ) {
				/* Equation (4) in Ref. 1 */
				mu_x = 1.0 / ( 1.0 + term * Math.abs ( ih - mu_1[it] ) );
				if ( !((mu_x  < 1e-06 ) || ( mu_x > 0.999999))) {
					/* Equation (6) & (8) in Ref. 1 */
					ent += data[ih] * ( -mu_x * Math.log ( mu_x ) - ( 1.0 - mu_x ) * Math.log ( 1.0 - mu_x ) );
				}
			}
			/* No need to divide by NUM_ROWS * NUM_COLS * LOG(2) ! */
			if ( ent < min_ent ) {
				min_ent = ent;
				threshold = it;
			}
		}
		return threshold;
	}

	boolean bimodalTest(double [] y) {
		int len=y.length;
		boolean b = false;
		int modes = 0;
 
		for (int k=1;k<len-1;k++){
			if (y[k-1] < y[k] && y[k+1] < y[k]) {
				modes++;
				if (modes>2)
					return false;
			}
		}
		if (modes == 2)
			b = true;
		return b;
	}

	int Intermodes(int [] data ) {
		// J. M. S. Prewitt and M. L. Mendelsohn, "The analysis of cell images," in
		// Annals of the New York Academy of Sciences, vol. 128, pp. 1035-1053, 1966.
		// ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.
		//
		// Assumes a bimodal histogram. The histogram needs is smoothed (using a
		// running average of size 3, iteratively) until there are only two local maxima.
		// j and k
		// Threshold t is (j+k)/2.
		// Images with histograms having extremely unequal peaks or a broad and
		// ﬂat valley are unsuitable for this method.
		double [] iHisto = new double [256];
		int iter =0;
		int threshold=-1;
		for (int i=0; i<256; i++)
			iHisto[i]=(double) data[i];

		double [] tHisto = iHisto;

		while (!bimodalTest(iHisto) ) {
			 //smooth with a 3 point running mean filter
			for (int i=1; i<255; i++)
				tHisto[i]= (iHisto[i-1] + iHisto[i] + iHisto[i+1])/3;
			tHisto[0] = (iHisto[0]+iHisto[1])/3; //0 outside
			tHisto[255] = (iHisto[254]+iHisto[255])/3; //0 outside
			iHisto = tHisto;
			iter++;
			if (iter>10000) {
				threshold = -1;
				IJ.log("Intermodes Threshold not found after 10000 iterations.");
				return threshold;
			}
		}

		// The threshold is the mean between the two peaks.
		int tt=0;
		for (int i=1; i<255; i++) {
			if (iHisto[i-1] < iHisto[i] && iHisto[i+1] < iHisto[i]){
				tt += i;
				//IJ.log("mode:" +i);
			}
		}
		threshold = (int) Math.floor(tt/2.0);
		return threshold;
	}

	int IsoData(int [] data ) {
		// Iterative procedure based on the isodata algorithm [T.W. Ridler, S. Calvard, Picture 
		// thresholding using an iterative selection method, IEEE Trans. System, Man and 
		// Cybernetics, SMC-8 (1978) 630-632.] 
		// The procedure divides the image into objects and background by taking an initial threshold,
		// then the averages of the pixels at or below the threshold and pixels above are computed. 
		// The averages of those two values are computed, the threshold is incremented and the 
		// process is repeated until the threshold is larger than the composite average. That is,
		//  threshold = (average background + average objects)/2
		// The code in ImageJ that implements this function is the getAutoThreshold() method in the ImageProcessor class. 
		//
		// From: Tim Morris (dtm@ap.co.umist.ac.uk)
		// Subject: Re: Thresholding method?
		// posted to sci.image.processing on 1996/06/24
		// The algorithm implemented in NIH Image sets the threshold as that grey
		// value, G, for which the average of the averages of the grey values
		// below and above G is equal to G. It does this by initialising G to the
		// lowest sensible value and iterating:

		// L = the average grey value of pixels with intensities < G
		// H = the average grey value of pixels with intensities > G
		// is G = (L + H)/2?
		// yes => exit
		// no => increment G and repeat

		int i, l, toth, totl, h, g=0;
		for (i = 1; i < 256; i++){
			if (data[i] > 0){
				g = i + 1;
				break;
			}
		}

		while (true){
			l = 0;
			totl = 0;
			for (i = 0; i < g; i++) {
				 totl = totl + data[i];
				 l = l + (data[i] * i);
			}
			h = 0;
			toth = 0;
			for (i = g + 1; i < 256; i++){
				toth += data[i];
				h += (data[i]*i);
			}

			if (totl > 0 && toth > 0){
				l /= totl;
				h /= toth;
				if (g == (l + h) / 2)
					break;
			}
			g++;

			if (g > 254)
				return -1;
		}
		return g;
	}


	int Li(int [] data ) {
		// Implements Li's Minimum Cross Entropy thresholding method
		// This implementation is based on the iterative version (Ref. 2) of the algorithm.
		// 1) Li C.H. and Lee C.K. (1993) "Minimum Cross Entropy Thresholding" 
		//    Pattern Recognition, 26(4): 617-625
		// 2) Li C.H. and Tam P.K.S. (1998) "An Iterative Algorithm for Minimum 
		//    Cross Entropy Thresholding"Pattern Recognition Letters, 18(8): 771-776
		// 3) Sezgin M. and Sankur B. (2004) "Survey over Image Thresholding 
		//    Techniques and Quantitative Performance Evaluation" Journal of 
		//    Electronic Imaging, 13(1): 146-165 
		//    http://citeseer.ist.psu.edu/sezgin04survey.html
		// Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
		int threshold;
		int ih;
		int num_pixels;
		int sum_back; /* sum of the background pixels at a given threshold */
		int sum_obj;  /* sum of the object pixels at a given threshold */
		int num_back; /* number of background pixels at a given threshold */
		int num_obj;  /* number of object pixels at a given threshold */
		double old_thresh;
		double new_thresh;
		double mean_back; /* mean of the background pixels at a given threshold */
		double mean_obj;  /* mean of the object pixels at a given threshold */
		double mean;  /* mean gray-level in the image */
		double tolerance; /* threshold tolerance */
		double temp;

		tolerance=0.5;
		num_pixels = 0;
		for (ih = 0; ih < 256; ih++ ) 
			num_pixels += data[ih];

		/* Calculate the mean gray-level */
		mean = 0.0;
		for ( ih = 0 + 1; ih < 256; ih++ ) //0 + 1?
			mean += ih * data[ih];
		mean /= num_pixels;
		/* Initial estimate */
		new_thresh = mean;

		do{
			old_thresh = new_thresh;
			threshold = (int) (old_thresh + 0.5);	/* range */
			/* Calculate the means of background and object pixels */
			/* Background */
			sum_back = 0;
			num_back = 0;
			for ( ih = 0; ih <= threshold; ih++ ) {
				sum_back += ih * data[ih];
				num_back += data[ih];
			}
			mean_back = ( num_back == 0 ? 0.0 : ( sum_back / ( double ) num_back ) );
			/* Object */
			sum_obj = 0;
			num_obj = 0;
			for ( ih = threshold + 1; ih < 256; ih++ ) {
				sum_obj += ih * data[ih];
				num_obj += data[ih];
			}
			mean_obj = ( num_obj == 0 ? 0.0 : ( sum_obj / ( double ) num_obj ) );

			/* Calculate the new threshold: Equation (7) in Ref. 2 */
			//new_thresh = simple_round ( ( mean_back - mean_obj ) / ( Math.log ( mean_back ) - Math.log ( mean_obj ) ) );
			//simple_round ( double x ) {
			// return ( int ) ( IS_NEG ( x ) ? x - .5 : x + .5 );
			//}
			//
			//#define IS_NEG( x ) ( ( x ) < -DBL_EPSILON ) 
			//DBL_EPSILON = 2.220446049250313E-16
			temp = ( mean_back - mean_obj ) / ( Math.log ( mean_back ) - Math.log ( mean_obj ) );

			if (temp < -2.220446049250313E-16)
				new_thresh = (int) (temp - 0.5);
			else
				new_thresh = (int) (temp + 0.5);
			/*  Stop the iterations when the difference between the
			new and old threshold values is less than the tolerance */
		}
		while ( Math.abs ( new_thresh - old_thresh ) > tolerance );
		return threshold;
	}

	int MaxEntropy(int [] data ) {
		// Implements Kapur-Sahoo-Wong (Maximum Entropy) thresholding method
		// Kapur J.N., Sahoo P.K., and Wong A.K.C. (1985) "A New Method for
		// Gray-Level Picture Thresholding Using the Entropy of the Histogram"
		// Graphical Models and Image Processing, 29(3): 273-285
		// M. Emre Celebi
		// 06.15.2007
		// Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
		int threshold=-1;
		int ih, it;
		int first_bin;
		int last_bin;
		double tot_ent;  /* total entropy */
		double max_ent;  /* max entropy */
		double ent_back; /* entropy of the background pixels at a given threshold */
		double ent_obj;  /* entropy of the object pixels at a given threshold */
		double [] norm_histo = new double[256]; /* normalized histogram */
		double [] P1 = new double[256]; /* cumulative normalized histogram */
		double [] P2 = new double[256]; 

		int total =0;
		for (ih = 0; ih < 256; ih++ ) 
			total+=data[ih];

		for (ih = 0; ih < 256; ih++ )
			norm_histo[ih] = (double)data[ih]/total;

		P1[0]=norm_histo[0];
		P2[0]=1.0-P1[0];
		for (ih = 1; ih < 256; ih++ ){
			P1[ih]= P1[ih-1] + norm_histo[ih];
			P2[ih]= 1.0 - P1[ih];
		}

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < 256; ih++ ) {
			if ( !(Math.abs(P1[ih])<2.220446049250313E-16)) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=255;
		for (ih = 255; ih >= first_bin; ih-- ) {
			if ( !(Math.abs(P2[ih])<2.220446049250313E-16)) {
				last_bin = ih;
				break;
			}
		}

		// Calculate the total entropy each gray-level
		// and find the threshold that maximizes it 
		max_ent = Double.MIN_VALUE;

		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			for ( ih = 0; ih <= it; ih++ )  {
				if ( data[ih] !=0 ) {
					ent_back -= ( norm_histo[ih] / P1[it] ) * Math.log ( norm_histo[ih] / P1[it] );
				}
			}

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			for ( ih = it + 1; ih < 256; ih++ ){
				if (data[ih]!=0){
				ent_obj -= ( norm_histo[ih] / P2[it] ) * Math.log ( norm_histo[ih] / P2[it] );
				}
			}

			/* Total entropy */
			tot_ent = ent_back + ent_obj;

		// IJ.log(""+max_ent+"  "+tot_ent);
			if ( max_ent < tot_ent ) {
				max_ent = tot_ent;
				threshold = it;
			}
		}
		return threshold;
	}

	int MinError(int [] data ) {
		// Kittler J. and Illingworth J. (1986) Minimum Error Thresholding. Pattern Recognition, 19(1): 41-47
		// C code by M. Emre Celebi
		// ported to ImageJ plugin by G.Landini
		int nPixels=0;
		int i;
		int threshold=-1;

		double tt;
		double J_min, J_value;	/* minimum and current values of the J (class separability) criterion */
		int [] P1_zeros = new int[256];
		int [] P2_zeros = new int[256];
		double [] P1 = new double[256];/* P1 = cumulative normalized histogram and P2[i] = 1.0 - P1[i] */
		double [] P2 = new double[256];
		double [] mean1 = new double[256];/* means of the two classes (background, foreground) */
		double [] mean2 = new double[256];
		double [] stdev1 = new double[256];
		double [] stdev2 = new double[256];
		double [] histo = new double[256];

		for (i=0; i<256; i++)
			nPixels+=data[i];

		//normalise
		for (i=0; i<256; i++){
			histo[i]=(double)data[i]/(double)nPixels;
		}

		/* recursive calculations (P1, mean1, stdev1) */
		P1[0] = histo[0];
		mean1[0] = 0.0;
		stdev1[0] = 0.0;

		for(i=1; i<256; i++) {
			P1[i] = P1[i - 1] + histo[i];
			mean1[i] = mean1[i - 1] + i * histo[i];
			stdev1[i] = stdev1[i - 1] + i * i * histo[i];
		}

		/* iterative calculations (P2, mean2) */
		for(i=0; i<256; i++) {
			P2[i] = 1.0 - P1[i];
			P1_zeros[i] = Math.abs(P1[i])<0.000001?1:0;
			P2_zeros[i] = Math.abs(P2[i])<0.000001?1:0;
			mean2[i] = mean1[255] - mean1[i];
		}

		/* normalize mean1 & mean2 */
		for(i = 0; i < 256; i++){
			if(P1_zeros[i] == 0)
				mean1[i] /= P1[i];

		    if(P2_zeros[i] == 0)
				mean2[i] /= P2[i];
		}
 
		// Final Loop: Calculate the stdev1, stdev2, and J values.
		//  Find the minimum J value.
		J_min =Double.MAX_VALUE;

		for(i = 0; i < 256; i++) {
			/* calculate the stdev values */
			stdev2[i] = stdev1[255] - stdev1[i];

			if(P1_zeros[i] == 0)
				stdev1[i] = stdev1[i] / P1[i];

			if(P2_zeros[i] == 0)
				stdev2[i] = stdev2[i] / P2[i];

			stdev1[i] -= mean1[i] * mean1[i];
			stdev2[i] -= mean2[i] * mean2[i];

			/* calculate the J value */
			J_value = 1.0;
			if(P1_zeros[i] == 0) {
				if(Math.abs(stdev1[i])>0.000001) {
					tt=stdev1[i]<0.0?0.0:Math.sqrt(stdev1[i]);
					J_value += 2.0 * P1[i] * ((tt<0.0?0.0:Math.log(tt)) - Math.log(P1[i]));
				}
				else
					J_value += -2.0 * P1[i] * Math.log(P1[i]);
			}

			if(P2_zeros[i] == 0) {
				if(Math.abs(stdev2[i])>0.000001){
					tt=stdev2[i]<0.0?0.0:Math.sqrt(stdev2[i]);
					J_value += 2.0 * P2[i] * ((tt<0.0?0.0:Math.log(tt)) - Math.log(P2[i]));
				}
				else
					J_value += -2.0 * P2[i] * Math.log(P2[i]);
			}

			/* minimize the J criterion */
			if(J_value < J_min) {
				J_min = J_value;
				threshold = i;
			}
			//IJ.log(""+J_min+" "+i);
		}
		return threshold;
	}

	int Minimum(int [] data ) {
		// J. M. S. Prewitt and M. L. Mendelsohn, "The analysis of cell images," in
		// Annals of the New York Academy of Sciences, vol. 128, pp. 1035-1053, 1966.
		// ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.
		//
		// Assumes a bimodal histogram. The histogram needs is smoothed (using a
		// running average of size 3, iteratively) until there are only two local maxima.
		// Threshold t is such that yt−1 > yt ≤ yt+1.
		// Images with histograms having extremely unequal peaks or a broad and
		// ﬂat valley are unsuitable for this method.
		int iter =0;
		int threshold = -1;
		double [] iHisto = new double [256];

		for (int i=0; i<256; i++)
			iHisto[i]=(double) data[i];

		double [] tHisto = iHisto;

		while (!bimodalTest(iHisto) ) {
			 //smooth with a 3 point running mean filter
			for (int i=1; i<255; i++)
				tHisto[i]= (iHisto[i-1] + iHisto[i] +iHisto[i+1])/3;
			tHisto[0] = (iHisto[0]+iHisto[1])/3; //0 outside
			tHisto[255] = (iHisto[254]+iHisto[255])/3; //0 outside
			iHisto = tHisto;
			iter++;
			if (iter>10000) {
				threshold = -1;
				IJ.log("Minimum Threshold not found after 10000 iterations.");
				return threshold;
			}
		}
		// The threshold is the minimum between the two peaks.
		for (int i=1; i<255; i++) {
			if (iHisto[i-1] > iHisto[i] && iHisto[i+1] >= iHisto[i])
				threshold = i;
		}
		return threshold;
	}

	int Moments(int [] data ) {
		// W. Tsai, "Moment-preserving thresholding: a new approach," Computer Vision,
		// Graphics, and Image Processing, vol. 29, pp. 377-393, 1985.
		// Ported to ImageJ plugin by G.Landini from the the open source project FOURIER 0.8
		// by  M. Emre Celebi , Department of Computer Science,  Louisiana State University in Shreveport
		// Shreveport, LA 71115, USA
		//  http://sourceforge.net/projects/fourier-ipal
		//  http://www.lsus.edu/faculty/~ecelebi/fourier.htm
		double total =0;
		double m0=1.0, m1=0.0, m2 =0.0, m3 =0.0, sum =0.0, p0=0.0;
		double cd, c0, c1, z0, z1;	/* auxiliary variables */
		int threshold = -1;

		double [] histo = new  double [256];

		for (int i=0; i<256; i++)
			total+=data[i];

		for (int i=0; i<256; i++)
			histo[i]=(double)(data[i]/total); //normalised histgram

		/* Calculate the first, second, and third order moments */
		for ( int i = 0; i < 256; i++ ){
			m1 += i * histo[i];
			m2 += i * i * histo[i];
			m3 += i * i * i * histo[i];
		}
		/* 
		First 4 moments of the gray-level image should match the first 4 moments
		of the target binary image. This leads to 4 equalities whose solutions 
		are given in the Appendix of Ref. 1 
		*/
		cd = m0 * m2 - m1 * m1;
		c0 = ( -m2 * m2 + m1 * m3 ) / cd;
		c1 = ( m0 * -m3 + m2 * m1 ) / cd;
		z0 = 0.5 * ( -c1 - Math.sqrt ( c1 * c1 - 4.0 * c0 ) );
		z1 = 0.5 * ( -c1 + Math.sqrt ( c1 * c1 - 4.0 * c0 ) );
		p0 = ( z1 - m1 ) / ( z1 - z0 );  /* Fraction of the object pixels in the target binary image */

		// The threshold is the gray-level closest  
		// to the p0-tile of the normalized histogram 
		sum=0;
		for (int i=0; i<256; i++){
			sum+=histo[i];
			if (sum>p0) {
				threshold = i;
				break;
			}
		}
		return threshold;
	}

	int Otsu(int [] data ) {
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k,kStar;	// k = the current threshold; kStar = optimal threshold
		int N1, N;	// N1 = # points with intensity <=k; N = total number of points
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		double num, denom;  // temporary bookeeping
		int Sk;  // The total intensity for all histogram points <=k
		int S, L=256; // The total intensity of the image

		// Initialize values:
		S = N = 0;
		for (k=0; k<L; k++){
			S += k * data[k];	// Total histogram intensity
			N += data[k];		// Total number of data points
		}

		Sk = 0;
		N1 = data[0]; // The entry for zero intensity
		BCV = 0;
		BCVmax=0;
		kStar = 0;

		// Look at each possible threshold value,
		// calculate the between-class variance, and decide if it's a max
		for (k=1; k<L-1; k++) { // No need to check endpoints k = 0 or k = L-1
			Sk += k * data[k];
			N1 += data[k];

			// The float casting here is to avoid compiler warning about loss of precision and
			// will prevent overflow in the case of large saturated images
			denom = (double)( N1) * (N - N1); // Maximum value of denom is (N^2)/4 =  approx. 3E10

			if (denom != 0 ){
				// Float here is to avoid loss of precision when dividing
				num = ( (double)N1 / N ) * S - Sk; 	// Maximum value of num =  255*N = approx 8E7
				BCV = (num * num) / denom;
			}
			else
				BCV = 0;

			if (BCV >= BCVmax){ // Assign the best threshold found so far
				BCVmax = BCV;
				kStar = k;
			}
		}
		// kStar += 1;	// Use QTI convention that intensity -> 1 if intensity >= k
		// (the algorithm was developed for I-> 1 if I <= k.)
		return kStar;
	}


	int Percentile(int [] data ) {
		// W. Doyle, "Operation useful for similarity-invariant pattern recognition,"
		// Journal of the Association for Computing Machinery, vol. 9,pp. 259-267, 1962.
		// ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.

		int iter =0;
		int threshold = -1;
		double ptile= 0.5; // default fraction of foreground pixels
		double [] avec = new double [256];

		for (int i=0; i<256; i++)
			avec[i]=0.0;

		double total =partialSum(data, 255);
		double temp = 1.0;
		for (int i=0; i<256; i++){
			avec[i]=Math.abs((partialSum(data, i)/total)-ptile);
			//IJ.log("Ptile["+i+"]:"+ avec[i]);
			if (avec[i]<temp) {
				temp = avec[i];
				threshold = i;
			}
		}
		return threshold;
	}


	double partialSum(int [] y, int j) {
		double x = 0;
		for (int i=0;i<=j;i++)
			x+=y[i];
		return x;
	}

	int RenyiEntropy(int [] data ) {
		// Kapur J.N., Sahoo P.K., and Wong A.K.C. (1985) "A New Method for
		// Gray-Level Picture Thresholding Using the Entropy of the Histogram"
		// Graphical Models and Image Processing, 29(3): 273-285
		// M. Emre Celebi
		// 06.15.2007
		// Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines

		int threshold; 
		int opt_threshold;

		int ih, it;
		int first_bin;
		int last_bin;
		int tmp_var;
		int t_star1, t_star2, t_star3;
		int beta1, beta2, beta3;
		double alpha;/* alpha parameter of the method */
		double term;
		double tot_ent;  /* total entropy */
		double max_ent;  /* max entropy */
		double ent_back; /* entropy of the background pixels at a given threshold */
		double ent_obj;  /* entropy of the object pixels at a given threshold */
		double omega;
		double [] norm_histo = new double[256]; /* normalized histogram */
		double [] P1 = new double[256]; /* cumulative normalized histogram */
		double [] P2 = new double[256]; 

		int total =0;
		for (ih = 0; ih < 256; ih++ ) 
			total+=data[ih];

		for (ih = 0; ih < 256; ih++ )
			norm_histo[ih] = (double)data[ih]/total;

		P1[0]=norm_histo[0];
		P2[0]=1.0-P1[0];
		for (ih = 1; ih < 256; ih++ ){
			P1[ih]= P1[ih-1] + norm_histo[ih];
			P2[ih]= 1.0 - P1[ih];
		}

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < 256; ih++ ) {
			if ( !(Math.abs(P1[ih])<2.220446049250313E-16)) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=255;
		for (ih = 255; ih >= first_bin; ih-- ) {
			if ( !(Math.abs(P2[ih])<2.220446049250313E-16)) {
				last_bin = ih;
				break;
			}
		}

		/* Maximum Entropy Thresholding - BEGIN */
		/* ALPHA = 1.0 */
		/* Calculate the total entropy each gray-level
		and find the threshold that maximizes it 
		*/
		threshold =0; // was MIN_INT in original code, but if an empty image is processed it gives an error later on.
		max_ent = 0.0;

		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			for ( ih = 0; ih <= it; ih++ )  {
				if ( data[ih] !=0 ) {
					ent_back -= ( norm_histo[ih] / P1[it] ) * Math.log ( norm_histo[ih] / P1[it] );
				}
			}

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			for ( ih = it + 1; ih < 256; ih++ ){
				if (data[ih]!=0){
				ent_obj -= ( norm_histo[ih] / P2[it] ) * Math.log ( norm_histo[ih] / P2[it] );
				}
			}

			/* Total entropy */
			tot_ent = ent_back + ent_obj;

			// IJ.log(""+max_ent+"  "+tot_ent);

			if ( max_ent < tot_ent ) {
				max_ent = tot_ent;
				threshold = it;
			}
		}
		t_star2 = threshold;

		/* Maximum Entropy Thresholding - END */
		threshold =0; //was MIN_INT in original code, but if an empty image is processed it gives an error later on.
		max_ent = 0.0;
		alpha = 0.5;
		term = 1.0 / ( 1.0 - alpha );
		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			for ( ih = 0; ih <= it; ih++ )
				ent_back += Math.sqrt ( norm_histo[ih] / P1[it] );

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			for ( ih = it + 1; ih < 256; ih++ )
				ent_obj += Math.sqrt ( norm_histo[ih] / P2[it] );

			/* Total entropy */
			tot_ent = term * ( ( ent_back * ent_obj ) > 0.0 ? Math.log ( ent_back * ent_obj ) : 0.0);

			if ( tot_ent > max_ent ){
				max_ent = tot_ent;
				threshold = it;
			}
		}

		t_star1 = threshold;

		threshold = 0; //was MIN_INT in original code, but if an empty image is processed it gives an error later on.
		max_ent = 0.0;
		alpha = 2.0;
		term = 1.0 / ( 1.0 - alpha );
		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			for ( ih = 0; ih <= it; ih++ )
				ent_back += ( norm_histo[ih] * norm_histo[ih] ) / ( P1[it] * P1[it] );

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			for ( ih = it + 1; ih < 256; ih++ )
				ent_obj += ( norm_histo[ih] * norm_histo[ih] ) / ( P2[it] * P2[it] );

			/* Total entropy */
			tot_ent = term *( ( ent_back * ent_obj ) > 0.0 ? Math.log(ent_back * ent_obj ): 0.0 );

			if ( tot_ent > max_ent ){
				max_ent = tot_ent;
				threshold = it;
			}
		}

		t_star3 = threshold;

		/* Sort t_star values */
		if ( t_star2 < t_star1 ){
			tmp_var = t_star1;
			t_star1 = t_star2;
			t_star2 = tmp_var;
		}
		if ( t_star3 < t_star2 ){
			tmp_var = t_star2;
			t_star2 = t_star3;
			t_star3 = tmp_var;
		}
		if ( t_star2 < t_star1 ) {
			tmp_var = t_star1;
			t_star1 = t_star2;
			t_star2 = tmp_var;
		}

		/* Adjust beta values */
		if ( Math.abs ( t_star1 - t_star2 ) <= 5 )  {
			if ( Math.abs ( t_star2 - t_star3 ) <= 5 ) {
				beta1 = 1;
				beta2 = 2;
				beta3 = 1;
			}
			else {
				beta1 = 0;
				beta2 = 1;
				beta3 = 3;
			}
		}
		else {
			if ( Math.abs ( t_star2 - t_star3 ) <= 5 ) {
				beta1 = 3;
				beta2 = 1;
				beta3 = 0;
			}
			else {
				beta1 = 1;
				beta2 = 2;
				beta3 = 1;
			}
		}
		//IJ.log(""+t_star1+" "+t_star2+" "+t_star3);
		/* Determine the optimal threshold value */
		omega = P1[t_star3] - P1[t_star1];
		opt_threshold = (int) (t_star1 * ( P1[t_star1] + 0.25 * omega * beta1 ) + 0.25 * t_star2 * omega * beta2  + t_star3 * ( P2[t_star3] + 0.25 * omega * beta3 ));

		return opt_threshold;
	}


	int Shanbhag(int [] data ) {
		// Shanhbag A.G. (1994) "Utilization of Information Measure as a Means of
		//  Image Thresholding" Graphical Models and Image Processing, 56(5): 414-419
		// Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
		int threshold;
		int ih, it;
		int first_bin;
		int last_bin;
		double term;
		double tot_ent;  /* total entropy */
		double min_ent;  /* max entropy */
		double ent_back; /* entropy of the background pixels at a given threshold */
		double ent_obj;  /* entropy of the object pixels at a given threshold */
		double [] norm_histo = new double[256]; /* normalized histogram */
		double [] P1 = new double[256]; /* cumulative normalized histogram */
		double [] P2 = new double[256]; 

		int total =0;
		for (ih = 0; ih < 256; ih++ ) 
			total+=data[ih];

		for (ih = 0; ih < 256; ih++ )
			norm_histo[ih] = (double)data[ih]/total;

		P1[0]=norm_histo[0];
		P2[0]=1.0-P1[0];
		for (ih = 1; ih < 256; ih++ ){
			P1[ih]= P1[ih-1] + norm_histo[ih];
			P2[ih]= 1.0 - P1[ih];
		}

		/* Determine the first non-zero bin */
		first_bin=0;
		for (ih = 0; ih < 256; ih++ ) {
			if ( !(Math.abs(P1[ih])<2.220446049250313E-16)) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin=255;
		for (ih = 255; ih >= first_bin; ih-- ) {
			if ( !(Math.abs(P2[ih])<2.220446049250313E-16)) {
				last_bin = ih;
				break;
			}
		}

		// Calculate the total entropy each gray-level
		// and find the threshold that maximizes it 
		threshold =-1;
		min_ent = Double.MAX_VALUE;

		for ( it = first_bin; it <= last_bin; it++ ) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			term = 0.5 / P1[it];
			for ( ih = 1; ih <= it; ih++ )  { //0+1?
				ent_back -= norm_histo[ih] * Math.log ( 1.0 - term * P1[ih - 1] );
			}
			ent_back *= term;

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			term = 0.5 / P2[it];
			for ( ih = it + 1; ih < 256; ih++ ){
				ent_obj -= norm_histo[ih] * Math.log ( 1.0 - term * P2[ih] );
			}
			ent_obj *= term;

			/* Total entropy */
			tot_ent = Math.abs ( ent_back - ent_obj );

			if ( tot_ent < min_ent ) {
				min_ent = tot_ent;
				threshold = it;
			}
		}
		return threshold;
	}


	int Triangle(int [] data ) {
		//   Zack, G. W., Rogers, W. E. and Latt, S. A., 1977,
		//  Automatic Measurement of Sister Chromatid Exchange Frequency,
		// Journal of Histochemistry and Cytochemistry 25 (7), pp. 741-753
		//
		//  from Johannes Schindelin plugin
		// 
		// find min and max
		int min = 0, dmax=0, max = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i]>0){
				min=i;
				break;
			}
		}

		for (int i =0; i < data.length; i++) {
			if (data[i] >dmax) {
				max=i;
				dmax=data[i];
			}
		}
		//IJ.log(""+min+" "+max);

		if (min == max)
			return min;

		// describe line by nx * x + ny * y - d = 0
		double nx, ny, d;
		nx = data[max] - data[min];
		ny = min - max;
		d = Math.sqrt(nx * nx + ny * ny);
		nx /= d;
		ny /= d;
		d = nx * min + ny * data[min];

		// find split point
		int split = min;
		double splitDistance = 0;
		for (int i = min + 1; i <= max; i++) {
			double newDistance = nx * i + ny * data[i] - d;
			if (newDistance > splitDistance) {
				split = i;
				splitDistance = newDistance;
			}
		}
		return split-1;
	}

	int Yen(int [] data ) {
		// Implements Yen  thresholding method
		// 1) Yen J.C., Chang F.J., and Chang S. (1995) "A New Criterion 
		//    for Automatic Multilevel Thresholding" IEEE Trans. on Image 
		//    Processing, 4(3): 370-378
		// 2) Sezgin M. and Sankur B. (2004) "Survey over Image Thresholding 
		//    Techniques and Quantitative Performance Evaluation" Journal of 
		//    Electronic Imaging, 13(1): 146-165
		//    http://citeseer.ist.psu.edu/sezgin04survey.html
		//
		// M. Emre Celebi
		// 06.15.2007
		// Ported to ImageJ plugin by G.Landini from E Celebi's fourier_0.8 routines
		int threshold;
		int ih, it;
		double crit;
		double max_crit;
		double [] norm_histo = new double[256]; /* normalized histogram */
		double [] P1 = new double[256]; /* cumulative normalized histogram */
		double [] P1_sq = new double[256]; 
		double [] P2_sq = new double[256]; 

		int total =0;
		for (ih = 0; ih < 256; ih++ ) 
			total+=data[ih];

		for (ih = 0; ih < 256; ih++ )
			norm_histo[ih] = (double)data[ih]/total;

		P1[0]=norm_histo[0];
		for (ih = 1; ih < 256; ih++ )
			P1[ih]= P1[ih-1] + norm_histo[ih];

		P1_sq[0]=norm_histo[0]*norm_histo[0];
		for (ih = 1; ih < 256; ih++ )
			P1_sq[ih]= P1_sq[ih-1] + norm_histo[ih] * norm_histo[ih];

		P2_sq[255] = 0.0;
		for ( ih = 254; ih >= 0; ih-- )
			P2_sq[ih] = P2_sq[ih + 1] + norm_histo[ih + 1] * norm_histo[ih + 1];

		/* Find the threshold that maximizes the criterion */
		threshold = -1;
		max_crit = Double.MIN_VALUE;
		for ( it = 0; it < 256; it++ ) {
			crit = -1.0 * (( P1_sq[it] * P2_sq[it] )> 0.0? Math.log( P1_sq[it] * P2_sq[it]):0.0) +  2 * ( ( P1[it] * ( 1.0 - P1[it] ) )>0.0? Math.log(  P1[it] * ( 1.0 - P1[it] ) ): 0.0);
			if ( crit > max_crit ) {
				max_crit = crit;
				threshold = it;
			}
		}
		return threshold;
	}

}

