import java.awt.AWTEvent;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/* -------------------------------------------------------------------------
/*
/* CLASS TextFieldFocus
/*
/* ------------------------------------------------------------------------- */

/** TextField that sends an ActionEvent when the integer Value has changed
 * either by hitting return (as the normal AWT TextField) or by
 * loss of Mouse Focus (new).*/

public class TextFieldFocus extends TextField implements FocusListener {

	private String currentText;
	private String actionCommand;

/* Constructors */
	public TextFieldFocus() {
		this("", "TextFieldFocus text changed");
	}

	public TextFieldFocus(int columns) {
		this("", "TextFieldFocus text changed");
		setColumns(columns);
	}

	public TextFieldFocus(String text, int columns) {
		this(text, "TextFieldFocus text changed");
		setColumns(columns);
	}

	public TextFieldFocus(String text, int columns, String actionCommand) {
		this(text, actionCommand);
		setColumns(columns);
	}

	public TextFieldFocus(String text, String actionCommand) {
		super(text);
		currentText = text;
		enableEvents(AWTEvent.ACTION_EVENT_MASK);
		addFocusListener(this);
		this.actionCommand = actionCommand;
	}

/* End of Constructors */

	public void setText(String text) {
		super.setText(text);
	}
	
/** Sets the command string returned by an ActionEvent. */
	public void setActionCommand(String newCommand) {
		actionCommand = newCommand;
	}
	
/** Returns the action command string. */
	public String getActionCommand() {
		return actionCommand;
	}

/* Hand on an ActionEvent to listeners only if a new Text has been entered. */
	protected void processActionEvent(ActionEvent e) {
		String tempText = getText();

		if (e.getSource() == this && !tempText.equals((Object)currentText)) {
			currentText = tempText;
			super.processActionEvent(new ActionEvent(this, e.getID(), actionCommand, e.getModifiers()));
		}
	}

/* If mouse focus is lost, trigger by the processActionEvent method. */
	public void focusLost(FocusEvent e) {
		if (e.getSource() == this) {
			processActionEvent(new ActionEvent(this, ActionEvent.ACTION_FIRST, actionCommand));
		}
	}

	public void focusGained(FocusEvent e) {
	}
}
