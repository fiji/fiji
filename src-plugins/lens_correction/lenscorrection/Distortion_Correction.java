/**

ImageJ plugin
Copyright (C) 2008 Verena Kaynig.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
**/


/* **************************************************************************** *
 * This ImageJ Plugin estimates a non linear lens distortion in                 *
 * the images given and corrects all images with the same transformation        *
 *                                                                              *
 *                                                                              *
 * TODO:                                                                        *
 * 	- show histogram of beta coefficients to evaluate kernel dimension needed   * 
 *  - show SIFT matches before and after correction?                            *
 *  - give numerical xcorr results in a better manner                           *
 *  - show visual impression of the stitching ?                                 *
 *  - DOKUMENTATION                                                             *
 *  - do mask images properly to incorporate into TrakEM                        *
 *                                                                              *
 *  Author: Verena Kaynig                                                       *
 *  Kontakt: verena.kaynig@inf.ethz.ch                                          *
 *                                                                              *
 *  SIFT implementation: Stephan Saalfeld                                       *	
 * **************************************************************************** */

package lenscorrection;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.FileSaver;

import ini.trakem2.imaging.Registration;
import mpi.fruitfly.general.MultiThreading;
import mpicbg.models.*;
import mpicbg.imagefeatures.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.io.BufferedWriter;
import java.io.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import Jama.Matrix;


public class Distortion_Correction implements PlugIn{
	
    int dimension = 0;
    String source_dir = "";
    String target_dir = "";
    String saveFileName = "distCorr.txt";
    //original and calibrated images are supposed to have the same names, 
    //but are in different directories
    String[] names;
    //FIXME: atm I am using the same model for the original and the calibrated images
    //this is kind of sloppy but saves computation time (and mine)
    RigidModel2D[] models;	
    int numberOfImages = 0;
    boolean applyCorrection = true;
    boolean visualizeResults = true;
    int firstImageIndex = 0;
    int saveOrLoad = 0;
	
    final Registration.SIFTParameters sp =  new Registration.SIFTParameters(null);
    NonLinearTransform nlt = new NonLinearTransform();
	
