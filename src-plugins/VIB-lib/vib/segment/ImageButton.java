package vib.segment;
/**
 * Copyright(c) 1997 DTAI, Incorporated (http://www.dtai.com)
 *
 *                        All rights reserved
 *
 * Permission to use, copy, modify and distribute this material for
 * any purpose and without fee is hereby granted, provided that the
 * above copyright notice and this permission notice appear in all
 * copies, and that the name of DTAI, Incorporated not be used in
 * advertising or publicity pertaining to this material without the
 * specific, prior written permission of an authorized representative of
 * DTAI, Incorporated.
 *
 * DTAI, INCORPORATED MAKES NO REPRESENTATIONS AND EXTENDS NO WARRANTIES,
 * EXPRESS OR IMPLIED, WITH RESPECT TO THE SOFTWARE, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR ANY PARTICULAR PURPOSE, AND THE WARRANTY AGAINST
 * INFRINGEMENT OF PATENTS OR OTHER INTELLECTUAL PROPERTY RIGHTS.  THE
 * SOFTWARE IS PROVIDED "AS IS", AND IN NO EVENT SHALL DTAI, INCORPORATED OR
 * ANY OF ITS AFFILIATES BE LIABLE FOR ANY DAMAGES, INCLUDING ANY
 * LOST PROFITS OR OTHER INCIDENTAL OR CONSEQUENTIAL DAMAGES RELATING
 * TO THE SOFTWARE.
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Iterator;
import java.util.Vector;

/**
 * ImageButton - A button component with an image in it
 *
 * @author	DTAI, Incorporated
 */

public class ImageButton extends Canvas implements MouseListener {

    public static final int UNARMED = 0;
    public static final int ARMED = 1;
    public static final int OVER = 2;
    public static final int DISABLED = 3;

    private static final Border defaultUnarmedBorder =
        new DefaultImageButtonBorder( false );
    private static final Border defaultArmedBorder =
        new DefaultImageButtonBorder( true );

    private MediaTracker tracker;

    private Image images[] = new Image[4];
    private Border borders[] = new Border[4];

    private boolean generatedDisabled = false;
    private boolean mousedown = false;

    private int buttonState = UNARMED;
	private String command = "";
    
    /**
     * Constructs an ImageButton
     */
    public ImageButton() {
        tracker = new MediaTracker( this );
        setUnarmedBorder( defaultUnarmedBorder );
        setArmedBorder( defaultArmedBorder );
		addMouseListener(this);
    }

    /**
     * Constructs an ImageButton with the given image.
     *
     * @param image         the image for all states of the button
     *                      (until other images are assigned)
     */
    public ImageButton( Image image ) {
        this();
        setUnarmedImage( image );
    }

    /**
     * Used internally to add the Image to the array and the MediaTracker,
     * start loading the image if necessary via the tracker's "checkID", and
     * repaint if necessary.
     *
     * @param id        the buttonState id (also used as image id for the MediaTracker)
     * @param image     the image, which is not supposed to be null
     */
	private synchronized void setImage( int id, Image image ) {
	    if (image == null)
		throw new RuntimeException("Image is null!");
	    if ( images[id] != image ) {
       	    images[id] = image;
        	if ( image != null ) {
    	        tracker.addImage( image, id );
        	    tracker.checkID( id, true );
        	}
    	    if ( buttonState == id ) {
        	    repaint();
        	}
        }
	}

    /**
     * Sets the image to display when the button is not pressed or hilited
     * because of a mouse-over.  This image is also used in those other cases
     * if no alternative image is requested.
     *
     * @param image     the unarmed image
     */
	public void setUnarmedImage( Image image ) {
        setImage( UNARMED, image );
        if ( images[ARMED] == null ) {
            setArmedImage( image );
        }
        if ( images[OVER] == null ) {
            setOverImage( image );
        }
        if ( ( images[DISABLED] == null ) ||
             generatedDisabled ) {
            setDisabledImage( null );
        }
	}

    /**
     * Sets the image to display when the button is pressed and the mouse
     * is still over the button.
     *
     * @param image     the armed image
     */
	public void setArmedImage( Image image ) {
   	    if ( image != null ) {
       	    setImage( ARMED, image );
   	    }
   	    else {
   	        setImage( ARMED, images[UNARMED] );
   	    }
	}

    /**
     * Sets the image to display when the button is not pressed and the mouse
     * is over the button.
     *
     * @param image     the over image
     */
	public void setOverImage( Image image ) {
   	    if ( image != null ) {
       	    setImage( OVER, image );
   	    }
   	    else {
   	        setImage( OVER, images[UNARMED] );
   	    }
	}

