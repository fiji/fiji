/*
 * Volume Viewer 2.0
 * 03.12.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class Gui extends JPanel implements 
MouseListener, MouseMotionListener, ChangeListener, ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	JPanel upperButtonPanel;
	JPanel lowerButtonPanel;
	JPanel transferFunctionPanel;
	private JPanel centerPanel;
	private JPanel slicePanel;

	private JCheckBox checkSlices, checkAxes, checkClipLines;

	private JComboBox renderChoice, interpolationChoice, lutChoice;
	private JCheckBox checkPickColor2, checkPickColor3;

	private JSlider scaleSlider, distSlider, positionXSlider, positionYSlider, positionZSlider; 
	private float scaleSliderValue; 

	private JLabel zAspectLabel;
	private String zAspectString = "z-Aspect:";
	private JTextField tfZaspect;

	private JLabel samplingLabel;
	private String samplingString = "Sampling:";
	private JTextField tfSampling;

	private int xStart, yStart, xAct, yAct;
	private JLabel scaleLabel1, scaleLabel2, distLabel1, distLabel2;
	private JLabel positionLabel, valueLabel;

	ImageRegion imageRegion;
	private ImageRegion sliceImageRegion;
	
	private JSpinner spinnerX, spinnerY, spinnerZ;
	private boolean enableSpinnerChangeListener = true;

	private JSlider alphaSlider1, alphaSlider2, alphaSlider3, alphaSlider4, sliderLumTolerance, sliderGradTolerance;
	private JButton autobutton1, autobutton2, autobutton3, clearbutton1, clearbutton2, clearbutton3, clearbutton4;

	private JTabbedPane transferFunctionTabbedPane;
	private JPanel lightBox;
	
	private String positionString, valueString;
	private int positionX, positionY, positionZ;
	private int maxPositionX, maxPositionY, maxPositionZ;
	
	Pic pic = null;
	Pic picSlice = null;
 
	private Control control;
	private Volume_Viewer vv;

	private JCheckBox checkLight;

	private JSlider ambientSlider;
	private JSlider diffuseSlider;
	private JSlider specularSlider;
	private JSlider shineSlider;

	private ImageRegion imageLightRegion;
	private Pic picLight;

	private ImageRegion imageLEDRegion;
	private Pic picLED;

	
	private JLabel jLabelLight;

	private JSlider objectLightSlider;
	
	public Gui(Control control, Volume_Viewer vv) {
		this.control = control;
		this.vv = vv;
	}


	void makeGui() {
		
		if (vv.tf_rgb == null) 
			vv.tf_rgb = new TFrgb(control, vv);
		if (vv.tf_a1 == null) 
			vv.tf_a1 = new TFalpha1(control, vv.vol);
		if (vv.tf_a2 == null) 
			vv.tf_a2 = new TFalpha2(control, vv.vol, vv.lookupTable.lut, vv.lookupTable.lut2D_2);
		if (vv.tf_a3 == null) 
			vv.tf_a3 = new TFalpha3(control, vv.vol, vv.lookupTable.lut, vv.lookupTable.lut2D_3);
		if (vv.tf_a4 == null) 
			vv.tf_a4 = new TFalpha4(control, vv.vol, vv.vol.aPaint_3D, vv.vol.aPaint_3D2);
		//vv.vol.calculateGradients();
		
		
		control.pickColor = false;
		if (checkPickColor2 != null) checkPickColor2.setSelected(control.pickColor);
		
		// image panel
		pic = new Pic(control, vv, control.windowWidthImageRegion, control.windowHeight);
		imageRegion = new ImageRegion(control);	
		imageRegion.setPlaneColor(control.backgroundColor);
		imageRegion.addMouseMotionListener(this);
		imageRegion.addMouseListener(this);
		imageRegion.setPic(pic);
		


		picSlice = new Pic(control, vv, control.windowWidthSlices, control.windowHeight-130);
		sliceImageRegion = new ImageRegion(control);
		sliceImageRegion.addMouseMotionListener(this);
		sliceImageRegion.addMouseListener(this);
		sliceImageRegion.setPic(picSlice);
		sliceImageRegion.newLines(3); 
		sliceImageRegion.newText(3);  

		positionX = (int) ((vv.vol.widthV-1)*control.positionFactorX);
		positionY = (int) ((vv.vol.heightV-1)*control.positionFactorY);
		positionZ = (int) ((vv.vol.depthV-1)*control.positionFactorZ)+1;
		picSlice.drawSlices();

		slicePanel = new JPanel();
		slicePanel.setLayout(new BorderLayout());
		slicePanel.add(sliceImageRegion, BorderLayout.NORTH);
		


		JPanel sliderBox = new JPanel();
		sliderBox.setLayout(new GridLayout(3,1));

		Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );

		maxPositionZ = vv.vol.depthV-1;
		positionZSlider = new JSlider(JSlider.HORIZONTAL, 0, maxPositionZ, (int)(maxPositionZ*control.positionFactorZ)); 
		positionZSlider.setBorder( new TitledBorder(empty, "xy", TitledBorder.LEADING, TitledBorder.TOP, new Font("Sans", Font.PLAIN, 12), new Color(0, 0xBB, 0xBB))); //Color.cyan));
		positionZSlider.setPreferredSize(new Dimension((int) (control.windowWidthSlices*0.25), 30));
		positionZSlider.setSnapToTicks(true);
		positionZSlider.addChangeListener( this );
		positionZSlider.addMouseListener(this);
		sliderBox.add(positionZSlider);	

		maxPositionX = vv.vol.widthV-1;
		positionXSlider = new JSlider(JSlider.HORIZONTAL, 0, maxPositionX, (int)(maxPositionX*control.positionFactorX)); 
		positionXSlider.setBorder( new TitledBorder(empty, "yz", TitledBorder.LEADING, TitledBorder.TOP, new Font("Sans", Font.PLAIN, 12), new Color(0, 150,0)));
		positionXSlider.setPreferredSize(new Dimension((int) (control.windowWidthSlices*0.25), 30));
		positionXSlider.setSnapToTicks(true);
		positionXSlider.addChangeListener( this );
		positionXSlider.addMouseListener(this);
		sliderBox.add(positionXSlider);	

		maxPositionY = vv.vol.heightV-1;
		positionYSlider = new JSlider(JSlider.HORIZONTAL, 0, maxPositionY, (int)(maxPositionY*control.positionFactorY)); 
		positionYSlider.setBorder( new TitledBorder(empty, "xz", TitledBorder.LEADING, TitledBorder.TOP, new Font("Sans", Font.PLAIN, 12), Color.red));
		positionYSlider.setPreferredSize(new Dimension((int) (control.windowWidthSlices*0.25), 30));
		positionYSlider.setSnapToTicks(true);
		positionYSlider.addChangeListener( this );
		positionYSlider.addMouseListener(this);
		sliderBox.add(positionYSlider);	

		slicePanel.add(sliderBox,BorderLayout.CENTER);
		


		JPanel labelBox = new JPanel();
		labelBox.setLayout(new GridLayout(2,1));
		positionLabel = new JLabel(positionString);
		positionLabel.setPreferredSize(new Dimension((int) (control.windowWidthSlices*0.9), 20));
		labelBox.add(positionLabel);
		valueLabel = new JLabel(valueString);
		valueLabel.setPreferredSize(new Dimension((int) (control.windowWidthSlices*0.9), 20));
		labelBox.add(valueLabel);

		slicePanel.add(labelBox, BorderLayout.SOUTH);

		vv.cube.initTextsAndDrawColors(imageRegion);

		centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());

		centerPanel.add(imageRegion,BorderLayout.CENTER);
		centerPanel.add(slicePanel,BorderLayout.WEST);

		
		// upper button panel
		upperButtonPanel = new JPanel();
		//upperButtonPanel.setLayout(new GridLayout(1,0));

		String renderString = "Mode:";
		JLabel renderLabel = new JLabel(renderString);
		upperButtonPanel.add(renderLabel);
		renderChoice = new JComboBox(Control.renderName);
		renderChoice.setSelectedIndex(control.renderMode);
		renderChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
		renderChoice.addActionListener(this);		
		renderChoice.setPreferredSize(new Dimension(160,24)); 
		upperButtonPanel.add(renderChoice);

		String interpolationString = "Interpolation:";
		JLabel interpolationLabel = new JLabel(interpolationString);
		upperButtonPanel.add(interpolationLabel);
		interpolationChoice = new JComboBox(Control.interpolationName);
		interpolationChoice.setSelectedIndex(control.interpolationMode);
		interpolationChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
		interpolationChoice.addActionListener(this);	
		interpolationChoice.setPreferredSize(new Dimension(160,24)); 
		upperButtonPanel.add(interpolationChoice);

		JPanel miniPanelZ = new JPanel();
		//miniPanelZ.setLayout(new GridLayout(1,2));
		zAspectLabel = new JLabel(zAspectString);
		zAspectLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		miniPanelZ.add(zAspectLabel);

		tfZaspect = new JTextField();
		tfZaspect.setText("" + control.zAspect);
		tfZaspect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.zAspect = Float.parseFloat(tfZaspect.getText());
				vv.setZAspect();
				vv.initializeTransformation();
				vv.buildFrame();
			}
		});
		miniPanelZ.add(tfZaspect);
		upperButtonPanel.add(miniPanelZ);

		JPanel miniPanelSampling = new JPanel();
		//miniPanelSampling.setLayout(new GridLayout(1,2));

		samplingLabel = new JLabel(samplingString);
		samplingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		miniPanelSampling.add(samplingLabel);

		tfSampling = new JTextField();
		tfSampling.setText("" + control.sampling);
		tfSampling.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.sampling = Float.parseFloat(tfSampling.getText());
				if (control.sampling <= 0) 
					control.sampling = 1;
				if (control.sampling > 20) 
					control.sampling = 20;

				tfSampling.setText("" + control.sampling);	
				newDisplayMode();
			}
		});
		miniPanelSampling.add(tfSampling);
		upperButtonPanel.add(miniPanelSampling);

		JButton buttonBackgroundColor = new JButton("Background");
		buttonBackgroundColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color bgColor = JColorChooser.showDialog(null, "Choose background color", null);
				if (bgColor != null){
					control.backgroundColor = bgColor;
					imageRegion.setPlaneColor(bgColor);
					newDisplayMode();
				}
			}
		});
		upperButtonPanel.add(buttonBackgroundColor); 

		JButton buttonSaveView = new JButton("Snapshot");
		buttonSaveView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imageRegion.saveToImage();
			}
		});
		upperButtonPanel.add(buttonSaveView); 
		
		
		JButton buttonReset = new JButton("Reset");
		buttonReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				vv.reset();
			}
		});
		upperButtonPanel.add(buttonReset); 


		// slider panel (dist & scale) ===========================================
		JPanel sliderPanel = new JPanel();
		sliderPanel.setPreferredSize(new Dimension(control.windowWidthSliderRegion, control.windowHeight));
		sliderPanel.setMaximumSize(new Dimension(control.windowWidthSliderRegion, control.windowHeight));
		sliderPanel.setLayout(new GridLayout(0,1));
				
		control.maxDist = (int)(Math.sqrt(vv.vol.zOffa*vv.vol.zOffa*control.zAspect*control.zAspect + vv.vol.yOffa*vv.vol.yOffa +vv.vol.xOffa*vv.vol.xOffa));
		if (!control.distWasSet && control.renderMode >= Control.PROJECTION_MAX)
			control.dist = -control.maxDist;
		control.dist = Math.min(Math.max(control.dist, -control.maxDist), control.maxDist);

		JPanel panelDist = new JPanel();	
		
		picLED = new Pic(control, vv, 15, 15);
		imageLEDRegion = new ImageRegion(control);	
		imageLEDRegion.setPlaneColor(UIManager.getColor ( "Panel.background" ));
		imageLEDRegion.setPic(picLED);
		panelDist.add(imageLEDRegion);
		picLED.render_LED(true);
		imageLEDRegion.setImage(picLED.image);
		imageLEDRegion.repaint();
		
		String textOnButton = (control.showTF) ?"<html><body><center>Hide<br>TF</center></body></html>" :
			"<html><body><center>Show<br>TF</center></body></html>";
		JButton tfButton = new JButton(textOnButton);
		tfButton.setMargin(new Insets(0,-30, 0,-30));
		tfButton.setPreferredSize(new Dimension(50,40));
		panelDist.add(tfButton);
		tfButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				control.showTF = !control.showTF;
				vv.buildFrame();
			}
		});

		panelDist.setPreferredSize(new Dimension(control.windowWidthSliderRegion, (control.windowHeight-20)/2));
		distSlider = new JSlider(JSlider.VERTICAL, -control.maxDist*2, control.maxDist*2, (int)(2*control.dist) );
		distSlider.setPreferredSize(new Dimension(20,(control.windowHeight-220)/2));
		distSlider.addChangeListener( this );
		distSlider.addMouseListener(this);

		distLabel1 = new JLabel(""+ control.dist);
		distLabel1.setHorizontalAlignment(SwingConstants.CENTER);
		distLabel1.setPreferredSize(new Dimension(control.windowWidthSliderRegion, 15));
		distLabel2 = new JLabel("Distance");
		distLabel2.setFont(new Font("Sans", Font.PLAIN, 11));
		distLabel2.setHorizontalAlignment(SwingConstants.CENTER);
		distLabel2.setPreferredSize(new Dimension(control.windowWidthSliderRegion, 15));

		panelDist.add(distSlider); 
		panelDist.add(distLabel1);
		panelDist.add(distLabel2);
		sliderPanel.add(panelDist); 

		String scaleString = "" + ((int) (control.scale*100 + 0.5f))/100f;
		int scaleVal = (int) (Math.log(control.scale)/Math.log(1.0717734) + 20);

		JPanel panelScale = new JPanel();
		panelScale.setPreferredSize(new Dimension(control.windowWidthSliderRegion,(control.windowHeight-20)/2));
		scaleSlider = new JSlider(JSlider.VERTICAL, 0, 90, scaleVal); 
		scaleSlider.setPreferredSize(new Dimension(20, (control.windowHeight-90)/2));
		scaleSlider.addChangeListener( this );
		scaleSlider.addMouseListener(this);

		scaleLabel1 = new JLabel(scaleString);
		scaleLabel1.setHorizontalAlignment(SwingConstants.CENTER);
		scaleLabel1.setPreferredSize(new Dimension(control.windowWidthSliderRegion, 15));
		scaleLabel2 = new JLabel("Scale");
		scaleLabel2.setHorizontalAlignment(SwingConstants.CENTER);
		scaleLabel2.setPreferredSize(new Dimension(control.windowWidthSliderRegion, 15));


		panelScale.add(scaleSlider); 
		panelScale.add(scaleLabel1);
		panelScale.add(scaleLabel2);		
		sliderPanel.add(panelScale); 

		
		// lower button panel (south) ===========================================
		lowerButtonPanel = new JPanel();
		lowerButtonPanel.setPreferredSize(new Dimension(900, 40));

		JPanel panelCheck = new JPanel();
		//panelCheck.setLayout(new GridLayout(1,3));

		checkAxes = new JCheckBox("Show:  Axes");
		checkAxes.setSelected(control.showAxes);
		checkAxes.setHorizontalAlignment(JCheckBox.CENTER);
		checkAxes.setHorizontalTextPosition(SwingConstants.LEADING);
		checkAxes.addItemListener (this);
		panelCheck.add(checkAxes);

		checkClipLines = new JCheckBox("Clipping");
		checkClipLines.setSelected(control.showClipLines);
		checkClipLines.setHorizontalAlignment(JCheckBox.CENTER);
		checkClipLines.setHorizontalTextPosition(SwingConstants.LEADING);
		checkClipLines.addItemListener (this);
		panelCheck.add(checkClipLines);

		checkSlices = new JCheckBox("Slice positions");
		checkSlices.setSelected(control.showSlices);
		checkSlices.setHorizontalAlignment(JCheckBox.CENTER);
		checkSlices.setHorizontalTextPosition(SwingConstants.LEADING);
		checkSlices.addItemListener (this);
		panelCheck.add(checkSlices);
		
		JPanel panelSpinners = new JPanel();
		JLabel labelX = new JLabel("Rotation: x:");
		panelSpinners.add(labelX);
		spinnerX = makeSpinner(Math.round(control.degreeX));
		panelSpinners.add(spinnerX);

		JLabel labelY = new JLabel(" y:");
		panelSpinners.add(labelY);
		spinnerY = makeSpinner(Math.round(control.degreeY));
		panelSpinners.add(spinnerY);

		JLabel labelZ = new JLabel(" z:");
		panelSpinners.add(labelZ);
		spinnerZ = makeSpinner(Math.round(control.degreeZ));
		panelSpinners.add(spinnerZ);
		
		JPanel panelOrientationButtons = new JPanel();
		panelOrientationButtons.setLayout(new GridLayout(1,3));

		JButton buttonXY = new JButton("xy");
		buttonXY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableSpinnerChangeListener = false;
				control.degreeX = 0; control.degreeY = 0; control.degreeZ = 0;
				vv.setRotation(control.degreeX, control.degreeY, control.degreeZ);
				newDisplayMode();
				enableSpinnerChangeListener = true;
			}
		});
		panelOrientationButtons.add(buttonXY); 

		JButton buttonYZ = new JButton("yz");
		buttonYZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableSpinnerChangeListener = false;
				control.degreeX = 0; control.degreeY = 90; control.degreeZ = -90;
				vv.setRotation(control.degreeX, control.degreeY, control.degreeZ);
				newDisplayMode();
				enableSpinnerChangeListener = true;
			}
		});
		panelOrientationButtons.add(buttonYZ); 

		JButton buttonXZ = new JButton("xz");
		buttonXZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableSpinnerChangeListener = false;
				control.degreeX = 90; control.degreeY = 0; control.degreeZ = 0;
				vv.setRotation(control.degreeX, control.degreeY, control.degreeZ);
				newDisplayMode();
				enableSpinnerChangeListener = true;
			}
		});
		panelOrientationButtons.add(buttonXZ); 

		lowerButtonPanel.add(panelSpinners);
		JLabel spaceLabel2 = new JLabel("   ");
		lowerButtonPanel.add(spaceLabel2);
		lowerButtonPanel.add(panelOrientationButtons);
		JLabel spaceLabel1 = new JLabel("   ");
		lowerButtonPanel.add(spaceLabel1);
		lowerButtonPanel.add(panelCheck);



		
		// transferfunction panel ===========================================
		transferFunctionPanel = new JPanel();
		
		transferFunctionPanel.setPreferredSize(new Dimension(280,650));
		JLabel tfLabel = new JLabel(" Transfer Function (TF): Color & Alpha");
		transferFunctionPanel.add(tfLabel);
		
		lutChoice = new JComboBox(Control.lutName);
		lutChoice.setSelectedIndex(control.lutNr);
		lutChoice.setPreferredSize(new Dimension(240, 30));
		lutChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
		lutChoice.addActionListener(this);
		transferFunctionPanel.add(lutChoice);

		JLabel rgbLabel = new JLabel("Draw LUT");
		transferFunctionPanel.add(rgbLabel);

		JRadioButton rgbButton = new JRadioButton("RGB");
		rgbButton.setHorizontalTextPosition(SwingConstants.LEFT);
		rgbButton.setActionCommand("RGB");
		rgbButton.setSelected(true);
		JRadioButton rButton = new JRadioButton("R");
		rButton.setHorizontalTextPosition(SwingConstants.LEFT);
		rButton.setActionCommand("R");
		JRadioButton gButton = new JRadioButton("G");
		gButton.setActionCommand("G");
		gButton.setHorizontalTextPosition(SwingConstants.LEFT);
		JRadioButton bButton = new JRadioButton("B");
		bButton.setActionCommand("B");
		bButton.setHorizontalTextPosition(SwingConstants.LEFT);

		ButtonGroup group = new ButtonGroup();
		group.add(rgbButton);
		group.add(rButton);
		group.add(gButton);
		group.add(bButton);

		//Register a listener for the radio buttons.
		rButton.addActionListener(this);
		gButton.addActionListener(this);
		bButton.addActionListener(this);
		rgbButton.addActionListener(this);

		transferFunctionPanel.add(rgbButton);
		transferFunctionPanel.add(rButton);
		transferFunctionPanel.add(gButton);
		transferFunctionPanel.add(bButton);

		transferFunctionPanel.add(vv.tf_rgb);
		vv.tf_rgb.channel = 3;
		transferFunctionPanel.add(vv.gradientLUT);


		// transfer function panel 1D
		JPanel tfPanel1D = new JPanel();
		tfPanel1D.setPreferredSize(new Dimension(256, 240));

		JLabel alphaLabel = new JLabel("Draw the alpha graph of the 1D-TF(lum)");
		alphaLabel.setFont(new Font("Sans", Font.PLAIN, 12));
		tfPanel1D.add(alphaLabel);
		JLabel alphaLabel1 = new JLabel("                                      ");
		tfPanel1D.add(alphaLabel1);
		tfPanel1D.add(vv.tf_a1);
		

		alphaSlider1 = new JSlider(0, 300, 150);
		alphaSlider1.setBorder( new TitledBorder(empty, "global alpha offset", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));

		alphaSlider1.addChangeListener( this );
		alphaSlider1.addMouseListener(this);
		alphaSlider1.setPreferredSize(new Dimension(140, 30));
		tfPanel1D.add(alphaSlider1);
		autobutton1 = new JButton("auto");
		autobutton1.setMargin(new java.awt.Insets(1, 1, 1, 1));
		autobutton1.setFont(new Font("Sans", Font.PLAIN, 12));
		autobutton1.setPreferredSize(new Dimension(50,20));
		autobutton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider1.setValue(150);
				vv.tf_a1.setAlphaOffset(0);
				vv.tf_a1.setAlphaAuto();
				vv.tf_a1.repaint();
				newDisplayMode();
			}
		});
		tfPanel1D.add(autobutton1);
		clearbutton1 = new JButton("clear");
		clearbutton1.setMargin(new java.awt.Insets(1, 1, 1, 1));
		clearbutton1.setFont(new Font("Sans", Font.PLAIN, 12));
		clearbutton1.setPreferredSize(new Dimension(50,20));
		clearbutton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider1.setValue(150);
				vv.tf_a1.setAlphaOffset(0);
				vv.tf_a1.clearAlpha();
				vv.tf_a1.repaint();
				newDisplayMode();
			}
		});
		tfPanel1D.add(clearbutton1);

		// transfer function panel 2D Luminance / Gradient
		JPanel tfPanel2D = new JPanel();
		tfPanel2D.setPreferredSize(new Dimension(256, 240));

		alphaLabel = new JLabel("Pick alpha (& color) and draw the 2D-");
		alphaLabel.setFont(new Font("Sans", Font.PLAIN, 12));
		tfPanel2D.add(alphaLabel);
		alphaLabel1 = new JLabel("TF(lum,grad). Draw with Alt key to erase.");
		alphaLabel1.setFont(new Font("Sans", Font.PLAIN, 12));
		tfPanel2D.add(alphaLabel1);

		vv.gradient2 = new Gradient(control, vv, 150, 18);
		tfPanel2D.add(vv.gradient2);

		checkPickColor2 = new JCheckBox("pick color");
		checkPickColor2.setSelected(control.pickColor);
		checkPickColor2.setHorizontalAlignment(JCheckBox.CENTER);
		checkPickColor2.addItemListener (this);
		boolean enableCheckColor =  !(control.lutNr == Control.ORIG || control.lutNr == Control.GRAY);
		checkPickColor2.setEnabled(enableCheckColor);
		tfPanel2D.add(checkPickColor2);

		tfPanel2D.add(vv.tf_a2);
		alphaSlider2 = new JSlider(0, 300, 150);
		alphaSlider2.setBorder( new TitledBorder(empty, "global alpha offset", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));
		alphaSlider2.addChangeListener( this );
		alphaSlider2.addMouseListener(this);
		alphaSlider2.setPreferredSize(new Dimension(140, 30));
		tfPanel2D.add(alphaSlider2);
		autobutton2 = new JButton("auto");
		autobutton2.setMargin(new java.awt.Insets(1, 1, 1, 1));
		autobutton2.setFont(new Font("Sans", Font.PLAIN, 12));
		autobutton2.setPreferredSize(new Dimension(50,20));
		autobutton2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider2.setValue(150);
				control.alphaPaint2 = 64;
				vv.gradient2.repaint();
				vv.tf_a2.setAlphaOffset(0);
				control.pickColor = false;
				if (checkPickColor2 != null) checkPickColor2.setSelected(control.pickColor);
				vv.lookupTable.setLut();
				vv.tf_a2.setAlphaAuto();
				vv.tf_a2.repaint();
				newDisplayMode();
			}
		});
		tfPanel2D.add(autobutton2);
		clearbutton2 = new JButton("clear");
		clearbutton2.setMargin(new java.awt.Insets(1, 1, 1, 1));
		clearbutton2.setFont(new Font("Sans", Font.PLAIN, 12));
		clearbutton2.setPreferredSize(new Dimension(50,20));
		clearbutton2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider2.setValue(150);
				vv.tf_a2.setAlphaOffset(0);
				vv.tf_a2.clearAlpha();
				vv.tf_a2.repaint();
				newDisplayMode();
			}
		});
		tfPanel2D.add(clearbutton2);

		// transfer function panel 2D LH 
		JPanel tfPanel2DLH = new JPanel();
		tfPanel2DLH.setPreferredSize(new Dimension(256, 240));

		alphaLabel = new JLabel("Pick alpha (& color) and draw the 2D-");
		alphaLabel.setFont(new Font("Sans", Font.PLAIN, 12));
		tfPanel2DLH.add(alphaLabel);
		alphaLabel1 = new JLabel("TF(mean,diff). Draw with Alt key to erase.");
		alphaLabel1.setFont(new Font("Sans", Font.PLAIN, 12));
		tfPanel2DLH.add(alphaLabel1);

		vv.gradient3 = new Gradient(control, vv, 150, 18);
		tfPanel2DLH.add(vv.gradient3);

		checkPickColor3 = new JCheckBox("pick color");
		checkPickColor3.setSelected(control.pickColor);
		checkPickColor3.setHorizontalAlignment(JCheckBox.CENTER);
		checkPickColor3.addItemListener (this);
		checkPickColor3.setEnabled(enableCheckColor);
		tfPanel2DLH.add(checkPickColor3);

		tfPanel2DLH.add(vv.tf_a3);
		alphaSlider3 = new JSlider(0, 300, 150);
		alphaSlider3.setBorder( new TitledBorder(empty, "global alpha offset", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));
		alphaSlider3.addChangeListener( this );
		alphaSlider3.addMouseListener(this);
		alphaSlider3.setPreferredSize(new Dimension(140, 30));
		tfPanel2DLH.add(alphaSlider3);
		autobutton3 = new JButton("auto");
		autobutton3.setMargin(new java.awt.Insets(1, 1, 1, 1));
		autobutton3.setFont(new Font("Sans", Font.PLAIN, 12));
		autobutton3.setPreferredSize(new Dimension(50,20));
		autobutton3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider3.setValue(150);
				control.alphaPaint3 = 64;
				vv.gradient3.repaint();
				vv.tf_a3.setAlphaOffset(0);
				control.pickColor = false;
				if (checkPickColor2 != null) checkPickColor2.setSelected(control.pickColor);
				vv.lookupTable.setLut();
				vv.tf_a3.setAlphaAuto();
				vv.tf_a3.repaint();
				newDisplayMode();
			}
		});
		tfPanel2DLH.add(autobutton3);
		clearbutton3 = new JButton("clear");
		clearbutton3.setMargin(new java.awt.Insets(1, 1, 1, 1));
		clearbutton3.setFont(new Font("Sans", Font.PLAIN, 12));
		clearbutton3.setPreferredSize(new Dimension(50,20));
		clearbutton3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider3.setValue(150);
				vv.tf_a3.setAlphaOffset(0);
				vv.tf_a3.clearAlpha();
				vv.tf_a3.repaint();
				newDisplayMode();
			}
		});
		tfPanel2DLH.add(clearbutton3);


		// transfer function panel ALPHA4
		JPanel tfPanelPaint = new JPanel();
		tfPanelPaint.setPreferredSize(new Dimension(256, 240));

		JPanel textLabels = new JPanel();
		textLabels.setLayout(new GridLayout(6,1));

		JLabel alphaLabel4a = new JLabel("Pick alpha (& color) values and click", JLabel.LEFT);
		alphaLabel4a.setFont(new Font("Sans", Font.PLAIN, 12));
		textLabels.add(alphaLabel4a);
		JLabel alphaLabel4b = new JLabel("the slice images on the left to assign", JLabel.LEFT);
		alphaLabel4b.setFont(new Font("Sans", Font.PLAIN, 12));
		textLabels.add(alphaLabel4b);
		JLabel alphaLabel4c = new JLabel("these values to connected similar", JLabel.LEFT);
		alphaLabel4c.setFont(new Font("Sans", Font.PLAIN, 12));
		textLabels.add(alphaLabel4c);
		JLabel alphaLabel4d = new JLabel("regions of the volume = 3D-TF(x,y,z)", JLabel.LEFT);
		alphaLabel4d.setFont(new Font("Sans", Font.PLAIN, 12));
		textLabels.add(alphaLabel4d);
		JLabel alphaLabel4e = new JLabel("Alt-click to erase.", JLabel.LEFT);
		alphaLabel4e.setFont(new Font("Sans", Font.PLAIN, 12));
		textLabels.add(alphaLabel4e);
		JLabel alphaLabel4f = new JLabel("                                       ");
		textLabels.add(alphaLabel4f);

		tfPanelPaint.add(textLabels);

		vv.gradient4 = new Gradient(control, vv, 150, 18);
		tfPanelPaint.add(vv.gradient4);
		JLabel alphaLabel4g = new JLabel("                                       ");
		tfPanelPaint.add(alphaLabel4g);

		JPanel sliderBox2 = new JPanel();
		sliderBox2.setLayout(new GridLayout(1,2));

		sliderLumTolerance = new JSlider(0, 128, control.lumTolerance);
		sliderLumTolerance.setBorder( new TitledBorder(empty, "luminance tolerance", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));
		sliderLumTolerance.addChangeListener( this );
		sliderLumTolerance.setPreferredSize(new Dimension(135, 30));
		sliderBox2.add(sliderLumTolerance);

		sliderGradTolerance = new JSlider(0, 128, control.gradTolerance);
		sliderGradTolerance.setBorder( new TitledBorder(empty, "gradient tolerance", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));
		sliderGradTolerance.addChangeListener( this );
		sliderGradTolerance.setPreferredSize(new Dimension(135, 30));
		sliderBox2.add(sliderGradTolerance);			

		tfPanelPaint.add(sliderBox2);

		alphaSlider4 = new JSlider(0, 300, 150);
		alphaSlider4.setBorder( new TitledBorder(empty, "global alpha offset", TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM, new Font("Sans", Font.PLAIN, 10)));
		alphaSlider4.addChangeListener( this );
		alphaSlider4.addMouseListener(this);
		alphaSlider4.setPreferredSize(new Dimension(140, 30));
		tfPanelPaint.add(alphaSlider4);

		clearbutton4 = new JButton("clear");
		clearbutton4.setMargin(new java.awt.Insets(1, 1, 1, 1));
		clearbutton4.setFont(new Font("Sans", Font.PLAIN, 12));
		clearbutton4.setPreferredSize(new Dimension(50,20));
		clearbutton4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				alphaSlider4.setValue(150);
				vv.tf_a4.setAlphaOffset(0);
				vv.tf_a4.clearAlpha();
				newDisplayMode();
			}
		});
		tfPanelPaint.add(clearbutton4);

		transferFunctionTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		JPanel jp1 = new JPanel();
		JPanel jp2 = new JPanel();
		JPanel jp3 = new JPanel();
		JPanel jp4 = new JPanel();

		transferFunctionTabbedPane.addTab("1D", jp1);
		transferFunctionTabbedPane.addTab("2D Grad", jp2);
		transferFunctionTabbedPane.addTab("2D MD", jp3);
		transferFunctionTabbedPane.addTab("3D Fill", jp4);
		transferFunctionTabbedPane.setSelectedIndex(control.alphaMode);

		transferFunctionTabbedPane.addChangeListener(new ChangeListener() {
			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane)evt.getSource();
				control.alphaMode = pane.getSelectedIndex();		// Get current tab id
				if (checkPickColor2 != null) checkPickColor2.setSelected(control.pickColor);
				if (checkPickColor3 != null) checkPickColor3.setSelected(control.pickColor);
				if (control.LOG) System.out.println("alphaMode " + control.alphaMode );
				control.drag = false;
				control.alphaWasChanged = true;
				newDisplayMode();
			}
		});

		jp1.add(tfPanel1D);
		jp2.add(tfPanel2D);
		jp3.add(tfPanel2DLH);
		jp4.add(tfPanelPaint);
		transferFunctionPanel.add(transferFunctionTabbedPane);
		
		
		// TODO  light
		lightBox = new JPanel();
		lightBox.setLayout(new BorderLayout());
		JPanel lightSliderBoxR = new JPanel();
		JPanel lightSliderBoxL = new JPanel();
		lightSliderBoxR.setPreferredSize(new Dimension(102, 110));
		lightSliderBoxL.setPreferredSize(new Dimension(58, 110));
		lightSliderBoxR.setLayout(new GridLayout(5,1));
		lightSliderBoxL.setLayout(new GridLayout(5,1));
		
		JLabel jl1 = new JLabel("object color", SwingConstants.RIGHT);
		jl1.setFont(new Font("Sans", Font.PLAIN, 10));
		lightSliderBoxL.add(jl1);
		objectLightSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (50*control.objectLightValue)); 
		objectLightSlider.addChangeListener( this );
		objectLightSlider.addMouseListener(this);
		objectLightSlider.setEnabled(control.useLight);
		lightSliderBoxR.add(objectLightSlider);
		
		JLabel jl2 = new JLabel("ambient", SwingConstants.RIGHT);
		jl2.setFont(new Font("Sans", Font.PLAIN, 10));
		lightSliderBoxL.add(jl2);
		ambientSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (control.ambientValue*100)); 
		ambientSlider.addChangeListener( this );
		ambientSlider.addMouseListener(this);
		ambientSlider.setEnabled(control.useLight);
		lightSliderBoxR.add(ambientSlider);	
		
		JLabel jl3 = new JLabel("diffuse", SwingConstants.RIGHT);
		jl3.setFont(new Font("Sans", Font.PLAIN, 10));
		lightSliderBoxL.add(jl3);
		diffuseSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (control.diffuseValue*100)); 
		diffuseSlider.addChangeListener( this );
		diffuseSlider.addMouseListener(this);
		diffuseSlider.setEnabled(control.useLight);
		lightSliderBoxR.add(diffuseSlider);	
		
		JLabel jl4 = new JLabel("specular", SwingConstants.RIGHT);
		jl4.setFont(new Font("Sans", Font.PLAIN, 10));
		lightSliderBoxL.add(jl4);
		specularSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) (control.specularValue*100)); 
		specularSlider.addChangeListener( this );
		specularSlider.addMouseListener(this);
		specularSlider.setEnabled(control.useLight);
		lightSliderBoxR.add(specularSlider);	
		
		JLabel jl5 = new JLabel("shine", SwingConstants.RIGHT);
		jl5.setFont(new Font("Sans", Font.PLAIN, 10));
		lightSliderBoxL.add(jl5);
		int val = (int) (200*Math.pow(control.shineValue/400, 1/3f)-20);
		val = Math.min(100, Math.max(0, val));
		shineSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, val); 
		shineSlider.addChangeListener( this );
		shineSlider.addMouseListener(this);
		shineSlider.setEnabled(control.useLight);
		lightSliderBoxR.add(shineSlider);	
		
		
		checkLight = new JCheckBox("Light");
		checkLight.setSelected(control.useLight);
		//checkLight.setHorizontalAlignment(JCheckBox.RIGHT);
		checkLight.addItemListener (this);
		
		picLight = new Pic(control, vv, 55, 55);
		imageLightRegion = new ImageRegion(control);	
		imageLightRegion.setPlaneColor(UIManager.getColor ( "Panel.background" ));
		imageLightRegion.addMouseMotionListener(this);
		imageLightRegion.addMouseListener(this);
		imageLightRegion.setPic(picLight);
		JPanel lightRegion = new JPanel();
		lightRegion.add(imageLightRegion);
		picLight.render_sphere();
		imageLightRegion.setImage(picLight.image);
		imageLightRegion.repaint();
		
		JPanel lightBoxLeft = new JPanel();
		lightBoxLeft.setPreferredSize(new Dimension(120, 115));
		lightBoxLeft.setLayout(new BorderLayout());
		lightBoxLeft.add(checkLight, BorderLayout.NORTH);
		lightBoxLeft.add(lightRegion, BorderLayout.CENTER);
		
		jLabelLight = new JLabel("<html>Drag sphere <P>to change<P> direction, <P> double click<P> to change <P>color of light.</html>");
		lightBoxLeft.add(jLabelLight, BorderLayout.WEST);
		//jLabelLight.setVisible(control.light == 1);
		jLabelLight.setFont(new Font("SANS", Font.PLAIN, 10));
		if (control.useLight) 
			jLabelLight.setForeground(Color.BLACK);
		else
			jLabelLight.setForeground(UIManager.getColor ("Panel.background"));
		
		
		JPanel lightBoxBottom = new JPanel();
		lightBoxBottom.setLayout(new GridLayout(1,2));

		
		lightBox.add(lightBoxLeft, BorderLayout.WEST);
		lightBox.add(lightSliderBoxL, BorderLayout.CENTER);
		lightBox.add(lightSliderBoxR, BorderLayout.EAST);
		transferFunctionPanel.add(lightBox);
		
		if (control.renderMode >= Control.PROJECTION_MAX) {
			transferFunctionTabbedPane.setVisible(true);
			samplingLabel.setVisible(true);
			tfSampling.setVisible(true);
		}
		else {
			transferFunctionTabbedPane.setVisible(false);
			samplingLabel.setVisible(false);
			tfSampling.setVisible(false);
		}
		
		if (control.renderMode == Control.VOLUME) 
			lightBox.setVisible(true);
		else 
			lightBox.setVisible(false);
		

		// put all together
		setLayout(new BorderLayout());
		add(centerPanel, BorderLayout.WEST);
		add(sliderPanel,BorderLayout.CENTER);
		if (control.showTF)
			add(transferFunctionPanel, BorderLayout.EAST);
		add(upperButtonPanel, BorderLayout.NORTH);
		add(lowerButtonPanel, BorderLayout.SOUTH);
		validate();
		control.scaledDist = control.scale*control.dist;
	}

	public void setPositionText(int wx, int wy, int wz) {
		int xs = 10, ys = 14;  /// !!
		sliceImageRegion.setText("xy slice  z=" + positionZ, 0, xs, wy+ys-3, 0, Color.black, 1);
		sliceImageRegion.setText("yz slice  x=" + positionX, 1, xs, wy+wz+2*ys-3, 0, Color.black, 1);
		sliceImageRegion.setText("xz slice  y=" + positionY, 2, xs, wy+2*wz+3*ys-3, 0, Color.black, 1);		
	}
	
	private JSpinner makeSpinner(float value) {
		JSpinner jSpinner;
		SpinnerNumberModel m_numberSpinnerModel;
		Float current = new Float(value);
		Float min = new Float(-360);
		Float max = new Float(360);
		Float step = new Float(1);
		m_numberSpinnerModel = new SpinnerNumberModel(current, min, max, step);
		jSpinner = new JSpinner(m_numberSpinnerModel);
		jSpinner.addChangeListener(new SpinnerListener());
		return jSpinner;
	}

	// adapt the dist slider to the size of the volume
	void updateDistSlider() {
		control.maxDist = (int)(Math.sqrt(vv.vol.zOffa*vv.vol.zOffa*control.zAspect*control.zAspect + vv.vol.yOffa*vv.vol.yOffa +vv.vol.xOffa*vv.vol.xOffa));
		distSlider.setMaximum(control.maxDist*2);
		distSlider.setMinimum(-control.maxDist*2);			
	}

	public void stateChanged( ChangeEvent e ){
		JSlider slider = (JSlider)e.getSource();

		if (slider == scaleSlider) {
			scaleSliderValue = scaleSlider.getValue();
			scaleSliderValue -= 20;
			float s = (float) Math.pow(1.0717734,scaleSliderValue);
			if (s == control.scale) return;
			control.scale = s;
			String scaleString = "" + ((int) (control.scale*100 + 0.5f))/100f;
			scaleLabel1.setText(scaleString);
			control.scaledDist = control.dist*control.scale;
			vv.setScale(); 
			vv.setZAspect();
			vv.initializeTransformation();
		}
		else if (slider == distSlider) {
			float d = (distSlider.getValue()/2f); 
			if (d == control.dist)
				return;
			control.dist = d; 
			control.scaledDist = control.dist*control.scale;
			distLabel1.setText(("" + control.dist));
		}
		else if (slider == positionXSlider) {
			control.positionFactorX = positionXSlider.getValue()/(float)maxPositionX;
			float xV = (vv.vol.widthV-1)*control.positionFactorX;
			positionX = (int) xV;
			if (slider.getValueIsAdjusting()) 
				showPositionAndValues();	
			vv.cube.setCornersYZ(xV);
			picSlice.drawSlices();
			sliceImageRegion.findLines(vv.cube, control.scaledDist, vv.vol.depthV); 
			sliceImageRegion.setImage(picSlice.image);
			sliceImageRegion.repaint();
		}
		else if (slider == positionYSlider) {
			control.positionFactorY = positionYSlider.getValue()/(float)maxPositionY;
			float yV = (vv.vol.heightV-1)*control.positionFactorY;
			positionY = (int) yV;
			if (slider.getValueIsAdjusting()) 
				showPositionAndValues();	
			vv.cube.setCornersXZ(yV);
			picSlice.drawSlices();
			sliceImageRegion.findLines(vv.cube, control.scaledDist, vv.vol.depthV); 
			sliceImageRegion.setImage(picSlice.image);
			sliceImageRegion.repaint();
		}
		else if (slider == positionZSlider) {
			control.positionFactorZ = positionZSlider.getValue()/(float)maxPositionZ;
			float zV = (vv.vol.depthV-1)*control.positionFactorZ;
			positionZ = (int) zV;
			if (slider.getValueIsAdjusting()) 
				showPositionAndValues();
			vv.cube.setCornersXY(zV);
			picSlice.drawSlices();
			sliceImageRegion.findLines(vv.cube, control.scaledDist, vv.vol.depthV); 
			sliceImageRegion.setImage(picSlice.image);
			sliceImageRegion.repaint();
		}
		else if (slider == alphaSlider1) {
			vv.tf_a1.setAlphaOffset(alphaSlider1.getValue() - 150);
			vv.tf_a1.scaleAlpha();
			vv.tf_a1.repaint();
		}
		else if (slider == alphaSlider2) {
			vv.tf_a2.setAlphaOffset(alphaSlider2.getValue() - 150);
			vv.tf_a2.scaleAlpha();
			vv.tf_a2.repaint();
		}
		else if (slider == alphaSlider3) {
			vv.tf_a3.setAlphaOffset(alphaSlider3.getValue() - 150);
			vv.tf_a3.scaleAlpha();
			vv.tf_a3.repaint();
		}
		else if (slider == alphaSlider4) {
			vv.tf_a4.setAlphaOffset(alphaSlider4.getValue() - 150);
			vv.tf_a4.scaleAlpha();
		}
		else if (slider == sliderLumTolerance) {
			control.lumTolerance = sliderLumTolerance.getValue();
		}
		else if (slider == sliderGradTolerance) {
			control.gradTolerance = sliderGradTolerance.getValue();
		}
		else if (slider == objectLightSlider) {
			control.objectLightValue = objectLightSlider.getValue()/50f;
		}
		else if (slider == ambientSlider) {
			control.ambientValue = ambientSlider.getValue()/100f;
		}
		else if (slider == diffuseSlider) {
			control.diffuseValue = diffuseSlider.getValue()/100f;
		}
		else if (slider == specularSlider) {
			control.specularValue = specularSlider.getValue()/100f;
		}
		else if (slider == shineSlider) {
			float k = 200f;
			control.shineValue = (float) (Math.pow((shineSlider.getValue()+20)/k,3)*2*k);	
		}
		if ( slider == objectLightSlider || slider == ambientSlider || slider == diffuseSlider || slider == specularSlider || slider == shineSlider ) {
			picLight.render_sphere();
			imageLightRegion.setImage(picLight.image);
			imageLightRegion.repaint();
		}
		
		if ( ( (slider == positionXSlider || slider == positionYSlider || slider == positionZSlider) && control.showSlices) ||
				slider == scaleSlider || slider == alphaSlider1 || slider == alphaSlider2 || slider == alphaSlider3 || slider == alphaSlider4 || 
				slider == objectLightSlider ||  slider == ambientSlider || slider == diffuseSlider || slider == specularSlider || slider == shineSlider ||slider == distSlider) {
			newDisplayMode();	
		}
	}

	private void showPositionAndValues() {
		positionString = String.format("  x=%3d, y=%3d, z=%3d", positionX, positionY, positionZ);
		if (control.isRGB) {
			int r = vv.vol.data3D[1][positionZ+2][positionY+2][positionX+2] & 0xFF;
			int g = vv.vol.data3D[2][positionZ+2][positionY+2][positionX+2] & 0xFF;
			int b = vv.vol.data3D[3][positionZ+2][positionY+2][positionX+2] & 0xFF;
			valueString = String.format("  R=%3d, G=%3d, B=%3d", r, g, b);
		}
		else {
			int val = vv.vol.data3D[0][positionZ+2][positionY+2][positionX+2] & 0xFF;	
			valueString = String.format("  Value=%3d", val);
		}
		valueLabel.setText(valueString);
		positionLabel.setText(positionString);
	}

	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("R"))
			vv.tf_rgb.channel = 0;
		else if (actionCommand.equals("G"))
			vv.tf_rgb.channel = 1;
		else if (actionCommand.equals("B"))
			vv.tf_rgb.channel = 2;
		else if (actionCommand.equals("RGB"))
			vv.tf_rgb.channel = 3;

		else {
			JComboBox cb = (JComboBox)e.getSource();
			if (cb == lutChoice) {
				control.lutNr = cb.getSelectedIndex();
				switch (control.lutNr) {
				case Control.ORIG:	 
					vv.lookupTable.orig();
					break;
				case Control.GRAY:	 
					vv.lookupTable.gray();
					break;
				case Control.SPECTRUM:  
					vv.lookupTable.spectrum();
					break;
				case Control.FIRE:  
					vv.lookupTable.fire();
					break;
				case Control.THERMAL:  
					vv.lookupTable.thermal();
					break;
				}
				vv.lookupTable.setLut();
				
				if (checkPickColor2 != null) checkPickColor2.setSelected(control.pickColor);
				if (checkPickColor3 != null) checkPickColor3.setSelected(control.pickColor);
				if (control.lutNr == Control.ORIG || control.lutNr == Control.GRAY) {
					if (checkPickColor2 != null) checkPickColor2.setEnabled(false);
					if (checkPickColor3 != null) checkPickColor3.setEnabled(false);	
				}
				else {
					if (checkPickColor2 != null) checkPickColor2.setEnabled(true);
					if (checkPickColor3 != null) checkPickColor3.setEnabled(true);	
				}

				control.rPaint = vv.lookupTable.lut[control.indexPaint][0];
				control.gPaint = vv.lookupTable.lut[control.indexPaint][1];
				control.bPaint = vv.lookupTable.lut[control.indexPaint][2];
				// TODO ???
				if (control.renderMode >= Control.PROJECTION_MAX && control.alphaMode == Control.ALPHA4)
					vv.gradient4.repaint();
			}

			else if (cb == renderChoice) {
				control.renderMode =  cb.getSelectedIndex();
				positionString = valueString = "";
				valueLabel.setText(valueString);
				positionLabel.setText(positionString);

				if (control.renderMode >= Control.PROJECTION_MAX) {
					transferFunctionTabbedPane.setVisible(true);
					samplingLabel.setVisible(true);
					tfSampling.setVisible(true);
					control.dist = -2*control.maxDist;
				}
				else { 
					transferFunctionTabbedPane.setVisible(false);
					samplingLabel.setVisible(false);
					tfSampling.setVisible(false);
					control.dist = 0;
				}
				
				if (control.renderMode == Control.VOLUME) 
					lightBox.setVisible(true);
				else 
					lightBox.setVisible(false);
				distSlider.setValue((int)control.dist);
				distLabel1.setText(("" + control.dist));
				control.scaledDist = control.dist*control.scale;
			}
			else if (cb == interpolationChoice) {
				control.interpolationMode = cb.getSelectedIndex();
			}
		}

		vv.cube.initTextsAndDrawColors(imageRegion);
		newDisplayMode();

		super.requestFocus();
	}

	public synchronized void itemStateChanged(ItemEvent e) {

		if (e.getSource() == checkAxes) 
			control.showAxes = checkAxes.isSelected ();  
		else if (e.getSource() == checkSlices) 
			control.showSlices = checkSlices.isSelected(); 
		else if (e.getSource() == checkClipLines)
			control.showClipLines = checkClipLines.isSelected ();
		else if (e.getSource() == checkPickColor2) {
			if (checkPickColor2.isSelected () ) {
				control.pickColor = true;
				control.rPaint = vv.lookupTable.lut[128][0];
				control.gPaint = vv.lookupTable.lut[128][1];
				control.bPaint = vv.lookupTable.lut[128][2];
			}
			else
				control.pickColor = false;
			vv.gradient2.repaint();
		}
		else if (e.getSource() == checkPickColor3) {
			if (checkPickColor3.isSelected () ) {
				control.pickColor = true;
				control.rPaint = vv.lookupTable.lut[128][0];
				control.gPaint = vv.lookupTable.lut[128][1];
				control.bPaint = vv.lookupTable.lut[128][2];
			}
			else
				control.pickColor = false;
			vv.gradient3.repaint();
		}
		else if (e.getSource() == checkLight) {
			control.useLight = checkLight.isSelected();
			picLight.render_sphere();
			imageLightRegion.setImage(picLight.image);
			imageLightRegion.repaint();
			if (control.useLight)
				jLabelLight.setForeground(Color.BLACK);
			else
				jLabelLight.setForeground(UIManager.getColor ( "Panel.background" ));	
			objectLightSlider.setEnabled(control.useLight);
			shineSlider.setEnabled(control.useLight);
			ambientSlider.setEnabled(control.useLight);
			diffuseSlider.setEnabled(control.useLight);
			specularSlider.setEnabled(control.useLight);
		}
		newDisplayMode();
		super.requestFocus();
	}

	public void mouseClicked(MouseEvent e) {
		Object source = e.getSource();
		if (source == sliceImageRegion) {
			int xm = e.getX();
			int ym = e.getY();

			if (control.alphaMode == Control.ALPHA4) {
				if (control.renderMode >= Control.PROJECTION_MAX) {
					int[] vals = sliceImageRegion.getValues(xm, ym);
					int alpha = control.alphaPaint4;
					if (e.isAltDown())
						alpha = 0;
					int lum = vals[0];
					int z = vals[4];
					int y = vals[5];
					int x = vals[6];
					vv.vol.findAndSetSimilarInVolume(lum, alpha, z, y, x);
					control.drag = false;
					newDisplayMode();
				}
			}
		}
		
		if (source == imageLightRegion) {
			if (e.getClickCount() >= 2) {
				Color lightColor = JColorChooser.showDialog(null, "Choose light color", null);
				if (lightColor != null){
					control.lightRed = lightColor.getRed();
					control.lightGreen = lightColor.getGreen();
					control.lightBlue = lightColor.getBlue();

					newDisplayMode();
					picLight.render_sphere();
					imageLightRegion.setImage(picLight.image);
					imageLightRegion.repaint();
				}
			}
		}
		
	}

	public void mouseEntered(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {
		if (control.renderMode >= Control.PROJECTION_MAX) {
			int[] vals = {-1, -1, -1, -1};
			if (control.alphaMode == Control.ALPHA1) {
				vv.tf_a1.setValues(vals);
				vv.tf_a1.repaint();
			}
			else if (control.alphaMode == Control.ALPHA2) {
				vv.tf_a2.setValues(vals);
				vv.tf_a2.repaint();
			}
			else if (control.alphaMode == Control.ALPHA3) {
				vv.tf_a3.setValues(vals);
				vv.tf_a3.repaint();
			}
		}	
		positionString = valueString = "";
		valueLabel.setText(valueString);
		positionLabel.setText(positionString);
	}

	public void mousePressed(MouseEvent arg0) {
		Object source = arg0.getSource();
		//control.drag = true;
		if (source == imageRegion || source == imageLightRegion) {
			xStart = arg0.getX();
			yStart = arg0.getY();
		}
	}

	public void mouseDragged(MouseEvent arg0) {
		//control.drag = true;
		Object source = arg0.getSource();

		if (source == imageRegion) {
			xAct = arg0.getX();
			yAct = arg0.getY();
			if (arg0.isShiftDown() ) 
				vv.changeTranslation(xAct-xStart, yAct-yStart);
			else 
				vv.changeRotation(xStart, yStart, xAct, yAct, control.windowWidthImageRegion); //pic.width);
			xStart = xAct;
			yStart = yAct;
			newDisplayMode();
		}
		if (source == imageLightRegion) {
			xAct = arg0.getX();
			yAct = arg0.getY();
			vv.changeRotationLight(xStart, yStart, xAct, yAct, 50); //pic.width);
			xStart = xAct;
			yStart = yAct;
			picLight.render_sphere();
			imageLightRegion.setImage(picLight.image);
			imageLightRegion.repaint();
			newDisplayMode();
		}	
	}

	public void mouseMoved(MouseEvent arg0) {
		Object source = arg0.getSource();
		int xS = arg0.getX();
		int yS = arg0.getY();
		if (source == sliceImageRegion) {
			int[] vals = sliceImageRegion.getValues(xS, yS);
			int xV = (int) vals[6];
			int yV = (int) vals[5];
			int zV = (int) vals[4];
			if (xV != -1 && yV != -1 && zV != -1) {
				positionString = String.format("  x=%3d, y=%3d, z=%3d", xV, yV, (zV+1));
				if (control.isRGB) {
					int r = vv.vol.data3D[1][zV+2][yV+2][xV+2] & 0xFF;
					int g = vv.vol.data3D[2][zV+2][yV+2][xV+2] & 0xFF;
					int b = vv.vol.data3D[3][zV+2][yV+2][xV+2] & 0xFF;
					valueString = String.format("  R=%3d, G=%3d, B=%3d", r, g, b);
				}
				else {
					int val = vv.vol.data3D[0][zV+2][yV+2][xV+2] & 0xFF;	
					valueString = String.format("  Value=%3d", val);
				}
			}
			else 
				positionString = valueString = "";
			valueLabel.setText(valueString);
			positionLabel.setText(positionString);

			if (control.renderMode >= Control.PROJECTION_MAX) {
				if (control.alphaMode == Control.ALPHA1) {
					vv.tf_a1.setValues(vals);
					vv.tf_a1.repaint();
				}
				else if (control.alphaMode == Control.ALPHA2) {
					vv.tf_a2.setValues(vals);
					vv.tf_a2.repaint();
				}
				else if (control.alphaMode == Control.ALPHA3) {
					vv.tf_a3.setValues(vals);
					vv.tf_a3.repaint();
				}
			}
		}
		else if (source == imageRegion) {
			if (control.renderMode <= Control.SLICE_AND_BORDERS) {
				float[] xyzV = vv.trScreen2Vol(xS, yS, control.scaledDist);
				float xV = xyzV[0];
				float yV = xyzV[1];
				float zV = xyzV[2];
				if (xV >= 0 && xV < vv.vol.widthV && yV >= 0 && yV < vv.vol.heightV && zV >= 0 && zV < vv.vol.depthV) {
					positionString = String.format("  x=%3d, y=%3d, z=%3d", (int)xV, (int)yV, ((int)zV+1));
					if (control.isRGB) {
						int r = vv.vol.data3D[1][(int)zV+2][(int)yV+2][(int)xV+2] & 0xFF;
						int g = vv.vol.data3D[2][(int)zV+2][(int)yV+2][(int)xV+2] & 0xFF;
						int b = vv.vol.data3D[3][(int)zV+2][(int)yV+2][(int)xV+2] & 0xFF;
						valueString = String.format("  R=%3d, G=%3d, B=%3d", r, g, b);
					}
					else {
						int val = vv.vol.data3D[0][(int)zV+2][(int)yV+2][(int)xV+2] & 0xFF;	
						valueString = String.format("  Value=%3d", val);
					}
				}
				else 					
					positionString = valueString = "";
				valueLabel.setText(valueString);
				positionLabel.setText(positionString);
			}
			else 					
				positionString = valueString = "";
			valueLabel.setText(valueString);
			positionLabel.setText(positionString);
		}
	}
	public void newDisplayMode(){

		if (control.LOG) System.out.println("newDisplayMode");
		signalBusy();

		vv.cube.setTextAndLines(imageRegion);
		
		if (control.renderMode >= Control.PROJECTION_MAX){ 
			pic.startVolumeRendering();
		}  
		else {
			vv.lookupTable.setLut();
			if (control.renderMode == Control.SLICE_AND_BORDERS) 
				pic.render_SliceAndBorders();
			else if (control.renderMode == Control.SLICE)  
				pic.render_Slice();

			imageRegion.setImage(pic.image);
			imageRegion.repaint();
			signalReady();
		} 

		picSlice.updateImage();
		sliceImageRegion.findLines(vv.cube, control.scaledDist, vv.vol.depthV); 
		sliceImageRegion.setImage(picSlice.image);
		sliceImageRegion.repaint();		
	}
	
	void signalReady() {
		if (control.LOG) {
			long end = System.currentTimeMillis();
			System.out.println("Render time " + (end-startR)+" ms.");
		}
		control.isReady = true;
		picLED.render_LED(true);
		imageLEDRegion.setImage(picLED.image);
		imageLEDRegion.paintImmediately(0, 0, imageLEDRegion.getWidth(), imageLEDRegion.getHeight());
	}
	
	long startR=0;

	
	void signalBusy() {
		if (control.LOG) {
			if (control.LOG) startR = System.currentTimeMillis();
		}
		control.isReady = false;
		picLED.render_LED(false);
		imageLEDRegion.setImage(picLED.image);
		imageLEDRegion.paintImmediately(0, 0, imageLEDRegion.getWidth(), imageLEDRegion.getHeight());
	}
	

	public void setSpinners() {
		enableSpinnerChangeListener = false;
		if (imageRegion != null && spinnerX != null && spinnerY != null && spinnerZ != null) {
			spinnerX.setValue((float)(int)Math.round(control.degreeX));
			spinnerY.setValue((float)(int)Math.round(control.degreeY));
			spinnerZ.setValue((float)(int)Math.round(control.degreeZ));
		}
		enableSpinnerChangeListener = true;		
	}
	
	
	class SpinnerListener implements ChangeListener /*, ActionListener */{

		static final int MIN_TIME = 200;	// minimum time in ms between update requests
		
		public void stateChanged(ChangeEvent evt) {
			if (enableSpinnerChangeListener) {
				control.spinnersAreChanging = true;
		
				control.degreeX = (Float) spinnerX.getValue();
				control.degreeY = (Float) spinnerY.getValue();
				control.degreeZ = (Float) spinnerZ.getValue();

				vv.setRotation(control.degreeX, control.degreeY, control.degreeZ);
				control.spinnersAreChanging = false;
				newDisplayMode();       
			}
		}

	}

}
