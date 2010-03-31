package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ and Fiji.
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

/*====================================================================
|   PointHandler
\===================================================================*/

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Vector;

/**
 * Class to deal with handle points in bUnwarpJ: here we have the methods
 * to paint the landmarks and the masks.
 */
public class PointHandler extends Roi
{ /* begin class PointHandler */

	/*....................................................................
       Private variables
    ....................................................................*/

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 4829296689557078996L;

	/** constant to keep half of the cross size */
	private static final int CROSS_HALFSIZE = 5;

	// Colors
	private final Vector <Color> listColors = new Vector <Color>();
	
	// List of crosses
	/** list of points */
	private final Vector <Point>  listPoints     = new Vector <Point> (0, 16);
	/** current point */
	private int           currentPoint   = -1;
	/** number of points */
	private int           numPoints      = 0;
	/** start flag */
	private boolean       started        = false;

	/** list of points in the mask */
	private final Vector <Point> listMaskPoints = new Vector <Point> (0,16);
	/** flat to check if the mask is closed or not */
	private boolean       maskClosed     = false;

	// Some useful references
	/** pointer to the image representation */
	private ImagePlus imp = null;
	/** pointer to the point actions */
	private PointAction  pa = null;
	/** pointer to the point toolbar */
	private PointToolbar tb = null;
	/** pointer to the mask */
	private Mask mask = null;
	/** pointer to the main bUnwarpJ dialog */
	private MainDialog dialog = null;
	/** hue for assigning new color ([0.0-1.0]) */
	private float hue = 0f;
	/** saturation for assigning new color ([0.5-1.0]) */
	private float saturation = 0.5f;

	/*....................................................................
       Public methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Constructor with graphical capabilities, create an instance of PointHandler.
	 *
	 * @param imp pointer to the image
	 * @param tb pointer to the toolbar
	 * @param mask pointer to the mask
	 * @param dialog pointer to the bUnwarpJ dialog
	 */
	public PointHandler (
			final ImagePlus imp,
			final PointToolbar tb,
			final Mask mask,
			final MainDialog dialog)
	{
		super(0, 0, imp.getWidth(), imp.getHeight(), imp);
		this.imp = imp;
		this.tb = tb;
		this.dialog = dialog;
		this.pa = new PointAction(imp, this, tb, dialog);
		final ImageWindow iw = imp.getWindow();
		final ImageCanvas ic = iw.getCanvas();
		//iw.requestFocus();
		iw.removeKeyListener(IJ.getInstance());
		iw.addKeyListener(pa);
		ic.removeMouseMotionListener(ic);
		ic.removeMouseListener(ic);
		ic.removeKeyListener(IJ.getInstance());
		ic.addKeyListener(pa);
		ic.addMouseListener(pa);
		ic.addMouseMotionListener(pa);
		started = true;

		this.mask = mask;
		//clearMask(); // This line was commented to allow loading masks from the second slice of a stack.
	} /* end PointHandler */

	/**
	 * Constructor without graphical capabilities, create an instance of PointHandler.
	 *
	 * @param imp image
	 */
	public PointHandler (final ImagePlus imp)
	{
		super(0, 0, imp.getWidth(), imp.getHeight(), imp);
		this.imp = imp;
		tb = null;
		dialog = null;
		pa = null;
		started = true;
		mask = null;
	} /* end PointHandler */


	/**
	 * Constructor without graphical capabilities, create an instance of PointHandler.
	 *
	 * @param width image width
	 * @param height image height
	 */
	public PointHandler (final int width, final int height)
	{
		super(0, 0, width, height);
		this.imp = null;
		tb = null;
		dialog = null;
		pa = null;
		started = true;
		mask = null;
	} /* end PointHandler */
	

