import java.util.EventObject;

/* -------------------------------------------------------------------------
/*
/* CLASS DisplayChangeEvent
/*
/* ------------------------------------------------------------------------- */

/** To be raised when a property of the image display has been changed */
public class DisplayChangeEvent extends EventObject {

/** Type of change in display:
 *  Coordinate X, Y, Z, the Zoom, time, color channel.
 *  So far there is no need for properties other than Z.
 */
    public static final int X = 1;
    public static final int Y = 2;
    public static final int Z = 3;
    public static final int ZOOM = 4;
    public static final int T = 5;
    public static final int CHANNEL = 6;

    private int type;
    private int value;

    public DisplayChangeEvent(Object source, int type, int value) {
        super(source);
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}