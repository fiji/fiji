/**
 * <p>Title: LevelSets</p>
 *
 * <p>Description: Level Sets Plugin. Executes a standard Level Set algorithm. This plugin is basde on the code by Arne-Michael Toersel
 * and from his diploma thesis, extended and adjusted to ImageJ by Erwin Frise.</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: The Hackathon 2008 @ Janelia Farm and previous work by Arne-Michael Toersel</p>
 *
 * <p>License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Arne-Michael Toersel and Erwin Frise
 * @version 1.0
 */

package levelsets.ij;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;
import java.util.EnumMap;
import java.util.Iterator;

import levelsets.algorithm.DeferredObjectArray3D;
import levelsets.algorithm.FastMarching;
import levelsets.algorithm.LevelSetFactory;
import levelsets.algorithm.LevelSetImplementation;
import levelsets.algorithm.LevelSetFactory.Parameter;
import levelsets.ij.StateContainer.States;

public class LevelSet implements PlugIn {
	

	private static String [] shapeList = {"none"}; // not implemented yet
	private static String [] preprocessList = {"none", "Gaussian difference"};
	public enum preprocessChoices { none, gaussian };

	private static boolean fast_marching = true, level_sets = true;
	private static String [] levelsetList ;
	private static int ls_choice = 0;
	private static String [] expansionList = {"outside", "inside"};
	private static short expansion_choice = 0;
	static boolean insideout = false;

	protected ImagePlus imp;
	protected ImageContainer ic = null;
	protected static LevelSetFactory lf;
	
	// Preset parameters
	// Iterations increment between showing the results
	protected int ITER_INC = 100;
	
	// Parameters (used and not used yet)
	protected static double fm_dist = -1; // Fast Marching
	protected static int fm_grey = -1; // Fast Marching
	protected EnumMap<Parameter,String> parameters;
	
	protected preprocessChoices preprocess = preprocessChoices.none;
	protected ImagePlus shapeStack = null;
	protected int fm_maxiter = 100000, ls_maxiter = 100;
	protected boolean ask_params = true;
	
	// Test values
	private static String testImageFn = "/Users/erwin/Desktop/fiji_git/Dot_Blot.tif";
	private static int [] poly_rx = { 78, 78, 291 };
	private static int [] poly_ry = { 79, 150, 69 };
	private static int [] rx = { 260 };
	private static int [] ry = { 180 };
	private static boolean test_dialog = false, test_algorithm = false, test_roi = false;
	
	
	public LevelSet() {
		// ask_params = false;
		if (lf == null ) {
			lf = new LevelSetFactory();
			levelsetList = lf.getImplementationNames();  
			lf.resetParameters(levelsetList[ls_choice]);
		}
		parameters = lf.getParameters();
		
		if ( fm_grey < 0 ) {
			fm_grey = FastMarching.getGreyThreshold();
			fm_dist = FastMarching.getDistanceThreshold();
		}

	}
	
	
	static public void main(String args[]) {
		
		String [] ij_args = { "-Dplugins.dir=/Users/erwin/Desktop/fiji_git/fiji/plugins",
				"-Dmacros.dir=/Users/erwin/Desktop/fiji_git/fiji/macros" };
		
		
//		ImageJ ij = new ImageJ();
//		ij.main(ij_args);
//		ij.run();
		
		ij.ImageJ.main(ij_args);
				
		ImagePlus testIP = new ImagePlus(testImageFn);
		testIP.show();
		
		PointRoi pr = new PointRoi(rx[0], ry[0], testIP);
		if ( rx.length > 1 ) {
			for ( int i=1; i < rx.length; i++ ) {
				pr.addPoint(rx[1], ry[1]);
				pr.addPoint(rx[2], ry[2]);
			}
		}
		testIP.setRoi(pr);
		
		
		LevelSet ls_filter = new LevelSet();
		if ( test_dialog ) {
			ls_filter.showDialog();
		}
		if ( test_roi ) {
			ls_filter.showROI(testIP, new Roi(10, 10, 20, 30), null);
			ls_filter.showROI(testIP, new PolygonRoi(poly_rx, poly_ry, poly_rx.length, PolygonRoi.POLYGON), null);
		}		
		if ( test_algorithm ) {
			ls_filter.imp = testIP;
			ls_filter.run("");
		}
		
	}

