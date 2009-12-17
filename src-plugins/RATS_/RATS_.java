import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.*;
import ij.util.*;
/**
  * RATS_ (Robust Automatic Threshold Selection) ImageJ plugin. For use 
  * with companion RATSQuadtree class.  Adaptation of C implementation by 
  * M.H.F. Wilkinson, 2000.
  *
  * Based upon work of M.H.F. Wilkinson and others, 
  * RATS establishes regionalized thresholds for a greyscale image
  * where the regions are established using quadtree architecture. Within each 
  * of the lowest quadtree regions the threshold is calculated as the sum of 
  * the orginal pixels weighted by the gradient pixels.
  * threshold = SUM(edge*orig)/SUM(edge) for pixels within each region.
  * The threshold calculated in each region is required to 
  * meet minimum criteria - these criteria are determined by the user as
  * a noise estimate (sigma) and a scaling factor (lambda).  The user may also
  * select the minimum region size.  The thresholds for all
  * region are then interpolated (bilinear) across the entire image.
  * In general, the best values for each of three parameters are determined 
  * by trial and error for a given suite of images.
  *  
  * Plugin Author: Ben Tupper (btupper@bigelow.org) for 
  *  Mike Sieracki at Bigelow Laboratory for Ocean Science.
  *
  * Plugin Version:
  * 2009-01-08
  * 2009-12-11 BTT small fix to verbose (defaults to false now) option and title for mask
  * 
  * The following has been information is copied directly from the header
  * of Wilkinson's rats.c.
  * ---------------------------------------------------------------------                                                                       
  * module rats.h: implements RATS algorithm for local, bilevel               
  *                thresholding, using square-sobel filtering                
  *                                                                          
  * References:    M.H.F. Wilkinson (1998) Optimizing edge detectors for     
  *                robust automatic threshold selection. Graph. Models       
  *                Image Proc. 60:                                           
  *                M.H.F. Wilkinson (1996) Rapid automatic segmentation of   
  *                fluorescent and phase-contrast images of bacteria. In:    
  *                Fluorescence Microscopy and Fluorescent Probes,           
  *                (J. Slavik, ed), pp 261-266, Plenum Press, New York.       
  * Author:        Michael H. F. Wilkinson                                   
  *                Institute for Mathematics and Computing Science           
  *                University of Groningen,                                  
  *                PO Box 800, 9700 AV Groningen, The Netherlands            
  *                e-mail: michael@cs.rug.nl                                 
  * Version:       14-04-2000                                                
  * Comments:      Feel free to use non-commercially, but do acknowledge     
  *                copyright, or (even better) cite articles above  :-)    
  * ---------------------------------------------------------------------
  *
  * To the above references please add...
  * M.H.F. Wilkinson "Segmentation Techniques in Image Analysis of Microbes" 
  * which is chapter 3 in  Digital Image Analysis of Microbes: Imaging,
  * Morphometry, Fluorometry and Motility Techniques and Applications. 
  * Edited by M.H.F. Wilkinson and F. Schut, Wiley Modern Microbiology Methods 
  * Series (1998). 
  **/
                                                                            
