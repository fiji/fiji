
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import jRenderer3D.JRenderer3D;
import jRenderer3D.Line3D;
import jRenderer3D.Text3D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.Kernel;
import java.awt.image.MemoryImageSource;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * Interactive_3D_SurfacePlot (was SurfacePlot_3D) 
 *
 * (C) Author:  Kai Uwe Barthel: barthel (at) fhtw-berlin.de 
 * 
 * Version 2.31
 * 			2008 August 17
 * 			- new macro key "snapshot" will copy the redered plot to an ImageJ image and exit
 * 
 * Version 2.3
 * 			2008 May 12
 * 			- added legend
 * 			- plugin is now scriptable and 
 * 			- saves preferences 
 * 
 * Version 2.23
 * 			2007 Nov. 5
 * 			Key "s" saves the plot
 * 			renamed z-Ratio to z-Scale
 * 
 * Version 2.22 
 * 			2007 Nov. 2
 * 			- axes may be labeled with number and units  
 * 			- z-ratio can be selected to be equal to the xy-ratio or it may be adapted automatically
 * 			- fixed a bug which could cause the frame to be resized on start-up 
 * 			  leading to wrong sizes, display or even crashes
 * 
 * Version 2.1 
 * 			2007 Jan. 15
 * 			corrected plots for selections
 * 			faster filled mode
 * 			gradient coloring	
 * 			isolines (may be improved)
 * 			(changed all transform data to double)
 * 
 * Version 2.0 (.01)
 * 			2007 Jan. 9
 * 			resizabel canvas 
 * 			scaling options
 * 			
 * 
 * Version 1.5 (.03)
 * 			2005 Oct. 10
 * 			texture mapping
 * 			aspect ratio is preserved
 * 
 * Version 1.4
 * 			surface plot image can be converted to an ImageJ image
 * 			better interpolation for lut-colors, faster filled mode
 * 
 * Version 1.33 
 * 			better lighting, better interpolation for Luts
 
 * 
 * Version 1.3			November, 26 2004
 * 			Smoothing and ligthing of the plot, better "filled" mode 
 * 
 * Version 1.2			November, 20 2004
 * 			only the bounding box of the selection is taken for the plot
 * 			8Bit Color fixed, 
 * 			Mesh Mode, Fill Mode fixed, better scaling
 * 
 * Version 1.0           November, 19 2004
 * 		First version
 *
 */

public class Interactive_3D_Surface_Plot implements PlugIn, MouseListener, MouseMotionListener, ItemListener{
	
	private final String version = " v2.31 ";

	
	// constants
	private final int DOTS = 0;
	private final int LINES = 1;
	private final int MESH = 2;
	private final int FILLED = 3;
	private final int ISOLINES = 4;
	private int plotType = LINES;
	
	private final String DOTS_PLOT = "Dots";
	private final String LINES_PLOT = "Lines";
	private final String MESH_PLOT = "Mesh";
	private final String FILLED_PLOT = "Filled";
	private final String ISOLINES_PLOT = "Isolines";
	
	private final static int ORIGINAL = 0;
	private final static int GRAYSCALE = 1;
	private final static int SPECTRUM = 2;
	private final static int FIRE = 3;
	private final static int THERMAL = 4;
	private final static int GRADIENT = 5;
	private final static int BLUE = 6;
	private final static int ORANGE = 7;
	private int colorType = ORIGINAL;
	
	private final String ORIGINAL_COLORS = "Original Colors";
	private final String GRAYSCALE_LUT = "Grayscale";
	private final String SPECTRUM_LUT = "Spectrum LUT";
	private final String FIRE_LUT = "Fire LUT";
	private final String THERMAL_LUT = "Thermal LUT";
	private final String GRADIENT_COLORS = "Gradient";
	private final String ORANGE_LUT = "Orange";
	private final String BLUE_LUT = "Blue";

	
	// application window
	private JFrame frame;
	
	
	// panels
	private JPanel mainPanel;
	private JPanel settingsPanel1;
	private JPanel settingsPanel2;
	
	
	// setting components
	private JComboBox comboDisplayType;
	private JComboBox comboDisplayColors;
	
	private JSlider sliderLight;
	private JSlider sliderGridSize;
	private JSlider sliderSmoothing;
	private JSlider sliderScale;	
	private JSlider sliderZAspectRatio;	
	private JSlider sliderPerspective;
	private JSlider sliderMin;	
	private JSlider sliderMax;	
	
	private JCheckBox checkInverse, checkIsEqualxyzRatio;
	
	
	// image panel / canvas
	private ImageRegion imageRegion;
	
	
	// imageJ components
	private ImagePlus image;	
	
	
	// imgeJ3D API components
	private JRenderer3D jRenderer3D;
	
	
	// other global params 
	final static int SIZE = 600;
	private int windowWidth = (int) (SIZE*1.1);
	private int windowHeight = SIZE;
	private int startWindowWidth = windowWidth;
	private int startWindowHeight = windowHeight;

	private double scaleWindow = 1.; // scaling caused by the resize 

	private int xStart;
	private int yStart;
	private boolean drag;
	private int xdiff;
	private int ydiff;
	private double light = 0.2;
	private double smoothOld = 0;

	private boolean invertZ = false;

	private boolean isExamplePlot = false;

	private int imageWidth;
	private int imageHeight;

	private double scaleInit = 1;
	private double zRatioInit = 1;

	private double scaledWidth;
	private double scaledHeight;

	private double minVal;
	private double maxVal;

	private String units;

	private double maxDistance;

	private Calibration cal;

	private boolean isEqualxyzRatio = true;

	private double zAspectRatioSlider = 1;
	private double zAspectRatio = 1;
	
	private double scaleSlider = 1;

	private double minZ;
	private double maxZ;

	protected boolean draftDrawing = true;

	private int xloc;
	private int yloc;
	private int grid = 128;
	private double smooth = 3;
	private double perspective = 0;

	private boolean drawText = true;
	private boolean drawLegend = true;
	private boolean drawAxes = true;
	private boolean drawLines = true;

	private double rotationX = 65;
	private double rotationZ = 39;
	
	private boolean doReset = false;
	
	private int minSlider = 0;
	private int maxSlider = 100;


	private boolean snapshot = false;

	
	
	public static void main(String args[]) {
		Interactive_3D_Surface_Plot sp = new Interactive_3D_Surface_Plot();
		
		new ImageJ(); // !!!
//		
//		//IJ.open("/users/barthel/Desktop/Depth_Image.tif");
//		//		IJ.open("/users/barthel/Desktop/K2.tif");
//		IJ.open("/Users/barthel/Pictures/Beispielbilder/plot2.tif");
//		//IJ.open("/users/barthel/Desktop/image_128-1.png");
//		//IJ.open("/users/barthel/Desktop/Dot_Blot.jpg");
		IJ.open("/users/barthel/Applications/ImageJ/_images/blobs.tif");
//		IJ.run("Set Scale...", "distance=1.001 known=100 pixel=1 unit=µm");
//		//IJ.run("Set Scale...", "distance=2.2 known=5 pixel=1 unit=µm");
		IJ.run("Set Scale...", "distance=12 known=100 pixel=1 unit=µm");
//		//IJ.run("Set Scale...", "distance=30 known=0.5 pixel=1 unit=µm");
//		//IJ.run("Fire");
//		//IJ.makeRectangle(80, 80, 4, 5);
//		IJ.makeOval(40, 40, 120, 150);
//		//IJ.run("makePolygon(98,147,163,92,243,127,206,228,147,197,150,180)");
//		//IJ.makeLine(80, 80, 80,80);
//		
//		
//		
		sp.image = IJ.getImage();
//		
//		int[] xpoints = new int[]{98,163,243,206,147,150};
//		int[] ypoints = new int[]{147,92,127,228,197,180};
//		Roi roi1 = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
//		sp.image.setRoi(roi1);
//		
//		Roi roi = sp.image.getRoi();
//
//		if (roi != null) {
//			Rectangle rect = roi.getBoundingRect();
//			if (rect.x < 0)
//				rect.x = 0;
//			if (rect.y < 0)
//				rect.y = 0;
//			sp.imageWidth = rect.width;
//			sp.imageHeight = rect.height;
//			
//			if (sp.imageWidth == 0 ||sp.imageHeight == 0) {
//				sp.image.killRoi();
//				sp.imageWidth = sp.image.getWidth();
//				sp.imageHeight = sp.image.getHeight();
//			}
//		}
//		else {
			sp.imageWidth = sp.image.getWidth();
			sp.imageHeight = sp.image.getHeight();
//		}
//		
////		sp.image.show();
////		sp.image.updateAndDraw();
		
		
		
	//	sp.generateSampleImage();

		//sp.run("");	
		sp.runApplication("Example Plot");
	}
	
