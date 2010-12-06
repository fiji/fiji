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
	/** reference to the segmentation backend */
	final WekaSegmentation wekaSegmentation;

	/** 50% alpha composite */
	final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
	/** 25% alpha composite */
	final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
	/** opacity (in %) of the result overlay image */
	int overlayOpacity = 33;
	/** alpha composite for the result overlay image */
	Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);

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

	/** Lookup table for the result overlay image */
	LUT overlayLUT;

	/** array of trace lists for every class */
	private java.awt.List exampleList[];
	/** array of buttons for adding each trace class */
	private JButton [] addExampleButton;

	/**
	 * Basic constructor for graphical user interface use
	 */
	public Weka_Segmentation()
	{
		// instantiate segmentation backend
		wekaSegmentation = new WekaSegmentation();

		// start with two classes
		wekaSegmentation.addClass();
		wekaSegmentation.addClass();

		// Create overlay LUT
		final byte[] red = new byte[256];
		final byte[] green = new byte[256];
		final byte[] blue = new byte[256];
		final int shift = 255 / WekaSegmentation.MAX_NUM_CLASSES;
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

		for(int i = 0; i < wekaSegmentation.getNumOfClasses() ; i++)
		{
			exampleList[i] = new java.awt.List(5);
			exampleList[i].setForeground(colors[i]);
		}
		numOfClasses = wekaSegmentation.getNumOfClasses();

		showColorOverlay = false;
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
						// Disable buttons until the training has finished
						setButtonsEnabled(false);

						try{
							if( wekaSegmentation.trainClassifier() ){
								wekaSegmentation.applyClassifier(false);
								classifiedImage = wekaSegmentation.getClassifiedImage();
								if(showColorOverlay)
									toggleOverlay();
								toggleOverlay();
							}
						}catch(Exception e){
							e.printStackTrace();
						}finally{
							updateButtonsEnabling();
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
			for(int i = 0; i < WekaSegmentation.MAX_NUM_CLASSES; i++)
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
					for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
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

	}

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
			trainingImage = new ImagePlus("Advanced Weka Segmentation",WindowManager.getCurrentImage().getProcessor().duplicate());


		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			if (!IJ.showMessageWithCancel("Warning", "At least one dimension of the image \n" +
					"is larger than 1024 pixels. \n" +
					"Feature stack creation and classifier training \n" +
					"might take some time depending on your computer.\n" +
			"Proceed?"))
				return;

		trainingImage.setProcessor("Advanced Weka Segmentation", trainingImage.getProcessor().duplicate().convertToByte(true));

		wekaSegmentation.loadNewImage(trainingImage);

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
		for(int i = 0 ; i < wekaSegmentation.getNumOfClasses(); i++)
		{
			exampleList[i].setEnabled(s);
			addExampleButton[i].setEnabled(s);
		}
	}

	/**
	 * Update buttons enabling depending on the current status of the plugin
	 */
	private void updateButtonsEnabling()
	{
		final boolean classifierExists =  null != wekaSegmentation.getClassifier();

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
		wekaSegmentation.addExample(i, r);
		exampleList[i].add("trace " + traceCounter[i]);
		traceCounter[i]++;
		drawExamples();
	}

	/**
	 * Draw the painted traces on the display image
	 */
	private void drawExamples()
	{

		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
		{
			roiOverlay[i].setColor(colors[i]);
			final ArrayList< Roi > rois = new ArrayList<Roi>();
			for (Roi r : wekaSegmentation.getExamples(i))
			{
				rois.add(r);
				//IJ.log("painted ROI: " + r + " in color "+ colors[i]);
			}
			roiOverlay[i].setRoi(rois);
		}
		displayImage.updateAndDraw();
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

			double shift = 255.0 / WekaSegmentation.MAX_NUM_CLASSES;
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

		for(int j = 0; j < wekaSegmentation.getNumOfClasses(); j++)
		{
			if (j == i)
			{
				final Roi newRoi = wekaSegmentation.getExamples(i).get(exampleList[i].getSelectedIndex());
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

		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			if (e.getSource() == exampleList[i])
			{
				//delete item from ROI
				int index = exampleList[i].getSelectedIndex();

				// kill Roi from displayed image
				if(displayImage.getRoi().equals( wekaSegmentation.getExamples(i).get(index) ))
					displayImage.killRoi();


				wekaSegmentation.getExamples(i).remove(index);
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
		wekaSegmentation.applyClassifier(true);
		final ImagePlus probImage = wekaSegmentation.getClassifiedImage();
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
		final Instances data;
		if (wekaSegmentation.getTraceTrainingData() == null)
			data = wekaSegmentation.getTraceTrainingData();
		else
			data = wekaSegmentation.getLoadedTrainingData();
		displayGraphs(data, wekaSegmentation.getClassifier());
		this.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
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

		final boolean probabilityMaps;

		int decision = JOptionPane.showConfirmDialog(null, "Create probability maps instead of segmentation?", "Probability maps?", JOptionPane.YES_NO_OPTION);
		if (decision == JOptionPane.YES_OPTION)
			probabilityMaps = true;
		else
			probabilityMaps = false;

		final int numProcessors     = Runtime.getRuntime().availableProcessors();
		final int numThreads        = Math.min(imageFiles.length, numProcessors);
		final int numFurtherThreads = (int)Math.ceil((double)(numProcessors - numThreads)/imageFiles.length) + 1;

		IJ.log("Processing " + imageFiles.length + " image files in " + numThreads + " threads....");

		setButtonsEnabled(false);

		Thread[] threads = new Thread[numThreads];

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

					ImagePlus segmentation = wekaSegmentation.applyClassifier(testImage, numFurtherThreads, probabilityMaps);

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
	 * Load a Weka classifier from a file
	 */
	public void loadClassifier()
	{
		int oldNumOfClasses = wekaSegmentation.getNumOfClasses();

		OpenDialog od = new OpenDialog("Choose Weka classifier file","");
		if (od.getFileName()==null)
			return;
		IJ.log("Loading Weka classifier from " + od.getDirectory() + od.getFileName() + "...");

		setButtonsEnabled(false);

		final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();


		// Try to load Weka model (classifier and train header)
		if( false == wekaSegmentation.loadClassifier(od.getDirectory() + od.getFileName()) )
		{
			IJ.error("Error when loading Weka classifier from file");
			wekaSegmentation.setClassifier(oldClassifier);
			updateButtonsEnabling();
			return;
		}

		IJ.log("Read header from " + od.getDirectory() + od.getFileName() + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")");

		if(wekaSegmentation.getTrainHeader().numAttributes() < 1)
		{
			IJ.error("Error", "No attributes were found on the model header");
			wekaSegmentation.setClassifier(oldClassifier);
			updateButtonsEnabling();
			return;
		}

		// update GUI
		int wekaNumOfClasses = wekaSegmentation.getNumOfClasses();
		while (numOfClasses < wekaNumOfClasses)
			win.addClass();
		for (int i = 0; i < numOfClasses; i++)
			addExampleButton[i].setText("Add to " + wekaSegmentation.getClassLabel(i));

		updateButtonsEnabling();
		repaintWindow();

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
		OpenDialog od = new OpenDialog("Choose new image", "");
		if (od.getFileName()==null)
			return;

		this.setButtonsEnabled(false);

		IJ.log("Loading image " + od.getDirectory() + od.getFileName() + "...");

		ImagePlus newImage = new ImagePlus(od.getDirectory() + od.getFileName());

		if( false == wekaSegmentation.loadNewImage( newImage ) )
		{
			IJ.error("Error while loading new image!");
			this.updateButtonsEnabling();
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
		wekaSegmentation.loadTrainingData(od.getDirectory() + od.getFileName());
	}

	/**
	 * Save training model into a file
	 */
	public void saveTrainingData()
	{
		SaveDialog sd = new SaveDialog("Choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;

		if(false == wekaSegmentation.saveData(sd.getDirectory() + sd.getFileName()))
			IJ.showMessage("There is no data to save");
	}


	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass()
	{
		if(wekaSegmentation.getNumOfClasses() == WekaSegmentation.MAX_NUM_CLASSES)
		{
			IJ.showMessage("Advanced Weka Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

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
		wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
		wekaSegmentation.addClass();

		// Add new class label and list
		win.addClass();

		repaintWindow();
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

		final boolean[] oldEnableFeatures = wekaSegmentation.getFeatureStack().getEnableFeatures();

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
			((TextField) gd.getNumericFields().get(0)).setEnabled(false);

		gd.addMessage("General options:");

		if( wekaSegmentation.getClassifier() instanceof FastRandomForest )
		{
			gd.addMessage("Fast Random Forest settings:");
			gd.addNumericField("Number of trees:", wekaSegmentation.getNumOfTrees(), 0);
			gd.addNumericField("Random features", wekaSegmentation.getNumRandomFeatures(), 0);
			gd.addNumericField("Max depth", wekaSegmentation.getMaxDepth(), 0);
		}
		else
		{
			String classifierName = (wekaSegmentation.getClassifier().getClass()).toString();
			int index = classifierName.indexOf(" ");
			classifierName = classifierName.substring(index + 1);
			gd.addMessage(classifierName + " settings");
			gd.addButton("Set Weka classifier options", new ClassifierSettingsButtonListener(wekaSegmentation.getClassifier()));
		}


		gd.addMessage("Class names:");
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			gd.addStringField("Class "+(i+1), wekaSegmentation.getClassLabel(i), 15);

		gd.addMessage("Advanced options:");
		gd.addCheckbox("Homogenize classes", wekaSegmentation.doHomogenizeClasses());
		gd.addButton("Save feature stack", new SaveFeatureStackButtonListener("Select location to save feature stack", wekaSegmentation.getFeatureStack()));
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
		if(newThickness != wekaSegmentation.getMembraneThickness())
		{
			featuresChanged = true;
			wekaSegmentation.setMembraneThickness(newThickness);
		}
		// Membrane patch size
		final int newPatch = (int) gd.getNextNumber();
		if(newPatch != wekaSegmentation.getMembranePatchSize())
		{
			featuresChanged = true;
			wekaSegmentation.setMembranePatchSize(newPatch);
		}
		// Field of view (minimum and maximum sigma/radius for the filters)
		final float newMinSigma = (float) gd.getNextNumber();
		if(newMinSigma != wekaSegmentation.getMinimumSigma() && newMinSigma > 0)
		{
			featuresChanged = true;
			wekaSegmentation.setMinimumSigma(newMinSigma);
		}

		final float newMaxSigma = (float) gd.getNextNumber();
		if(newMaxSigma != wekaSegmentation.getMaximumSigma() && newMaxSigma > wekaSegmentation.getMinimumSigma())
		{
			featuresChanged = true;
			wekaSegmentation.setMaximumSigma(newMaxSigma);
		}
		if(wekaSegmentation.getMinimumSigma() >= wekaSegmentation.getMaximumSigma())
		{
			IJ.error("Error in the field of view parameters: they will be reset to default values");
			wekaSegmentation.setMinimumSigma(0f);
			wekaSegmentation.setMaximumSigma(16f);
		}
		// Read fast random forest parameters and check if changed
		if( wekaSegmentation.getClassifier() instanceof FastRandomForest )
		{
			final int newNumTrees = (int) gd.getNextNumber();
			final int newRandomFeatures = (int) gd.getNextNumber();
			final int newMaxDepth = (int) gd.getNextNumber();

			// Update random forest if necessary
			if(newNumTrees != wekaSegmentation.getNumOfTrees()
					||	newRandomFeatures != wekaSegmentation.getNumRandomFeatures()
					||	newMaxDepth != wekaSegmentation.getMaxDepth())

				wekaSegmentation.updateClassifier(newNumTrees, newRandomFeatures, newMaxDepth);
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

			}
		}

		// Update flag to homogenize number of class instances
		wekaSegmentation.setDoHomogenizeClasses(gd.getNextBoolean());

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
			// Pack window to update buttons
			win.pack();
		}

		// Update feature stack if necessary
		if(featuresChanged)
		{
			//this.setButtonsEnabled(false);
			wekaSegmentation.getFeatureStack().setEnableFeatures(newEnableFeatures);
			// Force features to be updated
			wekaSegmentation.setFeaturesDirty();
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
}
