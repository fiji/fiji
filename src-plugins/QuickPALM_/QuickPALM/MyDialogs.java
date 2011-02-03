/** Main container to all dialogs within the plugin, dialogs */

package QuickPALM;

import ij.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.CurveFitter.*;
import java.awt.*;

class MyDialogs
{
	
	ImagePlus imp;
	RoiManager rmanager;
	Roi [] rois;
	
	ij.Prefs prefs = new ij.Prefs(); 
	
	java.lang.String ptablefile;
	
	int width;
	int height;
	int nslices;
	String imtitle;
	int nrois;
	
	int snr;
	double pixelsize;
	
	double fwhm;
	//int roirad;
	double pthrsh = 0.2;
	boolean smartsnr;

	int buffer;
	double cal_z;
	int window;
	double magn;

	// advanced settings
	double minsize;
	double maxsize;
	double symmetry;
	int maxpart; //maximum particles
	int threads;

	// used on calib
	java.lang.String model;
	java.lang.String [] models = new java.lang.String[5]; // used for the calibration
	boolean part_divergence;
	boolean part_extrainfo;
	
	// used on is3d
	boolean is3d;
	java.lang.String calfile;
	
	// used on view
	boolean view = true;
	int viewer_accumulate;
	int viewer_update;
	double saturation;
	
	// used on attach
	boolean attach;
	java.lang.String imagedir;
	java.lang.String pattern;
	java.lang.String prefix;
	java.lang.String sufix;
	int nimchars;
	int nimstart;
	int waittime;
	
	// Reconstruct interface vars
	double viewer_tpixelsize;
	int viewer_owidth;
	int	viewer_oheight;
	//double saturation;
		
	java.lang.String [] view_modes = new java.lang.String[4];
	java.lang.String view_mode;
	boolean viewer_doConvolve;
	//boolean viewer_doBW;
	boolean viewer_do3d;
	boolean viewer_doMovie;
	//boolean viewer_doSave;
		
	double viewer_mergeabove;
	double viewer_mergebellow;
	
	double viewer_fwhm;
	boolean viewer_is8bit;
	double viewer_zstep;
	//int viewer_update;
	//int viewer_accumulate;
	
	public boolean getImageDirectory()
	{
		DirectoryChooser chooser = new DirectoryChooser("Choose directory to process");
		imagedir = chooser.getDirectory();
		return true;
	}
	
	public boolean getCalibrationFile()
	{
		java.lang.String cal_dir = prefs.get("QuickPALM.cal_dir", ".");
		java.lang.String cal_file = prefs.get("QuickPALM.cal_file", "Astigmatism calibration.xls");
		OpenDialog chooser = new OpenDialog("Astigmatism calibration", cal_dir, cal_file);
		calfile = chooser.getDirectory()+chooser.getFileName();
		prefs.set("QuickPALM.cal_dir", chooser.getDirectory());
		prefs.set("QuickPALM.cal_file", chooser.getFileName());
		return true;
	}
	
	public boolean getParticlesTableFile()
	{
		java.lang.String ptable_dir = prefs.get("QuickPALM.ptable_dir", ".");
		java.lang.String ptable_file = prefs.get("QuickPALM.ptable_file", "Particles Table.xls");
		OpenDialog chooser = new OpenDialog("Load Particles Table", ptable_dir, ptable_file);
		ptablefile = chooser.getDirectory()+chooser.getFileName();
		prefs.set("QuickPALM.ptable_dir", chooser.getDirectory());
		prefs.set("QuickPALM.ptable_file", chooser.getFileName());
		return true;
	}
	
	public boolean checkBeads() {
		rmanager=RoiManager.getInstance();
		if (IJ.versionLessThan("1.26i"))
			return false;
		else if (rmanager==null)
		{
			IJ.error("Add bead ROIs to the RoiManager first (select region then press [t]).");
			return false;	
		}

		nrois = rmanager.getCount();
		if (nrois==0)
		{
			IJ.error("Add bead ROIs to the RoiManager first (select region then press [t]).");
			return false;
		}
		rois = rmanager.getRoisAsArray();
		return true;
	}
	
