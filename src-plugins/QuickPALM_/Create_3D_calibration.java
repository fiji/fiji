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
import java.util.*;

/** This plugin creates a 3D calibration table by observing the width and height
 * change of beads over depth (caused by astigmatism). This table can be then be
 * used to localize particles in 3D space.
*/
public class Create_3D_calibration implements PlugIn 
{
	ImagePlus imp;
	ImageProcessor ip;
	
	MyDialogs dg = new MyDialogs();
	MyFunctions fx = new MyFunctions();
	MyIO io = new MyIO();
	ResultsTable res = new ResultsTable();
	ResultsTable extrainfo = new ResultsTable();
	
	public boolean setup(String arg) 
	{
		if (! dg.checkBeads() || ! dg.beadCalibration3d())
			return false;
		imp = dg.imp;
        imp.setSlice(1);
		IJ.register(Create_3D_calibration.class);
		return true;
	}

	public void run(String arg) 
	{
		if (! setup(arg)) return;

		// Start processing
		ij.IJ.run(imp, "Select None", "");

		double [][] sgnl  = new double[dg.nrois][dg.nslices]; // signal array per ROI and slices
		double [][] xstd = new double[dg.nrois][dg.nslices];  // x stddev per ROI and slices
		double [][] ystd = new double[dg.nrois][dg.nslices];  // y stddev per ROI and slices
		double [][] wmh = new double[dg.nrois][dg.nslices];   // width-minus-height per ROI and slices
		double [] mean_wmh = new double[dg.nslices];          // mean width-minus-heigh for all ROIs
		
		int index_z0 = 0;
		double sSum = 0;
		double [] results;
		Rectangle roi;
		for (int s=1;s<=dg.nslices;s++)
		{
			imp.setSlice(s);
			ip=imp.getProcessor().duplicate();
			
			// build new frequency gatted image	
			ImageProcessor lpip = ip.duplicate();
			fx.gblur.blur(ip, 0.5);
			fx.gblur.blur(lpip, dg.fwhm*2);
			int v;
			for (int i=0;i<ip.getWidth();i++)
			{
				for (int j=0;j<ip.getHeight(); j++)
				{
					v=ip.get(i, j)-lpip.get(i, j);
					if (v>=0) ip.set(i, j, v);
					else ip.set(i, j, 0);
				}
			}
			
			sSum = 0;
			for (int r=0;r<dg.rois.length;r++)
			{
				roi = dg.rois[r].getBoundingRect();
				results = fx.getParticleForCalibration(ip, dg, roi.x, roi.x+roi.width, roi.y, roi.y+roi.height);
				sgnl[r][s-1]=results[0];
				xstd[r][s-1]=results[3]+results[4];
				ystd[r][s-1]=results[5]+results[6];
				wmh[r][s-1]=results[7];
				mean_wmh[s-1]+=results[7]*results[0];
				sSum+=results[0];
			}
			mean_wmh[s-1] /= sSum;
		}		
		
		// get the bias for each particle - particles are not on the same focal
		// plane since there is always a small tilt of the coverslip
		// we then need to recenter the measurements of each particle against
		// the average position of the group
		double [] bias = new double [dg.rois.length];
		for (int r=0;r<dg.rois.length;r++)
		{
			sSum = 0;
			for (int s=1;s<=dg.nslices;s++)
			{
				bias[r]+=(wmh[r][s-1]-mean_wmh[s-1])*sgnl[r][s-1];
				sSum+= sgnl[r][s-1];
			}
			bias[r]/=sSum;
		}
		
		// realign each ROI
		for (int r=0;r<dg.rois.length;r++)
			for (int s=1;s<=dg.nslices;s++)
				wmh[r][s-1]-=bias[r];
		
		//	averaging - recalculate the "mean" model particle
		for (int s=1;s<=dg.nslices;s++)
		{
			mean_wmh[s-1]=0;
			sSum=0;
			for (int r=0;r<dg.rois.length;r++)
			{
				mean_wmh[s-1]+=wmh[r][s-1]*sgnl[r][s-1];
				sSum+=sgnl[r][s-1];
			}
			mean_wmh[s-1]/=sSum;
		}
		mean_wmh=fx.movingMean(mean_wmh, dg.window);
		
		// calculate the Z-position
		double [] zpos = new double[dg.nslices];
		for (int s=1;s<=dg.nslices;s++) zpos[s-1]=s;
		
		double [] cal_mean_wmh = (double []) mean_wmh.clone();

		if (dg.model!=dg.models[0])
		{
			CurveFitter cf = new CurveFitter(zpos, mean_wmh);
			if (dg.model==dg.models[1])
				cf.doFit(CurveFitter.STRAIGHT_LINE);
			else if (dg.model==dg.models[2])
				cf.doFit(CurveFitter.POLY2);
			else if (dg.model==dg.models[3])
				cf.doFit(CurveFitter.POLY3);
			else if (dg.model==dg.models[4])
				cf.doFit(CurveFitter.POLY4);
			IJ.log("---- Model Estimation ----");
			IJ.log(cf.getResultString());
			cal_mean_wmh=cf.getResiduals();
			for (int s=0;s<cal_mean_wmh.length;s++)
				cal_mean_wmh[s]=mean_wmh[s]-cal_mean_wmh[s];
		}
		
		// realign with zero
		index_z0 = fx.getClosest(0, cal_mean_wmh, 0);
		for (int s=1;s<=dg.nslices;s++)
			zpos[s-1]=((s-index_z0)*dg.cal_z);
		
		// cut down inflexions
		int pmax = fx.argmax(cal_mean_wmh);
		int pmin = fx.argmin(cal_mean_wmh);
		
		int start = (pmax<pmin)?pmax:pmin;
		int stop = (pmax<pmin)?pmin:pmax;
		
		double [] tmp_mean_wmh     = new double [stop-start+1];
		double [] tmp_cal_mean_wmh = new double [stop-start+1];
		double [] tmp_zpos         = new double [stop-start+1];
		System.arraycopy(mean_wmh, start, tmp_mean_wmh, 0, stop-start+1);
		System.arraycopy(cal_mean_wmh, start, tmp_cal_mean_wmh, 0, stop-start+1);
		System.arraycopy(zpos, start, tmp_zpos, 0, stop-start+1);
		mean_wmh = tmp_mean_wmh;
		cal_mean_wmh = tmp_cal_mean_wmh;
		zpos = tmp_zpos;
	
		Plot plot = new Plot("Calibration Values", "Z-position (nm)", "PSF Width minus Height (px)", zpos, cal_mean_wmh);
		
		float a, b, c, f;
		java.awt.Color color;
		Random rand = new Random(0);
		
		plot.setLineWidth(1);
		for (int r=0;r<dg.nrois;r++)
		{
			f = ((float) r/(dg.nrois-1))*2;
			a = (f<1)?(1-f):0;
			b = (f<1)?f/2:(2-f)/2;
			c = (f<1)?0:(f-1);
			color = new java.awt.Color(a, b, c);
			plot.setColor(color);
			double [] tmp = new double[stop-start+1];
			System.arraycopy(wmh[r], start, tmp, 0, stop-start+1);
			plot.addPoints(zpos, tmp, Plot.CROSS);
		}
		
		plot.setLineWidth(2);
		plot.setColor(java.awt.Color.BLACK);
		plot.show();
		
		for (int s=0;s<zpos.length;s++)
		{
			res.incrementCounter();
			res.addValue("Z-Step", zpos[s]);
			res.addValue("Raw Width minus Heigh", mean_wmh[s]);
			res.addValue("Calibration Width minus Height", cal_mean_wmh[s]);
		}
		res.show("Astigmatism calibration");

		// Calculate the resolution based on the displacement between the model
		// and estimated bead positions.
		if (dg.part_divergence)
		{
			double [] mean_resol = new double[zpos.length];
			double [] std_resol  = new double[zpos.length];
			fx.cal3d_z = zpos;
			fx.cal3d_wmh = cal_mean_wmh;
			double zpart;
			double count = 0;
			for (int s=0;s<zpos.length;s++)
			{
				mean_resol[s]=0;
				std_resol[s] =0;
				for (int r=0;r<dg.rois.length;r++)
				{
					count = 0;
					zpart = fx.getZ(wmh[r][s]);
					if (zpart<9999)
					{
						mean_resol[s]+= Math.abs(zpart-zpos[s]); 
						std_resol[s] += Math.pow(zpart-zpos[s], 2);
						count++;
					}
				}
				mean_resol[s]/=count;
				std_resol[s] = Math.sqrt(std_resol[s]/count);
				if (std_resol[s]>1000) std_resol[s]=0;
				if (mean_resol[s]>1000) mean_resol[s]=0;
				//IJ.log("Z="+zpos[s]+" std="+std_resol[s]);
			}
			plot = new Plot("Particle divergence against model", "Z-position (nm)", "Stddev of bead positions vs. model (nm)", zpos, std_resol);
			plot.show();
		}
		
		if (!dg.part_extrainfo)
			return;

		for (int s=0;s<dg.nslices;s++)
		{
			extrainfo.incrementCounter();
			for (int r=0; r<dg.nrois;r++)
			{
				extrainfo.addValue("Width P"+r, xstd[r][s]);
				extrainfo.addValue("Height P"+r, ystd[r][s]);
			}
		}
		extrainfo.show("Particle extra information...");
	}
}
