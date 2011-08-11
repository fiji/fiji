package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;
import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;

import javax.swing.JComponent;
import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterType;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

/**
 * Mother class for spot segmenter settings panel. This panel is actually suitable for 2 implemented segmenters,
 * which is why it is a class concrete for now.  
 * <p>
 * Also offer a factory method to instantiate the correct panel pointed by a tracker type.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 *
 */
public class SegmenterSettingsPanel extends ActionListenablePanel {
	private static final long serialVersionUID = 3220742915219642676L;
	private JLabel jLabel1;
	private JLabel jLabelSegmenterName;
	private JLabel jLabel2;
	private JButton jButtonRefresh;
	private JTextField jTextFieldThreshold;
	private JLabel jLabelThreshold;
	private JLabel jLabelHelpText;
	private JCheckBox jCheckBoxMedianFilter;
	private JLabel jLabelBlobDiameterUnit;
	private JTextField jTextFieldBlobDiameter;
	/** The {@link SegmenterSettings} object set by this panel. */
	protected SegmenterSettings settings;
	
	/*
	 * CONSTRUCTOR
	 */
	
	
	public SegmenterSettingsPanel(SegmenterSettings settings) {
		super();
		if (null == settings)
			settings = new SegmenterSettings();
		this.settings = settings;
		initGUI();
	}
	
	
	/*
	 * STATIC METHOD
	 */
	

	/**
	 * Return a {@link SegmenterSettingsPanel} that is able to configure the {@link SpotSegmenter}
	 * selected in the given settings object.
	 */
	public static SegmenterSettingsPanel createSegmenterSettingsPanel(final Settings settings) {
		SegmenterType segmenterType = settings.segmenterType;
		SegmenterSettings segmenterSettings = settings.segmenterSettings;
		if (null == segmenterSettings || null == segmenterSettings.segmenterType) {
			segmenterSettings = segmenterType.createSettings();
			segmenterSettings.segmenterType = segmenterType;
			segmenterSettings.spaceUnits = settings.spaceUnits;
		}
		switch (segmenterType) {
		case DOG_SEGMENTER: {
			return new DogSegmenterSettingsPanel(segmenterSettings);
		}
		case LOG_SEGMENTER:
		case PEAKPICKER_SEGMENTER:
			return new SegmenterSettingsPanel(segmenterSettings);
		case MANUAL_SEGMENTER: {
			SegmenterSettingsPanel panel = new SegmenterSettingsPanel(segmenterSettings);
			JComponent[] uselessComponents = new JComponent[] {
					panel.jCheckBoxMedianFilter,
					panel.jLabelThreshold,
					panel.jTextFieldThreshold,
					panel.jButtonRefresh };
			for(JComponent c : uselessComponents)
				c.setVisible(false);
			return panel;
		}
		}
		return null;
	}	
	
	/*
	 * METHODS
	 */
	
	/**
	 * Update the settings object given with the parameters this panel allow to tune its
	 * {@link SegmenterSettings} field, with the sub-fields
	 * {@link SegmenterSettings#expectedDiameter}, {@link SegmenterSettings#useMedianFilter} and
	 * {@link SegmenterSettings#threshold}.
	 * @return  the updated Settings
	 */
	public SegmenterSettings getSettings() {
		settings.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		settings.threshold = Float.parseFloat(jTextFieldThreshold.getText());
		settings.useMedianFilter = jCheckBoxMedianFilter.isSelected();
		return settings;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Fill the text fields with parameters grabbed from current ImagePlus.
	 */
	private void refresh() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp)
			return;
		jTextFieldThreshold.setText(String.format("%.0f", imp.getProcessor().getMinThreshold()));
	}
	
	protected void initGUI() {
		try {
			GridBagLayout thisLayout = new GridBagLayout();
			this.setPreferredSize(new java.awt.Dimension(246, 399));
			thisLayout.rowWeights = new double[] {0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0};
			thisLayout.rowHeights = new int[] {15, 15, 7, 15, 15, 15, 7, 15};
			thisLayout.columnWeights = new double[] {0.1, 0.1, 0.1};
			thisLayout.columnWidths = new int[] {7, 7, 7};
			this.setLayout(thisLayout);
			{
				jLabel1 = new JLabel();
				this.add(jLabel1, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
				jLabel1.setText("Settings for segmenter:");
				jLabel1.setFont(FONT);
			}
			{
				jLabelSegmenterName = new JLabel();
				this.add(jLabelSegmenterName, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
				jLabelSegmenterName.setText(settings.segmenterType.toString());
				jLabelSegmenterName.setFont(BIG_FONT);
			}
			{
				jLabel2 = new JLabel();
				this.add(jLabel2, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 5, 0), 0, 0));
				jLabel2.setText("Estimated blob diameter:");
				jLabel2.setFont(FONT);

			}
			{
				jTextFieldBlobDiameter = new JNumericTextField();
				jTextFieldBlobDiameter.setSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldBlobDiameter, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldBlobDiameter.setFont(FONT);
				jTextFieldBlobDiameter.setText(""+(2*settings.expectedRadius));
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				this.add(jLabelBlobDiameterUnit, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
				jLabelBlobDiameterUnit.setFont(FONT);
				jLabelBlobDiameterUnit.setText(settings.spaceUnits);
			}
			{
				jCheckBoxMedianFilter = new JCheckBox();
				this.add(jCheckBoxMedianFilter, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jCheckBoxMedianFilter.setText("Use median filter ");
				jCheckBoxMedianFilter.setFont(FONT);
				jCheckBoxMedianFilter.setSelected(settings.useMedianFilter);
			}
			{
				jLabelHelpText = new JLabel();
				this.add(jLabelHelpText, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
				jLabelHelpText.setText(settings.segmenterType.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
				jLabelHelpText.setFont(FONT.deriveFont(Font.ITALIC));
			}
			{
				jLabelThreshold = new JLabel();
				this.add(jLabelThreshold, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 5, 0), 0, 0));
				jLabelThreshold.setText("Threshold:");
				jLabelThreshold.setFont(FONT);
			}
			{
				jTextFieldThreshold = new JNumericTextField();
				this.add(jTextFieldThreshold, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldThreshold.setText(""+settings.threshold);
				jTextFieldThreshold.setFont(FONT);
			}
			{
				jButtonRefresh = new JButton();
				this.add(jButtonRefresh, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 5, 0), 0, 0));
				jButtonRefresh.setText("Refresh");
				jButtonRefresh.setFont(SMALL_FONT);
				jButtonRefresh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						refresh();
					}
				});				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/*
	 * MAIN METHOD
	 */
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	*/
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		SegmenterSettings s = new SegmenterSettings();
		s.segmenterType = fiji.plugin.trackmate.segmentation.SegmenterType.PEAKPICKER_SEGMENTER;
		frame.getContentPane().add(new SegmenterSettingsPanel(s));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
}
