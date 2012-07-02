package QuickPALM;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import ij.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.CurveFitter.*;
import java.awt.*;
import ij.io.*;

public class Load_particles_tableFromImg implements PlugIn {
    	
    MyDialogs dg = new MyDialogs();
	MyFunctions f = new MyFunctions();
	MyIO io = new MyIO();
	ParticleSaver psaver = new ParticleSaver();
	
	ImagePlus imp;
	ImageProcessor ip;
	
	public void run(String arg) {
		
		imp = IJ.openImage();
		ip = imp.getProcessor();
		f.ptable.reset();
				
		IJ.showStatus("Loading Particles Table...");
		int nParticles = ip.getHeight();
		for (int n=0;n<nParticles;n++)
		{
			IJ.showProgress(n, nParticles);
			f.ptable.incrementCounter();
			f.ptable.addValue("Intensity",            (float) ip.getf(0,n));
			f.ptable.addValue("X (px)",               (float) ip.getf(1,n));
			f.ptable.addValue("Y (px)",               (float) ip.getf(2,n));
			f.ptable.addValue("X (nm)",               (float) ip.getf(3,n)*1000);
			f.ptable.addValue("Y (nm)",               (float) ip.getf(4,n)*1000);
			f.ptable.addValue("Z (nm)",               (float) ip.getf(5,n)*1000);
			f.ptable.addValue("Left-StdDev (px)",     (float) ip.getf(6,n));
			f.ptable.addValue("Right-StdDev (px)",    (float) ip.getf(7,n));
			f.ptable.addValue("Up-StdDev (px)",       (float) ip.getf(8,n));
			f.ptable.addValue("Down-StdDev (px)",     (float) ip.getf(9,n));			
			f.ptable.addValue("X Symmetry (%)",       (float) ip.getf(10,n));
			f.ptable.addValue("Y Symmetry (%)",       (float) ip.getf(11,n));
			f.ptable.addValue("Width minus Height (px)", (float) ip.getf(12,n));
			f.ptable.addValue("Frame Number",         (float) ip.getf(13,n)*1000000);
		}
		imp.close();
		if (f.ptable.getCounter()<5000000)
		{
            IJ.showStatus("Creating particle table, this should take a few seconds...");
            f.ptable.show("Results");
        }
        else
            IJ.showMessage("Warning", "Results table has too many particles, they will not be shown but the data still exists within it\nyou can still use all the plugin functionality or save table changes though the 'Save Particle Table' command.");
    }
}