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

public class Save_particles_table2img implements PlugIn {
    	
    MyDialogs dg = new MyDialogs();
	MyFunctions f = new MyFunctions();
	MyIO io = new MyIO();
	ParticleSaver psaver = new ParticleSaver();
	
	ImagePlus imp;
	ImageProcessor ip;
	
	public void run(String arg) {
				
        SaveDialog sd = new SaveDialog("File to save particles into", "Particles Table", ".tif");
        String path = sd.getDirectory();
        String filename = path+sd.getFileName();
		
		double [] s = 		f.ptable.getColumnAsDoubles(0);
		double [] x = 		f.ptable.getColumnAsDoubles(1);
		double [] y = 		f.ptable.getColumnAsDoubles(2);
		double [] x_ =		f.ptable.getColumnAsDoubles(3);
		double [] y_ =		f.ptable.getColumnAsDoubles(4);
		double [] z_ = 		f.ptable.getColumnAsDoubles(5);
		double [] left = 	f.ptable.getColumnAsDoubles(6);
		double [] right = 	f.ptable.getColumnAsDoubles(7);
		double [] up = 		f.ptable.getColumnAsDoubles(8);
		double [] down = 	f.ptable.getColumnAsDoubles(9);
		double [] xsym = 	f.ptable.getColumnAsDoubles(10);
		double [] ysym = 	f.ptable.getColumnAsDoubles(11);
		double [] wmh = 	f.ptable.getColumnAsDoubles(12);
		double [] frame = 	f.ptable.getColumnAsDoubles(13);
		
		ip = new FloatProcessor(14, s.length);
		imp = new ImagePlus("Particles Table", ip);
		
		IJ.showStatus("Generating Particles Table Image...");
		for (int n=0; n<s.length; n++)
		{
			IJ.showProgress(n, s.length);
			ip.setf(0, n, (float) s[n]);
			ip.setf(1, n, (float) x[n]);
			ip.setf(2, n, (float) y[n]);
			ip.setf(3, n, (float) x_[n]/1000);
			ip.setf(4, n, (float) y_[n]/1000);
			ip.setf(5, n, (float) z_[n]/1000);
			ip.setf(6, n, (float) left[n]);
			ip.setf(7, n, (float) right[n]);
			ip.setf(8, n, (float) up[n]);
			ip.setf(9, n, (float) down[n]);
			ip.setf(10, n, (float) xsym[n]);
			ip.setf(11, n, (float) ysym[n]);
			ip.setf(12, n, (float) wmh[n]);
			ip.setf(13, n, (float) frame[n]/1000000);
		}
		IJ.showStatus("Saving Particles Table Image...");
		IJ.save(imp, filename);
		imp.close();
    }
}