	public boolean checkDrift() {
		rmanager=RoiManager.getInstance();
		if (IJ.versionLessThan("1.26i"))
			return false;
		else if (rmanager==null)
		{
			IJ.error("Add ROIs to use in drift correction to the RoiManager first (select region then press [t]).");
			return false;	
		}
		nrois = rmanager.getCount();
		if (nrois==0)
		{
			IJ.error("Add ROIs to use in drift correction to the RoiManager first (select region then press [t]).");
			return false;
		}
		rois = rmanager.getRoisAsArray();
		return true;
	}
	
		
	public boolean checkResolution() {
		rmanager=RoiManager.getInstance();
		if (IJ.versionLessThan("1.26i"))
			return false;
		else if (rmanager==null)
		{
			IJ.error("Add ROIs to use in resolution estimation to the RoiManager first (select region then press [t]).");
			return false;	
		}
		nrois = rmanager.getCount();
		if (nrois==0)
		{
			IJ.error("Add ROIs to use in resolution estimation to the RoiManager first (select region then press [t]).");
			return false;
		}
		rois = rmanager.getRoisAsArray();
		return true;
	}

	public boolean beadCalibration3d() {
		imp = IJ.getImage();
		if (imp==null)
		{
            IJ.noImage();
            return false;
		}
		else if (imp.getStackSize() == 1) 
		{
            IJ.error("Stack required");
            return false;
		} 
		else if (imp.getType() != ImagePlus.GRAY8 && imp.getType() != ImagePlus.GRAY16 ) 
		{
            // In order to support 32bit images, pict[] must be changed to float[], and  getPixel(x, y); requires a Float.intBitsToFloat() conversion
            IJ.error("8 or 16 bit greyscale image required");
            return false;
		}
		width=imp.getWidth();
		height=imp.getHeight();
		nslices=imp.getStackSize();
		imtitle = imp.getTitle();
		
		models[0]="*None*";
		models[1]="line";
		models[2]="2nd degree polynomial";
		models[3]="3rd degree polynomial";
		models[4]="4th degree polynomial";

		GenericDialog gd = new GenericDialog("3D PALM calibration");
		gd.addNumericField("Maximum FWHM (in px)", prefs.get("QuickPALM.3Dcal_fwhm", 20), 0);
		gd.addNumericField("Particle local threshold (% maximum intensity)", prefs.get("QuickPALM.pthrsh", 20), 0);
       	gd.addNumericField("Z-spacing (nm)", prefs.get("QuickPALM.z-step", 10), 2);
		gd.addNumericField("Calibration Z-smoothing (radius)", prefs.get("QuickPALM.window", 1), 0);
		gd.addChoice("Model", models, prefs.get("QuickPALM.model", models[3]));
		gd.addCheckbox("Show divergence of bead positions against model", prefs.get("QuickPALM.3Dcal_showDivergence", false));
		gd.addCheckbox("Show extra particle info", prefs.get("QuickPALM.3Dcal_showExtraInfo", false));
		gd.addMessage("\n\nDon't forget to save the table in the end...");
		gd.showDialog();
		if (gd.wasCanceled())
            return false;
		fwhm = gd.getNextNumber();
		prefs.set("QuickPALM.QuickPALM.3Dcal_fwhm", fwhm);
		pthrsh = gd.getNextNumber()/100;
		prefs.set("QuickPALM.pthrsh", pthrsh*100);
		cal_z = gd.getNextNumber();
		prefs.set("QuickPALM.z-step", cal_z);
		window = (int) gd.getNextNumber();
		prefs.set("QuickPALM.window", window);
		model = gd.getNextChoice();
		prefs.set("QuickPALM.model", model);
		part_divergence = gd.getNextBoolean();
		prefs.set("QuickPALM.3Dcal_showDivergence", part_divergence);
		part_extrainfo = gd.getNextBoolean();
		prefs.set("QuickPALM.3Dcal_showExtraInfo", part_extrainfo);
		return true;
	}
	
