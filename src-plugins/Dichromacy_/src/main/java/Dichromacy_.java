import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;

public class Dichromacy_ implements PlugIn {

/*
Simulation of dichromatic vision (colour blindness)

This code is an implementation of an algorithm described by Hans Brettel,
Francoise Vienot and John Mollon in the Journal of the Optical Society of
America V14(10), pg 2647. (See http://vischeck.com/ for more info.).

Based on the GIMP's cdisplay_colorblind.c
by Michael Natterer <mitch@gimp.org>, Sven Neumann <sven@gimp.org>,
Robert Dougherty <bob@vischeck.com> and Alex Wade <alex@vischeck.com>.

This code is written using "Scribus coding standard" as a part of the
Scribus project (www.scribus.net).

author Petr Vanek <petr@scribus.info>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

// Ported to ImageJ Java plugin by G.Landini at bham ac uk from colorblind.cpp  which 
// is part of the Scribus desktop publishing package.
// 6 June 2010
// 24 July 2010 change non-RGB images to RGB
// Many thanks to  Robert Dougherty <bob@vischeck.com> and Alex Wade <alex@vischeck.com>
// who clarified the gamma scaling bug..


	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null){
			IJ.error("No image!");
			return;
		}
		int width = imp.getWidth();
		int height =imp.getHeight();
                String title = imp.getTitle();
		if(imp.getBitDepth() != 24)
			IJ.run(imp, "RGB Color", "");

		boolean createWindow = true;
		GenericDialog gd = new GenericDialog("Dichromacy v1.0", IJ.getInstance());
		//gd.addMessage("Select subtractive colour model");
		String [] type={"Protanope", "Deuteranope", "Tritanope"};
		gd.addChoice("Simulate", type, type[1]);
		gd.addCheckbox("Create New Window", createWindow);

		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int deficiency = gd.getNextChoiceIndex();
		createWindow = gd.getNextBoolean();
		//boolean hideLegend = gd.getNextBoolean();

		double tmp, red, green, blue;
		double [] rgb2lms = new double[9];
		double [] lms2rgb = new double[9];
		double [] gammaRGB = new double[3];
		double a1=0.0, b1=0.0, c1=0.0;
		double a2=0.0, b2=0.0, c2=0.0;
		double inflection=0.0;
		double redOld , greenOld;

		rgb2lms[0] = 0.05059983;
		rgb2lms[1] = 0.08585369;
		rgb2lms[2] = 0.00952420;

		rgb2lms[3] = 0.01893033;
		rgb2lms[4] = 0.08925308;
		rgb2lms[5] = 0.01370054;

		rgb2lms[6] = 0.00292202;
		rgb2lms[7] = 0.00975732;
		rgb2lms[8] = 0.07145979;

		lms2rgb[0] =	30.830854;
		lms2rgb[1] = -29.832659;
		lms2rgb[2] =	 1.610474;

		lms2rgb[3] =	-6.481468;
		lms2rgb[4] =	17.715578;
		lms2rgb[5] =	-2.532642;

		lms2rgb[6] =	-0.375690;
		lms2rgb[7] =	-1.199062;
		lms2rgb[8] =	14.273846;

		gammaRGB[0] = 2.0; 
		gammaRGB[1] = 2.0; 
		gammaRGB[2] = 2.0; 

		double [] anchor_e= new double[3];;
		double [] anchor= new double[12];

		/*
		Load the LMS anchor-point values for lambda = 475 & 485 nm (for
		protans & deutans) and the LMS values for lambda = 575 & 660 nm
		(for tritans)
		*/
		anchor[0] = 0.08008;  anchor[1]  = 0.1579;    anchor[2]  = 0.5897;
		anchor[3] = 0.1284;   anchor[4]  = 0.2237;    anchor[5]  = 0.3636;
		anchor[6] = 0.9856;   anchor[7]  = 0.7325;    anchor[8]  = 0.001079;
		anchor[9] = 0.0914;   anchor[10] = 0.007009;  anchor[11] = 0.0;

		/* We also need LMS for RGB=(1,1,1)- the equal-energy point (one of
		* our anchors) (we can just peel this out of the rgb2lms transform
		* matrix)
		*/
		anchor_e[0] = rgb2lms[0] + rgb2lms[1] + rgb2lms[2];
		anchor_e[1] = rgb2lms[3] + rgb2lms[4] + rgb2lms[5];
		anchor_e[2] = rgb2lms[6] + rgb2lms[7] + rgb2lms[8];

		ImageProcessor ip;
		ImagePlus imp2=null;
		if(createWindow){
			imp2 = new ImagePlus(title+"-"+type[deficiency],imp.getProcessor().duplicate());
			ip = imp2.getProcessor();
		}
		else {
			ip = imp.getProcessor();
			ip.snapshot();
			Undo.setup(Undo.FILTER, imp);
		}

