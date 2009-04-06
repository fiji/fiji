import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

	/** Draws an arrow at the location of the current straight straight
		line selection using the current color and line width.
	*/
	public class Arrow_ implements PlugInFilter {

	static final int HEAD_SIZE = 12;
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		Roi roi = imp.getRoi();
		if (roi==null || roi.getType()!=Roi.LINE)
			{IJ.error("Straight line selection required"); return;}
		ip.setColor(Toolbar.getForegroundColor());
		double size = HEAD_SIZE + HEAD_SIZE*Line.getWidth()*0.25;
		Line l = (Line)roi;
		drawArrow(ip, l.x1, l.y1, l.x2, l.y2, size);
		imp.killRoi();
	}

	/** Draws an arrow. Based on code posted to comp.lang.java.gui by Marcel Nijman. */
	public void drawArrow(ImageProcessor ip, int x1, int y1, int x2, int y2, double size) {
		double dx = x2-x1;
		double dy = y2-y1;
		double ra = java.lang.Math.sqrt(dx*dx + dy*dy);
		dx /= ra;
		dy /= ra;
		int x3 = (int)Math.round(x2-dx*size);
		int y3 = (int)Math.round(y2-dy*size);
		double r = 0.3*size;
		int x4 = (int)Math.round(x3+dy*r);
		int y4 = (int)Math.round(y3-dx*r);
		int x5 = (int)Math.round(x3-dy*r);
		int y5 = (int)Math.round(y3+dx*r);
		ip.moveTo(x1, y1); ip.lineTo(x2, y2);
		ip.moveTo(x4,y4); ip.lineTo(x2,y2); ip.lineTo(x5,y5);
	}
}
