import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.ImageStack;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Reslice_Z implements PlugInFilter {

	private ImagePlus image;
	private Calibration cal;
	private int w, h;

	public void run(ImageProcessor ip) {
		w = image.getWidth();
		h = image.getHeight();
		cal = image.getCalibration();
		GenericDialog gd = new GenericDialog("Reslice_Z");
		gd.addNumericField("New pixel depth", cal.pixelDepth, 3);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		double pixelDepth = gd.getNextNumber();
		int type = image.getType();
		if(type == ImagePlus.GRAY8)
			resliceByte(pixelDepth).show();
		else if(type == ImagePlus.COLOR_RGB)
			resliceColor(pixelDepth).show();
		else
			IJ.error("Wrong image type");
	}

	public ImagePlus resliceColor(double pixelDepth) {

		ImageStack stack = image.getStack();
		int numSlices = (int)(image.getStackSize() * cal.pixelDepth / 
					pixelDepth);
		
		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 0; z < numSlices; z++) {
			ColorProcessor bp = new ColorProcessor(w, h);
			int[] pixels = (int[])bp.getPixels();
			// getSliceBefore
			double currentPosition = z * pixelDepth;
			int ind_p = (int)Math.floor(
					currentPosition / cal.pixelDepth);
			int ind_n = ind_p + 1;
			if(ind_n >= image.getStackSize())
				break;
			double d_p = currentPosition - ind_p*cal.pixelDepth;
			double d_n = ind_n*cal.pixelDepth - currentPosition;

			int[] before = (int[])stack.
					getProcessor(ind_p+1).getPixels();
			int[] after = (int[])stack.
					getProcessor(ind_n+1).getPixels();
			
			for(int i = 0; i < pixels.length; i++) {
				byte red  = interpolate(
					(byte)((before[i]&0xff0000)>>16), 
					d_p,
					(byte)((after[i]&0xff0000)>>16), 
					d_n);
				byte green = interpolate(
					(byte)((before[i]&0xff00)>>8), 
					d_p,
					(byte)((after[i]&0xff00)>>8), 
					d_n);
				byte blue = interpolate(
					(byte)(before[i] & 0xff), 
					d_p,
					(byte)(after[i] & 0xff), 
					d_n);
				pixels[i] = ((((red&0xff) << 8) + (green&0xff)) << 8) + (blue&0xff);
			}
			
			newStack.addSlice("", bp);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}
	public ImagePlus resliceByte(double pixelDepth) {

		ImageStack stack = image.getStack();
		int numSlices = (int)(image.getStackSize() * cal.pixelDepth / 
					pixelDepth);
		
		// Create a new Stack
		ImageStack newStack = new ImageStack(w, h);
		for(int z = 0; z < numSlices; z++) {
			ByteProcessor bp = new ByteProcessor(w, h);
			byte[] pixels = (byte[])bp.getPixels();
			// getSliceBefore
			double currentPosition = z * pixelDepth;
			int ind_p = (int)Math.floor(
					currentPosition / cal.pixelDepth);
			int ind_n = ind_p + 1;
			if(ind_n >= image.getStackSize())
				break;
			double d_p = currentPosition - ind_p*cal.pixelDepth;
			double d_n = ind_n*cal.pixelDepth - currentPosition;

			byte[] before = (byte[])stack.
					getProcessor(ind_p+1).getPixels();
			byte[] after = (byte[])stack.
					getProcessor(ind_n+1).getPixels();
			
			for(int i = 0; i < pixels.length; i++) {
				pixels[i] = interpolate(before[i], d_p,
						after[i], d_n);
			}
			
			newStack.addSlice("", bp);
		}
		ImagePlus result = new ImagePlus("Resliced", newStack);
		cal = cal.copy();
		cal.pixelDepth = pixelDepth;
		result.setCalibration(cal);
		return result;
	}

	public byte interpolate(byte b, double db, byte n, double dn) {
		return (byte) ((((int)(b & 0xff))*dn + ((int)(n & 0xff))*db) / 
				(dn + db));
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_RGB;
	}
}
