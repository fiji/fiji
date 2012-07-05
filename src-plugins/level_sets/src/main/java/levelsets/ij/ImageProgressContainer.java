package levelsets.ij;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/*
 * From Stephan Saalfeld pseudocode:
 *      
 *      ij.process.ImageProcessor ip;
      
      ColorProcessor ipProgress = ( ColorProcessor )ip.duplicate().convertToRGB();
      ImagePlus progress = new ImagePlus( "Progress", ipProgress );
      
      ipProgres.draw...copy_images
      
      progress.updateAndDraw();
      

 */


public class ImageProgressContainer extends ImageContainer {

	protected int [][] cproc_pixels;

	public ImageProgressContainer() {}
	
	
	public ImageProgressContainer(ImageContainer cont) {		
		duplicateImages(cont.sproc);
		if (null != this.ip && null != cont.ip) {
			this.ip.setSlice(cont.ip.getCurrentSlice());
		}
	}
	
	
	public void duplicateImages( ImageContainer cont ) {
		duplicateImages(cont.sproc);
		if (null != this.ip && null != cont.ip) {
			this.ip.setSlice(cont.ip.getCurrentSlice());
		}
	}
	
	public void duplicateImages(ImageProcessor [] iproc) {
		stack_size = iproc.length;

		isStack = stack_size > 1 ? true : false;

		sproc = new ColorProcessor[stack_size];
		cproc_pixels = new int[stack_size][];

		for ( int i = 0; i < stack_size; i++ ) {
			sproc[i] = ( ColorProcessor )iproc[i].duplicate().convertToRGB();
			cproc_pixels[i] = (int []) sproc[i].getPixels();
		}

		width = sproc[0].getWidth();
		
		updateImagePlus(null);

	}
	
    

	public void showProgressStep() {
		ip.show();
		ip.updateAndDraw();
	}

	
	// TODO Should actually try to overlay instead of painting over.....
	public void setPixel(int x, int y, int z, int [] pixel)
	{		
		int ij_pixelval = pixel[0] << 16 | pixel[1] << 8 | pixel[2];
			
		cproc_pixels[z][y * width + x] = ij_pixelval;
	}

}
