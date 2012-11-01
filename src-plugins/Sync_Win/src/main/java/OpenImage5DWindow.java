import java.awt.*;
import java.awt.event.*;

import i5d.*;
import i5d.gui.*;


/* -------------------------------------------------------------------------
/*
/* CLASS OpenImage5DWindow
/*
/* ------------------------------------------------------------------------- */

/** Image5DWindow, which issues DispChanged Events
 *  to registered Listeners, when the displayed slice has been changed.
 */

/*
 * Created on 12.04.2005
 *
 */
public class OpenImage5DWindow extends Image5DWindow {
    Image5D i5d=null;
    DisplayChangeListener displayChangeListener = null;
	/**
	 * @param imp
	 */
	public OpenImage5DWindow(Image5D imp) {
		super(imp);
        i5d = imp;
        if (ij!=null) {
            Image img = ij.getIconImage();
            if (img!=null) setIconImage(img);
        }
	}

	/**
	 * @param imp
	 * @param ic
	 */
	public OpenImage5DWindow(Image5D imp, Image5DCanvas ic) {
		super(imp, ic);
        i5d = imp;
        if (ij!=null) {
            Image img = ij.getIconImage();
            if (img!=null) setIconImage(img);
        }
	}

//	/** Calls super.updateSliceSelector() and issues a DisplayChangeEvent to registered listeners.
//	 */
//	    public void updateSliceSelector() {
//	        super.updateSliceSelector();
////	 notify DisplayChangeListeners
//	        if (displayChangeListener != null) {
//                DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, i5d.getCurrentSlice());
//                displayChangeListener.displayChanged(dcEvent);
//	        }
//	    }
        
        /** Handles changes in the scrollbars for z and t. 
         */      
        public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
            super.adjustmentValueChanged(e);
            if (!running2 && displayChangeListener != null){
                // stack scrollbar changed
                if (e.getSource() == scrollbarsWL[3]) {
                    DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, scrollbarsWL[3].getValue());
                    displayChangeListener.displayChanged(dcEvent);
                }
                // frame scrollbar changed
                if (e.getSource() == scrollbarsWL[4]) {
                    DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.T, scrollbarsWL[4].getValue());
                    displayChangeListener.displayChanged(dcEvent);
                }
            }
        }     
        
        /** Handles change in ChannelControl. 
         * Is called by ChannelControl without any events involved.
         */
        public synchronized void channelChanged() {
            super.channelChanged();
            if (!running2 && displayChangeListener != null){
                DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.CHANNEL, channelControl.getCurrentChannel());
                displayChangeListener.displayChanged(dcEvent);
            }
        }
        
        /** Handles changing slice, channel, value by moving of MouseWheel */
        public void mouseWheelMoved(MouseWheelEvent event) {
            super.mouseWheelMoved(event);
            if (displayChangeListener != null) {
                if (event.isControlDown()) {
                    DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.CHANNEL, i5d.getCurrentChannel());
                    displayChangeListener.displayChanged(dcEvent);                    
                } else if (event.isShiftDown()) {
                    DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.T, i5d.getCurrentFrame());
                    displayChangeListener.displayChanged(dcEvent);                   
                } else {
                    DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, sliceSelector.getValue());
                    displayChangeListener.displayChanged(dcEvent);
                }
            } 
        }
        
	    public synchronized void addDisplayChangeListener(DisplayChangeListener l) {
	        displayChangeListener = IJEventMulticaster.add(displayChangeListener, l);
	    }
	    public synchronized void removeDisplayChangeListener(DisplayChangeListener l) {
	        displayChangeListener = IJEventMulticaster.remove(displayChangeListener, l);
	    }

	}		

