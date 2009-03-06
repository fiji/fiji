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
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.Checkbox;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import levelsets.algorithm.DeferredObjectArray3D;
import levelsets.algorithm.FastMarching;
import levelsets.algorithm.SparseFieldLevelSet;
import levelsets.ij.StateContainer.States;

import java.lang.reflect.Field;

public class LevelSet implements PlugInFilter {
	
	
	private int flags = DOES_16|DOES_32|DOES_8G|DOES_STACKS;
	private static String [] shapeList = {"none"}; // not implemented yet
	public final static String [] preprocessList = {"none", "Gaussian difference"};
	private static String [] expansionList = {"outside", "inside"};
	public enum preprocessChoices { none, gaussian };
	protected ImagePlus imp;
	protected ImageContainer ic = null;
	
	// Preset parameters
	// Iterations increment between showing the results
	protected int ITER_INC = 100;
	
	// Parameters (used and not used yet) TODO
	protected preprocessChoices preprocess = preprocessChoices.none;
	protected ImagePlus shapeStack = null;
	protected boolean fast_marching = true, level_sets = true;
	protected double fm_grey, fm_dist; // Fast Marching
	protected double w_adv, w_curv, w_gray, f_conv; // Active contours
	protected int fm_maxiter = 100000, ls_maxiter = 100;
	boolean ask_params = true;
	boolean insideout = false;
	
