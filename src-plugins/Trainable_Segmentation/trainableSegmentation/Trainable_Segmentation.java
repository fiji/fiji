package trainableSegmentation;
/** 
 * This is a small Plugin that should perform better in segmentation than thresholding
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
 * Authors: Verena Kaynig (verena.kaynig@inf.ethz.ch), Ignacio Arganda-Carreras (iarganda@mit.edu)
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */


import ij.IJ;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
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
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;


import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import hr.irb.fastRandomForest.FastRandomForest;

public class Trainable_Segmentation implements PlugIn {


	private static final int MAX_NUM_CLASSES = 5;

	private List<Roi> [] examples = new ArrayList[MAX_NUM_CLASSES]; 
	private ImagePlus trainingImage;
	private ImagePlus displayImage;
	private ImagePlus classifiedImage;
	private ImagePlus overlayImage;
	private FeatureStack featureStack = null;
	private CustomWindow win;

	private int traceCounter[] = new int[MAX_NUM_CLASSES];
	private boolean showColorOverlay;
	private Instances wholeImageData;
	private Instances loadedTrainingData;
	private FastRandomForest rf;
	
	private static boolean updateWholeData = true;
	
	final JButton addExampleButton;
	final JButton trainButton;
	final JButton overlayButton;
	final JButton resultButton;
	final JButton applyButton;
	final JButton loadDataButton;
	final JButton saveDataButton;
	final JButton settingsButton;

	final JButton addClassButton;