	public boolean reconstructDataset() {
		
		view_modes[0]="3D color";
		view_modes[1]="2D histogram";
		view_modes[2]="2D particle intensity (16-bit)";
		view_modes[3]="2D particle intensity (8-bit)";
		
		GenericDialog gd = new GenericDialog("Reconstruct PALM/STORM Dataset");
		gd.addNumericField("Target pixel size for the rendered image (nm)", prefs.get("QuickPALM.viewer_tpixelsize", 30), 2);
		gd.addNumericField("Original image width (px)", prefs.get("QuickPALM.viewer_owidth", 512), 2);
		gd.addNumericField("Original image height (px)", prefs.get("QuickPALM.viewer_oheight", 512), 2);
		gd.addChoice("View mode", view_modes, prefs.get("QuickPALM.view_mode", view_modes[1]));
		//gd.addNumericField("Allow image saturation (%)", prefs.get("QuickPALM.saturation", 50), 0);
		
		gd.addCheckbox("Simulate sub-difraction spot (gaussian convolution - only 2D)", prefs.get("QuickPALM.viewer_doConvolve", true));
		//gd.addCheckbox("Make 3D stack", prefs.get("QuickPALM.viewer_do3d", false));
		//gd.addCheckbox("Make movie", prefs.get("QuickPALM.viewer_doMovie", false));
		gd.addCheckbox("Make 3D stack", false);
		gd.addCheckbox("Make movie", false);
		//gd.addCheckbox("Save only and don't show", prefs.get("QuickPALM.viewer_doSave", false));
		gd.addMessage("\n");
		
		// -----------------------------------------
		gd.addMessage("-- Simulate sub-difraction spot settings (used only if selected) --");
		gd.addNumericField("FWHM of the spot", prefs.get("QuickPALM.viewer_fwhm", 30), 2);
		gd.addMessage("\n");
		
		// -----------------------------------------
		gd.addMessage("-- Make 3D stack settings (used only if selected) --");
		gd.addNumericField("Z-spacing between slices (nm)", prefs.get("QuickPALM.viewer_zstep", 50), 2);
		gd.addNumericField("Merge particle Z-position above (nm - 0 for full Z range)", prefs.get("QuickPALM.viewer_mergeabove", 0), 2);
		gd.addNumericField("Merge particle Z-position bellow (nm - 0 for full Z range)",prefs.get("QuickPALM.viewer_mergebellow", 0), 2);
		gd.addMessage("\n");
		
		// -----------------------------------------
		gd.addMessage("-- Make movie settings (used only if selected) --");
		gd.addNumericField("Make a reconstruction in every N frames", prefs.get("QuickPALM.viewer_update", 10), 0);
		gd.addNumericField("Accumulate N neighboring frames for each reconstruction\n(set to 0 to accumulate all the preceding frames)", prefs.get("QuickPALM.viewer_accumulate", 100), 0);
	
		gd.showDialog();
		if (gd.wasCanceled())
            return false;
		
		viewer_tpixelsize = gd.getNextNumber();
		prefs.set("QuickPALM.viewer_tpixelsize", viewer_tpixelsize );
		viewer_owidth = (int) gd.getNextNumber();
		prefs.set("QuickPALM.viewer_owidth", viewer_owidth);
		viewer_oheight = (int) gd.getNextNumber();
		prefs.set("QuickPALM.viewer_oheight", viewer_oheight);
		view_mode = gd.getNextChoice();
		prefs.set("QuickPALM.view_mode", view_mode);
		
		viewer_doConvolve = gd.getNextBoolean();
		prefs.set("QuickPALM.viewer_doConvolve", viewer_doConvolve);
		viewer_do3d = gd.getNextBoolean();
		prefs.set("QuickPALM.viewer_do3d", viewer_do3d);
		viewer_doMovie = gd.getNextBoolean();
		prefs.set("QuickPALM.viewer_doMovie", viewer_doMovie);
		//viewer_doSave = gd.getNextBoolean();
		//prefs.set("QuickPALM.viewer_doSave", viewer_doSave);
		
		// -- Simulate sub-difraction spot
		viewer_fwhm = gd.getNextNumber();
		prefs.set("QuickPALM.viewer_fwhm", viewer_fwhm);
		
		// -- Show B&W
		//viewer_is8bit = gd.getNextBoolean();
		//prefs.set("QuickPALM.viewer_is8bit", viewer_is8bit);
		
		// -- Make 3D stack
		viewer_zstep = gd.getNextNumber();
		prefs.set("QuickPALM.viewer_zstep", viewer_zstep);
		viewer_mergeabove = gd.getNextNumber();
		prefs.set("QuickPALM.viewer_mergeabove", viewer_mergeabove);
		viewer_mergebellow = gd.getNextNumber();
		prefs.set("QuickPALM.viewer_mergebellow", viewer_mergebellow);
		
		// -- Make Movie
		viewer_update = (int) gd.getNextNumber();
		prefs.set("QuickPALM.viewer_update", viewer_update);
		viewer_accumulate = (int) gd.getNextNumber();
		prefs.set("QuickPALM.viewer_accumulate", viewer_accumulate);
		
		return true;
	}