	// Test values
	static String testImageFn = "/Users/erwin/Desktop/Dot_Blot.tif";
	static int [] poly_rx = { 78, 78, 291 };
	static int [] poly_ry = { 79, 150, 69 };
	static int [] rx = { 260 };
	static int [] ry = { 180 };
	static boolean test_dialog = false, test_algorithm = true, test_roi = false;
	
	
	public LevelSet() {
		// ask_params = false;
	}
	
	
	static public void main(String args[]) {
		
		String [] ij_args = { "-Dplugins.dir=/Users/erwin/Desktop/fiji.git/20080512/fiji/plugins",
				"-Dmacros.dir=/Users/erwin/Desktop/fiji.git/20080512/fiji/macros" };
		
		
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
			ls_filter.setup("", testIP);
			ls_filter.run(testIP.getProcessor());
		}
		
	}
	

	public void run(ImageProcessor ip) {
		
		// TODO Would make sense to offer starting points as mask in separate image. 
		// If no ROI found, have the additional dialog field to select mask image. 
				
		Roi roi = imp.getRoi();
			if ( roi==null ) {
				// FastMarching needs points
					IJ.error("Seed (points) required");
			
			// TODO Active contour needs contour - 
			// 3 cases should be separate classes
			// - FastMarching
			// - ActiveContours with option of fast marching 
			return;
		}
		
		if ( ask_params == true ) {
			if ( showDialog() == false ) {
				return;
			}
		}
		 
		// Wrap the selected image into the ImageContainer
		ic = new ImageContainer(imp);
		
		// Create a ImageContainer for showing the progress
		ImageProgressContainer progressImage = null;
		if (ask_params) {
			progressImage = new ImageProgressContainer();
			progressImage.duplicateImages(ic);
			progressImage.createImagePlus("Segmentation progress of " + imp.getTitle());
			progressImage.showProgressStep();
		}
		
		// Create a initial state map out of the roi
		StateContainer sc_roi = new StateContainer();
		sc_roi.setROI(roi, ic.getWidth(), ic.getHeight(), ic.getImageCount(), imp.getCurrentSlice());
		sc_roi.setExpansionToInside(insideout);
		
		StateContainer sc_ls = null;
		StateContainer sc_final = null;
		
		int iter;
		
		IJ.showStatus("Press 'Esc' to abort");

		// Fast marching
		if ( fast_marching ) {
			FastMarching fm = new FastMarching(ic, progressImage, sc_roi, true);
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Starting Fast Marching");
			for ( iter = 0; iter < this.fm_maxiter; iter ++ ) {
				if ( fm.step(this.ITER_INC) == false ) {
					break;
				}
				if (IJ.escapePressed()) {
					IJ.log("Aborted");
					return;
				}
			}
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Finished Fast Marching");
			sc_ls = fm.getStateContainer();
			if ( sc_ls == null ) {
				// don't continue if something happened during Fast Marching
				return;
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
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Starting Level Set");
			SparseFieldLevelSet ls = new SparseFieldLevelSet(ic, progressImage, sc_ls);
			ls.setAdvectionWeight(w_adv);
			ls.setCurvatureWeight(w_curv);
			ls.setConvergenceFactor(f_conv);
			for ( iter = 0; iter < this.ls_maxiter; iter ++ ) {
				if ( ls.step(this.ITER_INC) == false ) {
					break;
				}
				if (IJ.escapePressed()) {
					IJ.log("Aborted");
					return;
				}
			}
			IJ.log("(" + new Date(System.currentTimeMillis()) + "): Finished Level Set");
			sc_final = ls.getStateContainer();
		}
		
		// Convert sc_final into binary image ImageContainer and display
		if ( sc_final == null ) {
			return;
		}
		ImageContainer result = new ImageContainer(sc_final.getIPMask());
		ImagePlus result_ip = result.createImagePlus("Segmentation of " + imp.getTitle());

		result_imp = result_ip;

		if (ask_params) {
			result_ip.show();
		}
	}

	private ImagePlus result_imp = null;

	public ImagePlus getResult() { return result_imp; }

	public int setup(String arg, ImagePlus imp) {
		// TODO: check for seed == selection
		
		this.imp = imp;
		
		// set the parameters to meaningful defaults from the class
		w_adv = SparseFieldLevelSet.getAdvectionWeight();
		w_curv = SparseFieldLevelSet.getCurvatureWeight();
		f_conv = SparseFieldLevelSet.getConvergenceFactor();
		fm_grey = FastMarching.getGreyThreshold();
		fm_dist = FastMarching.getDistanceThreshold();
		
		return flags;
	}
	
	
	public boolean showDialog() {
		// TODO interactive selection of gray value range
		
		GenericDialog gd = new GenericDialog("Level Set Segmentation");
		gd.addChoice("Preprocessing (Advection image)", preprocessList, preprocessList[0]);
		gd.addCheckbox("Use_Fast_Marching", true);
		gd.addNumericField("Grey_value threshold", FastMarching.getGreyThreshold(), 0);
		gd.addNumericField("Distance_threshold", FastMarching.getDistanceThreshold(), 2);
		gd.addCheckbox("Use_Level_Sets", true);
		//gd.addChoice("Shape_guidance_stack", shapeList, shapeList[0]);
		gd.addMessage("Level set weigths (0 = don't use)");
		gd.addNumericField("Advection", SparseFieldLevelSet.getAdvectionWeight(), 2);
		gd.addNumericField("Curvature", SparseFieldLevelSet.getCurvatureWeight(), 2);
		gd.addNumericField("Grayscale", 1, 0);
		gd.addMessage("Level set convergence criterion (be careful!)");
		gd.addNumericField("Convergence", SparseFieldLevelSet.getConvergenceFactor(), 4);
		gd.addChoice("Region_expands_to ", expansionList, expansionList[0]);
		gd.addMessage("");
		gd.addMessage("Developed by Erwin Frise.\nBased on code by Arne-Michael Toersel\n");
		
		gd.showDialog();

		if (gd.wasCanceled())
			return false;
		
		
		String preprocess_choice = gd.getNextChoice();
		if ( preprocess_choice.contentEquals(preprocessList[1])) {
			preprocess = preprocessChoices.gaussian;
		}
		//String shape = gd.getNextChoice();
		// this.shapeStack = WindowManager.getImage(stackIDs[((Choice)gd.getChoices().get(0)).getSelectedIndex()]);
		
		this.fast_marching = gd.getNextBoolean();
		this.level_sets = gd.getNextBoolean();
		
		if ( fast_marching == true ) {
			this.fm_grey = gd.getNextNumber();
			this.fm_dist = gd.getNextNumber();
		}
		if ( level_sets == true ) {
			this.w_adv = gd.getNextNumber();
			this.w_curv = gd.getNextNumber();
			this.w_gray = gd.getNextNumber();
			this.f_conv = gd.getNextNumber();
			String expansion = gd.getNextChoice();
			if ( expansion.contentEquals(expansionList[1]) ) {
				this.insideout = true;
				IJ.log("Inverse expansion");
			}
		}
		
		if ( test_dialog ) {
			IJ.log("Fast Marching enabled = " + fast_marching );
			IJ.log("Fast Marching: fm_grey=" + fm_grey + " , fm_dist=" + fm_dist);
			IJ.log("Level Sets enabled = " + level_sets );
			IJ.log("Level Sets: w_adv= " + w_adv + ", w_curv=" + w_curv + ", w_gray=" + w_gray + ", f_conv=" + f_conv);
		}
		
		
		return true;

	}

	public void printParameters() {
		for (Field field : getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				System.out.println(field.getName() + " = " + field.get(this));
			} catch (Exception e) { e.printStackTrace(); }
		}
	}

	/** Set the plugin to run without a GUI. */
	public void setParameters(ImagePlus imp, String preprocessList,
			                                 boolean fast_marching, double fm_grey, double fm_dist,
							 boolean level_sets, double w_adv, double w_curv, double w_gray, double f_conv, boolean insideout) {
		this.imp = imp;
		this.fast_marching = fast_marching;
		this.fm_grey = fm_grey;
		this.fm_dist = fm_dist;
		this.level_sets = level_sets;
		this.w_adv = w_adv;
		this.w_curv = w_curv;
		this.w_gray = w_gray;
		this.f_conv = f_conv;
		this.insideout = insideout;

		// Avoid dialog:
		this.ask_params = false;
	}

	
	public void showROI(ImagePlus ip, Roi roi, StateContainer sc_in) {

		// green coloured pixel for alive set pixel visualization
		int[] ALIVE_PIXEL = new int[] {0, 255, 0, 0};
		// red coloured pixel for trial set (band) pixel visualization
		int[] BAND_PIXEL = new int[] {255, 0, 0, 0};

		ImageProgressContainer progress = new ImageProgressContainer(new ImageContainer(ip));	
		DeferredObjectArray3D<States> map;
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

		ImageProgressContainer output = progress;
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
		if (ask_params) progress.showProgressStep();
	}
	
	
	// Verbatim from registration3d plugin, Stephan Preibisch
	private final void addEnablerListener(/*final GenericDialog gd, */final Checkbox master, final Component[] enable, final Component[] disable) 
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