	private void generateSampleImage() {
		
		imageWidth = 256;
		imageHeight = 256;
		int [] pixels = new int [imageWidth*imageHeight];
		
		for (int y = 0; y < imageHeight; y++) {
			int dy1 = y - 80;
			int dy2 = y - 60;
			
			for (int x = 0; x < imageWidth; x++) {
				int dx1 = x - 90;
				int dx2 = x - 180;
				double r1 = Math.sqrt(dx1*dx1+dy1*dy1)/60;
				double r2 = Math.sqrt(dx2*dx2+dy2*dy2)/100;
				
				int v1 = (int) (255*Math.exp(-r2*r2));
				int v2 = (int) (255*Math.exp(-r1*r1) );
				
	 			pixels[y*imageWidth+x] = 0xFF000000 | ((int)(v2+v1)<<16) | (int)((v2) << 8 ) | (y/4);
			}	
		}
		MemoryImageSource source = new MemoryImageSource(imageWidth, imageHeight, pixels, 0, imageWidth);
		
		Image awtImage = Toolkit.getDefaultToolkit().createImage(source);
		
		BufferedImage bufferedImage =  new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = bufferedImage.createGraphics();
		
		g2D.drawImage(awtImage, 0, 0, null);
		
		float ninth = 1.0f / 9.0f;
		float[] blurKernel = {
		        ninth, ninth, ninth,
		        ninth, ninth, ninth,
		        ninth, ninth, ninth,
		};
		    
		BufferedImageOp op = new ConvolveOp( new Kernel(3, 3, blurKernel));
		bufferedImage = op.filter(bufferedImage, null);
		bufferedImage = op.filter(bufferedImage, null);
		g2D = bufferedImage.createGraphics();
		g2D.setColor(new Color(0x3300FF));
		Font font = new Font("Sans", Font.BOLD, 60);
		g2D.setFont(font);
		g2D.drawString("ImageJ", 20, 220); 
		
		bufferedImage = op.filter(bufferedImage, null);
		bufferedImage = op.filter(bufferedImage, null);
		
		g2D.dispose();
		
		image = new ImagePlus("Example Plot", bufferedImage);
		isExamplePlot = true;
	}
	
	
	public void run(String arg) {
		if (IJ.versionLessThan("1.36b")) return;
					
		image = WindowManager.getCurrentImage();
		
		if (image!=null) {
			Roi roi = image.getRoi();

			if (roi != null) {
				Rectangle rect = roi.getBoundingRect();
				if (rect.x < 0)
					rect.x = 0;
				if (rect.y < 0)
					rect.y = 0;
				imageWidth = rect.width;
				imageHeight = rect.height;
				
				if (imageWidth == 0 || imageHeight == 0) {
					image.killRoi();
					imageWidth = image.getWidth();
					imageHeight = image.getHeight();
				}
			}
			else {
				imageWidth = image.getWidth();
				imageHeight = image.getHeight();
			}
		}
		else {
			generateSampleImage();
		}
		
		runApplication(image.getTitle());	
	}
		

