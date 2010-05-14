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

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import java.awt.AlphaComposite;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;
import hr.irb.fastRandomForest.FastRandomForest;

public class Trainable_Segmentation implements PlugIn 
{
	final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
	final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
	
	int overlayOpacity = 33;
	Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
	
	/** maximum number of classes (labels) allowed on the GUI*/
	private static final int MAX_NUM_CLASSES = 5;
	/** array of lists of Rois for each class */
	private List<Roi> [] examples = new ArrayList[MAX_NUM_CLASSES];
	/** image to be used in the training */
	private ImagePlus trainingImage;
	/** image to display on the GUI, it includes the painted rois */
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
	private Instances loadedTrainingData;
	/** current classifier */
	private AbstractClassifier classifier = null;
	/** default classifier (Fast Random Forest) */
	private FastRandomForest rf;
	/** flag to update the whole set of instances (used when there is any change on the features) */
	private boolean updateWholeData = true;
	
	/** train classifier button */
	JButton trainButton;
	/** toggle overlay button */
	JButton overlayButton;
	/** create result button */
	JButton resultButton;
	/** apply classifier button */
	JButton applyButton;
	/** load data button */
	JButton loadDataButton;
	/** save data button */
	JButton saveDataButton;
	/** settings button */
	JButton settingsButton;
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
	/** list of class names on the loaded data */
	ArrayList<String> loadedClassNames = null;
	/** executor service to launch threads for the plugin methods and events */
	final ExecutorService exec = Executors.newFixedThreadPool(1);

	/** GUI/no GUI flag */
	private boolean useGUI = true;
	
	/**
	 * Basic constructor
	 */
	public Trainable_Segmentation() 
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
		
		
		applyButton = new JButton ("Apply classifier");
		applyButton.setToolTipText("Load data and apply current classifier");
		applyButton.setEnabled(false);
		
		loadDataButton = new JButton ("Load data");
		loadDataButton.setToolTipText("Load previous segmentation from an ARFF file");
		
		saveDataButton = new JButton ("Save data");
		saveDataButton.setToolTipText("Save current segmentation into an ARFF file");

		addClassButton = new JButton ("Create new class");
		addClassButton.setToolTipText("Add one more label to mark different areas");
		
