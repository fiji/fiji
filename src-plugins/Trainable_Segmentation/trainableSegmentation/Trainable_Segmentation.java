package trainableSegmentation;
/* This is a small Plugin that should perform better in segmentation than thresholding
 * The idea is to train a random forest classifier on given manual labels
 * and then classify the whole image 
 * I try to keep parameters hidden from the user to make usage of the plugin
 * intuitive and easy. I decided that it is better to need more manual annotations
 * for training and do feature selection instead of having the user manually tune 
 * all filters.
 * 
 * ToDos:
 * - work on whole Stack 
 * - delete annotations with a shortkey
 * - save classifier and load classifier
 * - apply classifier to other images
 * - put thread solution to wiki http://pacific.mpi-cbg.de/wiki/index.php/Developing_Fiji#Writing_plugins
 * 
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Author: Verena Kaynig (verena.kaynig@inf.ethz.ch)
 */


import ij.IJ;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ColorProcessor;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.ImagePlus;
import ij.WindowManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import hr.irb.fastRandomForest.FastRandomForest;

public class Trainable_Segmentation implements PlugIn {
	
	private List<Roi> positiveExamples = new ArrayList< Roi>(); 
	private List<Roi> negativeExamples = new ArrayList< Roi>();
	private ImagePlus trainingImage;
	private ImagePlus displayImage;
	private ImagePlus classifiedImage;
	private ImagePlus overlayImage;
	private FeatureStack featureStack;
	private CustomWindow win;
	private java.awt.List posExampleList;
	private java.awt.List negExampleList;
	private int posTraceCounter;
	private int negTraceCounter;
   	private boolean showColorOverlay;
   	private Instances wholeImageData;
	final Button posExampleButton;
  	final Button negExampleButton;
  	final Button trainButton;
  	final Button overlayButton;
  	final Button resultButton;
  	
  	
  	public Trainable_Segmentation() {
	    	posExampleButton = new Button("positiveExample");
  	      	negExampleButton = new Button("negativeExample");
  	      	trainButton = new Button("train Classifier");
  	      	overlayButton = new Button("toggle Overlay");
  	      	overlayButton.setEnabled(false);
  	      	resultButton = new Button("create result");
  	      	resultButton.setEnabled(false);
  	      	posExampleList = new java.awt.List(5);
  	      	posExampleList.setForeground(Color.green);
  	      	negExampleList = new java.awt.List(5);
  	      	negExampleList.setForeground(Color.red);
  	      	posTraceCounter = 0;
  	      	negTraceCounter = 0;
  	      	showColorOverlay = false;
	}
	
  	ExecutorService exec = Executors.newFixedThreadPool(1);
  	
