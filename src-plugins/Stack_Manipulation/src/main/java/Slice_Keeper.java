import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

/** This plugin keeps slices from a stack. */
public class Slice_Keeper implements PlugIn {
	private static int first = 1;
	private static int last = 9999;
	private static int inc = 2;
	String title;
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{IJ.noImage(); return;}
		ImageStack stack = imp.getStack();
		if (stack.getSize()==1)
			{IJ.error("Stack Required"); return;}
		if (!showDialog(stack))
			return;
		title=imp.getTitle();
		keepSlices(stack, first, last, inc);
		//imp.setStack(null, stack);
		IJ.register(Slice_Keeper.class);
	}

	public boolean showDialog(ImageStack stack) {
		if (last>stack.getSize())
			last = stack.getSize();
		GenericDialog gd = new GenericDialog("Slice Keeper");
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

	public void keepSlices(ImageStack stack, int first, int last, int inc) {
		if (last>stack.getSize())
		last = stack.getSize();

		int count = 0;
		ImageProcessor ip;
		ImageStack newstack = new ImageStack(stack.getWidth(), stack.getHeight()) ;

		for (int i=first; i<=last; i+=inc)
			{
			if ((i-count)>stack.getSize())
				break;
			//stack.deleteSlice(i);
			ip = stack.getProcessor(i);
			newstack.addSlice("slice:" + i, ip);
			count++;
			}
		new ImagePlus(title+" kept stack", newstack).show();
	}

}
