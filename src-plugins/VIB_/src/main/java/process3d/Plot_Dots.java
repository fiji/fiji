package process3d;

import java.awt.Font;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import java.text.DecimalFormat;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.plugin.PlugIn;
import ij.ImagePlus;

public class Plot_Dots implements PlugIn, MouseMotionListener {

	public double[] x = new double[]{0, 5, 10};
	public double[] y = new double[]{0, 5, 10};
	public String[] labels = new String[]{"p1", "p2", "p3"};

	public static final int size = 300;

	public String xLabel = "xLabel";
	public String yLabel = "yLabel";

	public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);
	public static final DecimalFormat DF = new DecimalFormat("#####.000");

	public static final int INDENT = 40;
	public static final int CTRL_HEIGHT = 100;
	public static boolean drawLabels = true;

	private ImageProcessor ip;
	private double xmin, ymin, xmax, ymax, xdiff, ydiff, factor;
	private int w, h;
	private ImagePlus imp;


	public void run(String arg) {
	}

	public void create() {
		xmin = min(x);
		ymin = min(y);
		xmax = max(x);
		ymax = max(y);
		xdiff = xmax - xmin;
		ydiff = ymax - ymin;
		factor = xdiff > ydiff ? size / xdiff : size / ydiff;
		w = (int)(xdiff * factor);
		h = (int)(ydiff * factor);
		ip = new ByteProcessor(w+2*INDENT, h+3*INDENT+CTRL_HEIGHT);
		ip.setColor(255);
		ip.fill();
		drawAxis();
		drawPoints();
		imp = new ImagePlus("lkdjf", ip);
		imp.show();
		imp.getCanvas().addMouseMotionListener(this);
	}

	public void mouseMoved(MouseEvent e) {
		double x = (e.getX() - INDENT) / factor  + xmin;
		double y = (h + INDENT - e.getY()) / factor  + ymin;
		drawPos(x, y);
		drawNearestPoint(x, y);
		imp.updateAndDraw();
	}

	public int getNearestPoint(double xi, double yi) {
		double min = Double.MAX_VALUE;
		int index = 0;
		for(int i = 0; i < x.length; i++) {
			double d = Math.abs(xi - x[i]) + Math.abs(yi - y[i]);
			if(d < min) {
				min = d;
				index = i;
			}
		}
		return index;
	}

	public void mouseDragged(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouxeExited(MouseEvent e) {}

	public void drawAxis() {
		ip.setColor(0);
		ip.setFont(FONT);
		ip.drawLine(INDENT, h+INDENT, w+INDENT, h+INDENT);
		ip.drawLine(INDENT, h+INDENT, INDENT, INDENT);
		ip.setJustification(ImageProcessor.CENTER_JUSTIFY);
		ip.drawString(xLabel, w/2+INDENT, h+2*INDENT-3);
		ImageProcessor tmp = new ByteProcessor(h+2*INDENT, 12);
		tmp.setColor(255);
		tmp.fill();
		tmp.setFont(FONT);
		tmp.setJustification(ImageProcessor.CENTER_JUSTIFY);
		tmp.setColor(0);
		tmp.drawString(yLabel, h/2+INDENT, 12);
		tmp = tmp.rotateLeft();
		ip.insert(tmp, 0, 0);
		drawXMarker(xmin);
		drawXMarker(xmax);
		drawXMarker(xmin + (xmax-xmin)/2);
		drawYMarker(ymin);
		drawYMarker(ymax);
		drawYMarker(ymin + (ymax-ymin)/2);
	}

	public void drawPos(double x, double y) {
		ip.setColor(255);
		ip.setRoi(INDENT, h + 2*INDENT, w, CTRL_HEIGHT);
		ip.fill();
		ip.resetRoi();
		ip.setColor(0);
		ip.setFont(new Font("SansSerif", Font.PLAIN, 12));
		ip.drawString("X pos: " + DF.format(x), 
			INDENT, 
			h + 2*INDENT + 20);
		ip.drawString("Y pos: " + DF.format(y),
			INDENT,
			h + 2*INDENT + 35);
	}

	int previous = 0;

	public void drawNearestPoint(double xi, double yi) {
		ip.setFont(new Font("SansSerif", Font.BOLD, 12));
		ip.drawString("Nearest point:", INDENT, h + 2*INDENT + 55);

		drawPoint(previous, 0);
		int i = getNearestPoint(xi, yi);
		ip.setFont(new Font("SansSerif", Font.PLAIN, 12));
		ip.drawString("Point: " + labels[i],
				INDENT, h + 2*INDENT + 70);
		ip.drawString(xLabel + ": " + DF.format(x[i]), 
				INDENT, h + 2*INDENT + 85);
		ip.drawString(yLabel + ": " + DF.format(y[i]), 
				INDENT, h + 2*INDENT + 100);
		drawPoint(i, 100);
		previous = i;
	}


	public void drawXMarker(double x) {
		int xp = (int)(factor*(x-xmin)) + INDENT;
		ip.drawLine(xp, h+INDENT-2, xp, h+INDENT+2);
		ip.setJustification(ImageProcessor.CENTER_JUSTIFY);
		ip.drawString(DF.format(x), xp, h+2*INDENT-INDENT/2);
	}

	public void drawYMarker(double y) {
		int yp = h + INDENT - (int)(factor*(y-ymin)) ;
		ip.drawLine(INDENT-2, yp, INDENT+2, yp);
		ip.setJustification(ImageProcessor.CENTER_JUSTIFY);
		ip.drawString(DF.format(y), INDENT/2, yp);
	}

	public void drawPoint(int i, int c) {
		int xp = (int)(factor*(x[i]-xmin)) + INDENT;
		int yp = h + INDENT - (int)(factor*(y[i]-ymin)) ;
		ip.setColor(c);
		ip.setFont(FONT);
		ip.drawLine(xp-3, yp-3, xp+3, yp+3);
		ip.setLineWidth(2);
		ip.drawLine(xp-3, yp+3, xp+3, yp-3);
		if(drawLabels)
			ip.drawString(labels[i], xp+4, yp+4);
	}
		

	public void drawPoints() {
		ip.setJustification(ImageProcessor.LEFT_JUSTIFY);
		for(int i = 0; i < x.length; i++) {
			drawPoint(i, 0);
		}
	}

	public double max(double[] values) {
		double max = values[0];
		for(int i = 1; i < values.length; i++)
			if(values[i] > max)
				max = values[i];
		return max;
	}

	public double min(double[] values) {
		double min = values[0];
		for(int i = 0; i < values.length; i++)
			if(values[i] < min)
				min = values[i];
		return min;
	}

}
