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

class MyFunctions
{
	GaussianBlur gblur = new GaussianBlur();
	//ResultsTable ptable = new ResultsTable(); // Particle table
	ResultsTable ptable = Analyzer.getResultsTable(); // Particle table
	ResultsTable dtable = new ResultsTable(); // Drift table
	ResultsTable caltable = new ResultsTable(); // Astigmatism calibration table
	ParticleSaver psave; // Particle saver manager class
	
	double [] cal3d_z; // z positions
	double [] cal3d_wmh; // width minus height
	int cal3d_center; // closest index to the center value of the cal3d_* arrays
	
	java.util.concurrent.locks.Lock ptable_lock = new java.util.concurrent.locks.ReentrantLock();

	boolean debug = false;
	
	void MyFunctions()
	{
		ptable.setPrecision(3);
		dtable.setPrecision(3);
		caltable.setPrecision(3);
	}
	
	void log(java.lang.String txt)
	{
		if (debug) IJ.log(txt);
	}
	
	
	/** Loads values from the calibration table into the cal3d_* arrays. */
	void initialize3d()
	{
		cal3d_z = caltable.getColumnAsDoubles(0);
		cal3d_wmh = caltable.getColumnAsDoubles(1);
		cal3d_center=(int) Math.round(cal3d_z.length/2);
	}
	
	/** Given a calculated width-minus-height (wmh) converts this value into
	 * the corresponding coordinate in Z by comparing against the loaded
	 * Z-calibration table. Only used it the particle is disturbed by
	 * astigmatism.
	 *
	 * @param wmh the width-minus-height of a particle
	 * @return corresponding z-position value, will return 9999 if wmh is out of limits
	*/
	double getZ(double wmh)
	{
		int n = getClosest(wmh, cal3d_wmh, cal3d_center);
		if ((n==0) || (n==(cal3d_z.length-1)))
			return 9999;
		double x1 = (cal3d_wmh[n-1]+cal3d_wmh[n])/2;
		double x2 = (cal3d_wmh[n+1]+cal3d_wmh[n])/2;
		double y1 = (cal3d_z[n-1]+cal3d_z[n])/2;
		double y2 = (cal3d_z[n+1]+cal3d_z[n])/2;
		
		// linear interpolation between the two nearest values found on the
		// Z-calibration table
		return y1 + ((y2 - y1) / (x2 - x1))*(wmh - x1);
	}
	
	/** Grabs a new image on an observed folder on the case of the analysis
	 * being attached to the acquisition.
	 *
	 * @param dg dialog manager
	 * @param frame frame index to search for on the folder
	 * @return found image
	*/
	ImagePlus getNextImage(MyDialogs dg, int frame)
	{
		java.lang.String imname = ""+frame;
		while (imname.length()<dg.nimchars)
			imname="0"+imname;
		imname=dg.imagedir+dg.prefix+imname+dg.sufix;
	
		long start=java.lang.System.currentTimeMillis();
		ImagePlus imp = ij.IJ.openImage(imname);
		while ((imp==null) && ((java.lang.System.currentTimeMillis()-start)<dg.waittime))
		{
			imp = ij.IJ.openImage(imname);
			try
			{
				Thread.currentThread().sleep(1);
			}
			catch(Exception e)
			{
				IJ.error(""+e);
			}
		}
		if (imp!=null && imp.getType() != ImagePlus.GRAY8 && imp.getType() != ImagePlus.GRAY16)
			IJ.error("8 or 16 bit greyscale image required");
		
		return imp;
	}
	
