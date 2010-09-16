package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.SpotTrackerFrame.FONT;
import static fiji.plugin.trackmate.gui.SpotTrackerFrame.SMALL_FONT;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.Settings;

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
public class StartDialogPanel extends ActionListenablePanel {

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 1L;
	private static final float DEFAULT_BLOB_DIAMETER  = 10;
	private static final boolean DEFAULT_MEDIAN_FILTER = false;
	private static final boolean DEFAULT_ALLOW_EDGE = false;

	private static final Dimension TEXTFIELD_DIMENSION = new Dimension(40,18);
	
	
	private JLabel jLabelExpectedDiameter;
	private JLabel jLabelBlobDiameterUnit;
	private JLabel jLabelUseMdianFilter;
	private JLabel jLabelCheckCalibration;
	private JTextField jTextFieldPixelWidth;
	private JTextField jTextFieldZStart;
	private JTextField jTextFieldYStart;
	private JTextField jTextFieldXStart;
	private JLabel jLabelCropSetting;
	private JButton jButtonRefresh;
	private JTextField jTextFieldTEnd;
	private JLabel jLabelTo4;
	private JTextField jTextFieldTStart;
	private JLabel jLabelT;
	private JTextField jTextFieldZEnd;
	private JTextField jTextFieldYEnd;
	private JTextField jTextFieldXEnd;
	private JLabel jLabelTo3;
	private JLabel jLabelTo2;
	private JLabel jLabelTo1;
	private JLabel jLabelZ;
	private JLabel jLabelY;
	private JLabel jLabelX;
	private JLabel jLabelUnits3;
	private JLabel jLabelUnits2;
	private JLabel jLabelUnits1;
	private JTextField jTextFieldVoxelDepth;
	private JTextField jTextFieldPixelHeight;
	private JLabel jLabelVoxelDepth;
	private JLabel jLabelPixelHeight;
	private JLabel jLabelPixelWidth;
	private JCheckBox jCheckBoxUseMedianFilter;
	private JCheckBox jCheckBoxAllowEdgeMaxima;
	private JLabel jLabelAllowEdgeMaxima;
	private JLabel jLabelImageName;
	private JTextField jTextFieldExpectedBlobDiameter;