public class RATS_ implements PlugInFilter {
	ImagePlus imp;
	ImageProcessor ip, topIp, botIp, threshIp;
	RATSQuadtree qtTop, qtBot; //the quadtree (top level)
  private boolean bVerbose = false; //produce informational messages
  private double[] minSzPx = {32.0, 32.0};//anticipates future non-square leaves?
  private double sigma = 25.0; //aka "noise" as S.D. of image background
  private double lambda = 3.0; //scaling factor
  float[][] p; //copy of the original pixels
	float[][] top; //the numerator (SUM(edge*orig))
	float[][] bot; // the denominator (SUM(edge))
	int[] dim = new int[2]; //input image dims as [x,y]
	long startTime = System.currentTimeMillis();	
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + DOES_16 + DOES_32;
	}

	public void run(ImageProcessor ip) {
    this.ip = ip;
    this.dim[0] = ip.getWidth();
    this.dim[1] = ip.getHeight();
    //suggest a size that would have 5 levels
    minSzPx[0] = (double) ((int) dim[0]/5.0);
    minSzPx[1] = (double) ((int) dim[1]/5.0);
    if (minSzPx[0] < minSzPx[1]) {
      minSzPx[1] = minSzPx[0]; 
    } else {
      minSzPx[0] = minSzPx[1];
    }
    String opt = Macro.getOptions();
    if ((opt != null) && (opt.length() != 0)){
     // IJ.log("opt=" + opt);
      String[] s;
      String[] opts = opt.split(" ");
      for (int i = 0; i < opts.length; i++){
        s = opts[i].split("=");
        if (s[0].equalsIgnoreCase("noise")){
          sigma = Tools.parseDouble(s[1]);
        } else if (s[0].equalsIgnoreCase("lambda")){
          lambda = Tools.parseDouble(s[1]);
        } else if (s[0].equalsIgnoreCase("minSzPx")) {
          minSzPx[0] = Tools.parseDouble(s[1]);
          minSzPx[1] = minSzPx[0];
        } else if (s[0].equalsIgnoreCase("min")) {
          minSzPx[0] = Tools.parseDouble(s[1]);
          minSzPx[1] = minSzPx[0];
        }else if (s[0].equalsIgnoreCase("verbose")) {
          bVerbose = true;
        }else {
            IJ.log("oops! unrecognized argument: " + opts[i]);
        }
      } //for loop through arguments
    } else {
      if (showDialog() == false) {return;}
    }
    
 
     
    this.qtTop = new RATSQuadtree(this.dim, this.minSzPx);
    this.qtBot = new RATSQuadtree(this.dim, this.minSzPx);
    if (bVerbose) {
      IJ.log("**** RATS ****");
      IJ.log("  Image = " + imp.getTitle());
      IJ.log("  Noise = " + sigma);
      IJ.log("  Lambda = " + lambda);
      IJ.log("  Min Leaf Size = " + minSzPx[0]);
      IJ.log("  NLevels = " + qtTop.countLevels());
    }
    long startTime = System.currentTimeMillis();
    process();
    if (bVerbose) {
      long elapsed = System.currentTimeMillis() - startTime;
      IJ.log("  Elapsed time = " + elapsed  + " ms for processing");
    }
	}
  
  /** primary work horse*/
  private void process(){
   
   tick();
   fillArrays();
   if (bVerbose) tock("  FillArrays:");
   
   tick();
   gradientMHFW();
   if (bVerbose) tock("  Gradient:");
   
   tick();
   this.topIp = new FloatProcessor(top);
   qtTop.fillWithSums(topIp);
   this.botIp = new FloatProcessor(bot);
   qtBot.fillWithSums(botIp);
   if (bVerbose) tock("  FillWithSums:");   
   
   tick();
   float[][] thresh = rats();
   if (bVerbose) tock("  Rats:");
   
   tick();
   thresh = resize(thresh);
   if (bVerbose) tock("  Resize:");
   
   this.threshIp = new FloatProcessor(thresh);
   String title = imp.getShortTitle()+"-mask";
   ImagePlus resultImp = new ImagePlus(title, threshIp.convertToByte(true));
   resultImp.show();
  }
  
