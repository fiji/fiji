package trainableSegmentation;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;

import ij.IJ;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.StackConverter;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyEditor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;

import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;

import weka.core.FastVector;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;

import weka.gui.GUIChooser;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyDialog;
import weka.gui.PropertyPanel;

import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

/**
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
 * Authors: Ignacio Arganda-Carreras (iargandacarreras@gmail.com), Verena Kaynig,
 *          Albert Cardona
 */



/**
 * Segmentation plugin based on the machine learning library Weka
 */
public class Weka_Segmentation implements PlugIn
{
	/** reference to the segmentation backend */
	final WekaSegmentation wekaSegmentation;
	
	/** image to display on the GUI */
	private ImagePlus displayImage;
	/** image to be used in the training */
	private ImagePlus trainingImage;
	/** result image after classification */
	private ImagePlus classifiedImage;
	/** GUI window */
	private CustomWindow win;
	/** number of classes in the GUI */
	private int numOfClasses;
	/** array of number of traces per class */
	private int traceCounter[] = new int[WekaSegmentation.MAX_NUM_CLASSES];
	/** flag to display the overlay image */
	private boolean showColorOverlay;
	/** executor service to launch threads for the plugin methods and events */
	final ExecutorService exec = Executors.newFixedThreadPool(1);

	/** train classifier button */
	JButton trainButton;
	/** toggle overlay button */
	JButton overlayButton;
	/** create result button */
	JButton resultButton;
	/** get probability maps button */
	JButton probabilityButton;
	/** plot result button */
	JButton plotButton;
	/** new image button */
	//JButton newImageButton;
	/** apply classifier button */
	JButton applyButton;
	/** load classifier button */
	JButton loadClassifierButton;
	/** save classifier button */
	JButton saveClassifierButton;
	/** load data button */
	JButton loadDataButton;
	/** save data button */
	JButton saveDataButton;
	/** settings button */
	JButton settingsButton;
	/** Weka button */
	JButton wekaButton;
	/** create new class button */
	JButton addClassButton;

	/** array of roi list overlays to paint the transparent rois of each class */
	RoiListOverlay [] roiOverlay;
	
