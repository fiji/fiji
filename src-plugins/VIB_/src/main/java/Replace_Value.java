import ij.plugin.filter.PlugInFilter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;

public class Replace_Value implements PlugInFilter {
	
	private ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Replace value");
		gd.addNumericField("Pattern: [0..255] ", 0, 0);
		gd.addNumericField("Replacement: [0.255] ", 0, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		doit((int)gd.getNextNumber(), (int)gd.getNextNumber());
	}

	public void doit(int PATTERN, int REPLACEMENT) {
		int w = image.getWidth(), h = image.getHeight();
		int d = image.getStackSize();
		ImageStack stack = image.getStack();
		for(int z = 0; z < d; z++) {
			byte[] b = (byte[])stack.getProcessor(z+1).getPixels();
			for(int i = 0; i < w*h; i++) {
				if(((int)(b[i] & 0xff)) == PATTERN)
					b[i] = (byte)REPLACEMENT;
			}
		}
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_8C;
	}
}
