import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;

/** Inserts an image or stack into a stack.
*/
public class Stack_Inserter implements PlugIn {

	private static int index1;
	private static int index2;
	private static int x, y;
			
	public void run(String arg) {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			IJ.showMessage("Stack Inserter", "No windows are open.");
			return;
		}
		if (wList.length==1) {
			IJ.showMessage("Stack Inserter", "At least two windows must be open, \nincluding at least one stack.");
			return;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp!=null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}
		if (index1>=titles.length)index1 = 0;
		if (index2>=titles.length)index2 = 0;
		GenericDialog gd = new GenericDialog("Stack Inserter");
		gd.addChoice("Source Image or Stack: ", titles, titles[index1]);
		gd.addChoice("Destination Stack: ", titles, titles[index2]);
		gd.addNumericField("X Location: ", 0, 0);
		gd.addNumericField("Y Location: ", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		index1 = gd.getNextChoiceIndex();
		index2 = gd.getNextChoiceIndex();
		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		String title1 = titles[index1];
		String title2 = titles[index2];
		ImagePlus imp1 = WindowManager.getImage(wList[index1]);
		ImagePlus imp2 = WindowManager.getImage(wList[index2]);
		if (imp1.getType()!= imp2.getType()) {
			IJ.showMessage("Stack Inserter", "The source and destination must be the same type.");
			return;
		}
		if (imp2.getStackSize()==1) {
			IJ.showMessage("Stack Inserter", "The destination must be a stack.");
			return;
		}
		if (imp1== imp2) {
			IJ.showMessage("Stack Inserter", "The source and destination must be different.");
			return;
		}
		insert(imp1, imp2, x, y);
		IJ.register(Stack_Inserter.class);
	}
	
	void insert(ImagePlus imp1, ImagePlus imp2, int x, int y) {
		ImageStack stack1 = imp1.getStack();
		ImageStack stack2 = imp2.getStack();
		int size1 = stack1.getSize();
		int size2 = stack2.getSize();
		ImageProcessor ip1, ip2;
		for (int i=1; i<=size2; i++) {
			ip1 = stack1.getProcessor(i<=size1?i:size1);
			ip2 = stack2.getProcessor(i);
			ip2.insert(ip1, x, y);
			stack2.setPixels(ip2.getPixels(), i);
		}
		imp2.setStack(null, stack2);
	}


}
