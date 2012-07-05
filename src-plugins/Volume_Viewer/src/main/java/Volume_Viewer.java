/*
* Volume Viewer
*
* (C) Author:  Kai Uwe Barthel: barthel (at) fhtw-berlin.de 
* 
* 	This plugin shows stacks as volumes within a 3D-(xyz)-space. 
*   
* 	The viewing angle can be adjusted with the mouse. 
* 	This plugin can be used with 8/16, 32 bit and RGB stacks. 
* 
* 	Version 1.31
* 				Support for RGB stacks 
*				Fixed a drawing error (planar views)
* 
* 	Version 1.21
* 				Two volume rendering options: 
* 				back: classical volume rendering  
* 				front: overlay of the front pixels
* 
* 				The plugin can be called from a macro. 
* 				If called from a macro, only the resulting image is displayed.
* 				Possible parameters :
* 					Name 			Values		Default
* 		    		display_mode=   0 .. 6		1 (SLICE_TRILINEAR)	
*					lut=            0 .. 4		0 (ORIG)
*					z-aspect=       float		1
*					dist=           float		0
*					depth=          0 .. 25		6
*					thresh=         0 .. 255	128
*					axes=           0 / 1		1 (true)
*					markers=        0 / 1		1 (true)
*					scale=          0.01 .. 8   1    
*					angle_x=        0 .. 360    115
*					angle_z=        0 .. 360    -35
*						
* 	Version 1.15 			March 11 2005
* 				Value indicators
* 			
*   Version 1.12			March, 8  2005
*  				speedups,
* 				planar view indicators
* 				new display mode (projection)		
* 
* 	Version 1.06
* 				pixel depth corrected for uncalibrated stacks
* 	
* 	Version 1.05			February, 16 2005
* 				First version
*/
 
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.Macro;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.awt.image.IndexColorModel;
import java.awt.image.MemoryImageSource;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;




public final class Volume_Viewer implements PlugIn {
	
	private final static String version = "1.31"; 
	
	private static final int SLICE_NN = 0;
	private static final int SLICE_TRILINEAR = 1;
	private final static int SLICE_AND_BORDERS = 2;
	private final static int SLICE_AND_VOLUME_DOTS = 3;
	private static final int VOLUME_DOTS = 4;
	private static final int PROJECTION_TRILINEAR_FRONT = 5;
	private static final int PROJECTION_TRILINEAR_BACK = 6;
	
	private final String [] displayName = { 
			"Slice Nearest Neighbor", 
			"Slice Trilinear ", 
			"Slice & Borders", 
			"Slice & Volume Dots", 
			"Volume Dots", 
			"Volume I (slow)", 
			"Volume II (slow)", 
	} ;
	
	
	private final static int ORIG = 0;
	private final static int GRAY = 1;
	private final static int SPECTRUM = 2;
	private final static int FIRE = 3;
	private final static int THERMAL = 4;
	
	private boolean drag = false; 
	private boolean shift = false;
	private boolean pause = false;
	private boolean move = false;
	private boolean running = true;
	private boolean calledFormMacro = false;
	
	private byte[][][][] data3D = null;
	private byte[][][][] data3DS = null;
	
	private Cube cube = null;
	
	private Volume vol = null;
	private Lut lut = null;
	private Picture pic = null;
	private Picture pic2 = null;
	private ImageRegion imageRegion;
	private ImageRegion imageRegion2;
	
	private Transform tr = null;
	
	private JTextField tf2, tf3;
	
	private ImagePlus imp;
	private ImageProcessor ip; 
	
	private boolean isRGB = false;
	private boolean isRed = true;
	private boolean isGreen = true;
	private boolean isBlue = true;
	
	
	private int displayMode;
	private int lutNr;
	private float zAspect;
	private int dist;
	private int viewDist;
	private int renderDepth;
	private int thresh; 
	private boolean axes;
	private boolean markers;
	private float scale;
	private float angleX;
	private float angleZ;
	
	private String[] params   = {
			"display_mode=",
			"lut=",
			"z-aspect=",
			"dist=",
			"depth=",
			"thresh=",
			"axes=",
			"markers=",
			"scale=",
			"angle_x=",
			"angle_z="
	};
	
	private float[] paramVals = {
			SLICE_TRILINEAR,   // "display_mode=",
			ORIG,   // "lut=",
			1,   // "z-aspect=",
			0,   // "dist=",
			6,   // "depth=",
			128, // "thresh=",
			1,   // "axes=",
			1,   // "markers=",
			1,   // "scale=",
			115, // "angle_x=",
			-35  // "angle_z="
	};

	public int rendered;
	
	
	public static void main(String args[]) {
		
		//new ImageJ(); // open the ImageJ window to see images and results
		
		Volume_Viewer vv = new Volume_Viewer();
		
		new ImageJ();
		//IJ.open("/Users/barthel/Applications/ImageJ/_images/_stacks/mri-stack.tif");
		IJ.open("/Users/barthel/Applications/ImageJ/_images/_stacks/RGB_Stack.tif");
		
		vv.run("");
	}
	
