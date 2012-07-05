import java.awt.*;
import java.awt.event.*;

public class IntSlider extends Panel implements AdjustmentListener, ActionListener{

	private Scrollbar bar;
	private IntField field;

	private int value;
	private int minValue;
	private int maxValue;
	private String actionCommand;

    transient ActionListener actionListener;

	public static final int	HORIZONTAL = Scrollbar.HORIZONTAL;
	public static final int	VERTICAL = Scrollbar.VERTICAL;

	public static final int NORTH = 1;
	public static final int SOUTH = 2;
	public static final int EAST = 3;
	public static final int WEST = 4;


/** Creates a horizontal IntSlider with the IntField on the right,
 *  value minValue, BlockIncrement 1, and a range from minValue to maxValue.
 */
    public IntSlider(int minValue, int maxValue) {
		super(new BorderLayout(1, 1));
		if (maxValue<minValue) {
			throw new IllegalArgumentException("maximum smaller than minimum");
		}

		this.minValue = minValue;
		this.maxValue = maxValue;
		this.value = minValue;

		bar = new Scrollbar(HORIZONTAL, minValue, 1, minValue, maxValue+1);
		bar.addAdjustmentListener(this);
		this.add(bar, BorderLayout.CENTER, 0);

		field = new IntField(minValue, minValue, maxValue);
		field.addActionListener(this);
		this.add(field, BorderLayout.EAST, 1);
    }

/** Creates a horizontal IntSlider with the specified starting value, minimum and maximum values,
 * HORIZONTAL or VERTICAL orientation of the slider, specified BlockIncrement (visible)
 * and IntField in NORTH, SOUTH, WEST or EAST textPosition and size columns.
 */
	public IntSlider(int value, int minValue, int maxValue, int orientation, int visible, int textPosition) {
		this(value, minValue, maxValue, orientation, visible, textPosition, "IntSlider value changed");
	}

/** Creates a horizontal IntSlider with the specified starting value, minimum and maximum values,
 * HORIZONTAL or VERTICAL orientation of the slider, specified BlockIncrement (visible),
 * IntField in NORTH, SOUTH, WEST or EAST textPosition,
 * and command string of the ActionEvent.
 */
	public IntSlider(int value, int minValue, int maxValue, int orientation, int visible, int textPosition, String actionCommand) {
		super(new BorderLayout(1, 1));
		if (value < minValue || value > maxValue) {
			throw new IllegalArgumentException("value, minValue and maxValue inconsistent");
		}
		if (visible < 1 || visible > (maxValue-minValue)) {
			throw new IllegalArgumentException("block Increment out of range");
		}

		this.minValue = minValue;
		this.maxValue = maxValue;
		this.value = value;
		this.actionCommand = actionCommand;

		bar = new Scrollbar(orientation, value, visible, minValue, maxValue+visible);
		bar.addAdjustmentListener(this);
		this.add(bar, BorderLayout.CENTER, 0);

		field = new IntField(value, minValue, maxValue);
		field.addActionListener(this);
		switch (textPosition) {
			case NORTH:
				this.add(field, BorderLayout.NORTH, 1);
				break;
			case SOUTH:
				this.add(field, BorderLayout.SOUTH, 1);
				break;
			case EAST:
				this.add(field, BorderLayout.EAST, 1);
				break;
			case WEST:
				this.add(field, BorderLayout.WEST, 1);
				break;
			default:
				throw new IllegalArgumentException("unknown textPosition");
		}
	}

/** Sets the value of the slider to newValue without sending an ActionEvent. */
	public void setValue(int newValue) {
		setValue(newValue, false);
	}

/** Sets the value of the slider to newValue. Sends and ActionEvent,
 *	if sendEvent is true.
 */
	public void setValue(int newValue, boolean sendEvent) {
		if (newValue >= minValue && newValue <= maxValue) {
			if (newValue != value) {
				value = newValue;
				bar.setValue(value);
				field.setValue(value);
				if (sendEvent) {
					processEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
				}
			}
		} else {
			throw new IllegalArgumentException("value out of range: "+newValue);
		}
	}

/** Returns the current value. */
	public int getValue() {
		return value;
	}
	
/** Sets the command string returned by an ActionEvent. */
	public void setActionCommand(String newCommand) {
		actionCommand = newCommand;
	}
	
/** Returns the action command string. */
	public String getActionCommand() {
		return actionCommand;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == field) {
			int tempval = field.getValue();
			if (tempval != value) {
				value = tempval;
				bar.setValue(value);
				processEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
			} else {
				field.setValue(value);
			}
		}
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource() == bar && e.getValue() != value) {
			value = bar.getValue();
			field.setValue(value);

			processEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
		}
	}

// methods that hand on changes to the IntField
/** Sets the horizontal size of the IntField */
	public void setColumns(int columns) {
		field.setColumns(columns);
	}

/** Gets the horizontal size of the IntField */
	public int getColumns() {
		return field.getColumns();
	}

// Members to deal with ActionListeners, which want to be notified of a new Value.
// Copied from the source code of java.awt.TextField.
	public synchronized void addActionListener(ActionListener l) {
		if (l == null) {
			return;
		}
		actionListener = AWTEventMulticaster.add(actionListener, l);
	}

	public synchronized void removeActionListener(ActionListener l) {
		if (l == null) {
			return;
		}
		actionListener = AWTEventMulticaster.remove(actionListener, l);
	}

	protected void processEvent(AWTEvent e) {
		if (e instanceof ActionEvent) {
			processActionEvent((ActionEvent)e);
			return;
		}
		super.processEvent(e);
	}

	protected void processActionEvent(ActionEvent e) {
		if (actionListener != null) {
			actionListener.actionPerformed(e);
		}
	}

}