	public void run(final ImagePlus imp) {
		ImagePlus result = execute(imp, true);
		if (null != result) result.show();
	}

	/**
	 * @param imp The ImagePlus to operate on
	 * @param with_progress Whether to show an image that tracks progress
	 */
	public ImagePlus execute(ImagePlus imp, boolean with_progress) {
		
		// TODO Would make sense to offer starting points as mask in separate image. 
		// If no ROI found, have the additional dialog field to select mask image. 

		Roi roi = imp.getRoi();
        	if ( roi==null ) {
        		// FastMarching needs points

			// TODO Active contour needs contour - 
			// 3 cases should be separate classes
			// - FastMarching
			// - ActiveContours with option of fast marching 
			return null;
        	}
        
        	// Wrap the selected image into the ImageContainer
		ic = new ImageContainer(imp);
		
		// Create a ImageContainer for showing the progress
		ImageProgressContainer progressImage = with_progress ? new ImageProgressContainer() : null;
		if (null != progressImage) {
			progressImage.duplicateImages(ic);
			progressImage.createImagePlus("Segmentation progress of " + imp.getTitle());
			progressImage.showProgressStep();
		}
		
		// Create a initial state map out of the roi
		StateContainer sc_roi = new StateContainer();
		System.out.println("current slice: " + imp.getCurrentSlice());
		sc_roi.setROI(roi, ic.getWidth(), ic.getHeight(), ic.getImageCount(), imp.getCurrentSlice());
		sc_roi.setExpansionToInside(insideout);
		
		StateContainer sc_ls = null;
		StateContainer sc_final = null;
		
		int iter;
		
		IJ.showStatus("Press 'Esc' to abort");
		
		try {
			// Fast marching
			if ( fast_marching ) {
				final FastMarching fm = new FastMarching(ic, progressImage, sc_roi, true, (int) fm_grey, fm_dist);
				IJ.log("Fast Marching: Starting " + new Date(System.currentTimeMillis()));
				for ( iter = 0; iter < this.fm_maxiter; iter ++ ) {
					if ( fm.step(this.ITER_INC) == false ) {
						break;
					}
					if (IJ.escapePressed()) {
						IJ.log("Aborted");
						return null;
					}
				}
				IJ.log("Fast Marching: Finished " + new Date(System.currentTimeMillis()));
				sc_ls = fm.getStateContainer();
				if ( sc_ls == null ) {
					// don't continue if something happened during Fast Marching
					return null;
				}
				sc_ls.setExpansionToInside(insideout);
				sc_final = sc_ls;
			}
			else {
				sc_ls = sc_roi;
				// showROI(imp, null, sc_ls);
				// if ( sc_ls != null ) return;
			}
			
			// Level set
			if ( level_sets ) {
				IJ.log("Level Set (" + levelsetList[ls_choice] +"): Starting " + new Date(System.currentTimeMillis()));
				IJ.log("Note: Each iteration step is " + ITER_INC + " iterations");

				LevelSetImplementation ls = lf.getImplementation(levelsetList[ls_choice], ic, progressImage, sc_ls);

				for ( iter = 0; iter < this.ls_maxiter; iter ++ ) {
					if ( ls.step(this.ITER_INC) == false ) {
						break;
					}
					if (IJ.escapePressed()) {
						IJ.log("Aborted");
						return null;
					}
				}
				IJ.log("Level Set: Finished " + new Date(System.currentTimeMillis()));
				sc_final = ls.getStateContainer();
			}
			
			// Convert sc_final into binary image ImageContainer and display
			if ( sc_final == null ) {
				return null;
			}
			ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
			for (ImageProcessor bp : sc_final.getIPMask()) {
				stack.addSlice(null, bp);
			}
			ImagePlus seg = imp.createImagePlus();
			seg.setStack("Segmentation of " + imp.getTitle(), stack);
			seg.setSlice(imp.getCurrentSlice());
			return seg;
		} catch (IllegalArgumentException e) {
			// Usually happens with ROI specifications
			IJ.error(e.getMessage());
		} catch (ArithmeticException e) {
			// Numerical instability
			IJ.error("Arithmetic problem: " + e.getMessage());			
		}

		return null;
	}