    public void run(String arg) {
	//taken from Alberts registration plugin
		
	if (source_dir == ""){
	    DirectoryChooser dc = new DirectoryChooser("Calibration Images");
	    source_dir = dc.getDirectory();
	    if (null == source_dir) return;
	
	    source_dir = source_dir.replace('\\', '/');
	    if (!source_dir.endsWith("/")) source_dir += "/";
	}
	// get file listing
	final String exts = ".tif.jpg.png.gif.tiff.jpeg.bmp.pgm";
	names = new File(source_dir).list(new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    int idot = name.lastIndexOf('.');
		    if (-1 == idot) return false;
		    return exts.contains(name.substring(idot).toLowerCase());
		}
	    });
	Arrays.sort(names);
	
	GenericDialog gd = new GenericDialog("Input");
	gd.addNumericField("number of images: ", 9, 0);
	gd.addNumericField("power of polyn. kernel ", 5, 0);
	gd.addNumericField("lambda ", 0.000001, 6);
	gd.addChoice("first image", names, names[0]);
	gd.addCheckbox("apply correction to images", true);
	gd.addCheckbox("visualize results", false);
	String[] options = {"save", "load"};
	gd.addChoice("What to do? ", options, "save");
	gd.addStringField("file name: ", "distCorr.txt");
	gd.showDialog();
		
	if (gd.wasCanceled()) return;
	numberOfImages = (int) gd.getNextNumber();
	int dimension = (int) gd.getNextNumber();
	double lambda = gd.getNextNumber();
	firstImageIndex = gd.getNextChoiceIndex(); System.out.println("firstImageIndex: " + firstImageIndex);
	applyCorrection = gd.getNextBoolean();
	visualizeResults = gd.getNextBoolean();
	saveOrLoad = gd.getNextChoiceIndex();
	saveFileName = gd.getNextString();
		
		
		
	ImagePlus imgTmp = new Opener().openImage(source_dir + names[0]);
	int imageWidth=imgTmp.getWidth(), imageHeight=imgTmp.getHeight();
  //imgTmp was just needed to get width and height
  //of the images
  imgTmp.flush();
		
	Vector<Vector<PointMatch>> inliers = null;
		
	if((saveOrLoad==0) || visualizeResults){
	    sp.setup();
		
		
	    Vector<Feature>[] siftFeatures = extractSIFTFeaturesThreaded(numberOfImages, source_dir, names);
		
	    Vector[] inliersTmp = new Vector[numberOfImages*(numberOfImages-1)];
	    models = new RigidModel2D[numberOfImages*(numberOfImages-1)];
		
		
	    IJ.showStatus("Estimating Correspondences");
	    for(int i=0; i < numberOfImages; i++){
		IJ.log("Estimating correspondences of image " + i);
		IJ.showProgress((i+1)/(double)numberOfImages);
		extractSIFTPointsThreaded(i, siftFeatures,inliersTmp);
	    }
		
	    int wholeCount = 0;
	    inliers = new Vector<Vector<PointMatch>>(); 
	    for (int i=0; i < inliersTmp.length; i++){
		// if vector at inliersTmp[i] contains only one null element,
		// its size is still 1
		  if (inliersTmp[i].size() > 10){
		    wholeCount += inliersTmp[i].size();
		}
		//if I do not do this then the models have not the 
		//right index corresponding to the inliers positions in the vector
		inliers.add(inliersTmp[i]);
	    }
		
	    //this is really really ugly
	    double[][] tp = new double[wholeCount][6];
	    double h1[][] = new double[wholeCount][2];
	    double h2[][] = new double[wholeCount][2];
	    int count = 0;
	    for (int i=0; i < inliers.size(); i++){
		if (inliers.get(i).size() > 10){
		    double[][] points1 = new double[inliers.get(i).size()][2];
		    double[][] points2 = new double[inliers.get(i).size()][2];
			
		    for (int j=0; j < inliers.get(i).size(); j++){
					
			float[] tmp1 = ((PointMatch) inliers.get(i).get(j)).getP1().getL();
			float[] tmp2 = ((PointMatch) inliers.get(i).get(j)).getP2().getL();
			    
			points1[j][0] = (double) tmp1[0];
			points1[j][1] = (double) tmp1[1];
			points2[j][0] = (double) tmp2[0];
			points2[j][1] = (double) tmp2[1];
				    
			h1[count] = new double[] {(double) tmp1[0], (double) tmp1[1]};
			h2[count] = new double[] {(double) tmp2[0], (double) tmp2[1]};
				  
			models[i].getAffine().getMatrix(tp[count]);				    
			count++; 
		    }
			
		}
	    }	

	    if (saveOrLoad == 0){
		nlt = distortionCorrection(h1, h2, tp, dimension, lambda, imageWidth, imageHeight);
                nlt.visualizeSmall(lambda);

		while(true){
		GenericDialog gdl = new GenericDialog("New lambda?");
		gdl.addMessage("If the distortion field shows a clear translation, \n it is likely that you need to increase lambda.");
		gdl.addNumericField("lambda ", lambda, 6);
		gdl.showDialog();	
		if (gdl.wasCanceled()) {
		  break;
		}
		lambda = gdl.getNextNumber();
		nlt = distortionCorrection(h1, h2, tp, dimension, lambda, imageWidth, imageHeight);
                nlt.visualizeSmall(lambda);
								
		}
		nlt.save(source_dir + saveFileName);
	    }
	}
	//after all preprocessing is done, estimate the distortion correction transform


	if (saveOrLoad == 1){
	    nlt.load(source_dir + saveFileName);
	    nlt.print();
	}
	if (applyCorrection || visualizeResults){
	    target_dir = correctImages();
	}
		
	if (visualizeResults){
	    IJ.log("call nlt.visualize()");
	    nlt.visualize();
	    IJ.log("call evaluateCorrection(inliers)");
	    evaluateCorrection(inliers);
	}
		
	//System.out.println("FINISHED");
    }
	
    public void evaluateCorrection(Vector<Vector<PointMatch>> inliers){
	IJ.showStatus("Evaluating Distortion Correction");
	double[][] original = new double[numberOfImages][2];
	double[][] corrected = new double[numberOfImages][2];
	
	
 	for (int i=firstImageIndex; i < numberOfImages; i++){
	    original[i] = evaluateCorrectionXcorr(i, source_dir);
	    corrected[i] = evaluateCorrectionXcorr(i, target_dir);
	}
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        DefaultCategoryDataset datasetGain = new DefaultCategoryDataset();
        DefaultCategoryDataset datasetGrad = new DefaultCategoryDataset();

        for (int i=0; i < (original.length); i++){
						dataset.setValue(Math.abs(original[i][0]), "before", "image" + i);
						dataset.setValue(Math.abs(corrected[i][0]), "after", "image" + i);
						datasetGrad.setValue(Math.abs(original[i][1]), "before", "image" + i);
						datasetGrad.setValue(Math.abs(corrected[i][1]), "after", "image" + i);
						
						datasetGain.setValue(Math.abs(corrected[i][0]) - Math.abs(original[i][0]), "gray","image" + i);
						datasetGain.setValue(Math.abs(corrected[i][1]) - Math.abs(original[i][1]), "grad","image" + i);
        }
        
  
        JFreeChart chart = ChartFactory.createBarChart("Xcorr before and after correction",
						       "ImageNumber", "Xcorr", dataset, PlotOrientation.VERTICAL, false,
						       true, false);
        ImagePlus imp = new ImagePlus("Plot", chart.createBufferedImage(500, 300));
        imp.show();
		
        JFreeChart chartGrad = ChartFactory.createBarChart("XcorrGradient before and after correction",
							   "ImageNumber", "Xcorr", datasetGrad, PlotOrientation.VERTICAL, false,
							   true, false);
        ImagePlus impGrad = new ImagePlus("Plot", chartGrad.createBufferedImage(500, 300));
        impGrad.show();
        
        JFreeChart chartGain = ChartFactory.createBarChart("Gain in Xcorr",
							   "ImageNumber", "Xcorr", datasetGain, PlotOrientation.VERTICAL, false,
							   true, false);
        ImagePlus impGain = new ImagePlus("Plot", chartGain.createBufferedImage(500, 300));
        impGain.show();
        
	visualizePoints(inliers);
    

	//write xcorr data to file
	String original0 = "", original1 = "", corrected0 = "", corrected1 = "", gain0 = "", gain1 = "";
	for (int i=0; i < (original.length); i++){
			original0 =  original0 + Double.toString(original[i][0]) + "; ";
			original1 =  original1 + Double.toString(original[i][1]) + "; ";
			corrected0 =  corrected0 + Double.toString(corrected[i][0]) + "; ";
			corrected1 =  corrected1 + Double.toString(corrected[i][1]) + "; ";
			gain0 = gain0 + Double.toString(Math.abs(corrected[i][0]) - Math.abs(original[i][0])) + "; ";
			gain1 = gain1 + Double.toString(Math.abs(corrected[i][1]) - Math.abs(original[i][1])) + "; ";
	}

	try{
			BufferedWriter out = new BufferedWriter(new FileWriter(source_dir + "xcorrData.log"));
					
			out.write(original0);
			out.newLine();
			out.newLine();
			out.write(original1);
			out.newLine();
			out.newLine();
			out.write(corrected0);
			out.newLine();
			out.newLine();
			out.write(corrected1);
			out.newLine();
			out.newLine();
			out.write(gain0);
			out.newLine();
			out.newLine();
			out.write(gain1);
			out.newLine();
			out.close();
	}
	catch (Exception e){System.err.println("Error: " + e.getMessage());};
		}
	
	
    void extractSIFTPoints(int index, Vector<Feature>[]  siftFeatures, 
			   Vector<Vector<PointMatch>> inliers, 
			   Vector<RigidModel2D> models){
		
	//save all matching candidates
	Vector<Vector<PointMatch>> candidates = new Vector<Vector<PointMatch>>();

	for (int j=0; j<siftFeatures.length; j++){
	    if (index == j)
		continue;
	    //candidates.add(FloatArray2DSIFT.createMatches(siftFeatures.get(j), siftFeatures.get(index), 1.5f, null, Float.MAX_VALUE, 0.5f));
	    candidates.add(FloatArray2DSIFT.createMatches(siftFeatures[index], siftFeatures[j], 1.5f, null, Float.MAX_VALUE, 0.5f));
	}

		
	//get rid of the outliers and save the rigid transformations to match the inliers
		
	for (int i=0; i < candidates.size(); i++){
			
	    Vector<PointMatch> tmpInliers = new Vector<PointMatch>();
	    //			RigidModel2D m = RigidModel2D.estimateBestModel(candidates.get(i), tmpInliers, sp.min_epsilon, sp.max_epsilon, sp.min_inlier_ratio);
	    RigidModel2D m = null;
	    try
		{
		    m = Model.filterRansac(
					   RigidModel2D.class,
					   candidates.get( i ),
					   tmpInliers,
					   1000,
					   sp.max_epsilon,
					   sp.min_inlier_ratio );
		}
	    catch ( Exception e )
		{
		    //IJ.error( e.getMessage() );
		}
			
	    inliers.add(tmpInliers);
	    models.add(m);

	}
	
    }
	
    NonLinearTransform distortionCorrection(double hack1[][], double hack2[][], double transformParams[][], int dimension, double lambda, int w, int h){
	IJ.showStatus("Getting the Distortion Field");
	NonLinearTransform nlt = new NonLinearTransform(dimension, w, h);
		
	double expandedX[][] = nlt.kernelExpandMatrixNormalize(hack1);
	double expandedY[][] = nlt.kernelExpandMatrix(hack2);
		
	int s = expandedX[0].length;
	Matrix S1 = new Matrix(2*s, 2*s);
	Matrix S2 = new Matrix(2*s,1);
		
	for (int i=0; i < expandedX.length; i++){
	    Matrix xk_ij = new Matrix(expandedX[i],1);
	    Matrix xk_ji = new Matrix(expandedY[i],1);
			
	    Matrix yk1a = xk_ij.minus(xk_ji.times(transformParams[i][0]));
	    Matrix yk1b = xk_ij.times(0.0).minus(xk_ji.times(-transformParams[i][2]));
	    Matrix yk2a = xk_ij.times(0.0).minus(xk_ji.times(-transformParams[i][1]));
	    Matrix yk2b = xk_ij.minus(xk_ji.times(transformParams[i][3]));
						
	    Matrix y = new Matrix(2,2*s);
	    y.setMatrix(0, 0, 0, s-1, yk1a);
	    y.setMatrix(0, 0, s, 2*s-1, yk1b);
	    y.setMatrix(1, 1, 0, s-1, yk2a);
	    y.setMatrix(1, 1, s, 2*s-1, yk2b);
			
	    Matrix xk = new Matrix(2,2*expandedX[0].length);
	    xk.setMatrix(0, 0, 0, s-1, xk_ij);
	    xk.setMatrix(1, 1, s, 2*s-1, xk_ij);
			
	    double[] vals = {hack1[i][0], hack1[i][1]};
	    Matrix c = new Matrix(vals, 2);

	    Matrix X = xk.transpose().times(xk).times(lambda);
	    Matrix Y = y.transpose().times(y);
						
	    S1 = S1.plus(Y.plus(X));
			
	    double trans1 = (transformParams[i][2]* transformParams[i][5] - transformParams[i][0]*transformParams[i][4]);
	    double trans2 = (transformParams[i][1]* transformParams[i][4] - transformParams[i][3]*transformParams[i][5]);
	    double[] trans = {trans1, trans2};

			
	    Matrix translation = new Matrix(trans, 2);
	    Matrix YT = y.transpose().times(translation);
	    Matrix XC = xk.transpose().times(c).times(lambda);
			
	    S2 = S2.plus(YT.plus(XC));
	}
		

	Matrix regularize = Matrix.identity(S1.getRowDimension(), S1.getColumnDimension());
	Matrix beta = new Matrix(S1.plus(regularize.times(0.001)).inverse().times(S2).getColumnPackedCopy(),s);
		
	nlt.setBeta(beta.getArray());
	nlt.inverseTransform(hack1);
	return nlt;
    }
	
    String correctImages(){
	if (!applyCorrection){
			
	    target_dir = System.getProperty("user.dir").replace('\\', '/') + "/distCorr_tmp/";
	    System.out.println("Tmp target directory: " + target_dir);
			
	    if (new File(target_dir).exists()){
		System.out.println("removing old tmp directory!");

		String[] filesToDelete = new File(target_dir).list();
		for (int i=0; i < filesToDelete.length; i++){
		    System.out.println(filesToDelete[i]);
		    boolean deleted = new File(target_dir + filesToDelete[i]).delete();
		    if (! deleted)
			IJ.log("Error: Could not remove temporary directory!");
		}
		new File(target_dir).delete();
	    }			
	    try{
		// Create one directory
		boolean success = (new File(target_dir)).mkdir();
		if (success)
		    new File(target_dir).deleteOnExit();
		    	 
	    }
	    catch(Exception e){
		IJ.showMessage("Error! Could not create temporary directory. " + e.getMessage());
	    }
	}
	if (target_dir == ""){
	    DirectoryChooser dc = new DirectoryChooser("Target Directory");
	    target_dir = dc.getDirectory();
	    if (null == target_dir) return "";
	    target_dir = target_dir.replace('\\', '/');
	    if (!target_dir.endsWith("/")) target_dir += "/";
	}
		
		
	final String[] namesTarget = new File(target_dir).list(new FilenameFilter() {
		public boolean accept(File dir, String namesTarget) {
		    int idot = namesTarget.lastIndexOf('.');
		    if (-1 == idot) return false;
		    return namesTarget.contains(namesTarget.substring(idot).toLowerCase());
		}
	    });	
		
	if (namesTarget.length > 0){
	    IJ.showMessage("Overwrite Message", "There  are already images in that directory. These will be used for evaluation.");
	}
	else{
			
	    IJ.showStatus("Correcting Images");

	    final Thread[] threads = MultiThreading.newThreads();
	    final AtomicInteger ai = new AtomicInteger(applyCorrection ? 0 : firstImageIndex);
			
			
	    for (int ithread = 0; ithread < threads.length; ++ithread) {
		threads[ithread] = new Thread() {
			public void run() { 
			    setPriority(Thread.NORM_PRIORITY);

			    for (int i = ai.getAndIncrement(); i < (applyCorrection? names.length : (firstImageIndex + numberOfImages)); i = ai.getAndIncrement()) {
				IJ.log("Correcting image " + names[i]);
				ImagePlus imps = new Opener().openImage(source_dir + names[i]);
				imps.setProcessor(imps.getTitle(), imps.getProcessor().convertToShort(false));
				ImageProcessor[] transErg = nlt.transform(imps.getProcessor());
				imps.setProcessor(imps.getTitle(),transErg[0]);
				if (!applyCorrection)
						new File(target_dir + names[i]).deleteOnExit();
				new FileSaver(imps).saveAsTiff(target_dir + names[i]);
			    }
			}
		    };
	    }
	    MultiThreading.startAndJoin(threads);
			
			
	}
	return target_dir;
    }
	
    double[] evaluateCorrectionXcorr(int index, String directory){
	ImagePlus im1 = new Opener().openImage(directory + names[index]);
	im1.setProcessor(names[index], im1.getProcessor().convertToShort(false));
		
	int count = 0;
	ArrayList<Double> xcorrVals = new ArrayList<Double>();
	ArrayList<Double> xcorrValsGrad = new ArrayList<Double>();
		
	for (int i=0; i < numberOfImages; i++){
	    if (i == index){
		continue;
	    }
	    if (models[index*(numberOfImages-1)+count] == null){
		count++;
		continue;
	    }
			
	    ImagePlus newImg = new Opener().openImage(directory +  names[i+firstImageIndex]);
	    newImg.setProcessor(newImg.getTitle(), newImg.getProcessor().convertToShort(false));
			
	    newImg.setProcessor(names[i+firstImageIndex], applyTransformToImageInverse(models[index*(numberOfImages-1)+count], 
										       newImg.getProcessor()));
	    ImageProcessor testIp = im1.getProcessor().duplicate();

	    // If you want to see the stitching improvement run this			
	    //			for ( int x=0; x < testIp.getWidth(); x++){
	    //				for (int y=0; y < testIp.getHeight(); y++){
	    //					testIp.set(x, y, Math.abs(im1.getProcessor().get(x,y) - newImg.getProcessor().get(x,y)));
	    //				}
	    //			}
			
	    //			ImagePlus testImg = new ImagePlus(names[index] + " minus " + names[i], testIp);
	    //			testImg.show();
	    //			im1.show();
	    //			newImg.show();
			
			
	    xcorrVals.add(getXcorrBlackOut(im1.getProcessor(), newImg.getProcessor()));
			
	    xcorrValsGrad.add(getXcorrBlackOutGradient(im1.getProcessor(), newImg.getProcessor()));
	    count++;
	}
		
	Collections.sort(xcorrVals);
	Collections.sort(xcorrValsGrad);
		
	double[] medians = {xcorrVals.get(xcorrVals.size() / 2), xcorrValsGrad.get(xcorrValsGrad.size()/2)};
		
	double m1 = 0.0, m2 = 0.0;
	for (int i=0; i < xcorrVals.size(); i++){
	    m1 += xcorrVals.get(i);
	    m2 += xcorrValsGrad.get(i);
	}
		
	m1 /= xcorrVals.size();
	m2 /= xcorrVals.size();
		
	double[] means = {m1,m2};
		
	return means;
	//return medians;
    }
	
    ImageProcessor applyTransformToImageInverse(RigidModel2D a, ImageProcessor ip){
	ImageProcessor newIp = ip.duplicate();
	newIp.max(0.0);
		
	for (int x=0; x<ip.getWidth(); x++){
	    for (int y=0; y<ip.getHeight(); y++){
		float[] position = {(float)x,(float)y};
		//				float[] newPosition = a.apply(position);
		float[] newPosition = {0,0,};
		try{
		    newPosition = a.applyInverse(position);
		}
		catch(NoninvertibleModelException e){
		    System.out.println("NoninvertibleModelException");
		}
		int xn = (int) newPosition[0];
		int yn = (int) newPosition[1];
				
		if ( (xn >= 0) && (yn >= 0) && (xn < ip.getWidth()) && (yn < ip.getHeight()))
		    newIp.set(xn,yn,ip.get(x,y));	
				
	    }
	}
	return newIp;
    }
	
    double getXcorrBlackOutGradient(ImageProcessor ip1, ImageProcessor ip2){
	ImageProcessor ip1g = getGradientSobel(ip1);
	ImageProcessor ip2g = getGradientSobel(ip2);
		
	return getXcorrBlackOut(ip1g, ip2g);
    }
	
    //this blends out gradients that include black pixels to make the sharp border caused 
    //by the nonlinear transformation not disturb the gradient comparison
    //FIXME: this should be handled by a mask image!
    ImageProcessor getGradientSobel(ImageProcessor ip){
	ImageProcessor ipGrad = ip.duplicate();
	ipGrad.max(0.0);
		
	for (int i=1; i<ipGrad.getWidth()-1; i++){
	    for (int j=1; j<ipGrad.getHeight()-1; j++){
		if(ip.get(i-1,j-1)==0 || ip.get(i-1,j)==0 || ip.get(i-1,j+1)==0 || 
		   ip.get(i,j-1)==0 || ip.get(i,j)==0 || ip.get(i,j+1)==0 ||
		   ip.get(i+1,j-1)==0 || ip.get(i+1,j)==0 || ip.get(i+1,j+1)==0 ) 
		    continue;
				   
		double gradX = (double) -ip.get(i-1, j-1) - 2* ip.get(i-1,j) - ip.get(i-1,j+1)
		    +ip.get(i+1, j-1) + 2* ip.get(i+1,j) + ip.get(i+1,j+1);
				
		double gradY = (double) -ip.get(i-1, j-1) - 2* ip.get(i,j-1) - ip.get(i+1,j-1)
		    +ip.get(i-1, j+1) + 2* ip.get(i,j+1) + ip.get(i+1,j+1);
				
		double mag = Math.sqrt(gradX*gradX + gradY*gradY);
		ipGrad.setf(i,j,(float) mag);
	    }
	}
	return ipGrad;
    }
	
	
    //tested the result against matlab routine, this worked fine
    static	double getXcorrBlackOut(ImageProcessor ip1, ImageProcessor ip2){		
	
	ip1 = ip1.convertToFloat();
	ip2 = ip2.convertToFloat();
	
	//If this is not done, the black area from the transformed image influences xcorr result
	//better alternative would be to use mask images and only calculate xcorr of 
	//the region present in both images.
	for (int i=0; i<ip1.getWidth(); i++){
	    for (int j=0; j<ip1.getHeight(); j++){
		if (ip1.get(i,j) == 0)
		    ip2.set(i,j,0);
		if (ip2.get(i,j) == 0)
		    ip1.set(i,j,0);
	    }
	}
	
	//		FloatProcessor ip1f = (FloatProcessor)ip1.convertToFloat();
	//		FloatProcessor ip2f = (FloatProcessor)ip2.convertToFloat();
		
	float[] data1 = ( float[] )ip1.getPixels();
	float[] data2 = ( float[] )ip2.getPixels();
		
	double[] data1b = new double[data1.length];
	double[] data2b = new double[data2.length];
		
	int count = 0;
	double mean1 = 0.0, mean2 = 0.0;
		
	for (int i=0; i < data1.length; i++){
	    //if ((data1[i] == 0) || (data2[i] == 0))
	    //continue;
	    data1b[i] = data1[i];
	    data2b[i] = data2[i];
	    mean1 += data1b[i];
	    mean2 += data2b[i];
	    count++;
	}
		
	mean1 /= (double) count;
	mean2 /= (double) count;
		
	double L2_1 = 0.0, L2_2 = 0.0;
	for (int i=0; i < count; i++){
	    L2_1 += (data1b[i] - mean1) * (data1b[i] - mean1);
	    L2_2 += (data2b[i] - mean2) * (data2b[i] - mean2);
	}
		
	L2_1 = Math.sqrt(L2_1);
	L2_2 = Math.sqrt(L2_2);
		
	double xcorr = 0.0;
	for (int i=0; i < count; i++){
	    xcorr += ((data1b[i]-mean1) / L2_1) * ((data2b[i]-mean2) / L2_2);
	}
		
	//System.out.println("XcorrVal: " + xcorr);
	return xcorr;
    }

	
    void visualizePoints(Vector<Vector<PointMatch>> inliers){
	ColorProcessor ip = new ColorProcessor(nlt.getWidth(), nlt.getHeight());
	ip.setColor(Color.red);
		
	ip.setLineWidth(5);
	for (int i=0; i < inliers.size(); i++){
	    for (int j=0; j < inliers.get(i).size(); j++){
		float[] tmp1 = inliers.get(i).get(j).getP1().getW();
		float[] tmp2 = inliers.get(i).get(j).getP2().getL();
		ip.setColor(Color.red);
		ip.drawDot((int) tmp2[0], (int) tmp2[1]); 
		ip.setColor(Color.blue);
		ip.drawDot((int) tmp1[0], (int) tmp1[1]);
	    }
	}
		
	ImagePlus points = new ImagePlus("", ip);
	points.show();
    }
	
    public void getTransform(double[][] points1, double[][] points2, double[][] transformParams){
	double[][] p1 = new double[points1.length][3];
	double[][] p2 = new double[points2.length][3];
		
	for (int i=0; i < points1.length; i++){
	    p1[i][0] = points1[i][0];
	    p1[i][1] = points1[i][1];
	    p1[i][2] = 100.0;
			
	    p2[i][0] = points2[i][0];
	    p2[i][1] = points2[i][1];
	    p2[i][2] = 100.0;
	}
		
		
	Matrix s1 = new Matrix(p1);
	Matrix s2 = new Matrix(p2);
	Matrix t = (s1.transpose().times(s1)).inverse().times(s1.transpose()).times(s2);
	t = t.inverse();
	for (int i=0; i < transformParams.length; i++){
	    if (transformParams[i][0] == -10){
		transformParams[i][0] = t.get(0,0);
		transformParams[i][1] = t.get(0,1);
		transformParams[i][2] = t.get(1,0);
		transformParams[i][3] = t.get(1,1);
		transformParams[i][4] = t.get(2,0);
		transformParams[i][5] = t.get(2,1);
	    }
	}
		
	t.print(1, 1);
		
		
    }
	
	
    Vector<Feature>[] extractSIFTFeaturesThreaded(final int numberOfImages, final String directory, 
						  final String[] names ){
	//extract all SIFT Features
		
	final Vector[] siftFeatures = new Vector[numberOfImages];
	final Thread[] threads = MultiThreading.newThreads();
	final AtomicInteger ai = new AtomicInteger(0); // start at second slice
		
	IJ.showStatus("Extracting SIFT Features");
	for (int ithread = 0; ithread < threads.length; ++ithread) {
	    threads[ithread] = new Thread() {
		    public void run() {
			for (int i = ai.getAndIncrement(); i < numberOfImages; i = ai.getAndIncrement()) {
			    Vector< Feature > fs;
			    ImagePlus imps = new Opener().openImage(directory + names[i + firstImageIndex]);
			    imps.setProcessor(imps.getTitle(), imps.getProcessor().convertToFloat());
						
			    FloatArray2DSIFT sift = new FloatArray2DSIFT( sp.fdsize, sp.fdbins );
			    FloatArray2D fa = ImageArrayConverter.ImageToFloatArray2D( imps.getProcessor() );
			    Filter.enhance( fa, 1.0f );
						
			    float[] initial_kernel;
			    initial_kernel = Filter.createGaussianKernel(
									 ( float )Math.sqrt( sp.initial_sigma * sp.initial_sigma - 0.25 ), true );
						
			    fa = Filter.convolveSeparable( fa, initial_kernel, initial_kernel );
			    sift.init( fa, sp.steps, sp.initial_sigma, sp.min_size / 4, sp.max_size );
						
			    fs = sift.run( sp.max_size );
			    Collections.sort( fs );
			    IJ.log("Extracting SIFT of image: "+i);
					
			    siftFeatures[i]=fs;	

			}
		    }
		};
	}
	MultiThreading.startAndJoin(threads);
			
	return  siftFeatures;
    }
	
    void extractSIFTPointsThreaded(final int index, final Vector<Feature>[]  siftFeatures, 
				   final Vector<PointMatch>[] inliers){

	//save all matching candidates
	final Vector[] candidates = new Vector[siftFeatures.length-1];
		
	final Thread[] threads = MultiThreading.newThreads();
	final AtomicInteger ai = new AtomicInteger(0); // start at second slice
		
	for (int ithread = 0; ithread < threads.length; ++ithread) {
	    threads[ithread] = new Thread() {
		    public void run() { 
			setPriority(Thread.NORM_PRIORITY);

			for (int j = ai.getAndIncrement(); j < candidates.length; j = ai.getAndIncrement()) {
			    int i = (j<index ? j : j+1);
			    candidates[j] = FloatArray2DSIFT.createMatches(siftFeatures[index], siftFeatures[i], 1.5f, null, Float.MAX_VALUE, 0.5f);
			}
		    }
		};
	}
	
	MultiThreading.startAndJoin(threads);	

	//	get rid of the outliers and save the rigid transformations to match the inliers
		
	final AtomicInteger ai2 = new AtomicInteger(0);
	for (int ithread = 0; ithread < threads.length; ++ithread) {
	    threads[ithread] = new Thread() {
		    public void run() { 
			setPriority(Thread.NORM_PRIORITY);
			for (int i=ai2.getAndIncrement(); i < candidates.length; i = ai2.getAndIncrement()){
	
			    Vector<PointMatch> tmpInliers = new Vector<PointMatch>();
			    //RigidModel2D m = RigidModel2D.estimateBestModel(candidates.get(i), tmpInliers, sp.min_epsilon, sp.max_epsilon, sp.min_inlier_ratio);
			    RigidModel2D m = null;
			    try
				{
				    m = Model.filterRansac(
							   RigidModel2D.class,
							   candidates[i],
							   tmpInliers,
							   1000,
							   sp.max_epsilon,
							   sp.min_inlier_ratio );
				}
			    catch ( Exception e )
				{
				    //IJ.error( e.getMessage() );
				}
			    inliers[index*(numberOfImages-1)+i] = tmpInliers;
			    models[index*(numberOfImages-1)+i] = m;
			    //System.out.println("**** MODEL ADDED: " + (index*(numberOfImages-1)+i));
			}

		    }
		};
	}
	MultiThreading.startAndJoin(threads);	

    }
}
	
