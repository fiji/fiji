package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import fiji.util.NumberParser;
import ij.ImagePlus;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Configuration panel for spot detectors based on LoG detector. 
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class LogDetectorConfigurationPanel extends ConfigurationPanel {

	private static final long serialVersionUID = 4519313560718180405L;
	private JLabel jLabel1;
	protected JLabel jLabelSegmenterName;
	private JLabel jLabel2;
	protected JButton jButtonRefresh;
	protected JTextField jTextFieldThreshold;
	protected JLabel jLabelThreshold;
	protected JLabel jLabelHelpText;
	protected JCheckBox jCheckBoxMedianFilter;
	protected JLabel jLabelBlobDiameterUnit;
	protected JTextField jTextFieldBlobDiameter;
	protected JCheckBox jCheckSubPixel;
	/** The HTML text that will be displayed as a help. */
	protected final String infoText;
	protected final String spaceUnits;
	protected final String detectorName;
	protected JLabel lblSegmentInChannel;
	protected JSlider sliderChannel;
	protected JLabel labelChannel;
	protected ImagePlus imp;

	/*
	 * CONSTRUCTOR
	 */


	public LogDetectorConfigurationPanel(ImagePlus imp, String infoText, String detectorName, String spaceUnits) {
		this.imp = imp;
		this.infoText = infoText;
		this.detectorName = detectorName;
		this.spaceUnits = spaceUnits;
		initGUI();
	}

	/*
	 * METHODS
	 */

	@Override
	public Map<String, Object> getSettings() {
		HashMap<String, Object> settings = new HashMap<String, Object>(5);
		int targetChannel = sliderChannel.getValue();
		double expectedRadius = NumberParser.parseDouble(jTextFieldBlobDiameter.getText())/2;
		double threshold = NumberParser.parseDouble(jTextFieldThreshold.getText());
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
		jTextFieldBlobDiameter.setText(""+( 2 * (Double) settings.get(KEY_RADIUS)));
		jCheckBoxMedianFilter.setSelected((Boolean) settings.get(KEY_DO_MEDIAN_FILTERING));
		jTextFieldThreshold.setText("" + settings.get(KEY_THRESHOLD));
		jCheckSubPixel.setSelected((Boolean) settings.get(KEY_DO_SUBPIXEL_LOCALIZATION));
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Fill the text fields with parameters grabbed from stored ImagePlus.
	 */
	private void refresh() {
		if (null == imp)
			return;
		jTextFieldThreshold.setText(String.format("%.0f", imp.getProcessor().getMinThreshold()));
		sliderChannel.setValue(imp.getC());
	}

	protected void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 461));
			setLayout(null);
			{
				jLabel1 = new JLabel();
				jLabel1.setBounds(1, 10, 103, 13);
				this.add(jLabel1);
				jLabel1.setText("Settings for detector:");
				jLabel1.setFont(FONT);
			}
			{
				jLabelSegmenterName = new JLabel();
				jLabelSegmenterName.setBounds(11, 33, 225, 17);
				this.add(jLabelSegmenterName);
				jLabelSegmenterName.setFont(BIG_FONT);
				jLabelSegmenterName.setText(detectorName);
			}
			{
				jLabel2 = new JLabel();
				jLabel2.setBounds(16, 247, 152, 13);
				this.add(jLabel2);
				jLabel2.setText("Estimated blob diameter:");
				jLabel2.setFont(FONT);

			}
			{
				jTextFieldBlobDiameter = new JNumericTextField();
				jTextFieldBlobDiameter.setHorizontalAlignment(SwingConstants.CENTER);
				jTextFieldBlobDiameter.setColumns(5);
				jTextFieldBlobDiameter.setText("5");
				jTextFieldBlobDiameter.setLocation(168, 247);
				jTextFieldBlobDiameter.setSize(new Dimension(40, 16));
				this.add(jTextFieldBlobDiameter);
				jTextFieldBlobDiameter.setFont(FONT);
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				jLabelBlobDiameterUnit.setBounds(228, 245, 40, 17);
				this.add(jLabelBlobDiameterUnit);
				jLabelBlobDiameterUnit.setFont(FONT);
				jLabelBlobDiameterUnit.setText(spaceUnits);
			}
			{
				jCheckBoxMedianFilter = new JCheckBox();
				jCheckBoxMedianFilter.setBounds(11, 290, 230, 21);
				this.add(jCheckBoxMedianFilter);
				jCheckBoxMedianFilter.setText("Use median filter ");
				jCheckBoxMedianFilter.setFont(FONT);
			}
			{
				jLabelHelpText = new JLabel();
				jLabelHelpText.setBounds(10, 60, 280, 104);
				this.add(jLabelHelpText);
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelHelpText.setText(infoText
						.replace("<br>", "")
						.replace("<p>", "<p align=\"justify\">")
						.replace("<html>", "<html><p align=\"justify\">"));
			}
			{
				jLabelThreshold = new JLabel();
				jLabelThreshold.setBounds(16, 270, 152, 13);
				this.add(jLabelThreshold);
				jLabelThreshold.setText("Threshold:");
				jLabelThreshold.setFont(FONT);
			}
			{
				jTextFieldThreshold = new JNumericTextField();
				jTextFieldThreshold.setHorizontalAlignment(SwingConstants.CENTER);
				jTextFieldThreshold.setText("0");
				jTextFieldThreshold.setBounds(168, 268, 40, 16);
				this.add(jTextFieldThreshold);
				jTextFieldThreshold.setFont(FONT);
			}
			{
				// Add sub-pixel checkbox
				jCheckSubPixel = new JCheckBox();
				jCheckSubPixel.setBounds(11, 314, 231, 21);
				this.add(jCheckSubPixel);
				jCheckSubPixel.setText("Do sub-pixel localization ");
				jCheckSubPixel.setFont(FONT);
			}
			{
				lblSegmentInChannel = new JLabel("Segment in channel:");
				lblSegmentInChannel.setFont(SMALL_FONT);
				lblSegmentInChannel.setBounds(16, 219, 100, 13);
				add(lblSegmentInChannel);

				sliderChannel = new JSlider();
				sliderChannel.setBounds(126, 213, 91, 23);
				sliderChannel.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) { labelChannel.setText(""+sliderChannel.getValue()); }
				});
				add(sliderChannel);

				labelChannel = new JLabel("1");
				labelChannel.setHorizontalAlignment(SwingConstants.CENTER);
				labelChannel.setBounds(228, 216, 21, 18);
				labelChannel.setFont(SMALL_FONT);
				add(labelChannel);
			}
			{
				jButtonRefresh = new JButton();
				jButtonRefresh.setBounds(5, 370, 67, 21);
				this.add(jButtonRefresh);
				jButtonRefresh.setText("Refresh");
				jButtonRefresh.setFont(SMALL_FONT);
				jButtonRefresh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						refresh();
					}
				});
				
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
