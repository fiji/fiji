/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

/**
 * Evaluator - JEP Example Applet
 * Copyright (c) 2004 Nathan Funk
 *
 * @author Nathan Funk

HTML code for running the applet:
<applet code="org/nfunk/jepexamples/Evaluator.class" width=400 height=200>
</applet>

*/
package org.nfunk.jepexamples;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

import org.nfunk.jep.JEP;


/**
 * This applet is an simple example for how JEP can be used to evaluate
 * expressions. It also displays the different options, and the effects of
 * their settings.
 */
public class Evaluator extends Applet {

	private static final long serialVersionUID = 4592714713689369505L;

	/** Parser */
	private JEP myParser;
	
	/** Current xValue */
	private double xValue;

	/* GUI components */
	private TextField exprField, xField;
	private TextArea errorTextArea;
	private Label resultLabel;
	private Checkbox implicitCheckbox, undeclaredCheckbox;

	
	/** 
	 * This method is called if the applet is run as an standalone
	 * program. It creates a frame for the applet and adds the applet
	 * to that frame.
	 */
	public static void main(String args[]) {
		Evaluator a = new Evaluator();
		a.init();
		a.start();

		Frame f = new Frame("Evaluator");
		f.add("Center", a);
		f.setSize(400,200);
		f.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			}
		);
		
//		f.show(); 
		f.setVisible(true);
	}

	/**
	 * The initialization function of the applet. It adds all the
	 * components such as text fields and also creates the JEP object
	 */
	public void init() {
		// initialize value for x
		xValue = 10;

		// add the interface components
		addGUIComponents();
		
		// Set up the parser (more initialization in parseExpression()) 
		myParser = new JEP();
		myParser.initFunTab(); // clear the contents of the function table
		myParser.addStandardFunctions();
		myParser.setTraverse(true);

		
		// simulate changed options to initialize output
		optionsChanged();
	}

	/**
	 * Creates and adds the necessary GUI components.
	 */
	private void addGUIComponents() {
		setBackground(Color.white);
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		// Expression
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		Label exprFieldp = new Label("Expression: ", Label.RIGHT);
		gridbag.setConstraints(exprFieldp,c);
		add(exprFieldp);
		
		c.weightx = 0.8;
		exprField = new TextField(27);
		gridbag.setConstraints(exprField,c);
		add(exprField);
		
		// x
		c.weightx = 0.0;
		Label xFieldp = new Label("x: ", Label.RIGHT);
		gridbag.setConstraints(xFieldp,c);
		add(xFieldp);
		
		c.weightx = 0.2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		xField = new TextField("" + xValue,4);
		gridbag.setConstraints(xField,c);
		add(xField);
		
		// Result
		c.weightx = 0.0;
		c.gridwidth = 1;
		Label resultLabelText = new Label("Result: ", Label.RIGHT);
		gridbag.setConstraints(resultLabelText,c);
		add(resultLabelText);
		
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		resultLabel = new Label("", Label.LEFT);
		gridbag.setConstraints(resultLabel,c);
		add(resultLabel);
		
		// Options
		c.weightx = 0.0;
		c.gridwidth = 1;
		Label optionsLabelText = new Label("Options: ", Label.RIGHT);
		gridbag.setConstraints(optionsLabelText,c);
		add(optionsLabelText);
		
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		implicitCheckbox = new Checkbox("Implicit multiplication", true);
		gridbag.setConstraints(implicitCheckbox,c);
		add(implicitCheckbox);
		
		c.weightx = 0.0;
		c.gridwidth = 1;
		Label spaceLabelText = new Label(" ", Label.RIGHT);
		gridbag.setConstraints(spaceLabelText,c);
		add(spaceLabelText);

		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		undeclaredCheckbox = new Checkbox("Allow undeclared identifiers");
		gridbag.setConstraints(undeclaredCheckbox,c);
		add(undeclaredCheckbox);

		// Errors
		c.weightx = 0.0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTH;
		Label errorLabel = new Label("Errors: ", Label.RIGHT);
		gridbag.setConstraints(errorLabel,c);
		add(errorLabel);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		errorTextArea = new TextArea("");
		errorTextArea.setEditable(false);
		errorTextArea.setBackground(Color.white);
		gridbag.setConstraints(errorTextArea,c);
		add(errorTextArea);

		// Set up listeners
		exprField.addTextListener(
			new TextListener() {
				public void textValueChanged(TextEvent evt) {
					exprFieldTextValueChanged();
				}
			}
		);

		xField.addTextListener(
			new TextListener() {
				public void textValueChanged(TextEvent evt) {
					xFieldTextValueChanged();
				}
			}
		);
		
		implicitCheckbox.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent evt) {
					optionsChanged();
				}
			}
		);
		
		undeclaredCheckbox.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent evt) {
					optionsChanged();
				}
			}
		);
	}

	/**
	 * Parses the current expression in the exprField. This method also
	 * re-initializes the contents of the symbol and function tables. This
	 * is necessary because the "allow undeclared variables" option adds
	 * variables from expressions to the symbol table.
	 */
	private void parseExpression() {
		myParser.initSymTab(); // clear the contents of the symbol table
		myParser.addStandardConstants();
		myParser.addComplex(); // among other things adds i to the symbol table
		myParser.addVariable("x", xValue);
		myParser.parseExpression(exprField.getText());
	}

	/**
	 * Whenever the expression is changed, this method is called.
	 * The expression is parsed, and the updateResult() method
	 * invoked.
	 */
	private void exprFieldTextValueChanged() {
		parseExpression();
		updateResult();
	}
	
	/**
	 * Every time the value in the x field is changed, this method is
	 * called. It takes the value from the field as a double, and
	 * sets the value of x in the parser.
	 */
	private void xFieldTextValueChanged() {
		
		try {
			xValue = Double.valueOf(xField.getText()).doubleValue();
		} catch (NumberFormatException e) {
			System.out.println("Invalid format in xField");
			xValue = 0;
		}

		myParser.addVariable("x", xValue);

		updateResult();
	}
	
	/**
	 * Every time one of the options is changed, this method is called. The
	 * parser settings are adjusted to the GUI settings, the expression is
	 * parsed again, and the results updated.
	 */
	private void optionsChanged() {
		myParser.setImplicitMul(implicitCheckbox.getState());
		myParser.setAllowUndeclared(undeclaredCheckbox.getState());
		parseExpression();
		updateResult();
	}
	
	/**
	 * This method uses JEP's getValueAsObject() method to obtain the current
	 * value of the expression entered.
	 */
	private void updateResult() {
		Object result;
		String errorInfo;
		
		// Get the value
		result = myParser.getValueAsObject();
		
		// Is the result ok?
		if (result!=null) {
			resultLabel.setText(result.toString());
		} else {
			resultLabel.setText("");
		}
		
		// Get the error information
		if ((errorInfo = myParser.getErrorInfo()) != null) {
			errorTextArea.setText(errorInfo);
		} else {
			errorTextArea.setText("");
		}
	}
}