		switch (deficiency)  {
			case 1:
				/*  Deuteranope */
				a1 = anchor_e[1] * anchor[8] - anchor_e[2] * anchor[7]; 
				b1 = anchor_e[2] * anchor[6] - anchor_e[0] * anchor[8];
				c1 = anchor_e[0] * anchor[7] - anchor_e[1] * anchor[6];
				a2 = anchor_e[1] * anchor[2] - anchor_e[2] * anchor[1];
				b2 = anchor_e[2] * anchor[0] - anchor_e[0] * anchor[2];
				c2 = anchor_e[0] * anchor[1] - anchor_e[1] * anchor[0];
				inflection = (anchor_e[2] / anchor_e[0]);
				break;
			case 0 :
				/* Protanope */
				a1 = anchor_e[1] * anchor[8] - anchor_e[2] * anchor[7];
				b1 = anchor_e[2] * anchor[6] - anchor_e[0] * anchor[8];
				c1 = anchor_e[0] * anchor[7] - anchor_e[1] * anchor[6];
				a2 = anchor_e[1] * anchor[2] - anchor_e[2] * anchor[1];
				b2 = anchor_e[2] * anchor[0] - anchor_e[0] * anchor[2];
				c2 = anchor_e[0] * anchor[1] - anchor_e[1] * anchor[0];
				inflection = (anchor_e[2] / anchor_e[1]);
				break;
			case 2 : 
				/* Tritanope */
				a1 = anchor_e[1] * anchor[11] - anchor_e[2] * anchor[10];
				b1 = anchor_e[2] * anchor[9]  - anchor_e[0] * anchor[11];
				c1 = anchor_e[0] * anchor[10] - anchor_e[1] * anchor[9];
				a2 = anchor_e[1] * anchor[5]  - anchor_e[2] * anchor[4];
				b2 = anchor_e[2] * anchor[3]  - anchor_e[0] * anchor[5];
				c2 = anchor_e[0] * anchor[4]  - anchor_e[1] * anchor[3];
				inflection = (anchor_e[1] / anchor_e[0]);
				break;
		}

		int imagesize = width * height;
		int[] pixels = (int[]) ip.getPixels();

		// process the image
		IJ.showStatus("Dichromacy...");

		IJ.showProgress(0.5); //show that something is going on

		for (int j=0; j<imagesize; j++) {
			int i = pixels[j];
			red = (double) ((i & 0xff0000)>>16);
			green =(double) ((i & 0xff00)>>8) ;
			blue = (double) (i & 0xff);

			/* GL: Apply (not remove!) phosphor gamma to RGB intensities */
			// GL:  This is a Gimp/Scribus code bug, this way it returns values similar to those of Vischeck:
			red = Math.pow(red/255.0,  gammaRGB[0]);
			green = Math.pow(green/255.0, gammaRGB[1]);
			blue = Math.pow(blue/255.0,  gammaRGB[2]);

			redOld = red;
			greenOld = green;

			red = redOld * rgb2lms[0] + greenOld * rgb2lms[1] + blue * rgb2lms[2];
			green = redOld * rgb2lms[3] + greenOld * rgb2lms[4] + blue * rgb2lms[5];
			blue  = redOld * rgb2lms[6] + greenOld * rgb2lms[7] + blue * rgb2lms[8];

			switch (deficiency)  {
				case 1:
					/*  Deuteranope */
					tmp = blue / red;
					/* See which side of the inflection line we fall... */
					if (tmp < inflection)
						green = -(a1 * red + c1 * blue) / b1;
					else
						green = -(a2 * red + c2 * blue) / b2;
					break;
				case 0 :
					/* Protanope */
					tmp = blue / green;
					/* See which side of the inflection line we fall... */
					if (tmp < inflection)
						red = -(b1 * green + c1 * blue) / a1;
					else
						red = -(b2 * green + c2 * blue) / a2;
					break;
				case 2 : 
					/* Tritanope */
					tmp = green / red;
					/* See which side of the inflection line we fall... */
					if (tmp < inflection)
						blue = -(a1 * red + b1 * green) / c1;
					else
						blue = -(a2 * red + b2 * green) / c2;
					break;
			}

			/* Convert back to RGB (cross product with transform matrix) */
			redOld = red;
			greenOld = green;

			red = redOld * lms2rgb[0] + greenOld * lms2rgb[1] + blue * lms2rgb[2];
			green = redOld * lms2rgb[3] + greenOld * lms2rgb[4] + blue * lms2rgb[5];
			blue = redOld * lms2rgb[6] + greenOld * lms2rgb[7] + blue * lms2rgb[8];

			/* GL Remove (not apply!) phosphor gamma to go back to original intensities */
			// GL:  This is a Gimp/Scribus code bug, this way it returns values similar to those of Vischeck:
			int ired =(int)Math.round(Math.pow(red, 1.0/gammaRGB[0])*255.0);
			int igreen =(int)Math.round(Math.pow(green, 1.0/gammaRGB[1])*255.0);
			int iblue =(int) Math.round(Math.pow(blue, 1.0/gammaRGB[2])*255.0);

			/* Ensure that we stay within the RGB gamut */
			/* *** FIX THIS: it would be better to desaturate than blindly clip. */
			ired = (ired>255) ? 255: ( (ired<0 ) ?  0: ired);
			igreen = (igreen>255) ? 255: ( (igreen<0 ) ?  0: igreen);
			iblue = (iblue>255) ? 255: ( (iblue<0 ) ?  0: iblue);

			pixels[j]=((ired & 0xff)<<16)+((igreen & 0xff)<<8 )+(iblue & 0xff)  ;
		}
		IJ.showProgress(1); // erase the progress bar
		if(createWindow){
			imp2.show();
			imp2.updateAndDraw();
		}
		else
			imp.updateAndDraw();

	}
}

