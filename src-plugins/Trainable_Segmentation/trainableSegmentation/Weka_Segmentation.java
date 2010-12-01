package trainableSegmentation;

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
 * Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), Verena Kaynig (verena.kaynig@inf.ethz.ch), 
 *          Albert Cardona (acardona@ini.phys.ethz.ch)
 */


import ij.IJ;
import ij.ImageStack;
import ij.plugin.PlugIn;

import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.ImagePlus;
import ij.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyEditor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3f;

import util.FindConnectedRegions;
import util.FindConnectedRegions.Results;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.gui.GUIChooser;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyDialog;
import weka.gui.explorer.ClassifierPanel;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;
import hr.irb.fastRandomForest.FastRandomForest;

/**
 * Segmentation plugin based on the machine learning library Weka 
 */
public class Weka_Segmentation implements PlugIn 
{
	/** 50% alpha composite */
	final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
	/** 25% alpha composite */
	final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
	/** opacity (in %) of the result overlay image */
	int overlayOpacity = 33;
	/** alpha composite for the result overlay image */
	Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
	
	/** maximum number of classes (labels) allowed on the GUI*/
	private static final int MAX_NUM_CLASSES = 5;
	/** array of lists of Rois for each class */
	private List<Roi> [] examples = new ArrayList[MAX_NUM_CLASSES];
	/** image to be used in the training */
	private ImagePlus trainingImage;
	/** image to display on the GUI */
	private ImagePlus displayImage;
	/** result image after classification */
	private ImagePlus classifiedImage;
	/** features to be used in the training */
	private FeatureStack featureStack = null;
	/** GUI window */
	private CustomWindow win;
	/** array of number of traces per class */
	private int traceCounter[] = new int[MAX_NUM_CLASSES];
	/** flag to display the overlay image */
	private boolean showColorOverlay;
	/** set of instances for the whole training image */
	private Instances wholeImageData;
	/** set of instances from loaded data (previously saved segmentation) */
	private Instances loadedTrainingData = null;
	/** set of instances from the user's traces */
	private Instances traceTrainingData = null;
	/** current classifier */
	private AbstractClassifier classifier = null;
	/** train header */
	Instances trainHeader = null;
	
	/** default classifier (Fast Random Forest) */
	private FastRandomForest rf;
	/** flag to update the whole set of instances (used when there is any change on the features or the classes) */
	private boolean updateWholeData = false;
	/** flag to update the feature stack (used when there is any change on the features) */
	private boolean updateFeatures = false;
	
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
	JButton newImageButton;
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
	/** current segmentation result overlay */
	ImageOverlay resultOverlay;
	
