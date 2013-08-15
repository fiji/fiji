package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;

public class StartDialogPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -1L;
	private static final String TOOLTIP = "<html>" +
			"Pressing this button will make the current <br>" +
			"ImagePlus the source for TrackMate. If the <br>" +
			"image has a ROI, it will be used to set the <br>" +
			"crop rectangle as well.</html>";

	/** ActionEvent fired when the user press the refresh button. */
	private final ActionEvent IMAGEPLUS_REFRESHED = new ActionEvent(this, 0, "ImagePlus refreshed");

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
	private JLabel jLabelTimeInterval;
	private JLabel jLabelUnits4;
	private ImagePlus imp;
	private boolean impValid = false;

	public StartDialogPanel() {
		initGUI();
	}
	


	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Returns <code>true</code> if the {@link ImagePlus} selected is valid and can
	 * be processed.
	 * @return  a boolean flag.
	 */
	public boolean isImpValid() {
		return impValid;
	}
	
	/**
	 * Update the specified settings object, with the parameters set in this panel.	
	 * @param settings  the Settings to update. Cannot be <code>null</code>.
	 */
	public void updateTo(Model model, Settings settings) {
		settings.imp = imp;
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
		settings.width 		= imp.getWidth();
		settings.height		= imp.getHeight();
		settings.nslices	= imp.getNSlices();
		settings.nframes	= imp.getNFrames();
		// Units
		model.setPhysicalUnits(jLabelUnits1.getText(), jLabelUnits4.getText());
		// Roi
		Roi roi = imp.getRoi();
		if (null != roi) {
			settings.polygon = roi.getPolygon();
		}
		// File info
		if (null != imp.getOriginalFileInfo()) {
			settings.imageFileName	= imp.getOriginalFileInfo().fileName;
			settings.imageFolder 	= imp.getOriginalFileInfo().directory;
		}
	}


	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Fill the text fields with the parameters grabbed in the {@link Settings} argument.
	 */
	public void echoSettings(Model model, Settings settings) {
		jLabelImageName.setText(settings.imp.getTitle());
		jTextFieldPixelWidth.setText(""+settings.dx);
		jTextFieldPixelHeight.setText(""+settings.dy);
		jTextFieldVoxelDepth.setText(""+settings.dz);
		jTextFieldTimeInterval.setText(""+settings.dt);
		jLabelUnits1.setText(model.getSpaceUnits());
		jLabelUnits2.setText(model.getSpaceUnits());
		jLabelUnits3.setText(model.getSpaceUnits());
		jLabelUnits4.setText(model.getTimeUnits());
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
	 * Fill the text fields with parameters grabbed from specified ImagePlus. 
	 */
	public void getFrom(ImagePlus imp) {
		
		this.imp = imp;
		
		if (null == imp) {
			jLabelImageName.setText("No image selected.");
			impValid = false;
			return;
		}
		
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			// We do not know how to process RGB images
			jLabelImageName.setText(imp.getShortTitle()+" is RGB: invalid.");
			impValid = false;
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
		jTextFieldXStart.setText(""+(boundingRect.x)); 
		jTextFieldYStart.setText(""+(boundingRect.y));
		jTextFieldXEnd.setText(""+(boundingRect.width+boundingRect.x-1));
		jTextFieldYEnd.setText(""+(boundingRect.height+boundingRect.y-1));
		jTextFieldZStart.setText(""+0);
		jTextFieldZEnd.setText(""+(imp.getNSlices()-1));
		jTextFieldTStart.setText(""+0); 
		jTextFieldTEnd.setText(""+(imp.getNFrames()-1));
		
		impValid = true;
	}
	
	private void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(266, 476));
			setLayout(null);
			{
				jLabelImageName = new JLabel();
				jLabelImageName.setBounds(10, 14, 245, 17);
				this.add(jLabelImageName);
				jLabelImageName.setText("Select an image, and press refresh.");
				jLabelImageName.setFont(BIG_FONT);
			}
			{
				jLabelCheckCalibration = new JLabel();
				jLabelCheckCalibration.setBounds(10, 107, 93, 13);
				this.add(jLabelCheckCalibration);
				jLabelCheckCalibration.setText("Calibration settings:");
				jLabelCheckCalibration.setFont(SMALL_FONT);
			}
			{
				jLabelPixelWidth = new JLabel();
				jLabelPixelWidth.setBounds(63, 131, 55, 13);
				this.add(jLabelPixelWidth);
				jLabelPixelWidth.setText("Pixel width:");
				jLabelPixelWidth.setFont(SMALL_FONT);
			}
			{
				jLabelPixelHeight = new JLabel();
				jLabelPixelHeight.setBounds(58, 151, 60, 13);
				this.add(jLabelPixelHeight);
				jLabelPixelHeight.setText("Pixel height:");
				jLabelPixelHeight.setFont(SMALL_FONT);
			}
			{
				jLabelVoxelDepth = new JLabel();
				jLabelVoxelDepth.setBounds(58, 171, 60, 13);
				this.add(jLabelVoxelDepth);
				jLabelVoxelDepth.setText("Voxel depth:");
				jLabelVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jLabelTimeInterval = new JLabel();
				jLabelTimeInterval.setBounds(52, 191, 66, 13);
				this.add(jLabelTimeInterval);
				jLabelTimeInterval.setText("Time interval:" );				
				jLabelTimeInterval.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelWidth = new JNumericTextField();
				jTextFieldPixelWidth.setBounds(128, 130, 40, 15);
				this.add(jTextFieldPixelWidth);
				jTextFieldPixelWidth.setFont(SMALL_FONT);
			}
			{
				jTextFieldPixelHeight = new JNumericTextField();
				jTextFieldPixelHeight.setBounds(128, 150, 40, 15);
				this.add(jTextFieldPixelHeight);
				jTextFieldPixelHeight.setFont(SMALL_FONT);
			}
			{
				jTextFieldVoxelDepth = new JNumericTextField();
				jTextFieldVoxelDepth.setBounds(128, 170, 40, 15);
				this.add(jTextFieldVoxelDepth);
				jTextFieldVoxelDepth.setFont(SMALL_FONT);
			}
			{
				jTextFieldTimeInterval = new JNumericTextField();
				jTextFieldTimeInterval.setBounds(128, 190, 40, 15);
				this.add(jTextFieldTimeInterval);
				jTextFieldTimeInterval.setFont(SMALL_FONT);
			}
			{
				jLabelUnits1 = new JLabel();
				jLabelUnits1.setBounds(178, 131, 77, 13);
				this.add(jLabelUnits1);
				jLabelUnits1.setText("units");
				jLabelUnits1.setFont(SMALL_FONT);
			}
			{
				jLabelUnits2 = new JLabel();
				jLabelUnits2.setBounds(178, 151, 77, 13);
				this.add(jLabelUnits2);
				jLabelUnits2.setText("units");
				jLabelUnits2.setFont(SMALL_FONT);
			}
			{
				jLabelUnits3 = new JLabel();
				jLabelUnits3.setBounds(178, 171, 77, 13);
				this.add(jLabelUnits3);
				jLabelUnits3.setText("units");
				jLabelUnits3.setFont(SMALL_FONT);
			}
			{
				jLabelUnits4 = new JLabel();
				jLabelUnits4.setBounds(178, 191, 78, 13);
				this.add(jLabelUnits4);
				jLabelUnits4.setText("units");
				jLabelUnits4.setFont(SMALL_FONT);
			}
			{
				jLabelCropSetting = new JLabel();
				jLabelCropSetting.setBounds(10, 237, 111, 13);
				this.add(jLabelCropSetting);
				jLabelCropSetting.setText("Crop settings (in pixels, 0-based):");
				jLabelCropSetting.setFont(SMALL_FONT);
			}
			{
				jLabelX = new JLabel();
				jLabelX.setBounds(58, 262, 7, 13);
				this.add(jLabelX);
				jLabelX.setText("X");
				jLabelX.setFont(SMALL_FONT);
			}
			{
				jLabelY = new JLabel();
				jLabelY.setBounds(58, 285, 7, 13);
				this.add(jLabelY);
				jLabelY.setText("Y");
				jLabelY.setFont(SMALL_FONT);
			}
			{
				jLabelZ = new JLabel();
				jLabelZ.setBounds(58, 308, 6, 13);
				this.add(jLabelZ);
				jLabelZ.setText("Z");
				jLabelZ.setFont(SMALL_FONT);
			}
			{
				jTextFieldXStart = new JNumericTextField();
				jTextFieldXStart.setLocation(78, 260);
				this.add(jTextFieldXStart);
				jTextFieldXStart.setSize(40, 18);
				jTextFieldXStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldYStart = new JNumericTextField();
				jTextFieldYStart.setLocation(78, 283);
				this.add(jTextFieldYStart);
				jTextFieldYStart.setSize(40, 18);
				jTextFieldYStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYStart.setFont(SMALL_FONT);
			}
			{
				jTextFieldZStart = new JNumericTextField();
				jTextFieldZStart.setBounds(78, 306, 40, 18);
				this.add(jTextFieldZStart);
				jTextFieldZStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo1 = new JLabel();
				jLabelTo1.setBounds(146, 262, 9, 13);
				this.add(jLabelTo1);
				jLabelTo1.setText("to");
				jLabelTo1.setFont(SMALL_FONT);
			}
			{
				jLabelTo2 = new JLabel();
				jLabelTo2.setBounds(146, 285, 9, 13);
				this.add(jLabelTo2);
				jLabelTo2.setText("to");
				jLabelTo2.setFont(SMALL_FONT);
			}
			{
				jLabelTo3 = new JLabel();
				jLabelTo3.setBounds(146, 308, 9, 13);
				this.add(jLabelTo3);
				jLabelTo3.setText("to");
				jLabelTo3.setFont(SMALL_FONT);
			}
			{
				jTextFieldXEnd = new JNumericTextField();
				jTextFieldXEnd.setBounds(178, 260, 40, 18);
				this.add(jTextFieldXEnd);
				jTextFieldXEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldXEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldYEnd = new JNumericTextField();
				jTextFieldYEnd.setBounds(178, 283, 40, 18);
				this.add(jTextFieldYEnd);
				jTextFieldYEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldYEnd.setFont(SMALL_FONT);
			}
			{
				jTextFieldZEnd = new JNumericTextField();
				jTextFieldZEnd.setBounds(178, 306, 40, 18);
				this.add(jTextFieldZEnd);
				jTextFieldZEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldZEnd.setFont(SMALL_FONT);
			}
			{
				jLabelT = new JLabel();
				jLabelT.setBounds(58, 332, 7, 13);
				this.add(jLabelT);
				jLabelT.setText("T");
				jLabelT.setFont(SMALL_FONT);
			}
			{
				jTextFieldTStart = new JNumericTextField();
				jTextFieldTStart.setBounds(78, 330, 40, 18);
				this.add(jTextFieldTStart);
				jTextFieldTStart.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTStart.setFont(SMALL_FONT);
			}
			{
				jLabelTo4 = new JLabel();
				jLabelTo4.setBounds(146, 332, 9, 13);
				this.add(jLabelTo4);
				jLabelTo4.setText("to");
				jLabelTo4.setFont(SMALL_FONT);
			}
			{
				jTextFieldTEnd = new JNumericTextField();
				jTextFieldTEnd.setBounds(178, 330, 40, 18);
				this.add(jTextFieldTEnd);
				jTextFieldTEnd.setPreferredSize(TEXTFIELD_DIMENSION);
				jTextFieldTEnd.setFont(SMALL_FONT);
			}
			
			{
				jButtonRefresh = new JButton();
				jButtonRefresh.setBounds(10, 422, 108, 29);
				this.add(jButtonRefresh);
				jButtonRefresh.setText("Refresh source");
				jButtonRefresh.setToolTipText(TOOLTIP);
				jButtonRefresh.setFont(SMALL_FONT);

				jButtonRefresh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						imp = WindowManager.getCurrentImage();
						getFrom(imp);
						fireAction(IMAGEPLUS_REFRESHED);
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
