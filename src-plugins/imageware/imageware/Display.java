package imageware;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.process.ColorProcessor;

/**
 * Class Display.
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class Display {

	/**
	* Shows a imageware with a specifc title.
	*
	* @param title      a specific title
	* @param ds       	the imageware to be shown
	*/
	static public void show(String title, ImageWare ds) {
		(new ImagePlus(title, ds.buildImageStack())).show();
	}
	
	/**
	* Shows color image composed by three datasets with a specifc title.
	*
	* @param title      a specific title
	* @param red       	the imageware to be shown in the red channel
	* @param green     	the imageware to be shown in the green channel
	* @param blue      	the imageware to be shown in the blue channel
	*/
	static public void showColor(String title, ImageWare red, ImageWare green, ImageWare blue) {
		(new ImagePlus(title, buildColor(red, green, blue))).show();
	}

	/**
	* Shows a imageware with a specifc title and with a specific magnification.
	*
	* @param title     		a specific title
	* @param ds       		the imageware to be shown
	* @param magnification  zoom factor
	*/
	static public void show(String title, ImageWare ds, double magnification) {
		ImagePlus imp = new ImagePlus(title, ds.buildImageStack());
		imp.show();
		ImageWindow win = imp.getWindow();
		ImageCanvas canvas = win.getCanvas();
		canvas.setMagnification(magnification);
		canvas.setDrawingSize(
			(int)Math.ceil(ds.getWidth()*magnification), 
			(int)Math.ceil(ds.getHeight()*magnification));
		win.pack();
		imp.updateAndRepaintWindow();
	}
	
	/**
	* Shows color image composed by three datasets with a specifc title and with a specific magnification.
	*
	* @param title      a specific title
	* @param red       	the imageware to be shown in the red channel
	* @param green     	the imageware to be shown in the green channel
	* @param blue      	the imageware to be shown in the blue channel
	* @param magnification  zoom factor
	*/
	static public void showColor(String title, ImageWare red, ImageWare green, ImageWare blue, double magnification) {
		ImagePlus imp = new ImagePlus(title, buildColor(red, green, blue));
		imp.show();
		ImageWindow win = imp.getWindow();
		ImageCanvas canvas = win.getCanvas();
		canvas.setMagnification(magnification);
		canvas.setDrawingSize(
			(int)Math.ceil(red.getWidth()*magnification), 
			(int)Math.ceil(red.getHeight()*magnification));
		win.pack();
		imp.updateAndRepaintWindow();
	}
	
	/**
	*/
	static private ImageStack buildColor(ImageWare red, ImageWare green, ImageWare blue) {
		if (!red.isSameSize(green)) {
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to create a ImageStack the channel are not the same size.\n" + 
				"[" + red.getSizeX() + "," + red.getSizeY() + "," + red.getSizeZ() + "] != " +
				"[" + green.getSizeX() + "," + green.getSizeY() + "," + green.getSizeZ() + "].\n" +
				"-------------------------------------------------------\n"
			);
		}
		if (!red.isSameSize(blue)) {
			throw new ArrayStoreException(
				"\n-------------------------------------------------------\n" +
				"Error in imageware package\n" +
				"Unable to create a ImageStack the channel are not the same size.\n" + 
				"[" + red.getSizeX() + "," + red.getSizeY() + "," + red.getSizeZ() + "] != " +
				"[" + blue.getSizeX() + "," + blue.getSizeY() + "," + blue.getSizeZ() + "].\n" +
				"-------------------------------------------------------\n"
			);
		}
		int nx = red.getSizeX();
		int ny = red.getSizeY();
		int nz = red.getSizeZ();
		int nxy = nx*ny;
		ImageStack imagestack = new ImageStack(nx, ny);
		ColorProcessor cp;
		byte[] r = new byte[nxy];
		byte[] g = new byte[nxy];
		byte[] b = new byte[nxy];
		for (int z=0; z<nz; z++) {
			cp = new ColorProcessor(nx, ny);
			switch(red.getType()) {
			case ImageWare.DOUBLE:
				double[] dpixred = red.getSliceDouble(z);
				for (int k=0; k<nxy; k++)
					r[k] = (byte)(dpixred[k]);
				break;
			case ImageWare.FLOAT:
				float[] fpixred = red.getSliceFloat(z);
				for (int k=0; k<nxy; k++)
					r[k] = (byte)(fpixred[k]);
				break;
			case ImageWare.SHORT:
				short[] spixred = red.getSliceShort(z);
				for (int k=0; k<nxy; k++)
					r[k] = (byte)(spixred[k]);
				break;
			case ImageWare.BYTE:
				r = red.getSliceByte(z);
				break;
			}
			switch(green.getType()) {
			case ImageWare.DOUBLE:
				double[] dpixgreen = green.getSliceDouble(z);
				for (int k=0; k<nxy; k++)
					g[k] = (byte)(dpixgreen[k]);
				break;
			case ImageWare.FLOAT:
				float[] fpixgreen = green.getSliceFloat(z);
				for (int k=0; k<nxy; k++)
					g[k] = (byte)(fpixgreen[k]);
				break;
			case ImageWare.SHORT:
				short[] spixgreen = green.getSliceShort(z);
				for (int k=0; k<nxy; k++)
					g[k] = (byte)(spixgreen[k]);
				break;
			case ImageWare.BYTE:
				g = green.getSliceByte(z);
				break;
			}
			switch(blue.getType()) {
			case ImageWare.DOUBLE:
				double[] dpixblue = blue.getSliceDouble(z);
				for (int k=0; k<nxy; k++)
					b[k] = (byte)(dpixblue[k]);
				break;
			case ImageWare.FLOAT:
				float[] fpixblue = blue.getSliceFloat(z);
				for (int k=0; k<nxy; k++)
					b[k] = (byte)(fpixblue[k]);
				break;
			case ImageWare.SHORT:
				short[] spixblue = blue.getSliceShort(z);
				for (int k=0; k<nxy; k++)
					b[k] = (byte)(spixblue[k]);
				break;
			case ImageWare.BYTE:
				b = blue.getSliceByte(z);
				break;
			}
			cp.setRGB(r, g, b);
			imagestack.addSlice("" + z, cp);
		}
		return imagestack;
	}

}