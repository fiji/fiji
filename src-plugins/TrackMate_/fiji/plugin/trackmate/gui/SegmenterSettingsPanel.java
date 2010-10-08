package fiji.plugin.trackmate.gui;

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

import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

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
public class SegmenterSettingsPanel extends javax.swing.JPanel {
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
	private Settings settings;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public SegmenterSettingsPanel(Settings settings) {
		super();
		if (null == settings)
			settings = new Settings();
		this.settings = settings;
		initGUI();
	}
	
	
	/**
	 * Update the settings object given with the parameters this panel allow to tune its
	 * {@link SegmenterSettings} field, with the sub-fields
	 * {@link SegmenterSettings#expectedDiameter}, {@link SegmenterSettings#useMedianFilter} and
	 * {@link SegmenterSettings#threshold}.
	 * @return  the updated Settings
	 */
	public Settings getSettings() {
		settings.segmenterSettings.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		settings.segmenterSettings.threshold = Float.parseFloat(jTextFieldThreshold.getText());
		settings.segmenterSettings.useMedianFilter = jCheckBoxMedianFilter.isSelected();
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
	
	private void initGUI() {
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
				this.add(jLabelSegmenterName, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
				jLabelSegmenterName.setText(settings.segmenterSettings.segmenterType.toString());
				jLabelSegmenterName.setFont(FONT.deriveFont(Font.BOLD));
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
				jTextFieldBlobDiameter.setText("10");
				jTextFieldBlobDiameter.setFont(FONT);
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				this.add(jLabelBlobDiameterUnit, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
				jLabelBlobDiameterUnit.setText(settings.imp.getCalibration().getUnits());
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
				jLabelHelpText.setText(settings.segmenterSettings.segmenterType.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
				
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
				jTextFieldThreshold.setText("0");
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
		frame.getContentPane().add(new SegmenterSettingsPanel(new Settings()));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
	
}
