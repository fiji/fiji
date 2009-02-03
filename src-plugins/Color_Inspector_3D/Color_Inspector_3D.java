/*
 * Color_Inspector_3D
 *
 * (C) Author:  Kai Uwe Barthel: barthel (at) fhtw-berlin.de 
 * 
 * This ImageJ-PlugIn can display the distribution of the colors of  
 * an image in a 3D color space. 
 * The color distribution can be shown in different color spaces. 
 * 
 * Version 2.3      15.9.2007
 * 		PCA / KLT 
 * 		YIQ color space
 * 		color channels may be switschen on and off
 * 		minor corrections
 * 
 * Version 2.1		17.2.2006
 * 		Depth slider controls which colors are displayed
 * 		Segmentation added		
 * 		Modified image can be saved			
 * 
 * Version 2.0		September, 10. 2005
 * 		Program may be used as stand alone program
 * 		Large images may be displayed in the left window 		
 * 		Quantisation is performed within the chosen color space 
 * 		(for "RGB YUV YCbCr Lab and Luv) 
 * 		image manipulations, new color spaces: HSL, HSV
 * 		english / german  
 * 
 * Version 1.71		January 25 2005 
 * 		corrected histogram list
 * 
 * Version 1.7			January 20 2005
 * 		Different color quantization methods (uniform, Median Cut, and Xiaolin Wu's scheme)
 * 		Histogram bin size can be chosen
 * 		SHIFT-click in the left window prints the color percentages for histograms
 * 		Distribution can be saved as image
 *
 * Version 1.631		November 1 2004
 * 		Quantization effect for the calculation of the histogram is shown in the image
 * 		histogram bins colors can be chosen as centers and as means of the colors within each bin
 * 		problem for images with 101 colors fixed
 * 
 * Version 1.6		October, 29 2004
 * 		Automatic rotation of the cube
 *  	Histogram functions included 
 * 		corrected frequency weighted display
 * 		
 * Version 1.5
 * 		Added XYZ, Luv, and xyY color space, Lab corrected 
 * 		Device independent color spaces assume D50 illuminant and sRGB data
 * 		Reduced memory consumption
 * 
 * Version 1.41
 * 		new cursors
 * 		display of color values
 * 
 * Version 1.4           September, 26 2004
 * 		corrected z-depth for text 
 * 		display of axes can be switched
 * 		distribution image can be scaled 
 * 		distribution image can shifted (mouse drag + SHIFT-key)
 * 		better display when taking into account frequency 
 * 
 * Version 1.3           September, 20 2004
 * 		Added perspective view, 
 * 		HMMD color space,
 * 		corrected z-depth for colors and lines 
 * 
 * Version 1.2           June, 23 2004
 * 		Added LAB + YCbCr color space  
 * 
 * Version 1.1           June, 21 2004
 * 		Added support for Masks and minor bug fixes and speedups
 * 
 * Version 1.0           June, 16 2004
 * 		First version
 * 
 */
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



public final class Color_Inspector_3D implements PlugIn {

	private final static String version = "v2.3"; 
	public static final char degreeSymbol = (char)176;

	final static int RGB = 0;
	final static int YUV = 1;
	final static int YCBCR = 2;
	final static int YIQ = 3;
	final static int HSB = 4;
	final static int HSV = 5;
	final static int HSL = 6;
	final static int HMMD = 7;
	final static int LAB = 8;
	final static int LUV = 9;
	final static int XYY = 10;
	final static int XYZ = 11;
	final static int KLT = 12;
	final static int HCL = 13;

	private final static String [] stringColorSpaceNames = { "RGB", "YUV", "YCbCr", "YIQ", "HSB", "HSV", "HSL", "HMMD", "Lab", "Luv", "xyY", "XYZ", "KLT/PCA"} ;
	private final static String [][] stringColorSpaceLetters = { {"R","G","B"}, {"Y","U","V"}, {"Y","Cb","Cr"}, {"Y","I","Q"}, {"H","S","B"}, {"H","S","V"}, 
		{"H","S","L"}, {"Hue", "Diff", "Sum"},{"L","a","b"}, {"L","u","v"}, {"x","y","Y"}, {"X","Y","Z"}, {"C0","C1","C2"} } ;

	private boolean english = true;

	private String stringAllColors;
	private String stringWeighted;
	private String stringHistogram;	
	private String stringMedianCut;	
	private String stringWu;	

	private final String [] stringDisplayMode = new String[5]; 

	private JSlider slider0;
	private JSlider sliderPerspective;
	private JSlider sliderScale; 
	private JSlider sliderSaturation;
	private JSlider sliderBrightness;
	private JSlider sliderContrast;
	private JSlider sliderFactor0;
	private JSlider sliderFactor1;
	private JSlider sliderFactor2;
	private JSlider sliderHueChange;
	private JSlider sliderDepth;

	JLabel label;
	JLabel label0;
	JLabel labelInfo;
	JTextField jTextField;
	int sliderValue0 = 100; 
	int renderDepth = -222; 

	private  final static int OPAQUE = 0xFF000000;
	private  boolean showAxes = true;
	private  boolean showText = true;
	private  boolean colorMode = true;

	private  int mode = 0;
	private  boolean shift = false;
	private  boolean hist = false;
	private  boolean pause = false;
	private  boolean move = false;
	private  boolean rotation = false;
	private  boolean fitImage = true;
	private  boolean displayOrig = true;


	private  float delta = 1; 
	private int qMode = 0;

	private int numberOfColors;
	private int numberOfColorsOrig = 0;

	private  int maskSize; // ROI size

	private  ColHash[] colHash; 
	private  float freqFactor;

	private float saturation = 1;
	private int brightness = 0;
	private float contrast = 1;
	private float hueChange = 0;

	private double[] channelFactor = { 1, 1, 1};

	private Picture pic1;
	private Picture pic2;
	private ImageRegion imageRegion1;
	private ImageRegion imageRegion2;

	private CustomWindow  cw;
	private JFrame frame;

	private int depthColorNear = 0;
	private int depthColorFar = 0;

	private int xPos = 10;
	private int yPos = 10;
	private int colorSpace = RGB;

	private int hashSize;
	
	private ColorKLT kltColor = null;
	private double[][] kltMatrix = null;
	private double[] kltMean = null;
	

	public void init() {
//		String str = "/images/titel.jpg";
//		URL url = getClass().getResource(str);

		Color_Inspector_3D ci3D = new Color_Inspector_3D();
		ci3D.process("images/titel.jpg");
	}

	public static void main(String args[]) {

		//new ImageJ(); // open the ImageJ window to see images and results

		Color_Inspector_3D ci3D = new Color_Inspector_3D();
		if (args.length<1)
			ci3D.process("images/titel.jpg");
		else 
			ci3D.process(args[0]);
	}

	void process(String path) {
		if (IJ.versionLessThan("1.33b")) return;
		IJ.run("Open...", "path='"+path+"'");
		//IJ.run("Open...", "path=/Users/barthel/Applications/ImageJ/plugins/ColorInspector3D/images/cube6.png");

		run("");
	}

	public void run(String arg) {

		stringAllColors = (english) ? "All Colors" : "alle Farben ungewichtet";
		stringWeighted = (english) ? "Frequency Weighted" : "gewichtet nach H\u00E4ufigkeit";
		stringHistogram = (english) ? "Histogram" : "Histogramm";	
		stringMedianCut = (english) ? "Median Cut" : "Farbreduktion: Median Cut";	
		stringWu = (english) ? "Wu Quant": "Farbreduktion: Wu Quant";	

		stringDisplayMode[0] = stringAllColors;
		stringDisplayMode[1] = stringWeighted;
		stringDisplayMode[2] = stringHistogram; 
		stringDisplayMode[3] = stringMedianCut;
		stringDisplayMode[4] = stringWu;

		ImagePlus imp = WindowManager.getCurrentImage();

		if (imp==null) {
			String str = "/images/titel.jpg";
			URL url = null;
			try {
				url = getClass().getResource(str);
				Image image = Toolkit.getDefaultToolkit().getImage(url);            
				imp = new ImagePlus(str, image);
				//imp.show(); 
			}
			catch (Exception e) {
				String msg = e.getMessage();
				if (msg==null || msg.equals(""))
					msg = "" + e;	
				IJ.showMessage("Color Inspector 3D", msg + "\n \n" + url);
			}	
		}

		cw = new CustomWindow();
		cw.init(imp);

		frame = new JFrame("Color Inspector 3D (" +  version + ")   " + imp.getTitle());

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cw.cleanup();
				frame.dispose();
			}
		});
		frame.setLocation(xPos,yPos);
		frame.setJMenuBar(new Menu());
		frame.getContentPane().add(cw);
		frame.setResizable(false);
		frame.validate();
		frame.pack();
		frame.setVisible(true);
	}


	////////////////////////////////////////////////////////////////
	public class Menu extends JMenuBar {

		private String stringColor = (english) ? "Colors" : "Farbig";
		private String stringText = (english) ? "Legend" : "Beschriftungen";
		private String stringAxes = (english) ? "Axes" : "Koordinatenachsen";
		private String stringRotation = (english) ? "Automatic Rotation" : "automatische Rotation";
		private String stringResetManip = (english) ? "Reset Color Manipulation" : "Farbmanipulationen zur\u00FCcksetzen";
		private String stringFitImage = (english) ? "Fit Image to Frame" : "Bild einpassen";
		private String stringEnglish = (english) ? "Deutsche Menus" : "English Menus";

		private String stringManual = (english) ? "Manual" : "Bedienungsanleitung";
		private String stringError1 = (english) ? "Image is too huge, program will quit" : "Bild ist zu gro\u00DF.\nProgramm wird beendet.";
		private String stringOpen = (english) ? "Open ..." : "\u00D6ffnen ...";
		private String stringSaveVisualisation = (english) ? "Save Visualisation" : "Visualisierung als Bild speichern";
		private String stringSaveModified = (english) ? "Save Modified Image" : "Manipuliertes Bild speichern";
		private String stringDuplicateVisualisation = (english) ? "Duplicate Visualisation" : "Visualisierung duplizieren";
		private String stringDuplicateModified = (english) ? "Duplicate Modified Image" : "Manipuliertes Bild duplizieren";
		private String stringQuit = (english) ? "Quit" : "Beenden";

		private String stringGradient = (english) ? " Color Gradient" : " Farbverlauf";
		private String stringComic = " Comic" ;
		private String stringStart = (english) ? " Start Image" : " Startbild";
		private String stringphoto = (english) ? " Color Photo" : " Farbfoto";
		private String stringCG = (english) ? " Computer Graphic" : " Computer Grafik";
		private String stringPalette = (english) ? " Palletized Image" : " Palettenbild";
		private String stringWebColors = (english) ? " Web Colors" : " Web-Farben";
		private String stringColorCube1 = (english) ? " Color Cube 1" : "  Farbw\u00FCrfel 1";
		private String stringColorCube2 = (english) ? " Color Cube 2" : "  Farbw\u00FCrfel 2";

		private String stringSeg1 = "Original";
		private String stringSeg2 = (english) ? "Segmented: Black & White" : "Segmentiert: Scharz/Wei\u00DF";
		private String stringSeg3 = (english) ? "Segmented: Black & White (alpha =0.5)" : "Segmentiert: Scharz/Wei\u00DF (alpha =0.5)";
		private String stringSeg4 = (english) ? "Segmented: Original & White" : "Segmentiert: Original & Wei\u00DF";
		private String stringSeg5 = (english) ? "Segmented: Original & Black" : "Segmentiert: Original & Schwarz";
		private String stringSeg6 = (english) ? "Segmented: Original & Blue" : "Segmentiert: Original & Blau";


		public Menu(  ) {
			JMenu optionsMenu = new JMenu((english)? "Options" : "Optionen");
			JMenu fileMenu = new JMenu((english)? "File" : "Datei");
			JMenu segMenu = new JMenu((english)? "Segmentation" : "Segmentierung");
			JMenu helpMenu = new JMenu((english)? "Help" : "Hilfe");
			JMenu subMenu = new JMenu((english)? "Open Sample Images" : "Beispielbilder \u00F6ffnen");

			ActionListener menuListener = new ActionListener(  ) {
				public void actionPerformed(ActionEvent event) {
					String string = event.getActionCommand();
					//System.out.println("Menu item [" + string + "] was pressed.");

					if (string.equals(stringSeg1)) {
						depthColorNear = 0;
						depthColorFar = 0;
						displayOrig = true;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringSeg2)) {
						depthColorNear = 0xFFFFFFFF;
						depthColorFar  = 0xFF000000;
						displayOrig = false;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringSeg3)) {
						depthColorNear = 0x80FFFFFF;
						depthColorFar  = 0x80000000;
						displayOrig = false;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringSeg4)) {
						depthColorNear = 0xFFFFFFFF;
						depthColorFar = 0;
						displayOrig = false;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringSeg5)) {
						depthColorNear = 0xFF000000;
						depthColorFar = 0;
						displayOrig = false;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringSeg6)) {
						depthColorNear = 0xFF0000FF;
						depthColorFar = 0;
						displayOrig = false;
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					else if (string.equals(stringColor)) {
						colorMode = !colorMode;
						mode = (mode & 2) + ((colorMode==true) ? 0 : 1);
						pic2.initTextsAndDrawColors();
						pic2.updateDisplay();
						imageRegion2.repaint();		
					}
					else if (string.equals(stringText)) {
						showText = !showText;
						pic2.initTextsAndDrawColors();
						pic2.updateDisplay();
						imageRegion2.repaint();		
					}
					else if (string.equals(stringAxes)) {
						showAxes = !showAxes;
						pic2.initTextsAndDrawColors();
						pic2.updateDisplay();
						imageRegion2.repaint();		
					}
					else if (string.equals(stringRotation)) {
						rotation = !rotation;
						if (rotation) { 
							move = true;
							cw.setDx(2);
							cw.setDy(0);
						}
						else
							move = false;
					}
					else if (string.equals(stringFitImage)) {
						fitImage = !fitImage;

						int width, height;

						if (fitImage) {
							width = Math.min( 512, pic1.getWidth()); 
							float scalew = (float)width/pic1.getWidth();
							height  = Math.min( 512, pic1.getHeight()); 
							float scaleh = (float)height/pic1.getHeight();
							float scale = Math.min(scaleh, scalew);

							width = (int)(scale*pic1.getWidth()); 
							height= (int)(scale*pic1.getHeight()); 
						}
						else {
							width = pic1.getWidth(); 
							height= pic1.getHeight(); 
						}
						imageRegion1.setWidth(width);
						imageRegion1.setHeight(height);
						cw.scrollPanel.getViewport().add(imageRegion1);
						cw.scrollPanel.validate();
						frame.pack();
						imageRegion1.repaint();		
					}
					else if (string.equals(stringEnglish)) {
						english = !english;
						cw.cleanup();

						frame.dispose();
						run("");					
					}

					else if (string.equals(stringResetManip)) {
						brightness = 0;
						contrast = 1;
						saturation = 1;
						hueChange = 0;
						//renderDepth = -222; 
						pic2.setScale(1); 
						pic2.setD(33*33);

						cw.resetSliders();
						sliderHueChange.setValue(128); 
						sliderBrightness.setValue(255); 
						sliderContrast.setValue(128); 
						sliderSaturation.setValue(128); 
						sliderScale.setValue(15); 
						sliderPerspective.setValue(84); 

						pic1.resetToOriginalImage();
						if (hist) {

							if (qMode == 1) {
								delta = (float) (256/Math.pow((6*(sliderValue0+3)), 1/3.));	
								pic1.quantize();
							}
							else if (qMode == 2) {
								pic1.quantizeMedianCut((int)sliderValue0);
							}
							else if (qMode == 3) {
								pic1.wu_quant((int)sliderValue0);
							}		
						}
						pic1.selectChannels();
						pic1.findUniqueColors();
						imageRegion1.repaint();
						pic2.computeColorSpaceCoordinates();
						pic2.initTextsAndDrawColors();
						pic2.updateDisplay();
						imageRegion2.repaint();		
					}
					else if (string.startsWith(" ")) {
						
						kltColor = null;
						
						String str=null;
						
						if (string.equals(stringGradient)) 		str = "/images/verlauf2.jpg";
						if (string.equals(stringComic))      	str = "/images/lilo.jpg";
						if (string.equals(stringStart))   		str = "/images/titel.jpg";
						if (string.equals(stringphoto))         str = "/images/baboon400.jpg";
						//if (string.equals(stringphoto))         str = "/images/mountains.jpg";
						if (string.equals(stringCG)) 			str = "/images/pool.jpg";
						if (string.equals(stringPalette))   	str = "/images/sail.gif";
						if (string.equals(stringWebColors))   	str = "/images/webcolors.gif";
						if (string.equals(stringColorCube1)) 	str = "/images/cube6.png";
						if (string.equals(stringColorCube2)) 	str = "/images/col65k.png";

						URL url = null;
						try {
							url = getClass().getResource(str);

							if (url != null) {
								cw.resetSliders();
								cw.cleanup();

								Image image = Toolkit.getDefaultToolkit().getImage(url);            
								// display the image
								ImagePlus imp = new ImagePlus(str, image);
								imp.show(); 
								//xPos = frame.getX();
								//yPos = frame.getY();
								
								frame.dispose();
								run("");
							}
						}
						catch (Exception e) {
							String msg = e.getMessage();
							if (msg==null || msg.equals(""))
								msg = "" + e;	
							IJ.showMessage("Color Inspector 3D", msg + "\n \n" + url);
						}
					}
					else if (string.equals(stringOpen)) {

						kltColor = null;
						JFileChooser fc = new JFileChooser();

						// Show open dialog; this method does not return until the dialog is closed
						if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
							cw.cleanup();
							cw.resetSliders();
							String str = fc.getSelectedFile().getPath();

							try {
								IJ.run("Open...", "path='"+str+"'");
								ImagePlus imp = WindowManager.getCurrentImage();
								imp.show();
								cw.cleanup();
								xPos = frame.getX();
								yPos = frame.getY();
								frame.dispose();
								
								try {
									run("");
									//IJ.run("Close");
								} catch (Throwable e) {
									e.printStackTrace();
									JOptionPane.showMessageDialog(null,stringError1,
											"ColorInspector 3D",JOptionPane.PLAIN_MESSAGE);
								}
							} catch (RuntimeException e) {
								JOptionPane.showMessageDialog(null,"Bild kann nicht ge\u00F6ffnet werden",
										"ColorInspector 3D",JOptionPane.PLAIN_MESSAGE);
							}
						}
					}

					else if (string.equals(stringDuplicateVisualisation)) {
						imageRegion2.saveToImage(true);
						ImagePlus imp = WindowManager.getCurrentImage();
						imp.show();
					}
					else if (string.equals(stringDuplicateModified)) {
						imageRegion1.saveToImage(false);
						ImagePlus imp = WindowManager.getCurrentImage();
						imp.show();
					}
					else if (string.equals(stringSaveVisualisation)) {
						JFileChooser fc = new JFileChooser();
						fc.setSelectedFile(new File("color3d.tif"));

						if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
							String str = fc.getSelectedFile().getPath();
							imageRegion2.saveToImage(true);
							ImagePlus imp = WindowManager.getCurrentImage();
							new FileSaver(imp).saveAsTiff(str);

							IJ.run("Close");
						}
					}
					else if (string.equals(stringSaveModified)) {
						JFileChooser fc = new JFileChooser();
						fc.setSelectedFile(new File("modified.tif"));

						if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
							String str = fc.getSelectedFile().getPath();
							imageRegion1.saveToImage(false);
							ImagePlus imp = WindowManager.getCurrentImage();
							new FileSaver(imp).saveAsTiff(str);

							IJ.run("Close");
						}
					}

					else if (string.equals(stringQuit)) {
						cw.cleanup();
						frame.dispose();
					}

					else if (string.equals(stringManual)) {

						try {
							String url = (english) ? "http://www.f4.fhtw-berlin.de/~barthel/ImageJ/ColorInspector/help.htm" :
								"http://www.f4.fhtw-berlin.de/~barthel/ImageJ/ColorInspector/hilfe.htm";
							BrowserLauncher.openURL(url);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					else if (string.equals("Info")) {
						URL url = null;
						try {
							url = getClass().getResource("/images/info.jpg");
							if (url != null) {
								Image image = Toolkit.getDefaultToolkit().getImage(url);            
								// display the image
								ImagePlus imp = new ImagePlus("/images/info.jpg", image);
								imp.show();
							}
						}
						catch (Exception e) {
							String msg = e.getMessage();
							if (msg==null || msg.equals(""))
								msg = "" + e;	
							IJ.showMessage("Color Inspector 3D", msg + "\n \n" + url);
						}
					}
				}	
			};

			JMenuItem item;

			item = new JMenuItem(stringOpen);
			item.addActionListener(menuListener);
			fileMenu.add(item);

			fileMenu.add(subMenu);
			subMenu.add(item = new JMenuItem(stringphoto));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringComic));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringCG));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringGradient));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringStart));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringPalette));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringWebColors));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringColorCube1));
			item.addActionListener(menuListener);
			subMenu.add(item = new JMenuItem(stringColorCube2));
			item.addActionListener(menuListener);

			item = new JMenuItem(stringSaveVisualisation);
			item.addActionListener(menuListener);
			fileMenu.add(item);
			item = new JMenuItem(stringDuplicateVisualisation);
			item.addActionListener(menuListener);
			fileMenu.add(item);
			item = new JMenuItem(stringSaveModified);
			item.addActionListener(menuListener);
			fileMenu.add(item);
			item = new JMenuItem(stringDuplicateModified);
			item.addActionListener(menuListener);
			fileMenu.add(item);

			fileMenu.add(item = new JMenuItem(stringResetManip));
			item.addActionListener(menuListener);
			item.setSelected(true);

			item = new JMenuItem(stringQuit);
			item.addActionListener(menuListener);
			fileMenu.add(item);

			ButtonGroup buttonGroup = new ButtonGroup();
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg1));
			buttonGroup.add(item);
			item.addActionListener(menuListener);
			item.setSelected(true);
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg2));
			buttonGroup.add(item);
			item.addActionListener(menuListener);
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg3));
			buttonGroup.add(item);
			item.addActionListener(menuListener);
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg4));
			buttonGroup.add(item);
			item.addActionListener(menuListener);
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg5));
			buttonGroup.add(item);
			item.addActionListener(menuListener);
			segMenu.add(item = new JRadioButtonMenuItem(stringSeg6));
			buttonGroup.add(item);
			item.addActionListener(menuListener);

			// Assemble the Options menu.
			optionsMenu.add(item = new JCheckBoxMenuItem(stringColor));
			item.addActionListener(menuListener);
			item.setSelected(true);
			optionsMenu.add(item = new JCheckBoxMenuItem(stringAxes));
			item.addActionListener(menuListener);
			item.setSelected(true);
			optionsMenu.add(item = new JCheckBoxMenuItem(stringText));
			item.addActionListener(menuListener);
			item.setSelected(true);
			optionsMenu.add(item = new JCheckBoxMenuItem(stringRotation));
			item.setSelected(false);
			item.addActionListener(menuListener);
			if (pic1.getHeight() > 512 || pic1.getWidth() > 512) {
				optionsMenu.add(item = new JCheckBoxMenuItem(stringFitImage));
				item.setSelected(fitImage);
				item.addActionListener(menuListener);
			}
			optionsMenu.add(item = new JMenuItem(stringEnglish));
			item.addActionListener(menuListener);

			item = new JMenuItem(stringManual);
			item.addActionListener(menuListener);
			helpMenu.add(item);

			item = new JMenuItem("Info");
			item.addActionListener(menuListener);
			helpMenu.add(item);

			// Finally, add all the menus to the menu bar.
			add(fileMenu);
			add(optionsMenu);
			add(segMenu);
			add(helpMenu);
		}	
	}

	///////////////////////////////////////////////////////////////////////////
	class CustomWindow extends JPanel implements 
	MouseListener, MouseMotionListener, ChangeListener, ActionListener/*, ItemListener */{

		private final String stringBrightness = (english) ? "Brightness" :"Helligkeit";
		private final String stringContrast = (english) ? "Contrast" : "Kontrast";
		private final String stringSaturation = (english) ? "Saturation" : "S\u00E4ttigung";
		private final String stringHueChange = (english) ? "Color Rotation" : "Farbrotation";
		private final String stringPerspective = (english) ?  "Perspective" : "Perspektive";
		private final String stringScale = (english) ? "Scale" : "Skalierung";
		private final String stringDisplay = (english) ? "  Display Mode:" : "  Darstellungsart:";
		private final String stringColorSpace = (english) ? "Color Space:" : "Farbraum:";
		private final String stringDepth = (english) ? "Depth" : "Tiefe";

		protected  Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		protected  Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		protected  Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);

		private JPanel imagePanel, displayPanel;
		private JScrollPane scrollPanel;


		private JComboBox displayChoice; 
		private JComboBox colorSpaceChoice; 

		private boolean drag = false; 
		private int checkMove = 0;
		private int xStart;
		private int yStart;
		private int xAct;
		private int yAct;

		private int xdiff;
		private int ydiff;

		private float dx;
		private float dy;

		private Dimension leftSize  = new Dimension(); 
		private Dimension rightSize = new Dimension(); 

		
		private final static int H  = 522; // Height
		private final static int WL = 512; // max. left width
		private final static int WR = 512; // max. right width 
		private final static int WD = 75;  // width for depth slider 

		private TurnThread thread;
		private JButton button; 

		void cleanup() {
			if (thread != null)
				thread.interrupt();

			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (pic1 != null) {
				pic1.pixels = null;
				pic1.pixelsAlpha = null;
				pic1.pixelsZ = null;
				pic1 = null;
			}
			if (pic2 != null) {
				pic2.pixels = null;
				pic2.pixelsAlpha = null;
				pic2.pixelsZ = null;
				pic2 = null;
			}

			colorSpace = 0;
			colHash = null;

			imagePanel = null;

			renderDepth = -222; 
			saturation = 1;
			brightness = 0;
			contrast = 1;
			hueChange = 0;

			showAxes = true;
			showText = true;
			colorMode = true;
			sliderValue0 = 100;

			mode = 0;
			shift = false;
			hist = false;
			pause = false;
			move = false;
			rotation = false;
			fitImage  = true;
			displayOrig = true;

			delta = 1; 
		}

		public void resetSliders() {
			sliderFactor0.setValue(100);
			sliderFactor1.setValue(100);
			sliderFactor2.setValue(100);
			channelFactor[0] = channelFactor[1] = channelFactor[2] = 1;
			String str = stringColorSpaceLetters[colorSpace][0] + " ( x 1.0 )"; 
			setSliderTitle(sliderFactor0, Color.black, str );
			str = stringColorSpaceLetters[colorSpace][1] + " ( x 1.0 )"; 
			setSliderTitle(sliderFactor1, Color.black, str );
			str = stringColorSpaceLetters[colorSpace][2] + " ( x 1.0 )"; 
			setSliderTitle(sliderFactor2, Color.black, str );
			
			if (colorSpace==HSV || colorSpace==HSL || colorSpace==HMMD || colorSpace==XYY || colorSpace==XYZ) {
				sliderFactor0.setEnabled(false);
				sliderFactor1.setEnabled(false);
				sliderFactor2.setEnabled(false);
			}
			else {
				sliderFactor0.setEnabled(true);
				sliderFactor1.setEnabled(true);
				sliderFactor2.setEnabled(true);
			}
				
			str = stringSaturation + " ( x " + ((int) (saturation*100 + 0.5f))/100f +")"; 
			setSliderTitle(sliderSaturation, (saturation==1)?Color.black:Color.blue, str );

			str = stringBrightness + " ( " + ((brightness>=0)?"+" : "") + brightness +" )"; 
			setSliderTitle(sliderBrightness, (brightness==0)?Color.black:Color.blue, str );
		
			str = stringContrast + " ( x " + ((int) (contrast*100 + 0.5f))/100f +" )"; 
			setSliderTitle(sliderContrast, (contrast==1)?Color.black:Color.blue, str );
			
			str = stringHueChange + " ( "+ (int)(hueChange*360) +" " + degreeSymbol + " )"; 
			setSliderTitle(sliderHueChange, (hueChange==0)?Color.black:Color.blue, str );	
		}	

		void init(ImagePlus imp) {
			cleanup();
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			// display region
			imagePanel = new JPanel();
			imagePanel.setBackground(Color.lightGray);
			imagePanel.setLayout(new BorderLayout());
			
			displayPanel = new JPanel();
			displayPanel.setBackground(Color.lightGray);
			displayPanel.setLayout(new BorderLayout());

			////////////////////////////////////////////////////////
			// on the left the original image
			imageRegion1 = new ImageRegion();
			imageRegion1.addMouseMotionListener(this);
			imageRegion1.addMouseListener(this);

			pic1 = new Picture(imp);  
			imageRegion1.setImage(pic1);
			pic1.setImageRegion(imageRegion1);
			pic1.setupOverlayImage();


			leftSize.width = Math.min( WL, pic1.getWidth()); 
			leftSize.height  = Math.min( H, pic1.getHeight()); 

			if (fitImage) {
				float scalew = (float)leftSize.width/pic1.getWidth();
				float scaleh = (float)leftSize.height/pic1.getHeight();
				float scale = Math.min(scaleh, scalew);

				leftSize.width = (int)(scale*pic1.getWidth()); 
				leftSize.height= (int)(scale*pic1.getHeight()); 

				imageRegion1.setWidth(Math.min(leftSize.width, WL));
				imageRegion1.setHeight(Math.min(leftSize.height, H));
			}
			else {
				leftSize.width = (int)(pic1.getWidth()); 
				leftSize.height= (int)(pic1.getHeight()); 
				imageRegion1.setWidth(leftSize.width);
				imageRegion1.setHeight(leftSize.height);
			}

			// scrollPanel
//			scrollPanel = new JScrollPane();
//			scrollPanel.getViewport().add(imageRegion1);
			imageRegion1.setPreferredSize(new Dimension(leftSize.width, leftSize.height));
			scrollPanel = new JScrollPane(imageRegion1);
			
			scrollPanel.setPreferredSize(new Dimension(Math.max(leftSize.width+10,300), H));
			
			////////////////////////////////////////////////////////
			// in the center the color distribution 
			imageRegion2 = new ImageRegion();
			imageRegion2.setBackground(Color.lightGray);

			rightSize.width = WR;
			rightSize.height = H;

			imageRegion2.addMouseMotionListener(this);
			imageRegion2.addMouseListener(this);
			imageRegion2.setWidth(rightSize.width);
			imageRegion2.setHeight(rightSize.height);

			pic2 = new Picture(WR, H);
			imageRegion2.setImage(pic2);
			imageRegion2.setPreferredSize(new Dimension(WR,H));
			pic2.setImageRegion(imageRegion2);

			pic1.findUniqueColors();
			numberOfColorsOrig = numberOfColors;
			if (numberOfColorsOrig <= 256)
				freqFactor = sliderValue0*1000f/maskSize; 
			else 
				freqFactor = sliderValue0*200000f/maskSize;


			/////////////////////////////////////////////////////
			JPanel depthPanel = new JPanel();
			depthPanel.setLayout(new GridLayout(3,1));

			
			sliderDepth = makeTitledVerticalSilder(stringDepth , Color.black, -222, 222, renderDepth );
			depthPanel.add(sliderDepth);
			sliderPerspective = makeTitledVerticalSilder(stringPerspective + "", Color.black, 50, 118, 84 );
			depthPanel.add(sliderPerspective); 
			sliderScale = makeTitledVerticalSilder(stringScale + "", Color.black, 0, 30, 15 );
			depthPanel.add(sliderScale); 

			depthPanel.setPreferredSize(new Dimension(WD,H));

			displayPanel.add(scrollPanel,BorderLayout.WEST);			
			displayPanel.add(imageRegion2,BorderLayout.CENTER);

			imagePanel.add(displayPanel,BorderLayout.WEST);
			imagePanel.add(depthPanel,BorderLayout.EAST);			

			JPanel topPanel = new JPanel();
			topPanel.setLayout(new GridLayout(2,1));

			JPanel buttonPanel1 = new JPanel();
			buttonPanel1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

			JLabel label2 = new JLabel(stringColorSpace);
			buttonPanel1.add(label2);

			// color model (space)
			colorSpaceChoice = new JComboBox(stringColorSpaceNames);
			colorSpaceChoice.setMaximumRowCount(stringColorSpaceNames.length);
			colorSpaceChoice.addActionListener(this);	
			buttonPanel1.add(colorSpaceChoice);

			JLabel label1 = new JLabel(stringDisplay);
			buttonPanel1.add(label1);

			// display modes
			displayChoice = new JComboBox(stringDisplayMode);
			displayChoice.addActionListener(this);			
			buttonPanel1.add(displayChoice);

			label0 = new JLabel("");
			buttonPanel1.add(label0);
			label0.setVisible(false);

			jTextField = new JTextField();
			jTextField.setColumns(3);

			buttonPanel1.add(jTextField);
			jTextField.setVisible(false);

			slider0 = new JSlider(JSlider.HORIZONTAL, 1, 256, 100 );
			slider0.addChangeListener( this );
			slider0.addMouseListener(this);
			slider0.setVisible(false);
			buttonPanel1.add(slider0); 

			button = new JButton((english) ? "LUT" : "Liste");
			button.setVisible(false);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					pic1.printLut();
				}
			});
			buttonPanel1.add(button); 
			topPanel.add(buttonPanel1);

			JPanel buttonPanel0 = new JPanel();
			buttonPanel0.setLayout(new FlowLayout(FlowLayout.LEFT));

			label = new JLabel();
			buttonPanel0.add(label);

			labelInfo = new JLabel("                                    ");
			buttonPanel0.add(labelInfo);

			topPanel.add(buttonPanel0);


			///////////////////////////////////////////////////////////
			JPanel sliderPanel = new JPanel();
			sliderPanel.setPreferredSize(new Dimension(900,75));
			//sliderPanel.setLayout(new FlowLayout());
			sliderPanel.setLayout(new GridLayout(1,10,0,0));
			sliderFactor0 = makeTitledSilder("R" , Color.black, 0, 200, 100 );
			sliderPanel.add(sliderFactor0); 
			sliderFactor1 = makeTitledSilder("G" , Color.black, 0, 200, 100 );
			sliderPanel.add(sliderFactor1); 
			sliderFactor2 = makeTitledSilder("B" , Color.black, 0, 200, 100 );
			sliderPanel.add(sliderFactor2); 

			sliderBrightness = makeTitledSilder(stringBrightness , Color.black, 0, 510, 255 );
			sliderPanel.add(sliderBrightness); 
			sliderContrast = makeTitledSilder(stringContrast , Color.black, 0, 256, 128 );
			sliderPanel.add(sliderContrast); 
			sliderSaturation = makeTitledSilder(stringSaturation, Color.black, 0, 256, 128 );
			sliderPanel.add(sliderSaturation); 
			sliderHueChange = makeTitledSilder(stringHueChange, Color.black, 0, 256, 128 );
			sliderPanel.add(sliderHueChange); 
			resetSliders();
