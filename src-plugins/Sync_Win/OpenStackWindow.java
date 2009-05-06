import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseWheelEvent;

import ij.*;
import ij.gui.*;

/* -------------------------------------------------------------------------
/*
/* CLASS OpenStackWindow
/*
/* ------------------------------------------------------------------------- */

/** StackWindow, which issues DispChanged Events
 *  to registered Listeners, when the displayed slice has been changed.
 */
public class OpenStackWindow extends StackWindow {
    DisplayChangeListener displayChangeListener = null;

    public OpenStackWindow(ImagePlus imp) {
        super(imp);
        if (ij!=null) {
            Image img = ij.getIconImage();
            if (img!=null) setIconImage(img);
        }
    }

    public OpenStackWindow(ImagePlus imp, ImageCanvas ic) {
        super(imp, ic);
        if (ij!=null) {
            Image img = ij.getIconImage();
            if (img!=null) setIconImage(img);
        }
    }

/** Calls super.updateSliceSelector() and issues a DisplayChangeEvent to registered listeners.
 */
//    public void updateSliceSelector() {
//        super.updateSliceSelector();
//// notify DisplayChangeListeners
//        if (displayChangeListener != null) {
//            DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, imp.getCurrentSlice());
//            displayChangeListener.displayChanged(dcEvent);
//        }
//    }
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
        super.adjustmentValueChanged(e);
        if (!running2) {
//          notify DisplayChangeListeners
            if (displayChangeListener != null) {
                DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, sliceSelector.getValue());
                displayChangeListener.displayChanged(dcEvent);
            }            
        }
    }

    /** Handles changing slice by MouseWheel */   
    public void mouseWheelMoved(MouseWheelEvent event) {
        super.mouseWheelMoved(event);
        if (displayChangeListener != null) {
            DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, sliceSelector.getValue());
            displayChangeListener.displayChanged(dcEvent);
        } 
    }

    public synchronized void addDisplayChangeListener(DisplayChangeListener l) {
        displayChangeListener = IJEventMulticaster.add(displayChangeListener, l);
    }
    public synchronized void removeDisplayChangeListener(DisplayChangeListener l) {
        displayChangeListener = IJEventMulticaster.remove(displayChangeListener, l);
    }

}