    /**
     * Sets the image to display when the button is disabled.
     *
     * @param image     the disabled image
     */
	public void setDisabledImage( Image image ) {
        generatedDisabled = false;
   	    if ( ( image == null ) &&
   	         ( images[UNARMED] != null ) ) {
            generatedDisabled = true;
            image = createImage( new FilteredImageSource( images[UNARMED].getSource(),
                                                          new DisableImageFilter() ) );
   	    }
	    setImage( DISABLED, image );
	}

    /**
     * Gets the image to display when the button is not pressed or hilited
     * because of a mouse-over.  This image is also used in those other cases
     * if no alternative image is requested.
     *
     * @return     the unarmed image
     */
	public Image getUnarmedImage() {
		return ( images[UNARMED] );
	}

    /**
     * Gets the image to display when the button is pressed and the mouse
     * is still over the button.
     *
     * @return     the armed image
     */
	public Image getArmedImage() {
		return ( images[ARMED] );
	}

    /**
     * Gets the image to display when the button is not pressed and the mouse
     * is over the button.
     *
     * @return     the over image
     */
	public Image getOverImage() {
		return ( images[OVER] );
	}

    /**
     * Gets the image to display when the button is disabled.
     *
     * @return     the armed image
     */
	public Image getDisabledImage() {
		return ( images[DISABLED] );
	}

    /**
     * Used internally to add the Border to the array and repaint if necessary.
     *
     * @param   id      the buttonState, used to index the array
     * @param   border  the Border, which is not supposed to be null
     */
	private synchronized void setBorder( int id, Border border ) {
	    if ( borders[id] != border ) {
       	    borders[id] = border;
       	    if ( buttonState == id ) {
       	        repaint();
       	    }
        }
	}

    /**
     * Sets the border to display when the button is not pressed or hilited
     * because of a mouse-over.  This border is also used in those other cases
     * if no alternative border is requested.
     *
     * @param border     the unarmed border
     */
	public void setUnarmedBorder( Border border ) {
        setBorder( UNARMED, border );
        if ( borders[ARMED] == null ) {
            setArmedBorder( border );
        }
        if ( borders[OVER] == null ) {
            setOverBorder( border );
        }
        if ( borders[DISABLED] == null ) {
            setDisabledBorder( border );
        }
	}

    /**
     * Sets the border to display when the button is pressed and the mouse
     * is still over the button.
     *
     * @param border     the armed border
     */
	public void setArmedBorder( Border border ) {
   	    if ( border != null ) {
       	    setBorder( ARMED, border );
   	    }
   	    else {
   	        setBorder( ARMED, borders[UNARMED] );
   	    }
	}

    /**
     * Sets the border to display when the button is not pressed and the mouse
     * is over the button.
     *
     * @param border     the over border
     */
	public void setOverBorder( Border border ) {
   	    if ( border != null ) {
       	    setBorder( OVER, border );
   	    }
   	    else {
   	        setBorder( OVER, borders[UNARMED] );
   	    }
	    setBorder( OVER, border );
	}

    /**
     * Sets the border to display when the button is disabled.
     *
     * @param border     the disabled border
     */
	public void setDisabledBorder( Border border ) {
   	    if ( border != null ) {
       	    setBorder( DISABLED, border );
   	    }
   	    else {
   	        setBorder( DISABLED, borders[UNARMED] );
   	    }
	    if ( buttonState == DISABLED ) {
    	    repaint();
    	}
	}

    /**
     * Gets the border to display when the button is not pressed or hilited
     * because of a mouse-over.  This border is also used in those other cases
     * if no alternative border is requested.
     *
     * @return     the unarmed border
     */
	public Border getUnarmedBorder() {
		return ( borders[UNARMED] );
	}

    /**
     * Gets the border to display when the button is pressed and the mouse
     * is still over the button.
     *
     * @return     the armed border
     */
	public Border getArmedBorder() {
		return ( borders[ARMED] );
	}

    /**
     * Gets the border to display when the button is not pressed and the mouse
     * is over the button.
     *
     * @return     the over border
     */
	public Border getOverBorder() {
		return ( borders[OVER] );
	}

    /**
     * Gets the border to display when the button is disabled.
     *
     * @return     the armed border
     */
	public Border getDisabledBorder() {
		return ( borders[DISABLED] );
	}

    /**
     * Gets the current buttonState id for the button
     *
     * @return     the button state integer id
     */
	public int getButtonState() {
	    return buttonState;
	}

