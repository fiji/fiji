/*====================================================================
| Version: June 16, 2009
\===================================================================*/

/*====================================================================
| Philippe Thevenaz
| EPFL/STI/IOA/LIB/BM.4.137
| Station 17
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

// snake2D
import snake2D.Snake2D;
import snake2D.Snake2DKeeper;
import snake2D.Snake2DNode;
import snake2D.Snake2DScale;

// ImageJ
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

// Java 1.2
import java.awt.geom.Point2D;

/*====================================================================
|	Snakuscule_
\===================================================================*/
public class Snakuscule_
	implements
		PlugInFilter

{ /* begin class Snakuscule_ */

/*....................................................................
	private variables
....................................................................*/

private ImagePlus imp = null;

/*....................................................................
	PlugInFilter methods
....................................................................*/

/*------------------------------------------------------------------*/
public void run (
	final ImageProcessor ip
) {
	Snake2DKeeper keeper = null;
	final MySnakuscule snakuscule = new MySnakuscule(ip);
	keeper = new Snake2DKeeper();
	keeper.interactAndOptimize(snakuscule, imp);
} /* end run */

/*------------------------------------------------------------------*/
public int setup (
	final String arg,
	final ImagePlus imp
) {
	this.imp = imp;
	return(DOES_ALL);
} /* end setup */

} /* end class Snakuscule_ */

/*====================================================================
|	MySnakuscule
\===================================================================*/
class MySnakuscule
	implements
		Snake2D

