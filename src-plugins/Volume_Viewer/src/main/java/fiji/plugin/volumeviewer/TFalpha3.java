/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class TFalpha3 extends JPanel implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID = -1;
	private int height=128, width = 256;
	private int[] pixels;
	private int sampleMean = -1, sampleDiff = -1;
	private Control control;
	private Volume vol;
	private int[][] lut;
	
	private final int[][] alpha3back = new int[256][128];
	private final int[][] alpha3 = new int[256][128];
	final int[][] a3 = new int[256][128];
	
	private int scaleAlpha = 0;
	private int[][][] lut2D_3; 
	

	public TFalpha3(Control control, Volume vol, int[][] lut, int[][][] lut2D_3) {
		super();
		
		this.control = control;
		this.vol = vol;
		this.lut = lut;
		this.lut2D_3 = lut2D_3;
		
		for (int i = 0; i < alpha3.length; i++) {
			for (int j = 0; j < alpha3[0].length; j++) {
				alpha3[i][j] = - 1000;
			}	
		}
		
		setPreferredSize(new Dimension(256, height));
		addMouseListener(this);
		addMouseMotionListener(this);
		pixels  = new int[width*height];
		setAlphaAuto();
	}

	public void setValues(int[] vals) {
		sampleMean  = vals[2];	
		sampleDiff = vals[3];	
	}

	public void clearAlpha() {
		for (int y = 0; y < height; y++) { 
			for (int x = 0; x < 256; x++) {
				alpha3[x][y] = -1000;
				a3[x][y] = 0;
			}
		}
		control.alphaWasChanged = true;
	}

	public void scaleAlpha() {
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 128; y++) {

				int alpha = alpha3[x][y];
				if (alpha != -1000)
					alpha += scaleAlpha;
				else 
					alpha = 0;
				if (alpha > 255) alpha = 255;
				else if (alpha < 0)   alpha = 0;
				a3[x][y] = alpha;
			}
		}
		control.alphaWasChanged = true;
	}

	void updateLutGradVal() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				lut2D_3[x][y][0] = lut[x][0];
				lut2D_3[x][y][1] = lut[x][1];
				lut2D_3[x][y][2] = lut[x][2];
			}
		}
	}

	public void setAlphaAuto() {

		// find 2D maximum
		float max1 = 0; 
		for (int y = 0; y < height; y++)  // find maximum
			for (int x = 0; x < 256; x++) {  
				int val = vol.histMeanDiff[x][y];
				if (val > max1) max1 = val;	
			}

		//  2D for LH histogram image
		double norm = 1./max1; 
		for (int y = 0; y < height; y++) { // norm and scale to 0 .. 255
			for (int x = 0; x < 256; x++) {
				int val = (int) (255*2*Math.pow(vol.histMeanDiff[x][y]*norm, 0.22)); // 0.22
				if (y > 5)
					alpha3[x][y] = 128 - val;
				else 
					alpha3[x][y] = -1000;
				if (val > 255)
					val = 255;
				alpha3back[x][y] = 255-val;  
			}
		}

		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 128; y++) {
				if (alpha3[x][y] != -1000)
					a3[x][y] = (int) Math.min(Math.max(0,alpha3[x][y]+scaleAlpha), 255);
				else
					a3[x][y] = 0;
//				//TODO
//				if (y < 0) {
//					alpha3[x][y] = a3[x][y] = 0;
//				}
				lut2D_3[x][y][0] = lut[x][0];
				lut2D_3[x][y][1] = lut[x][1];
				lut2D_3[x][y][2] = lut[x][2];
			}
		}
		control.alphaWasChanged = true;
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		handleMouseDragged(e);
		control.drag = true;
	}
	public void mouseReleased(MouseEvent e) {
		control.drag = false;
		control.newDisplayMode();
	}

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
		int v = control.alphaPaint3;
		if (e.isAltDown())
			v = 0;

		for (int y_ = Math.max(0, y-6); y_ <= Math.min(y+6,height-1); y_++) {
			for (int x_ = Math.max(0, x-6); x_ <= Math.min(x+6,255); x_++) {
				a3[x_][y_] = v; 
				if (v > 0) {
					alpha3[x_][y_] = v - scaleAlpha;
				}
				else 
					alpha3[x_][y_] = -1000;

				if (control.pickColor) {
					lut2D_3[x_][y_][0] = control.rPaint;
					lut2D_3[x_][y_][1] = control.gPaint;
					lut2D_3[x_][y_][2] = control.bPaint;
				}
				else {
					lut2D_3[x_][y_][0] = lut[x_][0];
					lut2D_3[x_][y_][1] = lut[x_][1];
					lut2D_3[x_][y_][2] = lut[x_][2];
				}
			}
		}
		control.alphaWasChanged = true;
		repaint();
	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		int width=256;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pos = (height-1-y)*width+x;
				int valBack = alpha3back[x][y];
				int valA = (int) (Math.min(255, a3[x][y]*1.6));
				int val_A = 255-valA;

				int valTR = lut2D_3[x][y][0];
				int valTG = lut2D_3[x][y][1];
				int valTB = lut2D_3[x][y][2];

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

		if (sampleMean >= 0 && sampleDiff >= 0) {

			for (int y = 0; y < height; y++) {
				int pos = y*width+sampleMean;
				pixels[pos] = 0xff1ea2ff;
			}
			for (int x = 0; x < width; x++) {
				int pos = (height-1-sampleDiff)*width+x;
				pixels[pos] = 0xff1ea2ff;
			}
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		g2.drawImage(image, 0, 0, null);
	}

	public void setAlphaOffset(int scaleAlpha3) {
		this.scaleAlpha = scaleAlpha3;
	}
}
