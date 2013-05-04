/*
 * Volume Viewer 2.01
 * 01.12.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingWorker;
import javax.swing.UIManager;

public  class Pic {

	BufferedImage image;
	private int[] pixels = null;		
	private int width, height;
	private Control control;
	private Volume_Viewer vv;
	byte[][][] volData3D = null; 

	private Interpolation interpolation;

	public Pic (Control control, Volume_Viewer vv, int width, int height){
		this.control = control; 
		this.vv = vv; 
		this.width = width;
		this.height = height;
		
		this.subMax = Math.max(width, height) / 100;
		if (subMax % 2 != 1)
			subMax++;
		if (control.sampling > 1 || control.interpolationMode > 1)
			subMax += 2;
		if (control.LOG) System.out.println("subMax = " + subMax);

		pixels  = new int[width*height];

		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);

		interpolation = new Interpolation(control);
	}		

	float xd;
	float yd;
	float zd;
	int xs = 10, ys = 14;
	int xo = xs;
	int yo_xy = 0;
	int yo_yz;
	int yo_xz;
	
	private int lightRed; 
	private int lightGreen; 
	private int lightBlue; 

	public void setPixelsToZero() {
		Arrays.fill(pixels, 0);
	}

	Dimension getSliceViewSize(int width, int height){

		// unscaled size for the slices without gaps
		int my = (int) (vv.vol.heightV + vv.vol.depthV*Math.abs(control.zAspect)*2)+1;
		int mx = Math.max(vv.vol.widthV, vv.vol.heightV);

		// remaining width and height taking away the gaps
		int nWidth  = width  - 2*xs;
		int nHeight = height - 3*ys;

		float sy = nHeight/(float)my;
		float sx = nWidth/(float)mx;

		float s = Math.max(sy, sx);
		while (s*my > nHeight || s*mx > nWidth) {
			s *= 0.99;
		}
		return new Dimension((int)(s*mx+2*xs), (int)(3*ys+s*my));	
	}


	// show the three cuts
	void drawSlices() {
		
		// unscaled size for the slices without gaps
		int my = (int) (vv.vol.heightV + vv.vol.depthV*Math.abs(control.zAspect)*2)+1;
		int mx = Math.max(vv.vol.widthV, vv.vol.heightV);

		// remaining width and height taking away the gaps
		int nWidth  = width  - 2*xs;
		int nHeight = height - 3*ys;

		float sy = nHeight/(float)my;
		float sx = nWidth/(float)mx;

		float s = Math.max(sy, sx);
		while (s*my > nHeight || s*mx > nWidth) {
			s *= 0.99;
		}

		int Wx = (int) (vv.vol.widthV*s);
		int Wy = (int) (vv.vol.heightV*s);
		int Wz = (int) (vv.vol.depthV*Math.abs(control.zAspect)*s);

		xd = vv.vol.widthV/(float)Wx;
		yd = vv.vol.heightV/(float)Wy;
		zd = vv.vol.depthV/(float)Wz;

		Color color = UIManager.getColor ( "Panel.background" );
		int c = color.getRGB();
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = c;

		if (control.isRGB) {
			// xy
			int z_ = (int)(control.positionFactorZ*(vv.vol.depthV-1));
			for (int y=0; y < Wy; y++) {
				int y_ = (int) (y*yd);
				for (int x=0; x < Wx; x++) {
					int pos = (yo_xy+y)*width + x + xo;
					int x_ = (int) (x*xd);

					int valR = 0xFF & vv.vol.data3D[1][z_+2][y_+2][x_+2];
					int valG = 0xFF & vv.vol.data3D[2][z_+2][y_+2][x_+2];
					int valB = 0xFF & vv.vol.data3D[3][z_+2][y_+2][x_+2];
					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}	
			}

			// yz
			yo_yz = (int) (ys+Wy);
			int x_ = (int)(control.positionFactorX*(vv.vol.widthV-1)); 
			for (int y=0; y < Wz; y++) {
				z_ = (int)(y*zd);
				for (int x=0; x < Wy; x++) {
					int pos = (yo_yz+y)*width + x + xo;
					int y_ = (int)(x*yd);

					int valR = 0xFF & vv.vol.data3D[1][vv.vol.depthV+1-z_][y_+2][x_+2];
					int valG = 0xFF & vv.vol.data3D[2][vv.vol.depthV+1-z_][y_+2][x_+2];
					int valB = 0xFF & vv.vol.data3D[3][vv.vol.depthV+1-z_][y_+2][x_+2];												
					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}	
			}

			// xz
			yo_xz = (int) (2*ys+Wy+Wz);
			int y_ = (int)(control.positionFactorY*(vv.vol.heightV-1));
			for (int y=0; y < Wz; y++) {
				z_ = (int) (y*zd);
				for (int x=0; x < Wx; x++) {
					int pos = (yo_xz+y)*width + x + xo;
					x_ = (int) (x*xd); 

					int valR = 0xFF & vv.vol.data3D[1][vv.vol.depthV+1-z_][y_+2][x_+2];
					int valG = 0xFF & vv.vol.data3D[2][vv.vol.depthV+1-z_][y_+2][x_+2];
					int valB = 0xFF & vv.vol.data3D[3][vv.vol.depthV+1-z_][y_+2][x_+2];
					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}
			}
		}
		else {
			// xy
			int z_ = (int)(control.positionFactorZ*(vv.vol.depthV-1));
			for (int y=0; y < Wy; y++){
				int y_ = (int) (y*yd);
				for (int x=0; x < Wx; x++) {
					int pos = (yo_xy+y)*width + x + xo;
					int x_ = (int)(x*xd);

					int val = 0xFF & vv.vol.data3D[0][z_+2][y_+2][x_+2];
					pixels[pos] = 0xFF000000 | (val<<16) | (val<<8) | (val); 
					
//					// Gradient
//					int valR = 0xFF & vv.vol.nx_3D[z_+2][y_+2][x_+2];
//					int valG = 0xFF & vv.vol.ny_3D[z_+2][y_+2][x_+2];
//					int valB = 0xFF & vv.vol.nx_3D[z_+2][y_+2][x_+2];
//					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}	
			}

			// yz
			yo_yz = (int) (ys+Wy);
			int x_ = (int)(control.positionFactorX*(vv.vol.widthV-1)); 

			for (int y=0; y < Wz; y++) {
				z_ = (int) (y*zd);
				for (int x=0; x < Wy; x++) {
					int pos = (yo_yz+y)*width + x + xo;
					int y_ = (int) (x*yd);

					int val = 0xFF & vv.vol.data3D[0][vv.vol.depthV+1 - z_][y_+2][x_+2];
					pixels[pos] = 0xFF000000 | (val<<16) | (val<<8) | (val); 
					
//					int valR = 0xFF & vv.vol.nx_3D[vv.vol.depthV+1-z_][y_+2][x_+2];
//					int valG = 0xFF & vv.vol.ny_3D[vv.vol.depthV+1-z_][y_+2][x_+2];
//					int valB = 0xFF & vv.vol.nx_3D[vv.vol.depthV+1-z_][y_+2][x_+2];												
//					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}	
			}

			// xz
			yo_xz = (int) (2*ys+Wy+Wz);
			int y_ = (int)(control.positionFactorY*(vv.vol.heightV-1));

			for (int y=0; y < Wz; y++) {
				z_ = (int)(y*zd);
				for (int x=0; x < Wx; x++) {
					int pos = (yo_xz+y)*width + x + xo;
					x_ = (int) (x*xd); 

					int val = 0xFF & vv.vol.data3D[0][vv.vol.depthV+1-z_][y_+2][x_+2];
					pixels[pos] = 0xFF000000 | (val<<16) | (val<<8) | (val); 
					
//					int valR = 0xFF & vv.vol.nx_3D[vv.vol.depthV+1-z_][y_+2][x_+2];
//					int valG = 0xFF & vv.vol.ny_3D[vv.vol.depthV+1-z_][y_+2][x_+2];
//					int valB = 0xFF & vv.vol.nz_3D[vv.vol.depthV+1-z_][y_+2][x_+2];
//					pixels[pos] = 0xFF000000 | (valR<<16) | (valG<<8) | (valB);
				}
			}
		}
		vv.gui.setPositionText(Wx, Wy, Wz);

		image.setRGB(0, 0, width, height, pixels, 0, width);
	}

	public int[] getValuesfromSlices(int xm, int ym) {	
		int[] vals = {-1, -1, -1, -1, -1, -1, -1};		// lum grad mean diff z y x
		
		// unscaled size for the slices without gaps
		int my = (int) (vv.vol.heightV + vv.vol.depthV*Math.abs(control.zAspect)*2)+1;
		int mx = Math.max(vv.vol.widthV, vv.vol.heightV);

		// remaining width and height taking away the gaps
		int nWidth  = width  - 2*xs;
		int nHeight = height - 3*ys;

		float sy = nHeight/(float)my;
		float sx = nWidth/(float)mx;

		float s = Math.max(sy, sx);

		while (s*my > nHeight || s*mx > nWidth) {
			s *= 0.99;
		}

		int Wx = (int) (vv.vol.widthV*s);
		int Wy = (int) (vv.vol.heightV*s);
		int Wz = (int) (vv.vol.depthV*Math.abs(control.zAspect)*s);
		xd = vv.vol.widthV / (float)Wx;
		yd = vv.vol.heightV/ (float)Wy;
		zd = vv.vol.depthV / (float)Wz;
		yo_yz = (int) (ys+Wy);
		yo_xz = (int) (2*ys+Wy+Wz);

		// xy
		if (xm >= xo && xm < xo+Wx && ym >= yo_xy && ym < yo_xy+Wy) {
			int x = xm - xo;
			int y = ym - yo_xy;
			int x_ = (int)(x*xd);
			int y_ = (int)(y*yd);
			int z_ = (int)(control.positionFactorZ*(vv.vol.depthV-1));
			vals[0] = 0xFF & vv.vol.data3D[0][z_+2][y_+2][x_+2];
			vals[1] = 0xFF & vv.vol.grad3D[z_+2][y_+2][x_+2];
			vals[2] = 0xFF & vv.vol.mean3D[z_+2][y_+2][x_+2];
			vals[3] = 0xFF & vv.vol.diff3D[z_+2][y_+2][x_+2];
			vals[4] = z_;
			vals[5] = y_;
			vals[6] = x_;
			return vals;
		}
		// yz
		else if (xm >= xo && xm < xo+Wy && ym >= yo_yz && ym < yo_yz+Wz) {
			int x = xm - xo;
			int y = ym - yo_yz;
			int z_ = (int) (y*zd);
			int x_ = (int)(control.positionFactorX*(vv.vol.widthV-1)); 
			int y_ = (int) (x*yd);
			//System.out.println(x_ + " " +y_ + " " + z_);
			vals[0] = 0xFF & vv.vol.data3D[0][vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[1] = 0xFF & vv.vol.grad3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[2] = 0xFF & vv.vol.mean3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[3] = 0xFF & vv.vol.diff3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[4] = vv.vol.depthV-1 - z_;
			vals[5] = y_;
			vals[6] = x_;
			return vals;
		}
		// xz
		else if (xm >= xo && xm < xo+Wx && ym >= yo_xz && ym < yo_xz+Wz) {
			int x = xm - xo;
			int y = ym - yo_xz;
			int z_ = (int)(y*zd);
			int y_ = (int)(control.positionFactorY*(vv.vol.heightV-1));
			int x_ = (int) (x*xd);
			vals[0] = 0xFF & vv.vol.data3D[0][vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[1] = 0xFF & vv.vol.grad3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[2] = 0xFF & vv.vol.mean3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[3] = 0xFF & vv.vol.diff3D[vv.vol.depthV+1 - z_][y_+2][x_+2];
			vals[4] = vv.vol.depthV-1 - z_;
			vals[5] = y_;
			vals[6] = x_;
			return vals;
		}
		return vals;
	}

	void updateImage() {
		image.setRGB(0, 0, width, height, pixels, 0, width);
	}


	// draw slice at dist
	void render_Slice(){

		// TODO multi threaded		
		setPixelsToZero();

		int[] boundsXY = getXYRenderingBoundsSlice();
		int xSmin = boundsXY[0];
		int xSmax = boundsXY[1];
		int ySmin = boundsXY[2];
		int ySmax = boundsXY[3];

		float[] vecScreen = new float[3];

		if (!control.isRGB || control.lutNr != 0) {
			volData3D = vv.vol.data3D[0];
			vecScreen[2] = control.scaledDist; 
			vecScreen[1] = ySmin;
			vecScreen[0] = xSmin;

			float[] vecVolume = vv.trScreen2Volume(vecScreen);
			float xV00  = vecVolume[0], yV00  = vecVolume[1], zV00  = vecVolume[2]; 

			vecScreen[0] = xSmin+1;
			vecVolume = vv.trScreen2Volume(vecScreen);
			float xV01  = vecVolume[0], yV01  = vecVolume[1], zV01  = vecVolume[2]; 

			vecScreen[0] = xSmin;
			vecScreen[1] = ySmin+1;
			vecVolume = vv.trScreen2Volume(vecScreen);
			float xV10  = vecVolume[0], yV10  = vecVolume[1], zV10  = vecVolume[2]; 

			float dxVx = xV01 - xV00, dyVx = yV01 - yV00, dzVx = zV01 - zV00;
			float dxVy = xV10 - xV00, dyVy = yV10 - yV00, dzVy = zV10 - zV00;
			float xV_  = xV00,         yV_ = yV00,         zV_ = zV00; 

			int [] colors = vv.lookupTable.colors;
			for (int yS = ySmin; yS <= ySmax; yS++) {
				float xV = xV_; float yV = yV_; float zV = zV_; 
				for (int xS = xSmin; xS <= xSmax; xS++) {
					if (xV >=0  &&  xV < vv.vol.widthV && yV >=0  &&  yV < vv.vol.heightV && zV >=0  && zV < vv.vol.depthV) {
						int val = interpolation.get(volData3D, zV, yV, xV);
						pixels[yS*width + xS] = colors[val];
					}
					xV += dxVx; yV += dyVx; zV += dzVx;
				}
				xV_ += dxVy; yV_ += dyVy; zV_ += dzVy;		
			}
		}
		else {
			vecScreen[2] = control.scaledDist; // z

			for (int yS = ySmin; yS <= ySmax; yS++) {
				vecScreen[1] = yS;
				for (int xS = xSmin; xS <= xSmax; xS++) {
					vecScreen[0] = xS;

					float[] xyzV = vv.trScreen2Volume(vecScreen);
					float xV  = xyzV[0]; 
					float yV  = xyzV[1]; 
					float zV  = xyzV[2]; 

					if (xV >=0  &&  xV < vv.vol.widthV && yV >=0  &&  yV < vv.vol.heightV && zV >=0  && zV < vv.vol.depthV) { 
						int r = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[1], zV, yV, xV)][0];
						int g = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[2], zV, yV, xV)][1];
						int b = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[3], zV, yV, xV)][2];
						pixels[yS*width + xS] |= 0xFF000000 | (r<<16) | (g<<8) | b;
					}
				}
			}
		}
		updateImage();
	}

	public void render_SliceAndBorders(){

		setPixelsToZero();

		int[] boundsXY = getXYRenderingBoundsSlice();
		int xSmin = boundsXY[0];
		int xSmax = boundsXY[1];
		int ySmin = boundsXY[2];
		int ySmax = boundsXY[3];

		float[] vS = new float[3];
		int widthS =  control.windowWidthImageRegion; 

		if (control.isRGB && control.lutNr == 0) {
			for (int ch = 0; ch < 3; ch++) {

				int shift = (2-ch)*8;
				volData3D = vv.vol.data3D[ch+1];

				vS[2] = control.scaledDist; 	// draw slice at dist

				for (int y = ySmin; y <= ySmax; y++) {
					vS[1] = y;
					for (int x = xSmin; x <= xSmax; x++) {
						int pos = y*widthS + x;
						vS[0] = x;

						float[] xyzV = vv.trScreen2Volume(vS);
						float xV  = xyzV[0]; 
						float yV  = xyzV[1]; 
						float zV  = xyzV[2]; 

						if (xV >=0  &&  xV <= vv.vol.widthV  && yV >=0  &&  yV <= vv.vol.heightV && zV >=0  &&  zV <= vv.vol.depthV) 
							pixels[pos] |= 0xFF000000 | (interpolation.get(volData3D, zV, yV, xV)<< shift);				
					}
				}		

				float[] boundsXYZ = getXYZRenderingBoundsVolume();
				xSmin = (int) boundsXYZ[0];
				xSmax = (int) boundsXYZ[1];
				ySmin = (int) boundsXYZ[2];
				ySmax = (int) boundsXYZ[3];
				float zSmax = boundsXYZ[5];

				for (int y = ySmin; y < ySmax; y++) {

					for (int x = xSmin; x < xSmax; x++) {
						int pos = y*widthS + x;
						if (((pixels[pos] >> shift) & 0xFF) == 0) {
							if (vv.cube.isInside(x, y)) {
								vS[0] = x;
								vS[1] = y;
								vS[2] = control.scaledDist+1; // z

								float[] xyzV = vv.trScreen2Volume(vS);
								float xV  = xyzV[0]; 
								float yV  = xyzV[1]; 
								float zV  = xyzV[2]; 

								vS[2] = zSmax; // z
								xyzV = vv.trScreen2Volume(vS);
								float xV2  = xyzV[0]; 
								float yV2  = xyzV[1]; 
								float zV2  = xyzV[2]; 

								float nd = zSmax - (control.scaledDist+1);

								float nd1 = 1/nd;
								float dx = (xV2-xV)*nd1;
								float dy = (yV2-yV)*nd1;
								float dz = (zV2-zV)*nd1;

								// check if we need to follow the ray
								if ( (xV < 0 && dx < 0) || (xV > vv.vol.widthV && dx > 0) || (yV < 0 && dy < 0) || 
										(yV > vv.vol.heightV && dy > 0) || (zV < 0 && dz < 0) || (zV > vv.vol.depthV && dz > 0) ) 
									continue;

								for (; nd >= 0; nd-- ) {
									if (xV >=0  &&  xV <= vv.vol.widthV  && yV >=0  &&  yV <= vv.vol.heightV && zV >=0  &&  zV <= vv.vol.depthV) { 
										int val = interpolation.get(volData3D, zV, yV, xV);	
										pixels[pos] |= (0xFF << 24) | (val<< shift);
										break;
									}
									xV += dx;
									yV += dy;
									zV += dz;
								}
							}
						}	
					}
				}
			}
		}
		else {
			// draw slice at scaledDist
			vS[2] = control.scaledDist; // z
			volData3D = vv.vol.data3D[0];

			for (int y = ySmin; y < ySmax; y++) {
				vS[1] = y;
				for (int x = xSmin; x < xSmax; x++) {
					int pos = y*widthS + x;
					vS[0] = x;

					float[] xyzV = vv.trScreen2Volume(vS);
					float xV  = xyzV[0]; 
					float yV  = xyzV[1]; 
					float zV  = xyzV[2]; 

					if (xV >=0  &&  xV <= vv.vol.widthV  && yV >=0  &&  yV <= vv.vol.heightV && zV >=0  &&  zV <= vv.vol.depthV) { 
						pixels[pos] = vv.lookupTable.colors[interpolation.get(volData3D, zV, yV, xV)];
					}
				}
			}		

			float[] boundsXYZ = getXYZRenderingBoundsVolume();
			xSmin = (int) boundsXYZ[0];
			xSmax = (int) boundsXYZ[1];
			ySmin = (int) boundsXYZ[2];
			ySmax = (int) boundsXYZ[3];
			float zSmax = boundsXYZ[5];

			for (int y = ySmin; y <= ySmax; y++) {
				vS[1] = y;
				for (int x = xSmin; x <= xSmax; x++) {
					int pos = y*widthS + x;
					if (pixels[pos] == 0) {
						if (vv.cube.isInside(x, y)) {
							vS[0] = x;
							vS[2] = control.scaledDist+1; 

							float[] xyzV = vv.trScreen2Volume(vS);
							float xV  = xyzV[0]; 
							float yV  = xyzV[1]; 
							float zV  = xyzV[2]; 

							vS[2] = zSmax; 
							xyzV = vv.trScreen2Volume(vS);

							float nd = zSmax - (control.scaledDist+1);
							float nd1 = 1/nd;
							float dx = (xyzV[0]-xV)*nd1;
							float dy = (xyzV[1]-yV)*nd1;
							float dz = (xyzV[2]-zV)*nd1;

							// check if we need to follow the ray
							if ( (xV < 0 && dx < 0) || (xV > vv.vol.widthV && dx > 0) || (yV < 0 && dy < 0) || 
									(yV > vv.vol.heightV && dy > 0) || (zV < 0 && dz < 0) || (zV > vv.vol.depthV && dz > 0) ) 
								continue;

							for (; nd >= 0; nd-- ) {
								if (xV >=0 && xV <= vv.vol.widthV && yV >=0 && yV <= vv.vol.heightV && zV >=0 && zV <= vv.vol.depthV) { 
									pixels[pos] = vv.lookupTable.colors[interpolation.get(volData3D, zV, yV, xV)];
									break;
								}
								xV += dx;
								yV += dy;
								zV += dz;
							}
						}
					}	
				}
			}
		}
		updateImage();
	}

	private int maxThreads = 8;
	private int numThreads = 1;
	private int subMax;
	private int counter = 0;
	private boolean doStopRendering;
	private boolean isRendering = false;
	private int sub;
	private boolean isWaitingForRendering;
	private float[] light;
	private boolean lastReady;
	private boolean isRGB;


	public void render_volume(int sub){
		if (control.LOG) System.out.println("render volume, sub: " + sub);
		
		isRendering = true;
		counter = numThreads = (sub == subMax) ? 1 : maxThreads;
		
		setPixelsToZero();

		int actualInterpolationMode = control.interpolationMode;
		if (sub > 1)
			control.interpolationMode = Control.NN;

		getXYRenderingBoundsSlice(); 
		
		// light 
		light = vv.trLightVolume2Screen(1, 0, 0);
		
		lightRed =   control.lightRed; 
		lightGreen = control.lightGreen; 
		lightBlue =  control.lightBlue;  
		
		float[] boundsXYZ = getXYZRenderingBoundsVolume();
		int xSmin = (int) boundsXYZ[0];
		int xSmax = (int) boundsXYZ[1];
		int ySmin = (int) boundsXYZ[2];
		int ySmax = (int) boundsXYZ[3];
		float zSmin = boundsXYZ[4];
		float zSmax = boundsXYZ[5];

		float[] xyzV1 = vv.trScreen2Vol(0, 0, 0);
		float[] xyzV2 = vv.trScreen2Vol(0, 0, control.scale);
		float dxV = xyzV2[0] - xyzV1[0];
		float dyV = xyzV2[1] - xyzV1[1];
		float dzV = xyzV2[2] - xyzV1[2];

		float sample = control.sampling;
		if (sub == 3)
			sample = 1;
		if (sub > 3)
			sample = 0.5f;
		
		if (control.LOG) System.out.println("sample " + sample);
		int nd = (int) (sample*(zSmax - zSmin)/control.scale);
		dxV  /= sample;
		dyV  /= sample;
		dzV  /= sample;

		if (control.alphaMode == Control.ALPHA1) {
			for (int i = 0; i < vv.a1_R.length; i++) {
				float a = (vv.tf_a1.a1[i])/255f; 
				if (a < 0) a = 0;
				a = a * a;
				vv.a1_R[i] = Math.min(1,a/sample); 		
			}				
		}
		else if (control.alphaMode == Control.ALPHA2) {
			for (int x = 0; x < 256; x++) {
				for (int y = 0; y < 128; y++) {
					float a = (vv.tf_a2.a2[x][y])/255f;
					if (a < 0) a = 0;
					a = a * a;
					vv.a2_R[x][y] = Math.min(1,a/sample); 		
				}
			}
		}
		else if (control.alphaMode == Control.ALPHA3) {
			for (int x = 0; x < 256; x++) {
				for (int y = 0; y < 128; y++) {
					float a = (vv.tf_a3.a3[x][y])/255f;
					if (a < 0) a = 0;
					a = a * a;
					vv.a3_R[x][y] = Math.min(1,a/sample); 		
				}
			}
		}
		// calculate gradient from alpha
		if ((!control.drag && control.alphaWasChanged)) 
			vv.vol.calculateGradients();

		volData3D = vv.vol.data3D[0];
		
		ySmin = (ySmin/sub)*sub;
		int stripHeight = (ySmax-ySmin) / numThreads;
		stripHeight = (stripHeight/sub) *sub;
		int stripStart = ySmin;

		isRGB = control.isRGB && control.lutNr == 0;
		
		for(int i = 0; i < numThreads-1; i++, stripStart += stripHeight) 
			new RenderCalculations(sub, nd, dxV, dyV, dzV, xSmin, xSmax, stripStart, stripStart+stripHeight, zSmin).execute();
		new RenderCalculations(sub, nd, dxV, dyV, dzV, xSmin, xSmax, stripStart, stripStart+stripHeight, zSmin).execute();
			
		control.interpolationMode = actualInterpolationMode;			
	}


	private class RenderCalculations extends SwingWorker<Void, Void> {

		private int sub, nd, xSMin, xSMax, ySMin, ySMax;
		private float dxV, dyV, dzV, zSMin;
		
		public RenderCalculations(int sub, int nd, float dxV, float dyV, float dzV, 
				int xSMin, int xSMax, int ySMin, int ySMax, float zSMin) {
			this.sub = sub;
			this.nd = nd;
			this.dxV = dxV;
			this.dyV = dyV;
			this.dzV = dzV; 
			this.xSMin = xSMin;
			this.xSMax = xSMax;
			this.ySMin = ySMin;
			this.ySMax = ySMax;
			this.zSMin = zSMin;
		}

		@Override
		protected Void doInBackground(){
			return doRendering();
		}

		@Override
		protected void done(){
			counter--;
			if (doStopRendering && counter == 0) {
				isRendering = false;
				doStopRendering = false;
				sub = subMax;
				startVolumeRendering(sub);
			}
			else if (!doStopRendering && counter == 0) {
				updateImage();
				isRendering = false;
				if (sub==1)
					vv.gui.signalReady();
				vv.gui.imageRegion.paintImmediately(0, 0, vv.gui.imageRegion.getWidth(), vv.gui.imageRegion.getHeight());
				
				if (isWaitingForRendering) {
					isWaitingForRendering = false;
					sub = subMax;
					startVolumeRendering(sub);
				}
				else if (sub >= 3) {
					if (sub <= 5)
						sub -= 2;
					else {
						sub = sub/2;
						if (sub %2 == 0)
							sub++;
					}
					startVolumeRendering(sub);
				}
			}
		}

		private Void doRendering() {
			int s_2 = sub/2;

			// Startpunkt xSMin ySMin zSMin (tiefster Punkt) in Screenkoordinaten
			float[] xyzV = vv.trScreen2Vol(xSMin+s_2, ySMin+s_2, zSMin);
			float x0V = xyzV[0], y0V = xyzV[1], z0V = xyzV[2]; 

			// inkrementelle Aenderung in Volumenkoordinaten
			xyzV = vv.trScreen2Vol(xSMin+s_2+1, ySMin+s_2, zSMin);
			float dxVx = xyzV[0] - x0V, dyVx = xyzV[1] - y0V, dzVx = xyzV[2] - z0V; 

			xyzV = vv.trScreen2Vol(xSMin+s_2, ySMin+s_2+1, zSMin);
			float dxVy = xyzV[0] - x0V, dyVy = xyzV[1] - y0V, dzVy = xyzV[2] - z0V; 


			for (int yS = ySMin; yS < ySMax; yS++) {				
				for (int j=0, xS = xSMin; xS < xSMax; xS++, j++) {
					if (doStopRendering) return null;
					if (vv.cube.isInside(xS, yS)) {
						if (yS%sub == 0 && xS%sub == 0) {
							boolean hasBeenInTheVolume = false;

							float rand = (float) (-Math.random());
							float xV = x0V + j*dxVx + rand*dxV; 
							float yV = y0V + j*dyVx + rand*dyV; 
							float zV = z0V + j*dzVx + rand*dzV; 

							int ns = 0;		// check where to start rendering
							if(xV < 0) 
								if  (dxV > 0) ns = (int) (-xV / dxV);
								else continue;
							else if(xV > vv.vol.widthV) 
								if (dxV < 0) ns = (int) Math.max(((vv.vol.widthV-xV)/dxV),ns);
								else continue;
							if(yV < 0) 
								if (dyV > 0) ns = (int) Math.max((-yV/dyV), ns);
								else continue;
							else if(yV > vv.vol.heightV) 
								if (dyV < 0) ns = (int) Math.max(((vv.vol.heightV-yV)/dyV), ns);
								else continue;
							if(zV < 0) 
								if (dzV > 0) ns = (int) Math.max((-zV/dzV), ns);
								else continue;
							else if(zV > vv.vol.depthV) 
								if(dzV < 0) ns = (int) Math.max(((vv.vol.depthV-zV)/dzV), ns);
								else continue;

							xV += ns*dxV;
							yV += ns*dyV;
							zV += ns*dzV;		

							float valR = 0, valG=0, valB=0, nx = 0, ny= 0, nz= 0, a, aNext = 1, sumA = 1;
							int valProj = 0, mean=0, diff=0, rMax=0, gMax=0, bMax=0, val;
							int[] actLut = null;

							boolean didStartInVolume = false;
							for (int n = ns; n < nd; n++, xV += dxV, yV += dyV, zV += dzV) {
								if (xV >= 0 && xV <= vv.vol.widthV && yV >= 0 && yV <= vv.vol.heightV && zV >= 0 && zV <= vv.vol.depthV) { 
									hasBeenInTheVolume = true;
									
									if (control.alphaMode == Control.ALPHA1) {
										val = interpolation.get(volData3D, zV, yV, xV);
										a = vv.a1_R[val];
										if (a == 0) continue;
										actLut = vv.lookupTable.lut[val];
									}
									else if (control.alphaMode == Control.ALPHA2) {
										val = interpolation.get(volData3D, zV, yV, xV);
										int grad = Math.min(127,interpolation.get(vv.vol.grad3D, zV, yV, xV));
										a = vv.a2_R[val][grad];
										if (a == 0) continue;
										actLut = vv.lookupTable.lut2D_2[val][grad];
									}
									else if (control.alphaMode == Control.ALPHA3) {
										mean = interpolation.get(vv.vol.mean3D, zV, yV, xV);
										diff = Math.min(127,interpolation.get(vv.vol.diff3D, zV, yV, xV));
										a = vv.a3_R[mean][diff];
										if (a == 0) continue;
										actLut = vv.lookupTable.lut2D_3[mean][diff];
										val = mean;
									}
									else { // ALPHA4
										a = interpolation.get(vv.vol.aPaint_3D, zV, yV, xV);
										if (a == 0) continue;
										a *= 0.00392f; //  / 255;
										a = a * a; 	
										val = interpolation.get(vv.vol.col_3D, zV, yV, xV);
										actLut = vv.lookupTable.lut[val];
									}
									
									if (n - ns < 3 && (xV >= 3 && xV <= vv.vol.widthV-3 && yV >= 3 && yV <= vv.vol.heightV-3 && zV >= 3 && zV <= vv.vol.depthV-3))
										didStartInVolume = true;
									
									int r, g, b;
									if(isRGB) {
										r = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[1], zV, yV, xV)][0];
										g = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[2], zV, yV, xV)][1];
										b = vv.lookupTable.lut[interpolation.get(vv.vol.data3D[3], zV, yV, xV)][2];
									}
									else {
										r = actLut[0]; g = actLut[1]; b = actLut[2];	
									}
									
									if (control.renderMode == Control.VOLUME) {
										float an = a*aNext;
										valR += an * r;
										valG += an * g;
										valB += an * b;	
										
										if (control.useLight) {
											int dx = interpolation.get(vv.vol.nx_3D, zV, yV, xV) - 128;
											int dy = interpolation.get(vv.vol.ny_3D, zV, yV, xV) - 128;
											int dz = interpolation.get(vv.vol.nz_3D, zV, yV, xV) - 128;
											nx += an * dx;
											ny += an * dy;
											nz += an * dz;

										}
										aNext *= (1-a);
										if (aNext < 0.02) {
											aNext = 0; break;
										}
									}
									else if (control.renderMode == Control.PROJECTION) {
										valR += a * r;
										valG += a * g;
										valB += a * b;
										sumA += a;	
									}
									else { // if (renderMode == PROJECTION_MAX) {
										if (isRGB) {
											if (r+g+b > valProj) {
												valProj = r+g+b;
												rMax = r;
												gMax = g;
												bMax = b;
											}
										}
										else if (val > valProj) 
											valProj = val;
									}
								}
								else if (hasBeenInTheVolume) // has left the volume
									break;
							}

							if (control.renderMode == Control.VOLUME) {
								if (didStartInVolume) {
									nx += 20*dxV; ny += 20*dyV; nz += 20*dzV;
								}
								int alpha = (int) ((1-aNext)*255); 
								if (alpha > 0) {
									if (control.useLight) {
										// Oberflächen Normalen-Vektor 
										float[] xyz0 = vv.trVolume2Screen(0, 0, 0);
										float[] xyz = vv.trVolume2Screen(nx/control.scale, ny/control.scale, nz/(control.zAspect*control.scale));

										float[] n = new float[3]; 
										n[0] = xyz[0]-xyz0[0];
										n[1] = xyz[1]-xyz0[1];
										n[2] = xyz[2]-xyz0[2];
											
										float lenN = (n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
										if (lenN > 0) {
											lenN = (float) (1/Math.sqrt(lenN));
											n[0] *= lenN;
											n[1] *= lenN;
											n[2] *= lenN;
										}
										
										float diffuse = (n[0]*light[0] + n[1]*light[1] + n[2]*light[2]);

										// specular // Reflexion 2*(N*L)*N - L
										float sp = 2*(n[0]*light[0] + n[1]*light[1] + n[2]*light[2]);  // scalar product sp = 2*N*L
										//float[] r = new float[3];
										//r[0] = sp*n[0] - light[0];
										//r[1] = sp*n[1] - light[1];
										//r[2] = sp*n[2] - light[2];

										//float[] v = new float[3]; // view
										//v[2] = 1;
										//float spec = Math.max(0, r[0]*v[0] + r[1]*v[1] + r[2]*v[2]);
										float spec = Math.max(0, (sp*n[2] - light[2]));
										spec = (float) (Math.pow(spec,control.shineValue)*((control.shineValue+2)/(2*Math.PI)));

										float lightFactor = control.ambientValue  + diffuse*control.diffuseValue  + spec*control.specularValue;
										valR = (int) Math.min(255, Math.max(0, control.objectLightValue*valR + lightRed * lightFactor));
										valG = (int) Math.min(255, Math.max(0, control.objectLightValue*valG + lightGreen*lightFactor));
										valB = (int) Math.min(255, Math.max(0, control.objectLightValue*valB + lightBlue* lightFactor));										

//										valR = (int) Math.min(255, Math.max(0, 128 + 127*n[0]));
//										valG = (int) Math.min(255, Math.max(0, 128 + 127*n[1]));
//										valB = (int) Math.min(255, Math.max(0, 128 + 127*n[2]));		
										
//										valR = (int) (lenN*255);
//										valG = (int) (lenN*255);
//										valB = (int) (lenN*255);											

									}

									pixels[yS*width + xS] = (alpha << 24) | ((int) valR << 16) | ((int) valG << 8) | ((int) valB);
								}
							}
							else if (control.renderMode == Control.PROJECTION) {
								int al = 255;  
								valR /= sumA;
								valG /= sumA;
								valB /= sumA;
								pixels[yS*width + xS] = (al << 24) | ((int) valR << 16) | ((int) valG << 8) | ((int) valB);
							}
							else {
								if (isRGB)
									pixels[yS*width + xS] = (255 << 24) | (rMax << 16) | (gMax << 8) | bMax;
								else
									pixels[yS*width + xS] = vv.lookupTable.colors[valProj];
							}
						}
						else { // copy previous value (for sub > 1)
							pixels[yS*width + xS] = pixels[ (yS/sub)*sub*width + (xS/sub)*sub];	
						}
					}	
				}
				x0V += dxVy; 
				y0V += dyVy; 
				z0V += dzVy; 
			}
			return null;
		}
	}
	
	
	public void render_sphere(){
		if (control.LOG) System.out.println("render sphere");
		
		// light 
		light = vv.trLightVolume2Screen(-1, 0, 0);
		lightRed = control.lightRed; 
		lightGreen = control.lightGreen; 
		lightBlue = control.lightBlue;  
		
		int rad = 27;

		for (int y_ = 0; y_ < height; y_++) {
			int y = y_- height/2;
			for (int x_ = 0; x_ < width; x_++) {
				int x = x_- width/2;
				float d = (float) Math.sqrt(x*x + y*y); 
				if (d <= rad+1){
					float z = d - rad; 
					int alpha = 255;
					if (d > rad) {
						alpha = (int) ((1-z)*255);
					}
					int valR=64, valG=64, valB=64;
					
					if (control.useLight) {
						float[] n = new float[3]; 
						n[0] = x;
						n[1] = y;
						n[2] = z;
						float lenN = (float) (1/Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]));
						n[0] *= lenN;
						n[1] *= lenN;
						n[2] *= lenN;

						// Reflexion 2*(N*L)*N - L  
						float sp = n[0]*light[0] + n[1]*light[1] + n[2]*light[2];  // scalar product sp = N*L
						float[] r = new float[3];
						r[0] = 2*sp*n[0] - light[0];
						r[1] = 2*sp*n[1] - light[1];
						r[2] = 2*sp*n[2] - light[2];

						float diffuse = n[0]*light[0] + n[1]*light[1] + n[2]*light[2];

						// view
						float[] v = new float[3];
						v[2] = -1;
						// specular
						float spec = Math.max(0, r[2]*v[2]);
						spec = (float) (Math.pow(spec,control.shineValue)*((control.shineValue+2)/(2*Math.PI)));

						float lightFactor = control.ambientValue + diffuse*control.diffuseValue + spec*control.specularValue;
						valR = (int) Math.min(255, Math.max(0, 64 + lightRed  *lightFactor));
						valG = (int) Math.min(255, Math.max(0, 64 + lightGreen*lightFactor));
						valB = (int) Math.min(255, Math.max(0, 64 + lightBlue *lightFactor));
					}	
					pixels[y_*width + x_] = (alpha << 24) | ((int) valR << 16) | ((int) valG << 8) | ((int) valB);
				}
			}	
		}
		updateImage();
	}
	
	public void render_LED(boolean ready){
		
		if (ready == lastReady)
			return;
		lastReady = ready;
		if (control.LOG) System.out.println("render LED");
		
		// light 
		float[] lightLED = { -0.7f, -0.7f, 0.1f};
		int ledRed = 220; 
		int ledGreen = 80; 
		int ledBlue = 40;  
		if(ready) {
			ledRed = 80; 
			ledGreen = 180; 
			ledBlue = 40;  
		}
		int rad = 7;

		for (int y_ = 0; y_ < height; y_++) {
			int y = y_- height/2;
			for (int x_ = 0; x_ < width; x_++) {
				int x = x_- width/2;
				float d = (float) Math.sqrt(x*x + y*y); 
				if (d <= rad+1){
					float z = d - rad; 
					int alpha = 255;
					if (d > rad) 
						alpha = (int) ((1-z)*255);
					
					float[] n = new float[3]; 
					n[0] = x;
					n[1] = y;
					n[2] = z;
					float lenN = (float) (1/Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]));
					n[0] *= lenN;
					n[1] *= lenN;
					n[2] *= lenN;

					// Reflexion 2*(N*L)*N - L  
					float sp = n[0]*lightLED[0] + n[1]*lightLED[1] + n[2]*lightLED[2];  // scalar product sp = N*L
					float[] r = new float[3];
					r[0] = 2*sp*n[0] - lightLED[0];
					r[1] = 2*sp*n[1] - lightLED[1];
					r[2] = 2*sp*n[2] - lightLED[2];

					float diffuse = n[0]*lightLED[0] + n[1]*lightLED[1] + n[2]*lightLED[2];

					// view
					float[] v = new float[3];
					v[2] = -1;
					// specular
					float spec = Math.max(0, r[2]*v[2]);
					spec = (float) (Math.pow(spec,6)*((6+2)/(2*Math.PI)));

					float lightFactor = 1f + diffuse*0.5f + 2f*spec;
					int valR = (int) Math.min(255, Math.max(0, ledRed  *lightFactor));
					int valG = (int) Math.min(255, Math.max(0, ledGreen*lightFactor));
					int valB = (int) Math.min(255, Math.max(0, ledBlue *lightFactor));

					pixels[y_*width + x_] = (alpha << 24) | ((int) valR << 16) | ((int) valG << 8) | ((int) valB);
				}
			}	
		}
		updateImage();
	}
	

	float[] getXYZRenderingBoundsVolume() {

		vv.cube.findIntersections(control.scaledDist);
		float[][] iS = vv.cube.getCorners();

		float[] xyzS = vv.trVolume2Screen(iS[0]);
		float xi = xyzS[0];
		float yi = xyzS[1];
		float zi = xyzS[2];
		float xMin = xi, xMax = xi, yMin = yi, yMax = yi, zMin = zi, zMax = zi;

		for (int i = 1; i < 8; i++) {
			xyzS = vv.trVolume2Screen(iS[i]);
			xi = xyzS[0]; 
			if (xi < xMin) xMin = xi;
			else if (xi > xMax) xMax = xi;
			yi = xyzS[1]; 
			if (yi < yMin) yMin = yi;
			else if (yi > yMax) yMax = yi;
			zi = xyzS[2];				
			if (zi > zMax) zMax = zi;
			else if (zi < zMin) zMin = zi;
		}

		int widthS = control.windowWidthImageRegion; 
		int heightS = control.windowHeight;

		if (xMin < 0)  xMin = 0;
		if (xMax >= widthS) xMax = widthS-1;
		if (yMin < 0) yMin = 0;
		if (yMax >= heightS) yMax = heightS-1;
		if (control.scaledDist > zMin)
			zMin = control.scaledDist;

		float[] xyBounds = {xMin, xMax, yMin, yMax, zMin, zMax};
		return xyBounds;
	}

	int[] getXYRenderingBoundsSlice() {

		vv.cube.findIntersections(control.scaledDist);
		float[][] iS = vv.cube.getInterSections();
		float[] xyzS  = vv.trVolume2Screen(iS[0]);

		int[] x = new int[6];
		int[] y = new int[6];

		int xi = x[0] = (int)xyzS[0];
		int yi = y[0] = (int)xyzS[1];

		int xSMin = xi, xSMax = xi, ySMin = yi, ySMax = yi;

		for (int i = 1; i < 6; i++) {
			xyzS = vv.trVolume2Screen(iS[i]);
			xi = x[i] = (int) xyzS[0]; 
			if (xi < xSMin) xSMin = xi;
			else if (xi > xSMax) xSMax = xi;
			yi = y[i] = (int) xyzS[1]; 
			if (yi < ySMin) ySMin = yi;
			else if (yi > ySMax) ySMax = yi;
		}

		// compute center of intersections
		int xm = 0, ym = 0;
		for (int i = 0; i < y.length; i++) {
			xm += x[i];
			ym += y[i];
		}
		xm /= 6;
		ym /= 6;

		Map<Integer, Double> map = new HashMap<Integer, Double>();
		final double[] angle = new double[6];
		for (int i = 0; i < y.length; i++) {
			angle[i] = Math.atan2(y[i]-ym, x[i]-xm);
			map.put(i, angle[i]);
		}

		final Integer[] id = { 0, 1, 2, 3, 4, 5 };

		Arrays.sort(id, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return Double.compare(angle[o1], angle[o2]);
			}
		});

		Color color = (control.showClipLines) ? Color.ORANGE : new Color (0,0,0,0);
		for (int i = 0; i < 6; i++)
			vv.gui.imageRegion.setClipLine(i, x[id[i]], y[id[i]], x[id[(i+1)%6]], y[id[(i+1)%6]], -1, color);

		xSMin --;
		xSMax ++;
		ySMin --;
		ySMax ++;

		int widthS = control.windowWidthImageRegion; 
		int heightS = control.windowHeight; 

		if (xSMin < 0)  xSMin = 0;
		if (xSMax >= widthS) xSMax = widthS-1;
		if (ySMin < 0) ySMin = 0;
		if (ySMax >= heightS) ySMax = heightS-1;

		int[] xyBounds = {xSMin, xSMax, ySMin, ySMax};
		return xyBounds;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public Image getImage() {
		return image;
	}

	public void startVolumeRendering(int sub) {
		this.sub = sub;
		render_volume(sub);
	}

	public void startVolumeRendering() {

		if (isRendering && sub < subMax) {
			doStopRendering = true; 
			isWaitingForRendering = true;
			return;
		}
		sub = subMax;
		if (!isRendering) 
			render_volume(sub);
		else 
			isWaitingForRendering = true;
	}

}
