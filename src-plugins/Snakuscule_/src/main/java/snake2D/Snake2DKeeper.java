/*====================================================================
| Version: October 17, 2008
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

package snake2D;

// snake2D
import snake2D.Snake2DNode;

// ImageJ
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

// Java 1.1
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.LineMetrics;

// Java 1.2
import java.awt.geom.Point2D;

// Java 1.5
import java.util.Vector;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

/*====================================================================
|	Snake2DKeeper
\===================================================================*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This class encapsulates the interactive and managerial aspects of
 snakes. It handles objects that implement the <code>Snake2D</code>
 interface.
 @see Snake2D
 ********************************************************************/
public class Snake2DKeeper

{ /* begin class Snake2DKeeper */

/*....................................................................
	private variables
....................................................................*/

private Double energy = null;
private ImagePlus display = null;
private Snake2D snake = null;
private snake2DEditToolbar tb = null;
private snake2DSkinHandler sh = null;
private boolean optimalSnakeFound = false;
private boolean optimizing = false;
private static final double COMPLEMENTARY_GOLDEN_RATIO = 1.5 - sqrt(1.25);
private static final double GOLDEN_RATIO = 0.5 + sqrt(1.25);
private static final double MAXIMAL_PARABOLIC_EXCURSION = 100.0;
private static final double SQRT_TINY =
	sqrt((double)Float.intBitsToFloat((int)0x33FFFFFF));
private static final double TINY =
	(double)Float.intBitsToFloat((int)0x33FFFFFF);

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This method allows the user to alternate between interactive guiding
 of the snake and automatic optimization under control of the class
 <code>Snake2DKeeper</code>.
 @param display A mandatory <code>ImagePlus</code> object over which
 the handles used to interactively manipulate the snake will be
 overlaid. The skin used to represent the snake will be overlaid on
 the same image.
 ********************************************************************/
public void interactAndOptimize (
	final Snake2D snake,
	final ImagePlus display
) {
	this.snake = snake;
	this.display = display;
	if (snake == null) {
		return;
	}
	optimalSnakeFound = false;
	energy = null;
	if (display == null) {
		return;
	}
	final Snake2DNode[] youngSnake = snake.getNodes();
	final int K = youngSnake.length;
	final Snake2DNode[] X = new Snake2DNode[K];
	for (int k = 0; (k < K); k++) {
		X[k] = new Snake2DNode(youngSnake[k].x, youngSnake[k].y,
			youngSnake[k].frozen, youngSnake[k].hidden);
	}
	tb = new snake2DEditToolbar(Toolbar.getInstance(), this);
	final snake2DPointHandler ph = new snake2DPointHandler(display, snake, X,
		tb);
	final snake2DPointAction pa = new snake2DPointAction(display, ph, tb, this);
	ph.setPointAction(pa);
	this.display = display;
	display.setRoi(ph);
	while (true) {
		try{
			synchronized(this) {
				wait();
			}
		} catch (InterruptedException e) {
		}
		switch (tb.getCurrentTool()) {
			case snake2DPointAction.DONE: {
				tb.terminateInteraction(snake, ph.getPoints());
				cleanup();
				return;
			}
			case snake2DPointAction.START: {
				sh = new snake2DSkinHandler(display, snake);
				sh.activateDisplay();
				optimize(X);
				break;
			}
		}
		if (!optimizing) {
			if (tb.getCurrentTool() == snake2DPointAction.DONE) {
				tb.terminateInteraction(snake, ph.getPoints());
				cleanup();
				return;
			}
			stopOptimizing(ph);
			IJ.showStatus("Optimization interrupted");
			continue;
		}
		stopOptimizing(ph);
		IJ.showStatus("Optimization completed");
	}
} /* end interactAndOptimize */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected synchronized void destroyOptimality (
) {
	optimalSnakeFound = false;
	energy = null;
} /* end destroyOptimality */

/*------------------------------------------------------------------*/
protected synchronized boolean isOptimizing (
) {
	return(optimizing);
} /* end isOptimizing */

/*------------------------------------------------------------------*/
protected synchronized void startOptimizing (
) {
	optimizing = true;
} /* end startOptimizing */

/*------------------------------------------------------------------*/
protected synchronized void stopOptimizing (
	final snake2DPointHandler ph
) {
	optimizing = false;
	tb.setTool(snake2DPointAction.MOVE_CROSS);
	if (display != null) {
		display.setRoi(ph);
	}
} /* end stopOptimizing */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void cleanup (
) {
	optimizing = false;
	sh = null;
	if (display != null) {
		display.killRoi();
		display = null;
	}
} /* end cleanup */

/*------------------------------------------------------------------*/
private Double f (
	final Snake2DNode[] X,
	final double u,
	final Point2D.Double[] V
) {
	final int K = X.length;
	final Snake2DNode[] Y = new Snake2DNode[K];
	for (int k = 0; (k < K); k++) {
		Y[k] = new Snake2DNode(X[k].x, X[k].y);
		Y[k].x += u * V[k].x;
		Y[k].y += u * V[k].y;
	}
	snake.setNodes(Y);
	if (!optimizing) {
		return(null);
	}
	if (display != null) {
		display.setRoi(sh);
	}
	return(new Double(snake.energy()));
} /* end f */

/*------------------------------------------------------------------*/
private Point2D.Double[] g (
	final Snake2DNode[] X
) {
	final int K = X.length;
	snake.setNodes(X);
	if (!optimizing) {
		return(null);
	}
	Point2D.Double[] G = snake.getEnergyGradient();
	if (G != null) {
		final Point2D.Double[] G0 = new Point2D.Double[K];
		for (int k = 0; (k < K); k++) {
			if (X[k].frozen) {
				G0[k] = new Point2D.Double(0.0, 0.0);
			}
			else {
				G0[k] = new Point2D.Double(G[k].x, G[k].y);
			}
		}
		G = G0;
	}
	else {
		final Snake2DNode[] Y = new Snake2DNode[K];
		G = new Point2D.Double[K];
		for (int k = 0; (k < K); k++) {
			Y[k] = new Snake2DNode(X[k].x, X[k].y);
			G[k] = new Point2D.Double(0.0, 0.0);
		}
		for (int k = 0; (k < K); k++) {
			if (!X[k].frozen) {
				Y[k].x = X[k].x - SQRT_TINY;
				snake.setNodes(Y);
				if (!optimizing) {
					return(null);
				}
				double f0 = snake.energy();
				Y[k].x = X[k].x + SQRT_TINY;
				snake.setNodes(Y);
				if (!optimizing) {
					return(null);
				}
				double f1 = snake.energy();
				G[k].x = 0.5 * (f1 - f0) / SQRT_TINY;
				Y[k].x = X[k].x;
				Y[k].y = X[k].y - SQRT_TINY;
				snake.setNodes(Y);
				if (!optimizing) {
					return(null);
				}
				f0 = snake.energy();
				Y[k].y = X[k].y + SQRT_TINY;
				snake.setNodes(Y);
				if (!optimizing) {
					return(null);
				}
				f1 = snake.energy();
				G[k].y = 0.5 * (f1 - f0) / SQRT_TINY;
				Y[k].y = X[k].y;
			}
		}
		snake.setNodes(X);
	}
	return(G);
} /* end g */

/*------------------------------------------------------------------*/
private double lineMinimization (
	final Snake2DNode[] X,
	final Point2D.Double[] V
) {
	final int K = X.length;
	double a = 0.0;
	final Double Fa = f(X, a, V);
	if (Fa == null) {
		return(-1.0);
	}
	if (energy == null) {
		energy = Fa;
	}
	else {
		energy = (energy.compareTo(Fa) < 0) ? (energy) : (Fa);
	}
	double fa = Fa.doubleValue();
	if (!optimizing) {
		return(-1.0);
	}
	final Snake2DScale[] Pa = snake.getScales();
	double b = SQRT_TINY;
	final Double Fb = f(X, b, V);
	if (Fb == null) {
		return(-1.0);
	}
	energy = (energy.compareTo(Fb) < 0) ? (energy) : (Fb);
	double fb = Fb.doubleValue();
	if (!optimizing) {
		if (fb < fa) {
			for (int k = 0; (k < K); k++) {
				X[k].x += b * V[k].x;
				X[k].y += b * V[k].y;
			}
		}
		return(-1.0);
	}
	final Snake2DScale[] Pb = snake.getScales();
	if (fa < fb) {
		if (display != null) {
			sh.setBestSkin(Pa);
		}
		final double z = a;
		a = b;
		b = z;
		double f = fa;
		fa = fb;
		fb = f;
	}
	else {
		if (display != null) {
			sh.setBestSkin(Pb);
		}
	}
	double c = b + GOLDEN_RATIO * (b - a);
	Double Fc = f(X, c, V);
	if (Fc == null) {
		for (int k = 0; (k < K); k++) {
			X[k].x += b * V[k].x;
			X[k].y += b * V[k].y;
		}
		return(-1.0);
	}
	energy = (energy.compareTo(Fc) < 0) ? (energy) : (Fc);
	double fc = Fc.doubleValue();
	if (fc < fb) {
		if (!optimizing) {
			for (int k = 0; (k < K); k++) {
				X[k].x += c * V[k].x;
				X[k].y += c * V[k].y;
			}
			return(-1.0);
		}
		if (display != null) {
			sh.setBestSkin(snake.getScales());
		}
	}
	double u = c;
	double fu = fc;
	while (fc <= fb) {
		final double r = (b - a) * (fb - fc);
		final double q = (b - c) * (fb - fa);
		u = 0.5 * (b - (b - c) * q + (b - a) * r);
		u = (TINY < abs(q - r))
			? (u / (q - r)) : ((r < q) ? (u / TINY) : (-u / TINY));
		final double ulim = b + MAXIMAL_PARABOLIC_EXCURSION * (c - b);
		if (0.0 < ((b - u) * (u - c))) {
			Double Fu = f(X, u, V);
			if (Fu == null) {
				for (int k = 0; (k < K); k++) {
					X[k].x += c * V[k].x;
					X[k].y += c * V[k].y;
				}
				return(-1.0);
			}
			energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
			fu = Fu.doubleValue();
			if (fu < fc) {
				if (!optimizing) {
					for (int k = 0; (k < K); k++) {
						X[k].x += u * V[k].x;
						X[k].y += u * V[k].y;
					}
					return(-1.0);
				}
				if (display != null) {
					sh.setBestSkin(snake.getScales());
				}
				a = b;
				fa = fb;
				b = u;
				fb = fu;
				break;
			}
			else {
				if (fb < fu) {
					c = u;
					fc = fu;
					break;
				}
			}
			u = c + GOLDEN_RATIO * (c - b);
			Fu = f(X, u, V);
			if (Fu == null) {
				for (int k = 0; (k < K); k++) {
					X[k].x += c * V[k].x;
					X[k].y += c * V[k].y;
				}
				return(-1.0);
			}
			energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
			fu = Fu.doubleValue();
			if (fu < fc) {
				if (!optimizing) {
					for (int k = 0; (k < K); k++) {
						X[k].x += u * V[k].x;
						X[k].y += u * V[k].y;
					}
					return(-1.0);
				}
				if (display != null) {
					sh.setBestSkin(snake.getScales());
				}
			}
		}
		else {
			if (0.0 < ((c - u) * (u - ulim))) {
				Double Fu = f(X, u, V);
				if (Fu == null) {
					for (int k = 0; (k < K); k++) {
						X[k].x += c * V[k].x;
						X[k].y += c * V[k].y;
					}
					return(-1.0);
				}
				energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
				fu = Fu.doubleValue();
				if (fu < fc) {
					if (!optimizing) {
						for (int k = 0; (k < K); k++) {
							X[k].x += u * V[k].x;
							X[k].y += u * V[k].y;
						}
						return(-1.0);
					}
					if (display != null) {
						sh.setBestSkin(snake.getScales());
					}
					b = c;
					c = u;
					u = c + GOLDEN_RATIO * (c - b);
					fb = fc;
					fc = fu;
					Fu = f(X, u, V);
					if (Fu == null) {
						for (int k = 0; (k < K); k++) {
							X[k].x += c * V[k].x;
							X[k].y += c * V[k].y;
						}
						return(-1.0);
					}
					energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
					fu = Fu.doubleValue();
					if (fu < fc) {
						if (!optimizing) {
							for (int k = 0; (k < K); k++) {
								X[k].x += u * V[k].x;
								X[k].y += u * V[k].y;
							}
							return(-1.0);
						}
						if (display != null) {
							sh.setBestSkin(snake.getScales());
						}
					}
				}
			}
			else {
				if (0.0 <= ((u - ulim) * (ulim - c))) {
					u = ulim;
					Double Fu = f(X, u, V);
					if (Fu == null) {
						for (int k = 0; (k < K); k++) {
							X[k].x += c * V[k].x;
							X[k].y += c * V[k].y;
						}
						return(-1.0);
					}
					energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
					fu = Fu.doubleValue();
				}
				else {
					u = c + GOLDEN_RATIO * (c - b);
					Double Fu = f(X, u, V);
					if (Fu == null) {
						for (int k = 0; (k < K); k++) {
							X[k].x += c * V[k].x;
							X[k].y += c * V[k].y;
						}
						return(-1.0);
					}
					energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
					fu = Fu.doubleValue();
				}
				if (fu < fc) {
					if (!optimizing) {
						for (int k = 0; (k < K); k++) {
							X[k].x += u * V[k].x;
							X[k].y += u * V[k].y;
						}
						return(-1.0);
					}
					if (display != null) {
						sh.setBestSkin(snake.getScales());
					}
				}
			}
		}
		a = b;
		b = c;
		c = u;
		fa = fb;
		fb = fc;
		fc = fu;
	}
	double d = 0.0;
	double e = 0.0;
	double x = b;
	double v = b;
	double w = b;
	double fx = fb;
	double fv = fb;
	double fw = fb;
	if (c < a) {
		b = a;
		a = c;
		fb = fa;
		fa = fc;
	}
	else {
		b = c;
		fb = fc;
	}
	while (true) {
		final double xm = 0.5 * (a + b);
		final double tol1 = SQRT_TINY * abs(x) + TINY;
		final double tol2 = 2.0 * tol1;
		if (abs(x - xm) <= (tol2 - 0.5 * (b - a))) {
			double dx = 0.0;
			for (int k = 0; (k < K); k++) {
				X[k].x += x * V[k].x;
				X[k].y += x * V[k].y;
				dx += V[k].x * V[k].x + V[k].y * V[k].y;
			}
			return(abs(x) * sqrt(dx));
		}
		if (tol1 < abs(e)) {
			final double r = (x - w) * (fx - fv);
			double q = (x - v) * (fx - fw);
			double p = (x - v) * q - (x - w) * r;
			q = 2.0 * (q - r);
			if (0.0 < q) {
				p = -p;
			}
			q = abs(q);
			final double etemp = e;
			e = d;
			if ((abs(0.5 * q * etemp) <= abs(p))
				|| (p <= (q * (a - x))) || ((q * (b - x)) <= p)) {
				e = (xm <= x) ? (a - x) : (b - x);
				d = COMPLEMENTARY_GOLDEN_RATIO * e;
			}
			else {
				d = p / q;
				u = x + d;
				if (((u - a) < tol2) || ((b - u) < tol2)) {
					d = (x <= xm) ? (tol1) : (-tol1);
				}
			}
		}
		else {
			e = (xm <= x) ? (a - x) : (b - x);
			d = COMPLEMENTARY_GOLDEN_RATIO * e;
		}
		u = (tol1 <= abs(d))
			? (x + d) : (x + ((0.0 <= d) ? (tol1) : (-tol1)));
		final Double Fu = f(X, u, V);
		if (Fu == null) {
			for (int k = 0; (k < K); k++) {
				X[k].x += x * V[k].x;
				X[k].y += x * V[k].y;
			}
			return(-1.0);
		}
		energy = (energy.compareTo(Fu) < 0) ? (energy) : (Fu);
		fu = Fu.doubleValue();
		if (fu <= fx) {
			if (!optimizing) {
				for (int k = 0; (k < K); k++) {
					X[k].x += u * V[k].x;
					X[k].y += u * V[k].y;
				}
				return(-1.0);
			}
			if (display != null) {
				sh.setBestSkin(snake.getScales());
			}
			if (x <= u) {
				a = x;
			}
			else {
				b = x;
			}
			v = w;
			fv = fw;
			w = x;
			fw = fx;
			x = u;
			fx = fu;
		}
		else {
			if (u < x) {
				a = u;
			}
			else {
				b = u;
			}
			if ((fu <= fw) || (w == x)) {
				v = w;
				fv = fw;
				w = u;
				fw = fu;
			}
			else {
				if ((fu <= fv) || (v == x) || (v == w)) {
					v = u;
					fv = fu;
				}
			}
		}
	}
} /* end lineMinimization */

/*------------------------------------------------------------------*/
private void optimize (
	final Snake2DNode[] X
) {
	optimalSnakeFound = false;
	final int K = X.length;
	final Point2D.Double[] V = new Point2D.Double[K];
	for (int k = 0; (k < K); k++) {
		V[k] = new Point2D.Double(0.0, 0.0);
	}
	Point2D.Double[] G0 = g(X);
	if (G0 == null) {
		return;
	}
	double totalDisplacement;
	do {
		double g0 = 0.0;
		for (int k = 0; (k < K); k++) {
			V[k].x = -G0[k].x;
			V[k].y = -G0[k].y;
			g0 += G0[k].x * G0[k].x + G0[k].y * G0[k].y;
		}
		if (g0 <= SQRT_TINY) {
			snake.setNodes(X);
			optimalSnakeFound = true;
			break;
		}
		totalDisplacement = 0.0;
		for (int n = 0, N = 2 * K; (n <= N); n++) {
			final double dx = lineMinimization(X, V);
			if (dx < 0.0) {
				snake.setNodes(X);
				return;
			}
			totalDisplacement += dx;
			Point2D.Double[] G1 = g(X);
			if (G1 == null) {
				snake.setNodes(X);
				return;
			}
			double g1 = 0.0;
			double b = 0.0;
			for (int k = 0; (k < K); k++) {
				b += G1[k].x * (G1[k].x - G0[k].x)
					+ G1[k].y * (G1[k].y - G0[k].y);
				g1 += G1[k].x * G1[k].x + G1[k].y * G1[k].y;
			}
			if (g1 <= SQRT_TINY) {
				snake.setNodes(X);
				optimalSnakeFound = true;
				return;
			}
			else {
				b /= g0;
				double v = 0.0;
				for (int k = 0; (k < K); k++) {
					V[k].x = b * V[k].x - G1[k].x;
					V[k].y = b * V[k].y - G1[k].y;
					v += V[k].x * V[k].x + V[k].y * V[k].y;
				}
				if (v <= SQRT_TINY) {
					snake.setNodes(X);
					optimalSnakeFound = true;
					return;
				}
				g0 = g1;
				G0 = G1;
			}
		}
	} while (SQRT_TINY < totalDisplacement);
	snake.setNodes(X);
	optimalSnakeFound = true;
} /* end optimize */

} /* end class Snake2DKeeper */

