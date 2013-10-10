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


public class TFrgb extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	private int height = 128;
	int channel = 3;
	
	private Control control;

	private Volume_Viewer vv;
	
	public TFrgb(Control control, Volume_Viewer vv) {
		super();
		this.control = control;
		this.vv = vv;
		
		setPreferredSize(new Dimension(256, height));
		addMouseListener(this);
		addMouseMotionListener(this);
		for (int i = 0; i < 256; i++) {
			vv.lookupTable.lut[i][0] = vv.lookupTable.lut[i][1] = vv.lookupTable.lut[i][2] = i;
		}
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		xLast = e.getX();
		yLast = e.getY();
		handleMouseDragged(e);
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
		if(y > height) y = height;
		int sx = xLast, ex = x, sy = yLast, ey = y;
		if(ex < sx) {
			sx = x;
			ex = xLast;
			sy = y;
			ey = yLast;
		}

		int lx = ex - sx;
		int ly = ey - sy;
		for(int i = sx; i <= ex; i++) {
			if(lx == 0) lx = 1;
			double r = (double)(i - sx) / lx;
			int yi = (int)Math.round(sy + r * ly);
			int v = 255*(height - yi)/height;
			switch(channel) {
			case 0: vv.lookupTable.lut[i][0] = v; break;
			case 1: vv.lookupTable.lut[i][1] = v; break;
			case 2: vv.lookupTable.lut[i][2] = v; break;
			case 3: vv.lookupTable.lut[i][0] = vv.lookupTable.lut[i][1] = vv.lookupTable.lut[i][2] = v; break;
			}
		}

		yLast = y;
		xLast = x;
		vv.gradientLUT.repaint();

		if (control.alphaMode == Control.ALPHA2) {
			vv.gradient2.repaint();
			if (!control.pickColor) {
				vv.tf_a2.updateLutGradVal();
				vv.tf_a2.repaint();
			}
		}
		else if (control.alphaMode == Control.ALPHA3) {
			vv.gradient3.repaint();
			if (!control.pickColor) {
				vv.tf_a3.updateLutGradVal();
				vv.tf_a3.repaint();
			}
		}
		repaint();
	}

	public void paintComponent(Graphics g) {

		int width=256;
		int[] pixels  = new int[width*height];

		for (int i = 0; i < pixels.length; i++) 
			pixels[i] = 0xFF333333; 

		int[][] lut = vv.lookupTable.lut;
		int y, y1, min, max;
		for(int x = 0; x < width-1; x++) {

			y =  height*lut[x][0]/256;
			y1 = height*lut[x+1][0]/256;
			min = Math.min(y1, y);
			max = Math.max(y1, y);
			for (int i = min; i <= max; i++)		
				pixels[(height-1-i)*256+x] |= 0xFFFF0000;

			y =  height*lut[x][1]/256;
			y1 = height*lut[x+1][1]/256;
			min = Math.min(y1, y);
			max = Math.max(y1, y);
			for (int i = min; i <= max; i++)		
				pixels[(height-1-i)*256+x] |= 0xFF00FF00;

			y =  height*lut[x][2]/256;
			y1 = height*lut[x+1][2]/256;
			min = Math.min(y1, y);
			max = Math.max(y1, y);
			for (int i = min; i <= max; i++)		
				pixels[(height-1-i)*256+x] |= 0xFF0000FF;
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, 0, 0, null);
	}
}
