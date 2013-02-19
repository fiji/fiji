package math3d;

import ij.IJ;
import ij.plugin.PlugIn;

import java.util.Arrays;

public class Transform_IO extends TransformIO implements PlugIn {
	public void run(String arg) {
		// Only for testing
		float[] mat;
		if(arg.equals("")) mat=openAffineTransform();
		else mat=openAffineTransform(arg);
		IJ.log("fields:="+getFields());
		IJ.log("tags:="+getTags());
		IJ.log("mat = "+Arrays.toString(mat));
		//saveAffineTransform(mat);
	}
}
