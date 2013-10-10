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

public class TFalpha1 extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private int height=128, width = 256;
	int[] pixels;
	private int sampleValue = -1;
	
	private Control control;
	private Volume vol;

	private final float[] alpha1 = new float[256];
	final int[] a1 = new int[256];
	
	private int alphaOffset = 0;

	public int getScaleAlpha1() {
		return alphaOffset;
	}

	public void setAlphaOffset(int alphaOffset) {
		this.alphaOffset = alphaOffset;
	}

	public void setValues(int[] vals) {
		sampleValue  = vals[0];	
	}

	public TFalpha1(Control control, Volume vol) {
		super();
		this.control = control;
		this.vol = vol;
		pixels  = new int[width*height];
		setPreferredSize(new Dimension(256, height));
		addMouseListener(this);
		addMouseMotionListener(this);
		setAlphaAuto();
	}

	public void scaleAlpha() {
		for (int x = 0; x < 256; x++) {
			int alpha = (int) alpha1[x];
			if (alpha > 0)
				alpha += alphaOffset;
			if (alpha > 255) alpha = 255;
			if (alpha < 0)   alpha = 0;
			a1[x] = alpha;
		}
		control.alphaWasChanged = true;
	}

	public void setAlphaAuto() {

		float max1 = 0; //, max2 = 0; 
		for (int i = 0; i < vol.histVal.length; i++) { // find the max / modal value
			int val = vol.histVal[i];
			if (val > max1) 
				max1 = val;
		}

		float sum = 0;
		for (int x = 0; x < 256; x++) { // norm the histogram, store in alpha1D
			float val = (float) (1 - 1.2*Math.pow(vol.histVal[x]/max1, 0.3));

			if (val > 0)
				sum += val;
			alpha1[x] = val;
		}
		sum /= 256;	// mean hist height
		float[] alpha1Dauto = new float[256];

		for (int x = 0; x < 256; x++) { // lowpass filter for the histogram
			int xm2 = x>1 ? x-2 : 0;
			int xm1 = x>0 ? x-1 : 0;
			int xp1 = x<255 ? x+1 : 255;	
			int xp2 = x<254 ? x+2 : 255;	
			float val = (alpha1[xm2] + alpha1[xm1] + alpha1[x] + alpha1[xp1] + alpha1[xp2]) * 0.2f;
			val += 0.5f - sum;
			alpha1Dauto[x] = 255*val; // scale alpha1auto by 255, values may be > 255 or < 0  
		}

		for (int x = 0; x < 256; x++) {
			alpha1[x] = alpha1Dauto[x]; // copy alpha1Dauto to alpha1
			a1[x] = (int) Math.min(Math.max(0,alpha1[x]), 255);
		}
		control.alphaWasChanged = true;
	}

	public void clearAlpha() {

		for (int x = 0; x < 256; x++) {
			alpha1[x] = 0; 
			a1[x] = 0;
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
		if(x > 255) 
			x = 255;
		if(y > height) 
			y = height-1;
		int sx = xLast, ex = x, sy = yLast, ey = y;
		if(ex < sx) {
			sx = x;
			ex = xLast;
			sy = y;
			ey = yLast;
		}
		if (e.isAltDown())
			sy = ey = height-1;
		int lx = ex - sx;
		int ly = ey - sy;
		for(int i = sx; i <= ex; i++) {
			if(lx == 0) lx = 1;
			float r = (float)(i - sx) / lx;
			float yi = (sy + r * ly);
			float v = 255*(height-1 - yi)/(height-1);
			if (v < 0)
				v = 0;
			if (v > 255)
				v = 255;
			a1[i] = (int) v; 
			if (v > 0)
				alpha1[i] = v - alphaOffset;
			else
				alpha1[i] = 0;
		}
		yLast = y;
		xLast = x;
		control.alphaWasChanged = true;
		repaint();
	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		double max1 = 0, max2 = 0; 
		int mode = 0;
		for (int i = 0; i < vol.histVal.length; i++) {
			int val = vol.histVal[i];
			if (val > max1) {
				max1 = val;
				mode = i;
			}
		}
		for (int i = 0; i < vol.histVal.length; i++) {
			int val = vol.histVal[i];
			if (i != mode && val > max2)
				max2 = val;
		}

		if ((max1>(max2 * 2)) && (max2 != 0)) 
			max1 = (int)(max2 * 1.5);

		double norm = 1f/max1;

		double maxL = Math.log(max1);

		for (int x = 0; x < width; x++) {
			int val = (int) ((height-1)*vol.histVal[x]*norm);
			double valL = vol.histVal[x]==0?0:(int)(height*Math.log(vol.histVal[x])/maxL);
			if (valL>height-1) 
				valL = height-1;
			for (int y = 0; y < height; y++) {
				int pos = y*width+x;

				if (height-y < val)
					pixels[pos] = 0xff000000;	
				else if (height-y < valL)
					pixels[pos] = 0xff777777;
				else
					pixels[pos] = 0xffFFFFFF;
			}
		}

		int y, y1, min, max;
		for(int x = 0; x < width-1; x++) {
			y  = (height-1)*a1[x]/256;
			y1 = (height-1)*a1[x+1]/256;
			min = Math.min(y1, y);
			max = Math.max(y1, y);
			for (int i = min; i <= max; i++)		
				pixels[(height-1-i)*256+x] = 0xFFFF7700;
		}

		if (sampleValue >= 0) {
			for (y = 0; y < height; y++) {
				int pos = y*width+sampleValue;
				pixels[pos] = 0xff1ea2ff;
			}
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		g2.drawImage(image, 0, 0, null);	
	}
}
