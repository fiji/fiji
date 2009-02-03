package jRenderer3D;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.PixelGrabber;

class SurfacePlot {
	
//	Surface plot modes
//	private static final int PLOT_DOTSNOLIGHT = 0;
//	private static final int PLOT_DOTS = 1;
//	private final static int PLOT_LINES = 2;
//	private final static int PLOT_MESH = 3;
//	private final static int PLOT_FILLED = 4;
	
	/*****************************************************************************************/
	
	/**
	 * The size of the surface plot data 
	 */
	private int gridWidth = 256; 
	private int gridHeight = 256;
	
	private SurfacePlotData[] plotList = null;
	
	Image image;
	private int[] bufferPixels;
	private double[] zbufferPixels;
	private int bufferWidth;
	private int bufferHeight;
	private int lutNr = JRenderer3D.LUT_ORIGINAL;
	Lut lut;
	private Transform tr;
	private double light;
	private int surfacePlotMode;
	private double xCenter;
	private double yCenter;
	private double zCenter;
	private int min = 0;
	private int max = 100;
	private int inversefactor = 1;
	
	private int[] pixelsOrigColor;
	private int[] pixelsOrigLum;
	private int widthOrig;
	private int heightOrig;
	private int widthTex;
	private int heightTex;
	private int[] pixelsTexColor;
	private byte[] maskPixels;
	
	
	protected void draw() {
		if (surfacePlotMode == JRenderer3D.SURFACEPLOT_FILLED)
			surfacePlotFilled();
		else if (surfacePlotMode == JRenderer3D.SURFACEPLOT_ISOLINES)
			surfacePlotIsoLines();
		else if (surfacePlotMode == JRenderer3D.SURFACEPLOT_MESH)
			surfacePlotMesh();
		else if (surfacePlotMode == JRenderer3D.SURFACEPLOT_LINES)
			surfacePlotLines();
		else if (surfacePlotMode == JRenderer3D.SURFACEPLOT_DOTS)
			surfacePlotDots();
		else if (surfacePlotMode == JRenderer3D.SURFACEPLOT_DOTSNOLIGHT)
			surfacePlotDotsNoLight();
	}
	