/*====================================================================
|	snake2DEditToolbar
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DEditToolbar
	extends
		Canvas
	implements
		AdjustmentListener,
		MouseListener,
		MouseMotionListener

{ /* begin class snake2DEditToolbar */

/*....................................................................
	private variables
....................................................................*/

private Graphics g = null;
private ImagePlus display = null;
private	Scrollbar scrollbar = null;
private Snake2DKeeper keeper = null;
private Toolbar previousInstance = null;
private final boolean[] down = new boolean[TOOLS];
private int currentTool = snake2DPointAction.MOVE_CROSS;
private int x = 0;
private int xOffset = 0;
private int y = 0;
private int yOffset = 0;
private snake2DPointAction pa = null;
private snake2DPointHandler ph = null;
private snake2DEditToolbar instance = null;
private static final int OFFSET = 3;
private static final int TOOL_SIZE = 22;
private static final int TOOLS = 20;
private static final Color gray = Color.lightGray;
private static final Color brighter = gray.brighter();
private static final Color darker = gray.darker();
private static final Color evenDarker = darker.darker();

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DEditToolbar (
	final Toolbar previousToolbar,
	final Snake2DKeeper keeper
) {
	previousInstance = previousToolbar;
	this.keeper = keeper;
	instance = this;
	final Container container = previousToolbar.getParent();
	final Component component[] = container.getComponents();
	for (int n = 0, N = component.length; (n < N); n++) {
		if (component[n] == previousToolbar) {
			container.remove(previousToolbar);
			container.add(this, n);
			break;
		}
	}
	resetButtons();
	down[currentTool] = true;
	setForeground(evenDarker);
	setBackground(gray);
	addMouseListener(this);
	addMouseMotionListener(this);
	container.validate();
} /* end snake2DEditToolbar */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected int getCurrentTool (
) {
	return(currentTool);
} /* getCurrentTool */

