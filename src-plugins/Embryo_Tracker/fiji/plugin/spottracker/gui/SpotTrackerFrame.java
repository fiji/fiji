package fiji.plugin.spottracker.gui;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Logger;
import fiji.plugin.spottracker.Settings;
import fiji.plugin.spottracker.Spot;
import fiji.plugin.spottracker.Spot_Tracker;
import fiji.plugin.spottracker.tracking.LAPTracker;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class SpotTrackerFrame extends javax.swing.JFrame {

	static final Font FONT = new Font("Arial", Font.PLAIN, 11);
	static final Font SMALL_FONT = FONT.deriveFont(10f);
	static enum GuiState {
		START,
		SEGMENTING,
		THRESHOLD_BLOBS;
	};
	
	private static final long serialVersionUID = 1L;
	private static final String START_DIALOG_KEY = "Start";
	private static final String THRESHOLD_GUI_KEY = "Threshold";
	private static final String LOG_PANEL_KEY = "Log";
	
	protected static final int DEFAULT_RESAMPLING_FACTOR = 3;
	protected static final int DEFAULT_THRESHOLD = 50;

	private StartDialogPanel startDialogPanel;
	private ThresholdGuiPanel thresholdGuiPanel;
	private CardLayout cardLayout;
	private GuiState state;
	private Settings settings;
	private Spot_Tracker spotTracker;
	private LogPanel logPanel;
	private Logger logger;
	private TreeMap<Integer,Collection<Spot>> spots;
	private TreeMap<Integer,Collection<Spot>> selectedSpots;
	private SpotDisplayer displayer;
	private boolean is3D;
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	public SpotTrackerFrame(Spot_Tracker plugin) {
		if (null == plugin)
			plugin = new Spot_Tracker();
		this.spotTracker = plugin;
		initGUI();
	}
	
	public SpotTrackerFrame() {
		this(new Spot_Tracker());
	}
	
	private void next() {
		switch(state) {
			case START:
				state = GuiState.SEGMENTING;
				cardLayout.show(getContentPane(), LOG_PANEL_KEY);
				settings = startDialogPanel.updateSettings(settings);
				is3D = settings.imp.getNSlices() > 1;
				logger = logPanel.getLogger();
				logger.log("Starting segmentation...\n", Logger.BLUE_COLOR);
				new Thread() {					
					public void run() {
						long start = System.currentTimeMillis();
						try {
							spotTracker.setLogger(logger);
							logPanel.jButtonNext.setEnabled(false);
							spotTracker.execSegmentation(settings);
						} catch (Exception e) {
							logger.error("An error occured:\n"+e+'\n');
							e.printStackTrace();
						} finally {
							logPanel.jButtonNext.setEnabled(true);
							long end = System.currentTimeMillis();
							logger.log(String.format("Segmentation done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
						}
					}
				}.start();
				break;
				
			case SEGMENTING:
				spots = spotTracker.getSpots();
				
				// Launch renderer
				logger.log("Rendering results...\n",Logger.GREEN_COLOR);
				logPanel.jButtonNext.setEnabled(false);				
				final TreeMap<Integer, Collection<Spot>> spotsOverTime = new TreeMap<Integer, Collection<Spot>>();
				for(int i = 0; i < spots.size(); i++) 
					spotsOverTime.put(i, spots.get(i));

				// Thread for rendering
				Runnable renderingRunnable;
				if (is3D) { 
					renderingRunnable = new Runnable() {
						public void run() {
							// Render image data
							final Image3DUniverse universe = new Image3DUniverse();
							universe.show();
							ImagePlus[] images = makeImageForViewer(settings);
							Content imageContent = ContentCreator.createContent(
									settings.imp.getTitle(), 
									images, 
									Content.VOLUME, 
									DEFAULT_RESAMPLING_FACTOR, 
									0,
									null, 
									DEFAULT_THRESHOLD, 
									new boolean[] {true, true, true});
							// Render spots
							displayer = new SpotDisplayer3D(spotsOverTime, universe, settings.expectedDiameter/2); // TODO otherwise too big 							
							universe.addContentLater(imageContent);

						}
					};
				} else {
					renderingRunnable = new Runnable() {
						public void run() {
							final float[] calibration = new float[] {
									(float) settings.imp.getCalibration().pixelWidth, 
									(float) settings.imp.getCalibration().pixelHeight};
							displayer = new SpotDisplayer2D(spotsOverTime, settings.imp, settings.expectedDiameter/2, calibration);
						}
					};
				}
				Thread renderingThread = new Thread(renderingRunnable);

				renderingThread.start();
				try {
					renderingThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				displayer.render();
				logger.log("Rendering done.\n", Logger.GREEN_COLOR);
				logPanel.jButtonNext.setEnabled(true);
				cardLayout.show(getContentPane(), THRESHOLD_GUI_KEY);
				
				thresholdGuiPanel.setSpots(spots.values());
				thresholdGuiPanel.addThresholdPanel(Feature.MEAN_INTENSITY);
				thresholdSpots();
				state = GuiState.THRESHOLD_BLOBS;
				break;
				
			case THRESHOLD_BLOBS:
				cardLayout.show(getContentPane(), LOG_PANEL_KEY);
				logger.log("Thresholding spots...\n", Logger.BLUE_COLOR);
				logPanel.jButtonNext.setEnabled(false);
				new Thread() {					
					public void run() {
						Feature[] features = thresholdGuiPanel.getFeatures();
						double[] values = thresholdGuiPanel.getThresholds();
						boolean[] isAbove = thresholdGuiPanel.getIsAbove();
						for (int i = 0; i < features.length; i++)
							spotTracker.addThreshold(features[i], (float) values[i], isAbove[i]);
						spotTracker.execThresholding();
						selectedSpots = spotTracker.getSelectedSpots();
						logger.log("Thresholding done.\n", Logger.BLUE_COLOR);
						logPanel.jButtonNext.setEnabled(true);
						
						logger.log("Starting tracking.");
						LAPTracker tracker = new LAPTracker(selectedSpots);
						if (tracker.checkInput() && tracker.process()) {
							logger.log("Tracking finished!", Color.GREEN);
							for (int key : selectedSpots.keySet()) {
								for (Spot spot : selectedSpots.get(key))
									System.out.println(spot);// DEBUG
							}
						}
						else 
							logger.error("Problem occured in tracking:\n"+tracker.getErrorMessage());
					}
				}.start();
				
				
				break;
		}
	}
	
	/**
	 * Ensure an 8-bit gray image is sent to the 3D viewer.
	 */
	private static final ImagePlus[] makeImageForViewer(final Settings settings) {
		final ImagePlus origImp = settings.imp;
		final ImagePlus imp;
		
		if (origImp.getType() == ImagePlus.GRAY8)
			imp = origImp;
		else {
			imp = new Duplicator().run(origImp);
			new StackConverter(imp).convertToGray8();
		}
		
		int nChannels = imp.getNChannels();
		int nSlices = imp.getNSlices();
		int nFrames = settings.tend - settings.tstart + 1;
		ImagePlus[] ret = new ImagePlus[nFrames];
		int w = imp.getWidth(), h = imp.getHeight();

		ImageStack oldStack = imp.getStack();
		String oldTitle = imp.getTitle();
		
		for(int i = 0; i < nFrames; i++) {
			
			ImageStack newStack = new ImageStack(w, h);
			for(int j = 0; j < nSlices; j++) {
				int index = imp.getStackIndex(1, j+1, i+settings.tstart+1);
				Object pixels;
				if (nChannels > 1) {
					imp.setPositionWithoutUpdate(1, j+1, i+1);
					pixels = new ColorProcessor(imp.getImage()).getPixels();
				}
				else
					pixels = oldStack.getPixels(index);
				newStack.addSlice(oldStack.getSliceLabel(index), pixels);
			}
			ret[i] = new ImagePlus(oldTitle	+ " (frame " + i + ")", newStack);
			ret[i].setCalibration(imp.getCalibration().copy());
			
		}
		return ret;
	}

	/**
	 * Is called when the user change the color by feature combo box in the 
	 * {@link ThresholdGuiPanel}.
	 */
	private void recolorSpots() {
		Feature feature = thresholdGuiPanel.getColorByFeature();
		displayer.setColorByFeature(feature);
	}
	
	/**
	 * Is called when the user change the threshold settings in the 
	 * {@link ThresholdGuiPanel}.
	 */
	private void thresholdSpots() {
		displayer.refresh(thresholdGuiPanel.getFeatures(), thresholdGuiPanel.getThresholds(), thresholdGuiPanel.getIsAbove());
	}
	
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setTitle("Spot Tracker");
			cardLayout = new CardLayout();
			getContentPane().setLayout(cardLayout);
			this.setResizable(false);
			pack();
			this.setSize(300, 520);
			{
				startDialogPanel = new StartDialogPanel();
				startDialogPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				getContentPane().add(startDialogPanel, START_DIALOG_KEY);
			}
			{
				logPanel = new LogPanel();
				logPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				getContentPane().add(logPanel, LOG_PANEL_KEY);
			}
			{
				thresholdGuiPanel = new ThresholdGuiPanel();
				thresholdGuiPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (e == thresholdGuiPanel.NEXT_BUTTON_PRESSED)
							next();
						else if (e == thresholdGuiPanel.COLOR_FEATURE_CHANGED) {
							recolorSpots();
						} 
					}
				});
				thresholdGuiPanel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						thresholdSpots();
					}
				});
				getContentPane().add(thresholdGuiPanel, THRESHOLD_GUI_KEY);
			}
			cardLayout.show(getContentPane(), START_DIALOG_KEY);
			state = GuiState.START;
		} catch (Exception e) {
			e.printStackTrace();
		}
		repaint();
		validate();
	}


	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SpotTrackerFrame inst = new SpotTrackerFrame();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
}
