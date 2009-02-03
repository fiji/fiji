import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import ij.plugin.*;
import ij.measure.*;

public class Grid_ implements PlugIn, DialogListener {
	private static String[] colors = {"Red","Green","Blue","Magenta","Cyan","Yellow","Orange","Black","White"};
	private static String color = "Cyan";
	private static String[] types = {"Lines", "Crosses", "Points", "None"};
	private static String type = types[0];
	private static double areaPerPoint;
	private static boolean randomOffset;
	private Random random = new Random(System.currentTimeMillis());	
	private ImagePlus imp;
	private double tileWidth, tileHeight;
	private int xstart, ystart;
	private int linesV, linesH;
	private double pixelWidth=1.0, pixelHeight=1.0;
	private String units = "pixels";

	public void run(String arg) {
		imp = IJ.getImage();
		showDialog();
	}
		
	void drawPoints() {
		int one = 1;
		int two = 2;
		GeneralPath path = new GeneralPath();
		for(int h=0; h<linesV; h++) {
			for(int v=0; v<linesH; v++) {
				float x = (float)(xstart+h*tileWidth);
				float y = (float)(ystart+v*tileHeight);
				path.moveTo(x-two, y-one); path.lineTo(x-two, y+one);
				path.moveTo(x+two, y-one); path.lineTo(x+two, y+one);
				path.moveTo(x-one, y-two); path.lineTo(x+one, y-two);
				path.moveTo(x-one, y+two); path.lineTo(x+one, y+two);
			}
		}
		showGrid(path);
	}

	void drawCrosses() {
		GeneralPath path = new GeneralPath();
		float arm  = 5;
		for(int h=0; h<linesV; h++) {
			for(int v=0; v<linesH; v++) {
				float x = (float)(xstart+h*tileWidth);
				float y = (float)(ystart+v*tileHeight);
				path.moveTo(x-arm, y);
				path.lineTo(x+arm, y);
				path.moveTo(x, y-arm);
				path.lineTo(x, y+arm);
			}
		}
		showGrid(path);
	}

	void showGrid(Shape shape) {
		ImageCanvas ic = imp.getCanvas();
		if (ic==null) return;
		if (shape==null)
			ic.setDisplayList(null);
		else
			ic.setDisplayList(shape, getColor(), null);
	}

	void drawLines() {
		GeneralPath path = new GeneralPath();
		int width = imp.getWidth();
		int height = imp.getHeight();
		for(int i=0; i<linesV; i++) {
			float xoff = (float)(xstart+i*tileWidth);
			path.moveTo(xoff,0f);
			path.lineTo(xoff, height);
		}
		for(int i=0; i<linesH; i++) {
			float yoff = (float)(ystart+i*tileHeight);
			path.moveTo(0f, yoff);
			path.lineTo(width, yoff);
		}
		showGrid(path);
	}

	void showDialog() {
		int width = imp.getWidth();
		int height = imp.getHeight();
		Calibration cal = imp.getCalibration();
		int places;
		if (cal.scaled()) {
			pixelWidth = cal.pixelWidth;
			pixelHeight = cal.pixelHeight;
			units = cal.getUnits();
			places = 2;
		} else {
			pixelWidth = 1.0;
			pixelHeight = 1.0;
			units = "pixels";
			places = 0;
		}
		if (areaPerPoint==0.0)
			areaPerPoint = (width*cal.pixelWidth*height*cal.pixelHeight)/81.0; // default to 9x9 grid
		ImageWindow win = imp.getWindow();
		GenericDialog gd = new GenericDialog("Grid...");
		gd.addChoice("Grid Type:", types, type);
		gd.addNumericField("Area per Point:", areaPerPoint, places, 6, units+"^2");
		gd.addChoice("Color:", colors, color);
		gd.addCheckbox("Random Offset", randomOffset);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) 
			showGrid(null);
	}

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		int width = imp.getWidth();
		int height = imp.getHeight();
		type = gd.getNextChoice();
		areaPerPoint = gd.getNextNumber();
		color = gd.getNextChoice();
		randomOffset = gd.getNextBoolean();
		
		double minArea= (width*height)/50000.0;
		if (type.equals(types[1])&&minArea<144.0)
			minArea = 144.0;
		else if (minArea<16)
			minArea = 16.0;
		if (areaPerPoint/(pixelWidth*pixelHeight)<minArea) {
			String err = "\"Area per Point\" too small";
			if (gd.wasOKed())
				IJ.error("Grid", err);
			else
				IJ.showStatus(err);
			return true;
		}
		double tileSize = Math.sqrt(areaPerPoint);
		tileWidth = tileSize/pixelWidth;
		tileHeight = tileSize/pixelHeight;
		if (randomOffset) {
			xstart = (int)(random.nextDouble()*tileWidth);
			ystart = (int)(random.nextDouble()*tileHeight);
		} else {
			xstart = (int)(tileWidth/2.0+0.5);
			ystart = (int)(tileHeight/2.0+0.5);
		}
		linesV = (int)((width-xstart)/tileWidth)+1; 
		linesH = (int)((height-ystart)/tileHeight)+1;
		if (gd.invalidNumber())
			return true;
		if (type.equals(types[0]))
			drawLines();
		else if (type.equals(types[1]))
			drawCrosses();
		else  if (type.equals(types[2]))
			drawPoints();
		else
			showGrid(null);
        	return true;
	}

	Color getColor() {
		Color c = Color.cyan;
		if (color.equals(colors[0])) c = Color.red;
		else if (color.equals(colors[1])) c = Color.green;
		else if (color.equals(colors[2])) c = Color.blue;
		else if (color.equals(colors[3])) c = Color.magenta;
		else if (color.equals(colors[4])) c = Color.cyan;
		else if (color.equals(colors[5])) c = Color.yellow;
		else if (color.equals(colors[6])) c = Color.orange;
		else if (color.equals(colors[7])) c = Color.black;
		else if (color.equals(colors[8])) c = Color.white;
		return c;
	}
}
