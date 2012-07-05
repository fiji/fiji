import amira.AmiraParameters;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.plugin.filter.*;

/* This plugin takes a binned image as input. It then reassigns equally spaced
   gray values to the pixels. */
public class CropEditor_ implements PlugInFilter {
	ImagePlus image;
	ImageStack stack;
	int w,h,d;
	boolean verbose=false;

	private static int getPixel(byte[] b,int index) {
		return b[index] & 0xff;
	}

	public void run(ImageProcessor ip) {
		stack=image.getStack();
		if(stack==null)
			d=1;
		else
			d=stack.getSize();
		w=image.getWidth();
		h=image.getHeight();
		GenericDialog gd = new GenericDialog("Parameters");
		gd.addNumericField("xStart", 0, 0);
		gd.addNumericField("xEnd", w-1, 0);
		gd.addNumericField("yStart", 0, 0);
		gd.addNumericField("yEnd", h-1, 0);
		gd.addNumericField("zStart", 0, 0);
		gd.addNumericField("zEnd", d-1, 0);
		gd.addNumericField("fillValue", 0, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int xStart = (int)gd.getNextNumber();
		int xEnd = (int)gd.getNextNumber()+1;
		int yStart = (int)gd.getNextNumber();
		int yEnd = (int)gd.getNextNumber()+1;
		int zStart = (int)gd.getNextNumber();
		int zEnd = (int)gd.getNextNumber()+1;
		byte fillValue = (byte)gd.getNextNumber();

		if(xStart>=xEnd || yStart>=yEnd || zStart>=zEnd) {
			IJ.error("Invalid dimensions");
			return;
		}

		int newW=xEnd-xStart,newH=yEnd-yStart,newD=zEnd-zStart;
		ImageStack result = new ImageStack(newW,newH);

		byte[] backGround=null;
		if(fillValue!=0) {
			backGround=new byte[newW*newH];
			for(int i=0;i<newW*newH;i++)
				backGround[i]=fillValue;
		}

		for(int z=zStart;z<0;z++) {
			byte[] pixels=new byte[newW*newH];
			if(backGround!=null)
				System.arraycopy(backGround,0,pixels,0,newW*newH);
			result.addSlice(null,pixels);
		}

		for(int z=0;z<zEnd && z<d;z++) {
			byte[] oldPixels=(byte[])stack.getProcessor(z+1).getPixels();
			byte[] pixels=new byte[newW*newH];

			if(w==newW && h==newH)
				System.arraycopy(oldPixels,0,pixels,0,newW*newH);
			else {
				if(backGround!=null)
					System.arraycopy(backGround,0,pixels,0,newW*newH);
				for(int y=(yStart<0?-yStart:0);y<newH && y+yStart<h;y++)
					for(int x=(xStart<0?-xStart:0);x<newW && x+xStart<w;x++)
						pixels[x+newW*y]=oldPixels[x+xStart+w*(y+yStart)];
			}
			result.addSlice(null,pixels);
		}

		for(int z=d;z<zEnd;z++) {
			byte[] pixels=new byte[newW*newH];
			if(backGround!=null)
				System.arraycopy(backGround,0,pixels,0,newW*newH);
			result.addSlice(null,pixels);
		}

		ImagePlus r = new ImagePlus(image.getTitle()+" cropped",result);
		if (AmiraParameters.isAmiraMesh(image))
			new AmiraParameters(image).setParameters(r);
		if (IJ.getInstance() != null)
			r.show();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		// TODO: handle 16-bit and 32-bit
		return DOES_8G | DOES_8C | NO_CHANGES;
	}
}

