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
|   PointAction
\===================================================================*/
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;

import java.awt.Event;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Class for all actions related with points in the bUnwarpJ interface: moving, adding and 
 * deleting landmarks, and painting the masks. The points are stored and handle in the <code>PointHandler</code> class.
 */
public class PointAction extends ImageCanvas implements KeyListener, MouseListener,
      MouseMotionListener
{ /* begin class PointAction */

    /*....................................................................
       Public variables
    ....................................................................*/
	
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -8141177836023328859L;
	/**constant to identify the add cross tool */
	public static final int ADD_CROSS    = 0;
	/**constant to identify the move cross tool */
    public static final int MOVE_CROSS   = 1;
    /**constant to identify the remove cross tool */
    public static final int REMOVE_CROSS = 2;
    /**constant to identify the inner mask tool */
    public static final int MASK         = 3;
    /**constant to identify the outer mask tool */
    public static final int INVERTMASK   = 4;
    /**constant to identify the input/output menu tool */
    public static final int FILE         = 5;
    /**constant to identify the stop tool */
    public static final int STOP         = 7;
    /**constant to identify the magnifier tool */
    public static final int MAGNIFIER    = 11;

    /*....................................................................
       Private variables
    ....................................................................*/

    /** main image plus (active image) */
    private ImagePlus mainImp = null;
    /** secondary image plus (the other image involved in the registration) */
    private ImagePlus secondaryImp = null;
    /** main point handler (set of points of the active image) */
    private PointHandler mainPh = null;
    /** secondary point handler (set of point of the other image) */
    private PointHandler secondaryPh = null;
    /** pointer to the toolbar */
    private PointToolbar tb = null;
    /** pointer to the interface dialog */
    private MainDialog dialog = null;
    /** time the mouse is down */
    private long mouseDownTime;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Create an instance of PointAction.
     * 
     * @param imp input image
     * @param ph point handler
     * @param tb point toolbar
     * @param dialog pointer to the main bUnwarpJ dialog
     */
    public PointAction (
       final ImagePlus imp,
       final PointHandler ph,
       final PointToolbar tb,
       final MainDialog dialog)
    {
       super(imp);
       this.mainImp = imp;
       this.mainPh = ph;
       this.tb = tb;
       this.dialog = dialog;
    } // end PointAction (constructor)    
    
    
    /*------------------------------------------------------------------*/
    /**
     * Method key pressed.
     *
     * @param e key event
     */
    public void keyPressed (final KeyEvent e)
    {
    	// exit if the current tool is inner or outer mask
    	if (tb.getCurrentTool()==MASK || tb.getCurrentTool()==INVERTMASK) 
    		return;
    	
    	final Point p = mainPh.getPoint();
    	if (p == null) return;
    	final int x = p.x;
    	final int y = p.y;
    	switch (e.getKeyCode()) 
    	{
	    	case KeyEvent.VK_DELETE:
	    	case KeyEvent.VK_BACK_SPACE:
	    		mainPh.removePoint();
	    		secondaryPh.removePoint();
	    		updateAndDraw();
	    		break;
	    	case KeyEvent.VK_DOWN:
	    		mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
	    				mainImp.getWindow().getCanvas().screenY(y
	    						+ (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
	    		mainImp.setRoi(mainPh);
	    		break;
	    	case KeyEvent.VK_LEFT:
	    		mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
	    				- (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
	    				mainImp.getWindow().getCanvas().screenY(y));
	    		mainImp.setRoi(mainPh);
	    		break;
	    	case KeyEvent.VK_RIGHT:
	    		mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x
	    				+ (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())),
	    				mainImp.getWindow().getCanvas().screenY(y));
	    		mainImp.setRoi(mainPh);
	    		break;
	    	case KeyEvent.VK_TAB:
	    		mainPh.nextPoint();
	    		secondaryPh.nextPoint();
	    		updateAndDraw();
	    		break;
	    	case KeyEvent.VK_UP:
	    		mainPh.movePoint(mainImp.getWindow().getCanvas().screenX(x),
	    				mainImp.getWindow().getCanvas().screenY(y
	    						- (int)Math.ceil(1.0 / mainImp.getWindow().getCanvas().getMagnification())));
	    		mainImp.setRoi(mainPh);
	    		break;
    	}
    } /* end keyPressed */

    /*------------------------------------------------------------------*/
    /**
     * Method key released.
     *
     * @param e key event
     */
    public void keyReleased (final KeyEvent e){
    } /* end keyReleased */

    /*------------------------------------------------------------------*/
    /**
     * Method key typed.
     *
     * @param e key event
     */
    public void keyTyped (final KeyEvent e) {
    } /* end keyTyped */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse clicked.
     *
     * @param e mouse event
     */
    public void mouseClicked (final MouseEvent e) {
    } /* end mouseClicked */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse dragged, applied move the cross.
     *
     * @param e mouse event
     */
    public void mouseDragged (final MouseEvent e)
    {
       final int x = e.getX();
       final int y = e.getY();
       if (tb.getCurrentTool() == MOVE_CROSS) {
          mainPh.movePoint(x, y);
          updateAndDraw();
       }
       mouseMoved(e);
    } /* end mouseDragged */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse entered, applied to set the main window.
     *
     * @param e mouse event
     */
    public void mouseEntered (final MouseEvent e)
    {
       WindowManager.setCurrentWindow(mainImp.getWindow());
       mainImp.getWindow().toFront();
       updateAndDraw();
    } /* end mouseEntered */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse exited.
     *
     * @param e mouse event
     */
    public void mouseExited (final MouseEvent e)
    {
       IJ.showStatus("");
    } /* end mouseExited */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse moved, show the coordinates of the mouse pointer.
     *
     * @param e mouse event
     */
    public void mouseMoved (final MouseEvent e)
    {
       setControl();
       final int x = mainImp.getWindow().getCanvas().offScreenX(e.getX());
       final int y = mainImp.getWindow().getCanvas().offScreenY(e.getY());
       IJ.showStatus(mainImp.getLocationAsString(x, y) + getValueAsString(x, y));
    } /* end mouseMoved */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse pressed, allow all the different option over the images.
     *
     * @param e mouse event
     */
    public void mousePressed (final MouseEvent e)
    {
       if (dialog.isFinalActionLaunched()) 
    	   return;
       int x = e.getX(),xp = 0;
       int y = e.getY(),yp = 0;
       int currentPoint = 0;
       boolean doubleClick = (System.currentTimeMillis() - mouseDownTime) <= 250L;
       this.mouseDownTime = System.currentTimeMillis();
       switch (tb.getCurrentTool()) 
       {
          case ADD_CROSS:
             xp = mainImp.getWindow().getCanvas().offScreenX(x);
             yp = mainImp.getWindow().getCanvas().offScreenY(y);
             mainPh.addPoint(xp, yp);

             xp = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
             yp = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));
             secondaryPh.addPoint(xp, yp);

             updateAndDraw();
             break;
          case MOVE_CROSS:
             currentPoint = mainPh.findClosest(x, y);
             secondaryPh.setCurrentPoint(currentPoint);
             updateAndDraw();
             break;
          case REMOVE_CROSS:
             currentPoint = mainPh.findClosest(x, y);
             mainPh.removePoint(currentPoint);
             secondaryPh.removePoint(currentPoint);
             updateAndDraw();
             break;
          case MASK:
          case INVERTMASK:
              if (mainPh.canAddMaskPoints())
              {
                 if (!doubleClick)
                 {
                    if (dialog.isClearMaskSet())
                    {
                       mainPh.clearMask();
                       dialog.setClearMask(false);
                       dialog.ungrayImage(this);
                    }
                    x = positionX(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenX(x));
                    y = positionY(mainImp, secondaryImp, mainImp.getWindow().getCanvas().offScreenY(y));

                    mainPh.addMaskPoint(x, y);
                 }
                 else
                    mainPh.closeMask(tb.getCurrentTool());
                 updateAndDraw();
             } else {
                 IJ.error("A mask cannot be manually assigned since the mask was already in the stack");
             }
              break;
          case MAGNIFIER:
             final int flags = e.getModifiers();
             if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
                mainImp.getWindow().getCanvas().zoomOut(x, y);
             }
             else {
                mainImp.getWindow().getCanvas().zoomIn(x, y);
             }
             break;
       }
    } /* end mousePressed */

    /*------------------------------------------------------------------*/
    /**
     * Method mouse released.
     *
     * @param e mouse event
     */
    public void mouseReleased (final MouseEvent e) {
    } /* end mouseReleased */

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
       this.secondaryImp = secondaryImp;
       this.secondaryPh = secondaryPh;
       
       if(this.secondaryImp == null)
    	   IJ.log("Error: set secondary image as null!");
       if(this.secondaryPh == null)
    	   IJ.log("Error: set secondray point handler as null!");
       
    } /* end setSecondaryPointHandler */


    /*....................................................................
       Private methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Get a pixel value as string.
     *
     * @param x x- coordinate of the pixel
     * @param y y- coordinate of the pixel
     * @return pixel value in string form
     */
    private String getValueAsString (
       final int x,
       final int y)
    {
       final Calibration cal = mainImp.getCalibration();
       final int[] v = mainImp.getPixel(x, y);
       final int mainImptype=mainImp.getType();
       if (mainImptype==ImagePlus.GRAY8 || mainImptype==ImagePlus.GRAY16) {
           final double cValue = cal.getCValue(v[0]);
           if (cValue==v[0]) {
              return(", value=" + v[0]);
           }
           else {
              return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
           }
       } else if (mainImptype==ImagePlus.GRAY32) {
                return(", value=" + Float.intBitsToFloat(v[0]));
       } else if (mainImptype==ImagePlus.COLOR_256) {
           return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
       } else if (mainImptype == ImagePlus.COLOR_RGB) {
           return(", value=" + v[0] + "," + v[1] + "," + v[2]);
       } else {
           return("");
       }
    } /* end getValueAsString */

    /*------------------------------------------------------------------*/
    /**
     * Get a common x- position between two images.
     *
     * @param imp1 first image
     * @param imp2 second image
     * @param x x-coordinate
     * @return common position
     */
    private int positionX (
       final ImagePlus imp1,
       final ImagePlus imp2,
       final int x)
    {
       return((x * imp2.getWidth()) / imp1.getWidth());
    } /* end PositionX */

    /*------------------------------------------------------------------*/
    /**
     * Get a common y- position between two images.
     *
     * @param imp1 first image
     * @param imp2 second image
     * @param y y-coordinate
     * @return common position
     */
    private int positionY (
       final ImagePlus imp1,
       final ImagePlus imp2,
       final int y)
    {
       return((y * imp2.getHeight()) / imp1.getHeight());
    } /* end PositionY */

    /*------------------------------------------------------------------*/
    /**
     * Set control.
     */
    private void setControl ()
    {
       switch (tb.getCurrentTool()) {
          case ADD_CROSS:
             mainImp.getWindow().getCanvas().setCursor(crosshairCursor);
             break;
          case FILE:
          case MAGNIFIER:
          case MOVE_CROSS:
          case REMOVE_CROSS:
          case MASK:
          case INVERTMASK:
          case STOP:
             mainImp.getWindow().getCanvas().setCursor(defaultCursor);
             break;
       }
    } /* end setControl */

    /*------------------------------------------------------------------*/
    /**
     * Update the region of interest of the main and secondary images.
     */
    private void updateAndDraw ()
    {
    	if(mainImp != null)
    		mainImp.setRoi(mainPh);
    	if(secondaryImp != null)
    		secondaryImp.setRoi(secondaryPh);
    } /* end updateAndDraw */

} /* end class PointAction */