	protected void setSurfacePlotImage(ImagePlus imp){
	
			int widthTmp = imp.getWidth();
			int heightTmp = imp.getHeight();
	
			ImageProcessor ip = imp.getProcessor();
	
			lut = new Lut();
	
			int[] pixelsTmp = new int[widthTmp*heightTmp];
	
			image = imp.getImage();
			PixelGrabber pg =  new PixelGrabber(image, 0, 0, widthTmp, heightTmp, pixelsTmp, 0, widthTmp);
	
			try {
				pg.grabPixels();
			} catch (InterruptedException ex) {IJ.error("error grabbing pixels");}
	
			boolean isLut = ip.isColorLut();
	
			byte[] lutPixels = null;
	
			int bitDepth = imp.getBitDepth();
	
			if( isLut ) {
				if (bitDepth == 8)
					lutPixels = (byte []) ip.getPixels();
				else { 
					lutPixels = new byte[widthTmp*heightTmp];
	
					double min_ = ip.getMin();
					double max_ = ip.getMax();
					double a = 0, b = 1;
	
					Calibration cal = imp.getCalibration();
	
					if (cal != null) {
						if (cal.calibrated()) {
							min_ = cal.getCValue((int)min_);
							max_ = cal.getCValue((int)max_);
	
							double[] coef = cal.getCoefficients();
							if (coef != null) {		
								a = coef[0];
								b = coef[1];
							}
						}
					}
	
					float scale = (float) (255f/(max_-min_));
					if (bitDepth == 16) {
						short[] pixels = (short[]) ip.getPixels();
						int pos = 0;
						for (int y = 0; y < heightTmp; y++) {
							for (int x = 0; x <widthTmp; x++) {
	
								int val = (int) ((int)(0xFFFF & pixels[pos++])*b + a - min_);
								if (val<0f) val = 0;
								val = (int)(val*scale);
								if (val>255) val = 255;
								lutPixels[y*widthTmp+x] = (byte)(val);  
							}
						}
					}
					
					if (bitDepth == 32) {
						float[] pixels = (float[]) ip.getPixels();
	
						int pos = 0;
						for (int y = 0; y < heightTmp; y++) {
							for (int x = 0; x <widthTmp; x++) {
								float value = (float) (pixels[pos++] - min);
								if (value<0f) value = 0f;
								int ivalue = (int)(value*scale);
	
								if (ivalue>255) ivalue = 255;
								lutPixels[y*widthTmp+x] = (byte)(ivalue);  
							}
						}
					}
				}
			}
			
			Roi roi = imp.getRoi();
	
			if (roi != null) {
				ImageProcessor mask = roi.getMask();
				ImageProcessor ipMask;
	
				maskPixels = null;
				
				if (mask != null) {
					ipMask = mask.duplicate();
					maskPixels = (byte[])ipMask.getPixels();
				}
	
				Rectangle rect = roi.getBoundingRect();
				if (rect.x < 0)
					rect.x = 0;
				if (rect.y < 0)
					rect.y = 0;
	
				widthOrig = rect.width;
				heightOrig = rect.height;
	
				pixelsOrigColor = new int[widthOrig*heightOrig];
				pixelsOrigLum = new int[widthOrig*heightOrig];
	
				for (int j=0, y=rect.y; y<rect.y+rect.height; y++) {
					int offset = y*widthTmp;
	
					for (int x=rect.x; x< rect.x+rect.width ; x++, j++) {
						int i = offset + x;
	
						int c =pixelsOrigColor[j] = pixelsTmp[i];
	
						int lum;
						if (!isLut) { 
							int r = ((c >> 16) & 255);
							int g = ((c >>  8) & 255);
							int b = ((c      ) & 255);
	
							lum = (int)(0.299*r  + 0.587*g + 0.114*b);
						}
						else
							lum = (int)(0xFF & lutPixels[i]);
	
						pixelsOrigLum[j] = lum;
					}
				}
			}
			else {
				widthOrig = widthTmp;
				heightOrig = heightTmp;
	
				pixelsOrigColor = new int[widthOrig*heightOrig];
				pixelsOrigLum = new int[widthOrig*heightOrig];
	
				for (int y = 0; y < heightTmp; y++) {
					for (int x = 0; x < widthTmp; x++) {
						int pos = y * widthTmp + x;
	
						int c = pixelsOrigColor[pos] = pixelsTmp[pos];
	
						int lum; 
	
						int r = ((c >> 16) & 255);
						int g = ((c >>  8) & 255);
						int b = ((c      ) & 255);
						if (!isLut) {
							lum = (int)Math.round(0.299*r  + 0.587*g + 0.114*b);
						}
						else
							lum = (int)(0xFF & lutPixels[pos]);
	
						pixelsOrigLum[pos] = lum;
					}
				}
			}
		}

	protected void setSurfacePlotTextureImage(ImagePlus imp){

		widthTex = imp.getWidth();
		heightTex = imp.getHeight();

		pixelsTexColor = new int[widthTex*heightTex];

		image = imp.getImage();
		PixelGrabber pg =  new PixelGrabber(image, 0, 0, widthTex, heightTex, pixelsTexColor, 0, widthTex);

		try {
			pg.grabPixels();
		} catch (InterruptedException ex) {
			IJ.error("error grabbing pixels");
			pixelsTexColor =  null;
		}
	}
	


	protected void resample(){
		
		plotList = new SurfacePlotData[gridWidth*gridHeight];
		
		if (pixelsOrigColor != null && pixelsOrigLum != null) {
			double xOffset = xCenter;
			double yOffset = yCenter;

			double sx = widthOrig / (double) gridWidth;
			double sy = heightOrig / (double) gridHeight;

			for (int y = 0; y < gridHeight; y++) {
				int yB = (int) (y * sy);

				for (int x = 0; x < gridWidth; x++) {
					int posGrid = y * gridWidth + x;

					int xB = (int) (x * sx);
					
					int posOrig = yB * widthOrig + xB;
					
					plotList[posGrid] = new SurfacePlotData();

					plotList[posGrid].color = pixelsOrigColor[posOrig];
					plotList[posGrid].x = sx*(x+0.5) - xOffset;
					plotList[posGrid].y = sy*(y+0.5) - yOffset;
					plotList[posGrid].z = plotList[posGrid].zf = plotList[posGrid].lum =  
						pixelsOrigLum[posOrig] - zCenter;
					
					if (maskPixels!= null) {
						if (maskPixels[posOrig] != 0) 
							plotList[posGrid].isVisible = true;
						else
							plotList[posGrid].isVisible = false;
					}
					else
						plotList[posGrid].isVisible = true;
				}
			}
		}
		
		if (pixelsTexColor != null) {
			
			double sx = widthTex / (double) gridWidth;
			double sy = heightTex / (double) gridHeight;

			for (int y = 0; y < gridHeight; y++) {
				int yB = (int) (y * sy);

				for (int x = 0; x < gridWidth; x++) {
					int pos = y * gridWidth + x;

					int xB = (int) (x * sx);
					
					plotList[pos].color = pixelsTexColor[yB * widthTex + xB];
				}
			}
		}

		computeNormals();
		//computeNormals2();
	}
	

