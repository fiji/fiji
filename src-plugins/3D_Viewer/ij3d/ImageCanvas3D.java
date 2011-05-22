package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Background;
import javax.vecmath.Color3f;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.awt.Dimension;

import java.util.Map;
import java.util.HashMap;

import ij.process.ByteProcessor;
import ij.gui.ImageCanvas;
import ij.ImagePlus;
import ij.gui.Roi;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ImageCanvas3D extends Canvas3D implements KeyListener {

	private RoiImagePlus roiImagePlus;
	private ImageCanvas roiImageCanvas;
	private Map<Integer, Long> pressed, released;
	private Background background;
	private UIAdapter ui;
	final private ExecutorService exec = Executors.newSingleThreadExecutor();

	protected void flush() {
		exec.shutdown();
	}

	private class RoiImagePlus extends ImagePlus {
		public RoiImagePlus(String title, ByteProcessor ip) {
			super();
			setProcessor(title, ip);
			pressed = new HashMap<Integer, Long>();
			released = new HashMap<Integer, Long>();
		}

		public ImageCanvas getCanvas() {
			return roiImageCanvas;
		}
	}

	public ImageCanvas3D(int width, int height, UIAdapter uia) {
		super(SimpleUniverse.getPreferredConfiguration());
		this.ui = uia;
		setPreferredSize(new Dimension(width, height));
		ByteProcessor ip = new ByteProcessor(width, height);
		roiImagePlus = new RoiImagePlus("RoiImage", ip);
		roiImageCanvas = new ImageCanvas(roiImagePlus) {
			/* prevent ROI to enlarge/move on mouse click */
			public void mousePressed(MouseEvent e) {
				if(!ui.isMagnifierTool() && !ui.isPointTool())
					super.mousePressed(e);
			}
		};
		roiImageCanvas.removeKeyListener(ij.IJ.getInstance());
		roiImageCanvas.removeMouseListener(roiImageCanvas);
		roiImageCanvas.removeMouseMotionListener(roiImageCanvas);
		roiImageCanvas.disablePopupMenu(true);

		background = new Background(
			new Color3f(UniverseSettings.defaultBackground));
		background.setCapability(Background.ALLOW_COLOR_WRITE);

		addListeners();
	}

	public Background getBG() { //can't use getBackground()
		return background;
	}

	public void killRoi() {
		roiImagePlus.killRoi();
		render();
	}

	void addListeners() {
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if(ui.isRoiTool())
					exec.submit(new Runnable() { public void run() {
						postRender();
					}});
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(ui.isRoiTool())
					exec.submit(new Runnable() { public void run() {
						render();
					}});
			}
			public void mouseReleased(MouseEvent e) {
				if(ui.isRoiTool())
					exec.submit(new Runnable() { public void run() {
						render();
					}});
			}
			public void mousePressed(MouseEvent e) {
				if(!ui.isRoiTool())
					roiImagePlus.killRoi();
			}
		});
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				exec.submit(new Runnable() { public void run() {
					ByteProcessor ip = new ByteProcessor(
									getWidth(),
									getHeight());
					roiImagePlus.setProcessor("RoiImagePlus", ip);
					render();
				}});
			}
		});
	}

	public ImageCanvas getRoiCanvas() {
		return roiImageCanvas;
	}

	public Roi getRoi() {
		return roiImagePlus.getRoi();
	}

	public void render() {
		stopRenderer();
		swap();
		startRenderer();
	}

	/*
	 * Needed for the isKeyDown() method. Problem:
	 * keyPressed() and keyReleased is fired periodically,
	 * dependent on the operating system preferences,
	 * even if the key is hold down.
	 */
	public void keyTyped(KeyEvent e) {}

	public synchronized void keyPressed(KeyEvent e) {
		long when = e.getWhen();
		pressed.put(e.getKeyCode(), when);
	}

	public synchronized void keyReleased(KeyEvent e) {
		long when = e.getWhen();
		released.put(e.getKeyCode(), when);
	}

	public synchronized void releaseKey(int keycode) {
		pressed.remove(keycode);
		released.remove(keycode);
	}

	public synchronized boolean isKeyDown(int keycode) {
		if(!pressed.containsKey(keycode))
			return false;
		if(!released.containsKey(keycode))
			return true;
		long p = pressed.get(keycode);
		long r = released.get(keycode);
		return p >= r || System.currentTimeMillis() - r < 100;
	}

	public void postRender() {
		J3DGraphics2D g3d = getGraphics2D();
		Roi roi = roiImagePlus.getRoi();
		if(roi != null) {
			roi.draw(g3d);
		}
		g3d.flush(true);
	}
}