	/** Particle finding method, will search the image for particles.
	 * @param ip image to search for particles on
	 * @param dg dialog manager
	 * @param nframe the frame index corresponding to this image
	*/
	void detectParticles(ImageProcessor ip, MyDialogs dg, int nframe)
	{
		int i, j;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int s = 0; // signal from ip
		
		boolean mask [][] = new boolean [width][height];
		
		int xmin = 0;
		int ymin = 0;
		int smin = 99999;	
		double saturation = ip.getMax();

		for (i=0;i<width;i++)
			for (j=0;j<height;j++)
			{
				s=ip.get(i,j);
				if (s<smin)
				{
					smin=s;
					xmin=i;
					ymin=j;
				}
				if (s!=saturation) mask[i][j]=false;
				else
				{
					mask[i][j]=true;
					ip.set(i,j,0);
				}
			}
			
		ImageProcessor spip=ip.duplicate(); // short-pass version of ip
		ImageProcessor lpip=ip.duplicate(); // low-pass version of ip
		gblur.blur(spip, 0.5);
		gblur.blur(lpip, dg.fwhm*2);
				
		// build new frequency gatted image		
		for (i=0;i<width;i++)
			for (j=0;j<height;j++)
			{
				s = spip.get(i,j)-lpip.get(i,j);	
				ip.set(i, j, (s>0)?s:0);
			}
		
		// lets calculate the noise level
		int xstart = xmin-6;
		if (xstart<0) xstart=0;
		int xend = xmin+7;
		if (xend>width) xend = width;
		int ystart = ymin-6;
		if (ystart<0) ystart=0;
		int yend = ymin+7;
		if (yend>height) yend = height;
		
		double noise=0;
		int npixels = 0; // total non-zero pixels
		for (i=xstart;i<xend;i++)
			for (j=ystart;j<yend;j++)
			{
				s = ip.get(i, j);
				if (s>0)
				{
					noise+=ip.get(i, j);
					npixels++;
				}
			}
		noise /= npixels;
		
		// set minimum thresh
		double snrthresh = noise*dg.snr;
		
		// start detecting particles
		int [] maxs;
		int ok_nparticles = 0;
		int notok_nparticles = 0;
		int last_ok_nparticles = 0;
		int smartcounter = 0;

		for (int n=0;n<=dg.maxpart;n++)
		{
			maxs = getMaxPositions(ip);
			if (ip.get(maxs[1], maxs[2])<snrthresh) break;
			else if (getParticle(ip, mask, maxs, dg, ptable, nframe))
				ok_nparticles++;
			else notok_nparticles++;
			if (dg.smartsnr)
			{
				if (last_ok_nparticles!=ok_nparticles)
				{
					last_ok_nparticles=ok_nparticles;
					smartcounter=0;
				}
				else if (ok_nparticles>1 && smartcounter>dg.maxpart*0.1) break;
				else smartcounter++;
			}
		}
		IJ.log("'OK'/'Not OK' Particles= "+ok_nparticles+"/"+notok_nparticles);
	}
	
