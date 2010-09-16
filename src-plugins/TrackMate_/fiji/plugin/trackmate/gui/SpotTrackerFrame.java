package fiji.plugin.trackmate.gui;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackNode;
import fiji.plugin.trackmate.Utils;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;


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
	private JButton jButtonNext;
	private JPanel jPanelButtons;
	private JPanel jPanelMain;
	static final Font SMALL_FONT = FONT.deriveFont(10f);
	static enum GuiState {
		START,
		SEGMENTING,
		THRESHOLD_BLOBS;
	};
	
	private static final long serialVersionUID = 1L;
	private static final String START_DIALOG_KEY = "Start";
	private static final String THRESHOLD_GUI_KEY = "Threshold";
	private JButton jButtonSave;
	private JButton jButtonLoad;
	private JButton jButtonPrevious;
	private static final String LOG_PANEL_KEY = "Log";
	
	protected static final int DEFAULT_RESAMPLING_FACTOR = 3;
	protected static final int DEFAULT_THRESHOLD = 50;

	private StartDialogPanel startDialogPanel;
	private ThresholdGuiPanel thresholdGuiPanel;
	private CardLayout cardLayout;
	private GuiState state;
	private Settings settings;
	private TrackMate_ spotTracker;
	private LogPanel logPanel;
	private Logger logger;
	private TreeMap<Integer,Collection<Spot>> spots;
	private TreeMap<Integer,Collection<TrackNode<Spot>>> tracks;
	private SpotDisplayer<Spot> displayer;
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
	
	public SpotTrackerFrame(TrackMate_ plugin) {
		if (null == plugin)
			plugin = new TrackMate_();
		this.spotTracker = plugin;
		initGUI();
	}
	
	public SpotTrackerFrame() {
		this(new TrackMate_());
	}
	
	/**
	 * Called when the "Next >>" button is pressed.
	 */
	private void next() {
		switch(state) {
			case START:
				execSegmentationStep();
				state = GuiState.SEGMENTING;
				break;
				
			case SEGMENTING:
				execThresholdingStep();
				state = GuiState.THRESHOLD_BLOBS;
				break;
				
			case THRESHOLD_BLOBS:
				execTrackingStep();
				break;
		}
	}
	
	/**
	 * Called when the "<<" is pressed.
	 */
	private void previous() {		
	}
	
	/**
	 * Called when the "Load" button is pressed.
	 */
	private void load() {
	}
	
	/**
	 * Called when the "Save" button is pressed.
	 */
	private void save() {		
	}

	
	/**
	 * Switch to the log panel, and execute the segmentation step, which will be delegated to 
	 * the {@link TrackMate_} glue class in a new Thread.
	 */
	private void execSegmentationStep() {
		cardLayout.show(jPanelMain, LOG_PANEL_KEY);
		settings = startDialogPanel.updateSettings(settings);
		is3D = settings.imp.getNSlices() > 1;
		logger = logPanel.getLogger();
		logger.log("Starting segmentation...\n", Logger.BLUE_COLOR);
		new Thread("TrackMate segmentation thread") {					
			public void run() {
				long start = System.currentTimeMillis();
				try {
					spotTracker.setLogger(logger);
					jButtonNext.setEnabled(false);
					spotTracker.execSegmentation(settings);
				} catch (Exception e) {
					logger.error("An error occured:\n"+e+'\n');
					e.printStackTrace();
				} finally {
					jButtonNext.setEnabled(true);
					long end = System.currentTimeMillis();
					logger.log(String.format("Segmentation done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
			}
		}.start();
	}
	
	/**
	 * Collect the segmentation result, render it in another thread, the switch to the thresholding panel. 
	 */
	private void execThresholdingStep() {
		// Store results
		spots = spotTracker.getSpots();
		tracks = Utils.embed(spots);
		
		// Launch renderer
		logger.log("Rendering results...\n",Logger.BLUE_COLOR);
		jButtonNext.setEnabled(false);				
		
		// Thread for rendering
		new Thread("TrackMate rendering thread") {
			public void run() {
				// Render image data
				if (is3D) { 
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
					displayer = new SpotDisplayer3D<Spot>(tracks, universe, settings.expectedDiameter/2); 							
					universe.addContentLater(imageContent);

				} else {
					final float[] calibration = new float[] {
							(float) settings.imp.getCalibration().pixelWidth, 
							(float) settings.imp.getCalibration().pixelHeight};
					displayer = new SpotDisplayer2D<Spot>(tracks, settings.imp, settings.expectedDiameter/2, calibration);
				}
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
				cardLayout.show(jPanelMain, THRESHOLD_GUI_KEY);
				
				thresholdGuiPanel.setSpots(spots.values());
				thresholdGuiPanel.addThresholdPanel(Feature.LOG_VALUE);
				displayer.render();
				thresholdGuiPanel.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						thresholdSpots();
					}
				});
				thresholdSpots();
				jButtonNext.setEnabled(true);
			}
		}.start();
	}
	
	/**
	 * Switch to the log panel, and execute the tracking part in another thread.
	 */
	private void execTrackingStep() {
		cardLayout.show(jPanelMain, LOG_PANEL_KEY);
		jButtonNext.setEnabled(false);
		new Thread("TrackMate tracking thread") {					
			public void run() {
				// Threshold spots
				Feature[] features = thresholdGuiPanel.getFeatures();
				double[] values = thresholdGuiPanel.getThresholds();
				boolean[] isAbove = thresholdGuiPanel.getIsAbove();
				for (int i = 0; i < features.length; i++)
					spotTracker.addThreshold(features[i], (float) values[i], isAbove[i]);
				spotTracker.execThresholding();
				// Track
				spotTracker.execTracking();
				tracks = spotTracker.getTracks();
				// Forward to displayer
				displayer.setTrackObjects(tracks);
				displayer.setDisplayTracks(true);
				displayer.refresh(thresholdGuiPanel.getFeatures(), thresholdGuiPanel.getThresholds(), thresholdGuiPanel.getIsAbove());
				// Re-enable the GUI
				jButtonNext.setEnabled(true);
			}
		}.start();
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
			this.setResizable(false);
			{
				jPanelMain = new JPanel();
				cardLayout = new CardLayout();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(cardLayout);
				jPanelMain.setPreferredSize(new java.awt.Dimension(300, 461));
			}
			{
				jPanelButtons = new JPanel();
				getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
				jPanelButtons.setLayout(null);
				jPanelButtons.setSize(300, 30);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(300, 30));
				{
					jButtonNext = new JButton();
					jPanelButtons.add(jButtonNext);
					jButtonNext.setText("Next >>");
					jButtonNext.setFont(FONT);
					jButtonNext.setBounds(221, 0, 70, 25);
					jButtonNext.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							next();
						}
					});
				}
				{
					jButtonPrevious = new JButton();
					jPanelButtons.add(jButtonPrevious);
					jButtonPrevious.setText("<<");
					jButtonPrevious.setFont(FONT);
					jButtonPrevious.setBounds(181, 0, 40, 25);
					jButtonPrevious.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							previous();
						}
					});
				}
				{
					jButtonLoad = new JButton();
					jPanelButtons.add(jButtonLoad);
					jButtonLoad.setText("Load");
					jButtonLoad.setFont(FONT);
					jButtonLoad.setBounds(7, 0, 50, 25);
					jButtonLoad.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							load();
						}
					});
				}
				{
					jButtonSave = new JButton();
					jPanelButtons.add(jButtonSave);
					jButtonSave.setText("Save");
					jButtonSave.setFont(FONT);
					jButtonSave.setBounds(61, 0, 50, 25);
					jButtonSave.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							save();
						}
					});
				}
			}
			pack();
			this.setSize(300, 520);
			{
				startDialogPanel = new StartDialogPanel();
				startDialogPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				jPanelMain.add(startDialogPanel, START_DIALOG_KEY);
			}
			{
				logPanel = new LogPanel();
				logPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				jPanelMain.add(logPanel, LOG_PANEL_KEY);
			}
			{
				thresholdGuiPanel = new ThresholdGuiPanel();
				thresholdGuiPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
							recolorSpots();
						} 
					});				
				jPanelMain.add(thresholdGuiPanel, THRESHOLD_GUI_KEY);
			}
			cardLayout.show(jPanelMain, START_DIALOG_KEY);
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