	/*------------------------------------------------------------------*/
	/**
	 * Add a point to the mask.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 */
	public void addMaskPoint (
			final int x,
			final int y)
	{
		if (maskClosed) return;
		final Point p = new Point(x, y);
		listMaskPoints.addElement(p);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Add a point to the list of points.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 */
	public void addPoint (
			final int x,
			final int y)
	{
		final Point p = new Point(x, y);
		listPoints.addElement(p);
		Color c = Color.getHSBColor(this.hue, this.saturation, 1);
		// Calculate next color by golden angle
		this.hue += 0.38197f; // golden angle
		if (this.hue > 1) 
			this.hue -= 1;
		this.saturation += 0.38197f; // golden angle
		if (this.saturation > 1)
			this.saturation -= 1;
		this.saturation = 0.5f * this.saturation + 0.5f;
		
		listColors.addElement(c);
		currentPoint = numPoints;
		numPoints++;

	} // end addPoint

	/*------------------------------------------------------------------*/
	/**
	 * Check if it is possible to add points to the mask.
	 *
	 * @return false if the image is coming from a stack
	 */
	public boolean canAddMaskPoints()
	{
		return !mask.isFromStack();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Remove all the elements of the mask.
	 */
	public void clearMask ()
	{
		// Clear mask information in this object
		listMaskPoints.removeAllElements();
		maskClosed=false;
		mask.clearMask();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Close mask.
	 *
	 * @param tool option to invert or not the mask
	 */
	public void closeMask (int tool)
	{
		listMaskPoints.addElement(listMaskPoints.elementAt(0));
		maskClosed=true;
		mask.setMaskPoints(listMaskPoints);
		mask.fillMask(tool);
		dialog.grayImage(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Draw the landmarks and mask.
	 *
	 * @param g graphic element
	 */
	public void draw (final Graphics g)
	{		
		// Draw landmarks
		if (started)
		{
			final double mag = (double)ic.getMagnification();
			final int dx = (int)(mag / 2.0);
			final int dy = (int)(mag / 2.0);
			for (int k = 0; (k < numPoints); k++)
			{
				final Point p = (Point)listPoints.elementAt(k);
				//g.setColor(spectrum[((Integer)listColors.elementAt(k)).intValue()]);
				g.setColor(listColors.elementAt(k));
				
				if (k == currentPoint)
				{
					if (WindowManager.getCurrentImage() == imp)
					{
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - 1) + dy);
						g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
						g.drawLine(ic.screenX(p.x + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x - 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + 1) + dy);
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + 1) + dy,
								ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - 1) + dy);
						if (1.0 < ic.getMagnification()) {
							g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
									ic.screenY(p.y) + dy,
									ic.screenX(p.x + CROSS_HALFSIZE) + dx,
									ic.screenY(p.y) + dy);
							g.drawLine(ic.screenX(p.x) + dx,
									ic.screenY(p.y - CROSS_HALFSIZE) + dy,
									ic.screenX(p.x) + dx,
									ic.screenY(p.y + CROSS_HALFSIZE) + dy);
						}
					} // end if WindowManager.getCurrentImage() == imp
					else
					{
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy);
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE - 1) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE - 1) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE + 1) + dy);
					}
				} // end if (k == currentPoint)
					else
					{
						g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
								ic.screenY(p.y) + dy,
								ic.screenX(p.x + CROSS_HALFSIZE) + dx,
								ic.screenY(p.y) + dy);
						g.drawLine(ic.screenX(p.x) + dx,
								ic.screenY(p.y - CROSS_HALFSIZE) + dy,
								ic.screenX(p.x) + dx,
								ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					}
			}
			if (updateFullWindow)
			{
				updateFullWindow = false;
				imp.draw();
			}
						
		}

		// Draw mask
		int numberMaskPoints=listMaskPoints.size();
		if (numberMaskPoints!=0) {
			final double mag = (double)ic.getMagnification();
			final int dx = (int)(mag / 2.0);
			final int dy = (int)(mag / 2.0);

			int CIRCLE_RADIUS=CROSS_HALFSIZE/2;
			int CIRCLE_DIAMETER=2*CIRCLE_RADIUS;
			for (int i=0; i<numberMaskPoints; i++) {
				final Point p = (Point)listMaskPoints.elementAt(i);
				g.setColor(Color.yellow);
				g.drawOval(ic.screenX(p.x)-CIRCLE_RADIUS+dx, ic.screenY(p.y)-CIRCLE_RADIUS+dy,
						CIRCLE_DIAMETER, CIRCLE_DIAMETER);
				if (i!=0) {
					Point previous_p=(Point)listMaskPoints.elementAt(i-1);
					g.drawLine(ic.screenX(p.x)+dx,ic.screenY(p.y)+dy,
							ic.screenX(previous_p.x)+dx,ic.screenY(previous_p.y)+dy);
				}
			}
		}
	} /* end draw */

	/*------------------------------------------------------------------*/
	/**
	 * Find the the closest point to a certain point from the list of points.
	 *
	 * @param x x- point coordinate
	 * @param y y- point coordinate
	 * @return point index in the list of points
	 */
	public int findClosest (
			int x,
			int y)
	{
		if (numPoints == 0)
		{
			return(currentPoint);
		}

		x = ic.offScreenX(x);
		y = ic.offScreenY(y);

		Point p = new Point((Point)listPoints.elementAt(currentPoint));

		double distance = (double)(x - p.x) * (double)(x - p.x)
		+ (double)(y - p.y) * (double)(y - p.y);

		for (int k = 0; (k < numPoints); k++)
		{
			p = (Point)listPoints.elementAt(k);
			final double candidate = (double)(x - p.x) * (double)(x - p.x)
			+ (double)(y - p.y) * (double)(y - p.y);
			if (candidate < distance)
			{
				distance = candidate;
				currentPoint = k;
			}
		}
		return(currentPoint);
	} /* end findClosest */

	/*------------------------------------------------------------------*/
	/**
	 * Get the current point in the list of points.
	 *
	 * @return current point
	 */
	public Point getPoint ()
	{
		return((0 <= currentPoint) ? (Point)listPoints.elementAt(currentPoint) : (null));
	} /* end getPoint */

	/*------------------------------------------------------------------*/
	/**
	 * Get point action.
	 *
	 * @return point action
	 */
	public PointAction getPointAction () {return pa;}

	/*------------------------------------------------------------------*/
	/**
	 * Get current point index.
	 *
	 * @return index of current point
	 */
	public int getCurrentPoint ()
	{
		return(currentPoint);
	} /* end getCurrentPoint */

	/*------------------------------------------------------------------*/
	/**
	 * Get the list of points.
	 *
	 * @return list of points
	 */
	public Vector <Point> getPoints ()
	{
		return(listPoints);
	} /* end getPoints */

	/*------------------------------------------------------------------*/
	/**
	 * Kill listeners.
	 */
	public void killListeners ()
	{
		if(imp != null)
		{
			final ImageWindow iw = imp.getWindow();
			final ImageCanvas ic = iw.getCanvas();
		
			ic.removeKeyListener(pa);
			ic.removeMouseListener(pa);
			ic.removeMouseMotionListener(pa);
			ic.addMouseMotionListener(ic);
			ic.addMouseListener(ic);
			ic.addKeyListener(IJ.getInstance());
		}
	} /* end killListeners */

	/*------------------------------------------------------------------*/
	/**
	 * Move the current point into a new position.
	 *
	 * @param x x-coordinate of the new position
	 * @param y y-coordinate of the new position
	 */
	public void movePoint (
			int x,
			int y)
	{
		if (0 <= currentPoint)
		{
			x = ic.offScreenX(x);
			y = ic.offScreenY(y);
			x = (x < 0) ? (0) : (x);
			x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
			y = (y < 0) ? (0) : (y);
			y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
			listPoints.removeElementAt(currentPoint);
			final Point p = new Point(x, y);
			listPoints.insertElementAt(p, currentPoint);
		}
	} /* end movePoint */

	/*------------------------------------------------------------------*/
	/**
	 * Increase the current point index one position in the list.
	 */
	public void nextPoint ()
	{
		currentPoint = (currentPoint == (numPoints - 1)) ? (0) : (currentPoint + 1);
	} /* end nextPoint */

	/*------------------------------------------------------------------*/
	/**
	 * Remove the current point.
	 */
	public void removePoint ()
	{
		if (0 < numPoints) {
			listPoints.removeElementAt(currentPoint);
			//usedColor[((Integer)listColors.elementAt(currentPoint)).intValue()] = false;
			listColors.removeElementAt(currentPoint);
			numPoints--;
		}
		currentPoint = numPoints - 1;
		if (currentPoint < 0) {
			tb.setTool(PointAction.ADD_CROSS);
		}
	} /* end removePoint */

	/*------------------------------------------------------------------*/
	/**
	 * Remove one specific point.
	 *
	 * @param k index of the point to be removed
	 */
	public void removePoint (final int k)
	{
		if (0 < numPoints) {
			listPoints.removeElementAt(k);
			//usedColor[((Integer)listColors.elementAt(k)).intValue()] = false;
			listColors.removeElementAt(k);
			numPoints--;
		}
		currentPoint = numPoints - 1;
		if (currentPoint < 0) {
			tb.setTool(PointAction.ADD_CROSS);
		}
	} /* end removePoint */

	/*------------------------------------------------------------------*/
	/**
	 * Remove all points in the mask.
	 */
	public void removePoints ()
	{
		listPoints.removeAllElements();
		listColors.removeAllElements();
		numPoints = 0;
		currentPoint = -1;
		tb.setTool(PointAction.ADD_CROSS);
		imp.setRoi(this);
	} /* end removePoints */

	/*------------------------------------------------------------------*/
	/**
	 * Set the current point.
	 *
	 * @param currentPoint new current point index
	 */
	public void setCurrentPoint (final int currentPoint)
	{
		this.currentPoint = currentPoint;
	} /* end setCurrentPoint */

	/*------------------------------------------------------------------*/
	/**
	 * Set the set of test for the source.
	 *
	 * @param set number of source set
	 */
	public void setTestSourceSet (final int set)
	{
		removePoints();
		switch(set) {
		case 1: // Deformed_Lena 1
			addPoint(11,11);
			addPoint(200,6);
			addPoint(197,204);
			addPoint(121,111);
			break;
		case 2: // Deformed_Lena 1
			addPoint(6,6);
			addPoint(202,7);
			addPoint(196,210);
			addPoint(10,214);
			addPoint(120,112);
			addPoint(68,20);
			addPoint(63,163);
			addPoint(186,68);
			break;
		}
	} /* end setTestSourceSet */

	/*------------------------------------------------------------------*/
	/**
	 * Set the set of test for the target.
	 *
	 * @param set number of target set
	 */
	public void setTestTargetSet (final int set)
	{
		removePoints();
		switch(set) {
		case 1:
			addPoint(11,11);
			addPoint(185,15);
			addPoint(154,200);
			addPoint(123,92);
			break;
		case 2: // Deformed_Lena 1
		addPoint(6,6);
		addPoint(185,14);
		addPoint(154,200);
		addPoint(3,178);
		addPoint(121,93);
		addPoint(67,14);
		addPoint(52,141);
		addPoint(178,68);
		break;
		}
	} /* end setTestTargetSet */

	/*------------------------------------------------------------------*/
	/**
	 * Set the secondary point handler.
	 *
	 * @param secondaryImp pointer to the secondary image
	 * @param secondaryPh secondary point handler
	 */
	public void setSecondaryPointHandler (
			final ImagePlus secondaryImp,
			final PointHandler secondaryPh)
	{
		pa.setSecondaryPointHandler(secondaryImp, secondaryPh);
	} /* end setSecondaryPointHandler */



} /* end class PointHandler */
