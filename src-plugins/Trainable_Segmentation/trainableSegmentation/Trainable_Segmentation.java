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
 * - work with color features
 * - work on whole Stack 
 * - delete annotations with a shortkey
 * - change training image
 * - do probability output (accessible?) and define threshold
 * - put thread solution to wiki http://pacific.mpi-cbg.de/wiki/index.php/Developing_Fiji#Writing_plugins
 * 
 * - clean up gui (buttons, window size, funny zoom)
 * - give more feedback when classifier is trained or applied
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

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.ImagePlus;
import ij.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;


import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import hr.irb.fastRandomForest.FastRandomForest;

public class Trainable_Segmentation implements PlugIn {


	private final int MAX_NUM_CLASSES = 5;

	private List<Roi> [] examples = new ArrayList[MAX_NUM_CLASSES]; 
	private ImagePlus trainingImage;
	private ImagePlus displayImage;
	private ImagePlus classifiedImage;
	private ImagePlus overlayImage;
	private FeatureStack featureStack;
	private CustomWindow win;

	private int traceCounter[] = new int[MAX_NUM_CLASSES];
	private boolean showColorOverlay;
	private Instances wholeImageData;
	private Instances loadedTrainingData;
	private FastRandomForest rf;
	final JButton addExampleButton;
	final JButton trainButton;
	final JButton overlayButton;
	final JButton resultButton;
	final JButton applyButton;
	final JButton loadDataButton;
	final JButton saveDataButton;

	final JButton addClassButton;