	/** Particle analysis method, called for each particle candidate found by
	 * detectParticles.
	 * @param ip image to search for particles on
	 * @param dg dialog manager
	 * @param nframe the frame index corresponding to this image
	*/
	boolean getParticle(ImageProcessor ip, boolean [][] mask, int [] maxs, MyDialogs dg, ResultsTable ptable, int nframe)
	{
		int roirad = (int) Math.round(dg.fwhm);
		int xmax = maxs[1];
		int ymax = maxs[2];
		int smax = ip.get(xmax, ymax);
		int xstart = xmax-roirad;
		int xend = xmax+1+roirad;
		int ystart = ymax-roirad;
		int yend = ymax+1+roirad;
		int width = ip.getWidth();
		int height = ip.getHeight();
		double thrsh = smax*dg.pthrsh;
		
		int i, j;
		
		// skip perifery particles
		if (xstart<0 || xend>=width || ystart<0 || yend>=height) 
		{
			log("fail on perifery");
			xstart = (int) (xmax-roirad/2);
			ystart = (int) (ymax-roirad/2);
			xend = (int) (xmax+1+roirad/2);
			yend = (int) (ymax+1+roirad/2);
			
			xstart = (xstart<0)?0:xstart;
			ystart = (ystart<0)?0:ystart;
			xend = (xend>=width)?width-1:xend;
			yend= (yend>=height)?height-1:yend;
			clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
			return false;
		}

		// already analysed region
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
				if (mask[i][j])
				{
					log("fail on already analysed");
					xstart = (int) (xmax-roirad/2);
					ystart = (int) (ymax-roirad/2);
					xend = (int) (xmax+1+roirad/2);
					yend= (int) (ymax+1+roirad/2);
					clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
					return false;
				}
		
		int npixels=0;
		int s = 0;
		int sSum = 0;
	
		double xm = 0;
		double ym = 0;
		double xstd = 0; // stddev x
		double ystd = 0; // stddev y
		double xlstd = 0; // stddev left x
		double xrstd = 0; // stddev right x
		double ylstd = 0; // stddev left y
		double yrstd = 0; // stddev right y
		int xlsum = 0; // left pixel sum
		int xrsum = 0; // right pixel sum
		int ylsum = 0; // left pixel sum
		int yrsum = 0; // right pixel sum
				
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{	
					xm+=i*s;
					ym+=j*s;
					sSum+=s;
					npixels++;
				}
			}
		xm/=sSum;
		ym/=sSum;
		
