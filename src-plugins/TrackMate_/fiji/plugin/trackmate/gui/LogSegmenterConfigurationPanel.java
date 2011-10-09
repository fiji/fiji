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
import javax.swing.JLabel;
import javax.swing.JTextField;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

/**
 * Configuration panel for spot segmenters based on LoG segmentation. 
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class LogSegmenterConfigurationPanel extends SegmenterConfigurationPanel {

	private static final long serialVersionUID = -1376383272848535855L;
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
	/** The {@link LogSegmenterSettings} object set by this panel. */
	private LogSegmenterSettings settings;
	
	/*
	 * CONSTRUCTOR
	 */
	
	
	public LogSegmenterConfigurationPanel() {
		initGUI();
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
	@Override
	public SegmenterSettings getSegmenterSettings() {
		settings.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		settings.threshold = Float.parseFloat(jTextFieldThreshold.getText());
		settings.useMedianFilter = jCheckBoxMedianFilter.isSelected();
		return settings;
	}
	
	@Override
	public void setSegmenterSettings(TrackMateModel model) {
		this.settings = (LogSegmenterSettings) model.getSettings().segmenterSettings;
		echoSettings(model);
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
	
	private void echoSettings(TrackMateModel model) {
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().segmenter.toString());
		jLabelHelpText.setText(model.getSettings().segmenter.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
		
		jTextFieldBlobDiameter.setText(""+(2*settings.expectedRadius));
		jCheckBoxMedianFilter.setSelected(settings.useMedianFilter);
		jTextFieldThreshold.setText(""+settings.threshold);
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
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				this.add(jLabelBlobDiameterUnit, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
				jLabelBlobDiameterUnit.setFont(FONT);
			}
			{
				jCheckBoxMedianFilter = new JCheckBox();
				this.add(jCheckBoxMedianFilter, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jCheckBoxMedianFilter.setText("Use median filter ");
				jCheckBoxMedianFilter.setFont(FONT);
			}
			{
				jLabelHelpText = new JLabel();
				this.add(jLabelHelpText, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
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

}