	private void runApplication(String name) {	
		// create window/frame 
		String str = "Interactive 3D Surface Plot" + version + " (" + name +")";
		
		frame = new JFrame(str);
		if (!doReset ) {
			//if (!calledFromMacro)
			readPrefs();
		}
		str = Macro.getOptions();
		
//		IJ.log("Options: " + str);
//		str = "light=0.2 perspective=0.22 grid=64 plotType=3 smoot=11 colorType=3 min=30 max=60 scale=1.2 scaleZ=0.7";
		
		// read macro parameters
		try {
			if (str != null) {
				StringTokenizer ex1; // Declare StringTokenizer Objects
				ex1 = new StringTokenizer(str); //Split on Space (default)
				
				String[] params   = { "light=", "perspective=", "grid=", "smooth=", "plotType=", "colorType=", "drawAxes=", "drawLines=",
						"drawText=","drawLegend=", "invertZ=", "isEqualxyzRatio=", "rotationX=", "rotationZ=", "scale=", "scaleZ=",	"min=", "max=", "snapshot="};
				
				double[] paramVals = { light, perspective, grid, smooth, plotType, colorType, (drawAxes == true) ? 1 : 0, (drawLines == true) ? 1 : 0,
						(drawText == true) ? 1 : 0, (drawLegend == true) ? 1 : 0, (invertZ == true) ? 1 : 0, (isEqualxyzRatio == true) ? 1 : 0,
						rotationX, rotationZ, scaleSlider, zAspectRatioSlider, minSlider, maxSlider, (snapshot == true) ? 1 : 0};
				
				String errorString=null;
				while (ex1.hasMoreTokens()) {
					boolean found = false;
					str = ex1.nextToken();
					for (int j = 0; j<params.length; j++) {
						String pattern = params[j];
						if (str.lastIndexOf(pattern) > -1) { 
							int pos = str.lastIndexOf(pattern) + pattern.length();
							paramVals[j] = Double.parseDouble(str.substring(pos));
							found = true;
//							IJ.log("" + params[j] + ": " + paramVals[j]); 
						}
						if (!found)
							errorString = str;
					}
					if (!found)
						macroError(errorString);
				}
				
				light = Math.min(1, Math.max(0,paramVals[0]));
				perspective = Math.min(1, Math.max(0,paramVals[1]));
				grid = (int) Math.min(1024, Math.max(10,paramVals[2]));
				smooth = paramVals[3];
				plotType = (int) paramVals[4];
				colorType = (int) paramVals[5];
				drawAxes  = (paramVals[6] == 1) ? true : false;
				drawLines  = (paramVals[7] == 1) ? true : false;
				drawText  = (paramVals[8] == 1) ? true : false;
				drawLegend  = (paramVals[9] == 1) ? true : false;
				invertZ  = (paramVals[10] == 1) ? true : false;
				isEqualxyzRatio  = (paramVals[11] == 1) ? true : false;
				rotationX = paramVals[12];
				rotationZ = paramVals[13];
				scaleSlider = Math.min(3, Math.max(0.25,paramVals[14]));
				zAspectRatioSlider = Math.min(10, Math.max(0.1,paramVals[15]));
				minSlider = (int) Math.min(99, Math.max(0,paramVals[16]));
				maxSlider = (int) Math.min(100, Math.max(1,paramVals[17]));
				snapshot = (paramVals[18] == 1) ? true : false;
			}
		} catch (NumberFormatException e1) {
			macroError(" Incorrect parameter ! ");
		}

		doReset = false;

		// create application gui
		createGUI();				
		
		frame.setLocation(xloc, yloc);

		// creates the 3d renderer // NOTE: image must be loaded
		create3DRenderer();

		frame.pack();
		
		frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                writePrefs();
                e.getWindow().dispose();
                //WindowManager.removeWindow (frame); // if you add ISP3D to windowmanager
            }
        });
		
		if (snapshot) {
			imageRegion.saveToImageJImage(image.getShortTitle());
			frame.dispose();
		}
	}
	
    private void macroError(String errorStr) {
    	String str = 
    		"Valid macro parameters are:\n" +
    		" \n" +
    		"Keyword=Default    [Range]\n" +
    		" \n" +
    		"plotType=1              [0 .. 4]\n" +
    		"colorType=0            [0 .. 7]\n" +
    		"drawAxes=1            [0 / 1]\n" +
    		"drawLines=1            [0 / 1]\n" +
    		"drawText=1             [0 / 1]\n" +
    		"drawLegend=1        [0 / 1]\n" +
    		"grid=128                  [16 .. 512]\n" +
    		"smooth=0                [0 .. 100]\n" +
    		"perspective=0.1       [0 .. 1]\n" +
    		"light=0.2                   [0 .. 1]\n" +
    		"isEqualxyzRatio=0   [0 / 1]\n" +
    		"invertZ=0                  [0 / 1]\n" +
    		"rotationX=65           [-180 .. 180]\n" +
    		"rotationZ=-22.5      [-180 .. 180]\n" +
    		"scale=1                     [0.25 .. 3]\n" +
    		"scaleZ=1                   [0.1  .. 5]\n" +
    		"max=100                 [1 .. 100]\n" +
    		"min=0                       [0 .. 99]             \n" +
    		"snapshot=0        [0 / 1]\n" +
    		" \n" +
    		"Error: \"" + errorStr + "\"";

    	IJ.showMessage("Error in macro parameter list!", str); 
   }

	private void writePrefs() {
        Prefs.set("ISP3D.xloc", frame.getLocation ().x);
        Prefs.set("ISP3D.yloc", frame.getLocation().y);
        Prefs.set("ISP3D.light", light);
        Prefs.set("ISP3D.perspective", perspective);
        Prefs.set("ISP3D.grid", grid);
        Prefs.set("ISP3D.smooth", smooth);
        Prefs.set("ISP3D.plotType", plotType);
        Prefs.set("ISP3D.colorType", colorType);
        Prefs.set("ISP3D.drawAxes", drawAxes);
        Prefs.set("ISP3D.drawLines", drawLines);
        Prefs.set("ISP3D.drawText", drawText);
        Prefs.set("ISP3D.drawLegend", drawLegend);
        Prefs.set("ISP3D.invertZ", invertZ);
    	Prefs.set("ISP3D.isEqualxyzRatio", isEqualxyzRatio);
    	Prefs.set("ISP3D.rotationX", rotationX);
    	Prefs.set("ISP3D.rotationZ", rotationZ);
    	Prefs.set("ISP3D.windowHeight", windowHeight);
    	Prefs.set("ISP3D.windowWidth", windowWidth);
    	Prefs.set("ISP3D.scale", scaleSlider);
    	Prefs.set("ISP3D.zScale", zAspectRatioSlider);
    	Prefs.set("ISP3D.min", minSlider);
    	Prefs.set("ISP3D.max", maxSlider);	
    }
    
    private void readPrefs() {
        if (isExamplePlot) {  	

        	// get screen dimensions
    		Toolkit toolkit = Toolkit.getDefaultToolkit();
    		Dimension screenSize = toolkit.getScreenSize();
    		int screenWidth = screenSize.width;
    		int screenHeight = screenSize.height;	
        	
        	Insets ins = frame.getInsets();
        	xloc = (screenWidth-windowWidth-ins.left-ins.right -  70)/2;
        	yloc = (screenHeight-windowHeight-ins.bottom-ins.top - 75)/2;
        	light = 0.2;
        	perspective = 0.1;
        	grid = 128;
        	smooth = 6;
        	plotType = FILLED;
        	colorType = GRADIENT;
        	drawAxes = true;
        	drawLines = true;
        	drawText = true;
        	drawLegend = true;
        	invertZ = false;
        	isEqualxyzRatio = false;
        	rotationX =  65;    
        	rotationZ =  -22.5;  
        	scaleSlider = 1;
        	zAspectRatioSlider = 1;
        	minSlider = 0;
        	maxSlider = 100;
        }
        else {
        	xloc = (int) Prefs.get("ISP3D.xloc", 100);
        	yloc = (int) Prefs.get("ISP3D.yloc", 50);
        	light = Prefs.get("ISP3D.light", 0.2);
        	perspective = Prefs.get("ISP3D.perspective", 0);
        	grid = (int) Prefs.get("ISP3D.grid", 128);
        	smooth = Prefs.get("ISP3D.smooth", 0);
        	plotType = (int) Prefs.get("ISP3D.plotType", LINES);
        	colorType = (int) Prefs.get("ISP3D.colorType", ORIGINAL);
        	drawAxes = Prefs.get("ISP3D.drawAxes", true);
        	drawLines = Prefs.get("ISP3D.drawLines", true);
        	drawText = Prefs.get("ISP3D.drawText", true);
        	drawLegend = Prefs.get("ISP3D.drawLegend", true);
        	invertZ = Prefs.get("ISP3D.invertZ", false);
        	isEqualxyzRatio = Prefs.get("ISP3D.isEqualxyzRatio", false);
        	rotationX =  Prefs.get("ISP3D.rotationX", 65);    
        	rotationZ =  Prefs.get("ISP3D.rotationZ", 39);
        	windowHeight = (int) Prefs.get("ISP3D.windowHeight", windowHeight);
        	windowWidth = (int) Prefs.get("ISP3D.windowWidth", windowWidth);
        	scaleSlider = Prefs.get("ISP3D.scale", scaleSlider);
        	zAspectRatioSlider = Prefs.get("ISP3D.zScale", zAspectRatioSlider);
        	minSlider = (int) Prefs.get("ISP3D.min", minSlider);	
        	maxSlider = (int) Prefs.get("ISP3D.max", maxSlider);	
        } 
    }
	
	/********************************************************
	 * 														*
	 *						3D RENDERER						*						
	 *														*
	 ********************************************************/	
	
	/**
	 * Initializes the JRenderer3D. Set Background, the surface plot, plot mode, lightning mode.
	 * Adds a coordinate system. Sets scale. Renders and updates the image.
	 */		
	private void create3DRenderer(){
		
		double wc = (imageWidth)/2.;
		double hc = (imageHeight)/2.;
		double dc = 256/2.;
		
		cal = image.getCalibration();
		
		ImageProcessor ip = image.getProcessor();
		
		scaledWidth = cal.getX(imageWidth);
		scaledHeight = cal.getY(imageHeight);
		
		minVal = ip.getMin();
		maxVal = ip.getMax();
		units = cal.getUnits();
		
//		IJ.log("Units: " + units);
//		IJ.log("X: " + scaledWidth);
//		IJ.log("Y: " + scaledHeight);
//		IJ.log("scaled: " + cal.scaled());
//		//IJ.log("Ratio: " + cal.());
//		IJ.log("Min: " + minVal);
//		IJ.log("Max: " + maxVal);
		
		
		// create 3D renderer
		// center in the middle of the image
		jRenderer3D = new JRenderer3D(wc, hc, dc);	
		jRenderer3D.setBufferSize(windowWidth, windowHeight);
		
		setScaleAndZRatio(); 
		
		int gridHeight, gridWidth;;					
		if (imageHeight > imageWidth) {
			gridHeight = grid;
			gridWidth = grid*imageWidth/imageHeight;
		}
		else {
			gridWidth  = grid;
			gridHeight = grid*imageHeight/imageWidth;
		}
			
		jRenderer3D.setSurfacePlotGridSize(gridWidth,gridHeight);
		
		jRenderer3D.setAxes(drawAxes);
		jRenderer3D.setLines(drawLines);
		jRenderer3D.setText(drawText);
		jRenderer3D.setLegend(drawLegend);
		
		// surface plot
		jRenderer3D.setSurfacePlot(image);
		jRenderer3D.surfacePlotSetInverse(invertZ);
		
		jRenderer3D.setTransformRotationXYZ(rotationX, 0, rotationZ); // viewing angle (in degrees)
		
		jRenderer3D.setSurfaceSmoothingFactor(smooth);
		jRenderer3D.setSurfacePlotLight(light);
		jRenderer3D.setSurfacePlotMinMax(minSlider, maxSlider);
		
		setSurfaceColorType(colorType); 
		setSurfacePlotType(plotType);
		

		renderAndUpdateDisplay();
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {}
		renderAndUpdateDisplay();
	}
	
	private void setScaleAndZRatio() {
		if (isEqualxyzRatio) {
			zRatioInit  = (maxVal - minVal) /(255 * scaledWidth/imageWidth) ;  
		
			//  determine initial scale factor 
			scaleInit = 0.55* Math.max(startWindowHeight, startWindowWidth) / (double)Math.max(imageWidth, Math.max(255*zRatioInit, imageHeight)); 
		}
		else {
			scaleInit = 0.55* Math.max(startWindowHeight, startWindowWidth) / (double)Math.max(imageHeight, imageWidth); 
			zRatioInit  = 0.55* startWindowHeight/(256*scaleInit);  
		}	
		
		zAspectRatio = zRatioInit * zAspectRatioSlider;
		
		scaleWindow = Math.min(windowHeight, windowWidth)/(double)startWindowHeight;
		
		//IJ.log("Z-Ratio: " + zRatioInit);
		
		jRenderer3D.setTransformZAspectRatio(zAspectRatio);
		scaleSlider = sliderScale.getValue() / 100.; 
		
		double scale = scaleInit * scaleSlider * scaleWindow;
		//IJ.log("ScaleInit: " + scaleInit + " ScaleWindow " + scaleWindow + " Scale " + scale);
		
		jRenderer3D.setTransformScale(scale);
		
		jRenderer3D.setTransformPerspective(sliderPerspective.getValue()/100.);
		
		maxDistance = Math.max(scaledWidth, Math.max(scaledHeight, 256*Math.max(zAspectRatio,1)));
		jRenderer3D.setTransformMaxDistance(maxDistance);
		
		addCoordinateSystem();
		
	}

	/********************************************************************
	 * Draws a simple coordinate system and labels it
	 ********************************************************************/
	private void addCoordinateSystem(){
		
		jRenderer3D.clearText();
		jRenderer3D.clearLines();	
		jRenderer3D.clearCubes();
		
		int id = 256;
		
		minZ = minVal + minSlider/100. * (maxVal-minVal);
		maxZ = maxVal - (100. - maxSlider)/100 * (maxVal-minVal);
//		System.out.println("MinS " + minS + " MaxS " + maxS + " MinVal " + minVal + " MaxVal " + maxVal + " min " + min );
//		System.out.println("scaleInit " + scaleInit + " zRatioInit " + zRatioInit );

//		System.out.println("MinZ " + minZ + " MaxZ " + maxZ );
		jRenderer3D.setMinZValue(minZ);
		jRenderer3D.setMaxZValue(maxZ);
		

		// add text to the coordinate system
		
		double off =  12 / scaleInit;  // text position offset
		double fontSize = 10 / scaleInit;
		double offZ = off/zAspectRatio;
		int ticksDist = 40;
		Color textColor = Color.white;
		Color lineColor = new Color(90,90,100);
		
		double x1 = 0;
		double y1 = 0;
		double z1 = 0;
		double x2 = imageWidth; 
		double y2 = imageHeight; 
		double z2 = id; 
		
		int numTicks = (int) Math.round(imageHeight*scaleInit / ticksDist);
		double pos=0;
		double stepValue = calcStepSize(scaledHeight, numTicks);
		
		for (double value=0; value<=scaledHeight; value += stepValue) {
			String s;
			if (Math.floor(value) - value == 0)
				s = "" + (int)value;
			else
				s = "" + (int)Math.round(value*1000)/1000.;
			// unit String for the last position
			if (value + stepValue > scaledHeight || value == scaledHeight) {
				if (!units.equals("pixels"))
					s = "y/" + units;
				else
					s = "y";
			}
			
			pos = (value * imageHeight / scaledHeight);
			y1 = y2 = pos;
			
			jRenderer3D.addText3D(new Text3D(s,  x1-off,  y1,  z1-offZ, textColor, fontSize, 2));
			jRenderer3D.addText3D(new Text3D(s,  x2+off,  y2,  z1-offZ, textColor, fontSize));
			
			jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x1, y1, z2, lineColor, true));
			jRenderer3D.addLine3D(new Line3D(x2, y2, z1, x2, y2, z2, lineColor));			
			
			jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x2, y2, z1, lineColor, true));	
			jRenderer3D.addLine3D(new Line3D(x1, y1, z2, x2, y2, z2, lineColor));	
		}

		numTicks = (int) Math.round(imageWidth*scaleInit / ticksDist);
		stepValue = calcStepSize(scaledWidth, numTicks);
		
		y1 = 0;
		y2 = imageHeight;
		for (double value=0; value<=scaledWidth; value += stepValue) {
			String s;
			if (Math.floor(value) - value == 0)
				s = "" + (int)value;
			else
				s = "" + (int)Math.round(value*1000)/1000.;
			if (value + stepValue > scaledWidth || value == scaledWidth) {
				if (!units.equals("pixels"))
					s =  "x/" + units ;
				else
					s = "x";
			}
			
			pos = value*imageWidth / scaledWidth;
			x1 = x2 = pos;
			
			jRenderer3D.addText3D(new Text3D(s,  x1,  y1-off,  z1-offZ, textColor, fontSize, 2));
			jRenderer3D.addText3D(new Text3D(s,  x2,  y2+off,  z1-offZ, textColor, fontSize));
			
			jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x1, y1, z2, lineColor, true));
			jRenderer3D.addLine3D(new Line3D(x2, y2, z1, x2, y2, z2, lineColor));		
			
			jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x2, y2, z1, lineColor, true));		
			jRenderer3D.addLine3D(new Line3D(x1, y1, z2, x2, y2, z2, lineColor));		
		}
			
		double d = maxZ-minZ;
		numTicks = (int) Math.round(255*zAspectRatio*scaleInit / (ticksDist/1.3));
		//System.out.println("numTicks: " + numTicks);
		if (numTicks <= 2)
			numTicks = 3;
		stepValue = calcStepSize(d, numTicks);
		
		x1 = 0;
		y1 = 0;
		x2 = imageWidth;
		y2 = imageHeight;
		
		double minStart = Math.floor(minZ/stepValue)*stepValue;
		double delta = minStart - minZ;
		
		for (double value=0; value+delta<=d; value += stepValue) {
			String s;
			if (/*Math.abs(value) > 1 && */Math.floor(minStart+value) - (minStart+value) == 0)
				s = "" + (int)(minStart+value);
			else
				s = "" + (int)Math.round((minStart+value)*1000)/1000.;
			
			if (value + stepValue > d || value == d) {
				s = "z";
			}
			pos = ((value+delta) * id / d);
			if (pos >= 0) {
				z1 = z2 = pos;
				if (invertZ)
					z1 = z2 = 255-pos;
				jRenderer3D.addText3D(new Text3D(s,  x1-off,  y1-off,  z1, textColor, fontSize, 4));
				jRenderer3D.addText3D(new Text3D(s,  x2+off,  y2+off,  z2, textColor, fontSize));
				jRenderer3D.addText3D(new Text3D(s,  x1-off,  y2+off,  z1, textColor, fontSize));
				jRenderer3D.addText3D(new Text3D(s,  x2+off,  y1-off,  z2, textColor, fontSize));

				jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x1, y2, z2, lineColor, true));
				jRenderer3D.addLine3D(new Line3D(x2, y1, z1, x2, y2, z2, lineColor));		

				jRenderer3D.addLine3D(new Line3D(x1, y1, z1, x2, y1, z2, lineColor, true));
				jRenderer3D.addLine3D(new Line3D(x1, y2, z1, x2, y2, z2, lineColor));			
			}
		}
		
		// add coordinate system
		jRenderer3D.add3DCube(0, 0, 0, imageWidth, imageHeight, id, Color.white);
	}
	
	double calcStepSize( double range, double targetSteps )
	{
	    // Calculate an initial guess at step size
	    double tempStep = range / targetSteps;

	    // Get the magnitude of the step size
	    double mag = Math.floor( Math.log(tempStep)/Math.log(10.) );
	    double magPow = Math.pow( (double) 10.0, mag );

	    // Calculate most significant digit of the new step size
	    double magMsd =  ( (int) (tempStep / magPow + .5) );

	    // promote the MSD to either 1, 2, 4, or 5
	    if ( magMsd > 6 ) // 5
	        magMsd = 10.0;
	    else if ( magMsd > 3 )
	        magMsd = 5.0;
	    else if ( magMsd > 2 )
	        magMsd = 4.0;
	    else if ( magMsd > 1 )
	        magMsd = 2.0;

	    return magMsd * magPow;
	}
	
	/**
	 * Renders and updates the 3D image.
	 * Image region is repainted.
	 *
	 */
	private void renderAndUpdateDisplay() {
		jRenderer3D.doRendering();
		imageRegion.setImage(jRenderer3D);
		imageRegion.repaint();
	}	
	
	/********************************************************
	 * 														*
	 *					ACTION LISTENERS					*						
	 *														*
	 ********************************************************/	

	public void mouseClicked(MouseEvent arg0) {
		Object source = arg0.getSource();
		if (source == imageRegion) { // top view
			imageRegion.requestFocus();
			if(arg0.getClickCount() == 2) {
				jRenderer3D.setTransformRotationXYZ(0,0,0);
				renderAndUpdateDisplay();
			}	
			else if(arg0.getClickCount() >= 3) {
				jRenderer3D.setTransformRotationXYZ(90,0,0);
				renderAndUpdateDisplay();
			}	
		}
	}
	
	public void mouseMoved(MouseEvent arg0) {}
	
	public void mousePressed(MouseEvent arg0) {
		Object source = arg0.getSource();
		
		if (source == imageRegion) {
			xStart = arg0.getX();
			yStart = arg0.getY();
			xdiff = 0;
			ydiff = 0;
			drag = true;
			//renderAndUpdateDisplay();	
		}	
	}

	public void mouseReleased(MouseEvent arg0) {
		//Object source = arg0.getSource();
		drag = false;	
		setSurfacePlotType(plotType);
		setSurfaceColorType(colorType);
		jRenderer3D.setSurfacePlotLight(light);
		renderAndUpdateDisplay();		
	}
	
	public void mouseDragged(MouseEvent arg0) {
		Object source = arg0.getSource();
		if (source == imageRegion) {
			if (drag == true) {
				if (draftDrawing)
					jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_DOTSNOLIGHT);
				
				int xAct = arg0.getX();
				int yAct = arg0.getY();
				xdiff = xAct - xStart;
				ydiff = yAct - yStart;
				xStart = xAct;
				yStart = yAct;
				
				//jRenderer3D.applyTransformRotationXYZ(-ydiff/2., xdiff/2., 0);
				jRenderer3D.changeTransformRotationXZ(-ydiff/2., xdiff/2.);
				rotationX = jRenderer3D.getTransformRotationX();
				rotationZ = jRenderer3D.getTransformRotationZ();
							
				renderAndUpdateDisplay();	
				imageRegion.requestFocus();
			}
		}		
	}

	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	
	
	/**
	 * Sets the surface plot mode. 
	 *
	 */		
	private void setSurfacePlotType(int type) {		
		if (type == DOTS)		
			jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_DOTS);
		else if (type == LINES)	
			jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_LINES);
		else if (type == MESH)		
			jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_MESH);
		else if (type == ISOLINES)	
			jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_ISOLINES);
		else if (type == FILLED)	
			jRenderer3D.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_FILLED);
	}

	
	/**
	 * Sets the surface color type. 
	 *
	 */		
	private void setSurfaceColorType(int type) {	
		colorType = type;
		if (type == ORIGINAL) 	
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_ORIGINAL);
		else if (type == GRAYSCALE) 	
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_GRAY);
		else if (type == SPECTRUM) 	
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_SPECTRUM);
		else if (type == FIRE)			
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_FIRE);
		else if (type == THERMAL)		
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_THERMAL);
		else if (type == GRADIENT)		
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_GRADIENT);
		else if (type == ORANGE)		
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_ORANGE);
		else if (type == BLUE)		
			jRenderer3D.setSurfacePlotLut(JRenderer3D.LUT_BLUE);
	}		
	
	
	
	
	/**
	 * Updates illumination, smoothing and scaling. Renders and updates the image.
	 *
	 */		
	private void sliderChange(JSlider slider) {		
		if (slider == sliderLight) {
			light = sliderLight.getValue() / 100.;
			
			jRenderer3D.setSurfacePlotLight(light);
			String str = "Lighting: " + light; 
			setSliderTitle(sliderLight, Color.black, str );
			
		}
		else if (slider == sliderGridSize) {
			grid  = 1 << sliderGridSize.getValue(); 
			
			int gridHeight, gridWidth;					
			if (imageHeight > imageWidth) {
				gridHeight = grid;
				gridWidth = grid*imageWidth/imageHeight;
			}
			else {
				gridWidth  = grid;
				gridHeight = grid*imageHeight/imageWidth;
			}
			
			jRenderer3D.setSurfacePlotGridSize(gridWidth, gridHeight);
			//jRenderer3D.surfacePlotSetInverse(invertZ);
			
			smooth = sliderSmoothing.getValue() * (grid / 512.);
			if (!slider.getValueIsAdjusting() || grid <= 256) {
				jRenderer3D.setSurfaceSmoothingFactor(smooth);
				smoothOld = smooth;
			}
			String str = "Grid Size: " + grid; 
			setSliderTitle(sliderGridSize, Color.black, str );
			
			str = "Smoothing: " + (int)(smooth*100)/100.; 
			setSliderTitle(sliderSmoothing, Color.black, str );
		}
		else if (slider == sliderSmoothing) {
			grid = 1 << sliderGridSize.getValue(); 
			smooth = sliderSmoothing.getValue() * (grid / 512.);
			
			if (smooth != smoothOld && (!slider.getValueIsAdjusting() || (1 << sliderGridSize.getValue()) <= 256)) {
				jRenderer3D.setSurfaceSmoothingFactor(smooth);
				smoothOld = smooth;
			}
			String str = "Smoothing: " + (int)(smooth*100)/100.; 
			setSliderTitle(sliderSmoothing, Color.black, str );
		}
		else if (slider == sliderScale) { 	
			scaleSlider = sliderScale.getValue() / 100.; 
			String str = "Scale: " + (int)(scaleSlider*100)/100.; 
			setSliderTitle(sliderScale, Color.black, str );
			double scale = scaleInit * scaleWindow * scaleSlider;
			jRenderer3D.setTransformScale(scale);
		}
		else if (slider == sliderPerspective) {
			perspective = sliderPerspective.getValue()/100.; 
			jRenderer3D.setTransformPerspective(perspective);
			String str = "Perspective: " + perspective; 
			setSliderTitle(sliderPerspective, Color.black, str );
		}
		else if (slider == sliderMin) {
			maxSlider = sliderMax.getValue();
			minSlider = sliderMin.getValue();
			
			if (minSlider >= maxSlider) {
				maxSlider = Math.min(101,minSlider + 1);
				sliderMax.setValue(maxSlider);
				sliderMax.repaint();
			}
			String str = "Min: " + minSlider + " %"; 
			setSliderTitle(sliderMin, Color.black, str );
			str = "Max: " + maxSlider + " %";  
			setSliderTitle(sliderMax, Color.black, str );
			
			jRenderer3D.setSurfacePlotMinMax(minSlider, maxSlider);
			
			addCoordinateSystem();
		
		}
		else if (slider == sliderMax) {
			maxSlider = sliderMax.getValue();
			minSlider = sliderMin.getValue();
			
			if (maxSlider <= minSlider) {
				minSlider = Math.max(-1,maxSlider - 1);
				sliderMin.setValue(minSlider);
				sliderMin.repaint();
			}
			String str = "Min: " + minSlider + " %"; 
			setSliderTitle(sliderMin, Color.black, str );
			str = "Max: " + maxSlider  + " %";
			setSliderTitle(sliderMax, Color.black, str );
			
			jRenderer3D.setSurfacePlotMinMax(minSlider, maxSlider);
			
			addCoordinateSystem();
		}
		else if (slider == sliderZAspectRatio) {
			zAspectRatioSlider = sliderZAspectRatio.getValue()/100.;
			String str = "z-Scale:" + zAspectRatioSlider; 
			setSliderTitle(sliderZAspectRatio, Color.black, str );
			zAspectRatio  = zAspectRatioSlider*zRatioInit;
			jRenderer3D.setTransformZAspectRatio(zAspectRatio);
			maxDistance = Math.max(scaledWidth, Math.max(scaledHeight, 256*Math.max(zAspectRatio,1)));
			
			jRenderer3D.setTransformMaxDistance(maxDistance);
			
			addCoordinateSystem();
		}
		
		renderAndUpdateDisplay();	
	}	
	
	
	
	/**
	 * Rezises the buffer size of the image. Renders and updates image.
	 *
	 */	
	public void resizeImagePanel(int width, int height){
		if (jRenderer3D != null) {
			scaleWindow = Math.min(width, height)/(double)startWindowHeight;
			
			jRenderer3D.setBufferSize(width, height);

			setScaleAndZRatio();
			
			renderAndUpdateDisplay();
		}
	}	
	
	


	
	/********************************************************
	 * 														*
	 *						GUI-METHODS						*						
	 *														*
	 ********************************************************/		
	
	private void createGUI() {		
		// add window listener
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				frame.dispose();
			}
		});	
		
		// create window/frame content
		mainPanel = createMainPanel();
		
		// add content to window/frame
		frame.getContentPane().add(mainPanel);
		
		//	set size and visibility of frame
		//frame.setSize(startWindowWidth, startWindowHeight);
		frame.setSize(windowWidth, windowHeight);
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true); 	
		
		// add component/resize listener
		frame.addComponentListener(new ComponentAdapter(){
			
			public void componentResized(ComponentEvent event) {
				Insets insetsFrame = frame.getInsets();				
				windowWidth = frame.getWidth() - insetsFrame.left - insetsFrame.right - settingsPanel2.getWidth();
				windowHeight = frame.getHeight() - insetsFrame.bottom - insetsFrame.top - settingsPanel1.getHeight();
				if (windowHeight>0 && windowWidth > 0)
					resizeImagePanel(windowWidth, windowHeight);
				frame.pack();				
			}
		});		
		
	}
	
	
	private JPanel createMainPanel(){
		// create image region panel 
		JPanel imagePanel = createImagePanel();
		
		// create settings panel top
		settingsPanel1 = createSettingsPanelTop();
		
		// create settings panel right
		settingsPanel2 = createSettingsPanelRight();
		
		// create main panel
		JPanel mainPanel = new JPanel();	
		mainPanel.setLayout(new BorderLayout());
		
		// add panels to main panel
		mainPanel.add(settingsPanel1, BorderLayout.NORTH);		
		mainPanel.add(settingsPanel2, BorderLayout.EAST);		
		mainPanel.add(imagePanel, BorderLayout.CENTER);
		
		return mainPanel;
	}	
	
	
	private JPanel createImagePanel(){
		// create image region
		imageRegion = new ImageRegion();
		
		// init size

		imageRegion.setWidth(windowWidth);
		imageRegion.setHeight(windowHeight);
		
		imageRegion.addMouseListener(this);
		imageRegion.addMouseMotionListener(this);
		
		imageRegion.addKeyListener ( 			
				new KeyAdapter() { 
					private int number;

					public void keyPressed (KeyEvent e){ 
						if (e.isShiftDown()) {
						//	System.out.println("Shift");
						}
						if (e.getKeyChar() == 's') {
							number++;
							String str = image.getShortTitle() + " (" + number +")";
							imageRegion.saveToImageJImage(str);
						}
					} 
				}  
		); 
		imageRegion.requestFocus();
		
		// create image region panel/canvas
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
					
		// add image region to panel
		panel.add(imageRegion, BorderLayout.CENTER);
		
		// return image panel
		return panel;
	}
	
	
	private JPanel createSettingsPanelTop(){
		Dimension comboDim = new Dimension(400, 25);
		Dimension sliderDim1 = new Dimension(400, 50);
		
		// create combo panel
		JPanel comboPanel = createComboPanel();				
		comboPanel.setPreferredSize(comboDim);	
		
		// create slider panels
		JPanel sliderPanel1 = createSliderPanel1();					
		sliderPanel1.setPreferredSize(sliderDim1);
		
		// create settings panel
		settingsPanel1 = new JPanel();
		settingsPanel1.setLayout(new BorderLayout());
				
		// add combo/slider panels
		settingsPanel1.add(comboPanel, BorderLayout.NORTH);
		settingsPanel1.add(sliderPanel1, BorderLayout.SOUTH);				
		
		// return settings panel
		return settingsPanel1;
	}	
	
	private JPanel createSettingsPanelRight(){
		Dimension sliderDim2 = new Dimension(70, 400);
		
		// create slider panel
		JPanel sliderPanel2 = createSliderPanel2();					
		sliderPanel2.setPreferredSize(sliderDim2);
		
		// create settings panel
		settingsPanel2 = new JPanel();
		settingsPanel2.setLayout(new BorderLayout());
				
		// add combo/slider panels
		settingsPanel2.add(sliderPanel2, BorderLayout.CENTER);		
				
		// return settings panel
		return settingsPanel2;
	}	
	
	
	void createComboDisplayColors() {
//		 create display colors combo box		
		comboDisplayColors = new JComboBox();
		
		comboDisplayColors.addItem(ORIGINAL_COLORS);
		comboDisplayColors.addItem(GRAYSCALE_LUT);
		comboDisplayColors.addItem(SPECTRUM_LUT);
		comboDisplayColors.addItem(FIRE_LUT);
		comboDisplayColors.addItem(THERMAL_LUT);
		comboDisplayColors.addItem(GRADIENT_COLORS);
		comboDisplayColors.addItem(BLUE_LUT);
		comboDisplayColors.addItem(ORANGE_LUT);

		comboDisplayColors.setSelectedIndex(colorType);
		
		comboDisplayColors.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				setSurfaceColorType(comboDisplayColors.getSelectedIndex());
				renderAndUpdateDisplay();
			}	
		});		
	}
	
	private JPanel createComboPanel(){				
		
		// create display type combo box
		comboDisplayType = new JComboBox();
		
		comboDisplayType.addItem(DOTS_PLOT);
		comboDisplayType.addItem(LINES_PLOT);
		comboDisplayType.addItem(MESH_PLOT);
		comboDisplayType.addItem(FILLED_PLOT);
		comboDisplayType.addItem(ISOLINES_PLOT);
		
		int selectedIndex = plotType;
		
		comboDisplayType.setSelectedIndex(selectedIndex);
		
		comboDisplayType.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent evt) {
				plotType = comboDisplayType.getSelectedIndex();
				setSurfacePlotType(plotType);
				renderAndUpdateDisplay();
			}	
		});
		
		createComboDisplayColors();
		
		// create save button
		JButton saveButton = new JButton("Save Plot");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imageRegion.saveToImageJImage(image.getShortTitle());
			}
		});
		
		// create texture button
		JButton textureButton = new JButton("Load Texture");
		textureButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadTextureImage();
			}
		});
		
		final JPopupMenu popup = new JPopupMenu();
		
		final JCheckBoxMenuItem menuItem1 = new JCheckBoxMenuItem("Axes");
		menuItem1.setSelected(drawAxes);
		menuItem1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawAxes = menuItem1.isSelected();
				jRenderer3D.setAxes(drawAxes);
				renderAndUpdateDisplay();
			}
		});
		popup.add(menuItem1);

		final JCheckBoxMenuItem menuItem2 = new JCheckBoxMenuItem("Lines");
		menuItem2.setSelected(drawLines);
		menuItem2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawLines = menuItem2.isSelected();
				jRenderer3D.setLines(drawLines);
				renderAndUpdateDisplay();		
			}
		});
		popup.add(menuItem2);
		
		final JCheckBoxMenuItem menuItem3 = new JCheckBoxMenuItem("Text");
		
		menuItem3.setSelected(drawText);
		menuItem3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawText = menuItem3.isSelected();
				jRenderer3D.setText(drawText);
				renderAndUpdateDisplay();	
			}
		});
		popup.add(menuItem3);
		
		final JCheckBoxMenuItem menuItem4 = new JCheckBoxMenuItem("Legend");
		menuItem4.setSelected(drawLegend);
		menuItem4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawLegend = menuItem4.isSelected();
				jRenderer3D.setLegend(drawLegend);
				renderAndUpdateDisplay();	
			}
		});
		popup.add(menuItem4);
		
		final JCheckBoxMenuItem menuItem5 = new JCheckBoxMenuItem("Fast drawing on drag");
		menuItem5.setSelected(true);
		menuItem5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				draftDrawing = menuItem5.isSelected();
				//renderAndUpdateDisplay();	
			}
		});
		popup.add(menuItem5);
		
		final JMenuItem menuItem6 = new JMenuItem("Reset everything");
		menuItem6.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Reset");
				frame.dispose();
				doReset = true;
				light = 0.2;
	        	perspective = 0;
	        	grid = 128;
	        	smooth = 3;
	        	plotType = LINES;
	        	colorType = ORIGINAL;
	        	drawAxes = true;
	        	drawLines = true;
	        	drawText = true;
	        	drawLegend = true;
	        	invertZ = false;
	        	isEqualxyzRatio = false;
	        	rotationX =  65;    
	        	rotationZ =  39; 
	        	startWindowHeight = windowHeight = SIZE;
	        	startWindowWidth = windowWidth = (int) (1.1*SIZE);
	        	scaleSlider = 1;
				zAspectRatioSlider = 1;
				minSlider = 0;
				maxSlider = 100;
				
				run("");
				
				//renderAndUpdateDisplay();	
			}
		});
		popup.add(menuItem6);
		
		
		// create options button
		final JButton optionsButton = new JButton("Display Options");
		optionsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popup.show(optionsButton,30,22);
			}
		});
		
		// create combo panel
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 5, 0, 0));		
		
		
	    // add elements to combo panel
		panel.add(comboDisplayType);		
		panel.add(comboDisplayColors);
		panel.add(textureButton);
		panel.add(saveButton);
		panel.add(optionsButton);
		
		// return combo panel
		return panel;
	}
	
	
	private void loadTextureImage() {
		
		ImagePlus impTexture = null;
		
		int[] wList = WindowManager.getIDList();
		boolean loadFromDisk = false;
		if (wList==null) {
			loadFromDisk = true;
		}
		else {
			String[] titles = new String[wList.length + 1];
			for (int i=0; i<wList.length; i++) {
				ImagePlus imp = WindowManager.getImage(wList[i]);
				if (imp!=null)
					titles[i] = imp.getTitle();
				else
					titles[i] = "";
			}
			titles[wList.length] = "\"Load File from Disk\"";

			GenericDialog gd = new GenericDialog("Load texture", IJ.getInstance());
			
			gd.addMessage("Please select an Image to be used as texture");
			
			String defaultItem = titles[0];
			gd.addChoice("Open Image:", titles, defaultItem);

			gd.showDialog();
			if (gd.wasCanceled())
				return;
			int index = gd.getNextChoiceIndex();
			if(titles[index].equals("\"Load File from Disk\""))
				loadFromDisk = true;
			else {				
				impTexture = WindowManager.getImage(wList[index]);
			}
		}
		
		if (loadFromDisk == true) {
			JFileChooser fc = new JFileChooser();		// open texture image

			if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				String str = fc.getSelectedFile().getPath();
				try {
					IJ.run("Open...", "path='"+str+"'");
					impTexture = WindowManager.getCurrentImage();
				} catch (RuntimeException e) {
					JOptionPane.showMessageDialog(null,"Error opening Image","",JOptionPane.PLAIN_MESSAGE);
					return;
				}
			}
		}
		
		if (impTexture != null) {
			jRenderer3D.setSurfacePlotTexture(impTexture);
			
			setSurfacePlotType(plotType);
			
			jRenderer3D.setSurfacePlotLight(light);
			
			minSlider = sliderMin.getValue();
			maxSlider = sliderMax.getValue();

			jRenderer3D.setSurfacePlotMinMax(minSlider, maxSlider);
			jRenderer3D.surfacePlotSetInverse(invertZ);
			
			grid = 1 << sliderGridSize.getValue(); 
			smooth = sliderSmoothing.getValue() * (grid / 512.);
			if (smooth < 1) smooth = 0;
			jRenderer3D.setSurfaceSmoothingFactor(smooth);
			smoothOld = smooth;

			setSurfaceColorType(ORIGINAL);
			comboDisplayColors.setSelectedIndex(0);
			
			renderAndUpdateDisplay();
		}
	}


	private JPanel createSliderPanel1() {				
		// create sliders 
		String str = "Grid Size: " + grid;
		int gridSliderValue = (int)Math.round(Math.log(grid)/Math.log(2));
		gridSliderValue = Math.min(9, Math.max(5, gridSliderValue));
		sliderGridSize = createSliderHorizontal(str, 5, 9, gridSliderValue); // 32, 64, 128, 256, 512
		
		str = "Smoothing: " + (int)Math.round(smooth*100)/100.;
		int smoothSliderValue = (int) (smooth * (512 / grid));
		smoothSliderValue = Math.min(100, Math.max(0, smoothSliderValue));
		sliderSmoothing = createSliderHorizontal(str, 0, 100, smoothSliderValue);
		
		str = "Perspective: " + (int)Math.round(perspective*100)/100.;
		sliderPerspective = createSliderHorizontal(str, 0, 100, (int)(perspective*100));
		
		int light_ = (int)Math.round(light*100);
		sliderLight = createSliderHorizontal("Lighting: "+light_/100., 0, 100,light_);
		
		JPanel miniPanel = new JPanel();
		miniPanel.setLayout(new GridLayout(2,1,0,3));
		
		checkIsEqualxyzRatio = new JCheckBox("z-Ratio = xy-Ratio");
		checkIsEqualxyzRatio.setFont(new Font("Sans", Font.PLAIN, 11));
		checkIsEqualxyzRatio.setSelected(isEqualxyzRatio);
		checkIsEqualxyzRatio.addItemListener (this);
		
		checkInverse = new JCheckBox("Invert");
		checkInverse.setFont(new Font("Sans", Font.PLAIN, 11));
		checkInverse.setSelected(invertZ);
		checkInverse.addItemListener (this);
		
		miniPanel.add(checkIsEqualxyzRatio);
		miniPanel.add(checkInverse);
		
		// create slider panel
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,5));		
		
		// add elements to combo panel
		panel.add(sliderGridSize);		
		panel.add(sliderSmoothing);
		panel.add(sliderPerspective);
		panel.add(sliderLight);
		panel.add(miniPanel);
		
		return panel;		
	}	
	
	public synchronized void itemStateChanged(ItemEvent e) {
		
		if (e.getSource() == checkInverse) {
			invertZ  = checkInverse.isSelected ();
			
			jRenderer3D.surfacePlotSetInverse(invertZ);
			maxSlider = sliderMax.getValue();
			minSlider = sliderMin.getValue();
			jRenderer3D.setSurfacePlotMinMax(minSlider, maxSlider);

			addCoordinateSystem();			
		}

		if (e.getSource() == checkIsEqualxyzRatio) {
			if (checkIsEqualxyzRatio.isSelected () ) 
				isEqualxyzRatio = true;
			else 
				isEqualxyzRatio = false;
			
			setScaleAndZRatio();
		}
	
		renderAndUpdateDisplay();
	}

	
	private JPanel createSliderPanel2() {				
		
		// create sliders 
		String str;
		str = "Scale: " + ((int)(scaleSlider*100))/100.;
		sliderScale = createSliderVertical(str, 25, 300, (int)(scaleSlider*100));
		str = "z-Scale: " + ((int)(zAspectRatioSlider*100))/100.;
		sliderZAspectRatio = createSliderVertical(str, 10, 500, 100);
		str = "Min: " + minSlider + " %";
		sliderMin = createSliderVertical(str, 0, 100, minSlider);
		str = "Max: " + maxSlider + " %";
		sliderMax = createSliderVertical(str, 0, 100, maxSlider);
		
		// create slider panel
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(4, 1));		
		
		panel.add(sliderScale);
		panel.add(sliderZAspectRatio);
		panel.add(sliderMax);
		panel.add(sliderMin);
		return panel;		
	}	
	
	