		double sxdev = 0;
		double sydev = 0;
		// get the axial std	
		for (i=xstart;i<=xend;i++)
		{
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{
					sxdev = (i-xm)*s;
					sydev = (j-ym)*s;
					if (sxdev<0)
					{
						xlstd+=-sxdev;
						xlsum+=s;
					}
					else
					{
						xrstd+=sxdev;
						xrsum+=s;
					}
					if (sydev<0)
					{
						ylstd+=-sydev;
						ylsum+=s;
					}
					else
					{
						yrstd+=sydev;
						yrsum+=s;
					}
					xstd+=Math.abs(sxdev);
					ystd+=Math.abs(sydev);
				}
			}
		}
		xstd/=sSum;
		ystd/=sSum;
		xlstd/=xlsum;
		xrstd/=xrsum;
		ylstd/=ylsum;
		yrstd/=yrsum;
				
		// redimentionalize ROI based on axix std
		int xstart_ = (int) Math.round(xm-xlstd*1.177) - 1;
		int xend_ = (int) Math.round(xm+xrstd*1.177) + 1;
		int ystart_ =  (int) Math.round(ym-ylstd*1.177) - 1;
		int yend_ = (int) Math.round(ym+yrstd*1.177) + 1;
		if (xstart_>xstart) xstart = xstart_;
		if (ystart_>ystart) ystart = ystart_;
		if (xend_<xend) xend=xend_;
		if (yend_<yend) yend=yend_;
				
		double wmh = ((xlstd+xrstd)-(ylstd+yrstd))*1.177; // width minus height
		double z = 0;
		
		// area filter
		if (npixels<5 || ((xlstd+xrstd)*1.177>dg.fwhm) || ((ylstd+yrstd)*1.177>dg.fwhm))
		{
			log("fail on size");
			clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
			return false;
		}
		
		// symmetricity
		double xsym = 1-Math.abs((xlstd-xrstd)/(xlstd+xrstd));
		double ysym = 1-Math.abs((ylstd-yrstd)/(ylstd+yrstd));
		double sym = (xsym<ysym)?xsym:ysym;
		
		// if 2D
		if (!dg.is3d)
		{
			if (sym < dg.symmetry)
			{
				log("fail on 2D symmetry");
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
		}
		// if 3D
		else
		{
			if (xsym<dg.symmetry || ysym<dg.symmetry)
			{
				log("fail on 3D symmetry");
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
			z = getZ(wmh);
			if (z==9999)
			{
				clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
				return false;
			}
		}
		
		double s_ = sSum/npixels;
		double xm_=xm*dg.pixelsize;
		double ym_=ym*dg.pixelsize;
		double xlstd_=xlstd*1.177;
		double xrstd_=xrstd*1.177;
		double ylstd_=ylstd*1.177;
		double yrstd_=yrstd*1.177;
		double frame_=nframe+1;

		ptable_lock.lock();
		ptable.incrementCounter();
		ptable.addValue("Intensity", s_);
		ptable.addValue("X (px)", xm);
		ptable.addValue("Y (px)", ym);
		ptable.addValue("X (nm)", xm_);
		ptable.addValue("Y (nm)", ym_);
		ptable.addValue("Z (nm)", z);
		ptable.addValue("Left-Width (px)", xlstd_);
		ptable.addValue("Right-Width (px)", xrstd_);
		ptable.addValue("Up-Height (px)", ylstd_);
		ptable.addValue("Down-Height (px)", yrstd_);			
		ptable.addValue("X Symmetry (%)", xsym);
		ptable.addValue("Y Symmetry (%)", ysym);
		ptable.addValue("Width minus Height (px)", wmh);
		ptable.addValue("Frame Number", frame_);
		ptable_lock.unlock();
		if (psave!=null)
		{
			psave.saveParticle(s_, xm, ym, xm_, ym_, z, xlstd_, xrstd_, ylstd_, yrstd_, xsym, ysym, wmh, frame_);
		}
		
		clearRegion(thrsh, ip, mask, xstart, xend, ystart, yend);
		return true;
	}

	double [] getParticleForCalibration(ImageProcessor ip, MyDialogs dg, int xstart, int xend, int ystart, int yend)
	{		
		int i, j;
			
		int npixels=0;
		int s = 0;
		int sSum = 0;
	
		double xm = 0;
		double ym = 0;
		double xstd = 0; // stddev x
		double ystd = 0; // stddev y
		double xlstd = 0; // stddev left x
		double xrstd = 0; // stddev right x
		double ylstd = 0; // stddev left y
		double yrstd = 0; // stddev right y
		int xlsum = 0; // left pixel sum
		int xrsum = 0; // right pixel sum
		int ylsum = 0; // left pixel sum
		int yrsum = 0; // right pixel sum
		
		
		int smax=0;
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
				smax = ((s=ip.get(i,j))>smax)?s:smax;
			
		double thrsh = smax*dg.pthrsh;
		
		for (i=xstart;i<=xend;i++)
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{	
					xm+=i*s;
					ym+=j*s;
					sSum+=s;
					npixels++;
				}
			}
		xm/=sSum;
		ym/=sSum;
		
		double sxdev = 0;
		double sydev = 0;
		// get the axial std	
		for (i=xstart;i<=xend;i++)
		{
			for (j=ystart;j<=yend;j++)
			{
				s=ip.get(i, j);	
				if (s>thrsh)
				{
					sxdev = (i-xm)*s;
					sydev = (j-ym)*s;
					if ((sxdev)<0)
					{
						xlstd+=-sxdev;
						xlsum+=s;
					}
					else
					{
						xrstd+=sxdev;
						xrsum+=s;
					}
					if ((j-ym)<0)
					{
						ylstd+=-sydev;
						ylsum+=s;
					}
					else
					{
						yrstd+=sydev;
						yrsum+=s;
					}
					xstd+=Math.abs(sxdev);
					ystd+=Math.abs(sydev);
				}
			}
		}
		xstd/=sSum;
		ystd/=sSum;
		xlstd/=xlsum;
		xrstd/=xrsum;
		ylstd/=ylsum;
		yrstd/=yrsum;
				
		double wmh = ((xlstd+xrstd)-(ylstd+yrstd))*1.177; // width minus height
		
		// symmetricity
		double xsym = 1-Math.abs((xlstd-xrstd)/(xlstd+xrstd));
		double ysym = 1-Math.abs((ylstd-xrstd)/(ylstd+xrstd));
		double sym = (xsym<ysym)?xsym:ysym;
		
		double [] results = new double [9];
		results[0]=sSum/npixels; // intensity
		results[1]=xm;
		results[2]=ym;
		results[3]=xlstd;
		results[4]=xrstd;
		results[5]=ylstd;
		results[6]=yrstd;
		results[7]=wmh;
		results[8]=sym;
		
		return results;
	}

	void clearRegion(double thrsh, ImageProcessor ip, boolean [][] mask, int xstart, int xend, int ystart, int yend)
	{
		int s;
		for (int i=xstart;i<=xend;i++)
			for (int j=ystart;j<=yend;j++)
			{
				s=ip.get(i,j);
				if (s>thrsh)
				{
					ip.set(i, j, 0);
					mask[i][j]=true;
				}
			}
	}
	
	int [] getMaxPositions(ImageProcessor ip)
	{
		int [] results = new int [3];
		results[0]=0;
		results[1]=0;
		results[2]=0;
		int s = 0;
		
		for (int i=0;i<ip.getWidth();i++)
		{
			for (int j=0;j<ip.getHeight();j++)
			{
				s=ip.get(i, j);	
				if (s>results[0])
				{
					results[0]=s;
					results[1]=i;
					results[2]=j;
				}
			}
		}
		return results;			
	}

	double mean(double [] array, int start, int stop)
	{
		double mean=0;
		int l=array.length;
		
		if (start<0)
		   start=0;
		if (stop>l)
			stop=l;
		for (int n=start; n<=stop; n++)
		    mean+=array[n];
		if (mean!=0)
		    mean=mean/(stop-start);
		return mean;
	}

	int getClosest(double value, double [] arr, int center)
	{
		int maxd;
		int closest = center;
		int nvalues = arr.length;
		double err = 0;
		double olderr = 9999999;
		int p;
		if ((nvalues-center)>=nvalues)
			maxd=nvalues-center;
		else
			maxd=center;
		for (int n=0;n<maxd;n++)
		{
			p=center+n;
			if (p<nvalues)
			{
				err=Math.abs(value-arr[p]);
				if (err<olderr)
				{
					closest=p;
					olderr=err;
				}
			}
			p=center-n;
			if (p>0)
			{
				err=Math.abs(value-arr[p]);
				if (err<olderr)
				{
					closest=p;
					olderr=err;
				}
			}
		}
		return closest;
	}

	double [] movingMean(double [] arr, int window)
	{
		int nvalues = arr.length;
		double [] amean = new double[nvalues];
		int ws;
		for (int n=0;n<nvalues;n++)
		{
			ws=0;
			amean[n]=0;
			for (int w=0;w<(2*window+1);w++)
			{
				if ((n-window+w)>=0 && (n-window+w)<nvalues)
				{
					amean[n]+=arr[n-window+w];
					ws++;
				}
			}
			amean[n]/=ws;
		}
		return amean;
	}

	int argmax(double [] arr)
	{
		double v=arr[0];
		int p=0;
		int nvalues = arr.length;
		for (int i=0; i<nvalues;i++)
		{
			if (arr[i]>v) 
			{
				v=arr[i];
				p=i;
			}
		}
		return p;
	}

	int argmin(double [] arr)
	{
		double v=arr[0];
		int p=0;
		int nvalues = arr.length;
		for (int i=0; i<nvalues;i++)
		{
			if (arr[i]<v) 
			{
				v=arr[i];
				p=i;
			}
		}
		return p;
	}
	
	void showTable()
	{
		if (ptable.getCounter()<5000000)
        {
			IJ.showStatus("Creating particle table, this should take a few seconds...");
            ptable.show("Results");
        }
        else
            IJ.showMessage("Warning", "Results table has too many particles, they will not be shown but the data still exists within it\nyou can still use all the plugin functionality or save table changes though the 'Save Particle Table' command.");
	}
}

