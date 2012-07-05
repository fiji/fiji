package fiji.stacks;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.PlugInFilter;
import java.awt.*;

/* 	Author: Julian Cooper, based in part on code from Slicer.java in ImageJ
	Contact: Julian.Cooper [at] uhb.nhs.uk
	First version: 2009/02/22
	Licence: Public Domain	*/
	

/* Fetched from http://rsb.info.nih.gov/ij/plugins/radial-reslice/download/Radial_Reslice.java
 * on 2009-03-04
 * Special thanks to Julian Cooper for adding the license notice above, and for sharing so kindly.
 */

/** Implements the Radial Reslice command. */
public class Radial_Reslice implements PlugInFilter {

	private static final String[] sense = {"Clockwise", "Anti-clockwise"};
	private static String senseAt = sense[0];
	private static boolean centre = false;
	private static boolean suppress = false;
	private double inputZSpacing = 1.0;
	private double outputZSpacing = 1.0;
	private double arcangle = 360.0;
	private int outputSlices = 1;
	private boolean noRoi;
	private boolean rgb;
	private ImagePlus imp;




	public int setup(String arg, ImagePlus imp) {

		return DOES_ALL;
	}


	public void run(ImageProcessor ip) {
		 imp = WindowManager.getCurrentImage();
		 if (imp==null) {
				IJ.noImage();
				return;
		 }
		 int stackSize = imp.getStackSize();
		 Roi roi = imp.getRoi();
		 int roiType = roi!=null?roi.getType():0;
		 // stack required except for ROI = none or RECT
		 if (stackSize<2) {
				IJ.error("Radial Reslicer...", "Stack required");
				return;
		 }
		 // permissible ROI types: none,*LINE
		 if (roiType!=Roi.LINE) {
				IJ.error("Radial Reslicer...", "Line selection required");
				return;
		 }
		 if (!showDialog(imp))	//need to update the dialog
				return;
		 long startTime = System.currentTimeMillis();
		 ImagePlus imp2 = null;
		 rgb = imp.getType()==ImagePlus.COLOR_RGB;

		 imp2 = radreslice(imp);
		 if (imp2==null)
				return;
		 double min = ip.getMin();
		 double max = ip.getMax();

		 if (!rgb) imp2.getProcessor().setMinAndMax(min, max);
		 imp2.show();
		 if (noRoi)
				imp.killRoi();
		else
				imp.draw();
		IJ.showStatus(IJ.d2s(((System.currentTimeMillis()-startTime)/1000.0),2)+" seconds");
	}

	public ImagePlus radreslice(ImagePlus imp) {
		 ImagePlus imp2;
		 Roi roi = imp.getRoi();
		 int roiType = roi!=null?roi.getType():0;
		 Calibration origCal = imp.getCalibration();
		 imp2 = radslice(imp);

		 imp2.setCalibration(imp.getCalibration());
		 Calibration cal = imp2.getCalibration();
		 cal.pixelDepth = origCal.pixelHeight;

		 return imp2;
	}

	boolean showDialog(ImagePlus imp) {
		 Calibration cal = imp.getCalibration();
		 String units = cal.getUnits();
		 if (cal.pixelWidth==0.0)
				cal.pixelWidth = 1.0;
		 double outputSpacing = cal.pixelDepth;
		 inputZSpacing = cal.pixelDepth;

		 GenericDialog gd2 = new GenericDialog("Radial Reslice");
		 gd2.addNumericField("Angle (degrees):", 360, 0);
		 gd2.addNumericField("Degrees_per_slice", 1, 0);
		 gd2.addChoice("Direction:", sense, senseAt);
		 gd2.addCheckbox("Rotate_about_centre", centre);
		 gd2.addCheckbox("Suppress_reversed_duplicates", suppress);

		 gd2.showDialog();
		 if (gd2.wasCanceled())
				return false;
		 arcangle = gd2.getNextNumber();
		 if (arcangle >360 || arcangle <1) arcangle = 360;
		 if (cal.pixelDepth==0.0) cal.pixelDepth = 1.0;
		 outputSlices = (int) Math.round(arcangle / gd2.getNextNumber());

		 senseAt = gd2.getNextChoice();
		 centre = gd2.getNextBoolean();
         suppress = gd2.getNextBoolean();
		 return true;
	}



