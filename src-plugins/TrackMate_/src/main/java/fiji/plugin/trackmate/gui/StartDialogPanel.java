package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate_;
import fiji.util.NumberParser;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

public class StartDialogPanel extends ActionListenablePanel implements WizardPanelDescriptor {

	private static final long serialVersionUID = -1L;

	public static final String DESCRIPTOR = "StartDialog";

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
	private Settings settings;

	private TrackMate_ plugin;
	private TrackMateWizard wizard;

	
	public StartDialogPanel() {
		initGUI();
	}
	
	
	/*
	 * WIZARDPANELDESCRIPTOR METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		if (null == plugin) {
			this.settings = new Settings();
		} else {
			if (null == settings) {
				this.settings = new Settings();
			} else {
				this.settings = plugin.getModel().getSettings();
			}
		}
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return DetectorChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return null;
	}

	@Override
	public void aboutToDisplayPanel() {
		if (null == settings.imp) {
			imp = WindowManager.getCurrentImage();
			refresh();
		} else {
			echoSettings(settings);
			imp = settings.imp;
			refresh();
		}
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		// Get settings and pass them to the plugin managed by the wizard
		plugin.getModel().setSettings(getSettings());
		plugin.getModel().getLogger().log(plugin.getModel().getSettings().toStringImageInfo());
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
		settings.imp = imp;
		// Crop cube
		settings.tstart = NumberParser.parseInteger(jTextFieldTStart.getText());
		settings.tend 	= NumberParser.parseInteger(jTextFieldTEnd.getText());
		settings.xstart = NumberParser.parseInteger(jTextFieldXStart.getText());
		settings.xend 	= NumberParser.parseInteger(jTextFieldXEnd.getText());
		settings.ystart = NumberParser.parseInteger(jTextFieldYStart.getText());
		settings.yend 	= NumberParser.parseInteger(jTextFieldYEnd.getText());
		settings.zstart = NumberParser.parseInteger(jTextFieldZStart.getText());
		settings.zend 	= NumberParser.parseInteger(jTextFieldZEnd.getText());
		// Image info
		settings.dx 	= NumberParser.parseDouble(jTextFieldPixelWidth.getText());
		settings.dy 	= NumberParser.parseDouble(jTextFieldPixelHeight.getText());
		settings.dz 	= NumberParser.parseDouble(jTextFieldVoxelDepth.getText());
		settings.dt 	= NumberParser.parseDouble(jTextFieldTimeInterval.getText());
		settings.spaceUnits = jLabelUnits1.getText();
		settings.timeUnits  = jLabelUnits4.getText();
		settings.width 		= imp.getWidth();
		settings.height		= imp.getHeight();
		settings.nslices	= imp.getNSlices();
		settings.nframes	= imp.getNFrames();
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

		if (null == imp) {
			// Lock next button, because we still do not have a valid target image.
			wizard.setNextButtonEnabled(false);
			return;
		}

		if (imp.getType() == ImagePlus.COLOR_RGB) {
			// We do not know how to process RGB images
			jLabelImageName.setText(imp.getShortTitle()+" is RGB: invalid.");
			wizard.setNextButtonEnabled(false);
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

		// Re-enable target component, because we have a valid target image to operate on.
		wizard.setNextButtonEnabled(true);
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
				jButtonRefresh.setBounds(10, 430, 78, 21);
				this.add(jButtonRefresh);
				jButtonRefresh.setText("Refresh");
				jButtonRefresh.setFont(SMALL_FONT);

				jButtonRefresh.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						imp = WindowManager.getCurrentImage();
						refresh();
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
