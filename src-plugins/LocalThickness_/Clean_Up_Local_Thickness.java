import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;

/* Bob Dougherty August 1, 2007

Input: 3D Local Thickness map (32-bit stack)
Output: Same as input with border voxels corrected for "jaggies." Non-background voxels
adjacent to background voxels are have their local thickness values replaced by the average of
their non-background neighbors that do not border background points.

August 10.  Version 3 This version also multiplies the local thickness by 2 to conform with the
offficial definition of local thickness.  

 License:
	Copyright (c)  2007, OptiNav, Inc.
	All rights reserved.

	Redistribution and use in source and binary forms, with or without
	modification, are permitted provided that the following conditions
	are met:

		Redistributions of source code must retain the above copyright
	notice, this list of conditions and the following disclaimer.
		Redistributions in binary form must reproduce the above copyright
	notice, this list of conditions and the following disclaimer in the
	documentation and/or other materials provided with the distribution.
		Neither the name of OptiNav, Inc. nor the names of its contributors
	may be used to endorse or promote products derived from this software
	without specific prior written permission.

	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
	PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
public class Clean_Up_Local_Thickness implements  PlugInFilter {
	private ImagePlus imp;
	public float[][] s, sNew;
	public int w,h,d;

	public int setup(String arg, ImagePlus imp) {
 		this.imp = imp;
		return DOES_32;
	}
	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		w = stack.getWidth();
		h = stack.getHeight();
		d = imp.getStackSize();
		//Create 32 bit floating point stack for output, sNew.
		ImageStack newStack = new ImageStack(w,h);
		sNew = new float[d][];
		for(int k = 0; k < d; k++){
			ImageProcessor ipk = new FloatProcessor(w,h);
			newStack.addSlice(null,ipk);
			sNew[k] = (float[])ipk.getPixels();
		}
		//Create reference to input data
		s = new float[d][];
		for (int k = 0; k < d; k++)s[k] = (float[])stack.getPixels(k+1);
		//First set the output array to flags:
		// 0 for a background point
		// -1 for a non-background point that borders a background point
		// s (input data) for an interior non-background point
		for (int k = 0; k < d; k++){
			for (int j = 0; j < h; j++){
				for (int i = 0; i < w; i++){
					sNew[k][i + w*j] = setFlag(i,j,k);
				}//i
			}//j
		}//k
		//Process the surface points.  Initially set results to negative values
		//to be able to avoid including them in averages of for subsequent points.
		//During the calculation, positve values in sNew are interior non-background
		//local thicknesses.  Negative values are surface points.  In this case the
		//value might be -1 (not processed yet) or -result, where result is the
		//average of the neighboring interior points.  Negative values are excluded from
		//the averaging.
		for (int k = 0; k < d; k++){
			for (int j = 0; j < h; j++){
				for (int i = 0; i < w; i++){
					int ind = i + w*j;
					if(sNew[k][ind] == -1){
						sNew[k][ind] = -averageInteriorNeighbors(i,j,k);
					}
				}//i
			}//j
		}//k
		//Fix the negative values and double the results
		for (int k = 0; k < d; k++){
			for (int j = 0; j < h; j++){
				for (int i = 0; i < w; i++){
					int ind = i + w*j;
					sNew[k][ind] = (float)Math.abs(sNew[k][ind]);
				}//i
			}//j
		}//k
		IJ.showStatus("Clean Up Local Thickness complete");
		String title = stripExtension(imp.getTitle());
		ImagePlus impOut = new ImagePlus(title+"_CL",newStack);
		impOut.getProcessor().setMinAndMax(0,2*imp.getProcessor().getMax());
		impOut.show();
		IJ.run("Fire");
	}
	float setFlag(int i,int j,int k){
		if(s[k][i+w*j]==0)return 0;
		//change 1
		if(look(i,j,k-1)==0)return -1;
		if(look(i,j,k+1)==0)return -1;
		if(look(i,j-1,k)==0)return -1;
		if(look(i,j+1,k)==0)return -1;
		if(look(i-1,j,k)==0)return -1;
		if(look(i+1,j,k)==0)return -1;
		//change 1 before plus
		if(look(i,j+1,k-1)==0)return -1;
		if(look(i,j+1,k+1)==0)return -1;
		if(look(i+1,j-1,k)==0)return -1;
		if(look(i+1,j+1,k)==0)return -1;
		if(look(i-1,j,k+1)==0)return -1;
		if(look(i+1,j,k+1)==0)return -1;
		//change 1 before minus
		if(look(i,j-1,k-1)==0)return -1;
		if(look(i,j-1,k+1)==0)return -1;
		if(look(i-1,j-1,k)==0)return -1;
		if(look(i-1,j+1,k)==0)return -1;
		if(look(i-1,j,k-1)==0)return -1;
		if(look(i+1,j,k-1)==0)return -1;
		//change 3, k+1
		if(look(i+1,j+1,k+1)==0)return -1;
		if(look(i+1,j-1,k+1)==0)return -1;
		if(look(i-1,j+1,k+1)==0)return -1;
		if(look(i-1,j-1,k+1)==0)return -1;
		//change 3, k-1
		if(look(i+1,j+1,k-1)==0)return -1;
		if(look(i+1,j-1,k-1)==0)return -1;
		if(look(i-1,j+1,k-1)==0)return -1;
		if(look(i-1,j-1,k-1)==0)return -1;
		return s[k][i+w*j];
	}
	float averageInteriorNeighbors(int i,int j,int k){
		int n = 0;
		float sum = 0;
		//change 1
		float value = lookNew(i,j,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i,j,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i,j-1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i,j+1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j,k);
		if(value > 0){
			n++;
			sum += value;
		}
		//change 1 before plus
		value = lookNew(i,j+1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i,j+1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j-1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j+1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		//change 1 before minus
		value = lookNew(i,j-1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i,j-1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j-1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j+1,k);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		//change 3, k+1
		value = lookNew(i+1,j+1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j-1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j+1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j-1,k+1);
		if(value > 0){
			n++;
			sum += value;
		}
		//change 3, k-1
		value = lookNew(i+1,j+1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i+1,j-1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j+1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		value = lookNew(i-1,j-1,k-1);
		if(value > 0){
			n++;
			sum += value;
		}
		if(n > 0)return sum/n;
		return s[k][i+w*j];
	}
	float look(int i,int j,int k){
		if((i < 0)||(i >= w))return -1;
		if((j < 0)||(j >= h))return -1;
		if((k < 0)||(k >= d))return -1;
		return s[k][i+w*j];
	}
	//A positive result means this is an interior, non-background, point.
	float lookNew(int i,int j,int k){
		if((i < 0)||(i >= w))return -1;
		if((j < 0)||(j >= h))return -1;
		if((k < 0)||(k >= d))return -1;
		return  sNew[k][i+w*j];
	}

	//Modified from ImageJ code by Wayne Rasband
    String stripExtension(String name) {
        if (name!=null) {
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex>=0)
                name = name.substring(0, dotIndex);
		}
		return name;
    }
}