	private void computeNormals() {

		for (int y = 0; y < gridHeight; y++) {
			for (int x = 0; x < gridWidth; x++) {
				int i = y * gridWidth + x;

				double dx1 = 0;
				double dy1 = 0;
				double dz1 = 0;

				for (int y_ = -1; y_ <= 1; y_++) {
					int yn = y + y_;
					if (yn < 0) yn = 0;
					if (yn >= gridHeight) yn = gridHeight-1;

					for (int x_ = -1; x_ < 1; x_++) {

						int xn = x + x_;
						if (xn < 0) xn = 0;
						if (xn >= gridWidth) xn = gridWidth-1;

						int xn1 = xn+1;
						if (xn1 < 0) xn1 = 0;
						if (xn1 >= gridWidth) xn1 = gridWidth-1;

						int posn = yn*gridWidth+xn;
						int posn1 = yn*gridWidth+xn1;

						dx1 += plotList[posn1].x - plotList[posn].x;
						dz1 += plotList[posn1].z - plotList[posn].z;
					}
				}

				double dx2 = 0;
				double dy2 = 0;
				double dz2 = 0;

				for (int y_ = -1; y_ < 1; y_++) {

					int yn = y + y_;
					if (yn < 0) yn = 0;
					if (yn >= gridHeight) yn = gridHeight-1;

					int yn1 = yn+1;
					if (yn1 < 0) yn1 = 0;
					if (yn1 >= gridHeight) yn1 = gridHeight-1;

					for (int x_ = -1; x_ <= 1; x_++) {
						int xn = x + x_;
						if (xn < 0) xn = 0;
						if (xn >= gridWidth) xn = gridWidth-1;

						int posn =  yn*gridWidth+xn;
						int posn1 = yn1*gridWidth+xn;

						dy2 += plotList[posn1].y - plotList[posn].y;
						dz2 += plotList[posn1].z - plotList[posn].z;
					}
				}

				// outer product

				double dx =  (dy1*dz2 - dz1*dy2);
				double dy =  (dz1*dx2 - dx1*dz2);
				double dz =  (dx1*dy2 - dy1*dx2);

				double len = Math.sqrt(dx*dx + dy*dy + dz*dz); 

				plotList[i].dx = dx/len; 
				plotList[i].dy = dy/len; 
				plotList[i].dz = dz/len; 					

			}
		}
	}

	
	protected void applyMinMax() {
		//System.out.println("applyMinMax inverseFactor: " + inversefactor);
		int add = 0;
		if (inversefactor == -1)
			add = -1;
		for (int i = 0; i < gridHeight*gridWidth; i++) {
			double val = (100.*(plotList[i].zf+zCenter - 2.55*(min))/(max-min) - zCenter);
			plotList[i].z = inversefactor*Math.min(Math.max(-128,val),127) + add;   
		}
		computeNormals();
	}
 	
	

	protected void applySmoothingFilter(double rad) {
		
		float[] pixels = new float[gridHeight*gridWidth];
		
		for (int i=0; i < gridHeight*gridWidth; i++) {
			pixels[i] = (float) plotList[i].lum;
		}
		ImageProcessor ip = new FloatProcessor(gridWidth, gridHeight, pixels, null);
		new GaussianBlur().blur(ip, rad);
		// reset progress bar (which was set by gaussian blur)
		IJ.showProgress( 1.);
		
//		ImagePlus imp2 = new ImagePlus("test", ip); 
//		imp2.show();
//		imp2.updateAndDraw();
		
		pixels = (float[] )ip.getPixels();
		
		for (int i=0; i < gridHeight*gridWidth; i++) {
			plotList[i].z = plotList[i].zf = pixels[i];
		}
		
		applyMinMax();
	}
	
	
	/**************************************************************************************
	 *  
	 *  Drawing Routines 
	 *  
	 **************************************************************************************/

