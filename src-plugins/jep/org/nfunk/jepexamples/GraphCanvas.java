/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jepexamples;

import java.awt.*;

import org.nfunk.jep.JEP;

/**
 * This class plots a graph using the JEP API.
 */
public class GraphCanvas extends Canvas {
	private static final long serialVersionUID = -3169263228971794887L;

	/** Scaling of the graph in x and y directions */
	private int scaleX, scaleY;

	/** Dimensions of the canvas */
	private Dimension dimensions;

	/** Buffer for the graph */
	private Image buffer;

	/** Boolean flags */
	private boolean initializedBuffer, changedFunction, hasError;

	/** Math parser */
	private JEP myParser;

	/** The expression field where the functions are entered */
	private java.awt.TextField exprField;

	/**
	 * Constructor
	 */
	public GraphCanvas(String initialExpression,
						java.awt.TextField exprField_in) {
		scaleX = 1;
		scaleY = 1;
		dimensions = getSize();
		initializedBuffer = false;
		changedFunction = true;
		hasError = true;
		exprField = exprField_in;
		initParser(initialExpression);
	}

	/**
	 * Initializes the parser
	 */
	private void initParser(String initialExpression) {
		// Init Parser
		myParser = new JEP();
		
		// Allow implicit multiplication
		myParser.setImplicitMul(true);

		// Load the standard functions
		myParser.addStandardFunctions();

		// Load the standard constants, and complex variables/functions
		myParser.addStandardConstants();
		myParser.addComplex();
		
		// Add and initialize x to 0
		myParser.addVariable("x",0);

		// Set the string to the initial value
		setExpressionString(initialExpression);
	}

	/**
	 * Sets a new string to be used as function
	 */
	public void setExpressionString(String newString) {
		// Parse the new expression
		myParser.parseExpression(newString);

		// Find out whether there was an error in the expression
		hasError = myParser.hasError();
		if (hasError)
		  exprField.setForeground(Color.red);
		else
		  exprField.setForeground(Color.black);

		changedFunction = true;
	}

	/**
	 * @return The value of the function at an x value of the parameter.
	 */
	private double getYValue(double xValue) {
		// Save the new value in the symbol table
		myParser.addVariable("x", xValue);

		return myParser.getValue();
	}

	/**
	 * Fills the background with white.
	 */
	private void paintWhite(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0,0,dimensions.width,dimensions.height);
	}

	/**
	 * Paints the axes for the graph.
	 */
	private void paintAxes(Graphics g) {
		g.setColor(new Color(204,204,204));
		g.drawLine(0,dimensions.height/2,dimensions.width-1,dimensions.height/2);
		g.drawLine(dimensions.width/2,0,dimensions.width/2,dimensions.height-1);
	}

	/**
	 * Paints the graph of the function.
	 */
	private void paintCurve(Graphics2D g) {
		boolean firstpoint=true;
		int lastX=0, lastY=0;

		g.setColor(Color.black);

		for (int xAbsolute = 0; xAbsolute <= (dimensions.width-1); xAbsolute++)
		{
			double xRelative = (xAbsolute - dimensions.width/2)/scaleX;
			double yRelative = getYValue(xRelative);
			int yAbsolute = (int)(-yRelative*scaleY + dimensions.height/2);

			if (yAbsolute > dimensions.height)
				yAbsolute = dimensions.height;
			if (yAbsolute < -1)
				yAbsolute = -1;

			if (firstpoint != true)
				g.drawLine(lastX, lastY, xAbsolute, yAbsolute);
			else
				firstpoint = false;

			lastX = xAbsolute;
			lastY = yAbsolute;
		}
	}

	/**
	 * Draws the graph to the Graphics object. If the image buffer has been
	 * initialized, and the function has not changed since the last paint, 
	 * the image stored in the buffer is drawn straight to the Graphics
	 * object with drawImage().
	 * <p>
	 * If a image buffer has not yet been initialized (i.e. first time after
	 * being started) the buffer is created with createImage().
	 * <p>
	 * If the function has changed since the last paint, the graph is first 
	 * drawn on the buffer image, then that image is drawn on the Graphics
	 * object.
	 */
	public void paint(Graphics g_in) {
		boolean changedDimensions = !dimensions.equals(getSize());
		Graphics2D g = (Graphics2D) g_in;
		
		// If the buffer has not been initialized, do it now
		if (!initializedBuffer || changedDimensions)
		{
			dimensions = getSize();
			buffer = createImage(dimensions.width,dimensions.height);
			initializedBuffer = true;
		}
		
		// Get the Graphics instance of the buffer
		Graphics2D buffergc = (Graphics2D) buffer.getGraphics();
		// Turn on anti aliasing
		buffergc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Redraw the function on the buffer
		if (changedFunction || changedDimensions)
		{
			paintWhite(buffergc);
			paintAxes(buffergc);
			if (!hasError) paintCurve(buffergc);
			changedFunction = false;
		}

		// Copy the buffer to g
		g.drawImage(buffer, 0, 0, null);
	}	
}
