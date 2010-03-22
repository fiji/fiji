package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
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
|   PointToolbar
\===================================================================*/

/*------------------------------------------------------------------*/

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GUI;
import ij.gui.Toolbar;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
/**
 * Class to deal with the point toolbar option in the bUnwarpJ interface:
 * draw the toolbar and change between tools.
 */
public class PointToolbar extends Canvas implements MouseListener
{ /* begin class PointToolbar */

	/*....................................................................
       Private variables
    ....................................................................*/

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = 4280253125939921252L;
	/** number of tools */
	private static final int NUM_TOOLS = 19;
	/** size of toolbar */
	private static final int SIZE      = 22;
	/** offset */
	private static final int OFFSET    = 3;

	/** grey color */
	private static final Color gray       = Color.lightGray;
	/** bright grey color */
	private static final Color brighter   = gray.brighter();
	/** dark grey color */
	private static final Color darker     = gray.darker();
	/** very dark grey color */
	private static final Color evenDarker = darker.darker();

	/** flags for every tool */
	private final boolean[] down = new boolean[NUM_TOOLS];
	/** graphic pointer */
	private Graphics g;
	/** source image pointer */
	private ImagePlus sourceImp;
	/** target image pointer */
	private ImagePlus targetImp;
	/** previous toolbar instance */
	private Toolbar previousInstance;
	/** source point handler */
	private PointHandler sourcePh;
	/** target point handler */
	private PointHandler targetPh;
	/** toolbar instance */
	private PointToolbar instance;
	/** mouse down time */
	private long mouseDownTime;
	/** current tool */
	private int currentTool = PointAction.ADD_CROSS;
	/** x- coordinate */
	private int x;
	/** y- coordinate */
	private int y;
	/** x- offset */
	private int xOffset;
	/** y- offset */
	private int yOffset;
	/** pointer to the bUnwarpJ dialog */
	private MainDialog dialog;

