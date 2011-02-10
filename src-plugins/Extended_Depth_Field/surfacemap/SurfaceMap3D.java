//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio, Francois Aguet
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
//==============================================================================

package surfacemap;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
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
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SurfaceMap3D implements Runnable {

	private int plotNumber = 1;
	private final static int OPAQUE = 0xFF000000;

	private static final int DOTS = 0;
	private final static int LINES = 1;
	private final static int MESH = 2;
	private final static int FILLED = 3;
	private final static int SHADED = 4;

	private int displayMode = FILLED;

	private final static int ORIGINAL = 0;
	private final static int GRAY = 1;
	private final static int SPECTRUM = 2;
	private final static int FIRE = 3;
	private final static int THERMAL = 4;
	private final static int ORANGE = 5;
	private final static int BLACK = 6;

	private int lutNr = ORIGINAL;

	private boolean axes = true;
	private boolean inverse = false;

	private boolean drag = false;

	private boolean shift = false;
	private boolean pause = false;
	private boolean move = false;
	private boolean running = false;
	private boolean isTexture;

	private PlotVal[] plotList;

	private ImagePlus imp;
	private ImagePlus impTexture;

	/**
	 *
	 * @param imp
	 * @param impTexture
	 */
	public SurfaceMap3D(ImagePlus imp, ImagePlus impTexture){
		this.imp = imp;
		this.impTexture = impTexture;
	}

	/**
	 *
	 */
	public void run() {

		if (imp == null) {
			IJ.showMessage("Image required");
			return;
		}

		final CustomWindow  cw = new CustomWindow();

		cw.init(imp, impTexture);

		final Frame f = new Frame("Topology Viewer");

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

		cw.totalSize.height = CustomWindow.H + ins.bottom + ins.top + 65;
		cw.totalSize.width  = CustomWindow.WR + ins.left + ins.right;

//		f.setSize(cw.totalSize);
		f.pack();

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


	/**
	 *
	 * @author alex
	 *
	 */
	class CustomWindow extends JPanel implements
	MouseListener, MouseMotionListener, ChangeListener, ActionListener, ItemListener {

		private GridBagLayout gbl = new GridBagLayout();
		private GridBagConstraints gbc = new GridBagConstraints();

		protected  Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		protected  Cursor moveCursor = new Cursor(Cursor.MOVE_CURSOR);

		private Picture pic1, pic2;

		private JPanel imagePanel;

		private JCheckBox checkInverse, checkAxes;
		private JButton button;

		private JComboBox displayChoice, lutChoice;
//		private final String [] displayName = { "Dots", "Lines", "Mesh", "Filled"} ;
		private final String [] lutName = { "Original Colors", "Grayscale", "Spectrum LUT", "Fire LUT", "Thermal LUT", "Orange" } ;

		private JSlider slider1, slider2, slider3, slider4, slider5;
		private float sliderValue1, sliderValue2, sliderValue3;
		private int sliderValue4, sliderValue5Old, sliderValue5New;

		private int checkMove = 0;
		private int xStart, yStart, xAct, yAct;

		private int xdiff, ydiff;
		private float dx, dy;

		private ImageRegion imageRegion;

		private Dimension totalSize = new Dimension();
		private Dimension rightSize = new Dimension();

		//  do not change sizes !
		private final static int H = 512;
		private final static int WR = 512;

//		private TurnThread thread;


		/**
		 *
		 *
		 */
		void cleanup() {
			pic1.pixels = null;
			pic1.pixelsZ = null;
			pic1 = null;

			pic2.pixels = null;
			pic2.pixelsZ = null;
			pic2 = null;

			plotList = null;

			imagePanel = null;
			running = false;
//			thread = null;
		}


		/**
		 *
		 * @param imp
		 * @param impTexture
		 */
		void init(ImagePlus imp, ImagePlus impTexture) {

			setLayout(gbl);

			imagePanel = new JPanel();
			imagePanel.setLayout(gbl);

			pic1 = new Picture(imp, impTexture);
			imageRegion = new ImageRegion();
			imageRegion.addMouseMotionListener(this);
			imageRegion.addMouseListener(this);

			pic2 = new Picture(WR, H);
			imageRegion.setImage(pic2);
			pic2.setImageRegion(imageRegion);

			pic2.cube.initTextsAndDrawColors(imageRegion);
			pic2.setLut();
			pic2.newDisplayMode();

			addComponent(imagePanel, 0,0,1,1,0,imageRegion);
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.BOTH;
			gbl.setConstraints(imageRegion,gbc);

			addComponent(this,2,0,1,1,5,imagePanel);
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.BOTH;
			gbl.setConstraints(imagePanel,gbc);

//			imagePanel.add(imageRegion,BorderLayout.EAST);
//			add(imagePanel, BorderLayout.SOUTH);

			JPanel buttonPanel1 = new JPanel();
			buttonPanel1.setLayout(gbl);

//			displayChoice = new JComboBox(displayName);
//			displayChoice.setSelectedIndex(3);
//			displayChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
//			displayChoice.addActionListener(this);
//			addComponent(buttonPanel1,0,0,1,1,5,displayChoice);
//			buttonPanel1.add(displayChoice);

			lutChoice = new JComboBox(lutName);
			lutChoice.setSelectedIndex(0);
			lutChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
			lutChoice.addActionListener(this);
			addComponent(buttonPanel1,0,1,1,1,5,lutChoice);
//			buttonPanel1.add(lutChoice);

			JPanel miniPanel = new JPanel();
			miniPanel.setLayout(new GridLayout(1,2));

			checkInverse = new JCheckBox("Inv.");
			checkInverse.setSelected(false);
			checkInverse.setHorizontalAlignment(JCheckBox.CENTER);
			checkInverse.addItemListener (this);
			miniPanel.add(checkInverse);

			checkAxes = new JCheckBox("Axes");
			checkAxes.setSelected(true);
			checkAxes.setHorizontalAlignment(JCheckBox.CENTER);
			checkAxes.addItemListener(this);
			miniPanel.add(checkAxes);

			addComponent(buttonPanel1,0,2,1,1,5,miniPanel);
//			buttonPanel1.add(miniPanel);

			button = new JButton("Capture View");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					imageRegion.saveToImage();
				}
			});
			addComponent(buttonPanel1,0,3,1,1,5,button);
//			buttonPanel1.add(button);

			JPanel buttonPanel2 = new JPanel();
			buttonPanel2.setLayout(new GridLayout(1,4,5,0));
//			buttonPanel2.setLayout(gbl);

			Border empty = BorderFactory.createTitledBorder( BorderFactory.createEmptyBorder() );

			Dimension sliderDimension = new Dimension(95,40);

			slider1 = new JSlider(JSlider.HORIZONTAL, 50, 118, 84 );
			slider1.setBorder( new TitledBorder(empty,
					"Perspective", TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			slider1.setPreferredSize(sliderDimension);
			slider1.addChangeListener( this );
			slider1.addMouseListener(this);
			buttonPanel2.add(slider1);

			slider2 = new JSlider(JSlider.HORIZONTAL, 0, 30, 0 );
			slider2.setBorder( new TitledBorder(empty,
					"Scale", TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			slider2.addChangeListener( this );
			slider2.setPreferredSize(sliderDimension);
			slider2.addChangeListener( this );
			slider2.addMouseListener(this);
			buttonPanel2.add(slider2);



			slider3 = new JSlider(JSlider.HORIZONTAL, 0, 20, 0 );
			slider3.setBorder( new TitledBorder(empty,
					"Lighting", TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			slider3.setPreferredSize(sliderDimension);
			slider3.addChangeListener( this );
			slider3.addMouseListener(this);
			buttonPanel2.add(slider3);

			slider4 = new JSlider(JSlider.HORIZONTAL, 0, 10, 0 );
			slider4.setBorder( new TitledBorder(empty,
					"Smoothing", TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			slider4.setPreferredSize(sliderDimension);
			slider4.addChangeListener( this );
			slider4.addMouseListener(this);
			buttonPanel2.add(slider4);

			slider5 = new JSlider(JSlider.HORIZONTAL, 0, 10, 0 );
			slider5.setValue(0);
			slider5.setMajorTickSpacing(2);
			slider5.setMinorTickSpacing(1);
			slider5.setSnapToTicks(true);
			slider5.setPaintTicks(true);
			slider5.setBorder( new TitledBorder(empty,
					"z-step", TitledBorder.CENTER, TitledBorder.TOP,
					new Font("Sans", Font.PLAIN, 12)));
			slider5.setPreferredSize(sliderDimension);
			slider5.addChangeListener( this );
			slider5.addMouseListener( this );
			buttonPanel2.add(slider5);

			addComponent(this,0,0,1,1,5,buttonPanel1);
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbl.setConstraints(buttonPanel1,gbc);

			addComponent(this,1,0,1,1,5,buttonPanel2);
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbl.setConstraints(buttonPanel2,gbc);

			validate();

//			thread = new TurnThread(pic2, imageRegion);
//			thread.start();
			super.setCursor(defaultCursor);
		}


		/**
		 * Add a component in a panel in the northeast of the cell.
		 */
		protected void addComponent(JPanel pn, int row, int col, int width, int height, int space, Component comp) {
			gbc.gridx = col;
			gbc.gridy = row;
			gbc.gridwidth = width;
			gbc.gridheight = height;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(space, space, space, space);
			//gbc.weightx = IJ.isMacintosh()?90:100;
			gbl.setConstraints(comp, gbc);
			pn.add(comp);
		}


		/**
		 *
		 */
		public void stateChanged( ChangeEvent e ){
			JSlider slider = (JSlider)e.getSource();

			if(!slider.getValueIsAdjusting()){
				pause = true;

				if (slider == slider1) {
					sliderValue1 = 75 - slider1.getValue()/2.f;
					if ( sliderValue1 == 50)
						sliderValue1 = 100000;
					sliderValue1 = sliderValue1*sliderValue1;
					pic2.tr.setD(sliderValue1);
				}
				else if (slider == slider2) {
					sliderValue2 = slider2.getValue();
					float scale = (float) Math.pow(1.05,sliderValue2);

					pic2.tr.setScale(scale);
				}
				else if (slider == slider3) {
					sliderValue3 = slider3.getValue();
					float light = (float) (sliderValue3 / 20.);
					pic2.setLight(light);
				}
				else if (slider == slider4) {
					sliderValue4 = slider4.getValue();
					pic2.smoothingFilter(sliderValue4);
					pic2.normals();
				}
				else if (slider == slider5) {
					sliderValue5New = slider5.getValue();
					pic2.changeZStep(sliderValue5New);
					pic2.smoothingFilter(sliderValue4);
//					sliderValue5Old = sliderValue5New;
					pic2.normals();
				}

				pause = false;
				pic2.newDisplayMode();
				imageRegion.repaint();
				super.requestFocus();
			}
		}

		/**
		 *
		 */
		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox)e.getSource();
			if (cb == lutChoice) {
				String name = (String)cb.getSelectedItem();

				for (int i = 0; i < lutName.length; i++) {
					if (name.equals(lutName[i])) {
						lutNr = i;
						break;
					}
				}
				pic2.setLut();
			}

			else if (cb == displayChoice) {
//
//				String name = (String)cb.getSelectedItem();
//				for (int i = 0; i < displayName.length; i++)
//					if (name.equals(displayName[i])) { // all colors
//						displayMode = i;
//						break;
//					}
//				pic2.setLut();
			}

			pause = false;

			pic2.cube.initTextsAndDrawColors(imageRegion);
			pic2.newDisplayMode();
			imageRegion.repaint();
			super.requestFocus();

//			if (!thread.isAlive()) {
//				thread = new TurnThread(pic2, imageRegion);
//				thread.start();
//			}
		}

		/**
		 *
		 */
		public synchronized void itemStateChanged(ItemEvent e) {

			Object source = e.getSource();

			if (source == checkInverse) {
				if (checkInverse.isSelected () ) {
					inverse = true;
				}
				if (!checkInverse.isSelected () ) {
					inverse = false;
				}
			}
			if (source == checkAxes) {
				if (checkAxes.isSelected () ) {
					axes = true;
				}
				if (!checkAxes.isSelected () ) {
					axes = false;
				}
			}
			pic2.newDisplayMode();
			imageRegion.repaint();
			super.requestFocus();

//			if (!thread.isAlive()) {
//				thread = new TurnThread(pic2, imageRegion);
//				thread.start();
//			}
		}

		/**
		 *
		 */
		public void mouseClicked(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion) {
				move = false;
			}
		}

		/**
		 *
		 */
		public void mouseEntered(MouseEvent arg0) {
			Object source = arg0.getSource();
			if (source == imageRegion) {
				super.setCursor(moveCursor);
			}
		}

		/**
		 *
		 */
		public void mouseExited(MouseEvent arg0) {
			super.setCursor(defaultCursor);
		}

		/**
		 *
		 */
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
			else if (source == slider1 || source == slider2){
				drag = true;
			}
		}

		/**
		 *
		 */
		public void mouseReleased(MouseEvent arg0) {
			Object source = arg0.getSource();
			drag = false;
			if (source == imageRegion)
				checkMove = 5;
			pic2.newDisplayMode();
			imageRegion.repaint();
		}

		/**
		 *
		 */
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
						pic2.tr.setMouseMovement((float)xdiff, (float)ydiff);
					else
						pic2.tr.setMouseMovementOffset(xdiff, ydiff);
					xStart = xAct;
					yStart = yAct;
				}

				pic2.newDisplayMode();

				imageRegion.repaint();
			}

			super.requestFocus();

//			if (!thread.isAlive()) {
//				thread = new TurnThread(pic2, imageRegion);
//				thread.start();
//			}
		}

		/**
		 *
		 */
		public void mouseMoved(MouseEvent arg0) {
			Object source = arg0.getSource();

			if (source == imageRegion) {
//				if (!shift && checkMove > 0 && dx != 0 && dy != 0)
//				move = true;
//				else
				dx = dy = 0;
			}
		}


//		/**
//		 *
//		 *
//		 */
//		class TurnThread extends Thread {
//
//			private Picture pic;
//			private ImageRegion ir;
//
//			public TurnThread (Picture picture, ImageRegion imageRegion) {
//				pic = picture;
//				ir = imageRegion;
//			}
//
//			public void run() {
//
//				this.setPriority(Thread.MIN_PRIORITY);
//				long tna = 0, tn = 0;
//				long tm = System.currentTimeMillis();
//				float fps = 0;
//				int delay;
//
//				try {
//					while (running) {
//
//						if (move && !pause) {
//							delay = 25;
//
//							pic.tr.setMouseMovement(dx, dy);
//							pic.newDisplayMode();
//
//							long dt = (tna == 0 || tn == 0) ? 0 : (tn - tna);
//							if (dt > 0) {
//								if (fps == 0)
//									fps = 10000.f/dt;
//								else
//									fps = (2*fps + 10000.f/dt)/3.f;
//
//								ir.setText((Misc.fm(4,((int)fps/10.))+ " fps"),5);
//							}
//							ir.repaint();
//							tna = tn;
//							tn = System.currentTimeMillis();
//						}
//						else {
//							delay = 200;
//							ir.setText("", 5);
//							ir.repaint();
//							fps = 0;
//							checkMove--;
//						}
//
//						tm += delay;
//						long sleepTime = Math.max(0, tm - System.currentTimeMillis());
//						sleep(sleepTime);
//					}
//				} catch (InterruptedException e){}
//			}
//		}

	}

	/**
	 *
	 * @author alex
	 *
	 */
	class Cube {

		private int corner[][];

		private Color backColor;
		private Color frontColor;

		private final  Color [] letterCol = {Color.black, Color.orange, Color.orange, Color.orange};

		private final static int dm = 18;
		private final static int dp = 10;

		private final  int [][] cornersRGB =  {
				{-128,-128,-128}, // VUL
				{-128, 127, 127}, // VOR
				{ 127,-128, 127}, // HUR
				{ 127, 127,-128}, // HOL
				{-128,-128, 127}, // VUR
				{-128, 127,-128}, // VOL
				{ 127,-128,-128}, // HUL
				{ 127, 127, 127}  // HOR
		};

		private final int [][] textPos = {
				{ -128-dm, -128-dm, -128-dm}, // 0
				{  127+dp, -128-dm, -128-dm}, // R
				{ -128-dm,  127+dp, -128-dm}, // G
				{ -128-dm, -128-dm,  127+dp}  // B
		};

		private final  String[] letters = {"0", "y", "x", "z"};

		/**
		 * Constructor.
		 *
		 */
		Cube () {
			corner = new int[8][4]; // 8 x X, Y, Z, order
		}

		/**
		 *
		 * @param imageRegion
		 */
		public void initTextsAndDrawColors(ImageRegion imageRegion) {
			imageRegion.newText(8);
			imageRegion.newLines(12);

			imageRegion.setPlaneColor( new Color(0xFF777777) );

			backColor  = new Color (0, 0, 0, 100);
			frontColor = Color.white;

			Color color;

			for (int i= 0; i < letters.length; i++) {
				color = letterCol[i];
				imageRegion.setText(letters[i], i, color);
			}
			imageRegion.setText("",  5, 10, 20, 1, Color.white );
		}

		/**
		 *
		 * @param imageRegion
		 * @param pic
		 * @param tr
		 */
		void setTextAndLines(ImageRegion imageRegion, Picture pic, Transform tr) {

			for (int i=0; i<textPos.length; i++) {
				tr.xyzPos(textPos[i]);
				imageRegion.setTextPos(i, tr.X, tr.Y, tr.Z);
			}

			int line= 0;

			for (int i=0; i<8; i++) {
				tr.xyzPos(cornersRGB[i]);
				corner[i][0] = tr.X;
				corner[i][1] = tr.Y;
				corner[i][2] = tr.Z;
				corner[i][3] = 0;
			}

			int[][] cor = new int[3][];

			for (int i = 0; i < 4; i++) {
				int k = 0;
				for (int j = 4; j < 8; j++) {
					if (i+j != 7)
						cor[k++] = corner[j];
				}
				if (corner[i][2] >= corner[7-i][2] &&
						Misc.inside(corner[i], cor[0], cor[1], cor[2]))
					corner[i][3] = 1;  // hidden
			}
			for (int j = 4; j < 8; j++) {
				int k = 0;
				for (int i = 0; i < 4; i++) {
					if (i+j != 7)
						cor[k++] = corner[i];
				}
				if (corner[j][2] >= corner[7-j][2]  &&
						Misc.inside(corner[j], cor[0], cor[1], cor[2]))
					corner[j][3] = 1; // hidden
			}


			for (int i = 0; i < 4; i++)
				for (int j = 4; j < 8; j++) {
					if (i+j != 7) {
						if (corner[i][3] == 1 || corner[j][3] == 1)  // hidden
							imageRegion.setLine(line, corner[i][0], corner[i][1], corner[j][0], corner[j][1], 1, backColor);
						else
							imageRegion.setLine(line, corner[i][0], corner[i][1], corner[j][0], corner[j][1], -1, frontColor);
						line++;
					}
				}

			cor = null;

		}

	}

	/**
	 *
	 * @author alex
	 *
	 */
	class ImageRegion extends JPanel {
		private Image image;
		private int width;
		private int height;

		private int xPos;
		private int yPos;

		private TextField[] textField = null;
		private Lines[] lines = null;

		private Color planeColor = Color.lightGray;

		private Font font1 = new Font("Sans", Font.PLAIN, 18);
		private Font font2 = new Font("Sans", Font.PLAIN, 15);


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

		synchronized void saveToImage() {

			pause = true;

			BufferedImage bufferedImage =  new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			paint(bufferedImage.createGraphics());

			String s = "SurfacePlot_"+plotNumber;

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
		public void paint(Graphics g) {

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


	/**
	 *
	 * @author alex
	 *
	 */
	private class Transform {

		private final void xyzPos() {
			float Y1 =  sinB*y  + cosB*x;
			float Y2 =  cosR*Y1 - sinR*z;
			float Z2 =  sinR*Y1 + cosR*z;

			float sz = (scale * d /(Z2 + d));

			Z = (int) Z2;
			X = (int)( (cosB*y - sinB*x)* sz + xs  + scale * xoff);
			Y = (int)( Y2* sz + ys + scale * yoff);
		}

		private final void xyzPos_() {
			float Y1 =  sinB*y  + cosB*x;
			float Y2 =  cosR*Y1 - sinR*z;
			float Z2 =  sinR*Y1 + cosR*z;

			Z = (int) Z2;
			X = (int)( (cosB*y - sinB*x));
			Y = (int)( Y2);
		}

		private final void xyzPos(int[] rgb) {
			y = rgb[0];
			x = rgb[1];
			z = rgb[2];
			xyzPos();
		}

		public void setScale(float scale) {
			this.scale = scale;
		}

		public void setMouseMovement(float dx, float dy) {
			angleB += dx/100.;
			angleR += dy/100.;

			cosB = (float)Math.cos(angleB);
			sinB = (float)Math.sin(angleB);
			cosR = (float)Math.cos(angleR);
			sinR = (float)Math.sin(angleR);
		}

		public void setMouseMovementOffset(int dx, int dy) {
			xoff += dx;
			yoff += dy;
		}

		Transform() {};

		Transform(int width, int height ) {
			xs = (float)(width/2.  + 0.5);
			ys = (float)(height/2. + 0.5);
		};

		private double angleB = -0.6125; // angle for B-rotation
		private double angleR = 2;       // angle for R-rotation


		private float d = 33*33;      // perspective

		private float cosB = (float)Math.cos(angleB), sinB = (float)Math.sin(angleB);
		private float cosR = (float)Math.cos(angleR), sinR = (float)Math.sin(angleR);

		private float scale = 1;

		private float xs;
		private float ys;
		private int xoff;
		private int yoff;

		private int y, x, z; // color coordinates
		private int X, Y, Z; // screen coordinates

		public void setD(float d) {
			this.d = d;
		}

		public void transform(PlotVal col) {
			y = col.y;
			x = col.x;
			z = (inverse) ? - col.z : col.z;
			xyzPos();
		}
	}

	/**
	 *
	 * @author alex
	 *
	 */
	private class Picture{

		private Image  image;    // AWT-Image

		private ImageRegion imageRegion = null;
		private int widthOrig;
		private int heightOrig;

		private Lut lut = new Lut();
		private Cube cube;
		private Transform tr;

		private int[] pixels = null;
		private int[] pixelsZ = null;		// Z-buffer

		private MemoryImageSource source;

		// the size of the grid
		private int width = 256, height = 256;
		//private int width = 64, height = 64;

		private float light;

		public Picture(ImagePlus imp, ImagePlus impTex){
			int[] pixelsOrig;

			if (impTex == null) {
				impTex = imp;
				isTexture = false;
			}
			else
				isTexture = true;

			int widthOrigTex = impTex.getWidth();
			int heightOrigTex = impTex.getHeight();

			widthOrig = imp.getWidth();
			heightOrig = imp.getHeight();

			float ratioX = 1;
			float ratioY = 1;

			if (widthOrig > heightOrig)
				ratioY = (float) heightOrig / widthOrig;
			else if (widthOrig < heightOrig)
				ratioX = (float) widthOrig / heightOrig;

			pixels = new int[width*height];

			byte[] lutPixels = null;

			plotList = new PlotVal[width*height];

			ImageProcessor ip = imp.getProcessor();

			pixelsOrig = new int[widthOrig*heightOrig];
			int[] pixelsTex = new int[widthOrigTex*heightOrigTex];

			image = imp.getImage();
			PixelGrabber pg =  new PixelGrabber(image, 0, 0, widthOrig, heightOrig, pixelsOrig, 0, widthOrig);

			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {IJ.error("error grabbing pixels");}

			Image imageTex = impTex.getImage();
			pg =  new PixelGrabber(imageTex, 0, 0, widthOrigTex, heightOrigTex, pixelsTex, 0, widthOrigTex);

			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {IJ.error("error grabbing pixels");}

			boolean isLut = ip.isColorLut();

			if( isLut ) {
				lutPixels = (byte []) ip.getPixels();
			}

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

				Rectangle rect = roi.getBoundingRect();
				if (rect.x < 0)
					rect.x = 0;
				if (rect.y < 0)
					rect.y = 0;


				for (int y=0; y<heightOrig; y++) {
					int offset = y*widthOrig;
					for (int x=0; x< widthOrig ; x++) {
						int i = offset + x;

						if (x < rect.x || x >= rect.x+rect.width || y < rect.y || y >= rect.y+rect.height) {
							pixelsOrig[i] = (63 << 24) |  (pixelsOrig[i] & 0xFFFFFF);
						}
						else if (mask != null) {
							int mx = x-rect.x;
							int my = y-rect.y;

							if (mpixels[my *mWidth + mx ] == 0) {
								pixelsOrig[i] = (63 << 24) |  (pixelsOrig[i] & 0xFFFFFF);
							}
						}
					}
				}

				float sx = rect.width / (float) width;
				float sy = rect.height / (float) height;
				float sxT = widthOrigTex / (float) widthOrig;
				float syT = heightOrigTex / (float) heightOrig;

				for (int y = 0; y < height; y++) {
					int yB = (int) (y * sy);

					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						int xB = (int) (x * sx);

						int c = 0;
						int cTex = 0;
						try {
							c = pixels[pos] = pixelsOrig[(rect.y +yB) * widthOrig + rect.x + xB];
							cTex = pixelsTex[(int)((rect.y +yB)*syT) * widthOrigTex + (int)(sxT*(rect.x + xB))];

						} catch (RuntimeException e) {
							e.printStackTrace();
							IJ.log("pos = " +  pos + " " + rect.y +" " +yB +" " +  widthOrig + " " + rect.x +" " + xB);
						}
						if ( (c & OPAQUE) == OPAQUE ) { // opaque

							int lum;
							plotList[pos] = new PlotVal();
							int r = ((c >> 16) & 255);
							int g = ((c >>  8) & 255);
							int b = ((c      ) & 255);
							if (!isLut)
								lum = (int)(0.299*r  + 0.587*g + 0.114*b);
							else
								lum = (int)(0xFF & lutPixels[(rect.y +yB) * widthOrig + rect.x + xB]);
							plotList[pos].color = cTex;

							plotList[pos].y = (int) (ratioY*(y * 256/height - 128));
							plotList[pos].x = (int) (ratioX*(x * 256/width - 128));
							plotList[pos].z = lum - 128 ;
							plotList[pos].lum = lum;
							plotList[pos].lumf = lum;
							plotList[pos].lumt = lum;
						}
					}
				}
			}
			else {
				float sx = widthOrig / (float) width;
				float sy = heightOrig / (float) height;
				float sxT = widthOrigTex / (float) width;
				float syT = heightOrigTex / (float) height;


				for (int y = 0; y < height; y++) {
					int yB = (int) (y * sy);
					int yBT = (int) (y * syT);

					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						int xB = (int) (x * sx);
						int xBT = (int) (x * sxT);
						int c = pixels[pos] = pixelsOrig[yB * widthOrig + xB];
						int cTex = pixelsTex[yBT * widthOrigTex + xBT];

						if ( (c & OPAQUE) == OPAQUE ) { // opaque

							int lum;
							plotList[pos] = new PlotVal();
							int r = ((c >> 16) & 255);
							int g = ((c >>  8) & 255);
							int b = ((c      ) & 255);
							if (!isLut) {
								lum = (int)(0.299*r  + 0.587*g + 0.114*b);
							}
							else
								lum = (int)(0xFF & lutPixels[yB * widthOrig + xB]);
							plotList[pos].color = cTex;

							plotList[pos].y = (int) (ratioY*(y * 256/height - 128));
							plotList[pos].x = (int) (ratioX*(x * 256/width - 128));
							plotList[pos].z = lum - 128 ;
							plotList[pos].lum = lum;
							plotList[pos].lumf = lum;
							plotList[pos].lumt = lum;
						}
					}
				}
			}

			normals();

			source = new MemoryImageSource(width, height, pixels, 0, width);
			image = Toolkit.getDefaultToolkit().createImage(source);
		}


		private void normals() {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int i = y * width + x;

					if (plotList[i] != null) {
						int dx1 = 0;
						int dy1 = 0;
						int dz1 = 0;

						for (int y_ = -1; y_ <= 1; y_++)
							for (int x_ = -1; x_ < 1; x_++)
							{
								int yn = y + y_;
								int xn = x + x_;

								if (yn >= 0 && yn < height && xn >= 0 && xn < width-1) {
									dx1 += 1;
									int pos = yn*width+xn;
									if (plotList[pos+1] != null && plotList[pos] != null )
										dz1 += plotList[pos+1].z - plotList[pos].z;
								}
							}

						int dx2 = 0;
						int dy2 = 0;
						int dz2 = 0;

						for (int y_ = -1; y_ < 1; y_++)
							for (int x_ = -1; x_ <= 1; x_++)
							{
								int yn = y + y_;
								int xn = x + x_;

								if (yn >= 0 && yn < height-1 && xn >= 0 && xn < width) {
									dy2 += 1;
									int pos = yn*width+xn;
									if (plotList[pos+width] != null && plotList[pos] != null )
										dz2 += plotList[pos+width].z - plotList[pos].z;
								}
							}

						// outer product
						int dx =  10*(dy1*dz2 - dz1*dy2);
						int dy = -10*(dx1*dz2 - dz1*dx2);
						int dz =  10*(dx1*dy2 - dy1*dx2);

						int len = (int) Math.sqrt(dx*dx + dy*dy + dz*dz);

						plotList[i].dx = dx;
						plotList[i].dy = dy;
						plotList[i].dz = dz;
						plotList[i].len = (len > 0) ? len : 1;

					}
				}
			}
		}

		private void smoothingFilter(int rad) {
			int size = 2*rad+1;
			float []  k = getBlurKernel(2*rad+1);

			for (int y=0; y < height; y++) {
				for (int x=0; x < width; x++) {
					int i = y*width + x;

					if (plotList[i] != null) {

						float sum = 0;
						float n = 0;
						for (int dy = -rad, ky = 0; dy<=rad; dy++, ky+=size ){

							int y_ = Math.max(Math.min(y+dy,height-1),0);
							int offset = y_*width;

							for (int dx = -rad, kx = 0; dx<=rad; dx++, kx++ ) {
								int x_ = x+dx;
								if (x_ < 0) x_ = 0;
								if (x_ > width-1) x_ = width-1;
								//int x_ = Math.max(Math.min(x+dx,width-1),0);
								int j = offset + x_;
								if (plotList[j] != null) {
									float f = k[ky+kx];
									sum  += f* (plotList[j].lumt -128);
									n += f;
								}
							}
						}
//						plotList[i].lumf = (int) (sum / n);
//						plotList[i].z = plotList[i].lumf - 128;
						plotList[i].z = (int) (sum / n);
						plotList[i].lumf = plotList[i].z + 128;
					}
				}
			}
		}


		private float[] getBlurKernel(int size) {

			float[] values = new float[size*size];

			float [] k = makeKernel((size-1)/2);
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					values[y*size + x] = k[x]*k[y];
				}
			}
			return values;

		}
		private float[] makeKernel(double radius) {

			radius += 1;
			int size = (int)radius*2+1;
			float[] kernel = new float[size];

			double v;
			for (int i=0; i<size; i++)
				kernel[i] = (float)Math.exp(-0.5*(sqr((i-radius)/(radius*2)))/sqr(0.2));
			float[] kernel2 = new float[size-2];
			for (int i=0; i<size-2; i++)
				kernel2[i] = kernel[i+1];
			if (kernel2.length==1)
				kernel2[0] = 1f;

			return kernel2;

		}

		double sqr(double x) {return x*x;}

		public void setLight(float light) {
			this.light = light;
		}


		private void changeZStep(int n) {

			float f = 1f - (n/11f);

			for (int y=0; y < height; y++) {
				for (int x=0; x < width; x++) {
					int i = y*width + x;
					if (plotList[i] != null) {
						plotList[i].z = (int)((plotList[i].lum - 128)*f); //
						plotList[i].lumt = plotList[i].z + 128;
						plotList[i].lumf = plotList[i].z + 128;
					}
				}
			}
		}


