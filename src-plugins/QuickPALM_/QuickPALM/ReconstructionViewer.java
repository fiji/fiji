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

class ReconstructionViewer
{
	ImagePlus imp;
	ImageProcessor ip;
	
	MyDialogs settings;
	ResultsTable table;
	MyFunctions functions;
	
	GaussianBlur gblur = new GaussianBlur();
	
	int position = 0;
	int max=0;
	int min=9999999;
	double maxZ=10;
	double minZ=-10;
	
	double [] s;
	double [] x;
	double [] y;
	double [] z;
	double [] f;
	
	int nframes = 0;
	
	/** Class constructer used on the online rendering mode.
	 * @param title name for the rendering window
	 * @param width original image width
	 * @param height original image height
	 * @param dg dialog manager
	 * @param f functions manager
	*/
	ReconstructionViewer(java.lang.String title, int width, int height, MyDialogs dg, MyFunctions f)
	{
		settings = dg;
		table = f.ptable;
		functions = f;
		
		int new_width=(int) (width*settings.magn+1);
		int new_height=(int) (height*settings.magn+1);
		
		if (settings.view)
		{
			ip=new ColorProcessor(new_width, new_height);
			//calculateColorBar();
			imp = new ImagePlus(title, ip);
			IJ.run(imp, "Set Scale...", "distance=1 known="+settings.pixelsize/settings.magn+" pixel=1 unit=nm");
			imp.show();
		}
	}
	
	/** Class constructer used on the offline rendering mode.
	 * @param title name for the rendering window
	 * @param dg dialog manager
	 * @param f_ functions manager
	*/
	ReconstructionViewer(java.lang.String title, MyDialogs dg, MyFunctions f_)
	{
		settings = dg;
		table = f_.ptable;
		functions = f_;
		
		double pixelsize = table.getValue("X (nm)", 0)/table.getValue("X (px)", 0);
		settings.magn = pixelsize/settings.viewer_tpixelsize;
		
		int new_width=(int) (settings.viewer_owidth*settings.magn+1);
		int new_height=(int) (settings.viewer_oheight*settings.magn+1);
		
		// create the processor
		if (settings.view_mode == settings.view_modes[3]) // 2D particle intensity (8-bit)
			ip=new ByteProcessor(new_width, new_height);
		else if (settings.view_mode == settings.view_modes[1] || settings.view_mode == settings.view_modes[2]) // 2D histogram or 2D particle intensity (16-bit)
			ip=new ShortProcessor(new_width, new_height);
		else // color
			ip=new ColorProcessor(new_width, new_height);
		
		// create the image
		imp = new ImagePlus(title, ip);
		IJ.run(imp, "Set Scale...", "distance=1 known="+settings.viewer_tpixelsize+" pixel=1 unit=nm");
		
		// load data
		functions.ptable_lock.lock();
		s = table.getColumnAsDoubles(0);
		x = table.getColumnAsDoubles(1);
		y = table.getColumnAsDoubles(2);
		z = table.getColumnAsDoubles(5);
		f = table.getColumnAsDoubles(13);
		functions.ptable_lock.unlock();
		
		// load max & min values
		for (int n=0;n<f.length;n++)
		{
			if (f[n]>nframes) nframes=(int) f[n];
			if (s[n]>max) max=(int) Math.round(s[n]);
			if (s[n]<min) min=(int) Math.round(s[n]);
			if (z[n]>maxZ) maxZ=(int) Math.round(z[n]);
			if (z[n]<minZ) minZ=(int) Math.round(z[n]);
			if (f[n]>nframes) nframes=(int) f[n];
		}
		
		if (dg.viewer_mergeabove!=0) maxZ=dg.viewer_mergeabove;
		if (dg.viewer_mergebellow!=0) minZ=dg.viewer_mergebellow;
		//max = max/2;
		if (settings.view_mode == settings.view_modes[0]) // color
			calculateColorBar();
	}
	