	public void run(String args) {
		
		String st = Macro.getOptions();
		
		//st = "display_mode=5 axes=0 markers=0 z-aspect=4 lut=0 scale=0.75 dist=-300 depth=6 thresh=80 angle_x=115 angle_z=41";
		
		// read macro parameters
		try {
			if (st != null) {
				StringTokenizer ex1; // Declare StringTokenizer Objects
				ex1 = new StringTokenizer(st); //Split on Space (default)
				
				while (ex1.hasMoreTokens()) {
					String str = ex1.nextToken();
					for (int j = 0; j<params.length; j++) {
						String pattern = params[j];
						if (str.lastIndexOf(pattern) > -1) { 
							int pos = str.lastIndexOf(pattern) + pattern.length();
							paramVals[j] = Float.parseFloat(str.substring(pos));
							//IJ.log("" + params[j] + ": " + paramVals[j]); 
						}
					}
				}
				calledFormMacro = true;
			}
		} catch (NumberFormatException e1) {
			IJ.error("Error in macro parameter list");
		}
		
		displayMode = (int) paramVals[0];
		lutNr =       (int) paramVals[1];
		zAspect =           paramVals[2];
		viewDist = 
			dist =    (int) paramVals[3];
		renderDepth = (int) paramVals[4];
		thresh =      (int) paramVals[5]; 
		axes =       ((int) paramVals[6] == 0)? false : true;
		markers =    ((int) paramVals[7] == 0)? false : true;
		scale =             paramVals[8];
		angleX =      (int) paramVals[9];
		angleZ =      (int) paramVals[10];
		
		
		imp = WindowManager.getCurrentImage();
		
		tf2 = new JTextField();
		tf3 = new JTextField();
		
		if (imp == null  || !(imp.getStackSize() > 1)) {
			IJ.showMessage("Stack required");
			return;
		}
		//	  Check for RGB stack.
		if(imp.getType()==ImagePlus.COLOR_RGB) {
	    	isRGB = true;
		}
		
		final CustomWindow  cw = new CustomWindow();
		
		cw.init(imp);
		pic.newDisplayMode();
		
		if (calledFormMacro) {
			imageRegion.saveToImage();
			cw.cleanup();
		}
		else {
			final Frame f = new Frame("Volume Viewer " +  version + " ");
			
			f.setLocation(300,150);
			f.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cw.cleanup();
					f.dispose();
				}
			});
			
			f.add(cw);
			
			f.pack(); 
			f.setResizable(false);
			Insets ins = f.getInsets();
			
			cw.totalSize.height = CustomWindow.H + ins.bottom + ins.top + 90;
			cw.totalSize.width  = CustomWindow.WR + CustomWindow.WL + ins.left + ins.right + 70;
			
			f.setSize(cw.totalSize);
			
			f.setVisible(true);
			cw.requestFocus();
			
			cw.addKeyListener ( 
					new KeyAdapter() 
					{ 
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
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	class CustomWindow extends JPanel implements 
	MouseListener, MouseMotionListener, ChangeListener, ActionListener, ItemListener {
		
		protected  Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		protected  Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);
		
		private JPanel imagePanel;
		
		private JCheckBox checkMarkers, checkAxes;
		
		private JComboBox displayChoice, lutChoice;
		private final String [] lutName = { "Original", "Grayscale", "Spectrum LUT", "Fire LUT", "Thermal LUT"} ;
		private final String [] rgbName = { "RGB", "Red", "Green", "Blue"} ;
		
		private JSlider sliderScale, sliderThresh, sliderDepth, sliderDist; 
		private float sliderScaleValue; 
		
		private JLabel zAspectLabel;
		private String zAspectString = "z-aspect";
		private JTextField tf1;
		
		private int checkMove = 0;
		private int xStart, yStart, xAct, yAct;
		
		private int xdiff, ydiff;
		private float dx, dy;
		
		
		private Dimension totalSize = new Dimension(); 
		//private Dimension rightSize = new Dimension(); 
		
		private double distFactor; 
		
		//  do not change sizes !
		private final static int H = 512;
		private final static int WR = 512; 
		private final static int WL = 200; 
		
		private TurnThread thread;
		
		private void cleanup() {
			vol = null;
			
			pic.pixels = null;
			pic.pixelsZ = null;
			pic = null;
			pic2.pixels = null;
			pic2.pixelsZ = null;
			pic2 = null;
			
			imageRegion = null;
			imageRegion2 = null;
			
			imagePanel = null;
			running = false;
			thread = null;
			data3D = null;
			data3DS = null;
		}
		
		
		void init(ImagePlus imp) {
			setLayout(new BorderLayout());
			
			imagePanel = new JPanel();
			imagePanel.setLayout(new BorderLayout());
			
			pic = new Picture(WR, H);
			pic2 = new Picture(WL, H);
			
			cube = new Cube();
			
			vol = new Volume();
			pic.setVol(vol);
			
			tr = new Transform(WR, H);	
			tr.setScale(scale);
			
			imageRegion = new ImageRegion();
			
			imageRegion.addMouseMotionListener(this);
			imageRegion.addMouseListener(this);
			
			imageRegion.setImage(pic);
			pic.setImageRegion(imageRegion);
			
			imageRegion2 = new ImageRegion();
			
			imageRegion2.setImage(pic2);
			imageRegion2.newLines(3); 
			pic2.setImageRegion(imageRegion2);
			
			pic2.drawPlanarViews();
			cube.initTextsAndDrawColors(imageRegion);
			
			lut.setLut();
			
			imagePanel.add(imageRegion,BorderLayout.CENTER);
			imagePanel.add(imageRegion2,BorderLayout.WEST);
			
			add(imagePanel, BorderLayout.CENTER);
			
			JPanel buttonPanel1 = new JPanel();
			buttonPanel1.setLayout(new GridLayout(1,4,30,0));
			
			displayChoice = new JComboBox(displayName);
			displayChoice.setSelectedIndex(displayMode);
			displayChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			displayChoice.addActionListener(this);			
			buttonPanel1.add(displayChoice);
			
			if (!isRGB) {
				lutChoice = new JComboBox(lutName);
				lutChoice.setSelectedIndex(lutNr);
				lutChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
				lutChoice.addActionListener(this);
				buttonPanel1.add(lutChoice);
			}
			else {
				lutChoice = new JComboBox(rgbName);
				lutChoice.setSelectedIndex(0);
				lutChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
				lutChoice.addActionListener(this);
				buttonPanel1.add(lutChoice);
			}
				
			
			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new GridLayout(1,3));
			
			zAspectLabel = new JLabel(zAspectString);
			miniPanel.add(zAspectLabel);
			
			tf1 = new JTextField();
			
			tf1.setText("" + zAspect);
			tf1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					zAspect = Float.parseFloat(tf1.getText());
					setZAspect();
					tr.initializeTransformation();
					pic.newDisplayMode();
					
					pic2.drawPlanarViews();
					pic.findLines();
					pic2.updateImage();
				}
			});
			
			miniPanel.add(tf1);
			
			buttonPanel1.add(miniPanel);
			
			JButton buttonSaveView = new JButton("Save View");
			buttonSaveView.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					imageRegion.saveToImage();
				}
			});
			buttonPanel1.add(buttonSaveView); 
			
			
			JPanel buttonPanel2 = new JPanel();
			buttonPanel2.setLayout(new GridLayout(1,2));
			
			JPanel miniPanel1 = new JPanel();
			miniPanel1.setLayout(new GridLayout(1,2));
			
			Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );
			
			tr.setScale(scale);
			String st = "Scale: " + ((int) (scale*10 + 0.5f))/10f + "  ";
			
			int scaleVal = (int) (Math.log(scale)/Math.log(1.0717734) + 20);
			
			sliderScale = new JSlider(JSlider.HORIZONTAL, 0, 50, scaleVal );
			sliderScale.setBorder( new TitledBorder(empty,
					st, TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			sliderScale.addChangeListener( this );
			sliderScale.addMouseListener(this);
			miniPanel1.add(sliderScale); 
			
			JPanel miniPanel1right = new JPanel();
			miniPanel1right.setLayout(new GridLayout(1,2));
			
			checkAxes = new JCheckBox("Axes");
			checkAxes.setSelected(axes);
			checkAxes.setHorizontalAlignment(JCheckBox.CENTER);
			checkAxes.addItemListener (this);
			miniPanel1right.add(checkAxes);
			
			checkMarkers = new JCheckBox("Markers");
			checkMarkers.setSelected(markers);
			checkMarkers.setHorizontalAlignment(JCheckBox.CENTER);
			checkMarkers.addItemListener (this);
			miniPanel1right.add(checkMarkers);
			
			miniPanel1.add(miniPanel1right);
			
			
			
			JPanel miniPanel2 = new JPanel();
			miniPanel2.setLayout(new GridLayout(2,1));
			
			JPanel miniPanel2up = new JPanel();
			
			JLabel labelX = new JLabel("Angle of rotation   x:");
			miniPanel2up.add(labelX);
			tf2.setColumns(4);
			tr.setRotationX(Math.toRadians(angleX));
			tf2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int iVal = Integer.parseInt(tf2.getText());
					tr.setRotationX(Math.toRadians(iVal));
					move = false;
					checkMove = 0;
					pic.newDisplayMode();
				}
			});
			miniPanel2up.add(tf2);
			
			JLabel labelZ = new JLabel("z:");
			miniPanel2up.add(labelZ);
			tf3.setColumns(4);
			tr.setRotationZ(Math.toRadians(angleZ));
			tf3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int iVal = Integer.parseInt(tf3.getText());
					tr.setRotationZ(Math.toRadians(iVal));
					move = false;
					checkMove = 0;
					pic.newDisplayMode();
				}
			});
			miniPanel2up.add(tf3);
			
			JPanel miniPanel2down = new JPanel();
			miniPanel2down.setLayout(new GridLayout(1,3));
			
			JButton buttonXY = new JButton("xy");
			buttonXY.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tr.setView(0, 0);
					move = false;
					checkMove = 0;
					pic.newDisplayMode();
				}
			});
			miniPanel2down.add(buttonXY); 
			
			JButton buttonYZ = new JButton("yz");
			buttonYZ.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tr.setView(-Math.PI/2, Math.PI/2);
					move = false;
					checkMove = 0;
					pic.newDisplayMode();
				}
			});
			miniPanel2down.add(buttonYZ); 
			
			JButton buttonXZ = new JButton("xz");
			buttonXZ.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					tr.setView(0, Math.PI/2);
					move = false;
					checkMove = 0;
					pic.newDisplayMode();
				}
			});
			miniPanel2down.add(buttonXZ); 
			
			buttonPanel2.add(miniPanel1);
			
			miniPanel2.add(miniPanel2up);
			miniPanel2.add(miniPanel2down);
			
			buttonPanel2.add(miniPanel2);
			
			add(buttonPanel1, BorderLayout.NORTH);
			add(buttonPanel2, BorderLayout.SOUTH);
			validate();
			
			JPanel sliderPanel2 = new JPanel();
			sliderPanel2.setPreferredSize(new Dimension(65, 512));
			sliderPanel2.setLayout(new GridLayout(3,1));
			int maxDist = (int)(Math.sqrt(vol.zOffa*vol.zOffa*zAspect*zAspect + vol.yOffa*vol.yOffa +vol.xOffa*vol.xOffa));
			viewDist = Math.min(Math.max(viewDist, -maxDist), maxDist);
			sliderDist = new JSlider(JSlider.VERTICAL, -maxDist, maxDist, viewDist );
			sliderDist.setBorder( new TitledBorder(empty,
					("Dist: " + viewDist), TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			sliderDist.setMinorTickSpacing(maxDist/4);
			sliderDist.setMajorTickSpacing(maxDist);
			sliderDist.setPaintTicks(true);
			sliderDist.addChangeListener( this );
			sliderDist.addMouseListener(this);
			sliderPanel2.add(sliderDist); 
		
			
			sliderDepth = new JSlider(JSlider.VERTICAL, 1, 30, renderDepth );
			sliderDepth.setBorder( new TitledBorder(empty,
					("Depth: " + renderDepth), TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			//sliderThreshMax.setMinorTickSpacing(16);
			sliderDepth.setMajorTickSpacing(5);
			sliderDepth.setPaintTicks(true);
			sliderDepth.addChangeListener( this );
			sliderDepth.addMouseListener(this);
			sliderDepth.setEnabled(false);
			sliderPanel2.add(sliderDepth); 
			
			sliderThresh = new JSlider(JSlider.VERTICAL, 0, 255, thresh );
			sliderThresh.setBorder( new TitledBorder(empty,
					("Thr. " + thresh), TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			sliderThresh.setMinorTickSpacing(16);
			sliderThresh.setMajorTickSpacing(64);
			sliderThresh.setPaintTicks(true);
			sliderThresh.addChangeListener( this );
			sliderThresh.addMouseListener(this);
			sliderThresh.setEnabled(false);
			sliderPanel2.add(sliderThresh); 
			
			
			add(sliderPanel2, BorderLayout.EAST);
			dist = viewDist;
			updateDistFactor();
			 
			thread = new TurnThread(pic, imageRegion);
			thread.start();
			super.setCursor(defaultCursor);	
			//pic.newDisplayMode();
		}
		
		
		// adapt the dist slider to the size of the volume
		void updateDistFactor() {
			distFactor = Math.sqrt((vol.xOffa*vol.xOffa + vol.yOffa*vol.yOffa + vol.zOffa*vol.zOffa*zAspect*zAspect) / 
					(vol.xOffa*vol.xOffa + vol.yOffa*vol.yOffa + vol.zOffa*vol.zOffa));
		}
		
		public void stateChanged( ChangeEvent e ){
			JSlider slider = (JSlider)e.getSource();
			
			pause = true; 
			Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );
			
			if (slider == sliderScale) {
				sliderScaleValue = sliderScale.getValue();
				sliderScaleValue -= 20;
				float scale = (float) Math.pow(1.0717734,sliderScaleValue);
				String st = " Scale: " + ((int) (scale*100 + 0.5f))/100f;
				
				sliderScale.setBorder( new TitledBorder(empty,
						st, TitledBorder.CENTER, TitledBorder.TOP,
						new Font("Sans", Font.PLAIN, 12)));	
				tr.setScale(scale); 
				setZAspect();
				tr.initializeTransformation();
			}
			
			if (slider == sliderThresh) {
				thresh = sliderThresh.getValue();
				String st = "Thr.: " + thresh;
				sliderThresh.setBorder( new TitledBorder(empty,
						st, TitledBorder.CENTER, TitledBorder.TOP,
						new Font("Sans", Font.PLAIN, 12)));	
			}
			
			if (slider == sliderDepth) {
				renderDepth = sliderDepth.getValue();
				String st = "Depth: " + renderDepth;
				sliderDepth.setBorder( new TitledBorder(empty,
						st, TitledBorder.CENTER, TitledBorder.TOP,
						new Font("Sans", Font.PLAIN, 12)));		
			}
			
			if (slider == sliderDist) {
				dist = sliderDist.getValue();
				dist = (int) (dist * distFactor);
				String st = "Dist: " + dist;
				sliderDist.setBorder( new TitledBorder(empty,
						st, TitledBorder.CENTER, TitledBorder.TOP,
						new Font("Sans", Font.PLAIN, 12)));	
			}
			
			pause = false; 

			pic.newDisplayMode();
			
			pic2.drawPlanarViews();
			pic2.updateImage();
			
			//super.requestFocus();
		}
		
		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox)e.getSource();
			if (cb == lutChoice) {
				if (isRGB) {
					String name = (String)cb.getSelectedItem();

					if (name.equals(rgbName[0])) {
						isRed = true;
						isGreen = true;
						isBlue = true;
					}
					if (name.equals(rgbName[1])) {
						isRed = true;
						isGreen = false;
						isBlue = false;
					}
					if (name.equals(rgbName[2])) {
						isRed = false;
						isGreen = true;
						isBlue = false;
					}
					if (name.equals(rgbName[3])) {
						isRed = false;
						isGreen = false;
						isBlue = true;
					}
				}
				else {
					String name = (String)cb.getSelectedItem();

					for (int i = 0; i < lutName.length; i++) {
						if (name.equals(lutName[i])) {
							lutNr = i;
							break;
						}	
					}
					lut.setLut();
				}
			}
			
			else if (cb == displayChoice) {
				
				String name = (String)cb.getSelectedItem();
				for (int i = 0; i < displayName.length; i++)
					if (name.equals(displayName[i])) { // all colors 
						displayMode = i;
						break;	
					}
					  
				if (displayMode == SLICE_NN || displayMode == SLICE_TRILINEAR  || displayMode == SLICE_AND_BORDERS) {
					sliderThresh.setEnabled(false);
				}
				else {
					sliderThresh.setEnabled(true);
				}
				if (displayMode == PROJECTION_TRILINEAR_BACK|| displayMode == PROJECTION_TRILINEAR_FRONT)
					sliderDepth.setEnabled(true);
				else
					sliderDepth.setEnabled(false);
			}
			
			pause = false;
			
			pic2.drawPlanarViews();
			pic2.updateImage();
			
			cube.initTextsAndDrawColors(imageRegion);
			pic.newDisplayMode();

			super.requestFocus();
			
			if (!thread.isAlive()) {
				thread = new TurnThread(pic, imageRegion);
				thread.start();
			}
		}
		
		public synchronized void itemStateChanged(ItemEvent e) {
			Object source = e.getItemSelectable();
			
			if (e.getSource() == checkAxes) {
				if (checkAxes.isSelected () ) {
					axes = true;
				}
				if (!checkAxes.isSelected () ) {
					axes = false;
				}
			}
			if (e.getSource() == checkMarkers) {
				if (checkMarkers.isSelected () ) {
					markers = true;
				}
				if (!checkMarkers.isSelected () ) {
					markers = false;
				}
			}
			pic.newDisplayMode();
			
			super.requestFocus();
			
			if (!thread.isAlive()) {
				thread = new TurnThread(pic, imageRegion);
				thread.start();
			}
		}
		
		public void mouseClicked(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion) {
				move = false;		
			}	
			move = false;
		}
		
		public void mouseEntered(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion) {
				super.setCursor(moveCursor);		
			}
		}
		public void mouseExited(MouseEvent arg0) {
			super.setCursor(defaultCursor);
		}
		
		public void mousePressed(MouseEvent arg0) {
			Object source = arg0.getSource();
			
			if (source == imageRegion) {
				checkMove = 0;
				xStart = arg0.getX();
				yStart = arg0.getY();
				
				drag = true;
				
				dx = dy = 0;
				xdiff = 0;
				ydiff = 0;
				
				imageRegion.repaint();	
			}	
			else if (source == sliderScale || source == sliderDist || source == sliderThresh || source == sliderDepth){
				drag = true;
			}	
		}
		
		public void mouseReleased(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion && drag == true) 
				checkMove = 3;
			drag = false;	
			
			pic.newDisplayMode();
		}
		
		public void mouseDragged(MouseEvent arg0) {
			Object source = arg0.getSource();
			
			if (source == imageRegion) {
				if (drag == true) {
					checkMove = 0;
					move = false;
					xAct = arg0.getX();
					yAct = arg0.getY();
					xdiff = xAct - xStart;
					ydiff = yAct - yStart;
					
					dx = (5*dx + xdiff)/6.f;
					dy = (5*dy + ydiff)/6.f;
					
					if (shift == false) 
						tr.setMouseMovement((float)xdiff, (float)ydiff);
					else
						tr.setMouseMovementOffset(xdiff, ydiff);
					xStart = xAct;
					yStart = yAct;
				}
				
				pic.newDisplayMode();
			}	
			
			super.requestFocus();  
			
			if (!thread.isAlive()) {
				thread = new TurnThread(pic, imageRegion);
				thread.start();
			}
		}
		private int mc = 0;
		
		public void mouseMoved(MouseEvent arg0) {
			Object source = arg0.getSource();
			
			if (source == imageRegion) {
				if (!shift && checkMove > 0 && dx != 0 && dy != 0) {
					mc ++;
					if (mc > 5)
						move = true;
				}
				else {
					dx = dy = 0;
					mc = 0;
				}
			}
		}
		
		void setZAspect() {
			updateDistFactor();
			if (zAspect == 0)
				zAspect = 0.01f;
			
			cube.textPos[0][0] = (int) ( - Cube.dm/(2*tr.scale)); 
			cube.textPos[1][0] = (int) ( vol.widthX-1 + Cube.dp/tr.scale); 
			cube.textPos[2][0] = (int) ( - Cube.dm/tr.scale); 
			cube.textPos[3][0] = (int) ( - Cube.dm/tr.scale); 
			
			cube.textPos[0][1] = (int) ( - Cube.dm/(2*tr.scale)); 
			cube.textPos[1][1] = (int) ( - Cube.dm/tr.scale); 
			cube.textPos[2][1] = (int) ( vol.heightY-1 + Cube.dp/tr.scale); 
			cube.textPos[3][1] = (int) ( - Cube.dm/tr.scale); 
			
			cube.textPos[0][2] = (int) (  - Cube.dm/(2*tr.scale*zAspect)); 
			cube.textPos[1][2] = (int) (  - Cube.dm/(tr.scale*zAspect)); 
			cube.textPos[2][2] = (int) (  - Cube.dm/(tr.scale*zAspect)); 
			cube.textPos[3][2] = (int) ( vol.depthZ-1 + Cube.dp/(tr.scale*zAspect)); 
		}
		
		//////////////////////////////////////////////////////////////
		
		class TurnThread extends Thread {
			
			private Picture picture;
			private ImageRegion ir;
			
			public TurnThread (Picture picture, ImageRegion imageRegion) { 
				this.picture = picture; 
				ir = imageRegion;
			}
			
			public void run() {
				
				this.setPriority(Thread.MIN_PRIORITY);
				long tna = 0, tn = 0; 
				long tm = System.currentTimeMillis();
				float fps = 0;
				int delay;
				
				try {
					while (running) {
						
						if (move && !pause) {
							delay = 25;
							
							tr.setMouseMovement(dx, dy);
							picture.newDisplayMode();
							
							long dt = (tna == 0 || tn == 0) ? 0 : (tn - tna); 
							if (dt > 0) {
								if (fps == 0)
									fps = 10000.f/dt;
								else
									fps = (2*fps + 10000.f/dt)/3.f;
								
								ir.setText((Misc.fm(4,((int)fps/10.))+ " fps"),5);
							}
							tna = tn;
							tn = System.currentTimeMillis();
						}
						else { 
							delay = 200;
							ir.setText("", 5);
							fps = 0;
							checkMove--; 
						}
						
						tm += delay;
						long sleepTime = Math.max(0, tm - System.currentTimeMillis()); 
						sleep(sleepTime);
					}
				} catch (InterruptedException e){}
			} 
		}		
	}
	
	////////////////////////////////////////////////////////////////////////
	class Cube {
		
		public void initTextsAndDrawColors(ImageRegion imageRegion) {
			imageRegion.newText(8);  
			imageRegion.newLines(12); 
			
			//imageRegion.setPlaneColor(  new Color(0xFF003465) );
			imageRegion.setPlaneColor(  new Color(0xFF002649) );
			
			backColor  = new Color (0, 0, 0, 255);
			frontColor = Color.white;
			
			Color color;
			
			for (int i= 0; i < letters.length; i++) {
				color = letterCol[i]; 
				imageRegion.setText(letters[i], i, color);
			}
			imageRegion.setText("",  5, 10, 20, 1, Color.white );
		}
		
		private void transformCorners() {
			for (int i=0; i<8; i++) {
				tr.xyzPos(corners[i]);
				cornerT[i][0] = tr.X;
				cornerT[i][1] = tr.Y;
				cornerT[i][2] = tr.Z;
				cornerT[i][3] = 0;
			}
		}
		
		public void setTextAndLines(ImageRegion imageRegion, Transform tr) {
			
			for (int i=0; i<textPos.length; i++) {
				tr.xyzPos(textPos[i]);
				imageRegion.setTextPos(i, tr.X, tr.Y, tr.Z);
			}
			
			int line= 0;
			
			int[][] cor = new int[3][];
			
			for (int i = 0; i < 4; i++) {
				int k = 0;
				for (int j = 4; j < 8; j++) {
					if (i+j != 7) 
						cor[k++] = cornerT[j];
				}
				if (cornerT[i][2] >= cornerT[7-i][2] &&
						Misc.inside(cornerT[i], cor[0], cor[1], cor[2]))
					cornerT[i][3] = 1;  // hidden
			}
			for (int j = 4; j < 8; j++) {
				int k = 0;
				for (int i = 0; i < 4; i++) {
					if (i+j != 7) 
						cor[k++] = cornerT[i];
				}
				if (cornerT[j][2] >= cornerT[7-j][2]  &&  
						Misc.inside(cornerT[j], cor[0], cor[1], cor[2]))
					cornerT[j][3] = 1; // hidden
			}
			
			
			for (int i = 0; i < 4; i++)
				for (int j = 4; j < 8; j++) {
					if (i+j != 7) {
						if (cornerT[i][3] == 1 || cornerT[j][3] == 1)  // hidden
							imageRegion.setLine(line, cornerT[i][0], cornerT[i][1], cornerT[j][0], cornerT[j][1], 1, backColor);
						else 
							imageRegion.setLine(line, cornerT[i][0], cornerT[i][1], cornerT[j][0], cornerT[j][1], -1, frontColor);
						line++;
					}
				}
			
			cor = null;
			
		}
		
		private boolean isInside(int x, int y) {
			
			int n = 0;
			
			int[] p = new int[3];
			p[0] = x;
			p[1] = y;
			
			if (Misc.inside(p, cornerT[0], cornerT[1], cornerT[4])) return true;
			if (Misc.inside(p, cornerT[0], cornerT[1], cornerT[5])) return true;
			if (Misc.inside(p, cornerT[2], cornerT[3], cornerT[6])) return true;
			if (Misc.inside(p, cornerT[2], cornerT[3], cornerT[7])) return true;
			
			if (Misc.inside(p, cornerT[1], cornerT[3], cornerT[5])) return true;
			if (Misc.inside(p, cornerT[1], cornerT[3], cornerT[7])) return true;
			if (Misc.inside(p, cornerT[0], cornerT[2], cornerT[4])) return true;
			if (Misc.inside(p, cornerT[0], cornerT[2], cornerT[6])) return true;
			
			if (Misc.inside(p, cornerT[1], cornerT[2], cornerT[4])) return true;
			if (Misc.inside(p, cornerT[1], cornerT[2], cornerT[7])) return true;
			if (Misc.inside(p, cornerT[0], cornerT[3], cornerT[5])) return true;
			if (Misc.inside(p, cornerT[0], cornerT[3], cornerT[6])) return true;
			
			return false;
		}
		
		private int col;
		private synchronized void findIntersections(Transform tr, int d) {
			nI = 0;
			col = 0;
			
			for (int i = 0; i < 4; i++) {
				for (int j = 4; j < 8; j++) {
					if (i+j != 7) 
						findIntersection(corners[i], corners[j], d);
				}
			}
			if (nI == 0)
				nI = 1;
			for (int i = nI; i < iS.length; i++) {
				iS[i][0] = iS[nI-1][0];
				iS[i][1] = iS[nI-1][1];
				iS[i][2] = iS[nI-1][2];
			}
		}
		
		
		private synchronized void findIntersections_xy(Transform tr, int d) {
			nI = 0;
			findIntersection(corners[8],  corners[9], d);
			findIntersection(corners[9],  corners[10], d);
			findIntersection(corners[10], corners[11], d);
			findIntersection(corners[11], corners[8], d);
		}
		
		private synchronized void findIntersections_yz(Transform tr, int d) {
			nI = 0;
			findIntersection(corners[12],  corners[13], d);
			findIntersection(corners[13],  corners[14], d);
			findIntersection(corners[14], corners[15], d);
			findIntersection(corners[15], corners[12], d);
		}
		
		private synchronized void findIntersections_xz(Transform tr, int d) {
			nI = 0;
			findIntersection(corners[16],  corners[17], d);
			findIntersection(corners[17],  corners[18], d);
			findIntersection(corners[18], corners[19], d);
			findIntersection(corners[19], corners[16], d);
		}
		
		/** Finds the intersection between a line (through p0 and p1) and the plane z = dist
		 * @param p0 Point 0
		 * @param p1 Point 1
		 */
		private synchronized void findIntersection(int[]p0, int[]p1, int d) {
			
			tr.xyzPos(p0);
			float z0 = tr.Z;
			
			tr.xyzPos(p1);
			float z1 = tr.Z;
			
			if ((z0 - z1) != 0) {
				float t = (z0 - d) / ( z0 - z1);
				
				if (t >= 0 && t <= 1) {
					int x0 = p0[0];
					int y0 = p0[1];
					z0 = p0[2];
					int x1 = p1[0];
					int y1 = p1[1];
					z1 = p1[2];
					
					int xs = (int) (x0 + t*(x1-x0));
					int ys = (int) (y0 + t*(y1-y0));
					int zs = (int) (z0 + t*(z1-z0));
					
					int[] v = new int[3];
					
					v[0] = xs;
					v[1] = ys;
					v[2] = zs;
					
					tr.xyzPos(v);
					
					if (col != 0)
						pic.drawCircle(tr.X, tr.Y, 3, col);
					
					boolean newIntersection = true;
					
					for (int i = 0; i < nI; i++)
					{
						if (iS[i][0] == xs  && iS[i][1] == ys && iS[i][2] == zs)
							newIntersection = false;
					}
					if (newIntersection) {
						iS[nI][0] = xs;
						iS[nI][1] = ys;
						iS[nI][2] = zs;
						
						nI++;
						
						//IJ.log(" xs " + xs + " ys " + ys + " zs " + zs);
					}
				}
			}
		}
		
		
		Cube () {
			cornerT = new int[8][4]; // 8 x X, Y, Z, order
			iS = new int[8][3]; // 8 x X, Y, Z
			corners = new int[8+12][3];
			textPos = new int[4][3];
		}
		
		private int nI; // number of the intersection
		private int cornerT[][]; 
		private int corners[][]; 
		private int iS[][]; // intersections
		
		private int [][] textPos;
		
		private Color backColor;
		private Color frontColor;
		
		private final  Color [] letterCol = {Color.black, Color.orange, Color.orange, Color.orange};
		
		private final static int dm = 16; 
		private final static int dp = 10;
		
		private final  String[] letters = {"0", "x", "y", "z"};
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	class ImageRegion extends JPanel { 
		private Image image;
		private int width;
		private int height;
		
		
		private TextField[] textField = null;
		private Lines[] lines = null;
		
		private Color planeColor = Color.lightGray;
		
		private Font font1 = new Font("Sans", Font.PLAIN, 18);
		private Font font2 = new Font("Sans", Font.PLAIN, 15);
		
		private int plotNumber = 1;
		
		public void setPlaneColor(Color color) {
			planeColor = color; 
		}
		
		public void newText(int n) {
			textField = new TextField[n];
			for (int i = 0; i < n; i++) {
				textField[i] = new TextField();
			}
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
		
		public void setLine(int i, int x1, int y1, int x2, int y2, int z, Color color) {
			lines[i] = new Lines(x1, y1, x2, y2, z, color);
		}
		
		public void setImage(Picture pic){
			height = pic.getHeight();
			width = pic.getWidth();
			image = pic.getImage();
		}
		
		public void setImage(Image image){
			this.image = image;
		}
		
		public synchronized void saveToImage() {
			
			pause = true;
			
			BufferedImage bufferedImage =  new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			
			paint(bufferedImage.createGraphics());
			
			Graphics2D g2d = bufferedImage.createGraphics();
			g2d.setColor(Color.white);
			g2d.drawString("Volume Viewer", width - 100, height - 10); 
			g2d.dispose();
			
			String s = "Volume_Viewer_"+plotNumber;
			
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
		public synchronized void paint(Graphics g) {
			
			g.setColor(planeColor);
			g.fillRect(0, 0, width, height);
			
			g.setFont(font1); 
			
			if (textField != null && axes == true)
				for (int i=0; i<textField.length; i++) {
					if (textField[i] != null) 
						if (textField[i].getZ() > 0) {
							g.setColor(textField[i].getColor());
							g.drawString(textField[i].getText(), 
									textField[i].getXpos(), textField[i].getYpos());
						}
				}
			
			if (lines != null && axes == true)
				for (int i=0; i<lines.length; i++) {
					if (lines[i] != null) 
						if (lines[i].z > 0) {
							g.setColor(lines[i].color);
							g.drawLine(lines[i].x1, lines[i].y1, lines[i].x2, lines[i].y2);
						}		
				}
			
			if (image != null ) {
				g.drawImage(image, 0, 0, width, height, this);
			}
			
			if (lines != null  && axes == true)
				for (int i=0; i<lines.length; i++) {
					if (lines[i] != null) 
						if (lines[i].z <= 0) {
							g.setColor(lines[i].color);
							g.drawLine(lines[i].x1, lines[i].y1, lines[i].x2, lines[i].y2);
						}		
				}
			
			if (textField != null  && axes == true)
				for (int i=0; i<textField.length; i++) {
					if (textField[i] != null) 
						if (textField[i].getZ() <= 0) {
							if (i > 4)
								g.setFont(font2);
							
							g.setColor(textField[i].getColor());
							g.drawString(textField[i].getText(), 
									textField[i].getXpos(), textField[i].getYpos());
						}
				}
		}
		//-------------------------------------------------------------------
		
		public void update(Graphics g) {
			paint(g);
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
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private class Transform {
		
		
//		private void initializeTransformation_() {
//			cosB = (float)Math.cos(angleB);
//			sinB = (float)Math.sin(angleB);
//			cosR = (float)Math.cos(angleR); 
//			sinR = (float)Math.sin(angleR);
//
//			// Skalierung und Verschiebung
//			float mat1[][] = new float[4][4];
//			mat1[0][0] = scale ;
//			mat1[0][3] = scale*xoff - scale*xO;
//			mat1[1][1] = scale;
//			mat1[1][3] = scale*yoff - scale*yO; 
//			mat1[2][2] = scale*zAspect; 
//			mat1[2][3] = -scale*zAspect*zO; 
//			mat1[3][3] = 1.0f;
//			
//			
//			float mat2[][] = new float[4][4];
//			
//			mat2[0][0] = -sinB;
//			mat2[0][1] =  cosB;
//			mat2[1][0] =  cosB;
//			mat2[1][1] =  sinB;
//			mat2[2][2] =  1;
//			mat2[3][3] =  1; 
//			
//			float mat21[][] = new float[4][4];
//			
//			geomp4(mat21, mat2, mat1);
//			
//			float mat3[][] = new float[4][4];
//			
//			mat3[0][0] =  1;
//			mat3[0][3] =  xs;
//			mat3[1][1] = -sinR;
//			mat3[1][2] =  cosR;
//			mat3[1][3] =  ys; 
//			mat3[2][1] =  cosR;
//			mat3[2][1] =  sinR;
//			mat3[3][3] =  1; 
//			
//			
//			float matTrans[][] = new float[4][4];
//			
//			geomp4(matTrans, mat3, mat21);
//			
//			
//			a00 = matTrans[0][0];
//			a01 = matTrans[0][1];
//			a02 = matTrans[0][2];
//			a03 = matTrans[0][3];
//			
//			a10 = matTrans[1][0];
//			a11 = matTrans[1][1];
//			a12 = matTrans[1][2];
//			a13 = matTrans[1][3]; 
//			
//			a20 = matTrans[2][0];
//			a21 = matTrans[2][1];
//			a22 = matTrans[2][2];
//			a23 = matTrans[2][3];
//						
//			
//			matInv4(matInvTrans, matTrans);
//			
//			
//			ai00 = matInvTrans[0][0];
//			ai01 = matInvTrans[0][1];
//			ai02 = matInvTrans[0][2];
//			ai03 = matInvTrans[0][3];
//			
//			ai10 = matInvTrans[1][0];
//			ai11 = matInvTrans[1][1];
//			ai12 = matInvTrans[1][2];
//			ai13 = matInvTrans[1][3]; 
//			
//			ai20 = matInvTrans[2][0];
//			ai21 = matInvTrans[2][1];
//			ai22 = matInvTrans[2][2];
//			ai23 = matInvTrans[2][3];
//			
////			ai30 = matInvTrans[3][0];
////			ai31 = matInvTrans[3][1];
////			ai32 = matInvTrans[3][2];
////			ai33 = matInvTrans[3][3];
//			
//			cube.transformCorners();
//		}

		
		private void initializeTransformation() {
						
			int degrees = (int) ((Math.round(Math.toDegrees(angleB))+360) % 360);
			if (degrees > 180) degrees -= 360; 
			
			String st = "" + degrees;
			tf3.setText(st);
			degrees = (int) (Math.round(Math.toDegrees(angleR)+360) % 360);
			if (degrees > 180) degrees -= 360; 
			
			st = "" + degrees;
			tf2.setText(st);
			
			cosB = (float)Math.cos(angleB);
			sinB = (float)Math.sin(angleB);
			cosR = (float)Math.cos(angleR); 
			sinR = (float)Math.sin(angleR);
			
			matrix[0][0] = a00 =  scale*cosB;
			matrix[0][1] = a01 = -scale*sinB;
		    matrix[0][2] = a02 =  0;
			matrix[0][3] = a03 =  xs + scale*xoff - scale*cosB*xO + scale*sinB*yO;
			
			matrix[1][0] = a10 =  scale*cosR*sinB;
			matrix[1][1] = a11 =  scale*cosR*cosB;
			matrix[1][2] = a12 = -scale*sinR*zAspect;
			matrix[1][3] = a13 =  ys + scale*yoff - scale*cosR*sinB*xO - scale*cosR*cosB*yO + scale*zAspect*sinR*zO; 
			
			matrix[2][0] = a20 =  sinR*sinB;
			matrix[2][1] = a21 =  sinR*cosB;
			matrix[2][2] = a22 =  cosR*zAspect;
			matrix[2][3] = a23 = -sinR*sinB*xO - sinR*cosB*yO - cosR*zAspect*zO;
			
			
			tr.matInv4(matrixInv, matrix);
			
			ai00 = matrixInv[0][0];
			ai01 = matrixInv[0][1];
			ai02 = matrixInv[0][2];
			ai03 = matrixInv[0][3];
			
			ai10 = matrixInv[1][0];
			ai11 = matrixInv[1][1];
			ai12 = matrixInv[1][2];
			ai13 = matrixInv[1][3]; 
			
			ai20 = matrixInv[2][0];
			ai21 = matrixInv[2][1];
			ai22 = matrixInv[2][2];
			ai23 = matrixInv[2][3];
			
//			ai30 = matInvTrans[3][0];
//			ai31 = matInvTrans[3][1];
//			ai32 = matInvTrans[3][2];
//			ai33 = matInvTrans[3][3];
			
			cube.transformCorners();
		}

		
		public void setRotationZ(double rho) {
			angleB = rho;
			initializeTransformation();
		}
		
		public void setRotationX(double phi) {
			angleR = phi;
			initializeTransformation();
		}
		
		public void setView(double a, double b) {
			angleB = a;
			angleR = b;
			
			initializeTransformation();	
		}
		
		private final void invxyzPos() {
			x = (int)(ai00*X + ai01*Y + ai02*Z + ai03);
			y = (int)(ai10*X + ai11*Y + ai12*Z + ai13);
			z = (int)(ai20*X + ai21*Y + ai22*Z + ai23);
		}
		private final void invxyzPosf() {
			xf = ai00*X + ai01*Y + ai02*Z + ai03;
			yf = ai10*X + ai11*Y + ai12*Z + ai13;
			zf = ai20*X + ai21*Y + ai22*Z + ai23;
		}
		private final void xyzPos() {
			X = (int)(a00*x + a01*y /*+ a02*z*/ + a03);
			Y = (int)(a10*x + a11*y + a12*z + a13);
			Z = (int)(a20*x + a21*y + a22*z + a23);
		}
		
		private final void xyzPosf() {
			X = (int)(a00*xf + a01*yf /*+ a02*zf*/ + a03);
			Y = (int)(a10*xf + a11*yf + a12*zf + a13);
			Z = (int)(a20*xf + a21*yf + a22*zf + a23);
		}
		
		
		/* 4x4 matrix inverse */
		void matInv4(float z[][], float u[][]) {
			int    i, j, n, ii[] = new int[4];
			float f;
			float w[][] = new float[4][4];
			n=4;
			matCopy4(w,u);
			matUnit4(z);
			for (i=0; i<n; i++) {
				ii[i]=matge4(w,i);
				matXr4(w,i,ii[i]);
				for (j=0; j<n; j++) {
					if (i==j) continue;
					f=-w[i][j]/w[i][i];
					matAc4(w,j,j,f,i);
					matAc4(z,j,j,f,i);
				}
			}
			for (i=0; i<n; i++) 
				matMc4(z,1.0f/w[i][i],i);
			for (i=0; i<n; i++) {
				j=n-i-1; 
				matXc4(z,j,ii[j]);
			}
		}
		
		/* greatest element in the nth column of 4x4 matrix */
		int matge4(float p[][], int n) {
			float g, h; 
			int m;
			m=n;
			g=p[n][n];
			g=(g<0.0?-g:g);
			for (int i=n; i<4; i++) {
				h=p[i][n];
				h=(h<0.0?-h:h);
				if (h<g) continue;
				g=h; m=i;
			}
			return m;
		}
		
		/* copy 4x4 matrix */
		void matCopy4(float z[][], float x[][]) {
			int i, j;
			for (i=0; i<4; i++) 
				for (j=0; j<4; j++) 
					z[i][j]=x[i][j];
		}
		
		/* 4x4 unit matrix */
		void matUnit4(float z[][]) {
			for (int i=0; i<4; i++) {
				for (int j=0; j<4; j++) 
					z[i][j]=0.0f;
				z[i][i]=1.0f;
			}
		}
		
		/* exchange ith and jth columns of a 4x4 matrix */
		void matXc4(float z[][], int i, int j) {
			float t;
			if (i==j) 
				return;
			for (int k=0; k<4; k++) {
				t=z[k][i]; 
				z[k][i]=z[k][j]; 
				z[k][j]=t;}
		}
		
		/* exchange ith and jth rows of a 4x4 matrix */
		void matXr4(float z[][], int i, int j) {
			float t;
			if (i==j) 
				return;
			for (int k=0; k<4; k++) {
				t=z[i][k]; 
				z[i][k]=z[j][k]; 
				z[j][k]=t;
			}
		}
		
		/* extract nth column from 4x4 matrix */
		void matXtc4(float p[], float z[][], int n) {
			int i;
			for (i=0; i<4; i++) 
				p[i]=z[i][n];
		}
		
		/* augment column of a 4x4 matrix */
		void matAc4(float z[][], int i, int j, float f, int k) {
			int l;
			for (l=0; l<4; l++) 
				z[l][i] = z[l][j] + f*z[l][k];
		}
		
		/* multiply ith column of 4x4 matrix by a factor */
		void matMc4(float z[][], float f, int i) {
			int j;
			for (j=0; j<4; j++) 
				z[j][i]*=f;
		}
		
		/* product of two 4x4 matrices */
		void geomp4(float z[][], float x[][], float y[][]) {
			int i, j, k;
			float u[][] = new float[4][4];
			float v[][] = new float[4][4];
			
			matCopy4(u,x);
			matCopy4(v,y);
			for (i=0; i<4; i++) 
				for (j=0; j<4; j++) {
					z[i][j]=0.0f;
					for (k=0; k<4; k++) 
						z[i][j]+=u[i][k]*v[k][j];
				}
		}
		
		
		private final void xyzPos(int[] xyz) {
			x = xyz[0];
			y = xyz[1];
			z = xyz[2];
			xyzPos();
		}
		
		private final void xyzPosf(float[] xyz) {
			xf = xyz[0];
			yf = xyz[1];
			zf = xyz[2];
			xyzPosf();
		}
		
		private final void invxyzPos(int[] XYZ) {
			X = XYZ[0];
			Y = XYZ[1];
			Z = XYZ[2];
			invxyzPos();
		}
		
		private final void invxyzPosf(int[] XYZ) {
			X = XYZ[0];
			Y = XYZ[1];
			Z = XYZ[2];
			invxyzPosf();
		}
		
		public void setScale(float scale) {
			this.scale = scale;		
			initializeTransformation();
		}
		
		public void setMouseMovement(float dx, float dy) {
			angleB += dx/100.;
			angleR += dy/100.;
			
			initializeTransformation();			
		}
		
		public void setMouseMovementOffset(int dx, int dy) {
			xoff += dx;
			yoff += dy;
			
			initializeTransformation();
		}
		
		Transform(int width, int height ) {
			xs = (float)(width/2.  + 0.5);
			ys = (float)(height/2. + 0.5);
			
			matrix[3][3] = 1.0f;
			
			xO = vol.xOffa;
			yO = vol.yOffa;
			zO = vol.zOffa;
			
			int degrees = (int) ((Math.round(Math.toDegrees(angleB))+360) % 360);
			if (degrees > 180) degrees -= 360; 
			String st = "" + degrees;
			tf3.setText(st);
			
			degrees = (int) (Math.round(Math.toDegrees(angleR)) % 360);
			if (degrees > 180) degrees -= 360; 
			tf2.setText(st);
			
		};
		
		private double angleB; // = -0.6125; // angle for B-rotation
		private double angleR; // =  2;       // angle for R-rotation
		
		private float xs = 256; 
		private float ys = 256; 
		private int xoff = 0;
		private int yoff = 0;
		
		private float cosB; // = (float)Math.cos(angleB), 
		private float sinB; // = (float)Math.sin(angleB);
		private float cosR; // = (float)Math.cos(angleR); 
		private float sinR; // = (float)Math.sin(angleR);
		
		private float scale = 1;
		
		float matrix[][] = new float[4][4];
		float matrixInv[][] = new float[4][4];
		
		int xO;
		int yO;
		int zO;
		
		float a00, a01, a02, a03;  // coefficients of the tramsformation
		float a10, a11, a12, a13;
		float a20, a21, a22, a23;
		//float a30, a31, a32, a33;
		
		float ai00, ai01, ai02, ai03;  // coefficients of the inverse tramsformation
		float ai10, ai11, ai12, ai13;
		float ai20, ai21, ai22, ai23;
		//float ai30, ai31, ai32, ai33;
		
		private int y, x, z; // volume coordinates
		int[] xyz;
		private float yf, xf, zf; 
		
		private int X, Y, Z; // screen coordinates
		float Xf = 0;
		float Yf = 0;
		float Zf = 0;
		//private float[] XYZf;
		
	}
	/////////////////////////////////////////////////////////////////////////
	
	private  class Volume {
		
		private int widthX;   				
		private int heightY;  	 	   
		private int depthZ;

		private int xOffa;   				
		private int yOffa;  	 	   
		private int zOffa; 
		
		double a = 0, b = 1;
		
		private double min, max;
		
		double oldMin, oldMax;
		
		boolean firstTime = true;
		private int dS;
		private int hS;
		private int wS;
		private int depthS;
		private int heightS;
		private int widthS;
		
		boolean getMinMax() {
			min = ip.getMin();
			max = ip.getMax();
	
			Calibration cal = imp.getCalibration();
	
			if (cal != null) {
				if (cal.calibrated()) {
					
					min = cal.getCValue((int)min);
					max = cal.getCValue((int)max);
					
					double[] coef = cal.getCoefficients();
					if (coef != null) {		
						a = coef[0];
						b = coef[1];
					}
				}
			}
			
			if (firstTime) {
				// 1.06
				if (zAspect == 1) 
					zAspect = (float) (cal.pixelDepth / cal.pixelWidth);
				if (zAspect == 0)
					zAspect = 1;
				
				lut = new Lut();
				lut.readLut();
				
				oldMin = min;
				oldMax = max;
				firstTime = false;
			}
			
			boolean changed;
			if (oldMin == min && oldMax == max) 
				changed = false;
			else
				changed = true;
			oldMin = min;
			oldMax = max;

			return changed;
		}
		
		public Volume(){
			ip = imp.getProcessor();
			
			widthX = imp.getWidth();
			heightY = imp.getHeight();
			depthZ = imp.getStackSize();
			
			if (isRGB )
				data3D = new byte[3][depthZ][heightY][widthX]; //z y x
			else
				data3D = new byte[1][depthZ][heightY][widthX]; //z y x
			
			depthS = depthZ;
			heightS = heightY;
			widthS = widthX;
			
			dS = 1;
			hS = 1;
			wS = 1;
			
			int n = 128;
			while (depthS > n) {
				depthS /= 2;
				dS *=2;
			}
			
			while (heightS > n) {
				heightS /= 2;
				hS *= 2;
			}
			
			while (widthS > n) {
				widthS /= 2;
				wS *= 2;
			}
			
			//IJ.log( "depthS " + depthS + " heightS " + heightS + " widthS " + widthS );

			if (isRGB)
				data3DS = new byte[3][depthS][heightS][widthS]; //z y x
			else
				data3DS = new byte[1][depthS][heightS][widthS]; //z y x
				
			
			xOffa  = (widthX -1)/2;   				
			yOffa  = (heightY-1)/2;  	 	   
			zOffa  = (depthZ -1)/2;
			
			getMinMax();
			
			init();
		}
		
		private void init() {

			// Cube
			cube.corners[1][1] =  heightY-1;     
			cube.corners[1][2] =  depthZ -1;     
			cube.corners[2][0] =  widthX -1;     
			cube.corners[2][2] =  depthZ -1;     
			cube.corners[3][0] =  widthX -1;     
			cube.corners[3][1] =  heightY-1;     
			cube.corners[4][2] =  depthZ -1;     
			cube.corners[5][1] =  heightY-1;     
			cube.corners[6][0] =  widthX -1;     
			cube.corners[7][0] =  widthX -1;     
			cube.corners[7][1] =  heightY-1;     
			cube.corners[7][2] =  depthZ -1;     
			
			// Marker detection
			cube.corners[8][0]  =  0;     
			cube.corners[8][1]  =  0;     
			cube.corners[8][2]  =  (depthZ -1)/2;
			cube.corners[9][0]  =  (widthX -1);     
			cube.corners[9][1]  =   0;     
			cube.corners[9][2]  =  (depthZ -1)/2;     
			cube.corners[10][0] =  (widthX -1);     
			cube.corners[10][1] =  heightY-1;     
			cube.corners[10][2] =  (depthZ -1)/2;
			cube.corners[11][0] =  0;     
			cube.corners[11][1] =  heightY-1;     
			cube.corners[11][2] =  (depthZ -1)/2;
			
			cube.corners[12][0] =  (widthX -1)/2;     
			cube.corners[12][1] =  0;     
			cube.corners[12][2] =  0;
			cube.corners[13][0] =  (widthX -1)/2;     
			cube.corners[13][1] =   0;     
			cube.corners[13][2] = depthZ -1;     
			cube.corners[14][0] =  (widthX -1)/2;  
			cube.corners[14][1] =  heightY-1;     
			cube.corners[14][2] =  depthZ -1;
			cube.corners[15][0] =  (widthX -1)/2;
			cube.corners[15][1] =  heightY-1;     
			cube.corners[15][2] =  0;
			
			cube.corners[16][0] =  0;     
			cube.corners[16][1] =  (heightY -1)/2;     
			cube.corners[16][2] =  0;
			cube.corners[17][0] =  0;     
			cube.corners[17][1] =  (heightY -1)/2;     
			cube.corners[17][2] =  depthZ -1;     
			cube.corners[18][0] =  widthX -1;  
			cube.corners[18][1] =  (heightY -1)/2; 
			cube.corners[18][2] =  depthZ -1;
			cube.corners[19][0] =  widthX -1;
			cube.corners[19][1] =  (heightY -1)/2;     
			cube.corners[19][2] =  0;
			
			
			cube.textPos[0][0] = (int) ( -Cube.dm/2); 
			cube.textPos[1][0] = (int) (  Cube.dp +  widthX -1); 
			cube.textPos[2][0] = (int) ( -Cube.dm); 
			cube.textPos[3][0] = (int) ( -Cube.dm); 
			
			cube.textPos[0][1] = (int) ( -Cube.dm/2); 
			cube.textPos[1][1] = (int) ( -Cube.dm); 
			cube.textPos[2][1] = (int) (  Cube.dp + heightY -1); 
			cube.textPos[3][1] = (int) ( -Cube.dm); 
			
			cube.textPos[0][2] = (int) ( -Cube.dm/(2*zAspect)); 
			cube.textPos[1][2] = (int) ( -Cube.dm/zAspect); 
			cube.textPos[2][2] = (int) ( -Cube.dm/zAspect); 
			cube.textPos[3][2] = (int) (  Cube.dp/zAspect + depthZ-1); 
			
			
			
			ImageStack stack = imp.getStack();
			
			int bitDepth = imp.getBitDepth();
			if (bitDepth == 8) {
				float scale = (float) (255f/(max-min));
				
				for (int z=0;z<depthZ;z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					
					byte[] pixels = (byte[]) stack.getPixels(z+1);
					
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						for (int x = 0; x <widthX; x++) {
							int val = 0xff & pixels[pos++];
							val = (int)((val-min)*scale);
							
							if (val<0f) val = 0;
							if (val>255) val = 255;
							
							data3D[0][z][y][x] = (byte)(0xff & val); 
						}
					}
				}
			}
	
			if (bitDepth == 16) {
				float scale = (float) (255f/(max-min));
				
				for (int z=0;z<depthZ;z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					short[] pixels = (short[]) stack.getPixels(z+1);
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						
						for (int x = 0; x <widthX; x++) {
							
							int val = (int) ((int)(0xFFFF & pixels[pos++])*b + a - min);
							if (val<0f) val = 0;
							val = (int)(val*scale);
							
							if (val>255) val = 255;
							data3D[0][z][y][x] = (byte)(val);  
						}
					}
				}							
			}
			
			if (bitDepth == 24) {
				//float scale = (float) (255f/(max-min));
				
				for (int z=0;z<depthZ;z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					int[] pixels = (int[]) stack.getPixels(z+1);
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						
						for (int x = 0; x <widthX; x++) {
							
							int val = pixels[pos++];
							
							data3D[0][z][y][x] = (byte)((val>>16)&0xFF);  
							data3D[1][z][y][x] = (byte)((val>>8)&0xFF);  
							data3D[2][z][y][x] = (byte)(val&0xFF);  
							
						}
					}
				}							
			}
			
			if (bitDepth == 32) {
				
				float scale = (float) (255f/(max-min));
				
				for (int z=0; z<depthZ; z++) {
					IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
					IJ.showProgress((double)3*z/(4*depthZ));
					float[] pixels = (float[]) stack.getPixels(z+1);
					
					int pos = 0;
					for (int y = 0; y < heightY; y++) {
						for (int x = 0; x <widthX; x++) {
							float value = (float) (pixels[pos++] - min);
							if (value<0f) value = 0f;
							int ivalue = (int)(value*scale);
							
							if (ivalue>255) ivalue = 255;
							data3D[0][z][y][x] = (byte)(ivalue);  
						}
					}
				}
			}
			
			float d = 1f/(hS*wS*dS);
			
			IJ.showStatus("Converting stack");
			
			if (!isRGB) {
				for (int z=0, zs=0; z<dS*(depthZ/dS); z+=dS, zs++) {
					IJ.showProgress((double)z/(4*depthZ)+0.75);

					for (int y = 0, ys = 0; y < hS*(heightY/hS); y+=hS, ys++) {
						for (int x = 0, xs= 0; x <wS*(widthX/wS); x+=wS, xs++) {

							int val = 0;
							for (int z_ = z; z_ < z + dS; z_++)
								for (int y_ = y; y_ < y + hS; y_++)
									for (int x_ = x; x_ < x + wS; x_++)
										val += (int)(0xFF & data3D[0][z_][y_][x_]);

							val = (int)(val * d);

							data3DS[0][zs][ys][xs] = (byte) val;
						}
					}
				}
			}
			else {
				for (int ch = 0; ch< 3; ch ++) {
					for (int z=0, zs=0; z<dS*(depthZ/dS); z+=dS, zs++) {
						IJ.showProgress((double)z/(4*depthZ)+0.75);

						for (int y = 0, ys = 0; y < hS*(heightY/hS); y+=hS, ys++) {
							for (int x = 0, xs= 0; x <wS*(widthX/wS); x+=wS, xs++) {

								int val = 0;
								for (int z_ = z; z_ < z + dS; z_++)
									for (int y_ = y; y_ < y + hS; y_++)
										for (int x_ = x; x_ < x + wS; x_++)
											val += (int)(0xFF & data3D[ch][z_][y_][x_]);

								val = (int)(val * d);

								data3DS[ch][zs][ys][xs] = (byte) val;
							}
						}
					}
				}
			}
				

			IJ.showProgress(1.0);
			IJ.showStatus("");
		}
		

		int getWidth(){
			return widthX;
		}
		int getHeight(){
			return heightY;
		}
	}
	
	
	
	private class Lut {
		private int [] colors;
		private int [] origColors;
		
		Lut() {
			colors = new int[256];
			origColors = new int[256];
		}
		
		public void setLut() {
			
			switch (lutNr) {
			case ORIG:	 
				orig();
				break;
			case GRAY:	 
				gray();
				break;
			case SPECTRUM:  
				spectrum();
				break;
			case FIRE:  
				fire();
				break;
			case THERMAL:  
				thermal();
				break;
			}
		}
		
		void readLut() {
			LookUpTable lut_ = imp.createLut();
			int mapSize = 0;
			java.awt.image.ColorModel cm = lut_.getColorModel();
			byte[] rLUT,gLUT,bLUT;
			if (cm instanceof IndexColorModel) {
				IndexColorModel m = (IndexColorModel)cm;
				mapSize = m.getMapSize();
				rLUT = new byte[mapSize];
				gLUT = new byte[mapSize];
				bLUT = new byte[mapSize];
				m.getReds(rLUT);
				m.getGreens(gLUT);
				m.getBlues(bLUT);
				
				for (int i=0; i<256; i++) {
					byte r = rLUT[i];
					byte g = gLUT[i];
					byte b = bLUT[i];
					
					origColors[i] = colors[i] = 0xff000000  | ((int)(r&0xFF) << 16) | ((int)(g&0xFF) <<8) | (int)(b&0xFF);
				}
			} 
			else {
				for (int i=0; i<256; i++) {
					origColors[i] = colors[i] = 0xff000000  | (i << 16) | (i <<8) | i;
				}
			}
		}
		
		private final int [] fireTable = { 0,0,31,0,0,31,0,0,33,0,0,35,0,0,37,0,0,39,0,0,41,0,0,43,0,0,45,0,0,47,0,0,49,0,0,52,0,0,54,0,0,57,0,0,59,0,0,62,
				0,0,64,0,0,67,0,0,70,0,0,73,0,0,76,0,0,79,0,0,82,0,0,85,0,0,88,0,0,92,2,0,96,3,0,99,5,0,102,7,0,105,10,0,108,13,0,112,
				15,0,116,17,0,119,20,0,122,22,0,126,25,0,130,28,0,134,31,0,138,33,0,141,35,0,145,38,0,149,41,0,152,43,0,156,46,0,160,49,0,164,52,0,168,55,0,171,
				58,0,175,61,0,178,64,0,181,67,0,184,70,0,188,73,0,191,76,0,195,78,0,198,81,0,202,85,0,205,88,0,209,91,0,212,94,0,216,98,0,218,101,0,220,104,0,221,
				107,0,222,110,0,223,113,0,224,116,0,225,119,0,226,122,0,225,126,0,224,129,0,222,133,0,219,136,0,217,140,0,214,143,0,212,146,0,209,148,0,206,150,0,202,153,0,198,
				155,0,193,158,0,189,160,0,185,162,0,181,163,0,177,165,0,173,166,0,168,168,0,163,170,0,159,171,0,154,173,0,151,174,0,146,176,0,142,178,0,137,179,0,133,181,0,129,
				182,0,125,184,0,120,186,0,116,188,0,111,189,0,107,191,0,103,193,0,98,195,0,94,196,1,89,198,3,85,200,5,80,202,8,76,204,10,71,205,12,67,207,15,63,209,18,58,
				210,21,54,212,24,49,213,27,45,215,31,40,217,34,36,218,37,31,220,40,27,222,44,22,224,48,17,226,51,12,227,54,8,229,58,5,231,61,4,233,65,3,234,68,2,236,72,1,
				238,75,0,240,79,0,241,82,0,243,85,0,245,89,0,247,92,0,249,95,0,250,99,0,251,102,0,252,105,0,253,107,0,253,110,0,253,112,0,254,115,0,255,117,0,255,119,0,
				255,122,0,255,125,0,255,127,0,255,129,0,255,131,0,255,134,0,255,136,0,255,138,0,255,140,0,255,142,0,255,145,0,255,147,0,255,149,0,255,151,0,255,153,0,255,155,0,
				255,157,0,255,159,0,255,161,0,255,163,0,255,166,0,255,168,0,255,169,0,255,171,0,255,173,0,255,176,0,255,178,0,255,180,0,255,182,0,255,184,0,255,186,0,255,189,0,
				255,191,0,255,193,0,255,195,0,255,197,0,255,199,0,255,201,0,255,203,0,255,205,0,255,208,0,255,210,0,255,212,0,255,213,0,255,215,0,255,217,0,255,219,0,255,220,0,
				255,222,0,255,224,0,255,226,0,255,228,0,255,230,0,255,232,1,255,234,3,255,236,6,255,238,10,255,239,14,255,241,18,255,243,22,255,244,27,255,246,31,255,248,37,255,248,43,
				255,249,50,255,250,58,255,251,66,255,252,74,255,253,81,255,254,88,255,255,95,255,255,102,255,255,108,255,255,115,255,255,122,255,255,129,255,255,136,255,255,142,255,255,148,255,255,154,
				255,255,161,255,255,167,255,255,174,255,255,180,255,255,185,255,255,192,255,255,198,255,255,204,255,255,210,255,255,215,255,255,221,255,255,225,255,255,228,255,255,231,255,255,234,255,255,236,
				255,255,239,255,255,242,255,255,244,255,255,247,255,255,249,255,255,251,255,255,253,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255
		};
		
		private final int [] tempTable = { 70,0,115,70,0,115,70,0,116,70,0,118,70,0,120,70,0,122,70,0,124,70,0,126,70,0,128,70,0,131,70,0,133,70,0,136,70,0,139,70,0,141,70,0,144,70,0,147,
				70,0,151,70,0,154,70,0,157,70,0,160,70,0,164,70,0,167,70,0,170,70,0,174,70,0,177,70,0,181,70,0,184,70,0,188,70,0,194,70,0,200,70,0,206,70,0,211,
				70,0,217,70,0,222,70,0,227,70,0,232,70,0,236,70,0,240,70,0,244,69,0,248,69,0,251,68,0,253,67,2,255,66,5,255,64,9,255,63,13,255,61,17,255,59,22,255,
				57,27,255,55,32,255,53,38,255,51,44,255,48,50,255,45,57,255,43,63,255,40,70,255,37,77,255,34,84,255,31,91,255,28,98,255,26,106,255,23,113,254,20,121,253,17,128,252,
				14,136,251,12,144,250,9,152,248,6,160,247,4,168,246,2,176,245,0,183,243,0,191,242,0,198,241,0,205,240,0,212,239,0,218,238,0,224,237,0,230,236,0,235,235,0,240,235,
				0,245,235,0,249,234,0,253,234,1,255,234,4,255,234,7,255,234,11,255,235,16,255,236,21,255,237,27,255,238,32,255,239,39,255,240,45,255,241,52,255,243,60,255,244,68,255,246,
				76,255,247,84,255,249,92,255,250,101,255,252,109,255,253,117,255,254,126,255,254,134,255,254,143,255,254,152,255,254,160,255,254,168,255,254,176,255,254,184,255,254,192,255,254,199,255,254,
				206,255,254,213,255,254,219,255,254,225,255,254,231,255,254,236,255,254,240,255,254,244,255,254,247,255,254,250,255,254,252,254,254,253,254,254,254,254,254,254,254,252,253,254,249,252,254,246,
				251,254,243,249,254,239,246,254,236,243,254,231,240,254,227,237,254,223,233,254,218,228,254,213,223,255,208,219,255,203,214,255,198,208,255,192,203,255,187,196,255,181,190,255,175,184,255,169,
				178,255,163,171,255,157,165,255,151,158,255,145,151,255,138,144,255,132,138,255,126,129,255,118,120,255,110,112,255,102,103,255,94,95,255,87,87,255,79,79,255,72,71,255,65,64,255,58,
				57,255,51,51,255,45,45,255,38,39,255,32,35,255,27,30,255,22,27,255,17,24,255,13,21,255,8,20,255,5,19,255,2,19,255,1,21,255,1,23,255,1,27,255,1,32,255,1,
				37,255,1,44,255,1,51,255,1,59,255,1,68,255,1,77,255,1,86,255,1,97,255,3,107,255,5,118,255,8,125,255,10,131,255,12,137,255,14,144,255,16,150,255,17,156,255,19,
				162,255,21,168,255,23,174,255,25,180,255,26,185,255,28,191,255,30,197,254,31,202,254,33,207,252,34,212,252,36,217,250,37,222,249,38,227,248,39,231,246,40,235,245,41,238,243,42,
				242,241,43,245,239,43,248,237,43,251,235,44,253,233,44,255,229,44,255,226,44,255,222,43,255,218,43,255,213,42,255,208,42,255,203,41,255,198,40,255,192,40,255,187,39,255,181,38,
				255,175,37,255,169,36,255,162,34,255,156,33,255,149,32,255,143,31,255,136,30,255,129,28,255,122,27,255,116,25,255,109,24,255,102,23,255,95,21,255,89,20,255,82,19,255,76,17,
				255,70,16,255,63,15,255,57,13,255,51,12,255,45,11,255,40,10,255,35,9,255,29,7,255,25,6,255,20,6,255,16,5,255,12,4,255,8,3,255,5,3,255,2,2,255,2,2
		};
		
		void spectrum() {
			Color c;
			for (int i=0; i<256; i++) {
				c = Color.getHSBColor(i/255f, 1f, 1f);
				int r = c.getRed();
				int g = c.getGreen();
				int b = c.getBlue();
				colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
			}
		}
		
		void gray() {
			for (int i=0; i<256; i++) {
				colors[i] = 0xff000000  | (i << 16) | (i <<8) | i;
			}
		}
		
		void orig() {
			for (int i=0; i<256; i++) {
				colors[i] = origColors[i];
			}
		}
		
		void fire() {
			for (int i=0; i<256; i++) {
				int r = fireTable[3*i];
				int g = fireTable[3*i+1];
				int b = fireTable[3*i+2];
				colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
			}
		}
		
		void thermal() {
			for (int i=0; i<256; i++) {
				int r = tempTable[3*i];
				int g = tempTable[3*i+1];
				int b = tempTable[3*i+2];
				colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	class Lines {
		private int x1;
		private int y1;
		private int x2;
		private int y2;
		private int z;
		private Color color;
		
		public Lines(int x1, int y1, int x2, int y2, int z, Color color) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.z = z;
			this.color = color;	
		}
		
		public Lines() {
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
		
		public TextField(String text, Color color, int xpos, int ypos, int z) {
			this.text = text;
			this.color = color;
			
			this.xpos = xpos;
			this.ypos = ypos;
			this.z = z;
		}
		
		public TextField() {
		}
		
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
				s = " " + s;
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
		
		static boolean inside(int[] p, int[] p1, int[] p2, int[] p3) {
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
		
		static boolean inside(int x, int y, int x1, int y1, int x2, int y2, int x3, int y3) {
			
			int a = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);
			int b = (x3 - x2) * (y - y2) - (y3 - y2) * (x - x2);
			int c = (x1 - x3) * (y - y3) - (y1 - y3) * (x - x3);
			
			if ((a >= 0 && b >= 0 && c >= 0) || (a <= 0 && b <= 0 && c <= 0))
				return true;
			else
				return false;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	private  class Picture {
		
		private Image  image;    // AWT-Image
		
		private ImageRegion imageRegion = null;
		
		private int[] pixels = null;		
		private int[][] pixelsZ = null;		// Z-buffer
		
		private MemoryImageSource source;
		
		private int width, height;
		
		private int vW, vH, vD;
		
		public Picture (int width, int height){
			this.width = width;
			this.height = height;
			
			pixels  = new int[width*height];
			if (isRGB)
				pixelsZ  = new int[3][width*height];
			else
				pixelsZ  = new int[1][width*height];
				
			
			source = new MemoryImageSource(width, height, pixels, 0, width);
			image = Toolkit.getDefaultToolkit().createImage(source);
			

		}
		
		//float  zD = 1;
		//float  yD = 1;
		//float  xD = 1;
		
		
		/**
		 * @param vol
		 */
		public void setVol(Volume vol) {
			vW = vol.widthX-1;
			vH = vol.heightY-1;
			vD = vol.depthZ-1;			
		}

		private float s;
		private float xd;
		private float yd;
		private float zd;
		
		private int xo;
		private int yo_xy;
		private int yo_yz;
		private int yo_xz;

		// show the three cuts
		public void drawPlanarViews() {
			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = 0xFFD4D0C8;
			}
			
			int xs = 10, ys = 15;
			xo = xs;
			yo_xy = ys;
			
			int my = (int) (vol.heightY + vol.depthZ*Math.abs(zAspect)*2);
			int mx = Math.max(vol.widthX, vol.heightY);
			
			float sy = (height-4*ys)/(float)my;
			float sx = (width-2*xs)/(float)mx;
			
			s = Math.min(sy, sx);
			
			int Wx = (int) (vol.widthX*s);
			int Wy = (int) (vol.heightY*s);
			int Wz = (int) (vol.depthZ*Math.abs(zAspect)*s);
			
			xd = (vol.widthX-1)/(float)Wx;
			yd = (vol.heightY-1)/(float)Wy;
			zd = (vol.depthZ-1)/(float)Wz;
			
			if (isRGB) {
				// xy
				for (int y=0; y < Wy; y++) {
					int y_ = (int) (y*yd);
				
					for (int x=0; x < Wx; x++) {
						int pos = (yo_xy+y)*width + x + xo;
						int x_ = (int) (x*xd);
						int z_ = vol.zOffa;

						int valR = 0xFF & data3D[0][z_][y_][x_];
						int valG = 0xFF & data3D[1][z_][y_][x_];
						int valB = 0xFF & data3D[2][z_][y_][x_];
						pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
					}	
				}

				// yz
				yo_yz = (int) (2*ys+Wy);

				for (int y=0; y < Wz; y++) {
					int z_ = (int) Math.round(vol.depthZ-1 - (y*zd));
					for (int x=0; x < Wy; x++) {
						int pos = (yo_yz+y)*width + x + xo;
						int x_ = vol.xOffa; 
						int y_ = (int) (x*yd);

						int valR = 0xFF & data3D[0][z_][y_][x_];
						int valG = 0xFF & data3D[1][z_][y_][x_];
						int valB = 0xFF & data3D[2][z_][y_][x_];
						pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
					}	
				}

				// xz
				yo_xz = (int) (3*ys+Wy+Wz);

				for (int y=0; y < Wz; y++) {
					int z_ = (int) Math.round(vol.depthZ-1 - (y*zd));
					for (int x=0; x < Wx; x++) {
						int pos = (yo_xz+y)*width + x + xo;
						int x_ = (int) (x*xd); 
						int y_ = vol.yOffa;
					
						int valR = 0xFF & data3D[0][z_][y_][x_];
						int valG = 0xFF & data3D[1][z_][y_][x_];
						int valB = 0xFF & data3D[2][z_][y_][x_];
						pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
					}
				}
			}
			else {
				// xy
				for (int y=0; y < Wy; y++){
					int y_ = (int) (y*yd);
					for (int x=0; x < Wx; x++) {
						int pos = (yo_xy+y)*width + x + xo;
						int x_ = (int) (x*xd);
						int z_ = vol.zOffa;

						int val = 0xFF & data3D[0][z_][y_][x_];
						pixels[pos] = lut.colors[val];
					}	
				}

				// yz
				yo_yz = (int) (2*ys+Wy);

				for (int y=0; y < Wz; y++) {
					int z_ = (int) Math.round(vol.depthZ-1 - (y*zd));
					
					for (int x=0; x < Wy; x++) {
						int pos = (yo_yz+y)*width + x + xo;
						int x_ = vol.xOffa; 
						int y_ = (int) (x*yd);
					
						int val = 0xFF & data3D[0][z_][y_][x_];
						pixels[pos] = lut.colors[val];
					}	
				}

				// xz
				yo_xz = (int) (3*ys+Wy+Wz);

				for (int y=0; y < Wz; y++) {
					int z_ = (int) Math.round(vol.depthZ-1 - (y*zd));
					
					for (int x=0; x < Wx; x++) {
						int pos = (yo_xz+y)*width + x + xo;
						int x_ = (int) (x*xd); 
						int y_ = vol.yOffa;
					
						int val = 0xFF & data3D[0][z_][y_][x_];
						pixels[pos] = lut.colors[val];
					}
				}
			}
				
			
			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
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
		
		private void updateImage() {
			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
			imageRegion.repaint();
		}
		
		public void newDisplayMode(){
		
			
			if (vol.getMinMax())  { 
				IJ.error ("Original Image has changed! Image data needs to be updated.");
				vol.init();
				pic2.drawPlanarViews();
				drag = false;
			}
			
			cube.setTextAndLines(imageRegion, tr);
			
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0; 
			}
			
			if (displayMode == PROJECTION_TRILINEAR_FRONT){ 
				if (drag)
					volume_dots();
				else
					projection_trilinear_front();
				move = false;
			} 
			else if (displayMode == PROJECTION_TRILINEAR_BACK){ 
				if (drag)
					volume_dots();
				else {
					projection_trilinear_back();
					rendered++;
					System.out.println(rendered);
				}
				move = false;
			} 
			else if (displayMode == SLICE_AND_BORDERS){ 
				if (drag)
					volume_dots();
				else
					slice_and_borders();
				move = false;
			} 
			
			else if (displayMode == SLICE_TRILINEAR)  
				slice_trilinear();
			else if (displayMode == SLICE_NN)  
				slice_NN();
			else if (displayMode == SLICE_AND_VOLUME_DOTS)  
				slice_and_volume_dots();
			else if (displayMode == VOLUME_DOTS)  
				volume_dots();
			
			findLines(); 
			
			updateImage();	
			pic2.updateImage();
		}
		
		private void findLines() {
			int[][] iS = cube.iS;
			
			cube.col = 0xFFFF0000;
			cube.findIntersections(tr, dist);
			
			iS[0][0] = iS[1][0] = -1;
			
			cube.col = (markers) ? 0xFF0000FF : 0;
			cube.findIntersections_xy(tr, dist);
			
			if (iS[1][0] != -1) {
				int x1 = (int) (pic2.xo    + iS[0][0]/pic2.xd);
				int x2 = (int) (pic2.xo    + iS[1][0]/pic2.xd);
				int y1 = (int) (pic2.yo_xy + iS[0][1]/pic2.yd);
				int y2 = (int) (pic2.yo_xy + iS[1][1]/pic2.yd);
				
				imageRegion2.setLine(0, x1, y1, x2, y2, -1, Color.blue);
			}
			else
				imageRegion2.setLine(0, 0, 0, 0, 0, 1, Color.blue);
			
			
			iS[0][0] = iS[1][0] = -1;
			cube.col =  (markers) ? 0xFF00FF00 : 0;
			cube.findIntersections_yz(tr, dist);
			
			if (iS[1][0] != -1) {
				int x1 = (int) (pic2.xo    + iS[0][1]/pic2.yd);
				int x2 = (int) (pic2.xo    + iS[1][1]/pic2.yd);
				int y1 = (int) (pic2.yo_yz + (vol.depthZ-1 - iS[0][2])/pic2.zd);
				int y2 = (int) (pic2.yo_yz + (vol.depthZ-1 - iS[1][2])/pic2.zd);
				
				imageRegion2.setLine(1, x1, y1, x2, y2, -1, Color.green);
			}
			else
				imageRegion2.setLine(1, 0, 0, 0, 0, 1, Color.green);
			
			iS[0][0] = iS[1][0] = -1;
			cube.col =  (markers) ? 0xFFFF0000 : 0;
			cube.findIntersections_xz(tr, dist);
			
			if (iS[1][0] != -1) {
				int x1 = (int) (pic2.xo    + iS[0][0]/pic2.xd);
				int x2 = (int) (pic2.xo    + iS[1][0]/pic2.xd);
				int y1 = (int) (pic2.yo_xz + (vol.depthZ-1 - iS[0][2])/pic2.zd);
				int y2 = (int) (pic2.yo_xz + (vol.depthZ-1 - iS[1][2])/pic2.zd);
				
				imageRegion2.setLine(2, x1, y1, x2, y2, -1, Color.red);
			}
			else
				imageRegion2.setLine(2, 0, 0, 0, 0, 1, Color.red);
			
		}

		private void drawCircle(int xs, int ys, int rad, int color) {
			for (int y = -rad; y <=  rad; y++)
				for (int x = -rad; x <= rad; x++) {
					if (x*x+y*y <= rad*rad) {
						int Yy = ys+y;
						int Xx = xs+x;
						
						if (Xx >= 0 && Xx < width && Yy >= 0 && Yy < height) { 
							pixels[(Yy)*width + Xx] = color;		
						}
					}
				}
		}
		
		private int trilinear(byte[][][] data3D, float z, float y, float x) {
			
			int tx = (int)x;
			float dx = x - tx;
			int tx1 = (tx < vW) ? tx+1 : tx;
			int ty = (int)y;
			float dy = y - ty;
			int ty1 = (ty < vH) ? ty+1 : ty;
			int tz = (int)z;
			float dz = z - tz;
			int tz1 = (tz < vD) ? tz+1 : tz;
			
			int  v000 = (int) (0xff & data3D[tz ][ty ][tx ]);
			int  v001 = (int) (0xff & data3D[tz1][ty ][tx ]); 
			int  v010 = (int) (0xff & data3D[tz ][ty1][tx ]); 
			int  v011 = (int) (0xff & data3D[tz1][ty1][tx ]); 
			int  v100 = (int) (0xff & data3D[tz ][ty ][tx1]); 
			int  v101 = (int) (0xff & data3D[tz1][ty ][tx1]); 
			int  v110 = (int) (0xff & data3D[tz ][ty1][tx1]); 
			int  v111 = (int) (0xff & data3D[tz1][ty1][tx1]); 
			
			return (int) (
					(v100 - v000)*dx + 
					(v010 - v000)*dy + 
					(v001 - v000)*dz +
					(v110 - v100 - v010 + v000)*dx*dy +
					(v011 - v010 - v001 + v000)*dy*dz +
					(v101 - v100 - v001 + v000)*dx*dz +
					(v111 + v100 + v010 + v001 - v110 - v101 - v011 - v000)*dx*dy*dz + v000 );
		}

		public synchronized void slice_trilinear(){
			
			int[][] iS = cube.iS;
			
			cube.findIntersections(tr, dist);
			
			int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
			for (int i = 0;  i < 6; i++) {
				
				tr.xyzPos(iS[i]);
				
				int xi = tr.X;
				if (xi < xMin) xMin = xi;
				if (xi > xMax) xMax = xi;
				int yi = tr.Y;
				if (yi < yMin) yMin = yi;
				if (yi > yMax) yMax = yi;
			}
			
			xMin = (xMin < 0)   ?   0 : xMin;
			xMax = (xMax > 511) ? 511 : xMax;
			yMin = (yMin < 0)   ?   0 : yMin;
			yMax = (yMax > 511) ? 511 : yMax;
			
			int[] v = new int[3];
			
			if (!isRGB) {
				// draw slice at dist
				v[2] = dist; // z

				float widthX  = vol.widthX  - 0.5f;
				float heightY = vol.heightY - 0.5f;
				float depthZ  = vol.depthZ  - 0.5f;

				for (int y = yMin; y <= yMax; y++) {
					v[1] = y;
					for (int x = xMin; x <= xMax; x++) {
						int pos = y*512 + x;
						v[0] = x;
						tr.invxyzPosf(v);

						float x_  = tr.xf; 
						float y_  = tr.yf; 
						float z_  = tr.zf; 


						if ((x_ >=0  &&  x_ < widthX ) 
								&& (y_ >=0  &&  y_ < heightY ) 
								&& (z_ >=0  &&  z_ < depthZ  ) ) { 
							int val = trilinear(data3D[0], z_, y_, x_);

							pixels[pos] = lut.colors[val];
						}
					}
				}	
			}
			else {
				for (int ch=0; ch<3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					int shift = (2-ch)*8;
					
					// draw slice at dist
					v[2] = dist; // z

					float widthX  = vol.widthX  - 0.5f;
					float heightY = vol.heightY - 0.5f;
					float depthZ  = vol.depthZ  - 0.5f;

					for (int y = yMin; y <= yMax; y++) {
						v[1] = y;
						for (int x = xMin; x <= xMax; x++) {
							int pos = y*512 + x;
							v[0] = x;
							tr.invxyzPosf(v);

							float x_  = tr.xf; 
							float y_  = tr.yf; 
							float z_  = tr.zf; 


							if ((x_ >=0  &&  x_ < widthX ) 
									&& (y_ >=0  &&  y_ < heightY ) 
									&& (z_ >=0  &&  z_ < depthZ  ) ) { 
								int val = trilinear(data3D[ch], z_, y_, x_);

								pixels[pos] |= 0xFF000000 | (val<<shift);
							}
						}
					}	
				}
			}
		}

		public synchronized void projection_trilinear_front(){
			float width  = vol.widthX  - 0.5f;
			float height = vol.heightY - 0.5f;
			float depth  = vol.depthZ  - 0.5f;
			
			int zMax = -1000, zMin = 1000;
			int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
			
			for (int i = 0;  i < 8; i++) {
				tr.xyzPos(cube.corners[i]);
				int xi = tr.X;
				if (xi < xMin) xMin = xi;
				if (xi > xMax) xMax = xi;
				int yi = tr.Y;
				if (yi < yMin) yMin = yi;
				if (yi > yMax) yMax = yi;
				int zi = tr.Z; 
				if (zi > zMax) zMax = zi;
				if (zi < zMin) zMin = zi;
			}
			
			xMin = (xMin < 0)   ?   0 : xMin;
			xMax = (xMax > 511) ? 511 : xMax;
			yMin = (yMin < 0)   ?   0 : yMin;
			yMax = (yMax > 511) ? 511 : yMax;
			
			int[] v1 = new int[3];
			int[] v2 = new int[3];
			
			if (dist < zMin)
				dist = zMin;
			
			int nd = zMax - dist;
			
			if (isRGB) {

				for (int ch=0; ch<3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					int shift = (2-ch)*8;
					
					float dx=0;
					float dy=0;
					float dz=0;


					if (nd > 0) {

						float scaleLum = (renderDepth > 1) ? (renderDepth*255/((renderDepth-1)*255f)) : 2;;
						v1[2] = dist; 
						v2[2] = zMax; 

						float nd1 = 1f/nd;
						for (int y = yMin; y <= yMax; y++) {
							v1[1] = v2[1] = y;

							int percent = 100*(y-yMin)/ (yMax-yMin);
							//IJ.showProgress(percent, 100);

							IJ.showStatus("Rendering : " + percent +"%" );

							for (int x = xMin; x <= xMax; x++) {
								if (cube.isInside(x, y)) {

									int pos = (y<<9) + x;
									v1[0] = v2[0] = x;

									int alpha = 255;
									int V = 0;
									tr.invxyzPosf(v1);
									float x1 = tr.xf;
									float y1 = tr.yf;
									float z1 = tr.zf;

									tr.invxyzPosf(v2);

									dx = (tr.xf-x1)*nd1;
									dy = (tr.yf-y1)*nd1;
									dz = (tr.zf-z1)*nd1;

									int k = 0;

									for (int n = nd; n >= 0; n-- ) {
										if (x1 >= 0 && x1 < width &&  
												y1 >= 0 && y1 < height && 
												z1 >= 0 && z1 < depth) { 
											int val = trilinear(data3D[ch], z1, y1, x1);	

											if (val >= thresh) {	  
												int f = (val - thresh) + 50;
												if (f > 255)
													f = 255;

												alpha +=f;
												V += f*val;

												if (++k >= renderDepth)
													break;
											}
										}

										x1 += dx;
										y1 += dy;
										z1 += dz;
									}
									int val = (int) (scaleLum * V / alpha);
									if (val > 255)
										val = 255;
									pixels[pos] |= 0xFF000000 | (val << shift);
								}	
							}	
						}		
					}
				}
			}
			else {

				float dx=0;
				float dy=0;
				float dz=0;


				if (nd > 0) {

					float scaleLum = (renderDepth > 1) ? (renderDepth*255/((renderDepth-1)*255f)) : 2;;
					v1[2] = dist; 
					v2[2] = zMax; 

					float nd1 = 1f/nd;
					for (int y = yMin; y <= yMax; y++) {
						v1[1] = v2[1] = y;

						int percent = 100*(y-yMin)/ (yMax-yMin);
						//IJ.showProgress(percent, 100);

						IJ.showStatus("Rendering : " + percent +"%" );

						for (int x = xMin; x <= xMax; x++) {
							if (cube.isInside(x, y)) {

								int pos = (y<<9) + x;
								v1[0] = v2[0] = x;

								int alpha = 255;
								int V = 0;
								tr.invxyzPosf(v1);
								float x1 = tr.xf;
								float y1 = tr.yf;
								float z1 = tr.zf;

								tr.invxyzPosf(v2);

								dx = (tr.xf-x1)*nd1;
								dy = (tr.yf-y1)*nd1;
								dz = (tr.zf-z1)*nd1;

								int k = 0;

								for (int n = nd; n >= 0; n-- ) {
									if (x1 >= 0 && x1 < width &&  
											y1 >= 0 && y1 < height && 
											z1 >= 0 && z1 < depth) { 
										int val = trilinear(data3D[0], z1, y1, x1);	

										if (val >= thresh) {	  
											int f = (val - thresh) + 50;
											if (f > 255)
												f = 255;

											alpha +=f;
											V += f*val;

											if (++k >= renderDepth)
												break;
										}
									}

									x1 += dx;
									y1 += dy;
									z1 += dz;
								}
								int val = (int) (scaleLum * V / alpha);
								if (val > 255)
									val = 255;
								pixels[pos] = lut.colors[val];
							}	
						}	
					}		
				}
			}
			IJ.showStatus("");			
		}


		public synchronized void projection_trilinear_back(){
			float width  = vol.widthX  - 0.5f;
			float height = vol.heightY - 0.5f;
			float depth  = vol.depthZ  - 0.5f;
			
			int zMax = -1000, zMin = 1000;
			int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
			
			for (int i = 0;  i < 8; i++) {
				tr.xyzPos(cube.corners[i]);
				int xi = tr.X;
				if (xi < xMin) xMin = xi;
				if (xi > xMax) xMax = xi;
				int yi = tr.Y;
				if (yi < yMin) yMin = yi;
				if (yi > yMax) yMax = yi;
				int zi = tr.Z; 
				if (zi > zMax) zMax = zi;
				if (zi < zMin) zMin = zi;
			}
			
			xMin = (xMin < 0)   ?   0 : xMin;
			xMax = (xMax > 511) ? 511 : xMax;
			yMin = (yMin < 0)   ?   0 : yMin;
			yMax = (yMax > 511) ? 511 : yMax;
			
			int[] v1 = new int[3];
			int[] v2 = new int[3];
			
			if (dist < zMin)
				dist = zMin;
			
			int nd = zMax - dist;
			
			if (isRGB) {
				for (int ch = 0; ch < 3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					int shift = (2-ch)*8;
					
					float dx=0;
					float dy=0;
					float dz=0;

					int vals[] = new int [zMax-dist]; 

					if (nd > 0) {
						v1[2] = dist; 
						v2[2] = zMax; 

						float nd1 = 1f/nd;
						for (int y = yMin; y <= yMax; y++) {
							v1[1] = v2[1] = y;

							int percent = 100*(y-yMin)/ (yMax-yMin);
							
							IJ.showStatus("Rendering : " + percent +"%" );

							for (int x = xMin; x <= xMax; x++) {
								//if (cube.isInside(x, y)) {
								if (cube.isInside(x, y)) {

									int pos = (y<<9) + x;
									v1[0] = v2[0] = x;

									int V = 0;
									tr.invxyzPosf(v1);
									float x1 = tr.xf;
									float y1 = tr.yf;
									float z1 = tr.zf;

									tr.invxyzPosf(v2);

									dx = (tr.xf-x1)*nd1;
									dy = (tr.yf-y1)*nd1;
									dz = (tr.zf-z1)*nd1;

									int k = 0;
									for (int n = nd; n >= 0; n-- ) {
										if (x1 >= 0 && x1 < width &&  
												y1 >= 0 && y1 < height && 
												z1 >= 0 && z1 < depth) { 
											int val = trilinear(data3D[ch], z1, y1, x1);	

											if (val >= thresh) {
												vals[k++] = val;

												if (k > renderDepth)
													break;
											}
										}

										x1 += dx;
										y1 += dy;
										z1 += dz;
									}
									for (int i = k-1; i>= 0; i--) {
										int a = (vals[i] - thresh) + 50;

										if (a > 255)
											a = 255;

										V = (a*vals[i] + V*(255-a))/255;
									}

									int val = V;
									pixels[pos] |= 0xFF000000 | (val<<shift);

								}			
							}	
						}		
					}
				}
			}
			else {

				float dx=0;
				float dy=0;
				float dz=0;

				int vals[] = new int [zMax-dist]; 

				if (nd > 0) {
					v1[2] = dist; 
					v2[2] = zMax; 

					float nd1 = 1f/nd;
					for (int y = yMin; y <= yMax; y++) {
						v1[1] = v2[1] = y;

						int percent = 100*(y-yMin)/ (yMax-yMin);
						//IJ.showProgress(percent, 100);

						IJ.showStatus("Rendering : " + percent +"%" );

						for (int x = xMin; x <= xMax; x++) {
							//if (cube.isInside(x, y)) {
							if (cube.isInside(x, y)) {

								int pos = (y<<9) + x;
								v1[0] = v2[0] = x;

								int V = 0;
								tr.invxyzPosf(v1);
								float x1 = tr.xf;
								float y1 = tr.yf;
								float z1 = tr.zf;

								tr.invxyzPosf(v2);

								dx = (tr.xf-x1)*nd1;
								dy = (tr.yf-y1)*nd1;
								dz = (tr.zf-z1)*nd1;

								int k = 0;
								for (int n = nd; n >= 0; n-- ) {
									if (x1 >= 0 && x1 < width &&  
											y1 >= 0 && y1 < height && 
											z1 >= 0 && z1 < depth) { 
										int val = trilinear(data3D[0], z1, y1, x1);	

										if (val >= thresh) {
											vals[k++] = val;

											if (k > renderDepth)
												break;
										}
									}

									x1 += dx;
									y1 += dy;
									z1 += dz;
								}
								for (int i = k-1; i>= 0; i--) {
									int a = (vals[i] - thresh) + 50;

									if (a > 255)
										a = 255;

									V = (a*vals[i] + V*(255-a))/255;
								}

								int val = V;
								pixels[pos] = lut.colors[val];

							}			
						}	
					}		
				}
			}
			IJ.showStatus("");			
		}


		public synchronized void slice_and_volume_dots(){
			
			int[] v = new int[3];
			
			byte[][] datay;
			byte[]   datax;
			
			cube.findIntersections(tr, dist);
			
			int[][] iS = cube.iS;
			
			int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
			for (int i = 0;  i < 6; i++) {
				v[0] = iS[i][0];
				v[1] = iS[i][1];
				v[2] = iS[i][2];
				tr.xyzPos(v);
				
				int xi = tr.X;
				if (xi < xMin)
					xMin = xi;
				if (xi > xMax)
					xMax = xi;
				int yi = tr.Y;
				if (yi < yMin)
					yMin = yi;
				if (yi > yMax)
					yMax = yi;
			}
			
			
			xMin = (xMin < 0)   ?   0 : xMin;
			xMax = (xMax > 511) ? 511 : xMax;
			yMin = (yMin < 0)   ?   0 : yMin;
			yMax = (yMax > 511) ? 511 : yMax;
			
			
			if (isRGB) {
				for (int ch = 0; ch < 3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					
					for (int i = pixels.length-1; i >= 0; i--)  {
						pixelsZ[ch][i] = 1000;
					}
					
					int shift = (2-ch)*8;
					v[2] = dist; // z

					int width = vol.widthX-1;
					int height = vol.heightY-1;
					int depth = vol.depthZ-1;

					for (int y = yMin; y < yMax; y++) {
						v[1] = y;

						for (int x = xMin; x < xMax; x++) {
							int pos = y*512 + x;

							v[0] = x;

							tr.invxyzPosf(v);

							float x_  = tr.xf;
							if (x_ >= 0 && x_ < width) {

								float y_  = tr.yf; 
								if ( y_ >= 0 && y_ < height ) {

									float  z_  = tr.zf; 
									if ( z_ >= 0 && z_ < depth ) {

										int val = trilinear(data3D[ch], z_, y_, x_);

										if (val >= thresh) {
											pixels[pos] |= 0xFF000000 | (val<<shift);
											pixelsZ[ch][pos] = -1000;
										}
									}
								}	
							}
						}
					}

					int widthS = vol.widthS;

					float w1 = 1/(widthS-1f);

					for (int z=0; z < data3DS[ch].length ; z++){
						v[2] = z*vol.dS;
						datay = data3DS[ch][z];
						for (int y=0; y < datay.length ; y++){
							v[1] = y*vol.hS;
							datax = datay[y]; 

							v[0] = 0 ;
							tr.xyzPos(v);
							float x1 = tr.X;
							float y1 = tr.Y;
							float z1 = tr.Z;

							v[0] = datax.length-1;
							tr.xyzPos(v);
							float x2 = tr.X;
							float y2 = tr.Y;
							float z2 = tr.Z;

							float dx = (x2-x1)*w1;
							float dy = (y2-y1)*w1;
							float dz = (z2-z1)*w1;

							for (int x = 0; x < widthS ; x++){
								int val = 0xff & datax[x];

								if (val >= thresh) {

									int z_ = (int)(z1);
									if (z_ > dist) {
										int x_ = (int)(x1);
										int y_ = (int)(y1);

										if ((x_ & ~511) == 0 && (y_ & ~511) == 0) { // only for w = h = 512 
											int pos = (y_<<9) | x_;  // only for w = 512 !!!

											if (z_ < pixelsZ[ch][pos]) {
												pixelsZ[ch][pos] = z_;
												pixels[pos] |= (0xFF << 24) | (val << shift); 
											}
										}
									}
								}
								z1 += dz*vol.wS;
								y1 += dy*vol.wS;
								x1 += dx*vol.wS;
							}
						}
					}
				}
			}
			else {
				for (int i = pixels.length-1; i >= 0; i--)  {
					pixelsZ[0][i] = 1000;
				}
				v[2] = dist; // z

				int width = vol.widthX-1;
				int height = vol.heightY-1;
				int depth = vol.depthZ-1;

				for (int y = yMin; y < yMax; y++) {
					v[1] = y;

					for (int x = xMin; x < xMax; x++) {
						int pos = y*512 + x;

						v[0] = x;

						tr.invxyzPosf(v);

						float x_  = tr.xf;
						if (x_ >= 0 && x_ < width) {

							float y_  = tr.yf; 
							if ( y_ >= 0 && y_ < height ) {

								float  z_  = tr.zf; 
								if ( z_ >= 0 && z_ < depth ) {

									int val = trilinear(data3D[0], z_, y_, x_);

									if (val >= thresh) {
										pixels[pos] = lut.colors[val];
										pixelsZ[0][pos] = -1000;
									}
								}
							}	
						}
					}
				}

				int widthS = vol.widthS;

				float w1 = 1/(widthS-1f);

				for (int z=0; z < data3DS[0].length ; z++){
					v[2] = z*vol.dS;
					datay = data3DS[0][z];
					for (int y=0; y < datay.length ; y++){
						v[1] = y*vol.hS;
						datax = datay[y]; 

						v[0] = 0 ;
						tr.xyzPos(v);
						float x1 = tr.X;
						float y1 = tr.Y;
						float z1 = tr.Z;

						v[0] = datax.length-1;
						tr.xyzPos(v);
						float x2 = tr.X;
						float y2 = tr.Y;
						float z2 = tr.Z;

						float dx = (x2-x1)*w1;
						float dy = (y2-y1)*w1;
						float dz = (z2-z1)*w1;

						for (int x = 0; x < widthS ; x++){
							int val = 0xff & datax[x];

							if (val >= thresh) {

								int z_ = (int)(z1);
								if (z_ > dist) {
									int x_ = (int)(x1);
									int y_ = (int)(y1);

									if ((x_ & ~511) == 0 && (y_ & ~511) == 0) { // only for w = h = 512 
										int pos = (y_<<9) | x_;  // only for w = 512 !!!

										if (z_ < pixelsZ[0][pos]) {
											pixelsZ[0][pos] = z_;
											pixels[pos] = (0xFF << 24) | (val << 8); 
										}
									}
								}
							}
							z1 += dz*vol.wS;
							y1 += dy*vol.wS;
							x1 += dx*vol.wS;
						}
					}
				}
			}
		}

		public synchronized void slice_and_borders(){
			
			cube.findIntersections(tr, dist);
			
			int[][] iS = cube.iS;
			
			int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
			for (int i = 0;  i < 6; i++) {
				tr.xyzPos(iS[i]);
				
				int xi = tr.X;
				if (xi < xMin) xMin = xi;
				if (xi > xMax) xMax = xi;
				int yi = tr.Y;
				if (yi < yMin) yMin = yi;
				if (yi > yMax) yMax = yi;
			}
			xMin --;
			xMax ++;
			yMin --;
			yMax ++;
			
			xMin = (xMin < 0)   ?   0 : xMin;
			xMax = (xMax > 511) ? 511 : xMax;
			yMin = (yMin < 0)   ?   0 : yMin;
			yMax = (yMax > 511) ? 511 : yMax;
		
			int[] v = new int[3];
			
			
			if (isRGB) {

				for (int ch = 0; ch < 3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					int shift = (2-ch)*8;
					// draw slice at dist
					v[2] = dist; // z

					float width =  vol.widthX-1;
					float height = vol.heightY-1;
					float depth =  vol.depthZ-1;

					for (int y = yMin; y <= yMax; y++) {
						v[1] = y;
						for (int x = xMin; x <= xMax; x++) {
							int pos = y*512 + x;
							v[0] = x;
							tr.invxyzPosf(v);

							float x_  = tr.xf; 
							float y_  = tr.yf; 
							float z_  = tr.zf; 

							if ((x_ >=0  &&  x_ < width ) 
									&& (y_ >=0  &&  y_ < height ) 
									&& (z_ >=0  &&  z_ < depth  ) ) { 
								int val = trilinear(data3D[ch], z_, y_, x_);

								pixels[pos] |= 0xFF000000 | (val<< shift);
							}
						}
					}		

					int zMax = -1000, zMin = 1000;

					for (int i = 0;  i < 8; i++) {
						tr.xyzPos(cube.corners[i]);
						int xi = tr.X;
						if (xi < xMin) xMin = xi;
						if (xi > xMax) xMax = xi;
						int yi = tr.Y;
						if (yi < yMin) yMin = yi;
						if (yi > yMax) yMax = yi;
						int zi = tr.Z; 
						if (zi > zMax) zMax = zi;
						if (zi < zMin) zMin = zi;
					}
					xMin = (xMin < 0)   ?   0 : xMin;
					xMax = (xMax > 511) ? 511 : xMax;
					yMin = (yMin < 0)   ?   0 : yMin;
					yMax = (yMax > 511) ? 511 : yMax;

					for (int y = yMin; y <= yMax; y++) {
						
						int percent = 100*(y-yMin)/ (yMax-yMin);
						
						IJ.showStatus("Rendering : " + percent +"%" );
						for (int x = xMin; x <= xMax; x++) {
							int pos = (y<<9) + x;
							if (((pixels[pos] >> shift) & 0xFF) == 0) {
								boolean found = false;
								if (cube.isInside(x, y)) {
									v[1] = y;
									v[0] = x;
									v[2] = dist+1; // z

									tr.invxyzPosf(v);
									float x1 = tr.xf;
									float y1 = tr.yf;
									float z1 = tr.zf;

									v[2] = zMax; // z
									tr.invxyzPosf(v);
									float x2 = tr.xf;
									float y2 = tr.yf;
									float z2 = tr.zf;

									float nd = zMax - (dist+1);

									if (nd > 0) {
										float nd1 = 1/nd;
										float dx = (x2-x1)*nd1;
										float dy = (y2-y1)*nd1;
										float dz = (z2-z1)*nd1;

										for (; nd >= 0; nd-- ) {
											if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height &&  z1 >= 0 && z1 < depth) { 
												int val = trilinear(data3D[ch], z1, y1, x1);						
												pixels[pos] |= (0xFF << 24) | (val<< shift);
												found = true;
												break;
											}
											x1 += dx;
											y1 += dy;
											z1 += dz;
										}
									}
									if (!found) {
										v[2] = dist-1; // z

										tr.invxyzPosf(v);
										x1 = tr.xf;
										y1 = tr.yf;
										z1 = tr.zf;

										v[2] = zMin; // z
										tr.invxyzPosf(v);
										x2 = tr.xf;
										y2 = tr.yf;
										z2 = tr.zf;


										nd = (dist-1) - zMin;

										if (nd > 0) {
											float nd1 = 1/nd;
											float dx = (x2-x1)*nd1;
											float dy = (y2-y1)*nd1;
											float dz = (z2-z1)*nd1;

											for (; nd >= 0; nd-- ) {
												if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height &&  z1 >= 0 && z1 < depth) { 
													int val = trilinear(data3D[ch], z1, y1, x1);						
													pixels[pos] |= (0xFF << 24) | (val<< shift);
													break;
												}
												x1 += dx;
												y1 += dy;
												z1 += dz;
											}
										}
									}
								}
							}	
						}
					}
				}
			}
			else {
				// draw slice at dist
				v[2] = dist; // z

				float width =  vol.widthX-1;
				float height = vol.heightY-1;
				float depth =  vol.depthZ-1;

				for (int y = yMin; y <= yMax; y++) {
					v[1] = y;
					for (int x = xMin; x <= xMax; x++) {
						int pos = y*512 + x;
						v[0] = x;
						tr.invxyzPosf(v);

						float x_  = tr.xf; 
						float y_  = tr.yf; 
						float z_  = tr.zf; 

						if ((x_ >=0  &&  x_ < width ) 
								&& (y_ >=0  &&  y_ < height ) 
								&& (z_ >=0  &&  z_ < depth  ) ) { 
							int val = trilinear(data3D[0], z_, y_, x_);

							pixels[pos] = lut.colors[val];
						}
					}
				}		

				int zMax = -1000, zMin = 1000;

				for (int i = 0;  i < 8; i++) {
					tr.xyzPos(cube.corners[i]);
					int xi = tr.X;
					if (xi < xMin) xMin = xi;
					if (xi > xMax) xMax = xi;
					int yi = tr.Y;
					if (yi < yMin) yMin = yi;
					if (yi > yMax) yMax = yi;
					int zi = tr.Z; 
					if (zi > zMax) zMax = zi;
					if (zi < zMin) zMin = zi;
				}
				xMin = (xMin < 0)   ?   0 : xMin;
				xMax = (xMax > 511) ? 511 : xMax;
				yMin = (yMin < 0)   ?   0 : yMin;
				yMax = (yMax > 511) ? 511 : yMax;

				for (int y = yMin; y <= yMax; y++) {
					int percent = 100*(y-yMin)/ (yMax-yMin);
					
					IJ.showStatus("Rendering : " + percent +"%" );
					for (int x = xMin; x <= xMax; x++) {
						int pos = (y<<9) + x;
						if (pixels[pos] == 0) {
							boolean found = false;
							if (cube.isInside(x, y)) {
								v[1] = y;
								v[0] = x;
								v[2] = dist+1; // z

								tr.invxyzPosf(v);
								float x1 = tr.xf;
								float y1 = tr.yf;
								float z1 = tr.zf;

								v[2] = zMax; // z
								tr.invxyzPosf(v);
								float x2 = tr.xf;
								float y2 = tr.yf;
								float z2 = tr.zf;

								float nd = zMax - (dist+1);

								if (nd > 0) {
									float nd1 = 1/nd;
									float dx = (x2-x1)*nd1;
									float dy = (y2-y1)*nd1;
									float dz = (z2-z1)*nd1;

									for (; nd >= 0; nd-- ) {
										if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height &&  z1 >= 0 && z1 < depth) { 
											int val = ((trilinear(data3D[0], z1, y1, x1)* 3) >> 2) + 63;						
											pixels[pos] = (0xFF << 24) | (val << 8);
											found = true;
											break;
										}
										x1 += dx;
										y1 += dy;
										z1 += dz;
									}
								}
								if (!found) {
									v[2] = dist-1; // z

									tr.invxyzPosf(v);
									x1 = tr.xf;
									y1 = tr.yf;
									z1 = tr.zf;

									v[2] = zMin; // z
									tr.invxyzPosf(v);
									x2 = tr.xf;
									y2 = tr.yf;
									z2 = tr.zf;


									nd = (dist-1) - zMin;

									if (nd > 0) {
										float nd1 = 1/nd;
										float dx = (x2-x1)*nd1;
										float dy = (y2-y1)*nd1;
										float dz = (z2-z1)*nd1;

										for (; nd >= 0; nd-- ) {
											if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height &&  z1 >= 0 && z1 < depth) { 
												int val = ((trilinear(data3D[0], z1, y1, x1)* 3) >> 2) + 63;						
												pixels[pos] = (0xAF << 24) | (val );
												break;
											}
											x1 += dx;
											y1 += dy;
											z1 += dz;
										}
									}
								}
							}
						}	
					}
				}
			}	
			IJ.showStatus("");	
		}
		
		public synchronized void slice_NN(){
			int[] v = new int[3];
			
			cube.findIntersections(tr, dist);
			
			int[][] iS = cube.iS;
			
			if (isRGB) {

				for (int ch=0; ch<3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					int shift = (2-ch)*8;
					int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
					for (int i = 0;  i < 6; i++) {
						v[0] = iS[i][0];
						v[1] = iS[i][1];
						v[2] = iS[i][2];
						tr.xyzPos(v);

						int xi = tr.X;
						if (xi < xMin)
							xMin = xi;
						if (xi > xMax)
							xMax = xi;
						int yi = tr.Y;
						if (yi < yMin)
							yMin = yi;
						if (yi > yMax)
							yMax = yi;
					}

					v[2] = dist; // z
					xMin = (xMin < 0)   ?   0 : xMin;
					xMax = (xMax > 511) ? 511 : xMax;
					yMin = (yMin < 0)   ?   0 : yMin;
					yMax = (yMax > 511) ? 511 : yMax;

					for (int x = xMin; x < xMax; x++) {
						v[0] = x;
						for (int y = yMin; y < yMax; y++) {
							v[1] = y;

							tr.invxyzPosf(v);

							int x_  = (int) (tr.xf + 0.5f); 
							int y_  = (int) (tr.yf + 0.5f); 
							int z_  = (int) (tr.zf + 0.5f); 

							if (x_ >= 0 && x_ < vol.widthX &&
									y_ >= 0 && y_ < vol.heightY &&
									z_ >= 0 && z_ < vol.depthZ ) {

								int val = 0xff & data3D[ch][z_][y_][x_];						

								pixels[y*512 + x] |= 0xFF000000 | (val << shift);	
							}	
						}
					}
				}
			}
			else {

				int xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;
				for (int i = 0;  i < 6; i++) {
					v[0] = iS[i][0];
					v[1] = iS[i][1];
					v[2] = iS[i][2];
					tr.xyzPos(v);

					int xi = tr.X;
					if (xi < xMin)
						xMin = xi;
					if (xi > xMax)
						xMax = xi;
					int yi = tr.Y;
					if (yi < yMin)
						yMin = yi;
					if (yi > yMax)
						yMax = yi;
				}

				v[2] = dist; // z
				xMin = (xMin < 0)   ?   0 : xMin;
				xMax = (xMax > 511) ? 511 : xMax;
				yMin = (yMin < 0)   ?   0 : yMin;
				yMax = (yMax > 511) ? 511 : yMax;

				for (int x = xMin; x < xMax; x++) {
					v[0] = x;
					for (int y = yMin; y < yMax; y++) {
						v[1] = y;

						tr.invxyzPosf(v);

						int x_  = (int) (tr.xf + 0.5f); 
						int y_  = (int) (tr.yf + 0.5f); 
						int z_  = (int) (tr.zf + 0.5f); 

						if (x_ >= 0 && x_ < vol.widthX &&
								y_ >= 0 && y_ < vol.heightY &&
								z_ >= 0 && z_ < vol.depthZ ) {

							int val = 0xff & data3D[0][z_][y_][x_];						

							pixels[y*512 + x] = lut.colors[val];	
						}	
					}
				}
			}
		}

		public synchronized void volume_dots(){
			
			if (!isRGB) {
				for (int i = pixels.length-1; i >= 0; i--)  {
					pixelsZ[0][i] = 1000;
				}
				int[] v = new int[3];

				byte[][] datay;
				byte[]   datax;

				int widthS = vol.widthS;

				float w1 = vol.wS/(widthS-1f);

				for (int z=0; z < data3DS[0].length ; z++){
					v[2] = z*vol.dS;
					datay = data3DS[0][z];
					for (int y=0; y < datay.length ; y++){
						v[1] = y*vol.hS;
						datax = datay[y]; 

						v[0] = 0 ;
						tr.xyzPos(v);
						float x1 = tr.X;
						float y1 = tr.Y;
						float z1 = tr.Z;

						v[0] = datax.length-1 ;
						tr.xyzPos(v);

						float dx = (tr.X-x1)*w1;
						float dy = (tr.Y-y1)*w1;
						float dz = (tr.Z-z1)*w1;

						for (int x = 0; x < widthS ; x++){
							int val = 0xff & datax[x];

							if (val >= thresh) {

								int z_ = (int)(z1);
								if (z_ > dist) {
									int x_ = (int)(x1);
									int y_ = (int)(y1);

									if ((x_ & ~511) == 0 && (y_ & ~511) == 0) { // only for w = h = 512 
										int pos = (y_<<9) | x_;  // only for w = 512 !!!

										if (z_ < pixelsZ[0][pos]) {
											pixelsZ[0][pos] = z_;
											pixels[pos] = val+1;  
										}
									}
								}
							}
							z1 += dz;
							y1 += dy;
							x1 += dx;
						}
					}
				}
				for (int i = pixels.length-1; i >= 0; i--)  {
					int val = pixels[i];
					if (val > 255)
						val = 255;
					if (val > 0)
						pixels[i] = lut.colors[val];
				}
			}
			else {
				for (int ch=0; ch< 3; ch++) {
					if (ch == 0 && !isRed)
						continue;
					if (ch == 1 && !isGreen)
						continue;
					if (ch == 2 && !isBlue)
						continue;
					
					for (int i = pixelsZ[ch].length-1; i >= 0; i--)  {
						pixelsZ[ch][i] = 1000;
					}
					int[] v = new int[3];

					byte[][] datay;
					byte[]   datax;

					int widthS = vol.widthS;

					float w1 = vol.wS/(widthS-1f);

					for (int z=0; z < data3DS[ch].length ; z++){
						v[2] = z*vol.dS;
						datay = data3DS[ch][z];
						for (int y=0; y < datay.length ; y++){
							v[1] = y*vol.hS;
							datax = datay[y]; 

							v[0] = 0 ;
							tr.xyzPos(v);
							float x1 = tr.X;
							float y1 = tr.Y;
							float z1 = tr.Z;

							v[0] = datax.length-1 ;
							tr.xyzPos(v);

							float dx = (tr.X-x1)*w1;
							float dy = (tr.Y-y1)*w1;
							float dz = (tr.Z-z1)*w1;

							for (int x = 0; x < widthS ; x++){
								int val = 0xff & datax[x];

								if (val >= thresh) {

									int z_ = (int)(z1);
									if (z_ > dist) {
										int x_ = (int)(x1);
										int y_ = (int)(y1);

										if ((x_ & ~511) == 0 && (y_ & ~511) == 0) { // only for w = h = 512 
											int pos = (y_<<9) | x_;  // only for w = 512 !!!

											if (z_ < pixelsZ[ch][pos]) {
												pixelsZ[ch][pos] = z_;
												pixels[pos] |= 0xFF000000 | Math.min(255,val+1) << ((2-ch)*8);  
											}
										}
									}
								}
								z1 += dz;
								y1 += dy;
								x1 += dx;
							}
						}
					}
				}
			}		
		}
	}
}	
