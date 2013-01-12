/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class TFalpha2 extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	
	private int height = 128, width = 256;
	
	private final int[][] alpha2back = new int[256][128];
	private final float[][] alpha2 = new float[256][128];
	final int[][] a2 = new int[256][128];
	
	private int scaleAlpha = 0; 

	private int[] pixels;
	private int sampleValue = -1, gradValue = -1;

	private Control control;

	private Volume vol;

	private int[][][] lut2D_2;

	private int[][] lut;
	
	public void setAlphaOffset(int scaleAlpha2) {
		this.scaleAlpha = scaleAlpha2;
	}

	public TFalpha2(Control control, Volume vol, int[][] lut, int[][][] lut2D_2) {
		super();
		this.control = control;
		this.vol = vol;
		this.lut = lut;
		this.lut2D_2 = lut2D_2;
		
		for (int i = 0; i < alpha2.length; i++) {
			for (int j = 0; j < alpha2[0].length; j++) {
				alpha2[i][j] = - 1000;
			}	
		}
		
		setPreferredSize(new Dimension(256, height));
		addMouseListener(this);
		addMouseMotionListener(this);
		pixels  = new int[width*height];
		setAlphaAuto();
	}

	public void setValues(int[] vals) {
		sampleValue  = vals[0];	
		gradValue  = vals[1];	
	}

	public void clearAlpha() {
		for (int y = 0; y < height; y++) { 
			for (int x = 0; x < 256; x++) {
				alpha2[x][y] = -1000; 
				a2[x][y] = 0;	
			}
		}
		control.alphaWasChanged = true;
	}

	public void scaleAlpha() {
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 128; y++) {

				int alpha = (int)alpha2[x][y];
				if (alpha != -1000)
					alpha += scaleAlpha;
				else 
					alpha = 0;
				if (alpha > 255) alpha = 255;
				else if (alpha < 0) alpha = 0;
				a2[x][y] = alpha;
			}
		}
		control.alphaWasChanged = true;
	}

	public void setAlphaAuto() {

		// 2D find maximum
		float max1 = 0; 
		for (int y = 0; y < vol.histValGrad[0].length; y++)  
			for (int x = 0; x < vol.histValGrad.length; x++) {  
				int val = vol.histValGrad[x][y];
				if (val > max1) max1 = val;
			}

		// for 2D histogram image
		for (int y = 0; y < height; y++)  // norm and scale to 0 .. 255
			for (int x = 0; x < 256; x++) {
				int val = (int) (255*2*Math.pow(vol.histValGrad[x][y]/max1, 0.22));
				if (val > 255)
					val = 255;
				alpha2back[x][y] = 255-val;  // 0 = max						
			}

		float[] alpha1D = new float[256];
		float sum = 0;
		for (int x = 0; x < 256; x++) { // norm the histogram, store in alpha1D
			float val = (float) (1 - Math.pow(vol.histVal[x]/max1, 0.1)); // 0.3
			sum += val;
			alpha1D[x] = val;
		}
		sum /= 256;	// mean hist height

		float[] alpha1Dauto = new float[256];
		for (int x = 0; x < 256; x++) { // lowpass filter for the 1D histogram
			int xm2 = x>1 ? x-2 : 0;
			int xm1 = x>0 ? x-1 : 0;
			int xp1 = x<255 ? x+1 : 255;	
			int xp2 = x<254 ? x+2 : 255;	
			float val = (alpha1D[xm2] + alpha1D[xm1] + alpha1D[x] + alpha1D[xp1] + alpha1D[xp2]) * 0.2f;
			val += 0.5f - sum;
			alpha1Dauto[x] = Math.max(0, val);
		}
		// in alpha1Dauto stehen die 1D alpha-Werte  

		// 2D
		for (int y = 0; y < vol.histValGrad[0].length; y++)  // find maximum
			for (int x = 0; x < vol.histValGrad.length; x++) {  
				int val = vol.histValGrad[x][y];
				if (val > max1) max1 = val;
			}

		float[][] alpha2Dauto = new float[256][128];
		for (int x = 0; x < 256; x++) 
			for (int y = 0; y < 128; y++) { 
				float val = (float) (1 - 3*Math.pow(vol.histValGrad[x][y]/max1,0.22)); // 2
				sum += val;
				alpha2[x][y] = val;
			}
		sum /= (256*128);	// mean hist height

		// Filter 
		for (int y = 0; y < 128; y++) {
			int ym2 = y>1 ? y-2 : 0;
			int ym1 = y>0 ? y-1 : 0;
			int yp1 = y<height-1 ? y+1 : height-1;	
			int yp2 = y<height-2 ? y+2 : height-1;	
			for (int x = 0; x < 256; x++) {
				float val = (alpha2[x][ym2] + alpha2[x][ym1] + alpha2[x][y] + alpha2[x][yp1] + alpha2[x][yp2]) * 0.2f;
				alpha2Dauto[x][y] = val;
			}
		}

		for (int x = 0; x < 256; x++) { 
			for (int y = 0; y < 128; y++) {
				float val = alpha2Dauto[x][y] * alpha1Dauto[x];
				val = 255*(val + 0.8f - sum);
				alpha2[x][y] = val; // scale alpha1auto by 255, values may be > 255 or < 0  
				a2[x][y] = (int) Math.min(Math.max(0,val), 255);
				lut2D_2[x][y][0] = lut[x][0];
				lut2D_2[x][y][1] = lut[x][1];
				lut2D_2[x][y][2] = lut[x][2];
			}
		}
		control.alphaWasChanged = true;
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		xLast = e.getX();
		yLast = e.getY();
		control.drag = true;
		handleMouseDragged(e);
	}
	public void mouseReleased(MouseEvent e) {
		control.drag = false;
		control.newDisplayMode();
	}

	int xLast = -1, yLast = -1;
	int count = 0;
	public void mouseDragged(MouseEvent e) {
		handleMouseDragged(e);
		if ((count++ % 10) == 0)
			control.newDisplayMode();
	}

	public void handleMouseDragged(MouseEvent e) {

		int x = e.getX(), y = e.getY();
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		if(x > 255) x = 255;
		if(y > height) y = height-1;
		y = height-1-y;

		int v = control.alphaPaint2;
		if (e.isAltDown())
			v = 0;

		for (int y_ = Math.max(0, y-6); y_ <= Math.min(y+6,height-1); y_++) {
			for (int x_ = Math.max(0, x-6); x_ <= Math.min(x+6,255); x_++) {
				a2[x_][y_] = (int) v; 
				if (v > 0)
					alpha2[x_][y_] = v - scaleAlpha;
				else 
					alpha2[x_][y_] = -1000;

				if (control.pickColor) {
					lut2D_2[x_][y_][0] = control.rPaint;
					lut2D_2[x_][y_][1] = control.gPaint;
					lut2D_2[x_][y_][2] = control.bPaint;
				}
				else {
					lut2D_2[x_][y_][0] = lut[x_][0];
					lut2D_2[x_][y_][1] = lut[x_][1];
					lut2D_2[x_][y_][2] = lut[x_][2];
				}
			}
		}
		control.alphaWasChanged = true;
		repaint();
	}

	void updateLutGradVal() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				lut2D_2[x][y][0] = lut[x][0];
				lut2D_2[x][y][1] = lut[x][1];
				lut2D_2[x][y][2] = lut[x][2];
			}
		}
	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		int width=256;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pos = (height-1-y)*width+x;
				int valBack = alpha2back[x][y];
				int valA = (int) (Math.min(255, a2[x][y]*1.6));
				int val_A = 255-valA;

				int valTR = lut2D_2[x][y][0];
				int valTG = lut2D_2[x][y][1];
				int valTB = lut2D_2[x][y][2];

				if (control.lutNr == Control.GRAY || control.lutNr == Control.ORIG) {
					valTR = 255;
					valTG = 128;
					valTB = 0;
				}
				int valR = valBack, valG = valBack, valB = valBack;
				if (valBack < 255) {
					valR = (val_A*valBack + valA*valTR)>>8;
				valG = (val_A*valBack + valA*valTG)>>8;
		valB = (val_A*valBack + valA*valTB)>>8;
				}
				pixels[pos] = 0xff000000 | valR << 16 | valG << 8 | valB;
			}
		}

		if (sampleValue >= 0) {
			for (int y = 0; y < height; y++) {
				int pos = y*width+sampleValue;
				pixels[pos] = 0xff1ea2ff;
			}
		}
		if (gradValue >= 0) {
			for (int x = 0; x < width; x++) {
				int pos = (height-1-gradValue)*width+x;
				pixels[pos] = 0xff1ea2ff;
			}
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		g2.drawImage(image, 0, 0, null);
	}
}