	/** Main reconstruction drawing function used by the "Reconstruct Dataset" plugin.
	 * @param fstart show only particle after this frame
	 * @param fstop show only particle before this frame
	 * @param zstart show only particles bellow this z-position
	 * @param zstop show only particles above this z-position
	*/
	void draw(int fstart, int fstop, double zstart, double zstop)
	{		

		int type = imp.getBitDepth();		
		int xmag, ymag;
		double old_v, new_v;
		int [] old_rgb = new int [3];
		int [] new_rgb = new int [3];

		int satv;

		for (int n=0;n<f.length;n++)
		{
			if (f[n]>=fstart && f[n]<=fstop)
			{
				if (z[n]>=zstart && z[n]<=zstop)
				{
					xmag=(int) Math.round(x[n]*settings.magn);
					ymag=(int) Math.round(y[n]*settings.magn);
					xmag=(xmag>=ip.getWidth())?ip.getWidth()-1:xmag;
					xmag=(xmag<0)?0:xmag;
					ymag=(ymag>=ip.getHeight())?ip.getHeight()-1:ymag;
					ymag=(ymag<0)?0:ymag;
					
					if (settings.view_mode==settings.view_modes[0]) // color
					{						
						old_rgb = ip.getPixel(xmag, ymag, old_rgb);
						new_rgb = calculateColor(s[n], z[n]);
						new_rgb[0]+=old_rgb[0];
						new_rgb[1]+=old_rgb[1];
						new_rgb[2]+=old_rgb[2];
						new_rgb[0]=(new_rgb[0]>255)?255:new_rgb[0];
						new_rgb[1]=(new_rgb[1]>255)?255:new_rgb[1];
						new_rgb[2]=(new_rgb[2]>255)?255:new_rgb[2];
						ip.putPixel(xmag, ymag, new_rgb);
					}
					else if (settings.view_mode==settings.view_modes[1]) // 2D histogram
					{
						old_v=ip.get(xmag, ymag);
						new_v=old_v+1;
						ip.set(xmag, ymag, (int) new_v);
					}
					else if (settings.view_mode==settings.view_modes[2]) //2D particle intensity (16-bit)
					{
						old_v=ip.get(xmag, ymag);
						new_v=s[n]+old_v;
						ip.set(xmag, ymag, (int) new_v);
					}
					else // 2D particle intensity (8-bit)
					{
						satv = 255;
						old_v=ip.get(xmag, ymag);
						new_v=((s[n]-min)/(max-min))*satv;
						//if (old_v<new_v)
						//	ip.set(xmag, ymag, (int) Math.round(new_v));
						new_v = Math.round(new_v+old_v);
						new_v = (new_v>satv)?satv:new_v;
						ip.set(xmag, ymag, (int) new_v);
					}
				}
			}
		}
		if (settings.viewer_doConvolve) gblur.blur(ip, (settings.viewer_fwhm/2.354)/settings.viewer_tpixelsize);
		imp.updateAndDraw();
		//if (imp.isVisible())
		//	IJ.run(imp, "Enhance Contrast", "saturated=0.5");		
	}
	
	/** Calculates a color bar to guide users on the position of each particle in Z.*/
	ImagePlus calculateColorBar()
	{
		int border = 20;
		ImageProcessor ipbar = new ColorProcessor(80, 510+border*2);
		ImagePlus impbar = new ImagePlus("Depth coding", ipbar);
		
		
		int [] c = new int [3];
		for (int n=0; n<=510; n++)
		{
			c[0]=(n>=255)?n-255:0;
			c[1]=(n<=255)?n:510-n;
			c[2]=(n<=255)?255-n:0;
			
			for (int m=0; m<10; m++)
				ipbar.putPixel(m,n+border,c);
		}
		
		impbar.updateAndDraw();
		impbar.show();
		
		IJ.runMacro("setFont(\"SansSerif\", 18, \" antialiased\");");
		IJ.run("Colors...", "foreground=white background=black selection=yellow");
		IJ.runMacro("drawString(\"< "+Math.round(minZ)                +"\", "+12+", "+(border+6)+");");
		IJ.runMacro("drawString(\"  "+Math.round(minZ+(maxZ-minZ)*1/6)+"\", "+12+", "+(510*1/6+border+6)+");");
		IJ.runMacro("drawString(\"  "+Math.round(minZ+(maxZ-minZ)*2/6)+"\", "+12+", "+(510*2/6+border+6)+");");
		IJ.runMacro("drawString(\"  "+Math.round(minZ+(maxZ-minZ)*3/6)+"\", "+12+", "+(510*3/6+border+6)+");");
		IJ.runMacro("drawString(\"  "+Math.round(minZ+(maxZ-minZ)*4/6)+"\", "+12+", "+(510*4/6+border+6)+");");
		IJ.runMacro("drawString(\"  "+Math.round(minZ+(maxZ-minZ)*5/6)+"\", "+12+", "+(510*5/6+border+6)+");");
		IJ.runMacro("drawString(\"> "+Math.round(minZ+(maxZ-minZ)*6/6)+"\", "+12+", "+(510*6/6+border+6)+");");
		
		return impbar;
	}
	
	/** Calculates the color for a particle based on its intensity and position in Z.
	 * @param s particle intensity
	 * @param z particle position in z
	 * @return particle color as RGB values
	*/
	int [] calculateColor(double s, double z)
	{
		double vs = ((s-min)/(max-min))*(1+settings.saturation); // allow some saturation
		double vz = ((z-minZ)/(maxZ-minZ));
		int [] c = new int [3];
		
		if (vz>1) vz=1;
		else if (vz<0) vz=0;
		double vz_=255*vz*2;
		
		c[0] = (int) Math.round(((vz>=0.5)?(vz_-255):0)*vs); //R
		c[1] = (int) Math.round(((vz<=0.5)?vz_:(510-vz_))*vs); //G
		c[2] = (int) Math.round(((vz<=0.5)?(255-vz_):0)*vs); //B
		
		c[0] = (c[0]>255)?255:c[0];
		c[1] = (c[1]>255)?255:c[1];
		c[2] = (c[2]>255)?255:c[2];
		
		return c;
	}