//		public Picture(Image image, int width, int height){
//		this.image = image;
//		this.widthOrig  = width;    		// Breite bestimmen
//		this.heightOrig = height;  			// Hoehe bestimmen
//
//		pixels = new int[width*height];
//
//		PixelGrabber pg =  new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
//
//		try {
//		pg.grabPixels();
//		} catch (InterruptedException ex) {}
//
//		tr = new Transform(width, height);
//
//		}

		public Picture (int width, int height){
			this.widthOrig = width;
			this.heightOrig = height;

			cube = new Cube();

			pixels  = new int[width*height];
			pixelsZ  = new int[width*height];

			source = new MemoryImageSource(width, height, pixels, 0, width);

			image = Toolkit.getDefaultToolkit().createImage(source);

			tr = new Transform(width, height);
		}


		public void setImageRegion(ImageRegion imageRegion) {
			this.imageRegion = imageRegion;
		}

		int getWidth(){
			return widthOrig;
		}
		int getHeight(){
			return heightOrig;
		}
		Image getImage(){
			return image;
		}

		public void setLut() {

			switch (lutNr) {
			case ORIGINAL:
				break;
			case GRAY:
				lut.gray();
				break;
			case ORANGE:
				lut.orange();
				break;
			case BLACK:
				lut.black();
				break;
			case SPECTRUM:
				lut.spectrum();
				break;
			case FIRE:
				lut.fire();
				break;
			case THERMAL:
				lut.thermal();
				break;
			}
		}

		public void newDisplayMode(){

			cube.setTextAndLines(imageRegion, this, tr);

			if (drag)
				dotsNoLight();
			else if (displayMode == FILLED)
				filled();
			else if (displayMode == MESH)
				mesh();
			else if (displayMode == LINES)
				lines();
			else if (displayMode == DOTS )
				dots();
		}

		public synchronized void filled(){

			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0;
				pixelsZ[i] = 1000;
			}

			for (int row=0; row<height-1; row++){
				for (int col=0; col<width-1; col++){
					int i = row*width + col;

					PlotVal p0 = plotList[i];

					if (p0 != null) {
						PlotVal p1 = plotList[i+1];
						PlotVal p2 = plotList[i+width];
						PlotVal p3 = plotList[i+width+1];

						if ( (p1 != null) && (p2 != null) && (p3 != null)) {
							int c0=0, c1=0, c2=0, c3=0;

							tr.transform(p0);
							int x0 = tr.X, y0 = tr.Y, z0 = tr.Z;

							tr.transform(p1);
							int x1 = tr.X,   y1 = tr.Y, z1 = tr.Z;

							tr.transform(p2);
							int x2 = tr.X,   y2 = tr.Y, z2 = tr.Z;

							tr.transform(p3);
							int x3 = tr.X,   y3 = tr.Y, z3 = tr.Z;


							int l0=0, l1=0, l2=0, l3=0;
							int r, g, b;

							if (lutNr == ORIGINAL) {
								c0 = p0.color;
								c1 = p1.color;
								c2 = p2.color;
								c3 = p3.color;
							}

							else {
								l0 = p0.z + 128;
								l1 = p1.z + 128;
								l2 = p2.z + 128;
								l3 = p3.z + 128;
							}

							int xMin = Math.min(Math.min(x0,x1), Math.min(x2,x3));
							int xMax = Math.max(Math.max(x0,x1), Math.max(x2,x3));
							int yMin = Math.min(Math.min(y0,y1), Math.min(y2,y3));
							int yMax = Math.max(Math.max(y0,y1), Math.max(y2,y3));

							for (int y= yMin ; y < yMax ; y++ ) {
								for (int x= xMin ; x < xMax ; x++ ) {
									if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512

										if ( Misc.inside(x, y, x0, y0, x1, y1, x3, y3) ||
												Misc.inside(x, y, x0, y0, x3, y3, x2, y2))	{

											int d0 = (x - x0)*(x - x0)+ (y - y0)*(y - y0);
											int d1 = (x - x1)*(x - x1)+ (y - y1)*(y - y1);
											int d2 = (x - x2)*(x - x2)+ (y - y2)*(y - y2);
											int d3 = (x - x3)*(x - x3)+ (y - y3)*(y - y3);

											int sumDists = 0;
											int sum = 0;
											if (d0 > 0) {
												d0 = (int)Math.sqrt(d0);
												sumDists = d0;
												sum = d0*z3;
											}
											if (d1 > 0) {
												d1 = (int) Math.sqrt(d1);
												sumDists += d1;
												sum += d1*z2;
											}
											if (d2 > 0) {
												d2 = (int) Math.sqrt(d2);
												sumDists += d2;
												sum += d2*z1;
											}
											if (d3 > 0) {
												d3 = (int) Math.sqrt(d3);
												sumDists += d3;
												sum += d3*z0;
											}
											if (sumDists == 0)
												sumDists++;

											int z = sum/sumDists;

											int pos = (y<<9) | x;  // only for w = 512 !!!

											try {
												if (z < pixelsZ[pos]) {
													pixelsZ[pos] = z;
													r = g = b = 0;
													if (lutNr == ORIGINAL ) {
														if (d3 > 0) {
															r += d3*((c0 >> 16)& 0xff);
															g += d3*((c0 >> 8 )& 0xff);
															b += d3*( c0       & 0xff);
														}
														if (d2 > 0) {
															r += d2*((c1 >> 16)& 0xff);
															g += d2*((c1 >> 8 )& 0xff);
															b += d2*( c1       & 0xff);
														}
														if (d1 > 0) {
															r += d1*((c2 >> 16)& 0xff);
															g += d1*((c2 >> 8 )& 0xff);
															b += d1*( c2       & 0xff);
														}
														if (d0 > 0) {
															r += d0*((c3 >> 16)& 0xff);
															g += d0*((c3 >> 8 )& 0xff);
															b += d0*( c3       & 0xff);
														}

														r /= sumDists;
														g /= sumDists;
														b /= sumDists;
													}
													else {
														int l = (int) ((d0*l3 + d1*l2 + d2*l1 + d3*l0)/sumDists);
														int c = lut.colors[l];
														r = (c >> 16)& 0xff;
														g = (c >> 8 )& 0xff;
														b =  c       & 0xff;
													}

													if (light > 0) {
														tr.x   =  p0.dx;
														tr.y   =  p0.dy;
														tr.z   =  p0.dz;
														tr.xyzPos_();

														float l = light*(-tr.X/(float)p0.len) + 1;

														r = (int)Math.min(255,l*r);
														g = (int)Math.min(255,l*g);
														b = (int)Math.min(255,l*b);
													}
													pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
												}
											} catch (RuntimeException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}
									}
								}
							}
						}
					}
				}
			}

			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
		}


		public synchronized void mesh(){

			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0;
				pixelsZ[i] = 1000;
			}

			int c0, c1, c2;
			int l0 = 0, l1 = 0, l2 = 0;
			int r0 = 0, g0 = 0, b0 = 0, r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0;
			int r, g, b;

			for (int row=0; row<height-1; row++){
				for (int col=0; col<width-1; col++){
					int i = row*width + col;

					PlotVal p0 = plotList[i];

					if (p0 != null) {

						if (lutNr == ORIGINAL) {
							c0 = p0.color;
							r0 = (c0 >> 16)& 0xff;
							g0 = (c0 >> 8 )& 0xff;
							b0 =  c0       & 0xff;
						}
						else {
							c0 = getLutColor(p0);
							l0 = p0.z + 128;
						}

						tr.transform(p0);
						int x0 = tr.X, y0 = tr.Y, z0 = tr.Z;

						PlotVal p1 = plotList[i+1];
						PlotVal p2 = plotList[i+width];

						if ( p1 != null ) {
							tr.transform(p1);
							int x1 = tr.X,   y1 = tr.Y,   z1 = tr.Z;
							int dx10 = x1-x0, dy10 = y1-y0, dz10 = z1-z0;

							if (lutNr == ORIGINAL) {
								c1 = p1.color;
								r1 = (c1 >> 16)& 0xff;
								g1 = (c1 >> 8 )& 0xff;
								b1 =  c1       & 0xff;
							}
							else {
								l1 = p1.z + 128;
							}
							int numSteps = Math.max(Math.abs(dx10),Math.abs(dy10));

							float step = (numSteps > 0) ? 1 / (float)numSteps : 1;

							for (int s = 0; s < numSteps; s++) {
								float f = s * step;

								int x = (int) (x0 + f*dx10);
								int y = (int) (y0 + f*dy10);

								if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512
									int pos = (y<<9) | x;  // only for w = 512 !!!
									int z = (int) (z0 + f*dz10);
									if (z < pixelsZ[pos]) {
										pixelsZ[pos] = z;

										if (lutNr == ORIGINAL) {
											r = (int) (f*r1 + (1-f)*r0);
											g = (int) (f*g1 + (1-f)*g0);
											b = (int) (f*b1 + (1-f)*b0);
										}
										else {
											int l = (int) (f*l1 + (1-f)*l0);
											int c = lut.colors[l];
											r = (c >> 16)& 0xff;
											g = (c >> 8 )& 0xff;
											b =  c       & 0xff;
										}

										if (light > 0) {
											tr.x   =  p0.dx;
											tr.y   =  p0.dy;
											tr.z   =  p0.dz;
											tr.xyzPos_();

											float l = light*(-tr.X/(float)p0.len) + 1 ;
											r = (int)Math.min(255,l*r);
											g = (int)Math.min(255,l*g);
											b = (int)Math.min(255,l*b);
										}
										pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
									}
								}
							}
						}
						if ( p2 != null ) {
							tr.transform(p2);
							int x2 = tr.X,   y2 = tr.Y,   z2 = tr.Z;
							int dx20 = x2-x0, dy20 = y2-y0, dz20 = z2-z0;
							if (lutNr == ORIGINAL) {
								c2 = p2.color;
								r2 = (c2 >> 16)& 0xff;
								g2 = (c2 >> 8 )& 0xff;
								b2 =  c2       & 0xff;
							}
							else {
								l2 = p2.z + 128;
							}

							int numSteps = Math.max(Math.abs(dx20),Math.abs(dy20));

							float step = (numSteps > 0) ? 1 / (float)numSteps : 1;

							for (int s = 0; s < numSteps; s++) {
								float f = s * step;

								int x = (int) (x0 + f*dx20);
								int y = (int) (y0 + f*dy20);

								if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512
									int pos = (y<<9) | x;  // only for w = 512 !!!
									int z = (int) (z0 + f*dz20);
									if (z < pixelsZ[pos]) {
										pixelsZ[pos] = z;

										if (lutNr == ORIGINAL) {
											r = (int) (f*r2 + (1-f)*r0);
											g = (int) (f*g2 + (1-f)*g0);
											b = (int) (f*b2 + (1-f)*b0);
										}
										else {
											int l = (int) (f*l1 + (1-f)*l0);
											int c = lut.colors[l];
											r = (c >> 16)& 0xff;
											g = (c >> 8 )& 0xff;
											b =  c       & 0xff;
										}

										if (light > 0) {
											tr.x   =  p0.dx;
											tr.y   =  p0.dy;
											tr.z   =  p0.dz;
											tr.xyzPos_();

											float l = light*(-tr.X/(float)p0.len) + 1 ;
											r = (int)Math.min(255,l*r);
											g = (int)Math.min(255,l*g);
											b = (int)Math.min(255,l*b);
										}
										pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
									}
								}
							}

							if (( p1 != null ) && ( p2 != null )) {
								if ((x0 & ~511) == 0 && (y0 & ~511) == 0) { // only for w = h = 512
									int pos = (y0<<9) | x0;  // only for w = 512 !!!
									if (z0 < pixelsZ[pos]) {
										pixelsZ[pos] = z0;

										if (light > 0) {
											tr.x   =  p0.dx;
											tr.y   =  p0.dy;
											tr.z   =  p0.dz;
											tr.xyzPos_();

											float l = light*(-tr.X/(float)p0.len) + 1 ;

											r = (int)Math.min(255,l*((c0 >> 16)& 0xff));
											g = (int)Math.min(255,l*((c0 >> 8 )& 0xff));
											b = (int)Math.min(255,l*( c0       & 0xff));
											pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
										}

										else
											pixels[pos] = c0;
									}
								}
							}
						}
					}
				}
			}

			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
		}


		/**
		 *
		 *
		 */
		public synchronized void lines(){
			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0;
				pixelsZ[i] = 1000;
			}
			{

				for (int row=0; row<height-1; row++){
					for (int col=0; col<width-1; col++){
						int i = row*width + col;
						PlotVal p0 = plotList[i];
						int numSteps = 0;
						int l0 = 0, l1 = 0;
						//int r0 = 0, g0 = 0, b0 = 0, r1 = 0, g1 = 0, b1 = 0;
						int [] rgb0 = {0, 0, 0};
						int [] rgb1 = {0, 0, 0};

						if (p0 != null) {
							int c0 = p0.color;


							p0.z = p0.lumf - 128;

							tr.transform(p0);
							int x0 = tr.X, y0 = tr.Y, z0 = tr.Z;

							if (lutNr == ORIGINAL) {
								rgb0[0] = (c0 >> 16)& 0xff;
								rgb0[1] = (c0 >> 8 )& 0xff;
								rgb0[2] =  c0       & 0xff;
							}

							else {
								c0 = getLutColor(p0);
								l0 = p0.z + 128;
							}

							PlotVal p1 = plotList[i+1];

							if ( p1 != null ) {
								int c1 = p1.color;


								p1.z = p1.lumf - 128;

								tr.transform(p1);
								int x1 = tr.X,   y1 = tr.Y,   z1 = tr.Z;
								int dx1 = x1-x0, dy1 = y1-y0, dz1 = z1-z0;

								numSteps = Math.max(Math.abs(dx1),Math.abs(dy1));

								if (lutNr == ORIGINAL) {
									rgb1[0] = (c1 >> 16)& 0xff;
									rgb1[1] = (c1 >> 8 )& 0xff;
									rgb1[2] =  c1       & 0xff;
								}

								else {
									l1 = p1.z + 128;
								}

								float step = (numSteps > 0) ? 1 / (float)numSteps : 1;
								int r, g, b;
								for (int s = 0; s < numSteps; s++) {
									float f = s * step;

									int x = (int) (x0 + f*dx1);
									int y = (int) (y0 + f*dy1);

									if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512
										int pos = (y<<9) | x;  // only for w = 512 !!!
										int z = (int) (z0 + f*dz1);
										if (z < pixelsZ[pos]) {
											pixelsZ[pos] = z;

											if (lutNr == ORIGINAL ) {
												r = (int) (f*rgb1[0] + (1-f)*rgb0[0]);
												g = (int) (f*rgb1[1] + (1-f)*rgb0[1]);
												b = (int) (f*rgb1[2] + (1-f)*rgb0[2]);
											}
											else {
												int l = (int) (f*l1 + (1-f)*l0);
												int c = lut.colors[l];
												r = (c >> 16)& 0xff;
												g = (c >> 8 )& 0xff;
												b =  c       & 0xff;
											}
											if (light > 0) {
												tr.x   =  p0.dx;
												tr.y   =  p0.dy;
												tr.z   =  p0.dz;
												tr.xyzPos_();

												float l = light*(-tr.X/(float)p0.len) + 1 ;
												r = (int)Math.min(255,l*r);
												g = (int)Math.min(255,l*g);
												b = (int)Math.min(255,l*b);
											}
											pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
										}
									}
								}
							}

							if ( p1 == null  || numSteps == 0) {
								if ((x0 & ~511) == 0 && (y0 & ~511) == 0) { // only for w = h = 512
									int pos = (y0<<9) | x0;  // only for w = 512 !!!
									if (z0 < pixelsZ[pos]) {
										pixelsZ[pos] = z0;

										if (light > 0) {
											tr.x   =  p0.dx;
											tr.y   =  p0.dy;
											tr.z   =  p0.dz;
											tr.xyzPos_();

											float l = light*(-tr.X/(float)p0.len) + 1 ;

											int r = (int)Math.min(255,l*((c0 >> 16)& 0xff));
											int g = (int)Math.min(255,l*((c0 >> 8 )& 0xff));
											int b = (int)Math.min(255,l*( c0       & 0xff));
											pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
										}
										else
											pixels[pos] = c0;
									}
								}
							}
						}

					}
				}
			}

			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);

		}


		public synchronized void dots(){

			// clear image and z-buffer
			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0;
				pixelsZ[i] = 1000;
			}
			{
				for (int row=0; row<height-1; row++){
					for (int col=0; col<width-1; col++){
						int i = row*width + col;

						PlotVal p0 = plotList[i];
						if (p0 != null) {
							int c0 = (lutNr == ORIGINAL) ? p0.color : getLutColor(p0);

							p0.z = p0.lumf - 128;
							tr.transform(p0);
							int x = tr.X, y = tr.Y, z = tr.Z;


							if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512
								int pos = (y<<9) | x;  // only for w = 512 !!!

								if (z < pixelsZ[pos]) {
									pixelsZ[pos] = z;

									if (light > 0) {
										tr.x   =  p0.dx;
										tr.y   =  p0.dy;
										tr.z   =  p0.dz;
										tr.xyzPos_();

										float l = light*(-tr.X/(float)p0.len) + 1 ;

										int r = (int)Math.min(255,l*((c0 >> 16)& 0xff));
										int g = (int)Math.min(255,l*((c0 >> 8 )& 0xff));
										int b = (int)Math.min(255,l*( c0       & 0xff));
										pixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
									}
									else
										pixels[pos] = c0;
								}
							}
						}
					}
				}
			}

			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
		}


		private int getLutColor(PlotVal col) {
			int l = col.lumf;
			return lut.colors[l];
		}

		public synchronized void dotsNoLight(){

			for (int i = pixels.length-1; i >= 0; i--)  {
				pixels[i] = 0;
				pixelsZ[i] = 1000;
			}

			{
				for (int i=plotList.length-1; i>=0; i--){
					PlotVal p0 = plotList[i];
					if (p0 != null) {
						int c0 = 0;
						{
							p0.z = p0.lumf - 128;
							c0 = (lutNr == ORIGINAL) ? p0.color : getLutColor(p0);
						}
						tr.transform(p0);

						int x = tr.X;
						int y = tr.Y;
						int z = tr.Z;

						if ((x & ~511) == 0 && (y & ~511) == 0) { // only for w = h = 512
							int pos = (y<<9) | x;  // only for w = 512 !!!

							if (z < pixelsZ[pos]) {
								pixelsZ[pos] = z;
								pixels[pos] = c0;
							}
						}
					}
				}
			}

			image = Toolkit.getDefaultToolkit().createImage(source);
			imageRegion.setImage(image);
		}

	}


	/**
	 *
	 * @author alex
	 *
	 */
	private class Lut {
		private int [] colors;

		Lut() {
			colors = new int[256];
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

		void orange() {
			for (int i=0; i<256; i++) {
				int r = 255;
				int g = 184;
				int b = 0;
				colors[i] = 0xff000000  | (r << 16) | (g <<8) | b;
			}
		}
		void black() {
			for (int i=0; i<256; i++) {
				colors[i] = 0xff000000;
			}
		}
	}


	/**
	 *
	 * @author alex
	 *
	 */
	private final class PlotVal {

		public int lumt;
		public int lumf;
		private int lum;

		private int y;
		private int x;
		private int z;

		private int color;
		private int dx;
		private int dy;
		private int dz;
		private int len;

	}


	/**
	 *
	 * @author alex
	 *
	 */
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

	/**
	 *
	 * @author alex
	 *
	 */
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


} // end of class
