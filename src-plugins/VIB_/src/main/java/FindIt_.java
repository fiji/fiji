/*
 * This plugin helps solve FindIt type puzzles.
 *
 * FindIt displays two pictures side by side, where one of them is the original,
 * and the other contains small errors. The task is to find the errors.
 *
 * Some TV channels show FindIt puzzles late in the night (which can be recorded
 * by executing "tools/tv-snap.sh snap.jpg").
 *
 * The output of this plugin is a stack of the input image, an image where the
 * supposed original and copy are exchanged, and the difference image.
 *
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class FindIt_ implements PlugInFilter {
	ImagePlus imp;
	int width,height,fuzz,minOffset,maxOffset,samplingFactor,samplingBorder;
	int[] pixels;
	boolean debug;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;

		return DOES_ALL;
	}

	/*
	 * This method tells if the pixels' values match.
	 */
	boolean matches(int x1,int y1,int x2,int y2) {
		int bpp=3;
		int value1=pixels[x1+width*y1];
		int value2=pixels[x2+width*y2];
		for(int i=0;i<bpp*8;i+=8)
			if(Math.abs(((value1>>i)&0xff)-((value2>>i)&0xff))>fuzz)
				return false;
		return true;
	}

	/*
	 * This method tries to find the most likely horizontal
	 * offset of the right image. It only looks for values
	 * between min and max. It builds a histogram over these
	 * values by sampling every factor'th pixel in x and y
	 * direction. Two pixels match if all of their channels
	 * differ by at most fuzz.
	 */
	int offset;
	double find_offset(int min,int max,int factor) {
		int[] histogram=new int[max+1-min];
		int count=0,maxIndex=0;
		for(int i=min;i<=max;i++) {
			for(int x=factor/2;x<width-i;x+=factor)
				for(int y=factor/2;y<height;y+=factor)
					if(matches(x,y,x+i,y) &&
							!matches(x,y,x+samplingBorder,y))
						histogram[i-min]++;
			count+=histogram[i-min];
			if(histogram[maxIndex]<histogram[i-min])
				maxIndex=i-min;
		}
		offset=min+maxIndex;
		return (histogram[maxIndex]/(double)count);
	}

	int top,left,right,bottom;

	// find top left corner
	boolean find_top_left_corner(int minWidth,int minHeight) {
		int errors=5,error;
		for(int x=0;x<width-offset;x++)
			for(int y=0;y<height-minHeight;y++) {
				int h;
				for(h=0,error=0;h<minHeight
					&& matches(x,y+h,x+offset,y+h)
					&& (matches(x+minWidth,y+h,x+minWidth+offset,y+h)
						|| ++error<errors);
					h++,error=0);
				if(h>=minHeight) {
					top=y;
					left=x;
					return true;
				}
				y+=h;
			}
		return false;
	}

	// find the left rectangle
	boolean find_rectangle(int minWidth,int minHeight) {
		if(!find_top_left_corner(minWidth,minHeight))
			return false;

		int border=2,errors=3,error;
		if(debug)
			IJ.write("found ("+left+","+top+")");
		for(right=left+minWidth,error=0;right<left+offset-2*border &&
			((matches(right,top+border,right+bottom,top+border)
			  && matches(right,top+2*border,right+bottom,top+2*border))
			 || ++error<errors);right++,error=0);
		if(debug)
			IJ.write("found right: "+right);
		if(right==left+minWidth)
			return false;
		/* for(bottom=top+minHeight+border,error=0;bottom<height &&
			(matches(left+border,bottom,left+border+offset,bottom)
			 || +error<errors);bottom++,error=0); */
		for(bottom=top+minHeight+border,error=0;bottom<height
				&& error<(right-left)/2;bottom++) {
			error=0;
			for(int x=left;x<right;x++)
				if(!matches(x,bottom,x+offset,bottom))
					error++;
		}
		if(debug)
			IJ.write("found bottom: "+bottom);
		for(top--,error=0;top>0 && error<(right-left)/2;top--) {
			error=0;
			for(int x=left;x<right;x++)
				if(!matches(x,top,x+offset,top))
					error++;
		}
		top++;
		if(debug)
			IJ.write("found new top: "+top);
		return true;
	}

	void add_slices(ImageStack stack) {
		int[] p2=new int[pixels.length];
		int[] p3=new int[pixels.length];
		for(int i=0;i<pixels.length;i++) {
			p2[i]=pixels[i];
			p3[i]=0;
		}
		for(int y=top;y<bottom;y++) {
			for(int x=left;x<right;x++) {
				int i=x+width*y;
				p2[i]=pixels[i+offset];
				p2[i+offset]=pixels[i];
				p3[i]=0;
				for(int j=0;j<3*8;j+=8)
					p3[i]|=Math.abs(((p2[i]>>j)&0xff)
							-((pixels[i]>>j)&0xff))<<j;
			}
			p3[left+width*y]=-1;
			p3[right-1+width*y]=-1;
			
		}
		stack.addSlice("", new ij.process.ColorProcessor(width,height,pixels));
		stack.addSlice("", new ij.process.ColorProcessor(width,height,p2));
		stack.addSlice("", new ij.process.ColorProcessor(width,height,p3));
	}

	public void run(ImageProcessor ip) {
		ip=ip.convertToRGB();
		width=ip.getWidth();
		height=ip.getHeight();
		pixels=(int[])ip.getPixels();
		debug=false;

		GenericDialog gd=new GenericDialog("Parameters");
		gd.addNumericField("Fuzz", 25, 0);
		gd.addNumericField("MinOffset", width/7, 0);
		gd.addNumericField("MaxOffset", width/2, 0);
		gd.addNumericField("SamplingFactor", width/40, 0);
		gd.addNumericField("SamplingBorder", 20, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		fuzz=(int)gd.getNextNumber();
		minOffset=(int)gd.getNextNumber();
		maxOffset=(int)gd.getNextNumber();
		samplingFactor=(int)gd.getNextNumber();
		samplingBorder=(int)gd.getNextNumber();

		double confidence=find_offset(minOffset,maxOffset,samplingFactor);
		if(debug)
			IJ.write("Offset is "+offset+" ("+(confidence*100)+"%)");
		if(find_rectangle(offset/2,offset/2)) {
			ImageStack stack=new ImageStack(width,height);
			add_slices(stack);
			new ImagePlus("result",stack).show();
		}
	}

}
