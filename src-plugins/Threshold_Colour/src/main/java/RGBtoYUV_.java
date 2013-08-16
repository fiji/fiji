import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;



public class RGBtoYUV_ implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_RGB;
	}


	public void run(ImageProcessor ip) {

		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int c, x, y, i=0, Y, U, V, r, g, b;
		double yf;


		ImagePlus imp = WindowManager.getCurrentImage();

		for(y=0;y<ye;y++){
			for (x=0;x<xe;x++){
				c=ip.getPixel(x,y);

					r = ((c&0xff0000)>>16);//R
					g = ((c&0x00ff00)>>8);//G
					b = ( c&0x0000ff); //B 

					// Kai's plugin
					yf = (0.299 * r  + 0.587 * g + 0.114 * b);
					Y = ((int)Math.floor(yf + 0.5)) ;
					U = (128+(int)Math.floor((0.493 *(b - yf))+ 0.5)); 
					V = (128+(int)Math.floor((0.877 *(r - yf))+ 0.5)); 

					ip.putPixel(x,y, (((Y<0?0:Y>255?255:Y) & 0xff) << 16)+
									 (((U<0?0:U>255?255:U) & 0xff) << 8) +
								 	  ((V<0?0:V>255?255:V) & 0xff));
//old
//				// RGB
//				R = ((c&0xff0000)>>16); //R
//				G = ((c&0x00ff00)>>8); //G 
//				B = ( c&0x0000ff); //B
//				Y = ((  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
//				U = (( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
//				V = (( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;
				
				ip.putPixel(x,y, ((Y & 0xff) <<16) + ((U & 0xff) << 8) + ( V & 0xff));
			}
		}
	}
/*
run("RGB Stack");
run("Convert Stack to Images");
selectWindow("Red");
run("Rename...", "title=Y");
selectWindow("Green");
run("Rename...", "title=U");
selectWindow("Blue");
run("Rename...", "title=V");
*/

	void showAbout() {
		IJ.showMessage("About RGBtoYUV...",
		"RGBtoYUV by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"Converts from RGB to YUV and stores the results in\n"+
		"the same RGB image R=Y, G=U, B=V");
	}


}
