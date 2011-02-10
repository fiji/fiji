package ij3d.gui;

import ij.gui.GenericDialog;

import java.util.ArrayList;

import java.awt.*;
import java.awt.event.*;

public class LUTDialog extends GenericDialog {

	private final ArrayList<Listener> listeners = new ArrayList<Listener>();
	private final Repainter repainter = new Repainter();
	private final ChannelsTool tool;

	private final int[] rOld = new int[256];
	private final int[] gOld = new int[256];
	private final int[] bOld = new int[256];
	private final int[] aOld = new int[256];

	private final int[] r, g, b, a;

	public LUTDialog(final int[] r, final int[] g, final int[] b, final int[] a) {
		super("Transfer functions");

		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;

		System.arraycopy(r, 0, rOld, 0, 256);
		System.arraycopy(g, 0, gOld, 0, 256);
		System.arraycopy(b, 0, bOld, 0, 256);
		System.arraycopy(a, 0, aOld, 0, 256);

		setModal(false);
		tool = new ChannelsTool(r, g, b, a);
		addPanel(tool);
		String[] choice = new String[] {
			"Red", "Green", "Blue", "Alpha", "RGB", "RGBA" };
		addChoice("Channel", choice, choice[0]);
		final Choice cho = (Choice)getChoices().get(0);
		cho.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				tool.channel = cho.getSelectedIndex();
				tool.repaint();
			}
		});
		Panel p = new Panel(new FlowLayout());
		Button button = new Button("Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
				repainter.requestRepaint();
				tool.repaint();
			}
		});
		p.add(button);
		addPanel(p);
		repainter.start();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				repainter.quit();
			}
		});
	}

	public void addCtrlHint() {
		addMessage("Press <Ctrl> to create straight lines");
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Cancel")) {
			reset();
			repainter.requestRepaint();
			tool.repaint();
		}
		super.actionPerformed(e);
	}

	public void reset() {
		System.arraycopy(rOld, 0, r, 0, 256);
		System.arraycopy(gOld, 0, g, 0, 256);
		System.arraycopy(bOld, 0, b, 0, 256);
		System.arraycopy(aOld, 0, a, 0, 256);
	}

	public static interface Listener {
		public void applied();
	}

	public void addListener(Listener l) {
		listeners.add(l);
	}

	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	private void fireApplied() {
		for(Listener l : listeners)
			l.applied();
	}

	private class Repainter extends Thread {

		boolean repaintNeeded = false;
		boolean stop = false;

		public synchronized void requestRepaint() {
			if(!repaintNeeded) {
				repaintNeeded = true;
				this.notify();
			}
		}

		public void repaintIfNeeded() {
			synchronized(this) {
				if(!repaintNeeded) {
					try {
						this.wait();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					repaintNeeded = false;
				}
			}
			fireApplied();
		}

		// TODO not called so far
		public void quit() {
			stop = true;
			// make sure we're not waiting
			requestRepaint();
		}

		public void run() {
			while(!stop) {
				repaintIfNeeded();
			}
		}
	}

	private class ChannelsTool extends Panel implements MouseListener, MouseMotionListener {

		final static int RED   = 0;
		final static int GREEN = 1;
		final static int BLUE  = 2;
		final static int ALPHA = 3;
		final static int RGB   = 4;
		final static int RGBA  = 5;

		private boolean allLuts = false;

		private final int[][] luts;
		private final Color[] colors = new Color[] {
			Color.RED, Color.GREEN, Color.BLUE, Color.WHITE };

		private int channel = 0;

		public ChannelsTool(int[] r, int[] g, int[] b, int[] a) {
			super();
			setPreferredSize(new Dimension(256, 256));
			addMouseListener(this);
			addMouseMotionListener(this);
			setBackground(Color.BLACK);
			luts = new int[4][];
			luts[0] = r;
			luts[1] = g;
			luts[2] = b;
			luts[3] = a;
		}

		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			xLast = e.getX();
			yLast = e.getY();
			handleMouseDragged(e);
		}
		public void mouseReleased(MouseEvent e) {}
		public void mouseClicked(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}

		int xLast = -1, yLast = -1;
		public void mouseDragged(MouseEvent e) {
			handleMouseDragged(e);
		}

		public void handleMouseDragged(MouseEvent e) {
			int mod = e.getModifiers();
			boolean ctrl = ((mod & InputEvent.CTRL_MASK) != 0);
			handleMouseDraggedWithoutCtrl(e, ctrl);
		}

		public void handleMouseDraggedWithoutCtrl(MouseEvent e, boolean ctrl) {
			int x = e.getX(), y = e.getY();
			if(x < 0) x = 0;
			if(y < 0) y = 0;
			if(x > 255) x = 255;
			if(y > 255) y = 255;
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
				int v = 255 - yi;
				switch(channel) {
					case 0:
					case 1:
					case 2:
					case 3: luts[channel][i] = v; break;
					case 4: luts[0][i] = luts[1][i] = luts[2][i] = v; break;
					case 5: luts[0][i] = luts[1][i] = luts[2][i] = luts[3][i] = v; break;
				}
			}
			if(!ctrl) {
				yLast = y;
				xLast = x;
			}
			repaint();
			repainter.requestRepaint();
		}

		public void paint(Graphics g) {
			// single channel
			if(channel < 4) {
				paintLut(g, luts[channel], colors[channel]);
				return;
			}
			// rgb
			paintLut(g, luts[0], colors[0]);
			paintLut(g, luts[1], colors[1]);
			paintLut(g, luts[2], colors[2]);

			// rgba
			if(channel == 5)
				paintLut(g, luts[3], colors[3]);
		}

		public void paintLut(Graphics g, int[] lut, Color c) {
			g.setColor(c);
			int x = 0;
			int y = 255 - lut[x];
			for(int i = 1; i < 256; i++) {
				g.drawLine(x, y, i, 255 - lut[i]);
				x = i;
				y = 255 - lut[i];
			}
		}
	}
}
