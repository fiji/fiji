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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

import ij.gui.ImageWindow;

import ij.io.FileInfo;


import ij.plugin.HyperStackReducer;
import ij.plugin.PlugIn;

import ij.process.ImageProcessor;
import ij.process.LUT;

import mpicbg.imglib.cursor.LocalizableByDimCursor;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;

import mpicbg.imglib.type.numeric.RealType;



/**
 * Plugin interface to the graph cut algorithm.
 *
 * @author Jan Funke <jan.funke@inf.tu-dresden.de>
 * @version 0.1
 */
public class Graph_Cut<T extends RealType<T>> implements PlugIn {

	// the image to process
	private Image<T> image;

	// the ImagePlus version of it
	private ImagePlus imp;

	// the segmentation image
	private Image<T> segmentation;

	// the ImagePlus version of it
	private ImagePlus seg;

	// image dimensions
	private int[] dimensions;

	// the graph cut implementation
	private GraphCut graphCut;

	// the number of nodes
	private int numNodes;

	// the number of edges
	private int numEdges;

	// the potts weight
	private float pottsWeight = POTTS_INIT;

	// min, max, and default value for potts weight
	private static final int POTTS_MIN  = 0;
	private static final int POTTS_MAX  = 1000;
	private static final int POTTS_INIT = POTTS_MAX/2;


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
	private JPanel pottsPanel;

	// start graph cut button
	private JButton applyButton;

	// toggle segmentation overlay button
	private JButton overlayButton;

