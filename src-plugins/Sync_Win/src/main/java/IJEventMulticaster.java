import java.awt.AWTEventMulticaster;
import java.util.EventListener;

/* -------------------------------------------------------------------------
/*
/* CLASS IJEventMulticaster
/*
/* ------------------------------------------------------------------------- */

/**
 * Multicaster for events special to ImageJ
 * <p>
 * Example how to implement an object, which fires DisplayChangeEvents using the
 * IJEventMulticaster:
 *
 * <pre><code>
 * public mySpecialWindow extends StackWindow {
 *
 *      DisplayChangeListener dclistener = null;
 *
 *      public synchronized void addDisplayChangeListener(DisplayChangeListener l) {
 *          dclistener = IJEventMulticaster.add(dclistener, l);
 *      }
 *
 *      public synchronized void removeDisplayChangeListener(DisplayChangeListener l) {
 *          dclistener = IJEventMulticaster.remove(dclistener, l);
 *      }
 *
 *      public void myEventFiringMethod(arguments) {
 *          ... code ...
 *          if (dclistener != null) {
 *              DisplayChangeEvent dcEvent = new DisplayChangeEvent(this, DisplayChangeEvent.Z, zSlice);
 *              dclistener.displayChanged(dcEvent);
 *          }
 *          ... code ...
 *      }
 *
 *      ... other methods ...
 * }
 * </code></pre>
 *
 * To put in a new event-listener (by changing this class or extending it):
 * <p>
 * - Add the listener to the "implements" list.
 * <p>
 * - Add the methods of this listener to pass on the events (like displayChanged).
 * <p>
 * - Add the methods "add" and "remove" with the corresponding listener type.
 * <p>
 *
 * @author: code take from Sun's AWTEventMulticaster by J. Walter 2002-03-07
 */

public class IJEventMulticaster extends AWTEventMulticaster implements DisplayChangeListener {

    IJEventMulticaster(EventListener a, EventListener b) {
        super(a,b);
    }

    /**
     * Handles the DisplayChange event by invoking the
     * displayChanged methods on listener-a and listener-b.
     * @param e the DisplayChange event
     */

    public void displayChanged(DisplayChangeEvent e) {
        ((DisplayChangeListener)a).displayChanged(e);
        ((DisplayChangeListener)b).displayChanged(e);
    }
    /**
     * Adds DisplayChange-listener-a with DisplayChange-listener-b and
     * returns the resulting multicast listener.
     * @param a DisplayChange-listener-a
     * @param b DisplayChange-listener-b
     */
    public static DisplayChangeListener add(DisplayChangeListener a, DisplayChangeListener b) {
        return (DisplayChangeListener)addInternal(a, b);
    }
    /**
     * Removes the old DisplayChange-listener from DisplayChange-listener-l and
     * returns the resulting multicast listener.
     * @param l DisplayChange-listener-l
     * @param oldl the DisplayChange-listener being removed
     */
    public static DisplayChangeListener remove(DisplayChangeListener l, DisplayChangeListener oldl) {
	    return (DisplayChangeListener)removeInternal(l, oldl);
    }
}