	/** available colors for available classes*/
	final Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
			Color.cyan, Color.magenta};
	/** names of the current classes */
	String[] classLabels = new String[]{"class 1", "class 2", "class 3", "class 4", "class 5"};
	
	/** Lookup table for the result overlay image */
	LUT overlayLUT;
	
	/** current number of classes */
	private int numOfClasses = 2;
	/** array of trace lists for every class */
	private java.awt.List exampleList[];
	/** array of buttons for adding each trace class */
	private JButton [] addExampleButton;
	
	// Random Forest parameters
	/** current number of trees in the fast random forest classifier */
	private int numOfTrees = 200;
	/** current number of random features per tree in the fast random forest classifier */
	private int randomFeatures = 2;
	/** maximum depth per tree in the fast random forest classifier */
	private int maxDepth = 0;
	/** list of class names on the loaded data */
	ArrayList<String> loadedClassNames = null;
	/** executor service to launch threads for the plugin methods and events */
	final ExecutorService exec = Executors.newFixedThreadPool(1);
	
	/** GUI/no GUI flag */
	private boolean useGUI = true;
	
	/** expected membrane thickness */
	private int membraneThickness = 1;
	/** size of the patch to use to enhance the membranes */
	private int membranePatchSize = 19;
	
	/** minimum sigma to use on the filters */
	private float minimumSigma = 1f;
	/** maximum sigma to use on the filters */
	private float maximumSigma = 16f;
	
	/** list of the names of features to use */
	private ArrayList<String> featureNames = null;
	
	/** temporary folder name. It is used to stored intermediate results if different from null */
	private String tempFolder = null;
	
	/** flag to set the resampling of the training data in order to guarantee the same number of instances per class */
	private boolean homogenizeClasses = false;
	
	public static final double SIMPLE_POINT_THRESHOLD = 0;
	
	/**
	 * Basic constructor for graphical user interface use
	 */
	public Weka_Segmentation() 
	{
		this.useGUI = true;
		
		// Create overlay LUT
		final byte[] red = new byte[256];
		final byte[] green = new byte[256];
		final byte[] blue = new byte[256];
		final int shift = 255 / MAX_NUM_CLASSES;
		for(int i = 0 ; i < 256; i++)
		{
			final int colorIndex = i / (shift+1);
			//IJ.log("i = " + i + " color index = " + colorIndex);
			red[i] = (byte) colors[colorIndex].getRed();
			green[i] = (byte) colors[colorIndex].getGreen();
			blue[i] = (byte) colors[colorIndex].getBlue();
		}
		overlayLUT = new LUT(red, green, blue);
		
		exampleList = new java.awt.List[MAX_NUM_CLASSES];
		addExampleButton = new JButton[MAX_NUM_CLASSES];
		
		roiOverlay = new RoiListOverlay[MAX_NUM_CLASSES];
		resultOverlay = new ImageOverlay();
		
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
		
		newImageButton = new JButton("New image");
		newImageButton.setToolTipText("Load a new image to segment");
		
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

		addClassButton = new JButton ("Create new class");
		addClassButton.setToolTipText("Add one more label to mark different areas");
		
		settingsButton = new JButton ("Settings");
		settingsButton.setToolTipText("Display settings dialog");
		
		/** The weka image */		
		ImageIcon icon = new ImageIcon(Weka_Segmentation.class.getResource("/trainableSegmentation/images/weka.png"));
		wekaButton = new JButton( icon );
		wekaButton.setToolTipText("Launch Weka GUI chooser");
		
		for(int i = 0; i < numOfClasses ; i++)
		{
			examples[i] = new ArrayList<Roi>();
			exampleList[i] = new java.awt.List(5);
			exampleList[i].setForeground(colors[i]);
		}

		showColorOverlay = false;

		// Initialization of Fast Random Forest classifier
		rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);
		//this is the default that Breiman suggests
		//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		//but this seems to work better
		rf.setNumFeatures(randomFeatures);
		rf.setSeed(123);
				
		classifier = rf;
	}
	
	/**
	 * Listeners
	 */
	private ActionListener listener = new ActionListener() {
		public void actionPerformed(final ActionEvent e) {
			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
				public void run() 
				{											
					if(e.getSource() == trainButton)
					{
						try{
							if( trainClassifier() )
								applyClassifier();
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
					else if(e.getSource() == probabilityButton){
						showProbabilityImage();
					}
					else if(e.getSource() == plotButton){
						plotResult();
					}
					else if(e.getSource() == newImageButton){
						loadNewImage();
					}
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
					}
					else if(e.getSource() == wekaButton){
						launchWeka();
					}
					else{ 
						for(int i = 0; i < numOfClasses; i++)
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
	 * Custom window to define the Advanced Weka Segmentation GUI
	 */
	private class CustomWindow extends ImageWindow 
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
		
		CustomWindow(ImagePlus imp) 
		{
			super(imp, new CustomCanvas(imp));

			final CustomCanvas canvas = (CustomCanvas) getCanvas();
			
			// add roi list overlays (one per class)
			for(int i = 0; i < MAX_NUM_CLASSES; i++)
			{
				roiOverlay[i] = new RoiListOverlay();
				roiOverlay[i].setComposite( transparency050 );
				((OverlayedImageCanvas)ic).addOverlay(roiOverlay[i]);
			}

			// add result overlay
			resultOverlay.setComposite( overlayAlpha );
			((OverlayedImageCanvas)ic).addOverlay(resultOverlay);	
			
			// Remove the canvas from the window, to add it later
			removeAll();

			setTitle("Advanced Weka Segmentation");
			
			// Annotations panel
			annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
			annotationsConstraints.gridwidth = 1;
			annotationsConstraints.gridheight = 1;
			annotationsConstraints.gridx = 0;
			annotationsConstraints.gridy = 0;
			
			annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
			annotationsPanel.setLayout(boxAnnotation);
			
			
			
			for(int i = 0; i < numOfClasses ; i++)
			{
				exampleList[i].addActionListener(listener);
				exampleList[i].addItemListener(itemListener);
				addExampleButton[i] = new JButton("Add to " + classLabels[i]);
				addExampleButton[i].setToolTipText("Add markings of label '" + classLabels[i] + "'");

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
			for(int i = 0; i < numOfClasses ; i++)
				addExampleButton[i].addActionListener(listener);
			trainButton.addActionListener(listener);
			overlayButton.addActionListener(listener);
			resultButton.addActionListener(listener);
			probabilityButton.addActionListener(listener);
			plotButton.addActionListener(listener);
			newImageButton.addActionListener(listener);
			applyButton.addActionListener(listener);
			loadClassifierButton.addActionListener(listener);
			saveClassifierButton.addActionListener(listener);
			loadDataButton.addActionListener(listener);
			saveDataButton.addActionListener(listener);
			addClassButton.addActionListener(listener);
			settingsButton.addActionListener(listener);
			wekaButton.addActionListener(listener);
			
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
			trainingJPanel.add(newImageButton, trainingConstraints);
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
					for(int i = 0; i < numOfClasses ; i++)
						addExampleButton[i].removeActionListener(listener);
					trainButton.removeActionListener(listener);
					overlayButton.removeActionListener(listener);
					resultButton.removeActionListener(listener);
					probabilityButton.removeActionListener(listener);
					plotButton.removeActionListener(listener);
					newImageButton.removeActionListener(listener);
					applyButton.removeActionListener(listener);
					loadClassifierButton.removeActionListener(listener);
					saveClassifierButton.removeActionListener(listener);
					loadDataButton.removeActionListener(listener);
					saveDataButton.removeActionListener(listener);
					addClassButton.removeActionListener(listener);
					settingsButton.removeActionListener(listener);
					wekaButton.removeActionListener(listener);
					
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
			exampleList[numOfClasses].addItemListener(itemListener);
			addExampleButton[numOfClasses] = new JButton("Add to " + classLabels[numOfClasses]);
			
			annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
			annotationsConstraints.insets = new Insets(5, 5, 6, 6);
			
			boxAnnotation.setConstraints(addExampleButton[numOfClasses], annotationsConstraints);
			annotationsPanel.add(addExampleButton[numOfClasses]);
			annotationsConstraints.gridy++;
			
			annotationsConstraints.insets = new Insets(0,0,0,0);

			boxAnnotation.setConstraints(exampleList[numOfClasses], annotationsConstraints);
			annotationsPanel.add(exampleList[numOfClasses]);
			annotationsConstraints.gridy++;
			
			// Add listener to the new button
			addExampleButton[numOfClasses].addActionListener(listener);
			
			// increase number of available classes
			numOfClasses ++;
			
			//IJ.log("new number of classes = " + numOfClasses);
			
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
		
	}

	/**
	 * Plugin run method
	 */
	public void run(String arg) 
	{				
		//		trainingImage = IJ.openImage("testImages/i00000-1.tif");
		//get current image
		if (null == WindowManager.getCurrentImage()) 
		{
			trainingImage = IJ.openImage();
			if (null == trainingImage) return; // user canceled open dialog
		}
		else 		
			trainingImage = new ImagePlus("Advanced Weka Segmentation",WindowManager.getCurrentImage().getProcessor().duplicate());
		

		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
					"is larger than 1024 pixels. \n" +
					"Feature stack creation and classifier training \n" +
					"might take some time depending on your computer.\n" +
			"Proceed?"))
				return;


		trainingImage.setProcessor("Advanced Weka Segmentation", trainingImage.getProcessor().duplicate().convertToByte(true));
		
		// Initialize feature stack (no features yet)
		featureStack = new FeatureStack(trainingImage);
		
		displayImage = new ImagePlus();
		displayImage.setProcessor("Advanced Weka Segmentation", trainingImage.getProcessor().duplicate());

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
		if(useGUI)
		{
			trainButton.setEnabled(s);
			overlayButton.setEnabled(s);
			resultButton.setEnabled(s);
			probabilityButton.setEnabled(s);
			plotButton.setEnabled(s);
			newImageButton.setEnabled(s);
			applyButton.setEnabled(s);
			loadClassifierButton.setEnabled(s);
			saveClassifierButton.setEnabled(s);
			loadDataButton.setEnabled(s);
			saveDataButton.setEnabled(s);
			addClassButton.setEnabled(s);
			settingsButton.setEnabled(s);
			wekaButton.setEnabled(s);
			for(int i = 0 ; i < numOfClasses; i++)
			{
				exampleList[i].setEnabled(s);
				addExampleButton[i].setEnabled(s);
			}
		}
	}

	/**
	 * Update buttons enabling depending on the current status of the plugin
	 */
	private void updateButtonsEnabling()
	{
		if(useGUI)
		{
			final boolean classifierExists =  null != this.classifier;

			trainButton.setEnabled(classifierExists);		
			applyButton.setEnabled(classifierExists);

			final boolean resultExists = null != this.classifiedImage && 
										 null != this.classifiedImage.getProcessor();

			saveClassifierButton.setEnabled(classifierExists);
			overlayButton.setEnabled(resultExists);
			resultButton.setEnabled(resultExists);
			plotButton.setEnabled(resultExists);
			
			probabilityButton.setEnabled(classifierExists);

			newImageButton.setEnabled(true);
			loadClassifierButton.setEnabled(true);		
			loadDataButton.setEnabled(true);

			addClassButton.setEnabled(this.numOfClasses < MAX_NUM_CLASSES);
			settingsButton.setEnabled(true);
			wekaButton.setEnabled(true);

			boolean examplesEmpty = true;
			for(int i = 0; i < numOfClasses; i ++)
				if(examples[i].size() > 0)
				{
					examplesEmpty = false;
					break;
				}

			saveDataButton.setEnabled(!examplesEmpty || null != loadedTrainingData);

			for(int i = 0 ; i < numOfClasses; i++)
			{
				exampleList[i].setEnabled(true);
				addExampleButton[i].setEnabled(true);
			}
		}
	}	
	
	/**
	 * Add examples defined by the user to the corresponding list
	 * @param i list index
	 */
	private void addExamples(int i)
	{
		//get selected pixels
		final Roi r = displayImage.getRoi();
		if (null == r){
			return;
		}

		displayImage.killRoi();
		examples[i].add(r);
		exampleList[i].add("trace " + traceCounter[i]); 
		traceCounter[i]++;
		drawExamples();
	}

	/**
	 * Draw the painted traces on the display image
	 */
	private void drawExamples()
	{

		for(int i = 0; i < numOfClasses; i++)
		{
			roiOverlay[i].setColor(colors[i]);
			final ArrayList< Roi > rois = new ArrayList<Roi>();
			for (Roi r : examples[i])
			{
				rois.add(r);
				//IJ.log("painted ROI: " + r + " in color "+ colors[i]);
			}
			roiOverlay[i].setRoi(rois);
		}		
		displayImage.updateAndDraw();
	}
	
	
	/**
	 * Write current instances into an ARFF file
	 * @param data set of instances
	 * @param filename ARFF file name
	 */
	public boolean writeDataToARFF(Instances data, String filename)
	{
		BufferedWriter out = null;
		try{
			out = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream( filename ) ) );
			
			final Instances header = new Instances(data, 0);
			out.write(header.toString());
			
			for(int i = 0; i < data.numInstances(); i++)
			{
				out.write(data.get(i).toString()+"\n");				
			}			
		}
		catch(Exception e)
		{
			IJ.log("Error: couldn't write instances into .ARFF file.");
			IJ.showMessage("Exception while saving data as ARFF file");
			e.printStackTrace();
			return false;
		}	
		finally{
			try {
				out.close();
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}
		
		return true;
		 
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
			String attString = featureStack.getSliceLabel(i);
			attributes.add(new Attribute(attString));
		}
		
		final ArrayList<String> classes;

		int numOfInstances = 0;
		int numOfUsedClasses = 0;
		if(null == this.loadedTrainingData)
		{
			classes = new ArrayList<String>();
			for(int i = 0; i < numOfClasses ; i ++)
			{
				// Do not add empty lists
				if(examples[i].size() > 0)
				{
					classes.add(classLabels[i]);
					numOfUsedClasses++;
				}
				numOfInstances += examples[i].size();
			}
		}
		else
		{
			classes = this.loadedClassNames;
		}

				
		attributes.add(new Attribute("class", classes));

		final Instances trainingData =  new Instances("segment", attributes, numOfInstances);

		IJ.log("Training input:");
		
		// For all classes
		for(int l = 0; l < numOfClasses; l++)
		{			
			int nl = 0;
			// Read all lists of examples
			for(int j=0; j<examples[l].size(); j++)
			{
				Roi r = examples[l].get(j);
				
													
				// For polygon rois we get the list of points
				if( r instanceof PolygonRoi && r.getType() != Roi.FREEROI )
				{
					if(r.getStrokeWidth() == 1)
					{
						int[] x = r.getPolygon().xpoints;
						int[] y = r.getPolygon().ypoints;
						final int n = r.getPolygon().npoints;

						for (int i=0; i<n; i++)
						{
							double[] values = new double[featureStack.getSize()+1];
							for (int z=1; z<=featureStack.getSize(); z++)
								values[z-1] = featureStack.getProcessor(z).getPixelValue(x[i], y[i]);
							values[featureStack.getSize()] = (double) l;
							trainingData.add(new DenseInstance(1.0, values));
							// increase number of instances for this class
							nl ++;
						}
					}
					else // For thicker lines, include also neighbors
					{
						final int width = (int) Math.round(r.getStrokeWidth());
						FloatPolygon p = r.getFloatPolygon();
						int n = p.npoints;
												
						double x1, y1;
						double x2=p.xpoints[0]-(p.xpoints[1]-p.xpoints[0]);
						double y2=p.ypoints[0]-(p.ypoints[1]-p.ypoints[0]);
						for (int i=0; i<n; i++) 
						{
							x1 = x2; 
							y1 = y2;
							x2 = p.xpoints[i]; 
							y2 = p.ypoints[i];
							
							double dx = x2-x1;
							double dy = y1-y2;
				            double length = (float)Math.sqrt(dx*dx+dy*dy);
				            dx /= length;
				            dy /= length;
							double x = x2-dy*width/2.0;
							double y = y2-dx*width/2.0;
							
							int n2 = width;
							do {				
								if(x >= 0 && x < featureStack.getWidth() && y >= 0 && y <featureStack.getHeight())
								{
									double[] values = new double[featureStack.getSize()+1];
									for (int z=1; z<=featureStack.getSize(); z++)
										values[z-1] = featureStack.getProcessor(z).getInterpolatedValue(x, y);
									values[featureStack.getSize()] = (double) l;
									trainingData.add(new DenseInstance(1.0, values));
									// increase number of instances for this class
									nl ++;
								}								
								x += dy;
								y += dx;
							} while (--n2>0);
						}
						
					}
				}
				else // for the rest of rois we get ALL points inside the roi
				{														
					final ShapeRoi shapeRoi = new ShapeRoi(r); 
					final Rectangle rect = shapeRoi.getBounds();												
					
					final int lastX = rect.x + rect.width;
					final int lastY = rect.y + rect.height;

					for(int x = rect.x; x < lastX; x++)
						for(int y = rect.y; y < lastY; y++)
							if(shapeRoi.contains(x, y))
							{
								double[] values = new double[featureStack.getSize()+1];
								for (int z=1; z<=featureStack.getSize(); z++)
									values[z-1] = featureStack.getProcessor(z).getPixelValue(x, y);
								values[featureStack.getSize()] = (double) l;
								trainingData.add(new DenseInstance(1.0, values));
								// increase number of instances for this class
								nl ++;
							}
				}

				
			}
			
			IJ.log("# of pixels selected as " + classLabels[l] + ": " +nl);						
		}

		if (trainingData.numInstances() == 0)
			return null;
			
		// Set the index of the class attribute
		trainingData.setClassIndex(featureStack.getSize());
		
		return trainingData;
	}

	/**
	 * Train classifier with the current instances
	 */
	public boolean trainClassifier()
	{	
		// Two list of examples need to be non empty
		int nonEmpty = 0;
		for(int i = 0; i < numOfClasses; i++)
			if(examples[i].size() > 0)
				nonEmpty++;
		if (nonEmpty < 2 && null == loadedTrainingData){
			IJ.showMessage("Cannot train without at least 2 sets of examples!");
			return false;
		}
		
		// Disable buttons until the training has finished
		setButtonsEnabled(false);
				
		// Create feature stack if necessary (training from traces
		// and the features stack is empty or the settings changed)
		if(nonEmpty > 1 && featureStack.isEmpty() || updateFeatures)
		{
			IJ.showStatus("Creating feature stack...");
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			IJ.log("Features stack is now updated.");
		}		

		IJ.showStatus("Training classifier...");
		Instances data = null;
		if (nonEmpty < 1)
			IJ.log("Training from loaded data only...");
		else 
		{
			final long start = System.currentTimeMillis();
			traceTrainingData = data = createTrainingInstances();
			final long end = System.currentTimeMillis();
			IJ.log("Creating training data took: " + (end-start) + "ms");			
		}

		if (loadedTrainingData != null && data != null){
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++)
				data.add(loadedTrainingData.instance(i));
			IJ.log("Finished: total number of instances = " + data.numInstances());
		}
		else if (data == null)
		{
			data = loadedTrainingData;
			IJ.log("Taking loaded data as only data...");
		}
		
		if (null == data){
			IJ.log("WTF");
		}
		
		// Update train header
		this.trainHeader = new Instances(data, 0);
		
		// Resample data if necessary
		if(homogenizeClasses)
		{
			IJ.showStatus("Homogenizing classes distribution...");
			IJ.log("Homogenizing classes distribution...");
			data = homogenizeTrainingData(data);		
		}

		IJ.showStatus("Training classifier...");
		IJ.log("Training classifier...");
		
		// Train the classifier on the current data
		final long start = System.currentTimeMillis();
		try{
			classifier.buildClassifier(data);
		}
		catch(Exception e){
			IJ.showMessage(e.getMessage());
			e.printStackTrace();
			return false;
		}
		
		// Print classifier information
		IJ.log( this.classifier.toString() );
		
		final long end = System.currentTimeMillis();
						
		IJ.log("Finished training in "+(end-start)+"ms");
		return true;
	}
	
	/**
	 * Set flag to homogenize classes before training
	 * 
	 * @param homogenizeClasses true to resample the classes before training
	 */
	public void setHomogenizeClasses(boolean homogenizeClasses)
	{
		this.homogenizeClasses = homogenizeClasses;
	}
	
	/**
	 * Apply current classifier to current training image
	 */
	public void applyClassifier()
	{
		// Create feature stack if it was not created yet
		if(featureStack.isEmpty() || updateFeatures)
		{
			IJ.showStatus("Creating feature stack...");
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			IJ.log("Features stack is now updated.");
		}
		
		if(updateWholeData)
		{
			updateTestSet();
			IJ.log("Test dataset updated ("+ wholeImageData.numInstances() + " instances, " + wholeImageData.numAttributes() + " attributes).");
		}

		IJ.log("Classifying whole image...");

		classifiedImage = applyClassifier(wholeImageData, trainingImage.getWidth(), trainingImage.getHeight(), true);

		IJ.log("Finished segmentation of whole image.\n");
		if(useGUI)
		{
			if(showColorOverlay)
				toggleOverlay();
			toggleOverlay();
			setButtonsEnabled(true);
		}
	}
	
	
	/**
	 * Update whole data set with current number of classes and features
	 */
	private void updateTestSet() 
	{
		IJ.showStatus("Reading whole image data...");
		
		long start = System.currentTimeMillis();
		ArrayList<String> classNames = null;
		
		if(null != loadedClassNames)
			classNames = loadedClassNames;
		else
		{
			classNames = new ArrayList<String>();

			for(int i = 0; i < numOfClasses; i++)
				if(examples[i].size() > 0)
					classNames.add(classLabels[i]);
		}
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
	public ImagePlus applyClassifier(final Instances data, int w, int h, boolean parallelise)
	{
		IJ.showStatus("Classifying image...");
		
		final long start = System.currentTimeMillis();

		final int numOfProcessors;
		if (parallelise) {
			numOfProcessors = Runtime.getRuntime().availableProcessors();
		} else {
			numOfProcessors = 1;
		}
		final ExecutorService exe = Executors.newFixedThreadPool(numOfProcessors);
		final double[][] results = new double[numOfProcessors][];
		final Instances[] partialData = new Instances[numOfProcessors];
		final int partialSize = data.numInstances() / numOfProcessors;
		Future<double[]> fu[] = new Future[numOfProcessors];
		
		final AtomicInteger counter = new AtomicInteger();
		
		for(int i = 0; i<numOfProcessors; i++)
		{
			if(i == numOfProcessors-1)
				partialData[i] = new Instances(data, i*partialSize, data.numInstances()-i*partialSize);
			else
				partialData[i] = new Instances(data, i*partialSize, partialSize);
			
			fu[i] = exe.submit(classifyIntances(partialData[i], classifier, counter));
		}
		
		ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
		ScheduledFuture task = monitor.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				IJ.showProgress(counter.get(), data.numInstances());
			}
		}, 0, 1, TimeUnit.SECONDS);
		
		// Join threads
		for(int i = 0; i<numOfProcessors; i++)
		{
			try {
				results[i] = fu[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return null;
			} finally {
				exe.shutdown();
				task.cancel(true);
				monitor.shutdownNow();
				IJ.showProgress(1);
			}
		}
		
		
		exe.shutdown();
		
		// Create final array
		double[] classificationResult = new double[data.numInstances()];
		for(int i = 0; i<numOfProcessors; i++)
			System.arraycopy(results[i], 0, classificationResult, i*partialSize, results[i].length);
			
		
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
	 * Classify instance concurrently
	 * @param data set of instances to classify
	 * @param classifier current classifier
	 * @return classification result
	 */
	private static Callable<double[]> classifyIntances(
			final Instances data, 
			final AbstractClassifier classifier,
			final AtomicInteger counter)
	{
		return new Callable<double[]>(){
			public double[] call(){
				final int numInstances = data.numInstances();
				final double[] classificationResult = new double[numInstances];
				for (int i=0; i<numInstances; i++)
				{
					try{
						if (0 == i % 4000) counter.addAndGet(4000);
						classificationResult[i] = classifier.classifyInstance(data.instance(i));
					}catch(Exception e){
						IJ.showMessage("Could not apply Classifier!");
						e.printStackTrace();
						return null;
					}
				}
				return classificationResult;
			}
		};
		
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
			
			ImageProcessor overlay = classifiedImage.getProcessor().duplicate();
			
			double shift = 255.0 / MAX_NUM_CLASSES;
			overlay.multiply(shift+1);
			overlay = overlay.convertToByte(false);
			overlay.setColorModel(overlayLUT);
						
			resultOverlay.setImage(overlay);
		}
		else
			resultOverlay.setImage(null);

		displayImage.updateAndDraw();
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
			{
				final Roi newRoi = examples[i].get(exampleList[i].getSelectedIndex()); 
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
	void deleteSelected(final ActionEvent e){

		for(int i = 0; i < numOfClasses; i++)
			if (e.getSource() == exampleList[i]) 
			{
				//delete item from ROI
				int index = exampleList[i].getSelectedIndex();
				
				// kill Roi from displayed image
				if(displayImage.getRoi().equals( examples[i].get(index) ))
					displayImage.killRoi();

				
				examples[i].remove(index);
				//delete item from list
				exampleList[i].remove(index);
			}
		
		drawExamples();		
	}

	/**
	 * Display the whole image after classification 
	 */
	void showClassificationImage(){
		ImagePlus resultImage = new ImagePlus("classification result", classifiedImage.getProcessor().convertToByte(true).duplicate());
		resultImage.show();
	}

	/**
	 * Display the current probability maps 
	 */
	void showProbabilityImage()
	{
		IJ.showStatus("Calculating probability maps...");
		IJ.log("Calculating probability maps...");
		this.setButtonsEnabled(false);
		final ImagePlus probImage = this.getProbabilityMapsMT();
		if(null != probImage) 
			probImage.show();
		this.updateButtonsEnabling();
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
		this.setButtonsEnabled(false);
		final Instances data = null == traceTrainingData ? loadedTrainingData : traceTrainingData;
		displayGraphs(data, classifier);
		this.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
	}	
	/**
	 * Apply classifier to test data
	 */
	public void applyClassifierToTestData()
	{
		// array of files to process
		File[] imageFiles;
		String storeDir = "";

		// create a file chooser for the image files
		JFileChooser fileChooser = new JFileChooser(".");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setMultiSelectionEnabled(true);

		// get selected files or abort if no file has been selected
		int returnVal = fileChooser.showOpenDialog(null);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			imageFiles = fileChooser.getSelectedFiles();
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

		final int numProcessors = Runtime.getRuntime().availableProcessors();

		IJ.log("Processing " + imageFiles.length + " image files in " + numProcessors + " threads....");

		setButtonsEnabled(false);

		Thread[] threads = new Thread[numProcessors];

		class ImageProcessingThread extends Thread {

			final int     numThread;
			final int     numProcessors;
			final File[]  imageFiles;
			final boolean storeResults;
			final boolean showResults;
			final String  storeDir;

			public ImageProcessingThread(int numThread, int numProcessors,
			                             File[] imageFiles,
			                             boolean storeResults, boolean showResults,
			                             String storeDir) {
				this.numThread     = numThread;
				this.numProcessors = numProcessors;
				this.imageFiles    = imageFiles;
				this.storeResults  = storeResults;
				this.showResults   = showResults;
				this.storeDir      = storeDir;
			}

			public void run() {

				for (int i = numThread; i < imageFiles.length; i += numProcessors) {

					File file = imageFiles[i];

					ImagePlus testImage = IJ.openImage(file.getPath());

					IJ.log("Processing image " + file.getName() + " in thread " + numThread);

					boolean parallelise = (imageFiles.length < numProcessors);

					ImagePlus segmentation = applyClassifierToTestImage(testImage, parallelise);

					if (showResults) {
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
		for (int i = 0; i < numProcessors; i++) {

			threads[i] = new ImageProcessingThread(i, numProcessors, imageFiles, storeResults, showResults, storeDir);
			threads[i].start();
		}

		// join all threads
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}
		
		updateButtonsEnabling();		
	}

	/**
	 * Apply current classifier to image
	 * 
	 * @param testImage test image (2D single image or stack)
	 * @return result image (classification)
	 */
	public ImagePlus applyClassifierToTestImage(ImagePlus testImage, boolean parallelise)
	{		
		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[i].size() > 0)
					classNames.add(classLabels[i]);
		}
		else
			classNames = loadedClassNames;
		
		final ImageStack classified = new ImageStack(testImage.getWidth(), testImage.getHeight());
		
		for(int i=1; i<=testImage.getStackSize(); i++)
		{
			final ImagePlus testSlice = new ImagePlus(testImage.getImageStack().getSliceLabel(i), testImage.getImageStack().getProcessor(i).convertToByte(true));
			// Create feature stack for test image
			IJ.showStatus("Creating features for test image...");
			IJ.log("Creating features for test image " + i +  "...");
			final FeatureStack testImageFeatures = new FeatureStack(testSlice);
			// Use the same features as the current classifier
			testImageFeatures.setEnableFeatures(featureStack.getEnableFeatures());
			testImageFeatures.setMaximumSigma(maximumSigma);
			testImageFeatures.setMinimumSigma(minimumSigma);
			testImageFeatures.setMembranePatchSize(membranePatchSize);
			testImageFeatures.setMembraneSize(membraneThickness);
			testImageFeatures.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, testImageFeatures);

			final Instances testData = testImageFeatures.createInstances(classNames);
			testData.setClassIndex(testData.numAttributes() - 1);

			final ImagePlus testClassImage = applyClassifier(testData, testSlice.getWidth(), testSlice.getHeight(), parallelise);
			testClassImage.setTitle("classified_" + testSlice.getTitle());
			testClassImage.setProcessor(testClassImage.getProcessor().convertToByte(true).duplicate());
			classified.addSlice(testClassImage.getTitle(), testClassImage.getProcessor());
		}
		
		return new ImagePlus("Classification result", classified);
	}

	/**
	 * Load a Weka classifier from a file
	 */
	public void loadClassifier()
	{
		OpenDialog od = new OpenDialog("Choose Weka classifier file","");
		if (od.getFileName()==null)
			return;
		IJ.log("Loading Weka classifier from " + od.getDirectory() + od.getFileName() + "...");
		
		setButtonsEnabled(false);				
							
		final AbstractClassifier oldClassifier = this.classifier;
		
		
		// Try to load Weka model (classifier and train header)
		if( false == loadClassifier(od.getDirectory() + od.getFileName()) )
		{
			IJ.error("Error when loading Weka classifier from file");
			this.classifier = oldClassifier;
			updateButtonsEnabling();
			return;
		}
								
		
		IJ.log("Read header from " + od.getDirectory() + od.getFileName() + " (number of attributes = " + trainHeader.numAttributes() + ")");
		
		if(trainHeader.numAttributes() < 1)
		{
			IJ.error("Error", "No attributes were found on the model header");
			this.classifier = oldClassifier;
			updateButtonsEnabling();
			return;
		}
		
		updateButtonsEnabling();
		
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
		
		if( false == saveClassifier(this.classifier, this.trainHeader, sd.getDirectory() + sd.getFileName()) )
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
		OpenDialog od = new OpenDialog("Choose new image", "");
		if (od.getFileName()==null)
			return;
		
		this.setButtonsEnabled(false);
		
		IJ.log("Loading image " + od.getDirectory() + od.getFileName() + "...");
		
		ImagePlus newImage = new ImagePlus(od.getDirectory() + od.getFileName());
		
		if( false == loadNewImage( newImage ) )
		{
			IJ.error("Error while loading new image!");
			this.updateButtonsEnabling();
			return;
		}
		
		// Remove traces from the lists and ROI overlays
		for(int i = 0; i < numOfClasses; i ++)
		{
			exampleList[i].removeAll();			
			roiOverlay[i].setRoi(null);
		}
			
		// Updating image
		displayImage = new ImagePlus();
		displayImage.setProcessor("Advanced Weka Segmentation", trainingImage.getProcessor().duplicate());
		
		// Remove current classification result image
		resultOverlay.setImage(null);
					
		toggleOverlay();
		
		// Update GUI
		win.setImagePlus(displayImage);
		displayImage.updateAndDraw();
		win.pack();
				
		this.updateButtonsEnabling();
	}
	
	/**
	 * Load previously saved data
	 */
	public void loadTrainingData()
	{
		OpenDialog od = new OpenDialog("Choose data file","", "data.arff");
		if (od.getFileName()==null)
			return;
		IJ.log("Loading data from " + od.getDirectory() + od.getFileName() + "...");
		loadedTrainingData = readDataFromARFF(od.getDirectory() + od.getFileName());
		
		// Adjust current state to loaded data to match 
		// attributes and classes
		if (false == adjustSegmentationStateToData(loadedTrainingData) )
			loadedTrainingData = null;
		else
			IJ.log("Loaded data: " + loadedTrainingData.numInstances() + " instances");
	}
	
	/**
	 * Adjust current segmentation state (attributes and classes) to 
	 * loaded data
	 * @param data loaded instances
	 * @return false if error
	 */
	public boolean adjustSegmentationStateToData(Instances data)
	{
		// Check the features that were used in the loaded data
		boolean featuresChanged = false;		
		Enumeration<Attribute> attributes = data.enumerateAttributes();
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		
		// Initialize list of names for the features to use
		this.featureNames = new ArrayList<String>();
		
		float minSigma = Float.MAX_VALUE;
		float maxSigma = Float.MIN_VALUE;
		
		while(attributes.hasMoreElements())
		{
			final Attribute a = attributes.nextElement();
			this.featureNames.add(a.name());
			for(int i = 0 ; i < numFeatures; i++)
			{				
				if(a.name().startsWith(FeatureStack.availableFeatures[i]))
				{
					usedFeatures[i] = true;
					if(i == FeatureStack.MEMBRANE)
					{
						int index = a.name().indexOf("s_") + 4;
						int index2 = a.name().indexOf("_", index+1 );
						final int patchSize = Integer.parseInt(a.name().substring(index, index2));
						if(patchSize != membranePatchSize)	
						{
							membranePatchSize = patchSize;
							this.featureStack.setMembranePatchSize(patchSize);
							featuresChanged = true;
						}
						index = a.name().lastIndexOf("_");
						final int thickness = Integer.parseInt(a.name().substring(index+1));
						if(thickness != membraneThickness)	
						{
							membraneThickness = thickness;
							this.featureStack.setMembraneSize(thickness);
							featuresChanged = true;
						}
						
					}
					else if(i < FeatureStack.ANISOTROPIC_DIFFUSION)
					{
						String[] tokens = a.name().split("_"); 
						for(int j=0; j<tokens.length; j++)
							if(tokens[j].indexOf(".") != -1)
							{
								final float sigma = Float.parseFloat(tokens[j]);
								if(sigma < minSigma)
									minSigma = sigma;
								if(sigma > maxSigma)
									maxSigma = sigma;
							}
					}
				}
			}
		}
		
		IJ.log("Field of view: max sigma = " + maxSigma + ", min sigma = " + minSigma);
		IJ.log("Membrane thickness: " + membraneThickness + ", patch size: " + membranePatchSize); 
		if(minSigma != this.minimumSigma && minSigma != 0)
		{
			this.minimumSigma = minSigma;
			featuresChanged = true;
			this.featureStack.setMinimumSigma(minSigma);
		}
		if(maxSigma != this.maximumSigma)
		{
			this.maximumSigma = maxSigma;
			featuresChanged = true;
			this.featureStack.setMaximumSigma(maxSigma);
		}
		
		// Check if classes match
		Attribute classAttribute = data.classAttribute();
		Enumeration<String> classValues  = classAttribute.enumerateValues();
		
		// Update list of names of loaded classes
		loadedClassNames = new ArrayList<String>();
		
		int j = 0;
		while(classValues.hasMoreElements())
		{
			final String className = classValues.nextElement().trim();
			loadedClassNames.add(className);
		}
		
		for(String className : loadedClassNames)
		{
			IJ.log("Read class name: " + className);
			if( !className.equals(this.classLabels[j]))
			{
				String currentLabels = classLabels[0];
				for(int i = 1; i < numOfClasses; i++)
					currentLabels = currentLabels.concat(", " + classLabels[i]);
				String loadedLabels = loadedClassNames.get(0); 
				for(int i = 1; i < loadedClassNames.size(); i++)
					loadedLabels = loadedLabels.concat(", " + loadedClassNames.get(i));
				IJ.error("ERROR: Loaded classes and current classes do not match!\nLoaded: " + loadedLabels + "\nFound:" + currentLabels);
				return false;
			}
			j++;
		}
		
		if(j != numOfClasses)
		{
			IJ.error("ERROR: Loaded number of classes and current number do not match!");
			return false;
		}				
		
		
		final boolean[] oldEnableFeatures = this.featureStack.getEnableFeatures();
		// Read checked features and check if any of them changed
		for(int i = 0; i < numFeatures; i++)
		{
			if (usedFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}
		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			this.featureStack.setEnableFeatures(usedFeatures);
			// Force features to be updated
			updateFeatures = true;
		}
		
		return true;
	}
	
	/**
	 * Set features to use during training
	 * 
	 * @param featureNames list of feature names to use
	 * @return false if error
	 */
	public boolean setFeatures(ArrayList<String> featureNames)
	{
		if (null == featureNames)
			return false;
		
		this.featureNames = featureNames;
		
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		for(final String name : featureNames)
		{
			for(int i = 0 ; i < numFeatures; i++)						
				if(name.startsWith(FeatureStack.availableFeatures[i]))				
					usedFeatures[i] = true;							
		}
		
		this.featureStack.setEnableFeatures(usedFeatures);		
				
		return true;
	}
	
	/**
	 * Set the membrane patch size (it must be an odd number)
	 * @param patchSize membrane patch size
	 */
	public void setMembranePatchSize(int patchSize)
	{		
		this.membranePatchSize = patchSize;
		this.featureStack.setMembranePatchSize(patchSize);
	}
	
	/**
	 * Set the maximum sigma/radius to use in the features
	 * @param sigma maximum sigma to use in the features filters
	 */
	public void setMaximumSigma(float sigma)
	{		
		this.maximumSigma = sigma;
		this.featureStack.setMaximumSigma(sigma);
	}
	
	/**
	 * Set the minimum sigma (radius) to use in the features
	 * @param sigma minimum sigma (radius) to use in the features filters
	 */
	public void setMinimumSigma(float sigma)
	{		
		this.minimumSigma = sigma;
		this.featureStack.setMinimumSigma(sigma);
	}
	
	
	/**
	 * Save training model into a file
	 */
	public void saveTrainingData()
	{		
		SaveDialog sd = new SaveDialog("Choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;
		
		if(false == saveData(sd.getDirectory() + sd.getFileName()))
			IJ.showMessage("There is no data to save");
	}
	
	
	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass() 
	{
		if(numOfClasses == MAX_NUM_CLASSES)
		{
			IJ.showMessage("Advanced Weka Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

		//IJ.log("Adding new class...");
		
		String inputName = JOptionPane.showInputDialog("Please input a new label name");
		
		if(null == inputName)
			return;
		
		
		if (null == inputName || 0 == inputName.length()) {
			IJ.error("Invalid name for class");
			return;
		}
		inputName = inputName.trim();
		
		if (0 == inputName.toLowerCase().indexOf("add to ")) 
			inputName = inputName.substring(7);					
		
		
		// Add new name to the list of labels
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
		
		final boolean[] oldEnableFeatures = this.featureStack.getEnableFeatures();
		
		gd.addMessage("Training features:");
		final int rows = (int)Math.round(FeatureStack.availableFeatures.length/2.0);
		gd.addCheckboxGroup(rows, 2, FeatureStack.availableFeatures, oldEnableFeatures);
		
		if(loadedTrainingData != null)
		{
			final Vector<Checkbox> v = gd.getCheckboxes();
			for(Checkbox c : v)
				c.setEnabled(false);
			gd.addMessage("WARNING: no features are selectable while using loaded data");
		}
		
		// Expected membrane thickness
		gd.addNumericField("Membrane thickness:", membraneThickness, 0);
		// Membrane patch size
		gd.addNumericField("Membrane patch size:", membranePatchSize, 0);
		// Field of view
		gd.addNumericField("Minimum sigma:", minimumSigma, 1);
		gd.addNumericField("Maximum sigma:", maximumSigma, 1);
		
		if(loadedTrainingData != null)
			((TextField) gd.getNumericFields().get(0)).setEnabled(false);
		
		gd.addMessage("General options:");
						
		if( this.classifier instanceof FastRandomForest )
		{
			gd.addMessage("Fast Random Forest settings:");
			gd.addNumericField("Number of trees:", numOfTrees, 0);
			gd.addNumericField("Random features", randomFeatures, 0);
			gd.addNumericField("Max depth", maxDepth, 0);
		}
		else
		{
			String classifierName = (this.classifier.getClass()).toString();
			int index = classifierName.indexOf(" ");
			classifierName = classifierName.substring(index + 1);
			gd.addMessage(classifierName + " settings");
			gd.addButton("Set Weka classifier options", new ClassifierSettingsButtonListener(this.classifier));
		}

		
		gd.addMessage("Class names:");
		for(int i = 0; i < numOfClasses; i++)
			gd.addStringField("Class "+(i+1), classLabels[i], 15);
		
		gd.addMessage("Advanced options:");
		gd.addCheckbox("Homogenize classes", homogenizeClasses);
		gd.addButton("Save feature stack", new SaveFeatureStackButtonListener("Select location to save feature stack", featureStack));
		gd.addSlider("Result overlay opacity", 0, 100, overlayOpacity);
		gd.addHelp("http://pacific.mpi-cbg.de/wiki/Trainable_Segmentation_Plugin");
		
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

		// Membrane thickness
		final int newThickness = (int) gd.getNextNumber();
		if(newThickness != membraneThickness)
		{
			featuresChanged = true;
			membraneThickness = newThickness;
			this.featureStack.setMembraneSize(membraneThickness);
		}
		// Membrane patch size
		final int newPatch = (int) gd.getNextNumber();
		if(newPatch != membranePatchSize)
		{
			featuresChanged = true;
			membranePatchSize = newPatch;
			this.featureStack.setMembranePatchSize(newPatch);
		}
		// Field of view (minimum and maximum sigma/radius for the filters)
		final float newMinSigma = (float) gd.getNextNumber();
		if(newMinSigma != minimumSigma && newMinSigma > 0)
		{
			featuresChanged = true;
			minimumSigma = newMinSigma;
			this.featureStack.setMinimumSigma(newMinSigma);
		}
		
		final float newMaxSigma = (float) gd.getNextNumber();
		if(newMaxSigma != maximumSigma && newMaxSigma > minimumSigma)
		{
			featuresChanged = true;
			maximumSigma = newMaxSigma;
			this.featureStack.setMaximumSigma(newMaxSigma);
		}
		if(minimumSigma >= maximumSigma)
		{
			IJ.error("Error in the field of view parameters: they will be reset to default values");
			minimumSigma = 0f;
			maximumSigma = 16f;
		}
		// Read fast random forest parameters and check if changed
		if( this.classifier instanceof FastRandomForest )
		{
			final int newNumTrees = (int) gd.getNextNumber();
			final int newRandomFeatures = (int) gd.getNextNumber();
			final int newMaxDepth = (int) gd.getNextNumber();
			
			// Update random forest if necessary
			if(newNumTrees != numOfTrees 
					||	newRandomFeatures != randomFeatures
					||	newMaxDepth != maxDepth)
				
				updateClassifier(newNumTrees, newRandomFeatures, newMaxDepth);
		}		
		
		boolean classNameChanged = false;
		for(int i = 0; i < numOfClasses; i++)
		{
			String s = gd.getNextString();
			if (null == s || 0 == s.length()) {
				IJ.log("Invalid name for class " + (i+1));
				continue;
			}
			s = s.trim();
			if(!s.equals(classLabels[i]))
			{
				if (0 == s.toLowerCase().indexOf("add to ")) 
					s = s.substring(7);
				
				classLabels[i] = s;
				classNameChanged = true;
				addExampleButton[i].setText("Add to " + classLabels[i]);

			}
		}
		
		// Update flag to homogenize number of class instances
		homogenizeClasses = gd.getNextBoolean();
		
		// Update result overlay alpha
		final int newOpacity = (int) gd.getNextNumber();
		if( newOpacity != overlayOpacity )
		{
			overlayOpacity = newOpacity;
			overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
			resultOverlay.setComposite(overlayAlpha);
			
			if( showColorOverlay )
				displayImage.updateAndDraw();
		}

		
		// If there is a change in the class names, 
		// the data set (instances) must be updated.
		if(classNameChanged)
		{
			updateWholeData = true;
			// Pack window to update buttons
			win.pack();
		}
					
		
		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			this.featureStack.setEnableFeatures(newEnableFeatures);
			// Force features to be updated
			updateFeatures = true;
		}
		
		return true;
	}


	/**
	 * Button listener class to handle the button action from the 
	 * settings dialog to set the Weka classifier parameters
	 */
	static class ClassifierSettingsButtonListener implements ActionListener 
	{
		AbstractClassifier classifier;


		public ClassifierSettingsButtonListener(AbstractClassifier classifier) 
		{
			this.classifier = classifier;
		}

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
		FeatureStack featureStack;

		public SaveFeatureStackButtonListener(String title, FeatureStack featureStack) 
		{
			this.title = title;
			this.featureStack = featureStack;
		}

		public void actionPerformed(ActionEvent e) 
		{
			if(featureStack.isEmpty())
			{
				//IJ.error("Error", "The feature stack has not been initialized yet, please train first.");
				//return;
				featureStack.updateFeaturesMT();					
			}
			
			SaveDialog sd = new SaveDialog(title, "feature-stack", ".tif");
			final String dir = sd.getDirectory();
			final String filename = sd.getFileName();
			
			if(null == dir || null == filename)
				return;
								
			if(false == this.featureStack.saveStackAsTiff(dir + filename))
			{
				IJ.error("Error", "Feature stack could not be saved");
				return;
			}
			
			IJ.log("Feature stack saved as " + dir + filename);
		}
	}
	

	/**
	 * Update fast random forest classifier with new values
	 * 
	 * @param newNumTrees new number of trees
	 * @param newRandomFeatures new number of random features per tree
	 * @param newMaxDepth new maximum depth per tree
	 * @return false if error
	 */
	private boolean updateClassifier(
			int newNumTrees, 
			int newRandomFeatures,
			int newMaxDepth) 
	{
		if(newNumTrees < 1 || newRandomFeatures < 0)
			return false;
		numOfTrees = newNumTrees;
		randomFeatures = newRandomFeatures;
		maxDepth = newMaxDepth;
		
		rf.setNumTrees(numOfTrees);
		rf.setNumFeatures(randomFeatures);
		rf.setMaxDepth(maxDepth);
		
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	// Library style methods //////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	/**
	 * No GUI constructor
	 * 
	 * @param trainingImage input image
	 */
	public Weka_Segmentation(ImagePlus trainingImage) 
	{	
		// no GUI
		this.useGUI = false;
		
		this.trainingImage = trainingImage;
		
		for(int i = 0; i < numOfClasses ; i++)
			examples[i] = new ArrayList<Roi>();


		// Initialization of Fast Random Forest classifier
		rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);
		//this is the default that Breiman suggests
		//rf.setNumFeatures((int) Math.round(Math.sqrt(featureStack.getSize())));
		//but this seems to work better
		rf.setNumFeatures(randomFeatures);
		rf.setSeed(123);
		
		classifier = rf;
		
		// Initialize feature stack (no features yet)
		featureStack = new FeatureStack(trainingImage);
	}

	/**
	 * Load training data (no GUI)
	 * 
	 * @param pathname complete path name of the training data file (.arff)
	 * @return false if error
	 */
	public boolean loadTrainingData(String pathname)
	{
		IJ.log("Loading data from " + pathname + "...");
		loadedTrainingData = readDataFromARFF(pathname);
				
		// Check the features that were used in the loaded data
		Enumeration<Attribute> attributes = loadedTrainingData.enumerateAttributes();
		final int numFeatures = FeatureStack.availableFeatures.length;
		boolean[] usedFeatures = new boolean[numFeatures];
		while(attributes.hasMoreElements())
		{
			final Attribute a = attributes.nextElement();
			for(int i = 0 ; i < numFeatures; i++)
				if(a.name().startsWith(FeatureStack.availableFeatures[i]))
					usedFeatures[i] = true;
		}
		
		// Check if classes match
		Attribute classAttribute = loadedTrainingData.classAttribute();
		Enumeration<String> classValues  = classAttribute.enumerateValues();
		
		// Update list of names of loaded classes
		loadedClassNames = new ArrayList<String>();
		
		int j = 0;
		while(classValues.hasMoreElements())
		{
			final String className = classValues.nextElement().trim();
			loadedClassNames.add(className);
			
			IJ.log("Read class name: " + className);
			if( !className.equals(this.classLabels[j]))
			{
				String s = classLabels[0];
				for(int i = 1; i < numOfClasses; i++)
					s = s.concat(", " + classLabels[i]);
				IJ.error("ERROR: Loaded classes and current classes do not match!\nExpected: " + s);
				loadedTrainingData = null;
				return false;
			}
			j++;
		}
		
		if(j != numOfClasses)
		{
			IJ.error("ERROR: Loaded number of classes and current number do not match!");
			loadedTrainingData = null;
			return false;
		}
		
		IJ.log("Loaded data: " + loadedTrainingData.numInstances() + " instances");
		
		boolean featuresChanged = false;
		final boolean[] oldEnableFeatures = this.featureStack.getEnableFeatures();
		// Read checked features and check if any of them chasetButtonsEnablednged
		for(int i = 0; i < numFeatures; i++)
		{
			if (usedFeatures[i] != oldEnableFeatures[i])
				featuresChanged = true;
		}
		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			this.featureStack.setEnableFeatures(usedFeatures);
			// Force features to be updated
			updateFeatures = true;
		}
		
		return true;
	}

	/**
	 * Get current classification result
	 * @return classified image
	 */
	public ImagePlus getClassifiedImage()
	{
		return classifiedImage;
	}
	
	/**
	 * Get the current training header
	 * 
	 * @return training header (empty set of instances with the current attributes and classes)
	 */
	public Instances getTrainHeader()
	{
		return this.trainHeader;				
	}
	
	/**
	 * Read header classifier from a .model file
	 * @param filename complete path and file name
	 * @return false if error
	 */
	public boolean loadClassifier(String filename) 
	{	
		File selected = new File(filename);
		try {
			InputStream is = new FileInputStream( selected );
			if (selected.getName().endsWith(ClassifierPanel.PMML_FILE_EXTENSION)) 
			{
				PMMLModel model = PMMLFactory.getPMMLModel(is, null);
				if (model instanceof PMMLClassifier) 				
					classifier = (PMMLClassifier)model;				 
				else 				
					throw new Exception("PMML model is not a classification/regression model!");				
			} 
			else 
			{
				if (selected.getName().endsWith(".gz")) 				
					is = new GZIPInputStream(is);
				
				ObjectInputStream objectInputStream = new ObjectInputStream(is);
				classifier = (AbstractClassifier) objectInputStream.readObject();
				try 
				{ // see if we can load the header
					trainHeader = (Instances) objectInputStream.readObject();
				} 
				catch (Exception e) 
				{
					IJ.error("Load Failed", "Error while loading train header");
					return false;
				} 
				finally
				{
					objectInputStream.close();
				}
			}
		} 
		catch (Exception e) 
		{
			IJ.error("Load Failed", "Error while loading classifier");
			e.printStackTrace();
			return false;
		}	
		
		try{		
			// Check if the loaded information corresponds to current state of the segmentator
			// (the attributes can be adjusted, but the classes must match)
			if(false == adjustSegmentationStateToData(trainHeader))		
			{
				IJ.log("Error: current segmentator state could not be updated to loaded data requirements (attributes and classes)");				
			}
		}catch(Exception e)
		{
			IJ.log("Error while adjusting data!");
			e.printStackTrace();
		}
		
		
		return true;
	}

	
	/**
	 * Write current classifier into a file
	 * 
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public boolean saveClassifier(String filename)
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
		}
		if (saveOK)
			IJ.log("Saved model into " + filename );

		return saveOK;
	}
	
	/**
	 * Save training data into a file (.arff)
	 * @param pathname complete path name
	 * @return false if error
	 */
	public boolean saveData(final String pathname)
	{
		boolean examplesEmpty = true;
		for(int i = 0; i < numOfClasses; i ++)
			if(examples[i].size() > 0)
			{
				examplesEmpty = false;
				break;
			}
		if (examplesEmpty && loadedTrainingData == null){
			IJ.log("There is no data to save");
			return false;
		}
		
		if(featureStack.getSize() < 2 || updateFeatures)
		{
			setButtonsEnabled(false);
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
			updateButtonsEnabling();
		}
		
		Instances data = null;
		
		if(examplesEmpty == false)
		{
			data = createTrainingInstances();
			data.setClassIndex(data.numAttributes() - 1);
		}
		if (null != loadedTrainingData && null != data){
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished: total number of instances = " + data.numInstances());
		}
		else if (null == data)
			data = loadedTrainingData;

		
		IJ.log("Writing training data: " + data.numInstances() + " instances...");
		
		//IJ.log("Data: " + data.numAttributes() +" attributes, " + data.numClasses() + " classes");
		
		writeDataToARFF(data, pathname);
		IJ.log("Saved training data: " + pathname);
		
		return true;
	}
	
	public void setUseNeighbors(boolean useNeighbors)
	{
		this.featureStack.setUseNeighbors(useNeighbors);
	}
	
	
	/**
	 * Add instances to a specific class from a label (binary) image.
	 * Only white (non black) pixels will be added to the corresponding class.
	 *  
	 * @param labelImage binary image 
	 * @param featureStack corresponding feature stack
	 * @param className name of the class which receives the instances
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			FeatureStack featureStack,
			String className)
	{
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
					
		// Detect class index
		int classIndex = 0;
		for(classIndex = 0 ; classIndex < this.classLabels.length; classIndex++)
			if(className.equalsIgnoreCase(this.classLabels[classIndex]))
				break;
		if(classIndex == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className + "' not found.");
			return false;
		}
		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++){
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}
			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();						
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(classLabels[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);
			
			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}
		
			
		
		// Check all pixels different from black
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final ImageProcessor img = labelImage.getProcessor();
		int nl = 0;
		for(int x = 0 ; x < width ; x++)
			for(int y = 0 ; y < height; y++)
			{
				// White pixels are added to the class
				if(img.getPixelValue(x, y) > 0)
				{
					
					
						double[] values = new double[featureStack.getSize()+1];
						for (int z=1; z<=featureStack.getSize(); z++)
							values[z-1] = featureStack.getProcessor(z).getPixelValue(x, y);
						values[featureStack.getSize()] = (double) classIndex;
						loadedTrainingData.add(new DenseInstance(1.0, values));
						// increase number of instances for this class
						nl ++;					
				}
			}
		
		
		IJ.log("Added " + nl + " instances of '" + className +"'.");
		
		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() + 
				" instances, " + loadedTrainingData.numAttributes() + 
				" attributes, " + loadedTrainingData.numClasses() + " classes).");
		
		return true;
	}

	/**
	 * Add instances to a specific class from a label (binary) image.
	 * White pixels will be added to the corresponding class 1 and 
	 * black pixels will be added to class 2.
	 *  
	 * @param labelImage binary image 
	 * @param featureStack corresponding feature stack
	 * @param className1 name of the class which receives the white pixels
	 * @param className2 name of the class which receives the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			FeatureStack featureStack,
			String className1,
			String className2)
	{
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
					
		// Detect class indexes
		int classIndex1 = 0;
		for(classIndex1 = 0 ; classIndex1 < this.classLabels.length; classIndex1++)
			if(className1.equalsIgnoreCase(this.classLabels[classIndex1]))
				break;
		if(classIndex1 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return false;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.classLabels.length; classIndex2++)
			if(className2.equalsIgnoreCase(this.classLabels[classIndex2]))
				break;
		if(classIndex2 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className2 + "' not found.");
			return false;
		}
		
		// Create loaded training data if it does not exist yet
		if(null == loadedTrainingData)
		{
			IJ.log("Initializing loaded data...");
			// Create instances
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();
			for (int i=1; i<=featureStack.getSize(); i++)
			{
				String attString = featureStack.getSliceLabel(i);
				attributes.add(new Attribute(attString));
			}
			
			if(featureStack.useNeighborhood())
				for (int i=0; i<8; i++)
				{	
					IJ.log("Adding extra attribute original_neighbor_" + (i+1) + "...");
					attributes.add(new Attribute(new String("original_neighbor_" + (i+1))));
				}
			
			// Update list of names of loaded classes
			// (we assume the first two default class names)
			loadedClassNames = new ArrayList<String>();						
			for(int i = 0; i < numOfClasses ; i ++)
				loadedClassNames.add(classLabels[i]);
			attributes.add(new Attribute("class", loadedClassNames));
			loadedTrainingData = new Instances("segment", attributes, 1);
			
			loadedTrainingData.setClassIndex(loadedTrainingData.numAttributes()-1);
		}
							
		// Check all pixels different from black
		final int width = labelImage.getWidth();
		final int height = labelImage.getHeight();
		final ImageProcessor img = labelImage.getProcessor();
		int n1 = 0;
		int n2 = 0;
		int classIndex = -1;
		
		for(int y = 0 ; y < height; y++)
			for(int x = 0 ; x < width ; x++)
			{
				// White pixels are added to the class 1
				// and black to class 2
				if(img.getPixelValue(x, y) > 0)
				{
					classIndex = classIndex1;
					n1++;
				}
				else
				{
					classIndex = classIndex2;
					n2++;
				}
					
				/*
				double[] values = new double[featureStack.getSize()+1];
				for (int z=1; z<=featureStack.getSize(); z++)
					values[z-1] = featureStack.getProcessor(z).getPixelValue(x, y);
				values[featureStack.getSize()] = (double) classIndex;
				*/
				loadedTrainingData.add(featureStack.createInstance(x, y, classIndex));										
			}
				
		IJ.log("Added " + n1 + " instances of '" + className1 +"'.");
		IJ.log("Added " + n2 + " instances of '" + className2 +"'.");
		
		IJ.log("Training dataset updated ("+ loadedTrainingData.numInstances() + 
				" instances, " + loadedTrainingData.numAttributes() + 
				" attributes, " + loadedTrainingData.numClasses() + " classes).");
		
		return true;
	}

	/**
	 * Get current feature stack
	 * @return feature stack
	 */
	public FeatureStack getFeatureStack()
	{
		return this.featureStack;
	}

	/**
	 * Get loaded (or accumulated) training instances
	 * 
	 * @return loaded/accumulated training instances
	 */
	public Instances getTrainingInstances()
	{
		return this.loadedTrainingData;
	}
	
	/**
	 * Set current classifier
	 * @param cls new classifier
	 */
	public void setClassifier(AbstractClassifier cls)
	{
		this.classifier = cls;
	}
	
	/**
	 * Load a new image to segment (no GUI)
	 * 
	 * @param newImage new image to segment
	 * @return false if error
	 */
	public boolean loadNewImage( ImagePlus newImage )
	{
		// Warning for images larger than 1024x1024
		if (Math.max(newImage.getWidth(), newImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
					"is larger than 1024 pixels. \n" +
					"Feature stack creation and classifier training \n" +
					"might take some time depending on your computer.\n" +
			"Proceed?"))
				return false;
		
		// Accumulate current data in "loadedTrainingData"
		IJ.log("Storing previous image instances...");
						
		// Create feature stack if it was not created yet
		if(featureStack.isEmpty() || updateFeatures)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
		
		// Create instances
		Instances data = createTrainingInstances();		
		if (null != loadedTrainingData && null != data)
		{
			data.setClassIndex(data.numAttributes() - 1);
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++){
				// IJ.log("" + i)
				data.add(loadedTrainingData.instance(i));
			}
			IJ.log("Finished");
		}
		else if (null == data)
			data = loadedTrainingData;
		
		// Store merged data as loaded data
		loadedTrainingData = data;
		
		if(null != loadedTrainingData)
		{
			Attribute classAttribute = loadedTrainingData.classAttribute();
			Enumeration<String> classValues  = classAttribute.enumerateValues();

			// Update list of names of loaded classes
			loadedClassNames = new ArrayList<String>();		
			while(classValues.hasMoreElements())
			{
				final String className = classValues.nextElement().trim();
				loadedClassNames.add(className);
			}
			IJ.log("Number of accumulated examples: " + loadedTrainingData.numInstances());
		}
		else 
			IJ.log("Number of accumulated examples: 0");
		
		// Remove traces from the lists and ROI overlays
		IJ.log("Removing previous markings...");
		for(int i = 0; i < numOfClasses; i ++)
		{
			examples[i] = new ArrayList<Roi>();			
		}
			
		// Updating image
		IJ.log("Updating image...");
		
		trainingImage.setProcessor("Advanced Weka Segmentation", newImage.getProcessor().duplicate().convertToByte(true));
		
		// Initialize feature stack (no features yet)
		final boolean[] enabledFeatures = featureStack.getEnableFeatures();
		featureStack = new FeatureStack(trainingImage);
		featureStack.setEnableFeatures(enabledFeatures);
		featureStack.setMaximumSigma(this.maximumSigma);
		featureStack.setMinimumSigma(this.minimumSigma);
		featureStack.setMembranePatchSize(this.membranePatchSize);
		featureStack.setMembraneSize(this.membraneThickness);
		updateFeatures = true;
		updateWholeData = true;
		
		// Remove current classification result image
		classifiedImage = null;
					
		IJ.log("Done");
		
		return true;		
	}
	
	/**
	 * Add center lines of label image as binary data
	 * 
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addCenterLinesBinaryData(
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName)
	{
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
		
		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}
		
		// Process white pixels
		final ImagePlus whiteIP = new ImagePlus ("white", labelImage.getProcessor().duplicate());
		IJ.run(whiteIP, "Skeletonize","");
		// Add skeleton to white class
		if( false == this.addBinaryData(whiteIP, featureStack, whiteClassName) )
		{
			IJ.log("Error while loading white class center-lines data.");
			return false;
		}
				
		// Process black pixels
		final ImagePlus blackIP = new ImagePlus ("black", labelImage.getProcessor().duplicate());
		IJ.run(blackIP, "Invert","");
		IJ.run(blackIP, "Skeletonize","");
		// Add skeleton to black class
		if( false == this.addBinaryData(blackIP, featureStack, blackClassName))
		{
			IJ.log("Error while loading black class center-lines data.");
			return false;
		}
		return true;
	}
	
	/**
	 * Filter feature stack based on the list of feature names to use
	 */
	public void filterFeatureStackByList()
	{
		if (null == this.featureNames)
			return;
				
		for(int i=1; i<=this.featureStack.getSize(); i++)
		{
			final String featureName = this.featureStack.getSliceLabel(i);
			if(false == this.featureNames.contains( featureName ) )
			{
				// Remove feature
				this.featureStack.removeFeature( featureName );
				// decrease i to avoid skipping any name
				i--;
			}
		}				
	}

	
	/**
	 * Filter feature stack based on the list of feature names to use	
	 * 
	 * @param featureNames list of feature names to use
	 * @param featureStack feature stack to filter
	 */
	public static void filterFeatureStackByList(
			ArrayList<String> featureNames, 
			FeatureStack featureStack)
	{		
		if (null == featureNames)
			return;					
		
		IJ.log("Filtering feature stack by selected attributes...");
	
		for(int i=1; i<=featureStack.getSize(); i++)
		{			
			final String featureName = featureStack.getSliceLabel(i);
			//IJ.log(" " + featureName + "...");
			if(false == featureNames.contains( featureName ) )
			{
				// Remove feature
				featureStack.removeFeature( featureName );
				// decrease i to avoid skipping any name
				i--;				
			}
		}				
	}
	
	/**
	 * Add label image as binary data
	 * 
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName)
	{
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
		
		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}
		
		// Process label pixels
		final ImagePlus labelIP = new ImagePlus ("labels", labelImage.getProcessor().duplicate());
		// Make sure it's binary
		final byte[] pix = (byte[])labelIP.getProcessor().getPixels();
		for(int i =0; i < pix.length; i++)
			if(pix[i] > 0)
				pix[i] = (byte)255;

		
		if( false == this.addBinaryData(labelIP, featureStack, whiteClassName, blackClassName) )
		{
			IJ.log("Error while loading binary label data.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Add binary training data from input and label images.
	 * Input and label images can be 2D or stacks and their 
	 * sizes must match.
	 * 
	 * @param inputImage input grayscale image
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addBinaryData(
			ImagePlus inputImage,
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName)
	{
		// Check sizes		
		if(labelImage.getWidth() != inputImage.getWidth()
				|| labelImage.getHeight() != inputImage.getHeight()
				|| labelImage.getImageStackSize() != inputImage.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}
		
		final ImageStack inputSlices = inputImage.getImageStack();
		final ImageStack labelSlices = labelImage.getImageStack();
		
		for(int i=1; i <= inputSlices.getSize(); i++)
		{
		
			// Process label pixels
			final ImagePlus labelIP = new ImagePlus ("labels", labelSlices.getProcessor(i).duplicate());
			// Make sure it's binary
			final byte[] pix = (byte[])labelIP.getProcessor().getPixels();
			for(int j =0; j < pix.length; j++)
				if(pix[j] > 0)
					pix[j] = (byte)255;

			final FeatureStack featureStack = new FeatureStack(new ImagePlus("slice " + i, inputSlices.getProcessor(i)));
			featureStack.setEnableFeatures(this.featureStack.getEnableFeatures());
			featureStack.setMembranePatchSize(membranePatchSize);
			featureStack.setMembraneSize(this.membraneThickness);
			featureStack.setMaximumSigma(this.maximumSigma);
			featureStack.setMinimumSigma(this.minimumSigma);
			featureStack.updateFeaturesMT();
			filterFeatureStackByList(this.featureNames, featureStack);

			featureStack.setUseNeighbors(this.featureStack.useNeighborhood());
			
			if( false == this.addBinaryData(labelIP, featureStack, whiteClassName, blackClassName) )
			{
				IJ.log("Error while loading binary label data from slice " + i);
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Add eroded version of label image as binary data
	 * 
	 * @param labelImage binary label image
	 * @param whiteClassName class name for the white pixels
	 * @param blackClassName class name for the black pixels
	 * @return false if error
	 */
	public boolean addErodedBinaryData(
			ImagePlus labelImage,
			String whiteClassName,
			String blackClassName)
	{
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			IJ.log("Features stack is now updated.");
		}
		
		if(labelImage.getWidth() != this.trainingImage.getWidth()
				|| labelImage.getHeight() != this.trainingImage.getHeight())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return false;
		}
		
		// Process white pixels
		final ImagePlus whiteIP = new ImagePlus ("white", labelImage.getProcessor().duplicate());
		IJ.run(whiteIP, "Erode","");
		// Add skeleton to white class
		if( false == this.addBinaryData(whiteIP, featureStack, whiteClassName) )
		{
			IJ.log("Error while loading white class center-lines data.");
			return false;
		}
		

		
		// Process black pixels
		final ImagePlus blackIP = new ImagePlus ("black", labelImage.getProcessor().duplicate());
		IJ.run(blackIP, "Invert","");
		IJ.run(blackIP, "Erode","");
		// Add skeleton to white class
		if( false == this.addBinaryData(blackIP, featureStack, blackClassName))
		{
			IJ.log("Error while loading black class center-lines data.");
			return false;
		}
		return true;
	}

	/**
	 * Set pre-loaded training data (not from the user traces)
	 * @param data new data
	 */
	public void setLoadedTrainingData(Instances data)
	{
		this.loadedTrainingData = data;
	}
	
	/**
	 * Get probability distribution of each class for current classifier 
	 * @return probability stack, one image per class
	 */
	public ImagePlus getProbabilityMaps()
	{
		if(this.classifier == null)
			return null;
		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			IJ.log("Features stack is now updated.");
		}
		
		if(updateWholeData)
		{
			updateTestSet();
			IJ.log("Test dataset updated ("+ wholeImageData.numInstances() + " instances, " + wholeImageData.numAttributes() + " attributes).");
		}
		
		final int width = this.trainingImage.getWidth();
		final int height = this.trainingImage.getHeight();
		
		final ImageStack is = new ImageStack(width, height);
		final double[][] classProb = new double[ wholeImageData.numClasses() ] [ width * height ];
		
		IJ.log("Calculating class probability for whole image...");
		
		for(int i = 0; i < is.getWidth(); i++)
			for(int j = 0; j < is.getHeight(); j++)
			{
				try {
					final int index = i + j * width;
					double[] prob = this.classifier.distributionForInstance(wholeImageData.get(index));
					for(int k = 0 ; k < wholeImageData.numClasses(); k++)
						classProb[k][index] = prob[k];
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		
		IJ.log("Done");
		
		final Attribute classAttribute = wholeImageData.classAttribute();
		final Enumeration<String> classValues  = classAttribute.enumerateValues();
		
		for(int k = 0 ; k < wholeImageData.numClasses(); k++)
			is.addSlice(classValues.nextElement().trim(), new FloatProcessor(width, height, classProb[k]));
		
		return new ImagePlus("Class probabilities", is); 
	}
	
	/**
	 * Get probability distribution of each class for current classifier (multi-thread version)
	 * @return probability stack, one image per class
	 */
	public ImagePlus getProbabilityMapsMT()
	{
		if(this.classifier == null)
			return null;
		
		// Update features if necessary
		if(featureStack.getSize() < 2)
		{
			IJ.log("Creating feature stack...");
			featureStack.updateFeaturesMT();
			filterFeatureStackByList();
			updateFeatures = false;
			updateWholeData = true;
			IJ.log("Features stack is now updated.");
		}
		
		if(updateWholeData)
		{
			updateTestSet();
			IJ.log("Test dataset updated ("+ wholeImageData.numInstances() + " instances, " + wholeImageData.numAttributes() + " attributes).");
		}
		
		final int width = this.trainingImage.getWidth();
		final int height = this.trainingImage.getHeight();
		final int nClasses =  wholeImageData.numClasses();
		
		final ImageStack is = new ImageStack(width, height);
		final FloatProcessor[] classProb = new FloatProcessor[ nClasses ];
		for(int k = 0 ; k < nClasses; k++)
			classProb[k] = new FloatProcessor(width, height);
				
		
		// Check the number of processors in the computer 
		final int numOfProcessors = Runtime.getRuntime().availableProcessors();
		
		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(numOfProcessors);

		final ArrayList< Future<double[][]> > futures = new ArrayList< Future<double[][]> >();
						
		final Instances[] partialData = new Instances[numOfProcessors];
		int blockHeight = height / numOfProcessors;
		final int partialSize = blockHeight * width;
		final Rectangle[] rects = new Rectangle[numOfProcessors];
		
		ImagePlus result = null;
		
		try{
			
						
		
			for (int i=0; i<numOfProcessors; i++) 
			{
				int y_start = i*blockHeight;
				
				if(i == numOfProcessors-1)
				{
					partialData[i] = new Instances(wholeImageData, i*partialSize, wholeImageData.numInstances()-i*partialSize);
					blockHeight = height - i*blockHeight;
				}
				else
				{
					partialData[i] = new Instances(wholeImageData, i*partialSize, partialSize);
				}
				
				rects[i] = new Rectangle(0, y_start, width, blockHeight);
				
				futures.add( exe.submit(getDistributionForIntances(partialData[i], this.classifier)) );
			}

			for(int index = 0 ; index < futures.size(); index ++)
			{
				final double[][] partialProb = futures.get(index).get();
				for(int k = 0 ; k < nClasses; k++)
					classProb[k].insert( new FloatProcessor(width, blockHeight, partialProb[k]), rects[index].x, rects[index].y);
			}
		
			for(int k = 0 ; k < nClasses; k++)
				is.addSlice("class " + (k+1), classProb[k]);
			
			result = new ImagePlus("Class probabilities", is);
			
		}
		catch(Exception ex)
		{
			IJ.log("Error when extracting probability maps!");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}
		
		IJ.log("Done");			
		
		return result; 
	}

	
	/**
	 * Get probability distribution of each class for current classifier 
	 * and specific image data (multi-thread version)
	 * 
	 * @param data input data set
	 * @param width image width
	 * @param height image height
	 * @return probability stack, one image per class
	 */
	public ImagePlus getProbabilityMapsMT(
			Instances data,
			final int width,
			final int height)
	{
		if(this.classifier == null)
			return null;
		
				
		final int nClasses =  data.numClasses();
		
		final ImageStack is = new ImageStack(width, height);
		final FloatProcessor[] classProb = new FloatProcessor[ nClasses ];
		for(int k = 0 ; k < nClasses; k++)
			classProb[k] = new FloatProcessor(width, height);						
		
		// Check the number of processors in the computer 
		final int numOfProcessors = Runtime.getRuntime().availableProcessors();
		
		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(numOfProcessors);

		final ArrayList< Future<double[][]> > futures = new ArrayList< Future<double[][]> >();
						
		final Instances[] partialData = new Instances[numOfProcessors];		
		final Rectangle[] rects = new Rectangle[numOfProcessors];
		
		ImagePlus result = null;
		
		try{
			
			int block_height = height / numOfProcessors;
			if (height % 2 != 0) 
				block_height++;
			
			int partialSize = block_height * width;
			
			for (int i=0; i<numOfProcessors; i++) 
			{
				int y_start = i*block_height;
				
				if(i == numOfProcessors-1)
				{	
					block_height = height - i*block_height;
					partialData[i] = new Instances(data, i*partialSize, data.numInstances()-i*partialSize);				
				}
				else
				{
					partialData[i] = new Instances(data, i*partialSize, partialSize);
				}
				
				rects[i] = new Rectangle(0, y_start, width, block_height);
				
				futures.add( exe.submit(getDistributionForIntances(partialData[i], this.classifier)) );
			}

			for(int index = 0 ; index < futures.size(); index ++)
			{
				final double[][] partialProb = futures.get(index).get();
				if(null == partialProb)
				{
					IJ.log("Error while calculating probability map (part " + index + ")");
					return null;
				}
				for(int k = 0 ; k < nClasses; k++)
				{
					if(null == partialProb[k])
					{
						IJ.log("Error while calculating probability map (part " + index + ", class " + k + ")");
						return null;
					}
					classProb[k].insert( new FloatProcessor(rects[index].width, rects[index].height, partialProb[k]), rects[index].x, rects[index].y);
				}
			}
		
			for(int k = 0 ; k < nClasses; k++)
				is.addSlice("class " + (k+1), classProb[k]);
			
			result = new ImagePlus("Class probabilities", is);
			
		}
		catch(Exception ex)
		{
			IJ.log("Error when extracting probability maps!");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}
		
		IJ.log("Done");			
		
		return result; 
	}
	
	
	/**
	 * Get probability distribution for a set of instances (to be submitted in an ExecutorService)
	 * @param instances set of instances to get the class distribution from
	 * @param classifier current classifier
	 * @return probability values for each instance and class
	 */
	public Callable<double[][]> getDistributionForIntances(
			final Instances instances,
			final AbstractClassifier classifier) 
	{
		return new Callable<double[][]>(){
			public double[][] call(){
				final int nClasses = instances.numClasses();
				double[][] classProb = new double[nClasses][instances.numInstances()];
				for(int i = 0; i < instances.numInstances(); i++)					
				{
					try {						
						double[] prob = classifier.distributionForInstance(instances.get(i));
						for(int k = 0 ; k < nClasses; k++)
							classProb[k][i] = prob[k];
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
								
				return classProb;
			}
		};
	}
	
	
	/**
	 * Force segmentator to use all available features
	 */
	public void useAllFeatures()
	{
		boolean[] enableFeatures = this.featureStack.getEnableFeatures();
		for (int i = 0; i < enableFeatures.length; i++)
			enableFeatures[i] = true;
		this.featureStack.setEnableFeatures(enableFeatures);
	}
	
	/**
	 * Set the temporary folder
	 * @param tempFolder complete path name for temporary folder 
	 */
	public void setTempFolder(final String tempFolder)
	{
		this.tempFolder = tempFolder;
	}

	
	/**
	 * Homogenize number of instances per class
	 * 
	 * @param data input set of instances
	 * @return resampled set of instances
	 */
	public static Instances homogenizeTrainingData(Instances data)
	{
		final Resample filter = new Resample();
		Instances filteredIns = null;
		filter.setBiasToUniformClass(1.0);
		try {
			filter.setInputFormat(data);
			filter.setNoReplacement(false);
			filter.setSampleSizePercent(100);
			filteredIns = Filter.useFilter(data, filter);			
		} catch (Exception e) {
			IJ.log("Error when resampling input data!");
			e.printStackTrace();
		}
		return filteredIns;
		
	}

	/**
	 * Homogenize number of instances per class (in the loaded training data) 
	 */
	public void homogenizeTrainingData()
	{
		final Resample filter = new Resample();
		Instances filteredIns = null;
		filter.setBiasToUniformClass(1.0);
		try {
			filter.setInputFormat(this.loadedTrainingData);
			filter.setNoReplacement(false);
			filter.setSampleSizePercent(100);
			filteredIns = Filter.useFilter(this.loadedTrainingData, filter);			
		} catch (Exception e) {
			IJ.log("Error when resampling input data!");
			e.printStackTrace();
		}
		this.loadedTrainingData = filteredIns;		
	}	
	
	/**
	 * Select attributes of current data by BestFirst search.
	 * The data is reduced to the selected attributes (features).
	 * 
	 * @return false if the current dataset is empty
	 */
	public boolean selectAttributes()
	{
		if(null == loadedTrainingData)
		{
			IJ.error("There is no data so select attributes from.");
			return false;		
		}
		// Select attributes by BestFirst
		loadedTrainingData = selectAttributes(loadedTrainingData);
		// Update list of features to use
		this.featureNames = new ArrayList<String>();
		IJ.log("Selected attributes:");
		for(int i = 0; i < loadedTrainingData.numAttributes(); i++)
		{
			this.featureNames.add(loadedTrainingData.attribute(i).name());
			IJ.log((i+1) + ": " + this.featureNames.get(i));
		}
		
		// force data (ARFF) update
		this.updateWholeData = true;
		
		return true;
	}
	
	/**
	 * Select attributes using BestFirst search to reduce 
	 * the number of parameters per instance of a dataset
	 * 
	 * @param data input set of instances
	 * @return resampled set of instances
	 */
	public static Instances selectAttributes(Instances data)
	{
		final AttributeSelection filter = new AttributeSelection();
		Instances filteredIns = null;
		// Evaluator
		final CfsSubsetEval evaluator = new CfsSubsetEval();
		evaluator.setMissingSeparate(true);
		// Assign evaluator to filter
		filter.setEvaluator(evaluator);
		// Search strategy: best first (default values)
		final BestFirst search = new BestFirst();
		filter.setSearch(search);
		// Apply filter
		try {
			filter.setInputFormat(data);
		
			filteredIns = Filter.useFilter(data, filter);			
		} catch (Exception e) {
			IJ.log("Error when resampling input data with selected attributes!");
			e.printStackTrace();
		}
		return filteredIns;
		
	}

	/**
	 * Get training error (from loaded data).
	 * 
	 * @param verbose option to display evaluation information in the log window
	 * @return classifier error on the training data set.
	 */
	public double getTrainingError(boolean verbose)
	{
		if(null == this.trainHeader)
			return -1;

		double error = -1;
		try {
			final Evaluation evaluation = new Evaluation(this.loadedTrainingData);
			evaluation.evaluateModel(classifier, this.loadedTrainingData);
			if(verbose)
				IJ.log(evaluation.toSummaryString("\n=== Training set evaluation ===\n", false));
			error = evaluation.errorRate();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return error;
	}
	
	/**
	 * Get test error of current classifier on a specific image and its binary labels
	 * 
	 * @param image input image
	 * @param labels binary labels
	 * @param whiteClassIndex index of the white class
	 * @param blackClassIndex index of the black class
	 * @param verbose option to display evaluation information in the log window
	 * @return pixel classification error
	 */
	public double getTestError(
			ImagePlus image, 
			ImagePlus labels, 
			int whiteClassIndex, 
			int blackClassIndex,
			boolean verbose)
	{
		IJ.showStatus("Creating features for test image...");
		if(verbose)
			IJ.log("Creating features for test image " + image.getTitle() +  "...");
		

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if( null == loadedClassNames )
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[i].size() > 0)
					classNames.add(classLabels[i]);
		}
		else
			classNames = loadedClassNames;
				
		
		// Apply labels
		final int height = image.getHeight();
		final int width = image.getWidth();
		final int depth = image.getStackSize();
					
		Instances testData = null;
		
		for(int z=1; z <= depth; z++)
		{
			final ImagePlus testSlice = new ImagePlus(image.getImageStack().getSliceLabel(z), image.getImageStack().getProcessor(z).convertToByte(true));
			// Create feature stack for test image
			IJ.showStatus("Creating features for test image...");
			if(verbose)
				IJ.log("Creating features for test image " + z +  "...");
			final FeatureStack testImageFeatures = new FeatureStack(testSlice);
			// Use the same features as the current classifier
			testImageFeatures.setEnableFeatures(featureStack.getEnableFeatures());
			testImageFeatures.setMaximumSigma(maximumSigma);
			testImageFeatures.setMinimumSigma(minimumSigma);
			testImageFeatures.setMembranePatchSize(membranePatchSize);
			testImageFeatures.setMembraneSize(membraneThickness);
			testImageFeatures.updateFeaturesMT();
			testImageFeatures.setUseNeighbors(featureStack.useNeighborhood());
			filterFeatureStackByList(this.featureNames, testImageFeatures);
			
			final Instances data = testImageFeatures.createInstances(classNames);
			data.setClassIndex(data.numAttributes()-1);
			if(verbose)
				IJ.log("Assigning classes based on the labels...");
			
			final ImageProcessor slice = labels.getImageStack().getProcessor(z);
			for(int n=0, y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
				{
					final double newValue = slice.getPixel(x, y) > 0 ? whiteClassIndex : blackClassIndex;
					data.get(n).setClassValue(newValue);
				}
			
			if(null == testData)
				testData = data;
			else
			{
				for(int i=0; i<data.numInstances(); i++)
					testData.add( data.get(i) );
			}
		}
		if(verbose)
			IJ.log("Evaluating test data...");
		
		double error = -1;
		try {
			final Evaluation evaluation = new Evaluation(testData);
			evaluation.evaluateModel(classifier, testData);
			if(verbose)
			{
				IJ.log(evaluation.toSummaryString("\n=== Test data evaluation ===\n", false));
				IJ.log(evaluation.toClassDetailsString() + "\n");
				IJ.log(evaluation.toMatrixString());
			}
			error = evaluation.errorRate();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return error;		
	}
	
	/**
	 * Display the threshold curve window (for precision/recall, ROC, etc.).
	 * 
	 * @param data input instances
	 * @param classifier classifier to evaluate
	 */
	public void displayGraphs(Instances data, AbstractClassifier classifier)
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
	
	// **********************
	// BLOTC-related  methods
	// **********************
	
	/**
	 * Train a FastRandomForest classifier using BLOTC:
	 * Boundary Learning by Optimization with Topological Constraints
	 * Jain, Bollmann, Richardson, Berger, Helmstaedter, Briggman, Denk, Bowden, 
	 * Mendenhall, Abraham, Harris, Kasthuri, Hayworth, Schalek, Tapia, Lichtman, and Seung. 
	 * IEEE Conference on Computer Vision and Pattern Recognition [CVPR 2010]
	 * 
	 *  @param image input image
	 *  @param labels corresponding binary labels
	 *  @param numOfTrees number of trees to use in the random forest
	 *  @param randomFeatures number of random features in the random forest
	 *  @param maxDepth maximum depth allowed in the trees
	 *  @param seed fast random forest seed
	 *  @param resample flag to resample input data (to homogenize classes distribution)
	 *  @param selectAttributes flag to select best attributes and reduce the data size
	 *  @return trained fast random forest classifier
	 */
	public static FastRandomForest trainRandomForestBLOTC(
			final ImagePlus image, 
			final ImagePlus labels, 
			final int numOfTrees,
			final int randomFeatures,
			final int maxDepth,
			final int seed,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Initialization of Fast Random Forest classifier
		final FastRandomForest rf = new FastRandomForest();
		rf.setNumTrees(numOfTrees);		
		rf.setNumFeatures(randomFeatures);
		rf.setMaxDepth(maxDepth);
		rf.setSeed(seed);
		
		ImagePlus result = trainBLOTC(image, labels, rf, resample, selectAttributes);
		result.show();
		
		return rf;
	}
	
	/**
	 * Train current classifier using BLOTC (non-static method)
	 * 
	 * @param image input image
	 * @param labels binary labels
	 * @param mask binary mask to use in the warping
	 * @param resample flag to resample input data (to homogenize classes distribution)
	 * @param selectAttributes flag to select best attributes and filter the data
	 * @return warped labels from applying BLOTC 
	 */
	public ImagePlus trainBLOTC(
			final ImagePlus image, 
			final ImagePlus labels,
			final ImagePlus mask,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Create a float copy of the labels
		final ImageStack warpedLabelStack = new ImageStack(image.getWidth(), image.getHeight());
		for(int i=1; i<=labels.getStackSize(); i++)
			warpedLabelStack.addSlice("warped label " + i, labels.getStack().getProcessor(i).duplicate().convertToFloat());
		ImagePlus warpedLabels = new ImagePlus("warped labels", warpedLabelStack);
						
		// At the moment, use all features
		String firstClass = classLabels[0];
		String secondClass = classLabels[1];
		
		double error = Double.MAX_VALUE;
		
		final int numOfPixelsPerImage = image.getWidth() * image.getHeight();
				
		IJ.log("Adding labels to training data set...");
		
		// Add all labels as binary data (each input slice)
		addBinaryData(image, labels, secondClass, firstClass);
		
		Instances originalData = this.loadedTrainingData;
		
		// Reduce data size by selecting attributes
		if(selectAttributes)
		{
			// Reduce size of data by attribute selection			
			IJ.log("Selecting best attributes...");			
			final long start = System.currentTimeMillis();									
			selectAttributes();		
			final long end = System.currentTimeMillis();
			originalData = this.loadedTrainingData;
			IJ.log("Filtered data: " + originalData.numInstances() 
					+ " instances, " + originalData.numAttributes() 
					+ " attributes, " + originalData.numClasses() + " classes.");
			IJ.log("Filtering training data took: " + (end-start) + "ms");
		}
		
		Instances trainingData = originalData;
		
		// homogenize classes if resample is true
		if(resample)
		{
			// Resample data			
			IJ.log("Resampling input data (to homogenize the class distributions)...");			
			trainingData = homogenizeTrainingData(trainingData);			
			setLoadedTrainingData(trainingData);
		}
		
		// train BLOTC		
		int iter = 1;
		while(true)
		{
			IJ.log("BLOTC training...");
					
			// Train classifier with current ground truth
			trainClassifier();
			
			double newError = getTrainingError(true);
			
			IJ.log("BLOTC iteration " + iter + ": training error = " + newError);
			
			if(newError >= error)
				break;
			
			error = newError;
						
			final ImageStack proposalStack = new ImageStack(image.getWidth(), image.getHeight());
			
			for(int i=1; i<=image.getStackSize(); i++)
			{
				final Instances subDataSet = new Instances (originalData, (i-1)*numOfPixelsPerImage, numOfPixelsPerImage); 
				IJ.log("Calculating class probability for whole image " + i + "...");
				ImagePlus result = getProbabilityMapsMT(subDataSet, image.getWidth(), image.getHeight());
				proposalStack.addSlice("probability map " + i, result.getImageStack().getProcessor(2));
			}
									
			final ImagePlus proposal = new ImagePlus("proposal", proposalStack);
		
			//warpedLabels.show();
			//proposal.show();
			
			IJ.log("Warping ground truth...");
			
			final ArrayList<Point3f>[] mismatches = new ArrayList[image.getStackSize()];
			
			// Warp ground truth, relax original labels to proposal. Only simple
			// points warping is allowed.
			warpedLabels = simplePointWarp2dMT(warpedLabels, proposal, mask, 0.5, mismatches);

			// Update training data with warped labels
			if(!resample)
				udpateDataClassification(warpedLabels, secondClass, firstClass);
			else
			{
				IJ.log("Resampling training data...");
				updateDataClassification(originalData, warpedLabels, 1, 0, mismatches);
				trainingData = homogenizeTrainingData(originalData);
				setLoadedTrainingData(trainingData);
			}
			
			if(null != this.tempFolder)
			{
				final File temp = new File(tempFolder);
				if(null != temp && temp.exists())
				{
					saveClassifier(tempFolder + "/classifier-" + iter + ".model");
					IJ.saveAs(warpedLabels, "Tiff", tempFolder + "/warped-labels-" + iter + ".tif");	
				}
			}
			
			iter++;						
		}
		return warpedLabels;
	}
	
	
	
	/**
	 * Train a classifier using BLOTC (static method)
	 * 
	 * @param image input image
	 * @param labels binary labels
	 * @param classifier Weka classifier
	 * @param resample flag to resample input data (to homogenize classes distribution)
	 * @param selectAttributes flag to select best attributes and filter the data
	 * @return warped labels from applying BLOTC 
	 */
	public static ImagePlus trainBLOTC(
			final ImagePlus image, 
			final ImagePlus labels, 
			final AbstractClassifier classifier,
			final boolean resample,
			final boolean selectAttributes)
	{
		// Create a float copy of the labels
		final ImageStack warpedLabelStack = new ImageStack(image.getWidth(), image.getHeight());
		for(int i=1; i<=labels.getStackSize(); i++)
			warpedLabelStack.addSlice("warped label " + i, labels.getStack().getProcessor(i).duplicate().convertToFloat());
		ImagePlus warpedLabels = new ImagePlus("warped labels", warpedLabelStack);
		
		// Create segmentation project
		final Weka_Segmentation seg = new Weka_Segmentation(image);
		
		if( null != classifier )
			seg.setClassifier(classifier);
		
		// At the moment, use all features
		seg.useAllFeatures();
		String firstClass = seg.classLabels[0];
		String secondClass = seg.classLabels[1];
		
		double error = Double.MAX_VALUE;
		
		final int numOfPixelsPerImage = image.getWidth() * image.getHeight();
				
		IJ.log("Adding labels to training data set...");
		
		// Add all labels as binary data (each input slice)
		// class 2 = white, class 1 = black
		seg.addBinaryData(image, labels, secondClass, firstClass);
		
		Instances originalData = seg.getTrainingInstances();
		
		// Reduce data size by selecting attributes
		if(selectAttributes)
		{
			// Reduce size of data by attribute selection			
			IJ.log("Selecting best attributes...");			
			final long start = System.currentTimeMillis();									
			originalData = selectAttributes(originalData);		
			final long end = System.currentTimeMillis();
			seg.setLoadedTrainingData(originalData);
			IJ.log("Filtered data: " + originalData.numInstances() 
					+ " instances, " + originalData.numAttributes() 
					+ " attributes, " + originalData.numClasses() + " classes.");
			IJ.log("Filtering training data took: " + (end-start) + "ms");
		}
		
		Instances trainingData = originalData;
		
		// homogenize classes if resample is true
		if(resample)
		{
			// Resample data			
			IJ.log("Resampling input data (to homogenize the class distributions)...");			
			trainingData = homogenizeTrainingData(trainingData);
			
			seg.setLoadedTrainingData(trainingData);
		}
		
		// train using BLOTC
		int iter = 1;
		while(true)
		{
			IJ.log("BLOTC training...");
					
			// Train classifier with current ground truth
			seg.trainClassifier();
			
			double newError = seg.getTrainingError(true);
			
			IJ.log("BLOTC iteration " + iter + ": training error = " + newError);
			
			if(newError >= error)
				break;
			
			error = newError;
						
			final ImageStack proposalStack = new ImageStack(image.getWidth(), image.getHeight());
			
			for(int i=1; i<=image.getStackSize(); i++)
			{
				final Instances subDataSet = new Instances (originalData, (i-1)*numOfPixelsPerImage, numOfPixelsPerImage); 
				//final ImagePlus result = seg.applyClassifier(subDataSet, image.getWidth(), image.getHeight());
				//proposalStack.addSlice("classification result " + i, result.getProcessor().convertToFloat());
				IJ.log("Calculating class probability for whole image " + i + "...");
				ImagePlus result = seg.getProbabilityMapsMT(subDataSet, image.getWidth(), image.getHeight());
				proposalStack.addSlice("probability map " + i, result.getImageStack().getProcessor(2));
			}
									
			final ImagePlus proposal = new ImagePlus("proposal", proposalStack);
		
			//warpedLabels.show();
			//proposal.show();
			IJ.log("Warping ground truth...");
			
			final ArrayList<Point3f>[] mismatches = new ArrayList[image.getStackSize()];
			
			// Warp ground truth, relax original labels to proposal. Only simple
			// points warping is allowed.
			warpedLabels = seg.simplePointWarp2dMT(warpedLabels, proposal, null, 0.5, mismatches);

			// Update training data with warped labels
			if(!resample)
				seg.udpateDataClassification(warpedLabels, secondClass, firstClass);
			else
			{
				IJ.log("Resampling training data...");
				updateDataClassification(originalData, warpedLabels, 1, 0);
				trainingData = homogenizeTrainingData(originalData);
				seg.setLoadedTrainingData(trainingData);
			}
				
			iter++;
		}
		return warpedLabels;
	}
	
	/**
	 * Update the class attribute of "loadedTrainingData" from 
	 * the input binary labels. The number of instances of "loadedTrainingData" 
	 * must match the size of the input labels image (or stack)
	 * 
	 * @param labels input binary labels (single image or stack) 
	 * @param className1 name of the white (different from 0) class
	 * @param className2 name of the black (0) class
	 */
	public void udpateDataClassification(
			ImagePlus labels,
			String className1,
			String className2)
	{
				
		// Detect class indexes
		int classIndex1 = 0;
		for(classIndex1 = 0 ; classIndex1 < this.classLabels.length; classIndex1++)
			if(className1.equalsIgnoreCase(this.classLabels[classIndex1]))
				break;
		if(classIndex1 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className1 + "' not found.");
			return;
		}
		int classIndex2 = 0;
		for(classIndex2 = 0 ; classIndex2 < this.classLabels.length; classIndex2++)
			if(className2.equalsIgnoreCase(this.classLabels[classIndex2]))
				break;
		if(classIndex2 == this.classLabels.length)
		{
			IJ.log("Error: class named '" + className2 + "' not found.");
			return;
		}
		
		updateDataClassification(this.loadedTrainingData, labels, classIndex1, classIndex2);				
	}
	
	/**
	 * Update the class attribute of "data" from 
	 * the input binary labels. The number of instances of "data" 
	 * must match the size of the input labels image (or stack)
	 * 
	 * @param data input instances 
	 * @param labels binary labels
	 * @param classIndex1 index of the white (different from 0) class
	 * @param classIndex2 index of the black (0) class
	 */
	public static void updateDataClassification(
			Instances data,
			ImagePlus labels,
			int classIndex1,
			int classIndex2)
	{
		// Check sizes
		final int size = labels.getWidth() * labels.getHeight() * labels.getStackSize();
		if (size != data.numInstances())
		{
			IJ.log("Error: labels size does not match loaded training data set size.");
			return;
		}
		
		final int width = labels.getWidth();
		final int height = labels.getHeight();
		final int depth = labels.getStackSize();
		// Update class with new labels
		for(int n=0, z=1; z <= depth; z++)
		{
			final ImageProcessor slice = labels.getImageStack().getProcessor(z);			
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
					data.get(n).setClassValue(slice.getPixel(x, y) > 0 ? classIndex1 : classIndex2);
					
		}
	}

	/**
	 * Update the class attribute of "data" from 
	 * the input binary labels. The number of instances of "data" 
	 * must match the size of the input labels image (or stack)
	 * 
	 * @param data input instances 
	 * @param labels binary labels
	 * @param classIndex1 index of the white (different from 0) class
	 * @param classIndex2 index of the black (0) class
	 */
	public static void updateDataClassification(
			Instances data,
			ImagePlus labels,
			int classIndex1,
			int classIndex2,
			ArrayList<Point3f>[] mismatches)
	{
		// Check sizes
		final int size = labels.getWidth() * labels.getHeight() * labels.getStackSize();
		if (size != data.numInstances())
		{
			IJ.log("Error: labels size does not match loaded training data set size.");
			return;
		}
		
		final int width = labels.getWidth();
		final int height = labels.getHeight();
		final int depth = labels.getStackSize();
		// Update class with new labels
		for(int n=0, z=1; z <= depth; z++)
		{
			final ImageProcessor slice = labels.getImageStack().getProcessor(z);			
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++, n++)
				{
					final double newValue = slice.getPixel(x, y) > 0 ? classIndex1 : classIndex2;
					/*
					// reward matching with previous value...
					if(data.get(n).classValue() == newValue)
					{
						double weight = data.get(n).weight();
						data.get(n).setWeight(++weight);	
					}
					*/
					data.get(n).setClassValue(newValue);
				}

		}
		/*
		if(null !=  mismatches)
			for(int i=0; i<depth; i++)
			{
				IJ.log("slice " + i + ": " + mismatches[i].size() + " mismatches");
				
				for(Point3f p : mismatches[i])
				{
					//IJ.log("point = " + p);
					final int n = (int) p.x + ((int) p.y -1) * width + i * (width*height);
					double weight = data.get(n).weight();
					data.get(n).setWeight(++weight);
				}
			}
			*/
	}
	

	
	/**
	 * Calculate warping error
	 * 
	 * @param label original labels (single image or stack)
	 * @param proposal proposed new labels
	 * @param mask image mask
	 * @param binaryThreshold binary threshold to binarize proposal
	 * @return total warping error
	 */
	public static double warpingError(
			ImagePlus label,
			ImagePlus proposal,
			ImagePlus mask,
			double binaryThreshold)
	{		
		final ImagePlus warpedLabels = simplePointWarp2d(label, proposal, mask, binaryThreshold);
	
		if(null == warpedLabels)
			return -1;				
			
		double error = 0;
		double count = 0;
		
		
		for(int j=1; j<=proposal.getImageStackSize(); j++)
		{
			final float[] proposalPixels = (float[])proposal.getImageStack().getProcessor(j).getPixels();
			final float[] warpedPixels = (float[])warpedLabels.getImageStack().getProcessor(j).getPixels();
			for(int i=0; i<proposalPixels.length; i++)				
			{
				count ++;
				final float thresholdedProposal = (proposalPixels[i] > binaryThreshold) ? 1.0f : 0.0f;
				if (warpedPixels[i] != thresholdedProposal)
					error++;
			}
			
		}
		
		if(count != 0)
			return error / count;
		else
			return -1;
	}
	
	/**
	 * Use simple point relaxation to warp 2D source into 2D target. 
	 * Source is only modified at nonzero locations in the mask
	 * 
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image
	 */
	public static ImagePlus simplePointWarp2d(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}
		
		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;
		
		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());
		
		double warpingError = 0;
		for(int i = 1; i <= sourceSlices.getSize(); i++)
		{
			WarpingResults wr = simplePointWarp2d(sourceSlices.getProcessor(i), 
					targetSlices.getProcessor(i), null != mask ? maskSlices.getProcessor(i) : null, 
					binaryThreshold);
			if(null != wr.warpedSource)
				warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());	
			if(wr.warpingError != -1)
				warpingError += wr.warpingError;
		}
		
		IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));
		
		return new ImagePlus("warped source", warpedSource);
	}

	/**
	 * Use simple point relaxation to warp 2D source into 2D target. 
	 * Source is only modified at nonzero locations in the mask 
	 * (multi-thread version)
	 * 
	 * @param source input image to be relaxed
	 * @param target target image
	 * @param mask image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image
	 */
	public ImagePlus simplePointWarp2dMT(
			ImagePlus source,
			ImagePlus target,
			ImagePlus mask,
			double binaryThreshold,
			ArrayList<Point3f>[] mismatches)
	{
		if(source.getWidth() != target.getWidth()
				|| source.getHeight() != target.getHeight()
				|| source.getImageStackSize() != target.getImageStackSize())
		{
			IJ.log("Error: label and training image sizes do not fit.");
			return null;
		}
		
				
		final ImageStack sourceSlices = source.getImageStack();
		final ImageStack targetSlices = target.getImageStack();
		final ImageStack maskSlices = (null != mask) ? mask.getImageStack() : null;
		
		final ImageStack warpedSource = new ImageStack(source.getWidth(), source.getHeight());
		
		if(null == mismatches)
			mismatches = new ArrayList[sourceSlices.getSize()];
		
		// Executor service to produce concurrent threads
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		final ArrayList< Future<WarpingResults> > futures = new ArrayList< Future<WarpingResults> >();
		
		try{
			for(int i = 1; i <= sourceSlices.getSize(); i++)
			{
				futures.add(exe.submit( simplePointWarp2DConcurrent(sourceSlices.getProcessor(i), 
										targetSlices.getProcessor(i), 
										null != maskSlices ? maskSlices.getProcessor(i) : null, 
										binaryThreshold ) ) );
			}
			
			double warpingError = 0;
			int i = 0;
			// Wait for the jobs to be done
			for(Future<WarpingResults> f : futures)
			{
				final WarpingResults wr = f.get();
				if(null != wr.warpedSource)
					warpedSource.addSlice("warped source " + i, wr.warpedSource.getProcessor());	
				if(wr.warpingError != -1)
					warpingError += wr.warpingError;
				if(null != wr.mismatches)
					mismatches[i] = wr.mismatches;				
				i++;
			}
			IJ.log("Warping error = " + (warpingError / sourceSlices.getSize()));
		}
		catch(Exception ex)
		{
			IJ.log("Error when warping ground truth in a concurrent way.");
			ex.printStackTrace();
		}
		finally{
			exe.shutdown();
		}
		
		return new ImagePlus("warped source", warpedSource);
	}
	
	
	/**
	 * Calculate the simple point warping in a concurrent way
	 * (to be submitted to an Executor Service)
	 * @param source moving image
	 * @param target fixed image
	 * @param mask mask image
	 * @param binaryThreshold binary threshold to use
	 * @return warping results (warped labels, warping error value and mismatching points)
	 */
	public Callable<WarpingResults> simplePointWarp2DConcurrent(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			final double binaryThreshold)
	{
		return new Callable<WarpingResults>(){
			public WarpingResults call(){
		
				return simplePointWarp2d(source, target, mask, binaryThreshold);
			}
		};
	}
	
	
	/**
	 * Results from simple point warping (2D)
	 * 
	 */
	public static class WarpingResults{
		/** warped source image after 2D simple point relaxation */
		public ImagePlus warpedSource;
		/** warping error */
		public double warpingError;
		
		public ArrayList<Point3f> mismatches;
	}
	
	/**
	 * Use simple point relaxation to warp 2D source into 2D target. 
	 * Source is only modified at nonzero locations in the mask
	 * 
	 * @param source input 2D image to be relaxed
	 * @param target target 2D image
	 * @param mask 2D image mask
	 * @param binaryThreshold binarization threshold
	 * @return warped source image and warping error
	 */
	public static WarpingResults simplePointWarp2d(
			final ImageProcessor source,
			final ImageProcessor target,
			final ImageProcessor mask,
			double binaryThreshold)
	{
		if(binaryThreshold < 0 || binaryThreshold > 1)
			binaryThreshold = 0.5;
		
		// Grayscale target
		final ImagePlus targetReal;// = new ImagePlus("target_real", target.duplicate());
		// Binarized target
		final ImagePlus targetBin; // = new ImagePlus("target_aux", target.duplicate());
		
		final ImagePlus sourceReal; // = new ImagePlus("source_real", source.duplicate());
		
		final ImagePlus maskReal; // = (null != mask) ? new ImagePlus("mask_real", mask.duplicate().convertToFloat()) : null;
		
		final int width = target.getWidth();
		final int height = target.getHeight();
		
		// Resize canvas to avoid checking the borders
		//IJ.run(targetReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
		ImageProcessor ip = target.createProcessor(width+2, height+2);
		ip.insert(target, 1, 1);
		targetReal = new ImagePlus("target_real", ip.duplicate());
		
		// IJ.run(targetBin, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");		
		targetBin = new ImagePlus("target_aux", ip.duplicate());
		
		// IJ.run(sourceReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
		ip = target.createProcessor(width+2, height+2);
		ip.insert(source, 1, 1);
		sourceReal = new ImagePlus("source_real", ip.duplicate());
		
		if(null != mask)
		{
			//IJ.run(maskReal, "Canvas Size...", "width="+ (width + 2) + " height=" + (height + 2) + " position=Center zero");
			ip = target.createProcessor(width+2, height+2);
			ip.insert(mask, 1, 1);
			maskReal = new ImagePlus("mask_real", ip.duplicate());			
		}
		else{
			maskReal = null;
		}
		
		// make sure source and target are binary images
		final float[] sourceRealPix = (float[])sourceReal.getProcessor().getPixels();
		for(int i=0; i < sourceRealPix.length; i++)
			if(sourceRealPix[i] > 0)
				sourceRealPix[i] = 1.0f;

		final float[] targetBinPix = (float[])targetBin.getProcessor().getPixels();
		for(int i=0; i < targetBinPix.length; i++)
			targetBinPix[i] = (targetBinPix[i] > binaryThreshold) ? 1.0f : 0.0f;
						
		double diff = Double.MIN_VALUE;
		double diff_before = 0;
		
		final WarpingResults result = new WarpingResults();
		
		while(true)
		{
			ImageProcessor missclass_points_image = sourceReal.getProcessor().duplicate();
			missclass_points_image.copyBits(targetBin.getProcessor(), 0, 0, Blitter.DIFFERENCE);  
			
			diff_before = diff;
			
			// Count mismatches
			float pixels[] = (float[]) missclass_points_image.getPixels();			
			float mask_pixels[] = (null != maskReal) ? (float[]) maskReal.getProcessor().getPixels() : new float[pixels.length];
			if(null == maskReal)
				Arrays.fill(mask_pixels, 1f);
			
			diff = 0;
			for(int k = 0; k < pixels.length; k++)
				if(pixels[k] != 0 && mask_pixels[k] != 0)
					diff ++;
									
			//IJ.log("Difference = " + diff);
		
			if(diff == diff_before || diff == 0)
				break;
			
			final ArrayList<Point3f> mismatches = new ArrayList<Point3f>();
			
			final float[] realTargetPix = (float[])targetReal.getProcessor().getPixels();
			
			// Sort mismatches by the absolute value of the target pixel value - threshold
			for(int x = 1; x < width+1; x++)
				for(int y = 1; y < height+1; y++)
				{
					if(pixels[x+y*(width+2)] != 0 && mask_pixels[x+y*(width+2)] != 0)
						mismatches.add(new Point3f(x , y , (float) Math.abs( realTargetPix[x+y*(width+2)] - binaryThreshold) ));
				}									
			
			// Sort mismatches in descending order
			Collections.sort(mismatches,  new Comparator<Point3f>() {
			    public int compare(Point3f o1, Point3f o2) {
			        return (int)((o2.z - o1.z) *10000);
			    }});
			
			// Process mismatches
			for(final Point3f p : mismatches)
			{
				final int x = (int) p.x;
				final int y = (int) p.y;
				
				if(p.z < SIMPLE_POINT_THRESHOLD)
					continue;

				double[] val = new double[]{
						sourceRealPix[ (x-1) + (y-1) * (width+2) ],
						sourceRealPix[ (x  ) + (y-1) * (width+2) ],
						sourceRealPix[ (x+1) + (y-1) * (width+2) ],
						sourceRealPix[ (x-1) + (y  ) * (width+2) ],
						sourceRealPix[ (x  ) + (y  ) * (width+2) ],
						sourceRealPix[ (x+1) + (y  ) * (width+2) ],
						sourceRealPix[ (x-1) + (y+1) * (width+2) ],
						sourceRealPix[ (x  ) + (y+1) * (width+2) ],
						sourceRealPix[ (x+1) + (y+1) * (width+2) ]
				};
										
				final double pix = val[4];

				final ImagePlus patch = new ImagePlus("patch", new FloatProcessor(3,3,val));
				if( simple2D(patch, 4) )
				{/*
							for(int i=0; i<9;i++)
								IJ.log(" " + val[i]);
							IJ.log("pix = " + pix);*/
					sourceRealPix[ x + y * (width+2)] =  pix > 0.0 ? 0.0f : 1.0f ;
					//IJ.log("flipping pixel x: " + x + " y: " + y + " to " + (pix > 0  ? 0.0 : 1.0));

				}
				
			}
			
			result.mismatches = mismatches;
			
			
		}
		
		//IJ.run(sourceReal, "Canvas Size...", "width="+ width + " height=" + height + " position=Center zero");
		ip = source.createProcessor(width, height);
		ip.insert(sourceReal.getProcessor(), -1, -1);
		sourceReal.setProcessor(ip.duplicate());
		
				
		result.warpedSource = sourceReal;
		result.warpingError = diff / (width * height);
		return result;
	}
	
	
	/**
	 * Check if a point is simple (in 2D)
	 * @param im input patch
	 * @param n neighbors
	 * @return true if the center pixel of the patch is a simple point
	 */
	public static boolean simple2D(ImagePlus im, int n)
	{
		final ImagePlus invertedIm = new ImagePlus("inverted", im.getProcessor().duplicate());
		//IJ.run(invertedIm, "Invert","");
		final float[] pix = (float[])invertedIm.getProcessor().getPixels();
		for(int i=0; i<pix.length; i++)
			pix[i] = pix[i] == 0f ? 1f : 0f;
		
		switch (n)
		{
			case 4:
				if ( topo(im,4)==1 && topo(invertedIm, 8)==1 )
	            	return true;
				else
					return false;				
			case 8:
				if ( topo(im,8)==1 && topo(invertedIm, 4)==1 )
					return true;
				else
					return false;
			default:
				IJ.error("Non valid adjacency value");
				return false;
		}
	}
	
	/**
	 * Computes topological numbers for the central point of an image patch.
	 * These numbers can be used as the basis of a topological classification.
	 * T_4 and T_8 are used when IM is a 2d image patch of size 3x3 
	 * defined on p. 172 of Bertrand & Malandain, Patt. Recog. Lett. 15, 169-75 (1994).
	 * 
	 * @param im input image
	 * @param adjacency number of neighbors
	 * @return number of components in the patch excluding the center pixel
	 */
	public static int topo(final ImagePlus im, final int adjacency)
	{
		ImageProcessor components = null;
		final ImagePlus im2 = new ImagePlus("copy of im", im.getProcessor().duplicate());
		switch (adjacency)
		{
			case 4:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=4 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point
				
				im2.getProcessor().set(1, 1, 0);
				components = connectedComponents(im2, adjacency).allRegions.getProcessor(); 
				// zero out locations that are not in the four-neighborhood
				components.set(0,0,0);
				components.set(0,2,0);
				components.set(1,1,0);
				components.set(2,0,0);
				components.set(2,2,0);
				break;	
			case 8:
				if( im.getStack().getSize() > 1 )
				{
					IJ.error("n=8 is valid for a 2d image");
					return -1;
				}
				if( im.getProcessor().getWidth() > 3 || im.getProcessor().getHeight() > 3)
				{
					IJ.error("must be 3x3 image patch");
					return -1;
				}
				// ignore the central point				
				im2.getProcessor().set(1, 1, 0);
				components = connectedComponents(im2, adjacency).allRegions.getProcessor();
				break;
			default:
				IJ.error("Non valid adjacency value");
				return -1;
		}
		
		if(null == components)
			return -1;
		
		int t = 0;
		ArrayList<Integer> uniqueId = new ArrayList<Integer>();
		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 3; j++)
			{
				if(( t = components.get(i, j) ) != 0)
					if(!uniqueId.contains(t))
						uniqueId.add(t);
			}
		
		return uniqueId.size();
		
	}

	/**
	 * Connected components based on Find Connected Regions (from Mark Longair)
	 * @param im input image
	 * @param adjacency number of neighbors to check (4, 8...)
	 * @return list of images per regsion, all-regions image and regions info 
	 */
	public static Results connectedComponents(final ImagePlus im, final int adjacency)
	{
		if( adjacency != 4 && adjacency != 8 )
			return null;
		
		final boolean diagonal = adjacency == 8 ? true : false;
		
		FindConnectedRegions fcr = new FindConnectedRegions();
		try {
			final Results r = fcr.run( im,
				 diagonal,
				 false,
				 true,
				 false,
				 false,
				 false,
				 false,
				 0,
				 1,
				 -1,
				 true /* noUI */ );
			return r;
			
		} catch( IllegalArgumentException iae ) {
			IJ.error(""+iae);
			return null;
		}

	}
	
}

