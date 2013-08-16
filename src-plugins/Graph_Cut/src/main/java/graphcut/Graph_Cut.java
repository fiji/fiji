package graphcut;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
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
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.Arrays;
import java.util.Vector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.util.gui.OverlayedImageCanvas;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.ImageWindow;

import ij.io.FileInfo;

import ij.plugin.PlugIn;

import ij.process.ImageProcessor;
import ij.process.LUT;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;

/**
 * Graph_Cut plugin
 *
 * This is the interface plugin to the graph cut algorithm for images as
 * proposed by Boykkov and Kolmogorov in:
 *
 *		"An Experimental Comparison of Min-Cut/Max-Flow Algorithms for Energy
 *		Minimization in Vision."
 *		Yuri Boykov and Vladimir Kolmogorov
 *		In IEEE Transactions on Pattern Analysis and Machine
 *		Intelligence (PAMI),
 *		September 2004
 *
 * The GUI implementation reuses code/ideas of the Trainable Segmentation
 * plugin.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 */





/**
 * Plugin interface to the graph cut algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Graph_Cut<T extends RealType<T>> implements PlugIn {

	// the image the gui was started with
	private ImagePlus imp;

	// the edge image
	private ImagePlus edge;

	// the segmentation image for the gui
	private ImagePlus seg;

	// the sequence segmentation image
	private ImagePlus seq;

	// the potts weight
	private float dataWeight   = DATA_INIT;
	private float pottsWeight  = POTTS_INIT;
	private float edgeWeight   = EDGE_INIT;
	private float edgeVariance = EDGE_VARIANCE_INIT;

	// Indicates that edge weights are given implicity as gray-scale differences
	// of the edge image. This will be false for edge images with 2n-1 pixels
	// per dimension of the imp image. In this case, the edge weights are stored
	// directly in the edge image between the doubled pixel coordinates, e.g.,
	// [2x,2y,...] and [x+2,y,...].
	private boolean implicitEdgeWeights = true;

	private static final float DATA_SCALE = 0.01f;
	private static final int DATA_MIN  = 0;
	private static final int DATA_MAX  = 100;
	private static final float DATA_INIT = DATA_SCALE*((float)DATA_MAX/2.0f);

	private static final float POTTS_SCALE = 0.01f;
	private static final int POTTS_MIN  = 0;
	private static final int POTTS_MAX  = 1000;
	private static final float POTTS_INIT = POTTS_SCALE*((float)POTTS_MAX/2.0f);

	private static final float EDGE_SCALE = 0.1f;
	private static final int EDGE_MIN  = 0;
	private static final int EDGE_MAX  = 1000;
	private static final float EDGE_INIT = EDGE_SCALE*((float)EDGE_MAX/2.0f);

	private static final float EDGE_VARIANCE_SCALE = 0.1f;
	private static final int EDGE_VARIANCE_MIN  = 0;
	private static final int EDGE_VARIANCE_MAX  = 1000;
	private static final float EDGE_VARIANCE_INIT = EDGE_VARIANCE_SCALE*((float)EDGE_VARIANCE_MAX/2.0f);

	// use an eight connected neighborhood?
	private boolean eightConnect = true;

	// the GUI window
	private GraphCutWindow win;

	// the segmentation overlay
	private ImageOverlay resultOverlay;

	// color look up table for the segmentation overlay
	private LUT overlayLUT;

	// the image to show in the GUI
	private ImagePlus displayImage;

	// trasparency of the overlay
	private float overlayAlpha = 0.5f;

	// show the segmentation overlay?
	private boolean showColorOverlay = false;

	// the whole GUI
	private Panel all = new Panel();

	// panel for the left side of the GUI
	private JPanel applyPanel;

	// panel containing all buttons
	private JPanel buttonsPanel;

	// panel containing the potts slider
	private JPanel dataPanel;
	private JPanel pottsPanel;
	private JPanel edgesPanel;
	private JPanel edgeVariancePanel;
	private JPanel edgeSelectorPanel;

	// start graph cut button
	private JButton applyButton;

	// create a parameter sequence button
	private JButton sequenceButton;

	// start graph cut on several files
	private JButton batchButton;

	// toggle segmentation overlay button
	private JButton overlayButton;

	// slider to adjust the data weight
	private JSlider dataSlider;

	// slider to adjust the potts weight
	private JSlider pottsSlider;

	// slider to adjust the edge weight
	private JSlider edgeSlider;

	// slider to adjust the edge image value variance 
	private JSlider edgeVarianceSlider;

	// combo box to select the edge image
	private JComboBox edgeSelector;


	/**
	 * Custom canvas to deal with zooming an panning.
	 *
	 * (shamelessly stolen from the Trainable_Segmentation plugin)
	 */
	@SuppressWarnings("serial")
	private class CustomCanvas extends OverlayedImageCanvas {
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
	 * Custom window to define the graph cut GUI
	 */
	@SuppressWarnings("serial")
	private class GraphCutWindow extends ImageWindow {

		// executor service to launch threads for the plugin methods and events
		final ExecutorService exec = Executors.newFixedThreadPool(1);
	
		// action listener
		private ActionListener actionListener = new ActionListener() {
	
			public void actionPerformed(final ActionEvent e) {
				// listen to the buttons on separate threads not to block
				// the event dispatch thread
				exec.submit(new Runnable() {
					public void run() 
					{
						if(e.getSource() == applyButton)
						{
							try{
								setButtonsEnabled(false);
								final long start = System.currentTimeMillis();
								updateSegmentationImage();
								final long end = System.currentTimeMillis();
								seg.show();
								seg.updateAndDraw();
								IJ.log("Total time: " + (end - start) + "ms");
								showColorOverlay = false;
								toggleOverlay();
							}catch(Exception e){
								e.printStackTrace();
							}finally{
								setButtonsEnabled(true);
							}
						}
						if(e.getSource() == sequenceButton)
						{
							try{
								setButtonsEnabled(false);
								final long start = System.currentTimeMillis();
								createSequence();
								final long end = System.currentTimeMillis();
								seq.show();
								seq.updateAndDraw();
								IJ.log("Total time: " + (end - start) + "ms");
							}catch(Exception e){
								e.printStackTrace();
							}finally{
								setButtonsEnabled(true);
							}
						}
						else if(e.getSource() == overlayButton){
							toggleOverlay();
						} else if (e.getSource() == batchButton) {
							batchProcessImages();
						}

						if (e.getSource() == edgeSelector)
							edge = (ImagePlus)edgeSelector.getSelectedItem();
					}
				});
			}
		};
	
		// change listener for sliders
		private ChangeListener changeListener = new ChangeListener() {
	
			public void stateChanged(final ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (e.getSource() == pottsSlider)
					pottsWeight = source.getValue()*POTTS_SCALE;
				if (e.getSource() == edgeSlider)
					edgeWeight = source.getValue()*EDGE_VARIANCE_SCALE;
				if (e.getSource() == edgeVarianceSlider)
					edgeVariance = source.getValue()*EDGE_VARIANCE_SCALE;
				if (e.getSource() == dataSlider)
					dataWeight = source.getValue()*DATA_SCALE;
			}
		};

		/**
		 * Creates the GUI
		 *
		 * @param imp The image that should be processed.
		 */
		GraphCutWindow(ImagePlus imp) {

			super(imp, new CustomCanvas(imp));
	
			applyButton = new JButton ("Segment image");
			applyButton.setToolTipText("Start the min-cut computation");
	
			batchButton = new JButton ("Batch process");
			batchButton.setToolTipText("Apply the plugin to several images");
	
			overlayButton = new JButton ("Toggle overlay");
			overlayButton.setToolTipText("Toggle the segmentation overlay in the image");

			sequenceButton = new JButton ("Create sequence");
			sequenceButton.setToolTipText("Create a sequence of segmentations with different parameters");
	
			dataSlider = new JSlider(JSlider.HORIZONTAL, DATA_MIN, DATA_MAX, (int)(DATA_INIT/DATA_SCALE));
			dataSlider.setToolTipText("Adjust the expected numbers of foreground pixels.");
			dataSlider.setMajorTickSpacing(500);
			dataSlider.setMinorTickSpacing(10);
			dataSlider.setPaintTicks(true);
			dataSlider.setPaintLabels(true);

			pottsSlider = new JSlider(JSlider.HORIZONTAL, POTTS_MIN, POTTS_MAX, (int)(POTTS_INIT/POTTS_SCALE));
			pottsSlider.setToolTipText("Adjust the smoothness of the segmentation.");
			pottsSlider.setMajorTickSpacing(500);
			pottsSlider.setMinorTickSpacing(10);
			pottsSlider.setPaintTicks(true);
			pottsSlider.setPaintLabels(true);
	
			edgeSlider = new JSlider(JSlider.HORIZONTAL, EDGE_MIN, EDGE_MAX, (int)(EDGE_INIT/EDGE_SCALE));
			edgeSlider.setToolTipText("Adjust the influence of the edge image.");
			edgeSlider.setMajorTickSpacing(500);
			edgeSlider.setMinorTickSpacing(10);
			edgeSlider.setPaintTicks(true);
			edgeSlider.setPaintLabels(true);

			edgeVarianceSlider = new JSlider(JSlider.HORIZONTAL, EDGE_VARIANCE_MIN, EDGE_VARIANCE_MAX, (int)(EDGE_VARIANCE_INIT/EDGE_VARIANCE_SCALE));
			edgeVarianceSlider.setToolTipText("Set the variance of the edge image.");
			edgeVarianceSlider.setMajorTickSpacing(500);
			edgeVarianceSlider.setMinorTickSpacing(10);
			edgeVarianceSlider.setPaintTicks(true);
			edgeVarianceSlider.setPaintLabels(true);

			Vector<ImagePlus> windowList = new Vector<ImagePlus>();
			final int[] windowIds = WindowManager.getIDList();

			windowList.add(null); // "no edge image"
			for (int i = 0; ((windowIds != null) && (i < windowIds.length)); i++) 
				windowList.add(WindowManager.getImage(windowIds[i]));

			edgeSelector = new JComboBox(windowList);
	
			resultOverlay = new ImageOverlay();
	
			// create the color look up table
			final byte[] red   = new byte[256];
			final byte[] green = new byte[256];
			final byte[] blue  = new byte[256];
			for(int i = 0 ; i < 256; i++)
			{
				if (i < 128) {
					red[i]   = (byte)255;
					green[i] = 0;
					blue[i]  = 0;
				} else {
					red[i]   = 0;
					green[i] = (byte)255;
					blue[i]  = 0;
				}
			}
			overlayLUT = new LUT(red, green, blue);

			// add the overlay with transparency
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha);
			resultOverlay.setComposite(composite);
			((OverlayedImageCanvas)ic).addOverlay(resultOverlay);
			
			// Remove the canvas from the window, to add it later
			removeAll();
	
			setTitle("Graph Cut");
			
			applyButton.addActionListener(actionListener);
			batchButton.addActionListener(actionListener);
			overlayButton.addActionListener(actionListener);
			sequenceButton.addActionListener(actionListener);
			dataSlider.addChangeListener(changeListener);
			pottsSlider.addChangeListener(changeListener);
			edgeSlider.addChangeListener(changeListener);
			edgeVarianceSlider.addChangeListener(changeListener);
			edgeSelector.addActionListener(actionListener);
	
			// Apply panel (left side of the GUI)
			applyPanel = new JPanel();
			applyPanel.setBorder(BorderFactory.createTitledBorder("Apply"));
			GridBagLayout applyLayout = new GridBagLayout();
			GridBagConstraints applyConstraints = new GridBagConstraints();
			applyConstraints.anchor = GridBagConstraints.NORTHWEST;
			applyConstraints.fill = GridBagConstraints.HORIZONTAL;
			applyConstraints.gridwidth = 1;
			applyConstraints.gridheight = 1;
			applyConstraints.gridx = 0;
			applyConstraints.gridy = 0;
			applyConstraints.insets = new Insets(5, 5, 6, 6);
			applyPanel.setLayout(applyLayout);
			
			applyPanel.add(applyButton, applyConstraints);
			applyConstraints.gridy++;
			applyPanel.add(batchButton, applyConstraints);
			applyConstraints.gridy++;
			applyPanel.add(overlayButton, applyConstraints);
			applyConstraints.gridy++;
			applyPanel.add(sequenceButton, applyConstraints);
			applyConstraints.gridy++;
	
			// Potts panel
			GridBagLayout dataLayout = new GridBagLayout();
			GridBagConstraints dataConstraints = new GridBagConstraints();
			dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createTitledBorder("Foreground bias"));
			dataPanel.setLayout(dataLayout);
			dataConstraints.anchor = GridBagConstraints.NORTHWEST;
			dataConstraints.fill = GridBagConstraints.HORIZONTAL;
			dataConstraints.gridwidth = 1;
			dataConstraints.gridheight = 1;
			dataConstraints.gridx = 0;
			dataConstraints.gridy = 0;
			dataPanel.add(dataSlider, dataConstraints);
			dataConstraints.gridy++;
			dataConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout pottsLayout = new GridBagLayout();
			GridBagConstraints pottsConstraints = new GridBagConstraints();
			pottsPanel = new JPanel();
			pottsPanel.setBorder(BorderFactory.createTitledBorder("Smoothness"));
			pottsPanel.setLayout(pottsLayout);
			pottsConstraints.anchor = GridBagConstraints.NORTHWEST;
			pottsConstraints.fill = GridBagConstraints.HORIZONTAL;
			pottsConstraints.gridwidth = 1;
			pottsConstraints.gridheight = 1;
			pottsConstraints.gridx = 0;
			pottsConstraints.gridy = 0;
			pottsPanel.add(pottsSlider, pottsConstraints);
			pottsConstraints.gridy++;
			pottsConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout edgesLayout = new GridBagLayout();
			GridBagConstraints edgesConstraints = new GridBagConstraints();
			edgesPanel = new JPanel();
			edgesPanel.setBorder(BorderFactory.createTitledBorder("Edge image influence"));
			edgesPanel.setLayout(edgesLayout);
			edgesConstraints.anchor = GridBagConstraints.NORTHWEST;
			edgesConstraints.fill = GridBagConstraints.HORIZONTAL;
			edgesConstraints.gridwidth = 1;
			edgesConstraints.gridheight = 1;
			edgesConstraints.gridx = 0;
			edgesConstraints.gridy = 0;
			edgesPanel.add(edgeSlider, edgesConstraints);
			edgesConstraints.gridy++;
			edgesConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout edgeVarianceLayout = new GridBagLayout();
			GridBagConstraints edgeVarianceConstraints = new GridBagConstraints();
			edgeVariancePanel = new JPanel();
			edgeVariancePanel.setBorder(BorderFactory.createTitledBorder("Edge image decay"));
			edgeVariancePanel.setLayout(edgeVarianceLayout);
			edgeVarianceConstraints.anchor = GridBagConstraints.NORTHWEST;
			edgeVarianceConstraints.fill = GridBagConstraints.HORIZONTAL;
			edgeVarianceConstraints.gridwidth = 1;
			edgeVarianceConstraints.gridheight = 1;
			edgeVarianceConstraints.gridx = 0;
			edgeVarianceConstraints.gridy = 0;
			edgeVariancePanel.add(edgeVarianceSlider, edgeVarianceConstraints);
			edgeVarianceConstraints.gridy++;
			edgeVarianceConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout edgeSelectorLayout = new GridBagLayout();
			GridBagConstraints edgeSelectorConstraints = new GridBagConstraints();
			edgeSelectorPanel = new JPanel();
			edgeSelectorPanel.setBorder(BorderFactory.createTitledBorder("Edge image"));
			edgeSelectorPanel.setLayout(edgeSelectorLayout);
			edgeSelectorConstraints.anchor = GridBagConstraints.NORTHWEST;
			edgeSelectorConstraints.fill = GridBagConstraints.HORIZONTAL;
			edgeSelectorConstraints.gridwidth = 1;
			edgeSelectorConstraints.gridheight = 1;
			edgeSelectorConstraints.gridx = 0;
			edgeSelectorConstraints.gridy = 0;
			edgeSelectorPanel.add(edgeSelector, edgeSelectorConstraints);
			edgeSelectorConstraints.gridy++;
			edgeSelectorConstraints.insets = new Insets(5, 5, 6, 6);

			// Buttons panel
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsPanel = new JPanel();
			buttonsPanel.setLayout(buttonsLayout);
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsConstraints.gridwidth = 1;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsPanel.add(applyPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(dataPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(pottsPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(edgesPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(edgeVariancePanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(edgeSelectorPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsConstraints.insets = new Insets(5, 5, 6, 6);

			// everything panel
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);
	
			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.HORIZONTAL;
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

			final CustomCanvas canvas = (CustomCanvas) getCanvas();
			all.add(canvas, allConstraints);
	
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
					exec.shutdownNow();
					applyButton.removeActionListener(actionListener);
					batchButton.removeActionListener(actionListener);
					overlayButton.removeActionListener(actionListener);
					sequenceButton.removeActionListener(actionListener);
					pottsSlider.removeChangeListener(changeListener);
					edgeSlider.removeChangeListener(changeListener);
					edgeSelector.removeActionListener(actionListener);
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
		 * Toggle between overlay and original image
		 */
		void toggleOverlay() {
			showColorOverlay = !showColorOverlay;
	
			if (showColorOverlay) {
				
				ImageProcessor overlay = seg.getProcessor().duplicate();
				
				double shift = 127.0;
				overlay.multiply(shift+1);
				overlay = overlay.convertToByte(false);
				overlay.setColorModel(overlayLUT);
				
				resultOverlay.setImage(overlay);
			}
			else
				resultOverlay.setImage(null);
	
			displayImage.updateAndDraw();
		}
	}

	public void run(String arg) {

		IJ.log("Starting plugin Graph Cut");

		// read image
		imp   = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Please open an image first.");
			return;
		}
		int channels = imp.getNChannels();
		if (channels > 1) {

			int channel = 0;
			while (channel <= 0 || channel > channels)
				channel = (int)IJ.getNumber("Please give the number of the channel you wish to consider for the segmentation (1 - "  + channels + "):", 1);

			imp = extractChannel(imp, channel);
		}

		// start GUI
		displayImage = new ImagePlus();
		displayImage.setProcessor("Graph Cut", imp.getProcessor().duplicate());
	
		IJ.log("Starting GUI...");
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						IJ.log("Creating window...");
						win = new GraphCutWindow(displayImage);
						win.pack();
					}
				});
	}

	/**
	 * Processes a single channel image.
	 *
	 * The intensities of the image are interpreted as the probability of each
	 * pixel to belong to the foreground. The potts weight represents an
	 * isotropic edge weight.
	 *
	 * @param imp         The image to process
	 * @param edge        An edge image that increases the likelihood for cuts
	 *                    between certain pixels (can be null).
	 * @param pottsWeight Isotropic edge weights.
	 * @param edgeWeight  The influence of the edge image.
	 * @return A binary segmentation image
	 */
	public ImagePlus processSingleChannelImage(ImagePlus imp, ImagePlus edge, float dataWeight, float pottsWeight, float edgeWeight) {
		
		// prepare segmentation image
		int[] dimensions    = imp.getDimensions();
		int width   = dimensions[0];
		int height  = dimensions[1];
		int zslices = dimensions[3];

		ImagePlus seg = IJ.createImage(imp.getTitle() + " GraphCut segmentation", "8-bit",
		                               width, height, zslices);

		// fill it with the segmentation
		processSingleChannelImage(imp, edge, dataWeight, pottsWeight, edgeWeight, seg);

		return seg;
	}

	/**
	 * Processes a single channel image.
	 *
	 * The intensities of the image are interpreted as the probability of each
	 * pixel to belong to the foreground. The potts weight represents an
	 * isotropic edge weight.
	 *
	 * @param imp         The image to process
	 * @param edge        An edge image that increases the likelihood for cuts
	 *                    between certain pixels (can be null).
	 * @param pottsWeight Isotropic edge weights.
	 * @param edgeWeight  The influence of the edge image.
	 * @param seg         A grayscale image to store the binary result.
	 */
	public void processSingleChannelImage(ImagePlus imp, ImagePlus edge, float dataWeight, float pottsWeight, float edgeWeight, ImagePlus seg) {

		float maxValue     = (float)Math.pow(2, imp.getBitDepth());
		Image<T> image     = ImagePlusAdapter.wrap(imp);
		Image<T> edgeImage = null;
		if (edge != null)
			edgeImage = ImagePlusAdapter.wrap(edge);

		// get some statistics
		int[] dimensions = image.getDimensions();
		int   numNodes   = image.size();
		int   numEdges   = 0;

		// determine type of edge image
		if (edge != null) {
			int[] edgeDimensions = edge.getDimensions();
			if (edgeDimensions[0] == 2*dimensions[0] - 1)
				implicitEdgeWeights = false;
			else
				implicitEdgeWeights = true;
		}

		// compute number of edges
		if (eightConnect) {

			// straight and diagonal edges

			// n = (2*a-1)*(2*b-1)*...
			//   - a*b*c*...
			//   + (a-1)*(b-1)*...
			int prod1 = 1;
			for (int d = 0; d < dimensions.length; d++)
				prod1 *= (2*dimensions[d] - 1);

			int prod2 = 1;
			for (int d = 0; d < dimensions.length; d++)
				prod2 *= dimensions[d];

			int prod3 = 1;
			for (int d = 0; d < dimensions.length; d++)
				prod3 *= (dimensions[d] - 1);

			numEdges = prod1 - prod2 + prod3;
		} else {

			// straight edges
			for (int d = 0; d < dimensions.length; d++)
				numEdges += numNodes - numNodes/dimensions[d];
		}

		// setup imglib cursors
		LocalizableByDimCursor<T> cursor     = image.createLocalizableByDimCursor();
		LocalizableByDimCursor<T> edgeCursor = null;
		int[] imagePosition                  = new int[dimensions.length];

		// create a new graph cut instance
		// TODO: reuse an old one
		IJ.log("Creating graph structure of " + numNodes + " nodes and " + numEdges + " edges...");
		long start = System.currentTimeMillis();
		GraphCut graphCut = new GraphCut(numNodes, numEdges);
		long end   = System.currentTimeMillis();
		IJ.log("...done. (" + (end - start) + "ms)");

		// set terminal weights, i.e., segmentation probabilities
		IJ.log("Setting terminal weights with data prior " + dataWeight + "...");
		start = System.currentTimeMillis();
		while (cursor.hasNext()) {

			cursor.fwd();
			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition, dimensions);
			
			T type = cursor.getType();
			float value = type.getRealFloat();

			float probData  = (value/maxValue);
			float probPrior = dataWeight;
			float fweight = -(float)Math.log(probData) - (float)Math.log(probPrior);
			float bweight = -(float)Math.log(1.0 - probData) - (float)Math.log(1.0 - probPrior);

			graphCut.setTerminalWeights(nodeNum, fweight, bweight);
		}
		end = System.currentTimeMillis();
		IJ.log("...done. (" + (end - start) + "ms)");

		// set edge weights

		// create neighbor offsets
		int[][] neighborPositions;

		if (eightConnect) {

			int numNeighbors = dimensions.length*2;
			int numDiagonal = 1;
			for (int d = 0; d < dimensions.length; d++)
				numDiagonal *= 2;
			numNeighbors += numDiagonal;
			numNeighbors /= 2; // consider only half of the edges per pixel

			IJ.log("num neighbors per pixel: " + numNeighbors);

			neighborPositions = new int[numNeighbors][dimensions.length];

			Arrays.fill(neighborPositions[0], -1);

			for (int i = 1; i < neighborPositions.length; i++) {

				System.arraycopy(neighborPositions[i-1], 0, neighborPositions[i], 0, dimensions.length);

				boolean valid = false;
				do {

					for (int d = dimensions.length - 1; d >= 0; d--) {

						neighborPositions[i][d]++;
						if (neighborPositions[i][d] < 2)
							break;
						neighborPositions[i][d] = -1;
					}

					// check if valid neighbor
					for (int d = dimensions.length - 1; d >= 0; d--) {

						if (neighborPositions[i][d] < 0) {
							valid = true;
							break;
						}
					}

				} while (!valid);
				System.out.println(Arrays.toString(neighborPositions[i]));
			}
		} else {

			neighborPositions = new int[dimensions.length][dimensions.length];

			for (int d = 0; d < dimensions.length; d++) {
				Arrays.fill(neighborPositions[d], 0);
				neighborPositions[d][d] = -1;
			}
		}

		IJ.log("Setting edge weights to " + pottsWeight + "...");
		if (edge != null) {
			IJ.log("   (under consideration of edge image with weight " + edgeWeight + ")");
			if (implicitEdgeWeights)
				cursor   = edgeImage.createLocalizableByDimCursor();
			else {
				cursor     = image.createLocalizableByDimCursor();
				edgeCursor = edgeImage.createLocalizableByDimCursor();
			}
		} else
			cursor   = image.createLocalizableByDimCursor();

		int[] neighborPosition = new int[dimensions.length];
		int[] edgePosition     = new int[dimensions.length];
		int e = 0;
		start = System.currentTimeMillis();

		while (cursor.hasNext()) {

			cursor.fwd();

			// image position
			cursor.getPosition(imagePosition);
			int nodeNum = listPosition(imagePosition, dimensions);

			float value = cursor.getType().getRealFloat();

A:			for (int i = 0; i < neighborPositions.length; i++) {

				for (int d = 0; d < dimensions.length; d++) {

					neighborPosition[d] = imagePosition[d]   + neighborPositions[i][d];
					edgePosition[d]     = 2*imagePosition[d] + neighborPositions[i][d];

					if (neighborPosition[d] < 0 || neighborPosition[d] >= dimensions[d])
						continue A;
				}

				int neighborNum = listPosition(neighborPosition, dimensions);

				float weight = pottsWeight;

				if (edge != null) {

					if (implicitEdgeWeights) {

						cursor.setPosition(neighborPosition);
						float neighborValue = cursor.getType().getRealFloat();

						// TODO:
						// cache neighbor distances
						weight += edgeWeight*edgeLikelihood(value, neighborValue, imagePosition, neighborPosition, dimensions);
					} else {

						edgeCursor.setPosition(edgePosition);
						float edgeValue = edgeCursor.getType().getRealFloat();

						// TODO:
						// cache neighbor distances
						weight += edgeWeight*edgeLikelihood(0, edgeValue, imagePosition, neighborPosition, dimensions);
					}
				}

				// add weight_00 to source weight of node,
				// add weight_11 to sink weight of neighbor,
				// set edge weight node->neighbor to weight_10,
				// set edge weight neighbor->node to weight_01 - weight_00 -
				// weight_11
				//
				// since weight_00 = weight_11 = 0 and weight_01 = weight_10 =
				// weight, the following does it:
				graphCut.setEdgeWeight(nodeNum, neighborNum, weight);
				e++;
			}

			cursor.setPosition(imagePosition);
		}
		end = System.currentTimeMillis();
		IJ.log("...done inserting " + e + " edges. (" + (end - start) + "ms)");

		// calculate max flow
		IJ.log("Calculating max flow...");
		start = System.currentTimeMillis();
		float maxFlow = graphCut.computeMaximumFlow(false, null);
		end = System.currentTimeMillis();
		IJ.log("...done. Max flow is " + maxFlow + ". (" + (end - start) + "ms)");

		Image<T> segmentation = ImagePlusAdapter.wrap(seg);

		// create segmentation image
		cursor = segmentation.createLocalizableByDimCursor();
		imagePosition = new int[dimensions.length];
		while (cursor.hasNext()) {

			cursor.fwd();

			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition, dimensions);

			if (graphCut.getTerminal(nodeNum) == Terminal.FOREGROUND)
				cursor.getType().setReal(255.0);
			else
				cursor.getType().setReal(0.0);
		}
	}

	public ImagePlus createSequenceImage(ImagePlus imp, ImagePlus edge,
	                                     float dataStart, float dataStop, float dataStep,
	                                     float pottsWeight, float edgeWeight) {

		// prepare sequence image
		int[] dimensions    = imp.getDimensions();
		int width   = dimensions[0];
		int height  = dimensions[1];
		int zslices = dimensions[3];
		int frames  = (int)((dataStop - dataStart)/dataStep) + 1;

		ImageStack seqStack = new ImageStack(width, height);

		final int numThreads = Runtime.getRuntime().availableProcessors() + 1;

		class ImageProcessingThread extends Thread {

			ImageStack result;

			final ImagePlus imp;
			final ImagePlus edge;

			final float dataStart;
			final float numSteps;
			final float dataStep;
			final float pottsWeight;
			final float edgeWeight;

			public ImageProcessingThread(final ImagePlus imp, final ImagePlus edge,
			                             final float dataStart, final int numSteps, final float dataStep,
			                             final float pottsWeight, final float edgeWeight) {

				this.imp  = imp;
				this.edge = edge;

				this.dataStart = dataStart;
				this.numSteps  = numSteps;
				this.dataStep  = dataStep;

				this.pottsWeight = pottsWeight;
				this.edgeWeight  = edgeWeight;
			}

			public void run() {

				result = new ImageStack(imp.getWidth(), imp.getHeight());

				float dataWeight = dataStart;
				for (int i = 0; i < numSteps; i++) {

					IJ.log("Processing data weight " + dataWeight + "...");
					IJ.showProgress((float)i/numSteps);
					ImagePlus seg = processSingleChannelImage(imp, edge, dataWeight, pottsWeight, edgeWeight);
					for (int s = 0; s < seg.getStack().getSize(); s++)
						result.addSlice("", seg.getStack().getProcessor(s+1));

					dataWeight += dataStep;
				}
			}

			public ImageStack getResult() {
				return result;
			}
		}

		Vector<ImageProcessingThread> threads = new Vector<ImageProcessingThread>(numThreads);

		int numSteps = frames/numThreads;

		for (int i = 0; i < numThreads; i++) {

			float start  = dataStart + dataStep*(i*numSteps + 1);

			if (i == numThreads - 1)
				numSteps = frames - (numThreads - 1)*numSteps;

			IJ.log("Starting thread " + i + " from " + start + ", " + numSteps + " steps (step " + dataStep + ")");
			threads.add(new ImageProcessingThread(imp, edge, start, numSteps, dataStep, pottsWeight, edgeWeight));
			threads.get(i).start();
		}

		for (int i = 0; i < numThreads; i++)
			try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		IJ.showProgress(1.0);

		for (ImageProcessingThread ipt : threads) {

			ImageStack result = ipt.getResult();
			IJ.log("Merging result with " + result.getSize() + " slices...");

			for (int s = 0; s < result.getSize(); s++)
				seqStack.addSlice("", result.getProcessor(s+1));
		}

		ImagePlus seq = new ImagePlus(imp.getTitle() + " sequence segmentation " + dataStart + " - " + dataStop, seqStack);
		seq.setDimensions(1, zslices, frames);
		seq.setOpenAsHyperStack(true);

		return seq;
	}

	/**
	 * Apply graph cut to several images
	 */
	public void batchProcessImages()
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

			int decision = JOptionPane.showConfirmDialog(null, "You decided to process three or more image files. Do you want the results to be stored on the disk instead of opening them in Fiji?", "Save results?", JOptionPane.YES_NO_OPTION);

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

					ImagePlus batchImage = IJ.openImage(file.getPath());

					// take first channel only if image has several channels
					if (batchImage.getNChannels() > 1)
						batchImage = extractChannel(batchImage, 1);

					IJ.log("Processing image " + file.getName() + " in thread " + numThread);

					ImagePlus segmentation = processSingleChannelImage(batchImage, null, dataWeight, pottsWeight, edgeWeight);

					if (showResults) {
						segmentation.show();
						batchImage.show();
					}

					if (storeResults) {
						String filename = storeDir + File.separator + file.getName();
						IJ.log("Saving results to " + filename);
						IJ.save(segmentation, filename);
						segmentation.close();
						batchImage.close();
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

		setButtonsEnabled(true);
	}

	private ImagePlus extractChannel(ImagePlus imp, int channel) {

		int width    = imp.getWidth();
		int height   = imp.getHeight();
		int zslices  = imp.getNSlices();
		int frames   = imp.getNFrames();

		FileInfo fileInfo         = imp.getOriginalFileInfo();

		// create empty stack
		ImageStack stack2 = new ImageStack(width, height);
		// create new ImagePlus for selected channel
		ImagePlus imp2 = new ImagePlus();
		imp2.setTitle("C" + channel + "-" + imp.getTitle());

		// copy slices
		for (int t = 1; t <= frames; t++)
			for (int z = 1; z <= zslices; z++) {
				int slice = imp.getStackIndex(channel, z, t);
				stack2.addSlice("", imp.getStack().getProcessor(slice));
			}

		imp2.setStack(stack2);
		imp2.setDimensions(1, zslices, frames);
		if (zslices*frames > 1)
			imp2.setOpenAsHyperStack(true);
		imp2.setFileInfo(fileInfo);

		return imp2;
	}

	private ImagePlus extractZSlice(ImagePlus imp, int zslice) {

		int width    = imp.getWidth();
		int height   = imp.getHeight();
		int channels = imp.getNChannels();
		int frames   = imp.getNFrames();

		FileInfo fileInfo         = imp.getOriginalFileInfo();

		// create empty stack
		ImageStack stack2 = new ImageStack(width, height);
		// create new ImagePlus for selected frame
		ImagePlus imp2 = new ImagePlus();
		imp2.setTitle("Z" + zslice + "-" + imp.getTitle());

		// copy slices
		for (int f = 1; f <= frames; f++)
			for (int c = 1; c <= channels; c++) {
				int slice = imp.getStackIndex(c, zslice, f);
				stack2.addSlice("", imp.getStack().getProcessor(slice));
			}

		imp2.setStack(stack2);
		imp2.setDimensions(channels, 1, frames);
		if (channels*frames > 1)
			imp2.setOpenAsHyperStack(true);
		imp2.setFileInfo(fileInfo);

		return imp2;
	}

	private void updateSegmentationImage() {

		if (seg == null)
			seg = processSingleChannelImage(imp, edge, dataWeight, pottsWeight, edgeWeight);
		else
			processSingleChannelImage(imp, edge, dataWeight, pottsWeight, edgeWeight, seg);
	}

	private void createSequence() {

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

			int decision = JOptionPane.showConfirmDialog(null, "You decided to process three or more image files. Do you want the results to be stored on the disk instead of opening them in Fiji?", "Save results?", JOptionPane.YES_NO_OPTION);

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

		GenericDialog gd = new GenericDialog("Sequence Parameter");
		gd.addNumericField("Start", 0.0, 3);
		gd.addNumericField("End",   1.0, 3);
		gd.addNumericField("Step",  0.01, 3);

		gd.showDialog();

		if (gd.wasCanceled())
			return;

		float start = (float)gd.getNextNumber();
		float end   = (float)gd.getNextNumber();
		float step  = (float)gd.getNextNumber();

		boolean zsliceByZslice     = false;
		boolean rememberDecision = false;

		for (int i = 0; i < imageFiles.length; i++) {

			File file = imageFiles[i];

			ImagePlus sequenceImage = IJ.openImage(file.getPath());
			ImagePlus edgeImage;

			int width    = sequenceImage.getWidth();
			int height   = sequenceImage.getHeight();
			int channels = sequenceImage.getNChannels();
			int zslices  = sequenceImage.getNSlices();
			int sequenceLength = -1;

			if (zslices > 1 && rememberDecision == false) {
				int decision = JOptionPane.showConfirmDialog(null, "Process image zslice by zslice (as opposed to as a whole)?", "Frame by frame?", JOptionPane.YES_NO_OPTION);

				if (decision == JOptionPane.YES_OPTION)
					zsliceByZslice = true;

				// presumably, a lot of images are to be processed. in this
				// case, don't bother the user again...
				if (storeResults)
					rememberDecision = true;
			}

			// create empty stack
			ImageStack resultStack = new ImageStack(width, height);

			for (int zslice = 1; zslice <= (zsliceByZslice ? zslices : 1); zslice++) {

				ImagePlus sequenceSlice = (zsliceByZslice ? extractZSlice(sequenceImage, zslice) : sequenceImage);

				// take first channel as probability map and second as edge prior
				// (if available)
				if (channels > 1) {
					edgeImage     = extractChannel(sequenceSlice, 2);
					sequenceSlice = extractChannel(sequenceSlice, 1);
				} else
					edgeImage     = edge;

				IJ.log("Processing image " + file.getName() +
				       (edgeImage != null ? " under consideration of edge image in " + edgeImage.getTitle() : "") +
				       "...");

				seq = createSequenceImage(sequenceSlice, edgeImage, start, end, step, pottsWeight, edgeWeight);
				if (sequenceLength == -1)
					sequenceLength = seq.getStackSize();

				// add all slices of the segmentation result
				for (int s = 0; s < seq.getStack().getSize(); s++)
					resultStack.addSlice("", seq.getStack().getProcessor(s+1), (zslice-1) + s*(zslice));
			}

			// create result image plus
			ImagePlus result = new ImagePlus();
			result.setTitle("sequence-" + sequenceImage.getTitle());
			result.setStack(resultStack);
			result.setDimensions(1, zslices, sequenceLength);
			if (zslices*sequenceLength > 1)
				result.setOpenAsHyperStack(true);

			if (showResults) {
				result.show();
				result.updateAndDraw();
			}

			if (storeResults) {
				String filename = storeDir + File.separator + file.getName();
				IJ.log("Saving results to " + filename);
				IJ.save(result, filename);
				if (!showResults)
					result.close();
			}

			sequenceImage.close();
		}
	}

	private float edgeLikelihood(float value1, float value2, int[] position1, int[] position2, int[] dimensions) {

		float dist = 0;
		for (int d = 0; d < dimensions.length; d++)
			dist += (position1[d] - position2[d])*(position1[d] - position2[d]);
		dist = (float)Math.sqrt(dist);

		return (float)Math.exp(-((value1 - value2)*(value1 - value2))/(2*edgeVariance))/dist;
	}

	private int listPosition(int[] imagePosition, int[] dimensions) {

		int pos = 0;
		int fac = 1;
		for (int d = 0; d < dimensions.length; d++) {
			pos += fac*imagePosition[d];
			fac *= dimensions[d];
		}
		return pos;
	}

	private void setButtonsEnabled(boolean enabled) {
		applyButton.setEnabled(enabled);
		batchButton.setEnabled(enabled);
		overlayButton.setEnabled(enabled);
		sequenceButton.setEnabled(enabled);
	}
}
