package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import ij.ImagePlus;
import ij.measure.Calibration;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.util.JLabelLogger;

/**
 * Configuration panel for spot detectors based on LoG detector. 
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2013
 */
public class LogDetectorConfigurationPanel extends ConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private static final String TOOLTIP_PREVIEW = "<html>" +
			"Preview the current settings on the current frame." +
			"<p>" +
			"Advice: change the settings until you get at least <br>" +
			"<b>all</b> the spots you want, and do not mind the <br>" +
			"spurious spots too much. You will get a chance to <br>" +
			"get rid of them later." +
			"</html>";
	private static final String TOOLTIP_REFRESH = "<html>" +
			"Will read the threshold from the current <br>" +
			"ImagePlus and use its value here.</html>";
	private static final ImageIcon ICON_REFRESH = new ImageIcon(TrackMateGUIController.class.getResource("images/arrow_refresh_small.png"));
	private static final ImageIcon ICON_PREVIEW = new ImageIcon(TrackMateGUIController.class.getResource("images/flag_checked.png"));
	
	
	private JLabel jLabel1;
	protected JLabel jLabelSegmenterName;
	private JLabel jLabel2;
	protected JButton jButtonRefresh;
	protected JButton btnPreview;
	protected JTextField jTextFieldThreshold;
	protected JLabel jLabelThreshold;
	protected JLabel jLabelHelpText;
	protected JCheckBox jCheckBoxMedianFilter;
	protected JLabel jLabelBlobDiameterUnit;
	protected JTextField jTextFieldBlobDiameter;
	protected JCheckBox jCheckSubPixel;
	/** The HTML text that will be displayed as a help. */
	protected JLabel lblSegmentInChannel;
	protected JSlider sliderChannel;
	protected JLabel labelChannel;

	protected final String infoText;
	protected final String spaceUnits;
	protected final String detectorName;
	protected final ImagePlus imp;
	protected final Model model;
	private Logger localLogger;
	/** The layout in charge of laying out this panel content. */
	protected SpringLayout layout;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Creates a new {@link LogDetectorConfigurationPanel}, a GUI able to configure
	 * settings suitable to {@link LogDetectorFactory} and derived implementations. 
	 * @param imp  the {@link ImagePlus} to read the image content from as well as other metadata. 
	 * @param infoText  the detector info text, will be displayed on the panel.
	 * @param detectorName the detector name, will be displayed on the panel. 
	 * @param model the {@link Model} that will be fed with the preview results. It 
	 * is the responsibility of the views registered to listen to model change to display 
	 * the preview results.
	 */
	public LogDetectorConfigurationPanel(ImagePlus imp, String infoText, String detectorName, Model model) {
		this.imp = imp;
		this.infoText = infoText;
		this.detectorName = detectorName;
		this.model = model;
		this.spaceUnits = model.getSpaceUnits();
		initGUI();
	}

	/*
	 * METHODS
	 */

	@Override
	public Map<String, Object> getSettings() {
		HashMap<String, Object> settings = new HashMap<String, Object>(5);
		int targetChannel = sliderChannel.getValue();
		double expectedRadius = Double.parseDouble(jTextFieldBlobDiameter.getText())/2;
		double threshold = Double.parseDouble(jTextFieldThreshold.getText());
		boolean useMedianFilter = jCheckBoxMedianFilter.isSelected();
		boolean doSubPixelLocalization = jCheckSubPixel.isSelected();
		settings.put(KEY_TARGET_CHANNEL, targetChannel);
		settings.put(KEY_RADIUS, expectedRadius);
		settings.put(KEY_THRESHOLD, threshold);
		settings.put(KEY_DO_MEDIAN_FILTERING, useMedianFilter);
		settings.put(KEY_DO_SUBPIXEL_LOCALIZATION, doSubPixelLocalization);
		return settings;
	}

	@Override
	public void setSettings(final Map<String, Object> settings) {
		sliderChannel.setValue((Integer) settings.get(KEY_TARGET_CHANNEL));
		double diameter = (Double) settings.get(KEY_RADIUS);
		if (imp != null) {
			Calibration calibration = imp.getCalibration();
			double maxWidth = imp.getWidth() * 0.5 * (calibration == null ? 1 : calibration.pixelWidth);
			double maxHeight = imp.getHeight() * 0.5 * (calibration == null ? 1 : calibration.pixelHeight);
			double max = maxWidth < maxHeight ? maxWidth : maxHeight;
			if (diameter > max) diameter *= max * 4 / (imp.getWidth() + imp.getHeight());
		}
		jTextFieldBlobDiameter.setText(""+( 2 * diameter));
		jCheckBoxMedianFilter.setSelected((Boolean) settings.get(KEY_DO_MEDIAN_FILTERING));
		jTextFieldThreshold.setText("" + settings.get(KEY_THRESHOLD));
		jCheckSubPixel.setSelected((Boolean) settings.get(KEY_DO_SUBPIXEL_LOCALIZATION));
	}

	/**
	 * Returns a new instance of the {@link SpotDetectorFactory} that this configuration
	 * panels configures. The new instance will in turn be used for the preview 
	 * mechanism. Therefore, classes extending this class are advised to return a 
	 * suitable implementation of the factory.
	 * @return  a new {@link SpotDetectorFactory}.
	 */
	@SuppressWarnings("rawtypes")
	protected SpotDetectorFactory<?> getDetectorFactory() {
		return new LogDetectorFactory();
	}
	
	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Launch detection on the current frame.
	 */
	private void preview() {
		btnPreview.setEnabled(false);
		new Thread("TrackMate preview detection thread") {
			public void run() {
				Settings settings = new Settings();
				settings.setFrom(imp);
				int frame = imp.getFrame()-1;
				settings.tstart = frame;
				settings.tend = frame;
				
				settings.detectorFactory = getDetectorFactory();
				settings.detectorSettings = getSettings();
				
				TrackMate trackmate = new TrackMate(settings);
				trackmate.getModel().setLogger(localLogger);
				
				trackmate.execDetection();
				localLogger.log("Found " + trackmate.getModel().getSpots().getNSpots(false) + " spots."); 
				
				// Wrap new spots in a list.
				SpotCollection newspots = trackmate.getModel().getSpots();
				Iterator<Spot> it = newspots.iterator(frame, false);
				ArrayList<Spot> spotsToCopy = new ArrayList<Spot>(newspots.getNSpots(frame, false));
				while (it.hasNext()) {
					spotsToCopy.add(it.next());
				}
				// Pass new spot list to model.
				model.getSpots().put(frame, spotsToCopy);
				// Make them visible
				for (Spot spot : spotsToCopy) {
					spot.putFeature(SpotCollection.VISIBLITY, SpotCollection.ONE);
				}
				// Generate event for listener to reflect changes.
				model.setSpots(model.getSpots(), true);
				
				btnPreview.setEnabled(true);
				
			};
		}.start();
	}
	
	/**
	 * Fill the text fields with parameters grabbed from stored ImagePlus.
	 */
	private void refresh() {
		if (null == imp)
			return;
		double threshold = imp.getProcessor().getMinThreshold();
		if (threshold < 0) {
			threshold = 0;
		}
		jTextFieldThreshold.setText(String.format("%.0f", threshold));
		sliderChannel.setValue(imp.getC());
	}

	protected void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 461));
			layout = new SpringLayout();
			setLayout(layout);
			{
				jLabel1 = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabel1, 10, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jLabel1, 5, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.EAST, jLabel1, -5, SpringLayout.EAST, this);
				this.add(jLabel1);
				jLabel1.setText("Settings for detector:");
				jLabel1.setFont(FONT);
			}
			{
				jLabelSegmenterName = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabelSegmenterName, 33, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jLabelSegmenterName, 11, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.EAST, jLabelSegmenterName, -11, SpringLayout.EAST, this);
				this.add(jLabelSegmenterName);
				jLabelSegmenterName.setFont(BIG_FONT);
				jLabelSegmenterName.setText(detectorName);
			}
			{
				jLabel2 = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabel2, 247, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jLabel2, 16, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.EAST, jLabel2, -16, SpringLayout.EAST, this);
				this.add(jLabel2);
				jLabel2.setText("Estimated blob diameter:");
				jLabel2.setFont(FONT);

			}
			{
				jTextFieldBlobDiameter = new JNumericTextField();
				layout.putConstraint(SpringLayout.NORTH, jTextFieldBlobDiameter, 247, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jTextFieldBlobDiameter, 168, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jTextFieldBlobDiameter, 263, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jTextFieldBlobDiameter, 208, SpringLayout.WEST, this);
				jTextFieldBlobDiameter.setHorizontalAlignment(SwingConstants.CENTER);
				jTextFieldBlobDiameter.setColumns(5);
				jTextFieldBlobDiameter.setText("5");
				this.add(jTextFieldBlobDiameter);
				jTextFieldBlobDiameter.setFont(FONT);
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabelBlobDiameterUnit, 245, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jLabelBlobDiameterUnit, 228, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jLabelBlobDiameterUnit, 262, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jLabelBlobDiameterUnit, 268, SpringLayout.WEST, this);
				this.add(jLabelBlobDiameterUnit);
				jLabelBlobDiameterUnit.setFont(FONT);
				jLabelBlobDiameterUnit.setText(spaceUnits);
			}
			{
				jCheckBoxMedianFilter = new JCheckBox();
				layout.putConstraint(SpringLayout.NORTH, jCheckBoxMedianFilter, 312, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jCheckBoxMedianFilter, 11, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jCheckBoxMedianFilter, 333, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jCheckBoxMedianFilter, 241, SpringLayout.WEST, this);
				this.add(jCheckBoxMedianFilter);
				jCheckBoxMedianFilter.setText("Use median filter ");
				jCheckBoxMedianFilter.setFont(FONT);
			}
			{
				jLabelHelpText = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabelHelpText, 60, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jLabelHelpText, 10, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jLabelHelpText, 164, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jLabelHelpText, -10, SpringLayout.EAST, this);
				this.add(jLabelHelpText);
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelHelpText.setText(infoText
						.replace("<br>", "")
						.replace("<p>", "<p align=\"justify\">")
						.replace("<html>", "<html><p align=\"justify\">"));
			}
			{
				jLabelThreshold = new JLabel();
				layout.putConstraint(SpringLayout.NORTH, jLabelThreshold, -42, SpringLayout.NORTH, jCheckBoxMedianFilter);
				layout.putConstraint(SpringLayout.WEST, jLabelThreshold, 16, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jLabelThreshold, -29, SpringLayout.NORTH, jCheckBoxMedianFilter);
				layout.putConstraint(SpringLayout.EAST, jLabelThreshold, 168, SpringLayout.WEST, this);
				this.add(jLabelThreshold);
				jLabelThreshold.setText("Threshold:");
				jLabelThreshold.setFont(FONT);
			}
			{
				jTextFieldThreshold = new JNumericTextField();
				layout.putConstraint(SpringLayout.NORTH, jTextFieldThreshold, 268, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jTextFieldThreshold, 168, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jTextFieldThreshold, 284, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jTextFieldThreshold, 208, SpringLayout.WEST, this);
				jTextFieldThreshold.setHorizontalAlignment(SwingConstants.CENTER);
				jTextFieldThreshold.setText("0");
				this.add(jTextFieldThreshold);
				jTextFieldThreshold.setFont(FONT);
			}
			{
				// Add sub-pixel checkbox
				jCheckSubPixel = new JCheckBox();
				layout.putConstraint(SpringLayout.NORTH, jCheckSubPixel, 336, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jCheckSubPixel, 11, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jCheckSubPixel, 357, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jCheckSubPixel, 242, SpringLayout.WEST, this);
				this.add(jCheckSubPixel);
				jCheckSubPixel.setText("Do sub-pixel localization ");
				jCheckSubPixel.setFont(FONT);
			}
			{
				lblSegmentInChannel = new JLabel("Segment in channel:");
				layout.putConstraint(SpringLayout.NORTH, lblSegmentInChannel, 219, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, lblSegmentInChannel, 16, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.EAST, lblSegmentInChannel, 116, SpringLayout.WEST, this);
				lblSegmentInChannel.setFont(SMALL_FONT);
				add(lblSegmentInChannel);

				sliderChannel = new JSlider();
				layout.putConstraint(SpringLayout.NORTH, sliderChannel, 213, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, sliderChannel, 126, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, sliderChannel, 236, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, sliderChannel, 217, SpringLayout.WEST, this);
				sliderChannel.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) { labelChannel.setText(""+sliderChannel.getValue()); }
				});
				add(sliderChannel);

				labelChannel = new JLabel("1");
				layout.putConstraint(SpringLayout.NORTH, labelChannel, 216, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, labelChannel, 228, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, labelChannel, 234, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, labelChannel, 249, SpringLayout.WEST, this);
				labelChannel.setHorizontalAlignment(SwingConstants.CENTER);
				labelChannel.setFont(SMALL_FONT);
				add(labelChannel);
			}
			{
				jButtonRefresh = new JButton("Refresh treshold", ICON_REFRESH);
				layout.putConstraint(SpringLayout.NORTH, jButtonRefresh, 370, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, jButtonRefresh, 11, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, jButtonRefresh, 395, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, jButtonRefresh, 131, SpringLayout.WEST, this);
				this.add(jButtonRefresh);
				jButtonRefresh.setToolTipText(TOOLTIP_REFRESH);
				jButtonRefresh.setFont(SMALL_FONT);
				jButtonRefresh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						refresh();
					}
				});
			}
			{
				btnPreview = new JButton("Preview", ICON_PREVIEW);
				layout.putConstraint(SpringLayout.NORTH, btnPreview, 370, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, btnPreview, -141, SpringLayout.EAST, this);
				layout.putConstraint(SpringLayout.SOUTH, btnPreview, 395, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, btnPreview, -10, SpringLayout.EAST, this);
				btnPreview.setToolTipText(TOOLTIP_PREVIEW);
				this.add(btnPreview);
				btnPreview.setFont(SMALL_FONT);
				btnPreview.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						preview();
					}
				});
			}

			{

				// Deal with channels: the slider and channel labels are only visible if we find more than one channel.
				int n_channels = imp.getNChannels();
				sliderChannel.setMaximum(n_channels);
				sliderChannel.setMinimum(1);
				sliderChannel.setValue(imp.getChannel());
				
				if (n_channels <= 1) {
					labelChannel.setVisible(false);
					lblSegmentInChannel.setVisible(false);
					sliderChannel.setVisible(false);
				} else {
					labelChannel.setVisible(true);
					lblSegmentInChannel.setVisible(true);
					sliderChannel.setVisible(true);			
				}
			}
			{
				JLabelLogger labelLogger = new JLabelLogger();
				layout.putConstraint(SpringLayout.NORTH, labelLogger, 407, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.WEST, labelLogger, 10, SpringLayout.WEST, this);
				layout.putConstraint(SpringLayout.SOUTH, labelLogger, 431, SpringLayout.NORTH, this);
				layout.putConstraint(SpringLayout.EAST, labelLogger, -10, SpringLayout.EAST, this);
				add(labelLogger);
				localLogger = labelLogger.getLogger();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