	ImagePlus radslice(ImagePlus imp) {
		 double X0 = 0.0; //Will represent
		 double Y0 = 0.0; //the centre of rotation

		 double X1 = 0.0; //One end of the line
		 double Y1 = 0.0;

		 double X2 = 0.0; //The other end
		 double Y2 = 0.0;

		 double r = 0.0;	//The length of the line
		 double a = 0.0;	//angle increment
		 double a0 = 0.0;	//initial angle
		 double ang = 0.0;	//current angle
		 noRoi = false;

		 Roi roi = imp.getRoi();

		 Line line = (Line)roi;
		 X1 = Math.round(line.x1);
		 Y1 = Math.round(line.y1);
		 X2 = Math.round(line.x2);
		 Y2 = Math.round(line.y2);
		 X0 = X1; Y0 = Y1;					//default centre is one end of line
		 r = line.getRawLength();

		 a = (Math.PI/180.0)*(arcangle/outputSlices);
		 if(centre) {
		 	X0 = (X1+(X2-X1)/2); Y0 = (Y1+(Y2-Y1)/2); 	//rotation centre is middle of line
		 	r /= 2;						//half length for rotation
		 	if(suppress && arcangle>180) outputSlices *= 180/arcangle;		//only go halfway to prevent duplication of slices
		 }

		 if (senseAt.equals(sense[1])) a = -a;
		 a0 = Math.atan2(X2-X0,Y0-Y2);
		 if (a0 < 0) a0 += 2*Math.PI;

		 if (outputSlices==0) {
				IJ.error("Radial reslice", "Output Z spacing ("+IJ.d2s(outputZSpacing,0)+" pixels) is too large.");
				return null;
		 }
		 boolean virtualStack = imp.getStack().isVirtual();
		 String status = null;
		 ImagePlus imp2 = null;
		 ImageStack stack2 = null;
		 boolean isStack = imp.getStackSize()>1;
		 IJ.resetEscape();
		 for (int i=0; i<outputSlices; i++)	{
				if (virtualStack)
					status = outputSlices>1?(i+1)+"/"+outputSlices+", ":"";
				ImageProcessor ip = getSlice(imp, X1, Y1, X2, Y2, status);
				if (isStack) drawLine(X1, Y1, X2, Y2, imp);
				if (stack2==null) {
					stack2 = createOutputStack(imp, ip);
					if (stack2==null || stack2.getSize()<outputSlices) return null; // out of memory
				}
				stack2.setPixels(ip.getPixels(), i+1);
				ang = a0+a*(i+1);
				if (ang < 0) ang += 2*Math.PI;

				X2 = (r*Math.sin(ang) + X0);
				Y2 = (Y0 - r*Math.cos(ang));
				if (centre) {
					X1 = (-r*Math.sin(ang) + X0);
					Y1 = (Y0 + r*Math.cos(ang));
				}
				if (IJ.escapePressed())
					{IJ.beep(); imp.draw(); return null;}
		 }
		 return new ImagePlus("Reslice of "+imp.getShortTitle(), stack2);
	}

	ImageStack createOutputStack(ImagePlus imp, ImageProcessor ip) {
		 int bitDepth = imp.getBitDepth();
		 int w2=ip.getWidth(), h2=ip.getHeight(), d2=outputSlices;
		 int flags = NewImage.FILL_BLACK + NewImage.CHECK_AVAILABLE_MEMORY;
		 ImagePlus imp2 = NewImage.createImage("temp", w2, h2, d2, bitDepth, flags);
		 if (imp2!=null && imp2.getStackSize()==d2)
				IJ.showStatus("Reslice... (press 'Esc' to abort)");
		 if (imp2==null)
				return null;
		 else {
				ImageStack stack2 = imp2.getStack();
				stack2.setColorModel(ip.getColorModel());
				return stack2;
		 }
	}

	ImageProcessor getSlice(ImagePlus imp, double x1, double y1, double x2, double y2, String status) {
		 Roi roi = imp.getRoi();
		 int roiType = roi!=null?roi.getType():0;
		 ImageStack stack = imp.getStack();
		 int stackSize = stack.getSize();
		 ImageProcessor ip,ip2=null;
		 float[] line = null;

		 for (int i=0; i<stackSize; i++) {
				ip = stack.getProcessor(i+1);

				line = getLine(ip, x1, y1, x2, y2, line);
				if (i==0) ip2 = ip.createProcessor(line.length, stackSize);
				putRow(ip2, 0, i, line, line.length);

				if (status!=null) IJ.showStatus("Slicing: "+status +i+"/"+stackSize);
		 }
		 Calibration cal = imp.getCalibration();
		 double zSpacing = inputZSpacing/cal.pixelWidth;
		 if (zSpacing!=1.0) {
				ip2.setInterpolate(true);
				ip2 = ip2.resize(line.length, (int)(stackSize*zSpacing));
		 }
		 return ip2;
	}

	public void putRow(ImageProcessor ip, int x, int y, float[] data, int length) {
		 if (rgb) {
				for (int i=0; i<length; i++)
					ip.putPixel(x++, y, Float.floatToIntBits(data[i]));
		 } else {
				for (int i=0; i<length; i++)
					ip.putPixelValue(x++, y, data[i]);
		 }
	}



	private float[] getLine(ImageProcessor ip, double x1, double y1, double x2, double y2, float[] data) {
		 double dx = x2-x1;
		 double dy = y2-y1;
		 int n = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
		 if (data==null)
				data = new float[n];
		 double xinc = dx/n;
		 double yinc = dy/n;
		 double rx = x1;
		 double ry = y1;
		 for (int i=0; i<n; i++) {
				if (rgb) {
					int rgbPixel = ((ColorProcessor)ip).getInterpolatedRGBPixel(rx, ry);
					data[i] = Float.intBitsToFloat(rgbPixel&0xffffff);
				} else
					data[i] = (float)ip.getInterpolatedValue(rx, ry);
				rx += xinc;
				ry += yinc;
		 }
		 return data;
	}



	void drawLine(double x1, double y1, double x2, double y2, ImagePlus imp) {
		 ImageCanvas ic = imp.getCanvas();
		 if (ic==null) return;
		 Graphics g = ic.getGraphics();
		 g.setColor(Roi.getColor());
		 g.setXORMode(Color.black);
		 g.drawLine(ic.screenX((int)(x1+0.5)), ic.screenY((int)(y1+0.5)), ic.screenX((int)(x2+0.5)), ic.screenY((int)(y2+0.5)));
	}


}
