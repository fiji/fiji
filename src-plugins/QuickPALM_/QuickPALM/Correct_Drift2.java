// We correct the drift by minimizing particle track displacement between frames


package QuickPALM;

import ij.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.CurveFitter.*;
import java.awt.*;
import java.lang.*;

public class Correct_Drift2 implements PlugIn 
{
	ImagePlus imp;
	
	MyDialogs dg = new MyDialogs();
	MyFunctions fn = new MyFunctions();
	MyIO io = new MyIO();

	public void run(String arg) 
	{
		IJ.register(Correct_Drift.class);
		if (!dg.checkDrift()) return;
		
		imp = IJ.getImage();
		if (imp==null)
		{
            IJ.noImage();
            IJ.error("Need a reconstruction image opened");
			return;
		}
		
		if (fn.ptable.getCounter()==0 || !fn.ptable.columnExists(12))
		{
			IJ.error("Not able to detect a valid 'Particles Table', please load one");
			return;
		}
		
		double [] s = fn.ptable.getColumnAsDoubles(0);
		double [] x = fn.ptable.getColumnAsDoubles(1);
		double [] y = fn.ptable.getColumnAsDoubles(2);
		double [] x_ = fn.ptable.getColumnAsDoubles(3);
		double [] y_ = fn.ptable.getColumnAsDoubles(4);
		double [] z_ = fn.ptable.getColumnAsDoubles(5);	
		double [] frame = fn.ptable.getColumnAsDoubles(13);
		
		double pixelsize = fn.ptable.getColumnAsDoubles(3)[0]/x[0];
		double impcal = imp.getCalibration().getX(1);
		double magn = pixelsize/impcal;
		if (impcal==1)
		{
			IJ.error("Image does not seam to be calibrated, are you sure it's a reconstruction?");
			return;
		}		
		int nframes = 0;
		for (int n=0;n<frame.length;n++)
			if (frame[n]>nframes) nframes=(int) frame[n];
			
		long nres = fn.ptable.getCounter();
		
		double change = 99999;
		double lastchange = 0;
		double [] xdrft = new double [nframes];
		double [] ydrft = new double [nframes];
		double [] zdrft = new double [nframes];
		double [] xydrft_ = new double [nframes];
		int counter = 0;
		double err = 999;
		double smoothness = ij.IJ.getNumber("Smoothness for drift estimation (%)", 90)/100;
		
		while (counter<1000 && err>0.0001)
		{
			lastchange = change;
			change = calculate_drift(s, x, y, z_, frame, nframes, nres, magn, smoothness,
								     xdrft, ydrft, zdrft);
			err = Math.abs(change-lastchange)/(change+lastchange);
			counter++;
			IJ.showStatus("Estimating drift, iteration "+counter+" with error "+err);
			//ij.IJ.log(""+err);
		}
		
		double [] frameseq = new double [nframes];
		for (int f=0; f<nframes; f++)
			frameseq[f]=f+1;
		
		for (int n=0; n<nframes; n++)
			xydrft_[n]=Math.sqrt(Math.pow(xdrft[n], 2)+Math.pow(ydrft[n], 2))*pixelsize;
		
		Plot xyplot = new Plot("Drift XY", "Frame number", "Drift (nm)", frameseq, xydrft_);
		xyplot.setColor(java.awt.Color.BLACK);
		xyplot.show();
		
		Plot zplot = new Plot("Drift Z", "Frame number", "Drift (nm)", frameseq, zdrft);
		zplot.show();
		
		//Replace values in table
		IJ.showStatus("Replacing values in table for drift corrected... this should take a few seconds.");
		
		int f;
		for (int n=0; n<frame.length;n++)
		{
			f=(int) frame[n]-1;
			fn.ptable.setValue(1, n, x[n]+xdrft[f]); // x (px)
			fn.ptable.setValue(2, n, y[n]+ydrft[f]); // y (px)
			fn.ptable.setValue(3, n, x_[n]+xdrft[f]*pixelsize); // x (nm)
			fn.ptable.setValue(4, n, y_[n]+ydrft[f]*pixelsize); // y (nm)
			fn.ptable.setValue(5, n, z_[n]+zdrft[f]); // z (nm) 
		}
		if (fn.ptable.getCounter()<5000000)
			fn.ptable.show("Results");
		IJ.showStatus("Remake a reconstruction to see improvements.");
	}
	