    /**
     * Sets the current buttonState id for the button
     *
     * @param   buttonState     the button state integer id
     */
	protected void setButtonState( int buttonState ) {
	    if ( buttonState != this.buttonState ) {
    		this.buttonState = buttonState;
            repaint();
    	}
	}

    Color borderColor = Color.GRAY;

    /**
     * Overrides awt.Component.paint() to paint the current border and image.
     *
     * @param     g     The Graphics in which to draw
     */
    public void paint( Graphics g ) {
        Dimension size = getSize();
	setBackground(borderColor);
        borders[buttonState].paint( g, borderColor, 0, 0, size.width, size.height );
        try {
            if ( ! tracker.checkID( buttonState ) ) {
                tracker.waitForID( buttonState );
            }
            if ( ! tracker.isErrorID( buttonState ) ) {
                Insets insets = borders[buttonState].getInsets();
                int imageWidth = images[buttonState].getWidth( this );
                int imageHeight = images[buttonState].getHeight( this );
                int x = insets.left +
                        ( ( ( size.width - ( insets.left + insets.right ) ) -
                            imageWidth ) / 2 );
                int y = insets.top +
                        ( ( ( size.height - ( insets.top + insets.bottom ) ) -
                            imageHeight ) / 2 );
                g.drawImage( images[buttonState], x, y, this );
            }
        }
        catch ( InterruptedException ie ) {
        }
    }

    /**
     * Overrides awt.Component.preferredSize() to return the preferred size of the button.
     * This assumes the images (if more than one) are all the same size.  It also calculates
     * the maximum insets from all borders and adds them to the image dimensions.
     */
    public Dimension getPreferredSize() {
        Dimension pref = new Dimension();
        try {
            if ( ! tracker.checkID( buttonState ) ) {
                tracker.waitForID( buttonState );
            }
            if ( ! tracker.isErrorID( buttonState ) ) {
                Dimension size = getSize();
                pref.width = images[buttonState].getWidth( this );
                pref.height = images[buttonState].getHeight( this );
            }
            int maxWidthAdd = 0;
            int maxHeightAdd = 0;
            for ( int i = 0; i < DISABLED; i++ ) {
                Insets insets = borders[i].getInsets();
                maxWidthAdd = Math.max( maxWidthAdd, insets.left+insets.right );
                maxHeightAdd = Math.max( maxHeightAdd, insets.top+insets.bottom );
            }
            pref.width += maxWidthAdd;
            pref.height += maxHeightAdd;
        }
        catch ( InterruptedException ie ) {
        }
        return pref;
    }

    Vector actionListeners = new Vector();

	public void setActionCommand(String command) {
		this.command = command;
	}

	public String getActionCommand() {
		return command;
	}

    public void addActionListener(ActionListener l) {
	actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
	actionListeners.remove(l);
    }

    protected void processActionEvent(ActionEvent e) {
	Iterator iter = actionListeners.iterator();
	while(iter.hasNext()) {
	    ActionListener l = (ActionListener)iter.next();
	    l.actionPerformed(e);
	}
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mousePressed(MouseEvent arg0) {
        mousedown = true;
        setButtonState( ARMED );
    }

    public void mouseReleased(MouseEvent arg0) {
        mousedown = false;
	setButtonState( UNARMED );
	processActionEvent(new ActionEvent(this, 0, command));
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

}
/**
 * DisableImageFilter - an RGBImageFilter that "greys out" an image by "blanking out"
 * every other pixel.
 */
class DisableImageFilter extends RGBImageFilter
{
    /**
     * Constructs a DisableImageFilter.  The canFilterIndexColorModel is set to false
     * because the pixel index is important during filtering.
     */
    public DisableImageFilter() {
        canFilterIndexColorModel = false;
    }

    /**
     * Called when a disabled image is created to alter the pixels to be blanked out.
     *
     * @param   x   the x position of the pixel
     * @param   y   the y position of the pixel
     * @param   rgb the rgb value of the pixel
     */
    public int filterRGB( int x, int y, int rgb ) {
        if ( ( ( x % 2 ) ^ ( y % 2 ) ) == 1 ) {
            return ( rgb & 0xffffff );
        }
        else {
            return rgb;
        }
    }
}

/**
 * DefaultImageButtonBorder - a Border, subclassed to set the default border values.
 */
class DefaultImageButtonBorder extends Border {

    public DefaultImageButtonBorder( boolean armed ) {
        setBorderThickness( 2 );
        if ( armed ) {
            setType( THREED_IN );
            setMargins( 4, 4, 2, 2 );
        }
        else {
            setType( THREED_OUT );
            setMargins( 3 );
        }
    }
}
