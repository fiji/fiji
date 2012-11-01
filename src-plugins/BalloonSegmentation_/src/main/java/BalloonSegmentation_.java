//package BalloonSegmentation;
/**
	This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA."""
 */
/*********************************************************************
 * Version: 2012
 ********************************************************************/

/*********************************************************************
 * Lionel Dupuy
 * JHI, Dundee
 * Invergowrie
 * DUNDEE DD2 5DA
 * Scotland
 * UK
 *
 * Lionel.Dupuy@hutton.ac.uk
 * http://www.hutton.ac.uk/research/groups/ecological-sciences/plant-soil-ecology/plant-systems-modelling
 ********************************************************************/

// General Packages
import ij.*;
import ij.gui.*;
import ij.gui.Overlay;
import ij.io.*;
import ij.process.*;
import ij.gui.ProgressBar;
import ij.plugin.filter.Convolver;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.geom.GeneralPath;
import java.awt.Graphics2D;
import java.awt.Graphics2D.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;


import java.io.*;
import java.io.File.*;
import java.util.*;
import javax.swing.*;
import javax.swing.AbstractButton.*;
import javax.swing.JComponent.*;
import javax.swing.text.JTextComponent.*;
import java.awt.Color;
import javax.swing.border.*;

import balloonstructure.*;
import utils.*;
import java.lang.System;

