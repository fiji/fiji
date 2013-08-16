import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/**
 * Interleaves two stacks to create a new stack
 * This plugin is modified from Stack_Combiner (see Wayne Rasband)
 * @author J. Anthony Parker, MD PhD <J.A.Parker@IEEE.org>
 * @version 20September2001
 */

public class Stack_Interleaver implements PlugIn {

		ImagePlus imp1;
		ImagePlus imp2;

	public void run(String arg) {
		if (!showDialog())
			return;
		if (imp1.getType()!=imp2.getType())
			{error(); return;}
		ImageStack stack1 = imp1.getStack();
		ImageStack stack2 = imp2.getStack();
		ImageStack stack3 = interleave(stack1, stack2);
		new ImagePlus("Combined Stacks", stack3).show();
		IJ.register(Stack_Interleaver.class);
	}
	
	public ImageStack interleave(ImageStack stack1, ImageStack stack2) {
		int d1 = stack1.getSize();
		int d2 = stack2.getSize();
		int d3 = d1 + d2;
		int w1 = stack1.getWidth();
		int h1 = stack1.getHeight();
 		int w2 = stack2.getWidth();
		int h2 = stack2.getHeight();
		int w3 = Math.max(w1, w2);
		int h3 = Math.max(h1, h2);
		ImageStack stack3 = new ImageStack(w3, h3, stack1.getColorModel());
		ImageProcessor ip = stack1.getProcessor(1);
		ImageProcessor ip3;
		Color background = Toolbar.getBackgroundColor();
 		for (int i=1; i<=d3; i++) {
 			IJ.showProgress((double)i/d3);
 			ip3 = ip.createProcessor(w3, h3);
 			if  (i<=d1) {
				if (h1<h3 || w1<w3) {
					ip3.setColor(background);
					ip3.fill();
				}
				ip3.insert(stack1.getProcessor(i),0,0);
				stack3.addSlice(null, ip3);
			}
 			ip3 = ip.createProcessor(w3, h3);
			if  (i<=d2) {
				if (h2<h3 || w2<w3) {
					ip3.setColor(background);
					ip3.fill();
				}
				ip3.insert(stack2.getProcessor(i),0,0);
				stack3.addSlice(null, ip3);
			}
		}
		return stack3;
	}
	
		public boolean showDialog() {
		int[] wList = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			error();
			return false;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}

		GenericDialog gd = new GenericDialog("Interleaver");
		gd.addChoice("Stack 1:", titles, titles[0]);
		gd.addChoice("Stack 2:", titles, titles[1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		int index1 = gd.getNextChoiceIndex();
		int index2 = gd.getNextChoiceIndex();
		imp1 = WindowManager.getImage(wList[index1]);
		imp2 = WindowManager.getImage(wList[index2]);
		return true;
	}

	
	void error() {
		IJ.showMessage("Stack_Interleaver", "This plugin requires two stacks\n"
			+"that are the same data type.");
	}

}