  	private ActionListener listener = new ActionListener() {
  		public void actionPerformed(final ActionEvent e) {
  			exec.submit(new Runnable() {
  				public void run() {
  					if(e.getSource() == posExampleButton){
  		  				addPositiveExamples();
  		  			}
  		  			else if(e.getSource() == negExampleButton){
  		  				addNegativeExamples();
  		  			}
  		  			else if(e.getSource() == trainButton){
  		  				trainClassifier();
  		  			}
  		  			else if(e.getSource() == overlayButton){
  		  				toggleOverlay();
  		  			}
  		  			else if(e.getSource() == resultButton){
  						showClassificationImage();
  					}
  		  			else if(e.getSource() == posExampleList || e.getSource() == negExampleList){
  		  				deleteSelected(e);
  		  			}
  				}
  			});
  		}
  	};
  	
	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(final ItemEvent e) {
			exec.submit(new Runnable() {
				public void run() {
  		  			if(e.getSource() == posExampleList || e.getSource() == negExampleList){
  		  				IJ.log("exampleList clicked");
  		  				listSelected(e);
  		  			}
				}
			});
		}
	};
 
  	
  	private class CustomWindow extends ImageWindow {
  		CustomWindow(ImagePlus imp) {
  			super(imp);
  		
  			Panel piw = new Panel();
  			piw.setLayout(super.getLayout());
  			setTitle("Playground");
  			for (Component c : getComponents()) {
  				piw.add(c);
  			}
  			
 	      	Panel annotations = new Panel();
 	      	BoxLayout boxAnnotation = new BoxLayout(annotations, BoxLayout.Y_AXIS);
 	      	annotations.setLayout(boxAnnotation);
  	      	posExampleList.addActionListener(listener);
  	      	negExampleList.addActionListener(listener);	
  	      	posExampleList.addItemListener(itemListener);
  	      	negExampleList.addItemListener(itemListener);
  	      	annotations.add(posExampleList);
  	      	annotations.add(negExampleList);
  			
	      	Panel imageAndLists = new Panel();
			BoxLayout boxImgList = new BoxLayout(imageAndLists, BoxLayout.X_AXIS);
			imageAndLists.setLayout(boxImgList);
  	      	imageAndLists.add(piw);
  	      	imageAndLists.add(annotations);
  	      	
  			Panel buttons = new Panel();
  			buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
  			
  	      	posExampleButton.addActionListener(listener);
  	      	negExampleButton.addActionListener(listener);
  	      	trainButton.addActionListener(listener);
  	      	overlayButton.addActionListener(listener);
  	      	resultButton.addActionListener(listener);
  	      	buttons.add(posExampleButton);
  	      	buttons.add(negExampleButton);
  	      	buttons.add(trainButton);
  	      	buttons.add(overlayButton);
  	      	buttons.add(resultButton);
  	      	
  	      	for (Component c : new Component[]{posExampleButton, negExampleButton, trainButton, overlayButton, resultButton}) {
  	      		c.setMaximumSize(new Dimension(230, 50));
  	      		c.setPreferredSize(new Dimension(130, 30));
  	      	}
  	      	
  	      	Panel all = new Panel();
			BoxLayout box = new BoxLayout(all, BoxLayout.Y_AXIS);
			all.setLayout(box);
  	      	all.add(imageAndLists);
  	      	all.add(buttons);
  	      	removeAll();
  	      	add(all);
  	      	
  	      	pack();
  	      	
  	      	// Propagate all listeners
  	      	for (Panel p : new Panel[]{all, buttons, piw}) {
  	      		for (KeyListener kl : getKeyListeners()) {
  	      			p.addKeyListener(kl);
  	      		}
  	      	}
  	      	
  	      	addWindowListener(new WindowAdapter() {
  	      		public void windowClosing(WindowEvent e) {
  	      			// cleanup
  	      			exec.shutdownNow();
  	      		}
  	      	});
  		}
  		public void setDisplayImage(ImagePlus newDisplay){
  			
  		}
  	}
  	
	public void run(String arg) {
//		trainingImage = IJ.openImage("testImages/i00000-1.tif");
		//get current image
		if (null == WindowManager.getCurrentImage()) {
			trainingImage = IJ.openImage();
			if (null == trainingImage) return; // user canceled open dialog
		}
		else {
			trainingImage = new ImagePlus("training Image",WindowManager.getCurrentImage().getProcessor().duplicate());
		}
		
		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
													 "is larger than 1024 pixels. \n" +
													 "Feature stack creation and classifier training \n" +
													 "might take some time depending on your computer.\n" +
													 "Proceed?"))
				return;

		
		trainingImage.setProcessor("training image", trainingImage.getProcessor().duplicate().convertToByte(true));
		createFeatureStack(trainingImage);
		
		IJ.log("reading whole image data");
		long start = System.currentTimeMillis();
		wholeImageData = featureStack.createInstances();
		long end = System.currentTimeMillis();
		IJ.log("creating whole image data took: " + (end-start));
		wholeImageData.setClassIndex(wholeImageData.numAttributes() - 1);
		 
		displayImage = new ImagePlus();
		displayImage.setProcessor("training image", trainingImage.getProcessor().duplicate().convertToRGB());
		
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.FREELINE);
		
		//Build GUI
		win = new CustomWindow(displayImage);
		
		//trainingImage.getWindow().setVisible(false);
		}
	
	private void addPositiveExamples(){
		//get selected pixels
		Roi r = displayImage.getRoi();
		if (null == r){
			return;
		}
		displayImage.killRoi();
		positiveExamples.add(r);
		posExampleList.add("trace " + posTraceCounter); posTraceCounter++;
		drawExamples();
	}
	
	private void addNegativeExamples(){
		//get selected pixels
		Roi r = displayImage.getRoi();
		if (null == r){
			return;
		}
		displayImage.killRoi();
		negativeExamples.add(r);
		negExampleList.add("trace " + negTraceCounter); negTraceCounter++;
		drawExamples();
	}
	
	private void drawExamples(){
		if (!showColorOverlay)
			displayImage.setProcessor("Playground", trainingImage.getProcessor().convertToRGB());
		else
			displayImage.setProcessor("Playground", overlayImage.getProcessor().convertToRGB());
		
		displayImage.setColor(Color.GREEN);
		for (Roi r : positiveExamples){
			r.drawPixels(displayImage.getProcessor());
		}
		
		displayImage.setColor(Color.RED);
		for (Roi r : negativeExamples){
			r.drawPixels(displayImage.getProcessor());
		}
		
		displayImage.updateAndDraw();
	}
	
	public void createFeatureStack(ImagePlus img){
		IJ.log("creating feature stack");
		featureStack = new FeatureStack(img);
		int counter = 1;
		for (float i=2.0f; i<featureStack.getWidth()/5.0f; i*=2){
			IJ.showStatus("creating feature stack   " + counter);
			featureStack.addGaussianBlur(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			featureStack.addGradient(i); counter++;
			IJ.showStatus("creating feature stack   " + counter);			
			featureStack.addHessian(i); counter++;
			for (float j=2.0f; j<i; j*=2){
				IJ.showStatus("creating feature stack   " + counter);				
				featureStack.addDoG(i, j); counter++;
			}
		}
		featureStack.addMembraneFeatures(19, 1);
	}
	
	
	public void writeDataToARFF(Instances data, String filename){
		try{
			BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(
				new FileOutputStream( filename) ) );
			try{	
					out.write(data.toString());
					
			}
			catch(IOException e){IJ.showMessage("IOException");}
	}
	catch(FileNotFoundException e){IJ.showMessage("File not found!");}
		
	}

	public Instances createTrainingInstances(){
		FastVector attributes = new FastVector();
		for (int i=1; i<=featureStack.getSize(); i++){
			String attString = featureStack.getSliceLabel(i) + " numeric";
			attributes.addElement(new Attribute(attString));
		}
		FastVector classes = new FastVector();
		classes.addElement("foreground");
		classes.addElement("background");
		attributes.addElement(new Attribute("class", classes));
		
		Instances trainingData =  new Instances("segment", attributes, positiveExamples.size()+negativeExamples.size());
		
		for(int j=0; j<positiveExamples.size(); j++){
			Roi r = positiveExamples.get(j);
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			int n = r.getPolygon().npoints;
			
			for (int i=0; i<n; i++){
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++){
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
				}
				values[featureStack.getSize()] = 1.0;
				trainingData.add(new Instance(1.0, values));
			}
		}
		
		for(int j=0; j<negativeExamples.size(); j++){
			Roi r = negativeExamples.get(j);
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			int n = r.getPolygon().npoints;
			
			for (int i=0; i<n; i++){
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++){
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
				}
				values[featureStack.getSize()] = 0.0;
				trainingData.add(new Instance(1.0, values));
			}
		}
		return trainingData;
	}
	
	public void trainClassifier(){
		if (positiveExamples.size()==0){
			IJ.showMessage("Cannot train without positive examples!");
			return;
		}
		if (negativeExamples.size()==0){
			IJ.showMessage("Cannot train without negative examples!");
			return;
		}
		
		 IJ.showStatus("training classifier");
		 long start = System.currentTimeMillis();
		 Instances data = createTrainingInstances();
		 long end = System.currentTimeMillis();
		 IJ.log("creating training data took: " + (end-start));
		 data.setClassIndex(data.numAttributes() - 1);
		 
		 FastRandomForest rf = new FastRandomForest();
		 //FIXME: should depend on image size?? Or labels??
		 rf.setNumTrees(200);
		 //this is the default that Breiman suggests
		 //rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		 //but this seems to work better
		 rf.setNumFeatures(2);
		
		 
		 rf.setSeed(123);
		 
		 IJ.log("training classifier");
		 try{rf.buildClassifier(data);}
		 catch(Exception e){IJ.showMessage("Could not train Classifier!");}
		 applyClassifier(rf);
	}

	public void applyClassifier(FastRandomForest rf){
		 IJ.log("classifying image");
		 double[] classificationResult = new double[wholeImageData.numInstances()];
		 for (int i=0; i<wholeImageData.numInstances(); i++){
			 try{
			 classificationResult[i] = rf.classifyInstance(wholeImageData.instance(i));
			 }catch(Exception e){IJ.showMessage("Could not apply Classifier!");}
		 }
		 
		 IJ.log("showing result");
		 ImageProcessor classifiedImageProcessor = new FloatProcessor(trainingImage.getWidth(), trainingImage.getHeight(), classificationResult);
		 classifiedImageProcessor.convertToByte(true);
		 classifiedImage = new ImagePlus("classification result", classifiedImageProcessor);
		 overlayButton.setEnabled(true);
		 resultButton.setEnabled(true);
		 showColorOverlay = false;
		 toggleOverlay();
	}
	
	void toggleOverlay(){
		showColorOverlay = !showColorOverlay;
		if (showColorOverlay){
			//do this every time cause most likely classification changed
			int width = trainingImage.getWidth();
			int height = trainingImage.getHeight();
		
			ImageProcessor white = new ByteProcessor(width, height);
			white.setMinAndMax(255, 255);
			ImageProcessor black = new ByteProcessor(width, height);
			
			ImageStack redStack = new ImageStack(width, height);
			redStack.addSlice("red", trainingImage.getProcessor().duplicate());
			ImageStack greenStack = new ImageStack(width, height);
			greenStack.addSlice("green", classifiedImage.getProcessor().duplicate());
			ImageStack blueStack = new ImageStack(width, height);
			blueStack.addSlice("blue", white.duplicate());
		
			RGBStackMerge merger = new RGBStackMerge();
			ImageStack overlayStack = merger.mergeStacks(trainingImage.getWidth(), trainingImage.getHeight(), 
					   1, redStack, greenStack, blueStack, true);
	
			overlayImage = new ImagePlus("overlay image", overlayStack);
		}

		drawExamples();
	}
	
	void listSelected(final ItemEvent e){
		drawExamples();
		displayImage.setColor(Color.YELLOW);
			
		if (e.getSource() == posExampleList) {
			negExampleList.deselect(negExampleList.getSelectedIndex());
			positiveExamples.get(posExampleList.getSelectedIndex()).drawPixels(displayImage.getProcessor());
		}
		else{
			posExampleList.deselect(posExampleList.getSelectedIndex());
			negativeExamples.get(negExampleList.getSelectedIndex()).drawPixels(displayImage.getProcessor());
		}
		displayImage.updateAndDraw();
	}
	
	void deleteSelected(final ActionEvent e){
		if (e.getSource() == posExampleList) {
			//delete item from ROI
			int index = posExampleList.getSelectedIndex();
			positiveExamples.remove(index);
			//delete item from list
			posExampleList.remove(index);
		}
		else{
			//delete item from ROI
			int index = negExampleList.getSelectedIndex();
			negativeExamples.remove(index);
			//delete item from list
			negExampleList.remove(index);
		}
		
		if (!showColorOverlay)
			drawExamples();
		else{
			//FIXME I have no clue why drawExamples 
			//does not do the trick if overlay is displayed
			toggleOverlay();
			toggleOverlay();
		}
	}
	
	void showClassificationImage(){
		ImagePlus resultImage = new ImagePlus("classification result", classifiedImage.getProcessor().duplicate());
		resultImage.show();
	}
}