/*------------------------------------------------------------------*/
protected void installListeners (
	snake2DPointAction pa
) {
	this.pa = pa;
	final ImageWindow iw = display.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.removeKeyListener(IJ.getInstance());
	ic.removeKeyListener(IJ.getInstance());
	ic.removeMouseListener(ic);
	ic.removeMouseMotionListener(ic);
	ic.addMouseMotionListener(pa);
	ic.addMouseListener(pa);
	ic.addKeyListener(pa);
	iw.addKeyListener(pa);
	if (display.getWindow() instanceof StackWindow) {
		StackWindow sw = (StackWindow)display.getWindow();
		final Component component[] = sw.getComponents();
		for (int n = 0, N = component.length; (n < N); n++) {
			if (component[n] instanceof Scrollbar) {
				scrollbar = (Scrollbar)component[n];
				scrollbar.addAdjustmentListener(this);
			}
		}
	}
	else {
		scrollbar = null;
	}
} /* end installListeners */

/*------------------------------------------------------------------*/
protected void setTool (
	final int tool
) {
	if (tool == currentTool) {
		return;
	}
	down[tool] = true;
	down[currentTool] = false;
	final Graphics g = getGraphics();
	drawButton(g, currentTool);
	drawButton(g, tool);
	switch (tool) {
		case snake2DPointAction.MOVE_CROSS: {
			if (currentTool != snake2DPointAction.START) {
				drawButton(g, snake2DPointAction.START);
				drawButton(g, snake2DPointAction.RECORD);
			}
			break;
		}
		case snake2DPointAction.RECORD: {
			break;
		}
		case snake2DPointAction.START: {
			if (currentTool != snake2DPointAction.MOVE_CROSS) {
				drawButton(g, snake2DPointAction.MOVE_CROSS);
				drawButton(g, snake2DPointAction.RECORD);
			}
			break;
		}
	}
	update(g);
	g.dispose();
	currentTool = tool;
	display.setRoi(ph);
} /* end setTool */

/*------------------------------------------------------------------*/
protected void setWindow (
	final snake2DPointHandler ph,
	final ImagePlus display
) {
	this.ph = ph;
	this.display = display;
} /* end setWindow */

/*------------------------------------------------------------------*/
protected void terminateInteraction (
	final Snake2D snake,
	final Snake2DNode[] point
) {
	cleanUpListeners();
	restorePreviousToolbar();
	Toolbar.getInstance().repaint();
	display.killRoi();
	snake.setNodes(point);
} /* end terminateInteraction */

/*....................................................................
	AdjustmentListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public synchronized void adjustmentValueChanged (
	AdjustmentEvent e
) {
	display.setRoi(ph);
} /* adjustmentValueChanged */

/*....................................................................
	Canvas methods
....................................................................*/

/*------------------------------------------------------------------*/
public void paint (
	final Graphics g
) {
	for (int i = 0; (i < TOOLS); i++) {
		drawButton(g, i);
	}
} /* paint */