	/** Updates the reconstruction viewer with the lattest acquired particles. */
	void update()
	{
		if (!settings.view) return;
		int new_p=table.getCounter();
		if (new_p==0 || new_p==position) return;
		update(position, new_p-1);
		position = new_p;		
	}

	/** Updates the reconstruction viewer by showing the new particles found between
	 * the given indices.
	 * @param start first particle to be updated from the table
	 * @param stop last particle to be updated from the table
	*/
	void update(int start, int stop)
	{
		// updated May 5th - instead of loading the full columns as arrays, we only grab the needed values, should increase processing speed
		if (!settings.view) return;
		
		int nresults = table.getCounter();

		start=(start<0)?0:start;
		stop=(stop>nresults)?(nresults-1):stop;

		s = new double [stop-start+1];
		x = new double [stop-start+1];
		y = new double [stop-start+1];
		z = new double [stop-start+1];
		
		int index;
		functions.ptable_lock.lock();
		for (int n=start; n<=stop; n++)
		{
			index = n-start;
			s[index] = table.getValueAsDouble(0, n);
			x[index] = table.getValueAsDouble(1, n);
			y[index] = table.getValueAsDouble(2, n);
			z[index] = table.getValueAsDouble(5, n);
		}
		functions.ptable_lock.unlock();
		
		boolean newMax=false;
		boolean newMin=false;
		boolean newMaxZ=false;
		boolean newMinZ=false;
		
		// check if there is a new max/min value
		for (int n=0;n<=(stop-start);n++)
		{
			if (s[n]>max)
			{
				newMax=true;
				max=(int) Math.round(s[n]);
			}
			if (s[n]<min)
			{
				newMin=true;
				min=(int) Math.round(s[n]);
			}
			if (z[n]>maxZ)
			{
				newMaxZ=true;
				maxZ=z[n];
			}
			if (z[n]<minZ)
			{
				newMinZ=true;
				minZ=z[n];
			}
			
		}
		
		if (newMax || newMinZ || newMaxZ || newMin)
		{
			clear();
			update(0, stop); // if a new max/min value is found we need to reupdate the full image
			return;
		}
		
		int v, xmag, ymag;
		int [] old_rgb = new int [3];
		int [] new_rgb = new int [3];
		for (int n=0;n<=(stop-start);n++)
		{
			xmag=(int) Math.round(x[n]*settings.magn);
			ymag=(int) Math.round(y[n]*settings.magn);
			ip.getPixel(xmag, ymag, old_rgb);
			new_rgb = calculateColor(s[n], z[n]);
			if ((old_rgb[0]+old_rgb[1]+old_rgb[2])<(new_rgb[0]+new_rgb[1]+new_rgb[2]))
				ip.putPixel(xmag, ymag, new_rgb);
		}
		imp.updateAndDraw();;
	}
	
	/** Updates the reconstruction viewer by showing particles found between
	 * fstart and fstop.
	 * @param fstart start position of the frame range
	 * @param fstop stop position of the frame range
	*/
	void updateShort(int fstart, int fstop)
	{
		if (!settings.view) return;
		clear();
		functions.ptable_lock.lock();
		s = table.getColumnAsDoubles(0);
		x = table.getColumnAsDoubles(1);
		y = table.getColumnAsDoubles(2);
		z = table.getColumnAsDoubles(5);
		f = table.getColumnAsDoubles(13);
		functions.ptable_lock.unlock();
		
		int xmag, ymag;
		int [] old_rgb = new int [3];
		int [] new_rgb = new int [3];
		for (int n=0;n<f.length;n++)
		{
			if (f[n]>=fstart && f[n]<=fstop)
			{
				xmag=(int) Math.round(x[n]*settings.magn);
				ymag=(int) Math.round(y[n]*settings.magn);
				ip.getPixel(xmag, ymag, old_rgb);
				new_rgb = calculateColor(s[n], z[n]);
				if ((old_rgb[0]+old_rgb[1]+old_rgb[2])<(new_rgb[0]+new_rgb[1]+new_rgb[2]))
					ip.putPixel(xmag, ymag, new_rgb);
			}
		}
		imp.updateAndDraw();
	}
	
	/** Cleans the reconstruction viewer image. */
	void clear()
	{
		if (!settings.view) return;
		for (int i=0;i<ip.getWidth();i++)
			for (int j=0;j<ip.getHeight();j++)
				ip.set(i, j, 0);
		position=0;
	}
}
