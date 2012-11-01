import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;



public class RGBtoLab_ implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_RGB;
	}


	public void run(ImageProcessor ip) {
		// see:
		//http://www.brucelindbloom.com/index.html?WorkingSpaceInfo.html#Specifications
		//http://www.easyrgb.com/math.php?MATH=M7#text7
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int c, x, y, i=0;
		double rf, gf, bf;
		double X, Y, Z, fX, fY, fZ;
		double La, aa, bb;
		double ot=1/3.0, cont = 16/116.0;
		int Li, ai, bi;
		ImagePlus imp = WindowManager.getCurrentImage();

		for(y=0;y<ye;y++){
			for (x=0;x<xe;x++){
				c=ip.getPixel(x,y);

				// RGB to XYZ
				rf = ((c&0xff0000)>>16)/255.0; //R 0..1
				gf = ((c&0x00ff00)>>8)/255.0; //G 0..1
				bf = ( c&0x0000ff)/255.0; //B 0..1

				//white reference D65 PAL/SECAM
				X = 0.430587 * rf + 0.341545 * gf + 0.178336 * bf;
				Y = 0.222021 * rf + 0.706645 * gf + 0.0713342* bf;
				Z = 0.0201837* rf + 0.129551 * gf + 0.939234 * bf;
//var_X = X /  95.047          //Observer = 2, Illuminant = D65
//var_Y = Y / 100.000
//var_Z = Z / 108.883

				// XYZ to Lab
				if ( X > 0.008856 )
					fX =  Math.pow(X, ot);
				else
					fX = ( 7.78707 * X ) + cont;//7.7870689655172

				if ( Y > 0.008856 )
					fY = Math.pow(Y, ot);
				else
					fY = ( 7.78707 * Y ) + cont;

				if ( Z > 0.008856 )
					fZ =  Math.pow(Z, ot);
				else
					fZ = ( 7.78707 * Z ) + cont;

				La = ( 116 * fY ) - 16;
				aa = 500 * ( fX - fY );
				bb = 200 * ( fY - fZ );

				// Lab rescaled to the 0..255 range
				// a* and b* range from -120 to 120 in the 8 bit space
				La =  La * 2.55;
				aa =  Math.floor((1.0625 * aa + 128) + 0.5);
				bb =  Math.floor((1.0625 * bb + 128) + 0.5);

				// bracketing
				Li = (int)(La<0?0:(La>255?255:La));
				ai = (int)(aa<0?0:(aa>255?255:aa));
				bi = (int)(bb<0?0:(bb>255?255:bb));
				ip.putPixel(x,y, ((Li&0xff)<<16)+((ai&0xff)<<8)+(bi&0xff));
			}
		}
	}
/*
run("RGB Stack");
run("Convert Stack to Images");
selectWindow("Red");
run("Rename...", "title=L");
selectWindow("Green");
run("Rename...", "title=a");
selectWindow("Blue");
run("Rename...", "title=b");
run("LUT... ", "open=/home/gabriel/ImageJ/Lab_a.lut");
run("LUT... ", "open=/home/gabriel/ImageJ/Lab_b.lut");
*/

	void showAbout() {
		IJ.showMessage("About RGBtoLab...",
		"RGBtoLab by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"Converts from RGB to CIE L*a*b* and stores the results in the same\n"+
		"RGB image R=L*, G=a*, B=b*. Values are therfore offset and rescaled.");
	}

}
