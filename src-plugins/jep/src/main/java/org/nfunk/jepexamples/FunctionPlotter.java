/*
HTML code for applet:
<applet code="org.nfunk.jepexamples.FunctionPlotter" width=300 height=320>
<param name=initialExpression value="100 sin(x/3) cos(x/70)">
</applet>
*/

package org.nfunk.jepexamples;

import java.applet.*;
import java.awt.*;

/**
 * This applet is a demonstration of the possible applications of the JEP
 * mathematical expression parser.<p>
 * The FunctionPlotter class arranges the text field and GraphCanvas classes
 * and requests a repainting of the graph when the expression in the text
 * field changes. All plotting (and interaction with the JEP API) is preformed
 * in GraphCanvas class.
 */
public class FunctionPlotter extends Applet {
	private static final long serialVersionUID = -27867883051236035L;

	/** The expression field */
	private java.awt.TextField exprField;
	
	/** The canvas for plotting the graph */
	private GraphCanvas graphCanvas;

	/**
	 * Initializes the applet FunctionPlotter
	 */
	public void init () {
		initComponents();
	}

	/**
	 * Sets the layout of the applet window to BorderLayout, creates all
	 * the components and associates them with event listeners if neccessary.
	 */
	private void initComponents () {
		setLayout(new BorderLayout());
		setBackground (java.awt.Color.white);

		// get the initial expression from the parameters
		String expr = getParameter("initialExpression");
		
		// write the expression into the text field
		if (expr!=null)
			exprField = new java.awt.TextField(expr);
		else
			exprField = new java.awt.TextField("");

		// adjust various settings for the expression field
		exprField.setBackground (java.awt.Color.white);
		exprField.setName ("exprField");
		exprField.setFont (new java.awt.Font ("Dialog", 0, 11));
		exprField.setForeground (java.awt.Color.black);
		exprField.addTextListener (new java.awt.event.TextListener () {
			public void textValueChanged (java.awt.event.TextEvent evt) {
				exprFieldTextValueChanged (evt);
			}
		}
		);

		add ("North", exprField);
		
		// create the graph canvas and add it
		graphCanvas = new GraphCanvas(expr, exprField);
		add ("Center", graphCanvas);
	}


	/**
	 * Repaints the graphCanvas whenever the text in the expression field
	 * changes.
	 */
	private void exprFieldTextValueChanged(java.awt.event.TextEvent evt) {
		String newExpressionString = exprField.getText();
		graphCanvas.setExpressionString(newExpressionString);
		graphCanvas.repaint();
	}

}
