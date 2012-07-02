import java.awt.AWTEvent;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/* -------------------------------------------------------------------------
/*
/* CLASS IntField
/*
/* ------------------------------------------------------------------------- */

/** TextField that takes only integers, and sends an ActionEvent
 * when the integer Value has changed either by hitting return or by
 * loss of Mouse Focus.*/

public class IntField extends TextField implements FocusListener {

	private int value;
	private int minValue;
	private int maxValue;
	private String actionCommand;

/* Constructors */

/** Constructs an IntField with value 0 and maximal bounds. */
	public IntField() {
		this(0);
	}

/** Constructs an IntField with the specified value and maximal bounds. */
	public IntField(int value) {
		this(value, Integer.MIN_VALUE, Integer.MAX_VALUE, 3, "IntField value changed");
	}

/** Constructs an IntField with specified value and bounds. The number of columns is set automatically. */
	public IntField(int value, int minValue, int maxValue) {
		this(value, minValue, maxValue,
			 Math.max(Integer.toString(minValue).length(), Integer.toString(maxValue).length()),
			 "IntField value changed");
	}
	
/** Constructs an IntField with specified value, bounds and number of columns.*/
	public IntField(int value, int minValue, int maxValue, int columns) {
		this(value, minValue, maxValue, columns, "IntField value changed");
	}

/** Constructs an IntField with specified value, bounds, number of columns, and command string of the ActionEvent. */
	public IntField(int value, int minValue, int maxValue, int columns, String actionCommand) {
		super(Integer.toString(value), columns);
		if(columns<=0) {
			throw new IllegalArgumentException("Too few Columns in IntField.");
		}
		if(value<minValue || value>maxValue) {
			throw new IllegalArgumentException("value, minValue and maxValue of IntField are inconsistent.");
		}
		this.value = value;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.actionCommand = actionCommand;
		enableEvents(AWTEvent.ACTION_EVENT_MASK);
		addFocusListener(this);
	}

/* End of Constructors */

	public int getValue() {
		return value;
	}

/** Sets the value of the field to newValue without sending an ActionEvent. */
	public void setValue(int newValue) {
		setValue(newValue, false);
	}

/** Sets the value of the field to newValue. Sends and ActionEvent,
 *	if sendEvent is true.
 */
	public void setValue(int newValue, boolean sendEvent) {
		if (newValue >= minValue && newValue <= maxValue) {
			if (newValue != value) {
				if (sendEvent) {					
					setText(Integer.toString(newValue));
					processEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
				} else {
					value = newValue;
					setText(Integer.toString(newValue));
				}
			}
		} else {
			throw new IllegalArgumentException("value out of range: "+newValue);
		}
	}
	
/** Sets the command string returned by an ActionEvent. */
	public void setActionCommand(String newCommand) {
		actionCommand = newCommand;
	}
	
/** Returns the action command string. */
	public String getActionCommand() {
		return actionCommand;
	}


/** Hand on an ActionEvent to listeners only if a new and valid
	integer has been entered. */
	protected void processActionEvent(ActionEvent e) {
		int tempValue;
		if (e.getSource() == this) {
			try {
				tempValue = Integer.parseInt(getText());
			} catch (NumberFormatException ex) {
				setText(Integer.toString(value));
				return;
			}
			if (tempValue >= minValue && tempValue <= maxValue && tempValue != value) {
				value = tempValue;
				super.processActionEvent(new ActionEvent(this, e.getID(), actionCommand, e.getModifiers()));
			}
			setText(Integer.toString(value));
		}
	}

/* If mouse focus is lost, trigger inspection for valid integer
	by the processActionEvent method. */
	public void focusLost(FocusEvent e) {
		if (e.getSource() == this) {
			processEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
		}
	}

	public void focusGained(FocusEvent e) {
	}
}