private JSlider createSliderHorizontal(String borderTitle, int min, int max, int value) {
		
		// create nested border
		Border empty = BorderFactory.createTitledBorder( 
					   BorderFactory.createEmptyBorder());
				
		// create font for sliders
		Font sliderFont = new Font("Sans", Font.PLAIN, 11);
		
		// create slider
		JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, value);
		slider.setBorder(new TitledBorder(
				empty, borderTitle, TitledBorder.CENTER, 
				TitledBorder.BELOW_TOP,	sliderFont)); 		

		slider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent event) {
				sliderChange((JSlider)event.getSource());
			}	
		});		
		
		return slider;
	}

	private JSlider createSliderVertical(String borderTitle, int min, int max, int value) {
	
	// create nested border
	Border empty = BorderFactory.createTitledBorder( 
				   BorderFactory.createEmptyBorder());
			
	// create font for sliders
	Font sliderFont = new Font("Sans", Font.PLAIN, 11);
	
	// create slider
	JSlider slider = new JSlider(JSlider.VERTICAL, min, max, value);
	slider.setBorder(new TitledBorder(
			empty, borderTitle, TitledBorder.CENTER, 
			TitledBorder.BELOW_TOP,	sliderFont)); 		

	slider.addChangeListener(new ChangeListener(){
		public void stateChanged(ChangeEvent event) {
			sliderChange((JSlider)event.getSource());
		}	
	});		
	
	return slider;
}

	
	private void setSliderTitle(JSlider slider, Color color, String str) {
		Border empty = BorderFactory.createTitledBorder( 
					   BorderFactory.createEmptyBorder() );
		
		Font sliderFont = new Font("Sans", Font.PLAIN, 11);
		
		slider.setBorder(new TitledBorder(
				empty, str, TitledBorder.CENTER, 
				TitledBorder.BELOW_TOP,	sliderFont)); 		

		//TitledBorder tb = new TitledBorder(empty,
		//		"", TitledBorder.CENTER, TitledBorder.TOP,
		//		new Font("Sans", Font.PLAIN, 12));
		//tb.setTitleJustification(TitledBorder.LEFT);
		//tb.setTitle(str);
		//tb.setTitleColor(color);
		//slider.setBorder(tb);
	}
	
	
	/**
	 * Image Region
	 */
	class ImageRegion  extends JPanel {
		
		private Image image;
		private int width;
		private int height;
		
		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}
		public Dimension getMinimumSize() {
			return new Dimension(width, height);
		}
		
		public void setImage(JRenderer3D pic){
			height = pic.getHeight();
			width = pic.getWidth();
			image = pic.getImage();
		}
		
		public void setImage(Image image){
			this.image = image;
		}
		
		public void paint(Graphics g) {
			
			if (image != null ) {
				g.drawImage(image, 0, 0, width, height, (ImageObserver) this);
			}	
		}
		
		synchronized void saveToImageJImage(String name) {
			
			BufferedImage bufferedImage =  new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			
			paint(bufferedImage.createGraphics());
			
			String str = "Surface_Plot_of_" + name;
			
			ImagePlus plotImage = NewImage.createRGBImage (str, width, height, 1, NewImage.FILL_BLACK);
			
			ImageProcessor ip = plotImage.getProcessor();
			
			int[] pixels = (int[]) ip.getPixels();
			bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
			
			plotImage.show();
			plotImage.updateAndDraw();	
		}
		
		//-------------------------------------------------------------------
		
		public void update(Graphics g) {
			paint(g);
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
	}

}