	private double calculate_drift(double [] s, double [] x, double [] y, double [] z, double [] frame,
								   int nframes, long nres, double magn, double smoothness,
								   double [] xdrift, double [] ydrift, double [] zdrift)
	// used for multiple iteration
	{
		
		double [] rxm = new double [dg.nrois];
		double [] rym = new double [dg.nrois];
		double [] rzm = new double [dg.nrois];
		
		// new drifts
		double [] nxdrift = (double []) xdrift.clone();
		double [] nydrift = (double []) ydrift.clone();
		double [] nzdrift = (double []) zdrift.clone();
		//double [] nxdrift = fn.movingMean(xdrift, 3);
		//double [] nydrift = fn.movingMean(ydrift, 3);
		//double [] nzdrift = fn.movingMean(zdrift, 3);
		
		double [] weight = new double[nframes];
				
		int xstart, xend, ystart, yend;
		double sSum;
		Rectangle roi;
		
		// Calculate the center of each cluster
		for (int r=0;r<dg.nrois;r++)
		{
			roi = dg.rois[r].getBoundingRect();			
			xstart=(int) Math.round(roi.x/magn);
			xend=(int) Math.round((roi.x+roi.width)/magn);
			ystart=(int) Math.round(roi.y/magn);
			yend=(int) Math.round((roi.y+roi.height)/magn);
			
			sSum=0;
			rxm[r]=0;
			rym[r]=0;
			rzm[r]=0;
			int f_;
			// Calculate the center of each cluster
			for (int n=0;n<nres;n++)
			{
				if (x[n]>xstart && x[n]<xend && y[n]>ystart && y[n]<yend)
				{
					rxm[r]+=(x[n]+xdrift[(int) frame[n]-1])*s[n];
					rym[r]+=(y[n]+ydrift[(int) frame[n]-1])*s[n];
					rzm[r]+=(z[n]+zdrift[(int) frame[n]-1])*s[n];
					sSum+=s[n];
				}
			}
			rxm[r]/=sSum;
			rym[r]/=sSum;
			rzm[r]/=sSum;

			// Calculate the drift track
			for (int n=0;n<nres;n++)
			{
				if (x[n]>xstart && x[n]<xend && y[n]>ystart && y[n]<yend)
				{
					nxdrift[(int) frame[n]-1] += (rxm[r]-(x[n]+xdrift[(int) frame[n]-1]))*s[n];
					nydrift[(int) frame[n]-1] += (rym[r]-(y[n]+ydrift[(int) frame[n]-1]))*s[n];
					nzdrift[(int) frame[n]-1] += (rzm[r]-(z[n]+zdrift[(int) frame[n]-1]))*s[n];
					weight [(int) frame[n]-1] += s[n];
				}
			}			
		}
		// Renormalize all the values and smooth
		double walking_xmean = 0;
		double walking_ymean = 0;
		double walking_zmean = 0;
		
		for (int f=0; f<nframes; f++)
		{
			if (weight[f]!=0)
			{
				nxdrift[f] /= weight[f];
				nydrift[f] /= weight[f];
				nzdrift[f] /= weight[f];
				walking_xmean = walking_xmean*smoothness+nxdrift[f]*(1-smoothness);
				walking_ymean = walking_ymean*smoothness+nydrift[f]*(1-smoothness);
				walking_zmean = walking_zmean*smoothness+nzdrift[f]*(1-smoothness);
				nxdrift[f] = walking_xmean;
				nydrift[f] = walking_ymean;
				nzdrift[f] = walking_zmean;
			}
		}
		
		double change = 0;
		int last_known=0;
		int next_known=0;
		for (int f=1; f<nframes; f++)
		{
			// LINEAR INTERPOLATION BETWEEN LAST KNOWN DRIFT VALUES
			if (weight[f]!=0) // we already know the value, don't interpolate
				last_known = f;
			if (weight[f]==0) // need to interpolate
			{
				if (next_known<f) // search for next known value
				{
					for (int f_=f; f_<nframes; f_++)
					{
						if (weight[f_]!=0)
						{
							next_known=f_;
							break;
						}
					}
					if (next_known==0) next_known = nframes-1; // not found ,default to last value
				}
				nxdrift[f] = nxdrift[last_known] + ((nxdrift[next_known]-nxdrift[last_known])/(next_known-last_known))*(f-last_known);
				nydrift[f] = nydrift[last_known] + ((nydrift[next_known]-nydrift[last_known])/(next_known-last_known))*(f-last_known);
				nzdrift[f] = nzdrift[last_known] + ((nzdrift[next_known]-nzdrift[last_known])/(next_known-last_known))*(f-last_known);
			}			
			change += Math.abs(xdrift[f]-nxdrift[f]);
		}

		for (int n=0;n<nframes;n++)
		{
			xdrift[n] = nxdrift[n];
			ydrift[n] = nydrift[n];
			zdrift[n] = nzdrift[n];
		}
		
		return change;
	}
}