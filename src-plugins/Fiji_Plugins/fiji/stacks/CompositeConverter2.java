package fiji.stacks;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ColorProcessor;

import java.awt.Point;

/**
 * This is an incredibly stupid class, but I need the reference to the new ImagePlus, that's why I rewrote the CompositeConverter class here ...
 * 
 * @author Stephan Preibisch
 *
 */
public class CompositeConverter2
{
	public ImagePlus makeComposite( final ImagePlus imp ) 
	{
		if ( imp.isComposite() ) 
		{
			CompositeImage ci = (CompositeImage)imp;
			if (ci.getMode()!=CompositeImage.COMPOSITE) {
				ci.setMode(CompositeImage.COMPOSITE);
				ci.updateAndDraw();
			}
			return imp;
		}
		
		int z = imp.getStackSize();
		int c = imp.getNChannels();
		if ( c == 1 ) 
		{
			c = z;
			imp.setDimensions( c, 1, 1 );
		}
		
		if ( imp.getBitDepth()==24 ) 
		{
			Roi roi = imp.getRoi();
			ImagePlus newImp;
			
			if ( z > 1 )
				newImp = convertRGBToCompositeStack( imp );
			else
				newImp = convertRGBToCompositeImage( imp );
			
			newImp.setRoi( roi );
			return newImp;
		}
		else
		{
			return null;
		}
	}
	
	ImagePlus convertRGBToCompositeImage(ImagePlus imp) 
	{
		ImageWindow win = imp.getWindow();
		Point loc = win!=null?win.getLocation():null;
		ImagePlus imp2 = new CompositeImage(imp, CompositeImage.COMPOSITE);
		if (loc!=null) ImageWindow.setNextLocation(loc);
		imp2.show();
		imp.hide();
		WindowManager.setCurrentWindow(imp2.getWindow());
		return imp2;
	}

	ImagePlus convertRGBToCompositeStack( ImagePlus imp ) 
	{
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack1 = imp.getStack();
		int n = stack1.getSize();
		ImageStack stack2 = new ImageStack(width, height);
		for (int i=0; i<n; i++) {
			ColorProcessor ip = (ColorProcessor)stack1.getProcessor(1);
			stack1.deleteSlice(1);
			byte[] R = new byte[width*height];
			byte[] G = new byte[width*height];
			byte[] B = new byte[width*height];
			ip.getRGB(R, G, B);
			stack2.addSlice(null, R);
			stack2.addSlice(null, G);
			stack2.addSlice(null, B);
		}
		n *= 3;
		imp.changes = false;
		ImageWindow win = imp.getWindow();
		Point loc = win!=null?win.getLocation():null;
		ImagePlus imp2 = new ImagePlus(imp.getTitle(), stack2);
		imp2.setDimensions(3, n/3, 1);
		int mode = CompositeImage.COMPOSITE;
 		imp2 = new CompositeImage(imp2, mode);
		if (loc!=null) ImageWindow.setNextLocation(loc);
		imp2.show();
		imp.changes = false;
		imp.close();
		return imp2;
	}

}
