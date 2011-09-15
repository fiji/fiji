package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;
import fiji.plugin.trackmate.Settings;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class StartDialogPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -5495612173611259921L;
	private JLabel jLabelCheckCalibration;
	private JNumericTextField jTextFieldPixelWidth;
	private JNumericTextField jTextFieldZStart;
	private JNumericTextField jTextFieldYStart;
	private JNumericTextField jTextFieldXStart;
	private JLabel jLabelCropSetting;
	private JButton jButtonRefresh;
	private JNumericTextField jTextFieldTEnd;
	private JLabel jLabelTo4;
	private JNumericTextField jTextFieldTStart;
	private JLabel jLabelT;
	private JNumericTextField jTextFieldZEnd;
	private JNumericTextField jTextFieldYEnd;
	private JNumericTextField jTextFieldXEnd;
	private JLabel jLabelTo3;
	private JLabel jLabelTo2;
	private JLabel jLabelTo1;
	private JLabel jLabelZ;
	private JLabel jLabelY;
	private JLabel jLabelX;
	private JLabel jLabelUnits3;
	private JLabel jLabelUnits2;
	private JLabel jLabelUnits1;
	private JNumericTextField jTextFieldVoxelDepth;
	private JNumericTextField jTextFieldPixelHeight;
	private JLabel jLabelVoxelDepth;
	private JLabel jLabelPixelHeight;
	private JLabel jLabelPixelWidth;
	private JLabel jLabelImageName;
	private JNumericTextField jTextFieldTimeInterval;
	private GridBagLayout thisLayout;
	private JLabel jLabelTimeInterval;
	private JLabel jLabelUnits4;

	private ImagePlus imp;
	private Settings settings;
	private Component target;
	
	/**
	 * Create and lay out of new {@link StartDialogPanel}, with the default settings given, and a target component.
	 * The later will be disabled, until a valid {@link ImagePlus} is set in the {@link Settings} field returned
	 * by this panel.
	 */
	public StartDialogPanel(Settings settings, Component target) {		
		super();
		if (null == settings)
			settings = new Settings();
		this.settings = settings;
		this.target = target;
		initGUI();
		if (null == settings.imp) {
			refresh();
		} else {
			echoSettings(settings);
			imp = settings.imp;
			refresh();
		}
	}

	/*
	 * PUBLIC METHODS
	 */
	

	
	/**
	 * Update the settings object given with the parameters this panel allow to tune
	 * this plugin.	
	 * @param settings  the Settings to update. If <code>null</code>, a new default one
	 * is created.
	 * @return  the updated Settings
	 */
	public Settings getSettings() {
		settings.imp =  imp;
		// Crop cube
		settings.tstart = Math.round(Float.parseFloat(jTextFieldTStart.getText()));
		settings.tend 	= Math.round(Float.parseFloat(jTextFieldTEnd.getText()));
		settings.xstart = Math.round(Float.parseFloat(jTextFieldXStart.getText()));
		settings.xend 	= Math.round(Float.parseFloat(jTextFieldXEnd.getText()));
		settings.ystart = Math.round(Float.parseFloat(jTextFieldYStart.getText()));
		settings.yend 	= Math.round(Float.parseFloat(jTextFieldYEnd.getText()));
		settings.zstart = Math.round(Float.parseFloat(jTextFieldZStart.getText()));
		settings.zend 	= Math.round(Float.parseFloat(jTextFieldZEnd.getText()));
		// Image info
		settings.dx 	= Float.parseFloat(jTextFieldPixelWidth.getText());
		settings.dy 	= Float.parseFloat(jTextFieldPixelHeight.getText());
		settings.dz 	= Float.parseFloat(jTextFieldVoxelDepth.getText());
		settings.dt 	= Float.parseFloat(jTextFieldTimeInterval.getText());
		settings.spaceUnits = jLabelUnits1.getText();
		settings.timeUnits  = jLabelUnits4.getText();
		settings.width 		= imp.getWidth();
		settings.height		= imp.getHeight();
		settings.nslices	= imp.getNSlices();
		settings.nframes	= imp.getNFrames();
		if (null != settings.imp.getOriginalFileInfo()) {
			settings.imageFileName	= imp.getOriginalFileInfo().fileName;
			settings.imageFolder 	= imp.getOriginalFileInfo().directory;
		}
		return settings;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Fill the text fields with the parameters grabbed in the {@link Settings} argument.
	 */
	private void echoSettings(Settings settings) {
		jLabelImageName.setText(settings.imp.getTitle());
		jTextFieldPixelWidth.setText(""+settings.dx);
		jTextFieldPixelHeight.setText(""+settings.dy);
		jTextFieldVoxelDepth.setText(""+settings.dz);
		jTextFieldTimeInterval.setText(""+settings.dt);
		jLabelUnits1.setText(settings.spaceUnits);
		jLabelUnits2.setText(settings.spaceUnits);
		jLabelUnits3.setText(settings.spaceUnits);
		jLabelUnits4.setText(settings.timeUnits);
		jTextFieldXStart.setText(""+settings.xstart); 
		jTextFieldYStart.setText(""+settings.ystart);
		jTextFieldXEnd.setText(""+settings.xend);
		jTextFieldYEnd.setText(""+settings.yend);
		jTextFieldZStart.setText(""+settings.zstart);
		jTextFieldZEnd.setText(""+settings.zend);
		jTextFieldTStart.setText(""+settings.tstart); 
		jTextFieldTEnd.setText(""+settings.tend);
	}
	
	
	/**
	 * Fill the text fields with parameters grabbed from current ImagePlus. If the image is valid,
	 * enable the {@link #target} component.
	 */
	private void refresh() {
		imp = WindowManager.getCurrentImage();
		if (null == imp) {
			if (null != target) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						target.setEnabled(false);
					}
				});
			}
			return;
		}
		jLabelImageName.setText("Target: "+imp.getShortTitle());
		jTextFieldPixelWidth.setText(String.format("%g", imp.getCalibration().pixelWidth));
		jTextFieldPixelHeight.setText(String.format("%g", imp.getCalibration().pixelHeight));
		jTextFieldVoxelDepth.setText(String.format("%g", imp.getCalibration().pixelDepth));
		if (imp.getCalibration().frameInterval == 0) {
			jTextFieldTimeInterval.setText("1");
			jLabelUnits4.setText("frame");
		} else {
			jTextFieldTimeInterval.setText(String.format("%g", imp.getCalibration().frameInterval));
			jLabelUnits4.setText(imp.getCalibration().getTimeUnit());
		}
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

		if (null != target)
			target.setEnabled(true);
	}
	
	
	private void initGUI() {
		try {
			thisLayout = new GridBagLayout();
			thisLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
			thisLayout.rowHeights = new int[] {50, 20, 20, 20, 20, 50, 20, 20, 20, 20, 50, 20, 20, 20, 25, 7, 15};
			thisLayout.columnWeights = new double[] {0.1, 0.0, 0.1};
			thisLayout.columnWidths = new int[] {7, 50, 20};
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(266, 476));
			{
				jLabelImageName = new JLabel();
				this.add(jLabelImageName, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabelImageName.setText("Select an image, and press refresh.");
				jLabelImageName.setFont(BIG_FONT);
			}
			{
				jLabelCheckCalibration = new JLabel();
				this.add(jLabelCheckCalibration, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
				jLabelCheckCalibration.setText("Calibration settings:");
				jLabelCheckCalibration.setFont(SMALL_FONT);
			}
			{
				jLabelPixelWidth = new JLabel();
				this.add(jLabelPixelWidth, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelPixelWidth.setText("Pixel width:");
				jLabelPixelWidth.setFont(SMALL_FONT);
			}
			{
				jLabelPixelHeight = new JLabel();
				this.add(jLabelPixelHeight, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelPixelHeight.setText("Pixel height:");
				jLabelPixelHeight.setFont(SMALL_FONT);
			}
			{
				jLabelVoxelDepth = new JLabel();
				this.add(jLabelVoxelDepth, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelVoxelDepth.setText("Voxel depth:");
				jLabelVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jLabelTimeInterval = new JLabel();
				this.add(jLabelTimeInterval, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jLabelTimeInterval.setText("Time interval:" );				
				jLabelTimeInterval.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelWidth = new JNumericTextField();
				this.add(jTextFieldPixelWidth, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldPixelWidth.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelHeight = new JNumericTextField();
				this.add(jTextFieldPixelHeight, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldPixelHeight.setFont(SMALL_FONT);
			}
			{
				jTextFieldVoxelDepth = new JNumericTextField();
				this.add(jTextFieldVoxelDepth, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jTextFieldTimeInterval = new JNumericTextField();
				this.add(jTextFieldTimeInterval, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldTimeInterval.setFont(SMALL_FONT);
			}
			{
				jLabelUnits1 = new JLabel();
				this.add(jLabelUnits1, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits1.setText("units");
				jLabelUnits1.setFont(SMALL_FONT);
			}
			{
				jLabelUnits2 = new JLabel();
				this.add(jLabelUnits2, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits2.setText("units");
				jLabelUnits2.setFont(SMALL_FONT);
			}
			{
				jLabelUnits3 = new JLabel();
				this.add(jLabelUnits3, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits3.setText("units");
				jLabelUnits3.setFont(SMALL_FONT);
			}
			{
				jLabelUnits4 = new JLabel();
				this.add(jLabelUnits4, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelUnits4.setText("units");
				jLabelUnits4.setFont(SMALL_FONT);
			}
			{
				jLabelCropSetting = new JLabel();
				this.add(jLabelCropSetting, new GridBagConstraints(0, 10, 3, 1, 0.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
				jLabelCropSetting.setText("Crop settings (in pixels):");
				jLabelCropSetting.setFont(SMALL_FONT);
			}
			{
				jLabelX = new JLabel();
				this.add(jLabelX, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelX.setText("X");
				jLabelX.setFont(SMALL_FONT);
			}
			{
				jLabelY = new JLabel();
				this.add(jLabelY, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelY.setText("Y");
				jLabelY.setFont(SMALL_FONT);
			}
			{
				jLabelZ = new JLabel();
				this.add(jLabelZ, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelZ.setText("Z");
				jLabelZ.setFont(SMALL_FONT);
			}
			{
				jTextFieldXStart = new JNumericTextField();
				this.add(jTextFieldXStart, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldXStart.setSize(30, 22);
				jTextFieldXStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldYStart = new JNumericTextField();
				this.add(jTextFieldYStart, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldYStart.setSize(30, 22);
				jTextFieldYStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldZStart = new JNumericTextField();
				this.add(jTextFieldZStart, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldZStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo1 = new JLabel();
				this.add(jLabelTo1, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo1.setText("to");
				jLabelTo1.setFont(SMALL_FONT);
			}
			{
				jLabelTo2 = new JLabel();
				this.add(jLabelTo2, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo2.setText("to");
				jLabelTo2.setFont(SMALL_FONT);
			}
			{
				jLabelTo3 = new JLabel();
				this.add(jLabelTo3, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo3.setText("to");
				jLabelTo3.setFont(SMALL_FONT);
			}
			{
				jTextFieldXEnd = new JNumericTextField();
				this.add(jTextFieldXEnd, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldXEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldYEnd = new JNumericTextField();
				this.add(jTextFieldYEnd, new GridBagConstraints(2, 12, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldYEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldZEnd = new JNumericTextField();
				this.add(jTextFieldZEnd, new GridBagConstraints(2, 13, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldZEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZEnd.setFont(SMALL_FONT);
			}
			{
				jLabelT = new JLabel();
				this.add(jLabelT, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelT.setText("T");
				jLabelT.setFont(SMALL_FONT);
			}
			{
				jTextFieldTStart = new JNumericTextField();
				this.add(jTextFieldTStart, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
				jTextFieldTStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo4 = new JLabel();
				this.add(jLabelTo4, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jLabelTo4.setText("to");
				jLabelTo4.setFont(SMALL_FONT);
			}
			{
				jTextFieldTEnd = new JNumericTextField();
				this.add(jTextFieldTEnd, new GridBagConstraints(2, 14, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldTEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTEnd.setFont(SMALL_FONT);
			}
			{
				jButtonRefresh = new JButton();
				this.add(jButtonRefresh, new GridBagConstraints(0, 16, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
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
		ij.ImageJ.main(args);
		ImagePlus imp = NewImage.createByteImage("Test_image", 20, 100, 20, NewImage.FILL_BLACK);
		imp.setDimensions(1, 5, 4);
		imp.getCalibration().setUnit("um");
		imp.getCalibration().pixelDepth = 2;
		imp.getCalibration().pixelHeight = 0.4;
		imp.getCalibration().pixelWidth = 0.4;
		imp.setRoi(new Roi(10, 20, 5, 60));
		imp.show();
		frame.getContentPane().add(new StartDialogPanel(null, null));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		WindowManager.setCurrentWindow(imp.getWindow());
	}

}