	public void run(String arg) {
		// TODO: check for seed == selection
		
		if (null == imp) {
			imp = WindowManager.getCurrentImage();
			if (null == imp) {
				IJ.showMessage("Level Sets needs an image with a ROI in it!");
				return;
			}
		}

		Roi roi = imp.getRoi();
		if (null == roi) {
			IJ.error("Seed (points) required");
			return;
		}

		if ( ask_params == true ) {
			if ( showDialog() == false ) {
				return;
			}
		}

		// if never called before set the parameters to meaningful defaults from the class
		// otherwise keep them at previous values
		
		// FastMarching
		if ( fm_grey < 0 ) {
			fm_grey = FastMarching.getGreyThreshold();
			fm_dist = FastMarching.getDistanceThreshold();
		}

		
		run(imp);
	}
	
	
	public boolean showDialog() {
		// TODO interactive selection of gray value range
		

		GenericDialog gd = new GenericDialog("Level Set Segmentation");
		gd.addCheckbox("Use Fast Marching", fast_marching);
		gd.addNumericField("Grey value threshold", fm_grey, 0);
		gd.addNumericField("Distance threshold", fm_dist, 2);
		gd.addCheckbox("Use Level Sets", level_sets);
		gd.addChoice("Method", levelsetList, levelsetList[0]);
		gd.addMessage("(Not all parameters used in all methods)");
		gd.addMessage("Level set weigths (0 = don't use)");

		for ( Iterator<Parameter> it = parameters.keySet().iterator(); it.hasNext(); ) {
			Parameter pn = it.next();
			Object pv = lf.getParameterValue(pn);
			if ( pv == null ) {
				gd.addNumericField(parameters.get(pn), 0, 2);
			}
			if ( pv instanceof Double ) {
				gd.addNumericField(parameters.get(pn), ((Double) lf.getParameterValue(pn)).doubleValue(), 2);
			}
		}

		// not yet implemented
		// gd.addChoice("Shape guidance stack", shapeList, shapeList[0]);
		gd.addMessage("Leve set convergence criterion");
		gd.addNumericField("Convergence", ((Double) lf.getParameterValue(Parameter.CONVERGENCE)).doubleValue(), 4);
		gd.addChoice("Region expands to ", expansionList, expansionList[expansion_choice]);
		gd.addMessage("");
		gd.addMessage("Developed by Erwin Frise.\nBased on code by Arne-Michael Toersel\n");
		
		gd.showDialog();

		if (gd.wasCanceled())
			return false;
		

		String method = gd.getNextChoice();
		for (int i=0; i < levelsetList.length; i++) {
			if ( method.compareTo(levelsetList[i]) == 0 ) {
				ls_choice = i;
				break;
			}
		}
		
		// String shape = gd.getNextChoice();
		// this.shapeStack = WindowManager.getImage(stackIDs[((Choice)gd.getChoices().get(0)).getSelectedIndex()]);
		
		this.fast_marching = gd.getNextBoolean();
		this.level_sets = gd.getNextBoolean();
		
		this.fm_grey = (int) gd.getNextNumber();
		this.fm_dist = gd.getNextNumber();
		
		for ( Iterator<Parameter> it = parameters.keySet().iterator(); it.hasNext(); ) {
			Parameter pn = it.next();
			Object pv = lf.getParameterValue(pn);
			lf.setParameterValue(pn, new Double(gd.getNextNumber()));
		}
		
		lf.setParameterValue(Parameter.CONVERGENCE, new Double(gd.getNextNumber()));
		String expansion = gd.getNextChoice();
		if ( expansion.contentEquals(expansionList[1]) ) {
			this.insideout = true;
			this.expansion_choice = 1;
		} else {
			this.insideout = false;
			this.expansion_choice = 0;			
		}
		
		if ( test_dialog ) {
			IJ.log("Convergence to " + lf.getParameterValue(Parameter.CONVERGENCE));
			IJ.log("Fast Marching enabled = " + fast_marching );
			IJ.log("Fast Marching: fm_grey=" + fm_grey + " , fm_dist=" + fm_dist);
			IJ.log("Level Sets enabled = " + level_sets );
			IJ.log("Level Sets: w_adv= " + lf.getParameterValue(Parameter.W_ADVECTION) + 
					", w_curv=" + lf.getParameterValue(Parameter.W_CURVATURE) + 
					", w_gray=" + lf.getParameterValue(Parameter.TOL_GRAYSCALE) + 
					", f_conv=" + lf.getParameterValue(Parameter.CONVERGENCE));
			if ( this.insideout == true ) {
				IJ.log("Inverse expansion");
			}
		}
		
		
		return true;

	}
	
	
	public void showROI(ImagePlus ip, Roi roi, StateContainer sc_in) {

		// green coloured pixel for alive set pixel visualization
		final int[] ALIVE_PIXEL = new int[] {0, 255, 0, 0};
		// red coloured pixel for trial set (band) pixel visualization
		final int[] BAND_PIXEL = new int[] {255, 0, 0, 0};

		final ImageProgressContainer progress = new ImageProgressContainer(new ImageContainer(ip));	
		final DeferredObjectArray3D<States> map;
		StateContainer sc_test = null;
		
		if ( sc_in != null ) {
			sc_test = sc_in;
		}		
		if ( roi != null ) {
			sc_test = new StateContainer();
			sc_test.setROI(roi, progress.getWidth(), progress.getHeight(), progress.getImageCount(), 1);
		}
		if ( sc_test == null) {
			IJ.error("sc_test not set");
			return;
		}
		
		map = sc_test.getForSparseField();

		
		int px_alive=0, px_band=0, px_far=0;

		final ImageProgressContainer output = progress;
		if (ask_params) progress.showProgressStep();
		StateContainer.States cell_state = StateContainer.States.OUTSIDE;
		for (int z = 0; z < map.getZLength(); z++)
		{
			for (int x = 0; x < map.getXLength(); x++)
			{
				for (int y = 0; y < map.getYLength(); y++)
				{
					cell_state = map.get(x, y, z);
					if (cell_state == StateContainer.States.INSIDE)
					{
						output.setPixel(x, y, z, ALIVE_PIXEL);
						px_alive++;
					}
					else if (cell_state == StateContainer.States.ZERO)
					{
						output.setPixel(x, y, z, BAND_PIXEL);
						px_band++;
					}
					else {
						px_far++;
					}
				}
			}
		}
		progress.showProgressStep();
	}
	
	
	// Verbatim from registration3d plugin, Stephan Preibisch
	private final void addEnablerListener(final Checkbox master, final Component[] enable, final Component[] disable) 
	{
		master.addItemListener(new ItemListener() 
		{
			public void itemStateChanged(ItemEvent ie) 
			{
				if (ie.getStateChange() == ItemEvent.SELECTED) 
				{
					process(enable, true);
					process(disable, false);
				} else {
					process(enable, false);
					process(disable, true);
				}
			}

			private void process(final Component[] c, final boolean state) 
			{				
				if (null == c)
					return;
				for (int i = 0; i < c.length; i++)
				{
					c[i].setEnabled(state);
					//c[i].setVisible(state);
				}
				//gd.pack();
			}
		});
	}

}
