package jRenderer3D;

/* 
 * Version 1.01			18. 05. 2006 
 * 						Fixed xMax Error for volumeProjection_trilinear_front
 * 			
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

class Volume {
	
	ImagePlus imp;
	ImageProcessor ip; 
	
	private int widthX;   				
	private int heightY;  	 	   
	private int depthZ;
	
	private int widthXm1;
	private int heightYm1;
	private int depthZm1;	
	
	int [][] volumeLimits = new int[8][3];
	int [][] cornerT = new int[8][3];
		
	double a = 0, b = 1;
	
	private double min, max;
	
	double oldMin, oldMax;
	
	boolean firstTime = true;
	
	private double zAspect = 1;
	private byte[][][] data3D;
	private int cutDist = - 1000;
	
	private Lut lut;
	private int[] bufferPixels;
	private double[] zbufferPixels;
	private int bufferWidth;
	private int bufferHeight;
	private Transform tr;
	private int drawMode;
	private int threshold;
	//private int lutNr;
	private int dotsDeltaX = 1;  // subsampling factor in x direction (used by dots drawing)
	private int dotsDeltaY = 1;  // subsampling factor in y direction (used by dots drawing)
	private int dotsDeltaZ = 1;  // subsampling factor in z direction (used by dots drawing)
	private int renderDepth = 6;
	


	protected Volume (ImagePlus imp) {
		this.imp = imp;
		this.ip = imp.getProcessor();
		
		widthX = imp.getWidth();
		heightY = imp.getHeight();
		depthZ = imp.getStackSize();
		
		volumeLimits[0][0] = 0;  	  volumeLimits[0][1] =      0;   volumeLimits[0][2] = 0; 		// left  front down [0] 
		volumeLimits[1][0] = 0;       volumeLimits[1][1] = heightY;  volumeLimits[1][2] = depthZ;   // left  back  top  [1]
		volumeLimits[2][0] = widthX;  volumeLimits[2][1] =      0;   volumeLimits[2][2] = depthZ;   // right front top  [2]
		volumeLimits[3][0] = widthX;  volumeLimits[3][1] = heightY;  volumeLimits[3][2] = 0; 		// right back  down [3]
		volumeLimits[4][0] = 0; 	  volumeLimits[4][1] =      0;   volumeLimits[4][2] = depthZ;   // left  front top  [4]
		volumeLimits[5][0] = 0;    	  volumeLimits[5][1] = heightY;  volumeLimits[5][2] = 0; 		// left  back  down [5]
		volumeLimits[6][0] = widthX;  volumeLimits[6][1] =      0;   volumeLimits[6][2] = 0; 		// right front down [6] 
		volumeLimits[7][0] = widthX;  volumeLimits[7][1] = heightY;  volumeLimits[7][2] = depthZ;   // right back  top  [7]
		
		
		widthXm1 = widthX-1;
		heightYm1 = heightY-1;
		depthZm1 = depthZ-1;	
	
		data3D = new byte[depthZ][heightY][widthX]; //z y x
		
		getMinMax();
		
		init();
	}
	
	protected void setBuffers(int[] bufferPixels, double[] zbufferPixels, int bufferWidth, int bufferHeight) {
		this.bufferPixels =  bufferPixels;
		this.zbufferPixels = zbufferPixels;
		this.bufferWidth = bufferWidth;
		this.bufferHeight = bufferHeight;
	}
	
	protected void setTransform(Transform transform) {
		this.tr = transform;	
	}
	
	private boolean getMinMax() {
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
		}
		
		if (firstTime) {
			// 1.06
			if (zAspect == 1) 
				zAspect = (float) (cal.pixelDepth / cal.pixelWidth);
			if (zAspect == 0)
				zAspect = 1;
			
			lut = new Lut();
			lut.readLut(imp);
			
			oldMin = min;
			oldMax = max;
			firstTime = false;
		}
		
		boolean changed;
		if (oldMin == min && oldMax == max) 
			changed = false;
		else
			changed = true;
		oldMin = min;
		oldMax = max;
		
		return changed;
	}
	
	
	private void init() {
		
		ImageStack stack = imp.getStack();
		
		int bitDepth = imp.getBitDepth();
		if (bitDepth == 8) {
			float scale = (float) (255f/(max-min));
			
			for (int z=0;z<depthZ;z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
				IJ.showProgress((double)3*z/(4*depthZ));
				
				byte[] pixels = (byte[]) stack.getPixels(z+1);
				
				int pos = 0;
				for (int y = 0; y < heightY; y++) {
					for (int x = 0; x <widthX; x++) {
						int val = 0xff & pixels[pos++];
						val = (int)((val-min)*scale);
						
						if (val<0f) val = 0;
						if (val>255) val = 255;
						
						data3D[z][y][x] = (byte)(0xff & val); 
					}
				}
			}
		}
		
		if (bitDepth == 16) {
			float scale = (float) (255f/(max-min));
			
			for (int z=0;z<depthZ;z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
				IJ.showProgress((double)3*z/(4*depthZ));
				short[] pixels = (short[]) stack.getPixels(z+1);
				int pos = 0;
				for (int y = 0; y < heightY; y++) {
					
					for (int x = 0; x <widthX; x++) {
						
						int val = (int) ((int)(0xFFFF & pixels[pos++])*b + a - min);
						if (val<0f) val = 0;
						val = (int)(val*scale);
						
						if (val>255) val = 255;
						data3D[z][y][x] = (byte)(val);  
					}
				}
			}							
		}
		
		if (bitDepth == 32) {
			
			float scale = (float) (255f/(max-min));
			
			for (int z=0; z<depthZ; z++) {
				IJ.showStatus("Reading stack, slice: " + z + "/" + depthZ);
				IJ.showProgress((double)3*z/(4*depthZ));
				float[] pixels = (float[]) stack.getPixels(z+1);
				
				int pos = 0;
				for (int y = 0; y < heightY; y++) {
					for (int x = 0; x <widthX; x++) {
						float value = (float) (pixels[pos++] - min);
						if (value<0f) value = 0f;
						int ivalue = (int)(value*scale);
						
						if (ivalue>255) ivalue = 255;
						data3D[z][y][x] = (byte)(ivalue);  
					}
				}
			}
		}
		
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}
	
	
/************************************************************************/
	
	private void volumeDotsOrig(){
		
		int[] v = new int[3];
		
		byte[][] datay;
		byte[]   datax;
		
		double step = dotsDeltaX/(widthX-1.);
		
		
		for (int z=0; z < data3D.length ; z+= dotsDeltaZ){
			v[2] = z;
			datay = data3D[z];
			for (int y=0; y < datay.length ; y+= dotsDeltaY){
				v[1] = y;
				datax = datay[y]; 
				
				v[0] = 0 ;
				tr.xyzPos(v);
				double x1 = tr.X;
				double y1 = tr.Y;
				double z1 = tr.Z;
				
				v[0] = datax.length-1 ;
				tr.xyzPos(v);
				
				double dx = (tr.X-x1)*step;
				double dy = (tr.Y-y1)*step;
				double dz = (tr.Z-z1)*step;
				
				for (int x = 0; x < widthX ; x+= dotsDeltaX){
					int val = 0xff & datax[x];
					
					if (val >= threshold) {
						
						double z_ = z1;
						
						if (z_ > cutDist) {
							int x_ = (int)(x1);
							int y_ = (int)(y1);
							
							if (x_ >= 0 && y_ >= 0 && x_ < bufferWidth && y_ < bufferHeight) { 
								int pos = y_*bufferWidth + x_;  
								
								if (z_ < zbufferPixels[pos]) {
									zbufferPixels[pos] = z_;
									bufferPixels[pos] = val+1;  
								}
							}
						}
					}
					z1 += dz;
					y1 += dy;
					x1 += dx;
				}
			}
		}
		for (int i = bufferPixels.length-1; i >= 0; i--)  {
			int val = bufferPixels[i];
			if (val > 255)
				val = 255;
			if (val > 0)
				bufferPixels[i] = lut.colors[val];
		}
	}
	
	private void volumeDots(){
		
		int[] v = new int[3];
		
		byte[][] datay;
		byte[]   datax;
		
		for (int z=0; z < data3D.length ; z+= dotsDeltaZ){
			v[2] = z;
			datay = data3D[z];
			for (int y=0; y < datay.length ; y+= dotsDeltaY){
				v[1] = y;
				datax = datay[y]; 
				
				for (int x = 0; x < widthX ; x+= dotsDeltaX){
					v[0] = x;
					
					tr.xyzPos(v);
					double x1 = tr.X;
					double y1 = tr.Y;
					double z1 = tr.Z;
					
					int val = 0xff & datax[x];
					
					if (val >= threshold) {
						
						if (z1 > cutDist) {
							int x_ = (int)(x1);
							int y_ = (int)(y1);
							
							if (x_ >= 0 && y_ >= 0 && x_ < bufferWidth && y_ < bufferHeight) { 
								int pos = y_*bufferWidth + x_;  
								
								if (z1 < zbufferPixels[pos]) {
									zbufferPixels[pos] = z1;
									bufferPixels[pos] = lut.colors[val];  
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void volumeSliceNearestNeighbor(){
		
		int xMin = bufferWidth-1, xMax = 0, yMin = bufferHeight-1, yMax = 0;
		
		for (int i = 0;  i < 8; i++) {
			
			tr.xyzPos(volumeLimits[i]);
			
			int xi = (int) tr.X;
			if (xi < xMin) xMin = xi;
			if (xi > xMax) xMax = xi;
			int yi = (int) tr.Y;
			if (yi < yMin) yMin = yi;
			if (yi > yMax) yMax = yi;
		}
		
		xMin = (xMin < 0)   ?   0 : xMin;
		xMax = (xMax >= bufferWidth) ? bufferWidth-1 : xMax;
		yMin = (yMin < 0)   ?   0 : yMin;
		yMax = (yMax >= bufferHeight) ? bufferHeight-1 : yMax;
		
		int[] v = new int[3];
		
		v[2] = cutDist; // z
		for (int y = yMin; y < yMax; y++) {
			v[1] = y;
			int posOffset = y*bufferWidth;
			
			for (int x = xMin; x < xMax; x++) {
				v[0] = x;
				tr.invxyzPosf(v);
				
				double x_  = tr.x; 
				double y_  = tr.y; 
				double z_  = tr.z; 
				
				if (x_ >= 0 && x_ < widthX && y_ >= 0 && y_ < heightY && z_ >= 0 && z_ < depthZ ) {
					
					int val = 0xff & data3D[(int)z_][(int)y_][(int)x_];						
					int pos = posOffset + x;
					
//					if ((int)x_ == 0 && (int) y_ == 0) {
//						System.out.println("x: "+ x_ + " y: "+ y_ +" z: "+ z_);
//						bufferPixels[pos] = 0xFFFF0000;
//					}
//					else 
					int v_ = (cutDist * 20) + 128;
					v_ = Math.min(Math.max(0, v_), 255);
					v_ = 0xFF000000 |(v_ << 8);
					bufferPixels[pos] = lut.colors[val];	
					zbufferPixels[pos] = cutDist;	
				}	
			}
		}
	}
	


	private void volumeSliceTrilinear(){
		int xMin = bufferWidth-1, xMax = 0, yMin = bufferHeight-1, yMax = 0;
		
		for (int i = 0;  i < 8; i++) {
			
			tr.xyzPos(volumeLimits[i]);
			
			int xi = (int) tr.X;
			if (xi < xMin) xMin = xi;
			if (xi > xMax) xMax = xi;
			int yi = (int) tr.Y;
			if (yi < yMin) yMin = yi;
			if (yi > yMax) yMax = yi;
		}
		
		xMin = (xMin < 0)   ?   0 : xMin;
		xMax = (xMax >= bufferWidth) ? bufferWidth-1 : xMax;
		yMin = (yMin < 0)   ?   0 : yMin;
		yMax = (yMax >= bufferHeight) ? bufferHeight-1 : yMax;
		
		int[] v = new int[3];
		
		// draw slice at dist
		v[2] = cutDist; // z
		
		for (int y = yMin; y <= yMax; y++) {
			v[1] = y;
			int posOffset = y*bufferWidth;
			for (int x = xMin; x <= xMax; x++) {
				v[0] = x;
				tr.invxyzPosf(v);
		
				double x_  = tr.x; 
				double y_  = tr.y; 
				double z_  = tr.z; 
				
				if ((x_ >=0  &&  x_ < widthX ) && (y_ >=0  &&  y_<heightY) && (z_ >=0  &&  z_ < depthZ  ) ) { 
					int pos = posOffset + x;
					
					int val = trilinear(z_, y_, x_);

					bufferPixels[pos] = lut.colors[val];	
					zbufferPixels[pos] = cutDist;	
				}
			}
		}			
	}
	
	private boolean isInside(int x, int y) {
		
		int[] p = new int[3];
		p[0] = x;
		p[1] = y;
		
		if (Misc.inside(p, cornerT[0], cornerT[1], cornerT[4])) return true;
		if (Misc.inside(p, cornerT[0], cornerT[1], cornerT[5])) return true;
		if (Misc.inside(p, cornerT[2], cornerT[3], cornerT[6])) return true;
		if (Misc.inside(p, cornerT[2], cornerT[3], cornerT[7])) return true;
		
		if (Misc.inside(p, cornerT[1], cornerT[3], cornerT[5])) return true;
		if (Misc.inside(p, cornerT[1], cornerT[3], cornerT[7])) return true;
		if (Misc.inside(p, cornerT[0], cornerT[2], cornerT[4])) return true;
		if (Misc.inside(p, cornerT[0], cornerT[2], cornerT[6])) return true;
		
		if (Misc.inside(p, cornerT[1], cornerT[2], cornerT[4])) return true;
		if (Misc.inside(p, cornerT[1], cornerT[2], cornerT[7])) return true;
		if (Misc.inside(p, cornerT[0], cornerT[3], cornerT[5])) return true;
		if (Misc.inside(p, cornerT[0], cornerT[3], cornerT[6])) return true;
		
		return false;
	}
	
	public synchronized void volumeProjection_trilinear_front(){
		double width  = widthX  - 0.5f;
		double height = heightY - 0.5f;
		double depth  = depthZ  - 0.5f;
		
		transformVolumeLimits();
		
		
		int xMin = bufferWidth-1, xMax = 0;
		int yMin = bufferHeight-1, yMax = 0;
		int zMin = cutDist, zMax = cutDist;
		
		for (int i = 0;  i < 8; i++) {
			
			tr.xyzPos(volumeLimits[i]);
			
			int xi = (int) tr.X;
			if (xi < xMin) xMin = xi;
			if (xi > xMax) xMax = xi;
			int yi = (int) tr.Y;
			if (yi < yMin) yMin = yi;
			if (yi > yMax) yMax = yi;
			int zi = (int) tr.Z;
			if (zi < zMin) zMin = zi;
			if (zi > zMax) zMax = zi;
		}
		
		xMin = (xMin < 0)   ?   0 : xMin;
		xMax = (xMax >= bufferWidth) ? bufferWidth-1 : xMax;
		yMin = (yMin < 0)   ?   0 : yMin;
		yMax = (yMax >= bufferHeight) ? bufferHeight-1 : yMax;
		
		int[] v1 = new int[3];
		int[] v2 = new int[3];
		
		if (cutDist > zMin)
			zMin = cutDist;
		
		int nd = zMax - zMin;
		
		if (nd > 0) {
			
			float scaleLum = (renderDepth > 1) ? (renderDepth*255/((renderDepth-1)*255f)) : 2;;
			v1[2] = zMin; 
			v2[2] = zMax; 
			
			double nd1 = 1./nd;
			double d_z = (zMax - zMin)*nd1;
			
//			while (d_z > 0.3) {
//				d_z /= 1.1;
//				nd *= 1.1;
//				nd1 /= 1.1;
//			}
			
			for (int y = yMin; y <= yMax; y++) {
				v1[1] = v2[1] = y;
				int percent = 100*(y-yMin)/ (yMax-yMin);
				IJ.showStatus("Rendering : " + percent +"%" );
				
				for (int x = xMin; x <= xMax; x++) {
					
					if (isInside(x, y)) {
						double z = zMin; 
						
						int pos = y*bufferWidth + x;
						v1[0] = v2[0] = x;
						
						int alpha = 255;
						int V = 0;
						int alpha2 = 255;
						int V2 = 0;
						tr.invxyzPosf(v1);
						double xd1 = tr.x;
						double yd1 = tr.y;
						double zd1 = tr.z;
						
						tr.invxyzPosf(v2);
						double xd2 = tr.x;
						double yd2 = tr.y;
						double zd2 = tr.z;
						
						double dx = (xd2-xd1)*nd1;
						double dy = (yd2-yd1)*nd1;
						double dz = (zd2-zd1)*nd1;
						
						int k = 0;
						
						for (int n = nd; n >= 0; n-- ) {
							z += d_z;
							
							if (xd1 >= 0 && xd1 < width &&  
									yd1 >= 0 && yd1 < height && 
									zd1 >= 0 && zd1 < depth) { 
								int val = trilinear(zd1, yd1, xd1);	
								
//								if (val >= threshold) {	  
//									int f = (val - threshold) + 50;
//									if (f > 255)
//										f = 255;
//									
//									alpha +=f;
//									V += f*val;
//									
//									if (++k >= renderDepth)
//										break;
//								}
								
	
								
								int f = 255 - 10*Math.abs(val - threshold);
								if (f < 0)
									f = 0;
								if (f > 10) {
									alpha +=f;
									V += f*255;
								
								if (++k >= renderDepth)
									break;
								}
								
//								int f = 255 - 10*Math.abs(val - threshold);
//								if (f < 0)
//									f = 0;
//								if (f > 10) {
//									alpha +=f;
//									V += f*255;
//								
//								if (++k >= renderDepth)
//									break;
//								}
	

//								int f = 255 - 10*Math.abs(val - threshold);
//								if (f < 0)
//									f = 0;
//
//								if (f > 10) {
//
//									alpha2 +=f;
//									V2 += f*255;
//
//									if (++k >= renderDepth)
//										break;
//								}
								
							}
							
							xd1 += dx;
							yd1 += dy;
							zd1 += dz;
						}
						int val = (int) (scaleLum * V / alpha);
						if (val > 255)
							val = 255;
						
						int val2 = (int) (scaleLum * V2 / alpha2);
						if (val2 > 255)
							val2 = 255;
						
						
//						int v_ = (int) ((z * 10) + 128);
//						v_ = Math.min(Math.max(0, v_), 255);
//						v_ = 0xFF000000 | (v_ << 8);
						
						//bufferPixels[pos] = 0xFF000000 | (val << 16) | val2;
						bufferPixels[pos] = lut.colors[val];
					    zbufferPixels[pos] = z; 
					}	
				}	
			}		
		}
		IJ.showStatus("");			
	}
	
	private void volumeProjection_trilinear_back(){
		
		double width  = widthX  - 0.5f;
		double height = heightY - 0.5f;
		double depth  = depthZ  - 0.5f;
		
		transformVolumeLimits();
		
		
		int xMin = bufferWidth-1, xMax = 0;
		int yMin = bufferHeight-1, yMax = 0;
		int zMin = 10000, zMax = -10000;
		
		for (int i = 0;  i < 8; i++) {
			
			tr.xyzPos(volumeLimits[i]);
			
			int xi = (int) tr.X;
			if (xi < xMin) xMin = xi;
			if (xi > xMax) xMax = xi;
			int yi = (int) tr.Y;
			if (yi < yMin) yMin = yi;
			if (yi > yMax) yMax = yi;
			int zi = (int) tr.Z;
			if (zi < zMin) zMin = zi;
			if (zi > zMax) zMax = zi;
		}
		
		xMin = (xMin < 0)   ?   0 : xMin;
		xMax = (xMax >= bufferWidth) ? bufferWidth-1 : xMax;
		yMin = (yMin < 0)   ?   0 : yMin;
		yMax = (yMax >= bufferHeight) ? bufferHeight-1 : yMax;
		
		xMin = (xMin < 0)   ?   0 : xMin;
		xMax = (xMax > 511) ? 511 : xMax;
		yMin = (yMin < 0)   ?   0 : yMin;
		yMax = (yMax > 511) ? 511 : yMax;
		
		int[] v1 = new int[3];
		int[] v2 = new int[3];
		
		if (cutDist < zMin)
			cutDist = zMin;
		
		int nd = zMax - cutDist;
		
		double dx=0;
		double dy=0;
		double dz=0;
		
		//int threshMax2 = 2*renderDepth;
		
		int vals[] = new int [zMax-cutDist]; 
		
		if (nd > 0) {
			v1[2] = cutDist; 
			v2[2] = zMax; 
			
			double nd1 = 1f/nd;
			for (int y = yMin; y <= yMax; y++) {
				v1[1] = v2[1] = y;
				
				int percent = 100*(y-yMin)/ (yMax-yMin);
				//IJ.showProgress(percent, 100);
				
				IJ.showStatus("Rendering : " + percent +"%" );
				
				for (int x = xMin; x <= xMax; x++) {
					if (isInside(x, y)) 
					{
						
						int pos = y*bufferWidth + x;
						
						v1[0] = v2[0] = x;
						
						//int alpha = 255;
						int V = 0;
						tr.invxyzPosf(v1);
						double x1 = tr.x;
						double y1 = tr.y;
						double z1 = tr.z;
						
						tr.invxyzPosf(v2);
						
						dx = (tr.x-x1)*nd1;
						dy = (tr.y-y1)*nd1;
						dz = (tr.z-z1)*nd1;
						
						int k = 0;
						//int at = 0;
						for (int n = nd; n >= 0; n-- ) {
							if (x1 >= 0 && x1 < width &&  
									y1 >= 0 && y1 < height && 
									z1 >= 0 && z1 < depth) { 
								int val = trilinear(z1, y1, x1);	
								
								if (val >= threshold) {
									vals[k++] = val;
									
								if (k > renderDepth)
									break;
								}
							}
							
							x1 += dx;
							y1 += dy;
							z1 += dz;
						}
						for (int i = k-1; i>= 0; i--) {
							int a = (vals[i] - threshold) + 50;
							
							if (a > 255)
								a = 255;
							
							V = (a*vals[i] + V*(255-a))/255;
						}
						
						//int val = V; 
						// TODO changed
						int val = V+1;
						if (val > 255) val = 255;
						bufferPixels[pos] = lut.colors[val];
					    zbufferPixels[pos] = (int) tr.z;
						
					}			
				}	
			}		
		}
		IJ.showStatus("");			
	}


	
	private void transformVolumeLimits() {
		for (int i=0; i<8; i++) {
			tr.xyzPos(volumeLimits[i]);
			cornerT[i][0] = (int) tr.X;
			cornerT[i][1] = (int) tr.Y;
			cornerT[i][2] = (int) tr.Z;
		}
		
	}

	private int trilinear(double z, double y, double x) {
		
		int tx = (int)x;
		double dx = x - tx;
		int tx1 = (tx < widthXm1) ? tx+1 : tx;
		int ty = (int)y;
		double dy = y - ty;
		int ty1 = (ty < heightYm1) ? ty+1 : ty;
		int tz = (int)z;
		double dz = z - tz;
		int tz1 = (tz < depthZm1) ? tz+1 : tz;
		
		int  v000 = (int) (0xff & data3D[tz ][ty ][tx ]);
		int  v001 = (int) (0xff & data3D[tz1][ty ][tx ]); 
		int  v010 = (int) (0xff & data3D[tz ][ty1][tx ]); 
		int  v011 = (int) (0xff & data3D[tz1][ty1][tx ]); 
		int  v100 = (int) (0xff & data3D[tz ][ty ][tx1]); 
		int  v101 = (int) (0xff & data3D[tz1][ty ][tx1]); 
		int  v110 = (int) (0xff & data3D[tz ][ty1][tx1]); 
		int  v111 = (int) (0xff & data3D[tz1][ty1][tx1]); 
		
		return (int) (
				(v100 - v000)*dx + 
				(v010 - v000)*dy + 
				(v001 - v000)*dz +
				(v110 - v100 - v010 + v000)*dx*dy +
				(v011 - v010 - v001 + v000)*dy*dz +
				(v101 - v100 - v001 + v000)*dx*dz +
				(v111 + v100 + v010 + v001 - v110 - v101 - v011 - v000)*dx*dy*dz + v000 );
	}

	
	protected int getWidth(){
		return widthX;
	}
	protected int getHeight(){
		return heightY;
	}

	protected double getZaspectRatio() {
		return zAspect;
	}


	protected void setVolumeDrawMode(int volume_drawMode) {
		this.drawMode = volume_drawMode;
		
	}

	protected void draw() {
		
		if (drawMode == JRenderer3D.VOLUME_DOTS)
			volumeDots();
		else if (drawMode == JRenderer3D.VOLUME_SLICE_NEAREST_NEIGHBOR)
			volumeSliceNearestNeighbor();
		else if (drawMode == JRenderer3D.VOLUME_SLICE_TRILINEAR)
			volumeSliceTrilinear();
		else if (drawMode == JRenderer3D.VOLUME_DOTS )
			volumeDots();
		else if (drawMode == JRenderer3D.VOLUME_PROJECTION_TRILINEAR_FRONT)
			volumeProjection_trilinear_front();		
	}

	protected void setVolumeThreshold(int volume_threshold) {
		this.threshold = volume_threshold;
	}

	protected void setVolumeCutDist(int volume_cutDist) {
		this.cutDist = volume_cutDist;
	}

	protected void setVolumeLut(int volume_lutNr) {
		if (lut != null)
			lut.setLut(volume_lutNr);
	}

	protected void setVolumeDotsSubsampling(int volume_dotsDeltaX, int volume_dotsDeltaY, int volume_dotsDeltaZ) {
		this.dotsDeltaX = volume_dotsDeltaX;
		this.dotsDeltaY = volume_dotsDeltaY;
		this.dotsDeltaZ = volume_dotsDeltaZ;
		
	}
}