/*....................................................................
	MouseListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseClicked (
	final MouseEvent e
) {
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mousePressed (
	final MouseEvent e
) {
	final int x = e.getX() - 2;
	final int y = e.getY() + 2;
	final int previousTool = currentTool;
	int newTool = snake2DPointAction.UNDEFINED;
	for (int i = 0; (i < TOOLS); i++) {
		if (((i * TOOL_SIZE) <= x) && (x < (i * TOOL_SIZE + TOOL_SIZE))) {
			newTool = i;
		}
	}
	switch (newTool) {
		case snake2DPointAction.DONE: {
			if (keeper.isOptimizing()) {
				keeper.stopOptimizing(ph);
				setTool(newTool);
				IJ.showStatus("Optimization interrupted");
			}
			else {
				setTool(newTool);
				showMessage(newTool);
				synchronized(keeper) {
					keeper.notify();
				}
			}
			break;
		}
		case snake2DPointAction.MAGNIFIER: {
			setTool(newTool);
			showMessage(newTool);
			break;
		}
		case snake2DPointAction.MOVE_CROSS: {
			if (keeper.isOptimizing()) {
				keeper.stopOptimizing(ph);
				IJ.showStatus("Optimization interrupted");
			}
			else {
				setTool(newTool);
				showMessage(newTool);
			}
			break;
		}
		case snake2DPointAction.RECORD: {
			if (!keeper.isOptimizing()) {
				setTool(newTool);
				showMessage(newTool);
				final snake2DRecord recordDialog
					= new snake2DRecord(IJ.getInstance(), ph);
				GUI.center(recordDialog);
				recordDialog.setVisible(true);
				setTool(previousTool);
				recordDialog.dispose();
			}
			else {
				setTool(newTool);
				showMessage(snake2DPointAction.UNDEFINED);
			}
			break;
		}
		case snake2DPointAction.START: {
			if (!keeper.isOptimizing()) {
				keeper.startOptimizing();
				setTool(newTool);
				IJ.showStatus("Optimization started");
				synchronized(keeper) {
					keeper.notify();
				}
			}
			else {
				setTool(newTool);
				showMessage(newTool);
			}
			break;
		}
		case snake2DPointAction.UNDEFINED: {
			showMessage(newTool);
			break;
		}
		default: {
			setTool(newTool);
			showMessage(newTool);
			break;
		}
	}
} /* mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*....................................................................
	MouseMotionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseDragged (
	final MouseEvent e
) {
} /* end mouseDragged */

/*------------------------------------------------------------------*/
public void mouseMoved (
	final MouseEvent e
) {
	final int x = e.getX() - 2;
	final int y = e.getY() + 2;
	int newTool = snake2DPointAction.UNDEFINED;
	for (int i = 0; (i < TOOLS); i++) {
		if (((i * TOOL_SIZE) <= x) && (x < (i * TOOL_SIZE + TOOL_SIZE))) {
			newTool = i;
		}
	}
	switch (newTool) {
		case snake2DPointAction.DONE: {
			if (keeper.isOptimizing()) {
				IJ.showStatus("Stop optimizing and return to ImageJ");
			}
			else {
				IJ.showStatus("Return to ImageJ");
			}
			break;
		}
		case snake2DPointAction.MAGNIFIER: {
			IJ.showStatus("Magnify (<ALT>  to minify)");
			break;
		}
		case snake2DPointAction.MOVE_CROSS: {
			if (keeper.isOptimizing()) {
				IJ.showStatus("Interrupt the optimization");
			}
			else {
				IJ.showStatus("Reshape (<SHIFT> to translate)");
			}
			break;
		}
		case snake2DPointAction.RECORD: {
			if (!keeper.isOptimizing()) {
				IJ.showStatus("Table (add with <SPACE>, remove with <BACK>)");
			}
			break;
		}
		case snake2DPointAction.START: {
			if (!keeper.isOptimizing()) {
				IJ.showStatus("Optimize (<ENTER> or <CARRIAGE RETURN>)");
			}
			break;
		}
	}
} /* end mouseMoved */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void cleanUpListeners (
) {
	if (scrollbar != null) {
		scrollbar.removeAdjustmentListener(this);
	}
	final ImageWindow iw = display.getWindow();
	final ImageCanvas ic = iw.getCanvas();
	iw.removeKeyListener(pa);
	ic.removeKeyListener(pa);
	ic.removeMouseListener(pa);
	ic.removeMouseMotionListener(pa);
	ic.addMouseMotionListener(ic);
	ic.addMouseListener(ic);
	ic.addKeyListener(IJ.getInstance());
	iw.addKeyListener(IJ.getInstance());
} /* end cleanUpListeners */

/*------------------------------------------------------------------*/
private void d (
	int x,
	int y
) {
	x += xOffset;
	y += yOffset;
	g.drawLine(this.x, this.y, x, y);
	this.x = x;
	this.y = y;
} /* end d */

/*------------------------------------------------------------------*/
private void drawButton (
	final Graphics g,
	final int tool
) {
	fill3DRect(g, tool * TOOL_SIZE + 1, 1, TOOL_SIZE, TOOL_SIZE - 1,
		!down[tool]);
	g.setColor(Color.black);
	int x = tool * TOOL_SIZE + OFFSET;
	int y = OFFSET;
	if (down[tool]) {
		x++;
		y++;
	}
	this.g = g;
	switch (tool) {
		case snake2DPointAction.DONE: {
			xOffset = x;
			yOffset = y;
			m(5, 0);
			d(5, 8);
			m(4, 5);
			d(4, 7);
			m(3, 6);
			d(3, 7);
			m(2, 7);
			d(2, 9);
			m(1, 8);
			d(1, 9);
			m(2, 10);
			d(6, 10);
			m(3, 11);
			d(3, 13);
			m(1, 14);
			d(6, 14);
			m(0, 15);
			d(7, 15);
			m(2, 13);
			d(2, 13);
			m(5, 13);
			d(5, 13);
			m(7, 8);
			d(14, 8);
			m(8, 7);
			d(15, 7);
			m(8, 9);
			d(13, 9);
			m(9, 6);
			d(9, 10);
			m(15, 4);
			d(15, 6);
			d(14, 6);
			break;
		}
		case snake2DPointAction.MAGNIFIER: {
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
		}
		case snake2DPointAction.MOVE_CROSS: {
			xOffset = x;
			yOffset = y;
			if (keeper.isOptimizing()) {
				m(2, 3);
				d(13, 3);
				m(2, 4);
				d(13, 4);
				m(2, 5);
				d(13, 5);
				m(2, 6);
				d(13, 6);
				m(2, 7);
				d(13, 7);
				m(2, 8);
				d(13, 8);
				m(2, 9);
				d(13, 9);
				m(2, 10);
				d(13, 10);
				m(2, 11);
				d(13, 11);
				m(2, 12);
				d(13, 12);
				m(2, 13);
				d(13, 13);
			}
			else {
				m(1, 1);
				d(1, 10);
				m(2, 2);
				d(2, 9);
				m(3, 3);
				d(3, 8);
				m(4, 4);
				d(4, 7);
				m(5, 5);
				d(5, 7);
				m(6, 6);
				d(6, 7);
				m(7, 7);
				d(7, 7);
				m(11, 5);
				d(11, 6);
				m(10, 7);
				d(10, 8);
				m(12, 7);
				d(12, 8);
				m(9, 9);
				d(9, 11);
				m(13, 9);
				d(13, 11);
				m(10, 12);
				d(10, 15);
				m(12, 12);
				d(12, 15);
				m(11, 9);
				d(11, 10);
				m(11, 13);
				d(11, 15);
				m(9, 13);
				d(13, 13);
			}
			break;
		}
		case snake2DPointAction.RECORD: {
			if (!keeper.isOptimizing()) {
				xOffset = x;
				yOffset = y;
				m(2, 8);
				d(2, 9);
				m(3, 8);
				d(3, 10);
				m(4, 9);
				d(4, 11);
				m(5, 10);
				d(5, 12);
				m(6, 11);
				d(6, 13);
				m(7, 10);
				d(7, 13);
				m(8, 8);
				d(8, 12);
				m(9, 6);
				d(9, 10);
				m(10, 4);
				d(10, 8);
				m(11, 2);
				d(11, 6);
				m(12, 2);
				d(12, 4);
			}
			break;
		}
		case snake2DPointAction.START: {
			if (!keeper.isOptimizing()) {
				xOffset = x;
				yOffset = y;
				m(2, 3);
				d(3, 3);
				m(2, 4);
				d(5, 4);
				m(2, 5);
				d(7, 5);
				m(2, 6);
				d(9, 6);
				m(2, 7);
				d(11, 7);
				m(2, 8);
				d(13, 8);
				m(2, 9);
				d(11, 9);
				m(2, 10);
				d(9, 10);
				m(2, 11);
				d(7, 11);
				m(2, 12);
				d(5, 12);
				m(2, 13);
				d(3, 13);
			}
			break;
		}
	}
} /* end drawButton */

/*------------------------------------------------------------------*/
private void fill3DRect (
	final Graphics g,
	final int x,
	final int y,
	final int width,
	final int height,
	final boolean raised
) {
	if (raised) {
		g.setColor(gray);
	}
	else {
		g.setColor(darker);
	}
	g.fillRect(x + 1, y + 1, width - 2, height - 2);
	g.setColor((raised) ? (brighter) : (evenDarker));
	g.drawLine(x, y, x, y + height - 1);
	g.drawLine(x + 1, y, x + width - 2, y);
	g.setColor((raised) ? (evenDarker) : (brighter));
	g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
	g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
} /* end fill3DRect */

