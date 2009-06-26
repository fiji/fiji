import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/* Bob Dougherty 8/8/2006
Saito-Toriwaki algorithm for Euclidian Distance Transformation.
Direct application of Algorithm 1.
Version S1A: lower memory usage.
Version S1A.1 A fixed indexing bug for 666-bin data set
Version S1A.2 Aug. 9, 2006.  Changed noResult value.
Version S1B Aug. 9, 2006.  Faster.
Version S1B.1 Sept. 6, 2006.  Changed comments.
Version S1C Oct. 1, 2006.  Option for inverse case.
                           Fixed inverse behavior in y and z directions.
Version D July 30, 2007.  Multithread processing for step 2.

This version assumes the input stack is already in memory, 8-bit, and
outputs to a new 32-bit stack.  Versions that are more stingy with memory
may be forthcoming.

 License:
	Copyright (c) 2006, OptiNav, Inc.
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
public class EDT_S1D implements  PlugInFilter {
	private ImagePlus imp;
	public byte[][] data;
	public int w,h,d;
	public int thresh;
	public boolean inverse;

	public int setup(String arg, ImagePlus imp) {
 		this.imp = imp;
		return DOES_8G;
	}
	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		w = stack.getWidth();
		h = stack.getHeight();
		d = imp.getStackSize();
		int nThreads = Runtime.getRuntime().availableProcessors();

		if(!getScale())return;
		//Create references to input data
		data = new byte[d][];
		for (int k = 0; k < d; k++)data[k] = (byte[])stack.getPixels(k+1);
		//Create 32 bit floating point stack for output, s.  Will also use it for g in Transormation 1.
		ImageStack sStack = new ImageStack(w,h);
		float[][] s = new float[d][];
		for(int k = 0; k < d; k++){
			ImageProcessor ipk = new FloatProcessor(w,h);
			sStack.addSlice(null,ipk);
			s[k] = (float[])ipk.getPixels();
		}
		float[] sk;
		//Transformation 1.  Use s to store g.
		IJ.showStatus("EDT transformation 1/3");
		Step1Thread[] s1t = new Step1Thread[nThreads];
		for(int thread = 0; thread < nThreads; thread++){
			s1t[thread] = new Step1Thread(thread,nThreads,w,h,d,thresh,s,data);
			s1t[thread].start();
		}
		try{
			for(int thread = 0; thread< nThreads; thread++){
				s1t[thread].join();
			}
		}catch(InterruptedException ie){
			IJ.error("A thread was interrupted in step 1 .");
		}		
		//Transformation 2.  g (in s) -> h (in s)
		IJ.showStatus("EDT transformation 2/3");
		Step2Thread[] s2t = new Step2Thread[nThreads];
		for(int thread = 0; thread < nThreads; thread++){
			s2t[thread] = new Step2Thread(thread,nThreads,w,h,d,s);
			s2t[thread].start();
		}
		try{
			for(int thread = 0; thread< nThreads; thread++){
				s2t[thread].join();
			}
		}catch(InterruptedException ie){
			IJ.error("A thread was interrupted in step 2 .");
		}
		//Transformation 3. h (in s) -> s
		IJ.showStatus("EDT transformation 3/3");
		Step3Thread[] s3t = new Step3Thread[nThreads];
		for(int thread = 0; thread < nThreads; thread++){
			s3t[thread] = new Step3Thread(thread,nThreads,w,h,d,s,data);
			s3t[thread].start();
		}
		try{
			for(int thread = 0; thread< nThreads; thread++){
				s3t[thread].join();
			}
		}catch(InterruptedException ie){
			IJ.error("A thread was interrupted in step 3 .");
		}		
		//Find the largest distance for scaling
		//Also fill in the background values.
		float distMax = 0;
		int wh = w*h;
		float dist;
		for(int k = 0; k < d; k++){
			sk = s[k];
			for(int ind = 0; ind < wh; ind++){
				if(((data[k][ind]&255) < thresh)^inverse){
					sk[ind] = 0;
				}else{
					dist = (float)Math.sqrt(sk[ind]);
					sk[ind] = dist;
					distMax = (dist > distMax) ? dist : distMax;
				}
			}
		}

		IJ.showProgress(1.0);
		IJ.showStatus("Done");
		String title = stripExtension(imp.getTitle());
		ImagePlus impOut = new ImagePlus(title+"EDT",sStack);
		impOut.getProcessor().setMinAndMax(0,distMax);
		impOut.show();
		IJ.run("Fire");
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
	boolean getScale() {
		thresh = (int)Prefs.get("edtS1.thresh", 128);
		inverse = Prefs.get("edtS1.inverse", false);
		GenericDialog gd = new GenericDialog("EDT...", IJ.getInstance());
		gd.addNumericField("Threshold (1 to 255; value < thresh is background)", thresh, 0);
       	gd.addCheckbox("Inverse case (background when value >= thresh)",inverse);
		gd.showDialog();
		if (gd.wasCanceled())return false;
		thresh = (int)gd.getNextNumber();
      	inverse = gd.getNextBoolean();
		Prefs.set("edtS1.thresh", thresh);
		Prefs.set("edtS1.inverse", inverse);
		return true;
	}
	class Step1Thread extends Thread{
		int thread,nThreads,w,h,d,thresh;
		float[][] s; 
		byte[][] data;
		public Step1Thread(int thread, int nThreads, int w, int h, int d, int thresh, float[][] s,  byte[][] data){
			this.thread = thread;
			this.nThreads = nThreads;
			this.w = w;
			this.h = h;
			this.d = d;
			this.thresh = thresh;
			this.data = data;
			this.s = s;
		}
		public void run(){
			float[] sk;
			byte[] dk;
			int n = w;
			if(h > n) n = h;
			if(d > n) n = d;
			int noResult = 3*(n+1)*(n+1);
			boolean[] background = new boolean[n];			
			boolean nonempty;
			int test, min;			
			for(int k = thread; k < d; k+=nThreads){
				IJ.showProgress(k/(1.*d));
				sk = s[k];
				dk = data[k];
				for(int j = 0; j < h; j++){
					for (int i = 0; i < w; i++){
						background[i] = ((dk[i+w*j]&255) < thresh)^inverse;
					}
					for (int i = 0; i < w; i++){
						min = noResult;
						for (int x = i; x < w; x++){
							if(background[x]){
								test = i - x;
								test *= test;
								min = test;
								break;
							}
						}
						for (int x = i-1; x >=0 ; x--){
							if(background[x]){
								test = i - x;
								test *= test;
								if(test < min)min = test;
								break;
							}
						}
						sk[i+w*j] = min;
					}
				}
			}
		}//run
	}//Step1Thread
	class Step2Thread extends Thread{
		int thread,nThreads,w,h,d;
		float[][] s; 
		public Step2Thread(int thread, int nThreads, int w, int h, int d, float[][] s){
			this.thread = thread;
			this.nThreads = nThreads;
			this.w = w;
			this.h = h;
			this.d = d;
			this.s = s;
		}
		public void run(){
			float[] sk;
			int n = w;
			if(h > n) n = h;
			if(d > n) n = d;
			int noResult = 3*(n+1)*(n+1);
			int[] tempInt = new int[n];
			int[] tempS = new int[n];
			boolean nonempty;
			int test, min, delta;
			for(int k = thread; k < d; k+=nThreads){
				IJ.showProgress(k/(1.*d));
				sk = s[k];
				for (int i = 0; i < w; i++){
					nonempty = false;
					for (int j = 0; j < h; j++){
						tempS[j] = (int)sk[i+w*j];
						if(tempS[j] >0)nonempty = true;
					}
					if(nonempty){
						for (int j = 0; j < h; j++){
							min = noResult;
							delta = j;
							for(int y = 0; y < h; y++){
								test = tempS[y] + delta*delta--;
								if(test < min)min = test;
							}
							tempInt[j] = min;
						}
						for (int j = 0; j < h; j++){
							sk[i+w*j] = tempInt[j];
						}
					}
				}
			}
		}//run
	}//Step2Thread	
	class Step3Thread extends Thread{
		int thread,nThreads,w,h,d;
		float[][] s; 
		byte[][] data;
		public Step3Thread(int thread, int nThreads, int w, int h, int d, float[][] s, byte[][] data){
			this.thread = thread;
			this.nThreads = nThreads;
			this.w = w;
			this.h = h;
			this.d = d;
			this.s = s;
			this.data = data;
		}
		public void run(){
			int zStart,zStop,zBegin,zEnd;
			float[] sk;
			int n = w;
			if(h > n) n = h;
			if(d > n) n = d;
			int noResult = 3*(n+1)*(n+1);
			int[] tempInt = new int[n];
			int[] tempS = new int[n];
			boolean nonempty;
			int test, min, delta;
			for(int j = thread; j < h; j+=nThreads){
				IJ.showProgress(j/(1.*h));
				for(int i = 0; i < w; i++){
					nonempty = false;
					for(int k = 0; k < d; k++){
						tempS[k] = (int)s[k][i+w*j];
						if(tempS[k] >0)nonempty = true;
					}
					if(nonempty){
						zStart = 0;
						while((zStart < (d-1))&&(tempS[zStart] == 0))zStart++;
						if(zStart > 0)zStart--;
						zStop = d-1;
						while((zStop > 0)&&(tempS[zStop] == 0))zStop--;
						if(zStop < (d-1))zStop++;
	
						for(int k = 0; k < d; k++){
							//Limit to the non-background to save time,
							if(((data[k][i+w*j]&255) >= thresh)^inverse){
								min = noResult;
								zBegin = zStart;
								zEnd = zStop;
								if(zBegin > k)zBegin = k;
								if(zEnd < k)zEnd = k;
								delta = k - zBegin;
								for (int z = zBegin; z <= zEnd; z++){
									test = tempS[z] + delta*delta--;
									if(test < min)min = test;
									//min = (test < min) ? test : min;
								}
								tempInt[k] = min;
							}
						}
						for(int k = 0; k < d; k++){
							s[k][i+w*j] = tempInt[k];
						}
					}
				}
			}
		}//run
	}//Step2Thread	
}