//			JButton resetButton = new JButton("Reset");
//			sliderPanel.add(resetButton); 

			add(topPanel);
			add(imagePanel);
			add(sliderPanel);
			validate();
		

			imageRegion2.newText(7);  
			imageRegion2.newLines(30*2 + 2); 
			pic2.initTextsAndDrawColors();
			pic2.updateDisplay();
			imageRegion1.repaint();	
			imageRegion2.repaint();	

			thread = new TurnThread();

			super.addKeyListener ( 			
					new KeyAdapter() { 
						public void keyPressed (KeyEvent e){ 
							if (e.isShiftDown()) 
								shift = true;
						} 
						public void keyReleased (KeyEvent e){ 
							if (!e.isShiftDown())
								shift = false;
						} 
					}  
			); 
			super.setCursor(defaultCursor);	
		}

		private JSlider makeTitledSilder(String string, Color color, int minVal, int maxVal, int val) {
			//Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );

			JSlider slider = new JSlider(JSlider.HORIZONTAL, minVal, maxVal, val );
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), 
					//empty,
					"", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			tb.setTitle(string);
			tb.setTitleJustification(TitledBorder.LEFT);
			tb.setTitleColor(color);
			slider.setBorder(tb);
			slider.setMajorTickSpacing((maxVal - minVal)/6 );
			//slider.setMajorTickSpacing((maxVal - minVal)/10 );
			slider.setPaintTicks(true);
			slider.addChangeListener( this );
			slider.addChangeListener( this );
			slider.addMouseListener(this);
			return slider;
		}

		private JSlider makeTitledVerticalSilder(String string, Color color, int minVal, int maxVal, int val) {
			//Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );

			JSlider slider = new JSlider(JSlider.VERTICAL, minVal, maxVal, val );
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), //empty,
					"", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			tb.setTitle(string);
			//tb.setTitleJustification(TitledBorder.LEFT);
			tb.setTitleColor(color);
			slider.setBorder(tb);
			slider.setMajorTickSpacing((maxVal - minVal)/6 );
			slider.setPaintTicks(true);
			slider.addChangeListener( this );
			slider.addChangeListener( this );
			slider.addMouseListener(this);
			return slider;
		}
		
		private void setSliderTitle(JSlider slider, Color color, String str) {
			//Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );
			TitledBorder tb = new TitledBorder(BorderFactory.createEtchedBorder(), //empty,
					"", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM,
					new Font("Sans", Font.PLAIN, 11));
			tb.setTitleJustification(TitledBorder.LEFT);
			tb.setTitle(str);
			tb.setTitleColor(color);
			slider.setBorder(tb);
		}


		public void stateChanged( ChangeEvent e ){
			JSlider slider = (JSlider)e.getSource();

			float sliderValue1, sliderValue2; 

			if (slider == slider0 ) {
				sliderValue0 = slider0.getValue();

				if (qMode > 1 ) {
					jTextField.setText("" + sliderValue0);
				}

				if (!slider.getValueIsAdjusting() || pic1.getPixels().length <= 512*512) {
					if (numberOfColorsOrig <= 256)
						freqFactor = sliderValue0*1000f/maskSize; 
					else 
						freqFactor = sliderValue0*200000f/maskSize; 

					pic1.changeColorHSB();
					pic1.selectChannels();

					if (qMode == 1) {
						delta = (float) (256/Math.pow((6*(sliderValue0+3)), 1/3.));	
						pic1.quantize();
					}
					else if (qMode == 2) {
						pic1.quantizeMedianCut((int)sliderValue0);
					}
					else if (qMode == 3) {
						pic1.wu_quant((int)sliderValue0);
					}
					pic1.findUniqueColors();
					imageRegion1.repaint();
					pic2.initTextsAndDrawColors();
					pic2.computeColorSpaceCoordinates();
					pic2.updateDisplay();
					imageRegion2.repaint();
				}
			}
			else if (slider == sliderPerspective || slider == sliderScale) {	
				if (slider == sliderPerspective) {
					sliderValue1 = 75 - sliderPerspective.getValue()/2.f; 
					if ( sliderValue1 == 50)
						sliderValue1 = 100000;
					sliderValue1 = sliderValue1*sliderValue1;
					pic2.setD(sliderValue1);
				}
				if (slider == sliderScale) {
					sliderValue2 = sliderScale.getValue()-15;
					float scale = (float) Math.pow(1.05,sliderValue2);
					pic2.setScale(scale); 
				}

				pic2.updateDisplay();
				imageRegion2.repaint();
			}
			else {
				if (slider == sliderFactor0) {
					channelFactor[0] = sliderFactor0.getValue()/100.;
					String str = stringColorSpaceLetters[colorSpace][0] + " ( x " + channelFactor[0] + ")"; 
					setSliderTitle(sliderFactor0, (channelFactor[0]==1)?Color.black:Color.blue, str );
				}
				if (slider == sliderFactor1) {
					channelFactor[1] = sliderFactor1.getValue()/100.;
					String str = stringColorSpaceLetters[colorSpace][1] + " ( x " + channelFactor[1] + ")"; 
					setSliderTitle(sliderFactor1, (channelFactor[1]==1)?Color.black:Color.blue, str );
				}
				if (slider == sliderFactor2) {
					channelFactor[2] = sliderFactor2.getValue()/100.;
					String str = stringColorSpaceLetters[colorSpace][2] + " ( x" + channelFactor[2] + ")"; 
					setSliderTitle(sliderFactor2, (channelFactor[2]==1)?Color.black:Color.blue, str );
				}
				if (slider == sliderSaturation) {
					saturation = sliderSaturation.getValue()/128.f;
					saturation *= saturation;
					String str = stringSaturation + " ( x " + ((int) (saturation*100 + 0.5f))/100f +")"; 
					setSliderTitle(sliderSaturation, (saturation==1)?Color.black:Color.blue, str );
				}
				if (slider == sliderBrightness) {
					brightness = sliderBrightness.getValue() - 255;
					String str = stringBrightness + " ( " + ((brightness>=0)?"+" : "") + brightness +" )"; 
					setSliderTitle(sliderBrightness, (brightness==0)?Color.black:Color.blue, str );
				}
				if (slider == sliderContrast) {
					contrast = sliderContrast.getValue()/128.f;
					if (contrast < 1)
						contrast = (float) Math.pow(contrast,2);
					else if (contrast < 2)
						contrast = (float) Math.pow(contrast,4);
					else
						contrast = 256;
					String str = stringContrast + " ( x " + ((int) (contrast*100 + 0.5f))/100f +" )"; 
					setSliderTitle(sliderContrast, (contrast==1)?Color.black:Color.blue, str );
				}
				if (slider == sliderHueChange) {
					hueChange = (sliderHueChange.getValue() - 128)/256.f;
					String str = stringHueChange + " ( "+ (int)(hueChange*360) +" " + degreeSymbol + " )"; 
					setSliderTitle(sliderHueChange, (hueChange==0)?Color.black:Color.blue, str );
				}
				
				
				if (slider == sliderDepth) {
					Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );
					renderDepth = sliderDepth.getValue();
//					String st = "" + renderDepth;
//					sliderDepth.setBorder( new TitledBorder(empty, st, TitledBorder.CENTER, TitledBorder.BOTTOM,
//							new Font("Sans", Font.PLAIN, 12)));	

					pic2.updateDisplay();
					imageRegion2.repaint();

					if (!slider.getValueIsAdjusting() || pic1.getPixels().length <= 512*512) {
						pic1.checkDepth(displayOrig);
						imageRegion1.repaint();
					}
					return;
				}

				if (!slider.getValueIsAdjusting() || pic1.getPixels().length <= 512*512) {
					pic1.changeColorHSB();
					pic1.selectChannels();

					if (hist) {
						sliderValue0 = slider0.getValue();

						if (qMode == 1) {
							delta = (float) (256/Math.pow((6*(sliderValue0+3)), 1/3.));	
							pic1.quantize();
						}
						else if (qMode == 2) {
							pic1.quantizeMedianCut((int)sliderValue0);
						}
						else if (qMode == 3) {
							pic1.wu_quant((int)sliderValue0);
						}		
					}
				}

				if (!slider.getValueIsAdjusting() || pic1.getPixels().length <= 512*512) {
					pic1.findUniqueColors();
					pic1.checkDepth(displayOrig);
					imageRegion1.repaint();
					pic2.initTextsAndDrawColors();
					pic2.computeColorSpaceCoordinates();
					pic2.updateDisplay();
					imageRegion2.repaint();
				}
			}
		}

		synchronized public void actionPerformed(ActionEvent e) {
			String stringWeight = (english) ? " Weight" : " Gewichtung";
			String stringCells = (english) ? " Number of Color Cells" : " Anzahl Farbzellen";
			String stringNumColors = (english) ? " Number of Colors" : " Farbanzahl ";

			JComboBox cb = (JComboBox)e.getSource();
			if (cb == displayChoice) {
				move = false;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
				String name = (String)cb.getSelectedItem();
				hist = true;

				pic1.changeColorHSB();
				pic1.selectChannels();
				boolean isSlider0 = true; //(numberOfColorsOrig > 256) ? true : false;

				if (name.equals(stringDisplayMode[0])) { // all colors 
					hist = false;
					mode = mode & 1;
					label0.setVisible(false);
					jTextField.setVisible(false);
					slider0.setVisible(false);
					button.setVisible(false);
					qMode = 0;
				}
				else if (name.equals(stringDisplayMode[1])) { // frequency weighted
					hist = false;
					mode = (mode & 1) + 2;
					label0.setText(stringWeight);
					label0.setVisible(true);
					jTextField.setVisible(false);
					slider0.setVisible(true);
					button.setVisible(false);
					qMode = 0;
				}
				else if (name.equals(stringDisplayMode[2])) { // histogram AxBxC
					if ( numberOfColorsOrig > 256) {
						label0.setText(stringCells);
						label0.setVisible(isSlider0);
						slider0.setVisible(isSlider0);
					}
					else {
						slider0.setVisible(false);
						label0.setVisible(false);
					}
					jTextField.setVisible(false);
					button.setVisible(true);

					qMode = 1;
					delta = (float) (256/Math.pow((6*(slider0.getValue()+3)), 1/3.));	
					pic1.quantize();
				}
				else {
					button.setVisible(true);
					label0.setText(stringNumColors);
					jTextField.setVisible(true);
					jTextField.setText("" + (int)sliderValue0);
					jTextField.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							sliderValue0 = Integer.parseInt(jTextField.getText());
							slider0.setValue((int) sliderValue0);
						}
					});

					if (name.equals(stringDisplayMode[3])) { // median cut

						label0.setVisible(isSlider0);
						slider0.setVisible(isSlider0);
						qMode = 2;
						pic1.quantizeMedianCut(slider0.getValue()); 
					}
					else if (name.equals(stringDisplayMode[4])) { //  Wu Quant
						label0.setVisible(isSlider0);
						slider0.setVisible(isSlider0);
						qMode = 3;
						pic1.wu_quant(slider0.getValue()); 
					}
				}

				pic1.selectChannels();
				pic1.findUniqueColors();
				imageRegion1.repaint();

				pic2.computeColorSpaceCoordinates();
			}

			if (cb == colorSpaceChoice) {
				String name = (String)cb.getSelectedItem();

				for (int cs = 0; cs < stringColorSpaceNames.length; cs++) {
					if (name.equals(stringColorSpaceNames[cs])) {
						colorSpace = cs;
						resetSliders();
						move = false;
						pic2.computeColorSpaceCoordinates();
						break;
					}
				}	
			}

			pause = false;
			pic1.checkDepth(displayOrig);
			imageRegion1.repaint();
			pic2.initTextsAndDrawColors();
			pic2.updateDisplay();
			imageRegion2.repaint();
		}

		public void mouseClicked(MouseEvent arg0) {

			Object source = arg0.getComponent(); // .getSource();

			if (source == imageRegion1) {
				move = false;	
			}
			else if (source == imageRegion2) {
				move = false;	
				if(arg0.getClickCount() > 1) {
					pic1.setMouseAngle(0, 180); 
					pic2.setMouseAngle(0, 180); 
					pic1.checkDepth(displayOrig);
					imageRegion1.repaint();
					pic2.initTextsAndDrawColors();
					pic2.updateDisplay();
					imageRegion2.repaint();
				}	
			}
			else if ((JSlider)source == sliderFactor0) {
				if(arg0.getClickCount() > 1) {
					channelFactor[0] = 1;
					sliderFactor0.setValue(100); 
				}	
			}
			else if ((JSlider)source == sliderFactor1) {
				if(arg0.getClickCount() > 1) {
					channelFactor[1] = 1;
					sliderFactor1.setValue(100); 
				}	
			}
			else if ((JSlider)source == sliderFactor2) {
				if(arg0.getClickCount() > 1) {
					channelFactor[2] = 1;
					sliderFactor2.setValue(100); 
				}	
			}
			else if ((JSlider)source == sliderBrightness) {
				if(arg0.getClickCount() > 1) {
					brightness = 0;
					sliderBrightness.setValue(255); 
				}	
			}
			else if ((JSlider)source == sliderContrast) {
				if(arg0.getClickCount() > 1) {
					contrast = 1;
					sliderContrast.setValue(128); 
				}	
			}
			else if ((JSlider)source == sliderSaturation) {
				if(arg0.getClickCount() > 1) {
					saturation = 1;
					sliderSaturation.setValue(128); 
				}	
			}
			else if ((JSlider)source == sliderHueChange) {
				if(arg0.getClickCount() > 1) {
					hueChange = 0;
					sliderHueChange.setValue(128); 
				}	
			}
			else if ((JSlider)source == sliderScale) {
				if(arg0.getClickCount() > 1) {
					pic2.setScale(1);
					sliderScale.setValue(15);  
				}	
			}
			else if ((JSlider)source == sliderPerspective) {
				if(arg0.getClickCount() > 1) {
					pic2.setD(33*33);
					sliderPerspective.setValue(84);  
				}	
			}
			super.requestFocus();
		}

		public void mouseEntered(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion2) {
				super.setCursor(moveCursor);		
			}
			else if (source == imageRegion1) {
				super.setCursor(handCursor);
			}

		}
		public void mouseExited(MouseEvent arg0) {
			Object source = arg0.getSource();

			if (source == imageRegion1) {
				imageRegion2.setDot(false);
				imageRegion2.setText("", 6);
				imageRegion2.repaint();
				labelInfo.setText("");
			}
			super.setCursor(defaultCursor);
		}

		public void mousePressed(MouseEvent arg0) {
			Object source = arg0.getSource();

			if (source == imageRegion2) {
				checkMove = 0;
				xStart = arg0.getX();
				yStart = arg0.getY();

				drag = true;

				dx = dy = 0;
				xdiff = 0;
				ydiff = 0;

			}
			else if (source == imageRegion1) {
				int xPos = arg0.getX();
				int yPos = arg0.getY();

				if (xPos > 0 && xPos < leftSize.width && yPos > 0 && yPos < leftSize.height) {
					if (fitImage) {
						xPos = (xPos*pic1.getWidth() )/leftSize.width;
						yPos = (yPos*pic1.getHeight())/leftSize.height;
					}
					int c = pic1.getColor(xPos, yPos);

					String string = "  Position x: " + xPos +" y: " + yPos; 

					String string2 = pic2.showColorDot(c);

					labelInfo.setText(string + string2);
				}
				else {
					labelInfo.setText("");
					imageRegion2.setDot(false);
					imageRegion2.repaint();
				}
			}
			else if (source == slider0 || source == sliderSaturation || source == sliderBrightness || 
					source == sliderContrast || source == sliderHueChange)
				move = false;
			super.requestFocus();
		}

		public void mouseReleased(MouseEvent arg0) {
			Object source = arg0.getSource();
			drag = false;	
			if (source == imageRegion2) {
				checkMove = 3;
			}
			pic1.checkDepth(displayOrig);
			imageRegion1.repaint();
		}

		public void mouseDragged(MouseEvent arg0) {
			Object source = arg0.getSource();

			if (source == imageRegion2) {
				if (drag == true) {
					checkMove = 0;
					move = false;
					xAct = arg0.getX();
					yAct = arg0.getY();
					xdiff = xAct - xStart;
					ydiff = yAct - yStart;

					dx = (5*dx + xdiff)/6.f;
					dy = (5*dy + ydiff)/6.f;

					if (shift == false) { 
						pic1.setMouseMovement(xdiff, ydiff);
						pic2.setMouseMovement(xdiff, ydiff);
					}
					else
						pic2.setMouseMovementOffset(xdiff, ydiff);
					xStart = xAct;
					yStart = yAct;
				}

				if (pic1.getPixels().length <= 512*512) {
					pic1.checkDepth(displayOrig);
					imageRegion1.repaint();
				}
				pic2.updateDisplay();
				imageRegion2.repaint();
			}	
			else if (source == imageRegion1) {
				int xPos = arg0.getX();
				int yPos = arg0.getY();
				
				if (xPos > 0 && xPos < leftSize.width && yPos > 0 && yPos < leftSize.height) {
					if (fitImage) {
						xPos = (xPos*pic1.getWidth() )/leftSize.width;
						yPos = (yPos*pic1.getHeight())/leftSize.height;
					}
					int c = pic1.getColor(xPos, yPos);

					String string = "  Position x: " + xPos +" y: " + yPos; 

					String string2 = pic2.showColorDot(c);

					labelInfo.setText(string + string2);
				}
				else {
					labelInfo.setText("");
					imageRegion2.setDot(false);
					imageRegion2.repaint();
				}
			}
			super.requestFocus();  
		}

		public void mouseMoved(MouseEvent arg0) {
			Object source = arg0.getSource();

			if (source == imageRegion1) {
				int xPos = arg0.getX();
				int yPos = arg0.getY();
				//System.out.println(xPos + " " +yPos);

				if (xPos >= 0 && xPos < pic1.getWidth() && yPos >= 0 && yPos < pic1.getHeight()) {
					if (fitImage) {
						xPos = (xPos*pic1.getWidth() )/leftSize.width;
						yPos = (yPos*pic1.getHeight())/leftSize.height;
					}

					int c = pic1.getColor(xPos, yPos);

					if (c != 0) {
						String string = "  Position x: " + xPos +" y: " + yPos; 

						String string2 = pic2.showColorDot(c);

						labelInfo.setText(string + string2);
					}
				}
				else {
					labelInfo.setText("");
					imageRegion2.setDot(false);
					imageRegion2.repaint();
				}
			}

			if (source == imageRegion2) {
				if (checkMove > 0 && (dx != 0 || dy != 0) && rotation)
					move = true;
			}
		}


		//////////////////////////////////////////////////////////////

		class TurnThread extends Thread {

			public TurnThread () { 
				this.setPriority ( Thread.MIN_PRIORITY );
				this.start();
			}

			public void run() {
				long tna = 0, tn = 0; 
				long tm = System.currentTimeMillis();
				float fps = 0;
				int delay = 40;

				try {
					while (!interrupted()) {

						if (move && !pause) {
							delay = 40;

							long dt = (tna == 0 || tn == 0) ? 0 : (tn - tna); 
							if (dt > 0) {
								if (fps == 0)
									fps = 10000.f/dt;
								else
									fps = (9*fps + 10000.f/dt)/10.f;
								imageRegion2.setText((Misc.fm(4,((int)fps/10.))+ " fps"),5);
							}
							pic2.setMouseMovement((int)dx, (int)dy);
							pic1.setMouseMovement((int)dx, (int)dy);
							pic2.updateDisplay();

							if (imageRegion2.dot == true) {
								pic2.setDot();
								imageRegion2.setDot(pic2.getX(), pic2.getY());
							}
							imageRegion2.repaint();

							if (pic1 != null)
								if (pic1.getPixels().length <= 512*512) {
									pic1.checkDepth(displayOrig);
									imageRegion1.repaint();
								}


							tna = tn;
							tn = System.currentTimeMillis();
						}
						else { 
							imageRegion2.setText("", 5);

							if (delay != 200)
								imageRegion2.repaint();
							delay = 200;

							fps = 0;
							checkMove--; 
						}

						tm += delay;

						sleep(2+Math.max(0, tm - System.currentTimeMillis()));

					}
				} catch (InterruptedException e){}
			} 	
		}

		public void setDx(float dx) {
			this.dx = dx;
		}
		public void setDy(float dy) {
			this.dy = dy;
		}
	}


	///////////////////////////////////////////////////////////////////////////

	class ImageRegion extends JPanel { 
		private Image image;
		private Image imageOverlay;

		private int width;
		private int height;

		private float scale = 1;
		private int xPos;
		private int yPos;

		private TextField[] textField = null;
		private Lines[] lines = null;

		private boolean dot = false;

		private Color planeColor = Color.lightGray;

		private Font font1 = new Font("Sans", Font.PLAIN, 18);
		private Font font2 = new Font("Sans", Font.PLAIN, 15);

		private int plotNumber = 1;
		private short[] pixelsZ = null;

		public void setDot(int xPos, int yPos) {
			dot = true;
			this.xPos = xPos;
			this.yPos = yPos;	
		}
		public void setDot(boolean b) {
			dot = b;
		}
		public void setPlaneColor(Color color) {
			planeColor = color;
		}

		public void newText(int n) {
			textField = new TextField[n];
			for (int i = 0; i < n; i++)
				textField[i] = new TextField();
		}

		public void setText(String text, int i, int posx, int posy, int z, Color color) {
			textField[i].setText(text);
			textField[i].setXpos(posx);
			textField[i].setYpos(posy);
			textField[i].setColor(color);
		}

		public void setText(String text, int i, Color color) {
			textField[i].setText(text);
			textField[i].setColor(color);
		}
		public void setText(String text, int i) {
			textField[i].setText(text);
		}

		public void setTextPos(int i, int posx, int posy, int z) {
			textField[i].setXpos(posx);
			textField[i].setYpos(posy);
			textField[i].setZ(z);
		}

		public void newLines(int n) {
			lines = new Lines[n];
			for (int i = 0; i < n; i++)
				lines[i] = new Lines();
		}

		public void setLine(int i, int x1, int y1, int x2, int y2, int z1, int z2, Color color) {
			lines[i].setPos(x1, y1, x2, y2, z1, z2, color);
		}

		public void setImage(Picture pic){
			height = pic.getHeight();
			width = pic.getWidth();
			image = pic.getImage();
		}

		public void setImage(Image image){
			this.image = image;
		}
		
		public void setImage(Image image, short[] pixelsZ) {
			this.image = image;
			this.pixelsZ  = pixelsZ;
		}

		public void setOverlayImage(Image image){
			this.imageOverlay = image;
		}

		void saveToImage(boolean name) {

			pause = true;

			BufferedImage bufferedImage =  new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			paint(bufferedImage.createGraphics());

			Graphics2D g2d = bufferedImage.createGraphics();
			if (name) {
				g2d.setColor(Color.black);
				g2d.drawString("Color Inspector 3D", width - 150, height - 10); 
			}
			g2d.dispose();

			String s = "Color Distribution "+plotNumber;
			ImagePlus plotImage = NewImage.createRGBImage (s, width, height, 1, NewImage.FILL_BLACK);
			ImageProcessor ip = plotImage.getProcessor();
			int[] pixels = (int[]) ip.getPixels();
			bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

			plotImage.show();
			plotImage.updateAndDraw();	

			plotNumber++;
			pause = false;
		}

		//-------------------------------------------------------------------
		synchronized public void paintComponent(Graphics g1) {  

			Graphics2D g = (Graphics2D)g1;
			super.paintComponent(g); // paintComponent der super-Klasse !

			g.setColor(planeColor);
			g.fillRect(0, 0, width, height);

			g.setFont(font1); 

			if (textField != null && showText == true)
				for (int i=0; i<textField.length; i++) {
					if (textField[i] != null) 
						if (textField[i].getZ() > 0) {
							g.setColor(textField[i].getColor());
							g.drawString(textField[i].getText(), 
									textField[i].getXpos(), textField[i].getYpos());
						}
				}

			if (image != null ) {
				int xstart = (int) -((scale - 1)*width/2.); 
				int ystart = (int) -((scale - 1)*height/2.); 
				g.drawImage(image, xstart, ystart, (int)(scale*width), (int)(scale*height), this);

				if (imageOverlay != null ) {
					g.drawImage(imageOverlay, xstart, ystart, (int)(scale*width), (int)(scale*height), this);
				}
			}

			
			if (lines != null  && showAxes == true)
				for (int i=0; i<lines.length; i++) {
					if (lines[i] != null)  {
						float x1 = lines[i].x1;
						float y1 = lines[i].y1;
						float z1 = lines[i].z1;
						float x2 = lines[i].x2;
						float y2 = lines[i].y2;
						float z2 = lines[i].z2;
						
						float dx = x1-x2;
						float dy = y1-y2;
						float dz = z1-z2;
						
						float length = (float) Math.sqrt(dx*dx+dy*dy);
						if (length > 0) {
							dx /= length;
							dy /= length;
							dz /= length;

							g.setColor(lines[i].color);

							float x = x2;
							float y = y2;
							float z = z2;
							for (int j = 0; j < length; j++) {
								int pos = (int)y*width+(int)x;
								if (pos >= 0 && pos < pixelsZ.length)
									if (z <= pixelsZ[pos] ) {
										g.drawLine((int)x, (int)y, (int)(x+dx), (int)(y+dy));
									}
								x+=dx;
								y+=dy;
								z+=dz;
							}
						}		
					}
				}

			if (textField != null && showText == true)
				for (int i=0; i<textField.length; i++) {
					if (textField[i] != null) 
						if (textField[i].getZ() <= 0) {
							if (i > 3)
								g.setFont(font2);

							g.setColor(textField[i].getColor());
							g.drawString(textField[i].getText(), 
									textField[i].getXpos(), textField[i].getYpos());
						}
				}

			if (dot == true) {				
				g.setColor(Color.orange);
				g.drawLine(xPos-3, yPos-2 ,  xPos-10, yPos-2);
				g.drawLine(xPos-3, yPos-1 ,  xPos-10, yPos-1);
				g.drawLine(xPos-3, yPos+2 ,  xPos-10, yPos+2);
				g.drawLine(xPos-3, yPos+3 ,  xPos-10, yPos+3);
				g.drawLine(xPos+4, yPos-2 ,  xPos+10, yPos-2);
				g.drawLine(xPos+4, yPos-1 ,  xPos+10, yPos-1);
				g.drawLine(xPos+4, yPos+2 ,  xPos+10, yPos+2);
				g.drawLine(xPos+4, yPos+3 ,  xPos+10, yPos+3);
				g.drawLine(xPos-2, yPos-3 ,  xPos-2,  yPos-10);
				g.drawLine(xPos-1, yPos-3 ,  xPos-1,  yPos-10);
				g.drawLine(xPos+2, yPos-3 ,  xPos+2,  yPos-10);
				g.drawLine(xPos+3, yPos-3 ,  xPos+3,  yPos-10);
				g.drawLine(xPos-2, yPos+4 ,  xPos-2,  yPos+10);
				g.drawLine(xPos-1, yPos+4 ,  xPos-1,  yPos+10);
				g.drawLine(xPos+2, yPos+4 ,  xPos+2,  yPos+10);
				g.drawLine(xPos+3, yPos+4 ,  xPos+3,  yPos+10);

				g.setColor(Color.black);
				g.drawLine(xPos-3, yPos ,    xPos-12, yPos);
				g.drawLine(xPos-3, yPos+1 ,  xPos-12, yPos+1);
				g.drawLine(xPos+4, yPos ,    xPos+12, yPos);
				g.drawLine(xPos+4, yPos+1 ,  xPos+12, yPos+1);
				g.drawLine(xPos,   yPos-3 ,  xPos,   yPos-12);
				g.drawLine(xPos+1, yPos-3 ,  xPos+1, yPos-12);
				g.drawLine(xPos,   yPos+4 ,  xPos,   yPos+12);
				g.drawLine(xPos+1, yPos+4 ,  xPos+1, yPos+12);
			}
		}
		//-------------------------------------------------------------------

		synchronized public void update(Graphics g) {
			paintComponent(g);
		}
		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public int getWidth() {
			return width;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public void setScale(float scale) {
			this.scale = scale;
		}

	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * @author   barthel
	 */
	private  class Picture {

		private int R;
		private int G;
		private int B;
		private int X;
		private int Y;
		private int Z;
		private int dotR;
		private int dotG;
		private int dotB;

		private String stringColor = (english) ? "Color" : "Farbwert";
		private String stringNumber = (english) ? "Frequency" : "Haeufigkeit";

		private Image  image;    // AWT-Image

		
		private final static int dm = 18; 
		private final static int dp = 10;

		private final  int [][] cubeCornersRGB =  { 
				{-128,-128,-128}, // VUL
				{-128, 127, 127}, // VOR
				{ 127,-128, 127}, // HUR
				{ 127, 127,-128}, // HOL
				{-128,-128, 127}, // VUR
				{-128, 127,-128}, // VOL
				{ 127,-128,-128}, // HUL
				{ 127, 127, 127}  // HOR
		};

		private final  int [][] lineEndsYUV =  { 
				{   0,   0,-128}, // Y U
				{-128,   0,-128}, // U L
				{   0,-128,-128}, // V V
				{   0,   0, 127}, // Y 0
				{ 127,   0,-128}, // U L
				{   0, 127,-128}  // V H
		};
		private final  int [][] lineEndsKLT =  { 
				{   0,   0,-128}, // Y U
				{-128,   0,   0}, // U L
				{   0,-128,   0}, // V V
				{   0,   0, 127}, // Y 0
				{ 127,   0,   0}, // U L
				{   0, 127,   0}  // V H
		};
		private final  int [][] lineEndsHSB =  { 
				{   0,   0,-128}, // B U
				{   0,   0,-128}, // S M
				{   0,   0, 127}, // B O
				{ 127,   0,-128},  
		};
		private final  int [][] lineEndsHSV =  { 
				{   0,   0,-128}, // B U
				{   0,   0, 127}, // S M
				{   0,   0, 127}, // B O
				{ 127,   0, 127},  
		};
		private final  int [][] lineEndsHMMD =  { 
				{   0,   0,-128}, // B U
				{   0,   0,   0}, // S M
				{   0,   0, 127}, // B O
				{ 128,   0,   0}  // S 
		};
		private final  int [][] lineEndsLAB = lineEndsYUV;
		
		private final int [][] textPosRGB = {
				{ -128-dm, -128-dm, -128-dm}, // 0
				{  127+dp, -128-dm, -128-dm}, // R
				{ -128-dm,  127+dp, -128-dm}, // G
				{ -128-dm, -128-dm,  127+dp}  // B
		};
		private final int [][] textPosXYY = textPosRGB;
		private final int [][] textPosXYZ = textPosRGB;
		private final int [][] textPosYUV = { 
				{       0,       0, -128-dm},  // 0
				{  127+dp,       0, -128-dm},  // U 
				{       0,  127+dp, -128-dm},  // V
				{       0,       0,  127+dp}   // Y
		};
		private final int [][] textPosKLT = { 
				{       0,       0, -dm},  // 0
				{  127+dp,       0, -dm},  // U 
				{       0,  127+dp, -dm},  // V
				{       0,       0, 127+dp}  // Y
		};
		private final int [][] textPosYIQ = textPosYUV; 
		private final int [][] textPosYCbCr = textPosYUV; 
		private final int [][] textPosHSB = { 
				{       0,       0, -128-dm},  // 0
				{       0, 127+2*dp, 127+dp},  // H 
				{  127+dp,       0, -128-dm},  // S
				{       0,       0,  127+dp}   // B
		};
		private final int [][] textPosHSL = textPosHSB;
		private final int [][] textPosHSV = { 
				{       0,       0, -128-dm},  // 0
				{       0, 127+2*dp, 127+dp},  // H
				{  127+dp,       0,  127+dp},  // S 
				{       0,       0,  127+dp}   // B
		};
		private final int [][] textPosHCL = textPosHSV;
		private final int [][] textPosHMMD = { 
				{       0,       0, -128-dm},  // 0
				{       0,  128+2*dp,     0},  // Diff
				{  128+2*dp,       0,     0},  // Hue 
				{       0,       0,  127+dp}   // Sum
		};
		private final int [][] textPosLAB = { 
				{       0,       0, -128-dm},  // 0
				{  127+dp,       0, -128-dm},  // a 
				{       0,  127+dp, -128-dm},  // b
				{       0,       0,  127+dp}   // L
		};
		private final int [][] textPosLUV = textPosLAB;	

		private final  int [][][] textPositions =  { 
				textPosRGB,
				textPosYUV,
				textPosYCbCr,
				textPosYIQ,
				textPosHSB,
				textPosHSV,
				textPosHSL,
				textPosHMMD,
				textPosLAB,
				textPosLUV,	
				textPosXYY,
				textPosXYZ,
				textPosKLT,
				textPosHCL
		};
		private final  String [][] letters =  {
				{"0", "R", "G", "B"},
				{"0", "U", "V", "Y"},
				{"0", "Cb","Cr","Y"},
				{"0", "I", "Q", "Y"},
				{"0", "H", "S", "B"},
				{"0", "H", "S", "V"},
				{"0", "H", "S", "L"},
				{"0", "Hue", "Diff", "Sum"},
				{"0", "a", "b", "L"},
				{"0", "u", "v", "L"},
				{"0", "x", "y", "Y"},
				{"0", "X", "Y", "Z"},
				{"0", "C1","C2","C0"},
				{"0", "H", "C", "L"}
		};
		private final  Color [][] letterCol =  {
				{Color.black, Color.red,  Color.green, Color.blue},
				{Color.black, Color.orange, Color.orange,  Color.orange}
		};
		private final  Color [][] letterGray =  {
				{Color.black, Color.red,  Color.green, Color.blue},
				{Color.black, Color.blue, Color.blue,  Color.blue}
		};

		private ImageRegion imageRegion = null;
		private int width;   				
		private int height;  		   


		private int[] pixels = null;		
		private int[] pixelsAlpha = null;
		private int[] pixelsOverlay = null;
		private short[] pixelsZ = null;		// Z-buffer

		private float xs; 
		private float ys; 
		private int xoff;
		private int yoff;

		private float xs_xoff; 
		private float ys_yoff; 

		private double angleB = -0.6125; // angle for B-rotation
		private double angleR = 2;       // angle for R-rotation

		private float d = 33*33;      // perspective

		private float cosB = ( float ) Math.cos ( angleB ) ;
		private float sinB = ( float ) Math.sin ( angleB ) ;
		private float cosR = ( float ) Math.cos ( angleR ) ;
		private float sinR = ( float ) Math.sin ( angleR ) ;

		private float cosRsinB = cosR*sinB;  
		private float cosRcosB = cosR*cosB; 
		private float sinRsinB = sinR*sinB;
		private float sinRcosB = sinR*cosB;

		private int[] vec = {0,0,0};

		private float scale = 1;

		private Color cubeBackColor;
		private Color cubeFrontColor;


		private MemoryImageSource memoryImageSource;
		private MemoryImageSource memoryOverlayImageSource;

		boolean dot = false;
		private int[] pixelsOrig;

		public Picture(ImagePlus imp){

			image = imp.getImage();

			width = imp.getWidth();
			height = imp.getHeight();

			maskSize = width*height;

			xs_xoff = xs = (float)(width/2.  + 0.5);
			ys_yoff = ys = (float)(height/2. + 0.5);

			PixelGrabber pg = null;

			pixels = new int[width*height];
			pixelsOrig = new int[width*height];
			pg = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {IJ.error("error grabbing pixels");}


			Roi roi = imp.getRoi();

			if (roi != null) {
				ImageProcessor mask = roi.getMask();
				ImageProcessor ipMask;

				byte[] mpixels = null;
				int mWidth = 0;
				if (mask != null) {
					ipMask = mask.duplicate();

					mpixels = (byte[])ipMask.getPixels();
					mWidth = ipMask.getWidth();
				}

				Rectangle r = roi.getBoundingRect();

				for (int y=0; y<height; y++) {
					int offset = y*width;
					for (int x=0; x< width ; x++) {
						int i = offset + x;

						if (x < r.x || x >= r.x+r.width || y < r.y || y >= r.y+r.height) {
							pixels[i] = (63 << 24) |  (pixels[i] & 0xFFFFFF) ;
							maskSize--;
						}
						else if (mask != null) { 
							int mx = x-r.x;
							int my = y-r.y;

							if (mpixels[my *mWidth + mx ] == 0) {
								pixels[i] = (63 << 24) |  (pixels[i] & 0xFFFFFF);
								maskSize--;
							}
						}
					}
				}
			}

			for (int i = 0; i< pixels.length; i++)
				pixelsOrig[i] = pixels[i];

			memoryImageSource = new MemoryImageSource(width, height, pixels, 0, width);
			image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
		}



		public void setupOverlayImage() {
			pixelsOverlay = new int[width*height];
			memoryOverlayImageSource = new MemoryImageSource(width, height, pixelsOverlay, 0, width);
		}



		public void resetToOriginalImage() {
			for (int i = 0; i< pixels.length; i++)
				pixels[i] = pixelsOrig[i];
		}

		public Picture(Image image, int width, int height){
			this.image = image;
			this.width  = width;    		// Breite bestimmen
			this.height = height;  			// Hoehe bestimmen

			pixels = new int[width*height];

			PixelGrabber pg =  new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {}

			xs_xoff = xs = (float)(width/2.  + 0.5);
			ys_yoff = ys = (float)(height/2. + 0.5);
		}

		public Picture (int width, int height){
			this.width = width;
			this.height = height;

			pixels  = new int[width*height];
			pixelsAlpha = new int[width*height];
			//pixelsOverlay = new int[width*height];
			pixelsZ  = new short[width*height];

			memoryImageSource = new MemoryImageSource(width, height, pixels, 0, width);

			image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);

			xs_xoff = xs = (float)(width/2.  + 0.5);
			ys_yoff = ys = (float)(height/2. + 0.5);
		}


		public void initTextsAndDrawColors() {

			imageRegion.newText(7);  
			imageRegion.newLines(30*2 + 2); 

			boolean col = ((mode & 1) == 0);

			imageRegion.setPlaneColor( (col == true) ? new Color(0xFF777777) : Color.white );

			cubeBackColor  = new Color (0, 0, 0, 100);
			cubeFrontColor = (col == true) ? Color.white : Color.blue;

			Color color;
			for (int i= 0; i < letters[0].length; i++) {
				if (col == true)
					color = (colorSpace == RGB) ? letterCol[0][i] : letterCol[1][i]; 
					else 
						color = (colorSpace == RGB) ? letterGray[0][i] : letterGray[1][i];
						imageRegion.setText(letters[colorSpace][i], i, color);
			}

			imageRegion.setText("",   5, width - 60, height - 10, 1, (col == true) ? Color.white : Color.black);

			String string; 

			string = (english) ? (maskSize + " Pixels,  " + numberOfColors +" Colors    ") :(maskSize + " Pixel,  " + numberOfColors +" Farben    "); 
			if (numberOfColors == 1)
				string = (english) ? maskSize + " Pixels " + " 1 Color     " : maskSize + " Pixel " + " 1 Farbe     ";

			label.setText(string);
		}

		public void setDot() {
			R = dotR;
			G = dotG;
			B = dotB;

			xyzPos();
		}

		int quant(int x) {
			x -= 128;
			if (x >= 0) {
				x+= (delta/2);
				x = (int) (x / delta);
				x *= delta;
			}
			else {
				x = -x;
				x+= delta/2;
				x = (int) (x / delta);
				x *= delta;
				x = -x;
			}
			x += 128;	

			if (x < 0)
				x = 0;
			if (x > 255)
				x = 255;

			return x;
		}

		/**
		 * @param scale   The scale to set.
		 * @uml.property   name="scale"
		 */
		public void setScale(float s) {
			scale = s;
			imageRegion.setScale(scale);
		}

		public void setMouseMovement(int dx, int dy) {
			angleB += dx/100.;
			angleR += dy/100.;

			//System.out.println("B: " + Math.toDegrees(angleB) + " R " + Math.toDegrees(angleR));

			cosB = (float)Math.cos(angleB);
			sinB = (float)Math.sin(angleB);
			cosR = (float)Math.cos(angleR); 
			sinR = (float)Math.sin(angleR);

			cosRsinB = cosR*sinB;  
			cosRcosB = cosR*cosB; 
			sinRsinB = sinR*sinB;
			sinRcosB = sinR*cosB;	
		}

		public void setMouseAngle(float a, float b) {
			angleB = Math.toRadians(a);
			angleR = Math.toRadians(b);

			cosB = (float)Math.cos(angleB);
			sinB = (float)Math.sin(angleB);
			cosR = (float)Math.cos(angleR); 
			sinR = (float)Math.sin(angleR);

			cosRsinB = cosR*sinB;  
			cosRcosB = cosR*cosB; 
			sinRsinB = sinR*sinB;
			sinRcosB = sinR*cosB;	
		}
		public void setMouseMovementOffset(int dx, int dy) {
			xoff += dx;
			yoff += dy;

			xs_xoff = xs + xoff;
			ys_yoff = ys + yoff;
		}

		public void setImageRegion(ImageRegion imageRegion) {
			this.imageRegion = imageRegion;
		}

		int getWidth(){
			return width;
		}
		int getHeight(){
			return height;
		}
		Image getImage(){
			return image;
		}
		int[] getPixels(){
			return pixels;
		}

		private final void xyzPosC() {

			float Y2 =  cosRsinB*R  + cosRcosB*G - sinR*B;
			float z =   sinRsinB*R  + sinRcosB*G + cosR*B;
			Z = (int) z; 

			z += d;
			z = d / z;

			X = (int)( (cosB*R - sinB*G)* z + xs_xoff);
			Y = (int)( Y2* z + ys_yoff);
		}

		private final void xyzPos() {
			float Y1 =  sinB*R  + cosB*G;
			float Y2 =  cosR*Y1 - sinR*B;
			float z =   sinR*Y1 + cosR*B;

			float sz = (scale * d /(z + d));

			Z = (int) z; 
			X = (int)( (cosB*R - sinB*G)* sz + xs  + scale * xoff);
			Y = (int)( Y2* sz + ys + scale * yoff);
		}

		private final void xyzPos(int r, int g, int b) {
			R = r;
			G = g;
			B = b;
			xyzPos();
		}

		private final void xyzPos(int[] rgb) {
			R = rgb[0];
			G = rgb[1];
			B = rgb[2];
			xyzPos();
		}

		private void rgb2ycbcr(int r, int g, int b, int[] yuv) {
			int rgb [] = {r, g, b};
			rgb2ycbcr(rgb, yuv);
		}
		private void rgb2ycbcr(int[] rgb, ColHash cv) {
			rgb2ycbcr(rgb, vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		private void rgb2ycbcr(int[] rgb, int[] yuv) {
			yuv[2] = (int)( 0.299f  *rgb[0] + 0.587f * rgb[1] + 0.114f  *rgb[2]); // Y
			yuv[0] = (int)(-0.16874f*rgb[0] - 0.33126f*rgb[1] + 0.50000f*rgb[2]);
			yuv[1] = (int)( 0.50000f*rgb[0] - 0.41869f*rgb[1] - 0.08131f*rgb[2]);		
		}

		private void ycbcr2rgb(int[] yuv, int[] rgb) {
			float y = yuv[2]; 
			float u = yuv[0];
			float v = yuv[1];

			rgb[0] = (int)(y + 1.4020f*v);
			rgb[1] = (int)(y -0.3441f*u - 0.7141f*v);
			rgb[2] = (int)(y +1.7720f*u);
		}
		
		private void yiq2rgb(int[] yiq, int[] rgb) {
			float y = yiq[2]; 
			float i = yiq[0];
			float q = yiq[1];

			rgb[0] = (int)(y + 0.9563f*i + 0.6210f*q);
			rgb[1] = (int)(y - 0.2721f*i - 0.6473f*q);
			rgb[2] = (int)(y - 1.1070f*i + 1.7046f*q);
		}

		private void rgb2klt(int r, int g, int b, int[] yuv) {
			int rgb [] = {r, g, b};
			rgb2klt(rgb, yuv);
		}
		
		private void rgb2klt(int[] rgb, ColHash cv) {
			rgb2klt(rgb, vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		
		private void rgb2klt(int[] rgb, int[] yuv) {
			double m00 = kltMatrix[0][0];
			double m01 = kltMatrix[1][0];
			double m02 = kltMatrix[2][0];
			double m10 = kltMatrix[0][1];
			double m11 = kltMatrix[1][1];
			double m12 = kltMatrix[2][1];
			double m20 = kltMatrix[0][2];
			double m21 = kltMatrix[1][2];
			double m22 = kltMatrix[2][2];
			
			double r = (rgb[0] - kltMean[0]) * 0.7071;
			double g = (rgb[1] - kltMean[1]) * 0.7071;
			double b = (rgb[2] - kltMean[2]) * 0.7071;
			
			
			yuv[2] = (int)( m00 *r + m01 * g + m02 * b); 
			yuv[0] = (int)( m10 *r + m11 * g + m12 * b);
			yuv[1] = (int)( m20 *r + m21 * g + m22 * b);		
		}
		
		private void klt2rgb(int[] vec, int[] rgb) {
			double m00 = kltMatrix[0][0];
			double m01 = kltMatrix[0][1];
			double m02 = kltMatrix[0][2];
			double m10 = kltMatrix[1][0];
			double m11 = kltMatrix[1][1];
			double m12 = kltMatrix[1][2];
			double m20 = kltMatrix[2][0];
			double m21 = kltMatrix[2][1];
			double m22 = kltMatrix[2][2];
			
			double v0 = vec[2] *1.4142f;
			double v1 = vec[0] *1.4142f;
			double v2 = vec[1] *1.4142f;
			
			
			rgb[0] = (int)( m00 * v0 + m01 * v1 + m02 * v2 + kltMean[0]); 
			rgb[1] = (int)( m10 * v0 + m11 * v1 + m12 * v2 + kltMean[1]);
			rgb[2] = (int)( m20 * v0 + m21 * v1 + m22 * v2 + kltMean[2]);
		}
		
		private void rgb2yuv(int r, int g, int b, int[] yuv) {
			int rgb [] = {r, g, b};
			rgb2yuv(rgb, yuv);
		}
		private void rgb2yuv(int[] rgb, ColHash cv) {
			rgb2yuv(rgb, vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		private void rgb2yuv(int[] rgb, int[] yuv) {
			yuv[2] = (int)(0.299f*rgb[0]  + 0.587f*rgb[1] + 0.114f*rgb[2]);
			yuv[0] = (int)((rgb[2] - yuv[2])*0.492f*0.8f); 
			yuv[1] = (int)((rgb[0] - yuv[2])*0.877f*0.8f); 
		}

		private void yuv2rgb(int[] yuv, int[] rgb) {
			float b = (yuv[0]/0.492f/0.8f) + yuv[2]; 
			float r = (yuv[1]/0.877f/0.8f) + yuv[2]; 
			float g = (yuv[2] - 0.299f*r - 0.114f*b)/0.587f;
			rgb[0] = (int)r;
			rgb[1] = (int)g;
			rgb[2] = (int)b;
		}

		private void rgb2hsb_(int[] rgb, ColHash cv) {
			float [] hsbvals = new float[3]; 
			Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsbvals);

			float phi = (float)(360*hsbvals[0]);
			float s   = hsbvals[1]*128;
			float br  = hsbvals[2]*255;

			cv.R = (int) phi;
			cv.G = (int) s;
			cv.B = (int) br;
		}

		private void hsb2rgb_(int[] hsb, int[]vec) {
			float hue = (float) (channelFactor[0]*hsb[0]/360.);
			float sat = (float) (channelFactor[1]*hsb[1]/128f);
			float br  = (float) (channelFactor[2]*(hsb[2]-127.5)/255f + .5f);
			
			sat = Math.min(1, Math.max(0, sat));
			br = Math.min(1, Math.max(0, br));
			int c = Color.HSBtoRGB(hue, sat, br);
			vec[0] = (c>>16)&0xff;
			vec[1] = (c>> 8)&0xff;
			vec[2] = (c    )&0xff;
		}


		private void rgb2hsb(int[] rgb, ColHash cv) {
			rgb2hsb(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		private void rgb2hsb(int[] yuv, int[] rgb) {
			rgb2hsb(rgb[0], rgb[1], rgb[2], yuv);
		}
		private void rgb2hsb(int r, int g, int b, int[] hsb) {
			float [] hsbvals = new float[3]; 
			Color.RGBtoHSB(r, g, b, hsbvals);

			float phi = (float)(Math.toRadians(360*hsbvals[0]));
			float s   = hsbvals[1]*128;
			float br  = hsbvals[2]*255;

			hsb[0] = (int)(s*Math.cos(phi));
			hsb[1] = (int)(s*Math.sin(phi));
			hsb[2] = (int)(br);
			hsbvals = null;
		}

		private void rgb2hsb_(int r, int g, int b, int[] hsb) {
			float [] hsbvals = new float[3]; 
			Color.RGBtoHSB(r, g, b, hsbvals);

			hsb[0] = (int)(360*hsbvals[0] + 0.5f);
			hsb[1] = (int)(hsbvals[1]*100 + 0.5f);
			hsb[2] = (int)(hsbvals[2]*100 + 0.5f);

			hsbvals = null;
		}

		private void rgb2hmmd(int[] rgb, ColHash cv) {
			rgb2hmmd(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		
		
		private void rgb2yiq(int[] rgb, ColHash cv) {
			rgb2yiq(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		
		private void rgb2yiq(int r, int g, int b, int[] yiq) {
			int rgb [] = {r, g, b};
			rgb2yiq(rgb, yiq);
		}
		
		private void rgb2yiq(int[] rgb, int[] yiq) {
			yiq[2] = (int)( 0.299f  *rgb[0] + 0.587f *rgb[1] + 0.114f  *rgb[2]); // Y
			yiq[0] = (int)( 0.5957f *rgb[0] - 0.2744f*rgb[1] - 0.3212f *rgb[2]); // I
			yiq[1] = (int)( 0.2114f *rgb[0] - 0.5226f*rgb[1] + 0.3111f *rgb[2]); // Q
		}

		private void rgb2hcl(int[] rgb, ColHash cv) {
			rgb2hcl(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		/*
	use constant Y0     => 100;
	use constant gamma  => 3;
	use constant Al     => 1.4456;
	use constant Ah_inc => 0.16;

	return ( 0, 0, 0 ) if $max == 0; # special-case black
    my $alpha = ( $min / $max ) / Y0;
    my $Q = exp( $alpha * gamma );

    my( $rg, $gb, $br ) = ( $r - $g, $g - $b, $b - $r );
    my $L = ( $Q * $max + ( 1 - $Q ) * $min ) / 2;
    my $C = $Q * ( abs( $rg ) + abs( $gb ) + abs( $br ) ) / 3;
    my $H = rad2deg( atan2( $gb, $rg ) );

    # The paper uses 180, not 90, but using 180 gives
    # red the same HCL value as green...
#   Alternative A
#    $H = 90 + $H         if $rg <  0 && $gb >= 0;
#    $H = $H - 90         if $rg <  0 && $gb <  0;
#   Alternative B
    $H = 2 * $H / 3      if $rg >= 0 && $gb >= 0;
    $H = 4 * $H / 3      if $rg >= 0 && $gb <  0;
    $H = 90 + 4 * $H / 3 if $rg <  0 && $gb >= 0;
    $H = 3 * $H / 4 - 90 if $rg <  0 && $gb <  0;

    return ( $H, $C, $L );

		 */

		private void rgb2hcl(int r, int g, int b, int hcl[]) {

			int min;    //Min. value of RGB
			int max;    //Max. value of RGB
			float Y0 = 100;
			float gamma = 3;

			float L, C, H;

			if (r > g) { min = g; max = r; }
			else { min = r; max = g; }
			if (b > max) max = b;
			if (b < min) min = b;

			if (min == 0 && max == 0) {
				L = C = H = 0;
			}
			else {
				float alpha = (float)(min / max)  / Y0;
				float Q = (float) Math.exp( alpha * gamma );

				int rg = r-g;
				int gb = g-b;
				int br = b-r;

				L = (float) ((Q*max + (1-Q)*min)/2.);
				C = (float) (Q*(Math.abs(rg) + Math.abs(gb) + Math.abs(br))/3.);
				//H = (float) Math.toDegrees(Math.atan2(gb, rg));
				H = (float) Math.toDegrees(Math.atan(gb/(double)rg));

				if (rg <  0 && gb >= 0)
					H = 180 + H;         
				if (rg <  0 && gb <  0)
					H = H - 180;         
			}
			/*
			delMax = max - min; 

			float H = 0, S;
			float V = max;

			if ( delMax == 0 ) { H = 0; S = 0; }
			else {                                   
				S = delMax/255f;

				if ( r == max ) 
					H = (      (g - b)/(float)delMax)*60;
				else if ( g == max ) 
					H = ( 2 +  (b - r)/(float)delMax)*60;
				else if ( b == max ) 
					H = ( 4 +  (r - g)/(float)delMax)*60;	
			}
			 */
			float phi = (float)(Math.toRadians(H));
			hcl[0] = (int)(C*Math.cos(phi));
			hcl[1] = (int)(C*Math.sin(phi));
			hcl[2] = (int)(L);
		}

		private void rgb2hsl(int[] rgb, ColHash cv) {
			rgb2hsl(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		private void rgb2hsl(int[] rgb, int[] vec) {
			rgb2hsl(rgb[0], rgb[1], rgb[2], vec);
		}
		private void rgb2hsl(int r, int g, int b, int hsl[]) {

			float var_R = ( r / 255f );                     //RGB values = From 0 to 255
			float var_G = ( g / 255f );
			float var_B = ( b / 255f );

			float var_Min;    //Min. value of RGB
			float var_Max;    //Max. value of RGB
			float del_Max;    //Delta RGB value

			if (var_R > var_G) { var_Min = var_G; var_Max = var_R;
			}
			else { var_Min = var_R; var_Max = var_G;
			}
			if (var_B > var_Max) var_Max = var_B;
			if (var_B < var_Min) var_Min = var_B;
			del_Max = var_Max - var_Min; 

			float H = 0, S, L;
			L = ( var_Max + var_Min ) / 2f;

			if ( del_Max == 0 ) { H = 0; S = 0; } // gray
			else {                                   //Chromatic data..{
				if ( L < 0.5 ) 
					S = del_Max / ( var_Max + var_Min );
				else           
					S = del_Max / ( 2 - var_Max - var_Min );

				float del_R = ( ( ( var_Max - var_R ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
				float del_G = ( ( ( var_Max - var_G ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
				float del_B = ( ( ( var_Max - var_B ) / 6f ) + ( del_Max / 2f ) ) / del_Max;

				if ( var_R == var_Max ) 
					H = del_B - del_G;
				else if ( var_G == var_Max ) 
					H = ( 1 / 3f ) + del_R - del_B;
				else if ( var_B == var_Max ) 
					H = ( 2 / 3f ) + del_G - del_R;

				if ( H < 0 ) H += 1;
				if ( H > 1 ) H -= 1;
			}
			float phi = (float)(Math.toRadians(360*H));
			hsl[0] = (int)(S*Math.cos(phi)*128);
			hsl[1] = (int)(S*Math.sin(phi)*128);
			hsl[2] = (int)(L*255);
		}

		private void rgb2hsl_(int r, int g, int b, int hsl[]) {

			float var_R = ( r / 255f );                     //RGB values = From 0 to 255
			float var_G = ( g / 255f );
			float var_B = ( b / 255f );

			float var_Min;    //Min. value of RGB
			float var_Max;    //Max. value of RGB
			float del_Max;    //Delta RGB value

			if (var_R > var_G) { var_Min = var_G; var_Max = var_R;
			}
			else { var_Min = var_R; var_Max = var_G;
			}
			if (var_B > var_Max) var_Max = var_B;
			if (var_B < var_Min) var_Min = var_B;
			del_Max = var_Max - var_Min; 

			float H = 0, S, L;
			L = ( var_Max + var_Min ) / 2f;

			if ( del_Max == 0 ) { H = 0; S = 0; } // gray
			else {                                   //Chromatic data..{
				if ( L < 0.5 ) 
					S = del_Max / ( var_Max + var_Min );
				else           
					S = del_Max / ( 2 - var_Max - var_Min );

				float del_R = ( ( ( var_Max - var_R ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
				float del_G = ( ( ( var_Max - var_G ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
				float del_B = ( ( ( var_Max - var_B ) / 6f ) + ( del_Max / 2f ) ) / del_Max;

				if ( var_R == var_Max ) 
					H = del_B - del_G;
				else if ( var_G == var_Max ) 
					H = ( 1 / 3f ) + del_R - del_B;
				else if ( var_B == var_Max ) 
					H = ( 2 / 3f ) + del_G - del_R;

				if ( H < 0 ) H += 1;
				if ( H > 1 ) H -= 1;
			}
			hsl[0] = (int)(360*H);
			hsl[1] = (int)(S*100);
			hsl[2] = (int)(L*100);
		}

		private void rgb2hsv(int[] rgb, int vec[]) {
			rgb2hsv(rgb[0], rgb[1], rgb[2], vec);
		}

		private void rgb2hsv(int[] rgb, ColHash cv) {
			rgb2hsv(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		private void rgb2hsv(int r, int g, int b, int hsv[]) {

			int min;    //Min. value of RGB
			int max;    //Max. value of RGB
			int delMax;             //Delta RGB value

			if (r > g) { min = g; max = r; }
			else { min = r; max = g; }
			if (b > max) max = b;
			if (b < min) min = b;

			delMax = max - min; 

			float H = 0, S;
			float V = max;

			if ( delMax == 0 ) { H = 0; S = 0; }
			else {                                   
				S = delMax/255f;

				if ( r == max ) 
					H = (      (g - b)/(float)delMax)*60;
				else if ( g == max ) 
					H = ( 2 +  (b - r)/(float)delMax)*60;
				else if ( b == max ) 
					H = ( 4 +  (r - g)/(float)delMax)*60;	
			}

			float phi = (float)(Math.toRadians(H));
			hsv[0] = (int)(S*Math.cos(phi)*128);
			hsv[1] = (int)(S*Math.sin(phi)*128);
			hsv[2] = (int)(V);
		}

		private void rgb2hsv_(int r, int g, int b, int hsv[]) {

			int min;    //Min. value of RGB
			int max;    //Max. value of RGB
			int delMax;             //Delta RGB value

			if (r > g) { min = g; max = r; }
			else { min = r; max = g; }
			if (b > max) max = b;
			if (b < min) min = b;

			delMax = max - min; 

			float H = 0, S;
			float V = max;

			if ( delMax == 0 ) { H = 0; S = 0; }
			else {                                   
				S = delMax/255f;

				if ( r == max ) 
					H = (      (g - b)/(float)delMax)*60;
				else if ( g == max ) 
					H = ( 2 +  (b - r)/(float)delMax)*60;
				else if ( b == max ) 
					H = ( 4 +  (r - g)/(float)delMax)*60;	
			}
			if (H < 0)
				H += 360;
			hsv[0] = (int)(H);
			hsv[1] = (int)(S*100);
			hsv[2] = (int)((V/255F)*100);
		}

		private void rgb2hmmd_orig(int r, int g, int b, int[] hmmd) {

			float max = (int)Math.max(Math.max(r,g), Math.max(g,b));
			float min = (int)Math.min(Math.min(r,g), Math.min(g,b));
			float diff = (max - min);
			float sum = (float) ((max + min)/2.);

			float hue = 0;
			if (diff == 0)
				hue = 0;
			else if (r == max && (g - b) > 0)
				hue = 60*(g-b)/(max-min);
			else if (r == max && (g - b) <= 0)
				hue = 60*(g-b)/(max-min) + 360;
			else if (g == max)
				hue = (float) (60*(2.+(b-r)/(max-min)));
			else if (b == max)
				hue = (float) (60*(4.+(r-g)/(max-min)));

			diff /= 2;
			float phi = (float)(Math.toRadians(hue));
			hmmd[0] = (int)(diff*Math.cos(phi));
			hmmd[1] = (int)(diff*Math.sin(phi));
			hmmd[2] = (int)(sum);
		}

		private void rgb2hmmd(int r, int g, int b, int[] hmmd) {

			float max = (int)Math.max(Math.max(r,g), Math.max(g,b));
			float min = (int)Math.min(Math.min(r,g), Math.min(g,b));
			float diff = (max - min);
			float sum = (float) ((max + min)/2.);

			float hue = 0;

			if(max!=min){
				float temp=(float) (60./diff);

				if(r==max){
					if(g>=b)
						// g-b cannot underflow in T
						hue=(float)(g-b)*temp;
					else
						// b-g cannot underflow in T
						hue=(float) ((-((b-g))*temp)+360.);
				}
				else if(g==max)
					// (b-r) can underflow in T
					hue=(float) (120.+(b-r)*temp);
				else // note this has to be true: if(b==max)
					// (r-g) can underflow in T
					hue=(float) (240.+(r-g)*temp);

			}
			// the improvement:
			// note that temp, the arg for the cosine below, is
			// [0,120[ for every third of the hue circle
			// note that the added term below is 0,120 or 240 for 
			// each third of the hue circle, respectively
			// note also that for 120,240 and 360(==0) this improvement is NOP,
			// i.e. these values stay as they are, which can be used for the inverse
			float temp=(float) (((int)hue%120)+(hue-Math.floor(hue)));
			// note temp has to be in rad...
			temp = (float) Math.toRadians(temp);
			hue=(float) (60.*(1.-Math.cos(1.5*temp))+120.*Math.floor(hue/120.));





			diff /= 2;
			float phi = (float)(Math.toRadians(hue));
			hmmd[0] = (int)(diff*Math.cos(phi));
			hmmd[1] = (int)(diff*Math.sin(phi));
			hmmd[2] = (int)(sum);
		}

		private void rgb2hmmd_(int r, int g, int b, int[] hmmd) {

			float max = (int)Math.max(Math.max(r,g), Math.max(g,b));
			float min = (int)Math.min(Math.min(r,g), Math.min(g,b));
			float diff = (max - min);
//			float sum = (float) ((max + min)/2.);

			float hue = 0;
			if (diff == 0)
				hue = 0;
			else if (r == max && (g - b) > 0)
				hue = 60*(g-b)/(max-min);
			else if (r == max && (g - b) <= 0)
				hue = 60*(g-b)/(max-min) + 360;
			else if (g == max)
				hue = (float) (60*(2.+(b-r)/(max-min)));
			else if (b == max)
				hue = (float) (60*(4.+(r-g)/(max-min)));

			diff /= 2;
			hmmd[0] = (int)(hue);
			hmmd[1] = (int)(max);
			hmmd[2] = (int)(min);
			hmmd[3] = (int)(diff);
		}

		private void rgb2xyy(int[] rgb, ColHash cv) {
			rgb2xyy(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		public void rgb2xyy(int R, int G, int B, int []xyy) {
			//http://www.brucelindbloom.com

//			float rf, gf, bf;
			float r, g, b, X, Y, Z;

			// RGB to XYZ
			r = R/255.f; //R 0..1
			g = G/255.f; //G 0..1
			b = B/255.f; //B 0..1

			if (r <= 0.04045)
				r = r/12;
			else
				r = (float) Math.pow((r+0.055)/1.055,2.4);

			if (g <= 0.04045)
				g = g/12;
			else
				g = (float) Math.pow((g+0.055)/1.055,2.4);

			if (b <= 0.04045)
				b = b/12;
			else
				b = (float) Math.pow((b+0.055)/1.055,2.4);

			X =  0.436052025f*r	+ 0.385081593f*g + 0.143087414f *b;
			Y =  0.222491598f*r	+ 0.71688606f *g + 0.060621486f *b;
			Z =  0.013929122f*r	+ 0.097097002f*g + 0.71418547f  *b;

			float x;
			float y;

			float sum = X + Y + Z;
			if (sum != 0) {
				x = X / sum;
				y = Y / sum;
			}
			else {
				float Xr = 0.964221f;  // reference white
				float Yr = 1.0f;
				float Zr = 0.825211f;

				x = Xr / (Xr + Yr + Zr);
				y = Yr / (Xr + Yr + Zr);
			}

			xyy[2] = (int) (255*Y + .5);
			xyy[0] = (int) (255*x + .5); 
			xyy[1] = (int) (255*y + .5);	
		} 

		private void rgb2xyz(int[] rgb, ColHash cv) {
			rgb2xyz(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		public void rgb2xyz(int R, int G, int B, int []xyz) {
			float r, g, b, X, Y, Z;

			r = R/255.f; //R 0..1
			g = G/255.f; //G 0..1
			b = B/255.f; //B 0..1

			if (r <= 0.04045)
				r = r/12;
			else
				r = (float) Math.pow((r+0.055)/1.055,2.4);

			if (g <= 0.04045)
				g = g/12;
			else
				g = (float) Math.pow((g+0.055)/1.055,2.4);

			if (b <= 0.04045)
				b = b/12;
			else
				b = (float) Math.pow((b+0.055)/1.055,2.4);

			X =  0.436052025f*r	+ 0.385081593f*g + 0.143087414f *b;
			Y =  0.222491598f*r	+ 0.71688606f *g + 0.060621486f *b;
			Z =  0.013929122f*r	+ 0.097097002f*g + 0.71418547f  *b;

			xyz[2] = (int) (255*Y + .5);
			xyz[0] = (int) (255*X + .5); 
			xyz[1] = (int) (255*Z + .5);	
		} 

		private void rgb2lab(int[] rgb, int[] vec) {
			rgb2lab(rgb[0], rgb[1], rgb[2], vec);
		}
		private void rgb2luv(int[] rgb, int[] vec) {
			rgb2luv(rgb[0], rgb[1], rgb[2], vec);
		}

		private void rgb2lab(int[] rgb, ColHash cv) {
			rgb2lab(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}

		public void rgb2lab_alt(int R, int G, int B, int []lab) {
			//http://www.brucelindbloom.com

			float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
			float Ls, as, bs;
			float eps = 216.f/24389.f;
			float k = 24389.f/27.f;

			float Xr = 0.964221f;  // reference white D50
			float Yr = 1.0f;
			float Zr = 0.825211f;

			// RGB to XYZ
			r = R/255.f; //R 0..1
			g = G/255.f; //G 0..1
			b = B/255.f; //B 0..1

			// assuming sRGB (D65)
			if (r <= 0.04045)
				r = r/12;
			else
				r = (float) Math.pow((r+0.055)/1.055,2.4);

			if (g <= 0.04045)
				g = g/12;
			else
				g = (float) Math.pow((g+0.055)/1.055,2.4);

			if (b <= 0.04045)
				b = b/12;
			else
				b = (float) Math.pow((b+0.055)/1.055,2.4);

			/*
			 X_ =  0.412424f * r + 0.357579f * g + 0.180464f  * b;
			 Y_ =  0.212656f * r + 0.715158f * g + 0.0721856f * b;
			 Z_ = 0.0193324f * r + 0.119193f * g + 0.950444f  * b;

			 // chromatic adaptation transform from D65 to D50
			  X =  1.047835f * X_ + 0.022897f * Y_ - 0.050147f * Z_;
			  Y =  0.029556f * X_ + 0.990481f * Y_ - 0.017056f * Z_;
			  Z = -0.009238f * X_ + 0.015050f * Y_ + 0.752034f * Z_;
			 */

			X =  0.436052025f*r	+ 0.385081593f*g + 0.143087414f *b;
			Y =  0.222491598f*r	+ 0.71688606f *g + 0.060621486f *b;
			Z =  0.013929122f*r	+ 0.097097002f*g + 0.71418547f  *b;

			// XYZ to Lab
			xr = X/Xr;
			yr = Y/Yr;
			zr = Z/Zr;

			if ( xr > eps )
				fx =  (float) Math.pow(xr, 1/3.);
			else
				fx = (float) ((k * xr + 16.) / 116.);

			if ( yr > eps )
				fy =  (float) Math.pow(yr, 1/3.);
			else
				fy = (float) ((k * yr + 16.) / 116.);

			if ( zr > eps )
				fz =  (float) Math.pow(zr, 1/3.);
			else
				fz = (float) ((k * zr + 16.) / 116);


			Ls = ( 116 * fy ) - 16;
			as = 500*(fx-fy);
			bs = 200*(fy-fz);

			lab[2] = (int) (2.55*Ls + .5);
			lab[0] = (int) (as + .5); 
			lab[1] = (int) (bs + .5);	
		} 

		public void rgb2lab(int R, int G, int B, int []lab) {
			//http://www.brucelindbloom.com

			double r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
			double Ls, as, bs;
			double eps = 216./24389.;
			double k = 24389./27.;

//			double Xr = 0.964221;  // reference white D50
//			double Yr = 1.0;
//			double Zr = 0.825211;

			double divXr=1./0.964221;
			double divZr=1./0.825211;
			double divmax = 1./255;
			double div116=1./116.;

			// RGB to XYZ
			r = R*divmax; //R 0..1
			g = G*divmax; //G 0..1
			b = B*divmax; //B 0..1

			// assuming sRGB (D65)
			if (r <= 0.04045)
				r = r/12.92;
			else
				r = (float) Math.pow((r+0.055)/1.055,2.4);

			if (g <= 0.04045)
				g = g/12.92;
			else
				g = (float) Math.pow((g+0.055)/1.055,2.4);

			if (b <= 0.04045)
				b = b/12.92;
			else
				b = (float) Math.pow((b+0.055)/1.055,2.4);

			/*
			 X_ =  0.412424f * r + 0.357579f * g + 0.180464f  * b;
			 Y_ =  0.212656f * r + 0.715158f * g + 0.0721856f * b;
			 Z_ = 0.0193324f * r + 0.119193f * g + 0.950444f  * b;

			 // chromatic adaptation transform from D65 to D50
			  X =  1.047835f * X_ + 0.022897f * Y_ - 0.050147f * Z_;
			  Y =  0.029556f * X_ + 0.990481f * Y_ - 0.017056f * Z_;
			  Z = -0.009238f * X_ + 0.015050f * Y_ + 0.752034f * Z_;
			 */

			X =  0.436052025f*r	+ 0.385081593f*g + 0.143087414f *b;
			Y =  0.222491598f*r	+ 0.71688606f *g + 0.060621486f *b;
			Z =  0.013929122f*r	+ 0.097097002f*g + 0.71418547f  *b;

			// XYZ to Lab
			xr = X*divXr;
			yr = Y;
			zr = Z*divZr;

			if ( xr > eps )
				fx =  Math.pow(xr, 1/3.);
			else
				fx = ((k * xr + 16.)*div116);

			if ( yr > eps )
				fy =  Math.pow(yr, 1/3.);
			else
				fy = ((k * yr + 16.)*div116);

			if ( zr > eps )
				fz =  Math.pow(zr, 1/3.);
			else
				fz = ((k * zr + 16.)*div116);


			Ls = (116 * fy) - 16;
			as = 500*(fx-fy);
			bs = 200*(fy-fz);

			lab[2] = (int) (2.55*Ls + .5);
			lab[0] = (int) (as + .5); 
			lab[1] = (int) (bs + .5);	
		} 

		private void lab2rgb(int[] vec, int rgb[]) {

			double r,g,b;
			double X,Y,Z;
			double L,a_,b_;
			double xr,yr,zr;
			double fx,fy,fz;
			double temp;

			// all matrices, formulas etc. used here are from www.brucelindbloom.com
			final double eps=216./24389.;
			final double k=24389./27.;
			// k*eps:
			final double keps=8.; // 216./27.;	

			final double f1=100./255.;
//			final double sub=(double)(255./2+1);

			// XYZ reference white D50:
			final double Xr=0.964221;  
			// not Yr==1 for D50...
			// const double Yr=1.0;
			final double Zr=0.825211;

			// note multiplication is faster than division:
			final double div2p4=1./2.4;
			final double div116=1./116.;
			final double div200=1./200.;
			final double div500=1./500.;
			final double divk=27./24389.;

			final double dmax=255;

			L  = vec[2]*f1; 
			a_ = vec[0];
			b_ = vec[1];

			// LAB to XYZ:
			if(L>keps)
				yr=Math.pow((L+16.)*div116,3.);
			else
				yr=L*divk;

			if(yr>eps)
				fy=(L+16.)*div116;
			else
				fy=(k*yr+16.)*div116;

			fx=(a_*div500)+fy;
			fz=fy-(b_*div200);

			if((temp=Math.pow(fx,3.))>eps)
				xr=temp;
			else
				xr=(116.*fx-16.)*divk;

			if((temp=Math.pow(fz,3.))>eps)
				zr=temp;
			else
				zr=(116.*fz-16.)*divk;


			X=xr*Xr;
			Y=yr;// *Yr;
			Z=zr*Zr;

			// inverse:
			r=3.134051341*X+(-1.617027711)*Y+(-0.49065221)*Z;
			g=(-0.97876273)*X+1.916142228*Y+(0.033449629)*Z;
			b=(0.071942577 )*X+(-0.22897118)*Y+1.405218305 *Z;

			if(r<=.0031308)
				r*=12.92;
			else
				r=Math.pow(1.055*r,div2p4)-0.055;

			if(g<=.0031308)
				g*=12.92;
			else
				g=Math.pow(1.055*g,div2p4)-0.055;

			if(b<=.0031308)
				b*=12.92;
			else
				b=Math.pow(1.055*b,div2p4)-0.055;

			rgb[0] = (int)(dmax*r+0.5);
			rgb[1] = (int)(dmax*g+0.5);
			rgb[2] = (int)(dmax*b+0.5);
		}

		private void luv2rgb(int[] vec, int rgb[]) {

			double r,g,b;
			double X,Y,Z;
			double L,u,v;
			double a_,b_,d_;
			double tL;

			// all matrices, formulas etc. used here are from www.brucelindbloom.com
			// const double eps=216./24389.;
			// const double k=24389./27.;
			double keps=8; // k*eps

			// XYZ reference white D50:
			double Xr=0.964221;  
			// not Yr==1 for D50...
			// const double Yr=1.0;
			double Zr=0.825211;
			double f1=100./255.;

			double u0t13=13.*((4.*Xr)/(Xr+15.+3.*Zr));	// Yr==1
			double v0t13=13.*((9.)/(Xr+15.+3.*Zr));		// Yr==1
			double div3=1./3.;

			// note multiplication is faster than division:
			double div116=1./116.;
			double divk=27./24389.;
			double div2p4=1./2.4;
			double dmax=255;
			//double sub=maxChannelValue()/2+1;

			L=tL=vec[2]*f1;
			u=vec[0];
			v=vec[1];

			// LAB to XYZ:
			if(tL>keps)
				Y=Math.pow((L+16.)*div116,3.);
			else
				Y=L*divk;

			a_=div3*((52.*L/(u+L*u0t13))-1.);
			b_=-5.*Y;
			d_=Y*((39.*L/(v+L*v0t13))-5.);

			X=(d_-b_)/(a_+div3);
			Z=X*a_+b_;

			// chromatic adaption to d65 ref white:

			// X_d65=.955556*X+(-.028302)*Y+.012305*Z;
			// Y_d65=(-.023049)*X+1.009944*Y+(-.020494)*Z;
			// Z_d65=.063197*X+.021018*Y+1.330084*Z;

			// convert to rgb
			// r=3.24071*X_d65+(-1.53726)*Y_d65+(-0.498571)*Z_d65;
			// g=(-0.969258)*X_d65+1.87599*Y_d65+(0.0415557)*Z_d65;
			// b=(0.0556352)*X_d65+(-0.203996)*Y_d65+1.05707*Z_d65;

			// both transforms at once:

			// note this is propably not right: 
			// we may better use the inverse of RGB2LAB()
			// because RGB2LAB() followed by LAB2RGB() is not a nop, currently
			// r=3.100603999*X+(-1.654744053)*Y+(-0.591759767)*Z;
			// g=(-0.966793795)*X+1.922950202*Y+(0.004899313)*Z;
			// b=(0.124668106)*X+(-0.185381626)*Y+1.410857179*Z;

			// inverse:
			r=3.134051341*X+(-1.617027711)*Y+(-0.49065221)*Z;
			g=(-0.97876273)*X+1.916142228*Y+(0.033449629)*Z;
			b=(0.071942577 )*X+(-0.22897118)*Y+1.405218305 *Z;

			// there are different RGB color spaces
			// for most of these, we have to compute
			// r=r^(1/G) for the transform, G being the gamma 
			// of the RGB colorspace.

			// anyway, we don't know which RGB color space to use (why not?)
			// thus, we assume sRGB which is widely used in the win domain
			// for sRGB, gamma is approx. 2.2 and using the formula above is called
			// 'simplified sRGB', because the real formula is different: 
			if(r<=.0031308)
				r*=12.92;
			else
				r=Math.pow(1.055*r,div2p4)-0.055;

			if(g<=.0031308)
				g*=12.92;
			else
				g=Math.pow(1.055*g,div2p4)-0.055;

			if(b<=.0031308)
				b*=12.92;
			else
				b=Math.pow(1.055*b,div2p4)-0.055;

			// transform r,g,b from [0,1] to [0,maxChannelValue()]
			// take care: r, g, b may exceed [0,1] now, this may be true
			// if we do RGB2LUV() followed by LUV2RGB() due to cutting dezimals
			// when converting from double to type T
			// thus:
			rgb[0] =(int)(Math.max(0.,Math.min(255.,dmax*r))+.5);
			rgb[1] =(int)(Math.max(0.,Math.min(255.,dmax*g))+.5);
			rgb[2] =(int)(Math.max(0.,Math.min(255.,dmax*b))+.5);

		}



		private void rgb2luv(int[] rgb, ColHash cv) {
			rgb2luv(rgb[0], rgb[1], rgb[2], vec);
			cv.R = vec[0];
			cv.G = vec[1];
			cv.B = vec[2];
		}
		public void rgb2luv(int R, int G, int B, int []luv) {
			//http://www.brucelindbloom.com

			double r, g, b, X, Y, Z, yr;
			double L;
			double eps = 216./24389.;
			double k = 24389./27.;

			double Xr = 0.964221;  // reference white D50
			double Yr = 1.0;
			double Zr = 0.825211;


//			double ur=4.*.964221/(.964221+15.+3.*.825211);
//			// vr=9*Yr/(Xr+15*Yr+3*Zr)
//			double vr=9./(.964221+15.+3.*.825211);

//			double dmax=(double)255;
//			double dmaxdiv2=(double)(255./2.+1);

			// multiplication is faster than division
			double div12p92=1./12.92;
			double div1p055=1./1.055;
//			double aThird=1./3.;
			double divmax=1./(double)255;

			// RGB to XYZ

			r = R*divmax; //R 0..1
			g = G*divmax; //G 0..1
			b = B*divmax; //B 0..1

			// assuming sRGB (D65)
			if (r <= 0.04045)
				r *=div12p92;
			else
				r = (float) Math.pow((r+0.055)*div1p055,2.4);

			if (g <= 0.04045)
				g *=div12p92;
			else
				g = (float) Math.pow((g+0.055)*div1p055,2.4);

			if (b <= 0.04045)
				b *=div12p92;
			else
				b = (float) Math.pow((b+0.055)*div1p055,2.4);

			/*
			 X_ =  0.412424f * r + 0.357579f * g + 0.180464f  * b;
			 Y_ =  0.212656f * r + 0.715158f * g + 0.0721856f * b;
			 Z_ = 0.0193324f * r + 0.119193f * g + 0.950444f  * b;

			 // chromatic adaptation transform from D65 to D50
			  X =  1.047835f * X_ + 0.022897f * Y_ - 0.050147f * Z_;
			  Y =  0.029556f * X_ + 0.990481f * Y_ - 0.017056f * Z_;
			  Z = -0.009238f * X_ + 0.015050f * Y_ + 0.752034f * Z_;
			 */

			X =  0.436052025f*r	+ 0.385081593f*g + 0.143087414f *b;
			Y =  0.222491598f*r	+ 0.71688606f *g + 0.060621486f *b;
			Z =  0.013929122f*r	+ 0.097097002f*g + 0.71418547f  *b;

			// XYZ to Luv

			double u, v, u_, v_, ur_, vr_;

			u_ = 4*X / (X + 15*Y + 3*Z);
			v_ = 9*Y / (X + 15*Y + 3*Z);

			ur_ = 4*Xr / (Xr + 15*Yr + 3*Zr);
			vr_ = 9*Yr / (Xr + 15*Yr + 3*Zr);

			yr = Y/Yr;

			if ( yr > eps )
				L =  (float) (116*Math.pow(yr, 1/3.) - 16);
			else
				L = k * yr;

			u = 13*L*(u_ -ur_);
			v = 13*L*(v_ -vr_);

			luv[2] = (int) (2.55*L + .5);
			luv[0] = (int) (u + .5); 
			luv[1] = (int) (v + .5);	
		} 


		synchronized public void computeColorSpaceCoordinates() {
			int rgb[] = {0,0,0};
			ColHash cv;
			
			if (colorSpace == KLT) {
				if (kltColor == null) {
					kltColor = new ColorKLT(pic1.pixels);
					kltMatrix = kltColor.klt.m;
					kltMean = kltColor.mean;
				}
			}
			
			for (int i = 0; i < colHash.length; i++ ){
				cv = colHash[i];

				if (cv != null) {
					int c = cv.color;
					rgb[0] = ((c >> 16) & 255);
					rgb[1] = ((c >>  8) & 255);
					rgb[2] = ((c      ) & 255);

					switch (colorSpace) {
					case RGB:
						cv.R = rgb[0] - 128;
						cv.G = rgb[1] - 128;
						cv.B = rgb[2] - 128;
						break;
					case KLT:
						rgb2klt(rgb, cv);
						break;
					case YUV:	 
						rgb2yuv(rgb, cv); 
						cv.B -= 128;
						break;
					case YCBCR:  
						rgb2ycbcr(rgb, cv); 
						cv.B -= 128;
						break;
					case YIQ:  
						rgb2yiq(rgb, cv); 
						cv.B -= 128;
						break;
					case HSB: 
						rgb2hsb(rgb, cv); 
						cv.B -= 128;
						break;
					case HSV: 
						rgb2hsv(rgb, cv); 
						cv.B -= 128;
						break;
					case HSL: 
						rgb2hsl(rgb, cv); 
						cv.B -= 128;
						break;
					case HMMD:
						rgb2hmmd(rgb, cv); 
						cv.B -= 128;
						break;
					case LAB:
						rgb2lab(rgb, cv);
						cv.B -= 128;
						break;
					case LUV: 
						rgb2luv(rgb, cv); 
						cv.B -= 128;
						break;
					case XYY: 
						rgb2xyy(rgb, cv); 
						cv.R -= 128;
						cv.G -= 128;
						cv.B -= 128;
						break;
					case XYZ: 
						rgb2xyz(rgb, cv); 
						cv.R -= 128;
						cv.G -= 128;
						cv.B -= 128;
						break;
//						case HCL: 
//						rgb2hcl(rgb, cv); 
//						//cv.R -= 128;
//						//cv.G -= 128;
//						cv.B -= 128;
//						break;

					}
				}
			}
		}

		synchronized public void convertPixelColors(int[] pixelsSrc, int[] pixelsDst) {
			int rgb[] = {0,0,0};
			int vec[] = {0,0,0};
			
			if (colorSpace == KLT) {
				if (kltColor == null) {
					kltColor = new ColorKLT(pic1.pixels);
					kltMatrix = kltColor.klt.m;
					kltMean = kltColor.mean;
				}
			}
			
			for (int i = 0; i < pixels.length; i++ ){
				int c = pixelsSrc[i];

				if ((c & OPAQUE) == OPAQUE) {
					rgb[0] = ((c >> 16) & 255);
					rgb[1] = ((c >>  8) & 255);
					rgb[2] = ((c      ) & 255);

					switch (colorSpace) {
					case RGB:
						vec[0] = rgb[0];
						vec[1] = rgb[1];
						vec[2] = rgb[2];
						break;
					case YUV:	 
						rgb2yuv(rgb, vec); 
						vec[0] += 128;
						vec[1] += 128;
						break;
					case KLT:	 
						rgb2klt(rgb, vec); 
						vec[0] += 128;
						vec[1] += 128;
						vec[2] += 128;
						break;
					case YCBCR:  
						rgb2ycbcr(rgb, vec); 
						vec[0] += 128;
						vec[1] += 128;
						break;
					case YIQ:  
						rgb2yiq(rgb, vec); 
						vec[0] += 128;
						vec[1] += 128;
						break;
					case HSB: 
						rgb2hsb(rgb, vec); 
						break;
					case HSV: 
						rgb2hsv(rgb, vec);
						break;
					case HSL: 
						rgb2hsl(rgb, vec); 
						break;
					case HMMD:
						//rgb2hmmd(rgb, vec);

						break;
					case LAB:
						rgb2lab(rgb, vec);
						vec[0] += 128;
						vec[1] += 128;
						//vec[2] *= 100./255;
						break;
					case LUV:
						rgb2luv(rgb, vec);
						vec[0] += 128;
						vec[1] += 128;
						//vec[2] *= 100./255;
						break;
//						case XYY: 
//						rgb2xyy(rgb, cv); 
//						cv.R -= 128;
//						cv.G -= 128;
//						cv.B -= 128;
//						break;
//						case XYZ: 
//						rgb2xyz(rgb, cv); 
//						cv.R -= 128;
//						cv.G -= 128;
//						cv.B -= 128;
//						break;
					}
					int r = vec[0];
					int g = vec[1];
					int b = vec[2];
					r = (r>255) ? 255 : ((r < 0) ? 0 : r); 
					g = (g>255) ? 255 : ((g < 0) ? 0 : g); 
					b = (b>255) ? 255 : ((b < 0) ? 0 : b); 
					pixelsDst[i] = (0xFF<<24) | (r << 16) | (g << 8) | b; 
				}
				else
					pixelsDst[i] = c;
			}
		}



		synchronized public void convertColor(int c, int[] vec) {
			int rgb[] = {0,0,0};
			ColHash cv = new ColHash();
			

			rgb[0] = ((c >> 16) & 255);
			rgb[1] = ((c >>  8) & 255);
			rgb[2] = ((c      ) & 255);

			switch (colorSpace) {
			case RGB:
				cv.R = rgb[0];
				cv.G = rgb[1];
				cv.B = rgb[2];
				break;
			case YUV:	 
				rgb2yuv(rgb, cv); 
				break;
			case KLT:	 
				rgb2klt(rgb, cv); 
				break;
			case YCBCR:  
				rgb2ycbcr(rgb, cv); 
				break;
			case YIQ:  
				rgb2yiq(rgb, cv); 
				break;
			case HSB: 
				rgb2hsb_(rgb, cv); 
				break;
			case HSV: 
				rgb2hsv(rgb, cv); 
				break;
			case HSL: 
				rgb2hsl(rgb, cv); 
				break;
			case HMMD:
				rgb2hmmd(rgb, cv); 
				break;
			case LAB:
				rgb2lab(rgb, cv);
				break;
			case LUV: 
				rgb2luv(rgb, cv); 
				break;
			case XYY: 
				rgb2xyy(rgb, cv); 
				break;
			case XYZ: 
				rgb2xyz(rgb, cv); 
				break;
			}
			vec[0] = cv.R;
			vec[1] = cv.G;
			vec[2] = cv.B;
		}



		synchronized public void convertPixelColorsBack(int[] pixelsSrc, int[] pixelsDst) {
			int rgb[] = {0,0,0};
			int vec[] = {0,0,0};
			
			if (colorSpace == KLT) {
				if (kltColor == null) {
					kltColor = new ColorKLT(pic1.pixels);
					kltMatrix = kltColor.klt.m;
					kltMean = kltColor.mean;
				}
			}

			for (int i = 0; i < pixels.length; i++ ){
				int c = pixelsSrc[i];

				if ((c & OPAQUE) == OPAQUE) {
					vec[0] = ((c >> 16) & 255);
					vec[1] = ((c >>  8) & 255);
					vec[2] = ((c      ) & 255);

					switch (colorSpace) {
					case RGB:
						rgb[0] = vec[0];
						rgb[1] = vec[1];
						rgb[2] = vec[2];
						break;
					case YUV:
						vec[0] -= 128;
						vec[1] -= 128;
						yuv2rgb(vec, rgb); 
						break;
					case KLT:
						vec[0] -= 128;
						vec[1] -= 128;
						vec[2] -= 128;
						klt2rgb(vec, rgb); 
						break;
					case YCBCR:  
						vec[0] -= 128;
						vec[1] -= 128;
						ycbcr2rgb(vec, rgb); 
						break;
					case YIQ:  
						vec[0] -= 128;
						vec[1] -= 128;
						yiq2rgb(vec, rgb); 
						break;
						//					case HSB: 
							//						rgb2hsb(rgb, vec); 
							//						cv.B -= 128;
						//						break;
						//						case HSV: 
						//						rgb2hsv(rgb, cv); 
						//						cv.B -= 128;
						//						break;
						//						case HSL: 
						//						rgb2hsl(rgb, cv); 
						//						cv.B -= 128;
						//						break;
						//						case HMMD:
						//						rgb2hmmd(rgb, cv); 
						//						cv.B -= 128;
						//						break;
					case LAB:
						vec[0] -= 128;
						vec[1] -= 128;
						//vec[2] *= 255./100.;
						lab2rgb(vec, rgb);
						break;
					case LUV: 
						vec[0] -= 128;
						vec[1] -= 128;
						//vec[2] *= 255./100.;
						luv2rgb(vec, rgb);
						break;
						//						case XYY: 
						//						rgb2xyy(rgb, cv); 
						//						cv.R -= 128;
						//						cv.G -= 128;
						//						cv.B -= 128;
						//						break;
						//						case XYZ: 
						//						rgb2xyz(rgb, cv); 
						//						cv.R -= 128;
						//						cv.G -= 128;
						//						cv.B -= 128;
						//						break;
					}
					int r = rgb[0];
					int g = rgb[1];
					int b = rgb[2];
					r = (r>255) ? 255 : ((r < 0) ? 0 : r); 
					g = (g>255) ? 255 : ((g < 0) ? 0 : g); 
					b = (b>255) ? 255 : ((b < 0) ? 0 : b); 
					pixelsDst[i] = (0xFF << 24) | (r << 16) | (g << 8) | b; 
				}
			}
		}



		synchronized public void convertColorBack(int[] vec, int[] rgb) {

			switch (colorSpace) {
			case RGB:
				vec[0] *= channelFactor[0];
				vec[1] *= channelFactor[1];
				vec[2] *= channelFactor[2];
				rgb[0] = vec[0];
				rgb[1] = vec[1];
				rgb[2] = vec[2];
				break;
			case YUV:
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] =  (int) ((vec[2]-128)*channelFactor[0]+128);
				yuv2rgb(vec, rgb);
				break;
			case KLT:
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] *= channelFactor[0];
				klt2rgb(vec, rgb);

				break;
			case YCBCR:
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] =  (int) ((vec[2]-128)*channelFactor[0]+128);
				ycbcr2rgb(vec, rgb); 
				break;
			case YIQ:
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] =  (int) ((vec[2]-128)*channelFactor[0]+128);
				yiq2rgb(vec, rgb); 
				break;
			case HSB: 
				hsb2rgb_(vec, rgb); 
				break;
//				case HSV: 
//				rgb2hsv(rgb, cv); 
//				cv.B -= 128;
//				break;
//				case HSL: 
//				rgb2hsl(rgb, cv); 
//				cv.B -= 128;
//				break;
//				case HMMD:
//				rgb2hmmd(rgb, cv); 
//				cv.B -= 128;
//				break;
			case LAB:
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] =  (int) ((vec[2]-128)*channelFactor[0]+128);
				lab2rgb(vec, rgb);
				break;
			case LUV: 
				vec[0] *= channelFactor[1];
				vec[1] *= channelFactor[2];
				vec[2] =  (int) ((vec[2]-128)*channelFactor[0]+128);
				luv2rgb(vec, rgb);
				break;
//					case XYY: 
//					rgb2xyy(rgb, cv); 
//					cv.R -= 128;
//					cv.G -= 128;
//					cv.B -= 128;
//					break;
//					case XYZ: 
//					rgb2xyz(rgb, cv); 
//					cv.R -= 128;
//					cv.G -= 128;
//					cv.B -= 128;
//					break;
			}
		}


		synchronized public void checkDepth(boolean isOrig) {

			if (isOrig) {
				for (int i=0; i<pixelsOverlay.length; i++){
					pixelsOverlay[i] = 0;
				}
			}
			else {
				for (int i=0; i<pixels.length; i++){
					int c = pixels[i];

					if ( (c & OPAQUE) == OPAQUE) {

						int hash = (((c & 0xFFffFF) * HASH_F) % hashSize); // hash value

						while (colHash[hash] != null && colHash[hash].color != c ){
							hash = (hash + HASH_P) % hashSize;	
						}
						try {
							R = colHash[hash].R;
							G = colHash[hash].G;
							B = colHash[hash].B;
						} catch (RuntimeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						xyzPosC();

						if (Z >= renderDepth)
							pixelsOverlay[i] = depthColorFar;
						else
							pixelsOverlay[i] = depthColorNear;
					}
				}
			}
			image = Toolkit.getDefaultToolkit().createImage(memoryOverlayImageSource);
			imageRegion.setOverlayImage(image);
		}

		synchronized public void quantizeMedianCut(int numCols) {

			// convert RGB colors to display color space
			int [] pixelsTmp = new int [width*height];
			int cs = colorSpace;
			if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
				convertPixelColors(pixels, pixelsTmp);
			else {
				for (int i=0; i<pixels.length; i++)
					pixelsTmp[i] = pixels[i];
			}
			Median_Cut mc = new Median_Cut(pixelsTmp, width, height);
			Image image = mc.convert(numCols);

			PixelGrabber pg =  new PixelGrabber(image, 0, 0, width, height, pixelsTmp, 0, width);

			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {}

			// convert colors back to RGB
			if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
				convertPixelColorsBack(pixelsTmp, pixels);
			else {
				for (int i=0; i<pixels.length; i++)
					pixels[i] = pixelsTmp[i];
			}	

			for (int i=0; i<pixels.length; i++) {
				int c  = pixelsOrig[i];

				if ( (c & OPAQUE) != OPAQUE ) { // opaque
					pixels[i] = pixelsOrig[i];
				}
				else {
					c = pixels[i];
				}	
			}
		}

		synchronized public void wu_quant(int numCols) {

			// convert RGB colors to display color space
			int [] pixelsTmp = new int [width*height];
			// inverse conversion does not work for these color spaces yet
			int cs = colorSpace;
			if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
				convertPixelColors(pixels, pixelsTmp);
			else {
				for (int i=0; i<pixels.length; i++)
					pixelsTmp[i] = pixels[i];
			}
			WuCq wq = new WuCq (pixelsTmp, width*height, numCols);
			wq.main_();

			// convert colors back to RGB
			if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
				convertPixelColorsBack(pixelsTmp, pixels);
			else {
				for (int i=0; i<pixels.length; i++)
					pixels[i] = pixelsTmp[i];
			}
			for (int i=0; i<pixels.length; i++) {
				int c  = pixelsOrig[i];

				if ( (c & OPAQUE) != OPAQUE ) { // opaque
					pixels[i] = c;
				}
			}
		}

		public void copyOrigPixels() {
			for (int i=0; i<pixels.length; i++)
				pixels[i] = pixelsOrig[i];
		}

		synchronized public void quantize() {
			changeColorHSB();
			selectChannels();
			if (numberOfColorsOrig > 256 ) { // do not do any quantization for palletized images


				// convert RGB colors to display color space
				int [] pixelsTmp = new int [width*height];
				int cs = colorSpace;
				// inverse conversion does not work for these color spaces yet
				if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
					convertPixelColors(pixels, pixelsTmp);
				else {
					for (int i=0; i<pixels.length; i++)
						pixelsTmp[i] = pixels[i];
				}


				// perform quantization
				for (int i=0; i<pixelsTmp.length; i++){
					int c = pixelsTmp[i];

					if ( (c & OPAQUE) == OPAQUE) {
						int rq = quant( (c >> 16) & 255);
						int gq = quant( (c >>  8) & 255);
						int bq = quant( (c      ) & 255);

						c = OPAQUE | (rq <<16) | (gq << 8) | bq;
					}
					pixelsTmp[i] = c;

				}
				// convert colors back to RGB
				if (cs!=HSB && cs!=HSV && cs!=HSL && cs!=HMMD && cs!=XYY && cs!=XYZ)
					convertPixelColorsBack(pixelsTmp, pixels);
				else {
					for (int i=0; i<pixels.length; i++)
						pixels[i] = pixelsTmp[i];
				}	
			}			
		}

		synchronized void selectChannels() {

			if (channelFactor[0] == 1 && channelFactor[1] == 1 && channelFactor[2] == 1)
				return;
			
			int[] vec = new int[3];
			int[] rgb = new int[3];

			for (int i=0; i<pixels.length; i++){
				int c = pixels[i];

				if ( (c & OPAQUE) == OPAQUE) {

					convertColor(c,vec);

					convertColorBack(vec,rgb);

					int r = rgb[0];
					int g = rgb[1];
					int b = rgb[2];
					r = (r>255) ? 255 : ((r < 0) ? 0 : r); 
					g = (g>255) ? 255 : ((g < 0) ? 0 : g); 
					b = (b>255) ? 255 : ((b < 0) ? 0 : b); 

					c = (0xFF<<24) | (r <<16) | (g<<8) | b;

					pixels[i] = c;
				}
			}
			image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
			imageRegion.setImage(image);
		}

		synchronized void selectChannelsOrig() {

			if (channelFactor[0] == 1 && channelFactor[1] == 1 && channelFactor[2] == 1)
				return;
			
			int[] vec = new int[3];
			int[] rgb = new int[3];


			for (int i=0; i<pixels.length; i++){
				int c = pixels[i];

				if ( (c & OPAQUE) == OPAQUE) {

					convertColor(c,vec);

					convertColorBack(vec,rgb);

					int r = rgb[0];
					int g = rgb[1];
					int b = rgb[2];
					r = (r>255) ? 255 : ((r < 0) ? 0 : r); 
					g = (g>255) ? 255 : ((g < 0) ? 0 : g); 
					b = (b>255) ? 255 : ((b < 0) ? 0 : b); 

					c = (0xFF<<24) | (r <<16) | (g<<8) | b;

					pixels[i] = c;
				}
			}
			image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
			imageRegion.setImage(image);
		}

		public void changeColorHSB() {
			pause = true;
//			float bright = brightness/255f;
//			int[] rgb = new int[3];

			float d255 = 1/255f;
			float d6 = 1/6f;
			float d3 = 1/3f;
			float d2 = 1/2f;

			for (int i=0; i<pixels.length; i++){
				int c = pixelsOrig[i];

				if ( (c & OPAQUE) == OPAQUE) {

					int r = ((c >> 16) & 255);
					int g = ((c >>  8) & 255);
					int b = ((c      ) & 255);

					if (contrast != 1) {
						r = (int) (contrast*(r-127) + 127);
						g = (int) (contrast*(g-127) + 127);
						b = (int) (contrast*(b-127) + 127);

						if (r > 255) r = 255;
						if (r <   0) r = 0;
						if (g > 255) g = 255;
						if (g <   0) g = 0;
						if (b > 255) b = 255;
						if (b <   0) b = 0;
					}

					if (brightness != 0) {
						r = (int) (r + brightness);
						g = (int) (g + brightness);
						b = (int) (b + brightness);

						if (r > 255) r = 255;
						if (r <   0) r = 0;
						if (g > 255) g = 255;
						if (g <   0) g = 0;
						if (b > 255) b = 255;
						if (b <   0) b = 0;
					}

					if (hueChange != 0 || saturation != 1) {
						float var_Min;    //Min. value of RGB
						float var_Max;    //Max. value of RGB
						float del_Max;             //Delta RGB value

						float rf = r * d255;
						float gf = g * d255;
						float bf = b * d255;

						if (rf > gf) { var_Min = gf; var_Max = rf; }
						else { var_Min = rf; var_Max = gf; }
						if (bf > var_Max) var_Max = bf;
						if (bf < var_Min) var_Min = bf;

						del_Max = var_Max - var_Min; 

						float H = 0, S, L;
						L = ( var_Max + var_Min ) * d2;

						if ( del_Max == 0 ) { H = 0; S = 0; }
						else {                                   //Chromatic data..{
							if ( L < 0.5 ) 
								S = del_Max / ( var_Max + var_Min );
							else           
								S = del_Max / ( 2 - var_Max - var_Min );

							if ( rf == var_Max ) {
								float del_G = ( ( ( var_Max - gf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								float del_B = ( ( ( var_Max - bf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								H = del_B - del_G;
							}
							else if ( gf == var_Max ) {
								float del_R = ( ( ( var_Max - rf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								float del_B = ( ( ( var_Max - bf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								H = d3 + del_R - del_B;
							}
							else if ( bf == var_Max ) {
								float del_R = ( ( ( var_Max - rf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								float del_G = ( ( ( var_Max - gf ) * d6 ) + ( del_Max * d2 ) ) / del_Max;
								H = ( 2 * d3 ) + del_G - del_R;
							}
						}

						H += hueChange;

						if ( H < 0 ) H += 1;
						if ( H > 1 ) H -= 1;

						if ( S == 0 ) {                       //HSL values = From 0 to 1
							r = (int) (L * 255);         //RGB results = From 0 to 255
							g = (int) (L * 255);
							b = (int) (L * 255);
						}
						else {
							S*= saturation;

							float var_2;
							if ( L < 0.5 ) 
								var_2 = L * ( 1 + S );
							else           
								var_2 = ( L + S ) - ( S * L );

							float var_1 = 2 * L - var_2;

							r = (int) (255 * Hue_2_RGB( var_1, var_2, H + d3 ) );
							g = (int) (255 * Hue_2_RGB( var_1, var_2, H )      );
							b = (int) (255 * Hue_2_RGB( var_1, var_2, H - d3 ) );
						}
					}
					///////////////////

					if (r > 255) r = 255;
					if (r <   0) r = 0;
					if (g > 255) g = 255;
					if (g <   0) g = 0;
					if (b > 255) b = 255;
					if (b <   0) b = 0;

					c = OPAQUE | (r <<16) | (g << 8) | b;
				}
				pixels[i] = c;		
			}

			image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
			imageRegion.setImage(image);

			pause = false;		
		}

		void HSLtoRGB(float H, float S, float L, int[] RGB) {
			if ( S == 0 )                       //HSL values = From 0 to 1
			{
				RGB[0] = (int) (L * 255);                      //RGB results = From 0 to 255
				RGB[1] = (int) (L * 255);
				RGB[2] = (int) (L * 255);
			}
			else {
				float var_2;
				if ( L < 0.5 ) 
					var_2 = L * ( 1 + S );
				else           
					var_2 = ( L + S ) - ( S * L );

				float var_1 = 2 * L - var_2;

				RGB[0] = (int) (255 * Hue_2_RGB( var_1, var_2, H + ( 1 / 3f ) ));
				RGB[1] = (int) (255 * Hue_2_RGB( var_1, var_2, H ));
				RGB[2] = (int) (255 * Hue_2_RGB( var_1, var_2, H - ( 1 / 3f ) ));
			}
		}

		float Hue_2_RGB(float v1, float v2, float vH ) {            //Function Hue_2_RGB
			if ( vH < 0 ) 
				vH += 1;
			if ( vH > 1 ) 
				vH -= 1;
			if ( ( 6 * vH ) < 1 ) 
				return ( v1 + ( v2 - v1 ) * 6 * vH );
			if ( ( 2 * vH ) < 1 ) 
				return v2;
			if ( ( 3 * vH ) < 2 ) 
				return ( v1 + ( v2 - v1 ) * ( ( 2 / 3f ) - vH ) * 6 );
			return ( v1 );
		}

		private static final int HASH_F = 13;  // 11
		private static final int HASH_P = 101; //101


		synchronized public void findUniqueColors() { 

			//long ts = System.currentTimeMillis();

			hashSize = (5*maskSize)/3+1;
			if (hashSize % HASH_P == 0)
				hashSize++;

			numberOfColors = 0;

//			ColHash[] colHashtmp = new ColHash[hashSize];
			colHash = new ColHash[hashSize];

			for (int i=0; i<pixels.length; i++){
				int c = pixels[i];

				if ( (c & OPAQUE) == OPAQUE ) { // opaque

					int hash = (((c & 0xFFffFF) * HASH_F) % hashSize); // hash value

					while (colHash[hash] != null && colHash[hash].color != c ){
						hash = (hash + HASH_P) % hashSize;	
					}

					if (colHash[hash] == null) {
						colHash[hash] = new ColHash();
						colHash[hash].color = c;
						colHash[hash].R = ((c >> 16) & 255) - 128;
						colHash[hash].G = ((c >>  8) & 255) - 128;
						colHash[hash].B = ((c      ) & 255) - 128;

						numberOfColors++;
					}
					colHash[hash].frequency++;
				}
			}
		}


		private int getFrequency(int c) {

			if ( ((c >> 24) & 0xff) == 255 ) { // opaque

				int hash = (((c & 0xFFffFF) * HASH_F) % hashSize); // hash value

				while (colHash[hash] != null && colHash[hash].color != c ){
					hash = (hash + HASH_P) % hashSize;	
				}
				return colHash[hash].frequency;
			}
			else
				return -1;
		}

		//------------------------------------------------------------------------------
		public int getColor(int x, int y) {
			int pos = y*width + x; 
			if (pos >= 0 && pos < pixels.length)
				return pixels[y*width + x] ;
			else
				return 0;
		}

		public String showColorDot(int c) {

			int r = ((c >> 16)& 0xff);
			int g = ((c >> 8 )& 0xff);
			int b = ( c       & 0xff);

			String s = "      "+stringColor+":  RGB(" + Misc.fm(3,r) + "," + Misc.fm(3,g) + "," + Misc.fm(3,b) +")"; 

			int[] v = new int[4];
			switch (colorSpace) {
			case RGB:
				break;
			case YUV:	 
				rgb2yuv(r, g, b, v); 
				s += "  YUV(" + Misc.fm(3,v[2]) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case YIQ:	 
				rgb2yiq(r, g, b, v); 
				s += "  YIQ(" + Misc.fm(3,v[2]) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case KLT:	 
				rgb2klt(r, g, b, v); 
				s += "  KLT(" + Misc.fm(3,v[2]) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2] +128;
				break;
			case YCBCR:  
				rgb2ycbcr(r, g, b, v); 
				s += "  YCbCr(" + Misc.fm(3,v[2]) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case HSB: 
				rgb2hsb_(r, g, b, v); 
				s += "  HSB(" + Misc.fm(3,v[0]) +degreeSymbol+"," + Misc.fm(3,v[1]) +"%," + Misc.fm(3,v[2]) +"%)";
				rgb2hsb(r, g, b, v); 
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case HSV: 
				rgb2hsv_(r, g, b, v); 
				s += "  HSV(" + Misc.fm(3,v[0]) +degreeSymbol+"," + Misc.fm(3,v[1]) +"%," + Misc.fm(3,v[2]) +"%)";
				rgb2hsv(r, g, b, v); 
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case HSL: 
				rgb2hsl_(r, g, b, v); 
				s += "  HSL(" + Misc.fm(3,v[0]) +degreeSymbol+"," + Misc.fm(3,v[1]) +"%," + Misc.fm(3,v[2]) +"%)";
				rgb2hsl(r, g, b, v); 
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case HMMD:
				rgb2hmmd_(r, g, b, v); 
				s += "  HMMD(" + Misc.fm(3,v[0]) +"," + Misc.fm(3,v[1]) +"," + Misc.fm(3,v[2]) +"," + Misc.fm(3,2*v[3]) + ")";
				s += "  HSD(" + Misc.fm(3,v[0]) +"," + Misc.fm(3,(int)Math.round((v[1] + v[2] + 0.5)/2)) +"," + Misc.fm(3,2*v[3]) + ")";
				rgb2hmmd(r, g, b, v); 
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case LAB: 
				rgb2lab(r, g, b, v); 
				s += "  Lab(" + Misc.fm(3,(int)(v[2]/2.55 + 0.5)) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;
			case LUV: 
				rgb2luv(r, g, b, v); 
				s += "  Luv(" + Misc.fm(3,(int)(v[2]/2.55 + 0.5)) +"," + Misc.fm(4,v[0]) +"," + Misc.fm(4,v[1]) +")";
				r = v[0] + 128; g = v[1] + 128; b = v[2];
				break;			
			case XYY: 
				rgb2xyy(r, g, b, v); 
				s += "  xyY(" + Misc.fm(5,(int)(v[0]/0.255)/1000.) +"," + Misc.fm(5,(int)(v[1]/0.255)/1000.) +"," +Misc.fm(5,( (int)((v[2]+ 0.5)/0.255 ))/1000.) +")"  ;
				r = v[0]; g = v[1]; b = v[2];
				break;
			case XYZ: 
				rgb2xyz(r, g, b, v); 
				s += "  XYZ(" + Misc.fm(5,(int)(v[0]/0.255)/1000.) +"," + Misc.fm(5,(int)(v[1]/0.255)/1000.) +"," +Misc.fm(5,( (int)((v[2]+ 0.5)/0.255 ))/1000.) +")"  ;
				r = v[0]; g = v[1]; b = v[2];
				break;	
			}
			v = null;		

			R = dotR = r - 128;
			G = dotG = g - 128;
			B = dotB = b - 128;

			xyzPos();
			imageRegion.setDot(X, Y);

			dot = true;
			int num = getFrequency(c);
			if (num >= 0) {
				int percent = 1000* num / maskSize;
				s += "      "+ stringNumber + ": " + num + " ("+ Misc.fm(3,percent/10.) + "%)" ;
			}
			else
				s += "  ---";

			imageRegion.repaint();

			return(s);
		}


		public void printLut() {
			if (hist == true) {
				ResultsTable rt = new ResultsTable();

				for (int i = 0; i < colHash.length; i++) {
					if (colHash[i] != null) {
						int c = colHash[i].color;
						int frequency = getFrequency(c);

						double percent; 
						if (frequency >= 0) { 
							percent = 100.* frequency / maskSize;
							if (percent < 0.001)
								percent = 0;
						}
						else {
							percent = 0;
							frequency = 0;
						}
						int r = ((c >> 16)& 0xff);
						int g = ((c >> 8 )& 0xff);
						int b = ( c       & 0xff);

						rt.incrementCounter();
						if (english) {
							rt.addValue("Red   ", r);
							rt.addValue("Green ", g);
							rt.addValue("Blue  ", b);
							rt.addValue("Frequency", frequency);
							rt.addValue("%", percent);
						}
						else {
							rt.addValue("Rot   ", r);
							rt.addValue("Gr\u00FCn ", g);
							rt.addValue("Blau  ", b);
							rt.addValue("Anzahl", frequency);
							rt.addValue("%", percent);
						}
					}
				}
				rt.show("LUT"); 
			}	

		}


		synchronized public void updateDisplay(){
			try {
				if (!hist) {
					switch (mode) {
					case 0: 
						showColorsNoAlpha();
						break;
					case 1: 
						showNoColorsNoAlpha();
						break;
					case 2: 
						showColorsAlpha();
						break;
					case 3: 
						showNoColorsAlpha();
						break;
					}
				}
				else {
					showColorsHist();
				}
			} catch (RuntimeException e) {}
			
//			ShortProcessor impz = new ShortProcessor(width, height, pixelsZ, null);
//			ImagePlus ipz = new ImagePlus("Z", impz);
//			ipz.show();

			Image image = Toolkit.getDefaultToolkit().createImage(memoryImageSource);
			imageRegion.setImage(image, pixelsZ);
		}

		boolean isInside(int[] p, int[] p1, int[] p2, int[] p3) {
			int x  = p[0];
			int y  = p[1];
			int x1 = p1[0];
			int y1 = p1[1];
			int x2 = p2[0];
			int y2 = p2[1];
			int x3 = p3[0];
			int y3 = p3[1];

			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);

			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}

		private void setTextAndCube() {

			// Textpositionen setzen
			for (int i=0; i<textPositions[0].length; i++) {
				xyzPos(textPositions[colorSpace][i]);
				imageRegion.setTextPos(i, X, Y, Z);
			}

			int line= 0;

			// L and a, b lines 
			if (colorSpace == LAB || colorSpace == LUV) {
				for (int i = 0; i < 3; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsLAB[i]);
					int x1 = X;
					int y1 = Y;
					int z1 = Z;
					xyzPos(lineEndsLAB[i+3]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;
					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );	
				}
			}

			if (colorSpace == RGB || colorSpace == LAB || colorSpace == XYY || colorSpace == LUV || colorSpace == XYZ) {
				// determine color of lines
				int corner[][] = new int[8][4]; // 8 x X, Y, Z, order

				for (int i=0; i<8; i++) {
					xyzPos(cubeCornersRGB[i]);
					corner[i][0] = X;
					corner[i][1] = Y;
					corner[i][2] = Z;
				}

				int[][] cor = new int[3][];

				for (int i = 0; i < 4; i++) {
					int k = 0;
					for (int j = 4; j < 8; j++) {
						if (i+j != 7) 
							cor[k++] = corner[j];
					}
					if (corner[i][2]  >= corner[7-i][2] &&
							isInside(corner[i], cor[0], cor[1], cor[2]))
						corner[i][3] = 1;  // hidden
				}
				for (int j = 4; j < 8; j++) {
					int k = 0;
					for (int i = 0; i < 4; i++) {
						if (i+j != 7) 
							cor[k++] = corner[i];
					}
					if (corner[j][2]  >= corner[7-j][2]  &&  
							isInside(corner[j], cor[0], cor[1], cor[2]))
						corner[j][3] = 1; // hidden
				}

				for (int i = 0; i < 4; i++)
					for (int j = 4; j < 8; j++) {
						if (i+j != 7) {
							Color color;
							if (corner[i][3] == 1 || corner[j][3] == 1) { // hidden
								color = cubeBackColor;
							}
							else {
								color = cubeFrontColor;
							}
							imageRegion.setLine(line++, 
									corner[i][0], corner[i][1], corner[j][0], corner[j][1], corner[i][2],corner[j][2], color);
						}
					}

				cor = null;
				corner = null;
			}
			//-----------------------------------------------------------------	
			else if (colorSpace == YCBCR || colorSpace == YUV || colorSpace == YIQ){
				// Wuerfellinien eintragen
				int[] yuv_i = new int[3];
				int[] yuv_j = new int[3];
				for (int i = 0; i < 4; i++)
					for (int j = 4; j < 8; j++) {
						if (i+j != 7) {
							Color color = Color.lightGray;  
							if (colorSpace == YCBCR) {
								rgb2ycbcr(cubeCornersRGB[i], yuv_i);
								rgb2ycbcr(cubeCornersRGB[j], yuv_j);
							}
							else if (colorSpace == YUV){
								rgb2yuv(cubeCornersRGB[i], yuv_i);
								rgb2yuv(cubeCornersRGB[j], yuv_j);
							}
							else if (colorSpace == YIQ){
								rgb2yiq(cubeCornersRGB[i], yuv_i);
								rgb2yiq(cubeCornersRGB[j], yuv_j);
							}
							
							xyzPos(yuv_i);
							int x1 = X;
							int y1 = Y;
							int z1 = Z;
							xyzPos(yuv_j);
							int x2 = X;
							int y2 = Y;
							int z2 = Z;

							imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );
						}
					}
				for (int i = 0; i < 3; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsYUV[i]);
					int x1 = X;
					int y1 = Y;
					int z1 = Z;
					xyzPos(lineEndsYUV[i+3]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;
					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );	
				}
				yuv_i = null;
				yuv_j = null;
			}
			else if (colorSpace == KLT ){
				
				for (int i = 0; i < 3; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsKLT[i]);
					int x1 = X;
					int y1 = Y;
					int z1 = Z;
					xyzPos(lineEndsKLT[i+3]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;
					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );	
				}
			}
			//-----------------------------------------------------------------
			else if (colorSpace == HSB || colorSpace == HSL) {
				//B and S lines 
				for (int i = 0; i < 2; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsHSB[i]);
					int x1 = X;
					int y1 = Y;
					int z1  = Z;
					xyzPos(lineEndsHSB[i+2]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;

					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );
				}
				int step = 15;
				for (int i=0; i < 360; i+= step) {
					float phi = (float)Math.toRadians(i); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x1u = X; 
					int y1u = Y;
					int z1u = Z;
					B = -128;
					xyzPos();
					int x1d = X; 
					int y1d = Y;
					int z1d = Z;	
					
					phi = (float)Math.toRadians(i+step); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x2u = X; 
					int y2u = Y;
					int z2u = Z;
					B = -128;
					xyzPos();
					int x2d = X; 
					int y2d = Y;
					int z2d = Z;

					imageRegion.setLine(line++, x1u, y1u, x2u, y2u, z1u, z2u, cubeFrontColor);
					imageRegion.setLine(line++, x1d, y1d, x2d, y2d, z1d, z2d, cubeFrontColor);

					if (i % (2*step) == step) {
						B = 0;
						xyzPos();
						Color color = (Z < 0) ? new Color (0xFFBBBBBB) : new Color (0xFF666666);
						imageRegion.setLine(line++, x2u, y2u, x2d, y2d, Z, Z, color); // !!!
					}
				}
			}
			else if (colorSpace == HSV) {
				//B and S lines 
				for (int i = 0; i < 2; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsHSV[i]);
					int x1 = X;
					int y1 = Y;
					int z1 = Z;
					xyzPos(lineEndsHSV[i+2]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;

					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );
				}
				int step = 15;
				for (int i=0; i < 360; i+= step) {
					float phi = (float)Math.toRadians(i); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x1u = X; 
					int y1u = Y;
					int z1u = Z;
					B = -128;
					xyzPos();

					phi = (float)Math.toRadians(i+step); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x2u = X; 
					int y2u = Y;
					int z2u = Z;

					R = 0;
					G = 0;
					B = -128;
					xyzPos();
					int x2d = X; 
					int y2d = Y;
					int z2d = Z;

					imageRegion.setLine(line++, x1u, y1u, x2u, y2u, z1u, z2u, cubeFrontColor);
					//imageRegion.setLine(line++, x1d, y1d, x2d, y2d, Z, cubeFrontColor);

					if (i % (2*step) == step) {
						B = 0;
						xyzPos();
						Color color = (z2u < 0) ? new Color (0xFFBBBBBB) : new Color (0xFF666666);
						imageRegion.setLine(line++, x2u, y2u, x2d, y2d, z2u, z2d, color);
					}
				}
			}
			else if (colorSpace == HCL) {
				//B and S lines 
				for (int i = 0; i < 2; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsHSV[i]);
					int x1 = X;
					int y1 = Y;
					int z1  = Z;
					xyzPos(lineEndsHSV[i+2]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;
					
					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );
				}
				int step = 15;
				for (int i=0; i < 360; i+= step) {
					float phi = (float)Math.toRadians(i); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x1u = X; 
					int y1u = Y;
					int z1u = Z;
					B = -128;
					xyzPos();

					phi = (float)Math.toRadians(i+step); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 127);
					int x2u = X; 
					int y2u = Y;
					int z2u = Z;

					R = 0;
					G = 0;
					B = -128;
					xyzPos();
					int x2d = X; 
					int y2d = Y;
					int z2d = Z;

					imageRegion.setLine(line++, x1u, y1u, x2u, y2u, z1u, z2u, cubeFrontColor);
					//imageRegion.setLine(line++, x1d, y1d, x2d, y2d, Z, cubeFrontColor);

					if (i % (2*step) == step) {
						B = 0;
						xyzPos();
						Color color = (z2u < 0) ? new Color (0xFFBBBBBB) : new Color (0xFF666666);
						imageRegion.setLine(line++, x2u, y2u, x2d, y2d, z2u, z2d, color);
					}
				}
			}
			else if (colorSpace == HMMD) {
				//B and S lines 
				for (int i = 0; i < 2; i++) {
					Color color = (i == 0) ? cubeFrontColor : (((mode & 1) == 0) ? Color.orange : Color.red);
					xyzPos(lineEndsHMMD[i]);
					int x1 = X;
					int y1 = Y;
					int z1 = 1;
					xyzPos(lineEndsHMMD[i+2]);
					int x2 = X;
					int y2 = Y;
					int z2 = Z;

					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, color );
				}

				xyzPos(0, 0, 127);
				int x1u = X; 
				int y1u = Y;
				xyzPos(0, 0, -128);
				int x1d = X; 
				int y1d = Y;

				int step = 15;
				for (int i=0; i < 360; i+= step) {
					float phi = (float)Math.toRadians(i); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 0);
					int x1 = X; 
					int y1 = Y;
					int z1 = Z;
					phi = (float)Math.toRadians(i+step); 

					xyzPos((int)(128*Math.cos(phi)), (int)(128*Math.sin(phi)), 0);
					int x2 = X; 
					int y2 = Y;
					int z2 = Z;

					imageRegion.setLine(line++, x1, y1, x2, y2, z1, z2, cubeFrontColor);

					if (i % (2*step) == step) {
						Color color;
						B = 0;
						xyzPos();
						if (Z <= 0) 
							color = new Color (0xFFBBBBBB);
						else 
							color = new Color (0xFF666666);

						imageRegion.setLine(line++, x1u, y1u, x1, y1, Z, Z, color); // !!!
						imageRegion.setLine(line++, x1d, y1d, x1, y1, Z, Z, color);
					}
				}	
			}
		}

		/************************************************************/		
		// display routines 


		synchronized public void showColorsNoAlpha(){

			setTextAndCube();

			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i]  = 0; 
				pixelsZ[i] = 1000;
			}

			// set colors
			if (numberOfColors > 256) 
			{
				ColHash ch;
				for (int i=colHash.length-1; i>=0; i--){
					ch = colHash[i];
					if (ch != null) {
						R = ch.R;
						G = ch.G;
						B = ch.B;
						xyzPosC();
						if (Z < renderDepth)
							continue;
						//if (X >= 0 && X < width && Y >= 0 && Y < height) 
						if (((X & ~511) | (Y & ~511)) == 0) // only for w = h = 512 !!!
						{
							//int pos = Y*width + X;
							int pos = (Y<<9) | X;  // only for w = 512 !!!

							if (Z < pixelsZ[pos]) {
								pixelsZ[pos] = (short)Z;
								pixels[pos] = ch.color; 
							}
						}
					}
				}
			}

			else {
				for (int i=0; i< colHash.length; i++){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();
						if (Z < renderDepth)
							continue;

						for (int y = -4; y <= 4; y++) {
							int Yy = Y+y;
							for (int x = -4; x <= 4; x++) {

								if (x*x+y*y <= 16) {

									int Xx = X+x;

									// if ( X >= 0 && X < width && Y >= 0 && Y < height) {
									if ((Xx & ~511) == 0 && (Yy & ~511) == 0) { // only for w = h = 512

										int pos = Yy*width + Xx;
										if (Z < pixelsZ[pos]) {
											pixelsZ[pos] = (short)Z;
											pixels[pos] = colHash[i].color;
										}
									}
								}
							}
						}
					}
				}
			}
		}


		synchronized public void showColorsHist(){

			setTextAndCube();

			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0; 
				pixelsZ[i] = 1000;
			}

			float rFactor = (float) (40f/Math.pow(maskSize, 1/3.));


			for (int i=0; i< colHash.length; i++){
				if (colHash[i] != null) {
					R = colHash[i].R;
					G = colHash[i].G;
					B = colHash[i].B;
					xyzPosC();

					if (Z < renderDepth)
						continue;

					int rad = (int)(1.1f*Math.pow(colHash[i].frequency, 1/3.)*rFactor); 

					float sz = d /(Z + d);
					rad = (int) Math.max(Math.round(rad * sz),1);

					long r, g, b;

					if (colorMode == true) {
						int c = colHash[i].color;
						r = ((c >> 16)& 0xff);
						g = ((c >> 8 )& 0xff);
						b = ( c       & 0xff);
					}
					else 
						r = g = b = 200;

					long rr = rad*rad + 1;

					for (int y = -rad; y <=  rad; y++) {
						int Yy = Y+y;

						for (int x = -rad; x <= rad; x++) {
							long rxy = x*x + y*y;

							if (rxy < rr) {
								int Xx = X+x;
								if (((Xx & ~511) | (Yy & ~511)) == 0) {// only for w = h = 512 !!!

									int pos = Yy*width + Xx;
									if (Z < pixelsZ[pos]) {
										pixelsZ[pos] = (short)Z;
										float a = (float) (0.5+0.5*(rr*rr-rxy*rxy) / (float)(rr*rr));
										int r_ = (int) (a*r);
										int g_ = (int) (a*g);
										int b_ = (int) (a*b);

										pixels[pos] = OPAQUE | (r_ <<16) | (g_ << 8) | b_;
									}
								}
							}
						}
					}
				}
			}
		}	


		synchronized public void showColorsAlpha(){

			setTextAndCube();

			for (int i = pixels.length-1; i >= 0 ; i--)  {
				pixels[i] = 0;
				pixelsAlpha[i] = 0;
			}

			if (numberOfColors > 256){
				ColHash ch;
				for (int i=colHash.length-1; i >= 0; i--){
					ch = colHash[i];
					if (ch != null) {
						R = ch.R;
						G = ch.G;
						B = ch.B;
						xyzPosC();
						if (Z < renderDepth)
							continue;

						if ((X & ~511) == 0 && (Y & ~511) == 0) { // only for w = h = 512

							int pos = (Y<<9) | X;  // only for w = 512 !!!

							int c_ = pixels[pos];
							int alpha = pixelsAlpha[pos];

							int R_ = ((c_ >> 16)& 0xff);
							int G_ = ((c_ >> 8 )& 0xff);
							int B_ = ( c_       & 0xff);

							int f = (int) (freqFactor*colHash[i].frequency);
							f = (f <= 255) ? f : 255;

							float zs = (float) (Math.min(1,Math.max(0,(-Z + 128.)/256.))*0.9 + 0.1);
							f = (int) (f*zs) ; 

							c_ = colHash[i].color;
							R = ((c_ >> 16)& 0xff);
							G = ((c_ >> 8 )& 0xff);
							B = ( c_       & 0xff);

							int ag = alpha+f;
							ag = (ag == 0) ? 1 : ag;
							pixelsAlpha[pos] = ag;

							R = ((R*f + alpha*R_)/ag); 
							G = ((G*f + alpha*G_)/ag); 
							B = ((B*f + alpha*B_)/ag); 

							pixels[pos] = (Math.min( ag, 255)<<24) | (R  << 16) | (G << 8) | B;			
						}	
					}
				}
			}

			else {
				for (int i = 0; i < pixels.length; i++) {
					pixelsZ[i] = 1000;
				}

				for (int i=0; i< colHash.length; i++){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();

						if (Z < renderDepth)
							continue;

						//int f = colHash[i].freq;  
						int f = (int) (freqFactor*colHash[i].frequency);
						f = (f <= 255) ? f : 255;


						for (int y = -4; y <=  4; y++)
							for (int x = -4; x <= 4; x++) {

								if (x*x+y*y <= 16) {
									int Yy = Y+y;
									int Xx = X+x;

									// if ( X >= 0 && X < width && Y >= 0 && Y < height) {
									if ((Xx & ~511) == 0 && (Yy & ~511) == 0) { // only for w = h = 512

										int pos = (Yy)*width + (Xx);
										if (Z < pixelsZ[pos]) {
											pixelsZ[pos] = (short)Z;
											pixels[pos] = (f <<24) | (colHash[i].color & 0xFFffFF);
										}
									}
								}
							}
					}
				}
			}
		}


		synchronized public void showNoColorsAlpha(){

			setTextAndCube();

			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0;
				pixelsAlpha[i] = 0;
			}

			if (numberOfColors > 256){
				for (int i=0; i< colHash.length; i++){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();
						if (Z < renderDepth)
							continue;

						int pos = X + Y*width;

						//	if ( X >= 0 && X < width && Y >= 0 && Y < height) {
						if ((X & ~511) == 0 && (Y & ~511) == 0) { // only for w = h = 512



							int alpha = pixelsAlpha[pos];

							int f = (int) (freqFactor*colHash[i].frequency);
							f = (f <= 255) ? f : 255;

							f = (int) (f*(Math.min(1,Math.max(0,(-Z + 128.)/256.))*0.9+ 0.1)) ; 

							int ag = alpha+f;
							if (ag == 0)
								ag = (alpha == 0) ? 1 : alpha;
							pixelsAlpha[pos] = ag;

							pixels[pos] = (Math.min( ag, 255)<<24) ;
						}
					}	
				}
			}
			else {
				for (int i = 0; i < pixels.length; i++) {
					pixelsZ[i] = 1000;
				}

				for (int i=0; i< colHash.length; i++){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();

						if (Z < renderDepth)
							continue;

						int f = (int) (freqFactor*colHash[i].frequency);
						f = (f <= 255) ? f : 255;

						for (int y = -4; y <=  4; y++)
							for (int x = -4; x <= 4; x++) {

								if (x*x+y*y <= 16) {
									int Yy = Y+y;
									int Xx = X+x;

									// if ( X >= 0 && X < width && Y >= 0 && Y < height) {
									if ((Xx & ~511) == 0 && (Yy & ~511) == 0) { // only for w = h = 512
										int pos = (Yy)*width + (Xx);
										if (Z < pixelsZ[pos]) {
											pixelsZ[pos] = (short)Z;
											pixels[pos] = f <<24;
										}
									}
								}
							}
					}
				}
			}
		}

		synchronized public void showNoColorsNoAlpha(){

			setTextAndCube();

			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0; 
			}

			if (numberOfColors > 256){
				for (int i=colHash.length-1; i>=0; i--){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();

						if (Z < renderDepth)
							continue;

						// if ( X >= 0 && X < width && Y >= 0 && Y < height) {
						if ((X & ~511) == 0 && (Y & ~511) == 0) { // only for w = h = 512	
							pixels[X + Y*width] = OPAQUE;
						}
					}
				}
			}
			else {
				for (int i=0; i< colHash.length; i++){
					if (colHash[i] != null) {
						R = colHash[i].R;
						G = colHash[i].G;
						B = colHash[i].B;
						xyzPosC();

						if (Z < renderDepth)
							continue;
						for (int y = -4; y <=  4; y++)
							for (int x = -4; x <= 4; x++) {
								if (x*x+y*y <= 16) {
									int Yy = Y+y;
									int Xx = X+x;

									// if ( X >= 0 && X < width && Y >= 0 && Y < height) {
									if ((Xx & ~511) == 0 && (Yy & ~511) == 0) { // only for w = h = 512
										pixels[(Yy)*width + Xx] = (255 << 24);		
									}
								}
							}
					}	
				}
			}
		}

		public void setD(float d) {
			this.d = d;
		}

		public int getX() {
			return X;
		}
		public int getY() {
			return Y;
		}		
	}

	private final class ColHash {

		int R;
		int G;
		int B;

		int color;

		int frequency;
	}
	///////////////////////////////////////////////////////////////////////////

	class Lines {
		int x1;
		int y1;
		int x2;
		int y2;
		int z1;
		int z2;
		
		Color color;

		public void setPos(int x1, int y1, int x2, int y2, int z1, int z2, Color color) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.z1 = z1;
			this.z2 = z2;
			this.color = color;	
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}

	///////////////////////////////////////////////////////////////////////////

	class TextField {
		private String text = "";
		private Color color;

		private int xpos;
		private int ypos;
		private int z;

		public Color getColor() {
			return color;
		}
		public void setColor(Color color) {
			this.color = color;
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setXpos(int xpos) {
			this.xpos = xpos;
		}

		public void setYpos(int ypos) {
			this.ypos = ypos;
		}

		public String getText() {
			return text;
		}
		public int getXpos() {
			return xpos;
		}
		public int getYpos() {
			return ypos;
		}
		public void setZ(int z) {
			this.z = z;
		}
		public int getZ() {
			return z;
		}
	}

	static class Misc {

		static String fm(int len, int val) {
			String s = "" + val;

			while (s.length() < len) {
				s = "  " + s;
			}
			return s;
		}
		static String fm(int len, double val) {
			String s = "" + val;

			while (s.length() < len) {
				s = s + " ";
			}
			return s;
		}
	}

	public class Median_Cut {

		static final int MAXCOLORS = 256;  // maximum # of output colors
		static final int HSIZE = 32768;    // size of image histogram
		private int[] hist;          // RGB histogram and reverse color lookup table
		private int[] histPtr;        // points to colors in "hist"
		private Cube[] list;        // list of cubes
		private int[] pixels32;
		private int width;
		private int height;
		private IndexColorModel cm;

		public Median_Cut(int[] pixels, int width, int height) {
			int color16;

			pixels32 = pixels;
			this.width = width;
			this.height = height;

			//build 32x32x32 RGB histogram
			hist = new int[HSIZE];
			for (int i = 0; i < width * height; i++) {
				if ((pixels32[i] & 0xFF000000) == 0xFF000000 ) {
					color16 = rgb(pixels32[i]);
					hist[color16]++;
				}
			}
		}

		int getColorCount() {
			int count = 0;
			for (int i = 0; i < HSIZE; i++)
				if (hist[i] > 0) count++;
			return count;
		}


		Color getModalColor() {
			int max = 0;
			int c = 0;
			for (int i = 0; i < HSIZE; i++)
				if (hist[i] > max) {
					max = hist[i];
					c = i;
				}
			return new Color(red(c), green(c), blue(c));
		}


		// Convert from 24-bit to 15-bit color
		private final int rgb(int c) {
			int r = (c & 0xf80000) >> 19;
			int g = (c & 0xf800) >> 6;
			int b = (c & 0xf8) << 7;
			return b | g | r;
		}

		// Get red component of a 15-bit color
		private final int red(int x) {
			return (x & 31) << 3;
		}

		// Get green component of a 15-bit color
		private final int green(int x) {
			return (x >> 2) & 0xf8;
		}

		// Get blue component of a 15-bit color
		private final int blue(int x) {
			return (x >> 7) & 0xf8;
		}


		/** Uses Heckbert's median-cut algorithm to divide the color space defined by
		 "hist" into "maxcubes" cubes. The centroids (average value) of each cube
		 are are used to create a color table. "hist" is then updated to function
		 as an inverse color map that is used to generate an 8-bit image. */
		public Image convert(int maxcubes) {
			return convertToByte(maxcubes);
		}

		/** This is a version of convert that returns a ByteProcessor. */
		public Image convertToByte(int maxcubes) {
			int lr, lg, lb;
			int i, median, color;
			int count;
			int k, level, ncubes, splitpos;
			int longdim = 0;  //longest dimension of cube
			Cube cube, cubeA, cubeB;

			// Create initial cube
			list = new Cube[MAXCOLORS];
			histPtr = new int[HSIZE];
			ncubes = 0;
			cube = new Cube();
			for (i = 0, color = 0; i <= HSIZE - 1; i++) {
				if (hist[i] != 0) {
					histPtr[color++] = i;
					cube.count = cube.count + hist[i];
				}
			}
			cube.lower = 0;
			cube.upper = color - 1;
			cube.level = 0;
			Shrink(cube);
			list[ncubes++] = cube;

			//Main loop
			while (ncubes < maxcubes) {

				// Search the list of cubes for next cube to split, the lowest level cube
				level = 255;
				splitpos = -1;
				for (k = 0; k <= ncubes - 1; k++) {
					if (list[k].lower == list[k].upper)
						;  // single color; cannot be split
					else if (list[k].level < level) {
						level = list[k].level;
						splitpos = k;
					}
				}
				if (splitpos == -1)  // no more cubes to split
					break;

				// Find longest dimension of this cube
				cube = list[splitpos];
				lr = cube.rmax - cube.rmin;
				lg = cube.gmax - cube.gmin;
				lb = cube.bmax - cube.bmin;
				if (lr >= lg && lr >= lb) longdim = 0;
				if (lg >= lr && lg >= lb) longdim = 1;
				if (lb >= lr && lb >= lg) longdim = 2;

				// Sort along "longdim"
				reorderColors(histPtr, cube.lower, cube.upper, longdim);
				quickSort(histPtr, cube.lower, cube.upper);
				restoreColorOrder(histPtr, cube.lower, cube.upper, longdim);

				// Find median
				count = 0;
				for (i = cube.lower; i <= cube.upper - 1; i++) {
					if (count >= cube.count / 2) break;
					color = histPtr[i];
					count = count + hist[color];
				}
				median = i;

				// Now split "cube" at the median and add the two new
				// cubes to the list of cubes.
				cubeA = new Cube();
				cubeA.lower = cube.lower;
				cubeA.upper = median - 1;
				cubeA.count = count;
				cubeA.level = cube.level + 1;
				Shrink(cubeA);
				list[splitpos] = cubeA;        // add in old slot

				cubeB = new Cube();
				cubeB.lower = median;
				cubeB.upper = cube.upper;
				cubeB.count = cube.count - count;
				cubeB.level = cube.level + 1;
				Shrink(cubeB);
				list[ncubes++] = cubeB;        // add in new slot */
			}

			// We have enough cubes, or we have split all we can. Now
			// compute the color map, the inverse color map, and return
			// an 8-bit image.
			makeInverseMap(hist, ncubes);
			return makeImage();
		}

		void Shrink(Cube cube) {
			// Encloses "cube" with a tight-fitting cube by updating the
			// (rmin,gmin,bmin) and (rmax,gmax,bmax) members of "cube".

			int r, g, b;
			int color;
			int rmin, rmax, gmin, gmax, bmin, bmax;

			rmin = 255;
			rmax = 0;
			gmin = 255;
			gmax = 0;
			bmin = 255;
			bmax = 0;
			for (int i = cube.lower; i <= cube.upper; i++) {
				color = histPtr[i];
				r = red(color);
				g = green(color);
				b = blue(color);
				if (r > rmax) rmax = r;
				if (r < rmin) rmin = r;
				if (g > gmax) gmax = g;
				if (g < gmin) gmin = g;
				if (b > bmax) bmax = b;
				if (b < bmin) bmin = b;
			}
			cube.rmin = rmin;
			cube.rmax = rmax;
			cube.gmin = gmin;
			cube.gmax = gmax;
			cube.bmin = bmin;
			cube.bmax = bmax;
		}


		void makeInverseMap(int[] hist, int ncubes) {
			// For each cube in the list of cubes, computes the centroid
			// (average value) of the colors enclosed by that cube, and
			// then loads the centroids in the color map. Next loads
			// "hist" with indices into the color map

			int r, g, b;
			int color;
			float rsum, gsum, bsum;
			Cube cube;
			byte[] rLUT = new byte[256];
			byte[] gLUT = new byte[256];
			byte[] bLUT = new byte[256];

			for (int k = 0; k <= ncubes - 1; k++) {
				cube = list[k];
				rsum = gsum = bsum = (float) 0.0;
				for (int i = cube.lower; i <= cube.upper; i++) {
					color = histPtr[i];
					r = red(color);
					rsum += (float) r * (float) hist[color];
					g = green(color);
					gsum += (float) g * (float) hist[color];
					b = blue(color);
					bsum += (float) b * (float) hist[color];
				}

				// Update the color map
				r = (int) (rsum / (float) cube.count);
				g = (int) (gsum / (float) cube.count);
				b = (int) (bsum / (float) cube.count);
				if (r == 248 && g == 248 && b == 248)
					r = g = b = 255;  // Restore white (255,255,255)
				rLUT[k] = (byte) r;
				gLUT[k] = (byte) g;
				bLUT[k] = (byte) b;
			}
			cm = new IndexColorModel(8, ncubes, rLUT, gLUT, bLUT);

			// For each color in each cube, load the corre-
			// sponding slot in "hist" with the centroid of the cube.
			for (int k = 0; k <= ncubes - 1; k++) {
				cube = list[k];
				for (int i = cube.lower; i <= cube.upper; i++) {
					color = histPtr[i];
					hist[color] = k;
				}
			}
		}


		void reorderColors(int[] a, int lo, int hi, int longDim) {
			// Change the ordering of the 5-bit colors in each word of int[]
			// so we can sort on the 'longDim' color

			int c, r, g, b;
			switch (longDim) {
			case 0: //red
				for (int i = lo; i <= hi; i++) {
					c = a[i];
					r = c & 31;
					a[i] = (r << 10) | (c >> 5);
				}
				break;
			case 1: //green
				for (int i = lo; i <= hi; i++) {
					c = a[i];
					r = c & 31;
					g = (c >> 5) & 31;
					b = c >> 10;
				a[i] = (g << 10) | (b << 5) | r;
				}
				break;
			case 2: //blue; already in the needed order
				break;
			}
		}


		void restoreColorOrder(int[] a, int lo, int hi, int longDim) {
			// Restore the 5-bit colors to the original order

			int c, r, g, b;
			switch (longDim) {
			case 0: //red
				for (int i = lo; i <= hi; i++) {
					c = a[i];
					r = c >> 10;
				a[i] = ((c & 1023) << 5) | r;
				}
				break;
			case 1: //green
				for (int i = lo; i <= hi; i++) {
					c = a[i];
					r = c & 31;
					g = c >> 10;
				b = (c >> 5) & 31;
				a[i] = (b << 10) | (g << 5) | r;
				}
				break;
			case 2: //blue
				break;
			}
		}


		void quickSort(int a[], int lo0, int hi0) {
			// Based on the QuickSort method by James Gosling from Sun's SortDemo applet

			int lo = lo0;
			int hi = hi0;
			int mid, t;

			if (hi0 > lo0) {
				mid = a[(lo0 + hi0) / 2];
				while (lo <= hi) {
					while ((lo < hi0) && (a[lo] < mid))
						++lo;
					while ((hi > lo0) && (a[hi] > mid))
						--hi;
					if (lo <= hi) {
						t = a[lo];
						a[lo] = a[hi];
						a[hi] = t;
						++lo;
						--hi;
					}
				}
				if (lo0 < hi)
					quickSort(a, lo0, hi);
				if (lo < hi0)
					quickSort(a, lo, hi0);

			}
		}


		Image makeImage() {
			// Generate 8-bit image

			Image img8;
			byte[] pixels8;
			int color16;
			pixels8 = new byte[width * height];
			for (int i = 0; i < width * height; i++) {
				color16 = rgb(pixels32[i]);
				pixels8[i] = (byte) hist[color16];
			}
			img8 = Toolkit.getDefaultToolkit().createImage(
					new MemoryImageSource(width, height,
							cm, pixels8, 0, width));
			return img8;
		}


	} //class MedianCut


	class Cube {      // structure for a cube in color space
		int lower;      // one corner's index in histogram
		int upper;      // another corner's index in histogram
		int count;      // cube's histogram count
		int level;      // cube's level
		int rmin;
		int rmax;
		int gmin;
		int gmax;
		int bmin;
		int bmax;

		Cube() {
			count = 0;
		}

		public String toString() {
			String s = "lower=" + lower + " upper=" + upper;
			s = s + " count=" + count + " level=" + level;
			s = s + " rmin=" + rmin + " rmax=" + rmax;
			s = s + " gmin=" + gmin + " gmax=" + gmax;
			s = s + " bmin=" + bmin + " bmax=" + bmax;
			return s;
		}

	}

	/**
	 * @author barthel
	 *
	 adapted from 

	 C Implementation of Wu's Color Quantizer (v. 2)
	 (see Graphics Gems vol. II, pp. 126-133)

	 Author:	Xiaolin Wu
	 Dept. of Computer Science
	 Univ. of Western Ontario
	 London, Ontario N6A 5B7
	 wu@csd.uwo.ca

	 Algorithm: Greedy orthogonal bipartition of RGB space for variance
	 minimization aided by inclusion-exclusion tricks.
	 For speed no nearest neighbor search is done. Slightly
	 better performance can be expected by more sophisticated
	 but more expensive versions.

	 Free to distribute, comments and suggestions are appreciated.
	 ***********************************************************************/
	public class WuCq {

		private final static int MAXCOLOR	= 512;
		private final static int RED =	2;
		private final static int GREEN = 1;
		private final static int BLUE	= 0;

		//	Histogram is in elements 1..HISTSIZE along each axis,
		//	element 0 is for base or marginal value
		//	NB: these must start out 0!

		float[][][]	m2 = new float[33][33][33];
		long [][][]	wt = new long [33][33][33];
		long [][][]	mr = new long [33][33][33];
		long [][][]	mg = new long [33][33][33];
		long [][][]	mb = new long [33][33][33];

		int   pixels[];
		int	  size; // image size
		int	  K;    // color look-up table size
		int   Qadd[];

		int[] lut_r = new int [ MAXCOLOR ] ;
		int[] lut_g = new int [ MAXCOLOR ] ;
		int[] lut_b = new int [ MAXCOLOR ] ;
		int	tag[];

		WuCq (int[] pixels, int size, int numCols) {
			this.pixels = pixels;
			this.size = size;
			this.K = numCols;
		}

		void Hist3d(long vwt[][][], long vmr[][][], long vmg[][][], long vmb[][][], float m2[][][]) 
		{
			int r, g, b;
			int	inr, ing, inb;
			int [] table = new int[256];
			int i;

			for(i=0; i<256; ++i) 
				table[i]= i*i;

			Qadd = new int[size];

			for(i=0; i<size; ++i){
				int c = pixels[i];
				if ((c & 0xFF000000) == 0xFF000000 ) {

					r = ((c >> 16)& 0xff);
					g = ((c >> 8 )& 0xff);
					b = ( c       & 0xff);

					inr=(r>>3)+1; 
					ing=(g>>3)+1; 
					inb=(b>>3)+1; 
					Qadd[i]=(inr<<10)+(inr<<6)+inr+(ing<<5)+ing+inb;

					vwt[inr][ing][inb]++;
					vmr[inr][ing][inb] += r;
					vmg[inr][ing][inb] += g;
					vmb[inr][ing][inb] += b;

					m2[inr][ing][inb] += (float)(table[r]+table[g]+table[b]);
				}
			}
		}

		//	At conclusion of the histogram step, we can interpret
		//	wt[r][g][b] = sum over voxel of P(c)
		//	mr[r][g][b] = sum over voxel of r*P(c)  ,  similarly for mg, mb
		//	m2[r][g][b] = sum over voxel of c^2*P(c)
		//	Actually each of these should be divided by 'size' to give the usual
		//	interpretation of P() as ranging from 0 to 1, but we needn't do that here.

		//	We now convert histogram into moments so that we can rapidly calculate
		//	the sums of the above quantities over any desired box.

		void M3d(long vwt[][][], long vmr[][][], long vmg[][][], long vmb[][][], float m2[][][]) // compute cumulative moments. 
		{
			int i, r, g, b;
			long line, line_r, line_g, line_b;
			long[] area = new long[33], area_r = new long[33], area_g = new long[33], area_b = new long[33];
			float  line2; 
			float[] area2 = new float[33];

			for(r=1; r<=32; ++r){
				for(i=0; i<=32; ++i) 
					area2[i]=area[i]=area_r[i]=area_g[i]=area_b[i]=0;
				for(g=1; g<=32; ++g){
					line2 = line = line_r = line_g = line_b = 0;
					for(b=1; b<=32; ++b){
						//ind1 = (r<<10) + (r<<6) + r + (g<<5) + g + b; // [r][g][b] 

						line   += vwt[r][g][b];
						line_r += vmr[r][g][b]; 
						line_g += vmg[r][g][b]; 
						line_b += vmb[r][g][b];
						line2  += m2[r][g][b];

						area[b] += line;
						area_r[b] += line_r;
						area_g[b] += line_g;
						area_b[b] += line_b;
						area2[b] += line2;

						vwt[r][g][b] = vwt[r-1][g][b] + area[b];
						vmr[r][g][b] = vmr[r-1][g][b] + area_r[b];
						vmg[r][g][b] = vmg[r-1][g][b] + area_g[b];
						vmb[r][g][b] = vmb[r-1][g][b] + area_b[b];
						m2[r][g][b]  = m2[r-1][g][b]  + area2[b];
					}
				}
			}
		}

		//	 Compute sum over a box of any given statistic 
		long Vol( Box cube, long mmt[][][]) 
		{
			return(  mmt[cube.r1][cube.g1][cube.b1] 
			                               -mmt[cube.r1][cube.g1][cube.b0]
			                                                      -mmt[cube.r1][cube.g0][cube.b1]
			                                                                             +mmt[cube.r1][cube.g0][cube.b0]
			                                                                                                    -mmt[cube.r0][cube.g1][cube.b1]
			                                                                                                                           +mmt[cube.r0][cube.g1][cube.b0]
			                                                                                                                                                  +mmt[cube.r0][cube.g0][cube.b1]
			                                                                                                                                                                         -mmt[cube.r0][cube.g0][cube.b0] );
		}

		//	The next two routines allow a slightly more efficient calculation
		//	of Vol() for a proposed subbox of a given box.  The sum of Top()
		//	and Bottom() is the Vol() of a subbox split in the given direction
		//	and with the specified new upper bound.


		long Bottom(Box cube, int dir, long mmt[][][])
		//	Compute part of Vol(cube, mmt) that doesn't depend on r1, g1, or b1 
		//	(depending on dir) 
		{
			switch(dir){
			case RED:
				return( -mmt[cube.r0][cube.g1][cube.b1]
				                               +mmt[cube.r0][cube.g1][cube.b0]
				                                                      +mmt[cube.r0][cube.g0][cube.b1]
				                                                                             -mmt[cube.r0][cube.g0][cube.b0] );
			case GREEN:
				return( -mmt[cube.r1][cube.g0][cube.b1]
				                               +mmt[cube.r1][cube.g0][cube.b0]
				                                                      +mmt[cube.r0][cube.g0][cube.b1]
				                                                                             -mmt[cube.r0][cube.g0][cube.b0] );
			case BLUE:
				return( -mmt[cube.r1][cube.g1][cube.b0]
				                               +mmt[cube.r1][cube.g0][cube.b0]
				                                                      +mmt[cube.r0][cube.g1][cube.b0]
				                                                                             -mmt[cube.r0][cube.g0][cube.b0] );
			default:
				return 0;
			}
		}

		long Top(Box cube, int dir, int pos, long mmt[][][])
		//	 Compute remainder of Vol(cube, mmt), substituting pos for 
		//	 r1, g1, or b1 (depending on dir) 
		{
			switch(dir){
			case RED:
				return( mmt[pos][cube.g1][cube.b1] 
				                          -mmt[pos][cube.g1][cube.b0]
				                                             -mmt[pos][cube.g0][cube.b1]
				                                                                +mmt[pos][cube.g0][cube.b0] );
			case GREEN:
				return( mmt[cube.r1][pos][cube.b1] 
				                          -mmt[cube.r1][pos][cube.b0]
				                                             -mmt[cube.r0][pos][cube.b1]
				                                                                +mmt[cube.r0][pos][cube.b0] );
			case BLUE:
				return( mmt[cube.r1][cube.g1][pos]
				                              -mmt[cube.r1][cube.g0][pos]
				                                                     -mmt[cube.r0][cube.g1][pos]
				                                                                            +mmt[cube.r0][cube.g0][pos] );
			default:
				return 0;
			}
		}

		float Var(Box cube)
		//	 Compute the weighted variance of a box 
		//	 NB: as with the raw statistics, this is really the variance * size 
		{
			float dr, dg, db, xx;

			dr = Vol(cube, mr); 
			dg = Vol(cube, mg); 
			db = Vol(cube, mb);
			xx = m2[cube.r1][cube.g1][cube.b1] 
			                          -m2[cube.r1][cube.g1][cube.b0]
			                                                -m2[cube.r1][cube.g0][cube.b1]
			                                                                      +m2[cube.r1][cube.g0][cube.b0]
			                                                                                            -m2[cube.r0][cube.g1][cube.b1]
			                                                                                                                  +m2[cube.r0][cube.g1][cube.b0]
			                                                                                                                                        +m2[cube.r0][cube.g0][cube.b1]
			                                                                                                                                                              -m2[cube.r0][cube.g0][cube.b0];

			return( xx - (dr*dr+dg*dg+db*db)/(float)Vol(cube,wt) );    
		}

		//	We want to minimize the sum of the variances of two subboxes.
		//	The sum(c^2) terms can be ignored since their sum over both subboxes
		//	is the same (the sum for the whole box) no matter where we split.
		//	The remaining terms have a minus sign in the variance formula,
		//	so we drop the minus sign and MAXIMIZE the sum of the two terms.


		float Maximize(Box cube, int dir, int first, int last, int cut[],
				long whole_r, long whole_g, long whole_b, long whole_w)
		{
			long half_r, half_g, half_b, half_w;
			long base_r, base_g, base_b, base_w;
			int i;
			float temp, max;

			base_r = Bottom(cube, dir, mr);
			base_g = Bottom(cube, dir, mg);
			base_b = Bottom(cube, dir, mb);
			base_w = Bottom(cube, dir, wt);
			max = (float) 0.0;
			cut[0] = -1;
			for(i=first; i<last; ++i){
				half_r = base_r + Top(cube, dir, i, mr);
				half_g = base_g + Top(cube, dir, i, mg);
				half_b = base_b + Top(cube, dir, i, mb);
				half_w = base_w + Top(cube, dir, i, wt);
				// now half_x is sum over lower half of box, if split at i 
				if (half_w == 0) {        // subbox could be empty of pixels! 
					continue;             // never split into an empty box 
				} else
					temp = ((float)half_r*half_r + (float)half_g*half_g +
							(float)half_b*half_b)/half_w;

				half_r = whole_r - half_r;
				half_g = whole_g - half_g;
				half_b = whole_b - half_b;
				half_w = whole_w - half_w;
				if (half_w == 0) {        // subbox could be empty of pixels! 
					continue;             // never split into an empty box 
				} else
					temp += ((float)half_r*half_r + (float)half_g*half_g +
							(float)half_b*half_b)/half_w;

				if (temp > max) {
					max=temp; 
					cut[0]=i;
				}
			}
			return(max);
		}

		int Cut(Box set1, Box set2)
		{
			int dir;
			int[] cutr = {0}, cutg = {0}, cutb = {0};
			float maxr, maxg, maxb;
			long whole_r, whole_g, whole_b, whole_w;

			whole_r = Vol(set1, mr);
			whole_g = Vol(set1, mg);
			whole_b = Vol(set1, mb);
			whole_w = Vol(set1, wt);

			maxr = Maximize(set1, RED, set1.r0+1, set1.r1, cutr,
					whole_r, whole_g, whole_b, whole_w);
			maxg = Maximize(set1, GREEN, set1.g0+1, set1.g1, cutg,
					whole_r, whole_g, whole_b, whole_w);
			maxb = Maximize(set1, BLUE, set1.b0+1, set1.b1, cutb,
					whole_r, whole_g, whole_b, whole_w);

			if( (maxr>=maxg)&&(maxr>=maxb) ) {
				dir = RED;
				if (cutr[0] < 0) 
					return 0; // can't split the box 
			}
			else
				if( (maxg>=maxr)&&(maxg>=maxb) ) 
					dir = GREEN;
				else
					dir = BLUE; 

			set2.r1 = set1.r1;
			set2.g1 = set1.g1;
			set2.b1 = set1.b1;

			switch (dir){
			case RED:
				set2.r0 = set1.r1 = cutr[0];
				set2.g0 = set1.g0;
				set2.b0 = set1.b0;
				break;
			case GREEN:
				set2.g0 = set1.g1 = cutg[0];
				set2.r0 = set1.r0;
				set2.b0 = set1.b0;
				break;
			case BLUE:
				set2.b0 = set1.b1 = cutb[0];
				set2.r0 = set1.r0;
				set2.g0 = set1.g0;
				break;
			}
			set1.vol=(set1.r1-set1.r0)*(set1.g1-set1.g0)*(set1.b1-set1.b0);
			set2.vol=(set2.r1-set2.r0)*(set2.g1-set2.g0)*(set2.b1-set2.b0);
			return 1;
		}

		void Mark(Box cube, int label, int tag[]) {
			int r, g, b;

			for(r=cube.r0+1; r<=cube.r1; ++r)
				for(g=cube.g0+1; g<=cube.g1; ++g)
					for(b=cube.b0+1; b<=cube.b1; ++b)
						tag[(r<<10) + (r<<6) + r + (g<<5) + g + b] = label;
		}

		void main_()
		{
			Box[]	cube = new Box[MAXCOLOR];
			for (int i = 0; i < MAXCOLOR; i++)
				cube[i] = new Box();

			int		next;
			long    weight;
			float[]	vv = new float[MAXCOLOR];
			float   temp;

			Hist3d(wt, mr, mg, mb, m2); 
			//IJ.log("Histogram done\n");

			M3d(wt, mr, mg, mb, m2);    
			//IJ.log("Moments done\n");

			cube[0].r0 = cube[0].g0 = cube[0].b0 = 0;
			cube[0].r1 = cube[0].g1 = cube[0].b1 = 32;

			next = 0;
			for(int i=1; i<K; ++i) {
				if (Cut(cube[next], cube[i]) == 1) {
					// volume test ensures we won't try to cut one-cell box 
					vv[next] = (float) ((cube[next].vol>1) ? Var(cube[next]) : 0.0);
					vv[i] = (float) ((cube[i].vol>1) ? Var(cube[i]) : 0.0);
				} 
				else {
					vv[next] = (float) 0.0;   // don't try to split this box again 
					i--;              // didn't create box i 
				}
				next = 0; temp = vv[0];
				for(int k=1; k<=i; ++k)
					if (vv[k] > temp) {
						temp = vv[k]; next = k;
					}
				if (temp <= 0.0) {
					K = i+1;
					//IJ.log("Only got " + K + " boxes\n");
					break;
				}
			}
			//IJ.log("Partition done\n");

			tag = new int[33*33*33];

			for(int k=0; k<K; ++k){
				Mark(cube[k], k, tag);
				weight = Vol(cube[k], wt);
				if (weight > 0 ) {
					lut_r[k] = (int) (Vol(cube[k], mr) / weight);
					lut_g[k] = (int) (Vol(cube[k], mg) / weight);
					lut_b[k] = (int) (Vol(cube[k], mb) / weight);
				}
				else{
					//IJ.log("bogus box " + k);
					lut_r[k] = lut_g[k] = lut_b[k] = 0;		
				}
			}

			for(int i=0; i<size; ++i) {
				Qadd[i] = tag[Qadd[i]];
			}

//			// no search
//			for (int i=0; i<size; i++) {
//			int r = lut_r[Qadd[i]]; 
//			int g = lut_g[Qadd[i]]; 
//			int b = lut_b[Qadd[i]]; 

//			pixels[i] = (255<<24) | (r  << 16) | (g << 8) | b;	
//			}



			int [] red = new int[K+1];
			int [] green = new int[K+1];
			int [] blue = new int[K+1];
			int [] sum = new int[K+1];

			int [] ind = new int[size];
			for (int i=0; i<size; i++) {
				int c = pixels[i];

				if ((c & 0xFF000000) == 0xFF000000 ) {

					int k = Qadd[i];
					int r = ((c >> 16)& 0xff);
					int g = ((c >> 8 )& 0xff);
					int b = ( c       & 0xff);

					int kb = k;
					int eb = 100000000;
					for (int l = 0; l < K; l++ ) {
						int lr = lut_r[l];
						int lg = lut_g[l];
						int lb = lut_b[l];
						int dr = r - lr;
						int dg = g - lg;
						int db = b - lb;

						int e = dr*dr+dg*dg+db*db;
						if (e < eb) {
							eb = e;
							kb = l;
						}
					}

					red[kb]   += r; 
					green[kb] += g; 
					blue[kb]  += b;
					sum[kb] ++;

					ind[i] = kb; //(255<<24) | (r  << 16) | (g << 8) | b;
				}
			}


			for (int k=0; k<K; k++) {
				if (sum[k] > 0) {
					red[k]  /= sum[k];
					green[k] /= sum[k];
					blue[k] /= sum[k];
					//IJ.log("wu : " + k + " R:" + red[k] + " G: " + green[k] + " B: " + blue[k]);
				}
			}
			for (int i=0; i<size; i++) {
				int c = pixels[i];
				if ((c & 0xFF000000) == 0xFF000000 ) {
					int k = ind[i];
					int r = red[k]; 
					int g = green[k]; 
					int b = blue[k]; 
					pixels[i] = (255<<24) | (r  << 16) | (g << 8) | b;
				}
			}
		}

		class Box {
			int r0;			 // min value, exclusive 
			int r1;			 // max value, inclusive 
			int g0;  
			int g1;  
			int b0;  
			int b1;
			int vol;	
		}
	}

	
}
