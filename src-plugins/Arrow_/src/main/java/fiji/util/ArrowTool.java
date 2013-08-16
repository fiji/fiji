package fiji.util;

import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.plugin.frame.Recorder;

import ij.process.ImageProcessor;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

/**
 *	<h2>ArrowTool plugin</h2>
 *
 * This plugin aims at being a convenient interactive tool for drawing arrow shape on ImageJ
 * windows. It was made trying to copy the behavior of existing ROI tools in ImageJ.
 *
 * <h3>Usage in ImageJ</h3>
 * Run the plugin. A tool icon appear in ImageJ toolbar. When this icon is selected, drag the mouse
 * with left button to draw non-destructively a ROI arrow. To draw pixels on the image, use the classical
 * <i>draw</i> command (shortcut 'd') to draw the outline of the arrow, and the <i>fill</i> command
 * (shortcut 'f') to draw its interior.
 * <p>
 * Clicking and dragging near the arrow base or head drags them individually. Clicking and dragging near
 * the line drags the whole arrow.
 * <p>
 * Hold the SHIFT key to constrain the arrow direction to multiples of PI/4.
 * <p>
 * Double-click on the toolbar icon to display an option panel, on which the arrow head length, the
 * line thickness and the arrow style can be changed.
 * <p>
 * For backward compatibility  with the previous <code>Arrow_.java</code>, if there is a {@link Line} ROI
 * on the image when this plugin is called, we  draw an arrow non-interactively.
 * See {@link http://rsb.info.nih.gov/ij/plugins/download/Arrow_.java}.
 *
 * <h3>Code and classes</h3>
 *
 * Four classes are at play:
 * <ul>
 * <li> {@link ArrowTool} is the plugin class, that controls the arrow shape and position and deals with
 * user interactions.
 * <li> It inherits from {@link AbstractTool}, which is an abstract tool aimed at standardizing future
 * ROI tools. It has the mechanisms to register the tool to windows and to the toolbar.
 * <li> {@link ArrowShape} is the arrow class that has the mechanism to draw the arrow, according to a
 * given style, head length, ... For convenience, it is wrapped in {@link ArrowShapeRoi} that extends
 * {@link ShapeRoi}.
 * <li> {@link ArrowOptionPanel} is the option panel displayed when the toolbar icon is double-clicked.
 * </ul>
 *
 *
 * @author Jean-Yves Tinevez & Johannes Schindelin
 * Nov-Dec 2009
 * @version 1.0 - 3 Dec 2009
 */