	private int getColor(SurfacePlotData p0) {
		int c0;
		if (lutNr == JRenderer3D.LUT_ORIGINAL) {
			c0 = p0.color; 
		}
		else if (lutNr == JRenderer3D.LUT_GRADIENT) { 
			c0 = ((int) (p0.dx*127 + 127) << 16) | ((int) (p0.dy*127 + 127) << 8) | (int) (p0.dz*127 + 127);	
		}
		else if (lutNr == JRenderer3D.LUT_GRADIENT2) { 
			c0 = ((int) (p0.dx2*127 + 127) << 16) | ((int) (p0.dy2*127 + 127) << 8) | (int) (0);	
		}
		else {
			int index = (int) (p0.z + 128);
			if (index > 255)
				index = 255;
			if (index < 0)
				index = 0;
			c0 = lut.colors[index];
		}
		return c0;
	}

	private void surfacePlotFilled(){	
		for (int row = 0; row < gridHeight - 1; row++) {
			for (int col = 0; col < gridWidth - 1; col++) {
				int i = row * gridWidth + col;
	
				SurfacePlotData p0 = plotList[i];
	
				if (p0.isVisible) {
					SurfacePlotData p1 = plotList[i + 1];
					SurfacePlotData p2 = plotList[i + gridWidth];
					SurfacePlotData p3 = plotList[i + gridWidth + 1];
	
					if (p1.isVisible && p2.isVisible && p3.isVisible) {
//						System.out.println("-----------");
						
						tr.transform(p0);
						double x0 = tr.X, y0 = tr.Y, z0 = tr.Z;
						tr.x = p0.dx;
						tr.y = p0.dy;
						double light0 = tr.getScalarProduct();
						
						tr.transform(p1);
						double x1 = tr.X, y1 = tr.Y, z1 = tr.Z;
						tr.x = p1.dx;
						tr.y = p1.dy;
						double light1 = tr.getScalarProduct();
						
						tr.transform(p2);
						double x2 = tr.X, y2 = tr.Y, z2 = tr.Z;
						tr.x = p2.dx;
						tr.y = p2.dy;
						double light2 = tr.getScalarProduct();
						
						tr.transform(p3);
						double x3 = tr.X, y3 = tr.Y, z3 = tr.Z;
						tr.x = p3.dx;
						tr.y = p3.dy;
						double light3 = tr.getScalarProduct();
						
//						System.out.println("x0: " + x0);
//						System.out.println("x1: " + x1);
//						System.out.println("x2: " + x2);
//						System.out.println("x3: " + x3);
//						
//						System.out.println("y0: " + y0);
//						System.out.println("y1: " + y1);
//						System.out.println("y2: " + y2);
//						System.out.println("y3: " + y3);
						
						if(!(   x0 >= bufferWidth && x0 < 0 &&  y0 >= bufferHeight && y0 < 0 &&
								x1 >= bufferWidth && x1 < 0 &&  y1 >= bufferHeight && y1 < 0 &&
								x2 >= bufferWidth && x2 < 0 &&  y2 >= bufferHeight && y2 < 0 &&
								x3 >= bufferWidth && x3 < 0 &&  y3 >= bufferHeight && y3 < 0 ) ) {
							
							int c0 = getColor(p0);
							int c1 = getColor(p1);
							int c2 = getColor(p2);
							int c3 = getColor(p3);
							
							int r0 = ((c0 >> 16) & 0xff);
							int g0 = ((c0 >>  8) & 0xff);
							int b0 = ((c0      ) & 0xff);
							int r1 = ((c1 >> 16) & 0xff);
							int g1 = ((c1 >>  8) & 0xff);
							int b1 = ((c1      ) & 0xff);
							int r2 = ((c2 >> 16) & 0xff);
							int g2 = ((c2 >>  8) & 0xff);
							int b2 = ((c2      ) & 0xff);
							int r3 = ((c3 >> 16) & 0xff);
							int g3 = ((c3 >>  8) & 0xff);
							int b3 = ((c3      ) & 0xff);
							
							double n13 = Math.abs(y1-y3) + Math.abs(x1-x3); 
							double n02 = Math.abs(y0-y2) + Math.abs(x0-x2); 
//							double n13 = Math.sqrt((y1-y3)*(y1-y3) + (x1-x3)*(x1-x3)); 
//							double n02 = Math.sqrt((y0-y2)*(y0-y2) + (x0-x2)*(x0-x2)); 
							int stepsY = (int) (Math.max(n13, n02) + 1); 
							
							double dy = 1./stepsY;
				
							double dx02 = (x2-x0)*dy;
							double dy02 = (y2-y0)*dy;
							double dx13 = (x3-x1)*dy;
							double dy13 = (y3-y1)*dy;
	
							double x02 = x0;
							double y02 = y0;
							double x13 = x1;
							double y13 = y1;
							
							double v = 0;
							
							for(int sy=0; sy<stepsY; sy++, v+= dy) {
	
								x02 += dx02;
								y02 += dy02;
								x13 += dx13;
								y13 += dy13;
								
								//int stepsX = (int) (Math.abs(x02-x13) + Math.abs(y02-y13) + 1);
								int stepsX = (int) (Math.abs(x02-x13) + Math.abs(y02-y13) + 1);
								
								double dx = 1./stepsX;
	
								double dx0213 = (x13-x02)*dx;
								double dy0213 = (y13-y02)*dx;
	
								double x0213 = x02;
								double y0213 = y02;
								
								double h = 0;
								
								for(int sx=0; sx<stepsX; sx++, h+=dx) {
									
									x0213 += dx0213;
									y0213 += dy0213;
									
									if (x0213 >= 0 && x0213 < bufferWidth && y0213 >= 0 && y0213 < bufferHeight) {
										double d0 = (1 - h) * (1 - v);
										double d1 = h * (1 - v);
										double d2 = (1 - h) * v;
										double d3 = h * v;
										
										double z = d0 * z0 + d1 * z1 + d2 * z2 + d3 * z3;
										
//										System.out.println("x: " + (int)x0213 + "  y: " + (int)y0213);
										
										int pos = (int)y0213 * bufferWidth + (int)x0213;
										if (z < zbufferPixels[pos]) {
											zbufferPixels[pos] = z;
											int r = (int) (r3*d3 + r2*d2 + r1*d1 + r0*d0);
											int g = (int) (g3*d3 + g2*d2 + g1*d1 + g0*d0);
											int b = (int) (b3*d3 + b2*d2 + b1*d1 + b0*d0);
	
											double light0123 = d3*light3 + d2*light2 + d1*light1 + d0*light0;
	
											double l = -light * light0123 *255;
	
											r = (int) Math.min(255, Math.max(0, r + l));
											g = (int) Math.min(255, Math.max(0, g + l));
											b = (int) Math.min(255, Math.max(0, b + l));
	
											bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void surfacePlotIsoLines(){	
		for (int row = 0; row < gridHeight - 1; row++) {
			for (int col = 0; col < gridWidth - 1; col++) {
				int i = row * gridWidth + col;

				SurfacePlotData p0 = plotList[i];

				if (p0.isVisible) {
					SurfacePlotData p1 = plotList[i + 1];
					SurfacePlotData p2 = plotList[i + gridWidth];
					SurfacePlotData p3 = plotList[i + gridWidth + 1];

					if ((p1.isVisible) && (p2.isVisible) && (p3.isVisible)) {
						
						tr.transform(p0);
						double x0 = tr.X, y0 = tr.Y, z0 = tr.Z;
						tr.x = p0.dx;
						tr.y = p0.dy;
						double light0 = tr.getScalarProduct();
						
						tr.transform(p1);
						double x1 = tr.X, y1 = tr.Y, z1 = tr.Z;
						tr.x = p1.dx;
						tr.y = p1.dy;
						double light1 = tr.getScalarProduct();
						
						tr.transform(p2);
						double x2 = tr.X, y2 = tr.Y, z2 = tr.Z;
						tr.x = p2.dx;
						tr.y = p2.dy;
						double light2 = tr.getScalarProduct();
						
						tr.transform(p3);
						double x3 = tr.X, y3 = tr.Y, z3 = tr.Z;
						tr.x = p3.dx;
						tr.y = p3.dy;
						double light3 = tr.getScalarProduct();
						
						if(!(   x0 >= bufferWidth && x0 < 0 &&  y0 >= bufferHeight && y0 < 0 &&
								x1 >= bufferWidth && x1 < 0 &&  y1 >= bufferHeight && y1 < 0 &&
								x2 >= bufferWidth && x2 < 0 &&  y2 >= bufferHeight && y2 < 0 &&
								x3 >= bufferWidth && x3 < 0 &&  y3 >= bufferHeight && y3 < 0 ) ) {
							
							int c0 = getColor(p0);
							int c1 = getColor(p1);
							int c2 = getColor(p2);
							int c3 = getColor(p3);
							
							double lum0 = p0.z;
							double lum1 = p1.z;
							double lum2 = p2.z;
							double lum3 = p3.z;
							
							int r0 = ((c0 >> 16) & 0xff);
							int g0 = ((c0 >>  8) & 0xff);
							int b0 = ((c0      ) & 0xff);
							int r1 = ((c1 >> 16) & 0xff);
							int g1 = ((c1 >>  8) & 0xff);
							int b1 = ((c1      ) & 0xff);
							int r2 = ((c2 >> 16) & 0xff);
							int g2 = ((c2 >>  8) & 0xff);
							int b2 = ((c2      ) & 0xff);
							int r3 = ((c3 >> 16) & 0xff);
							int g3 = ((c3 >>  8) & 0xff);
							int b3 = ((c3      ) & 0xff);
							
							double n13 = Math.abs(x1-x3) + Math.abs(y1-y3);
							double n02 = Math.abs(x0-x2) + Math.abs(y0-y2);
							int stepsY = (int) (Math.max(n13, n02) + 1);
							
							double dy = 1./stepsY;
							
							double dx02 = (x2-x0)*dy;
							double dy02 = (y2-y0)*dy;
							double dx13 = (x3-x1)*dy;
							double dy13 = (y3-y1)*dy;

							double x02 = x0;
							double y02 = y0;

							double x13 = x1;
							double y13 = y1;
							
							double v = 0;
							
							for(int sy=0; sy<stepsY; sy++, v+= dy) {

								x02 += dx02;
								y02 += dy02;

								x13 += dx13;
								y13 += dy13;
								
								int stepsX = (int) (Math.abs(x02-x13) + Math.abs(y02-y13) + 1);
								
								double dx = 1./stepsX;

								double dx0213 = (x13-x02)*dx;
								double dy0213 = (y13-y02)*dx;

								double x0213 = x02;
								double y0213 = y02;
								
								double h = 0;
								
								for(int sx=0; sx<stepsX; sx++, h+=dx) {
									
									x0213 += dx0213;
									y0213 += dy0213;
									
									double d0 = (1 - h) * (1 - v);
									double d1 = h * (1 - v);
									double d2 = (1 - h) * v;
									double d3 = h * v;

									double z = d0 * z0 + d1 * z1 + d2 * z2 + d3 * z3;
									
									if (x0213 >= 0 && x0213 < bufferWidth && y0213 >= 0 && y0213 < bufferHeight) {
										int pos = (int)y0213 * bufferWidth + (int)x0213;
										if (z < zbufferPixels[pos]) {
											double lum = d0 * lum0 + d1 * lum1 + d2 * lum2 + d3 * lum3 + 132;

											if (lum - 12*(int)(lum/12) < 1.5) {

												zbufferPixels[pos] = z;
												int r = (int) (r3*d3 + r2*d2 + r1*d1 + r0*d0);
												int g = (int) (g3*d3 + g2*d2 + g1*d1 + g0*d0);
												int b = (int) (b3*d3 + b2*d2 + b1*d1 + b0*d0);

												double light0123 = d3*light3 + d2*light2 + d1*light1 + d0*light0;

												double l = -light * light0123 *255;

												r = (int) Math.min(255, Math.max(0, r + l));
												g = (int) Math.min(255, Math.max(0, g + l));
												b = (int) Math.min(255, Math.max(0, b + l));

												bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void surfacePlotMesh(){
		
		for (int row=0; row<gridHeight; row++){ 
			for (int col=0; col<gridWidth; col++){ 
				int i = row*gridWidth + col;
				
				SurfacePlotData p0 = plotList[i];
				int r0, g0, b0, r1, g1, b1, r2, g2, b2; 
				
				if (p0.isVisible) {
					tr.transform(p0);
					double x0 = tr.X, y0 = tr.Y, z0 = tr.Z;
					
					int c0 = getColor(p0);
					
					r0 = ((c0 >> 16) & 0xff);
					g0 = ((c0 >>  8) & 0xff);
					b0 = ((c0      ) & 0xff);
					
					SurfacePlotData p1 = (col<gridWidth-1) ? plotList[i+1] : plotList[i];
					
					if ( p1.isVisible ) {
						tr.transform(p1);
						double x1 = tr.X,   y1 = tr.Y,   z1 = tr.Z;
						double dx10 = x1-x0, dy10 = y1-y0, dz10 = z1-z0;
						
						int c1 = getColor(p1);
						
						r1 = ((c1 >> 16) & 0xff);
						g1 = ((c1 >>  8) & 0xff);
						b1 = ((c1      ) & 0xff);
						

						int numSteps = (int) (Math.max(Math.abs(dx10),Math.abs(dy10)) + 1);
						
						double step = 1. / numSteps;
						
						for (int s = 0; s < numSteps; s++) {
							double f = s * step;
							
							int x = (int) (x0 + f*dx10);
							int y = (int) (y0 + f*dy10);
							
							if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
								int pos = y*bufferWidth + x;  
								int z = (int) (z0 + f*dz10);
								if (z < zbufferPixels[pos]) {
									zbufferPixels[pos] = z;
									
									int r = (int) (f*r1 + (1-f)*r0);
									int g = (int) (f*g1 + (1-f)*g0);
									int b = (int) (f*b1 + (1-f)*b0);
												
									tr.x   =  p0.dx; 
									tr.y   =  p0.dy; 

									double l = -light * tr.getScalarProduct()  *255;

									r = (int) Math.min(255, Math.max(0, r + l));
									g = (int) Math.min(255, Math.max(0, g + l));
									b = (int) Math.min(255, Math.max(0, b + l));								

									bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b; 
								}
							}
						}
					}
					
					SurfacePlotData p2 = (row<gridHeight-1) ? plotList[i+gridWidth] : plotList[i];
					
					if ( p2.isVisible ) {
						tr.transform(p2);
						double x2 = tr.X,   y2 = tr.Y,   z2 = tr.Z;
						double dx20 = x2-x0, dy20 = y2-y0, dz20 = z2-z0;
						
						int c2 = getColor(p2);
						
						r2 = ((c2 >> 16) & 0xff);
						g2 = ((c2 >>  8) & 0xff);
						b2 = ((c2      ) & 0xff);

						
						int numSteps = (int) (Math.max(Math.abs(dx20),Math.abs(dy20)) + 1);
						
						double step = 1. / numSteps;
						
						for (int s = 0; s < numSteps; s++) {
							double f = s * step;
							
							int x = (int) (x0 + f*dx20);
							int y = (int) (y0 + f*dy20);
							
							if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
								int pos = y*bufferWidth + x;  
								int z = (int) (z0 + f*dz20);
								if (z < zbufferPixels[pos]) {
									zbufferPixels[pos] = z;
									
									int r = (int) (f*r2 + (1-f)*r0);
									int g = (int) (f*g2 + (1-f)*g0);
									int b = (int) (f*b2 + (1-f)*b0);

									tr.x   =  p0.dx; 
									tr.y   =  p0.dy; 

									double l = -light * tr.getScalarProduct()  *255;

									r = (int) Math.min(255, Math.max(0, r + l));
									g = (int) Math.min(255, Math.max(0, g + l));
									b = (int) Math.min(255, Math.max(0, b + l));	
									
									bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b; 
								}
							}
						}
					}
				}	
			}
		}			
	}
	
	
	private void surfacePlotLines(){
		for (int row=0; row<gridHeight; row++){
			for (int col=0; col<gridWidth-1; col++){
				int i = row*gridWidth + col;
				SurfacePlotData p0 = plotList[i]; 
				SurfacePlotData p1 = plotList[i+1]; 
				
				if (p0.isVisible &&  p1.isVisible) {
					
					tr.transform(p0);
					double x0 = tr.X, y0 = tr.Y, z0 = tr.Z;
					
					int c0 = getColor(p0);
					
					int r0 = ((c0 >> 16) & 0xff);
					int g0 = ((c0 >>  8) & 0xff);
					int b0 = ((c0      ) & 0xff);


					tr.transform(p1);
					double x1 = tr.X,   y1 = tr.Y,   z1 = tr.Z;
					double dx1 = x1-x0, dy1 = y1-y0, dz1 = z1-z0;

					int numSteps = (int) (Math.max(Math.abs(dx1),Math.abs(dy1))+1);

					int c1 = getColor(p1);

					int r1 = ((c1 >> 16) & 0xff);
					int g1 = ((c1 >>  8) & 0xff);
					int b1 = ((c1      ) & 0xff);

					double step = 1. / numSteps;
					int r, g, b;
					for (int s = 0; s < numSteps; s++) {
						double f = s * step;

						int x = (int) (x0 + f*dx1);
						int y = (int) (y0 + f*dy1);

						if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
							int pos = y*bufferWidth + x;  
							double z = z0 + f*dz1;
							if (z < zbufferPixels[pos]) {
								zbufferPixels[pos] = z;

								r = (int) ((1-f)*r0 + f*r1);
								g = (int) ((1-f)*g0 + f*g1);
								b = (int) ((1-f)*b0 + f*b1);

								tr.x   =  p0.dx; 
								tr.y   =  p0.dy; 

								double l = -light * tr.getScalarProduct()  *255;

								r = (int) Math.min(255, Math.max(0, r + l));
								g = (int) Math.min(255, Math.max(0, g + l));
								b = (int) Math.min(255, Math.max(0, b + l));							

								bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b; 
							}
						}
					}
				}
			}
		}
	}
	
	private void surfacePlotDots(){
		
		for (int i=plotList.length-1; i>=0; i--){

			SurfacePlotData p0 = plotList[i]; 
			if (p0.isVisible) {

				tr.transform(p0);
				int x = (int) tr.X, y = (int) tr.Y;

				if (x >= 0 && y >= 0 && x < bufferWidth && y < bufferHeight) { 
					int pos = y*bufferWidth + x;  
					int z = (int) tr.Z;
					if (z < zbufferPixels[pos]) {
						zbufferPixels[pos] = z;		

						int c0 = getColor(p0);
						
						int r0 = ((c0 >> 16) & 0xff);
						int g0 = ((c0 >>  8) & 0xff);
						int b0 = ((c0      ) & 0xff);
						
						tr.x   =  p0.dx; 
						tr.y   =  p0.dy; 

						double l = -light * tr.getScalarProduct()  *255;

						int r = (int) Math.min(255, Math.max(0, r0 + l));
						int g = (int) Math.min(255, Math.max(0, g0 + l));
						int b = (int) Math.min(255, Math.max(0, b0 + l));
						
						bufferPixels[pos] = 0xff000000 | (r << 16) | (g << 8) | b;

					}
				}	
			}	
		}
	}		

	
	
	private void surfacePlotDotsNoLight(){
		
		int delta = Math.max(gridHeight, gridWidth) / 128;
		if (delta < 1)
			delta = 1;
		
		for (int row=0; row<gridHeight; row+= delta){
			for (int col=0; col<gridWidth; col+= delta){
				int i = row*gridWidth + col;
				SurfacePlotData p0 = plotList[i]; 
				if (p0.isVisible) {

					tr.transform(p0);

					int x = (int) tr.X;
					int y = (int) tr.Y;

					if (x >= 0 && y >= 0 && x < bufferWidth-1 && y < bufferHeight-1) { 
						int pos = y*bufferWidth + x;  
						int z = (int) tr.Z;

						if (z < zbufferPixels[pos]) {
							int c0 = 0xFF000000 | getColor(p0);

							zbufferPixels[pos] = z;
							zbufferPixels[pos+1] = z;
							zbufferPixels[pos+bufferWidth] = z;
							zbufferPixels[pos+bufferWidth+1] = z;
							bufferPixels[pos] = c0; 
							bufferPixels[pos+1] = c0; 
							bufferPixels[pos+bufferWidth] = c0; 
							bufferPixels[pos+bufferWidth+1] = c0; 
						}
					}
				}
			}
		}
	}

	protected void setSurfacePLotSetLight(double light) {
		this.light = light;
	}

	protected void setSurfaceGridSize(int width, int height) {
		this.gridWidth = width; 
		this.gridHeight = height; 	
	}

	protected void setSurfacePlotMode(int surfacePlotMode) {
		this.surfacePlotMode = surfacePlotMode;
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

	protected void setSurfacePlotCenter(double xCenter, double yCenter, double zCenter) {
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.zCenter = zCenter;
	}

	protected void setSurfacePlotLut(int lutNr) {
		this.lutNr = lutNr;
		if (lut != null)
			lut.setLut(lutNr);	
	}
	
	public int getSurfacePlotLut() {
		return lutNr;
	}

	protected void setMinMax(int min, int max) {
		this.min = min;
		this.max = max;
	}

	protected void setInverse(boolean b) {
		inversefactor = (b) ? -1 : 1; 
			
		for (int i = 0; i < plotList.length; i++)
			plotList[i].z = inversefactor*plotList[i].zf;
	}

	public int getInversefactor() {
		return inversefactor;
	}
}