	final Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
			Color.orange, Color.pink};

	String[] classLabels = new String[]{"background", "foreground", "class-3", "class-4", "class-5"};

	private static int numOfClasses = 2;
	private java.awt.List exampleList[] = new java.awt.List[MAX_NUM_CLASSES];
	private JRadioButton [] classButton = new JRadioButton[MAX_NUM_CLASSES];
	//Group the radio buttons.
	ButtonGroup classButtonGroup = new ButtonGroup();

	
	// Random Forest parameters
	private static int numOfTrees = 200;
	private static int randomFeatures = 2;

	/**
	 * Basic constructor
	 */
	public Trainable_Segmentation() 
	{
		addExampleButton = new JButton("ADD");
		addExampleButton.setToolTipText("Add current ROI to selected label");
		
		trainButton = new JButton("Train classifier");
		trainButton.setToolTipText("Start training the classifier");
		
		overlayButton = new JButton("Toggle overlay");
		overlayButton.setToolTipText("Toggle between current segmentation and original image");		
		overlayButton.setEnabled(false);
		
		resultButton = new JButton("Create result");
		resultButton.setToolTipText("Generate result image");
		resultButton.setEnabled(false);
		
		
		applyButton = new JButton ("Apply classifier");
		applyButton.setToolTipText("Load data and apply current classifier");
		applyButton.setEnabled(false);
		
		loadDataButton = new JButton ("Load data");
		loadDataButton.setToolTipText("Load previous segmentation from an ARFF file");
		
		saveDataButton = new JButton ("Save data");
		saveDataButton.setToolTipText("Save current segmentation into an ARFF file");

		addClassButton = new JButton ("Create new label");
		addClassButton.setToolTipText("Add one more label to mark different areas");
		
		settingsButton = new JButton ("Settings");
		addClassButton.setToolTipText("Display advanced options");

		for(int i = 0; i < numOfClasses ; i++)
		{
			examples[i] = new ArrayList<Roi>();
			exampleList[i] = new java.awt.List(5);
			exampleList[i].setForeground(colors[i]);
		}

		showColorOverlay = false;

		rf = new FastRandomForest();
		//FIXME: should depend on image size?? Or labels??
		rf.setNumTrees(Trainable_Segmentation.numOfTrees);
		//this is the default that Breiman suggests
		//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		//but this seems to work better
		rf.setNumFeatures(Trainable_Segmentation.randomFeatures);


		rf.setSeed(123);
	}

	final ExecutorService exec = Executors.newFixedThreadPool(1);

	
	/**
	 * Listeners
	 */
	private ActionListener listener = new ActionListener() {
		public void actionPerformed(final ActionEvent e) {
			exec.submit(new Runnable() {
				public void run() {

					if(e.getSource() == addExampleButton)
					{
						//IJ.log("+ pressed");
						for(int i = 0; i < numOfClasses; i++)
						{
							if(classButton[i].isSelected())
							{
								addExamples(i);
								break;
							}
						}
					}
					else if(e.getSource() == trainButton)
					{
							try{
								trainClassifier();
							}catch(Exception e){
								e.printStackTrace();
							}
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
					else if(e.getSource() == addClassButton){
						addNewClass();
					}
					else if(e.getSource() == settingsButton){
						showSettingsDialog();
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
	
	/**
	 * Custom canvas to deal with zooming an panning
	 */
	private class CustomCanvas extends ImageCanvas {
		CustomCanvas(ImagePlus imp) {
			super(imp);
			Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
			setMinimumSize(dim);
			setSize(dim.width, dim.height);
			setDstDimensions(dim.width, dim.height);
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ke) {
					repaint();
				}
			});
		}
		//@Override
		public void setDrawingSize(int w, int h) {}

		public void setDstDimensions(int width, int height) {
			super.dstWidth = width;
			super.dstHeight = height;
			// adjust srcRect: can it grow/shrink?
			int w = Math.min((int)(width  / magnification), imp.getWidth());
			int h = Math.min((int)(height / magnification), imp.getHeight());
			int x = srcRect.x;
			if (x + w > imp.getWidth()) x = w - imp.getWidth();
			int y = srcRect.y;
			if (y + h > imp.getHeight()) y = h - imp.getHeight();
			srcRect.setRect(x, y, w, h);
			repaint();
		}

		//@Override
		public void paint(Graphics g) {
			Rectangle srcRect = getSrcRect();
			double mag = getMagnification();
			int dw = (int)(srcRect.width * mag);
			int dh = (int)(srcRect.height * mag);
			g.setClip(0, 0, dw, dh);

			super.paint(g);

			int w = getWidth();
			int h = getHeight();
			g.setClip(0, 0, w, h);

			// Paint away the outside
			g.setColor(getBackground());
			g.fillRect(dw, 0, w - dw, h);
			g.fillRect(0, dh, w, h - dh);
		}
	}

	/**
	 * Custom window to define the trainable segmentation GUI
	 * 
	 */
	private class CustomWindow extends ImageWindow 
	{
		/** layout for annotation panel */
		GridBagLayout boxAnnotation = new GridBagLayout();
		/** constraints for annotation panel */
		GridBagConstraints annotationsConstraints = new GridBagConstraints();
		/** Panel with class radio buttons and lists */
		JPanel annotationsPanel = new JPanel();
		
		JPanel buttonsPanel = new JPanel();
		
		JPanel trainingJPanel = new JPanel();
		JPanel optionsJPanel = new JPanel();		
		
		Panel all = new Panel();
		
		CustomWindow(ImagePlus imp) 
		{
			super(imp, new CustomCanvas(imp));

			final CustomCanvas canvas = (CustomCanvas) getCanvas();

			// Remove the canvas from the window, to add it later
			removeAll();

			setTitle("Trainable Segmentation");
			
			// Annotations panel
			annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
			annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
			annotationsConstraints.gridwidth = 1;
			annotationsConstraints.gridheight = 1;
			annotationsConstraints.gridx = 0;
			annotationsConstraints.gridy = 0;
			
			annotationsConstraints.insets = new Insets(5, 5, 6, 6);
			annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
			annotationsPanel.setLayout(boxAnnotation);
			
			annotationsPanel.add(addExampleButton, annotationsConstraints);
			annotationsConstraints.gridy++;
			
			annotationsConstraints.insets = new Insets(0,0,0,0);
			annotationsConstraints.fill = GridBagConstraints.NONE;
			
			for(int i = 0; i < numOfClasses ; i++)
			{
				exampleList[i].addActionListener(listener);
				classButton[i] = new JRadioButton(classLabels[i]);
				classButton[i].setToolTipText("Select to add markings of label '" + classLabels[i] + "'");
				classButtonGroup.add(classButton[i]);

				boxAnnotation.setConstraints(classButton[i], annotationsConstraints);
				annotationsPanel.add(classButton[i]);
				annotationsConstraints.gridy++;

				boxAnnotation.setConstraints(exampleList[i], annotationsConstraints);
				annotationsPanel.add(exampleList[i]);
				annotationsConstraints.gridy++;
			}

			// Select first class
			classButton[1].setSelected(true);
			

			// Add listeners
			addExampleButton.addActionListener(listener);
			trainButton.addActionListener(listener);
			overlayButton.addActionListener(listener);
			resultButton.addActionListener(listener);
			applyButton.addActionListener(listener);
			loadDataButton.addActionListener(listener);
			saveDataButton.addActionListener(listener);
			addClassButton.addActionListener(listener);
			settingsButton.addActionListener(listener);

			// Training panel (left side of the GUI)
			trainingJPanel.setBorder(BorderFactory.createTitledBorder("Training"));
			GridBagLayout trainingLayout = new GridBagLayout();
			GridBagConstraints trainingConstraints = new GridBagConstraints();
			trainingConstraints.anchor = GridBagConstraints.NORTHWEST;
			trainingConstraints.fill = GridBagConstraints.HORIZONTAL;
			trainingConstraints.gridwidth = 1;
			trainingConstraints.gridheight = 1;
			trainingConstraints.gridx = 0;
			trainingConstraints.gridy = 0;
			trainingConstraints.insets = new Insets(5, 5, 6, 6);
			trainingJPanel.setLayout(trainingLayout);
			
			trainingJPanel.add(trainButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(overlayButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(resultButton, trainingConstraints);
			trainingConstraints.gridy++;
			
			
			// Options panel
			optionsJPanel.setBorder(BorderFactory.createTitledBorder("Options"));
			GridBagLayout optionsLayout = new GridBagLayout();
			GridBagConstraints optionsConstraints = new GridBagConstraints();
			optionsConstraints.anchor = GridBagConstraints.NORTHWEST;
			optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
			optionsConstraints.gridwidth = 1;
			optionsConstraints.gridheight = 1;
			optionsConstraints.gridx = 0;
			optionsConstraints.gridy = 0;
			optionsConstraints.insets = new Insets(5, 5, 6, 6);
			optionsJPanel.setLayout(optionsLayout);
			
			optionsJPanel.add(applyButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(loadDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(saveDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(addClassButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(settingsButton, optionsConstraints);
			optionsConstraints.gridy++;
			
			// Buttons panel (including training and options)
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsPanel.setLayout(buttonsLayout);
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsConstraints.gridwidth = 1;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsPanel.add(trainingJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(optionsJPanel, buttonsConstraints);
			

			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 1;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;

			all.add(buttonsPanel, allConstraints);

			allConstraints.gridx++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			all.add(canvas, allConstraints);

			allConstraints.gridx++;
			allConstraints.anchor = GridBagConstraints.NORTHEAST;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			all.add(annotationsPanel, allConstraints);

			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.BOTH;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout(wingb);
			add(all, winc);

			
			// Propagate all listeners
			for (Component p : new Component[]{all, buttonsPanel}) {
				for (KeyListener kl : getKeyListeners()) {
					p.addKeyListener(kl);
				}
			}

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					//IJ.log("closing window");
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
					settingsButton.removeActionListener(listener);
					
					// Set number of classes back to 2
					numOfClasses = 2;
				}
			});

			canvas.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Rectangle r = canvas.getBounds();
					canvas.setDstDimensions(r.width, r.height);
				}
			});
		}

		/* 		public void changeDisplayImage(ImagePlus imp){
  			super.getImagePlus().setProcessor(imp.getProcessor());
  			super.getImagePlus().setTitle(imp.getTitle());
  		}
		 */


		/**
		 * Repaint all panels
		 */
		public void repaintAll()
		{
			this.annotationsPanel.repaint();
			getCanvas().repaint();
			this.buttonsPanel.repaint();
			this.all.repaint();
		}
		
		/**
		 * Add new segmentation class (new label and new list on the right side)
		 */
		public void addClass()
		{
			examples[numOfClasses] = new ArrayList<Roi>();
			exampleList[numOfClasses] = new java.awt.List(5);
			exampleList[numOfClasses].setForeground(colors[numOfClasses]);
			
			exampleList[numOfClasses].addActionListener(listener);
			classButton[numOfClasses] = new JRadioButton(classLabels[numOfClasses]);
			classButtonGroup.add(classButton[numOfClasses]);

			boxAnnotation.setConstraints(classButton[numOfClasses], annotationsConstraints);
			annotationsPanel.add(classButton[numOfClasses]);
			annotationsConstraints.gridy++;

			boxAnnotation.setConstraints(exampleList[numOfClasses], annotationsConstraints);
			annotationsPanel.add(exampleList[numOfClasses]);
			annotationsConstraints.gridy++;
			
			// increase number of available classes
			numOfClasses ++;
			
			//IJ.log("new number of classes = " + numOfClasses);
			
			repaintAll();
		}
	}

	/**
	 * Plugin run method
	 */
	public void run(String arg) {
		//		trainingImage = IJ.openImage("testImages/i00000-1.tif");
		//get current image
		if (null == WindowManager.getCurrentImage()) {
			trainingImage = IJ.openImage();
			if (null == trainingImage) return; // user canceled open dialog
		}
		else {
			trainingImage = new ImagePlus("Trainable Segmentation",WindowManager.getCurrentImage().getProcessor().duplicate());
		}

		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
					"is larger than 1024 pixels. \n" +
					"Feature stack creation and classifier training \n" +
					"might take some time depending on your computer.\n" +
			"Proceed?"))
				return;


		trainingImage.setProcessor("Trainable Segmentation", trainingImage.getProcessor().duplicate().convertToByte(true));
		
		// Initialize feature stack (no features yet)
		featureStack = new FeatureStack(trainingImage);
		
		displayImage = new ImagePlus();
		displayImage.setProcessor("Trainable Segmentation", trainingImage.getProcessor().duplicate().convertToRGB());

		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.FREELINE);

		
		//Build GUI
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win = new CustomWindow(displayImage);
						win.pack();
					}
				});
		
		//trainingImage.getWindow().setVisible(false);
	}

	/**
	 * Enable / disable buttons
	 * @param s enabling flag
	 */
	private void setButtonsEnabled(Boolean s)
	{
		addExampleButton.setEnabled(s);
		trainButton.setEnabled(s);
		overlayButton.setEnabled(s);
		resultButton.setEnabled(s);
		applyButton.setEnabled(s);
		loadDataButton.setEnabled(s);
		saveDataButton.setEnabled(s);
		addClassButton.setEnabled(s);
		settingsButton.setEnabled(s);
	}

	/**
	 * Add examples defined by the user to the corresponding list
	 * @param i list index
	 */
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
			displayImage.setProcessor("Trainable Segmentation", trainingImage.getProcessor().convertToRGB());
		else
			displayImage.setProcessor("Trainable Segmentation", overlayImage.getProcessor().convertToRGB());


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

	/**
	 * Write current instances into an ARFF file
	 * @param data set of instances
	 * @param filename ARFF file name
	 */
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

	/**
	 * Read ARFF file
	 * @param filename ARFF file name
	 * @return set of instances read from the file
	 */
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

	/**
	 * Create training instances out of the user markings
	 * @return set of instances
	 */
	public Instances createTrainingInstances()
	{
		//IJ.log("create training instances: num of features = " + featureStack.getSize());
		
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=featureStack.getSize(); i++){
			String attString = featureStack.getSliceLabel(i) + " numeric";
			attributes.add(new Attribute(attString));
		}
		
		final ArrayList<String> classes = new ArrayList<String>();

		int numOfInstances = 0;
		for(int i = 0; i < numOfClasses ; i ++)
		{
			// Do not add empty lists
			if(examples[i].size() > 0)
				classes.add(classLabels[i]);
			numOfInstances += examples[i].size();
		}

		attributes.add(new Attribute("class", classes));

		final Instances trainingData =  new Instances("segment", attributes, numOfInstances);

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

	/**
	 * Train classifier with the current instances
	 */
	public void trainClassifier()
	{	
		// Two list of examples need to be non empty
		int nonEmpty = 0;
		for(int i = 0; i < numOfClasses; i++)
			if(examples[i].size() > 0)
				nonEmpty++;
		if (nonEmpty < 2 && loadedTrainingData==null){
			IJ.showMessage("Cannot train without at least 2 sets of examples!");
			return;
		}
		
		// Disable buttons until the training has finished
		setButtonsEnabled(false);

		// Create feature stack if it was not created yet
		if(featureStack.isEmpty())
		{
			IJ.showStatus("Creating feature stack...");
			featureStack.addDefaultFeatures();
		}
		

		IJ.showStatus("Training classifier...");
		Instances data = null;
		if (0 == examples[0].size() || 0 == examples[1].size())
			IJ.log("Training from loaded data only...");
		else {
			long start = System.currentTimeMillis();
			data = createTrainingInstances();
			long end = System.currentTimeMillis();
			IJ.log("Creating training data took: " + (end-start) + "ms");
			data.setClassIndex(data.numAttributes() - 1);
		}

		if (loadedTrainingData != null && data != null){
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished");
		}
		else if (data == null){
			data = loadedTrainingData;
			IJ.log("Taking loaded data as only data...");
		}

		IJ.showStatus("Training classifier...");
		IJ.log("Training classifier...");
		if (null == data){
			IJ.log("WTF");
		}
		
		// Train the classifier on the current data
		try{
			rf.buildClassifier(data);
		}
		catch(Exception e){
			IJ.showMessage(e.getMessage());
		}
		
		//
		if(updateWholeData)
			updateTestSet();

		IJ.log("Classifying whole image...");

		classifiedImage = applyClassifier(wholeImageData, trainingImage.getWidth(), trainingImage.getHeight());

		IJ.log("Finished segmentation of whole image");
		
		overlayButton.setEnabled(true);
		resultButton.setEnabled(true);
		applyButton.setEnabled(true);
		showColorOverlay = false;
		toggleOverlay();

		setButtonsEnabled(true);
		
		//featureStack.show();
	}
	
	/**
	 * Update whole data set with current number of classes and features
	 */
	private void updateTestSet() 
	{
		IJ.showStatus("Reading whole image data...");
		
		long start = System.currentTimeMillis();
		ArrayList<String> classNames = new ArrayList<String>();
		for(int i = 0; i < numOfClasses; i++)
			if(examples[i].size() > 0)
				classNames.add(classLabels[i]);
		
		wholeImageData = featureStack.createInstances(classNames);
		long end = System.currentTimeMillis();
		IJ.log("Creating whole image data took: " + (end-start) + "ms");
		wholeImageData.setClassIndex(wholeImageData.numAttributes() - 1);
		
		updateWholeData = false;
	}

	/**
	 * Apply current classifier to set of instances
	 * @param data set of instances
	 * @param w image width
	 * @param h image height
	 * @return result image
	 */
	public ImagePlus applyClassifier(Instances data, int w, int h)
	{
		IJ.showStatus("Classifying image...");
		
		final long start = System.currentTimeMillis();
		final int numInstances = data.numInstances();
		final double[] classificationResult = new double[numInstances];
		for (int i=0; i<numInstances; i++)
		{
			IJ.showProgress((double) i / numInstances);
			try{
				classificationResult[i] = rf.classifyInstance(data.instance(i));
			}catch(Exception e){
				IJ.showMessage("Could not apply Classifier!");
				e.printStackTrace();
				return null;
			}
		}
		IJ.showProgress(1.0);
		
		final long end = System.currentTimeMillis();
		IJ.log("Classifying whole image data took: " + (end-start) + "ms");

		IJ.showStatus("Displaying result...");
		final ImageProcessor classifiedImageProcessor = new FloatProcessor(w, h, classificationResult);
		classifiedImageProcessor.convertToByte(true);
		ImagePlus classImg = new ImagePlus("Classification result", classifiedImageProcessor);
		return classImg;
	}

	/**
	 * Toggle between overlay and original image with markings
	 */
	void toggleOverlay()
	{
		showColorOverlay = !showColorOverlay;
		//IJ.log("toggel overlay to: " + showColorOverlay);
		if (showColorOverlay)
		{
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

	/**
	 * Select a list and deselect the others
	 * @param e item event (originated by a list)
	 * @param i list index
	 */
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

	/**
	 * Delete one of the ROIs
	 * 
	 * @param e action event
	 */
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

	/**
	 * Apply classifier to test data
	 */
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
			IJ.log("Size: " + testImageStack.getSize() + " " + testImageStack.getWidth() + " " + testImageStack.getHeight());
			for (int i=1; i<=testImageStack.getSize(); i++){
				IJ.log("Classifying image " + i + "...");
				ImagePlus currentSlice = new ImagePlus(testImageStack.getSliceLabel(i),testImageStack.getProcessor(i).duplicate());
				//applyClassifierToTestImage(currentSlice).show();
				testStackClassified.addSlice(currentSlice.getTitle(), applyClassifierToTestImage(currentSlice).getProcessor().duplicate());
			}
			testImage.show();
			ImagePlus showStack = new ImagePlus("Classified Stack", testStackClassified);
			showStack.show();
		}
		setButtonsEnabled(true);
	}

	/**
	 * Apply current classifier to image
	 * @param testImage test image
	 * @return result image
	 */
	public ImagePlus applyClassifierToTestImage(ImagePlus testImage)
	{
		testImage.setProcessor(testImage.getProcessor().convertToByte(true));

		IJ.showStatus("Creating features for test image...");
		final FeatureStack testImageFeatures = new FeatureStack(testImage);
		testImageFeatures.addDefaultFeatures();

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		for(int i = 0; i < numOfClasses; i++)
			if(examples[i].size() > 0)
				classNames.add(classLabels[i]);
		
		final Instances testData = testImageFeatures.createInstances(classNames);
		testData.setClassIndex(testData.numAttributes() - 1);

		final ImagePlus testClassImage = applyClassifier(testData, testImage.getWidth(), testImage.getHeight());
		testClassImage.setTitle("classified_" + testImage.getTitle());
		testClassImage.setProcessor(testClassImage.getProcessor().convertToByte(true).duplicate());

		return testClassImage;
	}

	/**
	 * Load previously saved model
	 */
	public void loadTrainingData(){
		OpenDialog od = new OpenDialog("choose data file","");
		if (od.getFileName()==null)
			return;
		IJ.log("Loading data from " + od.getDirectory() + od.getFileName() + "...");
		loadedTrainingData = readDataFromARFF(od.getDirectory() + od.getFileName());
		IJ.log("Loaded data: " + loadedTrainingData.numInstances());
	}
	
	/**
	 * Save training model into a file
	 */
	public void saveTrainingData()
	{
		boolean examplesEmpty = true;
		for(int i = 0; i < numOfClasses; i ++)
			if(examples[i].size() > 0)
			{
				examplesEmpty = false;
				break;
			}
		if (examplesEmpty && loadedTrainingData == null){
			IJ.showMessage("There is no data to save");
			return;
		}

		Instances data = createTrainingInstances();
		data.setClassIndex(data.numAttributes() - 1);
		if (null != loadedTrainingData && null != data){
			IJ.log("merging data");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished");
		}
		else if (null == data)
			data = loadedTrainingData;

		SaveDialog sd = new SaveDialog("Choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;
		IJ.log("writing training data: " + data.numInstances());
		writeDataToARFF(data, sd.getDirectory() + sd.getFileName());
		IJ.log("wrote training data " + sd.getDirectory() + " " + sd.getFileName());
	}
	
	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass() 
	{
		if(numOfClasses == MAX_NUM_CLASSES)
		{
			IJ.showMessage("Trainable Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

		//IJ.log("Adding new class...");
		
		String inputName = JOptionPane.showInputDialog("Please input a new label name");
		
		if(null == inputName)
			return;
		
		classLabels[numOfClasses] = inputName;
		
		// Add new class label and list
		win.addClass();
		
		repaintWindow();
		
		// Force whole data to be updated
		updateWholeData = true;
		
	}

	/**
	 * Repaint whole window
	 */
	private void repaintWindow() {
		// Repaint window
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win.invalidate();
						win.validate();
						win.repaint();
					}
				});
		
	}
	
	/**
	 * Show advanced settings dialog
	 * 
	 * @return false when canceled
	 */
	public boolean showSettingsDialog()
	{
		GenericDialog gd = new GenericDialog("Segmentation settings");
		
		final boolean[] oldEnableFeatures = this.featureStack.getEnableFeatures();
		
		gd.addMessage("Training features:");
		final int rows = (int)Math.round(FeatureStack.availableFeatures.length/2.0);
		IJ.log("rows = " + rows);
		gd.addCheckboxGroup(rows, 2, FeatureStack.availableFeatures, oldEnableFeatures);
		gd.addMessage("Fast Random Forest settings:");
		gd.addNumericField("Number of trees:", Trainable_Segmentation.numOfTrees, 0);
		gd.addNumericField("Random features", Trainable_Segmentation.randomFeatures, 0);
		
		gd.showDialog();
		
		if (gd.wasCanceled())
			return false;
		
		final int numOfFeatures = FeatureStack.availableFeatures.length;
		
		final boolean[] newEnableFeatures = new boolean[numOfFeatures];
		
		boolean featuresChanged = false;
		
		// Read checked features and check if any of them changed
		for(int i = 0; i < numOfFeatures; i++)
		{
			newEnableFeatures[i] = gd.getNextBoolean();
			if (newEnableFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}

		// Read fast random forest parameters and check if changed
		final int newNumTrees = (int) gd.getNextNumber();
		final int newRandomFeatures = (int) gd.getNextNumber();
		
		// Update random forest if necessary
		if(newNumTrees != Trainable_Segmentation.numOfTrees ||
				newRandomFeatures != Trainable_Segmentation.randomFeatures)
				updateClassifier(newNumTrees, newRandomFeatures);
		
		// Update feature stack if necessary
		if(featuresChanged)
		{
			this.setButtonsEnabled(false);
			//IJ.log("Settings (before update): num of features = " + featureStack.getSize());
			this.featureStack.setEnableFeatures(newEnableFeatures);
			this.featureStack.updateFeatures();
			//IJ.log("Settings (after update): num of features = " + featureStack.getSize());
			this.setButtonsEnabled(true);
			// Force whole data to be updated
			updateWholeData = true;
		}
		
		return true;
	}

	/**
	 * Update fast random forest classifier with new values
	 * 
	 * @param newNumTrees new number of trees
	 * @param newRandomFeatures new number of random features per tree
	 * @return false if error
	 */
	private boolean updateClassifier(int newNumTrees, int newRandomFeatures) 
	{
		if(newNumTrees < 1 || newRandomFeatures < 1)
			return false;
		Trainable_Segmentation.numOfTrees = newNumTrees;
		Trainable_Segmentation.randomFeatures = newRandomFeatures;
		
		rf.setNumTrees(Trainable_Segmentation.numOfTrees);
		rf.setNumFeatures(Trainable_Segmentation.randomFeatures);
		
		return true;
	}
	

}