	final Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
			Color.orange, Color.pink};

	String[] classLabels = new String[]{"background", "foreground", "class-3", "class-4", "class-5"};

	private int numOfClasses = 2;
	private java.awt.List exampleList[] = new java.awt.List[MAX_NUM_CLASSES];
	private JRadioButton [] classButton = new JRadioButton[MAX_NUM_CLASSES];
	//Group the radio buttons.
	ButtonGroup group = new ButtonGroup();


	public Trainable_Segmentation() 
	{
		addExampleButton = new JButton("+");
		trainButton = new JButton("Train classifier");
		overlayButton = new JButton("Toggle overlay");
		overlayButton.setEnabled(false);
		resultButton = new JButton("Create result");
		resultButton.setEnabled(false);
		applyButton = new JButton ("Apply classifier");
		applyButton.setEnabled(false);
		loadDataButton = new JButton ("Load data");
		saveDataButton = new JButton ("Save data");

		addClassButton = new JButton ("Add class");


		for(int i = 0; i < numOfClasses ; i++)
		{
			examples[i] = new ArrayList<Roi>();
			exampleList[i] = new java.awt.List(MAX_NUM_CLASSES);
			exampleList[i].setForeground(colors[i]);
		}

		showColorOverlay = false;

		rf = new FastRandomForest();
		//FIXME: should depend on image size?? Or labels??
				rf.setNumTrees(200);
				//this is the default that Breiman suggests
				//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
				//but this seems to work better
				rf.setNumFeatures(2);


				rf.setSeed(123);
	}

	final ExecutorService exec = Executors.newFixedThreadPool(1);

	private ActionListener listener = new ActionListener() {
		public void actionPerformed(final ActionEvent e) {
			exec.submit(new Runnable() {
				public void run() {

					if(e.getSource() == addExampleButton)
					{
						IJ.log("+ pressed");
						for(int i = 0; i < numOfClasses; i++)
						{
							if(classButton[i].isSelected())
							{
								addExamples(i);
								break;
							}
						}
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
					else if(e.getSource() == applyButton){
						applyClassifierToTestData();
					}
					else if(e.getSource() == loadDataButton){
						loadTrainingData();
					}
					else if(e.getSource() == saveDataButton){
						saveTrainingData();
					}
					else{ 
						for(int i = 0; i < numOfClasses; i++)
							if(e.getSource() == exampleList[i])
							{
								deleteSelected(e);
								break;
							}
					}

				}
			});


		}
	};

	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(final ItemEvent e) {
			exec.submit(new Runnable() {
				public void run() {
					for(int i = 0; i < numOfClasses; i++)
					{
						if(e.getSource() == exampleList[i])
							listSelected(e, i);
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
			setTitle("Trainable Segmentation");
			for (Component c : getComponents()) {
				piw.add(c);
			}

			// Panel with class radio buttons and lists
			Panel annotations = new Panel();
			GridBagLayout boxAnnotation = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.NORTHWEST;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 0;

			annotations.setLayout(boxAnnotation);
			for(int i = 0; i < numOfClasses ; i++)
			{
				exampleList[i].addActionListener(listener);
				classButton[i] = new JRadioButton(classLabels[i]);
				group.add(classButton[i]);

				boxAnnotation.setConstraints(classButton[i], c);
				annotations.add(classButton[i]);
				c.gridy++;

				boxAnnotation.setConstraints(exampleList[i], c);
				annotations.add(exampleList[i]);
				c.gridy++;
			}

			// Select first class
			classButton[0].setSelected(true);

			Panel imageAndLists = new Panel();
			BoxLayout boxImgList = new BoxLayout(imageAndLists, BoxLayout.X_AXIS);
			imageAndLists.setLayout(boxImgList);
			imageAndLists.add(piw);
			imageAndLists.add(annotations);

			Panel buttons = new Panel();
			BoxLayout buttonLayout = new BoxLayout(buttons, BoxLayout.Y_AXIS);
			buttons.setLayout(buttonLayout);
			//buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

			// Add listeners
			addExampleButton.addActionListener(listener);
			trainButton.addActionListener(listener);
			overlayButton.addActionListener(listener);
			resultButton.addActionListener(listener);
			applyButton.addActionListener(listener);
			loadDataButton.addActionListener(listener);
			saveDataButton.addActionListener(listener);
			addClassButton.addActionListener(listener);

			// Buttons panel (left side of the GUI)
			buttons.add(addExampleButton);
			buttons.add(trainButton);
			buttons.add(overlayButton);
			buttons.add(resultButton);
			buttons.add(applyButton);
			buttons.add(loadDataButton);
			buttons.add(saveDataButton);
			buttons.add(addClassButton);

			for (Component comp : new Component[]{addExampleButton, trainButton, overlayButton, 
					resultButton, applyButton, loadDataButton, saveDataButton, addClassButton}) {
				comp.setMaximumSize(new Dimension(230, 50));
				comp.setPreferredSize(new Dimension(130, 30));
			}

			Panel all = new Panel();
			BoxLayout box = new BoxLayout(all, BoxLayout.X_AXIS);
			all.setLayout(box);
			all.add(buttons);
			all.add(imageAndLists);
			removeAll();
			add(all);

			pack();
			pack();

			// Propagate all listeners
			for (Panel p : new Panel[]{all, buttons, piw}) {
				for (KeyListener kl : getKeyListeners()) {
					p.addKeyListener(kl);
				}
			}

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					IJ.log("closing window");
					// cleanup
					exec.shutdownNow();
					addExampleButton.removeActionListener(listener);
					trainButton.removeActionListener(listener);
					overlayButton.removeActionListener(listener);
					resultButton.removeActionListener(listener);
					applyButton.removeActionListener(listener);
					loadDataButton.removeActionListener(listener);
					saveDataButton.removeActionListener(listener);
					addClassButton.removeActionListener(listener);
				}
			});
		}

		/* 		public void changeDisplayImage(ImagePlus imp){
  			super.getImagePlus().setProcessor(imp.getProcessor());
  			super.getImagePlus().setTitle(imp.getTitle());
  		}
		 */ 	
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
		IJ.showStatus("reading whole image data");
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

	private void setButtonsEnabled(Boolean s){
		addExampleButton.setEnabled(s);
		trainButton.setEnabled(s);
		overlayButton.setEnabled(s);
		resultButton.setEnabled(s);
		applyButton.setEnabled(s);
		loadDataButton.setEnabled(s);
		saveDataButton.setEnabled(s);
		addClassButton.setEnabled(s);
	}

	private void addExamples(int i)
	{
		//IJ.log("add examples in list "+ i + " (numOfClasses = " + numOfClasses + ")");
		//get selected pixels
		Roi r = displayImage.getRoi();
		if (null == r){
			//IJ.log("no ROI");
			return;
		}

		//IJ.log("Before killRoi r = " + r + " examples[i].size + " + examples[i].size());

		displayImage.killRoi();
		examples[i].add(r);
		//IJ.log("added ROI " + r + " to list " + i);
		exampleList[i].add("trace " + traceCounter[i]); 
		traceCounter[i]++;
		drawExamples();
	}


	private void drawExamples()
	{
		if (!showColorOverlay)
			displayImage.setProcessor("Playground", trainingImage.getProcessor().convertToRGB());
		else
			displayImage.setProcessor("Playground", overlayImage.getProcessor().convertToRGB());


		for(int i = 0; i < numOfClasses; i++)
		{
			displayImage.setColor(colors[i]);
			for (Roi r : examples[i]){
				r.drawPixels(displayImage.getProcessor());
				//IJ.log("painted ROI: " + r + " in color "+ colors[i]);
			}
		}
		displayImage.updateAndDraw();
	}

	public void createFeatureStack(ImagePlus img){
		IJ.showStatus("creating feature stack");
		featureStack = new FeatureStack(img);
		featureStack.addDefaultFeatures();
	}


	public void writeDataToARFF(Instances data, String filename){
		try{
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream( filename) ) );
			try{	
				out.write(data.toString());
				out.close();
			}
			catch(IOException e){IJ.showMessage("IOException");}
		}
		catch(FileNotFoundException e){IJ.showMessage("File not found!");}

	}

	public Instances readDataFromARFF(String filename){
		try{
			BufferedReader reader = new BufferedReader(
					new FileReader(filename));
			try{
				Instances data = new Instances(reader);
				// setting class attribute
				data.setClassIndex(data.numAttributes() - 1);
				reader.close();
				return data;
			}
			catch(IOException e){IJ.showMessage("IOException");}
		}
		catch(FileNotFoundException e){IJ.showMessage("File not found!");}
		return null;
	}

	public Instances createTrainingInstances()
	{
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=featureStack.getSize(); i++){
			String attString = featureStack.getSliceLabel(i) + " numeric";
			attributes.add(new Attribute(attString));
		}
		ArrayList<String> classes = new ArrayList<String>();

		int numOfInstances = 0;
		for(int i = 0; i < numOfClasses ; i ++)
		{
			classes.add(classLabels[i]);
			numOfInstances += examples[i].size();
		}

		attributes.add(new Attribute("class", classes));

		Instances trainingData =  new Instances("segment", attributes, numOfInstances);

		for(int l = 0; l < numOfClasses; l++)
		{
			for(int j=0; j<examples[l].size(); j++)
			{
				Roi r = examples[l].get(j);
				//need to take care of shapeRois that are represented as multiple polygons
				Roi[] rois;
				if (r instanceof ij.gui.ShapeRoi){
					//IJ.log("shape roi detected");
					rois = ((ShapeRoi) r).getRois();
				}
				else{
					rois = new Roi[1];
					rois[0] = r;
				}

				for(int k=0; k<rois.length; k++){
					int[] x = rois[k].getPolygon().xpoints;
					int[] y = rois[k].getPolygon().ypoints;
					int n = rois[k].getPolygon().npoints;

					for (int i=0; i<n; i++){
						double[] values = new double[featureStack.getSize()+1];
						for (int z=1; z<=featureStack.getSize(); z++){
							values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
						}
						values[featureStack.getSize()] = (double) l;
						trainingData.add(new DenseInstance(1.0, values));
					}
				}
			}
		}

		return trainingData;
	}


	public void trainClassifier(){
		if (examples[0].size()==0 & loadedTrainingData==null){
			IJ.showMessage("Cannot train without positive examples!");
			return;
		}
		if (examples[1].size()==0 & loadedTrainingData==null){
			IJ.showMessage("Cannot train without negative examples!");
			return;
		}

		setButtonsEnabled(false);

		IJ.showStatus("training classifier");
		Instances data = null;
		if (0 == examples[0].size() | 0 == examples[1].size())
			IJ.log("training from loaded data only");
		else {
			long start = System.currentTimeMillis();
			data = createTrainingInstances();
			long end = System.currentTimeMillis();
			IJ.log("creating training data took: " + (end-start));
			data.setClassIndex(data.numAttributes() - 1);
		}

		if (loadedTrainingData != null & data != null){
			IJ.log("merging data");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished");
		}
		else if (data == null){
			data = loadedTrainingData;
			IJ.log("Taking loaded data as only data...");
		}

		IJ.showStatus("Ttraining classifier...");
		IJ.log("Training classifier...");
		if (null == data){
			IJ.log("WTF");
		}
		try{rf.buildClassifier(data);}
		catch(Exception e){IJ.showMessage(e.getMessage());}

		IJ.log("Classifying whole image...");

		classifiedImage = applyClassifier(wholeImageData, trainingImage.getWidth(), trainingImage.getHeight());

		overlayButton.setEnabled(true);
		resultButton.setEnabled(true);
		applyButton.setEnabled(true);
		showColorOverlay = false;
		toggleOverlay();

		setButtonsEnabled(true);
	}

	public ImagePlus applyClassifier(Instances data, int w, int h){
		IJ.showStatus("Classifying image...");
		double[] classificationResult = new double[data.numInstances()];
		for (int i=0; i<data.numInstances(); i++){
			try{
				classificationResult[i] = rf.classifyInstance(data.instance(i));
			}catch(Exception e){IJ.showMessage("Could not apply Classifier!");}
		}

		IJ.showStatus("showing result");
		ImageProcessor classifiedImageProcessor = new FloatProcessor(w, h, classificationResult);
		classifiedImageProcessor.convertToByte(true);
		ImagePlus classImg = new ImagePlus("classification result", classifiedImageProcessor);
		return classImg;
	}

	void toggleOverlay(){
		showColorOverlay = !showColorOverlay;
		IJ.log("toggel overlay to: " + showColorOverlay);
		if (showColorOverlay){
			//do this every time cause most likely classification changed
			int width = trainingImage.getWidth();
			int height = trainingImage.getHeight();

			ImageProcessor white = new ByteProcessor(width, height);
			white.setMinAndMax(255, 255);

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

	void listSelected(final ItemEvent e, final int i)
	{
		drawExamples();
		displayImage.setColor(Color.YELLOW);

		for(int j = 0; j < numOfClasses; j++)
		{
			if (j == i) 
				examples[i].get(exampleList[i].getSelectedIndex()).drawPixels(displayImage.getProcessor());
			else
				exampleList[j].deselect(exampleList[j].getSelectedIndex());
		}

		displayImage.updateAndDraw();
	}

	void deleteSelected(final ActionEvent e){

		for(int i = 0; i < numOfClasses; i++)
		{

			if (e.getSource() == exampleList[i]) {
				//delete item from ROI
				int index = exampleList[i].getSelectedIndex();
				examples[i].remove(index);
				//delete item from list
				exampleList[i].remove(index);
			}
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
		ImagePlus resultImage = new ImagePlus("classification result", classifiedImage.getProcessor().convertToByte(true).duplicate());
		resultImage.show();
	}

	public void applyClassifierToTestData(){
		ImagePlus testImage = IJ.openImage();
		if (null == testImage) return; // user canceled open dialog

		setButtonsEnabled(false);

		if (testImage.getImageStackSize() == 1){
			applyClassifierToTestImage(testImage).show();
			testImage.show();
		}
		else{
			ImageStack testImageStack = testImage.getStack();
			ImageStack testStackClassified = new ImageStack(testImageStack.getWidth(), testImageStack.getHeight());
			IJ.log("size: " + testImageStack.getSize() + " " + testImageStack.getWidth() + " " + testImageStack.getHeight());
			for (int i=1; i<=testImageStack.getSize(); i++){
				IJ.log("classifying image " + i);
				ImagePlus currentSlice = new ImagePlus(testImageStack.getSliceLabel(i),testImageStack.getProcessor(i).duplicate());
				//applyClassifierToTestImage(currentSlice).show();
				testStackClassified.addSlice(currentSlice.getTitle(), applyClassifierToTestImage(currentSlice).getProcessor().duplicate());
			}
			testImage.show();
			ImagePlus showStack = new ImagePlus("classified Stack", testStackClassified);
			showStack.show();
		}
		setButtonsEnabled(true);
	}


	public ImagePlus applyClassifierToTestImage(ImagePlus testImage){
		testImage.setProcessor(testImage.getProcessor().convertToByte(true));

		IJ.showStatus("creating features for test image");
		FeatureStack testImageFeatures = new FeatureStack(testImage);
		testImageFeatures.addDefaultFeatures();

		Instances testData = testImageFeatures.createInstances();
		testData.setClassIndex(testData.numAttributes() - 1);

		ImagePlus testClassImage = applyClassifier(testData, testImage.getWidth(), testImage.getHeight());
		testClassImage.setTitle("classified_" + testImage.getTitle());
		testClassImage.setProcessor(testClassImage.getProcessor().convertToByte(true).duplicate());

		return testClassImage;
	}

	public void loadTrainingData(){
		OpenDialog od = new OpenDialog("choose data file","");
		if (od.getFileName()==null)
			return;
		IJ.log("load data from " + od.getDirectory() + od.getFileName());
		loadedTrainingData = readDataFromARFF(od.getDirectory() + od.getFileName());
		IJ.log("loaded data: " + loadedTrainingData.numInstances());
	}
	public void saveTrainingData(){
		if (examples[0].size() == 0 & examples[1].size() == 0 & loadedTrainingData == null){
			IJ.showMessage("There is no data to save");
			return;
		}

		Instances data = createTrainingInstances();
		data.setClassIndex(data.numAttributes() - 1);
		if (null != loadedTrainingData & null != data){
			IJ.log("merging data");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("finished");
		}
		else if (null == data)
			data = loadedTrainingData;

		SaveDialog sd = new SaveDialog("choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;
		IJ.log("writing training data: " + data.numInstances());
		writeDataToARFF(data, sd.getDirectory() + sd.getFileName());
		IJ.log("wrote training data " + sd.getDirectory() + " " + sd.getFileName());
	}

}