	/** available colors for available classes */
	Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
			Color.cyan, Color.magenta};

	/** Lookup table for the result overlay image */
	LUT overlayLUT;

	/** array of trace lists for every class */
	private java.awt.List exampleList[];
	/** array of buttons for adding each trace class */
	private JButton [] addExampleButton;
	
	// Macro recording constants (corresponding to  
	// static method names to be called)
	/** name of the macro method to add the current trace to a class */
	public static final String ADD_TRACE = "addTrace";
	/** name of the macro method to delete the current trace */
	public static final String DELETE_TRACE = "deleteTrace";
	/** name of the macro method to train the current classifier */
	public static final String TRAIN_CLASSIFIER = "trainClassifier";
	/** name of the macro method to toggle the overlay image */
	public static final String TOGGLE_OVERLAY = "toggleOverlay";
	/** name of the macro method to get the binary result */
	public static final String GET_RESULT = "getResult";
	/** name of the macro method to get the probability maps */
	public static final String GET_PROBABILITY = "getProbability";
	/** name of the macro method to plot the threshold curves */
	public static final String PLOT_RESULT = "plotResultGraphs";
	/** name of the macro method to apply the current classifier to an image or stack */
	public static final String APPLY_CLASSIFIER = "applyClassifier";
	/** name of the macro method to load a classifier from file */
	public static final String LOAD_CLASSIFIER = "loadClassifier";
	/** name of the macro method to save the current classifier into a file */
	public static final String SAVE_CLASSIFIER = "saveClassifier";
	/** name of the macro method to load data from an ARFF file */
	public static final String LOAD_DATA = "loadData";
	/** name of the macro method to save the current data into an ARFF file */
	public static final String SAVE_DATA = "saveData";
	/** name of the macro method to create a new class */
	public static final String CREATE_CLASS = "createNewClass";
	/** name of the macro method to launch the Weka Chooser */
	public static final String LAUNCH_WEKA = "launchWeka";
	/** name of the macro method to enable/disable a feature */
	public static final String SET_FEATURE = "setFeature";
	/** name of the macro method to set the membrane thickness */
	public static final String SET_MEMBRANE_THICKNESS = "setMembraneThickness";
	/** name of the macro method to set the membrane patch size */
	public static final String SET_MEMBRANE_PATCH = "setMembranePatchSize";
	/** name of the macro method to set the minimum kernel radius */
	public static final String SET_MINIMUM_SIGMA = "setMinimumSigma";
	/** name of the macro method to set the maximum kernel radius */
	public static final String SET_MAXIMUM_SIGMA = "setMaximumSigma";
	/** name of the macro method to enable/disable the class homogenization */
	public static final String SET_HOMOGENIZATION = "setClassHomogenization";
	/** name of the macro method to set a new classifier */
	public static final String SET_CLASSIFIER = "setClassifier";
	/** name of the macro method to save the feature stack into a file or files */
	public static final String SAVE_FEATURE_STACK = "saveFeatureStack";
	/** name of the macro method to change a class name */
	public static final String CHANGE_CLASS_NAME = "changeClassName";
	/** name of the macro method to set the overlay opacity */
	public static final String SET_OPACITY = "setOpacity";
	/** boolean flag set to true while training */
	boolean trainingFlag = false;
		
	/**
	 * Basic constructor for graphical user interface use
	 */
	public Weka_Segmentation()
	{
		// instantiate segmentation backend
		wekaSegmentation = new WekaSegmentation();

		// Create overlay LUT
		final byte[] red = new byte[256];
		final byte[] green = new byte[256];
		final byte[] blue = new byte[256];
		final int shift = 255 / WekaSegmentation.MAX_NUM_CLASSES;
		
		// assign random colors if # of classes > 5		
		if( WekaSegmentation.MAX_NUM_CLASSES > 5 )
		{
			colors = new Color[ WekaSegmentation.MAX_NUM_CLASSES ];
			Random random = new Random();
			
			// hue for assigning new color ([0.0-1.0])
		    float hue = 0f;
		    // saturation for assigning new color ([0.5-1.0]) 
		    float saturation = 0.5f;
			
			for(int i=0; i<WekaSegmentation.MAX_NUM_CLASSES; i++)
			{
				colors[ i ] = Color.getHSBColor(hue, saturation, 1);
				
				hue += 0.38197f; // golden angle
		        if (hue > 1) 
		            hue -= 1;
		        saturation += 0.38197f; // golden angle
		        if (saturation > 1)
		            saturation -= 1;
		        saturation = 0.5f * saturation + 0.5f;							
			}
		}
			
		
		for(int i = 0 ; i < 256; i++)
		{
			final int colorIndex = i / (shift+1);
			//IJ.log("i = " + i + " color index = " + colorIndex);
			red[i] = (byte) colors[colorIndex].getRed();
			green[i] = (byte) colors[colorIndex].getGreen();
			blue[i] = (byte) colors[colorIndex].getBlue();
		}
		overlayLUT = new LUT(red, green, blue);

		exampleList = new java.awt.List[WekaSegmentation.MAX_NUM_CLASSES];
		addExampleButton = new JButton[WekaSegmentation.MAX_NUM_CLASSES];

		roiOverlay = new RoiListOverlay[WekaSegmentation.MAX_NUM_CLASSES];
		
		trainButton = new JButton("Train classifier");
		trainButton.setToolTipText("Start training the classifier");

		overlayButton = new JButton("Toggle overlay");
		overlayButton.setToolTipText("Toggle between current segmentation and original image");
		overlayButton.setEnabled(false);

		resultButton = new JButton("Create result");
		resultButton.setToolTipText("Generate result image");
		resultButton.setEnabled(false);

		probabilityButton = new JButton("Get probability");
		probabilityButton.setToolTipText("Generate current probability maps");
		probabilityButton.setEnabled(false);

		plotButton = new JButton("Plot result");
		plotButton.setToolTipText("Plot result based on different metrics");
		plotButton.setEnabled(false);

		//newImageButton = new JButton("New image");
		//newImageButton.setToolTipText("Load a new image to segment");

		applyButton = new JButton ("Apply classifier");
		applyButton.setToolTipText("Apply current classifier to a single image or stack");
		applyButton.setEnabled(false);

		loadClassifierButton = new JButton ("Load classifier");
		loadClassifierButton.setToolTipText("Load Weka classifier from a file");

		saveClassifierButton = new JButton ("Save classifier");
		saveClassifierButton.setToolTipText("Save current classifier into a file");
		saveClassifierButton.setEnabled(false);

		loadDataButton = new JButton ("Load data");
		loadDataButton.setToolTipText("Load previous segmentation from an ARFF file");

		saveDataButton = new JButton ("Save data");
		saveDataButton.setToolTipText("Save current segmentation into an ARFF file");
		saveDataButton.setEnabled(false);

		addClassButton = new JButton ("Create new class");
		addClassButton.setToolTipText("Add one more label to mark different areas");

		settingsButton = new JButton ("Settings");
		settingsButton.setToolTipText("Display settings dialog");

		/** The Weka icon image */
		ImageIcon icon = new ImageIcon(Weka_Segmentation.class.getResource("/trainableSegmentation/images/weka.png"));
		wekaButton = new JButton( icon );
		wekaButton.setToolTipText("Launch Weka GUI chooser");

		for(int i = 0; i < wekaSegmentation.getNumOfClasses() ; i++)
		{
			exampleList[i] = new java.awt.List(5);
			exampleList[i].setForeground(colors[i]);
		}
		numOfClasses = wekaSegmentation.getNumOfClasses();

		showColorOverlay = false;
	}

	/** Thread that runs the training. We store it to be able to
	 * to interrupt it from the GUI */
	private Thread trainingTask = null;
		
	/**
	 * Button listener
	 */
	private ActionListener listener = new ActionListener() {

		
	
		public void actionPerformed(final ActionEvent e) {

			final String command = e.getActionCommand();
			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
												
				public void run()
				{
					if(e.getSource() == trainButton)
					{
						runStopTraining(command);						
					}
					else if(e.getSource() == overlayButton){
						// Macro recording
						String[] arg = new String[] {};
						record(TOGGLE_OVERLAY, arg);
						win.toggleOverlay();
					}
					else if(e.getSource() == resultButton){
						// Macro recording
						String[] arg = new String[] {};
						record(GET_RESULT, arg);
						showClassificationImage();
					}
					else if(e.getSource() == probabilityButton){
						// Macro recording
						String[] arg = new String[] {};
						record(GET_PROBABILITY, arg);
						showProbabilityImage();
					}
					else if(e.getSource() == plotButton){
						// Macro recording
						String[] arg = new String[] {};
						record(PLOT_RESULT, arg);
						plotResult();
					}
					//else if(e.getSource() == newImageButton){
					//	loadNewImage();
					//}
					else if(e.getSource() == applyButton){
						applyClassifierToTestData();
					}
					else if(e.getSource() == loadClassifierButton){
						loadClassifier();
					}
					else if(e.getSource() == saveClassifierButton){
						saveClassifier();
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
						win.updateButtonsEnabling();
					}
					else if(e.getSource() == wekaButton){
						// Macro recording
						String[] arg = new String[] {};
						record(LAUNCH_WEKA, arg);
						launchWeka();
					}
					else{
						for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
						{
							if(e.getSource() == exampleList[i])
							{
								deleteSelected(e);
								break;
							}
							if(e.getSource() == addExampleButton[i])
							{
								addExamples(i);
								break;
							}
						}
						win.updateButtonsEnabling();
					}

				}

				
			});
		}
	};
	
	/**
	 * Item listener for the trace lists
	 */
	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(final ItemEvent e) {
			exec.submit(new Runnable() {
				public void run() {
					for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
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
	private class CustomCanvas extends OverlayedImageCanvas
	{
		/**
		 * default serial version UID
		 */
		private static final long serialVersionUID = 1L;

		CustomCanvas(ImagePlus imp)
		{
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

		public void setImagePlus(ImagePlus imp)
		{
			super.imp = imp;
		}
	}

	/**
	 * Custom window to define the Trainable Weka Segmentation GUI
	 */
	private class CustomWindow extends StackWindow
	{
		/** default serial version UID */
		private static final long serialVersionUID = 1L;
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
		
		/** 50% alpha composite */
		final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
		/** 25% alpha composite */
		final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
		/** opacity (in %) of the result overlay image */
		int overlayOpacity = 33;
		/** alpha composite for the result overlay image */
		Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
		/** current segmentation result overlay */
		ImageOverlay resultOverlay;
		
		/** boolean flag set to true when training is complete */
		boolean trainingComplete = false;

		/**
		 * Construct the plugin window
		 * 
		 * @param imp input image
		 */
		CustomWindow(ImagePlus imp)
		{
			super(imp, new CustomCanvas(imp));
			
			final CustomCanvas canvas = (CustomCanvas) getCanvas();

			// add roi list overlays (one per class)
			for(int i = 0; i < WekaSegmentation.MAX_NUM_CLASSES; i++)
			{
				roiOverlay[i] = new RoiListOverlay();
				roiOverlay[i].setComposite( transparency050 );
				((OverlayedImageCanvas)ic).addOverlay(roiOverlay[i]);
			}

			// add result overlay
			resultOverlay = new ImageOverlay();
			resultOverlay.setComposite( overlayAlpha );
			((OverlayedImageCanvas)ic).addOverlay(resultOverlay);

			// Remove the canvas from the window, to add it later
			removeAll();

			setTitle("Trainable Weka Segmentation");

			// Annotations panel
			annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
			annotationsConstraints.gridwidth = 1;
			annotationsConstraints.gridheight = 1;
			annotationsConstraints.gridx = 0;
			annotationsConstraints.gridy = 0;

			annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
			annotationsPanel.setLayout(boxAnnotation);

			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				exampleList[i].addActionListener(listener);
				exampleList[i].addItemListener(itemListener);
				addExampleButton[i] = new JButton("Add to " + wekaSegmentation.getClassLabel(i));
				addExampleButton[i].setToolTipText("Add markings of label '" + wekaSegmentation.getClassLabel(i) + "'");

				annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
				annotationsConstraints.insets = new Insets(5, 5, 6, 6);

				boxAnnotation.setConstraints(addExampleButton[i], annotationsConstraints);
				annotationsPanel.add(addExampleButton[i]);
				annotationsConstraints.gridy++;

				annotationsConstraints.insets = new Insets(0,0,0,0);

				boxAnnotation.setConstraints(exampleList[i], annotationsConstraints);
				annotationsPanel.add(exampleList[i]);
				annotationsConstraints.gridy++;
			}

			// Select first class
			addExampleButton[0].setSelected(true);

			// Add listeners
			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
				addExampleButton[i].addActionListener(listener);
			trainButton.addActionListener(listener);
			overlayButton.addActionListener(listener);
			resultButton.addActionListener(listener);
			probabilityButton.addActionListener(listener);
			plotButton.addActionListener(listener);
			//newImageButton.addActionListener(listener);
			applyButton.addActionListener(listener);
			loadClassifierButton.addActionListener(listener);
			saveClassifierButton.addActionListener(listener);
			loadDataButton.addActionListener(listener);
			saveDataButton.addActionListener(listener);
			addClassButton.addActionListener(listener);
			settingsButton.addActionListener(listener);
			wekaButton.addActionListener(listener);
			
			// add especial listener if the training image is a stack
			if(null != sliceSelector)
			{
				// add adjustment listener to the scroll bar
				sliceSelector.addAdjustmentListener(new AdjustmentListener() 
				{

					public void adjustmentValueChanged(final AdjustmentEvent e) {
						exec.submit(new Runnable() {
							public void run() {							
								if(e.getSource() == sliceSelector)
								{
									//IJ.log("moving scroll");
									displayImage.killRoi();
									drawExamples();
									updateExampleLists();
									if(showColorOverlay)
									{
										updateResultOverlay();
										displayImage.updateAndDraw();
									}
								}

							}
						});

					}
				});

				// mouse wheel listener to update the rois while scrolling
				addMouseWheelListener(new MouseWheelListener() {

					@Override
					public void mouseWheelMoved(final MouseWheelEvent e) {

						exec.submit(new Runnable() {
							public void run() 
							{
								//IJ.log("moving scroll");
								displayImage.killRoi();
								drawExamples();
								updateExampleLists();
								if(showColorOverlay)
								{
									updateResultOverlay();
									displayImage.updateAndDraw();
								}
							}
						});

					}
				});

				// key listener to repaint the display image and the traces
				// when using the keys to scroll the stack
				KeyListener keyListener = new KeyListener() {

					@Override
					public void keyTyped(KeyEvent e) {}

					@Override
					public void keyReleased(final KeyEvent e) {
						exec.submit(new Runnable() {
							public void run() 
							{
								if(e.getKeyCode() == KeyEvent.VK_LEFT ||
										e.getKeyCode() == KeyEvent.VK_RIGHT ||
										e.getKeyCode() == KeyEvent.VK_LESS ||
										e.getKeyCode() == KeyEvent.VK_GREATER ||
										e.getKeyCode() == KeyEvent.VK_COMMA ||
										e.getKeyCode() == KeyEvent.VK_PERIOD)
								{
									//IJ.log("moving scroll");
									displayImage.killRoi();
									updateExampleLists();
									drawExamples();
									if(showColorOverlay)
									{
										updateResultOverlay();
										displayImage.updateAndDraw();
									}
								}
							}
						});

					}

					@Override
					public void keyPressed(KeyEvent e) {}
				};
				// add key listener to the window and the canvas
				addKeyListener(keyListener);
				canvas.addKeyListener(keyListener);
			
			}
			
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
			trainingJPanel.add(probabilityButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(plotButton, trainingConstraints);
			trainingConstraints.gridy++;
			//trainingJPanel.add(newImageButton, trainingConstraints);
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
			optionsJPanel.add(loadClassifierButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(saveClassifierButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(loadDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(saveDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(addClassButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(settingsButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(wekaButton, optionsConstraints);
			optionsConstraints.gridy++;

			// Buttons panel (including training and options)
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsPanel.setLayout(buttonsLayout);
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsConstraints.gridwidth = 1;super.imp = imp;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsPanel.add(trainingJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(optionsJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 1;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.gridheight = 2;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;

			all.add(buttonsPanel, allConstraints);

			allConstraints.gridx++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(canvas, allConstraints);
			
			allConstraints.gridy++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			if(null != sliceSelector)
				all.add(sliceSelector, allConstraints);
			allConstraints.gridy--;

			allConstraints.gridx++;
			allConstraints.anchor = GridBagConstraints.NORTHEAST;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			allConstraints.gridheight = 2;
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
					// Stop any thread from the segmentator
					if(null != trainingTask)
						trainingTask.interrupt();
					wekaSegmentation.shutDownNow();
					exec.shutdownNow();	
					
					for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
						addExampleButton[i].removeActionListener(listener);
					trainButton.removeActionListener(listener);
					overlayButton.removeActionListener(listener);
					resultButton.removeActionListener(listener);
					probabilityButton.removeActionListener(listener);
					plotButton.removeActionListener(listener);
					//newImageButton.removeActionListener(listener);
					applyButton.removeActionListener(listener);
					loadClassifierButton.removeActionListener(listener);
					saveClassifierButton.removeActionListener(listener);
					loadDataButton.removeActionListener(listener);
					saveDataButton.removeActionListener(listener);
					addClassButton.removeActionListener(listener);
					settingsButton.removeActionListener(listener);
					wekaButton.removeActionListener(listener);

					// Set number of classes back to 2
					wekaSegmentation.setNumOfClasses(2);					
				}
			});

			canvas.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					Rectangle r = canvas.getBounds();
					canvas.setDstDimensions(r.width, r.height);
				}
			});			
			
		}

		/**
		 * Get the Weka segmentation object. This tricks allows to 
		 * extract the information from the plugin and use it from
		 * static methods. 
		 * 
		 * @return Weka segmentation data associated to the window.
		 */
		protected WekaSegmentation getWekaSegmentation()
		{
			return wekaSegmentation;
		}
		
		/**
		 * Draw the painted traces on the display image
		 */
		protected void drawExamples()
		{
			final int currentSlice = displayImage.getCurrentSlice();

			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				roiOverlay[i].setColor(colors[i]);
				final ArrayList< Roi > rois = new ArrayList<Roi>();
				for (Roi r : wekaSegmentation.getExamples(i, currentSlice))
				{
					rois.add(r);
					//IJ.log("painted ROI: " + r + " in color "+ colors[i] + ", slice = " + currentSlice);
				}
				roiOverlay[i].setRoi(rois);
			}
			
			displayImage.updateAndDraw();
		}
		
		/**
		 * Update the example lists in the GUI
		 */
		protected void updateExampleLists()
		{
			final int currentSlice = displayImage.getCurrentSlice();

			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				exampleList[i].removeAll();
				for(int j=0; j<wekaSegmentation.getExamples(i, currentSlice).size(); j++)
					exampleList[i].add("trace " + j + " (Z=" + currentSlice+")");
			}
			
		}
		
		protected boolean isToogleEnabled()
		{
			return showColorOverlay;
		}
		
		/**
		 * Get the displayed image. This method can be used to
		 * extract the ROIs from the current image.
		 * 
		 * @return image being displayed in the custom window
		 */
		protected ImagePlus getDisplayImage()
		{
			return this.getImagePlus();
		}
		
		/**
		 * Set the slice selector enable option
		 * @param b true/false to enable/disable the slice selector
		 */
		public void setSliceSelectorEnabled(boolean b)
		{
			if(null != sliceSelector)
				sliceSelector.setEnabled(b);
		}

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
			int classNum = numOfClasses;

			exampleList[classNum] = new java.awt.List(5);
			exampleList[classNum].setForeground(colors[classNum]);

			exampleList[classNum].addActionListener(listener);
			exampleList[classNum].addItemListener(itemListener);
			addExampleButton[classNum] = new JButton("Add to " + wekaSegmentation.getClassLabel(classNum));

			annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
			annotationsConstraints.insets = new Insets(5, 5, 6, 6);

			boxAnnotation.setConstraints(addExampleButton[classNum], annotationsConstraints);
			annotationsPanel.add(addExampleButton[classNum]);
			annotationsConstraints.gridy++;

			annotationsConstraints.insets = new Insets(0,0,0,0);

			boxAnnotation.setConstraints(exampleList[classNum], annotationsConstraints);
			annotationsPanel.add(exampleList[classNum]);
			annotationsConstraints.gridy++;

			// Add listener to the new button
			addExampleButton[classNum].addActionListener(listener);

			numOfClasses++;

			repaintAll();
		}

		/**
		 * Set the image being displayed on the custom canvas
		 * @param imp new image
		 */
		public void setImagePlus(final ImagePlus imp)
		{
			super.imp = imp;
			((CustomCanvas) super.getCanvas()).setImagePlus(imp);
			Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
			((CustomCanvas) super.getCanvas()).setDstDimensions(dim.width, dim.height);
			imp.setWindow(this);
			repaint();
		}
		
		/**
		 * Enable / disable buttons
		 * @param s enabling flag
		 */
		protected void setButtonsEnabled(Boolean s)
		{
			trainButton.setEnabled(s);
			overlayButton.setEnabled(s);
			resultButton.setEnabled(s);
			probabilityButton.setEnabled(s);
			plotButton.setEnabled(s);
			//newImageButton.setEnabled(s);
			applyButton.setEnabled(s);
			loadClassifierButton.setEnabled(s);
			saveClassifierButton.setEnabled(s);
			loadDataButton.setEnabled(s);
			saveDataButton.setEnabled(s);
			addClassButton.setEnabled(s);
			settingsButton.setEnabled(s);
			wekaButton.setEnabled(s);
			for(int i = 0 ; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				exampleList[i].setEnabled(s);
				addExampleButton[i].setEnabled(s);
			}
			setSliceSelectorEnabled(s);
		}
		
		/**
		 * Update buttons enabling depending on the current status of the plugin
		 */
		protected void updateButtonsEnabling()
		{
			// While training, set disable all buttons except the train buttons, 
			// which will be used to stop the training by the user. 
			if( trainingFlag == true )
			{
				setButtonsEnabled( false );
				trainButton.setEnabled( true );	
			}
			else // If the training is not going on
			{
				final boolean classifierExists =  null != wekaSegmentation.getClassifier();

				trainButton.setEnabled( classifierExists );
				applyButton.setEnabled( win.trainingComplete );

				final boolean resultExists = null != classifiedImage &&
											 null != classifiedImage.getProcessor();

				saveClassifierButton.setEnabled( win.trainingComplete );
				overlayButton.setEnabled(resultExists);
				resultButton.setEnabled(resultExists);
				
				plotButton.setEnabled( win.trainingComplete );				
				probabilityButton.setEnabled( win.trainingComplete );

				//newImageButton.setEnabled(true);
				loadClassifierButton.setEnabled(true);
				loadDataButton.setEnabled(true);

				addClassButton.setEnabled(wekaSegmentation.getNumOfClasses() < WekaSegmentation.MAX_NUM_CLASSES);
				settingsButton.setEnabled(true);
				wekaButton.setEnabled(true);

				boolean examplesEmpty = true;
				for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i ++)
					if(exampleList[i].getItemCount() > 0)
					{
						examplesEmpty = false;
						break;
					}
				boolean loadedTrainingData = null != wekaSegmentation.getLoadedTrainingData();

				saveDataButton.setEnabled(!examplesEmpty || loadedTrainingData);

				for(int i = 0 ; i < wekaSegmentation.getNumOfClasses(); i++)
				{
					exampleList[i].setEnabled(true);
					addExampleButton[i].setEnabled(true);
				}
				setSliceSelectorEnabled(true);
			}
		}

		/**
		 * Toggle between overlay and original image with markings
		 */
		void toggleOverlay()
		{
			showColorOverlay = !showColorOverlay;
			//IJ.log("toggle overlay to: " + showColorOverlay);
			if (showColorOverlay && null != classifiedImage)
			{
				updateResultOverlay();
			}
			else
				resultOverlay.setImage(null);

			displayImage.updateAndDraw();
		}

		/**
		 * Set a new result (classified) image
		 * @param classifiedImage new result image
		 */
		protected void setClassfiedImage(ImagePlus classifiedImage) 
		{
			updateClassifiedImage(classifiedImage);	
		}
		
		/**
		 * Update the buttons to add classes with current information
		 */
		public void updateAddClassButtons() 
		{
			int wekaNumOfClasses = wekaSegmentation.getNumOfClasses();
			while (numOfClasses < wekaNumOfClasses)
				win.addClass();
			for (int i = 0; i < numOfClasses; i++)
				addExampleButton[i].setText("Add to " + wekaSegmentation.getClassLabel(i));

			win.updateButtonsEnabling();
			repaintWindow();
		}

		/**
		 * Set the flag to inform the the training has finished or not
		 * 
		 * @param b tranining complete flag
		 */
		void setTrainingComplete(boolean b)
		{
			this.trainingComplete = b;
		}
		
	}// end class CustomWindow

	/**
	 * Plugin run method
	 */
	public void run(String arg)
	{
		//get current image
		if (null == WindowManager.getCurrentImage())
		{
			trainingImage = IJ.openImage();
			if (null == trainingImage) return; // user canceled open dialog
		}
		else
			trainingImage = WindowManager.getCurrentImage().duplicate();


		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			IJ.log("Warning: at least one dimension of the image "  +
					"is larger than 1024 pixels.\n" +
					"Feature stack creation and classifier training " +
					"might take some time depending on your computer.\n");


		//trainingImage.setProcessor("Trainable Weka Segmentation", trainingImage.getProcessor().duplicate().convertToByte(true));
		//wekaSegmentation.loadNewImage(trainingImage);
		/*
		if(trainingImage.getImageStackSize() > 1)
			(new StackConverter(trainingImage)).convertToGray8();
		else
			(new ImageConverter(trainingImage)).convertToGray8();
		*/
		wekaSegmentation.setTrainingImage(trainingImage);
		
		// The display image is a copy of the training image (single image or stack)
		displayImage = trainingImage.duplicate();
		displayImage.setTitle("Trainable Weka Segmentation");

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
	 * Add examples defined by the user to the corresponding list
	 * in the GUI and the example list in the segmentation object.
	 * 
	 * @param i GUI list index
	 */
	private void addExamples(int i)
	{
		//get selected pixels
		final Roi r = displayImage.getRoi();
		if (null == r)
			return;

		// IJ.log("Adding trace to list " + i);
		
		final int n = displayImage.getCurrentSlice();
	
		displayImage.killRoi();
		wekaSegmentation.addExample(i, r, n);
		traceCounter[i]++;
		win.drawExamples();
		win.updateExampleLists();
		// Record
		String[] arg = new String[] {
			Integer.toString(i), 
			Integer.toString(n)	};
		record(ADD_TRACE, arg);
	}


	/**
	 * Update the result image
	 * 
	 * @param classifiedImage new result image
	 */
	public void updateClassifiedImage(ImagePlus classifiedImage)
	{
		this.classifiedImage = classifiedImage;
	}
	
	/**
	 * Update the result image overlay with the corresponding slice
	 */
	public void updateResultOverlay()
	{
		ImageProcessor overlay = classifiedImage.getImageStack().getProcessor(displayImage.getCurrentSlice()).duplicate();

		//IJ.log("updating overlay with result from slice " + displayImage.getCurrentSlice());
		
		double shift = 255.0 / WekaSegmentation.MAX_NUM_CLASSES;
		overlay.multiply(shift+1);
		overlay = overlay.convertToByte(false);
		overlay.setColorModel(overlayLUT);

		win.resultOverlay.setImage(overlay);
	}
	
	/**
	 * Select a list and deselect the others
	 * 
	 * @param e item event (originated by a list)
	 * @param i list index
	 */
	void listSelected(final ItemEvent e, final int i)
	{
		// find the right slice of the corresponding ROI
		
		win.drawExamples();
		displayImage.setColor(Color.YELLOW);

		for(int j = 0; j < wekaSegmentation.getNumOfClasses(); j++)
		{
			if (j == i)
			{
				final Roi newRoi = 
					wekaSegmentation.getExamples(i, displayImage.getCurrentSlice())
							.get(exampleList[i].getSelectedIndex());
				// Set selected trace as current ROI
				newRoi.setImage(displayImage);
				displayImage.setRoi(newRoi);
			}
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
	void deleteSelected(final ActionEvent e)
	{

		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			if (e.getSource() == exampleList[i])
			{
				//delete item from ROI
				int index = exampleList[i].getSelectedIndex();

				// kill Roi from displayed image
				if(displayImage.getRoi().equals( 
						wekaSegmentation.getExamples(i, displayImage.getCurrentSlice()).get(index) ))
					displayImage.killRoi();

				// delete item from the list of ROIs of that class and slice
				wekaSegmentation.deleteExample(i, displayImage.getCurrentSlice(), index);
				//delete item from GUI list
				exampleList[i].remove(index);
				
				// Record
				String[] arg = new String[] {
						Integer.toString(i),
						Integer.toString( displayImage.getCurrentSlice() ),
						Integer.toString(index)};
				record(DELETE_TRACE, arg);
			}

		win.drawExamples();
		win.updateExampleLists();
	}

	/**
	 * Run/stop the classifier training
	 * 
	 * @param command current text of the training button ("Train classifier" or "STOP")
	 */
	void runStopTraining(final String command) 
	{
		// If the training is not going on, we start it
		if (command.equals("Train classifier")) 
		{				
			trainingFlag = true;
			trainButton.setText("STOP");
			final Thread oldTask = trainingTask;
			// Disable rest of buttons until the training has finished
			win.updateButtonsEnabling();

			// Set train button text to STOP
			trainButton.setText("STOP");							

			// Thread to run the training
			Thread newTask = new Thread() {								 
				
				public void run()
				{
					// Wait for the old task to finish
					if (null != oldTask) 
					{
						try { 
							IJ.log("Waiting for old task to finish...");
							oldTask.join(); 
						} 
						catch (InterruptedException ie)	{ /*IJ.log("interrupted");*/ }
					}
				       
					try{
						// Macro recording
						String[] arg = new String[] {};
						record(TRAIN_CLASSIFIER, arg);

						if( wekaSegmentation.trainClassifier() )
						{
							if( this.isInterrupted() )
							{
								//IJ.log("Training was interrupted by the user.");
								wekaSegmentation.shutDownNow();
								win.trainingComplete = false;
								return;
							}
							wekaSegmentation.applyClassifier(false);
							classifiedImage = wekaSegmentation.getClassifiedImage();
							if(showColorOverlay)
								win.toggleOverlay();
							win.toggleOverlay();
							win.trainingComplete = true;
						}
						else
						{
							IJ.log("The traning did not finish.");
							win.trainingComplete = false;
						}
						
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						trainingFlag = false;						
						trainButton.setText("Train classifier");
						win.updateButtonsEnabling();										
						trainingTask = null;
					}
				}
				
			};
			
			//IJ.log("*** Set task to new TASK (" + newTask + ") ***");
			trainingTask = newTask;
			newTask.start();							
		}
		else if (command.equals("STOP")) 							  
		{
			try{
				trainingFlag = false;
				win.trainingComplete = false;
				IJ.log("Training was stopped by the user!");
				win.setButtonsEnabled( false );
				trainButton.setText("Train classifier");
				
				if(null != trainingTask)
					trainingTask.interrupt();
				else
					IJ.log("Error: interrupting training failed becaused the thread is null!");
				
				wekaSegmentation.shutDownNow();
				win.updateButtonsEnabling();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * Display the whole image after classification
	 */
	void showClassificationImage()
	{
		if(null == classifiedImage)
			return;
		final ImagePlus resultImage = classifiedImage.duplicate();
		
		resultImage.setTitle("Classified image");
		
		if(resultImage.getImageStackSize() > 1)
			(new StackConverter(resultImage)).convertToGray8();
		else
			(new ImageConverter(resultImage)).convertToGray8();
		
		resultImage.show();
	}

	/**
	 * Display the current probability maps
	 */
	void showProbabilityImage()
	{
		IJ.showStatus("Calculating probability maps...");
		IJ.log("Calculating probability maps...");
		win.setButtonsEnabled(false);
		try{		
			wekaSegmentation.applyClassifier(true);
		}catch(Exception ex){
			IJ.log("Error while applying classifier! (please send bug report)");
			ex.printStackTrace(); 
			win.updateButtonsEnabling();
			return;
		}
		final ImagePlus probImage = wekaSegmentation.getClassifiedImage();
		if(null != probImage)
		{
			probImage.show();
			IJ.run(probImage, "Stack to Hyperstack...", 
					"order=xyczt(default) channels=" + numOfClasses + 
					" slices=" + displayImage.getImageStackSize() + 
					" frames=1 display=Grayscale");
		}
		win.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
	}
	
	/**
	 * Plot the current result
	 */
	void plotResult()
	{
		IJ.showStatus("Evaluating current data...");
		IJ.log("Evaluating current data...");
		win.setButtonsEnabled(false);
		final Instances data;
		if (wekaSegmentation.getTraceTrainingData() != null)
			data = wekaSegmentation.getTraceTrainingData();
		else
			data = wekaSegmentation.getLoadedTrainingData();
		
		if(null == data)
		{
			IJ.error("Error in plot result", "No data available yet to display results");
			return;
		}
		
		displayGraphs(data, wekaSegmentation.getClassifier());
		win.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
	}

	/**
	 * Display the threshold curve window (for precision/recall, ROC, etc.).
	 *
	 * @param data input instances
	 * @param classifier classifier to evaluate
	 */
	public static void displayGraphs(Instances data, AbstractClassifier classifier)
	{
		ThresholdCurve tc = new ThresholdCurve();

		FastVector predictions = null;
		try {
			final EvaluationUtils eu = new EvaluationUtils();
			predictions = eu.getTestPredictions(classifier, data);
		} catch (Exception e) {
			IJ.log("Error while evaluating data!");
			e.printStackTrace();
			return;
		}

		Instances result = tc.getCurve(predictions);
		ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
		vmc.setName(result.relationName() + " (display only)");
		PlotData2D tempd = new PlotData2D(result);
		tempd.setPlotName(result.relationName());
		tempd.addInstanceNumberAttribute();
		try {
			vmc.addPlot(tempd);
		} catch (Exception e) {
			IJ.log("Error while adding plot to visualization panel!");
			e.printStackTrace();
			return;
		}
		String plotName = vmc.getName();
		JFrame jf = new JFrame("Weka Classifier Visualize: "+plotName);
		jf.setSize(500,400);
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(vmc, BorderLayout.CENTER);
		jf.setVisible(true);
	}
	
	/**
	 * Apply classifier to test data. As it is implemented right now, 
	 * it will use one thread per input image and slice. 
	 */
	public void applyClassifierToTestData()
	{
		// array of files to process
		File[] imageFiles;
		String storeDir = "";

		// create a file chooser for the image files
		String dir = OpenDialog.getLastDirectory();
		if (null == dir)
			dir = OpenDialog.getDefaultDirectory();
		JFileChooser fileChooser = new JFileChooser( dir );
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setMultiSelectionEnabled(true);

		// get selected files or abort if no file has been selected
		int returnVal = fileChooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			imageFiles = fileChooser.getSelectedFiles();
			OpenDialog.setLastDirectory( imageFiles[ 0 ].getParent() );
		} else {
			return;
		}

		boolean showResults = true;
		boolean storeResults = false;

		if (imageFiles.length >= 3) {

			int decision = JOptionPane.showConfirmDialog(null, "You decided to process three or more image " +
					"files. Do you want the results to be stored on the disk instead of opening them in Fiji?",
					"Save results?", JOptionPane.YES_NO_OPTION);

			if (decision == JOptionPane.YES_OPTION) {
				// ask for the directory to store the results
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setMultiSelectionEnabled(false);
				returnVal = fileChooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					storeDir = fileChooser.getSelectedFile().getPath();
				} else {
					return;
				}
				showResults  = false;
				storeResults = true;
			}
		}

		final boolean probabilityMaps;

		int decision = JOptionPane.showConfirmDialog(null, "Create probability maps instead of segmentation?", "Probability maps?", JOptionPane.YES_NO_OPTION);
		if (decision == JOptionPane.YES_OPTION)
			probabilityMaps = true;
		else
			probabilityMaps = false;

		final int numProcessors     = Prefs.getThreads();
		final int numThreads        = Math.min(imageFiles.length, numProcessors);
		final int numFurtherThreads = (int)Math.ceil((double)(numProcessors - numThreads)/imageFiles.length) + 1;

		IJ.log("Processing " + imageFiles.length + " image files in " + numThreads + " thread(s)....");

		win.setButtonsEnabled(false);

		Thread[] threads = new Thread[numThreads];

		class ImageProcessingThread extends Thread {

			final int     numThread;
			final int     numThreads;
			final File[]  imageFiles;
			final boolean storeResults;
			final boolean showResults;
			final String  storeDir;

			public ImageProcessingThread(int numThread, int numThreads,
			                             File[] imageFiles,
			                             boolean storeResults, boolean showResults,
			                             String storeDir) {
				this.numThread     = numThread;
				this.numThreads    = numThreads;
				this.imageFiles    = imageFiles;
				this.storeResults  = storeResults;
				this.showResults   = showResults;
				this.storeDir      = storeDir;
			}

			public void run() {

				for (int i = numThread; i < imageFiles.length; i += numThreads) {
					File file = imageFiles[i];

					ImagePlus testImage = IJ.openImage(file.getPath());

					IJ.log("Processing image " + file.getName() + " in thread " + numThread);

					ImagePlus segmentation = wekaSegmentation.applyClassifier(testImage, numFurtherThreads, probabilityMaps);

					if (showResults && null != segmentation) 
					{
						segmentation.show();
						testImage.show();
					}

					if (storeResults) {
						String filename = storeDir + File.separator + file.getName();
						IJ.log("Saving results to " + filename);
						IJ.save(segmentation, filename);
						segmentation.close();
						testImage.close();
					}
				}
			}
		}

		// start threads
		for (int i = 0; i < numThreads; i++) {

			threads[i] = new ImageProcessingThread(i, numThreads, imageFiles, storeResults, showResults, storeDir);
			// Record
			String[] arg = new String[] {
				imageFiles[i].getParent(),
				imageFiles[i].getName(),
				"showResults=" + showResults,
				"storeResults=" + storeResults,
				"probabilityMaps="+ probabilityMaps,
				storeDir	};
			record(APPLY_CLASSIFIER, arg);
			threads[i].start();
		}

		// join all threads
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}

		win.updateButtonsEnabling();
	}

	/**
	 * Load a Weka classifier from a file
	 */
	public void loadClassifier()
	{
		OpenDialog od = new OpenDialog( "Choose Weka classifier file", "" );
		if (od.getFileName()==null)
			return;
		IJ.log("Loading Weka classifier from " + od.getDirectory() + od.getFileName() + "...");
		// Record
		String[] arg = new String[] { od.getDirectory() + od.getFileName() };
		record(LOAD_CLASSIFIER, arg);

		win.setButtonsEnabled(false);

		final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();


		// Try to load Weka model (classifier and train header)
		if( false == wekaSegmentation.loadClassifier(od.getDirectory() + od.getFileName()) )
		{
			IJ.error("Error when loading Weka classifier from file");
			IJ.log("Error: classifier could not be loaded.");
			win.updateButtonsEnabling();
			return;
		}

		IJ.log("Read header from " + od.getDirectory() + od.getFileName() + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")");

		if(wekaSegmentation.getTrainHeader().numAttributes() < 1)
		{
			IJ.error("Error", "No attributes were found on the model header");
			wekaSegmentation.setClassifier(oldClassifier);
			win.updateButtonsEnabling();
			return;
		}

		// Set the flag of training complete to true
		win.trainingComplete = true;
		
		// update GUI
		win.updateAddClassButtons();

		win.trainingComplete = true;
		IJ.log("Loaded " + od.getDirectory() + od.getFileName());
	}

	/**
	 * Load a Weka model (classifier) from a file
	 * @param filename complete path and file name
	 * @return classifier
	 */
	public static AbstractClassifier readClassifier(String filename)
	{
		AbstractClassifier cls = null;
		// deserialize model
		try {
			cls = (AbstractClassifier) SerializationHelper.read(filename);
		} catch (Exception e) {
			IJ.log("Error when loading classifier from " + filename);
			e.printStackTrace();
		}
		return cls;
	}

	/**
	 * Save current classifier into a file
	 */
	public void saveClassifier()
	{
		SaveDialog sd = new SaveDialog("Save model as...", "classifier",".model");
		if (sd.getFileName()==null)
			return;

		// Record
		String[] arg = new String[] { sd.getDirectory() + sd.getFileName() };
		record(SAVE_CLASSIFIER, arg);
		
		if( false == wekaSegmentation.saveClassifier(sd.getDirectory() + sd.getFileName()) )
		{
			IJ.error("Error while writing classifier into a file");
			return;
		}
	}

	/**
	 * Write classifier into a file
	 *
	 * @param classifier classifier
	 * @param trainHeader train header containing attribute and class information
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public static boolean saveClassifier(
			AbstractClassifier classifier,
			Instances trainHeader,
			String filename)
	{
		File sFile = null;
		boolean saveOK = true;


		IJ.log("Saving model to file...");

		try {
			sFile = new File(filename);
			OutputStream os = new FileOutputStream(sFile);
			if (sFile.getName().endsWith(".gz"))
			{
				os = new GZIPOutputStream(os);
			}
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
			objectOutputStream.writeObject(classifier);
			if (trainHeader != null)
				objectOutputStream.writeObject(trainHeader);
			objectOutputStream.flush();
			objectOutputStream.close();
		}
		catch (Exception e)
		{
			IJ.error("Save Failed", "Error when saving classifier into a file");
			saveOK = false;
			e.printStackTrace();
		}
		if (saveOK)
			IJ.log("Saved model into the file " + filename);

		return saveOK;
	}

	/**
	 * Write classifier into a file
	 *
	 * @param cls classifier
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public static boolean writeClassifier(AbstractClassifier cls, String filename)
	{
		try {
			SerializationHelper.write(filename, cls);
		} catch (Exception e) {
			IJ.log("Error while writing classifier into a file");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Load a new image to segment
	 */
	public void loadNewImage()
	{
		OpenDialog od = new OpenDialog("Choose new image", OpenDialog.getLastDirectory());
		if (od.getFileName()==null)
			return;

		win.setButtonsEnabled(false);

		IJ.log("Loading image " + od.getDirectory() + od.getFileName() + "...");

		ImagePlus newImage = new ImagePlus(od.getDirectory() + od.getFileName());

		if( false == wekaSegmentation.loadNewImage( newImage ) )
		{
			IJ.error("Error while loading new image!");
			win.updateButtonsEnabling();
			return;
		}

		// Remove traces from the lists and ROI overlays
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i ++)
		{
			exampleList[i].removeAll();
			roiOverlay[i].setRoi(null);
		}

		// Updating image
		displayImage = new ImagePlus();
		displayImage.setProcessor("Trainable Weka Segmentation", trainingImage.getProcessor().duplicate());

		// Remove current classification result image
		win.resultOverlay.setImage(null);

		win.toggleOverlay();

		// Update GUI
		win.setImagePlus(displayImage);
		displayImage.updateAndDraw();
		win.pack();

		win.updateButtonsEnabling();
	}

	/**
	 * Load previously saved data
	 */
	public void loadTrainingData()
	{
		OpenDialog od = new OpenDialog("Choose data file", OpenDialog.getLastDirectory(), "data.arff");
		if (od.getFileName()==null)
			return;
		
		// Macro recording
		String[] arg = new String[] { od.getDirectory() + od.getFileName() };
		record(LOAD_DATA, arg);
		
		win.setButtonsEnabled(false);
		IJ.log("Loading data from " + od.getDirectory() + od.getFileName() + "...");
		wekaSegmentation.loadTrainingData(od.getDirectory() + od.getFileName());
		win.updateButtonsEnabling();
	}

	/**
	 * Save training model into a file
	 */
	public void saveTrainingData()
	{
		SaveDialog sd = new SaveDialog("Choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;

		// Macro recording
		String[] arg = new String[] { sd.getDirectory() + sd.getFileName() };
		record(SAVE_DATA, arg);
		
		win.setButtonsEnabled(false);
		
		if(false == wekaSegmentation.saveData(sd.getDirectory() + sd.getFileName()))
			IJ.showMessage("There is no data to save");
		
		win.updateButtonsEnabling();
	}


	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass()
	{
		if(wekaSegmentation.getNumOfClasses() == WekaSegmentation.MAX_NUM_CLASSES)
		{
			IJ.showMessage("Trainable Weka Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

		String inputName = JOptionPane.showInputDialog("Please input a new label name");

		if(null == inputName)
			return;


		if (null == inputName || 0 == inputName.length()) 
		{
			IJ.error("Invalid name for class");
			return;
		}
		inputName = inputName.trim();

		if (0 == inputName.toLowerCase().indexOf("add to "))
			inputName = inputName.substring(7);

		// Add new name to the list of labels
		wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
		wekaSegmentation.addClass();

		// Add new class label and list
		win.addClass();

		repaintWindow();
		
		// Macro recording
		String[] arg = new String[] { inputName };
		record(CREATE_CLASS, arg);
	}

	/**
	 * Repaint whole window
	 */
	private void repaintWindow()
	{
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
	 * Call the Weka chooser
	 */
	public static void launchWeka()
	{
		GUIChooser chooser = new GUIChooser();
		for (WindowListener wl : chooser.getWindowListeners())
		{
			chooser.removeWindowListener(wl);
		}
		chooser.setVisible(true);
	}

	/**
	 * Show advanced settings dialog
	 *
	 * @return false when canceled
	 */
	public boolean showSettingsDialog()
	{
		GenericDialogPlus gd = new GenericDialogPlus("Segmentation settings");

		final boolean[] oldEnableFeatures = wekaSegmentation.getEnabledFeatures();

		gd.addMessage("Training features:");
		final int rows = (int)Math.round(FeatureStack.availableFeatures.length/2.0);
		gd.addCheckboxGroup(rows, 2, FeatureStack.availableFeatures, oldEnableFeatures);

		if(wekaSegmentation.getLoadedTrainingData() != null)
		{
			final Vector<Checkbox> v = gd.getCheckboxes();
			for(Checkbox c : v)
				c.setEnabled(false);
			gd.addMessage("WARNING: no features are selectable while using loaded data");
		}

		// Expected membrane thickness
		gd.addNumericField("Membrane thickness:", wekaSegmentation.getMembraneThickness(), 0);
		// Membrane patch size
		gd.addNumericField("Membrane patch size:", wekaSegmentation.getMembranePatchSize(), 0);
		// Field of view
		gd.addNumericField("Minimum sigma:", wekaSegmentation.getMinimumSigma(), 1);
		gd.addNumericField("Maximum sigma:", wekaSegmentation.getMaximumSigma(), 1);

		if(wekaSegmentation.getLoadedTrainingData() != null)
		{
			for(int i = 0; i < 4; i++)
				((TextField) gd.getNumericFields().get( i )).setEnabled(false);
		}

		gd.addMessage("Classifier options:");

		// Add Weka panel for selecting the classifier and its options
		GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();
		PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);
		m_ClassifierEditor.setClassType(Classifier.class);
		m_ClassifierEditor.setValue(wekaSegmentation.getClassifier());
					
		// add classifier editor panel
		gd.addComponent( m_CEPanel,  GridBagConstraints.HORIZONTAL , 1 );
		
		Object c = (Object)m_ClassifierEditor.getValue();
	    String originalOptions = "";
	    String originalClassifierName = c.getClass().getName();
	    if (c instanceof OptionHandler) 
	    {
	    	originalOptions = Utils.joinOptions(((OptionHandler)c).getOptions());
	    }		

		gd.addMessage("Class names:");
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			gd.addStringField("Class "+(i+1), wekaSegmentation.getClassLabel(i), 15);

		gd.addMessage("Advanced options:");
		gd.addCheckbox("Homogenize classes", wekaSegmentation.doHomogenizeClasses());
		gd.addButton("Save feature stack", new SaveFeatureStackButtonListener("Select location to save feature stack", wekaSegmentation.getFeatureStackArray()));
		gd.addSlider("Result overlay opacity", 0, 100, win.overlayOpacity);
		gd.addHelp("http://fiji.sc/Trainable_Weka_Segmentation");

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
			{
				featuresChanged = true;
				// Macro recording
				record(SET_FEATURE, new String[]{ FeatureStack.availableFeatures[ i ] + "=" + newEnableFeatures[ i ] });
			}
		}
		if(featuresChanged)
		{
			wekaSegmentation.setEnabledFeatures(newEnableFeatures);
		}		

		// Membrane thickness
		final int newThickness = (int) gd.getNextNumber();
		if(newThickness != wekaSegmentation.getMembraneThickness())
		{
			featuresChanged = true;
			wekaSegmentation.setMembraneThickness(newThickness);
			// Macro recording
			record(SET_MEMBRANE_THICKNESS, new String[] { Integer.toString( newThickness )});
		}
		// Membrane patch size
		final int newPatch = (int) gd.getNextNumber();
		if(newPatch != wekaSegmentation.getMembranePatchSize())
		{
			featuresChanged = true;
			// Macro recording
			record(SET_MEMBRANE_PATCH, new String[] { Integer.toString( newPatch )});
			wekaSegmentation.setMembranePatchSize(newPatch);
		}
		// Field of view (minimum and maximum sigma/radius for the filters)
		final float newMinSigma = (float) gd.getNextNumber();
		if(newMinSigma != wekaSegmentation.getMinimumSigma() && newMinSigma > 0)
		{
			featuresChanged = true;
			// Macro recording
			record(SET_MINIMUM_SIGMA, new String[] { Float.toString( newMinSigma )});
			wekaSegmentation.setMinimumSigma(newMinSigma);
		}

		final float newMaxSigma = (float) gd.getNextNumber();
		if(newMaxSigma != wekaSegmentation.getMaximumSigma() && newMaxSigma >= wekaSegmentation.getMinimumSigma())
		{
			featuresChanged = true;
			// Macro recording
			record(SET_MAXIMUM_SIGMA, new String[] { Float.toString( newMaxSigma )});
			wekaSegmentation.setMaximumSigma(newMaxSigma);
		}
		if(wekaSegmentation.getMinimumSigma() > wekaSegmentation.getMaximumSigma())
		{
			IJ.error("Error in the field of view parameters: they will be reset to default values");
			wekaSegmentation.setMinimumSigma(0f);
			wekaSegmentation.setMaximumSigma(16f);
		}
				
		// Set classifier and options
		c = (Object)m_ClassifierEditor.getValue();
	    String options = "";
	    final String[] optionsArray = ((OptionHandler)c).getOptions();
	    if (c instanceof OptionHandler) 
	    {
	      options = Utils.joinOptions( optionsArray );
	    }
	    //System.out.println("Classifier after choosing: " + c.getClass().getName() + " " + options);
	    if(originalClassifierName.equals( c.getClass().getName() ) == false
	    		|| originalOptions.equals( options ) == false)
	    {
	    	AbstractClassifier cls;
	    	try{
	    		cls = (AbstractClassifier) (c.getClass().newInstance());
	    		cls.setOptions( optionsArray );
	    	}
	    	catch(Exception ex)
	    	{
	    		ex.printStackTrace();
	    		return false;
	    	}
	    	
	    	// Assing new classifier
	    	wekaSegmentation.setClassifier( cls );
	    	
	    	// Set the training flag to false  
	    	win.trainingComplete = false;
	    	
	    	// Macro recording
			record(SET_CLASSIFIER, new String[] { c.getClass().getName(), options} );
	    		    	
	    	IJ.log("Current classifier: " + c.getClass().getName() + " " + options);
	    }
				
		boolean classNameChanged = false;
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
		{
			String s = gd.getNextString();
			if (null == s || 0 == s.length()) {
				IJ.log("Invalid name for class " + (i+1));
				continue;
			}
			s = s.trim();
			if(!s.equals(wekaSegmentation.getClassLabel(i)))
			{
				if (0 == s.toLowerCase().indexOf("add to "))
					s = s.substring(7);

				wekaSegmentation.setClassLabel(i, s);
				classNameChanged = true;
				addExampleButton[i].setText("Add to " + s);
				// Macro recording
				record(CHANGE_CLASS_NAME, new String[]{ Integer.toString(i), s});
			}
		}

		// Update flag to homogenize number of class instances		
		final boolean homogenizeClasses = gd.getNextBoolean();
		if( wekaSegmentation.doHomogenizeClasses() != homogenizeClasses )
		{
			wekaSegmentation.setDoHomogenizeClasses( homogenizeClasses );
			// Macro recording
			record(SET_HOMOGENIZATION, new String[] { Boolean.toString( homogenizeClasses )});
		}
		
		// Update result overlay alpha
		final int newOpacity = (int) gd.getNextNumber();
		if( newOpacity != win.overlayOpacity )
		{
			win.overlayOpacity = newOpacity;
			win.overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, win.overlayOpacity / 100f);
			win.resultOverlay.setComposite(win.overlayAlpha);

			// Macro recording
			record(SET_OPACITY, new String[] { Integer.toString( win.overlayOpacity )});
			
			if( showColorOverlay )
				displayImage.updateAndDraw();
		}


		// If there is a change in the class names,
		// the data set (instances) must be updated.
		if(classNameChanged)
		{
			// Pack window to update buttons
			win.pack();
		}

		// Update feature stack if necessary
		if(featuresChanged)
		{			
			// Force features to be updated
			wekaSegmentation.setFeaturesDirty();
		}
		else	// This checks if the feature stacks were updated while using the save feature stack button
			if(wekaSegmentation.getFeatureStackArray().isEmpty() == false 
					&& wekaSegmentation.getFeatureStackArray().getReferenceSliceIndex() != -1)
				wekaSegmentation.setUpdateFeatures(false);

		return true;
	}

	// Quite of a hack from Johannes Schindelin:
	// use reflection to insert classifiers, since there is no other method to do that...
	static {
		try {
			IJ.showStatus("Loading Weka properties...");
			IJ.log("Loading Weka properties...");
			Field field = GenericObjectEditor.class.getDeclaredField("EDITOR_PROPERTIES");
			field.setAccessible(true);
			Properties editorProperties = (Properties)field.get(null);
			String key = "weka.classifiers.Classifier";
			String value = editorProperties.getProperty(key);
			value += ",hr.irb.fastRandomForest.FastRandomForest";
			editorProperties.setProperty(key, value);
			//new Exception("insert").printStackTrace();
			//System.err.println("value: " + value);
		} catch (Exception e) {
			IJ.error("Could not insert my own cool classifiers!");
		}
	}

	/**
	 * Button listener class to handle the button action from the
	 * settings dialog to set the Weka classifier parameters
	 */
	static class ClassifierSettingsButtonListener implements ActionListener
	{
		AbstractClassifier classifier;

		/**
		 * Build the button listener for selecting the classifier
		 * @param classifier current classifier object
		 */
		public ClassifierSettingsButtonListener(AbstractClassifier classifier)
		{
			this.classifier = classifier;
		}

		/**
		 * Control the action when clicking on the classifier settings box.
		 * It displays the Weka dialog for selecting a classifier.
		 */
		public void actionPerformed(ActionEvent e)
		{
			try {
				GenericObjectEditor.registerEditors();
				GenericObjectEditor ce = new GenericObjectEditor(true);
				ce.setClassType(weka.classifiers.Classifier.class);
				Object initial = classifier;

			 	ce.setValue(initial);

				PropertyDialog pd = new PropertyDialog((Frame) null, ce, 100, 100);
				pd.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						PropertyEditor pe = ((PropertyDialog)e.getSource()).getEditor();
						Object c = (Object)pe.getValue();
						String options = "";
						if (c instanceof OptionHandler) {
							options = Utils.joinOptions(((OptionHandler)c).getOptions());
						}
						IJ.log(c.getClass().getName() + " " + options);
						return;
					}
				});
				pd.setVisible(true);
			} catch (Exception ex) {
				ex.printStackTrace();
				IJ.error(ex.getMessage());
			}
		}
	}

	/**
	 * Button listener class to handle the button action from the
	 * settings dialog to save the feature stack
	 */
	static class SaveFeatureStackButtonListener implements ActionListener
	{
		String title;
		TextField text;
		FeatureStackArray featureStackArray;

		/**
		 * Construct a listener for the save feature stack button
		 * 
		 * @param title save dialog title
		 * @param featureStackArray array of feature stacks to save
		 */
		public SaveFeatureStackButtonListener(String title, FeatureStackArray featureStackArray)
		{
			this.title = title;
			this.featureStackArray = featureStackArray;
		}

		/**
		 * Method to run when pressing the save feature stack button
		 */
		public void actionPerformed(ActionEvent e)
		{		
			SaveDialog sd = new SaveDialog(title, "feature-stack", ".tif");
			final String dir = sd.getDirectory();
			final String fileWithExt = sd.getFileName();

			if(null == dir || null == fileWithExt)
				return;

			if(featureStackArray.isEmpty() || featureStackArray.getReferenceSliceIndex() == -1)
			{
				IJ.showStatus("Creating feature stack...");
				IJ.log("Creating feature stack...");
				featureStackArray.updateFeaturesMT();
			}
			
			for(int i=0; i<featureStackArray.getSize(); i++)
			{
				final String fileName = dir + fileWithExt.substring(0, fileWithExt.length()-4) 
										+ String.format("%04d", (i+1)) + ".tif";
				if(false == this.featureStackArray.get(i).saveStackAsTiff(fileName))
				{
					IJ.error("Error", "Feature stack could not be saved");
					return;
				}

				IJ.log("Saved feature stack for slice " + (i+1) + " as " + fileName);
			}
			
			// macro recording
			record(SAVE_FEATURE_STACK, new String[]{ dir, fileWithExt });
		}
	}	

	/* **********************************************************
	 * Macro recording related methods
	 * *********************************************************/

	/**
	 * Macro-record a specific command. The command names match the static 
	 * methods that reproduce that part of the code.
	 * 
	 * @param command name of the command including package info
	 * @param args set of arguments for the command
	 */
	public static void record(String command, String... args) 
	{
		command = "call(\"trainableSegmentation.Weka_Segmentation." + command;
		for(int i = 0; i < args.length; i++)
			command += "\", \"" + args[i];
		command += "\");\n";
		if(Recorder.record)
			Recorder.recordString(command);
	}
	
	/**
	 * Add the current ROI to a specific class and slice. 
	 * 
	 * @param classNum string representing the class index
	 * @param nSlice string representing the slice number
	 */
	public static void addTrace(
			String classNum,
			String nSlice)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			final Roi roi = win.getDisplayImage().getRoi();
			wekaSegmentation.addExample(Integer.parseInt(classNum), 
					roi, Integer.parseInt(nSlice));
			win.getDisplayImage().killRoi();
			win.drawExamples();
			win.updateExampleLists();
		}
	}

	/**
	 * Delete a specific ROI from the list of a specific class and slice
	 * 
	 * @param classNum string representing the class index
	 * @param nSlice string representing the slice number
	 * @param index string representing the index of the trace to remove
	 */
	public static void deleteTrace(
			String classNum,
			String nSlice,
			String index)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			wekaSegmentation.deleteExample(Integer.parseInt(classNum),
					Integer.parseInt(nSlice),
					Integer.parseInt(index) );
			win.getDisplayImage().killRoi();
			win.drawExamples();
			win.updateExampleLists();
		}
	}

	/**
	 * Train the current classifier
	 */
	public static void trainClassifier()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			// Disable buttons until the training has finished
			win.setButtonsEnabled(false);

			win.setTrainingComplete(false);

			if( wekaSegmentation.trainClassifier() )
			{
				win.setTrainingComplete(true);
				wekaSegmentation.applyClassifier(false);
				win.setClassfiedImage( wekaSegmentation.getClassifiedImage() );
				if(win.isToogleEnabled())
					win.toggleOverlay();
				win.toggleOverlay();
			}
			win.updateButtonsEnabling();		
		}
	}

	/**
	 * Get the current result (labeled image)
	 */
	public static void getResult()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			final ImagePlus classifiedImage =  wekaSegmentation.getClassifiedImage();
			if(null == classifiedImage)
				return;
			final ImagePlus resultImage = classifiedImage.duplicate();

			resultImage.setTitle("Classified image");

			if(resultImage.getImageStackSize() > 1)
				(new StackConverter(resultImage)).convertToGray8();
			else
				(new ImageConverter(resultImage)).convertToGray8();

			resultImage.show();
		}
	}

	
	/**
	 * Display the current probability maps
	 */
	public static void getProbability()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			IJ.showStatus("Calculating probability maps...");
			IJ.log("Calculating probability maps...");
			win.setButtonsEnabled(false);
			wekaSegmentation.applyClassifier(true);
			final ImagePlus probImage = wekaSegmentation.getClassifiedImage();
			if(null != probImage)
			{
				probImage.setOpenAsHyperStack( true );				
				probImage.show();
			}
			win.updateButtonsEnabling();
			IJ.showStatus("Done.");
			IJ.log("Done");
		}
	}

	/**
	 * Plot the current result (threshold curves)
	 */
	public static void plotResultGraphs()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			IJ.showStatus("Evaluating current data...");
			IJ.log("Evaluating current data...");
			win.setButtonsEnabled(false);
			final Instances data;
			if (wekaSegmentation.getTraceTrainingData() != null)
				data = wekaSegmentation.getTraceTrainingData();
			else
				data = wekaSegmentation.getLoadedTrainingData();

			if(null == data)
			{
				IJ.error("Error in plot result", "No data available yet to display results");
				return;
			}

			displayGraphs(data, wekaSegmentation.getClassifier());
			win.updateButtonsEnabling();
			IJ.showStatus("Done.");
			IJ.log("Done");
		}
	}

	/**
	 * Apply current classifier to specific image (2D or stack)
	 * 
	 * @param dir input image directory path
	 * @param fileName input image name
	 * @param showResultsFlag string containing the boolean flag to display results
	 * @param storeResultsFlag string containing the boolean flag to store result in a directory
	 * @param probabilityMapsFlag string containing the boolean flag to calculate probabilities instead of a binary result
	 * @param storeDir directory to store the results
	 */
	public static void applyClassifier(
			String dir,
			String fileName,
			String showResultsFlag,
			String storeResultsFlag,
			String probabilityMapsFlag,
			String storeDir)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			ImagePlus testImage = IJ.openImage( dir +"/"+ fileName );

			if(null == testImage)
			{
				IJ.log("Error: " + dir +"/"+ fileName + " could not be opened");
				return;
			}

			boolean probabilityMaps = probabilityMapsFlag.contains("true");
			boolean storeResults = storeResultsFlag.contains("true");
			boolean showResults = showResultsFlag.contains("true");

			IJ.log("Processing image " + dir + "/" + fileName );

			ImagePlus segmentation = wekaSegmentation.applyClassifier(testImage, 0, probabilityMaps);

			if (showResults) 
			{
				segmentation.show();
				testImage.show();
			}

			if (storeResults) 
			{
				String filename = storeDir + File.separator + fileName;
				IJ.log("Saving results to " + filename);
				IJ.save(segmentation, filename);
				segmentation.close();
				testImage.close();
			}
		}
	}

	/**
	 * Toggle current result overlay image
	 */
	public static void toggleOverlay()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			win.toggleOverlay();
		}
	}

	/**
	 * Load a new classifier
	 * 
	 * @param newClassifierPathName classifier file name with complete path
	 */
	public static void loadClassifier(String newClassifierPathName)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			IJ.log("Loading Weka classifier from " + newClassifierPathName + "...");

			win.setButtonsEnabled(false);

			final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();

			// Try to load Weka model (classifier and train header)
			if( false == wekaSegmentation.loadClassifier(newClassifierPathName) )
			{
				IJ.error("Error when loading Weka classifier from file");
				win.updateButtonsEnabling();
				return;
			}

			IJ.log("Read header from " + newClassifierPathName + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")");

			if(wekaSegmentation.getTrainHeader().numAttributes() < 1)
			{
				IJ.error("Error", "No attributes were found on the model header");
				wekaSegmentation.setClassifier(oldClassifier);
				win.updateButtonsEnabling();
				return;
			}

			// update GUI
			win.updateAddClassButtons();

			IJ.log("Loaded " + newClassifierPathName);
		}
	}
	
	/**
	 * Save current classifier into a file
	 * 
	 * @param classifierPathName complete path name for the classifier file
	 */
	public static void saveClassifier( String classifierPathName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			if( false == wekaSegmentation.saveClassifier( classifierPathName ) )
			{
				IJ.error("Error while writing classifier into a file");
				return;
			}
		}
	}

	/**
	 * Load training data from file
	 * 
	 * @param arffFilePathName complete path name of the ARFF file
	 */
	public static void loadData(String arffFilePathName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			win.setButtonsEnabled(false);
			IJ.log("Loading data from " + arffFilePathName + "...");
			wekaSegmentation.loadTrainingData( arffFilePathName );
			win.updateButtonsEnabling();
		}
	}
	
	/**
	 * Save training data into an ARFF file
	 * 
	 * @param arffFilePathName complete path name of the ARFF file
	 */
	public static void saveData(String arffFilePathName)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			if(false == wekaSegmentation.saveData( arffFilePathName ))
				IJ.showMessage("There is no data to save");
		}
	}

	/**
	 * Create a new class 
	 * 
	 * @param inputName new class name
	 */
	public static void createNewClass( String inputName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			if (null == inputName || 0 == inputName.length()) 
			{
				IJ.error("Invalid name for class");
				return;
			}
			inputName = inputName.trim();

			if (0 == inputName.toLowerCase().indexOf("add to "))
				inputName = inputName.substring(7);

			// Add new name to the list of labels
			wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
			wekaSegmentation.addClass();

			// Add new class label and list
			win.addClass();

			win.updateAddClassButtons();
		}
	}


	/**
	 * Set membrane thickness for current feature stack
	 * 
	 * @param newThicknessStr new membrane thickness (in pixel units)
	 */
	public static void setMembraneThickness(String newThicknessStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final int newThickness = Integer.parseInt(newThicknessStr);		
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			if( newThickness != wekaSegmentation.getMembraneThickness() )
				wekaSegmentation.setFeaturesDirty();
			wekaSegmentation.setMembraneThickness(newThickness);
		}
	}
	
	/**
	 * Set a new membrane patch size for current feature stack
	 * 
	 * @param newPatchSizeStr new patch size (in pixel units)
	 */
	public static void setMembranePatchSize(String newPatchSizeStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			int newPatchSize = Integer.parseInt(newPatchSizeStr);
			if( newPatchSize  != wekaSegmentation.getMembranePatchSize() )
				wekaSegmentation.setFeaturesDirty();
			wekaSegmentation.setMembranePatchSize( newPatchSize );
		}
	}
	
	/**
	 * Set a new minimum radius for the feature filters
	 * 
	 * @param newMinSigmaStr new minimum radius (in float pixel units)
	 */
	public static void setMinimumSigma(String newMinSigmaStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			float newMinSigma = Float.parseFloat(newMinSigmaStr);
			if(newMinSigma  != wekaSegmentation.getMinimumSigma() && newMinSigma > 0)
			{
				wekaSegmentation.setFeaturesDirty();
				wekaSegmentation.setMinimumSigma(newMinSigma);
			}
			if(wekaSegmentation.getMinimumSigma() >= wekaSegmentation.getMaximumSigma())
			{
				IJ.error("Error in the field of view parameters: they will be reset to default values");
				wekaSegmentation.setMinimumSigma(0f);
				wekaSegmentation.setMaximumSigma(16f);
			}
		}
	}
	
	/**
	 * Set a new maximum radius for the feature filters
	 * 
	 * @param newMaxSigmaStr new maximum radius (in float pixel units)
	 */
	public static void setMaximumSigma(String newMaxSigmaStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			float newMaxSigma = Float.parseFloat(newMaxSigmaStr);
			if(newMaxSigma  != wekaSegmentation.getMaximumSigma() && newMaxSigma > wekaSegmentation.getMinimumSigma())
			{
				wekaSegmentation.setFeaturesDirty();
				wekaSegmentation.setMaximumSigma(newMaxSigma);
			}
			if(wekaSegmentation.getMinimumSigma() >= wekaSegmentation.getMaximumSigma())
			{
				IJ.error("Error in the field of view parameters: they will be reset to default values");
				wekaSegmentation.setMinimumSigma(0f);
				wekaSegmentation.setMaximumSigma(16f);
			}
		}
	}

	/**
	 * Set the class homogenization flag for training
	 * 
	 * @param flagStr true/false if you want to balance the number of samples per class before training
	 */
	public static void setClassHomogenization(String flagStr)
	{		
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			boolean flag = Boolean.parseBoolean(flagStr);
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			wekaSegmentation.setHomogenizeClasses(flag);
		}
	}
	
	/**
	 * Set classifier for current segmentation
	 * 
	 * @param classifierName classifier name with complete package information
	 * @param options classifier options
	 */
	public static void setClassifier(String classifierName, String options)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			try {
				AbstractClassifier cls = (AbstractClassifier)( Class.forName(classifierName).newInstance() );
				cls.setOptions( options.split(" "));
				wekaSegmentation.setClassifier(cls);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save current feature stack(s)
	 * 
	 * @param dir directory to save the stack(s)
	 * @param fileWithExt file name with extension for the file(s)
	 */
	public static void saveFeatureStack(String dir, String fileWithExt)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();
			final FeatureStackArray featureStackArray = wekaSegmentation.getFeatureStackArray();
			if(featureStackArray.isEmpty())
			{
				featureStackArray.updateFeaturesMT();
			}

			if(null == dir || null == fileWithExt)
				return;

			for(int i=0; i<featureStackArray.getSize(); i++)
			{
				final String fileName = dir + fileWithExt.substring(0, fileWithExt.length()-4) 
				+ String.format("%04d", (i+1)) + ".tif";
				if(false == featureStackArray.get(i).saveStackAsTiff(fileName))
				{
					IJ.error("Error", "Feature stack could not be saved");
					return;
				}

				IJ.log("Saved feature stack for slice " + (i+1) + " as " + fileName);
			}
		}
	}
	
	/**
	 * Change a class name
	 * 
	 * @param classIndex index of the class to change
	 * @param className new class name
	 */
	public static void changeClassName(String classIndex, String className)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			int classNum = Integer.parseInt(classIndex);
			wekaSegmentation.setClassLabel(classNum, className);
			win.updateAddClassButtons();
			win.pack();
		}
	}
	
	/**
	 * Enable/disable a single feature of the feature stack(s)
	 * 
	 * @param feature name of the feature + "=" true/false to enable/disable
	 */
	public static void setFeature(String feature)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

			int index = feature.indexOf("=");
			String featureName = feature.substring(0, index);
			boolean featureValue = feature.contains("true");

			boolean[] enabledFeatures = wekaSegmentation.getEnabledFeatures();
			boolean forceUpdate = false;
			for(int i=0; i<enabledFeatures.length; i++)
			{
				if(FeatureStack.availableFeatures[i].contains( featureName ))
				{	
					if(featureValue != enabledFeatures[i])
					{
						enabledFeatures[i] = featureValue;
						forceUpdate = true;
					}
				}
			}
			wekaSegmentation.setEnabledFeatures(enabledFeatures);

			if(forceUpdate)
			{
				// Force features to be updated
				wekaSegmentation.setFeaturesDirty();
			}
		}
	}	
	
	/**
	 * Set overlay opacity
	 * @param newOpacity string containing the new opacity value (integer 0-100)
	 */
	public static void setOpacity( String newOpacity )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			win.overlayOpacity = Integer.parseInt(newOpacity);
			AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,  win.overlayOpacity  / 100f);			
			win.resultOverlay.setComposite(alpha);
		}
	}
	
}// end of Weka_Segmentation class