/*------------------------------------------------------------------*/
private void m (
	final int x,
	final int y
) {
	this.x = xOffset + x;
	this.y = yOffset + y;
} /* end m */

/*------------------------------------------------------------------*/
private void resetButtons (
) {
	for (int i = 0; (i < TOOLS); i++) {
		down[i] = false;
	}
} /* end resetButtons */

/*------------------------------------------------------------------*/
private void restorePreviousToolbar (
) {
	removeMouseMotionListener(this);
	removeMouseListener(this);
	final Container container = instance.getParent();
	final Component component[] = container.getComponents();
	for (int n = 0, N = component.length; (n < N); n++) {
		if (component[n] == instance) {
			container.remove(instance);
			container.add(previousInstance, n);
			container.validate();
			break;
		}
	}
} /* end restorePreviousToolbar */

/*------------------------------------------------------------------*/
private void showMessage (
	final int tool
) {
	switch (tool) {
		case snake2DPointAction.DONE: {
			IJ.showStatus("Done");
			return;
		}
		case snake2DPointAction.MAGNIFIER: {
			IJ.showStatus("Magnifying glass");
			return;
		}
		case snake2DPointAction.MOVE_CROSS: {
			IJ.showStatus("Move crosses");
			return;
		}
		case snake2DPointAction.RECORD: {
			IJ.showStatus("Record snake");
			return;
		}
		default: {
			IJ.showStatus("Undefined operation");
			return;
		}
	}
} /* end showMessage */

} /* end class snake2DEditToolbar */

/*====================================================================
|	snake2DPointAction
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DPointAction
	extends
		ImageCanvas
	implements
		KeyListener

{ /* begin class snake2DPointAction */

/*....................................................................
	private variables
....................................................................*/

private ImagePlus display = null;
private Point mouse = null;
private Snake2DKeeper keeper = null;
private boolean active = false;
private snake2DEditToolbar tb = null;
private snake2DPointHandler ph = null;

/*....................................................................
	protected variables
....................................................................*/

protected static final int DONE = 19;
protected static final int MAGNIFIER = 11;
protected static final int MOVE_CROSS = 0;
protected static final int RECORD = 2;
protected static final int START = 1;
protected static final int UNDEFINED = -1;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DPointAction (
	final ImagePlus display,
	final snake2DPointHandler ph,
	final snake2DEditToolbar tb,
	final Snake2DKeeper keeper
) {
	super(display);
	this.display = display;
	this.ph = ph;
	this.tb = tb;
	this.keeper = keeper;
	tb.setWindow(ph, display);
	tb.installListeners(this);
} /* end snake2DPointAction */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected boolean isActive (
) {
	return(active);
} /* end isActive */

/*....................................................................
	Canvas methods
....................................................................*/

/*------------------------------------------------------------------*/
public void focusGained (
	final FocusEvent e
) {
	active = true;
	display.setRoi(ph);
} /* end focusGained */

/*------------------------------------------------------------------*/
public void focusLost (
	final FocusEvent e
) {
	active = false;
	display.setRoi(ph);
} /* end focusLost */

/*....................................................................
	ImageCanvas methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseClicked (
	final MouseEvent e
) {
	active = true;
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseDragged (
	final MouseEvent e
) {
	active = true;
	if (!keeper.isOptimizing()) {
		final int x = e.getX();
		final int y = e.getY();
		switch (tb.getCurrentTool()) {
			case MOVE_CROSS: {
				if (e.isShiftDown()) {
					double scale =
						display.getWindow().getCanvas().getMagnification();
					scale = (1.0 < scale) ? (1.0 / scale) : (scale);
					final int xScaled = (int)round(round(
						(double)x * scale) / scale);
					final int yScaled = (int)round(round(
						(double)y * scale) / scale);
					ph.translatePoints(xScaled - mouse.x, yScaled - mouse.y);
					mouse.x = xScaled;
					mouse.y = yScaled;
				}
				else {
					ph.movePoint(x, y);
				}
				display.setRoi(ph);
				keeper.destroyOptimality();
				break;
			}
		}
	}
	mouseMoved(e);
} /* end mouseDragged */