/** TODO  */
// Abort if no image
// export/import cells
// Updates graphics not working appart mouse-wheel induced
/**  TODO  */
public class BalloonSegmentation_ extends JFrame//JDialog
implements ActionListener, AdjustmentListener, ItemListener,
	WindowListener, KeyListener, MouseListener,  MouseWheelListener, PlugIn  { // Runnable,
	static String title = "2D BALLOON SEGMENTATION";

	// interface default parameter values
	static int init_channel = 2;					// initial channel selected for segmenting the cell architecture 1 - red, 2 - green, 3 - blue
	static int init_max_pixel = 40;				// initial max pixel intensity used for finding the doundaries of the colony of cells
	static int init_HL = 500;						// initial H*L factor used finding seeds in the population
	static int init_min_I = 80;					// initial pixel intensity when balloon algorithm starts
	static int init_max_I = 240;					// final pixel intensity when balloon algorithm stops

	// growing balloons definition structure
	Balloon bal1;						// inflatable balloon
	static BalloonPopulation pop;				// population of inflatable balloons
	// pop actually points to a element of popSequence
	static BalloonSequence popSequence;		// list of population associated with each stacked image
	static Watershed WAT;						// home made watershed for drawing the boundaries of the colony of tissue


	// edditing balloon/population properties
	static int selected_cell;					// id of the selected balloon candidate to modification
	static int currentSlice;	 				// index of the current slice
	static int[] currentStage;				// index indicating in which stage the user is in the process of segmentation: 1. bounds 2. points 3. connect 4. inflate
	static boolean is_movepoint = false;		// check if a point is being modified
	static int id_movepoint;					// id of the point being moved


	// image data. see InitiateImageData for details of how these are obtained
	static ImagePlus i1;						// imagePlus of the data currently analysed
	static ImageProcessor ipWallSegment;		// channelused for the segmenting of cell walls
	static ImageProcessor ipNuclSegment;		// channelused for the segmenting nucleus
	static int channel = 1;					// id of channel in use  X-DEL-X
	static ImageCanvas ic;
	static StackWindowRoi sw;

	// annotations
	static java.awt.geom.GeneralPath shape;  				// shapes drawing cellular shapes on the image
	static Font font = new Font("Test",Font.PLAIN,4); 	//	...
	FontMetrics fontMetrics = getFontMetrics(font);	//	...
	static RoiManager MROI = new RoiManager(true);
	static Overlay OL = new Overlay();

	// IO
	static public PrintWriter print_writer = null;
	static public String files_dir;
	static String[] file_list;

	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	//	Constructor & initialization
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
	 */

	public void run(java.lang.String arg) {
		i1 = IJ.getImage();	// get image  WindowManager
		if (i1 == null) {
			IJ.noImage();
			return;
		}

		ic = i1.getCanvas();					// get image canvas
		sw = new StackWindowRoi(i1, ic);
		i1.setWindow(sw);
	}

	public BalloonSegmentation_ ()
	{
		super("2D BALLOON SEGMENTATION");
		System.out.println("");
		System.out.println("");
		System.out.println("*******************************************************************");
		System.out.println("");
		System.out.println("Balloon Plugin for segmentation of plant cellular architectures");
		System.out.println("");
		System.out.println("Lionel Dupuy, JHI@Dundee");
		System.out.println("");
		System.out.println("*******************************************************************");
		System.out.println("");
		System.out.println("");

		loadProperties();
		doDialog();
		InitiateSegmentation();
	}

	/**
	 * Initiate the list of BalloonPopulation, convert to RGB if require
	 * this is meant to be run once in the image registration process
	 */
	public void InitiateSegmentation()
	{
		i1.setOverlay(null);
		i1.killRoi();
		Toolbar Tb = Toolbar.getInstance();
		Tb.setTool(Toolbar.HAND);


		if (i1.getType() != i1.GRAY8) {
			IJ.error("Sorry, this plugin currently works with 8bit grey scale images!");
			dispose();
			return;
		}


		ImageStack stack1 = i1.getStack();
		popSequence = new BalloonSequence(i1);
		currentStage = new int[i1.getStackSize()];
		IJ.showStatus("Loading");
		IJ.showProgress(0.0);

		for (int i=0; i<i1.getStackSize();i++){
			IJ.showProgress( ((double)i)/((double)i1.getStackSize()) );
			IJ.showStatus("Slice/Image-file: " + i + "/" +
					i1.getStackSize()) ;
			InitiateImageData (3, i+1);
			if (ipWallSegment != null)
			{
				ipWallSegment.setProgressBar(null);
				// Initiate the population
				popSequence.setSequence(i, ipWallSegment, 3);
			}
		}
		IJ.showStatus("");
		IJ.showProgress(1.0);
		//IJ.run("Close");
		currentSlice = i1.getCurrentSlice();
		ipWallSegment = (i1.getStack()).getProcessor(i1.getCurrentSlice());
		pop = popSequence.PopList[currentSlice-1];
	}

	/**
	 * Split red / green blue data prior to processing
	 * here the slice index start at 1, like imageJ does
	 */

	public void InitiateImageData (int channel, int slice)
	{
		// get current slice properties
		ImageStack stack1 = i1.getStack();
		currentSlice = slice;
		ipWallSegment = stack1.getProcessor(currentSlice);
		currentStage[currentSlice-1] = -1;

		// Stretch the Histogram
		//float s2 = (float)(16);
		//float[] kernelSm = {1/s2,2/s2,1/s2,2/s2,4/s2,2/s2,1/s2,2/s2,1/s2};
		//ipWallSegment.convolve( kernelSm, 3, 3);
		//IJ.run("Enhance Contrast", "saturated=0.0 normalize_all");
		i1.updateAndDraw();
		int w = ipWallSegment.getWidth();
		int h = ipWallSegment.getHeight();
	}

	/** load properties of the plugin from the properties file*/
	private void loadProperties() {
		InputStream propsFile;
		Properties tempProp = new Properties();

		try {
			//propsFile = new FileInputStream("plugins\\balloonplugin\\BalloonSegmentation.properties");
			propsFile = getClass().getResourceAsStream("/BalloonSegmentation.properties");
			tempProp.load(propsFile);
			propsFile.close();

			// load properties
			init_channel = Integer.parseInt(tempProp.getProperty("init_channel"));				// initial channel selected for segmenting the cell architecture 1 - red, 2 - green, 3 - blue
			init_max_pixel = Integer.parseInt(tempProp.getProperty("init_max_pixel"));			// initial max pixel intensity used for finding the doundaries of the colony of cells
			init_HL = Integer.parseInt(tempProp.getProperty("init_HL"));						// initial H*L factor used finding seeds in the population
			init_min_I = Integer.parseInt(tempProp.getProperty("init_min_I"));					// initial pixel intensity when balloon algorithm starts
			init_max_I = Integer.parseInt(tempProp.getProperty("init_max_I"));					// final pixel intensity when balloon algorithm stops

		}
		catch (IOException ioe) {
			IJ.error("I/O Exception: cannot read .properties file");
		}
	}
	/*
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
//                  FINDING THE OBJECT BOUNDARIES
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	 */
	/**
	 * Find the boundaries of the object with a watershed algorithm
	 */
	public void FindImgBoundaries (int PixLevel)
	{
		// find the boundaries
		WAT = new Watershed(ipWallSegment);
		WAT.Flow_bound(PixLevel);
		// output results
		int IMB[][][] = new int[WAT.sx][WAT.sy][3];
		IMB = WAT.IMB;
		pop.set_boundaries(IMB);
	}

	/*
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
//                  DRAWING ANNOTATIONS
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	 */
	/**
	 * Initiate the drawing of shapes on the image (other than ROIs)
	 */
	static void InitiateDraw() {
		shape  = new GeneralPath();
		i1.setOverlay(null);
		OL = new Overlay();
	}

	/**
	 * Delete all cells from the population and call clear draw
	 */
	static void ClearMarks() {
		if (pop != null)
		{
			pop.clear();
		}
		ClearDraw();
		i1.setOverlay(null);
		OL = new Overlay();
	}

	/**
	 * Clear all shapes on the image (other than ROIs)
	 */
	static void ClearDraw() {
		shape  = new GeneralPath();
		i1.setOverlay(null);
		OL = new Overlay();

	}

	/**
	 * draw the boundaries
	 */
	static void drawBounds() {
		if (currentStage[currentSlice-1]>0)
		{
			ImageStack stack1 = i1.getStack();
			ImageProcessor ip1 = stack1.getProcessor(currentSlice);
			pop.EnvelopBoundaries();
			if ((pop.YYi).length>0)
			{
				PolygonRoi Proi = new PolygonRoi(pop.XXi,pop.YYi,(pop.YYi).length,Roi.POLYGON);
				Proi.setStrokeColor(Color.red);
			    Proi.setStrokeWidth(3);
			    OL.add(Proi);
			}
			selected_cell = -1;
		}
	}
	/**
	 * Draw the ID of cells inflation
	 */
	static void drawCellID() {   // draws a cross at each cell centre
		float xc,yc;
		//GeneralPath path = new GeneralPath();
		if (currentStage[currentSlice-1]>1)
		{
			for (int i=0;i<pop.N;i++)
			{
				xc = (float) ( (Balloon)(pop.BallList.get(i)) ).x0;
				yc = (float) ( (Balloon)(pop.BallList.get(i)) ).y0;
				float arm  = 5;

				// Label Cell number
				TextRoi TROI = new TextRoi((int)xc, (int)yc, (""+i),new Font("SansSerif", Font.BOLD, 12));
				TROI.setNonScalable(false);
				OL.add(TROI);
			}
		}
	}
	/**
	 * Draw the center of the cells or alternatively the position of seeds before inflation
	 */
	static void drawCrosses() {   // draws a cross at each cell centre
		if (currentStage[currentSlice-1]>1)
		{
			float xc,yc;
			//GeneralPath path = new GeneralPath();

			for (int i=0;i<pop.N;i++)
			{
				xc = (float) ( (Balloon)(pop.BallList.get(i)) ).x0;
				yc = (float) ( (Balloon)(pop.BallList.get(i)) ).y0;
				float arm  = 5;

				// label cell position
				java.awt.geom.GeneralPath Nshape = new GeneralPath();
				Nshape.moveTo(xc-arm, yc);
				Nshape.lineTo(xc+arm, yc);
				Nshape.moveTo(xc, yc-arm);
				Nshape.lineTo(xc, yc+arm);

				Roi XROI = new ShapeRoi(Nshape);
				OL.add(XROI);
			}
		}
	}

	/**
	 * Draw a graph representing the result of the delaunay triangulation
	 */
	static void drawTopo() {
		if (currentStage[currentSlice-1]>2)
		{
			float x1,y1,x2,y2;
			int N = pop.N;
			ArrayList balloons = pop.BallList;

			for (int i=0;i<N;i++)
			{
				Point P1 = ((Balloon)(balloons.get(i))).getPoint();
				x1 = (float)(P1.getX());
				y1 = (float)(P1.getY());

				for (int j=0;j<i;j++)
				{
					if (pop.topo[i][j]==true)
					{
						// label connection between cells (potential neighbours)
						java.awt.geom.GeneralPath Tshape = new GeneralPath();
						Point P2 = ((Balloon)(balloons.get(j))).getPoint();
						x2 = (float)(P2.getX());
						y2 = (float)(P2.getY());
						Tshape.moveTo(x1, y1);
						Tshape.lineTo(x2, y2);
						Roi XROI = new ShapeRoi(Tshape);
						XROI.setStrokeWidth(1);
						XROI.setStrokeColor(Color.green);
						OL.add(XROI);
					}
				}
			}
		}
	}

	/**Draw a graph showing cell in contact with each other*/
	static void drawContacts(){
		if (currentStage[currentSlice-1]>3)
		{
			float x1,y1,x2,y2;
			int N = pop.N;
			ArrayList balloons = pop.BallList;

			for (int i=0;i<N;i++)
			{
				Point P1 = ((Balloon)(balloons.get(i))).getPoint();
				x1 = (float)(P1.getX());
				y1 = (float)(P1.getY());

				// draw connection between cell in contact
				for (int j=0;j<i;j++)
				{
					for (int k =0; k<((Balloon)(pop.BallList.get(i))).n0;k++)
					{
						if (pop.contacts[i][k]==j)
						{
							java.awt.geom.GeneralPath Cshape = new GeneralPath();
							Point P2 = ((Balloon)(balloons.get(j))).getPoint();
							x2 = (float)(P2.getX());
							y2 = (float)(P2.getY());
							Cshape.moveTo(x1, y1);
							Cshape.lineTo(x2, y2);
							Roi XROI = new ShapeRoi(Cshape);

							OL.add(XROI);
							break;
						}
					}
				}
			}
		}
	}

	/**Draw cells from the population of cells*/
	static void drawPop(){
		if (currentStage[currentSlice-1]>3)
		{
			float x1,y1;
			int N = pop.N;

			for (int i=0;i<N;i++)
			{
				Balloon bal;
				bal = (Balloon)(pop.BallList.get(i));
				int n = bal.XX.length;

				// filtering (for testing purposes)
				boolean isToDraw = true;
				Balloon B0 = ((Balloon)(pop.BallList.get(i)));
				B0.mass_geometry();
				if (pop.contacts != null)
				{
					for (int k=0;k<B0.n0;k++)
					{
						if (pop.contacts[i][k] == -1)
						{
							isToDraw = true;
							break;
						}
					}
				}
				// draw
				shape.setWindingRule(0);
				if (isToDraw)
				{
					PolygonRoi Proi = B0.Proi;
					Proi.setStrokeColor(Color.red);
				    Proi.setStrokeWidth(3);
				    OL.add(Proi);
				}
			}
		}
	}
	/**Draw ROI for cells from the population*/
	static void makeROI(){
		float x1,y1;
		int N = pop.N;

		for (int i=0;i<N;i++)
		{
			Balloon bal;
			bal = (Balloon)(pop.BallList.get(i));
			int n = bal.XX.length;

			// filtering (for testing purposes)
			boolean isToDraw = true;
			Balloon B0 = ((Balloon)(pop.BallList.get(i)));
			B0.mass_geometry();
			if (pop.contacts != null)
			{
				for (int k=0;k<B0.n0;k++)
				{
					if (pop.contacts[i][k] == -1)
					{
						isToDraw = true;
						break;
					}
				}
			}
			// draw
			shape.setWindingRule(0);
			if (isToDraw)
			{
				PolygonRoi Proi = B0.Proi;
			    MROI.add(i1,Proi,i);

			    // stuff to keep from old name
/*			    int nROI = MROI.getCount()-1;
			    String ROIName_old = MROI.getName(""+nROI);
			    String ROIName_new_sup = "";//ROIName_old.substring(4,14);

			    // part of the name that change due to match the slice number
			    String ROIName_new_sub = "";
			    if (currentSlice<10)
			    {
				ROIName_new_sub = "000" + currentSlice;
			    }
			    else if (currentSlice<100)
			    {
				ROIName_new_sub = "00" + currentSlice;
			    }
			    else if (currentSlice<1000)
			    {
				ROIName_new_sub = "0" + currentSlice;
			    }
			    else if (currentSlice<10000)
			    {
				ROIName_new_sub = "" + currentSlice;
			    }
			    String ROIName_new = ROIName_old;//ROIName_new_sub + ROIName_new_sup;
			    // Change names in the ROI manager
			    IJ.runMacro("roiManager(\"Select\"," + nROI + ") ",null);
			    IJ.runMacro("roiManager(\"Rename\", \"" + ROIName_new + "\") ",null);*/
			}
		}

	}

	/**Display the relevant annotation on the image*/
	static void showOverlay() {
		i1.setOverlay(OL);
	}
	static public void makeOverlayCurrent(int stage)
	{

		if (stage>0){
			InitiateDraw();
			drawBounds();
		}
		if (stage>1){
			drawCrosses();
			drawCellID();}
		if (stage>2 & stage<4){
			drawTopo();}
		if (stage>3)
			{
			drawPop();
			}
	}
	static public void drawCurrent()
	{
		currentSlice = i1.getCurrentSlice();
		pop = popSequence.PopList[currentSlice-1];
		ipWallSegment = (i1.getStack()).getProcessor(currentSlice);

		ClearDraw();
		makeOverlayCurrent(currentStage[i1.getCurrentSlice()-1]);
		showOverlay();
	}
	/*
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
//                  BALLOON STRUCTURE (add remove find balloons)
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	 */
	/**creates a population of palloons from a list of points in the image*/
	public void InitiateBalloons (ArrayList X0, ArrayList Y0)
	{
		// set the right data
		currentSlice = i1.getCurrentSlice();
		ipWallSegment = (i1.getStack()).getProcessor(i1.getCurrentSlice());
		popSequence.setSequence(currentSlice-1, ipWallSegment, X0, Y0, channel);
		pop = popSequence.PopList[currentSlice-1];
	}

	/**adds 1 balloon to the population of existing balloons*/
	public void InitiateBalloons (int x0, int y0)
	{
		// set the right data
		currentSlice = i1.getCurrentSlice();
		ipWallSegment = (i1.getStack()).getProcessor(i1.getCurrentSlice());
		pop = popSequence.PopList[currentSlice-1];
		pop.AddNewBalloon(x0,y0);
		pop = popSequence.PopList[currentSlice-1];
	}

	/**removes the closest balloon from x0,y0, if in a near distance*/
	public void RemoveBalloons (int x0, int y0)
	{
		currentSlice = i1.getCurrentSlice();
		ipWallSegment = (i1.getStack()).getProcessor(i1.getCurrentSlice());
		pop = popSequence.PopList[currentSlice-1];

		double min_distance = 100000;
		int candidate = -1;
		for (int i=0;i<pop.BallList.size();i++)
		{
			Point P = ((Balloon)(pop.BallList.get(i))).getPoint();
			double d = (x0-P.getX())*(x0-P.getX()) + (y0-P.getY())*(y0-P.getY());
			if (d<min_distance)
			{
				candidate = i;
				min_distance = d;
			}
		}
		if (min_distance<64)
		{
			pop.remove(candidate);
		}
		pop = popSequence.PopList[currentSlice-1];
	}


	/**Moves the closest balloon from x0,y0 to the next clicked position x0, y0*/
	public void MoveBalloons (int x0, int y0)
	{
		if (is_movepoint == false)
		{
			//ArrayList<float> X0 = new ArrayList<float>();
			//ArrayList<float> Y0 = new ArrayList<float>();
			double min_distance = 100000;
			int candidate = -1;
			for (int i=0;i<pop.BallList.size();i++)
			{
				Point P = ((Balloon)(pop.BallList.get(i))).getPoint();
				double d = (x0-P.getX())*(x0-P.getX()) + (y0-P.getY())*(y0-P.getY());
				if (d<min_distance)
				{
					candidate = i;
					min_distance = d;
				}
			}
			if (min_distance>64) { id_movepoint = -1; }
			else { is_movepoint = true; id_movepoint = candidate;}
		}
		else if  (is_movepoint == true & id_movepoint>-1)
		{
			Balloon B = (Balloon)(pop.BallList.get(id_movepoint));
			B.translateTo(x0,y0);
			is_movepoint = false;
			id_movepoint = -1;
		}
	}
	/*
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
// IO function: saving and loading the structure
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	 */

	/**Write a file showing */
	public boolean write2File(String directory, String file_name, String info) {
		try {
			FileOutputStream fos = new FileOutputStream(directory + file_name);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			print_writer = new PrintWriter(bos);
			print_writer.print(info);
			print_writer.close();
			return true;
		}
		catch (IOException e) {
			IJ.error("File could not be written properly in write2File" );
			return false;
		}
	}

	/**Write a file showing */
	public void makeReport()
	{
		if (currentStage[currentSlice-1] > 1)
		{
			ResultsTable rt = new ResultsTable();
			for (int j=0;j<popSequence.N;j++)
			{
				pop = popSequence.PopList[j];
				int N = pop.N;
				for (int i=0;i<N;i++)
				{
					rt.incrementCounter();
					Balloon bal;
					bal = (Balloon)(pop.BallList.get(i));
					bal.mass_geometry();
					rt.addValue("X",bal.x0);
					rt.addValue("Y",bal.y0);
					rt.addValue("Z",j);
					rt.addValue("ID",bal.id);
					rt.addValue("AREA",bal.area);
					rt.addValue("Ixx",bal.Ixx);
					rt.addValue("Iyy",bal.Iyy);
					rt.addValue("Ixy",bal.Ixy);
					rt.addValue("Lx",bal.lx);
					rt.addValue("Ly",bal.ly);
				}
			}
			rt.show("Report");
			currentSlice = i1.getCurrentSlice();
			ipWallSegment = (i1.getStack()).getProcessor(i1.getCurrentSlice());
			pop = popSequence.PopList[currentSlice-1];
		}
	}

	/**Clear all before closing */
	public void clearAll()
	{
		ClearMarks();
		pop = null;
		popSequence = null;
		i1.killRoi();
		ic.removeMouseListener(this);
		ic.removeKeyListener(this);
		StackWindow sw1 = new StackWindow(i1, ic);
		i1.setWindow(sw1);
		dispose();
	}

	/*
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	// Build the dialog box.
//-------------------------------------------------------------------------------
//-------------------------------------------------------------------------------
	 */
	private JSlider PixelLevel;
	private BoxLayout mainLayout;
	private BoxLayout pixelLayout;

	private GridBagLayout 	ActionLayout;
	private GridBagConstraints 	constraint;
	// Layout for Sub components of the Action panel
	private GridLayout 	boundsLayout;
	private GridLayout 	centerLayout;
	private GridLayout 	inflateLayout;
	private GridLayout 	resultsLayout;
	private GridLayout 	buttonsLayout;

	private JButton 		bnBound;
	//private JButton 		bnNeig;
	private JButton 		bnInflate;
	private JButton 		bnRefine;
	private JButton 		bnOptimize;
	private JButton 		bnClose;
	private JButton 		bnSample;
	private JToggleButton		bnEdit;
	private JButton 		bnClear;
	private JButton 		bnShow;
	private JButton 		bnModify;
	private JButton 		bnOpen;
	private JButton 		bnCapture;

	private JTextField	txtPixLevel;
	private JTextField	txtRem;
	private JTextField	txtHL;
	private JTextField	txtCellID;
	private JTextField	txtIniLevel;
	private JTextField	txtEndLevel;

	/*private CheckboxGroup choiceVisu = new CheckboxGroup();
	private Checkbox ChkBounds;
	private Checkbox ChkCentres;
	private Checkbox ChkTopo;
	private Checkbox ChkArchi;*/
	private Checkbox ChkProcessAll1;
	private Checkbox ChkProcessAll2;

	//private Scrollbar	scrpar1;
	private Checkbox	chkLog;
	private JComboBox showOptionsList;
	private JComboBox editOptionsList;
	int min_pix_level = 0;
	int max_pix_level = 100;
	private void doDialog() {
		// Layout
		ActionLayout = new GridBagLayout();
		constraint = new GridBagConstraints();

		boundsLayout = new GridLayout(1,2,10,10);
		centerLayout = new GridLayout(3,2,10,10);
		inflateLayout = new GridLayout(1,2,10,10);
		resultsLayout = new GridLayout(3,2,10,10);
		buttonsLayout = new GridLayout(1,2,10,10);

		// slider setting pixlevel
		PixelLevel = new JSlider(JSlider.VERTICAL,
				min_pix_level, max_pix_level, init_max_pixel);
		PixelLevel.setMajorTickSpacing(10);
		PixelLevel.setMinorTickSpacing(2);
		PixelLevel.setPaintTicks(true);
		PixelLevel.setPaintLabels(true);
		PixelLevel.addMouseWheelListener(this);
		// buttons
		bnBound = new JButton(" Bounds ");
		bnBound.setBackground(new Color(255,200,200));
		bnSample = new JButton("Sample (H*L)");
		bnSample.setBackground(Color.LIGHT_GRAY);
		bnEdit = new JToggleButton(" Manual ");
		bnEdit.setBackground(Color.LIGHT_GRAY);
		bnOpen = new JButton("    Import    ");
		bnOpen.setBackground(Color.LIGHT_GRAY);
		//bnNeig = new JButton(" Connect ");
		//bnNeig.setBackground(new Color(255,200,200));
		bnInflate = new JButton(" Inflate ");
		bnInflate.setBackground(new Color(255,200,200));
		bnClose =new JButton("    Close   ");
		bnModify = new JButton("Modify (Cell #)");
		bnModify.setBackground(Color.LIGHT_GRAY);
		bnCapture = new JButton("    Capture    ");
		bnCapture.setBackground(Color.LIGHT_GRAY);

		bnClear = new JButton(" Clear ");

		txtRem = new JTextField("0", 1);
		txtHL = new JTextField(""+init_HL, 1);
		txtCellID = new JTextField("0", 1);

		ChkProcessAll1 = new Checkbox("Process All", false) ;
		ChkProcessAll2 = new Checkbox("Process All", false) ;


		//ComboBox for View options/////////////////////////////////////////////////////////////////////
		String[] showOptions = { "Bounds", "Centres", "Interactions", "Neighbours", "Cells", "Report"};
		showOptionsList = new JComboBox(showOptions);
		showOptionsList.setSelectedIndex(0);
		showOptionsList.addActionListener(this);
		//ComboBox for Edit Option/////////////////////////////////////////////////////////////////////
		String[] editOptions = { "Add", "Move", "Del"};
		editOptionsList = new JComboBox(editOptions);
		editOptionsList.setSelectedIndex(0);
		/////////////////////////////////////////////////////////////////////////////////////////////////

		// Panel parameters
		JPanel pnMain = new JPanel();
		mainLayout = new BoxLayout(pnMain, BoxLayout.X_AXIS);
		JPanel pnAction = new JPanel();

		JPanel pnBounds = new JPanel();
		Border bd = BorderFactory.createBevelBorder(BevelBorder.RAISED); //BorderFactory.createEtchedBorder(); //
		pnBounds.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(bd, "Find Boundaries"), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		JPanel pnCenter = new JPanel();
		pnCenter.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(bd, "Find Centers"), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		JPanel pnInflate = new JPanel();
		pnInflate.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(bd, "Cell Detection"), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		JPanel pnResults = new JPanel();
		pnResults.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(bd, "Show Results"), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		JPanel pnButtons = new JPanel();


		// Panel for the slider
		JPanel pnPixel = new JPanel();
		pixelLayout = new BoxLayout(pnPixel, BoxLayout.X_AXIS);
		pnPixel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(bd, "Pixel I."), BorderFactory.createEmptyBorder(7, 7, 7, 7)));
		pnPixel.add(PixelLevel);
		pnPixel.setLayout(pixelLayout);

		pnMain.setLayout(mainLayout);
		pnAction.setLayout(ActionLayout);


		// Add components
		//addComponent(pnAction, 30, 0, 1, 1, 2, new JLabel("BOUNDS"));
		pnBounds.add(ChkProcessAll1);
		pnBounds.add(bnBound);
		pnBounds.setLayout(boundsLayout);
		//addComponent(pnCenters, 55, 0, 1, 1, 5, new JLabel("CENTERS"));
		pnCenter.add(editOptionsList);
		pnCenter.add(bnEdit);
		pnCenter.add(txtHL);
		pnCenter.add(bnSample);
		pnCenter.add(new Label(""));
		pnCenter.add(bnOpen);
		pnCenter.setLayout(centerLayout);

		//addComponent(pnAction, 90, 0, 1, 1, 5, new JLabel("SEGMENTATION"));
		pnInflate.add(ChkProcessAll2);
		pnInflate.add(bnInflate);
		pnInflate.setLayout(inflateLayout);

		//addComponent(pnResults, 120, 0, 1, 1, 5, new JLabel("RESULTS"));
		pnResults.add(new Label(""));
		pnResults.add(showOptionsList);
		pnResults.add(txtCellID);
		pnResults.add(bnModify);
		pnResults.add(new Label(""));
		pnResults.add(bnCapture);
		pnResults.setLayout(resultsLayout);


		pnButtons.add(bnClear);
		//pnButtons.add(bnOpen);
		pnButtons.add(bnClose);
		pnButtons.setLayout(buttonsLayout);
		// Implement the listeners
		// close listener
		addWindowListener(new WindowAdapter(){
		      public void windowClosing(WindowEvent we){
			  clearAll();
		      }
		    });

		// buttons listeners
		bnBound.addActionListener(this);
		bnSample.addActionListener(this);
		bnClear.addActionListener(this);
		bnEdit.addActionListener(this);
		bnClose.addActionListener(this);
		bnInflate.addActionListener(this);
		//bnNeig.addActionListener(this);
		bnModify.addActionListener(this);
		bnOpen.addActionListener(this);
		bnCapture.addActionListener(this);
		i1 = WindowManager.getCurrentImage();
		ic = i1.getCanvas();

		ic.addKeyListener(this);
		ic.addMouseListener(this);

		////////////////////////////////////////////////////////////////
		// Build panel
		////////////////////////////////////////////////////////////////
		addComponent(pnAction, 1, 0, 1, 1, 5, pnBounds);
		addComponent(pnAction, 2, 0, 1, 1, 5, pnCenter);
		addComponent(pnAction, 3, 0, 1, 1, 5, pnInflate);
		addComponent(pnAction, 4, 0, 1, 1, 5, pnResults);
		addComponent(pnAction, 5, 0, 1, 1, 5, pnButtons);

		pnMain.add(pnAction);

		pnMain.add(Box.createRigidArea(new Dimension(10, 0)));
		pnMain.add(pnPixel);

		add(pnMain);
		pack();
		setResizable(true);
		GUI.center(this);
		setVisible(true);
		Point IJ_location = (IJ.getInstance()).getLocation();
		int Dialog_width = getWidth();
		int XX = (int)(IJ_location.getX()  + (IJ.getInstance()).getWidth()- Dialog_width) ;
		int YY = (int)(IJ_location.getY()  + (IJ.getInstance()).getHeight());
		Point Dialog_pos = new Point(XX, YY);
		setLocation(Dialog_pos);
	}

	final private void addComponent(
			final JPanel pn,
			final int row, final int col,
			final int width, final int height,
			final int space,
			final Component comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = constraint.HORIZONTAL;
		ActionLayout.setConstraints(comp, constraint);
		pn.add(comp);
	}

	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		// check is slice number has changed

		notify();
	}
	public synchronized  void actionPerformed(ActionEvent e) {

		Toolbar Tb = Toolbar.getInstance();
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		//		B O U N D A R I E S
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////

		if (e.getSource() == bnBound) {
			if (ChkProcessAll1.getState())
			{
				for (int i=0;i<i1.getStackSize();i++)
				{
					currentSlice = i+1;
					pop = popSequence.PopList[currentSlice-1];
					ipWallSegment = (i1.getStack()).getProcessor(currentSlice);

					IJ.showStatus("Find bounds slice: " + (i+1));
					// FIND THE BOUNDS
					int pix_level = (int)(PixelLevel.getValue());
					FindImgBoundaries(pix_level);
					InitiateDraw();
					drawBounds();
					drawCellID();
					drawCrosses();
					showOverlay();
					currentStage[currentSlice-1] = 1;
					// SET THE TOOLBAR TO ROI EDIT
					Tb.setTool(Toolbar.RECTANGLE);

					// CHECK THAT EDIT NOT SELECTED
					bnEdit.setSelected(false);
				}
				IJ.showStatus("");

			}
			else
			{
				currentSlice = i1.getCurrentSlice();
				ipWallSegment = (i1.getStack()).getProcessor(currentSlice);
				pop = popSequence.PopList[currentSlice-1];

				IJ.showStatus("Find object bounds ");
				// FIND THE BOUNDS
				int pix_level = (int)(PixelLevel.getValue());
				FindImgBoundaries(pix_level);
				InitiateDraw();
				drawBounds();
				drawCellID();
				drawCrosses();
				showOverlay();
				currentStage[currentSlice-1] = 1;
				// SET THE TOOLBAR TO ROI EDIT
				Tb.setTool(Toolbar.RECTANGLE);

				// SET COMBOBOX VIEW
				//showOptionsList.setSelectedIndex(0);

				// CHECK THAT EDIT NOT SELECTED
				bnEdit.setSelected(false);
				IJ.showStatus("");
			}

			currentSlice = i1.getCurrentSlice();
			ipWallSegment = (i1.getStack()).getProcessor(currentSlice);
			pop = popSequence.PopList[currentSlice-1];
			InitiateDraw();
			drawBounds();
			drawCellID();
			drawCrosses();
			showOverlay();
			currentStage[currentSlice-1] = 1;

		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		//		S E E D
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////

		if (e.getSource() == bnSample) {
			if (currentStage[currentSlice-1] >=1)
			{
				i1.killRoi();
				float HL = Float.valueOf(txtHL.getText()).floatValue();
				pop.sample(HL);
				ClearDraw();

				InitiateDraw();
				drawBounds();
				drawCellID();
				drawCrosses();
				showOverlay();
				currentStage[currentSlice-1] = 2;

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);

				// SET COMBOBOX VIEW
				//showOptionsList.setSelectedIndex(1);

				// CHECK THAT EDIT NOT SELECTED
				bnEdit.setSelected(true);
			}
			else
			{
				IJ.error("You must use find the boundaries first");
			}
		}

		if (e.getSource() == bnEdit)
		{
			if (currentStage[currentSlice-1] >=1)
			{
				currentStage[currentSlice-1] = 3;

				i1.killRoi();
				InitiateDraw();
				drawBounds();
				drawCrosses();
				drawCellID();
				showOverlay();


				// sets the variables for moving points
				is_movepoint = false;		// check if a point is being modified
				id_movepoint = -1;			// id of the point being moved

				// SET COMBOBOX VIEW
				//showOptionsList.setSelectedIndex(1);

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}
			else
			{
				IJ.error("You must find the boundaries");
			}
		}

		if (e.getSource() == bnOpen) {
			/* Opens an 'open file' with the default directory as the imageJ 'image' directory*/
			OpenDialog od = new OpenDialog("Open structure", IJ.getDirectory("image"), "");
			files_dir = od.getDirectory();
			if (files_dir == null) return;
			file_list = new File(od.getDirectory()).list();

			// get the file extension
			int dotPlace = od.getFileName().lastIndexOf ( '.' );
			String ext = od.getFileName().substring( dotPlace + 1 );
			//String seg_ext = "txt";
			String pts_ext = "txt";
			//String seq_ext = "seq";
			if (dotPlace>1 & ext.compareTo(pts_ext) == 0)
			{
				ArrayList<double[]> SeedList = popSequence.importSeeds(od.getDirectory(), od.getFileName(), currentSlice);
				boolean isCurrentStageCorrect = true;
				for (int i=0;i<SeedList.size();i++)
				{
					// Extract next seed coordinate
					double[] row = SeedList.get(i);

					// Find relevant slice number
					int slice = currentSlice-1;
					if (row[2] >=0) {slice = (int)row[2];}
					// Find the right population and add one bolloon to it
					if (currentStage[slice]>=1){
						pop = popSequence.PopList[slice];
						pop.AddNewBalloon((int)row[0],(int)row[1]);
						currentStage[slice] = 2;
					}
					else {isCurrentStageCorrect = false;}
				}
				if (!isCurrentStageCorrect)
				{
					IJ.error("Some seeds were not used because object boundaries not found on some slices");
				}
			}
			currentSlice = i1.getCurrentSlice();
			ipWallSegment = (i1.getStack()).getProcessor(currentSlice);
			pop = popSequence.PopList[currentSlice-1];

			// show seeds being imported
			i1.killRoi();
			InitiateDraw();
			drawBounds();
			drawCellID();
			drawCrosses();
			showOverlay();

			// CHECK THAT EDIT NOT SELECTED
			bnEdit.setSelected(false);
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		//		S E G M E N T A T I O N
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////


		if (e.getSource() == bnInflate) {
			if (ChkProcessAll2.getState())
			{
				for (int i=0;i<i1.getStackSize();i++)
				{
					currentSlice = i+1;
					pop = popSequence.PopList[currentSlice-1];
					ipWallSegment = (i1.getStack()).getProcessor(currentSlice);

					IJ.showStatus("Find bounds slice: " + (i+1));
					if (currentStage[currentSlice-1] >=2)
					{
						// connect balloons together
						if (currentStage[currentSlice-1]<3)
						{

							pop.InitiateGrowingRegion();
							pop.MakeTopo();
							currentStage[currentSlice-1] = 3;
						}
					}
					else
					{
						IJ.error("You must select seeds");
					}
					if (currentStage[currentSlice-1] >=3)
					{
						// connect balloons together
						if (currentStage[currentSlice-1]<4)
						{
							pop.InitiateGrowingRegion();
							pop.MakeTopo();
						}

						// Inflate
						i1.killRoi();
						int ninc = 100;
						int ini = (int)(PixelLevel.getValue());
						//IJ.showProgress(0.0);
						for (int t=1;t<ninc+1;t++)
						{
							pop.Tick_inflate(ini,t);
							if ((t % 10) == 0) {
								showOverlay();
							IJ.showStatus("Inflate: " + (int)((double)(t)/(double)(ninc)*100) +"%");
							}
						}
						IJ.showProgress(1.0);
						IJ.showStatus("");
						/* Update and Save result in pop0*/


						pop.refineStructure();
						pop.mass_Geometry ();
						/* Start drawing*/
						InitiateDraw();
						drawBounds();
						drawCellID();
						drawCrosses();
						drawPop();
						showOverlay();
						currentStage[currentSlice-1] = 4;

						// SET THE TOOLBAR TO HAND
						Tb.setTool(Toolbar.HAND);

						// CHECK THAT EDIT NOT SELECTED
						bnEdit.setSelected(false);

					}
				}
				currentSlice = i1.getCurrentSlice();
				ipWallSegment = (i1.getStack()).getProcessor(currentSlice);
				pop = popSequence.PopList[currentSlice-1];

				InitiateDraw();
				drawBounds();
				drawCellID();
				drawCrosses();
				drawPop();
				showOverlay();

				IJ.showStatus("");

			}
			else
			{
				if (currentStage[currentSlice-1] >=2)
				{
					// connect balloons together
					if (currentStage[currentSlice-1]<3)
					{
						pop.InitiateGrowingRegion();
						pop.MakeTopo();
						currentStage[currentSlice-1] = 3;
					}
				}
				else
				{
					IJ.error("You must select seeds");
				}
				if (currentStage[currentSlice-1] >=3)
				{
					// connect balloons together
					if (currentStage[currentSlice-1]<4)
					{
						pop.InitiateGrowingRegion();
						pop.MakeTopo();
					}

					// Inflate
					i1.killRoi();
					int ninc = 100;
					int ini = (int)(PixelLevel.getValue());
					for (int t=1;t<ninc+1;t++)
					{
						pop.Tick_inflate(ini,t);
						if ((t % 10) == 0) {
							showOverlay();
							IJ.showStatus("Inflate: " + (int)((double)(t)/(double)(ninc)*100) +"%");
						}
					}
					IJ.showProgress(1.0);
					IJ.showStatus("");

					pop.refineStructure();
					pop.mass_Geometry ();
					currentStage[currentSlice-1] = 4;
					// Start drawing
					InitiateDraw();
					drawBounds();
					drawCellID();
					drawCrosses();
					drawPop();
					showOverlay();


					// SET THE TOOLBAR TO HAND
					Tb.setTool(Toolbar.HAND);

					// SET COMBOBOX VIEW
					//showOptionsList.setSelectedIndex(3);

					// CHECK THAT EDIT NOT SELECTED
					bnEdit.setSelected(false);

				}
			}
		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		//		O U T P U T
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////

		if (e.getSource() == showOptionsList) {  // (false){//
			i1.killRoi();

	        String showType = (String)(showOptionsList.getSelectedItem());

			if (showType == "Bounds"){
				selected_cell = -1;
				InitiateDraw();
				drawBounds();
				showOverlay();

				// SET THE TOOLBAR TO HANDLE ROI
				Tb.setTool(Toolbar.RECTANGLE);
			}
			else if (showType == "Centres"){
				selected_cell = -2;
				InitiateDraw();
				drawBounds();
				drawCrosses();
				drawCellID();
				showOverlay();

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}
			else if (showType == "Interactions")
			{
				selected_cell = -2;
				InitiateDraw();
				drawBounds();
				drawTopo();
				drawCellID();
				showOverlay();

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}
			else if (showType == "Neighbours") {
				selected_cell = -2;
				InitiateDraw();
				drawBounds();
				drawContacts();
				drawCellID();
				showOverlay();

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}
			else if (showType == "Cells") {
				selected_cell = -2;
				InitiateDraw();
				drawBounds();
				drawCellID();
				drawPop();
				showOverlay();

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}
			else if (showType == "Report") {
				makeReport();
				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.HAND);
			}

			// CHECK THAT EDIT NOT SELECTED
			bnEdit.setSelected(false);
		}
		if (e.getSource() == bnModify) {
			int id = Integer.parseInt(txtCellID.getText());
			if (id<(pop.BallList).size())
			{
				bal1 = (Balloon)(pop.BallList.get(id));

				int[][] bounds = bal1.Cexpand(false);
				int[] XXi = bounds[0];
				int[] YYi = bounds[1];

				//bal1.Select_boundaries(i1);
				PolygonRoi Proi = new PolygonRoi(XXi,YYi,(XXi).length,Roi.POLYGON);
				selected_cell = id;
				i1.setRoi(Proi);

				// SET THE TOOLBAR TO HAND
				Tb.setTool(Toolbar.RECTANGLE);

				//currentStage[currentSlice-1] = 4;

				// CHECK THAT EDIT NOT SELECTED
				bnEdit.setSelected(false);
			}
	}

		if (e.getSource() == bnCapture) {
			// Prepare image data for overlayed output
			ImagePlus ip_capture =  new ImagePlus();
			ImageStack stack = i1.createEmptyStack();
			int currentSlice_buff = currentSlice;

			// clear previous ROIs
			MROI.close();
			MROI = new RoiManager();
			for (int i=0;i<i1.getStackSize();i++)
			{
				currentSlice = i+1;
				pop = popSequence.PopList[currentSlice-1];
				ipWallSegment = (i1.getStack()).getProcessor(currentSlice);

				ClearDraw();
				ImageProcessor ip = (i1.getStack()).getProcessor(currentSlice);
				ImagePlus flat_im = new ImagePlus("" , ip );
				makeOverlayCurrent(currentStage[currentSlice-1]);

				flat_im.setOverlay(OL);
				ImageProcessor cp = (flat_im.flatten()).getProcessor();
				stack.addSlice(""+i, cp);
				if (currentStage[currentSlice-1]>3)
				{
					i1.setSlice(currentSlice);
					makeROI();
				}
			}

			MROI.show();
			ip_capture.setStack("Capture", stack);
			ip_capture.show();

			currentSlice = currentSlice_buff;
			pop = popSequence.PopList[currentSlice-1];
			ipWallSegment = (i1.getStack()).getProcessor(currentSlice);
			drawCurrent();

			// CHECK THAT EDIT NOT SELECTED
			bnEdit.setSelected(false);
		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		//		C L E A R
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////

		if (e.getSource() == bnClose) {
			clearAll();
		}
		if (e.getSource() == bnClear) {
			ClearMarks();
			pop = null;
			popSequence.PopList[currentSlice-1] = null;
			currentStage[currentSlice-1] = -1;
			i1.killRoi();

			// SET THE TOOLBAR TO HAND
			Tb.setTool(Toolbar.HAND);

			// SET COMBOBOX VIEW
			//showOptionsList.setSelectedIndex(0);

			// CHECK THAT EDIT NOT SELECTED
			bnEdit.setSelected(false);
			IJ.showProgress(1.0);
			IJ.showStatus("");
		}

		notify();
	}
	public synchronized void itemStateChanged(ItemEvent e) { notify(); 	}
	public void windowActivated(WindowEvent e) {}
	public void windowClosing(WindowEvent e) { dispose(); }
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) { 	}
	public void windowDeiconified(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowOpened(WindowEvent e){}


	// *************************
	// Mouse event manager
	// *************************
	public void mouseWheelMoved(MouseWheelEvent event) {
		//synchronized(this) {
			int pixlevel = (int)(PixelLevel.getValue()) - event.getWheelRotation();
			if (pixlevel < min_pix_level)
				pixlevel = min_pix_level;
			else if (pixlevel > max_pix_level)
				pixlevel = max_pix_level;
			PixelLevel.setValue(pixlevel);
			}
	public void mouseReleased(MouseEvent e)
	{
		// modify the coordinates of the corresponding balloon if a cell is selected when mouse is released
		Roi roi = i1.getRoi();
		if (roi !=null)
		{
			if (roi.getType() == roi.POLYGON & selected_cell>-1) // modify the selected cell
			{
				Polygon p = roi.getPolygon();
				int[] XXi = p.xpoints;
				int[] YYi = p.ypoints;
				bal1 = (Balloon)(pop.BallList.get(selected_cell));
				bal1.setXX(XXi);
				bal1.setYY(YYi);
				bal1.mass_geometry();

				InitiateDraw();
				drawBounds();
				drawCellID();
				drawCrosses();
				drawPop();
				showOverlay();
			}
			else if (roi.getType() == roi.POLYGON & selected_cell==-1)	// modify the boundaries
			{
				Polygon p = roi.getPolygon();
				int[] XXi = p.xpoints;
				int[] YYi = p.ypoints;
				pop.modify_boundaries(XXi,YYi);
			}
		}
	}

	public void mousePressed(MouseEvent e)  	{
	if (bnEdit.isSelected()){
			int x = ic.offScreenX(e.getX());
			int y = ic.offScreenY(e.getY());
			int xx=0;
		    String editType = (String)(editOptionsList.getSelectedItem());
			if (editType == "Add"){InitiateBalloons(x,y);}				// editOptionsList add a point to the list
			else if (editType == "Del"){RemoveBalloons(x,y);}				// delete point
			else if (editType == "Move"){MoveBalloons(x,y);}				// move point
			InitiateDraw();
			drawBounds();
			drawCellID();
			drawCrosses();
			showOverlay();
			currentStage[currentSlice-1] = 2;
		}

	}
	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}


	// *************************
	// KeyPress event manager
	// *************************
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e)
	{
		Roi roi = i1.getRoi();
		Polygon p = roi.getPolygon();
		int[] XXi = p.xpoints;
		int[] YYi = p.ypoints;

		PolygonRoi Proi = new PolygonRoi(XXi,YYi,(XXi).length,Roi.POLYGON);
		i1.setRoi(Proi);
	}
	public String modifiers(int flags) {
		String s = " [ ";
		if (flags == 0) return "";
		if ((flags & KeyEvent.SHIFT_MASK) != 0) s += "Shift ";
		if ((flags & KeyEvent.CTRL_MASK) != 0) s += "Control ";
		if ((flags & KeyEvent.META_MASK) != 0) s += "Meta ";
		if ((flags & KeyEvent.ALT_MASK) != 0) s += "Alt ";
		s += "] ";
		return s;
	}
	/*
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
		// Custom classes (Stackwindow, ROI) for linstening user events and have customized response.
	//-------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------
		 */


	/** StackWindowRoi: a stackwindow that allow updating Overlays on the graph when changing slices in the stack**/
	static class StackWindowRoi extends StackWindow implements MouseWheelListener{
		ListenerAdj Ladj;
		StackWindowRoi(ImagePlus image, ImageCanvas canvas) {
			super(image, canvas);

			imp.setSlice(1);
			Ladj = new ListenerAdj();
			if (i1.getStackSize()>1)
			{
				sliceSelector.addAdjustmentListener(Ladj);
			}
		}
		public void mouseWheelMoved(MouseWheelEvent event) {
			synchronized(this) {
				i1.killRoi();
				int slice = imp.getCurrentSlice()
					+ event.getWheelRotation();
				if (slice < 1)
					slice = 1;
				else if (slice > imp.getStack().getSize())
					slice = imp.getStack().getSize();
				imp.setSlice(slice);
				drawCurrent();

				}
			}


		public void clearListener()
		{
			sliceSelector.removeAdjustmentListener(Ladj);
			removeMouseWheelListener(this);
		}
		class ListenerAdj implements AdjustmentListener {
			int oldSlice;

			ListenerAdj() {
			}

			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				i1.killRoi();
				if (e.getAdjustmentType() == AdjustmentEvent.TRACK )
				{
					currentSlice = i1.getCurrentSlice();
					drawCurrent();
				}
				else
				{
					ClearDraw();
				}
			}
		}
	}
}	/** End BalloonSegmentation class*/
