import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
/* The author of this software is Christopher Philip Mauer.  Copyright (c) 2003.
Permission to use, copy, modify, and distribute this software for any purpose 
without fee is hereby granted, provided that this entire notice is included in 
all copies of any software which is or includes a copy or modification of this 
software and in all copies of the supporting documentation for such software.
Any for profit use of this software is expressly forbidden without first
obtaining the explicit consent of the author. 
THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. 
IN PARTICULAR, THE AUTHOR DOES NOT MAKE ANY REPRESENTATION OR WARRANTY 
OF ANY KIND CONCERNING THE MERCHANTABILITY OF THIS SOFTWARE OR ITS FITNESS FOR ANY 
PARTICULAR PURPOSE. 
*/
/* This PlugInFilter implements a recursive prediction/correction 
algorithm based on the Kalman Filter.  The application for which
it was designed is cleaning up timelapse image streams.  It operates 
in linear space by filtering a previously opened stack of images and
producing a new filtered stack.
																						Christopher Philip Mauer 
																						cpmauer@northwestern.edu
*/
/*Revision History:
	7/25/03 - I realized there was weirdness with the handling of the ip for the original
	stack.  Somehow, the original stackslice1 became the newstack's final slice.  This was
	handled by not using the Processor manipulating methods, and instead using alternate 
	methods. See comments in code.
	*/
public class Kalman_Stack_Filter implements PlugInFilter{
	protected ImagePlus imp;
	protected ImagePlus imp2;
	protected ImagePlus imp3;
	protected ImageStack stack;
	protected ImageStack stack2;
	protected ImageProcessor ip;
	protected ImageProcessor ip2;
	protected	double percentvar = 0.05;
	protected double gain = 0.8;
	
	public int setup(String argv, ImagePlus imp){
		this.imp = imp;
		try{
			stack = imp.getStack();
		}
		catch(Exception e){
			IJ.showMessage("A stack must be open.");
			return DONE;
		}
		String ErrorMessage = new String("One of your values was not properly formatted.\nThe default values will be used.");
		GenericDialog d = new GenericDialog("Kalman Stack Filter", IJ.getInstance());
		d.addNumericField("Acquisition_noise variance estimate:", percentvar, 2);
		d.addNumericField("Bias to be placed on the prediction:", gain, 2);
		d.showDialog();
		if(d.wasCanceled()) return DONE;
		
		percentvar = d.getNextNumber();
		if(d.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input Number");
			return DONE;
		}
		gain = d.getNextNumber();	
		if(d.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input Number");
			return DONE;
		}
		if(percentvar>1.0||gain>1.0||percentvar<0.0||gain<0.0){
			IJ.showMessage(ErrorMessage);
			percentvar = 0.05;
			gain = 0.8;
		}
		return DOES_16+STACK_REQUIRED+NO_UNDO;
	}
	public void run(ImageProcessor ip){
		imp2 = Kalmanizer(imp);
		//imp2.setProcessor(null, ip);/*original*/
		imp2.getProcessor().resetMinAndMax();/*7/25/03*/
		imp2.show();
		if (ij.macro.Interpreter.isBatchMode()) return;
		imp2.getWindow().setLocation(stack.getWidth()+25,97);
	}
	public ImagePlus Kalmanizer(ImagePlus imp){
		int m = stack.getWidth();
		int n = stack.getHeight();
		//imp2 = new ImagePlus("Kalman Stack Filter", imp.getProcessor());/*original*/
		imp2 = new ImagePlus("Kalman Stack Filter", imp.getStack());/*7/25/03*/
		stack2 = imp2.createEmptyStack();
		int dimension = m*n;
		int stacksize = stack.getSize();
		short stackslice[] = new short[dimension];
		short filteredslice[] = new short[dimension];
		double noisevar[] = new double[dimension];
		double average[] = new double[dimension];
		double predicted[] = new double[dimension];
		double predictedvar[] = new double[dimension];
		double observed[] = new double[dimension];
		double Kalman[] = new double[dimension];
		double corrected[] = new double[dimension];
		double correctedvar[] = new double[dimension];
		
		for (int i=0;i<dimension;++i) noisevar[i] = percentvar;
		stackslice = (short[])stack.getPixels(1);
		predicted = short2double(stackslice);
		stack2.addSlice("1",double2short(predicted));
		predictedvar = noisevar;
		
		for(int i=1;i<stacksize;++i){
			stackslice = (short[])stack.getPixels(i+1);
			observed = short2double(stackslice);
			for(int k=0;k<Kalman.length;++k){
				Kalman[k] = predictedvar[k]/(predictedvar[k]+noisevar[k]);
			}
			for(int k=0;k<corrected.length;++k){
				corrected[k] = gain*predicted[k]+(1.0-gain)*observed[k]+Kalman[k]*(observed[k] - predicted[k]);
			}
			for(int k=0;k<correctedvar.length;++k){
				correctedvar[k] = predictedvar[k]*(1.0 - Kalman[k]);
			}
			predictedvar = correctedvar;
			predicted = corrected;
			filteredslice = double2short(corrected);
			ip2 = imp2.getProcessor();
			ip2.setPixels(filteredslice);
			stack2.addSlice(String.valueOf(i+1), ip2);
		}
		imp2.setStack("Kalman Stack Filter", stack2);
		return imp2;
	}
	
	public short[] double2short(double array[]){
		short shortarray[] = new short[array.length];
		for(int j=0;j<array.length;++j){
			shortarray[j] = (short)((array[j]*65535)-32768);
		}
		return shortarray;
	}
	public double[] short2double(short array[]){
		double doublearray[] = new double[array.length];
		for(int j=0;j<array.length;++j){
			doublearray[j] = (((double)array[j])+32768)/65535;
		}
		return doublearray;
	}
}
