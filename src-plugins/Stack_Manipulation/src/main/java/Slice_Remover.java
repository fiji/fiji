import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This plugin removes slices from a stack. */
public class Slice_Remover implements PlugIn {
	private static int first = 1;
	private static int last = 9999;
	private static int inc = 2;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageStack stack = imp.getStack();
		if (stack.getSize()==1)
			{IJ.error("Stack Required"); return;}
		if (!showDialog(stack))
			return;
		removeSlices(stack, first, last, inc);
		imp.setStack(null, stack);
		IJ.register(Slice_Remover.class);
	}

	public boolean showDialog(ImageStack stack) {
		if (last>stack.getSize())
			last = stack.getSize();
		GenericDialog gd = new GenericDialog("Slice Remover");
		gd.addNumericField("First Slice:", first, 0);
		gd.addNumericField("Last Slice:", last, 0);
		gd.addNumericField("Increment:", inc, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		first = (int) gd.getNextNumber();
		last = (int) gd.getNextNumber();
		inc = (int) gd.getNextNumber();
		return true;
	}
	
	public void removeSlices(ImageStack stack, int first, int last, int inc) {
		if (last>stack.getSize())
			last = stack.getSize();
		int count = 0;
		for (int i=first; i<=last; i+=inc) {
			if ((i-count)>stack.getSize())
				break;
			stack.deleteSlice(i-count);
			count++;
		}
	}

}