public class ArrowTool extends AbstractTool implements ActionListener,
		MouseListener, MouseMotionListener, MouseWheelListener, ToolWithOptions {
	private ArrowShape arrow;
	private BasicStroke stroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	/**
	 * How close we have to be from control points to drag them.
	 */
	private double drag_tolerance;
	/**
	 * End and start point coordinates of the arrow.
	 */
	private double start_X, start_Y, end_X, end_Y;
	/**
	 * To monitor how much we drag for user interaction.
	 */
	private double start_drag_X, start_drag_Y;

	private ImagePlus imp;
	private ImageCanvas canvas;
	private enum InteractionStatus { NO_ARROW, FREE, DRAGGING_ARROW_HEAD, DRAGGING_ARROW_BASE, DRAGGING_LINE};
	private InteractionStatus status;

	/*
	 * INNER CLASS
	 */

	private static class ArrowShapeRoi extends ShapeRoi {
		private static final long serialVersionUID = 1L;
		private ArrowShape arrow;
		private BasicStroke stroke;
		public ArrowShapeRoi(ArrowShape _arrow, BasicStroke _stroke) {
			super(_arrow);
			Shape out_lineshape = _stroke.createStrokedShape(_arrow);
			this.or(new ShapeRoi(out_lineshape));
			arrow = _arrow;
			stroke = _stroke;
		}
		/**
		 * Overrides the {@link ShapeRoi#draw(Graphics)} of ShapeRoi so that we can have
		 * anti-aliased drawing.
		 */
		public void draw(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			super.draw(g2);
		}
		public ArrowShape getArrow() { return arrow; }
		public BasicStroke getStroke() { return stroke; }
	}

	/*
	 * RUN METHOD
	 */

	public void run(String arg) {
		arrow = new ArrowShape();
		imp = WindowManager.getCurrentImage();
		Roi roi = null;
		if (imp != null) {
			roi = imp.getRoi();
			canvas = imp.getCanvas();
		}
		if ( (roi!= null) && (roi instanceof Line)) {
			/* Legacy method: if there is a line when this plugin is called, then we directly draw
			 * an arrow, so as to comply with the previous Arrow_.java class that might be used
			 * in existing macros.
			 * See http://rsb.info.nih.gov/ij/plugins/download/Arrow_.java			 */
			Line line = (Line)roi;
			arrow.setStartPoint(new Point2D.Double(line.x1d, line.y1d) );
			arrow.setEndPoint(new Point2D.Double(line.x2d, line.y2d) );
			ShapeRoi arrow_roi = new ShapeRoi(arrow);
			ImageProcessor ip = imp.getProcessor();
			ip.fill(arrow_roi);
			ip.draw(arrow_roi);
		} else {
			/* Other wise we start the interactive mode */
			super.run(arg);
			drag_tolerance = arrow.getLength();
			status = InteractionStatus.NO_ARROW;
		}
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public String getToolName() {
		return "Arrow";
	}

	@Override
	public String getToolIcon() {
		return "C000P11aa8ceec8aa";
	}

	@Override
	public void mousePressed(MouseEvent e) {
		ImageCanvas source = (ImageCanvas) e.getSource();
		if (source != canvas) {
			// We changed image window. Update fields accordingly
			ImageWindow window = (ImageWindow) source.getParent();
			imp = window.getImagePlus();
			canvas = source;
			Roi current_roi = imp.getRoi();
			if ( (current_roi == null) || !(current_roi instanceof ArrowShapeRoi)) {
				status = InteractionStatus.NO_ARROW;
				arrow = new ArrowShape();
			} else {
				ArrowShapeRoi arrow_roi = (ArrowShapeRoi) current_roi;
				arrow = arrow_roi.getArrow();
				stroke = arrow_roi.getStroke();
				start_X = arrow.getStartPoint().getX();
				start_Y = arrow.getStartPoint().getY();
				end_X = arrow.getEndPoint().getX();
				end_Y = arrow.getEndPoint().getY();
				status = InteractionStatus.FREE;
			}
		}
		double x = canvas.offScreenXD(e.getX());
		double y = canvas.offScreenYD(e.getY());
		if (status == InteractionStatus.NO_ARROW) {
			start_X = x;
			start_Y = y;
			status = InteractionStatus.FREE;
			} else {
				final double dist_to_line = distanceToLine(x, y);
				final double dist_to_arrowhead = distanceToArrowHead(x, y);
				final double dist_to_arrowbase = distanceToArrowBase(x, y);
				if (dist_to_arrowhead < drag_tolerance) {
					status = InteractionStatus.DRAGGING_ARROW_HEAD;
				} else if (dist_to_arrowbase < drag_tolerance) {
					status = InteractionStatus.DRAGGING_ARROW_BASE;
				} else if (dist_to_line < drag_tolerance) {
					status = InteractionStatus.DRAGGING_LINE;
					start_drag_X = x;
					start_drag_Y = y;
				} else {
					status = InteractionStatus.FREE;
					start_X = x;
					start_Y = y;
				}
			}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		int modifiers = e.getModifiersEx();
		switch (status) {
		case DRAGGING_ARROW_HEAD:
		case FREE:
			if ( (modifiers & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK) {
				// Shift pressed, constrained move
				final double alpha = Math.atan2(y-start_Y, x-start_X);
				if ( (alpha<Math.PI/8 && alpha>=-Math.PI/8) || (alpha>=7*Math.PI/8 || alpha<-7*Math.PI/8) ) {
					end_X = x;
					end_Y = start_Y;
				} else if ( (alpha>=Math.PI/8 && alpha<3*Math.PI/8) || (alpha<-5*Math.PI/8 && alpha>=-7*Math.PI/8) ) {
					final double proj = (x-start_X + y-start_Y);
					end_X = start_X + proj/2 ;
					end_Y = start_Y + proj/2;
				} else if ( (alpha>=3*Math.PI/8 && alpha<5*Math.PI/8) || (alpha<-3*Math.PI/8 && alpha>=-5*Math.PI/8) ) {
					end_X = start_X;
					end_Y = y;
				} else { //if ( (alpha>=5*Math.PI/8 && alpha<7*Math.PI/8) || (alpha<-Math.PI/8 && alpha>=-3*Math.PI/8) ) {
					final double proj = (-x+start_X + y-start_Y);
					end_X = start_X - proj/2 ;
					end_Y = start_Y + proj/2;
				}
			} else {
				// Free
				end_X = x;
				end_Y = y;
			}
			break;
		case DRAGGING_ARROW_BASE:
			start_X =x;
			start_Y = y;
			break;
		case DRAGGING_LINE:
			final double dx = x-start_drag_X;
			final double dy = y-start_drag_Y;
			start_X += dx;
			start_Y += dy;
			end_X += dx;
			end_Y += dy;
			start_drag_X = x;
			start_drag_Y = y;
			break;
		}
		arrow.setStartPoint(new Point2D.Double(start_X,start_Y));
		arrow.setEndPoint(new Point2D.Double(end_X,end_Y));
		paint();
	}

	/**
	 * If the mouse is released near the base AND if we are not dragging the base, then we want
	 * to delete the tool.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		if  ( (status != InteractionStatus.DRAGGING_ARROW_BASE) && (Math.abs(start_X-x)< 1e-2) && (Math.abs(start_Y-y)< 1e-2) ) {
			// Released close to start: erase arrow
			imp.killRoi();
			status = InteractionStatus.NO_ARROW;
		} else {
			status = InteractionStatus.FREE;

			handleRecording();
		}
	}

	/**
	 * SHIFT key + MouseWheelEvent are used to change the arrow field.
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		final double x = canvas.offScreenXD(e.getX());
		final double y = canvas.offScreenYD(e.getY());
		final double dist_to_line = distanceToLine(x, y);
		final double dist_to_arrowhead = distanceToArrowHead(x, y);
		final double dist_to_arrowbase = distanceToArrowBase(x, y);

		/* For this to execute, we must have shift pressed AND be close to the arrow.
		 * Otherwise, we pass the event to the other MouseWheelListeners.		 */
		if ( !e.isShiftDown() ) {
			final MouseWheelListener[] listeners =  ((Component) e.getSource()).getParent().getMouseWheelListeners();
			for (MouseWheelListener listener : listeners) {	listener.mouseWheelMoved(e);}
			return;
		}

		if ( e.isControlDown() )  {
			if ( (dist_to_arrowhead < drag_tolerance) || (dist_to_arrowbase < drag_tolerance) ||  (dist_to_line < drag_tolerance) ) {
				// CTRL SHIFT: we change the style
				final ArrowShape.ArrowStyle style = arrow.getStyle();
				int style_index = style.ordinal();
				style_index += e.getWheelRotation();
				while (style_index < 0) { style_index += ArrowShape.ArrowStyle.values().length; }
				while (style_index > ArrowShape.ArrowStyle.values().length-1) { style_index -= ArrowShape.ArrowStyle.values().length; }
				arrow.setStyle(ArrowShape.ArrowStyle.values()[style_index]);
				paint();
			}
		} else {
			// otherwise fields
			if (dist_to_arrowhead < drag_tolerance) {
				// Near the arrow head, we change the arrow length
				arrow.setLength(Math.max(0,arrow.getLength() + e.getWheelRotation()) );
				paint();
				e.consume();
			} else if ( (dist_to_arrowbase < drag_tolerance) ||  (dist_to_line < drag_tolerance) ) {
				// Near the arrow, we change its thickness
				stroke = new BasicStroke( Math.max(0, stroke.getLineWidth()+e.getWheelRotation()),
						BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
				paint();
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}

	/*
	 * OPTION PANEL
	 */

	@Override
	public void showOptionDialog() {
		final ActionListener current_instance = this;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ArrowOptionPanel inst;
				if (status == InteractionStatus.NO_ARROW) {
					inst = new ArrowOptionPanel();
				} else {
					inst = new ArrowOptionPanel(arrow, stroke);
				}
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
				inst.addActionListener(current_instance);
			}
		});

	}

	/**
	 * Is called when the user change a property using the option panel. We update the
	 * arrow fields, and let the {@link #paint()} method draw it.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		ArrowOptionPanel panel = (ArrowOptionPanel) e.getSource();
		arrow.setLength(panel.getLength());
		arrow.setStyle(panel.getStyle());
		stroke = panel.getStroke();
		paint();
	}

	protected void handleRecording() {
		if (!Recorder.record)
			return;
		Point2D start = arrow.getStartPoint();
		Point2D end = arrow.getEndPoint();
		Recorder.record("call", getClass().getName() + ".select\", \"" + arrow.getStyle()
			+ "\", \"" + start.getX() + "\", \"" + start.getY() + "\", \"" + end.getX() + "\", \"" + end.getY()
			+ "\", \"" + stroke.getLineWidth() + "\", \"" + arrow.getLength());
	}

	// for macros
	public static void select(String style, String x1, String y1, String x2, String y2, String width, String headLength) {
		select(ArrowShape.ArrowStyle.valueOf(style),
			Double.parseDouble(x1), Double.parseDouble(y1), Double.parseDouble(x2), Double.parseDouble(y2),
			Float.parseFloat(width), Double.parseDouble(headLength));
	}

	public static void select(ArrowShape.ArrowStyle style, double x1, double y1, double x2, double y2, float width, double headLength) {
		IJ.getImage().setRoi(makeRoi(style, x1, y1, x2, y2, width, headLength));
	}

	public static Roi makeRoi(ArrowShape.ArrowStyle style, double x1, double y1, double x2, double y2, float width, double headLength) {
		ArrowShape arrow = new ArrowShape(style, headLength);
		arrow.setStartPoint(new Point2D.Double(x1, y1));
		arrow.setEndPoint(new Point2D.Double(x2, y2));
		BasicStroke stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		return new ArrowShapeRoi(arrow, stroke);
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Paint the arrow roi.
	 */
	private void paint() {
		if (status != InteractionStatus.NO_ARROW) {
			ArrowShapeRoi roi = new ArrowShapeRoi(arrow, stroke);
			imp.setRoi(roi);
			drag_tolerance = 0.5 * Math.max(arrow.getLength(), stroke.getLineWidth());

		}
	}

	/**
	 * Measure the distance to the line between coordinates start_X, start_Y and end_X, end_Y.
	 * If x, y is not in the space defined by the line segment, Inf is returned.
	 */
	private double distanceToLine(final double x, final double y) {
		final double Ax = x-start_X;
		final double Ay = y-start_Y;
		final double Bx = x-end_X;
		final double By = y-end_Y;
		final double Lx = end_X-start_X;
		final double Ly = end_Y-start_Y;
		final double al = Ax*Lx + Ay*Ly;
		final double bl = Bx*Lx + By*Ly;
		if ( (al<0) || (bl>0) ) {
			return Double.POSITIVE_INFINITY; // we are not within the segment space
		}
		final double a_square = Ax*Ax+Ay*Ay;
		final double b_square = Bx*Bx+By*By;
		final double l = Math.sqrt(Lx*Lx+Ly*Ly);
		final double h = Math.sqrt( a_square - al*al/l/l) + Math.sqrt( b_square - bl*bl/l/l);
		return h/2;
	}

	/**
	 * Returns the distance to the arrow head
	 */
	private double distanceToArrowHead(final double x, final double y) {
		final double dx = x-end_X;
		final double dy = y-end_Y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	/**
	 * Returns the distance to the arrow base
	 */
	private double distanceToArrowBase(final double x, final double y) {
		final double dx = x-start_X;
		final double dy = y-start_Y;
		return Math.sqrt(dx*dx+dy*dy);
	}
}