	public boolean analyseParticles(MyFunctions f) {	
		GenericDialog gd = new GenericDialog("Analyse PALM/STORM Particles");
		gd.addNumericField("Minimum SNR", prefs.get("QuickPALM.snr", 5), 2);
		gd.addNumericField("Maximum FWHM (in px)", prefs.get("QuickPALM.fwhm", 4), 0);
		gd.addNumericField("Image plane pixel size (nm)", prefs.get("QuickPALM.pixelsize", 106), 2);
		gd.addCheckbox("Smart SNR", prefs.get("QuickPALM.smartsnr", true));
		gd.addCheckbox("3D PALM (astigmatism) - will require calibration file", prefs.get("QuickPALM.is3d", false));
		gd.addCheckbox("Online rendering", prefs.get("QuickPALM.view", true));
		gd.addCheckbox("Attach to running acquisition", prefs.get("QuickPALM.attach", false));
		gd.addCheckbox("Stream particle info directly into file", prefs.get("QuickPALM.stream", true));
		gd.addMessage("\n");
		// -----------------------------------------
		gd.addMessage("-- Online rendering settings (used only if selected) --");
		gd.addMessage("\n");
		gd.addNumericField("Pixel size of rendered image (nm)", 30, 2);
		gd.addNumericField("Accumulate last (0 to accumulate all frames)", 0, 0);
		gd.addNumericField("Update every (frames)", 10, 0);
		//gd.addNumericField("Allow color saturation (%)", 50, 0);
		gd.addMessage("\n");
		// -----------------------------------------
		gd.addMessage("-- Attach to running acquisition settings (used only if selected) --");
		gd.addMessage("\n");
		gd.addStringField("_Image name pattern (NN...NN represents the numerical change)", prefs.get("QuickPALM.pattern", "imgNNNNNNNNN.tif"), 20);
		gd.addNumericField("Start NN...NN with", 0, 0);
		gd.addNumericField("In acquisition max. wait time for new image (ms)", 50, 0);
		gd.addMessage("\n");
		// -----------------------------------------
		gd.addMessage("-- Advanced settings (don't normally need to be changed) --");
		gd.addMessage("\n");
		gd.addNumericField("_Minimum symmetry (%)", prefs.get("QuickPALM.symmetry", 50), 0);
		gd.addNumericField("Local threshold (% maximum intensity)", prefs.get("QuickPALM.lthreshold", 20), 0);
		gd.addNumericField("_Maximum iterations per frame", prefs.get("QuickPALM.maxiter", 1000), 0);
		gd.addNumericField("Threads (each takes ~3*[frame size] in memory)", prefs.get("QuickPALM.nthreads", 50), 0);
		gd.addMessage("\n\nDon't forget to save the table in the end...");
		
		gd.showDialog();
		if (gd.wasCanceled())
            return false;

		snr = (int) gd.getNextNumber();
		prefs.set("QuickPALM.snr", snr);
		fwhm = gd.getNextNumber();
		prefs.set("QuickPALM.fwhm", fwhm);
		pixelsize = gd.getNextNumber();
		prefs.set("QuickPALM.pixelsize", pixelsize);
		
		smartsnr = gd.getNextBoolean();
		prefs.set("QuickPALM.smartsnr", smartsnr);
		is3d = gd.getNextBoolean();
		prefs.set("QuickPALM.is3d", is3d);
		view = gd.getNextBoolean();
		prefs.set("QuickPALM.view", view);
		attach = gd.getNextBoolean();
		prefs.set("QuickPALM.attach", attach);
		
		if (gd.getNextBoolean())
		{
			f.psave = new ParticleSaver();
			f.psave.setup();
			prefs.set("QuickPALM.stream", true);
		}
		else prefs.set("QuickPALM.stream", false);
		//--
		
		magn = pixelsize/gd.getNextNumber();
		viewer_accumulate = (int) gd.getNextNumber();
		viewer_update = (int) gd.getNextNumber();
		
		//--
		pattern = gd.getNextString().trim();
		prefs.set("QuickPALM.pattern", pattern);
		prefix = pattern.substring(0, pattern.indexOf("N"));
		sufix = pattern.substring(pattern.lastIndexOf("N")+1, pattern.length());
		nimchars = pattern.split("N").length-1;
		nimstart = (int) gd.getNextNumber();
		waittime = (int) gd.getNextNumber();
		
		//--
		
		symmetry = gd.getNextNumber()/100;
		prefs.set("QuickPALM.symmetry", symmetry);
		pthrsh = gd.getNextNumber()/100;
		prefs.set("QuickPALM.lthreshold", pthrsh*100);
		maxpart = (int) gd.getNextNumber();
		prefs.set("QuickPALM.maxiter", maxpart);
		threads = (int) gd.getNextNumber();
		prefs.set("QuickPALM.nthreads", threads);
		
		return true;
	}
}