/**
  * Resizes the threshold to image size uisng bilinear interpolation
  * this technique closely mirrors that used by Wilkinson in the 
  * origional rats.c.  The big difference is that interpolate locations
  * are predetermined in this method.
  * @param in the float array (presumably the threshold values for each leaflet)
  * @return the interpolated array in the original image size
  */
  private float[][] resize(float[][] in){
   
    int oh = dim[1];
    int ow = dim[0];
    int iw = in.length;
    int ih = in[0].length;
    float[][] out = new float[ow][oh];
    float[] sx = new float[ow];
    float[] sy = new float[oh];
    int[] ix = new int[ow];
    int[] iy = new int[oh];
    float r = 0.0f;
    float gx = 0.0f;
    float gy = 0.0f;
    float dx = 0.0f;
    float dy = 0.0f;
    float one = 1.0f;
    float on = 255.0f;

    //preconfigures the interpolate coords in iw and ih coords
    //plus one extra
    // s[xy] are the coords in "real" numbers
    // i[xy] are the equivalent integers
    for (int x = 0; x < ow; x++) {
       sx[x] = ((float) x )/((float)(ow-1)) * (iw-1);
       ix[x] = (int) sx[x];
    }
    sx[ow-1] = sx[ow-2] + 0.99f * (sx[ow-1]-sx[ow-2]);
    ix[ow-1] = (int) sx[ow-1];
    
    for (int y = 0; y < oh; y++) {
      sy[y] = ((float) y)/((float)(oh-1)) * (ih-1);
      iy[y] = (int) sy[y];
      //IJ.log("y="+y + " sy = " + sy[y] + " iy = " + iy[y]);
    }
    sy[oh-1] = sy[oh-2] + 0.99f * (sy[oh-1]-sy[oh-2]);
    iy[oh-1] = (int) sy[oh-1];

    
    for (int x = 0; x < ow; x++){
      for (int y = 0; y < oh; y++){
        //dx = (ix[x] + 1 - sx[x]) * in[ix[x]][iy[y]] + (sx[x]-ix[x])*in[ix[x]+1][iy[y]];
        //gx = (ix[x] + 1 - sx[x]) * in[ix[x]][iy[y]+1] + (sx[x]-ix[x])*in[ix[x]+1][iy[y]+1];
        dx = (ix[x] + 1 - sx[x]) * in[ix[x]][iy[y]] + (sx[x]-ix[x])*in[ix[x]+1][iy[y]];
        gx = (ix[x] + 1 - sx[x]) * in[ix[x]][iy[y]+1] + (sx[x]-ix[x])*in[ix[x]+1][iy[y]+1];
        r=(iy[y] + 1 - sy[y]) * dx + (sy[y]-iy[y])*gx;
        if (r < p[x][y]){out[x][y] = on;}
        //out[x][y] = r;
        //if ((r == 0)|| (Float.isNaN(r))) IJ.log("out["+x+"]["+y+"] = " + r);
      }//y
    }//x
    
    return out;
  }//resize
   

/**
  * Builds the RATS threshold image - which will be subsequently resized to the full
  * image size.  Each leaf is inspected, if it fails the threshold [(lambda*sigma)^2 
  * Wilkinson throws in a "3" which i don't understand by replicate] then 
  * return the threshold value for the parent quadtree.
  * @return An array of threshold values - each element represent the 
  * thereshold for a single leaflet in the bottom of the tree.
  */
  private float[][] rats(){
    int depth = qtTop.countLevels();
    RATSQuadtree[][] qTop = qtTop.getLevel(depth-1);
    RATSQuadtree[][] qBot = qtBot.getLevel(depth-1);

    float[][] threshP = qtTop.makeArrayFloat(depth-1);
    float test = (float) (3.0 * lambda * sigma * lambda * sigma );
    //float test = (float) (lambda*lambda*sigma*sigma);
    //if (bVerbose) {IJ.log("Test value for leaflets = " + test);}
    for (int x = 0; x < threshP.length; x++){
      for (int y = 0; y < threshP[0].length; y++){
        threshP[x][y] = ratsThresh(qTop[x][y],qBot[x][y], test);
      }//xloop
    } //yloop
    return threshP;
  }//rats
  
  
/** 
  * Determines the threshold for a single leaflet - becomes 
  * upwardly recursive as needed (if a leaflet fails the test)
  *
  * @param leaflet the quadtree in question
  * @return the threshold value selected
  */
  private float ratsThresh(RATSQuadtree qTop, RATSQuadtree qBot, float test){
    float topV = qTop.getSumFloat();
    float botV = qBot.getSumFloat();
    if (botV > test) {      
      return topV/botV; //passes the test
    } else { 
      if (qTop.getLevel() == 0) {
        return -1.0f; //top level can't go higher
      } else { 
        // fails the test, get the value for the parent
        return ratsThresh(qTop.getParent(),qBot.getParent(), test);
      }
    }//test
  }//ratsThresh

