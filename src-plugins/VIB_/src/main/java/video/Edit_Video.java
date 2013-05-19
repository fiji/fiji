package video;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.PolygonRoi;
import ij.gui.Line;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.Blitter;

import java.io.File;
import java.io.*;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.Panel;
import java.awt.Label;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import java.awt.Button;
import java.awt.Color;

import java.awt.geom.GeneralPath;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Edit_Video implements PlugIn, ActionListener {

	private VideoStack stack;
	private ImagePlus preview;
	private int speed = 10;
	private int color = (255 & 0xff) << 16;
	private int alpha = 128;
	private int linewidth = 2;

	public void setColor(int c) {
		color = c;
	}

	public int getColor() {
		return color;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setLinewidth(int l) {
		linewidth = l;
	}

	public int getLinewidth() {
		return linewidth;
	}

	public VideoStack getStack() {
		return stack;
	}

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Video editing");
		Panel all = new Panel();
		BoxLayout bl = new BoxLayout(all, BoxLayout.Y_AXIS);
		all.setLayout(bl);


		Panel p = new BorderPanel();
		p.setLayout(new GridLayout(1, 2, 5, 5));
		p.add(new Label("Open video"));
		Button b = new Button("Open");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(3,2));
		p.add(new Label("Add empty frame"));
		b = new Button("Add Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Copy frame"));
		b = new Button("Copy Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Delete Frame"));
		b = new Button("Delete Frame");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(2,2));
		p.add(new Label("Open Frame"));
		b = new Button("Open Frame");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Set Frame"));
		b = new Button("Set Frame");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(6, 2));
		p.add(new Label("Fade over"));
		b = new Button("Fade over");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Line"));
		b = new Button("Draw Line");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Oval"));
		b = new Button("Draw Oval");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Draw Roi"));
		b = new Button("Draw Roi");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Fill Roi transparent"));
		b = new Button("Fill Roi transparent");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Move Selection"));
		b = new Button("Move Selection");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		p = new BorderPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new Label("Create AVI"));
		b = new Button("Create AVI");
		b.addActionListener(this);
		p.add(b);
		p.add(new Label("Play AVI"));
		b = new Button("Play AVI");
		b.addActionListener(this);
		p.add(b);
		all.add(p);

		gd.addPanel(all);
		gd.setModal(false);
		gd.showDialog();
		bl.layoutContainer(all);
		all.repaint();
		if(gd.wasCanceled())
			return;
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Open"))
			open();
		else if(e.getActionCommand().equals("Add Frame"))
			addFrame();
		else if(e.getActionCommand().equals("Copy Frame"))
			copyFrame();
		else if(e.getActionCommand().equals("Open Frame"))
			openFrame();
		else if(e.getActionCommand().equals("Set Frame"))
			setFrame();
		else if(e.getActionCommand().equals("Delete Frame"))
			deleteFrame();
		else if(e.getActionCommand().equals("Fade over"))
			fadeOver();
		else if(e.getActionCommand().equals("Draw Line"))
			drawLine();
		else if(e.getActionCommand().equals("Draw Roi"))
			drawRoi();
		else if(e.getActionCommand().equals("Fill Roi transparent"))
			fillRoiTransparent();
		else if(e.getActionCommand().equals("Draw Oval"))
			drawOval();
		else if(e.getActionCommand().equals("Move Selection"))
			moveSelection();
		else if(e.getActionCommand().equals("Create AVI"))
			createAVI();
		else if(e.getActionCommand().equals("Play AVI"))
			playAVI();
		System.out.println("done");
	}

	public void open() {
			stack = new VideoStack();
			String dir = 
			"/home/bene/Desktop/video";
			String basename = "template";
			String ending = "png";
			stack.open(dir, basename, ending);
			preview = new ImagePlus("Preview", stack.getPreview());
			preview.show();
	}
	
	public void addFrame() {
		int index = preview.getCurrentSlice();
		stack.addSlice(index, null);
		preview.setSlice(index + 1);
		preview.updateAndDraw();
	}

	public void copyFrame() {
		int index = preview.getCurrentSlice();
		ImageProcessor ip = stack.getProcessor(index);
		stack.addSlice(index, ip);
		preview.setSlice(index + 1);
		preview.updateAndDraw();
	}

	public void openFrame() {
		int index = preview.getCurrentSlice();
		ImageProcessor ip = stack.getProcessor(index);
		new ImagePlus(stack.getPreview().
			getSliceLabel(index), ip).show();
	}

	public void setFrame() {
		int index = preview.getCurrentSlice();
		if(!IJ.getImage().getTitle().equals(
			stack.getPreview().getSliceLabel(index)))
			return;
		ImageProcessor ip = IJ.getImage().getProcessor();
		stack.setSlice(index, ip);
		IJ.getImage().changes = false;
		IJ.getImage().close();
		preview.setStack(null, stack.getPreview());
		preview.setSlice(index);
	}

	public void deleteFrame() {
		int index = preview.getCurrentSlice();
		stack.deleteSlice(index);
		preview.setStack(null, stack.getPreview());
		preview.setSlice(index);
	}

	public void fadeOver() {
		fade(20);
	}

	public void drawLine() {
		int index = preview.getCurrentSlice();
		ImagePlus frame = IJ.getImage();
		if(!frame.getTitle().equals(
			stack.getPreview().getSliceLabel(index)))
			return;
		Roi roi = frame.getRoi();
		if(roi == null || roi.getType() != Roi.LINE)
			return;
		Line line = (Line)roi;
		int i = drawLine(line.x1, line.y1, line.x2, line.y2, speed);
		preview.setSlice(index + i);
		frame.changes = false;
		frame.close();
	}

	public void drawRoi() {
		int index = preview.getCurrentSlice();
		ImagePlus frame = IJ.getImage();
		if(!frame.getTitle().equals(
			stack.getPreview().getSliceLabel(index))) {
			IJ.error("frame name and preview slice are not the same");
			return;
		}
		Roi roi = frame.getRoi();
		if(roi == null) {
			IJ.error("Selection required");
			return;
		}
		Roi[] rois = null;
		if(roi.getType() == Roi.COMPOSITE) {
			rois = ((ShapeRoi)roi).getRois();
		} else if(roi instanceof PolygonRoi) {
			rois = new Roi[] {roi};
		} else {
			IJ.error("Composite or polygon roi required");
			return;
		}
		int c = 0;
		for(int i = 0; i < rois.length; i++) {
			if(!(rois[i] instanceof PolygonRoi))
				continue;
			c += drawRoi(rois[i], speed);
			preview.setSlice(index + c);
		}
		frame.changes = false;
		frame.close();
	}

	public void fillRoiTransparent() {
		int index = preview.getCurrentSlice();
		ImagePlus frame = IJ.getImage();
		if(!frame.getTitle().equals(
			stack.getPreview().getSliceLabel(index)))
			return;
		Roi roi = frame.getRoi();
		if(roi == null) {
			IJ.error("Selection required");
			return;
		}
		Roi[] rois = null;
		if(roi.getType() == Roi.COMPOSITE) {
			rois = ((ShapeRoi)roi).getRois();
		} else if(roi instanceof PolygonRoi) {
			rois = new Roi[] {roi};
		} else {
			IJ.error("Composite or polygon roi required");
			return;
		}
		for(int i = 0; i < rois.length; i++) {
			if(!(rois[i] instanceof PolygonRoi))
				continue;
			fillRoiTransparent(rois[i]);
		}
		frame.changes = false;
		frame.close();
	}

	public void drawOval() {
		int index = preview.getCurrentSlice();
		ImagePlus frame = IJ.getImage();
		if(!frame.getTitle().equals(
			stack.getPreview().getSliceLabel(index)))
			return;
		Roi roi = frame.getRoi();
		if(roi == null || roi.getType() != Roi.OVAL)
			return;
		Rectangle r = roi.getBounds();
		drawCircle(r.x, r.y, r.width, r.height);
		frame.changes = false;
		frame.close();
	}

	public void moveSelection() {
		int index = preview.getCurrentSlice();
		ImagePlus frame = IJ.getImage();
		if(!frame.getTitle().equals(
			stack.getPreview().getSliceLabel(index)))
			return;
		Roi roi = frame.getRoi();
		if(roi == null)
			return;
		GenericDialog gd = new GenericDialog("Move selection");
		gd.addNumericField("dx", 0, 0);
		gd.addNumericField("dy", 0, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		moveSelection(roi, (int)gd.getNextNumber(),
			(int)gd.getNextNumber());
		frame.changes = false;
		frame.close();
	}

	public void moveSelection(Roi roi, int dx, int dy) {
		int x = roi.getBounds().x;
		int y = roi.getBounds().y;
		int index = preview.getCurrentSlice();
		int dt = dx > dy ? dx : dy;
		ImageProcessor ip = stack.getProcessor(index);
		ip.setRoi(roi);
		ImageProcessor copy = ip.crop();
		ImageProcessor ip2;
		for(int i = 0; i < dt; i++) {
			int xt = x + Math.round((float)i * dx/dt);
			int yt = y + Math.round((float)i * dy/dt);
			ip2 = ip.duplicate();
			ip2.snapshot();
			roi.setLocation(xt, yt);
			ip2.setRoi(roi);
			ip2.copyBits(copy, xt, yt, 
					Blitter.COPY_ZERO_TRANSPARENT);

			ip2.reset(ip2.getMask());
			if(!stack.addSlice(index + i, ip2))
				break;
		}
	}

	public void drawCircle(int x, int y, int w, int h) {
		int index = preview.getCurrentSlice();
		Ellipse2D e = new Ellipse2D.Float(x, y, w, h);
		float[] v = new float[6];
		boolean finished = false;

		ImageProcessor ip = stack.getProcessor(index);
		for(int z = 0; !finished; z++) {
			ip = ip.convertToRGB();
			ip.setValue(color);
			ip.setLineWidth(linewidth);
			FlatteningPathIterator it = new FlatteningPathIterator(
						e.getPathIterator(null), 1);
			it.currentSegment(v);
			ip.moveTo((int)v[0], (int)v[1]);
			it.next();
			for(int i = 0; i < z; i++) {
				it.currentSegment(v);
				ip.lineTo((int)v[0], (int)v[1]);
				it.next();
				finished = it.isDone();
			}
			if(!stack.addSlice(index + z, ip))
				break;
		}
	}

	public int drawRoi(Roi roi, int speed) {
		int index = preview.getCurrentSlice();
		Polygon poly = ((PolygonRoi)roi).getPolygon();
		int n = poly.npoints;
		int[] x = poly.xpoints;
		int[] y = poly.ypoints;
		int x_last = x[0], y_last = y[0];

		ImageProcessor ip = stack.getProcessor(index);
		ip = ip.convertToRGB();

		LineIterator li = new LineIterator();
		int c = -1;
		int slicesInserted = 0;
		for(int z = 0; z < n; z++) {
			li.init(x[z], y[z], x[(z+1)%n], y[(z+1)%n]);
			while(li.next() != null) {
				c++;
				ip.setValue(color);
				ip.setLineWidth(linewidth);
				ip.moveTo(x_last, y_last);
				x_last = (int)li.x;
				y_last = (int)li.y;
				ip.lineTo(x_last, y_last);
				if(speed == -1 || c % speed != 0)
					continue;
				if(!stack.addSlice(index + slicesInserted, ip))
					break;
				slicesInserted++;
				ip = ip.duplicate();
			}
		}
		// maybe the last one was not added (in case c % speed was 0)
		if(speed == -1) {
			stack.setSlice(index, ip);
			return 0;
		}
		if(c % speed != 0)
			if(stack.addSlice(index + slicesInserted, ip))
				slicesInserted++;
		return slicesInserted;
	}

	public int drawLine(int x1, int y1, int x2, int y2, int speed) {
		int index = preview.getCurrentSlice();
		int slicesInserted = 0;

		ImageProcessor ip = stack.getProcessor(index);
		ip = ip.convertToRGB();
		ip.setValue(color);
		ip.setLineWidth(linewidth);

		LineIterator li = new LineIterator(x1, y1, x2, y2);
		int z = -1;
		int x_last = x1, y_last = y1;
		while(li.next() != null) {
			z++;
			ip.setValue(color);
			ip.setLineWidth(linewidth);
			ip.moveTo(x_last, y_last);
			x_last = (int)li.x;
			y_last = (int)li.y;
			ip.lineTo(x_last, y_last);
			if(speed == -1 || z % speed != 0)
				continue;
			if(!stack.addSlice(index + slicesInserted, ip))
				break;
			slicesInserted++;
			ip = ip.duplicate();
		}
		// maybe the last one was not added (in case c % speed was 0)
		if(speed == -1) {
			stack.setSlice(index, ip);
			return 0;
		}
		if(z % speed != 0)
			if(stack.addSlice(index + slicesInserted, ip))
				slicesInserted++;
		return slicesInserted;
	}

	public void fillRoiTransparent(Roi roi) {
		int index = preview.getCurrentSlice();
		Polygon poly = ((PolygonRoi)roi).getPolygon();

		ImageProcessor ip = stack.getProcessor(index);
		ip = ip.convertToRGB();
		BufferedImage bi = (BufferedImage)ip.createImage();
		Graphics2D g = bi.createGraphics();
		Color col = new Color(color);
		col = new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);
		g.setColor(col);
		g.fill(poly);
		g.dispose();
		stack.setSlice(index, ip);
	}

	public void fade(int numSlices) {
		int index = preview.getCurrentSlice();
		int[] before = (int[])(stack.getProcessor(index).
				convertToRGB().getPixels());
		int[] after = (int[])(stack.getProcessor(index+1).
				convertToRGB().getPixels());

		for(int z = 1; z < numSlices; z++) {
			ColorProcessor bp = new ColorProcessor(
				stack.getWidth(), stack.getHeight());
			int[] pixels = (int[])bp.getPixels();
			double dp = z;
			double dn = numSlices-z;

			for(int i = 0; i < pixels.length; i++) {
				pixels[i] = interpolate(before[i], dp,
							after[i], dn);
			}
			if(!stack.addSlice(index + z - 1, bp))
				break;
		}
		preview.setSlice(preview.getCurrentSlice() + numSlices);
	}

	public void createAVI() {
		int w = stack.getWidth();
		int h = stack.getHeight();
		// optimal bit rate
		int obr = 50 * 25 * w * h / 256;
		// mpeg codec options
		String opt = "vbitrate=" + obr + ":mbd=2:keyint=132:vqblur=1.0:"
				+ "cmp=2:subcmp=2:dia=2:mv0:last_pred=3";
		String codec="msmpeg4v2";
		// clean temporary files that can interfere with the 
		// compression phase
		File cwd = new File(stack.getDir());
		new File(cwd, "divx2pass.log").delete();
		new File(cwd, "frameno.avi").delete();
		// compress
		String cmd;
		try {
			cmd = "mencoder -ovc lavc -lavcopts vcodec=" + codec + 
				":vpass=1:" + opt + " -mf type=png:w=" + w + 
				":h=" + h + ":fps=25 -nosound -o /dev/null " + 
				"mf://*.png";
			Process pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
			cmd = "mencoder -ovc lavc -lavcopts vcodec=" + codec + 
				":vpass=2:" + opt + " -mf type=png:w=" + w + 
				":h=" + h + ":fps=25 -nosound -o output.avi " + 
				"mf://*.png";
			pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void playAVI() {
		String cmd = "mplayer -loop 0 output.avi";
		File cwd = new File(stack.getDir());
		try {
			Process pro = Runtime.getRuntime().exec(cmd, null, cwd);
			pro.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public int interpolate(int p, double dp, int n, double dn) {
		int rp = (p&0xff0000) >> 16;
		int rn = (n&0xff0000) >> 16;
		int gp = (p&0xff00) >> 8;
		int gn = (n&0xff00) >> 8;
		int bp = p&0xff;
		int bn = n&0xff;

		byte r_int = (byte) ((rn*dp + rp*dn) / (dn + dp));
		byte g_int = (byte) ((gn*dp + gp*dn) / (dn + dp));
		byte b_int = (byte) ((bn*dp + bp*dn) / (dn + dp));

		return ((r_int&0xff) << 16) + ((g_int&0xff) << 8) + (b_int&0xff);
	}

	private static final Shape roiToShape(Roi roi) {
		Shape shape = null;
		Rectangle r = roi.getBounds();
		int[] xCoords = null;
		int[] yCoords = null;
		int nCoords = 0;
		switch(roi.getType()) {
			case Roi.LINE:
				Line line = (Line)roi;
				shape = new Line2D.Double(
						(double)(line.x1-r.x),
						(double)(line.y1-r.y),
						(double)(line.x2-r.x),
						(double)(line.y2-r.y) );
				break;
			case Roi.RECTANGLE:
				shape = new Rectangle2D.Double(0.0, 0.0,
					(double)r.width, (double)r.height);
				break;
			case Roi.OVAL:
				Polygon p = roi.getPolygon();
				for (int i=0; i<p.npoints; i++) {
					p.xpoints[i] -= r.x;
					p.ypoints[i] -= r.y;
				}
				break;
			case Roi.POLYGON:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new Polygon(xCoords,yCoords,nCoords);
				 break;
			case Roi.FREEROI: case Roi.TRACED_ROI:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new GeneralPath(
				 	GeneralPath.WIND_EVEN_ODD,nCoords);
				 ((GeneralPath)shape).moveTo(
				 	(float)xCoords[0], (float)yCoords[0]);
				 for (int i=1; i<nCoords; i++)
					 ((GeneralPath)shape).lineTo(
							(float)xCoords[i],
							(float)yCoords[i]);
				 ((GeneralPath)shape).closePath();
				 break;
			case Roi.POLYLINE: case Roi.FREELINE: case Roi.ANGLE:
				 nCoords =((PolygonRoi)roi).getNCoordinates();
				 xCoords = ((PolygonRoi)roi).getXCoordinates();
				 yCoords = ((PolygonRoi)roi).getYCoordinates();
				 shape = new GeneralPath(
				 	GeneralPath.WIND_NON_ZERO,nCoords);
				 ((GeneralPath)shape).moveTo(
				 	(float)xCoords[0], (float)yCoords[0]);
				 for (int i=1; i<nCoords; i++)
					 ((GeneralPath)shape).lineTo(
						 (float)xCoords[i],
						 (float)yCoords[i]);
				 break;
			case Roi.POINT:
				 ImageProcessor mask = roi.getMask();
				 byte[] maskPixels = (byte[])mask.getPixels();
				 Rectangle maskBounds = roi.getBounds();
				 int maskWidth = mask.getWidth();
				 Area area = new Area();
				 for (int y = 0; y < mask.getHeight(); y++) {
					 int yOffset = y * maskWidth;
					 for (int x = 0; x < maskWidth; x++) {
						 if (maskPixels[x + yOffset] != 0)
							 area.add(new Area(new Rectangle(
							 	x + maskBounds.x,
								y + maskBounds.y, 1, 1)));
					 }
				 }
				 shape = area;
				 break;
// 			case Roi.COMPOSITE:
// 				shape = ShapeRoi.cloneShape(((ShapeRoi)roi).getShape());
// 				break;
			default:
				 throw new IllegalArgumentException(
						 "Roi type not supported");
		}
		return shape;
	}

	private class LineIterator {

		int x1, y1;
		int x2, y2;
		int dx, dy;
		boolean finished;
		double x, y, dx_dt, dy_dt;

		public LineIterator() {}

		public LineIterator(int x1, int y1, int x2, int y2) {
			init(x1, y1, x2, y2);
		}

		public void init(int x1, int y1, int x2, int y2) {
			this.x1 = x1; this.x2 = x2;
			this.y1 = y1; this.y2 = y2;
			this.x = x1;
			this.y = y1;

			dx = x2 - x1;
			dy = y2 - y1;

			int dt = Math.abs(dx) > Math.abs(dy) ? dx : dy;
			dt = Math.abs(dt);
			if(dt == 0)
				dt = 1;

			dx_dt = (double)dx/dt;
			dy_dt = (double)dy/dt;

			dx = Math.abs(dx);
			dy = Math.abs(dy);

			finished = false;
		}

		public LineIterator next() {
			if(finished)
				return null;
			x += dx_dt;
			y += dy_dt;
			finished = Math.abs((int)x - x1) >= dx &&
				Math.abs((int)y - y1) >= dy;
			if(finished) {
				x = x2;
				y = y2;
			}
			return this;
		}
	}

	private class BorderPanel extends Panel {

		final Color BC = new Color(139, 142, 255);

		public Insets getInsets() {
			return new Insets(10, 10, 10, 10);
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g) {
			super.paint(g);
			if(getWidth() == 0 || getHeight() == 0)
				return;
			g.setColor(BC);
			g.drawRect(5, 5, getWidth()-10, getHeight()-10);
		}
	}
}