/*------------------------------------------------------------------*/
public void mouseEntered (
	final MouseEvent e
) {
	active = true;
	WindowManager.setCurrentWindow(display.getWindow());
	display.getWindow().toFront();
	display.setRoi(ph);
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
	final MouseEvent e
) {
	active = false;
	display.setRoi(ph);
	IJ.showStatus("");
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mouseMoved (
	final MouseEvent e
) {
	active = true;
	final int x = display.getWindow().getCanvas().offScreenX(e.getX());
	final int y = display.getWindow().getCanvas().offScreenY(e.getY());
	IJ.showStatus(display.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end mouseMoved */

/*------------------------------------------------------------------*/
public void mousePressed (
	final MouseEvent e
) {
	active = true;
	final int x = e.getX();
	final int y = e.getY();
	switch (tb.getCurrentTool()) {
		case MAGNIFIER: {
			final int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK))
				!= 0) {
				display.getWindow().getCanvas().zoomOut(x, y);
			}
			else {
				display.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		}
		case MOVE_CROSS: {
			if (e.isShiftDown()) {
				double scale =
					display.getWindow().getCanvas().getMagnification();
				scale = (1.0 < scale) ? (1.0 / scale) : (scale);
				mouse = new Point(
					(int)round(round((double)x * scale) / scale),
					(int)round(round((double)y * scale) / scale));
			}
			else {
				ph.findClosestPoint(x, y);
			}
			break;
		}
	}
	display.setRoi(ph);
} /* end mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
	final MouseEvent e
) {
	active = true;
	mouse = null;
} /* end mouseReleased */

/*....................................................................
	KeyListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void keyPressed (
	final KeyEvent e
) {
	active = true;
	final Point p = ph.getPoint();
	if (p == null) {
		return;
	}
	if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
		if (keeper.isOptimizing()) {
			keeper.stopOptimizing(ph);
			tb.setTool(DONE);
			IJ.showStatus("Optimization interrupted");
		}
		else {
			tb.setTool(DONE);
			IJ.showStatus("Done");
			synchronized(keeper) {
				keeper.notify();
			}
		}
	}
	final int x = p.x;
	final int y = p.y;
	int scaledX;
	int scaledY;
	int scaledShiftedX;
	int scaledShiftedY;
	switch (tb.getCurrentTool()) {
		case MOVE_CROSS: {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_DELETE: {
					ph.unrecordPoints();
					break;
				}
				case KeyEvent.VK_DOWN: {
					scaledX = display.getWindow().getCanvas().screenX(x);
					scaledShiftedY = display.getWindow().getCanvas().screenY(
						y + (int)ceil(1.0
						/ display.getWindow().getCanvas().getMagnification()));
					ph.movePoint(scaledX, scaledShiftedY);
					break;
				}
				case KeyEvent.VK_ENTER: {
					if (!keeper.isOptimizing()) {
						keeper.startOptimizing();
						tb.setTool(snake2DPointAction.START);
						IJ.showStatus("Optimization started");
						synchronized(keeper) {
							keeper.notify();
						}
					}
					break;
				}
				case KeyEvent.VK_SPACE: {
					ph.recordPoints();
					break;
				}
				case KeyEvent.VK_LEFT: {
					scaledShiftedX = display.getWindow().getCanvas().screenX(
						x - (int)ceil(1.0
						/ display.getWindow().getCanvas().getMagnification()));
					scaledY = display.getWindow().getCanvas().screenY(y);
					ph.movePoint(scaledShiftedX, scaledY);
					break;
				}
				case KeyEvent.VK_RIGHT: {
					scaledShiftedX = display.getWindow().getCanvas().screenX(
						x + (int)ceil(1.0
						/ display.getWindow().getCanvas().getMagnification()));
					scaledY = display.getWindow().getCanvas().screenY(y);
					ph.movePoint(scaledShiftedX, scaledY);
					break;
				}
				case KeyEvent.VK_UP: {
					scaledX = display.getWindow().getCanvas().screenX(x);
					scaledShiftedY = display.getWindow().getCanvas().screenY(
						y - (int)ceil(1.0
						/ display.getWindow().getCanvas().getMagnification()));
					ph.movePoint(scaledX, scaledShiftedY);
					break;
				}
			}
			break;
		}
	}
	display.setRoi(ph);
	updateStatus();
} /* end keyPressed */

/*------------------------------------------------------------------*/
public void keyReleased (
	final KeyEvent e
) {
	active = true;
} /* end keyReleased */

/*------------------------------------------------------------------*/
public void keyTyped (
	final KeyEvent e
) {
	active = true;
} /* end keyTyped */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private String getValueAsString (
	final int x,
	final int y
) {
	final Calibration cal = display.getCalibration();
	final int[] v = display.getPixel(x, y);
	switch (display.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16: {
			final double cValue = cal.getCValue(v[0]);
			if (cValue==v[0]) {
				return(", value=" + v[0]);
			}
			else {
				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		}
		case ImagePlus.GRAY32: {
			return(", value=" + Float.intBitsToFloat(v[0]));
		}
		case ImagePlus.COLOR_256: {
			return(", index=" + v[3] + ", value="
				+ v[0] + "," + v[1] + "," + v[2]);
		}
		case ImagePlus.COLOR_RGB: {
			return(", value=" + v[0] + "," + v[1] + "," + v[2]);
		}
		default: {
			return("");
		}
	}
} /* end getValueAsString */

/*------------------------------------------------------------------*/
private void updateStatus (
) {
	final Point p = ph.getPoint();
	if (p == null) {
		IJ.showStatus("");
		return;
	}
	final int x = p.x;
	final int y = p.y;
	IJ.showStatus(display.getLocationAsString(x, y) + getValueAsString(x, y));
} /* end updateStatus */

} /* end class snake2DPointAction */

/*====================================================================
|	snake2DPointHandler
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DPointHandler
	extends
		Roi

{ /* begin class snake2DPointHandler */

/*....................................................................
	private variables
....................................................................*/

private ImagePlus display = null;
private Snake2DNode[] point = null;
private Snake2DScale[] skin = null;
private Snake2D snake = null;
private boolean doLabeling = false;
private boolean firstTableEntry = true;
private boolean multiSkin = false;
private boolean started = false;
private final Vector<Integer> segments = new Vector<Integer>();
private int currentPoint = 0;
private snake2DEditToolbar tb = null;
private snake2DPointAction pa = null;
private static final ResultsTable table = new ResultsTable();
private static final int CROSS_HALFSIZE = 5;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DPointHandler (
	final ImagePlus display,
	final Snake2D snake,
	final Snake2DNode[] point,
	final snake2DEditToolbar tb
) {
	super(0, 0, display.getWidth(), display.getHeight(), display);
	this.display = display;
	this.snake = snake;
	this.point = point;
	this.tb = tb;
	if (point == null) {
		this.point = new Snake2DNode[0];
	}
	clearTable();
} /* end snake2DPointHandler */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected void clearTable (
) {
	table.reset();
	table.disableRowLabels();
	firstTableEntry = true;
	table.show("Snakuscules");
} /* end clearTable */

/*------------------------------------------------------------------*/
protected void findClosestPoint (
	int x,
	int y
) {
	if (point.length == 0) {
		return;
	}
	x = ic.offScreenX(x);
	y = ic.offScreenY(y);
	double distanceSq = point[currentPoint].distanceSq((double)x, (double)y);
	for (int k = 0, K = point.length; (k < K); k++) {
		final double candidateSq = point[k].distanceSq((double)x, (double)y);
		if (candidateSq < distanceSq) {
			distanceSq = candidateSq;
			currentPoint = k;
		}
	}
} /* end findClosestPoint */

/*------------------------------------------------------------------*/
protected Point getPoint (
) {
	return(new Point((int)round(point[currentPoint].getX()),
		(int)round(point[currentPoint].getY())));
} /* end getPoint */

/*------------------------------------------------------------------*/
protected Snake2DNode[] getPoints (
) {
	return(point);
} /* end getPoints */

/*------------------------------------------------------------------*/
protected boolean isTableEmpty (
) {
	return(table.getCounter() == 0);
} /* end isTableEmpty */

/*------------------------------------------------------------------*/
protected void movePoint (
	int x,
	int y
) {
	if (!point[currentPoint].frozen) {
		x = ic.offScreenX(x);
		y = ic.offScreenY(y);
		x = (x < 0) ? (0) : (x);
		x = (display.getWidth() <= x) ? (display.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (display.getHeight() <= y) ? (display.getHeight() - 1) : (y);
		point[currentPoint].setLocation((double)x, (double)y);
	}
} /* end movePoint */

/*------------------------------------------------------------------*/
protected void recordPoints (
) {
	if (firstTableEntry) {
		table.reset();
		table.disableRowLabels();
		firstTableEntry = false;
	}
	table.incrementCounter();
	for (int k = 0, K = point.length; (k < K); k++) {
		table.addValue("X" + k, point[k].x);
		table.addValue("Y" + k, point[k].y);
	}
	table.addValue("J", snake.energy());
	table.show("Snakuscules");
} /* end recordPoints */

/*------------------------------------------------------------------*/
protected void setDoLabeling (
	final boolean doLabeling
) {
	this.doLabeling = doLabeling;
} /* end setDoLabeling */

/*------------------------------------------------------------------*/
protected void setPointAction (
	final snake2DPointAction pa
) {
	this.pa = pa;
	started = true;
	display.setRoi(this);
} /* end setPointAction */

/*------------------------------------------------------------------*/
protected void showAll (
) {
	if (firstTableEntry || isTableEmpty()) {
		return;
	}
	final String headings = table.getColumnHeadings();
	int H = 0;
	int i = headings.indexOf('\t');
	while (0 <= i) {
		H++;
		i = headings.indexOf('\t', ++i);
	}
	H = (H - 2) / 2;
	final int K = table.getCounter();
	final Vector<Snake2DScale> scales = new Vector<Snake2DScale>();
	segments.clear();
	for (int k = 0; (k < K); k++) {
		final Snake2DNode[] node = new Snake2DNode[H];
		for (int h = 0; (h < H); h++) {
			node[h] = new Snake2DNode(table.getValue("X" + h, k),
				table.getValue("Y" + h, k));
		}
		snake.setNodes(node);
		final Snake2DScale[] scale = snake.getScales();
		if (scale == null) {
			continue;
		}
		segments.add(scale.length);
		for (int s = 0, S = scale.length; (s < S); s++) {
			scales.add(scale[s]);
		}
	}
	snake.setNodes(point);
	skin = (Snake2DScale[])scales.toArray(new Snake2DScale[0]);
	multiSkin = true;
	display.setRoi(this);
	final snake2DStamp doneDialog = new snake2DStamp(IJ.getInstance(),
		skin, segments, display, this);
	GUI.center(doneDialog);
	doneDialog.setVisible(true);
	doneDialog.dispose();
	multiSkin = false;
	skin = null;
	imp.updateImage();
} /* end showAll */

/*------------------------------------------------------------------*/
protected void translatePoints (
	int dx,
	int dy
) {
	dx = (int)round((double)dx / ic.getMagnification());
	dy = (int)round((double)dy / ic.getMagnification());
	for (int k = 0, K = point.length; (k < K); k++) {
		if (!point[k].frozen) {
			point[k].setLocation(point[k].x + (double)dx,
				point[k].y + (double)dy);
		}
	}
} /* end translatePoints */

/*------------------------------------------------------------------*/
protected void unrecordPoints (
) {
	if (firstTableEntry) {
		table.reset();
		table.disableRowLabels();
	}
	if (1 <= table.getCounter()) {
		table.deleteRow(table.getCounter() - 1);
	}
	table.show("Snakuscules");
} /* end unrecordPoints */

/*....................................................................
	Roi methods
....................................................................*/

/*------------------------------------------------------------------*/
public void draw (
	final Graphics g
) {
	if (started) {
		final Font font = new Font("SansSerif", Font.PLAIN,
			snake2DStamp.FONT_SIZE);
		final FontMetrics fontMetrics = g.getFontMetrics(font);
		g.setFont(font);
		final double mag = ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		if (!multiSkin) {
			snake.setNodes(point);
			skin = snake.getScales();
		}
		if (skin == null) {
			skin = new Snake2DScale[0];
		}
		int m = 0;
		int M = 0;
		int s = 0;
		int S = 0;
		if ((segments != null) && !segments.isEmpty()) {
			S = segments.elementAt(M);
		}
		double x0 = 0.0;
		double y0 = 0.0;
		for (int k = 0, K = skin.length; (k < K); k++) {
			final Color scaleColor = skin[k].bestAttemptColor;
			if (scaleColor == null) {
				g.setColor(ROIColor);
			}
			else {
				g.setColor(scaleColor);
			}
			final int[] xpoints = skin[k].xpoints;
			final int[] ypoints = skin[k].ypoints;
			final Polygon poly = new Polygon();
			final int N = skin[k].npoints;
			for (int n = 0; (n < N); n++) {
				poly.addPoint(ic.screenX(xpoints[n]) + dx,
					ic.screenY(ypoints[n]) + dy);
				x0 += (double)ic.screenX(xpoints[n]);
				y0 += (double)ic.screenY(ypoints[n]);
			}
			if (skin[k].closed && skin[k].filled) {
				g.fillPolygon(poly);
			}
			else {
				if (skin[k].closed) {
					g.drawPolygon(poly);
				}
				else {
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
			m += N;
			if (S <= ++s) {
				if (doLabeling) {
					final String label = "" + ++M;
					final LineMetrics lineMetrics = fontMetrics.getLineMetrics(
						label, g);
					final double midHeight = 0.5
						* (double)lineMetrics.getHeight();
					final double midWidth = 0.5
						* (double)fontMetrics.stringWidth(label);
					final double descent = (double)fontMetrics.getDescent();
					g.drawString(label,
						(int)round(x0 / (double)m - midWidth),
						(int)round(y0 / (double)m + midHeight - descent));
				}
				m = 0;
				s = 0;
				if ((segments != null) && (M < segments.size())) {
					S = segments.elementAt(M);
				}
				x0 = 0.0;
				y0 = 0.0;
			}
		}
		if (!multiSkin) {
			g.setColor(ROIColor);
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].hidden) {
					final Point p = new Point((int)round(point[k].getX()),
						(int)round(point[k].getY()));
					if (k == currentPoint) {
						if (pa.isActive()) {
							g.drawLine(
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - 1) + dy);
							g.drawLine(
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
							g.drawLine(
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
							g.drawLine(
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - 1) + dy);
							g.drawLine(
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - 1) + dy);
							g.drawLine(
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + 1) + dy);
							g.drawLine(
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + 1) + dy);
							g.drawLine(
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
							g.drawLine(
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
							g.drawLine(
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + 1) + dy);
							g.drawLine(
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + 1) + dy);
							g.drawLine(
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - 1) + dy);
							if (1.0 < ic.getMagnification()) {
								g.drawLine(
									ic.screenX(p.x - CROSS_HALFSIZE) + dx,
									ic.screenY(p.y) + dy,
									ic.screenX(p.x + CROSS_HALFSIZE) + dx,
									ic.screenY(p.y) + dy);
								g.drawLine(
									ic.screenX(p.x) + dx,
									ic.screenY(p.y - CROSS_HALFSIZE) + dy,
									ic.screenX(p.x) + dx,
									ic.screenY(p.y + CROSS_HALFSIZE) + dy);
							}
						}
						else {
							g.drawLine(
								ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
							g.drawLine(
								ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
						}
					}
					else {
						g.drawLine(
							ic.screenX(p.x - CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy,
							ic.screenX(p.x + CROSS_HALFSIZE) + dx,
							ic.screenY(p.y) + dy);
						g.drawLine(
							ic.screenX(p.x) + dx,
							ic.screenY(p.y - CROSS_HALFSIZE) + dy,
							ic.screenX(p.x) + dx,
							ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
				}
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			display.draw();
		}
	}
} /* end draw */

} /* end class snake2DPointHandler */

/*====================================================================
|	snake2DRecord
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DRecord
	extends
		Dialog
	implements
		ActionListener,
		WindowListener

{ /* begin class snake2DRecord */

/*....................................................................
	private variables
....................................................................*/

private snake2DPointHandler ph = null;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DRecord (
	final Frame parentWindow,
	final snake2DPointHandler ph
) {
	super(parentWindow, "Snakuscule Recorder", true);
	this.ph = ph;
	addWindowListener(this);
	setLayout(new GridLayout(0, 1));
	final Button addButton = new Button("Append to Table");
	final Button removeButton = new Button("Remove Last Table Row");
	final Button graphButton = new Button("Graph Table Entries");
	final Button clearButton = new Button("Clear Table");
	final Button cancelButton = new Button("Cancel");
	addButton.addActionListener(this);
	removeButton.addActionListener(this);
	graphButton.addActionListener(this);
	clearButton.addActionListener(this);
	cancelButton.addActionListener(this);
	if (ph.isTableEmpty()) {
		removeButton.setEnabled(false);
		graphButton.setEnabled(false);
		clearButton.setEnabled(false);
	}
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	add(separation1);
	add(addButton);
	add(removeButton);
	add(graphButton);
	add(clearButton);
	add(separation2);
	add(cancelButton);
	pack();
} /* end snake2DRecord */

/*....................................................................
	ActionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
	final ActionEvent ae
) {
	setVisible(false);
	if (ae.getActionCommand().equals("Append to Table")) {
		ph.recordPoints();
	}
	else if (ae.getActionCommand().equals("Remove Last Table Row")) {
		ph.unrecordPoints();
	}
	else if (ae.getActionCommand().equals("Graph Table Entries")) {
		ph.showAll();
	}
	else if (ae.getActionCommand().equals("Clear Table")) {
		ph.clearTable();
	}
	else if (ae.getActionCommand().equals("Cancel")) {
	}
} /* actionPerformed */

/*....................................................................
	Container methods
....................................................................*/

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*....................................................................
	WindowListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void windowActivated (
	final WindowEvent we
) {
} /* end windowActivated */

/*------------------------------------------------------------------*/
public void windowClosed (
	final WindowEvent we
) {
} /* end windowClosed */

/*------------------------------------------------------------------*/
public void windowClosing (
	final WindowEvent we
) {
	setVisible(false);
} /* end windowClosing */

/*------------------------------------------------------------------*/
public void windowDeactivated (
	final WindowEvent we
) {
} /* end windowDeactivated */

/*------------------------------------------------------------------*/
public void windowDeiconified (
	final WindowEvent we
) {
} /* end windowDeiconified */

/*------------------------------------------------------------------*/
public void windowIconified (
	final WindowEvent we
) {
} /* end windowIconified */

/*------------------------------------------------------------------*/
public void windowOpened (
	final WindowEvent we
) {
} /* end windowOpened */

} /* end class snake2DRecord */

/*====================================================================
|	snake2DSkinHandler
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DSkinHandler
	extends
		Roi

{ /* begin class snake2DSkinHandler */

/*....................................................................
	private variables
....................................................................*/

private ImagePlus display = null;
private Snake2DScale[] bestSkin = null;
private Snake2D snake = null;
private boolean started = false;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DSkinHandler (
	final ImagePlus display,
	final Snake2D snake
) {
	super(0, 0, display.getWidth(), display.getHeight(), display);
	this.display = display;
	this.snake = snake;
} /* end snake2DSkinHandler */

/*....................................................................
	protected methods
....................................................................*/

/*------------------------------------------------------------------*/
protected void activateDisplay (
) {
	started = true;
	display.setRoi(this);
} /* end activateDisplay */

/*------------------------------------------------------------------*/
protected void setBestSkin (
	final Snake2DScale[] bestSkin
) {
	if (bestSkin == null) {
		this.bestSkin = null;
		return;
	}
	this.bestSkin = new Snake2DScale[bestSkin.length];
	for (int k = 0, K = bestSkin.length; (k < K); k++) {
		this.bestSkin[k] = new Snake2DScale(bestSkin[k].bestAttemptColor,
			bestSkin[k].currentAttemptColor, bestSkin[k].closed,
			bestSkin[k].filled);
		for (int n = 0, N = bestSkin[k].npoints; (n < N); n++) {
			this.bestSkin[k].addPoint(
				bestSkin[k].xpoints[n], bestSkin[k].ypoints[n]);
		}
	}
} /* end setBestSkin */

/*....................................................................
	Roi methods
....................................................................*/

/*------------------------------------------------------------------*/
public void draw (
	final Graphics g
) {
	if (started) {
		Snake2DScale[] skin = snake.getScales();
		if (skin == null) {
			skin = new Snake2DScale[0];
		}
		final double mag = ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		for (int k = 0, K = skin.length; (k < K); k++) {
			Color scaleColor = skin[k].currentAttemptColor;
			if (scaleColor == null) {
				if (bestSkin == null) {
					g.setColor(ROIColor);
				}
				else {
					final int R = ((int)0x7F + ROIColor.getRed()) & (int)0xFF;
					final int G = ((int)0x7F + ROIColor.getGreen()) & (int)0xFF;
					final int B = ((int)0x7F + ROIColor.getBlue()) & (int)0xFF;
					g.setColor(new Color(R, G, B));
				}
			}
			else {
				if (bestSkin == null) {
					scaleColor = skin[k].bestAttemptColor;
					if (scaleColor == null) {
						g.setColor(ROIColor);
					}
					else {
						g.setColor(scaleColor);
					}
				}
				else {
					g.setColor(scaleColor);
				}
			}
			final int[] xpoints = skin[k].xpoints;
			final int[] ypoints = skin[k].ypoints;
			final Polygon poly = new Polygon();
			for (int n = 0, N = skin[k].npoints; (n < N); n++) {
				poly.addPoint(ic.screenX(xpoints[n]) + dx,
					ic.screenY(ypoints[n]) + dy);
			}
			if (skin[k].closed && skin[k].filled) {
				g.fillPolygon(poly);
			}
			else {
				if (skin[k].closed) {
					g.drawPolygon(poly);
				}
				else {
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
		}
		if (bestSkin != null) {
			for (int k = 0, K = bestSkin.length; (k < K); k++) {
				final Color scaleColor = bestSkin[k].bestAttemptColor;
				if (scaleColor == null) {
					g.setColor(ROIColor);
				}
				else {
					g.setColor(scaleColor);
				}
				final int[] xpoints = bestSkin[k].xpoints;
				final int[] ypoints = bestSkin[k].ypoints;
				final Polygon poly = new Polygon();
				for (int n = 0, N = bestSkin[k].npoints; (n < N); n++) {
					poly.addPoint(ic.screenX(xpoints[n]) + dx,
						ic.screenY(ypoints[n]) + dy);
				}
				if (bestSkin[k].closed && bestSkin[k].filled) {
					g.fillPolygon(poly);
				}
				else {
					if (bestSkin[k].closed) {
						g.drawPolygon(poly);
					}
					else {
						g.drawPolyline(poly.xpoints, poly.ypoints,
							poly.npoints);
					}
				}
			}
		}
		if (updateFullWindow) {
			updateFullWindow = false;
			display.draw();
		}
	}
} /* end draw */

} /* end class snake2DSkinHandler */

/*====================================================================
|	snake2DStamp
\===================================================================*/

/*------------------------------------------------------------------*/
class snake2DStamp
	extends
		Dialog
	implements
		ActionListener,
		ItemListener,
		WindowListener

{ /* begin class snake2DStamp */

/*....................................................................
	private variables
....................................................................*/

private ImagePlus imp = null;
private Snake2DScale[] skin = null;
private Vector<Integer> segments = null;
private snake2DPointHandler ph = null;
private static boolean doLabeling = true;
private final Checkbox labelsCheckbox = new Checkbox("Include Labels",
	doLabeling);

/*....................................................................
	protected variables
....................................................................*/

protected static final int FONT_SIZE = 9;

/*....................................................................
	constructor methods
....................................................................*/

/*------------------------------------------------------------------*/
protected snake2DStamp (
	final Frame parentWindow,
	final Snake2DScale[] skin,
	final Vector<Integer> segments,
	final ImagePlus imp,
	final snake2DPointHandler ph
) {
	super(parentWindow, "Table Graph", true);
	this.skin = skin;
	this.segments = segments;
	this.imp = imp;
	this.ph = ph;
	ph.setDoLabeling(doLabeling);
	addWindowListener(this);
	setLayout(new GridLayout(0, 1));
	final Label separation1 = new Label("");
	final Label separation2 = new Label("");
	final Button stampButton = new Button("Stamp");
	final Button cancelButton = new Button("Cancel");
	stampButton.addActionListener(this);
	cancelButton.addActionListener(this);
	labelsCheckbox.addItemListener(this);
	add(separation1);
	add(stampButton);
	add(labelsCheckbox);
	add(separation2);
	add(cancelButton);
	pack();
} /* end snake2DStamp */

/*....................................................................
	ActionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
	final ActionEvent ae
) {
	setVisible(false);
	if (ae.getActionCommand().equals("Stamp")) {
		final ImageProcessor ip = imp.getProcessor();
		final ImageWindow window = imp.getWindow();
		final Graphics g = window.getGraphics();
		final Font font = new Font("SansSerif", Font.PLAIN, FONT_SIZE);
		final FontMetrics fontMetrics = getFontMetrics(font);
		ip.setFont(font);
		ip.setJustification(ip.LEFT_JUSTIFY);
		ip.setAntialiasedText(true);
		ip.setColor(Roi.getColor());
		int m = 0;
		int M = 0;
		int s = 0;
		int S = 0;
		if ((segments != null) && !segments.isEmpty()) {
			S = segments.elementAt(M);
		}
		double x0 = 0.0;
		double y0 = 0.0;
		for (int k = 0, K = skin.length; (k < K); k++) {
			final int[] xpoints = skin[k].xpoints;
			final int[] ypoints = skin[k].ypoints;
			final int N = skin[k].npoints;
			if (N == 1) {
				ip.drawDot(xpoints[0], ypoints[0]);
				x0 += (double)xpoints[0];
				y0 += (double)ypoints[0];
			}
			else if (N == 2) {
				ip.drawLine(xpoints[0], ypoints[0], xpoints[1], ypoints[1]);
				x0 += (double)xpoints[0];
				y0 += (double)ypoints[0];
				x0 += (double)xpoints[1];
				y0 += (double)ypoints[1];
			}
			else {
				x0 += (double)xpoints[0];
				y0 += (double)ypoints[0];
				ip.moveTo(xpoints[0], ypoints[0]);
				for (int n = 1; (n < N); n++) {
					ip.lineTo(xpoints[n], ypoints[n]);
					x0 += (double)xpoints[n];
					y0 += (double)ypoints[n];
				}
				if (skin[k].closed) {
					ip.lineTo(xpoints[0], ypoints[0]);
				}
			}
			m += N;
			if (S <= ++s) {
				if (labelsCheckbox.getState()) {
					final String label = "" + ++M;
					final LineMetrics lineMetrics = fontMetrics.getLineMetrics(
						label, g);
					final double midHeight = 0.5 *
						(double)(lineMetrics.getHeight());
					final double midWidth = 0.5
						* (double)fontMetrics.stringWidth(label);
					ip.drawString(label,
						(int)round(x0 / (double)m - midWidth),
						(int)round(y0 / (double)m + midHeight));
				}
				m = 0;
				s = 0;
				if ((segments != null) && (M < segments.size())) {
					S = segments.elementAt(M);
				}
				x0 = 0.0;
				y0 = 0.0;
			}
		}
	}
	ph.setDoLabeling(false);
} /* actionPerformed */

/*....................................................................
	Container methods
....................................................................*/

/*------------------------------------------------------------------*/
public Insets getInsets (
) {
	return(new Insets(0, 20, 20, 20));
} /* end getInsets */

/*....................................................................
	ItemListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void itemStateChanged (
	final ItemEvent ie
) {
	doLabeling = labelsCheckbox.getState();
	ph.setDoLabeling(doLabeling);
	imp.setRoi(ph);
} /* end itemStateChanged */

/*....................................................................
	WindowListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void windowActivated (
	final WindowEvent we
) {
} /* end windowActivated */

/*------------------------------------------------------------------*/
public void windowClosed (
	final WindowEvent we
) {
} /* end windowClosed */

/*------------------------------------------------------------------*/
public void windowClosing (
	final WindowEvent we
) {
	setVisible(false);
	ph.setDoLabeling(false);
} /* end windowClosing */

/*------------------------------------------------------------------*/
public void windowDeactivated (
	final WindowEvent we
) {
} /* end windowDeactivated */

/*------------------------------------------------------------------*/
public void windowDeiconified (
	final WindowEvent we
) {
} /* end windowDeiconified */

/*------------------------------------------------------------------*/
public void windowIconified (
	final WindowEvent we
) {
} /* end windowIconified */

/*------------------------------------------------------------------*/
public void windowOpened (
	final WindowEvent we
) {
} /* end windowOpened */

} /* end class snake2DStamp */