	private ImagePlus imp;

	
	public StartDialogPanel() {
		super();
		initGUI();
		refresh();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Fill the text fields with parameters grabbed from current ImagePlus.
	 */
	private void refresh() {
		imp = WindowManager.getCurrentImage();
		if (null == imp)
			return;
		jLabelImageName.setText(imp.getTitle());
		jTextFieldPixelWidth.setText(String.format("%.1f", imp.getCalibration().pixelWidth));
		jTextFieldPixelHeight.setText(String.format("%.1f", imp.getCalibration().pixelHeight));
		jTextFieldVoxelDepth.setText(String.format("%.1f", imp.getCalibration().pixelDepth));
		jLabelBlobDiameterUnit.setText(imp.getCalibration().getUnit());
		jLabelUnits1.setText(imp.getCalibration().getXUnit());
		jLabelUnits2.setText(imp.getCalibration().getYUnit());
		jLabelUnits3.setText(imp.getCalibration().getZUnit());
		Roi roi = imp.getRoi();
		if (null == roi)
			roi = new Roi(0,0,imp.getWidth(),imp.getHeight());
		Rectangle boundingRect = roi.getBounds();
		jTextFieldXStart.setText(""+(boundingRect.x+1)); 
		jTextFieldYStart.setText(""+(boundingRect.y+1));
		jTextFieldXEnd.setText(""+(boundingRect.width+boundingRect.x+1));
		jTextFieldYEnd.setText(""+(boundingRect.height+boundingRect.y+1));
		jTextFieldZStart.setText(""+1);
		jTextFieldZEnd.setText(""+imp.getNSlices());
		jTextFieldTStart.setText(""+1); 
		jTextFieldTEnd.setText(""+imp.getNFrames());
	}
	
	
	/**
	 * Update the settings object given with the parameters this panel allow to tune, 
	 * namely {@link Settings#expectedDiameter}, {@link Settings#useMedianFilter} and
	 * {@link Settings#allowEdgeMaxima}.
	 * @param settings  the Settings to update. If <code>null</code>, a new default one
	 * is created.
	 * @return  the updated Settings
	 */
	public Settings updateSettings(Settings settings) {
		if (null == settings)
			settings = new Settings();
		try {
			settings.expectedDiameter = Float.parseFloat(jTextFieldExpectedBlobDiameter.getText());
		} catch (NumberFormatException nfe) {}
		settings.useMedianFilter = jCheckBoxUseMedianFilter.isSelected();
		settings.allowEdgeMaxima = jCheckBoxAllowEdgeMaxima.isSelected();
		settings.imp =  imp;
		
		settings.tstart = Integer.parseInt(jTextFieldTStart.getText());
		settings.tend 	= Integer.parseInt(jTextFieldTEnd.getText());
		settings.xstart = Integer.parseInt(jTextFieldXStart.getText());
		settings.xend 	= Integer.parseInt(jTextFieldXEnd.getText());
		settings.ystart = Integer.parseInt(jTextFieldYStart.getText());
		settings.yend 	= Integer.parseInt(jTextFieldYEnd.getText());
		settings.zstart = Integer.parseInt(jTextFieldZStart.getText());
		settings.zend 	= Integer.parseInt(jTextFieldZEnd.getText());
		
		return settings;
	}
	

	
	
	private void initGUI() {
		try {
			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
			thisLayout.rowHeights = new int[] {50, 25, 25, 25, 50, 25, 25, 25, 50, 20, 20, 20, 25, 7, 15};
			thisLayout.columnWeights = new double[] {0.1, 0.0, 0.1};
			thisLayout.columnWidths = new int[] {7, 50, 20};
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(266, 476));
			{
				jLabelImageName = new JLabel();
				this.add(jLabelImageName, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabelImageName.setText("Image name");
				jLabelImageName.setFont(FONT.deriveFont(Font.BOLD));
				jLabelImageName.setHorizontalAlignment(SwingConstants.CENTER);
			}
			{
				jLabelExpectedDiameter = new JLabel();
				this.add(jLabelExpectedDiameter, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelExpectedDiameter.setText("Expected blob diameter:");
				jLabelExpectedDiameter.setFont(FONT);
			}
			{
				jTextFieldExpectedBlobDiameter = new JTextField();
				this.add(jTextFieldExpectedBlobDiameter, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldExpectedBlobDiameter.setText(String.format("%.1f", DEFAULT_BLOB_DIAMETER));
				jTextFieldExpectedBlobDiameter.setFont(FONT);
			}
			{
				jLabelBlobDiameterUnit = new JLabel();
				jLabelBlobDiameterUnit.setText("units");
				jLabelBlobDiameterUnit.setFont(FONT);
				this.add(jLabelBlobDiameterUnit, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			}
			{
				jLabelUseMdianFilter = new JLabel();
				this.add(jLabelUseMdianFilter, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelUseMdianFilter.setText("Use median filter:");
				jLabelUseMdianFilter.setFont(FONT);
			}
			{
				jLabelAllowEdgeMaxima = new JLabel();
				this.add(jLabelAllowEdgeMaxima, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelAllowEdgeMaxima.setText("Allow edge maxima:");
				jLabelAllowEdgeMaxima.setFont(FONT);
			}
			{
				jCheckBoxAllowEdgeMaxima = new JCheckBox();
				this.add(jCheckBoxAllowEdgeMaxima, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jCheckBoxAllowEdgeMaxima.setSelected(DEFAULT_ALLOW_EDGE);
			}
			{
				jCheckBoxUseMedianFilter = new JCheckBox();
				this.add(jCheckBoxUseMedianFilter, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jCheckBoxUseMedianFilter.setSelected(DEFAULT_MEDIAN_FILTER);
			}
			{
				jLabelCheckCalibration = new JLabel();
				this.add(jLabelCheckCalibration, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
				jLabelCheckCalibration.setText("Calibration settings:");
				jLabelCheckCalibration.setFont(FONT);
			}
			{
				jLabelPixelWidth = new JLabel();
				this.add(jLabelPixelWidth, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelPixelWidth.setText("Pixel width:");
				jLabelPixelWidth.setFont(SMALL_FONT);
			}
			{
				jLabelPixelHeight = new JLabel();
				this.add(jLabelPixelHeight, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelPixelHeight.setText("Pixel height:");
				jLabelPixelHeight.setFont(SMALL_FONT);
			}
			{
				jLabelVoxelDepth = new JLabel();
				this.add(jLabelVoxelDepth, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelVoxelDepth.setText("Voxel depth:");
				jLabelVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelWidth = new JTextField();
				this.add(jTextFieldPixelWidth, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldPixelWidth.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelHeight = new JTextField();
				this.add(jTextFieldPixelHeight, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldPixelHeight.setFont(SMALL_FONT);
			}
			{
				jTextFieldVoxelDepth = new JTextField();
				this.add(jTextFieldVoxelDepth, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jLabelUnits1 = new JLabel();
				this.add(jLabelUnits1, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits1.setText("units");
				jLabelUnits1.setFont(SMALL_FONT);
			}
			{
				jLabelUnits2 = new JLabel();
				this.add(jLabelUnits2, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits2.setText("units");
				jLabelUnits2.setFont(SMALL_FONT);
			}
			{
				jLabelUnits3 = new JLabel();
				this.add(jLabelUnits3, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits3.setText("units");
				jLabelUnits3.setFont(SMALL_FONT);
			}
			{
				jLabelCropSetting = new JLabel();
				this.add(jLabelCropSetting, new GridBagConstraints(0, 8, 3, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
				jLabelCropSetting.setText("Crop settings (in pixels):");
				jLabelCropSetting.setFont(FONT);
			}
			{
				jLabelX = new JLabel();
				this.add(jLabelX, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelX.setText("X");
				jLabelX.setFont(SMALL_FONT);
			}
			{
				jLabelY = new JLabel();
				this.add(jLabelY, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelY.setText("Y");
				jLabelY.setFont(SMALL_FONT);
			}
			{
				jLabelZ = new JLabel();
				this.add(jLabelZ, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelZ.setText("Z");
				jLabelZ.setFont(SMALL_FONT);
			}
			{
				jTextFieldXStart = new JTextField();
				this.add(jTextFieldXStart, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldXStart.setSize(30, 22);
				jTextFieldXStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldYStart = new JTextField();
				this.add(jTextFieldYStart, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldYStart.setSize(30, 22);
				jTextFieldYStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldZStart = new JTextField();
				this.add(jTextFieldZStart, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldZStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo1 = new JLabel();
				this.add(jLabelTo1, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo1.setText("to");
				jLabelTo1.setFont(SMALL_FONT);
			}
			{
				jLabelTo2 = new JLabel();
				this.add(jLabelTo2, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo2.setText("to");
				jLabelTo2.setFont(SMALL_FONT);
			}
			{
				jLabelTo3 = new JLabel();
				this.add(jLabelTo3, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo3.setText("to");
				jLabelTo3.setFont(SMALL_FONT);
			}
			{
				jTextFieldXEnd = new JTextField();
				this.add(jTextFieldXEnd, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldXEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldYEnd = new JTextField();
				this.add(jTextFieldYEnd, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldYEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldZEnd = new JTextField();
				this.add(jTextFieldZEnd, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldZEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZEnd.setFont(SMALL_FONT);
			}
			{
				jLabelT = new JLabel();
				this.add(jLabelT, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelT.setText("T");
				jLabelT.setFont(SMALL_FONT);
			}
			{
				jTextFieldTStart = new JTextField();
				this.add(jTextFieldTStart, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldTStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo4 = new JLabel();
				this.add(jLabelTo4, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo4.setText("to");
				jLabelTo4.setFont(SMALL_FONT);
			}
			{
				jTextFieldTEnd = new JTextField();
				this.add(jTextFieldTEnd, new GridBagConstraints(2, 12, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldTEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTEnd.setFont(SMALL_FONT);
			}
			{
				jButtonRefresh = new JButton();
				this.add(jButtonRefresh, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
				jButtonRefresh.setText("Refresh");
				jButtonRefresh.setFont(FONT);
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
		ImagePlus imp = NewImage.createByteImage("Test_image", 20, 100, 20, NewImage.FILL_BLACK);
		imp.setDimensions(1, 5, 4);
		imp.getCalibration().setUnit("um");
		imp.getCalibration().pixelDepth = 2;
		imp.getCalibration().pixelHeight = 0.4;
		imp.getCalibration().pixelWidth = 0.4;
		imp.setRoi(new Roi(10, 20, 5, 60));
		imp.show();
		frame.getContentPane().add(new StartDialogPanel());
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		WindowManager.setCurrentWindow(imp.getWindow());
	}

}