	// slider to adjust the potts weight
	private JSlider pottsSlider;

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
								applyButton.setEnabled(false);
								processSingleChannelImage(pottsWeight);
								createSegmentationImage();
								showColorOverlay = false;
								toggleOverlay();
							}catch(Exception e){
								e.printStackTrace();
							}finally{
								applyButton.setEnabled(true);
							}
						}
						else if(e.getSource() == overlayButton){
							toggleOverlay();
						}
					}
				});
			}
		};
	
		// change listener for sliders
		private ChangeListener changeListener = new ChangeListener() {
	
			public void stateChanged(final ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if(e.getSource() == pottsSlider)
					pottsWeight = source.getValue();
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
	
			overlayButton = new JButton ("Toggle overlay");
			overlayButton.setToolTipText("Toggle the segmentation overlay in the image");

			pottsSlider = new JSlider(JSlider.HORIZONTAL, POTTS_MIN, POTTS_MAX, POTTS_INIT);
			pottsSlider.setToolTipText("Adjust the influence of the Potts term");
			pottsSlider.setMajorTickSpacing(500);
			pottsSlider.setMinorTickSpacing(1);
			pottsSlider.setPaintTicks(true);
			pottsSlider.setPaintLabels(true);
	
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
			overlayButton.addActionListener(actionListener);
			pottsSlider.addChangeListener(changeListener);
	
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
			applyPanel.add(overlayButton, applyConstraints);
			applyConstraints.gridy++;
	
			// Potts panel
			GridBagLayout pottsLayout = new GridBagLayout();
			GridBagConstraints pottsConstraints = new GridBagConstraints();
			pottsPanel = new JPanel();
			pottsPanel.setBorder(BorderFactory.createTitledBorder("Potts weight"));
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
			buttonsPanel.add(pottsPanel, buttonsConstraints);
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
					overlayButton.removeActionListener(actionListener);
					pottsSlider.removeChangeListener(changeListener);
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
		if (imp.getNChannels() > 1) {

			int width = imp.getWidth();
			int height = imp.getHeight();
			int channels = imp.getNChannels();
			int slices = imp.getNSlices();
			int frames = imp.getNFrames();
			int size = slices*frames;

			int channel = 0;
			while (channel <= 0 || channel > channels)
				channel = (int)IJ.getNumber("Please give the number of the channel you wish to consider for the segmentation (1 - "  + channels + "):", 1);

			FileInfo fileInfo         = imp.getOriginalFileInfo();
			HyperStackReducer reducer = new HyperStackReducer(imp);

			// create empty stack
			ImageStack stack2 = new ImageStack(width, height, size);
			// add first slice (just to create an ImagePlus)
			stack2.setPixels(imp.getProcessor().getPixels(), 1); 
			// create new ImagePlus for selected channel
			ImagePlus imp2 = new ImagePlus("C" + channel + "-" + imp.getTitle(), stack2);
			// remove content again
			stack2.setPixels(null, 1);

			// select desired channel in source image
			imp.setPosition(channel, 1, 1);
			// set number of channels, slices, and frames
			imp2.setDimensions(1, slices, frames);

			reducer.reduce(imp2);
			imp2.setOpenAsHyperStack(true);
			imp2.setFileInfo(fileInfo);

			// make this channel image the new working image
			imp = imp2;
		}

		image = ImagePlusAdapter.wrap(imp);

		// get some statistics
		numNodes   = image.size();
		dimensions = image.getDimensions();
		numEdges   = 0;
		for (int d = 0; d < dimensions.length; d++)
			numEdges += numNodes - numNodes/dimensions[d];

		// prepare segmentation image
		int[] segDimensions = new int[3];
		segDimensions[0] = 1;
		segDimensions[1] = 1;
		segDimensions[2] = 1;
		for (int d = 0; d < dimensions.length; d++)
			segDimensions[d] = dimensions[d];

		seg = IJ.createImage("GraphCut segmentation", "8-bit",
		                     segDimensions[0], segDimensions[1], segDimensions[2]);
		segmentation = ImagePlusAdapter.wrap(seg);

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

	private void createSegmentationImage() {

		// create segmentation image
		LocalizableByDimCursor<T> cursor = segmentation.createLocalizableByDimCursor();
		int[] imagePosition = new int[dimensions.length];
		while (cursor.hasNext()) {

			cursor.fwd();

			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition);

			if (graphCut.getTerminal(nodeNum) == Terminal.FOREGROUND)
				cursor.getType().setReal(255.0);
			else
				cursor.getType().setReal(0.0);
		}
		seg.show();
		seg.updateAndDraw();
	}

	/**
	 * Processes a single channel image.
	 *
	 * The intensities of the image are interpreted as the probability of each
	 * pixel to belong to the foreground. The potts weight represents an
	 * isotropic edge weight.
	 *
	 * @param pottsWeight Isotropic edge weights.
	 */
	public void processSingleChannelImage(float pottsWeight) {

		LocalizableByDimCursor<T> cursor = image.createLocalizableByDimCursor();
		int[] imagePosition              = new int[dimensions.length];

		// create a new graph cut instance
		// TODO: reuse an old one
		IJ.log("Creating graph structure of " + numNodes + " nodes and " + numEdges + " edges...");
		graphCut = new GraphCut(numNodes, numEdges);
		IJ.log("...done.");

		// set terminal weights, i.e., segmentation probabilities
		IJ.log("Setting terminal weights...");
		while (cursor.hasNext()) {

			cursor.fwd();
			cursor.getPosition(imagePosition);

			int nodeNum = listPosition(imagePosition);
			
			T type = cursor.getType();
			float value = type.getRealFloat();

			graphCut.setTerminalWeights(nodeNum, value, 255.0f - value);
		}
		IJ.log("...done.");

		// set edge weights
		IJ.log("Setting edge weights to " + pottsWeight + "...");
		cursor   = image.createLocalizableByDimCursor();
		int[] neighborPosition = new int[dimensions.length];
		int e = 0;
		while (cursor.hasNext()) {

			cursor.fwd();

			// image position
			cursor.getPosition(imagePosition);
			int nodeNum = listPosition(imagePosition);

			neighborPosition = imagePosition;

			for (int d = 0; d < dimensions.length; d++) {

				neighborPosition[d] -= 1;

				if (neighborPosition[d] >= 0) {

					int neighborNum = listPosition(neighborPosition);
					graphCut.setEdgeWeight(nodeNum, neighborNum, pottsWeight);
					e++;
				}
				neighborPosition[d] += 1;
			}
		}
		IJ.log("...done inserting " + e + " edges.");

		// calculate max flow
		IJ.log("Calculating max flow...");
		float maxFlow = graphCut.computeMaximumFlow(false, null);
		IJ.log("...done. Max flow is " + maxFlow);
	}

	private int listPosition(int[] imagePosition) {

		int pos = 0;
		int fac = 1;
		for (int d = 0; d < dimensions.length; d++) {
			pos += fac*imagePosition[d];
			fac *= dimensions[d];
		}
		return pos;
	}
}