/**
  * Calculates the gradient and numerator (gradient * original)
  * Stores results in gradImp and numImp.  Pixels below threshold are 
  * rejected (left as 0 value).  Modeled after Wilkinson's modified
  * Sobel operators.
  */
  private void gradientMHFW(){
    float tempG = 0.0f;
    float wx = 0.0f;
    float wy = 0.0f;
    int xL = 0;
    int xR = 0;
    int yU = 0;
    int yD = 0;
    float scale = 1.0f/16.0f; //normalize as multiplication (faster)
    float lambdasigma = (float) (lambda * sigma * lambda * sigma); //test value
    int w = dim[0];
    int h = dim[1];
    int wm1 = w-1;
    int hm1 = h-1;  
    float two = 2.0f;
    
    //step down to each row
    for (int y = 0; y < h; y ++) {
      //be careful around image edges
      if ((y % hm1) == 0) {
        yU = y - ((y == 0) ? 0 : 1);
        yD = y + ((y == hm1) ? 0: 1);
      } else {
        yU = y - 1;
        yD = y + 1;
      }
      // zip down the elements of that row
      for (int x = 0; x < w; x++){
        //be careful around image edges
        if ((x % wm1) == 0){
          xL = x - ((x == 0) ? 0 : 1);
          xR = x + ((x == wm1) ? 0: 1);
        } else {
          xL = x - 1;
          xR = x + 1;
        }
        
        //sobel x gradient
        wx = p[xL][yU] - p[xL][yD] + two*(p[x][yU]-p[x][yD]) + p[xR][yU] - p[xR][yD];
        //sobel y gradient
        wy = p[xL][yU] - p[xR][yU] + two*(p[xL][y]-p[xR][y]) + p[xL][yD] - p[xR][yD];
        //normalize
        tempG = (wx*wx + wy*wy)*scale;
        //test - is big enough assign to gradient otherwise leave as zero
        if (tempG > lambdasigma) {
          bot[x][y] = tempG;
          top[x][y] = p[x][y] * tempG;
        } // otherwise the edge and numerator are left as zero
      } // x loop
    } //y loop     
  }//gradientMHFW


/**
  * fills p the values in the original and creates 
  * top and bot with 0 value everywhere
  */
  private void fillArrays(){
   
   this.p = new float[dim[0]][dim[1]];
   this.top = new float[dim[0]][dim[1]];
   this.bot = new float[dim[0]][dim[1]];
   for (int y = 0; y < dim[1] ; y++){
    for (int x = 0; x < dim[0]; x++){
      p[x][y] = ip.getPixelValue(x,y);
    }//x loop
   }//y loop
  }//fillArrays

  private boolean showDialog() {
        
    GenericDialog gd = new GenericDialog("RATS");
    gd.addNumericField("Noise Threshold:", sigma, 0);
    gd.addNumericField("Lambda Factor:", lambda, 0);
    gd.addNumericField("Min Leaf Size (pixels):", minSzPx[0], 0);
    gd.addCheckbox("Verbose", bVerbose);
    
    gd.showDialog();
    if (gd.wasCanceled()) {return false;}
    
    sigma = gd.getNextNumber();
    lambda = gd.getNextNumber();
    minSzPx[0] = gd.getNextNumber();
    minSzPx[1] = minSzPx[0];
    bVerbose = gd.getNextBoolean();
    return true;
  }//showDialog

  private long tick(){
      this.startTime = System.currentTimeMillis();
      return startTime;
    }
    
  private long tock(){
      return (System.currentTimeMillis() - this.startTime);
    }
      
  private long tock(String message){
     long elapsedTime = tock();
     IJ.log(message + " " + elapsedTime + " ms"); 
     return elapsedTime;
  }  

}//class