		settingsButton = new JButton ("Settings");
		settingsButton.setToolTipText("Display settings dialog");
		
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
	}

	/**
	 * Custom window to define the trainable segmentation GUI
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

			setTitle("Trainable Segmentation");
			
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
  		}private void saveFeatureStack() {
					// TODO Auto-generated method stub
					
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
			trainingImage = new ImagePlus("Trainable Segmentation",WindowManager.getCurrentImage().getProcessor().duplicate());
		

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
		displayImage.setProcessor("Trainable Segmentation", trainingImage.getProcessor().duplicate());

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
			applyButton.setEnabled(s);
			loadDataButton.setEnabled(s);
			saveDataButton.setEnabled(s);
			addClassButton.setEnabled(s);
			settingsButton.setEnabled(s);
			for(int i = 0 ; i < numOfClasses; i++)
			{
				exampleList[i].setEnabled(s);
				addExampleButton[i].setEnabled(s);
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

		IJ.log("\nTraining input:");
		
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
							x2  =p.xpoints[i]; 
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
								double[] values = new double[featureStack.getSize()+1];
								for (int z=1; z<=featureStack.getSize(); z++)
									values[z-1] = featureStack.getProcessor(z).getInterpolatedValue(x, y);
								values[featureStack.getSize()] = (double) l;
								trainingData.add(new DenseInstance(1.0, values));
								// increase number of instances for this class
								nl ++;
																
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
		if (nonEmpty < 2)
			IJ.log("Training from loaded data only...");
		else 
		{
			final long start = System.currentTimeMillis();
			data = createTrainingInstances();
			final long end = System.currentTimeMillis();
			IJ.log("Creating training data took: " + (end-start) + "ms");
			data.setClassIndex(data.numAttributes() - 1);
		}

		if (loadedTrainingData != null && data != null){
			IJ.log("Merging data...");
			for (int i=0; i < loadedTrainingData.numInstances(); i++)
				data.add(loadedTrainingData.instance(i));
			IJ.log("Finished");
		}
		else if (data == null)
		{
			data = loadedTrainingData;
			IJ.log("Taking loaded data as only data...");
		}

		IJ.showStatus("Training classifier...");
		IJ.log("Training classifier...");
		if (null == data){
			IJ.log("WTF");
		}
		
		// Train the classifier on the current data
		final long start = System.currentTimeMillis();
		try{
			classifier.buildClassifier(data);
		}
		catch(Exception e){
			IJ.showMessage(e.getMessage());
			e.printStackTrace();
			return;
		}
		final long end = System.currentTimeMillis();
		final DecimalFormat df = new DecimalFormat("0.0000");
		
		final String outOfBagError = (rf != null) ? ", out of bag error: " + df.format(rf.measureOutOfBagError()) : "";
		IJ.log("Finished training in "+(end-start)+"ms"+ outOfBagError);
		
		if(updateWholeData)
		{
			updateTestSet();
			IJ.log("Test dataset updated ("+ wholeImageData.numInstances() + " instances)");
		}

		IJ.log("Classifying whole image...");

		classifiedImage = applyClassifier(wholeImageData, trainingImage.getWidth(), trainingImage.getHeight());

		IJ.log("Finished segmentation of whole image.");
		
		if(useGUI)
		{
			overlayButton.setEnabled(true);
			resultButton.setEnabled(true);
			applyButton.setEnabled(true);
			showColorOverlay = false;
			toggleOverlay();
			setButtonsEnabled(true);
		}
		
		
		//featureStack.show();
	}
	
	/**
	 * Update whole data set with current number of classes and features
	 */
	private void updateTestSet() 
	{
		IJ.showStatus("Reading whole image data...");
		
		long start = System.currentTimeMillis();
		ArrayList<String> classNames = null;
		
		if(loadedTrainingData != null)
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
	public ImagePlus applyClassifier(final Instances data, int w, int h)
	{
		IJ.showStatus("Classifying image...");
						
		final long start = System.currentTimeMillis();

		final int numOfProcessors = Runtime.getRuntime().availableProcessors();
		final ExecutorService exe = Executors.newFixedThreadPool(numOfProcessors);
		final double[][] results = new double[numOfProcessors][];
		final Instances[] partialData = new Instances[numOfProcessors];
		final int partialSize = data.numInstances() / numOfProcessors;
		Future<double[]> fu[] = new Future[numOfProcessors];
		
		final AtomicInteger counter = new AtomicInteger();
		
		//IJ.log("Dividing dataset into subsets for parallel execution...");
		
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
		
		//IJ.log("Waiting for jobs...");
		
		// Join threads
		for(int i = 0; i<numOfProcessors; i++)
		{
			try {
				results[i] = fu[i].get();
			} catch (InterruptedException e) {
				IJ.log("Interruption exception");
				e.printStackTrace();
				return null;
			} catch (ExecutionException e) {
				IJ.log("Execution exception");
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
		if (showColorOverlay)
		{
			
			ImageProcessor overlay = classifiedImage.getProcessor().duplicate();
			//classifiedImage.show();
			
			double shift = 255.0 / MAX_NUM_CLASSES;
			overlay.multiply(shift+1);
			overlay = overlay.convertToByte(false);
			overlay.setColorModel(overlayLUT);
			
			///new ImagePlus("Overlay", overlay).show();
			
			resultOverlay.setImage(overlay);
		}
		else
			resultOverlay.setImage(null);

		displayImage.updateAndDraw();
		
		//drawExamples();
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
	 * Apply classifier to test data
	 */
	public void applyClassifierToTestData()
	{
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

		// Create feature stack for test image
		IJ.showStatus("Creating features for test image...");
		final FeatureStack testImageFeatures = new FeatureStack(testImage);
		// Use the same features as the current classifier
		testImageFeatures.setEnableFeatures(featureStack.getEnableFeatures());
		testImageFeatures.updateFeatures();

		// Set proper class names (skip empty list ones)
		ArrayList<String> classNames = new ArrayList<String>();
		if(loadedTrainingData == null)
		{
			for(int i = 0; i < numOfClasses; i++)
				if(examples[i].size() > 0)
					classNames.add(classLabels[i]);
		}
		else
			classNames = loadedClassNames;
		
		final Instances testData = testImageFeatures.createInstances(classNames);
		testData.setClassIndex(testData.numAttributes() - 1);

		final ImagePlus testClassImage = applyClassifier(testData, testImage.getWidth(), testImage.getHeight());
		testClassImage.setTitle("classified_" + testImage.getTitle());
		testClassImage.setProcessor(testClassImage.getProcessor().convertToByte(true).duplicate());

		return testClassImage;
	}

	/**
	 * Load previously saved data
	 */
	public void loadTrainingData()
	{
		OpenDialog od = new OpenDialog("Choose data file","");
		if (od.getFileName()==null)
			return;				
		loadTrainingData(od.getDirectory() + od.getFileName());
		
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
		
		if(featureStack.getSize() < 2)
		{
			setButtonsEnabled(false);
			featureStack.updateFeatures();
			setButtonsEnabled(true);
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
		IJ.log("Writing training data: " + data.numInstances() + " instances...");
		writeDataToARFF(data, sd.getDirectory() + sd.getFileName());
		IJ.log("Wrote training data: " + sd.getDirectory() + sd.getFileName());
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
		
		gd.addMessage("General options:");
		//FIXME normalization
		//gd.addCheckbox("Normalize data", this.featureStack.isNormalized());
		
		gd.addMessage("Fast Random Forest settings:");
		gd.addNumericField("Number of trees:", numOfTrees, 0);
		gd.addNumericField("Random features", randomFeatures, 0);
		
		gd.addMessage("Class names:");
		for(int i = 0; i < numOfClasses; i++)
			gd.addStringField("Class "+(i+1), classLabels[i], 15);
		
		gd.addMessage("Advanced options:");
		gd.addButton("Save feature stack", new ButtonListener("Select location to save feature stack", featureStack));
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
		//FIXME normalization
		// Normalization
		//final boolean normalize = gd.getNextBoolean();
		final boolean normalize = false;
		
		// Read fast random forest parameters and check if changed
		final int newNumTrees = (int) gd.getNextNumber();
		final int newRandomFeatures = (int) gd.getNextNumber();
		
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
			
		// Update random forest if necessary
		if(newNumTrees != numOfTrees ||	newRandomFeatures != randomFeatures)
			updateClassifier(newNumTrees, newRandomFeatures);
		
		// Update feature stack if necessary
		if(featuresChanged || normalize != this.featureStack.isNormalized())
		{
			this.setButtonsEnabled(false);
			this.featureStack.setNormalize(normalize);
			this.featureStack.setEnableFeatures(newEnableFeatures);
			this.featureStack.updateFeatures();
			this.setButtonsEnabled(true);
			// Force whole data to be updated
			updateWholeData = true;
		}
		
		return true;
	}
	
	/**
	 * Button listener class to handle the button action from the 
	 * settings dialog 
	 */
	static class ButtonListener implements ActionListener 
	{
		String title;
		TextField text;
		FeatureStack featureStack;

		public ButtonListener(String title, FeatureStack featureStack) 
		{
			this.title = title;
			this.featureStack = featureStack;
		}

		public void actionPerformed(ActionEvent e) 
		{
			if(featureStack.isEmpty())
			{
				IJ.error("Error", "The feature stack has not been initialized yet, please train first.");
				return;
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
	 * @return false if error
	 */
	private boolean updateClassifier(int newNumTrees, int newRandomFeatures) 
	{
		if(newNumTrees < 1 || newRandomFeatures < 1)
			return false;
		numOfTrees = newNumTrees;
		randomFeatures = newRandomFeatures;
		
		rf.setNumTrees(numOfTrees);
		rf.setNumFeatures(randomFeatures);
		
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
	public Trainable_Segmentation(ImagePlus trainingImage) 
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
			this.setButtonsEnabled(false);
			this.featureStack.setEnableFeatures(usedFeatures);
			this.featureStack.updateFeatures();
			this.setButtonsEnabled(true);
			// Force whole data to be updated
			updateWholeData = true;
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
	
	
}