{ /* begin class MySnakuscule */

/*....................................................................
	private variables
....................................................................*/

private ImageProcessor ip = null;
private final Snake2DNode[] node = new Snake2DNode[2];
private static final double GOLDEN_RATIO = 0.5 + Math.sqrt(1.25);
private static final double REGULARIZATION_WEIGHT = 100.0;
private static final double SQRT2 = Math.sqrt(2.0);
private static final double SQRT_TINY =
	Math.sqrt((double)Float.intBitsToFloat((int)0x33FFFFFF));
private static final double TINY =
	(double)Float.intBitsToFloat((int)0x33FFFFFF);

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
MySnakuscule (
	final ImageProcessor ip
) {
	this.ip = ip;
	final int width = ip.getWidth();
	final int height = ip.getHeight();
	node[0] = new Snake2DNode((int)(0.5 * (double)width
		- Math.max(3.0 + 2.0 * SQRT2, 0.25 * (GOLDEN_RATIO - 1.0)
		* Math.min((double)width, (double)height))), height / 2);
	node[1] = new Snake2DNode((int)(0.5 * (double)width
		+ Math.max(3.0 + 2.0 * SQRT2, 0.25 * (GOLDEN_RATIO - 1.0)
		* Math.min((double)width, (double)height))), height / 2);
} /* end MySnakuscule */

/*....................................................................
	Snake2D methods
....................................................................*/

/*------------------------------------------------------------------*/
public double energy (
) {
	final double weightedArea = contrast(node[0], node[1]);
	final double regularization = regularization(node[0], node[1]);
	return(weightedArea + regularization);
} /* end energy */

/*------------------------------------------------------------------*/
public Point2D.Double[] getEnergyGradient (
) {
	final Point2D.Double[] gc = contrastGradient(node[0], node[1]);
	final Point2D.Double[] gr = regularizationGradient(node[0], node[1]);
	return(plus(gc, gr));
} /* end getEnergyGradient */

/*------------------------------------------------------------------*/
public Snake2DNode[] getNodes (
) {
	return(node);
} /* end getNodes */

/*------------------------------------------------------------------*/
public Snake2DScale[] getScales (
) {
	final int R = (int)Math.round(0.5 * node[0].distance(node[1]));
	if ((2.0 * (double)(ip.getWidth() + ip.getHeight()))
		< (2.0 * Math.PI * (double)R)) {
		return(null);
	}
	final int r = (int)Math.round(0.5 * node[0].distance(node[1]) / SQRT2);
	final int x0 = (int)Math.round(0.5 * (double)(node[0].x + node[1].x));
	final int y0 = (int)Math.round(0.5 * (double)(node[0].y + node[1].y));
	final Snake2DScale[] skin = new Snake2DScale[16];
	for (int k = 0, K = skin.length; (k < K); k++) {
		skin[k] = new Snake2DScale();
		skin[k].closed = false;
	}
	int H = 1 - R;
	int X = 0;
	int Y = R;
	int dU = 3;
	int dD = 5 - 2 * R;
	skin[0].addPoint(x0, y0 + Y);
	skin[1].addPoint(x0, y0 - Y);
	skin[2].addPoint(x0, y0 + Y);
	skin[3].addPoint(x0, y0 - Y);
	skin[4].addPoint(x0 + Y, y0);
	skin[5].addPoint(x0 + Y, y0);
	skin[6].addPoint(x0 - Y, y0);
	skin[7].addPoint(x0 - Y, y0);
	while (X < Y) {
		X++;
		if (H < 0) {
			H += dU;
			dD += 2;
		}
		else {
			Y--;
			H += dD;
			dD += 4;
		}
		dU += 2;
		skin[0].addPoint(x0 + X, y0 + Y);
		skin[1].addPoint(x0 + X, y0 - Y);
		skin[2].addPoint(x0 - X, y0 + Y);
		skin[3].addPoint(x0 - X, y0 - Y);
		skin[4].addPoint(x0 + Y, y0 + X);
		skin[5].addPoint(x0 + Y, y0 - X);
		skin[6].addPoint(x0 - Y, y0 + X);
		skin[7].addPoint(x0 - Y, y0 - X);
	}
	H = 1 - r;
	X = 0;
	Y = r;
	dU = 3;
	dD = 5 - 2 * r;
	skin[8].addPoint(x0, y0 + Y);
	skin[9].addPoint(x0, y0 - Y);
	skin[10].addPoint(x0, y0 + Y);
	skin[11].addPoint(x0, y0 - Y);
	skin[12].addPoint(x0 + Y, y0);
	skin[13].addPoint(x0 + Y, y0);
	skin[14].addPoint(x0 - Y, y0);
	skin[15].addPoint(x0 - Y, y0);
	while (X < Y) {
		X++;
		if (H < 0) {
			H += dU;
			dD += 2;
		}
		else {
			Y--;
			H += dD;
			dD += 4;
		}
		dU += 2;
		skin[8].addPoint(x0 + X, y0 + Y);
		skin[9].addPoint(x0 + X, y0 - Y);
		skin[10].addPoint(x0 - X, y0 + Y);
		skin[11].addPoint(x0 - X, y0 - Y);
		skin[12].addPoint(x0 + Y, y0 + X);
		skin[13].addPoint(x0 + Y, y0 - X);
		skin[14].addPoint(x0 - Y, y0 + X);
		skin[15].addPoint(x0 - Y, y0 - X);
	}
	return(skin);
} /* end getScales */

/*------------------------------------------------------------------*/
public void setNodes (
	final Snake2DNode[] node
) {
	this.node[0].x = node[0].x;
	this.node[0].y = node[0].y;
	this.node[1].x = node[1].x;
	this.node[1].y = node[1].y;
	final double x0 = 0.5 * (node[0].x + node[1].x);
	final double y0 = 0.5 * (node[0].y + node[1].y);
	final double R = 0.5 * node[0].distance(node[1]);
} /* end setNodes */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private double contrast (
	final Snake2DNode p,
	final Snake2DNode q
) {
	double c = 0.0;
	final double R = 0.5 * p.distance(q);
	if (R < (3.0 + 2.0 * SQRT2)) {
		return(Double.MAX_VALUE);
	}
	final double r = R / SQRT2;
	final double x0 = 0.5 * (p.x + q.x);
	final double y0 = 0.5 * (p.y + q.y);
	final int w = ip.getWidth();
	final int h = ip.getHeight();
	final int xmin = Math.max((int)Math.floor(x0 - R - 1.0), 0);
	final int xmax = Math.min((int)Math.ceil(x0 + R + 1.0), w - 1);
	final int ymin = Math.max((int)Math.floor(y0 - R - 1.0), 0);
	final int ymax = Math.min((int)Math.ceil(y0 + R + 1.0), h - 1);
	if ((xmax <= xmin) || (ymax <= ymin)) {
		return(Double.MAX_VALUE);
	}
	for (int y = ymin; (y <= ymax); y++) {
		final double dy2 = ((double)y - y0) * ((double)y - y0);
		for (int x = xmin; (x <= xmax); x++) {
			final double d = Math.sqrt(
				((double)x - x0) * ((double)x - x0) + dy2);
			if (d < (r - 0.5 * SQRT2)) {
				c -= ip.getPixelValue(x, y);
				continue;
			}
			if (d < (r + 0.5 * SQRT2)) {
				c += SQRT2 * (d - r) * ip.getPixelValue(x, y);
				continue;
			}
			if (d < (R - 1.0)) {
				c += ip.getPixelValue(x, y);
				continue;
			}
			if (d < (R + 1.0)) {
				c += 0.5 * (R + 1.0 - d) * ip.getPixelValue(x, y);
			}
		}
	}
	return(c / (4.0 * R * R));
} /* end contrast */

/*------------------------------------------------------------------*/
private Point2D.Double[] contrastGradient (
	final Snake2DNode p,
	final Snake2DNode q
) {
	final Point2D.Double[] gradient = new Point2D.Double[2];
	gradient[0] = new Point2D.Double(0.0, 0.0);
	gradient[1] = new Point2D.Double(0.0, 0.0);
	final double R = 0.5 * p.distance(q);
	if (R < (3.0 + 2.0 * SQRT2)) {
		return(gradient);
	}
	final double R2 = R * R;
	final double r = R / SQRT2;
	final double x0 = 0.5 * (p.x + q.x);
	final double y0 = 0.5 * (p.y + q.y);
	final double gx = p.x - q.x;
	final double gy = p.y - q.y;
	final int w = ip.getWidth();
	final int h = ip.getHeight();
	final int xmin = Math.max((int)Math.floor(x0 - R - 1.0), 0);
	final int xmax = Math.min((int)Math.ceil(x0 + R + 1.0), w - 1);
	final int ymin = Math.max((int)Math.floor(y0 - R - 1.0), 0);
	final int ymax = Math.min((int)Math.ceil(y0 + R + 1.0), h - 1);
	if ((xmax <= xmin) || (ymax <= ymin)) {
		return(gradient);
	}
	for (int y = ymin; (y <= ymax); y++) {
		final double dy2 = ((double)y - y0) * ((double)y - y0);
		for (int x = xmin; (x <= xmax); x++) {
			final double d = Math.sqrt(
				((double)x - x0) * ((double)x - x0) + dy2);
			if (d < SQRT_TINY) {
				continue;
			}
			final double f = ip.getPixelValue(x, y);
			if (d < (r - 0.5 * SQRT2)) {
				gradient[0].x += f * 0.5 * gx / R2;
				gradient[0].y += f * 0.5 * gy / R2;
				gradient[1].x -= f * 0.5 * gx / R2;
				gradient[1].y -= f * 0.5 * gy / R2;
				continue;
			}
			final double dx = (x0 - (double)x) / d;
			final double dy = (y0 - (double)y) / d;
			final double g1 = 0.25 / R - d / (SQRT2 * R2);
			if (d < (r + 0.5 * SQRT2)) {
				gradient[0].x += f * (dx / SQRT2 + gx * g1);
				gradient[0].y += f * (dy / SQRT2 + gy * g1);
				gradient[1].x += f * (dx / SQRT2 - gx * g1);
				gradient[1].y += f * (dy / SQRT2 - gy * g1);
				continue;
			}
			if (d < (R - 1.0)) {
				gradient[0].x -= f * 0.5 * gx / R2;
				gradient[0].y -= f * 0.5 * gy / R2;
				gradient[1].x += f * 0.5 * gx / R2;
				gradient[1].y += f * 0.5 * gy / R2;
				continue;
			}
			final double g2 = 0.5 / R - (d - 1.0) / R2;
			if (d < (R + 1.0)) {
				gradient[0].x -= 0.25 * f * (dx + gx * g2);
				gradient[0].y -= 0.25 * f * (dy + gy * g2);
				gradient[1].x -= 0.25 * f * (dx - gx * g2);
				gradient[1].y -= 0.25 * f * (dy - gy * g2);
			}
		}
	}
	gradient[0].x /= 4.0 * R2;
	gradient[0].y /= 4.0 * R2;
	gradient[1].x /= 4.0 * R2;
	gradient[1].y /= 4.0 * R2;
	return(gradient);
} /* end contrastGradient */

/*------------------------------------------------------------------*/
private Point2D.Double[] plus (
	final Point2D.Double[] gc,
	final Point2D.Double[] gr
) {
	final int K = gc.length;
	if (K != gr.length) {
		return(null);
	}
	final Point2D.Double[] g = new Point2D.Double[K];
	for (int k = 0; (k < K); k++) {
		g[k] = new Point2D.Double(gc[k].x + gr[k].x, gc[k].y + gr[k].y);
	}
	return(g);
} /* end plus */

/*------------------------------------------------------------------*/
private double regularization (
	final Snake2DNode p,
	final Snake2DNode q
) {
	final double D2 = p.distanceSq(q);
	if (D2 < TINY) {
		return(0.0);
	}
	final double dy = p.y - q.y;
	return(REGULARIZATION_WEIGHT * dy * dy / D2);
} /* end regularization */

/*------------------------------------------------------------------*/
private Point2D.Double[] regularizationGradient (
	final Snake2DNode p,
	final Snake2DNode q
) {
	final Point2D.Double[] gradient = new Point2D.Double[2];
	gradient[0] = new Point2D.Double(0.0, 0.0);
	gradient[1] = new Point2D.Double(0.0, 0.0);
	final double D2 = p.distanceSq(q);
	if (D2 < TINY) {
		return(gradient);
	}
	final double dx = p.x - q.x;
	final double dx2 = dx * dx;
	final double dy = p.y - q.y;
	final double dy2 = dy * dy;
	final double w = 2.0 * REGULARIZATION_WEIGHT / (D2 * D2);
	gradient[0].x = -w * dx * dy2;
	gradient[0].y = w * dx2 * dy;
	gradient[1].x = w * dx * dy2;
	gradient[1].y = -w * dx2 * dy;
	return(gradient);
} /* end regularizationGradient */

} /* end class MySnakuscule */