	/*....................................................................
       Public methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Create an instance of PointToolbar.
	 *
	 * @param previousToolbar pointer to the previous toolbar in order to be able
	 *                        to restore it
	 * @param dialog pointer to the bUnwarpJ interface dialog
	 */
	public PointToolbar (
			final Toolbar previousToolbar,
			final MainDialog dialog)
	{
		this.previousInstance = previousToolbar;
		this.dialog = dialog;
		this.instance = this;
		final Container container = previousToolbar.getParent();
		final Component[] component = container.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] == previousToolbar) {
				container.remove(previousToolbar);
				container.add(this, i);
				break;
			}
		}
		resetButtons();
		down[currentTool] = true;
		setTool(currentTool);
		setForeground(evenDarker);
		setBackground(gray);
		addMouseListener(this);
		container.validate();
	} /* end PointToolbar */


	/*------------------------------------------------------------------*/
	/**
	 * Get current tool.
	 */
	public int getCurrentTool ()
	{
		return(currentTool);
	} /* getCurrentTool */

	/*------------------------------------------------------------------*/
	/**
	 * Mouse clicked.
	 *
	 * @param e mouse event
	 */
	public void mouseClicked (final MouseEvent e) {
	} /* end mouseClicked */

	/*------------------------------------------------------------------*/
	/**
	 * Mouse entered.
	 *
	 * @param e mouse event
	 */
	public void mouseEntered (final MouseEvent e) {
	} /* end mouseEntered */

	/*------------------------------------------------------------------*/
	/**
	 * Mouse exited.
	 *
	 * @param e mouse event
	 */
	public void mouseExited (final MouseEvent e) {
	} /* end mouseExited */

	/*------------------------------------------------------------------*/
	/**
	 * Mouse pressed, applied to select the tool.
	 *
	 * @param e mouse event
	 */
	public void mousePressed (final MouseEvent e)
	{
		final int x = e.getX();
		//final int y = e.getY();
		int newTool = 0;
		for (int i = 0; (i < NUM_TOOLS); i++) 
		{
			if (((i * SIZE) < x) && (x < (i * SIZE + SIZE)))
			{
				newTool = i;
			}
		}
		boolean doubleClick = ((newTool == getCurrentTool())
				&& ((System.currentTimeMillis() - mouseDownTime) <= 500L)
				&& (newTool == PointAction.REMOVE_CROSS));
		mouseDownTime = System.currentTimeMillis();
		if (newTool == PointAction.STOP && !dialog.isFinalActionLaunched())
			return;
		if (newTool!=PointAction.STOP &&  dialog.isFinalActionLaunched())
			return;

		setTool(newTool);

		if (doubleClick) 
		{
			ClearAll clearAllDialog = new ClearAll(IJ.getInstance(), sourcePh, targetPh);
			GUI.center(clearAllDialog);
			clearAllDialog.setVisible(true);
			setTool(PointAction.ADD_CROSS);
			clearAllDialog.dispose();
		}

		switch (newTool) 
		{
			case PointAction.FILE:
				IODialog fileDialog = new IODialog(IJ.getInstance(),
						sourceImp, targetImp, sourcePh, targetPh,dialog);
				GUI.center(fileDialog);
				fileDialog.setVisible(true);
				setTool(PointAction.ADD_CROSS);
				fileDialog.dispose();
				break;
			case PointAction.MASK:
			case PointAction.INVERTMASK:
				dialog.setClearMask(true);
				break;
			case PointAction.STOP:
				dialog.setStopRegistration();
				break;
		}
	} /* end method mousePressed */

	/*------------------------------------------------------------------*/
	/**
	 * Mouse released.
	 *
	 * @param e mouse event
	 */
	public void mouseReleased (final MouseEvent e) {
	} /* end mouseReleased */

	/*------------------------------------------------------------------*/
	/**
	 * Paint the buttons of the toolbar.
	 *
	 * @param g graphic pointer
	 */
	public void paint (final Graphics g)
	{
		for (int i = 0; (i < NUM_TOOLS); i++) {
			drawButton(g, i);
		}
	} /* paint */

	/*------------------------------------------------------------------*/
	/**
	 * Restore the previous toolbar.
	 */
	public void restorePreviousToolbar ()
	{
		final Container container = instance.getParent();
		final Component[] component = container.getComponents();
		for (int i = 0; (i < component.length); i++) {
			if (component[i] == instance) {
				container.remove(instance);
				container.add(previousInstance, i);
				container.validate();
				break;
			}
		}
	} /* end restorePreviousToolbar */

	/*------------------------------------------------------------------*/
	/**
	 * Enable the tool buttons.
	 */
	public void setAllUp ()
	{
		for (int i=0; i<NUM_TOOLS; i++) 
			down[i] = false;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the source image.
	 *
	 * @param sourceImp pointer to the source image representation
	 * @param sourcePh source point handler
	 */
	public void setSource (
			final ImagePlus sourceImp,
			final PointHandler sourcePh)
	{
		this.sourceImp = sourceImp;
		this.sourcePh = sourcePh;
	} /* end setSource */

	/*------------------------------------------------------------------*/
	/**
	 * Set the target image.
	 *
	 * @param targetImp pointer to the target image representation
	 * @param targetPh target point handler
	 */
	public void setTarget (
			final ImagePlus targetImp,
			final PointHandler targetPh)
	{
		this.targetImp = targetImp;
		this.targetPh = targetPh;
	} /* end setTarget */

	/*------------------------------------------------------------------*/
	/**
	 * Set the tool.
	 *
	 * @param tool tool index
	 */
	public void setTool (final int tool)
	{
		if (tool == currentTool) {
			return;
		}
		down[tool] = true;
		down[currentTool] = false;
		Graphics g = this.getGraphics();
		drawButton(g, currentTool);
		drawButton(g, tool);
		g.dispose();
		showMessage(tool);
		currentTool = tool;
	} /* end setTool */



	/*....................................................................
       Private methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Draw a line from the current coordinates to a destination point.
	 *
	 * @param x x-coordinate of the destination point
	 * @param y y-coordinate of the destination point
	 */
	private void d (
			int x,
			int y)
	{
		x += xOffset;
		y += yOffset;
		g.drawLine(this.x, this.y, x, y);
		this.x = x;
		this.y = y;
	} /* end d */

	/*------------------------------------------------------------------*/
	/**
	 * Draw button in the toolbar.
	 *
	 * @param g graphic pointer
	 * @param tool specific tool button
	 */
	private void drawButton (
			final Graphics g,
			final int tool)
	{
		fill3DRect(g, tool * SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
		if (tool==PointAction.STOP && !dialog.isFinalActionLaunched())
			return;
		if (tool!=PointAction.STOP &&  dialog.isFinalActionLaunched())
			return;
		g.setColor(Color.black);
		int x = tool * SIZE + OFFSET;
		int y = OFFSET;
		if (down[tool])
		{
			x++;
			y++;
		}
		this.g = g;

		// Polygon for the mask
		int px[] = new int[5]; px[0]=x+4;px[1]=x+ 4;px[2]=x+14;px[3]=x+ 9;px[4]=x+14;
		int py[] = new int[5]; py[0]=y+3;py[1]=y+13;py[2]=y+13;py[3]=y+ 8;py[4]=y+ 3;

		switch (tool)
		{
		case PointAction.ADD_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			m(13, 11);
			d(13, 15);
			break;
		case PointAction.FILE:
			xOffset = x;
			yOffset = y;
			m(3, 1);
			d(9, 1);
			d(9, 4);
			d(12, 4);
			d(12, 14);
			d(3, 14);
			d(3, 1);
			m(10, 2);
			d(11, 3);
			m(5, 4);
			d(7, 4);
			m(5, 6);
			d(10, 6);
			m(5, 8);
			d(10, 8);
			m(5, 10);
			d(10, 10);
			m(5, 12);
			d(10, 12);
			break;
		case PointAction.MAGNIFIER:
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
		case PointAction.MOVE_CROSS:
			xOffset = x;
			yOffset = y;
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
			break;
		case PointAction.REMOVE_CROSS:
			xOffset = x;
			yOffset = y;
			m(7, 0);
			d(7, 1);
			m(6, 2);
			d(6, 3);
			m(8, 2);
			d(8, 3);
			m(5, 4);
			d(5, 5);
			m(9, 4);
			d(9, 5);
			m(4, 6);
			d(4, 8);
			m(10, 6);
			d(10, 8);
			m(5, 9);
			d(5, 14);
			m(9, 9);
			d(9, 14);
			m(7, 4);
			d(7, 6);
			m(7, 8);
			d(7, 8);
			m(4, 11);
			d(10, 11);
			g.fillRect(x + 6, y + 12, 3, 3);
			m(11, 13);
			d(15, 13);
			break;
		case PointAction.MASK:
			xOffset = x;
			yOffset = y;
			g.fillPolygon(px, py, 5);
			break;
		case PointAction.INVERTMASK:
			xOffset = x;
			yOffset = y;
			g.fillRect(x + 1, y + 1, 15, 15);
			g.setColor(gray);
			g.fillPolygon(px,py,5);
			g.setColor(Color.black);
			break;
		case PointAction.STOP:
			xOffset = x;
			yOffset = y;
			// Octogon
			m( 1,  5);
			d( 1, 11);
			d( 5, 15);
			d(11, 15);
			d(15, 11);
			d(15,  5);
			d(11,  1);
			d( 5,  1);
			d( 1,  5);
			// S
			m( 5,  6);
			d( 3,  6);
			d( 3,  8);
			d( 5,  8);
			d( 5, 10);
			d( 3, 10);
			// T
			m( 6,  6);
			d( 6,  8);
			m( 7,  6);
			d( 7, 10);
			// O
			m(11,  6);
			d( 9,  6);
			d( 9, 10);
			d(11, 10);
			d(11,  6);
			// P
			m(12, 10);
			d(12,  6);
			d(14,  6);
			d(14,  8);
			d(12,  8);
			break;
		}
	} /* end drawButton */

	/*------------------------------------------------------------------*/
	/**
	 * Fill a 3D rect.
	 *
	 * @param g graphic pointer
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param width rect width
	 * @param height rect height
	 * @param raised color flag
	 */
	private void fill3DRect (
			final Graphics g,
			final int x,
			final int y,
			final int width,
			final int height,
			final boolean raised)
	{
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
	/**
	 * Add the offset to the current coordinates.
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 */
	private void m (
			final int x,
			final int y)
	{
		this.x = xOffset + x;
		this.y = yOffset + y;
	} /* end m */

	/*------------------------------------------------------------------*/
	/**
	 * Reset tool buttons.
	 */
	private void resetButtons ()
	{
		for (int i = 0; (i < NUM_TOOLS); i++) {
			down[i] = false;
		}
	} /* end resetButtons */

	/*------------------------------------------------------------------*/
	/**
	 * Show a message for the corresponding tool.
	 *
	 * @param tool tool identifier
	 */
	private void showMessage (final int tool)
	{
		switch (tool) {
		case PointAction.ADD_CROSS:
			IJ.showStatus("Add crosses");
			return;
		case PointAction.FILE:
			IJ.showStatus("Input/Output menu");
			return;
		case PointAction.MAGNIFIER:
			IJ.showStatus("Magnifying glass");
			return;
		case PointAction.MOVE_CROSS:
			IJ.showStatus("Move crosses");
			return;
		case PointAction.REMOVE_CROSS:
			IJ.showStatus("Remove crosses");
			return;
		case PointAction.MASK:
			IJ.showStatus("Draw an inner mask");
			return;
		case PointAction.INVERTMASK:
			IJ.showStatus("Draw an outer mask");
			return;
		case PointAction.STOP:
			IJ.showStatus("Stop registration");
			return;
		default:
			IJ.showStatus("Undefined operation");
		return;
		}
	} /* end showMessage */

} /* end class PointToolbar */