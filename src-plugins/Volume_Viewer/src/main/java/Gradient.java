/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.UIManager;

public class Gradient extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private int height, width;
	private int wA=100;
	private Control control;
	private int[][] lut;
	private Volume_Viewer vv;

	public Gradient(Control control, Volume_Viewer vv, int width, int height) {
		super();
		
		this.control = control;
		this.vv = vv;
		
		this.lut = vv.lookupTable.lut;
		this.width = width;
		if (width != 256)
			height += 7;
		this.height = height;
		setPreferredSize(new Dimension(width, height));
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		mouseDragged(e);
	}

	public void mouseDragged(MouseEvent e) {

		int x = e.getX();

		if (width == 256) {
			if (x < 0) x = 0;
			if (x > 255) x = 255;
			control.rPaint = lut[x][0];
			control.gPaint = lut[x][1];
			control.bPaint = lut[x][2];	
			control.indexPaint = x;	

			if (control.LOG) System.out.println(control.rPaint + " " + control.gPaint + " " + control.bPaint);
		}
		else {
			if (x >= 0 && x <= wA) {
				int xv = x;
				if (xv < 0) xv = 0;
				if (xv > wA) xv = wA;
				if (control.alphaMode == Control.ALPHA2)
					control.alphaPaint2 = xv;
				else if (control.alphaMode == Control.ALPHA3)
					control.alphaPaint3 = xv;
				else if (control.alphaMode == Control.ALPHA4)
					control.alphaPaint4 = xv;
			}
		}
		if (control.alphaMode == Control.ALPHA2 && vv.gradient2 != null)
			vv.gradient2.repaint();
		if (control.alphaMode == Control.ALPHA3 && vv.gradient3 != null)
			vv.gradient3.repaint();
		if (control.alphaMode == Control.ALPHA4 && vv.gradient4 != null)
			vv.gradient4.repaint();

		repaint();
	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		int[] pixels  = new int[width*height];

		if (width == 256) {
			for (int x = 0; x < width; x++){
				int r =  lut[x][0];
				int gr = lut[x][1];
				int b =  lut[x][2];
				for (int y = 0; y < height; y++){
					int pos = y*width+x;

					if (x == 0 || x == width-1 || y == 0 || y == height-1)
						pixels[pos] = 0xFF777777;
					else
						pixels[pos] = 0xff000000  | (r << 16) | (gr <<8) | b;						
				}
			}
		}
		else {
			Color color = UIManager.getColor ( "Panel.background" );
			int cBack = color.getRGB();
			int alphaPaint = 0;
			if (control.alphaMode == Control.ALPHA2)
				alphaPaint = control.alphaPaint2;
			else if (control.alphaMode == Control.ALPHA3)
				alphaPaint = control.alphaPaint3;
			else if (control.alphaMode == Control.ALPHA4)
				alphaPaint = control.alphaPaint4;

			int cc = 0xFF000000 | (control.rPaint<<16) | (control.gPaint<<8) | control.bPaint;

			for (int i = 0; i < pixels.length; i++)
				pixels[i] = cBack;

			int yc = 9;
			int xc2 = wA + 22;

			for (int y = 0; y < 19; y++){
				int dy = Math.abs(y-yc);
				for (int x = 0; x < width; x++){
					int pos = y*width+x;
					// alpha gradient
					if (x >= 0 && x <= wA) { 
						int xv = x;
						if (x == 0 || x == wA || y == 0 || y == 18)
							pixels[pos] = 0xFF000000;
						else
							pixels[pos] = 0xFF000000 | ((255-xv)<<16) | ((255-xv)<<8) | (255-xv) ;
					}

					// color gradient
					if ((control.pickColor || control.alphaMode == Control.ALPHA4) && !(control.isRGB && control.lutNr == Control.ORIG)) {
						int dx = Math.abs(x-xc2);
						int d = Math.max(dx,dy);
						if (d == 9)
							pixels[pos] = 0xFF000000;
						else if (d < 9)
							pixels[pos] = cc;
					}		
				}
			}
			//xv = (int) ((xv-wA/2)*1.2)+wA/2;
			int apos = alphaPaint; //(int) ((alphaPaint-wA/2)*1.2 + wA/2);
			if (apos < 3) apos = 3;
			pixels[19*width + apos  ] = 
					pixels[20*width + apos-1] = 
					pixels[20*width + apos  ] = 
					pixels[20*width + apos+1] = 
					pixels[21*width + apos-2] = 
					pixels[21*width + apos-1] = 
					pixels[21*width + apos  ] = 
					pixels[21*width + apos+1] = 
					pixels[21*width + apos+2] = 
					pixels[22*width + apos-3] = 
					pixels[22*width + apos-2] =
					pixels[22*width + apos-1] = 
					pixels[22*width + apos  ] = 
					pixels[22*width + apos+1] = 
					pixels[22*width + apos+2] = 
					pixels[22*width + apos+3] = 
					pixels[23*width + apos-3] = 
					pixels[23*width + apos-2] =
					pixels[23*width + apos-1] = 
					pixels[23*width + apos  ] = 
					pixels[23*width + apos+1] = 
					pixels[23*width + apos+2] = 
					pixels[23*width + apos+3] = 0xFF000000;
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		g2.drawImage(image, 0, 0, null);
	}
}