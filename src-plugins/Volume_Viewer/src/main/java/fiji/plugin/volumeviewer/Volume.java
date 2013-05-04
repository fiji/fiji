/*
 * Volume Viewer 2.01
 * 01.12.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;


public class Volume {
	
	int widthV;		// size of the volume
	int heightV;
	int depthV;

	float xOffa;   	// center of the volume				
	float yOffa;  	 	   
	float zOffa; 

	byte[][][][] data3D = null;
	
	byte[][][] grad3D = null;
	
	byte[][][] mean3D = null;
	byte[][][] diff3D = null;

	byte[][][] col_3D = null;
	
	byte[][][] aPaint_3D = null;  	
	byte[][][] aPaint_3D2 = null;  	// -254 .. 254 

	byte[][][] nx_3D = null;
	byte[][][] ny_3D = null;
	byte[][][] nz_3D = null;

	private double a = 0, b = 1;
	private double min, max;

	boolean firstTime = true;

	int[][] histValGrad = new int[256][128]; // lum, grad
	int[][] histMeanDiff = new int[256][128]; 
	int[] histVal = new int[256];			// lum

	private ImageProcessor ip;
	private Control control;
	private ImagePlus imp;
	private Volume_Viewer vv; 
	

	public Volume(Control control, Volume_Viewer vv){
		this.control = control;
		this.vv = vv;
		this.imp = vv.imp;
		
		ip = imp.getProcessor();

		widthV = imp.getWidth();
		heightV = imp.getHeight();
		depthV = imp.getStackSize();

		// the volume data
		if (control.isRGB )
			data3D = new byte[4][depthV+4][heightV+4][widthV+4]; //z y x
		else
			data3D = new byte[1][depthV+4][heightV+4][widthV+4]; //z y x

		// these arrays could be shared 
		grad3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		// or
		mean3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		diff3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		// or
		aPaint_3D2 = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		aPaint_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		col_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		
		// for the illumination		
		nx_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		ny_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x
		nz_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		xOffa  = widthV/2.f;   				
		yOffa  = heightV/2.f;  	 	   
		zOffa  = depthV/2.f;

		getMinMax();

		readVolumeData();
	}
	
	void getMinMax() {
		min = ip.getMin();
		max = ip.getMax();

		Calibration cal = imp.getCalibration();

		if (cal != null) {
			if (cal.calibrated()) {

				min = cal.getCValue((int)min);
				max = cal.getCValue((int)max);

				double[] coef = cal.getCoefficients();
				if (coef != null) {		
					a = coef[0];
					b = coef[1];
				}
			}
			if (control.zAspect == 1) 
				control.zAspect = (float) (cal.pixelDepth / cal.pixelWidth);
		}

		if (control.zAspect == 0)
			control.zAspect = 1;
	}

	private void readVolumeData() {

		if (control.LOG) System.out.println("Read data");

		ImageStack stack = imp.getStack();

		int bitDepth = imp.getBitDepth();
		if (bitDepth == 8 || bitDepth == 16 || bitDepth == 32) {
			float scale = (float) (255f/(max-min));

			for (int z=1;z<=depthV;z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthV);
				IJ.showProgress(0.6*z/depthV);

				byte[] bytePixels = null;
				short[] shortPixels = null;
				float[] floatPixels = null;

				if (bitDepth == 8)
					bytePixels = (byte[]) stack.getPixels(z);
				else if (bitDepth == 16) 
					shortPixels = (short[]) stack.getPixels(z);
				else if ( bitDepth == 32)
					floatPixels = (float[]) stack.getPixels(z);

				int pos = 0;
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val;

						if (bitDepth == 32) {
							float value = (float) (floatPixels[pos++] - min);
							val = (int)(value*scale);
						}
						else if (bitDepth == 16) {
							val = (int) ((int)(0xFFFF & shortPixels[pos++])*b + a - min);
							val = (int)(val*scale);
						}
						else { // 8 bits 
							val = 0xff & bytePixels[pos++];
							val = (int)((val-min)*scale);
						}
						if (val<0f) val = 0;
						if (val>255) val = 255;

						data3D[0][z+1][y][x] = (byte)val; 
					}
					data3D[0][z+1][y][0] = data3D[0][z+1][y][1] = data3D[0][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[0][z+1][y][widthV+3] = data3D[0][z+1][y][widthV+2] = data3D[0][z+1][y][widthV+1]; 	// duplicate last 2 pixels
				}
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][z+1][0][x] = data3D[0][z+1][1][x] = data3D[0][z+1][2][x]; 						// duplicate first 2 rows
					data3D[0][z+1][heightV+3][x] = data3D[0][z+1][heightV+2][x] = data3D[0][z+1][heightV+1][x];	// duplicate last 2 rows
				}
			}
			for (int y = 0; y < heightV+4; y++) 
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][depthV+3][y][x] = data3D[0][depthV+2][y][x] = data3D[0][depthV+1][y][x];	// duplicate last 2 layers
					data3D[0][0][y][x] = data3D[0][1][y][x] = data3D[0][2][y][x];						// duplicate first 2 layers
				}
		}

		else if (bitDepth == 24) {
			for (int z=1; z<=depthV; z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthV);
				IJ.showProgress(0.6*z/depthV);
				int[] pixels = (int[]) stack.getPixels(z);

				int pos = 0;
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = pixels[pos++];
						int r = (val>>16)&0xFF;  
						int g = (val>> 8)&0xFF;  
						int b =  val&0xFF; 
						data3D[1][z+1][y][x] = (byte)r;  
						data3D[2][z+1][y][x] = (byte)g;  
						data3D[3][z+1][y][x] = (byte)b;  
						data3D[0][z+1][y][x] = (byte)((r+2*g+b)>>2);
					}
					data3D[0][z+1][y][0] = data3D[0][z+1][y][1] = data3D[0][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[1][z+1][y][0] = data3D[1][z+1][y][1] = data3D[1][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[2][z+1][y][0] = data3D[2][z+1][y][1] = data3D[2][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[3][z+1][y][0] = data3D[3][z+1][y][1] = data3D[3][z+1][y][2]; 						// duplicate first 2 pixels
					data3D[0][z+1][y][widthV+3] = data3D[0][z+1][y][widthV+2] = data3D[0][z+1][y][widthV+1]; 	// duplicate last 2 pixels
					data3D[1][z+1][y][widthV+3] = data3D[1][z+1][y][widthV+2] = data3D[1][z+1][y][widthV+1]; 	// duplicate last 2 pixels
					data3D[2][z+1][y][widthV+3] = data3D[2][z+1][y][widthV+2] = data3D[2][z+1][y][widthV+1]; 	// duplicate last 2 pixels
					data3D[3][z+1][y][widthV+3] = data3D[3][z+1][y][widthV+2] = data3D[3][z+1][y][widthV+1]; 	// duplicate last 2 pixels
				}
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][z+1][0][x] = data3D[0][z+1][1][x] = data3D[0][z+1][2][x]; 						// duplicate first 2 rows
					data3D[1][z+1][0][x] = data3D[1][z+1][1][x] = data3D[1][z+1][2][x]; 						// duplicate first 2 rows
					data3D[2][z+1][0][x] = data3D[2][z+1][1][x] = data3D[2][z+1][2][x]; 						// duplicate first 2 rows
					data3D[3][z+1][0][x] = data3D[3][z+1][1][x] = data3D[3][z+1][2][x]; 						// duplicate first 2 rows
					data3D[0][z+1][heightV+3][x] = data3D[0][z+1][heightV+2][x] = data3D[0][z+1][heightV+1][x];	// duplicate last 2 rows
					data3D[1][z+1][heightV+3][x] = data3D[1][z+1][heightV+2][x] = data3D[1][z+1][heightV+1][x];	// duplicate last 2 rows
					data3D[2][z+1][heightV+3][x] = data3D[2][z+1][heightV+2][x] = data3D[2][z+1][heightV+1][x];	// duplicate last 2 rows
					data3D[3][z+1][heightV+3][x] = data3D[3][z+1][heightV+2][x] = data3D[3][z+1][heightV+1][x];	// duplicate last 2 rows
				}
			}
			for (int y = 0; y < heightV+4; y++) 
				for (int x = 0; x < widthV+4; x++) {
					data3D[0][depthV+3][y][x] = data3D[0][depthV+2][y][x] = data3D[0][depthV+1][y][x];	// duplicate last 2 layers
					data3D[1][depthV+3][y][x] = data3D[1][depthV+2][y][x] = data3D[1][depthV+1][y][x];	// duplicate last 2 layers
					data3D[2][depthV+3][y][x] = data3D[2][depthV+2][y][x] = data3D[2][depthV+1][y][x];	// duplicate last 2 layers
					data3D[3][depthV+3][y][x] = data3D[3][depthV+2][y][x] = data3D[3][depthV+1][y][x];	// duplicate last 2 layers
					data3D[0][0][y][x] = data3D[0][1][y][x] = data3D[0][2][y][x];						// duplicate first 2 layers
					data3D[1][0][y][x] = data3D[1][1][y][x] = data3D[1][2][y][x];						// duplicate first 2 layers
					data3D[2][0][y][x] = data3D[2][1][y][x] = data3D[2][2][y][x];						// duplicate first 2 layers
					data3D[3][0][y][x] = data3D[3][1][y][x] = data3D[3][2][y][x];						// duplicate first 2 layers
				}
		}

		// calculate variance etc. and fill histograms
		int[] va = new int[7];
		int[] vb = new int[7];

		for(int z=2; z < depthV+2; z++) {
			IJ.showStatus("Analyzing stack, slice: " + (z-1) + "/" + depthV);
			IJ.showProgress(0.6+0.4*z/depthV);
			for (int y = 2; y < heightV+2; y++) {
				for (int x = 2; x < widthV+2; x++) {
					int val = 0xff & data3D[0][z][y][x];
					va[0] = 0xff & data3D[0][z-1][y  ][x  ];
					vb[0] = 0xff & data3D[0][z+1][y  ][x  ];
					va[1] = 0xff & data3D[0][z  ][y-1][x  ];
					vb[1] = 0xff & data3D[0][z  ][y+1][x  ];
					va[2] = 0xff & data3D[0][z  ][y  ][x-1];
					vb[2] = 0xff & data3D[0][z  ][y  ][x+1];
					va[3] = 0xff & data3D[0][z-1][y-1][x-1];
					vb[3] = 0xff & data3D[0][z+1][y+1][x+1];
					va[4] = 0xff & data3D[0][z-1][y+1][x-1];
					vb[4] = 0xff & data3D[0][z+1][y-1][x+1];
					va[5] = 0xff & data3D[0][z-1][y-1][x+1];
					vb[5] = 0xff & data3D[0][z+1][y+1][x-1];
					va[6] = 0xff & data3D[0][z-1][y+1][x+1];
					vb[6] = 0xff & data3D[0][z+1][y-1][x-1];

					int grad = 0, d, dMax = 0, iMax = 0;
					for (int i = 0; i < vb.length; i++) {
						grad += d = Math.abs(va[i] - vb[i]);
						if (d > dMax) {
							dMax = d;
							iMax = i;
						}
					}

					int low, high;
					if(va[iMax] < vb[iMax]) {
						low = va[iMax]; high = vb[iMax];
					}
					else {
						low = vb[iMax]; high = va[iMax];
					}							
					grad /= 7;
					if (grad > 127)
						grad = 127;

					grad3D[z][y][x] = (byte)grad;
					histValGrad[val][(int)grad]++;
					histVal[val]++;					// luminance
					int mean = (int) (Math.max(0, Math.min(255,(low+high)*0.5)));
					int diff = (int) (Math.max(0, Math.min(127,(high-low)*0.5)));
					mean3D[z][y][x] = (byte) mean;
					diff3D[z][y][x] = (byte) diff;
					histMeanDiff[mean][diff]++;
				}
			}
		}

		IJ.showProgress(1.0);
		IJ.showStatus("");
	}
	
	
	void calculateGradients() {
		control.alphaWasChanged = false;
		
		byte[][][] alpha_3D = new byte[depthV+4][heightV+4][widthV+4]; //z y x 
		byte[][][] alpha_3D_smooth = new byte[depthV+4][heightV+4][widthV+4]; //z y x

		long start = 0;
		if (control.LOG) { 
			IJ.log("Calculate Gradients ");
			start = System.currentTimeMillis();
		}

		if (control.alphaMode == Control.ALPHA1) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = data3D[0][z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a1_R[val]*255);
					}
				}
			}			
		}
		else if (control.alphaMode == Control.ALPHA2) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = data3D[0][z][y][x] & 0xFF;
						int grad = grad3D[z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a2_R[val][grad]*255);
					}
				}
			}
		}
		else if (control.alphaMode == Control.ALPHA3) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						int val = mean3D[z][y][x] & 0xFF;
						int diff = diff3D[z][y][x] & 0xFF;
						alpha_3D[z][y][x] = (byte) (vv.a3_R[val][diff]*255);
					}
				}
			}
		}
		else if (control.alphaMode == Control.ALPHA4) {
			for(int z = 2; z < depthV+2; z++) {
				for (int y = 2; y < heightV+2; y++) {
					for (int x = 2; x < widthV+2; x++) {
						alpha_3D[z][y][x] = aPaint_3D[z][y][x];
					}
				}
			}
		}

		// filter alpha
		for(int z=1; z < depthV+3; z++) {
			for (int y = 1; y < heightV+3; y++) {
				int a000, a010, a020;
				int a001 = 0xff & alpha_3D[z-1][y-1][0];
				int a011 = 0xff & alpha_3D[z-1][y  ][0];
				int a021 = 0xff & alpha_3D[z-1][y+1][0];
				int a002 = 0xff & alpha_3D[z-1][y-1][1];
				int a012 = 0xff & alpha_3D[z-1][y  ][1];
				int a022 = 0xff & alpha_3D[z-1][y+1][1];
				int a100, a110, a120;
				int a101 = 0xff & alpha_3D[z][y-1][0];
				int a111 = 0xff & alpha_3D[z][y  ][0];
				int a121 = 0xff & alpha_3D[z][y+1][0];
				int a102 = 0xff & alpha_3D[z][y-1][1];
				int a112 = 0xff & alpha_3D[z][y  ][1];
				int a122 = 0xff & alpha_3D[z][y+1][1];
				int a200, a210, a220;
				int a201 = 0xff & alpha_3D[z+1][y-1][0];
				int a211 = 0xff & alpha_3D[z+1][y  ][0];
				int a221 = 0xff & alpha_3D[z+1][y+1][0];
				int a202 = 0xff & alpha_3D[z+1][y-1][1];
				int a212 = 0xff & alpha_3D[z+1][y  ][1];
				int a222 = 0xff & alpha_3D[z+1][y+1][1];

				for (int x = 1; x < widthV+3; x++) {
					a000 = a001; a010 = a011; a020 = a021;
					a001 = a002; a011 = a012; a021 = a022;
					a002 = 0xff & alpha_3D[z-1][y-1][x+1];
					a012 = 0xff & alpha_3D[z-1][y  ][x+1];
					a022 = 0xff & alpha_3D[z-1][y+1][x+1];

					a100 = a101; a110 = a111; a120 = a121;
					a101 = a102; a111 = a112; a121 = a122;
					a102 = 0xff & alpha_3D[z][y-1][x+1];
					a112 = 0xff & alpha_3D[z][y  ][x+1];
					a122 = 0xff & alpha_3D[z][y+1][x+1];

					a200 = a201; a210 = a211; a220 = a221;
					a201 = a202; a211 = a212; a221 = a222;
					a202 = 0xff & alpha_3D[z+1][y-1][x+1];
					a212 = 0xff & alpha_3D[z+1][y  ][x+1];
					a222 = 0xff & alpha_3D[z+1][y+1][x+1];

					int a = (a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022 +
						 	 a100 + a101 + a102 + a110 + a111 + a112 + a120 + a121 + a122 +
							 a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) >> 5;

					alpha_3D_smooth[z][y][x] = (byte)a;
				}
			}
		}

		//alpha_3D_smooth = alpha_3D;
		
		// gradient
		for(int z=1; z < depthV+3; z++) {
			for (int y = 1; y < heightV+3; y++) {			
				int a000, a010, a020;
				int a001 = 0xff & alpha_3D_smooth[z-1][y-1][0];
				int a011 = 0xff & alpha_3D_smooth[z-1][y  ][0];
				int a021 = 0xff & alpha_3D_smooth[z-1][y+1][0];
				int a002 = 0xff & alpha_3D_smooth[z-1][y-1][1];
				int a012 = 0xff & alpha_3D_smooth[z-1][y  ][1];
				int a022 = 0xff & alpha_3D_smooth[z-1][y+1][1];
				int a100, a110, a120;
				int a101 = 0xff & alpha_3D_smooth[z][y-1][0];
				int a111 = 0xff & alpha_3D_smooth[z][y  ][0];
				int a121 = 0xff & alpha_3D_smooth[z][y+1][0];
				int a102 = 0xff & alpha_3D_smooth[z][y-1][1];
				int a112 = 0xff & alpha_3D_smooth[z][y  ][1];
				int a122 = 0xff & alpha_3D_smooth[z][y+1][1];
				int a200, a210, a220;
				int a201 = 0xff & alpha_3D_smooth[z+1][y-1][0];
				int a211 = 0xff & alpha_3D_smooth[z+1][y  ][0];
				int a221 = 0xff & alpha_3D_smooth[z+1][y+1][0];
				int a202 = 0xff & alpha_3D_smooth[z+1][y-1][1];
				int a212 = 0xff & alpha_3D_smooth[z+1][y  ][1];
				int a222 = 0xff & alpha_3D_smooth[z+1][y+1][1];

				for (int x = 1; x < widthV+3; x++) {
					a000 = a001; a010 = a011; a020 = a021;
					a001 = a002; a011 = a012; a021 = a022;
					a002 = 0xff & alpha_3D_smooth[z-1][y-1][x+1];
					a012 = 0xff & alpha_3D_smooth[z-1][y  ][x+1];
					a022 = 0xff & alpha_3D_smooth[z-1][y+1][x+1];

					a100 = a101; a110 = a111; a120 = a121;
					a101 = a102; a111 = a112; a121 = a122;
					a102 = 0xff & alpha_3D_smooth[z][y-1][x+1];
					a112 = 0xff & alpha_3D_smooth[z][y  ][x+1];
					a122 = 0xff & alpha_3D_smooth[z][y+1][x+1];

					a200 = a201; a210 = a211; a220 = a221;
					a201 = a202; a211 = a212; a221 = a222;
					a202 = 0xff & alpha_3D_smooth[z+1][y-1][x+1];
					a212 = 0xff & alpha_3D_smooth[z+1][y  ][x+1];
					a222 = 0xff & alpha_3D_smooth[z+1][y+1][x+1];

					int dx = ((a002 + a012 + a022 + a102 + a112 + a122 + a202 + a212 + a222) >> 2) - 
							 ((a000 + a010 + a020 + a100 + a110 + a120 + a200 + a210 + a220) >> 2);

					int dy = ((a020 + a021 + a022 + a120 + a121 + a122 + a220 + a221 + a222) >> 2) -
							 ((a000 + a001 + a002 + a100 + a101 + a102 + a200 + a201 + a202) >> 2);
					
					int dz = ((a200 + a201 + a202 + a210 + a211 + a212 + a220 + a221 + a222) >> 2) -
							 ((a000 + a001 + a002 + a010 + a011 + a012 + a020 + a021 + a022) >> 2);

//					int dx = (a102 + 2*a112 + a122 - a100 - 2*a110 - a120) / 4;
//					int dy = (a021 + 2*a121 + a221 - a001 - 2*a101 - a201) / 4;
//					int dz = (a210 + 2*a211 + a212 - a010 - 2*a011 - a012) / 4;
					
					nx_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dx))+128);
					ny_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dy))+128);
					nz_3D[z][y][x] = (byte)(Math.max(-127, Math.min(127,dz))+128);
				}
			}
		}
		
		alpha_3D = null;
		alpha_3D_smooth = null;

		if (control.LOG) {
			long end = System.currentTimeMillis();
			System.out.println("  Execution time "+(end-start)+" ms.");
		}
	}
	
	private static final byte YES = 1;
	
	void findAndSetSimilarInVolume(int lum, int alpha, int z0, int y0, int x0) {

		int width  = vv.vol.widthV+4;
		int height = vv.vol.heightV+4;
		int depth  = vv.vol.depthV+4;
		byte[][][] data = vv.vol.data3D[0];

		control.alphaWasChanged = true;

		int regionSize = 1;
		int pointsInQueue = 0;
		int queueArrayLength = 40000;
		int[] queue = new int[queueArrayLength];

		byte[] hasBeenProcessed = new byte[depth * width * height];
		setAlphaAndColorInVolume(alpha, z0, y0, x0); 
		int i = width * (z0 * height + y0) + x0;
		hasBeenProcessed[i] = YES;
		queue[pointsInQueue++] = i;

		while (pointsInQueue > 0) {

			int nextIndex = queue[--pointsInQueue];
			int pz = nextIndex / (width * height);
			int currentSliceIndex = nextIndex % (width * height);
			int py = currentSliceIndex / width;
			int px = currentSliceIndex % width;

			//int actualValue = data[pz][py][px] & 0xff;

			for (int k = 0; k < 6; k++) {

				int x = px;
				int y = py;
				int z = pz;

				if      (k == 0)  x = Math.max(0, x-1); 
				else if (k == 1)  x = Math.min(width-1, x+1); 
				else if (k == 2)  y = Math.max(0, y-1); 
				else if (k == 3)  y = Math.min(height-1, y+1);
				else if (k == 4)  z = Math.max(0, z-1); 
				else if (k == 5)  z = Math.min(depth-1, z+1);

				col_3D[z][y][x] = (byte)control.indexPaint; // set the color of neigbors

				int newPointStateIndex = width * (z * height + y) + x;
				if (hasBeenProcessed[newPointStateIndex] == YES) 
					continue;

				int neighbourValue = data[z][y][x] & 0xff; 
				int diff = Math.abs(neighbourValue-lum);
				//int diff = Math.abs(neighbourValue-actualValue);

				if (diff > control.lumTolerance)
					continue;

				int grad = vv.vol.grad3D[z][y][x] & 0xff; 
				if (grad > control.gradTolerance)
					continue;

				regionSize++;
				setAlphaAndColorInVolume(alpha, z, y, x); 

				hasBeenProcessed[newPointStateIndex] = YES;
				if (pointsInQueue == queueArrayLength) {
					int newArrayLength = (int) (queueArrayLength * 2);
					int[] newArray = new int[newArrayLength];
					System.arraycopy(queue, 0, newArray, 0, pointsInQueue);
					queue = newArray;
					queueArrayLength = newArrayLength;
				}
				queue[pointsInQueue++] = newPointStateIndex;
			}
		}

		if (regionSize < 100)
			IJ.error("Found only " + regionSize + " connected voxel(s).\n Try changing the tolerance values.");
	}
		

	private void setAlphaAndColorInVolume(int alpha, int z, int y, int x) {
		vv.vol.aPaint_3D[z][y][x] = (byte)alpha;
		if (alpha > 0) {
			int a = alpha - vv.tf_a4.getAlphaOffset();
			a = Math.min(254, Math.max(-254, a));
			aPaint_3D2[z][y][x] = (byte) (a/2);
		}
		else
			aPaint_3D2[z][y][x] = 0;

		col_3D[z][y][x] = (byte)control.indexPaint;
	}
	
}
