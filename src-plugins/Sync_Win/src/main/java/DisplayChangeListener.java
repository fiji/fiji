/* -------------------------------------------------------------------------
/*
/* INTERFACE DisplayChangeListener
/*
/* ------------------------------------------------------------------------- */

/** The Listener interface for receiving DisplayChange events.
 *  The listener can be registered to an Object issuing DisplayChange events
 *  by its addDisplayChangeListener method.
 *  So far only OpenStackWindow used by SyncWindows is such an Object.
 *  */
public interface DisplayChangeListener extends java.util.EventListener {

    public void displayChanged(DisplayChangeEvent e);
}
