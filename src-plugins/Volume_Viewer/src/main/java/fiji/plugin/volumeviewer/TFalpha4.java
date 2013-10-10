/*
 * Volume Viewer 2.0
 * 27.11.2012
 * 
 * (C) Kai Uwe Barthel
 */

package fiji.plugin.volumeviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;


public class TFalpha4 extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private Control control;


	private int widthV, heightV, depthV;
	private byte[][][] alpha_3D;
	private byte[][][] aPaint_3D2;
	private int alphaOffset = 0;
	
	public TFalpha4(Control control, Volume vol, byte[][][] alpha_3D, byte[][][] aPaint_3D2) {
		super();
		
		this.control = control;

		this.alpha_3D = alpha_3D;
		this.aPaint_3D2 = aPaint_3D2;
		
		depthV = vol.depthV;
		heightV = vol.heightV;
		widthV = vol.widthV;
		
		setPreferredSize(new Dimension(256, 128));
		addMouseListener(this);
		addMouseMotionListener(this);
		clearAlpha();
	}
	

	public void clearAlpha() {
		for(int z=0; z < depthV+4; z++) {
			for (int y = 0; y < heightV+4; y++) {
				for (int x = 0; x < widthV+4; x++) {
					alpha_3D[z][y][x] = aPaint_3D2[z][y][x] = 0;
				}
			}
		}
		control.alphaWasChanged = true;
	}

	public void scaleAlpha() {
		for(int z=0; z < depthV+4; z++) {
			for (int y = 0; y < heightV+4; y++) {
				for (int x = 0; x < widthV+4; x++) {
					int alpha = 2*aPaint_3D2[z][y][x];
					if (alpha > 0) {
						alpha += alphaOffset ;
						alpha = Math.min(255, Math.max(0, alpha));
						alpha_3D[z][y][x] = (byte)alpha;
					}
				}
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
		handleMouseDragged(e);
		control.drag = true;
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
		control.alphaWasChanged = true;
	}

	public void handleMouseDragged(MouseEvent e) {}

	public void paintComponent(Graphics g) {}


	public void setAlphaOffset(int alphaOffset) {
		this.alphaOffset = alphaOffset;	
	}

	public int getAlphaOffset() {
		return alphaOffset;
	}
}
